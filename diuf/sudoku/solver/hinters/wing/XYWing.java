/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Idx;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * XYWing implements both the XY-Wing and XYZ-Wing Sudoku solving techniques.
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
 * Confused yet? Read it again, and again; then look at the code.
 */
public final class XYWing extends AHinter {

	private final boolean isXYZ; // true=XYZ_Wing, false=XY_Wing

	private final int intersectionSize; // intersectionSize = isXYZ ? 1 : 0

	public XYWing(Tech tech) {
		super(tech); //degree: XY_Wing=2, XYZ_Wing=3;
		this.isXYZ = tech==Tech.XYZ_Wing;
		this.intersectionSize = isXYZ ? 1 : 0; // ie this.degree - 2
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Cell[] xyBiBuds; // buddies of xy with maybes.size==2
		XYWingHint hint;
		int xyb, xzb; // xy and xz .maybes.bits
		final int degree = this.degree;
		final int interSize = this.intersectionSize;
		final Idx bivalueCells = grid.getBivalueCells(); // CACHED!
		// xys are cells in grid with maybes.size == degree (XY=2, XYZ=3)
		// nb: create new arrays to leave the CAS free for xzs; The trick is to
		// avoid creating thousands of garbage arrays; 1 per hinter is fine.
		// We do this for XY-Wing: to NOT set the same CAS array twice, to two
		// different cell sets, which won't bloody work, will it. sigh.
		final Cell[] xys;
		if ( degree == 2 ) // XY-Wing
			xys = bivalueCells.cells(grid, new Cell[bivalueCells.size()]);
		else { // XYZ-Wing
			Idx xysIdx = grid.getIdx(new Idx(), (c) -> {
				return c.maybes.size == 3; // XY=2, XYZ=3
			});
			xys = xysIdx.cells(grid, new Cell[xysIdx.size()]);
		}
		// an Idx of buddies of xy with maybes.size==2
		final Idx xyBiBudsIdx = new Idx();
		// presume failure, ie that no hint will be found
		boolean result = false;
		// foreach xy (the locus cell) in the grid
		// nb: xy would be called xyz in XYZWing, coz it has 3 values: x, y, z
		for ( Cell xy : xys ) { // the locus cell
			if ( xyBiBudsIdx.setAndAny(xy.buds, bivalueCells) ) {
				xyb = xy.maybes.bits;
				// foreach bivalue buddy of xy
				for ( Cell xz : xyBiBuds=xyBiBudsIdx.cells(grid) ) {
					// this means: xy.maybes - xz.maybes == 1 maybe (ie z)
					if ( VSIZE[xyb & ~xz.maybes.bits] == 1 ) {
						xzb = xz.maybes.bits;
						// foreach bivalue buddy of xy (again)
						for ( Cell yz : xyBiBuds ) {
							if ( yz != xz // reference-equals OK
							  && VSIZE[xyb | xzb | yz.maybes.bits] == 3 // union
							  && VSIZE[xyb & xzb & yz.maybes.bits] == interSize ) { // XY=0 or XYZ=1
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
		// These are siblings of all 2 (or 3 for XYZ) wing cells.

		// get an index of cells which see both xz and yz
		cmnIdx.setAnd(xz.buds, yz.buds);
		if ( isXYZ ) // and xy in an XYZ-Wing (victims see all 3 cells)
			cmnIdx.and(xy.buds);
		else // XY-Wings just needs xy removed.
			cmnIdx.remove(xy.i);
		if ( cmnIdx.none() )
			return null; // happens 14.48% of time in XYZ

		// we are not our own victims. Draculla insists!
		assert !cmnIdx.contains(xz.i);
		assert !cmnIdx.contains(yz.i);
		assert !cmnIdx.contains(xy.i);
		// get the victim cells at cmnIdx in grid
		// XY_Wing  pass 76,591 of 76,591 = skip  0.00%
		// XYZ_Wing pass 62,942 of 73,596 = skip 14.48%
		final int n = cmnIdx.cellsN(grid.cells, victims);
//		if ( n == 0 )
//			return null; // happens 14.48% of time in XYZ
		assert n > 0;

		// get the zCand (the z value as a bitset) to remove from victims.
		final int zCand = xz.maybes.bits & yz.maybes.bits; // intersection
		if ( zCand == 0 ) // happened in generate IDKFA
			return null; // should never happen
		// any crapenstances should set-off this assert (programmers only).
		// zCand, as the name suggests, should contain only one value.
		assert Integer.bitCount(zCand) == 1 : "bitCount("+zCand+") != 1"
				+ " at xy="+xy+" xz="+xz+" yz="+yz;

		// Get the removable (red) potential values.
		Pots reds = null;
		for ( int i=0; i<n; ++i ) // nb '<n' (NOT the whole victimsArray)
			if ( (victims[i].maybes.bits & zCand) != 0 ) {
				if(reds==null) reds = new Pots();
				reds.put(victims[i], new Values(zCand, false));
			}
		// XY_Wing  pass 524 of 76,591 = skip 99.32%
		// XYZ_Wing pass 326 of 62,942 = skip 99.48%
		if ( reds == null )
			return null; // skip 99% of cases.

		// ----------------------------------------------------------
		// Performance is no issue from here down
		// ----------------------------------------------------------

		// Create and return the hint
		return new XYWingHint(this, reds, isXYZ, xy, xz, yz, FIRST_VALUE[zCand]);
	}
	// it's faster to re-use a fixed array than it is to clean, allocate, and
	// free repeatedly; especially when there's no requirement to clean it.
	private final Cell[] victims = new Cell[18]; // 18 is max common siblings
	private final Idx cmnIdx = new Idx();

}
