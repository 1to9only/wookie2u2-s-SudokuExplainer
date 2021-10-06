/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Build.ATV;
import static diuf.sudoku.Build.BUILT;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.GridFactory;
import diuf.sudoku.Pots;
import diuf.sudoku.PuzzleID;
import diuf.sudoku.Run;
import diuf.sudoku.Settings;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.checks.SolutionHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.align.LinkedMatrixCellSet;
import static diuf.sudoku.utils.Frmt.*;
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
 * it all together. In short: I'm the glue.
 * <p>
 * This design keeps the GUI focused on GUI-stuff and the actions blissfully
 * unaware that there even IS a GUI. I'm the only part that knows about the GUI
 * and the actions, so that the other two remain ignorant of each other. If the
 * action classes need to know about the GUI it goes via an interface that
 * exposes only the required methods in "abstract" form.
 * <p>
 * There's a few cases, such as asking the user (IAsker) and reporting run
 * statistics (IReporter) where there really is no choice but for the actions
 * to be aware of aspects of the GUI; but these "incursions" are always limited
 * to the minimal required using an interface. So every-time the GUI sticks
 * stuff in an action it does-so via an interface. Condoms!
 * <p>
 * So, obviously, anytime the GUI needs to know about an action it delegates
 * the details to me, by calling my "generic" (undetailed) method.
 * <p>
 * Anytime an action needs to know the GUI it does so through an interface,
 * like IAsker or IErrorReporter or similar; to limit the extent of the action
 * classes knowledge of the GUI. Ideally actions remain blissfully unaware of
 * the existence of any GUI; which isn't always practical, so we use interfaces
 * to limit such exposure.
 * <p>
 * If a new web-interface is required you implement your web stuff and a new
 * SudokuExplainerWeb. The action classes don't need to change (much, sigh).
 *
 * @see diuf.sudoku.gui.SudokuFrame
 * @see diuf.sudoku.solver.LogicalSolver
 */
public final class SudokuExplainer implements Closeable {

	/**
	 * Should apply detect and render invalid grid after hint apply'd?
	 */
	private static final boolean HANDLE_INVALID_GRID = true;

	private Grid grid;					// The Sudoku grid
	public LogicalSolver solver;		// The Sudoku solver
	public final SudokuFrame frame;			// The main GUI frame
	private final IAsker asker;			// an aspect of the main GUI frame
	private final SudokuGridPanel gridPanel;	// The grid

	// The raw un-filtered hints, and the cooked filtered version.
	private List<AHint> unfilteredHints = null; // all hints (unfiltered)
	private List<AHint> filteredHints = null; // filtered hints (by effects)
	// Cells which have already been filtered-out.
	private final LinkedMatrixCellSet filteredCells = new LinkedMatrixCellSet();
	// The potential cell values which have already been removed (filtered-out).
	private Pots filteredPots = new Pots();
	// The potential cell values which have already been set (to filter-out).
	private Pots presetPots = new Pots();

	 // The list of currently selected hint/s.
	private final LinkedList<AHint> selectedHints = new LinkedList<>();

	/** The recently accessed (opened or saved) files list - a self-
	 * persistent collection, so long as you close me in dispose. */
	RecentFiles recentFiles;

	// For skip bad setValue request unless repeated
	private Cell prevCell = null;
	private int prevValue = -1;

	//	god mode
	public boolean godMode = false;

	/**
	 * SudokuExplainer is a singleton.
	 * @return THE instance of SudokuExplainer.
	 */
	public static SudokuExplainer getInstance() {
		if ( theInstance == null ) {
			assert Log.out != null;
			theInstance = new SudokuExplainer();
		}
		return theInstance;
	}
	private static SudokuExplainer theInstance;

