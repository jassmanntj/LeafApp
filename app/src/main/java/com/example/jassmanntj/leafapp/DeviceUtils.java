package com.example.jassmanntj.leafapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.matrix.tdcomplex.DComplexMatrix1D;
import cern.colt.matrix.tdcomplex.DComplexMatrix2D;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix1D;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix2D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleSingularValueDecomposition;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DeviceUtils {
	
	public static DoubleDoubleFunction sub = new DoubleDoubleFunction() {
	    public double apply(double a, double b) { return a - b; }
	};   

    public static final DoubleFunction norm(final double pstd) {
        return new DoubleFunction() {
            public double apply(double a) {
                double val = a < pstd ? a:pstd;
                val = val > -pstd ? val:-pstd;
                val /= pstd;
                return val;
            }
        };
    }
	
	
	public static DoubleMatrix2D sigmoid(DoubleMatrix2D result) {
		result.assign(DoubleFunctions.neg);
		result.assign(DoubleFunctions.exp);
		result.assign(DoubleFunctions.plus(1));
		result.assign(DoubleFunctions.inv);
		return result;
	}
	
	
	public static DoubleMatrix2D conv2d(DoubleMatrix2D input, DoubleMatrix2D kernel) {
		int totalRows = input.rows() + kernel.rows() - 1;
		int totalCols = input.columns() + kernel.columns() - 1;
		int rowSize = input.rows() - kernel.rows() + 1;
		int colSize = input.columns() - kernel.columns() + 1;
		int startRows = (totalRows-rowSize)/2;
		int startCols = (totalCols-colSize)/2;

		DoubleFactory2D concatFactory = DoubleFactory2D.dense;

		DoubleMatrix2D flippedKernel = new DenseDoubleMatrix2D(kernel.rows(), kernel.columns());
		for(int i = 0; i < kernel.rows(); i++) {
			for(int j = 0; j < kernel.columns(); j++) {
				flippedKernel.set(i,j, kernel.get(kernel.rows()-1-i,kernel.columns()-1-j));
			}
		}
		kernel = flippedKernel;

		input = concatFactory.appendColumns(input, new DenseDoubleMatrix2D(input.rows(), kernel.columns()-1));
		input = concatFactory.appendRows(input, new DenseDoubleMatrix2D(kernel.rows()-1, input.columns()));
		kernel = concatFactory.appendColumns(kernel, new DenseDoubleMatrix2D(kernel.rows(), input.columns()-kernel.columns()));
		kernel = concatFactory.appendRows(kernel, new DenseDoubleMatrix2D(input.rows()-kernel.rows(), kernel.columns()));

        DenseDComplexMatrix2D inputDFT = new DenseDComplexMatrix2D(input);
		DenseDComplexMatrix2D kernelDFT = new DenseDComplexMatrix2D(kernel);

		DoubleFFT_2D t = new DoubleFFT_2D(input.rows(), input.columns());

		t.complexForward(inputDFT.elements());
		t.complexForward(kernelDFT.elements());
		kernelDFT.assign(inputDFT, DComplexFunctions.mult);
		
		t.complexInverse(kernelDFT.elements(), true);

		DoubleMatrix2D result = (DenseDoubleMatrix2D) kernelDFT.getRealPart();
		DenseDoubleAlgebra d = new DenseDoubleAlgebra();
		return d.subMatrix(result,startRows,startRows+rowSize-1,startCols,startCols+colSize-1);
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

    public static Bitmap decodeStream(InputStream is1, InputStream is2, int width, int height) throws IOException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is1, null, o);
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = calculateInSampleSize(o, width, height);
        o2.inJustDecodeBounds = false;
        Bitmap img = BitmapFactory.decodeStream(is2, null, o2);
        return scaleImage(img, width, height);

    }

    public static Bitmap scaleImage(Bitmap image, int width, int height) {
        if(image.getHeight() > image.getWidth()) {
            return Bitmap.createScaledBitmap(image, width, height, true);
        }
        else {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap temp = Bitmap.createScaledBitmap(image, height, width, true);
            return Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }
    }

	public static DoubleMatrix2D ZCAWhiten(DoubleMatrix2D input, double epsilon) {
        //DoubleMatrix2D img = flatten(input);
        DoubleFactory2D df = DoubleFactory2D.dense;
        double mean = 0;
        for(int j = 0; j < input.columns(); j++) {
            mean += input.get(0,j);
        }
        mean /= input.size();
        input.assign(DoubleFunctions.minus(mean));

        DoubleMatrix2D sigma = new DenseDoubleMatrix2D(input.rows(), input.rows());
        input.zMult(input, sigma, 0, 0, false, true);
        sigma = df.diagonal(df.diagonal(sigma));
        //input.assign(DoubleFunctions.div(input.rows()));
        DenseDoubleAlgebra d = new DenseDoubleAlgebra();
        DenseDoubleSingularValueDecomposition svd = d.svd(sigma);
        DoubleMatrix1D r1 = df.diagonal(svd.getS());
        r1.assign(DoubleFunctions.plus(epsilon));
        r1.assign(DoubleFunctions.inv);
        DoubleMatrix2D r2 = df.diagonal(r1);
        DoubleMatrix2D r3 = new DenseDoubleMatrix2D(input.rows(), input.rows());
        svd.getU().zMult(r2, r3);
        DoubleMatrix2D r4 = new DenseDoubleMatrix2D(input.rows(), input.rows());
        r3.zMult(svd.getU(), r4, 0, 0, false, true);
        DoubleMatrix2D r5 = new DenseDoubleMatrix2D(input.rows(), input.columns());
        return r4.zMult(input, r5);
	}

    public static DoubleMatrix2D flatten(DoubleMatrix2D[] z) {
        DoubleMatrix2D image = new DenseDoubleMatrix2D(1, z.length*z[0].rows()*z[0].columns());
        for(int i = 0; i < z.length; i++) {
            for(int j = 0; j < z[i].rows(); j++) {
                for(int k = 0; k < z[i].columns(); k++) {
                    image.setQuick(0, i*z[i].rows()*z[i].columns()+j*z[i].columns()+k,z[i].get(j,k));
                }
            }
        }
        return image;
    }

    public static DoubleMatrix2D[] normalizeData(DoubleMatrix2D[] input) {
        double mean = 0;
        double elements = 0;
        for(DoubleMatrix2D data : input) {
            Log.d("INZZZ", "U"+data.toString());
            mean += data.zSum();
            elements += data.size();
        }
        mean /= elements;
        Log.d("MEAN", "U"+mean);
        double var = 0;
        for(DoubleMatrix2D data : input) {
            data = data.assign(DoubleFunctions.minus(mean));
            DoubleMatrix2D squareData = new DenseDoubleMatrix2D(data.toArray());
            squareData.assign(data, DoubleFunctions.mult);
            var += squareData.zSum();
        }
        var /= elements;
        Log.d("VAR", "U"+var);
        double pstd = 3 * Math.sqrt(var);
        for(DoubleMatrix2D data : input) {
            data.assign(norm(pstd));
            data.assign(DoubleFunctions.plus(1));
            data.assign(DoubleFunctions.mult(0.4));
            data.assign(DoubleFunctions.plus(0.1));
        }

        return input;
    }
}
