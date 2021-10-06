/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.io;

import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
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
import java.util.Map;
import javax.swing.JComboBox;

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
	private static final String DEFAULT_USER_DIR
			= System.getProperty("user.home", "C:\\Users\\User")
			+ "\\Documents\\NetBeansProjects\\DiufSudoku";

	/**
	 * My home directory. The full path to the Sudoku Explainer directory.
	 * All of Sudoku Explainers files are contained in this directory.
	 * <p>
	 * I do not run (correctly) as a javascript Applet any longer, I need to be
	 * "installed" on the users PC. I suspect most users are also programmers,
	 * who can sort it out any problems for themselves.
	 */
	public static final String HOME
			= System.getProperty("user.dir", DEFAULT_USER_DIR)+"\\";

	/** The Settings for the DIUF Sudoku application. */
	public static final File SETTINGS
			= new File(HOME+"DiufSudoku_Settings.txt");

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

	/** The list of hint-regexs for SE's log-view feature. */
	public static final File LOG_VIEW_HINT_REGEXS
			= new File(HOME+"DiufSudoku_log_view_hint_regexs.txt");

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

	// =========================== reading ===============================

	/**
	 * Copy the given file to the given PrintStream. The most efficient way I
	 * can think of to deal with a long usage statement is to put in in a file
	 * which we cat when appropriate. To save some RAM.
	 * <p>
	 * NOTE: cat is a *nix command. The DOS/Windows knock-off is type.
	 *
	 * @param file to read
	 * @param out to print to
	 * @return true means it worked; false upon error (whinge to stderr)
	 */
	public static boolean cat(File file, PrintStream out) {
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			String line;
			while ( (line=reader.readLine()) != null )
				out.println(line);
		} catch (IOException ex) {
			StdErr.whinge(Log.me()+": IOException on "+file, ex);
			return false;
		}
		return true;
	}

//not_used
//	/**
//	 * Read this Reader into a List into an array of Strings.
//	 *
//	 * @param reader to read
//	 * @return {@code String[]} of the file contents
//	 * @throws java.io.FileNotFoundException
//	 */
//	public static String[] slurpArray(Reader reader) throws FileNotFoundException, IOException {
//		ArrayList<String> list = slurp(reader);
//		return list.toArray(new String[list.size()]);
//	}

//not_used
//	/**
//	 * Read this File into an array of Strings.
//	 *
//	 * @param file to read
//	 * @return {@code String[]} of the file contents
//	 * @throws java.io.FileNotFoundException
//	 */
//	public static String[] slurpArray(File file) throws FileNotFoundException, IOException {
//		ArrayList<String> list = slurp(new FileReader(file));
//		return list.toArray(new String[list.size()]);
//	}

	/**
	 * slurp reads text from the Reader into an ArrayList of Strings.
	 *
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
	 * Read this File into an ArrayList of Strings.
	 * @param file to read
	 * @return {@code ArrayList<String>} of the file contents
	 * @throws java.io.FileNotFoundException
	 */
	public static ArrayList<String> slurp(File file) throws FileNotFoundException, IOException {
		return slurp(new FileReader(file));
	}

	// asshole
	private static void slurp(Map<String,String> map, BufferedReader bufferedReader) throws IOException  {
		String line;
		int i; // indexOf('=')
		while ( (line=bufferedReader.readLine()) != null ) {
			if ( (i = line.indexOf('='))>-1 && i<=line.length() ) {
				map.put(line.substring(0, i), line.substring(i+1, line.length()));
			}
		}
	}
	// guts
	private static void slurp(Map<String,String> map, FileReader fileReader) throws IOException {
		try ( BufferedReader bufferedReader = new BufferedReader(fileReader) ) {
			slurp(map, bufferedReader);
		}
	}
	/**
	 * Load the contents of the file into the map.
	 * Each line is presumed to contain a name=value pair.
	 * There are no spaces around the '=' sign.
	 * Lines that do not contain an '=' sign are ignored.
	 * Lines that end in an '=' sign are ignored.
	 *
	 * @param map
	 * @param file
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	public static void slurp(Map<String,String> map, File file) throws FileNotFoundException, IOException {
		try ( FileReader fileReader = new FileReader(file) ) {
			slurp(map, fileReader);
		}
	}

	// asshole
	private static void slurpSB(StringBuilder sb, BufferedReader bufferedReader) throws IOException {
		String line;
		while ( (line=bufferedReader.readLine()) != null )
			sb.append(line).append(NL);
	}
	// guts
	private static void slurpSB(StringBuilder sb, FileReader fileReader) throws IOException{
		try ( BufferedReader bufferedReader = new BufferedReader(fileReader) ) {
			slurpSB(sb, bufferedReader);
		}
	}
	/**
	 * Load the file and return it all as one String.
	 * @param file the file to load: typically a HTML file, in a test-case.
	 * @return the contents of the file as a String.
	 * @throws java.io.FileNotFoundException when your cat refuses to go out
	 * @throws IOException it's your problem baby
	 */
	public static StringBuilder slurpSB(File file) throws FileNotFoundException, IOException {
		StringBuilder sb = new StringBuilder((int)file.length()>>1); // 2 bytes per char
		try ( FileReader fileReader = new FileReader(file) ) {
			IO.slurpSB(sb, fileReader);
		}
		return sb;
	}

