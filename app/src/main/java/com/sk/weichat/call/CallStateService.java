package com.sk.weichat.call;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession.InviteState;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午4:53:45
 * @描述: 来电处理
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class CallStateService extends Service {
	private CallStateReceiver callStateReceiver;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		final IntentFilter intentRFilter = new IntentFilter();
		callStateReceiver = new CallStateReceiver();
		intentRFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
		registerReceiver(callStateReceiver, intentRFilter);
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(callStateReceiver);
	}
	public class CallStateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			final String action = intent.getAction();

			if (NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)) {
				NgnInviteEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
				if (args == null) {
					Log.d("wang", "Invalid event args");
					return;
				}

				NgnAVSession avSession = NgnAVSession.getSession(args.getSessionId());
				if (avSession == null) {
					return;
				}

				final InviteState callState = avSession.getState();
				NgnEngine mEngine = NgnEngine.getInstance();

				switch (callState) {
				case NONE:
				default:
					break;
				case INCOMING:// 来电
					Log.d("wang", "收到来电了...Incoming call...");
					// Ring
					Intent in = new Intent(context, AVActivity.class);
					in.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  | Intent.FLAG_ACTIVITY_NEW_TASK);
					in.putExtra("id", Long.toString(avSession.getId()));
					Log.d("wang","Incoming call..id:"+Long.toString(avSession.getId()));
					startActivity(in);
					mEngine.getSoundService().startRingTone();
					break;
				case INCALL:
					Log.i("wang", "Call connected");
					mEngine.getSoundService().stopRingTone();
					break;
				case TERMINATED:
					Log.i("wang", "Call terminated");
					mEngine.getSoundService().stopRingTone();
					mEngine.getSoundService().stopRingBackTone();
					break;
				}
			}
		}
	}
}
