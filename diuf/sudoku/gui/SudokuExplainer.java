/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Settings.*;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.GridFactory;
import diuf.sudoku.Pots;
import diuf.sudoku.PuzzleID;
import diuf.sudoku.Run;
import diuf.sudoku.Settings;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.GrabBag;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.checks.SolutionHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.align.LinkedMatrixCellSet;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.solver.accu.AggregatedHint;
import diuf.sudoku.solver.checks.SolvedHint;
import diuf.sudoku.utils.IAsker;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyInteger;
import diuf.sudoku.utils.MyStrings;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 * SudokuExplainer is the "engine" underneath the GUI SudokuFrame. Most of the
 * SudokuFrame actions delegate to this "engine", which then calls appropriate
 * methods of "action" classes, especially the LogicalSolver, which gets AHints
 * from the various IHinters. The "engine" only handles hintsTree filter plus a
 * few trivial tasks. All non-trivial tasks are delegated to other classes. So,
 * I am the business delegate, and my super-power is knowing a bit (not a lot)
 * about the GUI, and knowing all of the "action" classes, so that I can bring
 * it all together. In short: I am the glue.
 * <p>
 * This design keeps the GUI focused on GUI-stuff and the actions blissfully
 * unaware that there even IS a GUI. I'm the only part that knows about the GUI
 * and the actions, and the other two remain as ignorant as possible of each
 * other. Clear?
 * <p>
 * There's a few cases, such as asking the user (IAsker), interrupting a long
 * running task (IInterruptMonitor), and reporting run statistics (IReporter)
 * where there really is no choice but for the actions to to be aware of
 * aspects of the GUI; but these "incursions" are always limited to just the
 * minimal aspect using an interface. So every-time the GUI sticks it's bits in
 * an action it does-so via an interface. Condoms are a good idea! Clear?
 * <p>
 * So, obviously, anytime the GUI needs to know about an action it delegates
 * the details to me, by calling my "generic" (undetailed) method.
 * <p>
 * Anytime an action needs to know the GUI it does so through an interface,
 * like IAsker or IErrorReporter or similar; to limit the extent of the action
 * classes knowledge of the GUI. Ideally actions remain blissfully unaware of
 * the existence of any GUI; which isn't always practical, so we use interfaces
 * to limit such exposure.
 *
 * @see diuf.sudoku.gui.SudokuFrame
 * @see diuf.sudoku.solver.LogicalSolver
 */
final class SudokuExplainer implements Closeable {

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	/**
	 * This switch is for a BUG in the application of multiple user-selected
	 * hints. A group of hints that are all individually valid send the damn
	 * grid invalid when applied as a group; and I have, as yet, been unable
	 * to work-out what the hell I've done wrong, like orange juice in my
	 * redPots, or some other stupid bloody thing! Sigh.
	 */
	private static final boolean GOT_DODGY_HINTS = true;

	private Grid grid;					// The Sudoku grid
	public LogicalSolver solver;		// The Sudoku solver
	final SudokuFrame frame;			// The main GUI frame
	private final IAsker asker;			// an aspect of the main GUI frame
	private final SudokuGridPanel gridPanel;	// The Sudoku grid panel

	// The raw un-filtered hints, and the cooked filtered version.
	private List<AHint> unfilteredHints = null; // all hints (unfiltered)
	private List<AHint> filteredHints = null; // filtered hints (by effects)
	// Cells which have already been filtered-out.
	private final LinkedMatrixCellSet filteredCells = new LinkedMatrixCellSet();
	// The potential cell values which have already been removed (filtered-out).
	private Pots filteredPots = new Pots();

	 // The list of currently selected hint/s.
	private final LinkedList<AHint> selectedHints = new LinkedList<>();

	/** The recently accessed (opened or saved) files list - a self-
	 * persistent collection, so long as you close me in dispose. */
	RecentFiles recentFiles;

	// For skip bad setValue request unless repeated
	private Cell prevCell = null;
	private int prevValue = -1;

	//	god mode
	public boolean degreleaseMode = false;

	// Note that Log.out is set before we invoke the constructor.
	private SudokuExplainer() {
		grid = new Grid();
		solver = LogicalSolverFactory.get();

		GrabBag.logicalSolver = solver;
		asker = frame = new SudokuFrame(grid, this);
		gridPanel = frame.gridPanel;

		repaintHintsTree();
		frame.pack();
		frame.setBounds(getBounds(frame));
		frame.setVisible(true);

		this.recentFiles = RecentFiles.getInstance();
		PuzzleID pid = recentFiles.mostRecent();
		PuzzleID loaded = loadFile(pid);
		if ( loaded != null ) {
			frame.defaultDirectory = loaded.file.getParentFile();
			frame.setTitle(Settings.ATV+"    "+loaded);
		} else {
			frame.setTitle(Settings.ATV+"    "+Settings.BUILT);
		}
	}

	private Rectangle getBounds(Component frame) {
		final Rectangle bounds = Settings.THE.getBounds();
		if ( bounds != null )
			return bounds;
		// default to the right-hand half(ish) of the screen
		final Toolkit tk = frame.getToolkit();
		final java.awt.Dimension scrnSize = tk.getScreenSize();
		final GraphicsConfiguration gc = frame.getGraphicsConfiguration();
		final java.awt.Insets insets = tk.getScreenInsets(gc);
		final java.awt.Dimension frameSize = frame.getSize();
		final int screenWidth = scrnSize.width - insets.left - insets.right;
		final int screenHeight = scrnSize.height - insets.top - insets.bottom;
		final int x = screenWidth - frameSize.width + insets.left;
		return new Rectangle(x, 0, frameSize.width, screenHeight - 100);
	}

