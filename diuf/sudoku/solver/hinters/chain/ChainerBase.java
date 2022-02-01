/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Ass.Cause.CAUSE_FOR;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Run;
import diuf.sudoku.SourceID;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Settings.THE_SETTINGS;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.ICleanUp;
import static diuf.sudoku.solver.hinters.Validator.*;
import static diuf.sudoku.utils.Frmt.OR;
import static diuf.sudoku.utils.Frmt.PLUS;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.Log;
import java.util.Iterator;

/**
 * ChainerBase is an abstract engine for searching a Grid for forcing chains.
 * It's extended by UnaryChainer and MultipleChainer to implement all Sudoku
 * solving techniques that involve following a chain of implications persuent
 * to an assumption of the value of a Cell within the Grid, including all types
 * of Forcing Chains and Bidirectional Cycles.
 * <p>
 * This code's a bit faster than that which I inherited from Nicolas Juillerat.
 * On my ~2900 Mhz intel i7, a slightly modified Juillerat took 21 minutes and
 * 54 seconds to solve top1465.d5.mt containing 1465 Sudoku puzzles, about 900
 * of which are too hard to solve manually. On 2019-10-12 in ACCURACY Mode the
 * latest solved top1465.d5.mt in 2 minutes and 10 seconds using Juillerats
 * techniques + NakedQuad. So yeah, just a little bit faster.
 * <p>
 * The -REDO switch (which uses only previously successful hinters read from
 * the log-file of a previous run) brought that down to 2 minutes 3 seconds.
 * <p>
 * The -SPEED -STATS switches (which throws all it's toys out of the pram) took
 * 40.8 seconds to solve those same 1465 puzzles, which is ~3.1% of the time.
 * Butyacannobeat Knuths recursive guesser at 992,370,784, ie 1465 puzzles in
 * just under a bloody second. Lick that one Scoobie!
 * <p>
 * Basically no expense is spared to find hints faster here (even correctness
 * takes a hit on occasion) including replacement of Javas HashSet class with
 * LinkedMatrixAssSet, for when a Hash-head is just too slow. My definition of
 * "correct" is: each hint is the simplest available.
 * <p>
 * If you can do it faster then please do so, but if you even read this code
 * then you MUST share your implementation. No secret squirrel business.
 * <p>
 * KRC AUG 2019 The Chainer used to apply the first found hint, it now always
 * finds all hints, and (when one hint is wanted) returns the most effective
 * (highest score) hint. This is slower but it allows us to step through a
 * solution in the GUI following the same path (hints) that LogicalSolverTester
 * used to solve the puzzle, which is exactly what I want. This change has
 * improved my development life immensely, and I find that my perception of the
 * importance of performance continues to reduce over time.
 * <p>
 * KRC 2019 OCT I'm proud of working-out that the fastest way to figure-out if
 * we already know the conjugate is by calculating the conjugates hashCode then
 * just comparing each calculated Ass.hashCode with it, instead of the old way
 * conjugating each calculated Ass, which double-hammers the Ass constructor to
 * create ship-loads of barely-used garbage Ass's, which end-up bogging the GC.
 * <p>
 * KRC 2011-11-01 I've split the existing Chainer class into UnaryChainer and
 * MultipleChainer, leaving the shared methods in this class, which is now
 * abstract, so is now ChainerBase. I'm doing this because the methods and even
 * fields of Unary and Multiple chaining are disjunct, especially at the upper
 * levels; they really only share the onToOff and OffToOn methods, and the few
 * differences between the two at the bottom levels are handled with call-backs
 * to the two subtypes.
 * <p>
 * This is experiment (FFMS!) worked first time, and it makes the code dryer,
 * and therefore (I hope) a bit easier to follow. A class that's 60% unused in
 * 75% of use-cases is s__t. A class should be focused on solving a specific
 * problem. I just narrowed-down the problem, and simplicity has (I hope, I'm
 * too deep into this s__t to judge) followed. Mind you, using inheritance to
 * make things simpler sounds oxymoronic.
 */
