package com.tlvcache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Memory cache implementing the LFU (Least Frequently Used) 
 * eviction strategy
 */
public class TlvLfuMemoryCache implements TlvMemoryCache {

	/**
	 * Max number of entries
	 */
	private final long maxSize;
	
	/**
	 * Current entries count.
	 * Made <code>volatile</code> to provide visibility 
	 * from different threads
	 */
	private volatile long entriesCount;
	
	/**
	 * Map that keeps key-value associations.
	 */
	private final ConcurrentHashMap<String, DataEntry> map = new ConcurrentHashMap<>();
	
	/**
	 * Handler to process evicted entries
	 */
	private final EvictionHandler evictionHandler;
	
	/**
	 * Stack of entries to keep them in order of how frequently they used
	 */
	private final HitchedStack stack = new HitchedStack();

	
	public TlvLfuMemoryCache(TlvCacheSettings settings, EvictionHandler evictionHandler) {
		maxSize = settings.getMemoryCacheMaxSize();
		this.evictionHandler = evictionHandler;
	}
	
	
	/**
	 * Returns the value associated with key in this cache,
	 * or null if there is no cached value for key
	 */
	@Override
	public Object get(String key) {
		DataEntry entry = map.get(key);
		if (entry == null || entry.getValue() == null) {
			return null;
		}
		entry.incrementHits();
		return entry.getValue();
	}

	
	/**
	 * Puts value to cache (associates the given value 
	 * the given with key in this cache)
	 */
	@Override
	public void put(String key, Object value) {
		
		DataEntry newEntry = new DataEntry(key, value);
		DataEntry oldEntry = map.put(key, newEntry);
		
		DataEntry evictedEntry = null;
		
		// guard access to entries stack
		synchronized (stack) {
			
			// if there was previous value for given key
			// we should set new entry at the same place in the entries stack
			if (oldEntry != null) {
				newEntry.hits = oldEntry.hits;
				newEntry.next = oldEntry.next;
				newEntry.prev = oldEntry.prev;
				oldEntry.next.prev = newEntry;
				oldEntry.prev.next = newEntry;
				oldEntry.next = null;
				oldEntry.prev = null;
			} 
			
			// if there was not previous value for given key
			// put new entry on the top of stack
			else {
				evictedEntry = stack.push(newEntry);
				if (evictedEntry == null) {
					entriesCount++;
				} 
			}
		}
		
		// if there was evicted entry then remove it from map and pass to handler
		if (evictedEntry != null) {
			map.remove(evictedEntry.key);
			if (evictedEntry.getValue() != null) {
				evictionHandler.onEvicted(evictedEntry.getKey(), evictedEntry.getValue());
			}
		}
	}

	
	/**
	 * Discards any cached value for given key
	 */
	@Override
	public void remove(String key) {
		
		DataEntry removedEntry = map.remove(key);
		
		// guard access to entries stack and entriesCount
		synchronized (stack) {
			if (removedEntry != null) {
				removedEntry.removeFromQueue();
			}
			entriesCount--;
		}
	}
	

