package com.tlvcache.fs;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;


/**
 * A file containing cache data
 */
class FsCacheFile {
	
	private final Path path;
	private final FileChannel channel;
	private volatile long size;
	
	
	FsCacheFile(Path path, long size, FileChannel channel) {
		this.path = path;
		this.size = size;
		this.channel = channel;
	}


	public Path getPath() {
		return path;
	}


	public long getSize() {
		return size;
	}
	
	
	public void setSize(long size) {
		this.size = size;
	}


	public FileChannel getChannel() throws IOException {
		return channel;
	}

}
