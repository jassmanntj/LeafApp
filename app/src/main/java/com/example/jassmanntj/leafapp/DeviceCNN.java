package com.example.jassmanntj.leafapp;

import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class DeviceCNN {
    DeviceConvolutionLayer[] cls;
	DeviceSparseAutoencoder[] sls;
    DeviceSoftmaxClassifier smx;

	public DeviceCNN(DeviceConvolutionLayer[] cls, DeviceSparseAutoencoder[] sls, DeviceSoftmaxClassifier smx) {
		this.cls = cls;
        this.sls = sls;
        this.smx = smx;
	}
	
	public DoubleMatrix2D compute(DoubleMatrix2D[] input) {
        DoubleMatrix2D in = null;
        for(int i = 0; i < cls.length; i++) {
			in = cls[i].compute(input);
		}
        for(int i = 0; i < sls.length; i++) {
            in = sls[i].compute(in);
        }
		return smx.compute(in);
	}
}
