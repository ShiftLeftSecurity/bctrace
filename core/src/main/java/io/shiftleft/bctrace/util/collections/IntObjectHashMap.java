package io.shiftleft.bctrace.util.collections;

import java.util.ConcurrentModificationException;

public class IntObjectHashMap<V> {

  private static final int INIT_CAPACITY = 4;

  transient int modCount;
  // Number of elements
  private int n;
  // Number of buckets
  private int m;
  // Linkedlist buckets
  private Node<V>[] st;

  public IntObjectHashMap() {
    this(INIT_CAPACITY);
  }

  public IntObjectHashMap(int m) {
    this.m = m;
    this.st = new Node[m];
  }

  private void resize(int chains) {
    ++modCount;
    IntObjectHashMap<V> temp = new IntObjectHashMap<V>(chains);
    for (int i = 0; i < m; i++) {
      Node<V> first = st[i];
      for (Node<V> n = first; n != null; n = n.next) {
        temp.put(n.key, n.val);
      }
    }
    this.m = temp.m;
    this.n = temp.n;
    this.st = temp.st;
  }

  private int hash(int key) {
    return (key & 0x7fffffff) % m;
  }

  public int size() {
    return n;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean contains(int key) {
    return get(key) != null;
  }

  public V get(int key) {
    int i = hash(key);
    return getValue(i, key);
  }

  public void put(int key, V val) {
    if (val == null) {
      remove(key);
      return;
    }

    // double table size if average length of list >= 10
    if (n >= 10 * m) {
      resize(2 * m);
    }

    int i = hash(key);
    if (!containsKey(i, key)) {
      n++;
    }
    put(i, key, val);
  }

  private void put(int bucket, int key, V value) {
    for (Node n = st[bucket]; n != null; n = n.next) {
      if (n.key == key) {
        n.val = value;
        return;
      }
    }
    Node n = new Node();
    n.key = key;
    n.val = value;
    n.next = st[bucket];
    st[bucket] = n;
    ++modCount;
  }

  private boolean containsKey(int bucket, int key) {
    for (Node n = st[bucket]; n != null; n = n.next) {
      if (n.key == key) {
        return true;
      }
    }
    return false;
  }

  private V getValue(int bucket, int key) {
    for (Node<V> n = st[bucket]; n != null; n = n.next) {
      if (n.key == key) {
        return n.val;
      }
    }
    return null;
  }

  private V delete(int bucket, int key) {
    Node prev = null;
    V ret = null;
    for (Node<V> n = st[bucket]; n != null; n = n.next) {
      if (n.key == key) {
        ++modCount;
        ret = n.val;
        if (prev == null) {
          st[bucket] = n.next;
        } else {
          prev.next = n.next;
        }
        break;
      }
      prev = n;
    }
    return ret;
  }

  public V remove(int key) {
    int i = hash(key);
    if (containsKey(i, key)) {
      n--;
    }
    V ret = delete(i, key);
    // halve table size if average length of list <= 2
    if (m > INIT_CAPACITY && n <= 2 * m) {
      resize(m / 2);
    }
    return ret;
  }

  public void visitEntries(EntryVisitor<V> visitor) {
    int mc = modCount;
    for (int i = 0; i < m; i++) {
      Node<V> first = st[i];
      Node<V> prev = null;
      for (Node<V> n = first; n != null; n = n.next) {

        if (visitor.remove(n.key, n.val)) {
          if (prev == null) {
            st[i] = n.next;
          } else {
            prev.next = n.next;
          }
          this.n--;
        }
        prev = n;
      }
    }
    if (modCount != mc) {
      throw new ConcurrentModificationException();
    }
    if (m > INIT_CAPACITY && n <= 2 * m) {
      resize(m / 2);
    }
  }

  public static interface EntryVisitor<V> {

    public boolean remove(int key, V value);
  }


  private static class Node<V> {

    private int key;
    private V val;
    private Node next;
  }

  public static void main(String[] args) {
    IntObjectHashMap<String> map = new IntObjectHashMap<String>();
    for (int i = 0; i < 100; i++) {
      map.put(i, "hola" + i);
      System.out.println(map.size());
      System.out.println("--> " + map.st.length);
    }

    for (int i = 2; i < 100; i++) {
      map.remove(i);
      System.out.println(map.size());
      System.out.println("<-- " + map.st.length);
    }

    System.out.println(map.get(0));
    System.out.println(map.get(1));
    System.out.println(map.get(2));
  }
}