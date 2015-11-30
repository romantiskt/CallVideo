package com.sk.weichat.call;

import android.app.Activity;

import org.doubango.ngn.services.INgnBaseService;

public interface IScreenService extends INgnBaseService{
	boolean back();
	boolean bringToFront(int action, String[]... args);
	boolean bringToFront(String[]... args);
	boolean show(Class<? extends Activity> cls, String id);
	boolean show(Class<? extends Activity> cls);
	boolean show(String id);
	void runOnUiThread(Runnable r);
	boolean destroy(String id);
	void setProgressInfoText(String text);
	IBaseScreen getCurrentScreen();
	IBaseScreen getScreen(String id);
}
