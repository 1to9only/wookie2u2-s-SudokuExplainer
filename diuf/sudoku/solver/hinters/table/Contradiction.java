/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.solver.hinters.table.ATableChainer.Eff;
import static diuf.sudoku.utils.Frmt.CAUSES;
import static diuf.sudoku.utils.Frmt.PLUS_OR_MINUS;

/**
 * Contradiction DTO is a pre-hint. It holds the essential data awaiting hint
 * creation. Stored in a TreeSet, hence implements Comparable.
 *
 * @author Keith Corlett 2023-11-06
 */
public class Contradiction implements Comparable<Contradiction> {

	public final Eff initOn;
	public final int indice;
	public final int value;

	public Contradiction(final Eff initOn, final int indice, final int value) {
		this.initOn = initOn;
		this.indice = indice;
		this.value = value;
	}

	@Override
	public int compareTo(final Contradiction o) {
		int result;
		if ( (result=initOn.compareTo(o.initOn)) != 0 )
			return result;
		if ( (result=value - o.value) != 0 )
			return result;
		return indice - o.indice;
	}

	@Override
	public String toString() {
		return "Contradiction: "+initOn+CAUSES+CELL_IDS[indice]+PLUS_OR_MINUS+value;
	}

}
