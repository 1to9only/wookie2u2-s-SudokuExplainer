/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKu's MiscellaneousSolver, by Bernhard Hobiger. It
 * is in the ALS package because I don't know where-else to put it. It's NOT an
 * ALS-hinter, so it doesn't extend AAlsHinter. Kudos to hobiwan. Mistakes are
 * mine. KRC.
 *
 * Here's hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
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
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Implements the Sue De Coq Sudoku solving technique. Sue De Coq is based on
 * Almost Almost Locked Sets (AALSs): 2 cells with 4 values, or 3 cells with 5
 * values.
 * <p>
 * WARN: There's only a few SueDeCoq in top1465: rarer than rocking horse s__t,
 * apparently. I implemented SueDeCoq to understand its logic, but I can't, so
 * this implementation probably contains outrageous bugs. You have been warned.
 *
 * @author Keith Corlett 2021 Jan
 */
public class SueDeCoq extends AHinter {

	/** An entry in the recursion stack for the region search */
	private static class StackEntry {
		/** The indice of the cell that is currently tried. */
		int indice = 0;
		/** The indices of the cells in the current set. The instance is final,
		 * but it's indices (contents) mutate. */
		final Idx idx = new Idx();
		/** A bitset (Values.bits) of the maybes in the current set. */
		int cands = 0;
		@Override
		public String toString() {
			return "indice="+indice+", idx="+idx+", cands="+Values.toString(cands);
		}
	}

	/** All indices in the current row-or-col (empty cells only). */
	private final Idx lineAllIdx = new Idx();
	/** All indices in the current box (empty cells only). */
	private final Idx boxAllIdx = new Idx();
	/** All final indices in the current intersection (empty cells only). */
	private final Idx interAllIdx = new Idx();
	/** The 2 or 3 empty cells in this intersection between line and box. */
	private final Cell[] interCells = Cells.arrayA(3);
	/** All indices only in line: line empties - intersection. */
	private final Idx lineOnlyIdx = new Idx();
	/** All indices only in box: box empties - intersection. */
	private final Idx boxOnlyIdx = new Idx();
	/** Stack for searching rows/cols. */
	private final StackEntry[] lineStack = new StackEntry[9];
	/** Stack for searching boxs. */
	private final StackEntry[] boxStack = new StackEntry[9];
	/** Cells of the current subset of the intersection. */
	private final Idx interActIdx = new Idx();
	/** Candidates of all cells in {@link #interActIdx}. */
	private int interActCands = 0;
	/** Indices of the current additional cells in the row/col. */
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
	/** The indices of cells in this line. */
	final int[] lineIndices = new int[6];
	/** The indices of cells in this box. */
	final int[] boxIndices = new int[6];

