package com.example.jassmanntj.leafapp;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by jassmanntj on 4/14/2015.
 */
public abstract class JDeviceConvPoolLayerFactory {
    public static final int CONVLAYER = 0;
    public static final int POOLLAYER = 1;

    public static JDeviceConvPoolLayer getLayer(BufferedReader reader) throws IOException {
        int type = Integer.parseInt(reader.readLine());
        switch(type) {
            case CONVLAYER:
                return new JDeviceConvolutionLayer(reader);
            case POOLLAYER:
                return new JDevicePoolingLayer(reader);
            default:
                throw new IOException("Invalid Layer: "+type);
        }
    }
}
