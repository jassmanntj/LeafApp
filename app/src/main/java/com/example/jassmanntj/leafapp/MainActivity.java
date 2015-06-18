package com.example.jassmanntj.leafapp;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.HashMap;

import Jama.Matrix;
import device.DeviceNeuralNetwork;
import device.DeviceUtils;

/**
 * LoadingUtils - utilities for loading and scaling an image on a device
 *
 * @author Timothy Jassmann
 * @version 06/17/2015
 */
public class MainActivity extends ActionBarActivity {
    DeviceNeuralNetwork cnn;
    //DeviceImageLoader loader;
    AssetManager am;
    HashMap<Integer, String> labelMap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_GALLERY = 100;
    private Uri selectedImage;
    private int width = 60;
    private int height = 80;
    private String networkFile = "nnF0";

    /**
     * dispatchGalleryIntent - starts up gallery to select image
     * @param view not used
     */
    public void dispatchGalleryIntent(View view) {
        Intent photoPicker = new Intent(Intent.ACTION_PICK);
        photoPicker.setType("image/*");
        startActivityForResult(photoPicker, 100);
    }

    /**
     * dispatchCameraIntent - starts up camera to take picture
     * @param view not used
     */
    public void dispatchCameraIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * onActivityResult - sets buttons to invisible and starts calculation task
     *
     * @param requestCode request camera or gallery
     * @param resultCode RESULT_OK if image successfully received
     * @param data data to load and calculate
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("RESULT", requestCode+"::"+resultCode);
        Button b1 = (Button) findViewById(R.id.camera_button);
        Button b2 = (Button) findViewById(R.id.gallery_button);
        TextView banner = (TextView) findViewById(R.id.banner);
        TextView loading = (TextView) findViewById(R.id.loading_text);
        GridLayout results = (GridLayout) findViewById(R.id.results);
        loading.setText(R.string.loading);
        results.setVisibility(View.INVISIBLE);
        banner.setVisibility(View.INVISIBLE);
        b1.setVisibility(View.INVISIBLE);
        b2.setVisibility(View.INVISIBLE);
        AsyncTask<Void, Void, double[]> t = new DoCalculationTask(data, resultCode);
        t.execute();
    }

    /**
     * onCreate - load network and setup ui on create
     *
     * @param savedInstanceState default param
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CREATE", "CREATE");
        setContentView(R.layout.activity_main);
        this.labelMap = new HashMap<Integer, String>();
        Button b1 = (Button) findViewById(R.id.camera_button);
        Button b2 = (Button) findViewById(R.id.gallery_button);
        TextView banner = (TextView) findViewById(R.id.banner);
        TextView loading = (TextView) findViewById(R.id.loading_text);
        GridLayout results = (GridLayout) findViewById(R.id.results);
        loading.setText(R.string.loading);
        results.setVisibility(View.INVISIBLE);
        banner.setVisibility(View.INVISIBLE);
        b1.setVisibility(View.INVISIBLE);
        b2.setVisibility(View.INVISIBLE);
        AsyncTask<Void, Void, Void> t = new DoLoadTask(labelMap);
        t.execute();
    }

    /**
     * onCreateOptionsMenu - default method
     *
     * @param menu default param
     *
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * onOptionsItemSelected - default method
     *
     * @param item default param
     *
     * @return default
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * onRestoreInstanceState - do nothing
     * @param savedInstanceState not used
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    /**
     * onSaveInstanceState - do nothing
     * @param outState not used
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    /**
     * loadImage - loads a bitmap's pixel data into a Jama Matrix
     *
     * @param channels channels in image
     * @param img bitmap of image
     *
     * @return Matrix array containing pixel data for image split by channel
     */
    private Matrix[] loadImage(int channels, Bitmap img) {
        // get input stream
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
        Matrix image = new Matrix(1, img.getHeight() * img.getWidth() * channels);
        Matrix[] res = new Matrix[channels];
        for (int i = 0; i < channels; i++) {
            res[i] = new Matrix(img.getHeight(), img.getWidth());
        }
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                for (int k = 0; k < channels; k++) {
                    res[k].set(i, j, ((pixels[i * img.getWidth() + j] >>> (8 * k)) & 0xFF));
                }
            }
        }
        return DeviceUtils.normalizeData(res);
    }

    /**
     * DoCalculationTask - Class for threading the execution of the network
     */
    private class DoCalculationTask extends AsyncTask<Void, Void, double[]> {
        Intent data;
        int resultCode;

        /**
         * DoCalculationTask - constructor for class
         *
         * @param data data to load and calculate
         * @param resultCode RESULT_OK if image successfully received
         */
        public DoCalculationTask(Intent data, int resultCode) {
            this.data = data;
            this.resultCode = resultCode;
        }

        /**
         * doInBackground - loads image and computes the results
         * @param arg0 not used
         * @return array of results
         */
        @Override
        protected double[] doInBackground(Void... arg0) {

            long calcstart = System.currentTimeMillis();
            double[] results = new double[6];
            Bitmap img = null;
            if (resultCode == RESULT_OK) {
                try {
                    selectedImage = data.getData();
                    InputStream imageStream1 = getContentResolver().openInputStream(selectedImage);
                    InputStream imageStream2 = getContentResolver().openInputStream(selectedImage);
                    img = LoadingUtils.decodeStream(imageStream1, imageStream2, width, height);
                    Matrix[] image = loadImage(3, img);
                    img.recycle();
                    Matrix result = cnn.compute(image);
                    image = null;
                    int[] res = DeviceUtils.computeResults(result);
                    results[0] = res[0];
                    results[1] = res[1];
                    results[2] = res[2];
                    results[3] = result.get(0, res[0]) * 100 - 0.005; //Subtract 0.005 for rounding
                    results[4] = result.get(0, res[1]) * 100 - 0.005;
                    results[5] = result.get(0, res[2]) * 100 - 0.005;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long calcend = System.currentTimeMillis();
            Log.d("Times", "CALC: "+((calcend-calcstart)/1000.0));
            return results;
        }

        /**
         * onPostExecute - updates display to display results
         *
         * @param result results passed from doInBackground
         */
        protected void onPostExecute(double[] result) {
            Button b1 = (Button) findViewById(R.id.camera_button);
            Button b2 = (Button) findViewById(R.id.gallery_button);
            TextView loading = (TextView) findViewById(R.id.loading_text);
            GridLayout results = (GridLayout) findViewById(R.id.results);
            String res1 = labelMap.get((int) result[0]);
            String res2 = labelMap.get((int) result[1]);
            String res3 = labelMap.get((int) result[2]);
            ((TextView) findViewById(R.id.res_1)).setText(res1);
            ((TextView) findViewById(R.id.res_2)).setText(res2);
            ((TextView) findViewById(R.id.res_3)).setText(res3);
            ((TextView)findViewById(R.id.res_p_1)).setText(String.format("%.2f%%",result[3]));
            ((TextView)findViewById(R.id.res_p_2)).setText(String.format("%.2f%%",result[4]));
            ((TextView)findViewById(R.id.res_p_3)).setText(String.format("%.2f%%",result[5]));
            try {
                ((ImageView) findViewById(R.id.res_img_1)).setImageBitmap(BitmapFactory.decodeStream(getAssets().open(res1 + ".jpg")));
                ((ImageView) findViewById(R.id.res_img_2)).setImageBitmap(BitmapFactory.decodeStream(getAssets().open(res2 + ".jpg")));
                ((ImageView) findViewById(R.id.res_img_3)).setImageBitmap(BitmapFactory.decodeStream(getAssets().open(res3 + ".jpg")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            loading.setText(R.string.none);
            results.setVisibility(View.VISIBLE);
            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.VISIBLE);
        }
    }

    /**
     * DoLoadTask - Class for threading of loading of the network
     */
    private class DoLoadTask extends AsyncTask<Void, Void, Void> {
        private HashMap<Integer, String> labelMap;

        /**
         * DoLoadTask - Constructor for DoLoadTask
         *
         * @param labelMap HashMap to load mapping of integer classifications to string classifications
         */
        public DoLoadTask(HashMap<Integer, String> labelMap) {
            this.labelMap = labelMap;
        }

        /**
         * doInBackground - loads network values
         *
         * @param arg0 not used
         *
         * @return null
         */
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                long loadstart = System.currentTimeMillis();
                am = getAssets();
                ObjectInputStream in = new ObjectInputStream(am.open(networkFile));
                cnn = (DeviceNeuralNetwork) in.readObject();
                InputStream is = am.open("LabelMap");
                @SuppressWarnings("resource")
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String data = reader.readLine();
                while (data != null) {
                    String[] split = data.split(":");
                    labelMap.put(Integer.parseInt(split[0]), split[1]);
                    data = reader.readLine();
                }
                long loadend = System.currentTimeMillis();
                Log.d("Times", "LOAD: "+((loadend-loadstart)/1000.0));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * onPostExecute - sets visibility of buttons to visible and removes loading text
         *
         * @param v not used
         */
        protected void onPostExecute(Void v) {
            Log.d("POST", "POST");
            Button b1 = (Button) findViewById(R.id.camera_button);
            Button b2 = (Button) findViewById(R.id.gallery_button);
            TextView loading = (TextView) findViewById(R.id.loading_text);
            loading.setText(R.string.none);
            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.VISIBLE);
        }
    }

}

