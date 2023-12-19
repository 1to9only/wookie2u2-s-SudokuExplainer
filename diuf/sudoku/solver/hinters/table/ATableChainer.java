/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.OFF;
import static diuf.sudoku.Ass.ON;
import diuf.sudoku.Config;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Idx.MASKED81;
import static diuf.sudoku.Idx.MASKOF;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.CachingHintsAccumulator;
import diuf.sudoku.solver.accu.CachingHintsAccumulator.Mint;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.FunkyAssSetFactory.newFunkyAssSet;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IntHashSet;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedFifoQueue;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract (Kraken) Table Chainer, exposes the TABLES to public, and exposes
 * fields to my subclasses.
 * <p>
 * The {@link TableReduction} class extends me.
 * <p>
 * The {@link diuf.sudoku.solver.hinters.fish.KrakenFisherman}
 * and {@link diuf.sudoku.solver.hinters.fish.KrackFisherman}
 * classes also use my {@link #TABLES}, but not my fields.
 * They have there own fields, rather confusingly, of the same name.
 *
 * @author Keith Corlett 2023-10-30
 */
public abstract class ATableChainer extends AHinter
		implements diuf.sudoku.solver.hinters.IPrepare
		, diuf.sudoku.solver.hinters.ICleanUp
{

//	public static final long[] COUNTS = new long[12];

	/**
	 * Set NO_CHANGED_VALUES_HACK to true to suppress this dirty little hack.
	 * false makes KrackFisherman take 25% of time to find ~60% of hints, and
	 * personally I will accept anything that makes Krakens faster. But I am
	 * well biased against Krakens. I think Krakens are stupid and WRONG. So
	 * flick this switch if you like waiting. I am impatient so false for me.
	 */
	public static final boolean NO_CHANGED_VALUES_HACK = false;

	// TABLES contains the consequences of initialOn: $examine+$candidate.
	// His .elims field contains all OFFs caused by: $examine+$candidate.
	// elims = TABLES.ons[candidate][indice].elims[valueEliminated];
	//         the TABLES.ons indexes identify the initialOn; and elims are an
	//         Idx exploded into a long[2] for speed by valueEliminated.
	// NOTE: public static means shared amongst all comers, including subclass
	// StaticReduction, fish.KrackenFisherman and fish.KrackFisherman.
	public static final Tables TABLES = Tables.getInstance();

	/**
	 * true means hints are cached, false for straight-through. Most test-cases
	 * pre-exist cache *= broken; hence useCache=false in test-cases. Fight!
	 */
	private boolean useCache = true;

	/**
	 * The hints cache stores hints that are found by this findHints call and
	 * uses them to satisfy the next findHints call. This makes sense in table
	 * chainers because initialising the TABLES is 90% of the work, so stopping
	 * the search at the first hint is silly. Instead we complete the search,
	 * cache the hints, and serve them up next time. Thus we avoid initialising
	 * the tables about 11% of the time, which is faster overall.
	 * <p>
	 * The downside is hints "go stale" in the interveening period, so
	 * {@link CachingHintsAccumulator#check} checks an elim still exists before
	 * adding each hint to the IAccumulator.
	 */
	protected final CachingHintsAccumulator hints = new CachingHintsAccumulator();

	/**
	 * A cache of Eff's, by [indice][value].
	 */
	protected static Set<Eff>[][] effsCache;

	/**
	 * Constructor.
	 *
	 * @param tech the Tech that you implement
	 * @param useCache true means hints are cached, false for straight-through
	 */
	public ATableChainer(final Tech tech, final boolean useCache) {
		super(tech);
		this.useCache = useCache && CFG.getBoolean(Config.isCachingHints, T);
	}

	/**
	 * Prepare to solve the puzzle in grid with this lesbianSlaver.
	 * <p>
	 * LesbianSlavers sold separately.
	 *
	 * @param grid to solve
	 * @param logicalSolver to solve with
	 */
	@Override
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		// reinitialise for each puzzle
		TABLES.invalidate();
	}

	/**
	 * cleanUp after each puzzle is solved.
	 */
	@Override
	public void cleanUp() {
		hints.reset();
		effsCache = null;
		effectsCache = null;
	}

	/**
	 * Does this ATableChainer subclass use the hints cache?
	 *
	 * @param useCache true to cache, false for straight-through
	 * @return the previous setting
	 */
	@Override
	public boolean setCaching(final boolean useCache) {
		final boolean pre = this.useCache;
		this.useCache = useCache;
		if ( pre && !useCache && hints!=null )
			hints.clear();
		return pre;
	}

	/**
	 * Get the list of hints out of my cache. Sigh.
	 *
	 * @return my hints
	 */
	LinkedList<AHint> getHints() {
		return hints.getList();
	}

	/**
	 * isKrakHead disables me in Generate.
	 *
	 * @return Yes, I am a KrakHead!
	 */
	@Override
	public boolean isKrakHead() {
		return true;
	}

	/**
	 * Each of my subclasses implements findTableHints to find its specific
	 * type of hints in the TABLES. He adds hints to the hints cache, not to
	 * the normal accu. The hints cache throws Mint when full (CFG defined).
	 * I handle the wafer thin Mint, using magic.
	 *
	 * @return any hints found
	 */
	public abstract boolean findTableHints();

	/**
	 * All I do is call findTableHints and handle the Mint exception. I am
	 * the demintor.
	 * <p>
	 * This method serves as the fault-boundary for the Mint exception that
	 * is thrown by the CachingHintsAccumulator when it really wants to say
	 * "Fuck off, I'm full" but is too polite. I am French, you see.
	 *
	 * @return any hint/s found
	 */
	private boolean gastroPatronum() {
		boolean result;
		try {
			result = findTableHints();
		} catch ( Mint eaten ) {
			result = true; // bucket provided
		}
		return result;
	}

	/**
	 * I manage the cache. findTableHints finds hints in the TABLES.
	 *
	 * @return any hints found.
	 */
	@Override
	public boolean findHints() {
		if ( useCache ) {
			try {
				if ( hints.check(grid, accu) ) // 11.13% hit rate
					return true;
				hints.clear();
				TABLES.initialise(grid, Run.isGenerator(), true);
				if ( gastroPatronum() ) {
					accu.add(hints.poll());
					return true;
				}
				return false;
			} catch (Exception ex) {
				Log.whinge("WARN: "+Log.me()+": cache disabled!", ex);
				ex.printStackTrace(System.err);
				useCache = false;
			}
		}
		TABLES.initialise(grid, Run.isGenerator(), true);
		if ( gastroPatronum() ) {
			accu.addAll(hints.getList());
			hints.clear();
			return true;
		}
		return false;
	}

	// ========================== EFFECTS ==========================

	/**
	 * Returns a set of the effects of assuming that $indice+$v.
	 * <p>
	 * This is several orders of magnitude slower than searching Tables,
	 * hence is used ONLY once we know a hint exists, to get the consequences
	 * to go into the hint.
	 *
	 * @param grid
	 * @param indice
	 * @param value
	 * @return
	 */
	private static IAssSet getEffectsImpl(final Grid grid, final int indice, final int value) {
		int ai, av; // ass.indice, ass.value
		Ass kid; // an effect of Ass a (the parent).
		final int[] maybes = grid.maybes;
		final Cell[] cells = grid.cells;
		final int[] sizes = grid.sizes;
		// STOPPING PROBLEM: Avert chasing endless-loops in consequence-chains.
		// effects are my result, and also provide uniqueness. IAssSets are all
		// funky, ie add is addOnly: it just returns false when asked to add an
		// existing item (unlike java.util.Set implementations which update the
		// element and return false). Hence we can if effects.add(kid) then
		// queue.add(kid). Simples.
		final IAssSet effects = newFunkyAssSet(128, 0.75F, OFF);
		final MyLinkedFifoQueue<Ass> queue = new MyLinkedFifoQueue<>();
		Ass ass = new Ass(indice, value, ON); // the initialOn
		effects.add(ass); // add the initialOn to avert endless loops
		do {
			ai = ass.indice;
			av = ass.value;
			if ( ass.isOn ) {
				for ( int otherValue : VALUESES[maybes[ai] & ~VSHFT[av]] ) {
					if ( effects.add(kid=new Ass(ai, otherValue, OFF, ass)) )
						queue.add(kid);
				}
				for ( Cell sib : cells[ai].siblings ) {
					if ( (sib.maybes & VSHFT[av]) > 0
					  && effects.add(kid=new Ass(sib.indice, av, OFF, ass)) )
						queue.add(kid);
				}
			} else {
				if ( sizes[ai] == 2
				  && effects.add(kid=new Ass(ai, VFIRST[maybes[ai] & ~VSHFT[av]], ON, ass)) )
					queue.add(kid);
				for ( ARegion region : cells[ai].regions ) {
					if ( region.numPlaces[av] == 2
					  && effects.add(kid=new Ass(region.idxs[av].otherThan(ai), av, ON, ass)) )
						queue.add(kid);
				}
			}
		} while ( (ass=queue.poll()) != null );
		return effects;
	}

	/**
	 * Returns a set of the effects of assuming that $indice+$v, cached!
	 *
	 * @param grid
	 * @param indice
	 * @param value
	 * @return
	 */
	public static IAssSet getEffects(final Grid grid, final int indice, final int value) {
		if ( effectsCache == null )
			effectsCache = new IAssSet[GRID_SIZE][VALUE_CEILING];
		if ( effectsCache[indice][value] == null )
			effectsCache[indice][value] = getEffectsImpl(grid, indice, value);
		return effectsCache[indice][value];
	}
	private static IAssSet[][] effectsCache;

	// ========================== EFFS ==========================

	/**
	 * Recursively find all of the effects of the parent assumption.
	 *
	 * @param effects to add effects to
	 * @param parent to find the effects of
	 */
	private static void effsRecurse(final Set<Eff> effects, final Eff parent) {
		final Eff[] kids = parent.kids;
		for ( int k=0,K=parent.numKids; k<K; ++k ) {
			if ( effects.add(kids[k]) && kids[k].hasKids )
				effsRecurse(effects, kids[k]);
		}
	}

	/**
	 * Find all of the consequences of setting indice to value, recursively.
	 *
	 * @param indice identifying the cell to set
	 * @param value the presumed value of cell
	 * @return the consequences
	 */
	private static Set<Eff> effsOf(final int indice, final int value) {
		final Eff on = TABLES.ons[value][indice];
		if ( on==null || !on.hasKids )
			return null;
		final Set<Eff> result = new FunkyTreeSet<>();
		result.add(on);
		effsRecurse(result, on);
		return result;
	}

	/**
	 * I cache the effects of each cell-value, fetching each ONCE.
	 *
	 * @param indice to get effects of
	 * @param value to get effects of
	 * @return the effects of setting indice to value
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Set<Eff> effsCached(final int indice, final int value) {
		if ( effsCache == null )
			effsCache = new Set[GRID_SIZE][VALUE_CEILING];
		if ( effsCache[indice][value] == null )
			effsCache[indice][value] = effsOf(indice, value);
		return effsCache[indice][value];
	}

	/**
	 * Funky: a {@code TreeSet<T>} whose add is addOnly, ie add just adds,
	 * ie add does NOT update existing items, instead it just returns false.
	 * java.util sets update the existing item and return false, which IMHO
	 * is just plain wrong. They focus on number of items in the set. I focus
	 * on has the set changed. It's just a difference of opinion. That's all.
	 * Also retainAll should return !isEmpty, and likewise addAll; and probably
	 * also removeAll especially when it takes a predicate. Sigh.
	 */
	private static class FunkyTreeSet<T> extends TreeSet<T> {
		private static final long serialVersionUID = 23509289323001L;
		@Override
		public boolean add(final T e) {
			return e!=null && !super.contains(e) && super.add(e);
		}
	}

	/**
	 * The Tables contain all possible <b>static XY Forcing Chains</b>
	 * (both ONs and OFFs), once initialised.
	 * <pre>
	 * Definitions of static, X, Y, and Forcing Chains are in chain package,
	 * where they belong. I am not. I am Fish based chains in tables. Sigh.
	 *
	 * The Tables are a fish answer to a chains question, in tables, mainly
	 * because I am too stupid (and lazy) to workout how to do it otherwise.
	 * This is cumbersome, but it works. This can be done better, I think.
	 *
	 * There be ONE instance of Tables (a singleton), that is shared by all
	 * ATableChainer subclasses: {@link TableReduction} and the Krakens.
	 *
	 * Note that the chains here-in are static only. Each link I detect exists
	 * in the grid as it was passed to me. When a maybe is OFFed it is NOT
	 * removed from maybes or from places; missing subsequent bivalue cells
	 * and/or biplaced regions. This results in false negatives. DynamicChains
	 * fixes this, down in the chain package, where it belongs.
	 *
	 * The Tables contain consequences of ALL POSSIBLE assumptions, so when we
	 * use these tables to find the consequences of a SPECIFIC assumption the
	 * search finds and follows consequences that do not exist for this initial
	 * Assumption, hence a Tables search also finds false positives, so it is
	 * indicative only, NOT deterministic.
	 *
	 * A second (correct but slower) search is required to eliminate these
	 * false positives. It took me distant monarch ages to work this out.
	 * How ____ing Embarrassment!
	 *
	 * So given that the Tables produce both false negatives (static chains)
	 * and false positives (alien links) why use them all at? Speed! Its just
	 * faster to NOT create anything in a search that mostly fails, even if the
	 * answer is so wrongful that it requires double checking with the correct
	 * (alien links) but slow (own Asses) way. Briefly slow hinters don't work
	 * without a fast prima-facea filter, coz we don't have time to do it all
	 * correctly, but we do have time to double-check only the indicated ones.
	 *
	 * KRC 2023-10-30 Exhumed fish KrakenTables into tables Tables, because
	 * they are now used in both fish and a table-chainer: StaticReduction.
	 * </pre>
	 *
	 * @author Keith Corlett 2020-10-11
	 * @author Keith Corlett 2023-05-05 Exhumed from Kraken for Superfischerman
	 * @author Keith Corlett 2023-05-30 Reinstate standard hinters over Superfisch.
	 * @author Keith Corlett 2023-06-30 Superfischerman drowned. Back into KrakenFisherman.
	 */
	public static class Tables {

		// If an assert goes-off then increase Q_SIZE by a power of 2
		// 1 2 3  4  5  6   7   8   9   10
		// 2 4 8 16 32 64 128 256 512 1024
		private static final int Q_SIZE = 1<<7; // must be a power of 2
		private static final int Q_MASK = Q_SIZE - 1; // for this trick to work

		/** a little more self-documenting. */
		private static final Eff NO_PARENT = null;

		// theInstance is umm, the ummm, instance.
		private static Tables theInstance;

		/**
		 * Everybody uses {@link ATableChainer#TABLES} so even this factory
		 * method runs once only; but running it twice is no problem, you just
		 * have to make it accessible. The factory method is currently private,
		 * as is my constructor. Just use the public TABLES!
		 *
		 * @return theInstance
		 */
		private static Tables getInstance() {
			if ( theInstance == null )
				theInstance = new Tables();
			return theInstance;
		}

		/**
		 * The grid.numSet for which tables are initialised.
		 * Thus the tables are reinitialised whenever a Cell is set.
		 */
		public int numSet;

		/**
		 * The grid.puzzleID for which tables are initialised.
		 * Thus the tables are reinitialised whenever the puzzle changes.
		 * Presuming that Random.nextLong never produces the same number twice
		 * in a row, which sounds VERY unlikely to me: One in a septillion, so
		 * it WILL happen, shortly after entropy degrades the universe into a
		 * bowl of petunias. Bistromathic probability of a tasty nose-booger.
		 */
		public long puzzleId;

		/**
		 * The offTable is all the OFF Eff(ects).
		 * The first index is the cell indice (concurrent with grid.cells).
		 * The second index is the potential value 1..9.
		 */
		public final Eff[][] offs = new Eff[VALUE_CEILING][GRID_SIZE];

		/**
		 * The onTable is all the ON Eff(ects).
		 * The first index is the cell indice (concurrent with grid.cells).
		 * The second index is the potential value 1..9.
		 */
		public final Eff[][] ons = new Eff[VALUE_CEILING][GRID_SIZE];

		/**
		 * A Set of Contradiction with a "funky" add method that ignores null,
		 * and also ignores any pre-existing Contradiction.
		 */
		public final TreeSet<Contradiction> contradictions = new TreeSet<Contradiction>() {
			private static final long serialVersionUID = 23590761392106L;
			@Override
			public boolean add(Contradiction e) {
				return e!=null && !super.contains(e) && super.add(e);
			}
		};

		/**
		 * The Constructor is private.
		 */
		private Tables() {
		}

		/**
		 * Invalidate the cache that is these Tables, forcing the next call to
		 * {@link #initialise(diuf.sudoku.Grid, boolean, boolean) } to actually
		 * initialise.
		 */
		public void invalidate() {
			numSet = -1;
			puzzleId = -1;
		}

		/**
		 * Repopulate ons/offs with all XY Forcing Chains in the grid.
		 * An "on" is a cells value being set.
		 * An "off" is a maybe elimination.
		 * <p>
		 * The idea is to find all possible ON and OFF effects in grid ONCE.
		 * Then {@link KrakenFisherman#kt1Search}
		 * and {@link KrakenFisherman#kt2Search}
		 * and whatever else
		 * all following kids links, instead of each invocation (hammered)
		 * building there own chains from scratch. I hope this is faster.
		 * <p>
		 * When reading the result the tricky part is stopping, so we "hide"
		 * initialOn.parents (else it endlessly-loops around the net) and
		 * restore them afterwards.
		 * <p>
		 * Tables need to be initialised every time the grid changes, which
		 * sucks coz I'm a bit slow and the resulting dependency-net is a lot
		 * like the previous one. I can't work out HOW to "prune" a net, so I
		 * rebuild it from scratch every time.
		 * <pre>
		 * Pseudocode for XY-chaining:
		 * if p.isOn // the previous assumption is an ON
		 *     1. add an OFF for each other potential value of p.cell
		 *     2. add an OFF for each other possible position of p.value
		 *        in each of p.cells three regions
		 * else // the previous assumption is an OFF
		 *     1. if p.cell has only two potential values then the cell must
		 *        be the other potential value, so add an ON
		 *     2. if any of p.cells 3 regions has two places for p.value then
		 *        that other place must be p.value, so add an ON
		 * endif
		 *
		 * NOTES:
		 * If we know all causes and all effects, we navigate the assumption
		 * tree in either direction. Note these are only static (not dynamic)
		 * chains. Static chains occur only in the grids current state.
		 * Presuming that a cell is not one of its maybes:
		 * * does NOT remove that value from the cells maybes; and
		 * * does NOT remove that cell from the possible places for that value
		 *   in the cells three regions.
		 * </pre>
		 *
		 * @param grid the current grid we are searching
		 * @param force re-initialise! (debug only)
		 * @param withElims true populates ons.elims: Exploded Idxs of OFFs
		 *  caused by each initialOn, by value.
		 * @return did I actually initialise? I do NOT eat RuntimeException
		 */
		public boolean initialise(final Grid grid, final boolean force, final boolean withElims) {
			boolean result = false; // did I initialise.
			// if my cache is dirty, or my caller forces it.
			// ie: the first hinter to call initialise actually initialises.
			// nb: we force in Generate coz puzzle is all over the place.
			if ( numSet != grid.numSet
			  || puzzleId != grid.puzzleId
			  || force
			) {
				// calculate the direct consequences of each possible initialOn
				// and wire them all together into a "forcing net".
				result = initialise(grid);
				// if that worked, and my caller also wants the elims
				if ( result && withElims )
					calculateElims(); // populates elims and sets of initOns
			}
			return result;
		}

		/**
		 * init records the direct effects (kids) of assuming that each
		 * possible cell-value is both:<pre>
		 * * ON (we assume that cell is value) and
		 * * OFF (we assume that cell is NOT value).
		 * </pre>
		 * Both sets of consequences are mashed into the same artifacts,
		 * which is confusing as ____, but still works. You fix it!
		 *
		 * @param grid
		 * @return
		 */
		private boolean initialise(final Grid grid) {
			Eff sibling; // He did it!
			Cell cell;
			int i // indice of current cell (not your usual index)
			  , sv // shiftedValue = VSHFT[v]
			  , o // the other one (He Did It!)
			  , cellMaybes // cell.maybes
			;
			final int[] maybes = grid.maybes;
			final int[] values = grid.getValues();
			final Cell[] cells = grid.cells;
			// 1. construct all possible ON and OFF assumptions.
			i = 0;
			do
				// skip set cells
				if ( maybes[i] > 0 )
					for ( int v : VALUESES[maybes[i]] ) {
						ons[v][i] = new Eff(i, v, ON, NO_PARENT);
						offs[v][i] = new Eff(i, v, OFF, NO_PARENT);
					}
			while (++i < GRID_SIZE);
			// 2. Find the direct consequences of each possible ON Ass.
			//    Ie, populate each Asses kids array.
			i = 0;
			do {
				// skip set cells
				if ( values[i] == 0 ) {
					cell = cells[i];
					cellMaybes = maybes[i];
					for ( int v : VALUESES[cellMaybes] ) {
						sv = VSHFT[v];
						// Assuming that cell is set to value
						// 1. add an OFF for each other potential value of this cell
						for ( int otherValue : VALUESES[cellMaybes & ~sv] )
							ons[v][i].addKid(offs[otherValue][i]);
						// 2. add an OFF for each of my siblings which maybe v
						for ( Cell sib : cell.siblings )
							if ( (sib.maybes & sv) > 0 )
								ons[v][i].addKid(offs[v][sib.indice]);
						// Assuming that value is eliminated from cell
						// 1. if cell has 2 maybes, other is ON
						if ( cell.size == 2 )
							offs[v][i].addKid(ons[VFIRST[cellMaybes & ~sv]][i]);
						// 2. if any cell.regions has 2 places, other is ON
						for ( ARegion r : cell.regions )
							if ( r.numPlaces[v] == 2
							  // -1 actually happened. (He did it!)
							  && (o=r.indices[IFIRST[r.places[v] & ~cell.placeIn[r.rti]]]) > -1
							  // null sibling happened (and not for -1)
							  && (sibling=ons[v][o]) != null )
								offs[v][i].addKid(sibling);
					}
				}
			} while (++i < GRID_SIZE);
			numSet = grid.numSet;
			puzzleId = grid.puzzleId;
			effsCache = null;
			effectsCache = null;
			return true;
		}

		/**
		 * Follow the static forcing chains (the consequences) of setting each
		 * cell in the grid that maybe value to value, storing the eliminations
		 * (my results) in the elims table, for fast look-up in Kraken search.
		 * <p>
		 * To be clear: the elims for each cell-value is a table of all of the
		 * maybes that are eliminated as a consequence of setting this cell to
		 * this value. The Kraken search finds common eliminations for every
		 * element in a "locked set":
		 * <pre>
		 * ONE of these scenarios MUST be true, and they all eliminate X,
		 * hence X is ____ed, therefore we can eliminate X.
		 * </pre>
		 * The elims are pre-calculated because it is MUCH more efficient to do
		 * this consequence-search ONCE per cell-value, than it is to do the
		 * same search per cell-value
		 * <u>per</u> numCombinations(numBases in numEligibleBases)
		 * <u>per</u> numCombinations(numCovers in numEligibleCovers).
		 * <hr>
		 * <pre>
		 * Here's the output from <b>{@link diuf.sudoku.utils.Permutations}</b>
		 * which calculates worste case numPerms(nb in neb) * numPerms(nc*nec)
		 * to show the <u>magnitude</u> of numbers involved in each fish type.
		 * Basically, it tells us why MutantJellyfish are too ____ing slow!
		 * An example of the tyranny of numbers. We fart them on the beaches.
		 * Standard fish:
		 * 2: 36*36         =       1,296
		 * 3: 84*84         =       7,056
		 * 4: 126*126       =      15,876
		 * Franken fish:
		 * 2: 153*153       =      23,409
		 * 3: 816*816       =     665,856
		 * 4: 3,060*3,060   =   9,363,600
		 * Mutant fish:
		 * 2: 351*351       =     123,201
		 * 3: 2,925*2,925   =   8,555,625
		 * 4: 17,550*17,550 = 308,002,500
		 * </pre>
		 */
		private void calculateElims() {
			Eff initOn, ass, kids[]; // the initial On, an ass, his kids
			int value // value
			  , indice // indice
			  , qr,qw // Q read/write index
			  , K, k // kids: size, index
			  , i, v // ass.indice, ass.value
			;
			final Eff[] Q = new Eff[Q_SIZE]; // Englishness!
			long[][] elims, sets; // shorthands
			// get the contradictions collection from the Tables instance.
			// Note that cons is actually a Set so adding twice is no problem.
			final Collection<Contradiction> cons = TABLES.contradictions;
			// any existing contradictions are now "stale"
			cons.clear();
			// populate elims arrays by following static XY Forcing Chains
			// through the kids array of each Eff.
			value = 1;
			do {
				indice = 0;
				do {
					// initOn: the initial ON assumption
					if ( (initOn=ass=ons[value][indice]) != null ) {
						// create the initOn.elims/sets, which I populate
						elims = initOn.elims = new long[VALUE_CEILING][2];
						sets = initOn.sets = new long[VALUE_CEILING][2];
						// examine each cell-value ONCE. isOn is irrelevant!
						final boolean[][] seen = new boolean[VALUE_CEILING][GRID_SIZE];
						// reset queues read and write indexes
						qr = qw = 0;
						// follow XY Forcing Chains from initOn, adding elims,
						// by chasing the kid-links we prepared earlier.
						do {
							i = ass.indice;
							v = ass.value;
							if ( ass.isOn ) { // ass is an ON
								sets[v][MASKOF[i]] |= MASKED81[i];
								if ( !seen[v][i] ) {
									seen[v][i] = true;
									if ( ass.hasKids ) {
										kids = ass.kids;
										K = ass.numKids;
										k = 0;
										do {
											Q[qw] = kids[k]; // an OFF
											qw = (qw+1) & Q_MASK;
											assert qw != qr;
										} while (++k < K);
									}
								} else if ( (elims[v][MASKOF[i]] & MASKED81[i]) > 0L ) {
									cons.add(new Contradiction(initOn, i, v));
								}
							} else { // ass is an OFF
								elims[v][MASKOF[i]] |= MASKED81[i];
								// nb: faster to NOT cache indice & value
								if ( !seen[v][i] ) {
									seen[v][i] = true;
									if ( ass.hasKids ) {
										//++COUNTS[ass.numKids];
										// 0        1        2       3      4
										// 0, 4337262, 1736337, 547660, 84492
										// 0, 64.6797, 25.8932, 8.1670, 1.259%
										// don't cache ass.kids and ass.numKids
										k = 0;
										do {
											Q[qw] = ass.kids[k]; // an ON
											qw = (qw+1) & Q_MASK;
											assert qw != qr;
										} while (++k < ass.numKids);
									}
								} else if ( (sets[v][MASKOF[i]] & MASKED81[i]) > 0L ) {
									cons.add(new Contradiction(initOn, i, v));
								}
							}
							ass = Q[qr];
							Q[qr] = null;
							qr = (qr+1) & Q_MASK;
						} while ( ass != null );
					}
				} while (++indice < GRID_SIZE);
			} while (++value < VALUE_CEILING);
		}

	}

	/**
	 * Eff(ect) extends Ass(umption) to provide an effects list (called kids)
	 * to turn an Ass into a doubly-linked-net, with both cause (Ass.parents)
	 * and effects (Eff.kids), for use in the Tables. Gopher Cleetus?
	 * <p>
	 * A kid is a direct effect of this assumption. A parent is a direct cause
	 * of this assumption. Together they make chains navigable in either
	 * direction: from cause to effect (and thus the whole consequence tree of
	 * any assumption), and from any effect we can walk all the way back to the
	 * root cause (ie the initial assumption), which is the only Ass in the
	 * consequence tree with no parent/s.
	 *
	 * @author Keith Corlett 2020-10-10
	 * @author Keith Corlett 2023-05-05 Exhumed from KrakenFisherman for Fisherman
	 * @author Keith Corlett 2023-06-30 Back into KrakenFisherman
	 */
	public static class Eff extends Ass {

		/**
		 * kidsHashCodes: IntHashSet does NOT grow like a java.util.Set hence
		 * an overpopulated IntHashSet may have VERY long lists. Power-of-2.
		 */
		public final IntHashSet kidsHCs = new IntHashSet(32);

		/**
		 * The Grommetorium.
		 */
		public final Eff[] kids = new Eff[24]; // unsure of length

		/**
		 * Number of Groms.
		 */
		public int numKids;

		/**
		 * Grom on, or Gromless.
		 */
		public boolean hasKids;

		// the following two fields are populated only in the initial Ons.

		/**
		 * indices OFFed by this initial On, by [value][mask].
		 * <p>
		 * This field exists in Offs and non-initial Ons, but remains null.
		 */
		public long[][] elims;

		/**
		 * indices ONed by this initial On, by [value][mask].
		 * <p>
		 * This field exists in Offs and non-initial Ons, but remains null.
		 */
		public long[][] sets;

		/**
		 * The Constructor.
		 * @param indice
		 * @param value
		 * @param isOn
		 * @param parent nullable, the Ass is created with a parents-list anyway.
		 */
		public Eff(final int indice, final int value, final boolean isOn, final Eff parent) {
			super(indice, value, isOn, parent);
		}

		/**
		 * Add this kid to my kids (if its NOT already one of my kids),
		 * and (if so) add me to his parents.
		 * <p>
		 * this.kids is a MyFunkyLinkedHashSet whose add method differs from
		 * java.util.HashMap in its handling of pre-existing elements:<ul>
		 * <li>FunkySet does add-or-(do-nothing-and-return-false); where</li>
		 * <li>java.util.HashSet does add-or-(update-and-return-false).</li>
		 * </ul>
		 */
		void addKid(final Eff kid) {
			// add the kid unless already exists.
			// this relies on Ass.hashCode uniquely identifying each Eff/Ass,
			// which is the case, but is unusual (most hashCodes are lossy).
			// Also we rely on hashCode being a public final field rather than
			// wear the tiny cost of invoking the hashCode() method.
			// This is executed BILLIONS of times so performance is critical;
			// hence I use kidsHCs for distinctness and kids is just an array
			// for its fast array-iterator (avoiding Iterators).
			// NOTE: let it NPE if kid is null
			if ( kidsHCs.add(kid.hashCode) ) {
				hasKids = true;
				kids[numKids++] = kid;
				kid.addParent(this);
			}
		}

		/**
		 * Add a parent to an Eff, where the parents list always pre-exists.
		 *
		 * @param parent Ass to add
		 * @return true, always; no RuntimeExceptions
		 */
		@Override
		public boolean addParent(final Ass parent) {
			assert parent != null;
			if ( firstParent == null )
				firstParent = parent;
			parents.linkLast(parent);
			return true;
		}

	}

}
