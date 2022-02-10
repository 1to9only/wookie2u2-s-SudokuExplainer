/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Tech;
import static diuf.sudoku.Tech.BIG_WING_TECHS;
import static diuf.sudoku.Settings.THE_SETTINGS;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.Log;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

/**
 * A JDialog for the user to de/select which Sudoku solving Tech(niques) are
 * used to find hints in the GUI.
 * <p>
 * De/selecting a Tech here sets LogicalSolver's wantedTechs which is used to
 * build wantedHinters, which effects LogicalSolverTester which developers use
 * to metricate the performance of the latest hinter. We value what we measure.
 */
class TechSelectDialog extends JDialog {

	private static final long serialVersionUID = -7071292711961723801L;

	private final SudokuExplainer engine;
	private final SudokuFrame parentFrame;

	private JPanel contentPanel = null;
	private JPanel northPanel = null;
	private JPanel centerPanel = null;
	private JPanel southPanel = null;
	private JPanel okButtonPanel = null;
	private JButton btnOk = null;
	private JPanel cancelButtonPanel = null;
	private JButton btnCancel = null;
	private JLabel lblExplanations = null;

	// a private copy of THE_SETTINGS.wantedTechs, which I mutate, then on OK I
	// set the whole THE_SETTINGS.wantedTechs, and save to file.
	private EnumSet<Tech> wantedTechs;

	/** Constructor. */
	TechSelectDialog(SudokuFrame parent, SudokuExplainer engine) {
		super(parent, "Solving Techniques Selection", true);
		this.engine = engine;
		this.parentFrame = parent;
		initialise();
		fillTechniques();
	}

	private void initialise() {
		this.setResizable(false);
		this.setContentPane(getContentPanel());
	}

	// ================================ CENTER ================================

	/** Number of Tech's (check-boxes) per line on this form. */
	private static final int NUM_COLS = 6;

	/**
	 * Populate the TechSelectDialog form with a CheckBox for each Tech.
	 */
	private void fillTechniques() {
		wantedTechs = THE_SETTINGS.getWantedTechs();
		int count = 0;
		for ( final Tech tech : Tech.allHinters() ) { // no validators
			boolean enable = true;
			final JCheckBox checkBox = new JCheckBox();
			checkBox.setText(tech.text());
			checkBox.setToolTipText(tech.tip);
			// "core" techs are always wanted, so the user can't unwant them.
			// NB: You can't unwant the validators either.
			switch (tech) {
			case NakedSingle:
			case HiddenSingle:
				checkBox.setSelected(true);
				checkBox.setEnabled(false);
				enable = false;
			}
			// if we're not already on a new line then
			if ( count%NUM_COLS != 0 ) {
				// each of these techs is the first on a line, to organise
				// techs into (roughly) a line per Difficulty.
				// first tech in each Difficulty on a new line "automatically".
				if ( Difficulty.isaFloor(tech) ) {
					// add an invisible JLabels to put tech on a new line
					while ( count%NUM_COLS != 0 ) {
						centerPanel.add(new JLabel());
						++count;
					}
				} else {
					// extra individual line breaks per Tech "manually".
					switch (tech) {
					// Difficulty no fit on line, so these just look nicer
					case Coloring: case DeathBlossom:
				    // Frankens and Krakens are oddly interleaved for speed
					case FrankenSwampfish: case KrakenSwampfish:
					   // hacked checkbox goes to the right of each of big A*E
					case AlignedPair: case AlignedPent: case AlignedHex:
					case AlignedSept: case AlignedOct: case AlignedNona:
					case AlignedDec:
						// add an invisible JLabels to put tech on a new line
						while ( count%NUM_COLS != 0 ) {
							centerPanel.add(new JLabel());
							++count;
						}
					}
				}
			}
			// if this tech is to be enabled
			if ( enable ) {
				// set the CheckBoxs selected state
				checkBox.setSelected(wantedTechs.contains(tech));
				// add an action listener to the checkbox
				checkBox.addActionListener((ActionEvent e) -> {
					if ( checkBox.isSelected() ) {
						// carp if toolTip is set and warning is true
						if ( tech.warning && tech.tip!=null ) {
							showMessageDialog(TechSelectDialog.this
									, tech.name()+SP+tech.tip, "WARNING"
									, WARNING_MESSAGE);
						}
						// unselect any mutually exclusive techs
						switch ( tech ) {
							// Locking XOR LockingBasic
							case Locking: unselect(Tech.LockingBasic); break;
							case LockingBasic: unselect(Tech.Locking); break;
							// BigWings XOR individual BigWing
							case BigWings: unselect(BIG_WING_TECHS); break;
							case WXYZ_Wing: case VWXYZ_Wing: case UVWXYZ_Wing:
							case TUVWXYZ_Wing: case STUVWXYZ_Wing:
								unselect(Tech.BigWings); break;
							// Medusa3D is counter-productive with GEM.
							case GEM: unselect(Tech.Medusa3D);
						}
						// Locking xor LockingBasic
						// BigWings is prefered to individual BigWing's.
						// then selecting any BigWing unselects BigWings.
						// Medusa3d is counter-productive with GEM
						wantedTechs.add(tech);
					} else
						wantedTechs.remove(tech);
				});
			}
			// add the checkBox to it's panel
			centerPanel.add(checkBox);
			++count;
			// hacked checkbox goes to the right of each of big A*E
			switch ( tech ) {
			case AlignedPent: case AlignedHex: case AlignedSept:
			case AlignedOct: case AlignedNona: case AlignedDec:
				centerPanel.add(newHackBox(tech.degree));
				++count;
			}
		}
	}

