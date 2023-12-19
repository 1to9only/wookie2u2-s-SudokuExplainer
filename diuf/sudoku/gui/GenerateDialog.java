/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.lieDown;
import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import static diuf.sudoku.gui.Generator.FACTORY_THREAD_NAME;
import static diuf.sudoku.gui.Generator.GENERATOR_THREAD_NAME;
import static diuf.sudoku.gui.Generator.MAX_FAILURES;
import static diuf.sudoku.gui.Generator.MAX_TRIES;
import static diuf.sudoku.gui.Generator.PRODUCE_THREAD_NAME;
import diuf.sudoku.io.IO;
import diuf.sudoku.solver.AHint;
import static diuf.sudoku.utils.Frmt.lng;
import diuf.sudoku.utils.Log;
import static diuf.sudoku.utils.MyStrings.wordWrap;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.utils.Frmt.NL;
import java.util.Collection;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

/**
 * The GenerateDialog facilitates getting Generate parameters from the user in
 * order to provide those parameters required to generate a new Sudoku puzzle.
 * The GenerateDialog is always on top.
 * <p>
 * The tricky (slow) part is analysing the puzzle, not actually generating it.
 * If we accept puzzles of random difficulty its fast, but generating a puzzle
 * of a specific difficulty is harder, so when you press the [Generate] button
 * it just displays a cached puzzle, which is then replaced by a background
 * (daemon) thread, which may take distant monarch ages for an IDKFA.
 * <p>
 * Note that all the supporting code for this form is in the package
 * {@code diuf.sudoku.gen}, which is a bit weird, but it works for me.
 *
 * @author Nicolas Juillerat 2006-2007
 * @author Keith Corlett 2018 Mar - Commented this class.
 */
final class GenerateDialog extends JDialog {

	private static final long serialVersionUID = 8620081149465721387L;

	private static boolean threadExists(String targetThreadName) {
		final Thread[] threads = new Thread[Thread.activeCount()];
		int n = Thread.enumerate(threads);
		for ( int i=0; i<n; ++i )
			if ( targetThreadName.equals(threads[i].getName()) )
				return true;
		return false;
	}

	// Techs the user must want, for speed. Generate is too slow without these.
	private static EnumSet<Tech> speedTechs() {
		return Tech.enumSet(Tech.NakedSingle, Tech.HiddenSingle, Tech.Locking
			, Tech.NakedPair, Tech.HiddenPair, Tech.Swampfish);
	}

	// Atleast one of these techs is required as a safety-net. DynamicPlus may
	// miss on the hardest puzzles, but all the Nested hinters ALWAYS hint.
	// DynamicPlus is a holey safety-net. Your risk-appetite is your affair.
	// Remember safety-nets are not called-upon until they are called-upon.
	// NestedUnary takes about four times as long to ALWAYS hint, but good luck
	// getting the bastard to run at all (with a sane Tech selection).
	private static EnumSet<Tech> safetyNets() {
		return Tech.enumSet(Tech.DynamicPlus, Tech.NestedUnary
			, Tech.NestedStatic, Tech.NestedDynamic, Tech.NestedPlus);
	}

	// nb: it appears that the nicest memory management for a "static String"
	// is to return it from its own method, so that is what I do. These where
	// private static final String = "yada yada"; but that keeps the string
	// permanently in memory, which is just a waste of RAM. This tecnique also
	// facilitates in-line "string expansion", rather than pissin-about with
	// search and replace (as per all of SEs html files, sigh).
	private static String techsQuestion() {
		return
"Warning: not all the \"speed\" solving techniques are wanted." + NL +
NL +
"Solving a Sudoku takes much longer without all of these techniques, so Generate" + NL +
"struggles without them, taking ages, or it could even run for ever." + NL +
NL +
"Speed Techs: Want ALL of:" + NL +
Tech.names(speedTechs()) + NL +
"These are for speed really, not safety, but tick bloody tock!" + NL +
NL +
"Safety Nets: Want ONE of:" + NL +
Tech.names(safetyNets()) + NL +
"ONE of these is needed for safety, else hard puzzles may not solve." + NL +
"DynamicPlus covers normal puzzles; Nested* all cover ALL puzzles." + NL +
NL +
"Continue anyway?" + NL
;
	}

