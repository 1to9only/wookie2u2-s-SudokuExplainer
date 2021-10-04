/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.Map;

/**
 * A TurboHint is raised when chaining solves the whole puzzle by
 * assuming one cell-value. This happens often in DynamicChain and
 * less so in DynamicPlus, and less so in DynamicChain, and so on,
 * and the ChainerUnary hasn't even heard of a turbo. City life
 * has made him a bit slow Marge.
 *
 * @author Keith Corlett 2021-07-03
 */
public class TurboHint extends AHint {

	private final Ass ass;
	private final Pots setPots;

	public TurboHint(AHinter hinter, Ass ass, Pots setPots) {
		super(hinter, null);
		this.ass = ass;
		this.setPots = setPots;
	}

	@Override
	public Pots getGreens(int viewNumUnused) {
		return setPots;
	}

	@Override
	public Pots getResults() {
		return setPots;
	}

	@Override
	public int getResultColor() {
		return 0; // green
	}

	@Override
	public int getNumElims() {
		return setPots.size();
	}

	@Override
	protected int getScore() {
		return setPots.size();
	}

	@Override
	protected int applyImpl(boolean isAutosolving, Grid grid) {
		int numElims = 0;
		for ( Map.Entry<Cell,Integer> e : setPots.entrySet() )
			numElims += 10 * e.getKey().set(VFIRST[e.getValue()]);
		return numElims;
	}

	@Override
	protected String toStringImpl() {
		return "Turbo: "+ass;
	}

	@Override
	protected String toHtmlImpl() {
		final String adjective;
		if ( hinter.getTech()==Tech.DynamicPlus || hinter.tech.isNested )
			adjective = " nearly";
		else
			adjective = " bloody";
		return Html.colorIn(
			   "<html><body>\n"
			 + "<h2>Turbo</h2>\n"
			 + "If you set <g><b>"+ass+"</b></g> the puzzle"+adjective+" falls out.\n"
			 + "</body></html>\n"
		);
	}

}