	// Copy from unfilteredHints to filteredHints, through the filter if its
	// active, and sort them by score (number of eliminations) descending then
	// by indice (toppest-leftest first)
	private void filterAndSortHints() {
		filteredHints = null;
		if ( unfilteredHints == null )
			return;
		if ( !Settings.THE.get(Settings.isFilteringHints) )
			// copy "as is"
			filteredHints = new ArrayList<>(unfilteredHints);
		else { 
			// unfiltered -> filter -> filtered
			filteredHints = new ArrayList<>(unfilteredHints.size());
			for ( AHint hint : unfilteredHints )
				if ( filterAccepts(hint) )
					addFilteredHint(hint);
		}
		filteredHints.sort(AHint.BY_SCORE_DESC_AND_INDICE);
	}

	/**
	 * Run only when user presses delete to "kill" hint in hintsTree TreeView.
	 * @param deadTech
	 */
	void removeHintsFrom(Tech deadTech) {
		if ( unfilteredHints == null )
			return;
		unfilteredHints.removeIf((hint) -> {
			return hint.hinter.tech == deadTech;
		});
		filterAndSortHints();
	}

	// returns Does this hint remove any NEW cells or maybes?
	@SuppressWarnings("fallthrough")
	private boolean filterAccepts(AHint hint) {
		switch ( hint.type ) {
		case AHint.INDIRECT:
			Pots redPots = hint.redPots;
			Values gone; // cell values that've allready been removed
			for ( Cell c : redPots.keySet() )
				if ( (gone=filteredPots.get(c)) == null
				  || (redPots.get(c).bits & ~gone.bits) != 0 )
					return true;
			// fallthrough
		case AHint.DIRECT:
			// nb: LinkedMatrixCellSet.contains is a fast O(1) operation.
			return hint.cell!=null && !filteredCells.contains(hint.cell);
		}
		return true; // case AHint.AGGREGATE or AHint.WARNING
	}

	@SuppressWarnings("fallthrough")
	private void addFilteredHint(AHint hint) {
		filteredHints.add(hint);
		switch( hint.type ) {
		case AHint.INDIRECT:
			filteredPots.upsertAll(hint.redPots);
			// fallthrough: some INDIRECT hints are actually DIRECT also!
		case AHint.DIRECT:
			if ( hint.cell != null )
				filteredCells.add(hint.cell);
			break;
		case AHint.AGGREGATE:
			AggregatedHint agg = (AggregatedHint)hint;
			for ( AHint h : agg.hints ) {
				if ( h.cell != null )
					filteredCells.add(h.cell);
				filteredPots.upsertAll(h.redPots);
			}
		//case AHint.WARNING: break; // no further action required		//case AHint.WARNING: break; // no further action required
		}
	}

	private void resetHintsFilter() {
		filteredPots = new Pots(); // RedPots we've already done
		filteredCells.clear(); // Cell values already encountered // faster to clear than create new
	}

	/** Set are we filtering-out subsequent hints with "repeat" outcomes. */
	void setFiltered(boolean isFilteringHints) {
		Settings.THE.set(Settings.isFilteringHints, isFilteringHints); // saves!
		Settings.THE.save();
		resetHintsFilter();
		this.selectedHints.clear(); // to unselect dissapeared hint
		filterAndSortHints();
		repaintAll();
	}

	private void repaintHints() {
		if ( selectedHints.size() == 1 ) {
			frame.setCurrentHint(selectedHints.peekFirst(), true);
			return;
		}
		frame.setCurrentHint(null, !selectedHints.isEmpty());
		if ( selectedHints.size() > 1 ) {
			// user has selected multiple hints to apply.
			repaintMultipleHints();
		}
	}

	// We set the SudokuGridPanel attributes "directly" rather than translate
	// the selected hints into an aggregate hint and display that. The downside
	// is that the SudokuGridPanel methods I call must then be package visible.
	private void repaintMultipleHints() {
		Pots greenPots = new Pots();
		Pots redPots = new Pots();
		Cell cell;
		for ( AHint hint : selectedHints ) {
			if ( (cell=hint.cell) != null )
				greenPots.put(cell, new Values(hint.value));
			if ( hint.type == AHint.INDIRECT )
				redPots.upsertAll(hint.redPots);
		}
		final SudokuGridPanel gp = gridPanel;
		gp.setAquaBGCells(greenPots.keySet());
		gp.setGreenPots(greenPots);
		gp.setRedPots(redPots);
		gp.setBluePots(null);
		gp.repaint();
		frame.setHintDetailArea(Html.load(this, "Multiple.html"));
	}

