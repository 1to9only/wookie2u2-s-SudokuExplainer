/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.gen.IInterruptMonitor;


/**
 * Aligned5ExclusionBase implements methods common to Aligned5Exclusion_1C and
 * Aligned5Exclusion_2H.
 * @author Keith Corlett 2020 Feb
 */
abstract class Aligned5ExclusionBase extends AAlignedSetExclusionBase {

	public Aligned5ExclusionBase(IInterruptMonitor monitor) {
		super(Tech.AlignedPent, monitor);
		assert tech.isAligned;
		assert degree == 5;
	}

	protected static ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcls
			, int numCmnExcls, Cell[] cells, Pots redPots) {
		final int[] SHFT = Values.VSHFT;
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);

		Cell c0=cells[0], c1=cells[1], c2=cells[2], c3=cells[3], c4=cells[4];

		int sv0, sv01, sv02, sv03, combo, i;

		// Now we do that dog____ing loop again except this time we're
		// building the excluded combos map, which we need for the hint.
		// This takes "some time", but this loop is executed < 100 times
		// for top1465, not millions of times like DOG_1.
		DOG_2: for ( int v0 : c0.maybes ) { // anything is fast enough for a small enough n.
			sv0 = SHFT[v0];
			for ( int v1 : c1.maybes ) {
				if ( v1==v0 && !c1.notSees[c0.i] ) {
					// nb: These 0's show in the hint, which I prefer
					// coz you can understand why it was excluded.
					map.put(new HashA(v0,v1,0,0,0), null);
					continue;
				}
				sv01 = sv0 | SHFT[v1];
				for ( int v2 : c2.maybes ) {
					if ( v2==v0 && !c2.notSees[c0.i] ) {
						map.put(new HashA(v0,0,v2,0,0), null);
						continue;
					} else if ( v2==v1 && !c2.notSees[c1.i] ) {
						map.put(new HashA(0,v1,v2,0,0), null);
						continue;
					}
					sv02 = sv01 | SHFT[v2];
					for ( int v3 : c3.maybes ) {
						if ( v3==v0 && !c3.notSees[c0.i] ) {
							map.put(new HashA(v0,0,0,v3,0), null);
							continue;
						} else if ( v3==v1 && !c3.notSees[c1.i] ) {
							map.put(new HashA(0,v1,0,v3,0), null);
							continue;
						} else if ( v3==v2 && !c3.notSees[c2.i] ) {
							map.put(new HashA(0,0,v2,v3,0), null);
							continue;
						}
						sv03 = sv02 | SHFT[v3];
						for ( int v4 : c4.maybes ) {
							if ( v4==v0 && !c4.notSees[c0.i] ) {
								map.put(new HashA(v0,0,0,0,v4), null);
								continue;
							} else if ( v4==v1 && !c4.notSees[c1.i] ) {
								map.put(new HashA(0,v1,0,0,v4), null);
								continue;
							} else if ( v4==v2 && !c4.notSees[c2.i] ) {
								map.put(new HashA(0,0,v2,0,v4), null);
								continue;
							} else if ( v4==v3 && !c4.notSees[c3.i] ) {
								map.put(new HashA(0,0,0,v3,v4), null);
								continue;
							}
							// (2) common excluder rule
							combo = sv03 | SHFT[v4];
							for ( i=0; i<numCmnExcls; ++i )
								if ( (cmnExcls[i].maybes.bits & ~combo) == 0 ) {
									map.put(new HashA(v0,v1,v2,v3,v4)
											, cmnExcls[i]);
									break; // we want only the first excluder of each combo
								}
							// value is allowed, but we've already done that part.
						} // next v4
					} // next v3
				} // next v2
			} // next v1
		} // next v0
		return map;
	}

}
