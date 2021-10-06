/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.solver.hinters.LinkedMatrixAssSet;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.MyLinkedFifoQueue;
import diuf.sudoku.utils.IMyPollSet;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import diuf.sudoku.utils.IFilter;
import java.util.Arrays;

/**
 * An engine for searching a Grid for unary forcing chains and bidirectional
 * cycles. An implementation of the Unary Forcing Chains and Bidirectional
 * Cycles Sudoku solving technique, which follows a chain of implications
 * consequent to a simple assumption of the value of a Cell in the Grid.
 * <p>
 * KRC 2019-11-01 I've split the Chainer class into two subtypes: UnaryChainer
 * and MultipleChainer; leaving the shared methods (like offToOns and onToOffs)
 * in an abstract ChainerBase class. I've done this to distinguish between the two
 * mostly disjunct techniques. Focus makes the code easier to grock, despite
 * its multi-level-ness making it actually more complex.
 * <p>
 * Dynamic and Unary chains have two disjunct sets of "upper level" methods;
 * they really only share the onToOff and offToOn methods, plus a few helper
 * methods; which sounds like inheritance to me. I find this design easier to
 * follow, but it's still a bitch!
 */
public final class ChainerUnary extends ChainerBase {

	// faster to create array once and clear-and-reuse it, even when only
	// creating hint (ie not too often). The GC isnt very smart. This array
	// is currently only used in createBidirectionalCycleHint but could be
	// used elsewhere, as long as the methods do not invoke each other.
	private static final boolean[] BOOLEANS81 = new boolean[GRID_SIZE];
	private static boolean[] booleans81() {
		Arrays.fill(BOOLEANS81, false);
		return BOOLEANS81;
	}

	/**
	 * The UnaryChainer Constructor: an engine for searching a Sudoku Grid
	 * for unary forcing chains.
	 * <p>A "unary" forcing chain follows the consequences of assuming that
	 * a Cell is a value through cells with only two possible values, and
	 * regions with only two positions for a value. This is the simplest and
	 * fastest chaining technique. The slower (more complex) multiple and
	 * dynamic chaining techniques are now implemented in the MultipleChainer
	 * class.
	 * @param isImbedded true ONLY if this is an imbedded (nested) Chainer.
	 * true prevents the superclass ChainerBase from caching my hints.
	 */
	ChainerUnary(boolean isImbedded) {
		super(Tech.UnaryChain, isImbedded);
	}
	public ChainerUnary() {
		this(false); // not imbedded
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		ducacChains.clear();
	}

	// called by supers getHints method to find the hints in this grid
	// and add them to the HintCache.
	@Override
	protected void findChainHints(Grid grid, HintCache hints) {
		// double-check my Tech.UnaryChain setup:
		// !isMultiple && !isDynamic (which implies !isNishio && degree==0)
		assert !isMultiple;
		assert !isDynamic;
		assert !isNishio;
		assert degree == 0;
		// xLoops: Cycles with X-Links (Coloring / Fishy).
		findUnaryChainsAndCycles(T, F, grid, hints);
		//                       X, Y
		// yLoops: Cycles with Y-Links (rare as rocking horse s__t).
		findUnaryChainsAndCycles(F, T, grid, hints);
		//                       X, Y
		// xyLoops: Cycles with both X and Y Links.
		findUnaryChainsAndCycles(T, T, grid, hints);
		//                       X, Y
	}

