/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.solver.AHint;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * The (Default)HintsAccumulator is a "vanilla" multiple-hints IAccumulator.
 * <p>
 * Its pretty much how you would expect a collector of AHints to be, except
 * that add ignores null hints, as required by IAccumulator.
 * <p>
 * Note that I collect multiple hints, meaning that I am NOT isSingle().
 * If you want oneHintOnly then use a SingleHintsAccumulator instead.
 */
public class HintsAccumulator implements IAccumulator {

	protected final LinkedList<AHint> list;

	/**
	 * Constructor.
	 * @param list The reference-type LinkedList is required because I use the
	 *  poll method in addition to the List interface
	 */
	public HintsAccumulator(final LinkedList<AHint> list) {
		this.list = list;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean add(final AHint hint) {
		if ( hint != null ) {
			list.add(hint);
		}
		return isSingle();
	}

	@Override
	public AHint poll() {
		return list.poll();
	}

	@Override
	public AHint peek() {
		return list.peek();
	}

	@Override
	public AHint peekLast() {
		return list.peekLast();
	}

	@Override
	public boolean any() {
		return list.size() > 0;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public void sort(final Comparator<AHint> comparator) {
		list.sort(comparator==null ? AHint.BY_SCORE_DESC : comparator);
	}

	@Override
	public boolean addAll(final Collection<? extends AHint> hints) {
		boolean result = false;
		if ( hints==null || hints.isEmpty() ) {
			return result;
		}
		for ( AHint h : hints ) {
			// NOTE use add BECAUSE it may be overridden!
			result |= add(h);
		}
		return result;
	}

	@Override
	public void removeAll(final List<AHint> toRemove) {
		for ( AHint h : toRemove ) {
			list.remove(h);
		}
	}

	/**
	 * Returns my internal list, which is actually a {@code LinkedList<AHint>}.
	 *
	 * @return {@code List<AHint>}
	 */
	@Override
	public LinkedList<AHint> getList() {
		return list;
	}

}
