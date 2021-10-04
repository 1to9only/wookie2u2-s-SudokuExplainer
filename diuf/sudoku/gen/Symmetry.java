/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

/** 
 * Enumeration of Sudoku grid's symmetries. A Symmetry supplies a list of points
 * (relative to x,y) from which we remove cell values from a solution to make a
 * puzzle.
*/
public enum Symmetry {
	None {
		@Override
		public int size() {
			return 1;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			};
		}
		@Override
		public String getDescription() {
			return "No symmetry";
		}
		@Override
		public String toString() {
			return "0: None (0)";
		}
	}
	, Vertical {
		@Override
		public int size() {
			return 2;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(8 - x, y)
			};
		}
		@Override
		public String getDescription() {
			return "Mirror symmetry around the vertical axis";
		}
		@Override
		public String toString() {
			return "1: Vertical (2)";
		}
	}
	, Horizontal {
		@Override
		public int size() {
			return 2;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(x, 8 - y)
			};
		}
		@Override
		public String getDescription() {
			return "Mirror symmetry around the horizontal axis";
		}
		@Override
		public String toString() {
			return "2: Horizontal (2)";
		}
	}
	, Diagonal {
		@Override
		public int size() {
			return 2;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(8 - y, 8 - x)
			};
		}
		@Override
		public String getDescription() {
			return "Mirror symmetry around the raising diagonal";
		}
		@Override
		public String toString() {
			return "3: Diagonal (2)";
		}
	}
	, Antidiagonal {
		@Override
		public int size() {
			return 2;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(y, x)
			};
		}
		@Override
		public String getDescription() {
			return "Mirror symmetry around the falling diagonal";
		}
		@Override
		public String toString() {
			return "4: Anti-diagonal (2)";
		}
	}
	, Rotate180 {
		@Override
		public int size() {
			return 2;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(8 - x, 8 - y)
			};
		}
		@Override
		public String getDescription() {
			return "Symmetric under a 180 degrees rotation (central symmetry)";
		}
		@Override
		public String toString() {
			return "5: 180 Rotational (2)";
		}
	}
	, Bidiagonal_4 {
		@Override
		public int size() {
			return 4;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(y, x)
			  , new Point(8 - y, 8 - x)
			  , new Point(8 - x, 8 - y)
			};
		}
		@Override
		public String getDescription() {
			return "Mirror symmetries around both diagonals";
		}
		@Override
		public String toString() {
			return "6: Bi-diagonal (4)";
		}
	}
	, Orthogonal_4 {
		@Override
		public int size() {
			return 4;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(8 - x, y)
			  , new Point(x, 8 - y)
			  , new Point(8 - x, 8 - y)
			};
		}
		@Override
		public String getDescription() {
			return "Mirror symmetries around the horizontal and vertical axes";
		}
		@Override
		public String toString() {
			return "7: Orthogonal (4)";
		}
	}
	, Rotate90_4 {
		@Override
		public int size() {
			return 4;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(8 - x, 8 - y)
			  , new Point(y, 8 - x)
			  , new Point(8 - y, x)
			};
		}
		@Override
		public String getDescription() {
			return "Symmetric under a 90 degrees rotation";
		}
		@Override
		public String toString() {
			return "8: 90 Rotational (4)";
		}
	}
	, Full_8 {
		@Override
		public int size() {
			return 4;
		}
		@Override
		public Point[] getPoints(int x, int y) {
			return new Point[] {
				new Point(x, y)
			  , new Point(8 - x, y)
			  , new Point(x, 8 - y)
			  , new Point(8 - x, 8 - y)
			  , new Point(y, x)
			  , new Point(8 - y, x)
			  , new Point(y, 8 - x)
			  , new Point(8 - y, 8 - x)
			};
		}
		@Override
		public String getDescription() {
			return "All symmetries (around the 8 axies and under a 90 degrees rotation)";
		}
		@Override
		public String toString() {
			return "9: Full (8)";
		}
	};

	/** Generating an IDKFA is too slow with any symmetry. */
	public static final Symmetry[] NONE = new Symmetry[] {
		  None
	};
	/** An array of all the 2 pointed Symmetries. */
	public static final Symmetry[] SMALLS = new Symmetry[] {
		None, Vertical, Horizontal, Diagonal, Antidiagonal, Rotate180
	};
	/** An array of the 4 and 8 pointed Symmetries. */
	public static final Symmetry[] BIGS = new Symmetry[] {
		Bidiagonal_4, Orthogonal_4, Rotate90_4, Full_8
	};
	/** An array of all Symmetries. */
	public static final Symmetry[] ALL = new Symmetry[] {
		  None, Vertical, Horizontal, Diagonal, Antidiagonal, Rotate180
		, Bidiagonal_4, Orthogonal_4, Rotate90_4, Full_8
	};

	public abstract int size();
	
	public abstract Point[] getPoints(int x, int y);

	public abstract String getDescription();
}
