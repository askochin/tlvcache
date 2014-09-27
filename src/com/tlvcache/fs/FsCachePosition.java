package com.tlvcache.fs;

/**
 * Position of key-value entry inside cache storage.
 */
class FsCachePosition {
	
	
	/**
	 * File of storage
	 */
	private final FsCacheFile file;
	
	/**
	 * Offset in file
	 */
	private final long offset;
	
	/**
	 * Serialized entry size
	 */
	private final int size;
	
	
	
	
	FsCachePosition(FsCacheFile file, long offset, int size) {
		this.file = file;
		this.offset = offset;
		this.size = size;
	}

	public FsCacheFile getFile() {
		return file;
	}

	public long getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}
}
