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
 * Implementation of the "TUVWXYZ-Wing" solving technique.
 * Similar to ALS-XZ with smaller ALS being a bivalue cell
 * can catch the double linked version which is similar to Sue-De-Coq
 * Larger  ALS has 5 cells in which cell candidates any size between 2-6
 */
public class TUVWXYZWing extends ABigWingHinter {

	private static boolean isWing(int value, Cell yz, Cell xz, Cell wz
			, Cell vz, Cell uz, Cell tz, Cell tuvwxyz) {
		final int sv = VSHFT[value];
		return !((xz.maybes.bits & sv)!=0 && yz.notSees[xz.i])
			&& !((wz.maybes.bits & sv)!=0 && yz.notSees[wz.i])
			&& !((vz.maybes.bits & sv)!=0 && yz.notSees[vz.i])
			&& !((uz.maybes.bits & sv)!=0 && yz.notSees[uz.i])
			&& !((tz.maybes.bits & sv)!=0 && yz.notSees[tz.i])
			&& !((tuvwxyz.maybes.bits & sv)!=0 && yz.notSees[tuvwxyz.i]);
	}

	public TUVWXYZWing() {
		super(Tech.TUVWXYZ_Wing);
	}

	/**
	 * Finds first/all TUVWXYZ-Wing hints in the given Grid, and adds it/them
	 * to the given IAccumulator. If accu.add returns true then I exit-early,
	 * abandoning the search for additional hints.
	 * <ul>
	 * <li>TUVWXYZ-Wing</li>
	 * <li>ALS-xz with a bivalue cell</li>
	 * <li>By SudokuMonster 2019</li>
	 * </ul>
	 * @return where hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// NB: the static XZ class holds the x and z values, as in ALS-XZ.
		// the yz cell must have 2 maybes (ie be bivalue)
		// a constant: contents do not change.
		final BitIdx bivalueCells = grid.getBitIdx((c) -> {
			return c.maybes.size == 2;
		});
		// candidates are cells with maybes.size 2..7 which see a bivalueCell.
		// a constant: contents do not change.
		final BitIdx candidates = grid.getBitIdx((c) -> {
			return c.maybes.size>1 && c.maybes.size<8;
		});
		// the cell-sets from which we select wz, xz, and yz
		// fixed instances, but with mutated contents.
		final BitIdx tzs = new BitIdx(grid)
				   , uzs = new BitIdx(grid)
				   , vzs = new BitIdx(grid)
				   , wzs = new BitIdx(grid)
				   , xzs = new BitIdx(grid)
				   , yzs = new BitIdx(grid);
		// the XZ class holds the x and z values, as in ALS-XZ;
		// and the both (ie isDoubleLinked) flag.
		final XZ yzVs = new XZ();
		// get an array of BitIdx's containing indices of each cell in the grid
		// which maybe each potential value 1..9.
		final BitIdx[] vs = grid.getBitIdxs();
		// a single BitIdx instance for all victims in a "run"
		final BitIdx victims = new BitIdx(grid);
		// the hint, if we ever find one
		AHint hint;
		// the combined potentials of n wing cells
		// wingCands is all 6 wing cells
		int cands2, cands3, cands4, cands5, wingCands;
		// is the xValue a wing? how about the zValue? how about both?
		boolean xWing, zWing;
		// the removable (red) potentials, if we ever find any
		final Pots theReds = new Pots();
		// presume that no hint will be found
		boolean result = false;
		// first we build-up the "wing" cells
		// foreach cell in the grid with 2..7 potential values
		for ( Cell tuvwxyz : candidates ) // C1
			for ( Cell tz : tzs.setAnd(candidates, tuvwxyz.forwards()) ) // C2
				if ( VSIZE[cands2=tuvwxyz.maybes.bits|tz.maybes.bits] < 8 )
					for ( Cell uz : uzs.setAnd(tzs, tz.forwards()) ) // C3
						if ( VSIZE[cands3=cands2|uz.maybes.bits] < 8 )
							for ( Cell vz : vzs.setAnd(uzs, uz.forwards()) ) // C4
								if ( VSIZE[cands4=cands3|vz.maybes.bits] < 8 )
									for ( Cell wz : wzs.setAnd(vzs, vz.forwards()) ) // C5
										if ( VSIZE[cands5=cands4|wz.maybes.bits] < 8 )
											for ( Cell xz : xzs.setAnd(wzs, wz.forwards()) ) // C6
		// </OUTDENT comment="no wrapping">
		// 6 cells with 7 values between them is an ALS (an Almost Locked Set).
		// These 6 cells are also called "the wing cells".
		// find "other" cells (yzs) which see 1+ of our six "wing" cells.
		if ( VSIZE[wingCands=cands5|xz.maybes.bits] == 7 ) {
			yzs.set(tuvwxyz.visible());
			yzs.addAll(tz.visible());
			yzs.addAll(uz.visible());
			yzs.addAll(vz.visible());
			yzs.addAll(wz.visible());
			yzs.addAll(xz.visible());
			yzs.remove(tuvwxyz);
			yzs.remove(tz);
			yzs.remove(uz);
			yzs.remove(vz);
			yzs.remove(wz);
			yzs.remove(xz);
			if ( yzs.retainAllAny(bivalueCells) )
			for ( Cell yz : yzs ) {
				// both of yz's maybes must be shared with the 6 "wing" cells
				if ( VSIZE[wingCands & yz.maybes.bits] == 2 ) {
					// get the "z" value and the "x" value in ALS-XZ
					yzVs.set(yz);
					// is there a wing on x and/or z?
					xWing = isWing(yzVs.x, yz, xz, wz, vz, uz, tz, tuvwxyz);
					zWing = isWing(yzVs.z, yz, xz, wz, vz, uz, tz, tuvwxyz);
					if ( xWing || zWing ) {
						// found a TUVWXYZ-Wing pattern
						if ( !xWing ) // single linked on z
							yzVs.swap();
						// double linked
						yzVs.both = xWing & zWing;
						// the 6 wing cells
						Cell[] wing = {tuvwxyz, tz, uz, vz, wz, xz};
						// all 7 cells = wing + yz
						Cell[] all = {tuvwxyz, tz, uz, vz, wz, xz, yz};
						// find eliminations
						Pots reds = eliminate(vs, victims, wingCands, yz, yzVs
								, wing, all, theReds);
						if ( reds != null ) {
							// create the hint
							hint = new TUVWXYZWingHint(this, reds, yz, yzVs
									, wingCands, wing, all);
							result = true;
							if ( accu.add(hint) )
								return true;
						}
					}
				}
			}
		}
		//</OUTDENT>
		return result;
	}

}
