/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import static diuf.sudoku.utils.Frmt.EQUALS;
import diuf.sudoku.utils.Pair;
import java.util.HashMap;
import java.util.Map;

/**
 * Usage is internal to LogicalSolver UsageMap. It stores:<ul>
 *	<li>calls: the number of times a Hinter has been called
 *	<li>hints: how many hints it produced
 *	<li>elims: how many maybes it has eliminated
 *	<li>time: how long it has taken to execute
 *	<li>maxDifficulty: the maximum hint difficulty
 *  <li>ttlDifficulty: the total of all hint difficulties. <br>
 *   ttlDifficulty differs for GEM because it sets lots of cells in one hint it
 *   adds numSetPots*NakedSingle.difficulty to bring it back into line with the
 *   equivalent NakedSingles, that would be applied if GEM were unwanted. Note
 *   that any future hint-type that sets multiple cells will need to override
 *   getDifficultyTotal to take number of cells set into account.
 *	<li>subHintsMap: hintTypeName =&gt; count: for Hinters which produce
 *   multiple hint-types.
 * </ul>
 * @author Keith Corlett 2017 Dec
 */
public final class Usage {

	// hintTypeName => Pair<numHints, difficulty>
	public final Map<String,Pair<Integer,Double>> subHintsMap;
	public int calls, hints, elims;
	public long time;
	// the difficulty for maximum calculations
	public double maxDifficulty = 0D;
	// the difficulty for total calculations
	public double ttlDifficulty = 0D;

	// the index of my hinter in the wantedHinters array
	// Still used by LogicalSolverTester, even though it's Comparator is gone.
	public int hinterIndex;

	public Usage(int calls, int hints, int elims, long time) {
		this.calls = calls;
		this.hints += hints;
		this.elims = elims;
		this.time = time;
		this.subHintsMap = new HashMap<>(4, 1F);
	}

	public void addonateSubHints(AHint hint, int numHints) {
		String hintTypeName = hint.getHintTypeName();
		double difficulty = hint.getDifficulty();
		Pair<Integer,Double> existing = subHintsMap.get(hintTypeName);
		if ( existing == null )
			subHintsMap.put(hintTypeName, new Pair<>(numHints, difficulty));
		else {
			existing.a += numHints;
			// Math.max is slower.
			if ( difficulty > existing.b )
				existing.b = difficulty;
		}
	}

	// I hate division by zero errors. The answer's 0 ya putz! If you divide 11
	// bananas amonst your 0 kids then you're just not ____ing hard enough, so
	// you can ____ right off to bedlam to continue ____ing like a dirty MO-FO!
	// Yeah, yeah, na... it just means that nobody gets any bananas, ie 0, and
	// I fail to see the motivation for differentiating this case from sharing
	// 0 bananas amonst 11 kids, except that the later case has been totally
	// and utterly normalised by you manky inbred whitewings tight old ___wits.
	private int div(long a, int b) {
		if ( b == 0 )
			return 0;
		return (int) a / b;
	}

	// for debugging only, never used in anger
	@Override
	public String toString() {
		return ""+time+"/"+elims+EQUALS+(div(time,elims));
	}

	public void add(Usage u) {
		calls += u.calls;
		hints += u.hints;
		elims += u.elims;
		time += u.time;
	}

}
