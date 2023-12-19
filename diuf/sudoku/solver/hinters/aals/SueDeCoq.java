/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKu MiscellaneousSolver, by Bernhard Hobiger.
 * It is in its own aals (Almost Almost Locked Sets) package coz I know not
 * where it belongs. Kudos to hobiwan. Mistakes are mine. KRC.
 *
 * Here is hobiwans standard licence statement:
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
package diuf.sudoku.solver.hinters.aals;

import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.CELL_IDXS;
import static diuf.sudoku.Grid.MAYBES_STR;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.IntQueue;

/**
 * SueDeCoc implements the {@link Tech#SueDeCoq} Sudoku solving technique.
 * <p>
 * SueDeCoq is based on Almost Almost Locked Sets (AALSs):<ul>
 * <li>2 cells with 4 values among them, or
 * <li>3 cells with 5 values among them.
 * </ul>
 * <p>
 * The author of SueDeCoq gave it a long esoteric name, so folks just called it
 * after it is author, which stuck, so SueDeCoq it is. Personally, I just love
 * the name SueDeCoq, and we are just kicking off on sueing cocks, but, being
 * Australian, I advise you to completely disregard the collective opinion
 * of a whole nation of convicted criminals, except maybe Gus, which is exactly
 * why he is in jail. Nicking someones land leaves an ugly mark on your soul,
 * and your kids, and there kids, and so on; so the only solution is to totes
 * blame the other bastards, and kill them all. It cannot be MY fault. Simples!
 * There is no them, ya morons. There is only us, such as we are.
 * <p>
 * WARN: I implemented SueDeCoq to understand it, but I cannot, so SueDeCoq
 * almost certainly has bugs. You have been warned. There is three SueDeCoqs in
 * top1465; so either they are very rare, or this code is deficient. Sigh.
 * <pre>
 * 2023-10-27 I tried to make this faster, but ended-up making 1.2 secs slower.
 * 300ms vs 1.5 seconds, hence I advise myself to NOT ____ with it any further!
 * </pre>
 *
 * @author Keith Corlett 2021 Jan
 */
