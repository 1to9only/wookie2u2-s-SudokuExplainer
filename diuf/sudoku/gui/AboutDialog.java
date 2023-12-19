/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Build.BUILT;
import static diuf.sudoku.Build.TITLE;
import static diuf.sudoku.Build.VERSION;
import diuf.sudoku.utils.Pair;
import java.awt.BorderLayout;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import java.awt.FlowLayout;
import java.awt.Font;
import static java.awt.Font.BOLD;
import static java.awt.Font.PLAIN;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.HORIZONTAL;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Help~About box: Hey, help used to be a thing, OK, now they just shoot
 * you for being too stupid.
 */
final class AboutDialog extends JDialog {

	private static final long serialVersionUID = -5231673684723681106L;

	private JPanel jContentPane = null;

	private JPanel pnlTop = null;
	private JLabel lblTitle = null;
	private JLabel lblCopyright = null;
	private JLabel lblCopyright2 = null;

	private JPanel pnlCenter = null;

	private JPanel pnlBottom = null;
	private JButton btnOk = null;

	/** Constructor. */
	AboutDialog(JFrame dummyParentFrameKnife) {
		super(dummyParentFrameKnife);
		initialise();
	}

	private void initialise() {
		this.setResizable(false);
		this.setSize(375, 250);
		this.setContentPane(getJContentPane());
		this.setTitle("Sudoku Explainer - About");
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowDeactivated(java.awt.event.WindowEvent e) {
				AboutDialog.this.setVisible(false);
				AboutDialog.this.dispose();
			}
		});
	}

	private JPanel getJContentPane() {
		if ( jContentPane == null ) {
			JPanel jp = new JPanel();
			jp.setLayout(new BorderLayout());
			jp.add(getPnlTop(), NORTH);
			jp.add(getPnlCenter(), CENTER);
			jp.add(getPnlBottom(), SOUTH);
			jContentPane = jp;
		}
		return jContentPane;
	}

	private JPanel getPnlTop() {
		if ( pnlTop == null ) {
			JLabel jl = new JLabel();
			jl.setText("(C) 2005-2007 Nicolas Juillerat");
			jl.setHorizontalAlignment(SwingConstants.CENTER);
			lblCopyright = jl;
			jl = new JLabel();
			jl.setText("(C) 2013-2023 Keith Corlett");
			jl.setHorizontalAlignment(SwingConstants.CENTER);
			lblCopyright2 = jl;
			jl = new JLabel();
			jl.setText(TITLE);
			jl.setFont(new Font("Comic Sans MS", BOLD, 24));
			jl.setHorizontalAlignment(SwingConstants.CENTER);
			jl.setHorizontalTextPosition(SwingConstants.TRAILING);
			jl.setPreferredSize(new java.awt.Dimension(234,48));
			jl.setIcon(new ImageIcon(getClass().getResource("Icon_Sudoku_Gray.gif")));
			lblTitle = jl;
			final JPanel jp = new JPanel();
			jp.setLayout(new BorderLayout());
			jp.add(lblTitle, NORTH);
			jp.add(lblCopyright, CENTER);
			jp.add(lblCopyright2, SOUTH);
			pnlTop = jp;
		}
		return pnlTop;
	}

	private JPanel getPnlCenter() {
		if ( pnlCenter == null ) {
			final JLabelPair version = new JLabelPair(0, "Version:", VERSION);
			final JLabelPair built   = new JLabelPair(1, "Built:", BUILT);
			final JLabelPair licence = new JLabelPair(2, "Licence:", "Lesser General Public License");
			final JLabelPair company = new JLabelPair(3, "Company:", "University of Fribourg (CH)");
			final JPanel jp = new JPanel();
			jp.setLayout(new GridBagLayout());
			version.addTo(pnlCenter = jp);
			built.addTo(pnlCenter);
			company.addTo(pnlCenter);
			licence.addTo(pnlCenter);
		}
		return pnlCenter;
	}

	private JPanel getPnlBottom() {
		if (pnlBottom == null) {
			final JPanel jp = new JPanel();
			jp.setLayout(new FlowLayout());
			jp.add(getBtnOk(), null);
			pnlBottom = jp;
		}
		return pnlBottom;
	}

	private JButton getBtnOk() {
		if (btnOk == null) {
			final JButton jb = new JButton();
			jb.setText("OK");
			jb.addActionListener((java.awt.event.ActionEvent e) -> {
				AboutDialog.this.setVisible(false);
				AboutDialog.this.dispose();
			});
			btnOk = jb;
		}
		return btnOk;
	}

	/**
	 * JLabelPair is, funnily enough, a pair of JLabels: <br>
	 * The first I call the "label" on the left, in a bold font, <br>
	 * The second I call "text" on the right, in a plain font.
	 * <p>
	 * There is nothing stopping this from being a separate class, its just
	 * private static in AboutDialog because thats the only place I want it.
	 * This is as DRY as I can make it, which is really only for fun.
	 */
	private static class JLabelPair extends Pair<Pair<JLabel,GridBagConstraints>,Pair<JLabel,GridBagConstraints>> {

		private static final String FONT_NAME = "Dialog";
		private static final int FONT_SIZE = 12;
		private static final Font FONT_BOLD = new Font(FONT_NAME, BOLD, FONT_SIZE);
		private static final Font FONT_PLAIN = new Font(FONT_NAME, PLAIN, FONT_SIZE);

		private static GridBagConstraints GBC(final int gridx, final int gridy) {
			final GridBagConstraints result = new GridBagConstraints();
			result.gridx = gridx;
			result.gridy = gridy;
			result.weightx = 1.0D;
			result.fill = HORIZONTAL;
			return result;
		}

		private static GridBagConstraints GBC(final int gridx, final int gridy, final Insets insets) {
			final GridBagConstraints result = GBC(gridx, gridy);
			result.insets = insets;
			return result;
		}

		private static JLabel jLabel(final String text, final Font font) {
			final JLabel result = new JLabel();
			result.setText(text);
			result.setFont(font);
			return result;
		}

		JLabelPair(final int row, final String label, final String text) {
			// "label" on the left, in bold font (NEW Insets required)
			super(new Pair<>(jLabel(label, FONT_BOLD), GBC(0, row, new Insets(2,10,2,0)))
			   // "text" on the right, in plain font
			   , new Pair<>(jLabel(text, FONT_PLAIN), GBC(1, row)));
		}

		void addTo(final JPanel panel) {
			panel.add(a.a, a.b);
			panel.add(b.a, b.b);
		}
	}

}
