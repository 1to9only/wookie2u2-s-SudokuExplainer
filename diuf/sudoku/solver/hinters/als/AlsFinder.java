/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.MIN_CLUES;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import diuf.sudoku.utils.Permutations;
import java.util.LinkedHashSet;
import java.util.Set;
import static diuf.sudoku.Grid.MAX_EMPTIES;

/**
 * AlsFinder finds Almost Locked Sets. This code was split-out of AAlsHinter
 * because it just kept getting more-and-more complex.
 * <p>
 * NOTE: AlsFinderRecursive extends AlsFinder, using my protected stuff.
 *
 * @author Keith Corlett 2021-06-04
 */
class AlsFinder {

//	// alss.index of start of each degree 2..8
//	final int[] starts = new int[REGION_SIZE];

//	// set in AlsFinderRecursive only, reported in BigWings
//	protected long took;

	private final AlsSet alsSet = new AlsSet();
	private final Idx tmp1 = new Idx();
	private final Idx tmp2 = new Idx();
	private final Idx tmp3 = new Idx();
	final Idx[] nakedSetIdxs = new Idx[NUM_REGIONS];

	AlsFinder() {
		for ( int i=0,n=nakedSetIdxs.length; i<n; ++i )
			nakedSetIdxs[i] = new Idx();
	}

	// find all distinct Almost Locked Sets in this grid
	int getAlss(final Grid grid, final Idx[] candidates, final Als[] alss
			, final boolean allowNakedSets) {
		// find Alss
		if ( !allowNakedSets && findNakedSetIdxs(grid) )
			findAlssNoNakedSets(grid);
		else
			findAlss(grid);
		// copy and computeFields
		int cnt = 0;
		for ( Als als : alsSet ) {
//			System.out.println(""+cnt+TAB+als);
			als.computeFields(grid, candidates);
			alss[cnt++] = als;
		}
		// clear the Set, for next-time
		alsSet.clear();
		return cnt;
	}

	// populate nakedSetIdxs: an array Idxs, one per region. Each Idx contains
	// indices of cells in any Naked Set in this region. This is necessary coz
	// any Naked Sets in ALSs break AlsChains.
	boolean findNakedSetIdxs(Grid grid) {
		Idx nakedSetIdx;
		Cell[] empties;
		int[] maybes;
		int n, ceiling, size, vs, i;
		boolean result = false; // are there any Naked Sets whatsoever
		for ( ARegion r : grid.regions ) {
			nakedSetIdx = nakedSetIdxs[r.index].clear();
			if ( (n=r.emptyCellCount) > 2 ) {
				empties = r.emptyCells(Cells.arrayA(n));
				maybes = Cells.maybes(empties, Idx.IAS_A[n]);
				// do Naked Pairs seperately for speed
				for ( int[] perm : new Permutations(n, Idx.IAS_B[2]) )
					if ( VSIZE[maybes[perm[0]] | maybes[perm[1]]] == 2 ) {
						nakedSetIdx.add(empties[perm[0]].i);
						nakedSetIdx.add(empties[perm[1]].i);
						result = true;
					}
				if(n<5) ceiling=n; else ceiling=5;
				for ( size=3; size<ceiling; ++size )
					for ( int[] perm : new Permutations(n, Idx.IAS_B[size]) )
						if ( VSIZE[vs=maybes[perm[0]] | maybes[perm[1]]] <= size ) {
							for ( i=2; i<size; ++i )
								vs |= maybes[perm[i]];
							if ( VSIZE[vs] == size ) {
								for ( i=0; i<size; ++i )
									nakedSetIdx.add(empties[perm[i]].i);
								result = true;
							}
						}
			}
		}
		return result;
	}

	// find Almost Locked Sets ignoring cells in Naked Sets
	private void findAlssNoNakedSets(Grid grid) {
		int[] maybeses;
		int n, numCells, vs, i;
		final Idx empties = grid.getEmpties();
		final Cell[] gcells = grid.cells;
		final Cell[] cas = Cells.arrayA(MAX_EMPTIES); // 64 = 81 - 17
		for ( ARegion r : grid.regions ) { // 27
			if ( tmp1.setAndMany(r.idx, empties)
			  && tmp2.setAndNotAny(tmp1, nakedSetIdxs[r.index])
			  && tmp2.size() > 2 // we need 3 or more cells to form an ALS
			) {
				for ( n=2; n<REGION_SIZE; ++n ) { // number of cells
					final int nPlus1 = n + 1; // number of cands
					if ( (numCells=tmp2.cellsWhere(gcells, cas, (c) -> {
								return c.size <= nPlus1;
					      })) > n ) {
						maybeses = Cells.maybes(cas, Idx.IAS_A[numCells]);
						for ( int[] perm : new Permutations(numCells, Idx.IAS_B[n]) ) {
							if ( VSIZE[vs=maybeses[perm[0]]|maybeses[perm[1]]] <= nPlus1 ) {
								for ( i=2; i<n; ++i )
									vs |= maybeses[perm[i]];
								if ( VSIZE[vs] == nPlus1 )
									alsSet.add(new Als(tmp3.set(cas, perm), vs, r));
							}
						}
					}
				}
			}
		}
	}

	// find Almost Locked Sets
	private void findAlss(Grid grid) {
		int[] maybeses;
		int n, numCells, vs, i;
		final Idx empties = grid.getEmpties();
		final Cell[] gcells = grid.cells;
		final Cell[] cas = Cells.arrayA(MAX_EMPTIES); // 64 = 81 - 17
		for ( ARegion r : grid.regions ) { // 27
			if ( tmp1.setAndMany(r.idx, empties) ) {
				for ( n=2; n<REGION_SIZE; ++n ) { // number of cells
					final int nPlus1 = n + 1; // number of cands
					if ( (numCells=tmp1.cellsWhere(gcells, cas, (c) -> {
								return c.size <= nPlus1;
					      })) > n ) {
						maybeses = Cells.maybes(cas, Idx.IAS_A[numCells]);
						for ( int[] perm : new Permutations(numCells, Idx.IAS_B[n]) ) {
							if ( VSIZE[vs=maybeses[perm[0]]|maybeses[perm[1]]] <= nPlus1 ) {
								for ( i=2; i<n; ++i )
									vs |= maybeses[perm[i]];
								if ( VSIZE[vs] == nPlus1 )
									alsSet.add(new Als(tmp3.set(cas, perm), vs, r));
							}
						}
					}
				}
			}
		}
	}

	// LinkedHashSet iterator uses a linked-list that respects "natural order",
	// ie it comes back in the order it was added. It's still an iterator which
	// are all slow because processing each element requires not one but two
	// method invocations, but it's faster than HashSet's iterator.
	protected class AlsSet extends LinkedHashSet<Als> implements Set<Als> {
		private static final long serialVersionUID = 159335969601498L;
		AlsSet() {
			super(MAX_ALSS, 1F);
		}
		@Override
		public boolean add(Als als) {
			return !contains(als)
				&& super.add(als);
		}
	}

}
