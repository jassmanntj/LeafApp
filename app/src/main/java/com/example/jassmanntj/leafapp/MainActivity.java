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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.HashMap;

import Jama.Matrix;
import mobile.DeviceNeuralNetwork;
import mobile.DeviceUtils;

/**
 * Created by jassmanntj on 4/13/2015.
 */
public class MainActivity extends ActionBarActivity {
    DeviceNeuralNetwork cnn;
    //DeviceImageLoader loader;
    AssetManager am;
    HashMap<Integer, String> labelMap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_GALLERY = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.labelMap = new HashMap<Integer, String>();
        Button b1 = (Button) findViewById(R.id.camera_button);
        Button b2 = (Button) findViewById(R.id.gallery_button);
        TextView banner = (TextView) findViewById(R.id.banner);
        TextView loading = (TextView) findViewById(R.id.loading_text);
        GridLayout results = (GridLayout) findViewById(R.id.results);
        loading.setText(R.string.loading);
        results.setVisibility(View.GONE);
        banner.setVisibility(View.GONE);
        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        AsyncTask<Void, Void, Void> t = new DoLoadTask(labelMap);
        t.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

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

    public void run(View view) {
        Intent photoPicker = new Intent(Intent.ACTION_PICK);
        photoPicker.setType("image/*");
        startActivityForResult(photoPicker, 100);
    }

    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private class DoLoadTask extends AsyncTask<Void, Void, Void> {
        private HashMap<Integer, String> labelMap;
        public DoLoadTask(HashMap<Integer, String> labelMap) {
            this.labelMap = labelMap;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                long loadstart = System.currentTimeMillis();
                am = getAssets();
                ObjectInputStream in = new ObjectInputStream(am.open("TestNNb0"));
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
            catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

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

    private class DoCalculationTask extends AsyncTask<Void, Void, double[]> {
        Intent data;
        int resultCode;
        int requestCode;

        public DoCalculationTask(Intent data, int resultCode, int requestCode) {
            this.data = data;
            this.resultCode = resultCode;
            this.requestCode = requestCode;
        }

        @Override
        protected double[] doInBackground(Void... arg0) {
            long calcstart = System.currentTimeMillis();
            double[] results = new double[6];
            Bitmap img = null;
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_IMAGE_GALLERY:
                        try {
                            Uri selectedImage = data.getData();
                            InputStream imageStream1 = getContentResolver().openInputStream(selectedImage);
                            InputStream imageStream2 = getContentResolver().openInputStream(selectedImage);
                            long start = System.currentTimeMillis();
                            img = LoadingUtils.decodeStream(imageStream1, imageStream2, 60, 80);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_IMAGE_CAPTURE:
                        Bundle extras = data.getExtras();
                        img = (Bitmap) extras.get("data");
                        img = LoadingUtils.scaleImage(img, 60, 80);
                        break;
                }

                try {
                    Matrix[] image = loadImage(3, 60, 80, img);
                    img.recycle();
                    long timeLoad = System.currentTimeMillis();
                    Matrix result = cnn.compute(image);
                    image = null;
                    int[] res = DeviceUtils.computeResults(result);
                    results[0] = res[0];
                    results[1] = res[1];
                    results[2] = res[2];
                    results[3] = result.get(0, res[0]) * 100 - 0.005;
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

        protected void onPostExecute(double[] result) {
            Button b1 = (Button) findViewById(R.id.camera_button);
            Button b2 = (Button) findViewById(R.id.gallery_button);
            TextView loading = (TextView) findViewById(R.id.loading_text);
            GridLayout results = (GridLayout) findViewById(R.id.results);
            Log.d("RES0", ""+result[0]);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Button b1 = (Button) findViewById(R.id.camera_button);
        Button b2 = (Button) findViewById(R.id.gallery_button);
        TextView banner = (TextView) findViewById(R.id.banner);
        TextView loading = (TextView) findViewById(R.id.loading_text);
        GridLayout results = (GridLayout) findViewById(R.id.results);
        loading.setText(R.string.loading);
        results.setVisibility(View.GONE);
        banner.setVisibility(View.GONE);
        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        AsyncTask<Void, Void, double[]> t = new DoCalculationTask(data, resultCode, requestCode);
        t.execute();
    }

    private String getString(Matrix mat) {
        String s = "";
        for(int i = 0; i < mat.getRowDimension(); i++) {
            for(int j = 0; j < mat.getColumnDimension(); j++) {
                s += mat.get(i,j);
            }
        }
        return s;
    }


    public Matrix[] loadImage(int channels, int width, int height, Bitmap img) throws IOException {
        // get input stream
        int[] pixels = new int[width * height];
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
        Matrix image = new Matrix(1, img.getHeight() * img.getWidth() * channels);
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                for (int k = 0; k < channels; k++) {
                    image.set(0, k * img.getHeight() * img.getWidth() + i * img.getWidth() + j, ((pixels[j * img.getHeight() + i] >>> (8 * k)) & 0xFF));
                }
            }
        }
        image = DeviceUtils.ZCAWhiten(image, 1e-5);

        Matrix[] res = new Matrix[channels];
        for (int i = 0; i < channels; i++) {
            res[i] = new Matrix(img.getHeight(), img.getWidth());
        }
        for (int i = 0; i < channels; i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                for (int k = 0; k < img.getWidth(); k++) {
                    res[i].set(j, k, image.get(0, i * img.getHeight() * img.getWidth() + j * img.getWidth() + k));
                }
            }
        }
        return res;
    }
}

