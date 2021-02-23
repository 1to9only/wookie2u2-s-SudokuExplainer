/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

/**
 * OverallSizes is a little helper class that prints the max-size of all the
 * MyAssSet2's, to work-out how big they should have been in the first place.
 * I just hate wasting time repeatedly copying into a new table O(n) just
 * because the programmer (me) is/was too lazy to workout how big it needed
 * to be in the first place. It's just lazy-assed, as apposed to lazy. HashMap
 * too-small causes a performance impost on each and every access. It's a small
 * is fast, big is fast, medium is nothing but trouble recipe.
 * @author Keith Corlett 2017 IIRC
 */
public final class OverallSizes extends java.util.LinkedHashMap<String, Integer> {
	private static final long serialVersionUID = 345675231L;

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	public static final OverallSizes THE = new OverallSizes();

	void max(String id, int size) {
		Integer existing = THE.get(id);
		if ( existing==null || size>existing )
			THE.put(id, size);
	}

	public void dump() {
		if ( size() == 0 )
			return;
		System.out.format(NL);
		for ( String id : keySet() ) {
			System.out.format("\t%,6d\t(%s)", get(id), id);
			System.out.format(NL);
		}
		System.out.format(NL);
		System.out.flush();
		clear();
	}

	private OverallSizes() {}
}