package org.benews.libbsonj;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;


import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;




public class BsonProxy extends Application implements Runnable{
	private final static String TAG="BsonProxy";

	public static final String HASH_FIELD_TYPE = "type";
	public static final String HASH_FIELD_PATH = "path";
	public static final String HASH_FIELD_TITLE = "title";
	public static final String HASH_FIELD_SUBJECT = "subject";
	public static final String HASH_FIELD_DATE = "date";
	public static final String HASH_FIELD_HEADLINE = "headline";
	public static final String HASH_FIELD_CONTENT = "content";
	public static final String TYPE_TEXT_DIR = "text";
	public static final String TYPE_AUDIO_DIR = "audio";
	public static final String TYPE_VIDEO_DIR = "video";
	public static final String TYPE_IMG_DIR = "img";
	public static final String TYPE_HTML_DIR = "html";
	public static final String HASH_FIELD_CHECKSUM = "checksum";
	
	public static final int HASH_FIELD_TYPE_POS = 4;
	public static final int HASH_FIELD_PATH_POS = 5;
	public static final int HASH_FIELD_TITLE_POS = 1;
	public static final int HASH_FIELD_DATE_POS = 0;
	public static final int HASH_FIELD_HEADLINE_POS = 2;
	public static final int HASH_FIELD_CONTENT_POS = 3;
	public static final int HASH_FIELD_IMEI_POS = 6;
	public static final int HASH_FIELD_TRIALS_POS = 7;
	public static final int HASH_FIELD_SUBJECT_POS = 8;

	public static final SimpleDateFormat dateFormatter=new SimpleDateFormat("dd/MM/yyyy hh:mm");

	private final static String serialFile=".news";
	private final static String serialFileTs=".ts";
	public static final String READY = "upAndRunning";

	private static boolean serviceRunning = false;
	static int news_n=0;
	HashMap<String,String>typesMap;



	private Thread coreThread;
	private boolean run = false;
	static private SocketAsyncTask runningTask=null;
	private ArrayList<HashMap<String,String> > list;

	private String dumpFolder=null;
	private String imei=null;
	HashMap<String,String> args_for_bkg = new HashMap<String, String>();
	private File serializeFolder;
	private Socket socket;
	private boolean noData=false;
	private SocketFactory sf = null;
	private Context appContext = null;
	private Certificate certificate;
	private long lastTs=0l;


	public interface NewsUpdateListener
	{
		void onNewsUpdate(ArrayList<HashMap<String,String> > list);
	}


	ArrayList<NewsUpdateListener> listeners = new ArrayList<NewsUpdateListener> ();

	public void setOnNewsUpdateListener (NewsUpdateListener listener)
	{
		// Store the listener object
		this.listeners.add(listener);
	}
	public boolean isThreadStarted(){

		return (coreThread!=null && coreThread.isAlive());
	}
	public String getSerialFile() {

		if(serializeFolder != null) {
			return serializeFolder.getAbsolutePath() + "/" + serialFile;
		}
		return null;
	}
	public String getSerialFileTs() {
		return serializeFolder.getAbsolutePath()+"/"+serialFileTs;
	}

	private void Core() {

	}

	static BsonProxy singleton;
	public synchronized void reset_news(){

		list.clear();
		try {
			serialise();
		} catch (Exception e) {
			Log.d(TAG, " (setStop):" + e);
		}
		updateListeners();
		Sleep(1);
		Log.d(TAG, " (reset_news):Done");
		noData=false;
		args_for_bkg.put(HASH_FIELD_DATE, "0");
	}
	public synchronized  void serialise_list() throws IOException {
		if( list!= null && getSerialFile()!=null) {
			FileOutputStream fos = new FileOutputStream(getSerialFile());
			ObjectOutputStream os = new ObjectOutputStream(fos);
			if (!list.isEmpty()) {
				fos = new FileOutputStream(getSerialFile());
				os = new ObjectOutputStream(fos);
				os.writeObject(list);
				os.close();
			}
		}
	}

	public synchronized  void serialise() throws IOException {
		serialise_list();
	}

