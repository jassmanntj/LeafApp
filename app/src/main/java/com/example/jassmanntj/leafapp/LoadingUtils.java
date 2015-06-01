package com.example.jassmanntj.leafapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Created by jassmanntj on 5/31/2015.
 */
public class LoadingUtils {
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
}
