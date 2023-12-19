/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.IFoxyHint;
import diuf.sudoku.Ass;
import static diuf.sudoku.Grid.REGION_TYPE_NAMES;
import diuf.sudoku.Idx;
import diuf.sudoku.Regions;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.IAssSet;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Set;

/**
 * A LockingHint is raised by Locking <b>OR</b> the Fisherman. You can
 * tell by the degree:
 * <pre><tt>
   Locking: 1=Pointing or Claiming
   Fisherman    : 2=Swampfish(X-Wing), 3=Swordfish, 4=Jellyfish
 </tt></pre>
 * <p>This is the only (at time of writing) place where two rules produce the
 *  same type of hint. It is all a bit odd, so shoot me.
 */
public final class LockingHint extends AHint implements IFoxyHint {

	final int valueToRemove;
	final Idx indices; // of lockedCells
	final ARegion base; // the region lockedCells where found in (blue)
	final ARegion cover; // the other common region of lockedCells (green)
	final boolean isPointing; //true=Pointing, false=Claiming

	/**
	 * Construct a new LockingHint.
	 *
	 * @param grid to search
	 * @param hinter the AHinter which created this hint
	 * @param valueToRemove the int value to be removed from whatever cells
	 * @param greenPots the highlighted (green) potential values
	 * @param redPots the removable (red) potential values
	 * @param base is the region to highlight in blue
	 * @param cover is the region to highlight in green
	 * @param indices of lockedCells
	 */
	public LockingHint(Grid grid, IHinter hinter, int valueToRemove, Pots greenPots
			, Pots redPots, ARegion base, ARegion cover, Idx indices) {
		super(grid, hinter, AHint.INDIRECT, null, 0, redPots, greenPots, null, null
				, Regions.array(base), Regions.array(cover));
		this.valueToRemove = valueToRemove;
		this.indices = indices;
		this.base = base;
		this.cover = cover;
		this.isPointing = base instanceof Box;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNumUnused) {
		return indices;
	}

	// Weird: Locking is only place we use one Tech for two hint-types.
	@Override
	public int getDifficulty() {
		int d = super.getDifficulty();
		if ( !isPointing ) // Claiming
			d += 1;
		return d;
	}

	// Weird: Locking is only place we use one Tech for two hint-types.
	@Override
	public String getHintTypeNameImpl() {
		return isPointing ? "Pointing" : "Claiming";
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet prntOffs) {
		MyLinkedList<Ass> result = null;
		final int v = this.valueToRemove;
		final int sv = VSHFT[v];
		for ( Cell c : base.cells )
			if ( (initGrid.maybes[c.indice] & sv) > 0 // 9bits
			  && (currGrid.maybes[c.indice] & sv) < 1 // 9bits
			  && !cover.contains(c) ) {
				if(result==null) result = new MyLinkedList<>();
				result.add(prntOffs.getAss(c, v));
			}
		if ( result == null )
			// Not a chaining hint! No message, because its eaten
			throw new UnsolvableException();
		return result;
	}

	/**
	 * HdkFisherman uses complexity() to store the simplest hint which produces
	 * these eliminations in a Map of redPots => AHint, to reduce the number of
	 * superfluous hints it was reporting (far too many).
	 * <p>
	 * Note that is implementation is a kludge. Ideally we would compare the hint
	 * types primarily and then the cellSet.size(), but I have no idea how to
	 * efficiently compare the hint-types, so I have not bothered. I am lazy.
	 * The only way I can see to compare hint-types is by a toString, which is
	 * a ____in DOG, so no I am not gunna. Don wanna. Not gunna!
	 *
	 * @return this.degree * 10 + cellSet.size();
	 */
	@Override
	public int complexity() {
		// nb: degree should be same for all hints in complexity() comparison,
		//     coz thats done in a hinter. It is included just to be thorough.
		return this.degree * 10 + indices.size();
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof LockingHint
			&& equals((LockingHint)o);
	}

	public boolean equals(final LockingHint o) {
		return this.valueToRemove == o.valueToRemove
			&& this.indices.equals(o.indices);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( int indice : indices )
			result = result<<4 ^ indice;
		result = result<<4 ^ valueToRemove;
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		// nb: use Impl to swap types, I think. Sigh.
		String s = "Look for a " + getHintTypeNameImpl();
		if ( isBig )
			s += " on <b>"+valueToRemove+"</b>";
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(base).append(AND).append(cover).append(ON).append(valueToRemove);
	}

	@Override
	public String toHtmlImpl() {
		// WARN: LockingHint.html is also used in SiameseLockingHint,
		// so any arguements changes here must also happen there.
		return Html.produce(this, "LockingHint.html"
				, getHintTypeName()					// {0}
				, Integer.toString(valueToRemove)	//  1
				, REGION_TYPE_NAMES[base.rti]		//  2
				, REGION_TYPE_NAMES[cover.rti]		//  3
				, reds.toString()					//  4
				, debugMessage						//  5
		);
	}

}
