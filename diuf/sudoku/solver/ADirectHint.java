/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.EQUALS;
import static diuf.sudoku.utils.Frmt.IN;
import java.util.Set;

/**
 * ADirectHint is an abstract class which extends AHint (the abstract hint) to
 * represent a hint that directly sets the value of a Cell in the Sudoku Grid,
 * ie: NakedSingle and HiddenSingle.
 * <p>
 * Subsequently I found that Direct Naked/Hidden Pair/Triple/Quad are "direct"
 * by the above definition, even though they produce Indirect Hints, so I have
 * remodelled everything to mostly remove the mostly pointless delineation
 * between direct and indirect hints. This ADirectHint class now just provides
 * default implementations of methods like getRegion, getAquaCells, etc.
 * <p>
 * The LogicalSolver now deals with ALL hints as AHints (which I extend) and
 * ALL hinters as IHinter, which all hinters implement. I judge that there is
 * now zero gain from abstracting AHint out into an IHint (etc) interface.
 * Everything that is a hint extends AHint for it is default implementation,
 * so AHint is also the reference type. If that ever changes then create an
 * IHint interface and change all references from AHint to IHint. Sigh.
 */
public abstract class ADirectHint extends AHint  {

	private final ARegion region; // The concerned region, if any

	// caches
	private int numElims;

	/**
	 * Create a new hint.
	 * @param grid the grid to which this hint applies
	 * @param hinter the Hinter which discovered this hint
	 * @param region the region for which the hint is applicable, else null
	 * @param cell the cell in which a value can be placed
	 * @param value the value that can be placed in the cell
	 */
	public ADirectHint(Grid grid, IHinter hinter, ARegion region, Cell cell, int value) {
		super(grid, hinter, cell, value);
		assert cell!=null && value!=0;
		this.region = region;
	}

	/** @return the Region which contains the Cell of this (Hidden Single)
	 * hint. */
	protected ARegion getRegion() {
		return region;
	}

	/** @return An array of the Regions of this hint. */
	@Override
	public ARegion[] getBases() {
		if ( region == null )
			return null;
		return Regions.array(region);
	}

	/**
	 * To apply a "direct" hint to the grid we just set the Cell to the value.
	 * <p>
	 * NB: redPots is always null for a "direct" hint (never empty).
	 * <p>
	 * I do not know how to score! (Sigh) I think that returning a score of 10
	 * for each cell set is enough to make the setting of a cell preferable to
	 * any/all removing of maybes; ie I think 9 is the most number of maybes
	 * that CAN be removed by setting a Cell IN A VALID SUDOKU (if not when
	 * building a Sudoku). What we want is for the most effective hint found to
	 * be applied. So I keep mulling-over adding the number of maybes actually
	 * eliminated to a reduced set-cell-constant. But 10 seems to work for now,
	 * just watch this space... thats all.
	 * <p>
	 * I guess a mathematician could give a reasonable answer to this question.
	 * I have mere instinct. Fresh meat is important, not the size/precision of
	 * your spear.
	 *
	 * @param isAutosolving true from LogicalSolver.solve makes me use Cells
	 * set(isAutosolving=true) instead of just plain old set.
	 * @param grid to apply this hint to
	 * @return the "score" which is 10 for each cell set. When isAutosolving
	 * is true the setting of one cell may cause the setting of another, which
	 * sets another. So setting one cell may cascade to fill the whole grid.
	 * So my return value is the "score", ie 10 FOR EACH CELL SET... which is
	 * just 10 when isAutosolving is false, ie we are in "normal" mode.
	 */
	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		assert grid != null;
		assert cell != null;
		assert value > 0;
		if ( Run.isBatch() && !isAutosolving )
			return grid.cells[cell.indice].set(value);
		return grid.cells[cell.indice].set(value, 0, isAutosolving, funnySB) * 10; // throws UnsolvableException
	}

	/**
	 * @return a score of 10 for setting the cell plus the number of maybes
	 * that would be removed if we applied this hint right now. This value
	 * is used to sort hints, so it is cached, so it must remain consistent.
	 */
	@Override
	public int getNumElims() {
		if(numElims!=0) return numElims;
		int ttlElims = 0;
		for ( ARegion r : cell.regions )
			ttlElims += r.numPlaces[value];
		// + 7 = 10 for cellSet - the 3 regions (each includes me in its idxsOf)
		return numElims = ttlElims + 7;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return Idx.of(cell.indice);
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		return new Pots(cell.indice, value);
	}

	@Override
	public Pots getRedPots(int viewNum) {
		final int values = cell.maybes & ~VSHFT[value];
		if ( values == 0  )
			return null;
		return new Pots(cell.indice, values, false);
	}

	/** @param o Object other
	 * @return Is this hint equivalent to this $other Object? */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ADirectHint))
			return false;
		ADirectHint other = (ADirectHint)o;
		return cell==other.cell
			&& hinter==other.hinter
			&& value==other.value;
	}

	@Override
	public int hashCode() {
		return cell.indice ^ hinter.hashCode() ^ value;
	}

	/**
	 * Returns a String representation of a direct hint, which sets a cells
	 * value.
	 * <pre>
	 * Format: "$cell.id=$value[ in $region.id]"
	 * where " in $region" is included if the given region is not null.
	 * </pre>
	 *
	 * @return A String representing this hint
	 */
	@Override
	public StringBuilder toStringImpl() {
		final StringBuilder sb = SB(13);
		sb.append(cell.id).append(EQUALS).append(value);
		if ( region != null )
			sb.append(IN).append(region.label);
		return sb;
	}

}
