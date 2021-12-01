/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Result;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Grid.REGION_SIZE;
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
import diuf.sudoku.solver.hinters.IChildHint;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.MyStrings.TWO_OR_MORE_SPACES;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedList;
import java.util.HashMap;

/**
 * Chaining hint. A chaining hint is any hint resulting from a chain of implications.
 */
public abstract class AChainingHint extends AHint implements IChildHint {

//public static int maxAncestorsSize = 0;

	// The size of the set used in getCompleteHinterParents.
	// Is a field to allow me to track max size and bump-up accordingly.
	private int gpSetSize = 64;

	protected final int flatViewCount;
	protected final boolean isXChain; // Is only a single value involved?
	protected final boolean isYChain; // Is only cells with two potential values involved?
	// XY-Chain (both) Is bidirectional cycle. Ie there are exactly two ways of placing
	//                 these values in these cells.
	protected final Ass resultAss; // may be null

	public AChainingHint(AHinter hinter, Pots redPots, int flatViewCount
			, boolean isYChain, boolean isXChain, Ass resultAss) {
		super(hinter, AHint.INDIRECT
				, resultAss!=null && resultAss.isOn ? resultAss.cell : null // cell
				, resultAss!=null && resultAss.isOn ? resultAss.value : 0 // value
				, redPots, null, null, null, null, null
		);
		this.flatViewCount = flatViewCount;
		this.isYChain = isYChain;
		this.isXChain = isXChain;
		this.resultAss = resultAss; // may be null
		assert isXChain || isYChain; // far more likely both
	}

	/** @return the result cell, causing the maybe to be displayed in a
	 * larger bold font, to distinguish it from the also-rans. */
	@Override
	public Result getResult() {
		if ( resultAss == null )
			return null;
		return new Result(resultAss.cell, resultAss.value, resultAss.isOn);
	}

	/**
	 * Get all the ancestors of this Assumption. Each Assumption may have
	 * multiple parents, which may have multiple parents, and so on.
	 * Here we just flatten the tree into a distinct BFS list, loosing the
	 * relationships.
	 * <p>
	 * NOTE that the results are cached, coz I'm called twice in succession.
	 * <p>
	 * This method is package visible for use by FullChain.
	 * <p>
	 * KRC 2020-08-24 added ancestorsMap to cache the list because I'm called
	 * by both getGreens and getReds, and it is wasteful to calculate the list
	 * (which can be quite long ~ upto about 100 I guess) twice.
	 * KRC 2020-09-12 RegionReductionHint and CellReductionHint both override
	 * this implementation to not cache coz, by definition, the target is NOT
	 * distinct, in fact it's the SAME target from all possible initial
	 * assumptions.
	 *
	 * @param target the Assumption to get ancestry of.
	 * @return an {@code ArrayList<Ass>}.
	 */
	ArrayList<Ass> getAncestorsList(Ass target) {
		ArrayList<Ass> list;
		if ( (list=ancestorsMap.get(target)) == null ) {
			list = getAncestorsListImpl(target);
			ancestorsMap.put(target, list);
		}
		return list;
	}
	HashMap<Ass, ArrayList<Ass>> ancestorsMap = new HashMap<>(16, 1F);

