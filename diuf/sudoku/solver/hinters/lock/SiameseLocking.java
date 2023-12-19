/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.hidden.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.utils.Permuter;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;
import diuf.sudoku.utils.IntQueue;

/**
 * SiameseLocking extends Locking which implements the (Pointing and Claiming)
 * solving techniques, but only in the GUI, where performance is NOT critical,
 * just nice to have. I merge hints when more than one value is eliminated from
 * the same slot. A slot is the three cells that are the intersection of a line
 * (a row, or a col) and a box. Slots are pure evil genius!
 * <p>
 * SiameseLocking was factored-out of Locking coz Locking got too complicated;
 * too many concerns in one class. I split it into Locking, LockingSpeedMode,
 * and SiameseLocking; to separate these concerns. I hope the result is easier
 * to follow. It may not be. Sigh.
 * <p>
 * findHints injects a {@link SiameseLockingAccumulator} into my super-classes
 * {@link Locking#findHints}, enabling me to merge Locking hints before adding
 * them to the normal IAccumulator passed into findHints.
 * <p>
 * Note that Locking is swapped on-the-fly, hence nothing in the lock package
 * can be a prepper!
 *
 * @author Keith Corlett 2021-07-12
 */
public final class SiameseLocking extends Locking {

	// wanking storage for idxOf (more on that later)
	private static final Idx IDX = new Idx();

	private static final Idx BUDS = new Idx(); // full

	// I need help finding any extra eliminations
	private final HiddenPair hiddenPair;
	private final HiddenTriple hiddenTriple;

	// the normal accumulator, passed into findHints
	private IAccumulator accu;

	// The slac (SiameseLockingAccumulator) wraps a ArrayList<LockingHint> to
	// store the hints in each region, calling me back to do the clever stuff.
	// super.findHints calls endRegion which I override to delegate back to
	// slac.endRegion, which switches on the number of hints in this region:
	// * if there is no hints does nothing;
	// * else if there is only one hint he adds it to the actual accu;
	// * else if there is multiple hints in the region then he calls-back my
	//   mergeSiameseHints method to do all the clever hint-merging stuff.
	// Then at the end of findHints we remove hints whose elims are a subset of
	// any other hints eliminations. So Mr T is ____ed. How can 4 men fire 25
	// million rounds at 15 fools, and kill only 12 of them? That is a hit rate
	// of I-cant-be-bothered, coz I have not eaten, coz I spent all my ____ing
	// money on ammo! 700+ BILLION on subs, and ~30% of people cannot afford a
	// ____ing house! The rich pricks fail to understand that they are pricks.
	private SiameseLockingAccumulator slac;

	/**
	 * Constructor for "normal mode".
	 * @param basics
	 */
	public SiameseLocking(final Map<Tech,IHinter> basics) {
		super();
		// NOTE: cast to implementation classes to access the search method,
		// which is not included in IHinter, coz its specific to HiddenSet.
		this.hiddenPair = (HiddenPair)basics.get(Tech.HiddenPair);
		this.hiddenTriple = (HiddenTriple)basics.get(Tech.HiddenTriple);
	}

	/**
	 * Locking calls me back at the start of processing of each Box.
	 *
	 * @param r the box that we are now searching
	 */
	@Override
	protected void startSiamese(final ARegion r) {
		if ( slac != null )
			slac.startRegion(r);
	}

	/**
	 * Locking calls me back the end of processing of each Box.
	 *
	 * @param r the box that we have now finished searching.
	 * @return {@link SiameseLockingAccumulator#endRegion(diuf.sudoku.Grid.ARegion)
	 */
	@Override
	protected boolean endSiamese(final ARegion r) {
		return slac!=null && slac.endRegion(r);
	}

