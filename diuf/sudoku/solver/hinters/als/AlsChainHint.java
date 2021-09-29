/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.List;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.IN;


/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsChainHint extends AHint  {

	// would be private except for the test-case
	final List<Als> alss;
	final String debugMessage;

	public AlsChainHint(AHinter hinter, Pots redPots, List<Als> alss
			, String debugMessage) {
		// nb: what are normally greens and oranges here
		super(hinter, redPots);
		this.alss = alss;
		this.debugMessage = debugMessage;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += IN+Frmu.csv(Als.regionsList(alss));
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmu.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(Als.regionsList(alss)))
		  .toString();
	}

	@Override
	public Collection<Als> getAlss() {
		return alss;
	}

	private String getAlssString() {
		// NOTE: My HTML appears inside a PRE-block.
		StringBuilder sb = Frmu.getSB();
		int i = 0, c; String r;
		for ( Als als : alss ) {
			if ( als != null ) { // the last als in alss may be null
				//Produces for example:
				//     (a) <b1>row 1: A1 G1 I1</b1>\n
				//     (b) <b2>col G: G1 G6</b2>\n
				if(als.region==null) r=EMPTY_STRING; else r=als.region.id+COLON_SP;
				// there's only 5 colors, same as my largest ALS-Chain
				c = (i%5) + 1;
				sb.append("    (").append(alpha(i)).append(") ") // nb: In Java calculating a char is a complete, total, and utter pain in the ____ing ass. Thank you Buggus Duckus for your charming comments. Your views have been noted, and will be completely ignored in due time. Sigh.
				  .append("<b").append(c).append('>')
				  .append(r).append(Frmu.and(als.cells))
				  .append("</b").append(c).append('>').append(NL);
				++i;
			}
		}
		// remove the trailing NL (or you can't see whole hint of long chain)
		sb.setLength(sb.length()-1);
		// colorIn manually coz that's done in load and cached, not in format.
		return Html.colorIn(sb.toString());
	}
	private char alpha(int i) {
		return (char)('a'+i);
	}

	@Override
	public String toHtmlImpl() {
		// there's only one z-value, it's the same for every cell.
		final int z = redPots.values().iterator().next().first();
		return Html.produce(this, "AlsChainHint.html"
			, getAlssString()				//{0}
			, Integer.toString(z)			// 1
			, Frmu.and(redPots.keySet())	// 2
			, debugMessage					// 3
			, redPots.toString()			// 4
		);
	}
}