//not_used
//	/**
//	 * Slurp filename into a new int array and returns it.
//	 * <p>
//	 * File format is: JUST 1 int per line. No commas, spaces or extraneous
//	 * crap, just digits and newlines, ie "%d\n" in 99% of cases.
//	 * @param file to load
//	 * @return a new {@code int[]} of the file contents.
//	 */
//	public static int[] slurpIntArray(File file) {
//		try {
//			String[] strings = IO.slurp(file).toArray(new String[0]);
//			return diuf.sudoku.utils.Frmt.toIntArray(strings);
//		} catch (IOException ex) {
//			StdErr.whinge("Failed to loadIntArray from: "+file, ex);
//			return null;
//		}
//	}

//not_used
//	/**
//	 * Slurp filename into this collection (nb: YOU clear c first, if necessary;
//	 * I don't so that you can load multiple files into one collection.)
//	 * @param c the {@code Collection<Integer>} to load
//	 * @param file to load
//	 * @return true if it saved, else false and carps to stderr.
//	 */
//	public static boolean slurpIntegers(Collection<Integer> c, File file) {
//		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
//			String line;
//			while ( (line=reader.readLine()) != null )
//				c.add(Integer.valueOf(line));
//			return true;
//		} catch (IOException ex) {
//			StdErr.carp("failed to load Collection<Integer> from : "+file, ex);
//			return false;
//		}
//	}

	// =========================== writing ===============================

	/**
	 * Save the given String to the given File.
	 * @param s String to save
	 * @param file to save to
	 * @throws IOException (it's your problem)
	 */
	public static void save(String s, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			writer.write(s);
		}
	}

	/**
	 * Save lines to text-file.
	 *
	 * @param lines {@code Iterable<?>} to save
	 * @param file to save to
	 * @throws IOException (it's your problem)
	 */
	public static void save(Iterable<?> lines, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( Object line : lines ) {
				writer.write(String.valueOf(line));
				writer.newLine();
			}
		}
	}

//not_used
//	/**
//	 * Save this array-of-sets-of-strings to file.
//	 *
//	 * @param sets {@code Set<String>[]} to save
//	 * @param file to save to
//	 * @throws IOException (it's your problem)
//	 */
//	public static void save(Set<String>[] sets, File file) throws IOException {
//		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
//			for ( Set<String> set : sets ) {
//				writer.write(Frmt.csvs(set));
//				writer.newLine();
//			}
//		}
//	}

	/**
	 * Save this JComboBox's contents to file, including the new item. sigh.
	 * <p>
	 * Saves the Items in the given JComboBox and ONE new entry that's been
	 * typed-in, but only one, sorry. sigh.
	 *
	 * @param cbo the JComboBox to save
	 * @param file to save to
	 * @throws IOException (it's your problem)
	 */
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

