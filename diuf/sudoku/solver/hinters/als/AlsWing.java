/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.IdxI;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.solver.hinters.IHinter.DUMMY;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.ALSS;

/**
 * AlsWing implements the {@link Tech.ALS_Wing} Sudoku solving technique.
 * <p>
 * An AlsWing involves three ALSs, which we get from the "All" RCCs, using the
 * starts and ends, as per AlsChain.
 * <p>
 * This is my second go at AlsWing. This version is faster than previous coz it
 * processes MANY-MANY less RCCs. Foreach ALS we get its RCCs, and we get RCCs
 * of its related ALS, so we have two RCCs, from which we select three ALSs,
 * which is also simpler coz we already know these three ALSs are related, so
 * it becomes an opt-out problem, not a mass opt-in shotgun-wanking. Amex?
 * <p>
 * The old version processed all possible pairs of RCCs, an O(n*n) operation,
 * where n is in the thousands, ergo it was never going to cut the mustard. The
 * nature of the operation is actually WORSE here O(n*n*n) but n is limited to
 * related RCCs only, which come in tens (not thousands) so it's heaps faster.
 * Net-result is this version takes about a third of the time.
 * <p>
 * <pre>
 * 2023-09-13 AlsWing1 takes 29.68% time, to find 99.66% of hints. Bargain!
 * AlsWing  4,736,693,700  8288  571,512  2716  1,743,996  ALS_Wing
 * AlsWing1 1,405,970,700  8272  169,967  2707    519,383  ALS_Wing
 * Don't ask me about that 0.34% I refuse to look.
 *
 * 2023-09-14 Binned the old version. Promoted AlsWing1 to AlsWing.
 * </pre>
 *
 * @author Keith Corlett 2023-09-13
 */
public class AlsWing extends AAlsHinter
//	implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(java.util.Arrays.toString(COUNTS));
//	}
//	private long[] COUNTS = new long[10];

	public AlsWing() {
		super(Tech.ALS_Wing);
	}

	@Override
	public void setFields(final Grid grid) {
		this.grid = grid;
		maybes = grid.maybes;
	}

	@Override
	public void clearFields() {
		grid = null;
		maybes = null;
	}

	/**
	 * AlsWings finds AlsWing hints in grid, adding them to hints,
	 * returning any hint/s found.
	 * <pre>
	 * An RCC is a relationship between two ALSs: source -> related. These two
	 * ALSs share v1 (and ~9% v2) where all vs see each other, hence v1 (and
	 * ~9% v2) must be in either one ALS or the other (never both), and an ALS
	 * may loose just ONE value before it goes invalid: insufficient values to
	 * place one value in each cell, hence we can chain ALSs together, and we
	 * know that a value (we know not which) is pushed along the chain. When we
	 * find a last ALS that likewise sees the first ALS we can eliminate z from
	 * external cells seeing all occurrences of each z-value: common maybes of
	 * these two ALSs, EXCEPT the two RC-values:
	 *    (x) used to link last ALS back to previous one in chain; and
	 *    (y) used to link second ALS back-to first ALS in chain;
	 *    (I can't remember which is x, and which y, but it matters not)
	 * because the z value is "locked into" the first and last ALS by the push
	 * action, in either direction, of the als-chain.
	 *
	 * An ALS_Wing is JUST an ALS_Chain of exactly three ALSs. AlsWing is
	 * separate from AlsChain because AlsWing can be (and is) implemented more
	 * efficiently, hence the ALS_Wing pattern became a distinct brand.
	 * </pre>
	 *
	 * @return any hint/s found
	 */
	@Override
	protected boolean findAlsHints() {											//      8,572
		// ANSII-C style vars, for ONE stackframe
		Rcc A, B; // two RCCs, in which we find three distinct ALSs
		Als first, last, middle; // first ALS, last ALS, middle ALS
		IdxI fbs[],fb, lbs[],lb; // first.vBuds,oneOf, last.vBuds,oneOf
		long vc0; int vc1; // victims exploded Idx
		int a // A loop: index
		, b,bn // B loop: index, size
		, aS,aR, aV1,aV2, aCands // A.attributes
		, zs, za[], zn, zi, z // z-values: bits, array, size, index, value
		;
		final Pots reds = new Pots(); // removable (red) Potentials
		final int[] rccsStart = rccFinder.getStarts(); // starting alss indexes
		final int[] rccsEnd = rccFinder.getEnds(); // ending alss indexes
		boolean anyElims = false // are there any eliminations
			  , result = false; // presume that no hints will be found
		// foreach first RCC
		for ( a=0; a<numRccs; ++a ) {											//  7,834,590
			aS = (A=RCCS[a]).source;
			aR = A.related;
			aV1 = A.v1;
			aV2 = A.v2;
			aCands = A.cands;
			// foreach RCC that's related to the first RCC.
			// nb: starts..ends is index of first..last+1 related RCC.
			// I look-up A.related to get RCCs that are related to it: B, so
			// now we have our two RCCs containing three distinct ALSs, so do
			// these three distinct ALSs fit the AlsWing pattern?
			for ( b=rccsStart[aR],bn=rccsEnd[aR]; b<bn; ++b ) {					// 84,867,055
				// we need two different common candidates in A & B, which
				// cannot be true if they have same single value.
				if ( (aV1!=(B=RCCS[b]).v1 || (aV2|B.v2)>0)						// 31,110,421
				  // and B's related ALS is not A's source
				  // we need THREE distinct ALSs, which cant happen if A.source
				  // is the same ALS as B.related. I should put starts and ends
				  // on forwardOnly, coz the All in All-RCCs slows me down, as
				  // does calculating All-RCCs when AlsChain isnt wanted. Lazy!
				  && B.related != aS											// 30,647,788
				) {
					// read the first ALS and last ALS from the RCCs.
					// nb: middle ALS is set only when we hint, for speed.
					last=ALSS[B.related]; first=ALSS[aS];
					// if these two ALSs have common buds to eliminate from
					if ( ( ((fb=first.buds).m0 & (lb=last.buds).m0)
						 | (fb.m1 & lb.m1) ) > 0								// 20,145,683
					  // and maybes common to both ALSs, except the RCs
					  && (zs=first.maybes & last.maybes & ~aCands & ~B.cands) > 0
					) {															// 15,504,307
						// yep, its an AlsWing, but any eliminations?
						// victims = other cells seeing all z in both ALSs
						fbs = first.vBuds;
						lbs = last.vBuds;
						zn = VSIZE[zs];
						za = VALUESES[zs];
						zi = 0;
						do {													// 25,927,813
							z = za[zi];
							fb = fbs[z];
							lb = lbs[z];
							if ( ( (vc0=fb.m0 & lb.m0)
								 | (vc1=fb.m1 & lb.m1) ) > 0 )					//      2,335
								anyElims |= reds.upsertAll(vc0,vc1, maybes, VSHFT[z], DUMMY);
						} while (++zi < zn);
						if ( anyElims ) {										//      2,197
							// FOUND an AlsWing, with eliminations!
							anyElims = false; // reset for next-time
							result = true; // remember that we found a hint
							// fastard: determine middle ALS only upon hint
							if ( first == ALSS[aR] )
								middle = ALSS[aS];
							else
								middle = ALSS[aR];
							// if accu says so, exit-early
							if ( accu.add(new AlsWingHint(grid, this
								, reds.copyAndClear(), first, middle, last
								, aV1, B.v1)) )
								return result;
						}
					}
				}
			}
		}
		return result;
	}

}
