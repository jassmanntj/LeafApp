package com.example.jassmanntj.leafapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix1D;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix2D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import org.jtransforms.fft.DoubleFFT_2D;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class DeviceUtils {
	
	public static DoubleDoubleFunction sub = new DoubleDoubleFunction() {
	    public double apply(double a, double b) { return a - b; }
	};   
	
	
	
	public static DoubleMatrix2D sigmoid(DoubleMatrix2D result) {
		result.assign(DoubleFunctions.neg);
		result.assign(DoubleFunctions.exp);
		result.assign(DoubleFunctions.plus(1));
		result.assign(DoubleFunctions.inv);
		return result;
	}
	
	
	public static DenseDoubleMatrix2D conv2d(DenseDoubleMatrix2D input, DenseDoubleMatrix2D kernel) {
		int totalRows = input.rows() + kernel.rows() - 1;
		int totalCols = input.columns() + kernel.columns() - 1;
		int rowSize = input.rows() - kernel.rows() + 1;
		int colSize = input.columns() - kernel.columns() + 1;
		int startRows = (totalRows-rowSize)/2;
		int startCols = (totalCols-colSize)/2;

		DoubleFactory2D concatFactory = DoubleFactory2D.dense;

		DenseDoubleMatrix2D flippedKernel = new DenseDoubleMatrix2D(kernel.rows(), kernel.columns());
		for(int i = 0; i < kernel.rows(); i++) {
			for(int j = 0; j < kernel.columns(); j++) {
				flippedKernel.set(i,j, kernel.get(kernel.rows()-1-i,kernel.columns()-1-j));
			}
		}
		kernel = flippedKernel;

		input = (DenseDoubleMatrix2D) concatFactory.appendColumns(input, new DenseDoubleMatrix2D(input.rows(), kernel.columns()-1));
		input = (DenseDoubleMatrix2D) concatFactory.appendRows(input, new DenseDoubleMatrix2D(kernel.rows()-1, input.columns()));
		kernel = (DenseDoubleMatrix2D) concatFactory.appendColumns(kernel, new DenseDoubleMatrix2D(kernel.rows(), input.columns()-kernel.columns()));
		kernel = (DenseDoubleMatrix2D) concatFactory.appendRows(kernel, new DenseDoubleMatrix2D(input.rows()-kernel.rows(), kernel.columns()));

        DenseDComplexMatrix2D inputDFT = new DenseDComplexMatrix2D(input);
		DenseDComplexMatrix2D kernelDFT = new DenseDComplexMatrix2D(kernel);
		DenseDComplexMatrix1D inDFT = (DenseDComplexMatrix1D) inputDFT.vectorize();
		DenseDComplexMatrix1D kernDFT = (DenseDComplexMatrix1D) kernelDFT.vectorize();
		
		DoubleFFT_2D t = new DoubleFFT_2D(input.rows(), input.columns());
		t.complexForward(inDFT.elements());
		t.complexForward(kernDFT.elements());
		
		inputDFT = (DenseDComplexMatrix2D) inDFT.reshape(inputDFT.rows(), inputDFT.columns());
		kernelDFT = (DenseDComplexMatrix2D) kernDFT.reshape(kernelDFT.rows(), kernelDFT.columns());
		kernelDFT.assign(inputDFT, DComplexFunctions.mult);
		kernDFT = (DenseDComplexMatrix1D) kernelDFT.vectorize();
		
		
		t.complexInverse(kernDFT.elements(), true);
		kernelDFT = (DenseDComplexMatrix2D) kernDFT.reshape(kernelDFT.rows(), kernelDFT.columns());
		
		DenseDoubleMatrix2D result = (DenseDoubleMatrix2D) kernelDFT.getRealPart();
		DenseDoubleAlgebra d = new DenseDoubleAlgebra();
		return (DenseDoubleMatrix2D) d.subMatrix(result,startRows,startRows+rowSize-1,startCols,startCols+colSize-1);
	 }

    public static int[] computeResults(DoubleMatrix2D result) {
        int[] results = new int[result.columns()];
        double[] current = new double[result.columns()];
        for(int j = 0; j < result.columns(); j++) {
            for(int k = 0; k < result.columns(); k++) {
                if(result.get(0,j) > current[k]) {
                    for(int l = result.columns()-1; l > k; l--) {
                        current[l] = current[l-1];
                        results[l] = results[l-1];
                    }
                    current[k] = result.get(0,j);
                    results[k] = j;
                    int a = 5;
                    break;
                }
            }
        }
        return results;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        Log.d("INPUT", ""+inSampleSize);
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, resId, options), reqHeight, reqWidth, true);
    }

    public static Bitmap decodeStream(InputStream is, int width, int height) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = calculateInSampleSize(o, width, height);
        o2.inJustDecodeBounds = false;
        Log.d("DECODE","Height:"+o.outHeight);
        Log.d("DECODE", "Width:" + o.outWidth);
        if(o.outWidth < o.outHeight) {
            Log.d("INPUT", "HERE");
            return Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is, null, o2), height, width, true);
        }
        else{
            Log.d("INPUT", "THERE");
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap temp = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is, null, o2), height, width, true);
            Log.d("DECODE", temp.getWidth()+"X"+temp.getHeight());
            return Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }
    }


	public static DenseDoubleMatrix2D ZCAWhiten(DenseDoubleMatrix2D input, DoubleMatrix1D meanPatch, DenseDoubleMatrix2D ZCAWhite) {
		for(int i = 0; i < input.rows(); i++) {
			input.viewRow(i).assign(meanPatch, DeviceUtils.sub);
		}
		DenseDoubleMatrix2D result = new DenseDoubleMatrix2D(input.rows(), ZCAWhite.columns());
		input.zMult(ZCAWhite, result);
		return result;
	}

}
