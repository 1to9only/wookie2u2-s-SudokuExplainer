/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.IO;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Aligned Triple Exclusion technique.
 * <p>Implements the Aligned Set Exclusion Sudoku solving technique for sets
 * of 3 Cells. This is coded to run quickly, not to look good, or impress peeps.
 * <p>The Juillerat implementation was slow and difficult to understand. This
 * one is even bloody harder to follow, but is substantially faster.
 * <p>We try to avoid calling methods inside "tight loops". A "tight loop" is
 * one at the bottom of several levels of iteration, which tends to mean that
 * it'll be "hammered" (ie executed repeatedly, possibly billions of times).
 * We also try to avoid creating variables inside tight loops. It's only a
 * few ops, but multiply anything by 42,000,000,000 and it gets BIG fast.
 * <p>All variables, especially collections, are created once, at top of method.
 * Likewise, we break all loops as soon as possible. Short-circuit wherever
 * possible. Call no methods, especially unnecessarily.
 * <p>Collections are sized to avoid repeatedly growing underlying arrays. As a
 * general rule initialCapacity should cover 90% of cases, except if 100% isn't
 * too big. Increased size means slower CONSTRUCTION but decreased add times,
 * with no O(n) growth delay. A HashMap is just an array of linked-lists. The
 * bigger the array then the shorter the linked-lists, and the more bits we have
 * in which to create a distinctive (collisionless) hashCode.
 * <p>The {@code Map<Cell, Collection<Cell>> cellExcluders} variable in the
 * Juillerat implementation is now a fast/simple simultaneous array.
 * The retainAll method hammers the contains method, and contains in CellSet
 * (ie LinkedMatrixCellSet) is an O(1) operation verses ArrayLists O(n).
 * <p>That "O notation" is short for "in the order of" which a measure of
 * algorithmic efficiency. Basically all you need to know is that "1" is a
 * f__k-of-a-lot smaller than "n", which implies "a f__k-of-a-lot faster".
 * If you're really interested in this stuff then please Wikipedia it.
 * <p>The {@code List<int[]> allowedPotentialCombinations} of the previous
 * implementation has been replaced with a virtual array of left-shifted bitsets
 * (called avb for allowedValuesBitsets). This is acceptable we don't care which
 * combos allowed each value, we just need to know if the value is allowed in
 * this position of ANY combo; so stashing them in a bitset turns the O(n^2)
 * look-up (ie distant monarch slow) into an O(n) operation (ie acceptable).
 * <p>I tried making most of the variables fields so that we didn't need to
 * push/pop anywhere near as much stuff onto/off the call-stack, which I thought
 * might be a bit quicker, but accessing a field takes longer than a local, so
 * it turned out to be just the sound of one hand wanking.
 * <p>KRC 2019-09-11 AlignedSetExclusion becomes AAlignedSetExclusionBase and
 * modified A3E, A4E, and A5E to use its protected methods, to mostly-clean-up
 * the nasty cut-and-paste coding. Also moved A5E to just before DynamicPlus in
 * the LogicalSolver chainers list. It's still too slow, but it doesn't matter.
 * <p>KRC 2019-11-13 I've been masticating A*E for months now, and it's a lot
 * faster than it used to be, but still too slow, especially for 5, 6 and 7.
 * <p>KRC 2019-11-29 isNotSiblingOf array. Loop while sv &lt;= maybes-bits.
 * <pre>
 * 2019-11-13 A3E 1465 in 154,547,077,612 (02:34) @ 105,492,885 ns/puzzle
 * 2019-11-29 A3E 1465 in 149,701,028,655 (02:29) @ 102,185,002 sv &lt;= maybes
 * </pre>
 */
