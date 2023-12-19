/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Idx;
import static diuf.sudoku.Grid.*;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;

/**
 * XYWing implements both {@link Tech#XY_Wing} and {@link Tech#XYZ_Wing} Sudoku
 * solving techniques.
 * <p>
 * In XY/Z-land <b>x</b>, <b>y</b>, and <b>z</b> are potential cell values
 * (ie maybes) not matrix (grid) coordinates, so in XY-land:<pre>
 *     cell xy maybe x or y (and no others); and
 *     cell xz maybe x or z (and no others); and
 *     cell yz maybe y or z (and no others).
 * </pre>
 * The important concept here is that the values x, y and z are shared between
 * these three cells, and xy sees both xz and yz, hence one of these two cells
 * must be z, so we can remove z from cells seeing both xz and yz.
 * <p>
 * In an <b>XY-Wing</b> the "locus" xy cell has two maybes, and it also has two
 * siblings (xz and yz) which also have two maybes. The "locus" xy cell shares
 * the value x with xz, and the value y with yz; and the two sibling cells xz
 * and yz also have a second value (z) in common. <br>
 * Cell xy must be either x or y, hence either xz or yz contains the value z,
 * therefore siblings of both xz and yz cannot be z. A "sibling" is a cell in
 * the same box, row, or col (and excludes the cell itself, so 20 siblings).
 * <p>
 * In an <b>XYZ-Wing</b> the "locus" xyz cell has three maybes (x, y, and z)
 * so one of the three cells (xyz, xz, or yz) must contain the value z, hence
 * siblings of all three cells cannot be z.
 * <p>
 * In this code the Cell xyz is called xy because that's what it is in XY-Wing.
 * This code also handles XYZ-Wings, but a variable cannot have two names, and
 * xy is distinctive and demi-descriptive even in the XYZ-Wing context.
 * <pre>
 * KRC 2021-07-10 Smell No Cell (very cheesey): use cell.indice, not Cell, for
 * speed. Moved createHint back into findHints for reduced stackwork.
 * </pre>
 */
public final class XYWing extends AHinter {

	private final boolean isXYZ; // true=XYZ_Wing, false=XY_Wing
	private final int intersectionSize; // intersectionSize = isXYZ ? 1 : 0;

	public XYWing(final Tech tech) {
		super(tech); //degree: XY_Wing=2, XYZ_Wing=3;
		this.isXYZ = tech==Tech.XYZ_Wing;
		this.intersectionSize = isXYZ ? 1 : 0; // ie this.degree - 2
	}

	@Override																	//      XY /     XYZ
	public boolean findHints() {												//  16,713 /  15,616
		// I am called 16,713/15,616 times XY_Wing/XYZ_Wing in top1465.
		// All vars predeclared (ANSI-C style) for ONE stackframe, til we hint.
		Idx t0; // temporary Idx pointer
		int xzi // index of the xz Cell in xzs
		, xzn // size of xzA (bivalue buddies of xy)
		, yzi // index in xzA of the yz indice
		, xz  // indice of xz cell
		, yz  // indice of yz cell
		, xym // xy.maybes
		, xzm // xz.maybes
		, xyz // xym | xzm;
		, x   // xym & xzm;
		, yzm // yz.maybes
		, z  // z-value, to remove from victims
		;
		long vc0; int vc1; // indices of cells to eliminate from, if any
		boolean result = false; // presume that no hint will be found
		final Idx xyBuds = new Idx(); // indices that see the xy cell
		final int[] xzArray = new int[NUM_SIBLINGS]; // bivalue buddies of xy
		// NOTE: I load the bivs cache
		final Idx bivs = grid.getBivalue(); // indices of bivalue cells
		// get xy (locus) cells: with 3 maybes in XYZ_Wing; else 2 in XY_Wing
		// nb: bivs are cached, trivs aren't coz I'm da only one dat wants em
		final int[] xyArray = isXYZ ? grid.indicesNew((c) -> c.size == 3)
									: bivs.toArrayNew();
		// foreach xy (locus) cell in the grid
		// nb: all my variables are named for XyWing, so xy it is.
		// and var x would be xz, coz it is 2 values: x and z.
		for ( int xy : xyArray ) { // the locus cell							// 243,234 / 301,382
			// now we seek two distinct bivalue buddies of the locus cell xy/z,
			// which we call xz and yz, after the values they share.
			// foreach xz in bivalue buddies of xy
			if ( xyBuds.setAndAny(BUDDIES[xy], bivs) ) {						// 235,859 / 284,612
				xym = maybes[xy];
				// toArrayN is the slowest part of XYWing: about two-thirds of
				// my time, but XYWing is still "fast enough" overall, and how
				// else? We use Idxs to calculate the indices of possible xz
				// and yz cells, and that requires "each"ing the bastards, and
				// calculating buddies any other way is slower overall. Sigh.
				// Juillerats non-Idx XyWing is actually faster! But this is
				// faster overall, coz someone has to load the biv cache.
				xzn = xyBuds.toArrayN(xzArray);
				// foreach xz in bivalue buds of xy
				// nb: post test coz first exists in order to get here
				xzi = 0;
				do {															//  991,515 / 997,163
					xz = xzArray[xzi];
					// if xy has one maybe other than xz.maybes, ergo y
					if ( VSIZE[xym & ~maybes[xz]] == 1 ) {						//  327,834 / 173,041
						xzm = maybes[xz];
						xyz = xym | xzm;
						x = xym & xzm;
						// foreach yz in bivalue buds of xy (again)
						// nb: post test coz first exists in order to get here
						yzi = 0;
						do {													//1,617,896 / 776,560
							yz = xzArray[yzi];
							yzm = maybes[yz];
							// if these 3 cells share XY=0/XYZ=1 common values
							if ( VSIZE[x & yzm] == intersectionSize
							  // and have 3 values in total
							  && VSIZE[xyz | yzm] == 3 // union
							  // and xz and yz have 1 common value
							  // (thus xz and yz are distinct cells)
							  && VSIZE[xzm & yzm] == 1							//   48,941 / 61,282
							) {
								// its an XY/Z-Wing, but any victims?
								// get the z-value
								z = VFIRST[xzm & yzm];
								// victims: zs seeing xz and yz
								vc0 = (t0=idxs[z]).m0 & BUDS_M0[xz] & BUDS_M0[yz];
								vc1 = t0.m1 & BUDS_M1[xz] & BUDS_M1[yz];
								if ( isXYZ ) { // and xy in XYZ_Wing
									vc0 &= BUDS_M0[xy];
									vc1 &= BUDS_M1[xy];
								} else { // just remove xy in XY_Wing
									vc0 &= ~(t0=CELL_IDXS[xy]).m0;
									vc1 &= ~t0.m1;
								}
								if ( (vc0|vc1) > 0L ) {
									// FOUND an XY/Z_Wing! with eliminations!
									result = true;
									if ( accu.add(new XYWingHint(grid, this
									, new Pots(vc0,vc1, maybes, VSHFT[z], DUMMY) // reds
									, isXYZ, z, cells[xy], cells[xz], cells[yz])) )
										return result;
								}
							}
						} while (++yzi < xzn);
					}
				} while (++xzi < xzn);
			}
		}
		return result;
	}

}
