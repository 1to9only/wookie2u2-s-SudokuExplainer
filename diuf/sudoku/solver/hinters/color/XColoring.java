/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGIONS;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import static diuf.sudoku.solver.hinters.HintValidator.invalidity;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/**
 * XColoring implements the Extended Coloring Sudoku solving technique.
 * <p>
 * Extended Coloring involves chains of bi-location candidates. Cells in
 * regions with 2 places for v are "colored" so that either colorA or colorB
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
 * <p>
 * KRC 2021-03-03 I was getting invalid hints from the version of this class
 * based on hobiwan's [Simple]Coloring, so I rewrote it from scratch; but it's
 * still rooted, and the code looks to me like it follows the algorithm. Either
 * my code is wrong in some way that I'm too stupid to understand (likely), or
 * the actual published algorithm is just plain wrong (unlikely).
 * <p>
 * I tried alterations (Step K:) to get it to work, without any joy. So now I
 * find myself at a loss. It finds all the hints in the test-cases, but it also
 * finds many false-positives: invalid hints. So I guess I have to declare that
 * the actual published algorithm is just plain wrong, which I do NOT think.
 * Surely anyone who published an algorithm would go to the trouble of testing
 * it over many many puzzles BEFORE they publish, to check that it doesn't do
 * unexpected things, like find false positives; so my actual conclusion is I'm
 * too stupid to understand the published algorithm, and also too stupid to
 * understand the authors intent, and so make it work despite his publication.
 * <p>
 * KRC 2021-03-03 09:35 Having failed to sort-out above invalid hint issues, I
 * have decided to password protect XColoring, so that only coders can use it.
 * My thinking is: I've tried everything I can think of to avoid/avert invalid
 * hints and either I'm misunderstanding the algorithm or it's WRONG! I think
 * it's just plain WRONG! All-though one should never underestimate one's own
 * capacity for sheer unadulterated stupidity. In which case I'm REALLY thick!
 * <pre>
 * ========================================================
 * !!!! THEREFORE Tech.XColoring is password protected !!!!
 * ========================================================
 * </pre>
 *
 * @author Keith Corlett 2021-02-27
 */
public final class XColoring extends AHinter {

	/** The first and second colors. */
	private static final int C1 = 0, C2 = 1;

	private static final int Q_SIZE = 32;			// must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;	// for this trick to work

	private static final int[] OPPOSITE_COLOR = {1, 0};

	private static boolean isEmpty(int[][] coloredIndiceQueue) {
		for ( int i=0; i<Q_SIZE; ++i )
			if ( coloredIndiceQueue[i] != null )
				return false;
		return true;
	}

	// ============================ debugger stuff ============================

	// DEBUG=true prints the "coloring journey" to stdout.
	private static final boolean DEBUG = false;
	private static void debug(String s) {
		if(DEBUG)System.out.println(s);
	}
	private void step(String s) {
		debug(s);
		steps.append(s);
		steps.append(NL);
	}
	private StringBuilder steps = new StringBuilder(1024);

	// ============================ instance stuff ============================

	/** The grid to search. */
	private Grid grid;

	/** The indices of cells in grid which maybe each value 1..9. */
	private Idx[] candidates;

	/** The IAccumulator to which any hints found are added. */
	private IAccumulator accu;

	// it's faster to construct these arrays ONCE
	// Indice Color Element Queue:
	//  first index is the queue element index
	// second index is 0=indice(0..80), 1=color(0..1)
	// * Also a healthy ICE habit.
	// * And we still have no idea what to do about melting it all. sigh.
	private final int[][] iceQueue = new int[Q_SIZE][];
	// the two indices of a conjugate pair in a region
	private final int[] conjugates = new int[2];
	// we reuse this ONE array to read the cells in an Idx
	private final Cell[] cells = new Cell[81];
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots redPots = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots setPots = new Pots();
	// we reuse this List rather than create one every time when we miss 99%.
	private final Collection<Link> links = new LinkedList<>();

