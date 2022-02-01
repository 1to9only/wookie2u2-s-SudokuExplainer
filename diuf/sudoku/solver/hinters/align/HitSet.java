/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.PERIOD;
import static diuf.sudoku.utils.Frmt.TAB;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyClass;
import diuf.sudoku.utils.MyFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A HitSet is the identifiers of puzzle, hintNumber, and aligned set of hints.
 *
 * @author Keith Corlett 2020 Mar 7
 */
public final class HitSet {

	public static String[] cellIds(Cell[] cells) {
		String[] results = new String[cells.length];
		int cnt = 0;
		for ( Cell cell : cells )
			results[cnt++] = cell.id;
		return results;
	}

	static File getHitFile(String mtFilename, AAlignedSetExclusionBase ae) {
		return new File(IO.HOME+MyFile.minusExt(mtFilename)+PERIOD+MyClass.nameOnly(ae)+".hits.txt");
	}

	public final File defaultHitsFile;
	private File file;
	private int[] any;

	// The max hints for any A*E is Aligned Quad at 1350
	//      39,561,700 16980      2,329   21   1,883,890 Aligned Pair
	//  32,297,584,300 16959  1,904,450  560  57,674,257 Aligned Triple
	// 496,548,800,600 16453 30,179,833 1350 367,813,926 Aligned Quad
	// 722,928,889,400 15251 47,402,064 1338 540,305,597 Aligned Pent
	// 485,811,887,200 14081 34,501,234 1035 469,383,465 Aligned Hex
	// 262,064,492,100 13205 19,845,853  637 411,404,226 Aligned Sept
	// 121,653,230,000 12649  9,617,616  485 250,831,402 Aligned Oct
	//  97,397,490,100 12227  7,965,771  289 337,015,536 Aligned Nona
	//  37,378,851,400 11974  3,121,667  212 176,315,336 Aligned Dec
	// So 1024 to let it grow ONCE for the big sets (FYI 128 is the minimum)
	private final Set<Hit> set = new LinkedHashSet<>(1024, 0.75F);

	/**
	 * Creates a new empty HitSet, on the default hitFile. When an actual file
	 * is loaded this changes. So the default hitFile is ONLY used to save if
	 * there are hits and hitFile has NOT been loaded, like when hackTop1465
	 * is switched-off in the GUI.
	 * @param defaultHitsFile the default *_HITS.txt for this hinter, from
	 *  the IO class.
	 */
	public HitSet(File defaultHitsFile) {
		this.file = this.defaultHitsFile = defaultHitsFile;
	}

	/**
	 * If hits are off then remember the hitsFile anyway, so that hits can be
	 * saved later, when we don't have the grid.source to get the filename.
	 *
	 * @param source
	 * @param ae
	 * @return true if set, else I use defaultHitsFile from constructor.
	 */
	void setHitFile(File hitsFile) {
		this.file = hitsFile;
	}

	/**
	 * Loads the current hitFile into this HitSet.
	 * <pre>
	 * Request For Change:
	 * if hackTop1465 in a file named top1465.d5.${classNameOnly}.hits.txt
	 * we'll store each hints:
	 * 1. gsl - grid.source.lineNumber: puzzles (1 based) lineNumber in file
	 * 2. AHint.hintNumber: the hint number (1 based) in this file
	 * 3. the id's of the cells in the aligned set
	 * and use this in the next run, to ignore all others. UBER HACK!
	 * </pre>
	 *
	 * @return true if the hitFile was loaded OK, else false already carped.
	 */
	public boolean load() {
		if ( file!=null && file.exists() )
			try ( BufferedReader rdr = new BufferedReader(new FileReader(file)) ) {
				String line;
				while ( (line=rdr.readLine()) != null )
					set.add(parse(line));
				final int n = set.size();
				// the any array should save time on each getHints call
				any = loadAnys(set, new int[n]);
				return n > 0;
			} catch (IOException ex) {
	// keep ya hair on: this is only a nicety!
	//			throw new IllegalStateException("HitSet failed to load: "+file, ex);
				Log.teeln("WARNING: HitSet.load: "+ex);
			}
		return false;
	}

	// load a hash of the grid.source.lineNumbers and hintNumbers in this.hits
	// into the given int array and return it.
	private static int[] loadAnys(Set<Hit> hits, int[] anys) {
		int cnt = 0;
		for ( Hit hit : hits )
			anys[cnt++] = hash(hit.gsl, hit.hintNum);
//		assert cnt == anys.length;
		Arrays.sort(anys); // need to sort coz I'm gonna binarySearch it
		return anys;
	}

	public boolean fileExists() {
		return file.exists();
	}

	public boolean save() {
		if ( file==null || set.isEmpty() ) // do not create empty files
			return false; // and no I did NOT save
		try ( PrintStream out = new PrintStream(file) ) {
			// turn the fooker into an array & sort that array ascending using
			// Hit's own comparator, in case multiple files have been loaded.
			// In 99.9% of usages this is useless. It covers the other 0.1%
			// which is important.
			Hit[] array = set.toArray(new Hit[set.size()]);
			Arrays.sort(array);
			for ( Hit hit : array )
				hit.println(out);
			return true;
		} catch (Exception ex) {
			StdErr.carp("Failed to save: "+file, ex);
			return false;
		}
	}

