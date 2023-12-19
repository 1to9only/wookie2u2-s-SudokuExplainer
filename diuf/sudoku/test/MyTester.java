package diuf.sudoku.test;

import static diuf.sudoku.test.TestHelp.*;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;
import static java.util.Calendar.*;

import diuf.sudoku.*;
import static diuf.sudoku.Constants.F;
import static diuf.sudoku.Constants.T;
import diuf.sudoku.Grid.*;
import static diuf.sudoku.Grid.BY9;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.io.IO;
import diuf.sudoku.solver.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.utils.*;

import java.io.*;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import static diuf.sudoku.Grid.SQRT;
import diuf.sudoku.Idx.MyIntQueue;
import diuf.sudoku.io.StdErr;
import static java.lang.Integer.bitCount;
import static java.lang.Integer.parseInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
//import sun.security.action.GetPropertyAction;
//import sudoku.SudokuSet;


/**
 * I use this class just to find-out stuff. It is not referenced elsewhere in
 * the project, but I import whatever classes I want, and leave them there...
 * so do NOT Ctrl-Shift-I.
 * <p>
 * I copy-paste and comment-out the previous main method, and leave it there as
 * a reference; I delete only the majorly embarrassing stuff.
 *
 * @author Keith Corlett 2013
 */

////   copyTo 1,000,000  45,510,306,408
////  copyCon 1,000,000  43,673,480,777
////    total     7,395

