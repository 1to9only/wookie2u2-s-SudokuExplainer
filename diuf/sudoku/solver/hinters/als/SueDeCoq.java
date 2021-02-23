/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
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
 * Implements the Sue De Coq Sudoku solving technique. Sue De Coq is lumped-in
 * with ALS's because I don't know where it belongs, and it's basis is almost
 * almost locked sets: 2 cells with 4 values, 3 cells with 5 values.
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
// @check commented out: for debugging only
//		@Override
//		public String toString() {
//			return ""+index+", "+indices+", "+Values.toString(candidates);
//		}
	}

	/** All indices in the current row/col (only cells that are not set yet) */
	private final Idx lineSet = new Idx();
	/** All indices in the current box (only cells that are not set yet) */
	private final Idx boxSet = new Idx();
	/** All final indices in the current intersection (only cells that are not set yet) */
	private final Idx interSet = new Idx();
	/** All indices in row/col that can hold additional cells (row/col - set cells - intersection) */
	private final Idx lineSrcSet = new Idx();
	/** All indices in box that can hold additional cells (box - set cells - intersection) */
	private final Idx boxSrcSet = new Idx();
	/** Stack for searching rows/cols */
	private final StackEntry[] stack1 = new StackEntry[9];
	/** Stack for searching boxs */
	private final StackEntry[] stack2 = new StackEntry[9];
	/** Cells of the current subset of the intersection */
	private final Idx interActSet = new Idx();
	/** Candidates of all cells in {@link #interActSet}. */
	private int interActCands = 0;
	/** Indices of the current additional cells in the row/col */
	private Idx lineActSet = new Idx();
	/** Candidates of all cells in {@link #lineActSet}. */
	private int lineActCands = 0;
	/** Valid candidates for box */
	private int boxOkCands = 0;
	/** Indices of the current additional cells in the box */
	private Idx boxActSet = new Idx();
	/** Candidates of all cells in {@link #boxActSet}. */
	private int boxActCands = 0;
	/** For temporary calculations */
	private final Idx tmpSet = new Idx();
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
	// the grid.cells
	private Cell[] cells;
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
		this.cells = grid.cells;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		try {
			if ( onlyOne )
				result = findHintsInt(grid.rows)
					  || findHintsInt(grid.cols);
			else
				result = findHintsInt(grid.rows)
					   | findHintsInt(grid.cols);
		} finally {
			// clean-up after myself
			this.grid = null;
			this.cells = null;
			this.accu = null;
			this.line = null;
			this.box = null;
		}
		return result;
	}

	/**
	 * Searches each intersection of the given {@code lines} with the boxes,
	 * delegating the search to {@link #checkIntersection(boolean)}.
	 *
	 * @param lines the array of rows or cols
	 * @return
	 */
	private boolean findHintsInt(ARegion[] lines) {
		// get all possible intersections between rows/cols and boxs
		Idx empties = grid.getEmptyCells();
		boolean result = false;
		// foreach row/col
		for ( ARegion line : lines ) {
			this.line = line;
			lineSet.setAnd(line.idx, empties);
			// foreach intersecting box
			for ( Box box : line.intersectingBoxs ) {
				this.box = box;
				// get the intersection
				if ( interSet.setAndAny(lineSet, boxSet.setAnd(box.idx, empties))
				  && interSet.size() > 1
				  // check the intersection
				  && checkIntersection() ) {
					result = true;
					if ( onlyOne )
						return true;
				}
			}
		}
		return result;
	}

	/**
	 * Checks all possible combos of cells in the intersection. If a combo
	 * holds 2 more candidates than cells, a SDC could possibly exist.
	 * <p>
	 * The method doesn't use recursion. There can be only two or three cells
	 * in an intersection for an SDC.
	 *
	 * @param onlyOne
	 * @return
	 */
	private boolean checkIntersection() {
		final int max = interSet.size();
		int nPlus;
		boolean result = false;
		interActSet.clear();
		for (int i1 = 0; i1 < max - 1; i1++) {
			// all candidates of the first cell
			int index1 = interSet.get(i1);
			interActSet.add(index1);
			int cands1 = cells[index1].maybes.bits;
			// now try the second cell
			for (int i2 = i1 + 1; i2 < max; i2++) {
				int index2 = interSet.get(i2);
				int cands2 = cands1 | cells[index2].maybes.bits;
				interActSet.add(index2);
				// we have two cells in the intersection
				if ( (nPlus=VSIZE[cands2] - 2) > 1 ) {
					// possible SDC -> check
					if ( checkHouses(nPlus, cands2) ) {
						result = true;
						if ( onlyOne )
							return result;
					}
				}
				// and the third cell
				for (int i3 = i2 + 1; i3 < max; i3++) {
					int index3 = interSet.get(i3);
					int cand3 = cands2 | cells[index3].maybes.bits;
					// now we have three cells in the intersection
					if ( (nPlus=VSIZE[cand3] - 3) > 1 ) {
						// possible SDC -> check
						interActSet.add(index3);
						if ( checkHouses(nPlus, cand3) ) {
							result = true;
							if ( onlyOne )
								return result;
						}
						interActSet.remove(index3);
					}
				}
				interActSet.remove(index2);
			}
			interActSet.remove(index1);
		}
		return result;
	}

	/**
	 * Builds a set with all cells in the row/col that are not part of the
	 * intersection and delegates the check to
	 * {@link #checkHouses(int, sudoku.Idx, int, int, boolean, boolean) }.
	 * @param nPlus How many more candidates than cells
	 * @param cand Candidates in the intersection
	 * @param onlyOne
	 * @return
	 */
	private boolean checkHouses(int nPlus, int cand) {
		// store the candidates of the current intersection
		interActCands = cand;
		// check lines: all cells not used in the intersection are valid
		lineSrcSet.set(lineSet);
		lineSrcSet.andNot(interActSet);
		// now check all possible combinations of cells in row/col
		return checkLine(nPlus, lineSrcSet, Values.ALL_BITS, true);
	}

	/**
	 * Does a non recursive search: All possible combinations of indices in
	 * {@code srcSet} are tried.
	 * <p>
	 * This method is called twice:<ul>
	 * <li>Round 1 searches possible sets of cells from the row/col. A set is
	 * valid if it contains candidates from the intersection, has at least one
	 * cell more than extra candidates (candidates not in the intersection) but
	 * leaves candidates in the intersection for the box search. If all those
	 * criteria are met, the method is called recursively for the second run.
	 * <li>Round 2 searches all possible sets of cells for the box. Each combo
	 * that meets the SDC criteria is checked for eliminations.
	 * </ul>
	 *
	 * @param nPlus
	 * @param src
	 * @param okCands
	 * @param pass1
	 * @return
	 */
	private boolean checkLine(int nPlus, Idx src, int okCands, boolean pass1) {
		if ( src.isEmpty() )
			return false; // nothing to do!
		StackEntry p, c; // previous and current StackEntry
		int indice, tmpCands, bothActCands, anzExtra;
		final StackEntry[] stack; if(pass1) stack=stack1; else stack=stack2;
		final int max = src.size();
		final int maxMinus1 = max - 1;
		final int maxMinus2 = max - 2;
		// level 0 is only a marker, we start with level 1
		int level = 1;
		// presume that no hint/s will be found.
		boolean result = false;
		p = stack[0]; // previous StackEntry
		p.index = -1;
		p.cands = 0;
		p.idx.clear();
		// get the first cell from sourceSet (there must be at least 1!)
		c = stack[1]; // current StackEntry
		c.index = -1;
		// check all possible combinations of cells
		for(;;) {
			// fall back all levels where nothing can be done anymore
			while ( stack[level].index > maxMinus2 )
				if ( --level < 1 )
					return result; // ok, done
			// ok, calculate next try
			p = stack[level - 1];
			c = stack[level];
			c.index++;
			c.idx.set(p.idx);
			c.idx.add(indice=src.get(c.index));
			c.cands = p.cands | cells[indice].maybes.bits;
			// the current cell combo must eliminate at least one candidate in
			// the current intersection or we dont have to look further.
			// the cells must not contain candidates that are not in okCands;
			// In round1 okCands is all values, in round2 it's boxOkCands.
			if ( (c.cands & ~okCands) == 0
			  // we need some candidates in the intersection
			  && VSIZE[c.cands & interActCands] > 0 ) {
				// number of candidates not drawn from the intersection
				anzExtra = VSIZE[tmpCands=c.cands & ~interActCands];
				// Here we differentiate between the first and second pass:
				// * In round1 we can switch over to the box if there are still
				//   candidates (and cells in the box) left.
				// * In round2 the number of candiates drawn from the
				//   intersection must equal nPlus.
				if ( pass1 ) {
					// level equals the number of current cells in the row/col
					if ( level>anzExtra && level-anzExtra<nPlus ) {
						// The combination of cells contains candidates from the
						// intersection, it has at least one cell more than the
						// number of additional candidates (so it eliminates at
						// least one cell from the intersection) and there are
						// uncovered candidates left in the intersection ->
						// switch over to the box
						// memorize current selection for second run
						lineActSet = c.idx;
						lineActCands = c.cands;
						boxSrcSet.set(boxSet);
						boxSrcSet.andNot(interActSet);
						// exclude all cells that are already used in row/col
						boxSrcSet.andNot(lineActSet);
						// candidates from row/col set are not allowed anymore
						// nb: tmpCands is num cands not in the intersection
						// nb: & Values.ALL_BITS chops-off extranious high-bits
						boxOkCands = ~(lineActCands & ~tmpCands) & Values.ALL_BITS;
						// and the second run
						if ( result |= checkLine(
								  nPlus - (lineActSet.size() - anzExtra )
								, boxSrcSet, boxOkCands, false)
						  && onlyOne )
							return result;
					}
				// pass2: number of candidates has to be exactly nPlus
				} else if ( c.idx.size()-anzExtra == nPlus ) {
					// It's a Sue de Coq! Can anything be eliminated?
					// possible eliminations (special case "same extra cand
					// in box and line" is not included below):
					//  - (interActCands + boxActCands) - lineActCands
					//    in boxSet - boxActSet - interActSet - lineActSet
					//  - (interActCands + lineActCands) - boxActCands in
					//    lineSet - lineActSet - interActSet - boxActSet
					// If both sets hold the same extra candidates they can
					// be eliminated from both sets!
					// get current data for box
					boxActSet = c.idx;
					boxActCands = c.cands;
					// get the extra candidates that are in both sets
					bothActCands = boxActCands & lineActCands;
					// all cells in the box that dont belong to the SDC
					tmpSet.set(boxSet);
					tmpSet.andNot(boxActSet);
					tmpSet.andNot(interActSet);
					// all candidates that can be eliminated in the box
					// (including extra candidates contained in both sets)
					tmpCands = ((interActCands | boxActCands) & ~lineActCands) | bothActCands;
					eliminate(tmpSet, tmpCands, theReds);
					// now the row/col
					tmpSet.set(lineSet);
					tmpSet.andNot(lineActSet);
					tmpSet.andNot(interActSet);
					// all candidates that can be eliminated in the row/col
					// (including extra candidates contained in both sets)
					tmpCands = ((interActCands | lineActCands) & ~boxActCands) | bothActCands;
					eliminate(tmpSet, tmpCands, theReds);
					if ( theReds.size() > 0 ) {
						// FOUND a Sue De Coq!
						Pots reds = new Pots(theReds);
						theReds.clear();
						// intersection is written into indices and values
						Pots greens = new Pots(grid, interActSet, interActCands);
						// all candidates that occur in the intersection
						// and in the row/col become fins (for display)
						Pots blues = potify(lineActSet, interActSet, lineActCands, new Pots());
						// all candidates that occur in the intersection
						// and in the box become endo fins (for display)
						Pots purples = potify(boxActSet, interActSet, boxActCands, new Pots());
// I don't know what to do with these as my hint has not concept of an ALS.
//							globalStep.addAls(interActSet, interActCands);
//							globalStep.addAls(boxActSet, boxActCands);
//							globalStep.addAls(lineActSet, lineActCands);
						AHint hint = new SueDeCoqHint(this, reds, greens
								, blues, purples, line, box);
						result = true;
						if ( accu.add(hint) )
							return result;
					}
				}
			}
			// on to the next level (if that is possible)
			if ( stack[level].index < maxMinus1 )
				// ok, go to next level
				stack[++level].index = stack[level - 1].index;
		}
	}

	/**
	 * If one of the cells in {@code idx} contains {@code cands} they can
	 * be eliminated.
	 *
	 * @param idx
	 * @param cands
	 */
	private void eliminate(Idx idx, int cands, Pots pots) {
		if ( VSIZE[cands]>0 && idx.size()>0 )
			idx.forEach1((index) -> {
				int elims = cells[index].maybes.bits & cands;
				if ( elims != 0 )
					for ( int cand : VALUESES[elims] )
						pots.upsert(cells[index], cand);
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
		tmpSet.setOr(a, b).forEach1((index) -> {
			for ( int v : VALUESES[cells[index].maybes.bits & cands] )
				pots.upsert(cells[index], v);
		});
		return pots;
	}

}
