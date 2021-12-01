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
 * A SourceID is an immutable transfer object, so it doesn't change once it's
 * constructed. Create a new one if/when the source changes.
 * <p>
 * I contain the file-name and optionally a 1-based line number that identifies
 * the source of a Sudoku Puzzle.
 */
public final class SourceID {

	// THE empty SourceID, which should probably be in SourceID. sigh.
	public static final SourceID EMPTY_PUZZLE_ID = new SourceID(null, 0);

	public final File file; // the file that contains this Sudoku puzzle
	public final String fileName;
	public final boolean isTop1465;
	public final int lineNumber; // 1 based, for MagicTour files

	/**
	 * parse a SourceID from the given line, which is in the format:
	 * {@code [hintNum/]lineNum#fileName}. The optional hintNum is ignored.
	 *
	 * @param line
	 * @return
	 */
	public static SourceID parse(String line) {
		// remove the hintNum/ from start of hintNum/lineNum#fileName
		final int i = line.indexOf('/'); // useless on unix! sigh.
		if ( i>0 && line.matches("\\d+/\\d+#.*") ) {
			line = line.substring(i+1);
		}
		// nb: split(char) is faster than grouped regex "^(\\d+)#(.*)"
		final String[] words = line.split("#", 0);
		if ( words.length != 2 ) {
			return new SourceID(new File(line), 0);
		}
		return new SourceID(new File(words[1]), Integer.parseInt(words[0]));
	}

	public SourceID(File file) {
		this(file, 0);
	}

	public SourceID(SourceID src) {
		this(src.file, src.lineNumber);
	}

	public SourceID(final File file, final int lineNumber) {
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
		if ( o==null || !(o instanceof SourceID) )
			return false;
		SourceID other = (SourceID)o;
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
