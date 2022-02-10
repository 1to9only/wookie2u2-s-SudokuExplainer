/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.solver.hinters.align.CellSet;
import diuf.sudoku.Ass;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Pots.EMPTY;
import static diuf.sudoku.utils.Frmt.NL;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.SP2;

/**
 * Static utility methods for debugging.
 *
 * @author Keith Corlett 2013
 */
public final class Debug {

	public static final boolean IS_ON = false; // @check false

	/** @see Log.out, DevNull.out */
	public static PrintStream out = System.out;

	// switch this on/off to activate/deactive your breakpoints. It's faster!
	public static boolean isOn;

	/**
	 * breakpoint() is a no-op for conditional breakpoints. The JIT compiler
	 * erases calls to no-op's, just in case you leave one in.
	 * <p>
	 * Netbeans (which I use) and other IDE's now have conditional breakpoints,
	 * but this "in code" technique is still MUCH faster, so I still use it
	 * routinely; that way I don't have to think "How many times does this run"
	 * when creating a conditional breakpoint that otherwise is never tripped
	 * because Netbeans conditional breakpoint implementation is so slow it
	 * takes for ever to reach the break-point condition.
	 * <p>
	 * I expected lambdas to expedite CBP's, but they haven't. yet Sigh.
	 */
	public static void breakpoint() { // @check No usages (Find Usages)
	}

	private static boolean startsWith(String s, String[] targets) {
		for ( String t : targets )
			if ( s.startsWith(t) )
				return true;
		return false;
	}
	public static boolean isCaller(int n, String... targets) {
		int i = 0;
		for ( StackTraceElement e : Thread.currentThread().getStackTrace() )
			if ( startsWith(e.toString(), targets) )
				return true;
			else if (++i > n )
				return false;
		return false;
	}
	public static StackTraceElement getCaller(int n, String... targets) {
		int i = 0;
		for ( StackTraceElement e : Thread.currentThread().getStackTrace() )
			if ( startsWith(e.toString(), targets) )
				return e;
			else if (++i > n )
				return null;
		return null;
	}

	public static boolean isClassNameInTheCallStack(int n, String className) {
		return isInTheCallStack(n+1, (e)->e.getClassName().contains(className));
	}

	public static boolean isMethodNameInTheCallStack(int n, String methodName) {
		return isInTheCallStack(n+1, (e)->e.getMethodName().contains(methodName));
	}

	// s.containsAny(targets)
	private static boolean containsAny(String s, String[] targets) {
		for ( String t : targets )
			if ( s.contains(t) )
				return true;
		return false;
	}
	public static boolean isMethodNameInTheCallStack(int n, String[] methodNames) {
		return isInTheCallStack(n+1, (e)->containsAny(e.getMethodName(), methodNames));
	}

	public static boolean isInTheCallStack(int n, String className, String methodName) {
		return isInTheCallStack(n+1, (e) -> e.getClassName().equals(className)
									   && e.getMethodName().equals(methodName));
	}

	public static boolean isInTheCallStack(int n, IFilter<StackTraceElement> filter) {
		final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		// 3 is getStackTrace, me, and my caller, which are all unsearchable
		for ( int i=3,N=MyMath.min(n, stack.length); i<N; ++i )
			if ( filter.accept(stack[i]) )
				return true;
		return false;
	}

	/**
	 * Parse {@link Frmt#csv(java.util.List)} back into the actual regions in
	 * the given Grid.
	 *
	 * @param csv for example: <tt>"row 2, row 5, row 8, row 9"</tt>
	 * @param grid the Grid to get regions from.
	 * @return a {@code new ARegion[]} of these regions in the given grid
	 */
	public static ARegion[] parse(String csv, Grid grid) {
		if ( csv==null || csv.length()<5 )
			return new ARegion[0]; // an empty array
		String[] ids = csv.split(CSP);
		ARegion[] regions = new ARegion[ids.length];
		int i = 0;
		for ( String id : ids )
			regions[i++] = grid.region(id);
		return regions;
	}

	/**
	 * Creates a new boolean[27] and sets the given regions to true.
	 * @param regions An array of regions to be marked as used.
	 * @return a new boolean[27] with the given regions true.
	 */
	public static boolean[] usedRegions(ARegion[] regions) {
		boolean[] result = new boolean[27];
		for ( ARegion r : regions )
			result[r.index] = true;
		return result;
	}

