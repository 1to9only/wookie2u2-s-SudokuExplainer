/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;


/**
 * Just playing really, with CSV-ifying a Collection of {@code E}. I need an
 * interface to separate the "turn this {@code E} into a String" step from the 
 * CSV-ification code; so that I can just drop a {@code Collection<E>} and a
 * {@code Formatter<E>} into a method, and get back nice clean CSV; so the
 * utilities Frmt class can format an {@code E}, without even knowing what a
 * bloody {@code E} is.
 * @author Keith Corlett 2019 OCT
 * @param <E> The Element type of the collection to be formatted.
 */
public interface IFormatter<E> {
	String format(E e);
}
