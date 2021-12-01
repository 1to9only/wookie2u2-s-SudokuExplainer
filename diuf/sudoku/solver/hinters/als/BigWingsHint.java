package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyMath;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The DTO for a BigWings (S/T/U/V/WXYZ-Wing) hint.
 *
 * @author Keith Corlett 2021 Jan IIRC
 */
class BigWingsHint extends AHint  {

	private static final String[] HINT_TYPE_NAMES = {
		// XY_Wing is DODGY coz it's not "Big" Wing, but they're already in the
		// ALS's, so it's faster/easier to finds-em vs filter-em.
		// Note that I do NOT find XYZ_Wings. See also "special" numberName.
		  Tech.XY_Wing.name()
		, Tech.WXYZ_Wing.name()
		, Tech.VWXYZ_Wing.name()
		, Tech.UVWXYZ_Wing.name()
		, Tech.TUVWXYZ_Wing.name()
		, Tech.STUVWXYZ_Wing.name()
	};

	// this array allows me to avoid doing maths to calculate the difficulty,
	// which tends to end in tears when we discover that double precision
	// floating-point arithmetic does not mean perfect precision floating-point
	// arithmetic, and is therefore commonly a bit wrong, which tends to
	// eventually make everything go horribly wrong, producing screaming users,
	// producing defiant programmers, who really just need a good cry.
	// So avoid calculating comparable doubles wherever possible.
	private static final double[] DIFFICULTIES = { // a better named variable does not exist anywhere in the kindom, except possibly in the Restaurant At the End Of The Universe. Or just because you really really want to!
		// XY_Wing is DODGY coz it's not "Big" Wing, but they're already in the
		// ALS's, so it's faster/easier to finds-em vs filter-em.
		// Note that I do NOT find XYZ_Wings. See also "special" numberName.
		  Tech.XY_Wing.difficulty
		, Tech.WXYZ_Wing.difficulty
		, Tech.VWXYZ_Wing.difficulty
		, Tech.UVWXYZ_Wing.difficulty
		, Tech.TUVWXYZ_Wing.difficulty
		, Tech.STUVWXYZ_Wing.difficulty
	};

	private final int[] wingValues; // als values - yz values
	private final Cell[] alsCells; // the cells in the ALS (Almost Locked Set)
	private final Cell biv; // the bivalue cell to complete the Wing pattern
	private final int x; // the primary link value (apparently)
	private final int z; // the secondary link value (apparently)
	private final boolean both; // ergo isDoubleLinked on both x and z
	private final Cell[] all; // the ALS cells and the yz Cell

	// use a local size in-place of the "normal" degree
	private final int size; // the ALS cells and the yz Cell

	BigWingsHint(final AHinter hinter, final Pots reds, final Cell biv
			, final int x, final int z, final boolean both, final Als als
			, final Cell[] alsCells, final Pots oranges) {
		super(hinter, reds, null, oranges, null, null, null);
		this.x = x;
		this.z = z;
		this.both = both;
		this.biv = biv;
		this.wingValues = VALUESES[als.maybes ^ biv.maybes];
		// if it's not atleast two cells then just pretend that it it's atleast
		// two cells, rather than breaking everything. sigh.
		this.size = MyMath.max(als.idx.size(), 2);
//		if ( degree<3 || degree>7 ) {
//			Log.teeln("WARN: "+Log.me()+": degree "+degree+" not in 3..7");
//		}
		this.alsCells = alsCells; // copy reused array
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
		if ( both ) { // all green
			for ( Cell c : all ) {
				result.put(c, c.maybes);
			}
		} else { // x green
			for ( Cell c : all ) {
				if ( c.maybe(x) ) {
					result.put(c, VSHFT[x]);
				}
			}
		}
		return result;
	}

	private void link(final int v, final Collection<Link> result) {
		for ( Cell c : alsCells ) {
			if ( c.maybe(v) ) {
				result.add(new Link(biv, v, c, v));
			}
		}
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			final int n = both ? alsCells.length*2 : alsCells.length;
			final Collection<Link> result = new LinkedHashSet<>(n);
			// biv -z-> alsCells
			link(z, result);
			// if I'm doubleLinked, then biv -x-> alsCells
			if ( both ) {
				link(x, result);
			}
			// too many links occlude the hint.
			if ( result.size() > 24 ) {
				result.clear();
			}
			return result;
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println(Log.me()+": "+ ex);
			return null;
		}
	}

	@Override
	protected String getHintTypeNameImpl() {
		// note the - 2 allows XY_Wing, which is NOT a "big" wing, but they're
		// in ALSs already, so it's faster/easier to finds-em than filter-em.
		return HINT_TYPE_NAMES[size-2];
	}

	@Override
	public double getDifficulty() {
		// note the - 2 allows XY_Wing, which is NOT a "big" wing, but they're
		// in ALSs already, so it's faster/easier to finds-em than filter-em.
		return DIFFICULTIES[size-2];
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig ) {
			s += ON+Frmt.csv(wingValues)+AND+"<b>"+z+"</b>";
		}
		return s;
	}

	@Override
	public String toStringImpl() {
		final StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.ssv(alsCells)).append(AND).append(biv.id)
		  .append(ON).append(x);
		if ( both ) {
			sb.append(AND).append(z);
		}
		return sb.toString();
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
		for ( Cell c : all ) {
			hc ^= c.hashCode;
		}
		return hc;
	}

	@Override
	public String toHtmlImpl() {
		// double-linked wings are called rings, so display BigRingsHint.html
		// providing a more specific explanation.
		String filename = both ? "BigRingsHint.html" : "BigWingsHint.html";
		return Html.produce(hinter, filename
			, Frmu.csv(alsCells)	 //{0}
			, Frmu.and(alsCells)	 // 1
			, biv.id				 // 2
			, x						 // 3 primary
			, z						 // 4 secondary
			, reds.toString()		 // 5
			, Integer.toString(size) // 6
			, numberName(size-1)	 // 7 numWingCells
			, numberName(size-2)	 // 8 numAlsCells
			, numberName(size-3)	 // 9 numOneShort
			, getHintTypeName()		 // 10
			// double-linked wings are called rings (used in BigRingsHint only)
			, hinter.tech.name().replaceFirst("Wing", "Ring") // 11
		);
	}

}
