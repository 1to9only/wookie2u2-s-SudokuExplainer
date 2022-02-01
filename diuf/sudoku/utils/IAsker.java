/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/** Interface for a GUI-component that can question the user. */
public interface IAsker {
	/**
	 * Ask the user a Yes/No question and wait for the answer.
	 * @param question the question to ask
	 * @param title the title of the message box (if any)
	 * @return whether the "yes" option was selected by the user
	 */
	public boolean ask(String question, String title);
	public boolean ask(String question);
	public String askForString(String question, String title);
	public String askForString(String question, String title, String defualt);
	public int askForInt(String question, int min, int max);
	
	public void carp(String msg, String title);
	public void carp(Exception ex, String title);
}
