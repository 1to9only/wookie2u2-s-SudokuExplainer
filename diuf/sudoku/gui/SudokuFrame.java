/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.*;
import diuf.sudoku.Build;
import diuf.sudoku.Grid;
import diuf.sudoku.GridClipboard;
import diuf.sudoku.Regions;
import diuf.sudoku.SourceID;
import diuf.sudoku.Run;
import diuf.sudoku.Config;
import static diuf.sudoku.Config.*;
import diuf.sudoku.Tech;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.EmptyHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.als.AlsHelper;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.IAsker;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.awt.*;
import static diuf.sudoku.gui.Event.*;
import diuf.sudoku.utils.MyClipboard;
import static diuf.sudoku.utils.MyFile.nameEndsWith;
import static diuf.sudoku.utils.StringPrintWriter.stackTraceOf;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import static java.awt.event.MouseEvent.BUTTON1;
import static java.awt.event.MouseEvent.BUTTON2;
import static java.awt.event.MouseEvent.BUTTON3;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import static javax.swing.JOptionPane.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.tree.*;

/**
 * The SudokuFrame is the main window of the application.
 * <p>
 * Pretty much all the actions are delegated to the {@link SudokuExplainer}
 * class. The GUI handles files itself because it is easier to display errors
 * from within the GUI.
 * <p>
 * <b>Programmers Hidden GUI Features</b><br>
 * I thought I should write a list of all the "handy stuff" I built into the GUI
 * while debugging this project and playing with Sudoku puzzles, so some handy
 * features are:<ul>
 * <li>You can Alt-right-click on a cell to set it is background grey (move the
 * mouse to another cell and you will see it), and Alt-right-click again to remove
 * it (ie a toggle). This allows you to highlight cells in a hint, which is
 * pretty handy when you have a log entry describing the hint, but the current
 * version of the software does not produce the damn hint any longer, and thats
 * why you want to look at the damn hint, which you cannot see. Sigh.
 * <li>You can select hint/s in the hintsTree and then right-click to copy all
 * selected hints toFullString to the clipboard, which is handy. Do not forget to
 * copy the grid as well. And now they even come with there Tech.name().
 * <li>You can Ctrl-right-click in the hintsTree to copy all UNFILTERED hints to
 * the clipboard. Now includes Tech.name(). Do not forget the grid!
 * <li>You can right-click in the hintDetailPanel to copy the hint-HTML to the
 * clipboard, so you can paste it into a .html file and view it your browser,
 * which I find handy because my browser enables me to enlarge the font.
 * <li>Good luck with A*E, and may all of your camels be well watered.
 * </ul>
 */
@SuppressWarnings("Convert2Lambda") // I treat warnings as errors, for consistency, and replacing inner classes with lambda expressions is more trouble than it is worth (in my humble opinion), so this warning message should ONLY apply to new projects, not pre-existing code-bases, but Netbeans authors lack the intelligence to see that.
public final class SudokuFrame extends JFrame implements IAsker {

	private static final long serialVersionUID = 8247189707924329043L;

	private static final Font DIALOG_BOLD_12 = new Font("Dialog", Font.BOLD, 12);

	// the clearHintsTree method may happen often, it depends on the user.
	private static final List<AHint> EMPTY_HINTS_LIST = Collections.emptyList();
	private static final HintNode EMPTY_HINTS_TREE = new HintsTreeBuilder().build(EMPTY_HINTS_LIST);

	private static final Pattern CELL_ID_PATTERN = Pattern.compile("[A-I][1-9]");
	private static final Pattern CELL_INDICE_PATTERN = Pattern.compile("\\d\\d?");
	private static final Pattern ALS_ID_PATTERN = Pattern.compile("[a-z][a-z]?");
	private static final Pattern REGION_LABEL_PATTERN = Pattern.compile("box \\d|row \\d|col [A-I]");

	private static final DecimalFormat RATING = new DecimalFormat("#,##0");

	// I suspect that 128 is bigger than the biggest hint, but now it is
	// bounded so that it cannot run away towards infinity and break s__t.
	// If 128 proves too small then double it again. There is no problem
	// with it being bigger (within reason) it just MUST be bounded.
	private final static int MAX_VIEWS = 128;
	private static final String[] VIEWS = new String[MAX_VIEWS];
	static {
		for ( int i=0; i<MAX_VIEWS; ++i )
			VIEWS[i] = "View " + (i + 1);
	}

	// EmptyHint should flash past when tree cleared before hint-search.
	private static final TreeModel EMPTY_TREE_MODEL
			= new DefaultTreeModel(new HintNode(new EmptyHint()));

	final static Image createImage(final String path) {
		return createImageIcon(path).getImage();
	}

	final static ImageIcon createImageIcon(final String path) {
		java.net.URL url = SudokuFrame.class.getResource(path);
		if ( url == null ) {
			StdErr.whinge("createImageIcon: Resource not found: " + path);
			return null;
		}
		return new ImageIcon(url);
	}

	/**
	 * SudokuExplainer is the "business delegate" (engine) underneath this GUI.
	 */
	private final SudokuExplainer engine;

	/**
	 * SudokuGridPanel displays the current Sudoku puzzle in a 9*9 grid.
	 */
	private final SudokuGridPanel gridPanel;

	/**
	 * The GenerateDialog allows the user to generate Sudoku puzzles.
	 */
	private GenerateDialog generateDialog; // Generate Sudoku Puzzles Dialog

	/**
	 * The TechSelectDialog allows the user to un/want Sudoku Solving
	 * Tech(niques).
	 */
	private TechSelectDialog techSelectDialog; // Solving Techniques Configuration Dialog

	/**
	 * The RecentFilesDialog allows the user to open a recent file.
	 */
	private RecentFilesDialog recentFilesDialog; // Recently Used Files Dialog

	private JFrame dummyFrameKnife; // I have no idea, but it works!
	private JPanel contentPane, viewSelectPanel, hintsTreePanel, northPanel
		, sudokuGridPanelHolder, hintDetailPanel, buttonsPane, buttonsPanel
		, hintsSouthPanel, hintsSouthWestPanel, ratingPanel
		, disabledTechsWarnPanel;
	private JScrollPane hintDetailsScrollPane, hintsTreeScrollpane;
	private JTree hintsTree;
	private JEditorPane hintDetailPane;
	private String hintDetailHtml; // contents of hintDetailArea because when
	// you get it back out of the JEditorPane it is been wrapped, badly, and we
	// want to copy it to the clipboard so that it can be re-used, or whatever.
	private JButton btnGetAllHints, btnSolveStep, btnGetNextHint, btnValidate
		, btnApplyHint;
	private JCheckBox chkFilterHints;
	private JLabel lblUseCache;
	private JComboBox<String> cmbViewSelector;
	private JLabel lblPuzzleRating, lblDisabledTechsWarning;

	// The menus
	private JMenuBar menuBar;
	// File
	private JMenu fileMenu;
	private JMenuItem mitNew, mitGenerate, mitRecentFiles, mitLoad, mitReload
		, mitLoadNext, mitSave, mitQuit;
	// Edit
	private JMenu editMenu;
	private JMenuItem mitCopy, mitPaste, mitClear;
	// Tools
	private JMenu toolsMenu;
	private JMenuItem mitResetPotentialValues, mitClearHints, mitSolveStep
		, mitSolveStepBig, mitGetNextHint, mitApplyHint, mitGetAllHints
		, mitGetAllHintsMore, mitGetClueSmall, mitGetClueBig, mitCheckValidity
		, mitSolve, mitAnalyse, mitAnalyseVerbose, mitAnalyseTiming, mitLogView
		, mitLogFollow, mitLibate;
	// Options
	private JMenu optionsMenu; // a sub-menu under optionsMenu
	private JMenuItem mitSelectTechniques, mitSaveConfig;
	private JCheckBoxMenuItem mitFilterHints, mitCacheHints, mitShowMaybes
		, mitGreenFlash, mitAntialiasing, mitHacky, mitGod;
	// Help
	private JMenu helpMenu;
	private JMenuItem mitShowWelcome, mitAbout;

	private AHint currHint; // the selected hint, nullable
	private int viewCount, viewNum = -1; // a hint can have >1 "views" in grid

	private File logViewFile = null;
	private String regex;

	private JLabel lblCheatName;

	/**
	 * Does Frame cache hints? Stored in {@link Config#isCachingHints}.
	 * <p>
	 * I use {@link #cacheHints} to cache hints before {@link #getAllHints} is
	 * run by the user, to (mostly) make the GUI snappy when you step through
	 * solving a puzzle, by just repeatedly pressing enter. NestedUnary is the
	 * only hinter that I use which is slower than me, everybody else finds the
	 * next hint before I grock the current one.
	 * <p>
	 * I know not if this is always true, but my puzzles (top1465) start with
	 * an easy hint. The only way to start with a hard-hint is save maybes as
	 * per test-cases. Interesting. I wonder if there is an explanation and/or
	 * name for this phenomenon? Anyway its convenient for caching, because the
	 * priming read is superfluous.
	 */
	private boolean useCache = CFG.getBoolean(Config.isCachingHints, true);

	/**
	 * The directory which the open-file and save-file dialogs start in.
	 */
	File defaultDirectory;

	/** Is {@link #getAllHintsInBackground} currently running? */
	private volatile boolean isGettingAllHintsInBackground = false;
	/** Should {@link #getAllHintsInBackground} logHints to the Log */
	private final boolean logHints = true;
	/** Should {@link #getAllHintsInBackground} printHints to System.out */
	private final boolean printHints = true;

