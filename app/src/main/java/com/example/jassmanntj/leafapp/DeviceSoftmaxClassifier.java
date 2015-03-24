package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;

public class DeviceSoftmaxClassifier extends DeviceNeuralNetworkLayer {
	private DenseDoubleMatrix2D theta;
	
	public DeviceSoftmaxClassifier(AssetManager a, String filename) {
		try {
			InputStream is = a.open(filename);
			@SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String[] thetaSize = reader.readLine().split(",");
			theta = new DenseDoubleMatrix2D(Integer.parseInt(thetaSize[0]), Integer.parseInt(thetaSize[1]));
			String[] data = reader.readLine().split(",");
			for(int i = 0; i < theta.rows(); i++) {
				for(int j = 0; j < theta.columns(); j++) {
					theta.set(i, j, Double.parseDouble(data[i * theta.columns() + j]));

				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public DenseDoubleMatrix2D compute(DoubleMatrix2D input) {
		DoubleMatrix2D result = new DenseDoubleMatrix2D(input.rows(), theta.columns());
		return (DenseDoubleMatrix2D) DeviceUtils.sigmoid(input.zMult(theta, result));
	}

	@Override
	public DoubleMatrix2D getTheta() {
		return theta;
	}

	@Override
	public DoubleMatrix2D getBias() {
		return null;
	}

}
