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
 * Implementation of the UVWXYZ-Wing solving technique. Similar to ALS-XZ with
 * the smaller ALS being a bivalue cell; also catches the double linked version
 * which is similar to Sue-De-Coq. The larger ALS is 5 cells with 6 maybes
 * between them. All wings are pre-term chaining, sans humidicrib. I don't like
 * them; they wet there nests... but they're fast!
 */
public class UVWXYZWing extends ABigWingHinter {

	/**
	 * Do the wing cells form a Wing pattern with yz on value?
	 * @param value
	 * @param yz
	 * @param wing an array of the cells in this wing.
	 * @return true if wing and yz form a Wing pattern on value.
	 */
	private static boolean isWing(int value, Cell uvwxyz, Cell uz, Cell vz
			, Cell wz, Cell xz, Cell yz) {
		final int sv = VSHFT[value]; //shiftedValue
		return !((uvwxyz.maybes.bits & sv)!=0 && yz.notSees[uvwxyz.i])
			&& !((uz.maybes.bits & sv)!=0 && yz.notSees[uz.i])
			&& !((vz.maybes.bits & sv)!=0 && yz.notSees[vz.i])
			&& !((wz.maybes.bits & sv)!=0 && yz.notSees[wz.i])
			&& !((xz.maybes.bits & sv)!=0 && yz.notSees[xz.i]);
	}

	public UVWXYZWing() {
		super(Tech.UVWXYZ_Wing);
	}

	/**
	 * Finds first/all UVWXYZ-Wing hint/s in the given Grid, depending on which
	 * IAccumulator you pass. Hints are added to the IAccumulator, and if add
	 * returns true then the search exits-early.
	 * <ul>
	 * <li>UVWXYZ-Wing</li>
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
		int cands2, cands3, cands4, wingCands;
		// is this an UVWXYZ-Wing on xValue, zValue, both.
		boolean isXWing, isZWing;
		// the yz cell must have 2 maybes
		// a constant: contents do not change.
		final BitIdx bivalueCells = grid.getBitIdxBivalue();
		// candidates are cells with maybes.size 2..6
		// a constant: contents do not change.
		final BitIdx candidates = grid.getBitIdx((c) -> {
			return c.maybes.size>1 && c.maybes.size<7;
		});
		// a single BitIdx instance for all victims in a "run"
		final BitIdx victims = new BitIdx(grid);
		// get cells in grid which maybe each value 1..9
		final BitIdx[] vs = grid.getBitIdxs();
		// indices of possible u/v/w/x/yz cells
		// nb: a BitIdx needs the grid to iterate its cells in KRC-land, where
		// in Jullerat-land Grid.cellCell is a static, which is s__t IMHO, coz
		// all Cell methods need you to pass them the grid.
		// My model is stateful, Jullerats stateless with state passed in.
		BitIdx uzs = new BitIdx(grid)
			 , vzs = new BitIdx(grid)
			 , wzs = new BitIdx(grid)
			 , xzs = new BitIdx(grid)
			 , yzs = new BitIdx(grid);
		// the XZ class holds the x and z values, as in ALS-XZ;
		// and the both (ie isDoubleLinked) flag.
		final XZ yzVs = new XZ();
		// the removable (red) Cell=>Values
		final Pots theReds = new Pots();
		// we presume that no hint will be found
		boolean result = false;
		// first we build-up the "wing" cells
		// foreach cell in the grid with 2..6 potential values
		for ( Cell uvwxyz : candidates ) // C1
			for ( Cell uz : uzs.setAnd(candidates, uvwxyz.forwards()) ) // C2
				if ( VSIZE[cands2=uvwxyz.maybes.bits|uz.maybes.bits]<7
				  && vzs.setAndMin(uzs, uz.forwards(), 3) )
					for ( Cell vz : vzs ) // C3
						if ( VSIZE[cands3=cands2|vz.maybes.bits] < 7
						  && wzs.setAndMin(vzs, vz.forwards(), 2) )
							for ( Cell wz : wzs ) // C4
								if ( VSIZE[cands4=cands3|wz.maybes.bits] < 7
								  && xzs.setAndAny(wzs, wz.forwards()) )
									for ( Cell xz : xzs ) // C5
		//<OUTDENT comment="no wrapping">
		if ( VSIZE[wingCands=cands4|xz.maybes.bits] == 6 ) {
			// 5 cells with 6 values between them is an ALS
			// yz's are bivalue cells visible by one or more of the cells in
			// the ALS we just found, with BOTH there maybes in the ALS.
			yzs.set(uvwxyz.visible());
			yzs.addAll(uz.visible());
			yzs.addAll(vz.visible());
			yzs.addAll(wz.visible());
			yzs.addAll(xz.visible());
			yzs.remove(uvwxyz);
			yzs.remove(uz);
			yzs.remove(vz);
			yzs.remove(wz);
			yzs.remove(xz);
			if ( yzs.retainAllAny(bivalueCells) )
			for ( Cell yz : yzs ) {
				if ( VSIZE[yz.maybes.bits & wingCands] == 2 ) {
					// get the "z" value and the "x" value in ALS-XZ
					yzVs.set(yz);
					// is there a Wing?
					isXWing = isWing(yzVs.x, uvwxyz, uz, vz, wz, xz, yz);
					isZWing = isWing(yzVs.z, uvwxyz, uz, vz, wz, xz, yz);
					if ( isZWing || isXWing ) {
						// found a UVWXYZ-Wing pattern
						if ( !isXWing ) // single linked on z
							yzVs.swap();
						// double linked
						yzVs.both = isZWing & isXWing;
						// the 5 wing cells
						final Cell[] wing = {uvwxyz, uz, vz, wz, xz};
						// all 6 cells = wing + yz
						final Cell[] all = {uvwxyz, uz, vz, wz, xz, yz};
						// find eliminations
						Pots reds = eliminate(vs, victims, wingCands, yz, yzVs
								, wing, all, theReds);
						if ( reds != null ) {
							// create the hint
							hint = new UVWXYZWingHint(this, reds, yz, yzVs
									, wingCands, wing, all);
							result = true; // meaning hint/s were found
							if ( accu.add(hint) )
								return true; // exit early: A hint was found
						}
					}
				}
			}
		}
		//</OUTDENT>
		return result; // were any hint/s found?
	}

}