public final class MyTester {

//	private static final int HOW_MANY = 1000 * 1000;
//	public static void main(String[] args) {
//		Grid grid = new Grid();
//		ErrMsg msg = IO.load(grid, new File("C:/Users/User/Documents/SudokuPuzzles/MagicTour/top87.mt"), 2);
//		if ( msg != null ) {
//			System.out.println("ERROR: "+msg);
//			return;
//		}
//
//		long start, finish;
//
//		long totalA[] = new long[HOW_MANY];
//		start = System.nanoTime();
//		for ( int i=0; i<HOW_MANY; ++i ) {
//			Grid copy = new Grid();
//			grid.copyTo(copy);
//			long ttl = 0;
//			for ( char c : copy.toString().toCharArray() )
//				ttl += c;
//			totalA[i] = ttl;
//			if ( i == 0 )
//				for (int r=0; r<27; ++r)
//					for (int v=1; v<10; ++v)
//						assert grid.regions[r].places[v] = copy.regions[r].places[v];
//		}
//		finish = System.nanoTime();
//		System.out.format("%9s %,9d %,15d\n", "copyTo", HOW_MANY, finish-start);
//
//		long totalB[] = new long[HOW_MANY];
//		start = System.nanoTime();
//		for ( int i=0; i<HOW_MANY; ++i ) {
//			Grid copy = new Grid(grid);
//			long ttl = 0;
//			for ( char c : copy.toString().toCharArray() )
//				ttl += c;
//			totalB[i] = ttl;
//			if ( i == 0 )
//				for (int r=0; r<27; ++r)
//					for (int v=1; v<10; ++v)
//						assert grid.regions[r].places[v] = copy.regions[r].places[v];
//		}
//		finish = System.nanoTime();
//		System.out.format("%9s %,9d %,15d\n", "copyCon", HOW_MANY, finish-start);
//
//		assert totalB[0] == totalA[0];
//		for ( int i=1; i<HOW_MANY; ++i ) {
//			assert totalA[i] == totalA[0];
//			assert totalB[i] == totalB[0];
//		}
//		System.out.format("%9s %,9d\n", "total", totalA[0]);
//	}
//}

//import diuf.sudoku.Indexes;
//import diuf.sudoku.tools.Interator;
//class MyTester {
//	//How fast is an interator compared to foreach-get
//	//HOW_MANY = 10
//	//      get 3          34,362
//	//interator 2       2,021,765
//	//HOW_MANY = 1,000,000
//	//      get 3     150,835,302
//	//interator 2     823,098,885
//	static final int HOW_MANY = 1000 * 1000;
//	public static void main(String[] args) {
//		Indexes three = Indexes.of(0, 6, 8);
//
//		int count = 0;
//		long start = System.nanoTime();
//		for ( int i=0; i<HOW_MANY; ++i )
//			for ( int j=0; j<9; ++j )
//				if ( three.get(j) )
//					++count;
//		long finish = System.nanoTime();
//		System.out.format("%9s %d %,15d\n", "get", count/HOW_MANY, finish-start);
//
//		count = 0;
//		start = System.nanoTime();
//		for ( int i=0; i<HOW_MANY; ++i )
//			for ( Interator it=three.interator(); it.hasNext(); ) {
//				int v = it.next();
//				++count;
//			}
//		finish = System.nanoTime();
//		System.out.format("%9s %d %,15d\n", "interator", count/HOW_MANY, finish-start);
//	}
//}

//	public static void main(String[] args) {
//		String et = true ? "phone home" : msg();
//		System.out.println(et);
//	}
//	private static String msg() {
//		System.out.println("I was here");
//		return "Well ____ Me Sideways";
//	}

//	public static void main(String[] args) {
//		Ass a = null;
//		boolean isInstance = a instanceof Ass;
//		System.out.println("isInstance="+isInstance);
//	}
//	//isInstance=false

//	static final int HOW_MANY = 100 * 1000;
//	public static void main(String[] args) {
//		Indexes indexes = new Indexes(0, 4, 5, 6, 8);
//		int total = 0;
//		long start = System.nanoTime();
//		for ( int i=0; i<HOW_MANY; ++i )
//			total += indexes.cardinality();
//		long finish = System.nanoTime();
//		System.out.println("cardinality()  "+(finish-start));
//
//		total = 0;
//		start = System.nanoTime();
//		for ( int i=0; i<HOW_MANY; ++i )
//			total += indexes.cardinality(2);
//		finish = System.nanoTime();
//		System.out.println("cardinality(2) "+(finish-start));
//	}

//	public static void main(String[] args) {
//		Values v = new Values("479");
//		System.out.println(v.nth(0));
//		System.out.println(v.nth(1));
//		System.out.println(v.nth(2));
//		System.out.println(v.nth(3));
//	}

//	public static void main(String[] args) {
//		for ( int nBits=0; nBits<=9; ++nBits ) {
//			for ( int nOnes=0; nOnes<=nBits; ++nOnes )
//				new Permutations(nOnes, nBits).printBinaryString(System.out);
//			System.out.println();
//		}
//	}

// Q: Performance test caching of card=idxOf[v].size
//	public static void main(String[] args) {
//		Grid grid = new Grid();
//		File file = new File("C:\\Users\\User\\Documents\\SudokuPuzzles\\MagicTour\\top1465.mt");
//		ErrMsg msg = IO.load(grid, file, 205);
//		if ( msg != null ) {
//			JOptionPane.showMessageDialog(null, msg.toString(), "Oops!", msg.getType());
//			return;
//		}
//
//		Region[] r1s = grid.cols;
//		int v = 7;
//		int dd = 4; // 2 Swampfish, 3 Swordfish, 4 Jellyfish
//		int[] candiIdxs = new int[9]; // candidate indexes
//		int n;
//
//		final int TIMES = 1000;
//		//final int TIMES = 1000*1000;
//		//final int TIMES = 10*1000*1000;
//
//		long t0 = System.nanoTime();
//		for ( int times=0; times<TIMES; ++times ) {
//			n = 0;
//			for ( int i=0; i<9; ++i ) // 9*9 = 81
//				if ( r1s[i].numPlaces[v]>1 && r1s[i].numPlaces[v]<=dd )
//					candiIdxs[n++] = i;
//		}
//
//		long t1 = System.nanoTime();
//		for ( int times=0; times<TIMES; ++times ) {
//			n = 0;
//			for ( int i=0; i<9; ++i ) { // 9*9 = 81
//				final int card = r1s[i].numPlaces[v];
//				if ( card>1 && card<=dd )
//					candiIdxs[n++] = i;
//			}
//		}
//
//		long t2 = System.nanoTime();
//
//		System.out.println("lazy="+(t1-t0)+"\tcard="+(t2-t1));
//		//         1000:	lazy=   2515054		card=   5393283
//		//         1000:	lazy=   2477409		card=   4977518
//		//    1000*1000:	lazy= 348475225		card= 233338455
//		// 10*1000*1000:	lazy=2790451370		card=2829273134
//
//	}

//	public static final String DIR = "C:\\Users\\User\\Documents\\SudokuPuzzles\\MagicTour\\";
//	public static void main(String[] args) {
//		final String filepath = DIR+"top1465.descending.mt";
//		final String logFilepath = filepath.replaceFirst("\\.mt$", ".log");
//		System.out.println(logFilepath);
//	}

//	public static void main(String[] args) {
//		int i = 0xf;
//		System.out.format("%d\t%s\n", i, Integer.toBinaryString(i));
//	}

// Q: Performance test ways of implementing Indexes set method.
//	public static void main(String[] args) {
//		//final int TIMES = 1000;
//		//final int TIMES = 1000*1000;
//		final int TIMES = 10*1000*1000;
//
//		long t0 = System.nanoTime();
//		Indexes idx0 = new Indexes();
//		for ( int times=0; times<TIMES; ++times ) {
//			for ( int i=0; i<9; ++i ) // 9*9 = 81
//				idx0.set(i, true);
//			for ( int i=0; i<9; i+=2 ) // 9*9 = 81
//				idx0.set(i, false);
//		}
//
//		long t1 = System.nanoTime();
//		Indexes idx1 = new Indexes();
//		for ( int times=0; times<TIMES; ++times ) {
//			for ( int i=0; i<9; ++i ) // 9*9 = 81
//				idx1.set2(i, true);
//			for ( int i=1; i<9; i+=2 ) // 9*9 = 81
//				idx1.set2(i, false);
//		}
//
//		long t2 = System.nanoTime();
//
//		//System.out.println(idx0.toString());
//		//System.out.println(idx1.toString());
//		System.out.println(""+TIMES+"\t\tset=   "+(t1-t0)+"\tset2=  "+(t2-t1));
//		// 1357
//		// 02468
//		//     1000		set=	  2578610	set2=	  2106832
//		//  1000000		set=	147712139	set2=	214880104
//		// 10000000		set=   1744834539	set2=  2255317785
//		// 10000000		set=   1577800740	set2=  2377333712
//		// 10000000		set=   1639320780	set2=  2406177740
//		// modified set
//		// 10000000		set=   1982113928	set2=  2634153083
//		// 10000000		set=   2006108458	set2=  2429153636
//		// 10000000		set=   1909842941	set2=  2424739178
//		// reverted
//		// 10000000		set=   1958138115	set2=  2565424097
//		// 10000000		set=   2018495507	set2=  2556229353
//		// 10000000		set=   2007604388	set2=  2565091583
//	}
//	public Indexes set2(int i, boolean isOn) {
//		if ( isOn )
//			bits |= SHFT[i];
//		else
//			bits &= ~SHFT[i];
//		this.size = Integer.bitCount(bits);
//		return this;
//	}

// Q: Performance test ways of implementing Indexes set method.
//	public static void main(String[] args) {
//		//final int TIMES = 1000;
//		//final int TIMES = 1000*1000;
//		final int TIMES = 10*1000*1000;
//
//		long t0 = System.nanoTime();
//		Indexes idx0 = new Indexes();
//		for ( int times=0; times<TIMES; ++times ) {
//			for ( int i=0; i<9; ++i ) // 9*9 = 81
//				idx0.set(i, true);
//			for ( int i=0; i<9; i+=2 ) // 9*9 = 81
//				idx0.set(i, false);
//		}
//
//		long t1 = System.nanoTime();
//		Indexes idx1 = new Indexes();
//		for ( int times=0; times<TIMES; ++times ) {
//			for ( int i=0; i<9; ++i ) // 9*9 = 81
//				idx1.set2(i, true);
//			for ( int i=1; i<9; i+=2 ) // 9*9 = 81
//				idx1.set2(i, false);
//		}
//
//		long t2 = System.nanoTime();
//
//		//System.out.println(idx0.toString());
//		//System.out.println(idx1.toString());
//		System.out.println(""+TIMES+"\t\tset=   "+(t1-t0)+"\tset2=  "+(t2-t1));
//		// 1357
//		// 02468
//		//     1000		set=	  2578610	set2=	  2106832
//		//  1000000		set=	147712139	set2=	214880104
//		// 10000000		set=   1744834539	set2=  2255317785
//		// 10000000		set=   1577800740	set2=  2377333712
//		// 10000000		set=   1639320780	set2=  2406177740
//		// modified set
//		// 10000000		set=   1982113928	set2=  2634153083
//		// 10000000		set=   2006108458	set2=  2429153636
//		// 10000000		set=   1909842941	set2=  2424739178
//		// reverted
//		// 10000000		set=   1958138115	set2=  2565424097
//		// 10000000		set=   2018495507	set2=  2556229353
//		// 10000000		set=   2007604388	set2=  2565091583
//	}
//	public Indexes set2(int i, boolean isOn) {
//		if ( isOn )
//			bits |= SHFT[i];
//		else
//			bits &= ~SHFT[i];
//		this.size = Integer.bitCount(bits);
//		return this;
//	}

//// Q: Performance test ways of iterating maybes, ie iterating the set bits in
////    a bitset.
//	public static void main(String[] args) {
//		final int TIMES = 1000*1000; // 1 million
//
//		Grid grid = new Grid("..7..8.6.1...5...8...6.....7...6..35..4..7....9.2......5.4...2..7....3......16.4."); // 4#top1465.d5.mt
//
//		long t0 = System.nanoTime();
//		int cnt0 = 0;
//		for ( int times=0; times<TIMES; ++times ) {
//			for ( Cell cell : grid.cells ) {
//				final int bits = cell.maybes;
//				if ( bits > 0 )
//					for ( int sv=1; sv<=bits; sv<<=1 )
//						if ( (bits & sv) != 0 && sv == 256 ) // ie 9
//							for ( Cell sib : cell.siblings ) {
//								final int sibBits = sib.maybes;
//								if ( sibBits > 0 )
//									for ( int sibv=1; sibv<=sibBits; sibv<<=1 )
//										if ( (sibBits & sibv) != 0 && sibv == 256 ) // ie 9
//											++cnt0;
//							}
//			}
//		}
//
//		long t1 = System.nanoTime();
//		// shiftedValueses: an array of jagged-arrays of the shifted-values
//		// that are packed into your maybes 0..511. See field for more.
//		final int[][] SVS = Values.SHIFTED;
//		int cnt1 = 0;
//		for ( int times=0; times<TIMES; ++times )
//			for ( Cell cell : grid.cells )
//				if ( cell.maybes > 0 )
//					for ( int sv : SVS[cell.maybes] )
//						if ( sv == 256 ) // ie 9
//							for ( Cell sib : cell.siblings )
//								if ( sib.maybes > 0 )
//									for ( int sibv : SVS[sib.maybes] )
//										if ( sibv == 256 ) // ie 9
//											++cnt1;
//
//		long t2 = System.nanoTime();
//
//		System.out.println(TIMES);
//		long o = t1 - t0;
//		long n = t2 - t1;
//		System.out.println("old  "+cnt0+TAB+o);
//		System.out.println("new  "+cnt1+TAB+n);
//		System.out.println("gain \t\t\t"+(o-n));
//		System.out.format ("pct  \t\t\t%5.2f%%\n", TestHelperMethods.pct(o-n, o) );
//	}
///*
//1000000
//old  494000000	5972751700
//new  494000000	4311081700
//gain			1661670000
//pct				27.82%
//
//1000000
//old  494000000	10797583300 <<<< FREAK!
//new  494000000	3366535000
//gain 			7431048300
//pct  			68.82%
//
//1000000
//old  494000000	6122763600
//new  494000000	4133944800
//gain 			1988818800
//pct  			32.48%
//
//1000000
//old  494000000	5844347100
//new  494000000	4123240900
//gain 			1721106200
//pct  			29.45%
//*/

// Q: Performance test ways of parsing maybes from string.
//	public static void main(String[] args) {
//		final int TIMES = 100*1000*1000;
//
//		long t0 = System.nanoTime();
//		long oldBits = 0L;
//		for ( int times=0; times<TIMES; ++times )
//			oldBits += oldV("123456789");
//
//		long t1 = System.nanoTime();
//		long newBits = 0L;
//		for ( int times=0; times<TIMES; ++times )
//			newBits += newV("123456789");
//
//		long t2 = System.nanoTime();
//
//		System.out.println(""+oldBits+TAB+newBits);
//		long a=(t1-t0), b=(t2-t1);
//		System.out.println(""+a+TAB+b+TAB+(a-b));
//		//10*1000*1000
//		// 389823148	550138426	-160315278
//		// 407610953	477505591	 -69894638
//		// 545106920	439778659	 105328261
//		// 404933308	519887723	-114954415
//		// 410230980	563206846	-152975866
//		// 538856824	335568417	 203288407
//		//100*1000*1000
//		// 4517853107	3807365182	 710487925
//		// 4830695521	3809070357	1021625164
//		// 4629115921	3681774988	 947340933
//	}
//	private static int oldV(String s) {
//		int bits = 0;
//		for ( int i=0; i<s.length(); ++i )
//			bits |= SHFT[s.charAt(i)-'0'];
//		return bits;
//	}
//	private static int newV(String s) {
//		int bits = 0;
//		for ( int i=0, n=s.length(); i<n; ++i )
//			bits |= SHFT[s.charAt(i)-'0'];
//		return bits;
//	}

//	private static final int TIMES = 10*1000*1000;
//	public static void main(String[] args) {
//		Values values = new Values();
//
//		long t0 = System.nanoTime();
//		int cnt0 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			cnt0 += values.toBinaryString().length();
//
//		long t1 = System.nanoTime();
//		int cnt1 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			cnt1 += values.toBinaryString2().length();
//
//		long t2 = System.nanoTime();
//		//System.out.format("%d\t%d\n", cnt0/TIMES, cnt1/TIMES);
//		System.out.format("%d\t%d\n", t1-t0, t2-t1);
//		// 9			9
//		//1000*1000
//		// 638807236	557959671
//		// 376363337	454688515
//		// 379606486	478763363
//		//10*1000*1000
//		// 4153496045	3258796509
//		// 4205861849	3347601999
//		// 4401918889	3262509830
//	}
//	public String toBinaryString() {
//		String s = Integer.toBinaryString(bits);
//		return "000000000".substring(0, 9-s.length()) + s;
//	}
//	// and toBinaryString2 becomes toBinaryString

//	private static final int TIMES = 10*1000*1000;
//	public static void main(String[] args) {
//		Values values = new Values(0);
//		System.out.println(""+values.bits+TAB+values.size+TAB+values.toBinaryString());
//		values.set("029");
//		System.out.println(""+values.bits+TAB+values.size+TAB+values.toBinaryString());
//		values.clear(0);
//		System.out.println(""+values.bits+TAB+values.size+TAB+values.toBinaryString());
//		values.clear(8);
//		System.out.println(""+values.bits+TAB+values.size+TAB+values.toBinaryString());
//		values.clear(9);
//		System.out.println(""+values.bits+TAB+values.size+TAB+values.toBinaryString());
//	}

// Q: Performance test is-subset-of method
//	private static final int TIMES = 1000*1000;
//	public static void main(String[] args) {
//		Values values = new Values(1, 3, 4, 8);
//
//		long t0 = System.nanoTime();
//		int cnt0 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			cnt0 += values.isSubsetOf(1, 4) ? 1 : 0;
//
//		long t1 = System.nanoTime();
//		int cnt1 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			cnt1 += values.isSubsetOfNaive(1, 4) ? 1 : 0;
//
//		long t2 = System.nanoTime();
//		System.out.format("%d\t%d\n", cnt0, cnt1);
//		System.out.format("%d\t%d\n", t1-t0, t2-t1);
//	}

//	 public static void main(String[] args) {
//		 int[] thePA = new int[2];
//		 int count = 0;
//		 for ( int[] permutation : new Permutations(2, 62, thePA) )
//			 ++count;
//		 System.out.format("%,d\n", count);
//	 }

//// KRC 2017-12-02 performance test ways of iterating a Values Set:
//// * next1(v+1) vs get(v) vs toArray(a)?
//	private static final int TIMES = 1000;
//	public static void main(String[] args) {
//		Values vs = new Values(1, 3, 4, 8);
//
//		long t0 = System.nanoTime();
//		int cnt0 = 0;
//		for ( int t=0; t<TIMES*TIMES; ++t )
//			for ( int v=vs.first(); v>-1; v=vs.next1(v+1) )
//				cnt0 += v;
//		long t1 = System.nanoTime();
//
//		int cnt1 = 0;
//		for ( int t=0; t<TIMES*TIMES; ++t )
//			for ( int v=1; v<10; ++v )
//				if ( vs.get(v) )
//					cnt1 += v;
//		long t2 = System.nanoTime();
//
//		int cnt2 = 0;
//		int[] va1 = new int[vs.size];
//		for ( int t=0; t<TIMES*TIMES; ++t ) {
//			int n = vs.toArray(va1);
//			for ( int i=0; i<n; ++i )
//				cnt2 += va1[i];
//		}
//		long t3 = System.nanoTime();
//
//		//System.out.format("%,11d\t\t%,11d\t\t%,11d\n", cnt0,  cnt1,  cnt2);
//		System.out.format("%,11d\t\t%,11d\t\t%,11d\n", t1-t0, t2-t1, t3-t2);
//	}
/////*
//// 16,000,000		 16,000,000		 16,000,000
////next1(v+1)		 get(value)		 toArray(a)
////with 1000 toArrays
////208,986,477		 75,273,674		 26,743,692
////215,802,078		 75,618,270		 26,297,127
////204,111,976		 79,071,292		 26,480,461
////with 1000*1000 toArrays
////206,499,778		 75,138,041		304,921,506
////208,347,290		 80,609,826		302,068,349
////211,848,084		 75,526,219		301,008,648
////*/

//// KRC2017-12-12 Performance test iterating an Indexes bitset.
//	private static final int TIMES = 1000 * 1000;
//	public static void main(String[] args) {
//		final int[] SHIFTED_VALUES = ISHFT;
//		final Indexes idxs = new Indexes(0,2,3,5,7,8);
//		final int bits = idxs.bits;
//
//		long t0 = System.nanoTime();
//
//		int cnt0 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			for ( int i=idxs.first(); i>-1; i=idxs.next(i+1) )
//				cnt0 += i;
//		long t1 = System.nanoTime();
//
//		int cnt1 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			for ( int i=numTrailingZeros(bits); i<9; ++i )
//				if ( (bits & 1<<i) != 0 )
//					cnt1 += i;
//		long t2 = System.nanoTime();
//
//		int cnt2 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			for ( int i=0; i<9; ++i )
//				if ( (bits & 1<<i) != 0 )
//					cnt2 += i;
//		long t3 = System.nanoTime();
//
//		int cnt3 = 0;
//		final int[] ISHFT = ISHFT;
//		for ( int t=0; t<TIMES; ++t )
//			for ( int i=0; i<9; ++i )
//				if ( (bits & ISHFT[i]) != 0 )
//					cnt3 += i;
//		long t4 = System.nanoTime();
//
//		int cnt4 = 0;
//		int[] idxsArray = new int[idxs.size];
//		int n = idxs.toArray(idxsArray);
//		for ( int t=0; t<TIMES; ++t )
//			for ( int i=0; i<n; ++i )
//				cnt4 += idxsArray[i];
//		long t5 = System.nanoTime();
//
//		int cnt5 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			for ( int i=0; i<9; ++i )
//				if ( idxs.get(i) )
//					cnt5 += i;
//		long t6 = System.nanoTime();
//
//		int cnt6 = 0;
//		for ( int t=0; t<TIMES; ++t )
//			for ( int si : SHIFTED_VALUES )
//				if ( (bits & si) != 0 )
//// comment out for Aligned*Exclusion where we need only the shifted value
////					cnt6 += unshift(si);
//					++cnt6;
//		long t7 = System.nanoTime();
//
//// *  next: first() and next(v)
//// *   ntz: start-at-local-numTrailingZeros & != 0
//// *  1<<i: bits & 1<<i != 0
//// *  shft: bits & ISHFT[i] & != 0
//// * array: toArray
//// *   get: Indexes.get(i)
//// * SHFTD: SHIFTED_VALUES => bits & i != 0
////		System.out.format("%11s\t\t%11s\t\t%11s\t\t%11s\t\t%11s\t\t%11s\t\t%11s\n", "next", "ntz", "1<<i", "shft", "array", "get", "SHFTD");
////		System.out.format("%,11d\t\t%,11d\t\t%,11d\t\t%,11d\t\t%,11d\t\t%,11d\t\t%,11d\n",  cnt0,  cnt1,  cnt2,  cnt3,  cnt4,  cnt5,  cnt6);
//		System.out.format("%,11d\t\t%,11d\t\t%,11d\t\t%,11d\t\t%,11d\t\t%,11d\t\t%,11d\n", t1-t0, t2-t1, t3-t2, t4-t3, t5-t4, t6-t5, t7-t6);
//	}
//	// From Integer.numberOfTrailingZeros(int i) and hacked for speed
//	private static int numTrailingZeros(int i) {
//		int y;
//		if (i == 0) return 32;
//		int n = 31;
//		y = i<<16; if(y!=0){ n-=16; i=y; }
//		y = i<< 8; if(y!=0){ n-= 8; i=y; }
//		y = i<< 4; if(y!=0){ n-= 4; i=y; }
//		y = i<< 2; if(y!=0){ n-= 2; i=y; }
//		return n - ( (i<<1) >>> 31 );
//	}
//	// turns the leftShiftedIndex si back into an index 0..8
//	// si is a bitset in which a single bit is set.
//	private static int unshift(final int si) {
//		switch (si) {
//		case   1: return 0; // 1<<(1-1)
//		case   2: return 1;
//		case   4: return 2;
//		case   8: return 3;
//		case  16: return 4;
//		case  32: return 5;
//		case  64: return 6;
//		case 128: return 7;
//		case 256: return 8; // 1<<(9-1)
//		}
//		throw new ArrayIndexOutOfBoundsException(""+si+" not in 1..256");
//	}
///*
//       next		   ntz+1<<i		     0+1<<i		      array
// 25,000,000		 25,000,000		 25,000,000		 25,000,000
// 98,083,764		 29,636,169		 48,055,479		112,408,840 // with 1000 toArrays
// 99,267,295		 29,857,984		 50,012,293		 12,103,633 // without
// 97,459,663		 29,773,547		 50,278,038		 12,191,703
//insert "0+shft" before "array"
//       next		   ntz+1<<i		     0+1<<i		     0+shft		      array
// 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000
//108,809,360		 29,707,197		 48,267,448		 23,002,923		 12,158,878
// 95,991,529		 29,132,753		 55,951,665		 27,002,943		 12,114,738
// 98,585,016		 30,158,093		 56,828,102		 25,163,533		 12,111,316
// 96,654,673		 30,639,578		 48,167,854		 25,211,095		 12,280,332
//100,399,353		 29,610,118		 52,303,924		 25,270,390		 12,419,107
//append new "get" after "array"
//       next		   ntz+1<<i		     0+1<<i		     0+shft		      array		        get
// 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000
//104,879,531		 29,140,645		 48,551,422		 23,126,892		 12,057,957		 26,858,511
// 96,631,556		 29,482,518		 57,256,369		 25,188,397		 12,160,973		 26,306,555
// 93,863,396		 42,771,846		 50,167,480		 25,211,374		 12,199,945		 26,307,603
// 98,789,651		 29,663,756		 53,855,309		 25,695,235		 12,142,046		 26,359,286
//// 1000 * 1000
//       next		        ntz		       1<<i		       shft		      array		        get		      SHFTD
// 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000 count
// 14,069,087		 20,997,315		 18,335,758		 13,074,705		 10,814,074		 18,867,153		 24,623,633
// 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000		 25,000,000		  6,000,000 recount no unshift
// 16,652,714		 24,885,629		 24,348,239		 10,679,022		 10,524,575		 13,008,412		 15,641,408 no unshift
// 13,690,022		 17,701,400		 16,080,063		 11,429,039		  7,054,114		 12,122,285		 13,520,766 no unshift
// 11,704,433		 17,297,299		 16,891,790		 15,185,474		  6,881,331		  9,939,935		 14,660,425 no unshift
// 14,112,459		 17,238,059		 16,751,801		 10,209,688		 12,349,370		  9,592,607		 13,793,339 no unshift
//*/

//// KRC2017-12-21 toCharArray verses getChar
//	public static boolean isFast;
//	private static final int HOW_MANY = 100;
//	public static void main(String[] args) {
//		final File file = new File("C:/Users/User/Documents/SudokuPuzzles/Test/LoadTest.txt");
//		try {
//			long t0, t1, t2;
//			ArrayList<String> lines = IO.slurp(file);
//			Grid grid = new Grid();
//			t0 = System.nanoTime();
//
//			isFast = false; // make Values set(s) use toCharArray()
//			for ( int i=0; i<HOW_MANY; ++i )
//				grid.load(lines);
//			t1 = System.nanoTime();
//
//			isFast = true; // make Values set(s) use charAt(i)
//			for ( int i=0; i<HOW_MANY; ++i )
//				grid.load(lines);
//			t2 = System.nanoTime();
//
////			System.out.format("%15s\t%15s\n", "toCharArray", "charAt");
//			System.out.format("%,15d\t%,15d\n", t1-t0, t2-t1);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}
///*
//    toCharArray	         charAt
//    117,890,262	     46,571,492
//    105,107,912	     47,762,565
//     92,878,215	     44,006,361
// */

///* Q: Whats the fastest way to iterate the siblings? Do we:
// *    (a) getSiblingsSet() OR getSiblingsArray()?
// *    (b) use a for-i loop or a foreach loop?
// *	for ( y=0; y<9; ++y )
// *		X_LOOP: for ( x=0; x<9; ++x ) {
// *			if ( (cell=grid.matrix[y][x]).maybesSize < 2 )
// *				continue; // skip set cells and any naked singles
// *			// find excluding cells (siblings with 1> maybesSize <= degree)
// *			for ( Cell sib : cell.getSiblingsArray() ) // 81*20=1620
// */
//	// KRC2017-12-21 Performance: array iterator VERSES for-i loop
//	// KRC2019-09-02 Performance: Does caching cell.value in ArrayIterator matter?
//	// NB: there is 2 mains in this session. The 2nd processes output of 1st.
//	private static final int HOW_MANY = 1000*1000;
//	public static void main(String[] args) {
//		final File file = new File("C:/Users/User/Documents/SudokuPuzzles/Test/LoadTest.txt");
//		try {
//			long t0, t1, t2, t3, t4;
//			ArrayList<String> lines = IO.slurp(file);
//			Grid grid = new Grid();
//			grid.load(lines);
//
//			Cell cell = grid.matrix[0][0];
//
//			// Actually do it ONCE, so all subsequents are equal no-ops:
//			// ie: Cell.canNotBe will go through:
//			//		if ((maybes & SHFT[theValueToRemove])==0)
//			//			return 0; // do nothing
//			// NB: this priming read creates the siblingsSet + siblingsArray
//			for ( Cell sib : cell.getSiblingsArray())
//				if ( sib.value == 0 )
//					sib.canNotBe(cell.value);
//
//			t0 = System.nanoTime();
//
//			{
//				Cell[] sibs;
//				for ( int i=0; i<HOW_MANY; ++i ) {
//					sibs = cell.getSiblingsArray();
//					for ( int j=0; j<20; ++j )
//						if ( sibs[j].value == 0 )
//							sibs[j].canNotBe(cell.value);
//				}
//			}
//			t1 = System.nanoTime();
//
//			for ( int i=0; i<HOW_MANY; ++i )
//				for ( Cell sib : cell.getSiblingsArray() )
//					if ( sib.value == 0 )
//						sib.canNotBe(cell.value);
//			t2 = System.nanoTime();
//
//			{
//				final int value = cell.value;
//				for ( int i=0; i<HOW_MANY; ++i )
//					for ( Cell sib : cell.getSiblingsArray() )
//						if ( sib.value == 0 )
//							sib.canNotBe(value);
//			}
//			t3 = System.nanoTime();
//
//			for ( int i=0; i<HOW_MANY; ++i )
//				for ( Cell sib : cell.getSiblingSet() )
//					if ( sib.value == 0 )
//						sib.canNotBe(cell.value);
//			t4 = System.nanoTime();
//
//			System.out.format("%15s\t%15s\t%15s\t%15s\n", "for-i loop", "array it", "ai cache v", "Set iterator");
//			System.out.format("%,15d\t%,15d\t%,15d\t%,15d\n", t1-t0, t2-t1, t3-t2, t4-t3);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}
///*
//* caching sib in for-i loop
//     for-i loop	       iterator
//     13,072,192	      2,779,333
//     15,804,663	      2,928,584
//     19,801,469	      2,502,623
//* not caching sib in for-i loop
//     for-i loop	       iterator
//      8,216,128	      6,212,032 // WTF: How come iterator is slower? It has not changed.
//     12,769,361	      6,234,521
//     27,505,590	     24,348,346
//      8,328,502	      6,042,598
//* according to this test iterator is actually faster, which is
//	definately NOT the expected result, so I upped HOW_MANY to 10,000
//     for-i loop	       iterator
//     33,294,804	     24,983,972
//     33,837,959	     24,332,003
//* and the iterator is still faster, so I reversed the order in which tests
//  are performed (and bumped-up HOW_MANY = 1000*1000) which REVERSES result:
//     for-i loop	       iterator
//  2,784,070,880	  2,504,064,833
//* reversed order of tests again
//       iterator	     for-i loop
//  2,749,046,177	  2,476,656,461
//  2,681,826,392	  2,471,973,323
//  2,851,303,238	  2,513,990,256
//* swap-back the order of tests again, and added Set iterator for comparison
//     for-i loop	       array it	         Set it
//  2,834,832,918	  2,492,400,501	  4,278,087,718
//  2,677,628,023	  2,481,310,683	  4,307,073,804
//  2,507,369,303	  2,502,931,308	  4,308,725,061
//* So it looks like there is ____-all in it speed-wise, though I must admit that
//  array iterator is faster (NOT what I expected), and the syntax is simpler,
//	so I will prefer the array iterator where-ever practicable.
//* KRC2019-09-02 Performance: Does caching cell.value in ArrayIterator matter?
//  HOW_MANY = 1000*1000
//      for-i loop	       array it	     ai cache v	   Set iterator
//#     51,608,990	     30,276,029	     29,828,205	     86,945,774
//#     30,995,366	     27,372,935	     29,692,447	     91,890,520
//      47,009,104	     27,630,345	     39,456,759	     99,780,320 <= THROW AWAY
//#     29,370,509	     26,133,839	     29,977,010	     87,938,390
//TTL  111,974,865	     83,782,803	     89,497,662	    266,774,684
//AVG   37,324,953	     27,927,599	     29,832,553	     88,924,893
//  So: not dereferencing cell.value makes ____ all difference, which is pretty
//  much what I expected, which is not to expect anything until I learn how Java
//  works differently from C/++
//*/
//// Get the averages of the above # lines.
//// Read this file: "grep" the lines which start with #: split them, read and sum
//// the numbers, then print just the average of each "column".
////	public static void main(String[] args) {
////		File f = new File("C:/Users/User/Documents/NetBeansProjects/DiufSudoku/src/diuf/sudoku/test/MyTester.java");
////		try ( BufferedReader r = new BufferedReader(new FileReader(f)) ) {
////			int[] sums = new int[4];
////			String line;
////			while ( (line=r.readLine()) != null ) {
////				if ( line.length() == 0
////				  || line.charAt(0) != '#' ) continue;
////				line = line.replaceFirst(HASH, EMPTY_STRING); // remove leading hash
////				line = line.replaceAll(SPACE, EMPTY_STRING); // remove spaces
////				line = line.replaceAll(COMMA, EMPTY_STRING); // remove commas
////				String[] fields = line.split(TAB);
////				assert fields.length == 4;
////				int i = 0;
////				for ( String field : fields )
////					sums[i++] += Integer.valueOf(field) / 3.0;
////			}
////			for ( int i=0; i<4; ++i )
////				System.out.format("%,d\n", sums[i]);
////		} catch (IOException ex) {
////			ex.printStackTrace(System.err);
////		}
////	}

//	// Q: What is the fastest way to implement a containsAny(Values) method?
//	// KRC2017-12-24 Locally anding bits is 10 times faster.
//	private static final int HOW_MANY = 1000*1000;
//	/** An array of "shifted" bitset-values (faster than 1<<v-1) */
//	private static final int[] SHFT = new int[9];
//	static {
//		for ( int v=0; v<9; ++v)
//			SHFT[v] = 1<<v;
//	}
//	private static boolean containsAny(Values other) {
//		final int ob=other.bits, tb=7;
//		int sv;
//		for ( int v=1; v<10; ++v )
//			if ( (ob & (sv=SHFT[v]))!=0 && (tb & sv)!=0 )
//				return true;
//		return false;
//	}
//	private static boolean containsAny2(Values other) {
//		final int ob=other.bits, tb=7;
//		for ( int sv : SHFT )
//			if ( (ob & sv)!=0 && (tb & sv)!=0 )
//				return true;
//		return false;
//	}
//	private static boolean containsAny3(Values other) {
//		final int tb=7;
//		if ( (tb & other.bits) != 0 )
//				return true;
//		return false;
//	}
//	public static void main(String[] args) {
//		try {
//			int cnt0=0, cnt1=0, cnt2=0, cnt3=0;
//			long t0, t1, t2, t3, t4;
//			final Values values = new Values(3);
//
//			t0 = System.nanoTime();
//
//			for ( int i=0; i<HOW_MANY; ++i )
//				if ( containsAny2(values) )
//					++cnt0;
//			t1 = System.nanoTime();
//
//			for ( int i=0; i<HOW_MANY; ++i )
//				if ( containsAny(values) )
//					++cnt1;
//			t2 = System.nanoTime();
//
//			for ( int i=0; i<HOW_MANY; ++i )
//				if ( containsAny3(values) )
//					++cnt2;
//			t3 = System.nanoTime();
//
//			final int tb=7;
//			for ( int i=0; i<HOW_MANY; ++i )
//				if ( (tb & values.bits) != 0 )
//					++cnt3;
//			t4 = System.nanoTime();
//
//			System.out.format("%15s\t%15s\t%15s\t%15s\n", "array it", "for-i loop", "& bits", "& local");
//			System.out.format("%,15d\t%,15d\t%,15d\t%,15d\n", cnt0, cnt1, cnt2, cnt3);
//			System.out.format("%,15d\t%,15d\t%,15d\t%,15d\n", t1-t0, t2-t1, t3-t2, t4-t3);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}
///*
//	 for-i loop	       array it
//	  1,000,000	      1,000,000
//	 69,328,155	     90,021,497
//	 41,297,357	     47,340,025
//	 43,442,393	     59,169,601
//looks like for-i has it, but learning from experience, swap the order of tests
//	 array it	     for-i loop
//	 58,488,230	     35,252,595
//	 56,298,566	     37,577,332
//	 54,102,267	     35,248,125
//AVG	 56,296,354	     36,026,017	  64%
//for-i takes about 64% of iterates time, as I expected.
//Tried again with 9 values in SHFT - removed the "unused" 0 index
//	 48,188,387	     38,085,777
//	 46,792,819	     35,545,579
//	 44,009,993	     34,143,446
//AVG	 46,330,400	     35,924,934	  78%
//       array it	     for-i loop	         & bits
//      1,000,000	      1,000,000	      1,000,000
//     47,380,533	     30,613,877	      3,964,540
//       array it	     for-i loop	         & bits	        & local
//      1,000,000	      1,000,000	      1,000,000	      1,000,000
//     48,602,545	     27,139,623	      3,503,867	      3,490,248
//     52,736,451	     31,530,404	      3,516,509	      3,512,248
//*/

// Q: How to make a Collection tell me its required (max used) capacity?
//// KRC2017-12-29 Locking redPots size
////output saved to: DIR + "top1465.descending2.PC-redPots-size.summary.log"
//	static final String DIR = "C:\\Users\\User\\Documents\\NetBeansProjects\\DiufSudoku\\logs\\";
//	static final File INPUT_FILE = new File(DIR+"top1465.descending2.PC-redPots-size.stdout.log");
//	public static void main(String[] args) {
//		try {
//			try ( BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE)) ) {
//				String line;
//				int count = 0;
//				while( (line=reader.readLine()) != null ) {
//					if ( line.startsWith("PC: ") ) {
//						if ( ++count > 1 )
//							System.out.format(comma);
//						System.out.format("%s", line.substring(4));
//					} else if ( line.matches("^[1-9]*\t.*") ) {
//						System.out.format("\t\t\t%d\n", count);
//						count = 0;
//					}
//				}
//				System.out.format("\t\t\t%d\n", count);
//				System.out.println();
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//	// Q: How to make a Collection tell me its required (max used) capacity?
//	// KRC2017-12-29 Locking redPots size
//	//output saved to: DIR + "top1465.descending2.PC-redPots-size.summary.log"
//	static final String DIR = "C:/Users/User/Documents/NetBeansProjects/DiufSudoku/logs/";
//	static final File INPUT_FILE = new File(DIR+"top1465.descending2.PC-redPots-size.stdout.log");
//	public static void main(String[] args) {
//		try {
//			try ( BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE)) ) {
//				String line;
//				int count = 0;
//				while( (line=reader.readLine()) != null ) {
//					if ( line.startsWith("PC: ") ) {
//						if ( ++count > 1 )
//							System.out.format(comma);
//						System.out.format("%s", line.substring(4));
//					} else if ( line.matches("^[1-9]*\t.*") ) {
//						System.out.format("\t\t\t%d\n", count);
//						count = 0;
//					}
//				}
//				System.out.format("\t\t\t%d\n", count);
//				System.out.println();
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//	// Q: How to make a Set tell me its required (max used) capacity?
//	// This version just greps those lines back out of the logfile.
//	// KRC2018-01-05 FunkyAssSets sizes
//	static final String DIR = "C:\\Users\\User\\Documents\\NetBeansProjects\\DiufSudoku\\logs\\";
//	static final File INPUT_FILE = new File(DIR+"FunkyAssSets.log");
//	public static void main(String[] args) {
//		try {
//			Map<String, Integer> map = new HashMap<>(16, 1F);
//			try ( BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE)) ) {
//				String line;
//				while ( (line=reader.readLine()) != null )
//					if ( in(line.charAt(0), '0','9') ) {
//						String[] fields = line.split(" so ");
//						int howMany = Integer.valueOf(fields[0]);
//						String id = fields[1];
//						Integer ex = map.get(id);
//						int existing = ex==null ? 0 : ex.intValue();
//						map.put(id, Math.max(existing, howMany));
//					}
//				for ( String id : map.keySet() )
//					System.out.format("%s\t%d\n", id, map.get(id));
//				System.out.println();
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}
//	private static boolean in(char ch, char from,char to) {
//		return ch>=from && ch<=to;
//	}
///*
//diuf.sudoku.solver.hinters.chain.Chains.doBinaryChains(Chains.java:404)	25
//diuf.sudoku.solver.hinters.chain.Chains.doRegionsChains(Chains.java:438)	20
//diuf.sudoku.solver.hinters.chain.Chains.doRegionsChains(Chains.java:444)	4
//diuf.sudoku.solver.hinters.chain.Chains.doUnaryChains(Chains.java:300)	62
//diuf.sudoku.solver.hinters.chain.Chains.doRegionsChains(Chains.java:437)	10
//diuf.sudoku.solver.hinters.chain.Chains.doRegionsChains(Chains.java:443)	2
//diuf.sudoku.solver.hinters.chain.Chains.getMultipleDynamicChains(Chains.java:220)	123
//diuf.sudoku.solver.hinters.chain.Chains.doUnaryChains(Chains.java:301)	78
//diuf.sudoku.solver.hinters.chain.Chains.getMultipleDynamicChains(Chains.java:219)	50
//diuf.sudoku.solver.hinters.chain.Chains.doBinaryChains(Chains.java:405)	55
//diuf.sudoku.solver.hinters.chain.Chains.doUnaryChains(Chains.java:311)	2
//
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:223)	63
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:224)	207
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:225)	63
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:226)	190
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:228)	62
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:229)	189
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:232)	63
//diuf.sudoku.solver.hinters.chain.Chains.doMultipleDynamicChains(Chains.java:233)	207
//
//*/

