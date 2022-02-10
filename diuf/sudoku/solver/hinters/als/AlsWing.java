/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.UnsolvableException;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.solver.hinters.Validator.reportRedPots;
import static diuf.sudoku.utils.Html.colorIn;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import static diuf.sudoku.solver.hinters.Validator.VALIDATE_ALS;
import static diuf.sudoku.solver.hinters.Validator.validOffs;

/**
 * AlsWing implements the Almost Locked Set Wing Sudoku solving technique.
 * An ALS-Wing is an ALS-Chain of three ALSs. Two Almost Locked Sets (ALSs)
 * which share a common candidate where all occurrences in both ALSs see each
 * other constitutes a Restricted (all see each other) Common Candidate (RCC),
 * so that this value will be one ALS or the other, but never both. Such ALSs
 * may be chained together into a loop. In AlsWing's case a short loop of just
 * three ALSs. We can eliminate occurrences of values common to both the first
 * ALS and the last ALS except the candidates used in links, from cells outside
 * of both ALSs, which see all occurrences in both ALSs, ie eliminate any
 * external z's.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * connect them with RCCs, and call my "custom" findAlsHints method.
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
 * <p>
 * No matter what has changed this is still, at it's heart, hobiwan's solution.
 * Kudos to hobiwan. Mistakes are mine.
 * <pre>
 * KRC 2021-05-31 Making XYWing faster, coz it's slowest hinter that I use.
 * The fastest implementation so far ran in about 18-20 seconds for top1465.
 * This implementation runs in about 20-22 seconds, but is more readable.
 * I'm willing to forego performance for readability.
 *
 * KRC 2021-10-19 Made AlsWing faster using lessons learned in AlsChain.
 * Primarily avoid the else to set ok false. Me like fast.
 * 6_30.175  8,211,412,700  6876  1,194,213  2833  2,898,486
 * 13-22-23  6,678,551,500  6875    971,425  2831  2,359,078
 * 18-21-00  6,756,448,400  6874    982,899  2831  2,386,594
 *
 * KRC 2022-01-10 Java 17 is about 30% slower than Java 8. Trying to speed-up.
 * suspect derefs slower in 17: AlsWing is particularly badly effected.
 * JDK08:  6,714,289,400 6633 1,012,255 2753 2,438,899 ALS_Wing
 * JDK17: 11,614,440,200 6633 1,751,008 2753 4,218,830 ALS_Wing yesterday
 * TODAY:  9,853,089,800 6633 1,485,465 2753 3,579,037 ALS_Wing dunno changes
 * avBuds, bvBuds is 24.4ms SLOWER
 *         9,877,521,100 6633 1,489,148 2753 3,587,911
 * reverted is slower again, is a WTF
 *         9,979,777,500 6633 1,504,564 2753 3,625,055 ALS_Wing
 * reinstate singleStackFrame: complex za loop with abv and bvb in initialiser
 *         9,680,566,100 6633 1,459,455 2753 3,516,369 ALS_Wing
 * always do the first zValue, then loop for subsequent zValues
 *         9,678,838,400 6633 1,459,194 2753 3,515,742 ALS_Wing
 * set ok in the if-statements to avoid defaulting to false
 *         9,691,214,000 6633 1,461,060 2753 3,520,237 ALS_Wing
 * set ok in only the FIRST if-statement to avoid defaulting to false
 *         9,610,639,800 6633 1,448,912 2753 3,490,969 ALS_Wing
 * just set ok instead of if(aBuds intersects bBuds) then ok = add elims
 *         9,674,020,972 6633 1,458,468 2753 3,513,992 ALS_Wing
 * put if-statement back in zValues loop, to not set ok unless necessary
 *         9,812,207,394 6633 1,479,301 2753 3,564,187 ALS_Wing
 * NOPE, still slower, so put if-statement back in the first zValue also
 *         9,657,391,575 6633 1,455,961 2753 3,507,951 ALS_Wing
 * do first z, then loop through subsequent; dunno when v1s/v2s went in
 *         9,505,527,495 6633 1,433,066 2753 3,452,788 ALS_Wing
 * forget VALUESES, just "poll" z.
 *         9,038,499,087 6633 1,362,656 2753 3,283,145 ALS_Wing
 * BUT ____ ME SIDEWAYS WE'RE STILL _____ING MILES AWAY FROM 6,714,289,400
 * So, run current version in Java 1.8 with mimimal changes to make it work.
 *         6,524,903,119 6633   983,703 2753 2,370,106 ALS_Wing
 *         about 200ms faster than prev 1.8 verses 2 seconds here, but
 *         1.8 takes two thirds of the time, so had less fat to loose.
 * CONCLUSION: Oracle made Java 17 slow, so stick with Java 1.8, for speed.
 *         6,512,673,200 6633   981,859 2753 2,365,664 ALS_Wing
 * </pre>
 *
 * @author Keith Corlett 2020 May 5
 */