	public void setRun(boolean run) {
		if(run == false){
			if(socket!=null){
				new Thread(new Runnable() {
					public void run() {
						try {
							socket.close();
						} catch (Exception e){
							e.printStackTrace();
						}
					}
				}).start();


			}
		}
		this.run = run;
	}
	public static final SimpleDateFormat dateFormatter_raw=new SimpleDateFormat("dd-MM-yyyy hh:mm");
	private static String validateDate(String date){
		if(date.isEmpty()){
			return null;
		}
		try{
			Date d = dateFormatter_raw.parse(date);
			 return String.valueOf(d.getTime()/1000l);
		} catch (ParseException e) {
			Log.w(TAG,"line not human readable format : dd-MM-yyyy hh:mm");
			try {
				long i =  Long.parseLong(date);
				return  date;
			}catch (NumberFormatException en){
				Log.w(TAG,"line not a number");
			}
		}
		return null;
	}
	private static byte[] streamDecodeWrite(final String dest, InputStream in) {
		byte[] digest = null;
		try {
			MessageDigest md5 = null;

			final FileOutputStream out = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			try {
				md5 = MessageDigest.getInstance("MD5");
				DigestInputStream dis = new DigestInputStream(in, md5);
				digest = md5.digest();
			}catch (NoSuchAlgorithmException na){
				Log.w(TAG, " (streamDecodeWrite): MD5 not available using objid");
				digest = String.valueOf(out.hashCode()).getBytes();
			}
			out.close();
			return digest;
		} catch (Exception ex) {
			Log.w(TAG, " (streamDecodeWrite): exception while saving file: " + ex);
		}
		return null;
	}

	/**
	 * Creates the directory.
	 *
	 * @param dir the dir
	 * @return true, if successful
	 */
	public static boolean createDirectory(final String dir) {
		final File file = new File(dir);
		file.mkdirs();
		return file.exists() && file.isDirectory();
	}
	public static byte[] dumpAsset(InputStream stream, String filename) {
		if(filename != null) {
			String[] tokens = filename.split("\\.(?=[^\\.]+$)");
			if(tokens.length == 2){
				if(createDirectory(tokens[0])==false){
					Log.w(TAG,"(dumpAsset) fail to create directory <" + tokens[0] + ">");
				}
			}
		}
		return streamDecodeWrite(filename, stream);
	}
	public synchronized static BsonProxy self(Context appContext) {
		if (singleton == null) {
			Log.d(TAG, " (self): initializing");
			singleton = new BsonProxy();
			singleton.typesMap = new HashMap<String, String>();
			singleton.typesMap.put("1", "txt");
			singleton.typesMap.put("2", "audio");
			singleton.typesMap.put("3", "video");
			singleton.typesMap.put("4", "img");
			singleton.typesMap.put("5", "html");
			if (appContext != null) {
				Log.d(TAG, " (self): initializing context");
				singleton.appContext = appContext;
				singleton.setSerializeFolder(appContext.getFilesDir());
				singleton.list = singleton.getList();
				try {
					PackageManager m = singleton.appContext.getPackageManager();
					String s = singleton.appContext.getPackageName();
					PackageInfo p = m.getPackageInfo(s, 0);
					singleton.setDumpFolder(p.applicationInfo.dataDir);
					TelephonyManager telephonyManager = ((TelephonyManager) singleton.appContext.getSystemService(Context.TELEPHONY_SERVICE));
					singleton.setImei(telephonyManager.getDeviceId());
					singleton.Start();
				} catch (PackageManager.NameNotFoundException e) {
					Log.w(TAG, "Error Package name not found ", e);
					singleton = null;
					return null;
				}
			} else {
				Log.d(TAG, " (self): skypping init");
			}
		}
		return singleton;
	}

