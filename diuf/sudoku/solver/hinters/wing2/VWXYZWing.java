/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 * This class was boosted from Sukaku.
 */
package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.Values.VSHFT;


/**
 * Implementation of the VWXYZ-Wing solving technique. Similar to ALS-XZ with
 * the smaller ALS being a bivalue cell; also catches the double linked version
 * which is similar to Sue-De-Coq. The larger ALS is 5 cells with 6 maybes
 * between them. All wings are pre-term chaining, sans humidicrib. I don't like
 * them; they wet there nests... but they're fast!
 */
public class VWXYZWing extends ABigWingHinter {
	
	/**
	 * Do the wing cells form a VWXYZ-Wing pattern with the yz-cell on value?
	 * All wing cells which maybe value must see the yz-cell.
	 * @param value the potential value of yz-cell to test
	 * @param vwxyz first wing cell
	 * @param vz second wing cell
	 * @param wz third wing cell
	 * @param xz fourth wing cell
	 * @param yz our "other" bivalue cell sees atleast one of the wing cells.
	 * @return Do the wing-cells and yz form a VWXYZ-Wing pattern on value?
	 *  ie: Do all wing cells which maybe value must see yz?
	 */
	private static boolean isWing(int value, Cell vwxyz, Cell vz, Cell wz
			, Cell xz, Cell yz) {
		final int sv = VSHFT[value]; //shiftedValue
		// foreach wing cell: cell is NOT value || cell sees yz
		// ergo: all wing cells which maybe value see yz.
		return ((vwxyz.maybes.bits & sv)==0 || yz.sees[vwxyz.i])
			&& ((vz.maybes.bits & sv)==0 || yz.sees[vz.i])
			&& ((wz.maybes.bits & sv)==0 || yz.sees[wz.i])
			&& ((xz.maybes.bits & sv)==0 || yz.sees[xz.i]);
	}

	public VWXYZWing() {
		super(Tech.VWXYZ_Wing);
	}

