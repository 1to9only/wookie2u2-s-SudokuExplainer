/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;

/**
 * Data transfer object for a Sue De Coc hint.
 *
 * @author Keith Corlett 2020-02-05
 */
public class SueDeCoqHint extends AHint implements IActualHint {

	private final Pots purples;

	public SueDeCoqHint(AHinter hinter, Pots reds, Pots greens, Pots blues
			, Pots purples, ARegion base, ARegion cover) {
		super(hinter, reds, greens, null, blues, Grid.regionList(base)
				, Grid.regionList(cover));
		this.purples = purples;
	}

	@Override
	public Pots getPurples() {
		return purples; // endo-fins
	}

	@Override
	protected String toStringImpl() {
		return "SueDeCoq: "+greens.toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.produce(this, "SueDeCoqHint.html"
				, redPots.toString()
				, greens.toString()
				, blues.toString()
				, purples.toString()
		);
	}
	
}