	// nb: this Constructor is private, but getInstance() is public.
	// nb: Log.log is ALWAYS set before we invoke the constructor.
	private SudokuExplainer() {
		// get mostRecent file
		this.recentFiles = RecentFiles.getInstance();
		final PuzzleID pid = recentFiles.mostRecent(); // nullable
		// we need a grid, so we always start with an empty one.
		grid = new Grid();
		// set the frame to the empty grid
		asker = frame = new SudokuFrame(grid, this);
		gridPanel = frame.gridPanel;
		// load the mostRecent pid into the grid and return it's ID
		final PuzzleID loaded = loadFile(pid); // nullable
		// display the loaded PuzzleID in the frame's title
		if ( loaded == null ) {
			frame.setTitle(ATV+"    "+BUILT);
		} else {
			frame.defaultDirectory = loaded.file.getParentFile();
			frame.setTitle(ATV+"    "+loaded);
		}
		// get the solver
		Log.teeln("\nnew SudokuExplainer...");
		solver = LogicalSolverFactory.get();
		// nb: GUI cares-not about Log.MODE
		Log.teeln(solver.getWantedHinterNames());
		Log.teeln(solver.getUnwantedHinterNames());
		// time to paint
		repaintHintsTree();
		frame.pack();
		frame.setBounds(getBounds(frame));
		frame.setVisible(true);
	}

	private Rectangle getBounds(Component frame) {
		final Rectangle bounds = THE_SETTINGS.getBounds();
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
		if ( unfilteredHints == null ) {
			return;
		}
		if ( THE_SETTINGS.isFilteringHints() ) {
			// unfiltered -> filter -> filtered
			filteredHints = new ArrayList<>(unfilteredHints.size());
			for ( AHint hint : unfilteredHints ) {
				if ( filterAccepts(hint) ) {
					addFilteredHint(hint);
				}
			}
		} else {
			// copy "as is"
			filteredHints = new ArrayList<>(unfilteredHints);
		}
		filteredHints.sort(AHint.BY_ORDER);
	}

	/**
	 * Run only when user presses delete to "kill" hint in hintsTree TreeView.
	 * @param deadTech
	 */
	void removeHintsFrom(Tech deadTech) {
		if ( unfilteredHints == null ) {
			return;
		}
		unfilteredHints.removeIf((hint) -> {
			return hint.hinter.tech == deadTech;
		});
		filterAndSortHints();
	}

	// returns Does this hint remove any NEW cells or maybes?
	@SuppressWarnings("fallthrough")
	private boolean filterAccepts(AHint hint) {
		switch ( hint.type ) {
		case AHint.MULTI: // XColoringHintMulti only, currently
			// fallthrough
		case AHint.INDIRECT:
			Pots setPots = hint.getResults();
			Pots redPots = hint.redPots;
			Integer gone; // cell values that've allready been removed
			if ( setPots != null  ) {
				// XColoringHintMulti sets multiple cells
				for ( java.util.Map.Entry<Cell,Integer> e : setPots.entrySet() ) {
					if ( (gone=presetPots.get(e.getKey())) == null
					  || (e.getValue() & ~gone) != 0 ) {
						return true;
					}
				}
			} else if ( redPots != null ) {
				for ( java.util.Map.Entry<Cell,Integer> e : redPots.entrySet() ) {
					if ( (gone=filteredPots.get(e.getKey())) == null
					  || (e.getValue() & ~gone) != 0 ) {
						return true;
					}
				}
			}
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
			// fallthrough: some INDIRECT hints are actually DIRECT also!
			// fallthrough: some INDIRECT hints are actually DIRECT also!
		case AHint.DIRECT:
			if ( hint.cell != null )
				filteredCells.add(hint.cell);
			break;
		case AHint.WARNING:
			break; // no further action required
		case AHint.MULTI:
			presetPots.upsertAll(hint.getResults());
			break;
		}
	}

	private void resetHintsFilter() {
		filteredPots = new Pots(); // redPots we've already done
		presetPots = new Pots(); // setPots we've already done
		filteredCells.clear(); // Cell values already encountered // faster to clear than create new
	}