	private static String idkfaQuestion() {
		return
"Warning: Generating an IDKFA puzzle takes ages!" + NL +
NL +
"It usually takes distant monarch ages to generate an IDKFA, so I advise you to just press \"No\" and" + NL +
"and abandon all interest in the whole IDKFA thing. But if you are dumb enough to press \"Yes\" then" + NL +
"SE will try "+lng(MAX_TRIES)+" times (aborting at "+lng(MAX_FAILURES)+" failures) to generate an IDKFA" + NL +
"Sudoku that contains a DynamicPlus or harder hint." + NL +
NL +
"IDKFA Generation is VERY hit-and-miss because SE generates \"random\" Sudoku puzzles" + NL +
"and DynamicPlus+ hints are really rare, so it will most probably take ages to hit one." + NL +
NL +
"I do not really understand this, but when run in JAVA.EXE the \""+PRODUCE_THREAD_NAME+"\" thread" + NL +
"aborts after just a few minutes. I suspect that Winblows JAVA.EXE kills a daemon that" + NL +
"is overheating the CPU, for safety, which is safe, but also prevents SE from generating" + NL +
"an IDKFA. So, if you really want an IDKFA then run SE in JVM without a safety-net, as in" + NL +
"all IDEs (I think), Personally, I use Nutbeans. There are many differences between VMs" + NL +
"so it is probable that Nutbeans VM is saving me from myself in some other-way that" + NL +
"JAVA.EXE does not." + NL +
NL +
"So punk, the question is, have you got an hour, are you in an IDE, and are you currently" + NL +
"feeling REALLY ____ing lucky. If so then Press \"Yes\" and go fetch a beer. Please wait..." + NL
;
	}

	private static String noWantedTechsMsg(final Collection<Tech> techs) {
		return
"No Sudoku solving techniques are wanted that are in the selected difficulty," + NL +
"so generate would just run for-ever without producing a puzzle." + NL +
NL +
"Use Options menu ~ Solving techniques... to want one or more of: " + NL +
wordWrap(Tech.names(techs), 80)+"" + NL +
"or just choose another difficulty to generate." + NL
;
	}

	private final SudokuFrame frame;
	private final SudokuExplainer engine;
	private JButton btnGenerate;
	private JButton btnNext;
	private JButton btnPrev;
	private JComboBox<Difficulty> cboDifficulty;
	private JRadioButton rdoExactDifficulty;
	private JRadioButton rdoMaxDifficulty;
	private JLabel lblDescription;
	private JCheckBox chkAnalysis;

	// a Set of the symettries selected for use.
	private final EnumSet<GenSymmetry> selectedSyms = EnumSet.noneOf(GenSymmetry.class);

	// get difficulty from CFG
	private Difficulty difficulty = CFG.getGenerateDifficulty(Difficulty.Hard);

	private boolean isExact = true;

	private final List<Grid> sudokuList = new ArrayList<>();
	private int sudokuIndex = 0;
	private final Map<Grid, AHint> analysisMap = new HashMap<>();
	private final JCheckBox[] chkSymmetries = new JCheckBox[GenSymmetry.ALL.size()];

	// its safer to add/remove to/from a List, in case multiple are started.
	private final Queue<Thread> generatorThreads = new LinkedList<>();

	private static GenerateDialog theInstance;
	static final GenerateDialog getInstance() {
		if ( theInstance == null )
			theInstance = new GenerateDialog(SudokuExplainer.getInstance().getFrame());
		return theInstance;
	}

	private boolean configChanged = false;

