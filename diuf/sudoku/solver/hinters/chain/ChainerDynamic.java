/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Ass.Cause.CAUSE;
import static diuf.sudoku.Ass.Cause.NakedSingle;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.ICleanUp;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.fish.Fisherman;
import diuf.sudoku.solver.hinters.hidden.HiddenPair;
import diuf.sudoku.solver.hinters.naked.NakedPair;
import diuf.sudoku.solver.hinters.lock.Locking;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Indexes.ILAST;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.BITS9; // VALL is 9bits
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VLAST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.IFoxyHint;
import diuf.sudoku.solver.hinters.IFoxyHinter;
import diuf.sudoku.utils.IAssSet;
import java.util.LinkedList;
import java.util.Map;
import static java.util.Arrays.fill;
import java.util.Collection;
import java.util.Deque;

/**
 * ChainerDynamic implements all of the following Sudoku solving techniques:
 * {@link Tech#DynamicChain},
 * {@link Tech#DynamicPlus},
 * {@link Tech#NestedUnary},
 * {@link Tech#NestedStatic},
 * {@link Tech#NestedDynamic},
 * {@link Tech#NestedPlus}.
 * <p>
 * I'm a hexosexual! With a nesting fetish.
 * <p>
 * ChainerDynamic seeks multiple dynamic XY forcing chains.
 * <pre>
 * * Multiple means chains start from cells with two-or-more maybes.
 * * Dynamic means that each Off is erased from the grid, to reveal any new
 *   bivalued cells and biplaced regions; hence condequences of multiple
 *   subchains are conjoined to find more consequences, hence more hints.
 * * XY forcing chains have two types of links, Y, and X:
 * * I put Y first because its implemented first, for efficiency.
 * * Y-links are based on cell maybes.
 *   * An On has every other cell.maybe Offed.
 *   * An Off causes an On only if this cell has two maybes:
 *     the other maybe is On.
 * * X-links are based on region places.
 *   * An On has every other place for value in each cell.regions Offed.
 *   * An Off causes an On only if there are two places for value in any
 *     cell.region: the other place is On.
 * * A Forcing Chain follows all the consequences of an initial-assumption.
 *   Chaining finds hints in those consequences using:
 *   * Contradiction: anAss causes a cell-value to be both an On and Off,
 *     hence anAss is proven false.
 *   * Cell Reduction: all maybes have the same consequence,
 *     hence that consequence is proven true.
 *   * Region Reduction: all places have the same consequence,
 *     hence that consequence is proven true.
 * * DynamicPlus's Plus are the FourQuickFoxes: Locking, HiddenPair, NakedPair,
 *   and Swampfish. FQFs are just fast standard hinters, with 1 addition: each
 *   hint they produce implements the {@link IFoxyHint} interface to provide da
 *   {@link ChainerHintsAccumulator} with the getParents method. The separation
 *   of concerns in CHA is pure evil genius. I love it. Kudos the Juillerat.
 * * Nested* add various chainers to the foxes.
 * @see #ChainerDynamic(diuf.sudoku.Tech, java.util.Map, boolean) for details.
 *
 * <b>SLOW HINTERS</b>:
 *
 * DynamicChain (DC) finds all Unary/Static/Nishio hints. When all implemented
 * efficiently, DC is my definition of "slow". Unary, Static, and Nishio chains
 * are all slow, by comparison, as is the bottom half of SE's hinters list.
 * There "but I am simpler" arguement lacks emperical support. Everything from
 * Jellyfish down is slower per elim. Get on with it!
 *
 * Rule of thumb: if ns/elim > DC then I dont want it. Sink or swim. So the
 * challenge is to make every hinter thats slower DC atleast as fast as DC,
 * which will be no mean feat, I can assure you. Every simpler hinter I dont
 * use (too slow) is a failure: keep trying. Failure is not failure. Failure is
 * not having succeeded, yet. Failure is giving up. I must admit defeat. Being
 * a complete idiot, its the best I can hope for. Sigh.
 *
 * KRC 2019-11-01 I have split the existing Chainer class into ChainerMulti
 * and UnaryChainer. See UnaryChainer comments for more details.
 *
 * KRC 2023 August split ChainerMulti into ChainerStatic and ChainerDynamic.
 * This eliminates type-switching code, making both a bit faster. They share
 * hint-creation methods in AChainerBase. They inline onToOff and offToOn.
 * </pre>
 */
public final class ChainerDynamic extends AChainerBase implements ICleanUp, IFoxyHinter
//		, diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[8];

