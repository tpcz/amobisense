/**
 * This file is part of is inspired by upload service of Funf, funf.org/, nadav@media.mit.edu.
 * File is under GNU Lesser GPL.
 */
package cz.cuni.mff.d3s.Amobisense.archivation;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.net.Uri;
import android.util.Log;
/**
 * Archives a file to the url specified using POST HTTP method.
 * 
 * NOTE: not complete or tested
 *
 */
public class HttpUploadArchive implements IFileArchivator {
	
	private String uploadUrl;
	
	@SuppressWarnings("unused")
	private String mimeType;
	
    private static final String TAG = "HttpUploader";
	
	public HttpUploadArchive(final String uploadUrl) {
		this(uploadUrl, "application/x-binary");
	}
	
	public HttpUploadArchive(final String uploadUrl, final String mimeType) {
		this.uploadUrl = uploadUrl;
		this.mimeType = mimeType;
	}
	
	public String getId() {
		return uploadUrl;
	}
	
	public boolean addFileToArchive(InputStream fIn, String fileNameInArchive) {
		if (!isValidUrl(uploadUrl)) {
			Log.e("http-uploader", "Url is not valid");
			return false;
		}
		
		return isValidUrl(uploadUrl) ? uploadFile(fIn, uploadUrl, fileNameInArchive) : false;
	}
	
	public static boolean isValidUrl(String url) {

		boolean isValidUrl = false;
		if (url != null &&  !url.trim().equals("")) {
			try {
				Uri test = Uri.parse(url);
				isValidUrl = test.getScheme() != null 
				&& test.getScheme().startsWith("http") 
				&& test.getHost() != null 
				&& !test.getHost().trim().equals("");
			} catch (Exception e) {
				Log.d(TAG, "Not valid", e);
			}
		}
		//Log.d(TAG, "Valid url? " + isValidUrl + " (" + url + " )");
		
		return isValidUrl;
	}
	
	/**
	 * Copied (and slightly modified) from Friends and Family
	 * @param file
	 * @param uploadurl
	 * @return
	 */
	public static boolean uploadFile(InputStream fIn, String uploadurl, String uploadName) {
		HttpURLConnection conn = null; 
		DataOutputStream dos = null; 

		String lineEnd = "\r\n"; 
		String twoHyphens = "--"; 
		String boundary =  "*****"; 


		int bytesRead, bytesAvailable, bufferSize; 
		byte[] buffer; 
		int maxBufferSize = 64*1024;

		boolean isSuccess = true;
		try 
		{ 
			// open a URL connection to the Servlet 
			URL url = new URL(uploadurl); 
			// Open a HTTP connection to the URL 
			conn = (HttpURLConnection) url.openConnection(); 
			// Allow Inputs 
			conn.setDoInput(true); 
			// Allow Outputs 
			conn.setDoOutput(true); 
			// Don't use a cached copy. 
			conn.setUseCaches(false); 
			// set timeout
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			// Use a post method. 
			conn.setRequestMethod("POST"); 
			conn.setRequestProperty("Connection", "Keep-Alive"); 
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary); 

			dos = new DataOutputStream( conn.getOutputStream() ); 
			dos.writeBytes(twoHyphens + boundary + lineEnd); 
			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + uploadName +"\"" + lineEnd); 
			dos.writeBytes(lineEnd); 

			//Log.i("FNF","UploadService Runnable:Headers are written"); 

			// create a buffer of maximum size 
			bytesAvailable = fIn.available(); 
			bufferSize = Math.min(bytesAvailable, maxBufferSize); 
			buffer = new byte[bufferSize]; 

			// read file and write it into form... 
			bytesRead = fIn.read(buffer, 0, bufferSize); 
			while (bytesRead > 0) 
			{ 
				dos.write(buffer, 0, bufferSize); 
				bytesAvailable = fIn.available(); 
				bufferSize = Math.min(bytesAvailable, maxBufferSize); 
				bytesRead = fIn.read(buffer, 0, bufferSize); 
			} 

			// send multi-part form data necessary after file data... 
			dos.writeBytes(lineEnd); 
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd); 

			// close streams 
			//Log.i("FNF","UploadService Runnable:File is written"); 
			fIn.close(); 
			dos.flush(); 
			dos.close(); 
		} 
		catch (Exception e) 
		{ 
			Log.e(TAG, "UploadService Runnable: Client Request error");
			isSuccess = false;
		} 

		// read the SERVER RESPONSE 
		try {
			if (conn!= null && conn.getResponseCode() != 200) {
				isSuccess = false;
			}
		} catch (Exception e) {
			Log.e(TAG, "Connection response error");
			isSuccess = false;
		}
 
		return isSuccess;
	}
}
