/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Ass.OFF;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Values.SET_BIT;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.MyFunkyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedFifoQueue;
import diuf.sudoku.utils.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedList;
import java.io.PrintStream;
import java.util.HashMap;
import diuf.sudoku.solver.hinters.IFoxyHint;

/**
 * The abstract Chaining hint. All hint-types in the chain package extend this
 * AChainingHint, which provides default implementations for many methods that
 * are common to all chaining hints. This class is complicated. It was all-good
 * until we imbedded hints. Even simple-things, like countAncestors, are
 * multi-faceted. Soz!
 */
public abstract class AChainingHint extends AHint implements IFoxyHint {

	private static final PrintStream OUT = System.out;
	private static final boolean DEBUG = false;
	private static void DEBUG() {
		if(DEBUG)OUT.println();
	}
	private static void DEBUG(final String msg) {
		if(DEBUG)OUT.println(msg);
	}
	private static void DEBUG(final String frmt, final Object... args) {
		if(DEBUG)OUT.format(frmt, args);
	}
	private static boolean carp(final String msg) {
		OUT.println(msg);
		return false;
	}

	/**
	 * If true the typeId field parachutes into each "$hintName (\\d):".
	 * <p>
	 * It allows a techie to track a hint back-to source,
	 * but its incomprehensible noise to the end-user,
	 * and interferes with the sorting of hints,
	 * and breaks ALL of the chainers test-cases,
	 * So <b>MUST</b> be false when you are done ____ing around!
	 */
	protected static final boolean WANT_TYPE_ID = false; // @check false

	// The size of the set used in getCompleteHinterParents.
	// Is a field to allow me to track max size and bump-up accordingly.
	private int gpSetSize = 64;

	protected final int flatViewCount;
	protected final boolean isXChain; // Is only a single value involved?
	protected final boolean isYChain; // Is only cells with two potential values involved?
	// XY-Chain (both) Is bidirectional cycle. Ie there are exactly two ways of placing
	//                 these values in these cells.
	protected final Ass resultAss; // may be null

	HashMap<Ass, ArrayList<Ass>> ancestorsMap = new HashMap<>(16, 1F);

	private Set<Integer>[] aquaCells;

	private LinkedList<AChainingHint> chains = null;

	private int currViewNum = -1; // ie impossible
	private Pots myRedPots; // my to differentiate them from super.redPots
	private Pots greenPots;
	private Pots orangePots;

	private Ass[] targets;
	private ArrayList<Ass> chainsTargets;

	private int viewCount;
	private int complexity;

	private String hinterName;
	private StringBuilder line;
	private int htmlSize;

	private Integer difficulty;

	public AChainingHint(Grid grid, IHinter hinter, Pots redPots
			, int flatViewCount, boolean isYChain, boolean isXChain
			, Ass result) {
		super(grid, hinter, AHint.INDIRECT
			, result!=null && result.isOn ? grid.cells[result.indice] : null // cell
			, result!=null && result.isOn ? result.value : 0 // value
			, redPots
			, null, null, null, null, null
		);
		this.flatViewCount = flatViewCount;
		this.isYChain = isYChain;
		this.isXChain = isXChain;
		this.resultAss = result; // may be null
		assert isXChain || isYChain; // probably both
	}

	/**
	 * Results are Pots that are displayed in a larger font, to differentiate
	 * the effects of this hint from the rest.
	 * <p>
	 * Any On Results are differentiated by adding SET_BIT to there values.
	 * Ons inherently have one value per cell, to which the cell is set when
	 * the hint is applied. Note that Results are display-only, not setPots!
	 *
	 * @return a Pots to display in a larger font.
	 */
	@Override
	public Pots getResults() {
		final Ass r = resultAss;
		if ( r != null ) {
			if ( r.isOn )
				return new Pots(r.indice, SET_BIT ^ VSHFT[r.value], DUMMY);
			return new Pots(r.indice, r.value);
		}
		return null;
	}

	/**
	 * Get all the ancestors of this Assumption. Each Assumption may have
	 * multiple parents, which may have multiple parents, and so on.
	 * Here we just flatten the tree into a distinct BFS list, loosing the
	 * relationships.
	 * <p>
	 * NOTE that the results are cached, coz I am called twice in succession.
	 * <p>
	 * This method is package visible for use by FullChain.
	 * <p>
	 * KRC 2020-08-24 added ancestorsMap to cache the list because I am called
	 * by both getGreens and getReds, and it is wasteful to calculate the list
	 * (which can be quite long ~ upto about 100 I guess) twice.
	 * KRC 2020-09-12 RegionReductionHint and CellReductionHint both override
	 * this implementation to not cache coz, by definition, the target is the
	 * SAME target for all initial assumptions.
	 *
	 * @param target the Assumption to get ancestry of.
	 * @return an {@code ArrayList<Ass>}.
	 */
	ArrayList<Ass> ancestors(final Ass target) {
		ArrayList<Ass> list;
		if ( (list=ancestorsMap.get(target)) == null ) {
			ancestorsMap.put(target, list=ancestorsImpl(target));
		}
		return list;
	}

