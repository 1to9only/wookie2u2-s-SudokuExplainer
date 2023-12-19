/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import java.util.Map;

/**
 *
 * @author Keith Corlett
 */
interface IMyMap<K, V> extends java.util.Map<K, V> {

	public Map.Entry<K, V> poll();

}
