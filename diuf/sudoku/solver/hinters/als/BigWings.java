/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;

/**
 * BigWings implements the {@link Tech#BigWings} Sudoku solving technique, but
 * my hints are called WXYZ_Wing, VWXYZ_Wing, UVWXYZ_Wing, TUVWXYZ_Wing and
 * STUVWXYZ_Wing.
 * <p>
 * BigWings owes its existence to the authors of Sukaku, but this code is pure
 * KRC, loosely based on that in Sukaku (poor code quality), so all mistakes
 * are mine, and kudos (if any) still belongs to Sukakus author/s, despite the
 * utter poopyness of there published code.
 * <p>
 * als.BigWings is a rewrite of wing.BigWing, to eradicate BitIdxs (which was
 * a {@code Set<Cell>} backed by a BitSet, and BitSets are slow), and use the
 * cached ALSs array which is available by extending AAlsHinter; hence the move
 * from the 'wing' package into the 'als' package.
 * <p>
 * So my defining feature is: I use cached alss-array from AAlsHinter where-as
 * BigWing recursed to find it is own ALSs (faster). Using the cache is faster
 * over-all presuming that you are also using DeathBlossom, AlsXz, AlsWing,
 * and/or AlsChain, but caching the ALSs is slow, so BigWings is slower than
 * all BigWing combined, but still faster over-all when you use DeathBlossom,
 * ALS_XZ, ALS_Wing, and/or ALS_Chain in addition to BigWings, which I do.
 * <p>
 * Because I do not find my own ALSs there size does not matter. Its more code
 * to limit each hinter to one ALS-size, so I have removed the distinction by
 * introducing Tech.BigWings which I implement, instead of running 5 instances
 * of BigWings, each limited to a degree.
 * <p>
 * BigWings finds all AlignedPair, and all bar one-or-two AlignedTriple and
 * AlignedQuad, plus I-know-not-what others besides. BigWings is MUCH faster
 * than my AlignedExclusion debacle. I am a bit chuffed about BigWings!
 * <p>
 * <pre>
 * KRC 2021-06-02 BigWings is slower than the old (now dead) BigWing.
 * BEFORE: independant S/T/U/V/WXYZ-Wing hinters
 *   386,083,800 12362 31,231  886    435,760 WXYZ-Wing
 *   516,803,600 11803 43,785  881    586,610 VWXYZ-Wing
 *   636,373,100 11346 56,087  387  1,644,374 UVWXYZ-Wing
 *   521,475,600 11151 46,764   86  6,063,669 TUVWXYZ-Wing
 *   275,114,200 11102 24,780    4 68,778,550 STUVWXYZ-Wing
 * 2,335,850,300              2244
 * AFTER: BigWings finds a few more (XY_Wings), but takes twice as long. sigh.
 * 5,011,710,900 12357 405,576 2260 2,217,571 Big-Wings
 * Note this code is (probably) fine, but getAlss in AAlsHinter is slower than
 * BigWings iterating sets to build sets, because it skips whole sets.
 *
 * KRC A BIT LATER: I replaced Collections in AAlsHinter with fixed-size arrays
 * 3,630,083,800 12354 293,838 2262 1,604,811 Big-Wings
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
 * 3,706,021,200 12039 307,834 2210 1,676,932 Big-Wings
 * 3,712,255,500 12254 302,942 2210 1,679,753 Big-Wings 06-04.11-19-16
 * 3,699,850,100 12128 305,066 2195 1,685,580 Big-Wings 06-06.16-28-20
 *
 * KRC 2021-06-17 RecursiveAlsFinder (used by AAlsHinter) now implements the
 * recursive ALS finding technique used by the BigWing class, which came from
 * Juillerat, but I think it originally came from SudokuMonster. It is about as
 * fast as the iterative AlsFinder, so I am using it, but its pretty close, and
 * finding ALSs is the fast part; finding the RCCs in numAlss*numAlss is the
 * expensive bit, and I have done my very best to expedite that process in the
 * RccFinder class, which is now "fully-optimised" until I think of something
 * else to ____around with. The hunter, endlessly seeking.
 *
 * KRC 2021-06-17 13:55 Replaced isWing O(als.size) method with a call to vBuds
 * contains O(1), which is about half a second faster over top1465. Gee wow.
 *
 * KRC 2021-12-03 10:05:11 Just updating comparative timings.
 * 2,767,546,000 11471 241,264 2079 1,331,190 BigWings
 *
 * KRC 2021-12-17 Removed eliminate and createHints methods.
 *
 * KRC 2021-12-25 Just use als.vBuds; do not reinvent the bloody wheel. Sigh.
 * 2,535,250,600 11513 220,207 2076 1,221,218 BigWings (with COUNTS)
 *
 * KRC 2023-06-28 Cleaner als.vBuds[x].has(b) is slower. Dont care!
 * 4,699,406,000 12785 367,571 2153 2,182,724 BigWings
 * 5,543,142,400 12785 433,566 2153 2,574,613 BigWings 2023-06-29.06-40-05
 *
 * KRC 2023-07-01.17-22-30 Exploded als.vBudsX for speed. Turns out I do care.
 * 3,781,051,400 12785 295,741 2153 1,756,178 BigWings
 *
 * KRC 2023-07-10 exploded idx ops, so no stackwork til we hint, for speed!
 * but FMS its slower, but I keep it anyway, to let it "bed in". Sigh. I guess
 * the extra variables take longer to create than they save in millions of ops.
 * PRE: 4,238,029,000 12788 331,406 2154 1,967,515 BigWings
 * PST: 4,271,741,300 12788 334,042 2154 1,983,166 BigWings
 *
 * KRC 2023-08-03 09:30 each elim was spuriously anding idxs[value].
 * PST: 4,088,754,000 12572 325,227 2168 1,885,956 BigWings
 * </pre>
 *
 * @author Keith Corlett 2021-06-02
 */