//	Q: What is the value of (1<<5)-1
//	private static final int XSHFT = (1<<5)-1;
//	public static void main(String[] args) {
//		try {
//			System.out.format("XSHFT=%d\n", XSHFT);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//	// Q: What is the value of exlusive-bitwise-or (^) VERSES bitwise-or (|)
//	public static void main(String[] args) {
//		try {
//			System.out.format("3^5=%d, 3|5=%d\n", 3^5, 3|5);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//	// Q: What is in System.getProperties()?
//	public static void main(String[] args) {
//		try {
//			System.out.format("getProperties()\n");
//			Properties props = System.getProperties();
//			for ( Object key : props.keySet() )
//				System.out.format("%s=%s\n", key, props.get(key));
//			System.out.format("\n\ngetenv()\n");
//			Map<String,String> env = System.getenv();
//			for ( String key : env.keySet() )
//				System.out.format("%s=%s\n", key, env.get(key));
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//  // Q: How long does it take to create 4,024,758 empty LinkedLists?
//	public static void main(String[] args) {
//		try {
//			final int n = 4024758;
//			long t0 = System.nanoTime();
//			List<Ass> list;
//			for ( int i=0; i<n; ++i )
//				list = new LinkedList<>();
//			long t1 = System.nanoTime();
//			System.out.format("Creating %d LinkedLists took %d\n", n, t1-t0);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//  // Q: What is the value of ((1<<4)-1)<<4 to hard-code?
//	public static void main(String[] args) {
//		try {
//			int i = ((1<<4)-1)<<4;
//			System.out.format("%d\n", i);
//			System.out.format("%s\n", Integer.toBinaryString(i));
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//	// Q: Can we i++ in the line: b[i] = a[i]; ?
//	public static void main(String[] args) {
//		try {
//			final int n = 8;
//			int a[] = new int[]{ 1, 2, 3, 4, 5, 6, 7, 8 };
//			int b[] = new int[n];
//			int i = 0;
//			do {
//			// was
//			//b[i] = a[i];
//			//++i;
//			// causes AIOBE
//			//b[i++] = a[i];
//			// correct, Ya Pillock!
//				b[i] = a[i++];
//			} while ( i < n );
//			System.out.println("Look for the 1 and the 8:");
//			for ( i=0; i<b.length; ++i )
//				System.out.print(SPACE+b[i]);
//			System.out.println();
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}

