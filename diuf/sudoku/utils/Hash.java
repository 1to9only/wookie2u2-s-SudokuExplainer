/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Hash exposes the constants used to calculate Ass hashCodes through-out.
 * <p>
 * The requirement for consistency demands that we keep all this s__t in one
 * place, and everyone comes here to get the facts from the horses mouth.
 * @author Keith Corlett
 */
public final class Hash {

	/** Constant to speed-up calculating Ass.hashCode.
	 * <p>
	 * NOTE: 4096 is hardcoded everywhere and I'm leaving it because it's
	 * shorter and it is still searchable, so long as 4096 doesn't crop-up
	 * anywhere else. Sigh.
	 */
	public static final int LSH12 = 1<<12; // 4096

	/** 0-based left-shifted 8 places. */
	public static final int[] LSH8 = new int[] {
		0, 1<<8, 2<<8, 3<<8, 4<<8, 5<<8, 6<<8, 7<<8, 8<<8, 9<<8
	};

	/** 0-based left-shifted 4 places. */
	public static final int[] LSH4 = new int[] {
		0, 1<<4, 2<<4, 3<<4, 4<<4, 5<<4, 6<<4, 7<<4, 8<<4, 9<<4
	};

	// never used
	private Hash() { }

}
