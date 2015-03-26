package com.example.jassmanntj.leafapp;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
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
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		DeviceConvolutionLayer cl = null;
		am = getAssets();
		cl = new DeviceConvolutionLayer(am, "16-Cross-5-4-200.0Layer0.layer");
		DeviceSparseAutoencoder sae = new DeviceSparseAutoencoder(am, "16-Cross-5-4-200.0Layer1.layer0");
		DeviceSoftmaxClassifier sc = new DeviceSoftmaxClassifier(am, "16-Cross-5-4-200.0Layer1.layer1");
		DeviceNeuralNetworkLayer[] nnl = {cl, sae, sc};
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
		cnn = new DeviceCNN(nnl);
		loader = new DeviceImageLoader(am);

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
	
	public void run(View view) throws Exception {
		long start = System.currentTimeMillis();
		loader.loadFolder(3, 80, 60);
		DenseDoubleMatrix2D image = loader.getImages();
        Log.d("INPUT", image.toString());
		long timeLoad = System.currentTimeMillis();
		DoubleMatrix2D result = cnn.compute(image);
        Log.d("RESULTD",result.toString());
        int[] res = DeviceUtils.computeResults(result);
        String message = "Results:";
        for(int i = 0; i < res.length; i++) {
            message += String.format("\n%s: %.2f%%", labelMap.get(res[i]), result.get(0,res[i])*100-0.005);
		}
		TextView textView = new TextView(this);
	    textView.setTextSize(20);
	    textView.setText(message);
	    // Set the text view as the activity layout
	    setContentView(textView);

	}
}
