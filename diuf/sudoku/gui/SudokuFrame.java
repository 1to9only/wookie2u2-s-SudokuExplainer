/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Build;
import static diuf.sudoku.Settings.*;

import diuf.sudoku.Grid;
import diuf.sudoku.PuzzleID;
import diuf.sudoku.Settings;
import diuf.sudoku.Tech;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.IAsker;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.awt.*;
import static java.awt.Event.ALT_MASK;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import static java.awt.Event.CTRL_MASK;
import static java.awt.Event.SHIFT_MASK;
import static java.awt.event.MouseEvent.BUTTON1;
import static java.awt.event.MouseEvent.BUTTON2;
import static java.awt.event.MouseEvent.BUTTON3;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessControlException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.tree.*;

/**
 * The SudokuFrame is the main window of the application.
 * <p>
 * Pretty much all the actions are delegated to the {@link SudokuExplainer}
 * class. The GUI handles files itself because it's easier to display errors
 * from within the GUI.
 * <p>
 * <b>Programmers Hidden GUI Features</b><br>
 * I thought I should write a list of all the "handy stuff" I built into the GUI
 * while debugging this project and playing with Sudoku puzzles, so some handy
 * features are:<ul>
 * <li>You can Alt-right-click on a cell to set it's background grey (move the
 * mouse to another cell and you'll see it), and Alt-right-click again to remove
 * it (ie a toggle). This allows you to highlight cells in a hint, which is
 * pretty handy when you have a log entry describing the hint, but the current
 * version of the software doesn't produce the damn hint any longer, and that's
 * why you want to look at the damn hint, which you can't see. Sigh.
 * <li>You can select hint/s in the hintsTree and then right-click to copy all
 * selected hints toFullString to the clipboard, which is handy. Don't forget to
 * copy the grid as well. And now they even come with there Tech.name().
 * <li>You can Ctrl-right-click in the hintsTree to copy all UNFILTERED hints to
 * the clipboard. Now includes Tech.name(). Don't forget the grid!
 * <li>You can right-click in the hintDetailPanel to copy the hint-HTML to the
 * clipboard, so you can paste it into a .html file and view it your browser,
 * which I find handy because my browser enables me to enlarge the font.
 * <li>Good luck with A*E, and may all of your camels be well watered.
 * </ul>
 */
@SuppressWarnings("Convert2Lambda") // I prefer to treat all warnings as errors, and replacing inner classes with lambda expressions is more trouble than it's worth (in my humble opinion), so this warning message should ONLY apply to new projects, not existing code-bases, but Netbeans authors lack the intelligence to see that.
final class SudokuFrame extends JFrame implements IAsker {

	private static final long serialVersionUID = 8247189707924329043L;

	private static final Font DIALOG_BOLD_12 = new Font("Dialog", Font.BOLD, 12);

	// the clearHintsTree method may happen often, it depends on the user.
	private static final List<AHint> EMPTY_HINTS_LIST = Collections.emptyList();
	private static final HintNode EMPTY_HINTS_TREE
			= new HintsTreeBuilder().build(EMPTY_HINTS_LIST);

	private final SudokuExplainer engine;
	public final SudokuGridPanel gridPanel;

	private GenerateDialog generateDialog; // Generate Sudoku Puzzles Dialog
	private TechSelectDialog techSelectDialog; // Solving Techniques Dialog
	private RecentFilesDialog recentFilesDialog; // Recently Used Files Dialog

	private JFrame dummyFrameKnife;
	private JPanel contentPane, viewSelectPanel, hintsTreePanel, northPanel
		, sudokuGridPanelHolder, hintDetailPanel, buttonsPane, buttonsPanel
		, hintsSouthPanel, ratingPanel, disabledTechsWarnPanel;
	private JScrollPane hintDetailsScrollPane, hintsTreeScrollpane;
	private JTree hintsTree;
	private JEditorPane hintDetailPane;
	private String hintDetailHtml; // contents of hintDetailArea because when
	// you get it back out of the JEditorPane it's been wrapped, badly, and we
	// want to copy it to the clipboard so that it can be re-used, or whatever.
	private JButton btnGetAllHints, btnSolveStep, btnGetNextHint, btnValidate
		, btnApplyHint;
	private JCheckBox chkFilterHints;
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
		, mitSolve, mitAnalyse, mitAnalyseVerbose, mitAnalyseTiming, mitLogView;
	// Options
	private JMenu optionsMenu, mitLookAndFeel; // a sub-menu under optionsMenu
	private JMenuItem mitSelectTechniques, mitSaveSettings;
	private JCheckBoxMenuItem mitFilterHints, mitShowMaybes, mitGreenFlash
		, mitAntialiasing, mitHacky, mitGodMode;
	// Help
	private JMenu helpMenu;
	private JMenuItem mitShowWelcome, mitAbout;

	private String welcomeText; // loaded once and stored here for redisplay
	private AHint currHint; // the selected hint, nullable
	private int viewCount, viewNum = -1; // a hint can have >1 "views" in grid

	private File logViewFile = null;
	private String regex;

	/**
	 * The directory which the open-file and save-file dialogs start in.
	 */
	File defaultDirectory;
	
	public final boolean logHints = true;
	public final boolean printHints = true;

