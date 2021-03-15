package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.Values.VSIZE;
import java.util.Arrays;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.Grid.AFTER;
import static diuf.sudoku.Grid.BUDETTES;


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
 * </pre>
 */
public class BigWing extends AHinter {

	/**
	 * Does it match the wing pattern on value, ie:
	 * Do all the ALS cells which maybe value see yz.
	 *
	 * @param value
	 * @param yz
	 * @param als
	 * @return
	 */
	private static boolean isWing(int value, Cell yz, Cell[] als) {
		final int sv = VSHFT[value];
		for ( Cell c : als )
			if ( (c.maybes.bits & sv)!=0 && yz.notSees[c.i] )
				return false;
		return true;
	}

	/**
	 * Weak eliminate 'value' from the 'wing' cells, adding eliminations to
	 * theReds, and return were any found?
	 *
	 * @param victims contents don't matter, I'm re-using THE instance.
	 * @param value the value to eliminate
	 * @param wing either wing (for weak) or all (for strong) cells
	 * @param yz required for weak, null for strong
	 * @return where any eliminations found?
	 */
	private static boolean weak(BitIdx victims, int value, Cell[] wing, Cell yz
			, Pots theReds) {
		final int sv = VSHFT[value]; //shiftedValue
		for ( Cell c : wing )
			if ( (sv & c.maybes.bits) != 0 )
//				victims.retainAll(c.visible());
				victims.retainAll(BUDETTES[c.i]);
		// THIS is the difference between strong and weak. For a weak elim the
		// victim does NOT need to be a buddy of the yz-cell, so we just remove
		// yz from the victims. We cannot be our own victim. Draculla insists.
		victims.remove(yz);
		if ( victims.isEmpty() )
			return false;
		boolean result = false;
		for ( Cell victim : victims )
			result |= theReds.upsert(victim, value);
		return result;
	}

