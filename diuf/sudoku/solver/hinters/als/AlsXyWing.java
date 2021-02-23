/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This is a localised-copy of the HoDoKu code which implements ALS-XY-Wing.
 * Everything from AlsSolver to SudokuStepFinder, et al, are all sucked into
 * this class. Kudos to hobiwan. Mistakes are mine. ~KRC.
 *
 * Here's hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.Arrays;


/**
 * HdkAlsXyWing implements HoDoKus Almost-Locked-Set XY-Wing Sudoku solving
 * technique. The x and y are values common to three ALSs. The x's are
 * Restricted Common Candidates and the z/s are just common candidates (not
 * restricted).
 * <p>
 * This is a localised-copy of the HoDoKu code which implements ALS-XY-Wing.
 * Everything from HdkGrid, AlsSolver, SudokuStepFinder, etc; all fields are
 * all drawn into this one class. All HdkAlsXyWing types are copies of HoDoKu
 * classes, with minimal changes required to bring just the ALS-XY-Wing
 * functionality of HoDoKu into DIUF. "HoDoKuAdapter" methods (search term)
 * have been added to my Grid, Idx, HdkSet in order to translate from my model
 * into hobiwans (and vice versa "DiufAdapter"), and I raise a
 *     {@code diuf.sudoku.solver.hinters.als.DiufAlsXyWingHint}.
 * Externally I'm all DIUF, but internally I'm all HoDoKu. The kudos goes to
 * hobiwan. The mistakes are all mine. KRC.
 *
 * @author Keith Corlett 2020 May 5
 */
public final class AlsXyWing extends AAlsHinter
{

	public AlsXyWing() {
		// see param explanations in the AlsXyChain constructor.
		// Tech, allowLockedSets, findRCCs, allowOverlaps, forwardOnly, useStartAndEnd
		super(Tech.ALS_Wing, true, true, true, true, false);
	}

