/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.UsageMap;
import diuf.sudoku.solver.Usage;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.SPACE;
import static diuf.sudoku.utils.Frmt.enspace;
import static diuf.sudoku.utils.Frmt.frmtDbl;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.utils.MyStrings.BIG_BUFFER_SIZE;
import static diuf.sudoku.utils.MyStrings.bigSB;
import diuf.sudoku.utils.Pair;
import java.util.Map;


/**
 * A warning hint from {@link diuf.sudoku.solver.LogicalAnalyser} contains
 * a difficulty rating of the Sudoku, and the hints used to solve it. The
 * solution is not shown, and the grid is not modified by applying this hint.
 *
 * @see diuf.sudoku.solver.checks.Analyser
 */
public final class AnalysisHint extends AWarningHint  {

	public final UsageMap usageMap;

	// The GUI reads these back to display them, and log them.
	public double maxDifficulty;
	public double ttlDifficulty;

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
			mySB.append(numHints).append(" x ").append(hintName).append(SPACE)
					.append(frmtDbl(difficulty)).append(NL);
		}
	}

	private void appendUsage(StringBuilder sb, IHinter h, Usage u) {
		final String difficulty;
		if ( u.maxDifficulty > 0.0 )
			difficulty = SPACE+frmtDbl(u.maxDifficulty);
		else
			difficulty = EMPTY_STRING;
		int numHints = u.hints;
		if ( numHints > 0 ) {
			sb.append(enspace(numHints, 2)).append(" x ")
			  .append(h).append(difficulty).append(NL);
			// if this hinter produced more than one type of hint
			// then append a count of each type of hint
			if ( u.subHintsMap.size() > 1 )
				appendSubHints(sb, u.subHintsMap);
		}
		// update the hint's difficulty feilds
		if ( u.maxDifficulty > maxDifficulty )
			maxDifficulty = u.maxDifficulty;
		ttlDifficulty += u.ttlDifficulty;
	}

	// Cast me to AnalysisHint to access this method.
	public StringBuilder appendUsageMap() {
		return appendUsageMap(bigSB());
	}

	// appendUsageMap takes mySB: a formatted summary of AnalysisHint.
	public StringBuilder appendUsageMap(StringBuilder sb) {
		Usage usage;
		for ( java.util.Map.Entry<IHinter,Usage> e : usageMap.entrySet() )
			if ( (usage=e.getValue()) == null ) {
				// no usage only happens with the catch-all
				sb.append(" 1 x catch-all usage data is unavailable (18.96)").append(NL)
				  .append(" 1 x enable some bloody hinters ya putz  (42.42)").append(NL)
				  .append(" 1 x or you've broken a bloody hinter    (86.69)").append(NL);
			    break; // NONE SHALL PASS
			} else
				appendUsage(sb, e.getKey(), usage);
		return sb;
	}

	@Override
	public String toHtmlImpl() {
		// nb: MyStrings.bigSB is already used!
		final StringBuilder mySB = new StringBuilder(BIG_BUFFER_SIZE);
		mySB.append(NL).append("<pre>").append(NL);
		appendUsageMap(mySB);
		mySB.append("</pre>").append(NL);
		return Html.produce(this, "AnalysisHint.html"
				, frmtDbl(getDifficulty()), mySB);
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