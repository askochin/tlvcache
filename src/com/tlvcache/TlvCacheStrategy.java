package com.tlvcache;

/**
 * Memory cache eviction strategy
 */
public enum TlvCacheStrategy {
	
	/**
	 * First In First Out
	 */
	FIFO,
	
	/**
	 * Least Recently Used
	 */
	LRU,
	
	/**
	 * Least Frequently Used
	 */
	LFU
}
