package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

public class DeviceConvolutionLayer {
	DenseDoubleMatrix2D whitenedTheta;
	DenseDoubleMatrix2D whitenedBias;
	DenseDoubleMatrix2D pooledFeatures;
	DenseDoubleMatrix2D input;
	int imageRows;
	int imageCols;
	int patchDim;
	int poolDim;
	
	public DeviceConvolutionLayer(DeviceNeuralNetworkLayer previousLayer, DenseDoubleMatrix2D zcaWhite, DenseDoubleMatrix2D meanPatch, int patchDim, int poolDim, int imageRows, int imageCols) {
		this.patchDim = patchDim;
		this.poolDim = poolDim;
		this.imageRows = imageRows;
		this.imageCols = imageCols;
		DoubleMatrix2D previousTheta = previousLayer.getTheta();
		whitenedBias = (DenseDoubleMatrix2D) previousLayer.getBias();
		whitenedTheta = new DenseDoubleMatrix2D(zcaWhite.rows(), previousTheta.columns());
		DoubleMatrix2D temp = new DenseDoubleMatrix2D(whitenedBias.rows(), whitenedBias.columns());
		zcaWhite.zMult(previousTheta, whitenedTheta);
		meanPatch.zMult(whitenedTheta, temp);
		whitenedBias.assign(temp, DeviceUtils.sub);
	}
	
	public DeviceConvolutionLayer(AssetManager a, String filename) {
		try {
			
			InputStream is = a.open(filename);
			@SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String[] data = reader.readLine().split(",");
			whitenedTheta = new DenseDoubleMatrix2D(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
			data = reader.readLine().split(",");
			for(int i = 0; i < whitenedTheta.rows(); i++) {
				for(int j = 0; j < whitenedTheta.columns(); j++) {
					whitenedTheta.set(i, j, Double.parseDouble(data[i * whitenedTheta.columns() + j]));
				}
			}
			data = reader.readLine().split(",");
			whitenedBias = new DenseDoubleMatrix2D(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
			data = reader.readLine().split(",");
			for(int i = 0; i < whitenedBias.rows(); i++) {
				for(int j = 0; j < whitenedBias.columns(); j++) {
					whitenedBias.set(i, j, Double.parseDouble(data[i * whitenedBias.columns() + j]));
				}
			}
			imageRows = Integer.parseInt(reader.readLine());
			imageCols = Integer.parseInt(reader.readLine());
			patchDim = Integer.parseInt(reader.readLine());
			poolDim = Integer.parseInt(reader.readLine());
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public DoubleMatrix2D compute(DoubleMatrix2D[] input) {
		int numFeatures = whitenedTheta.columns();
		int resultRows = (imageRows - patchDim+1) / poolDim;
		int resultCols = (imageCols - patchDim+1) / poolDim;
		pooledFeatures = new DenseDoubleMatrix2D(1, numFeatures * (resultRows * resultCols));
		for(int featureNum = 0; featureNum < whitenedTheta.columns(); featureNum++) {
			DoubleMatrix2D convolvedFeature = convFeature(input, featureNum);
			pool(convolvedFeature, featureNum, 0, resultRows, resultCols);
		}
		return pooledFeatures;
	}
	
	public DoubleMatrix2D convFeature(DoubleMatrix2D[] input, int featureNum) {
		DoubleMatrix2D convolvedFeature = new DenseDoubleMatrix2D(imageRows - patchDim + 1, imageCols - patchDim + 1);
		int patchSize = patchDim * patchDim;
		DenseDoubleAlgebra d = new DenseDoubleAlgebra();
		for(int channel = 0; channel < 3; channel++) {
			DenseDoubleMatrix2D feature = (DenseDoubleMatrix2D) d.subMatrix(whitenedTheta, patchSize*channel, patchSize*channel+patchSize-1, featureNum, featureNum);
			feature = reshape(feature, patchDim, patchDim);
			DoubleMatrix2D conv = DeviceUtils.conv2d(input[channel], feature);
			convolvedFeature.assign(conv, DoubleFunctions.plus);
		}
		convolvedFeature.assign(DoubleFunctions.plus(whitenedBias.get(0, featureNum)));
		return DeviceUtils.sigmoid(convolvedFeature);
	}
	
	private DenseDoubleMatrix2D reshape(DenseDoubleMatrix2D input, int rows, int cols) {
		DenseDoubleMatrix2D result = new DenseDoubleMatrix2D(rows, cols);
		DoubleMatrix1D in = input.vectorize();
		for(int i = 0; i < cols; i++) {
			for(int j = 0; j < rows; j++) {
				result.set(j, i, in.get(i*rows+j));
			}
		}
		return result;
	}
	
	public void pool(DoubleMatrix2D convolvedFeature, int featureNum, int imageNum, int resultRows, int resultCols) {
		DenseDoubleAlgebra d = new DenseDoubleAlgebra();
		for(int poolRow = 0; poolRow < resultRows; poolRow++) {
			for(int poolCol = 0; poolCol < resultCols; poolCol++) {
				DoubleMatrix2D patch = d.subMatrix(convolvedFeature, poolRow*poolDim, poolRow*poolDim+poolDim-1, poolCol*poolDim, poolCol*poolDim+poolDim-1);
				pooledFeatures.set(imageNum, featureNum*resultRows*resultCols+poolRow*resultCols+poolCol, patch.zSum()/(patch.size()));
			}
		}
	}

}