//	// Q: There cannot actually be a ____nuckle Java API team, surely?
//	public static void main(String[] args) {
//		try {
//			System.out.println("a".equals("b")
//					? "Yep, \"a\".equals(\"b\") Well ____ Me Sideways!"
//					: "Nope, it is just you who's the ____nuckle. Sigh.");
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}


//	// Q: How do you format a date in Java?
// /*
// // The following conversion characters are used for formatting times:
// // 'c' decription                 : ie
// // 'H' 2 digit 24-hour of the day : 00 - 23.
// // 'I' 2 digit 12-Hour of the day : 01 - 12.
// // 'M' 2 digit Minute             : 00 - 59.
// // 'S' 2 digit Seconds            : 00 - 60 (nb "60" supports leap seconds).
// // 'L' 3 digit Milliseconds       : 000 - 999.
// // 'N' 9 Digit Nanoseconds        : 000000000 - 999999999.
// // 'p' Locale-specific morning or afternoon marker in lower case, eg "am" or "pm".
// //     Use of the conversion prefix 'T' forces this output to upper case.
// // 'z' RFC 822 style numeric time zone offset from GMT, eg -0800. This value will
// //     be adjusted as necessary for Daylight Saving Time. For long, Long, and Date
// //     the time zone used is the default time zone for this instance of the Java
// //     virtual machine.
// // 'Z' A string representing the abbreviation for the time zone. This value will be
// //     adjusted as necessary for Daylight Saving Time. For long, Long, and Date the
// // 	time zone used is the default time zone for this instance of the Java
// // 	virtual machine. The Formatter's locale will supersede the locale of the
// // 	argument (if any).
// // 's' Seconds since the beginning of the epoch starting at 1 January 1970 00:00:00
// // 	UTC, ie Long.MIN_VALUE/1000 to Long.MAX_VALUE/1000.
// // 'Q' Milliseconds since the beginning of the epoch starting at 1 January 1970
// // 	00:00:00 UTC, ie Long.MIN_VALUE to Long.MAX_VALUE.
// //
// // The following conversion characters are used for formatting dates:
// // 'B' Locale-specific full month name,
// //     eg "January", "February".
// // 'b' Locale-specific abbreviated month name,
// //     eg "Jan", "Feb".
// // 'h' Same as 'b'.
// // 'A' Locale-specific full name of the day of the week,
// //     eg "Sunday", "Monday"
// // 'a' Locale-specific short name of the day of the week,
// //     eg "Sun", "Mon"
// // 'C' Four-digit year divided by 100, formatted as two digits with leading zero
// //     as necessary,
// //     ie 00 - 99
// // 'Y' Year, formatted as at least four digits with leading zeros as necessary,
// //     eg 0092 equals 92 CE for the Gregorian calendar.
// // 'y' Last two digits of the year, formatted with leading zeros as necessary,
// //     ie 00 - 99.
// // 'j' Day of year, formatted as three digits with leading zeros as necessary,
// //     eg 001 - 366 for the Gregorian calendar.
// // 'm' Month, formatted as two digits with leading zeros as necessary,
// //     ie 01 - 13.
// // 'd' Day of month, formatted as two digits with leading zeros as necessary,
// //     ie 01 - 31
// // 'e' Day of month, formatted as two digits,
// //     ie 1 - 31.
// // The following conversion characters are used for formatting common date/time
// // 	compositions.
// // 'R' Time formatted for the 24-hour clock as "%tH:%tM"
// // 'T' Time formatted for the 24-hour clock as "%tH:%tM:%tS".
// // 'r' Time formatted for the 12-hour clock as "%tI:%tM:%tS %Tp". The location of
// //     the morning or afternoon marker ('%Tp') may be locale-dependent.
// // 'D' Date formatted as "%tm/%td/%ty".
// // 'F' ISO 8601 complete date formatted as "%tY-%tm-%td".
// // 'c' Date and time formatted as "%ta %tb %td %tT %tZ %tY",
// //     eg "Sun Jul 20 16:17:00 EDT 1969".
// */
//	// Q: How do you format a date in Java?
//	public static void main(String[] args) {
// 		try {
// 			// NB:  printf now just calls format, which is slow.
// 			//		Atleast PrintStream retains its bloody Formatter.
// 			System.out.format("Default date : %s\n", new Date());				// Default date : Thu Oct 03 06:11:13 AEST 2019
// 			System.out.format("Local time   : %tT\n", Calendar.getInstance());	// Local time   : 06:11:13
//
// 			Calendar c = new GregorianCalendar(1995, MAY, 13);
// 			// NB: the argument_index$ syntax is used to reference the same arg
// 			// 3 times: %[argument_index$][flags][width][.precision]conversion
// 			// IMPL: return new Formatter().format(format, args).toString();
// 			// NB: It does not retain the Formatter(), which ain't cheap to make.
// 			// So do not use the convenience method in a loop! These assmongels
// 			// wonder why smart people think these assmongels is assmongels.
// 			// Retain the formatter ya' ____ing assmongols! It is stateless! And
//			// if it is not stateless then rip-out whatever to make it bloody
//			// stateless; and retain the bastard... coz it is fast. I'd chuck
//			// Java's formatter to the dump and just boost GNU's printf (even
//			// if the assmongels need to wrap it in format method) because
//			// it is a thousand times faster than this pile of crap.
// 			String s = String.format("Run away!!!!!! %1$tb %1$te, %1$tY", c);
// 			System.out.format("%s\n", s);	// Local time   : 06:11:13
// 			// 'F' ISO 8601 complete date formatted as "%tY-%tm-%td".
// 			// 'T' Time formatted for the 24-hour clock as "%tH:%tM:%tS".
// 			System.out.format("run : %1$tF %1$tT\n", new Date());
// 			// 'F' ISO 8601 complete date formatted as "%tY-%tm-%td".
// 			// 'H' 2 digit 24-hour of the day : 00 - 23.
// 			// 'M' 2 digit Minute             : 00 - 59.
// 			// 'S' 2 digit Seconds            : 00 - 60 (leap seconds).
// 			// YYYY-MM-DD.hh-mm-ss
// 			System.out.format("%s built %s ran %3$tF.%3$tH-%3$tM-%3$tS\n"
// 					, Config.ATV, Config.BUILT, new Date() );
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

// 	// Q: Is System.nanoTime() slow compared to System.currentTimeMillis()?
// 	//    I have instituted a c__k-block on A*E and I want to know if I should
// 	//    use a tape-measure, or something a little more English?
// 	public static void main(String[] args) {
// 		try {
// 			final int TIMES = 500*1000*1000;
// 			long start = System.nanoTime();
// 			long x;
// 			for ( int i=0; i<TIMES; ++i )
// 				x = System.nanoTime();
// 			System.out.println("nano : "+(System.nanoTime() - start));
// 			start = System.nanoTime();
// 			for ( int i=0; i<TIMES; ++i )
// 				x = System.currentTimeMillis();
// 			System.out.println("milli: "+(System.nanoTime() - start));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
// /*
// nano : 11,785,245,707
// milli:  1,734,446,218
// so System.currentTimeMillis() runs in about 14.72% of the time
// that System.nanoTime() does (today on my machine). Anglify now!
// Also interesting that all my debug timings are in nanoseconds,
// but for LogicalSolverTester and Aligned*Exclusion I am interested
// in how many minutes the bastard takes, so @maybe I should be using
// currentTimeMillis() for debug timings, despite its horendous name.
// */

//	// Q: How to format a binary number with indentf?
//	//    Moved code to debug coz it might actually be useful
// 	public static void main(String[] args) {
// 		try {
// 			Grid grid = new Grid("7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5");
//			Debug.dumpAllCellsMaybesBinary(grid);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
// /*
// 000000000, 110010001, 010110000, 100100011, 100110101, 100110110, 000000000, 000010111, 000000111
// 100111001, 000000000, 000111000, 100101001, 000000000, 100111100, 000110001, 000000000, 000000101
// 000111001, 000011001, 000000000, 000101011, 000110001, 000000000, 001110011, 001010011, 000000000
// 110001010, 111001000, 011001010, 000000000, 110100001, 100101000, 000000000, 001001011, 011001011
// 010011100, 000000000, 011011000, 010001001, 000000000, 000001100, 011010001, 000000000, 011001001
// 110011110, 110011000, 000000000, 110001000, 110000100, 000000000, 010010010, 000011010, 000000000
// 010111011, 011011001, 011111010, 000000000, 010110000, 000110010, 000000000, 001001011, 011001011
// 010010011, 000000000, 011010010, 111000010, 000000000, 100010010, 011000011, 000000000, 011000011
// 010101010, 011001000, 000000000, 011100010, 010100000, 000000000, 011000010, 001001110, 000000000
// */

