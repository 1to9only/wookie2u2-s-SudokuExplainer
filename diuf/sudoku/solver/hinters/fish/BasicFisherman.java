/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.Arrays;

/**
 * BasicFisherman implements the basic Fish Sudoku solving technique.
 * <p>
 * A basic fish has N (degree) regions (bases) which together have N places for
 * the Fish Candidate Value (v), so v is locked into the intersections of bases
 * and the cross regions (covers) eliminating all other places from the covers.
 * The "basic" means "not complex", ie no "fins" (extra v's in the bases).
 * <p>
 * Bases are rows, and covers are cols; and then<br>
 * bases are cols, and covers are rows.
 * <p>
 * This implementation uses hobiwans iterable-stack technique to search all
 * possible combinations of bases, because it's faster than Permutations.next,
 * but its harder to follow. The stack is decomposed into two parallel arrays:
 * indexes and veeses, to avoid repeated double-dereferencing, which is slower
 * than Permutations.next.
 * <pre>
 * KRC 2021-08-11 I'm faster than BasicFisherman by caching region-indexes-bits
 * in a flat array. Repeated double-dereferencing of two complex structures ate
 * all speed gains from hobiwans iterable-stack approach. This is Surprising!
 * old BasicFisherman using Permutations:
 * 56,040,464	  17204	         3,257	    607	        92,323	Swampfish
 * 84,713,889	  14690	         5,766	    226	       374,840	Swordfish
 * new BasicFisherman with stack split into parallel arrays: indexes and veeses
 * 47,147,634	  17204	         2,740	    607	        77,673	Swampfish
 * 57,888,900	  14690	         3,940	    226	       256,145	Swordfish
 * 6.30.161 2021-08-11 11:14:41 BasicFisherman1 now faster than BasicFisherman.
 * So after this release BasicFisherman1 replaces BasicFisherman.
 *
 * KRC 2021-12-05 I've tried every bloody thing I can think of trying to make
 * this faster, and if it can be done then I'll be buggered if I know how.
 *
 * KRC 2021-12-11 Exploded Idx ops.
 * 45,869,900	  17387	         2,638	    653	        70,244	Swampfish
 * 52,174,000	  13987	         3,730	    215	       242,669	Swordfish
 * These are solve calls only, but BruteForce also uses Swampfish, so a faster
 * Swampfish disproportionately reduces overall runtimes. BasicFisherman is
 * ~2.7% faster over ~1% more calls, so I'll be buggered!
 *
 * KRC 2021-12-11 09:36 Kill search method, for ONE stackframe per findHints.
 * 46,638,300	  17387	         2,682	    653	        71,421	Swampfish
 * 53,738,700	  13987	         3,842	    215	       249,947	Swordfish
 * But it's slower, but I like it, so retain anyway, so fastardise loop:
 * 47,083,100	  17387	         2,707	    653	        72,102	Swampfish
 * 53,506,900	  13987	         3,825	    215	       248,869	Swordfish
 * slower again, but 2 seconds faster overall is a WTF. It'll bed in. Sigh.
 * </pre>
 *
 * @author Keith Corlett 2021-05-01
 */
public class BasicFisherman extends AHinter {

	// storage for eligable bases
	private ARegion[] bases;

	/**
	 * indexes contains the index-in-bases of each base in the current Fish.
	 * <p>
	 * This array is half "the stack", concurrent with veeses. <br>
	 * [1] is the first base. [0] is just a stopper.
	 */
	private final int[] indexes = new int[degreePlus1];

	/**
	 * veeses contains an agglomerate of bitsets of regions-indexes 0..8, so
	 * each "level" 1..degree contains a bitset of distinct indexes of v's in
	 * this base and all bases to my left in "the stack", built-up from left to
	 * right, so when level==degree the Fish is complete and veeses[degree]
	 * contains the distinct regions-indexes of v's in all bases in this Fish.
	 * <p>
	 * This array is half "the stack", concurrent with indexes. <br>
	 * [1] is the first base. [0] is just a stopper.
	 */
	private final int[] veeses = new int[degreePlus1];

