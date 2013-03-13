package cz.cuni.mff.d3s.Amobisense.context;

import android.util.Log;

// array cyclic buffer for history
public class HistoryHolder {
	private int[]   ibuffer;
	private long[]  lbuffer;
	private float[] fbuffer;
	private double[] dbuffer;
	private StringBuilder[] sbuffer;
	private int head;
	private int prevHead;
	private int size;
	private String name;
	private ContextType type;
	
	private String TAG = "HistoryHolder";
	


	/* This file is INTENTIONALY not generic, since 
	 * java does not allow 
	 * basic types to be used for templating. As the 
	 * HistoryHolder lives in many instances and
	 * it is called every second to ad and remove
	 * points, Boxing and unboxing would be an 
	 * a significantly inefficient
	 */
	public HistoryHolder (int size, ContextType type, String name) {
		this.size = size;
		this.name = name;
		this.type = type;
		this.TAG = "HistoryHolder-" + name;
		
		switch (type){
			case INT:   
				ibuffer = new int[size];
				for (int i = 0; i < ibuffer.length; i ++) {
					ibuffer[i] = 0;
				}
				break;
			case LONG:  
				lbuffer = new long[size];
				for (int i = 0; i < lbuffer.length; i ++) {
					lbuffer[i] = 0;
				}
				break;
			case FLOAT: 
				fbuffer = new float[size];
				for (int i = 0; i < fbuffer.length; i ++) {
					fbuffer[i] = 0;
				}
				break;
			case DOUBLE: 
				dbuffer = new double[size];
				for (int i = 0; i < dbuffer.length; i ++) {
					dbuffer[i] = 0;
				}
				break;
			case STRING: 
				sbuffer = new StringBuilder[size];
				for (int i = 0; i < sbuffer.length; i ++) {
					sbuffer[i] = new StringBuilder();
				}
			case MIXED_LNUM_STRING:
				sbuffer = new StringBuilder[size];
				for (int i = 0; i < sbuffer.length; i ++) {
					sbuffer[i] = new StringBuilder();
				}
				lbuffer = new long[size];
				for (int i = 0; i < lbuffer.length; i ++) {
					lbuffer[i] = 0;
				}
				break;
		}	
		
		head = size -1;
		prevHead = head;
	}
	
	//public void setSize() {
	//	
	//}
	
	public void addPoint(int value) {
		switch (this.type){
		case INT:   
			__addPoint ((int) value);
			break;
		case LONG:  
			__addPoint ((long) value);
			break;
		case FLOAT: 
			__addPoint ((float) value);
			break;
		case DOUBLE: 
			__addPoint ((double) value);
			break;
		case STRING:
			__addPoint ("" + value);
		}
	}
	
	public void addPoint(long value) {
		switch (this.type){
		case INT:   
			__addPoint ((int) value);
			break;
		case LONG:  
			__addPoint ((long) value);
			break;
		case FLOAT: 
			__addPoint ((float) value);
			break;
		case DOUBLE: 
			__addPoint ((double) value);
			break;
		case STRING:
			__addPoint ("" + value);
		}
	}
	
	public void addPoint(float value) {
		switch (this.type){
		case INT:   
			__addPoint ((int)Math.round(value));
			break;
		case LONG:  
			__addPoint ((long) Math.round(value));
			break;
		case FLOAT: 
			__addPoint ((float) value);
			break;
		case DOUBLE: 
			__addPoint ((double) value);
			break;
		case STRING:
			__addPoint ("" + value);
		}
	}
	
	public void addPoint(double value) {
		switch (this.type){
		case INT:   
			__addPoint ((int)Math.round(value));
			break;
		case LONG:  
			__addPoint ((long) Math.round(value));
			break;
		case FLOAT: 
			__addPoint ((float) value);
			break;
		case DOUBLE: 
			__addPoint ((double) value);
			break;
		case STRING:
			__addPoint ("" + value);
		case NOT_SET:
			Log.w(TAG, "Adding point to not sed data!");
		}
	
	}
	
