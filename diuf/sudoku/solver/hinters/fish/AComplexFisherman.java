/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Config;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;
import static diuf.sudoku.Grid.NUM_REGIONS;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import static diuf.sudoku.Regions.types;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.CachingHintsAccumulator;
import diuf.sudoku.solver.accu.CachingHintsAccumulator.Mint;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.IHinter.DUMMY;
import static diuf.sudoku.solver.hinters.fish.FishType.fishTypes;
import static diuf.sudoku.solver.hinters.fish.FishType.isSashimi;
import diuf.sudoku.utils.IntQueue;
import diuf.sudoku.utils.Log;

/**
 * Abstract Complex Fisherman implements fields and methods common to
 * FinnedFisherman, FrankenFisherman, MutantFisherman, KrakenFisherman,
 * and MutantJellyfishKrakenFisherman.
 *
 * @author Keith Corlett 2023-10-30
 */
public abstract class AComplexFisherman extends AHinter {

	// fields shared between addRegions and search. Created and destoyed
	// in findHints to reduce my heap footprint between calls.

	// number of eligible bases
	protected int numBases;
	// array of all possible base units (indices in Grid.regions)
	protected int[] baseUnits;
	// unitBases: Grid.regions.index -> "0..numBases" index. Biindexuality!
	protected int[] unitBases;
	// vs in bases; v is shorthand for the fish candidate value
	protected long[] baseVsM0;
	protected int[] baseVsM1;

	// number of allCovers
	protected int numAllCovers;
	// array of all eligible cover regions (indices in Grid.regions)
	protected int[] allCoverUnits;
	// indices of grid.cells maybe v (Fish candidate value) in allCovers
	protected long[] allCoverVsM0;
	protected int[] allCoverVsM1;

	// cmnBudsM* are output by cmnBuds method.
	protected long cmnBudsM0;
	protected int cmnBudsM1;

	protected boolean useCache;
	protected final CachingHintsAccumulator hints = new CachingHintsAccumulator();

	// max fins in fish + 1 (a ceiling)
	// fyi: a fin is an extra v in a base
	protected final int maxFins;

	// maximum number of endofins (candidates in multiple bases).
	// a couple or few less than maxFins seems to work best.
	protected final int maxEndofins; // ceiling

	protected AComplexFisherman(final Tech tech, final boolean useCache) {
		super(tech);
		assert tech.isFishy;
		this.useCache = useCache && CFG.getBoolean(Config.isCachingHints, T);
		this.maxFins = CFG.getInt(Config.maxFins, 7)+1; // ceiling
		this.maxEndofins = CFG.getInt(Config.maxEndofins, 4)+1; // ceiling
	}

	@Override
	public boolean setCaching(final boolean useCache) {
		final boolean pre = this.useCache;
		this.useCache = useCache;
		if ( pre && !useCache && hints!=null )
			hints.clear();
		return pre;
	}

	private void setMyFields() {
		// array of all possible base units (indices in Grid.regions)
		baseUnits = new int[NUM_REGIONS];
		unitBases = new int[NUM_REGIONS];
		baseVsM0 = new long[NUM_REGIONS]; // vs in baseUnits
		baseVsM1 = new int[NUM_REGIONS];
		// array of all eligible cover units (indices in Grid.regions)
		allCoverUnits = new int[NUM_REGIONS];
		allCoverVsM0 = new long[NUM_REGIONS]; // vs in allCovers
		allCoverVsM1 = new int[NUM_REGIONS];
	}

	/**
	 * Clear everything that's set in setMyFields, to avoid any hangovers.
	 */
	private void clearMyFields() {
		baseVsM0 = allCoverVsM0 = null;
		baseUnits = unitBases = baseVsM1 = allCoverUnits = allCoverVsM1 = null;
	}

	abstract boolean findComplexFishHints();

