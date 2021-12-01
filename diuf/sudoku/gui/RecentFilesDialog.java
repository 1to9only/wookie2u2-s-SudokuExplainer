/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Build;
import diuf.sudoku.SourceID;
import diuf.sudoku.Settings;
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
	RecentFilesDialog(SudokuFrame frame, SudokuExplainer engine) {
		super(frame, false);
		this.frame = frame;
		this.engine = engine;

		// This
		setTitle("Select a recently accessed Sudoku Puzzle");
		setResizable(true);

		// Overall layout
		getContentPane().setLayout(new BorderLayout());

		// top pane: contains file list
		JPanel topPanel = new JPanel();
		JScrollPane scroll = new JScrollPane(topPanel
				, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
				, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scroll, BorderLayout.CENTER);
		lstFiles = new JList<>(engine.recentFiles.toArray());
		lstFiles.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				selectedPuzzleID = lstFiles.getSelectedValue();
				btnOpen.setEnabled(true);
			}
		});
		lstFiles.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) { }
			@Override
			public void keyPressed(KeyEvent e) { }
			@Override
			public void keyReleased(KeyEvent e) {
				final int k = e.getKeyCode();
				if ( k == KeyEvent.VK_ENTER )
					openSelectedFile();
			}
		});
		lstFiles.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if ( e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
					e.consume();
					openSelectedFile();
				}
			}

		});
		topPanel.add(lstFiles);

		// bottom pane
		JPanel bottomPanel = new JPanel(new GridLayout(1, 2)); //1 row, 2 cols
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPanel.add(pnlButtons);

		btnOpen = new JButton();
		btnOpen.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
		btnOpen.setText("Open");
		btnOpen.setEnabled(false);
		btnOpen.setMnemonic(KeyEvent.VK_O);
		btnOpen.setToolTipText("Open the selected file");
		btnOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openSelectedFile();
			}
		});
		pnlButtons.add(btnOpen);

		btnCancel = new JButton();
		btnCancel.setText("Cancel");
		btnCancel.setMnemonic(KeyEvent.VK_C);
		btnCancel.setToolTipText("Close this dialog");
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		pnlButtons.add(btnCancel);
	}

	private void openSelectedFile() {
		if ( selectedPuzzleID == null ) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
//		engine.frame.clearHintDetailArea();
		SourceID pid = selectedPuzzleID;
		SourceID loaded = engine.loadFile(pid);
		if ( loaded == null ) {
			frame.setTitle(Build.ATV+"    "+Build.BUILT);
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		frame.defaultDirectory = pid.file.getParentFile();
		frame.setTitle(Build.ATV+"    "+loaded);
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
