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
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Aligned2Exclusion implements the aligned set exclusion Sudoku solving
 * technique for sets of 2 cells, or pairs.
 * <p>
 * This class is simplest A*E implementation, so it's heavily commented. Try
 * to understand A2E fairly well before moving onto the larger sets. A5+E
 * differ from A234E, so A5E_1C is heavily commented also.
 * <p>
 * I struggled to understand Aligned Set Exclusion. Here's a brief precis. We
 * align $degree cells (candidates) around 1or2 cells (common excluders), and
 * calculate if each possible combo of there maybes is valid (rules below), if
 * so we just record "value allowed in candidate" and move onto the next combo.
 * When we've checked all possible combos of values we see if any cell-value/s
 * have NOT been allowed (ie cell-values/s are excluded), and if so we create a
 * hint. So excluded means "no combo allows it". Sounds simple enough. Sigh.
 * <p>
 * Each candidate is an unset Cell which has at least the minimum (1 or 2)
 * siblings with between 2 and $degree (2 in pairs) potential values (maybes).
 * <p>
 * There's two rules used to calculate "Is this combo of maybes valid?":
 * <pre><tt>
 * (1) Hidden Single rule: two cells in a region can't have the same value; and
 * (2) Common Excluders rule: if this combo covers (is a superset of) all of a
 *     common-excluder-cells potential values then this combo is NOT allowed,
 *     because the common excluder cell must be one of those values (no matter
 *     which). Note that "not allowed" does NOT mean "excluded" yet; a value is
 *     "excluded" only when NONE of the allowed combos includes it.
 * </tt></pre>
 * <p>
 * The minimum number of siblings varies. It's "correctly" 1 in A234E; but it's
 * the hacked 2 in A5678910E_2H rather than not use those A*E's at all (as per
 * original) because they're too slow. So just to be clear restricting A5+E to
 * at least 2 excluders is a BIG performance HACK, but the alternative is they
 * are so slow that nobody waits, so they're disabled; and some beats none, so
 * A5+E "hacked" or "correct" is user-selectable in the TechSelectDialog.
 * Personally I'll wait for A567E_1C, but I find A8910E too slow, especially
 * A10E_1C who should be SHOT. The bastard took about a DAY to do top1465.d5.mt
 * (on my ~2900 Mhz i7) and found only two hints!
 * <p>
 * Then we do value exclusion: we look at each possible combination (called a
 * combo) of the potential values of the cells in our aligned set, to workout
 * if the combo is allowed, and if so we record the fact that each value is
 * allowed in it's position.
 * <p>
 * We keep track of which values have been allowed in each position in the
 * allowedValuesBitsets (avb*: a virtual array), and when we get to the end of
 * the combos (a finite set) we just check if ALL of each candidate cells
 * maybes are allowed, and if they aren't (ie some value/s are excluded) then
 * we've found an aligned set exclusion, so we go forth and build a explanation
 * (an ExcludedCombosMap) of exactly why those value/s are excluded to pass to
 * the hint, to show to the user, who makes sense of life, the universe, and
 * the unprecedented copiousness of David Attenborough's mistress's navel lint,
 * and how last Tuesdays precipitation relates to section 42 of "The Principles
 * of Thermodynamics and Permanent Public Anal Hair Setting" by Frost et al
 * (3rd Edition: the one without the gannet).
 * <p>
 * NB: The explanation (ExcludedCombosMap) now includes only values that are
 * relevant to the red (removable) potential values. This filter is buried in
 * the overridden put method of the ExcludedCombosMap, whic is a static inner-
 * class of our super-class (Confused yet? Don't worry you soon will be).
 * <p>
 * This class extends (abstract) AAlignedSetExclusionBase but does not use its
 * populateCandidatesAndExcluders method, because my local version is specific
 * to Sets of 2. Likewise my local covers method.
 * <p>
 * Before you proceed down the A*E path please go fetch your cheese grater. You
 * will want it later.
 *
 * @author Keith Corlett
 */
public final class Aligned2Exclusion extends AAlignedSetExclusionBase
//		implements diuf.sudoku.solver.IReporter
//				 , java.io.Closeable
{

	// maximum number of candidates to permute (process)
	private static final int MAX_CANDIDATES = 52; // <HACK/>

	// common excluders array = commonExcludersSet.toArray
	private static final int NUM_CMN_EXCLS = 12; // observed 11

	// These arrays are all fields because it's faster to not recreate arrays
	// in each and every findHints call. Array creation in Java is well slow.
	// One suspects that Microsoft put a wait in the O/S API which Java uses,
	// because Java uses it, to push people to pay for the latest version of
	// MS-AssBungle instead. Conspiracy theory? What conspiracy theory? Just
	// don't trust them blindly! I wouldn't trust Microsoft to run a bath.
	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];
	private static final Cell[] CELLS_ARRAY = new Cell[2];

	// common excluders indexes: idx01 is an index of the siblings common
	// to c0 and c1. It's a field just to not have to recreate the Idx in
	// every call to findHints. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1

	public Aligned2Exclusion() {
		super(Tech.AlignedPair, null);
		assert tech.isAligned;
		assert degree == 2;
	}

//	@Override
//	public void close() throws java.io.IOException {
//		hits.save();
//	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * {@code Aligned2Exclusion} implements the Aligned Set Exclusion Sudoku
	 * solving technique for sets of 2 cells (ie a pair).
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {

		// Save yourself! Remove your genetalia with an angle-grinder. Stick
		// your nuts in an industrial conveyor belt and set the controls for
		// the heart of the sun. Gobble a grenade. Nibble a scorpions nipple.
		// Straighten your anal pubes with an oxy-acetylene torch. Profess
		// your undying ardor for Madge The Vadge at a meeting of the CFMEU.
		// Right, now go ahead and masturbate HARD with that cheese grater.
		// It's all far less painful than making A*E's performant; and relax,
		// it only gets worse as the aligned sets grow. Patience Willow! You
		// are going to need patience, and persistence, and intelligence, and
		// a new cheese grater. Sigh.

		// HintsApplicumulator buggers-up arrayonateShiftedMaybes() because the
		// .shiftedMaybes arrays go out of date. Same for all A*E which use the
		// .shiftedMaybes arrays, ie currently A23E, but this may change, coz
		// it's nearly as fast/convenient to use Values.SHIFTED directly.
		assert !(accu instanceof HintsApplicumulator);

		// localise this variable for speed (and make it final).
		// hackTop1465 is isHacky && filePath.contains("top1465")
		final boolean hackTop1465 = AHinter.hackTop1465;
		// istop1465 is just the filePath.contains("top1465")
		// I use it to filter when not isHacky, else top1465 took 34:16:04,
		// and I shall not wait a day and a half again!
		final boolean isTop1465 = grid.source!=null && grid.source.isTop1465;
		// grid.source.lineNumber ie which puzzle are we currently solving
		final int gsl = grid.source!=null ? grid.source.lineNumber : 0;

		// localise fields for speed if it's referenced 3 or more times. Java
		// takes about as long to create and set a local variable as it does to
		// dereference an attr TWICE (differs in C/++ and MASM, why?), so if
		// the field is referenced 3-or-more times then a local var is faster.
		// Also the local var cannot be modified behind your back by another
		// bloody concurrent thread. Any other thread running through this code
		// simultaneously has it's own local variables. Mine means mine. The
		// reason I use final so much is so that I cannot snaphoo myself by
		// accidentally mutating the value of a variable that I intended to be
		// a WORM (write once read many). Also da JIT compiler optimiser can do
		// things with immutable values, like just leave it on a register, that
		// it can't do (safely) if the value is mutable. Sometimes the bastard
		// does it anyway, when the value is not mutated in early invocations,
		// which is enough to drive you mad. Shoot the bastards!
		final int degree = this.degree;
		final int hintNum = AHint.hintNumber;
		final boolean useHits = this.useHits;

		// Cheat like big fat hairy mother....... Oh wait!
		// get an array of the Cells at which we hinted in the last run;
		// otherwise we skip this call to getHints
		final Cell[] hitCells = useHits // only true when AHinter.hackTop1465
				? hits.getHitCells(gsl, hintNum, degree, grid)
				: null;
		final boolean hitMe = hitCells != null;
		if ( useHits && !hitMe )
			return false; // no hints for this puzzle/hintNumber

		// The populateCandidatesAndExcluders fields: a candidate Cell has
		// maybes.size>1 and >0 excluders (siblings) with maybes.size==2
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = new Cell[NUM_CANDIDATES];
		final int numCandidates;
		// an array of the set-of-excluder-cells for each candidate. A sparse
		// array indexed by Cell.i: each cells index in Grid.cells array.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// cells is an array of the cells in each aligned set. nb: this array is
		// reused, not recreated for each aligned set, for performance; because
		// less garbage always means less GC, which makes it all faster.
		final Cell[] cells = CELLS_ARRAY;

		// the excluder cells common to c0 and c1
		final Cell[] cmnExcls = COMMON_EXCLUDERS_ARRAY;
		// the common-excluder-cells.maybes.bits. This set differs from the
		// common-excluder-cells in that supersets and disjuncts are removed.
		final int[] cmnExclBits = COMMON_EXCLUDERS_BITS;

		// the number of cells in the for-i0-loop
		final int n0;

		// index of c0's excluder cells set
		Idx idx0;

		// a virtual array of the 2 candidate cells in an aligned exclusion set.
		Cell c0, c1;

		// the number of common excluder cells, and the number of common-
		// excluder-bits, which may be smaller than numCmnExcls
		int numCmnExcls, numCmnExclBits;

		// cache c1.isNotSiblingOf[c0.i]
		boolean ns10;

		// allowedValuesBitsets concurrent with the cells virtual array
		int avb0, avb1;

		// the maybes.bits of the cells in the aligned set.
		int c0b, c1b;

		// ceb0 is short for CommonExcluder(cells.maybes.)Bits[0]
		int ceb0, ceb1, ceb2, ceb3, ceb4;

		// shiftedValues0and1
		int sv01; // = sv0 | sv1;

		// presume failure, ie no hint found
		boolean result = false;

		// collect the candidate cells and there excluders.
		// Aligned Pair Exclusion requires atleast 2 common excluder cells.
		numCandidates = populateCandidatesAndAtleastTwoExcluders(
				candidates, excluders, grid);
		if (numCandidates < degree) // ie < 2
			return false; // This'll never happen. Never say never.

		if ( hackTop1465 && numCandidates > MAX_CANDIDATES )
			return false;

		// before we go into the dog____ing loop we arrayonateShiftedMaybes()
		// to array-iterator over the left-shifted bitset of each cell.maybes,
		// instead of left-shifting in-situ; coz it's faster.
		grid.arrayonateShiftedMaybes();
		try {
			// for each candidate cell except the last one
			n0 = numCandidates - 1;
			for ( int i0=0; i0<n0; ++i0 ) {
				// idx() returns it's cached array. Don't ____ with idx0!
				idx0 = excluders[(c0=cells[0]=candidates[i0]).i].idx();
				if(hitMe && c0!=hitCells[0]) continue;
				c0b = c0.maybes.bits;
				// for each subsequent candidate cell, to make-up the pair.
				for ( int i1=i0+1; i1<numCandidates; ++i1 ) {
					// set the common excluders index (idx01) to intersection/s
					// of c1's excluders with c0's excluders.
					// nb: Aligned Pair Exclusion is only possible with atleast
					//     2 common excluder cells, so we call idx2.
					if ( excluders[candidates[i1].i].idx2(idx01, idx0) )
						continue;

					c1 = cells[1] = candidates[i1];
					if(hitMe && c1!=hitCells[1]) continue;
					c1b = c1.maybes.bits;

					// read common excluder cells from grid at idx01
					// Aligned Pair Exclusion only possible with >= 2 cmnExcls.
					if ( (numCmnExcls = idx01.cellsN(grid, cmnExcls)) < 2 ) {
						assert false : "you can't get here!";
						continue; // should never happen. Never say never.
					}
					// performance enhancement: examine smaller maybes sooner
					//KRC#2020-06-30 10:20:00 bubbleSort is a bit faster
					//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
					bubbleSort(cmnExcls, numCmnExcls);

					// get common excluder bits, removing any supersets,
					// eg {12,125}=>{12} coz 125 covers 12, so 125 does nothing.
					// Aligned2Exclusion is only possible with > 1 cmnExcls.
					if ( (numCmnExclBits = subsets(cmnExclBits, cmnExcls
							, numCmnExcls)) < 2 )
						continue;

					// remove each common excluder bits which contains a value
					// that is not in any cells maybes, because no combo can
					// cover it, so it can't contribute to any exclusions.
					// Aligned Pair Exclusion only possible with >= 2 cmnExcls.
					// pass 781,567 of 3,082,394 skip 2,300,827 = 74.64%
					if ( (numCmnExclBits = disdisjunct(cmnExclBits
							, numCmnExclBits, c0b|c1b)) < 2 )
						continue;

					// complete the notSibling cache
					ns10 = c1.notSees[c0.i];

					// cache the first common excluder cells maybes bits
					ceb0 = cmnExclBits[0];

					// reset the allowedValuesBitsets (a virtual array)
					avb0=avb1 = 0;

					// The dog____ing loop works-out if each combo is allowed.
					// ie: Is each possible combination of the potential values
					// of these $degree (2) cells allowed (rules below)? If so
					// then we add it to the allowedValuesBitsets virtual array;
					// else all combos which contain the-combo-so-far are
					// skipped with one continue statement (smart).
					// After the loop we skip this aligned-set if ALL potential
					// values of the-cells-in-this-aligned-set were in any
					// allowed combo; else one-or-more value/s is/are excluded,
					// so we build the ExcludedCombosMap and issue the hint.
					// There are 2 exclusion rules:
					// (1) Hidden Single rule: Two cells may be the same value
					//     only if those two cells do not share a region, ie are
					//     not in the same box, row, or col. This one per region
					//     rule is THE fundamental rule of Sudoku.
					// (2) Common Excluder rule: If the combo "covers" (ie combo
					//     is a superset of; ie combo contains ALL values in)
					//     any common excluder cells maybes then the combo is
					//     not allowed, because the excluder cell must be ONE
					//     of those values (no matter which).
					// nb: Unlike Juillerat we only populate the avb's in this
					// loop. We only build the ExcludedCombosMap when there's
					// an exclusion, which is S__TLOADS faster!
					// nb: it's faster to "pre-eliminate" ie remove maybes from
					// sibling cells for large (4+) sets than "skip collisions"
					// as in A23E; because we do more of the work less often.
					// Constrast DOG_1a with A4E's to see what I'm on about.

					// nb: this if statement for performance only, the default:
					// works for any numCmnExclBits. Just faster with no calls.
					// Ie: this switch avoids calling the covers method just for
					// speed, the default path works for any numCmnExclBits, it
					// is just faster to NOT call methods (or create variables)
					// in a tight loop, unless there's no alternative, and there
					// is almost always an alternative which you haven't thought
					// of yet, because it's unconventional/wierd/verbose/ugly.
++counts[0];
++counts[numCmnExclBits];
					switch ( numCmnExclBits ) {
					case 2:
						// there's still no need for a common excluders loop.
						//  w/o HitSet: 2 = 691,040 of 4,839,905 = 14.28% (no hints = WTF)
						// with HitSet: 2 = 1 of 20 = 5.00%
						ceb1 = cmnExclBits[1];
						DOG_1b: for (int sv0 : c0.shiftedMaybes)
							for (int sv1 : c1.shiftedMaybes)
								if ( (ns10 || sv1!=sv0)
								  // nb: it faster to | twice than set var once!
								  && ceb0 != (sv0|sv1)
								  && ceb1 != (sv0|sv1) ) {
									avb0|=sv0; avb1|=sv1;
								}
						break;
					case 3:
						// there's still no need for a common excluders loop.
						//  w/o HitSet: 3 = 82,486 of 4,839,905 = 1.70% (hints 19)
						// with HitSet: 3 = 12 of 20 = 60.00%
						ceb1 = cmnExclBits[1];
						ceb2 = cmnExclBits[2];
						DOG_1c: for (int sv0 : c0.shiftedMaybes)
							for (int sv1 : c1.shiftedMaybes)
								if ( (ns10 || sv1!=sv0)
								  && ceb0 != (sv01=sv0|sv1)
								  && ceb1 != sv01
								  && ceb2 != sv01 ) {
									avb0|=sv0; avb1|=sv1;
								}
						break;
					case 4:
						// there's still no need for a common excluders loop.
						//  w/o HitSet: 4 = 7,460 of 4,839,905 = 0.15% (hints 5)
						// with HitSet: 4 = 4 of 20 = 20.00%
						ceb1 = cmnExclBits[1];
						ceb2 = cmnExclBits[2];
						ceb3 = cmnExclBits[3];
						DOG_1d: for (int sv0 : c0.shiftedMaybes)
							for (int sv1 : c1.shiftedMaybes)
								if ( (ns10 || sv1!=sv0 )
								  && ceb0 != (sv01=sv0|sv1)
								  && ceb1 != sv01
								  && ceb2 != sv01
								  && ceb3 != sv01 ) {
									avb0|=sv0; avb1|=sv1;
								}
						break;
					case 5:
						// there's still no need for a common excluders loop.
						//  w/o HitSet: 5 = 514 of 4,264,906 = 0.01%
						//  w/o HitSet: 5 = 516 of 4,839,905 = 0.01% (hints 2)
						// with HitSet: 5 = 2 of 20 = 10.00%
						ceb1 = cmnExclBits[1];
						ceb2 = cmnExclBits[2];
						ceb3 = cmnExclBits[3];
						ceb4 = cmnExclBits[4];
						DOG_1e: for (int sv0 : c0.shiftedMaybes)
							for (int sv1 : c1.shiftedMaybes)
								if ( (ns10 || sv1!=sv0)
								  && ceb0 != (sv01=sv0|sv1)
								  && ceb1 != sv01
								  && ceb2 != sv01
								  && ceb3 != sv01
								  && ceb4 != sv01 ) {
									avb0|=sv0; avb1|=sv1;
								}
						break;
					case 1:
						// Aligned Pair Exclusion not possible with 1 cmnExcl!
						// case 1: exists just to differentiate from default:
						assert false : "You can't get here!";
						continue;
					default:
						// =============================================================================
						// I give up: The common excluders loop is in notCovers method, which (contrary
						// to prior experience) is FASTER than doing it inline. I GUESS the method must
						// be static, and I've also made all the parameters final, in the hope that the
						// JIT compiler will do some magic.
						// nb: top1465 has 65 (ie not many) aligned sets with 6-or-more common excluder
						// bits, but if there ever are "many" then it might pay to implement a case 7:
						// (etc), but we'll always retain the default: case to pick-up any dregs.
						// =============================================================================
						//  w/o HitSet: 6 = 43 of 4,839,905 = 0.00% (hints 1)
						//  w/o HitSet: 7 = 22 of 4,839,905 = 0.00% (hints 0)
						// with HitSet: 6 = 1 of 20 = 5.00%
						DOG_1f: for (int sv0 : c0.shiftedMaybes)
							for (int sv1 : c1.shiftedMaybes)
								if ( (ns10 || sv1!=sv0 )
								  // call local notCovers method instead of supers covers
								  // method coz I hope it's a bit faster, coz it just
								  // "combo == ceb[i]" and its return value is pre-negated
								  && notCovers(cmnExclBits, numCmnExclBits, sv0|sv1) ) {
									avb0|=sv0; avb1|=sv1;
								}
					}

					// if all the potential values of both cells are allowed
					// then there's no hint here. Try the next pair of cells.
					if ( avb0 == c0b
					  && avb1 == c1b )
						continue; // Nothing to see here, move along!

					// ================================================
					// To get here value/s in these cells are excluded.
					// Performance is not an issue from here down.
					// ================================================

++hintCounts[numCmnExclBits];
					if ( isTop1465 )
						hits.add(gsl, hintNum, cells);

					// build the removable (red) potentials
					// map of Cell/s which have removable values => those values
					Pots redPots = createRedPotentials(cells, avb0,avb1);
					// build a map of the excluded combos for the hint
					final ExcludedCombosMap map = buildExcludedCombosMap(
							cmnExcls, numCmnExcls, cells, redPots);
					// create and add hint
					AHint hint = new AlignedExclusionHint(this, redPots
							, cells, numCmnExcls, cmnExcls, map);
					result = true; // in case accu.add returns false
					if ( accu.add(hint) )
						return true;

				} // next c1 // next c1
			} // next c0
		} finally {
			grid.disarrayonateMaybes();
		}
		return result;
	}

	/**
	 * The notCovers method differs from supers covers method (used in A4+E) in
	 * that it's return value is "pre-negated" (which is confusing as hell) and
	 * also in that here we just: {@code cmnExclBits[i] == combo},<br>
	 * ie the each common excluder cells maybes.bits == combo,<br>
	 * where in A3+E we resort to the old {@code (cmnExclBits[i] & ~combo) == 0}
	 * trick to see if combo is a superset of the common excluder cells maybes.
	 * <p>
	 * The equals only works here coz we've got 2 cells in each aligned set and
	 * each common excluder has exactly 2 maybes, whereas in A3+E there may be
	 * more cells in the aligned set (ie combo size) than there are maybes in
	 * each common excluder cell.
	 * <p>
	 * NB: Testing has shown that calling a method in the loop is actually
	 * FASTER than doing it all in-line, contrary to previous experiences.
	 */
	private static boolean notCovers(final int[] cmnExclBits
			, final int numCmnExclBits, final int combo) {
		for ( int i=0; i<numCmnExclBits; ++i )
			if ( cmnExclBits[i] == combo )
				return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation caters for sets of 2 cells: c0, and c1.
	 * @return a new ExcludedCombosMap containing the excluded combo values to
	 * the optional "locking" or "excluding" cell. Null means "one per region".
	 */
	private static ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcls
			, int numCmnExcls, Cell[] cells, Pots redPots) {
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);
		final Cell c0=cells[0], c1=cells[1];

		int sv0, combo, i;

		// Foreach distinct combination of 3 potential values of (c0,c1,c2)
		for ( int v0 : c0.maybes ) { // even an iterator is fast enough here
			sv0 = VSHFT[v0];
			for ( int v1 : c1.maybes ) {
				if ( v1==v0 && !c1.notSees[c0.i] ) {
					map.put(new HashA(v0,v1,0),null);
					continue;
				}
				combo = sv0 | VSHFT[v1];
				for ( i=0; i<numCmnExcls; ++i )
					if ( (cmnExcls[i].maybes.bits & ~combo) == 0 ) {
						map.put(new HashA(v0,v1), cmnExcls[i]);
						break; // we want only the first excluder of each combo
					}
					// nb: the combo is allowed if it gets here,
					//     but we've got nothing to do with it.
				// next common excluder cell
			} // next v1
		} // next v0
		return map;
	}

}
