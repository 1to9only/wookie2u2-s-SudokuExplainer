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
import diuf.sudoku.Grid.Box;
import static diuf.sudoku.Grid.CELL_IDXS;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Implements the Sue De Coq Sudoku solving technique. SueDeCoq is in the ALS
 * package because I don't know where to put it. It's based on Almost Almost
 * Locked Sets (AALSs): 2 cells with 4 values, 3 cells with 5 values.
 * <p>
 * WARN: There's only a few SueDeCoq in top1465: rarer than rocking horse s__t,
 * apparently. I implemented SueDeCoq to understand its logic, but I can't, so
 * this implementation probably contains outrageous bugs. You have been warned.
 *
 * @author Keith Corlett 2021 Jan
 */
public class SueDeCoq extends AHinter {

	/** One entry in the recursion stack for the unit search */
	private static class StackEntry {
		/** The index of the cell that is currently tried */
		int index = 0;
		/** The indices of the cells in the current selection */
		Idx idx = new Idx();
		/** A bitset of the candidates in the current selection */
		int cands = 0;
//		// @check commented out: for debugging only
//		@Override
//		public String toString() {
//			return ""+index+", "+indices+", "+Values.toString(candidates);
//		}
	}

	/** All indices in the current row/col (only cells that are not set yet) */
	private final Idx lineIdx = new Idx();
	/** All indices in the current box (only cells that are not set yet) */
	private final Idx boxIdx = new Idx();
	/** All final indices in the current intersection (only cells that are not set yet) */
	private final Idx interIdx = new Idx();
	// the atmost 3 empty cells in the intersection between line and box
	private final Cell[] interCells = Cells.array(3);
	/** All indices in row/col that can hold additional cells (row/col - set cells - intersection) */
	private final Idx lineSrcIdx = new Idx();
	/** All indices in box that can hold additional cells (box - set cells - intersection) */
	private final Idx boxSrcIdx = new Idx();
	/** Stack for searching rows/cols */
	private final StackEntry[] stack1 = new StackEntry[9];
	/** Stack for searching boxs */
	private final StackEntry[] stack2 = new StackEntry[9];
	/** Cells of the current subset of the intersection */
	private final Idx interActIdx = new Idx();
	/** Candidates of all cells in {@link #interActIdx}. */
	private int interActCands = 0;
	/** Indices of the current additional cells in the row/col */
	private Idx lineActIdx;
	/** Candidates of all cells in {@link #lineActIdx}. */
	private int lineActCands;
	/** Indices of the current additional cells in the box */
	private Idx boxActIdx;
	/** Candidates of all cells in {@link #boxActIdx}. */
	private int boxActCands;
	/** For temporary calculations */
	private final Idx tmpIdx = new Idx();
	/** The removable (red) Cell=>Values. */
	private final Pots theReds = new Pots();

	public SueDeCoq() {
		super(Tech.SueDeCoq);
		for ( int i=0; i<9; ++i ) {
			stack1[i] = new StackEntry();
			stack2[i] = new StackEntry();
		}
	}

	// These are all set and cleaned-up by each findHints call.
	// the Grid to search
	private Grid grid;
	// the IAccumulator to which I add hints. If accu.add returns true then I
	// exit-early, abandoning the search for any subsequent hints
	private IAccumulator accu;
	// the current row/col that we're searching. This is only a field rather
	// than pass it all the way from top-of-stack right down to the bottom,
	// recursively, and with extra maple garter cheese and a cherry on top.
	private ARegion line;
	// the current box that we're searching... see above, minus silly bits.
	private Box box;

