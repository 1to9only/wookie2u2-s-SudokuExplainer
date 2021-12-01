package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;


/**
 * The DTO for a "big wing" (S/T/U/V/WXYZ-Wing) hint.
 *
 * @author Keith Corlett 2021 Jan IIRC
 */
class BigWingHint extends AHint  {

	private final int[] wingValues; // als values - yz values
	private final int x; // the primary link value to eliminate
	private final int z; // the secondary link value only in a double-link Wing
	private final boolean both; // are both x and z linked: ie isDoubleLinked
	private final Cell yz; // the bivalue cell to complete the Wing pattern
	private final Cell[] als; // the cells in the ALS (Almost Locked Set)
	private final Cell[] all; // the ALS cells and the yz Cell

	BigWingHint(AHinter hinter, Pots reds, Cell yz, int x, int z, boolean both
			, int alsCands, Cell[] als, Pots oranges) {
		super(hinter, reds, null, oranges, null, null, null);
		this.x = x;
		this.z = z;
		this.both = both;
		this.yz = yz;
		this.wingValues = VALUESES[alsCands ^ yz.maybes];
		this.als = als; // store the given array
		// all cells = als cells + the yz cell
		this.all = new Cell[als.length+1];
		System.arraycopy(als, 0, all, 0, degree);
		all[degree] = yz;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return Cells.set(als);
	}

	@Override
	public Set<Cell> getBlueCells(int viewNum) {
		return Cells.set(yz);
	}

	@Override
	public Pots getGreens(int viewNum) {
		Pots result = new Pots();
		if ( both ) // all green
			for ( Cell c : all )
				result.put(c, c.maybes);
		else // z green
			for ( Cell c : all )
				if ( c.maybe(z) )
					result.put(c, VSHFT[z]);
		return result;
	}

	private void addLinks(Collection<Link> result, int v) {
		for ( Cell c : als )
			if ( c.maybe(v) )
				result.add(new Link(yz, v, c, v));
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			final int n = both ? als.length*2 : als.length;
			Collection<Link> result = new ArrayList<>(n);
			addLinks(result, x);
			if ( both )
				addLinks(result, z);
			return result;
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("ABigWingHint.getLinks: "+ ex);
			return null;
		}
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += ON+Frmt.csv(wingValues)+" and <b>"+z+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		final StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.ssv(als))
		  .append(AND).append(yz.id)
		  .append(ON);
		if ( both )
			sb.append(x).append(AND).append(z);
		else
			sb.append(z);
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof BigWingHint && equals((BigWingHint) o);
	}

	public boolean equals(BigWingHint o) {
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
		String filename = both ? "BigWingHintDL.html" : "BigWingHint.html";
		return Html.produce(hinter, filename
			, Frmu.csv(als)			//{0}
			, Frmu.and(als)			// 1
			, yz.id						// 2
			, z							// 3
			, x							// 4
			, reds.toString()		// 5
			, Integer.toString(degree)	// 6
			, NUMBER_NAMES[degree-1]	// 7
			, NUMBER_NAMES[degree-2]	// 8
			, NUMBER_NAMES[degree-3]	// 9
			, hinter.tech.name()		// 10
			// double-linked wings are called rings (used in BigWingHintDL)
			, hinter.tech.name().replaceFirst("Wing", "Ring") // 11
		);
	}

}
