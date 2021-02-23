/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * Implementation of the Hidden Set Sudoku solving technique:
 * (Hidden Pair=2, Hidden Triple=3, Hidden Quad=4).
 */
public final class HiddenSet extends AHinter {

	private static final String ChainersHintsAccumulator =
			"diuf.sudoku.solver.hinters.chain.ChainersHintsAccumulator";

	// the Permutations Array (used by the Permutations class)
	private final int[] thePA;

	// candidateValues: values with 2..$degree possible positions in the region
	private final int[] candidateValues = new int[9];

	public HiddenSet(Tech tech) {
		super(tech);
		assert degree>1 && degree<=5; // I've tried 5: None in top1465. They do pop-up in get all hints though.
		this.thePA = new int[degree]; // for Permutations
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		final boolean exitEarly = accu.isSingle();
		boolean result = false;
		for ( ARegion r : grid.regions ) // 9boxs+9rows+9cols
			if ( (result |= search(r, grid, accu)) && exitEarly )
				break;
		return result;
	}

	/**
	 * This one is weird. Locking finds Naked Pairs and Naked Triples
 but only has the capacity to perform a subset of the eliminations, so
 it needs a way to ask me (NakedPair and NakedTriple) "Is this a Naked
 Set, and if so can you do the eliminations for me. Hence this method is
 public, with variables created for each region, which is a bit slower,
 but shows the user ALL eliminations from "the most eliminative pattern"
 available.
	 *
	 * @param r the ARegion to search
	 * @param grid the Grid to search (for debug only where It IS required!)
	 * @param accu the IAccumulator to and hint/s to
	 * @return true if any hint/s were found
	 */
	public boolean search(ARegion r, Grid grid, IAccumulator accu) {

		// we need atleast 3 empty cells in the region for a Hidden Pair to
		// remove any maybes (2 cells in the Pair + 1 to remove from)
		final int degreePlus1 = this.degreePlus1;
		if ( r.emptyCellCount < degreePlus1 )
			return false;

		// a candidateValue has 2..degree possible positions in this region
		final int[] candidateValues = this.candidateValues;
		// this regions indexesOf values
		final Indexes[] rio = r.indexesOf;
		// n is number of candidateValues, v is value, card-inality
		int n=0, v, card;
		for ( v=1; v<10; ++v ) // 27*9 = 243
			if ( (card=rio[v].size)>1 && card<degreePlus1 )
				candidateValues[n++] = v;
		if ( n < degree )
			return false;

		// localise everything for speed
		final int[] SIZE = Values.SIZE;
		final int[] SHFT = Values.SHFT;
		final int degree = this.degree;
		final int[] thePA = this.thePA;
		Pots redPots;
		int i, hdnSetIdx, hdnSetMaybes;

		// presume failure, ie that no hint will be found
		boolean result = false;

		// foreach distinct combination of $degree candidateValues
		// NB: Direct Hidden Pairs is the first use of Permutations in each
		//     solve, so any bugs there will probably show up here.
		// NB: idxs contains candiVals indexes, not region.cells indexes.
		for ( int[] perm : new Permutations(n, thePA) ) {

			// look for $degree positions for these $degree values

			// build hdnSetIdx: a bitset of the indices of the cells in a
			// (possible) Hidden Set in the region that we're searching.
			hdnSetIdx = 0;
			for ( i=0; i<degree; ++i )
				hdnSetIdx |= rio[candidateValues[perm[i]]].bits;
			if ( SIZE[hdnSetIdx] != degree )
				continue; // there aren't degree positions for degree values

//// KRC BUG 2020-08-20 888#top1465.d5.mt apply canNotBeBits UnsolvableException
//// So I debug mergeSiameseHints coz there are TWO triples in row 1:
//// First Naked Triple in A1, B1, C1 on 379 => siamese claiming.
//// Second Hidden Triple in G1, H1, I1 on 124 => HiddenTriple.
////
//// The maybes don't line-up! Siamese presumes that the locking-set MUST be same
//// as the hidden-set. In this instance it isn't! (There may be others)
//if ( Debug.isClassNameInTheCallStack(5, "Locking")
//  // 1 2 4 8 16 32 64 128 256
//  // 0 1 2 3 4  5  6  7   8
//  && r == grid.regions[9] // row 1
//  && 64+128+256 == hdnSetIdx ) // 448 is G1, H1, I1
//	Debug.breakpoint();

			// build a bitset of the hidden set values, which is not done
			// previously because the hit-rate is too for it to pay-off.
			hdnSetMaybes = 0;
			for ( i=0; i<degree; ++i )
				hdnSetMaybes |= SHFT[candidateValues[perm[i]]];

			// build the removable (red) potentials, to see if there are any.
			if ( (redPots=reds(r, hdnSetMaybes, hdnSetIdx)) == null )
				continue; // about 80% skip here

			//
			// Hidden Set found!
			//

			// build the hint and add it to the accumulator
			AHint hint = createHint(r, hdnSetMaybes, hdnSetIdx, redPots, accu);
			result |= (hint != null);
			if ( accu.add(hint) )
				return true;
			// we're done if there aren't enough remaining to make another set
			if ( n-degree < degree )
				return result;
		} // next permutation
		return result;
	}

