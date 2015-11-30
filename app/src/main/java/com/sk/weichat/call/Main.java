package com.sk.weichat.call;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;


import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.utils.NgnUriUtils;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午5:50:10
 * @描述: 处理音视频的入口
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class Main extends Activity {

	private BroadcastReceiver mSipBroadCastRecv;

	private final NgnEngine mEngine;
	private final INgnSipService mSipService;


	public static final int ACTION_NONE = 0;
	public static final int ACTION_RESTORE_LAST_STATE = 1;
	public static final int ACTION_SHOW_AVSCREEN = 2;
	public static final int ACTION_SHOW_CONTSHARE_SCREEN = 3;
	public static final int ACTION_SHOW_SMS = 4;
	public static final int ACTION_SHOW_CHAT_SCREEN = 5;

	public final static String SIP_DOMAIN = "120.24.211.24";
	public String phoneNumber;
	public boolean isAudio;
	public Main() {
		mEngine = NgnEngine.getInstance();
		mSipService = mEngine.getSipService();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		phoneNumber = getIntent().getStringExtra(Constants.AUDIO_PHONENUMBER);
		isAudio=getIntent().getBooleanExtra(Constants.IS_AUDIO_OR_VIDEO,true);


		// Listen for registration events
		mSipBroadCastRecv = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();

				// Registration Event
				if (NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT.equals(action)) {
					NgnRegistrationEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
					if (args == null) {
						return;
					}
					switch (args.getEventType()) {
					case REGISTRATION_NOK://Failed to register
						break;
					case UNREGISTRATION_OK://You are now unregistered
						break;
					case REGISTRATION_OK://You are now registered
						break;
					case REGISTRATION_INPROGRESS://Trying to register.
						break;
					case UNREGISTRATION_INPROGRESS://Trying to unregister.
						break;
					case UNREGISTRATION_NOK://Failed to unregister
						break;
					}
				}
			}
		};

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);
		registerReceiver(mSipBroadCastRecv, intentFilter);
		if(isAudio){
			makeVoiceCall(phoneNumber);
			
		}else{
			makeVideoCall(phoneNumber);
		}
	}

	

	@Override
	protected void onDestroy() {
		// release the listener
		if (mSipBroadCastRecv != null) {
			unregisterReceiver(mSipBroadCastRecv);
			mSipBroadCastRecv = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

   public boolean makeVideoCall(String phoneNumber){
	   final String validUri = NgnUriUtils.makeValidSipUri(String.format("sip:%s@%s", phoneNumber, SIP_DOMAIN));
		if (validUri == null) {//failed to normalize sip uri '" + phoneNumber
			return false;
		}
		NgnAVSession avSession = NgnAVSession.createOutgoingSession(mSipService.getSipStack(), NgnMediaType.AudioVideo);

		Intent in = new Intent(Main.this, AVActivity.class);
		in.putExtra("id", Long.toString(avSession.getId()));
		Log.d("wang","id:"+Long.toString(avSession.getId()));
		startActivity(in);
		Main.this.finish();
		return avSession.makeCall(validUri);
   }
	public boolean makeVoiceCall(String phoneNumber) {
		final String validUri = NgnUriUtils.makeValidSipUri(String.format("sip:%s@%s", phoneNumber, SIP_DOMAIN));
		if (validUri == null) {//failed to normalize sip uri '" + phoneNumber
			return false;
		}
		NgnAVSession avSession = NgnAVSession.createOutgoingSession(mSipService.getSipStack(), NgnMediaType.Audio);

		Intent in = new Intent(Main.this, AVActivity.class);
		in.putExtra("id", Long.toString(avSession.getId()));
		Log.d("wang","id:"+Long.toString(avSession.getId()));
		startActivity(in);
		Main.this.finish();
		return avSession.makeCall(validUri);
	}
}