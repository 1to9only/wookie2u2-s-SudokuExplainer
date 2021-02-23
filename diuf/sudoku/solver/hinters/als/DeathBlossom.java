// ============================================================================
// KRC 2021-01-06 DeathBlossom is broken so commented out.
// My death blossom implementation doesn't work: it reports hundreds of false
// positives (invalid hints). I've look through the code and cannot see what
// I've done wrong. I boosted this code from HoDoKu, which doesn't use it and
// now know why: Hobiwan wasn't inclined to do things like leave bread crumbs,
// so I strongly suspect that my source implementation was buggered; and with
// no other information on what this technique is SUPPOSED to do, I can only
// give up in frustration.
// ============================================================================
// KRC 2021-01-07 DeathBlossom still broken, but I'm still swinging.
// I tried eliminating what looked like possibly dodgy hints to me. Things like
// all ALS's in a DeathBlossom being the same ALS, then not being supersets of
// each other, then not being in the same region, which would also do both of
// the above if I was being efficient, and finally I still can't get rid of all
// of the invalid hints, and I'm out of ideas for today atleast, and 06:00 till
// 16:00 is a decent working day, so I give up for today. Try again tomorrow.
// ============================================================================
// KRC 2021-01-08 DeathBlossom is a first class pain in the ass.
// I have not yet eliminated ALL invalid hints, but I've set things up so that
// DeathBlossom is disabled when it dead-cats (produces the same zero elim hint
// twice) and is re-enabled in prepare, for the next puzzle. Dead-cat disabling
// effects ALL hinters, but none of the others currently dead-cat. All hinters
// which ever dead-cat must be fixed. I'm attempting to fix this one, which
// necessitates running the bastard, so I must swerve around the dead-cats.
// ============================================================================
// KRC 2021-01-09 DeathBlossom still incomprehensibly rooted! It's about here
// that I give up. Maybe one day I'll research it, but commented-out for now.
// I found a few things. No ALS's which contain LockedSets. No ALS may contain
// any other in a DB, and no two ALS's may even be in the same region (I think)
// but still it's producing MANY invalid hints, so I officially give-up until
// I can research it.
//
///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2020 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.solver.hinters.als;
//
//import diuf.sudoku.*;
//import diuf.sudoku.Grid.*;
//import diuf.sudoku.solver.AHint;
//import diuf.sudoku.solver.LogicalSolver;
//import diuf.sudoku.solver.accu.IAccumulator;
//import diuf.sudoku.solver.hinters.HintValidator;
//import diuf.sudoku.utils.Debug;
//import diuf.sudoku.utils.Log;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.BitSet;
//import java.util.List;
//import java.util.Set;
//
///**
// * DeathBlossom implements the Death Blossom solving technique.
// *
// * @author Keith Corlett 2020-01-05
// */
//public class DeathBlossom extends AAlsHinter
//		implements diuf.sudoku.solver.IPreparer
//{
//
//	// Is als a superset of any myAlss, or any myAlss a superset of als?
//	// Ie: Do any two ALS's contain the same values as each other?
//	private boolean isSuperset(int[] myAlss, Als als) {
//		for ( int ai : myAlss )
//			if ( ai>-1 && Idx.andEqualsEither(alss[ai].idx, als.idx) )
//				return true;
//		return false;
//	}
//
//	// Is als in the same region as any myAls?
//	private boolean sameRegions(int[] myAlss, Als als) {
//		for ( int ai : myAlss )
//			if ( ai>-1 && alss[ai].region == als.region )
//				return true;
//		return false;
//	}
//
//	/**
//	 * Restricted Common Candidates for Death Blossom. Joints between ALS's.
//	 * <p>
//	 * Each RCDB instance belongs to an empty grid cell: Holds the indexes of
//	 * all ALS in {@link #alss} which see this cell. The list is maintained
//	 * for all candidates 1..9; if two candidates from one ALS can see the same
//	 * cell, then the ALS is recorded twice. The cands field is a bitset
//	 * containing all the candidates for which an ALS has been added.
//	 * <p>
//	 * Once fully populated, if my cands bitset equals my cell.maybe.bits, then
//	 * I'm a stem cell for a Death Blossom, ie all my potential values are
//	 * involved in at-least one almost locked set (ALS).
//	 */
//	static class RCDB {
//		/** A bitset of each value 1..9 with at least one ALS. */
//		int cands;
//		/** The ALSs for each value 1..9. */
//		int[][] alss = new int[10][ALSS_PER_CANDIDATE];
//		/** The number of {@link #alss} stored for each value 1..9. */
//		int[] numAlss = new int[10];
//		/**
//		 * Adds an ALS for candidate.
//		 * @param als
//		 * @param v candidate value
//		 */
//		public void addAls(int als, int v) {
//			if ( numAlss[v] < ALSS_PER_CANDIDATE ) {
//				alss[v][numAlss[v]++] = als;
//				cands |= VSHFT[v];
//			}
//		}
//		/**
//		 * Returns the largest candidate value which has ALS/s.
//		 * @return the largest candidate value which has ALS/s.
//		 */
//		public int maxCand() {
//			int max = 0;
//			for ( int v=1; v<10; ++v )
//				if ( numAlss[v] > 0 )
//					max = v;
//			return max;
//		}
//	}
//
//	/** The maximum number of ALSs per candidate. */
//	private static final int ALSS_PER_CANDIDATE = 100;
//
//	/**
//	 * Should overlapping ALS's be allowed (true) or suppressed (false).
//	 * ========================= !!!! READ THIS !!!! ==========================
//	 * With ALLOW_OVERLAPS = false DeathBlossom finds 0 hints in top1465, but
//	 * with ALLOW_OVERLAPS = true DeathBlossom finds LOTS of invalid hints!
//	 * I'm attempting to differentiate the valid from the invalid, but I simply
//	 * don't understand the crap well enough to form an intelligent opinion, so
//	 * I log more info, for all hints, and run on top1465.F10.mt.
//	 * ========================================================================
//	 */
//	private static final boolean ALLOW_OVERLAPS = true;
//
//	/** An {@link RCDB} for each cell in the Grid. Empty cells get populated. */
//	private final RCDB[] rcdbs = new RCDB[81];
//	/** RCDB of stem cell that is currently being checked. */
//	private RCDB rcdb = null;
//	/** The current stem cell in Death Blossom search. */
//	private Cell stemCell;
//	/** The indice of the current stem cell in Death Blossom search. */
//	private int stemIndice = 0;
//	/** All indices of all ALS for a given stem cell (for recursive search). */
//	private final Idx idx = new Idx();
//	/** overlap between the new ALS and the existing ALS's in this DeathBlossom. */
//	private final Idx overlap = new Idx();
//	/** All common candidates in the current combination of ALS. */
//	private int cands = 0;
//	/** The common candidates that were reduced by the current ALS. */
//	private final int[] otherCands = new int[10];
//	/** The indices of all ALS in the current try of the Death Blossom search. */
//	private final int[] myAlss = new int[10];
//	/** The indices of all ALS's in a DB for a given candidate (v). */
//	private final Idx dbCc = new Idx();
//	/** The largest candidate to search for DeathBlossom. */
//	private int maxCand = 0;
//	/** accu.isSingle() for speed and convenience. */
//	private boolean onlyOne;
//	/** The Grid to search. */
//	private Grid grid;
//	/** An Idx for each value 1..9 containing the indices of cells which maybe value. */
//	private Idx[] candidates;
//	/** The IAccumulator to add hints to. */
//	private IAccumulator accu;
//	/** The ALS's (Almost Locked Sets) to search. */
//	private Als[] alss;
//	/** A general purpose idx. */
//	private final Idx cmnBuds = new Idx();
//	/** The removable (red) Cell=>Values. */
//	private final Pots theReds = new Pots();
//	/** A multi-purpose Idx */
//	private final Idx tmpIdx = new Idx();
//
//	public DeathBlossom() {
//		// construct my super-class AAlsHinter
//		// tech, allowOverlaps, allowLockedSets, findRCCs, forwardOnly, useStartAndEnd
//		super(Tech.DeathBlossom, true, false, false, UNUSED, UNUSED);
//	}
//
//	@Override
//	public void prepare(Grid grid, LogicalSolver logicalSolver) {
//		super.prepare(grid, logicalSolver);
//		// re-enable me, in case I went dead-cat in the last puzzle
//		setIsEnabled(true); // use the setter, in case it does other stuff.
//	}
//
//	/**
//	 * Collect all Restricted Commons for Death Blossom.
//	 * <p>
//	 * Find cells that see all instances of a candidate within an ALS.
//	 * Each cell without an existing {@link #rcdbs} entry has one created.
//	 * Existing rcdb's are added to; info is never removed or changed.
//	 * Upto ALSS_PER_CANDIDATE ALS's may be stored per cell-candidate-value.
//	 * Later {@code rcdb[i].cands} is checked against cell.maybes: if they
//	 * are equal, then this cell is a "stem" for a possible Death Blossom.
//	 * <p>
//	 * Calculate all buddies for all candidates in all ALSs -> they are all
//	 * possible stem cells.
//	 */
//	private void collectRCDBs(Als[] alss) {
//		// initialize rcdb
//		Arrays.fill(rcdbs, null);
//		// foreach ALS
//		for ( int i=0,n=alss.length; i<n; ++i ) {
//			Als als = alss[i];
//			final int alsIndex = i; // for lambda
//			// foreach candidate in the current ALS
//			for ( int v : VALUESES[als.maybes] ) {
//				// foreach indice of cell in ALS which maybe v
//				als.vs[v].forEach1((indice) -> {
//					if ( rcdbs[indice] == null )
//						rcdbs[indice] = new RCDB();
//					rcdbs[indice].addAls(alsIndex, v);
//				});
//			}
//		}
//	}
//
//	/**
//	 * Finds first/all Death Blossoms in the given Grid.
//	 *
//	 * Searches for all available Death blossoms: if a cell exists that
//	 * has at least one ALS for every candidate check all combinations of
//	 * available ALS for that cell. Any combination of (non overlapping) ALS
//	 * has to be checked for common candidates that can eliminate candidates
//	 * outside the ALS and the stem cell.<br><br>
//	 * @return any hint/s found?
//	 */
//	@Override
//	public boolean findHints(Grid grid, Idx[] candidates
//			, Rcc[] rccs, Als[] alss, IAccumulator accu) {
//		if ( !isEnabled )
//			return false; // I was called despite being disabled!
//		assert rccs == null; // I find my own RCC's (rcdbs).
//		this.grid = grid;
//		this.candidates = candidates;
//		this.accu = accu;
//		this.onlyOne = accu.isSingle();
//		this.alss = alss;
//		boolean result = false;
//		try {
//			// rcdbs = The ALS/cell combos
//			collectRCDBs(alss);
//			// foreach empty cell in the grid
//			for ( Cell cell : grid.getBitIdxEmpty() ) {
////if ( "C7".equals(cell.id) )
////	Debug.breakpoint();
//				if ( (rcdb=rcdbs[cell.i]) != null
//				  && rcdb.cands == cell.maybes.bits ) {
//					// this is a stem cell, so look for a Death Blossom (DB).
//					stemCell = cell;
//					stemIndice = cell.i;
//					maxCand = rcdb.maxCand();
//					idx.clear();
//					cands = Values.ALL_BITS;
//					Arrays.fill(myAlss, -1);
//					overlap.clear();
//					// search, starting with candidate value = 1
//					// try all combinations of ALS's
//					if ( recurseAlss(1) ) {
//						result = true;
//						if(onlyOne) return result;
//					}
//				}
//			}
//		} finally {
//			this.grid = null;
//			this.alss = null;
//			this.accu = null;
//		}
//		return result;
//	}
//
//	/**
//	 * Recursively tries each ALS for {@code cand} in cell {@link #stemIndice}.
//	 * If {@code cand==maxCand} then check all possible eliminations.
//	 * <p>
//	 * Speed: Predeclare all vars (ANSI-C style) to minimise stack-work.
//	 * Do NOT use continue, it slows down by a factor of about 5. IDKFA!
//	 * @param cand the candidate value
//	 * @param onlyOne true if only one hint is sought
//	 * @return any hint/s found?
//	 */
//	private boolean recurseAlss(int cand) {
//		if ( cand > maxCand )
//			return false; // exhausted (Never happens, never say never)
//		final int n = rcdb.numAlss[cand];
//		boolean result = false;
//		if ( n == 0 ) {
//			// cand not in this DeathBlossom, so try the next candidate
//			if ( recurseAlss(cand+1) ) {
//				result = true;
//				if(onlyOne) return result;
//			}
//		} else {
//			int ai;
//			Als als;
//			// there are ALS's to examine under this candidate
//			ALS: for ( int i=0; i<n; ++i ) {
//				// get the ALS and check it has common candidate/s
//				ai = rcdb.alss[cand][i];
//				als = alss[ai];
//				if ( (als.maybes & cands) != 0
////<KRC comment="These contraints are KRC inventions. They may be WRONG!">
//				  // and no two ALS's may reside in the same region!
//				  && ( !sameRegions(myAlss, als) )
//				  // and no two ALS's may contain one-other
//				  && ( !isSuperset(myAlss, als) )
//				  // and ALS has 2+ positions for cand
//				  && ( als.vs[cand].size() > 1 )
////</KRC>
//				) {
//					// or-in the intersection of this ALS with existing ones,
//					// then later remove from overlap after this ALS processed.
//					overlap.orAnd(idx, als.idx);
//					// check for NO overlap (if supressed)
//					if ( ALLOW_OVERLAPS || !overlap.any() ) {
//						// ALS is valid: common candidates exist && overlap OK.
//						// myAlss = ALS's in DeathBlossom: empty pre round1 and
//						// I populate by calling myself for each cand; so in da
//						// below "complete ALS combination" I contain all the
//						// ALS's in this DeathBlossom, indexed by cand.
//						myAlss[cand] = ai;
//						// get existing cands except those in this ALS, to
//						// restore them to cands after processing this ALS.
//						otherCands[cand] = cands & ~als.maybes;
//						// get the maybes common with the existing ALS's
//						cands &= als.maybes;
//						// add indices of the new ALS to this DeathBlossom
//						idx.or(als.idx);
//						if ( cand < maxCand ) {
//							// examine the next candidate value, recursively
//							if ( recurseAlss(cand+1) ) {
//								result = true;
//								if(onlyOne) return result;
//							}
//						} else {
//							// a complete ALS combination: check eliminations
//							int foundCands = 0; // found checkCands bitset
//							for ( int checkCand : VALUESES[cands] ) {
//								if ( myAlss[checkCand] == -1 ) {
//									// checkCand is possible stemCell
//									// dbCc=cells in DB which maybe checkCand
//									dbCc.clear();
//									for ( int ia : myAlss )
//										// skip non-maybes incl checkCand
//										if ( ia != -1 )
//											// dbVs+=ALS cells maybe checkCand
//											dbCc.or(alss[ia].vs[checkCand]);
//									if ( dbCc.any() ) {
//										// cmnBuds=cells which see ALL cells
//										// in DB which maybe checkCand.
//										Grid.cmnBuds(dbCc, cmnBuds);
//										// which maybe checkCand themselves
//										if ( cmnBuds.and(candidates[checkCand]).any() ) {
//											// no cannibalism
//											cmnBuds.andNot(idx);
//											// not in the stemCell
//											cmnBuds.remove(stemIndice);
//											// not in the stemCell's regions!
//											for ( ARegion r : stemCell.regions )
//												cmnBuds.andNot(r.idx);
//											// possible eliminations?
//											if ( cmnBuds.any() ) {
//												// Yeah! Death Blossom Found!
//												// record elims for checkCand
//												cmnBuds.forEach1((ci) -> {
//													Cell c = grid.cells[ci];
//													theReds.upsert(c, checkCand);
//												});
//												// remember we found this checkCand
//												foundCands |= VSHFT[checkCand];
//											}
//										}
//									}
//								}
//							}
//							if ( foundCands != 0 ) {
//								// each elim must see all v's in DB, or can it.
//								for ( Cell c : theReds.keySet() )
//									for ( int v : theReds.get(c) )
//										for ( int db : tmpIdx.setAnd(idx, candidates[v]).toArrayA() )
//											if ( c.notSees[db] )
//												return false;
//								// there SHOULD be no illegals to clean-out!
//								if ( !theReds.clean() ) {
//									// no eliminations remaining
//									Log.teeln("DEAD_CAT_BAIT!");
//									return false;
//								}
//								// copy theReds and clear them for next time
//								Pots reds = new Pots(theReds);
//								theReds.clear();
//								// eliminations found: create the hint.
//								AHint hint = createHint(reds);
//								// validate the hint
//								if ( HintValidator.DEATH_BLOSSOM_USES ) {
//									if ( !HintValidator.isValid(grid, hint.redPots) ) {
//										hint.isInvalid = true; // @toString
//										HintValidator.report(tech.nom, grid, hint.toFullString());
//										// ignore in batch; display in GUI where
//										// hintsTree delete-key removes hints &
//										// disables hinter, which is re-enabled
//										// by the next load puzzle (prepare).
//										if ( Run.type != Run.Type.GUI )
//											return false; // no hint here! sigh.
//									}
//								}
//								// add the hint to the accumulator
//								result = true;
//								if ( accu.add(hint) )
//									return result;
//							}
//						}
//					}
//					// and back again
//					cands |= otherCands[cand];
//					idx.andNot(als.idx);
//					overlap.andNotAnd(idx, als.idx);
//				}
//			}
//			// nothing to do -> examine the next candidate value, recursively
//			myAlss[cand] = -1;
//		}
//		return result;
//	}
//
//	private AHint createHint(Pots reds) {
//		// create all the hint attributes, then the hint
//		// The green (background and maybes color) is the stem cell
//		Pots greens = new Pots();
//		Cell stem = grid.cells[stemIndice];
//		greens.put(stem, new Values(stem.maybes));
//		// the blues (fins) are the value we used in each ALS to form our DB
//		final Pots blues = new Pots();
//		// ... and I also build a string of all the ALS's
//		StringBuilder sb = new StringBuilder(1024);
//		// ... and I also collect there regions (a Set for uniqueness)
//		BitSet regionsSet = new BitSet(27);
//		for ( int v=1; v<10; ++v )
//			if ( myAlss[v] != -1 ) {
//				Als als = alss[myAlss[v]];
//				final int value = v; // for lambda
//				als.vs[v].forEach1((ci) ->
//					blues.upsert(grid.cells[ci], value)
//				);
//				blues.put(stem, new Values(v)); // stem not in als.vs
//				sb.append(v).append(" in ").append(als).append(NL);
//				regionsSet.set(als.region.index);
//			}
//		// read the regionsSet into an array
//		List<ARegion> regions = toList(regionsSet);
//		// the pink-background cells are the overlaps between ALS's,
//		// WARN: always seems to contain extranious cells! WTF!
//		// Idx.cells(Grid) where BitIdx needs Grid to
//		// simulate a Set<Cell> with a BitSet. sigh.
//		Set<Cell> pinkCells = overlap.toBitIdx(grid);
//		return new DeathBlossomHint(this, reds
//				, greens , blues, pinkCells
//				, sb.toString(), regions, grid);
//	}
//
//	// Read the BitSet into a List of ARegions, by index.
//	private List<ARegion> toList(BitSet rs) {
//		List<ARegion> result = new ArrayList<>(rs.cardinality());
//		for ( int r=rs.nextSetBit(0); r>-1; r=rs.nextSetBit(r+1) )
//			result.add(grid.regions[r]);
//		return result;
//	}
//
//}
