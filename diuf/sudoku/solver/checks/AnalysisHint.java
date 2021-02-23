/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.UsageMap;
import diuf.sudoku.solver.Usage;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Pair;
import java.util.Map;


/**
 * A warning hint from {@link diuf.sudoku.solver.LogicalAnalyser} contains
 * an approximate rating of the sudoku, and the list of hints that have
 * been used to solve it. The actual solution is not shown, and the grid is
 * not modified by applying this hint.
 * @see diuf.sudoku.solver.checks.Analyser
 */
public final class AnalysisHint extends AWarningHint implements IActualHint {

	public final UsageMap usageMap;

	public double maxDifficulty;

	public AnalysisHint(AHinter hinter, UsageMap usageMap) {
		super(hinter);
		assert !usageMap.isEmpty();
		this.usageMap = usageMap;
	}

	private void appendSubHints(StringBuilder mySB
			, Map<String, Pair<Integer, Double>> subHints) {
		// append one line per hint in the hints Map.
		for ( String hintName : subHints.keySet() ) {
			// first append 5 leading spaces (to indent the "sub-hints".
			mySB.append("     ");
			// append right-aligned 1, 2 or 3 digit numHints (AFAIK max is 2)
			Pair<Integer, Double> tuple = subHints.get(hintName);
			int numHints = tuple.a;
			double difficulty = tuple.b;
			if(numHints < 100) mySB.append(' ');
			if(numHints < 10) mySB.append(' ');
			// append the line
			mySB.append(numHints).append(" x ").append(hintName).append(" ")
					.append(Frmt.dbl(difficulty)).append(NL);
		}
	}

	private void appendUsage(StringBuilder mySB, IHinter myHinter, Usage usage) {
		String difficulty = usage.maxDifficulty>0.0
				? " "+Frmt.dbl(usage.maxDifficulty) : "";
		int numHints = usage.numHints;
		if ( numHints > 0 ) {
			if(numHints < 10) mySB.append(' ');
			mySB.append(numHints).append(" x ")
			  .append(myHinter).append(difficulty).append(NL);
			// if this hinter produced more than one type of hint
			// then append a count of each hint type
			if ( usage.subHintsMap.size() > 1 )
				appendSubHints(mySB, usage.subHintsMap);
		}
	}

	// I Made appendUsageMap public returns mySB so that I can log a formatted
	// summary of an AnalysisHint without all the HTML-extras. Note that you'll
	// need to cast me to AnalysisHint to access this method.
	public StringBuilder appendUsageMap(StringBuilder mySB) {
		Usage usage;
		for ( IHinter myHinter : usageMap.keySet() )
			if ( (usage=usageMap.get(myHinter)) == null )
				// you can only get here when the catch-all was executed, coz
				// the puzzle wasn't solved with the selected techniques, which
				// most commonly means you just need to select more techniques;
				// and/or (but less commonly) you broke a bloody hinter (again)!
				mySB.append(" 1 x catch-all usage data is unavailable (18.96)").append(NL)
				    .append(" 1 x enable some bloody hinters ya putz! (42.42)").append(NL)
				    .append(" 1 x or you've broken a bloody hinter!   (86.69)").append(NL);
			else
				appendUsage(mySB, myHinter, usage);
		return mySB;
	}

	@Override
	public String toHtmlImpl() {
		StringBuilder mySB = Frmt.getSB();
		mySB.append(NL).append("<pre>").append(NL);
		appendUsageMap(mySB);
		mySB.append("</pre>").append(NL);
		return Html.produce(this, "AnalysisHint.html"
				, Frmt.dbl(getDifficulty())
				, mySB
		);
	}

	@Override
	public double getDifficulty() {
		if ( maxDifficulty == 0.0D )
			this.maxDifficulty = usageMap.getMaxDifficulty();
		return maxDifficulty;
	}

	@Override
	public String toStringImpl() {
		return "Sudoku Rating";
	}
}