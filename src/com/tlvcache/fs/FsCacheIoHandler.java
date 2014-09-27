package com.tlvcache.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;


/**
 * Helper class to perform input/output operation
 * on file system cache files.
 */
class FsCacheIoHandler {
	
	private static final int MAX_KEY_SIZE = 1_000_000;
	private static final int MAX_VALUE_SIZE = 10_000_000;
	
	
	/**
	 * Writes given key and value to buffer.
	 * If sum size of serialized key and value exceeds specified maxEntrySize,
	 * TooBigEntryException is thrown.
	 * 
	 * @return Buffer with written data. If specified buffer lacks capacity to write
	 * new buffer is allocated and returned.
	 * 
	 * @throws IOException
	 * @throws TooBigEntryException
	 * @throws InvalidClassException
	 * @throws NotSerializableException
	 */
	ByteBuffer writeEntryToBuffer(String key, Object value, ByteBuffer buffer, long maxEntrySize) 
			throws IOException, TooBigEntryException, InvalidClassException, NotSerializableException {
		
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		// reserve 8 bytes for sizes of key and data
		final byte[] BYTES_INT = {0, 0, 0, 0, 0, 0, 0, 0}; 
		byteStream.write(BYTES_INT);
		
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		
		objectStream.writeObject(key);
		int keySize = byteStream.size() - 8;
		
		if (value != null) {
			objectStream.writeObject(value);
		}
		int valueSize = byteStream.size() - keySize - 8;
		
		byte bytes[] = byteStream.toByteArray();
		if (valueSize + keySize + 8 >= maxEntrySize) {
			throw new TooBigEntryException("size = " + (valueSize + keySize + 8) + ", required < " + maxEntrySize);
		}
		
		if (buffer == null || buffer.capacity() < bytes.length) {
			buffer = ByteBuffer.wrap(bytes);
		} else {
			buffer.limit(bytes.length);
			buffer.rewind();
			buffer.put(bytes);
			buffer.rewind();
		}
		
	    buffer.putInt(keySize);
		buffer.putInt(valueSize);
		buffer.rewind();
		return buffer;
	}
	
	
	/**
	 * Writes buffer content to specified file.
	 * @param file File to write to
	 * @param buffer Buffer with data
	 * @return Position in file where data is placed
	 * @throws IOException
	 */
	FsCachePosition writeEntryBuffer(FsCacheFile file, ByteBuffer buffer) throws IOException {
		FileChannel channel = file.getChannel();
		long entryPos = channel.position() + 8; // 8 bytes for sizes of key and data
		while(buffer.hasRemaining()) {
		    channel.write(buffer);
		}
		file.setSize(channel.position());
		return new FsCachePosition(file, entryPos, (int) (channel.position() - entryPos));
	}
	
	
	/**
	 * Reads from specified channel value 
	 * from current position for given entry size.
	 * @return Read object
	 * @throws IOException
	 */
	Object readValue(FileChannel channel, int entrySize) throws IOException {
		
		byte[] bytes = readBytes(channel, ByteBuffer.allocate(entrySize));
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		
		try {
			objectStream.readObject(); // read and skip key
			return objectStream.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	
	/**
	 * Reads from specified channel value pair of values representing
	 * size of serialized key and value.
	 * @throws IOException
	 */
	EntrySize readEntrySize(FileChannel channel) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		if (channel.read(buffer) < 8) throw new IOException("Cannot read entry size") ;
		buffer.rewind();
		int keySize = buffer.getInt();
		if (keySize < 1 || keySize > MAX_KEY_SIZE) {
			throw new IOException("Illegal key size: " + keySize);
		}
		int valueSize = buffer.getInt();
		if (valueSize < 0 || valueSize > MAX_VALUE_SIZE) {
			throw new IOException("Illegal value size: " + valueSize);
		}
		return new EntrySize(keySize, valueSize);
	}
	
	
	/**
	 * Reads from specified channel key of size <code>keySize</code>
	 * starting from current channel position.
	 * @throws IOException
	 */
	String readKey(FileChannel channel, int keySize) throws IOException {
		return readKey(channel, ByteBuffer.allocate(keySize));
	}
	
	
	/**
	 * Reads from specified channel key through specified buffer.
	 * @throws IOException
	 */
	String readKey(FileChannel channel, ByteBuffer buffer) throws IOException {
		return readKey(readBytes(channel, buffer));
	}
	
	
	/** 
	 * Reads and deserializes key from given bytes
	 * @throws IOException
	 */
	String readKey(byte[] bytes) throws IOException {
		
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		
		try {
			return (String) objectStream.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	
	/**
	 * Reads from specified channel byte array of given size
	 * @throws IOException
	 */
	byte[] readBytes(FileChannel channel, int size) throws IOException {
		return readBytes(channel, ByteBuffer.allocate(size));
	}
	
	
	/**
	 * Reads from specified channel byte array using given buffer
	 * (buffer limit considered to define the size of array)
	 * @throws IOException
	 */
	byte[] readBytes(FileChannel channel, ByteBuffer buffer) throws IOException {
		while (buffer.hasRemaining() && channel.read(buffer) != -1);
		if (buffer.hasRemaining()) throw new IOException("Failed to fill buffer: size = " + buffer.limit());
		buffer.rewind();
		return buffer.array();
	}
	
	
	/**
	 * Reads from specified files positions of all entries persisted there.
	 * Read data is put to specified map <code>positions</code>.
	*/
	void readPositions(FsCacheFile file, Map<String, FsCachePosition> positions) throws IOException {
		
		ByteBuffer buffer = ByteBuffer.allocate(5000);
		FileChannel channel = file.getChannel(); 
				
		while (channel.position() < file.getSize() - 8) {
			
			long position = channel.position();
			
			// read entry size
			EntrySize entrySize = readEntrySize(channel);
			if (buffer.capacity() < entrySize.keySize) {
				buffer = ByteBuffer.allocate(entrySize.keySize);
			}
			
			// read key
			buffer.clear();
			buffer.limit(entrySize.keySize);
			String key = readKey(channel, buffer);
			
			if (entrySize.valueSize > 0) {
				positions.put(key, new FsCachePosition(file, position + 8, entrySize.getWholeSize()));
			} else {
				// valueSize = 0 for removal entries so specified key should be removed
				positions.remove(key);
			}
			
			channel.position(channel.position() + entrySize.valueSize);
		}
	}
	
	
	/**
	 * Size of key and value in persistent entry
	 */
	private class EntrySize {
		
		private final int keySize;
		private final int valueSize;

		EntrySize(int keySize, int valueSize) {
			this.keySize = keySize;
			this.valueSize = valueSize;
		}
		
		int getWholeSize() {
			return keySize + valueSize;
		}
	}
}
