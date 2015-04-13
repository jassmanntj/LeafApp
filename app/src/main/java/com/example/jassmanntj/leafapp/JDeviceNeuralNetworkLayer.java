package com.example.jassmanntj.leafapp;

import Jama.Matrix;

/**
 * Created by jassmanntj on 4/13/2015.
 */
public abstract class JDeviceNeuralNetworkLayer {
    public abstract Matrix[] compute(Matrix[] in);
}
