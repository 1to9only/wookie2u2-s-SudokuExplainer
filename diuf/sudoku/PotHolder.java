/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;

/**
 * A PotHolder "holds" a Pots. It exists to avoid needlessly creating a Pots (a
 * LinkedHashSet, whose constructor is a bit heavy) that is never populated, so
 * you create a PotsHolder instead. Then I create a Pots instance on demand the
 * first time you upsertAll, then you call copyAndClear to copy-off and clear
 * my Pots; hence a Pots is NOT created until there's something to add to it.
 * Simples!
 * <p>
 * I also expose the "any" field, to save you from having to call Pots.isEmpty.
 * Method calls are slow. Sigh.
 * <p>
 * That and I just like the pun.
 *
 * @author Keith Corlett 2021-12-17
 */
public class PotHolder {

	private Pots pots;
	public boolean any;

	public boolean upsertAll(final Idx victims, final Grid grid, final int v) {
		any = true;
		if ( pots == null )
			pots = new Pots();
		return pots.upsertAll(victims, grid, v);
	}

	public void put(final Cell c, final int cands) {
		any = true;
		if ( pots == null )
			pots = new Pots();
		pots.put(c, cands);
	}

	// if this gets called BEFORE upsertAll it'll throw an NPE. Let it!
	public Pots copyAndClear() {
		any = false;
		return pots.copyAndClear();
	}

}
