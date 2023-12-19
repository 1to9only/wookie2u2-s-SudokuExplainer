/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.beep;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.NL;
import javax.swing.JOptionPane;

/**
 * Ask enables a non-GUI-class to Ask the user a question. The downside is that
 * my message-boxs are unparented by the main-frame, and so are in-effect
 * independent applications. sigh. If you CAN use the implementation of IAsker
 * in {@link diuf.sudoku.gui.SudokuFrame} then please do so, because it behaves
 * exactly as the user has come to expect, where-as this one is "a bit weird",
 * BUT Ask allows us to ask-the-user from a place in library code that does not
 * know about the user-interface. So, It would be better if I did not exist,
 * but I do, coz of an actual need for me. I am only ever used as a plan-B.
 *
 * @author Keith Corlett
 */
final class Ask {

	static int forInt(final String question, final int min, final int max) {
		assert min <= max;
		final String q = question.replaceAll("\\n", NL);
		String response = JOptionPane.showInputDialog(q, "Please enter an integer between "+min+AND+max);
		final String retryMessage = "An integer between "+min+AND+max+" is required."+NL+q;
		while ( true ) {
			if ( response!=null && response.length()>0 ) {
				int value;
				try {
					value = Integer.parseInt(response);
					if ( value >= min && value <= max )
						return value;
				} catch (NumberFormatException ex) {
//Irrelevant so eaten
//					StdErr.whinge(ex);
					beep();
				}
			}
			response = JOptionPane.showInputDialog(retryMessage, "Try again.");
		}
	}

}
