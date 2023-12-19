/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Tech;
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
import static diuf.sudoku.utils.Frmt.NL;
import java.util.HashMap;
import java.util.Map;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Constants.beep;
import static diuf.sudoku.gui.Event.CTRL_MASK;
import static diuf.sudoku.gui.SudokuGridPanel.COLOR_AQUA;
import java.awt.Rectangle;
import java.util.LinkedList;

/**
 * A JDialog for the user to de/select which Sudoku solving Tech(niques) are
 * used to find hints in the GUI.
 * <p>
 * De/selecting a Tech here sets LogicalSolvers wantedTechs which is used to
 * build wantedHinters, which effects LogicalSolverTester which developers use
 * to metricate the performance of the latest hinter. We value what we measure.
 */
class TechSelectDialog extends JDialog {

	private static final long serialVersionUID = -7071292711961723801L;

	/** Number of Techs (check-boxes) per line on this form. */
	private static final int NUM_COLS = 7;

	// Fisch: when a fish is selected, auto-unselect all other fish.
	// Finned/Franken/Mutant * Swamp/Sword/Finned = 9 techs
	private final EnumSet<Tech> fishyTechs = Tech.where((t)->t.isFishy && !t.name().startsWith("Kraken"));

	// Krakens are separate
	private final EnumSet<Tech> krakenTechs = Tech.where((t)->t.name().startsWith("Kraken"));

	private final SudokuExplainer engine;
	private final SudokuFrame frame;

	private JPanel contentPanel = null;
	private JPanel northPanel = null;
	private JPanel centerPanel = null;
	private JPanel southPanel = null;
	private JPanel okButtonPanel = null;
	private JButton btnOk = null;
	private JPanel cancelButtonPanel = null;
	private JButton btnCancel = null;

	// a private copy of CFG.wantedTechs, which I mutate, then on OK I
	// set the whole CFG.wantedTechs, and save to file.
	private EnumSet<Tech> myWantedTechs;
	private final Map<Tech, JCheckBox> techBoxes = new HashMap<>(32, 0.75F);

	private final LinkedList<JCheckBox> fishTank = new LinkedList<>();

	/**
	 * Constructor.
	 *
	 * @param frame the SudokuFrame which is my parent in the GUI
	 * @param engine the SudokuExplainer is a business delegate upon which I
	 *  shall execute a method or two, specifically when the user presses OK
	 *  I need to clear any old hints out of the GUI, and recreate the whole
	 *  LogicalSolver, thus recreating all of the hinters. Basically, start
	 *  again from scratch.
	 */
	TechSelectDialog(final SudokuFrame frame, final SudokuExplainer engine) {
		super(frame, "Solving Techniques Selection", true);
		this.engine = engine;
		this.frame = frame;
		initialise();
		fillTechniques();
	}

	private void initialise() {
		this.setResizable(false);
		this.setContentPane(getContentPanel());
	}

	// ================================ CENTER ================================

