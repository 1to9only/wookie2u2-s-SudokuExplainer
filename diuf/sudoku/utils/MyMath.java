///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2020 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
///**
// * Static helper methods for maths ____.
// * @author Keith Corlett 2019 NOV
// */
//public final class MyMath {
//
//	public static int minimum(int... a) {
//		int min = Integer.MAX_VALUE;
//		for ( int i : a )
//			if ( i < min )
//				min = i;
//		return min;
//	}
//
//	public static int indexOfMaximum(int... a) {
//		int result = -1; // index of the first occurence of the largest value in a
//		int max = Integer.MIN_VALUE;
//		for ( int i=0,e,n=a.length; i<n; ++i )
//			if ( (e=a[i]) > max ) {
//				max = e;
//				result = i;
//			}
//		return result;
//	}
//	
//	// remove the element at indexToRemove from the array a
//	public static int remove(int[] a, int indexToRemove) {
//		final int m = a.length - 1;
//		for ( int i=indexToRemove; i<m; ++i )
//			a[i] = a[i + 1];
//		return m;
//	}
//	
//	// never used
//	private MyMath(){}
//
//}
