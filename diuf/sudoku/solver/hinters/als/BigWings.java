/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.IDX_SHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.Values.VFIRST;

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
 * than all BigWing combined, but still faster over-all (presuming that ...).
 * <p>
 * And because I don't find my own ALSs there size doesn't matter. It's more
 * code to limit each hinter to one ALS-size, so I've removed the distinction
 * by introducing Tech.BigWings which I now implement, instead of running 5
 * instances of me, each limited to a degree.
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
 * KRC 2021-06-17 13:55 Replaced isWing O(als.size) method with a call to vBuds
 * contains O(1), which is about half a second faster over top1465. Gee wow.
 * </pre>
 *
 * @author Keith Corlett 2021-06-02
 */
public class BigWings extends AAlsHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("\n%s: getAlssTime=%,d\n", this.getClass().getSimpleName(), getAlssTime);
//		//ALS_FINDER.took declared in AlsFinder, set in AlsFinderRecursive only
//		diuf.sudoku.utils.Log.teef("\n%s: ALS_FINDER.took=%,d\n", this.getClass().getSimpleName(), ALS_FINDER.took);
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNT="+java.util.Arrays.toString(COUNT));
//	}
//	private final long[] COUNT = new long[6];
	// BigWings: COUNT=[2790999, 39496849, 17510250, 1088613, 546616, 546449]

	/**
	 * eliminate looks for eliminations in the BigWing formed by the ALS-cells
	 * and the xz-cell (called biv).
	 * <p>
	 * If isStrong then seek strong elims: ALS-cells and biv all see each elim;
	 * else seek week elims: ALS-cells only see each elim, precluding the biv.
	 * <p>
	 * <pre>
	 * alsVs param is NEVER null, coz ALS must contain v for us to elim.
	 * If you're dealing with an NPE then:
	 * 1. elim'ing a value that isn't in the ALS is wrong.
	 * 2. pretest alsVs to avoid invoking elim pointlessly.
	 * </pre>
	 * <p>
	 * NB: I made eliminate static for speed, because Javas this-injection is
	 * a bit slow, which makes grid, candidates, VICTIMS, and REDS static,
	 * which is OK because only one instance of BigWings exists at a time. But
	 * if you're multi-threading then de-staticise eliminate and it's fields;
	 * either that or synchronise it, which will be slower.
	 * This is fast for how things are now: single-threaded (ST), which I wish
	 * it wasn't, but it is: It's fast ST, unless we multithread (MT) it all,
	 * and early-on I decided solve was an ST process, so hinters presume ST,
	 * which was silly, and now I'm stuck with it, coz MTing everything takes
	 * too much work, so I'm mentally welded to ST, but you need not be.
	 *
	 * @param isStrong do we seek strong (or week) eliminations: strong means
	 *  all cells in the ALS and the biv (bivalue cell, aka xz-cell) see each
	 *  elimination (ie all cells in the ALS and the biv which maybe v see
	 *  (same box, row, or col) the cell from which we are eliminating);
	 *  else false means only the ALS-cells see each elimination, and the biv
	 *  is precluded (ie we cannot eliminate from the xz-cell)
	 * @param v the value to eliminate, if possible
	 * @param alsVs is NEVER null, coz ALS must contain v for us to elim.
	 * @param biv the bivalue cell, aka the xz-cell after it's two possible
	 *  values, called x and z. sigh.
	 * @return are there any eliminations, which have been added to REDS: the
	 *  removable (red) potentials, as displayed in the hint
	 */
	private static boolean eliminate(final boolean isStrong, final int v
			, final Idx alsVs, final Cell biv) {
		VICTIMS.set(idxs[v]);
		alsVs.forEach((i)->VICTIMS.and(BUDDIES[i]));
		if ( isStrong ) { // buds of ALL wing cells, including the biv
			VICTIMS.and(biv.buds);
		} else { // weak: just remove the bivalue cell
			VICTIMS.remove(biv.i);
		}
		if ( VICTIMS.none() ) {
			return false;
		}
		REDS.upsertAll(VICTIMS, grid, v);
		return true;
	}

	// maybes of all bivalue cells in the grid, coincident with biva
	private static final int[] MAYBES = new int[64]; // 64=81-17

	// these are static only because elim is static, to reduce stackwork.
	// If you think you can do it "cleaner" then performance test it.
	private static Grid grid;
	private static Idx[] idxs; // grid.idxs
	private static final Idx VICTIMS = new Idx();
	private static final Pots REDS = new Pots();

	// NOTE BigWings is an AAlsHinter to get access to the cached alss array.
	public BigWings() {
		super(Tech.BigWings, true);
	}

	// NOTE that this "custom" findHints is called by AAlsHinter.findHints
	@Override
	protected boolean findHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		// ANSI-C style: ALL variables are predeclared, so no more stackwork!
		Als als; // the Almost Locked Set: N cells sharing N+1 maybes
		Cell biv; // the bivalue cell to complete the Wing pattern
		int[] ws; // values to weak eliminate: als.maybes XOR biv.maybes
		Idx[] avb; // als.vBuds: common buddies of v's in this ALS
		Idx vb; // als.vBuds[v] where v is the x or z value
		int i // the als index
		  , m // als.maybes: a bitset of the values in this ALS
		  , j // the current index in biva/MAYBES
		  , b // the bivalue cell indice in Grid
		  , x // the primary (mandatory) link value
		  , z // the secondary (optional) link value
		  , w // weak value index
			  // also hijacked as a biv.maybes to limit the stack-size
		  , W; // number of weak values (ws.length)
			   // also hijacked as a tmp to limit the stack-size
		boolean xWing // do all x's in the als see the biv?
		  , zWing // do all z's in the als see the biv?
		  , both // xWing & zWing, ie is this wing double-linked?
		  , zStrong // are there any strong eliminations on z?
		  , xStrong // are there any strong eliminations on x?
		  , weak; // are there any weak elims (from the ALS only)?
		final Cell[] cells = grid.cells;
		// presume that no hints will be found
		boolean result = false;
		try {
			// these fields are static to make elim static, to not pass this,
			// for speed. They are ALL cleared in the finally block.
			BigWings.grid = grid;
			BigWings.idxs = grid.idxs;
			// get Idx of bivalue cells (ie cells with size == 2)
			final Idx bivi = grid.getBivalue(); 
			// get bivalue indices ONCE (not foreach ALS)
			// coincident with MAYBES
			final int[] biva = bivi.toArrayA();
			// get bivalue maybes ONCE (not foreach ALS)
			int numBivs = bivi.maybes(cells, MAYBES);
			// Oh FFS! Something is SERIOUSLY screwy with lambdas + Generator!
			// NONE of this should EVER happen, but if it does (again).
			if(numBivs==0) return false;
			while ( MAYBES[numBivs-1] == 0 ) {
				if(--numBivs==0) return false;
			}
			assert MAYBES[numBivs-1] != 0;
			// foreach ALS (Almost Locked Set: N cells sharing N+1 maybes)
			for ( i=0; i<numAlss; ++i ) {
//				++COUNT[0]; // 2,790,999
				// foreach bivalue cell
				for ( m=(als=alss[i]).maybes,avb=als.vBuds,j=0; j<numBivs; ++j ) {
//					++COUNT[1]; // 39,496,849
					// if this biv shares both it's values with the ALS
//					if ( (m & (w=MAYBES[j])) == w ) {
// test w!=0 for Generator, which I do NOT understand!
// Java TROUBLE with getting the index instead the array value when you inline,
// so more reliable to just do it the old fashioned. I built a cubby, shed,
// house, hotel, bridge, and a whole city... then I built a cubby. I quite like
// cubbies. They remind me of the last time I felt safe.
					w = MAYBES[j];
					if ( w!=0 && (m & w)==w ) {
//						++COUNT[2]; // 17,510,250
					    // read x value from biv.maybes (the lower one)
						// calculate xWing = als.vBuds[x].has(b=biva[j])
						// NB: There's TROUBLE with inlining VFIRST for speed,
						// so it's more reliable do it the old fashioned way.
						x = VFIRST[w]; // Generator AIOOBE, before split line.
						vb = avb[x];
						if ((b=biva[j])<27) W=vb.a0; else if(b<54) W=vb.a1; else W=vb.a2;
						xWing = (W & IDX_SHFT[b%27]) != 0;
					    // read z value from biv.maybes (the higher one)
						// calculate zWing = als.vBuds[z].has(b)
						// inlined for speed
						z = w & ~VSHFT[x];
						z = VFIRST[z];
						vb = avb[z];
						if (b<27) W=vb.a0; else if(b<54) W=vb.a1; else W=vb.a2;
						zWing = (W & IDX_SHFT[b%27]) != 0;
						// if either-or-both x and z values are linked to ALS
						if ( xWing | zWing ) {
//							++COUNT[3]; // 1,088,613
							if ( !xWing ) { // ie zWing only
								// z is the primary link
								W = x;
								x = z;
								z = W;
							}
							// This ALS+biv form a BigWing; but any eliminations?
							// seek strong elims on z
							zStrong = eliminate(T, z, als.vs[z], biv=cells[b]);
							// if both x and z are linked
							if ( both = xWing & zWing ) {
//								++COUNT[4]; // 546,616
								// seek strong elims on x
								xStrong = eliminate(T, x, als.vs[x], biv);
								// seek weak elims on each alsMaybes XOR bivMaybes
								weak = false;
								for ( ws=VALUESES[m^w],w=0,W=ws.length; w<W; ++w ) {
									weak |= eliminate(F, ws[w], als.vs[ws[w]], biv);
								}
								// if this wing has no weak-links
								if ( !weak ) {
//									++COUNT[5]; // 546,449
									// it's double linked only if xStrong & zStrong
									both = xStrong & zStrong;
									// no z's means z is now x (the primary link).
									// which looks wrong, but is correct!
									if ( !zStrong ) {
										// x is the primary link
										W = x;
										x = z;
										z = W;
									}
								}
							}
							if ( !REDS.isEmpty() ) {
								// FOUND a BigWing on x and possibly z
								final AHint hint = createHint(als, x, z, both, biv);
								result = true;
								if ( accu.add(hint) ) {
									return result;
								}
							}
						}
					}
				}
			}
		} finally {
			// clean-up the statics
			BigWings.grid = null; // grid.cells: do NOT clear!
			BigWings.idxs = null;
			// just in case we blew a gasket
			BigWings.REDS.clear();
		}
		return result;
	}

	private AHint createHint(final Als als, final int x, final int z
			, final boolean both, final Cell biv) {
		final Cell[] alsCells = als.idx.cells(grid.cells);
		final Pots oranges = new Pots(alsCells, x);
		oranges.put(biv, VSHFT[x]);
		// NOTE: x and z are reversed, so IDKFA! It's nuts: You sort it out!
		return new BigWingsHint(this, REDS.copyAndClear(), biv
				, z, x, both, als, alsCells, oranges);
	}

}
