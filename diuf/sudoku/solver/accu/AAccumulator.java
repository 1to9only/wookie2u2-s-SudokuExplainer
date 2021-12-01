/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.solver.AHint;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * AAccumulator is an abstract default implementation of IAccumulator that you
 * extend to implement only required methods, typically add; else an anonymous
 * implementation of IAccumulator is a verbose boiler-plate pain in the ass.
 *
 * @author Keith Corlett 2021-04-06
 */
public abstract class AAccumulator implements IAccumulator {

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public boolean add(AHint hint) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends AHint> hints) {
		boolean any = false;
		for ( AHint h : hints )
			any |= add(h);
		return any;
	}

	@Override
	public AHint getHint() {
		return null;
	}

	@Override
	public AHint peek() {
		return null;
	}

	@Override
	public AHint peekLast() {
		return null;
	}

	@Override
	public boolean any() {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void reset() {
	}

	@Override
	public void sort(Comparator<AHint> comparator) {
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
	}

}
