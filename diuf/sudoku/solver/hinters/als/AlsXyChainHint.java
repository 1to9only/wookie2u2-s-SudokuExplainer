/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;


/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsXyChainHint extends AHint {

	private final Pots orangePots;
	private final Pots bluePots; // common candidates
	private final List<ARegion> bases; // blue
	private final List<ARegion> covers; // green
	private final List<Als> alss;
	private final String debugMessage;

	public AlsXyChainHint(AHinter hinter, Pots redPots, Pots orangePots
			, Pots bluePots, List<ARegion> bases, List<ARegion> covers
			, List<Als> alss, String debugMessage) {
		super(hinter, redPots);
		this.orangePots = orangePots;
		this.bluePots = bluePots;
		this.bases = bases;
		this.covers = covers;
		this.alss = alss;
		this.debugMessage = debugMessage;
	}

	@Override
	public Set<Grid.Cell> getAquaCells(int viewNumUnused) {
		return orangePots.keySet();
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
	public Pots getOranges(int viewNumUnused) {
		return orangePots;
	}

	@Override
	public Pots getReds(int viewNumUnused) {
		return redPots;
	}

	@Override
	public Pots getBlues(Grid gridUnused, int viewNumUnused) {
		return bluePots; // common candidates
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a " + getHintTypeName()
			// nb: cast to Object[] for nonvargs call and suppress warning
			+ (isBig ? " in "+Frmt.and(bases, covers) : "");
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(getHintTypeName()).append(": ")
		  .append(Frmt.and(bases, covers));
		return sb.toString();
	}

	private String getAlssString() {
		StringBuilder sb = Frmt.getSB();
		int i = 0;
		// NOTE: This HTML is inside a PRE-block.
		for ( Als als : alss ) {
			//Produces for example:
			//     (a) <b1>row 1: A1 G1 I1</b1>\n
			//     (b) <b2>col G: G1 G6</b2>\n
			int j = 1 + (i%3); // there's only 3 colors, so cycle through them
			sb.append("    (").append(alpha((char)i)).append(") ") // nb: In Java calculating a char is a complete, total, and utter pain in the ____ing ass. Thank you Buggus Duckus for your charming comments. Your views have been noted, and will be completely ignored in due time. Sigh.
			  .append("<b").append(j).append('>')
			  .append(als.region==null ? "" : als.region.id+": ")
			  .append(Frmt.and(als.cells))
			  .append("</b").append(j).append('>').append(NL);
			++i;
		}
		// remove the trailing NL (or you can't see whole hint of long chain)
		sb.setLength(sb.length()-1);
		// colorIn manually coz that's done in load and cached, not in format.
		return Html.colorIn(sb.toString());
	}
	private char alpha(char i) {
		return (char)('a'+i);
	}

	@Override
	public String toHtmlImpl() {
		// there's only one z-value, it's the same for every cell.
		final int z = redPots.values().iterator().next().first();
		return Html.produce(this, "AlsXyChainHint.html"
			, getAlssString()				//{0}
			, Integer.toString(z)			// 1
			, Frmt.and(redPots.keySet())	// 2
			, debugMessage					// 3
		);
	}
}