//	// Q: That'd be more useful in standard format wouldn't it?
//	//    Moved code to debug coz it might actually be useful
//	public static void main(String[] args) {
//		try {
//			Grid grid = new Grid("7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5");
//			Debug.dumpAllCellsMaybes(grid);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.out);
//		}
//	}
// /*
// -     , 1589  , 568   , 1269  , 13569 , 23569 , -     , 1235  , 123
// 14569 , -     , 456   , 1469  , -     , 34569 , 156   , -     , 13
// 1456  , 145   , -     , 1246  , 156   , -     , 12567 , 1257  , -
// 2489  , 4789  , 2478  , -     , 1689  , 469   , -     , 1247  , 12478
// 3458  , -     , 4578  , 148   , -     , 34    , 1578  , -     , 1478
// 234589, 4589  , -     , 489   , 389   , -     , 258   , 245   , -
// 124568, 14578 , 245678, -     , 568   , 256   , -     , 1247  , 12478
// 1258  , -     , 2578  , 2789  , -     , 259   , 1278  , -     , 1278
// 2468  , 478   , -     , 2678  , 68    , -     , 278   , 2347  , -
// */

//	// Q: What about the regions indexes of values?
//	//    Moved code to debug coz it might actually be useful
// 	public static void main(String[] args) {
// 		try {
// 			Grid grid = new Grid("7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5");
//			Debug.dumpRegionsIndexesOfAllValuesBinary(grid);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
// /*
// value = 1
//     box 1=011001010    box 2=011001011    box 3=011101110    box 4=000000000    box 5=000001010    box 6=000101110    box 7=000001011    box 8=000000000    box 9=000101110
//     row 1=110011010    row 2=101001001    row 3=011011011    row 4=110010000    row 5=101001000    row 6=000000000    row 7=110000011    row 8=101000001    row 9=000000000
//     col A=011000110    col B=001000101    col C=000000000    col D=000010111    col E=000001101    col F=000000000    col G=010010110    col H=001001101    col I=011011011
//
// value = 2
//     box 1=000000000    box 2=001000101    box 3=011000110    box 4=001000101    box 5=000000000    box 6=011000110    box 7=001101101    box 8=001101100    box 9=011101110
//     row 1=110101000    row 2=000000000    row 3=011001000    row 4=110000101    row 5=000000000    row 6=011000001    row 7=110100101    row 8=101101101    row 9=011001001
//     col A=111101000    col B=000000000    col C=011001000    col D=110000101    col E=000000000    col F=011000001    col G=110100100    col H=101101101    col I=011001001
//
// value = 3
//     box 1=000000000    box 2=000100110    box 3=000100110    box 4=001001000    box 5=010100000    box 6=000000000    box 7=000000000    box 8=000000000    box 9=010000000
//     row 1=110110000    row 2=100100000    row 3=000000000    row 4=000000000    row 5=000100001    row 6=000010001    row 7=000000000    row 8=000000000    row 9=010000000
//     col A=000110000    col B=000000000    col C=000000000    col D=000000000    col E=000100001    col F=000010011    col G=000000000    col H=100000001    col I=000000011
//
// value = 4
//     box 1=011101000    box 2=001101000    box 3=000000000    box 4=011101111    box 5=001101100    box 6=010100110    box 7=011000111    box 8=000000000    box 9=010000110
//     row 1=000000000    row 2=000101101    row 3=000001011    row 4=110100111    row 5=100101101    row 6=010001011    row 7=110000111    row 8=000000000    row 9=010000011
//     col A=101111110    col B=101101100    col C=001011010    col D=000110110    col E=000000000    col F=000011010    col G=000000000    col H=101101000    col I=001011000
//
// value = 5
//     box 1=011101110    box 2=010100110    box 3=011001010    box 4=011101000    box 5=000000000    box 6=011001000    box 7=000101111    box 8=000100110    box 9=000000000
//     row 1=010110110    row 2=001100101    row 3=011010011    row 4=000000000    row 5=001000101    row 6=011000011    row 7=000110111    row 8=000100101    row 9=000000000
//     col A=011110110    col B=001100101    col C=011010011    col D=000000000    col E=001000101    col F=011000011    col G=000110110    col H=000100101    col I=000000000
//
// value = 6
//     box 1=001101100    box 2=011101111    box 3=001001000    box 4=000000000    box 5=000000110    box 6=000000000    box 7=001000101    box 8=011000110    box 9=000000000
//     row 1=000111100    row 2=001101101    row 3=001011001    row 4=000110000    row 5=000000000    row 6=000000000    row 7=000110101    row 8=000000000    row 9=000011001
//     col A=101000110    col B=000000000    col C=001000011    col D=100000111    col E=101001101    col F=001001011    col G=000000110    col H=000000000    col I=000000000
//
// value = 7
//     box 1=000000000    box 2=000000000    box 3=011000000    box 4=000100110    box 5=000000000    box 6=000101110    box 7=010100110    box 8=001001000    box 9=011101110
//     row 1=000000000    row 2=000000000    row 3=011000000    row 4=110000110    row 5=101000100    row 6=000000000    row 7=110000110    row 8=101001100    row 9=011001010
//     col A=000000000    col B=101001000    col C=011011000    col D=110000000    col E=000000000    col F=000000000    col G=110010100    col H=101001100    col I=011011000
//
// value = 8
//     box 1=000000110    box 2=000000000    box 3=000000000    box 4=011101111    box 5=011001010    box 6=001101100    box 7=011101111    box 8=011001010    box 9=001101100
//     row 1=000000110    row 2=000000000    row 3=000000000    row 4=100010111    row 5=101001101    row 6=001011011    row 7=100010111    row 8=101001101    row 9=001011011
//     col A=111111000    col B=101101001    col C=011011001    col D=110110000    col E=101101000    col F=000000000    col G=110110000    col H=000000000    col I=011011000
//
// value = 9
//     box 1=000001010    box 2=000101111    box 3=000000000    box 4=011000011    box 5=011000110    box 6=000000000    box 7=000000000    box 8=000101000    box 9=000000000
//     row 1=000111010    row 2=000101001    row 3=000000000    row 4=000110011    row 5=000000000    row 6=000011011    row 7=000000000    row 8=000101000    row 9=000000000
//     col A=000101010    col B=000101001    col C=000000000    col D=010100011    col E=000101001    col F=010001011    col G=000000000    col H=000000000    col I=000000000
// */

//	// Q: That'd be more useful in standard format, wouldn't it?
//	//    Moved code to debug coz it might actually be useful
// 	public static void main(String[] args) {
// 		try {
// 			Grid grid = new Grid("7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5");
//			Debug.dumpRegionsIndexesOfAllValues(grid);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
// /*
// value = 1
//  box 1=1367     box 2=01367    box 3=123567   box 4=         box 5=13       box 6=1235     box 7=013      box 8=         box 9=1235
//  row 1=13478    row 2=0368     row 3=013467   row 4=478      row 5=368      row 6=         row 7=0178     row 8=068      row 9=
//  col A=1267     col B=026      col C=         col D=0124     col E=023      col F=         col G=1247     col H=0236     col I=013467
//
// value = 2
//  box 1=         box 2=026      box 3=1267     box 4=026      box 5=         box 6=1267     box 7=02356    box 8=2356     box 9=123567
//  row 1=3578     row 2=         row 3=367      row 4=0278     row 5=         row 6=067      row 7=02578    row 8=023568   row 9=0367
//  col A=35678    col B=         col C=367      col D=0278     col E=         col F=067      col G=2578     col H=023568   col I=0367
//
// value = 3
//  box 1=         box 2=125      box 3=125      box 4=36       box 5=57       box 6=         box 7=         box 8=         box 9=7
//  row 1=4578     row 2=58       row 3=         row 4=         row 5=05       row 6=04       row 7=         row 8=         row 9=7
//  col A=45       col B=         col C=         col D=         col E=05       col F=014      col G=         col H=08       col I=01
//
// value = 4
//  box 1=3567     box 2=356      box 3=         box 4=0123567  box 5=2356     box 6=1257     box 7=01267    box 8=         box 9=127
//  row 1=         row 2=0235     row 3=013      row 4=012578   row 5=02358    row 6=0137     row 7=01278    row 8=         row 9=017
//  col A=1234568  col B=23568    col C=1346     col D=1245     col E=         col F=134      col G=         col H=3568     col I=346
//
// value = 5
//  box 1=123567   box 2=1257     box 3=1367     box 4=3567     box 5=         box 6=367      box 7=01235    box 8=125      box 9=
//  row 1=12457    row 2=0256     row 3=01467    row 4=         row 5=026      row 6=0167     row 7=01245    row 8=025      row 9=
//  col A=124567   col B=0256     col C=01467    col D=         col E=026      col F=0167     col G=1245     col H=025      col I=
//
// value = 6
//  box 1=2356     box 2=0123567  box 3=36       box 4=         box 5=12       box 6=         box 7=026      box 8=1267     box 9=
//  row 1=2345     row 2=02356    row 3=0346     row 4=45       row 5=         row 6=         row 7=0245     row 8=         row 9=034
//  col A=1268     col B=         col C=016      col D=0128     col E=02368    col F=0136     col G=12       col H=         col I=
//
// value = 7
//  box 1=         box 2=         box 3=67       box 4=125      box 5=         box 6=1235     box 7=1257     box 8=36       box 9=123567
//  row 1=         row 2=         row 3=67       row 4=1278     row 5=268      row 6=         row 7=1278     row 8=2368     row 9=1367
//  col A=         col B=368      col C=3467     col D=78       col E=         col F=         col G=2478     col H=2368     col I=3467
//
// value = 8
//  box 1=12       box 2=         box 3=         box 4=0123567  box 5=1367     box 6=2356     box 7=0123567  box 8=1367     box 9=2356
//  row 1=12       row 2=         row 3=         row 4=01248    row 5=02368    row 6=01346    row 7=01248    row 8=02368    row 9=01346
//  col A=345678   col B=03568    col C=03467    col D=4578     col E=3568     col F=         col G=4578     col H=         col I=3467
//
// value = 9
//  box 1=13       box 2=01235    box 3=         box 4=0167     box 5=1267     box 6=         box 7=         box 8=35       box 9=
//  row 1=1345     row 2=035      row 3=         row 4=0145     row 5=         row 6=0134     row 7=         row 8=35       row 9=
//  col A=135      col B=035      col C=         col D=0157     col E=035      col F=0137     col G=         col H=         col I=
// */

//	// Q: What is the cost of accessing a field over a local? And what about a
//	//    static field?
//	private int field = 0;
//	private int lcl2Fld = 0;
//	private static int staticField = 0;
// 	public static void main(String[] args) {
// 		try {
//			final int TIMES = Integer.MAX_VALUE;
//			MyTester tester = new MyTester();
//
//			long t0 = System.nanoTime();
//			testStaticField(TIMES);
//
//			long t1 = System.nanoTime();
//			tester.testField(TIMES);
//
//			long t2 = System.nanoTime();
//			tester.testLocalToField(TIMES);
//
//			long t3 = System.nanoTime();
//			int local = tester.testLocal(TIMES);
//
//			long t4 = System.nanoTime();
//			System.out.format("static  = %,13d\t%,d\n", t1-t0, staticField);
//			System.out.format("field   = %,13d\t%,d\n", t2-t1, tester.field);
//			System.out.format("lcl/fld = %,13d\t%,d\n", t3-t2, tester.lcl2Fld);
//			System.out.format("local   = %,13d\t%,d\n", t4-t3, local);
//
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
//	private int testLocal(int times) {
//		int local = 0;
//		for ( int t=0; t<times; ++t )
//			++local;
//		return local;
//	}
//	private void testLocalToField(int times) {
//		int local = 0;
//		for ( int t=0; t<times; ++t )
//			++local;
//		lcl2Fld = local;
//	}
//	private void testField(int times) {
//		for ( int t=0; t<times; ++t )
//			++field;
//	}
//	private static void testStaticField(int times) {
//		for ( int t=0; t<times; ++t )
//			++staticField;
//	}
///*
//At face value it is quite expensive to hammer a field. The local is taking just
//over 1% of the field time, so DO NOT HAMMER A FIELD!!! but I am also thinking
//that it is a lot less clear-cut in the real world.
//local  =     1,383,671	2,147,483,647
//field  =   133,768,980	2,147,483,647
//static =   120,163,234	2,147,483,647
//
//local  =     1,794,118	2,147,483,647
//lcl/fld=     1,749,336	2,147,483,647
//field  =   160,166,280	2,147,483,647
//static =   124,323,768	2,147,483,647
//
//local   =     1,455,958	2,147,483,647
//lcl/fld =     1,519,781	2,147,483,647
//field   =   124,899,945	2,147,483,647
//static  =   151,632,584	2,147,483,647
//change the order because I have seen that dramatically effect results previously.
//static  =   148,385,682	2,147,483,647
//field   =   137,202,417	2,147,483,647
//lcl/fld =     1,743,694	2,147,483,647
//local   =     1,296,221	2,147,483,647
//*/

