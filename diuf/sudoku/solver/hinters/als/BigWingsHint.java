package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;


/**
 * The DTO for a BigWings (S/T/U/V/WXYZ-Wing) hint.
 *
 * @author Keith Corlett 2021 Jan IIRC
 */
class BigWingsHint extends AHint  {

	private static final String[] HINT_TYPE_NAMES = {
		  Tech.XYZ_Wing.name() // DODGY coz it's not Big, but they exist
		, Tech.WXYZ_Wing.name()
		, Tech.VWXYZ_Wing.name()
		, Tech.UVWXYZ_Wing.name()
		, Tech.TUVWXYZ_Wing.name()
		, Tech.STUVWXYZ_Wing.name()
	};

	private final int[] wingValues; // als values - yz values
	private final Cell[] alsCells; // the cells in the ALS (Almost Locked Set)
	private final Cell biv; // the bivalue cell to complete the Wing pattern
	private final int x; // the primary link value (apparently)
	private final int z; // the secondary link value (apparently)
	private final boolean both; // ergo isDoubleLinked on both x and z
	private final Cell[] all; // the ALS cells and the yz Cell

	// This one's WIERD: normally hint.degree is hinter.degree but this time
	// I'm finding hints of ALL degree's in one pass, because I'm parsing a
	// cache of ALS's so it costs me nothing to do them all in one, where-as
	// doing them by degree involves processing the same ALSs 5 times for the
	// same result, so ____ it, I'm overriding my parents degree field so that
	// I can just set it to the number of cells in my ALS.
	private final int degree; // the ALS cells and the yz Cell

	BigWingsHint(final AHinter hinter, final Pots reds, final Cell biv
			, final int x, final int z, final boolean both, Als als
			, Pots oranges) {
		super(hinter, reds, null, oranges, null, null, null);
		this.x = x;
		this.z = z;
		this.both = both;
		this.biv = biv;
		this.wingValues = VALUESES[als.maybes ^ biv.maybes.bits];
		this.degree = als.cells.length;
		assert degree>1 && degree<8 : "WTF: degree "+degree+" not in 2..7";
		this.alsCells = als.cells.clone(); // copy reused array
		// all cells = als cells + the biv cell
		final int n = alsCells.length;
		this.all = new Cell[n+1];
		System.arraycopy(alsCells, 0, all, 0, n);
		all[n] = biv;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return Cells.set(alsCells);
	}

	@Override
	public Set<Cell> getBlueCells(int viewNum) {
		return Cells.set(biv);
	}

	@Override
	public Pots getGreens(int viewNum) {
		Pots result = new Pots();
		if ( both ) // all green
			for ( Cell c : all )
				result.put(c, new Values(c.maybes));
		else // x green
			for ( Cell c : all )
				if ( c.maybe(x) )
					result.put(c, new Values(x));
		return result;
	}

	private void addLinks(Collection<Link> result, int v) {
		for ( Cell c : alsCells )
			if ( c.maybe(v) )
				result.add(new Link(biv, v, c, v));
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			final int n = both ? alsCells.length*2 : alsCells.length;
			final Collection<Link> result = new ArrayList<>(n);
			addLinks(result, z);
			if ( both )
				addLinks(result, x);
			return result;
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("ABigWingHint.getLinks: "+ ex);
			return null;
		}
	}

	@Override
	protected String getHintTypeNameImpl() {
		return HINT_TYPE_NAMES[degree-2]; // note the - 2 allows XYZ-Wing
	}

	@Override
	public double getDifficulty() {
		return Tech.WXYZ_Wing.difficulty + 0.1 * (degree-3);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on "+Frmt.csv(wingValues)+" and <b>"+z+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		String s = getHintTypeName()+": "+Frmt.ssv(alsCells)+" and "+biv.id+" on ";
		if ( both )
			s += x + " and " + z;
		else
			s += x; // primary only
		return s;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof BigWingsHint && equals((BigWingsHint) o);
	}

	public boolean equals(BigWingsHint o) {
		return Arrays.equals(all, o.all);
	}

	@Override
	public int hashCode() {
		// use the Cell.hashCode field instead of calling hashCode()
		int hc = 0;
		for ( Cell c : all )
			hc ^= c.hashCode;
		return hc;
	}

	@Override
	public String toHtmlImpl() {
		// double-linked wings have there own explanation for more eliminations
		String filename = both ? "BigWingsHintDL.html" : "BigWingsHint.html";
		return Html.produce(hinter, filename
			, Frmt.csv(alsCells)		//{0}
			, Frmt.and(alsCells)		// 1
			, biv.id					// 2
			, x							// 3 primary
			, z							// 4 secondary
			, redPots.toString()		// 5
			, Integer.toString(degree)	// 6
			, NUMBER_NAMES[degree-1]	// 7
			, NUMBER_NAMES[degree-2]	// 8
			, NUMBER_NAMES[degree-3]	// 9
			, getHintTypeName()			// 10
			// double-linked wings are called rings (used in BigWingHintDL)
			, hinter.tech.name().replaceFirst("Wing", "Ring") // 11
		);
	}

}
