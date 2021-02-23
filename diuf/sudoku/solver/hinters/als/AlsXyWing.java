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

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
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
		// Tech, LogicalSolver, allowOverlaps, lockedSets, forwardOnly
		super(Tech.ALS_Wing, true, true, true, false);
	}

	// findHints was boosted mainly from HoDoKu AlsSolver.getAlsXYWingInt
	@Override
	protected boolean findHints(Grid grid, Idx[] candidates
			, Rcc[] rccs, Als[] alss, IAccumulator accu) {
		boolean result = false;
		final int n = rccs.length;
		// used for various checks
		final Idx tmp = new Idx();
		// all buds of v (including the ALSs).
		final Idx zBuds = new Idx();
		final Pots redPots=new Pots(), bluePots=new Pots();
		final Cell[] cells = grid.cells;
		Rcc rccA;
		Rcc rccB;
		Als a;
		Als b, c;
		int i,I, j, k,K, zMaybes;
		int rcA1,rcA2, rcB1,rcB2;
		int alsA1,alsA2, alsB1,alsB2;
		boolean any;
		// foreach distinct pair of RCCs
		for ( i=0,I=n-1; i<I; ++i ) {
			rccA = rccs[i]; // rccs is an ArrayList whose get is O(1)
			rcA1 = rccA.getCand1();
			rcA2 = rccA.getCand2();
			for ( j=i+1; j<n; ++j ) {

				rccB = rccs[j];
				rcB1 = rccB.getCand1();
				rcB2 = rccB.getCand2();

				// need at least two different candidates in rcc1 and rcc2,
				// which can't be true if they have same single value.
				if ( rcA1==rcB1 && rcA2==0 && rcB2==0 )
					continue; // both RCs have the same single candidate

				// get the indexes of my ALSs in alss
				alsA1=rccA.getAls1(); alsA2=rccA.getAls2();
				alsB1=rccB.getAls1(); alsB2=rccB.getAls2();

				// the two RCCs have to connect 3 different ALSs; since
				//     rcc1.als1!=rcc1.als2 && rcc2.als1!=rcc2.als2
				// not too many possibilites remain, thankfully
				if ( !( (alsA1==alsB1 && alsA2!=alsB2)
					 || (alsA1==alsB2 && alsA2!=alsB1)
					 || (alsA2==alsB1 && alsA1!=alsB2)
					 || (alsA2==alsB2 && alsA1!=alsB1) ) )
					continue; // cant be an XY-Wing

				// identify c so we can check for eliminations
				a = b = c = null;
				if (alsA1 == alsB1) {
					a = alss[alsA2]; // alss is an ArrayList with O(1) get
					b = alss[alsB2];
					c = alss[alsA1];
				}
				if (alsA1 == alsB2) {
					a = alss[alsA2];
					b = alss[alsB1];
					c = alss[alsA1];
				}
				if (alsA2 == alsB1) {
					a = alss[alsA1];
					b = alss[alsB2];
					c = alss[alsA2];
				}
				if (alsA2 == alsB2) {
					a = alss[alsA1];
					b = alss[alsB1];
					c = alss[alsA2];
				}
				assert a!=null && b!=null && c!=null; // supress Nutbeans warnings

				// check there are common buds to eliminate from
				if ( Idx.andEmpty(a.buddies, b.buddies) )
					continue; // no common buds means no eliminations

				// There has been some confusion, so I must insist that:
				assert rcA1!=-1 && rcA2!=-1 && rcB1!=-1 && rcB2!=-1
						: "RC's \"none\" value is 0, not -1, ya putz!";

				// get maybes common to these two ALSs, except the RC values,
				// and check that there is at least one of them.
				if ( (zMaybes=a.maybes & b.maybes & ~SHFT[rcA1] & ~SHFT[rcA2]
						 & ~SHFT[rcB1] & ~SHFT[rcB2]) == 0 )
					continue; // no common maybes to search

				// check overlaps: RCs already done, so just a and b
				if ( !allowOverlaps && tmp.setAnd(a.set, b.set).any() )
					continue; // overlap supressed

				// one can't be a subset of the other, even if ALLOW_OVERLAP
				if ( Idx.orEqualsEither(a.set, b.set) )
					continue;

				// examine each z value
				any = false;
				for ( int z : VALUESES[zMaybes] ) {
					// get cells that see all occurrences of z in both ALSs
					if ( zBuds.setAnd(a.vBuds[z], b.vBuds[z]).none() )
						continue;
					// z can be eliminated
					zBuds.forEach1((zBud)->redPots.upsert(cells[zBud], z));
					// add the z's themselves as fins (for display only)
					tmp.setOr(a.vs[z], b.vs[z]);
					tmp.forEach1((t)->bluePots.upsert(cells[t], z));
					any = true;
				}
				if ( !any )
					continue;

				if ( HintValidator.ALS_USES ) {
					// Reject elimination of the solution value.
					if ( !HintValidator.isValid(grid, redPots) ) {
						HintValidator.report(this.getClass().getSimpleName()
								, grid, Arrays.asList(a, b, c));
//						// clear the pots, else they all go bad from here on.
//						redPots.clear(); bluePots.clear();
//						// for now just skip hint with invalid elimination/s
//						continue;
						// I think I'm on top of invalid hints so I die-early
						StdErr.exit(StdErr.me()+": invalid hint", new Throwable());
					}
				}

				//
				// ALS-XY-Wing found! build the hint and add it to accu
				//

				int zBits = redPots.valuesOf(); // z values as a bitset
				AHint hint = new AlsXyWingHint(
					  this
					, new Pots(redPots)
					, orangePots(grid, a.set, b.set, c.set)
							.removeAll(bluePots).removeAll(redPots)
					// nb: blue overwrites everything incl red; some reds are
					// in other ALSs, so remove reds from blues so they're red
					// nb: and we want only values that're removed to be blue
					, new Pots(bluePots.removeAll(redPots).retainAll(zBits))
					, Regions.list(a.region, c.region)
					, Regions.list(b.region)
					, a, b, c
					, rcA1 // x value
					, rcB1 // y value
					, Values.toString(zBits, ", ", " and ") // z value/s
				);
				// clear the pots for the next hint (if any)
				redPots.clear();  bluePots.clear();
				// we found a hint (for in the GUI)
				result = true;
				// add the hint to accu, and exit early in batch mode.
				if ( accu.add(hint) )
					return true;
			}
		}
		// GUI mode: sort hints by score (elimination count) descending
		if ( result )
			accu.sort();
		return result;
	}

}
