/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.io;

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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
 * works then consider yourself lucky. It is open source, so write a reader
 * your-bloody-self. Lol. And I cannot wait to read your writer. Toooo bad?
 * Yeah. He went mad soweshotim. There is whole unexplored world of programming
 * humour out there.
 * <p>
 * The save format is Keiths format, which is produced by grid.toString();
 * a line of 81 cell values with an empty cell represented by a period (.)
 * character, then a line of the maybes as 81 comma separated values of just
 * the digits which the cell may be. For example:
 * <pre><tt>{@code
 * 1...9..3...2..3.5.7..8.......3..7...9.........48...6...2..........14..79...7.68..
 * ,568,456,2456,,245,247,,24678,468,689,,46,167,,1479,,14678,,3569,4569,,1256,1245,1249,12469,1246,256,156,,24569,12568,,12459,12489,12458,,1567,1567,23456,123568,12458,123457,1248,1234578,25,,,2359,1235,1259,,129,12357,34568,,145679,359,358,589,1345,146,13456,3568,3568,56,,,258,235,,,345,1359,1459,,235,,,124,12345
 * }</tt></pre>
 * <p>
 * Once upon a time, in an application far far away I moved this IO class to
 * the utils package, and removed the io package (having assimilated the
 * former little multi-class-mess). Now I am putting IO back in io (are you
 * seeing a pattern here?) because changes in the app should not effect utils,
 * they are JUST a "toolbox", they do not care how warm the water is.
 */
public final class IO {

	// users home directory (where your documents are at)
	private static final String USER_HOME = System.getProperty("user.home", "C:\\Users\\User")+"\\";

	// users documents directory (where your ScudDildoSlaver is at)
	private static final String DOCUMENTS = USER_HOME+"Documents\\";

	// The DEFAULT path to this Projects directory (my home directory).
	private static final String DEFAULT = DOCUMENTS+"NetBeansProjects\\DiufSudoku";

	/**
	 * My home directory. The full path to the Sudoku Explainer directory.
	 * All of Sudoku Explainers files are contained in this directory.
	 * <p>
	 * I do not run (correctly) as a javascript Applet any longer, I need to be
	 * "installed" on the users PC. I suspect most users are also programmers,
	 * who can sort it out any problems for themselves.
	 */
	public static final String HOME = System.getProperty("user.dir", DEFAULT)+"\\";
	public static final File HOME_DIR = new File(HOME);
	static {
		assert HOME_DIR.exists() : "Directory does not exist: "+HOME;
	}

	// The DEFAULT path to this Projects directory (my home directory).
	private static final String PUZZLES = DOCUMENTS+"SudokuPuzzles\\";

	/** Directory DIUF Sudoku saves puzzles, by default. */
	public static final File PUZZLES_DIR = new File(PUZZLES);
	static {
		assert PUZZLES_DIR.exists() : "Directory does not exist: "+PUZZLES;
	}

	/** The Config for the DIUF Sudoku application. */
	public static final File CONFIG_FILE
			= new File(HOME+"DiufSudoku_Config.txt");

	/** The LogicalSolverTester usage statement is stored in this file. */
	public static final File LOGICAL_SOLVER_TESTER_USAGE
			= new File(HOME+"DiufSudoku_LogicalSolverTester_USAGE.txt");

	/** File to store the recent files list in - a plain text file. */
	public static final File RECENT_FILES
			= new File(HOME+"DiufSudoku_RecentFiles.txt");

	/** File in which we store the generated Sudoku. */
	public static final File GENERATED_FILE
			= new File(HOME+"DiufSudoku_Generated.txt");

	/** File in which PuzzleCache stores Sudoku puzzles. */
	public static final File PUZZLE_CACHE
			= new File(HOME+"DiufSudoku_PuzzleCache.txt");

	/** The list of hint-regexs for SEs log-view feature. */
	public static final File LOG_VIEW_HINT_REGEXS
			= new File(HOME+"DiufSudoku_log_view_hint_regexs.txt");

	// =========================== reading ===============================

	/**
	 * Copy the given file to the given PrintStream. The most efficient way I
	 * can think of to deal with a long usage statement is to put in in a file
	 * which we cat when appropriate. To save some RAM.
	 * <p>
	 * NOTE: cat is a *nix command. The DOG/Dildows knock-off is type.
	 * Cats dont like dogs much, mostly, cept on Toozdeys. Fish on Frodays.
	 * Specially in the EU.
	 *
	 * @param file to read
	 * @param out to print to
	 * @return true means it worked; false upon error (whinge to stderr)
	 */
	public static boolean cat(final File file, final PrintStream out) {
		try ( final BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			String line;
			while ( (line=reader.readLine()) != null )
				out.println(line);
		} catch (IOException ex) {
			StdErr.whinge("WARN: "+Log.me()+": IOException on "+file, ex);
			return false;
		}
		return true;
	}

	/**
	 * Count the number of lines in $file.
	 *
	 * @param file to count
	 * @return number of lines
	 * @throws java.io.IOException
	 */
	public static int count(final File file) throws IOException {
		int count = 0;
		try ( final BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			while ( reader.readLine() != null )
				++count;
		}
		return count;
	}

