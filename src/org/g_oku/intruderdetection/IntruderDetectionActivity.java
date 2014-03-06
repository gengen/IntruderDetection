package org.g_oku.intruderdetection;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class IntruderDetectionActivity extends PreferenceActivity {
	public static final String TAG = "IntruderDetection";
	public static final boolean DEBUG = true;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//フロントカメラ検出
		if(!checkFrontCamera()){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.dialog_error_title));
			builder.setMessage(getString(R.string.dialog_error_message));
		    builder.setPositiveButton(R.string.dialog_error_ok, new DialogInterface.OnClickListener() {
		    	public void onClick(DialogInterface dialog, int whichButton) {
		    		finish();
		    	}
		    });
		    AlertDialog dialog = builder.show();
		    //ダイアログ画面外を押された際に閉じないように設定
		    dialog.setCanceledOnTouchOutside(false);
		    return;
		}
		
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			addPreferencesFromResource(R.xml.preference);
		    PreferenceScreen nextMove1 = (PreferenceScreen)findPreference("gallery");
		    nextMove1.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		        @Override
		        public boolean onPreferenceClick(Preference preference) {
		        	if(DEBUG){
		        		Log.d(TAG, "onPreferenceClick");
		        	}
		        	startGalleryPreGB();
		            return true;
		        }
		    });
		}
		else{
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					new SettingFragment()).commit();
		}
	}
	
	private boolean checkFrontCamera(){
		int num = Camera.getNumberOfCameras();
		for(int i=0; i<num; i++){
			CameraInfo caminfo = new CameraInfo();
			Camera.getCameraInfo(i, caminfo);
		
			if(caminfo.facing == CameraInfo.CAMERA_FACING_FRONT){
				return true;
			}
		}
		
		return false;
	}
	 
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class SettingFragment extends PreferenceFragment{
		@Override
		public void onCreate(Bundle savedInstanceState){
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preference);
			
		    PreferenceScreen nextMove1 = (PreferenceScreen)findPreference("gallery");
		    nextMove1.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		        @Override
		        public boolean onPreferenceClick(Preference preference) {
		        	Log.d(TAG, "onPreferenceClick");
		        	startGallery();
		            return true;
		        }
		    });
		}
		
	    private void startGallery(){
	    	// ギャラリー表示
	    	Intent intent = null;
	    	try{
	    	    // for Honeycomb
	    	    intent = new Intent();
	    	    intent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.Gallery");
	    	    startActivity(intent);
	    	    return;
	    	}
	    	catch(Exception e){
	    	    try{
	    	        // for Recent device
	    	        intent = new Intent();
	    	        intent.setClassName("com.cooliris.media", "com.cooliris.media.Gallery");
	    	        startActivity(intent);
	    	    }
	    	    catch(ActivityNotFoundException e1){
	    	        try
	    	        {
	    	            // for Other device except HTC
	    	            intent = new Intent(Intent.ACTION_VIEW);
	    	            intent.setData(Uri.parse("content://media/external/images/media"));
	    	            startActivity(intent);
	    	        }
	    	        catch (ActivityNotFoundException e2){
	    	        	try{
	    	        		// for HTC
	    	        		intent = new Intent();
	    	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumTabSwitchActivity");
	    	        		startActivity(intent);
	    	        	}
	    	        	catch(ActivityNotFoundException e3){
	        	        	try{
	        	        		// for HTC
	        	        		intent = new Intent();
	        	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumMain.ActivityMainDropList");
	        	        		startActivity(intent);
	        	        	}
	        	        	catch(ActivityNotFoundException e4){
	        	    	    	intent = new Intent(Intent.ACTION_PICK);
	        	    	    	intent.setType("image/*");
	        	    	    	startActivity(intent);
	        	        	}
	    	        	}
	    	        }
	    	    }
	    	}
	    }
	}
	
    public static boolean isDetect(Context c){
    	return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("switch", false);
    }
    
    private void startGalleryPreGB(){
    	// ギャラリー表示
    	Intent intent = null;
    	try{
    	    // for Honeycomb
    	    intent = new Intent();
    	    intent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.Gallery");
    	    startActivity(intent);
    	    return;
    	}
    	catch(Exception e){
    	    try{
    	        // for Recent device
    	        intent = new Intent();
    	        intent.setClassName("com.cooliris.media", "com.cooliris.media.Gallery");
    	        startActivity(intent);
    	    }
    	    catch(ActivityNotFoundException e1){
    	        try
    	        {
    	            // for Other device except HTC
    	            intent = new Intent(Intent.ACTION_VIEW);
    	            intent.setData(Uri.parse("content://media/external/images/media"));
    	            startActivity(intent);
    	        }
    	        catch (ActivityNotFoundException e2){
    	        	try{
    	        		// for HTC
    	        		intent = new Intent();
    	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumTabSwitchActivity");
    	        		startActivity(intent);
    	        	}
    	        	catch(ActivityNotFoundException e3){
        	        	try{
        	        		// for HTC
        	        		intent = new Intent();
        	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumMain.ActivityMainDropList");
        	        		startActivity(intent);
        	        	}
        	        	catch(ActivityNotFoundException e4){
        	    	    	intent = new Intent(Intent.ACTION_PICK);
        	    	    	intent.setType("image/*");
        	    	    	startActivity(intent);
        	        	}
    	        	}
    	        }
    	    }
    	}
    }
}
