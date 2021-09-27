/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.Log;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * A JDialog with a combo box from: $HOME/DiufSudoku_log_view_hint_regexs.txt
 * Just add your own frequently used regex's in your text-editor.
 *
 * @author Keith Corlett 2021-06-05
 */
public class LogViewHintRegexDialog extends JDialog {

	private static final long serialVersionUID = 103968294295368L;

	private JPanel jContentPane = null;
	private final SudokuFrame sudokuFrame;

	private JPanel pnlTop = null;
	private JComboBox<String> cboRegex = null;
	private String regex;

	private JPanel pnlBottom = null;
	private JButton btnOk = null, btnCancel = null;

	public LogViewHintRegexDialog(SudokuFrame sudokuFrame) {
		super(sudokuFrame);
		this.sudokuFrame = sudokuFrame;
		initialise();
	}

	private void initialise() {
		this.setTitle("Log View: Hint Regex");
		final Dimension size = new Dimension(400, 120); // width, height
		this.setSize(size);
		final Dimension screen = getToolkit().getScreenSize();
		this.setLocation(screen.width/2 - size.width/2
				, screen.height/2 - size.height/2);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		// I'm attempting to make the enter key press btnOk
		btnOk.getRootPane().setDefaultButton(btnOk);
		// I'm attempting to make the escape key press btnCancel
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
					LogViewHintRegexDialog.this.setVisible(false);
					LogViewHintRegexDialog.this.dispose();
					e.consume();
				}
			}
		});
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				LogViewHintRegexDialog.this.setVisible(false);
				LogViewHintRegexDialog.this.dispose();
			}
		});
	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel(new BorderLayout());
			jContentPane.add(getPnlTop(), BorderLayout.NORTH);
			jContentPane.add(getPnlBottom(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}

	private JPanel getPnlTop() {
		if (pnlTop == null) {
			// cboRegex is the only control in the top panel, so do all here.
			cboRegex = new JComboBox<>();
			// allow the user to modify regex's (not saved unfortunately)
			cboRegex.setEditable(true);
			// tell the layout-manager how wide to make the combobox
			cboRegex.setPreferredSize(new Dimension(380, 30));
			boolean oops = false; // any Exception in load, for 1 OOPS message
			try {
				// load $HOME/DiufSudoku_log_view_hint_regexs.txt into cboRegex
				for ( String item : IO.slurp(IO.LOG_VIEW_HINT_REGEXS) )
					if ( item!=null && !item.isEmpty() )
						cboRegex.addItem(item);
			} catch (Exception ex) {
				oops = true;
				Log.teef("OOPS: %s: failed to load cboRegex: %s\n", Log.me(), ex);
				Toolkit.getDefaultToolkit().beep();
			}
			cboRegex.setToolTipText("a Java regular expression that matches the WHOLE hint string (char 65 onwards in a hint-line in the logFile).");
			cboRegex.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					regex = (String)cboRegex.getSelectedItem();
				}
			});
			// I'm attempting to make the escape key press btnCancel
			cboRegex.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
						LogViewHintRegexDialog.this.setVisible(false);
						LogViewHintRegexDialog.this.dispose();
						e.consume();
					}
				}
			});
			if ( cboRegex.getItemCount() > 0 )
				// fire above actionListener to set the regex
				cboRegex.setSelectedIndex(0);
			else if ( !oops ) // sucessfully loaded an empty file, I presume
				Log.teef("OOPS: %s: cboRegex remains empty, presumably because file is empty: %s\n", Log.me(), IO.LOG_VIEW_HINT_REGEXS);
			pnlTop = new JPanel();
			pnlTop.add(cboRegex);
		}
		return pnlTop;
	}

	private JPanel getPnlBottom() {
		if (pnlBottom == null) {
			pnlBottom = new JPanel(new FlowLayout());
			pnlBottom.add(getBtnOk(), null);
			pnlBottom.add(getBtnCancel(), null);
		}
		return pnlBottom;
	}

	private JButton getBtnOk() {
		if (btnOk == null) {
			btnOk = new JButton("Ok");
			btnOk.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					LogViewHintRegexDialog.this.setVisible(false);
					try {
						IO.save(cboRegex, IO.LOG_VIEW_HINT_REGEXS);
					} catch (IOException ex) {
						Log.format("OOPS: %s: failed to save cboRegex to file: %s", Log.me(), IO.LOG_VIEW_HINT_REGEXS);
					}
					sudokuFrame.logView(regex);
					LogViewHintRegexDialog.this.dispose();
				}
			});
		}
		return btnOk;
	}

	private JButton getBtnCancel() {
		if (btnCancel == null) {
			btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					LogViewHintRegexDialog.this.setVisible(false);
					LogViewHintRegexDialog.this.dispose();
				}
			});
		}
		return btnCancel;
	}

}
