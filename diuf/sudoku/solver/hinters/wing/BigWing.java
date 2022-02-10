package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.EXCEPT;
import static diuf.sudoku.Grid.LATER_BUDS;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxL;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * BigWing implements the S/T/U/V/WXYZ-Wing Sudoku solving techniques.
 * <pre>
 * Similar to ALS-XZ, with the ALS being of 3..8 cells:
 *     WXYZ_Wing the ALS has 3 cells sized 2..4
 *    VWXYZ_Wing the ALS has 4 cells sized 2..5
 *   UVWXYZ_Wing the ALS has 5 cells sized 2..6
 *  TUVWXYZ_Wing the ALS has 6 cells sized 2..6
 * STUVWXYZ_Wing the ALS has 7 cells sized 2..8
 * and the "XZ" being just a bivalue cell;
 * which catches the double linked version that is similar to Sue-De-Coq.
 *
 * NOTE: BigWing is the only use of BitIdx, which has feelers in everything, so
 * replacing BitIdx with the "normal" Idx eradicates lots of code. KRC tried
 * and failed to do this a couple of times. In the end I kept BitIdx, letting
 * it grow it's feelers into every-bloody-thing. sigh.
 *
 * KRC 2021-06-10 Having failed to replace BigWing with the BigWings class, I
 * decided to just replaced BitIdx with Idx in BigWing, to allow me to remove
 * the BitIdx class, which I hate because you have to set and clear it's grid.
 * Also deleted all BitIdx dependant code from Grid, so all-up we're about a
 * thousand lines lighter.
 *
 * KRC 2021-09-13 Removed the isWing method because it is not necessary, and
 * the current alternate: Idx.hasAll and Idx.setAndSome is a bit faster.
 *
 * KRC 2021-12-01 Instituted Idx.toArrayLease, which grants a Lease over an
 * int-array. The lease implements java.io.Closeable, so it's "used" just like
 * any java.io.Class. The close method returns the array to the cache, where
 * it's available for the next lease-request. If there is no existing array
 * then a new one is created, added to the cache, and returned, so it's
 * available for next-time. This creates a minial static cache of int-arrays in
 * the Idx class, which I hope is faster than creating new arrays on the fly,
 * which is pretty slow in Java, especially when you consider that there's no
 * requirement to clear each array before use because it's the right size, so
 * each and every element is overwritten every single time the array is used;
 * so you can stick ALL that slow crap up your jaxy Jackson. I Fixed It!
 *
 * Ideally, I'd move this mechanism to an IntArray class, for use everywhere I
 * need an int-array, but that sounds like a lot of work.
 *
 * KRC 2021-12-18 split eliminate into strong and weak, with static final REDS.
 * I also tried with no methods using IntArray.doWhile instead, but that's
 * slower: I guess doWhile invokes both accept and possibly the lambda for
 * each ALS indice, where-as this is all in ONE stackframe.
 * </pre>
 *
 * @author Keith Corlett 2010 May IIRC My code based on Juillerat's concept.
 */
