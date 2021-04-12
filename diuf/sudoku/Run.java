/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * I need a way to differentiate running in the GUI
 *                          from running in the LogicalSolverTester
 * and this is a bit s__t (TM) but it'll do.
 * @author Keith Corlett 2020 Mar 22
 */
public final class Run {

	public static Type setRunType(Type type) {
		Type prev = Run.type;
		Run.type = type;
		return prev;
	}

	public enum Type {
		  TestCase		// in a JUnit test-case
		, GUI			// Normal GUI
		, Generator		// The generator in the GUI
		, Batch			// LogicalSolverTester
	};
	/**
	 * The LogicalSolverTester sets this value to true. It's public static so
	 * that everywhere we need to know can just find out.
	 * <p>
	 * Note that there's existing hacks in the GUI (SudokuExplainer) to workout
	 * whether or not we're running in the GUI, as apposed to one of the
	 * test-cases or the LogicalSolverTester.
	 * <p>
	 * A Better solution might be to have a Run.Type enum {
	 * TestCase, GUI, LogicalSolverTester
	 * }
	 * which defaults to TestCase and is set by SudokuExplainer and by the
	 * LogicalSolverTester. Sigh.
	 */
	public static Type type = Type.TestCase;

// templates are useless because everything passes!
//	/** The instance of templates referenced by everyone. This attribute is in
//	 * the Run class so that Templates (Trebors Tables) are loaded from disk
//	 * ONCE when the application starts-up. */
//	public static final Templates templates = Templates.THE;

	private Run() { } // Never used!

}
