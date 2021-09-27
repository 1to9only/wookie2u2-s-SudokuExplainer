/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.bug;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Permutations;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * Implementation of the Bivalue Universal Grave (BUG) solving technique.
 * Supports types 1 to 4.
 */
public final class BUG extends AHinter
		implements diuf.sudoku.solver.hinters.ICleanUp
//				 , diuf.sudoku.solver.IReporter
{

//private int bug1HintCount=0, bug2HintCount=0
//			, bug3HintCount=0, bug4HintCount=0;
//private int bcCnt=0, bcPass=0;
//private int nbCnt=0, nbPass=0;
//private int npCnt=0, npPass=0;
//private int p2Cnt=0, p2Pass=0;
//private int rgCnt=0, rgPass=0;

	// Int ArrayS ~ We need 9 of them
	// saves on creating them for each call
	private static final int[][] IAS = new int[6][];
	static {
		for ( int i=2; i<6; ++i )
			IAS[i] = new int[i];
	}

	private IAccumulator accu;

	// a copy of the grid to strip potential-values from
	private final Grid stripper = new Grid();

	private Pots bcPots = null;
	private final Values allBugValues = new Values();

	// Index of common siblings
	private final Idx cmnSibsIdx = new Idx();

	public BUG() {
		super(Tech.BUG);
	}

	@Override
	public void cleanUp() {
		accu = null;
		bcPots = null;
		cmnSibsIdx.clear();
	}

//	@Override
//	public void report() {
//		Log.teeln("\nBivalueUniversalGrave");
//		Log.teef("// bug1HintCount %,d\n", bug1HintCount);
//		Log.teef("// bug2HintCount %,d\n", bug2HintCount);
//		Log.teef("// bug3HintCount %,d\n", bug3HintCount);
//		Log.teef("// bug4HintCount %,d\n", bug4HintCount);
//		Log.teef("// BugCell pass %,d of %,d = skip %4.2f%%\n"
//				, bcPass, bcCnt, Log.pct(bcCnt-bcPass, bcCnt));
//		Log.teef("// NonBug  pass %,d of %,d = skip %4.2f%%\n"
//				, nbPass, nbCnt, Log.pct(nbCnt-nbPass, nbCnt));
//		Log.teef("// NullPot pass %,d of %,d = skip %4.2f%%\n"
//				, npPass, npCnt, Log.pct(npCnt-npPass, npCnt));
//		Log.teef("// TwoPots pass %,d of %,d = skip %4.2f%%\n"
//				, p2Pass, p2Cnt, Log.pct(p2Cnt-p2Pass, p2Cnt));
//		Log.teef("// Region  pass %,d of %,d = skip %4.2f%%\n"
//				, rgPass, rgCnt, Log.pct(rgCnt-rgPass, rgCnt));
//	}

	// observed 51 calls in one analyse
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		boolean result = false;
		ARegion region;
		int card, ri, v;
		boolean firstTime = true;

		this.accu = accu;
		grid.copyTo(this.stripper); // a grid to erase maybes from

		// cleanup from the previous call
		this.bcPots = null; // bugCellsPots
		this.allBugValues.clear();
		this.cmnSibsIdx.clear();

		for ( ri=0; ri<27; ++ri ) { // foreach box, row, and col

			// skip any filled regions
			if ( (region = grid.regions[ri]).emptyCellCount == 0 )
				continue;

			V_LOOP: for ( v=1; v<10; ++v ) {

				// skip if value is placed in region, or has 2 places
				if ( (card=region.indexesOf[v].size)==0 || card==2 )
					continue;

				// search this region for the newBugCell: the only one which
				// maybe value in this region and has 3+ maybes.
				Cell newBugCell = null; // a field to save on passing it around
				for ( int i : INDEXES[region.indexesOf[v].bits] ) {
					if ( region.cells[i].maybes.size > 2 ) {
						// if there are multiple positions we can't decide
						// which is the BUG cell, so we just leave it for
						// another region to capture the single cell.
						if ( newBugCell != null )
							continue V_LOOP; // No bugCell here. move along!
						newBugCell = region.cells[i];
					}
				}
//++bcCnt;
				// BUG BugCell pass 45,309 of 46,421 = skip 2.40%
				if ( newBugCell == null )
					// no cell has more than two potential values.
					return false; // not a BUG
//++bcPass;

				// Good: a new BUG cell has been found

				// so we map newBugCell => value in bugCellsPots
				if ( bcPots == null )
					bcPots = new Pots(newBugCell, new Values(v));
				else
					bcPots.upsert(newBugCell, v);
				// and add value to allBugValues
				allBugValues.add(v); // yes really mutate THE instance

				// "strip" value off our bon-nommed grid
				try {
					Cell sc = stripper.cells[newBugCell.i];
					if ( sc.maybes.contains(v) )
						sc.canNotBe(v); // throws UnsolvableException
				} catch (UnsolvableException ex) { // should never happen
					// v was the last potential value for the cell
					return false; // not a BUG
				}

				// and add bugCells siblings to the set of common siblings
				if ( firstTime ) { // first time through to here
					firstTime = false;
					cmnSibsIdx.or(newBugCell.buds);
				} else
					cmnSibsIdx.and(newBugCell.buds);
				cmnSibsIdx.removeAll(bcPots.keySet());

				// now check if we've still got at a potential BUG
				// BUG NonBug  pass 38,532 of 45,309 = skip 14.96%
//++nbCnt;
				if ( bcPots.size()>1 && allBugValues.size>1
				  && cmnSibsIdx.none() )
					return false; // None of type 1, 2 or 3
//++nbPass;

			} // next value
		} // next region

//++npCnt;
		// BUG NullPot pass 6,479 of 8,427 = skip 23.12%
		if ( bcPots == null ) // ie we never made is down that far
			return false; // Not a BUG
//++npPass;

		// when the BUG value(s) have been removed, all remaining
		// empty cells will have exactly two potential values.
		// BUG TwoPots pass 66 of 6,479 = skip 98.98%
//++p2Cnt;
		for ( Cell sc : stripper.cells )
			if ( sc.value==0 && sc.maybes.size!=2 )
				return false; // Not a BUG // 6,213 top1465
//++p2Pass;

		// when the BUG value(s) have been removed, each region will have
		// exactly 2-or-0 positions for each value, or it's not a BUG.
		// BUG Region  pass 63 of 66 = skip 4.55%
//++rgCnt;
		for ( ri=0; ri<27; ++ri ) {
			region = stripper.regions[ri];
			for ( v=1; v<10; ++v )
				if ( (card=region.indexesOf[v].size)!=0 && card!=2 )
					return false; // Not a BUG
		}
//++rgPass;

		// Hooray: A BUG has been found !!!!

		// What type? Add the appropriate hint-type to accu
		final int numBugCells = bcPots.size();
		if ( numBugCells == 1 ) {
			// Yeah, potential BUG type-1 pattern found
			result = addBug1Hint();
		} else if ( allBugValues.size == 1 ) {
			// Yeah, potential BUG type-2 or type-4 pattern found
			result = addBug2Hint(grid);
			if ( numBugCells == 2 )
				// Yeah, Potential BUG type-4 pattern found
				result |= addBug4Hint();
		} else if ( !cmnSibsIdx.none() ) {
			if ( numBugCells == 2 )
				// Yeah, Potential BUG type-4 pattern found
				result = addBug4Hint();
			// Yeah, potential BUG type-3 pattern found
			result |= addBug3Hint(grid);
		}
		return result;
	}

	private boolean addBug1Hint() {
		Cell theBugCell = bcPots.firstKey();
		Pots redPots = new Pots();
		redPots.put(theBugCell, theBugCell.maybes.minus(allBugValues));
//++bug1HintCount;
		accu.add(new Bug1Hint(this, redPots, theBugCell, new Values(allBugValues)));
		return true;
	}

	private boolean addBug2Hint(Grid grid) {
		// check that cells were found
		if ( cmnSibsIdx==null || cmnSibsIdx.none() )
			return false;
		assert allBugValues.size == 1;
		int v = allBugValues.first(); // theBugValue
		final int sv = VSHFT[v];
		Pots redPots = null;
		for ( Cell sib : cmnSibsIdx.cells(grid) )
			if ( (sib.maybes.bits & sv) != 0 ) {
				if ( redPots == null )
					redPots = new Pots();
				redPots.put(sib, new Values(v));
			}
		if ( redPots == null )
			return false;
		// Create hint
//++bug2HintCount;
		accu.add(new Bug2Hint(this, redPots, bcPots.keySet(), v));
		return true;
	}

	/**
	 * Bug3 hints are untested as at 20 Aug 2019 because I've never come across
	 * a Sudoku puzzle which contains one. The furthest I've stepped through
	 * this method is down to {@code if(nakedSet==null) continue;}
	 * <p>Using the test data:<pre>
	 * 356#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
	 * C:\Users\User\Documents\SodukuPuzzles\Test\BUG-Type-3-ish.txt
	 * </pre>>
	 * @param grid the Grid to produce the hint for.
	 */
	private boolean addBug3Hint(Grid grid) {
		boolean result = false;
		assert !cmnSibsIdx.none();
		assert bcPots.size()!=1 && allBugValues.size!=1;
		// common cells list
		ArrayList<Cell> ccsList = new ArrayList<>(cmnSibsIdx.size());
		ARegion cmnRgn;
		for ( int rti=0; rti<3; ++rti ) { // regionTypeIndex: BOX, ROW, COL
			// Look for a region of this type that is shared by all BUG cells
			if ( (cmnRgn=Regions.common(bcPots.keySet(), rti)) == null )
				continue;
			// A common region of type rti has been found.
			// Gather other cells of this region from the grid.
			ccsList.clear();
			for ( Cell c : cmnSibsIdx.cells(grid) )
				if ( c.regions[rti] == cmnRgn )
					ccsList.add(c);
			// how many common cells are there
			final int n = ccsList.size();
			// common cells array
//			Cell[] ccsArray = ccsList.toArray(new Cell[n]);
			Cell[] ccsArray = ccsList.toArray(Cells.array(n));
			// foreach dd (degree) between (greater of 2 or n) and 6
			for ( int dd=Math.max(2, n); dd<7; ++dd ) {
				final int cc = dd - 1; // degreeMinusOne (cc is dd-1, right?)
				// NB: create these once per degree, not per permutation
				Values[] potentialValueses = new Values[dd];
				Cell[] nakedCells = Cells.array(cc); // something borrowed!
				Values otherCmnValues = new Values();
				// foreach possible combination of the missing $degree-1 cells
				P_LOOP: for ( int[] perm : new Permutations(n, IAS[cc]) ) {
					// NB: no need to clear potentials or nakedCells arrays coz
					//     they're completely overwritten before they are read
					otherCmnValues.clear(); // ie bits = size = 0;
					for ( int i=0; i<cc; ++i ) {
						// Fill array of missing naked cells
						nakedCells[i] = ccsArray[perm[i]];
						// Fill potential values array
						potentialValueses[i] = nakedCells[i].maybes;
						// Gather union of potentials
						if (otherCmnValues.add(potentialValueses[i]).size > dd)
							continue P_LOOP;
					}
					// Ensure that all values of the naked set are covered by non-bug cells
					if ( otherCmnValues.size != dd ) // size may still be LESS than dd
						continue;

					// ------------------------------------------
					// From here down performance is NOT an issue
					// ------------------------------------------

					// Get potentials for bug cells
					potentialValueses[cc] = allBugValues;
					// Search for a naked set
					Values nakedSetValues = Values.common(potentialValueses, dd);
					if ( nakedSetValues == null )
						continue;

					// -----------------------
					// UNTESTED From here down
					// -----------------------

					// One of bcPots.keySet() forms a naked set with nakedCells[]
					// Look for cells not part of the naked set, sharing the region
					Set<Cell> redCells = new HashSet<>(ccsList);
					for ( Cell c : nakedCells )
						redCells.remove(c); // exclude cells of the naked set
					redCells.removeAll(bcPots.keySet()); // exclude bug cells
					if ( redCells.isEmpty() )
						continue;
					// Ok, some cells in a common region. Look for removable potentials
					Pots redPots = new Pots();
					int redBits;
					for ( Cell cell : redCells )
						if ( (redBits=cell.maybes.bits & nakedSetValues.bits) != 0 )
							redPots.put(cell, new Values(redBits, false));
					if ( !redPots.isEmpty() ) {
//++bug3HintCount;
						result = true;
						if ( accu.add(new Bug3Hint(this, redPots, nakedCells
								, bcPots, allBugValues, nakedSetValues
								, cmnRgn)) )
							return true;
					}
				} // next Permutation
			} // next degree [2..6]
		} // next regionType
		return result;
	}

	private boolean addBug4Hint() {
		boolean result = false;
		assert bcPots.size()==2;
		assert allBugValues.size==1 || !cmnSibsIdx.none();
		// get the two BUG cells
		Cell c1, c2;
		{	// This block just localises 'it'.
			Iterator<Cell> it = bcPots.keySet().iterator();
			c1 = it.next();
			c2 = it.next();
		}
		// get the potential values common to both cells, minus the BUG values.
		Values cmnVals=c1.maybes.intersect(c2.maybes).remove(allBugValues);
		if (cmnVals.size != 1) // Uncle Fester just pissed on my gannet!
			return false; // No BUG type 4
		int value = cmnVals.first();
		// for regionType in {box, row, col}
		for ( int rti=0; rti<3; ++rti ) { // regionTypeIndex
			// Look for the region of this type shared by all bug cells
			final ARegion cmnRgn = Regions.common(bcPots.keySet(), rti);
			if ( cmnRgn != null ) {
				// Yeah! this is a BUG type 4, but does it kill any maybes?
				final Pots reds = new Pots();
				reds.putIfNotEmpty(c1, c1.maybes.minus(bcPots.get(c1), value));
				reds.putIfNotEmpty(c2, c2.maybes.minus(bcPots.get(c2), value));
				if ( !reds.isEmpty() ) {
//++bug4HintCount;
					result = true;
					if ( accu.add(new Bug4Hint(this, reds, c1, c2
							, new Pots(bcPots)
							, new Values(allBugValues)
							, value
							, cmnRgn)) )
						return true;
				}
			}
		}
		return result;
	}

}