	// Unselect the JCheckBox and unwant this Tech
	// return was it found and unselected
	private boolean unselect(Tech t) {
		final String text = t.text();
		JCheckBox chk;
		for ( Component c : centerPanel.getComponents() ) {
			if ( c instanceof JCheckBox
			  && text.equals((chk=(JCheckBox)c).getText()) ) {
				// chk.actionPerformed may not exist yet
				chk.setSelected(false);
				// so also "manually" remove the given tech
				wantedTechs.remove(t);
				repaint();
				return true;
			}
		}
		return false;
	}

	// Unselect all of the given techs
	private void unselect(Tech[] techs) {
		for ( Tech tech : techs )
			unselect(tech);
	}

	// create a new hacked JCheckBox
	private static JCheckBox newHackBox(final int degree) {
		final String settingName = "isa"+degree+"ehacked";
		final JCheckBox checkBox = new JCheckBox("hacked");
		checkBox.setSelected(THE_SETTINGS.getBoolean(settingName));
		checkBox.addActionListener((ActionEvent e) ->
			THE_SETTINGS.setBoolean(settingName, checkBox.isSelected())
		);
		return checkBox;
	}

	private JPanel getCenterPanel() {
		if (centerPanel == null) {
			TitledBorder titledBorder = BorderFactory.createTitledBorder(
				  null
				, "Available solving techniques"
				, TitledBorder.CENTER
				, TitledBorder.DEFAULT_POSITION
				, new Font("Dialog", Font.BOLD, 12)
				, new Color(51, 51, 51)
			);
			titledBorder.setBorder(null);
			titledBorder.setTitle("");
			centerPanel = new JPanel();
			centerPanel.setLayout(new GridLayout(0, NUM_COLS)); // rows, cols
			centerPanel.setBorder(titledBorder);
		}
		return centerPanel;
	}

	// ================================ NORTH ================================

	private JPanel getNorthPanel() {
		if (northPanel == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(FlowLayout.LEFT);
			lblExplanations = new JLabel();
			lblExplanations.setText("Select the solving techniques to use:");
			lblExplanations.setToolTipText("ticked Solving techniques are used"
			+" to solve and analyze Sudokus, unticked are not.");
			northPanel = new JPanel();
			northPanel.setLayout(flowLayout);
			northPanel.add(lblExplanations, null);
		}
		return northPanel;
	}

	// ================================ SOUTH ================================

	private JPanel getSouthPanel() {
		if (southPanel == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setColumns(3);
			southPanel = new JPanel();
			southPanel.setLayout(gridLayout);
			southPanel.add(getOkButtonPanel(), null);
			southPanel.add(getCancelButtonPanel(), null);
		}
		return southPanel;
	}

	private JPanel getOkButtonPanel() {
		if (okButtonPanel == null) {
			okButtonPanel = new JPanel();
			okButtonPanel.setLayout(new FlowLayout());
			okButtonPanel.add(getBtnOk(), null);
		}
		return okButtonPanel;
	}

