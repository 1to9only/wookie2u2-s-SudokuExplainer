/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Tech;
import static diuf.sudoku.Settings.THE_SETTINGS;
import static diuf.sudoku.utils.Frmt.SPACE;
import diuf.sudoku.utils.Log;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

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

	private static final Tech[] BIG_WING = {Tech.WXYZ_Wing, Tech.VWXYZ_Wing
			, Tech.UVWXYZ_Wing, Tech.TUVWXYZ_Wing, Tech.STUVWXYZ_Wing};

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

	/** Number of columns: the number of check-boxes across the form. */
	private static final int NUM_COLS = 3;

	/**
	 * Populate the TechSelectDialog form with a CheckBox for each Tech.
	 */
	private void fillTechniques() {
		wantedTechs = THE_SETTINGS.getWantedTechs();
		int count = 0;
		for ( final Tech tech : Tech.allHinters() ) { // no validators

			boolean enable = true;
			final JCheckBox chk = new JCheckBox();
			chk.setText(tech.text());
			chk.setToolTipText(tech.tip);

			// "core" techs are always wanted, so the user can't unwant them.
			switch (tech) {
			case NakedSingle:
			case HiddenSingle:
				chk.setSelected(true);
				chk.setEnabled(false);
				enable = false;
				break;
			}

			// These techs appear on a new-line to organise techs logically
			// on the screen. The logic is insanity to the uninitiated, but
			// it isn't. Wait until you don't want to fix it (much) before
			// you fix it (much); then you won't much want to (much) fix it
			// (much), so you probably (not too much) won't (much) fix it
			// (much). It's all a much of a muchness. Just don't go leaping
			// straight for the clitoris, OK! Dems da rulez Macca!
			// if we're not already on a new line
			if ( count%NUM_COLS != 0 ) {
				switch (tech) {
				// the first tech in each "block" of techs.
				case DirectNakedPair:
				case DirectNakedTriple:
				case NakedPair:
				case NakedTriple:
				case TwoStringKite:
				case Skyscraper:
				case XY_Wing:
				case Swampfish:
				case Swordfish:
				case Jellyfish:
				case BigWings:
				case FinnedSwampfish:
				case FrankenSwampfish:
				case KrakenSwampfish:
				case KrakenSwordfish:
				case KrakenJellyfish:
				case BUG:
				case ALS_XZ:
				case NakedQuad:
				case NakedPent:
				case URT:
				case UnaryChain:
				case NestedUnary:
				// hacked checkbox appears to my right
				case AlignedPair:
				case AlignedPent:
				case AlignedHex:
				case AlignedSept:
				case AlignedOct:
				case AlignedNona:
				case AlignedDec:
					// add empty JLabels until we're on the next line
					while ( count%NUM_COLS != 0 ) {
						centerPanel.add(new JLabel());
						++count;
					}
					break;
				}
			}

			if ( enable ) {
				// select and add an action listener to the checkbox
				chk.setSelected(wantedTechs.contains(tech));
				chk.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if ( chk.isSelected() ) {
							// carp if toolTip is set and warning is true
							if ( tech.warning && tech.tip!=null ) {
								JOptionPane.showMessageDialog(
								  TechSelectDialog.this
								, tech.name()+SPACE+tech.tip
								, "WARNING"
								, JOptionPane.WARNING_MESSAGE
								);
							}
							switch ( tech ) {
							// Locking xor LockingGeneralised
							case Locking:
								unselect(Tech.LockingGeneralised);
								break;
							case LockingGeneralised:
								unselect(Tech.Locking);
								break;
							// BigWings is prefered to individual BigWing's.
							case BigWings:
								for ( Tech bw : BIG_WING )
									unselect(bw);
								break;
							// then selecting any BigWing unselects BigWings.
							case WXYZ_Wing:
							case VWXYZ_Wing:
							case UVWXYZ_Wing:
							case TUVWXYZ_Wing:
							case STUVWXYZ_Wing:
								unselect(Tech.BigWings);
								break;
							// Medusa3d is counter-productive with GEM
							case GEM:
								unselect(Tech.Medusa3D);
								break;
							}
							wantedTechs.add(tech);
						} else
							wantedTechs.remove(tech);
					}
				});
			}
			centerPanel.add(chk);
			++count;

			// hacked checkbox to the right of each Large Aligned*Exclusion
			switch ( tech ) {
			case AlignedPent:
			case AlignedHex:
			case AlignedSept:
			case AlignedOct:
			case AlignedNona:
			case AlignedDec:
				centerPanel.add(newHackBox("isa"+tech.degree+"ehacked"));
				++count;
				break;
			}
		}
	}

//	// is target in techs?
//	private static boolean in(Tech target, Tech[] techs) {
//		for ( Tech t : techs )
//			if ( t == target )
//				return true;
//		return false;
//	}

	// Unselect the JCheckBox for this Tech
	// return was it found and unselected
	private boolean unselect(Tech t) {
		final String text = t.text();
		JCheckBox chk;
		for ( Component c : centerPanel.getComponents() ) {
			if ( c instanceof JCheckBox
			  && text.equals((chk=(JCheckBox)c).getText()) ) {
				chk.setSelected(false);
				repaint();
				return true;
			}
		}
		return false;
	}

	// create a new hacked JCheckBox
	private static JCheckBox newHackBox(final String settingName) {
		final JCheckBox box = new JCheckBox("hacked");
		box.setSelected(THE_SETTINGS.get(settingName));
		box.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				THE_SETTINGS.set(settingName, box.isSelected());
			}
		});
		return box;
	}

	private JPanel getCenterPanel() {
		if (centerPanel == null) {
			TitledBorder titledBorder = BorderFactory.createTitledBorder(
					  null
					, "Available solving techniques"
					, TitledBorder.CENTER
					, TitledBorder.DEFAULT_POSITION
					, new Font("Dialog", Font.BOLD, 12)
					, new Color(51, 51, 51));
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
			btnOk.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					commit();
				}
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
				, Tech.NestedDynamic, Tech.NestedPlus)
		  && !confirmNoSafetyNet() )
			return;
		// Coloring xor BUG (you can have neither, but not both).
		// Coloring finds a superset of BUG hints, and is faster.
		// I should probably remove BUG altogether, for simplicity.
		if ( wantedTechs.contains(Tech.Coloring) )
			wantedTechs.remove(Tech.BUG);
		// BigWings (slower, but faster overall) xor individual BigWing
		if ( wantedTechs.contains(Tech.BigWings) )
			for ( Tech t : BIG_WING )
				wantedTechs.remove(t);
		else if ( anyWanted(BIG_WING) )
			wantedTechs.remove(Tech.BigWings);
		THE_SETTINGS.justSetWantedTechs(wantedTechs);
		THE_SETTINGS.save();
		setVisible(false);
		engine.clearHints();
		Log.teeln("\nTechSelectDialog updated wanted hinters...");
		engine.recreateLogicalSolver();
		engine.solver.printWantedEnabledHinters(System.out);
		engine.solver.printWantedEnabledHinters(Log.log);
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
			btnCancel.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					TechSelectDialog.this.setVisible(false);
					TechSelectDialog.this.dispose();
				}
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