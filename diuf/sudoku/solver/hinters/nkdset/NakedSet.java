/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.chain.ChainerHacu;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.utils.MyArrays;
import java.util.Arrays;

/**
 * NakedSet implements the NakedPair, NakedTriple, NakedQuad, or NakedPent
 * Sudoku solving technique, depending on the Tech passed to my constructor.
 * <p>
 * Note: NakedSet can find larger sets, but NakedPent=5 and up are degenerate;
 * so Tech.NakedHex and up do NOT exist and are untested.
 */
public class NakedSet extends AHinter
		implements diuf.sudoku.solver.hinters.ICleanUp
{
	/**
	 * Claim each value in the cands bitset from each Cell in victims,
	 * adding any eliminations to the reds.
	 *
	 * @param victims to claim from
	 * @param nv numVictims
	 * @param cands to claim
	 * @param reds to add the claims to
	 * @return any
	 */
	protected static boolean claimFrom(final Cell[] victims, final int nv
			, final int cands, final Pots reds) {
		int pinkos;
		boolean any = false;
		for ( int i=0; i<nv; ++i )
			if ( (pinkos=victims[i].maybes & cands) != 0 )
				any |= reds.put(victims[i], pinkos) == null;
		return any;
	}

	// the Permutations Array
	private final int[] thePA;
	// the array of candidate cells
	private final Cell[] cells = new Cell[REGION_SIZE];
	// the maybes of the candidate cells
	private final int[] maybes = new int[REGION_SIZE];
	// removable (red) potentials
	private final Pots reds = new Pots();

	public NakedSet(final Tech tech) {
		super(tech);
		assert this instanceof NakedSetDirect || !tech.isDirect;
		assert degree>=2 && degree<=5; // Pair, Triple, Quad, Pent
		this.thePA = new int[degree]; // the Permutations Array
	}

	@Override
	public void cleanUp() {
		MyArrays.clear(cells);
	}

	//<NO_WRAP comment="wrapping makes this code LESS readable, IMHO">
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
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		Cell[] rcs; // region.cells array
		int[] ia; // indexes of cells in this region EXCEPT naked set cells
		int i // the ubiquitous index
		      // hijacked as cardinality: cell.size
		  , n // number of candidate cells in this region
		  , rti // region.typeIndex
		  , cands // bitset of combined maybes of cells in this naked set
		  , indexes // indexes in this region of cells in this naked set
		  , nn // ia.length: number of cells in region EXCEPT nkdSetCells
		  , x // ia[i]
		  , pinkos; // candidates to remove
		// local stack references to heap attributes, for speed
		final Cell[] cells = this.cells; // candidate cells
		final int[] maybes = this.maybes; // candidate cells
		final Pots reds = this.reds;
		final int[] thePA = this.thePA;
		final int degreePlus1 = this.degreePlus1;
		final int degree = this.degree;
		final int degreeMinus1 = this.degreeMinus1;
		// presume that no eliminations will be found
		boolean any = false;
		// presume that no hint will be found
		boolean result = false;
		// chainer never uses direct mode
		assert !tech.isDirect || !(accu instanceof ChainerHacu);
		// foreach region in the grid // 9*box, 9*row, 9*col
		for ( ARegion region : grid.regions ) {
			// if this region has an "extra" empty cell to remove maybes from
			if ( region.emptyCellCount > degree ) {
				// candidate cells := r.cells with 2..degree maybes
				n = 0; // number of candidate cells
				for ( Cell c : region.cells )
					if ( (i=c.size)>1 && i<degreePlus1 ) {
						maybes[n] = c.maybes;
						cells[n++] = c;
					}
				// need 2 cells for Pair, 3 for Triple, 4 for Quad, 5 for Pent,
				// but Pent's are degenerate (so they exist but are not used).
				if ( n > degreeMinus1 ) {
					rti = region.typeIndex;
					// foreach combo of degree cells among the candidates
					for ( int[] perm : new Permutations(n, thePA) ) {
						// are there degree maybes in this degree cells?
						// build a bitset of the maybes of this combo of cells
						cands = maybes[perm[0]] | maybes[perm[1]];
						for ( i=2; i<degree; ++i )
							cands |= maybes[perm[i]];
						if ( VSIZE[cands] == degree ) {
							// Naked Set found, but does it remove any maybes?
							// build a bitset of indexes-in-this-region of the
							// cells in the naked set
							indexes = ISHFT[cells[perm[0]].indexIn[rti]]
									| ISHFT[cells[perm[1]].indexIn[rti]];
							for ( i=2; i<degree; ++i )
								indexes |= ISHFT[cells[perm[i]].indexIn[rti]];
							// INDIRECT: 96+% remove no maybes
							// foreach cell in region except the naked set
							rcs = region.cells;
							for ( ia=INDEXES[VALL & ~indexes],nn=ia.length,i=0;
									i<nn; ++i )
								// nb: I tried caching, but it's slower.
								if ( (pinkos=rcs[x=ia[i]].maybes & cands) != 0 ) {
									reds.put(rcs[x], pinkos);
									any = true;
								}
							if ( any ) {
								// ----------------------------------------
								// Performance isn't a problem here down
								// ----------------------------------------
								// nb: NakedSetDirect overrides createHint
								final AHint hint = createHint(region, cands,
										indexes, reds);
								if ( hint != null ) {
									result = true;
									if ( accu.add(hint) ) {
										return result;
									}
								}
								any = false;
							}
						}
					} // next permutation
				}
			}
		}
		return result;
	}
	//</NO_WRAP>

	/**
	 * Create the hint.
	 * <p>
	 * Note NakedSetDirect overrides createHint to do it's thing.
	 *
	 * @param region the region we're searching
	 * @param cands the values of the hidden set
	 * @param indexes in region.cells of the cells in this hidden set
	 * @param reds removable (red) potentials
	 * @return a new NakedSetHint as a "base" AHint
	 */
	protected AHint createHint(final ARegion region, final int cands
			, final int indexes, final Pots reds) {
		final Cell[] nsCells = region.atNew(indexes);
		final ARegion[] regions;
		if ( degree<4 && region instanceof Box )
			// find any extra Pointing elims in NakedPair/Triple but not Quad+
			regions = doOtherCommonRegion(region, nsCells, degree, cands, reds);
		else
			regions = Regions.array(region);
		// Build the hint
		return new NakedSetHint(this, nsCells, cands
				, new Pots(nsCells, degree, cands, false)
				, reds.copyAndClear(), regions);
	}

	// doOtherCommonRegion finds extra Pointing eliminations in the OCR.
	// These cells are all in the same box, so if they all share another common
	// region (OCR), a row/col, then eliminate cands from the OCR, ie pointing.
	// If doOcr is omitted then the subsequent Locking run makes these elims,
	// it's just nicer to see them all in one hint, that's all.
	// doOcr runs only if the region is a Box to find any extra pointing elims
	// for the Box=>Row/Col NakedSet hint; and then the Row/Col=>Box doesnt (I
	// think) need to bother claiming. Find a disproving use-case and I'll code
	// for it, but Box (pointing) only til then.
	private static ARegion[] doOtherCommonRegion(final ARegion box
			, final Cell[] nsCells, final int nsSize, final int cands
			, final Pots reds) {
		final ARegion ocr = Regions.otherCommon(nsCells, box);
		if ( ocr != null ) {
			final Cell[] victims = Cells.arrayA(REGION_SIZE);
			final int nv = ocr.otherThan(nsCells, nsSize, victims);
			if ( nv>0 && claimFrom(victims, nv, cands, reds) ) {
				Arrays.fill(victims, null);
				return Regions.array(box, ocr);
			}
		}
		return Regions.array(box);
	}

}
