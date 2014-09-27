package com.tlvcache.fs;


/**
 * Exception when key-value entry is too big to save it to one file
 *
 */
class TooBigEntryException extends Exception {

	private static final long serialVersionUID = 1L;

	TooBigEntryException(String message) {
		super(message);
	}
	
}
