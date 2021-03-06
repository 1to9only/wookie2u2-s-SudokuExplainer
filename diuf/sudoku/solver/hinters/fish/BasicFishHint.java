/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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
 * A BasicFishHint is raised by the BasicFisherman (and BasicFisherman1). It's
 * the Data Transfer Object (DTO) of a "basic" Fish hint. It provides a textual
 * summary of this hint in the LogicalSolverTester (where speed is THE issue),
 * and also provides HTML to hopefully fully explain this hint to the GUI user
 * (where speed is much less of an issue).
 * <p>
 * BasicFisherman is one of the FOUR QUICK FOXES in the RecursiveSolver (brute
 * force), and in the DynamicPlus MultipleChainer (and NestedPlus); so
 * BasicFishHint implements IChildHint.getParents, enabling us to play "who's
 * your daddy" with our Assumptions.
 */
public final class BasicFishHint extends AHint
		implements IActualHint, IChildHint
{

	final int valueToRemove;
	final Set<Cell> cells;
	final String debugMessage;

	/**
	 * Construct a new BasicFishHint. You can tell which fish by the degree:
	 * 2=Swampfish, 3=Swordfish, 4=Jellyfish.
	 * @param hinter the AHinter which created this hint.
	 * @param v the int value to be removed from whatever cells.
	 * @param greens the highlighted (green) potential values.
	 * @param reds the removable (red) potential values.
	 * @param bases regions to highlight in blue
	 * @param covers regions to highlight in green
	 * @param debugMessage appears in the hint below the title
	 */
	public BasicFishHint(AHinter hinter, Pots reds, int v, Pots greens
			, String debugMessage, List<ARegion> bases, List<ARegion> covers) {
		super(hinter, AHint.INDIRECT, null, 0, reds, greens, null, null, bases
				, covers);
		this.valueToRemove = v;
		this.cells = greens.keySet();
		this.debugMessage = debugMessage;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return cells;
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
			, IAssSet parentOffs) {
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
					result.add(parentOffs.getAss(c, v));
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
		return this.degree * 10 + cells.size();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BasicFishHint))
			return false;
		BasicFishHint other = (BasicFishHint)o;
		if (this.valueToRemove != other.valueToRemove)
			return false;
		if (this.cells.size() != other.cells.size())
			return false;
		return this.cells.containsAll(other.cells);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Cell c : cells )
			result = result<<4 ^ c.hashCode;
		result = result<<4 ^ valueToRemove;
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on <b>"+valueToRemove+"</b>";
		return s;
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
		final String nn; if(degree<2) nn=""; else nn=NUMBER_NAMES[degree-2];
		return Html.produce(this, "BasicFishHint.html"
			, getHtmlHintTypeName()				// {0} "Swampfish (nee X-Wing)"
			, Integer.toString(valueToRemove)	//  1
			, Regions.typeName(bases)			//  2
			, Regions.typeName(covers)			//  3
			, nn								//  4 number name; BasicFishHint only
			, debugMessage						//  5 debugMessage
			, redPots.toString()				//  6
		);
	}

}
