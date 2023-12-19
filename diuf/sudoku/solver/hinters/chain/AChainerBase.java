/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Config;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Run;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.IHinter.DUMMY;
import diuf.sudoku.solver.hinters.ICleanUp;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.solver.hinters.IHinter.T;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.MyArrays;
import diuf.sudoku.utils.MyCollections;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * AChainerBase is an abstract engine for searching a Grid for forcing chains.
 * Extended by ChainerUnary, ChainerMulti, ChainerNishio, and ChainerDynamic to
 * implement all Sudoku solving techniques that involve following a chain of
 * consequences that are caused by an initialAssumption that a cell is, or is
 * not, a value in the current grid. This includes all types (AFAIK) of Forcing
 * Chains and Bidirectional Cycles.
 * <p>
 * SE is now a bit faster than Juillerats original. On my ~2900 Mhz intel i7,
 * a slightly modified Juillerat took 21:54 for top1465: 1465 Sudoku puzzles,
 * about 900 of which are beyond humans. On 2023-08-08 (log below) SE solved
 * top1465 in 1:22. Butyekannoobeat Knuth at 992,370,784 nanoseconds!
 * <pre>
 * DiufSudoku 6.30.229 built 2023-08-08 11:35 ran 2023-08-08.14-04-29
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     13,383,800 108258       123  57357        233 NakedSingle
 *     26,170,100  50901       514  16427      1,593 HiddenSingle
 *     73,117,500  34474     2,120  24148      3,027 Locking
 *     69,815,400  23141     3,016   7795      8,956 NakedPair
 *     57,122,800  21102     2,706   8259      6,916 HiddenPair
 *    117,393,000  19016     6,173   1578     74,393 NakedTriple
 *    102,185,800  18621     5,487    985    103,741 HiddenTriple
 *     47,582,700  18425     2,582   1304     36,489 TwoStringKite
 *     40,406,000  17121     2,360    426     94,849 Swampfish
 *     52,047,900  16942     3,072    892     58,349 W_Wing
 *     73,388,100  16328     4,494    529    138,729 XY_Wing
 *     39,972,600  15974     2,502    327    122,240 Skyscraper
 *     95,494,600  15804     6,042    582    164,080 EmptyRectangle
 *     46,138,000  15222     3,031    263    175,429 XYZ_Wing
 *     53,708,100  14977     3,586    223    240,843 Swordfish
 *    163,608,700  14916    10,968     97  1,686,687 NakedQuad
 *     78,366,100  14896     5,260     16  4,897,881 Jellyfish
 *    125,816,400  14893     8,448      4 31,454,100 HiddenQuad
 *    284,213,300  14891    19,086    178  1,596,703 Coloring
 *    856,099,000  14769    57,965    797  1,074,151 XColoring
 *  1,913,749,600  14505   131,937 124593     15,360 GEM
 *  1,427,245,300  13156   108,486    884  1,614,530 URT
 *  3,219,615,500  12527   257,014   2113  1,523,717 BigWings
 *  4,526,801,100  11353   398,731   2538  1,783,609 DeathBlossom
 *  4,983,949,100   9213   540,969   2048  2,433,568 ALS_XZ
 *  4,361,945,800   7953   548,465   2605  1,674,451 ALS_Wing
 *  9,790,040,300   5887 1,662,993   2106  4,648,642 ALS_Chain
 *  1,453,754,800   4237   343,109     26 55,913,646 SueDeCoq
 * 13,228,782,700   4232 3,125,893    322 41,083,176 FrankenSwordfish
 *  6,118,139,300   3979 1,537,607    431 14,195,218 UnaryChain
 *  4,229,248,500   3615 1,169,916     97 43,600,500 NishioChain
 *  5,523,580,700   3518 1,570,091   5397  1,023,453 DynamicChain  AWESOME!!!
 *     76,265,200     11 6,933,200     11  6,933,200 DynamicPlus
 * 63,269,147,800
 * pzls      total (ns)	(mm:ss)   each (ns)
 * 1465  82,854,098,600	(01:22)  56,555,698
 * </pre>
 * <p>
 * Previously, no expense was spared in the pursuit of speed; even correctness
 * took a hit. I define correct as "Each hint is the simplest available". Now I
 * recognise that many "simpler" hinters are slower, and will always be thus,
 * so an over-speed approach means exaggerated puzzle difficulties. If you can
 * do it faster then go ahead, just share your code, please. No secrets!
 * <p>
 * KRC AUG 2019 The Chainers used to apply the first found hint, it now always
 * finds all hints, and (when one hint is wanted) returns the most effective
 * (highest score) hint. This is slower but it allows us to step through a
 * solution in the GUI following the same path (hints) that LogicalSolverTester
 * used to solve the puzzle, which is exactly what I want. This change improved
 * my development life, and I find that my perception of the importance of
 * speed over accuracy continues to abate over time.
 * <p>
 * KRC 2019 OCT I am proud of working-out that the fastest way to figure-out if
 * we already know the conjugate is by calculating the conjugates hashCode then
 * just comparing each calculated Ass.hashCode with it, instead of the old way
 * conjugating each calculated Ass, which double-hammers the Ass constructor to
 * create ship-loads of barely-used garbage Asses, which end-up bogging the GC.
 * <p>
 * KRC 2011-11-01 I have split the existing Chainer class into ChainerUnary and
 * ChainerMulti, leaving commonality in AChainerBase (this abstract class). I
 * am doing this because the methods and even fields of Unary, Multiple, and
 * Dynamic chaining are disjunct, especially at the upper levels; they really
 * share only the onToOff and OffToOn methods, and the few differences at the
 * bottom levels are handled with call-backs to the subtypes.
 * This experiment worked first time (FFMS!), and it makes the code dryer, and
 * therefore (I hope) a bit easier to follow. A class that is 60% unused in
 * 75% of use-cases is s__t. A class should be focused on solving a specific
 * problem. I just narrowed-down the problem, and simplicity has (I hope, I am
 * too deep into this s__t to judge) followed. Mind you, using inheritance to
 * make things simpler sounds oxymoronic.
 * KRC 2023 August split ChainerMulti into ChainerMulti and ChainerDynamic to
 * eliminate repeated path selection. Its a bit faster. Also imbedded chainers
 * are created then never invoked. NestedUnary is last hinter used in anger; so
 * chainers imbedded in NestedMulti, NestedDynamic, and NestedPlus unexecuted;
 * so we construct LinkedMatrixAssSets (large) in first call to findChains.
 * Also I removed IAfter. Just use ICleanUp. Doesnt matter if it runs twice.
 */
