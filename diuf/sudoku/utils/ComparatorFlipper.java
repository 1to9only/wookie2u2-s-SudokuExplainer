///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2021 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//import java.util.Comparator;
//
///**
// * ComparatorFlipper's compare method multiplies c.compare by -1,<br>
// * ie reverses of the order of the given comparator,<br>
// * ie DESCENDING order, presuming that your comparator is in ascending order.
// * <p>
// * This convenience comes at the cost of negating EVERY compare, so if you're
// * comparing large (say size > 10k) collections it'll be faster to flip your
// * comparator "natively" by reversing the arguements: a,b -> b,a.
// * <p>
// * This "trick" class just lets us reverse ANY Comparator, for when we need
// * not know what that Comparator is beforehand.
// * 
// * @author Keith Corlett 2020 Jan
// */
//public class ComparatorFlipper<T> implements Comparator<T> {
//	private final Comparator<T> c;
//	public ComparatorFlipper(Comparator<T> c) {
//		this.c = c;
//	}
//	@Override
//	public int compare(T a, T b) {
//		return -c.compare(a, b);
//	}
//}
