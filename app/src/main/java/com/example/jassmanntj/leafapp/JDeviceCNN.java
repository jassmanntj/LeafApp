package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;
import android.util.Log;

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
        fcs = new JDeviceFCLayer[Integer.parseInt(structure[1])];
        for(int i = 0; i < cls.length; i++) {
            cls[i] = JDeviceConvPoolLayerFactory.getLayer(reader);
        }
        for(int i = 0; i < fcs.length; i++) {
            fcs[i] = new JDeviceFCLayer(reader);
        }
    }

    public Matrix compute(Matrix[] input) {
        Matrix in = null;
        //Log.d("VALS", "IMG: " + getString(input[0]));
        for(int i = 0; i < cls.length; i++) {
            input = cls[i].compute(input);
            //Log.d("VALS", "INPUT" + i + ": " + getString(input[0]));
        }
        in = JDeviceUtils.flatten(input);
        //Log.d("VALS","FLAT: "+getString(in));
        for(int i = 0; i < fcs.length; i++) {
            in = fcs[i].compute(in);
            //Log.d("VALS","IN"+i+": "+getString(in));

        }
        return in;
    }

    private String getString(Matrix mat) {
        String s = "";
        for(int i = 0; i < mat.getRowDimension(); i++) {
            for(int j = 0; j < mat.getColumnDimension(); j++) {
                s += mat.get(i,j);
            }
        }
        return s;
    }
}