public abstract class AChainerBase extends AHinter implements ICleanUp {

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

	// self-documenting FunkyAssSet constructor param
	protected static final boolean CLEAR = true;

	// self-documenting new Ass isOn param
	protected static final boolean ON = Ass.ON;
	protected static final boolean OFF = Ass.OFF;

	// These are for offToOns
	// NOTE WELL: For speed, we use only interned (static final) strings in
	// assumptions, so that we only store the address of an existing String,
	// rather than construct a new string in every assumption (of which there
	// are billions); which would be both expensive and RAM hungry. This is one
	// of the reasons SE is now MUCH faster than Juilerat: No time wasters!
	// The stack is faster than the heap, hence the Java architects are nuts.
	protected static final String ONLYVALUE =
		  "only remaining potential value in the cell";
	protected static final String[] ONLYPOS = new String[] {
		  "only remaining position in the box"
		, "only remaining position in the row"
		, "only remaining position in the column"
	};

	// see above NOTE WELL. These are for onToOffs
	protected static final String[] ONETIME = {
			  "the value can occur only once in the box"
			, "the value can occur only once in the row"
			, "the value can occur only once in the col"
	};
	protected static final String ONEVALUE
			= "the cell can contain only one value";

	/** circular-array replaces Queue, for speed. */
	protected static final int E_SIZE = 32;
	/** circular-array replaces Queue, for speed. */
	protected static final int E_MASK = E_SIZE - 1;

	/** MUTABLE Does this Chainer use its internal cache? */
	protected boolean useCache;
	// the grid.puzzleId of hintsCache
	private long puzzleId;
	// the grid.numSet of hintsCache
	private long numSet;

