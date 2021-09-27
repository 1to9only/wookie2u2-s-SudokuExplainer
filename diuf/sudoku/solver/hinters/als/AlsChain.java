/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * This is a copy-paste of ALL the HoDoKu code which implements ALS-XY-Chain.
 * Everything from AlsSolver to SudokuStepFinder, all fields, etc are all
 * sucked into this class. All kudos to hobiwan. The mistakes are mine. KRC.
 * Hobiwans licence statement is reproduced below.
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
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * AlsChain implements the Almost Locked Set XY-Chain Sudoku solving technique.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * determine there RCCs (connections), and call my "custom" findHints method
 * passing everything into my implementation of a specific search technique.
 * <p>
 * An ALS-Chain is 4-or-more ALSs that are chained-together by RCCs (Restricted
 * Common Candidates). The "restriction" is that all candidates in both ALSs
 * must see each other, and in ALS-Chains only cannot appear in the physical
 * overlap, if any, of the two ALSs. Each RCC must be in one ALS or the other,
 * pushing the other common candidate into the next ALS, and these ALSs form a
 * loop, so the non-restricted candidates common to the first and last ALS can
 * be removed from external cells seeing all occurrences of that value in both
 * of those ALSs.
 *
 * @author Keith Corlett 2020 May 24
 */