	int size() {
		return set.size();
	}

	void report() {
		Log.teef("HitSet.size=%d\n", size());
	}

	// this method only exists to ensure that all hashes are calculated the same
	// way. I've just had an instance where they were in opposite order in two
	// places calculating the same field. Some days you're a post, so put some
	// dog on it!
	// 1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768
	// 1 2 3 4  5  6  7   8   9  10   11   12   13   14    15    16
	// the max hintNumber looks like it's about 140, but that's with each
	// trailing nakedSingle counted as one hint, so 256 will do for hintNum.
	// My GSL's run upto 1465, but last A*E is in like 1140 or something.
	private static int hash(int gsl, int hintNum) {
		return (gsl<<9) ^ hintNum;
	}

	private String[] ids(int gsl, int hintNum) {
		// NOTE: it'd be faster to retain iterator coz requests occurr in same
		// order as hits appear in linked set (natural order).
		for ( Hit hit : set ) {
			if ( hit.gsl==gsl && hit.hintNum==hintNum ) {
				return hit.cellIds;
			}
		}
		return null;
	}

	Cell[] cells(int gsl, int hintNum, int degree, Grid grid) {
		if ( gsl<1 || hintNum<1 ) {
			return null;
		}
		// quickly check if there are any
		if ( Arrays.binarySearch(any, hash(gsl, hintNum)) < 0 ) {
			return null;
		}
		final String[] ids; // cell.id's in the aligned set
		if ( (ids=ids(gsl, hintNum)) == null ) {
			return null;
		}
		final Cell[] cells = new Cell[degree];
		int cnt = 0;
		for ( String id : ids )
			cells[cnt++] = grid.get(id);
		return cells;
	}

	void add(int gsl, int hintNumber, Cell[] cells) {
		set.add(new Hit(gsl, hintNumber, cellIds(cells)));
	}

	public static Hit parse(String line) {
		int tab=line.indexOf(TAB, 0);
		int gsl = Integer.parseInt(line.substring(0, tab));
		int prev=tab; tab=line.indexOf(TAB, prev+1);
		int hintNum = Integer.parseInt(line.substring(prev+1, tab));
		prev=tab;
		String[] cellIds = line.substring(prev+1).split(" *, *");
		return new Hit(gsl, hintNum, cellIds);
	}

	public static final class Hit implements Comparable<Hit> {
		public final int gsl;
		public final int hintNum;
		public final String[] cellIds;
		private Hit(int gsl, int hintNum, String[] cellIds) {
			this.gsl = gsl;
			this.hintNum = hintNum;
			this.cellIds = cellIds;
		}

		/**
		 * return the hashCode of this Hit.
		 * <pre>
		 * 1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768
		 * 1 2 3 4  5  6  7   8   9  10   11   12   13   14    15    16
		 * hits defaultSize is 2048, so the first 12 bits must be distinctive,
		 * but all I can fit in there is:
		 * * col    : the column index of the first cell in the aligned set
		 * * hintNum: the first 4 bits (ie mod 8) of the hintNumber
		 * * gsl    : the first 4 bits (ie mod 8) of the puzzleNumber
		 * I hope that it's enough to not have too many clashes. We'll see.
		 * </pre>
		 *
		 * @return
		 */
		@Override
		public int hashCode() {
			int col = (cellIds[0].charAt(0)-'A');
			int row = (cellIds[0].charAt(1)-'0');
			// row, col, hintNum, gsl
			return (((row<<4)|col)<<8)	// 8 bits
				 ^ ((hintNum&8)<<4)		// 4 bits
				 ^ (gsl&8);				// 4 bits
		}

		@Override
		public boolean equals(Object o) {
			return this == o
				|| ( o instanceof Hit
				  && equals((Hit) o) );
		}

		public boolean equals(Hit that) {
			return gsl == that.gsl
				&& hintNum == that.hintNum
				&& Arrays.deepEquals(this.cellIds, that.cellIds);
		}

		/**
		 * print a line of tab-separated-values of this Hit to 'out'.
		 * 1. gsl - grid.source.lineNumber: puzzles (1 based) lineNumber in file
		 * 2. AHint.hintNumber: the sequential index of this hint within puzzle
		 * 3. the cells in the aligned set
		 * @param out PrintStream to print to
		 */
		public void println(PrintStream out) {
			out.print(gsl);
			out.print(TAB);
			out.print(hintNum);
			out.print(TAB);
			out.print(Frmt.csv((Object[])cellIds));
			out.println();
		}

		@Override
		public String toString() {
			return ""+gsl+TAB+hintNum+TAB+Frmt.csv((Object[])cellIds);
		}

		@Override
		public int compareTo(Hit o) {
			int ret;
			if ( (ret=Integer.compare(gsl, o.gsl)) != 0 ) {
				return ret;
			}
			if ( (ret=Integer.compare(hintNum, o.hintNum)) != 0 ) {
				return ret;
			}
			assert cellIds.length == o.cellIds.length;
			for ( int i=0,n=cellIds.length; i<n; ++i ) {
				if ( (ret=cellIds[i].compareTo(o.cellIds[i])) != 0 ) {
					return ret;
				}
			}
			return 0;
		}

	}

}
