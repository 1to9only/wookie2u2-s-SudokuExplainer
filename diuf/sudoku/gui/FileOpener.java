/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.SourceID;
import diuf.sudoku.io.IO;
import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.MyFile.exists;
import static diuf.sudoku.utils.MyFile.nameEndsWith;
import java.io.File;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import javax.swing.filechooser.FileFilter;

/**
 * FileOpener exists because opening a file is complicated,
 * like a TinOpener for Rins tin of Tims.
 * Theres no Tims here mate!
 * <p>
 * This class gets all this code out of SudokuFrame, so he can concentrate on
 * being a SudokuFrame, and I concentrate on opening files. There is quite a
 * lot of code here, and its quite complex, to achieve such a simple task. I
 * wonder why the DOD is a mess? Shoot the users!
 * <p>
 * TYPICAL USE: See {@link SudokuFrame#openFile()}.
 *
 *
 * @author Keith Corlett 2023-06-25
 */
class FileOpener {

	private final SudokuFrame frame;
	private final SudokuExplainer engine;
	File defaultDirectory;

	FileOpener(final SudokuFrame frame, final SudokuExplainer engine, final File defaultDirectory) {
		this.frame = frame;
		this.engine = engine;
		this.defaultDirectory = defaultDirectory;
	}

	// provide a default for the defaultDirectory, ergo never null
	private File defaultDefaultDirectory(File defaultDirectory) {
		if ( defaultDirectory == null ) {
			try {
				// normal people use *.txt, hackers do *.mt and *.txt.
				if ( SourceID.isTextFile(engine.getGridSource()) )
					// one-puzzle: *.txt
					defaultDirectory = IO.PUZZLES_DIR;
				else
					// multi-puzzle: *.mt (MagicTour)
					defaultDirectory = IO.HOME_DIR;
			} catch (Exception eaten) {
				// default to one-puzzle: *.txt, for normal people
				defaultDirectory = IO.PUZZLES_DIR;
			}
		}
		return defaultDirectory;
	}

	// user chooses a File in $filter, with $defualt, else null
	private File chooseFileToOpen(final FileFilter filter, final File defualt) {
		final JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(filter);
		// set the jfcs current directory
		if ( defualt!=null && defualt.exists() )
			try {
				defaultDirectory = new File(defualt.getParent());
				assert defaultDirectory.exists();
			} catch (NullPointerException eaten) {
				// NEVER happens. Never say never.
				defaultDirectory = null;
			}
		else
			defaultDirectory = defaultDefaultDirectory(defaultDirectory);
		jfc.setCurrentDirectory(defaultDirectory);
		jfc.setSelectedFile(defualt);
		final int option = jfc.showOpenDialog(frame);
		defaultDirectory = jfc.getCurrentDirectory();
		return option==APPROVE_OPTION ? jfc.getSelectedFile() : null;
	}

	File logFile(final File defualt) {
		return chooseFileToOpen(new LogFileFilter(), defualt);
	}

	File logFile() {
		return logFile(null);
	}

	// ------------------------------------------------------------------------

	private File chooseSaveFile(final FileFilter filter) {
		final JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(filter);
		defaultDirectory = defaultDefaultDirectory(defaultDirectory);
		jfc.setCurrentDirectory(defaultDirectory);
		if ( APPROVE_OPTION != jfc.showSaveDialog(frame) )
			return null;
		defaultDirectory = jfc.getCurrentDirectory();
		return jfc.getSelectedFile();
	}

	File toSave() {
		final String ext = ".txt";
		File file = chooseSaveFile(new TextFileFilter());
		if ( file == null )
			return null;
		if ( !nameEndsWith(file, ext) )
			file = new File(file.getAbsolutePath() + ext);
		if ( file.exists()
		  && !frame.ask("The file \"" + file.getName() + "\" already exists." + NL
				+ "Do you want to replace the existing file ?") )
			return null;
		return file;
	}

	// ------------------------------------------------------------------------

	// Extract a SourceID from the selectedFile.toString().
	// This is required because Swing prepends the current directory to whatever
	// the user typed, because that absolutely MUST be a file name, right?
	// So we remove the preceeding current-directory-path, and parse the rest
	// of the string as a SourceID, and return it.
	private SourceID extractSourceID(String s) {
		try {
			// no hash means there is definately no SourceID here
			if ( s.indexOf('#') < 0 ) { // ie -1 meaning not found
				frame.carp("'#' not found in:" + NL + s, "File Open");
				return null;
			}
			// strip prepended current directory path
			final String dd = defaultDirectory.getAbsolutePath();
			if ( s.startsWith(dd, 0) )
				s = s.substring(dd.length() + 1);
			// parse the rest into a SourceID and return it
			return SourceID.parse(s);
		} catch (Exception ex) {
			frame.carp(ex, "File Open");
			return null;
		}
	}

	// handle SourceID special case, for example:
	// 347#C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
	private SourceID getSource(final File file) {
		// the user pressed cancel
		if ( file == null )
			return null;
		// the normal case (~95%)
		if ( file.exists() )
			return new SourceID(file, 0);
		// nope: has file extension, so it could be a SourceID
		if ( file.getName().contains(".") )
			return extractSourceID(file.toString());
		// nope: try $file.txt
		String filePath;
		if ( exists(filePath=file.getAbsolutePath() + ".txt") )
			return new SourceID(new File(filePath), 0);
		// nope: try $file.mt
		if ( exists(filePath=file.getAbsolutePath() + ".mt") )
			return new SourceID(new File(filePath), 1);
		// nope: last resort try SourceID, which may fail -> null
		return extractSourceID(file.toString());
	}

	SourceID puzzleSource() {
		return getSource(chooseFileToOpen(new PuzzleFileFilter(), null));
	}

	private static final class PuzzleFileFilter extends FileFilter {
		@Override
		public boolean accept(final File f) {
			if ( f.isDirectory() )
				return true;
			return nameEndsWith(f, ".txt")
				|| nameEndsWith(f, ".mt");
		}
		@Override
		public String getDescription() {
			return "Puzzle files (*.txt;*.mt)";
		}
	}

	// for mitLogView
	private final class LogFileFilter extends FileFilter {
		@Override
		public boolean accept(final File f) {
			if ( f.isDirectory() )
				return true;
			final String n = f.getName().toLowerCase();
			return n.endsWith(".log");
		}
		@Override
		public String getDescription() {
			return "Log files (*.log)";
		}
	}

	private final class TextFileFilter extends FileFilter {
		@Override
		public boolean accept(final File f) {
			if ( f.isDirectory() )
				return true;
			return f.getName().toLowerCase().endsWith(".txt");
		}
		@Override
		public String getDescription() {
			return "Text files (*.txt)";
		}
	}

}
