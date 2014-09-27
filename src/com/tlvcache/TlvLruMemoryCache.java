package com.tlvcache;

import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Memory cache implementing the LRU (Least Recently Used) 
 * eviction strategy.
 * 
 * Uses Guava caching library to provide cache all functionality.
 */
public class TlvLruMemoryCache implements TlvMemoryCache {
	
	/**
	 * Cache object for storing all key-value associations
	 */
	private final Cache<String, Object> cache;
	
	private TlvLruMemoryCache(Cache<String, Object> cache) {
		this.cache = cache;
	}
	
	
	/**
	 * Puts value to cache (associates the given value 
	 * the given with key in this cache)
	 */
	@Override
	public void put(String key, Object value) {
		cache.put(key, value);
	}

	
	/**
	 * Returns the value associated with key in this cache,
	 * or null if there is no cached value for key
	 */
	@Override
	public Object get(String key) {
		return cache.getIfPresent(key);
	}

	
	/**
	 * Discards any cached value for given key
	 */
	@Override
	public void remove(String key) {
		cache.invalidate(key);
	}

	
	/**
	 * Returns map containing all key-entry pairs in cache
	 * which values are garbage-collected
	 */
	@Override
	public Map<String, Object> getContent() {
		return cache.asMap();
	}


	/**
	 * Returns snapshot of current cache content
	 */
	@Override
	public Map<String, Object> getContentSnapshot() {
		return cache.asMap();
	}
	
	
	/**
	 * Returns a String representing current cache state description
	 */
	@Override
	public String getStateDescription() {
		return "size = " + cache.size();
	}

	
	/**
	 * Created and returns instance of LRU cache with specified configuration settings 
	 * and eviction handler
	 */
	public static TlvLruMemoryCache create(TlvCacheSettings settings, final EvictionHandler evictionHandler) {
		Cache<String, Object> cache = CacheBuilder.newBuilder()
			.maximumSize(settings.getMemoryCacheMaxSize())
			.softValues()  // keep values as soft references
			.removalListener(new RemovalListener<String, Object>() {

				@Override
				public void onRemoval(RemovalNotification<String, Object> notification) {
					if (notification.getCause() == RemovalCause.SIZE) {
						evictionHandler.onEvicted(notification.getKey(), notification.getValue());
					}
				}
				
			}).build();
		return new TlvLruMemoryCache(cache);
	}
	
}
