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

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Log;
import java.util.Arrays;

/**
 * AlsWing implements the Almost Locked Set Wing Sudoku solving technique.
 * An ALS-Wing is an ALS-Chain of three ALSs.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * determine there RCCs (connections), and call my "custom" findHints method
 * passing everything into my implementation of a specific search technique.
 * <p>
 * An ALS-Wing is formed by three Almost Locked Sets (ALSs). The values x
 * and z are common to a pair of ALSs. The x-values are Restricted Common
 * Candidates (RCCs). All occurrences of a restricted value in two ALSs see
 * each other. The z-values are just common (not restricted).
 * <p>
 * Each pair of ALSs share two common values, x and z (as above) such that x
 * is "pushed" along the chain, eventually eliminating z-value's common to the
 * first and last ALS (a and b in this implementation; c is the "middle" ALS in
 * the chain).
 * <pre>
 * KRC 2021-05-31 I've been trying to make XYWing faster, because it's the
 * slowest hinter that I'm still using, but I've been having very little joy.
 * The fastest implementation so far ran in about 18-20 seconds for top1465.
 * This implementation runs in about 20-22 seconds, but is a bit more readable.
 * I'm willing to forego performance for readability.
 * </pre>
 *
 * @author Keith Corlett 2020 May 5
 */
