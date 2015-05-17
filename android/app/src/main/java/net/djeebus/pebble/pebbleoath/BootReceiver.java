package net.djeebus.pebble.pebbleoath;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	private static final String TAG = "PebbleReceiver";
	
	private static final String BOOT_INTENT = 
			"android.intent.action.BOOT_COMPLETED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!BOOT_INTENT.equals(intent.getAction())) {
			Log.w(TAG, "Unexpected action" + intent.getAction());
			return;
		}
		
		Intent startService = new Intent(context, PebbleLinkService.class);
		
		Log.i(TAG, "Starting service");
		context.startService(startService);
	}
}
