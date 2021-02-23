/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;


/**
 * The IInterruptMonitor interface just allows an interruptible methods called
 * methods to monitor isInterrupted() in order to exit-early when the user
 * interrupts the process. I wish there was a "clean" way of doing this.
 * @author Keith Corlett 2019 OCT
 */
public interface IInterruptMonitor {
	public boolean isInterrupted();
}