//	// Q: Does %02d work?
//	// A: Yes: %02d => 03 and %03d => 003
// 	public static void main(String[] args) {
// 		try {
//			System.out.format("%02d\n", 3);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: Can a generic generic narrowCast method work?
//	// A: Yes: But JESUS H CHRIST it is ugly!
// 	public static void main(String[] args) {
// 		try {
//			LogicalSolver solver = new LogicalSolver();
//			List<IHinter> wh = solver.wantedHinters;
//			IHinter[] wantedHinters = wh.toArray(new IHinter[wh.size()]);
//			IHintNumberActivatableHinter[] activatables
//					= new IHintNumberActivatableHinter[0];
//			activatables = MyArrays.narrowCast(wantedHinters, activatables); // <<<< ==== THE INTERESTING BIT
//			System.out.print("activatables:");
//			for ( IHintNumberActivatableHinter hnah : activatables )
//				System.out.print(SPACE+hnah);
//			System.out.println();
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: Does the MyArrays.filter method work?
//	// A: Yes. But it does NOT downcast it is elements!!!
// 	public static void main(String[] args) {
// 		try {
//			final IFilter<IHinter> ACTIVATABLES_FILTER
//					= new IFilter<IHinter>() {
//				@Override
//				public boolean accept(IHinter h) {
//					return (h instanceof IHintNumberActivatableHinter);
//				}
//			};
//			final IFormatter<IHinter> HINTER_TECH_NAME
//					= new IFormatter<IHinter>(){
//				@Override
//				public String format(IHinter e) {
//					return e.getTechName();
//				}
//			};
//			LogicalSolver solver = new LogicalSolver();
//			List<IHinter> wh = solver.wantedHinters;
//			LinkedList<IHinter> activatables
//					= MyArrays.filter(wh, ACTIVATABLES_FILTER);
//			System.out.print("activatables:");
//			for ( IHinter hnah : activatables )
//				System.out.print(SPACE+HINTER_TECH_NAME.format(hnah));
//			System.out.println();
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: Does the MyArrays.filterAnd and filterOr methods work?
//	// A: Yes.
// 	public static void main(String[] args) {
// 		try {
//			final IFilter<IHinter> ACTIVATABLE = new IFilter<IHinter>() {
//				@Override
//				public boolean accept(IHinter h) {
//					return (h instanceof IHintNumberActivatableHinter);
//				}
//			};
//			final IFilter<IHinter> QUAD = new IFilter<IHinter>() {
//				@Override
//				public boolean accept(IHinter h) {
//					return (h.getTechName().toLowerCase().contains("quad") );
//				}
//			};
//			LogicalSolver solver = new LogicalSolver();
//			List<IHinter> wh = solver.wantedHinters;
//			println("Activatable:",         MyArrays.filter(wh, ACTIVATABLE));
//			println("Quad:",                MyArrays.filter(wh, QUAD));
//			println("ActivatableQuad:",     MyArrays.filterAnd(wh, ACTIVATABLE, QUAD));
//			println("Activatable OR Quad:", MyArrays.filterOr(wh, ACTIVATABLE, QUAD));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
//	private static final IFormatter<IHinter> HINTER_TECH_NAME = new IFormatter<IHinter>(){
//		@Override
//		public String format(IHinter e) {
//			return e.getTechName();
//		}
//	};
//	private static void println(String hdr, List<IHinter> list) {
//		println(hdr, HINTER_TECH_NAME, list);
//	}
//	private static void println(String hdr, IFormatter<IHinter> formatter, List<IHinter> list) {
//		System.out.print(hdr);
//		for ( IHinter hnah : list )
//			System.out.print(SPACE+formatter.format(hnah));
//		System.out.println();
//	}
/*
Oct 29, 2019 12:15:46 PM java.util.prefs.WindowsPreferences <init>
WARN: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0x80000002. Windows RegCreateKeyEx(...) returned error code 5.
LogicalSolver: unwanted: HiddenQuad
LogicalSolver: unwanted: NakedPent
LogicalSolver: unwanted: HiddenPent
LogicalSolver: unwanted: NestedUnary
LogicalSolver: unwanted: NestedMultiple
LogicalSolver: unwanted: NestedDynamic
Activatable: XY_Wing Swampfish Swordfish Jellyfish XYZ_Wing URT BUG AlignedPair AlignedTriple AlignedQuad AlignedPent AlignedHex AlignedSept AlignedOct AlignedNona AlignedDec UnaryChain NishioChain MultipleChain DynamicChain DynamicPlus
Quad: NakedQuad AlignedQuad
ActivatableQuad: AlignedQuad
Activatable OR Quad: XY_Wing Swampfish Swordfish NakedQuad Jellyfish XYZ_Wing URT BUG AlignedPair AlignedTriple AlignedQuad AlignedPent AlignedHex AlignedSept AlignedOct AlignedNona AlignedDec UnaryChain NishioChain MultipleChain DynamicChain DynamicPlus
BUILD SUCCESSFUL (total time: 0 seconds)
*/

//	// Q: Does MyArrays.indexOf and contains work?
//	// A: Yes
// 	public static void main(String[] args) {
// 		try {
//			LogicalSolver solver = new LogicalSolver();
//			List<IHinter> wh = solver.wantedHinters;
//			IHinter[] array = wh.toArray(new IHinter[wh.size()]);
//			IHinter target = find(wh, Tech.NakedQuad);
//			int i = MyArrays.indexOf(array, target);
//			System.out.format("%d\n", i);
//			boolean b = MyArrays.containsC(array, target);
//			System.out.format("%s\n", b);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
//	private static IHinter find(List<IHinter> list, Tech target) {
//		for ( IHinter h : list )
//			if ( h.getTech() == target )
//				return h;
//		return null;
//	}

//	// Q: Does MyArrays.contains work?
//	// A: Yes
// 	public static void main(String[] args) {
// 		try {
//			LogicalSolver solver = new LogicalSolver();
//			List<IHinter> wh = solver.wantedHinters;
//			IHinter[] array = wh.toArray(new IHinter[wh.size()]);
//			IHinter target = new NakedSet(Tech.NakedPent);
//			int i = MyArrays.indexOf(array, target);
//			System.out.format("%d\n", i);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: How to: 1 = 1,194,198,844 of 1,209,288,019 = 98.75%
//	// A: use the pct method
// 	public static void main(String[] args) {
// 		try {
//			System.out.format("%5.2f\n", pct(1194198844L, 1209288019L));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: What the hell is Long.MAX_VALUE (2^63)-1
//	// A: use the pct method
// 	public static void main(String[] args) {
// 		try {
//			System.out.format("%,d\n", Long.MAX_VALUE);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: Does Java evaluate both expressions in a ?: operation?
//	// A: NO, at least it does not if there is side-effects, which I presume
//	//    means that it just evaluates the selected path.
// 	public static void main(String[] args) {
// 		try {
//			Random rnd = Grid.RANDOM;
//			int a=0, b=100;
//			System.out.format("r = %,d\n", rnd.nextInt(2)==0 ? ++a : ++b);
//			System.out.format("a = %,d\n", a);
//			System.out.format("b = %,d\n", b);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: What is the ammortised cost of totalSibCollisions?
//	// A: See below
// 	public static void main(String[] args) {
// 		try {
//			for ( int n=5; n<11; ++n ) {
//				int sum = 0;
//				for ( int i=n; i>0; --i )
//					sum += i;
//				System.out.format("sum(%2d) = %d\n", n, sum);
//			}
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
///*
//sum( 5) = 15
//sum( 6) = 21
//sum( 7) = 28
//sum( 8) = 36
//sum( 9) = 45
//sum(10) = 55
//*/

//// Q: Performance test bitwise negation... is it faster to pre-flip before
////	repeatedly using it, or do we magically get our flips for free?
//
//	private static final Random RANDOM = Grid.RANDOM;
//	public static void main(String[] args) {
//
//		final int TIMES =    1000; // A THOUSAND
//		final int TIME2 = 1000000; // A MILLION
//
//		final int x = randomBits(10, 3); //0..9 * 3 bits,
//
//		int sv, flip;
//
//		int cnt0=0, cnt1=0;
//		long t0=0L, t1=0L, t,tmp;
//
//		for ( int times=0; times<TIMES; ++times ) {
//			sv = randomBits(10, 3); // 0..9
//			t = System.nanoTime();
//			for ( int time2=0; time2<TIME2; ++time2 )
//				if ( (x & ~sv) == 0 )
//					++cnt0;
//			t0 += ((tmp=System.nanoTime())-t); t=tmp;
//			flip = ~sv;
//			for ( int time2=0; time2<TIME2; ++time2 )
//				if ( (x & flip) == 0 )
//					++cnt1;
//			t1 += ((tmp=System.nanoTime())-t); t=tmp;
//		}
//
//		System.out.format("%4s = %,14d\t%d\n", "~", t0, cnt0);
//		System.out.format("%4s = %,14d\t%d\n", "flip", t1, cnt1);
//
//	}
//	private static final int randomBits(int max, int howMany) {
//		int x = 0;
//		do {
//			x |= 1<<(RANDOM.nextInt(max));
//		} while ( Integer.bitCount(x) < howMany );
//		return x;
//	}
///*
//	   ~ =    503,737,300	9000000
//	flip =    945,677,100	9000000
//	   ~ =    615,081,900	5000000
//	flip =  1,156,248,100	5000000
//	   ~ =    643,801,900	9000000
//	flip =  1,221,313,200	9000000
//	ERGO: Yes, it is actaully faster to ~ it repeatedly!
//*/

//	// Q: What is a good serialVersionUID?
//	// A: Blow it out your ass HAL, ya bitwit!
// 	public static void main(String[] args) {
// 		try {
//			for ( char c : "A_WORD".toCharArray() )
//				System.out.format("%d", (int)c);
//			System.out.println();
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: What are you doing Dave?
//	// A: Blow it out your ass HAL, ya bitwit!
// 	public static void main(String[] args) {
// 		try {
//			System.out.format("What are you doing Dave?\n");
//			System.out.format("Farting!\n");
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
//
////	// Q: Check what is ~0 in bits?
////	// A: 11111111111111111111111111111111 (32 set bits)
// 	public static void main(String[] args) {
// 		try {
//			System.out.format("~0=%s\n", Integer.toBinaryString(~0));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: Is (m1|m2)!=0 faster or slower than (m1!=0 || m2!=0)
//	// A: Yeah, it appears to be a bit faster, but only a bit; so I shall use
//	// the (m1|m2)!=0 method because it is more succinct; I just wanted to check
//	// that it was not actually slower.
// 	public static void main(String[] args) {
// 		try {
//			// Do it 1 billion 247 million times
//			final int HOW_MANY = Integer.MAX_VALUE;
//			// randomise the "bitsets" to be evaluated, to check it is actually
//			// being executed HOW_MANY times, ergo the test has not been chopped
//			// as a "no-op" by the bloody JIT compiler.
//			Random r = Grid.RANDOM;
//			long m1=r.nextBoolean()?1:0, m2=r.nextBoolean()?1:0;
//			// check that both empty-tests agree (ie are correct because I know
//			// from experience that the "m1!=0||m2!=0 version" is correct).
//			assert ((m1|m2)!=0) == (m1!=0||m2!=0);
//
//			// time the shorter (m1|m2)!=0 version
//			long nanos = System.nanoTime();
//			for ( int i=0; ((m1|m2)!=0) && i<HOW_MANY; ++i );
//			nanos = System.nanoTime() - nanos;
//			System.out.println("m1|m2       took "+nanos);
//
//			// time the traditional m1!=0||m2!=0 version
//			nanos = System.nanoTime();
//			for ( int i=0; (m1!=0||m2!=0) && i<HOW_MANY; ++i );
//			nanos = System.nanoTime() - nanos;
//			System.out.println("m1!=0||m2!= took "+nanos);
//
//			//
//			System.out.println("m1="+m1+", m2="+m2);
//
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
///*
//100*1000*1000:
//0,1; m1|m2 took 2,570,300  m1!=0||m2!= took 2,165,000
//1,0: m1|m2 took 2,508,100  m1!=0||m2!= took 2,864,500
//0,1: m1|m2 took 2,770,300  m1!=0||m2!= took 2,388,500
//1,0: m1|m2 took 2,276,000  m1!=0||m2!= took 3,581,900 // <<-- that is a 3!
//1,0: m1|m2 took 2,341,700  m1!=0||m2!= took 4,628,100 // <<-- that is a 4!
//1000*1000*1000:
//m1|m2 took 2192400 m1!=0||m2!= took 2173300
//*/

