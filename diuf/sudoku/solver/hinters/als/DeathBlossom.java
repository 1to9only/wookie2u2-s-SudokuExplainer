/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.*;
import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.utils.Log;

/**
 * DeathBlossom implements the {@link Tech#DeathBlossom} Sudoku solving
 * technique. A DeathBlossom is a stem-cell having two or three maybes, with
 * an ALS (Almost Locked Set) linked to each maybe of the stem-cell; where all
 * ALSs also share another common value that is NOT in the stem-cell. The whole
 * shebang forms a locked-set on all the other common maybe/s, eliminating
 * these values from any cells seeing all instances of that value in all of the
 * ALSs. Yep, its a head____. Read it again. Alternate explanation follows.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * but in DeathBlossom only does not bother determining the RCCs (connections).
 * He calls my "custom" findHints method passing along the ALSs. To be clear:
 * I determine my own RCCs!
 * <p>
 * Explanation from https://www.sudopedia.org/wiki/Solving_Technique
 * <p>
 * A Death Blossom consists of a stem cell and an Almost Locked Set (or ALS)
 * for each of the stem cells candidates. The ALS associated with a particular
 * stem candidate has that value as one of its own candidates, and within the
 * ALS, every cell that has the value as a candidate sees the stem cell. The
 * ALSs cannot overlap; ie no cell can belong to more than one ALS. Also, there
 * must be at least one value that is a candidate of every ALS, but is not a
 * candidate of the stem cell; these are the value/s (usually 1) to eliminate.
 * <p>
 * Having found a Death Blossom, if an outside cell (not in the ALSs) sees all
 * occurrences of that value (which is in all the ALSs and not in the stem) in
 * the DeathBlossom, then we can eliminate that value from the outside cell.
 * <pre>
 * KRC 2021-11-02 alssByValue {@code List<Als>[]} now Als[][] avoiding Iterator
 * to save about 3 seconds over top1465, costs more code, and a bit more RAM.
 *
 * KRC 2021-12-30 enspeedonate: Explode Idxs and ops. Just pass down the built
 * up vars, instead of building and unbuilding them; that was stupid. Also use
 * a seperate abvBuds array (coincident with alssByValue) of the als.vBuds, to
 * save deferencing vBuds from als-struct 700 million times.
 * PRE: 7,042,097,300   9966  706,612  2543  2,769,208 DeathBlossom
 * PST: 5,063,695,500   9966  508,097  2543  1,991,229 DeathBlossom
 * The new version is about 30% faster than the old version.
 * similarly avbIndexes array saves deferencing als.idx 68 million times
 * XXX: 4,946,477,700   9966  496,335  2543  1,945,134 DeathBlossom
 * I moved DeathBlossom up the charts, before Finned*Fish
 * XXX: 4,957,628,300  10337  479,600  2580  1,921,561 DeathBlossom
 * I relegate DB back to where it was, coz it increases calls of ALL hinters.
 * Also, it is slower per call than Finned*Fish, so promo was a false economy.
 *
 * KRC 2023-06-29 replaced abvBuddies with abvBuddiesX (Exploded Idxs)
 * And ____ me sideways its slower! Again! WTF?!?!
 * PRE: 6,040,552,900  11573  521,952  2567  2,353,156 DeathBlossom
 * PST: 6,460,096,700  11573  558,204  2567  2,516,593 DeathBlossom
 *
 * KRC 2023-08-25 split search method out of recurse, for speed.
 * PRE: 4,694,146,900  11639  403,311  2487  1,887,473 DeathBlossom
 * PST: 3,872,779,800  11639  332,741  2487  1,557,209 DeathBlossom
 * </pre>
 *
 * @author Keith Corlett 2020-01-13
 */
public final class DeathBlossom extends AAlsHinter
		implements diuf.sudoku.solver.hinters.IPrepare