	/**
	 * Constructor: Constructs a new GenerateDialog which is owned by the given
	 * SudokuFrame, in which we display our results, using the SudokuExplainer
	 * to analyse the difficulty of generated puzzles.
	 * @param frame SudokuFrame just pass me this. Sigh.
	 * @param engine SudokuExplainer the engine
	 */
	private GenerateDialog(final SudokuFrame parent) {
		super(parent, false);
		this.engine = SudokuExplainer.getInstance();
		this.frame = parent;
		initGUI();
	}

	private boolean override(final String question) {
		return YES_OPTION == showConfirmDialog(this, question, getTitle()
				, YES_NO_OPTION, QUESTION_MESSAGE);
	}

	private boolean userOverridesTechs() {
		return unsafeOk || (unsafeOk = override(techsQuestion()));
	}
	private boolean unsafeOk = false;

	private boolean userOverridesIdkfa() {
		return idkfaOk || (idkfaOk = override(idkfaQuestion()));
	}
	private boolean idkfaOk = false;

	private void initGUI() {
		// init contents
		selectedSyms.addAll(GenSymmetry.ALL);
		sudokuList.add(engine.getGrid());

		// This
		setTitle("Generate Sudoku");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Overall layout
		getContentPane().setLayout(new BorderLayout());

		// Parameters Panel: contains topPanel, middlePanel
		final JPanel paramPanel = new JPanel();
		paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
		getContentPane().add(paramPanel, BorderLayout.CENTER);

		// Bottom Panel: Control Buttons
		final JPanel bottomPanel = new JPanel(new GridLayout(1, 2)); //1 row, 2 cols
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		final JPanel generatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPanel.add(generatePanel);

		JButton jb = new JButton();
		jb.setText("<");
		jb.setEnabled(false);
		jb.setMnemonic(KeyEvent.VK_LEFT);
		jb.setToolTipText("Restore the previous Sudoku");
		jb.addActionListener((ActionEvent e) -> prev());
		generatePanel.add(btnPrev=jb);

		jb = new JButton();
		jb.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
		jb.setText("Generate");
		jb.setMnemonic(KeyEvent.VK_G);
		jb.setToolTipText("Generate a new Sudoku puzzle");
		jb.addActionListener((ActionEvent e) -> {
			if ( !generatorThreads.isEmpty() ) {
				stopGeneratorThread();
				return;
			}
			// check a symmetry is selected
			if ( selectedSyms.isEmpty() ) {
				carp("Generate", "Select at least one symmetry");
				return;
			}
			// check all "speed" techs are wanted and a "safety-net" is wanted
			if ( !(CFG.allWanted(speedTechs()) && CFG.anyWanted(safetyNets()))
			  && !userOverridesTechs() )
				return;
			// IDKFAs are rare, so take distant monarch ages to generate.
			if ( difficulty==Difficulty.IDKFA && !userOverridesIdkfa() )
				return;
			// check atleast one Tech of this difficulty is wanted
			final Collection<Tech> dts = difficulty.getTechs();
			if ( !CFG.anyWanted(dts) ) {
				carp("Generate", noWantedTechsMsg(dts));
				return;
			}
			if ( configChanged )
				CFG.save();
			// kick-off.
			startNewGeneratorThread();
		});
		generatePanel.add(btnGenerate=jb);

		jb = new JButton();
		jb.setText(">");
		jb.setEnabled(false);
		jb.setMnemonic(KeyEvent.VK_RIGHT);
		jb.setToolTipText("Restore the next Sudoku");
		jb.addActionListener((ActionEvent e) -> next());
		generatePanel.add(btnNext=jb);

		jb = new JButton();
		jb.setText("Close");
		jb.setMnemonic(KeyEvent.VK_C);
		jb.addActionListener((ActionEvent e) -> close());
		final JPanel pnlClose = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnlClose.add(jb);
		bottomPanel.add(pnlClose);

		// Top Panel: Symmetry
		final JPanel topPanel = new JPanel();
		topPanel.setBorder(new TitledBorder("Allowed symmetry types"));
		paramPanel.add(topPanel);
		topPanel.setLayout(new GridLayout(5, 2)); //was 3, 4
		int i = 0;
		for ( final GenSymmetry symmetry : GenSymmetry.values() ) {
			final JCheckBox chkSymmetry = chkSymmetries[i++] = new JCheckBox();
			chkSymmetry.setSelected(selectedSyms.contains(symmetry));
			chkSymmetry.setText(symmetry.toString());
			chkSymmetry.setToolTipText(symmetry.getDescription());
			chkSymmetry.addActionListener((ActionEvent e) -> {
				if ( chkSymmetry.isSelected() ) {
					selectedSyms.add(symmetry);
				} else {
					selectedSyms.remove(symmetry);
				}
			});
			topPanel.add(chkSymmetry);
		}

		// Middle Panel: Difficulty
		final JPanel middlePanel = new JPanel(new BorderLayout());
		middlePanel.setBorder(new TitledBorder("Difficulty"));
		paramPanel.add(middlePanel);

		final JPanel difficultyPanel = new JPanel();
		difficultyPanel.setLayout(new BoxLayout(difficultyPanel, BoxLayout.X_AXIS));
		middlePanel.add(difficultyPanel, BorderLayout.NORTH);

		JComboBox<Difficulty> jcb = new JComboBox<>();
		for ( Difficulty d : Difficulty.values() )
			jcb.addItem(d);
		jcb.setToolTipText("Choose the difficulty of the Sudoku to generate");
		jcb.setSelectedItem(difficulty);
		jcb.addActionListener((ActionEvent e) -> {
			difficulty = (Difficulty)cboDifficulty.getSelectedItem();
			lblDescription.setText(difficulty.getHtml());
			selectSymmetries(difficulty);
			CFG.setGenerateDifficulty(difficulty);
			configChanged = true; // the generate button saves
		});
		difficultyPanel.add(cboDifficulty=jcb);

		JRadioButton jrb = new JRadioButton("Exact");
		jrb.setToolTipText("Generate a Sudoku with exactly the chosen difficulty");
		jrb.setMnemonic(KeyEvent.VK_E);
		jrb.addActionListener((ActionEvent e) -> {
			if (rdoExactDifficulty.isSelected())
				isExact = true;
		});
		difficultyPanel.add(rdoExactDifficulty=jrb);

		jrb = new JRadioButton("Maximum");
		jrb.setToolTipText("Generate a Sudoku with at most the chosen difficulty");
		jrb.setMnemonic(KeyEvent.VK_M);
		jrb.addActionListener((ActionEvent e) -> {
			if ( rdoMaxDifficulty.isSelected() ) {
				isExact = false;
			}
		});
		difficultyPanel.add(rdoMaxDifficulty=jrb);
		final ButtonGroup bg = new ButtonGroup();
		bg.add(rdoExactDifficulty);
		bg.add(rdoMaxDifficulty);
		rdoExactDifficulty.setSelected(true);

		final JLabel jlText = new JLabel();
		jlText.setText("<html><body>Text!</body></html>");
		jlText.setToolTipText("Explanation of the chosen difficulty");
		invokeLater(() -> jlText.setText(difficulty.getHtml()));
		final JPanel pnlDescription = new JPanel(new BorderLayout());
		pnlDescription.setBorder(new TitledBorder("Description"));
		pnlDescription.add(lblDescription=jlText, BorderLayout.NORTH);
		middlePanel.add(pnlDescription, BorderLayout.CENTER);

		// Option Panel: Analysis
		final JPanel optionPanel = new JPanel();
		optionPanel.setBorder(new TitledBorder(""));
		optionPanel.setLayout(new GridLayout(1, 1));
		paramPanel.add(optionPanel, BorderLayout.NORTH);
		JCheckBox jkb = new JCheckBox("Show the analysis of the generated Sudoku");
		jkb.setToolTipText("Solve the Sudoku to summarise techiques required.");
		jkb.setMnemonic(KeyEvent.VK_A);
		jkb.setSelected(true);
		jkb.addActionListener((ActionEvent e) -> refreshSudokuPanel());
		optionPanel.add(chkAnalysis=jkb);

		if ( threadExists(FACTORY_THREAD_NAME) ) {
			btnGenerate.setEnabled(false);
			// Log the "trouble". This is the only notification.
			Log.teeln(FACTORY_THREAD_NAME+" running, so generate is disabled!");
			// disable generate until factory finishes (one at a time!)
			invokeLater(() -> {
				for(;;) {
					lieDown(333);
					if ( !threadExists(FACTORY_THREAD_NAME) )
						btnGenerate.setEnabled(true);
				}
			});
		}

		// set the minimum size of the dialog, otherwise the dialog is not tall
		// enough to fit the maximum height of lblDesciption (which shrinks and
		// grows as the description changes) so it and controls below it are
		// pushed below the bottom of the dialog, which looks crap.
		// PARTIAL FIX: the control buttons are now in there own panel that is
		// attached to bottom-of-form so the "show generated" checkBox (et al)
		// disappears underneath da buttons panel, which is atleast better than
		// occluding the cancel button!
		setMinimumSize(new Dimension(410, 410));
		setMaximumSize(new Dimension(510, 510));
		setResizable(true); // user can disappear controls if they want
		pack();
	}

