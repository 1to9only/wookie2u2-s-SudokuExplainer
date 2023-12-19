/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.SourceID;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import static diuf.sudoku.utils.Frmt.COMMA;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.IAsker;
import diuf.sudoku.utils.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import static diuf.sudoku.Grid.BY9;
import static diuf.sudoku.utils.MyFile.nameEndsWith;
import static diuf.sudoku.Values.BITS9;

/**
 * GridLoader exposes a static load method that loads a Grid from any/all
 * supported formats.
 * <p>
 * NOTE: GridLoader is available ONLY in the gui package, for use in the GUI.
 * I presume a user exists, of whom I can ask which puzzle to load. Everywhere
 * outside of the GUI just calls grid.load!
 * <p>
 * NOTE: Internally I use an inner class to handle each format.
 * <p>
 * NOTE: GridLoader is stateless: each method does its stuff and returns,
 * not retaining any state. All the statefull stuff is done before load is
 * called, or after; not in here.
 *
 * @author Keith Corlett 2021-07-22
 */
class GridLoader {

	/**
	 * Load a puzzle: ie loads this lineNumber of this File into this Grid.
	 * <p>
	 * Two puzzle-file-formats are supported:<ul>
	 * <li>Multiple puzzles are stored in .mt (MagicTour) format:<ul>
	 *  <li>example: <br>
	 *  7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5</li>
	 *  <li>a line of 81 chars: 1..9=clue, other=0=empty (standard is '.')</li>
	 *  </ul>
	 *  </li>
	 * <li>Single puzzles are stored in .txt (Text) format:<ul>
	 *  <li>example: <br>
	 *  7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5 <br>
	 *  ,1589,568,1269,13569,23569,,1235,123,14569,,456,1469,,34569,156,,13,1456,145,,1246,156,,12567,1257,,2489,4789,2478,,1689,469,,1247,12478,3458,,4578,148,,34,1578,,1478,234589,4589,,489,389,,258,245,,124568,14578,245678,,568,256,,1247,12478,1258,,2578,2789,,259,1278,,1278,2468,478,,2678,68,,278,2347,</li>
	 *  <li>a line of 81 chars: 1..9=clue, other=0=empty (standard is '.')</li>
	 *  <li>a line of 81 CSV maybes: ,1589,568,1269,...</li>
	 *  <li>The second line of maybes is optional</li>
	 *  <li>CSV stands for Comma Separated Values (an ISO spec exists)</li>
	 *  <li>This single puzzle format enables us to save the maybes so that a
	 *   partly-solved puzzle can be stored in its complete current state,
	 *   which is handy for testing.</li>
	 *  <li>NOTE: TextFormat also attempts to load all "old" text-file-formats
	 *   but this is NOT tested, so "old" text-files should be regarded as
	 *   deprecated, so all old text-files should be opened and re-saved
	 *   (overwritten) in the current format, ergo just open and save the
	 *   bastards, to save me from a mountain of backwards complexity.</li>
	 *  </ul>
	 *  </li>
	 * </ul>
	 *
	 * @param asker the SudokuFrame, or any other implementation of IAsker.
	 *  If asker is null I resort to asking the user questions using MsgBoxs
	 *  that are unattached to the rest of the interface (independent apps).
	 *  The only time I ask the user is when an .mt file is opened and the
	 *  lineNumber is 0 or is greater than the number of lines in the file,
	 *  which should not be an issue when reloading a recent file.
	 * @param grid to read into
	 * @param file to read from
	 * @param lineNumber of puzzle to read. Used only for .mt (MagicTour) files
	 * @return true if it loaded, else false (see standard error).
	 */
	static boolean load(final IAsker asker, final Grid grid, final File file, final int lineNumber) {
		if ( nameEndsWith(file, ".mt") ) // multi-puzzle format
			return MagicTourFormat.load(asker, grid, file, lineNumber);
		if ( nameEndsWith(file, ".txt") ) // single-puzzle format
			return TextFormat.load(grid, file);
		return StdErr.whinge("Unrecognised file format: "+file);
	}

	/**
	 * MagicTourFormat loads this .mt (MagicTour) format line into this Grid.
	 * <p>
	 * Note that MagicTour is the Grids "native" format, so the Grid does
	 * its own load, I just get the line from the file as a String.
	 *
	 * @author Keith Corlett 2021-07-22
	 */
	private static class MagicTourFormat {

		private static final String LINE_NUMBER_QUESTION =
				"MagicTour (*.mt) is a multi-line format."+NL
				+"So which line (1 based) do you want?";