	/**
	 * The logView method is intended for techies. It displays the hint of each
	 * occurrence of a hinter name (Tech.nom) in a LogicalSolverTester logFile.
	 * <p>
	 * Press Ctrl-L again to find the next match for this hinter name in the
	 * selected logFile.
	 * <p>
	 * To start another search (or change hinter names) use the menu: Tools ~
	 * LogView.
	 * <p>
	 * I use this to debug: to check my changes work on a variety of cases.
	 * <p>
	 * NOTE: if you see a bug in logView it's likely just a bug in logView.
	 * Open the problematic puzzle normally and run-through solving it before
	 * you panic.
	 *
	 * @param logFile
	 * @param regex
	 * @return the next techNom
	 */
	String logView(File logFile, String regex) {
		try {
			while ( regex.length() == 0 )
				if ( (regex=Ask.forString("hint regex", regex)) == null )
					return null;
			Pattern pattern = Pattern.compile(regex);
			// read the logFile first time || if logFile has changed
			if ( logLines==null || !logFile.equals(loadedLogFile) ) {
				logLines = IO.slurp(logFile);
				loadedLogFile = logFile;
				startLine = 0;
			}
			// get the number of lines in the current logFile
			final int n = logLines.size();
			// reset at end of file, and beep to say "I wrapped"
			if ( startLine >= n ) {
				startLine = 0;
				beep();
			}
			String line, hint;
			int howMany;  char ch;
			for ( int i=startLine; i<n; ++i ) {
				line = logLines.get(i);
				if ( line.startsWith("WARN: ")  ) {
					// WARN: Death Blossom invalidity I3-9 in @Death Blossom: G2-67 (I3-9)!
					try {
						hint = line.substring(line.indexOf(" in ")+4);
						Matcher matcher = pattern.matcher(hint);
						if ( !matcher.matches() )
							continue;
						grid.load(logLines.get(i-2)+NL+logLines.get(i-1)+NL);
						getAllHints(false, false);
						if ( filteredHints.size() > 1 )
							for ( AHint h : filteredHints )
								if ( h.toString().equals(hint) ) {
									selectedHints.clear();
									selectedHints.add(h);
								}
						startLine = i + 1;
						return regex;
					} catch ( Exception eaten ) {
						beep();
					}
				} else if ( line.indexOf('#', 0) > -1 )
					// remember this line in case we match, in a field
					// to handle multiple-matches in a puzzle.
					puzzleIdLine = line;
				// the hint.toString starts-at char 65
				else if ( line.length() > 64
// use if techNom is NOT a regex (mine now is)
//					   && line.contains(techNom)
					   // first char is a digit 1..9 (line startsWith %-5d)
					   && ((ch=line.charAt(0))>='1' && ch<='9')
				) {
					hint = line.substring(65, line.length());
					Matcher matcher = pattern.matcher(hint);
					if ( !matcher.matches() )
						continue;
//if ( hint.startsWith("Aggregate of ")
//  && MyStrings.word(hint, 13).length() > 1 )
//	Debug.breakpoint();
					// howMany: skip back past the Aggregated hint lines.
					howMany = hint.startsWith("Aggregate of ")
							? MyInteger.parse(MyStrings.word(hint, 13))
							: 0;
					// found, so load the puzzle in the previous 2 logLines
					try {
						grid.load(
							logLines.get(i-howMany-2) + NL
						  + logLines.get(i-howMany-1) + NL
						  // There's no point in doing this until there's a match.
						  // We want the line upto \t, which is the puzzleId.
						  // FYI after \t is the puzzle-as-a-string we don't want.
						  // 34#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	......94.....9...53....5.7..8.4..1..463...........7.8.8..7.....7......28.5.26....
						  // -> 34#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
						  + MyStrings.upto(puzzleIdLine, '\t')
						);
					} catch ( Exception ex ) {
						System.out.println("Grid.load: "+ex+" from:" + NL
						  + logLines.get(i-howMany-2) + NL
						  + logLines.get(i-howMany-1) + NL
						  + MyStrings.upto(puzzleIdLine, '\t')
						);
						ex.printStackTrace(System.out);
					}
					// search the grid again and it SHOULD find the same hint.
					// If not then either the software has changed since your
					// logFile was produced, or s__t just happens sometimes.
					// If you press F5 you can get the hint that was applied.
					// nb: wantMore and wantSolution are both false
					getAllHints(false, false);
					// now if there's multiple hints returned then we'll select
					// the hint with a description which matches the one we just
					// read from the logFile, if it's there... other than that
					// the user (a techie) is on there own.
					if ( filteredHints.size() > 1
					  && puzzleIdLine.length() >= 65 ) {
						String hintText = puzzleIdLine.substring(65, puzzleIdLine.length());
						for ( AHint h : filteredHints )
							if ( h.toString().equals(hintText) ) {
								selectedHints.clear();
								selectedHints.add(h);
							}
					}
					// start the next search at the next line
					startLine = i + 1;
					// we're outta here
					return regex;
				}
			}
			// not found
			startLine = 0;
			beep();
			// give the user a chance to change the regex for the next call
			return Ask.forString("hint regex", regex);
		} catch (Throwable ex) {
			StdErr.whinge("logView error", ex);
			beep();
			return null;
		}
	}
	private int startLine = 0;
	private File loadedLogFile;
	private String puzzleIdLine = "";
	private ArrayList<String> logLines; // the lines in the logFile

	void clearUndos() {
		undos.clear();
		redos.clear();
	}

	void undo() {
		if ( undos.isEmpty() ) {
			beep();
			return;
		}
		redos.addFirst(grid.toString());
		grid.load(undos.removeFirst());
		clearHints();
		prevCell = null; // so that next setCell/remove/restoreMaybe saves
	}

	void redo() {
		if ( redos.isEmpty() ) {
			beep();
			return;
		}
		undos.addFirst(grid.toString());
		grid.load(redos.removeFirst());
		prevCell = null; // so that next setCell/remove/restoreMaybe saves
	}

	private final int UNDOS_MAX = 64; // just a guess
	private final UndoList undos = new UndoList();
	private final UndoList redos = new UndoList();