	/**
	 * The Constructor. Note that it is only package visible, for use only by the
	 * SudokuExplainer (controller) to create the view (me). The model is the
	 * Grid class, and it is assorted cheese. We pass around instances of the
	 * model (grids) instead of having the GUI and controller lock-onto an
	 * instance of the model, which is the more usual approach.
	 */
	SudokuFrame(final Grid grid, final SudokuExplainer engine) {
		super();
		this.engine = engine;
		this.gridPanel = new SudokuGridPanel(grid, engine, this);
		initialise();
		resetViewSelector();
		showWelcomeHtml();
		setIconImage(createImage("Icon_Sudoku.gif"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		// inform user if any Sudoku Solving Techniques are disabled
		disabledTechsWarnPanel.setVisible(refreshDisabledRulesWarning());
		AutoBusy.addFullAutoBusy(this);
		SwingUtilities.invokeLater(() -> {
			if(useCache) updateHintsCache();
		});
		SwingUtilities.invokeLater(()->gridPanel.requestFocusInWindow());
	}

	private void initialise() {
		this.setTitle();
		this.setJMenuBar(getMyMainMenuBar());
		this.setContentPane(getJContentPane());
		setDropTargetToRecievePlainTextString();
	}

	@Override
	public void setTitle(final String title) {
		super.setTitle(Build.ATV+"    "+title);
	}

	void setTitle(final SourceID source) {
		if ( source != null )
			setTitle(source.toString());
	}

	void setTitle() {
		super.setTitle(Build.ATV+"    "+Build.BUILT);
	}

	private void setDropTargetToRecievePlainTextString() {
		this.setDropTarget(new DropTarget(gridPanel, new DropTargetAdapter() {
			@Override
			public void drop(final DropTargetDropEvent dtde) {
				try {
					final String stringData = GridClipboard.read(dtde);
					//System.out.println("SudokuFrame.drop: plain/text;java.lang.String:\n"+stringData);
					engine.loadStringIntoGrid(stringData);
					gridPanel.repaint();
					setTitle("(dropped in)");
					hintDetailPane.setText("Sudoku dropped!");
				} catch (UnsupportedFlavorException | IOException ex) {
					displayError(ex);
				}
			}
		}));
	}

	/**
	 * Display Welcome.html (Sudoku Explainers help) in the hint-details area.
	 */
	final void showWelcomeHtml() {
		engine.clearHints();
		setHintDetailArea(Html.load(this, "Welcome.html", true, false)); // NO cache
	}

	/**
	 * Sets the contents of the hintsTree JTree control.
	 */
	void setHintsTree(final HintNode root, final HintNode selected) {
		getHintsTree();
		hintsTree.setEnabled(false);
		hintsTree.setModel(new DefaultTreeModel(root));
		// Dis/enable the Filter checkbox and menu item.
		chkFilterHints.setSelected(CFG.isFilteringHints());
		chkFilterHints.setEnabled(true);
		mitFilterHints.setSelected(chkFilterHints.isSelected());
		mitFilterHints.setEnabled(chkFilterHints.isEnabled());
		// Select the selected node, if any
		if ( selected != null )
			hintsTree.setSelectionPath(new TreePath(selected.getPath()));
		hintsTree.setEnabled(true);
	}

	/**
	 * Clears the contents of the hintsTree JTree control.
	 */
	void clearHintsTree() {
		setHintsTree(EMPTY_HINTS_TREE, null);
	}

	/**
	 * Repaints the SudokuGridPanel with the given hint.
	 */
	private static void repaintHint(final AHint h, final SudokuGridPanel p, final int viewNum) {
		p.clearSelection(true);
		if ( h == null )
			clear(p);
		else
			display(h, p, viewNum);
		p.repaint();
	}

	/**
	 * Repaints the SudokuGridPanel with the current Hint.
	 */
	private void repaintHint() {
		repaintHint(currHint, gridPanel, viewNum);
	}

	private static void clear(final SudokuGridPanel p) {
		p.setResults(null);
		p.setSetPots(null);
		p.setSetPotsColor(-1); // meaning none
		p.setPinkRegions(null);
		p.setAquaBgIndices  (null);
		p.setPinkBgIndices  (null);
		p.setRedBgIndices   (null);
		p.setGreenBgIndices (null);
		p.setOrangeBgIndices(null);
		p.setBlueBgIndices  (null);
		p.setYellowBgIndices(null);
		p.setGreenPots (null);
		p.setRedPots   (null);
		p.setOrangePots(null);
		p.setBluePots  (null);
		p.setYellowPots(null);
		p.setPurplePots(null);
		p.setBrownPots (null);
		p.setAlss(null);
		p.setLinks(null);
		p.setBases(null);
		p.setCovers(null);
		p.setSupers(null);
		p.setSubs(null);
	}

	private static void display(final AHint h, final SudokuGridPanel p, final int viewNum) {
		p.setResults(h.getResults());
		p.setSetPots(h.getSetPots());
		p.setSetPotsColor(h.getSetPotsColor());
		p.setPinkRegions(h.getPinkRegions()); // null OK
		p.setAquaBgIndices  (h.getAquaBgIndices(viewNum)); // null OK
		p.setPinkBgIndices  (h.getPinkBgIndices(viewNum)); // null OK
		if ( h instanceof AWarningHint )
			p.setRedBgIndices(((AWarningHint) h).getRedBgIndices());
		p.setGreenBgIndices (h.getGreenBgIndices(viewNum)); // null OK
		p.setOrangeBgIndices(h.getOrangeBgIndices(viewNum)); // null OK
		p.setBlueBgIndices  (h.getBlueBgIndices(viewNum)); // null OK
		p.setYellowBgIndices(h.getYellowBgIndices(viewNum)); // null OK
		p.setGreenPots (h.getGreenPots(viewNum));
		p.setRedPots   (h.getRedPots(viewNum));
		p.setOrangePots(h.getOrangePots(viewNum));
		p.setBluePots  (h.getBluePots(p.getGrid(), viewNum));
		p.setYellowPots(h.getYellowPots());
		p.setPurplePots(h.getPurplePots());
		p.setBrownPots (h.getBrownPots());
		p.setAlss(h.getAlss());
		// rendered as brown arrows
		p.setLinks(h.getLinks(viewNum));
		p.setBases(h.getBases());
		p.setCovers(h.getCovers());
		p.setSupers(h.getSupers());
		p.setSubs(h.getSubs());
	}

	private int bounded(final int vc) { // viewCount
		if ( vc < 1 ) {
			return 1;
		}
		if ( vc > MAX_VIEWS ) {
			// this goes to StdErr, to tell the developer we have a problem.
			engine.whinge("SudokuFrame: viewCount=" + vc + " > MAX_VIEWS=" + MAX_VIEWS);
			return MAX_VIEWS;
		}
		return vc;
	}

	/**
	 * Displays the given hint.
	 *
	 * @param hint
	 * @param isApplicable
	 */
	public void setCurrentHint(final AHint hint, final boolean isApplicable) {
		this.currHint = hint;
		btnApplyHint.setEnabled(isApplicable);
		mitApplyHint.setEnabled(isApplicable);
		viewNum = 0;
		if ( hint == null ) {
			viewCount = 1;
			resetViewSelector();
			gridPanel.setBases(null);
			gridPanel.setLinks(null);
		} else {
			// Select view
			viewCount = bounded(hint.getViewCount());
			resetViewSelector();
			// Set explanations
			setHintDetailArea(hint.toHtml());
			// NB: AnalysisHint is "actual" despite not being an actual hint.
			if ( hint instanceof IPretendHint )
				lblPuzzleRating.setText("    0");
			else
				lblPuzzleRating.setText(RATING.format(hint.getDifficulty()));
		}
		repaintHint();
		this.repaint();
	}

	private void resetViewSelector() {
		cmbViewSelector.setEnabled(false);
		cmbViewSelector.removeAllItems();
		for ( int i=0; i<viewCount; ++i )
			cmbViewSelector.addItem(VIEWS[i]);
		cmbViewSelector.setSelectedIndex(viewNum);
		cmbViewSelector.setEnabled(viewCount > 1);
	}

	private final class HintsTreeCellRenderer implements TreeCellRenderer {

		// this Renderer "wraps" a DefaultTreeCellRenderer to handle HintNodes
		private final DefaultTreeCellRenderer DTCR = new DefaultTreeCellRenderer();

		HintsTreeCellRenderer() {
			DTCR.setLeafIcon(createImageIcon("Icon_Light.gif"));
		}

		@Override
		public Component getTreeCellRendererComponent(final JTree tree
				, final Object value, final boolean selected
				, boolean expanded, boolean leaf, final int row
				, final boolean hasFocus) {
			if ( value instanceof HintNode ) {
				final HintNode n = (HintNode)value;
				final boolean isEmpty = !n.hasHint() && n.getChildCount()==0;
				expanded |= isEmpty;
				leaf &= !isEmpty;
			}
			return DTCR.getTreeCellRendererComponent(tree, value, selected
					, expanded, leaf, row, hasFocus);
		}
	}

	private JPanel getJContentPane() {
		if ( contentPane == null ) {
			final JPanel jp = new JPanel(new BorderLayout());
			jp.add(getNorthPanel(), BorderLayout.NORTH);
			jp.add(getHintDetailPanel(), BorderLayout.CENTER);
			jp.add(getButtonsContainer(), BorderLayout.SOUTH);
			contentPane = jp;
		}
		return contentPane;
	}

	private JScrollPane getHintDetailsScrollPane() {
		if ( hintDetailsScrollPane == null ) {
			final JScrollPane jsp = new JScrollPane();
			if ( getToolkit().getScreenSize().height < 800 )
				jsp.setPreferredSize(new Dimension(700, 110));
			else
				jsp.setPreferredSize(new Dimension(1000, 510));
			jsp.setViewportView(getHintDetailPane());
			hintDetailsScrollPane = jsp;
		}
		return hintDetailsScrollPane;
	}

	private ArrayList<HintNode> getSelectedHintNodes() {
		final ArrayList<HintNode> result = new ArrayList<>();
		final TreePath[] pathes = hintsTree.getSelectionPaths();
		if ( pathes == null )
			return result;
		// if user selects any other than the first hint (3rd row of tree) then
		// hintsCache is invalid coz it PRESUMES dat first hint will be applied
		final int[] rows = hintsTree.getSelectionRows();
		if ( rows == null )
			return result;
		if ( rows.length!=1 || rows[0]!=2 )
			engine.cacheHintsClear();
		for ( TreePath path : pathes )
			result.add((HintNode) path.getLastPathComponent());
		return result;
	}

	private boolean setSelectedHintNode(final String text) {
		if ( text != null )
			try {
				final HintNode root = (HintNode)hintsTree.getModel().getRoot();
				for ( HintNode n : root.getHintNodes() ) {
					if ( n.getName().startsWith(text) ) {
						hintsTree.setSelectionPath(new TreePath(n));
						return true;
					}
				}
			} catch (Exception ex) {
				Log.teeln("WARN: "+Log.me()+": "+ex);
			}
		return false;
	}

	/**
	 * remove all hints from the given deadTech
	 * and disable hinter until a puzzle is prepared.
	 *
	 * @param deadTech the deceased Tech
	 */
	private void removeHintsAndDisableHinter(final Tech deadTech) {
		// disable the hinter; which is re-enabled by loading a puzzle
		final IHinter zombie = engine.getWantedHinter(deadTech);
		if ( zombie != null )
			zombie.setIsEnabled(false);
		// remove the hints
		engine.removeHintsFrom(deadTech);
	}

	private JTree getHintsTree() {
		if ( hintsTree == null ) {
			final JTree jt = new JTree();
			// NOTE: fixed-height averts NPE from VariableHeightLayoutCache when
			// the tree is painted whilst empty, which is stupid hence annoying!
			jt.setRowHeight(17);
			jt.setShowsRootHandles(true);
			jt.getSelectionModel().setSelectionMode(
					TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			jt.setCellRenderer(new HintsTreeCellRenderer());
			jt.setExpandsSelectedPaths(true);
			jt.addTreeSelectionListener(new TreeSelectionListener() {
				@Override
				public void valueChanged(final TreeSelectionEvent e) {
					if ( hintsTree.isEnabled() ) {
						engine.hintsSelected(getSelectedHintNodes());
					}
				}
			;
			});
			// WARN: This KeyListener is replicated in the hintDetailPane. If you
			// change here then change there too; or methodise it all (BFIIK).
			jt.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(final KeyEvent e) {
					final int keyCode = e.getKeyCode();
					if ( keyCode == KeyEvent.VK_ENTER ) {
						applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
						e.consume();
					} else if ( keyCode == KeyEvent.VK_DELETE ) {
						final ArrayList<HintNode> shins = getSelectedHintNodes();
						hintsTree.clearSelection();
						if ( shins==null || shins.isEmpty() )
							beep();
						else {
							// disable this hinter
							final Tech dead = shins.get(0).getHint().getTech();
							// NEVER disable Naked/Hidden Singles (mandatory)
							if ( dead==Tech.NakedSingle || dead==Tech.HiddenSingle )
								beep();
							else {
								removeHintsAndDisableHinter(dead);
								// clear the hintsTree
								hintsTree.setModel(EMPTY_TREE_MODEL);
								repaint();
								// get hints again now that this hinter is disabled
								getAllHintsInBackground(e.isShiftDown(), e.isControlDown());
							}
						}
						e.consume();
					}
				}
			});
			jt.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					final int btn = e.getButton();
					final int cnt = e.getClickCount();
					final int mod = e.getModifiersEx();
					// double-left-click
					if ( btn==BUTTON1 && cnt==2 ) {
						// apply the selected hint, then get the next hint
						applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
						e.consume(); // supress default action
					} else if ( btn==BUTTON3 && cnt==1 ) {
						// Ctrl-single-right-click
						if ( (mod & InputEvent.CTRL_DOWN_MASK) != 0 ) {
							// copy all hints to the clipboard
							if (!engine.copyUnfilteredHintsListToClipboard()) {
								beep();
								e.consume(); // supress default action
							}
						// Alt-single-right-click
						} else if ( (mod & InputEvent.ALT_DOWN_MASK) != 0 ) {
							// find clipboard hint in hints tree
							final String hintToString = MyClipboard.getText();
							if ( !setSelectedHintNode(hintToString) ) {
								beep();
								e.consume(); // supress default action
							}
						// plain-single-right-click
						} else {
							// copy selected hint to the clipboard
							if (!engine.copyToClipboard(getSelectedHintNodes())) {
								beep();
								e.consume(); // supress default action
							}
						}
					}
				}
			});
			hintsTree = jt;
		}
		return hintsTree;
	}

	/**
	 * Run hintsTree.requestFocusInWindow() on the EDT.
	 */
	private void hintsTreeRequestFocus() {
		SwingUtilities.invokeLater(()->hintsTree.requestFocusInWindow());
	}

	/**
	 * Invokes engine.getAllHints(wantMore) in a background thread, because it
	 * may take a while (in the order of 10 seconds or 15 seconds, or upto 2
	 * minutes if you wantMore), so we cannot have it blocking the EDT.
	 * <p>
	 * Called by hintsTree.VK_ENTER, applySelectedHintsAndGetNextHint,
	 * btnGetAllHints & mitGetAllHints, and mitGetMoreAllHints.
	 * <p>
	 * Note that hint-caching may cause unforseeable problems; so there is a
	 * {@link Config#isCachingHints} to disable caching.
	 *
	 * @param wantMore are more hints wanted
	 */
	private void getAllHintsInBackground(final boolean wantMore, final boolean wantSolution) {
		// One at a time!
		// NOTE: blocking the EDT is bad form, but how else?
		// If user holdsDown VK_ENTER, how else prevent/ignore repeats?
		while ( isGettingAllHintsInBackground )
			lieDown(64);
		isGettingAllHintsInBackground = true;
		setHintDetailArea("Searching for" + (wantMore ? " MORE" : "") + " hints...");
		repaint();
		final Thread goat = new Thread("getAllHints") {
			@Override
			public void run() {
				if ( useCache )
					while ( engine.isCachingHints() )
						lieDown(50);
				engine.getAllHints(wantMore, wantSolution, logHints, logHints, printHints);
				hintsTreeRequestFocus();
				repaint();
				if ( useCache )
					// No log on cache, only on "get", which sux for bugs!
					engine.cacheHints(wantMore, wantSolution, false, false, printHints);
				isGettingAllHintsInBackground = false;
			}
		};
		goat.setDaemon(true);
		goat.start();
	}

	// apply the currently selected hint, if any, and then get the next hint.
	private void applySelectedHintsAndGetNextHint(final boolean wantMore, final boolean wantSolution) {
		try {
			if ( engine.getGrid().numSet > 80 )
				setCurrentHint(engine.getSolvedHint(), false);
			else {
				engine.applySelectedHints(); // throws UnsolvableException
				getAllHintsInBackground(wantMore, wantSolution);
			}
		} catch (IllegalStateException eaten) {
			// already logged
		} catch (UnsolvableException ex) {
//			// UnsolvableException: the grid is rooted! Either the given puzzle
//			// is invalid, or there is a bug leading to an invalid hint being
//			// applied (still possible, apparently, despite my best efforts to
//			// avert this situation) so you should dig into this to (at least)
//			// workout which hinter is buggered, and raise an RFC or fix it
//			// yourself. The programmer humbly appologises for being merely
//			// human, and therebye sending-it-all-gone-bugger-up. ~KRC.
			logError(ex);
			displayError(ex);
		}
	}

	/**
	 * Logs the given exception to the applications log file. Note that errors
	 * are displayed to the user separately with displayError.
	 *
	 * @param ex
	 */
	void logError(final Exception ex) {
		// wait for stdout so stderr does not interleave
		// Sleep on this thread does not work, it stops stdout writing,
		// so try stderring in the background after a little lie down.
		SwingUtilities.invokeLater(()->{
			lieDown();
			Log.whinge("GUI Error!", ex);
		});
	}

	/**
	 * Displays the stackTrace of the given Exception in the hintDetailArea and
	 * prints it to the standard-error stream.
	 *
	 * @param ex Exception to display.
	 */
	void displayError(final Exception ex) {
		SwingUtilities.invokeLater(()->{
			// html of the stack trace
			setHintDetailArea("<html><body><font color=\"red\"><pre>"
					+ stackTraceOf(ex)
					+ "</pre></font></body></html>");
			beep();
		});
		// printStackTrace waits for stdout so stderr does not interleave.
		StdErr.printStackTrace(ex);
	}

	/**
	 * Clears the hint-detail area - white part near bottom of form.
	 */
	void clearHintDetailArea() {
		setHintDetailArea(null);
	}

	/**
	 * Displays the given HTML in the hint-detail area - white part near bottom
	 * of form.
	 */
	void setHintDetailArea(String htmlText) {
		// the empty string displays an empty unumbered list, which sucks.
		if ( htmlText!=null && htmlText.length()==0 )
			htmlText = null;
		final JEditorPane jep = getHintDetailPane();
		// remember the html ourselves because hintDetailHtml.getText() wraps!
		jep.setText(hintDetailHtml = htmlText);
		jep.setCaretPosition(0);
		// same length as a difficulty or the label jumps around distractingly
		lblPuzzleRating.setText("    0");
	}

	private JEditorPane getHintDetailPane() {
		if ( hintDetailPane == null ) {
			final JEditorPane jep = new JEditorPane("text/html", null) {
				private static final long serialVersionUID = -5658720148768663350L;
				@Override
				public void paint(final Graphics g) {
					final Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					super.paint(g);
				}
			};
			jep.setEditable(false);
			jep.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					final int btn = e.getButton();
					final int clks = e.getClickCount();
					// right-click: copy the html-text to the clipboard
					if ( clks==1 && (btn==BUTTON2 || btn==BUTTON3) ) {
						if (hintDetailHtml != null) {
							engine.copyToClipboard(hintDetailHtml);
						} else {
							beep();
						}
						e.consume();
					// double-left-click: if the selected text looks like a cell id
					// then focus on (yellow background) that cell in the grid;
					// else if id is an ALS-ID then repaint ALS over the others
	//				} else if ( clks==2 && btn==BUTTON1 ) {
					} else if ( btn==BUTTON1 ) {
						final String s = hintDetailPane.getSelectedText();
						if ( s != null ) {
							final int len = s.length();
							if ( len==2 && CELL_ID_PATTERN.matcher(s).matches() ) {
								gridPanel.setFocusedCellById(s);
							} else if ( len==2 && CELL_INDICE_PATTERN.matcher(s).matches() ) {
								gridPanel.setFocusedCellByIndice(s);
							} else if ( ( (len==1||len==2) && ALS_ID_PATTERN.matcher(s).matches() )
									// hintType: ALS-XZ, ALS-Wing, and ALS-Chain
									 && hintDetailHtml.contains("ALS") ) {
								// id looks like an ALS-ID, so selectAls, to repaint it
								// over the top of the others, so that you can see it.
								gridPanel.selectAls(AlsHelper.alsIndex(s));
							} else if ( len==5 && REGION_LABEL_PATTERN.matcher(s).matches() ) {
								try {
									gridPanel.setBases(Regions.array(gridPanel.getGrid().region(s)));
									gridPanel.repaint();
								} catch ( IllegalArgumentException eaten ) {
									// Do nothing
								}
							}
						}
					}
				}
			});
			// make keys do same here as in the hintsTree
			jep.addKeyListener(new KeyAdapter() {
				private String target;
				private int place;
				@Override
				public void keyPressed(final KeyEvent e) {
					final int keyCode = e.getKeyCode();
					if ( keyCode == KeyEvent.VK_ENTER ) {
						applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
						e.consume();
					} else if ( keyCode == KeyEvent.VK_DELETE ) {
						// this mess averts all possible NPEs getting deadTech.
						// It is ugly but safe.
						try {
							final ArrayList<HintNode> nodes = getSelectedHintNodes();
							if ( nodes!=null && !nodes.isEmpty() ) {
								final HintNode node = nodes.get(0);
								if ( node != null ) {
									final AHint hint = node.getHint();
									if ( hint != null ) {
										final Tech deadTech = hint.getTech();
										if ( deadTech != null ) {
											removeHintsAndDisableHinter(deadTech);
											hintsTree.clearSelection();
											repaint();
										}
									}
								}
							}
						} catch (Exception eaten) {
							// Do nothing, no biggy, it is only a hack
						}
						e.consume();
					// Ctrl-F or Ctrl-F3 to find
					} else if ( (keyCode==KeyEvent.VK_F || keyCode==KeyEvent.VK_F3)
							 && e.isControlDown() ) {
						findFirst();
						e.consume();
					// F3 to find next
					} else if ( keyCode==KeyEvent.VK_F3 ) {
						if ( target==null || target.isEmpty() || place==-1 )
							findFirst();
						else
							findNext();
						e.consume();
					}
				}
				private void findFirst() {
					target = askForString("Target", "Find hint details");
					if ( target==null || target.isEmpty() )
						place = -1;
					else
						try {
							final Caret caret = hintDetailPane.getCaret();
							final Document doc = hintDetailPane.getDocument();
							final String text = doc.getText(0, doc.getLength());
							place = text.indexOf(target);
							if ( place == -1 ) {
								caret.setDot(0);
								beep();
							} else {
								caret.setDot(place);
								caret.moveDot(place+target.length());
							}
						} catch (BadLocationException ex) {
							// Do nothing
						}
				}
				private void findNext() {
					assert target!=null && !target.isEmpty();
					assert place > -1;
					try {
						final Caret caret = hintDetailPane.getCaret();
						final Document doc = hintDetailPane.getDocument();
						final String text = doc.getText(0, doc.getLength());
						place = text.indexOf(target, place+target.length());
						if ( place == -1 ) {
							caret.setDot(0);
							beep();
						} else {
							caret.setDot(place);
							caret.moveDot(place+target.length());
						}
					} catch (BadLocationException ex) {
						// Do nothing
					}
				}
			});
			hintDetailPane = jep;
		}
		return hintDetailPane;

	}

	private JScrollPane getHintsTreeScrollPane() {
		if ( hintsTreeScrollpane == null ) {
			final JScrollPane jsp = new JScrollPane();
			jsp.setPreferredSize(new Dimension(100, 100));
			jsp.setViewportView(getHintsTree());
			hintsTreeScrollpane = jsp;
		}
		return hintsTreeScrollpane;
	}

	private JPanel getNorthPanel() {
		if ( northPanel == null ) {
			final JPanel jp = new JPanel(new BorderLayout());
			jp.add(getSudokuGridPanelHolder(), BorderLayout.WEST);
			jp.add(getHintsTreePanel(), BorderLayout.CENTER);
			northPanel = jp;
		}
		return northPanel;
	}

	private JPanel getSudokuGridPanelHolder() {
		if ( sudokuGridPanelHolder == null ) {
			final JPanel jp = new JPanel(new BorderLayout());
			jp.add(gridPanel, BorderLayout.CENTER);
			jp.add(getViewSelectionPanel(), BorderLayout.SOUTH);
			sudokuGridPanelHolder = jp;
		}
		return sudokuGridPanelHolder;
	}

	private JPanel getHintDetailPanel() {
		if ( hintDetailPanel == null ) {
			final JPanel jp = new JPanel(new BorderLayout());
			jp.add(getHintDetailsScrollPane(), BorderLayout.CENTER);
			hintDetailPanel = jp;
		}
		return hintDetailPanel;
	}

	private GridBagConstraints newGBC(final int gridx, final int gridy) {
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gridx;
		gbc.weightx = 1.0D;
		gbc.gridy = gridy;
		return gbc;
	}

	private JPanel getButtonsPane() {
		if ( buttonsPane == null ) {
			final JPanel jp = new JPanel(new GridBagLayout());
			jp.add(getBtnValidate(), newGBC(0, 0));
			jp.add(getBtnSolveStep(), newGBC(1, 0));
			jp.add(getBtnGetNextHint(), newGBC(2, 0));
			jp.add(getBtnApplyHint(), newGBC(4, 0));
			jp.add(getBtnGetAllHints(), newGBC(5, 0));
			buttonsPane = jp;
		}
		return buttonsPane;
	}

	private JButton getBtnGetNextHint() {
		if ( btnGetNextHint == null ) {
			final JButton jb = newJButton("Get next hint", KeyEvent.VK_N
					, "Get the next bloody hint");
			jb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.invokeGetNextHint(false, false);
				}
			});
			btnGetNextHint = jb;
		}
		return btnGetNextHint;
	}

	private JButton getBtnGetAllHints() {
		if ( btnGetAllHints == null ) {
			final JButton jb = newJButton("Get all hints", KeyEvent.VK_A
					, "Get all hints applicable on the current situation");
			jb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final int mod = e.getModifiers();
					final boolean wantMore = (mod & SHIFT_MASK) != 0;
					final boolean wantSolution = (mod & CTRL_MASK) != 0;
					getAllHintsInBackground(wantMore, wantSolution);
				}
			});
			btnGetAllHints = jb;
		}
		return btnGetAllHints;
	}

	/**
	 * @return the "Solve Step" JButton, which applies the currently selected
	 * hint, if any, and then searches the grid for the next hint. Package
	 * visible so that SudokuPanel.keyTyped can give me the focus.
	 */
	JButton getBtnSolveStep() {
		if ( btnSolveStep == null ) {
			final JButton jb = newJButton("Solve step", KeyEvent.VK_S
					, "Apply the selected hint (if any) and get the next one");
			jb.setFont(DIALOG_BOLD_12);
			jb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.solveStep(false);
				}
			});
			btnSolveStep = jb;
		}
		return btnSolveStep;
	}

	private JPanel getButtonsContainer() {
		if ( buttonsPanel == null ) {
			final JPanel jp = new JPanel(new GridLayout(1, 1));
			jp.add(getButtonsPane(), null);
			buttonsPanel = jp;
		}
		return buttonsPanel;
	}

	private JPanel getViewSelectionPanel() {
		if ( viewSelectPanel == null ) {
			final JPanel jp = new JPanel(new FlowLayout());
			// nb: cheatName remains "" until cheats are authorised
			jp.add(getLblCheatName());
			jp.add(getCmbViewSelector());
			viewSelectPanel = jp;
		}
		return viewSelectPanel;
	}

	private JPanel getHintsTreePanel() {
		if ( hintsTreePanel == null ) {
			final JPanel jp = new JPanel();
			jp.setLayout(new BorderLayout());
			jp.add(getHintsTreeScrollPane(), BorderLayout.CENTER);
			jp.add(getHintsSouthPanel(), BorderLayout.SOUTH);
			hintsTreePanel = jp;
		}
		return hintsTreePanel;
	}

	private JButton getBtnValidate() {
		if ( btnValidate == null ) {
			final JButton jb = newJButton("Validate", KeyEvent.VK_V
					, "Check the Sudoku is valid (has exactly one solution)");
			jb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// Ctrl-Shft-V to validate noisily
					final boolean quite = (e.getModifiers() & Event.SHIFT_MASK) == 0;
					if ( engine.validatePuzzleAndGrid(quite) )
						setHintDetailArea(Html.load(SudokuFrame.this, "Valid.html"));
				}
			});
			btnValidate = jb;
		}
		return btnValidate;
	}

	private JButton getBtnApplyHint() {
		if ( btnApplyHint == null ) {
			final JButton jb = newJButton("Apply hint", KeyEvent.VK_P
					, "Apply the selected hint(s)");
			jb.setEnabled(false);
			jb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.applySelectedHints();
				}
			});
			btnApplyHint = jb;
		}
		return btnApplyHint;
	}

	private JComboBox<String> getCmbViewSelector() {
		if ( cmbViewSelector == null ) {
			JComboBox<String> jcb = new JComboBox<>();
			jcb.setToolTipText("Toggle view, especially for chaining hints");
			jcb.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged( ItemEvent e) {
					if ( e.getStateChange() == ItemEvent.SELECTED
					  && cmbViewSelector.isEnabled() ) {
						viewNum = cmbViewSelector.getSelectedIndex();
						repaintHint();
					}
				}
			});
			cmbViewSelector = jcb;
		}
		return cmbViewSelector;
	}

	// ========================== HINTS SOUTH PANEL ===========================

	private JPanel getHintsSouthPanel() {
		if ( hintsSouthPanel == null ) {
			final JPanel jp = new JPanel(new BorderLayout());
			jp.add(getHintsSouthWestPanel(), BorderLayout.WEST);
			jp.add(getRatingPanel(), BorderLayout.EAST);
			hintsSouthPanel = jp;
		}
		return hintsSouthPanel;
	}

	private JPanel getHintsSouthWestPanel() {
		if ( hintsSouthWestPanel == null ) {
			final JPanel jp = new JPanel(new FlowLayout());
			jp.add(getUseChacheLabel());
			jp.add(getChkFilterHints());
			jp.add(getDisabledTechsWarnPanel());
			hintsSouthWestPanel = jp;
		}
		return hintsSouthWestPanel;
	}

	private JPanel getDisabledTechsWarnPanel() {
		if ( disabledTechsWarnPanel == null ) {
			final JLabel jl = new JLabel();
			jl.setText("");
			jl.setToolTipText(
					 "<html><body>"
					+"Not all the available Sudoko solving techniques are enabled.<br>"
					+"Click here to dis/enable solving techniques."
					+"</body></html>");
			jl.setIcon(createImageIcon("Icon_Warning.gif"));
			jl.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					selectSodokuSolvingTechniques();
				}
			});
			lblDisabledTechsWarning = jl;
			final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
			jp.add(jl, null);
			jp.setVisible(false);
			disabledTechsWarnPanel = jp;
		}
		return disabledTechsWarnPanel;
	}

	private JCheckBox getChkFilterHints() {
		if ( chkFilterHints == null ) {
			final JCheckBox jcb = new JCheckBox("Filter hints");
			jcb.setMnemonic(KeyEvent.VK_I);
			jcb.setToolTipText("Filter-out hints with repeat effects");
			jcb.setSelected(CFG.isFilteringHints());
			jcb.setEnabled(false);
			jcb.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					final boolean isSelected = chkFilterHints.isSelected();
					getMitFilterHints(0).setSelected(isSelected);
					engine.setFiltered(isSelected);
				}
			});
			chkFilterHints = jcb;
		}
		return chkFilterHints;
	}

	private JLabel getUseChacheLabel() {
		if ( lblUseCache == null ) {
			final JLabel jl = new JLabel();
			lblUseCacheUpdate(jl);
			jl.setEnabled(true);
			jl.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					setUseCache(!useCache);
				}
			});
			lblUseCache = jl;
		}
		return lblUseCache;
	}

	// a method to do ONCE, consistently
	private void lblUseCacheUpdate(final JLabel jl) {
		if ( useCache ) {
			jl.setToolTipText("hint cache is enabled");
			jl.setIcon(createImageIcon("Icon_Cache.gif"));
		} else {
			jl.setToolTipText("hint cache is disabled");
			jl.setIcon(createImageIcon("Icon_CacheDisabled.gif"));
		}
	}

	// a method to update both controls when either changes
	private void setUseCache(final boolean b) {
		useCache = b;
		// update GUI
		getMitCacheHints(0).setSelected(useCache);
		CFG.setBoolean(Config.isCachingHints, useCache);
		lblUseCacheUpdate(lblUseCache);
		// fetch when caching is switched on
		updateHintsCache();
	}

	void updateHintsCache() {
		if ( useCache )
			engine.cacheHints(false, false, false, false, false);
		else
			engine.cacheHintsClear();
	}

	private JPanel getRatingPanel() {
		if ( ratingPanel == null ) {
			final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
			jp.add(new JLabel("Hint rating: "), null);
			// same length as a difficulty or label jumps around distractingly
			jp.add(this.lblPuzzleRating = new JLabel("    0"), null);
			ratingPanel = jp;
		}
		return ratingPanel;
	}

	// ============================= MAIN MENU =============================

	private JMenuBar getMyMainMenuBar() {
		if ( menuBar == null ) {
			final JMenuBar jmb = new JMenuBar();
			jmb.add(getFileMenu());
			jmb.add(getEditMenu());
			jmb.add(getToolMenu());
			jmb.add(getOptionsMenu());
			jmb.add(getHelpMenu());
			menuBar = jmb;
		}
		return menuBar;
	}

	// menu keyboard-shortcut: normal (no modifier keys)
	private JMenuItem norm(final int keyCode, final JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(keyCode, 0));
		return mit;
	}

	// menu keyboard-shortcut: alt (alternate)
	private JMenuItem alt(final int keyCode, final JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(keyCode, ALT_MASK));
		return mit;
	}

	// menu keyboard-shortcut: shft (move your ass)
	private JMenuItem shft(final int keyCode, final JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(keyCode, SHIFT_MASK));
		return mit;
	}

	// menu keyboard-shortcut: ctrl (control)
	private JMenuItem ctrl(final char ch, final JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(ch, CTRL_MASK));
		return mit;
	}

	// ============================= FILE MENU =============================

	private JMenu getFileMenu() {
		if ( fileMenu == null ) {
			final JMenu jm = newJMenu("File", KeyEvent.VK_F, "Load and s__t");
			jm.add(ctrl('N', getMitFileNew(KeyEvent.VK_N)));
			jm.add(ctrl('G', getMitFileGenerate(KeyEvent.VK_G)));
			jm.addSeparator();
			jm.add(ctrl('M', getMitFilesRecent(KeyEvent.VK_M)));
			jm.addSeparator();
			jm.add(ctrl('O', getMitFileOpen(KeyEvent.VK_O)));
			jm.add(ctrl('R', getMitFileReload(KeyEvent.VK_R)));
			jm.add(ctrl('P', getMitFileNext(KeyEvent.VK_P)));
			jm.add(ctrl('S', getMitFileSave(KeyEvent.VK_S)));
			jm.addSeparator();
			jm.add(ctrl('L', getMitLogView(KeyEvent.VK_L)));
			jm.add(ctrl('F', getMitLogFollow(KeyEvent.VK_F)));
			jm.addSeparator();
			jm.add(ctrl('Q', getMitQuit(KeyEvent.VK_Q)));
			fileMenu = jm;
		}
		return fileMenu;
	}

	private JMenuItem getMitFileNew(final int keyEvent) {
		if ( mitNew == null ) {
			final JMenuItem jmi = newJMenuItem("New", keyEvent, "Clear the grid");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.clearGrid();
					setTitle();
				}
			});
			mitNew = jmi;
		}
		return mitNew;
	}

	private JMenuItem getMitFileGenerate(final int keyEvent) {
		if ( mitGenerate == null ) {
			final JMenuItem jmi = newJMenuItem("Generate...", keyEvent
					, "Open a dialog to generate a random Sudoku puzzle");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					if ( generateDialog == null ) {
						final GenerateDialog gd = GenerateDialog.getInstance();
						// pack() the dialog in order to get its width
						gd.pack();
						final int w = getToolkit().getScreenSize().width;
						gd.setLocation(w - gd.getSize().width, 0); // top-right of screen
						generateDialog = gd;
					}
					setTitle();
					generateDialog.setVisible(true);
				}
			});
			mitGenerate = jmi;
		}
		return mitGenerate;
	}

	private JMenuItem getMitFilesRecent(final int keyEvent) {
		if ( mitRecentFiles == null ) {
			final JMenuItem jmi = newJMenuItem("Recent Files...", keyEvent
					, "Open a dialog to select a recently accessed file");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					RecentFilesDialog rfd = recentFilesDialog;
					if ( rfd == null ) {
						rfd = new RecentFilesDialog(SudokuFrame.this, engine);
						rfd.pack();
						rfd.setLocation(0, 0);
						recentFilesDialog = rfd;
					}
					recentFilesDialog.setVisible(true);
				}
			});
			mitRecentFiles = jmi;
		}
		return mitRecentFiles;
	}

	private JMenuItem getMitFileOpen(final int keyEvent) {
		if ( mitLoad == null ) {
			final JMenuItem jmi = newJMenuItem("Open...", keyEvent
					, "Open a puzzle file");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						openFile();
						showWelcomeHtml();
						repaint();
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
			mitLoad = jmi;
		}
		return mitLoad;
	}

	private void openFile() {
		try {
			final FileOpener opener = new FileOpener(this, engine, defaultDirectory);
			final SourceID source = engine.loadFile(opener.puzzleSource());
			defaultDirectory = opener.defaultDirectory;
			setTitle(source);
		} catch (Exception ex) {
			displayError(ex);
		}
	}

	private void showError(Exception ex) {
		carp("Oops: "+ex.toString()+NL+"The artichokes are fighting back!"
			, "Ohhh bugger");
	}

	private JMenuItem getMitFileReload(final int keyEvent) {
		if ( mitReload == null ) {
			final JMenuItem jmi = newJMenuItem("Reload", keyEvent
					, "reload the current puzzle into the grid");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						setTitle(engine.reloadFile());
						repaint();
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
			mitReload = jmi;
		}
		return mitReload;
	}

	private JMenuItem getMitFileNext(final int keyEvent) {
		if ( mitLoadNext == null ) {
			final JMenuItem jmi = newJMenuItem("load next Puzzle", keyEvent
					, "load the next puzzle from the current MagicTour file");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						nextFile(Logging.of(e.getModifiers()));
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
			mitLoadNext = jmi;
		}
		return mitLoadNext;
	}

	private void nextFile(final Logging logging) {
		// there is no "next" without a source for "this"
		final SourceID src = engine.getGrid().source;
		if ( src == null ) {
			engine.cacheHintsClear();
			return;
		}
		// special: Ctrl-P generates next puzzle in generate.
		final GenerateDialog gd = generateDialog;
		if ( gd!=null && IO.GENERATED_FILE.equals(src.file) ) {
			gd.setVisible(true);
			gd.generate();
			hintsTreeRequestFocus();
			engine.cacheHintsClear();
			return;
		}
		// load the next puzzle (normally)
		final SourceID pid;
		if ( nameEndsWith(src.file, ".mt") ) {
			clearHintDetailArea();
			pid = engine.loadNextPuzzle();
		} else if ( nameEndsWith(src.file, ".txt") ) {
			clearHintDetailArea();
			pid = engine.loadFile(engine.nextTxtFile(src.file), 0);
		} else {
			hintsTreeRequestFocus();
			engine.cacheHintsClear();
			return;
		}
		setTitle(pid);
		repaint();
		// auto-analyse
		analyseInTheBackground(logging);
		hintsTreeRequestFocus();
		engine.cacheHintsClear();

	}

	private JMenuItem getMitFileSave(final int keyEvent) {
		if ( mitSave == null ) {
			final JMenuItem jmi = newJMenuItem("Save...", keyEvent
					, "Save this puzzle to a file");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						saveFile();
						repaint();
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
			mitSave = jmi;
		}
		return mitSave;
	}

	private void saveFile() {
		final FileOpener opener = new FileOpener(this, engine, defaultDirectory);
		final File file = opener.toSave();
		defaultDirectory = opener.defaultDirectory;
		if ( file == null )
			return;
		setTitle(engine.saveFile(file));
	}

	// I reckon only techies will ever use LogicalSolverTester. This menu item
	// allows you to view the hints-of-type in a LogicalSolverTester .log file.
	// * The intended regex is a Tech.name() or whatever as per the hint-line
	//   in the log-File. Only hint-text (char 65 on) is matched
	// * Use the menu to start a new search (even if search exists)
	// * Then press Ctrl-L to find-next (or start a new search, if none exists)
	private JMenuItem getMitLogView(final int keyEvent) {
		if ( mitLogView == null ) {
			final JMenuItem jmi = newJMenuItem("Log View", keyEvent
					, "View hints that match a regular expresion in an"
					+ " existing LogicalSolverTester .log file");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// no log yet
					if ( logViewFile==null || !logViewFile.exists()
					  // or no regex yet
					  || regex==null || regex.isEmpty()
					  // or initiated by the menu
					  || (e.getModifiers() & CTRL_MASK) == 0
					) {
						// get the .log File
						final FileOpener opener = new FileOpener(
								SudokuFrame.this, engine, defaultDirectory);
						logViewFile = opener.logFile();
						defaultDirectory = opener.defaultDirectory;
						if ( logViewFile == null || !logViewFile.exists() )
							return;
						// re/start the search
						engine.startLine = 0;
						// LogViewHintRegexDialog.btnOk calls-back logView (below)
						new LogViewHintRegexDialog(SudokuFrame.this).setVisible(true);
					} else {
						// view the next occurrence of regex in logViewFile
						logView(regex);
					}
				}
			});
			mitLogView = jmi;
		}
		return mitLogView;
	}

	// LogViewHintRegexDialog.btnOk calls-back this method
	void logView(final String re) {
		regex = engine.logView(logViewFile, re);
		setTitle(engine.getGrid().source);
	}

	// Techie follows the batch hintpath in *.log file.
	private JMenuItem getMitLogFollow(final int keyEvent) {
		if ( mitLogFollow == null ) {
			final JMenuItem jmi = newJMenuItem("Log Follow", keyEvent
					, "Follow hints in a batch *.log file");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// default to the logFile followed last time
					final File defualt = new File(CFG.getString(Config.logToFollow));
					final FileOpener opener = new FileOpener(SudokuFrame.this, engine, defaultDirectory);
					final File logFile = opener.logFile(defualt);
					if ( logFile != null ) {
						if ( logFile.exists() ) {
							// remember this logFile as next times default
							CFG.setString(Config.logToFollow, logFile.getAbsolutePath());
							final boolean was = useCache;
							useCache = false;
							engine.logFollow(logFile);
							useCache = was;
						} else {
							beep();
						}
					}
				}
			});
			mitLogFollow = jmi;
		}
		return mitLogFollow;
	}

	private JMenuItem getMitQuit(final int keyEvent) {
		if ( mitQuit == null ) {
			final JMenuItem jmi = newJMenuItem("Quit", keyEvent, "Bye bye");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					dispose();
				}
			});
			mitQuit = jmi;
		}
		return mitQuit;
	}

	// ============================= EDIT MENU =============================

	private JMenu getEditMenu() {
		if ( editMenu == null ) {
			final JMenu jm = newJMenu("Edit", KeyEvent.VK_E, "Edit the grid.");
			jm.add(ctrl('C', getMitCopy(KeyEvent.VK_C)));
			jm.add(ctrl('V', getMitPaste(KeyEvent.VK_P)));
			jm.addSeparator();
			jm.add(ctrl('E', getMitClear(KeyEvent.VK_E)));
			editMenu = jm;
		}
		return editMenu;
	}

	private JMenuItem getMitCopy(final int keyEvent) {
		if ( mitCopy == null ) {
			final JMenuItem jmi = newJMenuItem("Copy grid", keyEvent
					, "Copy the grid to the clipboard as plain text");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						engine.copyGridToClipboard();
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
			mitCopy = jmi;
		}
		return mitCopy;
	}

	private JMenuItem getMitPaste(final int keyEvent) {
		if ( mitPaste == null ) {
			final JMenuItem jmi = newJMenuItem("Paste grid", keyEvent
					, "Replace the grid with the content of the clipboard");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						engine.pastePuzzle();
					} catch (Exception ex) {
						showError(ex);
					}
				}
			});
			mitPaste = jmi;
		}
		return mitPaste;
	}

	private JMenuItem getMitClear(final int keyEvent) {
		if ( mitClear == null ) {
			final JMenuItem jmi = newJMenuItem("Clear grid", keyEvent
					, "Clear the grid");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.clearGrid();
				}
			});
			mitClear = jmi;
		}
		return mitClear;
	}

	// ============================= TOOL MENU =============================

	private JMenu getToolMenu() {
		if ( toolsMenu == null ) {
			final JMenu jm = newJMenu("Tools", KeyEvent.VK_T
					, "Saw, pin, wax, axe. Careful Eugeene!");
			// NOTE WELL: Each menu-item has ONE accelerator key only, so every
			// combo of norm/shft/alt/ctrl needs it is very own menu item, which
			// really s__ts me. There simply MUST be a better way to do it, so
			// the User does not see all my expert modifiers! Anybody who can read
			// the code (it will always be open source) could see all the advanced
			// stuff, but the GUI looks simple and vanilla, so that peeps can work
			// out how to use the bastard!
			// BTW: create method shftCtrl (or whatever) if/when you use it.
			jm.add(ctrl('#', getMitResetPotentials(KeyEvent.VK_NUMBER_SIGN)));
			jm.add(ctrl('D', getMitClearHints(KeyEvent.VK_D)));
			jm.addSeparator();
			jm.add(norm(KeyEvent.VK_F2, getMitSolveStep(KeyEvent.VK_S)));
			jm.add(shft(KeyEvent.VK_F2, getMitSolveStepBig(KeyEvent.VK_U)));
			jm.addSeparator();
			jm.add(norm(KeyEvent.VK_F3, getMitGetNextHint(KeyEvent.VK_N)));
			jm.add(norm(KeyEvent.VK_F4, getMitApplyHint(KeyEvent.VK_A)));
			jm.add(norm(KeyEvent.VK_F5, getMitGetAllHints(KeyEvent.VK_H)));
			jm.add(shft(KeyEvent.VK_F5, getMitGetAllHintsMore(KeyEvent.VK_M)));
			jm.addSeparator();
			jm.add(norm(KeyEvent.VK_F6, getMitGetClueSmall(KeyEvent.VK_C)));
			jm.add(shft(KeyEvent.VK_F6, getMitGetClueBig(KeyEvent.VK_B)));
			jm.addSeparator();
			jm.add(norm(KeyEvent.VK_F7, getMitCheckValidity(KeyEvent.VK_V)));
			jm.addSeparator();
			jm.add(norm(KeyEvent.VK_F8, getMitSolve(KeyEvent.VK_O)));
			jm.add(norm(KeyEvent.VK_F9, getMitAnalyse(KeyEvent.VK_Y)));
			jm.add(shft(KeyEvent.VK_F9, getMitAnalyseVerbose(KeyEvent.VK_J)));
			jm.add(alt(KeyEvent.VK_F9, getMitAnalyseTiming(KeyEvent.VK_T)));
			if ( Run.ASSERTS_ENABLED ) {
				jm.addSeparator();
				jm.add(norm(KeyEvent.VK_L, getMitLibate(KeyEvent.VK_L)));
			}
			toolsMenu = jm;
		}
		return toolsMenu;
	}

	private JMenuItem getMitResetPotentials(final int keyEvent) {
		if ( mitResetPotentialValues == null ) {
			final JMenuItem jmi = newJMenuItem("Reset potential values"
					, keyEvent // #
					, "Recompute the remaining potential values for the empty cells");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.resetPotentialValues();
				}
			});
			mitResetPotentialValues = jmi;
		}
		return mitResetPotentialValues;
	}

	private JMenuItem getMitClearHints(final int keyEvent) {
		if ( mitClearHints == null ) {
			final JMenuItem jmi = newJMenuItem("Clear hint(s)", keyEvent
					, "Clear the hint list");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.clearHints();
				}
			});
			mitClearHints = jmi;
		}
		return mitClearHints;
	}

	private JMenuItem getMitSolveStep(final int keyEvent) {
		if ( mitSolveStep == null ) {
			final JMenuItem jmi = newJMenuItem("Solve step", keyEvent
					, getBtnSolveStep().getToolTipText());
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.solveStep(false);
				}
			});
			mitSolveStep = jmi;
		}
		return mitSolveStep;
	}

	private JMenuItem getMitSolveStepBig(final int keyEvent) {
		if ( mitSolveStepBig == null ) {
			final JMenuItem jmi = newJMenuItem("Solve big step", keyEvent
					, "apply hint and get next BIG hint (aggregate chains and"
					+ " solve with singles).");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.solveStep(true);
				}
			});
			mitSolveStepBig = jmi;
		}
		return mitSolveStepBig;
	}

	private JMenuItem getMitGetNextHint(final int keyEvent) {
		if ( mitGetNextHint == null ) {
			final JMenuItem jmi = newJMenuItem("Get next hint", keyEvent
					, getBtnGetNextHint().getToolTipText());
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.invokeGetNextHint(false, false);
				}
			});
			mitGetNextHint = jmi;
		}
		return mitGetNextHint;
	}

	private JMenuItem getMitApplyHint(final int keyEvent) {
		if ( mitApplyHint == null ) {
			final JMenuItem jmi = newJMenuItem("Apply hint", keyEvent
					, getBtnApplyHint().getToolTipText());
			jmi.setEnabled(false);
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.applySelectedHints();
				}
			});
			mitApplyHint = jmi;
		}
		return mitApplyHint;
	}

	private JMenuItem getMitGetAllHints(final int keyEvent) {
		if ( mitGetAllHints == null ) {
			final JMenuItem jmi = newJMenuItem("Get all hints", keyEvent
					, getBtnGetAllHints().getToolTipText());
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					getAllHintsInBackground(false, false);
				}
			});
			mitGetAllHints = jmi;
		}
		return mitGetAllHints;
	}

	private JMenuItem getMitGetAllHintsMore(final int keyEvent) {
		if ( mitGetAllHintsMore == null ) {
			final JMenuItem jmi = newJMenuItem("Get MORE all hints", keyEvent
					, "Get MORE all hints (from all selected Techs).");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					getAllHintsInBackground(true, false);
				}
			});
			mitGetAllHintsMore = jmi;
		}
		return mitGetAllHintsMore;
	}

	private JMenuItem getMitGetClueSmall(final int keyEvent) {
		if ( mitGetClueSmall == null ) {
			final JMenuItem jmi = newJMenuItem("Get a small clue", keyEvent
					, "What is the next solving step?");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.getClue(false);
				}
			});
			mitGetClueSmall = jmi;
		}
		return mitGetClueSmall;
	}

	private JMenuItem getMitGetClueBig(final int keyEvent) {
		if ( mitGetClueBig == null ) {
			final JMenuItem jmi = newJMenuItem("Get a big clue", keyEvent
					, "What and where is the next solving step?");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.getClue(true);
				}
			});
			mitGetClueBig = jmi;
		}
		return mitGetClueBig;
	}

	private JMenuItem getMitCheckValidity(final int keyEvent) {
		if ( mitCheckValidity == null ) {
			final JMenuItem jmi = newJMenuItem("Check validity", keyEvent
					, "Is this Sudoku valid (has exactly one solution)?");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// hold down the Shift key to log the validations
					final boolean quite = (e.getModifiers() & Event.SHIFT_MASK) == 0;
					if ( engine.validatePuzzleAndGrid(quite) ) {
						setHintDetailArea(Html.load(SudokuFrame.this, "Valid.html"));
					}
				}
			});
			mitCheckValidity = jmi;
		}
		return mitCheckValidity;
	}

	private JMenuItem getMitSolve(final int keyEvent) {
		if ( mitSolve == null ) {
			final JMenuItem jmi = newJMenuItem("Solve", keyEvent
					, "Just solve the bastard, asap.");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.solveASAP();
				}
			});
			mitSolve = jmi;
		}
		return mitSolve;
	}

	private JMenuItem getMitAnalyse(final int keyEvent) {
		if ( mitAnalyse == null ) {
			final JMenuItem jmi = newJMenuItem("Analyse", keyEvent
				, "Summarise a (not the) simplest possible solution to this Sudoku");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					analyseInTheBackground(Logging.NORMAL);
				}
			});
			mitAnalyse = jmi;
		}
		return mitAnalyse;
	}
	private JMenuItem getMitAnalyseVerbose(final int keyEvent) {
		if ( mitAnalyseVerbose == null ) {
			final JMenuItem jmi = newJMenuItem("Analyse (verbose)", keyEvent
				, "Summarise the Sudoku, logging each hint (verbose)");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					analyseInTheBackground(Logging.HINTS);
				}
			});
			mitAnalyseVerbose = jmi;
		}
		return mitAnalyseVerbose;
	}
	private JMenuItem getMitAnalyseTiming(final int keyEvent) {
		if ( mitAnalyseTiming == null ) {
			final JMenuItem jmi = newJMenuItem("Analyse (very verbose)", keyEvent
				, "Summarise the Sudoku, logging hinter timings (very verbose)");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					analyseInTheBackground(Logging.HINTERS);
				}
			});
			mitAnalyseTiming = jmi;
		}
		return mitAnalyseTiming;
	}
	private static enum Logging{
		  NORMAL
		, HINTS
		, HINTERS;
		static Logging of(final int keyModifier) {
			// upside down: HINTERS implies HINTS which implies NORMAL.
			if((keyModifier & ALT_MASK) != 0) return HINTERS; // plus timings of each hinter (timings)
			if((keyModifier & SHIFT_MASK) !=0) return HINTS; // plus log the hints used during solve (verbose)
			return NORMAL; // just the analyse summary ("normal" output)
		}
	}
	// analyse can take a while, so run it in a background thread
	// if isNoisy is true (SHIFT down) then print hints to SudokuExplainer.log
	private void analyseInTheBackground(final Logging logging) {
		setHintDetailArea("Analysing ...");
		getHintDetailPane().repaint();
		final Runnable analyser = new Runnable() {
			@Override
			public void run() {
				if ( Generator.isRunning() )
					return;
				try {
					boolean myLogHints = logging != Logging.NORMAL;
					boolean myLogHinters = logging == Logging.HINTERS;
					Run.setHinterrupt(false);
					long start = System.nanoTime();
					engine.analysePuzzle(myLogHints, myLogHinters);
					long took = System.nanoTime() - start;
					Log.teef("Analyse took %,d ns\n", took);
					hintsTreeRequestFocus();
				} catch (UnsolvableException ex) {
					displayError(ex);
				}
			}
		};
		final Thread analyserThread = new Thread(analyser, "Analyser");
		analyserThread.setDaemon(true);
		// invokeLater should allow it to repaint BEFORE starting the analyse.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					analyserThread.start();
				} catch (UnsolvableException ex) {
					displayError(ex);
				}
			}
		});
	}

	private JMenuItem getMitLibate(final int keyEvent) {
		if ( mitLibate == null ) {
			final JMenuItem jmi = newJMenuItem("Libate", keyEvent
				, "Libate");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					engine.libate();
				}
			});
			mitLibate = jmi;
		}
		return mitLibate;
	}

	// ============================= OPTIONS MENU =============================

	private JMenu getOptionsMenu() {
		if ( optionsMenu == null ) {
			final JMenu jm = newJMenu("Options", KeyEvent.VK_O
			, "Turn s__t off and on, or on and off. Whatevs.");
			jm.add(getMitFilterHints(KeyEvent.VK_F));
			jm.add(getMitCacheHints(KeyEvent.VK_H));
			jm.add(getMitShowMaybes(KeyEvent.VK_S));
			jm.add(getMitGreenFlash(KeyEvent.VK_G));
			jm.add(getMitSelectTechniques(KeyEvent.VK_T));
			jm.add(getMitHacky(KeyEvent.VK_1));
			jm.add(getMitGod(KeyEvent.VK_D));
			jm.addSeparator();
			jm.add(getMitAntiAliasing(KeyEvent.VK_A));
			jm.add(getMitSaveConfig(KeyEvent.VK_K));
			optionsMenu = jm;
		}
		return optionsMenu;
	}

	private JCheckBoxMenuItem getMitFilterHints(final int keyEvent) {
		if ( mitFilterHints == null ) {
			final String tip = getChkFilterHints().getToolTipText();
			final JCheckBoxMenuItem mi = newJCheckBoxMenuItem("Filter hints"
					, keyEvent, tip, Config.isFilteringHints, T, F, T);
			mi.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					final boolean isSelected = mitFilterHints.isSelected();
					getChkFilterHints().setSelected(isSelected);
					engine.setFiltered(isSelected);
				}
			});
			mitFilterHints = mi;
		}
		return mitFilterHints;
	}

	private JCheckBoxMenuItem getMitCacheHints(final int keyEvent) {
		if ( mitCacheHints == null ) {
			final JCheckBoxMenuItem mi = newJCheckBoxMenuItem("Cache hints"
					, keyEvent, "Make the Enter key snappy"
					, Config.isCachingHints, T, F, T);
			mi.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					setUseCache(mitCacheHints.isSelected());
				}
			});
			mitCacheHints = mi;
		}
		return mitCacheHints;
	}

	private JCheckBoxMenuItem getMitShowMaybes(final int keyEvent) {
		if ( mitShowMaybes == null ) {
			mitShowMaybes = newJCheckBoxMenuItem("Show maybes", keyEvent
					, "Display each cells potential values in small digits"
					, Config.isShowingMaybes, true, true, true);
		}
		return mitShowMaybes;
	}

	private JCheckBoxMenuItem getMitGreenFlash(final int keyEvent) {
		if ( mitGreenFlash == null ) {
			mitGreenFlash = newJCheckBoxMenuItem("Green flash", keyEvent
					, "Flash green when the puzzle solves with singles"
					, Config.isGreenFlash, true, true, true);
		}
		return mitGreenFlash;
	}

	private JMenuItem getMitSelectTechniques(final int keyEvent) {
		if ( mitSelectTechniques == null ) {
			final JMenuItem jmi = newJMenuItem("Solving techniques..."
					, keyEvent, "En/disable Sudoku solving techniques");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					selectSodokuSolvingTechniques();
				}
			});
			mitSelectTechniques = jmi;
		}
		return mitSelectTechniques;
	}

	// Display a warning icon-label when hinter/s are disabled or unavailable.
	// AlignedPent may be unavailable, as apposed to merely dis/enabled.
	// return does the warning panel need to be visible.
	boolean refreshDisabledRulesWarning() {
		// build the disabled-rules warning message
		final EnumSet<Tech> techs = CFG.getWantedTechs();
		// -2 for [The Solution, Single Solution]
		final int numDisabled = Config.ALL_TECHS.size() - 2 - techs.size();
		String msg = ""+numDisabled+" techniques disabled.";
		// all these Techs are inconveniently slow.
		if ( techs.contains(Tech.KrakenJellyfish)
		  || techs.contains(Tech.MutantJellyfish) )
			msg += " Jellylegs!";
		if ( techs.contains(Tech.AlignedSept)
		  || techs.contains(Tech.AlignedOct)
		  || techs.contains(Tech.AlignedNona)
		  || techs.contains(Tech.AlignedDec) )
			msg += " Misaligned!";
		// set the warning JLabels text
		lblDisabledTechsWarning.setText(msg);
		// make the warning panel in/visible
		// turns out I allways want to see this warning.
		disabledTechsWarnPanel.setVisible(true);
		return true;
	}

	private void selectSodokuSolvingTechniques() {
		showWelcomeHtml();
		if ( techSelectDialog == null ) {
			final JDialog dialog = new TechSelectDialog(this, engine);
			dialog.pack();
			// top-right
			final Dimension ss = getToolkit().getScreenSize();
			dialog.setLocation(ss.width - dialog.getSize().width, 0);
			techSelectDialog = (TechSelectDialog)dialog;
		}
		refreshDisabledRulesWarning();
		techSelectDialog.setVisible(true);
	}

	private JCheckBoxMenuItem getMitHacky(final int keyEvent) {
		if ( mitHacky == null ) {
			mitHacky = newJCheckBoxMenuItem("Hack top1465", keyEvent
				, "<html><body>"
				 +"Use hacks in Aligned*Exclusion for a faster solve of top1465 only"
				 +"</body></html>"
				, Config.isHacky, true, true, false);
		}
		return mitHacky;
	}

	private JCheckBoxMenuItem getMitGod(final int keyEvent) {
		if ( mitGod == null ) {
			final JCheckBoxMenuItem mi = new JCheckBoxMenuItem("God Mode");
			mi.setMnemonic(keyEvent);
			mi.setToolTipText("for lazy bastards, like me");
			mi.setEnabled(true);
			mi.setSelected(engine.god);
			mi.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					// god is non-persistant. A shame really.
					// cok vs god decomposes into non-determinism.
					engine.cheater = engine.god = mitGod.isSelected() && god();
				}
			});
			mitGod = mi;
		}
		return mitGod;
	}

	boolean god() {
		return "IDDQD".equals("I" + askForString("assword:", "God", "DDQ"));
	}

	private JCheckBoxMenuItem getMitAntiAliasing(final int keyEvent) {
		if ( mitAntialiasing == null ) {
			mitAntialiasing = newJCheckBoxMenuItem("High Quality Rendering"
				, keyEvent, "Use anti-aliasing, which is indiscernably slower on decent GPUs"
				, Config.isAntialiasing, true, true, true);
		}
		return mitAntialiasing;
	}

	private JMenuItem getMitSaveConfig(final int keyEvent) {
		if ( mitSaveConfig == null ) {
			final JMenuItem mi = newJMenuItem("Save the config", keyEvent
				, "Persist the configuration, so SE is like now next time");
			mi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					CFG.save();
				}
			});
			mitSaveConfig = mi;
		}
		return mitSaveConfig;
	}

	// ============================= HELP MENU =============================

	private JMenu getHelpMenu() {
		if ( helpMenu == null ) {
			final JMenu m = newJMenu("Help", KeyEvent.VK_H
					, "Help me Rhonda, Help help me Rhonda... ");
			final JMenuItem mi = getMitShowWelcome(KeyEvent.VK_W);
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
			m.add(mi);
			m.addSeparator();
			m.add(getMitAbout(KeyEvent.VK_A));
			helpMenu = m;
		}
		return helpMenu;
	}

	private JMenuItem getMitShowWelcome(final int keyEvent) {
		if ( mitShowWelcome == null ) {
			final JMenuItem jmi = newJMenuItem("Show welcome message", keyEvent
					, "Display that big long welcome message");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					showWelcomeHtml();
				}
			});
			mitShowWelcome = jmi;
		}
		return mitShowWelcome;
	}

	private JMenuItem getMitAbout(final int keyEvent) {
		if ( mitAbout == null ) {
			final JMenuItem jmi = newJMenuItem("About", keyEvent
					, "Sudoku Explainer application version number");
			jmi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					if ( dummyFrameKnife == null ) {
						final JFrame dfk = new JFrame();
						final ImageIcon icon = createImageIcon("Icon_Knife.gif");
						dfk.setIconImage(icon.getImage());
						dummyFrameKnife = dfk;
					}
					final AboutDialog ad = new AboutDialog(dummyFrameKnife);
					// top-right
					ad.setLocation(getToolkit().getScreenSize().width
							- ad.getSize().width, 0);
					ad.setVisible(true);
				}
			});
			mitAbout = jmi;
		}
		return mitAbout;
	}

	SudokuGridPanel getGridPanel() {
		return gridPanel;
	}

	// =============================== HELPERS ================================

	private JMenu newJMenu(final String text, final int mnemonic, final String toolTipText) {
		final JMenu menu = new JMenu(text);
		menu.setMnemonic(mnemonic);
		menu.setToolTipText(toolTipText);
		return menu;
	}

	private JMenuItem newJMenuItem(final String text, final int mnemonic, final String toolTipText) {
		final JMenuItem item = new JMenuItem(text, mnemonic);
		item.setToolTipText(toolTipText);
		return item;
	}

	private JCheckBoxMenuItem newJCheckBoxMenuItem(final String text
			, final int mnemonic, final String toolTipText
			, final String settingName, final boolean isEnabled
			, final boolean wantStandardListener
			, final boolean defualtSetting) {
		final JCheckBoxMenuItem mi = new JCheckBoxMenuItem(text);
		mi.setMnemonic(mnemonic);
		mi.setToolTipText(toolTipText);
		mi.setSelected(CFG.getBoolean(settingName, defualtSetting));
		mi.setEnabled(isEnabled);
		if ( wantStandardListener ) {
			mi.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					CFG.setBoolean(settingName, mi.isSelected());
					repaint();
				}
			});
		}
		return mi;
	}

	private JButton newJButton(final String text, final int mnemonic, final String toolTipText) {
		final JButton btn = new JButton(text);
		btn.setMnemonic(mnemonic);
		btn.setToolTipText(toolTipText);
		return btn;
	}

	// =============================== PLUMBING ===============================

	/**
	 * Clean-up everything upon shutdown - specifically closes the
	 * SudokuExplainer which saves the RecentFiles list.
	 */
	@Override
	public void dispose() {
		CFG.setBounds(SudokuFrame.this.getBounds());
		SudokuFrame.this.setVisible(false);
		try {
			if ( engine != null ) {
				// engine is final so no setting it null, but stuff it closes
				// is nullable so subsequent engine.close() calls is a no-op.
				engine.close();
			}
		} catch (IOException ex) {
//			engine.whinge(ex);
		}
		if ( dummyFrameKnife != null ) {
			dummyFrameKnife.dispose();
			dummyFrameKnife = null;
		}
		if ( generateDialog != null ) {
			generateDialog.dispose();
			generateDialog = null;
		}
		if ( recentFilesDialog != null ) {
			recentFilesDialog.dispose();
			recentFilesDialog = null;
		}
		if ( techSelectDialog != null ) {
			techSelectDialog.dispose();
			techSelectDialog = null;
		}
		super.dispose();
	}

	// ================================ ASKER =================================

	/**
	 * Ask the user a Yes/No question.
	 *
	 * @param question
	 * @return
	 */
	@Override
	public boolean ask(final String question) {
		return ask(question, getTitle());
	}

	/**
	 * Ask the user a Yes/No question with title.
	 *
	 * @param question
	 * @param title
	 * @return
	 */
	@Override
	public boolean ask(final String question, final String title) {
		return showConfirmDialog(this, question, title, YES_NO_OPTION)
				== YES_OPTION;
	}

	/**
	 * Ask the user for some String input.
	 *
	 * @param question
	 * @param title
	 * @return
	 */
	@Override
	public String askForString(final String question, final String title) {
		final String q = question.replaceAll("\\n", NL); // sigh
		return showInputDialog(this, q, title, PLAIN_MESSAGE);
	}

	/**
	 * Ask the user for some String input, providing a default.
	 *
	 * @param question
	 * @param title
	 * @param defualt
	 * @return
	 */
	@Override
	public String askForString(final String question, final String title, final String defualt) {
		return (String)showInputDialog(this, question, title, PLAIN_MESSAGE
				, null, (Object[])null, (Object)defualt);
	}

	/**
	 * Ask the user for an integer between min and max.
	 *
	 * @param question the Question to ask the user
	 * @param min the minimum value, inclusive
	 * @param max the maximum value, inclusive. Note that max must be >= min.
	 * @return the int from the user
	 */
	@Override
	public int askForInt(final String question, int min, int max) {
		assert min <= max;
		final String qstn = question.replaceAll("\\n", NL);
		final String rtry = qstn+NL+"An integer between "+min+AND+max+" is required.";
		final String dflt = "Please enter an integer between "+min+AND+max;
		String rspc = JOptionPane.showInputDialog(this, qstn, dflt);
		while ( true ) {
			if ( rspc!=null && rspc.length()>0 ) {
				try {
					final int value = Integer.parseInt(rspc);
					if ( value >= min && value <= max )
						return value;
				} catch (NumberFormatException ex) {
//Irrelevant so eaten
//					StdErr.whinge(ex);
					beep();
				}
			}
			rspc = JOptionPane.showInputDialog(this, rtry, ""+min);
		}
	}

	void msgBox(final String msg, final String title) {
		showMessageDialog(this, msg, title, INFORMATION_MESSAGE);
	}

	/**
	 * I was trying to Ask.god(42) but he threw a NullPointerException!
	 *
	 * @return how many assbitrarians does it take run a real-estate scam?
	 */
	boolean cheat() {
		return "IDKFA".equals("I" + askForString("assword:", "Cheat", "DKF"));
	}

	/**
	 * Complain to the user: with String $msg and $title.
	 *
	 * @param msg
	 * @param title
	 */
	@Override
	public void carp(final String msg, final String title) {
		showMessageDialog(this, msg, title, ERROR_MESSAGE);
	}

	/**
	 * Complain to the user: with Exception $ex and $title.
	 *
	 * @param ex
	 * @param title
	 */
	@Override
	public void carp(final Exception ex, final String title) {
		String msg = ex.toString();
		for ( Throwable t=ex.getCause(); t!=null; t=t.getCause() ) {
			msg += NL + t.toString();
		}
		showMessageDialog(this, msg, title, ERROR_MESSAGE);
	}

	// ================================ CHEAT =================================

	// this method is package visible to be called by SudokuGridPanel,
	// which sets my text to the cheat-name in it is mouse-over event
	JLabel getLblCheatName() {
		if ( lblCheatName == null ) {
			lblCheatName = new JLabel();
		}
		return lblCheatName;
	}

}