public final class AlsChain extends AAlsHinter
//implements diuf.sudoku.solver.IReporter
{
// This method is out of place to keep it near it's implements line
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("%s: ttlRccs=%,d\n", tech.name(), ttlRccs);
//	}
//	private long ttlRccs;
//ALS_XZ:    ttlRccs=    8,801,276
//ALS_Wing:  ttlRccs=    6,085,665
//ALS_Chain: ttlRccs=3,100,592,090

	/**
	 * Is 'als' a superset or subset of any ALS in 'chain', stopping at 'n'
	 * INCLUSIVE; ie does the given als make a loop in this chain?.
	 * <p>
	 * <b>NOTE WELL:</b> chain is populated upto {@code <= n};<br>
	 * not the faster and therefore more usual {@code < n};<br>
	 * because chain[0] is the startAls.
	 */
	private static boolean loops(Als als, Als[] chain, int n) {
		try {
			for ( int i=0; i<=n; ++i )
				if ( Idx.inbred(chain[i].idx, als.idx) )
					return true;
		} catch (Throwable eaten) { // esp NPE and AIOOBE
			return true; // if in doubt, chuck it out. Try an oyster!
		}
		return false;
	}

	// these fields are all set (and cleared) by findHints
	private Grid grid;
//notUsed	private Idx[] candidates;
	private Rcc[] rccs;
	private Als[] alss;
	private IAccumulator accu;

	// singleHintMode=true when findHints is passed a SingleHintsAccumulator.
	// This determines if search-recursion should exit-early upon hint.
	private boolean oneOnly;

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.ALS_Chain
	 * * allowNakedSets = false in getAlss, no Almost Locked Set may contain a
	 *   cell in any Locked Set in the region; else invalid ALS-Chain hints, so
	 *   KRC supressed them.
	 * * findRCCs = true run getRccs to find the common values connecting ALSs
	 * * allowOverlaps = false ALSs which physically overlap are not allowed to
	 *   form an RCC; else invalid ALS-Chain hints, so KRC supressed them.
	 * * forwardOnly = false so that getRccs does a full search of all possible
	 *   combinations of ALSs, instead of the faster forwardOnly search for XZs
	 *   and XyWings. NOTE that true is faster, but finds less hints, and I do
	 *   not understand why. forwardOnly searches ALL possible combinations,
	 *   and so should find the same hints, but it does not. sigh.
	 * </pre>
	 */
	public AlsChain() {
		// KRC 2021-06-16 17:33 try allowNakedSets=true
		// KRC 2021-06-16 17:34 try allowOverlaps=true
		super(Tech.ALS_Chain, true, true, true, false);
	}

	@Override
	protected boolean findHints(Grid grid, Idx[] candidates, Als[] alss
			, int numAlss, Rcc[] rccs, int numRccs, IAccumulator accu) {
		this.grid = grid;
//		this.candidates = candidates;
		this.alss = alss;
		this.rccs = rccs;
		this.accu = accu;
		this.oneOnly = accu.isSingle();
		boolean result = false;
		try {
			deletesMap.clear();
			for ( int i=0; i<numAlss; ++i ) {
				chainAlss[0] = startAls = alss[i];
				if ( isAlsInChain == null )
					isAlsInChain = new boolean[MAX_ALSS];
				else
					Arrays.fill(isAlsInChain, 0, numAlss, false);
				isAlsInChain[i] = true;
				firstRCC = null;
				result |= recurse(i, null, 0);
				if ( result && oneOnly )
					break; // we found one already!
			}
		} finally {
			this.grid = null;
//			this.candidates = null;
			this.alss = null;
			this.rccs = null;
			this.accu = null;
		}
		// GUI mode, so sort hints by score descending
		if ( result && !oneOnly )
			accu.sort();
		return result;
	}
	/** all chains found so far: eliminations and number of links. */
	private final SortedMap<String, Integer> deletesMap = new TreeMap<>();
	/** The first ALS in the chain (needed for elimination checks). */
	private Als startAls;
	/** Chain search: for every ALS already contained in the chain the
	 * respective index is true. */
	private boolean[] isAlsInChain;
	/** The first RC in the current chain (always is {@link #chainRccs}[0];
	 * needed for test for eliminations, cached for performance reasons). */
	private Rcc firstRCC = null;

	/**
	 * Recursively search the ALS at alsIndex checking each of its RCC. If the
	 * RCC fulfils the adjacency rules and the ALS to which the RCC points is
	 * not already part of the chain, then the ALS is added and the search
	 * continues recursively until we find an ALS-XY-Chain, or we run out of
	 * ALSs.
	 * <p>
	 * When the chain size reaches 4, every step is tested for possible
	 * eliminations.
	 * <p>
	 * <b>Caution:</b> If the first RC has two candidates, both of them have to
	 * be tried independently.
	 *
	 * @param index of the ALS used to look-up RCC indexes in the startInds and
	 *  endInds arrays (not the index of the bloody RCC itself, ya dunce).
	 * @param prevRC RC of the previous link in the current chain, that's used
	 *  to do an adjacency check
	 * @param chainIndex just pass me 0. Internally this is the index of the
	 *  current RCC (INCLUSIVE) in the chain of RCCs that I'm building.
	 */
	private boolean recurse(final int index, final Rcc prevRC, int chainIndex) {
		Rcc rcc; // current Restricted Common Candidate common to two ALSs:
		         // s=startAls (left) and a=currAls (right)
		Als a; // the current ALS
		Idx[] avBuds, avs;
		int rc1,rc2,rc3,rc4, i,I, als2, zMaybes;
		boolean any;
		final Idx tmp = this.set1; // used for various checks
		final Idx zBuds = this.set2; // all buds of v (incl the ALSs).
		final Pots reds = this.redPots;
		final Pots blues = this.bluePots;
		final Als s = this.startAls;
		final Idx[] svBuds = s.vBuds;
		final Idx[] svs = s.vs;
		final Als[] chainAlss = this.chainAlss;
		final int[] starts = RCC_FINDER.startInds;
		final int[] ends = RCC_FINDER.endInds;
		final Rcc[] rccs = this.rccs;
		final Als[] alss = this.alss;
		final boolean[] isAlsInChain = this.isAlsInChain;
		final boolean allowOverlaps = this.allowOverlaps;
		// check all RCs; if none exist the loop is never entered
		boolean firstTry = true;
		// presume that no hint will be found
		boolean result = false;
//		ttlRccs += rccs.length;
		// foreach currAls (right) in ALS's connecting with startAls (left).
		// nb: the "map" between ALSs is stored in startIndices and endIndices.
		// nb: index is into start/endIndices arrays, not the index of an RCC.
		for ( i=starts[index],I=ends[index]; i<I; ++i )
			// select my RC (sets rcc.which for later)
			if ( (rcc=rccs[i]).whichRC(prevRC, firstTry)
			  // check ALS not already in chain
			  && !isAlsInChain[als2=rcc.als2]
			  // get 'a' the current ALS, and check for common buds.
			  && s.buddies.andAny((a=alss[als2]).buddies)
			  // 'a' can not be a super/subset of any in chainAlss<=chainIndex
			  // even if allowOverlaps
			  && !loops(a, chainAlss, chainIndex)
			) {
				// ok, currALS can be added
				if ( chainIndex == 0 )
					firstRCC = rcc;
				else if ( chainIndex+2 > MAX_RCCS_PER_CHAIN )
					// give-up when chain full (currently at 5).
					// Ive never seen a hint from more than 5, which does NOT
					// mean they don't exist, only that I dont have any, so dey
					// must be rare, so its not worth me looking any further.
					// You, however, might want to bump it up to 6, or even 7.
					// Hobiwans MAX_RCCS_PER_CHAIN=32 was overenthusiastic!
					break;
				// nb: chainAlss[0]=startALS, 1..chainIndex=currAlss INCLUSIVE
				// nb: from here down: to continue we also decrement chainIndex
				// otherwise da currALS stays in da chain even though it's been
				// skipped, so that subsequent recursions include it.
				// nb: hobiwans chainRccs was nonsensical. I have chainAlss.
				chainAlss[++chainIndex] = a;
				isAlsInChain[als2] = true;
				// if there's at least 4 ALSs then check for eliminations
				// nb: 4 ALSs = 3 in the chain + the startAls, called s.
				if ( chainIndex > 2 ) {
					// get the four RC values from the two RCCs
					if(firstRCC.which==2) rc1=0; else rc1=firstRCC.v1;
					if(firstRCC.which==1) rc2=0; else rc2=firstRCC.v2;
					// nb: if firstRCC.which==3 then both rc1 and rc2 are set
					switch ( rcc.which ) { // 0=none, 1=v1, 2=v2, 3=both
					case 1: rc3 = rcc.v1; break;
					case 2: rc3 = rcc.v2; break;
					case 3: rc3 = rcc.v1; break;
					default: rc3 = 0; break; // Never happens. Never say never.
					}
					rc4 = rcc.v2;
					// The RC "none" value has been confused: it's 0 (not -1)
					// because ~0 is 32 set bits, a no-op in below &'s
					assert rc1!=-1 && rc2!=-1 && rc3!=-1 && rc4!=-1;
					// get maybes common to both ALSs except the RCs,
					// and check that there's at least one.
					if ( (zMaybes=s.maybes & a.maybes & ~VSHFT[rc1]
							& ~VSHFT[rc2] & ~VSHFT[rc3] & ~VSHFT[rc4]) != 0
					  // check overlaps: RCs already done, so just s and a
					  && (allowOverlaps || s.idx.andNone(a.idx))
					) {
						// examine each maybe common to both ALSs except RCs
						any = false;
						avs = a.vs;
						avBuds = a.vBuds;
						for ( int z : VALUESES[zMaybes] )
							// get cells which see all z's in both ALSs
							if ( svBuds[z].andAny(avBuds[z]) ) {
								// zBuds are eliminated (red)
								zBuds.setAnd(svBuds[z], avBuds[z]).forEach(grid.cells
									,  (c) -> reds.upsert(c, z)
								);
								// z's are fins (blues) (display only)
								tmp.setOr(svs[z], avs[z]).forEach(grid.cells
									, (c) -> blues.upsert(c, z)
								);
								any = true;
							}
						if ( any ) {
							// FOUND an ALS-XY-Chain!
							if ( HintValidator.ALS_USES ) {
								// check elims are not in the solution. sigh.
								if ( !HintValidator.isValid(grid, reds) ) {
									HintValidator.report(
										  this.getClass().getSimpleName()
										, grid
										, Als.linkedList(chainIndex, chainAlss)
									);
//									// not on top of it -> report & continue
//									--chainIndex;
//									redPots.clear(); bluePots.clear();
									// on top of it -> die
									StdErr.exit(StdErr.me()+": invalid hint", new Throwable());
								}
							}
							// build a list of alss: chainAlss[0] = startAls
							//        and 1..chainIndex INCLUSIVE = currAlss
							final List<Als> alssList = Als.linkedList(chainIndex+1, chainAlss);
							// nb: blue overwrites all incl red; so rmv reds
							blues.removeAll(reds);
							// tag normally blank (so you see nothing)
//							String tag = invalidity.isEmpty() ? "" : "<br><h2>"+invalidity+"</h2>";
							final String tag = "";
							// build the hint
							final AHint hint = new AlsChainHint(
								  this
								, new Pots(reds) // copy-off the field!
								, alssList
								, tag
							);
							// clear the collection fields for next time.
							// nb: if !any then they both remain empty
							reds.clear();
							blues.clear();
							result = true;
							if ( accu.add(hint) )
								return true;
						}
					}
				}
				// and onto the next level ...
				// else no space left so stop recursing, go onto next RCC
				if ( chainIndex+1 < MAX_RCCS_PER_CHAIN ) // MAX_RC currently 5
					result |= recurse(als2, rcc, chainIndex);
				// go back a level (and-so-on all the way up recursive stack)
				if ( result && oneOnly )
					return true;
				isAlsInChain[als2] = false;
				chainAlss[chainIndex--] = null;
				if ( prevRC == null ) {
					// this is the first RC in the chain
					if ( rcc.v2!=0 && firstTry ) {
						// and a second RC value is present, so try again
						// using the second RC value
						firstTry = false;
						--i;
					} else
						firstTry = true;
				}
			}
		// next RCC
		return result;
	}
	// Maximum number of RCCs in an ALS-Chain (forward-only search).
	// * 5 is just the longest chain which produces a hint in top1465,
	//   but it might be possible to hint from longer chains,
	//   so this limit MAY BE WRONG!
	// * hobiwans MAX_RCCs was 32, which I think is "way over the top".
	private static final int MAX_RCCS_PER_CHAIN = 5;
	// ALS's in current Chain.
	// * chainAlss[0] is always the startAls, 1..chainIndex are currAls's
	private final Als[] chainAlss = new Als[MAX_RCCS_PER_CHAIN];

	// I create these variables ONCE rather than in each recursion
	private final Idx set1 = new Idx();
	// all buds of v (including the ALSs).
	private final Idx set2 = new Idx();
	// don't forget to copy-them-off to create a hint, then clear both!
	private final Pots redPots=new Pots(), bluePots=new Pots();

}