	/** Ignores all wrongens. */
	private void doDegrelease(int value, Cell cell) throws DegreleaseException {
		// ensure the given grid has a puzzleID, which is set when the Grid is
		// created and then reset whenever a puzzle is loaded into the Grid.
		// This is defensive programming against an upstream bug I'm not sure
		// exists but I think this should solve the problem. I'm not positive
		// this is necessary or even desirable, it just "feels right".
		if ( grid.puzzleID == 0L )
			grid.puzzleID = Grid.RANDOM.nextLong();
		// if solutionValues is unset || it's a different puzzle
		if ( solutionValues==null || grid.puzzleID!=solutionValuesGridId ) {
			if ( grid.solutionValues == null )
				solver.solveQuicklyAndQuietly(grid); // sets grid.solutionValues
			solutionValues = grid.solutionValues;
			solutionValuesGridId = grid.puzzleID;
			// not real sure I need to do this HERE... might be better to leave
			// it to solve/getFirstHint/getAllHints, but I'm doing it anyway.
			if ( HintValidator.ANY_USES )
				HintValidator.setSolutionValues(grid.solutionValues, grid.puzzleID);
		}
		// ignore all wrongens!
		if ( value != solutionValues[cell.i] )
			throw new DegreleaseException();
	}
	// need the solutionValues to validate user input against in godMode
	private int[] solutionValues = null;
	private long solutionValuesGridId;

	private final class UndoList extends LinkedList<String> {
		private static final long serialVersionUID = 216354987L;
		@Override
		public void addFirst(String e) {
			if ( super.size() >= UNDOS_MAX )
				super.removeLast();
			super.addFirst(e);
		}
		/**
		 * Please call the addFirst method directly. Overridden just for safety
		 * to call addFirst and return true.
		 * @param e to addFirst
		 * @return true
		 */
		@Override
		public boolean add(String e) {
			assert false : "please call addFirst directly."; // techies only
			addFirst(e);
			return true;
		}
	};

	/**
	 * Invoked only by the GUI when the user sets the value of a cell in the
	 * grid.
	 *
	 * @param cell the cell to set.
	 * @param value the value (1..9 inclusive) to set, or 0 meaning that
	 * the cells existing value is to be erased.
	 */
	void setTheCellsValue(Cell cell, int value) {
		// type the value twice if it is NOT in the maybes list.
		if ( value!=0 && cell.maybes.no(value)
		  && (prevCell!=cell || prevValue!=value) ) {
			beep();
			prevCell = cell;
			prevValue = value;
			return;
		}
		prevCell = null;
		prevValue = 0;
		try {
			if ( degreleaseMode )
				doDegrelease(value, cell);
			// add an undo BEFORE we change the grid
			undos.addFirst(grid.toString());
			// change the grid
			cell.setManually(value, cell.value); // throws UnsolvableException
			// validate the grid
			if ( grid.isInvalidated() ) { // missingMaybes, doubles, homeless
				gridPanel.flashBackgroundPink();
				beep();
				undo(); // so that (to the user) value won't set
				lastDitchRebuild();
			}
			boolean needRepaintHints = (filteredHints != null);
			clearHints(false);
			this.selectedHints.clear();
			if (needRepaintHints) {
				repaintHintsTree();
				repaintHints();
			}
		} catch (DegreleaseException eaten) {
			beep();
		} catch (UnsolvableException ex) { // when canNotBe 0's bits
			undo(); // so that (to the user) value doesn't dissapear
			beep();
			lastDitchRebuild();
		}
		repaintAll();
	}

	private void lastDitchRebuild() {
		try {
			grid.rebuildMaybesAndS__t();
		} catch (UnsolvableException ex) {
			beep(); beep(); beep(); beep(); beep();
			JOptionPane.showMessageDialog(frame, "Kick it in the guts!"
					, "We're screwed!", JOptionPane.ERROR_MESSAGE);
			//grid.clear(); // overkill!
		}
	}

	/** User wants to remove/reinstate this potential cell value. */
	void maybeTyped(Cell cell, int v) {
		if ( cell.maybes.contains(v) ) {
			// remove v from cells maybes
			if ( degreleaseMode )
				try {
					doDegrelease(v, cell);
				} catch (DegreleaseException ex) {
					beep();
					return;
				}
			// actually remove v from cells maybes
			try {
				// all maybe removals on a cell are undone by 1 undo request,
				// so the undoGrid is saved prior to the removal of the first
				// maybe of that cell (when cell!=prevCell).
				if ( cell != prevCell ) {
					undos.addFirst(grid.toString());
					prevCell = cell;
				}
				cell.canNotBe(v);
			} catch(UnsolvableException ex) {
				frame.setHintDetailArea(ex.toString());
				beep();
				return;
			}
			if ( grid.hasMissingMaybes() )
				beep();
		} else {
			// reinstate v to cells maybes
			if ( cell.canSeeValue(v) )
				beep();
			else {
				if ( cell != prevCell ) {
					// all maybe removals on the cell are undone by one undo.
					undos.addFirst(grid.toString());
					prevCell = cell;
				}
				cell.canSoBe(v);
				grid.cancelMaybes(); // for safety
			}
		}
	}

	public void beep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	/** Display the given hints which have been selected. */
	void hintsSelected(Collection<HintNode> hintNodes) {
		this.selectedHints.clear();
		AHint hint;
		for ( HintNode hn : hintNodes )
			if ( (hint=hn.getHint()) != null )
				this.selectedHints.add(hint);
		repaintHints();
	}

