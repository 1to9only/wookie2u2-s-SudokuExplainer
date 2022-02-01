/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKu's SimpleColoring class, by Bernhard Hobiger,
 * published under GNU's GPL licence. Kudos to hobiwan. Mistakes are mine. KRC.
 *
 * Here's hobiwans standard licence statement:
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

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.RIBS;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.solver.hinters.Validator.reportRedPots;
import static diuf.sudoku.solver.hinters.Validator.reportSetPots;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.SP;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import static diuf.sudoku.solver.hinters.Validator.VALIDATE_XCOLORING;
import static diuf.sudoku.solver.hinters.Validator.validOffs;
import static diuf.sudoku.solver.hinters.Validator.validOns;
import static java.lang.Integer.numberOfTrailingZeros;

/**
 * XColoring implements the Extended Coloring Sudoku solving technique.
 * <p>
 * Extended Coloring involves chains of bi-location candidates: Cells in
 * regions with 2 places for v are "colored", so that either colorA or colorB
 * contains v. The variable v is the Coloring candidate value, BTW.
 * <p>
 * Then X-forcing chains are located: if we presume this colored cell contains
 * v then some other cell must also contain v because it's the only location
 * remaining in a region.
 * <p>
 * Then cells which see both colors are eliminated, because either colorA
 * contains v or colorB contains v. If any cell sees two-or-more cells of the
 * same color, then the OTHER color must contain v. Similarly if all cells in
 * a region see cells of the same color then the OTHER color must contain v.
 * <pre>
 * KRC 2021-03-03 I was getting invalid hints from the version of this class
 * based on hobiwan's SimpleColoring, so I rewrote it from scratch; but it's
 * still rooted, and the code looks to me like it follows the algorithm. Either
 * my code is wrong in some way that I'm too stupid to understand (likely), or
 * the actual published algorithm is just plain wrong (unlikely).
 *
 * I tried alterations (Step K:) to get it to work, without any joy. So now I
 * find myself at a loss. It finds all the hints in the test-cases, but it also
 * finds many false-positives: invalid hints. So reluctantly declare that the
 * actual published algorithm is wrong, which I do NOT think. sigh.
 *
 * Surely anyone who published an algorithm would go to the trouble of testing
 * it over many many puzzles BEFORE they publish, to check that it doesn't do
 * unexpected things, like find false positives; so my actual conclusion is I'm
 * too stupid to understand the published algorithm, and also too stupid to
 * understand the authors intent, and so make it work despite his publication.
 *
 * KRC 2021-03-03 09:35 Having failed to sort-out above invalid hint issues, I
 * have decided to password protect XColoring, so that only coders can use it.
 * My thinking is: I've tried everything I can think of to avoid/avert invalid
 * hints and either I'm misunderstanding the algorithm or it's WRONG! I think
 * it's just plain WRONG! Allthough one should never underestimate one's own
 * capacity for sheer unadulterated stupidity.
 *
 * KRC 2021-05-11 At some time in the past I fixed the above invalid hint issue
 * but forgot to comment on it. XColoring is no longer password protected. The
 * problem with coloring is that if any of it is wrong then it's all wrong.
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

	// 4.2: Do we seek multiple cells in a region are the same color.
	// None in top1465, with "normal" Coloring first.
	// nb: false just 50ms faster over top1465, which is undetectable in GUI.
	private static final boolean DO_MULTIPLES = true;

	/** The first and second colors. */
	private static final int GREEN=0, BLUE=1;
	private static final String GREEN_TAG="green", BLUE_TAG="blue";
	private static final String[] CC = {GREEN_TAG, BLUE_TAG}; // current color
	private static final String[] OC = {BLUE_TAG, GREEN_TAG}; // opposite color

	// the opposite color
	private static final int[] OPPOSITE = {1, 0};

	// the iceQ size and mask
	private static final int Q_SIZE = 32;			// must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;	// for this trick to work

	// ============================== debug stuff =============================

