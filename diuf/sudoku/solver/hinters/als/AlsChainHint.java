/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.als.AlsHintHelper.link;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;

/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsChainHint extends AHint  {

	/**
	 * This Comparator is a delegate in {@link AHint#BY_ORDER} so that the GUI
	 * shows the shortest possible path to each number of eliminations.
	 * <p>
	 * PRE: a!=b and number of elims are equal
	 */
	public static final Comparator<AlsChainHint> BY_LENGTH = (AlsChainHint a, AlsChainHint b) -> {
		int ret;
		// by length ASCENDING
		if ( (ret=Integer.compare(a.alss.length, b.alss.length)) != 0 ) {
			return ret;
		}
		// then by total length of ALS's in the AlsChain ASCENDING
		// first one that's shorter (faster than total-shorter).
		if ( (ret=Integer.compare(a.totalSize(), b.totalSize())) != 0 ) {
			return ret;
		}
		// 0 doesn't mean equal, it means continue with the master comparator;
		// ergo equals as far as I know (which is just part of it)
		return 0;
	};

	// would be private except for the test-case
	final Als[] alss;
	final int[] values;
	final Rcc[] rccs;
	final int[] rccIndexes;

	public AlsChainHint(
			  final AHinter hinter
			, final Pots reds
			, final Als[] alss
			, final int[] values
			, final Rcc[] rccs
			, final int[] rccIndexes // pass me !null to turn on DEBUG
	) {
		// nb: what are normally greens are oranges here
		super(hinter, reds);
		this.alss = alss;
		this.values = values;
		this.rccs = rccs;
		this.rccIndexes = rccIndexes;
	}

	@Override
	public Als[] getAlss() {
		return alss;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		// NSN means Never Say Never: handle crap in any dodgy hints.
		final int last = alss.length - 1;
		if ( values==null || alss==null || last<1 ) {
			return null; // Never happens. NSN.
		}
		// use a Set for uniqueness (duplicates never happen. NSN.).
		final Collection<Link> result = new LinkedHashSet<>();
		// links aren't mission critical, so just log any exceptions.
		try {
			// foreach als except the last (done seperately)
			for ( int i=0; i<last; ++i ) {
				final int next = i + 1;
				final int v = values[next]; // ALSs are back-linked
				link(alss[i].vs[v], alss[next].vs[v], v, result);
			}
			// the eliminations are linked FROM both the firstAls and the lastAls.
			// backwards: foreach dst, foreach src to link ONLY to elim'd values.
			reds.entrySet().forEach((e) -> {
				final int d = e.getKey().i;
				for ( int z : VALUESES[e.getValue()] ) {
					link(alss[0].vs[z], d, z, result); // first ALS
					link(alss[last].vs[z], d, z, result); // last ALS
				}
			});
		} catch (Exception ex) {
			Log.teeln(Log.me()+": "+ex);
		}
		return result;
	}

	// get CSV of id's of the regions that contain my alss ONCE, for speed
	private String regionsCsv() {
		if ( regionsCsv == null ) {
			regionsCsv = Frmu.csv(Als.regionsList(alss));
		}
		return regionsCsv;
	}
	private String regionsCsv;

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig ) {
			s += IN+regionsCsv();
		}
		return s;
	}

	@Override
	public String toStringImpl() {
		if ( AlsChainDebug.HINTS ) // include the alss.length
			return Frmu.getSB().append(getHintTypeName()).append(COLON_SP)
			  .append(alss.length).append(COLON_SP)
			  .append(regionsCsv())
			  .toString();
		return Frmu.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(regionsCsv())
		  .toString();
	}

	// for the BY_LENGTH Comparator
	private int totalSize() {
		int sum = 0;
		for ( Als a : alss )
			sum += a.idx.size();
		return sum;
	}

	// just occassionally, there are multiple z-values.
	private String zsString() {
		return Values.andString(reds.valuesOf());
	}

	private String getAlssString() {
		// NOTE: The HTML produced here appears inside a PRE-block.
		int i = 0 // als index
		  , c; // color index is 1..5 inclusive
		String label;
		final int last = alss.length - 1;
		final StringBuilder sb = Frmu.getSB();
		// pluralise digits, ie 5s and 9s
		final String zs = zsString().replaceAll("(\\d)+", "$1s");
		for ( Als als : alss ) {
			// the last als in alss is sometimes null, so don't ask me, I just
			// ignore the bastard, because it was there and I can. So there!
			if ( als != null ) {
				// Html.colorIn has 7 <b*> colors, aligned with SudokuGridPanel
				// ALS_COLORS/ALS_BG_COLORS, so the ALS-string and the grid-ALS
				// are the same color for readability, which is still an issue,
				// so double-click on the alsId to highlight this ALS in grid.
				c = (i%7) + 1;
				label = AlsHelper.alsId(i);
				sb.append("    (").append(label).append(") ")
				  .append("<b").append(c).append('>');
				if ( AlsChainDebug.HINTS ) {
					sb.append(als.index).append(": ");
				}
				sb.append(als.region.id).append(": ")
				  .append(als.idx.ids(" "))
				  .append(" {").append(Values.toString(als.maybes)).append("}");
				if ( i < last ) {
					sb.append(" on ").append(values[i+1]);
					if ( AlsChainDebug.HINTS ) {
						sb.append(" in rccs[").append(rccIndexes[i+1]).append("]=")
						  .append(rccs[i+1]);
					}
				} else {
					sb.append(" eliminating ").append(zs)
					  .append(" that see all ").append(zs)
					  .append(" in both (").append(label).append(") and (a)");
				}
				sb.append("</b").append(c).append('>').append(NL);
				++i;
			}
		}
		// remove the trailing NL
		sb.setLength(sb.length()-1);
		// colorIn coz that's usually cached in load (not in format/produce).
		return Html.colorIn(sb.toString());
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "AlsChainHint.html"
			, getAlssString()			//{0}
			, zsString()				// 1
			, Frmu.and(reds.keySet())	// 2
			, debugMessage				// 3
			, reds.toString()			// 4
		);
	}

}
