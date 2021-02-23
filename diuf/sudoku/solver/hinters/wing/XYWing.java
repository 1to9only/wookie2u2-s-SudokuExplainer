/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Idx;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.hinters.AHintNumberActivatableHinter;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * XYWing Implements both the XY-Wing and XYZ-Wing Sudoku solving techniques.
 * <p>
 * I found the XY/Z techniques near impossible to get my head around, so
 * here's an <b>explanation</b> for those who come after me.
 * <p>
 * In XYZ-land <b>x</b>, <b>y</b>, and <b>z</b> are potential cell values
 * (ie maybes) not matrix (grid) coordinates, so:<pre>
 *     xy maybe x or y (and no others); and
 *     xz maybe x or z (and no others); and
 *     yz maybe y or z (and no others).
 * </pre>
 * The important concept here is that the values x, y and z are shared.
 * <p>
 * In an <b>XY-Wing</b> the "locus" xy cell has 2 potential values, and it
 * also has two siblings (xz and yz) which also have 2 potential values. The
 * Cell xy (locus) shares the value x with xz, and the value y with yz; and
 * the two sibling Cells xz and yz also have a second value (z) in common.
 * Cell xy must be either x or y, so either xz or yz must contain the value z,
 * therefore siblings of both xz and yz cannot be z.
 * <p>
 * In an <b>XYZ-Wing</b> the "locus" xyz cell has 3 potential values (x, y,
 * and z), so one of the three cells (xyz, xz, or yz) must contain the value z,
 * therefore any siblings of all three cells cannot be z.
 * <p>
 * The Cell xyz is just called xy because it can't have two names in the same
 * code, which is shared with XY-Wings; and xy is distinct, just not complete,
 * and therefore only demi-descriptive... but it works, so don't ____ with it.
 * <p>
 * Confused yet? Read it again, and again; then take a geezer at the codez.
 */
