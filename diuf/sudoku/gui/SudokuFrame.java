/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Settings.*;

import diuf.sudoku.Grid;
import diuf.sudoku.PuzzleID;
import diuf.sudoku.Settings;
import diuf.sudoku.Tech;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.checks.SolvedHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.IAsker;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
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
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
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
final class SudokuFrame extends JFrame implements IAsker {

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

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
		, mitSolveBigStep, mitGetNextHint, mitApplyHint, mitGetAllHints
		, mitGetMoreAllHints, mitGetSmallClue, mitGetBigClue, mitCheckValidity
		, mitSolve, mitAnalyse, mitLogView;
	// Options
	private JMenu optionsMenu, mitLookAndFeel; // a sub-menu under optionsMenu
	private JMenuItem mitSelectTechniques;
	private JCheckBoxMenuItem mitFilterHints, mitShowMaybes, mitGreenFlash
		, mitAntialiasing, mitHacky, mitDegreleaseMode;
	// Help
	private JMenu helpMenu;
	private JMenuItem mitShowWelcome, mitAbout;

	private String welcomeText; // loaded once and stored here for redisplay
	private AHint currHint; // the selected hint, nullable
	private int viewCount, viewNum = -1; // a hint can have >1 "views" in grid

	/**
	 * The directory which the open-file and save-file dialogs start in.
	 */
	File defaultDirectory;

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
		this.setTitle(ATV);
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
					setTitle(ATV + "   (dropped in)");
					hintDetailPane.setText("Sudoku dropped!");
				} catch (Exception ex) {
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
	void setHintsTree(HintNode root, HintNode selected, boolean isEnabled) {
		getHintsTree();
		hintsTree.setEnabled(false);
		hintsTree.setModel(new DefaultTreeModel(root));
		// Dis/enable the Filter checkbox and menu item.
		chkFilterHints.setSelected(Settings.THE.get(Settings.isFilteringHints));
		chkFilterHints.setEnabled(isEnabled);
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
		setHintsTree(EMPTY_HINTS_TREE, null, false);
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
			p.setAquaBGCells(null);
			p.setPinkBGCells(null);
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
		} else { // display it
			p.setResult(h.getResult());
			p.setAquaBGCells(h.getAquaCells(viewNum)); // null OK
			p.setPinkBGCells(h.getPinkCells(viewNum)); // null OK
			if ( h instanceof AWarningHint )
				p.setRedBGCells(((AWarningHint) h).getRedCells());
			p.setGreenBGCells(h.getGreenCells(viewNum)); // null OK
			p.setOrangeBGCells(h.getOrangeCells(viewNum)); // null OK
			p.setBlueBGCells(h.getBlueCells(viewNum)); // null OK
			p.setYellowBGCells(h.getYellowCells(viewNum)); // null OK
			p.setGreenPots(h.getGreens(viewNum));
			p.setRedPots(h.getReds(viewNum));
			p.setOrangePots(h.getOranges(viewNum));
			p.setBluePots(h.getBlues(p.getSudokuGrid(), viewNum));
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
			// NB: AnalysisHint implements IActualHint despite not being one.
			if ( hint instanceof IActualHint ) {
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
		int numDisabled = Settings.ALL_TECHS.size()
				- Settings.THE.getWantedTechniques().size();
		boolean isVisible = true; // turns out I allways want to see the warning.
		String disabledWarningText = numDisabled == 1
				? "1 solving technique disabled."
				: "" + numDisabled + " solving techniques disabled.";
		// add the unavailableText
		String unavailableText = "";
		EnumSet<Tech> wanted = Settings.THE.getWantedTechniques();
		if (Settings.THE.get(Settings.isHacky)
				// these Tech's are too slow for practical use when NOT hacked.
				&& (wanted.contains(Tech.AlignedSept)
				|| wanted.contains(Tech.AlignedOct)
				|| wanted.contains(Tech.AlignedNona)
				|| wanted.contains(Tech.AlignedDec))) {
			unavailableText = " Careful with that axe Eugene!";
		}
		disabledWarningText += unavailableText;
		// set the warning JLabel's text
		lblDisabledTechsWarning.setText(disabledWarningText);
		// make the warning panel in/visible
		disabledTechsWarnPanel.setVisible(isVisible);
		return isVisible;
	}

	private final class HintsTreeCellRenderer implements TreeCellRenderer {

		// this Renderer "wraps" a DefaultTreeCellRenderer to handle HintNodes
		private final DefaultTreeCellRenderer DTCR
				= new DefaultTreeCellRenderer();

		public HintsTreeCellRenderer() {
			DTCR.setLeafIcon(createImageIcon("Icon_Light.gif"));
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				 boolean selected, boolean expanded, boolean leaf, int row,
				 boolean hasFocus) {
			if (value instanceof HintNode) {
				HintNode hn = (HintNode) value;
				boolean isEmpty = !hn.isHintNode() && hn.getChildCount() == 0;
				expanded |= isEmpty;
				leaf &= !isEmpty;
			}
			return DTCR.getTreeCellRendererComponent(tree, value, selected,
					 expanded, leaf, row, hasFocus);
		}
	}

	private void setupLookAndFeelMenu() {
		String lookAndFeelName = Settings.THE.getLookAndFeelClassName();
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
			} catch (Exception ex) {
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
						Settings.THE.setLookAndFeelClassName(lafClassName);
						Settings.THE.save();
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
					} catch (Exception ex) {
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
				if (keyCode == KeyEvent.VK_ENTER) {
					applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
					e.consume();
				} else if (keyCode == KeyEvent.VK_DELETE) {
					ArrayList<HintNode> hintNodes = getSelectedHintNodes();
					if (hintNodes != null && !hintNodes.isEmpty()) {
						Tech deadTech = hintNodes.get(0).getHint().hinter.tech;
						removeHintsAndDisableHinter(deadTech);
						hintsTree.clearSelection();
						repaint();
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
				if (btn == MouseEvent.BUTTON1 && cnt == 2) {
					// double-left-click: as per VK_ENTER
					applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
					e.consume();
				} else if (btn == MouseEvent.BUTTON3 && cnt == 1) {
					// single-right-click
					if ((mod & InputEvent.CTRL_DOWN_MASK) != 0) {
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
		if (isGettingAllHints) {
			return;
		}
		isGettingAllHints = true;
		setHintDetailArea("Searching for" + (wantMore ? " MORE" : "") + " hints...");
		repaint();
		Thread getAllHintsThread = new Thread("getAllHints") {
			@Override
			public void run() {
				engine.getAllHints(wantMore, wantSolution);
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
				setCurrentHint(new SolvedHint(), false);
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

	private JEditorPane getHintDetailPane() {
		if (hintDetailPane != null) {
			return hintDetailPane;
		}
		hintDetailPane = new JEditorPane("text/html", null) {
			private static final long serialVersionUID
					= -5658720148768663350L;

			@Override
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						 RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				super.paint(g);
			}
		};
		hintDetailPane.setEditable(false);
		hintDetailPane.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int btn = e.getButton();
				int cnt = e.getClickCount();
				if ((btn == MouseEvent.BUTTON2 || btn == MouseEvent.BUTTON3) && cnt == 1) {
					// single-right-click
					if (hintDetailHtml != null) {
						engine.copyToClipboard(hintDetailHtml);
					} else {
						engine.beep();
					}
					e.consume();
				}
			}
		});
		// make keys do same in hintDetailPane as in the hintsTree
		hintDetailPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				final int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_ENTER) {
					applySelectedHintsAndGetNextHint(e.isShiftDown(), e.isControlDown());
					e.consume();
				} else if (keyCode == KeyEvent.VK_DELETE) {
					ArrayList<HintNode> hintNodes = getSelectedHintNodes();
					if (hintNodes != null && !hintNodes.isEmpty()) {
						Tech deadTech = hintNodes.get(0).getHint().hinter.tech;
						removeHintsAndDisableHinter(deadTech);
						hintsTree.clearSelection();
						repaint();
					}
					e.consume();
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
			btnGetNextHint = newJButton("Get next hint", KeyEvent.VK_N,
					 "Get the next bloody hint");
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
			btnGetAllHints = newJButton("Get all hints", KeyEvent.VK_A,
					 "Get all hints applicable on the current situation");
			btnGetAllHints.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int mod = e.getModifiers();
					boolean wantMore = (mod & ActionEvent.SHIFT_MASK) != 0;
					boolean wantSolution = (mod & ActionEvent.CTRL_MASK) != 0;
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
			btnSolveStep = newJButton("Solve step", KeyEvent.VK_S,
					 "Apply the selected hint (if any) and get the next one");
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
			btnValidate = newJButton("Validate", KeyEvent.VK_V,
					 "Check the Sudoku is valid (has exactly one solution)");
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
			btnApplyHint = newJButton("Apply hint", KeyEvent.VK_P,
					 "Apply the selected hint(s)");
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
					+ "Not all the available Sudoko solving techniques are enabled.<br>"
					+ "Click here to dis/enable solving techniques."
					+ "</body></html>");
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
			chkFilterHints.setSelected(Settings.THE.get(Settings.isFilteringHints));
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
			fileMenu = newJMenu("File", KeyEvent.VK_F,
					 "Load and dump s__t");
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
					setTitle(ATV);
				}
			});
		}
		return mitNew;
	}

	private JMenuItem getMitFileGenerate() {
		if (mitGenerate == null) {
			mitGenerate = newJMenuItem("Generate...", KeyEvent.VK_G,
					 "Open a dialog to generate a random Sudoku puzzle");
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
					setTitle(ATV);
					generateDialog.setVisible(true);
				}
			});
		}
		return mitGenerate;
	}

	private JMenuItem getMitFilesRecent() {
		if (mitRecentFiles == null) {
			mitRecentFiles = newJMenuItem("Recent Files...", KeyEvent.VK_M,
					 "Open a dialog to select a recently accessed file");
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
			mitLoad = newJMenuItem("Open...", KeyEvent.VK_O,
					 "Open a puzzle file");
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
				setTitle(ATV + "   " + engine.loadFile(pid));
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
				+ ex.getPermission().toString() + NL
				+ "Download the application to access this functionality.",
				 "Access denied");
	}

	private JMenuItem getMitFileReload() {
		if (mitReload == null) {
			mitReload = newJMenuItem("Reload", KeyEvent.VK_R,
					 "reload the current puzzle into the grid");
			mitReload.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						PuzzleID pid = engine.reloadFile();
						setTitle(ATV + (pid != null ? "    " + pid : ""));
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
			mitLoadNext = newJMenuItem("load next Puzzle", KeyEvent.VK_P,
					 "load the next puzzle from the current MagicTour file");
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
							gd.requestFocusInWindow();
							gd.generate();
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
						setTitle(ATV + (pid != null ? "    " + pid : ""));
						repaint();
						// auto-analyse
						analyseInTheBackground();
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
			mitSave = newJMenuItem("Save...", KeyEvent.VK_S,
					 "Save this puzzle to a file");
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
		setTitle(ATV + "    " + puzzleID);
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

	// I reckon only a developer will ever use LogicalSolverTester. This menu
	// item allows you to view the hints-of-type in a LogicalSolverTester .log
	// file. It presumes the target string is a complete Tech.nom: with the
	// trailing colon as it appears on the hint-line (in the log), ie the two
	// lines above are the puzzle which produces/d a hint of this type.
	private JMenuItem getMitLogView() {
		if (mitLogView != null) {
			return mitLogView;
		}
		mitLogView = newJMenuItem("Log View", KeyEvent.VK_W,
				 "View tech.nom: hints in a LogicalSolverTester log file");
		mitLogView.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int mods = e.getModifiers();
				// if this event was fired from the menu then restart the search
				if ( (mods & KeyEvent.CTRL_MASK) == 0
				  || logViewFile == null || !logViewFile.exists()
				  || techNom == null || techNom.isEmpty()) {
					logViewFile = chooseFile(new LogFileFilter());
					// supply a default tech.nom, hardcoded to whatever I'm
					// working on at the moment.
					if ( logViewFile == null || !logViewFile.exists() )
						return;
					if ( techNom == null || techNom.isEmpty() )
						// WARN: Death Blossom invalidity I3-9 in @Death Blossom: G2-67 (I3-9)!
						techNom = ".*@Death Blossom.*";
					techNom = Ask.forString("tech.nom (regex)", techNom);
				}
				if ( techNom == null || techNom.isEmpty() )
					return;
				techNom = engine.logView(logViewFile, techNom);
				setTitle(ATV + "   " + engine.getGrid().source);
			}
		});
		return mitLogView;
	}
	private String techNom = null;
	private File logViewFile = null;

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
			mitCopy = newJMenuItem("Copy grid", KeyEvent.VK_C,
					 "Copy the grid to the clipboard as plain text");
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
			mitPaste = newJMenuItem("Paste grid", KeyEvent.VK_P,
					 "Replace the grid with the content of the clipboard");
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
			mitClear = newJMenuItem("Clear grid", KeyEvent.VK_E,
					 "Clear the grid");
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
		toolsMenu = newJMenu("Tools", KeyEvent.VK_T,
				 "Saw, pin, wax, axe. Careful Eugeene!");
		toolsMenu.add(ctrl('R', getMitResetPotentials()));
		toolsMenu.add(ctrl('D', getMitClearHints()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F2, getMitSolveStep()));
		toolsMenu.add(shft(KeyEvent.VK_F2, getMitSolveBigStep()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F3, getMitGetNextHint()));
		toolsMenu.add(norm(KeyEvent.VK_F4, getMitApplyHint()));
		toolsMenu.add(norm(KeyEvent.VK_F5, getMitGetAllHints()));
		toolsMenu.add(shft(KeyEvent.VK_F5, getMitGetMoreAllHints()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F6, getMitGetSmallClue()));
		toolsMenu.add(shft(KeyEvent.VK_F6, getMitGetBigClue()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F7, getMitCheckValidity()));
		toolsMenu.addSeparator();
		toolsMenu.add(norm(KeyEvent.VK_F8, getMitSolve()));
		toolsMenu.add(norm(KeyEvent.VK_F9, getMitAnalyse()));
		return toolsMenu;
	}

	private JMenuItem getMitResetPotentials() {
		if (mitResetPotentialValues == null) {
			mitResetPotentialValues = newJMenuItem("Reset potential values",
					 KeyEvent.VK_R, "Recompute the remaining potential values for"
					+ " the empty cells");
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
			mitClearHints = newJMenuItem("Clear hint(s)", KeyEvent.VK_C,
					 "Clear the hint list");
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
			mitSolveStep = newJMenuItem("Solve step", KeyEvent.VK_S,
					 getBtnSolveStep().getToolTipText());
			mitSolveStep.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveStep(false);
				}
			});
		}
		return mitSolveStep;
	}

	private JMenuItem getMitSolveBigStep() {
		if (mitSolveBigStep == null) {
			mitSolveBigStep = newJMenuItem("Solve big step", KeyEvent.VK_U,
					 "apply hint and get next BIG hint (aggregate chains"
					+ " and solve with singles).");
			mitSolveBigStep.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveStep(true);
				}
			});
		}
		return mitSolveBigStep;
	}

	private JMenuItem getMitGetNextHint() {
		if (mitGetNextHint == null) {
			mitGetNextHint = newJMenuItem("Get next hint", KeyEvent.VK_N,
					 getBtnGetNextHint().getToolTipText());
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
			mitApplyHint = newJMenuItem("Apply hint", KeyEvent.VK_A,
					 getBtnApplyHint().getToolTipText());
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
			mitGetAllHints = newJMenuItem("Get all hints", KeyEvent.VK_H,
					 getBtnGetAllHints().getToolTipText());
			mitGetAllHints.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getAllHintsInBackground(false, false);
				}
			});
		}
		return mitGetAllHints;
	}

	private JMenuItem getMitGetMoreAllHints() {
		if (mitGetMoreAllHints == null) {
			mitGetMoreAllHints = newJMenuItem("Get MORE all hints", KeyEvent.VK_H,
					 "Get MORE all hints (from all selected Techs).");
			mitGetMoreAllHints.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getAllHintsInBackground(true, false);
				}
			});
		}
		return mitGetMoreAllHints;
	}

	private JMenuItem getMitGetSmallClue() {
		if (mitGetSmallClue == null) {
			mitGetSmallClue = newJMenuItem("Get a small clue", KeyEvent.VK_M,
					 "Get some information on the next solving step");
			mitGetSmallClue.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.getClue(false);
				}
			});
		}
		return mitGetSmallClue;
	}

	private JMenuItem getMitGetBigClue() {
		if (mitGetBigClue == null) {
			mitGetBigClue = newJMenuItem("Get a big clue", KeyEvent.VK_B,
					 "Get more information on the next solving step");
			mitGetBigClue.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.getClue(true);
				}
			});
		}
		return mitGetBigClue;
	}

	private JMenuItem getMitCheckValidity() {
		if (mitCheckValidity == null) {
			mitCheckValidity = newJMenuItem("Check validity", KeyEvent.VK_V,
					 "Check the Sudoku is valid (has exactly one solution)");
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
			mitSolve = newJMenuItem("Solve", KeyEvent.VK_O,
					 "Highlight the solution");
			mitSolve.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					engine.solveRecursively();
				}
			});
		}
		return mitSolve;
	}

	private JMenuItem getMitAnalyse() {
		if (mitAnalyse != null) {
			return mitAnalyse;
		}
		mitAnalyse = newJMenuItem("Analyse", KeyEvent.VK_Y,
				 "Summarise the simplest solution to this Sudoku");
		mitAnalyse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				analyseInTheBackground();
			}
		});
		return mitAnalyse;
	}

	// analyse can take a while, so run it in a background thread
	private void analyseInTheBackground() {
		setHintDetailArea("Analysing ...");
		getHintDetailPane().repaint();
		final Runnable target = new Runnable() {
			@Override
			public void run() {
				try {
					long start = System.nanoTime();
					engine.analyse();
					Log.teef("Analyse took %,d ns\n", System.nanoTime() - start);
					hintsTreeRequestFocus();
				} catch (UnsolvableException ex) {
					displayError(ex);
				}
			}

		};
		final Thread background = new Thread(target, "Analyser");
		background.setDaemon(true);
		// invokeLater should allow it to repaint BEFORE starting the analyse.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					background.start();
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
			optionsMenu = newJMenu("Options", KeyEvent.VK_O,
					 "Turn s__t off and on, or on and off");
			optionsMenu.add(getMitFilterHints());
			optionsMenu.add(getMitShowMaybes());
			optionsMenu.add(getMitGreenFlash());
			optionsMenu.add(getMitSelectTechniques());
			optionsMenu.add(getMitHacky());
			optionsMenu.add(getMitDegreleaseMode());
			optionsMenu.addSeparator();
			optionsMenu.add(getMitLookAndFeel());
			optionsMenu.add(getMitAntiAliasing());
		}
		return optionsMenu;
	}

	private JCheckBoxMenuItem getMitFilterHints() {
		if (mitFilterHints == null) {
			mitFilterHints = newJCheckBoxMenuItem(
					"Filter hints with repeat effects",
					 KeyEvent.VK_F, getChkFilterHints().getToolTipText(),
					 Settings.isFilteringHints, false, false);
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
			mitShowMaybes = newJCheckBoxMenuItem("Show maybes", KeyEvent.VK_C,
					 "Display each cells potential values in small digits",
					 Settings.isShowingMaybes, true, true);
		}
		return mitShowMaybes;
	}

	private JCheckBoxMenuItem getMitGreenFlash() {
		if (mitGreenFlash == null) {
			mitGreenFlash = newJCheckBoxMenuItem("Green flash", KeyEvent.VK_G,
					 "Flash green when the puzzle solves with singles",
					 Settings.isGreenFlash, true, true);
		}
		return mitGreenFlash;
	}

	private JMenuItem getMitSelectTechniques() {
		if (mitSelectTechniques == null) {
			mitSelectTechniques = newJMenuItem("Solving techniques...",
					 KeyEvent.VK_T, "En/disable Sudoku solving techniques");
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
			mitHacky = newJCheckBoxMenuItem("Hack top1465",
					 KeyEvent.VK_H,
					 "<html><body>"
					+ "Use IS_HACKY top1465 hacks in Aligned*Exclusion (et al) for a fast solve<br>"
					+ "NB: This setting also effects LogicalSolverTester (see LST code for more)."
					+ "</body></html>",
					 Settings.isHacky, true, true);
		}
		return mitHacky;
	}

	private JCheckBoxMenuItem getMitDegreleaseMode() {
		if (mitDegreleaseMode == null) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem("Degrelease Mode");
			item.setMnemonic(KeyEvent.VK_D);
			item.setToolTipText("for ugly old fat smelly lazy bastards");
			item.setEnabled(true);
			item.setSelected(engine.degreleaseMode);
			mitDegreleaseMode = item;
			mitDegreleaseMode.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					// note that degrelease is NOT persisted in Settings!
					engine.degreleaseMode = mitDegreleaseMode.isSelected()
							&& "IDDQD".equals("I" + askForString("assword:", "Enable Degrelease Mode"));
				}
			});
		}
		return mitDegreleaseMode;
	}

	private JMenu getMitLookAndFeel() {
		if (mitLookAndFeel == null) {
			mitLookAndFeel = newJMenu("Look & Feel", KeyEvent.VK_L,
					 "Change the appearance of the application by choosing one"
					+ " of the available schemes");
		}
		return mitLookAndFeel;
	}

	private JCheckBoxMenuItem getMitAntiAliasing() {
		if (mitAntialiasing == null) {
			mitAntialiasing = newJCheckBoxMenuItem("high quality renDering",
					 KeyEvent.VK_D, "Use slower high quality rendering",
					 Settings.isAntialiasing, true, true);
		}
		return mitAntialiasing;
	}

	// ============================= HELP MENU =============================
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = newJMenu("Help", KeyEvent.VK_H, "Help me Rhonda, Help"
					+ " help me Rhonda... ");
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
			mitShowWelcome = newJMenuItem("Show welcome message", KeyEvent.VK_W,
					 "Show the explanation text displayed when the application"
					+ " is started");
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
			mitAbout = newJMenuItem("About", KeyEvent.VK_A,
					 "Get information about the Sudoku Explainer application");
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

	private JCheckBoxMenuItem newJCheckBoxMenuItem(String text, int mnemonic,
			 String toolTipText, String settingName, boolean isEnabled,
			 boolean addStandardListener) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(text);
		item.setMnemonic(mnemonic);
		item.setToolTipText(toolTipText);
		item.setSelected(Settings.THE.get(settingName));
		item.setEnabled(isEnabled);
		if (addStandardListener) {
			addBooleanSettingListener(item, settingName);
		}
		return item;
	}

	// addItemListener for a standard boolean setting
	private void addBooleanSettingListener(final JCheckBoxMenuItem mit,
			 final String settingName) {
		mit.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				Settings.THE.set(settingName, mit.isSelected());
				Settings.THE.save();
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
		Settings.THE.setBounds(SudokuFrame.this.getBounds());
		SudokuFrame.this.setVisible(false);
//		System.out.format("Chainer.createCellReductionHint nullRedPots=%d, nullChains=%d%s"
//				, diuf.sudoku.solver.hinters.chain.Chainer.ccrhNullRedPots
//				, diuf.sudoku.solver.hinters.chain.Chainer.ccrhNullChains, NL);
		try {
			if (engine != null) // nb: engine is final so I can't set it to null, but the stuff
			// it closes are nullable, so calling engine.close() a second+
			// time is a no-op.
			{
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
		return JOptionPane.showConfirmDialog(this, question, title,
				 JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	/**
	 * Ask the user for some String input.
	 */
	@Override
	public String askForString(String question, String title) {
		return JOptionPane.showInputDialog(this, question, title,
				 JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Complain to the user: with String 'msg' and 'title'.
	 */
	@Override
	public void carp(String msg, String title) {
		JOptionPane.showMessageDialog(this, msg, title,
				 JOptionPane.ERROR_MESSAGE);
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
		JOptionPane.showMessageDialog(this, message, title,
				 JOptionPane.ERROR_MESSAGE);
	}

}
