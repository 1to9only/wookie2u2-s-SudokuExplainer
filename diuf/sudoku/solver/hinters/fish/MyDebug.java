/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.gui.*;

/**
 * MyDebug is a Debug static helper class for this package, so each package
 * may have its own Debug class, to avert static import collisions, whilst
 * supporting each packages distinctive quirks. This allows me to turn all
 * DEBUG messages in a package on/off in one place. So, just to be crystal
 * clear, this class exists in package to allow any/all DEBUG statements in
 * this package to be turned on/off in one place. Do NOT fuch with it!
 * <p>
 * FYI, check the static import statement. The constant and methods are all
 * imported in one, coz deyz mononominative. Weird huh?! Weez likes it!
 *
 * @author Keith Corlett 2023-06-19
 */
class MyDebug {

	static final boolean DEBUG = true;
	static void DEBUG(){
		if(DEBUG)System.out.println();
	}
	static void DEBUG(final String msg){
		if(DEBUG)System.out.println(msg);
	}
	static void DEBUG(final String frmt, final Object... args){
		if(DEBUG)System.out.format(frmt, args);
	}

	private MyDebug() { }

}