	private void repaintAll() {
		if ( gridPanel == null ) // we're in startUp
			return;
		repaintHintsTree();
		repaintHints();
		gridPanel.repaint();
	}

	/** Clears the current grid - actually replaces him with a brand new one.*/
	void clearGrid() {
		gridPanel.setSudokuGrid(grid = new Grid());
		clearHints();
		clearUndos();
		solutionValues = null;
		solutionValuesGridId = 0L;
//		frame.showWelcomeText();
		frame.setTitle(Settings.ATV+"    "+Settings.BUILT);
	}

	/** Make this Grid the current one. */
	void setGrid(Grid grid) {
		this.grid = grid;
		grid.rebuildMaybesAndS__t();
		gridPanel.setSudokuGrid(grid);
		gridPanel.clearSelection(true);
		clearHints();
//		// nb: empty string "" is displayed as an empty unumbered list.
//		frame.setHintDetailArea(null);
	}

	/** @return the current Grid. */
	Grid getGrid() {
		return gridPanel.getSudokuGrid();
	}

	/** clear the hints - both the selected hint(s) and the hints lists. */
	void clearHints() {
		clearHints(true);
	}

	/** clear the hints - both the selected hint(s) and the hints lists.
	 * @param if true then clear the selection else just clear the focus. */
	void clearHints(boolean isSelection) {
		if ( gridPanel == null ) // we're in startUp
			return;
		unfilteredHints = null;
		filteredHints = null;
		resetHintsFilter();
		selectedHints.clear();
		gridPanel.clearSelection(isSelection);
		repaintAll();
	}

	/** recreate the LogicalSolver because the Settings have changed. */
	void recreateLogicalSolver() {
		// we must recreate the LogicalSolver in order to make it pick-up any
		// change in the Settings.Preferences, specifically the wantedHinters.
		solver = LogicalSolverFactory.recreate();
		// prepare the new LogicalSolver on the premise that the old one was
		// prepared. It shouldn't matter if the old one was not prepared.
		solver.prepare(grid);
	}

	private void displayError(Throwable ex) {
		System.out.flush();
		ex.printStackTrace(System.out);
		try {
			repaintAll();
		} catch (Throwable t) {
			whinge(t);
		}
		frame.setHintDetailArea("<html><body><pre><font color=\"red\">"
				+ ex.toString().replace("\n", NL)
				+ "</font></pre></body></html>");
	}

	/** Validate the puzzle and the grid and display any warning/s.
	 * @return is the grid valid? */
	boolean validatePuzzleAndGrid(boolean isNoisy) {
		selectedHints.clear();
		unfilteredHints = new ArrayList<>();
		AHint hint = solver.validatePuzzleAndGrid(grid, isNoisy);
		if ( hint != null ) {
			unfilteredHints.add(hint);
			selectedHints.add(hint);
		}
		filterAndSortHints();
		repaintAll();
		return (hint == null);
	}

	/** Rebuild the potential values in the grid. */
	void resetPotentialValues() {
		grid.rebuildMaybesAndS__t();
		clearHints();
	}

	/** Searches the grid for the next hint, which is displayed,
	 * else an error-message is displayed.
	 * Called by this.solveStep
	 *       and SudokuFrames btnGetNextHint and mitGetNextHint.
	 * @param wantMore true when big (aggregate) hints are desired. */
	void getNextHint(boolean wantMore, boolean wantSolution) {
		try {
			AHint hint = this.getNextHintImpl(wantMore, wantSolution);
			if ( hint != null ) {
				addFilteredHint(hint);
				selectedHints.add(hint);
			}
			repaintAll();
		} catch (Throwable ex) {
			displayError(ex);
		}
	}

	// Called by getNextHint (above) and getClue (below)
	private AHint getNextHintImpl(boolean wantMore, boolean wantSolution) {
		GrabBag.grid = grid;
		if ( unfilteredHints == null ) {
			unfilteredHints = new ArrayList<>();
			filterAndSortHints();
		}
		// create temporary buffers for gathering a hint
		ArrayList<AHint> currHints = new ArrayList<>();
		SingleHintsAccumulator accu = new SingleHintsAccumulator();
		solver.singlesSolution = null;
		// find next hint
		if ( solver.solveWithSingles(grid, accu) ) {
			if ( wantSolution )
				accu.add(new SolutionHint(new DummySolutionHinter(), grid
						, solver.singlesSolution, false));
		} else if ( wantMore )
			solver.getAllHints(grid, wantMore, true);
		else
			solver.getFirstHint(grid, unfilteredHints, currHints, accu);
		// move the hint from accu to unfilteredHints
		AHint hint = accu.getHint();
		if ( hint != null )
			unfilteredHints.add(hint);
		selectedHints.clear();
		return hint; // may be null!
	}

	/** Get a clue: a vague pointer towards the next hint. In terse mode we tell
	 * the user what kind of pattern (ie Tech) to look for next; and if "isBig"
	 * then we tell a bit about where to look as well, which is basically a hint
	 * without the jam for simple (ie human do-able) hints.
	 * Called by SudokuFrame getMitGetSmallClue & getMitGetBigClue
	 * @param isBig Are big (more informative) clues desired? */
	void getClue(boolean isBig) {
		clearHints();
		AHint hint = getNextHintImpl(isBig, false);
		if ( hint == null )
			return;
		if ( hint instanceof IActualHint ) {
			String filename = isBig ? "BigClue.html" : "SmallClue.html";
			String html = Html.load(this, filename);
			html = html.replace("{0}", hint.getClueHtml(isBig));
			gridPanel.setBases(hint.getBases());
			html = Html.colorIn(html);
			frame.setHintDetailArea(html);
			unfilteredHints = null;
			resetHintsFilter();
			filterAndSortHints();
		} else {
			addFilteredHint(hint);
			selectedHints.add(hint);
			repaintAll();
		}
	}

