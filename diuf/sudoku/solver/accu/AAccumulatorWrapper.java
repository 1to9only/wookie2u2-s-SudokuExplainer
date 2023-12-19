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
import java.util.List;


/**
 * AAccumulatorWrapper wrap an IAccumulator; extend me to override just the
 * methods you need to. Each method in this implementation simply delegates to
 * the IAccumulator that was passed to my constructor.
 * <p>
 * WARN: accu.addAll MUST call add, not go around it.
 *
 * @author Keith Corlett 2023-05-25
 */
public abstract class AAccumulatorWrapper implements IAccumulator {

	protected final IAccumulator accu;
	public AAccumulatorWrapper(IAccumulator accu) {
		this.accu = accu;
	}

	@Override
	public boolean add(AHint hint) {
		return accu.add(hint);
	}

	@Override
	public boolean isSingle() {
		return accu.isSingle();
	}

	@Override
	public boolean addAll(Collection<? extends AHint> hints) {
		return accu.addAll(hints);
	}

	@Override
	public AHint poll() {
		return accu.poll();
	}

	@Override
	public AHint peek() {
		return accu.peek();
	}

	@Override
	public AHint peekLast() {
		return accu.peekLast();
	}

	@Override
	public boolean any() {
		return accu.any();
	}

	@Override
	public int size() {
		return accu.size();
	}

	@Override
	public void clear() {
		accu.clear();
	}

	@Override
	public void sort(Comparator<AHint> comparator) {
		accu.sort(comparator);
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		accu.removeAll(toRemove);
	}

	@Override
	public List<AHint> getList() {
		return accu.getList();
	}

}
