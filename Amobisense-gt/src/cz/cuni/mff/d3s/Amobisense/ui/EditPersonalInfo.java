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

*/

package cz.cuni.mff.d3s.Amobisense.ui;

import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import cz.cuni.mff.d3s.Amobisense.R;

public class EditPersonalInfo extends PreferenceActivity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.personal_info);
    
    //validation of values.
   
    
    Preference age = (EditTextPreference) getPreferenceScreen().findPreference("personalinfo_age");
    age.setOnPreferenceChangeListener( new OnPreferenceChangeListener() {

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean rtnval = true;
            String pattern = "[0-9]{1,3}";
            if (!Pattern.matches(pattern, newValue.toString())) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(EditPersonalInfo.this);
                builder.setTitle("Invalid Input (Age)");
                builder.setMessage("Invalid Input (Age)");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                rtnval = false;
            }
            return rtnval;
        }
    });
  }
}
