/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.chain.ChainersHintsAccumulator;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.utils.MyArrays;
import java.util.Arrays;
import java.util.List;


/**
 * Implementation of the Naked Pair, Naked Triple, Naked Quad, Naked Pent,
 * and Naked* solving techniques, depending upon the Tech passed into my
 * constructor.
 */
public final class NakedSet
		extends AHinter
		implements diuf.sudoku.solver.hinters.ICleanUp
//				 , diuf.sudoku.solver.IReporter
{
//private int cnshCnt, cnshPass;

	// just used in an assert to check that I'm not being chained directly.
	private static final String ChainersHintsAccumulatorName =
		ChainersHintsAccumulator.class.getCanonicalName();

	private final int[] thePA; // for Permutations
	private final Cell[] candidateCells = new Cell[9];

	public NakedSet(Tech tech) {
		super(tech);
		assert degree>=2 && degree<=5; // Pair, Triple, Quad, Pent
		this.thePA = new int[degree]; // for Permutations
	}

//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef(
//              "// "+tech.name+" pass %,14d of %,14d = skip %4.2f\n"
//				, cnshPass, cnshCnt, Log.pct(cnshPass, cnshCnt));
//	}

	@Override
	public void cleanUp() {
		MyArrays.clear(candidateCells);
	}

	/**
	 * For each region: find $degree cells which maybe $degree potential values.
	 * IE seek 2 cells which have the same 2 potential values;
	 * OR seek 3 cells whose combined maybes number just 3;
	 * OR seek 4 cells which can only be these 4 values (ie 1 each).
	 * @return was a hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// SingleHintsAccumulator.add returns true to indicate to all hinters
		// that I want you to wind-up the search at the first hint.
		final boolean exitEarly = accu.isSingle();
		// presume failure, ie that no hint will be found
		boolean result = false;
		for ( ARegion r : grid.regions ) { // 27 = 9*box, 9*row, 9*col
//// MIA: Naked Pair in 1106#top1465.d5.mt at E6 and F6 on 1 and 5
//// in both box 5 and row 6
////...279...9......7..746..........35...48.6...3..68..2.........3.8..1....5.17.9....
////1356,3568,135,,,,13468,14568,1468,,23568,1235,345,13458,1458,1368,,1268,1235,,,,1358,158,1389,12589,1289,127,29,129,479,124,,,68,68,1257,,,579,,1257,179,19,,1357,359,,,15,15,,149,1479,2456,2569,259,457,2458,245678,146789,,1246789,,2369,239,,234,2467,4679,2469,,23456,,,345,,24568,468,2468,2468
////1106#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
//if ( tech == Tech.NakedPair
//  && "row 6".equals(r.id)
//  // both E6 and F6 maybe 1 or 5
//  && "15".equals(r.cells[4].maybes.toString())
//  && "15".equals(r.cells[5].maybes.toString())
//  && !Debug.isClassNameInTheCallStack(5, "RecursiveAnalyser") )
//	Debug.breakpoint();
			if ( (result |= search(r, grid, accu)) && exitEarly )
				break;
		}
		return result;
	}

	/**
	 * Strap in, this one's weird. The Locking hinter has a habit of
 finding Naked Pairs and Naked Triples, but the "Locking" algorithms find
 only a subset of the possible eliminations, so he needs a way of asking
 NakedSet "Is this a NakedSet? And if so can you do the eliminations for
 me, because that's your specialty?"... and this method is how he asks.
	 * @param r
	 * @param grid
	 * @param accu
	 * @return
	 */
	public boolean search(ARegion r, Grid grid, IAccumulator accu) {
		// localise everything for speed
		final int[] SIZE = Values.SIZE;
		final int[] ISHFT = Indexes.SHFT;
		final int degreePlus1 = this.degreePlus1;
		final int degree = this.degree;
		final Cell[] candi = this.candidateCells;
		final int[] thePA = this.thePA;
		AHint hint;
		int i, n, card, nkdSetValsBits, nkdSetIdxBits;
		// presume failure, ie that no hint will be found
		boolean result = false;

		// We need 3 empty cells in the region for a Naked Pair to do any
		// good: 2 in the Pair and atleast 1 other to remove maybes from.
		if ( r.emptyCellCount < degreePlus1 )
			return false;
		// candidates := region.cells with 2..$degree maybes
		n = 0; // number of candidate cells
		for ( Cell cell : r.cells ) {
			card = cell.maybes.size;
			if ( card>1 && card<degreePlus1 )
				candi[n++] = cell;
		}
		if ( n < degree ) // 2 cells required for a naked pair
			return false;
		// foreach combination of $degree cells among the candidate cells
		for ( int[] perm : new Permutations(n, thePA) ) {
			// are there only $degree maybes in this $degree cells?
			nkdSetValsBits = 0;
			for ( i=0; i<degree; ++i )
				nkdSetValsBits |= candi[perm[i]].maybes.bits;
			if ( SIZE[nkdSetValsBits] != degree )
				continue;
			//
			// Yeah! We found a Naked Set, but does it remove any maybes?
			//
			// But first create a bitset of the positions of the naked set
			// cells in the region.cells array, to pass to the methods.
			nkdSetIdxBits = 0;
			for ( i=0; i<degree; ++i )
				// left-shift the cells index in this type of region
				nkdSetIdxBits |= ISHFT[candi[perm[i]].indexIn[r.typeIndex]];
			// So, are we in Direct or normal Mode?
			if ( tech.isDirect ) {
				// DIRECT MODE: hint only if it causes a Hidden Single
				// Chainer uses NakedSet, but NEVER in direct mode
				assert !accu.getClass().getCanonicalName()
						.equals(ChainersHintsAccumulatorName);
				if ( (hint=searchForSubsequentNakedSingle(grid, r
						, nkdSetValsBits, nkdSetIdxBits)) != null ) {
					result = true;
					if ( accu.add(hint) // SingleHA.add returns true
					  || n-degree < degree ) // there can be no others
						return true;
				}
			} else {
				// NORMAL MODE: hint (less than 4%) found!
				// (the other 96+% don't remove any maybes).
				if ( (hint=createNakedSetHint(grid, r, nkdSetIdxBits
						, nkdSetValsBits)) != null ) {
					result = true;
					if ( accu.add(hint) // SingleHA.add returns true
					  || n-degree < degree ) // there can be no others
						return true;
				}
			}
		} // next permutation
		return result;
	}

	private AHint createNakedSetHint(Grid grid, ARegion region
			, int nkdSetIdxBits, int nkdSetValsBits) {
		final int degree = this.degree;
		// JUST Build removable (red) potentials AND NO OTHER S__T!
		// to find out if this Naked Set removes any maybes (96+% don't)
		Pots reds = null;
		Cell sib;
		// foreach cell (sib) in the region except the-naked-set-cells
		for ( int i : Indexes.ARRAYS[Indexes.ALL_BITS & ~nkdSetIdxBits] )
			if ( ((sib=region.cells[i]).maybes.bits & nkdSetValsBits) != 0 ) {
				// sib maybe any nkdSetValue
				if(reds==null) reds = new Pots(9-degree, 1F);
				reds.put(sib, sib.maybes.intersectBits(nkdSetValsBits));
			}
//++cnshCnt;
		if ( reds == null )
			// NakedPair   2,176 of 69,407 = skip 96.86%
			// NakedTriple   403 of 25,733 = skip 98.43%
			// NakedQuad      22 of 14,113 = skip 99.84%
			return null;
//++cnshPass;

		// Find additional pointing/claiming eliminations.
		final List<Cell> nkdSetCellList = region.atNewArrayList(nkdSetIdxBits);
		ARegion[] regions = new ARegion[]{region};
		if ( region instanceof Grid.Box)
			regions = claimFromOtherCommonRegion(region, nkdSetCellList, grid
					, nkdSetValsBits, reds, regions);

		// Build highlighted (orange) potentials
		final Pots oranges = new Pots(degree, 1F);
		for ( Cell cell : nkdSetCellList )
			oranges.put(cell, cell.maybes.intersectBits(nkdSetValsBits));
		// Build the hint
		final Values nkdSetValues = new Values(nkdSetValsBits, degree, false);
		return new NakedSetHint(this, nkdSetCellList, nkdSetValues, oranges
				, reds, Arrays.asList(regions));
	}

	// Find additional pointing/claiming eliminations.
	// If the nkdSetCellList all share another common region then we can
	// eliminate the nakedSetValues from those cells, ie pointing/claiming;
	// so if this step were omitted then the Locking hinter finds and
	// eliminates these values; it's just nicer all in one hint, dat's all.
	// We need only perform this check if the region is a Box because they
	// are searched first, so we find any additional pointing eliminations
	// for the Box=>Row/Col NakedSet hint; and then the Row/Col=>Box hint
	// doesn't (I think) need to bother with claiming. Find a test-case dat
	// proves me wrong and I'll code for it, but Box only until then!
	private ARegion[] claimFromOtherCommonRegion(ARegion region
			, List<Cell> nkdSetCellList, Grid grid, int nkdSetValsBits
			, Pots reds, ARegion[] regions) {
		ARegion otherCR = grid.otherCommonRegion(nkdSetCellList, region);
		if ( otherCR != null ) {
			// add any eliminations in the otherRegion to reds
			List<Cell> victims = otherCR.otherThan(nkdSetCellList);
			if ( !victims.isEmpty() ) {
				boolean any = claimFrom(victims, nkdSetValsBits, reds);
				if ( any )
					regions = new ARegion[]{region, otherCR};
			}
		}
		return regions;
	}

	/**
	 * Claim the values in nkdSetValsBits from the Cells in the victims List,
	 * adding any eliminations to the reds.
	 *
	 * @param victims
	 * @param nkdSetValsBits
	 * @param reds
	 * @return
	 */
	private boolean claimFrom(List<Cell> victims, int nkdSetValsBits, Pots reds) {
		boolean any = false;
		int redBits;
		for ( Cell c : victims )
			if ( (redBits=c.maybes.bits & nkdSetValsBits) != 0 ) {
				// note that reds can't already contain these cells
				reds.put(c, new Values(redBits, true));
				any = true;
			}
		return any;
	}

	// If this Naked Set causes any sibling-cells maybes to be "stripped naked"
	// (ie down to 1 potential value) then create a Direct hint and return it.
	private AHint searchForSubsequentNakedSingle(Grid grid, ARegion region
			, int nkdSetValsBits, int nkdSetIdxBits) {
		assert tech.isDirect;
		final int[] SIZE = Values.SIZE;
		final int[] FIRST_INDEX = Indexes.NUM_TRAILING_ZEROS;
		Cell sib;  int redBits;
		// foreach cell in the region EXCEPT the cells in this naked set
		for ( int i : Indexes.ARRAYS[Indexes.ALL_BITS & ~nkdSetIdxBits] ) {
			// skip if sib doesn't have $degreePlus1 maybes
			if ( (sib=region.cells[i]).maybes.size != degreePlus1 )
				continue;
			// if the cell has the nkdSetValues plus ONE other
			if ( SIZE[redBits=sib.maybes.bits & ~nkdSetValsBits] == 1 ) {
				// then we can create the hint and add it to the accumulator
				return createNakedSetDirectHint(
					  grid, region, sib //cellToSet
					, FIRST_INDEX[redBits]+1 //valueToSet
					, nkdSetIdxBits //nkdSetIdxBits
					, nkdSetValsBits //nkdSetValsBits
				);
			}
		}
		return null;
	}

	// NB: This method is called in DIRECT mode only (ie only in the GUI)
	// so performance isn't and issue.
	private AHint createNakedSetDirectHint(Grid grid, ARegion region
			, Cell cellToSet, int valueToSet, int nkdSetIdxBits
			, int nkdSetValsBits) {
		assert tech.isDirect;
		// build removable (red) potentials
		Pots redPots = new Pots(9-degree, 1F);
		// foreach cell in the region EXCEPT the naked set cells
		Cell sib;
		for ( int i : Indexes.ARRAYS[Indexes.ALL_BITS & ~nkdSetIdxBits] )
			// if sib maybe any of the naked set values
			if ( ((sib=region.cells[i]).maybes.bits & nkdSetValsBits) != 0 )
				redPots.put(sib, sib.maybes.intersectBits(nkdSetValsBits));
		assert !redPots.isEmpty();
		// claim the NakedSet values from the other common region (if any)
		List<Cell> ndkSetCellsList = region.atNewArrayList(nkdSetIdxBits);
		ARegion ocr = grid.otherCommonRegion(ndkSetCellsList, region);
		if ( ocr != null )
			claimFrom(ocr.otherThan(ndkSetCellsList), nkdSetValsBits, redPots);
		Pots orangePots = new Pots();
		for ( Cell cell : ndkSetCellsList )
			orangePots.put(cell, new Values(cell.maybes));
		// build the hint
		return new NakedSetDirectHint(
			  this, cellToSet, valueToSet, ndkSetCellsList
			, new Values(nkdSetValsBits, degree, false) // nkdSetValues
			, orangePots
			, redPots, region
		);
	}

}