	/**
	 * Creates a new boolean[27] and sets the given regions to true.
	 * @param regions CSV of regions that're used: "box 2, row 3, col H"
	 * @param grid the Grid to get regions from.
	 * @return a new boolean[27] with the given regions true.
	 */
	public static boolean[] usedRegions(String regions, Grid grid) {
		return usedRegions(parse(regions, grid));
	}

	public static void println(String message) {
		if(IS_ON) out.println(message);
	}

	public static void println(Object object) {
		if(IS_ON) out.println(object);
	}

	public static void println() {
		if(IS_ON) out.println();
	}

	public static void print(String message) {
		if(IS_ON) out.print(message);
	}

	public static void format(String format, Object... args) {
		if(IS_ON) out.format(format, args);
	}

	// used internally so doesn't respect IS_ON
	public static void indentf(char c, int depth, String fmt, Object... args) {
		out.print(Debug.repeat(c, depth));
		out.format(fmt, args);
	}

	public static String indent(int howMany) {
		return repeat(' ', howMany);
	}

	public static String repeat(char c, int howMany) {
		final StringBuilder sb = new StringBuilder(howMany);
		for ( int i=0; i<howMany; ++i )
			sb.append(c);
		return sb.toString();
	}

	public static void dumpAncestors(List<Ass> targets) {
		out.println();
		out.println("dumpAncestors:");
		for ( Ass a : targets )
			recursivelyDumpAncestors(SP2, a);
		out.println();
	}
	public static void recursivelyDumpAncestors(String spaces, Ass a) {
		out.format("%s%s cause=%s; explanation=%s nestedChain=%s\n"
				, spaces, a, a.cause, a.explanation, a.nestedChain);
		spaces += SP2;
		if ( a.hasParents() ) // fast enough for Debug
			for ( Ass p : a.parents )
				recursivelyDumpAncestors(spaces, p);
	}

	// for debugging only DON'T DELETE UNLESS YOU REALLY REALLY WANT TO
	public static boolean dumpValuesEffectsAncestors(IAssSet[] valueEffects, Ass target) {
		out.format("VALUE EFFECTS target=%s:%s", target, NL);
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			if ( valueEffects[v] != null ) {
				out.println("VALUE: "+v);
				if ( valueEffects[v].isEmpty() )
					out.println(EMPTY);
				else {
					boolean found = false;
					for ( Ass a : valueEffects[v] ) {
						if ( a == target ) { // we want THE instance
							found = true;
							recursivelyDumpAncestors(SP2, a);
							break;
						}
					}
					if ( !found ) { // NB: it'll probably go through here
						out.println("target "+target+" not found in:");
						for ( Ass a : valueEffects[v] )
							recursivelyDumpAncestors(SP2, a);
					}
				}
				out.println();
			}
		}
		out.println();
		return false; // used by assert
	}

	public static boolean atleastOneIsTrue(boolean... bools) {
		for ( boolean bool : bools )
			if ( bool )
				return true;
		return false;
	}

	/**
	 * diff two Exclusion Maps. Left is before removals. Right is after.
	 * Dumps the elements that are only in the left map to stdout.
	 * @param lMap left {@code HashMap<Cell, CellSet>}
	 * @param rMap right {@code HashMap<Cell, CellSet>}
	 */
	public static void diff(HashMap<Cell, CellSet> lMap, HashMap<Cell, CellSet> rMap) {
		out.format("HDR: WAS=%s\n", Frmu.excludersMap(lMap));
		out.format("HDR: NOW=%s\n", Frmu.excludersMap(rMap));
		for ( Cell key : lMap.keySet() ) { // leftKey
			CellSet lv = lMap.get(key);
			CellSet rv = rMap.get(key);
			if ( rv == null )
				out.format("ALL: %-12s WAS=%d:[%s]\n", key, lv.size(),Frmu.csv(lv));
			else if ( lv.size() == rv.size() )
				out.format("NON: %-12s BOT=%d:[%s]\n", key, lv.size(),Frmu.csv(lv));
			else {
				CellSet xv = new CellSet(lv);  xv.removeAll(rv);
				out.format("SOM: %-12s WAS=%d:[%s]\n", key, lv.size(),Frmu.csv(lv));
				out.format("               - NOW=%d:[%s]\n", rv.size(),Frmu.csv(rv));
				out.format("               = GON=%d:[%s]\n", xv.size(),Frmu.csv(xv));
			}
		}
	}

	// =========== these just here coz I think they might be useful ===========

	// prints the maybes to stdout in standard format
 	public static void dumpAllCellsMaybes(Grid grid) {
 		try {
 			boolean isFirst = true;
 			for ( Cell cell : grid.cells ) {
 				if ( isFirst )
 					isFirst = false;
 				else
 					out.print(cell.i%REGION_SIZE==0 ? "\n" : ", ");
 				out.format("%-6s", cell.maybes);
 			}
 			out.println();
 		} catch (Exception ex) {
 			ex.printStackTrace(out);
 		}
 	}
