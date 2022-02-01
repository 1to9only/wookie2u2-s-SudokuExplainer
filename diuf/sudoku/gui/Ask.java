/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.NL;
import java.awt.Toolkit;
import javax.swing.JOptionPane;

/**
 * Ask enables a non-GUI-class to Ask the user a question. The downside is that
 * my message-box's are unparented by the main-frame, and so are in-effect
 * independent applications. sigh. If you CAN use the implementation of IAsker
 * in {@link diuf.sudoku.gui.SudokuFrame} then please do so, because it behaves
 * exactly as the user has come to expect, where-as this one is "a bit weird",
 * BUT Ask allows us to ask-the-user from a place in library code that doesn't
 * know about the user-interface. So, It'd be better if I didn't exist, but I
 * do, because there's an actual need for me. I'm only ever used as a plan-B.
 *
 * @author Keith Corlett
 */
public final class Ask {

	public static int forInt(String question, int min, int max) {
		assert min <= max;
		question = question.replaceAll("\\n", NL);
		String response = JOptionPane.showInputDialog(question
			, "Please enter an integer between "+min+AND+max);
		final String retryMessage =
			"An integer between "+min+AND+max+" is required."+NL+question;
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
					Toolkit.getDefaultToolkit().beep();
				}
			}
			response = JOptionPane.showInputDialog(retryMessage, "Try again.");
		}
	}

//not_used
//	public static String forString(String question, String defaultText) {
//		question = question.replaceAll("\\n", NL);
//		return JOptionPane.showInputDialog(question, defaultText);
//	}

}
