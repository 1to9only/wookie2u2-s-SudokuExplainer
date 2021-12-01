package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.LATER_BUDS;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
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
//	private final long[] COUNTS = new long[5];

	/**
	 * Eliminate 'v' from this Wing, adding eliminations to reds,
	 * and return were any found?
	 * <p>
	 * I'm static because Java's this injection is a bit slow.
	 *
	 * @param isStrong are these eliminations strong ones. A strong elimination
	 *  sees all v's in the wing, including the yz-cell. A weak elimination
	 *  only applies to ALS-only-values in a double-linked wing, and must see
	 *  all v's in the ALS (not the yz-cell).
	 * @param v the value to eliminate; xz.z or xz.x
	 * @param als array of indices of cells in this Almost Locked Set
	 * @param yz indice of the bivalue cell which completes the Wing pattern
	 * @param reds the removable (red) potentials
	 * @return where any eliminations found?
	 */
	private static boolean elim(final boolean isStrong, final int v
			, final int[] als, final int yz, final Pots reds
			, final Cell[] cells) {
		final int sv = VSHFT[v]; //shiftedValue
		VICTIMS.set(idxs[v]);
		for ( int i : als )
			if ( (maybes[i] & sv) != 0 )
				VICTIMS.and(BUDDIES[i]);
		if ( isStrong ) // buddy of ALL cells which maybe v, including yz
			VICTIMS.and(BUDDIES[yz]);
		else // weak: just remove yz
			VICTIMS.remove(yz);
		if ( VICTIMS.none() )
			return false;
		boolean result = false;
		for ( Cell victim : VICTIMS.cells(cells) )
			// nb: upsert in case x and z elim from same cell (sigh)
			result |= reds.upsert(victim, sv, false);
		return result;
	}
	// the victims are the removable cells
	private static final Idx VICTIMS = new Idx();

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
	// the removable (red) potentials
	private final Pots reds = new Pots();
	// the yzs cell set
	private final Idx yzs = new Idx();

	// ---- set and cleared by findHints ----
	// the grid to search
	private Grid grid;
	// the grid.cells to search
	private Cell[] cells;
	// the hints collector
	private IAccumulator accu;
	// accu.isSingle() ie is only one hint sought
	private boolean onlyOne;
	// indices of cells in grid with maybesSize==2
	private Idx bivi;
	// indices of cells in grid with value==0
	private Idx empties;
	
	// THESE static coz elim is static coz Java's this injection is a bit slow
	// indices of grid cells which maybe each value 1..9
	private static Idx[] idxs;
	// the maybes of each of the 81 cells in the grid
	private static int[] maybes;

	public BigWing(Tech tech) {
		super(tech);
		assert tech.name().matches("S?T?U?V?WXYZ_Wing") : "Bad tech: "+tech;
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
		// presume that no hint will be found
		boolean result = false;
		try {
			this.grid = grid;
			this.cells = grid.cells;
			this.accu = accu;
			this.onlyOne = accu.isSingle();
			bivi = grid.getBivalue();
			idxs = grid.idxs;
			empties = grid.getEmpties();
			assert grid.maybesCheck();
			maybes = grid.maybes;
			// indices of cells with size 2..degree+1
			sets[0] = empties.where(grid.cells, (c) -> {
				return c.size>1 && c.size<degreePlus2;
			});
			// foreach first-cell in each possible ALS
			for ( int i : sets[0].toArrayA() ) {
				cands[0] = maybes[als[0] = i];
				// check for enough cells (we need degree cells for an ALS)
				// sets[0] is cells with <= degree+1 maybes so no recheck here
				if ( sets[1].setAndMin(sets[0], LATER_BUDS[i], degreeMinus1)
				  // move right to find the next ALS cell, to find a BigWing
				  && (result |= recurse(0, 1, 2))
				  && onlyOne
				) {
					break;
				}
			}
		} finally {
			// forget all grid and cell references (for GC)
			this.grid = null;
			this.cells = null;
			this.accu = null;
			this.bivi = null;
			this.empties = null;
			BigWing.idxs = null;
			BigWing.maybes = null;
			reds.clear();
			sets[0] = null; // the other Idx's are mine
		}
		return result;
	}
	
	// MIA BigWingTest: STUVWXYZ_Wing: A4 B4 C4 E4 F4 H4 I4 and G5 on 3 (A5-3, B5-3, C5-3)
	private static int[] DEBUG_ALS = Grid.indices("A4 B4 C4 E4 F4 H4 I4");

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
	 *  nb: als[0], cands[0], sets[0] are always pre-initialised
	 * @param o Pass 2. I am just a fast n + 1
	 * @return any hint/s found?
	 */
	private boolean recurse(final int m, final int n, final int o) {
		boolean result = false;
		// nb: in recursion we must create a new array each time.
		for ( int i : sets[n].toArray(new int[sets[n].size()]) ) {
			als[n] = i;
			if ( n < degreeMinus1 ) { // incomplete ALS
				// if existing + this cell together have <= degree+1 maybes
				if ( VSIZE[cands[n]=cands[m]|maybes[i]] < degreePlus2
				  // need degree cells to form an ALS, this is the (i+1)'th
				  && sets[o].setAndMin(sets[n], LATER_BUDS[i], degreeMinus1-n)
				  // move right to find the next ALS cell, to find a BigWing
				  && (result |= recurse(n, o, o+1))
				  && onlyOne
				) {
					return result;
				}
			// else the ALS array is now full, so if these degree cells have
			// degree+1 maybes then it's an Almost Locked Set to be searched
			} else {
				// WARNING: o is the degree, not n as you might expect. sigh.
				// The 'als' array is size degree, so no need to pass length!
				assert n == degreeMinus1; // never more
//// MIA BigWingTest: STUVWXYZ_Wing: A4 B4 C4 E4 F4 H4 I4 and G5 on 3 (A5-3, B5-3, C5-3)
//if ( tech==Tech.STUVWXYZ_Wing && Arrays.equals(DEBUG_ALS, als) ) {
//	Debug.breakpoint();
//}
					
				if ( VSIZE[cands[n]=cands[m]|maybes[i]] == degreePlus1 ) {
	//				++COUNTS[0]; // 2,497,696
					// examine this ALS against each of it's possible yz cells;
					// yz's are buddies of any ALS cell, except the ALS cells,
					// that are bivalue, and keep going if there are any.
					yzs.set(BUDDIES[als[0]]);
					for ( j=1; j<degree; ++j ) {
						yzs.or(BUDDIES[als[j]]);
					}
					if ( yzs.andNot(alsIdx.set(als)).and(bivi).any() ) {
	//					++COUNTS[1]; // 2,478,203
						alsCands = cands[n]; // do the array look-up ONCE
						// nb: must use a new array in recursion
						for ( int yz : yzs.toArrayNew() ) {
	//						++COUNTS[2]; // 17,229,369
							// if yz shares both it's maybes with this ALS
							if ( (alsCands & (yzCands=maybes[yz])) == yzCands
	//						  && ++COUNTS[3] > 0L // 7,540,301
							  // and ( yz sees all x's in the ALS
							  && ( (xWing=BUDDIES[yz].hasAll(vi.setAnd(alsIdx, idxs[x=VFIRST[yzCands]])))
								 // or yz sees all z's in the ALS )
								 | (zWing=BUDDIES[yz].hasAll(vi.setAnd(alsIdx, idxs[z=VFIRST[yzCands & ~VSHFT[x]]]))) )
							) {
	//							++COUNTS[4]; // 981,499
								// It's a BigWing, but any eliminations?
								if ( !xWing ) {
									j = x;
									x = z;
									z = j;
								}
								// WARN: pass o instead of n, ya idjit! n is degreeMinus1 and we need degree: the number of cells in the ALS. sigh.
								zStrong = elim(true, z, als, yz, reds, cells);
								if ( both = xWing & zWing ) {
									xStrong = elim(true, x, als, yz, reds, cells);
									weak = false;
									for ( int w : VALUESES[alsCands ^ yzCands] ) {
										weak |= elim(false, w, als, yz, reds, cells);
									}
									if ( !weak ) {
										both = xStrong & zStrong;
										if ( !zStrong ) {
											j = x;
											x = z;
											z = j;
										}
									}
								}
								if ( !reds.isEmpty() ) {
									// FOUND a BigWing on x and possibly z
									final Cell[] alsCells = grid.cells(als);
									final Pots oranges = new Pots(alsCells, x);
									final Cell yzCell = cells[yz];
									oranges.put(yzCell, VSHFT[x]);
									final AHint hint = new BigWingHint(this
											, reds.copyAndClear(), yzCell, x, z
											, both, alsCands, alsCells, oranges);
									result = true;
									if ( accu.add(hint) ) {
										return result;
									}
								}
							}
						} // next yz
					}
				}
			}
		} // next cell in this set
		return result;
	}
	// recurse variables are fields to save on stack-work.
	private boolean xWing, zWing, both, zStrong, xStrong, weak;
	private int j, x, z, yzCands, alsCands;
	// Idx of the ALS cells
	private final Idx alsIdx = new Idx();
	// Idx of the ALS cells which maybe v, where v is x or z
	private final Idx vi = new Idx();

}
