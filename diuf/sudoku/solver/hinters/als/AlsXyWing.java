/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This is a localised-copy of the HoDoKu code which implements ALS-XY-Wing.
 * Everything from AlsSolver to SudokuStepFinder, et al, are all sucked into
 * this class and AAlsHinter. Kudos to hobiwan. Mistakes are mine. KRC.
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
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.Arrays;


/**
 * AlsXyWing implements the Almost Locked Set XY-Wing Sudoku solving technique.
 * The ALS-XY-Wing is formed by three Almost Locked Sets (ALSs). The values x
 * and y are common to a pair of ALSs. The x-values are Restricted Common
 * Candidates (RCCs). All occurrences of a restricted value in two ALSs see
 * each other. The y-values are just common (not restricted).
 * <p>
 * Each pair of ALSs share two common values, x and y (as above) such that a
 * value is "pushed" along the chain, eventually eliminating the z-value that
 * is common (non-restricted) to the first and last ALS (a and b in this
 * implementation; c is the middle ALS in the chain).
 * <p>
 *
 * @author Keith Corlett 2020 May 5
 */
public final class AlsXyWing extends AAlsHinter
//implements diuf.sudoku.solver.IReporter
{

	public AlsXyWing() {
		// see param explanations in the AlsXyChain constructor.
		// Tech, allowLockedSets, findRCCs, allowOverlaps, forwardOnly, useStartAndEnd
		super(Tech.ALS_Wing
				, true	// allowLockedSets: my Almost Locked Sets include cells
						// that are part of a Locked Set in the region.
				, true	// findRCCs: run getRccs to find values connecting ALSs
				, true	// allowOverlaps: include
				, true	// forwardOnly
				, false	// useStartAndEnd
		);
	}

	// findHints was boosted mainly from HoDoKu AlsSolver.getAlsXYWingInt
	@Override
	protected boolean findHints(Grid grid, Idx[] candidates, Rcc[] rccs
			, Als[] alss, IAccumulator accu) {
		Rcc rccA, rccB;
		Als a, b, c;
		int i, j, vA1,vA2, vB1,vB2, a1,a2, b1,b2, zs;
		boolean any;
//		final Idx tmp = this.tmp; // for various checks
//		final Idx victims = this.victims; // all buds of v (including the ALSs).
//		final Pots redPots = this.redPots;
//		final Pots bluePots = this.bluePots;
		final int n = rccs.length;
		final int m = n - 1;
		// presume that no hint will be found
		boolean result = false;
//		ttlRccs += n;
		// foreach distinct pair of RCCs
		for ( i=0; i<m; ++i ) {
			rccA = rccs[i]; // rccs is an ArrayList whose get is O(1)
			vA1 = rccA.cand1;
			vA2 = rccA.cand2;
			for ( j=i+1; j<n; ++j ) {
				rccB = rccs[j];
				vB1 = rccB.cand1;
				vB2 = rccB.cand2;
				// need at least two different common candidates in rcc1 & rcc2
				// which can't be true if they have same single value.
				if ( vA1!=vB1 || vA2!=0 || vB2!=0 ) {
					// get the indexes of my ALSs in the alss array
					a1=rccA.als1; a2=rccA.als2;
					b1=rccB.als1; b2=rccB.als2;
					// the two RCCs have to connect 3 different ALSs; since
					//     rcc1.als1!=rcc1.als2 && rcc2.als1!=rcc2.als2
					// not too many possibilites remain, thankfully
					if ( (a1==b1 && a2!=b2)
					  || (a1==b2 && a2!=b1)
					  || (a2==b1 && a1!=b2)
					  || (a2==b2 && a1!=b1) ) {
						// identify c so we can check for eliminations
						a = b = c = null;
						if ( a1 == b1 ) {
							a = alss[a2];
							b = alss[b2];
							c = alss[a1];
						}
						if ( a1 == b2 ) {
							a = alss[a2];
							b = alss[b1];
							c = alss[a1];
						}
						if ( a2 == b1 ) {
							a = alss[a1];
							b = alss[b2];
							c = alss[a2];
						}
						if ( a2 == b2 ) {
							a = alss[a1];
							b = alss[b1];
							c = alss[a2];
						}
						// supress IDE's possible null pointer warnings
						assert a!=null && b!=null && c!=null;
						// if any buds of both ALSs, to eliminate from
//						if ( a.buddies.andAny(b.buddies) // inlined for speed
						if ( ( ( (a.buddies.a0 & b.buddies.a0)
							   | (a.buddies.a1 & b.buddies.a1)
							   | (a.buddies.a2 & b.buddies.a2) ) != 0 )
						  // and there's atleast one z-value: maybes common to
						  // both ALSs, except the RC values.
						  && (zs=a.maybes & b.maybes & ~VSHFT[vA1] & ~VSHFT[vA2]
								& ~VSHFT[vB1] & ~VSHFT[vB2]) != 0
						  // check overlaps: RCs already done, so just a and b
						  // doing this inline was no faster so stop inlining
						  && (allowOverlaps || a.idx.andNone(b.idx))
						  // one cannot contain the other, even if allowOverlap
						  && !Idx.orEqualsEither(a.idx, b.idx)
						) {
							// presume that no eliminations will be found
							any = false;
							// examine each z value
							for ( int z : VALUESES[zs] ) {
								// victims: cells seeing all z's in both ALSs,
								// including the ALSs themselves.
								if ( victims.setAnd(a.vBuds[z], b.vBuds[z]).any() ) {
									// we found elimination/s
									any = true;
									// eliminate z from each victim
									victims.forEach(grid.cells
										, (cell) -> redPots.upsert(cell, z)
									);
									// ALS z's are fins (for display only)
									tmp.setOr(a.vs[z], b.vs[z]).forEach(grid.cells
										, (cell) -> bluePots.upsert(cell, z)
									);
								}
							}
							if ( any ) {
								// validate
								if ( HintValidator.ALS_USES ) {
									if ( !HintValidator.isValid(grid, redPots) ) {
										HintValidator.report(this.getClass().getSimpleName()
												, grid, Arrays.asList(a, b, c));
										if ( true ) {
											// I'm sort-of on top of invalid hints
											StdErr.exit(StdErr.me()+": invalid hint", new Throwable());
										} else {
											// just skip invalid elimination/s
											redPots.clear();
											bluePots.clear();
											continue;
										}
									}
								}
								// FOUND an ALS-XY-Wing!
								// get z values as a bitset
								final int zBits = redPots.valuesOf();
								// build the hint
								final AHint hint = new AlsXyWingHint(
									  this
									, new Pots(redPots)
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
								// add the hint to accu
								if ( accu.add(hint) )
									return result; // exit-early in batch
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
	// victims: all buds of v (including the ALSs).
	private final Idx victims = new Idx();
	// a couple of lazy Pot heads.
	private final Pots redPots = new Pots();
	private final Pots bluePots = new Pots();

//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("%s: ttlRccs=%,d\n", tech.name(), ttlRccs);
//	}
//	private long ttlRccs;
//ALS_XZ:    ttlRccs=    8,801,276
//ALS_Wing:  ttlRccs=    6,085,665
//ALS_Chain: ttlRccs=3,100,592,090

}
