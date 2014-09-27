package com.tlvcache;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Memory cache implementing the FIFO eviction strategy
 */
public class TlvFifoMemoryCache implements TlvMemoryCache {
	
	/**
	 * Max number of values
	 */
	private final long maxSize;
	
	/**
	 * Handler to process evicted entries
	 */
	private final EvictionHandler evictionHandler;

	/**
	 * Synchronized map that keeps key-value associations.
	 * 
	 * It's based on the <code>LinkedHashMap</code> representation
	 * that allows to keep queue of entries and remove them in the order
	 * they had come (FIFO eviction strategy).
	 * 
	 * Values are saved as soft references to prevent OutOfMemory exception
	 * if stored values are too large.
	 */
	private final Map<String, SoftReference<Object>> map = Collections.synchronizedMap(
		new LinkedHashMap<String, SoftReference<Object>>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Entry<String, SoftReference<Object>> entry) {
				boolean evicted = size() > maxSize;
				if (evicted && (entry.getValue().get() != null)) {
					evictionHandler.onEvicted(entry.getKey(), entry.getValue().get());
				}
				return evicted;
			}
		}
	);
	
	
	public TlvFifoMemoryCache(TlvCacheSettings settings, EvictionHandler evictionHandler) {
		maxSize = settings.getMemoryCacheMaxSize();
		this.evictionHandler = evictionHandler;
	}
	
	
	@Override
	public void put(String key, Object value) {
		map.put(key, new SoftReference<Object>(value));
	}

	
	@Override
	public Object get(String key) {
		SoftReference<Object> ref = map.get(key);
		return ref == null ? null : ref.get();
	}

	
	@Override
	public void remove(String key) {
		map.remove(key);
	}
	
	
	@Override
	public Map<String, Object> getContent() {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, SoftReference<Object>> entry : map.entrySet()) {
			if (entry.getValue().get() != null) {
				result.put(entry.getKey(), entry.getValue().get());
			};
		}
		return result;
	}


	@Override
	public Map<String, Object> getContentSnapshot() {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, SoftReference<Object>> entry : map.entrySet()) {
			Object value = entry.getValue().get();
			result.put(entry.getKey(), value == null ? "null" : value.toString());
		}
		return result;
	}


	@Override
	public String getStateDescription() {
		return "size = " + map.size();
	}
	
	
}
