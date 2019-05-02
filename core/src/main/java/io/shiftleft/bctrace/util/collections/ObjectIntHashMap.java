package io.shiftleft.bctrace.util.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A hash map implementation of {@link ObjectIntMap} that uses open addressing for keys. To minimize
 * the memory footprint, this class uses open addressing rather than chaining. Collisions are
 * resolved using linear probing. Deletions implement compaction, so cost of remove can approach
 * O(N) for full maps, which makes a small loadFactor recommended.
 *
 * @param <K> The value type stored in the map.
 */
public class ObjectIntHashMap<K> implements ObjectIntMap<K> {

  /**
   * Default initial capacity. Used if not specified in the constructor
   */
  private static final int DEFAULT_CAPACITY = 11;

  /**
   * Default load factor. Used if not specified in the constructor
   */
  private static final float DEFAULT_LOAD_FACTOR = 0.5f;

  /**
   * The maximum number of elements allowed without allocating more space.
   */
  private int maxSize;

  /**
   * The load factor for the map. Used to calculate {@link #maxSize}.
   */
  private final float loadFactor;

  private K[] keys;
  private int[] values;
  private int size;

  public ObjectIntHashMap() {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
  }

  public ObjectIntHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  public ObjectIntHashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("initialCapacity must be >= 1");
    }
    if (loadFactor <= 0.0f || loadFactor > 1.0f) {
      // Cannot exceed 1 because we can never store more than capacity elements;
      // using a bigger loadFactor would trigger rehashing before the desired load is reached.
      throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
    }

    this.loadFactor = loadFactor;

    // Adjust the initial capacity if necessary.
    int capacity = adjustCapacity(initialCapacity);

    // Allocate the arrays.
    keys = (K[]) new Object[capacity];
    @SuppressWarnings({"unchecked",})
    int[] temp = new int[capacity];
    values = temp;

    // Initialize the maximum size value.
    maxSize = calcMaxSize(capacity);
  }

  @Override
  public int get(K key) {
    int index = indexOf(key);
    return index == -1 ? 0 : values[index];
  }

  @Override
  public int put(K key, int value) {
    if (value == 0) {
      return remove(key);
    }
    int startIndex = hashIndex(key);
    int index = startIndex;

    for (; ; ) {
      if (values[index] == 0) {
        // Found empty slot, use it.
        keys[index] = key;
        values[index] = value;
        growSize();
        return 0;
      } else if (keys[index].equals(key)) {
        // Found existing entry with this key, just replace the value.
        int previousValue = values[index];
        values[index] = value;
        return previousValue;
      }

      // Conflict, keep probing ...
      if ((index = probeNext(index)) == startIndex) {
        // Can only happen if the map was full at MAX_ARRAY_SIZE and couldn't grow.
        throw new IllegalStateException("Unable to insert");
      }
    }
  }

  private int probeNext(int index) {
    return index == values.length - 1 ? 0 : index + 1;
  }

  @Override
  public int remove(K key) {
    int index = indexOf(key);
    if (index == -1) {
      return 0;
    }

    int prev = values[index];
    removeAt(index);
    return prev;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public void clear() {
    Arrays.fill(keys, null);
    Arrays.fill(values, 0);
    size = 0;
  }

  @Override
  public boolean containsKey(K key) {
    return indexOf(key) >= 0;
  }

  @Override
  public boolean containsValue(int value) {
    int v = value;
    for (int i = 0; i < values.length; ++i) {
      // The map supports null values; this will be matched as NULL_VALUE.equals(NULL_VALUE).
      if (values[i] != 0 && values[i] == v) {
        return true;
      }
    }
    return false;
  }

  @Override
  public K[] keys() {
    K[] outKeys = (K[]) new Object[size()];
    int targetIx = 0;
    for (int i = 0; i < values.length; ++i) {
      if (values[i] != 0) {
        outKeys[targetIx++] = keys[i];
      }
    }
    return outKeys;
  }


  @Override
  public int hashCode() {
    // Hashcode is based on all non-zero, valid keys. We have to scan the whole keys
    // array, which may have different lengths for two maps of same size(), so the
    // capacity cannot be used as input for hashing but the size can.
    int hash = size;
    for (int i = 0; i < keys.length; ++i) {
      // 0 can be a valid key or unused slot, but won't impact the hashcode in either case.
      // This way we can use a cheap loop without conditionals, or hard-to-unroll operations,
      // or the devastatingly bad memory locality of visiting value objects.
      // Also, it's important to use a hash function that does not depend on the ordering
      // of terms, only their values; since the map is an unordered collection and
      // entries can end up in different positions in different maps that have the same
      // elements, but with different history of puts/removes, due to conflicts.
      hash = hash ^ keys[i].hashCode();
    }
    return hash;
  }

  /**
   * Locates the index for the given key. This method probes using double hashing.
   *
   * @param key the key for an entry in the map.
   * @return the index where the key was found, or {@code -1} if no entry is found for that key.
   */
  private int indexOf(K key) {
    int startIndex = hashIndex(key);
    int index = startIndex;

    for (; ; ) {
      if (values[index] == 0) {
        // It's available, so no chance that this value exists anywhere in the map.
        return -1;
      } else if (key == keys[index]) {
        return index;
      }

      // Conflict, keep probing ...
      if ((index = probeNext(index)) == startIndex) {
        return -1;
      }
    }
  }

  /**
   * Returns the hashed index for the given key.
   */
  private int hashIndex(K key) {
    return key.hashCode() & 0xfffffff % keys.length;
  }

  /**
   * Grows the map size after an insertion. If necessary, performs a rehash of the map.
   */
  private void growSize() {
    size++;

    if (size > maxSize) {
      // Need to grow the arrays. We take care to detect integer overflow,
      // also limit array size to ArrayList.MAX_ARRAY_SIZE.
      rehash(adjustCapacity((int) Math.min(keys.length * 2.0, Integer.MAX_VALUE - 8)));
    } else if (size == keys.length) {
      // Open addressing requires that we have at least 1 slot available. Need to refresh
      // the arrays to clear any removed elements.
      rehash(keys.length);
    }
  }

  /**
   * Adjusts the given capacity value to ensure that it's odd. Even capacities can break probing.
   */
  private static int adjustCapacity(int capacity) {
    return capacity | 1;
  }

  /**
   * Removes entry at the given index position. Also performs opportunistic, incremental rehashing
   * if necessary to not break conflict chains.
   *
   * @param index the index position of the element to remove.
   */
  private void removeAt(int index) {
    --size;
    // Clearing the key is not strictly necessary (for GC like in a regular collection),
    // but recommended for security. The memory location is still fresh in the cache anyway.
    keys[index] = null;
    values[index] = 0;

    // In the interval from index to the next available entry, the arrays may have entries
    // that are displaced from their base position due to prior conflicts. Iterate these
    // entries and move them back if possible, optimizing future lookups.
    // Knuth Section 6.4 Algorithm R, also used by the JDK's IdentityHashMap.

    int nextFree = index;
    for (int i = probeNext(index); values[i] != 0; i = probeNext(i)) {
      int bucket = hashIndex(keys[i]);
      if ((i < bucket && (bucket <= nextFree || nextFree <= i))
          || (bucket <= nextFree && nextFree <= i)) {
        // Move the displaced entry "back" to the first available position.
        keys[nextFree] = keys[i];
        values[nextFree] = values[i];
        // Put the first entry after the displaced entry
        keys[i] = null;
        values[i] = 0;
        nextFree = i;
      }
    }
  }

  /**
   * Calculates the maximum size allowed before rehashing.
   */
  private int calcMaxSize(int capacity) {
    // Clip the upper bound so that there will always be at least one available slot.
    int upperBound = capacity - 1;
    return Math.min(upperBound, (int) (capacity * loadFactor));
  }

  /**
   * Rehashes the map for the given capacity.
   *
   * @param newCapacity the new capacity for the map.
   */
  private void rehash(int newCapacity) {
    K[] oldKeys = keys;
    int[] oldVals = values;

    keys = (K[]) new Object[newCapacity];
    @SuppressWarnings({"unchecked"})
    int[] temp = new int[newCapacity];
    values = temp;

    maxSize = calcMaxSize(newCapacity);

    // Insert to the new arrays.
    for (int i = 0; i < oldVals.length; ++i) {
      int oldVal = oldVals[i];
      if (oldVal != 0) {
        // Inlined put(), but much simpler: we don't need to worry about
        // duplicated keys, growing/rehashing, or failing to insert.
        K oldKey = oldKeys[i];
        int startIndex = hashIndex(oldKey);
        int index = startIndex;

        for (; ; ) {
          if (values[index] == 0) {
            keys[index] = oldKey;
            values[index] = oldVal;
            break;
          }

          // Conflict, keep probing. Can wrap around, but never reaches startIndex again.
          index = probeNext(index);
        }
      }
    }
  }

  @Override
  public String toString() {
    if (size == 0) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder(4 * size);
    for (int i = 0; i < values.length; ++i) {
      int value = values[i];
      if (value != 0) {
        sb.append(sb.length() == 0 ? "{" : ", ");
        sb.append(keys[i]).append('=').append(value);
      }
    }
    return sb.append('}').toString();
  }

  public static void main(String[] args) {
    ObjectIntMap<String> map = new ObjectIntHashMap<String>();
    map.put("hola", 3);
    map.put("adios", 0);
    map.put("adios", 5);

    System.out.println(map.get("hola"));
    System.out.println(map.get("adios"));
    System.out.println(map.get("holda"));
  }
}