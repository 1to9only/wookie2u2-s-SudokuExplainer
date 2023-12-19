//not used 2020-10-23 but I am loath to remove any of this crap
///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2023 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//import diuf.sudoku.Grid.Cell;
//
//
///**
// * Static helper methods for matrixes, ie two dimensional arrays.
// * I see Cell snuck in here. Sigh.
// * @author Keith Corlett 2013
// */
//public final class MyMatrixes {
//
//	public static void clear(boolean[][] matrix) {
//		for ( int y=0, Y=matrix.length; y<Y; ++y ) // foreach row
//			for ( int x=0, X=matrix[y].length; x<X; ++x ) // foreach column
//				matrix[y][x] = false;
//	}
//
//	public static void clear(Object[][] matrix) {
//		for ( int y=0, Y=matrix.length; y<Y; ++y ) // foreach row
//			for ( int x=0, X=matrix[y].length; x<X; ++x ) // foreach column
//				matrix[y][x] = null;
//	}
//
//	public static boolean[][] of(Cell[] cells) {
//		boolean[][] matrix = new boolean[9][9];
//		for ( Cell c : cells ) {
//			if ( c == null )
//				break; // it is a null terminated list
//			matrix[c.y][c.x] = true;
//		}
//		return matrix;
//	}
//
//turns out it is faster to do with with an int index instead of a 9*9 matrix
//	public static void of(boolean[][] matrix, Cell[] cells) {
//		clear(matrix);
//		for ( Cell c : cells )
//			matrix[c.y][c.x] = true;
//	}
//
//	public static boolean[][] of(Iterable<Cell> cells) {
//		boolean[][] matrix = new boolean[9][9];
//		for ( Cell c : cells )
//			matrix[c.y][c.x] = true;
//		return matrix;
//	}
//
//}
