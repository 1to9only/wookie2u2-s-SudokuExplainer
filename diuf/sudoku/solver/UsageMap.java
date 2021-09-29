/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.solver.hinters.IHinter;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A UsageMap stores the usage statistics of a collection of hinters. It's used
 * by AnalysisHint as a summary of the hints required to solve a puzzle. I'm
 * just a TreeMap from IHinter to Usage, plus a few helper methods.
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
	 * Add the given values if hinter exists, else add a new Usage for hinter.
	 */
	Usage addon(IHinter hinter, int calls, int hints, int elims, long time) {
		Usage u = super.get(hinter);
		if ( u == null ) {
			put(hinter, u = new Usage(calls, hints, elims, time));
		} else {
			u.calls += calls;
			u.hints += hints;
			u.elims += elims;
			u.time += time;
		}
		return u;
	}

	/**
	 * Adds the given toAddOn UsageMap to this UsageMap.
	 *
	 * @param toAddOn UsageMap to add on
	 */
	public void addonAll(UsageMap toAddOn) {
		Usage a; // each Usage to add
		for ( Entry<IHinter,Usage> e : toAddOn.entrySet() )
			addon(e.getKey(), (a=e.getValue()).calls, a.hints, a.elims, a.time);
	}

	/**
	 * Construct an ArrayList of usages to sort and report them.
	 * <p>
	 * Used only by LogicalSolverTester. so it's OK that I also
	 * set the hinterIndex of each Usage as it passes thrue.
	 *
	 * @return {@code ArrayList<Usage>}
	 */
	public ArrayList<Usage> toArrayList() {
		ArrayList<Usage> result = new ArrayList<>(size());
		for ( Entry<IHinter,Usage> e : entrySet() ) { // sorted
			final Usage u = e.getValue();
			// for LogicalSolverTester
			u.hinterIndex = e.getKey().getIndex();
			result.add(u);
		}
//entrySet is sorted, according to TreeMap documentation
//		// order by the index in the wantedHinters array
//		result.sort(Usage.BY_EXECUTION_ORDER);
		return result;
	}

//	public Usage getByTechName(String name) {
//		for ( IHinter hinter : keySet() )
//			if ( hinter.getTech().name().equals(name) )
//				return get(hinter);
//		return null;
//	}

	/**
	 * Get the maximum difficulty of all the Usage in this map.
	 * <p>Used only by AnalysisHint.getDifficulty()
	 * @return the maximum difficulty of these Usages.
	 */
	public double getMaxDifficulty() {
		double max = 0.0D;
		for (Usage u : values())
			max = Math.max(max, u.maxDifficulty);
		return max;
	}
}