	/**
	 * Populate the centerPanel with a CheckBox for each Tech.
	 */
	private void fillTechniques() {
		final boolean isColorful = CFG.getBoolean("isTechSelectDialogColorful");
		// getWantedTechs returns a COPY of CFG.wantedTechs, which I mutate,
		// then my save button sets CFG.wantedTechs. Simple. Transactional.
		myWantedTechs = CFG.getWantedTechs();
		int count = 0;
		// foreach hinter tech (wanted or not)
		for ( final Tech tech : Tech.hinters() ) { // no validators
			boolean enable = true;
			final JCheckBox box = new JCheckBox();
			box.setText(tech.text());
			box.setToolTipText(tech.tip);
			box.setActionCommand(tech.name());
			// disable so user cannot unwant these core techs
			switch (tech) {
			case NakedSingle:
			case HiddenSingle:
				box.setSelected(true);
				box.setEnabled(false);
				enable = false;
			}
			if ( fishyTechs.contains(tech) )
				fishTank.add(box);
			if ( tech == Tech.FrankenSwampfish )
				box.setEnabled(false); // FrankenSwampfish do not exist!
			if ( isColorful )
				box.setForeground(techColor(tech));
			// map the Tech to its CheckBox, for unselect.
			techBoxes.put(tech, box);
			// if we are not already on a new line then
			if ( count%NUM_COLS > 0 ) { // NEVER negative
				// techs are organised (roughly) into a line per Difficulty.
				if ( Difficulty.isaFloor(tech) ) {
					// add an invisible JLabels to put tech on a new line
					while ( count%NUM_COLS > 0 ) { // NEVER negative
						centerPanel.add(new JLabel());
						++count;
					}
				} else {
					// put each of these Techs first on a line
					switch (tech) {
					// just to look nice
					case Coloring: //fallthrough
				    // Finned/Franken/Mutant/Kraken are interleaved for speed
					case FinnedSwampfish: //fallthrough
					case FinnedSwordfish: //fallthrough
					case FinnedJellyfish: //fallthrough
					// Krakens go on there own line
					case KrakenSwampfish:
						// add an invisible JLabels to put tech on a new line
						while ( count%NUM_COLS > 0 ) { // NEVER negative
							centerPanel.add(new JLabel());
							++count;
						}
					}
				}
			}
			// if this tech is to be enabled
			if ( enable ) {
				// set the CheckBoxs selected state
				box.setSelected(myWantedTechs.contains(tech));
				// add an action listener to the checkbox
				box.addActionListener((ActionEvent e) -> {
					if ( box.isSelected() ) {
						// carp if toolTip is set and warning is true
						if ( tech.isWarning && tech.tip!=null )
							showMessageDialog(TechSelectDialog.this
									, tech.name()+SP+tech.tip, "WARNING"
									, WARNING_MESSAGE);
						// unselect any mutually exclusive techs
						switch ( tech ) {
							// Locking XOR LockingBasic
							case Locking: unselect(Tech.LockingBasic); break;
							case LockingBasic: unselect(Tech.Locking); break;
							// Medusa3D is counter-productive with GEM.
							case GEM: unselect(Tech.Medusa3D); break;
							// unselect all other fish //fallthrough all
							case FinnedSwampfish: case FinnedSwordfish: case FinnedJellyfish:
							case FrankenSwordfish: case FrankenSwampfish: case FrankenJellyfish:
							case MutantSwampfish:  case MutantSwordfish: case MutantJellyfish:
								// CTRL=multi-select so unselect is reduced to
								// fish above and to the left-of this checkBox,
								// except this checkBox itself.
								if ( (e.getModifiers() & CTRL_MASK) != 0 )
									unselect(aboveLeft(fishTank, box), "weed");
								else // unselect ALL other fishy checkBoxes
									unselect(Tech.where((t)->t!=tech, fishyTechs), "wave");
								break;
							case KrakenSwampfish: case KrakenSwordfish: case KrakenJellyfish:
								unselect(Tech.where((t)->t!=tech, krakenTechs), "wewease");
								break;
						}
						// Locking xor LockingBasic
						// BigWings is prefered to individual BigWings.
						// then selecting any BigWing unselects BigWings.
						// Medusa3d is counter-productive with GEM
						myWantedTechs.add(tech);
					} else {
// No {TableChains, Abduction, or UnaryChain} causes invalid StaticChains
//						// No UnaryChain causes invalid StaticChains
//						if ( tech == Tech.UnaryChain )
//							unselect(Tech.StaticChain);
						myWantedTechs.remove(tech);
					}
				});
			}
			// add the checkBox to it is panel
			centerPanel.add(box);
			++count;
		}
	}

