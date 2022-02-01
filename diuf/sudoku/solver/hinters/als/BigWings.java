/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.BITS_PER_ELEMENT;
import static diuf.sudoku.Idx.BITS_TWO_ELEMENTS;
import static diuf.sudoku.Idx.IDX_SHFT;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.IntArrays.iaLease;

/**
 * BigWings implements WXYZ_Wing, VWXYZ_Wing, UVWXYZ_Wing, TUVWXYZ_Wing and
 * STUVWXYZ_Wing Sudoku solving techniques, all in one hit.
 * <p>
 * als.BigWings is a rewrite of wing.BigWing, to eradicate BitIdx's (which was
 * a {@code Set<Cell>} backed by a BitSet, and BitSet's are slow), and use the
 * cached alss array that's available by transplanting me into the als package
 * to extend AAlsHinter, so I can share the cached ALSs.
 * <p>
 * So my defining feature is: I use cached alss-array from AAlsHinter where-as
 * BigWing recurses to find it's own ALSs (faster). Using the cache is faster
 * over-all presuming that you're also using AlsXz, AlsWing, and/or AlsChain,
 * but unfortunately caching the ALSs is the slow-part, so BigWings is slower
 * than all BigWing combined, but still faster over-all presuming that you also
 * want DeathBlossom, ALS_XZ, ALS_Wing, or ALS_Chain.
 * <p>
 * Because I don't find my own ALSs there size doesn't matter. It's more code
 * to limit each hinter to one ALS-size, so I've removed the distinction by
 * introducing Tech.BigWings which I implement, instead of running five
 * instances of BigWings, each limited to a degree.
 * <p>
 * BigWings (this or individual) finds all AlignedPair, and all bar one-or-two
 * AlignedTriple and AlignedQuad, plus many other hints besides; and all-this
 * is MUCH faster than my aligned debacle. I'm pretty chuffed about it!
 * <p>
 * <b>NOTE</b> individual BigWing's are faster, but BigWings is faster overall
 * because most of my time is spent building the ALS cache shared with AlsXz,
 * AlsWing, etc. I've done my best to make it as fast as possible. sigh.
 * <pre>
 * KRC 2021-06-02 And FMS it's actually slower than the old BigWing hinter!
 * BEFORE: independant S/T/U/V/WXYZ-Wing hinters
 *     386,083,800  12362   31,231   886     435,760 WXYZ-Wing
 *     516,803,600  11803   43,785   881     586,610 VWXYZ-Wing
 *     636,373,100  11346   56,087   387   1,644,374 UVWXYZ-Wing
 *     521,475,600  11151   46,764    86   6,063,669 TUVWXYZ-Wing
 *     275,114,200  11102   24,780     4  68,778,550 STUVWXYZ-Wing
 *   2,335,850,300                  2244             total
 * AFTER: BigWings finds a few more (XY_Wing's), but takes twice as long. sigh.
 *   5,011,710,900  12357  405,576  2260   2,217,571 Big-Wings
 * Note this code is (probably) fine, but getAlss in AAlsHinter is slower than
 * BigWing's iterating sets to build sets, because it skips whole sets.
 *
 * KRC A BIT LATER: I replaced Collections in AAlsHinter with fixed-size arrays
 *   3,630,083,800  12354  293,838  2262   1,604,811 Big-Wings
 * still slower than BigWing but improving; also speeds-up AlsXz, AlsWing, etc
 *
 * KRC 2021-06-03 Reverted getAlss to use an AlsSet, because iteration STILL
 * (unexpectedly) finds some duplicates.
 *
 * KRC 2021-06-03 speeding-up BigWings to (hopefully) beat the old BigWing.
 * 13-26-56: 3,593,583,700  12326  291,545  2258   1,591,489 Big-Wings
 * Takes 1.54 * BigWing, but total down to 2:07 and I cache the ALSs, so OK.
 * So now CAN use either BigWings or individual S/T/U/V/WXYZ_Wing.
 * ALL getAlssTime = 2,924,623,800
 * MY  getAlssTime = 2,668,607,000 which is slower than original but then
 * other getAlss   =   256,016,800 and we need to cache ALSs somewhere
 *
 * KRC 2021-06-04 still speeding things up.
 *   3,706,021,200  12039  307,834  2210  1,676,932  Big-Wings
 *   3,712,255,500  12254  302,942  2210  1,679,753  Big-Wings 06-04.11-19-16
 *   3,699,850,100  12128  305,066  2195  1,685,580  Big-Wings 06-06.16-28-20
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
 * KRC 2021-06-17 13:55 Replaced isWing O(als.size) method with a call to vBuds
 * contains O(1), which is about half a second faster over top1465. Gee wow.
 *
 * KRC 2021-12-03 10:05:11 Just updating comparative timings.
 *   2,767,546,000  11471  241,264  2079  1,331,190  BigWings
 *
 * KRC 2021-12-17 Removed eliminate and createHints methods.
 *
 * KRC 2021-12-25 Just use als.vBuds; don't reinvent the bloody wheel. Sigh.
 *   2,535,250,600  11513  220,207  2076  1,221,218  BigWings (with COUNTS)
 * </pre>
 *
 * @author Keith Corlett 2021-06-02
 */
