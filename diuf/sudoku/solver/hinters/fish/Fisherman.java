/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
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
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IFoxyHinter;
import java.util.Arrays;

/**
 * (Basic)Fisherman implements {@link Tech#Swampfish}, {@link Tech#Swordfish},
 * and {@link Tech#Jellyfish} Sudoku solving techniques. These are the three
 * "degrees" of basic fish: Swamp=2, Sword=3, Jelly=4.
 * <p>
 * Swampfish is my name for what is commonly known as an X-Wing.
 * Fish should have fish-names, not wing-names. Sigh.
 * Recall Yoda raising Luke's X-Wing from a Tatooen swamp.
 * <p>
 * Example Swampfish: There are two rows with the same two places (cols) for 7;
 * hence 7 cannot be in any other place in those two cols, else one of the rows
 * would have no 7.
 * <p>
 * Fisherman finds all "basic" Fish; with no fins (see ComplexFisherman).
 * If you want to know what a fish is please have a look at a hint. Fishing
 * sounds harder than it is, because Im not great at explaining stuff, and my
 * code is heavily obfuscated by performance constraints (fastardisation). It
 * could be simpler, but slower. His name was Earnie, and he drove the fastest
 * milkcart in the West. Id rather buy a Ford myself. Complex earwax removal
 * network. Tims burned his Levis again. If I could, I would. Sigh.
 * <p>
 * A basic fish has N (degree) regions (bases) which together have N places for
 * the Fish Candidate Value (v), hence v is locked into the intersections of
 * bases and the cross regions (covers) eliminating all other places from the
 * covers. "Basic" means "not complex", ie no "fins" (extra vs in the bases).
 * <p>
 * Bases are rows, and covers are cols; and then<br>
 * bases are cols, and covers are rows.
 * <p>
 * This implementation uses hobiwans iterable-stack technique to search all
 * possible combinations of bases, because its faster than Permutations.next,
 * but its harder to follow. The stack is decomposed into two parallel arrays:
 * indexes and veezes, to avoid repeated double-dereferencing, which is slower
 * than Permutations.next.
 * <pre>
 * KRC 2021-08-11 faster BasicFisherman by caching region-indexes-bits in an
 * array. Repeated double-dereferencing of two complex structures ate all speed
 * gain from hobiwans iterable-stack approach. This is Surprising!
 * OLD BasicFisherman using Permutations:
 * 56,040,464	  17204	         3,257	    607	        92,323	Swampfish
 * 84,713,889	  14690	         5,766	    226	       374,840	Swordfish
 * NEW BasicFisherman with stack split into parallel arrays: indexes and veezes
 * 47,147,634	  17204	         2,740	    607	        77,673	Swampfish
 * 57,888,900	  14690	         3,940	    226	       256,145	Swordfish
 * 6.30.161 2021-08-11 11:14:41 BasicFisherman1 now faster than BasicFisherman.
 * So after this release BasicFisherman1 replaces BasicFisherman.
 *
 * KRC 2021-12-05 Ive tried every bloody thing I can think of trying to make
 * this faster, and if it can be done then Ill be buggered if I know how.
 *
 * KRC 2021-12-11 Exploded Idx ops.
 * 45,869,900	  17387	         2,638	    653	        70,244	Swampfish
 * 52,174,000	  13987	         3,730	    215	       242,669	Swordfish
 * These are solve calls only, but BruteForce also uses Swampfish, so a faster
 * Swampfish disproportionately reduces overall runtimes. BasicFisherman is
 * ~2.7% faster over ~1% more calls, so Ill be buggered!
 *
 * KRC 2021-12-11 09:36 No search method, for ONE stackframe per findHints.
 * 46,638,300	  17387	         2,682	    653	        71,421	Swampfish
 * 53,738,700	  13987	         3,842	    215	       249,947	Swordfish
 * But its slower, but I like it, so retain anyway, so fastardise loop:
 * 47,083,100	  17387	         2,707	    653	        72,102	Swampfish
 * 53,506,900	  13987	         3,825	    215	       248,869	Swordfish
 * slower again, but 2 seconds faster overall is a WTF. Let it bed in. Sigh.
 *
 * KRC 2023-11-24 Fisherman Still roughly on song, in time per call.
 * 44,563,600     17534          2,541      457         97,513  Swampfish
 * 73,785,700     15351          4,806      229        322,208  Swordfish
 * </pre>
 *
 * @author Keith Corlett 2021-05-01
 */
