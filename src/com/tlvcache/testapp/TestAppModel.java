package com.tlvcache.testapp;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tlvcache.TlvCacheException;
import com.tlvcache.TlvCacheInstance;
import com.tlvcache.TlvCacheSettings;
import com.tlvcache.TlvCacheState;


/**
 * Application data model (as part of MVC pattern)
 */
public class TestAppModel {
	
	static final Logger logger = Logger.getLogger(TestAppModel.class.getName());
	
	/**
	 * Current cache instance
	 */
	private TlvCacheInstance cacheInstance;
	
	/**
	 * Configurations table data model
	 */
	private SettingsTableModel settingsTableModel;
	
	
	TestAppModel() {
		Properties properties = new Properties();
		
//		try (FileReader f = new FileReader("D:\\\\Data\\config.properties")) {
//			properties.load(f);
//		} catch (IOException e) {
//			logger.log(Level.WARNING, "Failed to load default cache configuration", e);
//		}
		
		try {
			properties.load(this.getClass().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load default cache configuration", e);
		}
		
		settingsTableModel = new SettingsTableModel(properties);
	}
	
	
	/**
	 * Returns configurations table data model
	 */
	SettingsTableModel getSettingsTableModel() {
		return settingsTableModel;
	}
	
	
	/**
	 * Initializes configurations table data using given config params
	*/
	void initCacheSettings(Properties props) {
		settingsTableModel.setProperties(props);
	}

	
	/**
	 * Creates and initializes new cache instance using current configuration params
	 * @throws TlvCacheException
	 */
	void initNewCacheInstance() throws TlvCacheException {
		TlvCacheSettings settings = TlvCacheSettings.create(settingsTableModel.getSettingsAsProperties());
		this.cacheInstance = TlvCacheInstance.create(settings);
	}
	
	
	/**
	 * Returns current configuration instance
	 */
	TlvCacheInstance getCacheInstance() {
		return this.cacheInstance;
	}
	
	
	/**
	 * Returns current cache instance state description
	 */
	String getCacheStateDesciption() {
		if (cacheInstance == null) {
			return "Cache not initialized";
		}
		return cacheInstance.getStateDescription();
	}
	
	
	/**
	 * Initiates cache shutdown
	 */
	void shutdownCache() {
		if (cacheInstance != null && (
			cacheInstance.getState() == TlvCacheState.WORKING ||
			cacheInstance.getState() == TlvCacheState.STOPPING)) 
		{
        	try {
        		cacheInstance.shutdown(1, TimeUnit.MINUTES);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
        }
	}
}