	/**
	 * Get all the ancestors of this Assumption, uncached. Each Assumption may
	 * have multiple parents, which may have multiple parents, and so on. So we
	 * flatten the tree into a distinct BFS list, loosing the relationships.
	 * @param target the Assumption to get ancestry of.
	 * @return an {@code ArrayList<Ass>}.
	 */
	protected ArrayList<Ass> getAncestorsListImpl(Ass target) {
		// we need each distinct ancestor (my parents, and my parents
		// parents, and there parents, and so on) of the target Ass.
		// MyFunkyLinkedHashSet does the distinct for us, quickly & easily
		// because it's add-method is add-only (it doesn't update existing).
		// A HashSet of 128 has enough bits for Ass.hashCode (8 bits).
		final Set<Ass> distinctSet = new MyFunkyLinkedHashSet<>(128, 1F);
		// we append the parents of each assumption to the end of a FIFO
		// queue, and poll-off the head of the queue until it's empty.
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		for ( Ass a=target; a!=null; a=todo.poll() )
			if ( distinctSet.add(a) ) // add only, doesn't update existing
				todo.addAll(a.parents); // addAll ignores a null list
		return new ArrayList<>(distinctSet);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Man Of Colors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Also a good rock-n-roll song!
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
	protected Pots getColor(Ass target, boolean wantOn) {
		final ArrayList<Ass> ancestors = getAncestorsList(target);
		// presume that 40% of the hints are ons, and 60% are offs, which is
		// pretty close to how it actually works out, from what I've seen.
		// Remember that the map can grow, it's just less growth is faster.
		final double factor; if(wantOn) factor=0.4D; else factor=0.6D;
		// Plus 0.51 to round up, and plus 2 for "a bit of wiggle room".
		// Pots (a HashMap) constructor rounds capacity UP to the next base-2.
		final int size = Math.max(Pots.MIN_CAPACITY, (int)(ancestors.size() * factor + 2.51));
		Pots color = new Pots(size, 1F);
		for ( Ass a : ancestors )
			if ( a.isOn == wantOn )
				color.upsert(a.cell, a.value);
		return color;
	}

	// has no overrides (yet) so make me final if you feel the need.
	protected Pots getFlatGreens(int viewNum) {
		return getColor(getChainTarget(viewNum), true);
	}
	private Pots getNestedGreens(int nestedViewNum) {
		assert hinter.tech.isNested;
		Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null )
			return null;
		return pair.a.getGreens(pair.b);
	}
	// overridden by BidirectionalCycleHint, all other subtypes use me.
	public Pots getGreensImpl(int viewNum) {
		if ( viewNum < flatViewCount )
			return getFlatGreens(viewNum);
		return getNestedGreens(viewNum - flatViewCount);
	}

