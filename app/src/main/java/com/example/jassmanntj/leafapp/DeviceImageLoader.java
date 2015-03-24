package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;


public class DeviceImageLoader {
	DenseDoubleMatrix2D images;
	DenseDoubleMatrix2D labels;
	int channels;
	int width, height;
	HashMap<String, Double> labelMap;
	DoubleFactory2D f = DoubleFactory2D.dense;
	Bitmap b;
	AssetManager a;
	
	DeviceImageLoader(AssetManager a) {
		this.a = a;
	}
	
	public void loadFolder(int channels, int width, int height) throws IOException {
		labelMap = new HashMap<String, Double>();
		this.channels = channels;
		this.width = width;
		this.height = height;
		double labelNo = -1;
		
		
		try {
            // get input stream
            InputStream ims = a.open("20150313_151856.jpg");
            Bitmap img = DeviceUtils.decodeStream(ims, width, height);
			int[] pixels = new int[width*height];
			img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
			if(pixels.length==width*height) {
				DenseDoubleMatrix2D row = new DenseDoubleMatrix2D(1, pixels.length*channels);
				DenseDoubleMatrix2D lRow = new DenseDoubleMatrix2D(1,1);
				lRow.set(0, 0, labelNo);
				for(int i = 0; i < pixels.length; i++) {
					for(int j = 0; j < channels; j++) {
						row.set(0, j*pixels.length+i, ((pixels[i]>>>(8*j)) & 0xFF));
					}
				}
				if(images == null) {
					images = row;
					labels = lRow;
				}
				else {
					labels = (DenseDoubleMatrix2D) f.appendRows(labels, lRow);
					images = (DenseDoubleMatrix2D) f.appendRows(images, row);
				}
			}
		}
        catch(IOException ex) {
            return;
        }
	}
	
	public DenseDoubleMatrix2D normalizeData(DenseDoubleMatrix2D data) {
		DenseDoubleMatrix2D mean = getRowMeans(data);
		data = (DenseDoubleMatrix2D) data.assign(mean, DeviceUtils.sub);
		DenseDoubleMatrix2D squareData = new DenseDoubleMatrix2D(data.rows(), data.columns());
		data.zMult(data, squareData);
		
		double var = squareData.zSum()/squareData.size();
		double stdev = Math.sqrt(var);
		double pstd = 3 * stdev;
		for(int i = 0; i < data.rows(); i++) {
			for(int j = 0; j < data.columns(); j++) {
				double x = data.get(i, j);
				double val = x<pstd?x:pstd;
				val = val>-pstd?val:-pstd;
				val /= pstd;
				data.set(i, j, val);
			}
		}
		data.assign(DoubleFunctions.plus(1));
		data.assign(DoubleFunctions.mult(0.4));
		data.assign(DoubleFunctions.plus(0.1));
		return data;
	}
	
	private DenseDoubleMatrix2D getRowMeans(DenseDoubleMatrix2D data) {
		DenseDoubleMatrix2D means = new DenseDoubleMatrix2D(data.rows(), data.columns());
		for(int i = 0; i < data.rows(); i++) {
			double mean = 0;
			for(int j = 0; j < data.columns(); j++) {
				mean += data.get(i, j)/data.columns();
			}
			for(int j = 0; j < data.columns(); j++) {
				means.set(i, j, mean);
			}
		}
		return means;
	}
	
	public DenseDoubleMatrix2D getImages() throws Exception {
		if(images==null) {
			throw new Exception("AalsdflKJHLJHFALKSJDHFLKAJSHDFLKJASHDFKLAJSHDFLKAJHSDFKLASJHDFKLJASHDFKLJHASDKLFJH");
		}
		return images;
	}
}
