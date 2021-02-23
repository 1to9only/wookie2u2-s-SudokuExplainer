/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.utils.Pair;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


/**
 * Usage is internal to LogicalSolver UsageMap. It stores:<ul>
 *	<li>numCalls: the number of times a Hinter has been called
	<li>timeNS: how long it has taken to execute
	<li>numElims: how many maybes it has eliminated
	<li>subHintsMap: hintTypeName =&gt; count: for Hinters which produce multiple
 *   hint-types.
 * </ul>
 * @author Keith Corlett 2017 Dec
 */
public final class Usage {

	public static final Comparator<Usage> BY_NUM_CALLS_DESC
			= new Comparator<Usage>() {
		@Override
		public int compare(Usage a, Usage b) {
			return b.numCalls - a.numCalls; // DESCENDING
		}
	};

	public static final Comparator<Usage> BY_NS_PER_ELIM_ASC
			= new Comparator<Usage>() {
		@Override
		public int compare(Usage a, Usage b) {
			long aa = a.numElims==0 ? Long.MAX_VALUE : a.time / a.numElims;
			long bb = b.numElims==0 ? Long.MAX_VALUE : b.time / b.numElims;
			if ( aa < bb )
				return -1; // ASCENDING
			if ( aa > bb )
				return 1; // ASCENDING
			return 0;
		}
	};

	// hintTypeName => Pair<numHints, difficulty>
	public final Map<String,Pair<Integer,Double>> subHintsMap;
	public int numCalls, numHints, numElims;
	public long time;
	public double maxDifficulty = 0D;

	public String hinterName;

	public Usage(int numCalls, int numHints, int numElims, long time) {
		this.subHintsMap = new HashMap<>(4, 1F);
		this.numCalls = numCalls;
		this.numHints += numHints;
		this.numElims = numElims;
		this.time = time;
	}

	public void addonateSubHints(AHint hint, int numHints) {
		String hintTypeName = hint.getHintTypeName();
		double difficulty = hint.getDifficulty();
		Pair<Integer,Double> existing = subHintsMap.get(hintTypeName);
		if ( existing == null )
			subHintsMap.put(hintTypeName, new Pair<>(numHints, difficulty));
		else {
			existing.a += numHints;
			existing.b = Math.max(existing.b, difficulty);
		}
	}

	// I hate division by zero errors. The answer's 0 ya putz! If you divide 11
	// bananas amonst your 0 kids then you're just not ____ing hard enough, so
	// you can ____ right off to bedlam, crash, and burn like a dirty MO-FO!
	// Yeah, yeah, na... it just means that nobody gets any bananas, ie 0, and
	// I fail to see the motivation for differentiating this case from sharing
	// 0 bananas amonst 11 kids, except that the later case has been utterly
	// normalised by tight old ___holes like Donald.
	private int div(long a, int b) {
		return b==0 ? 0 : (int)a/b;
	}

	@Override
	public String toString() {
		return ""+time+"/"+numElims+"="+(div(time,numElims));
	}

}
