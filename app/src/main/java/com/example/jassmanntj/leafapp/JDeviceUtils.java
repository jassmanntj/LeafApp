package com.example.jassmanntj.leafapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;

/**
 * Created by jassmanntj on 4/13/2015.
 */
public class JDeviceUtils {

    public static final int NUMTHREADS = 8;
    public static final int NONE = 0;
    public static final int SIGMOID = 1;
    public static final int PRELU = 2;
    public static final int RELU = 3;


    public static Matrix conv2d(Matrix input, Matrix kernel) {
        Matrix flippedKernel = new Matrix(kernel.getRowDimension(), kernel.getColumnDimension());
        for(int i = 0; i < kernel.getRowDimension(); i++) {
            for(int j = 0; j < kernel.getColumnDimension(); j++) {
                flippedKernel.set(i,j, kernel.get(kernel.getRowDimension()-1-i,kernel.getColumnDimension()-1-j));
            }
        }
        kernel = flippedKernel;
        int totalRows = input.getRowDimension() + kernel.getRowDimension() - 1;
        int totalCols = input.getColumnDimension() + kernel.getColumnDimension() - 1;
        int rowSize = input.getRowDimension() - kernel.getRowDimension() + 1;
        int colSize = input.getColumnDimension() - kernel.getColumnDimension() + 1;
        int startRows = (totalRows-rowSize)/2;
        int startCols = (totalCols-colSize)/2;
        double[][] in = new double[totalRows][totalCols*2];
        double[][] kern = new double[totalRows][totalCols*2];
        for(int i = 0; i < input.getRowDimension(); i++) {
            for(int j = 0; j < input.getColumnDimension(); j++) {
                in[i][j] = input.get(i,j);
            }
        }
        for(int i = 0; i < kernel.getRowDimension(); i++) {
            for(int j = 0; j < kernel.getColumnDimension(); j++) {
                kern[i][j] = kernel.get(i,j);
            }
        }

        DoubleFFT_2D t = new DoubleFFT_2D(totalRows, totalCols);
        t.realForwardFull(in);
        t.realForwardFull(kern);
        double[][] res = complexMult(in, kern);

        t.complexInverse(res, true);

        Matrix result = new Matrix(rowSize, colSize);
        for(int i = 0; i < rowSize; i++) {
            for(int j = 0; j < colSize; j++) {
                result.set(i,j,res[startRows+i][startCols+j*2]);
            }
        }
        return result;
    }

    private static double[][] complexMult(double[][] a, double[][] b) {
        double[][] res = new double[a.length][a[0].length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[i].length; j+=2) {
                res[i][j] = a[i][j] * b[i][j] - (a[i][j+1] * b[i][j+1]);
                res[i][j+1] = a[i][j] * b[i][j+1] - (a[i][j+1] * b[i][j]);
            }
        }
        return res;
    }

    public static int[] computeResults(Matrix result) {
        int[] results = new int[result.getColumnDimension()];
        double[] current = new double[result.getColumnDimension()];
        for(int j = 0; j < result.getColumnDimension(); j++) {
            for(int k = 0; k < result.getColumnDimension(); k++) {
                if(result.get(0,j) > current[k]) {
                    for(int l = result.getColumnDimension()-1; l > k; l--) {
                        current[l] = current[l-1];
                        results[l] = results[l-1];
                    }
                    current[k] = result.get(0,j);
                    results[k] = j;
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
        Log.d("INPUT", "" + inSampleSize);
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
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(90);
            Bitmap temp = Bitmap.createScaledBitmap(image, height, width, true);
            return Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }
    }

    public static Matrix ZCAWhiten(Matrix input, double epsilon) {
        //DoubleMatrix2D img = flatten(input);
        double mean = 0;
        for(int j = 0; j < input.getColumnDimension(); j++) {
            mean += input.get(0,j);
        }
        mean /= input.getRowDimension()*input.getColumnDimension();
        input.minus(new Matrix(input.getRowDimension(), input.getColumnDimension(), mean));
        Matrix sigma = input.times(input.transpose());
        sigma.arrayTimesEquals(Matrix.identity(sigma.getRowDimension(),sigma.getColumnDimension()));
        SingularValueDecomposition svd = sigma.svd();
        Matrix s = svd.getS();
        for(int i = 0; i < s.getRowDimension(); i++) {
            s.set(i, i, 1/(s.get(i, i)+epsilon));
        }
        Matrix res = svd.getU().times(s).times(svd.getU()).times(input);

        return res;
    }

    public static Matrix flatten(Matrix[] z) {
        Matrix image = new Matrix(1, z.length*z[0].getRowDimension()*z[0].getColumnDimension());
        for(int i = 0; i < z.length; i++) {
            for(int j = 0; j < z[i].getRowDimension(); j++) {
                for(int k = 0; k < z[i].getColumnDimension(); k++) {
                    image.set(0, i*z[i].getRowDimension()*z[i].getColumnDimension()+j*z[i].getColumnDimension()+k,z[i].get(j,k));
                }
            }
        }
        return image;
    }

    public static Matrix[] normalizeData(Matrix[] input) {
        double mean = 0;
        double elements = 0;
        for(Matrix data : input) {
            for(int i = 0; i < data.getRowDimension(); i++) {
                for(int j = 0; j < data.getColumnDimension(); j++) {
                    mean += data.get(i,j);
                    elements++;
                }
            }
        }
        mean /= elements;
        double var = 0;
        for(Matrix data : input) {
            data.minusEquals(new Matrix(data.getRowDimension(), data.getColumnDimension(), mean));
            Matrix squareData = data.arrayTimes(data);
            for(int i = 0; i < squareData.getRowDimension(); i++) {
                for(int j = 0; j < squareData.getColumnDimension(); j++) {
                    var += squareData.get(i,j);
                }
            }
        }
        var /= elements;
        double pstd = 3 * Math.sqrt(var);
        for(Matrix data : input) {
            for(int i = 0; i < data.getRowDimension(); i++) {
                for(int j = 0; j < data.getColumnDimension(); j++) {
                    double x = data.get(i,j);
                    double val = x<pstd?x:pstd;
                    val = val>-pstd?val:-pstd;
                    val /= pstd;
                    data.set(i,j,(val+1)*0.5);
                }
            }
        }

        return input;
    }

        public static Matrix activationFunction(int type, Matrix z, double a) {
            switch(type) {
                case SIGMOID:
                    return sigmoid(z);
                case PRELU:
                    return prelu(z, a);
                case RELU:
                    return relu(z);
                case NONE:
                    return z;
                default:
                    return sigmoid(z);
            }
        }


    public static Matrix sigmoid(Matrix input) {
        for(int i = 0; i < input.getRowDimension(); i++) {
            for(int j = 0; j < input.getColumnDimension(); j++) {
                input.set(i,j,1/(1+Math.exp(-input.get(i,j))));
            }
        }
        return input;
    }

    public static Matrix prelu(Matrix z, double a) {
        //return z.le(0).mul(a-1).add(1).mul(z);
        Matrix res = new Matrix(z.getRowDimension(), z.getColumnDimension());
        for(int i = 0; i < res.getRowDimension(); i++) {
            for(int j = 0; j < res.getColumnDimension(); j++) {
                double k = z.get(i,j);
                res.set(i,j,Math.max(0,k)+a*Math.min(0,k));
            }
        }
        return res;
    }

    public static Matrix relu(Matrix z) {
        return prelu(z, 0);
    }
}

