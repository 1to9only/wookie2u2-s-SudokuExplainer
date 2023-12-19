/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Config;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import java.util.LinkedList;

/**
 * I cache hints for {@link diuf.sudoku.solver.hinters.fish.KrakenFisherman }.
 * Asat 2023-08-25 this is my ONLY usage, subject to change without notice.
 * <p>
 * It is possible to cache hints mostly within the bounds of the IAccumulator
 * interface, adding only a check method, to check for hints, so the reference
 * type in KrakenFisherman.findHints is CachingHintsAccumulator, but everything
 * else (all the add calls) see a plain IAccumulator. Simples.
 * <p>
 * The HintsAccumulator class is no-longer final, to make me possible.
 *
 * @author Keith Corlet 2023-06-17
 */
public class CachingHintsAccumulator extends HintsAccumulator {

	// It's only waffer thin! So we need only one of them.
	private static final Mint MINT = new Mint();

	// are cached hints still current?
	private int numSet;
	private long puzzleId;

	// I ignore hints with only repeat eliminations
	private final Pots knownElims = new Pots();

	// The maximum number of hints to cache
	private final int maxHints;

	// what I'm trying to avert is KrakenFisherman finding
	// the same elims a hundred bloody times.
	private final int maxAdds;
	private int numAdds;

	/**
	 * Constructor taking the default settings.
	 */
	public CachingHintsAccumulator() {
		this( Math.min(Math.max(1, CFG.getInt(Config.hintsCacheSize, 4)), 512)
			, Math.min(Math.max(1, CFG.getInt(Config.hintsCacheBored, 8)), 1024) );
	}

	/**
	 * Constructor.
	 *
	 * @param maxHints maximum number of hints with distinct eliminations
	 * @param maxAdds maximum number of times add is called before I pop
	 */
	public CachingHintsAccumulator(final int maxHints, final int maxAdds) {
		super(new LinkedList<AHint>());
		this.maxHints = maxHints;
		this.maxAdds = maxAdds;
	}

	/**
	 * My add method either returns true or it throws a Creasotic Mint.
	 *
	 * @param hint to add
	 * @return false meaning keep searching
	 * @throws Mint meaning "Fuck Off, I'm full"
	 */
	@Override
	public boolean add(final AHint hint) {
		// first rule of IAccumulator is ignore null hints.
		if ( hint == null )
			return false; // hint not added.
		// I'm bored
		if ( ++numAdds > maxAdds ) {
			numAdds = 0;
			super.add(hint); // add the hint (ignoring retval)
			if ( super.any() )
				throw MINT;
			return false; // hint not added. BFIIK why. Ask super.
		}
		// ignore hints with only repeat eliminations.
		if ( !knownElims.upsertAny(hint.reds) )
			return false; // keep searching!
		// reset bored on each new elimination
		numAdds = 0;
		// add the hint
		super.add(hint);
		// Creasotic proctology
		if ( super.size() > maxHints )
			throw MINT;
		return true; // hint added
	}

	/**
	 * check transfers ONE cached-hint (if exists) to the actual IAccumulator
	 * that was passed into findHints. It checks if hints are "still fresh",
	 * meaning that no cell has been set in the grid since these hinters where
	 * collected, and that this hint still eliminates something.
	 * <p>
	 * CachingHintsAccumulator handles eliminations only; it does NOT handle
	 * setPots, though it could do so with a simple code change: just check for
	 * fresh setPots (setPots cell/s are still empty). I havent done this coz I
	 * use CachingHintsAccumulator only in KrakenFisherman which is elims only,
	 * and its faster this way; just NOT a complete hint-caching solution.
	 * A CachingBigHintsAccumulator might best, I think.
	 *
	 * @param grid the grid we search
	 * @param accu the actual IAccumulator that was passed into findHints
	 * @return
	 */
	public boolean check(final Grid grid, final IAccumulator accu) {
		// if same puzzle, with no more set cells, then check the cache
		if ( grid.numSet==numSet && grid.puzzleId==puzzleId ) {
			AHint hint;
			while ( (hint=list.poll()) != null )
				if ( hint.reds.anyCurrent(grid) ) {
					accu.add(hint);
					return true; // cache hit: regardless of what accu says
				}
			// cache miss, so fall through
		}
		// puzzle changed, or more cell/s set, so any existing hints are stale
		numSet = grid.numSet;
		puzzleId = grid.puzzleId;
		knownElims.clear();
		numAdds = 0;
		list.clear();
		return false; // cache miss
	}

	public void reset() {
		// puzzle changed, or more cell/s set, so any existing hints are stale
		numSet = numAdds = 0;
		puzzleId = 0L;
		knownElims.clear();
		list.clear();
	}

	/**
	 * The Mint RuntimeException is a comedic response to a serious problem.
	 * The comedy is available to those who are not pythonically challenged.
	 * The problem is KrakenFishermans CachingHintsAccumulator gets too-many
	 * hints (each large), eventually causing an OutOfMemoryError!
	 * <p>
	 * The batch wants the first hint. In GUI we want the most effective hint,
	 * but we dont want to wait forever for the calculation thereof. The simple
	 * answer is to set an arbitrary capacity limit. Currently that limit is 4,
	 * but YMMV. Remember that, even in the GUI, 99% of the time, only ONE hint
	 * is applied, so waiting for hundreds of hints is nuts.
	 * <p>
	 * The hintsCacheSize setting is 4 currently, which means that after the
	 * fourth hint is added to the cache, the cache throws a Mint exception,
	 * stopping the current search. It will be rare that more than four hints
	 * are used, and if so then we start the search again from the top. I think
	 * the best size satisfies 90% of repeat-requests. The other 10% can search
	 * twice.
	 * <p>
	 * In order to get added to the cache a hint must eliminate something that
	 * has not already been eliminated by hints already cached. Whenever a hint
	 * is added the "bored count" is reset. Read on...
	 * <p>
	 * There's a counter in add that throws a Mint exception when it's called
	 * n-times to add hints that don't have any new eliminations. I get bored
	 * easily, so the hintsCacheBored setting is 8 currently. This means that
	 * hinters (the Krakens) which find many hints with the same eliminations
	 * get cut short and whatever is in the cache gets returned rather than
	 * sitting patiently waiting for more hints that will probably never be
	 * used.
	 * <p>
	 * Note that I am NOT sure that these settings are "fastest". They're just
	 * a guess. I know not how to "tune it", but I suspect that tuning it might
	 * be a good idea, if I could just work-out how. Trial-n-error is effective
	 * but slow and painful. Sigh.
	 */
	public static class Mint extends RuntimeException {
		private static final long serialVersionUID = 1313424200099L;
		public Mint() {
			super("Fuck Off, I'm full!");
		}
	}

}
