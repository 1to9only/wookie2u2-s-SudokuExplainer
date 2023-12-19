/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.ON;
import diuf.sudoku.solver.hinters.IFoxyHint;

/**
 * A BasicFishHint is raised by the BasicFisherman (and BasicFisherman). Its
 * the Data Transfer Object (DTO) of a "basic" Fish hint. It provides a textual
 * summary of this hint in the LogicalSolverTester (where speed is THE issue),
 * and also provides HTML to hopefully fully explain this hint to the GUI user
 * (where speed is much less of an issue).
 * <p>
 * BasicFisherman is one of the FOUR QUICK FOXES in the RecursiveSolver (brute
 * force), and in the DynamicPlus ChainerMulti (and NestedPlus); so
 * BasicFishHint implements IChildHint.getParents, enabling us to play "whos
 * your daddy" with our Assumptions.
 */
public final class FishHint extends AHint implements IFoxyHint {

	final int valueToRemove;
	final Set<Integer> indices;

	/**
	 * Construct a new BasicFishHint. You can tell which fish by the degree:
	 * 2=Swampfish, 3=Swordfish, 4=Jellyfish.
	 * @param grid the grid on which this hint sits
	 * @param hinter the AHinter which created this hint.
	 * @param v the int value to be removed from whatever cells.
	 * @param greens the highlighted (green) potential values.
	 * @param reds the removable (red) potential values.
	 * @param bases regions to highlight in blue
	 * @param covers regions to highlight in green
	 */
	public FishHint(final Grid grid, final IHinter hinter, final int v
			, final Pots reds, final Pots greens, final ARegion[] bases
			, final ARegion[] covers) {
		super(grid, hinter, AHint.INDIRECT, null, 0, reds, greens, null, null, bases, covers);
		this.valueToRemove = v;
		this.indices = greens.keySet();
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return indices;
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initialGrid, Grid currentGrid
			, IAssSet parentOffs) {
		if ( bases == null )
			return null;
		MyLinkedList<Ass> result = null; // created on demand
		final int v = this.valueToRemove;
		final int sv = VSHFT[v];
		for ( ARegion base : bases )
			for ( Cell c : base.cells )
				if ( (initialGrid.cells[c.indice].maybes & sv) > 0 // 9bits
				  && (currentGrid.cells[c.indice].maybes & sv) < 1 // 9bits
				  && !Regions.contains(covers, c)
				) {
					if ( result == null )
						result = new MyLinkedList<>();
					result.add(parentOffs.getAss(c.indice, v));
				}
		if ( result == null )
			throw new UnsolvableException("Not a chaining hint!");
		return result;
	}

	/**
	 * HdkFisherman uses complexity() to store the simplest hint which produces
	 * these eliminations in a Map of redPots => AHint, to reduce the number of
	 * superfluous hints it was reporting (far too many).
	 * <p>
	 * Note that is implementation is a kludge. Ideally wed compare the hint
	 * types primarily and then the cellSet.size(), but I have no idea how to
	 * efficiently compare the hint-types, so I havent bothered, coz Im lazy.
	 * The only way I can see to compare hint-types is by a toString, which is
	 * a ____in DOG, so no Im not gunna. Don wanna. Not gunna!
	 *
	 * @return this.degree * 10 + cellSet.size();
	 */
	@Override
	public int complexity() {
		// nb: degree should be same for all hints in complexity() comparison,
		//     coz thats done in a hinter. Its included just to be thorough.
		return this.degree * 10 + indices.size();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FishHint))
			return false;
		FishHint other = (FishHint)o;
		if (this.valueToRemove != other.valueToRemove)
			return false;
		if (this.indices.size() != other.indices.size())
			return false;
		return this.indices.containsAll(other.indices);
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
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += ON+"<b>"+valueToRemove+"</b>";
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		// work that s__t out Scoobie!
		return Frmu.basesAndCovers(SB(64).append(getHintTypeName()).append(COLON_SP)
		, bases, covers).append(ON).append(valueToRemove);
	}

	// This method yields a special-case common name "Swampfish (nee X-Wing)"
	// in the HTML tech name, as apposed to just the "ubiqitous" Tech.name().
	// NB: this effects only the HTML, not the Hints TreeView (et al).
	private String getHtmlHintTypeName() {
		return getHintTypeName().replaceFirst("Swampfish", "Swampfish (aka X-Wing)");
	}

	@Override
	public String toHtmlImpl() {
		// NOTE: Superfisher{man/Perm} size is bases.length, NOT Tech.degree
		final String sizeName = bases.length<2 ? EMPTY_STRING : NUMBER_NAMES[bases.length-2];
		return Html.produce(this, "FishHint.html"
			, getHtmlHintTypeName()				// {0} "Swampfish (nee X-Wing)"
			, Integer.toString(valueToRemove)	//  1
			, Regions.typeName(bases)			//  2
			, Regions.typeName(covers)			//  3
			, sizeName							//  4 number of bases as a word; BasicFishHint only
			, debugMessage						//  5 debugMessage
			, reds.toString()					//  6
		);
	}

}
