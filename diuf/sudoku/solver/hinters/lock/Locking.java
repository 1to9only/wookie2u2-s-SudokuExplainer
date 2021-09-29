/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.Col;
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import diuf.sudoku.Settings;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.AppliedHintsSummaryHint;
import diuf.sudoku.solver.accu.HintsAccumulator;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.hdnset.HiddenSetHint;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyHashMap;
import diuf.sudoku.utils.MyLinkedHashMap;
import diuf.sudoku.utils.Permutations;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.and;


/**
 * Implementation of Locking (Pointing and Claiming) solving techniques.
 * <p>
 * Locking is when a candidate is "locked into" a region; ie when these are
 * the only cells in a region that maybe this value then one of them MUST
 * be this value (each value must appear in each region), eliminating the
 * value from other cells in other region/s common to all cells in the set.
 * <p>
 * Note that in "standard" Sudoku with region types: box, row, and col there
 * is only one other possible type-of-region common to two-or-more cells. For
 * a box it's either a row or a col; and for a row-or-col it's a box; hence I
 * have decomposed Locking into Pointing and Claiming; because I found the term
 * Locking nonsensical when I started my Sudoku journey (I get it now), so here
 * are my definitions of my preferred "simpler" terms:<ul>
 * <li><b>Pointing</b> is when all candidates in a box are in one row-or-col,
 *  therefore we can remove all other candidates from the row-or-col.
 * <li>for example: both cells that maybe 5 in box1 are both in colC,
 *  <br>so one of those two cells must be 5,
 *  <br>therefore no other cell in colC may be 5.
 * <li><b>Claiming</b> is when all candidates in a row-or-col are in one box,
 *  therefore we can remove all other candidates from the box.
 * <li>for example: the three cells that maybe 9 in row1 are all in box2,
 *  <br>so one of those three cells must be 9,
 *  <br>therefore no other cell in box2 may be 9.
 * </ul>
 * <p>
 * Locking may use a HintsApplicumulator to apply each Pointing and Claiming
 * hint as soon as it is found, so that "all" hints are applied in one pass
 * through the grid. Then I add a AppliedHintsSummaryHint to the "normal"
 * Single/Default/Chains IAccumulator to pass nElims back to {@code apply()}
 * to keep track of "the score".
 * <p>
 * NB: If you implement Resetable you'll need to put a null apcu check in the
 * existing reset method, coz LogicalSolver calls reset when there's nothing
 * to reset; ie outside of RecursiveAnaylser; ie "normal" mode.
 * <p>
 * KRC 2020-02-29 Added mergeSiameseHints and eliminateSubsets to this already
 * too-complex class. Used only in the GUI when !wantMore && !isFilter.
 * A "Siamese" hint is when multiple pointing hints are generated for multiple
 * values in the same region, whose redPots are all in a common region.
 * Example: {@code C:/Users/User/Documents/SodukuPuzzles/Test/ClaimingFromABoxWith3EmptyCells_001.txt}
 * eliminateSubsets occurred to me after implementing siamese to remove any/all
 * hints which have been superceeded by an "upgraded" HiddenSetHint (but beware
 * it works with ALL hints in the accu), so that the best (highest score) hint
 * is selected, so that I can just press enter again.
 */
public final class Locking extends AHinter {

	private final HintsApplicumulator apcu;
	private final boolean speedMode;

	// each region of each effected cell, and it's maybes before eliminations
	private final RegionQueue dirtyRegions;

	/**
	 * Constructor for "normal mode".
	 */
	public Locking() {
		super(Tech.Locking);
		this.apcu = null;
		this.speedMode = false;
		this.dirtyRegions = null;
	}

	/**
	 * Constructor for "speed mode". Used by RecursiveAnalyser,
	 * ie RecursiveSolver, passing a HintsApplicumulator, so that my
	 * getHints applies it's hints the directly grid; so that any
	 * subsequent hints can also be found in a pass through the Grid,
	 * then I add an AppliedHintsSummaryHint (numElims = total elims)
	 * to the "normal" HintsAccumulator passed into findHints.
	 * <p>
	 * The notable bit is that the Grid mutates WHILE I'm searching it,
	 * and it all still works anyway, which is pretty damn cool. In my
	 * humble opinion, ConcurrentModificationException is programmereese
	 * for "____ing soft-cock"; it's most often thrown unnecessarily.
	 *
	 * @param apcu HintsApplicumulator
	 */
	public Locking(HintsApplicumulator apcu) {
		super(Tech.Locking);
		this.apcu = apcu;
		this.speedMode = true;
		this.dirtyRegions = new RegionQueue();
		// asserts only for techies (who java -ea) and this is a constructor,
		// so not performance critical; but it's still a hack.
		assert Debug.isClassNameInTheCallStack(7, "RecursiveAnalyser");
	}

