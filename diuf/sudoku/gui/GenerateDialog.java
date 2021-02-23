/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.Settings;
import diuf.sudoku.gen.Generator;
import diuf.sudoku.gen.PuzzleCache;
import diuf.sudoku.gen.Symmetry;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;


/**
 * The GenerateDialog is allways on top. It allows the user to enter desired
 * parameters and generate a new Sudoku puzzle, which is the paragon of
 * Sudokuness: You see a duck, and I see his feet.
 * <p>The tricky (slow) part is analysing the puzzle, not actually generating
 * it. If we wanted to sit in a loop generating thousands of puzzles of random
 * difficulty that'd be quick, however generating a puzzle that's exactly
 * "Fiendish" is difficult, so I've pushed that onto a background thread.
 * When you press the generate button is displays a puzzle from the cache and
 * then replaces that puzzle in the background, which takes upto a few seconds.
 * <p>Note that all the supporting code for this form is in the package
 * {@code diuf.sudoku.gen}, which is a bit weird but it works for me.
 * @author Nicolas Juillerat 2006-2007
 * @author Keith Corlett 2018 Mar - Commented this class.
 */
public final class GenerateDialog extends JDialog {

	private static final long serialVersionUID = 8620081149465721387L;

	// load the PuzzleCache when the GenerateDialog form is first loaded, to
	// give him time to load/generate a cache of puzzles BEFORE you press the
	// Generate button.
	static {
		PuzzleCache.staticInitialiser();
	}

	// Techs we require the user to use, they're not used here!
	private static final Tech[] SAFE_TECHS = new Tech[] {
		// these are really for speed, not actually required for safety
		  Tech.NakedSingle
		, Tech.HiddenSingle
		, Tech.Locking
		, Tech.NakedPair
		, Tech.HiddenPair
		, Tech.Swampfish // aka X-Wing, but I prefer a fish related name.
		// keeper: DynamicPlus is required coz it allways (AFAIK) finds a hint.
		// ~50ms per call. Not used unless user unwants too many hinters.
		, Tech.DynamicPlus
	};

	private final SudokuFrame frame;
	private final SudokuExplainer engine;
	private JButton btnGenerate;
	private JButton btnNext;
	private JButton btnPrev;
	private JLabel lblDifficulty;
	private JCheckBox chkAnalysis;

	// a Set of the symettries selected for use.
	private final EnumSet<Symmetry> selectedSymmetries
			= EnumSet.noneOf(Symmetry.class);

	private Difficulty difficulty = Difficulty.Fiendish;
	private boolean isExact = true;

	private volatile GeneratorThread generatorThread = null;
	private final List<Grid> sudokuList = new ArrayList<>();
	private int sudokuIndex = 0;
	private final Map<Grid, AHint> analysisses = new HashMap<>();
	private final JCheckBox[] chkSymmetries = new JCheckBox[Symmetry.ALL.length];

	/**
	 * Constructor: Constructs a new GenerateDialog which is owned by the given
	 * SudokuFrame, in which we display our results, using the SudokuExplainer
	 * to analyse the difficulty of generated puzzles.
	 * @param frame SudokuFrame just pass me this. Sigh.
	 * @param engine SudokuExplainer the engine
	 */
	GenerateDialog(SudokuFrame frame, SudokuExplainer engine) {
		super(frame, false);
		this.frame = frame;
		this.engine = engine;
		initParameters();
		initGUI();
	}

	private void initParameters() {
		// Select the 4/8 pointed symmetries coz they seem to be fastest.
//		selectedSymmetries.add(Symmetry.Bidiagonal_4);	// 4
//		selectedSymmetries.add(Symmetry.Orthogonal_4);	// 4
//		selectedSymmetries.add(Symmetry.Rotate90_4);	// 4
//		selectedSymmetries.add(Symmetry.Full_8);		// 8
		selectedSymmetries.addAll(Arrays.asList(Symmetry.ALL));
		sudokuList.add(engine.getGrid());
	}