	/**
	 * Finds first/all VWXYZ-Wing hint/s in the given Grid, depending on which
	 * IAccumulator you pass. Hints are added to the IAccumulator, and if add
	 * returns true then the search exits-early.
	 * <ul>
	 * <li>VWXYZ-Wing</li>
	 * <li>ALS-XZ with a bivalue cell</li>
	 * <li>By SudokuMonster 2019</li>
	 * </ul>
	 * <p>
	 * A Brief Explanation: u, v, w, z, y, and z, are potential cell values.
	 * We collect 5 cells which share 6 possible values between them, an almost
	 * locked set (ALS); then we find an associated bivalue cell with 1-or-2 of
	 * it's potential values in the ALS. Look for if the YZ cell is x (sigh)
	 * then does that leave the ALS with sufficient possible values to fill all
	 * of it's cells? How about if YZ is z, does that leave the ALS with enough
	 * values? If the ALS "is broken" when YZ is either of it's possible values
	 * then Houston we have a hint.
	 * <p>
	 * Note that we're doing "faster but limited" chaining here, because it's
	 * faster! A lot like ALS-XZ/Wing/Chains that are extensions of the XY/XYZ
	 * /W-Wing hinter-family, hence this pre-term chaining is named after its
	 * Winged ancestors, not its chaining anticedants. Confused yet? sigh.
	 * Think of all Wings as limited chaining. No chickens here. Try the fish!
	 *
	 * @return were any hint/s found.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// NB: the static XZ class holds the x and z values, as in ALS-XZ.
		// the hint, if we ever find one.
		AHint hint;
		// bitsets: the candidates of n cells combined.
		// wingCands are the candidates in all 5 wing cells
		int cands2, cands3, wingCands;
		// is this an VWXYZ-Wing on x, z, both.
		boolean xWing, zWing;
		// the yz cell must have 2 maybes
		// a constant: contents do not change.
		final BitIdx bivalueCells = grid.getBitIdxBivalue();
		// candidates are cells with maybes.size 2..5
		// the possible vwxyz cells.
		// a constant: contents do not change.
		final BitIdx candidates = grid.getBitIdx((c) -> {
			return c.maybes.size>1 && c.maybes.size<5;
		});
		// Set<Cell>s of possible vz, wz, xz, and yz cells.
		// the instances are fixed, but there contents change.
		final BitIdx vzs = new BitIdx(grid)
			       , wzs = new BitIdx(grid)
			       , xzs = new BitIdx(grid)
			       , yzs = new BitIdx(grid);
		// the XZ class holds the x and z values, as in ALS-XZ;
		// and the both (ie isDoubleLinked) flag.
		final XZ yzVs = new XZ();
		// the BitIdx instance to use in eliminations
		final BitIdx victims = new BitIdx(grid);
		// get cells in grid which maybe each value 1..9
		final BitIdx[] vs = grid.getBitIdxs();
		// the removable (red) Cell=>Values
		final Pots theReds = new Pots();
		// we presume that no hint will be found
		boolean result = false;
		// foreach cell in the grid with 2..5 potential values
		for ( Cell vwxyz : candidates ) // C1
			for ( Cell vz : vzs.setAnd(candidates, vwxyz.forwards()) ) // C2
				if ( VSIZE[cands2=vwxyz.maybes.bits|vz.maybes.bits] < 6
				  && wzs.setAndMin(vzs, vz.forwards(), 2) )
					for ( Cell wz : wzs ) // C3
						if ( VSIZE[cands3=cands2|wz.maybes.bits] < 6
						  && xzs.setAndAny(wzs, wz.forwards()) )
							for ( Cell xz : xzs ) // C4
		//<OUTDENT comment="no wrapping">
		if ( VSIZE[wingCands=cands3|xz.maybes.bits] == 5 ) {
			// 4 cells with 5 values between them is an ALS.
			// These 4 cells are commonly called "the wing cells".
			// The yz-cell is the "other" cell for the VWXYZ-Wing pattern.
			// Potential yz's are bivalue cells visible by wing cell/s.
			yzs.set(vwxyz.visible());
			yzs.addAll(wz.visible());
			yzs.addAll(wz.visible());
			yzs.addAll(xz.visible());
			yzs.remove(vwxyz);
			yzs.remove(wz);
			yzs.remove(wz);
			yzs.remove(xz);
			if ( yzs.retainAllAny(bivalueCells) )
				for ( Cell yz : yzs )
					// to be a yz both it's maybes must be in the wing
					if ( VSIZE[yz.maybes.bits & wingCands] == 2 ) {
						// get the "z" value and the "x" value in ALS-XZ
						yzVs.set(yz);
						// test x and z to see if we're possibly double-linked.
						// isWing = all wing cells which maybe value see yz.
						xWing = isWing(yzVs.x, vwxyz, vz, wz, xz, yz);
						zWing = isWing(yzVs.z, vwxyz, vz, wz, xz, yz);
						if ( xWing || zWing ) {
							// found a VWXYZ-Wing pattern
							if ( !xWing ) // single linked on z
								yzVs.swap();
							// double linked
							yzVs.both = xWing & zWing;
							// the 4 wing cells
							final Cell[] wing = {vwxyz, vz, wz, xz};
							// all 5 cells: wing + yz
							final Cell[] all = {vwxyz, vz, wz, xz, yz};
							// any eliminations?
							Pots reds = eliminate(vs, victims, wingCands, yz
									, yzVs, wing, all, theReds);
							if ( reds != null ) {
								// create hint and add it to the accu
								hint = new VWXYZWingHint(this, reds, wingCands
										, yz, yzVs, wing, all);
								result = true;
								if ( accu.add(hint) )
									return true; // exit early
							}
						}
					}
		}
		//</OUTDENT>
		return result; // were any hint/s found?
	}

}