	/**
	 * Finds all UnaryChains and BidirectionalCycles in the Grid. Unary chains
	 * are the simplest possible forcing chains. They involve a single
	 * assumption which contradicts itself: EG: if we assume that A1 is 6, we
	 * see that A1 cannot be 6, therefore the initial assumption must be
	 * wrong, ie A1 cannot be 6.
	 * <p>NB: In chaining X and Y are link-types, not coordinate system axies.
	 * If I ever trip-over an adequate description of what those link types are
	 * I'll be sure to promulgate it, but I haven't, so I don't know myself.
	 * <p>NB: cycles are sought only in "vanilla" unary chaining mode, ie when
	 * !isMultiple && !isDynamic (which implies degree==0 && !isNishio)
	 * <p>NB: accepts either isXChain or isYChain, or both, but not neither.
	 *
	 * @param isXChain look for Chains and Cycles with X-Links (Coloring/Fishy).
	 * That is do other possible positions for this value get "off"ed?
	 * When isXChain {@code IFilter<Cell>} accepts cells with 2-or-more maybes;
	 * when false we examine only cells with 2 potential values (ie we seek
	 * Y-only-cycles, of which there are just a dozen in top1465, ie these
	 * switches are a bit of a wank, IMHO).
	 * @param isYChain look for Chains and Cycles with Y-Links:
	 * makes onToffs(...) seek consequent naked singles (ie do other potential
	 * values of this cell get "off"ed?); and
	 * makes offToOns(...) seek naked singles (ie if there are only 2 potential
	 * values for this cell then the other value gets "on"ed).
	 * @param hints the HintCache to populate.
	 */
	private void findUnaryChainsAndCycles(boolean isXChain, boolean isYChain
			, Grid grid, HintCache hints) {
		// NB: either isXChain or isYChain, or both, but not neither.
		assert isXChain || isYChain;
		final IFilter<Cell> filter = ucacCellFilter(isXChain);
		// FunkyAssSet has add() only method: doesn't update existing entries.
		final IAssSet onToOn = new FunkyAssSet(128, 1F, true); // observed 94
		final IAssSet onToOff = new FunkyAssSet(256, 1F, false); //observed 144
		// Set for uniqueness + Queue.poll() // observed max is 27
//		final IMyPollSet<Ass> effects = new MyLinkedHashSet<>(32, 1F);
		final IMyPollSet<Ass> effects = new LinkedMatrixAssSet();
		for ( Cell cell : grid.cells ) {
			// filter: X/XYChain max 64, YChain much < 64 (maybesSize==2)
			// and also skip naked/hidden single not yet applied
			if ( filter.accept(cell) && !cell.skip )
				for ( int v : VALUESES[cell.maybes] ) // 64*9 = 576
					doUnaryChainAndCycle(
						  new Ass(cell, v, true)
						, onToOn, onToOff
						, isXChain, isYChain
						, effects, hints
					);
			interrupt();
		}
	}

	/**
	 * unary chains and cycles cell filter: returns the appropriate
	 * {@code Filter<Cell>} for the given isXChain. When isXChain is true we
	 * examine cells which have 2-or-more maybes, and when false (ie we seek
	 * Y-cycles only) we examine cells which have exactly 2 maybes.
	 * <p>
	 * NB: Y-cycles are as rare as rocking-horse-s__t (a dozen in top1465)
	 * but we must still code for them, as efficiently as possible.
	 * 
	 * @param isXChain
	 * @return the appropriate cell filter.
	 */
	private IFilter<Cell> ucacCellFilter(boolean isXChain) {
		if ( isXChain )
			// XY-chains start from cells with two or more maybes.
			return new IFilter<Cell>() {
				@Override
				public boolean accept(Cell cell) {
					return cell.size > 1;
				}
			};
		// Y-cycles start from cells with exactly two maybes.
		return new IFilter<Cell>() {
			@Override
			public boolean accept(Cell cell) {
				return cell.size == 2;
			}
		};
	}