	/**
	 * resets apcu.numElims (the total number of eliminations performed by the
	 * most recent execution of the getHints method) to 0.
	 */
	@Override
	public void reset() {
		// let it NPE if this Locking wasn't created with an apcu. If you
		// make Locking Resetable it NPE's in LogicalSolver.reset, so do
		// NOT make Locking Resetable!
		apcu.numElims = 0;
	}

	/**
	 * doSiamese is set true by LogicalSolver.getAllHints (in the GUI) when
	 * !wantMore (ie find first hint only).
	 *
	 * KRC 2020-08-20 LogicalSolverTester now makes LogicalSolver setSiamese in
	 * order to locate the puzzle causing bug in mergeSiameseHints when it find
	 * the wrong hidden triple (1 of 2) on top row -> kill all H1 candidates.
	 */
	private boolean doSiamese;
	private HiddenSet hiddenPair, hiddenTriple;
	public void setSiamese(HiddenSet hiddenPair, HiddenSet hiddenTriple) {
		doSiamese = true;
		this.hiddenPair = hiddenPair;
		this.hiddenTriple = hiddenTriple;
	}
	/** Clean-up after a setSiamese. */
	public void clearSiamese() {
		doSiamese = false;
		this.hiddenPair = null;
		this.hiddenTriple = null;
	}

	/**
	 * Searches the given grid for Pointing (box on row/col) and Claiming
	 * (row/col on box) Hints, which are added to the given HintsAccumulator.
	 * <p>
	 * S__t goes weird in "speed mode" when a HintsApplicumulator was passed to
	 * my Constructor, in which case we apply each hint now (to autosolve grid)
	 * and then pass-back the total elims in a AppliedHintsSummaryHint to
	 * getHints-caller via the "normal" HintsAccumulator passed into getHints.
	 * <p>
	 * It's all bit complicated, so writing an explanation seems harder than
	 * writing the code, and I expect the explanation is probably harder to
	 * understand than the code. I just suck at explaining stuff. sigh.
	 * <p>
	 * Note that the HintsApplicumulator has a "debug mode" where it populates
	 * a StringBuilder with the-full-string of each hint applied to the grid.
	 *
	 * @return was a hint/s found.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( speedMode ) // ie a HintsApplicumulator was provided
			return findHintsSpeedMode(grid, accu);

		// normal mode: GUI, test-cases, batch
		if ( accu.isSingle() )
			// user wants 1 hint so prefer pointing to claiming.
			// nb: "normal" short-circuiting boolean-or operator.
			return pointing(grid, accu)
				|| claiming(grid, accu);
		// GUI wants multiple hints so collect both pointing & claiming
		// nb: "unusual" bitwise-or operator, so they're both executed.
		final boolean result = pointing(grid, accu)
							 | claiming(grid, accu);
		// GUI only: siamese: remove any hints whose eliminations are a
		// subset of any other hints eliminations.
		if ( result
		  && Run.type == Run.Type.GUI // removeSubsets breaks test-cases
		  && accu instanceof HintsAccumulator // no chainers!
		  && accu.size() > 1 // and there's more than one locking hint
		  // only if isFilteringHints, so user can see all "plain" hints
		  && THE_SETTINGS.getBoolean(Settings.isFilteringHints, false) )
			// remove each hint whose elims are a subset of anothers
			removeSubsets(((HintsAccumulator)accu).getHints());
		return result;
	}

	/**
	 * speed mode: RecursiveAnalyser
	 * <p>
	 * <b>CAUTION:</b> Some Seriously Weird S__t!
	 * <p>
	 * This Locking was created by RecursiveAnalyser with a HintsApplicumulator
	 * to apply all point and claim hints in one pass through the grid. It's a
	 * bit quicker that way.
	 * <p>
	 * I do an exhaustive search, so that when a maybe is removed from a region
	 * that's already been searched we search it again. It's a bit slower here,
	 * but this exhaustive-search is faster overall, because it doesn't miss a
	 * hint that leaves recursiveSolve guessing a cell-value that can't be OK.
	 * <p>
	 * Then we accu.add an AppliedHintsSummaryHint and return have any hints
	 * been applied.
	 * 
	 * @param grid
	 * @param accu
	 * @return 
	 */
	private boolean findHintsSpeedMode(Grid grid, IAccumulator accu) {
		MyHashMap.Entry<ARegion,Values> e;
		ARegion dirtyRegion; // Dougs box hole?
		dirtyRegions.clear(); // Na, bog roll!
		final int preElims = apcu.numElims;
		// note the bitwise-or operator (|) so they're both executed.
		final boolean result = pointing(grid, apcu)
							 | claiming(grid, apcu);
		if ( result ) {
			// Second pass of the regions.
			// NB: I've never found anything in a third pass, so now it's just
			// an if (was a while loop), and we're only re-processing the dirty
			// regions, not doing all regions twice, for speed.
			while ( (e=dirtyRegions.poll()) != null ) // wax
				if ( (dirtyRegion=e.getKey()) instanceof Box ) // on
					pointFrom((Box)dirtyRegion, grid.rows, grid.cols, e.getValue().bits, grid);
				else // Row or Col // off
					claimFrom(dirtyRegion, e.getValue().bits, grid);
		}
		final int myElims = apcu.numElims - preElims;
		if ( myElims > 0 ) {
			final AHint hint = new AppliedHintsSummaryHint(Log.me(), myElims, apcu);
			if ( accu.add(hint) )
				return true;
		}
		return result;
	}

