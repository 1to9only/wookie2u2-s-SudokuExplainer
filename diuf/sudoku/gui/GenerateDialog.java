/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import static diuf.sudoku.Tech.techNames;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.gen.Generator;
import static diuf.sudoku.gen.Generator.FACTORY_THREAD_NAME;
import static diuf.sudoku.gen.Generator.GENERATOR_THREAD_NAME;
import static diuf.sudoku.gen.Generator.MAX_FAILURES;
import static diuf.sudoku.gen.Generator.MAX_TRIES;
import static diuf.sudoku.gen.Generator.PRODUCE_THREAD_NAME;
import static diuf.sudoku.gen.Generator.getGenerator;
import diuf.sudoku.gen.Symmetry;
import diuf.sudoku.gen.Threads;
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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import static java.util.Arrays.asList;
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
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * The GenerateDialog allows the user to provide the parameters required to
 * generate a new Sudoku puzzle. It is always on top.
 * <p>
 * The tricky (slow) part is analysing the puzzle, not actually generating it.
 * If we accept puzzles of random difficulty it's fast, but generating a puzzle
 * that's exactly Fiendish is a bit more difficult, so when you press the
 * [Generate] button it displays a puzzle from the cache, and then replaces
 * that puzzle in the background, which may take several minutes for an IDKFA.
 * <p>
 * Note that all the supporting code for this form is in the package
 * {@code diuf.sudoku.gen}, which is a bit weird, but it works for me.
 *
 * @author Nicolas Juillerat 2006-2007
 * @author Keith Corlett 2018 Mar - Commented this class.
 */
public final class GenerateDialog extends JDialog {

	private static final long serialVersionUID = 8620081149465721387L;

	private static final String NL = System.lineSeparator();

// now called in SudokuExplainer itself, to pre-arm.
//	// load the PuzzleCache when the GenerateDialog form is first loaded, to
//	// give him time to load/generate a cache of puzzles BEFORE you press the
//	// Generate button. This only matters the first time a machine loads the
//	// generate form, and most users will poke-around-a-bit before pressing
//	// generate for the first time, so I'm relying on that time to pre-cache,
//	// which'll probably work for everything except IDKFA, which should take
//	// upto around 3 minutes to generate. I hope nobody goes big first fast,
//	// but if they do then they'll just have to wait for generate. sigh.
//	static {
//		PuzzleCache.staticInitialiser();
//	}

	// Techs the user must want, for speed. Generate is too slow without these.
	private static Tech[] speedTechs() {
		return new Tech[] {
			  Tech.NakedSingle
			, Tech.HiddenSingle
			, Tech.Locking
			, Tech.NakedPair
			, Tech.HiddenPair
			, Tech.Swampfish // aka X-Wing, but I prefer a fish related name.
		};
	}

	// Atleast one of these techs is required as a safety-net. DynamicPlus may
	// miss on the hardest puzzles, but all the Nested hinters ALWAYS hint.
	// DynamicPlus is a holey safety-net. Your risk-appetite is your affair.
	// Remember that safety-nets aren't called-upon until they're called-upon.
	// NestedUnary takes about four times as long to ALWAYS hint, but good luck
	// getting the bastard to run at all (with a sane Tech selection).
	private static Tech[] safetyNets() {
		return new Tech[] {
			  Tech.DynamicPlus
			, Tech.NestedUnary
			, Tech.NestedMultiple
			, Tech.NestedDynamic
			, Tech.NestedPlus
		};
	}

	private static String overrideTechsQuestion() {
		return
"Warning: not all the \"speed\" solving techniques are wanted.\n" +
"\n" +
"Solving a Sudoku takes much longer without all of these techniques, so Generate\n" +
"struggles without them, taking ages, or it could even run for ever.\n" +
"\n" +
"Speed Techs: Want ALL of:\n" +
techNames(speedTechs()) + "\n" +
"These are for speed really, not safety, but tick bloody tock!\n" +
"\n" +
"Safety Nets: Want ONE of:\n" +
techNames(safetyNets()) + "\n" +
"ONE of these is needed for safety, else hard puzzles may not solve.\n" +
"DynamicPlus covers normal puzzles; Nested* all cover ALL puzzles.\n" +
"\n" +
"Continue anyway?\n"
;
	}

