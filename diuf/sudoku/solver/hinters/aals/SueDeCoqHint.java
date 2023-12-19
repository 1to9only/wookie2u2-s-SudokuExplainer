/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.aals;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.utils.Frmt.COLON_SP;

/**
 * Data transfer object (DTO) for a SueDeCocHint. Its an American!
 *
 * @author Keith Corlett 2020-02-05
 */
public class SueDeCoqHint extends AHint  {

	private final Pots purples;

	public SueDeCoqHint(Grid grid, IHinter hinter, Pots reds, Pots greens
			, Pots blues, Pots purples, ARegion base, ARegion cover) {
		super(grid, hinter, reds, greens, null, blues
				, Regions.array(base), Regions.array(cover));
		this.purples = purples;
	}

	@Override
	public Pots getPurplePots() {
		return purples; // endo-fins, <b6>
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP).append(greens);
	}

	@Override
	protected String toHtmlImpl() {
		return Html.produce(this, "SueDeCoqHint.html"
			, greens.toString()	 // intersection <g>
			, blues.toString()	 // inter & line <b1>
			, purples.toString() // inter & box  <b6>
			, reds.toString() // eliminations <r>
		);
	}

}