//	// Q: Is SudokuSetBase setAndAnd faster than set; and; and?
//	// Check ONCE that both Test A and Test B produce the same result.
//	// Run a loop a million times, which:
//	// * Test A: does it the old way: call set; and; and.
//	// * Test B: does it my way: just call setAndAnd.
//	// Print out timings (in nanoseconds) of A and B, one underneath the other.
//	//
//	// A: Yeah, setAndAnd is a bit faster. It takes about half the time.
//	//	A took   10,589,500   13,560,900   10,449,100   10,000,400
//	//	B took    6,434,900    8,243,100    6,799,900    6,775,100
//	//
//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	//
//	// Q2: How how does that compare to "going the full monty" and splitting
//	// the set into mask1 and mask2 which are calculated locally?
//	//
//	// A: I do not know! Really! It was ship-loads faster A, B, C; but then I
//	// just swapped the order of the tests around to C, B, A to assure myself
//	// that the order in which tests are run is NOT effecting there timings;
//	// and I find to my horror that it DOES in fact effect timings. So the
//	// honest answer is: I do not know, but I still strongly suspect that "the
//	// full monty" is fastest, but only by 22.95% (not the expected shipload).
//	//
//	// Original order: A, B, C:                                        AVERAGE
//	// A took   11,557,300   14,433,800   11,177,200   11,558,600   12,181,725
//	// B took    6,359,800    6,165,200    7,318,200    7,186,400    6,757,400
//	// C took    5,192,000    6,202,300    5,053,900    5,190,200    5,409,600
//	// which (as expected) indicates that the full monty is fastest.
//	//
//	// Swap the order of tests to assure myself it does not affect timings:
//	// C took   12,709,800    8,528,100    7,707,100    7,814,300    9,189,825
//	// B took   11,503,300    8,552,100   11,765,900   13,772,500   11,398,450
//	// A took   12,373,900   30,711,600    9,460,100    7,808,900   15,088,625
//	//
//	// WTF? Is it repeatable?
//	// C took    8,523,900    7,859,900    7,983,200    8,237,800    8,151,200
//	// B took    8,941,200    5,975,500   13,374,300   12,915,700   10,301,675
//	// A took   10,646,400    7,817,700    9,372,100   10,846,000    9,670,550
//	//
//	// WWTF? so is A, B, C repeatable?
//	// A took   11,461,200   11,526,000   11,707,400   11,548,600   11,560,800
//	// B took   10,603,600   10,482,600    9,466,600    7,406,600    9,489,850
//	// C took    7,158,800    5,122,100    5,139,100    5,133,600    5,638,400
//	//
//	// Is A, B, C repeatable repeatable?
//	// A took   12,457,100   11,924,500   12,402,400   12,428,700   12,303,175
//	// B took   12,117,800   10,923,100    8,682,600   13,509,300   11,308,200
//	// C took    5,737,100    6,426,300    7,245,900    7,996,600    6,851,475
//	//
//	// Now look at the "A times" verses A, B, C (they are nuts!) and all timings
//	// are now all over the place (unlike uniform A, B, C) -> WTWTFF?
//	//
//	// It is like the compiler is conspiring to prove anything you like, so long
//	// as you test possibilities logically: orginal, version 1, version 2!
//	// So the honest answer is I do not know, meaning that I failed to prove or
//	// disprove otherwise (I tested my test and it FAILED); but I THINK so.
//	// And I also think that my test MUST be defective somehow. But How? What
//	// have I done wrong now? Arrgghh!?!?!?!?!?
//	// ERGO: Lighten up: It seems to be dependant on How you look at it. Sigh.
//	//
// 	public static void main(String[] args) {
// 		try {
//			// Run tests 1 million times
//			final int HOW_MANY = 1000*1000;
//			// A timing variable
//			long nanos;
//
//			// The 3 test-data sets.
//			SudokuSet set1 = new SudokuSet()
//					, set2 = new SudokuSet()
//					, set3 = new SudokuSet();
//			// My three test sets.
//			SudokuSet a = new SudokuSet()
//					, b = new SudokuSet()
//					, c = new SudokuSet();
//			long c1, c2;
//			int indice;
//
//			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			// generate 3 test-data sets which intersect with each other.
//			// set1 contains 9 random indices.
//			for ( int i=0; i<9; ++i) {
//				do {
//					indice = RANDOM.nextInt(81);
//				} while ( set1.contains(indice) );
//				set1.add(indice);
//			}
//			// set2 contains 2 elements of set1, then random fill to 9
//			while ( set2.size() < 2 )
//				set2.add(set1.get(RANDOM.nextInt(9)));
//			while ( set2.size() < 9 )
//				set2.add(RANDOM.nextInt(81));
//			// set3 is the intersection of set1 and set2, then random fill to 9
//			set3.setAnd(set1, set2);
//			while ( set3.size() < 9 )
//				set3.add(RANDOM.nextInt(81));
//
//			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			// Check ONCE that both methods produce the same result
//			// ie check that both sets contain the same indices.
//			System.out.println("check set1="+set1);
//			System.out.println("check set2="+set2);
//			System.out.println("check set3="+set3);
//			a.set(set1);
//			a.and(set2);
//			a.and(set3);
//			System.out.println("check a="+a);
//
//			b.setAndAnd(set1, set2, set3);
//			System.out.println("check b="+b);
//			if ( !b.equals(a) )
//				throw new IllegalStateException("b != a");
//
//			c1 = set1.mask1 & set2.mask1 & set3.mask1;
//			c2 = set1.mask2 & set2.mask2 & set3.mask2;
//			c.set(c1, c2);
//			if ( !c.equals(a) )
//				throw new IllegalStateException("c != a");
//			System.out.println("check c="+c);
//
//			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			// A: original code timing: set; and; and
//			nanos = System.nanoTime();
//			for ( int i=0; i<HOW_MANY; ++i ) {
//				a.set(set1);
//				a.and(set2);
//				a.and(set3);
//			}
//			System.out.format("A took %,12d\n", System.nanoTime() - nanos);
//
//			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			// B: new code timing: setAndAnd
//			nanos = System.nanoTime();
//			for ( int i=0; i<HOW_MANY; ++i )
//				b.setAndAnd(set1, set2, set3);
//			System.out.format("B took %,12d\n", System.nanoTime() - nanos);
//
//			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			// C: full monty: c = set1 & set2 & set3
//			nanos = System.nanoTime();
//			for ( int i=0; i<HOW_MANY; ++i ) {
//				c1 = set1.mask1 & set2.mask1 & set3.mask1;
//				c2 = set1.mask2 & set2.mask2 & set3.mask2;
//			}
//			System.out.format("C took %,12d\n", System.nanoTime() - nanos);
//			// nb: if no reference c1 and c2 suspect compiler snips out loop!
////			System.out.println("again c="+SudokuSet.toString(c1, c2));
//
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
//	private static final Random RANDOM = Grid.RANDOM;
///*
//check set1=1 17 18 31 32 34 38 42 73
//check set2=1 13 26 29 34 36 38 39 64
//check set3=1 21 28 34 38 47 61 71 72
//check a=1 34 38
//check b=1 34 38
//A took   11,548,600
//B took    7,406,600
//C took    5,133,600
//check c=1 34 38
//*/

//	// Q: the bits var is intended to speed things up, but does it?
//	// A: looks like bits var is actually SLOWER!
// 	public static void main(String[] args) {
// 		try {
//			long start, sum;
//			int i,n, t;
//			final Random random = new Random();
//			final int HOW_MANY = 1000000; // how many times to call toArrayA
//			final int NUM_TIMES = 100; // how many times to repeat tests
//			final PrintStream out = System.out;
//
//			// create an Idx of 10..20 "random" indices
//			final Idx idx = new Idx();
//			for ( i=0,n=10+random.nextInt(11); i<n; ++i )
//				idx.add(random.nextInt(64));
//
//			start = System.nanoTime();
//			for ( t=0; t<NUM_TIMES; ++t ) {
//				// test the original version
//				sum = 0L;
//				for ( i=0; i<HOW_MANY; ++i )
//					for ( int indice : Idx.toArrayA(idx.a0, idx.a1, idx.a2) )
//						sum += indice;
//				System.out.format("original sum=%,d\n", sum);
//			}
//			out.format("original average took=%,d\n\n", (System.nanoTime()-start)/NUM_TIMES);
//
//			start = System.nanoTime();
//			for ( t=0; t<NUM_TIMES; ++t ) {
//				// test the modified version
//				sum = 0L;
//				for ( i=0; i<HOW_MANY; ++i )
//// Idx.toArrayA_MODIFIED is now the current Idx.toArrayA, ie change accepted
//					for ( int indice : Idx.toArrayA_MODIFIED(idx.a0, idx.a1, idx.a2) )
//						sum += indice;
//				System.out.format("modified sum=%,d\n", sum);
//			}
//			out.format("modified average took=%,d\n\n", (System.nanoTime()-start)/NUM_TIMES);
//
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
//
//static final int RABBITS = 0;
///*
//RUN 1
//modified average took=29,278,165
//original average took=30,006,282
//
//RUN 2
//original average took=33,661,588
//modified average took=31,683,276
//*/

//	// Q: What is the System LAF?
//	// A: null
// 	public static void main(String[] args) {
// 		try {
//			final String laf = AccessController.doPrivileged(new GetPropertyAction("swing.systemlaf"));
//			System.out.println("swing.systemlaf="+laf);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: What is Integer.MIN_VALUE
//	// A: Integer.MIN_VALUE=-2,147,483,648
// 	public static void main(String[] args) {
// 		try {
//			System.out.format("Integer.MIN_VALUE=%,d\n\n", Integer.MIN_VALUE);
// 		} catch (Exception ex) {
//			System.out.println(Log.me()+" exception");
//			System.out.flush();
// 			ex.printStackTrace(System.out);
//			System.out.flush();
// 		}
// 	}

//	// Q: Is this regex correct?
//	// A: Yes
// 	public static void main(String[] args) {
// 		try {
//			final String re = "S?T?U?V?WXYZ_Wing";
//			assert Tech.WXYZ_Wing.name().matches(re);
//			assert Tech.VWXYZ_Wing.name().matches(re);
//			assert Tech.UVWXYZ_Wing.name().matches(re);
//			assert Tech.TUVWXYZ_Wing.name().matches(re);
//			assert Tech.STUVWXYZ_Wing.name().matches(re);
//			assert !Tech.BigWings.name().matches(re);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: How to calculate the boxIndex from cell-indice?
//	// A: Look it up in Grid.BOX_OF
// 	public static void main(String[] args) {
// 		try {
//			final int N = REGION_SIZE;
//			final int R = SQRT;
//			final PrintStream out = System.out;
//			for ( int i=0; i<GRID_SIZE; ++i ) {
//				out.print(", ");
//				out.print((((i/N)/R)*R) + ((i%N)/R));
//				if ( (i+1)%REGION_SIZE == 0 )
//					out.println();
//			}
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: I know knuffink!
//	// A: Yep.
// 	public static void main(String[] args) {
// 		try {
//			System.out.println(Integer.toString(42, 2));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//
//	// Q: How do AlsChain's IAS's grow?
//	// A: Bigger arrays grow more slowly, ie in more increments
//	// size 1..13 : 0, 1, 2, 4, 7, 11, 17, 26, 40, 61, 92, 139, 209, 209
//	// size 14+   : 0, 1, 2, 3, 4, 5, 7, 9, 11, 14, 17, 21, 26, 32, 39, 47, 57, 69, 83, 100, 121, 146, 176, 212, 212
//
//	// java.util.ArrayList growth factor is 1.5, which seems like overkill.
//	// The IAS does not need to grow often, so it should grow well, mimimising
//	// the fat inherent in the system.
//	// formula: (int)(n*factor) + 1
//	// product: 0,1,2,4,7,11,14,17,21,26,32,39,47,58,70,85,103,124,149,179
//	private static double factor(final int n) {
//		if ( n > 13 ) // the larger the array the more the fat costs.
//			return 1.2D; // grow by just 20% at a time (reducing fat)
//		return 1.5D; // grow early by 50% at a time
//	}
//
//	// growIas grows the IAS[cnt] array, and returns a new array, so it is as if
//	// the AIOOBE never happened.
//	// @param size of the required cached array.
//	private static int grow(final int size, final int oldN) {
//		final double factor = factor(size);
//		assert factor > 1.0D;
//		final int newN = (int)(oldN*factor) + 1;
//		assert newN > oldN;
//		System.out.print(", "+newN);
//		return newN;
//	}
//
// 	public static void main(String[] args) {
// 		try {
//			final int size = 13;
//			int i = 0;
//			System.out.print(i);
//			while ( i < 200 ) {
//				i = grow(size, i);
//			}
//			System.out.println(", "+i);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

	// ========================================================================

