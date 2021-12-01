/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Indexes.ISIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BasicFisherman implements the basic Fish Sudoku solving technique.
 * <p>
 * A basic fish has N (degree) regions (bases) which together have N places for
 * the Fish Candidate Value (v), so v is locked into the intersections of bases
 * and covers (the cross regions) eliminating all other places from the covers.
 * The "basic" means "not complex", ie no "fins" (extra v's in the bases).
 * <p>
 * Bases are rows, and covers are cols; and then<br>
 * bases are cols, and covers are rows.
 * <p>
 * This implementation uses hobiwans iterable-stack approach to search all
 * possible combinations of bases, coz its faster than Permutations.next (a bit
 * slow). A stack is faster, but its harder to understand. Further complicating
 * things, the stack is decomposed into two parallel arrays: indexes and vs, to
 * avoid repeatedly double-dereferencing structs, which is even slower than the
 * Permutations.next method, which is so slow we're trying to avoid it. sigh.
 * <pre>
 * KRC 2021-08-11 I'm faster than BasicFisherman by caching region-indexes-bits
 * in a flat array. Repeated double-dereferencing of two complex structures ate
 * all speed gains from hobiwans iterable-stack approach. This is Surprising!
 * old BasicFisherman (using Permutations):
 * 56,040,464	  17204	         3,257	    607	        92,323	Swampfish
 * 84,713,889	  14690	         5,766	    226	       374,840	Swordfish
 * new BasicFisherman with stack split into parallel arrays: indexes and vs
 * 47,147,634	  17204	         2,740	    607	        77,673	Swampfish
 * 57,888,900	  14690	         3,940	    226	       256,145	Swordfish
 * 6.30.161 2021-08-11 11:14:41 BasicFisherman1 now faster than BasicFisherman.
 * So after this release the old BasicFisherman class was removed, and the new
 * BasicFisherman1 was renamed BasicFisherman (as was always the plan).
 * </pre>
 *
 * @author Keith Corlett 2021-05-01
 */
public class BasicFisherman extends AHinter {

	/**
	 * This array contains those bases having 2..degree places for v.
	 * Note that "indexes" are into this bases array (not possibleBases).
	 */
	private final ARegion[] bases = new ARegion[REGION_SIZE];

	/**
	 * indexes contains the index-in-bases of each base in the current Fish.
	 * <p>
	 * This array is half "the stack", concurrent with vs.
	 * [1] is the first base. [0] is just a stopper.
	 */
	private final int[] indexes;

	/**
	 * vs contains an agglomerate of bitsets of regions-indexes 0..8, such that
	 * each "level" 1..degree contains a bitset of distinct indexes of v's in
	 * this base and all bases to my left in "the stack", built-up from left to
	 * right, so that when level==degree the Fish is complete, and vs[degree]
	 * contains the distinct regions-indexes of v's in all bases in this Fish,
	 * which we search for eliminations: any/all v's in covers and not bases.
	 * <p>
	 * This array is half "the stack", concurrent with indexes.
	 * [1] is the first base. [0] is just a stopper.
	 */
	private final int[] vs;

	/**
	 * Constructor.
	 * @param t the Tech to implement: Swampfish=2, Swordfish=3, Jellyfish=4.
	 */
	public BasicFisherman(Tech t) {
		super(t);
		// BasicFisherman implements the basic Fish techs (no complex ones)
		assert t==Tech.Swampfish || t==Tech.Swordfish || t==Tech.Jellyfish;
		// the working stack entries are index 1..degree; 0 is just a stopper.
		// the stack is split into two parallel arrays: indexes and vs, to save
		// repeatedly deferencing the struct, for speed.
		indexes = new int[degreePlus1];
		vs = new int[degreePlus1];
	}

	// the grid to search
	private Grid grid;
	// the IAccumulator to which I add hints
	private IAccumulator accu;

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		this.grid = grid;
		this.accu = accu;
		boolean result = false;
		try {
			if ( accu.isSingle() ) {
				if ( search(grid.rows, grid.cols)
				  || search(grid.cols, grid.rows) )
					return true;
			} else {
				result |= search(grid.rows, grid.cols)
						| search(grid.cols, grid.rows);
			}
		} finally {
			this.grid = null;
			this.accu = null;
			Arrays.fill(this.bases, null);
		}
		return result;
	}

	/**
	 * Search each combination in N possibleBases having 2..degree v's, for a
	 * Fish of size degree. Create a hint for each Fish and add it to the accu.
	 * If the accu says "stop now" then I stop, to produce one hint; returning
	 * were any hints (ie Fish) found?
	 *
	 * @param possibleBases grid.rows/cols
	 * @param covers grid.cols/rows
	 * @return were any hints found?
	 */
	private boolean search(ARegion[] possibleBases, ARegion[] covers) {
		int v // the Fish candidate value
		  , l // current level in the stack
		  , i // the ubiquitious index
			  // hijacked as cardinality: number of v's in this base
		  , n // number of "eligible" bases (with 2..degree v's)
		  , m // number of bases minus 1, just coz > is faster than >=
		  , vees; // saves repeated array-lookup of vs[l]
		// presume that no hint will be found
		boolean result = false;
		// foreach possible fish candidate value
		for ( v=1; v<VALUE_CEILING; ++v ) {
			// bases array := possibleBases having 2..degree places for v
			n = 0;
			for ( ARegion base : possibleBases ) {
				// hijack i as cardinality: number of v's in this possibleBase
				if ( (i=base.ridx[v].size)>1 && i<degreePlus1 ) {
					bases[n++] = base;
				}
			}
			// if there's sufficient bases to form a fish of my size
			if ( n > degreeMinus1 ) {
				// m is the last valid base index, whereas n is how many.
				// The m variable exists just because > is faster than >=.
				m = n - 1;
				// save repeated double-dereferencing: get only the current v's
				// in the current bases, and stick them in a flat array. This
				// simple change makes BasicFisherman1 about-as-fast-as the old
				// BasicFisherman. Without this optimisation it's slower. sigh.
				for ( i=0; i<n; ++i ) {
					basesVs[i] = bases[i].ridx[v].bits;
				}
				// the first level is [1], and we start at it's first base, 0.
				indexes[l=1] = 0;
				// keep going until all levels of the stack are exhausted.
				// We build-up "the vs stack" from left-to-right, so that each
				// level in the stack contains indexes of v's in this base and
				// all bases to my left.
//<HAMMERED>
				// ============================================================
				// LOOP runs billions of times, so KISS it hard.
				// ============================================================
				LOOP: for(;;) {
					// while this level is exhausted: fallback a level
					while ( indexes[l] > m ) {
						if ( --l < 1 ) {
							break LOOP; // done
						}
					}
					// current v's = previous v's + this bases v's
					// vees just saves repeated array-lookup of vs[l].
					vees = vs[l] = vs[l-1] | basesVs[indexes[l]++];
					// if the fish is not yet complete
					if ( l < degree ) {
						// if not already too many distinct v's in these bases
						if ( ISIZE[vees] < degreePlus1 ) {
							// move onto the next level in the stack, starting
							// with the base after current base (forward-only).
							indexes[++l] = indexes[l-1];
						}
					// else it's complete, but is it a fish?
					} else if ( ISIZE[vees] == degree ) {
//</HAMMERED>
						// it's a fish, but does it eliminate anything?
						// get indices of the v's in the bases
						baseVs.set(bases[indexes[1]-1].idxs[v]);
						for ( i=2; i<degreePlus1; ++i ) {
							baseVs.or(bases[indexes[i]-1].idxs[v]);
						}
						// get indices of the v's in the covers
						coverVs.clear();
						for ( i=0; i<REGION_SIZE; ++i ) {
							if ( (vees & ISHFT[i]) != 0 ) {
								coverVs.or(covers[i].idxs[v]);
							}
						}
						// if there are any v's in covers and not bases
						// then any remaining coverVs are our victims
						if ( coverVs.andNot(baseVs).any() ) {
							// FOUND a Fish, with eliminations!
							final Pots reds = new Pots();
							reds.upsertAll(coverVs, grid, v);
							final AHint hint = createHint(v, reds
									, Regions.list(degree, covers, vees));
							result = true;
							if ( accu.add(hint) ) {
								return result; // exit-early
							}
						}
					}
				}
			}
		}
		return result;
	}
	// dont repeatedly double-dereference bases[i].ridx[v].bits, for speed
	private final int[] basesVs = new int[9];
	// indices of v's in the base regions
	private final Idx baseVs = new Idx();
	// indices of v's in the cover regions
	private final Idx coverVs = new Idx();

	/**
	 * Create and return a new BasicFishHint.
	 *
	 * @param v the Fish candidate value
	 * @param reds the removable Cell=>Values
	 * @param covers a List of cover regions in this fish
	 * @return a new BasicFishHint, always.
	 */
	private AHint createHint(final int v, final Pots reds
			, final List<ARegion> covers) {
		// get highlighted (green) potentials = the corners
		final Pots greens = new Pots();
		final int sv = VSHFT[v];
		for ( int i=1; i<degreePlus1; ++i ) {
			for ( Cell cc : bases[indexes[i]-1].cells ) {
				if ( (cc.maybes & VSHFT[v]) != 0 ) {
					greens.put(cc, sv);
				}
			}
		}
		// get a List of the base regions in this fish
		final List<ARegion> baseL = new ArrayList<>(degree);
		for ( int i=1; i<degreePlus1; ++i ) {
			baseL.add(bases[indexes[i]-1]);
		}
		// create and return the new hint
		return new BasicFishHint(this, reds, v, greens, "", baseL, covers);
	}

}
