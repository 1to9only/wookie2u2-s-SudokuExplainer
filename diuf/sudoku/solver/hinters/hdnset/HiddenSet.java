/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.FIRST_INDEX;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.Values.ALL;


/**
 * Implementation of the Hidden Set Sudoku solving technique:
 * (HiddenPair=2, HiddenTriple=3, HiddenQuad=4).
 * <p>
 * A "hidden set" is a set of N (degree) cells that are the only possible
 * positions for N values in a region, and every value must appear in each
 * region, hence these N values are "locked into" these N cells (ie each of
 * these values must appear only in one of these cells); therefore all other
 * potential values can be eliminated from each of these N cells.
 * <p>
 * "Locked sets" are fun: the logic is that if any of these cells where any
 * value other than the hidden set values that leaves no place in the region
 * for one of the hidden set values, rendering the Sudoku invalid, and as a
 * valid Sudoku is not invalid, we can conclude that none of cells may be any
 * other value, so we can remove all other potential values from these cells.
 */
public final class HiddenSet extends AHinter {

	// the Permutations Array (used by the Permutations class)
	private final int[] thePA;

	// values with 2..$degree possible positions in the region
	private final int[] candidates = new int[9];

	/**
	 * Constructor.
	 * <p>
	 * The passed Tech primarily determines the degree: the number of cells in
	 * the HiddenSets that this hinter seeks:<ul>
	 * <li>HiddenPair = 2
	 * <li>HiddenTriple = 3
	 * <li>HiddenQuad = 4
	 * <li>HiddenPent = 5 is degenerate, meaning that all occurrences are
	 *  combinations of simpler techniques (like a hidden pair and a hidden
	 *  triple, in one region); but they CAN appear in get MORE hints.<br>
	 *  So I usually unwant Naked Pent and Hidden Pent, coz they're useless!
	 * </ul>
	 * <p>
	 * There are also "Direct" variant Techs:<ul>
	 * <li>DirectHiddenPair
	 * <li>DirectHiddenTriple
	 * </ul>
	 * which seek HiddenSets that cause a HiddenSingle; ie when we remove all
	 * other possible values from each of these cells that leaves one only
	 * possible location for a value in this region. These direct variants are
	 * fundamentally useless, in that all hints that they find would be found
	 * anyway by the "normal" HiddenSet hinter; the only difference is the
	 * direct variant produces a "bigger" hint with the "subsequently set this
	 * hidden single", where-as the "normal" version leaves the subsequent
	 * hidden single to the next HiddenSingle, because that's what it's for.
	 * <p>
	 * For speed unwant all the Direct hinters (they're useless).<br>
	 * If you want fancy hints then select them (they're still useless).
	 *
	 * @param tech the Tech to implement, see above.
	 */
	public HiddenSet(Tech tech) {
		super(tech);
		assert degree>1 && degree<=5;
		this.thePA = new int[degree]; // for Permutations
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// if accu.isSingle then it wants oneOnly hint
		final boolean oneOnly = accu.isSingle();
		// presume that no hint will be found
		boolean result = false;
		for ( ARegion r : grid.regions ) // 9boxs + 9rows + 9cols
			if ( (result|=search(r, grid, accu)) && oneOnly )
				break;
		return result;
	}

	/**
	 * This one is weird. Locking finds Naked Pairs and Naked Triples but only
	 * has the capacity to perform a subset of the eliminations, so it needs a
	 * way to ask me (NakedPair and NakedTriple) "Is this a Naked Set? And if
	 * so can you do the eliminations for me please?" Hence search is public,
	 * with variables created for each region, which is a bit slower, but shows
	 * the user ALL eliminations from "the most eliminative pattern" available.
	 *
	 * @param region the ARegion to search
	 * @param grid the Grid to search
	 * @param accu the implementation of IAccumulator to which I add hints
	 * @return were any hint/s found?
	 */
	public boolean search(ARegion region, Grid grid, IAccumulator accu) {
		// presume that no hint will be found
		boolean result = false;
		// we need atleast 3 empty cells in the region for a Hidden Pair
		// to remove any maybes (2 cells in Pair + 1 to remove from)
		if ( region.emptyCellCount > degree ) {
			// there are sufficient empty cells in this region
			// a candidateValue has 2..degree possible positions in this region
			final int[] candidates = this.candidates;
			// the number of cells in each aligned set
			final int degree = this.degree;
			// this regions indexesOf values 1..9
			final Indexes[] rio = region.indexesOf;
			// select the candidate values (with 2..degree places in region)
			int n = 0; // number of candidate values
			for ( int v=1,card; v<10; ++v ) // 27*9 = 243
				if ( (card=rio[v].size)>1 && card<degreePlus1 ) // card-inality
					candidates[n++] = v;
			// if there are at least degree candidate values
			if ( n >= degree ) {
				// Last stack-frame til we hint. Declare all here, not in loop.
				Pots reds; // removable (red) Cell=>Values
				int i // ubiquitous index
				  , indexes // bitset of indexes-in-region-cells of hidden set
				  , values; // bitset of values in the hidden set
				// foreach possible combination of degree candidateValues.
				// ------------------------------------------------------------
				// Note: [Direct]HiddenPairs is the first use of Permutations
				// in each solve, so any bugs there tend to show up here. Each
				// perm is an array of degree indexes in the candiValues array.
				// Each perm is actually thePA, which was been repopulated with
				// a distinct set of indexes; so all perm's equals all possible
				// combinations of candidateValues.
				// ------------------------------------------------------------
				for ( int[] perm : new Permutations(n, thePA) ) {
					// look for $degree positions for these $degree values.
					// indexes is a bitset of the combined indexes of cells
					// having our degree values (ie a possible Hidden Set).
					indexes = 0;
					for ( i=0; i<degree; ++i )
						indexes |= rio[candidates[perm[i]]].bits;
					// if there are degree positions for our degree values
					if ( VSIZE[indexes] == degree ) {
//// BUG 2020-08-20 888#top1465.d5.mt UnsolvableException: apply canNotBeBits
//// So I debug mergeSiameseHints. There are TWO triples in row 1:
//// First Naked Triple in A1, B1, C1 on 379 => siamese claiming.
//// Second Hidden Triple in G1, H1, I1 on 124 => HiddenTriple.
//// Maybes don't line-up! Siamese presumes that the locking-set MUST equal the
//// hidden-set. In this case it doesn't. There may be other occurrences.
//if ( Debug.isClassNameInTheCallStack(5, "Locking")
//  // 1 2 4 8 16 32 64 128 256
//  // 0 1 2 3 4  5  6  7   8
//  && r == grid.regions[9] // row 1
//  && 64+128+256 == hdnSetIdx ) // 448 is G1, H1, I1
//	Debug.breakpoint();
						// build a bitset of the hidden set values
						values = 0;
						for ( i=0; i<degree; ++i )
							values |= VSHFT[candidates[perm[i]]];
						// if any eliminations then create and add hint
						if ( (reds=eliminate(region, values, indexes)) != null ) {
							// FOUND HiddenSet! (about 80% skip)
							final AHint hint = createHint(region, values
									, indexes, reds, accu);
							if ( hint != null ) {
								result = true;
								if ( accu.add(hint) )
									return result;
							}
						}
						// exit if not enough cells for another set,
						// even if we didn't actually hint here
						if ( n-degree < degree )
							return result;
					}
				} // next permutation
			}
		}
		return result;
	}

