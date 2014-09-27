package com.tlvcache;


/**
 * Base interface for all types of cache instances
 */
public interface CacheInstance {

	/**
	 * Puts value to cache (associates the given value 
	 * the given with key in this cache)
	 */
	void put(String key, Object value);
	
	/**
	 * Returns the value associated with key in this cache,
	 * or null if there is no cached value for key
	 */
	Object get(String key);
	
	/**
	 * Discards any cached value for given key
	 */
	void remove(String key);
	
	/**
	 * Returns a String representing current cache state description
	 */
	String getStateDescription();
}