	// setSelected on those chkSymmetries appropriate for the given Difficulty.
	private void selectSymmetries(final Difficulty difficulty) {
		switch ( difficulty ) {
		case Easy:
		case Medium:
		case Hard:
		case Fiendish:
			selectedSyms.addAll(GenSymmetry.ALL);
			break;
		case Alsic:
		case Fisch:
		case Ligature:
			selectedSyms.addAll(GenSymmetry.ALL);
			selectedSyms.remove(GenSymmetry.Full_8); // basically impossible
			break;
		case Diabolical:
			// basically impossible with 4 or 8
			selectedSyms.clear();
			selectedSyms.addAll(GenSymmetry.SMALLS);
			break;
		case IDKFA:
			// basically impossible with anything but none
			selectedSyms.clear();
			selectedSyms.add(GenSymmetry.None);
		}
		// refresh the selected symetries check boxes
		for ( int i=0,n=chkSymmetries.length; i<n; ++i )
			chkSymmetries[i].setSelected(selectedSyms.contains(GenSymmetry.ALL.get(i)));
		repaint();
	}

	void generate() {
		btnGenerate.doClick();
	}

	private void startNewGeneratorThread() {
		// Generate grid
		final Thread t = new GeneratorThread(selectedSyms, difficulty, isExact);
		generatorThreads.add(t);
		t.start();
	}

