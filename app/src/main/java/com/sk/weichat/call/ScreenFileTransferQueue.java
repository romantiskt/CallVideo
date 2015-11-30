package com.sk.weichat.call;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yang.wang.audiovoiceproject.R;

import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.sip.NgnMsrpSession;
import org.doubango.ngn.utils.NgnListUtils;
import org.doubango.ngn.utils.NgnPredicate;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.ngn.utils.NgnUriUtils;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午4:59:11
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class ScreenFileTransferQueue extends BaseScreen{
	private static final String TAG = ScreenFileTransferQueue.class.getCanonicalName();
	
	private ListView mListView;
	private ScreenFileTransferQueueAdapter mAdapter;
	
	public ScreenFileTransferQueue() {
		super(SCREEN_TYPE.FILETRANSFER_QUEUE_T, TAG);		
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_filetrans_queue);
        
        mListView = (ListView)findViewById(R.id.screen_filetrans_queue_listView);
        mAdapter = new ScreenFileTransferQueueAdapter(this);
		mListView.setAdapter(mAdapter);
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final NgnMsrpSession session = (NgnMsrpSession)mAdapter.getItem(position);
				if(session != null){
					if(mScreenService.show(ScreenFileTransferView.class, new Long(session.getId()).toString())){
						mScreenService.destroy(getId());
					}
				}
			}
		});
	}
	
	@Override
	public boolean hasBack(){
		return true;
	}
	
	@Override
	public boolean back(){
		boolean ret =  mScreenService.back();
		if(ret){
			mScreenService.destroy(getId());
		}
		return ret;
	}
	
	//
	// ScreenFileTransferQueueAdapter
	//
	static class ScreenFileTransferQueueAdapter extends BaseAdapter implements Observer {
		private List<NgnMsrpSession> mSessions;
		private final NgnPredicate<NgnMsrpSession> mFilter;
		private final Handler mHandler;
		private final ScreenFileTransferQueue mBaseScreen;
		private final LayoutInflater mInflater;
		
		ScreenFileTransferQueueAdapter(ScreenFileTransferQueue baseScreen){
			mBaseScreen = baseScreen;
			mHandler = new Handler();
			mInflater = LayoutInflater.from(mBaseScreen);
			mFilter = new NgnPredicate<NgnMsrpSession>() {
				@Override
				public boolean apply(NgnMsrpSession session) {
					return session != null && session.isActive() && session.getMediaType() == NgnMediaType.FileTransfer;
				}
			};
			mSessions = NgnListUtils.filter(NgnMsrpSession.getSessions().values(), mFilter);
			NgnMsrpSession.getSessions().addObserver(this);
		}
		
		@Override
		protected void finalize() throws Throwable {
			NgnMsrpSession.getSessions().deleteObserver(this);
			super.finalize();
		}
		
		@Override
		public void update(Observable observable, Object data) {
			mSessions = NgnListUtils.filter(NgnMsrpSession.getSessions().values(), mFilter);
//			if(Thread.currentThread() == Looper.getMainLooper().getThread()){
//				notifyDataSetChanged();
//			}
//			else{
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						notifyDataSetChanged();
					}
				});
			//}
		}

		@Override
		public int getCount() {
			return mSessions.size();
		}

		@Override
		public Object getItem(int position) {
			return mSessions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			
			final NgnMsrpSession session = (NgnMsrpSession)getItem(position);
			if(session == null || session.getMediaType() != NgnMediaType.FileTransfer){
				Log.e(TAG, "Invalid MSRP session");
				return null;
			}
			if(view == null){
				view = mInflater.inflate(R.layout.screen_filetrans_queue_item, null);
			}
			final ImageView ivIcon = (ImageView) view .findViewById(R.id.screen_filetrans_queue_item_imageView);
            final TextView tvFileName = (TextView) view .findViewById(R.id.screen_filetrans_queue_item_textView_name);
            final TextView tvRemoteUri = (TextView) view .findViewById(R.id.screen_filetrans_queue_item_textView_remoteUri);
            final ProgressBar progressBar = (ProgressBar) view .findViewById(R.id.screen_filetrans_queue_item_progressBar);
            
            ivIcon.setImageResource(session.isOutgoing() ? R.drawable.document_up_48 : R.drawable.document_down_48);
            final String fileName = session.getFileName();
            if(fileName == null){
                tvFileName.setText("UNKNOWN.3GP");
            }
            else{
                tvFileName.setText(fileName);
            }
            String remoteParty = NgnUriUtils.getDisplayName(session.getRemotePartyUri());
            if(NgnStringUtils.isNullOrEmpty(remoteParty)){
            	remoteParty = NgnStringUtils.nullValue();
            }
            tvRemoteUri.setText(remoteParty);
            
			final long end = session.getEnd();
			final long total = session.getTotal();
			progressBar.setMax(100);
			if (end >= 0 && total > 0 && end <= total) {
				progressBar.setProgress((int) ((100 * end) / total));
				progressBar.setIndeterminate(false);
			} else {
				progressBar.setIndeterminate(true);
			}

			return view;
		}
	}
}