	/**
	 * The Constructor. Note that it's only package visible, for use only by the
	 * SudokuExplainer (controller) to create the view (me). The model is the
	 * Grid class, and it's assorted cheese. We pass around instances of the
	 * model (grids) instead of having the GUI and controller lock-onto an
	 * instance of the model, which is the more usual approach.
	 */
	SudokuFrame(Grid grid, SudokuExplainer engine) {
		super();
		this.engine = engine;
		this.gridPanel = new SudokuGridPanel(grid, engine, this);

		initialise();
		resetViewSelector();
		AutoBusy.addFullAutoBusy(this);
		showWelcomeText();
		setIconImage(createImage("Icon_Sudoku.gif"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		// inform user if any Sudoku Solving Techniques are disabled
		disabledTechsWarnPanel.setVisible(refreshDisabledRulesWarning());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				gridPanel.requestFocusInWindow();
			}
		});
	}

	private void initialise() {
		this.setTitle(Build.ATV);
		setupLookAndFeelMenu();
		this.setJMenuBar(getMyMainMenuBar());
		this.setContentPane(getJContentPane());
		setDropTargetToRecievePlainTextString();
	}

	private void setDropTargetToRecievePlainTextString() {
		this.setDropTarget(new DropTarget(gridPanel, new DropTargetAdapter() {
			@Override
			public void drop(DropTargetDropEvent dtde) {
				try {
					String stringData = IO.readStringFromDropEvent(dtde);
					//System.out.println("SudokuFrame.drop: plain/text;java.lang.String:\n"+stringData);
					engine.loadStringIntoGrid(stringData);
					gridPanel.repaint();
					setTitle(Build.ATV + "   (dropped in)");
					hintDetailPane.setText("Sudoku dropped!");
				} catch (UnsupportedFlavorException | IOException ex) {
					displayError(ex);
				}
			}
		}));
	}

	/**
	 * Displays the welcome to my nightmare message in the hint-details area.
	 */
	final void showWelcomeText() {
		if (welcomeText == null) // cached up here, not in the Html class.
		{
			welcomeText = Html.load(this, "Welcome.html", true, false);
		}
		engine.clearHints();
		setHintDetailArea(welcomeText);
	}

	final Image createImage(String path) {
		return createImageIcon(path).getImage();
	}

	final ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = SudokuFrame.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			StdErr.whinge("Couldn't find file: " + path);
			return null;
		}
	}

	/**
	 * Sets the contents of the hintsTree JTree control.
	 */
	void setHintsTree(HintNode root, HintNode selected) {
		getHintsTree();
		hintsTree.setEnabled(false);
		hintsTree.setModel(new DefaultTreeModel(root));
		// Dis/enable the Filter checkbox and menu item.
		chkFilterHints.setSelected(THE_SETTINGS.get(Settings.isFilteringHints));
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
	 * Repaints the SudokuGridPanel with the currHint.
	 */
	private void repaintHint() {
		final AHint h = currHint; // h for Hint
		SudokuGridPanel p = gridPanel; // p for panel
		p.clearSelection(true);
		if ( h == null ) { // clear it
			p.setResult(null);
			p.setResults(null);
			p.setResultColor(-1); // meaning none
			p.setAquaBGCells(null);
			p.setPinkBGCells(null);
			p.setPinkRegions(null);
			p.setRedBGCells(null);
			p.setGreenBGCells(null);
			p.setOrangeBGCells(null);
			p.setBlueBGCells(null);
			p.setYellowBGCells(null);
			p.setGreenPots(null);
			p.setRedPots(null);
			p.setOrangePots(null);
			p.setBluePots(null);
			p.setYellowPots(null);
			p.setPurplePots(null);
			p.setBrownPots(null);
			p.setAlss(null);
			p.setLinks(null);
			p.setBases(null);
			p.setCovers(null);
			p.setSupers(null);
			p.setSubs(null);
		} else { // display it
			p.setResult(h.getResult());
			p.setResults(h.getResults());
			p.setResultColor(h.getResultColor());
			p.setAquaBGCells(h.getAquaCells(viewNum)); // null OK
			p.setPinkBGCells(h.getPinkCells(viewNum)); // null OK
			p.setPinkRegions(h.getPinkRegions()); // null OK
			if ( h instanceof AWarningHint )
				p.setRedBGCells(((AWarningHint) h).getRedCells());
			p.setGreenBGCells(h.getGreenCells(viewNum)); // null OK
			p.setOrangeBGCells(h.getOrangeCells(viewNum)); // null OK
			p.setBlueBGCells(h.getBlueCells(viewNum)); // null OK
			p.setYellowBGCells(h.getYellowCells(viewNum)); // null OK
			p.setGreenPots(h.getGreens(viewNum));
			p.setRedPots(h.getReds(viewNum));
			p.setOrangePots(h.getOranges(viewNum));
			p.setBluePots(h.getBlues(p.getGrid(), viewNum));
			// The three extra colors are currently only in the ColoringHint,
			// but other hint-types may use them in future, so don't go round.
			p.setYellowPots(h.getYellows());
			p.setPurplePots(h.getPurples());
			p.setBrownPots(h.getBrowns());
			p.setAlss(h.getAlss());
			// rendered as brown arrows
			p.setLinks(h.getLinks(viewNum));
			p.setBases(h.getBases());
			p.setCovers(h.getCovers());
			p.setSupers(h.getOns());
			p.setSubs(h.getOffs());
		}
		p.repaint();
	}

	private static final DecimalFormat RATING = new DecimalFormat("#0.00");

	// I suspect that 128 is bigger than the biggest hint, but now it's
	// bounded so that it can't run away towards infinity and break s__t.
	// If 128 proves too small then double it again. There's no problem
	// with it being bigger (within reason) it just MUST be bounded.
	private final static int MAX_VIEWS = 128;
	private static final String[] VIEWS = new String[MAX_VIEWS];

	static {
		for (int i = 0; i < MAX_VIEWS; ++i) {
			VIEWS[i] = "View " + (i + 1);
		}
	}

	private int bounded(int vc) { // viewCount
		if (vc < 1) {
			vc = 1;
		} else if (vc > MAX_VIEWS) {
			// this goes to StdErr, to tell the developer we have a problem.
			engine.whinge("SudokuFrame: viewCount=" + vc + " > MAX_VIEWS=" + MAX_VIEWS);
			vc = MAX_VIEWS;
		}
		return vc;
	}

	/**
	 * Displays the given hint.
	 */
	void setCurrentHint(AHint hint, boolean isApplyEnabled) {
		this.currHint = hint;
		btnApplyHint.setEnabled(isApplyEnabled);
		mitApplyHint.setEnabled(isApplyEnabled);
		viewNum = 0;
		if (hint == null) {
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
			if ( !(hint instanceof IPretendHint) ) {
				lblPuzzleRating.setText(RATING.format(hint.getDifficulty()));
			}
		}
		repaintHint();
		this.repaint();
	}

	private void resetViewSelector() {
		cmbViewSelector.setEnabled(false);
		cmbViewSelector.removeAllItems();
		for (int i = 0; i < viewCount; ++i) {
			cmbViewSelector.addItem(VIEWS[i]);
		}
		cmbViewSelector.setSelectedIndex(viewNum);
		cmbViewSelector.setEnabled(viewCount > 1);
	}

	// Display a warning icon-label when hinter/s are disabled or unavailable.
	// AlignedPent may be unavailable, as apposed to merely dis/enabled.
	// return does the warning panel need to be visible.
	boolean refreshDisabledRulesWarning() {
		// build the disabled-rules warning message
		EnumSet<Tech> wanted = THE_SETTINGS.getWantedTechs();
		// -2 for [The Solution, Single Solution]
		final int numDisabled = Settings.ALL_TECHS.size() - 2 - wanted.size();
		String msg = ""+numDisabled+" techniques disabled.";
		// these Tech's are far too slow for practical use.
		if ( wanted.contains(Tech.KrakenJellyfish)
		  || wanted.contains(Tech.MutantJellyfish) )
			msg += " Jellylegs!";
		// these Tech's are too slow for practical use.
		if ( wanted.contains(Tech.AlignedSept)
		  || wanted.contains(Tech.AlignedOct)
		  || wanted.contains(Tech.AlignedNona)
		  || wanted.contains(Tech.AlignedDec) )
			msg += " Megaligned!";
		// set the warning JLabel's text
		lblDisabledTechsWarning.setText(msg);
		// make the warning panel in/visible
		// turns out I allways want to see this warning.
		disabledTechsWarnPanel.setVisible(true);
		return true;
	}

	private final class HintsTreeCellRenderer implements TreeCellRenderer {

		// this Renderer "wraps" a DefaultTreeCellRenderer to handle HintNodes
		private final DefaultTreeCellRenderer DTCR
				= new DefaultTreeCellRenderer();

		public HintsTreeCellRenderer() {
			DTCR.setLeafIcon(createImageIcon("Icon_Light.gif"));
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value
				, boolean selected, boolean expanded, boolean leaf, int row
				, boolean hasFocus) {
			if (value instanceof HintNode) {
				HintNode hn = (HintNode) value;
				boolean isEmpty = !hn.isHintNode() && hn.getChildCount() == 0;
				expanded |= isEmpty;
				leaf &= !isEmpty;
			}
			return DTCR.getTreeCellRendererComponent(tree, value, selected
					, expanded, leaf, row, hasFocus);
		}
	}

	private void setupLookAndFeelMenu() {
		String lookAndFeelName = THE_SETTINGS.getLookAndFeelClassName();
		if (lookAndFeelName == null) {
			lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
		}
		ButtonGroup group = new ButtonGroup();
		boolean firstError = true;
		for ( LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels() ) {
			final JRadioButtonMenuItem menuItem
					= new JRadioButtonMenuItem(lafi.getName());
			menuItem.setName(lafi.getClassName());
			try {
				Class<?> lafiClass = Class.forName(lafi.getClassName());
				LookAndFeel instance = (LookAndFeel) lafiClass.newInstance();
				menuItem.setToolTipText(instance.getDescription());
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
				if (firstError) {
					engine.whinge(ex); // full stack trace
					firstError = false;
				} else {
					engine.carp("Buggered again!", ex); // a one-liner
				}
			}
			group.add(menuItem);
			getMitLookAndFeel().add(menuItem);
			if (lafi.getClassName().equals(lookAndFeelName)) {
				menuItem.setSelected(true);
			}
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!menuItem.isSelected()) {
						return;
					}
					String lafClassName = menuItem.getName();
					try {
						UIManager.setLookAndFeel(lafClassName);
						THE_SETTINGS.setLookAndFeelClassName(lafClassName);
						SwingUtilities.updateComponentTreeUI(SudokuFrame.this);
						// recreate the renderer to reload the correct icons
						hintsTree.setCellRenderer(new HintsTreeCellRenderer());
						SudokuFrame.this.repaint();
						GenerateDialog gd = generateDialog;
						if (gd != null && gd.isVisible()) {
							SwingUtilities.updateComponentTreeUI(gd);
							gd.pack();
							gd.repaint();
						}
					} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
						displayError(ex);
					}
				}
			});
		}
	}

	private JPanel getJContentPane() {
		if (contentPane == null) {
			contentPane = new JPanel(new BorderLayout());
			contentPane.add(getNorthPanel(), BorderLayout.NORTH);
			contentPane.add(getHintDetailPanel(), BorderLayout.CENTER);
			contentPane.add(getButtonsContainer(), BorderLayout.SOUTH);
		}
		return contentPane;
	}

	private JScrollPane getHintDetailsScrollPane() {
		if (hintDetailsScrollPane == null) {
			hintDetailsScrollPane = new JScrollPane();
			if (getToolkit().getScreenSize().height < 800) {
				hintDetailsScrollPane.setPreferredSize(new Dimension(700, 110));
			} else {
				hintDetailsScrollPane.setPreferredSize(new Dimension(1000, 510));
			}
			hintDetailsScrollPane.setViewportView(getHintDetailPane());
		}
		return hintDetailsScrollPane;
	}

	private ArrayList<HintNode> getSelectedHintNodes() {
		ArrayList<HintNode> result = new ArrayList<>();
		TreePath[] pathes = hintsTree.getSelectionPaths();
		if (pathes != null) {
			for (TreePath path : pathes) {
				result.add((HintNode) path.getLastPathComponent());
			}
		}
		return result;
	}

	/**
	 * and remove all hints from the given deadTech; and disable that hinter,
	 * until a puzzle is re/loaded (prepare).
	 *
	 * @param deadTech
	 */
	private void removeHintsAndDisableHinter(Tech deadTech) {
		// disable the hinter; which is re-enabled by loading a puzzle
		IHinter hinter = engine.solver.findWantedHinter(deadTech);
		if (hinter != null) {
			hinter.setIsEnabled(false);
		}
		// remove the hints
		engine.removeHintsFrom(deadTech);
	}

	private JTree getHintsTree() {
		if (hintsTree != null) {
			return hintsTree;
		}
		hintsTree = new JTree();
		hintsTree.setShowsRootHandles(true);
		hintsTree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		hintsTree.setCellRenderer(new HintsTreeCellRenderer());
		hintsTree.setExpandsSelectedPaths(true);
		hintsTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (hintsTree.isEnabled()) {
					engine.hintsSelected(getSelectedHintNodes());
				}
			}
		;
		});
		// WARN: This KeyListener is replicated in the hintDetailPane. If you
		// change here then change there too; or methodise it all (BFIIK).
		hintsTree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				final int keyCode = e.getKeyCode();
				if ( keyCode == KeyEvent.VK_ENTER ) {
					applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
					e.consume();
				} else if ( keyCode == KeyEvent.VK_DELETE ) {
					ArrayList<HintNode> hintNodes = getSelectedHintNodes();
					if ( hintNodes!=null && !hintNodes.isEmpty() ) {
						hintsTree.clearSelection();
						// disable this hinter
						Tech deadTech = hintNodes.get(0).getHint().hinter.tech;
						removeHintsAndDisableHinter(deadTech);
						// clear the hintsTree
						hintsTree.setModel(new DefaultTreeModel(null));
						repaint();
						// get hints again now that this hinter is disabled
						getAllHintsInBackground(e.isShiftDown(), e.isControlDown());
					}
					e.consume();
				}
			}
		});
		hintsTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int btn = e.getButton();
				int cnt = e.getClickCount();
				int mod = e.getModifiersEx();
				if ( btn==BUTTON1 && cnt==2 ) {
					// double-left-click: as per VK_ENTER
					applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
					e.consume();
				} else if ( btn==BUTTON3 && cnt==1 ) {
					// single-right-click
					if ( (mod & InputEvent.CTRL_DOWN_MASK) != 0 ) {
						// Ctrl-single-right-click
						if (!engine.copyUnfilteredHintsListToClipbard()) {
							engine.beep();
						}
					} else {
						// plain-single-right-click
						if (!engine.copyToClipboard(getSelectedHintNodes())) {
							engine.beep();
						}
					}
				}
			}
		});
		return hintsTree;
	}

	/**
	 * Invokes engine.getAllHints(wantMore) in a background thread, because it
	 * may take a while (in the order of 10 seconds or 15 seconds, or upto 2
	 * minutes if you wantMore), so we can't have it blocking the EDT.
	 * <p>
	 * Called by hintsTree.VK_ENTER, applySelectedHintsAndGetNextHint,
	 * btnGetAllHints & mitGetAllHints, and mitGetMoreAllHints.
	 *
	 * @param wantMore are more hints wanted
	 */
	private void getAllHintsInBackground(final boolean wantMore, final boolean wantSolution) {
		// One at a time please! I'm blocking manually with a boolean,
		// because synchronized can't block accross threads.
		if ( isGettingAllHints )
			return;
		isGettingAllHints = true;
		setHintDetailArea("Searching for" + (wantMore ? " MORE" : "") + " hints...");
		repaint();
		Thread getAllHintsThread = new Thread("getAllHints") {
			@Override
			public void run() {
				engine.getAllHints(wantMore, wantSolution, logHints, printHints);
				isGettingAllHints = false;
				hintsTreeRequestFocus();
			}
		};
		getAllHintsThread.setDaemon(true);
		getAllHintsThread.start();
	}
	private volatile boolean isGettingAllHints = false;

	private void applySelectedHintsAndGetNextHint(boolean wantMore, boolean wantSolution) {
		try {
			if ( engine.getGrid().isFull() ) {
				setCurrentHint(engine.solver.SOLVED_HINT, false);
			} else {
				engine.applySelectedHints(); // throws UnsolvableException
				getAllHintsInBackground(wantMore, wantSolution);
			}
		} catch (IllegalStateException eaten) {
			// already logged
		} catch (UnsolvableException ex) {
//			// UnsolvableException: the grid is rooted! Either the given puzzle
//			// is invalid, or there's a bug leading to an invalid hint being
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
	public void logError(Exception ex) {
		// wait for stdout so stderr doesn't interleave
		try {
			Thread.sleep(1);
		} catch (InterruptedException eaten) {
		}
		Log.whinge("GUI Error!", ex);
	}

	/**
	 * Displays the stackTrace of the given Exception in the hintDetailArea and
	 * prints it to the standard-error stream.
	 *
	 * @param ex Exception to display.
	 */
	public void displayError(Exception ex) {
		// wait for stdout so stderr doesn't interleave
		try {
			Thread.sleep(1);
		} catch (InterruptedException eaten) {
		}
		System.err.println();
		ex.printStackTrace(System.err);
		System.err.println();
		System.err.flush();
		// get html of the stack trace
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String html = "<html><body><font color=\"red\"><pre>"
				+ sw.toString()
				+ "</pre></font></body></html>";
		setHintDetailArea(html);
		engine.beep();
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
		if (htmlText != null && htmlText.length() == 0) {
			htmlText = null;
		}
		JEditorPane hdp = getHintDetailPane();
		// remember the html ourselves because hintDetailHtml.getText() wraps!
		hdp.setText(hintDetailHtml = htmlText);
		hdp.setCaretPosition(0);
		// same length as a difficulty or the label jumps around distractingly
		lblPuzzleRating.setText("0.00");
	}

	private static final Pattern CELL_ID_PATTERN = Pattern.compile("[A-I][1-9]");

	private JEditorPane getHintDetailPane() {
		if (hintDetailPane != null) {
			return hintDetailPane;
		}
		hintDetailPane = new JEditorPane("text/html", null) {
			private static final long serialVersionUID = -5658720148768663350L;
			@Override
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				super.paint(g);
			}
		};
		hintDetailPane.setEditable(false);
		hintDetailPane.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				final int btn = e.getButton();
				final int clks = e.getClickCount();
				// right-click: copy the html-text to the clipboard
				if ( clks==1 && (btn==BUTTON2 || btn==BUTTON3) ) {
					if (hintDetailHtml != null)
						engine.copyToClipboard(hintDetailHtml);
					else
						engine.beep();
					e.consume();
				// double-left-click: if the selected text looks like a cell id
				// then focus on (yellow background) that cell in the grid.
				} else if ( clks==2 && btn==BUTTON1 ) {
					final String id = hintDetailPane.getSelectedText();
					if ( id.length()==2 && CELL_ID_PATTERN.matcher(id).matches() )
						gridPanel.setFocusedCellS(id);
				}
			}
		});
		// make keys do same in hintDetailPane as in the hintsTree
		hintDetailPane.addKeyListener(new KeyAdapter() {
			private String target;
			private int place;
			@Override
			public void keyPressed(KeyEvent e) {
				final int keyCode = e.getKeyCode();
				if ( keyCode == KeyEvent.VK_ENTER ) {
					applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
					e.consume();
				} else if ( keyCode == KeyEvent.VK_DELETE ) {
					// this mess averts all possible NPE's getting deadTech.
					// It's ugly but ultimate-safe.
					try {
						final ArrayList<HintNode> nodes = getSelectedHintNodes();
						if ( nodes!=null && !nodes.isEmpty() ) {
							final HintNode node = nodes.get(0);
							if ( node != null ) {
								final AHint hint = node.getHint();
								if ( hint != null ) {
									final AHinter hinter = hint.hinter;
									if ( hinter != null ) {
										final Tech deadTech = hinter.tech;
										if ( deadTech != null ) {
											removeHintsAndDisableHinter(deadTech);
											hintsTree.clearSelection();
											repaint();
										}
									}
								}
							}
						}
					} catch (Exception eaten) {
						// Do nothing, no biggy, it's only a hack
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
				if ( target==null || target.isEmpty() ) {
					place = -1;
				} else {
					try {
						final Caret caret = hintDetailPane.getCaret();
						final Document doc = hintDetailPane.getDocument();
						final String text = doc.getText(0, doc.getLength());
						place = text.indexOf(target);
						if ( place == -1 ) {
							caret.setDot(0);
							engine.beep();
						} else {
							caret.setDot(place);
							caret.moveDot(place+target.length());
						}
					} catch (BadLocationException ex) {
						// Do nothing
					}
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
						engine.beep();
					} else {
						caret.setDot(place);
						caret.moveDot(place+target.length());
					}
				} catch (BadLocationException ex) {
					// Do nothing
				}
			}
		});
		return hintDetailPane;

	}

	private JScrollPane getHintsTreeScrollPane() {
		if (hintsTreeScrollpane == null) {
			hintsTreeScrollpane = new JScrollPane();
			hintsTreeScrollpane.setPreferredSize(new Dimension(100, 100));
			hintsTreeScrollpane.setViewportView(getHintsTree());
		}
		return hintsTreeScrollpane;
	}

	private JPanel getNorthPanel() {
		if (northPanel == null) {
			northPanel = new JPanel(new BorderLayout());
			northPanel.add(getSudokuGridPanelHolder(), BorderLayout.WEST);
			northPanel.add(getHintsTreePanel(), BorderLayout.CENTER);
		}
		return northPanel;
	}

	private JPanel getSudokuGridPanelHolder() {
		if (sudokuGridPanelHolder == null) {
			sudokuGridPanelHolder = new JPanel(new BorderLayout());
			sudokuGridPanelHolder.add(gridPanel, BorderLayout.CENTER);
			sudokuGridPanelHolder.add(getViewSelectionPanel(), BorderLayout.SOUTH);
		}
		return sudokuGridPanelHolder;
	}

	private JPanel getHintDetailPanel() {
		if (hintDetailPanel == null) {
			hintDetailPanel = new JPanel(new BorderLayout());
			hintDetailPanel.add(getHintDetailsScrollPane(), BorderLayout.CENTER);
		}
		return hintDetailPanel;
	}

	private GridBagConstraints newGBC(int gridx, int gridy) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gridx;
		gbc.weightx = 1.0D;
		gbc.gridy = gridy;
		return gbc;
	}

	private JPanel getButtonsPane() {
		if (buttonsPane == null) {
			buttonsPane = new JPanel(new GridBagLayout());
			buttonsPane.add(getBtnValidate(), newGBC(0, 0));
			buttonsPane.add(getBtnSolveStep(), newGBC(1, 0));
			buttonsPane.add(getBtnGetNextHint(), newGBC(2, 0));
			buttonsPane.add(getBtnApplyHint(), newGBC(4, 0));
			buttonsPane.add(getBtnGetAllHints(), newGBC(5, 0));
		}
		return buttonsPane;
	}

	private JButton getBtnGetNextHint() {
		if (btnGetNextHint == null) {
			btnGetNextHint = newJButton("Get next hint", KeyEvent.VK_N
					, "Get the next bloody hint");
			btnGetNextHint.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.invokeGetNextHint(false, false);
				}
			});
		}
		return btnGetNextHint;
	}

	private JButton getBtnGetAllHints() {
		if (btnGetAllHints == null) {
			btnGetAllHints = newJButton("Get all hints", KeyEvent.VK_A
					, "Get all hints applicable on the current situation");
			btnGetAllHints.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int mod = e.getModifiers();
					boolean wantMore = (mod & SHIFT_MASK) != 0;
					boolean wantSolution = (mod & CTRL_MASK) != 0;
					getAllHintsInBackground(wantMore, wantSolution);
				}
			});
		}
		return btnGetAllHints;
	}

	/**
	 * @return the "Solve Step" JButton, which applies the currently selected
	 * hint, if any, and then searches the grid for the next hint. Package
	 * visible so that SudokuPanel.keyTyped can give me the focus.
	 */
	JButton getBtnSolveStep() {
		if (btnSolveStep == null) {
			btnSolveStep = newJButton("Solve step", KeyEvent.VK_S
					, "Apply the selected hint (if any) and get the next one");
			btnSolveStep.setFont(DIALOG_BOLD_12);
			btnSolveStep.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveStep(false);
				}
			});
		}
		return btnSolveStep;
	}

	private JPanel getButtonsContainer() {
		if (buttonsPanel == null) {
			buttonsPanel = new JPanel(new GridLayout(1, 1));
			buttonsPanel.add(getButtonsPane(), null);
		}
		return buttonsPanel;
	}

	private JPanel getViewSelectionPanel() {
		if (viewSelectPanel == null) {
			viewSelectPanel = new JPanel(new FlowLayout());
			viewSelectPanel.add(getCmbViewSelector());
		}
		return viewSelectPanel;
	}

	private JPanel getHintsTreePanel() {
		if (hintsTreePanel == null) {
			hintsTreePanel = new JPanel();
			hintsTreePanel.setLayout(new BorderLayout());
			hintsTreePanel.add(getHintsTreeScrollPane(), BorderLayout.CENTER);
			hintsTreePanel.add(getHintsSouthPanel(), BorderLayout.SOUTH);
		}
		return hintsTreePanel;
	}

	private JButton getBtnValidate() {
		if (btnValidate == null) {
			btnValidate = newJButton("Validate", KeyEvent.VK_V
					, "Check the Sudoku is valid (has exactly one solution)");
			btnValidate.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Ctrl-Shft-V to validate noisily
					boolean quite = (e.getModifiers() & Event.SHIFT_MASK) == 0;
					if (engine.validatePuzzleAndGrid(quite)) {
						setHintDetailArea(Html.load(SudokuFrame.this, "Valid.html"));
					}
				}
			});
		}
		return btnValidate;
	}

	private JButton getBtnApplyHint() {
		if (btnApplyHint == null) {
			btnApplyHint = newJButton("Apply hint", KeyEvent.VK_P
					, "Apply the selected hint(s)");
			btnApplyHint.setEnabled(false);
			btnApplyHint.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.applySelectedHints();
				}
			});
		}
		return btnApplyHint;
	}

	private JComboBox<String> getCmbViewSelector() {
		if (cmbViewSelector == null) {
			cmbViewSelector = new JComboBox<>();
			cmbViewSelector.setToolTipText(
					"Toggle view, especially for chaining hints");
			cmbViewSelector.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED
							&& cmbViewSelector.isEnabled()) {
						viewNum = cmbViewSelector.getSelectedIndex();
						repaintHint();
					}
				}
			});
		}
		return cmbViewSelector;
	}

	// ========================== HINTS SOUTH PANEL ===========================
	private JPanel getHintsSouthPanel() {
		if (hintsSouthPanel == null) {
			hintsSouthPanel = new JPanel(new BorderLayout());
			hintsSouthPanel.add(getDisabledTechsWarnPanel(), BorderLayout.NORTH);
			hintsSouthPanel.add(getChkFilterHints(), BorderLayout.WEST);
			hintsSouthPanel.add(getRatingPanel(), BorderLayout.EAST);
		}
		return hintsSouthPanel;
	}

	private JPanel getDisabledTechsWarnPanel() {
		if (disabledTechsWarnPanel == null) {
			JLabel lbl = new JLabel();
			lbl.setText("");
			lbl.setToolTipText(
					 "<html><body>"
					+"Not all the available Sudoko solving techniques are enabled.<br>"
					+"Click here to dis/enable solving techniques."
					+"</body></html>");
			lbl.setIcon(createImageIcon("Icon_Warning.gif"));
			lbl.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					selectSodokuSolvingTechniques();
				}
			});
			lblDisabledTechsWarning = lbl;
			JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
			pnl.add(lbl, null);
			pnl.setVisible(false);
			disabledTechsWarnPanel = pnl;
		}
		return disabledTechsWarnPanel;
	}

	private JCheckBox getChkFilterHints() {
		if (chkFilterHints == null) {
			chkFilterHints = new JCheckBox("Filter hints");
			chkFilterHints.setMnemonic(KeyEvent.VK_I);
			chkFilterHints.setToolTipText("Filter hints with similar outcome");
			chkFilterHints.setSelected(THE_SETTINGS.get(Settings.isFilteringHints));
			chkFilterHints.setEnabled(false);
			chkFilterHints.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					boolean isSelected = chkFilterHints.isSelected();
					getMitFilterHints().setSelected(isSelected);
					engine.setFiltered(isSelected);
				}
			});
		}
		return chkFilterHints;
	}

	private JPanel getRatingPanel() {
		if (ratingPanel == null) {
			ratingPanel = new JPanel(new FlowLayout(java.awt.FlowLayout.LEFT));
			ratingPanel.add(new JLabel("Hint rating: "), null);
			// same length as a difficulty or label jumps around distractingly
			ratingPanel.add(this.lblPuzzleRating = new JLabel("0.00"), null);
		}
		return ratingPanel;
	}

	// ============================= MAIN MENU =============================
	private JMenuBar getMyMainMenuBar() {
		if (menuBar == null) {
			menuBar = new JMenuBar();
			menuBar.add(getFileMenu());
			menuBar.add(getEditMenu());
			menuBar.add(getToolMenu());
			menuBar.add(getOptionsMenu());
			menuBar.add(getHelpMenu());
		}
		return menuBar;
	}

	// menu keyboard-shortcut: normal (no modifier keys)
	private JMenuItem norm(int keyCode, JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(keyCode, 0));
		return mit;
	}

	// menu keyboard-shortcut: alt (alternate)
	private JMenuItem alt(int keyCode, JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(keyCode, InputEvent.ALT_MASK));
		return mit;
	}

	// menu keyboard-shortcut: shft (move your ass)
	private JMenuItem shft(int keyCode, JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(keyCode, InputEvent.SHIFT_MASK));
		return mit;
	}

	// menu keyboard-shortcut: ctrl (control)
	private JMenuItem ctrl(char ch, JMenuItem mit) {
		mit.setAccelerator(KeyStroke.getKeyStroke(ch, InputEvent.CTRL_MASK));
		return mit;
	}

	// ============================= FILE MENU =============================
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = newJMenu("File", KeyEvent.VK_F
					, "Load and dump s__t");
			fileMenu.add(ctrl('N', getMitFileNew()));
			fileMenu.add(ctrl('G', getMitFileGenerate()));
			fileMenu.addSeparator();
			fileMenu.add(ctrl('M', getMitFilesRecent()));
			fileMenu.addSeparator();
			fileMenu.add(ctrl('O', getMitFileOpen()));
			fileMenu.add(ctrl('R', getMitFileReload()));
			fileMenu.add(ctrl('P', getMitFileNext()));
			fileMenu.add(ctrl('S', getMitFileSave()));
			fileMenu.addSeparator();
			fileMenu.add(ctrl('L', getMitLogView()));
			fileMenu.addSeparator();
			fileMenu.add(ctrl('Q', getMitQuit()));
		}
		return fileMenu;
	}

	private JMenuItem getMitFileNew() {
		if (mitNew == null) {
			mitNew = newJMenuItem("New", KeyEvent.VK_N, "Clear the grid");
			mitNew.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.clearGrid();
					setTitle(Build.ATV);
				}
			});
		}
		return mitNew;
	}

	private JMenuItem getMitFileGenerate() {
		if (mitGenerate == null) {
			mitGenerate = newJMenuItem("Generate...", KeyEvent.VK_G
					, "Open a dialog to generate a random Sudoku puzzle");
			mitGenerate.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					GenerateDialog dialog = generateDialog;
					if (dialog == null) {
						dialog = new GenerateDialog(SudokuFrame.this, engine);
						dialog.pack();
						// top-right
						dialog.setLocation(getToolkit().getScreenSize().width
								- dialog.getSize().width, 0);
						generateDialog = dialog;
					}
					setTitle(Build.ATV);
					generateDialog.setVisible(true);
				}
			});
		}
		return mitGenerate;
	}

	private JMenuItem getMitFilesRecent() {
		if (mitRecentFiles == null) {
			mitRecentFiles = newJMenuItem("Recent Files...", KeyEvent.VK_M
					, "Open a dialog to select a recently accessed file");
			mitRecentFiles.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					RecentFilesDialog rfd = recentFilesDialog;
					if (rfd == null) {
						rfd = new RecentFilesDialog(SudokuFrame.this, engine);
						rfd.pack();
						rfd.setLocation(0, 0);
						recentFilesDialog = rfd;
					}
					recentFilesDialog.setVisible(true);
				}
			});
		}
		return mitRecentFiles;
	}

	private JMenuItem getMitFileOpen() {
		if (mitLoad == null) {
			mitLoad = newJMenuItem("Open...", KeyEvent.VK_O
					, "Open a puzzle file");
			mitLoad.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						openFile();
						showWelcomeText();
						repaint();
					} catch (AccessControlException ex) {
						showAccessError(ex);
					}
				}
			});
		}
		return mitLoad;
	}

	private void openFile() {
		try {
			File selectedFile = chooseFile(new PuzzleFileFilter());
			if (selectedFile != null) {
				// handle PuzzleID special case, for example:
				// 347#C:\\Users\\User\\Documents\\NetBeansProjects\\DiufSudoku\\top1465.d5.mt
				// without the escaped backslashes, obviously.
				PuzzleID pid = selectedFile.exists()
						? new PuzzleID(selectedFile, 0)
						: extractPuzzleID(selectedFile.toString());
				setTitle(Build.ATV + "   " + engine.loadFile(pid));
			}
		} catch (Exception ex) {
			displayError(ex);
		}
	}

	private final class PuzzleFileFilter
			extends javax.swing.filechooser.FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			final String n = f.getName().toLowerCase();
			return n.endsWith(".txt") || n.endsWith(".mt");
		}

		@Override
		public String getDescription() {
			return "Puzzle files (*.txt;*.mt)";
		}
	}

	// user chooses a File from the given filter, else null
	// called by openFile() above and mitLogView ActionListener
	private File chooseFile(FileFilter fileFilter) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(fileFilter);
		if (defaultDirectory != null) {
			chooser.setCurrentDirectory(defaultDirectory);
		}
		int result = chooser.showOpenDialog(SudokuFrame.this);
		defaultDirectory = chooser.getCurrentDirectory();
		return result == JFileChooser.APPROVE_OPTION
				? chooser.getSelectedFile()
				: null;
	}

	// Extract a PuzzleID from the selectedFile.toString().
	// This is required because Swing prepends the current directory to whatever
	// the user typed, because that absolutely MUST be a file name, right?
	// So we remove the preceeding current-directory-path, and parse the rest
	// of the string as a PuzzleID, and return it.
	private PuzzleID extractPuzzleID(String s) {
		try {
			// no hash means there's definately no PuzzleID here
			if (s.indexOf('#') < 0) { // ie -1 meaning not found
				carp("'#' not found in:" + NL + s, "File Open");
				return null;
			}
			// strip prepended current directory path
			String defDir = defaultDirectory.getAbsolutePath();
			if (s.startsWith(defDir, 0)) {
				s = s.substring(defDir.length() + 1); // remove default directory
			}			// parse the rest into a PuzzleID and return it
			return PuzzleID.parse(s);
		} catch (Exception ex) {
			carp(ex, "File Open");
			return null;
		}
	}

	private void showAccessError(AccessControlException ex) {
		carp("Sorry, this functionality cannot be used from an applet." + NL
			+ex.getPermission().toString() + NL
			+"Download the application to access this functionality."
			, "Access denied");
	}

	private JMenuItem getMitFileReload() {
		if (mitReload == null) {
			mitReload = newJMenuItem("Reload", KeyEvent.VK_R
					, "reload the current puzzle into the grid");
			mitReload.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						PuzzleID pid = engine.reloadFile();
						setTitle(Build.ATV + (pid != null ? "    " + pid : ""));
						repaint();
					} catch (AccessControlException ex) {
						showAccessError(ex);
					}
				}
			});
		}
		return mitReload;
	}

	private JMenuItem getMitFileNext() {
		if (mitLoadNext == null) {
			mitLoadNext = newJMenuItem("load next Puzzle", KeyEvent.VK_P
					, "load the next puzzle from the current MagicTour file");
			mitLoadNext.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						// there is no "next" without a source for "this"
						PuzzleID src = engine.getGrid().source;
						if (src == null) {
							return;
						}
						// special: Ctrl-P generates next puzzle in generate.
						GenerateDialog gd = generateDialog;
						if (gd != null && IO.GENERATED_FILE.equals(src.file)) {
							gd.setVisible(true);
							gd.generate();
							hintsTree.requestFocusInWindow();
							return;
						}
						// load the next puzzle
						String filename = src.file.getName().toLowerCase();
						PuzzleID pid;
						if (filename.endsWith(".mt")) {
							clearHintDetailArea();
							pid = engine.loadNextPuzzle();
						} else if (filename.endsWith(".txt")) {
							clearHintDetailArea();
							pid = engine.loadFile(engine.nextTxtFile(src.file), 0);
						} else {
							return;
						}
						setTitle(Build.ATV + (pid != null ? "    " + pid : ""));
						repaint();
						// auto-analyse
						analyseInTheBackground(Logging.of(e.getModifiers()));
					} catch (AccessControlException ex) {
						showAccessError(ex);
					}
				}
			;
		}
		);
		}
		return mitLoadNext;
	}

	private JMenuItem getMitFileSave() {
		if (mitSave == null) {
			mitSave = newJMenuItem("Save...", KeyEvent.VK_S
					, "Save this puzzle to a file");
			mitSave.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						saveFile();
						repaint();
					} catch (AccessControlException ex) {
						showAccessError(ex);
					}
				}
			});
		}
		return mitSave;
	}

	private void saveFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new TextFileFilter());
		if (defaultDirectory != null) {
			chooser.setCurrentDirectory(defaultDirectory);
		}
		if (chooser.showSaveDialog(SudokuFrame.this)
				!= JFileChooser.APPROVE_OPTION) {
			return;
		}
		defaultDirectory = chooser.getCurrentDirectory();
		File file = chooser.getSelectedFile();
		if (!file.getName().toLowerCase().endsWith(".txt")) {
			file = new File(file.getAbsolutePath() + ".txt");
		}
		if (file.exists()
				&& !ask("The file \"" + file.getName() + "\" already exists." + NL
						+ "Do you want to replace the existing file ?")) {
			return;
		}
		String puzzleID = engine.saveFile(file);
		setTitle(Build.ATV + "    " + puzzleID);
	}

	private final class TextFileFilter // is still required for Save dialog
			extends javax.swing.filechooser.FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			return f.getName().toLowerCase().endsWith(".txt");
		}

		@Override
		public String getDescription() {
			return "Text files (*.txt)";
		}
	}

	// I reckon only techies will ever use LogicalSolverTester. This menu item
	// allows you to view the hints-of-type in a LogicalSolverTester .log file.
	// * The intended regex is a Tech.name() or whatever as per the hint-line
	//   in the log-File. Only hint-text (char 65 on) is matched
	// * Use the menu to start a new search (even if search exists)
	// * Then press Ctrl-L to find-next (or start a new search, if none exists)
	private JMenuItem getMitLogView() {
		if ( mitLogView != null )
			return mitLogView;
		mitLogView = newJMenuItem("Log View", KeyEvent.VK_W
				, "View hints that match a regular expresion in an existing LogicalSolverTester .log file");
		mitLogView.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// no log yet
				if ( logViewFile==null || !logViewFile.exists()
				  // or no regex yet
				  || regex==null || regex.isEmpty()
				  // or initiated by the menu
				  || (e.getModifiers() & KeyEvent.CTRL_MASK) == 0 ) {
					// get the .log File
					logViewFile = chooseFile(new LogFileFilter());
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
		return mitLogView;
	}

	// LogViewHintRegexDialog.btnOk calls-back this method
	void logView(String re) {
		regex = engine.logView(logViewFile, re);
		setTitle(Build.ATV + "   " + engine.getGrid().source);
	}

	// for mitLogView
	private final class LogFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			final String n = f.getName().toLowerCase();
			return n.endsWith(".log");
		}

		@Override
		public String getDescription() {
			return "Log files (*.log)";
		}
	}

	private JMenuItem getMitQuit() {
		if (mitQuit == null) {
			mitQuit = newJMenuItem("Quit", KeyEvent.VK_Q, "Bye bye");
			mitQuit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
		}
		return mitQuit;
	}

	// ============================= EDIT MENU =============================
	private JMenu getEditMenu() {
		if (editMenu == null) {
			editMenu = newJMenu("Edit", KeyEvent.VK_E, "Edit the grid.");
			editMenu.add(ctrl('C', getMitCopy()));
			editMenu.add(ctrl('V', getMitPaste()));
			editMenu.addSeparator();
			editMenu.add(ctrl('E', getMitClear()));
		}
		return editMenu;
	}

	private JMenuItem getMitCopy() {
		if (mitCopy == null) {
			mitCopy = newJMenuItem("Copy grid", KeyEvent.VK_C
					, "Copy the grid to the clipboard as plain text");
			mitCopy.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						engine.copyGridToClipboard();
					} catch (AccessControlException ex) {
						showAccessError(ex);
					}
				}
			});
		}
		return mitCopy;
	}

	private JMenuItem getMitPaste() {
		if (mitPaste == null) {
			mitPaste = newJMenuItem("Paste grid", KeyEvent.VK_P
					, "Replace the grid with the content of the clipboard");
			mitPaste.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						engine.pastePuzzle();
					} catch (AccessControlException ex) {
						showAccessError(ex);
					}
				}
			});
		}
		return mitPaste;
	}

	private JMenuItem getMitClear() {
		if (mitClear == null) {
			mitClear = newJMenuItem("Clear grid", KeyEvent.VK_E
					, "Clear the grid");
			mitClear.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.clearGrid();
				}
			});
		}
		return mitClear;
	}

	// ============================= TOOL MENU =============================
	private JMenu getToolMenu() {
		if (toolsMenu != null) {
			return toolsMenu;
		}
		toolsMenu = newJMenu("Tools", KeyEvent.VK_T
				, "Saw, pin, wax, axe. Careful Eugeene!");
		// NOTE WELL: Each menu-item has ONE accelerator key only, so every
		// combo of norm/shft/alt/ctrl needs it's very own menu item, which
		// really s__ts me. There simply MUST be a better way to do it, so
		// the User dosn't see all my "expert" modifiers! Anybody who can read
		// the code (it'll always be open source) could see all the "advanced"
		// stuff, but the GUI looks simple and vanilla, so that c___ts can work
		// out how to use the bastard!
		// BTW: create method shftCtrl (or whatever) if/when you use it.
		toolsMenu.add(ctrl('R', getMitResetPotentials()));
		toolsMenu.add(ctrl('D', getMitClearHints()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F2, getMitSolveStep()));
		toolsMenu.add(shft(KeyEvent.VK_F2, getMitSolveStepBig()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F3, getMitGetNextHint()));
		toolsMenu.add(norm(KeyEvent.VK_F4, getMitApplyHint()));
		toolsMenu.add(norm(KeyEvent.VK_F5, getMitGetAllHints()));
		toolsMenu.add(shft(KeyEvent.VK_F5, getMitGetAllHintsMore()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F6, getMitGetClueSmall()));
		toolsMenu.add(shft(KeyEvent.VK_F6, getMitGetClueBig()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F7, getMitCheckValidity()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F8, getMitSolve()));
		toolsMenu.add(norm(KeyEvent.VK_F9, getMitAnalyse()));
		toolsMenu.add(shft(KeyEvent.VK_F9, getMitAnalyseVerbose()));
		toolsMenu.add(alt(KeyEvent.VK_F9, getMitAnalyseTiming()));
		return toolsMenu;
	}

	private JMenuItem getMitResetPotentials() {
		if (mitResetPotentialValues == null) {
			mitResetPotentialValues = newJMenuItem("Reset potential values"
					, KeyEvent.VK_R
					, "Recompute the remaining potential values for"
					 +" the empty cells");
			mitResetPotentialValues.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.resetPotentialValues();
				}
			});
		}
		return mitResetPotentialValues;
	}

	private JMenuItem getMitClearHints() {
		if (mitClearHints == null) {
			mitClearHints = newJMenuItem("Clear hint(s)", KeyEvent.VK_C
					, "Clear the hint list");
			mitClearHints.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.clearHints();
				}
			});
		}
		return mitClearHints;
	}

	private JMenuItem getMitSolveStep() {
		if (mitSolveStep == null) {
			mitSolveStep = newJMenuItem("Solve step", KeyEvent.VK_S
					, getBtnSolveStep().getToolTipText());
			mitSolveStep.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveStep(false);
				}
			});
		}
		return mitSolveStep;
	}

	private JMenuItem getMitSolveStepBig() {
		if (mitSolveStepBig == null) {
			mitSolveStepBig = newJMenuItem("Solve big step", KeyEvent.VK_U
					, "apply hint and get next BIG hint (aggregate chains"
					 +" and solve with singles).");
			mitSolveStepBig.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveStep(true);
				}
			});
		}
		return mitSolveStepBig;
	}

	private JMenuItem getMitGetNextHint() {
		if (mitGetNextHint == null) {
			mitGetNextHint = newJMenuItem("Get next hint", KeyEvent.VK_N
					, getBtnGetNextHint().getToolTipText());
			mitGetNextHint.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.invokeGetNextHint(false, false);
				}
			});
		}
		return mitGetNextHint;
	}

	private JMenuItem getMitApplyHint() {
		if (mitApplyHint == null) {
			mitApplyHint = newJMenuItem("Apply hint", KeyEvent.VK_A
					, getBtnApplyHint().getToolTipText());
			mitApplyHint.setEnabled(false);
			mitApplyHint.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.applySelectedHints();
				}
			});
		}
		return mitApplyHint;
	}

	private JMenuItem getMitGetAllHints() {
		if (mitGetAllHints == null) {
			mitGetAllHints = newJMenuItem("Get all hints", KeyEvent.VK_H
					, getBtnGetAllHints().getToolTipText());
			mitGetAllHints.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getAllHintsInBackground(false, false);
				}
			});
		}
		return mitGetAllHints;
	}

	private JMenuItem getMitGetAllHintsMore() {
		if (mitGetAllHintsMore == null) {
			mitGetAllHintsMore = newJMenuItem("Get MORE all hints"
					, KeyEvent.VK_H
					, "Get MORE all hints (from all selected Techs).");
			mitGetAllHintsMore.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getAllHintsInBackground(true, false);
				}
			});
		}
		return mitGetAllHintsMore;
	}

	private JMenuItem getMitGetClueSmall() {
		if (mitGetClueSmall == null) {
			mitGetClueSmall = newJMenuItem("Get a small clue", KeyEvent.VK_M
					, "Get some information on the next solving step");
			mitGetClueSmall.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.getClue(false);
				}
			});
		}
		return mitGetClueSmall;
	}

	private JMenuItem getMitGetClueBig() {
		if (mitGetClueBig == null) {
			mitGetClueBig = newJMenuItem("Get a big clue", KeyEvent.VK_B
					, "Get more information on the next solving step");
			mitGetClueBig.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.getClue(true);
				}
			});
		}
		return mitGetClueBig;
	}

	private JMenuItem getMitCheckValidity() {
		if (mitCheckValidity == null) {
			mitCheckValidity = newJMenuItem("Check validity", KeyEvent.VK_V
					, "Check the Sudoku is valid (has exactly one solution)");
			mitCheckValidity.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// hold down the Shift key to log the validations
					boolean quite = (e.getModifiers() & Event.SHIFT_MASK) == 0;
					if (engine.validatePuzzleAndGrid(quite)) {
						setHintDetailArea(Html.load(SudokuFrame.this, "Valid.html"));
					}
				}
			});
		}
		return mitCheckValidity;
	}

	private JMenuItem getMitSolve() {
		if (mitSolve == null) {
			mitSolve = newJMenuItem("Solve", KeyEvent.VK_O
					, "Highlight the solution");
			mitSolve.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveASAP();
				}
			});
		}
		return mitSolve;
	}

	private JMenuItem getMitAnalyse() {
		if ( mitAnalyse != null )
			return mitAnalyse;
		mitAnalyse = newJMenuItem("Analyse", KeyEvent.VK_Y
			, "Summarise a simplest possible solution to this Sudoku");
		mitAnalyse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				analyseInTheBackground(Logging.NORMAL);
			}
		});
		return mitAnalyse;
	}

	private JMenuItem getMitAnalyseVerbose() {
		if (mitAnalyseVerbose != null)
			return mitAnalyseVerbose;
		mitAnalyseVerbose = newJMenuItem("Analyse (verbose)", KeyEvent.VK_J
			, "Summarise the bastard, logging hints (verbose)");
		mitAnalyseVerbose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				analyseInTheBackground(Logging.HINTS);
			}
		});
		return mitAnalyseVerbose;
	}

	private JMenuItem getMitAnalyseTiming() {
		if (mitAnalyseTiming != null)
			return mitAnalyseTiming;
		mitAnalyseTiming = newJMenuItem("Analyse (timing)", KeyEvent.VK_J
			, "Summarise the bastard, logging hints (VERY verbose)");
		mitAnalyseTiming.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				analyseInTheBackground(Logging.HINTERS);
			}
		});
		return mitAnalyseTiming;
	}

	private static enum Logging{ NORMAL, HINTS, HINTERS;
		private static Logging of(final int mod) {
			// upside down: HINTERS implies HINTS which implies NORMAL.
			if((mod & ALT_MASK) != 0) return HINTERS; // plus timings of each hinter (timings)
			if((mod & SHIFT_MASK) !=0) return HINTS; // plus log the hints used during solve (verbose)
			return NORMAL; // just the analyse summary ("normal" output)
		}
	}
	// analyse can take a while, so run it in a background thread
	// if isNoisy is true (SHIFT down) then print hints to SudokuExplainer.log
	private void analyseInTheBackground(Logging logging) {
		setHintDetailArea("Analysing ...");
		getHintDetailPane().repaint();
		final Runnable analyser = new Runnable() {
			@Override
			public void run() {
				try {
					long start = System.nanoTime();
					boolean myLogHints = logging != Logging.NORMAL;
					boolean myLogHinters = logging == Logging.HINTERS;
					engine.analysePuzzle(myLogHints, myLogHinters);
					Log.teef("Analyse took %,d ns\n", System.nanoTime() - start);
					hintsTreeRequestFocus();
				} catch (UnsolvableException ex) {
					displayError(ex);
				}
			}

		};
		final Thread backgroundAnalyser = new Thread(analyser, "Analyser");
		backgroundAnalyser.setDaemon(true);
		// invokeLater should allow it to repaint BEFORE starting the analyse.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					backgroundAnalyser.start();
				} catch (UnsolvableException ex) {
					displayError(ex);
				}
			}
		});
	}

	private void hintsTreeRequestFocus() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				hintsTree.requestFocusInWindow();
			}
		});
	}

	// ============================= OPTIONS MENU =============================
	private JMenu getOptionsMenu() {
		if (optionsMenu == null) {
			optionsMenu = newJMenu("Options", KeyEvent.VK_O
					, "Turn s__t off and on, or on and off");
			optionsMenu.add(getMitFilterHints());
			optionsMenu.add(getMitShowMaybes());
			optionsMenu.add(getMitGreenFlash());
			optionsMenu.add(getMitSelectTechniques());
			optionsMenu.add(getMitHacky());
			optionsMenu.add(getMitGodMode());
			optionsMenu.addSeparator();
			optionsMenu.add(getMitLookAndFeel());
			optionsMenu.add(getMitAntiAliasing());
			optionsMenu.add(getMitSaveSettings());
		}
		return optionsMenu;
	}

	private JCheckBoxMenuItem getMitFilterHints() {
		if (mitFilterHints == null) {
			mitFilterHints = newJCheckBoxMenuItem(
					  "Filter hints with repeat effects"
					, KeyEvent.VK_F, getChkFilterHints().getToolTipText()
					, Settings.isFilteringHints, false, false);
			mitFilterHints.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					boolean isSelected = mitFilterHints.isSelected();
					getChkFilterHints().setSelected(isSelected);
					engine.setFiltered(isSelected);
				}
			});
		}
		return mitFilterHints;
	}

	private JCheckBoxMenuItem getMitShowMaybes() {
		if (mitShowMaybes == null) {
			mitShowMaybes = newJCheckBoxMenuItem("Show maybes", KeyEvent.VK_C
					, "Display each cells potential values in small digits"
					, Settings.isShowingMaybes, true, true);
		}
		return mitShowMaybes;
	}

	private JCheckBoxMenuItem getMitGreenFlash() {
		if (mitGreenFlash == null) {
			mitGreenFlash = newJCheckBoxMenuItem("Green flash", KeyEvent.VK_G
					, "Flash green when the puzzle solves with singles"
					, Settings.isGreenFlash, true, true);
		}
		return mitGreenFlash;
	}

	private JMenuItem getMitSelectTechniques() {
		if (mitSelectTechniques == null) {
			mitSelectTechniques = newJMenuItem("Solving techniques..."
					, KeyEvent.VK_T, "En/disable Sudoku solving techniques");
			mitSelectTechniques.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selectSodokuSolvingTechniques();
				}
			});
		}
		return mitSelectTechniques;
	}

	private void selectSodokuSolvingTechniques() {
		TechSelectDialog tsd = techSelectDialog;
		if (tsd == null) {
			tsd = new TechSelectDialog(this, SudokuFrame.this.engine);
			tsd.pack();
			// top-right
			Dimension screenSize = getToolkit().getScreenSize();
			tsd.setLocation(screenSize.width - tsd.getSize().width, 0);
			techSelectDialog = tsd;
		}
		refreshDisabledRulesWarning();
		techSelectDialog.setVisible(true);
	}

	private JCheckBoxMenuItem getMitHacky() {
		if (mitHacky == null) {
			mitHacky = newJCheckBoxMenuItem("Hack top1465"
					, KeyEvent.VK_H
					, "<html><body>"
					 +"used top1465 in Aligned*Exclusion for faster solve"
					 +"</body></html>"
					, Settings.isHacky, true, true);
		}
		return mitHacky;
	}

	private JCheckBoxMenuItem getMitGodMode() {
		if (mitGodMode == null) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem("God Mode");
			item.setMnemonic(KeyEvent.VK_D);
			item.setToolTipText("for lazy bastards, like me");
			item.setEnabled(true);
			item.setSelected(engine.godMode);
			mitGodMode = item;
			mitGodMode.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					// God does NOT persist in your Settings. A shame really.
					engine.godMode = mitGodMode.isSelected() && god();
					engine.cheatMode = engine.godMode;
				}
			});
		}
		return mitGodMode;
	}

	boolean god() {
		return "IDDQD".equals("I" + askForString("assword:", "God", "DDQ"));
	}

	private JMenu getMitLookAndFeel() {
		if (mitLookAndFeel == null) {
			mitLookAndFeel = newJMenu("Look & Feel", KeyEvent.VK_L
					, "For wankers who care about how stuff looks");
		}
		return mitLookAndFeel;
	}

	private JCheckBoxMenuItem getMitAntiAliasing() {
		if (mitAntialiasing == null) {
			mitAntialiasing = newJCheckBoxMenuItem("high quality renDering"
					, KeyEvent.VK_D, "Use slower high quality rendering"
					, Settings.isAntialiasing, true, true);
		}
		return mitAntialiasing;
	}

	private JMenuItem getMitSaveSettings() {
		if (mitSaveSettings == null) {
			mitSaveSettings = newJMenuItem("Save the settings"
					, KeyEvent.VK_ASTERISK
					, "Persist the settings in the registry");
			mitSaveSettings.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					THE_SETTINGS.save();
				}
			});
		}
		return mitSelectTechniques;
	}

	// ============================= HELP MENU =============================
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = newJMenu("Help", KeyEvent.VK_H
					, "Help me Rhonda, Help help me Rhonda... ");
			helpMenu.add(getMitShowWelcome());
			KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
			getMitShowWelcome().setAccelerator(key);
			helpMenu.addSeparator();
			helpMenu.add(getMitAbout());
		}
		return helpMenu;
	}

	private JMenuItem getMitShowWelcome() {
		if (mitShowWelcome == null) {
			mitShowWelcome = newJMenuItem("Show welcome message"
					, KeyEvent.VK_W
					, "Display that big long help message");
			mitShowWelcome.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showWelcomeText();
				}
			});
		}
		return mitShowWelcome;
	}

	private JMenuItem getMitAbout() {
		if (mitAbout == null) {
			mitAbout = newJMenuItem("About", KeyEvent.VK_A
					, "Get information about the Sudoku Explainer application");
			mitAbout.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFrame dfk = dummyFrameKnife;
					if (dfk == null) {
						dfk = new JFrame();
						ImageIcon icon = createImageIcon("Icon_Knife.gif");
						dfk.setIconImage(icon.getImage());
						dummyFrameKnife = dfk;
					}
					AboutDialog ad = new AboutDialog(dummyFrameKnife);
					// top-right
					ad.setLocation(getToolkit().getScreenSize().width
							- ad.getSize().width, 0);
					ad.setVisible(true);
				}
			});
		}
		return mitAbout;
	}

	// =============================== HELPERS ================================
	private JMenu newJMenu(String text, int mnemonic, String toolTipText) {
		JMenu menu = new JMenu(text);
		menu.setMnemonic(mnemonic);
		menu.setToolTipText(toolTipText);
		return menu;
	}

	private JMenuItem newJMenuItem(String text, int mnemonic, String toolTipText) {
		JMenuItem item = new JMenuItem(text, mnemonic);
		item.setToolTipText(toolTipText);
		return item;
	}

	private JCheckBoxMenuItem newJCheckBoxMenuItem(String text, int mnemonic
			, String toolTipText, String settingName, boolean isEnabled
			, boolean addStandardListener) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(text);
		item.setMnemonic(mnemonic);
		item.setToolTipText(toolTipText);
		item.setSelected(THE_SETTINGS.get(settingName));
		item.setEnabled(isEnabled);
		if (addStandardListener) {
			addBooleanSettingListener(item, settingName);
		}
		return item;
	}

	// addItemListener for a standard boolean setting
	private void addBooleanSettingListener(final JCheckBoxMenuItem mit
			, final String settingName) {
		mit.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				THE_SETTINGS.set(settingName, mit.isSelected());
				repaint();
			}
		});
	}

	private JButton newJButton(String text, int mnemonic, String toolTipText) {
		JButton btn = new JButton(text);
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
		THE_SETTINGS.setBounds(SudokuFrame.this.getBounds());
		SudokuFrame.this.setVisible(false);
		try {
			if (engine != null) {
				// nb: engine is final so I can't set it to null, but the stuff
				// it closes are nullable, so calling engine.close() a second+
				// time is a no-op.
				engine.close();
			}
		} catch (IOException ex) {
//			engine.whinge(ex);
		}
		if (dummyFrameKnife != null) {
			dummyFrameKnife.dispose();
			dummyFrameKnife = null;
		}
		if (generateDialog != null) {
			generateDialog.dispose();
			generateDialog = null;
		}
		if (recentFilesDialog != null) {
			recentFilesDialog.dispose();
			recentFilesDialog = null;
		}
		if (techSelectDialog != null) {
			techSelectDialog.dispose();
			techSelectDialog = null;
		}
		super.dispose();
	}

	// ================================ ASKER =================================

	/**
	 * Ask the user a Yes/No question.
	 */
	@Override
	public boolean ask(String question) {
		return ask(question, getTitle());
	}

	/**
	 * Ask the user a Yes/No question with title. Preferred.
	 */
	@Override
	public boolean ask(String question, String title) {
		return showConfirmDialog(this, question, title, YES_NO_OPTION)
				== YES_OPTION;
	}

	/**
	 * Ask the user for some String input.
	 */
	@Override
	public String askForString(String question, String title) {
		return showInputDialog(this, question, title, PLAIN_MESSAGE);
	}

	/**
	 * Ask the user for some String input, providing a default.
	 */
	@Override
	public String askForString(String question, String title, String defualt) {
		return (String)showInputDialog(this, question, title, PLAIN_MESSAGE
				, null, (Object[])null, (Object)defualt);
	}

	/**
	 * I was trying to IAsk.god but it keeps coming back as a cheat!
	 *
	 * @return how many assbitrarianists does it take pluck a duck?
	 */
	boolean cheat() {
		return "IDKFA".equals("I" + askForString("assword:", "Cheat", "DKF"));
	}

	/**
	 * Complain to the user: with String 'msg' and 'title'.
	 */
	@Override
	public void carp(String msg, String title) {
		showMessageDialog(this, msg, title, ERROR_MESSAGE);
	}

	/**
	 * Complain to the user: with Exception 'ex' and 'title'.
	 */
	@Override
	public void carp(Exception ex, String title) {
		String message = ex.toString();
		for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
			message += NL + t.toString();
		}
		showMessageDialog(this, message, title, ERROR_MESSAGE);
	}

}
