/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import java.io.File;
import java.util.Objects;

/**
 * File name and optionally a 1-based line number that identifies a
 * a Sudoku Puzzle.
 * <p>
 * A PuzzleID is an immutable transfer object, meaning that it doesn't change
 * after it's constructed. If a PuzzleID has changed you construct a new one.
 */
public final class PuzzleID {

	public final File file; // the file that contains this Sudoku puzzle
	public final String fileName;
	public final boolean isTop1465;
	public final int lineNumber; // 1 based, for MagicTour files

	public static PuzzleID parse(String line) {
		File file; int lineNumber;
		// nb: split(single char) is faster than grouped regex "^(\\d+)#(.*)"
		String[] words = line.split("#", 0);
		if ( words.length == 2 ) {
			lineNumber = Integer.parseInt(words[0]);
			file = new File(words[1]);
		} else {
			lineNumber = 0;
			file = new File(line);
		}
		return new PuzzleID(file, lineNumber);
	}

	public PuzzleID(File file) {
		this(file, 0);
	}

	public PuzzleID(PuzzleID src) {
		this(src.file, src.lineNumber);
	}

	public PuzzleID(final File file, final int lineNumber) {
		this.file = file;
		if ( file != null) {
			this.fileName = file.getName();
			this.isTop1465 = fileName.toLowerCase().contains("top1465");
		} else {
			this.fileName = "";
			this.isTop1465 = false;
		}
		this.lineNumber = lineNumber;
	}

	public String toStringShort() {
		if ( toStringS == null ) {
			if ( lineNumber > 0 ) // lineNumber is 1 based!
				toStringS = Integer.toString(lineNumber)+"#"+fileName;
			else
				toStringS = fileName;
		}
		return toStringS;
	}
	private String toStringS;

	@Override
	public String toString() {
		if ( toString == null ) {
			if ( file == null )
				toString = "";
			else if ( lineNumber > 0 ) // lineNumber is 1 based!
				toString = Integer.toString(lineNumber)+"#"+file.getAbsolutePath();
			else
				toString = file.getAbsolutePath();
		}
		return toString;
	}
	private String toString;

	@Override
	public boolean equals(Object o) {
		if ( o==null || !(o instanceof PuzzleID) )
			return false;
		PuzzleID other = (PuzzleID)o;
		return lineNumber == other.lineNumber
			&& file.equals(other.file);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79 * hash + this.lineNumber;
		hash = 79 * hash + Objects.hashCode(this.file);
		return hash;
	}
}
