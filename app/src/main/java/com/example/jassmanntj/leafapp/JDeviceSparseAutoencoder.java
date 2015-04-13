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
public class JDeviceSparseAutoencoder {
    Matrix theta1;
    Matrix bias1;
    public JDeviceSparseAutoencoder(AssetManager a, String filename) {
        try {
            InputStream is = a.open(filename);
            @SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String[] thetaSize = reader.readLine().split(",");
            theta1 = new Matrix(Integer.parseInt(thetaSize[0]), Integer.parseInt(thetaSize[1]));
            String[] data = reader.readLine().split(",");
            for(int i = 0; i < theta1.getRowDimension(); i++) {
                for(int j = 0; j < theta1.getColumnDimension(); j++) {
                    theta1.set(i, j, Double.parseDouble(data[i * theta1.getColumnDimension() + j]));

                }
            }
            String[] biasSize = reader.readLine().split(",");
            bias1 = new Matrix(Integer.parseInt(biasSize[0]), Integer.parseInt(biasSize[1]));
            data = reader.readLine().split(",");
            for(int i = 0; i < bias1.getRowDimension(); i++) {
                for(int j = 0; j < bias1.getColumnDimension(); j++) {
                    bias1.set(i, j, Double.parseDouble(data[i * bias1.getColumnDimension() + j]));

                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public Matrix compute(Matrix input) {
        Matrix result = input.times(theta1);

        for(int i = 0; i < result.getRowDimension(); i++) {
            for(int j = 0; j < result.getColumnDimension(); j++) {
                result.set(i,j, result.get(i,j) + bias1.get(0,j));
            }
        }
        return JDeviceUtils.sigmoid(result);
    }


}
