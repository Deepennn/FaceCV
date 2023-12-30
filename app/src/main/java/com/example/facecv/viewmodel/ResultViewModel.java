package com.example.facecv.viewmodel;

import static com.example.facecv.util.Constant.FUNC_DETECT;
import static com.example.facecv.util.Constant.FUNC_INFER;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.facecv.model.ResultRepository;
import com.example.facecv.model.ResultModel;
import com.example.facecv.view.ResultView;

import java.util.Objects;

public class ResultViewModel extends AndroidViewModel {

    private MutableLiveData<String> resultLabelLiveData;

    private MutableLiveData<String> functionLabelLiveData;

    private MutableLiveData<Integer> functionTypeLiveData;


    private ResultModel resultModel; // *M

    private final String PREFIX_SINGULAR = "FACE: ";
    private final String PREFIX_PLURAL = "FACES: ";
    private final String LABEL_DETECT = "DETECT";
    private final String LABEL_INFER = "INFER";

    public ResultViewModel(@NonNull Application application) {
        super(application);

        // * 结果标签
        this.resultLabelLiveData =new MutableLiveData<>();
        this.resultLabelLiveData.setValue(PREFIX_SINGULAR+"0");

        // * 按键标签
        this.functionLabelLiveData =new MutableLiveData<>();
        this.functionLabelLiveData.setValue(LABEL_INFER);

        // * 功能模式
        this.functionTypeLiveData =new MutableLiveData<>();
        this.functionTypeLiveData.setValue(FUNC_DETECT);

        this.resultModel = new ResultModel(application.getApplicationContext()); // * M

    }

    public MutableLiveData<String> getResultLabelLiveData() {
        return resultLabelLiveData;
    }

    public MutableLiveData<String> getFunctionLabelLiveData() {
        return functionLabelLiveData;
    }


    public ResultRepository analyzeImage(ImageProxy image, int rotationDegrees, boolean changeToBack,
                                         ResultView resultView)
    {

//        // * 临时功能模式: 保证 VM <--> M 功能一致性
        Integer tempFunctionType = this.functionTypeLiveData.getValue();

        ResultRepository result =
                resultModel.analyzeImageImpl(image,rotationDegrees,changeToBack,
                        // * add
                        resultView, tempFunctionType); // 调用分析图像方法

        if(Objects.equals(tempFunctionType, FUNC_DETECT)){

            int numerationResult = result.getResults().size();
            String prefix = numerationResult <= 1 ? PREFIX_SINGULAR : PREFIX_PLURAL;
            this.resultLabelLiveData.postValue(prefix + numerationResult);

        }

        else if(Objects.equals(tempFunctionType, FUNC_INFER)){

            String emotionResult = result.getEmotionResult();
            this.resultLabelLiveData.postValue(emotionResult);

        }

        return result;

    }


    public void toggleFunction() {

        // * 按键标签与功能模式相反
        this.functionLabelLiveData.postValue(this.functionLabelLiveData.getValue().equals(LABEL_INFER)? LABEL_DETECT : LABEL_INFER);
        this.functionTypeLiveData.postValue(this.functionTypeLiveData.getValue().equals(FUNC_DETECT)? FUNC_INFER : FUNC_DETECT);

    }


    }
