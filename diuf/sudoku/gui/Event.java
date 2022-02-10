/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

/**
 * All I've done is copy-paste the masks that I use from java.awt.Event.
 * <p>
 * java.awt Event is deprecated as of JDK9, along with the whole event tree,
 * but having no internet connection I have no way to find out how to use the
 * replacement classes; so I just suppress the compiler warnings (I use lint
 * and treat all warnings as errors, except you can ignore warnings, I'm just
 * doing so "permanently"). If/When the old event classes are removed from the
 * JAPI (don't hold you breath) then I will have to find out how to use there
 * replacements; but this will do for now.
 *
 * @author Keith Corlett 2022-01-08
 */
public class Event {

	/* Modifier constants */

	/**
	 * This flag indicates that the Shift key was down when the event
	 * occurred.
	 */
	public static final int SHIFT_MASK          = 1 << 0;

	/**
	 * This flag indicates that the Control key was down when the event
	 * occurred.
	 */
	public static final int CTRL_MASK           = 1 << 1;

//	/**
//	 * This flag indicates that the Meta key was down when the event
//	 * occurred. For mouse events, this flag indicates that the right
//	 * button was pressed or released.
//	 */
//	public static final int META_MASK           = 1 << 2;

	/**
	 * This flag indicates that the Alt key was down when
	 * the event occurred. For mouse events, this flag indicates that the
	 * middle mouse button was pressed or released.
	 */
	public static final int ALT_MASK            = 1 << 3;

}
