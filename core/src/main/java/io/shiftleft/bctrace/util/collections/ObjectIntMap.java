package io.shiftleft.bctrace.util.collections;

/**
 * Interface for a primitive map that uses {@code int}s as values.
 *
 * @param <K> the key type used in the map.
 */
public interface ObjectIntMap<K> {

  /**
   * An Entry in the map.
   *
   * @param <K> the key type used in the map.
   */
  interface Entry<K> {

    /**
     * Gets the key for this entry.
     */
    K key();

    /**
     * Gets the value for this entry.
     */
    int value();

    /**
     * Sets the value for this entry.
     */
    void setValue(int value);
  }

  /**
   * Gets the value in the map with the specified key.
   *
   * @param key the key whose associated value is to be returned.
   * @return the value or {@code null} if the key was not found in the map.
   */
  int get(K key);

  /**
   * Puts the given entry into the map.
   *
   * @param key the key of the entry.
   * @param value the value of the entry.
   * @return the previous value for this key or {@code null} if there was no previous mapping.
   */
  int put(K key, int value);

  /**
   * Removes the entry with the specified key.
   *
   * @param key the key for the entry to be removed from this map.
   * @return the previous value for the key, or {@code null} if there was no mapping.
   */
  int remove(K key);

  /**
   * Returns the number of entries contained in this map.
   */
  int size();

  /**
   * Indicates whether or not this map is empty (i.e {@link #size()} == {@code 0]).
   */
  boolean isEmpty();

  /**
   * Clears all entries from this map.
   */
  void clear();

  /**
   * Indicates whether or not this map contains a value for the specified key.
   */
  boolean containsKey(K key);

  /**
   * Indicates whether or not the map contains the specified value.
   */
  boolean containsValue(int value);

  /**
   * Gets the keys contained in this map.
   */
  K[] keys();
}
