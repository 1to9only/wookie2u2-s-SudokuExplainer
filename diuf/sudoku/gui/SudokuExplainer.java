/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.beep;
import static diuf.sudoku.Constants.lieDown;
import diuf.sudoku.*;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.gui.RecentFiles.getRecentFiles;
import diuf.sudoku.solver.*;
import static diuf.sudoku.solver.AHint.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.checks.SolutionHint;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.*;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Constants.SB;
import diuf.sudoku.solver.hinters.DummyHinter;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.chain.AChainingHint;
import diuf.sudoku.solver.hinters.color.GEMHintBig;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.DateTimeParser;
import diuf.sudoku.utils.MyCollections;
import static diuf.sudoku.utils.MyStrings.indexOfLast;
import static diuf.sudoku.utils.MyStrings.substring;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import static java.lang.Integer.parseInt;
import java.util.EnumMap;
import java.util.regex.Matcher;

/**
 * SudokuExplainer is the "engine" underneath the GUIs SudokuFrame.
 * <p>
 * Most SudokuFrame actions delegate to me, the "engine". In turn I delegate to
 * the "action" classes, especially the LogicalSolver, which gets AHints from
 * the various IHinters. The "engine" itself does hintsTree filter, plus a few
 * trivial tasks. All non-trivial tasks are delegated to other classes. So I am
 * the business delegate, and my super-power is knowing a bit (not a lot) about
 * the GUI, and a bit (not a lot) about the actions, so that I can bring it all
 * together. In short: I am the glue.
 * <p>
 * This design keeps the GUI focused on GUI-stuff and actions (mostly) unaware
 * that there even IS a GUI. I am the only part that knows about da GUI and da
 * actions, so that the other two remain ignorant of each other. If the action
 * classes need to know about the GUI everything goes through an interface
 * exposing just the required behaviours in an abstracted form.
 * <p>
 * Note that debug-code, such as the Validator, breaks all the rules which is
 * fine because its only used by techies, not in production, so no complaints
 * if we have to change, or even remove it.
 * <p>
 * There are a few cases, such as asking the user (IAsker) and hinter reporting
 * (IReporter) where there really is no choice but for an action to be aware of
 * a UI; but such incursions are interface-abstracted; limiting the extent of
 * the action classes knowledge of the UI.
 * <p>
 * So, ideally actions remain unaware even of the existence of a UI; which is
 * not always possible, so we use interface/s to limit exposure.
 * <p>
 * When the GUI needs to know about an action it delegates the details to me,
 * by calling my abstract (generic, undetailed, simple, plain) method.
 * <p>
 * So, if a web-interface is required we implement SudokuExplainerWeb, and an
 * AbstractSudokuExplainer of commons; and I become SudokuExplainerSwing. The
 * action classes wont need to change, much. Thats the idea anyway.
 *
 * @see diuf.sudoku.gui.SudokuFrame
 * @see diuf.sudoku.solver.LogicalSolver
 */
public final class SudokuExplainer implements Closeable {

	// Some code needs to know if its in:
	// * the batch (LogicalSolverTester), or
	// * the GUI (SudokuExplainer), or
	// * a jUnit (test/.../*Test) test-case.
	// Which is very hackish. Soz!
	// NOTE: Happens early, averting read before write.
	static {
		// SudokuExplainer in used ONLY in the GUI.
		Run.setRunType(Run.Type.GUI);
	}

	/** Should apply detect and render invalid grid after hint applied? */
	private static final boolean HANDLE_INVALID_GRID = CFG.getBoolean(Config.isInvalidGridHandled, true);

	/** DEBUG and print* used in LogFollower, but available elsewhere. */
	private static final boolean DEBUG = false;
	private static void DEBUG(final String msg) {
		if(DEBUG)println(msg);
	}
	private static final PrintStream OUT = System.out;
	private static void println() {
		OUT.println();
	}
	private static void println(String msg) {
		OUT.println(msg);
	}

	private Grid grid;					// The Sudoku grid
	private LogicalSolver solver;		// The Sudoku solver
	private final SudokuFrame frame;			// The main GUI frame
	private final IAsker asker;			// an aspect of the main GUI frame
	private final SudokuGridPanel gridPanel;	// The grid

	/** is {@link #cacheHints} currently running */
	private volatile boolean isCaching;
	/** 1. {@link #cacheHints} "pre-raw" hints */
	LinkedList<AHint> hintsCache = null;
	/** 2. {@link #getAllHints} "raw" unfiltered hints */
	private LinkedList<AHint> hintsAll = null;
	/** 3. {@link #filterAndSortHints} "cooked" filtered hints */
	private LinkedList<AHint> hintsFiltered = null;
	/** cells already filtered-out; LinkedMatrixCellSet for fast contains
	 * and add, but any {@code Set<Cell>} would do, eventually */
	private final Idx filteredIndices = new Idx();
	/** potential set-cells already set (to filter-out) */
	private Pots filteredSets = new Pots();
	/** potential eliminations already eliminated (to filter-out) */
	private Pots filteredReds = new Pots();

	/** user-selected hint/s (defaulted to first, if exists). */
	private final LinkedList<AHint> selectedHints = new LinkedList<>();

	/** The recently accessed (opened or saved) files list.
	 * A self-persistent-collection, saved in dispose. */
	RecentFiles recentFiles;

	// For skip bad setValue request unless repeated
	private Cell prevCell = null;
	private int prevValue = -1;

	// god mode
	boolean god = false;

	// ------------------------------- logView --------------------------------
	// these all persist between logView calls
	int startLine = 0; // reset by SudokuFrame
	private File loadedLogFile;
	private String pidLine = null;
	private ArrayList<String> logLines; // the lines in the logFile

	/**
	 * SudokuExplainer is a singleton.
	 * @return THE instance of SudokuExplainer.
	 */
	public static SudokuExplainer getInstance() {
		if ( theInstance == null ) {
			assert Log.out != null;
			theInstance = new SudokuExplainer();
			// start generator factory running, if DiufSudoku_Generated.txt
			// is incomplete (ie does not have a puzzle for every difficulty
			// EXCEPT IDKFA, simply because they take too long to generate).
			GenPuzzleCache.staticInitialiser();
		}
		return theInstance;
	}
	private static SudokuExplainer theInstance;

	// nb: this Constructor is private, but getInstance() is public.
	// nb: Log.log is ALWAYS set before we invoke the constructor.
	private SudokuExplainer() {
		// get mostRecent file
		this.recentFiles = getRecentFiles();
		final SourceID pid = recentFiles.mostRecent(); // nullable
		// we need a grid, so we always start with an empty one.
		grid = new Grid();
		// set the frame to the empty grid
		asker = frame = new SudokuFrame(grid, this);
		gridPanel = frame.getGridPanel();
		// load the mostRecent pid into the grid and return its ID
		final SourceID loaded = loadFile(pid); // nullable
		// display the loaded SourceID in the frames title
		if ( loaded == null )
			frame.setTitle();
		else {
			frame.defaultDirectory = loaded.file.getParentFile();
			frame.setTitle(loaded.toString());
		}
		// get the solver
		Log.teeln("\nnew SudokuExplainer...");
		solver = LogicalSolverFactory.get();
		// nb: GUI cares-not about Log.MODE
		solver.reportConfiguration();
		// time to paint
		repaintHintsTree();
		frame.pack();
		frame.setBounds(getBounds(frame));
		frame.setVisible(true);
	}

	private Rectangle getBounds(final Component frame) {
		final Rectangle bounds = CFG.getBounds();
		if ( bounds != null )
			return bounds;
		// default to the right-hand half(ish) of the screen
		final Toolkit tk = frame.getToolkit();
		final java.awt.Dimension scrnSize = tk.getScreenSize();
		final GraphicsConfiguration gc = frame.getGraphicsConfiguration();
		final java.awt.Insets insets = tk.getScreenInsets(gc);
		final int screenWidth = scrnSize.width - insets.left - insets.right;
		final int screenHeight = scrnSize.height - insets.top - insets.bottom;
		final java.awt.Dimension frameSize = frame.getSize();
		final int x = screenWidth - frameSize.width + insets.left;
		return new Rectangle(x, 0, frameSize.width, screenHeight - 100);
	}