	/**
	 * Returns an {@code EnumSet<Tech>} of the $pond that are above and to the
	 * left-of $targetFish, excluding $targetFish itself. These fishy techs are
	 * both smaller (Tech.degree) and simpler (fishType) than me. Fish get
	 * bigger top-to-bottom, and more complex left-to-right. Krakens have there
	 * own pond, at the bottom; they get bigger left-to-right.
	 * <p>
	 * This relies on checkBox.actionCommand being tech.name(). In fact all
	 * checkBox.actionCommands are set to tech.name(), for me, and whatever.
	 * My result is passed to unselect when the user CTRL-multiselects the
	 * CheckBox of any of the 9 "grouped" complex fisherman.
	 * <p>
	 * Unusually, this logic is based on the position of each control on the
	 * form, which works only because each Fisherman finds all Fish that are
	 * smaller (above me) and simpler (to my left) than me; which are placed
	 * above me and to my left, by design. You could base the same logic on
	 * tech attributes, but that would require a fishType attribute, where as
	 * my cheat uses the existing attribute: position, hence I implemented it
	 * this way.
	 * <p>
	 * This technique is dependant upon the order of fishy Techs in Tech,
	 * especially that Fish increase in complexity left-to-right, and upon each
	 * fish-size appearing on it's own line in the TechSelectDialog. If you
	 * MUST change any of that then create a fishType attribute in Tech, which
	 * must be comparable, hence an int taking its value from
	 * <pre>{@code diuf.sudoku.solver.hinters.fish.FishType.BASIC_FTYPE (IDE bug in link)}</pre>
	 * and the existing {@link Tech#degree}, then rewrite this mess based on "the real
	 * logic". It would have been faster to write it than explain it. Sigh.
	 *
	 * @param pond JCheckBoxes of all isFishy techs, excluding Krakens
	 * @param targetFish bottom right corner of the rectangle from which all
	 *  techs need to be unselected, myself excluded
	 * @return a new {@code EnumSet<Tech> } of pond techs that are above and
	 *  to the left of targetBox, excluding the targetFish, obviously. Sigh.
	 */
	private EnumSet<Tech> aboveLeft(final Iterable<JCheckBox> pond, final JCheckBox targetFish) {
		Rectangle cb;
		final EnumSet<Tech> result = EnumSet.noneOf(Tech.class);
		final Rectangle tb = targetFish.getBounds(); // targetBounds
		for ( JCheckBox fish : pond ) { // candidateBox
			cb = fish.getBounds(); // candidateBounds
			if ( cb.x<=tb.x && cb.y<=tb.y && fish!=targetFish )
				result.add(Tech.valueOf(fish.getActionCommand()));
		}
		return result;
	}

//DEBUG: aboveLeft (above) not sure if he's to me left. Difficult!
//	private String format(final LinkedList<JCheckBox> fishTank, final JCheckBox checkBox) {
//		final StringBuilder sb = SB(64+(fishTank.size()<<5)); // * 32
//		sb.append(checkBox.getActionCommand())
//		.append(" ").append(checkBox.getBounds().x)
//		.append(" ").append(checkBox.getBounds().y);
//		for ( JCheckBox box : fishTank ) {
//			sb.append(", ").append(box.getActionCommand())
//			.append(" ").append(box.getBounds().x)
//			.append(" ").append(box.getBounds().y);
//		}
//		return sb.toString();
//	}

	/**
	 * Get the JCheckBox whose actionCommand==tech.name().
	 *
	 * @param tech to get
	 * @return the JCheckBox that represents the given tech
	 */
	private JCheckBox boxFor(final Tech tech) {
		// techBoxes is populated with every tech=>JCheckBox upon creation
		JCheckBox box = techBoxes.get(tech);
		// so slow search is Plan B (should never happen).
		if ( box == null ) {
			final String techName = tech.name();
			for ( Component c : centerPanel.getComponents() ) {
				if ( c instanceof JCheckBox
				  && techName.equals(((JCheckBox)c).getActionCommand()) ) {
					box = (JCheckBox)c;
					techBoxes.put(tech, box);
					break;
				}
			}
		}
		return box;
	}

	// Unselect the JCheckBox and unwant this Tech
	// return was it found and unselected
	private boolean unselect(final Tech tech) {
		final JCheckBox box = boxFor(tech);
		if ( box == null )
			return false;
		// box.actionPerformed may not exist yet
		box.setSelected(false);
		// so also "manually" remove the given tech
		myWantedTechs.remove(tech);
		repaint();
		return true;
	}

	// Unselect all of the given techs
	private boolean unselect(final EnumSet<Tech> techs, final String label) {
		assert !techs.isEmpty();
		boolean result = true;
		for ( Tech tech : techs ) {
			if ( !unselect(tech) ) {
				carp(label+": unselectable: "+tech);
				result = false;
			}
		}
		return result;
	}

	private JPanel getCenterPanel() {
		if ( centerPanel == null ) {
			final TitledBorder tb = BorderFactory.createTitledBorder(
				  null
				, "" // "Available solving techniques"
				, TitledBorder.CENTER
				, TitledBorder.DEFAULT_POSITION
				, new Font("Dialog", Font.BOLD, 12)
				, new Color(51, 51, 51)
			);
			final JPanel jp = new JPanel(new GridLayout(0, NUM_COLS)); // rows, cols
			jp.setBorder(tb);
			centerPanel = jp;
		}
		return centerPanel;
	}

	// ================================ NORTH ================================

	private JPanel getNorthPanel() {
		if ( northPanel == null ) {
			final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
			jp.add(newLblExplanations(), null);
			northPanel = jp;
		}
		return northPanel;
	}

	private JLabel newLblExplanations() {
		final JLabel jl = new JLabel("Wanted Sudoku solving techniques:");
		jl.setToolTipText("ticked Techs are used to solve/analyse Sudokus."
		+ " Mouse-over is a precis. F1 for help.");
		return jl;
	}
	// ================================ SOUTH ================================

