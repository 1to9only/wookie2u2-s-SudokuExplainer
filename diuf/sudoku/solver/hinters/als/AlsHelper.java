/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * Static helper methods for Als's.
 * <p>
 * I have no idea how to do this properly, so I'm just stick it in a separate
 * helper class. In the GUI, I need to convert an alsId: a..z,aa..az into an
 * index, but data concerns are NOT the GUI's, so I feel it should be done in
 * the als package, to keep all my ALS s__t together, so here it is.
 * <p>
 * I then decided to put the alsId method in here as well, to go both ways.
 *
 * @author Keith Corlett 2021-10-31
 */
public class AlsHelper {

	/**
	 * Return the hint.alss index of alsId in a..z,aa..az.
	 * <p>
	 * In current practice, the highest known used alsId is 'x'.
	 *
	 * @param alsId a..z,aa..az
	 * @return index 0..52, but current highest for 'x' is 24
	 */
	public static int alsIndex(String alsId) {
		try {
			int i = alsId.charAt(0) - 'a';
			if ( alsId.length() == 2 )
				i = i*26 + (alsId.charAt(1) - 'a');
			return i;
		} catch (Exception eaten) {
			return -1; // NONE
		}
	}

	/**
	 * ALS labels are a..z, aa..az.
	 * <p>
	 * NB: filterHints hides the big alss.lengths reported in batch logs coz it
	 * orders by {numElims D, numAlss, numCells} to show the shortest path,
	 * which is NEVER huge. 26 biggest I've ever seen in the GUI, when batch
	 * reported 33 (its degree was 52). So I drop Tech.ALS_Chain.degree to 26!
	 *
	 * @param i
	 * @return
	 */
	public static String alsId(int i) {
		if ( i < 26 )
			return ""+((char)('a'+i)); // a..z
		// beyond 52 breaks these labels (my max is 32)
		return "a"+((char)('a'+i%26)); // aa..az
	}

	private AlsHelper() { } // Never used

}
