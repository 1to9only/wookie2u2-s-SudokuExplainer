/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.utils.IntQueue;
import static diuf.sudoku.utils.IntQueue.QEMPTY;
import java.util.Collection;

/**
 * Static helper methods that are used in Als*Hint, that is the hints raised
 * by all subtypes of AAlsHinter, but I really do not need an AAlsHint because
 * I can get away with this AlsHintHelper instead. So AlsHintHelper exists
 * rather than repeat these methods in each Als*Hint class.
 * <p>
 * Note that this class and all of it is methods are only package visible. Each
 * method is individually static-imported.
 *
 * @author Keith Corlett 2021-
 */
class AlsHintHelper {

	/**
	 * Add new Links to $result from each $src to each $dst on v.
	 *
	 * @param src source indices
	 * @param dst destination indices
	 * @param v value to link on
	 * @param result to which I add new Links
	 */
	static void link(final Idx src, final Idx dst, final int v, final Collection<Link> result) {
		if ( src!=null && dst!=null ) { // Never null. NSN.
			int s; // source indice
			final int[] da = dst.toArrayNew(); // destination indices ONCE
			for ( final IntQueue q=src.indices(); (s=q.poll())>QEMPTY; )
				for ( int d : da )
					result.add(new Link(s, v, d, v));
		}
	}

	/**
	 * Add new Links to $result from each $src to $d on $v.
	 *
	 * @param src source Idx
	 * @param d destination indice
	 * @param v value to link
	 * @param result to which I add new Links
	 */
	static void link(final Idx src, final int d, final int v, final Collection<Link> result) {
		if ( src != null ) { // Never null. NSN.
			int s; // source indice
			for ( final IntQueue q=src.indices(); (s=q.poll())>QEMPTY; )
				result.add(new Link(s, v, d, v));
		}
	}

	private AlsHintHelper() { } // NEVER used

}