abstract class ChainerBase extends AHinter
		implements ICleanUp
{
//	/** If true && VERBOSE_3_MODE then I write stuff to Log.out */
//	public static final boolean IS_NOISY = false; // check false
//
//	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on.
//	 * @param msg */
//	protected static void noiseln(String msg) {
//		if (Log.MODE >= Log.VERBOSE_3_MODE) {
//			Log.format(NL+msg+NL);
//		}
//	}
//
//	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on.
//	 * @param fmt a PrintStream.format format argument
//	 * @param args a PrintStream.format args list */
//	protected static void noisef(String fmt, Object... args) {
//		if (Log.MODE >= Log.VERBOSE_3_MODE) {
//			Log.format(fmt, args);
//		}
//	}

	/**
	 * If true the typeID field (\\d) is included in the hint-type.
	 * This is for developers to back-trace where a hint came from,
	 * but it is incomprehensible noise to the end-user,
	 * and interferes with the sorting of hints,
	 * So I must ALWAYS be set back to false for a release build!
	 */
	protected static final boolean WANT_TYPE_ID = false; // @check false

	/** Are multiple cell and region chains sought. Multiple means more than 2
	 * potential values, or more than 2 possible positions. */
	protected final boolean isMultiple;
	/** Is this Chainer removing maybes as it goes? */
	protected final boolean isDynamic;
	/** Is this Chainer looking for Dynamic Contradictions. */
	protected final boolean isNishio;
	/** Is this Chainer imbedded (nested) inside another Chainer? */
	protected final boolean noCache;

	/**
	 * I retain a list of hints.
	 */
	private final HintCache cache;

	/**
	 * The number of set cells in the grid when that cache was filled, so that
	 * we can force-refresh the cache each time a cell is set, so that we never
	 * see those annoying outdated hints where a cell is presumed to be one of
	 * it's old maybes, when the cell has now been set. Note that about 90% of
	 * eliminations can be reproduced through a simpler path in a simpler grid,
	 * and I'm willing to write-off the others to avoid displaying what is now
	 * clearly questionable logic, which smart people will question even though
	 * (being the same grid) it still stands-up, despite no longer being there.
	 */
	private int cacheNumSet;

	/**
	 * As per cacheNumSet: grid.source: lineNumber/file of src of crnt puzzle.
	 */
	private SourceID cacheSource;

	/**
	 * Constructs an abstract Chainer: an engine for searching a Sudoku Grid
	 * for forcing chains.
	 * <p>
	 * My cache (an implementation of HintCache) is constructed when the hinter
	 * is created (ie when the LogicalSolver is constructed) based on the
	 * <b>CURRENT</b> isFilteringHints setting, unlike other isFilteringHints
	 * features the caching-implementation used doesn't change when you switch
	 * on/off isFilteringHints, it's set for the life of the LogicalSolver, so
	 * to switch on/off caching you have to set isFilteringHints and then go
	 * into the TechSelectDialog (Options menu ~ Solving techniques...) and
	 * press the [OK] button, which recreates the LogicalSolver.
	 * <p>
	 * When isFilteringHints is true I use a HintCacheFiltered which sorts
	 * hints by there score (number of eliminations) descending, so that the
	 * most effective hint is first.
	 * When isFilteringHints is I use a plain-old HintCacheBasic which returns
	 * hints in "natural order": the order in which they were added.
	 *
	 * @param tech a Tech with isChainer==true,
	 * @param noCache true only when this is an imbedded hinter, ie is nested
	 * inside another hinter.
	 */
	protected ChainerBase(final Tech tech, final boolean noCache) {
		super(tech);
		assert tech.isChainer;
		this.isMultiple	 = tech.isMultiple;
		this.isDynamic	 = tech.isDynamic;
		this.isNishio	 = tech.isNishio;
		// caching breaks the test-cases
		this.noCache = noCache | Run.type == Run.Type.TestCase;
		// NEVER cache in a test-case (say you're the GUI to test the cache!)
		if ( Run.type == Run.Type.TestCase )
			cache = new HintCacheBasic();
		// always cache in the batch
		else if ( Run.type == Run.Type.Batch
		  // or when isFiltering is switched-on
		  || THE_SETTINGS.isFilteringHints() )
			cache = new HintCacheSorted();
		else
			cache = new HintCacheBasic();
		assert degree>=0 && degree<=5;
	}

	/** Do other possible positions for this value get "off"ed. */
	protected boolean isXChain;
	protected void setIsXChain(final boolean isXChain) {
		this.isXChain = isXChain;
	}

	/** Do other potential values of this cell get "off"ed. */
	protected boolean isYChain;
	protected void setIsYChain(final boolean isYChain) {
		this.isYChain = isYChain;
	}

//not_used
//	protected ChainerBase(Tech tech) {
//		this(tech, false, null); // not imbedded
//	}

	/**
	 * Clear out the hints cache.
	 * <p>
	 * This implementation is overridden by both UnaryChainer and
	 * MultipleChainer to call me back, then cleanUp there own s__t.
	 */
	@Override
	public void cleanUp() {
		cache.clear();
	}

	/**
	 * Find any chaining hints in the given grid and add them to hints list.
	 * This method is implemented by my subtypes: UnaryChainer, MultipleChainer
	 * to find the bloody hints already already. It's done this way to allow my
	 * findHints method to cache hints.
	 *
	 * @param grid Grid to search
	 * @param hints HintCache to add hints to.
	 */
	protected abstract void findChainHints(final Grid grid, final HintCache hints);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This implementation follows forcing chains through the grid to the hints.
	 * <p>
	 * Note that hints are cached locally.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		assert grid != null;
		assert accu != null;

		// first try the existing hints (a hint cache)
		// do NOT cache if I'm an imbedded hinter (buggers-up chaining)
		// I do not understand why/how it buggers-up chaining, all I know is
		// that when I put-in the cache NestedUnary produced invalid hints.
		if ( cache instanceof HintCacheSorted // ie, was isFilteringHints on when I was created
		  && cache.size() > 0 // got cache
		  && grid.source == cacheSource // same puzzle
		  && grid.numSet == cacheNumSet // same number of set cells
		  && !noCache // caching breaks imbedded chainers, and test-cases
		) {
			AHint cached;
			// nb: I poll: so I throw away the "dead wood"; hints are sorted by
			// score (number of eliminations) DESCENDING, so we take the most
			// effective hint (not simplest, but all hints come from the same
			// Chainer, so they're in the same ball park atleast).
			while ( (cached=cache.pollFirst()) != null )
				// if either any elimination in the cached hint is still there
				if ( cached.reds != null ) {
					if ( cached.reds.anyCurrent()
				      // or cached hint sets a cell which has not yet been set
					  || cached.cell!=null && cached.cell.value==0
					) {
						// -> then return the cached hint so that it is applied,
						accu.add(cached);
						return true; // always, regardless of accu.add's retVal
					}
				} else { // Nothing to see here
					accu.add(cached);
				}
		}

		// remember these for next time
		cacheSource = grid.source;
		cacheNumSet = grid.numSet;

		cache.clear();

		// implemented by my subtypes to find the bloody hints.
		findChainHints(grid, cache);

		if ( VALIDATE_CHAINER ) {
			// valid nested hints, they've got problems!
			if ( tech.isNested  )
				validateTheHints(grid, cache);
		}

		processFunkyAssSizes();
//		if ( IS_NOISY )
//			noisef("\t\t\t%s %d hints%s", toString(), hints.size(), NL);
		final int n = cache.size();
		// no hints
		if ( n == 0 )
			return false;
		// flick-pass a single hint as itself
		if ( n == 1 ) {
			accu.add(cache.pollFirst());
			return true; // unusually, I ignore accu.add's return value
		}
		// caller wants one hint so find the most effective hint to apply.
		if ( accu.isSingle() ) {
			accu.add(cache.pollFirst());
			// nb: ignore accu.add's retval coz we already stopped searching.
			return true; // coz we know n != 0
		}
		// pass through multiple hints
		accu.addAll(cache);
		// Do NOT cache hints if I am an imbedded (nested) Chainer.
		if ( noCache )
			cache.clear();
		// nb: ignore accu.add's return value coz we already stopped searching.
		return true; // coz we know n != 0
	}

	/**
	 * Calculates the "off" effects of "anOn".
	 * <p>
	 * Note that there is no isXChain parameter because it's always true;
	 * ie other possible positions for this value in its regions always get
	 * "off"ed; because that's the fundamental rule of Sudoku.
	 * <p>
	 * Called 9,927,934 times in top1465.
	 *
	 * @param anOn an "on" assumption to find the "off" effects of
	 * @param isYChain true means that all other potential values of this cell
	 * get "off"ed
	 */
	protected void onToOffs(final Ass anOn) {
		assert anOn.isOn;
		ew = er = 0;
		final Cell cell = anOn.cell;
		final int value = anOn.value;
		final int sv = VSHFT[value]; // shiftedValue
		// Rule 1: other potential values of this cell get "off"ed.
		if ( isYChain ) { // Y-Chain: look for consequent naked singles
			final int ovb; // otherValuesBits
			// nb: "empty test" for dynamic mode where grid has naked singles
			if ( (ovb=cell.maybes & ~sv) != 0 )
				for ( int v : VALUESES[ovb] ) {
					effects[ew] = new Ass(cell, v, false, anOn
							, Cause.NakedSingle
							, "the cell can contain only one value");
					ew = (ew+1) % E_MASK;
					assert ew != er;
				}
		}
		// Rule 2: X-Chain: other possible positions for value get "off"ed.
		// NB: done this way because we need to know which region we're in;
		// and it is faster to "repeat" the "same" code three times. YMMV.
		for ( Cell sib : cell.box.cells )
			if ( (sib.maybes & sv)!=0 && sib!=cell ) {
				effects[ew] = new Ass(sib, value, false, anOn
					, Cause.CAUSE_FOR[Grid.BOX]
					, "the value can occur only once in the box");
				ew = (ew+1) % E_MASK;
				assert ew != er;
			}
		for ( Cell sib : cell.row.cells )
			if ( (sib.maybes & sv)!=0 && sib!=cell ) {
				effects[ew] = new Ass(sib, value, false, anOn
						, Cause.CAUSE_FOR[Grid.ROW]
						, "the value can occur only once in the row");
				ew = (ew+1) % E_MASK;
				assert ew != er;
			}
		for ( Cell sib : cell.col.cells )
			if ( (sib.maybes & sv)!=0 && sib!=cell ) {
				effects[ew] = new Ass(sib, value, false, anOn
						, Cause.CAUSE_FOR[Grid.COL]
						, "the value can occur only once in the col");
				ew = (ew+1) % E_MASK;
				assert ew != er;
			}
	}
	// circular-array replaces Queue, for speed.
	protected static final int E_SIZE = 32;
	protected static final int E_MASK = E_SIZE - 1;
	protected final Ass[] effects = new Ass[E_SIZE];
	protected int ew, er;

	/**
	 * MultipleChainer overrides this method; UnaryChainer just retains this
	 * no-op, but never calls it. This method is called by offToOns only in
	 * MultipleChainer when it isDynamic, to add any "hidden" causal parent
	 * assumptions to the effect Ass's parent-list.
	 * @param effect
	 * @param cell
	 * @param prntOffs
	 */
	protected void addHiddenParentsOfCell(Ass effect, Cell cell
		, IAssSet prntOffs) {
		// NB: this implementation never invoked. MultipleChainer overrides me.
	}

	/**
	 * MultipleChainer overrides this method; UnaryChainer just retains this
	 * no-op, but never calls it. This method is called by offToOns only in
	 * MultipleChainer when it isDynamic, to add any "hidden" causal parent
	 * assumptions to the effects parent-list. Apologies for parameters list
	 * length, it just kept growing.
	 *
	 * @param effect
	 * @param v value
	 * @param region
	 * @param currPlaces grid.region.ridx[value].bits
	 * @param oci otherCellIndice
	 * @param rti regionTypeIndex
	 * @param rents A set of complete parent offs, must be an IAssSet for that
	 *  weird getAss method, which I rely on, for speed, and comfort.
	 */
	protected void addHiddenParentsOfRegion(int oci, int rti, int v
			, int currPlaces, ARegion region, IAssSet rents, Ass effect) {
		// NB: this implementation never invoked. MultipleChainer overrides me.
	}

	/**
	 * Calculates the direct "on" effects of "anOff".
	 * Note that either isXChain or isYChain (or both) must be true.
	 * <p>USES: initGrid when isDynamic.
	 * <p>HAMMERED: called 31,758,114 times in top1465
	 *
	 * @param anOff the "off" Assumption to find the effects of.
	 * @param rents the Set of parent "off" Assumptions (each complete with
	 *  its parents), from which I fetch the parents of the Asses I create
	 * @param isXChain true means if there are only 2 possible positions for
	 *  anOff.value in the region, then the other position gets "on"ed?
	 * @param isYChain true means if there are only 2 potential values for
	 *  anOn.cell then the other value gets "on"ed?
	 * @return any found? (about a third of offs cause an on)
	 */
	protected boolean offToOns(final Ass anOff, final IAssSet rents) {
		assert !anOff.isOn;
		assert isXChain || isYChain;
		er = ew = 0;
		final Cell cell = anOff.cell;
		boolean result = false;

		// Rule 1: Y-Chain: if there are only two potential values for this
		// cell then the other value gets "on"ed.
		if ( isYChain && cell.size==2 ) {
			final Ass on = effects[ew] = new Ass(cell, anOff.otherValue(), true
					, anOff, Cause.NakedSingle, ONLYVALUE);
			ew = (ew+1) & E_MASK;
			assert ew != er;
			result = true;
			// add any erased parents of eOn to eOn (only in MultipleChainer)
			if ( isDynamic )
				// call-back MultipleChainer coz it has the current grid
				// and the initial grid, which I do not have access to.
				addHiddenParentsOfCell(on, cell, rents);
		}

		// Rule 2: X-Chain: if there are only two positions for this value in
		// the region then the other position gets "on"ed.
		if ( !isXChain )
			return result;
		final int v = anOff.value; // the value of the initial "off" Ass
		// foreach of the cells regions which has 2 positions for value
		for ( int rti=0; rti<3; ++rti ) { // regionTypeIndex: BOX, ROW, COL
			// skip unless there are 2 possible positions for value in region
			if ( cell.regions[rti].ridx[v].size == 2 ) {
				// Create the effectOn Ass and add to effects
				final ARegion r = cell.regions[rti]; // cellsRegion
				final int p = r.ridx[v].bits; // places
				final int i = cell.indexIn[rti]; // indexOfCellInRegion
				final Cell oc = r.cells[IFIRST[p & ~ISHFT[i]]]; // otherCell
				final Ass on = effects[ew] = new Ass(oc, v, true
						, anOff, CAUSE_FOR[rti], ONLYPOS[rti]);
				ew = (ew+1) & E_MASK;
				assert ew != er;
				result = true;
				// Dynamic: on.parents += Ass's erasing value in region.
				if ( isDynamic ) // ie I am a MultipleChainer in dynamic mode
					// call-back my MultipleChainer subclass because it has the
					// current and initial grids, to which I have no access.
					addHiddenParentsOfRegion(oc.i, rti, v, p, r, rents, on);
			}
		} // next regionTypeIndex
		return result;
	}
	// use only interned (static final) strings in assumptions, so we store
	// an address rather than a copy of the string in each assumption.
	private static final String ONLYVALUE =
		  "only remaining potential value in the cell";
	private static final String[] ONLYPOS = new String[] {
		  "only remaining position in the box"
		, "only remaining position in the row"
		, "only remaining position in the column"
	};

	/**
	 * returns target.parents.first.first...
	 * <p>This method is package visible because it's used by AChainingHint
	 * and MultipleChainer, instead of implementing it twice.
	 * @param target the Ass to find the root-cause of
	 * @return the root-cause Assumption which caused this Assumption
	 */
	static Ass getSource(Ass target) {
		Ass a = target;
//		while ( a.parents!=null && a.parents.size>0 )
		while ( a.parents!=null )
			a = a.parents.first.item;
		return a;
	}

	protected Pots createRedPots(Ass target) {
		final int bits;
		final int sv = VSHFT[target.value]; // shiftedValue
		if ( target.isOn ) { // target is an "On"
			// chaining a NakedSingle nullified my pinkBits! Hmmm.
			if ( target.cell.maybes == sv )
				return null; // I don't think this ever happens
			// NB: target is an "On" -> all other cells potential values
			bits = target.cell.maybes & sv;
		} else // target is an "Off" -> just the value
			bits = sv;
		assert bits != 0;
		return new Pots(target.cell, bits, false);
	}

	private void validateTheHints(Grid grid, HintCache hints) {
		final int[] solution = grid.getSolution();
		for ( Iterator<AHint> it = hints.iterator(); it.hasNext(); ) {
			AHint hint = it.next();
			if ( hint == null ) {
				// should never happen! Never say never.
				Log.println("ChainerBase.validateTheHints:");
				Log.println("IMPOSSIBLE: hint is null!");
				it.remove(); // skip it
			} else if ( hint.value!=0 && hint.cell!=null ) {
				int sv = solution[hint.cell.i];
				if ( hint.value != sv ) {
					String problem = "hint says "+hint.cell.id+PLUS+hint.value+" when solution value is "+hint.cell.id+PLUS+sv;
					Log.println("ChainerBase.validateTheHints:");
					Log.println("Invalid hint ("+problem+"): "+hint.toFullString());
					Log.println("Invalid grid:\n"+grid);
					it.remove(); // skip it
				}
			} else if ( hint.reds!=null && !hint.reds.isEmpty() ) {
				if ( !validOffs(grid, hint.reds) ) {
					String problem = "invalidity: "+invalidity;
					Log.println("ChainerBase.validateTheHints:");
					Log.println("Invalid hint ("+problem+"): "+hint.toFullString());
					Log.println("Invalid grid:\n"+grid);
					it.remove(); // skip it
				}
			} else {
				// This should never happen. Every hint eliminates a cell value
				// OR sets a cell value. Never say never.
				Log.println("ChainerBase.validateTheHints:");
				Log.println("Dodgy: MIA "+hint.cell+PLUS+hint.value+OR+hint.reds);
				Log.println("Dodgy hint: No elims in: "+hint.toFullString());
				Log.println("Dodgy grid:\n"+grid);
				it.remove(); // skip it
			}
		}
	}

	/**
	 * A FunkyAssSet2 is a {@link FunkyAssSet} which tracks it's
	 * own maxSize using FunkyAssSetSizes (below). To workout how big your
	 * FunkyAssSet's need to be (determinable only at runtime) search for the
	 * FunkyAssSet constructors and replace them with with FunkyAssSet2; then
	 * run the LogicalSolverTester which dump all FunkyAssSet2-sizes to stdout.
	 * NB: A good initialCapacity covers atleast 90% of cases (or 100% if that
	 * is not too big, or you are creating the set just ONCE). Repeated growth
	 * is my pet hate. I find it exponentially retarded/annoying! Hence I go
	 * to the effort of this reasonably-roundabout-way to bloody avoid it.
	 * <pre>
	 * Observed:
	 *	94	getUnaryChainsAndCycles(Chainer.java:327)
	 * 144	getUnaryChainsAndCycles(Chainer.java:328)
	 *	 2	doUnaryChainAndCycle(Chainer.java:404)
	 *	63	getMultipleOrDynamicChains(Chainer.java:530)
	 * 178	getMultipleOrDynamicChains(Chainer.java:531)
	 *	62	getMultipleOrDynamicChains(Chainer.java:535)
	 * 183	getMultipleOrDynamicChains(Chainer.java:536)
	 * </pre>
	 */
	protected static final class FunkyAssSet2 extends FunkyAssSet { // 1465 in 416 secs at 0.284
		public int maxSize = 0;
		public FunkyAssSet2(int initialCapacity, float loadFactor, boolean isOn) {
			super(initialCapacity, loadFactor, isOn);
			// get a representation of the line of code which created me.
			// Note that this relies on my caller actually creating me, not
			// delegating creation to a helper-method.
			String id = Thread.currentThread().getStackTrace()[2].toString();
			FunkyAssSizes.THE.put(id, this);
		}
		/** This copy-constructor clears the given {@code IMySet<Ass> c} if
		 * isClearing: it's more efficient to poll() each element out of c
		 * than it is to addAll and then clear c.
		 * @param initialCapacity
		 * @param loadFactor
		 * @param isOn
		 * @param c
		 * @param isClearing */
		public FunkyAssSet2(int initialCapacity, float loadFactor, boolean isOn
				, IMySet<Ass> c, boolean isClearing) { // can clear c
			super(Math.max(initialCapacity,c.size()), loadFactor, isOn);
			String id = Thread.currentThread().getStackTrace()[2].toString();
			FunkyAssSizes.THE.put(id, this);
			if ( isClearing ) // steal all from c
				for ( Ass a=c.poll(); a!=null; a=c.poll() ) // removes head
					add(a);
			else
				addAll(c);
		}
		@Override
		public boolean add(Ass o) {
			maxSize = Math.max(size()+1, maxSize);
			return super.add(o);
		}
	}

	protected static final class FunkyAssSizes
			extends java.util.LinkedHashMap<String, FunkyAssSet2> {
		private static final long serialVersionUID = 173460243L;
		public static final FunkyAssSizes THE = new FunkyAssSizes();
		private FunkyAssSizes() {} // can't hide a static inner class constructor
	}

	protected void processFunkyAssSizes() {
		final FunkyAssSizes sizes = FunkyAssSizes.THE; // a set of the sizes of sets.
		if ( sizes.size() > 0 ) {
			final OverallSizes overalls = OverallSizes.THE;
			for ( java.util.Map.Entry<String,FunkyAssSet2> e : sizes.entrySet() )
				overalls.max(e.getKey(), e.getValue().maxSize);
			sizes.clear();
		}
	}

}
