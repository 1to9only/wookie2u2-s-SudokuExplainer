package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.Arrays;
import static diuf.sudoku.Grid.BITIDX_BUDDIES;
import static diuf.sudoku.Grid.BITIDX_FOLLOWING;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VSHFT;


/**
 * Implementation of the "S/T/U/V/WXYZ-Wing" solving techniques.
 * <pre>
 * Similar to ALS-XZ
 * with the larger ALS being of 3..8 cells
 * *     WXYZ_Wing the ALS has 3 cells sized 2..4
 * *    VWXYZ_Wing the ALS has 4 cells sized 2..5
 * *   UVWXYZ_Wing the ALS has 5 cells sized 2..6
 * *  TUVWXYZ_Wing the ALS has 6 cells sized 2..6
 * * STUVWXYZ_Wing the ALS has 7 cells sized 2..8
 * and the smaller ALS being just a bivalue cell;
 * catches the double linked version which is similar to Sue-De-Coq
 * <p>
 * NOTE: BigWing is only use of BitIdx, which pushes feelers into everything,
 * so replacing BitIdx with the "normal" Idx eradicates lots of code. KRC has
 * tried and failed to do this a couple of times. In the end I kept BitIdx,
 * letting it grow into every-bloody-thing. sigh.
 * </pre>
 */
public class BigWing extends AHinter {

	/**
	 * Does value form a Wing in these cells?
	 *
	 * @param value the candidate value
	 * @param yz the bivalue cell (completes the wing)
	 * @param als the cells in the almost locked set (guts of the wing)
	 * @return do all ALS cells which maybe value see the yz cell?
	 */
	private static boolean isWing(int value, Cell yz, Cell[] als) {
		final int sv = VSHFT[value];
		for ( Cell c : als )
			if ( (c.maybes.bits & sv)!=0 && yz.notSees[c.i] )
				return false;
		return true;
	}

	/**
	 * Weak eliminate 'value' from the 'wing' pattern, adding
	 * eliminations to reds, and return were any found?
	 * <p>
	 * Note that weak and strong are separate methods for speed only.
	 *
	 * @param victims I reuse THE instance: contents don't matter.
	 * @param value the value to eliminate
	 * @param als an array of the ALS cells
	 * @param yz the bivalue cell which completes the Wing pattern
	 * @param reds theReds: THE removable (red) potentials
	 * @return where any eliminations found?
	 */
	private static boolean weak(BitIdx victims, int value, Cell[] als
			, Cell yz, Pots reds) {
		final int sv = VSHFT[value]; //shiftedValue
		for ( Cell c : als )
			if ( (c.maybes.bits & sv) != 0 )
				victims.retainAll(BITIDX_BUDDIES[c.i]);
		// This is the difference between strong and weak. For a weak elim each
		// victim does NOT need to be a buddy of the yz-cell, so we just remove
		// yz from the victims. One is not one's own victim. Draculla insists.
		victims.remove(yz);
		if ( victims.isEmpty() )
			return false;
		boolean result = false;
		for ( Cell victim : victims )
			result |= reds.upsert(victim, value);
		return result;
	}

	/**
	 * Strong eliminate 'value' from the 'wing' pattern, adding
	 * eliminations to reds, and return were any found?
	 * <p>
	 * Note that weak and strong are separate methods for speed only.
	 *
	 * @param victims I reuse THE instance: contents don't matter.
	 * @param value the value to eliminate
	 * @param als an array of the ALS cells
	 * @param yz the bivalue cell which completes the Wing pattern
	 * @param reds theReds: THE removable (red) potentials
	 * @return where any eliminations found?
	 */
	private static boolean strong(BitIdx victims, int value, Cell[] als
			, Cell yz, Pots reds) {
		final int sv = VSHFT[value]; //shiftedValue
		for ( Cell c : als )
			if ( (c.maybes.bits & sv) != 0 )
				victims.retainAll(BITIDX_BUDDIES[c.i]);
		// This is the difference between strong and weak. In a strong elim the
		// victim is a buddy of ALL cells which maybe v, including the yz cell.
		victims.retainAll(BITIDX_BUDDIES[yz.i]);
		if ( victims.isEmpty() )
			return false;
		boolean result = false;
		for ( Cell victim : victims )
			result |= reds.upsert(victim, value);
		return result;
	}

