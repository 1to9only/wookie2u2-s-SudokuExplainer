/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKus SimpleColoring class, by Bernhard Hobiger,
 * published under GNUs GPL licence. Kudos to hobiwan. Mistakes are mine. KRC.
 *
 * Here is hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file was NOT part of HoDuKo, but the ideas where.
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
package diuf.sudoku.solver.hinters.color;

import static diuf.sudoku.Constants.BLUE;
import static diuf.sudoku.Constants.GREEN;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.RIBS;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.color.Words.*;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.IntQueue;
import java.util.Collection;
import java.util.LinkedList;

/**
 * XColoring implements the {@link Tech#XColoring} Sudoku solving technique.
 * XColoring is short for Extended Coloring.
 * <p>
 * Extended Coloring involves chains of bi-location candidates: Cells in
 * regions with 2 places for v are "colored", so that either colorA or colorB
 * contains v. The variable v is the Coloring candidate value, BTW.
 * <p>
 * Then X-forcing chains are located: if we presume this colored cell contains
 * v then some other cell must also contain v because it is the only location
 * remaining in a region.
 * <p>
 * Then cells which see both colors are eliminated, because either colorA
 * contains v or colorB contains v. If any cell sees two-or-more cells of the
 * same color, then the OTHER color must contain v. Similarly if all cells in
 * a region see cells of the same color then the OTHER color must contain v.
 * <pre>
 * KRC 2021-03-03 I was getting invalid hints from the version of this class
 * based on hobiwans SimpleColoring, so I rewrote it from scratch; but it is
 * still rooted, and the code looks to me like it follows the algorithm. Either
 * my code is wrong in some way that I am too stupid to understand (likely), or
 * the actual published algorithm is just plain wrong (unlikely).
 *
 * I tried alterations (Step K:) to get it to work, without any joy. So now I
 * find myself at a loss. It finds all the hints in the test-cases, but it also
 * finds many false-positives: invalid hints. So reluctantly declare that the
 * actual published algorithm is wrong, which I do NOT think. sigh.
 *
 * Surely anyone who published an algorithm would go to the trouble of testing
 * it over many many puzzles BEFORE they publish, to check that it does not do
 * unexpected things, like find false positives; so my actual conclusion is I am
 * too stupid to understand the published algorithm, and also too stupid to
 * understand the authors intent, and so make it work despite his publication.
 *
 * KRC 2021-03-03 09:35 Having failed to sort-out above invalid hint issues, I
 * have decided to password protect XColoring, so that only coders can use it.
 * My thinking is: I have tried everything I can think of to avoid/avert invalid
 * hints and either I am misunderstanding the algorithm or it is WRONG! I think
 * it is just plain WRONG! Allthough one should never underestimate ones own
 * capacity for sheer unadulterated stupidity.
 *
 * KRC 2021-05-11 At some time in the past I fixed the above invalid hint issue
 * but forgot to comment on it. XColoring is no longer password protected. The
 * problem with coloring is that if any of it is wrong then it is all wrong.
 *
 * KRC 2021-05-17 Investigated XColoring-hints found after GEM has run (moved
 * GEM up in LogicalSolver.configureHinters). I can confirm that the XColoring
 * algorithm finds hints that GEM misses, because GEM requires "and conversely"
 * to paint a cell-value, so XColoring keeps painting where GEM stops. So we
 * use Coloring (for multi and fast basics), XColoring (for misses) and GEM.
 * </pre>
 *
 * @author Keith Corlett 2021-02-27
 */