public class BigWing extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[6];

	// indices of cells from which this value can be removed
	private static final Idx VICTIMS = new Idx();
	// the removable (red) potentials
	private static final Pots REDS = new Pots();

	/**
	 * Eliminate 'v' from this Wing (the ALS and yz cell), adding eliminations
	 * to reds, and return were any found?
	 * <p>
	 * eliminate is called about 3,000,000 times, ie hammered. I'm static coz
	 * Java's this injection is a bit slow, but makes my fields static. A new
	 * stackframe in recurse turns out to be faster as well as more convenient.
	 * <p>
	 * eliminate is now split into strong and weak, for speed: <br>
	 * strong: start with all grid.cells which maybe v, that see yz <br>
	 * weak: start with all grid.cells which maybe v, except yz
	 * <p>
	 * eliminate is faster than equivalent IntArray.doWhile, coz this is all
	 * in ONE stackframe, where-as doWhile invokes once-or-twice per als-cell
	 *
	 * @param v the value to eliminate; xz.z or xz.x
	 * @param sv VSHFT[v] is a param only for speed: it seems to be faster if
	 *  the whole stackframe is populated in the call, which I don't understand
	 * @param als array of indices of cells in this Almost Locked Set
	 * @param yz indice of the bivalue cell which completes the Wing pattern
	 * @return where any eliminations found?
	 */
	private static boolean strong(final int v, final int sv, final int[] als, final int yz) {
		VICTIMS.setAnd(gridIdxs[v], BUDDIES[yz]);
		// and buddy of each ALS cell which maybe v
		for ( int i : als )
			if ( (gridMaybes[i] & sv) != 0
			  && !VICTIMS.andAny(BUDDIES[i]) )
				return false;
		return REDS.upsertAll(VICTIMS, gridMaybes, gridCells, sv, DUMMY);
	}
	private static boolean weak(final int v, final int sv, final int[] als, final int yz) {
		VICTIMS.setAnd(gridIdxs[v], EXCEPT[yz]);
		// and buddy of each ALS cell which maybe v
		for ( int i : als )
			if ( (gridMaybes[i] & sv) != 0
			  && !VICTIMS.andAny(BUDDIES[i]) )
				return false;
		return REDS.upsertAll(VICTIMS, gridMaybes, gridCells, sv, DUMMY);
	}


	// ============================ instance stuff ============================

	// degree is the number of cells in an ALS.
	// WXYZ=3, UWXYZ=4, TUWXYZ=5, STUWXYZ=6
	private final int degreePlus2 = degree + 2;
	// Idx of the cells at each level 0..degreeMinus1
	private final Idx[] sets = new Idx[degree];
	// an array of the indices of the $degree cells in this Almost Locked Set.
	// As we recurse the als-array contains 'n' cells. The als-array is full
	// when n == degreeMinus1, so we check this als for BigWing/s. So 'n' is da
	// last valid index in the als-array, not the "normal" size. sigh.
	private final int[] als = new int[degree];
	// candidates of index+1 cells combined
	private final int[] cands = new int[degree];
	// the yz's cell set
	private final Idx yzs = new Idx();
	private int[] yza; // yz-array
	private int yzi; // index in yza
	private int yzn; // yza.length
	private int yz; // the indice of the yz cell

	// ---- set and cleared by findHints ----
	// the hints collector
	private IAccumulator accu;
	// accu.isSingle() ie is only one hint sought
	private boolean onlyOne;
	// the grid to search
	private Grid grid;
	// indices of cells in grid with maybesSize==2
	private IdxL bivi;
	// indices of cells in grid with value==0
	private Idx empties;

	// THESE are static because eliminate is static
	// the grid.cells to search
	private static Cell[] gridCells;
	// the grid.maybes to search
	private static int[] gridMaybes;
	// the grid.idxs to search: indices of grid cells which maybe value 1..9
	private static Idx[] gridIdxs;


	// recurse vars are fields to save on stack-work.
	private boolean xWing, zWing, both, zStrong, xStrong, weak, any;
	private int j, x, z, yzCands, alsCands;
	// Idx of the ALS cells
	private final Idx alsIdx = new Idx();
	// Idx of the ALS cells which maybe v, where v is x or z
	private final Idx vi = new Idx();

	public BigWing(final Tech tech) {
		super(tech);
		assert tech.in(Tech.BIG_WING_TECHS) : "Not in BIG_WING_TECHS: "+tech;
		// nb: sets[0] instance is set directly
		for ( int i=1; i<degree; ++i ) {
			sets[i] = new Idx();
		}
	}

	/**
	 * Find first/all big-wings of size $degree in Grid and add them to accu.
	 * First or all depends on the IAccumulator: if accu.add returns true then
	 * I exit-early. The $degree is set by the Tech passed to my constructor.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// pre-check for trouble in paradise
		assert grid.maybesCheck();
		assert grid.sizeCheck();
		// presume that no hint will be found
		boolean result = false;
		try {
			this.accu = accu;
			onlyOne = accu.isSingle();
			this.grid = grid;
			gridCells = grid.cells;
			gridMaybes = grid.maybes;
			gridIdxs = grid.idxs;
			bivi = grid.getBivalue();
			empties = grid.getEmpties();
			// get indices of cells with size 2..degree+1
			sets[0] = empties.where((i)->grid.size[i] < degreePlus2);
			// foreach first-cell in each possible ALS
			try ( final IALease lease = sets[0].toArrayLease() ) {
				for ( int i : lease.array ) {
					cands[0] = gridMaybes[als[0] = i];
					// check for enough cells (we need degree cells for an ALS)
					// sets[0] is cells with <= degree+1 maybes so no recheck here
					if ( sets[1].setAndMin(sets[0], LATER_BUDS[i], degreeMinus1)
					  // move right to find the next ALS cell, to find a BigWing
					  && (result |= recurse(0, 1, 2))
					  && onlyOne )
						break;
				}
			}
		} finally {
			this.accu = null;
			this.grid = null;
			gridCells = null;
			gridMaybes = null;
			gridIdxs = null;
			bivi = null;
			empties = null;
			sets[0] = null; // the other Idx's are mine
		}
		return result;
	}

//// MIA BigWingTest: STUVWXYZ_Wing: A4 B4 C4 E4 F4 H4 I4 and G5 on 3 (A5-3, B5-3, C5-3)
//	private static final int[] DEBUG_ALS = Grid.indices("A4 B4 C4 E4 F4 H4 I4");

	/**
	 * Recursively build-up $degree cells in the ALS array, then check each YZ
	 * bivalue cell for a BigWing; add hints to accu; returning any.
	 * <p>
	 * Note that recursion is used just put an arbitrary number of cells in the
	 * ALS array (there's no fancy permutations search or anything); its just
	 * the first way I thought of. There may be a faster iterative way. All
	 * recurse variables (except result) are fields, to minimise stack usage.
	 * eliminate is called 30,000 times in top1465 so performance matters.
	 * <p>
	 * All my helper methods are statics, so everything is passed into them,
	 * and there are no leakages.
	 *
	 * @param m Pass 0. I am just a fast n - 1
	 * @param n Pass 1. number of cells in the als. I recurse to degreeMinus1.
	 *  Note that als[0], cands[0], and sets[0] are all pre-initialised
	 * @param o Pass 2. I am just a fast n + 1
	 * @return any hint/s found?
	 */
	private boolean recurse(final int m, final int n, final int o) {
		boolean result = false;
		// nb: recurse takes a Lease over a cached array, rather than wear the
		// expense of creating a new array each time, for more speed! The Lease
		// implements java.io.Closeable, so it's "used" like a Reader/etc.
		try ( final IALease lease = sets[n].toArrayLease() ) {
			for ( int i : lease.array ) {
				als[n] = i;
				if ( n < degreeMinus1 ) { // incomplete ALS
					// if existing + this cell together have <= degree+1 maybes
					if ( VSIZE[cands[n]=cands[m]|gridMaybes[i]] < degreePlus2
					  // need degree cells to form an ALS, this is the (i+1)'th
					  && sets[o].setAndMin(sets[n], LATER_BUDS[i], degreeMinus1-n)
					  // move right to find the next ALS cell, to find a BigWing
					  && (result |= recurse(n, o, o+1))
					  && onlyOne
					)
						return result;
				// else the ALS array is now full, so if these degree cells have
				// degree+1 maybes then it's an Almost Locked Set to be searched
				} else {
					// WARNING: o is the degree, not n as you might expect. sigh.
					// The 'als' array is size degree, so no need to pass length!
					assert n == degreeMinus1; // never more
//// MIA BigWingTest: STUVWXYZ_Wing: A4 B4 C4 E4 F4 H4 I4 and G5 on 3 (A5-3, B5-3, C5-3)
//if ( tech==Tech.STUVWXYZ_Wing && Arrays.equals(DEBUG_ALS, als) )
//	Debug.breakpoint();
					if ( VSIZE[cands[m]|gridMaybes[i]] == degreePlus1 ) {
//						++COUNTS[0]; // 2,497,696
						// faster to NOT cache this, and we're done recursing.
						cands[n] = cands[m] | gridMaybes[i];
						// examine this ALS against each of it's possible yz cells;
						// yz's are buddies of any ALS cell, except the ALS cells,
						// that are bivalue, and keep going if there are any.
						yzs.set(BUDDIES[als[0]]);
						for ( j=1; j<degree; ++j )
							yzs.or(BUDDIES[als[j]]);
						if ( yzs.andNot(alsIdx.set(als)).and(bivi).any() ) {
//							++COUNTS[1]; // 2,478,203
							alsCands = cands[n]; // do the array look-up ONCE
							try ( final IALease yzLease = yzs.toArrayLease() ) {
								for ( yza=yzLease.array,yzi=0,yzn=yza.length; yzi<yzn; ++yzi ) {
//									++COUNTS[2]; // 17,229,369
									// if yz shares both it's maybes with this ALS
									if ( (alsCands & (yzCands=gridMaybes[yz=yza[yzi]])) == yzCands
//									  && ++COUNTS[3] > 0L // 7,540,301
									  // and ( yz sees all x's in the ALS
									  && ( (xWing=BUDDIES[yz].hasAll(vi.setAnd(alsIdx, gridIdxs[x=VFIRST[yzCands]])))
										 // or yz sees all z's in the ALS )
										 | (zWing=BUDDIES[yz].hasAll(vi.setAnd(alsIdx, gridIdxs[z=VFIRST[yzCands & ~VSHFT[x]]]))) )
									) {
//										++COUNTS[4]; // 981,499
										// It's a BigWing, but any eliminations?
										if ( !xWing ) {
											j = x;
											x = z;
											z = j;
										}
										// MAG: 1,000,000
										// strong eliminate z from buds of als and yz
										any |= zStrong = strong(z, VSHFT[z], als, yz);
										if ( both = xWing & zWing ) {
											// MAG: 400,000
											// strong eliminate x from buds of als and yz
											any |= xStrong = strong(x, VSHFT[x], als, yz);
											// weak eliminate each w from buds of als EXCEPT yz
											// w's are alsCands EXCEPT the two yzCands
											weak = false;
											for ( int w : VALUESES[alsCands ^ yzCands] )
												// MAG: 1,600,000
												any |= weak |= weak(w, VSHFT[w], als, yz);
											if ( !weak ) {
												both = xStrong & zStrong;
												if ( !zStrong ) {
													j = x;
													x = z;
													z = j;
												}
											}
										}
										if ( any ) {
											// FOUND a BigWing
											final Cell yzCell = gridCells[yz];
											final Cell[] alsCells = grid.cellsNew(als);
											final Pots oranges = new Pots(alsCells, x);
											oranges.put(yzCell, VSHFT[x]);
											result = true;
											if ( accu.add(new BigWingHint(this, REDS.copyAndClear(), yzCell
													, x, z, both, alsCands, alsCells, oranges)) )
												return result;
										}
									}
								} // next yz
							}
						}
					}
				}
			} // next cell in this set
		}
		return result;
	}

}
