package cz.cuni.mff.d3s.Amobisense.archivation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.InflaterInputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import cz.cuni.mff.d3s.Amobisense.utils.CryptoUtils;
import edu.umich.PowerTutor.service.LogUploader;

public class LogWebUploader extends LogUploader {
	
	public static final long UPLOAD_RETRY_INTERVAL_MS = 1000 * 60;
	Queue<UploadParams> filesToBeUploaded = new LinkedList<UploadParams>();
	
	boolean endExecution = false;
	
	BroadcastReceiver rec;

	public LogWebUploader(Context context) {
		super(context);
		this.context = context;
		uploaderThread = new UploaderThread();
		uploaderThread.setDaemon(true);
		uploaderThread.start();
		this.url = PreferenceManager.getDefaultSharedPreferences(context).getString("upload_url", "http://perun.ms.mff.cuni.cz:8000/upload");
		this.httpUploader = new HttpUploadArchive(url);
		
		rec = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				if (isOnlineOnWifi()) {
					synchronized (filesToBeUploaded) {
						filesToBeUploaded.notify();
					}
				}
				Log.i("UPLOADER", "Connectivity changed");
			}
		};
		
		context.registerReceiver(rec, new IntentFilter( ConnectivityManager.CONNECTIVITY_ACTION));
	}
		
	public class UploadParams {
		   String origFileName = "";
	   	   String archiveNameBase = "";
	   	   boolean removeWhenFinish = false;
	   	   boolean inflate = false;
	   	  
	   	  
	   	   public UploadParams(String origFileName, String uploadNameBase, boolean removeWhenFinished, boolean inflate) {
	   		   this.origFileName = origFileName;
	   		   this.archiveNameBase = uploadNameBase;
	   		   this.removeWhenFinish = removeWhenFinished;
	   		   this.inflate = inflate; 
	   	   }
		}
		
		private class UploaderThread extends Thread {
	     	// Message statusMsg  = new Message();
	     	  
	     	
	     	 private String getUploadFileName (String baseName, String origFileName) {
	     		long fileNr = 0;
	     		try {
	     			fileNr = Integer.parseInt(origFileName.substring(origFileName.lastIndexOf("-"), origFileName.length()));
	     	  	} catch (Exception e) {
	     	  		fileNr = 0;
	     	  	}
	     		
	           	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	           	long run_nr = prefs.getLong("run_nr", 0); 
	     		 
	           	
	     		return  baseName + "-" + 
	     				 CryptoUtils.getEncryptedUserUID(context)  + "-" + 
	     				
	     				 String.format("%04d", run_nr)  + "-" + 
	     				 String.format("%04d", fileNr)  +  "-" +
						 SystemClock.elapsedRealtime() +  ".log";
	     	 } 
	     	  
	     	 private boolean uploadFile (UploadParams params) {
	     		try {
		           	 InputStream logIn = null;
		           	 if (!params.inflate) {  
		           		 logIn =  new FileInputStream (new File (params.origFileName));
		           	 }else {
		           		 logIn = new InflaterInputStream( new FileInputStream(new File(params.origFileName))); 
		           	 }
		           	 	
		           	String uploadedFileName = getUploadFileName (params.archiveNameBase, params.origFileName);
		        	 	
		            if (httpUploader.addFileToArchive(logIn, uploadedFileName)) {
		            	  logIn.close();
			           	  if (params.removeWhenFinish) {
			         		  File f = new File (params.origFileName);
			         		  
			         		  if (f.exists()){
			         			  f.delete();
			         			  Log.i("UPLOADER", "Deleting successfully uploaded file: " + params.origFileName);
			         		  } else {
			         			  //statusMsg.arg1 = UMLogger.MESSAGE_FAIL;
			                 	  //UMLogger.makeToastHandler.sendMessage(statusMsg);
			                 	  //statusMsg  = new Message();
			                 	  Log.e("UPLOADER", "File IO Error, can not removed! (doe not exist...)"  + params.origFileName);
			         		  } 
			         	  }
			           	return true;
		            } else {
		            	 logIn.close();
			           	 try {
			           		
			           		 // deffer next attempt for 
			           		 sleep(UPLOAD_RETRY_INTERVAL_MS);
			           	 }catch (Exception e) {
			           		 // nothing
			           	 }
		            }             
	     		} catch(java.io.EOFException e) {
	              		//statusMsg.arg1 = MainActivity.MESSAGE_FAIL;
	            	    //UMLogger.makeToastHandler.sendMessage(statusMsg);
	            	    //statusMsg  = new Message();
	            	    Log.e("UPLOADER", "File EOF Error"  + e.getMessage());
	            	    return false;
	     		} catch(IOException e) {
	              		//statusMsg.arg1 = MainActivity.MESSAGE_FAIL;
	            	    //UMLogger.makeToastHandler.sendMessage(statusMsg);
	            	    //statusMsg  = new Message();
	            	    Log.e("UPLOADER", "File IO Error"  + e.getMessage());
	            	    return false;
	     		}
	     		return false;
	     	 }
	     	  
	         public void run() {
	        	 UploadParams p;
	        	 
	        	 while (!endExecution && !uploaderThread.isInterrupted()) { 
	        		 
	        		 // wait for somebody to input something in the queue
	        		 synchronized (filesToBeUploaded) {
	        			    while (!endExecution &&  filesToBeUploaded.isEmpty() && !uploaderThread.isInterrupted() ) {
	        			    	try  {    
	        			    		filesToBeUploaded.wait();
	        			    	}catch (InterruptedException e) {
	        			    		// nothing. Thread is ended by an interrupt request..
	        			    	}
	        			    }
	        			    
	        			    p = filesToBeUploaded.poll();
	        		 }
	        		 
	        		
		        	 if (!endExecution && !Thread.interrupted()) {
		        		 if (p != null) {
			        		 Log.i("UPLOADER", "Thread running, uploading file " + p.origFileName);
				        	 isUploading = true;
		        		 }
			        	 
			        	 if ((p != null ) &&  !uploadFile(p)) {
			        	 //if ((p != null ) && f.exists() &&  !uploadFile(p)) {
			        		 synchronized (filesToBeUploaded) {
			        			 filesToBeUploaded.add(p); // but not notify, wait for next data to try..
							} 
			        	 }
			        	 isUploading = false;
		        	 }
		        	 
		        	 // sleep one minute before next try...
		        	 if (!endExecution && shouldUpload() == false) {
		        		 try { 
		        			 Thread.sleep(UPLOAD_RETRY_INTERVAL_MS);
		        		 }
		        		 
		        		 catch (Exception e) {
							// nothing
						}
		        	 }
		        	 
		        	 
	        	 } // big while loop
	         }
	       }; 	
		
		
		
	  private Context context;
	  private boolean isUploading  = false;
	  private UploaderThread uploaderThread = null;
	  
	  public String url;
	  private HttpUploadArchive httpUploader = null;
	  
	
	  
	  public boolean isOnlineOnWifi() {
			    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			    if (cm == null) {
			    	return false;
			    }
		  	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
		  	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
		  	    	if (netInfo.getTypeName().equalsIgnoreCase("WIFI")) {
		  	    		return true;
		  	    	}else {
		  	    		return false;
		  	    	}
		  	    }
		  	    return false;
	  }

	  /* Returns true if this module supports uploading logs. */
	  public static boolean uploadSupported() {
	    return true;
	  }

	  /* Returns true if the log should be uploaded now.  This may depend on log
	   * file size, network conditions, etc. */
	  // TODO: This should probably give the file name of the log
	  public boolean shouldUpload() {		
	      return isOnlineOnWifi();
	  }

	  /* Called when the device is plugged in or unplugged.  The intended use of
	   * this is to improve upload policy decisions. */
	  public void plug(boolean plugged) {
	  }
	  
	  /* Initiate the upload of the file with the passed location. */
	  /* This function is asynchronout, can not return. */
	  public void enqueueForUpload(String origFileName, String uploadNameBase, boolean removeWhenFinished, boolean inflate) {
		 
		   synchronized (filesToBeUploaded) {
			   filesToBeUploaded.add(new UploadParams(origFileName, uploadNameBase, removeWhenFinished, inflate));
			   filesToBeUploaded.notify();
		   }   
	  }

	  /* Returns true if a file is currently being uploaded. */
	  public boolean isUploading() {
	    return this.isUploading;
	  }

	  /* Interrupt any threads doing upload work. */
	  public void interrupt() {
		 endExecution = true;
		 uploaderThread.interrupt();
	 	 synchronized (filesToBeUploaded) {  
		     Log.d("Cleaner", "Notoify Before");
			 filesToBeUploaded.notify();
			 Log.d("Cleaner", "Notoify After");
	 	 }
	 	 context.unregisterReceiver(rec);
	  } 

	  /* Join any threads that may be performing log upload work. */
	  public void join() throws InterruptedException {
		  //not necessary, ulpader thread is deamon...
		 uploaderThread.join();
	  }
	
	
}
