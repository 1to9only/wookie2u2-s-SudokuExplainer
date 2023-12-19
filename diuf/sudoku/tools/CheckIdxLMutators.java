/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.Idx;
import diuf.sudoku.IdxL;
import diuf.sudoku.utils.Mutator;
import java.lang.reflect.Method;

/**
 * Print out each Idx @Mutator that is not overridden by IdxL.
 * <p>
 * I use @Mutator in Idx, to denote methods that need to be overridden by IdxL,
 * to check the lock. If EVERY mutator checks the lock then a locked IdxL cannot
 * be modified, but if just one override is missing then the whole IdxL class
 * is basically useless. Obviously it is still encumbent on the Idx programmer
 * to mark each mutator with the @Mutator annotation. This is NOT perfect, or
 * indeed any good at all really. Sigh.
 * <p>
 * So this tool prints each @Mutator that is not overridden. This requires Idx
 * authors to ensure that EVERY mutator is marked as a @Mutator, but its still
 * better than nothing, coz its easier to miss an override than an annotation.
 * Eventually we will miss an annotation, but I do not know how to determine
 * (using code) whether or not a method mutates its instance; so I eyeball the
 * bastards and annotate them.
 * <p>
 * So BEFORE running this tool go and double-check that every Idx-mutator is
 * annotated as a @Mutator, and if not then add the annotation; then run me,
 * and override any methods I print-out in the IdxL class. Not perfect, and
 * not even really usable. Sigh.
 *
 * @see diuf.sudoku.utils.Mutator
 * @author Keith Corlett 2021-12-26
 */
public class CheckIdxLMutators {

	public static void main(String[] args) {
		try {
			final Class<Mutator> mutator = Mutator.class;
			for ( Method method : Idx.class.getDeclaredMethods()) {
				if ( method.getAnnotation(mutator) != null ) {
					try {
						// no care about retval, just NoSuchMethodException
						IdxL.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
					} catch (NoSuchMethodException ex) {
						System.out.println(method);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