public final class XColoring extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[10];

	// NOTE WELL: BLUE=0 and GREEN=1 are defined in Constants, for consistency
	// throughout the coloring hinters: Coloring, XColoring, Medusa3D, GEM.
	// These constants are also used in the GUI, hence go all the way up in the
	// Constants class, which is visible throughout SE. The rule is that if it
	// is referenced externally, it gets promoted up the tree. That way "local
	// names" still work, and "global names" are as limited as possible, and
	// remain consistent accross the codebase. Name spaces are complex!
	private static final int RED = 2;

	/** The first, second and RED colors. */
	private static final String GREEN_TAG = GON+"green"+GOFF
							  , BLUE_TAG  = BON+"blue"+BOFF
							  , RED_TAG   = RON+"red"+ROFF;
	/** COLOR_NAME HTML tags. */
	private static final String[] CNAME = {GREEN_TAG, BLUE_TAG, RED_TAG};

	/** COLOR_ON HTML tags. */		    //GREEN  BLUE  RED
	private static final String[] CON  = {GON,  BON,  RON};
	/** COLOR_OFF HTML tags. */
	private static final String[] COFF = {GOFF, BOFF, ROFF};

	// the opposite color
	private static final int[] OPPOSITE = {1, 0};

	// pollDirtyRegions uses Integer.numberOfTrailingZeros(x) which returns 32
	// meaning that there are no set (1) bits in this int, so that is my NADA.
	private static final int NADA = 32;

	// the iceQ size and mask
	// iceQ is a Queue of IndiceColorElement's, an array of int arrays, used as
	// a "circular buffer", which wraps-back to 0 at the end of the array.
	// This array must be large enough to hold num-elements-awaiting-processing
	// which is less than the-total-number-of-elements, hence we save some RAM.
	// An array is faster mostly because access is native, no methods means, no
	// stack-work, and reducing stackwork is somewhat an obsession of mine. We
	// seem to spend most of our CPU-time these days creating new stackframes,
	// which leaves very little time for actually doing anything. A tyranny of
	// decomposition. Go back to torts and just "do it all manually". Simples.
	private static final int Q_SIZE = 32;			// must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;	// for this trick to work

	// ============================ instance stuff ============================

	// a bitset of indexes of regions that are "dirty", ergo a queue of regions
	// to be reprocessed, added by search and read by pollDirtyRegions.
	// nb: dirtyRegions finds 0 elims in top1465, so removed it, but take twice
	// as long (a WTF), so I reinstated it, but it STILL takes twice. BIG WTF!
	// nb: I'm a field to be mutated in the pollDirtyRegions method.
	private int dirtyRegions;

	// do we build an explanation of the coloring steps?
	// GUI/test-cases require explanation. Not batch (speed).
	private final boolean wantWhy;

	public XColoring() {
		super(Tech.XColoring);
		wantWhy = !Run.isBatch();
	}

	@Override
	public void setFields(final Grid grid) {
		this.grid = grid;
		regions = grid.regions;
		cells = grid.cells;
		maybes = grid.maybes;
		idxs = grid.idxs;
	}

	@Override
	public void clearFields() {
		grid = null;
		regions = null;
		cells = null;
		maybes = null;
		idxs = null;
	}

	/**
	 * Finds all X-Color Trap/Wrap hints in the grid and adds them to accu.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints() {
		Cell cell; // the cell that we are currently examining
		AHint hint; // the hint, if we ever bloody find one
		Idx buds; // buddies of the current cell
		Idx colorSet; // current color
		Idx otherSet; // OPPOSITE color
		IntQueue q; // interator!
		int ice[] // indice color element, indexes [indice][color]
		, riva[] // VALUESES[region.places[v]]
		, qr, qw // iceQ read/write index
		, g, b // green and blue conjugate pair indices
		, indice // grid.cells indice is this color
		, conjugate // the other indice in the pair is the other color
		, subtype // the hint type, if any
		, c // this colors colorSets index: GREEN=0 or BLUE=1
		, o // the OPPOSITE (ergo other) color to c
		, dri // dirtyRegionIndex (dirty means "needs to be reprocessed")
		, colorOfOns // the color to paint results in XColoringHintBig
		;
		boolean any, first; // any conjugates, first conjugate
		// The steps StringBuilder says why this hint exists.
		// It is part of the hint-HTML.
		final StringBuilder steps;
		// GUI/test-cases require explanation. Not batch (speed).
		if ( wantWhy ) {
			steps = SB(1024);
			sb = SB(256); // steps contributor
		} else {
			steps = sb = null;
		}
		// no need for fields for arrays (reduce hangover heap waste)
		final Pots oranges = new Pots(); // intertesting cellulites
		final Pots reds = new Pots(); // eliminations
		final Pots setPots = new Pots(); // {cell-value}s to set
		final Collection<Link> links = new LinkedList<>();
		// indices of cells that are painted this color.
		// Coloring is about painting cells, deliniating two Cell sets.
		// One of these sets is true, the other one is Donald Trump.
		// * GREEN=0 is the first color
		// * BLUE=1 is the second color
		final Idx green = new Idx(), blue = new Idx();
		final Idx colorSets[] = {green, blue};
		final Idx bothColors = new Idx(); // colorSets[0] | colorSets[1]
		final Idx tribe = new Idx(); // colorSet|BUDDIES[colorSet], hijack BUDDIES[colorSet]
		final Idx xSet = new Idx(); // The Exception Cell, or X for short
		final Idx tmp = new Idx(); // temporary Idx (set, read, forget)
		// Indice Color Element Queue:
		// first index is the queue element index: 0..Q_SIZE-1
		// second index is the color index, C1 or C2
		final int[][] iceQ = new int[Q_SIZE][];
		boolean result = false; // presume that no hint will be found
		// foreach region in the grid
		OUTER: for ( ARegion region : regions ) {
			// foreach value that remains to be placed in this region
			for ( int v : VALUESES[region.unsetCands] ) {
				// with 2 places for v (ie a conjugate pair in region on v)
				if ( region.numPlaces[v] == 2 ) {
					// ----------------------------------------------------
					// Step 1: Paint conjugate pair (only two places for v
					// in a region). Color the first GREEN, second BLUE.
					// ----------------------------------------------------
					// nb: build steps delayed till Step2 coz most Step1s
					// go nowhere, even in the GUI.
					bothColors.set(region.idxs[v]);
					// region.places[v] contains 2 cells: $a and $b
					// which we set in the 2 colorSets: green and blue
					riva = INDEXES[region.places[v]];
					green.setIndice(g=region.indices[riva[0]]);
					blue.setIndice(b=region.indices[riva[1]]);
					// ----------------------------------------------------
					// Step 2: Until no more new cells are colored, do:
					// Find an uncolored conjugate of a colored cell
					// and color it the OPPOSITE color.
					// ----------------------------------------------------
					any = false;
					first = true;
					// there's always a first ice-cube to suck on
					// IndiceColorElement, is just a Queue, in an array.
					ice = new int[]{g, GREEN};
					// add the second ice-cube
					iceQ[0] = new int[]{b, BLUE};
					qw = 1;
					qr = 0;
					// while there is any ICE remaining in the queue
					ICE: for(;;) {
						// read cell indice = first int in the ICE
						indice = ice[0];
						cell = cells[indice];
						// foreach region which contains this cell
						for ( ARegion r : cell.regions ) {
							// if this region has 2 places for v
							if ( r.numPlaces[v] == 2
							  // and the conjugate is not already colored
							  && !bothColors.has(conjugate=r.indices[
								  IFIRST[r.places[v] & ~cell.placeIn[r.rti]]])
							) {
								// paint the conjugate the other color
								// read color = second int in the ICE
								// and look-up the opposite (other) color
								c = ice[1];
								o = OPPOSITE[c];
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										steps.setLength(0); // clear the steps string
										steps.append("Step 1: Color conjugate pair: ")
										.append(colorCellId(GREEN, g))
										.append(AND).append(colorCellId(BLUE, b))
										.append(IN).append(region.label)
										.append(ON).append(v)
										.append(NL);
									}
									steps.append("Step 2: ")
									.append(colorColorCellId(c, indice))
									.append(" =conjugate=> ")
									.append(colorColorCellId(o, conjugate))
									.append(IN).append(r.label)
									.append(NL);
								}
								colorSets[o].add(conjugate);
								bothColors.add(conjugate);
								// add this painted cell to the queue
								iceQ[qw] = new int[]{conjugate, o};
								qw = (qw+1) & Q_MASK;
								assert qw != qr; // queue undersized
								any = true;
							}
						}
						// nb: this is just iceQ.poll(), without a method call,
						// which is the main reason why this mess is faster.
						if ( (ice=iceQ[qr]) == null )
							break;
						iceQ[qr] = null;
						qr = (qr+1)&Q_MASK;
					}
					// ----------------------------------------------------
					// Step 3. Extended coloring
					// If all bar one v in a region see cells of the same color
					// then paint it the SAME color.
					// ----------------------------------------------------
					if ( any ) {
						// foreach color
						c = 0;
						COLORS: do {
							tribe.set(colorSet=colorSets[c]);
							// tribe := colorSet | BUDDIES[colorSet]
							for ( int ii : colorSet )
								tribe.or(BUDDIES[ii]);
							dirtyRegions = 0;
							// foreach region in the grid
							for ( ARegion r : regions )
								step3("Step 3.1: ", c, r, v, xSet
									, tribe, steps, colorSet, bothColors);
							// reprocess dirty regions, adding any new ones
							// to the queue, until the queue is empty.
							while ( (dri=pollDirtyRegions()) < NADA )
								step3("Step 3.2: ", c, regions[dri], v, xSet
									, tribe, steps, colorSet, bothColors);
						} while (++c < 2);
						// just pre-clear everything, EVERY time. KISS.
						subtype = 0;	 // the type of hint created
						reds.clear();	 // Type 1 only
						setPots.clear(); // Type 2 and Type 3
						colorOfOns = -1; // color to paint setPots: NONE
						links.clear();	 // Type 3 only
						oranges.clear(); // interesting cellulites
						// ------------------------------------------------
						// Step 4: Once all the coloring is done:
						// 4.2 If multiple cells in a region are the same
						//     color, then the OTHER color is true.
						// ------------------------------------------------
						// nb: Type 2 is as rare as rocking horse s__t.
						// None in top1465, with "normal" Coloring first.
						c = 0;
						COLORS: do {
							colorSet = colorSets[c];
							for ( ARegion r : regions ) {
								// nb: I am hijacking xSet
								if ( xSet.setAndMany(colorSet, r.idx) ) {
									otherSet = colorSets[o=OPPOSITE[c]];
									if ( wantWhy ) {
										steps.append("Step 4.2: Multiple cells ")
										.append(colorCellIds(c, xSet)).append(IN)
										.append(r.label).append(ARE).append(CNAME[c])
										.append(", which is invalid, so the ")
										.append(CNAME[o]).append(SET)
										.append(colorCellIds(o, otherSet))
										.append(" must be true, ie ").append(v)
										.append(NL);
									}
									setPots.upsertAll(otherSet, maybes, v);
									colorOfOns = o;
									subtype = 2; // Type 2
									break COLORS; // first only
								}
							}
						} while (++c < 2);
						if ( subtype == 0 ) {
							// 4.3 If ALL cells in region which maybe v see
							//     cells of the same color, then the OTHER
							//     color is true.
							c = 0;
							COLORS: do {
								colorSet = colorSets[c];
								// tribe := BUDDIES[colorSet] (hijack)
								tribe.clear();
								for ( int ii : colorSet )
									tribe.or(BUDDIES[ii]);
								for ( ARegion r : regions ) {
									// if ALL vs in r see a thisColorCell
									if ( xSet.setAndAny(tribe, r.idxs[v])
									  && xSet.size() == r.numPlaces[v]
									) {
										otherSet = colorSets[o=OPPOSITE[c]];
										if ( wantWhy ) {
											steps.append("Step 4.3: ALL cells in ")
											.append(r.label).append(" which maybe ")
											.append(v).append(SP).append(r.idxs[v].ids())
											.append(" see a ").append(colorValue(c, v))
											.append(", which is invalid, so the ")
											.append(CNAME[o]).append(SET).append(colorCellIds(o, otherSet))
											.append(" must be true, ie ").append(v)
											.append(NL);
											// batch no use oranges or links
											for ( int x : xSet ) {
												oranges.put(x, VSHFT[v]);
												// first (random) bud is ok
												links.add(new Link(x, v, tmp.setAnd(colorSet, BUDDIES[x]).poll(), v));
											}
										}
										setPots.upsertAll(otherSet, maybes, v);
										colorOfOns = OPPOSITE[c];
										subtype = 3;
										break COLORS; // first only
									}
								}
							} while (++c < 2);
							if ( subtype == 0
							  // 4.1 If a cell sees both GREEN and BLUE,
							  //     then exclude v from this cell.
							  // myCells := all uncolored vs in grid.
							  && xSet.setAndNotAny(idxs[v], bothColors)
							) {
								// foreach uncolored v in grid
								for ( q=xSet.indices(); (indice=q.poll())>QEMPTY; ) {
									buds = BUDDIES[indice];
									// if cell sees both GREEN and BLUE
									if ( buds.intersects(green)
									  && buds.intersects(blue)
									) {
										if ( wantWhy ) {
											steps.append("Step 4.1: cell ").append(colorCellId(RED, indice))
											.append(" sees BOTH ").append(colorCellIds(GREEN, tmp.setAnd(buds, green)))
											.append(" and ").append(colorCellIds(BLUE, tmp.setAnd(buds, blue)))
											.append(NL);
										}
										reds.put(indice, VSHFT[v]);
										subtype = 1; // Type 1 (type not reported)
									}
								}
							}
						}
						// create the hint, if any
						if ( subtype > 0 ) {
							boolean ok = true;
							// Type 2 or Type 3: Set multiple cells (rare).
							if ( subtype==3 || subtype==2 ) {
								hint = new XColoringHintBig(grid, this, v
								, subtype
								, new Idx[]{new Idx(green), new Idx(blue)}
								, new Idx(xSet)
								, colorOfOns
								, wantWhy ? steps.toString() : null
								, setPots.copyAndClear() // copy to reuse
								, new Pots(green, maybes, VSHFT[v], DUMMY)
								, new Pots(blue, maybes, VSHFT[v], DUMMY)
								, new LinkedList<>(links) // copy to reuse
								, oranges.copyAndClear() // Type 3 cause
								, null // region
								);
								// clear fields for next time
								links.clear();
							// Type 1: Eliminations are common.
							} else {
								hint = new XColoringHint(grid, this, v
								, reds.copyAndClear()
								, new Pots(green, maybes, VSHFT[v], DUMMY)
								, new Pots(blue, maybes, VSHFT[v], DUMMY)
								, new Idx[] {new Idx(green), new Idx(blue)}
								, wantWhy ? steps.toString() : null
								, null
								);
							}
							// add the hint to this accumulator
							if ( ok ) {
								result = true;
								if ( accu.add(hint) )
									break OUTER;
							}
							// Same hint from all regions, AFAIK, so next value
							break; // the region loop, even if not OK
						}
					}
				}
			}
		}
		sb = null;
		return result;
	}

	/**
	 * step3 is a method because it's called twice.
	 * <p>
	 * Step 3: Until no more new cells are colored:
	 * If all bar one (called X) vs in a region see cells of the same color
	 * then paint X the SAME color.
	 * <p>
	 * nb: Step 3 is the extension in Extended Colors: an extension of Weak
	 * Coloring, so we CANNOT paint the conjugate/s of X the opposite color,
	 * because that is not always the case: if src(green)=>X(green) is always
	 * true, but src(green)=>X(green)=conjugate=>blue is not ALWAYS true.
	 *
	 * @param label "Step 3.1: " or "Step 3.2: "
	 * @param c the current color
	 * @param r the current region
	 * @param v the current value
	 * @param xSet an Idx, not pre-set
	 * @param tribe is preset to colorSet[value] and there buddies
	 * @param steps explanation of painting steps
	 * @param colorSet indices painted the current color
	 * @param bothColors cells painted either color
	 */
	private void step3(final String label, final int c, final ARegion r
			, final int v, final Idx xSet, final Idx tribe
			, final StringBuilder steps, final Idx colorSet
			, final Idx bothColors) {
		if ( xSet.setAndNotAny(r.idxs[v], tribe)
		  && xSet.size() == 1 ) {
			final int indice = xSet.peek();
			if ( wantWhy ) {
				steps.append(label)
				.append(colorCellId(c, indice))
				.append(" is the only place for ")
				.append(v).append(IN).append(r.label)
				.append(", so it is ").append(CNAME[c])
				.append(NL);
			}
			colorSet.or(xSet);
			bothColors.or(xSet);
			tribe.or(xSet, BUDDIES[indice]);
			// enqueue cell.regions for reprocessing
			dirtyRegions |= RIBS[indice] & ~IDX_SHFT[r.index];
		}
	}

	// if wantWhy, sb is created ONCE at start of findHints, then EVERY user
	// calls the sb() method, which clears and returns SAME buffer. Note that
	// StringBuilder can append StringBuilder, and toString is expensive, hence
	// all of these "colored" methods return the actual StringBuilder, not a
	// copy (a new char-array) of its contents. Just calling toString makes
	// XColoring twice as slow! This is/was the cause of wantWhy, I think, so
	// demand for wantWhy is now mostly mitigated, but it is still faster to
	// not build unused why-strings, hence it has been retained.
	// Let this be a lesson to me. And Comment It!
	private StringBuilder sb;
	private StringBuilder sb() {
		sb.setLength(0);
		return sb;
	}

	private StringBuilder colorCellId(final int color, final int indice) {
		return sb().append(CON[color]).append(CELL_IDS[indice]).append(COFF[color]);
	}

	private StringBuilder colorColorCellId(final int color, final int indice) {
		return sb().append(CON[color]).append(CNAME[color]).append(SP)
			.append(CELL_IDS[indice]).append(COFF[color]);
	}

	private StringBuilder colorCellIds(final int color, final Idx idx) {
		return sb().append(CON[color]).append(idx.ids()).append(COFF[color]);
	}

	private StringBuilder colorValue(final int color, final int value) {
		return sb().append(CNAME[color]).append(SP).append(value);
	}

	// poll dirtyRegions, a bitset that I am using as a Queue.
	// poll removes and returns the first set (1) bit (else 32, sigh).
	// @return any number less than 32 is the next index of a dirty region;
	//  else 32 (the constant NADA) meaning that the queue is empty.
	private int pollDirtyRegions() {
		final int x = dirtyRegions & -dirtyRegions; // lowestOneBit
		dirtyRegions &= ~x; // remove x from dirtyRegions
		return Integer.numberOfTrailingZeros(x); // indexOfLowestOneBit-1 which is spot-on for a 0-based index
	}

}
