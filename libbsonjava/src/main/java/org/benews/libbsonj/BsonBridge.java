package org.benews.libbsonj;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.HashMap;


/**
 * Created by zad on 15/10/14.
 * to refresh the c header fot the library libson:
 * 1) cd in src directory of the .java : cd src/main/java/
 * 2) build the single java : javac -d tmp .org/benews/BsonBridge.java
 * 3) cd tmp
 * 4) generate c header : javah -jni org.benews.libbsonj.BsonBridge
 * 5) resulting header file in our case is /tmp/org_benews_BsonBridge.h
 * 6) it can be copied in src/libbson/include/bsonBridge.h
 * */
public class BsonBridge {
	static {
		System.loadLibrary("bson");
	}
	/*	public class ReceivedSnap{
		};
	*/
	public static final String TAG = "BsonBridge";
	public static final int BSON_TYPE_TEXT = 0;

	public static native HashMap<String,String> deserialize(String baseDir, ByteBuffer payload);
	public static native  byte[] getToken(String imei,String cks,String baseDir);

	//public static native byte[] F(ReceivedSnap a, String b);

	public static HashMap<String,String> deserializeBson(String baseDir, ByteBuffer payload){
		Log.d(TAG, "serialize called\n");
		return deserialize(baseDir, payload);
	}
	public static  byte[] getTokenBson (String imei, String cks,String baseDir){
		Log.d(TAG,"getToken called\n");
		return getToken(imei,cks,baseDir);

	}
}
