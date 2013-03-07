/*
Copyright (C) 2011 The University of Michigan, modified
2013 by Tomas Pop, Charles University in Prague.

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

package cz.cuni.mff.d3s.Amobisense.context.readers;

import android.content.Context;
import edu.umich.PowerTutor.phone.PhoneConstants;
import edu.umich.PowerTutor.service.DataCollector;

/**
 * Abstract class for context state holders based on events 
 * (such as phone state listeners or broadcast listeners).
 * 
 * @author pop
 * 
 */
public abstract class AbstractEventReader extends AbstractReader implements IDataReader {
		
	protected boolean stopHistory = false;
	
	/** should be changed in subclasses! */
	public static final String TAG = "EventReader";
	
	public AbstractEventReader(Context c, PhoneConstants phoneValues, String mainDataId) {
		super(c, phoneValues, mainDataId);
	}
	
	
	protected void rememberHistory() {
		rememberHistory(1);
	}
	
	
	protected void rememberHistory(final int intervalSecond) {
		stopHistory = false;

		Runnable collector = new Runnable() {
			public void run() {
				
				if (handler != null && !stopHistory) {
					handler.postDelayed(this, intervalSecond * 1000);
				}
				updateHistory();
			}
		};

		if (!stopHistory) {
			handler.post(collector);
		}
	}	
}