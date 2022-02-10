/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Cells.CALease;
import static diuf.sudoku.Cells.caLease;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.MAX_EMPTIES;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxI;
import diuf.sudoku.IdxL;
import diuf.sudoku.IntArrays.IALease;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import diuf.sudoku.utils.ArraySet;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.Permutations;
import java.util.LinkedHashSet;
import java.util.Set;
import static diuf.sudoku.IntArrays.iaLease;
import javax.naming.OperationNotSupportedException;

/**
 * AlsFinder finds Almost Locked Sets. This code was split-out of AAlsHinter
 * because it just kept getting more-and-more complex.
 * <p>
 * NOTE: AlsFinderRecursive extends AlsFinder to override getAlss, and use my
 * protected stuff.
 * <p>
 * NOTE: Likewise, AlsFinderPlain extends AlsFinder to override getAlss.
 * <pre>
 * KRC 2021-10-27 @stretch AlsFinder impl not used, so maybe should merge this
 * back in with AlsFinderPlain, except this setup gives us the flexibility for
 * other implementation, such as AlsFinderRecursive, which I'm loath to remove
 * now that it exists.
 * </pre>
 *
 * @author Keith Corlett 2021-06-04
 */
class AlsFinder {
//	// I'm not a hinter, so there's no point implementing IReporter.
//	// Instead AAlsHinter implements IReporter and calls me.
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(getClass().getName()+": COUNTS="+java.util.Arrays.toString(COUNTS)+" computFieldsTook="+Als.cfTook+" cmnBudsNewTook="+Idx.cbnTook);
//	}
//public final long[] COUNTS = new long[3];
///*
//KRC 2022-01-13 08:45
//                    alss          rccs
//BigWings : 2,383,096,800             0
//ALS_XZ   : 1,646,102,500 7,395,405,800
//ALS_Wing :       436,700       309,900
//ALS_Chain:       274,500 9,195,447,300
//                      Permute  computeFields    filterNSs
//AlsFinderPlain: 1,711,977,600, 2,312,422,100, 370,460,000
//Inline Idx ops in Als.computFields. Bees dick faster (150ms).
//These are with timings, so they're MUCH slower!
//AlsFinderPlain: 2,036,925,900, 4,099,530,600, 425,730,500
//computFieldsTook               3,919,568,100
//cmnBudsNewTook                 2,378,024,600
//*/


//	// set in AlsFinderRecursive only, reported in BigWings
//	protected long took;

	private final AlsSet alsSet = new AlsSet();
	private final Idx tmp1 = new Idx();
	private final Idx tmp2 = new Idx();
	// package visible for use in the other AlsFinders and AAlsHinter
	final Idx[] nakedSetIdxs = new Idx[NUM_REGIONS];

	AlsFinder() {
		for ( int i=0; i<NUM_REGIONS; ++i ) {
			nakedSetIdxs[i] = new Idx();
		}
	}

	// find all distinct Almost Locked Sets in this grid
	int getAlss(final Grid grid, final Als[] alss, final boolean allowNakedSets) {
		// KRC 2021-10-27 AlsFinderTest is the ONLY use of this implementation,
		// everything else now uses a subtype
		if ( !Debug.isClassNameInTheCallStack(5, "AlsFinderTest") ) {
			throw new UnsupportedOperationException();
		}
		// find Alss
		if ( !allowNakedSets && setNakedSetIdxs(grid) ) {
			findAlssNoNakedSets(grid);
		} else {
			findAlss(grid);
		}
		// copy and computeFields
		int cnt = 0;
		for ( Als als : alsSet ) {
//			System.out.println(""+cnt+TAB+als);
			alss[cnt] = als.computeFields(grid, cnt);
			++cnt;
		}
		// clear the Set, for next-time
		alsSet.clear();
		return cnt;
	}