	/**
	 * Finds all UnaryChains and BidirectionalCycles in the Grid which start
	 * from 'anOn' (an "On" Ass assumes that this cell is this value).
	 * <p>Used by: getUnaryChainsAndCycles (in all modes: X, Y, and XY)
	 * <p>Note that cycles are sought only in "vanilla" unary chaining mode,
	 * ie when !isMultiple && !isDynamic (which implies degree==0 && !isNishio)
	 * @param anOn the initial "On" assumption.
	 * @param onToOn {@code IAssSet} to populate with the "On" assumptions that
	 *  are caused by anOn (the initial assumption).
	 * @param onToOff {@code IAssSet} to populate with the "Off" assumptions
	 *  that are caused by anOn (the initial assumption).
	 * @param isXChain Do other possible positions for this value get "off"ed?
	 *  <br>Note that {@code assert isXChain || anOn.cell.maybesSize==2}.
	 * @param isYChain Do other potential values of this cell get "off"ed?
	 * @param effects {@code IMyPollSet<Ass>} populated by find OnToOff and
	 *  OffToOn. Only used internally, ie not by the caller.
	 * @param hints the HintCache which I will populate with
	 *  BidirectionalCycleHint's and UnaryChainHint's.
	 */
	private void doUnaryChainAndCycle(
		  final Ass anOn
		, IAssSet onToOn, IAssSet onToOff
		, boolean isXChain, boolean isYChain
		, IMyPollSet<Ass> effects
		, HintCache hints
	) {
		// Y Cycles can only start if cell has 2 maybes
		assert isXChain || anOn.cell.size==2;
		final List<Ass> cycles = new LinkedList<>();
		if ( doCycle(anOn, onToOn, onToOff, isXChain, isYChain, effects, cycles) )
			for ( Ass dstOn : cycles ) // Bidirectional Cycle found
				hints.add(createBidirectionalCycleHint(dstOn, isYChain, isXChain));
		if ( isXChain ) {
			final LinkedMatrixAssSet chains = this.ducacChains;
			assert chains.isEmpty();
			// follow the forcing chains from the "on" assumption
			doUnary(anOn, onToOn, onToOff, isYChain, effects, chains);
			// follow the forcing chains from the "off" assumption
			doUnary(anOn.flip(), onToOn, onToOff, isYChain, effects, chains);
			// produce unary chain hints
			if ( chains.size > 0 )
				for (Ass c=chains.poll(); c!=null; c=chains.poll())
					hints.add(createUnaryChainHint(c, isXChain, isYChain));
		}
		onToOn.clear();
		onToOff.clear();
	}
	// ducac is short for doUnaryChainAndCycle.
	// LinkedMatrixAssSet.poll is faster than MyLinkedFifoQueue.poll (and I don't understand why)
	private final LinkedMatrixAssSet ducacChains = new LinkedMatrixAssSet(); // observed 2

	/**
	 * Finds Bidirectional Cycles in the Grid which start from the initial "On"
	 * assumption.
	 * <p>A Cycle always starts (and ends) with an "On" assumption. The cells
	 * form a bidirectional cycle: there are exactly two ways of placing the
	 * value in these cells, forming two different possible configurations.
	 * Some values appear in a box, row, or col regardless of which
	 * configuration turns out to be correct. Because one of the two
	 * configurations must be correct, other occurrences of the value can be
	 * removed from effected boxs, rows, or cols.
	 * <p>Mind why this is a BFS and works. I learned that cycles are only
	 * found by DFS. Maybe we are missing loops?
	 * @param initialOnAss the initial "On" assumption.
	 * @param toOn upon entry contains the initial "On" assumption to look at,
	 *  and is then populated with "On" assumptions which are caused by that.
	 * @param toOff the Set of "Off" assumptions to populate with the effects
	 *  of the initial "On" assumption.
	 * @param isXChain Do other possible positions for this value get "off"ed?
	 * @param isYChain Do other potential values of this cell get "off"ed?
	 * @param effects The {@code List<Ass>} that we will add the effects to.
	 * @param cycles a {@code List<Ass>} to populate with assumptions that are
	 *  the start and finish of a bidirectional cycle, from which my caller can
	 *  raise Bidirectional Cycle hints.
	 */
	private boolean doCycle(
		  Ass initialOnAss
		, IAssSet toOn, IAssSet toOff
		, boolean isXChain, boolean isYChain
		, IMyPollSet<Ass> effects
		, List<Ass> cycles
	) {
		assert toOn.isEmpty() && toOff.isEmpty();
		dcQ.clear();
//		assert dcQ.size==0; // the doCycleQueue
		final int initialOnHC = initialOnAss.hashCode;
		int cycleLength=0, ancestorsHC; // ancestorsHashCode
		Ass a, e; // an assumption and it's effect
		toOn.add(initialOnAss);
		dcQ.add(initialOnAss);
		while ( (a=dcQ.poll()) != null ) {
			if ( a.isOn ) { // a is an On
				// populate effects with the direct consequences of this On
				onToOffs(a, isYChain, effects); // high hit rate
				E_LOOP: while ( (e=effects.poll()) != null ) { // e is an Off
					// was: !a.hasAncestor(e) but that's too many calls!
					// now: is 'e' (the new Off) an ancestor of 'a' (the On)?
					ancestorsHC = e.hashCode; // avoid repeated dereferencing
					for ( Ass p=a; p.parents!=null && p.parents.size>0; ) // parent
						if ( (p=p.parents.first.item).hashCode == ancestorsHC )
							continue E_LOOP; // a already hasAncestor e
					toOff.add(e);
					dcQ.add(e);
				} // wend
			} else { // a is an Off
				++cycleLength;
				// populate effects with the direct consequences of this Off
				if ( offToOns(a, toOff, isXChain, isYChain, effects) ) // low hit rate
					while ( (e=effects.poll()) != null ) { // e is an On
						if ( e.hashCode==initialOnHC && cycleLength>1 )
							cycles.add(e);  // Cycle found
						if ( toOn.add(e) ) // Not processed yet
							dcQ.add(e);
					} // wend
			} // esle
		} // wend 'a' in dcQ
		return cycles.size() > 0;
	}
	private final MyLinkedFifoQueue<Ass> dcQ  = new MyLinkedFifoQueue<>();

