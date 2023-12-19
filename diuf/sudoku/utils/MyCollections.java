/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.solver.AHint;
import static diuf.sudoku.utils.MyStrings.SB;
import java.util.Collection;
import java.util.Set;

/**
 * Static helper methods for {@link java.util.Collection }.
 *
 * @author Keith Corlett 2023-06-19
 */
public class MyCollections {

	public static boolean any(Collection<?> c) {
		return c!=null && !c.isEmpty();
	}

	public static boolean notNull(Collection<?> c) {
		for ( Object o : c ) {
			if ( o == null ) {
				return false;
			}
		}
		return true;
	}

	public static String toString(Collection<?> c) {
		StringBuilder sb = SB(c.size()<<5); // size*16
		boolean first = true;
		for ( Object o : c ) {
			if ( first )
				first = false;
			else
				sb.append(", ");
			sb.append(String.valueOf(o));
		}
		return sb.toString();
	}

	private MyCollections() { }
}
