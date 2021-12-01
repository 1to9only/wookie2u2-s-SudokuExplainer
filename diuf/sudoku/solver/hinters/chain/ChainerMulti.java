/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.ICleanUp;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.LinkedMatrixAssSet;
import diuf.sudoku.solver.hinters.fish.BasicFisherman;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.nkdset.NakedSet;
import diuf.sudoku.solver.hinters.lock.Locking;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.NULL_ST;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMyPollSet;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.MyArrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A MultipleChainer is an engine for searching a Grid for multiple and
 * possibly dynamic forcing chains. This is an implementation of Sudoku solving
 * techniques which involve following a chain of implications consequent to a
 * simple assumption of the value/not-value of a Cell in the Grid, involving
 * Multiple Chains with cells with more than two possible values and regions
 * with more than two possible locations for a value; and also Dynamic Chaining
 * which involves erasing each assumptions maybes from the grid,  enabling us
 * to come back later to find the parent Ass's which caused these erasures.
 * <p>KRC 2019-11-01 I've split the existing Chainer class into MultipleChainer
 * and UnaryChainer. See UnaryChainer comments for more details.
 */
public final class ChainerMulti extends ChainerBase
		implements ICleanUp
{

//	/**
//	* Sexy DEBUG feature to let you watch the chaining happen, step-by-step.
//	*
//	* I've instituted a feature in ChainerMulti that allows you to watch a grid
//	* being erased-from in the GUI (ie the chaining process). It's a bit grubby
//	* but fulfills my current needs. Ideally, it would look more like a
//	* chaining-hint, but I'm lazy and it's only a debug feature.
//	*
//	* To use this feature:
//	* * uncomment the 'df' variable and all references, and the GWH class.
//	* * In ChainerMulti search for GRID_WATCH to find the set-up phrase and
//	*   change the if statement so that it matches ONLY the interesting hint/s.
//	* * Click on all the Debug.breakpoint lines to set the bloody breakpoints!
//	* * run the GUI in your IDE's debug mode, open your puzzle (mine is below)
//	* * TIP: You'll probably do it repeatedly, so it's easier to start the GUI,
//	*   open the file, and close the GUI so it updates the RecentFiles list.
//	*   Restart the GUI, so now (and every future time) it's auto-loaded.
//	* * Press F5 to find the next hint
//	* * Press the Delete key to disable any/all hinters which find hints BEFORE
//	*   the chainer. You'll know when you're there when you hit the breakpoint.
//	* * Move the IDE and the GUI so that you can see both. I have GUI to left
//	*   and IDE to right, to see the grid behind the IDE. Make sure you're
//	*   focused on the IDE before you press F8 to step-through the code, or you
//	*   get the bloody solution, again. sigh.
//	*
//	* 7.8...3.....2.1...5.........4.....263...8.......1...93.9.6....4....7.5...........
//	* ,12,,459,4569,4569,,1456,125,49,36,3469,,3456,,467,4578,578,,1236,12346,78,346,78,269,146,129,189,,1579,3579,59,3579,178,,,,1267,1269,479,,24679,147,1457,157,268,578,2567,,2456,2467,478,,,128,,1357,,125,2358,127,1378,,12468,2368,12346,3489,,23489,,1368,1289,12468,3578,1234567,34589,1249,234589,2679,13678,12789
//	* C:\Users\User\Documents\SudokuPuzzles\Test\DynamicPlusTest_MiaRegionReductionChain.txt
//	* HINT NOT FOUND: Region Reduction Chain: 5 in box 2 so C5+9
//	* 17703031 Region Reduction Chain: 5 in box 2 so B8+8 (B8+8 B8-236)
//	*  8937795 Region Reduction Chain: 5 in box 2 so D5+4 (D5+4 D5-79)
//	* 19300404 Region Reduction Chain: 5 in box 2 so E4+9 (E4+9 E4-5)
//	*       19 Binary Contradiction Chain: D1+5 so G6+/-8 (D1-5)
//	*/
//	private static final boolean WATCH_GRID = true;
//	private diuf.sudoku.gui.SudokuFrame df; // debugFrame

	/**
	 * Return number of hinters in the hinters array of this degree.
	 *
	 * @param degree
	 * @return 1=4, even=5, or odd=6
	 */
	private static int numHinters(int degree) {
		if ( degree == 1 )
			return 4;
		else if ( degree%2 == 0 )
			return 5;
		return 6;
	}

	/**
	 * An array of Four Quick Foxes (et al) used by the
	 * {@code getHintersEffects(...)} method. Populated in constructor.
	 */
	protected final IHinter[] hinters;

	/**
	 * The grid used by this call to getHints. I think that a field might be
	 * faster than passing it around, It's certainly cleaner.
	 */
	protected Grid grid;

	/**
	 * The initial (unmodified) grid used to find parent (causal) Assumptions.
	 */
	protected Grid theInitialGrid;

	/**
	 * if doChains solves do you want the solution?
	 */
	private boolean turbo;

	/**
	 * The actual Constructor is private. It is exposed by the following public
	 * constructor to validate and provide the default isImbedded=false.
	 *
	 * @param tech a Tech with isChainer==true
	 * @param basics from which, if not null, I retrieve the Four Quick Foxes
	 *  (simple hinters whose single instances are owned by the LogicalSolver)
	 *  else I just create my own instance of each of these hinters
	 * @param noCache true ONLY when this is an imbedded (nested) Chainer.
	 *  true prevents the superclass ChainerBase from caching my hints. It all
	 *  goes straight to hell in a hand-basket when nested hinters fail to vend
	 *  ALL hints-found at the time they are found, both coz the master-chainer
	 *  requires ALL hints, and coz the puzzle most certainly WILL have changed
	 *  the next-time we're called, coz my master-chainer is testing another
	 *  assumption about a cells value. So true means don't cache hints!
	 */
	@SuppressWarnings("fallthrough")
	private ChainerMulti(Tech tech, Map<Tech,IHinter> basics, boolean noCache) {
		super(tech, noCache);
		// any chainer except unary
		assert tech.isChainer && tech!=Tech.UnaryChain;
		// hair-dryer?
		turbo = THE_SETTINGS.getBoolean("turbo", false);
		// build the hinters array
		assert degree>=0 && degree<=5;
		if ( degree < 1 ) {
			// UnaryChain, NishioChain, MultipleChain, or DynamicChain
			this.hinters = null;
			return;
		}
		// DynamicPlus, NestedUnary, NestedMultiple, NestedDynamic, NestedPlus
		// populate the hinters array.
		this.hinters = new IHinter[numHinters(degree)];
		// imbed the Four Quick Foxes
		// ALWAYS use a new "standard" Locking
		hinters[0] = new Locking();
		// fetch the others from the LogicalSolver, if supplied
		if ( basics != null ) {
			hinters[1] = basics.get(Tech.HiddenPair);
			hinters[2] = basics.get(Tech.NakedPair);
			hinters[3] = basics.get(Tech.Swampfish);
		} else {
			hinters[1] = new HiddenSet(Tech.HiddenPair);
			hinters[2] = new NakedSet(Tech.NakedPair);
			hinters[3] = new BasicFisherman(Tech.Swampfish);
		}
		// create the nested (imbedded) chainers, by Tech (not degree, sigh)
		// retaining this chart mapped by degree (sigh)
		// 0 = UnaryChain, NishioChain, MultipleChain, DynamicChain
		// 1 = DynamicPlus    = Dynamic with Four Quick Foxes (FQF) only
		// 2 = NestedUnary    = Dynamic with FQF + Unary
		// 3 = NestedMultiple = Dynamic with FQF + Unary + Multiple
		// 4 = NestedDynamic  = Dynamic with FQF + Dynamic
		// 5 = NestedPlus     = Dynamic with FQF + Dynamic + DynamicPlus
		switch (tech) {
		case DynamicPlus: // no imbedded chainers, just above Four Quick Foxes
			break;
		case NestedMultiple: // has imbedded UnaryChain + MultipleChain
			hinters[5]=new ChainerMulti(Tech.MultipleChain, basics, T);
 			//fallthrough
		case NestedUnary: // has imbedded UnaryChain
			hinters[4]=new ChainerUnary(Tech.UnaryChain, T);
			break;
		case NestedPlus: // has imbedded DynamicChain + DynamicPlus
			hinters[5]=new ChainerMulti(Tech.DynamicPlus, basics, T);
			//fallthrough
		case NestedDynamic: // has imbedded DynamicChain
			hinters[4]=new ChainerMulti(Tech.DynamicChain, basics, T);
			//fallout
		}
	}

	/**
	 * The "basic" MultipleChainer Constructor.
	 *
	 * @param tech a Tech with isChainer==true.
	 * @param basics from which, if not null, I retrieve the Four Quick Foxes
	 *  (simple hinters whose single instances are owned by the LogicalSolver)
	 *  else I just create my own instance of each of these hinters
	 */
	public ChainerMulti(Tech tech, Map<Tech,IHinter> basics) {
		this(tech, basics, F);
		// any chainer except unary
		assert tech.isChainer && tech!=Tech.UnaryChain;
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		// cleanUp imbedded hinters
		if ( hinters != null )
			for ( IHinter hinter : hinters )
				if ( hinter instanceof ICleanUp )
					((ICleanUp)hinter).cleanUp();
		// forget my grid
		grid = null;
		// theInitialGrid is final, so just clear it
		if ( theInitialGrid != null )
			theInitialGrid.clear();
	}

	/**
	 * {@inheritDoc}
	 *
	 *<p>This implementation finds multiple and optionally dynamic chaining
	 * hints in this.grid and adds them to the given HintCache.
	 * @param hints
	 */
	@Override
	protected void findChainHints(Grid grid, HintCache hints) {
		this.grid = grid;
		assert isMultiple || isDynamic;
		if ( isNishio )
			assert isDynamic;
		// initGrid created on firstuse else LS creates * Chainers each with a
		// grid, then rarely gets down as far as Nested*, so Big RAM locked in
		// chainers that're held but never called. Downside is initGrid is not
		// final, which works so long as it's treated as if it was final.
		if ( theInitialGrid == null )
			theInitialGrid = new Grid();
		if ( !isDynamic ) // (ie isMultiple) copy ONCE at top of stack
			theInitialGrid.copyFrom(grid); // used in doChains
		// looks at cells/regions size > 2 and combines ramifications.
		findMultipleOrDynamicChains(hints);
		this.grid = null;
	}

	/**
	 * Finds Multiple and/or Dynamic Chaining hints in this.grid, by looking
	 * at cells/regions with size &gt; 2 and combining implications.
	 * <p>
	 * Note that <b>performance</b> of getMultipleOrDynamicChains is the most
	 * important to overall performance because it's a slow technique and it is
	 * the most used in: NishioChain, MultipleChain, DynamicChain, DynamicPlus,
	 * NestedUnary, NestedMultiple, NestedDynamic, and last but certainly not
	 * least: NestedPlus (10+ seconds per invocation).
	 * <p>
	 * <b>Nishio</b> seeks a contradiction in an assumptions implications.
	 * Note that isNishio implies isDynamic.
	 * <p>
	 * <b>Multiple</b>:<ul>
	 *  <li>examines cells with more than two maybes (hence the name).</li>
	 *  <li>Y-Chain means all other potential values of this cell are OFF.</li>
	 *  <li>X-Chain means that if there are only two positions for this value
	 *   in the region then the other position is ON.</li>
	 *  <li>does NOT doContradiction (detected but not reported); the grid
	 *  should (under normal circumstances) be contradiction free before a
	 *  multiple chainer is invoked, but it (mostly, sigh) works anyways.</li>
	 *  <li>ON means this cell is presumed to be this potential value</li>
	 *  <li>OFF means the cell is NOT this potential value</li>
	 * </ul>
	 * <p>
	 * <b>Dynamic</b> means that we find hints that rely on combining the
	 * previously calculated implications of the root assumption:
	 * <ul>
	 *  <li>We remove maybes (ie potential values) from the grid in the
	 *   {@code doChains} method (well actually it's buried down in the
	 *   {@code offToOns} method, where it's SAFER);</li>
	 *  <li>which allows us to comeback later and play "Who's Your Daddy" with
	 *   assumptions by comparing the current grid with it's initial-state, to
	 *   work-out which previous assumptions must be true for this assumption
	 *   to exist.</li>
	 * </ul>
	 * <p>
	 * <b>Plus</b> means that we getHintersEffects of the FOUR QUICK FOXES:
	 * Locking, HiddenPair, NakedPair, Swampfish (aka X-Wing).
	 * <p>
	 * <b>Nested</b> means that this Chainer getHintersEffects of an imbedded
	 * Chainer, so we're making assumptions on our assumptions, which is always
	 * fun. Ask a large-L-Liberal (use a whip, and a chair).
	 * <p>
	 * <b>NestedPlus</b> getHintersEffects of an imbedded Chainer which itself
	 * has imbedded FOUR QUICK FOXES, so we use hinters to find the effects of
	 * our assumptions on our assumptions, which is loads of fun, I assure you.
	 * Just stick your dick in this Mix Master and I'll plug it in for you. Are
	 * You Ready?
	 * <p>
	 * Note that the hardest Sudoku puzzles are solved by NestedUnary, so
	 * NestedPlus has only "academic value"; ie we do this just because we can
	 * and we are total smart-asses who must prove how tiny our equipment is,
	 * ie we are impossibly smart-assed smart-asses who implement the useless
	 * to add some colour and flavour to the mere impossible. Gopher Cleetus?
	 *
	 * @param hints the HintCache to be populated.
	 */
	private void findMultipleOrDynamicChains(HintCache hints) {
		// NB: isNishio implies isDynamic. Nishio means dynamic contradiction.
		final boolean doBinaryContradictions = isDynamic;
		// doBinaryReductions depends on cardinality of current cell.
		final boolean doReduction = isDynamic && !isNishio;

		// We create all the following collections ONCE up here outside loops.
		// They'd all be fields if there were no impost on accessing a field.
		// NB: LinkedMatrixAssSet constructor takes 19,549 nanos (9,270 bytes)
		// these 2 are populated by both doBinaryChains and doRegionChains
		IAssSet onToOns = new LinkedMatrixAssSet(); // all "on" effects
		IAssSet onToOffs = new LinkedMatrixAssSet(); // all "off" effects
		// these 2 collections are used only by doBinaryChains, we create them
		// once to avert the repeated (per cell/value) creation overheads.
		// the "on"  effects of pOff // observed 63
		IAssSet offToOns = new FunkyAssSet(128, 1F, true);
		// the "off" effects of pOff // observed 178
		IAssSet offToOffs = new FunkyAssSet(256, 1F, false);
		// these 6 collections are only used by doRegionsChains
		// the "on"  effects of sibling cell being value
		IAssSet sibOns = new LinkedMatrixAssSet();
		// the "off" effects of sibling cell being value
		IAssSet sibOffs = new LinkedMatrixAssSet();
		// the "on"  effects in this region // observed 62
		IAssSet rgnOns = new FunkyAssSet(128, 1F, true);
		// the "off" effects in this region // observed 183
		IAssSet rgnOffs = new FunkyAssSet(256, 1F, false);
		// the "on"  effects of each position of value in this region
		IAssSet[] posOns = new IAssSet[REGION_SIZE];
		// the "off" effects of each position of value in this region
		IAssSet[] posOffs = new IAssSet[REGION_SIZE];
		// Set enforces uniqueness. Used as a queue. Hammers: add, poll, clear.
		// An IMyPollSet<Ass> has no IAssSet.getAss because effects are both
		// "On" and "Off", so getAss won't work.
		// the effects of anOn/Off from find
		IMyPollSet<Ass> effects = new LinkedMatrixAssSet();

		// these variables are only used by this method.
		// the "on"  effects of each potential value of cell
		IAssSet[] valuesOns = new IAssSet[VALUE_CEILING];
		// the "off" effects of each potential value of cell
		IAssSet[] valuesOffs = new IAssSet[VALUE_CEILING];
		// the "on" and "off" effects of this cell
		IMySet<Ass> cellOns, cellOffs;
		int card; // cardinality: count of set (1) bits in cell.maybes
		boolean doBinaryReductions; // set later to doReduction && card>2
		boolean doCellReductions; // set later when we know the card

		// always skip naked singles, and MultipleChain also skips bivs
		final int floor; if(isDynamic) floor=1; else floor=2;
		// foreach empty cell (except naked/hidden singles)
		for ( Cell cell : grid.cells ) {
			// cell.skip is true when cell is an unapplied naked/hidden single
			if ( cell.size>floor && !cell.skip ) {
				card = cell.size;
				doBinaryReductions = doReduction && card>2; // Why the card>2?
				doCellReductions = !isNishio && (card==2 || (isMultiple && card>2));
				cellOns = cellOffs = null;
				// foreach INITIAL maybe of this cell (there must be 2+).
				// NOTE: VALUESES[cell.maybes] is set BEFORE we iterate,
				// so doChains "erase" does NOT effect the iteration,
				// and hence we iterate each INITIAL maybe of this cell,
				// as required for algorithmic correctness!
				for ( int v : VALUESES[cell.maybes] ) {
					// do binary chaining: contradictions and reductions
					// contradition: the initialAss causes a cell to be both on
					// and off, which is absurd, so the initialAss is wrong.
					// reduction: regardless of whether the initialAss is right
					// or wrong a cell is/not a value, so it must/not be value.
					try {
						doBinaryChains(
							  new Ass(cell, v, true)
							, onToOns, onToOffs
							, offToOns, offToOffs
							// ie isDynamic (nb isNishio implies isDynamic)
							, doBinaryContradictions
							// ie isDynamic && !isNishio && card>2
							, doBinaryReductions
							, effects, hints
						);
					} catch(UnsolvableException ex) { // from cell.canNotBe(v)
						// looks like this is never used. never say never.
						effects.clear(); // clean-up for next use
					}
					// do region chaining: all possible positions of v in this
					// region have the same effect.
					if ( !isNishio )
						try {
							doRegionsChains(
								  cell, v
								, onToOns, onToOffs
								, rgnOns, rgnOffs
								, posOns, posOffs
								, sibOns, sibOffs
								, effects, hints
							);
						} catch(UnsolvableException ex) {
							// from cell.canNotBe(v).
							// never used. never say never.
							MyArrays.clear(posOns);
							MyArrays.clear(posOffs);
							sibOns.clear();
							sibOffs.clear();
							effects.clear();
						}
					// collect the results for later cell reduction
					if ( doCellReductions ) {
						// observed 57
						valuesOns[v]  = new FunkyAssSet( 64, 1F, T, onToOns , F);
						// observed 160
						valuesOffs[v] = new FunkyAssSet(256, 1F, F, onToOffs, F);
						if ( cellOns == null ) { // first pass through the loop
							// clears onToOns  // observed 62
							cellOns  = new FunkyAssSet( 64, 1F, T, onToOns , T);
							// clears onToOffs // observed 177
							cellOffs = new FunkyAssSet(256, 1F, F, onToOffs, T);
						} else { // each subsequent pass through the loop
							assert cellOffs != null; // avert IDE warning
							// clears onToOns
							cellOns.retainAllClear(onToOns);
							// clears onToOffs
							cellOffs.retainAllClear(onToOffs);
						}
					} else {
						// faster to clear a LinkedMatrixAssSet than create a
						// new FunkyAssSet (a HashMap) for each cell value.
						onToOns.clear();
						onToOffs.clear();
					}
					assert onToOns.isEmpty() && onToOffs.isEmpty();
				} // next potential value
				// do cell reduction (still producing spam NestedPlus hints)
				if ( doCellReductions ) {
					if ( cellOns.size() > 0 )
						for ( Ass a : cellOns )
							hints.add(createCellReductionHint(cell, a, valuesOns, 0));
					if ( cellOffs.size() > 0 )
						for ( Ass a : cellOffs )
							hints.add(createCellReductionHint(cell, a, valuesOffs, 1));
					MyArrays.clear(valuesOns);
					MyArrays.clear(valuesOffs);
				}
				interrupt();
			}
		} // next cell
	}

	/**
	 * Do Binary (two state: ie on & off) Chaining.
	 *
	 * Given an initial assumption that {@code Cell cell} is {@code int value},
	 * find the binary chains consequent to both states ("on" and "off") such
	 * that:
	 * <ul>
	 *  <li>{@code anOn = new Ass(cell, value, true)} and
	 *  <li>{@code anOff = new Ass(cell, value, false)}
	 * </ul>
	 * to create the following sets:
	 * <ul>
	 *  <li><b>{@code onToOn}</b> the set of assumptions that must be
	 *   "on" when {@code anOn} is "on"
	 *  <li><b>{@code onToOff}</b> the set of assumptions that must be
	 *   "off" when {@code anOn} is "on"
	 *  <li><b>{@code offToOn}</b> the set of assumptions that must be
	 *   "on" when {@code anOn} is "off"
	 *  <li><b>{@code offToOff}</b> the set of assumptions that must be
	 *   "off" when {@code anOn} is "off"
	 * </ul>
	 * Then the following Sudoku solving techniques are applied:
	 * <ul>
	 *  <li><b>Contradictions</b>
	 *  <ul>
	 *   <li>If an assumption is in both {@code onToOn} and {@code onToOff}
	 *    then the assumption {@code anOn} cannot be "on" because that implies
	 *    that an assumption is both "on" and "off", which is absurd.
	 *   <li>If an assumption is in both {@code offToOn} and {@code offToOff}
	 *    then the assumption {@code anOn} cannot be "off" because that implies
	 *    that an assumption is both "on" and "off", which is absurd.<br>
	 *    NOTE: these are much rarer but set the cells value directly.
	 *  </ul>
	 *  <li><b>Reductions</b>
	 *  <ul>
	 *   <li>If an assumption is in both {@code onToOff} and {@code offToOff}
	 *    then this assumption must be "off", because it is implied to be "off"
	 *    by both possible states of {@code anOn}.
	 *   <li>If an assumption is in both {@code onToOn} and {@code offToOn}
	 *    then this assumption must be "on", because it is implied to be "on"
	 *    by both possible states of {@code anOn}.<br>
	 *    NOTE: these are much rarer but set the cells value directly.
	 *  </ul>
	 *  <li>NB: if an assumption belongs to all the four sets then the
	 *   Sudoku has no solution. This is not checked. Not my problem.
	 *  <li>NB: Circular Chains (hypothesis implying its negation)
	 *   are already covered by Cell Chains, so are not checked for.
	 * </ul>
	 * <p>doBinaryChains is called by getMultipleOrDynamicChains for each maybe
	 * of each unset cell, which means quite often: 254,314 times in top1465 so
	 * the performance of code in ANY loops here-under really really matters.
	 *
	 * @param anOn the initial Assumption (a presumed Cell value) to gather
	 *  hints from. This is ALWAYS an "on" assumption, never an "off".
	 * @param onToOns an empty set, to which we add the "on" effects of anOn.
	 * @param onToOffs an empty set, to which we add the "off" effects of anOn.
	 * @param offToOns an empty set, to which we add the "on" effects of anOff.
	 * @param offToOffs an empty set, to which we add the "off" effects of
	 *  anOff.
	 * @param doBinaryContradictions Should Contradiction hints be reported?
	 * Note that they're always detected so that we can correctly discontinue
	 * the search, they're just not reported when doContradiction is false.
	 * @param doReduction Should we search for Reduction hints?
	 * @param effects the effects of anOn/Off from find, for internal used.
	 * @param hints the hints list to populate.
	 */
	private void doBinaryChains(
		  Ass anOn
		, IAssSet onToOns, IAssSet onToOffs
		, IAssSet offToOns, IAssSet offToOffs
		, boolean doBinaryContradictions // ie isDynamic (isNishio implies isDynamic)
		, boolean doBinaryReductions // ie isDynamic && !isNishio && card>2
		, IMyPollSet<Ass> effects
		, HintCache hints
	) {
		assert anOn.isOn; // ALWAYS an "on" assumption, never an "off"

		// Test anOn="on", ie let us assume that anOn.cell IS anOn.value,
		// to seek two contradictory effects of anOn, else null
		Ass[] contras = doChains(anOn, onToOns, onToOffs, effects, hints);
		if ( doBinaryContradictions && contras!=null ) // Nishio
			// anOn is wrong because it causes this contradiction
			// dstOn, dstOff, source, target, weAreOn, isContradiction, typeID
			hints.add(createBinaryChainHint(contras[0], contras[1]
				, anOn, anOn, false, true, 0));


		// Test anOn="off", ie let usassume that anOff.cell IS NOT anOff.value,
		// to seek two contradictory effects of anOff, else null
		offToOns.clear(); // apparently not required but it's here for safety.
		Ass anOff = anOn.flip(); // a new Ass with isOn negated.
		contras = doChains(anOff, offToOns, offToOffs, effects, hints);
		if ( doBinaryContradictions && contras!=null ) // Nishio
			// anOff is wrong because it causes this contradiction
			// dstOn, dstOff, source, target, weAreOn, isContradiction, typeID
			hints.add(createBinaryChainHint(contras[0], contras[1]
				, anOff, anOff, true, true, 1));

		// doBinaryReductions is isDynamic && !isNishio && card>2
		if ( doBinaryReductions ) {
			// Reduce means find any matches in the consequences of initialAss
			// being On (the value) and Off (not the value).
			// (a) Look for any consequences that are "on" in both cases
			reduce(anOn , onToOns , offToOns , true , 2, hints);
			// (b) Look for any consequences that are "off" in both cases
			reduce(anOff, onToOffs, offToOffs, false, 4, hints);
		}
	}

	/**
	 * Raise a BinaryChainHint for any matches in the two given Sets of Asses.
	 * <p>So if anOn is correct then A1 is 7, and if it's incorrect then A1 is
	 * also 7, so we'd just better set A1 to 7, hadn't we?
	 * <p>Or (far more likely) if A1 is 7 then I2 can't be 3, and if A1 is not
	 * 7 then I2 still can't be 3, so either way I2 can't be bloody 3. So Shoot
	 * The Dang Varmint!
	 * <p>And yes I DO have a thing for twins. Sigh.
	 *
	 * @param source the initialAss
	 * @param aSet expect onTo<b>Ons</b>  or offTo<b>Ons</b> coz we're the Ons!
	 * @param bSet expect onTo<b>Offs</b> or offTo<b>Offs</b> coz we're the Offs!
	 * @param weAreOn <b>true</b> when processing the To<b>Ons</b> lists<br>
	 *  and <b>false</b> for the To<b>Offs</b>. This effects the calculation of
	 *  the removable (red) potential values in any resulting hints:<br>
	 *  <b>true</b>: {@code redVals=target.cell.maybes.minus(target.value)}<br>
	 *  <b>false</b>: {@code redVals=target.value}
	 * @param typeID tells programmer where this hint comes from. Currently the
	 *  user sees these, because I'm the only user. REMOVE before release. To
	 *  non-techies it's just useless incomprehensible noise.
	 *  @maybe an enum to produce a meaningful 4 char code, or something?
	 * @param hints the hints list to add any {@code BinaryChainHint}s to.
	 */
	private void reduce(
		  Ass source
		, IAssSet aSet, IAssSet bSet
		, boolean weAreOn
		, int typeID
		, HintCache hints
	) {
		// NB: for efficiency we iterate the smaller of the two sets. You can
		//     tell which path we traversed by the hints typeID: Note the + 1
		//     in the downstairs dunny. Piss poor humour that. Just shoot me.
		final int aSz=aSet.size(), bSz=bSet.size();
		if ( aSz < bSz ) {	// it appears there's always more offs than ons!
			Ass b;			// So it always goes this way, apparently.
			for ( Ass a : aSet )
				if ( (b=bSet.get(a)) != null )
					//dstOn, dstOff, source, target, weAreOn, isContradiction, typeID
					hints.add(createBinaryChainHint(a, b, source, a, weAreOn
							, false, typeID));
		} else if ( bSz>0 && aSz>0 ) {
			Ass a;
			for ( Ass b : bSet )
				if ( (a=aSet.get(b)) != null )
					//dstOn, dstOff, source, target, weAreOn, isContradiction, typeID
					hints.add(createBinaryChainHint(a, b, source, a, weAreOn
							, false, typeID+1));
		}
	}

	/**
	 * Do Region Chaining: A region chain occurs when all possible positions
	 * of a value have the same consequence.
	 * <p>EG: No matter where we put the 5 in box 4 then F6 cannot be 2.
	 * <p>444 calls/puzzle for first 10 in top1465.d2, ie not too many, however
	 * this is still the slowest/heaviest method in the solve process.
	 * <p>The only reason the 2 "sib" Sets (and effects) are passed-in is
	 * efficiency: We avoid creating 2 {@code LinkedMatrixAssSet}s (a slow
	 * process) for each potential value of each Cell (mathematically max 1,152
	 * Sets per call to getMultipleDynamicChains). Instead, we create the 2
	 * Sets ONCE and clear them as required, which is a bit faster. They are
	 * not intended for use outside of this method.
	 * @param cell the Cell of the initial "on" assumption (anOn).
	 * @param v the value of the initial "on" assumption (anOn).
	 * @param onToOns the "On" consequences of anOn. I both populate and use
	 *  this Set.
	 * @param onToOffs the "Off" consequences of anOn. I both populate and
	 *  use this Set.
	 * @param rgnOns regionToOns: the Set of "On"s in this region.
	 * @param rgnOffs regionToOffs: the Set of "Off"s in this region.
	 * @param posOns positionToOns: an array of "On" Ass-Sets indexed by the
	 *  positions of 'value' in this region [0..8].
	 * @param posOffs positionToOffs: an array of "off" Ass-Sets indexed by
	 *  the positions in this region [0..8].
	 * @param sibOns siblingToOns: "On" effects of sibling being value.
	 *  <br>Not for external reference. see comments above.
	 * @param sibOffs siblingToOffs: "Off" effects of sibling being value.
	 *  <br>Not for external reference. see comments above.
	 * @param effects An {@code IMyPollSet<Ass>} populated by onToOffs and
	 *  offToOns.
	 *  <br>Not for external reference. see comments above.
	 * @param hints The HintCache to which I add any RegionReductionHint's.
	 */
	private void doRegionsChains(
		  Cell cell, int v
		, IAssSet onToOns, IAssSet onToOffs	  // on => on/off
		, IAssSet rgnOns, IAssSet rgnOffs	  // region => on/off
		, IAssSet[] posOns, IAssSet[] posOffs // position => on/off
		, IAssSet sibOns, IAssSet sibOffs	  // siblings => on/off
		, IMyPollSet<Ass> effects	// populated by onToOffs & offToOns.
		, HintCache hints
	) {
		int[] riv; // indexes of value in this region
		int n; // n is the number of possible positions for value in region
		int i; // i is the current possible position for value in region
		int j; // j is the current index into the riv array, ie i=riv[j].
		Ass a; // the current Assumption
		boolean noOns, noOffs; // is rgnOns/Offs empty?
		for ( ARegion r : cell.regions ) { // Box, Row, Col of this cell
			// Each region is examined ONCE, so is r worth looking at?
			// does r have 2 (or-more if isMultiple) positions for the value of
			// the initial assumption?
			// WARN: we need a copy of idxsOf[v] (ie riv) because the doChains
			// method erases them when isDynamic. We also want an array (riv)
			// instead of a bitset, so that works out rather nicely.
			if ( !( (n=(riv=INDEXES[r.ridx[v].bits]).length) == 2
				 || (isMultiple && n>2) )
			  // are we seeing this region for the first time?
			  || r.cells[i=riv[0]] != cell ) // reference equals OK
				continue;
			// for the first possible position of value in this region (i)
			rgnOns.addAll(posOns[i] = onToOns);
			rgnOffs.addAll(posOffs[i] = onToOffs);
			// foreach subsequent possible position of value within this region
			noOns = noOffs = false; // early exit when they're both true
			// foreach subsequent possible position of value in this region
			for ( j=1; j<n; ) {
				assert sibOns.isEmpty() && sibOffs.isEmpty();
				// get the effects of cell being value in sibToOns, sibToOffs
				// nb: effects is passed down just to save time on set creation
				doChains(new Ass(r.cells[i=riv[j++]], v, T), sibOns, sibOffs, effects, hints);
				if ( noOns || (noOns=rgnOns.retainAllIsEmpty(sibOns)) )
					sibOns.clear();
				else
					posOns[i] = new FunkyAssSet(64, 1F, T, sibOns, T); // clears sibOns // observed 60
				if ( noOffs || (noOffs=rgnOffs.retainAllIsEmpty(sibOffs)) ) {
					sibOffs.clear();
					if ( noOns ) // noOffs && noOns, so search failed
						break; // this only seems to happen on the last i. Sigh.
				} else
					posOffs[i] = new FunkyAssSet(256, 1F, F, sibOffs, T); // clears sibOffs // observed 140
			} // next subsequent position
			// turn any surviving region "On"s into hints.
			if(!noOns) while( (a=rgnOns.poll()) != null )
				hints.add(createRegionReductionHint(r, v, a, posOns));
			// turn any surviving region "Off"s into hints.
			if(!noOffs) while( (a=rgnOffs.poll()) != null )
				hints.add(createRegionReductionHint(r, v, a, posOffs));
			// clear the position arrays // quicker than overloading the GC
			MyArrays.clear(posOns);
			MyArrays.clear(posOffs);
		} // next cell.region
	}

	/**
	 * Find the effects of the initial assumption in the grid.
	 * <p>
	 * Firstly "Chaining" is just following the chain of consequences from
	 * an assumption. Sadly, it has nothing to do with the bed room.
	 * <p>
	 * doChains is pretty complex, so here's a <b>brief explanation</b> of
	 * what's going on. We're given the initial assumption that a cell is (or
	 * is not) a value, and a couple of sets of assumptions, the "Ons" and the
	 * "Offs": we use these 2 sets to keep track of what we've already seen, to
	 * avoid going into an endless loop of chain steps (a tangle).
	 * <p>
	 * Let's start with an <b>"On"</b> (it really doesn't matter): so let's
	 * assume that A1 is 7, we calculate the direct "Off" consequences in the
	 * grid of A1 being 7 (using onToOffs) and for each of those "Off"s we
	 * check if we've already seen the conjugate (ie the opposite assumption)
	 * and if so then we return the contradiction; if not then we continue to
	 * check each "Off" against our set-of-known-Offs (to avoid going into an
	 * endless loop) before adding it to our offs-to-do-list; and that's about
	 * it for an "On". Simple really.
	 * <p>
	 * To process an <b>"Off"</b> from our offs-to-do-list we first calculate
	 * any "Ons" which are a direct consequence of the "Off" (using offToOns)
	 * In dynamic mode we then remove the "off" from the grid, so that we can
	 * come back later and play "Who's your daddy?" with our Ass's, and (when
	 * degree > 0) so that the Four Quick Foxes (just "standard" hinters) can
	 * "see" our assumptions. We then process each of the new "Ons": first we
	 * check if we've already seen the-"Ons"-conjugate and if so we return the
	 * contradiction; if not then we check if this "On" is in our set-of-known-
	 * Ons (to avoid going into an endless loop) before adding it to our ons-to
	 * -do-list. And that's it for an "Off".
	 * <p>
	 * Finally if degree > 0 and there is no next step in the chain then we run
	 * our Four Quick Foxes (and possibly an additional Chainer or two) to find
	 * the next "Off" link in the chain (using <b>getHintersEffects</b>).
	 * And that's about it. Endless hours of family entertainment, playing hide
	 * the hint with our Ass's on the line.
	 * <p>
	 * doChains is called by doBinaryChains (twice) and by doRegionsChains,
	 * which are both called only by the getMultipleOrDynamicChains. Ergo this
	 * method isn't called under the "vanilla" unary-chains-and-cycles branch.
	 * <p>
	 * doChains is called 942,316 times in top1465 SPEED, so performance is
	 * quite important for EVERYthing in this whole class.
	 * <p>
	 * <b>uses:</b> initGrid especially when isDynamic, when the grid is
	 * modified (by "erasing" the off assumptions) and then always restored to
	 * it's initial state (ie initGrid) before exiting. This is a field to save
	 * on creating a new Grid (which is expensive) for each cell value. Instead
	 * we just copyTo an existing grid, which is still expensive, but a lot
	 * less expensive. When !isDynamic the grid is copied to initGrid ONCE and
	 * is NOT modified.
	 * <p>
	 * Q: This method seems to NEED both its queues: it was easy to remove
	 * the dual queues from doCycle and doUnary, and I've tried 4 times to do
	 * the same here, and each time I fail my test-case, and I'm stumped!
	 * A: Got it! In dynamic mode we need to onToOff as soon as the "On" is
	 * discovered, so that it's causal maybes aren't erased beforehand.
	 * <p>
	 * Q: Why does onToOff only eat fresh meat? Why can't it digest erasures?
	 * A: As above, onToOff NEEDs the causal maybes.
	 *
	 * @param anAss the initial assumption may be an "On" or an "Off".
	 * @param toOns Output: the "On" consequences of initialAss.
	 * @param toOffs Output: the "Off" consequences of initialAss.
	 * @param effects An empty {@code IMyPollSet<Ass>} used internally. Not for
	 * external reference. It's passed just for efficiency, coz it's quicker to
	 * create one queue-set, and re-use it, than to ad-hoc create everywhere.
	 * @return the first contradiction (if any): an assumption which is found
	 * to be both "on" and "off", which is absurd; else null meaning none.
	 */
	private Ass[] doChains(
		  Ass anAss					// input: the initial Assumption
		, IAssSet toOns				// output LinkedMatrixAssSet // IAssSet for getAss and contains(Ass), funky: add retains existing.
		, IAssSet toOffs			// output LinkedMatrixAssSet // IAssSet for getAss and contains(Ass), funky: add retains existing.
		, IMyPollSet<Ass> effects	// working LinkedMatrixAssSet // IMyPollSet has poll, but no getAss.
		, HintCache hints			// results // HintsListSorted or HintsListStraight
	) {
		assert effects.isEmpty();
		assert dchOnQ.size == 0;
		assert dchOffQ.size == 0;
		// nb: new LinkedMatrixAssSet takes a while so we use two fields; and
		// to create stack-references for speed, coz it takes longer to resolve
		// a heap-reference than a stack-reference (visibility rules I guess).
		final LinkedMatrixAssSet onQ=dchOnQ, offQ=dchOffQ;
		if ( anAss.isOn ) {
			toOns.clear();
			toOns.add(anAss);
			onQ.add(anAss);
			toOffs.clear();
		} else {
			toOns.clear();
			toOffs.clear();
			toOffs.add(anAss);
			offQ.add(anAss);
		}

//		//C:\Users\User\Documents\SudokuPuzzles\Test\DynamicPlusTest_MiaRegionReductionChain.txt
//		//MIA: DynamicPlusTest: Region Reduction Chain: 5 in box 2 so C5+9
//		if ( WATCH_GRID
//		  && diuf.sudoku.Run.type == diuf.sudoku.Run.Type.GUI // only in the GUI!
//		  && tech == Tech.DynamicPlus // only in DynamicPlus
//		  && anAss.value == 5 // 5
//		  && anAss.cell.box.equals(grid.boxs[1]) // in box 2
//		  && Debug.isMethodNameInTheCallStack(3, "doBinaryChains") // not doRegionChains
//		) {
//			if ( df == null ) // first-time only
//				df = diuf.sudoku.gui.SudokuExplainer.getInstance().frame;
//			df.setCurrentHint(new GWH(anAss, toOns, toOffs, null, null, anAss), false);
//			Debug.breakpoint();
//		}

		//assert toOns.size() + toOffs.size() == 1;
		// Y-Chain means other potential values of this cell get "off"ed.
		final boolean isYChain = !isNishio; // Seek naked singles = !Nishio
		// if (isDynamic) then backup before maybes are erased from the grid,
		// else the grid is copied ONCE at top of stack to save time.
		if ( isDynamic )
			theInitialGrid.copyFrom(grid);
		try {
			Ass a, e, c; // an assumption, it's effect, and effects conjugate.
			boolean anyOns; // did offToOns find any Ons? A performance tweak.
			// process ALL On's before touching an Off! coz onToOffs needs ALL
			// the causal maybes of this "On" to be in tact!
			while ( (a=onQ.poll())!=null || offQ.size>0 ) {
				if ( a != null ) { // a is an "On"
					// nb: there's always atleast one "Off" effect of an "On"!
//					if ( df != null ) {
// 						df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, a), false);
//						Debug.breakpoint();
//					}
					onToOffs(a, isYChain, effects);
					// foreach e: an "Off" effect of the "On" a
					while ( (e=effects.poll()) != null )
						// if e's opposite exists there's a contradiction.
						// note that this relies on all toOns being "On".
						if ( (c=toOns.getAss(e.cell, e.value)) != null ) {
							// Contradiction found!
							effects.clear(); // for next time
//							if ( df != null )
//								df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, c, e), false);
							return new Ass[]{c, e}; // Can't be both on&off
						} else if ( toOffs.add(e) ) { // Not processed yet
							// queue the offs until we've done ALL the Ons, so
							// grid remains consistent with that which caused
							// these Ons; then we can safely erase the Offs
							// when isDynamic mode is on. That's why we need
							// two queues: one for Offs and another for Ons.
							offQ.add(e);
//							if (df != null )
//								df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, a), false);
						}
					// wend
				} else { // a is an "Off", so e (if any) is an "On"
					// get the Off, and find any subsequent On/s
					// nb: remove throws NoSuchElementException if offQ isEmpty
					anyOns = offToOns(a=offQ.remove(), toOffs, true
									  , isYChain, effects); // USES: initGrid
//					if ( df != null ) {
//						df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, a), false);
//						Debug.breakpoint();
//					}
					// WARNING: Do offToOns BEFORE erase, or we miss them all!
					if ( isDynamic )
						a.erase(); // a.cell.canNotBe(a.value) in the grid
//					if ( df != null )
//						df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, a), false);
					if ( anyOns )
						// foreach e: an "On" effect of the "Off" a
						while ( (e=effects.poll()) != null )
							// if get e's conjugate (with parents) is not null
							// note that this relies on all toOffs being "Off"
							// note that toOffs
							if ( (c=toOffs.getAss(e.cell, e.value)) != null ) {
								// Contradiction found!
								effects.clear(); // for next time
//								if ( df != null )
//									df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, e, c), false);
								return new Ass[]{e, c}; // Can't be both on&off
							} else if ( toOns.add(e) ) { // Not processed yet
								onQ.add(e); // to be processed asap!
//								if ( df != null )
//									df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, a), false);
							}
						// wend
					// fi
				} // fi
				// if degree>0 and there is no other next step in the chain
				if ( degree>0 && onQ.size==0 && offQ.size==0 )
					// search for advanced chain steps with my imbedded hinters
					for ( Ass h : getHintersEffects(toOffs) )
						if ( toOffs.add(h) ) // Not processed yet
							offQ.add(h);
				// fi
			} // wend: next assumption from onQ, if any, else offQ
			if ( turbo && isValidSolution() )
				hints.add(new TurboHint(this, anAss, setPots()));
//			if ( df != null ) {
//				df.setCurrentHint(new GWH(anAss, toOns, toOffs, onQ, offQ, (Ass[])null), false);
//				Debug.breakpoint();
//			}
		} finally {
			if ( isDynamic ) // NB: grid is not modified when !isDynamic
				grid.copyFrom(theInitialGrid);
			onQ.clear();
			offQ.clear();
//			df = null;
		}
		return null;
	}
	private final LinkedMatrixAssSet dchOnQ  = new LinkedMatrixAssSet();
	private final LinkedMatrixAssSet dchOffQ = new LinkedMatrixAssSet();

	/**
	 * Get the effects (a List of Ass's) of the Four Quick Foxes and any
	 * embedded Chainers in the current grid, which IS modified because
	 * degree > 0 implies isDynamic.
	 */
	private List<Ass> getHintersEffects(IAssSet parentOffs) {
		assert degree>0 && isDynamic;
		// NB: LinkedList is faster than ArrayList when we don't know max-size.
		final List<Ass> effects = new LinkedList<>();
		// accu adds each resulting Assumption to the effects list
		final IAccumulator hacu
				= new ChainerHacu(theInitialGrid, grid, parentOffs, effects);
		for ( IHinter hinter : hinters )
			if ( hinter.findHints(grid, hacu) ) {
//				if ( df != null ) {
//					df.setCurrentHint(hacu.getHint(), false);
//					Debug.breakpoint();
//				}
				break;
			}
		return effects;
	}

	/**
	 * Is the grid full and a valid solution?
	 *
	 * @param grid a grid
	 * @return true if the grid is solved, and false there-after, so that
	 *  the GUI doesn't end-up with a list of 40 cells all of which solve
	 *  a ludicrously simple Sudoku that happens to contain one chain, and
	 *  is therefore not ludicrously simple. sigh.
	 */
	private boolean isValidSolution() {
		if ( iToldHimWeveAlreadyGotOne )
			return false;
		//  is the grid full: every cell has a value or 1 maybe
		for ( Cell cell : grid.cells )
			if ( cell.value==0 && cell.size>1 )
				return false;
		// validate: 9 bits in 9 cells in each region
		int vs;
		for ( ARegion r : grid.regions ) {
			vs = 0;
			for ( Cell cell : r.cells ) {
				assert (cell.value==0 && VSIZE[cell.maybes]==1)
					|| (cell.value!=0 && cell.maybes==0);
				vs |= VSHFT[cell.value] | cell.maybes;
			}
			if ( vs != VALL )
				return false;
		}
		return iToldHimWeveAlreadyGotOne = true;
	}
	private boolean iToldHimWeveAlreadyGotOne;

	/**
	 * Pass me a solved grid and I return a nice full setPots.
	 *
	 * @param grid a nice accidentally solved grid.
	 * @return a nice big setPots
	 */
	private Pots setPots() {
		final Pots result = new Pots(128, 1F);
		for ( Cell cell : grid.cells )
			if ( cell.value==0 && cell.size==1 )
				result.put(cell, cell.maybes);
		return result;
	}

	private BinaryChainHint createBinaryChainHint(
		  Ass dstOn, Ass dstOff
		, Ass source, Ass target
		, boolean weAreOn
		, boolean isContradiction
		, int typeID
	) {
		final Pots redPots = weAreOn ? createRedPots(target)
				: new Pots(target.cell, target.value);
		if ( redPots == null )
			return null;
		String tid; if(WANT_TYPE_ID) tid=" ("+typeID+")"; else tid=EMPTY_STRING;
		return new BinaryChainHint(this, redPots, source, dstOn, dstOff
				, isNishio, isContradiction, tid);
	}

	private CellReductionHint createCellReductionHint(
		  Cell srcCell
		, Ass target
		, IAssSet[] valuesEffects
		, int typeID
	) {
		// check that source.cell is this cell (when I'm a Nested*) to suppress
		// funky hints when embedded DynamicChain+ finds a RAW grid hint.
		if ( tech.isNested && getSource(target) == null ) // target.parents.first.first...
			return null;  // this can't be a CellReductionHint
		// Build removable potentials
		final Pots redPots = createRedPots(target);
		if(redPots==null) return null;
		// Build chains
		assert srcCell.size > 0;
		LinkedHashMap<Integer, Ass> chains =
				new LinkedHashMap<>(srcCell.size, 0.75f);
		for ( int v : VALUESES[srcCell.maybes] ) { // iterator fast enough here.
			IAssSet vEffects = valuesEffects[v];
			if ( vEffects==null || vEffects.isEmpty() ) { // shouldn't exist
				assert false : "valueEffects["+v+"] is "
					+(vEffects==null?NULL_ST:"empty")+"\n"
					+" cell="+srcCell.toFullString()
					+" target="+target
					+" typeID="+typeID;
				return null; // WTF: this is a real WTF
			}
			// get the actual target assumption, with ancestors
			Ass a = vEffects.get(target);
			if ( a==null ) { // WTF: how did it get here in the first place.
				assert Debug.dumpValuesEffectsAncestors(valuesEffects, target) // allways returns false
					: "target not in valueEffects["+v+"]\n"
					  + " cell="+srcCell.toFullString()
					  + " target="+target
					  + " typeID="+typeID;
				return null; // WTF: something is fubared
			}
			chains.put(v, a);
		} // next v
		if(chains.isEmpty()) return null; // should never happen AFAIK
		// Build & return the hint
		String tid; if(WANT_TYPE_ID) tid=" ("+typeID+")"; else tid=EMPTY_STRING;
		return new CellReductionHint(this, redPots, srcCell, chains, tid);
	}

	private RegionReductionHint createRegionReductionHint(
		  ARegion region
		, int value
		, Ass target
		, IMySet<Ass>[] positionEffects
	) {
		final int typeID; if(target.isOn) typeID=0; else typeID=1;
		// build removable potentials
		final Pots reds;
		if ( target.isOn ) {
			if ( target.cell.maybes == VSHFT[target.value] )
				return null; // naked singles are not my problem
			reds = new Pots(target.cell, target.cell.maybes & ~VSHFT[target.value], false);
		} else
			reds = new Pots(target.cell, target.value);
		// build chains
		LinkedHashMap<Integer, Ass> chains = null;
		IMySet<Ass> effects; // the effects of r.cells[i] being v
		Ass targetWithParents; // this Ass has parents (target is just a dolly)
		for ( int i : region.ridx[value] ) // iterator is fast enough
			// add the complete parent assumption (with ancestors) to the chain
			if ( (effects=positionEffects[i]) != null
			  && (targetWithParents=effects.get(target)) != null ) {
				if(chains==null) chains=new LinkedHashMap<>();
				chains.put(i, targetWithParents);
			}
		if(chains==null) return null;
		String tid; if(WANT_TYPE_ID) tid=" ("+typeID+")"; else tid=EMPTY_STRING;
		return new RegionReductionHint(this, reds, region, value, chains, tid);
	}

	/**
	 * This method is called by supers offToOns method (only when isDynamic)
	 * to add any hidden parent (causal) assumptions to the 'effect' Ass's
	 * parents-list; so we're building a back-chained tree where each node
	 * (Ass) knows it's parents (not it's children).
	 * <p>
	 * A "hidden parent" is a preceeding assumption which must be true in
	 * order for this assumption to become applicable.
	 */
	@Override
	protected void addHiddenParentsOfCell(
		  Ass effect
		, Cell cell
		, IAssSet prntOffs
	) {
		// we only "erase" assumptions when isDynamic, so that we can
		// comeback later and play "Who's your daddy".
		// called 9,102,331 times in top1465
		final int initBits = theInitialGrid.maybes[cell.i];
		final int currBits = grid.maybes[cell.i];
		if ( currBits != initBits ) {
			// foreach value of cell that has been removed
			Ass p; // parent
			for ( int v : VALUESES[initBits & ~currBits] )
				if ( (p=prntOffs.getAss(cell,v)) != null )
					// nb: we call the addParent method despite it being slower
					// because it is required to create a missing parents list
					effect.parents.linkLast(p);
		}
	}

	/**
	 * This method is called by supers offToOns method (only when isDynamic)
	 * to add any hidden parent (causal) assumptions within this region to
	 * the effects parents list.
	 * <p>
	 * A "hidden parent" is a preceeding assumption which must be true in
	 * order for this assumption to become applicable.
	 */
	@Override
	protected void addHiddenParentsOfRegion(int oci, int rti, int v
			, int currPlaces, ARegion region, IAssSet rents, Ass effect) {
		// get the erased places of value in region
		// ie in the initialGrid andNot in the currentGrid.
		final int erasedPlaces = theInitialGrid.cells[oci].regions[rti].ridx[v].bits & ~currPlaces;
		if ( erasedPlaces != 0 ) {
			Ass p; // parent
			// foreach possible position of v in the region that has been erased
			for ( int i : INDEXES[erasedPlaces] )
				// nb: parentOffs is a LinkedMatrixAssSet with the strange getAss
				// method (java.util.Set has no get) defined by IAssSet to fetch
				// the Ass from the Set at this cell with this value; whether or
				// not the Ass is an ON does not matter here coz all asses in an
				// IAssSet must either be all ONs or all OFFs.
				if ( (p=rents.getAss(region.cells[i],v)) != null )
					effect.parents.linkLast(p);
		}
	}

////RETAIN for debugging: GridWatchHint is called GWH for brevity
//	private static class GWH extends diuf.sudoku.solver.AHint {
//
//		private static final diuf.sudoku.solver.hinters.AHinter DUMMY_HINTER
//				= new diuf.sudoku.solver.hinters.DummyHinter();
//		private final Ass anAss;
//		private final IAssSet toOns;
//		private final IAssSet toOffs;
//		private final LinkedMatrixAssSet onQ;
//		private final LinkedMatrixAssSet offQ;
//		private final Ass[] asses;
//
//		public GWH(Ass anAss, IAssSet toOns, IAssSet toOffs
//				, LinkedMatrixAssSet onQ, LinkedMatrixAssSet offQ, Ass... asses) {
//			super(DUMMY_HINTER, null);
//			this.anAss = anAss;
//			this.toOns = toOns;
//			this.toOffs = toOffs;
//			this.onQ = onQ;
//			this.offQ = offQ;
//			this.asses = asses;
//		}
//
//		@Override
//		public Pots getGreens(int viewNum) {
//			Pots greens = null;
//			if ( asses == null )
//				return greens;
//			for ( Ass a : asses )
//				if ( a!=null && a.isOn ) {
//					if(greens==null) greens=new Pots();
//					greens.put(a.cell, VSHFT[a.value]);
//				}
//			return greens;
//		}
//
//		@Override
//		public Pots getReds(int viewNum) {
//			Pots reds = null;
//			if ( asses == null )
//				return reds;
//			for ( Ass a : asses )
//				if ( a!=null && !a.isOn ) {
//					if(reds==null) reds=new Pots();
//					reds.put(a.cell, VSHFT[a.value]);
//				}
//			return reds;
//		}
//
//		@Override
//		protected int applyImpl(boolean isAutosolving, Grid grid) {
//			return 0; // I'm a Do nothing deadCat
//		}
//
//		@Override
//		protected String toHtmlImpl() {
//			StringBuilder sb = new StringBuilder(1024);
//			sb.append("<html><body>\n<h2>Grid Watch</h2>\n<pre>\n")
//			  .append("anAss : ").append(anAss).append(NL)
//			  .append("toOns : ").append(toOns).append(NL)
//			  .append("toOffs: ").append(toOffs).append(NL)
//			  .append("onQ   : ").append(onQ).append(NL)
//			  .append("offQ  : ").append(offQ).append(NL);
//			if ( asses == null )
//				sb.append("ass   : none").append(NL);
//			else
//				for ( Ass a : asses )
//					if ( a == null )
//						sb.append("ass   : null").append(NL);
//					else
//						sb.append("ass   : ").append(a).append(NL);
//			return sb.toString();
//		}
//
//		@Override
//		protected String toStringImpl() {
//			return "GWH";
//		}
//	}

}
