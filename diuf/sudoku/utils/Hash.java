/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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

	/** The bit that differentiates an ON assumption from an OFF. Set is ON. */
	public static final int ON_BIT = 1<<11; // 2048

	/** The shift for values, currently 7 */
	public static final int[] SHFT_V = new int[] {
		0, 1<<7, 2<<7, 3<<7, 4<<7, 5<<7, 6<<7, 7<<7, 8<<7, 9<<7
	};

	// never used
	private Hash() { }

}
