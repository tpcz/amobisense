package cz.cuni.mff.d3s.Amobisense.utils;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Utils {
	@SuppressWarnings("unused")
	private static void debugIntent(Intent intent, String tag) {
	      Log.w(tag, "action: " + intent.getAction());
	      Log.w(tag, "component: " + intent.getComponent());
	      Bundle extras = intent.getExtras();
	      if (extras != null) {
	         for (String key: extras.keySet()) {
	            Log.w(tag, "key [" + key + "]: " +
	               extras.get(key));
	         }
	      }
	      else {
	         Log.w(tag, "no extras");
	      }
	   }

}
