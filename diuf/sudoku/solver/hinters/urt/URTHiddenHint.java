/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;

/**
 * The DTO for a Hidden Rectangle only (no loops) Hint; ie loop.size==4.
 *
 * @author Keith Corlett 2021-02-25
 */
public final class URTHiddenHint extends AURTHint {

	private final ARegion base; // the B -> D edge of the rectangle
	private final ARegion cover; // the C -> D edge of the rectangle
	public URTHiddenHint(final AHinter hinter, final Cell[] loop, final int n
			, final int v1, final int v2, final Pots reds, final ARegion base
			, final ARegion cover) {
		// Type 7 is a Hidden Unique Rectangle (aka Unique Rectangle Hidden)
		super(7, hinter, loop, n, v1, v2, reds);
		this.base = base;
		this.cover = cover;
	}

	@Override
	public ARegion[] getBases() {
		return Regions.array(base);
	}

	@Override
	public ARegion[] getCovers() {
		return Regions.array(cover);
	}

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			final Pots pots = new Pots(4, 1F);
			final int sv = VSHFT[v1]; // v1 (a) is locked into rectangle
			for ( int i=0; i<loopSize; ++i )
				pots.put(loop[i], sv);
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public Pots getBlues(Grid grid, int viewNum) {
		if ( bluePots == null ) {
			final Pots pots = new Pots(4, 1F);
			final int sv = VSHFT[v2]; // v2 (b) is value to remove
			for ( int i=0; i<loopSize; ++i )
				pots.put(loop[i], sv);
			// remove the removable (red) pot so that it appears red
			pots.removeAll(getReds(0));
			bluePots = pots;
		}
		return bluePots;
	}
	private Pots bluePots;

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URTHiddenHint.html"
			, getTypeName()				//{0}
			, v1						// 1
			, v2						// 2
			, Frmu.csv(loopSize, loop)	// 3
			, reds.toString()			// 4
			, loop[0].id				// 5 A
			, loop[1].id				// 6 B
			, loop[2].id				// 7 D (out of order!)
			, loop[3].id				// 8 C (out of order!)
		);
	}
}