	/**
	 * I serve as a fault barrier for the Mint exception that is thrown by the
	 * CachingHintsAccumulator when it really wants to say "Fuck Off, I'm full"
	 * but is far too polite. I am French, you see.
	 * <p>
	 * I'm a demipotic response to a Creasotic question. Those who are not
	 * pythonically challenged will get it. It's a cross-generational simile.
	 * There's a blue whale around somewhere, and possibly a bowl of petunias.
	 *
	 * @see Mint javadoc for a discussion.
	 *
	 * @return any hint/s found
	 */
	private boolean gastroPatronum() {
		boolean result;
		try {
			result = findComplexFishHints();
		} catch (Mint eaten) { // Bring me a bucket!
			result = true;
		}
		return result;
	}

	/**
	 * Find all Finned fish upto my size in the $grid, adding any hints to
	 * $accu. FinnedFisherman finds only Finned fish. My size is Tech.degree
	 * that was passed to my constructor.
	 * <p>
	 * foreach candidate in 1..9: {@link #search(boolean)} for Fish.
	 * Search rows first, then (except Mutant) cols.
	 * If accu.isSingle then exit-early when the first hint is found.
	 *
	 * @return were any hint/s found
	 */
	@Override
	public boolean findHints() {
		boolean result = false;
		try {
			setMyFields();
			if ( useCache ) {
				try {
					if ( hints.check(grid, accu) ) // 11.13% hit rate
						return true;
					if ( gastroPatronum() ) {
						accu.add(hints.poll());
						return true;
					}
					return false;
				} catch (Exception ex) {
					Log.whinge("WARN: "+Log.me()+": cache disabled!", ex);
					useCache = false;
				}
			}
			if ( gastroPatronum() ) {
				result = true;
				accu.add(hints.poll());
			}
		} finally {
			clearMyFields();
		}
		return result;
	}

	/**
	 * Add eligible bases to baseUnits, and eligible covers to allCoverUnits.
	 *
	 * @param bases rows/cols
	 * @param covers cols/rows
	 * @param value the Fish candidate value (the value to search on)
	 */
	protected void addRegions(final ARegion[] bases, final ARegion[] covers, final int value) {
		Idx x;
		for ( ARegion base : bases )
			if ( base.numPlaces[value] > 1 ) {
				baseUnits[numBases] = base.index;
				baseVsM0[numBases] = (x=base.idxs[value]).m0;
				baseVsM1[numBases++] = x.m1;
			}
		for ( ARegion cover : covers )
			if ( cover.numPlaces[value] > 1 ) {
				allCoverUnits[numAllCovers] = cover.index;
				allCoverVsM0[numAllCovers] = (x=cover.idxs[value]).m0;
				allCoverVsM1[numAllCovers++] = x.m1;
			}
	}

	/**
	 * Create a new hint.
	 *
	 *
	 * @param value
	 * @param usedBases
	 * @param usedCovers
	 * @param coverLevel
	 * @param dlM0 deletes
	 * @param dlM1
	 * @param skM0 sharks
	 * @param skM1
	 * @param acceptNothingToDelete if true throw an UnsolvableException if
	 *  both deletes and sharks are empty, if false
	 * @param fnM0 fins
	 * @param fnM1
	 * @param cvM0 covers
	 * @param cvM1
	 * @param vsM0 bases
	 * @param vsM1
	 * @param efM0 endofins
	 * @param efM1
	 * @return a new ComplexFishHint
	 * @throw UnsolvableException if no hints or sharks.
	 */
	protected ComplexFishHint hint(final int value, final int usedBases
		, final int usedCovers, final int coverLevel
		, final long dlM0, final int dlM1 // deletes
		, final long skM0, final int skM1 // sharks
		, final boolean acceptNothingToDelete
		, final long fnM0, final int fnM1 // fins
		, final long cvM0, final int cvM1 // covers
		, final long vsM0, final int vsM1 // bases
		, final long efM0, final int efM1 // endofins
	) {
		// shifted candidate value
		final int sv = VSHFT[value];
		// eliminations: cells with values to be eliminated from them
		final Pots reds = new Pots();
		// normal operation: just add the deletes and sharks to reds,
		// and then return null (no hint) if reds come-out null or empty.
		// Eliminate candidates in covers except bases.
		if ( !reds.upsertAll(dlM0,dlM1, maybes, sv, DUMMY)
		   & !reds.upsertAll(skM0,skM1, maybes, sv, DUMMY)
		   & !acceptNothingToDelete)
			throw new UnsolvableException("No deletes or sharks");
		// get the regions from the used boolean arrays
		final ARegion[] bases = Regions.used(usedBases, grid);
		final ARegion[] covers = Regions.used(usedCovers, grid);
		// fins: vs in bases and not covers (extra vs)
		final boolean anyFins = (fnM0|fnM1) > 0L;
		final FishType type = fishTypes(types(bases), types(covers)
			, anyFins && isSashimi(bases, cvM0, cvM1)
			, anyFins)[coverLevel - 2];
		// green corners: vs in bases and covers
		final Pots greens = new Pots(vsM0&cvM0, vsM1&cvM1, maybes, VSHFT[value], DUMMY);
		final Pots blues = new Pots(fnM0,fnM1, maybes, sv, DUMMY);
		// yellow sharks: vs in two+ covers
		final Pots yellows = new Pots(skM0,skM1, maybes, sv, DUMMY);
		// purple endofins: vs in two+ bases
		final Pots purple = new Pots(efM0,efM1, maybes, sv, DUMMY);
		// paint eliminations red, especially sharks!
		reds.removeFromAll(yellows, greens, blues, purple);
		// paint sharks yellow, not green or blue or purple
		yellows.removeFromAll(greens, blues, purple);
		// create the hint
		return new ComplexFishHint(grid, this, type, value, bases, covers
				, reds, greens, blues, purple, yellows);
	}

