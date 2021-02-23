/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyClass;
import diuf.sudoku.utils.MyStrings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A HitSet is the identifiers of puzzle, hintNumber, and aligned set of hints.
 * @author Keith Corlett 2020 Mar 7
 */
public final class HitSet {

	/**
	 * Returns the hits File identifier.
	 * <p>Usage example: <pre>{@code
	 * useHits = hits.load(HitSet.hitFile(grid.source.file, this));
	 * where: grid.source.file = C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
	 *   and: this = diuf.sudoku.solver.hinters.align.Aligned5Exclusion_1C
	 * ==> C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.Aligned5Exclusion_1C.hits.txt
	 * }</pre>
	 * @param puzzleFileName the grid.source.fileName that we're solving.
	 * @param ane Your Aligned${n}Exclusion object (ie this)
	 * @return the hits File. Eg: top1465.d5.Aligned2Exclusion.hits.txt
	 */
	public static File hitFile(String puzzleFileName, Object ane) {
		String aneClassName = MyClass.nameOnly(ane);
		String filename = MyStrings.minus(puzzleFileName,".mt")
				+ "." + aneClassName + ".hits.txt";
		return new File(IO.HOME+filename);
	}

	public static String[] cellIds(Cell[] cells) {
		String[] results = new String[cells.length];
		int cnt = 0;
		for ( Cell cell : cells )
			results[cnt++] = cell.id;
		return results;
	}

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
	 * Creates a new empty HitSet
	 */
	public HitSet() {
	}

	/**
	 * Loads the given hitFile into this HitSet.
	 * <pre>
	 * Request For Change (RFC):
	 * if hackTop1465 in a file named top1465.d5.${classNameOnly}.hits.txt
	 * we'll store each hints:
	 * 1. gsl - grid.source.lineNumber: puzzles (1 based) lineNumber in file
	 * 2. AHint.hintNumber: the hint number (1 based) in this file
	 * 3. the id's of the cells in the aligned set
	 * and use this in the next run, to ignore all others. UBER HACK!
	 * </pre>
	 *
	 * @param hitFile the File to load and save to later.<br>
	 * Pass me: {@code HitSet.hitFile(grid.source.file, this)}<br>
	 * For example: top1465.d5.Aligned5Exclusion_1C.hits.txt
	 * @return true if the file was loaded OK, else false and carps to stderr.
	 */
	public boolean load(File hitFile) {
		this.file = hitFile;
		try ( BufferedReader rdr = new BufferedReader(new FileReader(file)) ) {
			String line;
			while ( (line=rdr.readLine()) != null )
				set.add(parse(line));
			final int n = set.size();
			// the any array should save time on each getHints call
			any = loadAnys(set, new int[n]);
			return n > 0;
		} catch (IOException ex) {
			StdErr.carp("Failed to load: "+file, ex);
			return false;
		}
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
		return save(this.file);
	}
	public boolean save(File file) {
		this.file = file;
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
		} catch (IOException ex) {
			StdErr.carp("Failed to save: "+file, ex);
			return false;
		}
	}

	int size() {
		return set.size();
	}

	void report() {
		Log.teef("HitSet.size = %d\n", size());
	}

	// this method only exists to ensure that all hashes are calculated the same
	// way. I've just had an instance where they were in opposite order in two
	// places calculating the same field. Some days you're a post, so put some
	// dog on it!
	// 1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768
	// 1 2 3 4  5  6  7   8   9  10   11   12   13   14    15    16
	// the max hintNumber looks like it's about 140, but that's with each
	// trailing nakedSingle counted as one hint, so 256 will do for hintNum.
	// My GSL's stretch to 1465, but last A*E is in like 1140 or something.
	private static int hash(int gsl, int hintNum) {
		return (gsl<<9) ^ hintNum;
	}

	private String[] getCellIds(int gsl, int hintNum) {
		//Schlemeil the Painter: it'd be faster to retain the iterator, because
		// we expect requests to occurr in the same order as the hits appear in
		// the set, because it's a linked set which is saved/retrieved in order.
		for ( Hit hit : set )
			if ( hit.gsl==gsl && hit.hintNum==hintNum )
				return hit.cellIds;
		return null;
	}

	Cell[] getHitCells(int gsl, int hintNum, int degree, Grid grid) {
		if ( gsl<1 || hintNum<1 )
			return null;
		assert gsl > 0;
		assert hintNum > 0;
		// first we quickly check if there are any, rather than creating
		// arrays and using iterators and s__t when it's not required.
		if ( Arrays.binarySearch(any, hash(gsl, hintNum)) < 0 )
			return null;
		String[] cellIds; // cellId's of cells in the aligned set
		if ( (cellIds = getCellIds(gsl, hintNum)) == null )
			return null;
		Cell[] hitCells = new Cell[degree];
		int cnt = 0;
		for ( String cellId : cellIds )
			hitCells[cnt++] = grid.get(cellId);
		return hitCells;
	}

	void add(int gsl, int hintNumber, Cell[] cells) {
		set.add(new Hit(gsl, hintNumber, cellIds(cells)));
	}

	public static Hit parse(String line) {
		int tab=line.indexOf("\t", 0);
		int gsl = Integer.parseInt(line.substring(0, tab));
		int prev=tab; tab=line.indexOf("\t", prev+1);
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
			int row = (cellIds[0].charAt(1)-'0'); // zero
			//row,col,hintNum,gsl
			return (((row<<4)|col)<<8)	// 8 bits
				 | ((hintNum&8)<<4)		// 4 bits
				 | (gsl&8);				// 4 bits
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Hit other = (Hit) obj;
			if (this.gsl != other.gsl) {
				return false;
			}
			if (this.hintNum != other.hintNum) {
				return false;
			}
			if (!Arrays.deepEquals(this.cellIds, other.cellIds)) {
				return false;
			}
			return true;
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
			out.print("\t");
			out.print(hintNum);
			out.print("\t");
			out.print(Frmt.csv((Object[])cellIds));
			out.println();
		}

		@Override
		public String toString() {
			return ""+gsl+"\t"+hintNum+"\t"+Frmt.csv((Object[])cellIds);
		}

		@Override
		public int compareTo(Hit o) {
			int gslC = Integer.compare(gsl, o.gsl);
			if ( gslC != 0 )
				return gslC;
			int hintNumC = Integer.compare(hintNum, o.hintNum);
			if ( hintNumC != 0 )
				return hintNumC;
			assert cellIds.length == o.cellIds.length;
			int idC;
			for ( int i=0,n=cellIds.length; i<n; ++i ) {
				idC = cellIds[i].compareTo(o.cellIds[i]);
				if ( idC != 0 )
					return idC;
			}
			return 0;
		}

	}

}