	private JButton getBtnOk() {
		if (btnOk == null) {
			btnOk = new JButton();
			btnOk.setText("OK");
			btnOk.setMnemonic(KeyEvent.VK_O);
			btnOk.addActionListener((java.awt.event.ActionEvent e) -> {
				commit();
			});
		}
		return btnOk;
	}

	private void commit() {
		// I want to require DynamicPlus OR NestedUnary, but still allow the
		// user to override that requirement. If you override then you risk not
		// finding solutions to puzzles that SE can solve. Just be patient!
		// DynamicPlus misses on expert puzzles, but NestedUnary ALWAYS hints,
		// as do all thereafter.
		if ( !anyWanted(Tech.DynamicPlus, Tech.NestedUnary, Tech.NestedMultiple
			, Tech.NestedDynamic, Tech.NestedPlus) && !confirmNoSafetyNet() ) {
			return;
		}
		// Locking XOR LockingBasic, never neither, never both.
		if ( wantedTechs.contains(Tech.Locking) ) {
			wantedTechs.remove(Tech.LockingBasic);
		} else {
			wantedTechs.add(Tech.LockingBasic);
		}
		// BigWings (slower, but faster overall) XOR individual BigWing
		if ( wantedTechs.contains(Tech.BigWings) ) {
			for ( Tech t : BIG_WING_TECHS ) {
				wantedTechs.remove(t);
			}
		} else if ( anyWanted(BIG_WING_TECHS) ) {
			wantedTechs.remove(Tech.BigWings);
		}
		THE_SETTINGS.justSetWantedTechs(wantedTechs);
		THE_SETTINGS.save();
		setVisible(false);
		engine.clearHints();
		Log.teeln("\nTechSelectDialog updating wanted hinters...");
		engine.recreateLogicalSolver();
		parentFrame.refreshDisabledRulesWarning();
		dispose();
	}

	// does wantedTechs.containsAny(techs)?
	private boolean anyWanted(Tech... techs) {
		for ( Tech t : techs )
			if ( wantedTechs.contains(t) )
				return true;
		return false;
	}

	private static final String NO_SAFETY_NET_QUESTION =
"Select \"Dynamic Plus\" as a safety-net for \"normal\" Sudoku puzzles;"+System.lineSeparator()
+"and/or \"Nested Unary\" as a safety-net for ALL Sudoku puzzles,"+System.lineSeparator()
+"or alternately Nested Multiple/Dynamic/Plus. If you don't select"+System.lineSeparator()
+"a catch-all you risk not getting solutions to Sudoku puzzles that"+System.lineSeparator()
+"Sudoku Explainer can actually solve. Just be patient!"+System.lineSeparator()
+"Do you wish to ignore this most excellent advice and continue anyway?";

	private boolean confirmNoSafetyNet() {
		return parentFrame.ask(NO_SAFETY_NET_QUESTION, "Confirm no safety-net");
	}

	private JPanel getCancelButtonPanel() {
		if (cancelButtonPanel == null) {
			cancelButtonPanel = new JPanel();
			cancelButtonPanel.setLayout(new FlowLayout());
			cancelButtonPanel.add(getBtnCancel(), null);
		}
		return cancelButtonPanel;
	}

	private JButton getBtnCancel() {
		if (btnCancel == null) {
			btnCancel = new JButton();
			btnCancel.setText("Cancel");
			btnCancel.setMnemonic(KeyEvent.VK_C);
			btnCancel.addActionListener((java.awt.event.ActionEvent e) -> {
				TechSelectDialog.this.setVisible(false);
				TechSelectDialog.this.dispose();
			});
		}
		return btnCancel;
	}

	// =============================== CONTENT ================================

	private JPanel getContentPanel() {
		if (contentPanel == null) {
			contentPanel = new JPanel();
			contentPanel.setLayout(new BorderLayout());
			contentPanel.add(getNorthPanel(), BorderLayout.NORTH);
			contentPanel.add(getCenterPanel(), BorderLayout.CENTER);
			contentPanel.add(getSouthPanel(), BorderLayout.SOUTH);
		}
		return contentPanel;
	}

}