	// GUI only: remove each hint whose eliminations are a subset of those
	// in any other hint. Note that this may remove ALL LockingHint's when
	// siamese found a HiddenSetHint, rendering me a silent hint killer (near
	// impossible to debug) so if got-bug then set isFilteringHints to false,
	// and if works then bug is likely in mergeSiameseHints, not removeSubsets
	// at all (I'm pure vanilla: if I work at all then I should work always).
	//
	// NOTE: that the given hints list is the one "inside" the accumulator!
	private void removeSubsets(List<AHint> hints) {
		// get an array of the hints to avert concurrent modification error
		final int n = hints.size();
		final AHint[] array = hints.toArray(new AHint[n]);
		Pots rp1, rp2; // rp1 is the left hand side, rp2 the right.
		// foreach hint except the last
		for ( int i=0,m=n-1; i<m; ++i ) {
			if ( (rp1=array[i].redPots) == null )
				continue; // a direct hint
			// foreach subsequent hint (a forward only search)
			for ( int j=i+1; j<n; ++j ) {
				if ( (rp2=array[j].redPots) == null )
					continue; // a direct hint
				// if second is subset of first then remove second
				if ( rp2.isSubsetOf(rp1) )
					hints.remove(array[j]);
				// else if first is a subset of second then remove first
				// and break the inner-loop!
				else if ( rp1.isSubsetOf(rp2) ) {
					hints.remove(array[i]);
					break; // goto next h1
				}
			}
		}
		// sort the remaining hints
		hints.sort(AHint.BY_SCORE_DESC);
	}

	// constants to determine if a boxes indexesOf[value] are all in a row.
	// The best way to show the logic is in binary, and that costs nothing.
	// ROWs appear upside down in a left-to-right view of right-to-left bits.
	private static final int ROW1 = Integer.parseInt("000"
												   + "000"
												   + "111", 2); // 7
	private static final int ROW2 = Integer.parseInt("000"
												   + "111"
												   + "000", 2); // 56 = 7<<3
	private static final int ROW3 = Integer.parseInt("111"
												   + "000"
												   + "000", 2); // 448 = 56<<3
	// constants to determine if a boxes idxsOf[value] are all in a column.
	private static final int COL1 = Integer.parseInt("001"
												   + "001"
												   + "001", 2); // 73
	private static final int COL2 = Integer.parseInt("010"
												   + "010"
												   + "010", 2); // 146
	private static final int COL3 = Integer.parseInt("100"
												   + "100"
												   + "100", 2); // 292

