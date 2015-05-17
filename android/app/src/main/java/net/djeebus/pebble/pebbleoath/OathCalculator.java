package net.djeebus.pebble.pebbleoath;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.util.Log;

import com.google.android.apps.authenticator.Base32String;
import com.google.android.apps.authenticator.PasscodeGenerator;
import com.google.android.apps.authenticator.TotpCounter;
import com.google.android.apps.authenticator.TotpClock;
import com.google.android.apps.authenticator.Utilities;
import com.google.android.apps.authenticator.Base32String.DecodingException;

public class OathCalculator {
	static final String TAG = "OathCalculator";
	private static final int PIN_LENGTH = 6; // HOTP or TOTP
	private static final int REFLECTIVE_PIN_LENGTH = 9; // ROTP
	
	class CodeInfo {
		String _code;
		Date _expiration;

		public CodeInfo(String code, Date expiration) {
			_code = code;
			_expiration = expiration;
		}

		public String getCode() {
			return _code;
		}

		public Date getExpiration() {
			return _expiration;
		}
	}
	
	TotpCounter mTotpCounter = new TotpCounter(30);
	TotpClock mTotpClock = null;
	
	public OathCalculator(Context context) {
		mTotpClock = new TotpClock(context);
	}
	
	public CodeInfo calculate(TokenInfo tokenInfo) {
		String checkCode;
		try {
		    long otp_state;

		    if (tokenInfo.getType() == 0) {
		    	long seconds = Utilities.millisToSeconds(mTotpClock.currentTimeMillis());
		    	
		    	// For time-based OTP, the state is derived from clock.
		    	otp_state = mTotpCounter.getValueAtTime(seconds);
		    } else {
		    	throw new Exception("oh noes");
		    }

		    checkCode = computePin(tokenInfo.getSecret(), otp_state, null);
		} catch (Exception e) {
			checkCode = "";
		}
		return new CodeInfo(checkCode, new Date());
	}
	
	private String computePin(String secret, long otp_state, byte[] challenge)
			throws Exception {
		if (secret == null || secret.length() == 0) {
			throw new Exception("Null or empty secret");
		}

		try {
			PasscodeGenerator.Signer signer = getSigningOracle(secret);
			PasscodeGenerator pcg = new PasscodeGenerator(
					signer,
					(challenge == null) ? PIN_LENGTH : REFLECTIVE_PIN_LENGTH);

			return (challenge == null) ?
						pcg.generateResponseCode(otp_state) :
						pcg.generateResponseCode(otp_state, challenge);
		} catch (GeneralSecurityException e) {
			throw new Exception("Crypto failure", e);
		}
	}
	
	static PasscodeGenerator.Signer getSigningOracle(String secret) {
		try {
			byte[] keyBytes = decodeKey(secret);
			final Mac mac = Mac.getInstance("HMACSHA1");
			mac.init(new SecretKeySpec(keyBytes, ""));

		    // Create a signer object out of the standard Java MAC implementation.
		    return new PasscodeGenerator.Signer() {
		    	@Override
		    	public byte[] sign(byte[] data) {
		    		return mac.doFinal(data);
		    	}
		    };
		} catch (DecodingException | NoSuchAlgorithmException | InvalidKeyException error) {
			Log.e(TAG, error.getMessage());
    	}

		return null;
	}

	private static byte[] decodeKey(String secret) throws DecodingException {
		return Base32String.decode(secret);
	}
}
