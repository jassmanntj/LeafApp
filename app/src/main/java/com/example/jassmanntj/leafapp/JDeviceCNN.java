package com.example.jassmanntj.leafapp;

import Jama.Matrix;

/**
 * Created by jassmanntj on 4/13/2015.
 */

public class JDeviceCNN {
    JDeviceConvolutionLayer[] cls;
    JDeviceSparseAutoencoder[] sls;
    JDeviceSoftmaxClassifier smx;

    public JDeviceCNN(JDeviceConvolutionLayer[] cls, JDeviceSparseAutoencoder[] sls, JDeviceSoftmaxClassifier smx) {
        this.cls = cls;
        this.sls = sls;
        this.smx = smx;
    }

    public Matrix compute(Matrix[] input) {
        Matrix in = null;
        for(int i = 0; i < cls.length; i++) {
            in = cls[i].compute(input);
        }
        for(int i = 0; i < sls.length; i++) {
            in = sls[i].compute(in);
        }
        return smx.compute(in);
    }
}