public final class SueDeCoq extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln("SueDeCoq: COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[3];

	/** all indices in the current row-or-col (empty cells only) */
	private final Idx lineAllIdx = new Idx();
	/** all indices in the current box (empty cells only) */
	private final Idx boxAllIdx = new Idx();
	/** all indices in the current intersection (empty cells only) */
	private final Idx slotAllIdx = new Idx();
	/** the 2 or 3 empty cells in the intersection between line and box */
	private final int[] slot = new int[3];
	/** all indices only in line: line empties - intersection */
	private final Idx lineOnlyIdx = new Idx();
	/** all indices only in box: box empties - intersection */
	private final Idx boxOnlyIdx = new Idx();
	/** stack for searching rows/cols */
	private final StackEntry[] lineStack = new StackEntry[REGION_SIZE];
	/** stack for searching boxs */
	private final StackEntry[] boxStack = new StackEntry[REGION_SIZE];
	/** the actual subset of the intersection */
	private final Idx slotActIdx = new Idx();
	/** candidates of all cells in {@link #slotActIdx} */
	private int slotActCands = 0;
	/** the actual additional cells in the row/col */
	private Idx lineActIdx;
	/** candidates of all cells in {@link #lineActIdx} */
	private int lineActCands;
	/** for temporary calculations */
	private final Idx tmp = new Idx();
	/** the removable (red) Cell=>Values */
	private final Pots reds = new Pots();
	/** the indices of cells in this line */
	final int[] lineIndices = new int[REGION_SIZE];
	/** the indices of cells in this box */
	final int[] boxIndices = new int[REGION_SIZE];
	/** are there any eliminations */
	private boolean any;

	public SueDeCoq() {
		super(Tech.SueDeCoq);
		for ( int i=0; i<REGION_SIZE; ++i ) {
			lineStack[i] = new StackEntry();
		}
		for ( int i=0; i<REGION_SIZE; ++i ) {
			boxStack[i] = new StackEntry();
		}
	}

	// accu.isSingle(), just to save calling the method repeatedly.
	boolean onlyOne;
	// the current row/col that we are searching. This is only a field rather
	// than pass it all the way from top-of-stack right down to the bottom,
	// recursively, and with extra maple garter cheese and a cherry on top.
	private ARegion line;
	// the current box that we are searching... see above, minus silly bits.
	private ARegion box;

	@Override
	public boolean findHints() {
		final boolean result;
		try {
			if ( oneHintOnly ) // stop on success
				result = search(rows) || search(cols);
			else
				result = search(rows) | search(cols);
		} finally {
			// clean-up after myself
			this.line = null;
			this.box = null;
		}
		return result;
	}

	/**
	 * Search each intersection of the given {@code lines} with the boxs,
	 * delegating the search to {@link #searchSlot}.
	 *
	 * @param lines to examine: grid.rows or grid.cols
	 * @return any hint/s found?
	 */
	private boolean search(ARegion[] lines) {
		// examine each intersection between lines and boxs
		// get the empty cells in this grid (cached)
		final Idx empties = grid.getEmpties();
		// presume that no hints will be found
		boolean result = false;
		// foreach row/col
		for ( ARegion line : lines ) {
			// nb: in SueDeCoq each line has 0 or >= 2 empty cells, because
			// HiddenSingle is mandatory, and it knocks-off the 1s, so that
			// setAndAny is equivalent to setAndMany, it is just a bit faster.
			// nb: we start with "All" sets of the empty cells in the regions,
			// which searchInter then reduces down to "Act" sets of cells in
			// the "actual" current sets. Clear?
			if ( lineAllIdx.setAndAny(line.idx, empties) ) {
				// foreach intersecting box (each box this row/col crosses)
				for ( ARegion box : line.intersectors ) {
					// if theres 2+ empty cells in intersection
					if ( slotAllIdx.setAndMany(lineAllIdx
							, boxAllIdx.setAnd(box.idx, empties))
					  // search the intersecting cells (2 or 3 of them)
					  && (result |= searchSlot(line, box))
					  // we found a SueDeCoq
					  && onlyOne ) {
						return result;
					}
				}
			}
		}
		// above result logic is a bit interesting. If onlyOne then the first
		// result returns, else onlyOne is evalutated repeatedly once the first
		// hint is found, but is still false, so does not return; then when we
		// get down here result is true, so it works despite being more than a
		// little convoluted; hence this explanation.
		return result;
	}

	/**
	 * searchSlot searches all combos of cells in the intersection of a line
	 * and a box. In SE, the three cells intersecting a line and a box is named
	 * a "slot", for brevity. If a combination holds atleast 2 more candidates
	 * than cells then its a possible AALS, hence a possible SueDeCoq.
	 * <p>
	 * the line and box fields are preset;<br>
	 * as are the lineIdx, boxIdx, and the slotIdx fields.
	 * <p>
	 * This method doesnt use recursion coz there can be just two or three
	 * cells in a slot, so we just iterate them. Iteration is usually faster
	 * than recursion (no new stackframes).
	 *
	 * @param line the current line
	 * @param box the current box
	 * @return any hint/s found. When onlyOne that means ONE hint was found,
	 *  else that means one or more hints were found.
	 */
	private boolean searchSlot(final ARegion line, final ARegion box) {
		int i1, i2, i3
		  , indice1, indice2, indice3
		  , cands1, cands2, cands3
		  , nLines; // number of indices read into lineIndices
		// read indices from interAllIdx into the intersection array
		final Idx slotActIdx = this.slotActIdx;
		final Idx lineOnlyIdx = this.lineOnlyIdx;
		final Idx lineAllIdx = this.lineAllIdx;
		final int[] slot = this.slot; // intersection of line and box
		final int[] array1 = new int[REGION_SIZE];
		final int n = slotAllIdx.toArrayN(slot);
		final int m = n - 1;
		// set the fields rather than pass them down any further
		this.line = line;
		this.box = box;
		// presume that no hints will be found
		boolean result = false;
		// build a bitset of the combined maybes of cells in boxAllIdx,
		// for efficient repeated reading down in checkLine.
		i1 = 0; // hijack i1,i2,i3
		for ( i2=0,i3=boxAllIdx.toArrayN(array1); i2<i3; ++i2 ) {
			i1 |= maybes[array1[i2]];
		}
		boxAllCands = i1;
		// indices of the 2 or 3 empty cells in the intersection of line & box
		// nb: this set should always be empty before clear. Paranoia rulz!
		slotActIdx.clear();
		// first we get the first cell
		for ( i1=0; i1<m; ++i1 ) {
			// set the bitset of the first candidate value
			cands1 = maybes[indice1=slot[i1]];
			// add this cell to the intersection actual set
			slotActIdx.add(indice1);
			// now we get the second cell
			for ( i2=i1+1; i2<n; ++i2 ) {
				// add this cell to the intersection actual set
				// note: do before if coz the threes use him too! sigh.
				slotActIdx.add(indice2=slot[i2]);
				// build-up the bitset of two candidate values
				// we have two cells in the intersection, so they need atleast
				// four candidates between them in order to form an AALS
				if ( VSIZE[cands2=cands1 | maybes[indice2]] > 3 ) {
					// possible SDC -> check
					// store the candidates of the current intersection
					slotActCands = cands2;
					// check line cells except intersection cells
					if ( lineOnlyIdx.setAndNotAny(lineAllIdx, slotActIdx)
					  // SDC can't hint with more than 6 free cells in line
					  && (nLines=lineOnlyIdx.toArrayN(lineIndices)) < 7
					  // now check all possible combos of cells in row/col
					  // boolean check(int nPlus, Idx src, int okCands, boolean pass1)
					  && checkLine(VSIZE[cands2]-2, nLines) ) {
						result = true;
						if ( onlyOne ) {
							return result;
						}
					}
				}
				// and now we get the third cell, if any
				for ( i3=i2+1; i3<n; ++i3 ) {
					// build-up the bitset of three candidate values
					// now we have three cells in the intersection, so they
					// need atleast five candidates in order to form an AALS
					if ( VSIZE[cands3=cands2 | maybes[indice3=slot[i3]]] > 4 ) {
						// possible SDC -> check
						// store the candidates of the current intersection
						slotActCands = cands3;
						// add this cell to the intersection actual set
						slotActIdx.add(indice3);
						// check line cells except intersection cells
						if ( lineOnlyIdx.setAndNotAny(lineAllIdx, slotActIdx)
						  // SDC can't hint with more than 6 free cells in line
						  && (nLines=lineOnlyIdx.toArrayN(lineIndices)) < 7
						  // now check all possible combos of cells in row/col
						  // boolean check(int nPlus, Idx src, int okCands, boolean pass1)
						  && checkLine(VSIZE[cands3]-3, nLines) ) {
							result = true;
							if ( onlyOne ) {
								return result;
							}
						}
						slotActIdx.remove(indice3);
					}
				}
				slotActIdx.remove(indice2);
			}
			slotActIdx.remove(indice1);
		}
		return result;
	}
	private int boxAllCands;

	/**
	 * checkLine checks each possible combination of cells in the current line,
	 * by checking each possible combination of box-only-cells.
	 * <p>
	 * Hobiwan says:
	 * <pre>
	 * We search each possible combination of cells in the line-only.
	 * A valid set contains candidates from the intersection
	 * and has at least one cell more than extra candidates (candidates not in
	 *                                                       the intersection)
	 * and leaves candidates in the intersection for the box.
	 * If all criteria are met then I call checkBox.
	 * </pre>
	 * <p>
	 * To whit I (KRC) respond:
	 * <pre>
	 * <b>param nPlus</b> is the number of whatsits in the thingymajig.
	 * Its a nonnominative-nominative, ergo a piss-poor name. I am struggling
	 * to reverse-engineer its meaning from the code, so WARNING any/all of
	 * the following statement may well be WRONG!
	 *
	 * I think that in checkLine nPlus is the maximum number of candidate
	 * values, which I reckon is:
	 *     if ((nPuss=VSIZE[boxAllCands & ~slotCands]) > 0)
	 * ergo If boxAllCands is NOT a subset of slotCands then process box.
	 * nPlus has other (closely associated) meanings in other methods.
	 *
	 * But I am confused. Hobiwan is a genius, who didn't even attempt to
	 * document it, because its too bloody complicated (ergo it seems simple
	 * enough to me, and keeping the bastards guessing creates a dependancy)
	 * but he is a genius none the less.
	 *
	 * {@code trouble *= iq * rush}. BIG Sigh! ~KRC.
	 * </pre>
	 *
	 * @param nPlus See above. BFIIK but I reckon: number of "extra" candidates
	 *  in the cells in this intersection between line and box,
	 *  ergo {@code numCands - numCells}
	 * @return
	 */
	private boolean checkLine(final int nPlus, final int nLines) {				//   329,028
		StackEntry p, c; // previous and current StackEntry
		Idx prevIdx, currIdx, me; // previous Idx, current Idx, my Idx
		int indice // the indice of the current cell in this line
		  , lineCands // faster c.cands
		  , lineOnlyCands // line cands except the intersection cands
		  , anzExtra // number of (cands except the intersection cands)
		;
		// presume that no hint/s will be found.
		boolean result = false;
		// stack references to heap fields, for speed, I hope
		final StackEntry[] lineStack = this.lineStack; // the line stack
		final int[] lineIndices = this.lineIndices; // lineOnlyIdx indices
		final int m = nLines - 1;
		final int l = nLines - 2;
		// level 0 is just a stopper, we start with level 1
		int level = 1;
		// a stack of cells to represent the current combination of cells in
		// the intersection of line (row/col) and box that we are searching.
		p = lineStack[0]; // previous StackEntry
		p.indice = -1; // before first
		p.cands = 0;
		// get the first cell from src (there must be at least 1!)
		c = lineStack[1]; // current StackEntry
		c.indice = -1; // before first
		// check all possible combinations of cells
		for(;;) {																// 8,346,848
			// fallback a level while this level is exhausted
			while ( lineStack[level].indice > l )
				if ( --level < 1 )
					return result; // ok, done (level 0 is just a stopper)
			// ok, calculate next try
			// get a shorthand reference to the previous stack entry
			p = lineStack[level - 1];
			// get a shorthand reference to the current stack entry
			c = lineStack[level];
			// indice of the current cell in this line
			indice = lineIndices[++c.indice];
			// current cells = previous cells | this cell
			(currIdx=c.idx).m0 = (prevIdx=p.idx).m0 | (me=CELL_IDXS[indice]).m0;
			currIdx.m1 = prevIdx.m1 | me.m1;
			assert currIdx.size() == level;
			// current cands = previous cands + this cells maybes
			lineCands = c.cands = p.cands | maybes[indice];
			// if current-line-cells-cands contains a candidate in intersection
			if ( (lineCands & slotActCands) > 0 ) { // 9bits					// 7,970,743
				// number of (candidates except the intersection candidates)
				anzExtra = VSIZE[lineOnlyCands=lineCands & ~slotActCands];
				// we are dealing with line (the row/col) in checkLine.
				// level = number of cells currently in the lineStack
				// anzExtra = number of candidates in LINE except those in
				//            intersection of LINE and BOX
				// So WTF is nPlus? max number of candidates in inter?
				if ( level>anzExtra && level<nPlus+anzExtra						// 4,606,666
				  // The combination of cells contains candidates from the
				  // intersection, it has at least one cell more than the
				  // number of additional candidates (so it eliminates atleast
				  // one cell from the intersection) and there are uncovered
				  // candidates left in the intersection then switch over to
				  // the box and memorize current selection for the second run.
				  // get the cells only in this box, excluding cells that are
				  // already used in the line									// 4,606,666
				  && boxOnlyIdx.setAndNotAny(boxAllIdx, slotActIdx, (lineActIdx=currIdx))
				  // candidates from line are not allowed anymore
				  // and now we check each combo of cells in the box
				  && checkBox(nPlus - (lineActIdx.size()-anzExtra)
					, boxAllCands & ~((lineActCands=lineCands) & ~lineOnlyCands))
				) {																//        10
					result = true;
					if ( onlyOne ) {
						return result;
					}
				}
			}
			// on to the next level (if any)
			if ( lineStack[level].indice < m ) {
				// ok, go to next level
				lineStack[++level].indice = lineStack[level - 1].indice;
			}
		}
	}

	/**
	 * checkBox checks each possible combination of cells in the current box.
	 * <pre><b>param nPlus</b> Struggling to reverse-engineer the meaning of
	 * nPlus from the code, so WARNING any/all of this may well be WRONG!
	 * Starting from the bottom: <b>checkBox</b> uses nPlus only in:
	 * <code>
	 *     anzExtra = VSIZE[c.cands & ~slotActCands];
	 *     if ( c.idx.size()-anzExtra == nPlus ) {
	 *         // It is a Sue de Coq</code>
	 * 1. anzExtra: VSIZE contains Integer.bitCount results. c.cands is the
	 *    candidates in the current box-cells-set, and slotActCands is the
	 *    candidates in the cells in the intersection between line and box.
	 *    So anzExtra is number of (cands in box-cells except intersection).
	 *    I guess "anz" is German for "number of"; and "extra" candidates are
	 *    those distinct to the current box-cells-set, called boxOnlyCands.
	 * 2. c.idx contains the indices of current box-cells-set, so it is size
	 *    is the number of box-cells, which I shall call numBoxCells.
	 * 3. the original:{@code if ( c.idx.size()-anzExtra == nPlus )}
	 *    in my words :{@code if ( numBoxCells-numBoxOnlyCands == nPlus )}
	 * So I reckon nPlus is the number of intersection candidates required in
	 * each box, which still makes ____all-sense to me, because I am stupid.
	 *
	 * So I look at the calculation of nPlus, to understand it is contents.
	 * 1. nPlus starts when searchInter passes its value into <b>checkLine</b>.
	 *    For two cells it is{@code VSIZE[cands2]-2}, and
	 *    for three cells it is{@code VSIZE[cands3]-3}, so
	 *    I reckon the formula is{@code numCands - numCells}, ie num "extra"
	 *    candidates in the cells in this intersection between line and box.
	 * 2. checkLine uses nPlus in two places:
	 *    (a){@code anzExtra = VSIZE[lineOnlyCands=c.cands & ~slotActCands];
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
	 *    which means{@code nPlus // max num cands in slot
	 *                - ( lineActIdx.size() // num empty cells in line
	 *                    - anzExtra ) // num lineOnlyCands}
	 *    simplified: nPlus - (num line cells sharing cands with box)
	 *    ie num "extra" cands in slot - num line cells sharing cands with box
	 *    so I reckon nPlus is num cands only in each box-cells-set
	 *    <b>but I do not think I understand it!</b>
	 * </pre>
	 *
	 * @param nPlus exact number of candidates only in each box
	 * @param slotOnlyCands intersection only candidates
	 * @return
	 */
	private boolean checkBox(final int nPlus, final int slotOnlyCands) {		//   4,606,666
		StackEntry p, c; // previous and current StackEntry
		Idx prevIdx, currIdx, me; // previous Idx, current Idx, my Idx
		int i // index in grid.cells of the current cell in this box
		  , boxCands // shorthand for c.cands
		  , bothCands // extra candidates that are in both line and box
		  , pinkos // candidates to be eliminated
		;
		final StackEntry[] boxStack = this.boxStack; // the box stack
		final int[] boxIndices = this.boxIndices; // the box indices array
		// read src into an array to save 0.1 seconds in top1465 (wow).
		final int n = boxOnlyIdx.toArrayN(boxIndices)
				, nMinus1 = n - 1
				, nMinus2 = n - 2;
		// level 0 is just a stopper, we start with level 1
		int level = 1;
		// presume that no hint/s will be found.
		boolean result = false;
		// a stack of cells to represent the current combination of cells in
		// the intersection of line (row/col) and box that we are searching.
		p = boxStack[0]; // previous StackEntry
		// get the first cell from src (there must be at least 1!)
		c = boxStack[1]; // current StackEntry
		p.indice = c.indice = -1; // before first
		p.cands = 0;
//<HAMMERED comment="top1465 iterates combo loop over a hundred million times">
		// check all possible combinations of cells in the box
		for(;;) {																// 110,669,603
			// fallback a level while this level is exhausted
			while ( boxStack[level].indice > nMinus2 )
				if ( --level < 1 ) // level 0 is just a stopper
					return result; // done
			// get a shorthand/fast reference to the previous stack entry
			p = boxStack[level - 1];
			// get a shorthand/fast reference to the current stack entry
			c = boxStack[level];
			// get the indice of the current cell in this box
			i = boxIndices[++c.indice];
			// current cells = previous cells | this cell
			(currIdx=c.idx).m0 = (prevIdx=p.idx).m0 | (me=CELL_IDXS[i]).m0;
			currIdx.m1 = prevIdx.m1 | me.m1;
			assert currIdx.size() == level;
			// current cands = previous cands | this cells maybes
			boxCands = c.cands = p.cands | maybes[i];
			// the current cell combo must eliminate at least one candidate in
			// the current intersection.
			// KRC: if boxCands is NOT covered-by candsOnlyInTheIntersection
			//      && boxCands intersects slotCands
			if ( (boxCands & ~slotOnlyCands) < 1								//   1,071,128  0.97%
//</HAMMERED>
			  // and contains a candidate in the intersection
			  && (boxCands & slotActCands) > 0 // 9bits							//     562,105 52.47%
			  // and currentCombo has extra cands to be eliminated (KRC thinks)
			  // VSIZE[boxCands & ~slotActCands] was called anzExtra, which KRC
			  // thinks might better be called numBoxOnlyCands.
			  // nPlus: num cands only in these box-cells (KRC thinks).
			  // giving us numBoxOnlyCands == numBoxOnlyCands, so BFIIK!
			  && level-VSIZE[boxCands & ~slotActCands] == nPlus					//       9,455  1.68%
			) {
				// It is a SueDeCoq, but any eliminations?
				// get the extra candidates that are in both line and box
				bothCands = boxCands & lineActCands;
				// first the box
				// pinkos is all candidates that can be elimnated in box,
				//       including extra candidates contained in both sets
				if ( (pinkos=((slotActCands | boxCands) & ~lineActCands) | bothCands) > 0
				  && tmp.setAndNotAny(boxAllIdx, currIdx, slotActIdx) )			//       9,455
					for ( final IntQueue q=tmp.indices(); (i=q.poll())>QEMPTY; )
						any |= reds.upsert(i, maybes[i] & pinkos, DUMMY);
				// then the line (row/col)
				// pinkos is all candidates that can be eliminated in line,
				//       including extra candidates contained in both sets
				if ( (pinkos=((slotActCands | lineActCands) & ~boxCands) | bothCands) > 0
				  && tmp.setAndNotAny(lineAllIdx, lineActIdx, slotActIdx) )		//       9,455
					for ( final IntQueue q=tmp.indices(); (i=q.poll())>QEMPTY; )
						any |= reds.upsert(i, maybes[i] & pinkos, DUMMY);
				if ( any ) {													//          10
					// FOUND a Sue De Coq!
					any = false;
					assert reds.any();
					result = true;
					if ( accu.add(new SueDeCoqHint(grid, this, reds.copyAndClear()
							, new Pots(slotActIdx, maybes, slotActCands, DUMMY) // greens
							, potify(lineActIdx, slotActIdx, lineActCands) // blues
							, potify(currIdx, slotActIdx, boxCands) // purples
							, line, box)) )
						return result;
				}
			}
			// on to the next level (if any)
			if ( boxStack[level].indice < nMinus1 ) {
				// ok, go to next level
				boxStack[++level].indice = boxStack[level - 1].indice;
			}
		}
	}

	/**
	 * Returns a new $pots populated with each cell in $a or $b which maybe
	 * $cands. Each Values are those $cands that are in this cell.
	 *
	 * @param a the first Idx
	 * @param b the second Idx
	 * @param cands a bitset of the candidate values
	 */
	private Pots potify(final Idx a, final Idx b, final int cands) {
		int i;
		final Pots result = new Pots();
		for ( final IntQueue q=tmp.setOr(a, b).indices(); (i=q.poll())>QEMPTY; )
			result.upsert(i, maybes[i] & cands, DUMMY);
		return result;
	}

	/** An entry in the recursion stack for the region search */
	private static class StackEntry {
		/** The indice of the cell that is currently tried. */
		int indice = 0;
		/** The indices of the cells in the current set. The instance is final,
		 * but it is indices (contents) mutate. */
		final Idx idx = new Idx();
		/** A bitset (Values.bits) of the maybes in the current set. */
		int cands = 0;
		@Override
		public String toString() {
			return "indice="+indice+", idx="+idx+", cands="+MAYBES_STR[cands];
		}
	}

}
