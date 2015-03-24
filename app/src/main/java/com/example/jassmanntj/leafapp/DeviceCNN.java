package com.example.jassmanntj.leafapp;

import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class DeviceCNN {
	DeviceNeuralNetworkLayer[] layers;

	public DeviceCNN(DeviceNeuralNetworkLayer[] layers) {
		this.layers = layers;
	}
	
	public DoubleMatrix2D compute(DoubleMatrix2D input) {
		long[] time = new long[layers.length];
		for(int i = 0; i < layers.length; i++) {
			long start = System.currentTimeMillis();
			input = layers[i].compute(input);
			long end = System.currentTimeMillis();
			time[i] = end - start;
		}
		return input; //DeviceUtils.computeResults(input);
	}
}