	private static String overrideIdkfaQuestion() {
		return
"Warning: IDKFA puzzles are REALLY rare!\n" +
"\n" +
"It'll take distant monarch ages to generate an IDKFA, so I advise you to just press \"No\"\n" +
"and abandon all interest in the whole IDKFA thing. If you're dumb enough to press \"Yes\"\n" +
"anyway then SE will try "+lng(MAX_TRIES)+" times (aborting at "+lng(MAX_FAILURES)+" failures) to generate an IDKFA\n" +
"Sudoku that contains a DynamicPlus+ hint.\n" +
"\n" +
"The IDKFA Generator is hit and miss because SE generates \"random\" Sudoku puzzles\n" +
"and DynamicPlus+ hints are really rare, so it'll most probably take ages to strike one.\n" +
"\n" +
"I don't really understand this, but when run in JAVA.EXE the \""+PRODUCE_THREAD_NAME+"\" thread\n" +
"aborts after just a few minutes. I suspect that Winblows JAVA.EXE kills a daemon that\n" +
"is overheating the CPU, for safety, which is safe, but also prevents SE from generating\n" +
"an IDKFA. So, if you really want an IDKFA then run SE in JVM without a safety-net, as in\n" +
"all IDEs (I think), Personally, I use Nutbeans. There are many differences between VM's\n" +
"so it is probable that Nutbeans VM is saving me from myself in some other-way that\n" +
"JAVA.EXE does not.\n" +
"\n" +
"IDKFA. That's my matra. Leaning into stupidity since 1972'ish. Who's going to be there\n" +
"to clean-up the mess?\n" +
"\n" +
"So punk, the question is, have you got an hour, are you in an IDE, and are you currently\n" +
"feeling Felix Felicis level (ie REALLY ____ing) lucky. I'd recommend a beer, or four!\n"
;
	}

	private static String noWantedTechsMsg(final Tech[] techs) {
		return
"No Sudoku solving techniques are wanted that are in the selected difficulty,\n"
+"so generate would just run for-ever without producing a puzzle.\n"
+"\n"
+"Use Options menu ~ Solving techniques... to want one or more of: \n"
+wordWrap(techNames(techs), 80)+"\n"
+"or just choose another difficulty to generate.\n"
;
	}

	private final SudokuFrame frame;
	private final SudokuExplainer engine;
	private JButton btnGenerate;
	private JButton btnNext;
	private JButton btnPrev;
	private JButton btnClose;
	private JComboBox<Difficulty> cboDifficulty;
	private JRadioButton rdoExactDifficulty;
	private JRadioButton rdoMaxDifficulty;
	private JLabel lblDescription;
	private JCheckBox chkAnalysis;

	// a Set of the symettries selected for use.
	private final EnumSet<Symmetry> selectedSyms = EnumSet.noneOf(Symmetry.class);

	private Difficulty difficulty = Difficulty.Fiendish; // default selected
	private boolean isExact = true;

	private final List<Grid> sudokuList = new ArrayList<>();
	private int sudokuIndex = 0;
	private final Map<Grid, AHint> analysisMap = new HashMap<>();
	private final JCheckBox[] chkSymmetries = new JCheckBox[Symmetry.ALL.length];

	// it's safer to add/remove to/from a List, in case multiple are started.
	private final Queue<Thread> generatorThreads = new LinkedList<>();

	private static GenerateDialog theInstance;
	public static final GenerateDialog getGenerateDialog() {
		if ( theInstance == null )
			theInstance = new GenerateDialog();
		return theInstance;
	}

	/**
	 * Constructor: Constructs a new GenerateDialog which is owned by the given
	 * SudokuFrame, in which we display our results, using the SudokuExplainer
	 * to analyse the difficulty of generated puzzles.
	 * @param frame SudokuFrame just pass me this. Sigh.
	 * @param engine SudokuExplainer the engine
	 */
	private GenerateDialog() {
		super(SudokuExplainer.getInstance().frame, false);
		this.engine = SudokuExplainer.getInstance();
		this.frame = engine.frame;
		initGUI();
	}

	private boolean checkTechs() {
		return THE_SETTINGS.allWanted(speedTechs())
			&& THE_SETTINGS.anyWanted(safetyNets());
	}

	private boolean ask(final String question) {
		return YES_OPTION == showConfirmDialog(this, question, getTitle()
				, YES_NO_OPTION, QUESTION_MESSAGE);
	}

	private boolean userOverridesTechs() {
		return unsafeOk
			|| (unsafeOk = ask(overrideTechsQuestion()));
	}
	private boolean unsafeOk = false;

	private boolean userOverridesIdfka() {
		return idkfaOk
			|| (idkfaOk = ask(overrideIdkfaQuestion()));
	}
	private boolean idkfaOk = false;

