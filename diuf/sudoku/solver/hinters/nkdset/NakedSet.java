/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.chain.ChainersHintsAccumulator;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.utils.MyArrays;
import java.util.Arrays;
import java.util.List;
import static diuf.sudoku.Values.ALL;


/**
 * Implementation of the Naked Pair, Naked Triple, Naked Quad, Naked Pent,
 * and Naked* solving techniques, depending upon the Tech passed into my
 * constructor.
 */
public final class NakedSet
		extends AHinter
		implements diuf.sudoku.solver.hinters.ICleanUp
{

	// just used in an assert to check that I'm not being chained directly.
	private static final String ChainersHintsAccumulatorName =
		ChainersHintsAccumulator.class.getCanonicalName();

	private final int[] thePA; // the Permutations Array
	private final Cell[] candidateCells = new Cell[9];

	public NakedSet(Tech tech) {
		super(tech);
		assert degree>=2 && degree<=5; // Pair, Triple, Quad, Pent
		this.thePA = new int[degree]; // the Permutations Array
	}

	@Override
	public void cleanUp() {
		MyArrays.clear(candidateCells);
	}

	/**
	 * <pre>
	 * For each region: find $degree cells which maybe $degree potential values.
	 * IE seek 2 cells which have the same 2 potential values;
	 * OR seek 3 cells whose combined maybes number just 3;
	 * OR seek 4 cells which can only be these 4 values (ie 1 each).
	 * </pre>
	 *
	 * @return was a hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// SingleHintsAccumulator.add returns true to indicate to all hinters
		// that I want you to wind-up the search at the first hint.
		final boolean oneOnly = accu.isSingle();
		// presume failure, ie that no hint will be found
		boolean result = false;
		for ( ARegion r : grid.regions ) // 27 = 9*box, 9*row, 9*col
			if ( (result|=search(r, grid, accu)) && oneOnly )
				break;
		return result;
	}

	/**
	 * WEIRD: The Locking hinter has a habit of finding Naked Pairs/Triples,
	 * but Locking finds only a subset of the possible eliminations, so Locking
	 * asks NakedSet "Is this a NakedSet? And if so do the eliminations for me,
	 * coz that's your gig?"; and search also does the "normal" search.
	 *
	 * @param r
	 * @param grid
	 * @param accu
	 * @return
	 */
	public boolean search(final ARegion r, final Grid grid, final IAccumulator accu) {
		// no point looking unless this region has an "extra" empty cell to
		// possibly remove maybes from. Note that I'm public, so done here.
		if ( r.emptyCellCount < this.degreePlus1 )
			return false;
		AHint hint;
		int i, n, card, nkdSetValsBits, nkdSetIdxBits;
		final int degreePlus1 = this.degreePlus1;
		final int degree = this.degree;
		final Cell[] candi = this.candidateCells;
		final int[] thePA = this.thePA;
		final int rti = r.typeIndex;
		// presume failure, ie that no hint will be found
		boolean result = false;
		// candidates := region.cells with 2..$degree maybes
		n = 0; // number of candidate cells
		for ( Cell cell : r.cells )
			if ( (card=cell.maybes.size)>1 && card<degreePlus1 )
				candi[n++] = cell;
		if ( n < degree ) // 2 cells for a naked pair, 3 for triple, 4 for quad
			return false;
		// foreach combination of $degree cells among the candidate cells
		for ( int[] perm : new Permutations(n, thePA) ) {
			// are there only $degree maybes in this $degree cells?
			nkdSetValsBits = 0;
			for ( i=0; i<degree; ++i )
				nkdSetValsBits |= candi[perm[i]].maybes.bits;
			if ( VSIZE[nkdSetValsBits] != degree )
				continue;
			//
			// Yeah! Naked Set found, but does it remove any maybes?
			//
			// but first create a bitset of the positions of the naked set
			// cells in the region.cells array, to pass to the methods.
			nkdSetIdxBits = 0;
			for ( i=0; i<degree; ++i )
				// left-shift the cells index in this type of region
				nkdSetIdxBits |= ISHFT[candi[perm[i]].indexIn[rti]];
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
		// just build removable (red) potentials AND NO OTHER S__T!
		// to find out if this Naked Set removes any maybes (96+% don't)
		Pots reds = null;
		Cell sib;
		// foreach cell (sib) in the region except the-naked-set-cells
		for ( int i : INDEXES[ALL & ~nkdSetIdxBits] )
			if ( ((sib=region.cells[i]).maybes.bits & nkdSetValsBits) != 0 ) {
				// sib maybe any nkdSetValue
				if(reds==null) reds = new Pots();
				reds.put(sib, sib.maybes.intersectBits(nkdSetValsBits));
			}
		if ( reds == null )
			return null;
		// find additional pointing/claiming eliminations.
		final List<Cell> nkdSetCellList = region.atNewArrayList(nkdSetIdxBits);
		ARegion[] regions = new ARegion[]{region};
		if ( region instanceof Grid.Box )
			regions = claimFromOtherCommonRegion(region, nkdSetCellList
					, nkdSetValsBits, reds, regions);
		// build highlighted (orange) potentials
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
			, List<Cell> nkdSetCellList, int nkdSetValsBits, Pots reds
			, ARegion[] regions) {
		ARegion otherCR = Grid.otherCommonRegion(nkdSetCellList, region);
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

	// If this NakedSet strips any victim "naked" (ie down to 1) then
	// create a Direct hint and return it; else null.
	private AHint searchForSubsequentNakedSingle(Grid grid, ARegion region
			, int nkdSetValsBits, int nkdSetIdxBits) {
		assert tech.isDirect;
		Cell sib;  int redBits;
		// foreach cell in the region EXCEPT the cells in this naked set
		for ( int i : INDEXES[ALL & ~nkdSetIdxBits] )
			// skip if sib doesn't have $degreePlus1 maybes
			if ( (sib=region.cells[i]).maybes.size == degreePlus1
			  // if sib has the nkdSetValues plus ONE other
			  && VSIZE[redBits=sib.maybes.bits & ~nkdSetValsBits] == 1 )
				// then we can create the hint and add it to the accumulator
				return createNakedSetDirectHint(
					  region
					, sib						// cellToSet
					, FIRST_VALUE[redBits]		// valueToSet
					, nkdSetIdxBits				// nkdSetIdxBits
					, nkdSetValsBits			// nkdSetValsBits
				);
		return null;
	}

	// NB: This method is called in DIRECT mode only (ie only in the GUI)
	// only when creating a hint, so performance isn't really and issue.
	private AHint createNakedSetDirectHint(ARegion region, Cell cellToSet
			, int valueToSet, int nkdSetIdxBits, int nkdSetValsBits) {
		assert tech.isDirect;
		// build removable (red) potentials
		Pots reds = new Pots(9-degree, 1F);
		// foreach cell in the region EXCEPT the naked set cells
		Cell sib;  int redBits; // bitset of values to remove from sib
		for ( int i : INDEXES[ALL & ~nkdSetIdxBits] )
			// if sib maybe any of the naked set values
			if ( (redBits=(sib=region.cells[i]).maybes.bits & nkdSetValsBits) != 0 )
				reds.put(sib, new Values(redBits, false));
		assert !reds.isEmpty();
		// claim the NakedSet values from the other common region (if any)
		List<Cell> ndkSetCells = region.atNewArrayList(nkdSetIdxBits);
		ARegion ocr = Grid.otherCommonRegion(ndkSetCells, region);
		if ( ocr != null )
			claimFrom(ocr.otherThan(ndkSetCells), nkdSetValsBits, reds);
		Pots oranges = new Pots();
		for ( Cell cell : ndkSetCells )
			oranges.put(cell, new Values(cell.maybes));
		// build the hint
		return new NakedSetDirectHint(this, cellToSet, valueToSet, ndkSetCells
			, new Values(nkdSetValsBits, degree, false) // nkdSetValues
			, oranges, reds, region);
	}

}
