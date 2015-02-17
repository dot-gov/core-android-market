package org.benews;



import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;


import org.benews.libbsonj.BsonProxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static org.benews.libbsonj.BsonProxy.Sleep;


public class BeNews extends FragmentActivity implements BeNewsFragList.OnFragmentInteractionListener , View.OnClickListener ,BsonProxy.NewsUpdateListener {
	private final static String TAG="BeNews";
	private static Context context;
	private static ProgressBar pb=null;
	private static ArrayList<HashMap<String, String>> newsList;
	ArrayAdapter<HashMap<String,String>> listAdapter;
	private File serializeFolder;
	private final static String serialFile="news_list";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_be_news);
		BitmapHelper.init(getResources().getDisplayMetrics().density);
		context = getApplicationContext();
		final Intent serviceIntent = new Intent(context, PullIntentService.class);
		context.startService(serviceIntent);
    }

	public void setProgressBar(int progress){
		if(pb!=null){
			pb.setProgress(progress);
		}

	}
	boolean toUpdate=false;

	public synchronized boolean isToUpdate() {
		return toUpdate;
	}

	public synchronized void setToUpdate(boolean toUpdate) {
		this.toUpdate = toUpdate;
	}

	@Override
	public synchronized void onNewsUpdate(ArrayList<HashMap<String,String> > list) {
		if(!list.isEmpty()) {
			for ( HashMap<String,String> h: list){
				if (!newsList.contains(h)){
					newsList.add(h);
				}
			}
			try {
				serialise();
				setToUpdate(true);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		//final Button b = ((Button) findViewById(R.id.bt_refresh));
		this.runOnUiThread(new Runnable() {
			public synchronized void run() {
				if (isToUpdate()) {
					listAdapter.notifyDataSetChanged();
					setToUpdate(false);
				}
			}
		});
	}
	@Override
	protected void onStop() {
		super.onStop();
		/* Save db status
		 * release Memory
		 * stop cpu intensive task
		 */
		//Log.d(TAG, "onStop");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(listAdapter != null) {
			listAdapter.notifyDataSetChanged();
		}

	}
	@Override
	protected void onStart() {
		super.onStart();
		if(BsonProxy.self(getAppContext()).isThreadStarted()){
			finishOnStart();
		}

	}
	public synchronized ArrayList<HashMap<String, String>> getList() {
		if(newsList==null && new File(getSerialFile()).exists()) {
			try {
				FileInputStream fis = new FileInputStream(getSerialFile());
				ObjectInputStream is = new ObjectInputStream(fis);
				newsList = (ArrayList<HashMap<String, String>>) is.readObject();
				is.close();
			} catch (Exception e) {
				Log.d(TAG, " (getList):" +e);
				e.printStackTrace();
			}
		}
		if(newsList==null){
			Log.d(TAG, " (getList) initializing list");
			newsList = new ArrayList<HashMap<String, String>>();
		}
		return newsList;
	}


	public String getSerialFile() {
		if (serializeFolder == null ){
			serializeFolder = getAppContext().getFilesDir();
		}
		return serializeFolder.getAbsolutePath()+"/"+serialFile;
	}
	public void setSerializeFolder(File filesDir) {
		this.serializeFolder=filesDir;
	}
	public synchronized  void serialise_list() throws IOException {
		FileOutputStream fos = new FileOutputStream(getSerialFile());
		ObjectOutputStream os = new ObjectOutputStream(fos);
		if (!newsList.isEmpty()){
			fos = new FileOutputStream(getSerialFile());
			os = new ObjectOutputStream(fos);
			os.writeObject(newsList);
			os.close();
		}
	}

	public synchronized  void serialise() throws IOException {
		serialise_list();
	}

	public void finishOnStart(){

			final BsonProxy sucker = BsonProxy.self(getAppContext());
			BeNewsFragList bfl = new BeNewsFragList();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_placeholder, bfl);
			ft.commit();
			newsList = new ArrayList<HashMap<String, String>>(getList());
			listAdapter = new BeNewsArrayAdapter(this, newsList);
			bfl.setListAdapter(listAdapter);
			final Button b = ((Button) findViewById(R.id.bt_refresh));
			b.setOnClickListener(this);
			pb = (ProgressBar) findViewById(R.id.progressBar);
			pb.setProgress(0);
			pb.setMax(100);
			sucker.setOnNewsUpdateListener(this);
			setToUpdate(true);

	}

	@Override
	protected void onResume() {
		super.onResume();
		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(new IntentFilter(BsonProxy.READY)));
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister since the activity is not visible
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	// handler for received Intents for the "my-event" event
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
			String message = intent.getStringExtra("message");
			Log.d("receiver", "Got message: " + message);
			finishOnStart();
		}
	};
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu
				.be_news_menu, menu);
		return true;
	}
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onItemPress(int position) {
		try {
			Object o = listAdapter.getItem(position);
			String keyword = o.toString();
			Toast.makeText(this, "You selected: " + keyword, Toast.LENGTH_SHORT).show();
			DetailFragView details = DetailFragView.newInstance((HashMap<String, String>) o);
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_placeholder, details);
			//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.addToBackStack("DETAILS");
			ft.commit();

		}catch (Exception e){
			Log.d(TAG,"Exception:" + e);
		}
	}



	@Override
	public void onBackPressed() {
		if (getSupportFragmentManager().findFragmentById(R.id.detail_image) != null) {
			// I'm viewing Fragment C
			getSupportFragmentManager().popBackStack("DETAILS",
					FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} else {
			super.onBackPressed();
		}
	}

	public static Context getAppContext(){
		return BeNews.context;
	}

	@Override
	public void onClick(View view) {
		// Start lengthy operation in a background thread
		final Button button = (Button)view;
		final BsonProxy sucker = BsonProxy.self(getAppContext());
		button.setEnabled(false);
		sucker.setRun(false);
		int i = 0;
		new Thread(new Runnable() {
			public void run() {
				int i = 0;
				setProgressBar(i);
				while (sucker.isRunning()) {

					Sleep(1);
					i += 1;
					if((i <= 100)) {
						setProgressBar(i);
					}
				}
				sucker.reset_news();
			}
		}).start();
	}
}
