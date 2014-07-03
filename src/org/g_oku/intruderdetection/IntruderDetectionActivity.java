package org.g_oku.intruderdetection;

import java.io.File;

import jp.beyond.sdk.Bead;
import jp.beyond.sdk.Bead.ContentsOrientation;

import org.jraf.android.backport.switchwidget.SwitchPreference;

import android.annotation.SuppressLint;
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
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class IntruderDetectionActivity extends PreferenceActivity {
	public static final String TAG = "IntruderDetection";
	public static final boolean DEBUG = false;
	
    //BEAD ad
    private Bead mBeadExit = null;
    
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
		    PreferenceScreen galleryPref = (PreferenceScreen)findPreference("gallery");
		    galleryPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		        @Override
		        public boolean onPreferenceClick(Preference preference) {
		        	if(DEBUG){
		        		Log.d(TAG, "onPreferenceClick");
		        	}
		        	startGalleryPreGB();
		            return true;
		        }
		    });
		    
		    SwitchPreference switchPref = (SwitchPreference)findPreference("switch");
		    switchPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if(DEBUG){
						Log.d(TAG, "value = " + ((SwitchPreference)preference).isChecked());
					}
					//OFFにした際にサービスを終了
					//なぜかOFFでtrueが帰る
					if(((SwitchPreference)preference).isChecked()){
						if(DEBUG){
							Log.d(TAG, "stop service");
						}						
				    	//監視用サービス終了
				    	Intent intent = new Intent(IntruderDetectionActivity.this, WatchService.class);
				    	stopService(intent);
					}
					
					return true;
				}
		    });
		}
		else{
			getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingFragment()).commit();
		}
		
        //BEAD ad
        mBeadExit = Bead. createExitInstance("df90e2a0ddfc86515ca26e04e660a066b57d3444454d80e1", ContentsOrientation.Auto);
        mBeadExit.requestAd(this);		
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
		//Context mContext;
		
		public SettingFragment(){
			//mContext = context;
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState){
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preference);
			
		    PreferenceScreen galleryPref = (PreferenceScreen)findPreference("gallery");
		    galleryPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		        @Override
		        public boolean onPreferenceClick(Preference preference) {
		        	if(DEBUG){
		        		Log.d(TAG, "onPreferenceClick");
		        	}
		        	startGallery();
		            return true;
		        }
		    });
		    
		    SwitchPreference switchPref = (SwitchPreference)findPreference("switch");
		    switchPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if(DEBUG){
						Log.d(TAG, "value = " + ((SwitchPreference)preference).isChecked());
					}
					//OFFにした際にサービスを終了
					//なぜかOFFでtrueが帰る
					if(((SwitchPreference)preference).isChecked()){
						if(DEBUG){
							Log.d(TAG, "stop service");
						}						
				    	//監視用サービス終了
				    	Intent intent = new Intent(getActivity(), WatchService.class);
				    	getActivity().stopService(intent);
					}
					
					return true;
				}
		    });
		    
		}
		
	    private void startGallery(){
	    	//自前のGalleryを呼ぶ
	    	Intent intent = new Intent(getActivity(), MyGalleryActivity.class);
	    	startActivity(intent);
	    }
	}
	
    public static boolean isDetect(Context c){
    	return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("switch", false);
    }
    
    //GB以前は自前のGalleryではなく標準のGalleryを呼ぶ
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getMenuInflater().inflate(R.menu.intruder_detection, menu);
		}
		return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if(DEBUG){
			Log.d(TAG, "onOptionsItemSelected");
		}
    	// Handle presses on the action bar items
    	switch (item.getItemId()) {
    	case R.id.action_settings:
    		Intent intent = new Intent(this, IntruderDetectionPreference.class);
    		startActivity(intent);
    		return true;

    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    public void onBackPressed(){
    	// 広告ダイアログ表示
    	mBeadExit.showAd(this);
    }

    @Override
    protected void onPause(){
    	super.onPause();
    }
    
    @Override
	public void onDestroy(){
    	super.onDestroy();
    	
    	// 広告終了
    	if(mBeadExit != null){
    		mBeadExit.endAd();
    	}
    	
    	deleteCache(getCacheDir());
    }

    public static boolean deleteCache(File dir) {
    	if(dir==null) {
    		return false;
    	}
    	if (dir.isDirectory()) {
    		String[] children = dir.list();
    		for (int i = 0; i < children.length; i++) {
    			boolean success = deleteCache(new File(dir, children[i]));
    			if (!success) {
    				return false;
    			}
    		}
    	}
    	return dir.delete();
    }
}
