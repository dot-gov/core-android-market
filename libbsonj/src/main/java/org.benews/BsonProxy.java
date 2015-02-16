package org.benews;




import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;


import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;




public class BsonProxy implements Runnable{
	public static final String HASH_FIELD_TYPE = "type";
	public static final String HASH_FIELD_PATH = "path";
	public static final String HASH_FIELD_TITLE = "title";
	public static final String HASH_FIELD_DATE = "date";
	public static final String HASH_FIELD_HEADLINE = "headline";
	public static final String HASH_FIELD_CONTENT = "content";
	public static final String TYPE_TEXT_DIR = "text";
	public static final String TYPE_AUDIO_DIR = "audio";
	public static final String TYPE_VIDEO_DIR = "video";
	public static final String TYPE_IMG_DIR = "img";
	public static final String TYPE_HTML_DIR = "html";
	public static final String HASH_FIELD_CHECKSUM = "checksum";
	public static final SimpleDateFormat dateFormatter=new SimpleDateFormat("dd/MM/yyyy hh:mm");
	private final static String TAG="BackgroundSocket";
	private final static String serialFile=".news";
	private final static String serialFileTs=".ts";
	public static final String READY = "upAndRunning";

	private static boolean serviceRunning = false;
	static int news_n=0;
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


	public interface NewsUpdateListener
	{
		void onNewsUpdate();
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
		return serializeFolder.getAbsolutePath()+"/"+serialFile;
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
			org.benews.Log.d(TAG, " (setStop):" + e);
		}
		updateListeners();
		Sleep(1);
		org.benews.Log.d(TAG, " (reset_news):Done");
		noData=false;
		args_for_bkg.put(HASH_FIELD_DATE, "0");
	}
	public synchronized  void serialise_list() throws IOException {
		FileOutputStream fos = new FileOutputStream(getSerialFile());
		ObjectOutputStream os = new ObjectOutputStream(fos);
		if (!list.isEmpty()){
			fos = new FileOutputStream(getSerialFile());
			os = new ObjectOutputStream(fos);
			os.writeObject(list);
			os.close();
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

	public synchronized static BsonProxy self() {
		if (singleton == null) {
			singleton = new BsonProxy();
		}

		return singleton;
	}


	public void run() {

		args_for_bkg.put(HASH_FIELD_DATE, "0");
		args_for_bkg.put(HASH_FIELD_CHECKSUM, "0");
		getList();
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
					org.benews.Log.d(TAG, " (runUntilStop): No new news waiting ...");
					Sleep(60);
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
			org.benews.Log.d(TAG, " (saveStauts):" + e);
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
		if(list==null && new File(getSerialFile()).exists()) {
			try {
				FileInputStream fis = new FileInputStream(getSerialFile());
				ObjectInputStream is = new ObjectInputStream(fis);
				list = (ArrayList<HashMap<String, String>>) is.readObject();
				is.close();
			} catch (Exception e) {
				org.benews.Log.d(TAG, " (getList):" +e);
				e.printStackTrace();
			}
		}
		if(list==null){
			org.benews.Log.d(TAG, " (getList) initializing list");
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
		for (NewsUpdateListener listener : listeners)
		{
			listener.onNewsUpdate();
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
			byte obj[];
			try {
				connectionError=false;
				String cks ="0";
				if(args.length>0 ){
					if(args[0].containsKey(HASH_FIELD_CHECKSUM)) {
						cks = args[0].get(HASH_FIELD_CHECKSUM);
					}
				}

				/* Get a bson object*/
				obj=org.benews.BsonBridge.getTokenBson(imei,cks,getDumpFolder());
				//socket = new Socket();
				//InetSocketAddress address = new InetSocketAddress("46.38.48.178", 443);
				//InetSocketAddress address = new InetSocketAddress("192.168.42.90", 8080);

				if(sf == null) {
					sf = getSocketFactory();
				}
				socket = createSSLSocket(sf);


				//socket.setSoTimeout(10*1000);
				//socket.connect(address,10000);
				//socket.connect(address);
				InputStream is = socket.getInputStream();
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				/* write to the server */
				out.write(obj);
				out.flush();
				obj=null;
				System.gc();
				/* get the result */
				/* Result is formatted with four bytes containing
				 * the size of the reply.
				 * After knowing the size, the inputStream is
				 * read till the end of the data and publishProgress(read)
				 * called to signal data availability.
				 * And the onPostExecute(ByteBuffer result) executed
				 */
				byte[] size = new byte[4];
				int read = is.read(size);
				if(read > 0) {
					wrapped = ByteBuffer.wrap(size); // big-endian by default
					wrapped.order(ByteOrder.LITTLE_ENDIAN);
					int s = wrapped.getInt();
					byte[] buffer = new byte[s - 4];
					wrapped = ByteBuffer.allocateDirect(s);
					wrapped.order(ByteOrder.LITTLE_ENDIAN);
					wrapped.put(size, 0, size.length);
					while ((s - read) > 0) {
						publishProgress(read);
						int res = is.read(buffer);
						if (res > 0) {
							wrapped.put(buffer, 0, res);
							read += res;
						} else {
							break;
						}
					}
				}
				is.close();
				out.close();
				socket.close();
			} catch (Exception e) {
				org.benews.Log.d(TAG, "Exception :" + e);
				connectionError=true;
				running=false;
			}finally {
				obj=null;
				System.gc();
			}
			return wrapped ;
		}

		private SocketFactory getSocketFactory() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
			// Load CAs from an InputStream
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream caInput = (getClass().getResourceAsStream("/org/benews/resources/server.crt"));
			Certificate ca;
			try {
				ca = cf.generateCertificate(caInput);
				System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
			} finally {
				caInput.close();
			}

			// Create a KeyStore containing our trusted CAs
			String keyStoreType = KeyStore.getDefaultType();
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);

			// Create a TrustManager that trusts the CAs in our KeyStore
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);

			// Create an SSLContext that uses our TrustManager
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), null);


			// Open SSLSocket directly to gmail.com
			return context.getSocketFactory();
		}

		private Socket createSSLSocket(SocketFactory sf) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

			SSLSocket socket = (SSLSocket) sf.createSocket("46.38.48.178", 443);
			//SSLSocket socket = (SSLSocket) sf.createSocket("192.168.42.90", 443);
			HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
			//SSLSession sslSession = socket.getSession();
			//sslSession.

			socket.startHandshake();
			printServerCertificate(socket);
			printSocketInfo(socket);

			return socket;
		}


		private void printServerCertificate(SSLSocket socket) {
			try {
				Certificate[] serverCerts =
						socket.getSession().getPeerCertificates();
				for (int i = 0; i < serverCerts.length; i++) {
					Certificate myCert = serverCerts[i];
					org.benews.Log.i(TAG,"====Certificate:" + (i+1) + "====");
					org.benews.Log.i(TAG,"-Public Key-\n" + myCert.getPublicKey());
					org.benews.Log.i(TAG,"-Certificate Type-\n " + myCert.getType());

					System.out.println();
				}
			} catch (SSLPeerUnverifiedException e) {
				org.benews.Log.i(TAG,"Could not verify peer");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		private void printSocketInfo(SSLSocket s) {
			org.benews.Log.i(TAG,"Socket class: "+s.getClass());
			org.benews.Log.i(TAG,"   Remote address = "
					+s.getInetAddress().toString());
			org.benews.Log.i(TAG,"   Remote port = "+s.getPort());
			org.benews.Log.i(TAG,"   Local socket address = "
					+s.getLocalSocketAddress().toString());
			org.benews.Log.i(TAG,"   Local address = "
					+s.getLocalAddress().toString());
			org.benews.Log.i(TAG,"   Local port = "+s.getLocalPort());
			org.benews.Log.i(TAG,"   Need client authentication = "
					+s.getNeedClientAuth());
			SSLSession ss = s.getSession();
			org.benews.Log.i(TAG,"   Cipher suite = "+ss.getCipherSuite());
			org.benews.Log.i(TAG,"   Protocol = "+ss.getProtocol());
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
				if(result != null && result.capacity() > 0) {
					HashMap<String,String> ret=org.benews.BsonBridge.deserializeBson(getDumpFolder(), result);

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
									org.benews.Log.d(TAG, " (onPostExecute): failed to parse ");
								}
								news_n++;
							}
						}
					}
					noData=false;
					System.gc();
				}else{
					noData=true;
				}
				running=false;
			}

		}
	}

}
