package com.tlvcache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.tlvcache.fs.TlvFilesystemCache;


/**
 * Two-level cache service.
 * 
 * Level 1 is memory, level 2 is file system.
 * Memory level supports 3 types of eviction strategies:
 * - LRU (Least Recently Used), 
 * - LFU (Least Frequently Used),
 * - FIFO (First In First Out).
 * Strategy and other cache config params can be passed cache through 
 * the settings attribute of the cache create() factory method.
 * 
 * Key is arbitrary string. Keys are not hashed and are saved as is.
 * Values are saved as soft references to prevent OutOfMemory exception
 * if stored values are too large. To be stored in file system cache
 * values should implement the Serializable interface.
 * 
 * Cache has various states during its lifetime. 
 * CREATED - just after the creation and just before calling the start() method.
 * STARTING - after the cache start is initiated (calling the start() method).
 * WORKING - after the start() method completed.
 * STOPPING - after the cache stop is initiated (calling the stop() method).
 * STOPPED - after the stop() method completed.
 * 
 * So during lifetime cache instance can pass the chain of states:
 * CREATED -> STARTING -> WORKING -> STOPPING -> STOPPED.
 * Any other state transions are not permitted.
 */
public class TlvCacheInstance implements CacheInstance {
	
	private final Logger logger = Logger.getLogger(TlvCacheInstance.class.getName());
	
	/**
	 * Configuration settings
	 */
	private final TlvCacheSettings settings;
	
	/**
	 * Memory cache instance
	 */
	private final TlvMemoryCache memoryCache;
	
	/**
	 * File system cache instance
	 */
	private final TlvFilesystemCache fsCache;
	
	/**
	 * State of service
	 */
	private AtomicReference<TlvCacheState> state = new AtomicReference<>(TlvCacheState.CREATED);
	
	private TlvCacheInstance(TlvMemoryCache memoryCache, TlvFilesystemCache fsCache, TlvCacheSettings settings) {
		this.settings = settings;
		this.memoryCache = memoryCache;
		this.fsCache = fsCache;
	}
	

