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
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.chain.ChainersHintsAccumulator;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.utils.MyArrays;
import java.util.List;

/**
 * NakedSet implements the NakedPair, NakedTriple, NakedQuad, or NakedPent
 * Sudoku solving technique, depending on the Tech passed to my constructor.
 * <p>
 * Note: NakedSet can find larger sets, but NakedPent=5 and up are degenerate;
 * so Tech.NakedHex and up do NOT exist and are untested.
 */
public final class NakedSet extends AHinter
		implements diuf.sudoku.solver.hinters.ICleanUp
{
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

	//<NO_WRAP>
	/**
	 * <pre>
	 * Foreach region: find N cells which maybe only N potential values.
	 * IE seek 2 cells which have the same 2 potential values;
	 * OR seek 3 cells whose combined maybes number just 3;
	 * OR seek 4 cells which can only be these 4 values (ie 1 each).
	 *
	 * These N cells form a "Locked Set", meaning that each of these N values
	 * MUST be in one of these N cells, so these N values can be removed from
	 * other cells which see all N cells in the Locked Set.
	 *
	 * N is the degree, ya putz!
	 * </pre>
	 *
	 * @return was a hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Cell sib; // sibling
		int[] ia; // indexes of cells in this region EXCEPT naked set cells
		int i // the ubiquitous index
		  , n // number of candidate cells in this region
		  , card // cardinality: cell.maybes.size
		  , cands // maybes.bits of cells in this naked set
		  , nkdSet // indexes in this region of cells in this naked set
		  , nn; // ia.length: number of cells in region EXCEPT nkdSetCells
		boolean any; // were any eliminations added?
		final int degreePlus1 = this.degreePlus1;
		final int degree = this.degree;
		final int degreeMinus1 = degree - 1;
		final Cell[] candi = this.candidateCells;
		final int[] thePA = this.thePA;
		final boolean isDirect = tech.isDirect;
		int rti; // r.typeIndex
		// presume that no hint will be found
		boolean result = false;
		// chainer never uses direct mode
		assert !tech.isDirect || !(accu instanceof ChainersHintsAccumulator);
		// foreach region in the grid // 27 = 9*box, 9*row, 9*col
		for ( ARegion r : grid.regions ) {
			// if this region has an "extra" empty cell to remove maybes from
			if ( r.emptyCellCount > degree ) {
				// candidates := region.cells with 2..$degree maybes
				n = 0; // number of candidate cells
				for ( Cell cell : r.cells )
					if ( (card=cell.maybes.size)>1 && card<degreePlus1 )
						candi[n++] = cell;
				// need 2 cells for Pair, 3 for Triple, 4 for Quad
				if ( n > degreeMinus1 ) {
					rti = r.typeIndex;
					// foreach combo of $degree cells among the candidates
					for ( int[] perm : new Permutations(n, thePA) ) {
						// are there $degree maybes in this $degree cells?
						cands = 0;
						for ( i=0; i<degree; ++i )
							cands |= candi[perm[i]].maybes.bits;
						if ( VSIZE[cands] == degree ) {
							// Naked Set found, but does it remove any maybes?
							// build bitset of indexes of naked set in region
							nkdSet = 0;
							for ( i=0; i<degree; ++i )
								// index-shift cells index in this-region-type
								nkdSet |= ISHFT[candi[perm[i]].indexIn[rti]];
							// Direct or normal Mode?
							if ( isDirect ) {
								// ----------------------------------------
								// Performance no issue here: GUI only!
								// ----------------------------------------
								// DIRECT: hint only if causes a Single
								final AHint hint = findSubsequentSingle(r, cands, nkdSet);
								if ( hint != null ) {
									result = true;
									if ( accu.add(hint) )
										return result;
								}
							} else {
								// NORMAL MODE: 96+% remove no maybes
								// foreach cell (sib) in the region except the-naked-set-cells
								for ( any=false,ia=INDEXES[VALL & ~nkdSet],nn=ia.length,i=0; i<nn; ++i )
									if ( ((sib=r.cells[ia[i]]).maybes.bits & cands) != 0 ) {
										redPots.put(sib, sib.maybes.intersectBits(cands));
										any = true;
									}
								if ( any ) {
									// ----------------------------------------
									// Performance isn't a problem here down
									// ----------------------------------------
									// find additional pointing/claiming elims.
									final List<Cell> nkdSetCells = r.atNewArrayList(nkdSet);
									final List<ARegion> regions;
									if ( r instanceof Grid.Box )
										regions = claimFromOtherCommonRegion(r, nkdSetCells, cands, redPots);
									else
										regions = Regions.list(r);
									// Build the hint
									final AHint hint = new NakedSetHint(
											  this
											, nkdSetCells
											, new Values(cands, degree, false)
											, new Pots(nkdSetCells, cands, false)
											, redPots.copyAndClear()
											, regions
									);
									result = true;
									if ( accu.add(hint) )
										return result;
								}
							}
						}
					} // next permutation
				}
			}
		}
		return result;
	}
	private final Pots redPots = new Pots(); // removable (red) Cell=>Values
	//</NO_WRAP>

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
	private List<ARegion> claimFromOtherCommonRegion(ARegion region
			, List<Cell> nkdSetCellList, int cands, Pots reds) {
		ARegion otherCR = Grid.otherCommonRegion(nkdSetCellList, region);
		if ( otherCR != null ) { // add any elims in the otherCR to reds
			final List<Cell> victims = otherCR.otherThan(nkdSetCellList);
			if ( !victims.isEmpty() && claimFrom(victims, cands, reds) )
				return Regions.list(region, otherCR);
		}
		return Regions.list(region);
	}

	/**
	 * Claim the values in nkdSetValsBits from the Cells in the victims List,
	 * adding any eliminations to the reds.
	 *
	 * @param victims
	 * @param cands
	 * @param reds
	 * @return
	 */
	private static boolean claimFrom(final List<Cell> victims, final int cands, final Pots reds) {
		boolean any = false;
		int redBits;
		for ( Cell c : victims )
			if ( (redBits=c.maybes.bits & cands) != 0 ) {
				// note that reds can't already contain these cells
				reds.put(c, new Values(redBits, true));
				any = true;
			}
		return any;
	}

	// If this NakedSet strips any victim "naked" (ie down to 1) then
	// create a Direct hint and return it; else null.
	private AHint findSubsequentSingle(ARegion r, int cands, int nkdSet) {
		assert tech.isDirect;
		Cell sib;  int redBits;
		// foreach cell in the region EXCEPT the cells in this naked set
		for ( int i : INDEXES[VALL & ~nkdSet] )
			// skip if sib doesn't have $degreePlus1 maybes
			if ( (sib=r.cells[i]).maybes.size == degreePlus1
			  // if sib has the nkdSetValues plus ONE other
			  && VSIZE[redBits=sib.maybes.bits & ~cands] == 1 )
				// then we can create the hint and add it to the accumulator
				return createNakedSetDirectHint(r
					, sib					// cellToSet
					, VFIRST[redBits]	// valueToSet
					, nkdSet				// nkdSetIdxBits
					, cands					// nkdSetValsBits
				);
		return null;
	}

	// NB: This method is called in DIRECT mode only (ie only in the GUI)
	// only when creating a hint, so performance isn't really and issue.
	private AHint createNakedSetDirectHint(ARegion r, Cell cellToSet
			, int valueToSet, int nkdSet, int cands) {
		assert tech.isDirect;
		// build removable (red) potentials
		Pots reds = new Pots(9-degree, 1F);
		// foreach cell in the region EXCEPT the naked set cells
		Cell sib;  int redBits; // bitset of values to remove from sib
		for ( int i : INDEXES[VALL & ~nkdSet] )
			// if sib maybe any of the naked set values
			if ( (redBits=(sib=r.cells[i]).maybes.bits & cands) != 0 )
				reds.put(sib, new Values(redBits, false));
		assert !reds.isEmpty();
		// claim the NakedSet values from the other common region (if any)
		List<Cell> ndkSetCells = r.atNewArrayList(nkdSet);
		ARegion ocr = Grid.otherCommonRegion(ndkSetCells, r);
		if ( ocr != null )
			claimFrom(ocr.otherThan(ndkSetCells), cands, reds);
		Pots oranges = new Pots();
		for ( Cell cell : ndkSetCells )
			oranges.put(cell, new Values(cell.maybes));
		// build the hint
		return new NakedSetDirectHint(this, cellToSet, valueToSet, ndkSetCells
			, new Values(cands, degree, false) // nkdSetValues
			, oranges, reds, r);
	}

}
