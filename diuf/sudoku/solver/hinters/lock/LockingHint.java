/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.Ass;
import diuf.sudoku.Regions;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.and;


/**
 * A LockingHint is raised by Locking <b>OR</b> the Fisherman. You can
 * tell by the degree:
 * <pre><tt>
   Locking: 1=Pointing or Claiming
   Fisherman    : 2=Swampfish(X-Wing), 3=Swordfish, 4=Jellyfish
 </tt></pre>
 * <p>This is the only (at time of writing) place where two rules produce the
 *  same type of hint. It's all a bit odd, so shoot me.
 */
public final class LockingHint extends AHint implements IChildHint {

	final int valueToRemove;
	final Set<Cell> cellSet;
	final ARegion base;
	final ARegion cover;
	final boolean isPointing; //true=Pointing, false=Claiming
	String debugMessage;

	/**
	 * Construct a new LockingHint.
	 * @param hinter the AHinter which created this hint.
	 * @param valueToRemove the int value to be removed from whatever cells.
	 * @param greenPots the highlighted (green) potential values.
	 * @param redPots the removable (red) potential values.
	 * @param base is the region to highlight in blue.
	 * @param cover is the region to highlight in green.
	 * @param debugMessage appears in the hint below the title
	 */
	public LockingHint(AHinter hinter, int valueToRemove, Pots greenPots
			, Pots redPots, ARegion base, ARegion cover
			, String debugMessage) {
		super(hinter, AHint.INDIRECT, null, 0, redPots, greenPots, null, null
				, Regions.list(base), Regions.list(cover));
		this.valueToRemove = valueToRemove;
		this.cellSet = greenPots.keySet();
		this.base = base;
		this.cover = cover;
		this.isPointing = base instanceof Box;
		this.debugMessage = debugMessage;
	}

	/**
	 * Returns an idx of the cells in this hint.
	 * <p>
	 * Note that idx is only called by Locking.mergeSiameseHints and
	 * SiameseLockingHint constructor; ie only in GUI, so speed not critical.
	 * @return
	 */
	public Idx idx() {
		if ( idx == null )
			idx = new Idx();
		else
			idx.clear();
		for ( Cell cell : cellSet )
			idx.add(cell.i);
		return idx;
	}
	private Idx idx; // cellSet index cache

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return cellSet;
	}

	// Weird: Locking is only place we use one Tech for two hint-types.
	@Override
	public double getDifficulty() {
		double d = super.getDifficulty();
		if ( !isPointing ) // Claiming
			d += 0.1;
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
		for ( Cell c : base.cells )
			if ( initGrid.cells[c.i].maybes.contains(v)
			  && currGrid.cells[c.i].maybes.no(v)
			  && !cover.contains(c) ) {
				if(result==null) result = new MyLinkedList<>();
				result.add(prntOffs.getAss(c, v));
			}
		if (result==null)
			throw new UnsolvableException("Not a chaining hint!");
		return result;
	}

	/**
	 * HdkFisherman uses complexity() to store the simplest hint which produces
	 * these eliminations in a Map of redPots => AHint, to reduce the number of
	 * superfluous hints it was reporting (far too many).
	 * <p>
	 * Note that is implementation is a kludge. Ideally we'd compare the hint
	 * types primarily and then the cellSet.size(), but I have no idea how to
	 * efficiently compare the hint-types, so I haven't bothered, coz I'm lazy.
	 * The only way I can see to compare hint-types is by a toString, which is
	 * a ____in DOG, so no I'm not gunna. Don wanna. Not gunna!
	 *
	 * @return this.degree * 10 + cellSet.size();
	 */
	@Override
	public int complexity() {
		// nb: degree should be same for all hints in complexity() comparison,
		//     coz that's done in a hinter. It's included just to be thorough.
		return this.degree * 10 + cellSet.size();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LockingHint))
			return false;
		LockingHint other = (LockingHint)o;
		if (this.valueToRemove != other.valueToRemove)
			return false;
		if (this.cellSet.size() != other.cellSet.size())
			return false;
		return this.cellSet.containsAll(other.cellSet);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Cell c : cellSet )
			result = result<<4 ^ c.hashCode;
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
	public String toStringImpl() {
		return Frmu.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(base).append(AND).append(cover)
		  .append(ON).append(valueToRemove)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		// WARN: LockingHint.html is also used in SiameseLockingHint,
		// so any arguements changes here must also happen there.
		return Html.produce(this, "LockingHint.html"
				, getHintTypeName()						// {0}
				, Integer.toString(valueToRemove)		//  1
				, base.typeName							//  2
				, cover.typeName						//  3
				, redPots.toString()					//  4
				, debugMessage							//  5
		);
	}

}