	/**
	 * Puts value to cache (associates the given value 
	 * the given with key in this cache)
	 */
	@Override
	public void put(String key, Object value) {
		if (state.get() != TlvCacheState.WORKING) return;
		memoryCache.put(key, value);
		fsCache.remove(key); // to avoid stale data presence
	}

	
	/**
	 * Returns the value associated with key in this cache,
	 * or null if there is no cached value for key
	 */
	@Override
	public Object get(String key) {
		if (state.get() != TlvCacheState.WORKING) return null;
		Object result = memoryCache.get(key);
		if (result == null) return fsCache.get(key);
		else return result;
	}

	
	/**
	 * Discards any cached value for given key
	 */
	@Override
	public void remove(String key) {
		if (state.get() != TlvCacheState.WORKING) return;
		memoryCache.remove(key);
		fsCache.remove(key);
	}
	
	
	/**
	 * Returns state of cache instance
	 */
	public TlvCacheState getState() {
		return state.get();
	}
	
	
	/**
	 * Returns snapshot of current memory cache content
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getMemoryCacheContentSnapshot() {
		if (state.get() == TlvCacheState.WORKING) {
			return memoryCache.getContentSnapshot();
		} else {
			return Collections.EMPTY_MAP;
		}
	}
	
	
	/**
	 * Returns snapshot of current file system cache content
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getFsCacheContentSnapshot() {
		if (state.get() == TlvCacheState.WORKING) {
			return fsCache.getContentSnapshot();
		} else {
			return Collections.EMPTY_MAP;
		}
	}
	
	
	/**
	 * Returns a String representing current cache state description
	 */
	public String getStateDescription() {
		return "state = " + state.get() + ", memory [" + memoryCache.getStateDescription() + 
			"], filesystem [" + fsCache.getStateDescription() + "]";
	}
	
	
	/**
	 * Returns cache configuration specified while creating
	 */
	public TlvCacheSettings getSettings() {
		return settings;
	}

	
	/**
	 * Starts cache service.
	 * 
	 * While starting cache loads data saved in file system storage
	 * so it can take some period of time until start is not completed.
	 * At this time cache is in the STARTING state.
	 * 
	 * @throws TlvCacheException
	 */
	public void start() throws TlvCacheException {
		state.set(TlvCacheState.STARTING);
		try {
			fsCache.start();
		} catch (Exception e) {
			state.set(TlvCacheState.STOPPED);
			throw new TlvCacheException("Error while starting filesystem cache", e);
		}
		state.set(TlvCacheState.WORKING);
	}
	
	
	/**
	 * Initiates ordinal cache service shutdown.
	 * 
	 * Just after calling this method cache is set to the STOPPING state.
	 * When this method is completed cache is set to the STOPPED state
	 * and specified callback object <code>onStopped</code> is called.
	 * 
	 * @param onStopped Callback to perform after stop completed
	 */
	public void stop(final Runnable onStopped) {
		if (state.get() == TlvCacheState.STOPPED) return;
		if (!state.compareAndSet(TlvCacheState.WORKING, TlvCacheState.STOPPING)) {
			throw new IllegalStateException("Illegal to stop not started cache");
		}
		fsCache.stop(new Runnable() {
			@Override
			public void run() {
				state.set(TlvCacheState.STOPPED);
				onStopped.run();
			}
		}, memoryCache.getContent()); // pass memory content to file storage
	}
	
	
	/**
	 * Initiates abnormal urgent cache shutdown.
	 * 
	 * Blocks until storing execution is terminated, or the timeout occurs, 
	 * or the current thread is interrupted, whichever happens first.
	 * 
	 * Does not perform any data persistent operations,
	 * just shutdowns internal cache services.
	 * 
	 * @return True if this cache service terminated and false if the timeout elapsed before termination
	 */
	public boolean shutdown(long timeout, TimeUnit timeUnit)  throws InterruptedException {
		if (state.get() == TlvCacheState.STOPPED) return true;
		if (state.get() != TlvCacheState.STOPPING && !state.compareAndSet(TlvCacheState.WORKING, TlvCacheState.STOPPING)) {
			throw new IllegalStateException("Illegal to shutdown not started cache");
		}
		state.set(TlvCacheState.STOPPING);
		if (fsCache.shutdown(timeout, timeUnit)) {
			state.set(TlvCacheState.STOPPED);
			return true;
		} 
		return false;
	}
	
	
	/**
	 * Create new instance of cache
	 * @param settings Set of cache configuration params
	 * @return New cache instance in the CREATED state
	 * @throws TlvCacheException
	 */
	public static TlvCacheInstance create(TlvCacheSettings settings) throws TlvCacheException {
		InstanceEvictionHandler evictionHandler = new InstanceEvictionHandler();
		TlvMemoryCache memoryCache = createMemoryCache(settings, evictionHandler);
		TlvFilesystemCache fsCache = TlvFilesystemCache.create(settings);
		TlvCacheInstance result = new TlvCacheInstance(memoryCache, fsCache, settings);
		evictionHandler.cacheRef.set(result);
		return result;
	}
	
	
	/**
	 * Creates and returns instance of memory cache object 
	 * according specified eviction strategy
	 */
	private static TlvMemoryCache createMemoryCache(TlvCacheSettings settings, TlvMemoryCache.EvictionHandler evictionHandler) {
		switch (settings.getStrategy()) {
			case FIFO : return new TlvFifoMemoryCache(settings, evictionHandler);
			case LFU  : return new TlvLfuMemoryCache(settings, evictionHandler);
			case LRU  : return TlvLruMemoryCache.create(settings, evictionHandler);
			default   : throw new IllegalArgumentException("Illegal strategy value: " + settings.getStrategy());
		}
	}
	
	
	/**
	 * Handler to process data eviction happened in memory cache.
	 * All evicted data is transmitted to file system cache to persist.
	 */
	private static class InstanceEvictionHandler implements TlvMemoryCache.EvictionHandler {
		
		/**
		 * Atomic reference to prevent unsafe cache instance publication
		 * while construction.
		 */
		private final AtomicReference<TlvCacheInstance> cacheRef = new AtomicReference<>(null);

		@Override
		public void onEvicted(String key, Object value) {
			cacheRef.get().logger.fine("Entry evicted: " + key);
			cacheRef.get().fsCache.put(key, value);  // transfer data to file system cache
		}
	}
}