//not_used
//	/**
//	 * Save the given array-of-int to the given filename.
//	 *
//	 * @param a the array to save
//	 * @param file to save to
//	 * @return true if it saved, else false and carps to stderr.
//	 */
//	public static boolean save(int[] a, File file) {
//		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
//			for ( int e : a ) {
//				writer.write(String.valueOf(e));
//				writer.newLine();
//			}
//		} catch (IOException ex) {
//			StdErr.whinge("failed to save int[] to : "+file, ex);
//			return false;
//		}
//		return true;
//	}

	/**
	 * Save the given {@code Map<String,String> map}
	 * to file formatted as "${key}=${value}".
	 *
	 * @param map to save
	 * @param file to write
	 * @throws IOException
	 */
	public static void save(Map<String,String> map, File file) throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( Map.Entry<String,String> e : map.entrySet() ) {
				writer.write(e.getKey());
				writer.write("=");
				writer.write(e.getValue());
				writer.newLine();
			}
		}
	}

//not_used
//	// Writes the first 'n' elements of 'array' to 'writer' separated by 'sep'.
//	// For Example:
//	//   writeCsv(writer, 3, {123,324,0}, ", ")
//	//   writes: 123, 324, 0 (with a newLine)
//	//   and returns true.
//	// Where as:
//	//   writeCsv(writer, 2, {123,324,0}, ", ")
//	//   writes: 123, 324 (with a newLine)
//	//   and returns true.
//	// if array is empty then an empty line is written and true is returned.
//	// if array is null then NOTHING is written and returns false.
//	//   This will handle a fixed-size-array with trailing nulls;
//	public static boolean writeCsvLine(BufferedWriter writer, final int n, int[] array, String sep) throws IOException {
//		if ( array==null )
//			return false;
//		if ( n > 0 ) {
//			writer.write(String.valueOf(array[0]));
//			for ( int i=1; i<n; ++i ) {
//				writer.write(sep);
//				writer.write(String.valueOf(array[i]));
//			}
//		}
//		writer.newLine();
//		return true;
//	}

//not_used
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

//not_used
//	public static boolean saveIdxs(Collection<Idx> c, File file) {
//		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
//			for ( Idx idx : c ) {
//				writer.write(idx.a0);
//				writer.write(COMMA_SP);
//				writer.write(idx.a1);
//				writer.write(COMMA_SP);
//				writer.write(idx.a2);
//				writer.newLine();
//			}
//		} catch (IOException ex) {
//			StdErr.whinge("failed to saveIdxs to : "+file, ex);
//			return false;
//		}
//		return true;
//	}

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

//not_used
//	/**
//	 * Copy the given $file to $file.bak, if $file exists.
//	 * <p>There's an assert to stop you creating .bak.bak files accidentally.
//	 * @param file to create a .bak file of
//	 * @return the .bak File, or null if $file does not exist.
//	 * @throws IOException (it's your problem)
//	 */
//	public static File backup(File file) throws IOException {
//		if ( file.exists() ) {
//			assert !file.getAbsolutePath().endsWith(".bak");
//			File bak = new File(file.getAbsolutePath()+".bak");
//			save(slurp(file), bak); // Bugger Winblows and do it manually
//			return bak;
//		}
//		return null;
//	}

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
		// it works only when I hardcode it!
		DataFlavor flavor = dtde.getCurrentDataFlavors()[2];
		assert "java.awt.datatransfer.DataFlavor[mimetype=text/plain;representationclass=java.lang.String]"
				.equals(flavor.toString());
		Transferable transferable = dtde.getTransferable();
		String s = (String)transferable.getTransferData(flavor);
		dtde.dropComplete(true);
		return s;
	}

}
