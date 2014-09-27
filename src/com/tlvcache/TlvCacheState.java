package com.tlvcache;

/**
 * Cache state
 */
public enum TlvCacheState {
	
	/**
	 * State just after the creation and just before calling the start() method
	 */
	CREATED,
	
	/**
	 * State after the cache start is initiated (calling the start() method)
	 */
	STARTING,
	
	/**
	 * State after the start() method completed.
	 * This is the base state of cache when it can perform 
	 * all caching operations.
	 */
	WORKING,
	
	/**
	 * State after the cache stop is initiated (calling the stop() method)
	 */
	STOPPING,
	
	/**
	 * State after the stop() method completed
	 */
	STOPPED;
}