	// Pointing: Box => Row/Col
	// Eg: All the cells in box 2 which maybe 9 are in column D, hence one of
	//     those 2-or-3 cells must be a 9, therefore no other cell in column D
	//     can be 9.
	// Optimisation: inspect only THE row or THE column (r2) which is common
	//               to all cells in the box base which maybe v.
	private boolean pointing(Grid grid, IAccumulator accu) {
//if ( doSiamese ) // ie we're not in the RecursiveAnalyser
//	Debug.breakpoint();
		Box box; // the current Box
		Indexes[] bio; // box.idxsOf array, indexed by value
		ARegion cover; // the region we eliminate from, either a Row or a Col
		// the hints in each box (only in the GUI, see doSiamese)
		ArrayList<LockingHint> regionHints = null;
		Cell[] cells; // the cells to go in the hint
		// i = region Index
		// v = value
		// card = cardinality: the number of set (1) bits in a bitset
		// b = box.idxsOf[v].bits mainly coz it needs a really short name
		// offset = the 0-based offset of this row/col from start of this box.
		int i, v, card, b, offset, cnt;
		// presume that no hints will be found
		boolean result = false;

		for ( i=0; i<9; ++i ) { // for each of the 9 boxes in the grid
			// we need atleast 3 empty cells to form this pattern
			if ( (box=grid.boxs[i]).emptyCellCount < 3 )
				continue;
			bio = box.indexesOf;
			if ( doSiamese )
				if ( regionHints == null ) // first eligable box
					regionHints = new ArrayList<>(9);
				else // subsequent eligable box
					regionHints.clear();
			for ( v=1; v<10; ++v ) {
				// 2 or 3 cells in a box could all be in a row or column; but
				// 4 or more can't be. 1 is a Hidden Single (not my problem),
				// and 0 means that this value is already placed in this box.
				if ( (card=bio[v].size)>1 && card<4 ) {
					// fast bit twiddling test: skip unless all v's in this box are
					// all in one row, or all in one column (ie r2).
					cover = null;
					b = bio[v].bits;						  // box's 2or3 possible positions of v are:
					if ( ((b & ROW1)==b && ((offset=0)==0))	  // all in the first 3 cells, so offset is 0
					  || ((b & ROW2)==b && ((offset=1)==1))	  // all in the second 3 cells, so offset is 1
					  || ((b & ROW3)==b && ((offset=2)==2)) ) // all in the third 3 cells, so offset is 2
						cover = grid.rows[box.top + offset];	  // therefore r2 is the row at box.top + offset
					else if ( ((b & COL1)==b && ((offset=0)==0))
						   || ((b & COL2)==b && ((offset=1)==1))
						   || ((b & COL3)==b && ((offset=2)==2)) )
						cover = grid.cols[box.left + offset];    // therefore r2 is the col at box.left + offset
					// all v's in the box are also in the cover region, so if the
					// cover region also has other locations for v it's a Pointing
					if ( cover!=null && cover.indexesOf[v].size > card ) {
						// get the 2-or-3 cells in base which maybe v
						// nb: when we get here we're ALWAYS gonna create a hint,
						// so it's OK to create the cells array which we'll need.
						cnt = box.maybe(VSHFT[v], cells=Cells.array(card));
						assert cnt == card;
						final LockingHint hint = createHint(LockType.Pointing
								, box, cover, cells, card, v, grid);
						if ( hint != null ) {
							result = true;
							if ( doSiamese )
								regionHints.add(hint);
							else
								accu.add(hint);
						}
					}
				}
			} // next value
//if ( regionHints!=null && !regionHints.isEmpty()
//	&& !Debug.isClassNameInTheCallStack(5, "RecursiveAnalyser") )
//	Debug.breakpoint();
			// merge siamese when box points-out multiple values.
			if ( doSiamese )
				switch ( regionHints.size() ) {
				case 0:
					break;
				case 1:
					if ( accu.add(regionHints.get(0)) )
						return true; // it's a SingleHintsAccumulator
					break;
				default:
					mergeSiameseHints(grid, box, regionHints, accu);
				}
		} // next box
		return result;
	}