public class BigWings extends AAlsHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln("BigWings: COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
	// counterintuitively, it's faster incrementing COUNTS that go unreported,
	// which begs the question "Why?", to which I have no answer, coz I'm an
	// idiot. Mine is not to reason why, mine is just to do it faster.
	private static final long[] COUNTS = new long[4];

	// the victims, if any
	private static final Idx VICTIMS = new Idx();

	// removable (red) potentials, if any
	private static final Pots REDS = new Pots();

	/**
	 * BigWings is an AAlsHinter to get access to the cached alss array.
	 * allowNakedSets true: getAlss includes cells that're in NakedSets.
	 * BigWings does NOT getRccs (I use the alss-only).
	 */
	public BigWings() {
		super(Tech.BigWings, true);
	}

	// this "custom" findHints is called by AAlsHinter.findHints
	@Override
	protected boolean findAlsHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		// ANSI-C style: ALL variables are predeclared, so no more stackwork!
		Idx[] alsVBuds; // als.vBuds: common buddies of v's in this ALS
		Als als; // the Almost Locked Set: N cells sharing N+1 maybes
		int i // the als index
		  , alsMaybes // als.maybes: a bitset of the values in this ALS
		  , j // the current index in biva/MAYBES
		  , b // indice of the bivalue cell
		  , x // the primary (mandatory) link value
		  , z // the secondary (optional) link value
		  , bivMaybes // the candidates of the current bivalue cell
		  , tmp; // a temporary variable: set, read and forget.
		boolean xWing // do all x's in the als see the biv?
		  , zWing // do all z's in the als see the biv?
		  , both // xWing & zWing, ie is this wing double-linked?
		  , zStrong // are there any strong eliminations on z?
		  , xStrong // are there any strong eliminations on x?
		  , weak // are there any weak elims (from the ALS only)?
		  , any // are there ANY eliminations?
		  ;
		// presume that no hints will be found
		boolean result = false;
		// get bivalue indices ONCE (not foreach ALS), and get a lease over an
		// array there-of.
		// we "use" a lease so that it's close returns the array to the pool
		try ( final IALease iLease = grid.getBivalue().toArrayLease();
			  final IALease mLease = iaLease(iLease.array.length) ) {
			// a right-sized array of indices of bivalue cells
			final int[] bivArray = iLease.array;
			final int numBivs = bivArray.length;
			if(numBivs==0) return false; // happens in Shft-F5 only
			final int lastBivIndex = numBivs - 1; // the last j.
			// a right-sized array of maybes of bivalue cells
			final int[] maybesArray = mLease.array;
			// read bivMaybesArray from grid.cells at bivArray
			grid.maybes(bivArray, maybesArray);
			// foreach ALS (Almost Locked Set: N cells sharing N+1 maybes)
			for ( i=0; i<numAlss; ++i ) {
				// foreach bivalue cell
				for ( alsMaybes=(als=alss[i]).maybes,alsVBuds=als.vBuds,j=0; ; ) {
					// bivMaybes!=0 averts VFIRST[0]->33->AIOOBE in Generator.
					// I'll NEVER understand what's going on in the Generator.
					if ( (bivMaybes=maybesArray[j]) != 0
					  // and this bivalue cell shares both values with this ALS
					  && (alsMaybes & bivMaybes) == bivMaybes ) {
++COUNTS[0]; // 16,010,801
					    // read x and z values from biv.maybes
						x = VALUESES[bivMaybes][0]; // the lower
						z = VALUESES[bivMaybes][1]; // the higher
						// calculate xWing := als.vBuds[x].has(b)
						//       and zWing := als.vBuds[z].has(b)
						if ( (b=bivArray[j]) < BITS_PER_ELEMENT ) {
							xWing = (alsVBuds[x].a0 & IDX_SHFT[b]) != 0;
							zWing = (alsVBuds[z].a0 & IDX_SHFT[b]) != 0;
						} else if ( b < BITS_TWO_ELEMENTS ) {
							tmp = IDX_SHFT[b-BITS_PER_ELEMENT];
							xWing = (alsVBuds[x].a1 & tmp) != 0;
							zWing = (alsVBuds[z].a1 & tmp) != 0;
						} else {
							tmp = IDX_SHFT[b-BITS_TWO_ELEMENTS];
							xWing = (alsVBuds[x].a2 & tmp) != 0;
							zWing = (alsVBuds[z].a2 & tmp) != 0;
						}
						// if either-or-both x and z values are linked to ALS
						if ( xWing | zWing ) {
++COUNTS[1]; // 1,006,876
							// this ALS+biv form a BigWing; but any elims?
							// if zWing ONLY then z is primary
							if ( !xWing ) {
								tmp = x;
								x = z;
								z = tmp;
							}
							// seek strong elims on z
							any = zStrong = VICTIMS.setAndAny(grid.idxs[z], BUDDIES[b], alsVBuds[z])
										 && REDS.upsertAll(VICTIMS, grid, z);
							// if both x and z are linked
							if ( both = xWing & zWing ) {
++COUNTS[2]; // 515,419
								// get indices of each value 1..9 in the Grid.
								final Idx[] idxs = grid.idxs;
								// seek strong elims on x
								any |= xStrong = VICTIMS.setAndAny(idxs[x], BUDDIES[b], alsVBuds[x])
											  && REDS.upsertAll(VICTIMS, grid, x);
								// seek weak elims on als.maybes ^ biv.maybes
								weak = false;
								for ( int w : VALUESES[alsMaybes ^ bivMaybes] ) {
++COUNTS[3]; // 1,703,938
									any |= weak |= VICTIMS.setAndAny(idxs[w], alsVBuds[w])
												&& REDS.upsertAll(VICTIMS, grid, w);
								}
								// if this wing has no weak-links
								if ( !weak ) {
									// double linked = xStrong & zStrong
									both = xStrong & zStrong;
									// no z's means z is now x (primary link).
									if ( !zStrong ) {
										tmp = x;
										x = z;
										z = tmp;
									}
								}
							}
							if ( any ) {
								// FOUND a BigWing
								result = true;
								final Pots reds = REDS.copyAndClear();
								final Cell biv = grid.cells[b]; // bivalue cell
								final Cell[] alsCells = als.idx.cellsNew(grid.cells);
								final Pots oranges = new Pots(alsCells, x);
								oranges.put(biv, VSHFT[x]);
								// NB: x and z are reversed. Umm. You fix it.
								if ( accu.add(new BigWingsHint(this, reds, biv
										, z, x, both, als, alsCells, oranges)) )
									return result;
							}
						}
					}
					if ( ++j > lastBivIndex )
						break;
				}
			}
		}
		return result;
	}

}
