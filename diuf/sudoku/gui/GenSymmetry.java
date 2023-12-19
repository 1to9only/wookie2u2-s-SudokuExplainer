/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import java.util.ArrayList;

/**
 * Enumeration of Sudoku grids symmetries, supplying a list of points (relative
 * to x,y) from which we remove cell values from a solution to make a puzzle.
 */
enum GenSymmetry {

	None {
		@Override
		int size() {
			return 1;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			};
		}
		@Override
		String getDescription() {
			return "No symmetry";
		}
		@Override
		public String toString() {
			return "0: None (0)";
		}
	}

	, Vertical {
		@Override
		int size() {
			return 2;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(8 - x, y)
			};
		}
		@Override
		String getDescription() {
			return "Mirror symmetry around the vertical axis";
		}
		@Override
		public String toString() {
			return "1: Vertical (2)";
		}
	}

	, Horizontal {
		@Override
		int size() {
			return 2;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(x, 8 - y)
			};
		}
		@Override
		String getDescription() {
			return "Mirror symmetry around the horizontal axis";
		}
		@Override
		public String toString() {
			return "2: Horizontal (2)";
		}
	}

	, Diagonal {
		@Override
		int size() {
			return 2;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(8 - y, 8 - x)
			};
		}
		@Override
		String getDescription() {
			return "Mirror symmetry around the raising diagonal";
		}
		@Override
		public String toString() {
			return "3: Diagonal (2)";
		}
	}

	, Antidiagonal {
		@Override
		int size() {
			return 2;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(y, x)
			};
		}
		@Override
		String getDescription() {
			return "Mirror symmetry around the falling diagonal";
		}
		@Override
		public String toString() {
			return "4: Anti-diagonal (2)";
		}
	}

	, Rotate180 {
		@Override
		int size() {
			return 2;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(8 - x, 8 - y)
			};
		}
		@Override
		String getDescription() {
			return "Symmetric under a 180 degrees rotation (central symmetry)";
		}
		@Override
		public String toString() {
			return "5: 180 Rotational (2)";
		}
	}

	, Bidiagonal_4 {
		@Override
		int size() {
			return 4;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(y, x)
			  , new GenPoint(8 - y, 8 - x)
			  , new GenPoint(8 - x, 8 - y)
			};
		}
		@Override
		String getDescription() {
			return "Mirror symmetries around both diagonals";
		}
		@Override
		public String toString() {
			return "6: Bi-diagonal (4)";
		}
	}

	, Orthogonal_4 {
		@Override
		int size() {
			return 4;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(8 - x, y)
			  , new GenPoint(x, 8 - y)
			  , new GenPoint(8 - x, 8 - y)
			};
		}
		@Override
		String getDescription() {
			return "Mirror symmetries around the horizontal and vertical axes";
		}
		@Override
		public String toString() {
			return "7: Orthogonal (4)";
		}
	}

	, Rotate90_4 {
		@Override
		int size() {
			return 4;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(8 - x, 8 - y)
			  , new GenPoint(y, 8 - x)
			  , new GenPoint(8 - y, x)
			};
		}
		@Override
		String getDescription() {
			return "Symmetric under a 90 degrees rotation";
		}
		@Override
		public String toString() {
			return "8: 90 Rotational (4)";
		}
	}

	, Full_8 {
		@Override
		int size() {
			return 4;
		}
		@Override
		GenPoint[] getPoints(final int x, final int y) {
			return new GenPoint[] {
				new GenPoint(x, y)
			  , new GenPoint(8 - x, y)
			  , new GenPoint(x, 8 - y)
			  , new GenPoint(8 - x, 8 - y)
			  , new GenPoint(y, x)
			  , new GenPoint(8 - y, x)
			  , new GenPoint(y, 8 - x)
			  , new GenPoint(8 - y, 8 - x)
			};
		}
		@Override
		String getDescription() {
			return "All symmetries (around the 8 axies and under a 90 degrees rotation)";
		}
		@Override
		public String toString() {
			return "9: Full (8)";
		}
	};

	/**
	 * returns a new java.util.ArrayList of the given array.
	 * <p>
	 * This method exists because java.util.ArrayList has no addAll(E[] a),
	 * hence no constructor that takes an array. Which I find annoying.
	 *
	 * @param a the array to read into a new java.util.ArrayList
	 * @return a new java.util.ArrayList
	 */
	public static final ArrayList<GenSymmetry> gsList(final GenSymmetry[] a) {
		return new ArrayList<>(java.util.Arrays.asList(a));
	}

	/**
	 * Generating an IDKFA is impeded by ANY symmetry, so it always uses NONE.
	 * <p>
	 * <b>RANT<b>: I choose java.util.ArrayList (impl) as my reference type,
	 * coz {@link java.util.LinkedList#get} is an O(n/2) implementation of the
	 * normative reference type {@link java.util.List}, and I call get "often",
	 * hence I prefer {@link java.util.ArrayList#get}, which is O(1) atleast,
	 * even if it does involve excessive and unnecessary stack-work.
	 * <p>
	 * I wish Oracle (TM) could employ <u>and retain</u> competent (performance
	 * aware) programmers to enfastonate (rewrite) Collections. I guess maybe
	 * an eighth of Collections CPU time is waste, and it keeps getting worse
	 * in each release. We've descended from tail-call-awareness to manky crap.
	 * Oracles architects (qualified interior decorators) stay stum. To whit
	 * they retort "You do better!" to whit my only response is "Don't think I
	 * can mate. Carry on". Sigh. I'm only smart. Not a genius. And I Know It
	 * Too. Stuff like this is constantly frustrating. Makes me narky. I reckon
	 * LinkedList should be called ArrayFactory. Faster that way. Ignore me.
	 * <p>
	 * Note that the "ArrayList" returned by {@link java.util.Arrays#asList} is
	 * NOT a {@link java.util.ArrayList}, as one might quite reasonably expect,
	 * and its implementation of contains is O(n) verses O(n/2) in LinkedList,
	 * which I regard as deficient. I want to say crap. Crap crap! That just
	 * about summarises it nicely. Nothing to see here. Move along. Narkyness
	 * managed. Mischief can go and get ____ed.
	 */
	static final ArrayList<GenSymmetry> NONE = gsList(new GenSymmetry[] {
		  None
	});
	/** An array of all the 2 pointed Symmetries. */
	static final ArrayList<GenSymmetry> SMALLS = gsList(new GenSymmetry[] {
		None, Vertical, Horizontal, Diagonal, Antidiagonal, Rotate180
	});
	/** An array of the 4 and 8 pointed Symmetries. */
	static final ArrayList<GenSymmetry> BIGS = gsList(new GenSymmetry[] {
		Bidiagonal_4, Orthogonal_4, Rotate90_4, Full_8
	});
	/** An array of all Symmetries. */
	static final ArrayList<GenSymmetry> ALL = gsList(new GenSymmetry[] {
		  None, Vertical, Horizontal, Diagonal, Antidiagonal, Rotate180
		, Bidiagonal_4, Orthogonal_4, Rotate90_4, Full_8
	});

	static ArrayList<GenSymmetry> alues() {
		return gsList(values());
	}

	abstract int size();

	abstract GenPoint[] getPoints(int x, int y);

	abstract String getDescription();

}
