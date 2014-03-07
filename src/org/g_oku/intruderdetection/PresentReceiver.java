package org.g_oku.intruderdetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PresentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
		boolean flag = IntruderDetectionActivity.isDetect(context);

		if(IntruderDetectionActivity.DEBUG){
			String switchFlag = flag ? "on" : "off";
			//Log.d(IntruderDetectionActivity.TAG, "flag = " + switchFlag);
		}

		if(flag){
	    	//監視用サービス起動
	    	Intent i = new Intent(context, WatchService.class);
	    	context.startService(i);
		}		
    }
}