	/** Searches the grid for all the simplest available hint(s).
	 * <p>Called by SudokuFrame.getAllHintsLater (Shift-F5) which runs the
	 * search on a background thread, so it's doesn't block the EDT.
	 * @param wantMore are more hints wanted, regardless of how "hard" they are,
	 * or how long it takes to find them all; which in practice means a wait of
	 * max 15 seconds on a ~2900 Mhz intel i7.
	 * @param wantSolution
	 */
	void getAllHints(boolean wantMore, boolean wantSolution) {
		try {
			IAccumulator accu = new SingleHintsAccumulator();
			if ( grid.isFull() )
				// it's already solved numbnuts!
				unfilteredHints = AHint.list(new SolvedHint());
			else if ( wantSolution && solver.solveWithSingles(grid, accu) )
				// hold down the Shift key to get the solution NOW!
				unfilteredHints = AHint.list(
						new SolutionHint(new DummySolutionHinter(), grid
								, solver.singlesSolution, true) );
			else
				// find the next hint
				unfilteredHints = solver.getAllHints(grid, wantMore, true);
			selectedHints.clear();
			resetHintsFilter();
			filterAndSortHints();
			if ( filteredHints!=null && !filteredHints.isEmpty() )
				selectedHints.add(filteredHints.get(0));
			repaintAll();
		} catch (Throwable ex) {
			displayError(ex);
		}
	}

	/**
	 * Apply the selected hints. Applying a "direct" hint means setting the
	 * cell value. Applying an "indirect" hint means removing the removable
	 * potentials. Some hints do a bit of both, just to keep it interesting,
	 * for instance a Direct Naked Triple.
	 *
	 * Multiple hints may be user-selected and applied all in one hit. BUT
	 * there's a bug in applying multiple hints: a group of hints that are
	 * ALL individually valid sends the grid invalid when they are applied
	 * all in one hit, and I'll be ____ed if I know what I've done wrong!
	 *
	 * You HAVE been warned. Sigh.
	 */
	void applySelectedHints() {
		final int n = selectedHints.size();
		if ( n > 0 ) {
			String backup = grid.toString();
			try {
				if ( grid.isFull() )
					return;
				GrabBag.grid = grid; // used to log grid + hint in VERBOSE mode
				for ( AHint hint : selectedHints ) {
					undos.addFirst(grid.toString());
					hint.apply(Grid.NO_AUTOSOLVE, true);
					if ( grid.isFull() )
						break;
// This is temporary code to deal with a bug where the application of multiple
// hints (where all hints there-in are valid if applied individually) sends the
// grid invalid; so I check validity after applying each hint, and reject the
// whole selection when we hit the first hint that makes the grid invalid. It's
// then upto the user to unselect the offending hint and try again, or, give-up
// applying multiple hints! Sigh on that sigh. May I borrow your cheese grater?
// 2020-10-21 It also detects my dodgy Skycrapper hints. Good!
if ( GOT_DODGY_HINTS ) {
	if ( solver.recursiveAnalyser.countSolutions(grid, false) != 1 ) {
		Log.print(NL+"Funky grid:"+NL+grid+NL);
		grid.restore(backup);
		Log.print(NL+"Restored (previous) grid:"+NL+grid+NL);
		IllegalStateException ex =
			new IllegalStateException("invalid after: "+hint.toFullString());
		// pucker-up folks
		beep(); beep(); beep();
		gridPanel.flashBackgroundPink();
		displayError(ex);
		throw ex;
	}
}
				}
				if ( Settings.THE.get(Settings.isGreenFlash) )
					try {
						if ( solver.solveWithSingles(grid, null) )
							gridPanel.flashBackgroundAqua();
					} catch(Throwable eaten) {
						// Do nothing: don't care if she doesn't flash!
					}
			} catch(UnsolvableException ex) { // from hint.apply
				System.out.flush();
				System.out.format(NL+ex+NL);
				for ( AHint h : selectedHints )
					System.out.format("%s%s", h.toFullString(), NL);
				System.out.print(NL+"Dodgy grid:"+NL+grid+NL);
				ex.printStackTrace(System.out);

				grid.restore(backup);
				System.out.print(NL+"Restored grid:"+NL+grid+NL);

				System.out.flush();
			}
		}
		clearHints();
		repaintAll();
	}