	private static HashMap<String, String> get_news() {
		if(singleton!=null && singleton.appContext == null){
			return null;
		}
		AssetManager assets = singleton.appContext.getAssets();
		if (assets != null) {
				// todo:check loading correctness
				BufferedReader br = null;
				String line = "";
				String cvsSplitBy = "\\|";

				try {
					br = new BufferedReader(new InputStreamReader(assets.open("server.set")));
					while ((line = br.readLine()) != null) {

						if( line.isEmpty() || line.startsWith("#") || line.trim().isEmpty()){
							Log.w(TAG, "Empty line or comment: <" + line + ">");
							continue;
						}
						// use pipe as separator

						String[] fields = line.split(cvsSplitBy,15);
						String news_date;
						if (fields.length > 8 && (news_date=validateDate(fields[BsonProxy.HASH_FIELD_DATE_POS]))!=null) {
							HashMap<String, String> news = new HashMap<String, String>();
							long now = (new Date()).getTime()/1000l;
							long new_time = Long.parseLong(news_date);
							if (now < new_time){
								Log.w(TAG,"news time in the future skip this one ");
								continue;
							}
							if (singleton.lastTs >=new_time ){
								Log.w(TAG,"news already read ");
								continue;
							}
							news.put(BsonProxy.HASH_FIELD_DATE, news_date);
							news.put(BsonProxy.HASH_FIELD_TITLE, fields[BsonProxy.HASH_FIELD_TITLE_POS]);
							news.put(BsonProxy.HASH_FIELD_CONTENT, fields[BsonProxy.HASH_FIELD_CONTENT_POS]);
							news.put(BsonProxy.HASH_FIELD_HEADLINE, fields[BsonProxy.HASH_FIELD_HEADLINE_POS]);
							news.put(BsonProxy.HASH_FIELD_SUBJECT, fields[BsonProxy.HASH_FIELD_SUBJECT_POS]);
							if(singleton.typesMap.containsKey(fields[BsonProxy.HASH_FIELD_TYPE_POS])) {
								news.put(BsonProxy.HASH_FIELD_TYPE, singleton.typesMap.get(fields[BsonProxy.HASH_FIELD_TYPE_POS]));
							}else{
								news.put(BsonProxy.HASH_FIELD_TYPE, "unknown");
							}
							news.put(BsonProxy.HASH_FIELD_PATH, singleton.serializeFolder + "/" + fields[BsonProxy.HASH_FIELD_PATH_POS]);
							byte md5[] = dumpAsset(assets.open(fields[BsonProxy.HASH_FIELD_PATH_POS]), singleton.serializeFolder + "/" + fields[BsonProxy.HASH_FIELD_PATH_POS]);
							//news.put(BsonProxy.HASH_FIELD_CHECKSUM, md5.toString());
							news.put(BsonProxy.HASH_FIELD_CHECKSUM, "0");
							//singleton.list.add(news);
							singleton.lastTs = Long.parseLong(news_date);
							return news;
						}else{
							Log.w(TAG,"malformed line: <" + line + ">");
							continue;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
		}
		return  null;
	}


	public void run() {

		args_for_bkg.put(HASH_FIELD_DATE, "0");
		args_for_bkg.put(HASH_FIELD_CHECKSUM, "0");
		updateListeners();
		while (true) {
			runUntilStop(args_for_bkg);
			Sleep(2);
		}
	}

	private boolean runUntilStop(HashMap<String, String> args) {
		while (run) {
			/* keep trace of timestamp sequence
			* in order to decide when ask for the next news*/
			long old_ts=0;
			try {
				if(args.containsKey(HASH_FIELD_DATE)) {
					Long.parseLong(args.get(HASH_FIELD_DATE));
				}
			}catch (Exception e){

			}
			if (runningTask == null || !runningTask.isRunning()) {
				runningTask = new SocketAsyncTask(args);
				runningTask.execute(args);
			}
			if(runningTask != null && runningTask.isRunning()){
				if((old_ts!= 0 && !runningTask.isConnectionError()) || runningTask.noData()){
					Log.d(TAG, " (runUntilStop): No new news waiting ...");
					Sleep(20);
				}
			}
			Sleep(1);
			//Log.d(TAG, "Running:" + runningTask.isRunning());
		}
		return false;
	}
	public synchronized void saveStauts()
	{
		try {
			serialise_list();
		} catch (Exception e) {
			Log.d(TAG, " (saveStauts):" + e);
		}
	}
	public static void Sleep(int i) {
		try {
			Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public  static BsonProxy newCore() {
		if (singleton == null) {
			singleton = new BsonProxy();
		}
		return singleton;
	}

	public boolean Start() {
		if (serviceRunning == true) {
			return false;
		}

		coreThread = new Thread(this);
		try {
			setRun(true);
			coreThread.start();
		} catch (final Exception e) {

		}
		serviceRunning = true;

		return true;
	}

	public void setDumpFolder(String dumpFolder) {
		this.dumpFolder = new String(dumpFolder);
	}

	public String getDumpFolder() {
		return dumpFolder;
	}
	public void setImei(String imei) {
		this.imei = new String(imei);
	}

	public String getImei() {
		return imei;
	}



	public synchronized ArrayList<HashMap<String, String>> getList() {
		if(list==null && getSerialFile() != null && new File(getSerialFile()).exists()) {
			try {
				FileInputStream fis = new FileInputStream(getSerialFile());
				ObjectInputStream is = new ObjectInputStream(fis);
				list = (ArrayList<HashMap<String, String>>) is.readObject();
				is.close();
			} catch (Exception e) {
				Log.d(TAG, " (getList):" +e);
				e.printStackTrace();
			}
		}
		if(list==null){
			Log.d(TAG, " (getList) initializing list");
			list = new ArrayList<HashMap<String, String>>();
		}
		return list;
	}



	public boolean isRunning() {
		if(runningTask==null){
			return false;
		}
		return runningTask.isRunning();
	}

	public void setSerializeFolder(File filesDir) {
		this.serializeFolder=filesDir;
	}

	public void updateListeners(){
		if(list.isEmpty()){
			return;
		}
		for (NewsUpdateListener listener : listeners)
		{
			listener.onNewsUpdate(list);
		}
		if (!listeners.isEmpty()) {
			list.clear();
		}
	}

	private class SocketAsyncTask extends AsyncTask<HashMap<String,String>, Void, ByteBuffer> {


		private final HashMap<String, String> args;
		private boolean running = false;

		private boolean connectionError=false;

		public boolean isConnectionError() {
			return connectionError;
		}
		public boolean noData() {
			return noData;
		}

		private SocketAsyncTask(HashMap<String,String> args) {
			super();
			this.args = args;

		}

		@Override
		protected void onPreExecute() {
			running=true;
			super.onPreExecute();
		}

		@Override
		protected ByteBuffer doInBackground(HashMap<String,String>... args) {
			ByteBuffer wrapped = null;

			return wrapped ;
		}

		public boolean isRunning() {
			return running;
		}


		private void publishProgress(int read) {
			//	Log.d(TAG,"read:"+ read+" bytes");
		}

		@Override
		protected void onPostExecute(ByteBuffer result) {

			synchronized (this) {

					HashMap<String,String> ret= singleton.get_news();
					if (ret!=null && ret.size()>0) {
						if (ret.containsKey(HASH_FIELD_DATE)) {
							args.put(HASH_FIELD_DATE, ret.get(HASH_FIELD_DATE));
						}
						if (ret.containsKey(HASH_FIELD_CHECKSUM)) {
							args.put(HASH_FIELD_CHECKSUM, ret.get(HASH_FIELD_CHECKSUM));
							String cks = ret.get(HASH_FIELD_CHECKSUM);
							if ( cks.contentEquals("0")  && ret.containsKey(HASH_FIELD_PATH)) {
								list.add(ret);
								saveStauts();
								updateListeners();
								try {
									if (ret.containsKey(HASH_FIELD_DATE)) {
										args.put(HASH_FIELD_DATE, ret.get(HASH_FIELD_DATE));
										//todo: is "ok" it used?
										args.put("ok", "0");
									}
								} catch (Exception e) {
									Log.d(TAG, " (onPostExecute): failed to parse ");
								}
								news_n++;
							}
						}
						noData=false;
					}else{
						noData = true;
					}
					System.gc();

				running=false;
			}

		}
	}

}
