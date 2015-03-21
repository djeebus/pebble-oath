package net.djeebus.pebble.oath;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TokenRepository {
	public final String TAG = "TokenRepository";

	public TokenInfo[] getTokensFromDatabase(Context context, String dbPath) {
		SQLiteDatabase database = openDatabase(context, dbPath);
		if (database == null) {
			Log.w(TAG, "openOrCreateDatabase returned null");
		}
		
		try {
			Cursor cursor = getCursor(database);
			if (cursor == null) {
				Log.i(TAG, "Accounts table had no rows");
				return null;
			}
			
			try {
				List<TokenInfo> tokens = new ArrayList<TokenInfo>();
				
				if (!cursor.moveToFirst()) {
					Log.w(TAG, "Error moving to first record");
					return null;
				}
				
				int id_index = cursor.getColumnIndex("_id");
				int email_index = cursor.getColumnIndex("email");
				int secret_index = cursor.getColumnIndex("secret");
				int type_index = cursor.getColumnIndex("type");
				int provider_index = cursor.getColumnIndex("provider");
				int counter_index = cursor.getColumnIndex("counter");
				
				do {
					byte id = (byte)cursor.getShort(id_index);
					String email = cursor.getString(email_index);
					String secret = cursor.getString(secret_index);
					int type = cursor.getInt(type_index);
					int provider = cursor.getInt(provider_index);
					int counter = cursor.getInt(counter_index);
					
					TokenInfo token = new TokenInfo(id, email, secret, type, provider, counter);
					tokens.add(token);
				} while (cursor.moveToNext());
				
				Log.i(TAG, "Found " + tokens.size() + " accounts");
				
				return tokens.toArray(new TokenInfo[] { });
			} finally {
				if (!cursor.isClosed()) {
					cursor.close();
				}
			}

		} finally {
			if (database.isOpen()) {
				database.close();
			}
		}
	}
	
	private SQLiteDatabase openDatabase(Context context, String dbPath) {
		return context.openOrCreateDatabase(dbPath, Context.MODE_PRIVATE, null);
	}
	
	private Cursor getCursor(SQLiteDatabase database) {
		return database.query(
			"accounts", 
			null,
			null, 
			null, null, null, null);
	}
}
