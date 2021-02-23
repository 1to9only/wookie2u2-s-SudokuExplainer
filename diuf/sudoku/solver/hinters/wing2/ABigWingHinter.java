/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * An abstract base-class implementing methods common to all "big wings".
 *
 * @author Keith 2021-01-04
 */
abstract class ABigWingHinter extends AHinter {

	/**
	 * Eliminate 'value' from the 'wing' cells, adding any eliminations to
	 * theReds, and return were any eliminations found?
	 * <p>
	 * This one method does both weak and strong eliminations.<ul>
	 * <li>For weak pass: cells = wing, yz = yz.
	 * <li>For strong pass: cells = all (wing + yz), yz = null.
	 * </ul>
	 *
	 * @param victims contents don't matter, I'm re-using THE instance.
	 * @param value the value to eliminate
	 * @param cells either wing (for weak) or all (for strong) cells
	 * @param yz required for weak, null for strong
	 * @return where any eliminations found?
	 */
	private static boolean elim(BitIdx victims, int value
			, Cell[] cells, Cell yz, Pots theReds) {
		final int sv = VSHFT[value]; //shiftedValue
		for ( Cell c : cells )
			if ( (sv & c.maybes.bits) != 0 )
				victims.retainAll(c.visible());
		if ( yz != null )
			victims.remove(yz);
		if ( victims.isEmpty() )
			return false;
		boolean result = false;
		for ( Cell victim : victims )
			result |= theReds.upsert(victim, value);
		return result;
	}

//NOT USED, coz I'm slower! Each "big wing" implements it's own method.
//	/**
//	 * Do all the wing cells which maybe value see the yz-cell.
//	 *
//	 * @param value
//	 * @param yz
//	 * @param xz
//	 * @param wz
//	 * @param vz
//	 * @param uz
//	 * @param tz
//	 * @param tuvwxyz
//	 * @return
//	 */
//	static boolean isWing(int value, Cell yz, Cell[] wing) {
//		final int sv = VSHFT[value];
//		for ( Cell c : wing )
//			if ( (c.maybes.bits & sv)!=0 && yz.notSees[c.i] )
//				return false;
//		return true;
//	}

	/**
	 * Find any eliminations in these wing cells.
	 * <p>
	 * NB: the x and z values, and both are in an instance of the XZ class, to
	 * enable me to set both and swap x and z, and have my caller see those
	 * changes; ie it's a mutable DTO, which suits multithreading, even though
	 * this code is not currently (ie yet) multithreaded. For efficiency, we
	 * create a single instance of XZ at the start of each BigWing.findHints
	 * and pass it around; so that each thread has it's own single instance.
	 *
	 * @param vs cells in grid which maybe each value 1..9
	 * @param victims contents don't matter, I'm re-using THE instance.
	 * @param wingCands the candidates of all the wing cells combined
	 * @param yz required for weak, null for strong
	 * @param wing the wing cells
	 * @param all the wing cell + the "other" yz cell.
	 * @param theReds the Pots (a single instance) to add eliminations too.
	 *  This parameter exists only for speed. We use a single instance rather
	 *  than create an empty Pots 99+% of eliminate calls.<br>
	 *  If there are any eliminations theReds is copied, and cleared; so it's
	 *  ALWAYS empty upon return. The returned Pots contains the eliminations.
	 * @return the eliminations, if any; else null.
	 */
	protected static Pots eliminate(final BitIdx[] vs, BitIdx victims
			, int wingCands, Cell yz, XZ yzVs, Cell[] wing, Cell[] all, Pots theReds) {
			
		// find eliminations
		// find strong zValue links
		final boolean strongZ = elim(victims.set(vs[yzVs.z]), yzVs.z, all, null
				, theReds);
		if ( yzVs.both ) {
			// find strong xValue links
			final boolean strongX = elim(victims.set(vs[yzVs.x]), yzVs.x, all
					, null, theReds);
			// find weak weakValues links
			boolean weak = false;
			for ( int w : VALUESES[wingCands ^ yz.maybes.bits] ) // weakCands
				weak |= elim(victims.set(vs[w]), w, wing, yz, theReds);
			// is it really double linked?
			if ( !weak ) {
				if ( !strongZ ) {
					yzVs.both = false;
					yzVs.swap();
				} else if ( !strongX )
					yzVs.both = false;
			}
		}
		if ( theReds.isEmpty() )
			return null; // none
		Pots reds = new Pots(theReds);
		theReds.clear();
		return reds;
	}

	protected ABigWingHinter(Tech tech) {
		super(tech);
	}

}
