/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.utils.Frmt.COLON_SP;

/**
 * Data transfer object for a Sue De Coc hint.
 *
 * @author Keith Corlett 2020-02-05
 */
public class SueDeCoqHint extends AHint  {

	private final Pots purples;

	public SueDeCoqHint(AHinter hinter, Pots reds, Pots greens, Pots blues
			, Pots purples, ARegion base, ARegion cover) {
		super(hinter, reds, greens, null, blues
				, Regions.list(base), Regions.list(cover));
		this.purples = purples;
	}

	@Override
	public Pots getPurples() {
		return purples; // endo-fins, <b6>
	}

	@Override
	protected String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName())
		  .append(COLON_SP).append(greens)
		  .toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.produce(this, "SueDeCoqHint.html"
			, greens.toString()	 // intersection <g>
			, blues.toString()	 // inter & line <b1>
			, purples.toString() // inter & box  <b6>
			, redPots.toString() // eliminations <r>
		);
	}
	
}
