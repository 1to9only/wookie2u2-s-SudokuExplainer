package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxI;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import diuf.sudoku.utils.ArraySet;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.Permuter;

/**
 * AlsFinder is the simplest-possible AlsFinder, using a plain iterative
 * algorithm. Iterating Permutations is known slow, but its still faster
 * than its recursive alternative. BFIIK. Maybe my recur-impl was s__t?
 * <pre>
 * 2021-09-14 I am about a second faster than AlsFinderRecursive.
 * HIM 4,043,189,800 12568 321,705 2218 1,822,898 BigWings
 * ME  3,016,482,600 12550 240,357 2186 1,379,909 BigWings
 * 2021-09-28 Using the new AlsSetPlain2 which wraps a Als[] into a
 * {@code Set<Als>} of which I use just the add method, but it is internal
 *  array is public, as it is size, so instead of an Iterator I get the "raw"
 *  array, and array-iterator over it, which is a bit faster.
 * V02 3,265,100,400 12483 261,563 2158 1,513,021 BigWings BUGGER!
 * V03 3,337,909,700 12483 267,396 2158 1,546,760 BigWings DOUBLE BUGGER!
 * V04 3,129,754,500 12483 250,721 2158 1,450,303 BigWings STILL BUGGER!
 *
 * 2023-07-11 major surgery for speed. The maxAlsSize parameter is now used
 * properly, I think. The case statement executes once and the permutations
 * loops are repeated in it, maybes are no longer cached, the sized array is
 * now indices (was Cells); exhumed sized method from Grid.ARegion because
 * this was its only use, and clean now takes indices. A few seconds faster.
 * </pre>
 *
 * @author Keith Corlett 2021-09-14
 */
final class AlsFinder {

	// Repopulates $indices with cells in this region
	// where size in 2(inclusive)..ceiling(EXCLUSIVE)
	// and returns hows many.
	// nb: a method is slower, but its neat, so who really gives a ____.
	// I am static to reduce invocation overheads (this injection costs).
	// I wish each region had its own sizes array. Food for thought.
	//
	// @param region of source cells
	// @param ceiling the maximum size of wanted cells exclusive (ie + 1)
	// @param indices to repopulate
	// @return how many
	private static int sized(final ARegion region, final int ceiling, final int[] indices) {
		int count = 0;
		for ( Cell cell : region.cells ) {
			if ( cell.size>1 && cell.size<ceiling ) {
				indices[count++] = cell.indice;
			}
		}
		return count;
	}