	/**
	 * Sets cmnBudsM0,cmnBudsM1 to common buddies of the virtual Idx m0,m1.
	 * <p>
	 * Note that the two cmnBudsM* fields are my output, because I am lazy,
	 * and I don't want to create a return instance which instantly becomes
	 * garbage, to take some pressure of the GC. I think it's faster this
	 * way, in a method that is totally hammered.
	 *
	 * @param m0 mask0
	 * @param m1 mask1
	 * @param rM0 result mask0
	 * @param rM1 result mask1
	 * @return any
	 */
	protected boolean cmnBuds(final long m0, final int m1, long rM0, int rM1) {
		int i;
		// initialise result to "pre", taking-out "the set", to stop sooner.
		// nb: rM* are locals, not fields, coz locals are a bit faster.
		for (final IntQueue q=new Idx.MyIntQueue(m0,m1); (i=q.poll())>QEMPTY;)
			if ( ( (rM0 &= BUDS_M0[i])
				 | (rM1 &= BUDS_M1[i]) ) < 1L )
				return false;
		// cmnBudsM* (my output) are fields.
		cmnBudsM0=rM0; cmnBudsM1=rM1;
		return true;
	}

	/**
	 * Returns are there any common buddies of the virtual Idx m0,m1.
	 *
	 * @param m0 mask0
	 * @param m1 mask1
	 * @param rM0 result mask0
	 * @param rM1 result mask1
	 * @return any
	 */
	protected boolean anyCmnBuds(final long m0, final int m1, long rM0, int rM1) {
		int i;
		// initialise result to "pre", taking-out "the set", to stop sooner.
		// nb: rM* are locals, not fields, coz locals are a bit faster.
		for (final IntQueue q=new Idx.MyIntQueue(m0,m1); (i=q.poll())>QEMPTY;)
			if ( ( (rM0 &= BUDS_M0[i])
				 | (rM1 &= BUDS_M1[i]) ) < 1L )
				return false;
		return true;
	}

	/** An entry in the recursion stack of the covers search. */
	protected static class StackEntry {
		/** index of the previous region (to unuse when done) */
		int prevIndex = -1;
		/** index of the current region */
		int index;
		/** Exploded Idx of vs in base/cover */
		long vsM0; int vsM1;
		/**
		 * Exploded Idx: endofins in bases, sharks in covers <br>
		 * endofins are cells in two+ bases <br>
		 * sharks (cannibles): in two+ covers, hence eliminatable
		 */
		long xxM0; int xxM1;
		// for debug only, not used in code
		@Override
		public String toString() {
			return ""+index+"/"+prevIndex+": "+Idx.toString(vsM0, vsM1);
		}
	}

}