	private void initGUI() {
		// init contents
		selectedSyms.addAll(asList(Symmetry.ALL));
		sudokuList.add(engine.getGrid());

		// This
		setTitle("Generate Sudoku");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Overall layout
		getContentPane().setLayout(new BorderLayout());

		// Parameters Panel: contains topPanel, middlePanel
		JPanel paramPanel = new JPanel();
		paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
		getContentPane().add(paramPanel, BorderLayout.CENTER);

		// Bottom Panel: Control Buttons
		JPanel bottomPanel = new JPanel(new GridLayout(1, 2)); //1 row, 2 cols
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		JPanel pnlGenerate = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPanel.add(pnlGenerate);

		btnPrev = new JButton();
		btnPrev.setText("<");
		btnPrev.setEnabled(false);
		btnPrev.setMnemonic(KeyEvent.VK_LEFT);
		btnPrev.setToolTipText("Restore the previous Sudoku");
		btnPrev.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				prev();
			}
		});
		pnlGenerate.add(btnPrev);

		btnGenerate = new JButton();
		btnGenerate.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
		btnGenerate.setText("Generate");
		btnGenerate.setMnemonic(KeyEvent.VK_G);
		btnGenerate.setToolTipText("Generate a new Sudoku puzzle");
		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( !generatorThreads.isEmpty() ) {
					stopGeneratorThread();
				} else {
					// check that a symmetry is selected
					if ( selectedSyms.isEmpty() ) {
						carp("Generate", "Select at least one symmetry");
						return;
					}
					// check the "speed" techs are wanted
					// and that a "safety-net" is wanted
					if ( !checkTechs() && !userOverridesTechs() )
						return;
					// IDKFA's are rare, so take ages to generate randomly.
					if ( difficulty==Difficulty.IDKFA && !userOverridesIdfka())
						return;
					// check that atleast one Tech of this difficulty is wanted
					final Tech[] dts = difficulty.techs();
					if ( !THE_SETTINGS.anyWanted(dts) ) {
						carp("Generate", noWantedTechsMsg(dts));
						return;
					}
					// start a new generatorThread
					startNewGeneratorThread();
				}
			}
		});
		pnlGenerate.add(btnGenerate);

		btnNext = new JButton();
		btnNext.setText(">");
		btnNext.setEnabled(false);
		btnNext.setMnemonic(KeyEvent.VK_RIGHT);
		btnNext.setToolTipText("Restore the next Sudoku");
		btnNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});
		pnlGenerate.add(btnNext);

		btnClose = new JButton();
		btnClose.setText("Close");
		btnClose.setMnemonic(KeyEvent.VK_C);
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		JPanel pnlClose = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnlClose.add(btnClose);
		bottomPanel.add(pnlClose);

		// Top Panel: Symmetry
		JPanel topPanel = new JPanel();
		topPanel.setBorder(new TitledBorder("Allowed symmetry types"));
		paramPanel.add(topPanel);
		topPanel.setLayout(new GridLayout(5, 2)); //was 3, 4
		int i = 0;
		for ( final Symmetry symmetry : Symmetry.values() ) {
			final JCheckBox chkSymmetry = chkSymmetries[i++] = new JCheckBox();
			chkSymmetry.setSelected(selectedSyms.contains(symmetry));
			chkSymmetry.setText(symmetry.toString());
			chkSymmetry.setToolTipText(symmetry.getDescription());
			chkSymmetry.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (chkSymmetry.isSelected()) {
						selectedSyms.add(symmetry);
					} else {
						selectedSyms.remove(symmetry);
					}
				}
			});
			topPanel.add(chkSymmetry);
		}

		// Middle Panel: Difficulty
		JPanel middlePanel = new JPanel(new BorderLayout());
		middlePanel.setBorder(new TitledBorder("Difficulty"));
		paramPanel.add(middlePanel);

		JPanel difficultyPanel = new JPanel();
		difficultyPanel.setLayout(new BoxLayout(difficultyPanel, BoxLayout.X_AXIS));
		middlePanel.add(difficultyPanel, BorderLayout.NORTH);

		cboDifficulty = new JComboBox<>();
		for ( Difficulty d : Difficulty.values() )
			cboDifficulty.addItem(d);
		cboDifficulty.setToolTipText("Choose the difficulty of the Sudoku to generate");
		cboDifficulty.setSelectedItem(Difficulty.Fiendish);
		cboDifficulty.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				difficulty = (Difficulty)cboDifficulty.getSelectedItem();
				lblDescription.setText(difficulty.getHtml());
				selectSymmetries(difficulty);
			}
		});
		difficultyPanel.add(cboDifficulty);

		rdoExactDifficulty = new JRadioButton("Exact");
		rdoExactDifficulty.setToolTipText("Generate a Sudoku with exactly the chosen difficulty");
		rdoExactDifficulty.setMnemonic(KeyEvent.VK_E);
		rdoExactDifficulty.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (rdoExactDifficulty.isSelected())
					isExact = true;
			}
		});
		difficultyPanel.add(rdoExactDifficulty);

		rdoMaxDifficulty = new JRadioButton("Maximum");
		rdoMaxDifficulty.setToolTipText("Generate a Sudoku with at most the chosen difficulty");
		rdoMaxDifficulty.setMnemonic(KeyEvent.VK_M);
		rdoMaxDifficulty.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (rdoMaxDifficulty.isSelected())
					isExact = false;
			}
		});
		difficultyPanel.add(rdoMaxDifficulty);
		ButtonGroup difficultyRadioGroup = new ButtonGroup();
		difficultyRadioGroup.add(rdoExactDifficulty);
		difficultyRadioGroup.add(rdoMaxDifficulty);
		rdoExactDifficulty.setSelected(true);

		lblDescription = new JLabel();
		lblDescription.setText(
				"<html><body><b>This</b><br>is just<br>a text.</body></html>");
		lblDescription.setToolTipText("Explanation of the chosen difficulty");
		invokeLater(new Runnable() {
			@Override
			public void run() {
				lblDescription.setText(difficulty.getHtml());
			}
		});
		final JPanel pnlDescription = new JPanel(new BorderLayout());
		pnlDescription.setBorder(new TitledBorder("Description"));
		pnlDescription.add(lblDescription, BorderLayout.NORTH);
		middlePanel.add(pnlDescription, BorderLayout.CENTER);

		// Option Panel: Analysis
		JPanel optionPanel = new JPanel();
		optionPanel.setBorder(new TitledBorder(""));
		optionPanel.setLayout(new GridLayout(1, 1));
		paramPanel.add(optionPanel, BorderLayout.NORTH);
		chkAnalysis = new JCheckBox("Show the analysis of the generated Sudoku");
		chkAnalysis.setToolTipText("Solve the Sudoku to rate its difficulty,"
				+ " and summarise the techiques required.");
		chkAnalysis.setMnemonic(KeyEvent.VK_A);
		chkAnalysis.setSelected(true);
		chkAnalysis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshSudokuPanel();
			}
		});
		optionPanel.add(chkAnalysis);

		if ( Threads.exists(FACTORY_THREAD_NAME) ) {
			btnGenerate.setEnabled(false);
			// Log the "trouble". This is the only notification.
			Log.teeln(FACTORY_THREAD_NAME+" running, so generate is disabled!");
			// disable generate until factory finishes (one at a time!)
			invokeLater(new Runnable() {
				@Override
				public void run() {
					for(;;) {
						try{Thread.sleep(333);}catch(InterruptedException eaten){}
						if ( !Threads.exists(FACTORY_THREAD_NAME) )
							btnGenerate.setEnabled(true);
					}
				}
				@Override
				public String toString() {
					return "GenerateEnabler";
				}

			});
		}

		// set the minimum size of the dialog, otherwise the dialog isn't tall
		// enough to fit the maximum height of lblDesciption (which shrinks and
		// grows as the description changes) so it and controls below it are
		// pushed below the bottom of the dialog, which sux, especially when
		// the user can't resize the dialog to fix it.
		setMinimumSize(new Dimension(400, 400));
		setResizable(false); // prevent user accidentally disappearing controls
		pack();
	}

	// setSelected on those chkSymmetries appropriate for the given Difficulty.
	private void selectSymmetries(Difficulty diff) {
		switch(diff) {
		case Easy:
		case Medium:
		case Hard:
		case Fiendish:
			selectedSyms.addAll(asList(Symmetry.ALL));
			break;
		case Airotic:
		case AlsFish:
		case Ligature:
			selectedSyms.addAll(asList(Symmetry.ALL));
			selectedSyms.remove(Symmetry.Full_8); // basically impossible
			break;
		case Diabolical:
			// basically impossible with 4 or 8
			selectedSyms.clear();
			selectedSyms.addAll(asList(Symmetry.SMALLS));
			break;
		case IDKFA:
			// basically impossible with anything but none
			selectedSyms.clear();
			selectedSyms.add(Symmetry.None);
		}
		// refresh the selected symetries check boxes
		for ( int i=0,n=chkSymmetries.length; i<n; ++i )
			chkSymmetries[i].setSelected(selectedSyms.contains(Symmetry.ALL[i]));
		repaint();
	}

	void generate() {
		btnGenerate.doClick();
	}

	private void startNewGeneratorThread() {
		// Generate grid
		Thread gt = new GeneratorThread(
				  selectedSyms.toArray(new Symmetry[selectedSyms.size()])
				, difficulty
				, isExact
		);
		generatorThreads.add(gt);
		gt.start();
	}

	public void generateCompleted() {
		generatorThreads.poll(); // remove the current generator thread
		refreshSudokuPanel();
		displayGenerateButton();
	}

	public void generateKilled() {
		generatorThreads.poll(); // remove the current generator thread
		refreshSudokuPanel();
		displayGenerateButton();
	}

	public void stopGeneratorThread() {
		Thread gt = generatorThreads.poll();
		if ( gt != null ) {
			gt.interrupt();
			try {
				gt.join(250); // wait a quarter second
				refreshSudokuPanel();
				displayGenerateButton();
			} catch (InterruptedException ex) {
				Log.teef(Log.me()+": interrupted");
			}
		}
	}

	private void carp(String title, String message) {
		JOptionPane.showMessageDialog(this, message, title
				, JOptionPane.ERROR_MESSAGE);
	}

	public void addGeneratorThread(Thread gt) {
		generatorThreads.add(gt);
	}

	/**
	 * Thread that generates a new grid.
	 */
	private final class GeneratorThread extends Thread {
		private final Symmetry[] symetries;
		private final Difficulty difficulty;
		private final boolean isExact;
	    public GeneratorThread(Symmetry[] symetries, Difficulty difficulty, boolean isExact) {
			super(GENERATOR_THREAD_NAME);
			this.symetries = symetries;
			this.difficulty = difficulty;
			this.isExact = isExact;
		}
		@Override
		public void interrupt() {
			getGenerator().interrupt();
		}
		@Override
		public void run() {
			try {
				displayStopButton(); // btnGenerate says "Stop"
				// fetch a puzzle from the cache
				final Generator gen = getGenerator();
				final Grid puzzle = gen.cachedGenerate(symetries, difficulty, isExact);
				if ( puzzle != null ) {
					// display the puzzle in the GUI on the AWT thread
					// nb: I know not why but a lambda does not work here, it
					// appears to invalidate hinters array fields. Funky GC?
					invokeLater(new Runnable() {
						@Override
						public void run() {
							sudokuList.add(puzzle);
							sudokuIndex = sudokuList.size() - 1;
							refreshSudokuPanel(); // sets engine.grid
							engine.saveFile(IO.GENERATED_FILE); // also adds to recentFiles, and catches IOException in a standard way
							frame.setTitle(IO.GENERATED_FILE.getAbsolutePath());
						}
					});
				}
			} catch ( Exception ex ) {
				Log.stackTrace(Log.me()+" hit a snag.", ex);
			}
		}
	}

	// first update the GUI on the Swing thread
	private void displayStopButton() {
		try {
			invokeAndWait(new Runnable() { // NOTE: wait!
				@Override
				public void run() {
					engine.setGrid(new Grid()); // clear the grid
					engine.frame.setHintDetailArea("<html><body>Generating...</body></html>");
					AutoBusy.setBusy(GenerateDialog.this, true);
					AutoBusy.setBusy(btnGenerate, false);
					btnGenerate.setText("Stop");
					btnGenerate.repaint();
				}
			});
		} catch (Exception eaten) {
			// Do nothing
		}
	}

	public boolean doDone = true;
	private void displayGenerateButton() {
		if ( doDone ) {
			invokeLater(new Runnable() {
				@Override
				public void run() {
					if ( GenerateDialog.this.isVisible() ) {
						AutoBusy.setBusy(GenerateDialog.this, false);
						AutoBusy.setBusy(btnGenerate, false);
						btnGenerate.setText("Generate");
						btnGenerate.repaint();
					}
				}
			});
		}
	}

	private void next() {
		if (sudokuIndex < sudokuList.size() - 1) {
			sudokuIndex += 1;
			refreshSudokuPanel();
		}
	}

	private void prev() {
		if (sudokuIndex > 0) {
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
					engine.frame.setHintDetailArea("<html><body>Generate was interrupted!</body></html>");
					return;
				}
				analysisMap.put(grid, analysis);
			}
			engine.showHint(analysis);
		}
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
