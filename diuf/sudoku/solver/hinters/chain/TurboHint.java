/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Constants;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VFIRST;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Stick a Hair Dryer in it!
 * <p>
 * A TurboHint is raised when chaining solves the whole puzzle
 * by simply assuming one cell-value.
 *
 * @author Keith Corlett 2021-07-03
 */
public class TurboHint extends AChainingHint {

	private final Ass hole;
	private final Pots setPots;

	public TurboHint(final Grid grid, final IHinter hinter, final Ass hole
			, final Pots setPots) {
		super(grid, hinter, setPots, 1, true, true, null);
		this.hole = hole;
		this.setPots = setPots;
	}

	@Override
	public Pots getGreenPots(int viewNumUnused) {
		return setPots;
	}

	@Override
	public Pots getSetPots() {
		return setPots;
	}

	@Override
	public int getSetPotsColor() {
		return Constants.GREEN; // 0 for Ons
	}

	@Override
	public Set<Integer> getFlatAquaIndices() {
		return setPots.keySet();
	}

	@Override
	public int getNumElims() {
		return setPots.size();
	}

	@Override
	protected int getScore() {
		return setPots.size() * 10;
	}

	@Override
	protected int getFlatComplexity() {
		return setPots.size();
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return null;
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return null;
	}

	@Override
	protected int applyImpl(boolean isAutosolving, Grid grid) {
		int numElims = 0;
		final Cell[] cells = grid.cells;
		for ( Map.Entry<Integer,Integer> e : setPots.entrySet() ) {
			numElims += 10 * cells[e.getKey()].set(VFIRST[e.getValue()]);
		}
		return numElims;
	}

	@Override
	public String getNameMiddle() {
		return "Turbo";
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(32).append(getHintTypeName()).append(COLON_SP).append(hole);
	}

	@Override
	protected String toHtmlImpl() {
		return Html.colorIn(
			   "<html><body>\n"
			 + "<h2>Turbo</h2>\n"
			 + "If you set <g><b>"+hole+"</b></g> the bastard just falls out.\n"
			 + "</body></html>\n"
		);
	}

}
