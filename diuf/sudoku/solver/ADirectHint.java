/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.utils.Frmt.EQUALS;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.IN;


/**
 * ADirectHint is an abstract class which extends AHint (the abstract hint) to
 * represent a hint that directly sets the value of a Cell in the Sudoku Grid,
 * ie: NakedSingle and HiddenSingle.
 * <p>
 * Subsequently I found that Direct Naked/Hidden Pair/Triple/Quad are "direct"
 * by the above definition, even though they produce Indirect Hints, so I've
 * remodelled everything to mostly remove the mostly pointless delineation
 * between direct and indirect hints. This ADirectHint class now just provides
 * default implementations of methods like getRegion, getAquaCells, etc.
 * <p>
 * The LogicalSolver now deals with ALL hints as AHint's (which I extend) and
 * ALL hinters as IHinter, which all hinters implement. I judge that there's
 * now zero gain from abstracting AHint out into an IHint (etc) interface.
 * Everything that is a hint extends AHint for it's default implementation,
 * so AHint is also the reference type. If that ever changes then create an
 * IHint interface and change all references from AHint to IHint. Sigh.
 */
public abstract class ADirectHint extends AHint  {

	private final ARegion region; // The concerned region, if any

	// caches
	private int numElims;
	private String ts;

	/**
	 * Create a new hint.
	 * @param hinter the Hinter which discovered this hint
	 * @param region the region for which the hint is applicable, else null
	 * @param cell the cell in which a value can be placed
	 * @param value the value that can be placed in the cell
	 */
	public ADirectHint(AHinter hinter, ARegion region, Cell cell, int value) {
		super(hinter, cell, value);
		assert cell!=null && value!=0;
		this.region = region;
		// mark this cell as having an unapplied naked/hidden single, so that
		// Chains applies all singles before making assumptions.
		cell.skip = true;
	}

	/** @return the Region which contains the Cell of this (Hidden Single)
	 * hint. */
	protected ARegion getRegion() {
		return region;
	}

	/** @return An array of the Regions of this hint. */
	@Override
	public List<ARegion> getBases() {
		if ( region == null )
			return null;
		return Regions.list(region);
	}

	/**
	 * To apply a "direct" hint to the grid we just set the Cell to the value.
	 * <p>
	 * NB: redPots is always null for a "direct" hint (never empty).
	 * <p>
	 * I don't know how to score! (Sigh) I think that returning a score of 10
	 * for each cell set is enough to make the setting of a cell preferable to
	 * any/all removing of maybes; ie I think 9 is the most number of maybes
	 * that CAN be removed by setting a Cell IN A VALID SUDOKU (if not when
	 * building a Sudoku). What we want is for the most effective hint found to
	 * be applied. So I keep mulling-over adding the number of maybes actually
	 * eliminated to a reduced set-cell-constant. But 10 seems to work for now,
	 * just watch this space... that's all.
	 * <p>
	 * I guess a mathematician could give a reasonable answer to this question.
	 * I have mere instinct. Fresh meat is important, not the size/precision of
	 * your spear.
	 *
	 * @param isAutosolving true from LogicalSolver.solve makes me use Cells
	 * set(isAutosolving=true) instead of just plain old set.
	 * @return the "score" which is 10 for each cell set. When isAutosolving
	 * is true the setting of one cell may cause the setting of another, which
	 * sets another. So setting one cell may cascade to fill the whole grid.
	 * So my return value is the "score", ie 10 FOR EACH CELL SET... which is
	 * just 10 when isAutosolving is false, ie we're in "normal" mode.
	 */
	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		return cell.set(value, 0, isAutosolving, SB) * 10; // throws UnsolvableException
	}

	/** @return a score of 10 for setting the cell plus the number of maybes
	 * that'd be removed if we applied this hint right now. This value is used
	 * to sort hints, so it's cached, so it must remain consistent. */
	@Override
	public int getNumElims() {
		if(numElims!=0) return numElims;
		int ttlElims = 0;
		for ( ARegion r : cell.regions )
			ttlElims += r.indexesOf[value].size;
		// + 7 = 10 for cellSet - the 3 regions (each includes me in its idxsOf)
		return numElims = ttlElims + 7;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return Collections.singleton(cell);
	}

	@Override
	public Pots getGreens(int viewNum) {
		return new Pots(cell, new Values(value));
	}

	@Override
	public Pots getReds(int viewNum) {
		int bits = cell.maybes.bits & ~VSHFT[value];
		if ( bits == 0  )
			return null;
		return new Pots(cell, new Values(bits, false));
	}

	/** @param o Object other
	 * @return Is this hint equivalent to this 'other' Object? */
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
		return cell.hashCode() ^ hinter.hashCode() ^ value;
	}

	/** @return A String representation of this hint.
	 * Format: "$id=$value in $region". */
	@Override
	public String toStringImpl() {
		final StringBuilder sb = new StringBuilder(13);
		sb.append(cell.id).append(EQUALS).append(value);
		if ( region != null )
			sb.append(IN).append(region.id);
		return sb.toString();
	}
}