	// findHints was boosted mainly from HoDoKu AlsSolver.getAlsXYWingInt
	@Override
	protected boolean findHints(Grid grid, Idx[] candidates
			, Rcc[] rccs, Als[] alss, IAccumulator accu) {
		Rcc rccA, rccB;
		Als a, b, c;
		int i,I, j, zMaybes, vA1,vA2, vB1,vB2, a1,a2, b1,b2;
		boolean any;
		final Idx tmp = this.tmp; // for various checks
		final Idx zBuds = this.zBuds; // all buds of v (including the ALSs).
		final Pots redPots = this.redPots;
		final Pots bluePots = this.bluePots;
		final int n = rccs.length;

		// presume that no hint will be found
		boolean result = false;

		// foreach distinct pair of RCCs
		for ( i=0,I=n-1; i<I; ++i ) {
			rccA = rccs[i]; // rccs is an ArrayList whose get is O(1)
			vA1 = rccA.getCand1();
			vA2 = rccA.getCand2();
			for ( j=i+1; j<n; ++j ) {

				rccB = rccs[j];
				vB1 = rccB.getCand1();
				vB2 = rccB.getCand2();

				// need at least two different candidates in rcc1 and rcc2,
				// which can't be true if they have same single value.
				if ( vA1!=vB1 || vA2!=0 || vB2!=0 ) {

					// get the indexes of my ALSs in alss
					a1=rccA.getAls1(); a2=rccA.getAls2();
					b1=rccB.getAls1(); b2=rccB.getAls2();

					// the two RCCs have to connect 3 different ALSs; since
					//     rcc1.als1!=rcc1.als2 && rcc2.als1!=rcc2.als2
					// not too many possibilites remain, thankfully
					if ( (a1==b1 && a2!=b2)
					  || (a1==b2 && a2!=b1)
					  || (a2==b1 && a1!=b2)
					  || (a2==b2 && a1!=b1) ) {

						// identify c so we can check for eliminations
						a = b = c = null;
						if (a1 == b1) {
							a = alss[a2]; // alss is an ArrayList with O(1) get
							b = alss[b2];
							c = alss[a1];
						}
						if (a1 == b2) {
							a = alss[a2];
							b = alss[b1];
							c = alss[a1];
						}
						if (a2 == b1) {
							a = alss[a1];
							b = alss[b2];
							c = alss[a2];
						}
						if (a2 == b2) {
							a = alss[a1];
							b = alss[b1];
							c = alss[a2];
						}
						assert a!=null && b!=null && c!=null; // no warnings

						// check there are common buds to eliminate from
						if ( !Idx.andEmpty(a.buddies, b.buddies)
						  // get maybes common to these two ALSs except RCs;
						  // there must be at least one of them.
						  && (zMaybes=a.maybes & b.maybes & ~VSHFT[vA1]
							  & ~VSHFT[vA2] & ~VSHFT[vB1] & ~VSHFT[vB2]) != 0
						  // check overlaps: RCs already done, so just a and b
						  && (allowOverlaps || tmp.setAnd(a.idx, b.idx).none())
						  // one can't contain other, even if allowOverlaps
						  && !Idx.orEqualsEither(a.idx, b.idx)
						) {
							// examine each z value
							any = false;
							for ( int z : VALUESES[zMaybes] ) {
								// get cells that see all occurrences of z in both ALSs
								if ( zBuds.setAnd(a.vBuds[z], b.vBuds[z]).any() ) {
									// z can be eliminated
									zBuds.forEach(grid.cells
										, (x) -> redPots.upsert(x, z)
									);
									// add the z's themselves as fins (for display only)
									tmp.setOr(a.vs[z], b.vs[z]).forEach(grid.cells
										, (x) -> bluePots.upsert(x, z)
									);
									any = true;
								}
							}
							if ( any ) {

								// validate the bastard
								if ( HintValidator.ALS_USES ) {
									// Reject elimination of the solution value.
									if ( !HintValidator.isValid(grid, redPots) ) {
										HintValidator.report(this.getClass().getSimpleName()
												, grid, Arrays.asList(a, b, c));
//											// clear the pots, else they all go bad from here on.
//											redPots.clear();
//											bluePots.clear();
//											// for now just skip hint with invalid elimination/s
//											continue;
										// I think I'm on top of invalid hints so I die-early
										StdErr.exit(StdErr.me()+": invalid hint", new Throwable());
									}
								}

								//
								// ALS-XY-Wing found! build the hint
								//

								int zBits = redPots.valuesOf(); // z values as a bitset
								AHint hint = new AlsXyWingHint(
									  this
									, new Pots(redPots)
									, orangePots(grid, a.idx, b.idx, c.idx)
										.removeAll(bluePots).removeAll(redPots)
									// nb: blue overwrites everything incl red; some reds are
									// in other ALSs, so remove reds from blues so they're red
									// nb: and we want only values that're removed to be blue
									, new Pots(bluePots.removeAll(redPots).retainAll(zBits))
									, Regions.list(a.region, c.region)
									, Regions.list(b.region)
									, a, b, c
									, vA1 // x value
									, vB1 // y value
									, Values.toString(zBits, ", ", " and ") // z value/s
								);
								// clear the pots for the next hint (if any)
								redPots.clear();
								bluePots.clear();
								// we found a hint (for in the GUI)
								result = true;
								// add the hint to accu, and exit early in batch mode.
								if ( accu.add(hint) )
									return true;
							}
						}
					}
				}
			}
		}
		// GUI mode: sort hints by score (elimination count) descending
		if ( result )
			accu.sort();
		return result;
	}
	// used for various checks
	private final Idx tmp = new Idx();
	// all buds of v (including the ALSs).
	private final Idx zBuds = new Idx();
	// a couple of lazy Pot heads.
	private final Pots redPots = new Pots();
	private final Pots bluePots = new Pots();

}
