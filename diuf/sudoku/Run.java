/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * Run holds some key facts about the application.
 * <p>
 * Basically, I need a way to differentiate the GUI from LogicalSolverTester.
 * This might be a bit s__t, but it will do.
 *
 * @author Keith Corlett 2020 Mar 22
 */
public final class Run {

	/**
	 * Was the -ea (-enableassertions) switch given to my JAVA.EXE.
	 */
	public static final boolean ASSERTS_ENABLED;
	static {
		boolean ae;
		try {
			assert false : "meh!";
			ae = false;
		} catch (AssertionError eaten) {
			ae = true;
		}
		ASSERTS_ENABLED = ae;
	}

	/**
	 * System.nanoTime() between hints in GUI
	 */
	public static long time;

	public enum Type {
		  TestCase		// in a JUnit test-case
		, GUI			// Normal GUI
		, Generator		// The generator in the GUI
		, Batch			// LogicalSolverTester
	};

	/**
	 * <pre>
	 * The LogicalSolverTester sets Run.Type.Batch.
	 * The SudokuExplainer sets Run.Type.GUI.
	 * The Generator temporarily sets Run.Type.Generator.
	 * The test-cases leave the default Run.type.TestCase.
	 * So anywhere we need to know, we can find out which context we are in.
	 * </pre>
	 */
	private static Type type = Type.TestCase;

	public static boolean isTestCase() {
		return type == Type.TestCase;
	}

	public static boolean isGui() {
		return type == Type.GUI;
	}

	public static boolean isGenerator() {
		return type == Type.Generator;
	}

	public static boolean isBatch() {
		return type == Type.Batch;
	}

	public static Type setRunType(Type type) {
		Type old = Run.type;
		Run.type = type;
		return old;
	}

	// make the generator pleaseStopNow
	private static volatile boolean isHinterupted = false;

	/**
	 * Set true when {@link diuf.sudoku.gen.Generator#generate} is stopped
	 * by the user. Do NOT forget to reset before the next generate.
	 *
	 * @param pleaseStopNow
	 */
	public static void setHinterrupt(boolean pleaseStopNow) {
		Run.isHinterupted = pleaseStopNow;
	}
	/**
	 * @return Has {@link diuf.sudoku.gen.Generator#generate} been stopped by
	 * the user.
	 */
	public static boolean isHinterrupted() {
		return isHinterupted;
	}

// templates are useless because everything passes!
//	/** The instance of templates referenced by everyone. This attribute is in
//	 * the Run class so that Templates (Trebors Tables) are loaded from disk
//	 * ONCE when the application starts-up. */
//	public static final Templates templates = Templates.THE;

	private Run() { } // Never used!

}
