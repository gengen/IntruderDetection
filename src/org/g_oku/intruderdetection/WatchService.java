package org.g_oku.intruderdetection;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;

public class WatchService extends Service {
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowMgr;
    private View mOverLayView;
    
    private int mID;
    
    SurfaceHolder mHolder;
    SurfaceView mSurface;
    private CameraPreview mPreview;
    
    private ContentResolver mResolver;

	@Override
	public void onCreate(){
	}

	@Override
	public void onStart(Intent intent, int startId) {
		mID = startId;
		setLayer();
		//TODO Notificationへの表示は要検討
		//displayNotificationArea();
        mResolver = getApplicationContext().getContentResolver();

		mSurface = (SurfaceView)mOverLayView.findViewById(R.id.camera);
        mHolder = mSurface.getHolder();

        mPreview = new CameraPreview(this);
        mHolder.addCallback(mPreview);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	private void setLayer(){
        mWindowMgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        mOverLayView = layoutInflater.inflate(R.layout.overlay, null);
        mParams = new WindowManager.LayoutParams(
                1, 1, 
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        mWindowMgr.addView(mOverLayView, mParams);
	}
	
    private void displayNotificationArea(){
        Intent intent = new Intent(getApplicationContext(), IntruderDetectionActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
        		getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext());
        builder.setContentIntent(contentIntent);
        builder.setTicker(getString(R.string.app_name));
        //builder.setSmallIcon(R.drawable.ic_lock_lock);
        builder.setContentTitle(getString(R.string.app_name));
        //builder.setContentText(getString(R.string.notification_message));
        //builder.setOngoing(true);
        
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(R.string.app_name, builder.build());
    }
    
    public void saveGallery(ContentValues values){
		mResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public void stop(){
		mWindowMgr.removeView(mOverLayView);
		mWindowMgr = null;
		mOverLayView = null;
		stopSelf(mID);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mWindowMgr != null && mOverLayView != null){
			mWindowMgr.removeView(mOverLayView);
		}

		/*
		//Notificationを非表示
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(R.string.app_name);
        */
	}
}
