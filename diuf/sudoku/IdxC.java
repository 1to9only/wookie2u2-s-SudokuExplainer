/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;
import static diuf.sudoku.utils.IntArray.THE_EMPTY_INT_ARRAY;

/**
 * IdxC (for cached) is an Idx with indices and plusBuds cache. I have my
 * own class rather than litter Idx with the extra fields that are not used
 * in most instances of Idx. If you think an Idx is iterated repeatedly then
 * make it an IdxC. It'll probably be faster. If it isn't put it back.
 *
 * @author Keith Corlett 2023-10-16
 */
public class IdxC extends Idx {

	private static final long serialVersionUID = 978900552211048L;

	/** the result of {@link #plusBuds() } is cached. */
	private Idx plusBuds;

	/** masks cached by {@link #plusBuds() }. */
	private long pbA; private int pbB;

	/** masks cached by {@link #newArray() }. */
	private long arA; private int arB;

	/** A cache of the indices in this Idx, for my use, and external use. */
	private int[] array;

	public IdxC() {
		super();
	}

	/**
	 * Get a cached Idx of cells in this Idx plus there buddies.
	 * <p>
	 * If this is the first call, or this idx has changed since the previous
	 * call, then plusBuds are recalculated. Cache hit-rate is about 85%, a
	 * significant enspeedonation.
	 *
	 * @return the indices in this Idx and all of there buddies, CACHED!
	 */
	public Idx plusBuds() {
		assert (m0|m1) > 0L;
		if ( m0!=pbA || m1!=pbB ) { // about 15% of calls are cache misses
			long pb0 = pbA = m0;
			int  pb1 = pbB = m1;
			for ( int i : indicesCached() ) {
				pb0 |= BUDS_M0[i];
				pb1 |= BUDS_M1[i];
			}
			if ( plusBuds == null )
				plusBuds = new Idx(pb0, pb1);
			else
				plusBuds.set(pb0, pb1);
		}
		return plusBuds; // about 85% of calls are cache hits
	}

	/**
	 * Returns a cached array of the indices in this Idx. The returned array is
	 * readonly to user. Mutation will cause "arbitrary consequences".
	 * <pre>
	 * GEM is now about 20% faster using toArrayCached verses MyIntQueue.
	 * PRE: GEM.findHints()	 1,358 ms (3.4%)  MyIntQueue
	 * PST: GEM.findHints()	 1,087 ms (2.8%)  toArrayCached
	 * </pre>
	 *
	 * @return Cached (readonly to you) array of indices in this Idx.
	 */
	public int[] indicesCached() {
		if ( (m0|m1) < 1L )
			return THE_EMPTY_INT_ARRAY;
		if ( m0!=arA || m1!=arB )
			return toArrayNew();
		return array;
	}

	/**
	 * Override toArrayNew to remember the source masks and cache the array.
	 * <p>
	 * I override toArrayNew so that no-matter how it gets called the array is
	 * cached for any subsequent calls. Users of IdxPlusBuds call the bespoke
	 * (____ off Kevin) toArrayCached, but any Idx references (supertype) may
	 * still call toArrayNew, in which case the array still gets cached.
	 *
	 * @return a new cached (readonly to you) array of indices in this Idx.
	 */
	@Override
	public int[] toArrayNew() {
		arA=m0; arB=m1;
		return array = super.toArrayNew();
	}

}
