/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.io;

import diuf.sudoku.PuzzleID;
import diuf.sudoku.Grid;
import diuf.sudoku.gui.Ask;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.Idx;
import static diuf.sudoku.utils.Frmt.COMMA;
import static diuf.sudoku.utils.Frmt.NL;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import javax.swing.JComboBox;
import static diuf.sudoku.utils.Frmt.COMMA_SP;

/**
 * Static methods to read/write Sudokus to/from files or clipboard.
 * <p>
 * The support for formats is minimal. The following plain text formats are
 * supported for reading:
 * <ul>
 * <li>A single line of 81 characters. Characters not in the range '1'..'9'
 * will be parsed as an empty cell; with an OPTIONAL second line of 81 comma
 * separated values (see example below) which will be parsed as the maybes
 * (cells potential values).
 * <li>The MagicTour format of hundreds of lines of puzzles. One per line of
 * 81 characters, as per the first line of the below example.
 * </ul>
 * <p>
 * Other formats are not supported. There is ZERO warranty. If ANY format
 * works then consider yourself lucky. It's open source, so write a reader
 * your-bloody-self. Lol. And I can't wait to read your writer. Toooo bad?
 * Yeah. He went mad soweshotim. There's whole unexplored world of programming
 * humour out there.
 * <p>
 * The save format is Keiths format, which is produced by grid.toString();
 * a line of 81 cell values with an empty cell represented by a period (.)
 * character, then a line of the maybes as 81 comma separated values of just
 * the digits which the cell may be. For example:
 * 1...9..3...2..3.5.7..8.......3..7...9.........48...6...2..........14..79...7.68..
 * ,568,456,2456,,245,247,,24678,468,689,,46,167,,1479,,14678,,3569,4569,,1256,1245,1249,12469,1246,256,156,,24569,12568,,12459,12489,12458,,1567,1567,23456,123568,12458,123457,1248,1234578,25,,,2359,1235,1259,,129,12357,34568,,145679,359,358,589,1345,146,13456,3568,3568,56,,,258,235,,,345,1359,1459,,235,,,124,12345
 * <p>
 * Once upon a time, in an application far far away I moved this IO class to
 * the 'utils' package, and removed the 'io' package (having assimilated the
 * former little multi-class-mess). Now I'm putting 'IO' back in 'io' (are
 * you seeing a pattern here?) because changes in the app shouldn't effect
 * utils, they're JUST a "toolbox", they don't care how warm the water is.
 */
public final class IO {

	// The DEFAULT path to this Projects directory (my home directory).
	private static final String DEFAULT_USER_DIR =
			System.getProperty("user.home", "C:\\Users\\User")
			+ "\\Documents\\NetBeansProjects\\DiufSudoku";

	/**
	 * My home directory. The full path to the Sudoku Explainer directory.
	 * All of Sudoku Explainers files are contained in this directory.
	 * <p>
	 * I do not run (correctly) as a javascript Applet any longer, I need to be
	 * "installed" on the users PC. I suspect most users are also programmers,
	 * who can sort it out any problems for themselves.
	 */
	public static final String HOME = System.getProperty("user.dir", DEFAULT_USER_DIR) + "\\";

	/** The LogicalSolverTester usage statement is stored in this file. */
	public static final File LOGICAL_SOLVER_TESTER_USAGE
			= new File(HOME+"LogicalSolverTester_USAGE.txt");

	/** File to store the recent files list in - a plain text file */
	public static final File RECENT_FILES
			= new File(HOME+"DiufSudoku_RecentFiles.txt");

	/** File in which we store the generated Sudoku. */
	public static final File GENERATED_FILE
			= new File(HOME+"DiufSudoku_Generated.txt");

	/** File in which PuzzleCache stores Sudoku puzzles. */
	public static final File PUZZLE_CACHE
			= new File(HOME+"DiufSudoku_PuzzleCache.txt");

	/** File in which we store AHinter performance statistics. */
	public static final File PERFORMANCE_STATS
			= new File(HOME+"DiufSudoku_Stats.txt");

// -REDO buggers-up HoDOKu somehow and I'm too lazy to even attempt to fix it!
//	/** The VERBOSE_5_MODE logFile of a previous "keeper" run from which we
//	 * parse the hinters to use in each subsequent -REDO run.
//	 * mode: ACCURACY !REDO !STATS */
//	public static final File KEEPER_LOG
//			= new File(HOME+"top1465.d5.2020-08-06.22-21-31.keep.log");