public class AlsWing extends AAlsHinter
//implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+Arrays.toString(COUNTS));
//		diuf.sudoku.utils.Log.teeln(tech.name()+": min="+minGoodNumRccs+"/"+minNumRccs+" max="+maxGoodNumRccs+"/"+maxNumRccs);
//	}
//	private final long[] COUNTS = new long[20];
//	private int minNumRccs=Integer.MAX_VALUE, maxNumRccs=0
//		  , minGoodNumRccs=Integer.MAX_VALUE, maxGoodNumRccs=0;

	// adds debug detail to my hints, to reveal exactly which ALSs are in the
	// hint, in order to debug through it, to workout where my hint went.
	// package visible for use in AlsWingHint.
	static final boolean DEBUG_MODE = false; // @check false

	// buds of all v's (including the ALSs).
	private final Idx victims = new Idx();

	// removable (red) potential values
	private final Pots reds = new Pots();

	// these arrays are (currently) 9K each! Go no bigger! and no more of!
	private final int[] sources = new int[MAX_RCCS];
	private final int[] relateds = new int[MAX_RCCS];
	private final int[] v1s = new int[MAX_RCCS];
	private final int[] v2s = new int[MAX_RCCS];
	private final int[] candss = new int[MAX_RCCS];

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.ALS_Wing
	 * * allowNakedSets = true my Almost Locked Sets include cells that are
	 *   part of a Locked Set in the region.
	 *   WARNING: If you set this false then you also need to uncomment code!
	 * * findRccs = true run getRccs to find the common values connecting ALSs
	 * * allowOverlaps = true the ALSs are allowed to physically overlap
	 * * forwardOnly = true do a forward only search for RCCs
	 * </pre>
	 */
	public AlsWing() {
		super(Tech.ALS_Wing, true, true, true, true);
		assert allowOverlaps; // else you need to uncomment code
		assert allowNakedSets; // else you need to uncomment code
	}

	@Override
	protected boolean findAlsHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
