/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * MyMap is java.util.Map plus a new make method to get-else-put an element
 * atomically (in one operation). This makes sense when this Map is wrapped in
 * a Set, where all values are just a single instance of Object, so having the
 * value to add when putting a key is not an issue.
 *
 * @author Keith Corlett 2021-06-21
 */
interface IMyMap<K, V> extends java.util.Map<K, V> {
	public boolean visit(K key, V value);
}
