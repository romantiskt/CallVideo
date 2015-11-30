package com.sk.weichat.call;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.NgnNativeService;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnMsrpSession;
import org.doubango.ngn.utils.NgnPredicate;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午4:54:49
 * @描述: 处理引擎
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class Engine extends NgnEngine{
	private final static String TAG = Engine.class.getCanonicalName();
	
	private static final String CONTENT_TITLE = "IMSDroid";
	
	private static final int NOTIF_AVCALL_ID = 19833892;
	private static final int NOTIF_SMS_ID = 19833893;
	private static final int NOTIF_APP_ID = 19833894;
	private static final int NOTIF_CONTSHARE_ID = 19833895;
	private static final int NOTIF_CHAT_ID = 19833896;
	
	private IScreenService mScreenService;
	public static NgnEngine getInstance(){
		if(sInstance == null){
			sInstance = new Engine();
		}
		return sInstance;
	}
	
	public Engine(){
		super();
	}

	@Override
	public boolean start() {//去开启各种服务
		return super.start();
	}
	
	@Override
	public boolean stop() {
		return super.stop();
	}
	
	private void showNotification(int notifId, int drawableId, String tickerText) {
		if(!mStarted){
			return;
		}
        // Set the icon, scrolling text and timestamp
        final Notification notification = new Notification(drawableId, "", System.currentTimeMillis());
        
        Intent intent = new Intent(ImsApplication.getContext(), Main.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  | Intent.FLAG_ACTIVITY_NEW_TASK);
        
        switch(notifId){
        	case NOTIF_APP_ID:
        		notification.flags |= Notification.FLAG_ONGOING_EVENT;
        		intent.putExtra("notif-type", "reg");
        		break;
        		
        	case NOTIF_CONTSHARE_ID:
                intent.putExtra("action", Main.ACTION_SHOW_CONTSHARE_SCREEN);
                notification.defaults |= Notification.DEFAULT_SOUND;
                break;
                
        	case NOTIF_SMS_ID:
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.defaults |= Notification.DEFAULT_SOUND;
                notification.tickerText = tickerText;
                intent.putExtra("action", Main.ACTION_SHOW_SMS);
                break;
                
        	case NOTIF_AVCALL_ID:
        		tickerText = String.format("%s (%d)", tickerText, NgnAVSession.getSize());
        		intent.putExtra("action", Main.ACTION_SHOW_AVSCREEN);
        		break;
        		
        	case NOTIF_CHAT_ID:
        		notification.defaults |= Notification.DEFAULT_SOUND;
        		tickerText = String.format("%s (%d)", tickerText, NgnMsrpSession.getSize(new NgnPredicate<NgnMsrpSession>() {
					@Override
					public boolean apply(NgnMsrpSession session) {
						return session != null && NgnMediaType.isChat(session.getMediaType());
					}
				}));
        		intent.putExtra("action", Main.ACTION_SHOW_CHAT_SCREEN);
        		break;
        		
       		default:
       			
       			break;
        }
        
        PendingIntent contentIntent = PendingIntent.getActivity(ImsApplication.getContext(), notifId/*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);     

        notification.setLatestEventInfo(ImsApplication.getContext(), CONTENT_TITLE, tickerText, contentIntent);

        mNotifManager.notify(notifId, notification);
    }
	
	public void showAppNotif(int drawableId, String tickerText){
    	Log.d(TAG, "showAppNotif");
    	showNotification(NOTIF_APP_ID, drawableId, tickerText);
    }
	
	public void showAVCallNotif(int drawableId, String tickerText){
    	showNotification(NOTIF_AVCALL_ID, drawableId, tickerText);
    }
	
	public void cancelAVCallNotif(){
    	if(!NgnAVSession.hasActiveSession()){
    		mNotifManager.cancel(NOTIF_AVCALL_ID);
    	}
    }
	
	public void refreshAVCallNotif(int drawableId){
		if(!NgnAVSession.hasActiveSession()){
    		mNotifManager.cancel(NOTIF_AVCALL_ID);
    	}
    	else{
    		showNotification(NOTIF_AVCALL_ID, drawableId, "In Call");
    	}
    }
	
	public void showContentShareNotif(int drawableId, String tickerText){
    	showNotification(NOTIF_CONTSHARE_ID, drawableId, tickerText);
    }
	
	public void cancelContentShareNotif(){
    	if(!NgnMsrpSession.hasActiveSession(new NgnPredicate<NgnMsrpSession>() {
			@Override
			public boolean apply(NgnMsrpSession session) {
				return session != null && NgnMediaType.isFileTransfer(session.getMediaType());
			}}))
    	{
    		mNotifManager.cancel(NOTIF_CONTSHARE_ID);
    	}
    }
    
	public void refreshContentShareNotif(int drawableId){
		if(!NgnMsrpSession.hasActiveSession(new NgnPredicate<NgnMsrpSession>() {
			@Override
			public boolean apply(NgnMsrpSession session) {
				return session != null && NgnMediaType.isFileTransfer(session.getMediaType());
			}}))
    	{
    		mNotifManager.cancel(NOTIF_CONTSHARE_ID);
    	}
    	else{
    		showNotification(NOTIF_CONTSHARE_ID, drawableId, "Content sharing");
    	}
    }
	
	public void showContentChatNotif(int drawableId, String tickerText){
    	showNotification(NOTIF_CHAT_ID, drawableId, tickerText);
    }
	
	public void cancelChatNotif(){
    	if(!NgnMsrpSession.hasActiveSession(new NgnPredicate<NgnMsrpSession>() {
			@Override
			public boolean apply(NgnMsrpSession session) {
				return session != null && NgnMediaType.isChat(session.getMediaType());
			}}))
    	{
    		mNotifManager.cancel(NOTIF_CHAT_ID);
    	}
    }
    
	public void refreshChatNotif(int drawableId){
		if(!NgnMsrpSession.hasActiveSession(new NgnPredicate<NgnMsrpSession>() {
			@Override
			public boolean apply(NgnMsrpSession session) {
				return session != null && NgnMediaType.isChat(session.getMediaType());
			}}))
    	{
    		mNotifManager.cancel(NOTIF_CHAT_ID);
    	}
    	else{
    		showNotification(NOTIF_CHAT_ID, drawableId, "Chat");
    	}
    }
	
	public void showSMSNotif(int drawableId, String tickerText){
    	showNotification(NOTIF_SMS_ID, drawableId, tickerText);
    }
	
	public IScreenService getScreenService(){
		if(mScreenService == null){
			mScreenService = new ScreenService();
		}
		return mScreenService;
	}
	
	@Override
	public Class<? extends NgnNativeService> getNativeServiceClass(){
		return null;
	}
}