	// Copy from unfilteredHints to filteredHints, through the filter if its
	// active, and sort them by score (number of eliminations) descending then
	// by indice (toppest-leftest first)
	private void filterAndSortHints() {
		hintsFiltered = null;
		if ( hintsAll == null )
			return;
		if ( CFG.isFilteringHints() ) {
			// sort prefilter to prefer shortest path in AlsChainHints
			if ( hintsAll.size() > 1 )
				hintsAll.sort(AHint.BY_ORDER);
			// all -> filter -> filtered
			hintsFiltered = new LinkedList<>();
			if ( !hintsAll.isEmpty() )
				hintsAll.stream().filter((h)->filterAccepts(h))
						.forEachOrdered((h)->addFilteredHint(h));
		} else
			// copy them over "as is"
			hintsFiltered = new LinkedList<>(hintsAll);
	}

	/**
	 * Run only when user presses delete to "kill" hint in hintsTree TreeView.
	 * @param deadTech
	 */
	void removeHintsFrom(final Tech deadTech) {
		if ( hintsAll == null )
			return;
		hintsAll.removeIf((h)->h.hinter.getTech()==deadTech);
		filterAndSortHints();
	}

	/**
	 * Returns anything new here? ie any new set cells, or removed maybes.
	 * <p>
	 * Ergo does this hint set any cell values or remove any potential cell
	 * values that have not already been set/removed by previous hints, in
	 * the hints list that we are filtering?
	 * <p>
	 * The overall result is that filteredHints contains only useful hints.
	 * The {@link #filteredSets} and {@link filteredPots}
	 *
	 * @param hint to test
	 * @return any new sets/elims in this hint?
	 */
	@SuppressWarnings("fallthrough")
	private boolean filterAccepts(final AHint hint) {
		switch ( hint.type ) {
		case AHint.MULTI: // XColoringHintBig only, currently
			// fallthrough
		case AHint.INDIRECT:
			final Pots setPots = hint.getSetPots();
			final Pots redPots = hint.reds; // nb: NOT getRedPots, for chains
			Integer gone; // maybes that have allready been removed
			if ( setPots != null  ) {
				// XColoringHintBig sets multiple cells
				for ( java.util.Map.Entry<Integer,Integer> e : setPots.entrySet() )
					if ( (gone=filteredSets.get(e.getKey())) == null
					  || (e.getValue() & ~gone) > 0 ) // 9bits
						return true;
			} else if ( redPots != null ) {
				// and many types of hint remove many maybes from many cells
				for ( java.util.Map.Entry<Integer,Integer> e : redPots.entrySet() )
					if ( (gone=filteredReds.get(e.getKey())) == null
					  || (e.getValue() & ~gone) > 0 ) // 9bits
						return true;
			}
			// fallthrough
		case AHint.DIRECT:
			// nb: Idx.has is a fast O(1) operation.
			return hint.cell!=null && !filteredIndices.has(hint.cell.indice);
		}
		return true; // case AHint.AGGREGATE or AHint.WARNING
	}

	@SuppressWarnings("fallthrough")
	private void addFilteredHint(final AHint hint) {
		if ( hintsFiltered == null ) // BFIIK
			hintsFiltered = new LinkedList<>();
		hintsFiltered.add(hint);
		switch( hint.type ) {
		case INDIRECT:
			filteredReds.upsertAll(hint.reds);
			// fallthrough: some INDIRECT hints are actually DIRECT also!
		case DIRECT:
			if ( hint.cell != null )
				filteredIndices.add(hint.cell.indice);
			break;
		case WARNING:
			break; // no further action required
		case MULTI:
			filteredSets.upsertAll(hint.getSetPots());
			break;
		}
	}

	private void resetHintsFilter() {
		filteredReds = new Pots(); // redPots we have already done
		filteredSets = new Pots(); // setPots we have already done
		filteredIndices.clear(); // Cell values already encountered // faster to clear than create new
	}

	/** Set are we filtering-out subsequent hints with "repeat" outcomes. */
	void setFiltered(final boolean isFilteringHints) {
		CFG.setIsFilteringHints(isFilteringHints);
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
		if ( selectedHints.size() > 1 )
			// user has selected multiple hints to apply.
			repaintMultipleHints();
	}

