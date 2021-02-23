/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Wrap a single chaining hint to change the behaviour of the
 * <tt>equals()</tt> and <tt>hashCode</tt> methods so that they
 * consider the entire chain(s) instead of just the outcome.
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
		if ( o == this )
			return true;
		if ( !(o instanceof FullChain) ) 
			return false;
		FullChain other = (FullChain)o;
		// NB: use ArrayList<Ass> so that equals is implemented correctly.
		ArrayList<Ass> thisTargets = target.getChainsTargets();
		ArrayList<Ass> otherTargets = other.target.getChainsTargets();
		if ( !thisTargets.equals(otherTargets) )
			return false;
		Iterator<Ass> it1 = thisTargets.iterator();
		Iterator<Ass> it2 = otherTargets.iterator();
		while ( it1.hasNext() && it2.hasNext() ) {
			Ass a1 = it1.next();
			Ass a2 = it2.next();
			if ( a1!=a2 && !target.getAncestorsList(a1).equals(
					  other.target.getAncestorsList(a2)) )
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		if ( hc != 0 )
			return hc;
		int result = 0;
		for ( Ass t : target.getChainsTargets() )
			for ( Ass a : target.getAncestorsList(t) )
				result ^= a.hashCode;
		return hc = result;
	}
}