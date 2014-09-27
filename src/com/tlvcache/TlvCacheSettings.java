package com.tlvcache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A class representing configuration paramaters
 * of the cache.
 */
public class TlvCacheSettings {

	/**
	 * Eviction strategy
	 */
	private final TlvCacheStrategy strategy;
	
	/**
	 * Max number of objects in memory cache
	 */
	private final long memoryCacheMaxSize;
	
	/**
	 * Max size of file system cache (bytes)
	 */
	private final long fsCacheMaxSize;
	
	/**
	 * Number of data files for file system cache
	 */
	private final long fsCacheFilesCount;
	
	/**
	 * Directory of file system cache storage
	 * (where data files are placed)
	 */
	private final String fsCacheDirPath;
	
	/**
	 * Properties object from which this settings have 
	 * been initiated.
	 */
	private final Map<String, String> initProperties;
	
	
	private TlvCacheSettings(Properties props) throws TlvCacheException {
		strategy           = readStrategy(props, "strategy");
		memoryCacheMaxSize = readLong(props, "memoryCacheMaxSize", 5, 1_000_000);
		fsCacheMaxSize     = readLong(props, "fsCacheMaxSize", 100, 1_000_000);
		fsCacheFilesCount  = readLong(props, "fsCacheFilesCount", 2, 1_000);
		fsCacheDirPath     = readString(props, "fsCacheDirPath");
		
		if (fsCacheMaxSize/fsCacheFilesCount < 100) {
			throw new TlvCacheException("Too much files for filesystem specified: Max size of file should be more 100b.");
		}
		
		initProperties = new HashMap<>();
		for (Object key : props.keySet()) {
			initProperties.put(key.toString(), props.getProperty(key.toString()).toString());
		}
	}
	
	
	//-------------- PUBLIC METHODS -----------------------------
	
	
	/**
	 * Initiates from specified properties and creates new settings instance
	 * @throws TlvCacheException
	 */
	public static TlvCacheSettings create(Properties props) throws TlvCacheException {
		return new TlvCacheSettings(props);
	}
	
	
	/**
	 * Returns number of data files for file system cache
	 */
	public long getFsCacheFilesCount() {
		return fsCacheFilesCount;
	}

	
	/**
	 * Returns directory of file system cache storage
	 * (where data files are placed)
	 */
	public String getFsCacheDirPath() {
		return fsCacheDirPath;
	}
	
	
	/**
	 * Returns eviction strategy
	 */
	public TlvCacheStrategy getStrategy() {
		return strategy;
	}
	
	
	/**
	 * Returns max size of file system cache (bytes)
	 */
	public long getFsCacheMaxSize() {
		return fsCacheMaxSize;
	}
	
	
	/**
	 * Returns max number of objects in memory cache
	 */
	public long getMemoryCacheMaxSize() {
		return memoryCacheMaxSize;
	}
	
	
	/**
	 * Returns properties object from which this settings have 
	 * been initiated.
	 */
	public Map<String, String> getInitialSettings() {
		return new HashMap<>(initProperties);
	}
	
	
	
	//--------------------- PRIVATE METHODS ---------------------------------
	
	
	/**
	 * Reads and return long value of given param from specified properties
	 * @throws TlvCacheException if value is not found or incorrect
	 */
	private static long readLong(Properties props, String paramName, long min, long max) throws TlvCacheException {
		String strValue = readString(props, paramName);
		long result;
		try {
			result = Long.parseLong(strValue);
		} catch (NumberFormatException e) {
			throw new TlvCacheException(wrongParamMsg(props, paramName, "Number from " + min + " to " + max + " required"));
		}
		if (result < min || result > max) {
			throw new TlvCacheException(wrongParamMsg(props, paramName, "Number from " + min + " to " + max + " required"));
		}
		return result;
	}
	
	
	/**
	 * Reads and return eviction strategy value of given param from specified properties
	 * @throws TlvCacheException if value is not found or incorrect
	 */
	private static TlvCacheStrategy readStrategy(Properties props, String paramName) throws TlvCacheException {
		try {
			return TlvCacheStrategy.valueOf(readString(props, "strategy"));
		} catch (IllegalArgumentException ex) {
			throw new TlvCacheException(wrongParamMsg(props, paramName, 
					"One of " + Arrays.toString(TlvCacheStrategy.values())  + " required"));
		}
		
	}
	
	
	/**
	 * Reads and return string value of given param from specified properties
	 * @throws TlvCacheException if value doesn't exist or is empry
	 */
	private static String readString(Properties props, String paramName) throws TlvCacheException {
		String result = props.getProperty(paramName);
		if (result == null) {
			throw new TlvCacheException("Parameter missed: " + paramName);
		}
		if (result.trim().equals("")) {
			throw new TlvCacheException("Parameter empty: " + paramName);
		}
		return result;
	}
	
	
	/**
	 * Returns error message about problem with specified parameter
	*/
	private static String wrongParamMsg(Properties props, String paramName, String msg) {
		return String.format("Wrong parameter value: %s = %s. %s.", paramName, props.get(paramName), msg);
	}
}