//	// Q: HOW Big are the IASs?
//	// A: Big enough: 15,747 * 4 bytes per int = 63k
//	private static final int[] IAS_CAPACITY = {
//		   0, 88, 76, 87, 67, 65, 67, 70, 66, 64
//		, 60, 56, 77, 58, 51, 35, 45, 28, 28, 33
//		, 21, 22, 21, 21, 15, 15, 26, 14, 11,  8
// 		,  5, 13,  6, 10,  4,  8,  2,  4,  0,  2
//		,  2,  2,  0,  0,  0,  2,  0,  0,  0,  0
//		,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0
//		,  0,  0,  0,  0,  0
//	};
//
// 	public static void main(String[] args) {
// 		try {
//			int sum = 0;
//			for ( int i=0; i<IAS_CAPACITY.length; ++i ) {
//				if ( i>0 && i % 10 == 0 )
//					System.out.println();
//				System.out.format(", %3d", i*IAS_CAPACITY[i]);
//				sum += i*IAS_CAPACITY[i];
//			}
//			System.out.println();
//			System.out.println(sum);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//
//	// Q: What does 0..26 look like in binary
//	// A: Pretty sexy. Sigh.
// 	public static void main(String[] args) {
// 		try {
//			for ( int i=0; i<27; ++i )
//				System.out.format("%2d %5s\n", i, Integer.toBinaryString(i));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//
//	// Q: What are these binaries in decimal
//	// A: Pretty sexy. Sigh.
// 	public static void main(String[] args) {
// 		try {
//			final int[] a = {
//		  parseInt("000"
//				 + "011"
//				 + "011", 2) //        {0, 1, 3, 4}  2, 2
//		, parseInt("000"
//				 + "101"
//				 + "101", 2) //        {0, 2, 3, 5}  2, 1
//		, parseInt("000"
//				 + "110"
//				 + "110", 2) //        {1, 2, 4, 5}  2, 0
//		// row 2
//		, parseInt("011"
//				 + "000"
//				 + "011", 2) //        {0, 1, 6, 7}  1, 2
//		, parseInt("101"
//				 + "000"
//				 + "101", 2) //        {0, 2, 6, 8}  1, 1
//		, parseInt("110"
//				 + "000"
//				 + "110", 2) //        {1, 2, 7, 8}  1, 0
//		// row 3
//		, parseInt("011"
//				 + "011"
//				 + "000", 2) //        {3, 4, 6, 7}  0, 2
//		, parseInt("101"
//				 + "101"
//				 + "000", 2) //        {3, 5, 6, 8}  0, 1
//		, parseInt("110"
//				 + "110"
//				 + "000", 2) //        {4, 5, 7, 8} 0, 0
//			};
//			System.out.println(java.util.Arrays.toString(a));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//
//	// Q: What are these binaries in decimal
//	// A: Pretty sexy. Sigh.
// 	public static void main(String[] args) {
// 		try {
//	final int SLOT1 = parseInt("000"
//						   + "000"
//						   + "111", 2); // 7=1+2+4
//	final int SLOT2 = parseInt("000"
//						   + "111"
//						   + "000", 2); // 56=7<<3
//	final int SLOT3 = parseInt("111"
//						   + "000"
//						   + "000", 2); // 448=7<<6
//	// col slots
//	final int SLOT4 = parseInt("001"
//						   + "001"
//						   + "001", 2); // 73=1+8+64
//	final int SLOT5 = parseInt("010"
//						   + "010"
//						   + "010", 2); // 146=73<<1
//	final int SLOT6 = parseInt("100"
//						   + "100"
//						   + "100", 2); // 292=73<<2
//			final int[] a = {SLOT1,SLOT2,SLOT3,SLOT4,SLOT5,SLOT6};
//			System.out.println(java.util.Arrays.toString(a));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//	static boolean whinge(final String msg) {
//		return StdErr.whinge(msg);
//	}
//
//	static void complain(final String msg, final String title) {
//		whinge(msg);
//		showMessageDialog(null, msg, title, ERROR_MESSAGE);
//	}
//
//	// Q: Why will not my forskin stretch over a rain barrel?
//	// A: ?
// 	public static void main(String[] args) {
//		Exception x = new UnsolvableException("it is rooted");
// 		try {
//			complain("FUBAR: lastDitchRebuild: "+x+"\n"
//					+"It looks like this puzzle is ____ed. Try reloading it.\n"
//					+"If it ____s-up again try restarting Sudoku Explainer.\n"
//					+"Failing that this puzzle really is ____ing ____ed.\n"
//					+"Email it to the Kingon war fleet!\n"
//					, "Sudoku Explainer is Fubared!");
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//    /**
//     * Returns the number of one-bits in the two's complement binary
//     * representation of the specified {@code int} value.  This function is
//     * sometimes referred to as the <i>population count</i>.
//     *
//     * @param i the value whose bits are to be counted
//     * @return the number of one-bits in the two's complement binary
//     *     representation of the specified {@code int} value.
//     * @since 1.5
//     */
//    public static int bitCount(int i) {
//		// Figure 5-2, Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
//		//
//		// for example, lets say i is binary 1011 (decimal 11)
//		// 1. i = i - ((i >>> 1) & 0x55555555);
//		//    i = 1011 - (101 & 1010101010101010101010101010101)
//		//    -> 110 (decimal 6)
//        // 2. i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
//		//    i = (110 & ‭00110011001100110011001100110011‬) + (1 & ‭00110011001100110011001100110011‬)
//		//    i = (10) + (1)
//		//    -> 11
//        // 3. i = (i + (i >>> 4)) & 0x0f0f0f0f;
//		//    i = (11 + (11 >>> 4)) & ‭1111000011110000111100001111‬;
//		//    i = (11 + 0) & ‭1111000011110000111100001111‬;
//		//    -> 11
//		// 4. i = i + (i >>> 8);
//		//    i = 11 + (11 >>> 8);
//		//    i = 11 + (0)
//		//    -> 11
//        // 5. i = i + (i >>> 16);
//		//    i = 11 + (11 >>> 16);
//		//    i = 11 + (0);
//		//    -> 11
//        // 6. return i & 0x3f;
//		//    return 11 & ‭00111111‬;
//		//    -> 11
//		// So 1011 contains three 1s, and 11 in binary is three in decimal.
//        i = i - ((i >>> 1) & 0x55555555);
//        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
//        i = (i + (i >>> 4)) & 0x0f0f0f0f;
//        i = i + (i >>> 8);
//        i = i + (i >>> 16);
//        return i & 0x3f;
//    }
//
//	// Q: Why will not my forskin stretch over a rain barrel?
//	// A: ?
// 	public static void main(String[] args) {
// 		try {
//			final int i = 11;
//			System.out.format("%s\n", Integer.toBinaryString((i >>> 1)));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// Q: How many bean counters does it take to make a cup of coffee these
//	//    days (in Australia)?
//	// A: ? Hmm... ?
// 	public static void main(String[] args) {
// 		try {
//			final int i = Integer.MIN_VALUE;
//			System.out.format("%s\n", Integer.toBinaryString(i));
//			// -> 10, so >>> ignores the sign bit
//			System.out.format("%s\n", Integer.toBinaryString((i >>> 29)));
//			// -> 11111111111111111111111111111110, so >> drags sign bit down
//			System.out.format("%s\n", Integer.toBinaryString((i >> 29)));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//
//	/**
//	 * Explanation: These Enums know each other. A Basic may contain an Averter
//	 * and/or a Remover (both, one, or neither), and both Averter and Remover
//	 * contains a Basic. This is a chicken-egg problem. Interesting. Thats all.
//	 * Just interesting. I presume that Java creates undefined Enum types for
//	 * construction (sort of like a place-holder for a post, which is of course
//	 * a post, but is not painted yet, let alone carved, with nice bristlings,
//	 * nor a gannet, let alone a newt; sigh) then does a second pass to refine
//	 * atleast one of those types.
//	 */
//
//	private static enum Averter {
//		    GrometAverter	(Basic.Franga, T)
//		  , MarriageAverter	(Basic.Franga, F)
//		;
//		public final Basic basic;
//		public final boolean isC;
//		Averter(Basic basic, boolean isC) {
//			this.basic = basic;
//			this.isC = isC;
//		}
//		@Override
//		public String toString() {
//			return name()+": columbian="+isC;
//		}
//	}
//
//	private static enum Remover {
//		  EarwaxRemover		(Basic.Earwax,		F, T)
//		, PineappleRemover	(Basic.Pineapple,	T, F)
//		;
//		public final Basic basic;
//		public final boolean isC;
//		public final boolean isD;
//		Remover(Basic basic, boolean isC, boolean isD) {
//			this.basic = basic;
//			this.isC = isC;
//			this.isD = isD;
//		}
//		@Override
//		public String toString() {
//			return name()+": plastic="+isC+" tasty="+isD;
//		}
//	}
//
//	private static enum Basic {
//		  Earwax	(T, F, null, Remover.EarwaxRemover)
//		, Pineapple	(F, T, null, Remover.PineappleRemover)
//		, Franga	(F, F, Averter.GrometAverter, null)
//		;
//		public final boolean isA;
//		public final boolean isB;
//		public final Averter averter;
//		public final Remover remover;
//		public final String toString;
//		Basic(boolean isA, boolean isB, Averter averter, Remover remover) {
//			this.isA = isA;
//			this.isB = isB;
//			this.averter = averter;
//			this.remover = remover;
//			// construct toString ONCE (All my enums are immutable)
//			String s = name()+": horse="+isA+" chucker="+isB;
//			if ( averter != null )
//				s += " averter=["+averter+"]";
//			if ( remover != null )
//				s += " remover=["+remover+"]";
//			this.toString = s;
//		}
//		@Override
//		public String toString() {
//			return toString;
//		}
//	}
//
//	// Q: How is it possible to include an as yet undefined Enum type in
//	//    another Enum? Maybe its the same for classes? Object then specify?
//	// A: BFIIK?
// 	public static void main(String[] args) {
// 		try {
//			for ( Basic b : Basic.values() )
//				System.out.format("%s\n", b);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

//	// ========================================================================
//
//	// Q: Is greater than (>) faster than not equal to (!=)
//	// G: I think greater than (>) is a simpler faster binary operation.
//	//    But I suspect it may depend on which you test first. Sigh.
//	// A: YMMV. I have failed to determine this empirically. Go with my gut!
//	// ACTUAL A: It makes ____all difference, so do not worry about it!
//	// NB: I take results only from those random runs where a > b, obviously,
//	// else != will win every time, but a substantial margin.
//	// FYI: COUNTS and TIMES were static, now local -> ~40% time drop
//	// So, are you actually testing what you think you are testing?
//	// Does it matter?
// 	public static void main(String[] args) {
// 		try {
//			int i;
//			long start;
//			long counts0=0L, counts1=0L;
//			long times0, times1;
//			final Random random = new Random();
//			final int a = random.nextInt();
//			final int b = random.nextInt();
//			final int howMany = 1024 * 1024 * 1024; // A BILLION
//			// Greater than: >
//			start = System.nanoTime();
//			for ( i=0; i<howMany; ++i )
//				if ( a > b )
//					++counts1;
//			times1 = System.nanoTime() - start;
//
//			// Not equals: !=
//			start = System.nanoTime();
//			for ( i=0; i<howMany; ++i )
//				if ( a != b )
//					++counts0;
//			times0 = System.nanoTime() - start;
//
//			// output
//			System.out.format("              %13s\t%13s\n", "TIMES", "COUNTS");
//			System.out.format("Greater than: %,13d\t%,13d\n", times1, counts1);
//			System.out.format("Not equals  : %,13d\t%,13d\n", times0, counts0);
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}
///*
//                      TIMES	       COUNTS
//STATIC
//Greater than:    50,703,000	1,073,741,824
//Not equals  :    46,779,800	1,073,741,824
//Greater than:    47,157,800	1,073,741,824
//Not equals  :    52,057,100	1,073,741,824
//Greater than:    47,921,600	1,073,741,824
//Not equals  :    46,567,400	1,073,741,824
//
//LOCAL
//Greater than:    29,813,500	1,073,741,824
//Not equals  :    29,138,400	1,073,741,824
//Greater than:    31,128,000	1,073,741,824
//Not equals  :    26,953,100	1,073,741,824
//Greater than:    31,108,700	1,073,741,824
//Not equals  :    28,755,500	1,073,741,824
// */

//	// ========================================================================
//
//	// Q: What REs match start and end of a puzzle in a batch log.
//	// A: Answers here-in, verified by asserts, to be sure that each puzzle:
//	//    (a) starts at the beginning (hintNumber==1).
//	//    (b) is being processed (puzzleNumber==prevPN+1), and
//	// This is a regularity run: totes dependant on the logFile format,
//	// which was DESIGNED to facilitate this sort of s__t.
// 	public static void main(String[] args) {
// 		try {
//			final File logFile = new File(IO.HOME+"top1465.d5.2023-06-20.20-38-29.log");
//			// beginningOfPuzzleRegularExpression "1/1#" .. "63/1465#"
//			final Matcher bopRE = Pattern.compile("^\\d+/\\d+#.*").matcher("");
//			// endOfPuzzleRegularExpression: "     1,805,873,600" .. "   140,404,100,500"
//			final Matcher eopRE = Pattern.compile("^ {3,11}[\\d,]{0,13}\\d\\d\\d*$").matcher("");
//			String line;
//			int hintNumber, prevPN, puzzleNumber=0, slash;
//			try ( final BufferedReader reader = new BufferedReader(new FileReader(logFile)) ) {
//				PARSER: while ( (line=reader.readLine()) != null ) {
//					if ( !bopRE.reset(line).matches() ) {
////						System.out.println("#"+line);
//						continue;
//					}
//					System.out.println(line);
//					slash = line.indexOf('/');
//					hintNumber = Integer.parseInt(line.substring(0, slash));
//					assert hintNumber == 1;
//					prevPN = puzzleNumber;
//					puzzleNumber = Integer.parseInt(line.substring(slash+1, line.indexOf('#')));
//					assert puzzleNumber == prevPN + 1;
//					while ( (line=reader.readLine()) != null )
//						if ( eopRE.reset(line).matches() ) {
////							System.out.println("@"+line);
//							continue PARSER;
//						}
//				}
//			}
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

// ========================================================================

//	// Q: how to use System.arraycopy to omit one src element?
// 	public static void main(String[] args) {
// 		try {
//			final int[] src = {1,2,3,4,5,6,7,8,9};
//			final int[] dst = new int[src.length-1];
//			System.arraycopy(src, 0, dst, 0, 4);
//			System.arraycopy(src, 5, dst, 4, dst.length-4);
//			System.out.println(java.util.Arrays.toString(dst));
// 		} catch (Exception ex) {
// 			ex.printStackTrace(System.out);
// 		}
// 	}

// ========================================================================

//public static void main(String[] args) {
//	try {
//		int i;
//		long start;
//		long times0, times1;
//		long counts0=0L, counts1=0L;
//		final int howMany = 1024 * 1024; // A MILLION
//		// generate a "random" Idx
//		final Random random = new Random();
//		final int a = random.nextInt(1<<27);
//		final int b = random.nextInt(1<<27);
//		final int c = random.nextInt(1<<27);
//		final int[] indices = new Idx(a, b, c).toArrayNew();
//
//		// make it compile the Idx class
//		for ( i=0; i<12; ++i ) {
//			IdxI.of(indices); // switch
//			IdxI.of2(indices); // array
//		}
//
//		// switch
//		start = System.nanoTime();
//		for ( i=0; i<howMany; ++i )
//			counts1 += IdxI.of(indices).size();
//		times1 = System.nanoTime() - start;
//
//		// array
//		start = System.nanoTime();
//		for ( i=0; i<howMany; ++i )
//			counts0 += IdxI.of2(indices).size();
//		times0 = System.nanoTime() - start;
//
//		// output
//		System.out.format("        %13s\t%13s\n", "TIMES", "COUNTS");
//		System.out.format("switch: %,13d\t%,13d\n", times1, counts1);
//		System.out.format("array : %,13d\t%,13d\n", times0, counts0);
//	} catch (Exception ex) {
//		ex.printStackTrace(System.out);
//	}
//}
///*
//array :    77,653,300	   39,845,888
//switch:    42,863,900	   39,845,888
//
//array :    92,221,900	   50,331,648
//switch:    52,711,300	   50,331,648
//
//array :    80,140,400	   39,845,888
//switch:    42,165,100	   39,845,888
//
//REVERSE ORDER
//switch:    54,892,200	   42,991,616
//array :    71,823,000	   42,991,616
//
//switch:    54,064,500	   39,845,888
//array :    77,077,100	   39,845,888
//
//switch:    54,615,800	   45,088,768
//array :    75,477,000	   45,088,768
// */

// ========================================================================

	// Q: How many bitwise-ors in one statement?
	// A: More than enough.
	// WTF: This does NOT seem to be the case in AlsFinder!
 	public static void main(String[] args) {
 		try {
			int zer0=0,zer1=0,zer2=0;
			int uno0=0,uno1=0,uno2=0;
			int duo0=0,duo1=0,duo2=0;
			int tri0=0,tri1=0,tri2=0;
			int qud0=0,qud1=0,qud2=0;
			int pen0=0,pen1=0,pen2=0;
			int hex0=0,hex1=0,hex2=0;
			int sep0=0,sep1=0,sep2=0;
			int oct0=0,oct1=0,oct2=0;
			int non0=0,non1=0,non2=0;
			int dec0=0,dec1=0,dec2=1;
			if ( (zer0|zer1|zer2 | uno0|uno1|uno2 | duo0|duo1|duo2 | tri0|tri1|tri2 | qud0|qud1|qud2 | pen0|pen1|pen2 | hex0|hex1|hex2 | sep0|sep1|sep2 | oct0|oct1|oct2 | non0|non1|non2 |dec0|dec1|dec2) > 0 ) {
				System.out.println("Yep");
			} else {
				System.out.println("Nope");
			}
 		} catch (Exception ex) {
 			ex.printStackTrace(System.out);
 		}
 	}

}
