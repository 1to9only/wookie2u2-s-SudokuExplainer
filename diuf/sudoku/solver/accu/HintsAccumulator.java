/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.solver.AHint;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The (Default)HintsAccumulator is a "vanilla" multiple-hints IAccumulator.
 * It's pretty much how you'd expect a collector of AHints to be, except the
 * add method checks that this Accumulator doesn't already contain a hint which
 * equals the new one, using hints equals method.
 * <p>
 * Note that I collect multiple hints, meaning that I'm NOT isSingle().
 */
public final class HintsAccumulator implements IAccumulator {

	private final LinkedList<AHint> list;

	public HintsAccumulator(LinkedList<AHint> list) {
		this.list = list;
	}

	/**
	 * Returns my internal list, which is actually a {@code LinkedList<AHint>}.
	 * <p>
	 * Note that this method (unlike all others) is NOT specified by the
	 * IAccumulator interface.
	 * @return {@code List<AHint>}
	 */
	public List<AHint> getHints() {
		return list;
	}

	@Override
	public boolean isSingle() { return false; }

	@Override
	public void reset() {
		list.clear();
	}

	@Override
	public boolean add(AHint hint) {
		if ( hint != null )
			list.add(hint);
		return false;
	}

	@Override
	public boolean addAll(Collection<AHint> hints) {
		if ( hints==null || hints.size()==0 )
			return false;
		list.addAll(hints);
		return true;
	}

	@Override
	public AHint getHint() {
		return list.poll(); // returns null if list is empty
	}

	@Override
	public AHint peek() {
		try {
			return list.get(0);
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public AHint peekLast() {
		try {
			return list.get(list.size()-1);
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public boolean hasAny() {
		return list.size() > 0;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public void sort() {
		// sort hints by score (number of eliminations) descending
		list.sort(AHint.BY_SCORE_DESC);
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		for ( AHint h : toRemove )
			list.remove(h);
	}

}
