/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.gen.Generator;
import diuf.sudoku.gen.PuzzleCache;
import diuf.sudoku.gen.Symmetry;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import java.awt.BorderLayout;
import java.awt.Dimension;
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

	// load the PuzzleCache when the GenerateDialog form is first loaded, to
	// give him time to load/generate a cache of puzzles BEFORE you press the
	// Generate button. This only matters the first time a machine loads the
	// generate form, and most users will poke-around-a-bit before pressing
	// generate for the first time, so I'm relying on that time to pre-cache,
	// which'll probably work for everything except IDKFA which can take upto
	// like 10 minutes to generate. Let's hope they don't go big first.
	static {
		PuzzleCache.staticInitialiser();
	}

	// Techs we want the user to want. These are really for speed, not actually
	// required for safety.
	private static final Tech[] SAFE_TECHS = new Tech[] {
			  Tech.NakedSingle
			, Tech.HiddenSingle
			, Tech.Locking
			, Tech.NakedPair
			, Tech.HiddenPair
			, Tech.Swampfish // aka X-Wing, but I prefer a fish related name.
	};

	// One of these techs is required as a safety net. DynamicPlus only misses
	// on the very hardest puzzles, but all the Nested hinters ALWAYS hint.
	private static final Tech[] SAFETY_NETS = new Tech[] {
			  Tech.DynamicPlus
			, Tech.NestedUnary
			, Tech.NestedMultiple
			, Tech.NestedDynamic
			, Tech.NestedPlus
	};

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

	private Difficulty difficulty = Difficulty.Fiendish;
	private boolean isExact = true;

	private volatile GeneratorThread generatorThread = null;
	private final List<Grid> sudokuList = new ArrayList<>();
	private int sudokuIndex = 0;
	private final Map<Grid, AHint> analysisMap = new HashMap<>();
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
		selectedSyms.addAll(Arrays.asList(Symmetry.ALL));
		sudokuList.add(engine.getGrid());
	}

	private boolean checkSafeTechs() {
		return THE_SETTINGS.allWanted(SAFE_TECHS)
			&& THE_SETTINGS.anyWanted(SAFETY_NETS);
	}

	private static final String UNSAFE_QUESTION =
