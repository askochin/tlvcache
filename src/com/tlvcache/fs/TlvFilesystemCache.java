package com.tlvcache.fs;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.tlvcache.CacheInstance;
import com.tlvcache.TlvCacheException;
import com.tlvcache.TlvCacheSettings;

/**
 * File-based cache storage.
 * 
 * Storage contains a set of data files each of the same structure.
 * Files are named with special format so every file has a unique
 * number inside the storage. Each files consists of a set of records.
 * Each record represents key-value entry that is the cache data.
 * 
 * There are 2 types of records (entries): adding and removing. 
 * When data is being loaded from storage: 
 * - on each adding entry its position is put to memory index,
 * - on each removing entry key is removed from memory index.
 * Removing entry differs from adding entry by associated value absence.
 */
public class TlvFilesystemCache implements CacheInstance {
	
	/**
	 * Max capacity of storing executor queue
	 */
	static final int TASKS_QUEUE_CAPACITY = 100;
	
	/**
	 * File names format pattern
	 */
	private static Pattern DATA_FILES_PATTERN = Pattern.compile("tlv\\d{8}\\.fsc");
	
	/**
	 * Logger
	 */
	private final Logger logger = Logger.getLogger(TlvFilesystemCache.class.getName());
	
	/**
	 * Cache settings
	 */
	private final TlvCacheSettings settings;
	
	/**
	 * Max data file size (storage size / files count)
	 */
	private final long fsCacheFileMaxSize;
	
	/**
	 * Helper object to perform input/output operations on storage.
	 */
	private final FsCacheIoHandler ioHandler;
	
	/**
	 * Set of data files
	 */
	private final SortedMap<Integer, FsCacheFile> files = new ConcurrentSkipListMap<>();
	
	/**
	 * Entries positions index
	 */
	private final Map<String, FsCachePosition> positions = new ConcurrentHashMap<>();
	
	/**
	 * Storing entries executor
	 */
	private volatile ThreadPoolExecutor persistanceExecutor;
	
	
	
	
	private TlvFilesystemCache(TlvCacheSettings settings) {
		this.settings = settings;
		fsCacheFileMaxSize = settings.getFsCacheMaxSize() / settings.getFsCacheFilesCount();
		ioHandler = new FsCacheIoHandler();
	}
	
	
	/**
	 * Creates new file system cache instance 
	 */
	public static TlvFilesystemCache create(TlvCacheSettings settings) throws TlvCacheException {
		Path path = Paths.get(settings.getFsCacheDirPath());
		if (!Files.exists(path)) {
			throw new TlvCacheException("Filesystem cache directory does not exist: " + settings.getFsCacheDirPath());
		}
		if (!Files.isDirectory(path)) {
			throw new TlvCacheException("Filesystem cache directory path is not a directory: " + settings.getFsCacheDirPath());
		}
		return new TlvFilesystemCache(settings);
	}
	
	
	/**
	 * Retrieves object from storage by key.
	 */
	@Override
	public Object get(String key) {		
		try {
			return tryGet(key);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to get: key = " + key, e);
			return null;
		}
	}
	
	
	/**
	 * Puts object to storage. To be stored object must implement
	 * <code>Serializable</code> interface.
	 * Method doesn't do storing directly. Instead special task
	 * is created and submitted to the storing executor.
	 */
	@Override
	public void put(final String key, final Object value) {
		if (value instanceof Serializable) {
			logger.fine("Put requested: " + key);
			persistanceExecutor.execute(new Runnable() {
				public void run() {
					try {
						FsCachePosition pos = persistEntry(key, value);
						positions.put(key, pos);
						logger.fine("Put done: " + key);
					} catch (TooBigEntryException | IOException e) {
						logger.log(Level.WARNING, "Failed to put: key = " + key, e);
					}
				}
			});
		}
	}
	