	/**
	 * Strong eliminate 'value' from the 'wing' cells, adding eliminations to
	 * theReds, and return were any found?
	 *
	 * @param victims contents don't matter, I'm re-using THE instance.
	 * @param value the value to eliminate
	 * @param wing either wing (for weak) or all (for strong) cells
	 * @param yz required for weak, null for strong
	 * @return where any eliminations found?
	 */
	private static boolean strong(BitIdx victims, int value, Cell[] wing
			, Cell yz, Pots theReds) {
		final int sv = VSHFT[value]; //shiftedValue
		for ( Cell c : wing )
			if ( (sv & c.maybes.bits) != 0 )
				victims.retainAll(BUDETTES[c.i]);
		// THIS is the difference between strong and weak. In a strong elim the
		// victim is a buddy of ALL cells which maybe v, including the yz cell.
		victims.retainAll(BUDETTES[yz.i]);
		if ( victims.isEmpty() )
			return false;
		boolean result = false;
		for ( Cell victim : victims )
			result |= theReds.upsert(victim, value);
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
	 * @param vs cells in grid which maybe each value 1..9
	 * @param victims contents don't matter, I'm re-using THE instance.
	 * @param wingCands the candidates of all the wing cells combined
	 * @param yz required for weak, null for strong
	 * @param als the ALS cells
	 * @param theReds the Pots (a single instance) to add eliminations too.
	 *  This parameter exists only for speed. We use a single instance rather
	 *  than create an empty Pots 99+% of eliminate calls.<br>
	 *  If there are any eliminations theReds is copied, and cleared; so it's
	 *  ALWAYS empty upon return. The returned Pots contains the eliminations.
	 * @return the eliminations, if any; else null.
	 */
	private static Pots eliminate(final BitIdx[] vs, BitIdx victims
			, int wingCands, Cell yz, XZ yzVs, Cell[] als, Pots theReds) {

		// find strong links on zValue
		final boolean strongZ =
				strong(victims.set(vs[yzVs.z]), yzVs.z, als, yz, theReds);
		if ( yzVs.both ) {
			// find strong links on xValue
			final boolean strongX =
					strong(victims.set(vs[yzVs.x]), yzVs.x, als, yz, theReds);
			// find weak links
			boolean weak = false;
			for ( int w : VALUESES[wingCands ^ yz.maybes.bits] ) // weakCands
				weak |= weak(victims.set(vs[w]), w, als, yz, theReds);
			// is it really double linked?
			if ( !weak ) {
				if ( !strongZ ) {
					yzVs.both = false;
					yzVs.swap();
				} else if ( !strongX )
					yzVs.both = false;
			}
		}
		if ( theReds.isEmpty() )
			return null; // none
		Pots reds = new Pots(theReds);
		theReds.clear();
		return reds;
	}

	// ============================ instance stuff ============================

	// degree is the number of cells in an ALS.
	private final int degreePlus2 = degree + 2;
	// degree is the number of cells in an ALS.
	private final int degreeMinus1 = degree - 1;
	// A BitIdx of the cells at each level 0..degreeMinus1
	private final BitIdx[] sets = new BitIdx[degree];
	// the $degree cells in this ALS
	private final Cell[] als = new Cell[degree];
	// candidates of index+1 cells combined
	private final int[] cands = new int[degree];
	// the XZ class holds the x and z values, as in ALS-XZ;
	// and the both (ie isDoubleLinked) flag.
	private final XZ yzVs = new XZ();
	// the removable (red) potentials
	private final Pots theReds = new Pots();
	// the victims are the removable cells
	private final BitIdx victims = new BitIdx();
	// the yzs cell set
	private final BitIdx yzs = new BitIdx();

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
	private Pots reds;

	public BigWing(Tech tech) {
		super(tech);
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
		yzs.grid = grid;
		victims.grid = grid;
		for ( int i=1; i<degree; ++i )
			sets[i].grid = grid;
		bivalueCells = grid.getBitIdxBivalue();
		this.accu = accu;
		candidates = grid.getBitIdxs();
		onlyOne = accu.isSingle();
		// presume that no hint will be found
		boolean result = false;
		try {
			// get the candidate cells, with size 2..4
			sets[0] = grid.getBitIdxEmpties().where((c) -> {
				return c.maybes.size < degreePlus2;
			});
			// recurse to build-up $degree ALS cells, examine for wing;
			// add hints to accu, return any.
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
//		assert i>-1 && i<degree;
		boolean result = false;
		for ( Cell c : sets[i] ) {
			als[i] = c;
			if ( i == 0 ) { // the first cell is handled differently
				// sets[0] contains only cells with <= degree maybes,
				// so there's no need to check that here.
				cands[0] = c.maybes.bits;
				// check for enough cells (we need degree cells for an ALS)
				if ( sets[1].setAndMin(sets[0], AFTER[c.i], degreeMinus1)
				  // and go move right to the next ALS cell
				  && recurse(1) ) {
					result = true;
					if ( onlyOne )
						return result; // exit-early
				}
			} else if ( i < degreeMinus1 ) { // incomplete ALS
				// if als cells + this cell have <= degree+1 maybes
				if ( VSIZE[cands[i]=cands[i-1]|c.maybes.bits] < degreePlus2
				  // and theres enough cells to my right; we need degree
				  // cells to form an ALS, of which this is the (i+1)'th
				  && sets[i+1].setAndMin(sets[i], AFTER[c.i], degreeMinus1-i)
				  // and move right to the next ALS cell
				  && recurse(i+1) ) {
					result = true;
					if ( onlyOne )
						return result; // exit-early
				}
			} else { // complete ALS ($degree cells)
//				assert i == degreeMinus1; // it's an index so it's zero based
				// degree+1 maybes in degree cells is an Almost Locked Set
				if ( VSIZE[cands[i]=cands[i-1]|c.maybes.bits] == degreePlus1 ) {
					yzs.set(BUDETTES[als[0].i]);
					for ( int j=1; j<degree; ++j )
						yzs.addAll(BUDETTES[als[j].i]);
					yzs.removeAll(als);
					if ( yzs.retainAllAny(bivalueCells) ) {
						for ( Cell yz : yzs ) {
							// both yz.maybes must be shared with the ALS
							if ( VSIZE[cands[i] & yz.maybes.bits] == 2 ) {
								// get x and z (in ALS-XZ parlance)
								yzVs.set(yz);
								// is there a wing on x and/or z?
								xWing = isWing(yzVs.x, yz, als);
								zWing = isWing(yzVs.z, yz, als);
								if ( xWing || zWing ) {
									// found a TUVWXYZ-Wing pattern
									if ( !xWing ) // single linked on z
										yzVs.swap();
									// double linked
									yzVs.both = xWing & zWing;
									// find eliminations
									reds = eliminate(candidates, victims
										, cands[i], yz, yzVs, als, theReds);
									if ( reds != null ) {
										// create the hint
										AHint hint = new BigWingHint(this, reds
											, yz, yzVs, cands[i], als);
										result = true;
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

}
