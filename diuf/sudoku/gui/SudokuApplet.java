/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import java.applet.Applet;
import javax.swing.SwingUtilities;


/** Minimal applet support for the sudoku explainer. */
public final class SudokuApplet extends Applet {
	private static final long serialVersionUID = -1770658360372460892L;
	@Override
	public void init() {
		super.init();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Thread() {
					@Override
					public void run() {
						// IE gets the focus just after the applet has started,
						// so our main window is pushed back. This small delay
						// is a hack that solves this problem.
						try{Thread.sleep(500);}catch(InterruptedException eaten){}
						SudokuExplainer.main(null);
					}
				}.start();
			}
		});
	}
}