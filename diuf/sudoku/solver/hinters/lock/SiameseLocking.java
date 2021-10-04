/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.chain.ChainerHacu;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.hdnset.HiddenSetHint;
import diuf.sudoku.utils.Permutations;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * SiameseLocking extends Locking which implements the (Pointing and Claiming)
 * solving techniques. I just "merge" hints when more than one value is
 * eliminated from the same "slot" (the three cells that are the intersection
 * of a row-or-col and a box).
 * <p>
 * This class was factored out of the Locking class because Locking has grown
 * to be too complicated to follow, with too many complicating concerns in the
 * one class, so I'm refactoring it into Locking, LockingSpeedMode, and this
 * SiameseLocking class, to separate the concerns. I hope the result is easier
 * to understand. It may not. Sigh.
 * <p>
 * Most of the code for this class is actually in the SiameseLockingAccumulator
 * which is "injected into" my base instance of Locking, to enable me to merge
 * his hints before passing them onto the IAccumulator passed to my findHints.
 *
 * @author Keith Corlett 2021-07-12
 */
public final class SiameseLocking extends Locking {

	private final HiddenSet hiddenPair, hiddenTriple;

	private Grid grid;
	private IAccumulator accu;
	// The hacu (Hack Accumulator) wraps an ArrayList<LockingHint> to store
	// the hints in each region. It calls me back to do all the clever stuff;
	// especially my base Locking calls endRegion, which I override to call
	// the hacu, which, depending on the number of hints in this region:
	// if there's no hints does nothing (always ideal option for a lazy boy);
	// else if there's only one hint he adds that hint to the actual accu;
	// else if there's multiple hints in the region then he calls my
	//         mergeSiameseHints method to do all the clever stuff.
	// Then at the end of findHints we remove any hints whose eliminations are
	// a subset of any other hints eliminations. So Mr T is ____ed. Fool! How
	// the hell can 4 grown men fire 25,000,000 rounds at 15 ragheads and only
	// kill 12 of them? That's a hit rate of I've forgotten because I can't eat
	// because I've spent all my _____ing money on ____ing ammo!
	private SiameseLockingAccumulator hacu;

	/**
	 * Constructor for "normal mode".
	 * @param hiddenPair
	 * @param hiddenTriple
	 */
	public SiameseLocking(HiddenSet hiddenPair, HiddenSet hiddenTriple) {
		super();
		this.hiddenPair = hiddenPair;
		this.hiddenTriple = hiddenTriple;
	}

	@Override
	protected void startRegion(ARegion r) {
		if ( hacu != null )
			hacu.startRegion(r);
	}

	@Override
	protected boolean endRegion(ARegion r) {
		if ( hacu == null )
			return false;
		return hacu.endRegion(r);
	}

