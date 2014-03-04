package org.g_oku.intruderdetection;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class IntruderDetectionActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intruder_detection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.intruder_detection, menu);
		return true;
	}

}
