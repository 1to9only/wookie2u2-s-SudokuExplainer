/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.naked;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.MASKED;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.IVisitor2;
import diuf.sudoku.utils.Permuter;
import java.util.function.Predicate;
import static diuf.sudoku.Idx.FIFTY_FOUR;
import static diuf.sudoku.Idx.MASKED81;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;

/**
 * NakedSet is a partial implementation of the NakedPair, NakedTriple,
 * and NakedQuad Sudoku solving techniques, depending on the Tech passed to my
 * constructor.
 * <p>
 * Note: NakedSet can find larger sets, but NakedPent=5 and up are degenerate
 * so the NakedPent Tech has been deleted. Degenerate means that every hint
 * can be replaced with a simpler hint (or combination of simpler hints) that
 * make the same eliminations.
 * <pre>
 * KRC 2022-01-20 converted to Idxs instead of reading the nakedSet and elims
 * from region.cells, because it is simpler, and very very nearly as fast. This
 * solution is about 1ms slower, which is not too shabby. I tried expanding all
 * the Idx ops, including commonBuddies, but the result is a LOT of code and it
 * was actually a tad slower, so I guess the compiler prefers nice little blobs
 * of code to optimise, as apposed to war and bloody peace. BIG Sigh.
 *
 * KRC 2022-01-22 Added IntHashMap knownNakedSets (see below), for speed.
 * KRC 2023-03-26 Trying to make this faster.
 * BEFORE
 *   87,578,000	  22125	         3,958	   7845	        11,163	NakedPair
 *  114,050,300	  18028	         6,326	   1644	        69,373	NakedTriple
 * AFTER
 *   87,689,600	  22125	         3,963	   7845	        11,177	NakedPair
 *  112,824,600	  18028	         6,258	   1644	        68,628	NakedTriple
 *
 * 2023-07-20 KRC Split into NakedPair, NakedTriple, and NakedQuad which extend
 * NakedSet to re-implement the findHints method so that reading permutations
 * is efficient; all in one line, which is only possible when knows at compile
 * time the size of each permutation.
 *
 * 2023-10-07 KRC Replace cmnBuds2 wit direct forEachIndice on an exploded Idx,
 * hence faster to calc cands and possible victims first, which are reduced by
 * forEachIndice, hence forEachIndce stops ASAP upon failure (~97% case).
 * Basically, delay doing common buds until the last step, so it fails faster.
 * Then I find this impl used only in NakedSetDirect, so these changes are not
 * worth a pinch of goats__t, until I port them to the specific size overrides.
 * Get back to work! Sigh.
 * </pre>
 */
public class NakedSet extends AHinter {

	// A Depermuter reads permutations
	private final ICandsDepermuter cander;

	/**
	 * Permuter produces possible combinations of k in n elements of a set.
	 * <pre>
	 * k is pa.length, ie degree.
	 * n is number of cells in region with 2..degree maybes.
	 * </pre>
	 */
	protected final Permuter permuter = new Permuter();

	protected NakedSet(final Tech tech) {
		super(tech);
		assert this instanceof NakedSetDirect || !tech.isDirect;
		assert degree>=2 && degree<5; // Pair, Triple, Quad (Pent died!)
		// lambdas because theres no macros in java (sigh)
		switch ( degree ) {
			case 2: cander=(m,p)->m[p[0]]|m[p[1]]; break;
			case 3: cander=(m,p)->m[p[0]]|m[p[1]]|m[p[2]]; break;
			case 4: cander=(m,p)->m[p[0]]|m[p[1]]|m[p[2]]|m[p[3]]; break;
			case 5: cander=(m,p)->m[p[0]]|m[p[1]]|m[p[2]]|m[p[3]]|m[p[4]]; break;
			default: throw new IllegalStateException("bad degree="+degree);
		}
	}

