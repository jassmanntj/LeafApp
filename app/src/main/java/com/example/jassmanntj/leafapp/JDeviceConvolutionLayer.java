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
public class JDeviceConvolutionLayer extends JDeviceConvPoolLayer {
    private Matrix theta[][];
    private Matrix bias;
    private Matrix input;
    private int layer1 = JDeviceUtils.PRELU;
    private double a;

    public JDeviceConvolutionLayer(BufferedReader reader) throws IOException {
        String[] data = reader.readLine().split(",");
        layer1 = Integer.parseInt(data[0]);
        a = Double.parseDouble(data[1]);
        theta = new Matrix[Integer.parseInt(data[2])][Integer.parseInt(data[3])];
        for(int i = 0; i < theta.length; i++) {
            for(int j = 0; j < theta[i].length; j++) {
                theta[i][j] = Matrix.read(reader);
            }
        }
        bias = Matrix.read(reader);
    }


    public Matrix[] compute(Matrix[] input) {
        Matrix[] result = new Matrix[theta.length];
        for (int feature = 0; feature < theta.length; feature++) {
            Matrix res = new Matrix(input[0].getRowDimension() - theta[0][0].getRowDimension() + 1, input[0].getColumnDimension() - theta[0][0].getColumnDimension() + 1);
            for (int channel = 0; channel < theta[feature].length; channel++) {
                res.plusEquals(JDeviceUtils.conv2d(input[channel], theta[feature][channel]));
            }

            result[feature] = JDeviceUtils.activationFunction(layer1, res.plus(new Matrix(res.getRowDimension(), res.getColumnDimension(), bias.get(0, feature))), a);
        }


        return result;
    }

}

