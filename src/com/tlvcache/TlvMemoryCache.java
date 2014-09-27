package com.tlvcache;

import java.util.Map;

/**
 * Memory cache instance
 */
interface TlvMemoryCache extends CacheInstance {

	/**
	 * Returns snapshot of current cache content
	 */
	Map<String, Object> getContentSnapshot();
	
	
	/**
	 * Returns map containing all key-entry pairs in cache
	 * which values are garbage-collected
	 */
	Map<String, Object> getContent();
	
	
	/**
	 * Handler of evicting key-value entries
	 */
	interface EvictionHandler {
		void onEvicted(String key, Object value);
	}
	
}
