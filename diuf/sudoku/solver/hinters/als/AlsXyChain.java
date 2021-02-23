/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Implements the ALS-XY-Chain (aka ALS-Chain) Sudoku solving technique.
 * I extend abstract AAlsHinter implements getHints to call my findHints.
 *
 * @author Keith Corlett 2020 May 24
 */
public final class AlsXyChain extends AAlsHinter {

	/**
	 * Returns Is 'als' and superset or subset of any ALS in 'chain', 
	 * stopping at 'n' (inclusive).
	 * <p>
	 * Note: chain is populated upto {@code <= n};<br>
	 * not the more usual {@code < n};<br>
	 * because chain[0] is the startAls.
	 */
	private static boolean isSuperSubset(Als als, Als[] chain, int n) {
		try {
			for ( int i=0; i<=n; ++i )
				if ( Idx.andEqualsEither(chain[i].idx, als.idx) )
					return true;
		} catch (Throwable eaten) { // esp NPE and AIOOBE
			return true; // if in doubt, chuck it out. Try the oysters!
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
	 * Constructs a new HdkAlsXyChain hinter.
	 * <p>
	 * nb: I use my supers valid method, which sets the solutionValues array
	 * using the (brute-force) solve method of the given logicalSolver.
	 */
	public AlsXyChain() {
		// Tech, allowLockedSets, findRCCs, allowOverlaps, forwardOnly, useStartAndEnd
		super(Tech.ALS_Chain
			, false // suppress lockedSets: in getAlss, no ALS may contain a
				    // cell which is part of any Locked Set in the ALSs region.
					// Allowing a cell that is part of an actual Locked Set in
					// this region to be part of an Almost Locked Set produced
					// invalid hints, so KRC introduced there supression.
			, true  // findRCCs: true runs getRccs as per "normal" ALS use.
					// this is only ever false in DeathBlossom.
			, false // suppress overlaps: ALSs which physically overlap are
					// not allowed to form a RCC (a connection). Allowing
					// overlaps in ALSs to form an ALS-Chain produces
					// invalid hints, so KRC supressed them.
			, false // forwardOnly: false makes getRccs do a full search of
					// all possible combinations of ALSs, instead of the
					// faster forwardOnly search for XZ's and XyWing's.
			, true	// useStartAndEnd: true makes getRccs populate startIndices
				    // and endIndices arrays. Chaining uses them as a map.
		);
	}

	@Override
	protected boolean findHints(Grid grid, Idx[] candidates
			, Rcc[] rccs, Als[] alss, IAccumulator accu) {
		this.grid = grid;
//		this.candidates = candidates;
		this.rccs = rccs;
		this.alss = alss;
		this.accu = accu;
		this.oneOnly = accu.isSingle();
		boolean result = false;
		try {
			deletesMap.clear();
			final int n = alss.length;
			for ( int i=0; i<n; ++i ) {
				chainAlss[0] = startAls = alss[i];
//				chainIndex = 0;
				if ( isAlsInChain==null || isAlsInChain.length<n )
					isAlsInChain = new boolean[n];
				else
					Arrays.fill(isAlsInChain, false);
				isAlsInChain[i] = true;
				firstRCC = null;
				result |= recurseChains(i, null, 0);
				if ( result && oneOnly )
					break; // we found one already!
			}
		} finally {
			this.grid = null;
//			this.candidates = null;
			this.rccs = null;
			this.alss = null;
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
//	/** The index into {@link #chainRccs} for the current search. */
//	private int chainIndex = -1;
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
	 * @param index index of the RCC to search (left) in startIndices/endIndices
	 *  arrays, not the index of the RCC itself.
	 * @param prevRC RC of the previous step (needed for adjacency check)
	 */
	private boolean recurseChains(final int index, final Rcc prevRC, int chainIndex) {
		Rcc rcc; // the current Restricted Common Candidate
				 // common to two ALSs: s=startAls (left) and a=currAls (right)
		Als a; // the current ALS
		int arc, rc1,rc2,rc3,rc4, i,I, als2, zMaybes;
		boolean any;
		final Idx tmp = this.set1; // used for various checks
		final Idx zBuds = this.set2; // all buds of v (incl the ALSs).
		final Pots reds = this.redPots;
		final Pots blues = this.bluePots;
		final Als s = this.startAls;
		final Als[] chainAlss = this.chainAlss;
		final int[] startIndices = this.startIndices;
		final int[] endIndices = this.endIndices;
		final Rcc[] rccs = this.rccs;
		final Als[] alss = this.alss;
		final boolean[] isAlsInChain = this.isAlsInChain;
		final boolean allowOverlaps = this.allowOverlaps;
		// check all RCs; if none exist the loop is never entered
		boolean firstTry = true;

		// presume that no hint will be found
		boolean result = false;

		// foreach currAls (right) in ALS's connecting with startAls (left).
		// nb: the "map" between ALSs is stored in startIndices and endIndices.
		// nb: index is into start/endIndices arrays, not the index of an RCC.
		for ( i=startIndices[index],I=endIndices[index]; i<I; ++i ) {
			rcc = rccs[i];
			// check the RC (sets the rcc.actualRC for later getActualRC())
			if ( rcc.checkRC(prevRC, firstTry)
			  // check ALS not already in chain
			  && !isAlsInChain[als2=rcc.getAls2()]
			  // get 'a' the current ALS, and check for common buds.
			  && !Idx.andEmpty(s.buddies, (a=alss[als2]).buddies)
			  // 'a' can not be a super/subset of any in chainAlss<=chainIndex
			  // even if allowOverlaps
			  && !isSuperSubset(a, chainAlss, chainIndex)
			) {
				// ok, currALS can be added
				if ( chainIndex == 0 )
					firstRCC = rcc;
				// give-up when chain full (currently at 5).
				// I've never seen a hint from more than 5, which doesn't mean
				// they can't exist, only that I dont have any, so they must be
				// rare, so it's not worth looking any further.
				// NB: hobiwan had MAX_RCCs=32, which seems extreme!
				else if ( chainIndex+2 > MAX_RCCs ) //ie chainIndex+1>=MAX_RCCs
					break; // chain full (Never happens. Never say never.)

				// nb: chainAlss[0]=startALS and 1..chainIndex INCLUSIVE=currAls's
				// nb: from here down: to continue we also decrement chainIndex,
				// otherwise the currALS stays in the chain even though it's been
				// skipped, so that subsequent recursions include it.
				// nb: Hobiwan used chainRccs, which was nonsensical when it was
				// always used to access the ALS's in those RCC's, so just store
				// the bloody ALS's: that way we can put them ALL in the array
				// including the startALS, to simplify the reader code.
				chainAlss[++chainIndex] = a;
				isAlsInChain[als2] = true;

				// if there's at least 4 ALSs then check for eliminations
				// nb: 4 ALSs = 3 in the chain + the startAls, called s.
				if ( chainIndex > 2 ) {
					// get the four RC values from the two RCCs
					arc = firstRCC.getActualRC();
					if(arc==2) rc1 = 0; else rc1 = firstRCC.getCand1();
					if(arc==1) rc2 = 0; else rc2 = firstRCC.getCand2();
					switch ( rcc.getActualRC() ) {
					case 1: rc3 = rcc.getCand1(); break;
					case 2: rc3 = rcc.getCand2(); break;
					case 3: rc3 = rcc.getCand1(); break;
					default: rc3 = 0; break; // Never happens. Never say never.
					}
					rc4 = rcc.getCand2();
					// The RC "none" value has been confused: it's 0 (not -1)
					// because ~0 is 32 set bits, a no-op in below &'s
					assert rc1!=-1 && rc2!=-1 && rc3!=-1 && rc4!=-1;
					// get maybes common to both ALSs except the RCs,
					// and check that there's at least one.
					if ( (zMaybes=s.maybes & a.maybes & ~VSHFT[rc1]
							& ~VSHFT[rc2] & ~VSHFT[rc3] & ~VSHFT[rc4]) != 0
					  // check overlaps: RCs already done, so just s and a
					  && (allowOverlaps || Idx.andEmpty(s.idx, a.idx))
					) {
						// examine each maybe common to both ALSs except RCs
						any = false;
						for ( int z : VALUESES[zMaybes] )
							// get cells which see all z's in both ALSs
							if ( zBuds.setAnd(s.vBuds[z], a.vBuds[z]).any() ) {
								// zBuds are eliminated (red)
								zBuds.forEach(grid.cells
									,  (c) -> reds.upsert(c, z)
								);
								// z's are fins (blues) (display only)
								tmp.setOr(s.vs[z], a.vs[z]).forEach(grid.cells
									, (c) -> blues.upsert(c, z)
								);
								any = true;
							}
						if ( any ) {
							if ( HintValidator.ALS_USES ) {
								// check that each elimination isn't in the solution. Sigh.
								if ( !HintValidator.isValid(grid, reds) ) {
									HintValidator.report(
										  this.getClass().getSimpleName()
										, grid
										, Als.linkedList(chainIndex, chainAlss)
									);
//									// I know I'm not on top of invalid hints so I report
//									// and continue, to see all the invalid hints, so that
//									// I can get on top of them.
//									// back this ALS out of the chain
//									--chainIndex;
//									// clear the pots, else they all go bad from here on.
//									redPots.clear(); bluePots.clear();
//									// for now just skip hint with invalid elimination/s
//									continue;
									// I think I'm on top of invalid hints so I die-early
									StdErr.exit(StdErr.me()+": invalid hint", new Throwable());
								}
							}

							//
							// ALS-XY-Chain found! so build the hint and add it to accu
							// nb: All DIUF types below (keeping it all HoDoKu above).
							//

							// we start with some lists of stuff
							List<ARegion> bases = new LinkedList<>();
							List<ARegion> covers = new LinkedList<>();
							List<Idx> setsList = new LinkedList<>();
							List<Als> alssList = new LinkedList<>();
							// chainAlss[0]=startAls and 1..chainIndex INCLUSIVE=currAlss
							for ( int j=0,J=chainIndex; j<=J; ++j ) {
								Als x = chainAlss[j];
								if ( j%2==0 )
									bases.add(x.region);
								else
									covers.add(x.region);
								setsList.add(x.idx);
								alssList.add(x);
							}

							// nb: blue overwrites everything incl red; some reds are
							// in other ALSs, so remove reds from blues so they're red!
							blues.removeAll(reds);

							// debugMessage is normally blank (so you don't see anything)
//							String debugMessage = invalidity.isEmpty() ? "" : "<br><h2>"+invalidity+"</h2>";
							String debugMessage = "";

							// build the hint
							AHint hint = new AlsXyChainHint(
								  this
								, new Pots(reds) // copy-off the field!
								  // remove fins and reds from oranges.
								, oranges(grid, setsList).removeAll(blues).removeAll(reds)
								, new Pots(blues) // copy-off the field!
								, bases
								, covers
								, alssList
								, debugMessage
							);

							// clear the collection fields for the next iteration.
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
				if ( chainIndex+1 < MAX_RCCs ) // MAX_RC is currently 5
					result |= recurseChains(als2, rcc, chainIndex);
				// else no space left so we stop recursing and bail out

				// and back one level
				if ( result && oneOnly )
					return true; // and-so-on all the way back-up the recursive callstack!
				isAlsInChain[als2] = false;
				chainAlss[chainIndex--] = null;

				if ( prevRC == null ) {
					// this is the first RC in the chain
					if ( rcc.getCand2()!=0 && firstTry ) {
						// and a second RC value is present, so back-up to try
						// again with the second RC value of the current RCC
						firstTry = false;
						--i;
					} else
						firstTry = true;
				}
			}
		} // next RCC // next RCC
		return result;
	}
	// Maximum number of RCCs in an ALS-Chain (forward-only search).
	// * 5 is just the longest chain which produces a hint in top1465. It may
	//   be possible to hint from longer chains.
	// * hobiwans MAX_RCCs was 32, which I think is "over the top".
	private static final int MAX_RCCs = 5;
	// ALS's in current Chain.
	// * chainAlss[0] is always the startAls, 1..chainIndex are currAls's
	private final Als[] chainAlss = new Als[MAX_RCCs];

	// I create these variables ONCE rather than in each recursion
	private final Idx set1 = new Idx();
	// all buds of v (including the ALSs).
	private final Idx set2 = new Idx();
	// don't forget to copy-them-off to create a hint, then clear both!
	private final Pots redPots=new Pots(), bluePots=new Pots();

}
