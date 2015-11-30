package com.sk.weichat.call;


import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.doubango.ngn.services.impl.NgnBaseService;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午5:00:20
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class ScreenService extends NgnBaseService implements IScreenService {
	private final static String TAG = ScreenService.class.getCanonicalName();
	
	private int mLastScreensIndex = -1; // ring cursor
	private final String[] mLastScreens =  new String[]{ // ring
    		null,
    		null,
    		null,
    		null
	};
	
	@Override
	public boolean start() {
		Log.d(TAG, "starting...");
		return true;
	}

	@Override
	public boolean stop() {
		Log.d(TAG, "stopping...");
		return true;
	}

	@Override
	public boolean back() {
		String screen;
		return true;
	}

	@Override
	public boolean bringToFront(int action, String[]... args) {
		Intent intent = new Intent(ImsApplication.getContext(), Main.class);
		try{
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("action", action);
			for(String[] arg : args){
				if(arg.length != 2){
					continue;
				}
				intent.putExtra(arg[0], arg[1]);
			}
			ImsApplication.getContext().startActivity(intent);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean bringToFront(String[]... args) {
		return this.bringToFront(Main.ACTION_NONE);
	}

	@Override
	public boolean show(Class<? extends Activity> cls, String id) {
		return false;
	}

	@Override
	public boolean show(Class<? extends Activity> cls) {
		return this.show(cls, null);
	}


	@Override
	public void runOnUiThread(Runnable r) {
		if(Engine.getInstance().getMainActivity() != null){
			Engine.getInstance().getMainActivity().runOnUiThread(r);
		}
		else{
			Log.e(this.getClass().getCanonicalName(), "No Main activity");
		}
	}

	@Override
	public boolean destroy(String id) {
		return false;
	}

	@Override
	public void setProgressInfoText(String text) {
	}

	@Override
	public boolean show(String id) {
		return false;
	}

	@Override
	public IBaseScreen getCurrentScreen() {
		return null;
	}

	@Override
	public IBaseScreen getScreen(String id) {
		return null;
	}
}
