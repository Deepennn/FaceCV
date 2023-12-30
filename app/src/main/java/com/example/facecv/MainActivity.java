package com.example.facecv;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.facecv.controller.AbstractCameraActivity;
import com.example.facecv.databinding.ActivityMainBinding;
import com.example.facecv.model.ResultRepository;
import com.example.facecv.view.ResultView;
import com.example.facecv.viewmodel.ResultViewModel;

public final class MainActivity extends AbstractCameraActivity {

    private ActivityMainBinding binding; // * data binding
    private ResultViewModel resultViewModel; // * VM

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //0.引入VM(通过VMP)
        resultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        //1.DB绑定V(通过DBU.set)
        binding= DataBindingUtil.setContentView(this,R.layout.activity_main);

        //2.DB绑定VM(通过set)
        binding.setResultViewModel(resultViewModel);

        //3.DB绑定`MA`(通过set)
        binding.setLifecycleOwner(this);

    }

    @Override
    protected ResultRepository getAnalysisResult(ImageProxy image, int rotationDegrees, boolean changeToBack,
                                                 ResultView resultView) {
        return resultViewModel.analyzeImage(
                image,
                rotationDegrees,
                changeToBack,
                resultView
        );
    }

}