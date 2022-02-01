/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.chain.ChainerHintsAccumulator;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.hdnset.HiddenSetHint;
import diuf.sudoku.utils.Permutations;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

	private IAccumulator accu;

	// The hacu (Hack Accumulator) wraps an ArrayList<LockingHint> to store
	// the hints in each region, and calls me back to do all the clever stuff.
	// super.findHints calls endRegion which I override to delegate back to
	// hacu.endRegion, which switches on the number of hints in this region:
	// * if there's no hints does nothing;
	// * else if there's only one hint he adds that hint to the actual accu;
	// * else if there's multiple hints in the region then he calls my
	//   mergeSiameseHints method to do all the clever stuff.
	// Then at the end of findHints we remove any hints whose eliminations are
	// a subset of any other hints eliminations. Hence Mr T is ____ed. How can
	// 4 men fire 25 million rounds at 15 fools, and kill just 12 of them? That
	// is a hit rate of I-can't-be-bothered coz I haven't eaten coz I spent all
	// of my ____ing money on ammo! Submarines anyone? Get-em while deyr ot!
	private SiameseLockingAccumulator hacu;

	/**
	 * Constructor for "normal mode".
	 * @param basics
	 */
	public SiameseLocking(Map<Tech,IHinter> basics) {
		super();
		this.hiddenPair = (HiddenSet)basics.get(Tech.HiddenPair);
		this.hiddenTriple = (HiddenSet)basics.get(Tech.HiddenTriple);
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
	 * "Siamese" is multiple Locking hints on different values in a region; so
	 * I'm not finding my own hints, merely parsing Locking hints to merge the
	 * siamese ones, which I do using a SiameseLockingAccumulator that calls
	 * me back when it's done searching each region.
	 * <p>
	 * NOTE: "Siamese" a nicety: I just re-present existing hints succinctly.
	 * I do not find any new hints, so I'm a waste of time in the batch; hence
	 * SiameseLocking is used only in the GUI. Basically, I wrote this just for
	 * the challenge of doing so, which seemed like a good idea at the time.
	 *
	 * @param grid the puzzle to search for hints
	 * @param accu the IAccumulator to which I add hints
	 * @return was a hint/s found.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// Siamese breaks test-cases, so SiameseLockingTest says it's GUI
		if ( Run.type != Run.Type.TestCase
		  // no chainers!
		  && !(accu instanceof ChainerHintsAccumulator)
		  // In GUI, only if isFilteringHints, so user can still see all hints
		  && (Run.type != Run.Type.GUI || THE_SETTINGS.isFilteringHints(false)) ) {
			// Siamese mode: find hints per region and Siamese them
			final boolean result;
			try {
				this.accu = accu;
				// the hacu is persistent: he calls me back at end-of-region
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
				this.accu = null;
			}
			return result;
		}
		// Normal mode: delegate to standard Locking,
		// which still calls me back, passing null.
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
	private List<? extends AHint> removeSubsets(final List<? extends AHint> hints) {
		// get an array of hints to avoid concurrent modification issues.
		final int n = hints.size(), m = n - 1;
		final AHint[] array = hints.toArray(new AHint[n]);
		// a the lefthand, b the righthand.
		Pots a, b;
		int i;
		// foreach hint except the last
		for ( i=0; i<m; ++i ) {
			if ( (a=array[i].reds) != null
			  && hints.contains(array[i]) // not already removed
			) {
				// foreach subsequent hint (a forward only search)
				for ( int j=i+1; j<n; ++j ) {
					if ( (b=array[j].reds) != null ) {
						// if b is subset of a then remove b
						if ( b.isSubsetOf(a) ) {
							hints.remove(array[j]);
						// else if a is a subset of b then remove a
						} else if ( a.isSubsetOf(b) ) {
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
	 * new-hint in accu. I also check if multiple pointing/claiming constitute
	 * a HiddenSet, and if so I add my eliminations to the HiddenSet hint, and
	 * then remove the old-hints.
	 * <p>
	 * Done in the GUI only because it's a bit slow and only a user cares about
	 * "presentation". LogicalSolverTester, BruteForce, and DynamicPlus+ care
	 * about Locking hints, but not there "succinctness", so it's quicker to
	 * just process each in turn instead of summarising.
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
	 * @param r the region that was searched to find these hints
	 * @param list the pointing or claiming hints in this region
	 */
	boolean mergeSiameseHints(final ARegion r, final ArrayList<LockingHint> list) {
		final int n = list.size();
		assert n > 1; // or I'm not called
		final boolean isPointing = list.get(0).isPointing; // else Claiming
		final Idx idx = new Idx();
		final List<AHint> newHints = new LinkedList<>();
		final Grid grid = r.getGrid();
		// try largest set (all) first, else work-down possible set-sizes as
		// far as 2 (ie each pair of hints).
		for ( int size=n; size>1; --size ) {
			// avert AIOOBE when regionHints are converted into newHints
			if ( list.size() < size )
				break;
			// oldHints are a possible combo of size hints among regionHints
			final LockingHint[] oldHints = new LockingHint[size];
			// foreach possible combination of size hints in our n hints
			LOOP: for ( int[] perm : new Permutations(n, new int[size]) ) {
				// build an array of this combo of Locking hints
				for ( int i=0; i<size; ++i ) {
					oldHints[i] = list.get(perm[i]);
				}
				// build an Idx of all the cells in these Locking hints
				idx.set(oldHints[0].idx());
				for ( int i=1; i<size; ++i ) {
					idx.or(oldHints[i].idx());
				}
				// search for a Hidden Pair or Triple depending on idx.size
				switch (idx.size()) {
				case 2:
					if ( hiddenPair.search(r, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, oldHints, size, idx) ) {
						removeAll(oldHints, list);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(oldHints) ) { // CAS_A
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, oldHints, isPointing));
						removeAll(oldHints, list);
						break LOOP; // we're done here!
					}
					break;
				case 3:
					if ( hiddenTriple.search(r, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, oldHints, size, idx) ) {
						removeAll(oldHints, list);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(oldHints) ) { // CAS_A
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, oldHints, isPointing));
						removeAll(oldHints, list);
						break LOOP; // we're done here!
					}
					break;
				}
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
			if ( newHints.size() > 0 ) {
				result = accu.add(newHints.get(0));
			} else if ( list.size() > 0 ) {
				result = accu.add(list.get(0));
			}
		} else {
			// add all available hints
			result = accu.addAll(newHints);
			for ( AHint h : list ) {
				result |= accu.add(h);
			}
		}
		list.clear();
		return result;
	}

	// remove all 'toRemove' from 'from'
	private static void removeAll(final LockingHint[] toRemove, final ArrayList<LockingHint> from) {
		for ( LockingHint h : toRemove ) {
			from.remove(h);
		}
	}

	/**
	 * Do the redPots (aka reds) of lockingHints all share a common region?
	 * If not then this ain't Siamese, just disjunct hints from one region,
	 * which is rare but does happen (about a dozen times in top1465).
	 *
	 * @param a an array of LockingHint to examine
	 * @return do all reds in all locking hints share a common region
	 */
	private boolean redsAllShareARegion(final LockingHint[] a){
		int crs;
		try ( final IALease lease = a[0].reds.leaseIndices() ) {
			crs = Regions.commonI(lease.array);
		}
		for ( int i=1,n=a.length; i<n; ++i )
			try ( final IALease lease = a[i].reds.leaseIndices() ) {
				crs &= Regions.commonI(lease.array);
			}
		return crs != 0;
	}

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
	 * @param mine
	 * @param size
	 * @param idx
	 * @return
	 */
	private static boolean addElims(
			  final HiddenSetHint hisHint // from NakedSet
			, final Grid grid
			, final List<AHint> newHints
			, final LockingHint[] mine // the Locking hints
			, final int size // number of theseHints
			, final Idx idx // of the cells in the Locking hints
	) {
		// KRC BUG 2020-08-20 888#top1465.d5.mt UnsolvableException from apply.
		// Ignore this HiddenSet hint if hisReds do NOT intersect myReds, to
		// NOT upgrade Claiming's into a HiddenSet that is in another Box.
		int myReds = 0;
		for ( int i=0; i<size; ++i )
			myReds |= mine[i].reds.valuesOf();
		if ( (hisHint.hdnSetValues & myReds) == 0 )
			return false;
		// add my Pointing/Claiming eliminations to the HiddenSetHint's redPots
		final Pots heds = hisHint.reds;
		for ( int i=0; i<size; ++i )
			heds.upsertAll(mine[i].reds);
		// Also claim the values of the HiddenSet from each common region.
		// nb: point/claim elims are from only ONE common region and we want
		// the other one (if any) also. Q: How to calc "the other one"?
		final int hsv = hisHint.hdnSetValues;
		final Idx tmp = TMP; // working storage for idxOf
		final Cell[] gridCells = grid.cells;
		// foreach region common to all the cells in theseHints
		try ( final IALease lease = idx.toArrayLease() ) {
			commonRegions(gridCells, lease.array).forEach((cr) ->
				cr.idxOf(hsv, tmp).andNot(idx).forEach(gridCells, (c) ->
						heds.upsert(c, c.maybes & hsv, false)));
		}
		// and add hisHint to newHints.
		newHints.add(hisHint);
		return true;
	}
	private static final Idx TMP = new Idx(); // working storage for idxOf

	/**
	 * Return the regions (expect 1 or 2 of them, so make it 3 to be safe)
	 * common to all cells in the given idx.
	 *
	 * @param gridCells grid.cells
	 * @param indices to find the common regions of
	 * @return a {@code ArrayList<ARegion>} may be empty, but never null.
	 */
	private static ArrayList<ARegion> commonRegions(final Cell[] gridCells, final int[] indices) {
		final ArrayList<ARegion> result = new ArrayList<>(2);
		ARegion cr, r;
		for ( int rti=0; rti<3; ++rti ) {
			cr = null;
			for ( int i : indices ) {
				r = gridCells[i].regions[rti];
				if ( cr == null ) { // the first cell
					cr = r;
				} else if ( r != cr ) {
					cr = null;
					break; // not all cells share a region of this type
				}
			}
			if ( cr != null ) {
				result.add(cr);
			}
		}
		return result;
	}

	// allows the hacu to add hints to the accu
	boolean add(LockingHint h) {
		return accu.add(h);
	}

}