	/**
	 * Searches the given grid for Pointing (box on row/col) and Claiming
	 * (row/col on box) Hints, which are added to the given HintsAccumulator.
	 * <p>
	 * S__t goes weird in "speed mode" when a HintsApplicumulator was passed to
	 * my Constructor, in which case we apply each hint now (to autosolve grid)
	 * and then pass-back the total elims in a AppliedHintsSummaryHint to
	 * getHints-caller via the "normal" HintsAccumulator passed into getHints.
	 * <p>
	 * It's all bit complicated, so writing an explanation seems harder than
	 * writing the code, and I expect the explanation is probably harder to
	 * understand than the code. I just suck at explaining stuff. sigh.
	 * <p>
	 * Note that the HintsApplicumulator has a "debug mode" where it populates
	 * a StringBuilder with the-full-string of each hint applied to the grid.
	 *
	 * @return was a hint/s found.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// Siamese breaks test-cases, so SiameseLockingTest says it's GUI
		if ( Run.type != Run.Type.TestCase
		  // no chainers!
		  && !(accu instanceof ChainerHacu)
		  // In GUI, only if isFilteringHints, so user can still see all hints
		  && (Run.type != Run.Type.GUI || THE_SETTINGS.isFilteringHints(false)) ) {
			// Siamese mode: find hints per region and Siamese them
			final boolean result;
			try {
				this.grid = grid;
				this.accu = accu;
				// the hacu is persistent
				if ( this.hacu == null )
					this.hacu = new SiameseLockingAccumulator(this);
				result = super.findHints(grid, hacu);
				if ( result )
					// remove each hint whose elims are a subset of anothers
					if ( accu.size() > 1 )
						removeSubsets(accu.getList());
					else
						accu.add(hacu.getHint());
			} finally {
				this.grid = null;
				this.accu = null;
			}
			return result;
		}
		// Normal mode: delegate to standard Locking
		// It still calls me back, passing a null
		return super.findHints(grid, accu); // as per normal
	}

	/**
	 * GUI only: remove each hint whose eliminations are a subset of those
	 * in any other hint. Note that this may remove ALL LockingHint's when
	 * siamese found a HiddenSetHint, rendering me a silent hint killer (near
	 * impossible to debug) so if got-bug then set isFilteringHints to false,
	 * and if works then bug is likely in mergeSiameseHints, not removeSubsets
	 * at all (I'm pure vanilla: if I work at all then I should work always).
	 * <p>
	 * NOTE: that the given hints list is the one "inside" the accumulator!
	 *
	 * @param hints
	 */
	private List<? extends AHint> removeSubsets(List<? extends AHint> hints) {
		// get an array of hints to avoid concurrent modification issues.
		final int n = hints.size();
		final AHint[] array = hints.toArray(new AHint[n]);
		// a the lefthand, b the righthand.
		Pots a, b;
		// foreach hint except the last
		for ( int i=0,m=n-1; i<m; ++i ) {
			if ( (a=array[i].redPots) != null
			  && hints.contains(array[i]) // not already removed
			) {
				// foreach subsequent hint (a forward only search)
				for ( int j=i+1; j<n; ++j ) {
					if ( (b=array[j].redPots) != null ) {
						// if b is subset of a then remove b
						if ( b.isSubsetOf(a) )
							hints.remove(array[j]);
						// else if a is a subset of b then remove a
						else if ( a.isSubsetOf(b) ) {
							hints.remove(array[i]);
							break; // goto next a
						}
					}
				}
			}
		}
		// sort the remaining hints: most effective first
		hints.sort(AHint.BY_SCORE_DESC);
		return hints;
	}

