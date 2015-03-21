package net.djeebus.pebble.oath;

class TokenInfo {
	private byte _id;
	private String _email;
	private String _secret;
	private int _type;
	private int _provider;
	private int _counter;
	
	public TokenInfo(byte id, String email, String secret, int type, int provider, int counter) {
		_id = id;
		_email = email;
		_secret = secret;
		_type = type;
		_provider = provider;
		_counter = counter;
	}
	
	public String getEmail() {
		return _email;
	}
	
	public byte getId() {
		return _id;
	}
	
	public int getType() {
		return _type;
	}
	
	public int getProvider() {
		return _provider;
	}
	
	public String getSecret() {
		return _secret;
	}
	
	public int getCounter() {
		return _counter;
	}
}