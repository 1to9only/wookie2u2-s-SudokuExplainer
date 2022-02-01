/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.Idx;
import diuf.sudoku.IdxL;
import diuf.sudoku.utils.Mutator;
import java.lang.reflect.Method;

/**
 * Print out Idx @Mutator's that aren't overridden by IdxL.
 *
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