//	// DEBUG=true prints the "coloring journey" to stdout.
//	private static final boolean DEBUG = false;
//	private static void debug(String s) {
//		if(DEBUG)System.out.println(s);
//	}

	// ============================ instance stuff ============================

	/** The grid to search. */
	private Grid grid;
	/** The cells in the grid to search. */
	private Cell[] gridCells;
	/** The regions in the grid to search. */
	private ARegion[] gridRegions;
	/** The indices of cells in grid which maybe each value 1..9. */
	private Idx[] gridIdxs;

	/** The IAccumulator to which any hints found are added. */
	private IAccumulator accu;

	/** Should we accrue an explanation of coloring steps. */
	private boolean wantWhy;

	// Indice Color Element Queue:
	// first index is the queue element index: 0..Q_SIZE-1
	// second index is the color index, C1 or C2
	private final int[][] iceQ = new int[Q_SIZE][];
	// we reuse this ONE array to read the cells in an Idx
	private final Cell[] myCells = new Cell[GRID_SIZE];
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots reds = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots setPots = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots oranges = new Pots();
	// we reuse this List rather than create one every time when we miss 99%.
	private final Collection<Link> links = new LinkedList<>();

	// The steps StringBuilder contains an explanation of why this hint exists.
	// It's part of the hint-HTML.
	private final StringBuilder steps = new StringBuilder(1024);

	// it's faster to construct Idx's ONCE
	// indices of cells which are this color
	// * GREEN is 0 is the first color
	// * BLUE is 1 is the second color
	private final Idx green, blue;
	private final Idx[] colorSets = {
			  green = new Idx()
			, blue = new Idx()
	};
	// both colors = colorSets[C1] | colorSets[C2]
	private final Idx bothColors = new Idx();
	// The Exception Cell, or X for short
	private final Idx xSet = new Idx();
	// buddies of this color | bothColors, and then it's reused for other stuff
	private final Idx colorBuds = new Idx();
	// the buddies of both colors
	private final Idx tribe = new Idx();
	// a temporary Idx: set, read, forget.
	private final Idx tmp = new Idx();

	// a bitset of indexes of regions that are "dirty", ergo a queue of regions
	// to be reprocessed, added by search and read by pollDirtyRegions.
	// NB: dirtyRegions finds 0 elims in top1465, so removed it, but take twice
	// as long (a WTF), so I reinstated it, but it STILL takes twice. BIG WTF!
	private int dirtyRegions;

	public XColoring() {
		super(Tech.XColoring);
	}

	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// each hinter must enabled/disable itself to do it on the fly.
		if ( !isEnabled ) {
			return false;
		}
		this.grid = grid;
		this.gridRegions = grid.regions;
		this.gridCells = grid.cells;
		this.gridIdxs = grid.idxs;
		this.accu = accu;
		// The batch doesn't want explanations. They're unused and slow.
		this.wantWhy = Run.type != Run.Type.Batch;
		boolean result;
		try {
			// find all/first X-Color hints
			result = search();
		} finally {
			this.grid = null;
			this.gridRegions = null;
			this.gridCells = null;
			this.gridIdxs = null;
			this.accu = null;
			// each cell holds the whole grid
			Arrays.fill(myCells, null);
//			Cells.cleanCasA();
		}
		return result;
	}

	/**
	 * Append 's' to the steps StringBuilder, followed by a newline.
	 *
	 * @param s to append
	 */
	private void step(String s) {
//		debug(s);
		steps.append(s).append(NL);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ SIMPLE COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all X-Color Trap/Wrap hints in the grid and adds them to accu.
	 *
	 * @return were any hint/s found?
	 */
	private boolean search() {
		Cell cell; // If you need an explanation you're in the wrong codebase.
		Idx buds; // an Idx of the buddies of the current cell.
		AHint hint; // Similarly.
		ARegion dr; // a dirty region, to be re-processed.
		Idx colorSet // current color (C1 or C2)
		  , otherSet; // OPPOSITE color (C2 or C1)
		int[] ice // indice color element, indexes [indice][color]
		    , riva; // VALUESES[region.ridx[v].bits]
		int read, write // iceQ read/write index
		  , i // the uniquitious general purpose index
		  , n // the number of whatevers added to the array
		  , a, b // the indices of the cells in this conjugate pair
		  , conjugate // the indice of the conjugate cell
		  , indice, color // grid.cells indice is colored C0 or C1
		  , subtype // the hint type, if any
		  , c // the indice into colorSet
		  , o // the OPPOSITE_COLOR to c
		  , dri // dirty region index
		  , resultColor; // the color to paint results in XColoringHintMulti
		boolean any; // did we find any cells?
		// presume that no hint will be found
		boolean result = false;
		// foreach value
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			final int fsv = VSHFT[v]; // finalShiftedValue, for lambdas. sigh.
//			debug("");
//			debug("");
//			debug("XColoring Value="+v);
//			debug("=================");
			// foreach region in the grid
			for ( ARegion r : gridRegions ) {
				// with 2 places for v (ie a conjugate pair in region on v)
				if ( r.ridx[v].size == 2 ) {
					// --------------------------------------------------------
					// Step 1: Select a conjugate pair (the only two places for
					// v in a region). Color the first C1, and second C2.
					bothColors.set(r.idxs[v]);
					// region.ridx[v] contains 2 cells: 'a' and 'b'
					// which we set in the 2 colorSets: green and blue
					riva = INDEXES[r.ridx[v].bits];
					green.clear().add(a = r.cells[riva[0]].i);
					blue.clear().add(b = r.cells[riva[1]].i);
//					debug("");
//					debug("a="+a+", b="+b);
					if ( wantWhy ) {
						steps.setLength(0); // clear the steps string
						step(Frmt.getSB(64).append("Step 1: Color conjugate pair: green=")
						  .append(green.ids())
						  .append(" and blue=").append(blue.ids())
						  .append(IN).append(r.id)
						  .append(ON).append(v).toString());
					}
					// --------------------------------------------------------
					// Step 2: Until no more new cells are colored, do:
					// Find an uncolored conjugate of a colored cell
					// and color it the OPPOSITE color.
					any = false;
					// initialise the Indice Color Element Queue
					iceQ[0] = new int[]{a, GREEN};
					iceQ[1] = new int[]{b, BLUE};
					write = 2;
					read = 0;
					// nb: this is just iceQ.poll(), without a method call,
					// which is the main reason why this mess is faster.
					for ( ice=iceQ[read], iceQ[read]=null, read=(read+1)&Q_MASK;
						  ice!=null;
						  ice=iceQ[read], iceQ[read]=null, read=(read+1)&Q_MASK
					) {
						// foreach region containing this cell
						for ( ARegion r2 : gridCells[ice[0]].regions ) {
							if ( r2.ridx[v].size == 2 ) {
//handle generate trouble, which has disappeared. I Still Have No Idea!
//								// XColoring is first use of region.idxs
//								if ( r2.idxs[v].size() != 2 ) {
//									//recover(r2, v);
//									throw new UnsolvableException();
//								}
								// and my conjugate is not colored
								if ( !bothColors.has(conjugate=r2.idxs[v].otherThan(ice[0])) ) {
									indice = ice[0];
									color = ice[1];
									if ( wantWhy ) {
										step(Frmt.getSB(64).append("    Step 2: ")
										  .append(CELL_IDS[indice]).append(" (")
										  .append(CC[color]).append(") =conjugate=> ")
										  .append(CELL_IDS[conjugate]).append(" (")
										  .append(OC[color]).append(") in ")
										  .append(r2.id).toString());
									}
									// color it the OPPOSITE color
									colorSets[OPPOSITE[color]].add(conjugate);
									bothColors.add(conjugate);
									// add this newly colored cell to the queue
									iceQ[write]=new int[]{conjugate, OPPOSITE[color]};
									write = (write+1) & Q_MASK;
									assert write != read; // queue undersized
									any = true;
								}
							}
						}
					}
					// --------------------------------------------------------
					// Step 3: Until no more new cells are colored, do: If all
					// bar one (called X) v's in a region see cells of the same
					// color then paint X with the SAME color.
					// NB: Step 3 is the extension in Extended Colors, which is
					// an extension of Weak Coloring, so we CAN'T paint the
					// conjugate/s of X the opposite color, because that's not
					// allways the case: if src(green)=>X(green) is always true
					// but src(green)=>X(green)=conjugate=>blue is NOT ALWAYS
					// TRUE (it's just logical, which I find REALLY confusing).
					// If you conjugate then you get many more hints, but many
					// of them (~40%) are invalid, and there commonality is
					// relying on the X=>conjugate, so don't try it. I did and
					// it doesn't! } finally { comment it }
					if ( any ) {
						// foreach color
						for ( c=0; c<2; ++c ) {
							// tribe = colorSet | colorSet.BUDDIES
							tribe.set(colorSet=colorSets[c]);
							colorSet.forEach((j)->tribe.or(BUDDIES[j]));
							dirtyRegions = 0;
							// foreach region in the grid
							for ( ARegion r2 : gridRegions ) {
								// if ONE uncolored v remains in region
								if ( xSet.setAndNot(r2.idxs[v], tribe).size() == 1 ) {
									indice = xSet.peek();
									if ( wantWhy ) {
										step(Frmt.getSB(64).append("    Step 3: ")
										  .append(CELL_IDS[indice])
										  .append(" is the only place for ")
										  .append(v).append(IN).append(r2.id)
										  .append(", so it's ").append(CC[c])
										  .toString());
									}
									colorSet.or(xSet);
									bothColors.or(xSet);
									tribe.or(xSet).or(BUDDIES[indice]);
									// enqueue cell.regions for reprocessing
									dirtyRegions |= RIBS[indice];
								}
								// don't reprocess this region
								dirtyRegions &= ~IDX_SHFT[r2.index];
							}
							// now reprocess the dirty regions, adding any new
							// ones to the queue, until the queue is empty.
							// 32 means dirtyRegions==0, ie the queue is empty.
							while ( (dri=pollDirtyRegions()) != 32 ) {
								dr = gridRegions[dri];
								// if ONE uncolored v remains in region
								if ( xSet.setAndNot(dr.idxs[v], tribe).size() == 1 ) {
									// UNTESTED: never get here in top1465!
									indice = xSet.peek();
									if ( wantWhy ) {
										step("    Step 3: "+CELL_IDS[indice]
											+" is the only place for "+v+" in "
											+dr.id+", so it's "+CC[c]);
									}
									colorSet.or(xSet);
									bothColors.or(xSet);
									tribe.or(xSet).or(BUDDIES[indice]);
									// reprocess my regions, except this one
									dirtyRegions |= RIBS[indice] & ~IDX_SHFT[dr.index];
								}
							}
						}
						// the type of hint created
						subtype = 0;
						// clear EVERY time. sigh.
						reds.clear(); // Type 1 only
						setPots.clear(); // Type 2 and Type 3
						resultColor = 0; // color to paint the setPots
						links.clear(); // Type 3 only
						oranges.clear();
						// ------------------------------------------------
						// Step 4: Once all the coloring is done:
						// 4.2 If multiple cells in a region are the same
						//     color, then the OTHER color is true.
						// NB: Type 2 is as rare as rocking horse s__t.
						// None in top1465, with "normal" Coloring first.
						if ( DO_MULTIPLES ) {
							COLOR: for ( c=0; c<2; ++c ) {
								o = OPPOSITE[c];
								colorSet = colorSets[c];
								otherSet = colorSets[o];
								for ( ARegion r2 : gridRegions ) {
									// nb: I'm hijacking xSet
									if ( xSet.setAndMany(colorSet, r2.idx) ) {
										if ( wantWhy ) {
											step(Frmt.getSB(64)
											  .append("    Step 4.2: Multiple cells {")
											  .append(xSet.ids()).append("} in ")
											  .append(r2.id).append(" are ").append(CC[c])
											  .append(", which is invalid, so the ")
											  .append(OC[c]).append(" set {")
											  .append(otherSet.ids())
											  .append("} must be true, ie ").append(v)
											  .toString());
										}
										setPots.upsertAll(otherSet, grid, v);
										resultColor = o;
										subtype = 2; // Type 2
										break COLOR; // first only
									}
								}
							}
						}
						if ( subtype == 0 ) {
							// 4.3 If ALL cells in region which maybe v see
							//     cells of the same color, then the OTHER
							//     color is true.
							COLOR: for ( c=0; c<2; ++c ) { // GREEN, BLUE
								colorSet = colorSets[c];
								otherSet = colorSets[OPPOSITE[c]];
								// colorBuds := cells seen by the colorSet
								colorBuds.clear();
								colorSet.forEach((j)->colorBuds.or(BUDDIES[j]));
								for ( ARegion r2 : gridRegions ) {
									// if ALL v's in r2 see this colored cells
									if ( xSet.setAndAny(colorBuds, r2.idxs[v])
									  && xSet.size() == r2.ridx[v].size ) {
										if ( wantWhy ) {
											step(Frmt.getSB(64)
											  .append("    Step 4.3: ALL cells in ")
											  .append(r2.id).append(" which maybe ")
											  .append(v).append(" {")
											  .append(r2.idxs[v].ids())
											  .append("} see a ").append(CC[c])
											  .append(SP).append(v)
											  .append(", which is invalid, so the ")
											  .append(OC[c]).append(" set {")
											  .append(otherSet.ids())
											  .append("} must be true, ie ")
											  .append(v).toString());
											// batch no use oranges/links
											// nb: lambda is too painful.
											try ( final IALease lease = xSet.toArrayLease() ) {
												for ( int s : lease.array ) {
													oranges.put(gridCells[s], fsv);
													// first (random) bud is ok
													links.add(new Link(s, v, tmp.setAnd(colorSet, BUDDIES[s]).poll(), v));
												}
											}
										}
										setPots.upsertAll(otherSet, grid, v);
										resultColor = OPPOSITE[c];
										subtype = 3;
										break COLOR; // first only
									}
								}
							}
							if ( subtype == 0 ) {
								// 4.1 If a cell sees both GREEN and BLUE,
								//     then exclude v from this cell.
								// myCells := all uncolored v's in grid.
								if ( xSet.setAndNotAny(gridIdxs[v], bothColors) ) {
									n = xSet.cellsN(grid, myCells);
									// foreach uncolored v in grid
									for ( i=0; i<n; ++i ) {
										// if cell sees both GREEN and BLUE
										if ( (buds=(cell=myCells[i]).buds).intersects(green)
										  && buds.intersects(blue) ) {
											if ( wantWhy ) {
												step("    Step 4.1: cell "+cell.id+" sees"
													+" BOTH green={"+tmp.setAnd(buds, green).ids()+"}"
													+" and blue={"+tmp.setAnd(buds, blue).ids()+"}");
											}
											reds.put(cell, fsv);
											subtype = 1; // Type 1 (type not reported)
										}
									}
								}
							}
						}
						// create the hint, if any
						if ( subtype != 0 ) {
// XColoring: COUNTS=[0, 229, 0, 39, 0, 0, 0, 0, 0, 0]
// There are 0 Type 2's
//							++COUNTS[subtype];
							boolean ok = true;
							// Type 2 or Type 3: Set multiple cells (rare).
							if ( subtype==3 || subtype==2 ) {
								hint = new XColoringHintBig(this, v, subtype
									, new Idx[]{new Idx(green), new Idx(blue)}
									, xSet.toCellSet(gridCells)
									, resultColor
									, steps.toString() // coloring steps which built this hint
									, new Pots(setPots) // copy-off for reuse
									, new Pots(green, grid, VSHFT[v], DUMMY)
									, new Pots(blue, grid, VSHFT[v], DUMMY)
									, new LinkedList<>(links) // copy-off for reuse
									, new Pots(oranges) // Type 3 causal cell-values
									, null // region
								);
								// clear fields for next time
								steps.setLength(0);
								setPots.clear();
								links.clear();
								oranges.clear();
								if ( VALIDATE_XCOLORING ) {
									if ( !validOns(grid, setPots) ) {
										hint.setIsInvalid(true);
										hint.invalidity = prevMessage;
										reportSetPots(tech.name()+"Multi", grid, prevMessage, hint.toFullString());
										if ( Run.type != Run.Type.GUI ) {
											ok = false;
										}
									}
								}
							// Type 1: Eliminations are common.
							} else {
								hint = new XColoringHint(this, v, reds.copyAndClear()
									, new Pots(green, grid, VSHFT[v], DUMMY) // greens
									, new Pots(blue, grid, VSHFT[v], DUMMY) // blues
									, new Idx[] {new Idx(green), new Idx(blue)}
									, steps.toString()
									, null
								);
								// clear fields
								steps.setLength(0);
								if ( VALIDATE_XCOLORING ) {
									if ( !validOffs(grid, reds) ) {
										hint.setIsInvalid(true);
										hint.invalidity = prevMessage;
										reportRedPots(tech.name(), grid, hint.toFullString());
										// Show in GUI, hide in batch/testcases
										if ( Run.type != Run.Type.GUI ) {
											ok = false;
										}
									}
								}
							}
							// add the hint to this accumulator
							if ( ok ) {
//								debug("    ADDING hint: "+hint.toFullString());
								result = true;
								if ( accu.add(hint) ) {
									return result;
								}
							}
							// Same hint from all regions, AFAIK, so next value
							break; // the region loop, even if not OK
						}
					}
				}
			}
		}
		return result;
	}

	// poll the dirtyRegions bitset: remove and return it's first set (1) bit.
	private int pollDirtyRegions() {
		int x = dirtyRegions & -dirtyRegions; // lowestOneBit
		dirtyRegions &= ~x; // remove x
		return numberOfTrailingZeros(x);
	}

//either try to recover first, or just throw yourself
//	// trouble in paradise: the ridx[v].size==2 but idxs[v].size()!=2
//	// so for a start reindex this region, if that doesn't fix it reindex
//	// the whole bloody grid, and if that still doesn't fix it then give up.
//	private void recover(final ARegion r, final int v) {
//		// TROUBLE
//		r.rebuildAllS__t();
//		if ( r.ridx[v].size == 2
//		  && r.idxs[v].size() != 2 ) {
//		    // STILL TROUBLE
//			grid.rebuild();
//		}
//		if ( r.ridx[v].size == 2
//		  && r.idxs[v].size() != 2 ) {
//		    // TOTALLY ROOTED
//			throw new UnsolvableException(); // which stops the batch
//		}
//	}

}
