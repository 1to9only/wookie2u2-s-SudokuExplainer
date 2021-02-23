/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Static helper methods for Java classes. I just can't believe that class
 * doesn't have a "nameOnly()" method. Mental!
 * @author Keith Corlett 2020 Mar 12
 */
public final class MyClass {

	public static String nameOnly(Object ane) {
		// eg: diuf.sudoku.solver.hinters.align.Aligned5Exclusion_1C
		//     ==> Aligned5Exclusion_1C
		// or: Aligned5Exclusion_1C ==> Aligned5Exclusion_1C
		String name = ane.getClass().getName();
		int dot = name.lastIndexOf('.');
		return dot>0 ? name.substring(dot+1) : name;
	}
	
	// Never used
	private MyClass() { }

}
