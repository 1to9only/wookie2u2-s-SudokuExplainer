/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import static java.lang.Integer.parseInt;


/**
 * A "slot" is three cells that are the intersection of a box and a row/col.
 * Slots are used in Locking and EmptyRectangle. Its a hack, for speed.
 *
 * @author Keith Corlett 2021-12-03
 */
public class Slots {

	// row slots (remember they are upside-down)
	public static final int TOP_ROW = parseInt("000"
											 + "000"
											 + "111", 2); // 7=1+2+4
	public static final int MID_ROW = parseInt("000"
											 + "111"
											 + "000", 2); // 56=7<<3
	public static final int BOT_ROW = parseInt("111"
											 + "000"
											 + "000", 2); // 448=7<<6
	// col slots (remember that left-is-right)
	public static final int LEF_COL = parseInt("001"
											 + "001"
											 + "001", 2); // 73=1+8+64
	public static final int MID_COL = parseInt("010"
											 + "010"
											 + "010", 2); // 146=73<<1
	public static final int RIG_COL = parseInt("100"
											 + "100"
											 + "100", 2); // 292=73<<2

	public enum Slot {
		  a(TOP_ROW, 0, 1)
		, b(MID_ROW, 3, 1)
		, c(BOT_ROW, 6, 1)
		, d(LEF_COL, 0, 2)
		, e(MID_COL, 1, 2)
		, f(RIG_COL, 2, 2)
		;
		public final int bits; // bitset of places for this slot
		private final int ci; // index in box.cells
		private final int rti; // index in cell.regions
		private Slot(int bits, int ci, int rti) {
			this.bits = bits;
			this.ci = ci;
			this.rti = rti;
		}
		public ARegion line(Box box) {
			return box.cells[ci].regions[rti];
		}
	}

	/**
	 * For {@link diuf.sudoku.solver.hinters.lock.LockingSpeedMode#pointFrom}:
	 * SLOTS is an array of Slots, whose line method returns the ARegion (a Row
	 * or Col) containing the three cells that are the intersection of the given
	 * box and it is intersecting rows (a,b,c) and cols (d,e,f);
	 * ordered top-to-bottom and left-to-right.
	 */
	public static final Slot[] SLOTS = {
		Slot.a, Slot.b, Slot.c, Slot.d, Slot.e, Slot.f
	};

	/**
	 * Masks used to determine if all vs in a box are in the same row.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally invert my rows for it to make sense.
	 */
	public static final int[] ROW_SLOTS = {TOP_ROW, MID_ROW, BOT_ROW};

	/**
	 * Masks used to determine if all vs in a box are in the same col.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally mirror my bits for it to make sense.
	 */
	public static final int[] COL_SLOTS = {LEF_COL, MID_COL, RIG_COL};

}