	/** Are multiple cell and region chains sought. Multiple means more than 2
	 * potential values, or more than 2 possible positions. */
	protected final boolean isMultiple;
	/** Is this Chainer removing maybes as it goes? */
	protected final boolean isDynamic;
	/** Is this Chainer looking for Dynamic Contradictions. */
	protected final boolean isNishio;

	/** Is this Chainer embedded in another chainer. */
	protected final boolean isEmbedded;

	/** the hints */
	protected final HintsQueue hints = new HintsQueue();

	/**
	 * Constructs an abstract Chainer: an engine for searching a Sudoku Grid
	 * for forcing chains.
	 * <p>
	 * My cache (an implementation of HintCache) is constructed when the hinter
	 * is created (ie when the LogicalSolver is constructed) based on the
	 * <b>CURRENT</b> isFilteringHints setting, unlike other isFilteringHints
	 * features the caching-implementation used does not change when you switch
	 * on/off isFilteringHints, it is set for the life of the LogicalSolver, so
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
	 * @param useCache normally true, false when imbedded and in test-cases
	 */
	protected AChainerBase(final Tech tech, final boolean useCache) {
		super(tech);
		assert tech.isChainer;
		assert degree>=0 && degree<=5;
		this.isMultiple	 = tech.isMultiple;
		this.isDynamic	 = tech.isDynamic;
		this.isNishio	 = tech.isNishio;
		this.isEmbedded = !useCache;
		this.useCache = useCache && !Run.isTestCase()
					 && CFG.getBoolean(Config.isCachingHints, T);
	}

	public HintsQueue getHints() {
		return hints;
	}

	/**
	 * Find any chaining hints in the given grid and add them to hints list.
	 * This method is implemented by my subtypes: ChainerUnary, ChainerStatic
	 * ChainerNishio, and ChainerDynamic to find the chains. It is done this
	 * way so that this.findHints just does the cache, and I just find chains.
	 * Separation of concerns.
	 * <p>
	 * NOTE: findChains (unlike most) does NOT return success!
	 * Instead success is determined by "hints.size() > 0".
	 */
	protected abstract void findChainHints();

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation finds XY Forcing Chains hints in the grid.
	 * <p>
	 * Actually, this method now just caches. The former findHints is called
	 * findChains. I do the cache. He finds chains.
	 * <p>
	 * Note hints are cached locally. I dabbled with a HintsCache interface
	 * with two implementations and then abandoned it because it was getting
	 * too hard. Its better to roll your own.
	 */
	@Override
	public final boolean findHints() {
		// useCache: caching breaks imbedded chainers, and test-cases.
		if ( useCache
		  && grid.numSet == numSet
		  && grid.puzzleId == puzzleId
		  && (!Run.isGui() || CFG.isFilteringHints())
		  && !hints.isEmpty()
		) {
			// return the FIRST fresh hint, eating stale ones.
			AChainingHint hint;
			while ( (hint=hints.poll()) != null )
				if ( hint.anyCurrent(grid) ) {
					accu.add(hint);
					return true;
				}
		}
		// cache miss (or caching is disabled)
		this.hints.clear();
		this.numSet = grid.numSet;
		this.puzzleId = grid.puzzleId;
		// populate hints using above fields
		findChainHints();
		// populate accu from hints
		final int n = hints.size();
		if ( n == 0 )
			return false;
		if ( useCache
		  && ( n == 1
			|| accu.isSingle()
			|| (Run.isGui() && CFG.isFilteringHints()) ) )
			accu.add(hints.poll());
		else {
			accu.addAll(hints);
			hints.clear();
		}
		return true;
	}

	/**
	 * Switch internal hint caching on/off.
	 * The cache is cleared when you switch off.
	 *
	 * @param useCache true is on, false is off
	 * @return the previous setting
	 */
	@Override
	public final boolean setCaching(final boolean useCache) {
		boolean was = this.useCache;
		this.useCache = useCache;
		// clear cache when switching-off
		if ( was && !useCache ) {
			hints.clear();
		}
		return was;
	}

	/**
	 * Returns the initial assumption that caused $target.
	 * <p>
	 * This method is package visible for use in AChainingHint.
	 *
	 * @param target the Ass to find the initialAssumption of
	 * @return target or the first parent of target with a null parent;
	 *  else null meaning that target is null
	 */
	static final Ass getInitialAss(final Ass target) {
		if ( target == null ) {
			return target;
		}
		Ass a = target;
		while ( a.firstParent != null ) {
			a = a.firstParent;
		}
		return a;
	}

