/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
*/

package cz.cuni.mff.d3s.Amobisense.ui;

import java.util.Vector;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import cz.cuni.mff.d3s.Amobisense.R;
import cz.cuni.mff.d3s.Amobisense.context.readers.AbstractReader;
import cz.cuni.mff.d3s.Amobisense.context.readers.Orientation;
import edu.umich.PowerTutor.phone.PhoneSelector;
import edu.umich.PowerTutor.service.DataCollector;

public class EditPreferences extends PreferenceActivity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    
    CheckBoxPreference pr = (CheckBoxPreference) getPreferenceScreen().findPreference("allow_orientation_sensor");
    pr.setOnPreferenceChangeListener( new OnPreferenceChangeListener() {

        /* (non-Javadoc)
         * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
         */
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            
        	/*if ((Boolean) newValue == true) {
	        	if( DataCollector.getInstance() !=null) {
		            
	        		Orientation ori = new Orientation(DataCollector.getInstance().service, PhoneSelector.getConstants(DataCollector.getInstance().service));
		    		
					if (ori.isSupported()) {
						ori.start();
						DataCollector.getInstance().getContextReaders().add(ori);
					}
	        	}
        	} else {
        		if(DataCollector.getInstance() !=null) {
        			Vector<AbstractReader> readers = DataCollector.getInstance().getContextReaders();
        			for (AbstractReader reader : readers){
        				if (reader instanceof Orientation){
        					readers.remove(reader);
        				}
        			}
        		}
        	}
        	
			return true;
		*/
        	Toast.makeText(EditPreferences.this, "Please stop and start service in main screen to take effect", Toast.LENGTH_SHORT).show();
        	return true;
        }
    });
  }
}