	/**
	 * Removes object from storage. 
	 * Removal operation is synchronous because it's important
	 * that stale data should not be available.
	 * @param key
	 */
	@Override
	public void remove(String key) {
		if (positions.remove(key) != null) {
			try {
				
				// Practically nothing is removed.
				// Just special entry (without value) persists to storage.
				
				persistEntry(key, null);
				logger.fine("Key removed: " + key);
			} catch (TooBigEntryException | IOException e) {
				logger.log(Level.WARNING, "Failed to remove: key = " + key, e);
			}
		}
	}

	
	/**
	 * Starts filesystem cache.
	 * Index of entries positions is created and storing executor starts.
	 * @throws TlvCacheException
	 */
	public void start() throws TlvCacheException {
		loadData();
		persistanceExecutor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.DAYS, 
			new ArrayBlockingQueue<Runnable>(TASKS_QUEUE_CAPACITY));
		logger.info("Filesystem cache started");
	}
	
	
	/**
	 * Initiates an orderly cache shutdown.
	 * Persists specified entries from memory cache to file storage.
	 * @param onStopped Action to perform after cache stopped.
	 * @param entriesToPersist
	 */
	public void stop(final Runnable onStopped, final Map<String, Object> entriesToPersist) {
		logger.info("Filesystem cache stop initiated");
		
		// change the policy so that we can submit task even if executor queue is full
		persistanceExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
		persistanceExecutor.execute(new Runnable() {
			public void run() {
				try {
					persistEntries(entriesToPersist);
				} catch (IOException e) {
					logger.log(Level.WARNING, "Failed to persist entries while stopping", e);
				} finally {
					for (FsCacheFile file : files.values()) {
						closeFileChannel(file); // close channels for data files
					}
					logger.info("Filesystem cache stop done");
					onStopped.run();
				}
			}
		});
		persistanceExecutor.shutdown();
	}


	/**
	 * Attempts to stop cache service immediately without 
	 * any persistent operations (abnormal shutdown).
	 * 
	 * Blocks until storing execution is terminated, or the timeout occurs, 
	 * or the current thread is interrupted, whichever happens first.
	 * 
	 * @return True if this cache service terminated and false if the timeout elapsed before termination
	 * @throws InterruptedException
	 */
	public boolean shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {
		logger.info("Filesystem cache shutdown initiated");
		persistanceExecutor.shutdownNow();
		for (FsCacheFile file : files.values()) {
			closeFileChannel(file); // close file channels
		}
		return persistanceExecutor.awaitTermination(timeout, timeUnit);
	}


	/**
	 * Returns snapshot of current cache content
	 */
	public Map<String, Object> getContentSnapshot() {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, FsCachePosition> entry : positions.entrySet()) {
			FsCachePosition pos = entry.getValue();
			result.put(entry.getKey(), pos.getFile().getPath().getFileName().toString() + " - [" + pos.getOffset() + ", " + pos.getSize() + "]");
		}
		return result;
	}
	
	
	/**
	 * Returns description of current state
	 */
	@Override
	public String getStateDescription() {
		return "size = " + getFilesSumSize();
	}

	
	/**
	 * Try to retrieve value object from storage by specified key.
	 * @param key
	 * @return Value object if it's in storage, null otherwise.
	 * @throws IOException
	 */
	private Object tryGet(String key) throws IOException {
		
		FsCachePosition pos = positions.get(key);
		if (pos == null) return null;
		
		FsCacheFile file = pos.getFile();
		file.getChannel().position(pos.getOffset());
		return ioHandler.readValue(file.getChannel(), pos.getSize());
	}
	
	
	/**
	 * Persists entry to storage
	 * @param key
	 * @param value
	 * @return Position of entry
	 * @throws IOException
	 * @throws InvalidClassException
	 * @throws NotSerializableException
	 * @throws TooBigEntryException if sum size of key and object is too big to save then to one file.
	 */
	private synchronized FsCachePosition persistEntry(String key, Object value) 
			throws IOException, InvalidClassException, NotSerializableException, TooBigEntryException {
		
		ByteBuffer buffer = prepareToPersist(key, value, null);
		return ioHandler.writeEntryBuffer(files.get(files.lastKey()), buffer);
	}
	
	
	/**
	 * Persists set of entries
	 * @param entriesMap
	 * @throws IOException
	 */
	private synchronized void persistEntries(Map<String, Object> entriesMap) throws IOException {
		
		if (entriesMap.isEmpty()) {
			return;
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(10000);
		for (Map.Entry<String, Object> entry : entriesMap.entrySet()) {
			
			if (entry.getValue() instanceof Serializable) {	
				try {
					buffer = prepareToPersist(entry.getKey(), entry.getValue(), buffer);
				} catch (InvalidClassException | NotSerializableException e) {
					logger.log(Level.WARNING, "Entry persistent error", e);
					continue;
				} catch (TooBigEntryException e) {
					logger.log(Level.WARNING, "Too big entry: key = " + entry.getKey(), e);
					continue;
				}
				ioHandler.writeEntryBuffer(files.get(files.lastKey()), buffer);
			}
		}
	}
	
	
	/**
	 * Prepare file storage and transfer buffer to save given key-value entry.
	 * If buffer doesn't have enough capacity to transfer entry, creates new
	 * and allocate appropriate memory size.
	 * 
	 * @param key
	 * @param value
	 * @param buffer
	 * @return Buffer containing serialized representation of given key-value entry
	 * @throws IOException
	 * @throws InvalidClassException
	 * @throws NotSerializableException
	 * @throws TooBigEntryException
	 */
	private ByteBuffer prepareToPersist(String key, Object value, ByteBuffer buffer) 
			throws IOException, InvalidClassException, NotSerializableException, TooBigEntryException {
		
		buffer = ioHandler.writeEntryToBuffer(key, value, buffer, fsCacheFileMaxSize);
		
		if (files.isEmpty()) {
			addNewFile();  // create new file if the storage is empty
		} else {
			FsCacheFile fileToWrite = files.get(files.lastKey());
			
			// if current open for writing file doesn't have enough space to save entry,
			// create new file and remove oldest files to keep storage size satisfying max size
			if (fileToWrite.getSize() + buffer.limit() > fsCacheFileMaxSize) {
				clearSizeForNewFile();
				addNewFile();
			}
		}
		
		return buffer;
	}
	
	
	/**
	 * Load entries positions index
	 */
	private void loadData() {
		
		// load list of actual data files
		loadFiles();
		
		// read files and create index
		loadPositions();
		
		if (files.isEmpty()) {
			logger.info("No files loaded");
		} else {
			List<String> loadedFileNames = new ArrayList<String>();
			for (FsCacheFile file : files.values()) {
				loadedFileNames.add(file.getPath().getFileName().toString());
			}
			logger.info("Data files loaded: " + loadedFileNames.toString());
		}
		
		// remove data files that didn't get in storage
		List<String> removedFileNames = removeUnusedFiles();
		if (!removedFileNames.isEmpty()) {
			logger.info("Removed unused files: " + removedFileNames.toString());
		}
		
	}
	
	
	/**
	 * Load list of data files
	 */
	private void loadFiles() {
		
		files.clear();
		
		long maxFsSize = settings.getFsCacheMaxSize();
		
		List<Path> files = null;
		try {
			files = getDataFiles(settings.getFsCacheDirPath());
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load data files", e);
			return;
		}
		
		Collections.sort(files, new Comparator<Path>() {
			@Override
			public int compare(Path p1, Path p2) {
				return p2.getFileName().toString().compareTo(p1.getFileName().toString());
			}
		});
		
		long allSize = 0;
		for (Path path : files) {
			long fileSize;
			try {
				fileSize = Files.size(path);
				if (allSize + fileSize > maxFsSize) {
					break;
				}
				allSize += fileSize;
				includeFile(path, fileSize, path != files.get(0)); // last file should be open for writing
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to load data file: " + path.toString(), e);
				return;
			}
		}
		
	}
	

	/**
	 * Create entries positions index
	 */
	private void loadPositions() {
		
		positions.clear();
		
		Iterator<FsCacheFile> iter = files.values().iterator();
		while (iter.hasNext()) {
			
			FsCacheFile file = iter.next();
			try {
				ioHandler.readPositions(file, positions);
			} catch (IOException e) {
				
				// If there were error while loading positions
				// some removing entries can be lost that can result in
				// stale values presence. So to prevent this we removes 
				// all previously loaded data.
				
				iter.remove();
				closeFileChannel(file);
				positions.clear();
				logger.log(Level.WARNING, "Failed to read entries from data file " + file.getPath(), e);
			}
		}
	}
	
	
	/**
	 * Removes oldest files (and corresponding entries from index)
	 * to assure that new data file can be created.
	 */
	private void clearSizeForNewFile() {
		
		long fsSize = getFilesSumSize();
		long filesCount = files.size();
		
		Set<Integer> toRemoveFileNumbers = new HashSet<>();
		Set<FsCacheFile> toRemoveFiles = new HashSet<>();
		
		for (Map.Entry<Integer, FsCacheFile> entry : files.entrySet()) {
			
			if (fsSize + fsCacheFileMaxSize <= settings.getFsCacheMaxSize() && filesCount < settings.getFsCacheFilesCount()) {
				break;
			}
			
			FsCacheFile file = entry.getValue();
			toRemoveFileNumbers.add(entry.getKey());
			toRemoveFiles.add(file);
			fsSize -= file.getSize();
			filesCount--;
		}
		
		if (!toRemoveFiles.isEmpty()) {
			
			Iterator<Map.Entry<String, FsCachePosition>> pi = positions.entrySet().iterator();
			while (pi.hasNext()) {
				FsCacheFile file = pi.next().getValue().getFile();
				if (toRemoveFiles.contains(file)) {
					pi.remove();
				}
			}
			
			for (Integer fileNumber : toRemoveFileNumbers) {
				deleteFile(files.remove(fileNumber));
			}
		}
		
	}
	
	
	/**
	 * Include given data file to storage.
	 * @param path
	 * @param size
	 * @param forReading
	 * @throws IOException
	 */
	private void includeFile(Path path, long size, boolean forReading) throws IOException {
		FileChannel channel = forReading ? FileChannel.open(path, StandardOpenOption.READ) :
			FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
		files.put(parseFileNumber(path), new FsCacheFile(path, size, channel));
	}
	
	
	/**
	 * Parse file number from file name
	 * @param file
	 * @return Parsed number
	 */
	private static int parseFileNumber(Path file) {
		return Integer.parseInt(file.getFileName().toString().substring(3, 11));
	}
	
	
	/**
	 * Generate data file path according to given number
	 * @param fileNumber
	 * @return File path
	 */
	private Path generateFilePath(int fileNumber) {
		return Paths.get(settings.getFsCacheDirPath(), String.format("tlv%08d.fsc", fileNumber));
	}
	
	
	/**
	 * Removes from storage directory all data files that did not get in storage
	 * @return List of removed files
	 */
	private List<String> removeUnusedFiles() {
		
		List<String> removed = new ArrayList<String>();
		
		// get list of all data files in directory
		List<Path> allFiles = null;
		try {
			allFiles = getDataFiles(settings.getFsCacheDirPath());
		} catch (IOException e) {
			return removed;
		}
		
		// remove files which data is not in cache
		for (Path file : allFiles) {
			if (!files.containsKey(parseFileNumber(file))) {
				try {
					Files.delete(file);
					removed.add(file.getFileName().toString());
				} catch (IOException e) {
					logger.log(Level.WARNING, "Failed to remove unused data file: " + file.getParent(), e);
				}
			}
		}
		return removed;
	}
	
	
	/**
	 * Returns list of data files in specified directory
	 * @param dirPath
	 * @return
	 * @throws IOException
	 */
	private List<Path> getDataFiles(String dirPath) throws IOException {
		
		Path dir = Paths.get(dirPath);
		DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
		
		List<Path> result = new ArrayList<>();
		for (Path path : stream) {
			if (Files.isRegularFile(path) && DATA_FILES_PATTERN.matcher(path.getFileName().toString()).matches()) {
				result.add(path);
			}
		}
		
		return result;
	}
	
	
	/** 
	 * Creates and add to storage new file
	 * @return Object representing storage data file
	 * @throws IOException
	 */
	private FsCacheFile addNewFile() throws IOException {
		int newFileNumber = (files.isEmpty() ? 1 : files.lastKey() + 1);
		Path path = generateFilePath(newFileNumber);
		FileChannel channel = FileChannel.open(path, 
			StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ);
		FsCacheFile newFile = new FsCacheFile(path, 0, channel);
		files.put(newFileNumber, newFile);
		logger.info("Data file created: " + newFile.getPath().getFileName().toString());
		return newFile;
	}
	
	
	/**
	 * Close channel for given file
	 * @param file
	 */
	private void closeFileChannel(FsCacheFile file) {
		try {
			file.getChannel().close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to close file channel: " + file.getPath(), e);
		}
	}
	
	
	/**
	 * Delete specified file from disk
	 * @param file
	 */
	private void deleteFile(FsCacheFile file) {
		
		closeFileChannel(file);
		try {
			Files.delete(file.getPath());
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to delete data file: " + file.getPath(), e);
		}
		logger.info("Data file removed: " + file.getPath().getFileName());
	}
	
	
	/**
	 * Returns sum size of storage (sum size of all data files)
	 * @return
	 */
	private long getFilesSumSize() {
		long result = 0;
		for (FsCacheFile file : files.values()) {
			result += file.getSize();
		}
		return result;
	}
}