"<html><body>" +
"<b>Warning:</b> not all the \"safe\" solving techniques are enabled.<br>" +
"Sudoku Explainer may not be able to generate a Sudoku puzzle using only<br>" +
"the selected techniques, the generator may just run for ever.<br>" +
"<br>" +
"<u>Safe Techs</u> Select ALL of: " + Tech.names(SAFE_TECHS) +"<br>" +
"These are for speed really, not safety. Tick Tock!<br>" +
"<br>" +
"<u>Safety Nets</u> Select ONE of: " + Tech.names(SAFETY_NETS) + "<br>" +
"ONE of these is needed for safety, else puzzles may not solve.<br>" +
"DynamicPlus covers normal puzzles; Nested all cover ALL puzzles.<br>" +
"<br>" +
"Do you wish to continue anyway?" +
"</body></html>";

	private boolean userOverridesUnsafe() {
		return overriddenUnsafe
			|| (overriddenUnsafe = JOptionPane.YES_OPTION ==
				JOptionPane.showConfirmDialog(this, UNSAFE_QUESTION
						, getTitle(), JOptionPane.YES_NO_OPTION
						, JOptionPane.QUESTION_MESSAGE));
	}
	private boolean overriddenUnsafe = false;

	private static final String NL = System.lineSeparator();
	private static final String BAD_TECHS_MESSAGE =
		"No Sudoku solving techniques are wanted that are in the"+NL
		+"selected difficulty, so generate would just run for ever,"+NL
		+"without replacing the cached Sudoku puzzle."+NL
		+NL
		+"Menu: Options ~ Solving techniques... to choose more techs,"+NL
		+"or just choose another difficulty to generate.";

	private void initGUI() {
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
				if ( generatorThread == null ) {
					// a symmetry is required
					if ( selectedSyms.isEmpty() ) {
						carp("Generate", "Please select at least one symmetry");
						return;
					}
					// check the "safe" (faster) techs are selected
					if ( !checkSafeTechs() && !userOverridesUnsafe() )
						return;
					// check selected techs can produce this difficulty
					if ( !THE_SETTINGS.anyWanted(difficulty.techs()) ) {
						carp("Generate", BAD_TECHS_MESSAGE);
						return;
					}
					// start a new generatorThread
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
				lblDescription.setText(difficulty.html);
				setSelectedSymmetries(difficulty);
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				lblDescription.setText(difficulty.html);
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

		// set the minimum size of the dialog, otherwise the dialog isn't tall
		// enough to fit the maximum height of lblDesciption (which shrinks and
		// grows as the description changes) so it and controls below it are
		// pushed below the bottom of the dialog, which sux, especially when
		// the user can't resize the dialog to fix it.
		setMinimumSize(new Dimension(440, 440));
		setResizable(false); // prevent user accidentally disappearing controls
		pack();
	}

	private void setSelectedSymmetries(Difficulty diff) {
		switch(diff) {
		case Easy:
		case Medium:
		case Hard:
		case Fiendish:
			selectedSyms.addAll(Arrays.asList(Symmetry.ALL));
			break;
		case Nightmare:
			selectedSyms.addAll(Arrays.asList(Symmetry.ALL));
			selectedSyms.remove(Symmetry.Full_8); // basically impossible
			break;
		case Diabolical:
			selectedSyms.clear();
			selectedSyms.addAll(Arrays.asList(Symmetry.SMALLS)); // basically impossible with 4 or 8
			break;
		case IDKFA:
			selectedSyms.clear();
			selectedSyms.add(Symmetry.None); // basically impossible with anything but none
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
		generatorThread = new GeneratorThread(
				  selectedSyms.toArray(new Symmetry[selectedSyms.size()])
				, difficulty
				, isExact
		);
		generatorThread.start();
	}

	private void stopGeneratorThread() {
		if ( generatorThread!=null && generatorThread.isAlive() ) {
			generatorThread.interrupt();
			try {
				generatorThread.join(500); // wait half a second
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
	    public GeneratorThread(Symmetry[] syms, Difficulty diff, boolean isExact) {
			super("GeneratorThread");
			this.syms = syms;
			this.diff = diff;
			this.isExact = isExact;
		}
		@Override
		public void interrupt() {
			if ( generator != null )
				generator.interrupt();
		}
		@Override
		public void run() {
			// first update the GUI on the Swing thread
			// BUG: only works the first time, and I don't understand why.
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					engine.setGrid(new Grid()); // clear the grid
					AutoBusy.setBusy(GenerateDialog.this, true);
					AutoBusy.setBusy(btnGenerate, false);
					btnGenerate.setText("Stop");
					repaint();
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
						sudokuList.add(solution);
						sudokuIndex = sudokuList.size() - 1;
						refreshSudokuPanel(); // sets engine.grid
						engine.saveFile(IO.GENERATED_FILE); // also adds to recentFiles, and catches IOException in a standard way
						frame.setTitle(IO.GENERATED_FILE.getAbsolutePath());
					}
					if ( GenerateDialog.this.isVisible() ) {
						AutoBusy.setBusy(GenerateDialog.this, false);
						AutoBusy.setBusy(btnGenerate, false);
						btnGenerate.setText("Generate");
						repaint();
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
		Grid grid = sudokuList.get(sudokuIndex);
		engine.setGrid(grid);
		// Generate buggers-up krakens
		btnPrev.setEnabled(sudokuIndex > 0);
		btnNext.setEnabled(sudokuIndex < sudokuList.size()-1);
		if ( chkAnalysis.isSelected() ) {
			// Display the analysis of this Sudoku
			AHint analysis = analysisMap.get(grid);
			if ( analysis == null ) {
				analysis = engine.analysePuzzle(false, false); // NEVER noisy in generate!
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
		stopGeneratorThread();
		super.setVisible(false);
		super.dispose();
	}

}
