/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.UsageMap;
import diuf.sudoku.solver.Usage;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.dbl;
import static diuf.sudoku.utils.Frmt.enspace;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Pair;
import java.util.Map;
import static diuf.sudoku.utils.Frmt.SP;
import static diuf.sudoku.utils.MyStrings.BIG_BFR_SIZE;


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

	@Override
	public double getDifficulty() {
		if ( maxDifficulty == 0.0D )
			this.maxDifficulty = usageMap.getMaxDifficulty();
		return maxDifficulty;
	}

	// sb.append a line per hint in the subHints Map.
	private void appendSubHints(StringBuilder sb, Map<String, Pair<Integer, Double>> subHints) {
		for ( String hintName : subHints.keySet() ) {
			// 5 leading spaces to indent the "sub-hints".
			sb.append("     ");
			// append right-aligned 1, 2 or 3 digit numHints (AFAIK max is 2)
			final Pair<Integer, Double> tuple = subHints.get(hintName);
			final int numHints = tuple.a;
			final double difficulty = tuple.b;
			if(numHints < 100) sb.append(' ');
			if(numHints < 10) sb.append(' ');
			// append the line
			sb.append(numHints).append(" x ").append(hintName).append(SP)
					.append(dbl(difficulty)).append(NL);
		}
	}

	private void appendUsage(StringBuilder sb, IHinter h, Usage u) {
		final String difficulty;
		if ( u.maxDifficulty > 0.0 )
			difficulty = SP+dbl(u.maxDifficulty);
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
		// update my difficulty fields
		if ( u.maxDifficulty > maxDifficulty )
			maxDifficulty = u.maxDifficulty;
		ttlDifficulty += u.ttlDifficulty;
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
		final StringBuilder sb = new StringBuilder(BIG_BFR_SIZE);
		sb.append(NL).append("<pre>").append(NL);
		appendUsageMap(sb);
		sb.append("</pre>").append(NL);
		return Html.produce(this, "AnalysisHint.html", dbl(getDifficulty()), sb);
	}

	@Override
	public String toStringImpl() {
		return "Sudoku Rating";
	}

}