	/**
	 * Find any eliminations in these wing cells.
	 * <p>
	 * NB: the x and z values, and both are in an instance of the XZ class, to
	 * enable me to set both and swap x and z, and have my caller see those
	 * changes; ie it's a mutable DTO, which suits multithreading, even though
	 * this code is not currently (ie yet) multithreaded. For efficiency, we
	 * create a single instance of XZ at the start of each BigWing.findHints
	 * and pass it around; so that each thread has it's own single instance.
	 *
	 * @param vs indices of cells in grid which maybe each value 1..9
	 * @param victims I reuse THE instance: contents don't matter
	 * @param wingCands the candidates of all the wing cells combined
	 * @param yz the bivalue which completes the Wing pattern
	 * @param xz the single instance of XZ containing the x and z values, and
	 *  the both (isDoubleLinked) flag, so that x and z can be swapped and
	 *  those changes are visible to my caller.
	 * @param als an array of the ALS cells
	 * @param reds the Pots (a single instance) to add any eliminations too.
	 *  This parameter exists only for speed. We reuse a single instance rather
	 *  than create an empty Pots in 99+% of calls to eliminate.<br>
	 *  If there are any eliminations theReds is copied and cleared after the
	 *  hint is created. Upon return reds contains any eliminations.
	 * @return are there any eliminations?
	 */
	private static boolean eliminate(final BitIdx[] vs, BitIdx victims
			, int wingCands, Cell yz, XZ xz, Cell[] als, Pots reds) {
		// find strong links on the zValue
		final boolean strongZ =
				strong(victims.set(vs[xz.z]), xz.z, als, yz, reds);
		// if both x and z conform to the Wing pattern
		if ( xz.both ) {
			// find strong links on the xValue
			final boolean strongX =
					strong(victims.set(vs[xz.x]), xz.x, als, yz, reds);
			// find weak links
			boolean weak = false;
			for ( int w : VALUESES[wingCands ^ yz.maybes.bits] ) //ie weakCands
				weak |= weak(victims.set(vs[w]), w, als, yz, reds);
			// is it really double linked?
			if ( !weak ) {
				if ( !strongZ ) {
					xz.both = false;
					xz.swap();
				} else if ( !strongX )
					xz.both = false;
			}
		}
		return !reds.isEmpty();
	}

	// ============================ instance stuff ============================

	// degree is the number of cells in an ALS.
	private final int degreePlus2 = degree + 2;
	// degree is the number of cells in an ALS.
	private final int degreeMinus1 = degree - 1;
	// A BitIdx of the cells at each level 0..degreeMinus1
	private final BitIdx[] sets = new BitIdx[degree];
	// the $degree cells in this ALS (Almost Locked Set)
	private final Cell[] als = new Cell[degree];
	// candidates of index+1 cells combined
	private final int[] cands = new int[degree];
	// this single instance of the XZ class holds the x and z values (in ALS-XZ
	// terms), which are the two values of the yz cell (in Wing terms); as well
	// as the both (isDoubleLinked) flag. Yes, it's a bit confusing.
	private final XZ xz = new XZ();
	// the removable (red) potentials
	private final Pots theReds = new Pots();
	// the victims are the removable cells
	private final BitIdx victims = new BitIdx();
	// the yzs cell set
	private final BitIdx yzs = new BitIdx();

// for uncached get for use in gemSolve (BigWing is no longer in gemSolve)
//	private final Grid.CellFilter candidateFilter = new Grid.CellFilter() {
//		@Override
//		public boolean accept(Cell c) {
//			return c.maybes.size>0 && c.maybes.size<degreePlus2;
//		}
//	};