	public void addPoint(long value, String svalue) {
		switch (this.type){
		case INT:   
			__addPoint ((int) value);
			break;
		case LONG:  
			__addPoint ((long) value);
			break;
		case FLOAT: 
			__addPoint ((float) value);
			break;
		case DOUBLE: 
			__addPoint ((double) value);
			break;
		case STRING:	
			__addPoint ("" + value);
		case MIXED_LNUM_STRING:
			__addPoint (value, svalue);
		}
		
	}
	
	
	public void addPoint(String svalue) {
		switch (this.type){
		case INT:   
			int ivalue = 0;
			try {
				ivalue = Integer.parseInt(svalue);
			} catch (NumberFormatException e) {
			
			}
			Log.w(TAG, name + ": Trying to add nonumerical string to numerical history");
			__addPoint (ivalue);
			break;
		case LONG: 
			long lvalue = 0;
			try {
				lvalue = Long.parseLong(svalue);
			} catch (NumberFormatException e) {
			
			}
			Log.w(TAG, "Trying to add nonumerical string to numerical history");
			__addPoint (lvalue);
			break;
		case FLOAT: 
			float fvalue = 0;
			try {
				fvalue = Float.parseFloat(svalue);
			} catch (NumberFormatException e) {
			
			}
			Log.w(TAG, "Trying to add nonumerical string to numerical history");
			__addPoint (fvalue);
			break;
		case DOUBLE: 
			double dvalue = 0;
			try {
				dvalue = Long.parseLong(svalue);
			} catch (NumberFormatException e) {
			
			}
			Log.w(TAG, "Trying to add nonumerical string to numerical history");
			__addPoint (dvalue);
			break;
		case STRING:
			__addPoint (svalue);
		}
	}

		
	private void __addPoint(int value) {
		ibuffer [head] = (int) value;
		prevHead = head;
		head = (head + size - 1) % size; 
	}
	
	private void __addPoint(long value) {
		lbuffer [head] = value;
		prevHead = head;
		head = (head + size - 1) % size; 
	}
	
	private void __addPoint(float value) {
		fbuffer [head] = value;
		prevHead = head;
		head = (head + size - 1) % size; 
	}
	
	private void __addPoint(double value) {
		dbuffer [head] = value;
		prevHead = head;
		head = (head + size - 1) % size; 
	}
	
	private void __addPoint(String value) {
		sbuffer [head].setLength(0);
		sbuffer [head].append(value);
		prevHead = head;
		head = (head + size - 1) % size; 
	}
	
	private void __addPoint(long val, String value) {
		lbuffer [head] = val;
		sbuffer [head].setLength(0);
		sbuffer [head].append(value);
		prevHead = head;
		head = (head + size - 1) % size; 
	}

	
	public int[] getIHistory () {
		int[] ret = null;
		switch (type){
			case INT:   
				return __getIHistory();
			case LONG:  
			case MIXED_LNUM_STRING:
				ret = new int[this.lbuffer.length];
				for (int i = 0; i < this.lbuffer.length; i ++) {
					ret[i] = (int) this.lbuffer[i];
				}
				return ret;
			case FLOAT: 
				ret = new int[this.fbuffer.length];
				for (int i = 0; i < this.fbuffer.length; i ++) {
					ret[i] = (int) Math.round(this.fbuffer[i]);
				}
				return ret;
			case DOUBLE:
				ret = new int[this.dbuffer.length];
				for (int i = 0; i < this.dbuffer.length; i ++) {
					ret[i] = (int) Math.round(this.dbuffer[i]);
				}
				return ret;
			case STRING:
				ret = new int[this.sbuffer.length];
				for (int i = 0; i < this.sbuffer.length; i ++) {
					try {
						ret[i] = (int) Integer.parseInt(this.sbuffer[i].toString());
					} catch (NumberFormatException e) {
						ret[i] = 0;
					}
				}
				return ret;

		}
		// deth code, ret is null, but  this point should be never reached
		return ret;
	}
	
	
	public long[] getLHistory () {
		long[] ret = null;
		switch (type){
			case INT:   
				ret = new long[this.ibuffer.length];
				for (int i = 0; i < this.ibuffer.length; i ++) {
					ret[i] = (long) this.ibuffer[i];
				}
			case LONG:
			case MIXED_LNUM_STRING:
				return __getLHistory();
			case FLOAT: 
				ret = new long[this.fbuffer.length];
				for (int i = 0; i < this.fbuffer.length; i ++) {
					ret[i] = (long) Math.round(this.fbuffer[i]);
				}
				return ret;
			case DOUBLE:
				ret = new long[this.dbuffer.length];
				for (int i = 0; i < this.dbuffer.length; i ++) {
					ret[i] = (long) Math.round(this.dbuffer[i]);
				}
				return ret;
			case STRING:
				ret = new long[this.sbuffer.length];
				for (int i = 0; i < this.sbuffer.length; i ++) {
					try {
						ret[i] = Long.parseLong(this.sbuffer[i].toString());
					} catch (NumberFormatException e) {
						ret[i] = 0;
					}
				}
				return ret;
		}
		
		// deth code, ret is null, but  this point should be never reached
		return ret;
	}
	
	
	public double[] getDHistory () {
		double[] ret = null;
		switch (type){
			case INT:   
				ret = new double[this.ibuffer.length];
				for (int i = 0; i < this.ibuffer.length; i ++) {
					ret[i] = (double) this.ibuffer[i];
				}
			case LONG:  
			case MIXED_LNUM_STRING:
				ret = new double[this.lbuffer.length];
				for (int i = 0; i < this.lbuffer.length; i ++) {
					ret[i] = (double) this.lbuffer[i];
				}
				return ret;
			case FLOAT: 
				ret = new double[this.fbuffer.length];
				for (int i = 0; i < this.fbuffer.length; i ++) {
					ret[i] = (double) this.fbuffer[i];
				}
				return ret;
			case DOUBLE:
				return __getDHistory();
			case STRING:
				ret = new double[this.sbuffer.length];
				for (int i = 0; i < this.sbuffer.length; i ++) {
					try {
						ret[i] = Double.parseDouble(this.sbuffer[i].toString());
					} catch (NumberFormatException e) {
						ret[i] = 0;
					}
				}
				return ret;
		}
		
		// deth code, ret is null, but  this point should be never reached
		return ret;
	}
	
	
	public float[] getFHistory () {
		float[] ret = null;
		switch (type){
			case INT:   
				ret = new float[this.ibuffer.length];
				for (int i = 0; i < this.ibuffer.length; i ++) {
					ret[i] = (float) this.ibuffer[i];
				}
			case LONG:  
			case MIXED_LNUM_STRING:
				ret = new float[this.lbuffer.length];
				for (int i = 0; i < this.lbuffer.length; i ++) {
					ret[i] = (float) this.lbuffer[i];
				}
				return ret;
			case FLOAT:
				return __getFHistory();
			case DOUBLE:
				ret = new float[this.dbuffer.length];
				for (int i = 0; i < this.dbuffer.length; i ++) {
					ret[i] = (float) this.dbuffer[i];
				}
				return ret;
				
			case STRING:
				ret = new float[this.sbuffer.length];
				for (int i = 0; i < this.sbuffer.length; i ++) {
					try {
						ret[i] = Float.parseFloat(this.sbuffer[i].toString());
					} catch (NumberFormatException e) {
						ret[i] = 0;
					}
				}
				return ret;
		}
		
		// deth code, ret is null, but  this point should be never reached
		return ret;
	}
	
