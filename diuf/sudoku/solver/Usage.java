/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
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

	public String hinterName; // for LogicalSolverTester
	public int calls, hints, elims;
	public long time;
	// hintTypeName => Pair<numHints, difficulty>
	public final Map<String,Pair<Integer,Integer>> subHintsMap;
	// the difficulty for maximum calculations
	public int maxDifficulty = 0;
	// the difficulty for total calculations
	public int ttlDifficulty = 0;

	public Usage(final String hinterName, final int calls, final int hints, final int elims, final long time) {
		this.hinterName = hinterName;
		this.calls = calls;
		this.hints += hints;
		this.elims = elims;
		this.time = time;
		this.subHintsMap = new HashMap<>(4, 1F);
	}

	public void upsertSubHints(final AHint hint, final int numHints) {
		final String name = hint.getHintTypeName();
		final int difficulty = hint.getDifficulty();
		final Pair<Integer,Integer> existing = subHintsMap.get(name);
		if ( existing == null )
			subHintsMap.put(name, new Pair<>(numHints, difficulty));
		else {
			existing.a += numHints;
			if ( difficulty > existing.b ) { // Math.max is slower.
				existing.b = difficulty;
			}
		}
	}

	// I hate division by zero errors. The answer's 0 ya putz! If you divide 11
	// bananas amonst your 0 kids then you are just not ____ing hard enough, so
	// you can ____ right off to bedlam to continue ____ing like a dirty MO-FO!
	// Yeah, yeah, na... it just means that nobody gets any bananas, ie 0, and
	// I fail to see the motivation for differentiating this case from sharing
	// 0 bananas amonst 11 kids, except that the later case has been totally
	// and utterly normalised by you manky inbred whitewings tight old ___wits.
	private int div(final long a, final int b) {
		if ( b == 0 )
			return 0;
		return (int) a / b;
	}

	// for debugging only, never used in anger
	@Override
	public String toString() {
		return ""+time+"/"+elims+EQUALS+(div(time,elims));
	}

	public void add(final Usage u) {
		add(u.calls, u.hints, u.elims, u.time);
	}

	public void add(final int calls, final int hints, final int elims, final long time) {
		this.calls += calls;
		this.hints += hints;
		this.elims += elims;
		this.time += time;
	}

}