	// populate nakedSetIdxs: an Idx per region. Each Idx contains indices of
	// cells in any Naked Set in this region. This is necessary coz an actual
	// NakedSet in an AlmostLockedSet breaks DeathBlossom (et al?).
	// @return are there any NakedSets in this grid?
	final boolean setNakedSetIdxs(final Grid grid) {
		Idx nakedSets; // indices of cells in nakedSet/s in this region
		int[] ia; // indices array: indices of ec (for speed)
		int[] ma; // maybes array: maybes of ec (for speed)
		int n // number of empty cells in this region
		  , s // number of sized cells
		  ; // this cannot be a Java method: it has no i.
		// the regions in the grid
		final ARegion[] regions = grid.regions;
		// presume there are no naked sets
		boolean result = false;
		try ( CALease eLease = caLease(REGION_SIZE);
			  CALease sLease = caLease(REGION_SIZE) ) {
			// the empty cells in this region
			final Cell[] empties = eLease.array;
			// empties having size <= currentNakedSetSize
			final Cell[] sized = sLease.array;
			// foreach region with atleast 2 empty cells
			for ( int r=0; r<NUM_REGIONS; ++r ) {
				nakedSets = nakedSetIdxs[r].clear();
				// a naked pair needs three empty cells in region
				// NOTE: each size is done seperately just to put all the |'s
				// in one line, coz it's a bit faster that way. sigh.
				// Naked Pairs
				if ( (n=regions[r].emptyCells(empties)) > 2 ) {
					if ( (s=Cells.sized(3, empties, n, sized)) > 1 ) {
						try ( IALease iaLease = iaLease(n);
							  IALease maLease = iaLease(n);
							  IALease paLease = iaLease(2); ) {
							Cells.indices(empties, s, ia=iaLease.array);
							Cells.maybes(empties, s, ma=maLease.array);
							for ( int[] pa : new Permutations(s, paLease.array) ) {
								if ( VSIZE[ma[pa[0]]|ma[pa[1]]] == 2 ) {
									nakedSets.add(ia[pa[0]]);
									nakedSets.add(ia[pa[1]]);
									result = true;
								}
							}
						}
					}
					// Naked Triples
					if ( n > 3 ) {
						if ( (s=Cells.sized(4, empties, n, sized)) > 2 ) {
							try ( IALease iaLease = iaLease(n);
								  IALease maLease = iaLease(n);
								  IALease paLease = iaLease(3); ) {
								Cells.indices(empties, s, ia=iaLease.array);
								Cells.maybes(empties, s, ma=maLease.array);
								for ( int[] pa : new Permutations(s, paLease.array) ) {
									// do the first three in one line
									if ( VSIZE[ma[pa[0]]|ma[pa[1]]|ma[pa[2]]] == 3 ) {
										nakedSets.add(ia[pa[0]]);
										nakedSets.add(ia[pa[1]]);
										nakedSets.add(ia[pa[2]]);
										result = true;
									}
								}
							}
						}
						// Naked Quads
						if ( n > 4
						  && (s=Cells.sized(5, empties, n, sized)) > 3 ) {
							try ( IALease iaLease = iaLease(n);
								  IALease maLease = iaLease(n);
								  IALease paLease = iaLease(4); ) {
								Cells.indices(empties, s, ia=iaLease.array);
								Cells.maybes(empties, s, ma=maLease.array);
								for ( int[] pa : new Permutations(s, paLease.array) ) {
									// do the first three in one line
									if ( VSIZE[ma[pa[0]]|ma[pa[1]]|ma[pa[2]]|ma[pa[3]]] == 4 ) {
										nakedSets.add(ia[pa[0]]);
										nakedSets.add(ia[pa[1]]);
										nakedSets.add(ia[pa[2]]);
										nakedSets.add(ia[pa[3]]);
										result = true;
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	// find Almost Locked Sets ignoring cells in Naked Sets
	private void findAlssNoNakedSets(final Grid grid) {
		int[] ma; // maybes of cells in region having size 2..d+1
		int d // degree
		  , n // number of cells in region having size 2..d+1
		  , m // aggregated maybes of this comination of cells
		  , i; // if you don't know what an i is by now you need shootin
		final Cell[] gridCells = grid.cells;
		final Idx empties = grid.getEmpties();
		try ( CALease caLease = caLease(MAX_EMPTIES) ) {
			final Cell[] ca = caLease.array; // 64 = 81 - 17
			for ( ARegion r : grid.regions ) { // 27
				if ( tmp1.setAndMany(r.idx, empties)
				  && tmp2.setAndNotAny(tmp1, nakedSetIdxs[r.index])
				  && tmp2.size() > 2 // we need 3 or more cells to form an ALS
				) {
					for ( d=2; d<REGION_SIZE; ++d ) { // degree: number of cells
						// must be final and therefore local for the lambda
						final int e=d+1, f=e+1; // number of cands, + 1
						if ( (n=tmp2.where(gridCells, ca, (c)->c.size < f)) > d ) {
							try ( IALease mLease = iaLease(n);
								  IALease pLease = iaLease(d); ) {
								Cells.maybes(ca, n, ma=mLease.array);
								for ( int[] perm : new Permutations(n, pLease.array) )
									if ( VSIZE[m=ma[perm[0]]|ma[perm[1]]] < f ) {
										for ( i=2; i<d; ++i )
											m |= ma[perm[i]];
										if ( VSIZE[m] == e )
											alsSet.add(new Als(IdxI.of(ca, perm), m, r));
									}
							}
						}
					}
				}
			}
		}
	}

	// find Almost Locked Sets (including cells in Naked Sets)
	private void findAlss(final Grid grid) {
		// ####################################
		// ## This method is no longer used! ##
		// ####################################
		int[] ma; // maybes of cells in region having size 2..d+1
		int d // degree
		  , n // number of cells in region having size 2..d+1
		  , m // aggregated maybes of this comination of cells
		  , i; // if you don't know what an i is by now you need shootin
		final Cell[] gridCells = grid.cells;
		final Idx empties = grid.getEmpties();
		// find ALS's of max 6 cells (beyond three or four are less useful)
		final int ALS_SIZE_CEILING = REGION_SIZE - 1;
		try ( final CALease caLease = caLease(MAX_EMPTIES) ) {
			final Cell[] ca = caLease.array;
			for ( ARegion r : grid.regions ) { // 27
				if ( tmp1.setAndMany(r.idx, empties) ) {
					for ( d=2; d<ALS_SIZE_CEILING; ++d ) { // degree: num cells
						final int e=d+1, f=d+2; // number of cands, plus one
						if ( (n=tmp1.where(gridCells, ca, (c)->c.size<f)) > d) {
							try ( IALease maLease = iaLease(n);
								  IALease paLease = iaLease(d); ) {
								Cells.maybes(ca, n, ma=maLease.array);
								for ( int[] perm : new Permutations(n, paLease.array) ) {
									if ( VSIZE[m=ma[perm[0]]|ma[perm[1]]] < f ) {
										for ( i=2; i<d; ++i )
											m |= ma[perm[i]];
										if ( VSIZE[m] == e )
											alsSet.add(new Als(IdxI.of(ca, perm), m, r));
									}
								}
							}
						}
					}
				}
			}
		}
		if ( !Debug.isClassNameInTheCallStack(6, "AlsFinderTest") )
			throw new UnsupportedOperationException("This method is no longer used!");
	}

	// An AlsSet is a LinkedHashSet whose add method is an addOnly, ie ignores
	// duplicate ALS's, where duplicate means same cells in the same region.
	// nb: I must extend HashSet for it's faster (than Collection) contains.
	//     I actually extend LinkedHashSet for it's natural order iteration
	protected static class AlsSet extends LinkedHashSet<Als> implements Set<Als> {
		private static final long serialVersionUID = 159335969601498L;
		AlsSet() {
			super(MAX_ALSS, 1F);
		}
		@Override
		public boolean add(final Als als) {
			return !contains(als) && super.add(als);
		}
		// for AlsSetPlain
		protected final boolean justAdd(final Als als) {
			return super.add(als);
		}
	}

	// AlsSetNoNakedSets add method ignores ALS's which contain a cell that is
	// in any NakedSet in this region. You call nakedSetIdxs(grid) to init the
	// nakedSetIdxs array before you start adding ALS's to this set.
	protected final class AlsSetNoNakedSets extends AlsSet {
		private static final long serialVersionUID = 346983766784L;
		@Override
		public boolean add(final Als als) {
			return als.idx.disjunct(nakedSetIdxs[als.region.index])
				&& super.add(als);
		}
	}

	// deactivate AlsSet's custom add for AlsFinderPlain,
	// so an AlsSet is now just an LinkedHashSet<Als>,
	// whose add method does nothing fancy, for speed
	protected static final class AlsSetPlain extends AlsSet {
		private static final long serialVersionUID = 346983766395L;
		@Override
		public boolean add(final Als als) {
			return super.justAdd(als);
		}
	}

/*
This one busts CAPACITY = 128
5..4..8......9..1...2..1..56..3..4...5..741....4.....83..6..7...6..4..8...8..2..1
,13,13679,,26,367,,23679,23679,478,3478,367,25,,3567,23,,2347,479,349,,78,368,,369,34679,,,278,179,,15,589,,2579,279,28,,39,289,,,,2369,269,17,2379,,1259,256,569,2359,23579,,,12,159,,158,89,,2459,249,129,,579,159,,357,2359,,239,479,479,,579,35,,3569,3569,
*/

	// AlsArraySet is a Set<Als> with a "plain" add method (no contains check).
	// It's used only in AlsFinderPlain, as a replacement for AlsSetPlain.
	// The diuf.sudoku.utils.ArraySet class is genericised version of what was
	// this class (which was specific to Als's), but there's zero motivation to
	// not genericise it, so I've done so.
	protected static final class AlsArraySet extends ArraySet<Als> {
		public static final int MY_CAPACITY = MAX_ALSS>>2; // quarter: 128
		public static final int MY_MAX_CAPACITY = MAX_ALSS>>1; // half: 256
		AlsArraySet() {
			super(MY_CAPACITY, MY_MAX_CAPACITY);
		}
	}

}
