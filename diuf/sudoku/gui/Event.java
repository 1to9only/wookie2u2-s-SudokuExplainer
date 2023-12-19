/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

/**
 * Selected java.awt.Event masks.
 * <p>
 * I just copy-pasted the masks <b>THAT I USE</b> from java.awt.Event because
 * java.awt.Event is deprecated as of JDK9, along with the whole event tree,
 * but I have no interest replacing it; so I must suppress the warning. I use
 * lint, and treat warnings as errors. Event permanently averts this warning.
 * If/when Events are removed from the JAPI (dont hold your breath) then I will
 * have to work-out how to use there replacement. This will do for now.
 *
 * @author Keith Corlett 2022-01-08
 */
final class Event {

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
