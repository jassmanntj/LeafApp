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

public class JDeviceCNN {
    JDeviceConvPoolLayer[] cls;
    JDeviceFCLayer[] fcs;

    public JDeviceCNN(AssetManager am, String filename) throws IOException {
        InputStream is = am.open(filename);
        @SuppressWarnings("resource")
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String[] structure = reader.readLine().split(",");
        cls = new JDeviceConvPoolLayer[Integer.parseInt(structure[0])];
        fcs = new JDeviceFCLayer[Integer.parseInt(structure[1])+1];
        for(int i = 0; i < cls.length; i++) {
            cls[i] = JDeviceConvPoolLayerFactory.getLayer(reader);
        }
        for(int i = 0; i < fcs.length; i++) {
            fcs[i] = new JDeviceFCLayer(reader);
        }
    }

    public Matrix compute(Matrix[] input) {
        Matrix in = null;
        for(int i = 0; i < cls.length; i++) {
            input = cls[i].compute(input);
        }
        in = JDeviceUtils.flatten(input);
        for(int i = 0; i < fcs.length; i++) {
            in = fcs[i].compute(in);
        }
        return in;
    }
}