	/**
	 * Returns map containing all key-entry pairs in cache
	 * which values are garbage-collected
	 */
	@Override
	public Map<String, Object> getContent() {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, DataEntry> entry : map.entrySet()) {
			if (entry.getValue().getValue() != null) {
				result.put(entry.getKey(), entry.getValue().getValue());
			}
		}
		return result;
	}


	/**
	 * Returns snapshot of current cache content
	 */
	@Override
	public Map<String, Object> getContentSnapshot() {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, DataEntry> entry : map.entrySet()) {
			Object value = entry.getValue().getValue();
			result.put(entry.getKey(), entry.getValue().hits + " - " + (value == null ? "null" : value.toString()));
		}
		return result;
	}
	
	
	/**
	 * Returns a String representing current cache state description
	 */
	@Override
	public String getStateDescription() {
		return "size = " + entriesCount;
	}
	
	
	/**
	 * Entries stack to provide LFU eviction strategy.
	 * 
	 * Entries in stack partially sorted according to frequency of their usage 
	 * (how many times they were retrieved with get cache operation).
	 * Each retrieve is called hit. 
	 * 
	 * Stack is separated with special nodes called hitches.
	 * Hitch is like a bookmark in stack. All entries that are
	 * under certain hitch are guarantied to have equal or more hits count
	 * than hitch min hits count.
	 * 
	 * This implementation doesn't provide strict order of entries.
	 * It just guaranties that entry to evict is taken from top not empty
	 * hitch section.
	 */
	private class HitchedStack {
		
		private static final int HITCHES_COUNT = 256;
		
		/**
		 * Hitches in maps are sorted according to its upper bound (hitsMax)
		 */
		private final NavigableMap<Integer, HitchEntry> hitchesMap = new TreeMap<>();
		
		/**
		 * Pointer to stack top (hitch with minimal value of hitsMax)
		 */
		private final HitchEntry top;
		
		HitchedStack() {
			
			// fill hitches map before stack is starting to be used
			HitchEntry hitches[] = new HitchEntry[HITCHES_COUNT];
			int count = HITCHES_COUNT, pos = 0, d = 0, min = 0;
			while (count > 1) {
				count = count / 2;
				d = (d == 0) ? 1 : d * 2;
				for (int i = 0; i < count; i++) {
					hitches[pos++] = new HitchEntry(min, min + d - 1);
					min += d;
				}
			}
			hitches[pos] = new HitchEntry(min, Integer.MAX_VALUE);
			
			for (int i = 0; i < pos; i++) {
				hitches[i].next = hitches[i + 1];
				hitches[i + 1].prev = hitches[i];
				hitchesMap.put(hitches[i].hitsMax, hitches[i]);
			}
			hitchesMap.put(hitches[pos].hitsMax, hitches[pos]);
			top = hitches[0];
		}
		
		
		/**
		 * Push entry on the top of stack and evict one of
		 * previously pushed entries if max cache size exceeded
		 */
		DataEntry push(DataEntry newEntry) {
			DataEntry removed = null;
			if (entriesCount == maxSize) {
				removed = removeLeastFrequent();
			}
			newEntry.insertAfter(top);
			return removed;
		}
		
		
		/**
		 * Evict least frequent entry from stack
		 */
		DataEntry removeLeastFrequent() {
			
			HitchEntry currHitch = top;
			StackEntry currEntry = top.next;

			while (currEntry != null && currEntry.getHits() > currHitch.hitsMax) {
				if (currEntry.isHitch()) {
					currHitch = (HitchEntry) currEntry;
					currEntry = currEntry.next;
				}
				else {
					
					// if entry is placed in current hitch section 
					// but doesn't suit its bounds - move it to the right section
					
					StackEntry movingEntry = currEntry;
					currEntry = currEntry.next;
					movingEntry.removeFromQueue();
					HitchEntry ceilingHitch = hitchesMap.ceilingEntry(movingEntry.getHits()).getValue();
					movingEntry.insertAfter(ceilingHitch);
				}
			}
			
			currEntry.removeFromQueue(); 
			return (DataEntry) currEntry;
		}
	}
	
	
	/**
	 * Stack node base class
	 */
	private abstract class StackEntry {
		
		StackEntry prev = null; // previous node
		StackEntry next = null; // next node
		
		abstract boolean isHitch();
		abstract int getHits();
		
		/**
		 * Removes this node from stack
		 */
		void removeFromQueue() {
			prev.next = next;
			next.prev = prev;
			prev = null;
			next = null;
		}
		
		/**
		 * Inserts this node in stack after specified node
		 * @param prevEntry Node to insert after
		 */
		void insertAfter(StackEntry prevEntry) {
			prev = prevEntry;
			next = prevEntry.next;
			prev.next = this;
			next.prev = this;
		}
	}
	
	
	/**
	 * Hitch node of entries stack
	 */
	private class HitchEntry extends StackEntry {
		
		private final int hitsMin;  // min hits count
		private final int hitsMax;  // max hits count
		
		public HitchEntry(int hitsMin, int hitsMax) {
			this.hitsMin = hitsMin;
			this.hitsMax = hitsMax;
		}

		@Override
		boolean isHitch() {
			return true;
		}
		
		@Override
		int getHits() {
			return hitsMin;
		}
		
		@Override
		public String toString() {
			return "[" + hitsMin + ", " + hitsMax + "]";
		}
	}
	
	
	/**
	 * Cache data associated with cache key and
	 * stack node in entries stack
	 */
	private class DataEntry extends StackEntry {
		
		private final String key;
		private volatile int hits = 0;
		
		/**
		 * Values are saved as soft references to prevent OutOfMemory exception
		 * if stored values are too large.
		 */
		private final SoftReference<Object> valueRef;
		
		
		public DataEntry(String key, Object value) {
			this.key = key;
			this.valueRef = new SoftReference<Object>(value);
		}
		
		@Override
		public String toString() {
			return "[" + key + ", " + hits + "]";
		}
		
		@Override
		boolean isHitch() {
			return false;
		}

		
		@Override
		public int getHits() {
			return hits;
		}

		public void incrementHits() {
			hits++;
		}

		public String getKey() {
			return key;
		}

		public Object getValue() {
			return valueRef.get();
		}
	}
}
