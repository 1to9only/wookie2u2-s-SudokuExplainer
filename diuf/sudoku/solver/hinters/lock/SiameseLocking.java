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
 * SiameseLocking extends Locking which implements the (Pointing and
 * Claiming) solving techniques. I just "merge" hints when more than
 * one value is eliminated from the same "slot" (the three cells that
 * are the intersection of a row-or-col and a box).
 * <p>
 * SiameseLocking factored out of Locking because Locking got too
 * complicated, with too many complicating concerns in one class, so I
 * refactored it into Locking, LockingSpeedMode, and SiameseLocking;
 * to separate the concerns. I hope the result is easier to understand.
 * It may not be. Sigh.
 * <p>
 * This class "injects" a SiameseLockingAccumulator into the base
 * Locking, to enable me to "merge" his hints before adding them to the
 * "normal" IAccumulator passed to findHints.
 *
 * @author Keith Corlett 2021-07-12
 */
public final class SiameseLocking extends Locking {

	private final HiddenSet hiddenPair, hiddenTriple;

	private IAccumulator accu;

	// The hacu (Hack Accumulator) wraps an ArrayList<LockingHint> to
	// store the hints in each region, and calls me back to do all the
	// clever stuff. super.findHints calls endRegion which I override
	// to delegate back to hacu.endRegion, which switches on the number
	// of hints in this region:
	// * if there's no hints does nothing;
	// * else if there's only one hint he adds it to the actual accu;
	// * else if there's multiple hints in the region then he calls my
	//   mergeSiameseHints method to do all the clever stuff.
	// Then at the end of findHints we remove any hints whose elims are
	// a subset of any other hints eliminations. So Mr T is ____ed. How
	// can 4 men fire 25 million rounds at 15 fools, and kill just 12
	// of them? That is a hit rate of I-can't-be-bothered coz I haven't
	// eaten coz I spent all of my ____ing money on ammo! Submarines
	// anyone? Get-em while deyr ot! I don't eat frogs legs, or snails.
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
		return hacu!=null && hacu.endRegion(r);
	}

	/**
	 * Searches the given grid for Pointing (box on row/col) and
	 * Claiming (row/col on box) Hints, added to the given
	 * HintsAccumulator.
	 * <p>
	 * "Siamese" is multiple Locking hints on different values in
	 * a region; so I'm not finding my own hints, merely parsing
	 * Locking hints to merge the siamese ones, which I do using a
	 * SiameseLockingAccumulator that calls me back when it's done
	 * searching each region.
	 * <p>
	 * NOTE: "Siamese" a nicety: I just re-present existing hints
	 * succinctly. I do not find any new hints, so I'm a waste of
	 * time in the batch; hence SiameseLocking is used only in the
	 * GUI. Basically, I wrote this for the challenge of doing so,
	 * which seemed like a good idea at the time.
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
		  && (Run.type != Run.Type.GUI || THE_SETTINGS.isFilteringHints(false))
		) {
			// Siamese mode: find hints per region and Siamese them
			final boolean result;
			try {
				this.accu = accu;
				// my persistent hacu calls me back at each endRegion
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
		// Normal mode: delegate-up to standard Locking.
		return super.findHints(grid, accu); // as per normal
	}

	/**
	 * GUI only: remove each hint whose eliminations are a subset of
	 * those in any other hint. Note that this may remove ALL
	 * LockingHint's when siamese found a HiddenSetHint, rendering me a
	 * silent hint killer (near impossible to debug) so if got-bug then
	 * set isFilteringHints to false, and if works then bug is likely in
	 * mergeSiameseHints, not removeSubsets at all (I'm pure vanilla: if
	 * I work at all then I should work always).
	 *
	 * @param hints the list that is still "inside" the accumulator!<br>
	 *  Prolapse Sally, won't you slow that mustang down!<br>
	 *  Ergo: Careful with that exe Eugeene.<br>
	 *  Ergo: Don't be a Dick! (Hills A.)
	 */
	private List<? extends AHint> removeSubsets(final List<? extends AHint> hints) {
		// how many lesbians did the lesbian plucker pluck?
		final int n = hints.size(), m = n - 1;
		// get an array of hints to avoid concurrent modification issues.
		final AHint[] array = hints.toArray(new AHint[n]);
		Pots a, b; // a the lefthand, b the righthand.
		int i; // c is the ____wit formerly known as Prince. And frighten it!
		// foreach hint except the last
		for ( i=0; i<m; ++i )
			// if we got red-wings
			if ( (a=array[i].reds) != null
			  // that're not already removed
			  && hints.contains(array[i]) )
				// foreach subsequent hint (a forward only search)
				for ( int j=i+1; j<n; ++j )
					// if b's got red-wings too
					if ( (b=array[j].reds) != null )
						// if b is subset of a then remove b
						if ( b.isSubsetOf(a) )
							hints.remove(array[j]); // throws NothingToSeeHereException
						// else if a is a subset of b then remove a
						else if ( a.isSubsetOf(b) ) {
							hints.remove(array[i]); // throws MassivelyUnflangableError
							break; // goto next a
						}
		// sort the remaining hints: most effective first
		hints.sort(AHint.BY_SCORE_DESC);
		return hints;
	}

	/**
	 * Siamese runs only in the GUI when isFilteringHints is true.
	 * <p>
	 * EXCEPT: When LogicalSolverTester setsSiamese! It warns on stdout!
	 * <p>
	 * I merge 2 or 3 "Siamese" pointing or claiming hints on different values
	 * in a single region into one hint: replacing several old-hints with one
	 * new-hint in accu. I also check if multiple pointing/claiming constitute
	 * a HiddenSet, and if so I add my eliminations to the HiddenSet hint, and
	 * then remove the old-hints.
	 * <p>
	 * Done in the GUI only because it's a bit slow and only a user cares
	 * about presentation. LogicalSolverTester, BruteForce, and DynamicPlus+
	 * care about Locking hints, but not about now many, so it's quicker to
	 * just process each in turn, instead of summarising them.
	 * <p>
	 * Note that hiddenPair and hiddenTriple are used (cheekily) by Locking to
	 * upgrade siamese locking hints to full HiddenSet hint with extra elims.
	 * This necessitates the {@code boolean NakedSet.search(region)} method,
	 * which slows down the NakedSet search, but only a bit. Sigh.
	 * <p>
	 * I don't always get it right. Odd hints that aren't logically Siamese are
	 * merged anyway, but fast delineation alludes me; so please just shoot me!
	 *
	 * @param r the region that was searched to find these hints
	 * @param origHints the pointing or claiming hints in this region<br>
	 *  see also the original sin, and wanking; always they hide the wanking.<br>
	 *  Hence one infers that "they" must be English. There's no wankers here!
	 */
	boolean mergeSiameseHints(final ARegion r, final ArrayList<LockingHint> origHints) {
		final int n = origHints.size();
		assert n > 1; // or I'm not called
		final boolean isPointing = origHints.get(0).isPointing; // else Claiming
		final Idx idx = new Idx();
		final List<AHint> newHints = new LinkedList<>();
		final Grid grid = r.getGrid();
		// try largest set (all) first, else work-down possible set-sizes as
		// far as 2 (ie each pair of hints).
		for ( int size=n; size>1; --size ) {
			// avert AIOOBE when regionHints are converted into newHints
			if ( origHints.size() < size )
				break;
			// oldHints are a possible combo of size hints among regionHints
			final LockingHint[] oldHints = new LockingHint[size];
			// foreach possible combination of size hints in our n hints
			LOOP: for ( int[] perm : new Permutations(n, new int[size]) ) {
				// build an array of this combo of Locking hints
				for ( int i=0; i<size; ++i )
					oldHints[i] = origHints.get(perm[i]);
				// build an Idx of all the cells in these Locking hints
				idx.set(oldHints[0].idx());
				for ( int i=1; i<size; ++i )
					idx.or(oldHints[i].idx());
				// search for a Hidden Pair or Triple depending on idx.size
				switch ( idx.size() ) {
				case 2:
					if ( hiddenPair.search(r, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, oldHints, size, idx) ) {
						removeAll(oldHints, origHints);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(oldHints) ) { // CAS_A
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, oldHints, isPointing));
						removeAll(oldHints, origHints);
						break LOOP; // we're done here!
					}
					break;
				case 3:
					if ( hiddenTriple.search(r, grid, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), grid
							, newHints, oldHints, size, idx) ) {
						removeAll(oldHints, origHints);
						break LOOP; // we're done here!
					} else if ( redsAllShareARegion(oldHints) ) { // CAS_A
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(this
								, oldHints, isPointing));
						removeAll(oldHints, origHints);
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
			//     cache complication, so I give it a miss, for now.
			if ( newHints.size() > 0 )
				result = accu.add(newHints.get(0));
			else if ( origHints.size() > 0 )
				result = accu.add(origHints.get(0));
		} else {
			// add all available hints
			result = accu.addAll(newHints);
			for ( AHint h : origHints )
				result |= accu.add(h);
		}
		origHints.clear();
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
			, final Idx idx // indices of reds in the Locking hints
	) {
		// KRC BUG 2020-08-20 888#top1465.d5.mt UnsolvableException from apply.
		// Ignore this HiddenSet hint if hisReds do NOT intersect myReds, to
		// NOT upgrade Claiming's into a HiddenSet that is in another Box.
		int myReds = 0;
		for ( int i=0; i<size; ++i )
			myReds |= mine[i].reds.valuesOf();
		if ( (hisHint.hdnSetValues & myReds) == 0 )
			return false;
		// add my Pointing/Claiming eliminations to the HiddenSetHint's reds.
		// Note that I (unusually) add new elims to the existing Pots instance,
		// which is still referenced by hisHint: ergo they're not even my Pots,
		// which is VERY poor form, so @todo: Create a new hint from da two old
		// ones, rather than modify the other mans hint, for tracabilty.
		final Pots hisReds = hisHint.reds;
		for ( int i=0; i<size; ++i )
			hisReds.upsertAll(mine[i].reds);
		// Also claim the values of the HiddenSet from each common region.
		// nb: point/claim elims are from only ONE common region and we want
		// the other one (if any) also. Q: How to calc "the other one"?
		final int cands = hisHint.hdnSetValues;
		final Idx tmp = TMP; // working storage for idxOf
		final Cell[] gridCells = grid.cells;
		// foreach region common to all the cells in theseHints
		for ( ARegion cr : grid.regions(idx.commonRibs()) )
			cr.idxOf(cands, tmp).andNot(idx).forEach(gridCells, (c) ->
				hisReds.upsert(c, c.maybes & cands, false));
		// and add hisHint to newHints.
		newHints.add(hisHint);
		return true;
	}
	private static final Idx TMP = new Idx(); // working storage for idxOf

	// allows the hacu to add hints to the accu
	boolean add(LockingHint h) {
		return accu.add(h);
	}

}