	/**
	 * Foreach region: find N cells which maybe only N potential values,
	 * where N is the degree of the Tech passed to my constructor.
	 * <p>
	 * These N cells form a "Locked Set", meaning that each of these N values
	 * MUST be in one of these N cells, so these N values can be removed from
	 * other cells which see all N cells in the Locked Set.
	 * <p>
	 * <b>WARNING</b>: As of 2023-07-20 this implementation is executed only by
	 * {@link NakedSetDirect} and the test-cases. Direct* is used only in the
	 * GUI, by putzes, so performance of this implementation no longer matters!
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return was a hint/s found?
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// ChainerDynamic never uses Direct*
		assert !tech.isDirect || !accu.isChaining();
		Idx idx;
		// victims Exploded Idx (mutated in lambda)
		long vc0=0L; int vc1=0;
		int i // the ubiquitous index
		  , n // a count of the candidate cells in this region
		  , cands // bitset of combined maybes of cells in this naked set
		  , indice // the index of a cell in Grid.cells
		  , va[], vn, vi // array, size, index
		;
		boolean any;
		final Predicate<Cell> sized = (c)-> c.size>1 && c.size<degreePlus1;
		// the maybes of the candidate cells
		final int[] myMaybes = new int[REGION_SIZE]; // intentional mispelling, this is not supers maybes
		final IVisitor2<Cell, Integer> maybesReader = (cell, count) -> myMaybes[count] = cell.maybes;
		// the indices in grid.cells of the candidate cells
		final int[] myIndices = new int[REGION_SIZE];
		final IVisitor2<Cell, Integer> indicesReader = (cell, count) -> myIndices[count] = cell.indice;
		final int[] pa = new int[degree]; // permutations array
		long ns0; int ns1; // nakedSet Exploded Idx
		final Idx[] idxs = grid.idxs; // cells in grid that maybe each value
		boolean result = false; // presume that no hints will be found
		// foreach region in the grid
		for ( ARegion region : grid.regions ) {
			// if this region has an "extra" empty cell to remove maybes from
			if ( region.emptyCellCount > degree
			  // and we need atleast degree niceSized cells, where niceSized is
			  // select whatever from region.cells where size in 2..degree; and
			  // in this case whatever is maybes, which I need for de.permute.
			  && (n=region.query(sized, maybesReader)) > degreeMinus1
			) {
//<HAMMERED comment="top1465 iterates the below loop over 10 million times, which is my definition of hammered, which basically means warning this takes time. All code in SE thats hammered gets a hammered marker comment, and this is the first time (AFAIK) that this occurs in SEs hinters callstack, hence this explanation. Basically HAMMERED just means watch your step, for performance.">
//UPDATE: This implementation of findHints is now used only by NakedSetDirect, which is only used in the GUI by putzes who want Direct*, so performance matters not!
				// foreach possible combination of $degree cells among the $n
				// candidate cells. Permutations.reset() is used rather than
				// creating a new Permutations for each matching region, coz
				// thats faster; less malloc means less GC, ie more speed!
				for ( int[] p : permuter.permute(n, pa) ) {
					// Are there n maybes in this permutation of n cells?
					// cands is an aggregate of the maybes of the cells in this
					// NakedSet (ie this permutation of cells).
					// cander.read is a lambda chosen in my constructor.
					if ( VSIZE[cander.read(myMaybes, p)] == degree ) {
						// its a NakedSet, but any elims? (~96% have none)
//</HAMMERED>
						// initialise victims to all vs in the grid,
						// where v is a maybe of a cell in the NakedSet.
						// sideEffect: cands = maybes of cells in the NakedSet
						// foreach maybe of the cells in the NakedSet
						for ( va=VALUESES[cands=cander.read(myMaybes, p)],vn=va.length,vi=0; vi<vn; ) {
							// victims |= commonBuddies & vsInTheGrid
							vc0 |= (idx=idxs[va[vi++]]).m0;
							vc1 |= idx.m1;
						}
						// read indices of cells in this NakedSet
						region.query(sized, indicesReader); // -> indices
						// reduce vc* to cells seeing ALL cells in NakedSet
						any = true;
						for ( i=0; i<degree; ++i ) {
							indice = myIndices[p[i]];
							if ( ( (vc0 &= BUDS_M0[indice])
							     | (vc1 &= BUDS_M1[indice]) ) < 0L ) {
								any = false;
								break;
							}
						}
						if ( any ) {
							ns0 = ns1 = 0;
							for ( i=0; i<degree; ++i )
								if ( (indice=myIndices[p[i]]) < FIFTY_FOUR )
									ns0 |= MASKED81[indice];
								else
									ns1 |= MASKED[indice];
							// WARN: This is NOT a tautology in Generate!
							if ( Idx.size(ns0,ns1) == degree ) {
								// FOUND a NakedSet!
								// WARN: NakedSetDirect overrides createHint!
								final AHint hint = hint(grid, region, cands
								, new Idx(ns0, ns1)
								, new Pots(vc0,vc1, grid.maybes, cands, DUMMY));
								vc0 = vc1 = 0; // cough drop!
								if ( hint != null ) {
									result = true;
									if ( accu.add(hint) )
										return result;
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Create the hint.
	 * <p>
	 * WARN: overridden by {@link NakedSetDirect#hint }.
	 *
	 * @param grid the grid we are searching
	 * @param region the region we are searching
	 * @param cands the values of the NakedSet
	 * @param nakedSet indices of cells in the NakedSet
	 * @param reds removable (red) potentials
	 * @return a new NakedSetHint as a "base" AHint
	 */
	protected AHint hint(final Grid grid, final ARegion region, final int cands
			, final Idx nakedSet, final Pots reds) {
		// for NakedSetDirect: find extra elims in NakedPair/Triple (not Quad+)
		final ARegion[] nsRegions;
		if ( degree<4 && region instanceof Box )
			nsRegions = Regions.array(grid.regions, nakedSet.commonRibs());
		else
			nsRegions = Regions.array(region);
		final Pots greens = new Pots(nakedSet, grid.maybes, cands, DUMMY);
		// Build the hint
		return new NakedSetHint(grid, this, nakedSet.cellsNew(grid.cells)
				, cands, greens, reds, nsRegions);
	}

	/**
	 * A Depermuter is a lambda expression that reads a permutation. This one
	 * reads cands (a maybes bitset).
	 */
	private static interface ICandsDepermuter {
		int read(int[] data, int[] permutation);
	}

}
