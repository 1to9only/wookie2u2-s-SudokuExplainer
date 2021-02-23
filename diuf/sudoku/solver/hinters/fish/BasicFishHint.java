/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * A BasicFishHint is raised by the BasicFisherman.
 */
public final class BasicFishHint extends AHint
		implements IActualHint, IChildHint {

	final int valueToRemove;
	final Set<Cell> aquaCells;
	final Pots greenPots;
	final List<ARegion> bases;
	final List<ARegion> covers;
	String debugMessage;

	/**
	 * Construct a new BasicFishHint. You can tell which fish by the degree:
	 * 2=Swampfish, 3=Swordfish, 4=Jellyfish.
	 * @param hinter the AHinter which created this hint.
	 * @param valueToRemove the int value to be removed from whatever cells.
	 * @param greenPots the highlighted (green) potential values.
	 * @param redPots the removable (red) potential values.
	 * @param bases regions to highlight in blue
	 * @param covers regions to highlight in green
	 * @param debugMessage appears in the hint below the title
	 */
	public BasicFishHint(AHinter hinter, Pots redPots, int valueToRemove
			, Pots greenPots, List<ARegion> bases, List<ARegion> covers
			, String debugMessage) {
		super(hinter, AHint.INDIRECT, null, 0, redPots);
		this.valueToRemove = valueToRemove;
		this.aquaCells = greenPots.keySet();
		this.greenPots = greenPots;
		this.bases = bases;
		this.covers = covers;
		this.debugMessage = debugMessage;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return aquaCells;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return greenPots;
	}

	@Override
	public Pots getReds(int viewNum) {
		return redPots;
	}

	@Override
	public List<ARegion> getBases() {
		return bases;
	}

	@Override
	public List<ARegion> getCovers() {
		return covers;
	}

	@Override
	public double getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		double d = super.getDifficulty();
		// 2=Swampfish=3.2, 3=Swordfish=3.8, 4=Jellyfish=5.3
		return d;
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet prntOffs) {
		MyLinkedList<Ass> result = null;
		// make basesAndCovers null safe
		final Collection<ARegion> bases = this.bases;
		if ( bases == null )
			return result;
		final int v = this.valueToRemove;
		for ( ARegion base : bases )
			for ( Cell c : base.cells )
				if ( initGrid.cells[c.i].maybes.contains(v)
				  && currGrid.cells[c.i].maybes.no(v)
				  && !Regions.contains(covers, c) ) {
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
		return this.degree * 10 + aquaCells.size();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BasicFishHint))
			return false;
		BasicFishHint other = (BasicFishHint)o;
		if (this.valueToRemove != other.valueToRemove)
			return false;
		if (this.aquaCells.size() != other.aquaCells.size())
			return false;
		return this.aquaCells.containsAll(other.aquaCells);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Cell c : aquaCells )
			result = result<<4 ^ c.hashCode;
		result = result<<4 ^ valueToRemove;
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a " + getHintTypeName()
			+(isBig ? " on <b>"+valueToRemove+"</b>" : "");
	}

	@Override
	public String toStringImpl() {
		if ( toString == null ) {
			try {
				StringBuilder sb = new StringBuilder(128);
				sb.append(getHintTypeName()).append(": ");
				Frmt.basesAndCovers(sb, bases, covers);
				sb.append(" on ").append(valueToRemove);
				toString = sb.toString();
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
		return toString;
	}
	private String toString;

	// This method yields a special-case common name "Swampfish (nee X-Wing)"
	// in the HTML tech name, as apposed to just the "ubiqitous" Tech.nom.
	// NB: this effects only the HTML, not the Hints TreeView (et al).
	private String getHtmlHintTypeName() {
		return getHintTypeName().replaceFirst("Swampfish"
				, "Swampfish (aka X-Wing)");
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "BasicFishHint.html"
			, getHtmlHintTypeName()					// {0} "Swampfish (nee X-Wing)"
			, Integer.toString(valueToRemove)		//  1
			, Regions.typeName(bases)				//  2
			, Regions.typeName(covers)				//  3
			, degree<2?"":NUMBER_NAMES[degree-2]	//  4 BasicFishHint only
			, debugMessage							//  5 debugMessage is always an empty String by default
			, redPots.toString()					//  6
		);
	}

}
