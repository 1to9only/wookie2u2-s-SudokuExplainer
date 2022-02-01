/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import static java.lang.Integer.parseInt;

/**
 * These "slots" are used in Locking and EmptyRectangle. A "slot" is three
 * cells that are the intersection of a box and a row/col.
 *
 * @author Keith Corlett 2021-12-03
 */
public class Slots {

	// row slots
	public static final int SLOT1 = parseInt("000"
										   + "000"
										   + "111", 2); // 7=1+2+4
	public static final int SLOT2 = parseInt("000"
										   + "111"
										   + "000", 2); // 56=7<<3
	public static final int SLOT3 = parseInt("111"
										   + "000"
										   + "000", 2); // 448=7<<6
	// col slots
	public static final int SLOT4 = parseInt("001"
										   + "001"
										   + "001", 2); // 73=1+8+64
	public static final int SLOT5 = parseInt("010"
										   + "010"
										   + "010", 2); // 146=73<<1
	public static final int SLOT6 = parseInt("100"
										   + "100"
										   + "100", 2); // 292=73<<2

	public enum Slot {
		a(SLOT1) {
			@Override
			public ARegion line(Box box) {
				return box.getGrid().rows[box.top];
			}
		}
		, b(SLOT2) {
			@Override
			public ARegion line(Box box) {
				return box.getGrid().rows[box.top + 1];
			}
		}
		, c(SLOT3) {
			@Override
			public ARegion line(Box box) {
				return box.getGrid().rows[box.top + 2];
			}
		}
		, d(SLOT4) {
			@Override
			public ARegion line(Box box) {
				return box.getGrid().cols[box.left];
			}
		}
		, e(SLOT5) {
			@Override
			public ARegion line(Box box) {
				return box.getGrid().cols[box.left + 1];
			}
		}
		, f(SLOT6) {
			@Override
			public ARegion line(Box box) {
				return box.getGrid().cols[box.left + 2];
			}
		}
		;
		public final int bits;
		private Slot(int bits) {
			this.bits = bits;
		}
		public abstract ARegion line(Box box);
	}

	public static final Slot[] SLOTS = {
		Slot.a, Slot.b, Slot.c, Slot.d, Slot.e, Slot.f
	};

}