public final class XYWing extends AHintNumberActivatableHinter
//		implements diuf.sudoku.solver.IReporter
{
//private int victCnt=0, victPass=0, emptyCnt=0, emptyPass=0;

	private final boolean isXYZ; // true=XYZ_Wing, false=XY_Wing

	private final int intersectionSize; // intersectionSize = isXYZ ? 1 : 0

	public XYWing(Tech tech, int firstHintNumber) {
		super(tech, firstHintNumber); //degree: XY_Wing=2, XYZ_Wing=3;
		this.isXYZ = tech==Tech.XYZ_Wing;
		this.intersectionSize = isXYZ ? 1 : 0; // ie this.degree - 2
	}

//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("// "+tech.name()+" pass victims %d of %d, empty %d of %d\n"
//				, victPass,victCnt, emptyPass,emptyCnt);
//	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// localise for speed
		final int[] SIZE = Values.SIZE;
		final int degree = this.degree;
		final int intersectionSize = this.intersectionSize;
		// local variables
		XYWingHint hint;
		Values xzm, yzm;
		int xyb, xzb; // xy and xz .maybes.bits
		// presume failure, ie that no hint will be found
		boolean result = false;
		// foreach xy (the locus cell) in the grid
		// nb: xy would be called xyz in XYZWing, coz it has 3 values: x, y, z
		for ( Cell xy : grid.cells ) { // the "locus" cell
			if ( xy.maybes.size == degree ) { // XY=2, XYZ=3
				xyb = xy.maybes.bits;
				// foreach sibling of xy (the locus cell)
				// FYI: first use of siblings. Bug there will show-up here.
				for ( Cell xz : xy.siblings ) {
					if ( (xzm=xz.maybes).size == 2
					  // this means: xy.maybes - xz.maybes == 1 maybe (ie z)
					  && SIZE[xyb & ~xzm.bits] == 1 ) {
						xzb = xzm.bits;
						// foreach sibling of the locus cell (again)
						for ( Cell yz : xy.siblings ) {
							if ( (yzm=yz.maybes).size == 2
							  && yz != xz // reference-equals OK
							  && SIZE[xyb | xzb | yzm.bits] == 3 // union
							  && SIZE[xyb & xzb & yzm.bits] == intersectionSize ) { // XY=0 or XYZ=1
								// XY/Z found, but does it remove any maybes?
								hint = createHint(grid, xy, xz, yz);
								result |= (hint != null);
								if ( accu.add(hint) )
									return true;
							}
						}
					}
				}
			}
		}
		return result;
	}

	private XYWingHint createHint(Grid grid, Cell xy, Cell xz, Cell yz) {
		// Build a set of cells from which we possibly could remove maybes.
		// These are siblings of all 2 (or 3 for XYZ) "main" cells.

		// get an index of siblings common to xz, yz
		cmnIdx.setAnd(xz.buds, yz.buds);
		if ( isXYZ ) // and xy in an XYZ-Wing (victims are siblings of all 3 cells)
			cmnIdx.and(xy.buds);
		else // XY-Wings need just xy removed.
			cmnIdx.remove(xy.i);
		// we are not our own victims. Draculla insists!
		assert !cmnIdx.contains(xz.i);
		assert !cmnIdx.contains(yz.i);
		assert !cmnIdx.contains(xy.i);
		// get the victim cells at cmnIdx in grid
//++victCnt;
		// XY_Wing  pass victims 76,591 of 76,591 = skip  0.00%
		// XYZ_Wing pass victims 62,942 of 73,596 = skip 14.48%
		final int n = cmnIdx.cellsN(grid, victims);
		if ( n == 0 )
			return null; // happens 14.48% of time in XYZ
//++victPass;

//		if ( false ) { // do it with Sets to check the idx results
//			final Set<Cell> ov = new LinkedHashSet<>(32, 0.75F); // originalVictims
//			ov.addAll(xz.getSeeSet());
//			ov.retainAll(yz.getSeeSet()); // intersection
//			if ( isXYZ ) // in an XYZ-Wing victims must be siblings of all 3 cells.
//				ov.retainAll(xy.getSeeSet()); // intersection
//			ov.remove(xy); // we are not our own victims. Draculla insists!
//			ov.remove(xz);
//			ov.remove(yz);
//			assert ov.size() == n
//					: " size " + diuf.sudoku.utils.Frmt.csv(ov)
//					+" != "+diuf.sudoku.utils.Frmt.csv(THE_VICTIMS);
//			Cell[] oa = ov.toArray(new Cell[ov.size()]);
//			// the two ways put cells in different orders, so we have to sort
//			// both arrays before we java.util.Arrays.equals them.
//			assert diuf.sudoku.utils.MyArrays.equalsSorted(oa, THE_VICTIMS, n)
//					: diuf.sudoku.utils.Frmt.csv(ov)
//					+" != "+diuf.sudoku.utils.Frmt.csv(THE_VICTIMS);
//		}

		// get the zMaybes (the z values as a bitset) to remove from victims.
		final int zMaybes = xz.maybes.bits & yz.maybes.bits; // intersection
		// crapenstances set-off this assert (programmers only).
		assert Integer.bitCount(zMaybes) == 1 : "bitCount("+zMaybes+") != 1"
				+ " at xy="+xy+" xz="+xz+" yz="+yz;

		// Get the removable (red) potential values.
		Pots redPots = null;
		Cell victim;
		for ( int i=0; i<n; ++i ) // nb '<n' (NOT just iterate victimsArray)
			if ( ((victim=victims[i]).maybes.bits & zMaybes) != 0 ) {
				if(redPots==null) redPots = new Pots();
				redPots.put(victim, new Values(zMaybes, 1, false));
			}
//++emptyCnt;
		// XY_Wing  pass empty 524 of 76,591 = skip 99.32%
		// XYZ_Wing pass empty 326 of 62,942 = skip 99.48%
		if ( redPots == null )
			return null; // skip 99% of cases.
//++emptyPass;

		// ----------------------------------------------------------
		// NOTE: Performance is not an issue from here down
		// ----------------------------------------------------------

		// Create and return the hint
		final int zValue = Indexes.NUM_TRAILING_ZEROS[zMaybes]+1;
		assert zMaybes == Values.SHFT[zValue]; // AIOOBE means its really ____ed
		return new XYWingHint(this, redPots, isXYZ, xy, xz, yz, zValue);
	}
	// it's faster to re-use a fixed array than it is to clean, allocate, free; 
	// clean, allocate, free; clean, allocate, free; are we there yet?
	// especially when (like this one) there's no requirement to clean it.
	private final Cell[] victims = new Cell[18]; // max possible number of common siblings
	private final Idx cmnIdx = new Idx();

}
