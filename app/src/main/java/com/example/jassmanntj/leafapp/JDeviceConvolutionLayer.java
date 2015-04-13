package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import Jama.Matrix;

/**
 * Created by jassmanntj on 4/13/2015.
 */
public class JDeviceConvolutionLayer {
    Matrix whitenedTheta;
    Matrix whitenedBias;
    Matrix pooledFeatures;
    Matrix input;
    int imageRows;
    int imageCols;
    int patchDim;
    int poolDim;

    public JDeviceConvolutionLayer(AssetManager a, String filename) throws IOException {
        try {

            InputStream is = a.open(filename);
            @SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String[] data = reader.readLine().split(",");
            whitenedTheta = new Matrix(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
            data = reader.readLine().split(",");
            for(int i = 0; i < whitenedTheta.getRowDimension(); i++) {
                for(int j = 0; j < whitenedTheta.getColumnDimension(); j++) {
                    whitenedTheta.set(i, j, Double.parseDouble(data[i * whitenedTheta.getColumnDimension() + j]));
                }
            }
            data = reader.readLine().split(",");
            whitenedBias = new Matrix(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
            data = reader.readLine().split(",");
            for(int i = 0; i < whitenedBias.getRowDimension(); i++) {
                for(int j = 0; j < whitenedBias.getColumnDimension(); j++) {
                    whitenedBias.set(i, j, Double.parseDouble(data[i * whitenedBias.getColumnDimension() + j]));
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

    public Matrix compute(Matrix[] input) {
        int numFeatures = whitenedTheta.getColumnDimension();
        int resultRows = (imageRows - patchDim+1) / poolDim;
        int resultCols = (imageCols - patchDim+1) / poolDim;
        pooledFeatures = new Matrix(1, numFeatures * (resultRows * resultCols));
        for(int featureNum = 0; featureNum < whitenedTheta.getColumnDimension(); featureNum++) {
            Matrix convolvedFeature = convFeature(input, featureNum);
            pool(convolvedFeature, featureNum, 0, resultRows, resultCols);
        }
        return pooledFeatures;
    }

    public Matrix convFeature(Matrix[] input, int featureNum) {
        Matrix convolvedFeature = new Matrix(imageRows - patchDim + 1, imageCols - patchDim + 1);
        int patchSize = patchDim * patchDim;
        for(int channel = 0; channel < 3; channel++) {
            Matrix feature = whitenedTheta.getMatrix(patchSize*channel, patchSize*channel+patchSize-1, featureNum, featureNum);
            feature = reshape(feature, patchDim, patchDim);
            Matrix conv = JDeviceUtils.conv2d(input[channel], feature);
            convolvedFeature.plusEquals(conv);
        }
        convolvedFeature.plusEquals(new Matrix(convolvedFeature.getRowDimension(), convolvedFeature.getColumnDimension(), whitenedBias.get(0, featureNum)));
        return JDeviceUtils.sigmoid(convolvedFeature);
    }

    private Matrix reshape(Matrix input, int rows, int cols) {
        Matrix result = new Matrix(rows, cols);
        for(int i = 0; i < rows * cols; i++) {
            result.set(i/cols, i%cols, input.get(i/input.getColumnDimension(), i%input.getColumnDimension()));
        }
        return result;
    }

    public void pool(Matrix convolvedFeature, int featureNum, int imageNum, int resultRows, int resultCols) {
        for(int poolRow = 0; poolRow < resultRows; poolRow++) {
            for(int poolCol = 0; poolCol < resultCols; poolCol++) {
                Matrix patch = convolvedFeature.getMatrix(poolRow*poolDim, poolRow*poolDim+poolDim-1, poolCol*poolDim, poolCol*poolDim+poolDim-1);
                double sum = 0;
                for(int i = 0; i < patch.getRowDimension(); i++) {
                    for(int j = 0; j < patch.getColumnDimension(); j++) {
                        sum += patch.get(i,j);
                    }
                }
                pooledFeatures.set(imageNum, featureNum*resultRows*resultCols+poolRow*resultCols+poolCol, sum/(poolDim*poolDim));
            }
        }
    }

}

