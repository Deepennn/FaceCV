package com.example.facecv.model;

import static com.example.facecv.util.Constant.FUNC_DETECT;
import static com.example.facecv.util.Constant.FUNC_INFER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.example.facecv.util.FileUtil;
import com.example.facecv.util.PrePostProcessor;
import com.example.facecv.view.ResultView;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResultModel {

    private Context context;

    private Module mModule;

    private Module emotionModule;

    private final String[] EMOTION_CLASSES = new String[]{
            "ANGER",
            "DISGUST",
            "FEAR",
            "HAPPY",
            "NORMAL",
            "SAD",
            "SURPRISED"
    };

    public ResultModel(Context context) {

        super();

        this.context=context;

        try {

            // 加载模型权重文件pt
            mModule = LiteModuleLoader.load(FileUtil.assetFilePath(context, "epoch40facebest.torchscript.pt"));

            emotionModule = LiteModuleLoader.load(FileUtil.assetFilePath(context, "mobilenetV3large.ptl"));

            // 读取类别文件classes.txt，并存储到类别列表中
            List<String> classes = new ArrayList<>();
            InputStream inputStream = context.getAssets().open("classes.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                classes.add(line);
            }

            // 将类别列表转换为字符串数组
            PrePostProcessor.mClasses = new String[classes.size()];
            for (int i = 0; i < classes.size(); i++) {
                PrePostProcessor.mClasses[i] = classes.get(i);
            }
        } catch (IOException e) {
            Log.e("ResultModel()", "Error reading assets", e);
        }

    }


    /**
     * 对图像进行分析和检测
     *
     * @param image
     * @param rotationDegrees 旋转的角度
     * @param changeToBack    是否将图像切换到前面
     * @return
     */
    public ResultRepository analyzeImageImpl(ImageProxy image, int rotationDegrees, boolean changeToBack,
                                             // * add
                                             ResultView resultView,
                                             Integer functionType)
    {


        try {

            if (mModule == null) {
                // 初始化模型
                mModule = LiteModuleLoader.load(FileUtil.assetFilePath(context, "epoch40facebest.torchscript.pt"));
            }

            if (emotionModule == null) {
                // 初始化模型
                emotionModule = LiteModuleLoader.load(FileUtil.assetFilePath(context, "mobilenetV3large.ptlmobilenetV3large.ptl"));
            }

        } catch (IOException e) {
            Log.e("analyzeImageImpl()", "Error reading assets", e);
            return null;
        }

        // * new 获取预处理和后处理器的实例
        PrePostProcessor prePostProcessor = PrePostProcessor.INSTANCE;

        @SuppressLint("UnsafeOptInUsageError") Bitmap bitmap = imgToBitmap(image.getImage());

        // 需要将图片统一顺时针旋转rotationDegrees，让图片恢复成竖直
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // 如果是前摄，则需要镜像翻转
        if (!changeToBack) {
            Matrix matrixMirror = new Matrix();
            matrixMirror.preScale(-1f, 1f);
            matrixMirror.postTranslate(bitmap.getWidth(), 0f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrixMirror, true);
        }

        // 将bitmap变成规定的输入尺寸
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, prePostProcessor.getMInputWidth(), prePostProcessor.getMInputWidth(), true);


        // * new
        if(Objects.equals(functionType, FUNC_DETECT)) {

            // bitmap转换为tensor格式
            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                    prePostProcessor.getNO_MEAN_RGB(), prePostProcessor.getNO_STD_RGB());
            // 推理并得到结果：这里体现了模型的使用
            IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
            Tensor outputTensor = outputTuple[0].toTensor();
            float[] outputs = outputTensor.getDataAsFloatArray();
            // 得到bitmap和模型要求的输入的长宽对应的比例、bitmap和模型输出的长款对应比例，用来恢复output中box的位置
            float imgScaleX = bitmap.getWidth() / (float) prePostProcessor.getMInputWidth();
            float imgScaleY = bitmap.getHeight() / (float) prePostProcessor.getMInputHeight();
            float ivScaleX = resultView.getWidth() / (float) bitmap.getWidth();
            float ivScaleY = resultView.getHeight() / (float) bitmap.getHeight();

            // 将模型的输出outputs转换成结果列表，每个结果包括矩形的位置，得分和类别
            ArrayList<Result> results = prePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0f, 0f);
            return new ResultRepository(results);

        }
        else if(Objects.equals(functionType, FUNC_INFER)) {

            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB, MemoryFormat.CHANNELS_LAST);
            // * 计算表情
            Tensor outputTensor = emotionModule.forward(IValue.from(inputTensor)).toTensor();

            // getting tensor content as java array of floats
            final float[] scores = outputTensor.getDataAsFloatArray();

            // searching for the index with maximum score
            float maxScore = -Float.MAX_VALUE;
            int maxScoreIdx = -1;
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i];
                    maxScoreIdx = i;
                }
            }

            String result = EMOTION_CLASSES[maxScoreIdx];
            return new ResultRepository(result,null);

        }


        return null;


    }


    /**
     * 将图片转化为bitmap，工具方法
     *
     * @param image
     * @return Bitmap
     */
    private Bitmap imgToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


}
