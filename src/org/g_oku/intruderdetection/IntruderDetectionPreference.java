package org.g_oku.intruderdetection;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class IntruderDetectionPreference extends PreferenceActivity {
    
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingFragment()).commit();
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
			addPreferencesFromResource(R.xml.intruder_preference);
		}
	}
	
    public static boolean isNotSave(Context c){
    	return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("save_gallery", false);
    }
}
