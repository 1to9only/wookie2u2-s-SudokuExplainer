/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Wrap a single AChainingHint to change the behaviour of the
 * <tt>equals()</tt> and <tt>hashCode</tt> methods so that they
 * consider the entire chain(s) instead of just the target Ass.
 */
public final class FullChain {

	private final AChainingHint target;

	private int hc;

	public FullChain(AChainingHint target) {
		this.target = target;
	}

	public AChainingHint get() {
		return this.target;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == this ) {
			return true; // probable (they are small lists)
		}
		if ( !(o instanceof FullChain) ) {
			return false; // never happens. never say never.
		}
		final FullChain other = (FullChain)o;
		final AChainingHint oTarget = other.target;
		// NB: use java.util.ArrayList<Ass> for its correct equals impl.
		// Arrays.asList ArrayList does NOT implement equals properly. Sigh.
		final ArrayList<Ass> myList = target.getChainsTargets();
		final ArrayList<Ass> hisList = oTarget.getChainsTargets();
		if ( !myList.equals(hisList) ) { // compares lengths!
			return false;
		}
		// deep equals: is every ancestor in both lists equals
		final Iterator<Ass> myIt = myList.iterator();
		final Iterator<Ass> hisIt = hisList.iterator();
		while ( myIt.hasNext() && hisIt.hasNext() ) {
			Ass mine = myIt.next();
			Ass his = hisIt.next();
			if ( mine != his
			  || !target.ancestors(mine).equals(oTarget.ancestors(his)) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		if ( hc != 0 )
			return hc; // cached
		int result = 0;
		for ( Ass t : target.getChainsTargets() )
			for ( Ass a : target.ancestors(t) )
				result ^= a.hashCode;
		return hc = result;
	}
}