	// accu.isSingle(), just to save calling the method repeatedly.
	boolean onlyOne;

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// WARN: A single exit method, to clean-up the grid field.
		boolean result;
		this.grid = grid;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		try {
			if ( onlyOne )
				result = search(grid.rows) || search(grid.cols);
			else
				result = search(grid.rows) | search(grid.cols);
		} finally {
			// clean-up after myself
			this.grid = null;
			this.accu = null;
			this.line = null;
			this.box = null;
		}
		return result;
	}

	/**
	 * Searches each intersection of the given {@code lines} with the boxes,
	 * delegating the search to {@link #searchIntersection}.
	 *
	 * @param lines the array of rows or cols
	 * @return
	 */
	private boolean search(ARegion[] lines) {
		// examine each intersection between lines and boxs
		final Idx empties = grid.getEmpties();
		// presume that no hints will be found
		boolean result = false;
		// foreach row/col
		for ( ARegion line : lines ) {
			this.line = line;
			lineIdx.setAnd(line.idx, empties);
			// foreach intersecting box
			for ( Box box : line.intersectingBoxs ) {
				this.box = box;
				// get the intersection
				if ( interIdx.setAndMany(lineIdx, boxIdx.setAnd(box.idx, empties))
				  // search this intersection
				  && searchIntersection() ) {
					result = true;
					if ( onlyOne )
						return result;
				}
			}
		}
		return result;
	}

	/**
	 * Searches all possible combos of cells in the intersection. If a combo
	 * holds 2 more candidates than cells, an SDC could possibly exist.
	 * <p>
	 * This method doesn't use recursion because there can be only two or three
	 * cells in an intersection for an SDC.
	 *
	 * @return
	 */
	private boolean searchIntersection() {
		Cell c1, c2, c3;
		int nPlus, cands1, cands2, cands3;
		// read the interIdx into the interCells array
		final int n = interIdx.cellsN(grid, interCells);
		// presume that no hints will be found
		boolean result = false;
		// indices of the 3 cells in the intersection of line and box
		interActIdx.clear();
		for ( int i1=0,m=n-1; i1<m; ++i1 ) {
			// set the bitset of the first candidate value
			cands1 = (c1=interCells[i1]).maybes.bits;
			// add this cell to the intersection actual set
			interActIdx.add(c1.i);
			// now try the second cell
			for ( int i2=i1+1; i2<n; ++i2 ) {
				// build-up the bitset of two candidate values
				cands2 = cands1 | (c2=interCells[i2]).maybes.bits;
				// add this cell to the intersection actual set
				interActIdx.add(c2.i);
				// we have two cells in the intersection
				if ( (nPlus=VSIZE[cands2] - 2) > 1 ) {
					// possible SDC -> check
					// store the candidates of the current intersection
					interActCands = cands2;
					// check line cells except intersection cells
					if ( lineSrcIdx.setAndNot(lineIdx, interActIdx).any()
					  // now check all possible combos of cells in row/col
					  && checkLine(nPlus, lineSrcIdx, Values.ALL, true) ) {
						result = true;
						if ( onlyOne )
							return result;
					}
				}
				// and now the third cell, if any
				for ( int i3=i2+1; i3<n; ++i3 ) {
					cands3 = cands2 | (c3=interCells[i3]).maybes.bits;
					// now we have three cells in the intersection
					if ( (nPlus=VSIZE[cands3] - 3) > 1 ) {
						// possible SDC -> check
						// store the candidates of the current intersection
						interActCands = cands3;
						// add this cell to the intersection actual set
						interActIdx.add(c3.i);
						// check line cells except intersection cells
						if ( lineSrcIdx.setAndNot(lineIdx, interActIdx).any()
						  // now check all possible combos of cells in row/col
						  && checkLine(nPlus, lineSrcIdx, Values.ALL, true) ) {
							result = true;
							if ( onlyOne )
								return result;
						}
						interActIdx.remove(c3.i);
					}
				}
				interActIdx.remove(c2.i);
			}
			interActIdx.remove(c1.i);
		}
		return result;
	}

	/**
	 * Search all possible combinations of indices in {@code src}.
	 * <p>
	 * This method calls itself recursively ONCE, so there's two passes:<ul>
	 * <li>pass1 searches possible sets of cells from the row/col. A valid set
	 * contains candidates from the intersection and has at least one cell more
	 * than extra candidates (candidates not in the intersection), but leaves
	 * candidates in the intersection for the box search. If all criteria are
	 * met then this method calls itself recursively for pass2.
	 * <li>pass2 searches all possible sets of cells for the box. Each combo
	 * that meets the SueDeCoq (SDC) criteria is checked for eliminations.
	 * <li>Note that each pass has it's own stack, and it's own indices array.
	 * </ul>
	 *
	 * @param nPlus
	 * @param src
	 * @param okCands
	 * @param pass1
	 * @return
	 */
	private boolean checkLine(int nPlus, Idx src, int okCands, boolean pass1) {
		StackEntry p, c; // previous and current StackEntry
		int indice, cands, bothCands, anzExtra;
		final StackEntry[] stack;
		final int[] indices;
		if ( pass1 ) {
			stack = stack1;
			indices = indices1;
		} else { // pass2
			stack = stack2;
			indices = indices2;
		}
		// read src into an array to save 0.1 seconds in top1465 (wow).
		// Note that there's two arrays underneath this, one for each call.
		final int size = src.toArrayN(indices);
		final int sizeMinus1 = size - 1;
		final int sizeMinus2 = size - 2;
		// level 0 is just a stopper, we start with level 1
		int level = 1;
		// presume that no hint/s will be found.
		boolean result = false;
		p = stack[0]; // previous StackEntry
		p.index = -1; // before first
		p.cands = 0;
		p.idx.clear();
		// get the first cell from src (there must be at least 1!)
		c = stack[1]; // current StackEntry
		c.index = -1; // before first
		// check all possible combinations of cells
		for(;;) {
			// fallback all levels where we're out of indices
			while ( stack[level].index > sizeMinus2 )
				if ( --level < 1 )
					return result; // ok, done (level 0 is just a stopper)
			// ok, calculate next try
			p = stack[level - 1];
			c = stack[level];
			// current cells = previous cells + this cell
			c.idx.setOr(p.idx, CELL_IDXS[indice=indices[++c.index]]);
			// current cands = previous cands + this cells maybes
			c.cands = p.cands | grid.cells[indice].maybes.bits;
			// the current cell combo must eliminate at least one candidate in
			// the current intersection or we dont have to look further.
			// the cells must not contain candidates that are not in okCands;
			// In pass1 okCands is all values, in pass2 it's boxOkCands.
			if ( (c.cands & ~okCands) == 0
			  // we need some candidates in the intersection
			  && (c.cands & interActCands) != 0 ) {
				// number of candidates not drawn from the intersection
				anzExtra = VSIZE[cands=c.cands & ~interActCands];
				// Here we differentiate between the first and second pass:
				// * In round1 we can switch over to the box if there are still
				//   candidates (and cells in the box) left.
				// * In round2 the number of candiates drawn from the
				//   intersection must equal nPlus.
				if ( pass1 ) {
					// level equals the number of current cells in the row/col
					if ( level>anzExtra && level-anzExtra<nPlus ) {
						// The combination of cells contains candidates from da
						// intersection, it has at least one cell more than the
						// number of additional candidates (so it eliminates at
						// least one cell from the intersection) and there are
						// uncovered candidates left in the intersection ->
						// switch over to the box
						// memorize current selection for second run
						lineActIdx = c.idx;
						lineActCands = c.cands;
						// exclude all cells that are already used in the line
						if ( boxSrcIdx.setAndNot(boxIdx, interActIdx, lineActIdx).any()
						  // candidates from line are not allowed anymore
						  // nb: Values.ALL drops extranious high-bits
						  // nb: cands is c.cands not in the intersection
						  // and now pass2 (ONE recursive call)
						  && checkLine(nPlus-(lineActIdx.size()-anzExtra)
									, boxSrcIdx
									, Values.ALL & ~(lineActCands & ~cands)
									, false) ) {
							result = true;
							if ( onlyOne )
								return result;
						}
					}
				// pass2: number of candidates has to be exactly nPlus
				} else if ( c.idx.size()-anzExtra == nPlus ) {
					// It's a Sue de Coq, but are the any eliminations?
					// get current data for box
					boxActIdx = c.idx;
					boxActCands = c.cands;
					// get the extra candidates that are in both line and box
					bothCands = boxActCands & lineActCands;
					// all cells in the box that dont belong to the SDC
					tmpIdx.setAndNot(boxIdx, boxActIdx, interActIdx);
					// all candidates that can be eliminated in the box
					// (including extra candidates contained in both sets)
					cands = ((interActCands | boxActCands) & ~lineActCands) | bothCands;
					eliminate(tmpIdx, cands, theReds);
					// now the row/col
					tmpIdx.setAndNot(lineIdx, lineActIdx, interActIdx);
					// all candidates that can be eliminated in the row/col
					// (including extra candidates contained in both sets)
					cands = ((interActCands | lineActCands) & ~boxActCands) | bothCands;
					eliminate(tmpIdx, cands, theReds);
					if ( theReds.size() > 0 ) {
						// FOUND a Sue De Coq!
						Pots reds = new Pots(theReds);
						theReds.clear();
						// intersection is written into indices and values
						Pots greens = new Pots(grid, interActIdx, interActCands);
						// all candidates that occur in the intersection
						// and in the row/col become fins (for display)
						Pots blues = potify(lineActIdx, interActIdx, lineActCands, new Pots());
						// all candidates that occur in the intersection
						// and in the box become endo fins (for display)
						Pots purples = potify(boxActIdx, interActIdx, boxActCands, new Pots());
						// create the hint and add it to the IAccumulator
						AHint hint = new SueDeCoqHint(this, reds, greens, blues
								, purples, line, box);
						result = true;
						if ( accu.add(hint) )
							return result;
					}
				}
			}
			// on to the next level (if any)
			if ( stack[level].index < sizeMinus1 )
				// ok, go to next level
				stack[++level].index = stack[level - 1].index;
		}
	}
	final int[] indices1 = new int[6];
	final int[] indices2 = new int[6];

	/**
	 * If one of the cells in {@code idx} contains {@code cands} they can
	 * be eliminated.
	 *
	 * @param idx
	 * @param cands
	 */
	private void eliminate(Idx idx, int cands, Pots pots) {
		if ( VSIZE[cands]>0 && idx.size()>0 )
			idx.forEach(grid.cells, (c) -> {
				final int elims = c.maybes.bits & cands;
				if ( elims != 0 )
					for ( int v : VALUESES[elims] )
						pots.upsert(c, v);
			});
	}

	/**
	 * {@code pots} += {@code cands} that are in either {@code a} or {@code b}.
	 * <p>
	 * Convenience method: Some candidates are written as fins/endo fins for
	 * display purposes.
	 *
	 * @param a
	 * @param b
	 * @param cands
	 * @param dest
	 */
	private Pots potify(Idx a, Idx b, int cands, Pots pots) {
		tmpIdx.setOr(a, b).forEach(grid.cells, (c) -> {
			int elims = c.maybes.bits & cands;
			if ( elims != 0 )
				for ( int v : VALUESES[elims] )
					pots.upsert(c, v);
		});
		return pots;
	}

}
