/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * A UsageMap stores the usage statistics (see below) of the used hinters in a
 * TreeMap that is sorted BY_EXECUTION_ORDER. A UsageMap is just a Map from
 * IHinter to Usage, plus a few helper methods.
 * {@link diuf.sudoku.solver.checks.AnalysisHint} uses UsageMap as a summary of
 * the hints that solve a puzzle.
 * <p>
 * UsageMap is a table of hinters: the number of times each hinter was called,
 * number of hints produced, total number of eliminations, and elapsed time.
 * <p>
 * Setting a cell-value counts as 10 eliminations in the total-eliminations.
 *
 * @author Keith Corlett
 */
public final class UsageMap extends TreeMap<IHinter, Usage> {

	private static final long serialVersionUID = 8945158L;

	/**
	 * Constructs a new empty UsageMap
	 */
	public UsageMap() {
		super(IHinter.BY_EXECUTION_ORDER);
	}

	/**
	 * if $h already exists then add $s to it,
	 * else insert new Usage for $h with values $s.
	 */
	void upsert(final IHinter h, final Usage s) {
		final Usage u = super.get(h);
		if ( u == null )
			put(h, new Usage(h.toString(), s.calls, s.hints, s.elims, s.time));
		else
			u.add(s.calls, s.hints, s.elims, s.time);
	}

	/**
	 * Adds $src UsageMap to this UsageMap.
	 *
	 * @param src UsageMap to add on
	 */
	public void addonAll(final UsageMap src) {
		for ( Map.Entry<IHinter,Usage> e : src.entrySet() )
			upsert(e.getKey(), e.getValue());
	}

	/**
	 * Construct an ArrayList of usages to sort and report them.
	 * <p>
	 * Used only by LogicalSolverTester. so it is OK that I also
	 * set the hinterIndex of each Usage as it passes thrue.
	 *
	 * @return {@code ArrayList<Usage>}
	 */
	public ArrayList<Usage> toArrayList() {
		final ArrayList<Usage> result = new ArrayList<>(size());
		for ( Map.Entry<IHinter,Usage> e : entrySet() ) // pre-sorted
			result.add(e.getValue());
		return result;
	}

	/**
	 * Get the maximum difficulty of all the Usage in this map.
	 * <p>Used only by AnalysisHint.getDifficulty()
	 * @return the maximum difficulty of these Usages.
	 */
	public int getMaxDifficulty() {
		int max = 0;
		for ( Usage u : values() )
			max = Math.max(max, u.maxDifficulty);
		return max;
	}

	public String totalTime() {
		long t = 0L;
		for ( Usage u : values() )
			t += u.time;
		return Frmt.lng(t);
	}

	public boolean any() {
		return size() > 0;
	}

	public void addAll(final IHinter[] hinters) {
		for ( IHinter h : hinters )
			put(h, new Usage(h.toString(), 0, 0, 0, 0));
	}

	public void removeAll(final Predicate<Usage> filter) {
		for ( Iterator<Map.Entry<IHinter,Usage>> it = entrySet().iterator(); it.hasNext(); )
			if ( filter.test(it.next().getValue()) )
				it.remove();
	}

}
