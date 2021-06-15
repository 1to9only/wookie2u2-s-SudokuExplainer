/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Settings;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/** The Help~About box - Hey, it used to be a thing, OK. */
public final class AboutDialog extends JDialog {

	private static final long serialVersionUID = -5231673684723681106L;

	private JPanel jContentPane = null;
	private JPanel pnlTop = null;
	private JLabel lblTitle = null;
	private JLabel lblCopyright = null;
	private JLabel lblCopyright2 = null;
	private JPanel pnlCenter = null;
	private JPanel pnlBottom = null;
	private JButton btnOk = null;
	private JLabel lblVersion = null;
	private JLabel txtVersion = null;
	private JLabel lblCompany = null;
	private JLabel txtCompany = null;
	private JLabel lblLicense = null;
	private JLabel txtLicense = null;

	/** Constructor. */
	public AboutDialog(JFrame dummyParentFrameKnife) {
		super(dummyParentFrameKnife);
		initialise();
	}

	private void initialise() {
		this.setSize(new java.awt.Dimension(255,203));
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("Sudoku Explainer - About");
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowDeactivated(java.awt.event.WindowEvent e) {
				AboutDialog.this.setVisible(false);
				AboutDialog.this.dispose();
			}
		});
		txtVersion.setText(Settings.VERSION + " " + Settings.BUILT);
	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getPnlTop(), java.awt.BorderLayout.NORTH);
			jContentPane.add(getPnlCenter(), java.awt.BorderLayout.CENTER);
			jContentPane.add(getPnlBottom(), java.awt.BorderLayout.SOUTH);
		}
		return jContentPane;
	}

	private JPanel getPnlTop() {
		if (pnlTop == null) {
			lblCopyright = new JLabel();
			lblCopyright.setText("(C) 2005-2007 Nicolas Juillerat");
			lblCopyright.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			lblCopyright2 = new JLabel();
			lblCopyright2.setText("(C) 2013-2021 Keith Corlett");
			lblCopyright2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			lblTitle = new JLabel();
			lblTitle.setText("Sudoku Explainer");
			lblTitle.setFont(new java.awt.Font("Comic Sans MS", java.awt.Font.BOLD, 24));
			lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			lblTitle.setHorizontalTextPosition(javax.swing.SwingConstants.TRAILING);
			lblTitle.setPreferredSize(new java.awt.Dimension(234,48));
			lblVersion = new JLabel();
			lblTitle.setText("Sudoku Explainer");
			lblTitle.setFont(new java.awt.Font("Comic Sans MS", java.awt.Font.BOLD, 24));
			lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			lblTitle.setHorizontalTextPosition(javax.swing.SwingConstants.TRAILING);
			lblTitle.setPreferredSize(new java.awt.Dimension(234,48));

			lblTitle.setIcon(new ImageIcon(getClass().getResource("Icon_Sudoku.gif")));
			pnlTop = new JPanel();
			pnlTop.setLayout(new BorderLayout());
			pnlTop.add(lblTitle, java.awt.BorderLayout.NORTH);
			pnlTop.add(lblCopyright, java.awt.BorderLayout.CENTER);
			pnlTop.add(lblCopyright2, java.awt.BorderLayout.SOUTH);
		}
		return pnlTop;
	}

	private JPanel getPnlCenter() {
		if (pnlCenter == null) {
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 1;
			gridBagConstraints6.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints6.weightx = 1.0D;
			gridBagConstraints6.gridy = 4;
			txtLicense = new JLabel();
			txtLicense.setText("Lesser General Public License");
			txtLicense.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));

			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.weightx = 1.0D;
			gridBagConstraints5.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints5.insets = new java.awt.Insets(2,10,2,0);
			gridBagConstraints5.gridy = 4;
			lblLicense = new JLabel();
			lblLicense.setText("License:");
			lblLicense.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));

			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 1;
			gridBagConstraints4.weightx = 1.0D;
			gridBagConstraints4.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints4.gridy = 3;
			txtCompany = new JLabel();
			txtCompany.setText("University of Fribourg (CH)");
			txtCompany.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));

			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.weightx = 1.0D;
			gridBagConstraints3.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints3.insets = new java.awt.Insets(2,10,2,0);
			gridBagConstraints3.gridy = 3;
			lblCompany = new JLabel();
			lblCompany.setText("Company:");
			lblCompany.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));

			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.weightx = 1.0D;
			gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.insets = new java.awt.Insets(2,10,2,0);
			gridBagConstraints1.gridy = 1;
			lblVersion = new JLabel();
			lblVersion.setText("Version:");
			lblVersion.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));

			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 1;
			gridBagConstraints2.weightx = 1.0D;
			gridBagConstraints2.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints2.gridy = 1;
			txtVersion = new JLabel();
			txtVersion.setText(""); // is set in initialise, for some reason
			txtVersion.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));

			pnlCenter = new JPanel();
			pnlCenter.setLayout(new GridBagLayout());
			pnlCenter.add(lblVersion, gridBagConstraints1);
			pnlCenter.add(txtVersion, gridBagConstraints2);
			pnlCenter.add(lblCompany, gridBagConstraints3);
			pnlCenter.add(txtCompany, gridBagConstraints4);
			pnlCenter.add(lblLicense, gridBagConstraints5);
			pnlCenter.add(txtLicense, gridBagConstraints6);
		}
		return pnlCenter;
	}

	private JPanel getPnlBottom() {
		if (pnlBottom == null) {
			pnlBottom = new JPanel();
			pnlBottom.setLayout(new FlowLayout());
			pnlBottom.add(getBtnOk(), null);
		}
		return pnlBottom;
	}

	private JButton getBtnOk() {
		if (btnOk == null) {
			btnOk = new JButton();
			btnOk.setText("OK");
			btnOk.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					AboutDialog.this.setVisible(false);
					AboutDialog.this.dispose();
				}
			});
		}
		return btnOk;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