	// ---- set and cleared by findHints ----
	// accu.isSingle() ie is only one hint sought
	private boolean onlyOne;
	// the cells in the current grid with two potential values
	private BitIdx bivalueCells;
	// indices of grid cells which maybe each value 1..9
	private BitIdx[] candidates;
	// the hints collector
	private IAccumulator accu;

	// ---- recurse variables ----
	private boolean xWing, zWing;

	public BigWing(Tech tech) {
		super(tech);
		// nb: sets[0] instance is set directly
		for ( int i=1; i<degree; ++i )
			sets[i] = new BitIdx();
	}

	/**
	 * Find first/all big-wings of size $degree in Grid and add them to accu.
	 * First or all depends on the IAccumulator: if accu.add returns true then
	 * I exit-early. The $degree is set by the Tech passed to my constructor.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		this.accu = accu;
		yzs.grid = grid;
		victims.grid = grid;
		for ( int i=1; i<degree; ++i )
			sets[i].grid = grid;
		// WEIRD: gemSolve doesn't increment AHint.hintNumber so cache under
		// getBitIdxBivalue gets dirty, so use the uncached version instead.
		// NOW: gemSolve dropped BigWing, so we cache.
		bivalueCells = grid.getBitIdxBivalue(); // cached
//		bivalueCells = grid.getBitIdx(BIVALUE_FILTER); // not cached
		candidates = grid.getBitIdxs(); // cached
//		candidates = grid.getBitIdxsImpl(); // not cached
		onlyOne = accu.isSingle();
		// presume that no hint will be found
		boolean result = false;
		try {
			// get the candidate cells, with size 2..4
			// WEIRD: gemSolve (see above)
			sets[0] = grid.getBitIdxEmpties().where((c) -> {
				return c.maybes.size < degreePlus2;
			}); // empties cache goes dirty in gemSolve
//			sets[0] = grid.getBitIdx(candidateFilter); // not cached
			// recurse to build-up $degree ALS cells; hints to accu, return any
			result = recurse(0);
		} finally {
			// forget all grid and cell references (for GC)
			yzs.grid = null;
			victims.grid = null;
			for ( int i=0; i<degree; ++i )
				sets[i].grid = null;
			bivalueCells = null;
			this.accu = null;
			candidates = null;
			Arrays.fill(als, null);
		}
		return result;
	}

	/**
	 * Recursively build-up $degree cells in the ALS array, then check each YZ
	 * cell for a big-wing; add hints to accu; returning any.
	 * <p>
	 * Note that recursion is used just put an arbitrary number of cells in the
	 * ALS array (there's no fancy permutations search or anything); its just
	 * the first way I thought of. There may be a faster iterative way. All
	 * recurse variables (except result) are fields, to minimise stack usage.
	 * The only exception to this rule is the hint, which happens so rarely it
	 * makes no difference. Note that eliminate is called 30,000-odd times in
	 * top1465, so performance matters all the way to the bottom of the jar.
	 * <p>
	 * All my helper methods are statics, so everything is passed into them,
	 * and there are no leakages.
	 *
	 * @param i
	 * @return
	 */
	private boolean recurse(int i) {
		assert i>=0 && i<degree; // i is an index
		boolean result = false;
		for ( Cell c : sets[i] ) {
			als[i] = c;
			if ( i == 0 ) { // the first cell is handled differently
				// sets[0] contains only cells with <= degree maybes,
				// so there's no need to check that here.
				cands[0] = c.maybes.bits;
				// check for enough cells (we need degree cells for an ALS)
				if ( sets[1].setAndMin(sets[0], BITIDX_FOLLOWING[c.i], degreeMinus1)
				  // and move right to the next ALS cell
				  && recurse(1) ) {
					result = true;
					if ( onlyOne )
						return result; // exit-early
				}
			} else if ( i < degreeMinus1 ) { // incomplete ALS
				// if existing + this cell together have <= degree+1 maybes
				if ( VSIZE[cands[i]=cands[i-1]|c.maybes.bits] < degreePlus2
				  // and theres enough cells to my right; we need degree
				  // cells to form an ALS, of which this is the (i+1)'th
				  && sets[i+1].setAndMin(sets[i], BITIDX_FOLLOWING[c.i], degreeMinus1-i)
				  // and move right to the next ALS cell
				  && recurse(i+1) ) {
					result = true;
					if ( onlyOne )
						return result; // exit-early
				}
			} else { // complete ALS ($degree cells)
				assert i == degreeMinus1; // it's an index so it's zero based
				// degree+1 maybes in degree cells is an Almost Locked Set
				if ( VSIZE[cands[i]=cands[i-1]|c.maybes.bits] == degreePlus1 ) {
					yzs.set(BITIDX_BUDDIES[als[0].i]);
					for ( int j=1; j<degree; ++j )
						yzs.addAll(BITIDX_BUDDIES[als[j].i]);
					yzs.removeAll(als);
					if ( yzs.retainAllAny(bivalueCells) ) {
						for ( Cell yz : yzs ) {
							// both yz.maybes must be shared with the ALS
							if ( VSIZE[cands[i] & yz.maybes.bits] == 2 ) {
								// get x and z values (in ALS-XZ terms)
								xz.set(yz);
								// is there a wing on x and/or z?
								xWing = isWing(xz.x, yz, als);
								zWing = isWing(xz.z, yz, als);
								if ( xWing | zWing ) {
									// found a BigWing pattern, but does it
									// eliminate anything?
									// if only z is linked then swap x and z
									if ( !xWing )
										xz.swap();
									// double linked
									xz.both = xWing & zWing;
									// find any eliminations
									if ( eliminate(candidates, victims, cands[i]
											, yz, xz, als, theReds) ) {
										// FOUND a BigWing on x and possibly z
										final Pots oranges = new Pots();
										oranges.put(yz, new Values(xz.x));
										for ( Cell cc : als )
										  if ( cc.maybe(xz.x) )
											oranges.put(cc, new Values(xz.x));
										// we found one
										result = true;
										// create the hint and add it to accu
										final AHint hint = new BigWingHint(this
												, theReds.copyAndClear(), yz
												, xz.x, xz.z, xz.both, cands[i]
												, als, oranges);
										if ( accu.add(hint) )
											return result; // exit-early
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * XZ allows is to swap x and z, permanently. There is only one instance of
	 * the XZ class in existence at any one time.
	 * <p>
	 * I'm called XZ (in ALS-XZ terms) but set takes yz-cell (in Wing terms).
	 * My variable names are ALS-XZ-based. It's all too bloody confusing, IMHO.
	 * A better name would be BivalueCell, and "attach" the cell itself, to
	 * pass it around as one reference (instead of yz and XZ, which is nuts).
	 *
	 * @author Keith Corlett 2021-01-12
	 */
	private class XZ {
		int x; // the x value (may be swapped)
		int z; // the z value (may be swapped)
		boolean both; // do both x and z fit the Wing pattern?
		/**
		 * Set my x and z values from the given yz-cell, which must be bivalue.
		 * @param yz the bivalue Cell whose values will be set. The lower value
		 *  is x, and the higher is y. This is arbitrary. If z forms a BigWing
		 *  and x does not then the x and z values are swapped, so that when we
		 *  are all done the x value always forms a Wing (the z value may not).
		 */
		void set(Cell yz) {
			assert yz.maybes.size == 2; // ensure it's a bivalue cell
			x = FIRST_VALUE[yz.maybes.bits];
			z = FIRST_VALUE[yz.maybes.bits & ~VSHFT[x]];
		}
		/**
		 * Swap the x and z values.
		 */
		void swap() {
			int tmp = x;
			x = z;
			z = tmp;
		}
	}

}