	/**
	 * Searches the given grid for Pointing (box on row/col) and
	 * Claiming (row/col on box) Hints, added to the given
	 * HintsAccumulator.
	 * <p>
	 * "Siamese" is multiple Locking hints on different values in
	 * a region; so I am not finding my own hints, merely parsing
	 * Locking hints to merge the siamese ones, which I do using a
	 * SiameseLockingAccumulator that calls me back when it is done
	 * searching each region.
	 * <p>
	 * NOTE: "Siamese" a nicety: I just re-present existing hints
	 * succinctly. I do not find any new hints, so I am a waste of
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
		// Siamese breaks test-cases, so SiameseLockingTest says it is GUI
		if ( !Run.isTestCase()
		  // no chainers!
		  && !accu.isChaining()
		  // In GUI, only if isFilteringHints, so user can still see all hints
		  && (!Run.isGui() || CFG.isFilteringHints())
		) {
			// Siamese mode: find hints per region and Siamese them
			final boolean result;
			try {
				this.accu = accu;
				// my slac calls me back at each endRegion
				if ( this.slac == null )
					this.slac = new SiameseLockingAccumulator(this);
				result = super.findHints(grid, slac);
				if ( result ) {
					// remove each hint whose elims are a subset of anothers
					if ( accu.size() > 1 )
						removeSubsets(accu.getList());
					else
						accu.add(slac.poll());
				}
			} finally {
				this.accu = null;
			}
			return result;
		}
		// Normal mode: delegate-up to standard Locking.
		return super.findHints(grid, accu); // as per normal
	}

	/**
	 * GUI only: remove each hint whose eliminations are a subset of those in
	 * any other hint. Note that this may remove ALL LockingHints when siamese
	 * found a HiddenSetHint, rendering me a silent hint killer (impossible to
	 * debug) so if got-bug then set isFilteringHints to false, and if it works
	 * then bug is likely in mergeSiameseHints, not in removeSubsets.
	 * I am vanilla: if I work at all then I should always work.
	 *
	 * @param hints the list that is still "inside" the accumulator!<br>
	 *  Ergo: Careful with that axe Eugeene.
	 */
	private List<? extends AHint> removeSubsets(final List<? extends AHint> hints) {
		// num hints, minus 1 because every good boy deserves pussy
		final int n = hints.size(), m = n - 1;
		// avert concurrent modification exception
		final AHint[] array = hints.toArray(new AHint[n]);
		Pots a, b; // a left, b right.
		int i, j; // a index, b index. And Frighten It!
		// foreach hint except the last
		for ( i=0; i<m; ++i ) {
			// if I have reds, that are not already removed
			if ( (a=array[i].reds)!=null && hints.contains(array[i]) ) {
				// foreach subsequent hint (forwards only search)
				for ( j=i+1; j<n; ++j ) {
					if ( (b=array[j].reds) != null ) {
						if ( b.isSubsetOf(a) ) {
							hints.remove(array[j]); // remove b
						} else if ( a.isSubsetOf(b) ) {
							hints.remove(array[i]); // remove a
							break; // next a
						}
					}
				}
			}
		}
		// sort remaining hints: most eliminations first
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
	 * Done in the GUI only because it is a bit slow and only a user cares
	 * about presentation. LogicalSolverTester, BruteForce, and DynamicPlus+
	 * care about Locking hints, but not about now many, so it is quicker to
	 * just process each in turn, instead of summarising them.
	 * <p>
	 * Note that hiddenPair and hiddenTriple are used (cheekily) by Locking to
	 * upgrade siamese locking hints to full HiddenSet hint with extra elims.
	 * This necessitates the {@code boolean NakedSet.search(region)} method,
	 * which slows down the NakedSet search, but only a bit. Sigh.
	 * <p>
	 * I do not always get it right. Odd hints that are not logically Siamese are
	 * merged anyway, but fast delineation alludes me; so please just shoot me!
	 *
	 * @param region the region that was searched to find these hints
	 * @param origHints the pointing or claiming hints in this region. See also
	 *  the original sin: wanking; always they secrete the wanking. Hence they
	 *  are English. Theres no wankers here mate! Just keep walking. 2023 Ashes
	 *  in a nutshell. ~KRC.
	 */
	boolean mergeSiameseHints(final ARegion region, final ArrayList<AHint> origHints) {
		final int n = origHints.size();
		assert n > 1; // or I am not called
		grid = region.getGrid();
		maybes = grid.maybes;
		regions = grid.regions;
		final boolean isPointing = ((LockingHint)origHints.get(0)).isPointing; // else Claiming
		final Idx lockingIdx = new Idx();
		final List<AHint> newHints = new LinkedList<>();
		final Permuter permuter = new Permuter();
		// try largest set (all) first, else work-down possible set-sizes as
		// far as 2 (ie each quad, triple, pair of hints).
		for ( int size=n; size>1; --size ) {
			// avert starvation when origHints are converted into newHints
			if ( origHints.size() < size )
				break;
			// oldHints are a possible combo of size hints among regionHints
			final LockingHint[] oldHints = new LockingHint[size];
			// foreach possible combination of size hints in our n hints
			LOOP: for ( int[] perm : permuter.permute(n, new int[size]) ) {
				// build an array of this combo of Locking hints
				for ( int i=0; i<size; ++i )
					oldHints[i] = (LockingHint)origHints.get(perm[i]);
				// build an Idx of all the cells in these Locking hints
				lockingIdx.set(oldHints[0].indices);
				for ( int i=1; i<size; ++i )
					lockingIdx.or(oldHints[i].indices);
				// search for a Hidden Pair or Triple depending on idx.size
				switch ( lockingIdx.size() ) {
				case 2:
					if ( hiddenPair.search(region, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), newHints
							  , oldHints, size, lockingIdx) ) {
						removeAll(oldHints, origHints);
						break LOOP; // we are done here!
					} else if ( redsAllShareARegion(oldHints) ) { // CAS_A
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(grid, this
								, oldHints, isPointing));
						removeAll(oldHints, origHints);
						break LOOP; // we are done here!
					}
					break;
				case 3:
					if ( hiddenTriple.search(region, accu)
					  && addElims((HiddenSetHint)accu.peekLast(), newHints
							  , oldHints, size, lockingIdx) ) {
						removeAll(oldHints, origHints);
						break LOOP; // we are done here!
					} else if ( redsAllShareARegion(oldHints) ) { // CAS_A
						// create a new "summary" hint from theseHints
						newHints.add(new SiameseLockingHint(grid, this
								, oldHints, isPointing));
						removeAll(oldHints, origHints);
						break LOOP; // we are done here!
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
			//     use a HintCache (TreeSet by numElims descending); which we would
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
		grid = null;
		maybes = null;
		regions = null;
		return result;
	}

	// remove all $toRemove from $from
	private static void removeAll(final LockingHint[] toRemove, final ArrayList<AHint> from) {
		for ( LockingHint h : toRemove )
			from.remove(h);
	}

	/**
	 * Do all the reds of all these LockingHints share a common region?
	 * If not then its not Siamese, just disjunct hints from one region,
	 * which is rare but does happen (about a dozen times in top1465).
	 *
	 * @param hints an array of two+ LockingHint to examine
	 * @return do all reds in all locking hints share a common region
	 */
	private boolean redsAllShareARegion(final LockingHint[] hints){
		int crs = hints[0].reds.commonRibs();
		for ( int i=1,n=hints.length; i<n; ++i )
			crs &= hints[i].reds.commonRibs();
		return crs > 0; // 27bits (NEVER negative)
	}

	/**
	 * Get an array of grid.regions at RIBS.
	 *
	 * @param gridRegions grid.regions of the grid we search
	 * @param ribs Region Indexes BitSet, I expect from {@link Idx#commonRibs }
	 *  but there may be other uses for ribs that are yet to come to light.
	 *  This is a 27 bit bitset, one for each index in the Grid.regions array.
	 * @return new ARegion[] of regions at RIBS in this grid.
	 */
	private ARegion[] regions(final int ribs) {
		final int n = Integer.bitCount(ribs); // 27 bits
		if ( n == 0 )
			return Regions.EMPTY_ARRAY;
		final ARegion[] result = new ARegion[n];
		int cnt = 0;
		for ( int i=0; cnt<n && i<27; ++i )
			if ( (1<<i & ribs) > 0 )
				result[cnt++] = regions[i];
		assert cnt == n;
		return result;
	}

	/**
	 * Add my eliminations to hisHint; and claim redValues from common regions.
	 * <pre>
	 * NB: done in a method just to not repeat the code (keeps growing).
	 * NB: HiddenSets were added later, so I am not designed just hacked-in.
	 * </pre>
	 *
	 * @param hiddenSetHint the HiddenSetHint to which I add Locking elims
	 * @param newHints to which I add the updated hint, if any
	 * @param mine the Locking hints array
	 * @param size number of hints in mine
	 * @param lockingReds indices of reds in the Locking hints
	 * @return was this hint updated (I bale-out early upon none)
	 */
	private boolean addElims(final HiddenSetHint hiddenSetHint
			, final List<AHint> newHints, final LockingHint[] mine
			, final int size, final Idx lockingReds) {
		assert size > 0;
		IntQueue q;
		final Pots hisReds;
		final Idx empties;
		long budsM0; int budsM1;
		int i, indice, pinkos; // pinkos are candidates eliminated
		// KRC BUG 2020-08-20 888#top1465.d5.mt UnsolvableException from apply.
		// Ignore this HiddenSet hint if hisReds do NOT intersect myReds, to
		// NOT upgrade Claimings into a HiddenSet that is in another Box.
		pinkos = 0;
		i=0; do pinkos |= mine[i].reds.candsOf(); while (++i < size);
		final int hdnSetCands = hiddenSetHint.hdnSetCands;
		if ( (hdnSetCands & pinkos) == 0 )
			return false;
		// add my Pointing/Claiming eliminations to the HiddenSetHints reds.
		// Note that I (unusually) add new elims to the existing Pots instance,
		// which is still referenced by hisHint: ergo they are not even my Pots,
		// which is VERY poor form, so @stretch: Create a new hint from two old
		// ones, rather than modify the other mans hint, for tracabilty.
		hisReds = hiddenSetHint.reds;
		i=0; do hisReds.upsertAll(mine[i].reds); while (++i < size);
		// Get BUDS: an Idx of cells that see all cells in the HiddenSet.
		// New elims are limited to these buddy cells.
		assert !hiddenSetHint.greens.isEmpty();
		empties = grid.getEmpties();
		budsM0 = empties.m0;
		budsM1 = empties.m1;
		for ( int g : hiddenSetHint.greens.keySet() )
			if ( ( (budsM0 &= BUDS_M0[g])
			     | (budsM1 &= BUDS_M1[g]) ) < 1L )
				return false;
		BUDS.set(budsM0,budsM1);
		// claim values of the HiddenSet from the other common region, if any.
		// foreach region common to all reds in the Locking hints
		for ( ARegion cmnRgn : regions(lockingReds.commonRibs()) ) {
			cmnRgn.idxOf(hdnSetCands, IDX).and(BUDS).andNot(lockingReds);
			for ( q=IDX.indices(); (indice=q.poll()) > QEMPTY; )
				hisReds.upsert(indice, maybes[indice] & hdnSetCands, DUMMY);
		}
		// and add hisHint to newHints.
		newHints.add(hiddenSetHint);
		return true;
	}

	// allows the hacu to add hints to the accu
	boolean add(final LockingHint h) {
		return accu.add(h);
	}

}
