//package diuf.sudoku.utils;
//
///**
// * IntIntTreeMap implements a basic Red-Black tree of two int's.
// *
// * This is a copy-paste of Java 8's {@code java.util.TreeMap<int, int>} that
// * retains only the operations I use. Note that IntIntTreeMap is <b>NOT</b> a
// * java.util.Map!
// * 
// * It is presumed that integers in this map will all be positive. The NOT_FOUND
// * value is currently -1. If that doesn't work for you then change the value
// * of NOT_FOUND, but beware that you must retest all usages of IntIntTreeMap.
// *
// * My requirement is for a fast red/black tree: a self balancing binary tree to
// * use as a list index, so I want to avoid the delay of boxing and unboxing
// * Integers (or even Ints, with faster compareTo and better caching).
// *
// * The retained operations I actually use are: clear, put, get and the
// * underlying red/black operations. size() is also retained, just in case.
// * There is no iterator(), or keySet() or values() or anything you associate
// * with a Map, coz I'm not a Map, just a basic red-black tree of two ints.
// *
// * Initially I tried "doing it the hard way" and went mad twice, over two days,
// * trying to implement a "raw int" version of everything that TreeMap uses, and
// * eventually gave up and just did it minimally.
// *
// * IntIntTreeMap saves 7 seconds for top1465.d5.mt. Wow! Sigh.
// *
// * @author Keith Corlett 2020-08-03
// */
//public final class IntIntTreeMap {
//
//	/** Get returns NOT_FOUND (-1) when he can't find your s__t. */
//	public static final int NOT_FOUND = -1;
//
//	private transient Entry root;
//
//	/** The number of entries in the tree */
//	private transient int size = 0;
//
//	/** Constructs a new empty IntIntTreeMap */
//	public IntIntTreeMap() {
//	}
//
//	/** @return the number of entries in this map */
//	public final int size() {
//		return size;
//	}
//
//	/**
//	 * Gets the value for the given key, else {@code NOT_FOUND} (-1).
//	 *
//	 * @param key the int you seek
//	 * @return the int value, else NOT_FOUND (-1)
//	 */
//	public int get(int key) {
//		Entry p = getEntry(key);
//		return (p==null ? NOT_FOUND : p.value);
//	}
//
//	private Entry getEntry(int key) {
//		// ripped-out comparator-based version for sake of performance
//		Entry p = root;
//		while (p != null) {
//			int cmp = p.key - key;
//			if (cmp < 0)
//				p = p.left;
//			else if (cmp > 0)
//				p = p.right;
//			else
//				return p;
//		}
//		return null;
//	}
//
//	/**
//	 * Associates the specified value with the specified key in this map.
//	 * If the map previously contained a mapping for the key, the old
//	 * value is replaced.
//	 *
//	 * @param key key with which the specified value is to be associated
//	 * @param value value to be associated with the specified key. I assert
//	 *  that value != NOT_FOUND so developers will see this. Users will just
//	 *  get strange behaviour: a value added will be unretrievable.
//	 *
//	 * @return the previous value associated with {@code key}, or
//	 *         {@code null} if there was no mapping for {@code key}.
//	 *         (A {@code null} return can also indicate that the map
//	 *         previously associated {@code null} with {@code key}.)
//	 * @throws ClassCastException if the specified key cannot be compared
//	 *         with the keys currently in the map
//	 * @throws NullPointerException if the specified key is null
//	 *         and this map uses natural ordering, or its comparator
//	 *         does not permit null keys
//	 */
//	public int put(int key, int value) {
//		assert value != NOT_FOUND; // don't add my NOT_FOUND value!
//		Entry t = root;
//		if (t == null) {
//			root = new Entry(key, value, null);
//			size = 1;
//			return NOT_FOUND;
//		}
//		int cmp;
//		Entry parent;
//		do {
//			parent = t;
//			cmp = t.key - key;
//			if (cmp < 0)
//				t = t.left;
//			else if (cmp > 0)
//				t = t.right;
//			else
//				return t.setValue(value);
//		} while (t != null);
//		Entry e = new Entry(key, value, parent);
//		if (cmp < 0)
//			parent.left = e;
//		else
//			parent.right = e;
//		fixAfterInsertion(e);
//		size++;
//		return NOT_FOUND;
//	}
//
//	/**
//	 * Removes the mapping for the given key from this map and returns it's
//	 * value, else returns NOT_FOUND (-1).
//	 *
//	 * @param  key key for which mapping should be removed
//	 * @return the previous value associated with {@code key},
//	 *  or {@code NOT_FOUND} (-1) if there was no mapping for {@code key}.
//	 *  Note that a {@code NOT_FOUND} return may also indicate that
//	 *  {@code NOT_FOUND} was previously associated with {@code key}.
//	 *  It is presumed that integers in this map will all be positive.
//	 */
//	public int remove(int key) {
//		Entry p = getEntry(key);
//		if (p == null)
//			return NOT_FOUND;
//		int oldValue = p.value;
//		deleteEntry(p);
//		return oldValue;
//	}
//
//	/**
//	 * Removes all of the mappings from this map.
//	 * The map will be empty after this call returns.
//	 */
//	public void clear() {
//		size = 0;
//		root = null;
//	}
//
//	// Red-black mechanics
//
//	private static final boolean RED   = false;
//	private static final boolean BLACK = true;
//
//	/**
//	 * An Entry is a node in the tree, which doubles as a means to pass
//	 * key-value pairs back.
//	 */
//	private static final class Entry {
//		int key;
//		int value;
//		Entry left;
//		Entry right;
//		Entry parent;
//		boolean color = BLACK;
//
//		/**
//		 * Make a new cell with given key, value, and parent, and with
//		 * {@code null} child links, and BLACK color.
//		 */
//		Entry(int key, int value, Entry parent) {
//			this.key = key;
//			this.value = value;
//			this.parent = parent;
//		}
//
//		/** @return the key */
//		public int getKey() { return key; }
//		/** @return the value associated with the key */
//		public int getValue() { return value; }
//
//		/**
//		 * Replaces the value currently associated with the key with the given
//		 * value.
//		 *
//		 * @return the value associated with the key before this method was
//		 *         called
//		 */
//		public int setValue(int value) {
//			int oldValue = this.value;
//			this.value = value;
//			return oldValue;
//		}
//
//		@Override
//		public boolean equals(Object o) {
//			if ( !(o instanceof Entry) )
//				return false;
//			Entry e = (Entry)o;
//			return key==e.getKey() && value==e.getValue();
//		}
//
//		@Override
//		public int hashCode() {
//			return key ^ value;
//		}
//
//		@Override
//		public String toString() {
//			return key + "=" + value;
//		}
//	}
//
////not used
////	/**
////	 * Returns the first Entry in this IntIntTreeMap (according to the key-sort
////	 * function). Returns null if this Map is empty.
////	 */
////	private final Entry getFirstEntry() {
////		Entry p = root;
////		if (p != null)
////			while (p.left != null)
////				p = p.left;
////		return p;
////	}
//
////not used
////	/**
////	 * Returns the last Entry in this IntIntTreeMap (according to the key-sort
////	 * function). Returns null if this Map is empty.
////	 */
////	private final Entry getLastEntry() {
////		Entry p = root;
////		if (p != null)
////			while (p.right != null)
////				p = p.right;
////		return p;
////	}
//
//	/**
//	 * Returns the successor of the specified Entry, or null if no such.
//	 */
//	private static Entry successor(Entry t) {
//		if (t == null)
//			return null;
//		else if (t.right != null) {
//			Entry p = t.right;
//			while (p.left != null)
//				p = p.left;
//			return p;
//		} else {
//			Entry p = t.parent;
//			Entry ch = t;
//			while (p != null && ch == p.right) {
//				ch = p;
//				p = p.parent;
//			}
//			return p;
//		}
//	}
//
////not used
////	/**
////	 * Returns the predecessor of the specified Entry, or null if no such.
////	 */
////	private static Entry predecessor(Entry t) {
////		if (t == null)
////			return null;
////		else if (t.left != null) {
////			Entry p = t.left;
////			while (p.right != null)
////				p = p.right;
////			return p;
////		} else {
////			Entry p = t.parent;
////			Entry ch = t;
////			while (p != null && ch == p.left) {
////				ch = p;
////				p = p.parent;
////			}
////			return p;
////		}
////	}
//
//	/**
//	 * Balancing operations.
//	 *
//	 * Implementations of rebalancings during insertion and deletion are a bit
//	 * different to the CLR version. Rather than using dummy nilnodes, we use a
//	 * set of accessors that deal properly with null.  They are used to avoid
//	 * messiness surrounding nullness checks in the main algorithms.
//	 */
//
//	private static boolean colorOf(Entry p) {
//		return p==null ? BLACK : p.color;
//	}
//
//	private static void setColor(Entry p, boolean c) {
//		if (p != null)
//			p.color = c;
//	}
//
//	private static Entry parentOf(Entry p) {
//		return p==null ? null: p.parent;
//	}
//
//	private static Entry leftOf(Entry p) {
//		return p==null ? null: p.left;
//	}
//
//	private static Entry rightOf(Entry p) {
//		return p==null ? null: p.right;
//	}
//
//	/** From CLR */
//	private void rotateLeft(Entry p) {
//		if (p != null) {
//			Entry r = p.right;
//			p.right = r.left;
//			if (r.left != null)
//				r.left.parent = p;
//			r.parent = p.parent;
//			if (p.parent == null)
//				root = r;
//			else if (p.parent.left == p)
//				p.parent.left = r;
//			else
//				p.parent.right = r;
//			r.left = p;
//			p.parent = r;
//		}
//	}
//
//	/** From CLR */
//	private void rotateRight(Entry p) {
//		if (p != null) {
//			Entry l = p.left;
//			p.left = l.right;
//			if (l.right != null) l.right.parent = p;
//			l.parent = p.parent;
//			if (p.parent == null)
//				root = l;
//			else if (p.parent.right == p)
//				p.parent.right = l;
//			else p.parent.left = l;
//			l.right = p;
//			p.parent = l;
//		}
//	}
//
//	/** From CLR */
//	private void fixAfterInsertion(Entry x) {
//		x.color = RED;
//
//		while (x != null && x != root && x.parent.color == RED) {
//			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
//				Entry y = rightOf(parentOf(parentOf(x)));
//				if (colorOf(y) == RED) {
//					setColor(parentOf(x), BLACK);
//					setColor(y, BLACK);
//					setColor(parentOf(parentOf(x)), RED);
//					x = parentOf(parentOf(x));
//				} else {
//					if (x == rightOf(parentOf(x))) {
//						x = parentOf(x);
//						rotateLeft(x);
//					}
//					setColor(parentOf(x), BLACK);
//					setColor(parentOf(parentOf(x)), RED);
//					rotateRight(parentOf(parentOf(x)));
//				}
//			} else {
//				Entry y = leftOf(parentOf(parentOf(x)));
//				if (colorOf(y) == RED) {
//					setColor(parentOf(x), BLACK);
//					setColor(y, BLACK);
//					setColor(parentOf(parentOf(x)), RED);
//					x = parentOf(parentOf(x));
//				} else {
//					if (x == leftOf(parentOf(x))) {
//						x = parentOf(x);
//						rotateRight(x);
//					}
//					setColor(parentOf(x), BLACK);
//					setColor(parentOf(parentOf(x)), RED);
//					rotateLeft(parentOf(parentOf(x)));
//				}
//			}
//		}
//		root.color = BLACK;
//	}
//
//	/**
//	 * Delete node p, and then rebalance the tree.
//	 */
//	private void deleteEntry(Entry p) {
//		size--;
//
//		// If strictly internal, copy successor's element to p and then make p
//		// point to successor.
//		if (p.left != null && p.right != null) {
//			Entry s = successor(p);
//			p.key = s.key;
//			p.value = s.value;
//			p = s;
//		} // p has 2 children
//
//		// Start fixup at replacement node, if it exists.
//		Entry replacement = (p.left != null ? p.left : p.right);
//
//		if (replacement != null) {
//			// Link replacement to parent
//			replacement.parent = p.parent;
//			if (p.parent == null)
//				root = replacement;
//			else if (p == p.parent.left)
//				p.parent.left  = replacement;
//			else
//				p.parent.right = replacement;
//
//			// Null out links so they are OK to use by fixAfterDeletion.
//			p.left = p.right = p.parent = null;
//
//			// Fix replacement
//			if (p.color == BLACK)
//				fixAfterDeletion(replacement);
//		} else if (p.parent == null) { // return if we are the only node.
//			root = null;
//		} else { //  No children. Use self as phantom replacement and unlink.
//			if (p.color == BLACK)
//				fixAfterDeletion(p);
//
//			if (p.parent != null) {
//				if (p == p.parent.left)
//					p.parent.left = null;
//				else if (p == p.parent.right)
//					p.parent.right = null;
//				p.parent = null;
//			}
//		}
//	}
//
//	/** From CLR */
//	private void fixAfterDeletion(Entry x) {
//		while (x != root && colorOf(x) == BLACK) {
//			if (x == leftOf(parentOf(x))) {
//				Entry sib = rightOf(parentOf(x));
//
//				if (colorOf(sib) == RED) {
//					setColor(sib, BLACK);
//					setColor(parentOf(x), RED);
//					rotateLeft(parentOf(x));
//					sib = rightOf(parentOf(x));
//				}
//
//				if (colorOf(leftOf(sib))  == BLACK &&
//					colorOf(rightOf(sib)) == BLACK) {
//					setColor(sib, RED);
//					x = parentOf(x);
//				} else {
//					if (colorOf(rightOf(sib)) == BLACK) {
//						setColor(leftOf(sib), BLACK);
//						setColor(sib, RED);
//						rotateRight(sib);
//						sib = rightOf(parentOf(x));
//					}
//					setColor(sib, colorOf(parentOf(x)));
//					setColor(parentOf(x), BLACK);
//					setColor(rightOf(sib), BLACK);
//					rotateLeft(parentOf(x));
//					x = root;
//				}
//			} else { // symmetric
//				Entry sib = leftOf(parentOf(x));
//
//				if (colorOf(sib) == RED) {
//					setColor(sib, BLACK);
//					setColor(parentOf(x), RED);
//					rotateRight(parentOf(x));
//					sib = leftOf(parentOf(x));
//				}
//
//				if (colorOf(rightOf(sib)) == BLACK &&
//					colorOf(leftOf(sib)) == BLACK) {
//					setColor(sib, RED);
//					x = parentOf(x);
//				} else {
//					if (colorOf(leftOf(sib)) == BLACK) {
//						setColor(rightOf(sib), BLACK);
//						setColor(sib, RED);
//						rotateLeft(sib);
//						sib = leftOf(parentOf(x));
//					}
//					setColor(sib, colorOf(parentOf(x)));
//					setColor(parentOf(x), BLACK);
//					setColor(leftOf(sib), BLACK);
//					rotateRight(parentOf(x));
//					x = root;
//				}
//			}
//		}
//
//		setColor(x, BLACK);
//	}
//
//}
