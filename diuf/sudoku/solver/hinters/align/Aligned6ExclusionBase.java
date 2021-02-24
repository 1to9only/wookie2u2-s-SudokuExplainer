/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.gen.IInterruptMonitor;


/**
 * Aligned6ExclusionBase holds methods common to both Aligned6Exclusion_1C and
 * Aligned6Exclusion_2H. There's enough of them now to make copy-paste coding
 * undesirable.
 * @author Keith Corlett 2020 Feb
 */
abstract class Aligned6ExclusionBase extends AAlignedSetExclusionBase {

	public Aligned6ExclusionBase(IInterruptMonitor monitor) {
		super(Tech.AlignedHex, monitor);
		assert tech.isAligned;
		assert degree == 6;
	}

	protected static ExcludedCombosMap buildExcludedCombosMap(Grid.Cell[] cmnExcls
			, int numCmnExcls, Grid.Cell[] cells, Pots redPots) {
		final int[] SHFT = Values.VSHFT;
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);

		final Grid.Cell c0=cells[0], c1=cells[1], c2=cells[2], c3=cells[3]
				 , c4=cells[4], c5=cells[5];

		int sv0, sv01, sv02, sv03, sv04, combo, i;

		// Now we do that dog____ing loop again except this time we're
		// building the excluded combos map, which we need for the hint.
		// This takes "some time", but this loop is executed < ?1000? times
		// for top1465, not millions of times like DOG_1.
		DOG_2: for ( int v0 : c0.maybes ) { // anything is fast enough for a small enough n, even my Values iterator.
			sv0 = SHFT[v0];
			for ( int v1 : c1.maybes ) {
				if ( v1==v0 && !c1.notSees[c0.i] ) {
					// nb: These 0's show in the hint, which I prefer
					// coz you can understand why it was excluded.
					map.put(new HashA(v0,v1,0,0,0,0), null);
					continue;
				}
				sv01 = sv0 | SHFT[v1];
				for ( int v2 : c2.maybes ) {
					if ( v2==v0 && !c2.notSees[c0.i] ) {
						map.put(new HashA(v0,0,v2,0,0,0), null);
						continue;
					} else if ( v2==v1 && !c2.notSees[c1.i] ) {
						map.put(new HashA(0,v1,v2,0,0,0), null);
						continue;
					}
					sv02 = sv01 | SHFT[v2];
					for ( int v3 : c3.maybes ) {
						if ( v3==v0 && !c3.notSees[c0.i] ) {
							map.put(new HashA(v0,0,0,v3,0,0), null);
							continue;
						} else if ( v3==v1 && !c3.notSees[c1.i] ) {
							map.put(new HashA(0,v1,0,v3,0,0), null);
							continue;
						} else if ( v3==v2 && !c3.notSees[c2.i] ) {
							map.put(new HashA(0,0,v2,v3,0,0), null);
							continue;
						}
						sv03 = sv02 | SHFT[v3];
						for ( int v4 : c4.maybes ) {
							if ( v4==v0 && !c4.notSees[c0.i] ) {
								map.put(new HashA(v0,0,0,0,v4,0), null);
								continue;
							} else if ( v4==v1 && !c4.notSees[c1.i] ) {
								map.put(new HashA(0,v1,0,0,v4,0), null);
								continue;
							} else if ( v4==v2 && !c4.notSees[c2.i] ) {
								map.put(new HashA(0,0,v2,0,v4,0), null);
								continue;
							} else if ( v4==v3 && !c4.notSees[c3.i] ) {
								map.put(new HashA(0,0,0,v3,v4,0), null);
								continue;
							}
							sv04 = sv03 | SHFT[v4];
							for ( int v5 : c5.maybes ) {
								if ( v5==v0 && !c5.notSees[c0.i] ) {
									map.put(new HashA(v0,0,0,0,0,v5), null);
									continue;
								} else if ( v5==v1 && !c5.notSees[c1.i] ) {
									map.put(new HashA(0,v1,0,0,0,v5), null);
									continue;
								} else if ( v5==v2 && !c5.notSees[c2.i] ) {
									map.put(new HashA(0,0,v2,0,0,v5), null);
									continue;
								} else if ( v5==v3 && !c5.notSees[c3.i] ) {
									map.put(new HashA(0,0,0,v3,0,v5), null);
									continue;
								} else if ( v5==v4 && !c5.notSees[c4.i] ) {
									map.put(new HashA(0,0,0,0,v4,v5), null);
									continue;
								}
								// (2) common excluder rule
								combo = sv04 | SHFT[v5];
								for ( i=0; i<numCmnExcls; ++i )
									if ( (cmnExcls[i].maybes.bits & ~combo) == 0 ) {
										map.put(new HashA(v0,v1,v2,v3,v4,v5)
												, cmnExcls[i]);
										break; // we want only the first excluder of each combo
									}
								// combo is allowed, but we have already populated the
								// allowed values bitsets (avbs) array, so do nothing.
							} // next v5
						} // next v4
					} // next v3
				} // next v2
			} // next v1
		} // next v0
		return map;
	}

}