	private int[] __getIHistory () {			
		int[] ret = new int[size];
		System.arraycopy(ibuffer, prevHead, ret, 0, size - prevHead);
		System.arraycopy(ibuffer, 0, ret, size - prevHead , prevHead);
		
		return ret;
	}
	
	private long[] __getLHistory () {
		// TODO if not this type -> exception
		long[] ret = new long[size];
		System.arraycopy(lbuffer, prevHead, ret, 0, size - prevHead);
		System.arraycopy(lbuffer, 0, ret, size - prevHead , prevHead);
		
		return ret;
	}
	
	private float[] __getFHistory () {
		// TODO if not this type -> exception
		float[] ret = new float[size];
		System.arraycopy(fbuffer, prevHead, ret, 0, size - prevHead);
		System.arraycopy(fbuffer, 0, ret, size - prevHead , prevHead);
		return ret;
	}
	
	private double[] __getDHistory () {
		// TODO if not this type -> exception
		double[] ret = new double[size];
		System.arraycopy(dbuffer, prevHead, ret, 0, size - prevHead);
		System.arraycopy(dbuffer, 0, ret, size - prevHead , prevHead);
		return ret;
	}

	public double getDLastValue () {
		switch (type){
			case INT:   
				return ibuffer[prevHead];
			case LONG:  
				return lbuffer[prevHead];
			case FLOAT: 
				return fbuffer[prevHead];
			case DOUBLE: 
				return dbuffer[prevHead];
			case STRING:
				return 0;
				
		}
		return 0;
	}
	
	public long getLLastValue () {
		switch (type){
			case INT:   
				return ibuffer[prevHead];
			case LONG:  
				return lbuffer[prevHead];
			case FLOAT: 
				return Math.round(fbuffer[prevHead]);
			case DOUBLE: 
				return Math.round (dbuffer[prevHead]);
			case STRING:
				return 0;
		}
		return 0; 
	}
	
	public String getSLastValue () {
		switch (type){
			case INT:   
				return ""+ ibuffer[prevHead];
			case LONG:  
				return ""+lbuffer[prevHead];
			case FLOAT: 
				return ""+ fbuffer[prevHead];
			case DOUBLE: 
				return ""+ dbuffer[prevHead];
			case STRING:
			case MIXED_LNUM_STRING:
				return sbuffer[prevHead].toString();
		}
		return ""; 
	}
	

	
	public boolean isIntegerType() {
		return 
				this.type == ContextType.LONG || 
				this.type == ContextType.INT;
	}
	
	public boolean isPoorNumericType () {
		return 
				this.type == ContextType.LONG ||
				this.type == ContextType.INT || 
				this.type == ContextType.DOUBLE ||
				this.type == ContextType.FLOAT;
	}
}
