/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.LogicalAnalyser;
import diuf.sudoku.solver.UsageMap;
import diuf.sudoku.solver.Usage;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.enspace;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Pair;
import java.util.Map;
import static diuf.sudoku.utils.Frmt.SP;
import static diuf.sudoku.utils.Frmt.lng;
import static diuf.sudoku.utils.MyStrings.BIG_BFR_SIZE;

/**
 * A warning hint from {@link LogicalAnalyser} contains a difficulty rating of
 * the Sudoku, and the hints used to solve it. The solution is not shown, and
 * the grid is not modified by applying this hint.
 *
 * @see diuf.sudoku.solver.checks.Analyser
 */
public final class AnalysisHint extends AWarningHint {

	public final UsageMap usageMap;

	// The GUI reads these back to display them, and log them.
	public int maxDifficulty;
	public int ttlDifficulty;

	public AnalysisHint(final IHinter hinter, final UsageMap usageMap) {
		super(null, hinter);
		assert usageMap.any();
		this.usageMap = usageMap;
	}

	@Override
	public int getDifficulty() {
		if ( maxDifficulty == 0 )
			this.maxDifficulty = usageMap.getMaxDifficulty();
		return maxDifficulty;
	}

	// sb.append a line per hint in the subHints Map.
	private void appendSubHints(final StringBuilder sb, final Map<String, Pair<Integer, Integer>> subHints) {
		for ( String hintName : subHints.keySet() ) {
			// 5 leading spaces to indent the "sub-hints".
			sb.append("     ");
			// append right-aligned 1, 2 or 3 digit numHints (AFAIK max is 2)
			final Pair<Integer, Integer> tuple = subHints.get(hintName);
			final int numHints = tuple.a;
			final int difficulty = tuple.b;
			if ( numHints < 100 )
				sb.append(' ');
			if ( numHints < 10 )
				sb.append(' ');
			// append the line
			sb.append(numHints).append(" x ").append(hintName).append(SP).append(lng(difficulty)).append(NL);
		}
	}

	private void appendUsage(final StringBuilder sb, final IHinter hinter, final Usage usage) {
		final int numHints = usage.hints;
		if ( numHints > 0 ) {
			final String difficulty;
			if ( usage.maxDifficulty > 0.0 )
				difficulty = SP + lng(usage.maxDifficulty);
			else
				difficulty = EMPTY_STRING;
			sb.append(enspace(numHints, 2)).append(" x ").append(hinter).append(difficulty).append(NL);
			// if this hinter produced more than one type of hint
			// then append a count of each type of hint
			if ( usage.subHintsMap.size() > 1 )
				appendSubHints(sb, usage.subHintsMap);
		}
		// update my difficulty fields
		if ( usage.maxDifficulty > maxDifficulty )
			maxDifficulty = usage.maxDifficulty;
		ttlDifficulty += usage.ttlDifficulty;
	}

	// appendUsageMap takes mySB: a formatted summary of this AnalysisHint.
	public StringBuilder appendUsageMap(final StringBuilder sb) {
		for ( Map.Entry<IHinter, Usage> e : usageMap.entrySet() ) {
			final Usage usage = e.getValue();
			if ( usage != null )
				appendUsage(sb, e.getKey(), usage);
			else { // no usage data can only happen with the catch-all
				sb.append(" 1 x catch-all usage data is unavailable (997)").append(NL)
				  .append(" 1 x enable some bloody hinters ya putz  (998)").append(NL)
				  .append(" 1 x or you have broken a bloody hinter  (999)").append(NL);
				break; // NONE SHALL PASS
			}
		}
		return sb;
	}

	@Override
	public String toHtmlImpl() {
		// nb: MyStrings.bigSB is already used!
		final StringBuilder sb = SB(BIG_BFR_SIZE);
		sb.append(NL).append("<pre>").append(NL);
		appendUsageMap(sb);
		sb.append(NL).append("TIME: ").append(usageMap.totalTime());
		sb.append("</pre>").append(NL);
		return Html.produce(this, "AnalysisHint.html", lng(getDifficulty()), sb);
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(16).append("Sudoku Rating");
	}

}