	// build the removable (red) potentials separately to check that there are
	// any BEFORE we declare "found" and create the hint.
	private Pots reds(ARegion r, int hdnSetMaybes, int hdnSetIdx) {
		Pots redPots=null;  Cell cell;  int redBits;
		// foreach index-of-a-cell-in-the-hidden-set
		for ( int i : Indexes.ARRAYS[hdnSetIdx] )
			// redBits := cell.maybes.bits - hdnSetValuesBits
			if ( (redBits=((cell=r.cells[i]).maybes.bits & ~hdnSetMaybes)) != 0 ) {
//if ( tech == Tech.HiddenTriple
//  && !Debug.isClassNameInTheCallStack(5, "RecursiveAnalyser")
//  && cell.id.equals("H1") )
//	Debug.breakpoint();
				// yes it's a hidden set, with elimination/s
				if ( redPots == null )
					redPots = new Pots();
				redPots.put(cell, new Values(redBits, false));
			}
		return redPots;
	}

	private AHint createHint(ARegion region, int hdnSetMaybes, int hdnSetIdx
			, Pots redPots, IAccumulator accu) {

		final Values hdnSetValues = new Values(hdnSetMaybes, false);

		// build the highlighted (green) Cell=>Values
		final Pots greenPots = new Pots();
		Cell cell;
		for ( int i : Indexes.ARRAYS[hdnSetIdx] )
			greenPots.put((cell=region.cells[i])
					, cell.maybes.intersect(hdnSetValues));

		// build an array of the cells in this hidden set (for the hint)
		final Cell[] cells = region.atNew(hdnSetIdx);

		// NORMAL MODE: A Hint has been found
		if ( !tech.isDirect )
			return new HiddenSetHint(this, cells, hdnSetValues
					, greenPots, redPots, region, hdnSetIdx);

		// DIRECT MODE: Does this Hidden Set cause a Hidden Single?

		// Direct mode is slower, so never used by RecursiveAnalyser
		assert !accu.getClass().getCanonicalName().equals(ChainersHintsAccumulator);

//this is the first "Direct Hidden Triple:"
//12#top1465.d5.mt	5..4..8......9..1...2..1..56..3..4...5..7......4.....83..6..7...6.....8...8..2..1
//5..4..8......9..1...2..1..56..3..4...5..741.6..4.....832.6.87...6.....8...8..2..1
//,13,13679,,26,367,,2379,379,478,3478,367,25,,3567,23,,2347,479,349,,78,38,,369,3467,,,1789,17,,158,59,,27,27,28,,39,28,,,,39,,1279,379,,12,26,69,359,357,,,,159,,145,,,459,49,149,,57,1579,345,37,2359,,234,479,479,,579,345,,3569,3456,
//36   	        126,000	29	 144	 11	Direct Hidden Triple          	Direct Hidden Triple: B4, E4, F4: 5, 8, 9 in row 4 (C4+1 B4-17, E4-1)
//if ( true
//&& tech == Tech.DirectHiddenTriple
//&& region.id.equals("row 4")
//&& hdnSetValsArray[0] == 5
//&& hdnSetValsArray[1] == 8
//&& hdnSetValsArray[2] == 9
//)
//	diuf.sudoku.utils.Debug.breakpoint();

		// figure-out if my eliminations result in a hidden single
		final int[] SIZE = Values.SIZE;
		final int[] FIRST_INDEX = Indexes.NUM_TRAILING_ZEROS;
		final Indexes[] riv = region.indexesOf;
		int bits;
		// foreach value EXCEPT the hiddenSetValues
		for ( int v : Values.ARRAYS[Values.ALL_BITS & ~hdnSetMaybes] ) {
			// means: region.idxsOf[v].minus(hdnSetIdxBits).size == 1
			if ( SIZE[bits=riv[v].bits & ~hdnSetIdx] == 1 ) // zero garbage
				// the aligned set causes a Hidden Single
				return new HiddenSetDirectHint(
					  this, cells, hdnSetValues, greenPots, redPots, region, v
					// means: region.idxsOf[v].minus(hdnSetIdxs).first()
					, region.cells[FIRST_INDEX[bits]] // Zero G
				);
		}
		return null; // No hidden single found
	}
}