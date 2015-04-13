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
public class JDeviceSoftmaxClassifier {
    private Matrix theta;

    public JDeviceSoftmaxClassifier(AssetManager a, String filename) throws IOException {
        try {
            InputStream is = a.open(filename);
            @SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String[] thetaSize = reader.readLine().split(",");
            theta = new Matrix(Integer.parseInt(thetaSize[0]), Integer.parseInt(thetaSize[1]));
            String[] data = reader.readLine().split(",");
            for(int i = 0; i < theta.getRowDimension(); i++) {
                for(int j = 0; j < theta.getColumnDimension(); j++) {
                    theta.set(i, j, Double.parseDouble(data[i * theta.getColumnDimension() + j]));

                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public Matrix compute(Matrix input) {
        return  JDeviceUtils.sigmoid(input.times(theta));
    }

}
