package com.sk.weichat.call;

import android.util.Log;

import org.doubango.ngn.NgnApplication;

public class ImsApplication extends NgnApplication{
	private final static String TAG = ImsApplication.class.getCanonicalName();
	
	public ImsApplication() {
    	Log.d(TAG,"ImsApplication()");
    }
}
