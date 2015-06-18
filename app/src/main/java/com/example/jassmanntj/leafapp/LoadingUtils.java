package com.example.jassmanntj.leafapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

/**
 * LoadingUtils - utilities for loading and scaling an image on a device
 *
 * @author Timothy Jassmann
 * @version 06/17/2015
 */
public abstract class LoadingUtils {

    /**
     * decodeStream - returns a scaled image from input stream
     *
     * @param is1 one input stream pointing to data
     * @param is2 another input stream pointing to same data
     * @param width width to scale images to
     * @param height height to scale images to
     *
     * @return scaled image from input stream
     */
    public static Bitmap decodeStream(InputStream is1, InputStream is2, int width, int height) {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is1, null, o);
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = calculateInSampleSize(o, o.outWidth, o.outHeight);
        o2.inJustDecodeBounds = false;
        Bitmap img = BitmapFactory.decodeStream(is2, null, o2);
        return scaleImage(img, width, height);

    }

    /**
     * scaleImage - scales an bitmap image to the specified width and height
     *
     * @param image bitmap to scale
     * @param width width to scale images to
     * @param height height to scale images to
     *
     * @return scaled bitmap
     */
    public static Bitmap scaleImage(Bitmap image, int width, int height) {
        if((image.getHeight() < image.getWidth() && height < width)
                || (image.getHeight() > image.getWidth() && height > width)) {
            return Bitmap.createScaledBitmap(image, width, height, true);
        }
        else {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(90);
            Bitmap temp = Bitmap.createScaledBitmap(image, height, width, true);
            return Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }
    }

    /**
     * calculateInSampleSize - calculates size to sample image down to
     *
     * @param options options containing image width and height info
     * @param reqWidth width image is being scaled down to
     * @param reqHeight height image is being scaled down to
     *
     * @return power of 2 that image can easily be scaled by
     */
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
        return inSampleSize;
    }
}
