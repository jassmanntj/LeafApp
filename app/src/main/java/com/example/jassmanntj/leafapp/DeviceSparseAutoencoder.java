package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

public class DeviceSparseAutoencoder extends DeviceNeuralNetworkLayer {
	DoubleMatrix2D theta1;
	DoubleMatrix2D bias1;
	public DeviceSparseAutoencoder(AssetManager a, String filename) {
		try {
			InputStream is = a.open(filename);
			@SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String[] thetaSize = reader.readLine().split(",");
			theta1 = new DenseDoubleMatrix2D(Integer.parseInt(thetaSize[0]), Integer.parseInt(thetaSize[1]));
			String[] data = reader.readLine().split(",");
			for(int i = 0; i < theta1.rows(); i++) {
				for(int j = 0; j < theta1.columns(); j++) {
					theta1.set(i, j, Double.parseDouble(data[i * theta1.columns() + j]));

				}
			}
			String[] biasSize = reader.readLine().split(",");
			bias1 = new DenseDoubleMatrix2D(Integer.parseInt(biasSize[0]), Integer.parseInt(biasSize[1]));
			data = reader.readLine().split(",");
			for(int i = 0; i < bias1.rows(); i++) {
				for(int j = 0; j < bias1.columns(); j++) {
					bias1.set(i, j, Double.parseDouble(data[i * bias1.columns() + j]));

				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public DoubleMatrix2D compute(DoubleMatrix2D input) {
		DoubleMatrix2D result = new DenseDoubleMatrix2D(input.rows(), theta1.columns());
		input.zMult(theta1, result);
		for(int i = 0; i < input.rows(); i++) {
			result.viewRow(i).assign(bias1.viewRow(0), DoubleFunctions.plus);
		}
		return DeviceUtils.sigmoid(result);
	}

	@Override
	public DoubleMatrix2D getTheta() {
		return theta1;
	}

	@Override
	public DoubleMatrix2D getBias() {
		return bias1;
	}


}
