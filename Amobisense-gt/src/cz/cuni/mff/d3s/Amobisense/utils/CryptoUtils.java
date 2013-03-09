package cz.cuni.mff.d3s.Amobisense.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

public class CryptoUtils {
	
	  private static final String TAG = "CryptoUtils"; 
	
	   private static String bytesToHexString(byte[] bytes) {
	        // http://stackoverflow.com/questions/332079
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < bytes.length; i++) {
	            String hex = Integer.toHexString(0xFF & bytes[i]);
	            if (hex.length() == 1) {
	                sb.append('0');
	            }
	            sb.append(hex);
	        }
	        return sb.toString();
	    }
	
	public static String SHA256(String input) {
	    MessageDigest digest=null;
	    String hash = "SHA256-NOT-IMPELEMENTED";
	    
	    try {
	        digest = MessageDigest.getInstance("SHA-256");
	        digest.update(input.getBytes());

	        hash = bytesToHexString(digest.digest());

	    } catch (NoSuchAlgorithmException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	        Log.e(TAG, "SHA2 not impelemnted on this device!");
	    }
		
		return hash;
	}
	
	public static String getEncryptedUserUID(Context context) {
		return CryptoUtils.SHA256(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
	}
	
	public static String anonymizeValue(String input, int outputLength) {
		return CryptoUtils.SHA256(input).substring(0, outputLength);
	}
	
	public static String anonymizeValue(int input, int outputLength) {
		return CryptoUtils.SHA256("" + input).substring(0, outputLength);
	}
}
