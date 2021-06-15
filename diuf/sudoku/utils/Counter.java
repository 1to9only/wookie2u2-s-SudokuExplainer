/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Settings;


/**
 * Counter helps with filtering in the Aligned*Exclusion classes.
 * <p>
 * You do:<ul>
 * <li>Before your filter you count: {@code A8E_val.count(val);}
 * <li>Then you filter manually: {@code if (val<16 || val>42) continue;}
 * <li>Then you increment pass: {@code ++A8E_val.pass;}
 * <li>Then when you create a hint you hit me: {@code A8E_val.hit();}
 * <li>Then in the report method you print me: {code A8E_val.print();}
 * </ul>
 * This class eliminates lots of patternated code.
 * <p>
 * @author Keith Corlett
 */
public class Counter {

	public enum Type {
		  Both // the default, so it's the first (0) entry
		, Pass
		, MinMax
	}
	
	public final Type type; // all lower case please
	private final String name; // all lower case please

	public long cnt=0L; // it saves time to just increment it externally
	public long pass=0L; // it saves time to just increment it externally
	private int value=0, min=Integer.MAX_VALUE, max=0;
	private long hitCnt=0;
	private int hitMin=Integer.MAX_VALUE, hitMax=0;
	
	private boolean isDouble = false;
	private double dVal=0D, dMin=Double.MAX_VALUE, dMax=0D;
	private double hitDMin=Double.MAX_VALUE, hitDMax=0D;

	/**
	 * Constructs a new Counter.
	 * @param name use all lower case names if you're saving hitMin and hitMax
	 * to the registry, which inserts a slash in the name before /Each /Capital
	 * /Letter. At least it's not a bloody backslash.
	 */
	public Counter(String name) {
		this(name, Type.Both);
	}

	/**
	 * Constructs a new Counter.
	 * @param name use all lower case names if you're saving hitMin and hitMax
	 * to the registry, which inserts a slash in the name before /Each /Capital
	 * /Letter. At least it's not a bloody backslash.
	 * @param type one of the Counter.Type enums
	 */
	public Counter(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	public void report() {
		switch (type) {
		case Pass: printPass(); break;
		case MinMax: printMinMax(); break;
		default: print();
		}
	}

// The cnt field is now public to be incremented externally, coz that's faster.
// 4.2 BILLION method calls take a wee while.
//	@Deprecated
//	public void count() {
//		++cnt;
//	}

	public void count(int val) {
		++cnt;
		this.value = val;
		if(val<min) min=val;
		if(val>max) max=val;
	}

	public void count(double val) {
		isDouble = true;
		++cnt;
		this.dVal = val;
		if(val<dMin) dMin=val;
		if(val>dMax) dMax=val;
	}

// The pass field is now public to be incremented externally, coz that's faster.
// 4.2 BILLION method calls take a wee while.
//  @Deprecated
//	public void pass() {
//		++pass;
//	}

	public void hit() {
		++hitCnt;
		if ( isDouble ) {
			double val = this.dVal;
			if(val<hitDMin) hitDMin=val;
			if(val>hitDMax) hitDMax=val;
		} else {
			int val = this.value;
			if(val<hitMin) hitMin=val;
			if(val>hitMax) hitMax=val;
		}
	}

	public void print() {
		long skip = cnt - pass;
		if ( isDouble ) {
			Log.teef("// %s min=%,5.3f/%,5.3f max=%,5.3f/%,5.3f pass %,d of %,d skip %,d = %4.2f%%\n"
					// nb: the -/+ 0.001 avoids rounding error cutting off the min/max.
					// remember that it's already done (now), and don't do it again.
					, name, hitDMin-0.001, dMin, hitDMax+0.001, dMax, pass, cnt, skip, Log.pct(skip,cnt));
		} else {
			Log.teef("// %s min=%,d/%,d max=%,d/%,d pass %,d of %,d skip %,d = %4.2f%%\n"
					, name, hitMin, min, hitMax, max, pass, cnt, skip, Log.pct(skip,cnt));
		}
	}

	public void printMinMax() {
		Log.teef("// %s min=%,d/%,d max=%,d/%,d\\n"
				, name, hitMin, min, hitMax, max);
	}

	public void printPass() {
		long skip = cnt - pass;
		Log.teef("// %s pass %,d of %,d skip %,d = %4.2f%%\n"
				, name, pass, cnt, skip, Log.pct(skip,cnt));
	}

	public void printPercentageOf(long of) {
		Log.teef("// %s = %,d of %,d = %4.2f%%\n"
				, name, cnt, of, Log.pct(cnt,of));
	}

	public void save() {
		// if hit() was called and hitMax was set to a valid value.
		// I really don't want crap values to be stored in the registry.
		if ( hitCnt>0  && hitMax!=0 ) {
			Settings.THE.putInt(name+"Min", hitMin);
			Settings.THE.putInt(name+"Max", hitMax);
		}
	}

}
