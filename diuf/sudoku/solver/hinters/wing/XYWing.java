/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Idx;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.Values.VFIRST;


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
 * therefore siblings of both xz and yz cannot be z. A "sibling" is a cell in
 * the same box, row, or col (and excludes the cell itself, so 20 cells).
 * <p>
 * In an <b>XYZ-Wing</b> the "locus" xyz cell has 3 potential values (x, y,
 * and z), so one of the three cells (xyz, xz, or yz) must contain the value z,
 * therefore any siblings of all three cells cannot be z.
 * The Cell xyz is just called xy because it can't have two names in the same
 * code, which is shared with XY-Wings; and xy is distinct, just not complete,
 * and therefore only demi-descriptive... but it works, so don't ____ with it.
 * <p>
 * Confused yet? Read it again, and again; then look at the code.
 * <pre>
 * KRC 2021-07-10 Smell no Cell: Swap cell arrays for int arrays, for speed.
 * To be clear, everything now uses indice-of-cell where it previously used the
 * Cell itself. I hope this will be faster.
 * I also removed createHint method, sticking it's contents back into findHints
 * so that there is NO stack-frame below findHints.
 * </pre>
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
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// All variables are pre-declared, ANSI-C style, for speed.
		// There is no stack-frame below this, til we new XYWingHint.
		AHint hint; // the bloody hint, if we ever find one
		int xyi // index of the xz Cell in xzs
		  , XYI // size of xzs
		  , yzi // index in xzs of the yz indice
		  , yz  // indice of yz
		  , xz  // indice of xz
		  , xym // xy.maybes
		  , xzm // xz.maybes
		  , yzm // yz.maybes
		  , zCand // bitset of the z-value, to remove from victims
		  , n   // number of victim cells
		  , i;  // the ubiqitious index
		boolean any; // were any redPots added
		// move all heap-references onto the stack, for speed
		final int degree = this.degree;
		final boolean isXYZ = this.isXYZ;
		final int intersectionSize = this.intersectionSize;
		final Idx xyBuds = this.xyBuds; // indices of buds of xy with 2 maybes
		final int[] xyBud = this.xyBud; // indices of buds of xy with 2 maybes
		final Idx victims = this.victims; // indices of victims
		final int[] victim = this.victim; // indices of victims
		final Pots redPots = this.redPots; // removable (red) Cell=>Values
		// array of each cells maybes, for speed.
		final int[] maybes = grid.maybes(); // O(81)
		// indices of bivalue cells
		final Idx bivi = grid.getBivalue(); // O(81) // fills the cache
		// indices of the xy (locus) cell
		// nb: OK to create a new array per hinter call, just never in loop.
		final int[] xys;
		if ( isXYZ ) // XYZ-Wing: three maybes
			// nb: hijack xyBivBuds rather than create a new temp Idx
			xys = grid.idx((cc)->cc.size==3, xyBuds).toArrayNew();
		else // XY-Wing: two maybes
			xys = bivi.toArrayNew();
		// presume failure, ie that no hint will be found
		boolean result = false;
		// foreach xy (the locus cell) in the grid
		// nb: XYZWing would call xy xyz, coz it has 3 values: x, y, z
		for ( int xy : xys ) // the locus cell
			if ( xyBuds.setAndAny(BUDDIES[xy], bivi) )
				// foreach xz in bivalue buds of xy
				for ( xym=maybes[xy],xyi=0,XYI=xyBuds.toArrayN(xyBud); xyi<XYI; ++xyi )
					// means: xy.maybes - xz.maybes == 1 maybe (ie z)
					if ( VSIZE[xym & ~maybes[xz=xyBud[xyi]]] == 1 )
						// foreach yz in bivalue buds of xy (again)
						for ( xzm=maybes[xz],yzi=0; yzi<XYI; ++yzi )
							// if these 3 cells share 3 values
							if ( VSIZE[xym | xzm | (yzm=maybes[xyBud[yzi]])] == 3 // union
							  // and these 3 cells all have 0=XY/1=XYZ value/s
							  && VSIZE[xym & xzm & yzm] == intersectionSize
							  // and xz and yz have 1 common value
							  // hence xz and yz cannot be the same cell
							  && VSIZE[zCand=xzm & yzm] == 1
							) {
							    // XY/Z_Wing found, but any victims?
								// find victims: siblings of both xz and yz
								//               (and xy in XYZ_Wing).
								victims.setAnd(BUDDIES[xz], BUDDIES[yz=xyBud[yzi]]);
								if ( isXYZ ) // victims see all 3 wing cells
									victims.and(BUDDIES[xy]);
								else // XY_Wing just remove xy
									victims.remove(xy);
								if ( victims.any() ) { // XYZ skips 14.48%
									// we are not our own victims.
									assert !victims.has(xz);
									assert !victims.has(yz);
									assert !victims.has(xy);
									// get removable (red) Cell=>Values.
									for ( any=false,i=0,n=victims.toArrayN(victim); i<n; ++i )
										if ( (maybes[victim[i]] & zCand) != 0 ) {
											redPots.put(grid.cells[victim[i]], zCand);
											any = true;
										}
									// XY_Wing  pass 524/76,591 skip 99.32%
									// XYZ_Wing pass 326/62,942 skip 99.48%
									if ( any ) {
										// ------------------------------------
										// Performance no problem here down
										// ------------------------------------
										// create and return the hint
										hint = new XYWingHint(this
												, redPots.copyAndClear()
												, isXYZ
												, grid.cells[xy]
												, grid.cells[xz]
												, grid.cells[yz]
												, VFIRST[zCand]
										);
										result = true;
										if ( accu.add(hint) )
											return result;
										hint = null;
									}
								}
							}
		return result;
	}
	// it's faster to re-use a fixed array than it is to clean, allocate, and
	// free repeatedly; especially when there's no requirement to clean it.
	private final int[] xyBud = new int[20]; // 20 is max siblings
	private final int[] victim = new int[18]; // 18 is max common siblings
	private final Idx victims = new Idx();
	private final Idx xyBuds = new Idx();
	private final Pots redPots = new Pots();

}
