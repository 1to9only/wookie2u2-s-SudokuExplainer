/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyStrings;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * An aggregation of all hints found in a pass through a hinter (Chains and
 * Franken HdkFisherman, to speed-up autosolving puzzles in
 * {@link diuf.sudoku.test.LogicalSolverTester} because these hinter take some
 * time, and typically generate multiple hints when they hint at all.
 * <p>
 * Design note: There's nothing "Chaining" in this class. To make this class
 * into an abstract AggregatedHint all that needs to change is make toString
 * return a String constant passed into my constructor.
 *
 * @author Keith Corlett 2017 Dec
 */
public final class AggregatedHint extends AHint implements IActualHint {

	public final Collection<AHint> hints;
	public final String hintTypeName;

	private double maxDifficulty;

	/** Constructs a new aggregate of the given AChainingHint's.
	 *
	 * @param hinter The Chains which produced this hint
	 * @param hints The {@code Collection<AChainingHint>} to be aggregated.
	 * @param hintTypeName The name of the hinter which created me; used as the
	 * name of hint-types in my toString method. */
	public AggregatedHint(AHinter hinter, Collection<AHint> hints, String hintTypeName) {
		// 128 is next power of 2 from 81
		super(hinter, AHint.AGGREGATE, null, 0, new Pots(128, 1F), null, null, null, null, null);
		this.hints = hints;
		this.hintTypeName = hintTypeName;
	}

	/** apply all the hints in this collection and
	 * @param isAutosolving
	 * @return the total number of eliminations. */
	@Override
	public int applyImpl(boolean isAutosolving) {
		int sumElims = 0;
		for ( AHint hint : hints ) {
			if ( Log.MODE >= Log.VERBOSE_4_MODE )
				Log.format("%32s%s%s", "", MyStrings.squeeze(hint.toFullString()), NL);
			sumElims += hint.apply(isAutosolving, applyIsNoisy);
		}
		return sumElims;
	}
	public boolean applyIsNoisy;

	/**
	 * @return the green Pots: cell=>value to be set by these hints
	 * (no oranges).
	 */
	@Override
	public Pots getGreens(int viewNum) {
		if(greenPots!=null) return greenPots;
		greenPots = new Pots(128, 1F); // 128 is next power of 2 from 81
		for ( AHint hint : hints )
			if ( (hint.cell) != null )
				greenPots.put(hint.cell, new Values(hint.value));
		return greenPots;
	}
	private Pots greenPots;

	/** @return the red Pots that'll be removed by these hints (no oranges). */
	@Override
	public Pots getReds(int viewNum) {
		if(!redPots.isEmpty()) return redPots;
		for ( AHint hint : hints )
			redPots.upsertAll(hint.redPots);
		// Deorangify coz there are commonly too many of them to be useful.
		redPots.removeAll(getGreens(viewNum));
		return redPots;
	}

	/** @return the total score of all these hints - irrelevant anyway because
	 * (by definition) there's 1 hint, so sort order is not an issue. */
	@Override
	public int getNumElims() {
		int score = 0;
		for ( AHint hint : hints ) {
			// include both for mixed-hints which eliminate to set a cell.
			if ( hint.cell != null )
				score += 10;
			score += hint.redPots.totalSize();
		}
		return score;
	}

	/**
	 * @param notUsed
	 * @return Set of cells to be set by these hints.
	 */
	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		Set<Cell> cells = null;
		for ( AHint hint : hints )
			if ( hint.cell != null ) {
				if(cells==null) cells=new HashSet<>();
				cells.add(hint.cell);
			}
		return cells;
	}

	/** @return the cheats maximum difficulty, which gives up as soon as it
	 * finds a diabolical hint, coz that'll be the puzzles rating. */
	@Override
	public double getDifficulty() {
		if ( maxDifficulty != 0.0D )
			return maxDifficulty;
		double d;
		for ( AHint hint : hints )
			if ( (d=hint.getDifficulty()) > maxDifficulty )
				maxDifficulty = d;
		return maxDifficulty;
	}

	/** @return String HTML explanation of this hint. */
	@Override
	public String toHtmlImpl() {
		StringBuilder sb = new StringBuilder(hints.size()*128);
		sb.append("<pre>");
		for ( AHint hint : hints )
			sb.append(hint).append(NL);
		sb.append("</pre>");
		return sb.toString();
	}

	@Override
	public String getHintTypeNameImpl() {
		return hintTypeName;
	}

	private String distinctCellsToSet() {
		StringBuilder sb = new StringBuilder(24);
		boolean[][] got = new boolean[9][9];
		Cell c;
		int i = 0;
		for ( AHint hint : hints ) {
			if( (c=hint.cell)==null || got[c.y][c.x] ) continue;
			got[c.y][c.x] = true;
			if ( ++i > 1 )
				sb.append(", ");
			sb.append(c.id).append("+").append(hint.value);
		}
		return sb.toString();
	}

	private Pots allRedPots() {
		Pots allRedPots = new Pots(hints.size()*2, 1F);
		for ( AHint hint : hints )
			allRedPots.upsertAll(hint.redPots);
		return allRedPots;
	}

	/** @return distinctCellsToSet + allRedPots. */
	@Override
	public String toFullString() {
		final String c = distinctCellsToSet();
		final String r = allRedPots().toString();
		final String sep; if(!c.isEmpty()&&!r.isEmpty()) sep=", "; else sep="";
		return toStringImpl()+" ("+c+sep+r+")";
	}

	/** @return "Aggregate of "+hints.size()+" "+hintTypeName+" hints". */
	@Override
	public String toStringImpl() {
		return "Aggregate of "+hints.size()+" "+hintTypeName+" hints";
	}
}