public final class Aligned3Exclusion extends AAlignedSetExclusionBase
//		implements diuf.sudoku.solver.IReporter
//				 , java.io.Closeable
{
	// the minimum number of candidates to process.
	private static final int MIN_CANDIDATES = 22; // <HACK/>

	// the maximum number of candidates to process.
	private static final int MAX_CANDIDATES = 59; // <HACK/>

	private static final int NUM_CMN_EXCLS = 16;
	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[3];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2

	public Aligned3Exclusion() {
		super(Tech.AlignedTriple, null, IO.A3E_HITS);
		assert tech.isAligned;
		assert degree == 3;
	}

//	@Override
//	public void close() throws java.io.IOException {
//		hits.save();
//	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The Aligned3Exclusion class implements the aligned set exclusion
	 * Sudoku solving technique for sets of 3 cells (ie a triple).
	 * <p>We're going to look for all the candidate cells, each with a set of
	 * excluder cells: ie it's siblings with 2..3 maybes. Then we're going to
	 * take each distinct triple of candidate-cells, and (virtually) throw them
	 * on the grid to see if two of there excluder-cells line-up (ie are
	 * concurrent); and if so then we go into the dog____ing loop which iterates
	 * through all possible combinations of the maybes (potential values) of our
	 * 3-cells, to work-out if each value is allowed in each cell. If all maybes
	 * are allowed then there's "no hint here", so we go onto the next triple.
	 * <p>Otherwise (atleast one value is excluded) we calculate the removable
	 * (red) potential values:<pre>
	 *     redValues = cell.maybes - allowedValuesOfThisCell;
	 * </pre> and then we run back into that dog____ing loop again except this
	 * time there's no performance worries because we know that we're only going
	 * to execute it ?1000? times in top1465. So foreach excluded permutation
	 * of potential values we add an entry to the excludedCombosMap: combo to
	 * the optional locking/excluding cell. If the locking cell is null it just
	 * means "one occurrence of value per region". We need the excludedCombosMap
	 * to build the hint for display to the user.
	 * <p>Counter-intuitively, doing a dog____ing loop TWICE is more efficient
	 * because its hit rate is so low that it's actually cheaper to first do the
	 * absolute minimum amount of work required to figure out if we need to come
	 * back and actually do the bloody work; rather than the "normal" approach
	 * of just doing the work on the presumption that it'll be used; which
	 * mixes nicely with computers getting faster, so the "wasteligate" approach
	 * becomes totally normalised because it's almost allways adequate, ie it's
	 * only in this sort of algorithm where iterations come in BILLIONS and the
	 * hit rate is so low that our "normalised waste" becomes onerous.
	 * <b>DISCLAIMER:</b> The author does not condone canidamorism. We prefer
	 * sheep, apparently. Ask a Kiwi! Well what the hell would YOU call the
	 * loop? It's named to stick. Pun intended.
	 * <p>KRC 2019-10-02 abort when max-numCandidates is exceeded.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {

		// HintsApplicumulator will bugger-up the arrayonateShiftedMaybes()
		// because all the cell.maybesArray's will go out of date.
		assert !(accu instanceof HintsApplicumulator);

		// localise this variable for speed (and make it final).
		// hackTop1465 is isHacky && filePath.contains("top1465")
		final boolean hackTop1465 = AHinter.hackTop1465;
		// istop1465 is just the filePath.contains("top1465")
		// I use it to filter when not isHacky, otherwise takes 34:16:04
		// for top1465, and I'm not waiting that long again. It sux!
		final boolean isTop1465 = grid.source!=null && grid.source.isTop1465;
		// grid.source.lineNumber ie which puzzle are we currently solving
		final int gsl = grid.source!=null ? grid.source.lineNumber : 0;

		// localise fields for speed (if it's referenced more than three times)
		final int degree = this.degree;
		final int hintNum = grid.hintNumber;
		final boolean useHits = this.useHits;

		// get an array of the Cells at which we hinted last time;
		// otherwise we skip this call to getHints
		final Cell[] hitCells = useHits // only true when AHinter.hackTop1465
				? hits.getHitCells(gsl, hintNum, degree, grid)
				: null;
		final boolean hitMe = hitCells != null;
		if ( useHits && !hitMe )
			return false; // no hints for this puzzle/hintNumber

		// The populate populateCandidatesAndExcluders fields: a candidate has
		// maybes.size>=2 and has 1 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = CANDIDATES_ARRAY;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// an array of the cells in each aligned set. Note that the array is
		// reused, not recreated for each aligned set.
		final Cell[] cells = CELLS_ARRAY;

		// the number of cells in each for-i-loop
		final int n0, n1;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = COMMON_EXCLUDERS_ARRAY;
		// the common-excluder-cells-maybes-bits. This set differs from the
		// common-excluder-cells in that supersets & disjuncts are removed
		// from cmnExclBits WITHOUT modifying cmnExcls; which is required
		// "as is" to build the hint.
		final int[] cmnExclBits = COMMON_EXCLUDERS_BITS; // observed 6
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// bitset of positions of c0's excluder cells in Grid.cells
		Idx idx0;

		int i0, i1, i2; // index of each cell in the for-i-loops

		// a virtual array of the 3 cells in an aligned exclusion set.
		Cell c0, c1, c2;

		// the maybes.bits of the cells in the aligned set.
		int c0b, c1b, c2b;

		// notSibling: cache of isNotSiblingOf results.
		boolean ns10, ns20, ns21;

		 // sv01 is shiftedValues 0 and 1,     ie: sv01=sv0|sv1;
		 // sv02 is shiftedValues 0 through 2, ie: sv02=sv0|sv1|sv2;
		int sv01, sv02;

		int avb0, avb1, avb2; // allowedValuesBitsets

		// commonExcluder(maybes)Bits[0]
		int ceb0, ceb1, ceb2, ceb3;

		// presume failure, ie no hint found
		boolean result = false;

		// find candidate cells and the excluders set for each.
		// Aligned Triplet Exclusion requires atleast 2 common excluder cells.
		numCandidates = populateCandidatesAndAtleastTwoExcluders(
				candidates, excluders, grid);
		if ( numCandidates < degree )
			return false;

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false;
			if ( numCandidates > MAX_CANDIDATES )
				return false;
		}

		// before we go into the dog____ing loop we arrayonateShiftedMaybes() so
		// that we can array-iterator over the left-shifted bitset representatin
		// of each cells maybes, instead of left-shifting manually. Faster!
		grid.arrayonateShiftedMaybes(); // set 81 arrays
		try {
			n0 = numCandidates - 2;
			n1 = numCandidates - 1;
			for ( i0=0; i0<n0; ++i0 ) {
				// get c0 and an index-into-Grid.cells of c0's excluder cells.
				idx0 = excluders[(c0=cells[0]=candidates[i0]).i].idx();
				if(hitMe && c0!=hitCells[0]) continue;
				c0b = c0.maybes.bits;
				for ( i1=i0+1; i1<n1; ++i1 ) {
					// get c1 and index of excluders common to c1 and c0.
					// A3E requires atleast 2 common excluder cells.
					if ( excluders[candidates[i1].i].idx2(idx01, idx0) )
						continue;
					c1 = cells[1] = candidates[i1];
					if(hitMe && c1!=hitCells[1]) continue;
					c1b = c1.maybes.bits;
					ns10 = c1.notSees[c0.i];
					for ( i2=i1+1; i2<numCandidates; ++i2 ) {
						// get c2 and index of excluders common to c0, c1, and c2.
						// A3E requires atleast 2 common excluder cells.
						if ( excluders[candidates[i2].i].idx2(idx02, idx01) )
							continue;
						c2 = cells[2] = candidates[i2];
						if(hitMe && c2!=hitCells[2]) continue;

						// read common excluder cells from grid at idx02
						// A3E requires atleast 2 common excluder cells.
						if ( (numCmnExcls=idx02.cellsN(grid, cmnExcls)) < 2 )
							continue;
						// performance enhancement: examine smaller maybes sooner.
						//KRC#2020-06-30 10:20:00 bubbleSort is a bit faster
						//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
						bubbleSort(cmnExcls, numCmnExcls);

						// get the common excluders bits, removing any supersets
						// {12,125}=>{12} coz 125 covers 12, so 125 does nada.
						// A3E requires atleast 2 common excluder cells.
						if ( (numCmnExclBits=subsets(cmnExclBits, cmnExcls
								, numCmnExcls)) < 2 )
							continue;

						// remove each common excluder bits which contains a
						// value that is not in any cells maybes, coz no combo
						// can cover it, so it can't contribute to an exclusion.
						// A3E requires atleast 2 common excluders.
						// pass 7,464,472 of 16,725,495 skip 9,261,023 = 55.37%
						if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
								, c0b|c1b|(c2b=c2.maybes.bits))) < 2 )
							continue;

						// complete the isSiblingOf cache
						ns20 = c2.notSees[c0.i];
						ns21 = c2.notSees[c1.i];

						// reset the allowedValuesBitsets
						avb0=avb1=avb2 = 0;

						// The dog____ing loop works-out is each combo allowed?
						// There are 2 exclusion rules which disallow a combo:
						// (1) hidden single rule: two cells which share a
						//     region cannot contain the same value.
						// (2) common excluder rule: If the combo contains ALL
						//     the value in any common excluder cells maybes
						//     then the combo is not allowed, because the common
						//     excluder cell must be one of those values (no
						//     matter which).

++counts[0];
++counts[numCmnExclBits];
						// nb: this switch is for performance only, the default:
						// handles any numCmnExclBits but faster with no calls.
						switch ( numCmnExclBits ) {
						case 2:
							// we still don't need a common excluders loop
							//  w/o HitSet: 2 = 6,405,619 of 58,603,654 = 10.93%
							// with HitSet: 2 = 301 of 442 = 68.10%
							ceb0 = cmnExclBits[0];
							ceb1 = cmnExclBits[1];
							DOG_1b: for ( int sv0 : c0.shiftedMaybes )
								for ( int sv1 : c1.shiftedMaybes )
									// (1) hidden single rule
									if ( sv1!=sv0 || ns10 ) {
										sv01 = sv0 | sv1;
										for ( int sv2 : c2.shiftedMaybes )
											// (1) hidden single rule
											if ( (ns20 || sv2!=sv0)
											  && (ns21 || sv2!=sv1)
											  // (2) common excluder rule
											  && (ceb0 & ~(sv01|sv2)) != 0
											  && (ceb1 & ~(sv01|sv2)) != 0 ) {
												// Combo Allowed! so add it to allowedValuesBitsets
												avb0|=sv0; avb1|=sv1; avb2|=sv2;
											} // fi
										// next sv2
									} // fi
								// next sv1
							// next sv0
							break;
						case 3:
							// we still don't need a common excluders loop
							//  w/o HitSet: 3 = 1,116,162 of 58,603,654 = 1.90%
							// with HitSet: 3 = 112 of 442 = 25.34%
							ceb0 = cmnExclBits[0];
							ceb1 = cmnExclBits[1];
							ceb2 = cmnExclBits[2];
							DOG_1c: for ( int sv0 : c0.shiftedMaybes )
								for ( int sv1 : c1.shiftedMaybes )
									// (1) hidden single rule
									if ( sv1!=sv0 || ns10 ) {
										sv01 = sv0 | sv1;
										for ( int sv2 : c2.shiftedMaybes ) {
											sv02 = sv01 | sv2;
											// (1) hidden single rule
											if ( (ns20 || sv2!=sv0)
											  && (ns21 || sv2!=sv1)
											  // (2) common excluder rule
											  && (ceb0 & ~sv02) != 0
											  && (ceb1 & ~sv02) != 0
											  && (ceb2 & ~sv02) != 0 ) {
												// Combo Allowed! so add it to allowedValuesBitsets
												avb0|=sv0; avb1|=sv1; avb2|=sv2;
											} // fi
										} // next sv2
									} // fi
								// next sv1
							// next sv0
							break;
						case 4:
							// we still don't need a common excluders loop
							//  w/o HitSet: 4 = 155,559 of 58,603,654 = 0.27%
							// with HitSet: 4 = 20 of 442 = 4.52%
							ceb0 = cmnExclBits[0];
							ceb1 = cmnExclBits[1];
							ceb2 = cmnExclBits[2];
							ceb3 = cmnExclBits[3];
							DOG_1d: for ( int sv0 : c0.shiftedMaybes )
								for ( int sv1 : c1.shiftedMaybes )
									// (1) hidden single rule
									if ( sv1!=sv0 || ns10 ) {
										sv01 = sv0 | sv1;
										for ( int sv2 : c2.shiftedMaybes ) {
											sv02 = sv01 | sv2;
											// (1) hidden single rule
											if ( (ns20 || sv2!=sv0)
											  && (ns21 || sv2!=sv1)
											  // (2) common excluder rule
											  && (ceb0 & ~sv02) != 0
											  && (ceb1 & ~sv02) != 0
											  && (ceb2 & ~sv02) != 0
											  && (ceb3 & ~sv02) != 0 ) {
												// Combo Allowed! so add it to allowedValuesBitsets
												avb0|=sv0; avb1|=sv1; avb2|=sv2;
											} // fi
										} // next sv2
									} // fi
								// next sv1
							// next sv0
							break;
						case 1:
							assert false : "You can't get here";
							continue; // 0 hints from 1 common excluder bits
						default:
							// I give up: we need a common excluders loop
							//  w/o HitSet: 5 = 10,561 of 58,603,654 = 0.02%
							//  w/o HitSet: 6 =    552 of 58,603,654 = 0.00%
							//  w/o HitSet: 7 =     34 of 58,603,654 = 0.00%
							//  w/o HitSet: 8 =      5 of 58,603,654 = 0.00%
							// with HitSet: 5 = 4 of 442 = 0.90%
							// with HitSet: 6 = 1 of 442 = 0.23%
							// with HitSet: 7 = 1 of 442 = 0.23%
							// and I'm suprised there's no hits with 8 cmnExcls!
							DOG_1e: for ( int sv0 : c0.shiftedMaybes )
								for ( int sv1 : c1.shiftedMaybes )
									// (1) hidden single rule
									if ( sv1!=sv0 || ns10 ) {
										sv01 = sv0 | sv1;
										for ( int sv2 : c2.shiftedMaybes )
											// (1) hidden single rule
											if ( (ns20 || sv2!=sv0)
											  && (ns21 || sv2!=sv1)
											  // (2) common excluders rule
											  // call the local notCovers method in place of supers covers method
											  // coz I hope it's a bit faster, coz its return value is pre-negated
											  && notCovers(cmnExclBits, numCmnExclBits, sv01|sv2) ) {
												// Combo Allowed! so add it to allowedValuesBitsets
												avb0|=sv0; avb1|=sv1; avb2|=sv2;
											} // fi
										// next sv2
									} // fi
								// next sv1
							// next sv0
						}

						// are all potential values allowed?
						if ( avb0 == c0b
						  && avb1 == c1b
						  && avb2 == c2b )
							continue; // Nothing to see here. Move along!

						// ====================================================
						// Performance is not an issue from here down.
						// To get here, some value/s is/are are not allowed in
						// any possible combination of the potential values of
						// the cells in the aligned set, so we know that we're
						// going to produce an exclusion hint.
						// ====================================================

++hintCounts[numCmnExclBits];
						if ( isTop1465 )
							hits.add(gsl, hintNum, cells);

						// build the red (removable) potentials.
						Pots redPots = createRedPotentials(cells, avb0,avb1,avb2);
						if ( redPots.isEmpty() )
							continue; // should never happen. Never say never.
						// build the excluded combos map for the hint
						ExcludedCombosMap map = buildExcludedCombosMap(
								cmnExcls, numCmnExcls, cells, redPots);
						// create and add the hint
						AHint hint = new AlignedExclusionHint(
								this, redPots, cells, numCmnExcls, cmnExcls, map);
						result = true; // in case add returns false
						if ( accu.add(hint) )
							return true;

					} // next c2
				} // next c1
			} // next c0
		} finally {
			grid.disarrayonateMaybes();
		}
		return result;
	}

	/**
	 * The notCovers method differs from supers covers method (used in A4+E)
	 * only in that it's return value is "pre-negated", which is confusing as
	 * hell. It just feels like sacralidge.
	 * <p>
	 * NB: Tests show that calling a static method in the tight-loop is actually
	 * FASTER than doing it all in-line, which is directly contrary to previous
	 * experience, which worries me.
	 */
	private static boolean notCovers(final int[] cmnExclBits
			, final int numCmnExclBits, final int combo) {
		for ( int i=0; i<numCmnExclBits; ++i )
			if ( (cmnExclBits[i] & ~combo) == 0 )
				return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation caters for sets of 3 cells: c0, c1, and c2.
	 * @return a new ExcludedCombosMap containing the excluded combo values to
	 * the optional "locking" or "excluding" cell. Null means "one per region".
	 */
	private static ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcls
			, int numCmnExcls, Cell[] cells, Pots redPots) {
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);
		final Cell c0=cells[0], c1=cells[1], c2=cells[2];

		int sv0, sv01, combo, i;

		// Foreach distinct combination of 3 potential values of (c0,c1,c2)
		for ( int v0 : c0.maybes ) { // anything is fast enough for a small enough n
			sv0 = VSHFT[v0];
			for ( int v1 : c1.maybes ) {
				if ( v1==v0 && !c1.notSees[c0.i] ) {
					map.put(new HashA(v0,v1,0),null);
					continue;
				}
				sv01 = sv0 | VSHFT[v1];
				for ( int v2 : c2.maybes ) {
					if ( v2==v0 && !c2.notSees[c0.i] ) {
						map.put(new HashA(v0,0,v2),null);
						continue;
					} else if ( v2==v1 && !c2.notSees[c1.i] ) {
						map.put(new HashA(0,v1,v2),null);
						continue;
					}
					combo = sv01 | VSHFT[v2];
					for ( i=0; i<numCmnExcls; ++i )
						if ( (cmnExcls[i].maybes.bits & ~combo) == 0 ) {
							map.put(new HashA(v0,v1,v2), cmnExcls[i]);
							break; // we want only the first excluder of each combo
						}
						// nb: the combo is allowed if it gets here, but we have
						// nothing to do with it.
					// next common excluder cell
				} // next v2
			} // next v1
		} // next v0
		return map;
	}

}
