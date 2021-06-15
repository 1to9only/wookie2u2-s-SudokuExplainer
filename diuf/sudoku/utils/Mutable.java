///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2021 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//
///**
// * A mutable generically-typed object.
// * @param <T> the type of the value
// */
//public class Mutable<T> {
//
//	public T value;
//
//	public Mutable() {
//	}
//
//	public Mutable(T value) {
//		this.value = value;
//	}
//
//	private boolean eq(Object o1, Object o2) {
//		return o1==null ? o2==null : o1.equals(o2);
//	}
//
//	@Override
//	public boolean equals(Object o) {
//		if (o instanceof Mutable) {
//			Mutable<?> other = (Mutable<?>)o;
//			return eq(this.value, other.value);
//		}
//		return false;
//	}
//
//	@Override
//	public int hashCode() {
//		return value==null ? 0 : value.hashCode();
//	}
//
//	// not used in anger
//	@Override
//	public String toString() {
//		return value==null ? "" : value.toString();
//	}
//
//}