	// baseVees := bases[i].ridx[v].bits
	private final int[] baseVees = new int[REGION_SIZE];

	// Do I need to (re)create my bases array?
	private boolean reset = true;

	private static final Idx VICTIMS = new Idx();
	private static final Idx BASEVS = new Idx();

	/**
	 * Constructor.
	 * @param t the Tech to implement: Swampfish=2, Swordfish=3, Jellyfish=4.
	 */
	public BasicFisherman(final Tech t) {
		super(t);
		// BasicFisherman implements the basic Fish techs (no complex ones)
		assert t==Tech.Swampfish || t==Tech.Swordfish || t==Tech.Jellyfish;
	}

	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// ANSI-C style variables, for ONE stackframe, for speed.
		// All Idx operations are exploded, for speed.
		Idx tmp; // a temporary Idx pointer
		int[] a; // INDEXES[vees]
		int v // the Fish candidate value
		  , l // current level in the stack
		  , i // the ubiquitious index (multiuse)
		  , n // number of "eligible" bases (with 2..degree v's)
			  // hijacked in the COMBO_LOOP as a.length
		  , m // number of bases minus 1, just coz > is faster than >=
		  , vees // saves repeated array-lookup of veeses[l]
		  , bv0,bv1,bv2 // an exploded basesVs Idx
		  , vt0=0,vt1=0,vt2=0 // an exploded victims Idx
		  ;
		// ====================================================================
		// Generate bug when this array is a final field. If we hit exception
		// it's cleared, but the next call doesn't reinitialise it, despite it
		// being in the code to do so, so it's null. I blame the optimiser. I
		// guess it skips setup on subsequent requests, ie optimiser is wrong.
		// But I just create my own array each call and the problem disappears
		// but it makes BasicFisherman about 20% slower. So I hijinx it.
		// ====================================================================
		if ( reset ) {
			this.bases = new ARegion[REGION_SIZE];
			reset = false;
		}
		// bases contains possibleBases with 2..degree places for v.
		// Note that "indexes" are into this bases array (not possibleBases).
		final ARegion[] bases = this.bases;
		// presume that no hint will be found
		boolean result = false;
		// the first iteration does rows/cols
		ARegion[] possibleBases = grid.rows
				, covers        = grid.cols;
		try {
			// for possibleBases/covers in rows/cols, cols/rows
			for(;;) { // bases/covers "loop"
				// foreach fish candidate value
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// bases := possibleBases with 2..degree places for v
					n = 0;
					for ( ARegion possibleBase : possibleBases )
						if ( (i=possibleBase.ridx[v].size)>1 && i<degreePlus1 )
							bases[n++] = possibleBase;
					// if there's sufficient bases to form a fish of my size
					if ( n > degreeMinus1 ) {
						// m is the last valid base index, where n is how many.
						// m exists just because > is faster than >=.
						m = n - 1;
						// save repeated double-dereferencing: get only the current v's
						// in the current bases, and stick them in a flat array. This
						// optimisation makes the iteration-stack about-as-fast-as the
						// Permutations alternative, but without it's suprisingly
						// slower. Who knows, maybe there's a Permutations tweak that
						// will put it ahead again. As fast as possible means dream-up
						// a challenge for a tired assumption, gather evidence, adapt,
						// and hope that you've just done real-good yourself. Ideally,
						// regions[i].ridx[v].bits would also be in Grid.places[v][i]
						// with size in Grid.spots[v][i], to globally avoid the common
						// performance pitfall that is double-dereferencing. I would
						// go right ahead and "fix" this, but it's just so much work.
						for ( i=0; i<n; ++i )
							baseVees[i] = bases[i].ridx[v].bits;
						// the first level is [1], and we start at it's first base, 0.
						// (indexes[0] is just a stopper, it's not actually used)
						indexes[l=1] = 0;
						// The COMBO_LOOP examines each possible combination of $degree
						// bases among the $n bases. Keep going until all levels of the
						// stack are exhausted (a level is exhausted when it's index
						// exceeds the last valid base index). Build-up veeses from
						// left to right, so that each level in veeses contains indexes
						// of v's in this base and all bases to my left.
//<HAMMERED>
						// ====================================================
						// COMBO_LOOP runs 11,380,464 times in top1465.
						// ====================================================
						COMBO_LOOP: for(;;) {
							// while this level is exhausted: fallback a level
							while ( indexes[l] > m )
								if ( --l < 1 )
									break COMBO_LOOP; // ie continue the v loop
							// current v's = previous v's | this bases v's,
							// and then increment indexes[l], for next time.
							vees = veeses[l] = veeses[l-1] | baseVees[indexes[l]++];
							// if the fish is not yet complete
							if ( l < degree ) {
								// if not already too many distinct v's in these bases
								if ( ISIZE[vees] < degreePlus1 )
									// move onto the next level in the stack, starting
									// with the base after current base (forward-only).
									// nb: indexes[l] is pre-incremented.
									indexes[++l] = indexes[l-1];
							// else it's complete, but is it a fish?
							} else if ( ISIZE[vees] == degree ) {
//</HAMMERED>
								// ============================================
								// This runs 217,292 times in top1465.
								// ============================================
								// it's a fish, but does it eliminate anything?
								// get indices of the v's in the bases
								// nb: indexes[*] are pre-incremented, so -1 is current
								bv0 = (tmp=bases[indexes[1]-1].idxs[v]).a0;
								bv1 = tmp.a1;
								bv2 = tmp.a2;
								for ( i=2; i<degreePlus1; ++i ) {
									bv0 |= (tmp=bases[indexes[i]-1].idxs[v]).a0;
									bv1 |= tmp.a1;
									bv2 |= tmp.a2;
								}
								if ( (bv0|bv1|bv2) == 0 )
									return result; // should NEVER happen
								// if this is happening then the array/s are
								// screwed, probably

								// get indices of the v's in the covers
								// WARN: I hijack n, which we're finished with anyway.
								for ( a=INDEXES[vees],n=a.length,i=0; i<n; ++i ) {
									vt0 |= (tmp=covers[a[i]].idxs[v]).a0;
									vt1 |= tmp.a1;
									vt2 |= tmp.a2;
								}
								// if any vs in covers except bases they're victims
								if ( ( (vt0 &= ~bv0)
									 | (vt1 &= ~bv1)
									 | (vt2 &= ~bv2) ) != 0 ) {
									// FOUND BasicFish!
									result = true;
									VICTIMS.set(vt0,vt1,vt2);
									assert VICTIMS.any();
									BASEVS.set(bv0,bv1,bv2);
									assert BASEVS.any();
									if ( accu.add(new BasicFishHint(this, v
											, new Pots(VICTIMS, grid, v) // reds
											, new Pots(BASEVS, grid, v) // greens
											, currentBases(bases)
											, currentCovers(covers, a))) )
										return result;
									vt0=vt1=vt2 = 0;
								}
							}
						}
					}
				}
				if ( possibleBases == grid.cols )
					break;
				// the second iteration does cols/rows
				possibleBases = grid.cols;
				covers        = grid.rows;
			}
		} catch (UnsolvableException ex) {
			reset = true;
			throw ex;
		} catch (Exception ex) {
			// presumably an NPE from bases being null
			reset = true;
			throw new UnsolvableException(ex.toString());
		} finally {
			Arrays.fill(bases, null);
		}
		return result;
	}

	// read the bases at indexes into a new array
	private ARegion[] currentBases(final ARegion[] bases) {
		final ARegion[] result = new ARegion[degree];
		int cnt = 0;
		for ( int i=1; i<degreePlus1; ++i )
			result[cnt++] = bases[indexes[i]-1];
		assert cnt == degree;
		return result;
	}

	// read the covers at the given indexes into a new array
	private ARegion[] currentCovers(final ARegion[] covers, final int[] indexes) {
		final ARegion[] result = new ARegion[degree];
		int cnt = 0;
		for ( int i : indexes )
			result[cnt++] = covers[i];
		assert cnt == degree;
		return result;
	}

}
