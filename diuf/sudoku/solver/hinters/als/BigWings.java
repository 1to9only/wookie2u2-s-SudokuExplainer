/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * BigWings implements the Tech.BigWings Sudoku Solving technique, which is an
 * aggregate of: WXYZ, VWXYZ, UVWXYZ, TUVWXYZ and STUVWXYZ_Wing.
 * <p>
 * als.BigWings is a rewrite of wing.BigWing, to eradicate BitIdx's, and use
 * the cached alss array that's available by transplanting me into the als
 * package to extend AAlsHinter, so I can share the cached alss.<br>
 * So my defining feature is: BigWing recurses to find it's own ALS's (faster)<br>
 * where-as I use cached alss-array from AAlsHinter (which is faster over-all
 * presuming that you're also using AlsXz, AlsWing, and/or AlsChain).
 * <p>
 * And because I don't find my own ALS's there size doesn't matter. It's more
 * code to limit each hinter to one ALS-size, so I've removed the distinction
 * by introducing Tech.BigWings which I now implement, instead of running 5
 * instances of me, each limited to a degree.
 * <p>
 * BigWings (this or individual) finds all AlignedPair, and all bar one-or-two
 * AlignedTriple and AlignedQuad, plus many other hints besides; and all-this
 * is MUCH faster than my aligned debacle. I'm a bit chuffed!
 * <p>
 * <b>NOTE WELL</b> individual BigWing are faster, but BigWings faster overall
 * coz most of it's time is building ALS cache shared with AlsXz and AlsWing.
 * I've done my best to expediate AAlsHinter.getAlss but BigWing still quicker.
 * <pre>
 * KRC 2021-06-02 And FMS it's actually slower than the old BigWing hinter!
 * BEFORE: independant S/T/U/V/WXYZ-Wing hinters
 *     386,083,800  12362   31,231   886     435,760 WXYZ-Wing
 *     516,803,600  11803   43,785   881     586,610 VWXYZ-Wing
 *     636,373,100  11346   56,087   387   1,644,374 UVWXYZ-Wing
 *     521,475,600  11151   46,764    86   6,063,669 TUVWXYZ-Wing
 *     275,114,200  11102   24,780     4  68,778,550 STUVWXYZ-Wing
 *   2,335,850,300                  2244             total
 * AFTER: this cluster____ finds a few more, but takes twice as long. sigh.
 *   5,011,710,900  12357  405,576  2260   2,217,571 Big-Wings
 * Note this code is (probably) fine, but getAlss in AAlsHinter is slower than
 * BigWing's iterating sets to build sets, because it skips whole sets.
 *
 * KRC A BIT LATER: I replaced Collections in AAlsHinter with fixed-size arrays
 *   3,630,083,800  12354  293,838  2262   1,604,811 Big-Wings
 * so we're still slower than the original BigWing but we're improving. This
 * change also speed-up AlsXz, AlsWing, AlsChain, and DeathBlossom.
 *
 * KRC 2021-06-03 Reverted getAlss to use an AlsSet, because iteration STILL
 * (unexpectedly) finds some duplicates.
 *
 * KRC 2021-06-03 speeding-up BigWings to (hopefully) beat the old BigWing.
 * 13-26-56: 3,593,583,700  12326  291,545  2258   1,591,489 Big-Wings
 * So it still takes 1.54 times as long as orig, but total time is down to 2:07
 * and I cache the ALSs, so it's all good. So we can use either Big-Wings or
 * WXYZ, VWXYZ, UVWXYZ, TUVWXYZ and STUVWXYZ-Wing individually.
 * ALL getAlssTime = 2,924,623,800
 * MY  getAlssTime = 2,668,607,000 which is slower than original but then
 * other getAlss   =   256,016,800 and we need to cache ALSs somewhere
 *
 * KRC 2021-06-04 still speeding things up.
 *   3,706,021,200  12039  307,834  2210  1,676,932 Big-Wings
 *   3,712,255,500  12254  302,942  2210  1,679,753 Big-Wings 06-04.11-19-16
 *   3,699,850,100  12128  305,066  2195  1,685,580 Big-Wings 06-06.16-28-20
 *
 * KRC 2021-06-17 RecursiveAlsFinder (used by AAlsHinter) now implements the
 * recursive ALS finding technique used by the BigWing class, which came from
 * Juillerat, but I think it originally came from SudokuMonster. It's about as
 * fast as the iterative AlsFinder, so I'm using it, but it's pretty close, and
 * finding ALS's is the fast part; finding the RCC's in numAlss*numAlss is the
 * expensive bit, and I've done my very best to expedite that process in the
 * RccFinder class, which is now "fully-optimised" until I think of something
 * else to ____around with. The hunter, endlessly seeking.
 * 
 * KRC 2021-06-17 13:55 Replaced isWing o(als.size) method with a call to vBuds
 * contains O(1), which is about half a second faster over top1465. Gee wow.
 * </pre>
 *
 * @author Keith Corlett 2021-06-02
 */
public class BigWings extends AAlsHinter
//implements diuf.sudoku.solver.IReporter
{
//	// method out-of-place to comment/uncomment with above implements line
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("\n%s: count=%s\n", this.getClass().getSimpleName(), java.util.Arrays.toString(count));
//		diuf.sudoku.utils.Log.teef("\n%s: getAlssTime=%,d\n", this.getClass().getSimpleName(), getAlssTime);
//		//ALS_FINDER.took declared in AlsFinder, set in AlsFinderRecursive only
//		diuf.sudoku.utils.Log.teef("\n%s: ALS_FINDER.took=%,d\n", this.getClass().getSimpleName(), ALS_FINDER.took);
//	}
//	private final long[] count = new long[2];

	// note that making this method static forces grid, candidates, and VICTIMS
	// to be static, which they can be because only one instance of BigWings
	// exists at any one time. If you're multi-threading then make the elim
	// method "non-static" and de-staticise it's fields; either that or you'll
	// need a synchronised block, which would be wasteful.
	// @param alsVs is NEVER null, coz ALS must contain v for us to elim.
	//  If you're dealing with an NPE then:
	//  1. elim'ing a value that isn't in the ALS is wrong.
	//  2. pretest alsVs to avoid invoking elim pointlessly.
	private static boolean elim(final boolean isStrong, final int v
			, final Idx alsVs, final Cell biv, final Pots reds) {
		VICTIMS.set(candidates[v]);
		alsVs.forEach((i)->VICTIMS.and(BUDDIES[i]));
		if ( isStrong ) // buds of ALL wing cells, including the biv
			VICTIMS.and(biv.buds);
		else // weak: just remove the bivalue cell
			VICTIMS.remove(biv.i);
		if ( VICTIMS.none() )
			return false;
		VICTIMS.forEach(grid.cells, (cell)->reds.upsert(cell, v));
		return true;
	}

	private static Grid grid; // static coz elim is static
	private static Idx[] candidates; // static coz elim is static
	private final Pots reds = new Pots();
	private static final Idx VICTIMS = new Idx();  // static coz elim is static

	// NOTE BigWings is an AAlsHinter to get access to the cached alss array.
	public BigWings() {
		super(Tech.BigWings, true);
	}

	// NOTE that this "custom" findHints is called by AAlsHinter.findHints
	@Override
	protected boolean findHints(Grid grid, Idx[] candidates, Als[] alss
			, int numAlss, Rcc[] rccs, int numRccs, IAccumulator accu) {
		// ANSI-C style: ALL variables are predeclared, so no stackwork!
		// I call only static methods, having no vars, so no stackwork!
		Als als; // the Almost Locked Set: N cells sharing N+1 maybes
		Cell biv; // the bivalue cell to complete the Wing pattern
		int[] ws; // values to weak elim: als.maybes ^ biv.maybes.bits
		int i // the als index
		  , j // the bivalue cell index
		  , x // the primary (mandatory) link value
		  , z // the secondary (optional) link value
		  , w; // weak value index, hijacked as a tmp to limit stacksize.
		boolean xWing // do all x's in the als see the biv?
		  , zWing // do all z's in the als see the biv?
		  , both // xWing & zWing, ie is this wing double-linked?
		  , zStrong // are there any strong eliminations on z?
		  , xStrong // are there any strong eliminations on x?
		  , weak; // are there any weak elims (from the ALS only)?
		// presume that no hints will be found
		boolean result = false;
		try {
			// these fields are static to make elim static, to not pass this,
			// for speed. They are ALL cleared in the finally block.
			BigWings.grid = grid;
			BigWings.candidates = candidates;
			// use the 81-Cell array from the CAS
			final Cell[] bivs = Cells.array(81); // cached
			// get Idx of cells with maybes.size == 2
			final Idx bivi = grid.getBivalue(); // cached
			// get an array of bivalue cells ONCE, instead of foreach ALS
			final int numBivs = bivi.cellsN(grid, bivs);
			// foreach ALS (Almost Locked Set: N cells sharing N+1 maybes)
			for ( i=0; i<numAlss; ++i )
				// foreach bivalue cell
				for ( als=alss[i],j=0; j<numBivs; ++j )
					// if this bivalue cell shares both it's values with this ALS
					if ( VSIZE[(w=(biv=bivs[j]).maybes.bits) & als.maybes] == 2
					  // and get x and z values from biv.maybes.bits (above w)
					  // and all x's in the ALS see the bivalue cell
					  //  or all z's in the ALS see the bivalue cell
					  && ( (xWing=als.vBuds[x=FIRST_VALUE[w]].contains(biv.i))
						 | (zWing=als.vBuds[z=FIRST_VALUE[w & ~VSHFT[x]]].contains(biv.i)) )
					) {
						if ( !xWing ) { // ie zWing only
							// x is the primary link
							w = x;
							x = z;
							z = w;
						}
						// This ALS+biv is a BigWing; but any eliminations?
						// seek strong elims on z
						zStrong = elim(true, z, als.vs[z], biv, reds);
						// if both x and z are linked
						if ( both = xWing & zWing ) {
							// seek strong elims on x
							xStrong = elim(true, x, als.vs[x], biv, reds);
							// seek weak elims on each alsMaybes XOR bivMaybes
							weak = false;
							for ( ws=VALUESES[als.maybes ^ biv.maybes.bits],w=0; w<ws.length; ++w )
								weak |= elim(false, ws[w], als.vs[ws[w]], biv, reds);
							// if this wing has no weak-links
							if ( !weak ) {
								// it's double linked only if xStrong & zStrong
								both = xStrong & zStrong;
								// no z's means z is now x (the primary link).
								// which looks wrong, but is correct!
								if ( !zStrong ) {
									// x is the primary link
									w = x;
									x = z;
									z = w;
								}
							}
						}
						if ( !reds.isEmpty() ) {
							// FOUND a BigWing on x and possibly z
							final AHint hint = createHint(als, x, z, both, biv, reds);
							result = true;
							if ( accu.add(hint) )
								return result;
						}
					}
		} finally {
			BigWings.grid = null;
			BigWings.candidates = null;
			this.reds.clear(); // just in case we blew a gasket
		}
		return result;
	}

	private AHint createHint(final Als als, final int x, final int z
			, final boolean both, final Cell biv, final Pots reds) {
		final Pots oranges = new Pots(als.cells, x);
		oranges.put(biv, new Values(x));
		// NOTE: x and z are reversed!
		return new BigWingsHint(this, reds.copyAndClear()
				, biv, z, x, both, als, oranges);
	}

}