public class BigWings extends AAlsHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[12];

	/**
	 * BigWings is an AAlsHinter to get access to the cached alss array.
	 * allowNakedSets true: getAlss includes cells that are in NakedSets.
	 * BigWings does NOT getRccs (I use the alss-only).
	 */
	public BigWings() {
		super(Tech.BigWings);
	}

	@Override
	public void setFields(Grid grid) {
		this.grid = grid;
		maybes = grid.maybes;
		cells = grid.cells;
	}

	@Override
	public void clearFields() {
		grid = null;
		maybes = null;
		cells = null;
	}

	/**
	 * This "custom" findHints is called by {@link AAlsHinter#findHints}.
	 * <p>
	 * The Abstract ALS Hinter fetches and caches the alss and rccs centrally,
	 * then calls the findAlsHints in its subtype to find many types of hints
	 * in those alss: BigWings, DeathBlossom, AlsXz, AlsWing, and AlsChain.
	 * Note that the alss and rccs are cached in static permanent arrays, so
	 * neither Als nor Rcc can hold a Cell (or any Grid artifact), just Idxs.
	 * <p>
	 * I find BigWings hints. A BigWing is S/T/U/V/WXYZ-Wing, by which I mean:
	 * <pre>
	 * WXYZ-Wing is 3 cells (4 values) in an ALS and a bivalue "stem" cell
	 * VWXYZ-Wing is 4 cells (5 values) in an ALS ...
	 * UVWXYZ-Wing is 5 cells (6 values) in an ALS ...
	 * TUVWXYZ-Wing is 6 cells (7 values) in an ALS ...
	 * STUVWXYZ-Wing is 7 cells (8 values) in an ALS ...
	 * </pre>
	 * BigWings are named for the values in the ALS, so WXYZ-Wing is on an ALS
	 * of 3 cells with 4 values. Each value is ascribed a name: w, x, y, and z.
	 * Likewise for each extra value.
	 * <p>
	 * Note that 7 cells is far as we can go with 9 cells in a region. I dont
	 * really understand why. Ask a smart bastard. What I can tell you is that
	 * 8 wastes time, rather a lot of it, to find diddly-squat. Sigh.
	 *
	 * @return where any hint/s found
	 */
	@Override
	protected boolean findAlsHints() {											//     13,075
		// fastard: predeclare ALL vars -> no stackwork til we hint -> speed!
		Idx[] vBuds; // als.vBuds: common buddies of vs in this ALS
		Idx t0,t1; // fastard: temp Idx pointers for inline idx ops-> speed!
		Als als; // the Almost Locked Set: N cells sharing N+1 maybes
		boolean[][] vBudsB; // fastard: do hammered "has" operation AFAP!
							// als.vBudsB: boolean common buddies of vs in ALS
		long vc0; int vc1; // victims Exploded Idx
		int wa[], wn, wi, w // weak values array, number of, index, value
		  , i // the als index
		  , alsMaybes // als.maybes: a bitset of the values in this ALS
		  , j // the current index in biva/MAYBES
		  , b // indice of the bivalue cell
		  // WARN: I evidently do NOT understand roles of x and z values,
		  // but hobiwans code works anyway, so I advise dont ____ with it!
		  , x // the primary (mandatory) link value
		  , z // the secondary (optional) link value
		  , bivs // the candidates of the current bivalue cell
		  , tmp // a temporary variable: set, read and forget.
		  // fastard: inline Idx ops -> no stackwork till we hint -> speed!
		;
		boolean xWing // do all xs in the als see the biv?
		  , zWing // do all zs in the als see the biv?
		  , both // xWing & zWing, ie is this wing double-linked?
		  , zStrong // are there any strong eliminations on z?
		  , xStrong // are there any strong eliminations on x?
		  , weak // are there any weak elims (from the ALS only)?
		  , any // are there ANY eliminations?
		;
		boolean result = false; // presume no hints will be found
		final Pots reds = new Pots(); // eliminations if any
		// get an array of indices of all bivalue cells in grid.
		// nb: Generator force-fetches getBivalue (grid changes underneath).
		final int[] bivArray = grid.getBivalue(Run.isGenerator()).toArrayNew();
		final int bivN = bivArray.length; // number of bivAarray
		if ( bivN < 1 )
			return result; // Shft-F5 Sux!
		// foreach ALS (Almost Locked Set: N cells sharing N+1 maybes)
		i = 0;
		do {																	//  3,044,735
			als = ALSS[i];
			vBuds = als.vBuds; // idx of buddies of vs in ALS
			vBudsB = als.vBudsB; // vBuds in a boolean array for speed
			alsMaybes = als.maybes; // bitset of potential values
			// foreach bivalue cell (has two maybes); biv for short
			j = 0;
			do {																// 42,687,392
				// get biv.maybes
				bivs = maybes[bivArray[j]];
				// if this biv shares both maybes with this ALS
				if ( (alsMaybes & bivs) == bivs ) {								// 18,349,274
					// read x (lower) and z (higher) values from biv.maybes
					x = VALUESES[bivs][0];
					z = VALUESES[bivs][1];
					// get biv.indice
					b = bivArray[j];
					// calculate xWing := alss[i].vBuds[x].has(b)
					//       and zWing := alss[i].vBuds[z].has(b)
					// ie do all xs/zs in this ALS see this bivalue cell?
					// SPEED: Als.vBudsB is just cheating! Weez likes it!
					// if biv is a common bud of this ALS on x, z, or both
					if ( (xWing=vBudsB[x][b]) | (zWing=vBudsB[z][b]) ) {		//  1,140,004
						// this ALS+biv form a BigWing; but any elims?
						// if z ONLY swap x and z, to make z primary value.
						// WARN: Noncomprendez x and z role swapping!!!
						if ( !xWing ) { // 273,713
							tmp = x;
							x = z;
							z = tmp;
						}
						// seek strong elims on z (the primary link)
						// strong elims are zs seeing biv and all zs in ALS
						// fastard: inlined all Idx ops, for speed, so the
						// search run sans invocations til we hint (rare).
						any = zStrong = ( (vc0=(t0=BUDDIES[b]).m0 & (t1=vBuds[z]).m0)
										| (vc1=t0.m1 & t1.m1) ) > 0
						  && reds.upsertAll(vc0,vc1, maybes, VSHFT[z], DUMMY);
						// if both x and z are linked
						if ( both = xWing & zWing ) {							//    581,893
							// seek strong elims on x (the secondary link)
							// fastard: t0 is already set to BUDDIES[b]
							any |= xStrong = ( (vc0=t0.m0 & (t1=vBuds[x]).m0)
											 | (vc1=t0.m1 & t1.m1) ) > 0
							  && reds.upsertAll(vc0,vc1, maybes, VSHFT[x], DUMMY);
							// seek weak elims on als.maybes ^ biv.maybes
							//                 ie maybes only in this ALS
							// weak elims are ws seeing all ws in ALS
							weak = false;
							wa = VALUESES[alsMaybes^bivs];
							wn = wa.length;
							wi = 0;
							do {												// 1,951,859
								any |= weak |= ((t1=vBuds[w=wa[wi]]).m0 | t1.m1) > 0
								  && reds.upsertAll(vBuds[w], maybes, VSHFT[w], DUMMY);
							} while (++wi < wn);
							// if this wing has no weak-links
							if ( !weak ) {										//    581,737
								// double linked = xStrong & zStrong
								both = xStrong & zStrong;
								// no zs means swap x and z, to
								// make x the primary link value.
								if ( !zStrong ) {								//    581,719
									tmp = x;
									x = z;
									z = tmp;
								}
							}
						}
						if ( any ) { // 1,213
							// FOUND a BigWing
							result = true;
							final Cell[] alsCells = als.idx.cellsNew(cells);
							final Pots oranges = new Pots(alsCells, x);
							oranges.put(b, VSHFT[x]);
							// WARN: x and z are reversed. You fix it!
							// Evidently I do NOT understand there roles!
							if ( accu.add(new BigWingsHint(grid, this
							    , reds.copyAndClear()
								, cells[b] // bivalue cell
								, z, x, both, als, alsCells, oranges)) ) {
								return result;
							}
						}
					}
				}
			} while (++j < bivN);
		} while (++i < numAlss);
		return result;
	}

}