	/**
	 * Create the removable (red) Pots for the given $target Ass.
	 *
	 * @param target your Ass
	 * @return a new Pots
	 */
	protected final Pots createRedPots(final Ass target) {
		final int cands;
		if ( target.isOn ) {
			final int sv = VSHFT[target.value];
			// NakedSingles ____ chainers! Hmmm. ~Yoda
			if ( maybes[target.indice] == sv )
				return null;
			// only the present ones!
			cands = maybes[target.indice] & sv;
		} else
			cands = VSHFT[target.value];
		assert cands > 0; // NEVER negative, and not 0 either
		return new Pots(target.indice, cands, DUMMY);
	}

	/**
	 * Construct a new BinaryChainHint.
	 *
	 * @param dstOn destinationOn in a contradiction this is the On
	 * @param dstOff destinationOff in a contradiction this is the Off,
	 *  that contradicts dstOn, hence the target Ass is proved false
	 * @param initialAss the initialOn/Off
	 * @param target is the Ass at the consequence-end of the chain;
	 *  the down-stream end; the opposite of the initial assumption.
	 *  Target is back-linked all the way to the initial assumption.
	 *  Target is the result, so "which one" is often arbitrary; any one that
	 *  displays the correct larger colored maybe in the grid will do
	 * @param isAnOn true if this hint sets a cell value;
	 *  false for a workaday eliminations hint
	 * @param isContradiction BinaryChainHint presents three hint types: <br>
	 *  if isNishio "Binary Nishio Chain" BinaryChainHintNishio.html; else <br>
	 *  true "Binary Contradiction Chain" BinaryChainHintContradiction.html; <br>
	 *  false "Binary Reduction Chain" BinaryChainHintReduction.html
	 * @param typeId for hackers to uniquely identify this hints code-path,
	 *  which is the only way, AFAIK, to assure that all paths are working.
	 *  Too check switch-on WANT_TYPE_ID, batch, tools.DistinctHintTypes.
	 * @return new BinaryChainHint
	 */
	protected final BinaryChainHint binaryChain(final Ass dstOn, final Ass dstOff
		, final Ass initialAss, final Ass target, final boolean isAnOn
		, final boolean isContradiction, final int typeId) {
		final Pots reds;
		if ( isAnOn ) {
			if ( (reds=createRedPots(target)) == null )
				return null;
		} else
			reds = new Pots(target.indice, target.value);
		final Ass result = isContradiction||isNishio ? initialAss.flip() : dstOn;
		return new BinaryChainHint(grid, this, reds, initialAss, dstOn, dstOff
				, isNishio, isContradiction, typeId, result);
	}

	/**
	 * Construct a new CellReductionHint.
	 *
	 * @param indice of cell whose every maybe produces the same consequence
	 * @param target the common effect of every chain from every maybe
	 * @param valuesEffects effects of assuming each $cell+$maybe, by maybe
	 * @param typeId for hackers to uniquely identify this hints code-path,
	 *  which is the only way, AFAIK, to assure that all paths are working.
	 *  nb: formerly there where test-cases to check this. I dropped them.
	 *  Too check just turn on WANT_TYPE_ID and grep the batch log.
	 * @return a new CellReductionHint; else null meaning theres no hint here.
	 *  IAccumulator.add ignores null.
	 */
	protected final CellReductionHint cellReduction(final int indice, final Ass target
			, final LinkedMatrixAssSet[] valuesEffects, final int typeId) {
		// Shft-F5: Nested* check source.cell is this cell to avert invalid
		// hints when the embedded chainer finds a RAW grid hint.
		if ( tech.isNesting && getInitialAss(target) == null )
			return null;  // this cannot be a CellReductionHint
		// Build removable (reds) potentials
		final Pots reds;
		if ( (reds=createRedPots(target)) == null )
			return null;
		// Build chains
		assert sizes[indice] > 0;
		LinkedMatrixAssSet effects;
		Ass targetWP // targetWithParents
		  , result = null; // result is arbitrarily the first targetWP
		final ArrayList<Ass> chains = new ArrayList<>(sizes[indice]);
		for ( int value : VALUESES[maybes[indice]] ) {
			if ( (effects=valuesEffects[value]) == null
			  || (targetWP=effects.getAss(target.indice, target.value)) == null )
				return null; // fubared
			// result is arbitrarily the first target (they are all the same)
			if ( result == null )
				result = targetWP;
			chains.add(targetWP);
		}
		if ( chains.isEmpty() )
			return null; // never (I think)
		return new CellReductionHint(grid, this, reds, indice, chains, typeId, result);
	}

