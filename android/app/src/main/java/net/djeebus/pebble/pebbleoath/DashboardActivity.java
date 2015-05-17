package net.djeebus.pebble.pebbleoath;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ToggleButton;

public class DashboardActivity extends Activity {
	public final String TAG = "DashboardActivity";
	
	private ToggleButton _toggleButton;
	private Handler _timerHandler = new Handler();
	private Runnable _timerRunnable = new Runnable() {
		@Override 
		public void run() {
			boolean isServiceRunning = isServiceRunning();
			
			_toggleButton.setChecked(isServiceRunning);
			_toggleButton.setEnabled(true);
			
			_timerHandler.postDelayed(this, 1000);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		_toggleButton = (ToggleButton)findViewById(R.id.toggleService);
		_timerHandler.postDelayed(_timerRunnable, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.dashboard, menu);
		return true;
	}
	
	public void toggleService_onClick(View view) {
		ToggleButton button = (ToggleButton)view;
		if (!button.isEnabled()) {
			return;
		}
		
		if (button.isChecked()) {
			this.startLinkService();
		} else {
			this.stopLinkService();
		}
	}

	private boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (PebbleLinkService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		
	    return false;		
	}
	
	private void startLinkService() {
		Context context = this.getBaseContext();
		Intent startServiceIntent = new Intent(context, PebbleLinkService.class);
		
		Log.i(TAG, "Starting service");
		ComponentName component = context.startService(startServiceIntent);
		if (component == null) {
			Log.w(TAG, "Failed to start service: component is null");
		}
	}
	
	private void stopLinkService() {
		Context context = this.getBaseContext();		
		Intent stopServiceIntent = new Intent(context, PebbleLinkService.class);
		
		Log.i(TAG, "Stopping service");
		boolean result = context.stopService(stopServiceIntent);
		if (result) {
			Log.i(TAG, "Service successfully stopped");
		} else {
			Log.w(TAG, "Failed to stop service");
		}
	}
}
