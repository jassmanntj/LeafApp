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
public class JDeviceFCLayer {
    private int activation;
    private double a;
    private Matrix theta;
    private Matrix bias;

    public JDeviceFCLayer(BufferedReader reader) throws IOException {
        String[] data = reader.readLine().split(",");
        activation = Integer.parseInt(data[0]);
        a = Double.parseDouble(data[1]);
        theta = Matrix.read(reader);
        if(Boolean.parseBoolean(data[2])) bias = Matrix.read(reader);
        else bias = null;
    }


    public Matrix compute(Matrix input) {
        Matrix result = input.times(theta);
        if(bias != null) result.plusEquals(bias);
        return JDeviceUtils.activationFunction(activation, result, a);
    }


}
