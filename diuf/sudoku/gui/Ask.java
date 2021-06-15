/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import java.awt.Toolkit;
import javax.swing.JOptionPane;

public final class Ask {

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	public static int forInt(String question, int min, int max) {
		assert min <= max;
		question = question.replaceAll("\\n", NL);
		String response = JOptionPane.showInputDialog(question
			, "Please enter an integer between "+min+" and "+max);
		final String retryMessage =
			"An integer between "+min+" and "+max+" is required."+NL+question;
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

	public static String forString(String question, String defaultText) {
		question = question.replaceAll("\\n", NL);
		return JOptionPane.showInputDialog(question, defaultText);
	}

}
