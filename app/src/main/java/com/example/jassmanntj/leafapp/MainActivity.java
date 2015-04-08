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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;

public class MainActivity extends ActionBarActivity {
	DeviceCNN cnn;
	DeviceImageLoader loader;
	AssetManager am;
    HashMap<Integer, String> labelMap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_GALLERY = 100;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		am = getAssets();
        loader = new DeviceImageLoader(am);
        DeviceConvolutionLayer[] cl = {new DeviceConvolutionLayer(am, "16-Cross-5-4-200.0Layer0.layer")};
		DeviceSparseAutoencoder[] sae = {new DeviceSparseAutoencoder(am, "16-Cross-5-4-200.0Layer1.layer0")};
		DeviceSoftmaxClassifier sc = new DeviceSoftmaxClassifier(am, "16-Cross-5-4-200.0Layer1.layer1");
        this.labelMap = new HashMap<Integer, String>();
        try {
            InputStream is = am.open("LabelMap");
            @SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String data = reader.readLine();
            while(data != null) {
                String[] split = data.split(":");
                labelMap.put(Integer.parseInt(split[0]),split[1]);
                data = reader.readLine();
            }
        }

        catch(IOException e) {
            e.printStackTrace();
        }
		cnn = new DeviceCNN(cl, sae, sc);
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

    private class DoCalculationTask extends AsyncTask<Void, Void, String[] > {
        Intent data;
        int resultCode;
        int requestCode;

        public DoCalculationTask(Intent data, int resultCode, int requestCode) {
            this.data = data;
            this.resultCode = resultCode;
            this.requestCode = requestCode;
        }
        @Override
        protected String[] doInBackground(Void... arg0) {
            String[] results = new String[6];
            Bitmap img = null;
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_IMAGE_GALLERY:
                        try {
                            Uri selectedImage = data.getData();
                            InputStream imageStream1 = getContentResolver().openInputStream(selectedImage);
                            InputStream imageStream2 = getContentResolver().openInputStream(selectedImage);
                            long start = System.currentTimeMillis();
                            img = DeviceUtils.decodeStream(imageStream1, imageStream2, 60, 80);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_IMAGE_CAPTURE:
                        Bundle extras = data.getExtras();
                        img = (Bitmap) extras.get("data");
                        img = DeviceUtils.scaleImage(img, 60, 80);
                        break;
                }

                try {
                    DoubleMatrix2D[] image = loadImage(3, 60, 80, img);
                    img.recycle();
                    long timeLoad = System.currentTimeMillis();
                    DoubleMatrix2D result = cnn.compute(image);
                    image = null;
                    int[] res = DeviceUtils.computeResults(result);
                    results[0] = labelMap.get(res[0]);
                    results[1] = labelMap.get(res[1]);
                    results[2] = labelMap.get(res[2]);
                    results[3] = String.format("%.2f%%", result.get(0, res[0]) * 100 - 0.005);
                    results[4] = String.format("%.2f%%", result.get(0, res[1]) * 100 - 0.005);
                    results[5] = String.format("%.2f%%", result.get(0, res[2]) * 100 - 0.005);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return results;
        }

        protected void onPostExecute(String[] result) {
            Button b1 = (Button) findViewById(R.id.camera_button);
            Button b2 = (Button) findViewById(R.id.gallery_button);
            TextView loading = (TextView) findViewById(R.id.loading_text);
            GridLayout results = (GridLayout)findViewById(R.id.results);
            ((TextView)findViewById(R.id.res_1)).setText(result[0]);
            ((TextView)findViewById(R.id.res_2)).setText(result[1]);
            ((TextView)findViewById(R.id.res_3)).setText(result[2]);
            ((TextView)findViewById(R.id.res_p_1)).setText(result[3]);
            ((TextView)findViewById(R.id.res_p_2)).setText(result[4]);
            ((TextView)findViewById(R.id.res_p_3)).setText(result[5]);
            try {
                Bitmap img = BitmapFactory.decodeStream(getAssets().open("20140420_174214s.jpg"));
                ((ImageView)findViewById(R.id.res_img_1)).setImageBitmap(img);
                ((ImageView)findViewById(R.id.res_img_2)).setImageBitmap(img);
                ((ImageView)findViewById(R.id.res_img_3)).setImageBitmap(img);
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
        GridLayout results = (GridLayout)findViewById(R.id.results);
        loading.setText(R.string.loading);
        results.setVisibility(View.GONE);
        banner.setVisibility(View.GONE);
        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        String[] res = new String[3];
        AsyncTask<Void, Void, String[]> t = new DoCalculationTask(data, resultCode, requestCode);
        t.execute();
        /*Bitmap img = null;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_GALLERY:
                    try {
                        Uri selectedImage = data.getData();
                        InputStream imageStream1 = getContentResolver().openInputStream(selectedImage);
                        InputStream imageStream2 = getContentResolver().openInputStream(selectedImage);
                        long start = System.currentTimeMillis();
                        img = DeviceUtils.decodeStream(imageStream1, imageStream2, 60, 80);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    Bundle extras = data.getExtras();
                    img = (Bitmap) extras.get("data");
                    img = DeviceUtils.scaleImage(img, 60, 80);
                    break;
            }

            try {
                DoubleMatrix2D[] image = loadImage(3, 60, 80, img);
                img.recycle();
                long timeLoad = System.currentTimeMillis();
                DoubleMatrix2D result = cnn.compute(image);
                image = null;
                int[] res = DeviceUtils.computeResults(result);
                TextView res1 = (TextView)findViewById(R.id.res_1);
                TextView res2 = (TextView)findViewById(R.id.res_2);
                TextView res3 = (TextView)findViewById(R.id.res_3);
                res1.setText(String.format(" %s: %.2f%%", labelMap.get(res[0]), result.get(0, res[0]) * 100 - 0.005));
                res2.setText(String.format(" %s: %.2f%%", labelMap.get(res[1]), result.get(0, res[1]) * 100 - 0.005));
                res3.setText(String.format(" %s: %.2f%%", labelMap.get(res[2]), result.get(0, res[2]) * 100 - 0.005));
                loading.setText(R.string.none);
                results.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
                loading.setText(R.string.error);
            } finally {
                b1.setVisibility(View.VISIBLE);
                b2.setVisibility(View.VISIBLE);
            }
        }*/
    }


    public DoubleMatrix2D[] loadImage(int channels, int width, int height, Bitmap img) throws IOException {
        // get input stream
        int[] pixels = new int[width*height];
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
        DoubleMatrix2D[] image = {  new DenseDoubleMatrix2D(img.getHeight(), img.getWidth()),
                                    new DenseDoubleMatrix2D(img.getHeight(), img.getWidth()),
                                    new DenseDoubleMatrix2D(img.getHeight(), img.getWidth())};
        for(int i = 0; i < img.getHeight(); i++) {
            for(int j = 0; j < img.getWidth(); j++) {
                for(int k = 0; k < channels; k++) {
                    image[k].set(i,j, ((pixels[j*img.getHeight()+i] >>> (8 * k)) & 0xFF));
                }
            }
        }
        image = DeviceUtils.normalizeData(image);
        for(int i = 0; i < channels; i++) {
            Log.d("XXX", image[i].toString());
        }

        return image;
    }

}
