/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
 * <p>
 * So {@link diuf.sudoku.tools.CheckIdxLMutators} prints @Mutator's that aren't
 * overridden. This requires Idx authors to ensure EVERY mutator is marked as a
 * "@Mutator", but it's still better than nothing, because it's a lot easier to
 * miss an override than an annotation. We're eventually going to miss an
 * annotation, but I don't know how to determine (using code) whether or not a
 * method mutates it's containing instance; so I just eyeball the bastards and
 * annotate them.
 * <p>
 * So BEFORE running this tool go and double-check that every Idx-mutator is
 * annotated as a @Mutator, and if not then add the annotation; then run me,
 * and override any methods I print-out in the IdxL class. Not perfect, but
 * simples.
 *
 * @author Keith Corlett 2021-12-26
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mutator {

}