	/**
	 * Build the removable (red) Cell=&gt;Values to see if any exist before
	 * creating the hint. All potential values other than the hidden set
	 * values can be removed from each cell in the hidden set.
	 *
	 * @param region the region that we're searching
	 * @param values a bitset of the hidden set values
	 * @param indexes a bitset of indexes in region.cells of cells in the
	 *  hidden set
	 * @return removable (red) Cell=&gt;Values if any, else null (about 80%)
	 */
	private Pots eliminate(ARegion region, int values, int indexes) {
		int pinks; // a bitset of the removable maybes of each cell in hdn set
		Pots reds = null; // the result removable (red) Cell=>Values
		// foreach index-in-this-region of a cell that is in the hidden set
		for ( int i : INDEXES[indexes] )
			// removable-values := cell.maybes.bits - hidden-set-values
			// if there are any removable-values
			if ( (pinks=(region.cells[i].maybes.bits & ~values)) != 0 ) {
//if ( tech == Tech.HiddenTriple
//&& !Debug.isClassNameInTheCallStack(5, "RecursiveAnalyser")
//&& "H1".equals(r.cells[i].id) )
//	Debug.breakpoint();
				if ( reds == null )
					reds = new Pots();
				reds.put(region.cells[i], new Values(pinks, false));
			}
		return reds;
	}

	/**
	 * Create and return a new AHint, else null meaning none.
	 * <pre>
	 * if the Tech passed to my constructor isDirect then
	 *     return a new HiddenSetDirectHint, else null meaning none;
	 * else
	 *     return a new HiddenSetHint, always.
	 * </pre>
	 * Note that if Direct hidden sets are wanted then they ALWAYS run before
	 * the "normal" hidden set hinters, so there will be no hidden sets which
	 * cause hidden singles remaining when the "normal" hinters run.
	 *
	 * @param r the region that we're searching
	 * @param values a bitset of the values in the hidden set
	 * @param indexes a bitset of indexes in r of the cells in the hidden set
	 * @param reds the removable (red) Cell=>Values
	 * @param accu an implementation of IAccumulator
	 * @return the new hint, else null meaning none
	 */
	private AHint createHint(ARegion r, int values, int indexes, Pots reds
			, IAccumulator accu) {
		// build a Values of the values in the hidden set
		final Values vs = new Values(values, false);
		// build the highlighted (green) Cell=>Values
		final Pots greens = new Pots();
		for ( int i : INDEXES[indexes] ) {
			Cell cell = r.cells[i];
			greens.put(cell, cell.maybes.intersect(vs));
		}
		// build an array of the cells in this hidden set (for the hint)
		final Cell[] cells = r.atNew(indexes);
		//
		// NORMAL MODE: A Hint has been found
		//
		if ( !tech.isDirect )
			return new HiddenSetHint(this, cells, vs, greens, reds, r, indexes);
		//
		// DIRECT MODE: Does this Hidden Set cause a Hidden Single?
		//
		// Direct mode is slower, so never used by RecursiveAnalyser
		assert !"diuf.sudoku.solver.hinters.chain.ChainersHintsAccumulator"
				.equals(accu.getClass().getCanonicalName());

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
		// do the eliminations cause a hidden single?
		int bits;
		// foreach value EXCEPT the hidden set values
		// nb: Logically it should be foreach value that has been removed, but
		// there's no fast way to calculate that, AFAIK, so this is faster.
		for ( int v : VALUESES[ALL & ~values] )
			if ( VSIZE[bits=r.indexesOf[v].bits & ~indexes] == 1 )
				// this aligned set causes a Hidden Single
				return new HiddenSetDirectHint(this, cells, vs, greens, reds, r
						, v, r.cells[FIRST_INDEX[bits]]);
		return null; // No hidden single found
	}

}