	// We set the SudokuGridPanel attributes "directly" rather than translate
	// the selected hints into an aggregate hint and display that. The downside
	// is that the SudokuGridPanel methods I call must then be package visible.
	private void repaintMultipleHints() {
		final Pots greens = new Pots();
		final Pots reds = new Pots();
		for ( final AHint h : selectedHints ) {
			if ( h.cell != null )
				greens.put(h.cell.indice, VSHFT[h.value]);
			if ( h.type == AHint.INDIRECT )
				reds.upsertAll(h.reds);
		}
		final SudokuGridPanel gp = gridPanel;
		gp.setAquaBgIndices(greens.keySet());
		gp.setGreenPots(greens);
		gp.setRedPots(reds);
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
	 * NOTE: if you see a bug in logView it is likely just a bug in logView.
	 * Open the problematic puzzle normally and run-through solving it before
	 * you panic.
	 *
	 * @param logFile the LogicalSolverTester .log to display hints from
	 * @param regex the hint regex (regular expression) pattern
	 * @return the new/modified regex
	 */
	String logView(final File logFile, final String regex) {
		return new LogViewer(logFile, regex).view();
	}

	// ------------------------------- logFollow ------------------------------

	/**
	 * Following a log means viewing it in the GUI. Each hint in the user
	 * selected logFile is displayed in the GUI. Some hints wont be found,
	 * especially Chains, coz there are differences in how batch and GUI
	 * do stuff, especially in the chainers; strange hairy-ninja voodoo!
	 * Take No Prisoners!
	 * <pre>
	 * foreach puzzle in logFile do
	 *   if solvesWithSingles skip to end of puzzle
	 *   elseif getFirstHint is Target display it
	 *   elseif getAllHints and findTarget display it
	 * next
	 * </pre>
	 * BUG: Many chainer hints are MIA. The longer it runs the more hints are
	 * missed, yet if you skip down to that puzzle its OK.
	 *
	 * @param logFile from batch, eg top1465.d5.2023-06-20.20-38-29.log
	 * @param dummy I forget now, but this was a parameter yesterday morning
	 */
	void logFollow(final File logFile) {
		assert logFile!=null && logFile.exists(); // or you cant get here
		// invalidate all other hint lists
		cacheHintsClear();
		hintsAll = null;
		hintsFiltered = new LinkedList<>();
		new LogFollower(logFile).follow();
	}

	// ------------------------------ yada yada -------------------------------

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

	/**
	 * Do the libating.
	 */
	void libate() {
		solver.report();
	}

	/**
	 * Do god.
	 *
	 * @param value black
	 * @param cell knight
	 * @throws ReligiousException if none shall pass
	 */
	private void god(final int value, final Cell cell) throws ReligiousException {
		// ensure the given grid has a puzzleID, which is set when the Grid is
		// created, and reset when a puzzle is loaded.
		// * Invalidates all of grids caches (but maybe not others, sigh).
		// * Not certain this is necessary, it just "feels right".
		if ( grid.puzzleId == 0L ) {
			grid.puzzleIdReset();
			grid.hintNumberReset();
		}
		// if solution is unset or it is a different puzzle
		if ( solution==null || grid.puzzleId!=solutionPuzzleId ) {
			solution = grid.solution = solver.solve(grid).getValues();
			solutionPuzzleId = grid.puzzleId;
		}
		// ignore all wrongens!
		if ( value != solution[cell.indice] )
			throw new ReligiousException();
	}
	// need the solution to validate user input against in godMode
	private int[] solution = null;
	private long solutionPuzzleId;

	private final class UndoList extends LinkedList<String> {
		private static final long serialVersionUID = 216354987L;
		@Override
		public void addFirst(final String e) {
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
		public boolean add(final String e) {
			assert false : "please call addFirst directly."; // techies only
			addFirst(e);
			return true;
		}
	};

	/**
	 * Invoked only by the GUI when the user sets the value of a cell.
	 *
	 * @param cell the cell to set.
	 * @param value the value (1..9 inclusive) to set, or 0 meaning that
	 * the cells existing value is to be erased.
	 */
	void setTheCellsValue(final Cell cell, final int value) {
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
			if ( god )
				god(value, cell);
			// add an undo BEFORE we change the grid
			undos.addFirst(grid.toString());
			// change the grid
			cell.setManually(value, cell.value); // throws UnsolvableException
			// if cell cleared rebuild bloody everything
			if ( value == 0 ) {
				grid.rebuildRemoveInvalidMaybes();
				grid.rebuildMaybes();
			}
			// rebuild (the validators use places/indexes)
			grid.rebuild();
			// validate the grid
			if ( grid.isInvalidated() ) { // MIA maybes, double-ups, homeless
				whinge("Invalidity: "+grid.invalidity);
				whinge(grid.toString());
				gridPanel.flashPink();
				beep();
				undo(); // so that (to the user) value will not set
				lastDitchRebuild();
			}
			boolean needRepaintHints = (hintsFiltered != null);
			clearHints(false);
			this.selectedHints.clear();
			if ( needRepaintHints ) {
				repaintHintsTree();
				repaintHints();
			}
		} catch (ReligiousException cake) {
			beep();
		} catch (UnsolvableException ex) { // when canNotBe 0s bits
			whinge(ex.toString());
			whinge(grid.toString());
			undo(); // so that (to the user) value does not dissapear
			beep();
			lastDitchRebuild();
		}
		repaintAll();
	}

	private void lastDitchRebuild() {
		try {
			grid.rebuildRemoveWrongCellValues();
			grid.rebuildMaybes();
			grid.rebuild();
			grid.countNumSet();
		} catch (Exception ex) {
			whinge("FUBARED: lastDitchRebuild: "+ex);
			whinge(ex);
			whinge(grid.toString());
			beep();
			errMsgBox(
			 "FUBARED: lastDitchRebuild: "+ex+"\n"
			+"It looks like this puzzle is ____ed. Try reloading it.\n"
			+"If it ____s-up again try restarting Sudoku Explainer.\n"
			+"Failing that the ____er is ____ing ____ed, so email it\n"
			+"to the Klingon war fleet, or something.\n"
			+"\n"
			+"Its not my problem!"
			, "Sudoku Explainer FUBARED: lastDitchRebuild failed!"
			);
		}
	}

	/**
	 * User wants to remove/reinstate this potential cell value.
	 */
	void maybeTyped(final Cell cell, final int v) {
		if ( (cell.maybes & VSHFT[v]) > 0 ) { // 9bits
			// remove v from cells maybes
			if ( god )
				try {
					god(v, cell);
				} catch (ReligiousException eaten) {
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
				cell.canNotBeBits(VSHFT[v]);
			} catch(UnsolvableException ex) {
				frame.setHintDetailArea(ex.toString());
				beep();
				return;
			}
			if ( grid.hasMissingMaybes() )
				beep();
		} else {
			// reinstate v to cells maybes
			if ( (cell.seesValues() & VSHFT[v]) > 0 ) // 9bits
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

	/**
	 * Display the given hints which have been selected.
	 */
	void hintsSelected(final Collection<HintNode> hintNodes) {
		this.selectedHints.clear();
		AHint hint;
		for ( HintNode hn : hintNodes )
			if ( (hint=hn.getHint()) != null )
				this.selectedHints.add(hint);
		repaintHints();
	}

	private void repaintAll() {
		if ( gridPanel == null ) // we are in startUp
			return;
		repaintHintsTree();
		repaintHints();
		gridPanel.repaint();
	}

	/**
	 * Clears the current grid - actually replaces him with a brand new one.
	 */
	void clearGrid() {
		if ( gridPanel != null ) {
			gridPanel.setGrid(grid = new Grid());
			clearHints();
			clearUndos();
			solution = null;
			solutionPuzzleId = 0L;
			frame.setTitle();
		}
	}

	/**
	 * Make this Grid the current one.
	 */
	void setGrid(final Grid grid) {
		grid.rebuildMaybes();
		grid.rebuildAllRegionsEmptyCellCounts();
		grid.rebuildIndexes(true); // does rebuildAllRegionsSetCands
		grid.hiddenSetDisplayState.reset();
		gridPanel.setGrid(this.grid = grid);
		gridPanel.clearSelection(true);
		clearHints();
	}

	/**
	 * @return the current Grid.
	 */
	Grid getGrid() {
		return gridPanel.getGrid();
	}

	SourceID getGridSource() {
		final Grid g = getGrid();
		return g!=null ? g.source : null;
	}

	/**
	 * clear the hints - both the selected hint(s) and the hints lists.
	 */
	void clearHints() {
		clearHints(true);
	}

	/**
	 * clear the hints - both the selected hint(s) and the hints lists.
	 *
	 * @param if true then clear the selection else just clear the focus.
	 */
	void clearHints(final boolean isSelection) {
		if ( gridPanel == null ) // we are in startUp
			return;
//Not here!
//		hintsCache = null;
		hintsAll = null;
		hintsFiltered = null;
		resetHintsFilter();
		selectedHints.clear();
		gridPanel.clearSelection(isSelection);
		repaintAll();
	}

	/**
	 * recreate the LogicalSolver because the Config have changed.
	 */
	void recreateLogicalSolver() {
		// we must recreate the LogicalSolver in order to make it pick-up any
		// change in the Config.Preferences, specifically the wantedHinters.
		solver = LogicalSolverFactory.recreate();
		// prepare the new LogicalSolver on the premise that the old one was
		// prepared. It should not matter if the old one was not prepared.
		solver.prepare(grid);
	}

	private void displayError(final Throwable ex) {
		System.out.flush();
		ex.printStackTrace(System.out);
		System.out.flush();
		try {
			repaintAll();
		} catch (Exception t) {
			whinge(t);
		}
		frame.setHintDetailArea("<html><body><pre><font color=\"red\">"
				+ ex.toString().replace("\n", NL)
				+ "</font></pre></body></html>");
	}

	/**
	 * Validate the puzzle and the grid and display any warning/s.
	 *
	 * @return is the grid valid?
	 */
	boolean validatePuzzleAndGrid(final boolean isNoisy) {
		selectedHints.clear();
		hintsAll = new LinkedList<>();
		final AHint hint = solver.validatePuzzleAndGrid(grid, isNoisy);
		if ( hint != null ) {
			hintsAll.add(hint);
			selectedHints.add(hint);
		}
		filterAndSortHints();
		repaintAll();
		return (hint == null);
	}

	/**
	 * Rebuild the potential values in the grid.
	 */
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
	 * @param wantMore true when big (aggregate) hints are desired.
	 */
	void getNextHint(final boolean wantMore, final boolean wantSolution) {
		try {
			final AHint hint = this.getNextHintImpl(wantMore, wantSolution);
			if ( hint != null ) {
				addFilteredHint(hint);
				selectedHints.clear();
				selectedHints.add(hint);
			}
			repaintAll();
		} catch (Exception ex) {
			displayError(ex);
		}
	}

	// Called by getNextHint (above)
	private AHint getNextHintImpl(final boolean wantMore, final boolean wantSolution) {
		// find next hint
		if ( wantSolution ) {
			final IAccumulator accu = new SingleHintsAccumulator();
			if ( solver.solveWithSingles(grid, accu, false) )
				return new SolutionHint(grid, solver.singles);
		} else if ( wantMore ) {
			LinkedList<AHint> allHints = solver.getAllHints(grid, wantMore, true, true, true);
			if ( allHints!=null && !allHints.isEmpty() ) {
				final AHint hint = allHints.poll();
				if ( hint != null )
					return hint;
			}
		} else {
			final SingleHintsAccumulator accu = new SingleHintsAccumulator();
			solver.getFirstHint(grid, accu);
			final AHint hint = accu.poll();
			if ( hint != null )
				return hint;
		}
		return null;
	}

	/**
	 * Get a clue: a vague pointer towards the next hint.
	 * @param isBig If isBig is false then tell the user which pattern (Tech)
	 * to look for; else also say where to look, which is basically the whole
	 * hint for simple (ie human do-able) hint-types.
	 * <p>
	 * Called by SudokuFrame getMitGetSmallClue & getMitGetBigClue
	 *
	 * @param isBig Are big (more informative) clues desired?
	 */
	void getClue(final boolean isBig) {
		clearHints();
		if ( hintsAll == null ) {
			hintsAll = new LinkedList<>();
			filterAndSortHints();
		}
		final SingleHintsAccumulator accu = new SingleHintsAccumulator();
		solver.singles = null;
		solver.getFirstHint(grid, accu); // first hint only
		final AHint hint = accu.poll();
		if ( hint == null ) {
			Log.teef("BUGGER: No hint!");
			beep();
		} else {
			hintsAll.add(hint);
		}
		selectedHints.clear();
		if ( hint != null ) {
			if ( hint instanceof IPretendHint ) {
				addFilteredHint(hint);
				selectedHints.add(hint);
				repaintAll();
			} else {
				gridPanel.setBases(isBig ? hint.getBases() : null);
				frame.setHintDetailArea(Html.colorIn(Html.load(this
						, isBig ? "BigClue.html" : "SmallClue.html")
						.replace("{0}", hint.getClueHtml(isBig))));
				hintsAll = null;
				resetHintsFilter();
				filterAndSortHints();
			}
		}
	}

	/**
	 * Search the grid for all the simplest available hint(s).
	 * <p>
	 * Called by {@link SudokuFrame#getAllHintsInBackground} (F5 or Shift-F5).
	 * <p>
	 * Note that getAllHints now works with {@link #cacheHints} via the
	 * {@link #hintsCache}, but hint-caching may cause unforseeable problems;
	 * so there is a {@link Config#isCachingHints} to disable caching.
	 *
	 * @param wantMore are more hints wanted regardless of how hard they are,
	 *  or how long it takes to find them all; which in practice means a wait
	 *  of max 15 seconds on my ~2900 Mhz intel i7. Note that "heavy" hinters
	 *  use an internal cache, so may still return one hint per request, even
	 *  in Shift-F5, because I am too lazy to work-out how to disable caching
	 *  for-this-request-only. I doubt it can be done nicely, but I know not.
	 *  I am sure that I am not smart enough to imagine how to do it nicely.
	 * @param wantSolution if it solvesWithSingles do you want that solution?
	 * @param logHints to the Log
	 * @param printHints to System.out
	 */
	void getAllHints(final boolean wantMore, final boolean wantSolution
			, final boolean logGrid, final boolean logHints, final boolean printHints) {
		if ( isCaching )
			return;
		try {
			// check the cache
			if ( MyCollections.any(hintsCache) )
				hintsAll = hintsCache;
			else {
				// hintCache does not exist so fetch a hint now, but next time
				// we come through here there will be a hintsCache.
				// The SudokuFrame calls the cacheHints method (directly below)
				// after me, on the same daemon thread, to make the hintsCache
				// that I flick-pass above, for a "snappy" response to each
				// user-request for the next hint. It's all smoke a mirrors
				// really, and some bullshit smart algorithms (Juillerat, and
				// hobiwan), and some reasonably competent implementation, even
				// if I do say so myself.
				final IAccumulator accu = new SingleHintsAccumulator();
				if ( grid.numSet > 80 )
					hintsAll = AHint.linkedList(solver.solvedHint);
				else if ( wantSolution ) {
					if ( solver.solveWithSingles(grid, accu, false) )
						// hold down the Shift key to get the solution NOW!
						hintsAll = AHint.linkedList(new SolutionHint(grid, solver.singles));
				} else // find the next hint
					hintsAll = solver.getAllHints(grid, wantMore, logGrid, logHints, printHints);
			}
			selectedHints.clear();
			resetHintsFilter();
			filterAndSortHints();
			if ( hintsFiltered!=null && !hintsFiltered.isEmpty() )
				selectedHints.add(hintsFiltered.get(0));
			repaintAll();
		} catch (Exception ex) {
			System.out.println(grid);
			displayError(ex);
		}
	}

	void getAllHints() {
		getAllHints(false, false, false, false, false);
	}

	/**
	 * Search the grid for all the simplest available hint(s),
	 * which it adds to the {@link #hintsCache}.
	 * <p>
	 * Note that hint-caching may cause unforseeable problems; so there is a
	 * {@link Config#isCachingHints} to disable caching.
	 * <p>
	 * Called by {@link SudokuFrame#getAllHintsInBackground} (Shift-F5).
	 * <p>
	 * Note that this cacheHints method is a cut-and-paste of getAllHints.
	 * All I did was copy the grid and apply the first hint (to simulate
	 * pressing enter in the GUI) and change allHints to hintsCache.
	 *
	 * @param wantMore are more hints wanted regardless of how hard they are,
	 *  or how long it takes to find them all; which in practice means a wait
	 *  of max 15 seconds on my ~2900 Mhz intel i7. Note that "heavy" hinters
	 *  use an internal cache, so may still return one hint per request, even
	 *  in Shift-F5, because I am too lazy to work-out how to disable caching
	 *  for-this-request-only. I doubt it can be done nicely, but I know not.
	 *  I am sure that I am not smart enough to imagine how to do it nicely.
	 * @param wantSolution if it solvesWithSingles do you want that solution?
	 * @param logGrid to the Log
	 * @param logHints to the Log
	 * @param printHints to System.out
	 */
	void cacheHints(final boolean wantMore, final boolean wantSolution
			, final boolean logGrid, final boolean logHints
			, final boolean printHints) {
		if ( isCaching )
			return;
		try {
			if ( selectedHints!=null ) {
				final AHint hint = selectedHints.peek();
				if ( hint != null ) {
					isCaching = true;
					final Grid copy = new Grid(this.grid);
					copy.puzzleId = this.grid.puzzleId; // use internal caches!
					hint.applyQuitely(wantSolution, copy);
					final IAccumulator accu = new SingleHintsAccumulator();
					if ( copy.numSet > 80 )
						hintsCache = AHint.linkedList(solver.solvedHint);
					else if ( wantSolution ) {
						if ( solver.solveWithSingles(copy, accu, false) )
							// hold down the Shift key to get the solution NOW!
							hintsCache = AHint.linkedList(new SolutionHint(copy, solver.singles));
					} else // find the next hint
						hintsCache = solver.getAllHints(copy, wantMore, logGrid, logHints, printHints);
				}
			}
		} catch (Exception ex) {
			System.out.println(grid);
			displayError(ex);
		}
		isCaching = false;
	}

	void cacheHintsClear() {
		hintsCache = null;
	}

	/**
	 * Is {@link #cacheHints} currently running.
	 * Package visible for use in SudokuFrame.
	 *
	 * @return {@link #isCaching}
	 */
	boolean isCachingHints() {
		return isCaching;
	}

	/**
	 * Apply the selected hints.<br>
	 * Apply a direct hint means setting the cell value.<br>
	 * Apply an indirect hint means eliminating its redPots.<br>
	 * Apply a multi hint does both; eg: Direct Naked Triple.
	 * <p>
	 * Aside: I think hint categories are superfluous and we would be better
	 * off if AHint (the base) was multi-hint based, with subtypes ADirectHint,
	 * AIndirectHint, AMultiHint (wafer thin), and ANonHint; each implementing
	 * the apply method appropriately for hints of this category.
	 */
	void applySelectedHints() {
		final int n = selectedHints.size();
		if ( n > 0 ) {
			final String backup = grid.toString();
			try {
				if ( grid.countNumSet() > 80 )
					return;
				for ( AHint hint : selectedHints ) {
					undos.addFirst(grid.toString());
					hint.applyNoisily(Grid.NO_AUTOSOLVE, grid);
					++grid.hintNumber;
					if ( grid.numSet > 80 ) {
						// tell the solver to clean-up now
						solver.cleanUp();
						// print the solution
						Log.teef("\nSOLUTION: %s\n", grid.toShortString());
						break;
					}
					// AggregatedHint (et al) sends grid invalid
					if ( HANDLE_INVALID_GRID )
						handleInvalidGrid(backup, hint);
				}
				if ( CFG.getBoolean(Config.isGreenFlash) )
					try {
						if ( solver.solveWithSingles(grid, null, true) )
							gridPanel.flashAqua();
					} catch(Exception eaten) {
						// Do nothing: Failed-flashers are not worth chasing.
					}
			} catch(UnsolvableException ex) { // from hint.apply
				final PrintStream out = System.out;
				out.flush();
				out.format(NL+ex+NL);
				selectedHints.forEach((h)->out.format("%s%s", h.toFullString(), NL));
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
	 * I am dealing with a bug in AggregatedHint (all hints there-in are valid
	 * when applied individually) which sends the grid invalid; so we check the
	 * grids validity after applying each hint, and reject the whole selection
	 * upon the first hint that sends the grid invalid. It is then upto the user
	 * to unselect the offending hint and retry; or not select multiple hints.
	 * This is also a last line of defence against ANY invalid hint.
	 *
	 * @throws IllegalStateException if the grid is invalid.
	 */
	private void handleInvalidGrid(final String backup, final AHint hint) {
		// nb: countSolutions no longer uses logic, ie no hinters!
		if ( solver.countSolutions(grid) != 1 ) {
			// Just log it; System.out is on catch of my throw
			Log.print(NL+"Invalid (post-hint) grid:"+NL+grid+NL);
			grid.restore(backup);
			Log.print(NL+"Restored (pre-hint) grid:"+NL+grid+NL);
			// prepare
			final IllegalStateException ex = new IllegalStateException(
					"Invalid after: "+AHint.toFullString(hint));
			// pucker-up
			beep(); beep(); beep();
			gridPanel.flashPink();
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
		if ( hintsFiltered == null )
			frame.clearHintsTree();
		else {
			// select the first hint, or none if the list is empty
			final List<AHint> hints = hintsFiltered;
			final HintNode root = new HintsTreeBuilder().build(hints);
			// terniaries are fast enough here, at the "top" level
			final HintNode selected = hints.isEmpty() ? null
					: root.getNodeFor(hints.get(0));
			frame.setHintsTree(root, selected);
		}
	}

	/**
	 * Paste the clipboard into the current grid.
	 */
	void pastePuzzle() {
		// tell the LogicalSolver that we are finished with the existing puzzle.
		// nb: if we miss this the solver still has hints cached, which are all
		//     very borken, because they are not even for the current puzzle.
		solver.cleanUp();
		// we need a backup to revert to if all goes horribly wrong.
		final Grid backup = new Grid(grid);
		// clear the existing puzzle (if any) from display.
		clearGrid();
		try {
			grid.source = null;
			GridClipboard.pasteTo(grid);
			grid.hintNumberReset();
			frame.setTitle("    (clipboard)");
		} catch (IOException ex) {
			grid.copyFrom(backup);
			grid.source = backup.source;
			asker.carp(ex, "Paste");
		}
	}

	/**
	 * Copy the grid to the clipboard.
	 */
	void copyGridToClipboard() {
		grid.copyToClipboard();
	}

	/**
	 * Copy the given String to the Systems clipboard.
	 *
	 * @param s to copy
	 */
	void copyToClipboard(final String s) {
		GridClipboard.copy(s);
	}

	/**
	 * Reload the current file.
	 */
	final SourceID reloadFile() {
		return loadFile(grid.source);
	}

	/**
	 * Load the next puzzle in a .mt (MagicTour, ie multi-puzzle) file.
	 */
	final SourceID loadNextPuzzle() {
		final SourceID gs = grid.source;
		return gs!=null && gs.file!=null ? loadFile(gs.file, gs.lineNumber+1) : null;
	}

	/**
	 * Load the given SourceID.
	 */
	final SourceID loadFile(final SourceID id) {
		return id!=null ? loadFile(id.file, id.lineNumber) : null;
	}

	/**
	 * Load lineNumber puzzle from the given file into the grid.
	 *
	 * @param file to load
	 * @param lineNumber to load. Used only when filename.endsWith(".mt")
	 * @return String puzzleName (SourceID.toString())
	 */
	final SourceID loadFile(final File file, final int lineNumber) {
		cacheHintsClear();
		if ( file==null || !file.exists() )
			return null;
		// clear the solvers hint-cache/s before we load the new puzzle,
		// otherwise we see hints from the previous puzzle transposed onto
		// this puzzle, which is JUST PLAIN WRONG!
		if ( solver != null )
			solver.cleanUp();
		final Grid backup;
		if ( frame == null ) // first time
			backup = null;
		else { // clean existing before load
			backup = new Grid(this.grid);
			// clear the current grid from the display
			clearGrid();
			frame.clearHintDetailArea();
		}
		// if a puzzle is re/loaded during logView then logLines=null (release
		// bulk RAM) in which case logView then reloads logLines (slow).
		resetLogLines();
		// load the file into the grid
		try {
			if ( !GridLoader.load((IAsker)frame, grid, file, lineNumber)
			  && backup != null )
				grid.copyFrom(backup);
		} catch (IllegalArgumentException ex) {
			Log.teeln("BUGGER: loadFile: "+ex);
			if ( backup != null )
				grid.copyFrom(backup);
		}
		return recentFiles.add(grid.source);
	}

	// when you open a puzzle it resets the logLines, to free-up memory after
	// you have viewed a log-file, without restarting SE. sigh.
	void resetLogLines() {
		loadedLogFile = null;
		logLines = null;
	}

	/**
	 * Loads the given String into the grid. The string is presumed to
	 * be in the format produced by the {@code Grid,toString()} method.
	 *
	 * @param puzzle the puzzle to load.
	 */
	void loadStringIntoGrid(final String puzzle) {
		grid.hintNumberReset(); // do now incase load fails
		grid.source = null; // default to null, may be set by grid.load from 3rd line in stringData
		grid.load(puzzle);
	}

	/**
	 * Returns the next .txt File to be opened.
	 */
	File nextTxtFile(final File file) {
		final File[] txtFiles = file.getParentFile().listFiles(
			(File dir, String name) -> name.toLowerCase().endsWith(".txt"));
		final String filename = file.getName();
		final int n = txtFiles.length - 1;
		for ( int i=0; i<n; ++i )
			if ( txtFiles[i].getName().equalsIgnoreCase(filename) )
				return txtFiles[i+1];
		if ( txtFiles[n].getName().equalsIgnoreCase(filename) )
			return txtFiles[0];
		return null;
	}

	/**
	 * Save the grid to the given File.
	 */
	String saveFile(final File file) {
		try {
			IO.save(grid.toString(), file);
			grid.source = new SourceID(file);
			clearUndos();
			return recentFiles.add(grid.source).toString();
		} catch (IOException ex) {
			asker.carp(ex, "save");
			return null;
		}
	}

	/**
	 * Solve the current grid recursively (ie ASAP) to display the solution.
	 */
	void solveASAP() {
		clearHints();
		hintsAll = new LinkedList<>();
		final long t0 = System.nanoTime();
		final AHint solutionHint = solver.getSolutionHint(grid);
		if (Log.LOG_MODE >= Log.VERBOSE_2_MODE) {
			System.out.format("<solveASAP: %,15d%s %s", System.nanoTime()-t0, solutionHint instanceof SolutionHint, NL);
		}
		if ( solutionHint != null ) {
			hintsAll.add(solutionHint);
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
			// exit-early when called during shutdown (do not ask me why/how)
			if ( solver == null )
				return null;
			clearHints();
			hintsAll = new LinkedList<>();
			// we need to make a copy, else the grid underneath the GUI changes
			// so mouseMove-while-it is-analysing repaints the modified grid.
			// Then it all snaps-back when analyse finishes, which is UGLY!
			final Grid copy = new Grid(grid);
			// prepare the hinters to analyse this puzzle
			solver.prepare(copy);
			// analyse this puzzle
			final AHint hint = solver.analyse(copy, isNoisy, isTiming);
			// hint should never be null. Never say never.
			if ( hint != null ) {
				if ( hintsAll != null )
					hintsAll.add(hint);
				if ( selectedHints != null )
					selectedHints.add(hint);
			}
			filterAndSortHints();
			return hint;
		} catch (UnsolvableException ex) {
			throw ex;
		} catch (Exception ex) {
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
	void showHint(final AHint hint) {
		clearHints();
		hintsAll = new LinkedList<>();
		if ( hint != null ) {
			hintsAll.add(hint);
			selectedHints.add(hint);
		}
		filterAndSortHints();
		repaintAll();
	}

	boolean copyUnfilteredHintsListToClipboard() {
		try {
			if ( hintsAll==null || hintsAll.isEmpty() )
				return false;
			final StringBuilder sb = SB(hintsAll.size() * AHint.CHARS_PER_HINT);
			hintsAll.forEach((h) -> sb.append(h.hinter.getTechName()).append(TAB)
					.append(h.toFullString()).append(NL));
			GridClipboard.copy(sb.toString());
			return true;
		} catch (Exception ex) {
			whinge(ex);
			return false;
		}
	}

	private static void appendBranchRecursivelyTo(final StringBuilder sb
			, final HintNode hintNode) {
		final AHint hint = hintNode.getHint();
		if ( hint != null ) // leaf
			sb.append(hint.hinter.getTechName()).append(TAB)
			  .append(hint.toFullString()).append(NL);
		else
			for ( int i=0,n=hintNode.getChildCount(); i<n; ++i ) // branch
				appendBranchRecursivelyTo(sb, (HintNode)hintNode.getChildAt(i));
	}

	boolean copyToClipboard(final ArrayList<HintNode> selections) {
		if ( selections==null || selections.isEmpty() )
			return false;
		try {
			final int size = selections.size() * AHint.CHARS_PER_HINT;
			final StringBuilder sb = SB(size);
			selections.forEach((hintNode) -> {
				final AHint hint = hintNode.getHint();
				if ( hint != null ) // hints are the leaves
					sb.append(hint.hinter.getTechName()).append(TAB)
							.append(hint.toFullString()).append(NL);
				else
					appendBranchRecursivelyTo(sb, hintNode);
			});
			GridClipboard.copy(sb.toString());
			return true;
		} catch (Exception ex) {
			whinge(ex);
			return false;
		}
	}

	IHinter getWantedHinter(final Tech tech) {
		return solver.getWantedHinter(tech);
	}

	AHint getSolvedHint() {
		return solver.solvedHint;
	}

	public SudokuFrame getFrame() {
		return frame;
	}

	/**
	 * Return green Pots from the IHinter for this $column in this $grid.
	 * <p>
	 * The hinters get harder as you go from column 'A' on the left to column
	 * 'I' on the right, then the right mouse-button are encoded 'a' to 'i'.
	 * <p>
	 * Note that any and all Exception$grids are eaten. These are only cheats, so
	 * I do not care if it goes bad.
	 *
	 * @param key 'A'..'I', 'a'..'i' identifies which hinter to run
	 * @param grid the Grid to search
	 * @return a Pots of the green cells to highlight, else null.
	 *
	 */
	@SuppressWarnings("empty")
	Pots cheat(final String key, final Grid grid) {
		try {
			// (authorise OR Naked/HiddenSingle only) AND Frighten It
			return solver.cheat(cheater||(cheater=frame.cheat())?key:"A", grid);
		} catch ( Exception eaten ) {
			// I do not care
		}
		return null;
	}
	// package visible to be read/set by SudokuFrame.
	boolean cheater;

	String cheatName(final int i) {
		if ( cheatNames == null )
			cheatNames = solver.cheatNames();
		// avoid any AIOOBE$grids
		if ( i<0 || i>=cheatNames.length )
			return "";
		// nb: cheatNames[0] is also ""
		return cheatNames[i];
	}
	private String cheatNames[];

	/**
	 * Prints stackTrace of $t to stderr and returns false.
	 *
	 * @param t
	 * @return
	 */
	boolean whinge(final Throwable t) {
		return StdErr.whinge(t);
	}

	/**
	 * Prints $msg to stderr and returns false.
	 *
	 * @param msg
	 * @return
	 */
	boolean whinge(final String msg) {
		return StdErr.whinge(msg);
	}

	/**
	 * Show $msg to the GUI user in an error message box with $title.
	 *
	 * @param msg
	 * @param title
	 */
	void errMsgBox(final String msg, final String title) {
		showMessageDialog(frame, msg, title, ERROR_MESSAGE);
	}

	/**
	 * Start point of the application. No splash screen.
	 *
	 * @param args program arguments (not used)
	 */
	public static void main(final String[] args) {
		// I need at-least one argument: the name of the logFile
		if ( args.length < 1 )
			StdErr.exit("GUI usage: java -jar SudokuExplainer.jar whatever.log (eg SudokuExplainer.$YYYY-$MM-$DD.$hh-$mm-$ss.log)");
		// Prevent the GUI overwriting the .mt-file when a GUI.jar is run as if
		// it is a LogicalSolverTester jar-file;
		// ie ya built the wrong configuration ya great leapin putz!
		final File logFile = new File(new DateTimeParser().parse(args[0]));
		if ( !logFile.getAbsolutePath().toLowerCase().endsWith(".log") ) {
			StdErr.whinge("GUI requires *.log not: "+logFile.getAbsolutePath());
			return;
		}
		try {
			Log.out = new PrintStream(logFile);
		} catch (Exception t) {
			StdErr.exit("FAIL: failed to set Log.out", t);
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SudokuExplainer.getInstance();
			}
		});
	}

	/**
	 * Closes (ie saves) the recent files list.
	 */
	@Override
	public void close() throws java.io.IOException {
		if ( solver != null ) {
			try {
				solver.close();
			} catch (Exception eaten) {
				// Do nothing.
			}
			solver = null;
		}
		if ( recentFiles != null ) {
			try {
				recentFiles.close();
			} catch (Exception eaten) {
				// Do nothing.
			}
			recentFiles = null;
		}
	}

	/**
	 * The first rule of god mode is we do not talk about god mode.
	 *
	 * @author Keith Corlett 2020 Jun 20
	 */
	private static final class ReligiousException extends Exception {
		private static final long serialVersionUID = 124752002380L;
		ReligiousException() { }
	}

	/**
	 * The LogViewer class just keeps all of its methods together.
	 */
	private class LogViewer {

		private final File logFile;
		private final String regex;

		private LogViewer(File logFile, String regex) {
			this.logFile = logFile;
			this.regex = regex;
		}

		String view() {
			try {
				String line, hint;
				int howMany;
				char ch;
				// the regular expression
				final String re = MyStrings.any(regex) ? regex
								: askForString("hint regex");
				if ( re == null )
					return null;
				final Matcher matcher = Pattern.compile(re).matcher("");
				// read the logFile first time || if logFile has changed
				// if a puzzle is re/loaded during viewing then logLines=null (to
				// free up RAM), in which case logLines will be reloaded (slow).
				if ( logLines==null || !logFile.equals(loadedLogFile) ) {
					logLines = IO.slurp(logFile); // that's BIG!
					loadedLogFile = logFile;
				}
				// get the number of lines in the current logFile
				final int n = logLines.size();
				// reset at end of file, and beep to say "I wrapped"
				if ( startLine >= n ) {
					startLine = 0;
					beep();
				}
				for ( int i=startLine; i<n; ++i ) {
					line = logLines.get(i);
					// WARN: invalidity I3-9 in @Death Blossom: G2-67 (I3-9)!
					if ( line.startsWith("WARN: ")  ) {
						try {
							hint = substring(line, line.indexOf(IN)+4);
							if ( hint!=null && matcher.reset(hint).matches() ) {
								grid.load(logLines.get(i-2)+NL+logLines.get(i-1)+NL);
								// wantMore, wantSolution, logHints, printHints
								cacheHintsClear();
								getAllHints();
								if ( hintsFiltered.size() > 1 )
									for ( AHint h : hintsFiltered )
										if ( h.toString().equals(hint) ) {
											selectedHints.clear();
											selectedHints.add(h);
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
					  && (hint=substring(line, 65, line.length())) != null
					  && matcher.reset(hint).matches()
					  && pidLine != null // just in case
					) {
						// howMany: skip back past the Aggregated hint lines.
						howMany = hint.startsWith("Aggregate of ")
								? MyInteger.parse(MyStrings.word(hint, 13))
								: 0;
						// read the puzzle from these 2 logLines
						final String puzzle = logLines.get(i-howMany-2) + NL
							  + logLines.get(i-howMany-1) + NL
							  // pid is pidLine upto first \t.
							  + MyStrings.upto(pidLine, '\t');
						// Print the puzzle and the hint-line.
						println();
						println(puzzle);
						println(line);
						// load the puzzle
						boolean isLoaded;
						try {
							isLoaded = grid.load(puzzle);
						} catch (Exception ex) {
							Log.teeln("WARN: "+Log.me()+": failed to grid.load line "+i+" of "+logFile+":"+NL+puzzle+NL);
							Log.teeTrace(ex);
							beep();
							isLoaded = false;
						}
						if ( isLoaded ) {
							// search grid again which SHOULD find the same hint.
							// If not then either something has changed since your
							// logFile was produced, or s__t just happened.
							// wantMore, wantSolution, logHints, printHints
							cacheHintsClear();
							getAllHints();
							// if multiple hints are returned then select the hint
							// with a description that matches the one we just read
							// from the logFile, if exist, else the user (a techie)
							// is on there own.
							String target;
							if ( hintsFiltered.size() > 1
							  && line.length() > 64
							  && (target=line.substring(65, line.length())) != null
							  && !target.isEmpty() )
								for ( AHint h : hintsFiltered ) {
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
				// Note: cannot use LogViewHintRegexDialog here coz he calls-back
				// SudokuFrame.setRegex, which calls me. sigh.
				return frame.askForString("EOL: hint regex", re);
			} catch (IOException ex) {
				StdErr.whinge("WARN: logView error", ex);
				beep();
				return null;
			}

		}

		// cannot use LogViewHintRegexDialog coz he calls SudokuFrame.setRegex,
		// which calls logView (my caller) which calls me. An Endless loop. Sigh.
		private String askForString(String q) {
			String s = null;
			if ( (s=frame.askForString(q, s))==null || !s.isEmpty() )
				return s;
			// s isEmpty
			q = "NONE: " + q;
			do if ( (s=frame.askForString(q, s)) == null )
				return null;
			while ( s.isEmpty() );
			return s;
		}

	}

	/**
	 * The LogFollower class exists just to keep all of its s__t together,
	 * and also to be a Runnable, ie a Thread.
	 * <p>
	 * KRC 2023-06-27 This was a bitch to write! Took ages. The time-waster was
	 * a bug in Grid where Cell.skip was not reset when a puzzle is loaded into
	 * the grid, which matters not in da batch because da batch uses a new Grid
	 * foreach puzzle, where here I SudokuExplainer.this.grid.load, obviously.
	 * So that's fixed, and it now works, even with internal caches enabled.
	 * Cant believe how long it took me to workout cell.skip was hangover true.
	 * It was right there in da debugger, I just didnt look, coz I KNOW it cant
	 * be true, coz the batch works. Programming! Sigh.
	 */
	private class LogFollower implements Runnable {

		// the logFollower thread
		private Thread logFollower;
		// the logStopper thread
		private Thread logStopper;
		// milliseconds to show each hint: 256..1024 ms works pretty well.
		// Below I cant groc hints. Above is too slow. The path is long!
		// actually lieDown for half napTime + hint.getDifficulty() so that
		// harder hints are displayed for a bit longer -> more time to groc.
		// The stopper sleeps for nt*2, nt*4, nt*8 between stop-attempts.
		// Hardest hint is NestedUnary=200, so stopper outweigths follower.
		// Longest getHints is NestedUnary, taking about a second. Sigh.
		// It should stop pretty reliably. I hate multithreading. Unmathable!
		private static final int NAP_TIME   = 256;
		private static final int HALF_NAP   = NAP_TIME >> 1; // 128+diff<=200
		private static final int DUO_NAP    = NAP_TIME << 1; // 512 ms
		private static final int QUAD_NAP   = NAP_TIME << 2; // 1024 ms
		private static final int OCTO_NAP   = NAP_TIME << 3; // 2048 ms

		// true makes logFollow stop following the log
		private volatile boolean isStopped;

		private final File logFile;
		LogFollower(final File logFile) {
			this.logFile = logFile;
		}

		/**
		 * follow starts a new logFollower thread, and also a new logStopper
		 * thread, with a MsgBox, which is system modal, so the GUI is unusable
		 * (and also unmovable, which is a pain in the ass) whilst following.
		 */
		void follow() {
			// ==== Follower Thread ====
			logFollower = new Thread(this, "logFollower");
			logFollower.setDaemon(true);
			logFollower.start();
			// ==== Stopper Thread ====
			logStopper = new Thread(new LogStopper(), "logStopper");
			logStopper.setDaemon(true);
			logStopper.start();
		}

		/**
		 * Run implements LogFollower. Thus he ran, upon start.
		 */
		@Override
		public void run() {
			// I am the batch. Be the batch. Humina humina.
			Run.Type preRunType = Run.setRunType(Run.Type.Batch);
//			// hinters where useCache was true before disableInternalCaches
//			Collection<IHinter> cachers = null;
			try ( final BufferedReader reader = new BufferedReader(new FileReader(logFile)) ) {
				Tech tech;
				IHinter hinter;
				AHint hint;
				String line, hintLine, targetHint, label, techName;
//				int puzzleNumber;
//				// defeat caching to fetch all hints (not just the first one).
//				cachers = solver.disableInternalCaches();
				// get a map of all hinters (sans validators)
				final EnumMap<Tech, IHinter> hinters = solver.getHintersMap();
				final String[] gridLines = new String[2];
				long puzzleId = 0L; // puzzleId
				// nb: solveWithSingles requires SingleHintsAccumulator
				final SingleHintsAccumulator sacu = new SingleHintsAccumulator();
				final LinkedList<AHint> list = new LinkedList<>();
				final HintsAccumulator accu = new HintsAccumulator(list);
				// beginningOfPuzzleRegularExpression "1/1#" .. "63/1465#"
				final Matcher bopRE = Pattern.compile("^\\d+/\\d+#.*").matcher("");
				// endOfPuzzleRegularExpression: "     1,805,873,600" .. "   140,404,100,500"
				final Matcher eopRE = Pattern.compile("^ {1,11}[\\d,]{0,12}\\d\\d\\d$").matcher("");
				// foreach puzzle in logFile do
				PARSER: while ( (line=reader.readLine()) != null ) {
					// skip to beginning of puzzle (typically one skip)
					if ( !bopRE.reset(line).matches() )	 // 1/1#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
						continue;
					gridLines[0] = reader.readLine();	 // 7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5
					gridLines[1] = reader.readLine();	 // ,1589,568,1269,13569,23569,,1235,123,14569,,456,1469,,34569,156,,13,1456,145,,1246,156,,12567,1257,,2489,4789,2478,,1689,469,,1247,12478,3458,,4578,148,,34,1578,,1478,234589,4589,,489,389,,258,245,,124568,14578,245678,,568,256,,1247,12478,1258,,2578,2789,,259,1278,,1278,2468,478,,2678,68,,278,2347,
					hintLine = reader.readLine();		 // 1    	      6,087,500	25	 214	  1	HiddenSingle                  	HiddenSingle: H9=3 in box 9 (H9+3)
					reader.readLine();					 // blank line
					// the targetHints toFullString()
					targetHint = hintLine.substring(65); // HiddenSingle: H9=3 in box 9 (H9+3)
					// stop when stopped (has been a problem , can you tell?)
					if(isStopped()){stopNow(); break;}
					// for DEBUG messages
					label = line+" "+targetHint;
					// load the puzzle into SudokuExplainer.this.grid
					grid.load(gridLines);
					if ( line.startsWith("1/") ) {
						puzzleId = grid.puzzleId; // fool the caches!
//						puzzleNumber = parsePuzzleNumber(line); // 1 in "/1#"
						frame.setTitle(grid.sourceShort()); // 1#top1465.d5.mt
					} else {
						grid.puzzleId = puzzleId; // fool the caches!
					}
					// these grid fields appear in getAllHints log messages
					grid.hintNumber = parseHintNumber(line);
					grid.source = SourceID.parse(line);
					if(isStopped()){stopNow(); break;}
					// if solveWithSingles skip to end of puzzle
					if ( solver.solveWithSingles(grid, sacu, false) ) {
						frame.setCurrentHint(new SolutionHint(grid, solver.singles), false); // repaints
						gridPanel.flash(Color.CYAN, 350);
						while ( (line=reader.readLine()) != null )
							if ( eopRE.reset(line).matches() )
								continue PARSER;
					}
					if(isStopped()){stopNow(); break;}
					// "HiddenSingle" in 1    	      6,087,500	25	 214	  1	HiddenSingle                  	HiddenSingle: H9=3 in box 9 (H9+3)
					techName = parseTechName(hintLine);
					if ( (tech=Enum.valueOf(Tech.class, techName)) == null ) {
						hint = new HintNotFoundHint(null, targetHint);
						if(DEBUG)DEBUG("#NoTech "+techName+" in "+hintLine);
					} else if ( (hinter=hinters.get(tech)) == null ) {
						hint = new HintNotFoundHint(null, targetHint);
						if(DEBUG)DEBUG("#NoHinter "+tech+" in "+hintLine);
					} else {
						if(isStopped()){stopNow(); break;}
						list.clear(); // just in case
						if ( !hinter.findHints(grid, accu) ) { //populates list
							hint = new HintNotFoundHint(null, targetHint);
							if(DEBUG)DEBUG("#Failed: "+hinter+": "+label);
						} else {
							if(isStopped()){stopNow(); break;}
							if ( (hint=find(list, targetHint)) == null ) {
								if(isStopped()){stopNow(); break;}
								// last resort: otherHint with the same pots, by
								// Strings, which is RAM hungry but more flexible,
								// so usable as a plan-B only. If all byString it
								// would run like a three legged dog. 2048*~40 is
								// too big, which matters not coz I am not used!
								if ( (hint=planB(list, targetHint)) == null ) {
									hint = new HintNotFoundHint(list, targetHint);
									if(DEBUG)DEBUG("#NotFound: "+hinter+": "+label);
								}
							}
						}
					}
					if(isStopped()){stopNow(); break;}
					// display it
					hintsFiltered.clear();
					addFilteredHint(hint);
					selectedHints.clear();
					selectedHints.add(hint);
					frame.setCurrentHint(hint, false); // repaints
					repaintAll();
					lieDown(HALF_NAP + hint.getDifficulty());
					if(isStopped()){stopNow(); break;}
				}
			} catch (Exception ex) { // IO, IllegalState, or whatever
				try{stopNow();}catch(Exception eaten){}
				Log.whinge("logFollower", ex);
			} finally {
				Run.setRunType(preRunType);
//				if ( solver!=null && cachers!=null )
//					solver.restoreInternalCaches(cachers);
			}
		}

		// shutdown now
		private void stopNow() {
			setIsStopped(true);
			if ( logStopper != null )
				logStopper.interrupt();
			logFollower = null;
		}

		// find by toString (not toFullString, sigh)
		private AHint findByTS(final Iterable<AHint> hints, final String target) {
			final String t = target.substring(0, indexOfLast(target, '(')-1);
			for ( AHint hint : hints )
				if ( hint!=null && t.equals(hint.toString()) )
					return hint;
			return null;
		}

		private AHint findByReds(final Iterable<AHint> hints, final Pots target) {
			for ( AHint hint : hints )
				if ( hint!=null && hint.reds!=null && hint.reds.equals(target) )
					return hint;
			return null;
		}

		private AHint findBySets(final Iterable<AHint> hints, final Pots target) {
			final String s = target.toString().replace("-", "+");
			for ( AHint hint : hints )
				if ( hint instanceof AChainingHint ) {
					Pots r = hint.getResults();
					if ( r!=null && r.anyOns() && s.equals(r.toString()) )
						return hint;
				} else if ( hint instanceof GEMHintBig
				  && target.equals(((GEMHintBig)hint).getSetPots()) )
					return hint;
			return null;
		}

		// find target in hints
		private AHint find(final Collection<AHint> hints, final String target) {
			AHint hint;
			if ( (hint=findByTS(hints, target)) != null )
				return hint;
			final String s = parsePots(target);
			final Pots pots = Pots.parse(grid, s);
			if ( s.indexOf('+') > -1 )
				return findBySets(hints, pots);
			return findByReds(hints, pots); // may be null
		}

		// turn the hints into one big string, find the first occurrence of
		// target.reds in that string and read the otherHint from that line,
		// which we then seek in hints, to find any hint that eliminates the
		// same s__t as my hint. A Three Ringed Circitous! ~ksh ninja.
		private AHint planB(final Collection<AHint> hints, final String target) {
			final String hintsStr = AHint.toString(hints, 2048);
			final int j = hintsStr.indexOf(parsePots(target));
			if ( j < 0 )
				return null;
			final String otherHint = MyStrings.lineOf(hintsStr, j);
			return find(hints, otherHint);
		}

		// return "A4-1" from "AHint: whatever (A4-1)"
		private String parsePots(final String hint) {
			return hint.substring(indexOfLast(hint, '(')+1, hint.length()-1);
		}

		private String parseTechName(final String hintLine) {
			return hintLine.substring(34, 64).trim();
		}

		private int parseHintNumber(final String line) {
			return parseInt(line.substring(0, line.indexOf('/')));
		}

		private int parsePuzzleNumber(final String line) {
			return parseInt(line.substring(line.indexOf('/')+1, line.indexOf('#')));
		}

		// WARN: solver==null means that SudokuExplainer is closed.
		// ALWAYS use the getter/setter, or it goes wrong. Noncomprendez!
		private boolean isStopped() {
			return isStopped || solver==null;
		}

		// Set isFollowerStopped, a volatile local field, to b.
		// ALWAYS use the getter/setter, or it goes screwie. Noncomprendez!
		private void setIsStopped(final boolean stop) {
			isStopped = stop;
		}

		private class LogStopper implements Runnable {
			@Override
			public void run() {
				// block until MsgBox closes, or interrupted
				frame.msgBox("Press OK to stop following"
						, "Stop logFollower");
				setIsStopped(true);
				// 512 ms (longer than halfNap+diff and 99.99% getHints)
				lieDown(DUO_NAP);
				if ( logFollower!=null && logFollower.isAlive() ) {
					// persistent little ____er aintcha
					setIsStopped(true);
					logFollower.interrupt(); // really stop, including modals
					// 1024 ms (longer than everthing, except possibly a very
					//          slow NestedUnary)
					lieDown(QUAD_NAP);
					if ( logFollower!=null && logFollower.isAlive() ) {
						// I see you jimmy
						setIsStopped(true);
						solver = null;
						logFollower.interrupt();
						setIsStopped(true);
						solver = null; // its DOA now whatever happens
						// 2048 ms (longer than even a very slow NestedUnary)
						lieDown(OCTO_NAP);
						if ( logFollower!=null && logFollower.isAlive() )
							// hjya! ~thePig (Respect)
							threadStop(logFollower);
					}
				}
				logStopper = null;
			}
			// a method so that SuppressWarnings targets ONLY Thread.stop
			@SuppressWarnings("deprecation")
			private void threadStop(final Thread thread) {
				thread.stop();
			}
		}

		/**
		 * HintNotFoundHint is used only in {@link #logFollow}.
		 * <p>
		 * Q: Why are inner classes of inner classes not allowed to be static?
		 *    Seems wrong to me. Static inners eliminate leaks, which is right,
		 *    so this feels like rape. Yet another thing that my tiny mind...
		 * A: ?
		 */
		private class HintNotFoundHint extends AWarningHint implements IPretendHint {
			private final String numHints;
			private final String hints;
			private final String targetHint;
			HintNotFoundHint(final Collection<AHint> hints, final String targetHint) {
				super(null, new DummyHinter());
				this.numHints = hints==null ? "FAILED" : ""+hints.size();
				// first 6 only (ignoring occassional MANY)
				this.hints = (hints==null ? "" : AHint.toString(hints, 6));
				this.targetHint = targetHint;
			}
			@Override
			protected String toHtmlImpl() {
				beep();
				return ("<html><body>"+NL
					+"<h2>MIA Hint</h2>"+NL
					+"<pre>"+NL
					+"Target: "+targetHint+NL
					+"In: "+numHints+" hints"+NL
					+hints+NL
					+"</pre>"+NL
					+"</body></html>");
			}
			@Override
			protected StringBuilder toStringImpl() {
				return SB(64).append("HintNotFoundHint: hint not found");
			}
			@Override
			public int getDifficulty() {
				return 250; // for lieDown time
			}
		}

	}

}
