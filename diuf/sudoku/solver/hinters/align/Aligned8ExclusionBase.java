/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import java.io.File;


/**
 * Aligned8ExclusionBase implements methods common to both Aligned8Exclusion_1C
 * and Aligned8Exclusion_2H.
 * @author Keith Corlett 2020 Feb
 */
public abstract class Aligned8ExclusionBase extends AAlignedSetExclusionBase {

	public Aligned8ExclusionBase(File hitFile) {
		super(Tech.AlignedOct, hitFile);
		assert tech.isAligned;
		assert degree == 8;
	}

	protected static ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcls
			, int numCmnExcls, Cell[] cells, Pots redPots) {
		final int[] SHFT = Values.VSHFT;
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);

		// unpack the cells from there array
		Cell c0=cells[0], c1=cells[1], c2=cells[2], c3=cells[3]
				, c4=cells[4], c5=cells[5], c6=cells[6], c7=cells[7];

		int sv0,sv01,sv02,sv03,sv04,sv05,sv06, combo, i;

		// Now we do that dog____ing loop again except this time we're
		// building the excluded combos map, which we need for the hint.
		// This takes "some time", but this loop is executed < ?1000? times
		// for top1465, not millions of times like DOG_1.
		DOG_2: for ( int v0 : VALUESES[c0.maybes] ) {
			sv0 = SHFT[v0];
			for ( int v1 : VALUESES[c1.maybes] ) {
				if ( v1==v0 && !c1.notSees[c0.i] ) {
					// nb: These 0's show in the hint, which I prefer
					// coz you can understand why it was excluded.
					map.put(new HashA(v0,v1,0,0,0,0,0,0), null);
					continue;
				}
				sv01 = sv0 | SHFT[v1];
				for ( int v2 : VALUESES[c2.maybes] ) {
					if ( v2==v0 && !c2.notSees[c0.i] ) {
						map.put(new HashA(v0,0,v2,0,0,0,0,0), null);
						continue;
					} else if ( v2==v1 && !c2.notSees[c1.i] ) {
						map.put(new HashA(0,v1,v2,0,0,0,0,0), null);
						continue;
					}
					sv02 = sv01 | SHFT[v2];
					for ( int v3 : VALUESES[c3.maybes] ) {
						if ( v3==v0 && !c3.notSees[c0.i] ) {
							map.put(new HashA(v0,0,0,v3,0,0,0,0), null);
							continue;
						} else if ( v3==v1 && !c3.notSees[c1.i] ) {
							map.put(new HashA(0,v1,0,v3,0,0,0,0), null);
							continue;
						} else if ( v3==v2 && !c3.notSees[c2.i] ) {
							map.put(new HashA(0,0,v2,v3,0,0,0,0), null);
							continue;
						}
						sv03 = sv02 | SHFT[v3];
						for ( int v4 : VALUESES[c4.maybes] ) {
							if ( v4==v0 && !c4.notSees[c0.i] ) {
								map.put(new HashA(v0,0,0,0,v4,0,0,0), null);
								continue;
							} else if ( v4==v1 && !c4.notSees[c1.i] ) {
								map.put(new HashA(0,v1,0,0,v4,0,0,0), null);
								continue;
							} else if ( v4==v2 && !c4.notSees[c2.i] ) {
								map.put(new HashA(0,0,v2,0,v4,0,0,0), null);
								continue;
							} else if ( v4==v3 && !c4.notSees[c3.i] ) {
								map.put(new HashA(0,0,0,v3,v4,0,0,0), null);
								continue;
							}
							sv04 = sv03 | SHFT[v4];
							for ( int v5 : VALUESES[c5.maybes] ) {
								if ( v5==v0 && !c5.notSees[c0.i] ) {
									map.put(new HashA(v0,0,0,0,0,v5,0,0), null);
									continue;
								} else if ( v5==v1 && !c5.notSees[c1.i] ) {
									map.put(new HashA(0,v1,0,0,0,v5,0,0), null);
									continue;
								} else if ( v5==v2 && !c5.notSees[c2.i] ) {
									map.put(new HashA(0,0,v2,0,0,v5,0,0), null);
									continue;
								} else if ( v5==v3 && !c5.notSees[c3.i] ) {
									map.put(new HashA(0,0,0,v3,0,v5,0,0), null);
									continue;
								} else if ( v5==v4 && !c5.notSees[c4.i] ) {
									map.put(new HashA(0,0,0,0,v4,v5,0,0), null);
									continue;
								}
								sv05 = sv04 | SHFT[v5];
								for ( int v6 : VALUESES[c6.maybes] ) {
									if ( v6==v0 && !c6.notSees[c0.i] ) {
										map.put(new HashA(v0,0,0,0,0,0,v6,0), null);
										continue;
									} else if ( v6==v1 && !c6.notSees[c1.i] ) {
										map.put(new HashA(0,v1,0,0,0,0,v6,0), null);
										continue;
									} else if ( v6==v2 && !c6.notSees[c2.i] ) {
										map.put(new HashA(0,0,v2,0,0,0,v6,0), null);
										continue;
									} else if ( v6==v3 && !c6.notSees[c3.i] ) {
										map.put(new HashA(0,0,0,v3,0,0,v6,0), null);
										continue;
									} else if ( v6==v4 && !c6.notSees[c4.i] ) {
										map.put(new HashA(0,0,0,0,v4,0,v6,0), null);
										continue;
									} else if ( v6==v5 && !c6.notSees[c5.i] ) {
										map.put(new HashA(0,0,0,0,0,v5,v6,0), null);
										continue;
									}
									sv06 = sv05 | SHFT[v6];
									for ( int v7 : VALUESES[c7.maybes] ) {
										if ( v7==v0 && !c7.notSees[c0.i] ) {
											map.put(new HashA(v0,0,0,0,0,0,0,v7), null);
											continue;
										} else if ( v7==v1 && !c7.notSees[c1.i] ) {
											map.put(new HashA(0,v1,0,0,0,0,0,v7), null);
											continue;
										} else if ( v7==v2 && !c7.notSees[c2.i] ) {
											map.put(new HashA(0,0,v2,0,0,0,0,v7), null);
											continue;
										} else if ( v7==v3 && !c7.notSees[c3.i] ) {
											map.put(new HashA(0,0,0,v3,0,0,0,v7), null);
											continue;
										} else if ( v7==v4 && !c7.notSees[c4.i] ) {
											map.put(new HashA(0,0,0,0,v4,0,0,v7), null);
											continue;
										} else if ( v7==v5 && !c7.notSees[c5.i] ) {
											map.put(new HashA(0,0,0,0,0,v5,0,v7), null);
											continue;
										} else if ( v7==v6 && !c7.notSees[c6.i] ) {
											map.put(new HashA(0,0,0,0,0,0,v6,v7), null);
											continue;
										}
										// (2) common excluder rule
										combo = sv06 | SHFT[v7];
										for ( i=0; i<numCmnExcls; ++i )
											if ( (cmnExcls[i].maybes & ~combo) == 0 ) {
												map.put(new HashA(v0,v1,v2,v3,v4,v5,v6,v7)
														, cmnExcls[i]);
												break; // we want only the first excluder of each combo
											}
										// value is allowed, but we've already done that part.
									} // next v7
								} // next v6
							} // next v5
						} // next v4
					} // next v3
				} // next v2
			} // next v1
		} // next v0
		return map;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
