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
 * Aligned10ExclusionBase implements methods common to both
 * Aligned10Exclusion_1C and Aligned10Exclusion_2H
 * @author Keith Corlett 2020 Feb
 */
abstract class Aligned10ExclusionBase extends AAlignedSetExclusionBase {

	public Aligned10ExclusionBase(int firstHintNumber, IInterruptMonitor monitor) {
		super(Tech.AlignedDec, firstHintNumber, monitor);
		assert tech.isAligned;
		assert degree == 10;
	}

	protected static ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcls
			, int numCmnExcls, Cell[] cells, Pots redPots) {
		final int[] SHFT = Values.SHFT;
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);

		// unpack the cells from there array
		Cell c0=cells[0], c1=cells[1], c2=cells[2], c3=cells[3], c4=cells[4]
		   , c5=cells[5], c6=cells[6], c7=cells[7], c8=cells[8], c9=cells[9];

		int combo, i;
		int sv0, sv01, sv02, sv03, sv04, sv05, sv06, sv07, sv08;

		// Now we do that dog____ing loop again except this time we're
		// building the excluded combos map, which we need for the hint.
		// This takes "some time", but this loop is executed < ?1000? times
		// for top1465, not millions of times like DOG_1.
		DOG_2: for ( int v0 : c0.maybes ) { // anything is fast enough for a small enough n.
			sv0 = SHFT[v0];
			for ( int v1 : c1.maybes ) {
				if ( v1==v0 && !c0.notSees[c1.i] ) {
					// nb: These 0's show in the hint, which I actually prefer
					// coz it's succinct, so you can see why it was excluded.
					map.put(new HashA(v0,v1,0,0,0,0,0,0,0,0), null);
					continue;
				}
				sv01 = sv0 | SHFT[v1];
				for ( int v2 : c2.maybes ) {
					if ( v2==v0 && !c2.notSees[c0.i] ) {
						map.put(new HashA(v0,0,v2,0,0,0,0,0,0,0), null);
						continue;
					} else if ( v2==v1 && !c2.notSees[c1.i] ) {
						map.put(new HashA(0,v1,v2,0,0,0,0,0,0,0), null);
						continue;
					}
					sv02 = sv01 | SHFT[v2];
					for ( int v3 : c3.maybes ) {
						if ( v3==v0 && !c3.notSees[c0.i] ) {
							map.put(new HashA(v0,0,0,v3,0,0,0,0,0,0), null);
							continue;
						} else if ( v3==v1 && !c2.notSees[c1.i] ) {
							map.put(new HashA(0,v1,0,v3,0,0,0,0,0,0), null);
							continue;
						} else if ( v3==v2 && !c2.notSees[c2.i] ) {
							map.put(new HashA(0,0,v2,v3,0,0,0,0,0,0), null);
							continue;
						}
						sv03 = sv02 | SHFT[v3];
						for ( int v4 : c4.maybes ) {
							if ( v4==v0 && !c4.notSees[c0.i] ) {
								map.put(new HashA(v0,0,0,0,v4,0,0,0,0,0), null);
								continue;
							} else if ( v4==v1 && !c4.notSees[c1.i] ) {
								map.put(new HashA(0,v1,0,0,v4,0,0,0,0,0), null);
								continue;
							} else if ( v4==v2 && !c4.notSees[c2.i] ) {
								map.put(new HashA(0,0,v2,0,v4,0,0,0,0,0), null);
								continue;
							} else if ( v4==v3 && !c4.notSees[c3.i] ) {
								map.put(new HashA(0,0,0,v3,v4,0,0,0,0,0), null);
								continue;
							}
							sv04 = sv03 | SHFT[v4];
							for ( int v5 : c5.maybes ) {
								if ( v5==v0 && !c5.notSees[c0.i] ) {
									map.put(new HashA(v0,0,0,0,0,v5,0,0,0,0), null);
									continue;
								} else if ( v5==v1 && !c5.notSees[c1.i] ) {
									map.put(new HashA(0,v1,0,0,0,v5,0,0,0,0), null);
									continue;
								} else if ( v5==v2 && !c5.notSees[c2.i] ) {
									map.put(new HashA(0,0,v2,0,0,v5,0,0,0,0), null);
									continue;
								} else if ( v5==v3 && !c5.notSees[c3.i] ) {
									map.put(new HashA(0,0,0,v3,0,v5,0,0,0,0), null);
									continue;
								} else if ( v5==v4 && !c5.notSees[c4.i] ) {
									map.put(new HashA(0,0,0,0,v4,v5,0,0,0,0), null);
									continue;
								}
								sv05 = sv04 | SHFT[v5];
								for ( int v6 : c6.maybes ) {
									if ( v6==v0 && !c6.notSees[c0.i] ) {
										map.put(new HashA(v0,0,0,0,0,0,v6,0,0,0), null);
										continue;
									} else if ( v6==v1 && !c6.notSees[c1.i] ) {
										map.put(new HashA(0,v1,0,0,0,0,v6,0,0,0), null);
										continue;
									} else if ( v6==v2 && !c6.notSees[c2.i] ) {
										map.put(new HashA(0,0,v2,0,0,0,v6,0,0,0), null);
										continue;
									} else if ( v6==v3 && !c6.notSees[c3.i] ) {
										map.put(new HashA(0,0,0,v3,0,0,v6,0,0,0), null);
										continue;
									} else if ( v6==v4 && !c6.notSees[c4.i] ) {
										map.put(new HashA(0,0,0,0,v4,0,v6,0,0,0), null);
										continue;
									} else if ( v6==v5 && !c6.notSees[c5.i] ) {
										map.put(new HashA(0,0,0,0,0,v5,v6,0,0,0), null);
										continue;
									}
									sv06 = sv05 | SHFT[v6];
									for ( int v7 : c7.maybes ) {
										if ( v7==v0 && !c7.notSees[c0.i] ) {
											map.put(new HashA(v0,0,0,0,0,0,0,v7,0,0), null);
											continue;
										} else if ( v7==v1 && !c7.notSees[c1.i] ) {
											map.put(new HashA(0,v1,0,0,0,0,0,v7,0,0), null);
											continue;
										} else if ( v7==v2 && !c7.notSees[c2.i] ) {
											map.put(new HashA(0,0,v2,0,0,0,0,v7,0,0), null);
											continue;
										} else if ( v7==v3 && !c7.notSees[c3.i] ) {
											map.put(new HashA(0,0,0,v3,0,0,0,v7,0,0), null);
											continue;
										} else if ( v7==v4 && !c7.notSees[c4.i] ) {
											map.put(new HashA(0,0,0,0,v4,0,0,v7,0,0), null);
											continue;
										} else if ( v7==v5 && !c7.notSees[c5.i] ) {
											map.put(new HashA(0,0,0,0,0,v5,0,v7,0,0), null);
											continue;
										} else if ( v7==v6 && !c7.notSees[c6.i] ) {
											map.put(new HashA(0,0,0,0,0,0,v6,v7,0,0), null);
											continue;
										}
										sv07 = sv06 | SHFT[v7];
										for ( int v8 : c8.maybes ) {
											if ( v8==v0 && !c8.notSees[c0.i] ) {
												map.put(new HashA(v0,0,0,0,0,0,0,0,v8,0), null);
												continue;
											} else if ( v8==v1 && !c8.notSees[c1.i] ) {
												map.put(new HashA(0,v1,0,0,0,0,0,0,v8,0), null);
												continue;
											} else if ( v8==v2 && !c8.notSees[c2.i] ) {
												map.put(new HashA(0,0,v2,0,0,0,0,0,v8,0), null);
												continue;
											} else if ( v8==v3 && !c8.notSees[c3.i] ) {
												map.put(new HashA(0,0,0,v3,0,0,0,0,v8,0), null);
												continue;
											} else if ( v8==v4 && !c8.notSees[c4.i] ) {
												map.put(new HashA(0,0,0,0,v4,0,0,0,v8,0), null);
												continue;
											} else if ( v8==v5 && !c8.notSees[c5.i] ) {
												map.put(new HashA(0,0,0,0,0,v5,0,0,v8,0), null);
												continue;
											} else if ( v8==v6 && !c8.notSees[c6.i] ) {
												map.put(new HashA(0,0,0,0,0,0,v6,0,v8,0), null);
												continue;
											} else if ( v8==v7 && !c8.notSees[c7.i] ) {
												map.put(new HashA(0,0,0,0,0,0,0,v7,v8,0), null);
												continue;
											}
											sv08 = sv07 | SHFT[v8];
											for ( int v9 : c9.maybes ) {
												if ( v9==v0 && !c9.notSees[c0.i] ) {
													map.put(new HashA(v0,0,0,0,0,0,0,0,0,v9), null);
													continue;
												} else if ( v9==v1 && !c9.notSees[c1.i] ) {
													map.put(new HashA(0,v1,0,0,0,0,0,0,0,v9), null);
													continue;
												} else if ( v9==v2 && !c9.notSees[c2.i] ) {
													map.put(new HashA(0,0,v2,0,0,0,0,0,0,v9), null);
													continue;
												} else if ( v9==v3 && !c9.notSees[c3.i] ) {
													map.put(new HashA(0,0,0,v3,0,0,0,0,0,v9), null);
													continue;
												} else if ( v9==v4 && !c9.notSees[c4.i] ) {
													map.put(new HashA(0,0,0,0,v4,0,0,0,0,v9), null);
													continue;
												} else if ( v9==v5 && !c9.notSees[c5.i] ) {
													map.put(new HashA(0,0,0,0,0,v5,0,0,0,v9), null);
													continue;
												} else if ( v9==v6 && !c9.notSees[c6.i] ) {
													map.put(new HashA(0,0,0,0,0,0,v6,0,0,v9), null);
													continue;
												} else if ( v9==v7 && !c9.notSees[c7.i] ) {
													map.put(new HashA(0,0,0,0,0,0,0,v7,0,v9), null);
													continue;
												} else if ( v9==v8 && !c9.notSees[c8.i] ) {
													map.put(new HashA(0,0,0,0,0,0,0,0,v8,v9), null);
													continue;
												}
												// (2) common excluder rule
												combo = sv08 | SHFT[v9];
												for ( i=0; i<numCmnExcls; ++i )
													if ( (cmnExcls[i].maybes.bits & ~combo) == 0 ) {
														map.put(new HashA(v0,v1,v2,v3,v4,v5,v6,v7,v8,v9)
																, cmnExcls[i]);
														break; // we want only the first common excluder of each combo
													}
												// value is allowed, but we've already done that part.
											} // next v9
										} // next v8
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

}