	/** A*E records which puzzles it hints on. */
	public static final File A2E_HITS = new File(HOME+"DiufSudoku_A2E_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A3E_HITS = new File(HOME+"DiufSudoku_A3E_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A4E_HITS = new File(HOME+"DiufSudoku_A4E_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A5E_1C_HITS = new File(HOME+"DiufSudoku_A5E_1C_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A6E_1C_HITS = new File(HOME+"DiufSudoku_A6E_1C_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A7E_1C_HITS = new File(HOME+"DiufSudoku_A7E_1C_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A8E_1C_HITS = new File(HOME+"DiufSudoku_A8E_1C_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A9E_1C_HITS = new File(HOME+"DiufSudoku_A9E_1C_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A10E_1C_HITS = new File(HOME+"DiufSudoku_A10E_1C_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A5E_2H_HITS = new File(HOME+"DiufSudoku_A5E_2H_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A6E_2H_HITS = new File(HOME+"DiufSudoku_A6E_2H_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A7E_2H_HITS = new File(HOME+"DiufSudoku_A7E_2H_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A8E_2H_HITS = new File(HOME+"DiufSudoku_A8E_2H_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A9E_2H_HITS = new File(HOME+"DiufSudoku_A9E_2H_hits.txt");
	/** A*E records which puzzles it hints on. */
	public static final File A10E_2H_HITS = new File(HOME+"DiufSudoku_A10E_2H_hits.txt");

	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A2E_WINDEX = new File(HOME+"DiufSudoku_A2E_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A3E_WINDEX = new File(HOME+"DiufSudoku_A3E_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A4E_WINDEX = new File(HOME+"DiufSudoku_A4E_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A5E_1C_WINDEX = new File(HOME+"DiufSudoku_A5E_1C_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A5E_2H_WINDEX = new File(HOME+"DiufSudoku_A5E_2H_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A6E_1C_WINDEX = new File(HOME+"DiufSudoku_A6E_1C_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A6E_2H_WINDEX = new File(HOME+"DiufSudoku_A6E_2H_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A7E_1C_WINDEX = new File(HOME+"DiufSudoku_A7E_1C_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A7E_2H_WINDEX = new File(HOME+"DiufSudoku_A7E_2H_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A8E_1C_WINDEX = new File(HOME+"DiufSudoku_A8E_1C_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A8E_2H_WINDEX = new File(HOME+"DiufSudoku_A8E_2H_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A9E_1C_WINDEX = new File(HOME+"DiufSudoku_A9E_1C_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A9E_2H_WINDEX = new File(HOME+"DiufSudoku_A9E_2H_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A10E_1C_WINDEX = new File(HOME+"DiufSudoku_A10E_1C_windex.txt");
	/** A*E stores an index of the cells in each aligned set which hints. */
	public static final File A10E_2H_WINDEX = new File(HOME+"DiufSudoku_A10E_2H_windex.txt");

	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A2E_HASHCODES = new File(HOME+"DiufSudoku_A2E_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A3E_HASHCODES = new File(HOME+"DiufSudoku_A3E_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A4E_HASHCODES = new File(HOME+"DiufSudoku_A4E_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A5E_1C_HASHCODES = new File(HOME+"DiufSudoku_A5E_1C_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A5E_2H_HASHCODES = new File(HOME+"DiufSudoku_A5E_2H_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A6E_1C_HASHCODES = new File(HOME+"DiufSudoku_A6E_1C_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A6E_2H_HASHCODES = new File(HOME+"DiufSudoku_A6E_2H_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A7E_1C_HASHCODES = new File(HOME+"DiufSudoku_A7E_1C_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A7E_2H_HASHCODES = new File(HOME+"DiufSudoku_A7E_2H_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A8E_1C_HASHCODES = new File(HOME+"DiufSudoku_A8E_1C_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A8E_2H_HASHCODES = new File(HOME+"DiufSudoku_A8E_2H_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A9E_1C_HASHCODES = new File(HOME+"DiufSudoku_A9E_1C_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A9E_2H_HASHCODES = new File(HOME+"DiufSudoku_A9E_2H_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A10E_1C_HASHCODES = new File(HOME+"DiufSudoku_A10E_1C_hash.txt");
	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A10E_2H_HASHCODES = new File(HOME+"DiufSudoku_A10E_2H_hash.txt");

	/** The list of hint-regexs for SE's log-view feature. */
	public static final File LOG_VIEW_HINT_REGEXS = new File(HOME+"DiufSudoku_log_view_hint_regexs.txt");

//	private static final String SUPPORTED_DATA_FLAVOR_NAME
//			= "java.awt.datatransfer.DataFlavor[mimetype=text/plain;representationclass=java.lang.String]";

	// =========================== reading ===============================

	public static boolean exists(String filename) {
		return new File(filename).exists();
	}

	/**
	 * Load this puzzle: ie loads lineNumber of File into this Grid.
	 * @param grid to read into
	 * @param file to read from
	 * @param lineNumber of puzzle to read
	 * @return true if it loaded, else false (and see standard error).
	 */
	public static boolean load(Grid grid, File file, int lineNumber) {
		String filename = file.getName().toLowerCase();
		if ( filename.endsWith(".txt") )
			return loadTxt(grid, file);
		if ( filename.endsWith(".mt") )
			return loadMT(grid, file, lineNumber);
		return StdErr.whinge("Unrecognised file format: "+file);
	}

	private static boolean loadTxt(Grid grid, File file) {
		try {
			ArrayList<String> lines = IO.slurp(file);
			if ( lines.get(0).length() == 81 )
				// toString format: delegate back to the grid
				grid.load(lines); // sets grid.isMaybesLoaded internally
			else {
				// presume old format: 9 lines of 9 values
				//                   + 9 lines of 9 csv'ed maybes
				// most existing files are in this format. No plan to convert.
				for ( int y=0; y<9; ++y ) // read cell values
					setClues(grid, lines.get(y), y);
				if ( lines.size() >= 18 ) {
					grid.isMaybesLoaded = true;
					for ( int y=0; y<9; ++y ) // read maybes
						if ( !setMaybes(grid, lines.get(y+9), y) ) {
							grid.isMaybesLoaded = false;
							break; // they'll all be splattered anyway
						}
				}
			}
			grid.source = new PuzzleID(file, 0);
		} catch (Exception ex) {
			return StdErr.whinge(ex);
		}
		return true;
	}

	private static void setClues(Grid grid, String line, int y) {
		//assert line.matches("[1..9]{9}");
		for ( int x=0; x<9; ++x ) {
			final char ch = line.charAt(x);
			if ( ch>='1' && ch<='9' )
				grid.cells[y*9+x].set(ch-'0', 0, false, null); // NB: x,y (ie col,row)
		}
	}

	private static boolean setMaybes(Grid grid, String line, int y) {
		y *= 9; // only used to calculate a cell indice
		String[] fields = line.split(COMMA, 9);
		if ( fields.length == 9 ) {
			Grid.Cell cell;
			for ( int x=0; x<9; ++x ) {
				if ( (cell=grid.cells[y+x]).value == 0 ) {
					String field = fields[x];
					for ( int i=0, n=field.length(); i<n; ++i )
						cell.maybes.add(field.charAt(i)-'0');
				}
			}
			return true;
		}
		// unexpected input file format
		for ( int x=0; x<9; ++x )
			grid.cells[y+x].maybes.fill();
		return false;
	}

	private static final String QUESTION =
			"MagicTour (*.mt) is a multi-line format."+NL
			+"So which line (1 based) do you want?";

	private static boolean loadMT(Grid grid, File file, int lineNumberArg) {
		try {
			final int lineCount = countLines(file);
			final int lineNumber = lineNumberArg>0 && lineNumberArg<=lineCount
					? lineNumberArg
					: Ask.forInt(QUESTION, 1, lineCount);
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				for ( int i=1; i<lineNumber; ++i )
					reader.readLine();
				String line = reader.readLine();
				if ( line==null || line.length()<81 )
					throw new IOException("Line number "+lineNumber+" is not"
							+ " atleast 81 characters. Line:"+NL+line);
				char c;
				for ( int i=0; i<81; ++i ) {
					c = line.charAt(i);
					grid.cells[i].set(c>='1'&&c<='9' ? c-'0' : 0, 0, false, null);
				}
			} catch (Exception ex) {
				StdErr.whinge("failed to loadMT from: "+lineNumber+"#"+file, ex);
			}
			grid.isMaybesLoaded = false;
			grid.source = new PuzzleID(file, lineNumber);
			return true;
		} catch (IOException ex) {
			return StdErr.whinge(ex);
		}
	}

	/**
	 * Counts the number of lines (1 based) in the given file.
	 * @param file to count lines of/in (I never know).
	 * @return the 1 based number of lines in this file. WARN: maybe 0.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	static int countLines(File file) throws FileNotFoundException, IOException {
		int lineCount = 0;
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			while ( reader.readLine() != null )
				++lineCount;
		}
		return lineCount;
	}

	/**
	 * Copy the given file to the given PrintStream. The most efficient way
	 * I can think of to deal with a long usage statement is to put in in
	 * a file which we cat the stderr when appropriate. To save some RAM.
	 * <p>cat is a *nix command. The DOS/Windows poxivalent is type.<br>
	 * NB: Poxivalence is best defined as "of or relating to Bill."
	 *
	 * @param file File to read.
	 * @param out PrintStream to print to.
	 * @return true means it worked; false upon error (and carp to stderr)
	 */
	public static boolean cat(File file, PrintStream out) {
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			String line;
			while ( (line=reader.readLine()) != null )
				out.println(line);
		} catch (IOException ex) {
			StdErr.whinge("Failed to cat: "+file, ex);
			return false;
		}
		return true;
	}

	/**
	 * Read this Reader into a List into an array of Strings.
	 * @param reader to read
	 * @return {@code String[]} of the file contents
	 * @throws java.io.FileNotFoundException
	 */
	public static String[] slurpArray(Reader reader) throws FileNotFoundException, IOException {
		ArrayList<String> list = slurp(reader);
		return list.toArray(new String[list.size()]);
	}

	/**
	 * slurp reads text from the Reader into an ArrayList of Strings.
	 * @param reader to read
	 * @return {@code ArrayList<String>} of the contents of the Reader
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<String> slurp(Reader reader) throws FileNotFoundException, IOException {
		ArrayList<String> result = new ArrayList<>(18);
		try ( BufferedReader theReader = (reader instanceof BufferedReader)
				? (BufferedReader)reader : new BufferedReader(reader) ) {
			String line;
			while ( (line=theReader.readLine()) != null )
				result.add(line);
		}
		return result;
	}

	/**
	 * Read this File into an array of Strings.
	 * @param file to read
	 * @return {@code String[]} of the file contents
	 * @throws java.io.FileNotFoundException
	 */
	public static String[] slurpArray(File file) throws FileNotFoundException, IOException {
		ArrayList<String> list = slurp(new FileReader(file));
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Read this File into an ArrayList of Strings.
	 * @param file to read
	 * @return {@code ArrayList<String>} of the file contents
	 * @throws java.io.FileNotFoundException
	 */
	public static ArrayList<String> slurp(File file) throws FileNotFoundException, IOException {
		return slurp(new FileReader(file));
	}

	/**
	 * Loads filename into a new int array and returns it.
	 * <p>
	 * File format is: JUST 1 int per line. No commas, spaces or extraneous
	 * crap, just digits and newlines, ie "%d\n" in 99% of cases.
	 * @param filename the name of the file to load
	 * @return a new {@code int[]} of the file contents.
	 */
	public static int[] loadIntArray(String filename) {
		return loadIntArray(new File(filename));
	}
	public static int[] loadIntArray(File file) {
		try {
			String[] strings = IO.slurp(file).toArray(new String[0]);
			return diuf.sudoku.utils.Frmt.toIntArray(strings);
		} catch (IOException ex) {
			StdErr.whinge("Failed to loadIntArray from: "+file, ex);
			return null;
		}
	}

	/**
	 * Load filename into this collection (nb: YOU clear c first, if necessary;
	 * I don't so that you can load multiple files into one collection.)
	 * @param c the {@code Collection<Integer>} to load
	 * @param filename the name of the file to load;<br>
	 * <b>or</b> param File file to load
	 * @return true if it saved, else false and carps to stderr.
	 */
	public static boolean loadIntegers(Collection<Integer> c, String filename) {
		return IO.loadIntegers(c, new File(filename));
	}
	public static boolean loadIntegers(Collection<Integer> c, File file) {
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			String line;
			while ( (line=reader.readLine()) != null )
				c.add(Integer.valueOf(line));
			return true;
		} catch (IOException ex) {
			StdErr.carp("failed to load Collection<Integer> from : "+file, ex);
			return false;
		}
	}

//	public static boolean loadIntegerArrays(Collection<int[]> c, File file) {
//		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
//			String line;
//			while ( (line=reader.readLine()) != null )
//				// toIntArray parses a String[] into an int[]
//				c.add(Frmt.toIntArray(line.split(" *, *")));
//			return true;
//		} catch (IOException ex) {
//			StdErr.carp("failed to load Collection<int[]> from : "+file, ex);
//			return false;
//		}
//	}

	// =========================== writing ===============================

	/**
	 * Save the given String to the given File.
	 * @param s String to save
	 * @param file to save to
	 * @throws IOException (it's your problem baby.)
	 */
	public static void save(String s, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			writer.write(s);
		}
	}

	/**
	 * Save the given String to the given File.
	 * @param lines {@code Collection<String>} to save
	 * @param file to save to
	 * @throws IOException (it's your problem baby.)
	 */
	public static void save(Collection<String> lines, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( String line : lines ) {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	/**
	 * Save the given array-of-sets-of-strings to the given File.
	 * @param sets {@code Set<String>[]} to save
	 * @param file to save to
	 * @throws IOException (it's your problem baby.)
	 */
	public static void save(Set<String>[] sets, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( Set<String> set : sets ) {
				writer.write(Frmt.csvs(set));
				writer.newLine();
			}
		}
	}

	public static void save(JComboBox<String> cbo, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			// newbies don't appear in the items
			String newbie;
			if ( cbo.getSelectedIndex() == -1 // not in the list
			  && (newbie=(String)cbo.getSelectedItem()) != null
			  && !newbie.isEmpty() ) {
				writer.write(newbie);
				writer.newLine();
			}
			// the existing items
			for ( int i=0,n=cbo.getItemCount(); i<n; ++i ) {
				writer.write(cbo.getItemAt(i));
				writer.newLine();
			}
		}
	}

	/**
	 * Save the given array-of-int to the given filename.
	 * @param a the array to save
	 * @param filename the name of the file to save to;<br>
	 * <b>or</b> param File file to save to
	 * @return true if it saved, else false and carps to stderr.
	 */
	public static boolean save(int[] a, String filename) {
		return save(a, new File(filename));
	}
	public static boolean save(int[] a, File file) {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( int e : a ) {
				writer.write(String.valueOf(e));
				writer.newLine();
			}
		} catch (IOException ex) {
			StdErr.whinge("failed to save int[] to : "+file, ex);
			return false;
		}
		return true;
	}

	/**
	 * Save these Integers to the given file/name
	 * @param c the {@code Collection<Integer>} to save
	 * @param filename the name of the file to save;<br>
	 * <b>or</b> param File file to save to
	 * @return true if it saved, else false and carps to stderr.
	 */
	public static boolean saveIntegers(Collection<Integer> c, String filename) {
		return saveIntegers(c, new File(filename));
	}
	public static boolean saveIntegers(Collection<Integer> c, File file) {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			write(c, System.lineSeparator(), writer);
		} catch (IOException ex) {
			StdErr.whinge("failed to saveIntegers to : "+file, ex);
			return false;
		}
		return true;
	}
	// Writes c to writer separated by sep's.
	// NB: sep is always printed after each element (ie I use lineSeperator)
	private static void write(Collection<?> c, String sep, BufferedWriter writer) throws IOException {
		for ( Object e : c ) {
			writer.write(String.valueOf(e));
			writer.write(sep);
		}
	}

	// Writes the first 'n' elements of 'array' to 'writer' separated by 'sep'.
	// For Example:
	//   writeCsv(writer, 3, {123,324,0}, ", ")
	//   writes: 123, 324, 0 (with a newLine)
	//   and returns true.
	// Where as:
	//   writeCsv(writer, 2, {123,324,0}, ", ")
	//   writes: 123, 324 (with a newLine)
	//   and returns true.
	// if array is empty then an empty line is written and true is returned.
	// if array is null then NOTHING is written and returns false.
	//   This will handle a fixed-size-array with trailing nulls;
	private static boolean writeCsvLine(BufferedWriter writer, final int n, int[] array, String sep) throws IOException {
		if ( array==null )
			return false;
		if ( n > 0 ) {
			writer.write(String.valueOf(array[0]));
			for ( int i=1; i<n; ++i ) {
				writer.write(sep);
				writer.write(String.valueOf(array[i]));
			}
		}
		writer.newLine();
		return true;
	}

//	/**
//	 * Saves the {@code Collection<int[]>} to 'file'. Each {@code int[]} is
//	 * written one per line, in csv format.
//	 * <p>For example:<pre>{@code
//	 * 264708, 524290, 524288
//	 * 0, 525365, 524288
//	 * 0, 245816, 0
//	 * }</pre>
//	 * <p>any empty arrays appear as an empty line.
//	 * <p>any null arrays are ignored.
//	 * @param c the {@code Collection<int[]>} to save
//	 * @param file the file to save to
//	 * @return true unless there was an IOException which was written to stderr.
//	 */
//	public static boolean saveIntegerArrays(Collection<int[]> c, File file) {
//		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
//			for ( int[] array : c )
//				writeCsvLine(writer, array.length, array, ", ");
//		} catch (IOException ex) {
//			StdErr.whinge("failed to saveIntegerArrays to : "+file, ex);
//			return false;
//		}
//		return true;
//	}

	public static boolean saveIdxs(Collection<Idx> c, File file) {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( Idx idx : c ) {
				writer.write(idx.a0);
				writer.write(COMMA_SP);
				writer.write(idx.a1);
				writer.write(COMMA_SP);
				writer.write(idx.a2);
				writer.newLine();
			}
		} catch (IOException ex) {
			StdErr.whinge("failed to saveIdxs to : "+file, ex);
			return false;
		}
		return true;
	}

//	/**
//	 * Sort these Integers numerically, then save them to filename.
//	 * @param c the {@code Collection<Integer>} to sort and save
//	 * @param filename the name of the file to save;<br>
//	 * <b>or</b> param File file to save to
//	 * @return true if it saved, else false and carps to stderr.
//	 */
//	public static boolean saveSorted(Collection<Integer> c, String filename) {
//		return saveSorted(c, new File(filename));
//	}
//	public static boolean saveSorted(Collection<Integer> c, File file) {
//		return save(MyArrays.sort(MyArrays.unbox(c)), file);
//	}

	/**
	 * Copy the given $file to $file.bak, if $file exists.
	 * <p>There's an assert to stop you creating .bak.bak files accidentally.
	 * @param file to create a .bak file of
	 * @return the .bak File, or null if $file does not exist.
	 * @throws IOException (it's your problem baby.)
	 */
	public static File backup(File file) throws IOException {
		if ( file.exists() ) {
			assert !file.getAbsolutePath().endsWith(".bak");
			File bak = new File(file.getAbsolutePath()+".bak");
			save(slurp(file), bak); // Bugger Winblows and do it manually
			return bak;
		}
		return null;
	}

	// =========================== clipboard ===============================

	/**
	 * Copies the given String to Systems clipboard.
	 * @param s string to copy to clipboard
	 */
	public static void copyToClipboard(String s) {
		StringSelection ss = new StringSelection(s);
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard clipboard = tk.getSystemClipboard();
		clipboard.setContents(ss, ss);
	}

	/**
	 * Reads the String content of the drop event and returns it.
	 * @param dtde the {@code DropTargetDropEvent} received by
	 *  {@code DropTargetAdapter.drop}
	 * @return java.lang.String contents of the drop event.
	 * @throws UnsupportedFlavorException
	 * @throws IOException
	 */
	public static String readStringFromDropEvent(DropTargetDropEvent dtde)
			throws UnsupportedFlavorException, IOException {
		dtde.acceptDrop(DnDConstants.ACTION_COPY);
//<CRAP_RAY @todo="research how to get the DataFlavour properly">
//		DataFlavor flavor = null;
//		for ( DataFlavor df : dtde.getCurrentDataFlavors() )
//			if ( SUPPORTED_DATA_FLAVOR_NAME.equals(df.toString()) ) {
//				flavor = df;
//				break;
//			}
//		if ( flavor == null )
//			throw new UnsupportedDataTypeException("transferable: "+dtde.getTransferable().toString());
		//
		DataFlavor flavor = dtde.getCurrentDataFlavors()[2]; // it works only when I hardcode it!
		assert "java.awt.datatransfer.DataFlavor[mimetype=text/plain;representationclass=java.lang.String]"
				.equals(flavor.toString());
//</CRAP_RAY>
		Transferable transferable = dtde.getTransferable();
		String s = (String)transferable.getTransferData(flavor);
		dtde.dropComplete(true);
		return s;
	}

}