	/**
	 * Siamese runs only in the GUI when isFilteringHints is true!
	 * EXCEPT: When LogicalSolverTester setsSiamese! (it warns on stdout)
	 * I merge 2 or 3 "Siamese" pointing or claiming hints on different values
	 * in a single region into one hint: replacing several old-hints with one
	 * new-hint in accu. I now also check if these multiple pointings/claimings
	 * are a HiddenSet, and if so I "upgrade" the HiddenSet hint with my
	 * eliminations, and remove the old-hints.
	 * <p>
	 * We do this in the GUI only because it's a bit slow and only the user
	 * cares about "hint presentation". LogicalSolverTester, SingleSolution,
	 * and the DynamicPlus+ care about Locking hints, but couldn't give three
	 * parts of a flying-farnarkle about there "succinctness", so it's quicker
	 * to seek-and-set each in turn.
	 * <p>
	 * NOTE: SiameseLocking is active only when Filter Hints is switched on.
	 * <p>
	 * NOTE LogicalSolver's hiddenPair and hiddenTriple are used (cheekily) by
	 * Locking to upgrade siamese locking hints to full HiddenSet hint with
	 * additional eliminations. Also note that this necessitates the
	 * {@code boolean NakedSet.search(region)} method, which slows down the
	 * NakedSet search, but only a little bit.
	 * <p>
	 * NOTE I don't always get it "right". Some hints which are not logically
	 * "Siamese" are merged, but I can't think of a fast way to tell em apart,
	 * so I'm a bit too eager. Shoot me!
	 *
	 * @param region the region that was searched to find these hints
	 * @param list the pointing or claiming hints in this region
	 */
	boolean mergeSiameseHints(ARegion region, ArrayList<LockingHint> list) {
		final int n = list.size();
		assert n > 1; // or I'm not called
		final boolean isPointing = list.get(0).isPointing; // else Claiming
		final Idx idx = new Idx();
		final List<AHint> newHints = new LinkedList<>();
		// try the largest set (all) first and we're done if it works,
		// else work-down the possible sizes as far as 2
		// CHANGE: I'm being lazy: This loop now iterates ONCE with size=n.
		// I kept the loop rather than translate the-break-out-logic into
		// I-don't-know-what.
		for ( int size=n; size>1; --size ) {
			// avert AIOOBE when regionHints are converted into newHints
			if ( list.size() < size )
				break;
			// theseHints are a possible combo of size hints among regionHints
			final LockingHint[] theseHints = new LockingHint[size];
			// foreach possible combination of size hints in our n hints
			LOOP: for ( int[] perm : new Permutations(n, new int[size]) ) {
				// build an array of this combo of Locking hints
				for ( int i=0; i<size; ++i )
					theseHints[i] = list.get(perm[i]);
				// build an Idx of all the cells in these Locking hints
				idx.set(theseHints[0].idx());
				for ( int i=1; i<size; ++i )
					idx.or(theseHints[i].idx());
				// search for a Hidden Pair or Triple depending on idx.size
				switch (idx.size()) {
				case 2:
					if ( hiddenPair.search(region, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, theseHints, size, idx) ) {
						removeAll(theseHints, list);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(theseHints) ) {
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, theseHints, isPointing));
						removeAll(theseHints, list);
						break LOOP; // we're done here!
					}
					break;
				case 3:
					if ( hiddenTriple.search(region, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, theseHints, size, idx) ) {
						removeAll(theseHints, list);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(theseHints) ) {
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, theseHints, isPointing));
						removeAll(theseHints, list);
						break LOOP; // we're done here!
					}
					break;
				}
				// just do the full set, not any subsets (too slow)
				break;
			}
		}
		// add the hint/s to the accumulator
		boolean result = false;
		if ( accu.isSingle() ) {
			// add the first (most eliminative) hint
			// nb: the newHints having been "upgraded" tend (not guarantee) to
			//     eliminate more potential values. The only alternative is to
			//     use a HintCache (TreeSet by numElims descending); which we'd
			//     then use as a cache, but my brain hurts already, without the
			//     cach complication, so I give it a miss, for now.
			if ( newHints.size() > 0 )
				result = accu.add(newHints.get(0));
			else if ( list.size() > 0 )
				result = accu.add(list.get(0));
		} else {
			// add all available hints
			result = accu.addAll(newHints);
			for ( AHint h : list )
				result |= accu.add(h);
		}
		list.clear();
		return result;
	}

	// remove all theseHints from regionHints
	// nb: done in a loop coz theseHints is an array, not a collection. Sigh.
	private static void removeAll(LockingHint[] theseHints, ArrayList<LockingHint> regionHints) {
		for ( LockingHint h : theseHints )
			regionHints.remove(h);
	}

	/**
	 * Do the redPots (aka reds) of lockingHints all share a common region?
	 * If not then this ain't Siamese, just disjunct hints from one region,
	 * which is rare but does happen (about a dozen times in top1465).
	 *
	 * @param lockingHints
	 * @return
	 */
	private boolean redsAllShareARegion(LockingHint[] lockingHints){
		// working storage is for speed only.
		// we need 2 of them because set.retainAll(set) is nonsensical.
		// nb: if I wrote retainAll it would assert c!=this;
		final ArrayList<ARegion> crs = Regions.clear(WS1); // commonRegions
		final ArrayList<ARegion> ws2 = Regions.clear(WS2); // working storage
		boolean first = true;
		for ( LockingHint lh : lockingHints )
			if ( first ) {
				Regions.common(lh.redPots.keySet(), crs);
				first = false;
			} else
				crs.retainAll(Regions.common(lh.redPots.keySet(), ws2));
		return !crs.isEmpty();
	}
	private final ArrayList<ARegion> WS1 = new ArrayList<>(3);
	private final ArrayList<ARegion> WS2 = new ArrayList<>(3);

	/**
	 * Add my eliminations to hisHint; and claim redValues from common regions.
	 * <pre>
	 * NB: done in a method just to not repeat the code (keeps growing).
	 * NB: HiddenSets were added later, so I'm not designed just hacked-in.
	 * </pre>
	 *
	 * @param hisHint
	 * @param grid
	 * @param newHints
	 * @param theseHints
	 * @param size
	 * @param idx
	 * @return
	 */
	private boolean addElims(
			  HiddenSetHint hisHint // from NakedSet
			, Grid grid
			, List<AHint> newHints
			, LockingHint[] theseHints // the Locking hints
			, int size // number of theseHints
			, Idx idx // of the cells in the Locking hints
	) {
		// KRC BUG 2020-08-20 888#top1465.d5.mt UnsolvableException from apply.
		// Ignore this HiddenSet hint if hisReds do NOT intersect myReds, to
		// NOT upgrade Claiming's into a HiddenSet that is in another Box.
		int myReds = 0;
		for ( int i=0; i<size; ++i )
			myReds |= theseHints[i].redPots.valuesOf();
		if ( (hisHint.hdnSetValues & myReds) == 0 )
			return false;

		// add my Pointing/Claiming eliminations to the HiddenSetHint's redPots
		final Pots hisReds = hisHint.redPots;
		for ( int i=0; i<size; ++i )
			hisReds.upsertAll(theseHints[i].redPots);

		// and also claim the values of the HiddenSet from each common region.
		// Note that point/claim elims are from only ONE common region and we
		// want the other one (if any) also. Q: How to calc "the other one"?
		// A: Pass down the frickin point/claim region ya putz! Sigh.
		final int hdnSetVals = hisHint.hdnSetValues;
		final Idx tmp = new Idx(); // just working storage for r.idxOf
		final Cell[] cells = grid.cells; // just working storage for r.idxOf
		// foreach region common to all the cells in theseHints
		for ( ARegion r : commonRegions(cells, idx) )
			r.idxOf(hdnSetVals, tmp).andNot(idx).forEach(cells, (cell) ->
				hisReds.upsert(cell, cell.maybes & hdnSetVals, false));
		// and add hisHint to newHints.
		newHints.add(hisHint);
		return true;
	}

	/**
	 * Return the regions (expect 1 or 2 of them, so make it 3 to be safe)
	 * common to all cells in the given idx.
	 *
	 * @param cells the cells of the grid we're solving
	 * @param idx containing indices you want
	 * @return a {@code ArrayList<ARegion>} may be empty, but never null.
	 */
	private ArrayList<ARegion> commonRegions(Cell[] cells, Idx idx) {
		assert idx.size() > 1;
		final int[] idxArray = idx.toArrayA(); // get an array ONCE
		final ArrayList<ARegion> result = new ArrayList<>(2);
		ARegion commonRegion, cellsRegion;
		for ( int rti=0; rti<3; ++rti ) {
			commonRegion = null;
			for ( int i : idxArray ) {
				cellsRegion = cells[i].regions[rti];
				if ( commonRegion == null ) // the first cell
					commonRegion = cellsRegion;
				else if ( cellsRegion != commonRegion ) {
					commonRegion = null;
					break; // not all cells share a region of this type
				}
			}
			if ( commonRegion != null )
				result.add(commonRegion);
		}
		return result;
	}

	// allows the hacu to add hints to the accu
	boolean add(LockingHint h) {
		return accu.add(h);
	}

}
