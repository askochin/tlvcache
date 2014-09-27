package com.tlvcache;

/**
 * Common TlvCache framework exception
 */
public class TlvCacheException extends Exception {

	private static final long serialVersionUID = 1L;

	public TlvCacheException() {
		super();
	}

	public TlvCacheException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public TlvCacheException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TlvCacheException(String arg0) {
		super(arg0);
	}

	public TlvCacheException(Throwable arg0) {
		super(arg0);
	}

	
	
}
