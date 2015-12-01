package com.yang.wang.audiovoiceproject;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.sk.weichat.call.CallStateService;
import com.sk.weichat.call.Constants;
import com.sk.weichat.call.Main;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.utils.NgnConfigurationEntry;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private BroadcastReceiver mSipBroadCastRecv;

    private final NgnEngine mEngine;
    private final INgnConfigurationService mConfigurationService;
    private final INgnSipService mSipService;



    public final static String SIP_DOMAIN = "120.24.211.24";
    public final String SIP_USERNAME = "10000070";
    public final static String SIP_PASSWORD = "ms_2014_0528@sk!@#";
    public static String SIP_SERVER_HOST = "120.24.211.24";
    public final static int SIP_SERVER_PORT = 5060;// 端口

    public final static String EXTRAT_SIP_SESSION_ID = "SipSession";
    public MainActivity() throws Exception{
        mEngine = NgnEngine.getInstance();
        mConfigurationService = mEngine.getConfigurationService();
        mSipService = mEngine.getSipService();
    }
    private Button mBtVideo;
    private Button mBtAudio;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       initView();
        Intent i=new Intent(this,CallStateService.class);
        startService(i);

    }

    private void initView() {
        mBtAudio= (Button) findViewById(R.id.bt_audio);
        mBtVideo= (Button) findViewById(R.id.bt_video);
        mBtVideo.setOnClickListener(this);
        mBtAudio.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEngine.isStarted()) {
            mEngine.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mEngine.isStarted()) {
            if (mEngine.start()) {//Engine started

            } else {//Failed to start the engine
            }
        }
        // Register 注册sip
        if (mEngine.isStarted()) {
            if (!mSipService.isRegistered()) {
                // Set credentials
                mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, SIP_USERNAME);// id
                // 10000070
                mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU,
                        String.format("sip:%s@%s", SIP_USERNAME, SIP_DOMAIN));// public
                mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, SIP_PASSWORD);// 密码
                mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, SIP_SERVER_HOST);// 120.24.211.24
                mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, SIP_SERVER_PORT);// 5060
                mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM, SIP_DOMAIN);// 120.24.211.24
                mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_WIFI, true);
                mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G, true);
                mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G, true);

                // VERY IMPORTANT: Commit changes
                mConfigurationService.commit();
                // register (log in)
                mSipService.register(MainActivity.this);
            }
        }
    }

    @Override
    public void onClick(View v) {
       int id= v.getId();
       switch (id){
           case R.id.bt_audio:
               Intent intent1=new Intent(this,Main.class);
               intent1.putExtra(Constants.AUDIO_PHONENUMBER,"10000072");
               intent1.putExtra(Constants.IS_AUDIO_OR_VIDEO, true);//true为语音

               startActivity(intent1);
               break;
           case R.id.bt_video:
               Intent intent2=new Intent(MainActivity.this,Main.class);
               intent2.putExtra(Constants.AUDIO_PHONENUMBER, "10000072");//为对方的帐号
               intent2.putExtra(Constants.IS_AUDIO_OR_VIDEO, false);//true为语音
               startActivity(intent2);
               break;
       }
    }
}