	// overriden by BinaryChainingHint, all other subtypes use me.
	protected Pots getFlatReds(int viewNum) {
		return getColor(getChainTarget(viewNum), false);
	}
	protected final Pots getNestedReds(int nestedViewNum) {
		assert hinter.tech.isNested;
		Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null )
			return null;
		return pair.a.getReds(pair.b);
	}
	public Pots getRedsImpl(int viewNum) {
		if ( viewNum < flatViewCount )
			return getFlatReds(viewNum);
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
	private int currViewNum = -1; // ie impossible
	private Pots myRedPots; // my to differentiate them from super.redPots
	private Pots greenPots;
	private Pots orangePots;
	@Override
	public Pots getGreens(int viewNum) {
		getColors(viewNum);
		return greenPots;
	}
	// overridden by BidirectionalCycleHint, all other subtypes use me
	@Override
	public Pots getReds(int viewNum) {
		getColors(viewNum);
		return myRedPots;
	}
	@Override
	public Pots getOranges(int viewNum) {
		getColors(viewNum);
		return orangePots;
	}

	/**
	 * The bluePots are candidates (cells potential values) that have already
	 * been removed from the grid by a previous (nested) hint. They are for
	 * display only, but are considered important coz they're necessary for the
	 * user (well, the programmer, really) to follow the logic underneath the
	 * assumption-chain, in order for them to justify the hint to themselves.
	 * This is essential during testing. One simply cannot test all possible
	 * combinations, but one can find an instance of This and verify that the
	 * constituent logic holds up, and then find an instance of That and check
	 * that it also holds-up; and thereby gain confidence that Sudoku Explainer
	 * handles This AND That. A complex logical construct cannot ever be proven
	 * correct, we can only fail to shoot-down it's constituent parts. One out,
	 * all out. UnsolvableException: You ____ed Up!
	 * <p>
	 * Sudoku Explainer only (currently) needs the bluePots for manual testing,
	 * it doesn't use them itself internally.
	 *
	 * @param grid
	 * @param viewNum
	 * @return
	 */
	@Override
	public Pots getBlues(Grid grid, int viewNum) {
		if ( (viewNum -= flatViewCount) < 0 )
			return null;
		// create a grid from the container (or "main") chain
		Grid nestedGrid = new Grid(grid);
		Pair<AChainingHint, Integer> pair = getNestedChain(viewNum);
		if(pair==null) return null;
		AChainingHint nestedChain = pair.a;
		int nestedViewNum = pair.b;
		Ass target = getContainerTarget(nestedChain);
		for ( Ass a : getAncestorsList(target) )
			if (!a.isOn) // Remove deductions of the container chain
				nestedGrid.cells[a.i].canNotBe(a.value);
		// Use the hinter's parent collector
		Set<Ass> blueAsses = new LinkedHashSet<>(); // theRents
		Ass nestedTarget = nestedChain.getChainTarget(nestedViewNum);
		nestedChain.collectHinterParents(grid, nestedGrid, nestedTarget
				, blueAsses);
		// convert blueAsses to bluePots, a Map of Cell=>Values.
		Pots bluePots = null;
		if ( blueAsses.size() > 0 ) {
			bluePots = new Pots();
			for ( Ass a : blueAsses ) {
				// NB: get the corresponding cell in the current grid because
				// NESTED HINT: a.cell may be in different Grid (different hint)
				bluePots.upsert(grid.cells[a.i], a.value);
			}
		}
		return bluePots;
	}

	// ~~~~~~~~~~~ It is my considered opinion that they're nesting ~~~~~~~~~~~

	private LinkedList<AChainingHint> getNestedChainsRecursively(
			LinkedList<AChainingHint> chainsList) {
		// casually observed max 14, guess 32 covers atleast 90% of cases
		Set<FullChain> set = new MyFunkyLinkedHashSet<>(32, .75F);
		AChainingHint nc;
		for ( Ass target : getChainsTargets() )
			for ( Ass a : getAncestorsList(target) )
				// FullChain's equals and hashCode compares the full chain
				if ( (nc=a.nestedChain)!=null
				  // add is an add-only, it doesn't update existing keys
				  && set.add(new FullChain(nc)) )
					chainsList.add(nc);
		// Look for more than one level of nesting.
		if ( chainsList.size() > 0 )
			for ( AChainingHint chain : new LinkedList<>(chainsList) )
				chainsList.addAll(chain.getNestedChainsRecursively(new LinkedList<>()));
		return chainsList;
	}

	// only used in interactive mode - performance is not uber-important.
	private LinkedList<AChainingHint> getNestedChains() {
		assert hinter.tech.isNested;
		if ( chains == null )
			chains = getNestedChainsRecursively(new LinkedList<>());
		return chains;
	}
	private LinkedList<AChainingHint> chains = null;

	private Pair<AChainingHint, Integer> getNestedChain(int nestedViewNum) {
		assert hinter.tech.isNested;
		int count;
		for ( AChainingHint nc : getNestedChains() ) {
			if ( (count=nc.getViewCount()) > nestedViewNum )
				return new Pair<>(nc, nestedViewNum);
			nestedViewNum -= count;
		}
		return null;
	}

	// Find the assumption from which the given nested chain started.
	// @return the assumption at which the nestedChain starts.
	// KRC 2019-08-20 reverted to orginal coz there are cases where Ass.owner
	// was being over-written, so we were getting "random" targets quickly in
	// CellReductionHint's produced by DynamicPlus and Nested* (ie degree > 0)
	private Ass getContainerTarget(AChainingHint nestedChain) {
        for (Ass target : getChainsTargets())
            for (Ass a : getAncestorsList(target))
                if (a.nestedChain == nestedChain) // reference == OK
                    return a;
        return null;
	}

	public abstract Set<Cell> getFlatAquaCells();

	public Set<Cell> getNestedAquaCells(int nestedViewNum) {
		assert hinter.tech.isNested;
		Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null )
			return null;
		return pair.a.getAquaCells(0);
	}

	/**
	 * The "aqua cells" are those which are highlighted with an aqua background
	 * to denote "this is interesting" to the user. "Interesting" is defined
	 * locally by each hint-type. Most commonly they're the cells in the hint
	 * pattern; sometimes (TwoStringKite) it's only SOME of those cells, but
	 * aquas could be ANY cell/s that you want to draw the users attention to,
	 * in order to the make the logic of the hint make sense to the user.
	 *
	 * @param viewNum
	 * @return
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final Set<Cell> getAquaCells(int viewNum) {
		if ( aquaCells == null )
			this.aquaCells = new Set[getViewCount()];
		else if ( aquaCells[viewNum] != null )
			return aquaCells[viewNum];
		if ( viewNum < flatViewCount )
			// NB: call from getNestedAquaCells allways goes here coz viewNum=0
			aquaCells[viewNum] = getFlatAquaCells();
		else
			aquaCells[viewNum] = getNestedAquaCells(viewNum - flatViewCount);
		return aquaCells[viewNum];
	}
	private Set<Cell>[] aquaCells;

	// note that this method is invoked on the nestedChain (a AChainingHint),
	// not on the "starting" hint that was just found.
	private void collectHinterParents(Grid initGrid, Grid currGrid
			, Ass target, Set<Ass> theRents) { // slang for parents, which is "reserved" in my world
		final Set<Ass> distinctSet = new MyFunkyLinkedHashSet<>(128, 1F);
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		for ( Ass a=target; a!=null; a=todo.poll() )
			if ( distinctSet.add(a) )
				if ( a.isOn )
					collectHinterParentsOfAss(a, initGrid, currGrid, theRents
							, todo);
				else
					// nb: addAll is null-safe
					todo.addAll(a.parents); // no concurrent modification exceptions
	}

	private void collectHinterParentsOfAss(Ass a
			, Grid initGrid, Grid currGrid
			, Set<Ass> theRents, Queue<Ass> todo) {
		assert a.isOn;
		final Cell[] ic = initGrid.cells; // initial matrix
		Cause cause = a.cause; // the type of the cause of this Assumption
		if ( cause == null ) { // This is the initial assumption
			assert !a.hasParents() || (this instanceof BidirectionalCycleHint);
			if ( this instanceof CellReductionHint )
				cause = Cause.NakedSingle;
			else if ( this instanceof RegionReductionHint )
				// region.cause is HiddenBox, HiddenRow, or HiddenCol
				cause = Cause.CAUSE_FOR[((RegionReductionHint)this).getRegion().typeIndex];
		}
		if ( cause != null ) {
			final Cell c = a.cell;
			if ( cause == Cause.NakedSingle ) {
				final int i=c.i;
				final Cell cc = currGrid.cells[i];
				// removed maybes = initial maybes - current maybes
				final int rmvdBits;
				if ( (rmvdBits=ic[i].maybes & ~cc.maybes) != 0 )
					for ( int v : VALUESES[rmvdBits] )
						if ( (rmvdBits & VSHFT[v]) != 0 )
							theRents.add(new Ass(cc, v, false));
			} else { // HiddenSingle
				// this'll throw an AIOOBE if cause is not Hidden*
				final ARegion[] ir = initGrid.regions; // initial regions
				final ARegion cr = c.regions[cause.regionTypeIndex]; // current region
				final int v = a.value;
				Cell cc; // current cell
				// if values possible positions have changed
				if ( ir[cr.index].ridx[v].bits != cr.ridx[v].bits ) {
					final int sv = VSHFT[v];
					for ( int i=0; i<REGION_SIZE; ++i )
						// if any (removed = initial - current) maybes
						if ( (ic[(cc=cr.cells[i]).i].maybes
								& ~(cc.maybes) & sv) != 0 )
							theRents.add(new Ass(cc, v, false));
				}
			}
		}
		// HACK this is an on, my parents are offs, but his parents are ons.
		if ( a.parents!=null && a.parents.size>0 )
			for ( Ass p : a.parents )
				todo.addAll(p.parents); // no concurrent modification exceptions
	}

	protected Collection<Link> getFlatLinks(Ass target) {
		Collection<Link> links = new ArrayList<>(64);
		for ( Ass c : getAncestorsList(target) ) { // c for child
			// nb: don't create an iterator for an empty list!
			if ( c.parents!=null && c.parents.size>0 )
				for ( Ass p : c.parents ) // p for parent
					links.add(new Link(p, c));
		}
		return links;
	}
//private static int flatLinksCalls = 0;
//private static long totalFlatLinksSize = 0;
//private static int maxFlatLinksSize = 0;

	protected final Collection<Link> getNestedLinks(int nestedViewNum) {
		Pair<AChainingHint, Integer> pair = getNestedChain(nestedViewNum);
		if ( pair == null )
			return null;
		return pair.a.getLinks(pair.b);
	}

	/** Get the links - the brown arrow from a potential value to his effected
	 * potential value in a chain.
	 * @param viewNum the view number, zero-based.
	 * @return the links to draw, or <tt>null</tt> if none. */
	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			if ( viewNum < flatViewCount )
				return getFlatLinks(getChainTarget(viewNum));
			else
				return getNestedLinks(viewNum - flatViewCount);
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("AChainingHint.getLinks: "+ ex);
			return null;
		}
	}

	/**
	 * Returns a {@code LinkedList<Ass>} containing the parent assumptions
	 * of this Chaining Hint.
	 * <p>Called by ChainsHintsAccumulator.add for each chaining hint.
	 * @param initGrid the initial (unmodified) Grid
	 * @param currGrid the current (modified) Grid
	 * @param parentOffs the Set of parent assumptions which I will
	 *  search each new assumptions parents.
	 * @return the parents (if any, else it's an empty {@code LinkedList<Ass>})
	 */
	@Override
	public MyLinkedList<Ass> getParents(final Grid initGrid
			, final Grid currGrid, final IAssSet parentOffs) {
		// gchpSetSize currently starts at 64, which "grows" automatically
		final Set<Ass> resultSet = new LinkedHashSet<>(gpSetSize, .75f);
		final Set<Ass> distinctSet = new MyFunkyLinkedHashSet<>(128, 1F);
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		// WARN: collect each target separately because the targets will have
		// different parents even though they are equals (by design).
		for ( Ass target : getChainsTargets() ) {
			assert todo.isEmpty();
			for ( Ass a=target; a!=null; a=todo.poll() ) // poll removes head
				if ( distinctSet.add(a) ) // add only, no update
					collectParentsOfAss(a, initGrid, currGrid, resultSet
							, parentOffs, todo);
		}
		if ( resultSet.size() > gpSetSize ) { // getParentsSetSize
			gpSetSize = resultSet.size();
			Debug.println("AChainingHint.gpSetSize = "+gpSetSize
					+" for "+toString());
		}
		return new MyLinkedList<>(resultSet);
	}

	private void collectParentsOfAss(final Ass a, final Grid initGrid
			, final Grid currGrid , final Set<Ass> resultSet
			, final IAssSet prntOffs, final MyLinkedFifoQueue<Ass> todo) {
		// we only find parents of the On assumptions, so this test handles just
		// the case that the initial target Ass is an off.
		if ( !a.isOn ) {
			todo.addAll(a.parents); // no concurrent modification exceptions
			return;
		}
		// NORMAL processing: assert a.isOn
		Cause cause = a.cause; // the type of thing that caused this Ass
		if ( cause == null ) { // This is initial assumption
			assert (this instanceof BidirectionalCycleHint)
					|| (a.parents==null||a.parents.size==0); //!a.hasParents();
			if ( this instanceof CellReductionHint )
				cause = Cause.NakedSingle;
			else if ( this instanceof RegionReductionHint )
				cause = Cause.CAUSE_FOR[((RegionReductionHint)this).getRegion().typeIndex];
		}
		if ( cause != null ) {
			final Cell[] ic = initGrid.cells; // initial matrix
			final Cell c = a.cell;
			if ( cause == Cause.NakedSingle ) { // Naked Single
				final int i=c.i; // cells index
				final Cell cc = currGrid.cells[i]; // currentCell
				// removed = initial maybes - current maybes
				int rmvdBits; // removedBits
				if ( (rmvdBits=ic[i].maybes & ~cc.maybes) != 0 )
					for ( int v : VALUESES[rmvdBits] )
						resultSet.add(prntOffs.getAss(cc, v));
			} else { // Hidden Single
//slow:			assert cause.name().startsWith("Hidden");
				// this'll throw an AIOOBE if cause is not Hidden*
				final Cell[] rc = c.regions[cause.regionTypeIndex].cells; // cells in the region which contains c
				final int v=a.value, sv=VSHFT[v]; // shiftedValue
				Cell cc; // currGrid's cell
				for ( int i=0; i<REGION_SIZE; ++i ) // removed = initial - current
					if ( (ic[(cc=rc[i]).i].maybes
							& ~cc.maybes & sv) != 0 )
						resultSet.add(prntOffs.getAss(cc, v));
			}
		}
		// this is an on, so my parents are offs, so there parents are ons.
		// if parents is not null or empty. create no iterator for empty list!
		if ( a.parents!=null && a.parents.size>0 )
			for ( Ass p : a.parents )
				todo.addAll(p.parents); // no concurrent modification exceptions
	}

	@Override
	public ChainerBase getHinter() {
		return (ChainerBase)hinter;
	}

	protected abstract Ass getChainTarget(int viewNum);

	protected Ass getChainTargetFromArray(Collection<Ass> theTargets, int viewNum) {
		if ( targets == null ) {
			targets = new Ass[flatViewCount];
			int count = 0;
			for ( Ass target : theTargets )
				targets[count++] = target;
		}
		return targets[viewNum];
	}
	private Ass[] targets;

	/**
	 * Extenders override this one, and I'll cache the result for you.
	 * If chainsTargets is not invariant (ie can't be cached) then override
	 * getChainsTargets() (it's currently final) and make your subclasses
	 * getChainsTargetsImpl() throw a UOE.
	 * @return {@code ArrayList<Ass>} (not List) coz it's equitable.
	 */
	protected abstract ArrayList<Ass> getChainsTargetsImpl();

	/**
	 * Callers use this one, it caches the result getChainsTargetsImpl().
	 * @return an {@code ArrayList<Ass>} (coz I'm using equals on the lists)
	 * containing the targets of the chains.
	 */
	protected final ArrayList<Ass> getChainsTargets() {
		return chainsTargets!=null ? chainsTargets
				: (chainsTargets=getChainsTargetsImpl());
	}
	private ArrayList<Ass> chainsTargets;

	protected int getNestedViewCount() {
		if ( !hinter.tech.isNested )
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
	private int viewCount;

	protected abstract int getFlatComplexity();

	private int getNestedComplexity() {
		assert hinter.tech.isNested;
		int nestedComplexity = 0;
		for ( AChainingHint nc : getNestedChains() )
			nestedComplexity += nc.getComplexity();
		return nestedComplexity;
	}

	private int getComplexity() {
		if ( complexity == 0 ) {
			complexity = getFlatComplexity();
			if ( hinter.tech.isNested )
				complexity += getNestedComplexity();
		}
		return complexity;
	}
	private int complexity;

	protected double getLengthDifficulty() {
		double added = 0.0;
		int ceiling = 4;
		int length = getComplexity() - flatViewCount;
		boolean isOdd = false;
		while ( length > ceiling ) {
			added += 0.1;
			if ( isOdd )
				ceiling = ceiling * 4/3;
			else
				ceiling = ceiling * 3/2;
			isOdd = !isOdd;
		}
		return added;
	}

	// this method is ALLWAYS used by overriders of getDifficulty to get the
	// base difficulty of this hint before adding to it. (or in ONE case
	// subtracting from it before adding possibly more back to it. Sigh.)
	protected double getBaseDifficulty() {
		// just return my base difficulty := hinter.tech.difficulty
		// no offsets are required at this stage
		return super.getDifficulty();
	}

	@Override
	public double getDifficulty() {
		return getBaseDifficulty() + getLengthDifficulty();
	}

	// just returns target.parents.first.first.first...
	// base implementation moved to Chainer, where it's also used.
	protected Ass getSource(Ass target) {
		return ChainerBase.getSource(target);
	}

	// recurseSource tries harder than getSource to locate and return the
	// parent of the given target Ass which sets the given srcCell.
	protected Ass recurseSource(Ass target, Cell srcCell) {
		if ( srcCell!=null && target!=null
		  && target.parents!=null && target.parents.size>0 ) {
			Ass returned;
			for ( Ass a : target.parents ) {
				if ( a.isOn && a.cell.hashCode == srcCell.hashCode )
					return a; // NB: We don't know what value we're look for!
				if ( (returned=recurseSource(a, srcCell)) != null )
					return returned;
			}
		}
		return null; // we also get here if not found
	}

	public String chainType() {
		if ( isXChain )
			if ( isYChain )
				return "XY";
			else
				return "X";
		return "Y";
	}

	protected String getNamePrefix() {
		return EMPTY_STRING;
	}
	protected String getNameMiddle() {
		final ChainerBase base = (ChainerBase)hinter;
		if ( base.isNishio )
			return "Nishio";
		if ( base.isDynamic ) {
			if ( base.degree > 1 )
				return "Nested";
			return "Dynamic";
		}
		if ( base.isMultiple )
			return "Multiple";
		return "Unknown"; // should NEVER happen
	}
	protected String getNameSuffix() {
		//int degree = ((Chainer)hinter).degree;
		switch (degree) {
			case 0 : return "Chain";
			case 1 : return "Plus";
			case 2 : return "Nested (+ "+Tech.UnaryChain.name()+")";
			case 3 : return "Nested (+ "+Tech.MultipleChain.name()+")";
			case 4 : return "Nested (+ "+Tech.DynamicChain.name()+")";
			case 5 : return "Nested (+ "+Tech.DynamicPlus.name()+")";
		}
		throw new IllegalStateException("Bad degree="+degree);
	}
	/** @return the user-oriented name of this type of hint, which defaults
	 * to the name of the hinter which created this hint, and is overridden
	 * by (currently): CellReductionHint, RegionChainingHint. */
	@Override
	public String getHintTypeNameImpl() {
		String s = getNamePrefix()+SP+getNameMiddle()+SP+getNameSuffix();
		s = s.replaceAll(TWO_OR_MORE_SPACES, SP).trim();
		return s;
	}

	protected int countAncestors(Ass target) {
		return getAncestorsList(target).size();
	}

	protected int countAncestors(Collection<Ass> targets) {
		int ancestorCount = 0;
		for ( Ass target : targets )
			ancestorCount += countAncestors(target);
		return ancestorCount;
	}

	/**
	 * Populate htmlLines with HTML of a depth first search (DFS) of Ass 'a's
	 * ancestry. The ancestors list is used as working storage, and is passed
	 * down from the top rather than have each top invocation create it's own
	 * list. It's populated down the DFS stack and processed at the top, so
	 * that each invocation appends a line of HTML for each of
	 * me-and-all-of-my-ancestors.
	 *
	 * @param a the Ass to examine (Taste!)
	 * @param ancestors A list of all 'a's parents that is (this is weird)
	 *  populated down in the depth first recursion, and then virtually passed
	 *  back UP to the recurseChainHtmlLines which processes it.
	 * @param htmlLines A List of the lines of HTML which is also passed back up
	 *  from the ancestoral depths, thanks to the DFS.
	 * @param path for debugging, to see "Where the hell are we?"
	 */
	private void recurseChainHtml(final Ass a, final List<Ass> ancestors
			, final List<String> htmlLines, String path) { // path for debug only
		// First add parent chains (DFS)
		if ( a.parents!=null && a.parents.size>0 )
			for ( Ass p : a.parents )
				recurseChainHtml(p, ancestors, htmlLines, path+=SP+p);
		//System.out.println(path);
		// NB: When we get back from the recursive call the 'parents' List will
		// contain all a's parent assumptions. Remember that it's the one list
		// being mutated by all levels of recursion, so changes down the call
		// stack are seen "back up here".
		// NB: LinkedLists O(n) indexOf is fast enough, coz n is < 100.
		assert ancestors.size() < 101 : "We're gonna need a bigger boat."; // a MyLinkedHashSet, I think: O(1) heavy get/contains, ordered iterator,
//maxAncestorsSize = Math.max(maxAncestorsSize, ancestors.size());
		if ( a.parents!=null && a.parents.size>0 && ancestors.indexOf(a)==-1 ) { // O(n/2) ONCE
			final int n=htmlLines.size(), m=n-1;
			// Add chain item for given assumption
			line.setLength(0); // we're reusing a single buffer. lessG==lessGC.
			line.append('(').append(n+1).append(") ").append("If ");
			// now list each parent Assumption of this Assumption (last first)
			final int last = a.parents.size - 1;
			int i = last;
			// parents DESCENDING is newest to oldest (the last added first)
			for ( Iterator<Ass> it=a.parents.descendingIterator(); it.hasNext(); --i ) {
				final Ass p = it.next(); // parent
				// recursiveNumCalls * a.parents.size * O(ancestors.size/2) = a bit slow Redge
				final int pi = ancestors.indexOf(p); // parentsIndex
				if ( i < last ) // if this is not the first parent processed
					if ( i > 0 )
						line.append(CSP);
					else
						line.append(AND); // last when i==0
				line.append(p.weak());
				if ( m < 0 ) { // -1 meaning these are the parent of the initial assumption
					if ( p.nestedChain != null ) // parent is a nested chain
						line.append(" (<b>").append(p.nestedChain).append("</b>)");
//					else // this is first consequence of initial assumption
//						sb.append(" (<b>aero coitus</b>)"); // getting warmer
				} else if ( pi < m ) { // if parent is not previous assumption
					if ( pi < 0 ) // -1 parent not found in ancestors (aliens!)
						if ( p.nestedChain != null ) // parent is nested chain
							line.append(" (<b>").append(p.nestedChain).append("</b>)");
						else
//							sb.append(" (<b>initial assumption</b>)");
							line.append(" (<b>misplaced towel</b>)"); // getting warm
					else if ( pi < 1 ) // 0 is the initial assumption
//						sb.append(" (<b>initial assumption</b>)"); // I hope
						line.append(" (<b>lost luggage</b>)"); // Euston Station
					else // a previous assumption, but not THE previous.
						line.append(" (").append(pi+1).append(')');
				}
			} // next parent (or previous, depending on how you look at it)
			line.append(" then ").append(a.strong());
			String explanation = a.explanation;
			if ( explanation != null )
				if ( a.cause == Cause.Advanced ) // bold the "odd" explanations
					line.append(" (<b>").append(explanation).append("</b>)");
				else
					line.append(" (").append(explanation).append(')');
			ancestors.add(a);
			htmlSize += line.length();
			htmlLines.add(line.toString());
		}
	}
	private StringBuilder line;
	private int htmlSize;

	// the override is a hack around
	protected String getChainHtml(Ass a) {
		final List<Ass> ancestors = new LinkedList<>(); // working storage
		final List<String> lines = new LinkedList<>(); // the output
		htmlSize = 0;
		line = new StringBuilder(512); // max observed 481
		recurseChainHtml(a, ancestors, lines, a.toString());
		line = null;
		htmlSize += lines.size() * 6; // <br>NL
		final StringBuilder sb = new StringBuilder(htmlSize);
		for ( String line : lines )
			sb.append(line).append("<br>").append(NL);
		return sb.toString();
	}

	/**
	 * For use by the subtypes to append the "generic" nested chain details to
	 * the specific hint HTML (when hinter.tech.isNested).
	 * <p>Used by BinaryChainHint, CellReductionHint, UnaryChainHint, and
	 *  RegionReductionHint</p>
	 * @param html the specific hint HTML, fully formed.
	 * @return the complete hint HTML, with the details of all nested chains.
	 */
	protected String appendNestedChainsHtml(String html) {
		assert hinter.tech.isNested; // check this before calling me
		LinkedList<AChainingHint> nestedChains = getNestedChains();
		if (nestedChains.isEmpty())
			return html; // valid coz not all hints by Nested* are actually nested
		StringBuilder bs = new StringBuilder(html.length() + 128*nestedChains.size());
		// append the existing html upto-the-</body>-tag
		int pos = html.toLowerCase().indexOf("</body>");
		bs.append(html.substring(0, pos)).append(NL);
		// now append the nested chains html
		bs.append("<br>").append(NL)
		  .append("<br>").append(NL)
		  .append("<b>Nested Chains details</b>")
		  .append(" (Note that each Nested Chain may rely upon some")
		  .append(" <font color=\"blue\">candidates</font>")
		  .append(" being pre-excluded by it's main Chain):<br><br>")
		  .append(NL);
		int index = flatViewCount + 1;
		for ( AChainingHint nestedHint : nestedChains ) {
			bs.append("<i><b>")
			  .append("Nested ").append(nestedHint)
			  .append("</b></i><br>").append(NL);
			for ( Ass target : nestedHint.getChainsTargets() ) {
//if ( hinter.tech==Tech.NestedPlus && "B6+8".equals(target.toString()) )
//	Debug.breakpoint();
				// get the source: The Alice Ass of the target Ass
				Ass source = getSource(target); // target.parents.first.first...
				if ( source == null ) {
					System.out.print("AChainingHint.appendNestedChainsHtml: "+this.toFullString());
					System.out.print("target="+target);
					Debug.dumpAncestors(getChainsTargets());
					throw new IrrelevantHintException();
				}
				bs.append("Chain ").append(index).append(": <b>If ")
						.append(source.weak()).append(" then ")
						.append(target.strong()).append("</b> (View ")
						.append(index).append("):<br>").append(NL)
						.append(getChainHtml(target)).append("<br>").append(NL);
				++index;
			}
		}
		// finally append the </body</html>
		bs.append(html.substring(pos)).append(NL);
		return bs.toString();
	}

	protected String getHinterName() {
		if ( hinterName == null ) {
			hinterName = hinter.toString();
			int i = hinterName.lastIndexOf("Chain");
			if ( i > -1 )
				hinterName = hinterName.substring(0, i);
		}
		return hinterName;
	}
	private String hinterName = null;

	protected String getPlusHtml() {
		if ( ((ChainerBase)hinter).degree == 0 )
			return EMPTY_STRING;
		return "<p>Plus means the four quick foxes: Locking, NakedPairs," + NL
		     + "HiddenPairs, and Swampfish are applied only when no basic" + NL
		     + "chain-step is available.</p>";
	}

	protected String getNestedHtml() {
		if ( !((ChainerBase)hinter).tech.isNested )
			return EMPTY_STRING; // empty for "normal" hints
		return "<p>Nested means this chainer parses its assumptions with a" + NL
			 + "chainer that itself makes less complex assumptions, so we're" + NL
			 + "making assumptions on our assumptions. Tequila and hand-guns." + NL
			 + "Don't try this at home kids!</p>";
	}

}