//				 , diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[14];

	// ALSS_BY_VALUE_SIZE: I suspect 256 is more than big-enough, but YMMV!
	// The starting size of the alss-array foreach value, which can grow, but
	// thats slower, with an O(n) copy on each growth. Big-enough is faster!
	private static final int ABV_SIZE = 256;

	// an array-of-arrays of all the ALSs containing each value 1..9
	private Als[][] alssByValue;
	// budsByValue[v][index] is alssByValue[v][index].vBudsB[v]
	// just to save dereferencing the als and the double-v-lookup.
	// it's about 600ms faster over top1465, coz it runs 600 million times.
	private boolean[][][] budsByValue;
	// number of alss per value 1..9
	private int[] numAlssByValue;

	// deathBlossomAlssByValue: alss in this DeathBlossom by stem.maybe.
	// Each ALS is associated with a maybe of the stem cell.
	// NOTE: I am knee dbAlssByValue in order to clearly differentiate from
	// alssByValue (containing ALL alss) which got me confused hence I ____ed
	// it all up, so I renamed this one, to make them LOOK different.
	// recurse builds-up dbAbv when it allocates an ALS to each stem.maybe.
	// recurse also tears-down dbAbv upon ascent. A self-cleaning algorithm.
	private Als[] dbAbv;

	@Override
	public void setFields(final Grid grid) {
		int v;
		super.setFields(grid);
		alssByValue = new Als[VALUE_CEILING][];
		v=1; do alssByValue[v] = new Als[ABV_SIZE]; while(++v<VALUE_CEILING);
		budsByValue = new boolean[VALUE_CEILING][][];
		v=1; do budsByValue[v] = new boolean[ABV_SIZE][]; while(++v<VALUE_CEILING);
		numAlssByValue = new int[VALUE_CEILING];
		dbAbv = new Als[VALUE_CEILING];
	}

	@Override
	public void clearFields() {
		super.clearFields();
		alssByValue = null;
		budsByValue = null;
		numAlssByValue = null;
		dbAbv = null;
	}

	/**
	 * Constructor.
	 */
	public DeathBlossom() {
		super(Tech.DeathBlossom);
	}

	// copy an alssByValue array (or any other type T)
	private static <T> T[] copy(final T[] src, final T[] dst, final int length) {
		System.arraycopy(src, 0, dst, 0, length);
		return dst;
	}

	// copy a budsByValue array
	private static boolean[][] copy(final boolean[][] src, final boolean[][] dst, final int length) {
		System.arraycopy(src, 0, dst, 0, length);
		return dst;
	}

	// enlarge existing array by 32. It wont need much more, coz it starts at
	// more than large-enough to cover all known use-cases; grow handles only
	// exceptional use-cases. If it grows twice increase ABV_SIZE, and/or the
	// growth size, and/or the growth scheme to double as per Collections.
	private void growByValueArrays(final int v) {
		final int oldSize = alssByValue[v].length;
		final int newSize = oldSize + 32;
		Log.println("WARN: growByValueArrays: "+v+": "+oldSize+" -> "+newSize);
		alssByValue[v] = copy(alssByValue[v], new Als[newSize], oldSize);
		budsByValue[v] = copy(budsByValue[v], new boolean[newSize][], oldSize);
	}

	/**
	 * Finds DeathBlossom hints in the given Grid.
	 * <p>
	 * <pre>
	 * A DeathBlossom is a stem cell and an ALS (petal) for each maybe.
	 * * The ALS for each stem.maybe has that value, and
	 *   each ALS.cell which maybe value sees the stem.
	 * * The ALSs cant overlap.
	 * * Theres atleast one maybe in every ALS but not the stem.
	 * </pre>
	 * <p>
	 * This findAlsHints method is called via supers IHinter findHints method,
	 * passing in the rccs and alss (common to all of the als hinters).
	 * <p>
	 * I call recurse for each eligible stem cell; recurse associates each stem
	 * maybe with an ALS containing that value, and then searches each complete
	 * DeathBlossom for eliminations.
	 *
	 * @return any hint/s found?
	 */
	@Override
	protected boolean findAlsHints() {											//   12,101
		Als als;
		int j, i = 0;
		boolean result = false;
		// build alssByValue: all ALSs indexed by each maybe, to expedite
		// recurse by doing some preparation minimal times.
		do {
			als = ALSS[i];
			for ( int v : VALUESES[als.buddedMaybes] ) {
				if ( (j=numAlssByValue[v]++) == alssByValue[v].length )
					growByValueArrays(v);
				alssByValue[v][j] = als;
				budsByValue[v][j] = als.vBudsB[v];
			}
		} while (++i < numAlss);
		// a stem is a cell with 2..3 maybes: I tap-out early at 3, coz I
		// find 0 hints on stems with 4+ maybes in top1465, but this may
		// occur in other puzzles, I simply cannot say, so all other code
		// handles 4; and correctness demands that we increase < 4 to < 5,
		// until it is proven that 4 CAN NOT produce a hint.
		//
		// This implementation is excessively efficient, coz I am too dumb
		// to prove that 4maybes cannot hint. I observe it DOES NOT HINT
		// and impose a reasonable (but arbitrary until proven otherwise)
		// limit, because I like speed (electron, not chemical).
		//
		// foreach stem cell indice in the grid
		i = 0;
		do {
			// if this cell has 2..3 maybes its a potential stem cell
			if ( sizes[i]>1 && sizes[i]<4
			  // Seek an ALS for each stem.maybe (each freeCand).
			  // @commonCands values other than stem.maybes common to all
			  // ALS in this DeathBlossom. Starts as all values other than
			  // stem.maybes, then reduced to those common with each ALS as
			  // its added to the DeathBlossom.
			  // @freeCands: a stem.maybe links each ALS to the stem cell,
			  // ie to the current DeathBlossom. freeCands is a bitset of
			  // values that are yet to be associated. Starts as all maybes
			  // of the stem cell, then we remove each value when an ALS is
			  // linked to the stem-cell using this value.
			  && recurse(i, BITS9 & ~maybes[i], maybes[i], 0L,0)
			) {																//    2,134
				result = true;
				if ( oneHintOnly )
					return result; // exit early
			}
		} while (++i < GRID_SIZE);
		return result;
	}

	/**
	 * Recurse associates each maybe of the stem-cell with every "eligible" ALS
	 * in turn, to build a complete DeathBlossom (an ALS for each maybe of the
	 * Stem cell), then for each completed DeathBlossom it calls eliminations
	 * to do the eliminations.
	 * <pre>
	 * For an ALS (a petal) to "eligable" to be part of this DeathBlossom:
	 * * this ALS shares a value with the stem-cell, where every instance of
	 *   that value in this ALS sees (same box/row/col) the stem-cell;
	 * * The ALSs in a DB do NOT physically overlap.
	 * * All ALSs in a DB share a common value thats NOT in the stem-cell;
	 * </pre>
	 *
	 * @param stem: indice of a cell with 2/3 maybes, each of which I associate
	 *  with an ALS sharing a value where all instances of value in the ALS see
	 *  the stem. The ALSs cannot overlap, and all ALSs share a common value,
	 *  other than stem.maybes, which becomes the value/s to try to eliminate
	 * @param cmnCands a bitset of values common to all ALSs in this DB
	 * @param freeCands stem cell maybes yet to be associated with an ALS
	 * @param db0 an exploded Idx of the cells in this DeathBlossom
	 * @param db1 an exploded Idx of the cells in this DeathBlossom
	 * @return any hint/s found
	 */
	private boolean recurse(final int stem, int cmnCands, final int freeCands
			, final long db0, final int db1 ) {									//    8,432,076
		// associate the next freeCands with each eligable ALS in turn
		Als als; // the current als
		int cmn; // common to ALSs already in DB and this ALS.
		// presume that no hint will be found
		boolean result = false;
		// get the next free (unassociated) stem.maybe that I use to associate
		// the stem-cell with each eligable ALS in turn.
		final int v = VFIRST[freeCands];
		// get the remaining freeStemCands
		final int free = freeCands & ~VSHFT[v];
		// a complete DB has no freeStemCands remaining to be ascribed an ALS.
		// If it's incomplete we ascribe the next available ALS in turn;
		// thus (overall) we examine every possible combination of ALSs.
		final boolean incomplete = free > 0;
		// get ALSs having v as one of its maybes from alssByValue
		final Als[] alss = alssByValue[v];
		// get buds for these alss from budsByValue, for speed
		// nb: budsByValue[v][i] = alssByValue[i].vBudsB[v], to enable me to
		// quickly check if stem is a common buddy of each ALS in alsByValue.
		// It's a parallel array to avoid derefencing the ALS 638,994,086 times
		// This saves something like 3 seconds in top1465. I'd hoped for more.
		final boolean[][] buds = budsByValue[v];
		// get the number of ALSs having v. If none then we are done here.
		final int n = numAlssByValue[v];
		// foreach ALS with v as one of its maybes, associate this ALS and call
		// recurse again, to associate the next stem.maybe, til every maybe is
		// associated with an ALS; then search completed DB for eliminations.
		// NOTE WELL: pretest-loop coz (n=numAlssByValue[v]) is 0, rarely.
		for ( int i=0; i<n; ++i ) {												//  638,994,086
			// if all vs in this ALS see the stem cell
			if ( buds[i][stem]													//   72,353,500
			  // and this ALS doesnt overlap with the ALSs already in the DB
			  && ( (db0 & (als=alss[i]).m0)
				 | (db1 & als.m1) ) < 1L // 27bit								//   31,981,691
			  // and this ALSs shares a maybe, other than stem.maybes,
			  // with all the ALSs already in the DB
			  && (cmn=cmnCands & als.maybes) > 0								//   19,406,040
			) {
				// add this ALS to dbAbv (deathBlossomAlssByValue), ergo
				// associate this als with the stem-cell via this value
				dbAbv[v] = als;
				// if any stem.maybes remain to be associated with an ALS
				if ( incomplete ) {												//    8,067,197
					// try to associate an ALS with the next freeCandidate
					// add this ALS to cells already in DeathBlossom
					if ( recurse(stem, cmn, free, db0|als.m0, db1|als.m1) ) {
						result = true;
						if ( oneHintOnly )  {
							dbAbv[v] = null; // I clean-up myself as I go
							return result; // exit early
						}
					}
				// else search this completed DB for eliminations				//   11,338,843
				} else if ( eliminations(stem, cmn) ) {
					result = true;
					if ( oneHintOnly )  {
						dbAbv[v] = null; // I clean-up myself as I go
						return result; // exit early
					}
				}
				dbAbv[v] = null; // I clean-up myself as I go
			}
		}
		return result;
	}

	/**
	 * Each maybe of the Stem cell is now associated with an ALS (the petal),
	 * so search this completed DeathBlossom for any eliminations.
	 * <pre>
	 * * The ALS associated with each stem.maybes has that value; and
	 *   every cell in this ALS which maybe value sees the stem-cell.
	 * * The ALSs in the DB do NOT overlap.
	 * * All ALSs in the DB share atleast one commonValue not in stem.maybes.
	 * * The whole DB forms a Locked Set on each of its commonMaybes.
	 *   Note well that stem.maybes are excluded from commonMaybes.
	 * * Victims: eliminate each commonValue from external cells (not in ALSs
	 *   or stem) that see every cell in the DB that maybe commonValue.
	 * </pre>
	 *
	 * @param stem the indice of the stem-cell
	 * @param commonCands a bitset of the values common to every ALS in
	 *  this DeathBlossom, excluding stem.maybes
	 * @return any hint found
	 */
	private boolean eliminations(final int stem, final int commonCands){		//   11,338,843
		Idx idx; // grid.idxs[commonValue]
		// bad-name: x marks ye spots, but it does make the code fit on a line.
		// x is dbAbv[stemValue].vBudsX[commonValue]: external cells seeing all
		// commonValues in this ALS, thats linked to da stem-cell by stemValue.
		// External means "except cells in this ALS", and when we and-together
		// the vBuds of every ALS in DB we get "common buddies of the whole DB"
		// which (as always) excludes the DB cells themselves.
		Idx x;
		long vc0; int vc1; // victims exploded Idx
		boolean result = false; // presume that a hint will not be found
		Pots reds = null; // removable (red) Cell=>Values
		// faster to look them up ONCE, I think
		final int[] stemValues = VALUESES[maybes[stem]];
		// victims start as all occurrences of commonValue in the grid, and are
		// reduced to those which see every commonValue in each ALS in the DB.
		// When no victims remain then try the next commonValue.
		// nb: each ALS-cell isnt a victim coz its not in its own als.vBuds and
		// stem-cell isnt a victim coz none of its maybes are in commonMaybes,
		// hence victims and the DB are guaranteed to be disjunct setsOf cells.
		// foreach value common to every ALS in this DB, except stem.maybes
		for ( int cmnValue : VALUESES[commonCands] ) {							//   14,303,549
			vc0 = (idx=idxs[cmnValue]).m0;
			vc1 = idx.m1;
			// foreach stemValue, just to get da ALS dats linked via dis value
			// nb: NON_LOOP eradicates an ok var. It's a tad faster.
			NON_LOOP: for(;;) {
			for ( int stemValue : stemValues )									//   21,151,416
				// remove victims that do NOT see each commonValue in this ALS.
				// If no victims remain then there are no elims on commonValue,
				// but there might still be elims on a subsequent commonValue.
				// Rarely (~2%) a DB eliminates on multiple commonValues.
				if ( ( (vc0 &= (x=dbAbv[stemValue].vBuds[cmnValue]).m0)
					 | (vc1 &= x.m1) ) < 1L )									//   14,301,371
					break NON_LOOP;
			if(reds==null) reds = new Pots();
			result |= reds.upsertAll(vc0,vc1, maybes, VSHFT[cmnValue], DUMMY);
			assert result;
			break;
			}
		}
		if ( result )															//        2,134
			// FOUND a DeathBlossom!
			accu.add(new DeathBlossomHint(grid, this, reds, cells[stem], dbAbv.clone()));
		return result;
	}

}
