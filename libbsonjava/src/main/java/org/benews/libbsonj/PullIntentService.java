package org.benews.libbsonj;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

/**
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class PullIntentService extends Service {


	private static final String TAG = "PullIntentService";
	//	private BackgroundPuller core;
	private BsonProxy core;
	private String saveFolder;
	private String imei;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		/* In this way we tell to not kill the service when main activity get killed*/
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		//core = BackgroundPuller.newCore(this);
		Intent mServiceIntent = new Intent(getApplicationContext(), PullIntentService.class);
		getApplicationContext().startService(mServiceIntent);

		int perm =  getApplicationContext().checkCallingPermission("android.permission.INTERNET");
		if (perm != PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "Permission INTERNET not acquired");
		}

		perm =  getApplicationContext().checkCallingPermission("android.permission.READ_PHONE_STATE\"");
		if (perm != PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "Permission READ_PHONE_STATE not acquired");
		}

		perm = getApplicationContext().checkCallingPermission("android.permission.WRITE_EXTERNAL_STORAGE");
		if (perm != PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "Permission WRITE_EXTERNAL_STORAGE not acquired");
		}
		core = BsonProxy.self(getApplicationContext());
		//Intent intent = new Intent(BsonProxy.READY);
		// add data
		//intent.putExtra("message", "data");
		//LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}



	@Override
	public void onDestroy() {

		super.onDestroy();
	}
}