	public SueDeCoq() {
		super(Tech.SueDeCoq);
		for ( int i=0; i<9; ++i )
			lineStack[i] = new StackEntry();
		for ( int i=0; i<9; ++i )
			boxStack[i] = new StackEntry();
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
			Cells.cleanCasA();
		}
		return result;
	}

	/**
	 * Searches each intersection of the given {@code lines} with the boxes,
	 * delegating the search to {@link #searchInter}.
	 *
	 * @param lines array of regions to examine: grid.rows or grid.cols
	 * @return
	 */
	private boolean search(ARegion[] lines) {
		// examine each intersection between lines and boxs
		// get the empty cells in this grid (cached)
		final Idx empties = grid.getEmpties();
		// presume that no hints will be found
		boolean result = false;
		// foreach row/col
		for ( ARegion l : lines )
			// nb: in SueDeCoq each line has 0 or >= 2 empty cells, because
			// HiddenSingle is mandatory, and it knocks-off the 1's, so that
			// setAndAny is equivalent to setAndMany, it's just a bit faster.
			// nb: we start with "All" sets of the empty cells in the regions,
			// which searchInter then reduces down to "Act" sets of cells in
			// the "actual" current sets. Clear?
			if ( lineAllIdx.setAndAny((line=l).idx, empties) )
				// foreach intersecting box (each box this row/col crosses)
				for ( Box b : l.intersectingBoxs )
					// get the intersecting cells: "Many" means atleast two
					if ( interAllIdx.setAndMany(lineAllIdx, boxAllIdx.setAnd((box=b).idx, empties))
					  // and search the intersecting cells (2 or 3 of them)
					  && searchInter()
					) {
						// we found a SueDeCoq (don't hold your breath)
						result = true;
						if ( onlyOne )
							return result;
					}
		return result;
	}

	/**
	 * searchInter[section] searches all combos of cells in the intersection of
	 * a line and a box. If a combo holds atleast 2 more candidates than cells
	 * then it's a possible AALS, hence a possible SueDeCoq.
	 * <p>
	 * the line and box fields are preset;<br>
	 * as are the lineIdx, boxIdx, and the interIdx fields.
	 * <p>
	 * This method doesn't use recursion because there can be only two or three
	 * cells in an intersection, so it's possible to just iterate them, and
	 * iteration is (usually) faster than recursion.
	 *
	 * @return
	 */
	private boolean searchInter() {
		Cell c1, c2, c3;
		int cands1, cands2, cands3;
		// read the interIdx into the interCells array
		final int n = interAllIdx.cellsN(grid, interCells);
		// presume that no hints will be found
		boolean result = false;
		// indices of the 2 or 3 empty cells in the intersection of line & box
		// nb: this set should always be empty before clear. Paranoia rulz!
		interActIdx.clear();
		// build a bitset of maybes in boxAllIdx (empties in current box).
		// Source boxAllIdx is set before searchInter is called, so boxAllCands
		// is set ONCE here, for efficient repeated reading down in checkLine.
		boxAllCands = boxAllCands();
		// first we get the first cell
		for ( int i1=0,m=n-1; i1<m; ++i1 ) {
			// set the bitset of the first candidate value
			cands1 = (c1=interCells[i1]).maybes;
			// add this cell to the intersection actual set
			interActIdx.add(c1.i);
			// now we get the second cell
			for ( int i2=i1+1; i2<n; ++i2 ) {
				// add this cell to the intersection actual set
				// note: do before if coz the threes use him too! sigh.
				interActIdx.add((c2=interCells[i2]).i);
				// build-up the bitset of two candidate values
				// we have two cells in the intersection, so they need atleast
				// four candidates between them in order to form an AALS
				if ( VSIZE[cands2 = cands1 | c2.maybes] > 3 ) {
					// possible SDC -> check
					// store the candidates of the current intersection
					interActCands = cands2;
					// check line cells except intersection cells
					if ( lineOnlyIdx.setAndNotAny(lineAllIdx, interActIdx)
					  // now check all possible combos of cells in row/col
					  // boolean check(int nPlus, Idx src, int okCands, boolean pass1)
					  && checkLine(VSIZE[cands2]-2) ) {
						result = true;
						if ( onlyOne )
							return result;
					}
				}
				// and now we get the third cell, if any
				for ( int i3=i2+1; i3<n; ++i3 ) {
					// build-up the bitset of three candidate values
					// now we have three cells in the intersection, so they
					// need atleast five candidates in order to form an AALS
					if ( VSIZE[cands3 = cands2 | (c3=interCells[i3]).maybes] > 4 ) {
						// possible SDC -> check
						// store the candidates of the current intersection
						interActCands = cands3;
						// add this cell to the intersection actual set
						interActIdx.add(c3.i);
						// check line cells except intersection cells
						if ( lineOnlyIdx.setAndNotAny(lineAllIdx, interActIdx)
						  // now check all possible combos of cells in row/col
						  // boolean check(int nPlus, Idx src, int okCands, boolean pass1)
						  && checkLine(VSIZE[cands3]-3) ) {
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
	private int boxAllCands;

	/**
	 * Returns the candidates in boxAllIdx.
	 *
	 * @return a bitset of the combined maybes of cells in boxAllIdx
	 */
	private int boxAllCands() {
		final Cell[] cells = grid.cells;
		int values = 0;
		for ( int i : boxAllIdx.toArrayB() )
			values |= cells[i].maybes;
		return values;
	}

	/**
	 * checkLine checks each possible combination of cells in the current line,
	 * by checking each possible combination of box-only-cells.
	 * <pre> We search each possible combination of cells in the line-only.
	 * A valid set contains candidates from the intersection
	 * and has at least one cell more than extra candidates (candidates not in
	 *                                                       the intersection)
	 * and leaves candidates in the intersection for the box.
	 * If all criteria are met then I call checkBox.
	 *
	 * <b>param nPlus</b> is the number of whatsits in the thingymajig.
	 * I'm sorry, I've been struggling to reverse-engineer the meaning of nPlus
	 * from the code, so WARNING any/all of this may well be WRONG!
	 *
	 * I think it's the maximum (in checkLine) number of candidate values,
	 * which I think is: boxAllCands except intersectionCands.
	 * If boxAllCands is covered-by intersection-cands then
	 * process these box-cells.
	 *
	 * Hobiwan is a genius, a confusing bloody genius, who didn't even attempt
	 * to document it, because it's too bloody complicated; but a genius none
	 * the less.{@code trouble = iq * rush}. BIG Sigh.
	 * </pre>
	 *
	 * @param nPlus BFIIK. See above. I reckon: number of "extra" candidates in
	 *  the cells in this intersection between line and box:{@code numCands - numCells}
	 * @return
	 */
	private boolean checkLine(int nPlus) {
		StackEntry p, c; // previous and current StackEntry
		int indice, lineOnlyCands, anzExtra;
		// stack references to heap fields, for speed, I hope
		final Cell[] cells = grid.cells;
		final Idx srcIdx = this.lineOnlyIdx;
		final StackEntry[] stack = lineStack; // the line stack
		final int[] indices = lineIndices; // the line indices array
		// read src into an array to save 0.1 seconds in top1465 (wow).
		final int n = srcIdx.toArrayN(indices);
		final int nMinus1 = n - 1;
		final int nMinus2 = n - 2;
		// level 0 is just a stopper, we start with level 1
		int level = 1;
		// presume that no hint/s will be found.
		boolean result = false;
		// a stack of cells to represent the current combination of cells in
		// the intersection of line (row/col) and box that we're searching.
		p = stack[0]; // previous StackEntry
		p.indice = -1; // before first
		p.cands = 0;
		// get the first cell from src (there must be at least 1!)
		c = stack[1]; // current StackEntry
		c.indice = -1; // before first
		// check all possible combinations of cells
		for(;;) {
			// fallback a level while this level is exhausted
			while ( stack[level].indice > nMinus2 )
				if ( --level < 1 )
					return result; // ok, done (level 0 is just a stopper)
			// ok, calculate next try
			// get a shorthand reference to the previous stack entry
			p = stack[level - 1];
			// get a shorthand reference to the current stack entry
			c = stack[level];
			// current cells = previous cells + this cell
			c.idx.setOr(p.idx, CELL_IDXS[indice=indices[++c.indice]]);
			// current cands = previous cands + this cells maybes
			c.cands = p.cands | cells[indice].maybes;
			// if current-line-cells-cands contains a candidate in intersection
			if ( (c.cands & interActCands) != 0 ) {
				// number of (candidates except the intersection candidates)
				anzExtra = VSIZE[lineOnlyCands=c.cands & ~interActCands];
				// we're dealing with line (the row/col) in checkLine.
				// level = number of cells currently in the lineStack
				// anzExtra = number of candidates in LINE except those in
				//            intersection of LINE and BOX
				// So WTF is nPlus? max number of candidates in inter?
				if ( level>anzExtra && level<nPlus+anzExtra ) {
					// The combination of cells contains candidates from da
					// intersection, it has at least one cell more than the
					// number of additional candidates (so it eliminates at
					// least one cell from the intersection) and there are
					// uncovered candidates left in the intersection ->
					// switch over to the box
					// memorize current selection for second run
					lineActIdx = c.idx;
					lineActCands = c.cands;
					// get the cells only in this box, excluding cells that
					// are already used in the line
					if ( boxOnlyIdx.setAndNot(boxAllIdx, interActIdx, lineActIdx).any()
					  // candidates from line are not allowed anymore
					  // and now we check each combo of cells in the box
					  && checkBox( nPlus - (lineActIdx.size()-anzExtra)
							, boxAllCands & ~(lineActCands & ~lineOnlyCands) )
					) {
						result = true;
						if ( onlyOne )
							return result;
					}
				}
			}
			// on to the next level (if any)
			if ( stack[level].indice < nMinus1 )
				// ok, go to next level
				stack[++level].indice = stack[level - 1].indice;
		}
	}

	/**
	 * checkBox checks each possible combination of cells in the current box.
	 * <pre><b>param nPlus</b> I'm struggling to reverse-engineer the meaning of nPlus
	 * from the code, so WARNING any/all of this may well be WRONG!
	 * Starting from the bottom: <b>checkBox</b> uses nPlus only in these lines:
	 * <code>    anzExtra = VSIZE[c.cands & ~interActCands];
	 *     if ( c.idx.size()-anzExtra == nPlus ) {
	 *         // It's a Sue de Coq</code>
	 * 1. anzExtra: VSIZE contains Integer.bitCount results. c.cands is the
	 *    candidates in the current box-cells-set, and interActCands is the
	 *    candidates in the cells in the intersection between line and box.
	 *    So anzExtra is number of (cands in box-cells except intersection).
	 *    I guess "anz" is German for "number of"; and "extra" candidates are
	 *    those distinct to the current box-cells-set, called boxOnlyCands.
	 * 2. c.idx contains the indices of current box-cells-set, so it's size
	 *    is the number of box-cells, which I shall call numBoxCells.
	 * 3. the original:{@code if ( c.idx.size()-anzExtra == nPlus )}
	 *    in my words :{@code if ( numBoxCells-numBoxOnlyCands == nPlus )}
	 * So I reckon nPlus is the number of intersection candidates required in
	 * each box, which still makes ____all-sense to me, because I'm stupid.
	 *
	 * So I look at the calculation of nPlus, to understand it's contents.
	 * 1. nPlus starts life when searchInter passes it's value into <b>checkLine</b>.
	 *    For two cells it's{@code VSIZE[cands2]-2}, and
	 *    for three cells it's{@code VSIZE[cands3]-3}, so
	 *    I reckon the formula is{@code numCands - numCells}, ie num "extra"
	 *    candidates in the cells in this intersection between line and box.
	 * 2. checkLine uses nPlus in two places:
	 *    (a){@code anzExtra = VSIZE[lineOnlyCands=c.cands & ~interActCands];
	 *    if ( level>anzExtra && level<nPlus+anzExtra ) {
	 *         // then process this line-cells-set
	 *    }}
	 *    so level is the number of cells in the current line-cells-set,
	 *    and anzExtra is the number of lineOnlyCands
	 *    and (nPlus+anzExtra) is the maximum number of cells in the current
	 *        line-cells-set, which I shall call maxNumCells.
	 *    So nPlus is the difference between maxNumCells and anzExtra,
	 *    ie the maximum number of candidates in the intersection.
	 *    (b) checkLine passes the following nPlus value into checkBox:
	 *       {@code nPlus - (lineActIdx.size()-anzExtra)}
	 *    which means{@code nPlus // max num cands in inter
	 *                - ( lineActIdx.size() // num empty cells in line
	 *                    - anzExtra ) // num lineOnlyCands}
	 *    simplified: nPlus - (num line cells sharing cands with box)
	 *    ie num "extra" cands in inter - num line cells sharing cands with box
	 *    so I reckon nPlus is num cands only in each box-cells-set
	 *    <b>but I don't think I understand it!</b>
	 * </pre>
	 *
	 * @param nPlus exact number of candidates only in each box
	 * @param okCands intersection only candidates
	 * @return
	 */
	private boolean checkBox(int nPlus, int okCands) {
		StackEntry p, c; // previous and current StackEntry
		int indice, bothCands, anzExtra;
		final Idx srcIdx = this.boxOnlyIdx;
		final StackEntry[] stack = boxStack; // the box stack
		final int[] indices = boxIndices; // the box indices array
		// read src into an array to save 0.1 seconds in top1465 (wow).
		final int n = srcIdx.toArrayN(indices)
				, nMinus1 = n - 1
				, nMinus2 = n - 2;
		// stack references to heap fields, for speed, I hope
		final Cell[] cells = grid.cells;
		// level 0 is just a stopper, we start with level 1
		int level = 1;
		// presume that no hint/s will be found.
		boolean result = false;
		// a stack of cells to represent the current combination of cells in
		// the intersection of line (row/col) and box that we're searching.
		p = stack[0]; // previous StackEntry
		p.indice = -1; // before first
		p.cands = 0;
		// get the first cell from src (there must be at least 1!)
		c = stack[1]; // current StackEntry
		c.indice = -1; // before first
		// check all possible combinations of cells
		for(;;) {
			// fallback a level while this level is exhausted
			while ( stack[level].indice > nMinus2 )
				if ( --level < 1 )
					return result; // ok, done (level 0 is just a stopper)
			// ok, calculate next try
			// get a shorthand reference to the previous stack entry
			p = stack[level - 1];
			// get a shorthand reference to the current stack entry
			c = stack[level];
			// current cells = previous cells + this cell
			c.idx.setOr(p.idx, CELL_IDXS[indice=indices[++c.indice]]);
			// current cands = previous cands + this cells maybes
			c.cands = p.cands | cells[indice].maybes;
			// the current cell combo must eliminate at least one candidate in
			// the current intersection or we dont have to look further.
			// KRC: if box-cands is covered-by intersection-only-cands
			//      && box-cands intersects interCands
			if ( (c.cands & ~okCands) == 0
			  // and contains a candidate in the intersection
			  && (c.cands & interActCands) != 0
			) {
				// number of (current-box-cands except interActCands), ergo
				// number of (boxOnlyCands); ie "extra" cands are boxOnly
				anzExtra = VSIZE[c.cands & ~interActCands];
				// nPlus: num cands only in these box-cells (I think)
				if ( c.idx.size()-anzExtra == nPlus ) {
					// It's a Sue de Coq, but are the any eliminations?
					// get current data for box
					boxActIdx = c.idx;
					boxActCands = c.cands;
					// get the extra candidates that are in both line and box
					bothCands = boxActCands & lineActCands;
					// first the box
					// tmpIdx is all cells in box that dont belong to the SDC
					// cands is all candidates that can be eliminated in box,
					//       including extra candidates contained in both sets
					eliminate(tmpIdx.setAndNot(boxAllIdx, boxActIdx, interActIdx) // idx
							, ((interActCands | boxActCands) & ~lineActCands) | bothCands // cands
							, theReds);
					// now the line (row/col)
					// tmpIdx is cells in line except SDC
					// cands is all candidates that can be eliminated in line,
					//       including extra candidates contained in both sets
					eliminate(tmpIdx.setAndNot(lineAllIdx, lineActIdx, interActIdx) // idx
							, ((interActCands | lineActCands) & ~boxActCands) | bothCands // cands
							, theReds);
					if ( !theReds.isEmpty() ) {
						// FOUND a Sue De Coq!
						final Pots reds = new Pots(theReds);
						theReds.clear();
						// intersection is written into indices and values
						final Pots greens = new Pots(grid, interActIdx, interActCands);
						// all candidates that occur in the intersection
						// and in the row/col become fins (for display)
						final Pots blues = potify(lineActIdx, interActIdx, lineActCands);
						// all candidates that occur in the intersection
						// and in the box become endo fins (for display)
						final Pots purples = potify(boxActIdx, interActIdx, boxActCands);
						// create the hint and add it to the IAccumulator
						final AHint hint = new SueDeCoqHint(this, reds, greens
								, blues, purples, line, box);
						result = true;
						if ( accu.add(hint) )
							return result;
					}
				}
			}
			// on to the next level (if any)
			if ( stack[level].indice < nMinus1 )
				// ok, go to next level
				stack[++level].indice = stack[level - 1].indice;
		}
	}

	/**
	 * Eliminate 'cands' from cells in 'idx' which maybe 'cands'.
	 *
	 * @param idx indices of cells to be eliminated from
	 * @param cands candidate values to be eliminated
	 * @param reds theReds (the bloody eliminations)
	 */
	private void eliminate(final Idx idx, final int cands, final Pots reds) {
		if ( VSIZE[cands]>0 && idx.any() )
			idx.forEach(grid.cells, (cell) -> {
				final int bits = cell.maybes & cands;
				if ( bits != 0 )
					reds.upsert(cell, bits, false);
			});
	}

	/**
	 * Returns a new 'pots' populated with each cell in 'a' or 'b' which maybe
	 * 'cands'. Each Values are those 'cands' that are in this cell.
	 *
	 * @param a the first Idx
	 * @param b the second Idx
	 * @param cands a bitset of the candidate values
	 */
	private Pots potify(final Idx a, final Idx b, final int cands) {
		final Pots result = new Pots();
		tmpIdx.setOr(a, b).forEach(grid.cells, (cell) -> {
			int bits = cell.maybes & cands;
			if ( bits != 0 )
				result.upsert(cell, bits, false);
		});
		return result;
	}

}