	/**
	 * Get all the ancestors of this Assumption, uncached. Each Assumption may
	 * have multiple parents, which may have multiple parents, and so on. So we
	 * flatten the tree into a distinct BFS list, loosing the relationships.
	 *
	 * @param target the Assumption to get ancestry of.
	 * @return an {@code ArrayList<Ass>}.
	 */
	protected ArrayList<Ass> ancestorsImpl(final Ass target) {
		final Set<Ass> distinct = new MyFunkyLinkedHashSet<>(128, 1F);
		final Queue<Ass> queue = new MyLinkedFifoQueue<>();
		for ( Ass a=target; a!=null; a=queue.poll() ) {
			if ( distinct.add(a) ) { // Funky!
				queue.addAll(a.parents);
			}
		}
		return new ArrayList<>(distinct);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Man Of Colors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//
	// KRC 2020 Aug: Getting the colors is now pretty complicated, hence this
	// design note. The oranges used to be the reds plus the greens (ie any
	// cell that was in both the redPots and the greenPots was painted orange)
	// but I seperated the oranges out to there own color after adding an
	// additional three colors to support the (HoDoKu) Coloring hint. So this
	// code is intended to "hide the complexity" from the rest of the world.
	// It presents the "new" interface of getOranges, getReds, and getGreens
	// to SudokuExplainer (when )

	/**
	 * get the Pots for a color.
	 * @param target the target assumption is the first initial Ass(umption).
	 * @param wantOn true for greenPots to get Ass(umption)s with isOn==true;
	 * or false for the redPots to get Ass(umption)s with isOn==false.
	 * @return
	 */
	protected Pots getColor(final Ass target, final boolean wantOn) {
		final ArrayList<Ass> ancestors = ancestors(target);
		// ~40% of the hints are ons, and ~60% are offs
		final double factor = wantOn ? 0.4D : 0.6D;
		// + 2.51 to round up, with wiggle room
		final int size = Math.max(Pots.MIN_CAPACITY, (int)(ancestors.size() * factor + 2.51));
		final Pots color = new Pots(size, 1F);
		ancestors.stream().filter((a)->a.isOn==wantOn).forEachOrdered((a) ->
			color.upsert(a.indice, VSHFT[a.value], DUMMY)
		);
		return color;
	}

	// has no overrides (yet) so make me final if you feel the need.
	protected Pots getFlatGreens(int viewNum) {
		return getColor(getChainTarget(viewNum), true);
	}
	private Pots getNestedGreens(int nestedViewNum) {
		assert hinter.isNesting();
		final Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null ) {
			return null;
		}
		return pair.a.getGreenPots(pair.b);
	}
	// overridden by BidirectionalCycleHint, all other subtypes use me.
	public Pots getGreensImpl(int viewNum) {
		if ( viewNum < flatViewCount ) {
			return getFlatGreens(viewNum);
		}
		return getNestedGreens(viewNum - flatViewCount);
	}

