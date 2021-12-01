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

import diuf.sudoku.Grid;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.Validator;
import static diuf.sudoku.solver.hinters.Validator.ALS_VALIDATES;
import static diuf.sudoku.solver.hinters.Validator.isValid;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.utils.Html.colorIn;
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
 * KRC 2021-05-31 Making XYWing faster, coz it's slowest hinter that I use.
 * The fastest implementation so far ran in about 18-20 seconds for top1465.
 * This implementation runs in about 20-22 seconds, but is more readable.
 * I'm willing to forego performance for readability.
 *
 * KRC 2021-10-19 Made AlsWing faster using lessons learned in AlsChain.
 * Primarily avoid the else to set ok false.
 * 6_30.175  8,211,412,700  6876  1,194,213  2833  2,898,486
 * 13-22-23  6,678,551,500  6875    971,425  2831  2,359,078
 * 18-21-00  6,756,448,400  6874    982,899  2831  2,386,594
 * I am no longer willing for forgeo performance.
 * </pre>
 *
 * @author Keith Corlett 2020 May 5
 */
public final class AlsWing extends AAlsHinter
//implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[5];

	// adds debug detail to my hints, to reveal exactly which ALSs are in the
	// hint, in order to debug through it, to workout where my hint went.
	// package visible for use in AlsWingHint.
	static final boolean DEBUG_MODE = false; // @check false

	// buds of all v's (including the ALSs).
	private final Idx victims = new Idx();

	// removable (red) potential values
	private final Pots reds = new Pots();

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
		super(Tech.ALS_Wing, true, true, true, true); // AlsFinderForwardOnlyAllowOverlaps
		assert allowOverlaps; // else you need to uncomment code
	}

	// findHints was boosted mainly from HoDoKu AlsSolver.getAlsXYWingInt
	// DESIGN: I tried splitting-out searching of ALSs into a search method,
	// but it's significantly slower with the extra stack-work, so reverted.
	@Override
	protected boolean findHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		// ANSI-C style variables, for reduced stack-work.
		// Two RCCs connect three ALSs to form an AlsWing.
		Rcc A, B; // first and second Restricted Common Candidate
		Idx aB, bB; // vBuds[z] of alss a and b
		int[] za; // VALUESES[zs]
		int i // index of first RCC (A) in rccs
		  , j // index of second RCC (B) in rccs
		  , v1, v2 // A.v1, A.v2
		  , aCands // A.cands is VSHFT[A.v1] | SVSHFT[A.v2]
		  , a1, a2 // alss-indexes: A.source, A.related
		  , b1, b2 // alss-indexes: B.source, B.related
		  , zs // maybes common to a and b except both RC-values
		  , zi,zn // za index, za.length
		  , i0,i1,i2 // victims Idx exploded
		  ;
		boolean ok = false; // are there three ALSs, then any eliminations
		Als a=null, b=null, c=null; // the three ALSs
		final int lastRcc = numRccs - 1; // index of the last rcc
		final Idx victims = this.victims; // cells eliminated from
		// presume that no hint will be found
		boolean result = false;
		// foreach rcc except the last
		for ( i=0; i<lastRcc; ++i ) {
			v1 = (A=rccs[i]).v1;
			v2 = A.v2;
			aCands = A.cands;
			a1 = A.source;
			a2 = A.related;
			// foreach subsequent rcc (a forward only search)
			// this loop executes about 2.3 BILLION times
			for ( j=i+1; j<numRccs; ++j ) {
				// the two RCCs contain four ALSs, from which we need to
				// connect three distinct ALSs; and since
				//     A!=B && A.source!=A.related && B.source!=B.related
				// thankfully not too many possibilites remain, so compare them
				// all to determine ALS c (ordered by hit-rate descending)
				if ( (b1=(B=rccs[j]).source)==a1 & (b2=B.related)!=a2 ) {
					// first;   last;       middle;
					a=alss[a2]; b=alss[b2]; c=alss[a1]; ok=true;
				} else if ( b1==a2 && b2!=a1 ) {
					a=alss[a1]; b=alss[b2]; c=alss[a2]; ok=true;
				} else if ( b2==a2 && b1!=a1 ) {
					a=alss[a1]; b=alss[b1]; c=alss[a2]; ok=true;
				}
				// if we found our three alss to search
				if ( ok ) { // nb: just ~0.764% of rcc-pairs are ok!
					// reset for next-time even if I am rejected.
					ok = false;
					// avert IDE warning: deferencing possible null pointer
					assert a!=null && b!=null;
					// need atleast 2 different common candidates in A & B
					// which can't be true if they have same single value.
					if ( (v1!=B.v1 || v2!=0 || B.v2!=0)
					  // and a and b have any common buds to eliminate
					  && ( ((aB=a.buds).a0 & (bB=b.buds).a0)
						 | (aB.a1 & bB.a1)
						 | (aB.a2 & bB.a2) ) != 0
					  // and there are z-value/s: maybes common to a and b
					  // except the RC-values.
					  && (zs=a.maybes & b.maybes & ~aCands & ~B.cands) != 0
//allowOverlaps is allways true in my world
//					  // check overlaps: RCs already done, so just a and b
//					  // doing this inline was no faster so stop inlining
//					  && (allowOverlaps || a.idx.andNone(b.idx))
					) {
						// looks like an AlsWing, but any eliminations?
						// foreach z-value (usually one)
						for ( za=VALUESES[zs],zn=za.length,zi=0; zi<zn; ++zi ) {
							// victims: externals seeing all zs in both ALSs
							if ( ( (i0=(aB=a.vBuds[za[zi]]).a0 & (bB=b.vBuds[za[zi]]).a0)
								 | (i1=aB.a1 & bB.a1)
								 | (i2=aB.a2 & bB.a2) ) != 0
							) {
								ok |= reds.upsertAll(victims.set(i0,i1,i2), grid, za[zi]);
							}
						}
						if ( ok ) {
							// FOUND an AlsWing, with eliminations!
							// debug only
							final Rcc[] rcs; if(DEBUG_MODE) rcs=new Rcc[]{A, B}; else rcs=null;
							// nb: A.v1 is x, B.v1 is y
							// nb: swap b and c, which are misnamed above.
							final AlsWingHint hint = new AlsWingHint(this
								, reds.copyAndClear(), a, c, b, v1, B.v1, rcs);
							if ( ALS_VALIDATES && !isValid(grid, hint.reds) ) {
								ok = handle(hint, grid, Arrays.asList(a, b, c));
							}
							if ( ok ) {
								ok = false;
								result = true;
								if ( accu.add(hint) ) {
									return result; // exit-early (in batch)
								}
							}
						}
					}
				}
			}
		}
		// GUI mode: sort hints by score (elimination count) descending
		if ( result ) {
			accu.sort(null);
		}
		return result;
	}

	private boolean handle(AlsWingHint hint, Grid grid, Iterable<Als> alss) {
		Validator.report(tech.name(), grid, alss);
		if ( Run.type==Run.Type.GUI && Run.ASSERTS_ENABLED ) {
			hint.isInvalid = true;
			hint.debugMessage = colorIn("<p><r>"+prevMessage+"</r>");
			return true;
		}
		// GUI without -ea, test-cases, batch
		throw new UnsolvableException(Log.me()+": bad reds: "+hint.reds);
	}

}
