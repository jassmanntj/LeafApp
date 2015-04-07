package com.example.jassmanntj.leafapp;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
                String message = "Results:";
                for (int i = 0; i < res.length; i++) {
                    message += String.format("\n%s: %.2f%%", labelMap.get(res[i]), result.get(0, res[i]) * 100 - 0.005);
                }
                Log.d("MESSAGE", message);
                TextView textView = new TextView(this);
                textView.setTextSize(20);
                textView.setText(message);
                // Set the text view as the activity layout
                setContentView(textView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