	void generateStopped() {
		// turns out I am the same. Sigh.
		generateCompleted();
	}

	void generateCompleted() {
		generatorThreads.poll(); // remove the current generator thread
		refreshSudokuPanel();
		displayGenerateButton();
	}

	void stopGeneratorThread() {
		final Thread thread = generatorThreads.poll();
		if ( thread != null ) {
			thread.interrupt();
			try {
				thread.join(250); // wait a quarter second
				refreshSudokuPanel();
				displayGenerateButton();
			} catch (InterruptedException ex) {
				Log.teef("WARN: "+Log.me()+": interrupted");
			}
		}
	}

	private void carp(final String title, final String message) {
		showMessageDialog(this, message, title, ERROR_MESSAGE);
	}

	void addGeneratorThread(final Thread gt) {
		generatorThreads.add(gt);
	}

	/**
	 * Thread that generates a new grid.
	 */
	private final class GeneratorThread extends Thread {
		private final ArrayList<GenSymmetry> symmetries;
		private final Difficulty difficulty;
		private final boolean isExact;
	    GeneratorThread(final EnumSet<GenSymmetry> symmetries, final Difficulty difficulty, final boolean isExact) {
			super(GENERATOR_THREAD_NAME);
			// I think I should copy collection into new thread, so mine is
			// mine, and therefore utterly immutable. No trust anybody!
			this.symmetries = new ArrayList<>(symmetries);
			this.difficulty = difficulty;
			this.isExact = isExact;
		}
		@Override
		public void interrupt() {
			Generator.getInstance().interrupt();
		}
		@Override
		public void run() {
			try {
				displayStopButton(); // btnGenerate says "Stop"
				// fetch a puzzle from the cache
				final Generator gen = Generator.getInstance();
				final Grid puzzle = gen.cachedGenerate(symmetries, difficulty, isExact);
				if ( puzzle != null ) {
					// display the puzzle in the GUI on the AWT thread.
					invokeLater(() -> {
						try {
							sudokuList.add(puzzle);
							sudokuIndex = sudokuList.size() - 1;
							refreshSudokuPanel(); // sets engine.grid
							engine.saveFile(IO.GENERATED_FILE); // also adds to recentFiles, and catches IOException in a standard way
							frame.setTitle(IO.GENERATED_FILE.getAbsolutePath());
						} catch ( Exception ex ) {
							Log.teeTrace(GENERATOR_THREAD_NAME+".refreshSP caught ", ex);
						}
					});
				}
			} catch ( Exception ex ) {
				Log.teeTrace(GENERATOR_THREAD_NAME+".run caught ", ex);
			}
		}
	}