	/**
	 * slurp reads text from the Reader into an ArrayList of Strings.
	 *
	 * @param reader to read
	 * @return {@code ArrayList<String>} of the contents of the Reader
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<String> slurp(final Reader reader) throws FileNotFoundException, IOException {
		final ArrayList<String> result = new ArrayList<>(18);
		try ( final BufferedReader theReader = (reader instanceof BufferedReader)
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
	public static ArrayList<String> slurp(final File file) throws FileNotFoundException, IOException {
		return slurp(new FileReader(file));
	}

	// for reading the config file
	private static void slurp(final Map<String,String> map, final BufferedReader bufferedReader) throws IOException  {
		String line;
		int i; // indexOf('=')
		while ( (line=bufferedReader.readLine()) != null )
			if ( !line.startsWith("#")
			  && (i=line.indexOf('='))>-1 && i<=line.length() )
				map.put(line.substring(0, i), line.substring(i+1, line.length()));
	}

	// for reading the config file
	private static void slurp(final Map<String,String> map, final FileReader fileReader) throws IOException {
		try ( final BufferedReader bufferedReader = new BufferedReader(fileReader) ) {
			slurp(map, bufferedReader);
		}
	}
	/**
	 * Load the contents of a config file into the given map.
	 * <pre>
	 * Each line is presumed to contain a name=value pair.
	 * There are no spaces around the '=' sign.
	 * Lines that do not contain an '=' sign are ignored.
	 * Lines that end in an '=' sign are ignored.
	 * Lines that start with a "#" are ignored (comments).
	 * </pre>
	 *
	 * @param map probably your Config instance (which is a Map)
	 * @param file your config file
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	public static void slurpConfig(final Map<String,String> map, final File file) throws FileNotFoundException, IOException {
		try ( final FileReader fileReader = new FileReader(file) ) {
			slurp(map, fileReader);
		}
	}

	private static StringBuilder slurpSbImpl(final StringBuilder sb, final BufferedReader bufferedReader) throws IOException {
		String line;
		while ( (line=bufferedReader.readLine()) != null )
			sb.append(line).append(NL);
		return sb;
	}

	/**
	 * Load reader into sb. Reader may be a FileReader, or a plain Reader from
	 * the system Clipboard.
	 *
	 * @param sb to read into
	 * @param reader to read
	 * @return sb for method-chaining
	 * @throws IOException when s__t happened
	 */
	public static StringBuilder slurpSB(final StringBuilder sb, final Reader reader) throws IOException{
		try ( final BufferedReader bufferedReader = new BufferedReader(reader) ) {
			return slurpSbImpl(sb, bufferedReader);
		}
	}

	/**
	 * Load the file and return it all as one String.
	 *
	 * @param file the file to load: typically a HTML file, in a test-case.
	 * @return the contents of the file as a String.
	 * @throws java.io.FileNotFoundException when your cat refuses to go out
	 * @throws IOException it is your problem baby
	 */
	public static StringBuilder slurpSB(final File file) throws FileNotFoundException, IOException {
		final StringBuilder sb = SB((int)file.length()>>1); // 2 bytes per char
		try ( FileReader fileReader = new FileReader(file) ) {
			IO.slurpSB(sb, fileReader);
		}
		return sb;
	}

	// =========================== writing ===============================

	/**
	 * Save the given String to the given File.
	 * @param s String to save
	 * @param file to save to
	 * @throws IOException (it is your problem)
	 */
	public static void save(final String s, final File file) throws IOException {
		try ( final BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			writer.write(s);
		}
	}

	/**
	 * Save lines to text-file.
	 *
	 * @param lines {@code Iterable<?>} to save
	 * @param file to save to
	 * @throws IOException (it is your problem)
	 */
	public static void save(final Iterable<?> lines, final File file) throws IOException {
		try ( final BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( Object line : lines ) {
				writer.write(String.valueOf(line));
				writer.newLine();
			}
		}
	}

	/**
	 * Save this JComboBoxs contents to file, including the new item. sigh.
	 * <p>
	 * Saves the Items in the given JComboBox and ONE new entry that is been
	 * typed-in, but only one, sorry. sigh.
	 *
	 * @param cbo the JComboBox to save
	 * @param file to save to
	 * @throws IOException (it is your problem)
	 */
	public static void save(final JComboBox<String> cbo, final File file) throws IOException {
		try ( final BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			// newbies do not appear in the items
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
	 * Save the given {@code Map<String,String> map}
	 * to file formatted as "${key}=${value}".
	 * Note that comments will be lost. Sigh.
	 *
	 * @param map to save
	 * @param file to write
	 * @throws IOException
	 */
	public static void saveConfig(final Map<String,String> map, final File file) throws IOException {
		try ( final BufferedWriter writer = new BufferedWriter(new FileWriter(file)) ) {
			for ( final Map.Entry<String,String> e : map.entrySet() ) {
				writer.write(e.getKey());
				writer.write("=");
				writer.write(e.getValue());
				writer.newLine();
			}
		}
	}

	/**
	 * Save an array of any type of Object, one line per element,
	 * relying on there toString methods for formatting.
	 *
	 * @param array to write
	 * @param n number of array elements to write
	 * @param file to write to
	 * @return success
	 */
	public static boolean save(final Object[] array, final int n, final File file) {
		try ( final PrintWriter writer = new PrintWriter(file) ) {
			for ( int i=0; i<n; ++i )
				writer.println(array[i]);
			return true;
		} catch (IOException ex) {
			Log.teeln("WARN: "+Log.me()+": "+ex);
			return false;
		}
	}

	/**
	 * Save an array of int, one line per element.
	 *
	 * @param array to write
	 * @param n number of array elements to write
	 * @param file to write to
	 * @return success
	 */
	public static boolean save(final int[] array, final int n, final File file) {
		try ( final PrintWriter writer = new PrintWriter(file) ) {
			for ( int i=0; i<n; ++i )
				writer.println(array[i]);
			return true;
		} catch (IOException ex) {
			Log.teeln("WARN: "+Log.me()+": "+ex);
			return false;
		}
	}

}
