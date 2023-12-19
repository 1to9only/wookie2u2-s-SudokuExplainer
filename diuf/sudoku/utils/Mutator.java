/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Mutator is a method that mutates (changes) the contents of its class.
 * <p>
 * I use this annotation in Idx, to denote methods that need to be overridden
 * by IdxL, to check the lock. If EVERY mutator checks the lock then a locked
 * IdxL instance cannot be modified, but if just one override is missing then
 * the whole IdxL class is useless.
 *
 * @see diuf.sudoku.Idx
 * @see diuf.sudoku.IdxL
 * @see diuf.sudoku.tools.CheckIdxLMutators
 * @author Keith Corlett 2021-12-26
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mutator {

}