	/** Set are we filtering-out subsequent hints with "repeat" outcomes. */
	void setFiltered(boolean isFilteringHints) {
		THE_SETTINGS.setIsFilteringHints(isFilteringHints); // saves!
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
		final Pots greenPots = new Pots();
		final Pots redPots = new Pots();
		for ( AHint hint : selectedHints ) {
			if ( hint.cell != null ) {
				greenPots.put(hint.cell, VSHFT[hint.value]);
			}
			if ( hint.type == AHint.INDIRECT ) {
				redPots.upsertAll(hint.redPots);
			}
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
	 * occurrence of a Tech name in a LogicalSolverTester logFile.
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
	 * @param logFile the LogicalSolverTester .log to display hints from
	 * @param re the hint-regex (regular expression) String
	 * @return the new/modified regex
	 */
	String logView(File logFile, String re) {
		try {
			while ( re==null || re.length()==0 ) {
				// Note: can't use LogViewHintRegexDialog here coz he calls-back
				// SudokuFrame.setRegex, which calls me. sigh.
				if ( (re=frame.askForString("NONE: hint regex", re)) == null ) {
					return null;
				}
			}
			Pattern pattern = Pattern.compile(re);
			// read the logFile first time || if logFile has changed
			// if a puzzle is re/loaded during viewing then logLines=null (to
			// free up RAM), in which case logLines will be reloaded (slow).
			if ( logLines==null || !logFile.equals(loadedLogFile) ) {
				logLines = IO.slurp(logFile);
				loadedLogFile = logFile;
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
				// WARN: Death Blossom invalidity I3-9 in @Death Blossom: G2-67 (I3-9)!
				if ( line.startsWith("WARN: ")  ) {
					try {
						if ( pattern.matcher(hint=line.substring(line.indexOf(IN)+4)).matches() ) {
							grid.load(logLines.get(i-2)+NL+logLines.get(i-1)+NL);
							getAllHints(false, false, false, false); // wantMore, wantSolution, logHints, printHints
							if ( filteredHints.size() > 1 ) {
								for ( AHint h : filteredHints ) {
									if ( h.toString().equals(hint) ) {
										selectedHints.clear();
										selectedHints.add(h);
									}
								}
							}
							startLine = i + 1;
							return re;
						}
					} catch ( Exception eaten ) {
						beep();
					}
				// 1#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5
				} else if ( line.indexOf('#', 0) > -1 ) {
					// remember in field for multiple hints in one puzzle.
					pidLine = line; // PID is short for PuzzleID
				// 1    	      3,248,200	25	 214	 10	Hidden Single                 	Hidden Single: H9=3 in box 9 (H9+3)
				} else if ( line.length() > 64
				  // hint line startsWith %-5d, to reduce pattern matching
				  && line.charAt(5) == '\t'
				  && ((ch=line.charAt(0))>='1' && ch<='9')
				  && ((ch=line.charAt(4))==' ' || (ch>='0' && ch<='9'))
				  // it looks like a hint line, so get hint text, and if the
				  // given regex matches it then load puzzle and display hint
				  && pattern.matcher(hint=line.substring(65, line.length())).matches()
				  && pidLine != null // just in case
				) {
					// howMany: skip back past the Aggregated hint lines.
					if ( hint.startsWith("Aggregate of ") ) {
						howMany = MyInteger.parse(MyStrings.word(hint, 13));
					} else {
						howMany = 0;
					}
					// read the puzzle from these 2 logLines
					final String puzzle = logLines.get(i-howMany-2) + NL
						  + logLines.get(i-howMany-1) + NL
						  // pid is pidLine upto first \t.
						  + MyStrings.upto(pidLine, '\t');
					// load the puzzle
					boolean isLoaded;
					try {
						isLoaded = grid.load(puzzle);
					} catch (Exception ex) {
						Log.teeln("BUGGER: "+Log.me()+": failed to grid.load line "+i+" of "+logFile+":"+NL+puzzle+NL);
						Log.teeTrace(ex);
						beep();
						isLoaded = false;
					}
					if ( isLoaded ) {
						// search grid again which SHOULD find the same hint.
						// If not then either a hinter has changed since your
						// logFile was produced, or some s__t just happened.
						getAllHints(false, false, false, false); // wantMore, wantSolution, logHints, printHints
						// if multiple hints are returned then select the hint
						// with a description that matches the one we just read
						// from the logFile, if exist, else the user (a techie)
						// is on there own.
						if ( filteredHints.size()>1 ) {
							assert line.length() > 64;
							String target = line.substring(65, line.length());
							assert target!=null && !target.isEmpty();
							for ( AHint h : filteredHints )
								if ( target.equals(h.toString()) ) {
									selectedHints.clear();
									selectedHints.add(h);
								}
						}
						// start the next search at the next line
						startLine = i + 1;
					}
					return re;
				}
			}
			// not found
			startLine = 0;
			pidLine = null;
			beep();
			// give the user a chance to change the regex for the next call
			// Note: can't use LogViewHintRegexDialog here coz he calls-back
			// SudokuFrame.setRegex, which calls me. sigh.
			return frame.askForString("EOL: hint regex", re);
		} catch (Throwable ex) {
			StdErr.whinge("logView error", ex);
			beep();
			return null;
		}
	}
	// these all persist between logView calls
	int startLine = 0; // reset by SudokuFrame
	private File loadedLogFile;
	private String pidLine = null;
	private ArrayList<String> logLines; // the lines in the logFile

	// when you open a puzzle it resets the logLines, to free-up memory after
	// you've viewed a log-file, without restarting SE. sigh.
	void resetLogLines() {
		loadedLogFile = null;
		logLines = null;
	}

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

	/** Ignores all wrongens, specially those religious AARRRGGGHHHH. */
	private void godMode(int value, Cell cell) throws ReligiousException {
		// ensure the given grid has a puzzleID, which is set when the Grid is
		// created, and reset when a puzzle is loaded.
		// * Invalidates all of grid's caches (but maybe not others, sigh).
		// * Not certain this is necessary, it just "feels right".
		if ( grid.pid == 0L ) {
			grid.pidReset();
			grid.hintNumberReset();
		}
		// if solutionValues is unset || it's a different puzzle
		if ( solutionValues==null || grid.pid!=solutionValuesPid ) {
			if ( grid.solutionValues == null ) {
				solver.solveASAP(grid); // sets grid.solutionValues
			}
			solutionValues = grid.solutionValues;
			solutionValuesPid = grid.pid;
		}
		// ignore all wrongens!
		if ( value != solutionValues[cell.i] ) {
			throw new ReligiousException();
		}
	}
	// need the solutionValues to validate user input against in godMode
	private int[] solutionValues = null;
	private long solutionValuesPid;

	private final class UndoList extends LinkedList<String> {
		private static final long serialVersionUID = 216354987L;
		@Override
		public void addFirst(String e) {
			if ( super.size() >= UNDOS_MAX ) {
				super.removeLast();
			}
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
		if ( value!=0 && (cell.maybes & VSHFT[value])==0
		  && (prevCell!=cell || prevValue!=value) ) {
			beep();
			prevCell = cell;
			prevValue = value;
			return;
		}
		prevCell = null;
		prevValue = 0;
		try {
			if ( godMode ) {
				godMode(value, cell);
			}
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
		} catch (ReligiousException cake) {
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
			grid.rebuild();
		} catch (UnsolvableException ex) {
			beep(); beep(); beep(); beep(); beep();
			JOptionPane.showMessageDialog(frame, "We're screwed!"
					, "Kick it in the guts!", JOptionPane.ERROR_MESSAGE);
			//grid.clear(); // overkill!
		}
	}

	/** User wants to remove/reinstate this potential cell value. */
	void maybeTyped(Cell cell, int v) {
		if ( (cell.maybes & VSHFT[v]) != 0 ) {
			// remove v from cells maybes
			if ( godMode ) {
				try {
					godMode(v, cell);
				} catch (ReligiousException cake) {
					beep();
					return;
				}
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
			if ( grid.hasMissingMaybes() ) {
				beep();
			}
		} else {
			// reinstate v to cells maybes
			if ( (cell.seesValues() & VSHFT[v]) != 0 ) {
				beep();
			} else {
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
		for ( HintNode hn : hintNodes ) {
			if ( (hint=hn.getHint()) != null ) {
				this.selectedHints.add(hint);
			}
		}
		repaintHints();
	}

	private void repaintAll() {
		if ( gridPanel == null ) { // we're in startUp
			return;
		}
		repaintHintsTree();
		repaintHints();
		gridPanel.repaint();
	}

	/** Clears the current grid - actually replaces him with a brand new one.*/
	void clearGrid() {
		if ( gridPanel != null ) {
			gridPanel.setGrid(grid = new Grid());
			clearHints();
			clearUndos();
			solutionValues = null;
			solutionValuesPid = 0L;
			frame.setTitle(ATV+"    "+BUILT);
		}
	}

	/** Make this Grid the current one. */
	void setGrid(Grid grid) {
		grid.rebuildMaybes();
		grid.rebuildAllRegionsEmptyCellCounts();
		grid.rebuildAllRegionsIndexsOfAllValues(true); // does rebuildAllRegionsContainsValues
		grid.rebuildAllRegionsIdxsOfAllValues(); // does rebuildIdxs
		gridPanel.setGrid(this.grid = grid);
		gridPanel.clearSelection(true);
		clearHints();
	}

	/** @return the current Grid. */
	Grid getGrid() {
		return gridPanel.getGrid();
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
		System.out.flush();
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
		grid.rebuildIndexes(false);
		clearHints();
	}

	/**
	 * Searches the grid for the next hint, which is displayed,
	 * else an error-message is displayed.
	 * <p>
	 * Used by solveStep and SudokuFrames btnGetNextHint and mitGetNextHint.
	 *
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
		if ( unfilteredHints == null ) {
			unfilteredHints = new ArrayList<>();
			filterAndSortHints();
		}
		// create temporary buffers for gathering a hint
		SingleHintsAccumulator accu = new SingleHintsAccumulator();
		solver.singlesSolution = null;
		// find next hint
		if ( solver.solveWithSingles(grid, accu) ) {
			if ( wantSolution ) {
				accu.add(new SolutionHint(grid, solver.singlesSolution));
			}
		} else if ( wantMore ) {
			solver.getAllHints(grid, wantMore, true, true);
		} else {
			solver.getFirstHint(grid, accu);
		}
		// move the hint from accu to unfilteredHints
		AHint hint = accu.getHint();
		if ( hint != null ) {
			unfilteredHints.add(hint);
		}
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
		final AHint hint = getNextHintImpl(isBig, false);
		if ( hint != null ) {
			if ( hint instanceof IPretendHint ) {
				addFilteredHint(hint);
				selectedHints.add(hint);
				repaintAll();
			} else {
				String filename = isBig ? "BigClue.html" : "SmallClue.html";
				String html = Html.load(this, filename);
				html = html.replace("{0}", hint.getClueHtml(isBig));
				gridPanel.setBases(hint.getBases());
				html = Html.colorIn(html);
				frame.setHintDetailArea(html);
				unfilteredHints = null;
				resetHintsFilter();
				filterAndSortHints();
			}
		}
	}

	/**
	 * Searches the grid for all the simplest available hint(s).
	 * <p>
	 * Called by {@link SudokuFrame#getAllHintsInBackground} (Shift-F5).
	 *
	 * @param wantMore are more hints wanted regardless of how hard they are,
	 *  or how long it takes to find them all; which in practice means a wait
	 *  of max 15 seconds on my ~2900 Mhz intel i7.
	 * @param wantSolution if it solvesWithSingles do you want that solution?
	 * @param logHints to the Log
	 * @param printHints to System.out
	 */
	void getAllHints(final boolean wantMore, final boolean wantSolution
			, final boolean logHints, final boolean printHints) {
		try {
			IAccumulator accu = new SingleHintsAccumulator();
			if ( grid.numSet > 80 )
				// it's already solved numbnuts!
				unfilteredHints = AHint.list(solver.SOLVED_HINT);
			else if ( wantSolution ) {
				solver.singlesSolution = null; // defeat solution cache
				if ( solver.solveWithSingles(grid, accu) ) {
					// hold down the Shift key to get the solution NOW!
					unfilteredHints = AHint.list(
							new SolutionHint(grid, solver.singlesSolution) );
				}
			} else {
				// find the next hint
				unfilteredHints = solver.getAllHints(grid, wantMore, logHints
						, printHints);
			}
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
	 * Apply the selected hints.<br>
	 * Apply a direct hint means setting the cell value.<br>
	 * Apply an indirect hint means eliminating its redPots.<br>
	 * Apply a multi hint does both; eg: Direct Naked Triple.
	 * <p>
	 * Aside: I know think hint categories are superfluous and we'd be better
	 * off if AHint (the base) was multi-hint based, with subtypes ADirectHint,
	 * AIndirectHint, AMultiHint (wafer thin), and ANonHint; each implementing
	 * the apply method appropriately for hints of this category.
	 */
	void applySelectedHints() {
		final int n = selectedHints.size();
		if ( n > 0 ) {
			final String backup = grid.toString();
			try {
				if ( grid.numSet > 80 )
					return;
				for ( AHint hint : selectedHints ) {
					undos.addFirst(grid.toString());
					hint.applyNoisily(Grid.NO_AUTOSOLVE, grid);
					if ( grid.numSet > 80 ) {
						// print the solution
						Log.teef("\nSOLUTION: %s\n", grid.toShortString());
						break;
					}
					// AggregatedHint (et al) sends grid invalid
					if ( HANDLE_INVALID_GRID ) {
						handleInvalidGrid(backup, hint);
					}
				}
				if ( THE_SETTINGS.getBoolean(Settings.isGreenFlash) )
					try {
						if ( solver.solveWithSingles(grid, null) )
							gridPanel.flashBackgroundAqua();
					} catch(Throwable eaten) {
						// Do nothing: Failed-flashers aren't worth chasing.
					}
			} catch(UnsolvableException ex) { // from hint.apply
				final PrintStream out = System.out;
				out.flush();
				out.format(NL+ex+NL);
				for ( AHint h : selectedHints )
					out.format("%s%s", h.toFullString(), NL);
				out.print(NL+"Invalid grid:"+NL+grid+NL);
				ex.printStackTrace(out);
				grid.restore(backup);
				out.print(NL+"Restored grid:"+NL+grid+NL);
				out.flush();
			}
		}
		clearHints();
		repaintAll();
	}

	/**
	 * I'm dealing with a bug in AggregatedHint (all hints there-in are valid
	 * when applied individually) which sends the grid invalid; so we check the
	 * grid's validity after applying each hint, and reject the whole selection
	 * upon the first hint that sends the grid invalid. It's then upto the user
	 * to unselect the offending hint and retry; or not select multiple hints.
	 * This is also a last line of defence against ANY invalid hint.
	 * @throws IllegalStateException if the grid is invalid.
	 */
	private void handleInvalidGrid(String backup, AHint hint) {
		if ( solver.countSolutions(grid) != 1 ) {
			// Just log it; System.out is on catch of my throw
			Log.print(NL+"Invalid (post-hint) grid:"+NL+grid+NL);
			grid.restore(backup);
			Log.print(NL+"Restored (pre-hint) grid:"+NL+grid+NL);
			// prepare
			IllegalStateException ex = new IllegalStateException(
					"Invalid after: "+AHint.toFullString(hint));
			// pucker-up
			beep(); beep(); beep();
			gridPanel.flashBackgroundPink();
			// show and frow
			displayError(ex);
			throw ex;
		}
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
		if ( grid.numSet < GRID_SIZE )
			invokeGetNextHint(wantMore, false);
	}

	private void repaintHintsTree() {
		if ( filteredHints == null ) {
			frame.clearHintsTree();
		} else {
			// select the first hint, or none if the list is empty
			final List<AHint> hints = filteredHints;
			final HintNode root = new HintsTreeBuilder().build(hints);
			// terniaries are fast enough here, at the "top" level
			final HintNode selected = hints.isEmpty() ? null
					: root.getNodeFor(hints.get(0));
			frame.setHintsTree(root, selected);
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
			grid.hintNumberReset();
			frame.setTitle(ATV+"    (clipboard)");
		} catch (IOException ex) {
			grid.copyFrom(backup);
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
		if ( id == null ) // first time
			return null;
		return loadFile(id.file, id.lineNumber);
	}

	/**
	 * Load lineNumber puzzle from the given file into the grid.
	 *
	 * @param file to load
	 * @param lineNumber to load. Used only when filename.endsWith(".mt")
	 * @return String puzzleName (PuzzleID.toString())
	 */
	final PuzzleID loadFile(File file, int lineNumber) {
		if ( file==null || !file.exists() )
			return null;
		// clear the solvers hint-cache/s before we load the new puzzle,
		// otherwise we see hints from the previous puzzle transposed onto
		// this puzzle, which is JUST PLAIN WRONG!
		if ( solver != null )
			solver.cleanUp();
		final Grid backup;
		if ( frame == null ) { // first time
			backup = null;
		} else { // clean existing before load
			backup = new Grid(this.grid);
			// clear the current grid from the display
			clearGrid();
			frame.clearHintDetailArea();
		}
		// if a puzzle is re/loaded during logView then logLines=null (release
		// bulk RAM) in which case logView then reloads logLines (slow).
		resetLogLines();
		// load the file into the grid
		if ( !GridLoader.load((IAsker)frame, grid, file, lineNumber) 
		  && backup != null ) {
			grid.copyFrom(backup);
		}
		grid.hintNumberReset();
		AHinter.hackTop1465 = grid.hackTop1465();
		return recentFiles.add(grid.source);
	}

	/**
	 * Loads the given String into the grid. The string is presumed to
	 * be in the format produced by the {@code Grid,toString()} method.
	 * @param stringData the puzzle to load.
	 */
	void loadStringIntoGrid(String stringData) {
		grid.hintNumberReset(); // do now incase load fails
		grid.source = null; // default to null, may be set by grid.load from 3rd line in stringData
		grid.load(stringData);
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
	void solveASAP() {
		clearHints();
		unfilteredHints = new ArrayList<>();
		long t0 = System.nanoTime();
		AHint solutionHint = solver.getSolutionHint(grid);
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			System.out.format("<solveASAP: %,15d%s %s", System.nanoTime()-t0, solutionHint instanceof SolutionHint, NL);
		}
		if ( solutionHint != null ) {
			unfilteredHints.add(solutionHint);
			selectedHints.add(solutionHint);
		}
		filterAndSortHints();
		repaintAll();
	}

	/**
	 * Solve this puzzle using logic to display each used solving technique.
	 */
	AHint analysePuzzle(final boolean isNoisy, final boolean isTiming) {
		try {
			// exit-early when called during shutdown (don't ask me why/how)
			if ( solver == null )
				return null;
			clearHints(true);
			unfilteredHints = new ArrayList<>();
			// we need to make a copy, else the grid underneath the GUI changes
			// so mouseMove-while-it's-analysing repaints the modified grid.
			// Then it all snaps-back when analyse finishes, which is UGLY!
			final Grid copy = new Grid(grid);
			// prepare the hinters to analyse this puzzle
			solver.prepare(copy);
			// analyse this puzzle
			final AHint hint = solver.analysePuzzle(copy, isNoisy, isTiming);
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
			final StringBuilder sb = new StringBuilder(unfilteredHints.size() * AHint.CHARS_PER_HINT);
			for ( AHint hint : unfilteredHints )
				sb.append(hint.hinter.tech.name()).append(TAB)
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
			final StringBuilder sb = new StringBuilder(selections.size() * AHint.CHARS_PER_HINT);
			for ( HintNode hintNode : selections ) {
				AHint hint = hintNode.getHint();
				if ( hint != null ) { // hints are the leaves
					sb.append(hint.hinter.tech.name()).append(TAB)
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
			sb.append(hint.hinter.tech.name()).append(TAB)
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
		// I need at-least one argument: the name of the logFile
		if ( args.length < 1 )
			StdErr.exit("GUI usage: java -jar SudokuExplainer.jar logFileName (eg SudokuExplainer.log)");
		// some classes need to know whether or not they're running in the
		// LogicalSolverTester, as apposed to the GUI or a jUnit test-case.
		// It's all a bit hackish. Please don't forget to breath between vollies
		// of cursing me. You'll really really want to. Quite a lot. Trust me.
		Run.type = Run.Type.GUI;
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

		// I do not have an internet connection because I cannot afford one.
		// The existance of an internet connection is presumed by UIManager,
		// so that if an internet connection does not exist it blocks setting
		// the LAF for upto about 5 minutes, presumably waiting for an internet
		// connection to become available, and then it eventually gives-up
		// waiting for an internet connection to magically become available and
		// works perfectly; which would be fine except that in the meantime
		// all my custom dialogs (et al) have allready been layed-out, and my
		// preferred look and feel has a larger bolder font, which, being
		// "vision impaired", I prefer. But now I can read everything it does
		// not all fit on the form, because UIManager is a MORON, who I accuse
		// of basically being spyware, until proven otherwise.
		{
			String laf = THE_SETTINGS.getLookAndFeelClassName();
			if ( !"#NONE#".equals(laf) ) {
				try {
					if ( laf == null )
						laf = UIManager.getSystemLookAndFeelClassName();
					UIManager.setLookAndFeel(laf);
				} catch(Exception ex) {
					System.out.flush();
					ex.printStackTrace(System.out);
					System.out.flush();
				}
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SudokuExplainer.getInstance();
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
	 * The first rule of god mode is we don't talk about god mode.
	 * @author Keith Corlett 2020 Jun 20
	 */
	private static final class ReligiousException extends Exception {
		private static final long serialVersionUID = 124752002380L;
		public ReligiousException() { }
	}

	// -------------------------------- cheat ---------------------------------

	/**
	 * Return green Pots from the IHinter for this 'column' in this 'grid'.
	 * The hinters get harder as you go from column 'A' on the left to column
	 * 'I' on the right, then the right mouse-button for 'a' to 'i'.
	 * These are only cheats, so if anything goes wrong I just give-up.
	 *
	 * @param chaet the Grid to search
	 * @param cheat 'A'..'I', 'a'..'i' identifies which IHinter to execute
	 * @return a Pots of the green cells to highlight, else null
	 */
	@SuppressWarnings("empty")
	public Pots cheat(final Grid chaet, final String cheat) {
		try {
			if ( "A".equals(cheat) || cheatMode || (cheatMode=frame.cheap()) );
				return solver.cheat(chaet, cheat, cheat==null, cheatMode);
		} catch (Throwable eaten) {
// I really just don't care!
//			beep();
		}
		return null;
	}
	boolean cheatMode;

}