	// first update the GUI on the Swing thread
	private void displayStopButton() {
		try {
			// NOTE: wait!
			invokeAndWait(() -> {
				engine.setGrid(new Grid()); // clear the grid
				frame.setHintDetailArea("<html><body>Generating...</body></html>");
				AutoBusy.setBusy(GenerateDialog.this, true);
				AutoBusy.setBusy(btnGenerate, false);
				btnGenerate.setText("Stop");
				btnGenerate.repaint();
			});
		} catch (Exception eaten) {
			// Do nothing
		}
	}

	boolean doDone = true;
	private void displayGenerateButton() {
		if ( doDone ) {
			invokeLater(() -> {
				if ( GenerateDialog.this.isVisible() ) {
					AutoBusy.setBusy(GenerateDialog.this, false);
					AutoBusy.setBusy(btnGenerate, false);
					btnGenerate.setText("Generate");
					btnGenerate.repaint();
				}
			});
		}
	}

	private void next() {
		if ( sudokuIndex < sudokuList.size() - 1 ) {
			sudokuIndex += 1;
			refreshSudokuPanel();
		}
	}

	private void prev() {
		if ( sudokuIndex > 0 ) {
			sudokuIndex -= 1;
			refreshSudokuPanel();
		}
	}

	private void refreshSudokuPanel() {
		final Grid grid = sudokuList.get(sudokuIndex);
		engine.setGrid(grid);
		// Generate buggers-up krakens
		btnPrev.setEnabled(sudokuIndex > 0);
		btnNext.setEnabled(sudokuIndex < sudokuList.size()-1);
		if ( chkAnalysis.isSelected() ) {
			// Display the analysis of this Sudoku
			AHint analysis = analysisMap.get(grid);
			if ( analysis == null ) {
				analysis = engine.analysePuzzle(false, false);
				if ( analysis == null ) {
					frame.setHintDetailArea("<html><body>Generate was interrupted!</body></html>");
					return;
				}
				analysisMap.put(grid, analysis);
			}
			engine.showHint(analysis);
		}
		frame.updateHintsCache();
	}

	@Override
	public void dispose() {
		close();
	}

	private void close() {
		while ( generatorThreads.peek() != null )
			stopGeneratorThread();
		super.setVisible(false);
		super.dispose();
	}

}