		static boolean load(final IAsker asker, final Grid grid, final File file, final int lineNumberParam) {
			try {
				final int lineCount = countLines(file);
				final int lineNumber;
				if ( lineNumberParam>0 && lineNumberParam<=lineCount )
					lineNumber = lineNumberParam;
				else
					if ( asker != null ) {
						// normal operation
						lineNumber = asker.askForInt(LINE_NUMBER_QUESTION, 1, lineCount);
					} else {
						// only at start-up if a .mt file is ever truncated.
						lineNumber = Ask.forInt(LINE_NUMBER_QUESTION, 1, lineCount);
					}
				try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
					for ( int i=1; i<lineNumber; ++i )
						reader.readLine();
					String line = reader.readLine();
					if ( line==null || line.length()<GRID_SIZE )
						throw new IOException("Not "+GRID_SIZE+" chars:"+NL+line);
					// MT (MagicTour) is Grids native format.
					grid.load(line);
				} catch (Exception ex) {
					StdErr.whinge("WARN: MagicTourFormat.load failed: "+lineNumber+"#"+file, ex);
				}
				grid.isMaybesLoaded = false;
				grid.source = new SourceID(file, lineNumber);
				return true;
			} catch (IOException ex) {
				return StdErr.whinge("WARN: "+Log.me()+" IOException", ex);
			}
		}

		/**
		 * Counts the number of lines (1 based) in the given file.
		 * @param file to count lines of/in (I never know).
		 * @return the 1 based number of lines in this file. WARN: maybe 0.
		 * @throws FileNotFoundException
		 * @throws IOException
		 */
		private static int countLines(final File file) throws FileNotFoundException, IOException {
			int lineCount = 0;
			try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
				while ( reader.readLine() != null )
					++lineCount;
			}
			return lineCount;
		}

	}

	/**
	 * TextFormat loads this "old" .txt file format into this Grid.
	 * <p>
	 * Note that TextFormat also attempts to load all previously used formats
	 * for a .txt file, but this is NOT tested, and so may-not-work over time.
	 * There is a limit to how much previous stupidity can be supported.
	 * Witness the conservatives. We really should pinch that one off.
	 *
	 * @author Keith Corlett 2021-07-22
	 */
	private static class TextFormat {

		static boolean load(final Grid grid, final File file) {
			try {
				final ArrayList<String> lines = IO.slurp(file);
				if ( lines.get(0).length() == GRID_SIZE ) {
					// toString format: delegate back to the grid
					grid.load(lines); // sets grid.isMaybesLoaded internally
				} else if ( lines.size() >= REGION_SIZE ) {
					// presume old format: 9 lines of 9 values
					//                   + 9 lines of 9 csved maybes
					// most existing files are in this format. No plan to convert.
					for ( int y=0; y<REGION_SIZE; ++y ) // read cell values
						setClues(grid, lines.get(y), y);
					if ( lines.size() >= REGION_SIZE<<1 ) {
						grid.isMaybesLoaded = true;
						for ( int y=0; y<REGION_SIZE; ++y ) // read maybes
							if ( !setMaybes(grid, lines.get(y+REGION_SIZE), y) ) {
								grid.isMaybesLoaded = false;
								break; // they will all be splattered anyway
							}
					}
				} else {
					Log.teeln("GridLoader.load: Bad lines:");
					for ( String line : lines )
						Log.teeln(line);
					throw new IllegalArgumentException("Bad lines!");
				}
				grid.source = new SourceID(file, 0);
			} catch (Exception ex) {
				return StdErr.whinge("WARN: "+Log.me()+" exception", ex);
			}
			return true;
		}

		private static void setClues(final Grid grid, final String line, final int y) {
			if ( line==null || line.length() < REGION_SIZE )
				return;
			char ch;
			final int offset = BY9[y];
			for ( int x=0; x<REGION_SIZE; ++x )
				if ( (ch=line.charAt(x))>='1' && ch<='9' )
					grid.cells[offset+x].set(ch-'0'); // NB: x,y (ie col,row)
		}

		private static boolean setMaybes(final Grid grid, final String line, final int y) {
			final String[] fields = line.split(COMMA, REGION_SIZE);
			if ( fields.length != REGION_SIZE ) { // unexpected format
				for ( int x=0,i; x<REGION_SIZE; ++x )
					grid.maybes[i=y+x] = grid.cells[i].maybes = BITS9;
				return false;
			}
			Cell cell;
			String field;
			int m, i, n;
			final int offset = BY9[y]; // partial cell indice
			final Cell[] cells = grid.cells;
			for ( int x=0; x<REGION_SIZE; ++x )
				if ( (cell=cells[offset+x]).value == 0 ) {
					for ( m=i=0,field=fields[x],n=field.length(); i<n; ++i )
						m |= VSHFT[field.charAt(i) - '0'];
					grid.maybes[cell.indice] = cell.maybes = m;
				}
			return true;
		}

	}

}