	/**
	 * Follow the forcing chains from the given "on" or "off" assumption.
	 * @param initAss an "On" or an "Off" assumption to follow
	 * @param toOn a Set of the On assumptions for my use. This wouldn't be a
	 *  parameter if creating a Set with a fast add-only method was fast.
	 *  This set is cleared before (and after by my caller) use.
	 * @param toOff as per toOn except this one holds the "Off"s.
	 * @param isYChain do we seek Y-Chains (and Y-Cycles)
	 * @param effects a poll-able Set of Ass for my internal use. If creating
	 *  one on-the-fly was cheap then I wouldn't pass it around. But creating
	 *  it several million times is bloody expensive, so I create it once, and
	 *  pass him around to all-and-sundry for there independent use; which is
	 *  all bloody Anglo-Bloody-Chineese to me. Sigh.
	 * @param chains the Set of chains to which I add Cyclic Contradictions (ie
	 *  hinty Ass's).
	 */
	private void doUnary( Ass initAss
		, IAssSet toOn, IAssSet toOff
		, boolean isYChain
		, IMyPollSet<Ass> effects
		, Set<Ass> chains
	) {
		if ( duQ.size > 0 )
			duQ.clear();
		// calculate the hashCode of the conjugate (negation) of the initial
		// Assumption ONCE so that we can compare it directly with each
		// potential conjugates hashCode field. Much faster than the old way
		// of creating a conjugate Ass just to equals it, ie it's hashCode!
		// NOTE: 4096 is 1<<12 (13th bit) denoting ON=1 or OFF=0; so to flip
		// an Assumption all we need do is XOR its hashCode with 4096. Simples!
		final int conjugatesHC = initAss.hashCode ^ 4096; // ON=>OFF, OFF=>ON
//		if ( initAss.isOn)
//			conjugatesHC = LSH8[initAss.value] ^ initAss.cell.hashCode;
//		else
//			conjugatesHC = 4096 ^ LSH8[initAss.value] ^ initAss.cell.hashCode;

		int ancestorsHC;
		Ass a, e; // an assumption and it's effect
		boolean found;
		if ( initAss.isOn ) {
			toOn.clear();  toOn.add(initAss);
			toOff.clear();
		} else {
			toOn.clear();
			toOff.clear();  toOff.add(initAss);
		}
		a = initAss;
		do {
			if ( a.isOn ) {
				// ONs (pretty much) allways cause atleast 1 OFF
				onToOffs(a, isYChain, effects);
				while ( (e=effects.poll()) != null ) {
					// if the Conjugate equals our initial assumption.
					// NB: relies on Ass hashcodes being dictinctive
					if ( e.hashCode == conjugatesHC )
						// Cyclic Contradiction found
						chains.add(e); // add only, no update
					// enque e only if none of a's parents are e,
					// ie: if ( !a.hasAncestor(e) ) duQ.add(e)
					// otherwise we go into an infinite loop.
					ancestorsHC = e.hashCode;
					found = false;
					for ( Ass p=a; p.parents!=null && p.parents.size>0; )
						if ( (p=p.parents.first.item).hashCode == ancestorsHC ) {
							found = true;
							break;
						}
					if ( !found && toOff.add(e) ) // Not processed yet
						duQ.add(e);
				} // wend
			// about 1 in 3 OFFs causes an ON, so offToOns returns any?
			} else if ( offToOns(a, toOff, true, isYChain, effects) ) {
				while ( (e=effects.poll()) != null ) {
					// if the Conjugate equals our initial assumption...
					if ( e.hashCode == conjugatesHC )
						// Cyclic Contradiction found
						chains.add(e); // add only, no update.
					if ( toOn.add(e) ) // Not processed yet
						duQ.add(e);
				}
			}
		} while ( (a=duQ.poll()) != null ); // remove head, else stop
	}
	private final MyLinkedFifoQueue<Ass> duQ = new MyLinkedFifoQueue<>();

