package com.example.facecv.controller;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.facecv.R;
import com.example.facecv.model.ResultRepository;
import com.example.facecv.view.ResultView;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractCameraActivity extends AppCompatActivity {
    private long mLastAnalysisResultTime = 0;
    private ImageCapture imageCapture; // 拍照的用例
    private static final int REQUEST_CODE_PERMISSIONS = 10; // 定义权限的请求码
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"}; // 请求列表，只有一个相机使用权限
    private ResultView mResultView;
    private ExecutorService mBackgroundExecutor;
    private boolean firstIn = true;

    // * 摄像头方向
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    // * 预览视图
    private PreviewView textureView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取预处理和后处理器的实例
        // prePostProcessor = PrePostProcessor.INSTANCE;

        // *1. 判断权限是否开启, 如开启则设置相机⼯作参数
        // 判断权限是否已经获得
        if (allPermissionsGranted()) {
            // 如果权限已获得，则启动相机
            startCamera();
        } else {
            // 如果权限未获得，则向用户请求所需权限
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 10);
        }
        // *2. 指定的输出⽬录
        // outputDirectory = getOutputDirectory();

        // *3. 点击按钮，开始拍照
        // capturePhotoButton.setOnClickListener(view -> capturePhoto());

        // *4.old. 获取线程池资源，⽤以完成相机的加载、拍摄等
        // cameraExecutor = Executors.newSingleThreadExecutor(); //* cameraExecutor<=>mBackgroundExecutor

        // *4.new. 获取线程池资源，⽤以完成相机的加载、拍摄等
        // 创建后台线程池
        mBackgroundExecutor = Executors.newSingleThreadExecutor(); //* cameraExecutor<=>mBackgroundExecutor


    }


    /**
     * mBackgroundExecutor是一个线程池，当不再需要使用相机捕获图片时，应该及时关闭mBackgroundExecutor线程池，以释放资源并避免内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBackgroundExecutor.shutdown();
    }


    /**
     * 主要是进行相机拍照前相关参数的配置
     */
    protected void startCamera() { // * startCamera<=>setupCamera
        // ProcessCameraProvider实例可以用来配置相机的用例（使用方式），例如预览、照片捕获以及视频捕获等
        // 利用getInstance在后台线程中执行provider的实例化，返回值为一个ListenableFuture
        // ListenableFuture<T>表示一个异步计算的结果，它会在T异步计算完成后调用监听器，addListener后可以通过get方法拿出完成的实例化对象
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
                    try {
                        // 此时拿到cameraProvider实例化
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();


                        // preview用于接收相机捕获的图像预览
                        // Preview preview = new Preview.Builder()
                        //        .setTargetResolution(new Size(1920, 1080)) // 设置预览的分辨率
                        //        .setTargetRotation(Surface.ROTATION_90) // 设置预览的方向
                        //        .setTargetAspectRatio(new Rational(16, 9)) // 设置预览的宽高比
                        //        .setTargetName("my_preview") // 设置预览的名称
                        //        .build();
//                            Preview preview = new Preview.Builder().build();
                        Preview preview = new Preview.Builder().build();

                        //  PreviewView textureView = getCameraPreviewTextureView();
                        // * new : Only onCreate() that findViewById() is permitted
                        textureView = firstIn?getCameraPreviewTextureView():textureView;
                        firstIn = false;

                        // textureView提供了Surface对象来呈现相机预览
                        // preview将相机预览输出连接到textureView提供的Surface上
                        // Preview preview = new Preview.Builder()
                        //        .setTargetResolution(new Size(1920, 1080)) // 设置预览的分辨率
                        //        .setTargetRotation(Surface.ROTATION_90) // 设置预览的方向
                        //        .setTargetAspectRatio(new Rational(16, 9)) // 设置预览的宽高比
                        //        .setTargetName("my_preview") // 设置预览的名称
                        //        .build();
                        preview.setSurfaceProvider(textureView.createSurfaceProvider());

                        // imageCapture用于拍摄静态图像，提供了如拍摄尺寸、文件格式、闪光灯等配置选项
                        // ImageCapture imageCapture = new ImageCapture.Builder()
                        //        .setFlashMode(ImageCapture.FLASH_MODE_AUTO) // 设置闪光灯模式，包括自动、开启和关闭等模式
                        //        .setTargetAspectRatio(new Rational(16, 9)) // 设置照片的宽高比
                        //        .setTargetRotation(Surface.ROTATION_90) // 设置照片的旋转角度
                        //        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // 设置照片的捕获模式
                        //        .setCropAspectRatio(new Rational(4, 3)) // 设置照片的裁剪宽高比
                        //        .build();
                        imageCapture = new ImageCapture.Builder().build();

                        // cameraSelector用于设置前摄与后摄
//                        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
//                        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                        // 创建ImageAnalysis实例，用于分析图像
                        // 背压策略（Backpressure Strategy）是在处理数据流（例如图像或事件流）时，用于控制数据流速度和压力的一种策略。两种：
                        // 1. 当消费者处理速度较慢时，该策略会跳过中间的帧，只处理最新的帧 ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        // 2， 保留所有的图像帧 ImageAnalysis.STRATEGY_KEEP_ALL
//                            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
//                                    .setTargetResolution(new Size(480, 640)) // 设置目标分辨率
//                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 设置背压策略
//                                    .build();
                        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(480, 640)) // 设置目标分辨率
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 设置背压策略
                                .build();


                        // 设置图像分析器，需要用到一个线程池，并分析得到的image
                        imageAnalyzer.setAnalyzer(mBackgroundExecutor, image -> {

                            int rotationDegrees = image.getImageInfo().getRotationDegrees(); // 获取图像的旋转角度
                            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime >= 300) { // 检查是否满足分析时间间隔要求

                                // * new
                                // Log.i(Constant.TAG, "ANALYZING!!!!!!");
                                ResultRepository result = getAnalysisResult(image,
                                        rotationDegrees,
                                        cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA,
                                        // * add
                                        mResultView
                                ); // 调用分析图像方法

                                Log.i("Camera's Analyzer", "RESULT: " + result); // 打印分析结果
                                mLastAnalysisResultTime = SystemClock.elapsedRealtime(); // 更新上次分析结果时间
                                if (result != null) {
                                    runOnUiThread(() -> applyToUiAnalyzeImageResult(result)); // 将分析结果应用于UI线程
                                } else {
//                                  Log.e(Constant.TAG, "RESULT: NULL!!!!!!");
                                }

                            }
                            image.close(); // 关闭图像，释放资源

                        });


                        // 在使用CameraX库进行相机开发时，应用程序通常会将不同的相机用例（如预览、拍照等）绑定到相机对象上，以便实现相应的功能
                        // 当不再需要使用相机或相机用例时，应用程序需要将它们与相机解除绑定关系，释放资源
                        cameraProvider.unbindAll();

                        // 将相机用例配置与相机对象绑定在一起，实现相机预览和拍照等功能
                        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer, imageCapture);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                ContextCompat.getMainExecutor(this) // 用于创建一个可用于在主线程中执行任务的Executor对象
        );
    }


    /**
     * 对分析结果进行UI显示
     *
     * @param result
     */
    protected void applyToUiAnalyzeImageResult(ResultRepository result) {
        mResultView.setResults(result.getResults());
        // 用于标记一个视图（View）无效，并请求重新绘制
        mResultView.invalidate();
    }


    /**
     * 获取预览的TextureView
     *
     * @return
     */
    private PreviewView getCameraPreviewTextureView() {
        mResultView = findViewById(R.id.resultView);
        if (!firstIn) {
            return findViewById(R.id.object_detection_texture_view);
        } else {
            return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                    .inflate()
                    .findViewById(R.id.object_detection_texture_view);
        }
    }


    /**
     * 检查所有请求的权限是否同意
     */
    private boolean allPermissionsGranted() {
        boolean allGranted = true;
        for (String request :
                REQUIRED_PERMISSIONS) {
            allGranted &= ContextCompat.checkSelfPermission(this, request) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }


    /**
     * @param requestCode  请求码
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    // * 拍摄照片
    /**
     * 新版本实现方式：使用MediaStore - 存储在系统相册中
     */
    public void capturePhoto(View view)
    { // * new
        if (imageCapture != null) {
            // ContentValues
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis());
            // 创建一个 ContentValues 对象用来存储保存图片的详细信息。
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //如果设备运行的Android版本大于等于Android 10 (Q-API29)，设置图片的相对路径为"Pictures/CameraXImage"。
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraXImage");
            }
            //配置如何保存图片。将图片保存到设备的外部存储的图片目录。
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                    .Builder(this.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    .build();
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    String msg = "存储成功：" + outputFileResults.getSavedUri();
                    Log.i("capturePhoto()", Objects.requireNonNull(msg));
                    Toast.makeText(AbstractCameraActivity.this,
                            msg, Toast.LENGTH_LONG).show();
                    Log.d("capturePhoto()", msg);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("capturePhoto()", exception.toString());
                }
            });
        }
        else {
            Log.e("capturePhoto()", " NO imageCapture");
        }
    }


    public void flipCamera(View view){
        cameraSelector = (cameraSelector==CameraSelector.DEFAULT_BACK_CAMERA) ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        // * 重启摄像头
        startCamera();
    }


    protected abstract ResultRepository getAnalysisResult(ImageProxy image, int rotationDegrees, boolean changeToBack,
                                                          ResultView resultView);



}