//		minNumRccs = Math.min(minNumRccs, numRccs);
//		maxNumRccs = Math.max(maxNumRccs, numRccs);
		// HACK: specific to top1465, which may miss hints in other puzzles.
		if ( numRccs < 200 ) // ALS_Wing: min=200/171 max=3996/3996
			return false;
		// ANSI-C style variables, for reduced stack-work.
		Idx[] avBuds, bvBuds; // a.vBuds, b.vBuds
		Idx aBuds, bBuds; // a.buds, b.buds
		int i // index of first RCC (A) in rccs
		  , j // index of second RCC (B) in rccs
		  , v1, v2 // A.v1, A.v2
		  , aCands // A.cands is VSHFT[A.v1] | VSHFT[A.v2]
		  , a1, a2 // alss-indexes: A.source, A.related
		  // HIJACK: b1,b2 are hijacked coz smaller stack is faster!
		  // nb: do NOT hijack cached A properties (or indexes) in the B loop!
		  , b1, b2 // alss-indexes: B.source, B.related
		  , i0,i1,i2 // victims Idx exploded
		  , zs // zCands: bitset of values common to both alss EXCEPT rcc.cands
		  , z // the zValue, one of the zCands
		  ;
		boolean ok; // are there three ALSs, then any elims
		boolean result = false; // presume that no hint will be found
		Als a=null, b=null, c=null; // the three ALSs: first, last, middle
		// faster to dereference Rcc properties once per RCC than it is to
		// dereference them from the B's factorial(numRccs) times.
		// stack references are faster than heap-references.
		final int[] sources = this.sources;
		final int[] relateds = this.relateds;
		final int[] v1s = this.v1s;
		final int[] v2s = this.v2s;
		final int[] candss = this.candss;
		for ( i=0; i<numRccs; ++i ) {
			final Rcc rcc = rccs[i];
			sources[i] = rcc.source;
			relateds[i] = rcc.related;
			v1s[i] = rcc.v1;
			v2s[i] = rcc.v2;
			candss[i] = rcc.cands;
		}
		final int lastRcc = numRccs - 1; // index of the last rcc
		// foreach rcc except the last
		for ( i=0; i<lastRcc; ++i ) { // A loop
			a1 = sources[i];
			a2 = relateds[i];
			v1 = v1s[i];
			v2 = v2s[i];
			aCands = candss[i];
//<HAMMERED comment="the B loop executes about 2,641,246,688 times">
			// foreach subsequent rcc (a forward only search)
			for ( j=i+1; j<numRccs; ++j ) { // B loop: rccs[j] is logically 'B'
				// WTF: Two RCCs connect three ALSs to form an AlsWing.
				// the two RCCs contain four ALSs, from which we need to
				// connect three distinct ALSs; and since
				//     A!=B && A.source!=A.related && B.source!=B.related
				// thankfully not too many possibilites remain, so compare
				// them all to determine ALS c (the middle ALS).
				// order: RCCS by source ASCENDING, related ASCENDING
				// fastard: set ok true/false once, then change only if true.
				// fastard: use parallel sources and relateds arrays instead of
				// dereferencing from the Rcc struct factorial(numRccs) times.
				// Array lookups are MUCH faster than instance dereferencing.
				if ( ok = (b1=sources[j])==a1 & (b2=relateds[j])!=a2 ) {
					// first;   last;       middle;
					a=alss[a2]; b=alss[b2]; c=alss[a1];
				} else if ( b2 == a2 ) {
					assert b1 != a1;
					a=alss[a1]; b=alss[b1]; c=alss[a2]; ok = true;
				} else if ( b1==a2 && b2!=a1 ) {
					a=alss[a1]; b=alss[b2]; c=alss[a2]; ok = true;
				}
				// if we found our three alss to search
				// nb: only about 52 MILLION or 2% of rcc-pairs are ok!
				if ( ok ) {
//</HAMMERED>
					assert a!=null && b!=null; // avert IDE's NPE warning
					// need atleast 2 different common candidates in A & B
					// which can't be true if they have same single value.
					// fastard: use parallel v1s and v2s instead of deref'ing.
					if ( (v1!=v1s[j] || v2!=0 || v2s[j]!=0)
					  // and a and b have any common buds to eliminate from
					  && ( ((aBuds=a.buds).a0 & (bBuds=b.buds).a0)
						 | (aBuds.a1 & bBuds.a1)
						 | (aBuds.a2 & bBuds.a2) ) != 0
					  // and there are z-value/s: maybes common to a and b
					  // except the RC-values.
					  // nb: zs is zCands: the zValues as a bitset.
					  && (zs=a.maybes & b.maybes & ~aCands & ~candss[j]) != 0
//allowOverlaps is allways true in my world
//					  // check overlaps: RCs already done, so just a and b
//					  // doing this inline was no faster so stop inlining
//					  && (allowOverlaps || a.idx.andNone(b.idx))
					) {
						// foreach z: add external cells seeing all occurrences
						// of z in both alss to the reds.
						// zs != 0 so there's always a first z
						// fastard: set ok ONCE here, then each subsequent ok
						// is or'ed in only if true; so ok is set less often,
						// which is a bit faster. sigh.
						ok = ( (i0 = (aBuds=(avBuds=a.vBuds)[z=VFIRST[zs]]).a0
								   & (bBuds=(bvBuds=b.vBuds)[z]).a0)
							 | (i1 = aBuds.a1 & bBuds.a1)
							 | (i2 = aBuds.a2 & bBuds.a2) ) != 0
							&& reds.upsertAll(victims.set(i0,i1,i2), grid, z);
						// foreach subsequent z
						// fastard: remove the previous z from the zs bitset to
						// "poll" z's, coz its faster than iterating VALUESES.
						while ( (zs &= ~VSHFT[z]) != 0 ) // remove z
							// fastard: an if-statement seems to be faster here
							if ( ( (i0 = (aBuds=avBuds[z=VFIRST[zs]]).a0
									   & (bBuds=bvBuds[z]).a0)
								 | (i1 = aBuds.a1 & bBuds.a1)
								 | (i2 = aBuds.a2 & bBuds.a2) ) != 0 )
								ok |= reds.upsertAll(victims.set(i0,i1,i2), grid, z);
						if ( ok ) {
//							minGoodNumRccs = Math.min(minGoodNumRccs, numRccs);
//							maxGoodNumRccs = Math.max(maxGoodNumRccs, numRccs);
							// FOUND an AlsWing, with eliminations!
							// nb: rccs[j] is logically Rcc 'B' (uncached)
							// nb: A.v1 is x, B.v1 is y
							// nb: swap b and c, which are mispositioned above
							//     into first, last, middle where-as we always
							//     have them in order F,M,L in the hints.
							//     Foreskin Markup Language, obviously!
							//     Masturbation sans chainsaw recommended.
							//     Tuxedo rental costs extra.
							final AlsWingHint hint = new AlsWingHint(this
								, reds.copyAndClear(), a, c, b, v1, v1s[j]
								, DEBUG_MODE ? new Rcc[]{rccs[i], rccs[j]} : null);
							if ( VALIDATE_ALS ) {
								if ( !validOffs(grid, hint.reds) )
									ok = handle(hint, grid, Arrays.asList(a, b, c));
							}
							if ( ok ) {
								result = true;
								if ( accu.add(hint) )
									return result;
							}
						}
					}
				}
			}
		}
		// GUI mode: sort hints by score (elimination count) descending
		if ( result )
			accu.sort(null);
		return result;
	}

	private boolean handle(final AlsWingHint hint, final Grid grid, final Iterable<Als> alss) {
		reportRedPots(tech.name(), grid, alss);
		if ( Run.type==Run.Type.GUI && Run.ASSERTS_ENABLED ) {
			hint.setIsInvalid(true);
			hint.setDebugMessage(colorIn("<p><r>"+prevMessage+"</r>"));
			return true;
		}
		// GUI without -ea, test-cases, batch
		throw new UnsolvableException(Log.me()+": bad reds: "+hint.reds);
	}

}
