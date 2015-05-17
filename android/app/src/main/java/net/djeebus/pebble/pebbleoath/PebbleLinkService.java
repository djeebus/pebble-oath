package net.djeebus.pebble.pebbleoath;

import java.io.IOException;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class PebbleLinkService extends Service {

	public final String TAG = "PebbleLinkService";
	
	public final UUID APP_ID = UUID.fromString("8E5F0DC4-93A9-43D6-A79A-1ED0BFEF5F4F");

	public final String REMOTE_DATABASE_PATH = "/data/data/com.google.android.apps.authenticator2/databases/databases";
	public final String LOCAL_DATABASE_PATH = "authenticator2.db";

	public final int OATH_KEY_FETCH = 0;
	public final int OATH_KEY_APPEND = 1;
	public final int OATH_KEY_CODE = 2;
	public final int OATH_KEY_EXPIRATION = 3;
	public final int OATH_KEY_NAME = 4;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private PebbleKit.PebbleDataReceiver _dataReceiver;
	private PebbleKit.PebbleAckReceiver _ackReceiver;
	private PebbleKit.PebbleNackReceiver _nackReceiver;
	private BroadcastReceiver _connectedReceiver;

	private TokenInfo[] _tokens;
	private TokenRepository _tokenRepository;

	private int _transactionId = 0;
	
	public PebbleLinkService() {
		_tokenRepository = new TokenRepository();
	}

	private int getNextTransactionId() {
		return _transactionId++;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Context context = this.getBaseContext();
		
		String remotePath = REMOTE_DATABASE_PATH;
		String localPath = context.getFilesDir().getPath() + "/" + LOCAL_DATABASE_PATH;
		
		try {
			copyDatabase(remotePath, localPath);
		} catch (Exception e) {
			Log.e(TAG, "Error copying database", e);
		}

		try {
			_tokens = _tokenRepository.getTokensFromDatabase(context, localPath);
		} catch (Exception e) {
			Log.e(TAG, "Error caching database", e);
		}
		
		try {
			deleteDatabase(localPath);
		} catch (Exception e) {
			Log.e(TAG, "Error deleting copy of database", e);
		}
		
		initializePebbleDataReceiver();		
		
		return Service.START_STICKY;
	}
	
	private void copyDatabase(String remoteFilename, String localFilename)
			throws IOException, InterruptedException {
		String[] commandLine = { 
			"su", 
			"-c",
			"cp " + remoteFilename + " " + localFilename + "; chmod 666 " + localFilename
		}; 
		
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(commandLine);
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			throw new IOException("Process returned with error code: " + returnCode);
		}
	}

	private void initializePebbleDataReceiver() {
		_dataReceiver = new PebbleKit.PebbleDataReceiver(APP_ID) {
            @Override
            public void receiveData(final Context context, final int transactionId,
									final PebbleDictionary data) {
            	Log.i(TAG, "Received data");
            	PebbleKit.sendAckToPebble(context, transactionId);
            	
            	if (!data.iterator().hasNext()) {
            		Log.w(TAG, "No data received");
            		return;
            	}
            	
            	final Long fetchValue = data.getUnsignedIntegerAsLong(OATH_KEY_FETCH);
            	if (fetchValue != null) {
            		Log.i(TAG, "Handle fetch command");
            		handleGetNamesCommand(data);
            	} else {
            		Log.w(TAG, "No command to process");
            	}
            }
        };
        
        PebbleKit.registerReceivedDataHandler(this, _dataReceiver);

		_ackReceiver = new PebbleKit.PebbleAckReceiver(APP_ID) {

			@Override
			public void receiveAck(Context context, int i) {
				Log.i(TAG, String.format("Received ACK %d", i));
			}
		};
		PebbleKit.registerReceivedAckHandler(this, _ackReceiver);

		_connectedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "Pebble connected!");
			}
		};
		PebbleKit.registerPebbleConnectedReceiver(this, _connectedReceiver);

		_nackReceiver = new PebbleKit.PebbleNackReceiver(APP_ID) {
			@Override
			public void receiveNack(Context context, int i) {
				Log.i(TAG, String.format("Nack: %d", i));
			}
		};
		PebbleKit.registerReceivedNackHandler(this, _nackReceiver);
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(_dataReceiver);			_dataReceiver = null;
		unregisterReceiver(_nackReceiver);			_nackReceiver = null;
		unregisterReceiver(_ackReceiver);			_ackReceiver = null;
		unregisterReceiver(_connectedReceiver);		_connectedReceiver = null;

		super.onDestroy();
	}

	protected void handleGetNamesCommand(PebbleDictionary request) {
		Context context = getApplicationContext();
		OathCalculator calculator = new OathCalculator(context);

		for (TokenInfo token : _tokens) {
			Log.i(TAG, String.format("Sending %s", token.getEmail()));
			PebbleDictionary response = new PebbleDictionary();
			response.addUint8(OATH_KEY_APPEND, token.getId());
			response.addString(OATH_KEY_NAME, token.getEmail());

			OathCalculator.CodeInfo code = calculator.calculate(token);
			response.addString(OATH_KEY_CODE, code.getCode());
			response.addString(OATH_KEY_EXPIRATION, code.getExpiration().toString());

			PebbleKit.sendDataToPebbleWithTransactionId(
					context, APP_ID, response, getNextTransactionId());

			Log.i(TAG, String.format("Sent %s", token.getEmail()));

			SystemClock.sleep(250);
		}
	}
}