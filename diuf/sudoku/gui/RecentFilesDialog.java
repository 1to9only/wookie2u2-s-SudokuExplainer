/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.beep;
import diuf.sudoku.SourceID;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * GUI Ctrl-M => RecentFiles Dialog enables user to quickly and easily re-open
 * a recently accessed (opened or saved) file. This is especially useful when,
 * like me, you have Sudoku Puzzles in multiple directories.
 * @author Keith Corlett 2017 Dec
 */
class RecentFilesDialog extends JDialog {

	private static final long serialVersionUID = 8345654679870154673L;

	private final SudokuFrame frame;
	private final SudokuExplainer engine;

	private JButton btnOpen;
	private final JButton btnCancel;

	private JList<SourceID> lstFiles;
	private SourceID selectedPuzzleID = null;

	/** Constructor. */
	RecentFilesDialog(final SudokuFrame frame, final SudokuExplainer engine) {
		super(frame, false);
		this.frame = frame;
		this.engine = engine;

		// This
		setTitle("Select a recently accessed Sudoku Puzzle");
		setResizable(true);

		// Overall layout
		getContentPane().setLayout(new BorderLayout());

		// top pane: contains file list
		final JPanel topPanel = new JPanel();
		final JScrollPane scroll = new JScrollPane(topPanel
				, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
				, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scroll, BorderLayout.CENTER);
		final JList<SourceID> jl = new JList<>(engine.recentFiles.toArray());
		jl.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				selectedPuzzleID = lstFiles.getSelectedValue();
				btnOpen.setEnabled(true);
			}
		});
		jl.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) { }
			@Override
			public void keyPressed(final KeyEvent e) { }
			@Override
			public void keyReleased(final KeyEvent e) {
				final int k = e.getKeyCode();
				if ( k == KeyEvent.VK_ENTER )
					openSelectedFile();
			}
		});
		jl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if ( e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
					e.consume();
					openSelectedFile();
				}
			}

		});
		topPanel.add(lstFiles=jl);

		// bottom pane
		final JPanel bottomPanel = new JPanel(new GridLayout(1, 2)); //1 row, 2 cols
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		final JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPanel.add(pnlButtons);

		JButton jb = new JButton();
		jb.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
		jb.setText("Open");
		jb.setEnabled(false);
		jb.setMnemonic(KeyEvent.VK_O);
		jb.setToolTipText("Open the selected file");
		jb.addActionListener((ActionEvent e) -> {
			openSelectedFile();
		});
		pnlButtons.add(btnOpen=jb);

		jb = new JButton();
		jb.setText("Cancel");
		jb.setMnemonic(KeyEvent.VK_C);
		jb.setToolTipText("Close this dialog");
		jb.addActionListener((ActionEvent e) -> {
			close();
		});
		pnlButtons.add(btnCancel=jb);
	}

	private void openSelectedFile() {
		if ( selectedPuzzleID == null ) {
			beep();
			return;
		}
//		engine.frame.clearHintDetailArea();
		final SourceID pid = selectedPuzzleID;
		final SourceID loaded = engine.loadFile(pid);
		if ( loaded == null ) {
			frame.setTitle();
			beep();
			return;
		}
		frame.defaultDirectory = pid.file.getParentFile();
		frame.setTitle(loaded);
		close();
	}

//	private void carp (String title, String message) {
//		JOptionPane.showMessageDialog(this, message, title
//			, JOptionPane.ERROR_MESSAGE);
//	}

	private void close() {
		super.setVisible(false);
		super.dispose();
	}

}