	public XColoring() {
		super(Tech.XColoring);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// each hinter must enabled/disable itself to do it on the fly.
		if ( !isEnabled )
			return false;
		this.grid = grid;
		this.candidates = grid.getIdxs();
		this.accu = accu;
		boolean result;
		try {
			// find Extended Colors
			result = findXColorHints();
		} finally {
			this.grid = null;
			this.candidates = null;
			this.accu = null;
			// each cell holds the whole grid, so forget all cells!
			Arrays.fill(cells, null);
		}
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ SIMPLE COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all X-Color Trap/Wrap hints in the grid and adds them to accu.
	 *
	 * @return were any hint/s found?
	 */
	private boolean findXColorHints() {
		Cell cell; // If you need this explaining you're in the wrong codebase.
		AHint hint; // Similarly.
		int subtype; // the hint type, if any
		int i; // the uniquitious general purpose index
		int n; // the number of whatevers added to the array
		int conjugate; // the indice of the conjugate cell
		int read, write; // iceQueue read/write index
		int[] ice; // indice color element: 0=indice, 1=color
		int indice, color; // grid.cells indice is colored C0 or C1
		int c; // the indice into colorSet
		int oppositeColor; // the other color
		int dri; // dirty region index
		int resultColor; // the color to paint results in XColoringHintMulti
		boolean any; // did we find any cells?
		Idx colorSet; // current color (C1 or C2)
		Idx otherSet; // OPPOSITE color (C2 or C1)
		// presume that no hint will be found
		boolean result = false;
		// foreach value
		VALUES: for ( int v=1; v<10; ++v ) {
			debug("");
			debug("");
			debug("XColoring Value="+v);
			debug("=================");
			// foreach region
			for ( ARegion region : grid.regions ) {
				// with just 2 places for value
				if ( region.indexesOf[v].size == 2 ) {
					// --------------------------------------------------------
					// Step 1: Select a conjugate pair (the only two places for
					// v in a region). Color the first C1, and second C2.
					bothColors.set(region.idxs[v]);
					n = region.idxs[v].toArrayN(conjugates);
					assert n == 2;
					colorSets[C1].clear().add(conjugates[0]);
					colorSets[C2].clear().add(conjugates[1]);
					debug("");
					steps.setLength(0); // clear the steps string
					step("Step 1: Color conjugate pair:"
						+" green="+colorSets[C1].ids()
						+" and blue="+colorSets[C2].ids()
						+" in "+region.id
						+" on "+v);
					// --------------------------------------------------------
					// Step 2: Until no more new cells are colored, do:
					// Find an uncolored conjugate of a colored cell
					// and color it the OPPOSITE color.
					any = false;
					iceQueue[0] = new int[]{conjugates[0], C1};
					iceQueue[1] = new int[]{conjugates[1], C2};
					write = 2;
					read = 0;
					for ( ice=iceQueue[read], iceQueue[read]=null, read=(read+1)&Q_MASK;
						  ice!=null;
						  ice=iceQueue[read], iceQueue[read]=null, read=(read+1)&Q_MASK
					) {
						indice = ice[0];
						color = ice[1];
						for ( ARegion r : grid.cells[indice].regions ) {
							if ( r.indexesOf[v].size == 2
//							  // skip the region that got us here
//							  && r != region
							  // find an __uncolored__ conjugate
							  && !bothColors.contains(conjugate=r.idxs[v].otherThan(indice))
							) {
								step("    Step 2: "+CELL_IDS[indice]+" ("+(color==0?"green":"blue")+")"
									+" =conjugate=> "+CELL_IDS[conjugate]+" ("+(color==0?"blue":"green")+")"
									+" in "+r.id);
								// color it the OPPOSITE color
								oppositeColor = OPPOSITE_COLOR[color];
								colorSets[oppositeColor].add(conjugate);
								bothColors.add(conjugate);
								// add this newly colored cell to the queue
								iceQueue[write]=new int[]{conjugate, oppositeColor};
								write = (write+1) & Q_MASK;
								assert write != read; // queue undersized
								any = true;
							}
						}
					}
					// --------------------------------------------------------
					// Step 3: Until no more new cells are colored, do: If all
					// bar one (called X) v's in a region see cells of the same
					// color then paint X with the SAME color.
					// NOTE: Step 3 is the extension in Extended Colors, which
					// is an extension of Weak Coloring, so we CAN'T paint the
					// conjugate/s of X the opposite color, because that's not
					// allways the case: if src(green)=>X(green) is always true
					// but src(green)=>X(green)=conjugate=>blue is NOT ALWAYS
					// TRUE (it's just logical, which I find REALLY confusing).
					// If you conjugate then you get many more hints, but many
					// of them (~40%) are invalid, and there commonality is
					// relying on the X=>conjugate, so just don't try it, OK?
					// I did and it doesn't! } finally { comment it }
					if ( any ) {
						// foreach color
						for ( c=0; c<2; ++c ) {
							colorSet = colorSets[c];
							// uncolored cells= colorSet.BUDDIES | colorSet
							bothBuds.clear();
							colorSet.forEach((j)->bothBuds.or(BUDDIES[j]));
							bothBuds.or(colorSet);
							dirtyRegions.clear();
							any = false; // any for this color only
							// foreach region in the grid
							for ( ARegion r : grid.regions ) {
								if ( xSet.setAndNot(r.idxs[v], bothBuds).size() == 1 ) {
									step("    Step 3: "+CELL_IDS[xSet.peek()]
										+" is the only place for "+v+" in "+r.id
										+", so it's "+(c==0?"green":"blue"));
									colorSet.or(xSet);
									bothColors.or(xSet);
									indice = xSet.peek();
									bothBuds.or(xSet).or(BUDDIES[indice]);
									// enqueue cell.regions for re-examination.
									dirtyRegions.or(REGIONS[indice]);
									any = true;
								}
								// No need to re-examine this region.
								if ( any )
									dirtyRegions.remove(r.index);
							}
							// now re-examine the dirty regions, adding any new
							// ones to the tail-of-queue, until queue is empty.
							while ( (dri=dirtyRegions.poll()) > -1 ) {
								ARegion r = grid.regions[dri];
								if ( xSet.setAndNot(r.idxs[v], bothBuds).size() == 1 ) {
									step("    Step 3: "+CELL_IDS[xSet.peek()]
										+" is the only place for "+v+" in DIRTY "+r.id
										+", so it's "+(c==0?"green":"blue"));
									colorSet.or(xSet);
									bothColors.or(xSet);
									indice = xSet.peek();
									bothBuds.or(xSet).or(BUDDIES[indice]);
									// queue cell.regions for re-exam.
									dirtyRegions.or(REGIONS[indice]);
									// no need to re-exam this region.
									dirtyRegions.remove(r.index);
								}
							}
						}
						// the type of hint created
						subtype = 0;
						// clear EVERY time. sigh.
						redPots.clear(); // Type 1 only
						setPots.clear(); // Type 2 and Type 3
						resultColor = 0; // color to paint the setPots
						links.clear(); // Type 3 only
						// ------------------------------------------------
						// Step 4: Once all the coloring is done:
						// 4.2 If two-or-more cells in a region are da same
						//     color, then the OTHER color is true.
						// NOTE: Type 2 is as rare as rocking horse s__t.
						COLOR: for ( c=0; c<2; ++c ) {
							colorSet = colorSets[c];
							otherSet = colorSets[OPPOSITE_COLOR[c]];
							for ( ARegion r : grid.regions )
								// hijack xSet
								if ( xSet.setAnd(colorSet, r.idx).any()
								  && xSet.size() > 1 ) {
									n = otherSet.cellsN(grid, cells);
									for ( i=0; i<n; ++i )
										setPots.put(cells[i], new Values(v));
									step("    Step 4.2: More than one cell {"+xSet.ids()+"} in "+r.id
										+" is "+(c==0?"green":"blue")+", which is invalid"
										+", so the "+(c==0?"blue":"green")+" set"
										+" {"+colorSets[OPPOSITE_COLOR[c]].ids()+"} must be "+v);
									subtype = 2; // Type 2
									resultColor = OPPOSITE_COLOR[c];
									break COLOR; // first only
								}
						}
						if ( subtype == 0 ) {
							// 4.3 If ALL cells in region which maybe v see
							//     cells of the same color, then the OTHER
							//     color is true.
							COLOR: for ( c=0; c<2; ++c ) {
								colorSet = colorSets[c];
								otherSet = colorSets[OPPOSITE_COLOR[c]];
								// buddies of THIS color
								colorBuds.clear();
								colorSet.forEach((j)->colorBuds.or(BUDDIES[j]));
								for ( ARegion r : grid.regions )
									// hijack xSet
									if ( xSet.setAnd(colorBuds, r.idxs[v]).any()
									  && xSet.size() == r.indexesOf[v].size ) {
										n = otherSet.cellsN(grid, cells);
										for ( i=0; i<n; ++i )
											setPots.put(cells[i], new Values(v));
										step("    Step 4.3: ALL cells in "+r.id+" which maybe "+v
											+" {"+r.idxs[v].ids()+"} see "+(c==0?"green":"blue")+" cells"
											+", so the "+(c==0?"blue":"green")+" set "+colorSets[OPPOSITE_COLOR[c]].ids()+" is "+v);
										for ( int x : xSet.toArrayA() ) {
											tmp.setAnd(colorSet, BUDDIES[x]);
											links.add(new Link(grid.cells[x], v, grid.cells[tmp.poll()], v));
										}
										subtype = 3;
										resultColor = OPPOSITE_COLOR[c];
										break COLOR; // first only
									}
							}
							if ( subtype == 0 ) {
								// 4.1 If a cell sees both colored cells (A AND B),
								//     then exclude v from this cell.
								// get cells = all uncolored v's in grid.
								n = xSet.setAndNot(candidates[v], bothColors).cellsN(grid, cells);
								// foreach uncolored v in grid
								for ( i=0; i<n; ++i ) {
									cell = cells[i];
									// if this cell sees both:
									// (a) any element of colorSets[C1] AND
									// (b) any element of colorSets[C2]
									if ( cell.buds.andAny(colorSets[C1])
									  && cell.buds.andAny(colorSets[C2]) ) {
										step("    Step 4.1: cell "+cell.id+" sees"
											+" BOTH green={"+tmp.set(cell.buds).and(colorSets[C1]).ids()+"}"
											+" and blue={"+tmp.set(cell.buds).and(colorSets[C2]).ids()+"}");
										redPots.put(cell, new Values(v));
										subtype = 1; // Type 1 (type not reported)
									}
								}
							}
						}
						// create the hint, if any
						if ( subtype != 0 ) {
							boolean ok = true;
							// Type 2 or Type 3: Set multiple cells (rare).
							if ( subtype==2 || subtype==3 ) {
								hint = new XColoringHintMulti(this, v
									, subtype
									// deep-copy-off the Idx[] array, which is re-used
									, new Idx[]{new Idx(colorSets[0]), new Idx(colorSets[1])}
									, xSet.toCellSet(grid)
									, resultColor
									, steps.toString() // coloring steps which built this hint
									, new Pots(setPots) // copy-of field
									, new Pots(colorSets[0].cells(grid), new Values(v))
									, new Pots(colorSets[1].cells(grid), new Values(v))
									// copy-off the links List field, which is re-used
									, new LinkedList<>(links)
								);
								if ( HintValidator.XCOLORING_USES ) {
									if ( !HintValidator.isValidSetPots(grid, setPots) ) {
										hint.isInvalid = true;
										HintValidator.reportSetPots(grid, "XColoringMulti", invalidity, hint.toFullString());
										hint.invalidity = HintValidator.lastMessage;
										if ( Run.type != Run.Type.GUI )
											ok = false;
									}
								}
							// Type 1: Eliminations are common.
							} else {
								hint = new XColoringHint(this, v
									, new Pots(redPots)
									, new Pots(colorSets[0].cells(grid), new Values(v))
									, new Pots(colorSets[1].cells(grid), new Values(v))
									, new Idx[]{new Idx(colorSets[0]), new Idx(colorSets[1])}
									, steps // coloring steps which built this hint
								);
								if ( HintValidator.XCOLORING_USES ) {
									if ( !HintValidator.isValid(grid, redPots) ) {
										hint.isInvalid = true;
										HintValidator.report("XColoring", grid, hint.toFullString());
										hint.invalidity = HintValidator.lastMessage;
										// Show in GUI, hide in batch/testcases
										if ( Run.type != Run.Type.GUI )
											ok = false;
									}
								}
							}
							// add the hint to this accumulator
							if ( ok ) {
								debug("    ADDING hint: "+hint.toFullString());
								result = true;
								if ( accu.add(hint) )
									return result;
							}
							// Skip rest of v coz all hints are same, AFAIK
							continue VALUES; // even if not OK
						}
					}
				}
			}
		}
		return result;
	}
	// it's faster to construct Idx's ONCE
	// indices of cells which are this color
	// * C1 is 0 is colorA is green
	// * C2 is 1 is colorB is blue
	private final Idx[] colorSets = {new Idx(), new Idx()};
	// both colors = colorSets[C1] | colorSets[C2]
	private final Idx bothColors = new Idx();
	// the indexes of regions to re-examine
	private final Idx dirtyRegions = new Idx();
	// The Exception Cell, or X for short
	private final Idx xSet = new Idx();
	// buddies of this color | bothColors, and then it's reused for other stuff
	private final Idx colorBuds = new Idx();
	// the buddies of both colors
	private final Idx bothBuds = new Idx();
	// a temporary Idx: set, read, forget.
	private final Idx tmp = new Idx();

}