	private JPanel getSouthPanel() {
		if ( southPanel == null ) {
			final JPanel jp = new JPanel(new GridLayout(1, 3));
			jp.add(getOkButtonPanel(), null);
			jp.add(getCancelButtonPanel(), null);
			southPanel = jp;
		}
		return southPanel;
	}

	private JPanel getOkButtonPanel() {
		if ( okButtonPanel == null ) {
			final JPanel jp = new JPanel(new FlowLayout());
			jp.add(getBtnOk(), null);
			okButtonPanel = jp;
		}
		return okButtonPanel;
	}

	// --------- btnOk ---------

	// does wantedTechs.containsAny(techs)?
	private boolean anyWanted(final Tech... techs) {
		for ( Tech t : techs )
			if ( myWantedTechs.contains(t) ) // O(n) is fast enough here
				return true;
		return false;
	}

	private void commit() {
		// I want to require DynamicPlus OR NestedUnary, but still allow the
		// user to override that requirement. If you override then you risk not
		// finding solutions to puzzles that SE can solve. Just be patient!
		// DynamicPlus misses on expert puzzles, but NestedUnary ALWAYS hints,
		// as do all thereafter.
		if ( !anyWanted(Tech.DynamicPlus, Tech.NestedUnary, Tech.NestedStatic
			, Tech.NestedDynamic, Tech.NestedPlus) && !confirmNoSafetyNet() )
			return;
		// Locking XOR LockingBasic, never neither, never both.
		if ( myWantedTechs.contains(Tech.Locking) )
			myWantedTechs.remove(Tech.LockingBasic);
		else
			myWantedTechs.add(Tech.LockingBasic);
		CFG.setAndSaveWantedTechs(myWantedTechs);
		setVisible(false);
		engine.clearHints();
		Log.teeln("\nTechSelectDialog updating wanted hinters...");
		engine.recreateLogicalSolver();
		frame.refreshDisabledRulesWarning();
		dispose();
	}

	private JButton getBtnOk() {
		if ( btnOk == null ) {
			final JButton jb = new JButton("OK");
			jb.setMnemonic(KeyEvent.VK_O);
			jb.addActionListener((ActionEvent e) -> commit());
			btnOk = jb;
		}
		return btnOk;
	}

	// -------------------------

	private boolean confirmNoSafetyNet() {
		return frame.ask(
"Select \"Dynamic Plus\" as a safety-net for \"normal\" Sudoku puzzles;"+NL
+"and/or \"Nested Unary\" as a safety-net for ALL Sudoku puzzles,"+NL
+"or alternately Nested Multiple/Dynamic/Plus. If you do not select"+NL
+"a catch-all you risk not getting solutions to Sudoku puzzles that"+NL
+"Sudoku Explainer can actually solve. Just be patient!"+NL
+"Do you wish to ignore this most excellent advice and continue anyway?"
		, "Confirm no safety-net");
	}

	private JPanel getCancelButtonPanel() {
		if ( cancelButtonPanel == null ) {
			final JPanel jp = new JPanel(new FlowLayout());
			jp.add(getBtnCancel(), null);
			cancelButtonPanel = jp;
		}
		return cancelButtonPanel;
	}

	private JButton getBtnCancel() {
		if ( btnCancel == null ) {
			final JButton bc = new JButton("Cancel");
			bc.setMnemonic(KeyEvent.VK_C);
			bc.addActionListener((ActionEvent e) -> {
				TechSelectDialog.this.setVisible(false);
				TechSelectDialog.this.dispose();
			});
			btnCancel = bc;
		}
		return btnCancel;
	}

	// =============================== CONTENT ================================

	private JPanel getContentPanel() {
		if ( contentPanel == null ) {
			final JPanel cp = new JPanel(new BorderLayout());
			cp.add(getNorthPanel(), BorderLayout.NORTH);
			cp.add(getCenterPanel(), BorderLayout.CENTER);
			cp.add(getSouthPanel(), BorderLayout.SOUTH);
			contentPanel = cp;
		}
		return contentPanel;
	}

	@SuppressWarnings("fallthrough")
	private Color techColor(final Tech tech) {
		if ( tech.tip == null )
			return this.getForeground();
		switch ( tech.tip.substring(0, 4) ) {
			case "WANT": return Color.green.darker();
			case "KEEP": return COLOR_AQUA.darker();
			case "WANK": case "DROP": return Color.orange;
			default: return this.getForeground();
		}
	}

	private static void carp(String msg) {
		Log.teeln(msg);
		beep();
	}

}
