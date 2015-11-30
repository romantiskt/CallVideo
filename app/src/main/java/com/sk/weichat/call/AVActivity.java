package com.sk.weichat.call;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yang.wang.audiovoiceproject.R;

import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.events.NgnInviteEventTypes;
import org.doubango.ngn.events.NgnMediaPluginEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.model.NgnContact;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession.InviteState;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnContentType;
import org.doubango.ngn.utils.NgnGraphicsUtils;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.ngn.utils.NgnTimer;
import org.doubango.ngn.utils.NgnUriUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午4:52:16
 * @描述: 视频语音页面
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class AVActivity extends Activity {
	 private static final String TAG = "AVActivity";
	private static final SimpleDateFormat sDurationTimerFormat = new SimpleDateFormat("mm:ss");

	private static int mCountBlankPacket;
	private static int mLastRotation; // values: degrees
	private boolean mSendDeviceInfo;
	private int mLastOrientation; // values: portrait, landscape...

	private String mRemotePartyDisplayName;
	private Bitmap mRemotePartyPhoto;

	private ViewType mCurrentView;
	private LayoutInflater mInflater;
	private RelativeLayout mMainLayout;
	private BroadcastReceiver mBroadCastRecv;

	private View mViewTrying;
	private View mViewInAudioCall;
	private View mViewInCallVideo;
	private FrameLayout mViewLocalVideoPreview;
	private FrameLayout mViewRemoteVideoPreview;
	private View mViewTermwait;
	private View mViewProxSensor;

	private NgnTimer mTimerInCall=null;
	private NgnTimer mTimerSuicide=null;
	private NgnTimer mTimerBlankPacket=null;
	private NgnAVSession mAVSession;
	private boolean mIsVideoCall;

	private TextView mTvInfo;
	private TextView mTvDuration;

	private AlertDialog mTransferDialog;
	private NgnAVSession mAVTransfSession;

	private MyProxSensor mProxSensor;
	private KeyguardLock mKeyguardLock;
	private OrientationEventListener mListener;

	private PowerManager.WakeLock mWakeLock;

	private static final int SELECT_CONTENT = 1;

	private final static int MENU_PICKUP = 0;
	private final static int MENU_HANGUP = 1;
	private final static int MENU_HOLD_RESUME = 2;
	private final static int MENU_SEND_STOP_VIDEO = 3;
	private final static int MENU_SHARE_CONTENT = 4;
	private final static int MENU_SPEAKER = 5;

	private static boolean SHOW_SIP_PHRASE = true;

	private static enum ViewType {
		ViewNone, ViewTrying, ViewInCall, ViewProxSensor, ViewTermwait
	}
    private String mId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_av);
		Log.d("wang", "oncreate...AVActivity");
		mCurrentView = ViewType.ViewNone;

		mTimerInCall = new NgnTimer();
		mTimerSuicide = new NgnTimer();
		mTimerBlankPacket = new NgnTimer();

		mId = getIntent().getStringExtra("id");
		if (NgnStringUtils.isNullOrEmpty(mId)) {
			Log.e("wang", "Invalid audio/video session");
			finish();
			return;
		}
		mAVSession = NgnAVSession.getSession(NgnStringUtils.parseLong(mId, -1));
		if (mAVSession == null) {
			finish();
			return;
		}
		mAVSession.incRef();
		mAVSession.setContext(this);

		final NgnContact remoteParty = Engine.getInstance().getContactService().getContactByUri(mAVSession.getRemotePartyUri());
		if (remoteParty != null) {
			mRemotePartyDisplayName = remoteParty.getDisplayName();
			if ((mRemotePartyPhoto = remoteParty.getPhoto()) != null) {
				mRemotePartyPhoto = NgnGraphicsUtils.getResizedBitmap(mRemotePartyPhoto,
						NgnGraphicsUtils.getSizeInPixel(128), NgnGraphicsUtils.getSizeInPixel(128));
			}
		} else {
			mRemotePartyDisplayName = NgnUriUtils.getDisplayName(mAVSession.getRemotePartyUri());
		}
		if (NgnStringUtils.isNullOrEmpty(mRemotePartyDisplayName)) {
			mRemotePartyDisplayName = "Unknown";
		}

		mIsVideoCall = mAVSession.getMediaType() == NgnMediaType.AudioVideo
				|| mAVSession.getMediaType() == NgnMediaType.Video;

		mSendDeviceInfo = Engine.getInstance().getConfigurationService().getBoolean(
				NgnConfigurationEntry.GENERAL_SEND_DEVICE_INFO, NgnConfigurationEntry.DEFAULT_GENERAL_SEND_DEVICE_INFO);
		mCountBlankPacket = 0;
		mLastRotation = -1;
		mLastOrientation = -1;

		mInflater = LayoutInflater.from(this);

		mBroadCastRecv = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(intent.getAction())) {
					handleSipEvent(intent);
				} else if (NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT.equals(intent.getAction())) {
					handleMediaEvent(intent);
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
		intentFilter.addAction(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT);
		registerReceiver(mBroadCastRecv, intentFilter);

		if (mIsVideoCall) {
			mListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
				@Override
				public void onOrientationChanged(int orient) {
					try {
						if ((orient > 345 || orient < 15) || (orient > 75 && orient < 105)
								|| (orient > 165 && orient < 195) || (orient > 255 && orient < 285)) {
							int rotation = mAVSession.compensCamRotation(true);
							if (rotation != mLastRotation) {
							/*	Log.d(ScreenAV.TAG, "Received Screen Orientation Change setRotation["
										+ String.valueOf(rotation) + "]");*/
								applyCamRotation(rotation);
								if (mSendDeviceInfo && mAVSession != null) {
									final android.content.res.Configuration conf = getResources().getConfiguration();
									if (conf.orientation != mLastOrientation) {
										mLastOrientation = conf.orientation;
										switch (mLastOrientation) {
										case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
											mAVSession.sendInfo("orientation:landscape\r\nlang:fr-FR\r\n",
													NgnContentType.DOUBANGO_DEVICE_INFO);
											break;
										case android.content.res.Configuration.ORIENTATION_PORTRAIT:
											mAVSession.sendInfo("orientation:portrait\r\nlang:fr-FR\r\n",
													NgnContentType.DOUBANGO_DEVICE_INFO);
											break;
										}
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			if (!mListener.canDetectOrientation()) {
			}
		}

		mMainLayout = (RelativeLayout) findViewById(R.id.screen_av_relativeLayout);
		loadView();

		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}

	@Override
	protected void onStart() {
		super.onStart();

		final KeyguardManager keyguardManager = ImsApplication.getKeyguardManager();
		if (keyguardManager != null) {
			if (mKeyguardLock == null) {
			}
			if (keyguardManager.inKeyguardRestrictedInputMode()) {
				mKeyguardLock.disableKeyguard();
			}
		}

		final PowerManager powerManager = ImsApplication.getPowerManager();
		if (powerManager != null && mWakeLock == null) {
			mWakeLock = powerManager.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.SCREEN_BRIGHT_WAKE_LOCK
					| PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
			if (mWakeLock != null) {
				mWakeLock.acquire();
			}
		}

		if (mProxSensor == null && !ImsApplication.isBuggyProximitySensor()) {
			mProxSensor = new MyProxSensor(AVActivity.this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mProxSensor != null) {
			mProxSensor.stop();
		}

		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}

		if (mListener != null && mListener.canDetectOrientation()) {
			mListener.disable();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mProxSensor != null) {
			mProxSensor.start();
		}

		if (mAVSession != null) {
			if (mAVSession.getState() == InviteState.INCALL) {
				mTimerInCall.schedule(mTimerTaskInCall, 0, 1000);
			}
		}

		if (mListener != null && mListener.canDetectOrientation()) {
			mListener.enable();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mKeyguardLock != null) {
			mKeyguardLock.reenableKeyguard();
		}
	}

	@Override
	protected void onDestroy() {
		if (mBroadCastRecv != null) {
			unregisterReceiver(mBroadCastRecv);
			mBroadCastRecv = null;
		}

		mTimerInCall.cancel();
		mTimerSuicide.cancel();
		cancelBlankPacket();

		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		mWakeLock = null;

		if (mAVSession != null) {
			mAVSession.setContext(null);
			mAVSession.decRef();
		}
		super.onDestroy();
	}
	protected String getPath(Uri uri) {
    	try{
	        String[] projection = { MediaStore.Images.Media.DATA };
	        Cursor cursor = managedQuery(uri, projection, null, null, null);
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	        final String path = cursor.getString(column_index);
	        cursor.close();
	        return path;
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		return null;
    	}
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case SELECT_CONTENT:
				if (mAVSession != null) {
					Uri selectedContentUri = data.getData();
					String selectedContentPath = getPath(selectedContentUri);
					ScreenFileTransferView.sendFile(mAVSession.getRemotePartyUri(), selectedContentPath);
				}
				break;
			}
		}
	}


	public boolean onVolumeChanged(boolean bDown) {
		if (mAVSession != null) {
			return mAVSession.onVolumeChanged(bDown);
		}
		return false;
	}

	public static boolean receiveCall(NgnAVSession avSession) {
		((Engine) Engine.getInstance()).getScreenService().bringToFront(Main.ACTION_SHOW_AVSCREEN,
				new String[] { "session-id", Long.toString(avSession.getId()) });
		return true;
	}

	private void applyCamRotation(int rotation) {
		if (mAVSession != null) {
			mLastRotation = rotation;
			// libYUV
			mAVSession.setRotation(rotation);

			// FFmpeg
			/*
			 * switch (rotation) { case 0: case 90:
			 * mAVSession.setRotation(rotation);
			 * mAVSession.setProducerFlipped(false); break; case 180:
			 * mAVSession.setRotation(0); mAVSession.setProducerFlipped(true);
			 * break; case 270: mAVSession.setRotation(90);
			 * mAVSession.setProducerFlipped(true); break; }
			 */
		}
	}

	private boolean hangUpCall() {
		if (mAVSession != null) {
			return mAVSession.hangUpCall();
		}
		return false;
	}

	private boolean acceptCall() {
		if (mAVSession != null) {
			return mAVSession.acceptCall();
		}
		return false;
	}

	private void handleMediaEvent(Intent intent) {
		final String action = intent.getAction();

		if (NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT.equals(action)) {
			NgnMediaPluginEventArgs args = intent.getParcelableExtra(NgnMediaPluginEventArgs.EXTRA_EMBEDDED);
			if (args == null) {
				return;
			}

			switch (args.getEventType()) {
			case STARTED_OK: // started or restarted (e.g. reINVITE)
			{
				mIsVideoCall = (mAVSession.getMediaType() == NgnMediaType.AudioVideo
						|| mAVSession.getMediaType() == NgnMediaType.Video);
				loadView();

				break;
			}
			case PREPARED_OK:
			case PREPARED_NOK:
			case STARTED_NOK:
			case STOPPED_OK:
			case STOPPED_NOK:
			case PAUSED_OK:
			case PAUSED_NOK: {
				break;
			}
			}
		}
	}

	private void handleSipEvent(Intent intent) {
		@SuppressWarnings("unused")
		InviteState state;
		if (mAVSession == null) {
			return;
		}
		final String action = intent.getAction();
		if (NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)) {
			NgnInviteEventArgs args = intent.getParcelableExtra(NgnInviteEventArgs.EXTRA_EMBEDDED);
			if (args == null) {
				return;
			}
			if (args.getSessionId() != mAVSession.getId()) {
				if (args.getEventType() == NgnInviteEventTypes.REMOTE_TRANSFER_INPROGESS) {
					// Native code created new session handle to be used to
					// replace the current one (event = "tsip_i_ect_newcall").
					mAVTransfSession = NgnAVSession.getSession(args.getSessionId());
				}
				return;
			}

			switch ((state = mAVSession.getState())) {
			case NONE:
			default:
				break;

			case INCOMING:
			case INPROGRESS:
			case REMOTE_RINGING:
				loadTryingView();
				break;

			case EARLY_MEDIA:
			case INCALL:
				Engine.getInstance().getSoundService().stopRingTone();
				mAVSession.setSpeakerphoneOn(false);
				loadInCallView();
				if (mAVSession != null) {
					applyCamRotation(mAVSession.compensCamRotation(true));
					mTimerBlankPacket.schedule(mTimerTaskBlankPacket, 0, 250);
					if (!mIsVideoCall) {
						mTimerInCall.schedule(mTimerTaskInCall, 0, 1000);
					}
				}

				// release power lock if not video call
				if (!mIsVideoCall && mWakeLock != null && mWakeLock.isHeld()) {
					mWakeLock.release();
				}

				switch (args.getEventType()) {
				case REMOTE_DEVICE_INFO_CHANGED: {
					Log.d("wang", String.format("Remote device info changed: orientation: %s",
							mAVSession.getRemoteDeviceInfo().getOrientation()));
					break;
				}
				case MEDIA_UPDATED: {
					if ((mIsVideoCall = (mAVSession.getMediaType() == NgnMediaType.AudioVideo
							|| mAVSession.getMediaType() == NgnMediaType.Video))) {
						loadInCallVideoView();
					} else {
						loadInCallAudioView();
					}
					break;
				}
				case LOCAL_TRANSFER_TRYING: {
					if (mTvInfo != null) {
						mTvInfo.setText("Call Transfer: Initiated");
					}
					break;
				}
				case LOCAL_TRANSFER_FAILED: {
					if (mTvInfo != null) {
						mTvInfo.setText("Call Transfer: Failed");
					}
					break;
				}
				case LOCAL_TRANSFER_ACCEPTED: {
					if (mTvInfo != null) {
						mTvInfo.setText("Call Transfer: Accepted");
					}
					break;
				}
				case LOCAL_TRANSFER_COMPLETED: {
					if (mTvInfo != null) {
						mTvInfo.setText("Call Transfer: Completed");
					}
					break;
				}
				case LOCAL_TRANSFER_NOTIFY:
				case REMOTE_TRANSFER_NOTIFY: {
					if (mTvInfo != null && mAVSession != null) {
						short sipCode = intent.getShortExtra(NgnInviteEventArgs.EXTRA_SIPCODE, (short) 0);

						mTvInfo.setText("Call Transfer: " + sipCode + " " + args.getPhrase());
						if (sipCode >= 300 && mAVSession.isLocalHeld()) {
							mAVSession.resumeCall();
						}
					}
					break;
				}

				case REMOTE_TRANSFER_REQUESTED: {
					String referToUri = intent.getStringExtra(NgnInviteEventArgs.EXTRA_REFERTO_URI);
					if (!NgnStringUtils.isNullOrEmpty(referToUri)) {
						String referToName = NgnUriUtils.getDisplayName(referToUri);
						if (!NgnStringUtils.isNullOrEmpty(referToName)) {
							mTransferDialog = CustomDialog.create(AVActivity.this, R.drawable.exit_48, null,
									"Call Transfer to " + referToName + " requested. Do you accept?", "Yes",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.cancel();
											mTransferDialog = null;
											if (mAVSession != null) {
												mAVSession.acceptCallTransfer();
											}
										}
									}, "No", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.cancel();
											mTransferDialog = null;
											if (mAVSession != null) {
												mAVSession.rejectCallTransfer();
											}
										}
									});
							mTransferDialog.show();
						}
					}
					break;
				}

				case REMOTE_TRANSFER_FAILED: {
					if (mTransferDialog != null) {
						mTransferDialog.cancel();
						mTransferDialog = null;
					}
					mAVTransfSession = null;
					break;
				}
				case REMOTE_TRANSFER_COMPLETED: {
					if (mTransferDialog != null) {
						mTransferDialog.cancel();
						mTransferDialog = null;
					}
					if (mAVTransfSession != null) {
						mAVTransfSession.setContext(mAVSession.getContext());
						mAVSession = mAVTransfSession;
						mAVTransfSession = null;
						loadInCallView(true);
					}
					break;
				}
				default: {
					break;
				}
				}
				break;

			case TERMINATING:
			case TERMINATED:
				if (mTransferDialog != null) {
					mTransferDialog.cancel();
					mTransferDialog = null;
				}
				mTimerTaskInCall.cancel();
				mTimerBlankPacket.cancel();
				loadTermView(SHOW_SIP_PHRASE ? args.getPhrase() : null);

				// release power lock
				if (mWakeLock != null && mWakeLock.isHeld()) {
					mWakeLock.release();
				}
				break;
			}
		}
	}

	private void loadView() {
		switch (mAVSession.getState()) {
		case INCOMING:
		case INPROGRESS:
		case REMOTE_RINGING:
			loadTryingView();
			break;

		case INCALL:
		case EARLY_MEDIA:
			loadInCallView();
			break;

		case NONE:
		case TERMINATING:
		case TERMINATED:
		default:
			loadTermView();
			break;
		}
	}

	private void loadTryingView() {
		if (mCurrentView == ViewType.ViewTrying) {
			return;
		}
		Log.d("wang", "loadTryingView()");

		if (mViewTrying == null) {
			mViewTrying = mInflater.inflate(R.layout.view_call_trying, null);
			loadKeyboard(mViewTrying);
		}
		mTvInfo = (TextView) mViewTrying.findViewById(R.id.view_call_trying_textView_info);

		final TextView tvRemote = (TextView) mViewTrying.findViewById(R.id.view_call_trying_textView_remote);
		final ImageButton btPick = (ImageButton) mViewTrying.findViewById(R.id.view_call_trying_imageButton_pick);
		final ImageButton btHang = (ImageButton) mViewTrying.findViewById(R.id.view_call_trying_imageButton_hang);
		final ImageView ivAvatar = (ImageView) mViewTrying.findViewById(R.id.view_call_trying_imageView_avatar);
		btPick.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				acceptCall();
			}
		});
		btHang.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hangUpCall();
			}
		});

		switch (mAVSession.getState()) {
		case INCOMING:
			mTvInfo.setText(getString(R.string.string_call_incoming));
			break;
		case INPROGRESS:
		case REMOTE_RINGING:
		case EARLY_MEDIA:
		default:
			mTvInfo.setText(getString(R.string.string_call_outgoing));
			btPick.setVisibility(View.GONE);
			break;
		}

		tvRemote.setText(mRemotePartyDisplayName);
		if (mRemotePartyPhoto != null) {
			ivAvatar.setImageBitmap(mRemotePartyPhoto);
		}

		mMainLayout.removeAllViews();
		mMainLayout.addView(mViewTrying);
		mCurrentView = ViewType.ViewTrying;
	}

	private void loadInCallAudioView() {
		Log.d("wang", "loadInCallAudioView()");
		if (mViewInAudioCall == null) {
			mViewInAudioCall = mInflater.inflate(R.layout.view_call_incall_audio, null);
			loadKeyboard(mViewInAudioCall);
		}
		mTvInfo = (TextView) mViewInAudioCall.findViewById(R.id.view_call_incall_audio_textView_info);

		final TextView tvRemote = (TextView) mViewInAudioCall.findViewById(R.id.view_call_incall_audio_textView_remote);
		final ImageButton btHang = (ImageButton) mViewInAudioCall
				.findViewById(R.id.view_call_incall_audio_imageButton_hang);
		final ImageView ivAvatar = (ImageView) mViewInAudioCall
				.findViewById(R.id.view_call_incall_audio_imageView_avatar);
		mTvDuration = (TextView) mViewInAudioCall.findViewById(R.id.view_call_incall_audio_textView_duration);

		btHang.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hangUpCall();
			}
		});

		tvRemote.setText(mRemotePartyDisplayName);
		if (mRemotePartyPhoto != null) {
			ivAvatar.setImageBitmap(mRemotePartyPhoto);
		}
		mTvInfo.setText(getString(R.string.string_incall));

		mViewInAudioCall.findViewById(R.id.view_call_incall_audio_imageView_secure)
				.setVisibility(mAVSession.isSecure() ? View.VISIBLE : View.INVISIBLE);

		mMainLayout.removeAllViews();
		mMainLayout.addView(mViewInAudioCall);
		mCurrentView = ViewType.ViewInCall;
	}

	private void loadInCallVideoView() {
		Log.d("wang", "loadInCallVideoView()");
		if (mViewInCallVideo == null) {
			mViewInCallVideo = mInflater.inflate(R.layout.view_call_incall_video, null);
			mViewLocalVideoPreview = (FrameLayout) mViewInCallVideo
					.findViewById(R.id.view_call_incall_video_FrameLayout_local_video);
			mViewRemoteVideoPreview = (FrameLayout) mViewInCallVideo
					.findViewById(R.id.view_call_incall_video_FrameLayout_remote_video);
		}
		if (mTvDuration != null) {
			synchronized (mTvDuration) {
				mTvDuration = null;
			}
		}
		mTvInfo = null;
		mMainLayout.removeAllViews();
		mMainLayout.addView(mViewInCallVideo);

		final View viewSecure = mViewInCallVideo.findViewById(R.id.view_call_incall_video_imageView_secure);
		if (viewSecure != null) {
			viewSecure.setVisibility(mAVSession.isSecure() ? View.VISIBLE : View.INVISIBLE);
		}

		// Video Consumer
		loadVideoPreview();

		// Video Producer
		startStopVideo(mAVSession.isSendingVideo());

		mCurrentView = ViewType.ViewInCall;
	}

	private void loadInCallView(boolean force) {
		if (mCurrentView == ViewType.ViewInCall && !force) {
			return;
		}
		Log.d("wang", "loadInCallView()");

		if (mIsVideoCall) {
			loadInCallVideoView();
		} else {
			loadInCallAudioView();
		}
	}

	private void loadInCallView() {
		loadInCallView(false);
	}

	private void loadProxSensorView() {
		if (mCurrentView == ViewType.ViewProxSensor) {
			return;
		}
		Log.d("wang", "loadProxSensorView()");
		if (mViewProxSensor == null) {
			mViewProxSensor = mInflater.inflate(R.layout.view_call_proxsensor, null);
		}
		mMainLayout.removeAllViews();
		mMainLayout.addView(mViewProxSensor);
		mCurrentView = ViewType.ViewProxSensor;
	}

	private void loadTermView(String phrase) {
		Log.d("wang", "loadTermView()");

		if (mViewTermwait == null) {
			mViewTermwait = mInflater.inflate(R.layout.view_call_trying, null);
			loadKeyboard(mViewTermwait);
		}
		mTvInfo = (TextView) mViewTermwait.findViewById(R.id.view_call_trying_textView_info);
		mTvInfo.setText(NgnStringUtils.isNullOrEmpty(phrase) ? getString(R.string.string_call_terminated) : phrase);

		// loadTermView() could be called twice (onTermwait() and OnTerminated)
		// and this is why we need to
		// update the info text for both
		if (mCurrentView == ViewType.ViewTermwait) {
			return;
		}

		final TextView tvRemote = (TextView) mViewTermwait.findViewById(R.id.view_call_trying_textView_remote);
		final ImageView ivAvatar = (ImageView) mViewTermwait.findViewById(R.id.view_call_trying_imageView_avatar);
		mViewTermwait.findViewById(R.id.view_call_trying_imageButton_pick).setVisibility(View.GONE);
		mViewTermwait.findViewById(R.id.view_call_trying_imageButton_hang).setVisibility(View.GONE);
		mViewTermwait.setBackgroundResource(R.drawable.grad_bkg_termwait);

		tvRemote.setText(mRemotePartyDisplayName);
		if (mRemotePartyPhoto != null) {
			ivAvatar.setImageBitmap(mRemotePartyPhoto);
		}

		mMainLayout.removeAllViews();
		mMainLayout.addView(mViewTermwait);
		mCurrentView = ViewType.ViewTermwait;
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				AVActivity.this.finish();
			}
		}, 1000);
	}

	private void loadTermView() {
		loadTermView(null);
	}

	private void loadVideoPreview() {
		mViewRemoteVideoPreview.removeAllViews();
		final View remotePreview = mAVSession.startVideoConsumerPreview();
		if (remotePreview != null) {
			final ViewParent viewParent = remotePreview.getParent();
			if (viewParent != null && viewParent instanceof ViewGroup) {
				((ViewGroup) (viewParent)).removeView(remotePreview);
			}
			mViewRemoteVideoPreview.addView(remotePreview);
		}
	}

	private final TimerTask mTimerTaskInCall = new TimerTask() {
		@Override
		public void run() {
			if (mAVSession != null && mTvDuration != null) {
				synchronized (mTvDuration) {
					final Date date = new Date(new Date().getTime() - mAVSession.getStartTime());
					AVActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							try {
								mTvDuration.setText(sDurationTimerFormat.format(date));
							} catch (Exception e) {
							}
						}
					});
				}
			}
		}
	};

	private final TimerTask mTimerTaskBlankPacket = new TimerTask() {
		@Override
		public void run() {
			Log.d("wang", "Resending Blank Packet " + String.valueOf(mCountBlankPacket));
			if (mCountBlankPacket < 3) {
				if (mAVSession != null) {
					mAVSession.pushBlankPacket();
				}
				mCountBlankPacket++;
			} else {
				cancel();
				mCountBlankPacket = 0;
			}
		}
	};

	private void cancelBlankPacket() {
		if (mTimerBlankPacket != null) {
			mTimerBlankPacket.cancel();
			mCountBlankPacket = 0;
		}
	}

	private void startStopVideo(boolean bStart) {
		Log.d("wang", "startStopVideo(" + bStart + ")");
		if (!mIsVideoCall) {
			return;
		}

		mAVSession.setSendingVideo(bStart);

		if (mViewLocalVideoPreview != null) {
			mViewLocalVideoPreview.removeAllViews();
			if (bStart) {
				cancelBlankPacket();
				final View localPreview = mAVSession.startVideoProducerPreview();
				if (localPreview != null) {
					final ViewParent viewParent = localPreview.getParent();
					if (viewParent != null && viewParent instanceof ViewGroup) {
						((ViewGroup) (viewParent)).removeView(localPreview);
					}
					if (localPreview instanceof SurfaceView) {
						((SurfaceView) localPreview).setZOrderOnTop(true);
					}
					mViewLocalVideoPreview.addView(localPreview);
					mViewLocalVideoPreview.bringChildToFront(localPreview);
				}
			}
			mViewLocalVideoPreview.setVisibility(bStart ? View.VISIBLE : View.GONE);
			mViewLocalVideoPreview.bringToFront();
		}
	}

	private View.OnClickListener mOnKeyboardClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mAVSession != null) {
				mAVSession.sendDTMF(NgnStringUtils.parseInt(v.getTag().toString(), -1));
			}
		}
	};

	private void loadKeyboard(View view) {
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_0), "0", "+", DialerUtils.TAG_0,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_1), "1", "", DialerUtils.TAG_1,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_2), "2", "ABC", DialerUtils.TAG_2,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_3), "3", "DEF", DialerUtils.TAG_3,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_4), "4", "GHI", DialerUtils.TAG_4,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_5), "5", "JKL", DialerUtils.TAG_5,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_6), "6", "MNO", DialerUtils.TAG_6,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_7), "7", "PQRS", DialerUtils.TAG_7,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_8), "8", "TUV", DialerUtils.TAG_8,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_9), "9", "WXYZ", DialerUtils.TAG_9,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_star), "*", "", DialerUtils.TAG_STAR,
				mOnKeyboardClickListener);
		DialerUtils.setDialerTextButton(view.findViewById(R.id.view_dialer_buttons_sharp), "#", "",
				DialerUtils.TAG_SHARP, mOnKeyboardClickListener);
	}

	/**
	 * MyProxSensor
	 */
	static class MyProxSensor implements SensorEventListener {
		private final SensorManager mSensorManager;
		private Sensor mSensor;
		private final AVActivity mAVScreen;
		private float mMaxRange;

		MyProxSensor(AVActivity avScreen) {
			mAVScreen = avScreen;
			mSensorManager = ImsApplication.getSensorManager();
		}

		void start() {
			if (mSensorManager != null && mSensor == null) {
				if ((mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)) != null) {
					mMaxRange = mSensor.getMaximumRange();
					mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
				}
			}
		}

		void stop() {
			if (mSensorManager != null && mSensor != null) {
				mSensorManager.unregisterListener(this);
				mSensor = null;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			try { // Keep it until we get a phone supporting this feature
				if (mAVScreen == null) {
					return;
				}
				if (event.values != null && event.values.length > 0) {
					if (event.values[0] < this.mMaxRange) {
						mAVScreen.loadProxSensorView();
					} else {
						mAVScreen.loadView();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