// -     , 1589  , 568   , 1269  , 13569 , 23569 , -     , 1235  , 123
// 14569 , -     , 456   , 1469  , -     , 34569 , 156   , -     , 13
// 1456  , 145   , -     , 1246  , 156   , -     , 12567 , 1257  , -
// 2489  , 4789  , 2478  , -     , 1689  , 469   , -     , 1247  , 12478
// 3458  , -     , 4578  , 148   , -     , 34    , 1578  , -     , 1478
// 234589, 4589  , -     , 489   , 389   , -     , 258   , 245   , -
// 124568, 14578 , 245678, -     , 568   , 256   , -     , 1247  , 12478
// 1258  , -     , 2578  , 2789  , -     , 259   , 1278  , -     , 1278
// 2468  , 478   , -     , 2678  , 68    , -     , 278   , 2347  , -

  	// prints all cells maybes to stdout in binary format.
 	public static void dumpAllCellsMaybesBinary(Grid grid) {
 		try {
 			boolean isFirst = true;
 			for ( Cell cell : grid.cells ) {
 				if ( isFirst )
 					isFirst = false;
 				else
 					out.print(cell.i%REGION_SIZE==0 ? "\n" : ", ");
 				String s = Integer.toBinaryString(cell.maybes);
 				indentf('0', REGION_SIZE-s.length(), "%s", s);
 			}
 			out.println();
 		} catch (Exception ex) {
 			ex.printStackTrace(out);
		}
 	}
// 000000000, 110010001, 010110000, 100100011, 100110101, 100110110, 000000000, 000010111, 000000111
// 100111001, 000000000, 000111000, 100101001, 000000000, 100111100, 000110001, 000000000, 000000101
// 000111001, 000011001, 000000000, 000101011, 000110001, 000000000, 001110011, 001010011, 000000000
// 110001010, 111001000, 011001010, 000000000, 110100001, 100101000, 000000000, 001001011, 011001011
// 010011100, 000000000, 011011000, 010001001, 000000000, 000001100, 011010001, 000000000, 011001001
// 110011110, 110011000, 000000000, 110001000, 110000100, 000000000, 010010010, 000011010, 000000000
// 010111011, 011011001, 011111010, 000000000, 010110000, 000110010, 000000000, 001001011, 011001011
// 010010011, 000000000, 011010010, 111000010, 000000000, 100010010, 011000011, 000000000, 011000011
// 010101010, 011001000, 000000000, 011100010, 010100000, 000000000, 011000010, 001001110, 000000000

  	// prints all the regions indexes of all values to stdout in standard format
 	public static void dumpRegionsIndexesOfAllValues(Grid grid) {
 		try {
 			for ( int v=1; v<VALUE_CEILING; ++v ) {
 				out.println("value = "+v);
 				for ( ARegion r : grid.regions ) {
					out.format(" %s=%-8s", r.id, r.ridx[v]);
 					if ( (r.index+1)%REGION_SIZE == 0 )
 						out.println();
 				}
 				out.println();
 			}
 		} catch (Exception ex) {
 			ex.printStackTrace(out);
 		}
 	}
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

  	// prints all the regions indexes of all values to stdout in binary-format
 	public static void dumpRegionsIndexesOfAllValuesBinary(Grid grid) {
 		try {
 			for ( int v=1; v<VALUE_CEILING; ++v ) {
 				out.println("value = "+v);
 				for ( ARegion r : grid.regions ) {
 					out.format("    %s=", r.id);
 					String s = Integer.toBinaryString(r.ridx[v].bits);
 					indentf('0', REGION_SIZE-s.length(), "%s", s);
 					if ( (r.index+1)%REGION_SIZE == 0 )
 						out.println();
 				}
 				out.println();
 			}
 		} catch (Exception ex) {
 			ex.printStackTrace(out);
 		}
 	}
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

	public static void dump(String label, Iterable<?> c) {
		System.out.println(label);
		for ( Object e : c )
			System.out.println(e);
	}

	private Debug() {} // never used
}
