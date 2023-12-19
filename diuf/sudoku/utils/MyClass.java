/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.MyStrings.substring;

/**
 * Static helper methods for Java classes. I just cannot believe that class
 * does not have a "nameOnly()" method. Mental!
 * @author Keith Corlett 2020 Mar 12
 */
public final class MyClass {

	/**
	 * Get the name only of from instance.getClass.getName().
	 * <pre>
	 * eg: diuf.sudoku.solver.hinters.align.Aligned5Exclusion_1C
	 *     ==> Aligned5Exclusion_1C
	 * or: Aligned5Exclusion_1C ==> Aligned5Exclusion_1C
	 * </pre>
	 *
	 * @param instance
	 * @return simple class name
	 */
	public static String nameOnly(Object instance) {
		String name = instance.getClass().getName();
		int i = name.lastIndexOf('.');
		return i<0 ? name : substring(name, i+1);
	}

	// Never used
	private MyClass() { }

}
