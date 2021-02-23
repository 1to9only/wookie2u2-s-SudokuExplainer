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
 * Implementation of the "WXYZ-Wing" solving technique.
 * Similar to ALS-XZ with smaller ALS being a bivalue cell
 * can catch the double linked version which is similar to Sue-De-Coq
 * Larger ALS has 3 cells in which cell candidates any size between 2-4
 */
public class WXYZWing extends ABigWingHinter {

	private static boolean isWing(int v, Cell wxyz, Cell wz, Cell xz, Cell yz) {
		int sv = VSHFT[v];
		return !((wxyz.maybes.bits & sv)!=0 && yz.notSees[wxyz.i])
			&& !((wz.maybes.bits & sv)!=0 && yz.notSees[wz.i])
			&& !((xz.maybes.bits & sv)!=0 && yz.notSees[xz.i]);
	}

	public WXYZWing() {
		super(Tech.WXYZ_Wing);
	}

	/**
	 * Find first/all WXYZ-Wing hints in the given Grid and add them to accu.
	 * First or all depends on which IAccumulator is passed: if accu.add
	 * returns true then I exit-early, returning just the first hint found.
	 * <ul>
	 * <li>WXYZ-Wing</li>
	 * <li>ALS-xz with a bivalue cell</li>
	 * <li>By SudokuMonster 2019</li>
	 * </ul>
	 * @return were hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		AHint hint;
		int cands2, wingCands; // candidates of n cells combined
		// is xValue a wing? how about the zValue? both?
		boolean xWing, zWing;
		// the yz cell must have 2 maybes
		// a constant: contents do not change.
		final BitIdx bivalueCells = grid.getBitIdxBivalue();
		// candidates are cells with maybes.size 2..4
		// a constant: contents do not change.
		final BitIdx candidates = grid.getBitIdx((c) -> {
			return c.maybes.size>1 && c.maybes.size<5;
		});
		// the cell-sets from which we select wz, xz, and yz
		// fixed instances, but with mutated contents.
		final BitIdx wzs = new BitIdx(grid)
				   , xzs = new BitIdx(grid)
				   , yzs = new BitIdx(grid);
		// the XZ class holds the x and z values, as in ALS-XZ;
		// and the both (ie isDoubleLinked) flag.
		final XZ yzVs = new XZ();
		// get cells in grid which maybe each value 1..9
		final BitIdx[] vs = grid.getBitIdxs();
		// a single BitIdx instance for all victims in a "run"
		final BitIdx victims = new BitIdx(grid);
		// the removable (red) Cell=>Values
		final Pots theReds = new Pots();
		// presume that no hint will be found
		boolean result = false;
		// first we build-up the "wing" cells
		// foreach candidate cell
		for ( Cell wxyz : candidates )
			// foreach candidate forward of wxyz
			for ( Cell wz : wzs.setAnd(candidates, wxyz.forwards()) )
				// if both cells combined have 2..4 maybes
				if ( VSIZE[cands2=wxyz.maybes.bits|wz.maybes.bits] < 5 )
					// foreach candidate forward of wz
					for ( Cell xz : xzs.setAnd(wzs, wz.forwards()) )
		// <OUTDENT comment="no wrapping">
		if ( VSIZE[wingCands=cands2|xz.maybes.bits] == 4 ) {
			// 3 cells with 4 maybes between them is an ALS (almost locked set)
			// These 3 cells are also called "the wing cells".
			// potential yz's are bivalue cells visible by a wing cell/s
			yzs.set(wxyz.visible());
			yzs.addAll(wz.visible());
			yzs.addAll(xz.visible());
			yzs.remove(wxyz);
			yzs.remove(wz);
			yzs.remove(xz);
			if ( yzs.retainAllAny(bivalueCells) )
			for ( Cell yz : yzs ) {
				if ( VSIZE[yz.maybes.bits & wingCands] == 2 ) {
					// get the "z" value and the "x" value in ALS-XZ
					// WTF: There are two naming conventions: Wing and ALS-XZ.
					// Here we use ALS-XZ nomenclature in a bloody wing. sigh.
					yzVs.set(yz);
					xWing = isWing(yzVs.x, wxyz, wz, xz, yz);
					zWing = isWing(yzVs.z, wxyz, wz, xz, yz);
					if ( xWing || zWing ) {
						// found a WXYZ-Wing pattern
						if ( !xWing ) // single linked on z
							yzVs.swap();
						// double linked
						yzVs.both = xWing & zWing;
						// the 3 wing cells
						final Cell[] wing = {wxyz, wz, xz};
						// all 4 cells = wing + yz
						final Cell[] all = {wxyz, wz, xz, yz};
						// find eliminations (shared static impl in super)
						Pots reds = eliminate(vs, victims, wingCands, yz, yzVs
								, wing, all, theReds);
						if ( reds != null ) {
							// create the hint and add it to accu
							hint = new WXYZWingHint(this, reds, wingCands, yzVs
									, yz, wing, all);
							result = true;
							if ( accu.add(hint) )
								return true; // exit early
						}
					}
				}
			}
		}
		//</OUTDENT>
		return result;
	}

}