	// Claiming: Row/Col => Box:
	// Eg: All the cells which maybe 9 in row 1 are in box 3, hence one of
	//     those 2-or-3-cells has to be a 9, therefore no other cell in box 3
	//     can be 9. Ie: Row 1 claims 9 from box 3.
	private boolean claiming(Grid grid, IAccumulator accu) {
		ARegion base; // a row (9..17) or a column (18..26)
		Indexes[] rio; // bases idxsOf array
		Box[] crossingBoxes; // the 3 boxes which intersect this base
		// the hints in each box
		ArrayList<LockingHint> regionHints = null;
		Cell[] cells;
		// i = region Index
		// v = value
		// card = cardinality: the number of elements in a bitset
		// b = rio[v].bits
		// offset = the offset of the matching box in the grid.boxs array
		int i, v, card, b, offset, cnt;
		// presume that no hints will be found
		boolean result = false;

		// for each row (9..17) and column (18..26) in the grid
		for ( i=9; i<27; ++i ) {
			// we need atleast 2 empty cells to form this pattern
			if ( (base=grid.regions[i]).emptyCellCount < 3 )
				continue;
			rio = base.indexesOf;
			crossingBoxes = base.crossingBoxs;
			if ( doSiamese )
				if ( regionHints==null )
					regionHints = new ArrayList<>(9);
				else
					regionHints.clear();
			for ( v=1; v<10; ++v ) {
				// 2 or 3 cells in base might all be in a box; 4 or more won't.
				// 1 is a hidden single, 0 means v is set, so not my problem.
				if ( (card=rio[v].size)>1 && card<4
				  // if all v's in base are in the same box.
				  // nb: b = bits of v's possible places in this region
			      // nb: ROW1/2/3 values work on cols too (despite there name)
			      // because theyre applied to the regions cells array it makes
				  // no difference which direction the region points.
				  // nb: offset is 0 if first crossingBox, 1 second, 2 third.
				  // The offset expressions are tautologies, whichre only in da
				  // if coz its cleaner to have it all in one expression verses
				  // if card; get b, ROW1, ROW2, ROW3; if crossing
				  && ( (((b=rio[v].bits) & ROW1)==b && (offset=0)==0) // bases 2or3 possible positions of v are all in the first 3 cells, so the offset is 0
					|| ((b & ROW2)==b && (offset=1)==1) // all in the second 3 cells => 1
					|| ((b & ROW3)==b && (offset=2)==2) ) // all in the third 3 cells => 2
				  // and there are some extra v's in the box to be removed
				  && crossingBoxes[offset].indexesOf[v].size > card ) {
					// Claiming found!
//// to find a test case puzzle
//if ( isDirty )
//	Debug.breakpoint();
					// borrow the cells array
					cells = Cells.array(card);
					cnt = base.maybe(VSHFT[v], cells);
					assert cnt == card;
					// create the hint and add it to the accumulator
					final LockingHint hint = createHint(LockType.Claiming, base
							, crossingBoxes[offset]
							, cells, card, v, grid);
					assert hint.isPointing == false;
					if ( hint != null ) {
						result = true; // never say never!
						if ( doSiamese )
							regionHints.add(hint);
						else
							accu.add(hint);
					}
				}
			} // next value
			// merge siamese when row/col claims multiple values.
			if ( doSiamese ) {
				switch ( regionHints.size() ) {
				case 0:
					break;
				case 1:
					if ( accu.add(regionHints.get(0)) )
						return true; // it's a SingleHintsAccumulator
					regionHints.clear();
					break;
				default:
					mergeSiameseHints(grid, base, regionHints, accu);
				}
			}
		} // next region1
		return result;
	}

	// WARN: Make a copy of cells array if you stash it, it's reused!
	// NB: a "Dynamic Plus" puzzle comes through this method 3,340 times
	// @param type tells you who called me, in error-messages for debugging
	private LockingHint createHint(LockType type, ARegion base, ARegion cover
			, Cell[] cells, int card, int valueToRemove, Grid grid) {
		// Build removable (red) potentials
		final Pots redPots = new Pots();
		// foreach cell which maybe valueToRemove in covers except bases
		final int sv = VSHFT[valueToRemove]; // shiftedValueToRemove
		for ( Cell cell : cover.cells )
			if ( (cell.maybes.bits & sv)!=0 && !base.contains(cell) )
				redPots.put(cell, new Values(valueToRemove));
		if ( redPots.isEmpty() ) {
			// this should NEVER happen coz we check that r2 has extra idxs
			// before we createHint. Developers investigate. Users no see.
			// IGNORE if "RecursiveAnalyser" is in my callstack (BFIIK)
			assert Debug.isClassNameInTheCallStack(5, "RecursiveAnalyser")
				: "BAD "+type+": No elims at "+base+AND+cover+ON+valueToRemove
				  +IN+Arrays.toString(cells)+NL+grid.toString();
			return null;
		}
		if ( speedMode )
			dirtyRegions.add(redPots.keySet());
		// Build highlighted (green) potentials
		final Pots greenPots = new Pots(cells, card, valueToRemove);
		// Build the hint from LockingHint.html
		return new LockingHint(this, valueToRemove, greenPots, redPots
				, base, cover, EMPTY_STRING);
	}