	/**
	 * Create and return a new BidirectionalCycleHint.
	 * @param dstOn the destination On Assumption
	 * @param isYChain do we seek Y-Chains (and Y-Loops)?
	 * @param isXChain do we seek X-Chains?
	 * @return a new BidirectionalCycleHint
	 */
	private BidirectionalCycleHint createBidirectionalCycleHint(Ass dstOn
			, boolean isYChain, boolean isXChain) {
		assert dstOn.isOn; // Cycles start and end with an "on" assumption.

		// build a contains-array of the cells in the chain
		// nb: more efficient to walk the parent chain ONCE than it is to walk
		// the whole damn parent chain for each sibling in each element.
		// Ideally we'd add each cell to an Idx, so that I can just !contains.
		final boolean[] isInChain = booleans81();
		int numCycles = 0;
		Ass a;
		for ( a=dstOn; a.parents!=null&&a.parents.size>0; a=a.parents.first.item ) {
			assert a.parents.size == 1; // it's a "straight" causal chain
			isInChain[a.i] = true;
			++numCycles;
		}
		assert a.equals(dstOn); // dstOn occurs at beginning and end

		// cancel-out assumptions
		final int n = (numCycles>>1) + 1;
		Set<Ass> cancelOn = new LinkedHashSet<>(n, 1F);
		Set<Ass> cancelOff = new LinkedHashSet<>(n, 1F);
		Set<Ass> cancel;
		int sv; // shiftedValue
		for ( a=dstOn; a.parents!=null&&a.parents.size>0; a=a.parents.first.item ) {
//			assert a.parents.size == 1;
//			assert a.cell == grid.cells[a.i];
			if ( a.isOn ) // terniaries are slow!
				cancel = cancelOn;
			else
				cancel = cancelOff;
			sv = VSHFT[a.value];
			for ( Cell sib : a.cell.siblings )
				if ( (sib.maybes & sv)!=0 && !isInChain[sib.i] )
					cancel.add(new Ass(sib, a.value, false));
		}
		// remove all cells from cancelOn that are NOT in cancelOff
		cancelOn.retainAll(cancelOff);
		if ( cancelOn.isEmpty() )
			return null;

		// build removable potentials
		Pots redPots = new Pots(cancelOn.size());
		for ( Ass redAss : cancelOn ) // McCarthy will be pleased
			redPots.upsert(redAss.cell, redAss.value);

		// create and return the hint
		Ass dstOff = reverseCycle(dstOn);
		return new BidirectionalCycleHint(this, redPots, isYChain, isXChain
				, dstOn, dstOff);
	}

	/**
	 * Reverses the bidirectional cycle which starts and ends with the given
	 * Ass 'a'. It's not just you. This method IS a nightmare!
	 * <p>
	 * Used by createBidirectionalCycleHint (above) to reverse the chain.
	 *
	 * @param a Ass
	 * @return Ass the target (start and end of) the reversed cycle.
	 */
	private Ass reverseCycle(Ass a) {
		if ( a == null )
			return null;
		// use a stack (a LIFO Queue) to reverse the order of the parent-list.
		Deque<Ass> stack = new LinkedList<>();
		String prevExpl = null;
		for ( Ass c=a; c!=null; c=c.parents.peekFirst() ) { // c for current
			stack.addFirst(new Ass(c.cell, c.value, !c.isOn
					, null // parent is null, for now
					, c.cause, prevExpl));
			prevExpl = c.explanation;
			if ( c.parents==null )
				break; // the end of a's ancestoral-tree
		}
		// rewire parents in the reverse order. Confused yet?
		// This is the bit that kept me up at knight.
		// But your leg's off. No it isn't.
		Ass p = null; // p for previous
		for ( Ass c : stack ) { // c for current
			if ( p != null )
				p.addParent(c); // need method coz p.parents==null before call
			p = c;
		}
		return stack.getFirst(); // ie the first item in the parent-list
	}

	private UnaryChainHint createUnaryChainHint(Ass target, boolean isXChain
			, boolean isYChain) {
		final Pots redPots = createRedPots(target);
		if(redPots==null) return null;
		return new UnaryChainHint(this, target, redPots, isYChain, isXChain);
	}

}