	/**
	 * Construct a new RegionReductionHint, to keep the sheep looking nervous,
	 * despite being too far West. That one was low! Even for me.
	 *
	 * @param region the search region
	 * @param value the value we seek
	 * @param t for target is just a dolly Ass thats used to look-up the actual
	 *  targetWithParents in pozsEffects
	 * @param pozsEffects position effects is an array of IAssSet indexed by
	 *  region.places[value]. Asses herein have parents. Note first position is
	 *  a LinkedMatrixAssSet, and subsequent ones are all FunkyAssSets; but
	 *  both classes implement IAssSet, so no worries
	 * @return a new RegionReductionHint; else null meaning sheep happened
	 */
	protected final RegionReductionHint regionReduction(final ARegion region
		, final int value, final Ass t, final IMySet<Ass>[] pozsEffects) {
		// I have had a gutful of dodgy params from him upstairs. Sigh.
		assert region != null; // never none
		assert value>0 && value<Grid.VALUE_CEILING;
		assert t != null; // never none
		assert region.places[value] > 0; // NEVER negative. never none.
		// pozsEffects has a Set for every region.places[value]
		// nb: those sets still may not contain target. Sigh.
		assert MyArrays.notNull(pozsEffects, INDEXES[region.places[value]]);
		IMySet<Ass> pozEffects; // the effects of region.cells[i] being value
		Ass targetWP; // targetWithParents (t for target is just a dolly)
		Ass result = null; // this Ass (the first) has parents
		// build reds (removable potentials)
		final Pots reds;
		final int typeId;
		if ( t.isOn ) {
			if ( maybes[t.indice] == VSHFT[t.value] )
				return null; // shft-f5: NakedSingle is not my problem
			reds = new Pots(t.indice, maybes[t.indice] & ~VSHFT[t.value], DUMMY);
			typeId = 0;
		} else {
			reds = new Pots(t.indice, t.value);
			typeId = 1;
		}
		// chains: targets of the forcing chains from each region.places[value]
		// by target I mean the consequence end of the chain. Every target in a
		// region forcing chain is the same (only ancestors differ), and thats
		// how we got here: Every place for value in region produces the same
		// consequence.
		final ArrayList<Ass> chains = new ArrayList<>(VSIZE[region.places[value]]);
		for ( int i : INDEXES[region.places[value]] ) {
			// add targetWithParents (ergo a complete ancestoral chain)
			if ( (pozEffects=pozsEffects[i]) == null
			  || (targetWP=pozEffects.get(t)) == null ) {
				// if any place misses the target theres no hint here.
				return null; // should never happen. never say never.
			}
			if ( result == null ) {
				result = targetWP;
			}
			chains.add(targetWP);
		}
		assert chains.size() == VSIZE[region.places[value]];
		assert MyCollections.notNull(chains);
		return new RegionReductionHint(grid, this, reds, region, value, chains, typeId, result);
	}

	/**
	 * Clear the hints cache, and call my subclasses cleanUpImpl.
	 */
	@Override
	public final void cleanUp() {
		hints.clear();
		puzzleId = -0L;
		numSet = -81;
		cleanUpImpl();
	}

	/**
	 * Each subclass implements its own ICleanUp here, which I call in cleanUp,
	 * pre-Grid.load, post-solve; or whenever we are done with this puzzle.
	 */
	protected abstract void cleanUpImpl();

	/** I retain a queue of hints. My add ignores null hints. */
	protected static final class HintsQueue extends LinkedList<AChainingHint> {
		private static final long serialVersionUID = 345644845031073L;
		@Override
		public boolean add(final AChainingHint e) {
			return e!=null && super.add(e);
		}
	};

}