	private boolean checkRuleSetIsSafe() {
		return Settings.THE.areAllWanted(SAFE_TECHS);
	}

	private static final String CONTINUE_QUESTION =
"<html><body>" +
"<b>Warning:</b> not all the \"safe\" solving techniques are enabled.<br>" +
"Sudoku Explainer may not be able to generate a Sudoku puzzle using only<br>" +
"the selected techniques, it may loop for ever until you stop it.<br>" +
"<br>" +
"Do you wish to continue anyway?" +
"</body></html>";

	private boolean userConfirmsUnsafe() {
		return confirmedUnsafe
			|| (confirmedUnsafe = JOptionPane.YES_OPTION ==
				JOptionPane.showConfirmDialog(this, CONTINUE_QUESTION
						, getTitle(), JOptionPane.YES_NO_OPTION
						, JOptionPane.QUESTION_MESSAGE));
	}
	private boolean confirmedUnsafe = false;

	private void initGUI() {
		// This
		setTitle("Generate a Sudoku");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setResizable(false);

		// Overall layout
		getContentPane().setLayout(new BorderLayout());

		// parameters pane: contains symmetryPanel, difficultyPanel
		JPanel paramPanel = new JPanel();
		paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
		getContentPane().add(paramPanel, BorderLayout.CENTER);

		// Command pane
		JPanel commandPanel = new JPanel(new GridLayout(1, 2)); //1 row, 2 cols
		getContentPane().add(commandPanel, BorderLayout.SOUTH);
		JPanel pnlGenerate = new JPanel(new FlowLayout(FlowLayout.CENTER));
		commandPanel.add(pnlGenerate);
		JPanel pnlClose = new JPanel(new FlowLayout(FlowLayout.CENTER));
		commandPanel.add(pnlClose);

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
		btnGenerate.setToolTipText("Generate a new Sudoku");
		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (generatorThread == null) {
					// validate everything and start a new generatorThread
					if ( selectedSymmetries.isEmpty() ) {
						carp("Generate", "Please select at least one symmetry");
						return;
					}
					if ( !checkRuleSetIsSafe() && !userConfirmsUnsafe() )
						return;
					startNewGeneratorThread();
				} else {
					stopGeneratorThread();
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

		JButton btnClose = new JButton();
		btnClose.setText("Close");
		btnClose.setMnemonic(KeyEvent.VK_C);
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		pnlClose.add(btnClose);

		// Parameters - Symmetry pane
		JPanel symmetryPanel = new JPanel();
		symmetryPanel.setBorder(new TitledBorder("Allowed symmetry types"));
		paramPanel.add(symmetryPanel);
		symmetryPanel.setLayout(new GridLayout(5, 2)); //was 3, 4
		int i = 0;
		for ( final Symmetry symmetry : Symmetry.values() ) {
			final JCheckBox chkSymmetry = chkSymmetries[i++] = new JCheckBox();
			chkSymmetry.setSelected(selectedSymmetries.contains(symmetry));
			chkSymmetry.setText(symmetry.toString());
			chkSymmetry.setToolTipText(symmetry.getDescription());
			chkSymmetry.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (chkSymmetry.isSelected()) {
						selectedSymmetries.add(symmetry);
					} else {
						selectedSymmetries.remove(symmetry);
					}
				}
			});
			symmetryPanel.add(chkSymmetry);
		}

		// Parameters - Difficulty
		JPanel difficultyPanel = new JPanel(new BorderLayout());
		difficultyPanel.setBorder(new TitledBorder("Difficulty"));
		paramPanel.add(difficultyPanel);

		JPanel diffChooserPanel = new JPanel();
		diffChooserPanel.setLayout(new BoxLayout(diffChooserPanel
				, BoxLayout.X_AXIS));
		difficultyPanel.add(diffChooserPanel, BorderLayout.NORTH);

		final JComboBox<Difficulty> cboDiff = new JComboBox<>();
		for ( Difficulty d : Difficulty.values() )
			cboDiff.addItem(d);
		cboDiff.setToolTipText(
				"Choose the difficulty of the Sudoku to generate");
		cboDiff.setSelectedItem(Difficulty.Fiendish);
		cboDiff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				difficulty = (Difficulty)cboDiff.getSelectedItem();
				lblDifficulty.setText(difficulty.html());
				setSelectedSymmetries(difficulty);
			}
		});
		diffChooserPanel.add(cboDiff);

		final JRadioButton rdoExactDifficulty = new JRadioButton("Exact");
		rdoExactDifficulty.setToolTipText(
				"Generate a Sudoku with exactly the chosen difficulty");
		rdoExactDifficulty.setMnemonic(KeyEvent.VK_E);
		rdoExactDifficulty.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (rdoExactDifficulty.isSelected())
					isExact = true;
			}
		});
		diffChooserPanel.add(rdoExactDifficulty);

		final JRadioButton rdoMaximumDifficulty = new JRadioButton("Maximum");
		rdoMaximumDifficulty.setToolTipText(
				"Generate a Sudoku with at most the chosen difficulty");
		rdoMaximumDifficulty.setMnemonic(KeyEvent.VK_M);
		rdoMaximumDifficulty.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (rdoMaximumDifficulty.isSelected())
					isExact = false;
			}
		});
		diffChooserPanel.add(rdoMaximumDifficulty);
		ButtonGroup group = new ButtonGroup();
		group.add(rdoExactDifficulty);
		group.add(rdoMaximumDifficulty);
		rdoExactDifficulty.setSelected(true);

		JPanel pnlDifficulty = new JPanel(new BorderLayout());
		pnlDifficulty.setBorder(new TitledBorder("Description"));
		difficultyPanel.add(pnlDifficulty, BorderLayout.CENTER);
		lblDifficulty = new JLabel();
		lblDifficulty.setText(
				"<html><body><b>This</b><br>is just<br>a text.</body></html>");
		lblDifficulty.setToolTipText("Explanations of the chosen difficulty");
		pnlDifficulty.add(lblDifficulty, BorderLayout.NORTH);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				lblDifficulty.setText(difficulty.html());
			}
		});

		// Parameters - Warning label
		JPanel warningPanel = new JPanel();
		warningPanel.setBorder(new TitledBorder("Warning"));
		paramPanel.add(warningPanel);
		JLabel lblWarning = new JLabel();
		warningPanel.add(lblWarning);
		lblWarning.setText(
  "<html><body>It can take for ever to generate a replacement IDKFA.<br>"
+ "The easier the faster, but EGBDF. Gopher Cleetus?</body></html>"
		);

		// Parameters - options
		JPanel optionPanel = new JPanel();
		optionPanel.setBorder(new TitledBorder(""));
		optionPanel.setLayout(new GridLayout(1, 1));
		paramPanel.add(optionPanel, BorderLayout.NORTH);
		chkAnalysis = new JCheckBox(
				"Show the analysis of the generated Sudoku");
		chkAnalysis.setToolTipText(
		"Show the difficulty rating of this Sudoku and the simplest"
		+ " techniques that are necessary to solve it.");
		chkAnalysis.setMnemonic(KeyEvent.VK_A);
		chkAnalysis.setSelected(true);
		chkAnalysis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshSudokuPanel();
			}
		});
		optionPanel.add(chkAnalysis);
	}

	private void setSelectedSymmetries(Difficulty diff) {
		switch(diff) {
		case Easy:
		case Medium:
		case Hard:
		case Fiendish:
			selectedSymmetries.addAll(Arrays.asList(Symmetry.ALL));
			break;
		case Nightmare:
			selectedSymmetries.addAll(Arrays.asList(Symmetry.ALL));
			selectedSymmetries.remove(Symmetry.Full_8); // basically impossible
			break;
		case Diabolical:
			selectedSymmetries.clear();
			selectedSymmetries.addAll(Arrays.asList(Symmetry.SMALLS)); // basically impossible with 4 or 8
			break;
		case IDKFA:
			selectedSymmetries.clear();
			selectedSymmetries.add(Symmetry.None); // basically impossible with anything but none
		}
		// refresh the selected symetries check boxes
		for ( int i=0,n=chkSymmetries.length; i<n; ++i )
			chkSymmetries[i].setSelected(selectedSymmetries.contains(Symmetry.ALL[i]));
		repaint();
	}

	void generate() {
		btnGenerate.doClick();
	}

	private void startNewGeneratorThread() {
		// Generate grid
		Symmetry[] syms = selectedSymmetries.toArray(
				new Symmetry[selectedSymmetries.size()] );
		generatorThread = new GeneratorThread(syms, difficulty, isExact);
		generatorThread.start();
	}

	private void stopGeneratorThread() {
		if ( generatorThread!=null && generatorThread.isAlive() ) {
			generatorThread.interrupt();
			try {
				generatorThread.join();
			} catch (InterruptedException ex) {
				StdErr.whinge(ex);
			}
			refreshSudokuPanel();
		}
		generatorThread = null;
	}

	private void carp(String title, String message) {
		JOptionPane.showMessageDialog(this, message, title
				, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Thread that generates a new grid.
	 */
	private final class GeneratorThread extends Thread {
		private final Symmetry[] syms;
		private final Difficulty diff;
		private final boolean isExact;
		private Generator generator;
	    public GeneratorThread(Symmetry[] syms, Difficulty diff
				, boolean isExact) {
			super("GeneratorThread");
			this.syms = syms;
			this.diff = diff;
			this.isExact = isExact;
		}
		@Override
		public void interrupt() {
			generator.interrupt();
		}
		@Override
		public void run() {
			// first update the GUI on the Swing thread
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					engine.setGrid(new Grid());
					AutoBusy.setBusy(GenerateDialog.this, true);
					AutoBusy.setBusy(btnGenerate, false);
					btnGenerate.setText("Stop");
					btnGenerate.repaint();
				}
			});
			// then generate a new puzzle (ie fetch it from the cache)
			generator = new Generator();
			final Grid solution = generator.generate(syms, diff, isExact);
			// then display the puzzle in the GUI on the Swing thread
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if ( solution != null ) {
//						try {
							sudokuList.add(solution);
							sudokuIndex = sudokuList.size() - 1;

//							IO.save(solution.toString(), IO.GENERATED_FILE);
//							solution.source = new PuzzleID(IO.GENERATED_FILE);
//							engine.clearUndos();
							engine.saveFile(IO.GENERATED_FILE); // also adds to recentFiles, and catches IOException in a standard way

							frame.setTitle(IO.GENERATED_FILE.getAbsolutePath());
							refreshSudokuPanel();
//						} catch (IOException ex) {
//							final String msg = "Save generated error";
//							StdErr.whinge(msg, ex);
//							carp(msg, ex.toString());
//						}
					}
					if ( GenerateDialog.this.isVisible() ) {
						AutoBusy.setBusy(GenerateDialog.this, false);
						AutoBusy.setBusy(btnGenerate, false);
						btnGenerate.setText("Generate");
						btnGenerate.repaint();
					}
				}
			});
			// and clear the generate dialog's reference to this thread
			GenerateDialog.this.generatorThread = null;
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
		Grid sudoku = sudokuList.get(sudokuIndex);
		engine.setGrid(sudoku);
		// Generate buggers-up krakens
		btnPrev.setEnabled(sudokuIndex > 0);
		btnNext.setEnabled(sudokuIndex < sudokuList.size()-1);
		if ( chkAnalysis.isSelected() ) {
			// Display analysis of the Sudoku
			AHint analysis = analysisses.get(sudoku);
			if (analysis == null) {
				analysis = engine.analyse();
				analysisses.put(sudoku, analysis);
			}
			engine.showHint(analysis);
		}
	}

	@Override
	public void dispose() {
		close();
	}

	private void close() {
		stopGeneratorThread();
		super.setVisible(false);
		super.dispose();
	}

}