public class Fisherman extends AHinter implements IFoxyHinter
//		, diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": "+java.util.Arrays.toString(COUNTS));
//	}
//	final long[] COUNTS = new long[24];

	/**
	 * Constructor.
	 *
	 * @param tech to implement: {@link Tech#Swampfish},
	 *  {@link Tech#Swordfish}, or {@link Tech#Jellyfish}.
	 */
	public Fisherman(final Tech tech) {
		super(tech);
		// BasicFisherman implements the basic Fish techs (no complex ones)
		assert tech == Tech.Swampfish
			|| tech == Tech.Swordfish
			|| tech == Tech.Jellyfish;
	}

	/**
	 * Find all Swampfish/Swordfish/Jellyfish hints in $grid and add them to
	 * $accu.
	 * <p>
	 * Swampfish is used in BruteForce, so performance really matters here.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {		//    15,593
		// WARN: Swampfish is in BruteForce and DynamicPlus, so its HAMMERED!
		// ANSI-C style variables, for ONE stackframe, for speed.
		// All Idx operations are exploded, for speed.
		Idx idx; // a temporary Idx pointer
		ARegion[] possibleBases, covers;
		int a[] // INDEXES[veez]
		, v // the Fish candidate value
		, bL // baseLevel: 1-based index of current level in basesStack
		, pL // previousBaseLevel
		, i // the ubiquitious index (multiuse)
		, n // number of "eligible" bases (with 2..degree vs) (hijacked)
		, m // number of bases minus 1, just coz > is faster than >=
		, veez // saves repeated array-lookup of veezes[bL]
		;
		long bs0; int bs1; // basesVs exploded Idx
		long vc0; int vc1; // victims exploded Idx
		// bases contains possibleBases with 2..degree places for v.
		// Note that "indexes" are into this bases array (not possibleBases).
		final ARegion[] bases = new ARegion[REGION_SIZE];
		// the vs in the bases
		final int[] baseVeezes = new int[REGION_SIZE];
		// the other vs, or veezers, or maybe even visas. Square pizzas!
		final int[] veezes = new int[degreePlus1];
		// the indexes of bases, or something, not nothing. Indexes. Windexes?
		final int[] indexes = new int[degreePlus1];
		// presume that no hint will be found
		boolean result = false;
		// the first iteration does rows/cols
		// the second iteration does cols/rows
		final ARegion[][] ca = {grid.cols, grid.rows};
		final ARegion[][] ba = {grid.rows, grid.cols};
		// the ubiquitious index
		int index = 0;
		try {
			do {																//    31,182
				possibleBases = ba[index];
				covers = ca[index];
				// foreach fish candidate value
				v = 1;
				do {															//   280,632
					// bases := possibleBases with 2..degree places for v
					n = 0;
					for ( ARegion pb : possibleBases )
						if ( (i=pb.numPlaces[v])>1 && i<degreePlus1 )
							bases[n++] = pb;
					// we need atleast $degree bases to form a fish
					if ( n > degreeMinus1 ) {									//   249,002
						// m is the last valid base index, where n is how many.
						// m exists just because > is a bit faster than >=.
						m = n - 1;
						// avert repeated double-deref: build a flat array of
						// only the current vs in the current bases. This makes
						// hobiwan-stack as-fast-as Permuter, but without
						// hobiwan-stack is, suprisingly, slower.
						i=0; do baseVeezes[i] = bases[i].places[v]; while (++i < n); // 1,339,774
						// the first level is [1]. start at its first base=0.
						// indexes[0] is just a stopper, it is not used. This
						// is coz veezes[0] is used, and they are concurrent.
						pL = indexes[bL=1] = 0;
						// The PERM_LOOP examines each possible combination of
						// $degree bases among the $n bases. Keep going until
						// all levels of the stack are exhausted (a level is
						// exhausted when its index exceeds the last valid base
						// index). Build-up veezes from left to right, so that
						// each level in veezes contains indexes of vs in this
						// base and all bases to my left.
						// ====================================================
						// PERM_LOOP runs 11,380,464 times in top1465.
						// ====================================================
//<HAMMERED>
						PERM_LOOP: for(;;) {									// 6,612,986
							// while this level is exhausted: fallback a level
							while ( indexes[bL] > m ) {
								if ( --bL < 1 )
									break PERM_LOOP; // ie continue the v loop
								--pL;
							}
							// current vs = previous vs | this bases vs,
							// and then increment indexes[vL] for next time.
							veez = veezes[bL] = veezes[pL] | baseVeezes[indexes[bL]++];
							// if the fish is not yet complete
							if ( bL < degree ) {								// 6,134,548
								// if not already too many vs in bases
								// nb: veez is a bitset, eating distinctness,
								// which matters not coz base vs are distinct,
								// as are bases.
								if ( ISIZE[veez] < degreePlus1 ) {				// 3,180,275
									// move onto the next level in the stack,
									// starting with the base after the current
									// base (a forwards-only search).
									// the next previousLevel is currentLevel.
									pL = bL;
									// nb: indexes[pL] is already incremented.
									indexes[++bL] = indexes[pL];
								}
							// else its complete (degree bases) but is it fish?
							// nb: bL==degree (ie level never exceeds degree)
							} else if ( ISIZE[veez] == degree ) {				//    37,050
//</HAMMERED>
								// ============================================
								// this runs 217,292 times in top1465.
								// ============================================
								// its a fish, but does it eliminate anything?
								// victims: covers except bases that maybe v.
								// get indices of the vs in the bases.
								// nb: indexes[0] just a stopper; its NOT used!
								// nb: indexes pre-incremented, so -1 = current
								bs0 = (idx=bases[indexes[1]-1].idxs[v]).m0;
								bs1 = idx.m1;
								i = 2;
								do {											//   111,150
									bs0 |= (idx=bases[indexes[i]-1].idxs[v]).m0;
									bs1 |= idx.m1;
								} while (++i < degreePlus1);
								assert (bs0|bs1) > 0L; // NEVER!
								// get indices of the vs in the covers.
								// hijack n, which we are finished with.
								a = INDEXES[veez];
								n = a.length;
								vc0 = vc1 = i = 0;
								do {
									vc0 |= (idx=covers[a[i]].idxs[v]).m0;		//   148,200
									vc1 |= idx.m1;
								} while (++i < n);
								assert (vc0|vc1) > 0L; // NEVER!
								// if any victims: vs in covers but not bases
								if ( ( (vc0 &= ~bs0)
									 | (vc1 &= ~bs1) ) > 0L
								) {												//         4
									// FOUND BasicFish!
									result = true;
									if ( accu.add(new FishHint(grid, this, v
										, new Pots(vc0,vc1, VSHFT[v], DUMMY) // reds
										, new Pots(bs0,bs1, VSHFT[v], DUMMY) // greens
										, newBasesArray(bases, indexes)
										, newCoversArray(covers, a))) )
										return result;
								}
							}
						}
					}
				} while (++v < VALUE_CEILING);
			} while (++index < 2);
		} catch (UnsolvableException ex) {
			throw ex;
		} catch (Exception ex) { // especially NPE
			ex.printStackTrace(System.out); // Need this!
			throw new UnsolvableException(ex.toString());
		} finally {
			Arrays.fill(bases, null);
		}
		return result;
	}

	// read the bases at indexes into a new array
	private ARegion[] newBasesArray(final ARegion[] bases, final int[] indexes) {
		final ARegion[] result = new ARegion[degree];
		int cnt = 0;
		int i = 1;
		do
			result[cnt++] = bases[indexes[i]-1];
		while (++i < degreePlus1);
		assert cnt == degree;
		return result;
	}

	// read the covers at the given indexes into a new array
	private ARegion[] newCoversArray(final ARegion[] covers, final int[] indexes) {
		final ARegion[] result = new ARegion[degree];
		int cnt = 0;
		for ( int i : indexes )
			result[cnt++] = covers[i];
		assert cnt == degree;
		return result;
	}

}
