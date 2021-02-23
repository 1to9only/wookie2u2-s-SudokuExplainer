/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IHinter;
import java.util.ArrayList;
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
		super(AHinter.BY_DIFFICULTY_ASC_TOSTRING_ASC);
	}

	/**
	 * Add the values if hinter already exists, else insert it.
	 * <p>Used by LogicalSolver.timeHinters, and by the local addonate(...).
	 */
	void addon(IHinter hinter, int numCalls, int numHints, int numElims, long time) {
		Usage existing = super.get(hinter);
		if (existing == null)
			put(hinter, new Usage(numCalls, numHints, numElims, time));
		else {
			existing.numCalls += numCalls;
			existing.numHints += numHints;
			existing.numElims += numElims;
			existing.time += time;
		}
	}

	/**
	 * Adds the given toAddOn UsageMap to this UsageMap. Used to produce the
	 * run-total-usages from each puzzles-usages.
	 * <p>Used only by LogicalSolverTester.
	 * @param toAddOn UsageMap to add on
	 */
	public void addonate(UsageMap toAddOn) {
		Usage u;
		for ( IHinter hinter : toAddOn.keySet() ) {
			u = toAddOn.get(hinter);
			addon(hinter, u.numCalls, u.numHints, u.numElims, u.time);
		}
	}

	/**
	 * Listify totals to sort and report them.
	 * <p>Used only by LogicalSolverTester.
	 * @return {@code ArrayList<Usage>}
	 */
	public ArrayList<Usage> toArrayList() {
		ArrayList<Usage> result = new ArrayList<>(size());
		for ( IHinter hinter : keySet() ) {
			final Usage u = get(hinter);
			u.hinterName = hinter.toString();
			result.add(u);
		}
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
