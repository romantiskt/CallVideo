package com.sk.weichat.call;

import android.view.Menu;

import com.sk.weichat.call.BaseScreen.SCREEN_TYPE;

public interface IBaseScreen {
	String getId();
	SCREEN_TYPE getType();
	boolean hasMenu();
	boolean hasBack();
	boolean back();
	boolean createOptionsMenu(Menu menu);
}
