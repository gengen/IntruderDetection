package org.g_oku.intruderdetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PresentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	//監視用サービス起動
    	Intent i = new Intent(context, WatchService.class);
    	context.startService(i);
    }
}