public final class AlsWing extends AAlsHinter
//implements diuf.sudoku.solver.IReporter
{

	// buds of all v's (including the ALSs).
	private final Idx victims = new Idx();
	// removable (red) potential values
	private final Pots redPots = new Pots();

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.ALS_Wing
	 * * allowNakedSets = true my Almost Locked Sets include cells that are
	 *   part of a Locked Set in the region.
	 *   WARNING: If you set this false then you also need to uncomment code!
	 * * findRCCs = true run getRccs to find the common values connecting ALSs
	 * * allowOverlaps = true the ALSs are allowed to physically overlap
	 * * forwardOnly = true do a forward only search for RCCs
	 * </pre>
	 */
	public AlsWing() {
		// WARNING: you need to uncomment code if you change allowNakedSets!
		super(Tech.ALS_Wing, true, true, true, true);
	}

//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("%s: ttlRccs=%,d counts=%s\n", tech.name(), ttlRccs, Arrays.toString(counts));
//	}
//	private long ttlRccs;
////ALS_XZ:    ttlRccs=    8,801,276
////ALS_Wing:  ttlRccs=    6,085,665
////ALS_Chain: ttlRccs=3,100,592,090
//	private final long[] counts = new long[5];
////ALS_Wing: ttlRccs=6,079,210 counts=[1987565350, 5891824, 0, 5959622, 5487569]
////Note I've commented out the if-statement for counts[3]

	// findHints was boosted mainly from HoDoKu AlsSolver.getAlsXYWingInt
	// NOTE: I tried splitting-out searching of ALSs into a search method, but
	// it's significantly slower with the extra stack-work, so reverted.
	@Override
	protected boolean findHints(Grid grid, Idx[] candidates, Als[] alss
			, int numAlss, Rcc[] rccs, int numRccs, IAccumulator accu) {
		try {
			int i,n,m, j, v1,v2,a1,a2, v3,v4,b1,b2, zs, count;
			Rcc A, B;
			Als c, a=null, b=null;
			// presume that no hint will be found
			boolean result = false;
	//		ttlRccs += rccs.length;
			// foreach distinct pair of RCCs (a forward-only search)
			for ( i=0,n=numRccs,m=n-1; i<m; ++i ) {
				v1 = (A=rccs[i]).v1;
				v2 = A.v2;
				a1 = A.als1;
				a2 = A.als2;
				// WARNING: this loop executes nearly 2.3 BILLION times
				for ( j=i+1; j<n; ++j ) {
					v4 = (B=rccs[j]).v2;
					// need at least two different common candidates in A & B
					// which can't be true if they have same single value.
					if ( v1!=(v3=B.v1) || v2!=0 || v4!=0 ) {
						// the two RCCs contain 4 ALSs, from which we need to
						// connect 3 distinct ALSs; and since
						//     A.als1!=A.als2 && B.als1!=B.als2
						// thankfully not too many possibilites remain,
						// so compare them all to determine ALS c
						b2=B.als2;
						if ( a2==(b1=B.als1) && a1!=b2 ) { // out of sequence
							a=alss[a1]; b=alss[b2]; c=alss[a2];
	//						++counts[3];
						} else if ( a1==b1 && a2!=b2 ) {
							a=alss[a2]; b=alss[b2]; c=alss[a1];
	//						++counts[1];
						} else if ( a2==b2 && a1!=b1 ) {
							a=alss[a1]; b=alss[b1]; c=alss[a2];
	//						++counts[4];
	////counts[2] is 0, so either there's no hits or there's a mistake I can't see!
	//					} else if ( a1==b2 && a2!=b1 ) { // out of sequence
	//						a=alss[a2]; b=alss[b1]; c=alss[a1];
	//						++counts[2];
						} else {
							c = null;
	//						++counts[0];
						}
						if ( c != null
						  // if ALS a and ALS b have any common buds to eliminate
						  // done inline for speed
						  // ignore IDE: Dereferencing possible null pointer
						  && ( ( (a.buddies.a0 & b.buddies.a0)
							   | (a.buddies.a1 & b.buddies.a1)
							   | (a.buddies.a2 & b.buddies.a2) ) != 0 )
						  // and there's atleast one z-value: maybes common to
						  // ALS a and ALS b, excluding the RCC values.
						  && (zs=a.maybes & b.maybes & ~VSHFT[v1] & ~VSHFT[v2]
								& ~VSHFT[v3] & ~VSHFT[v4]) != 0
	//allowOverlaps=true in AlsWing constructor so this does nothing except eat CPU
	//					  // check overlaps: RCs already done, so just a and b
	//					  // doing this inline was no faster so stop inlining
	//					  && (allowOverlaps || a.idx.andNone(b.idx))
	//I believe this is "correct", but doesn't do anything in top1465.
	//					  // one cannot contain the other, even if allowOverlap
	//					  && !Idx.orEqualsEither(a.idx, b.idx)
						) {
							// examine each z value (usually one)
							count = 0; // number of cells eliminated from
							for ( int z : VALUESES[zs] )
								// victims: cells seeing all z's in both ALSs,
								// including the ALSs themselves.
								if ( victims.setAndAny(a.vBuds[z], b.vBuds[z]) )
									// eliminate z from each victim
									count += victims.forEach(grid.cells, (x) ->
										redPots.upsert(x, z)
									);
							if ( count > 0 ) {
								// FOUND an ALS-XY-Wing, with eliminations!
								// validate
								if ( HintValidator.ALS_USES ) {
									if ( !HintValidator.isValid(grid, redPots) ) {
										HintValidator.report(this.getClass().getSimpleName()
												, grid, Arrays.asList(a, b, c));
										if ( true ) {
											// I'm sort-of on top of invalid hints
											StdErr.exit(Log.me()+": invalid hint", new Throwable());
										} else {
											// just skip invalid elimination/s
											redPots.clear();
											return false;
										}
									}
								}
								// get the zValues String BEFORE we clear redPots
								final String zValues = Values.toString(redPots.valuesOf(), Frmt.COMMA_SP, Frmt.AND);
								// build the hint
								final AHint hint = new AlsWingHint(
									  this
									, redPots.copyAndClear()
									, a, b, c
									, v1 // x value
									, v3 // y value
									, zValues
								);
								// we found a hint (in GUI)
								result = true;
								// add the hint to accu
								if ( accu.add(hint) )
									return result; // exit-early (in batch)
							}
						}
					}
				}
			}
			// GUI mode: sort hints by score (elimination count) descending
			if ( result )
				accu.sort();
			return result;
		} finally {
			Cells.cleanCasA();
		}
	}

}
