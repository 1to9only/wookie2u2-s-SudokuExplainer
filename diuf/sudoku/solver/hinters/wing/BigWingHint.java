package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
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
class BigWingHint extends AHint implements IActualHint {

	private final int[] wingValues;
	private final int x; // the primary value to eliminate
	private final int z; // the second value in a double-linked Wing
	private final boolean both;
	private final Cell yz;
	private final Cell[] als;
	private final Cell[] all;

	BigWingHint(AHinter hinter, Pots reds, Cell yz, XZ yzVs
			, int alsCands, Cell[] als) {
		super(hinter, reds);
		this.x = yzVs.x;
		this.z = yzVs.z;
		this.both = yzVs.both;
		this.yz = yz;
		this.wingValues = VALUESES[alsCands ^ yz.maybes.bits];
		this.als = als.clone(); // copy reused array
		// all cells = als cells + the yz cell
		this.all = new Cell[als.length+1];
		System.arraycopy(als, 0, all, 0, degree);
		all[degree] = yz;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return Grid.cellSet(als);
	}

	@Override
	public Set<Cell> getPinkCells(int viewNum) {
		return Grid.cellSet(yz);
	}

	@Override
	public Pots getGreens(int viewNum) {
		Pots result = new Pots();
		if ( both ) // all green
			for ( Cell c : all )
				result.put(c, new Values(c.maybes));
		else // z green
			for ( Cell c : all )
				if ( c.maybe(z) )
					result.put(c, new Values(z));
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
			s += " on "+Frmt.csv(wingValues)+" and <b>"+z+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		String s = getHintTypeName()+": "+Frmt.ssv(als)+" and "+yz.id+" on ";
		if ( both )
			s += x + " and " + z;
		else
			s += z;
		return s;
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
		String filename = both ? "BigWing2Hint.html" : "BigWingHint.html";
		return Html.produce(hinter, filename
			, Frmt.csv(als)				//{0}
			, Frmt.and(als)				// 1
			, yz.id						// 2
			, z							// 3
			, x							// 4
			, redPots.toString()		// 5
			, Integer.toString(degree)	// 6
			, NUMBER_NAMES[degree-1]	// 7
			, NUMBER_NAMES[degree-2]	// 8
			, NUMBER_NAMES[degree-3]	// 9
			, hinter.tech.nom			// 10
			, hinter.tech.nom.replaceFirst("-Wing", "-Ring") // 11
		);
	}

}