	// overriden by BinaryChainingHint, all other subtypes use me.
	protected Pots getFlatReds(int viewNum) {
		return getColor(getChainTarget(viewNum), false);
	}
	protected final Pots getNestedReds(int nestedViewNum) {
		assert hinter.isNesting();
		final Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null ) {
			return null;
		}
		return pair.a.getRedPots(pair.b);
	}
	public Pots getRedsImpl(int viewNum) {
		if ( viewNum < flatViewCount ) {
			return getFlatReds(viewNum);
		}
		return getNestedReds(viewNum - flatViewCount);
	}

	private void getColors(int viewNum) {
		if ( viewNum!=currViewNum || myRedPots==null ) {
			currViewNum = viewNum;
			myRedPots = getRedsImpl(viewNum);
			greenPots = getGreensImpl(viewNum);
			// oranges: just because I like traffic lights, but only when...
			orangePots = myRedPots.intersection(greenPots);
			myRedPots.removeAll(orangePots);
			greenPots.removeAll(orangePots);
		}
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		getColors(viewNum);
		return greenPots;
	}
	// overridden by BidirectionalCycleHint, all other subtypes use me
	@Override
	public Pots getRedPots(int viewNum) {
		getColors(viewNum);
		return myRedPots;
	}
	@Override
	public Pots getOrangePots(int viewNum) {
		getColors(viewNum);
		return orangePots;
	}

	/**
	 * The bluePots are candidates (cells potential values) that have already
	 * been removed from the grid by a previous (nested) hint. They are for
	 * display only, but are considered important coz they are necessary for the
	 * user (well, the programmer, really) to follow the logic underneath the
	 * assumption-chain, in order for them to justify the hint to themselves.
	 * This is essential during testing. One simply cannot test all possible
	 * combinations, but one can find an instance of This and verify that the
	 * constituent logic holds up, and then find an instance of That and check
	 * that it also holds-up; and thereby gain confidence that Sudoku Explainer
	 * handles This AND That. A complex logical construct cannot ever be proven
	 * correct, we can only fail to shoot-down it is constituent parts. One out,
	 * all out. UnsolvableException: You ____ed Up!
	 * <p>
	 * Sudoku Explainer only (currently) needs the bluePots for manual testing,
	 * it does not use them itself internally.
	 *
	 * @param grid
	 * @param viewNum
	 * @return
	 */
	@Override
	public Pots getBluePots(Grid grid, int viewNum) {
		if ( (viewNum -= flatViewCount) < 0 ) {
			return null;
		}
		// first create a grid from the container (or "main") chain,
		// by removing the deductions of each parent chain
		final Pair<AChainingHint, Integer> pair = getNestedChain(viewNum);
		if ( pair == null ) {
			return null; // not nesting -> no bluePots
		}
		final Grid nestedGrid = new Grid(grid);
		final AChainingHint nestedChain = pair.a;
		final int nestedViewNum = pair.b;
		final Ass target = getContainerTarget(nestedChain);
		ancestors(target).stream().filter((a)-> !a.isOn).forEachOrdered(
			(ass)-> nestedGrid.cells[ass.indice].canNotBeBits(VSHFT[ass.value])
		);
		// now use the nested chains parent collector on the nestedGrid
		final Ass nestTarget = nestedChain.getChainTarget(nestedViewNum);
		// parents of nestedTarget
		final Set<Ass> rents = nestedChain.collectHinterParents(
				grid, nestedGrid, nestTarget, new LinkedHashSet<>());
		// convert blueAsses to bluePots, a Map of Cell=>Values.
		return Pots.of(rents);
	}

	// ~~~~~~~~~~ It is my considered opinion that they are nesting ~~~~~~~~~~

	// nb: chaining hints are NOT deeply nested, typically one level only.
	private LinkedList<AChainingHint> recurseNestedChains(final LinkedList<AChainingHint> chains) {
		// casually observed max 14, guess 32 covers atleast 90% of cases
		final Set<FullChain> distinct = new MyFunkyLinkedHashSet<>(32, 0.75F);
		for ( Ass target : getChainsTargets() ) {
			for ( Ass a : ancestors(target) ) {
				if ( a.nestedChain != null
				  // add is an addOnly, it does not update existing keys.
				  // FullChain equals and hashCode compares the full chain.
				  && distinct.add(new FullChain(a.nestedChain)) ) {
					chains.add(a.nestedChain);
				}
			}
		}
		// Handle more than one level of nesting. I do not currently do more
		// than one level of nesting. If you do more than one level of nesting:
		// @stretch: new LinkedList<>(chains) averts CME, but wastes time: O(n)
		// * numRecursions. A List that does not CME would be faster, so write
		// a new List impl sans CME, and change chains to it, and get rid of da
		// new list! BTW, I would start from LinkedList.
		// Note that we do not currently do more than one level of nesting as
		// no known Sudoku requires it, and all Sudokus are known, apparently,
		// so just ignore this, it will never be a real problem. It will only
		// effect smart-asses who LOVE to Shft-F5 and HATE waiting, like me.
		// Because It Was There!
		if ( !chains.isEmpty() ) {
			for ( AChainingHint chain : new LinkedList<>(chains) ) {
				chains.addAll(chain.recurseNestedChains(new LinkedList<>()));
			}
		}
		return chains;
	}

	// only in GUI: performance is not important, but doesnt suck too badly.
	private LinkedList<AChainingHint> getNestedChains() {
		assert hinter.isNesting();
		if ( chains == null ) {
			chains = recurseNestedChains(new LinkedList<>());
		}
		return chains;
	}

	private Pair<AChainingHint, Integer> getNestedChain(int nestedViewNum) {
		assert hinter.isNesting();
		int count;
		for ( AChainingHint nc : getNestedChains() ) {
			if ( (count=nc.getViewCount()) > nestedViewNum ) {
				return new Pair<>(nc, nestedViewNum);
			}
			nestedViewNum -= count;
		}
		return null;
	}

	// Find the assumption from which the given nested chain started.
	// @return the assumption at which the nestedChain starts.
	// KRC 2019-08-20 reverted to orginal coz there are cases where Ass.owner
	// was being over-written, so we were getting "random" targets quickly in
	// CellReductionHints produced by DynamicPlus and Nested* (ie degree > 0)
	private Ass getContainerTarget(AChainingHint nestedChain) {
        for ( Ass target : getChainsTargets() ) {
            for ( Ass a : ancestors(target) ) {
                if ( a.nestedChain == nestedChain ) { // reference == OK
                    return a;
				}
			}
		}
        return null;
	}

	// each subclass implements getting the actual aquaIndices
	public abstract Set<Integer> getFlatAquaIndices();

	// get indices of cells to paint with an aqua background from nested hint
	// final: nobody overrides me, but its feasible (just remove final).
	public final Set<Integer> getNestedAquaIndices(final int nestedViewNum) {
		assert hinter.isNesting();
		final Pair<AChainingHint, Integer> p = getNestedChain(nestedViewNum);
		// terniary fast enough in hint-land (but not in search-land).
		return p!=null ? p.a.getAquaBgIndices(0) : null;
	}

	/**
	 * The "aqua cells" are those which are highlighted with an aqua background
	 * to denote "this is interesting" to the user. "Interesting" is defined
	 * locally by each hint-type. Usually aquas are cells in the hint pattern;
	 * sometimes (TwoStringKite) its only SOME of those cells, but aquas could
	 * be ANY cell/s that you want to draw the users attention to, in order to
	 * render the logic underpinning the hint.
	 * <p>
	 * <b>NOTE WELL</b>: getNestedAquaCells passes viewNum=0, hence an endless
	 * birecursive loop is not possible, because it goes flat immediately.
	 * This precludes double-imbedding chainers, but ONE level of imbedding is
	 * complex enough, thank you very much.
	 *
	 * @param viewNum view number 0 is the master hint, and each imbedded hint
	 *  has its own viewNumber (which is its order in the list), thus we allow
	 *  the user to view each contributing hint in turn, if they wish to do so
	 * @return indices of cells to paint with an aqua background
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final Set<Integer> getAquaBgIndices(final int viewNum) {
		// check cache
		if ( aquaCells == null )
			this.aquaCells = new Set[getViewCount()];
		else if ( aquaCells[viewNum] != null )
			return aquaCells[viewNum];
		// calculate and encache
		if ( viewNum < flatViewCount )
			aquaCells[viewNum] = getFlatAquaIndices();
		else
			aquaCells[viewNum] = getNestedAquaIndices(viewNum - flatViewCount);
		return aquaCells[viewNum];
	}

	// note that this method is invoked on the nestedChain (a AChainingHint),
	// not on the "main" starting hint that was shown to user.
	private Set<Ass> collectHinterParents(final Grid initGrid, final Grid currGrid
			, final Ass target, final Set<Ass> rents) { // parents
		final Set<Ass> distinct = new MyFunkyLinkedHashSet<>(128, 1F);
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		for ( Ass a=target; a!=null; a=todo.poll() ) {
			if ( distinct.add(a) ) {
				if ( a.isOn ) {
					collectHinterParentsOfOn(a, initGrid, currGrid, rents, todo);
				} else { // nb: addAll is null-safe
					todo.addAll(a.parents); // no concurrent modification exceptions
				}
			}
		}
		return rents;
	}

	private void collectHinterParentsOfOn(final Ass a, final Grid initGrid
			, final Grid currGrid, final Set<Ass> rents, final Queue<Ass> todo) {
		assert a.isOn;
		Cause cause = a.cause; // the type of the cause of this Assumption
		if ( cause == null ) { // This is the initial assumption
			assert !a.hasParents() || (this instanceof CycleHint);
			if ( this instanceof CellReductionHint )
				cause = Cause.NakedSingle;
			else if ( this instanceof RegionReductionHint )
				// region.cause is HiddenBox, HiddenRow, or HiddenCol
				cause = Cause.CAUSE[((RegionReductionHint)this).getRegion().rti];
		}
		if ( cause != null ) {
			if ( cause == Cause.NakedSingle ) {
				final int pinkos;
				final int indice = a.indice;
				// pinkos = initial maybes except current maybes
				if ( (pinkos=initGrid.maybes[indice] & ~currGrid.maybes[indice]) > 0 ) // 9bits
					for ( int v : VALUESES[pinkos] )
						rents.add(new Ass(indice, v, OFF));
			} else { // HiddenSingle
				// throws AIOOBE if cause is not Hidden*
				final ARegion cr = currGrid.cells[a.indice].regions[cause.rti]; // current region
				final ARegion ir = initGrid.regions[cr.index]; // initial region
				final int value = a.value;
				// foreach erased place for value in region
				// erased = initial except current
				for ( int i : INDEXES[ir.places[value] & ~cr.places[value]] )
					rents.add(new Ass(cr.indices[i], value, OFF));
			}
		}
		// HACK this is an on, my parents are offs, but his parents are ons.
		if ( a.parents!=null )
			for ( Ass p : a.parents )
				todo.addAll(p.parents); // no concurrent modification exception
	}

	protected Collection<Link> getFlatLinks(final Ass target) {
		final Collection<Link> links = new ArrayList<>(64);
		for ( Ass c : ancestors(target) ) // child
			if ( c.parents != null )
				for ( Ass p : c.parents ) // parent
					links.add(new Link(p, c));
		return links;
	}

	protected final Collection<Link> getNestedLinks(final int nestedViewNum) {
		final Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null )
			return null;
		return pair.a.getLinks(pair.b);
	}

	/**
	 * Get the links - the brown arrow from a potential value to his effected
	 * potential value in a chain.
	 *
	 * @param viewNum the view number, zero-based.
	 * @return the links to draw, or <tt>null</tt> if none.
	 */
	@Override
	public Collection<Link> getLinks(final int viewNum) {
		try {
			if ( viewNum < flatViewCount )
				return getFlatLinks(getChainTarget(viewNum));
			else
				return getNestedLinks(viewNum - flatViewCount);
		} catch (Exception ex) {
			// I am only ever called in the GUI, so just log it.
			Log.println("AChainingHint.getLinks: "+ ex);
			return null;
		}
	}

	/**
	 * Returns a {@code LinkedList<Ass>} containing the parent assumptions
	 * of this Chaining Hint.
	 * <p>
	 * Called by ChainsHintsAccumulator.add for each chaining hint.
	 *
	 * @param initGrid the initial (unmodified) Grid
	 * @param currGrid the current (modified) Grid
	 * @param rents the Set of parent assumptions which I will
	 *  search each new assumptions parents.
	 * @return the parents (if any, else it is an empty {@code LinkedList<Ass>})
	 */
	@Override
	public MyLinkedList<Ass> getParents(final Grid initGrid
			, final Grid currGrid, final IAssSet rents) {
		Cause cause;
		ARegion ir, cr;
		int[] im, cm, ii;
		int indice, v, r;
		// start at 64, it will grow automatically
		final Set<Ass> result = new LinkedHashSet<>(gpSetSize, 0.75F);
		final Set<Ass> distinct = new MyFunkyLinkedHashSet<>(128, 1F);
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		// WARN: collect each target separately because in Cell/RegionReduction
		// targets that are equals have varying ancestors (ie causes).
		for ( Ass target : getChainsTargets() )
			for ( Ass a=target; a!=null; a=todo.poll() ) //off with his head!
				if ( distinct.add(a) ) { // add only, no update
					if ( a.isOn ) {
						// find the parents of this On
						cause = a.cause; // type of hint that caused this Ass
						if ( cause == null ) { // This is the initial Ass
							assert (this instanceof CycleHint) || a.parents==null;
							if ( this instanceof CellReductionHint )
								cause = Cause.NakedSingle;
							else if ( this instanceof RegionReductionHint )
								cause = Cause.CAUSE[((RegionReductionHint)this).getRegion().rti];
						}
						if ( cause != null ) {
							if ( cause == Cause.NakedSingle ) { // NakedSingle
								indice = a.indice;
								cm = currGrid.maybes;
								im = initGrid.maybes;
								for ( int x : VALUESES[im[indice] & ~cm[indice]] )
									result.add(rents.getAss(indice, x));
							} else { // HiddenBox, HiddenRow, HiddenCol
								v = a.value;
								r = currGrid.cells[a.indice].regions[cause.rti].index;
								cr = currGrid.regions[r];
								ir = initGrid.regions[r];
								ii = ir.indices;
								for ( int index : INDEXES[ir.places[v] & ~cr.places[v]] )
									result.add(rents.getAss(ii[index], v));
							}
						}
					}
					todo.addAll(a.parents);
				}
		if ( result.size() > gpSetSize ) { // getParentsSetSize
			gpSetSize = result.size();
			Debug.println("AChainingHint.gpSetSize="+gpSetSize+" for "+toString());
		}
		return new MyLinkedList<>(result);
	}

	/**
	 *
	 * @param viewNum
	 * @return
	 */
	protected abstract Ass getChainTarget(int viewNum);

	protected Ass getChainTargetFromArray(Collection<Ass> theTargets, int viewNum) {
		if ( targets == null ) {
			targets = new Ass[flatViewCount];
			int count = 0;
			for ( Ass target : theTargets ) {
				targets[count++] = target;
			}
		}
		return targets[viewNum];
	}

	/**
	 * Extenders override this one, and getChainsTargets caches my results.
	 * If chainsTargets is not invariant (ie cannot be cached) then override
	 * getChainsTargets() (you will need to remove his final) and make your
	 * getChainsTargetsImpl() throw a UOE.
	 *
	 * @return {@code ArrayList<Ass>} (not List) coz it is equitable.
	 */
	protected abstract ArrayList<Ass> getChainsTargetsImpl();

	/**
	 * Callers use this one, it caches the result getChainsTargetsImpl().
	 *
	 * @return an {@code ArrayList<Ass>} (coz I am using equals on the lists)
	 * containing the targets of the chains.
	 */
	protected final ArrayList<Ass> getChainsTargets() {
		return chainsTargets!=null ? chainsTargets
				: (chainsTargets=getChainsTargetsImpl());
	}

	protected int getNestedViewCount() {
		if ( !hinter.isNesting() )
			return 0;
		int ttlViewCount = 0;
		for ( AChainingHint nc : getNestedChains() )
			ttlViewCount += nc.getViewCount();
		return ttlViewCount;
	}

	@Override
	public int getViewCount() {
		return viewCount!=0 ? viewCount
				: (viewCount=flatViewCount + getNestedViewCount());
	}

	protected abstract int getFlatComplexity();

	private int getNestedComplexity() {
		int result = 0;
		if ( hinter.isNesting() ) {
			for ( AChainingHint nested : getNestedChains() ) {
				result += nested.getComplexity();
			}
		}
		return result;
	}

	private int getComplexity() {
		if ( complexity == 0 ) {
			complexity = getFlatComplexity() + getNestedComplexity();
		}
		return complexity;
	}

	protected int getLengthDifficulty() {
		int added = 0;
		double ceiling = 4.0D;
		int length = getComplexity() - flatViewCount;
		boolean isOdd = false;
		while ( length > ceiling ) {
			++added;
			if ( isOdd )
				ceiling = ceiling * 4.0D / 3; // integer division intended
			else
				ceiling = ceiling * 3.0D / 2; // integer division intended
			isOdd = !isOdd;
		}
		return added;
	}

	// this method is ALLWAYS used by overriders of getDifficulty to get the
	// base difficulty of this hint before adding to it. (or in ONE case
	// subtracting from it before adding possibly more back to it. Sigh.)
	protected int getBaseDifficulty() {
		// just return my base difficulty := hinter.tech.difficulty
		// no offsets are required at this stage
		return super.getDifficulty();
	}

	@Override
	public int getDifficulty() {
		if ( difficulty == null ) {
			difficulty = getBaseDifficulty() + getLengthDifficulty();
		}
		return difficulty;
	}

	// just returns target.firstParent.firstParent.fir...
	protected Ass getInitialAss(Ass target) {
		// implemented in AChainerBase, coz the hinters use it also.
		return AChainerBase.getInitialAss(target);
	}

	// getInitialAssumptionRecursively searches the whole of targets ancestry
	// (a DFS) for an On with $indice.
	protected Ass getInitialAssRecursively(Ass target, int indice) {
		if ( target.parents != null ) {
			Ass r;
			for ( Ass p : target.parents ) {
				if(p.isOn && p.indice==indice) return p;
				if((r=getInitialAssRecursively(p, indice))!=null) return r;
			}
		}
		return null; // we also get here if not found
	}

	/**
	 * Get the optional first of three names.
	 * Any non-empty string needs a trailing space.
	 *
	 * @return name 1 of 3 (optional)
	 */
	protected String getNamePrefix() {
		return "";
	}
	/**
	 * Get the MANDATORY second of three names.
	 * With neither a leading nor trailing space.
	 * <p>
	 * This method is implemented in each subclass of AChiningHint.
	 *
	 * @return name 2 of 3 (MANDATORY)
	 */
	protected abstract String getNameMiddle();
	/**
	 * Get the optional third of three names.
	 * Any non-empty string needs a leading space.
	 *
	 * @return name 3 of 3 (optional)
	 */
	protected String getNameSuffix() {
		return "";
	}
	/** @return the user-oriented name of this type of hint, which defaults
	 * to the name of the hinter which created this hint, and is overridden
	 * by (currently): CellReductionHint, RegionChainingHint. */
	@Override
	public String getHintTypeNameImpl() {
		return getNamePrefix()+getNameMiddle()+getNameSuffix();
	}

	protected int countAncestors(final Ass target) {
		return ancestors(target).size();
	}

	protected int countAncestors(final Collection<Ass> targets) {
		int count = 0;
		for ( Ass target : targets ) {
			count += countAncestors(target);
		}
		return count;
	}

	/**
	 * Populate htmlLines with HTML of a depth first search (DFS) of asses
	 * ancestry. The ancestors list is used as working storage, and is passed
	 * down from the top rather than have each top invocation create it is own
	 * list. It is populated down the DFS stack and processed at the top, so
	 * that each invocation appends a line of HTML for each of
	 * me-and-all-of-my-ancestors.
	 *
	 * @param a the Ass to examine (Taste!)
	 * @param ancestors A list of all $a parents that is (this is weird)
	 *  populated down in the depth first recursion, and then virtually
	 *  passed back UP to the recurseChainHtmlLines which processes it.
	 * @param htmlLines A List of the lines of HTML which is also passed back
	 *  up from the ancestoral depths, thanks to the DFS.
	 * @param path for debugging, to see "Where the hell are we?"
	 */
	private void recurseChainHtml(final Ass a, final List<Ass> ancestors
			, final List<String> htmlLines, String path) { // path for debug
		// First add parent chains (DFS)
		if ( a.parents!=null ) {
			for ( Ass p : a.parents ) {
				recurseChainHtml(p, ancestors, htmlLines, path+=SP+p);
			}
		}
		if(DEBUG)DEBUG(path);
		// NB: When we get back from the recursive call the $parents List will
		// contain all $a parent assumptions. Remember that it is the one list
		// being mutated by all levels of recursion, so changes down the call
		// stack are seen "back up here".
		// NB: LinkedLists O(n) indexOf is fast enough, coz n is < 100.
		assert ancestors.size() < 101 : "Youre gonna need a bigger boat!";
		if ( a.parents!=null && ancestors.indexOf(a)==-1 ) { // O(n/2) ONCE
			final int n=htmlLines.size(), m=n-1;
			// Add chain item for given assumption
			line.setLength(0); // we are reusing a single buffer. lessG==lessGC.
			line.append('(').append(n+1).append(") ").append("If ");
			// now list each parent Assumption of this Assumption (last first)
			final int last = a.parents.size - 1;
			int i = last;
			// parents DESCENDING is newest to oldest (the last added first)
			for ( Iterator<Ass> it=a.parents.descendingIterator(); it.hasNext(); --i ) {
				final Ass p = it.next(); // parent
				if ( i < last ) { // if this is not the first parent processed
					if ( i > 0 ) {
						line.append(CSP);
					} else {
						line.append(AND); // last when i==0
					}
				}
				line.append(p.weak());
				// recursiveNumCalls * a.parents.size * O(ancestors.size/2)
				// ie a Schlemeil the Painters algorithm (slow), but how else?
				final int pi = ancestors.indexOf(p); // parentsIndex
				if ( m < 0 ) { // -1 meaning these are the parent of the initial assumption
					if ( p.nestedChain != null ) { // parent is a nested chain
						line.append(" (<b>").append(p.nestedChain).append("</b>)");
					}
//					else { // this is first consequence of initial assumption
//						sb.append(" (<b>wingardium coitiotus</b>)"); // getting warmer
//					}
				} else if ( pi < m ) { // if parent is not previous assumption
					if ( pi < 0 ) { // -1 parent not found in ancestors (aliens!)
						if ( p.nestedChain != null ) { // parent is nested chain
							line.append(" (<b>").append(p.nestedChain).append("</b>)");
						} else {
							line.append(" (<b>initial assumption</b>)");
//							line.append(" (<b>misplaced towel</b>)"); // getting warm
						}
					} else if ( pi < 1 ) { // 0 is the initial assumption
						line.append(" (<b>initial assumption</b>)"); // I hope
//						line.append(" (<b>lost luggage</b>)"); // Euston Station
					} else { // a previous assumption, but not THE previous.
						line.append(" (").append(pi+1).append(')');
					}
				}
			} // next parent (or previous, depending on how you look at it)
			line.append(" then ").append(a.strong());
			final String explanation = a.explanation;
			if ( explanation != null ) {
				if ( a.cause == Cause.Foxy ) { // bold
					line.append(" (<b>").append(explanation).append("</b>)");
				} else {
					line.append(" (").append(explanation).append(')');
				}
			}
			ancestors.add(a);
			htmlSize += line.length();
			htmlLines.add(line.toString());
		}
	}

	// the override is a hack around
	protected String getChainHtml(Ass a) {
		// working storage. NB: O(n) contains is not great!
		final List<Ass> ancestors = new LinkedList<>();
		// the output
		final List<String> lines = new LinkedList<>();
		htmlSize = 0;
		line = SB(512); // max observed 481
		recurseChainHtml(a, ancestors, lines, a.toString());
		line = null;
		htmlSize += lines.size()<<3; // <br>NL
		final StringBuilder sb = SB(htmlSize);
		for ( String line : lines ) {
			sb.append(line).append("<br>").append(NL);
		}
		return sb.toString();
	}

	/**
	 * For use by the subtypes to append the "generic" nested chain details to
	 * the specific hint HTML (when hinter.tech.isNesting).
	 * <p>
	 * Used by BinaryChainHint, CellReductionHint, UnaryChainHint, and
	 * RegionReductionHint
	 *
	 * @param html the specific hint HTML, fully formed.
	 * @return the complete hint HTML, with the details of all nested chains.
	 */
	protected String appendNestedChainsHtml(String html) {
		assert hinter.isNesting(); // check this before calling me
		final LinkedList<AChainingHint> nestedChains = getNestedChains();
		if ( nestedChains.isEmpty() )
			return html; // valid coz not all nesting hints have a nest
		final StringBuilder bs = SB(html.length() + (nestedChains.size()<<7)); // * 128
		// append the existing html upto-the-</body>-tag
		final int pos = html.toLowerCase().indexOf("</body>");
		bs.append(html.substring(0, pos)).append(NL);
		// now append the nested chains html
		bs.append("<br>").append(NL)
		  .append("<br>").append(NL)
		  .append("<b>Nested Chains details</b>")
		  .append(" (Note: Nested Chains may rely on some")
		  .append(" <font color=\"blue\">candidates</font>")
		  .append(" that are pre-excluded by there main Chain):<br><br>")
		  .append(NL);
		int index = flatViewCount + 1;
		for ( AChainingHint nestedHint : nestedChains ) {
			bs.append("<i><b>")
			  .append("Nested ").append(nestedHint)
			  .append("</b></i><br>").append(NL);
			for ( Ass target : nestedHint.getChainsTargets() ) {
//if ( hinter.getTech()==Tech.NestedPlus && "B6+8".equals(target.toString()) )
//	Debug.breakpoint();
				// the initialOn is target.firstParent.firstParent.fir...
				final Ass initialOn = getInitialAss(target);
				if ( initialOn == null ) {
					carp("AChainingHint.appendNestedChainsHtml: "+this.toFullString());
					carp("target="+target);
					Debug.dumpAncestors(getChainsTargets());
					throw new IrrelevantHintException();
				}
				bs.append("Chain ").append(index).append(": <b>If ")
						.append(initialOn.weak()).append(" then ")
						.append(target.strong()).append("</b> (View ")
						.append(index).append("):<br>").append(NL)
						.append(getChainHtml(target)).append("<br>").append(NL);
				++index;
			}
		}
		// append the </body></html>
		bs.append(html.substring(pos)).append(NL);
		return bs.toString();
	}

	protected String getHinterName() {
		if ( hinterName == null ) {
			String s = hinter.toString();
			int i = s.lastIndexOf("Chain");
			hinterName = i>-1 ? s.substring(0, i) : s;

		}
		return hinterName;
	}

	protected String getPlusHtml() {
		if ( ((AChainerBase)hinter).degree == 0 )
			return EMPTY_STRING;
		return "<p>\n"
+"Plus means NakedSingle, HiddenSingle, and the Four Quick Foxes: Locking,\n"
+"NakedPairs, HiddenPairs and Swampfish are applied only when no basic chain\n"
+"step is available.\n";
	}

	protected String getNestedHtml() {
		if ( !((AChainerBase)hinter).tech.isNesting )
			return EMPTY_STRING; // empty for "normal" hints
		return "<p>Nested means this chainer parses its assumptions with a" + NL
			 + "chainer that itself makes less complex assumptions, so we are" + NL
			 + "making assumptions on our assumptions. Tequila and hand-guns." + NL
			 + "Do not try this at home kids!</p>";
	}

	/**
	 * Html.colorIn the String s of the Ass a, using boldCyan for the cell.id
	 * and boldOrange for value, because each HTML {?} param needs it is own
	 * custom coloring-in, which sucks. But how else? Maybe, on all html, we
	 * call colorIn AFTER the {?} substitution. Sigh.
	 *
	 * @param a the Ass to be colored in, from which take the cell.id and value
	 * @param s the string to color in; ie a.toWeak(), a.toStrong(), et al.
	 * @return the HTML-color-coded string
	 */
	protected static final String colorAss(Ass a, String s) {
		return Html.colorIn(s
			.replaceAll("value "+a.value, "value <b><o>"+a.value+"</o></b>")
			.replaceAll(CELL_IDS[a.indice], "<b><c>"+CELL_IDS[a.indice]+"</c></b>")
		);
	}

	/**
	 * For caching: Does this hint have any current eliminations.
	 *
	 * @return does this hint still do anything?
	 */
	boolean anyCurrent(Grid grid) {
		return (cell!=null && grid.cells[cell.indice].value == 0)
			|| (reds!=null && reds.anyCurrent(grid));
	}

}
