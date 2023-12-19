/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.beep;
import diuf.sudoku.io.IO;
import diuf.sudoku.utils.Log;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
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
 * Just add your own frequently used regexs in your text-editor.
 *
 * @author Keith Corlett 2021-06-05
 */
class LogViewHintRegexDialog extends JDialog {

	private static final long serialVersionUID = 103968294295368L;

	private JPanel jContentPane = null;
	private final SudokuFrame sudokuFrame;

	private JPanel pnlTop = null;
	private JComboBox<String> cboRegex = null;
	private String regex;

	private JPanel pnlBottom = null;
	private JButton btnOk = null, btnCancel = null;

	LogViewHintRegexDialog(final SudokuFrame sudokuFrame) {
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
		// I am attempting to make the enter key press btnOk
		btnOk.getRootPane().setDefaultButton(btnOk);
		// I am attempting to make the escape key press btnCancel
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) {
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
					LogViewHintRegexDialog.this.setVisible(false);
					LogViewHintRegexDialog.this.dispose();
					e.consume();
				}
			}
		});
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(final WindowEvent e) {
				LogViewHintRegexDialog.this.setVisible(false);
				LogViewHintRegexDialog.this.dispose();
			}
		});
	}

	private JPanel getJContentPane() {
		if ( jContentPane == null ) {
			final JPanel jp = new JPanel(new BorderLayout());
			jp.add(getPnlTop(), BorderLayout.NORTH);
			jp.add(getPnlBottom(), BorderLayout.SOUTH);
			jContentPane = jp;
		}
		return jContentPane;
	}

	private JPanel getPnlTop() {
		if ( pnlTop == null ) {
			// cboRegex is the only control in the top panel, so do all here.
			final JComboBox<String> jcb = new JComboBox<>();
			// allow the user to modify regexs (not saved unfortunately)
			jcb.setEditable(true);
			// tell the layout-manager how wide to make the combobox
			jcb.setPreferredSize(new Dimension(380, 30));
			boolean oops = false; // any Exception in load, for 1 OOPS message
			try {
				// load $HOME/DiufSudoku_log_view_hint_regexs.txt into jcb
				for ( String item : IO.slurp(IO.LOG_VIEW_HINT_REGEXS) )
					if ( item!=null && !item.isEmpty() )
						jcb.addItem(item);
			} catch (IOException ex) {
				oops = true;
				Log.teef("WARN: %s: failed to load: %s\n", Log.me(), ex);
				beep();
			}
			jcb.setToolTipText("a Java regular expression that matches the WHOLE"
			+ " hint string (char 65 onwards in a hint-line in the logFile).");
			jcb.addActionListener((ActionEvent e) -> {
				regex = (String)jcb.getSelectedItem();
			});
			// I am attempting to make the escape key press btnCancel
			jcb.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(final KeyEvent e) {
					if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
						LogViewHintRegexDialog.this.setVisible(false);
						LogViewHintRegexDialog.this.dispose();
						e.consume();
					}
				}
			});
			if ( jcb.getItemCount() > 0 )
				// fire above actionListener to set the regex
				jcb.setSelectedIndex(0);
			else if ( !oops ) // sucessfully loaded an empty file, I presume
				Log.teef("WARN: %s: empty file: %s\n", Log.me(), IO.LOG_VIEW_HINT_REGEXS);
			pnlTop = new JPanel();
			pnlTop.add(cboRegex=jcb);
		}
		return pnlTop;
	}

	private JPanel getPnlBottom() {
		if ( pnlBottom == null ) {
			final JPanel jp = new JPanel(new FlowLayout());
			jp.add(getBtnOk(), null);
			jp.add(getBtnCancel(), null);
			pnlBottom = jp;
		}
		return pnlBottom;
	}

	private JButton getBtnOk() {
		if ( btnOk == null ) {
			final JButton jb = new JButton("Ok");
			jb.addActionListener((ActionEvent e) -> {
				LogViewHintRegexDialog.this.setVisible(false);
				try {
					IO.save(cboRegex, IO.LOG_VIEW_HINT_REGEXS);
				} catch (IOException ex) {
					Log.format("WARN: %s: failed to save: %s", Log.me(), IO.LOG_VIEW_HINT_REGEXS);
				}
				sudokuFrame.logView(regex);
				LogViewHintRegexDialog.this.dispose();
			});
			btnOk = jb;
		}
		return btnOk;
	}

	private JButton getBtnCancel() {
		if ( btnCancel == null ) {
			final JButton jb = new JButton("Cancel");
			jb.addActionListener((ActionEvent e) -> {
				LogViewHintRegexDialog.this.setVisible(false);
				LogViewHintRegexDialog.this.dispose();
			});
			btnCancel = jb;
		}
		return btnCancel;
	}

}