	/**
	 * doSiamese is true only in the GUI when isFilteringHints is true!
	 * EXCEPT: When LogicalSolverTester setsSiamese! (it warns on stdout)
	 * I merge 2 or 3 "Siamese" pointing or claiming hints from a single region
	 * into a single hint: replacing the old-hints with a new-hints in accu.
	 * I now also check if these multiple pointings/claimings are a HiddenSet,
	 * and if so I "upgrade" that hint with my pointings/claimings, and remove
	 * my old-hints.
	 * <p>
	 * We do this in the GUI only because it's a bit slow and only the user
	 * cares about "hint presentation". RecursiveAnalyser, Chainers, and
	 * LogicalSolverTester care about Locking hints, but couldn't give
	 * three parts of a flying-farnarkle about there "succinctness", so it's
	 * quicker to seek-and-set each in turn.
	 * <p>
	 * NOTE that I do nothing unless Settings.isFilteringHints is true.
	 * <p>
	 * NOTE that LogicalSolver.hiddenPair and LogicalSolver.hiddenTriple are
	 * used (cheekily) by Locking to upgrade siamese locking hints to
	 * the full HiddenSet hint, with extra eliminations. Also note that this
	 * necessitates the {@code boolean NakedSet.search(region)} method, which
	 * actually slows down the NakedSet search (but only a little bit).
	 * <p>
	 * NOTE that this doesn't always get it "right". Some hints which are not
	 * logically "Siamese" are merged, but I can't think of a fast way to
	 * differentiate them, so I'm just a tad over-eager. Shoot me!
	 * <p>
	 * NOTE many Naked Pairs/Triples are revealed by Siamese Locking which only
	 * does a subset of the possible eliminations; so maybe pass those regions
	 * off to a NakedSet.search(region) method, or maybe just remove the hint
	 * and leave it for NakedSet except then I'd need a 100% "Siamese" test,
	 * <p>
	 * NOTE It's All Crap Ray!
	 *
	 * @param regionHints the pointing or claiming hints in a region
	 * @param accu
	 */
	private void mergeSiameseHints(
			  Grid grid
			, ARegion region
			, ArrayList<LockingHint> regionHints
			, IAccumulator accu
	) {
		// Safety first! They should have been set by the setSiamese method,
		// or mergeSiameseHints is never called because doSiamese is false.
		if ( hiddenPair==null || hiddenTriple==null )
			return;
		final int n = regionHints.size();
		final boolean isPointing = regionHints.get(0).isPointing; // else Claiming
		final Idx idx = new Idx();
		final List<AHint> newHints = new LinkedList<>();

		// try the largest set (all) first and we're done if it works,
		// else work-down the possible sizes as far as 2
		//
		// CHANGE: I'm being lazy: This loop now iterates ONCE with size=n.
		// I kept the loop rather than translate the-break-out-logic into
		// I-don't-know-what.
		for ( int size=n; size>1; --size ) {
			// avert AIOOBE when regionHints are converted into newHints
			if ( regionHints.size() < size )
				break;
			// theseHints are a possible combo of size hints among regionHints
			final LockingHint[] theseHints = new LockingHint[size];
			// foreach possible combination of size hints in our n hints
			LOOP: for ( int[] perm : new Permutations(n, new int[size]) ) {
				// build an array of this combo of Locking hints
				for ( int i=0; i<size; ++i )
					theseHints[i] = regionHints.get(perm[i]);
				// build an Idx of all the cells in these Locking hints
				idx.set(theseHints[0].idx());
				for ( int i=1; i<size; ++i )
					idx.or(theseHints[i].idx());
				// search for a Hidden Pair or Triple depending on idx.size
				switch (idx.size()) {
				case 2:
					if ( hiddenPair.search(region, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, theseHints, size, idx) ) {
						removeAll(theseHints, regionHints);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(theseHints) ) {
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, theseHints, isPointing));
						removeAll(theseHints, regionHints);
						break LOOP; // we're done here!
					}
					break;
				case 3:
					if ( hiddenTriple.search(region, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, theseHints, size, idx) ) {
						removeAll(theseHints, regionHints);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(theseHints) ) {
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, theseHints, isPointing));
						removeAll(theseHints, regionHints);
						break LOOP; // we're done here!
					}
					break;
				}
				// just do the full set, not any subsets (too slow)
				break;
			}
		}
		// add the hint/s to the accumulator
		if ( accu.isSingle() ) {
			// add the first (most eliminative) hint
			// nb: the newHints having been "upgraded" tend (not guarantee) to
			//     eliminate more potential values. The only alternative is to
			//     use a HintsList (TreeSet by numElims descending); which we'd
			//     then use as a cache, but my brain hurts already, without the
			//     cach complication, so I give it a miss, for now.
			if ( newHints.size() > 0 )
				accu.add(newHints.get(0));
			else if ( regionHints.size() > 0 )
				accu.add(regionHints.get(0));
		} else {
			// add all available hints
			accu.addAll(newHints);
			for ( AHint h : regionHints )
				accu.add(h);
		}
		regionHints.clear();
	}

	// add my eliminations to hisHint; and claim redValues from common regions.
	// NB: done in a method just to not repeat the code (keeps growing).
	// NB: HiddenSets were added later, so I'm not designed just hacked-in.
	private boolean addElims(
			  HiddenSetHint hisHint // from NakedSet
			, Grid grid
			, List<AHint> newHints
			, LockingHint[] theseHints // the Locking hints
			, int size // number of theseHints
			, Idx idx // of the cells in the Locking hints
	) {
		// KRC BUG 2020-08-20 888#top1465.d5.mt UnsolvableException from apply.
		// Ignore this HiddenSet hint if hisReds do NOT intersect myReds, to
		// NOT upgrade Claiming's into a HiddenSet that is in another Box.
		int myReds = 0;
		for ( int i=0; i<size; ++i )
			myReds |= theseHints[i].redPots.valuesOf();
		if ( (hisHint.hdnSetValues.bits & myReds) == 0 )
			return false;

		// add my Pointing/Claiming eliminations to the HiddenSetHint's redPots
		final Pots hisReds = hisHint.redPots;
		for ( int i=0; i<size; ++i )
			hisReds.upsertAll(theseHints[i].redPots);

		// and also claim the values of the HiddenSet from each common region.
		// Note that point/claim elims are from only ONE common region and we
		// want the other one (if any) also. Q: How to calc "the other one"?
		// A: Pass down the frickin point/claim region ya putz! Sigh.
		final Values hdnSetVals = hisHint.hdnSetValues;
		final Idx tmp = new Idx(); // just working storage for r.idxOf
		final Cell[] cells = grid.cells; // just working storage for r.idxOf
		// foreach region common to all the cells in theseHints
		for ( ARegion r : commonRegions(cells, idx) )
			r.idxOf(hdnSetVals.bits, tmp).andNot(idx).forEach(cells, (cell) ->
				hisReds.upsert(cell, cell.maybes.intersect(hdnSetVals)));
		// and add hisHint to newHints.
		newHints.add(hisHint);
		return true;
	}

	/**
	 * Return the regions (expect 1 or 2 of them, so make it 3 to be safe)
	 * common to all cells in the given idx.
	 * @param cells the cells of the grid we're solving
	 * @param idx containing indices you want
	 * @return a {@code ArrayList<ARegion>} may be empty, but never null.
	 */
	private ArrayList<ARegion> commonRegions(Cell[] cells, Idx idx) {
		assert idx.size() > 1;
		final int[] idxArray = idx.toArrayA(); // get an array ONCE
		final ArrayList<ARegion> result = new ArrayList<>(2);
		ARegion commonRegion, cellsRegion;
		for ( int rti=0; rti<3; ++rti ) {
			commonRegion = null;
			for ( int i : idxArray ) {
				cellsRegion = cells[i].regions[rti];
				if ( commonRegion == null ) // the first cell
					commonRegion = cellsRegion;
				else if ( cellsRegion != commonRegion ) {
					commonRegion = null;
					break; // not all cells share a region of this type
				}
			}
			if ( commonRegion != null )
				result.add(commonRegion);
		}
		return result;
	}

	// Do the redPots (aka reds) of lockingHints all share a common region?
	// If not then this ain't Siamese, just disjunct hints from one region,
	// which is rare but does happen (about a dozen times in top1465).
	private boolean redsAllShareARegion(LockingHint[] lockingHints){
		// working storage is for speed only.
		// we need 2 of them because set.retainAll(set) is nonsensical.
		// nb: if I wrote retainAll it would assert c!=this;
		final ArrayList<ARegion> crs = Regions.clear(WS1); // commonRegions
		final ArrayList<ARegion> ws2 = Regions.clear(WS2); // working storage
		boolean first = true;
		for ( LockingHint lh : lockingHints )
			if ( first ) {
				Regions.common(lh.redPots.keySet(), crs);
				first = false;
			} else
				crs.retainAll(Regions.common(lh.redPots.keySet(), ws2));
		return !crs.isEmpty();
	}
	private final ArrayList<ARegion> WS1 = new ArrayList<>(3);
	private final ArrayList<ARegion> WS2 = new ArrayList<>(3);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ Johnny come lately ~~~~~~~~~~~~~~~~~~~~~~~~~~
	// the whole "speed mode" thing was added after the fact. It's only an
	// accomplice to puzzle murder, and cannot be held accountable for it's
	// taste in art, music, or eliminations. The wide blue gander. Hoooonk!

	/**
	 * Used in error messages, to tell the programmer which method called
	 * createHint, just to make the errors a bit more traceable. sigh.
	 */
	private static enum LockType {
		  Pointing
		, Claiming
		, PointFrom
		, ClaimFrom
	}

	private boolean pointFrom(final Box box, final Row[] rows, final Col[] cols
			, final int cands, final Grid grid) {
		int card;
		boolean result = false;
		for ( int v : VALUESES[cands] ) {
			if ( (card=box.indexesOf[v].size)>1 && card<4 ) {
				final int b = box.indexesOf[v].bits;
				if ( (b & ROW1) == b )
					result |= pointFromElims(rows[box.top], box, v, card, grid);
				else if ( (b & ROW2) == b )
					result |= pointFromElims(rows[box.top + 1], box, v, card, grid);
				else if ( (b & ROW3) == b )
					result |= pointFromElims(rows[box.top + 2], box, v, card, grid);
				else if ( (b & COL1) == b )
					result |= pointFromElims(cols[box.left], box, v, card, grid);
				else if ( (b & COL2) == b )
					result |= pointFromElims(cols[box.left + 1], box, v, card, grid);
				else if ( (b & COL3) == b )
					result |= pointFromElims(cols[box.left + 2], box, v, card, grid);
			}
		}
		return result;
	}
	// called ONLY by above pointFrom, to handle it's eliminations, rather than
	// farnackle my way around repeating the same code multiple times.
	// * a line is a row or a col
	// * card is the number of cells in the box which maybe v
	// * the grid is currently only used for error messages.
	// * I'm only called in speed mode, and so I add hints to the apcu.
	private boolean pointFromElims(final ARegion line, final Box box
			, final int v, final int card, final Grid grid) {
		// if v's in line other than those in the line-box-intersection
		if ( line.indexesOf[v].size > card ) {
			final Cell[] cells;
			box.maybe(VSHFT[v], cells=Cells.array(card));
			final AHint hint = createHint(LockType.PointFrom, box, line
					, cells, card, v, grid);
			if ( hint != null ) {
				apcu.add(hint);
				return true;
			}
		}
		return false;
	}

	// Note that I'm only called in speed mode, so add my hints to the apcu!
	private boolean claimFrom(final ARegion line, final int cands, final Grid grid) {
		Cell[] cells;
		int card, b, offset;
		boolean result = false;
		for ( int v : VALUESES[cands] ) {
			if ( (card=line.indexesOf[v].size)>1 && card<4 ) {
				b = line.indexesOf[v].bits;
				if ( ( ((b & ROW1)==b && (offset=0)==0)
					|| ((b & ROW2)==b && (offset=1)==1)
					|| ((b & ROW3)==b && (offset=2)==2) )
				  && line.crossingBoxs[offset].indexesOf[v].size > card ) {
					line.maybe(VSHFT[v], cells=Cells.array(card));
					final AHint hint = createHint(LockType.ClaimFrom, line
							, line.crossingBoxs[offset], cells, card, v, grid);
					if ( hint != null ) {
						result |= true; // never say never!
						apcu.add(hint);
					}
				}
			}
		}
		return result;
	}

	// remove all theseHints from regionHints
	// nb: done in a loop coz theseHints is an array, not a collection. Sigh.
	private void removeAll(LockingHint[] theseHints, ArrayList<LockingHint> regionHints) {
		for ( LockingHint h : theseHints )
			regionHints.remove(h);
	}

	/**
	 * {@link #dirtyRegions} is a MyLinkedHashMap plus add(cells) to upsert
	 * (update or insert) all these cells and all of there maybes. Note that
	 * we use MyLinkedHashMap's poll() method, which is not in java.util.Map.
	 */
	private static class RegionQueue extends MyLinkedHashMap<ARegion, Values> {
		private static final long serialVersionUID = 1459048958903L;
		private void add(Iterable<Cell> cells) {
			for ( Cell cell : cells )
				for ( ARegion r : cell.regions ) {
					Values existing = super.get(r);
					if ( existing == null )
						super.put(r, new Values(cell.maybes));
					else
						existing.add(cell.maybes);
				}
		}
	}

}