	void invokeGetNextHint(final boolean wantMore, final boolean wantSolution) {
		frame.setHintDetailArea("Searching ...");
		frame.repaint();
		// invokeLater to repaint before we getNextHint.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				getNextHint(wantMore, wantSolution);
			}
			@Override
			public String toString() {
				return "getNextHint_"+super.toString();
			}
		});
	}

	/**
	 * Apply the selected hint, if any, and get the next hint.
	 */
	void solveStep(final boolean wantMore) {
		applySelectedHints();
		if ( !grid.isFull() )
			invokeGetNextHint(wantMore, false);
	}

	private void repaintHintsTree() {
		final List<AHint> fh = filteredHints;
		if ( fh == null )
			frame.clearHintsTree();
		else {
			final HintNode root = new HintsTreeBuilder().build(fh);
			final HintNode selected;
			if ( fh.isEmpty() )
				selected = null; // Never say never
			else
				selected = root.getNodeFor(fh.get(0));
			frame.setHintsTree(root, selected, fh.size()>1);
		}
	}

	/** Paste the clipboard into the current grid. */
	void pastePuzzle() {
		// tell the LogicalSolver that we're finished with the existing puzzle.
		// nb: if we miss this the solver still has hints cached, which are all
		//     very borken, because they're not even for the current puzzle.
		solver.cleanUp();
		// we need a backup to revert to if all goes horribly wrong.
		Grid backup = new Grid(grid);
		// clear the existing puzzle (if any) from display.
		clearGrid();
		try {
			grid.source = null;
			GridFactory.pasteClipboardTo(grid);
			AHinter.hackTop1465 = grid.hackTop1465();
			AHint.hintNumber = 1; // reset the hint number
			frame.setTitle(ATV+"    (clipboard)");
		} catch (IOException ex) {
			backup.copyTo(grid);
			grid.source = backup.source;
			AHinter.hackTop1465 = grid.hackTop1465();
			asker.carp(ex, "Paste");
		}
	}

	/** Copy the grid to the clipboard. */
	void copyGridToClipboard() {
		grid.copyToClipboard();
	}

	/** Copy the given String to the Systems clipboard.
	 * @param s to copy */
	void copyToClipboard(String s) {
		IO.copyToClipboard(s);
	}

	/** Reload the current file. */
	final PuzzleID reloadFile() {
		return loadFile(grid.source);
	}

	/** Load the next puzzle in a .mt (MagicTour, ie multi-puzzle) file. */
	final PuzzleID loadNextPuzzle() {
		PuzzleID gs = grid.source;
		if ( gs==null || gs.file==null )
			return null;
		return loadFile(gs.file, gs.lineNumber+1);
	}

	/** Load the given PuzzleID. */
	final PuzzleID loadFile(PuzzleID id) {
		return id==null ? null : loadFile(id.file, id.lineNumber);
	}

	/**
	 * Load lineNumber puzzle from the given file into the grid.
	 * @param file to load
	 * @param lineNumber to load. Only used when filename.endsWith(".mt")
	 * @return String puzzleName (PuzzleID.toString())
	 */
	final PuzzleID loadFile(File file, int lineNumber) {
		if ( !file.exists() )
			return null;
		// clear the solvers hint-cache/s before we load the new puzzle,
		// otherwise we see hints from the previous puzzle transposed onto
		// this puzzle, which is JUST PLAIN WRONG!
		if ( solver != null )
			solver.cleanUp();
		// take a backup before we load da fooker
		Grid backup = new Grid(this.grid);
		// clear the current grid from the display
		clearGrid();
		frame.clearHintDetailArea();
		// load da fooker!
		if ( !IO.load(grid, file, lineNumber) )
			backup.copyTo(grid);
		else if ( !grid.isMaybesLoaded )
			grid.rebuildMaybesAndS__t();
		GrabBag.grid = grid;
		AHint.hintNumber = 1; // reset the hint number
		AHinter.hackTop1465 = grid.hackTop1465();
		return recentFiles.add(grid.source);
	}

	/**
	 * Loads the given String into the grid. The string is presumed to
	 * be in the format produced by the {@code Grid,toString()} method.
	 * @param stringData the puzzle to load.
	 */
	void loadStringIntoGrid(String stringData) {
		AHint.hintNumber = 1; // reset the hint number
		grid.source = null; // default to null, may be set by grid.load from 3rd line in stringData
		grid.load(stringData);
		GrabBag.grid = grid;
		AHinter.hackTop1465 = grid.hackTop1465();
	}

	/** Returns the next .txt File to be opened. */
	File nextTxtFile(File file) {
		File[] txtFiles = file.getParentFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".txt");
			}
		});
		String filename = file.getName();
		int n = txtFiles.length - 1;
		for ( int i=0; i<n; ++i )
			if ( txtFiles[i].getName().equalsIgnoreCase(filename) )
				return txtFiles[i+1];
		if ( txtFiles[n].getName().equalsIgnoreCase(filename) )
			return txtFiles[0];
		return null;
	}

	/** Save the grid to the given File. */
	String saveFile(File file) {
		try {
			IO.save(grid.toString(), file);
			grid.source = new PuzzleID(file);
			clearUndos();
			return recentFiles.add(grid.source).toString();
		} catch (IOException ex) {
			asker.carp(ex, "save");
			return null;
		}
	}

	/** Solve the current grid recursively (ie ASAP) to display the solution. */
	void solveRecursively() {
		clearHints();
		unfilteredHints = new ArrayList<>();
		long t0 = System.nanoTime();
		AHint solutionHint = solver.solveRecursively(grid);
		if ( Log.MODE >= Log.VERBOSE_2_MODE )
			System.out.format("<solveASAP: %,15d%s", System.nanoTime()-t0, NL);
		if ( solutionHint != null ) {
			unfilteredHints.add(solutionHint);
			selectedHints.add(solutionHint);
		}
		filterAndSortHints();
		repaintAll();
	}

	/** Solve the current grid using logic and display the required
	 * Hinters&nbsp;/&nbsp;hintTypes and there difficulty-ratings. */
	AHint analyse() {
		try {
			// exit-early when called during shutdown (don't ask me why/how)
			if ( solver == null )
				return null;
			clearHints(true);
			unfilteredHints = new ArrayList<>();
			// we need to make a copy, else the grid underneath the GUI changes
			// so mouseMove-while-it's-analysing repaints the modified grid.
			// Then it all snaps-back when analyse finishes, which is UGLY!
			Grid copy = new Grid(grid);
			// prepare the hinters to analyse this puzzle
			solver.prepare(copy);
			// analyse this puzzle
			AHint hint = solver.analyse(copy);
			// hint should never be null. Never say never.
			if ( hint != null ) {
				unfilteredHints.add(hint);
				selectedHints.add(hint);
			}
			filterAndSortHints();
			return hint;
		} catch (UnsolvableException ex) {
			throw ex;
		} catch (Throwable ex) {
			displayError(ex);
			return null;
		} finally {
			repaintAll();
		}
	}

	/**
	 * Display a single hint.
	 * @param hint the hint to display
	 */
	void showHint(AHint hint) {
		clearHints();
		unfilteredHints = new ArrayList<>();
		if ( hint != null ) {
			unfilteredHints.add(hint);
			selectedHints.add(hint);
		}
		filterAndSortHints();
		repaintAll();
	}

	boolean copyUnfilteredHintsListToClipbard() {
		try {
			if ( unfilteredHints==null || unfilteredHints.isEmpty() )
				return false;
			StringBuilder sb = new StringBuilder(
					unfilteredHints.size() * AHint.AVG_CHARS_PER);
			for ( AHint hint : unfilteredHints )
				sb.append(hint.hinter.tech.name()).append("\t")
				  .append(hint.toFullString()).append(NL);
			IO.copyToClipboard(sb.toString());
			return true;
		} catch (Exception ex) {
			whinge(ex);
			return false;
		}
	}

	boolean copyToClipboard(ArrayList<HintNode> selections) {
		if ( selections==null || selections.isEmpty() )
			return false;
		try {
			StringBuilder sb = new StringBuilder(
					selections.size() * AHint.AVG_CHARS_PER);
			for ( HintNode hintNode : selections ) {
				AHint hint = hintNode.getHint();
				if ( hint != null ) { // hints are the leaves
					sb.append(hint.hinter.tech.name()).append("\t")
							.append(hint.toFullString()).append(NL);
				} else appendBranchRecursivelyTo(sb, hintNode);
			}
			IO.copyToClipboard(sb.toString());
			return true;
		} catch (Exception ex) {
			whinge(ex);
			return false;
		}
	}

	void appendBranchRecursivelyTo(StringBuilder sb, HintNode hintNode) {
		AHint hint = hintNode.getHint();
		if ( hint != null ) // leaf
			sb.append(hint.hinter.tech.name()).append("\t")
			  .append(hint.toFullString()).append(NL);
		else for ( int i=0,n=hintNode.getChildCount(); i<n; ++i ) // branch
			appendBranchRecursivelyTo(sb, (HintNode)hintNode.getChildAt(i));
	}

	/** prints t's stackTrace to stderr and returns false. */
	public boolean whinge(Throwable t) {
		return StdErr.whinge(t);
	}

	/** prints msg to stderr and returns false. */
	public boolean whinge(String msg) {
		return StdErr.whinge(msg);
	}

	/** prints a one line error message and returns false. */
	public boolean carp(String msg, Throwable t) {
		return StdErr.carp(msg, t);
	}

	/**
	 * Start point of the application. No splash screen is
	 * handled there.
	 * @param args program arguments (not used)
	 */
	public static void main(String[] args) {
		// some classes need to know whether or not they're running in the
		// LogicalSolverTester, as apposed to the GUI or a jUnit test-case.
		// It's all a bit hackish. Please don't forget to breath between vollies
		// of cursing me. You'll really really want to. Quite a lot. Trust me.
		Run.type = Run.Type.GUI;
		// I need at-least one argument: the name of the logFile
		if ( args.length < 1 )
			StdErr.exit("GUI usage: java -jar SudokuExplainer.jar logFileName (eg SudokuExplainer.log)");
		// Prevent the GUI overwriting the .mt-file when a GUI.jar is run as if
		// it's a LogicalSolverTester jar-file;
		// ie ya built the wrong configuration ya great leapin putz!
		File logFile = new File(args[0]);
		if ( !logFile.getAbsolutePath().toLowerCase().endsWith(".log") ) {
			StdErr.whinge("GUI logFileName: *.log required, not: "
					+logFile.getAbsolutePath());
			return;
		}
		try {
			Log.out = new PrintStream(logFile);
		} catch (Throwable t) {
			StdErr.exit("failed to set Log.out", t);
		}

		try {
			String lookAndFeelClassName = Settings.THE.getLookAndFeelClassName();
			if (lookAndFeelClassName == null)
				lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelClassName);
		} catch(Exception ex) {
			System.out.flush();
			ex.printStackTrace(System.out);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new SudokuExplainer();
			}
		});
	}

	/** Closes (ie saves) the recent files list. */
	@Override
	public void close() throws java.io.IOException {
		if ( solver != null ) {
			try {
				solver.close();
			} catch (Throwable eaten) {
				// Do nothing.
			}
			solver = null;
		}
		if ( recentFiles != null ) {
			try {
				recentFiles.close();
			} catch (Throwable eaten) {
				// Do nothing.
			}
			recentFiles = null;
		}
	}

	/**
	 * The first rule of degrelease mode is I don't do questions d__khead.
	 * @author Keith Corlett 2020 Jun 20
	 */
	private static final class DegreleaseException extends Exception {
		private static final long serialVersionUID = 124752002380L;
		public DegreleaseException() { }
	}

}
