/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import java.util.Collection;

/**
 * Static helper methods that are used in Als*Hint, that is the hints raised
 * by all subtypes of AAlsHinter, but I really don't need an AAlsHint because
 * I can get away with this AlsHintHelper instead. So AlsHintHelper exists
 * rather than repeat these methods in each Als*Hint class.
 * <p>
 * Note that this class and all of it's methods are only package visible. Each
 * method is individually static-imported.
 *
 * @author Keith Corlett 2021-
 */
class AlsHintHelper {

	static void link(final Idx src, final Idx dst, final int v, final Collection<Link> result) {
		if ( src!=null && dst!=null ) { // Never null. NSN.
			src.forEach((s)->dst.forEach((d)->result.add(new Link(s, v, d, v))));
		}
	}

	static void link(final Idx src, final int d, final int v, final Collection<Link> result) {
		if ( src != null ) { // Never null. NSN.
			src.forEach((s)->result.add(new Link(s, v, d, v)));
		}
	}

	private AlsHintHelper() { } // NEVER used
}