	/**
	 * Get Almost Locked Sets returning how many.
	 * <pre>
	 * find all distinct Almost Locked Sets in $grid sized 2..$maxAlsSize,
	 * adding each to the $alss array, and return how many.
	 * </pre>
	 *
	 * @param grid to search
	 * @param alss to repopulate
	 * @param maxAlsSize the maximum size of an almost locked set: 7
	 * @return numAlss: the number of alss in the alss array
	 */
	int getAlss(final Grid grid, final Als[] alss, final int maxAlsSize) {
		// single stack-frame, for no extra stack-work
		AlsArraySet set;
		int i // ubiqituous index
		  , D // max degree: maximum number of cells in an ALS (NOT ceiling)
		  , d // degree: the number of cells in each ALS
		  , c // d - 1: floor for number of cells, for > instead of >=
		  , e // d + 1: number of maybes in each ALS of d cells
		  , f // d + 2: one more than e, for < instead of <=
		  , cands // combination of maybes of these cells
		  , n; // number of cells in region having size 2..degree+1
		// an array of AlsSets, one for each size 2..maxAlsSize.
		// NOTE: maxAlssSize capped at 7 coz thats as large as an ALS gets, in
		// practice. An empty region has 9 empty cells. All empty cells in a
		// region are by definition a locked set. 1 less than that is 8, but
		// that produces lots of ALSs with very low useful-rate. We want useful
		// ALSs as quickly as possible, so its absolutely capped at 7. If you
		// are anally-retentive then bump this and AAlsHinter.MAX_ALS_SIZE upto
		// 8. Be warned that all als hinters will take about twice as long to
		// under 20% more eliminations. Big ALSs are really slow. BFFIK.
		final int maxD = Math.min(maxAlsSize, 7);
		final AlsArraySet[] sets = new AlsArraySet[maxD+1];
		for ( i=2; i<=maxD; ++i ) {
			sets[i] = new AlsArraySet(); // no duplicate filter
		}
		// the permutations arrays, once, instead of per region
		final int[][] permutationsArrays = new int[REGION_SIZE][];
		for ( i=2; i<REGION_SIZE; ++i ) {
			permutationsArrays[i] = new int[i];
		}
		// distinct combinations of $d cells in the $n empty cells in region
		final Permuter permuter = new Permuter();
		// sets are found by size in 2..degree-1
		final int[] s = new int[REGION_SIZE];
		// a bitset of the potential values of each cell in the grid by indice
		final int[] m = grid.maybes;
		// the grid.idxs
		final Idx[] idxs = grid.idxs;
		// my return value: the number of ALSs found
		int numAlss = 0;
		try {
			// foreach region in the grid (boxs, rows, cols)
			for ( ARegion region : grid.regions ) {
				// D is ceiling for num cells in each ALS, capped at REGION_SIZE-1.
				// A ceiling because all empty cells in a region are a LockedSet.
				D = Math.min(region.emptyCellCount, maxD);
				// foreach degree: the number of cells in each ALS
				// c = floor for number of sized cells (d - 1)
				// d = the current degree
				// e = number of maybes in an ALS (d + 1)
				// f = d + 2 just for < instead of <=
				for ( c=1,d=2,e=3,f=4; d<=D; c=d,d=e,e=f++ ) { // d<=D
					// the set of alss for this degree
					set = sets[d];
					// get region.cells with size in 2..d+1 inclusive, because an
					// ALS of d cells cant contain a cell with more than d+1 maybes
					if ( (n=sized(region, f, s)) > c ) {
						// get the dee-array: permutations array = int[d]
						final int[] da = permutationsArrays[d];
						// a permutation of d cells sharing e maybes is an ALS.
						// nb: switch once per degree (not per permutation) for
						// efficiency: to read each permutation in ONE single
						// statement, instead of iterating the elements, which
						// is much slower. This lead me to Naked/Hidden Pair/
						// Triple/Quad. It took me ages to work this out, so I
						// belabour the point somewhat. All because Java has no
						// macros. Flexibility at the cost of speed. Perfect if
						// your main game is high-performance hardware. Sigh.
						switch ( d ) {
						case 2:
							for ( int[] p : permuter.permute(n, da) ) {
								if ( VSIZE[cands=m[s[p[0]]] | m[s[p[1]]]] == e ) {
									set.justAdd(new Als(idxs, IdxI.of(s, p, d), cands, region.index));
								}
							}
							break;
						case 3:
							for ( int[] p : permuter.permute(n, da) ) {
								if ( VSIZE[cands=m[s[p[0]]] | m[s[p[1]]] | m[s[p[2]]]] == e ) {
									set.justAdd(new Als(idxs, IdxI.of(s, p, d), cands, region.index));
								}
							}
							break;
						case 4:
							for ( int[] p : permuter.permute(n, da) ) {
								if ( VSIZE[cands=m[s[p[0]]] | m[s[p[1]]] | m[s[p[2]]] | m[s[p[3]]]] == e ) {
									set.justAdd(new Als(idxs, IdxI.of(s, p, d), cands, region.index));
								}
							}
							break;
						case 5:
							for ( int[] p : permuter.permute(n, da) ) {
								if ( VSIZE[cands=m[s[p[0]]] | m[s[p[1]]] | m[s[p[2]]] | m[s[p[3]]] | m[s[p[4]]]] == e ) {
									set.justAdd(new Als(idxs, IdxI.of(s, p, d), cands, region.index));
								}
							}
							break;
						case 6:
							for ( int[] p : permuter.permute(n, da) ) {
								if ( VSIZE[cands=m[s[p[0]]] | m[s[p[1]]] | m[s[p[2]]] | m[s[p[3]]] | m[s[p[4]]] | m[s[p[5]]]] == e ) {
									set.justAdd(new Als(idxs, IdxI.of(s, p, d), cands, region.index));
								}
							}
							break;
						case 7:
							for ( int[] p : permuter.permute(n, da) ) {
								if ( VSIZE[cands=m[s[p[0]]] | m[s[p[1]]] | m[s[p[2]]] | m[s[p[3]]] | m[s[p[4]]] | m[s[p[5]]] | m[s[p[6]]]] == e ) {
									set.justAdd(new Als(idxs, IdxI.of(s, p, d), cands, region.index));
								}
							}
							break;
						}
					}
				}
			}
		} catch ( ArrayIndexOutOfBoundsException ex ) {
			Log.teeln("WARN: "+Log.me()+": "+ex);
		}
		// move sets to one alss array
		try {
			for ( d=2; d<=maxD; ++d ) {
				final Object[] a = (set=sets[d]).array;
				for ( i=0,n=set.size; i<n; ++i ) {
					alss[numAlss++] = (Als)a[i];
					a[i] = null;
				}
				set.size = 0;
			}
		} catch ( ArrayIndexOutOfBoundsException ex ) {
			Log.teeln("WARN: "+Log.me()+": MAX_ALSS exceeded: "+ex);
			cleanUp(sets, maxD);
		}
		// clean-up: a cell reference holds the whole grid in memory
		return numAlss;
	}

	// clear all sets
	private void cleanUp(final AlsArraySet[] sets, final int maxD) {
		int j, J;
		for ( int d=0; d<=maxD; ++d ) {
			AlsArraySet set = sets[d];
			Object[] a = set.array;
			for ( j=0,J=set.size; j<J; ++j ) {
				a[j] = null;
			}
			set.size = 0;
		}
	}

	// AlsArraySet is a Set<Als> with a plain justAdd method (no contains
	// check, no growth, no null check).
	// The diuf.sudoku.utils.ArraySet class is a genericised version of what
	// was this class, except I was Als specific.
	protected static final class AlsArraySet extends ArraySet<Als> {
		AlsArraySet() {
			// NOTE: 1024 (MAX_ALSS) is ArraySets notional max MAX_CAPACITY,
			// otherwise need a fast indexOf, which is O(n) nonperformant.
			super(MAX_ALSS, MAX_ALSS);
		}
	}

}