//	private static final boolean DEBUG = false;
//	private static void DEBUG(String msg) {
//		System.out.println(msg);
//	}
//	private static void DEBUG(String frmt, Object... args) {
//		System.out.format(frmt, args);
//	}

	private static final int ON_SIZE = 64; // a power of 2
	private static final int ON_MASK = ON_SIZE - 1;
	private static final Ass[] ONQ = new Ass[ON_SIZE];

	private static final int OFF_SIZE = 256; // a power of 2
	private static final int OFF_MASK = OFF_SIZE - 1;
	private static final Ass[] OFFQ = new Ass[OFF_SIZE];

	/**
	 * An array of the Four Quick Foxes (and imbedded chainers),
	 * for execution in the {@link #fourQuickFoxes} method.
	 * <p>
	 * The array is populated in my
	 * {@link #ChainerDynamic(diuf.sudoku.Tech, java.util.Map, boolean) constructor}
	 * as determined by the given {@link Tech#degree}.
	 * <p>
	 * This is pure evil genius. Weez likes it!
	 */
	private final IFoxyHinter[] foxes;
	private final int numFoxes;

	/**
	 * Use foxes? ie degree > 0. false in DynamicChain; true in DynamicPlus,
	 * NestedUnary, NestedMultiple, NestedDynamic, and NestedPlus.
	 */
	private final boolean isFoxy;

	/**
	 * The initial (unmodified) grid used to find parent (causal) Assumptions.
	 * We search for cell-vs that have been removed from the current grid that
	 * are still in this unmodified initGrid.
	 */
	private Grid initGrid;
	private ARegion[] initRegions; // initGrid.regions
	private int[] initMaybes; // initGrid.maybes

	/**
	 * Has this hinter been initialised, ie have sets been constructed.
	 * Note that when isFoxy, All imbedded chainers initialise and deinitialise
	 * when the master chainer does, NOT every time they are called, for speed.
	 */
	private boolean isInitialised;

	/**
	 * Do you want the solution when doChains solves the Sudoku?
	 * A "hidden" feature, coz it takes all the fun out of it.
	 */
	private boolean turbo;

	// all On/Off consequences of anOn // doBinaryChains, doRegionChains
	private LinkedMatrixAssSet onToOns, onToOffs;
	// On/Off consequences of anOff // doBinaryChains (contradictions)
	private LinkedMatrixAssSet offToOns, offToOffs;
	// On/Off consequences of $sibling+$value // doRegionsChains
	private LinkedMatrixAssSet sibOns, sibOffs;
	// On/Off consequences common to every place in region // doRegionsChains
	private LinkedMatrixAssSet rgnOns, rgnOffs;
	// On/Off consequences common to every maybe of cell // findChains
	private LinkedMatrixAssSet cellOns, cellOffs;
	// On/Off consequences of each place in region // doRegionsChains
	private LinkedMatrixAssSet[] posOns, posOffs;
	// On/Off consequences of each cell.maybe // doRegionsChains
	private LinkedMatrixAssSet[] valOns, valOffs;

	/**
	 * Constructor. See the class comments for overview. This is detail.
	 * <pre>
	 * ChainerDynamic instances, by Tech:
	 * DynamicChain   = 0 = Dynamic chains only, no foxes
	 * DynamicPlus    = 1 = Dynamic + FourQuickFoxes (FQF)
	 * NestedUnary    = 2 = Dynamic + FQF + Unary
	 * NestedMultiple = 3 = Dynamic + FQF + Unary + Multiple
	 * NestedDynamic  = 4 = Dynamic + FQF + Dynamic
	 * NestedPlus     = 5 = Dynamic + FQF + Dynamic + DynamicPlus
	 * </pre>
	 *
	 * @param tech a Tech with isChainer==true.
	 * @param basics from which, if not null and this Tech requires them, I get
	 *  the Four Quick Foxes: Locking, HiddenPair, NakedPair, and Swampfish.
	 *  If basics is null and tech requires them, then I construct my own.
	 * @param useCache normally true; false when imbedded and in test-cases
	 */
	@SuppressWarnings("fallthrough")
	public ChainerDynamic(final Tech tech, final Map<Tech,IHinter> basics
			, final boolean useCache) {
		super(tech, useCache);
		assert tech != Tech.UnaryChain; // has its own class
		assert tech != Tech.StaticChain; // has its own class
		assert tech != Tech.NishioChain; // has its own class
		assert isDynamic; // all my techs are dynamic!
		assert degree>=0 && degree<=5;
		// Gopher Cleetus?
		turbo = CFG.getBoolean("turbo", false);
		if ( degree < 1 ) {
			this.foxes = null;
			this.numFoxes = -1;
			this.isFoxy = false;
			return;
		}
		// populate foxes in DynamicPlus, NestedUnary, NestedMultiple,
		//                   NestedDynamic, or NestedPlus.
		this.isFoxy = true;
		if ( degree == 1 )
			numFoxes = 4; // DynamicPlus
		else if ( degree%2 == 0 )
			numFoxes = 5; // NestedUnary, NestedDynamic
		else
			numFoxes = 6; // NestedMultiple, NestedPlus
		this.foxes = new IFoxyHinter[numFoxes];
		if ( basics != null ) {
			// the Four Quick Foxes (existing)
			foxes[0] = (IFoxyHinter)basics.get(Tech.Locking);
			foxes[1] = (IFoxyHinter)basics.get(Tech.HiddenPair);
			foxes[2] = (IFoxyHinter)basics.get(Tech.NakedPair);
			foxes[3] = (IFoxyHinter)basics.get(Tech.Swampfish);
		} else {
			// the Four Quick Foxes (new)
			foxes[0] = new Locking();
			foxes[1] = new HiddenPair();
			foxes[2] = new NakedPair();
			foxes[3] = new Fisherman(Tech.Swampfish);
		}
		// create the nested (imbedded) chainers, by Tech (not degree, sigh)
		// retaining this chart mapped by degree (sigh).
		// 0 = NishioChain, MultipleChain, DynamicChain
		// 1 = DynamicPlus    = Dynamic + Four Quick Foxes (FQF)
		// 2 = NestedUnary    = Dynamic + FQF + Unary
		// 3 = NestedMultiple = Dynamic + FQF + Unary + Multiple
		// 4 = NestedDynamic  = Dynamic + FQF + Dynamic
		// 5 = NestedPlus     = Dynamic + FQF + Dynamic + DynamicPlus
		switch (tech) {
		case DynamicPlus:	 // Dynamic + Four Quick Foxes (FQF)
			break; // the Frog Quinc ____ers are already in the foxes array
		case NestedStatic: // Dynamic + FQF + UnaryChain + MultipleChain
			foxes[5] = new ChainerStatic(false);
 			//fallthrough
		case NestedUnary:	 // Dynamic + FQF + UnaryChain
			foxes[4] = new ChainerUnary(false);
			break;
		case NestedPlus:		 // Dynamic + FQF + DynamicChain + DynamicPlus
			foxes[5] = new ChainerDynamic(Tech.DynamicPlus, basics, false);
			//fallthrough
		case NestedDynamic:	 // Dynamic + FQF + DynamicChain
			foxes[4] = new ChainerDynamic(Tech.DynamicChain, basics, false);
			break;
		}
	}

	// create all my sets
	@Override
	public void initialise() {
		if ( isInitialised )
			return;
		int i;
		onToOns  = new LinkedMatrixAssSet();
		onToOffs = new LinkedMatrixAssSet();
		offToOns  = new LinkedMatrixAssSet();
		offToOffs = new LinkedMatrixAssSet();
		sibOns  = new LinkedMatrixAssSet();
		sibOffs = new LinkedMatrixAssSet();
		rgnOns  = new LinkedMatrixAssSet();
		rgnOffs = new LinkedMatrixAssSet();
		cellOns  = new LinkedMatrixAssSet();
		cellOffs = new LinkedMatrixAssSet();
		posOns  = new LinkedMatrixAssSet[REGION_SIZE];
		i=0; do posOns[i] = new LinkedMatrixAssSet(); while (++i<REGION_SIZE);
		posOffs = new LinkedMatrixAssSet[REGION_SIZE];
		i=0; do posOffs[i] = new LinkedMatrixAssSet(); while (++i<REGION_SIZE);
		valOns  = new LinkedMatrixAssSet[VALUE_CEILING];
		i=1; do valOns[i] = new LinkedMatrixAssSet(); while (++i<VALUE_CEILING);
		valOffs = new LinkedMatrixAssSet[VALUE_CEILING];
		i=1; do valOffs[i] = new LinkedMatrixAssSet(); while (++i<VALUE_CEILING);
		// WARN: treat these as if they where final! (as above)
		initGrid = new Grid();
		initRegions = initGrid.regions;
		initMaybes = initGrid.maybes;
		if ( isFoxy )
			for ( IFoxyHinter fox : foxes )
				fox.initialise();
		isInitialised = true;
	}

	// nullify all my sets
	@Override
	public void deinitialise() {
		if ( !isInitialised )
			return;
		int i;
		initMaybes = null;
		initRegions = null;
		initGrid = null;
		i=0; do posOns[i] = posOffs[i] = null; while(++i<REGION_SIZE);
		i=1; do valOns[i] = valOffs[i] = null; while(++i<VALUE_CEILING);
		posOns = posOffs = valOns = valOffs = null;
		onToOns = onToOffs = offToOns = offToOffs = sibOns = sibOffs = rgnOns
				= rgnOffs = cellOns = cellOffs = null;
		if ( isFoxy )
			for ( IFoxyHinter fox : foxes )
				fox.deinitialise();
		isInitialised = false;
	}

	/**
	 * ChainerDynamic implements {@link AChainerBase#findChainHints() } to find all
	 * Dynamic Multiple forcing chain hints in the $grid, adding them to $hints
	 * (the hints-cache). See the class comments for discussion.
	 */
	@Override
	protected void findChainHints() {
		try {
			Ass effect; // effect
			int values[], vn, vi, v; // array, size, index, value
			boolean reduce; // cell.size > 2
			boolean anyOns, anyOffs; // are there any common Ons/Offs
			if ( !isInitialised )
				initialise();
			// look at cells/regions size > 1, combining consequences.
			// foreach cell in the grid
			int indice = 0;
			do
				// if cell has two-or-more maybes
				if ( sizes[indice] > 1 )
					try {
						// do reduction if cell has three-or-more maybes
						reduce = sizes[indice] > 2;
						// foreach INITIAL maybe of this cell (there are 2+).
						// NOTE: VALUESES[cell.maybes] set BEFORE we iterate,
						// so doChains "erase" does NOT effect the iteration,
						// hence we iterate each INITIAL maybe of this cell,
						// as required for algorithmic correctness!
						values = VALUESES[maybes[indice]];
						vn = values.length;
						v = values[0];
						// binary chaining: contradictions and reductions
						// * contradition: anAss causes both On and Off,
						//   which is absurd, hence anAss is false
						// * reduction: same consequence from On and Off
						// nb: doRegionsChains reuses onToOns/Offs.
						doBinaryChains(indice, v, reduce);
						// region chaining: every place for v in region
						// has the same effect (reuses onToOns/Offs).
						doRegionsChains(indice, v);
						// collect sets for cell reduction
						anyOns = cellOns.addAll(onToOns);
						anyOffs = cellOffs.addAll(onToOffs);
						valOns[v].pollAll(onToOns); // clears OnToOns
						valOffs[v].pollAll(onToOffs); // clears OnToOffs
						vi = 1;
						do {
							v = values[vi];
							// binary chaining: contradictions and reductions
							// * contradition: anAss causes both On and Off,
							//   which is absurd, hence anAss is false
							// * reduction: same consequence from On and Off
							// nb: doRegionsChains reuses onToOns/Offs.
							doBinaryChains(indice, v, reduce);
							// region chaining: every place for v in region
							// has the same effect (reuses onToOns/Offs).
							doRegionsChains(indice, v);
							// collect sets for cell reduction
							if ( anyOns )
								anyOns &= cellOns.retainAll(onToOns.nodes);
							anyOffs &= cellOffs.retainAll(onToOffs.nodes);
							valOns[v].pollAll(onToOns); // clears OnToOns
							valOffs[v].pollAll(onToOffs); // clears OnToOffs
						} while ( ++vi < vn );
						// cell reduction: consequences common to every maybe
						if ( anyOns )
							while ( (effect=cellOns.poll()) != null )
								hints.add(cellReduction(indice, effect, valOns, 0));
						if ( anyOffs )
							while ( (effect=cellOffs.poll()) != null )
								hints.add(cellReduction(indice, effect, valOffs, 1));
						vi = 0;
						do {
							v = values[vi];
							valOns[v].clear();
							valOffs[v].clear();
						} while ( ++vi < vn );
						onToOns.clear();
						onToOffs.clear();
						offToOns.clear();
						offToOffs.clear();
						hinterrupt();
					} catch ( UnsolvableException ex ) { // from Ass.erase()
						// Do nothing; reduction is pretty risky.
					}
			while ( ++indice < GRID_SIZE );
		} catch (TurboException eaten) {
			// Do nothing: we are done here.
		} finally {
			if ( !isEmbedded && isInitialised )
				deinitialise();
		}
	}

	/**
	 * Do Binary (two state: ie on & off) Chaining.
	 * <p>
	 * Given an initial On assumption: {@code Cell cell} is {@code int value},
	 * find the binary chains consequent to both states (On and Off) such
	 * that: <ul>
	 *  <li>{@code On = new Ass(cell, value, true)} (ie cell+value); and
	 *  <li>{@code Off = new Ass(cell, value, false)} (ie cell-value)
	 * </ul>
	 * <p>
	 * Create the following sets: <ul>
	 *  <li><b>{@code onToOn}</b> consequent Ons of {@code On}
	 *  <li><b>{@code onToOff}</b> consequent Offs of {@code On}
	 *  <li><b>{@code offToOn}</b> consequent Ons of {@code Off}
	 *  <li><b>{@code offToOff}</b> consequent Offs of {@code Off}
	 * </ul>
	 * <p>
	 * Then apply these techniques: <ul>
	 *  <li><b>Contradiction:</b><ul>
	 *   <li>If a consequence is in both {@code onToOn} and {@code onToOff}
	 *    then the initial-On-Assumption is false because it implies that a
	 *    cell-value is both On and Off, which is absurd.
	 *   <li>If a consequence is in both {@code offToOn} and {@code offToOff}
	 *    then the initial-Off-Assumption is false because it implies that a
	 *    cell-value is both On and Off, which is absurd. <br>
	 *    NOTE: these are much rarer but set the cells value directly.
	 *  </ul>
	 *  <li><b>Reduction:</b><ul>
	 *   <li>If an assumption is in both {@code onToOff} and {@code offToOff}
	 *    then this assumption must be Off, because it is implied to be Off
	 *    by both possible states of the initialAssumption (true or false).
	 *   <li>If an assumption is in both {@code onToOn} and {@code offToOn}
	 *    then this assumption must be On, because it is implied to be On
	 *    by both possible states of the initialAssumption (true or false). <br>
	 *    NOTE: these are much rarer but set the cells value directly.
	 *  </ul>
	 *  <li>NB: if an assumption belongs to all the four sets then the
	 *   Sudoku has no solution. This is not checked. Not my problem.
	 *  <li>NB: Circular Chains (hypothesis implying its negation)
	 *   are already covered by Cell Chains, so are not sought.
	 * </ul>
	 * <p>doBinaryChains is called by getMultipleOrDynamicChains for each maybe
	 * of each unset cell, which means quite often: 254,314 times in top1465 so
	 * the performance of code in ANY loops here-under really really matters.
	 *
	 * @param indice of cell of the initial Assumption
	 * @param value of the initial Assumption (is both On and Off)
	 * @param reduce should I search for Reduction hints?
	 */
	private void doBinaryChains(final int indice, final int value, final boolean reduce) {
		Ass[] con; // contradiction: {On, Off}
		// test Off: assume that cell IS NOT value, calculate consequences,
		// returning any contradiction.
		// Do Off first coz its contradiction is an On-hint (set cell) which is
		// more valuable, but rarer because only ~30% of Offs cause an On, so
		// most fail at the first step. Doing Ons first is actually faster, but
		// Offs first produces shorter solutions. I do Offs first because I am
		// no-longer obsessed with performance, so shorter solutions preferred.
		// nb: cannot reduce sets that contain contradiction/s because reduce
		// (on/off commonality) logic broken; so reduce produces invalid hints.
		final Ass off = new Ass(indice, value, OFF); // $cell-$value
		if ( (con=doChains(off, offToOns, offToOffs)) != null
		  // anOff is false because it causes this contradiction
		  && hints.add(binaryChain(con[0], con[1], off, off, ON, T, 1)) ) {
			// dont forget to cleanUp the offTo* Sets. Sigh.
			offToOns.clear();
			offToOffs.clear();
			return;
		}
		// test On: assume that cell IS value, calculate consequences,
		// returning any contradiction.
		final Ass on = new Ass(indice, value, ON); // $cell+$value
		if ( (con=doChains(on, onToOns, onToOffs)) != null
		  // anOn is false because it causes this contradiction
		  && hints.add(binaryChain(con[0], con[1], on, on, OFF, T, 0)) ) {
			// dont forget to cleanUp the offTo* Sets. Sigh.
			offToOns.clear();
			offToOffs.clear();
			return;
		}
		// reduce is size > 2
		if ( reduce ) {
			// reduce: the same consequence (a cell-value) from On or Off.
			// (a) Look for any consequences that are On in both cases
			reduce(onToOns, offToOns, ON, on, 2);
			// (b) Look for any consequences that are Off in both cases
			reduce(onToOffs, offToOffs, OFF, off, 4);
		}
		// Finished with the offTo* Sets now. The offs are used in reduction
		// but not in regionReduction, whereas the onTo* Sets are.
		offToOns.clear();
		offToOffs.clear();
	}

	/**
	 * reduce raises a Binary Reduction Chain hint for any matches in the two
	 * sets ons, offs.
	 * <p>
	 * Basically, if ons and offs contain the same assumption then we can raise
	 * a hint to set/eliminate this assumption; because it is a consequence of
	 * an assumption being both on (true) and off (false); and an assumption
	 * has only two possibilities, hence we can raise a binary reduction hint
	 * (a BinaryChainHint, with isContradiction=false).
	 * <p>
	 * <b>WARN</b>: ons and offs are re-used by doRegionChains and findChains,
	 * so do NOT eat them here! Ergo, dont use retainAll to find commonality.
	 *
	 * @param ons either onTo<b>Ons</b> or offTo<b>Ons</b>
	 * @param offs either onTo<b>Offs</b> or offTo<b>Offs</b>
	 * @param isOn <b>true</b> for the To<b>Ons</b> lists; <br>
	 *  or <b>false</b> for the To<b>Offs</b>. <br>
	 *  Changes the calculation of the removable (red) Pots:<br>
	 *  <b>true</b>: {@code redVals=target.cell.maybes.minus(target.value)}<br>
	 *  <b>false</b>: {@code redVals=target.value}
	 * @param initialAss the initialAss
	 * @param typeId tells programmer where this hint comes from. The user sees
	 *  these when {@link #WANT_TYPE_ID} is true, which breaks the test-cases.
	 *  False for release. To non-techies its just annoying noise.
	 */
	private void reduce(final LinkedMatrixAssSet ons, final LinkedMatrixAssSet offs
			, final boolean isOn, final Ass initialAss, final int typeId) {
		final int numOns=ons.size, numOffs=offs.size;
		// if any offs and any ons
		if ( numOffs>0 && numOns>0 ) {
			// faster to iterate smaller set (LMAS.get beats iteration)
			if ( numOns < numOffs ) {
				// foreach on
				final LinkedMatrixAssSet.Node[][] offNodes = offs.nodes;
				LinkedMatrixAssSet.Node n = ons.head;
				do
					if ( offNodes[n.ass.indice][n.ass.value] != null ) {
						final Ass on = n.ass;
						final Ass off = offNodes[on.indice][on.value].ass;
						hints.add(binaryChain(on, off, initialAss, on, isOn, F, typeId));
					}
				while ( (n=n.next) != null );
			} else {
				// foreach off
				final LinkedMatrixAssSet.Node[][] onNodes = ons.nodes;
				LinkedMatrixAssSet.Node n = offs.head;
				do
					if ( onNodes[n.ass.indice][n.ass.value] != null ) {
						final Ass off = n.ass;
						final Ass on = onNodes[off.indice][off.value].ass;
						hints.add(binaryChain(on, off, initialAss, on, isOn, F, typeId+1)); // NOTE the +1
					}
				while ( (n=n.next) != null );
			}
		}
	}

	/**
	 * Do Region Chaining: A region chain occurs when all possible positions
	 * of a value have the same consequence.
	 * <p>
	 * EG: No matter where we put the 5 in box 4 then F6 cannot be 2.
	 * <p>
	 * 444 calls/puzzle for first 10 in top1465.d2, ie not too many, however
	 * this is still the slowest/heaviest method in the solve process.
	 * <p>
	 * The only reason the 2 "sib" Sets (and effects) are passed-in is
	 * efficiency: We avoid creating 2 {@code LinkedMatrixAssSet}s (a slow
	 * process) for each potential value of each Cell (mathematically max 1,152
	 * Sets per call to getMultipleDynamicChains). Instead, we create the 2
	 * Sets ONCE and clear them as required, which is a bit faster. They are
	 * not intended for use outside of this method.
	 * <p>
	 * In the params:<br>
	 * <b>IN</b> means s__t to search. <br>
	 * <b>OUT</b> means this collection is populated for external reference. <br>
	 * <b>MY</b> means not for external reference.
	 *
	 * @param cell IN the Cell of the initial On assumption (anOn).
	 * @param v IN the value of the initial On assumption (anOn).
	 * @param hints The HintCache to which I add any RegionReductionHints.
	 */
	private void doRegionsChains(final int indice, final int v) {
		Ass a; // current Assumption
		ARegion cRegions[], region; // cells[indice].regions, one of
		int indices[] // region.indices
		, places[], pn, pi, place // array, size, index, place
		, ri; // index in cRegions
		boolean anyOns, anyOffs; // any Ons/Offs?
		boolean earlyExit = true; // presume we will trip over an exception
		try {
			cRegions = cells[indice].regions;
			ri = 0;
			do { // box/row/col
				// I thinks its faster to not imbed this in the next line,
				// which is already too busy, else it'll run out registers
				region = cRegions[ri];
				places = INDEXES[region.places[v]];
				// if region has two-or-more places for this value
				if ( (pn=places.length) > 1
				  // and we are seeing this region for the first time, to avert
				  // examining the whole region foreach place therein, which
				  // just wastes time to produce repeat hints (no new ones)
				  && (indices=region.indices)[place=places[0]] == indice
				) {
					// for the first place for value in this region
					posOns[place].addAll(onToOns);
					posOffs[place].addAll(onToOffs);
					rgnOns.addAll(onToOns);
					rgnOffs.addAll(onToOffs);
					anyOns = anyOffs = true; // stop when both false
					assert sibOns.isEmpty();
					assert sibOffs.isEmpty();
					// foreach subsequent place for value in this region
					pi = 1;
					do {
						place = places[pi];
						// get effects of $places[j]+$value in sibToOns/Offs
						doChains(new Ass(indices[place], v, ON), sibOns, sibOffs);
						if ( anyOns && (anyOns=rgnOns.retainAllAny(sibOns)) )
							posOns[place].pollAll(sibOns);
						else
							sibOns.clear();
						if ( anyOffs &= rgnOffs.retainAllAny(sibOffs) )
							posOffs[place].pollAll(sibOffs);
						else
							sibOffs.clear();
					} while ( ++pi < pn );
					// turn any surviving regionOns into hints.
					if ( anyOns )
						while ( (a=rgnOns.poll()) != null )
							hints.add(regionReduction(region, v, a, posOns));
					// turn any surviving regionOffs into hints.
					if ( anyOffs )
						while ( (a=rgnOffs.poll()) != null )
							hints.add(regionReduction(region, v, a, posOffs));
					// clear the position arrays // quicker than overloading the GC
					pi = 0;
					do {
						place = places[pi];
						posOns[place].clear();
						posOffs[place].clear();
					} while ( ++pi < pn );
				}
			} while ( ++ri <  3 );
			earlyExit = false;
		} finally {
			// cleanUp on earlyExit (exception)
			if ( earlyExit ) {
				sibOns.clear();
				sibOffs.clear();
				rgnOns.clear();
				rgnOffs.clear();
				place = 0;
				do {
					posOns[place].clear();
					posOffs[place].clear();
				} while (++place < REGION_SIZE );
			}
		}
	}

	/**
	 * Find the consequences of the initial assumption in the grid. The initial
	 * assumption may be an On (cell is value) or an Off (cell is NOT value).
	 * <p>
	 * doChains is pretty complex, so needs an explanation. This is quick and
	 * dirty, but the best I can do. I am not an edifiers asshole. Start by
	 * reading the {@link ChainerDynamic class comments} then come back.
	 * <p>
	 * Firstly "Chaining" follows the chain of consequences of an assumption.
	 * Doing forcing chains means elucidating all of the consequences of an
	 * assumption, hence ?, hence ?, hence ?, hence ?; therefore whatever.
	 * <pre>
	 * In SEs chains, the terms On and Off mean:
	 * * On means we assume that this cell is this value.
	 * * Off means we assume that this cell is NOT this value.
	 * Ons always cause Offs, usually many. About a third of Offs cause an On,
	 * rarely two. (Nested* have rare Ons that dont cause Offs).
	 * </pre>
	 * <p>
	 * We are given an initial assumption that a cell is (or is not) a value,
	 * and a couple of sets of assumptions: Ons and Offs. These two sets keep
	 * track of Asses we have already seen, to avert following an endless loop
	 * of implications, which I call a "tangle". These sets MUST be Funky, by
	 * which I mean the add method is an addOnly, which, unlike java.utils Set
	 * implementations, does NOT update the set when asked to add an item that
	 * already exists in the Set. Funky Sets just return false, like a visit
	 * operation, hence we can, and do, use them as a uniqueness test.
	 * <p>
	 * Lets start with an <b>On</b>: we assume A1 is 7 and find its direct Off
	 * effects (below). For each Off: we look for a conjugate (opposite) Ass.
	 * If conjugate exists return a contradiction (A1+7 -&gt; B4+1 and B4-1);
	 * else look if this Off is already in the set of knownOffs (avert tangle);
	 * and if not then we add it to the offsQueue. If it already exists then
	 * just ignore it: Mischief managed.
	 * <p>
	 * To process an <b>Off</b> from the offsQueue we find any Ons that are a
	 * direct consequence of this Off (below). Only about a third of Offs cause
	 * an On. If its not already a knownOff we add it to the offsQueue.
	 * <p>
	 * There are two categories of consequences in XY chains.
	 * * Y-links are based on cell maybes:
	 *   * On makes every other cell.maybe Off
	 *   * Off makes an On if this cell has two maybes then the other one is On
	 * * X-links are based on region places:
	 *   * On makes every other place for value in each cell.regions Off
	 *   * Off makes an On if there are two places for value in any cell.region
	 *     then the other place is On
	 * NOTE: XY is alphabetical, but in code its implemented YX, for efficiency
	 * <p>
	 * We erase (remove) the Off from the grid, to reveal new bivalueCells and
	 * biplacedRegions, and also when isFoxy (degree>0) the FourQuickFoxes and
	 * any imbedded chainers can see our Assumptions. We then process each new
	 * On (as above): first look for the conjugate (opposite) and return a
	 * contradiction; else if On is new (avert tangle) add to onsQueue.
	 * <p>
	 * So we immediately process ALL Ons to produce Offs, and then we process
	 * Offs which might produce an On; until we reach the end of the consequence
	 * tree. Some Off assumptions have zero consequences. Some assumptions can
	 * solve the whole grid! You never know until you try.
	 * <p>
	 * Finally, if isFoxy and theres no next-link then run the FourQuickFoxes:
	 * Locking, NakedPair, HiddenPair, SwampFish, plus (in Nested*) one or two
	 * imbedded chainers; and converts there hints into {@link Cause.Foxy} Offs,
	 * which are then processed as per normal; sans any further algorithmic
	 * consequences. Smart!
	 * <p>
	 * The {@link ChainerHintsAccumulator} is brilliant, especially separation
	 * of concerns, with each {@link diuf.sudoku.solver.hinters.IFoxyHint}
	 * finding its own parentage. Kudos to Juillerat.
	 * <p>
	 * In order to build chains (back-linked consequence trees) in dynamic mode
	 * we need to go back and play "Whos your daddy?" with our effect Ass. That
	 * means finding all parent Asses, which are all true in order for our Ass
	 * to exist, using the initialGrid. Offs are erased from the current grid,
	 * which is later restored to its initial state (initGrid) upon exit. The
	 * initGrid is a field to save creating a new Grid (expensive) for each and
	 * every cell value (there are hundreds of them in each grid); instead we
	 * copyTo an existing grid (this is still expensive, just less so). If I
	 * could have anything it would be a faster Grid backup/restore in binary,
	 * like clone, but I am stupid, so copyFrom is as good as I can make it.
	 * <p>
	 * doChains is called twice by {@link #doBinaryChains }
	 * and once by {@link #doRegionsChains },
	 * which are both called only by {@link #findChainHints }.
	 * <p>
	 * doChains is called 942,316 times in top1465, so performance is important
	 * everywhere in this class. Its fast compared to HoDoKu nets.
	 * <p>
	 * Q: This method seems to NEED both its queues: it was easy to remove the
	 * dual queues from ChainerUnary, and I have tried 4 times to do so here,
	 * and each time it fails, and Im stumped.
	 * A: Finding an Ons consequences requires the maybes that caused this On,
	 * hence we must find the Off consequences of all Ons BEFORE we erase any
	 * Off; so we use two queues, so that all Ons go first.
	 * <p>
	 * And thats about it. Endless hours of family entertainment fornicating
	 * with four foxes, whos your daddy, and why rodger a rabbit? I still have
	 * a thing for rangas. Sigh.
	 * <p>
	 * <pre>
	 * <b>PSEUDOCODE</b>:{@code
	 *
	 * doChains calculates the consequence-tree of anAss in the grid.
	 * Given an initial assumption, anAss (either an On or an Off),
	 * and a pair of IAssSet ons and offs;
	 * doChains populates ons and offs (my actual output),
	 * returning the first contradiction encountered, if any.
	 *
	 * Dynamic means that each Off is erased from the grid, to reveal any new
	 * bivalue cells and/or biplaced regions, to extend the consequence tree.
	 * Multiple (I think) means starts from cells with three or more values.
	 * NishioChain (Dynamic Contradiction) is NOT a ChainerDynamic Tech, I do:
	 * DynamicChain, DynamicPlus, NestedUnary, NestedStatic, NestedDynamic,
	 * and NestedPlus.
	 *
	 * PRE:
	 * * grid and initialGrid are preset for me in findHints(Grid, IAccu).
	 * * grid component fields (regions, maybes, sizes, etc) are preset.
	 * * initialGrid is a copy of grid at start of findHints; sans-erasures.
	 * * Both of the given ons and offs IAssSets ARE Funky, which means add is
	 *   addOnly: does NOT update pre-existing asses, it just returns false.
	 * * Dequeu<Ass> ONQ, OFFQ // two EMPTY Ass queues // now endless arrays
	 *
	 * POST:
	 * the given ons and offs IAssSet are populated (appended) with
	 * all consequences of the given initial assumption anAss;
	 * returning any Contradiction immediately.
	 *
	 * PARAMS:
	 * Ass anAss // INPUT the initial assumption: let us assume that cell is value
	 * IAssSet ons // OUTPUT On consequences
	 * IAssSet offs // OUTPUT Off consequences
	 *
	 * Ass a = anAss // the initial assumption
	 * (a.isOn ? ons : offs).add(a);
	 * do {
	 *    if a.isOn then // a is an On: its effects are Offs
	 *       // (1) Y-Link cells other values are Off
	 *       foreach otherValue in a.cell.maybes except a.value do
	 *          off = new Ass(a.cell, otherValue, OFF, a)
	 *          if (on=ons.get(off.cell, off.value)) != null then
	 *             return new Ass[]{on, off} // Contradiction
	 *          elif offs.add(off) then // add is Funky!
	 *             OFFQ.add(off)
	 *          endif
	 *       next
	 *       // (2) X-Link other places in cells regions are Off
	 *       foreach region in a.cell.regions do
	 *          foreach otherPlace in region.placesFor[a.value] except a.placeIn[region] do
	 *             off = new Ass(otherPlace, a.value, OFF, a)
	 *             if (on=ons.get(off.cell, off.value)) != null then
	 *                return new Ass[]{on, off}; // Contradiction
	 *             elif offs.add(off) then // add is Funky!
	 *                OFFQ.add(off)
	 *             endif
	 *          next
	 *       next
	 *    else // a is an Off: its effects, if any, are Ons
	 *       // (1) Y-Link if cell has 2 maybes the other one is On
	 *       if a.cell.size == 2 then
	 *          otherValue = VFIRST[a.cell.maybes & ~VSHFT[a.cell.value]];
	 *          if (off=offs.get(a.cell, otherValue)) != null then
	 *             on = new Ass(a.cell, otherValue, ON, a)
	 *             addHiddenParentsCell(on);
	 *             return new Ass[]{on, off}; // Contradiction
	 *          elif !ons.exists(a.cell, otherValue) then // nodes used!
	 *             on = new Ass(a.cell, otherValue, ON, a)
	 *             addHiddenParentsCell(on);
	 *             if ons.add(on) then // add is Funky!
	 *                ONQ.add(on)
	 *             endif
	 *          endif
	 *       endif
	 *       // (2) X-Link if cells region has 2 places other one is On
	 *       foreach region in a.cell.regions
	 *          if region.numPlaces[a.value] == 2 then
	 *             otherPlace = region.indices[region.places[a.value] & ~a.cell.placeIn[region.type]];
	 *             if (off=offs.get(otherPlace, a.value)) != null then
	 *                on = new Ass(otherPlace, a.value, ON, a)
	 *                addHiddenParentsRegion(on);
	 *                return new Ass[]{on, off}; // Contradiction
	 *             elif !ons.exists(otherPlace, a.value) then // nodes used!
	 *                on = new Ass(otherPlace, a.value, ON, a)
	 *                addHiddenParentsRegion(on);
	 *                if ons.add(on) then // add is Funky!
	 *                   ONQ.add(on)
	 *                endif
	 *             endif
	 *          endif
	 *       next
	 *       grid.erase(a); // eliminate a.value from a.cell in the grid!
	 *    endif
	 *    if isFoxy then // ChainerPlus or Nested*: fourQuickFoxes++
	 *       if effects=getHinterEffects(offs) != null then
	 *          foreach effect in effects do
	 *             OFFQ.add(effect)
	 *          next
	 *       endif
	 *    endif
	 *    if (a=ONQ.poll()) == null then
	 *       a = OFFQ.poll()
	 *    endif
	 * } while (a != null);
	 *
	 * // offs of other maybes of this cell are my hidden parents
	 * // nb: this method has been inlined, for speed
	 * void addHiddenParentsCell(Ass effect, int indice, int value) {
	 *    foreach otherValue in initMaybes[indice] & ~maybes[indice] do
	 *       if (parent=offs.get(indice, otherValue)) != null then
	 *          effect.parents.linkLast(parent);
	 *       endif
	 *    next
	 * }
	 *
	 * // offs of other places for value in this region are my hidden parents
	 * // nb: this method has been inlined, for speed
	 * void addHiddenParentsRegion(Ass effect, int indice, int value) {
	 *    foreach place in INDEXES[initRegions[region.indice].places[value] & ~places] do
	 *       if (parent=offs.get(r.indices[place], value)) != null then
	 *          effect.parents.linkLast(parent);
	 *       endif
	 *    next
	 * }
	 *
	 * FASTARDISATIONS IN THIS IMPLEMENTATION:
	 * 1. Eliminate stackwork, or atleast reduce it as much as possible.
	 * a) Predeclare ALL variables (ANSII-C style) for ONE stackframe.
	 * b) This includes the iteration vars, which is faster, but the code is
	 *    messy (overly complex). You can't have everything!
	 *    BUG_FIX: pretest i&ltn EVERYWHERE for Nested*.
	 *    Post-testing causes AIOOBEs from the odd-ball cases,
	 *    and its VERY hard to define all possible odd-ball cases,
	 *    hence we go defensive everywhere, and be done with it.
	 *    This includes ALL the chainers, coz they're imbedded.
	 * c) inline all invocations except new Ass and the Set.add method,
	 *    which I cannot work-out how to inline, but suspect its possible.
	 * 2. Avert creating unused effects, to reduce garbage, hence reduce GC.
	 *    Delay creating the effect Ass until we know we need it, either in a
	 *    Contradiction, or not in master set via evil public field LMAS.nodes;
	 *    which shamelessly leaks implementation detail because an array-lookup
	 *    is faster than any method invocation, for speed.
	 * 3. 1c caused LMAS.justAdd which does NOT check exists (it just adds) and
	 *    returns the given Ass to add to the appropriate Queue, for speed.
	 * 4. The two addHiddenParents methods have been inlined, for speed, but
	 *    they are ____all faster, and the code is a mess.
	 * 5. ONQ and OFFQ are now arrays (not Deques) all inline, for speed.
	 * }</pre>
	 *
	 * @param a the initialAssumption, either an On or an Off.
	 * @param ons OUTPUT the *ToOn consequences of initialAss.
	 * @param offs OUTPUT the *ToOff consequences of initialAss.
	 * @return first contradiction: an On and an Off (ordered). anAss causes
	 *  both the On and Off of a cell-value, which is absurd, hence anAss is
	 *  false. For example: A1+1 -&gt H6+6 and H6-6, hence A1 is not 1. A
	 *  return value of null means there is no contradiction. This is complete
	 *  "hard" logic, so null does not just mean not found, it means that no
	 *  contradiction exists. Complete logic is one out all out. All steps are
	 *  required. There is no partial implementation.
	 */
	private Ass[] doChains(
		  Ass a // initial assumption (an On or an Off)
		, final LinkedMatrixAssSet ons // OUT
		, final LinkedMatrixAssSet offs // OUT
	) {
		Ass e; // an effect of assumption ass
		Cell cell; // ass.cell (the current cell)
		ARegion cRegion; // one of the ass.cell.regions
		int indices[] // region.indices
		  , cPlaceIn[] // cell.placeIn
		  , array[], n, m, i // array, size, last, index (ONE stackframe)
		  , value // ass.value
		  , indice // ass.cell.i, hijacked as siblingIndice
		  , other // two uses: otherValue or otherIndex
		  , places // region.places[value], hijacked as erased places
		;
		int onR=0, onW=0, offR=0, offW=0; // ONQ/OFFQ read/write index
		boolean earlyExit = true; // presume we wont make it to end of method
		final Deque<Ass> foxEffects;
		if ( isFoxy )
			foxEffects = new LinkedList<>();
		else
			foxEffects = null;
		final LinkedMatrixAssSet.Node[][] onNodes = ons.nodes; // pure evil!
		final LinkedMatrixAssSet.Node[][] offNodes = offs.nodes; // pure evil!
		// clearing ons and offs is upstairs responsibility, to reduce the
		// number of times we invoke clear, and thereby reduce the amount of
		// time we are wasting in stackwork and in actually clearing sets.
		// Permanent Sets were intended to be faster, but clearing them turned
		// into a complex nightmare, implemented slap-dash, which is SLOWER!
		// So now some rigour: Dont clear a set unless you must! FMS! If you
		// must then you MUST. Do or do not. There is no try. Slapdash be gone!
		assert ons.isEmpty();
		assert offs.isEmpty();
		// add the initialAssumption to the appropriate Set
		if ( a.isOn )
			ons.add(a);
		else
			offs.add(a);
		// snap-shot the grid, to find parents (isFoxy), and reversion.
		initGrid.copyFrom(grid);
		try {
			for(;;) {
				value = a.value;
				cell = cells[indice=a.indice];
				if ( a.isOn ) { // every On has Off effect/s, most have many.
					// (1) Y-Link: all other maybes of this cell are Off
					for ( array=VALUESES[cell.maybes & ~VSHFT[value]], n=array.length, i=0; i<n; ++i )
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice][other=array[i]] != null )
							return new Ass[] {
								onNodes[indice][other].ass
							  , new Ass(indice, other, OFF, a, NakedSingle, ONEVALUE)
							};
						// avert tangle: process each Off ONCE
						else if ( offNodes[indice][other] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, other, OFF, a, NakedSingle, ONEVALUE));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					// (2) X-Link: all other places for value in this cells
					//     three regions are Off
					cPlaceIn = cell.placeIn;
					indices = (cRegion=cell.box).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[BOX]], n=array.length, i=0; i<n; ++i )
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice=indices[array[i]]][value] != null )
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX])
							};
						// avert tangle: process each Off ONCE
						else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					indices = (cRegion=cell.row).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[ROW]], n=array.length, i=0; i<n; ++i )
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice=indices[array[i]]][value] != null )
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW])
							};
						// avert tangle: process each Off ONCE
						else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					indices = (cRegion=cell.col).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[COL]], n=array.length, i=0; i<n; ++i )
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice=indices[array[i]]][value] != null )
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL])
							};
						// avert tangle: process each Off ONCE
						else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
				} else { // ass is an Off: about 30% of Offs cause On effects
					// (1) Y-Link: if this cell has two maybes then the other
					//     potential value is On
					if ( cell.size == 2 ) {
						other = VFIRST[cell.maybes & ~VSHFT[value]];
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice][other] != null ) {
							e = new Ass(indice, other, ON, a, NakedSingle, ONLYVALUE);
							if ( (places=initMaybes[indice] & ~cell.maybes) > 0 ) // 9bits
								for ( m=VLAST[places], array=VALUESES[places], i=0; ; ) {
									e.addParent(offNodes[indice][array[i]].ass);
									if(++i > m) break;
								}
							return new Ass[] {e, offNodes[indice][other].ass};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][other] == null ) {
							e = new Ass(indice, other, ON, a, NakedSingle, ONLYVALUE);
							if ( (places=initMaybes[indice] & ~cell.maybes) > 0 ) // 9bits
								for ( m=VLAST[places], array=VALUESES[places], i=0; ; ) {
									e.addParent(offNodes[indice][array[i]].ass);
									if(++i > m) break;
								}
							ONQ[onW] = ons.justAdd(e);
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					// (2) X-Link: if any of the three regions of this cell has
					//     just two places for value then other place is On
					cPlaceIn = cell.placeIn;
					if ( (cRegion=cell.box).numPlaces[value] == 2 ) {
						places = cRegion.places[value];
						indices = cRegion.indices;
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice=indices[IFIRST[places & ~cPlaceIn[BOX]]]][value] != null ) {
							e = new Ass(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]);
							if ( (places=initRegions[cRegion.index].places[value] & ~places) > 0 ) // 9bits
								for ( m=ILAST[places], array=INDEXES[places], i=0; ; ) {
									e.addParent(offNodes[indices[array[i]]][value].ass);
									if(++i > m) break;
								}
							return new Ass[] {e, offNodes[indice][value].ass};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][value] == null ) {
							e = new Ass(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]);
							if ( (places=initRegions[cRegion.index].places[value] & ~places) > 0 ) // 9bits
								for ( m=ILAST[places], array=INDEXES[places], i=0; ; ) {
									e.addParent(offNodes[indices[array[i]]][value].ass);
									if(++i > m) break;
								}
							ONQ[onW] = ons.justAdd(e);
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					if ( (cRegion=cell.row).numPlaces[value] == 2 ) {
						places = cRegion.places[value];
						indices = cRegion.indices;
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice=indices[IFIRST[places & ~cPlaceIn[ROW]]]][value] != null ) {
							e = new Ass(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]);
							if ( (places=initRegions[cRegion.index].places[value] & ~places) > 0 ) // 9bits
								for ( m=ILAST[places], array=INDEXES[places], i=0; ; ) {
									e.addParent(offNodes[indices[array[i]]][value].ass);
									if(++i > m) break;
								}
							return new Ass[] {e, offNodes[indice][value].ass};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][value] == null ) {
							e = new Ass(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]);
							if ( (places=initRegions[cRegion.index].places[value] & ~places) > 0 ) // 9bits
								for ( m=ILAST[places], array=INDEXES[places], i=0; ; ) {
									e.addParent(offNodes[indices[array[i]]][value].ass);
									if(++i > m) break;
								}
							ONQ[onW] = ons.justAdd(e);
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					if ( (cRegion=cell.col).numPlaces[value] == 2 ) {
						places = cRegion.places[value];
						indices = cRegion.indices;
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice=indices[IFIRST[places & ~cPlaceIn[COL]]]][value] != null ) {
							e = new Ass(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]);
							if ( (places=initRegions[cRegion.index].places[value] & ~places) > 0 ) // 9bits
								for ( m=ILAST[places], array=INDEXES[places], i=0; ; ) {
									e.addParent(offNodes[indices[array[i]]][value].ass);
									if(++i > m) break;
								}
							return new Ass[] {e, offNodes[indice][value].ass};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][value] == null ) {
							e = new Ass(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]);
							if ( (places=initRegions[cRegion.index].places[value] & ~places) > 0 ) // 9bits
								for ( m=ILAST[places], array=INDEXES[places], i=0; ; ) {
									e.addParent(offNodes[indices[array[i]]][value].ass);
									if(++i > m) break;
								}
							ONQ[onW] = ons.justAdd(e);
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					// remove $cell-$value from the grid
					cell.removeMaybes(VSHFT[value]);
				}
				// if ima DynamicPlus or Nested* and there is no next link
				if ( onW==onR && offW==offR && isFoxy
				  // and the imbedded hinters find next links
				  && fourQuickFoxes(offs, foxEffects) )
					// add these "Foxy" next links, then continue as normal
					while ( (e=foxEffects.poll()) != null )
						if ( offs.add(e) ) {
							OFFQ[offW] = e;
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
				// WARN: Must process all Ons before any Off
				if ( (a=ONQ[onR]) != null ) {
					ONQ[onR] = null;
					onR = (onR + 1) & ON_MASK;
				} else if ( (a=OFFQ[offR]) != null ) {
					OFFQ[offR] = null;
					offR = (offR + 1) & OFF_MASK;
				} else
					break;
			}
			earlyExit = false;
			if ( turbo && isSolved() ) {
				hints.clear(); // Turbo is the ONLY hint
				hints.add(new TurboHint(grid, this, a, turboPots()));
				throw TURBO_EXCEPTION;
			}
		} finally {
			// revert grid to how it was upon arrival here
			grid.copyFrom(initGrid);
			// clear on earlyExit, the set Q elements only.
			if ( earlyExit ) {
				if ( onR < onW )
					fill(ONQ, onR, onW+1, null);
				else if ( onR > onW )
					fill(ONQ, onW, onR+1, null);
				if ( offR < offW )
					fill(OFFQ, offR, offW+1, null);
				else if ( offR > offW )
					fill(OFFQ, offW, offR+1, null);
			}
		}
		return null;
	}

	/**
	 * Run the FourQuickFoxes: Locking, HiddenPair, NakedPair, and Swampfish,
	 * (plus any Nested Chainers over the grid, which IS modified; convert the
	 * hints into Asses and add them to effects (CHA does that), returning any.
	 * <p>
	 * @See #ChainerDynamic(diuf.sudoku.Tech, java.util.Map, boolean)
	 * for the nested/imbedded chainers in each Nested* Tech.
	 *
	 * @param rents the existing Off Asses, from which I take parents of each
	 *  Ass I create.
	 * @param effects the effects Collection I add to (and size)
	 * @return where any Asses found
	 */
	private boolean fourQuickFoxes(final IAssSet rents, final Collection<Ass> effects) {
		// Cleetus takes tea with his gopher. Strong and black. No sugar.
		final ChainerHintsAccumulator cha = new ChainerHintsAccumulator(initGrid, grid, rents, effects);
		int i = 0;
		do
			if ( foxes[i].findHints(grid, cha) )
				return true;
		while (++i < numFoxes);
		return false;
	}

	/**
	 * Is the grid solved, with a valid solution?
	 * <p>
	 * This method is only used to implement turbo.
	 *
	 * @return is the grid solved, with a valid solution?
	 */
	private boolean isSolved() {
		// is solved: does each cell have 1 value or 1 maybe?
		for ( Cell cell : cells )
			if ( cell.value==0 && cell.size>1 )
				return false;
		// valid solution: does each region have 9 values in its 9 cells?
		int cands, v, m;
		for ( ARegion region : grid.regions ) {
			cands = 0;
			for ( Cell cell : region.cells ) {
				v = cell.value;
				m = cell.maybes;
				assert (v==0 && VSIZE[m]==1) || (v!=0 && m==0);
				cands |= VSHFT[v] | m;
			}
			if ( cands != BITS9 )
				return false;
		}
		return true;
	}

	/**
	 * Pass me a solved grid and I return a nice big Pots of all the
	 * cell=&gt;values to set.
	 *
	 * @return a nice big Pots
	 */
	private Pots turboPots() {
		final Pots result = new Pots(128, 1F);
		for ( Cell cell : cells )
			if ( cell.value==0 && cell.size==1 )
				result.put(cell.indice, cell.maybes);
		return result;
	}

	/**
	 * Implements ICleanUp.
	 */
	@Override
	protected final void cleanUpImpl() {
	}

	private static class TurboException extends RuntimeException {
		private static final long serialVersionUID = 494578239813L;
	}

	// THE single instance of TurboException, because it takes about as long to
	// construct an Exception as is does to solve a whole Sudoku. I do not care
	// about the stacktrace, because this exception is ALWAYS eaten.
	private static final TurboException TURBO_EXCEPTION = new TurboException();

}
