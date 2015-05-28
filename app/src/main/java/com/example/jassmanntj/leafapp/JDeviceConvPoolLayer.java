package com.example.jassmanntj.leafapp;

import java.io.BufferedReader;
import java.io.IOException;

import Jama.Matrix;

/**
 * Created by jassmanntj on 4/13/2015.
 */
public abstract class JDeviceConvPoolLayer {
    public abstract Matrix[] compute(Matrix[] in);
}
