/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The SiameseLockingAccumulator is an IAccumulator that helps merge Siamese
 * Locking hints. Hints are considered "Siamese" if there is multiple hints on
 * different values in the same "slot" (the intersection of a row-or-col and a
 * box in the grid). I take the hints from the base Locking class, and callback
 * SiameseLocking at the end of each region, to give him a chance to merge
 * "siamese" hints into a single hint, and/or upgrade them to HiddenSet hints,
 * whichever produces more eliminations or crying bastards (English EXCLUDED).
 * <p>
 * I am only a thin wrapper around an ArrayList of LockingHint. All the tricky
 * stuff is delegated back to the SiameseLocking which injects me into Locking.
 *
 * @author Keith Corlett 2021-07-12.
 */
public final class SiameseLockingAccumulator implements IAccumulator {

	private final SiameseLocking parent;
	private final ArrayList<AHint> list = new ArrayList<>();

	public SiameseLockingAccumulator(SiameseLocking parent) {
		this.parent = parent;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public boolean add(AHint hint) {
		return list.add((LockingHint)hint);
	}

	@Override
	public boolean addAll(Collection<? extends AHint> hints) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public AHint poll() {
		if ( list.isEmpty() )
			return null;
		return list.remove(0);
	}

	@Override
	public AHint peek() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public AHint peekLast() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean any() {
		return size() > 0;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public void sort(final Comparator<AHint> comparator) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		throw new UnsupportedOperationException("Not supported.");
	}

	// ---------------- unique to SiameseLockingAccumulator ----------------

	void startRegion(ARegion r) {
		clear();
	}

	boolean endRegion(ARegion r) {
		switch ( list.size() ) {
		case 0: return false;
		case 1: return parent.add((LockingHint)list.remove(0));
		// two or more hints (size is NEVER negative)
		default: return parent.mergeSiameseHints(r, list);
		}
	}

	@Override
	public List<AHint> getList() {
		return list;
	}

}
