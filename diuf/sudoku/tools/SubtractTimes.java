/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

/**
 * SubtractTimes prints the difference between two times, in minutes:secs.
 * @author Keith Corlett 2020 Jan
 */
public class SubtractTimes {
	public static void main(String[] args) {
//		long a = parse("274:15"); 
		long a = parse("23:49");
		long b = parse("17:48");
		System.out.println(format(a-b));
		System.out.format("%4.2f%%\n", pct(a-b, a));
	}
	
	private static double pct(long howMany, long of) {
		return howMany==0L||of==0L
				? 0.00D
				: (double)howMany / (double)of * 100.00D;
	}

	private static long parse(String s) {
		String[] a = s.split(":");
		return Integer.parseInt(a[0])*60 + Integer.parseInt(a[1]);
	}

	private static String format(long l) {
		int m = (int)(l / 60);
		int s = (int)(l % 60);
		return String.format("%d:%02d", m, s);
	}
}
