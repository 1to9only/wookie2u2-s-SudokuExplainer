/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.utils.Hash.ON_BIT;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Arrays;

/**
 * ChainerUnary: searches a Sudoku for unary forcing chains and bidirectional
 * cycles.
 * <p>
 * A "unary" forcing chain follows the consequences of assuming that a Cell
 * is a value through cells with only two possible values, and regions with
 * only two positions for a value. This is the simplest/fastest chaining
 * technique. The more complex (slower) multiple and dynamic chaining
 * techniques are implemented in {@link ChainerMulti}. A Bidirectional Cycle
 * is a series of regions with two places for v that can (like coloring)
 * eliminate external occurrences seeing both sets. Cycles are only occasional
 * post-coloring; There's four coloring hinters and Unary Chains still finds
 * s__t they all miss, in about a tenth of the code. Sigh.
 * <p>
 * KRC 2019-11-01 I've split the Chainer class into two subtypes: UnaryChainer
 * and ChainerMulti; leaving the shared methods (like offToOns and onToOffs)
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

	// a circular-array instead of a Queue.
	private static final int Q_SIZE = 256; // must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;
	private final Ass[] Q = new Ass[Q_SIZE];

	// Creating a HashSet is a little heavy, so it's done ONCE.
	private final IAssSet ons = new FunkyAssSet(128, 1F, true); // observed 94
	private final IAssSet offs = new FunkyAssSet(256, 1F, false); //observed 144

	// both cycles and contradictions
	private final Ass[] results = new Ass[16]; // 16 is just a guess
	private int numResults;

	/**
	 * Constructor.
	 *
	 * @param tech ChainerUnary ONLY implements Tech.UnaryChain, but it's
	 *  passed-in anyway to make it's constructor consistent with the other
	 *  chainers, which require the Tech to be passed-in because they implement
	 *  multiple Tech's. So this parameter exists only to make this constructor
	 *  consistent with those of the other chainers. Clear?
	 * @param isImbedded true ONLY if this is an imbedded (nested) Chainer.
	 * true prevents the superclass ChainerBase from caching my hints.
	 */
	ChainerUnary(final Tech tech, final boolean isImbedded) {
		super(tech, isImbedded);
		// double-check my Tech.UnaryChain setup:
		assert !isMultiple;
		assert !isDynamic;
		assert !isNishio;
		assert degree == 0;
		// ChainerUnary now finds only XY-Cycles and XY-Chains. Y-Cycles are
		// history, as are X-Cycles and X-Chains, because searching the whole
		// grid for them three times was just a waste of time.
		setIsXChain(true);
		setIsYChain(true);
	}
	public ChainerUnary(final Tech tech) {
		this(tech, false); // not imbedded
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		numResults = 0;
		Arrays.fill(results, null);
	}

	// called by supers getHints method to find the hints in this grid
	// and add them to the HintCache.
	@Override
	protected void findChainHints(final Grid grid, final HintCache hints) {
		Ass anOn;
		int i;
		for ( Cell cell : grid.cells ) {
			if ( cell.size>1 && !cell.skip )
				for ( int v : VALUESES[cell.maybes] ) {
					if ( doCycle(anOn=new Ass(cell, v, true)) ) {
						for ( i=0; i<numResults; ++i )
							hints.add(createBidirectionalCycleHint(results[i]));
						numResults = 0;
					}
					if ( doUnary(anOn) | doUnary(anOn.flip()) ) {
						for ( i=0; i<numResults; ++i )
							hints.add(createUnaryChainHint(results[i]));
						numResults = 0;
					}
					ons.clear();
					offs.clear();
				}
			interrupt();
		}
	}

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
	 * @param anOn the initial "On" assumption.
	 */
	private boolean doCycle(final Ass anOn) {
		assert ons.isEmpty() && offs.isEmpty();
		Ass a, e; // an assumption and it's effect
		int ancestorsHC; // ancestorsHashCode
		final int initialOnHC = anOn.hashCode;
		int cycleLength = 0;
		boolean ok = true; // presume that 'e' is NOT an ancestor of 'a'
		ons.add(anOn);
		Q[0] = anOn;
		int r = 0, w = 1; // read and write (we just wrote one).
		// nb: for-loop allows inline poll, for speed. The alternate while-loop
		// is much more concise, but requires a poll method, which is slower.
		// So this is uglier than a hatful of assholes, BUT IT'S BLOODY FAST!
		for ( a=Q[r],Q[r]=null,r=(r+1)&Q_MASK;
			  a!=null;
			  a=Q[r],Q[r]=null,r=(r+1)&Q_MASK
		) {
			if ( a.isOn ) { // a is an On
				// populate effects with the direct consequences of this On
				onToOffs(a); // high hit rate
				for ( e=effects[er],effects[er]=null,er=(er+1)&E_MASK;
					  e!=null;
					  e=effects[er],effects[er]=null,er=(er+1)&E_MASK
				) { // e is an Off
					// suppress if 'e' (effect) is an ancestor of 'a' (initOn)
					ancestorsHC = e.hashCode; // avoid repeated dereferencing
					for ( Ass p=a; p.parents!=null; ) // parent
						if ( (p=p.parents.first.item).hashCode == ancestorsHC ) {
							ok = false; // a already hasAncestor e
							break;
						}
					if ( ok ) {
						offs.add(e);
						Q[w] = e;
						w = (w+1) & Q_MASK;
						assert w != r;
					} else // reset for next-time
						ok = true;
				} // wend
			} else { // a is an Off
				++cycleLength;
				// populate effects with the direct consequences of this Off
				if ( offToOns(a, offs) ) // low hit rate
					for ( e=effects[er],effects[er]=null,er=(er+1)&E_MASK;
					      e!=null;
						  e=effects[er],effects[er]=null,er=(er+1)&E_MASK
					) { // e is an On
						if ( e.hashCode==initialOnHC && cycleLength>1 )
							results[numResults++] = e;  // Cycle found
						if ( ons.add(e) ) { // Not processed yet
							Q[w] = e;
							w = (w+1) & Q_MASK;
							assert w != r;
						}
					} // wend
			} // esle
		} // wend 'a' in dcQ
		return numResults > 0;
	}

	/**
	 * Follow the forcing chains from the given "on" or "off" assumption.
	 *
	 * @param anAss an "On" or an "Off" assumption to follow
	 */
	private boolean doUnary(final Ass anAss) {
		Ass a, e; // an assumption and it's effect
		int ancestorsHC;
		boolean found;
		// Calculate the hashCode of the conjugate (negation) of the initial
		// Assumption ONCE so we can compare it with each potential conjugates
		// hashCode field. Much faster than the old way of creating a conjugate
		// Ass just to equals it, ie it's hashCode!
		// NOTE: ON_BIT denotes ON=1 or OFF=0; so to flip an Assumption all we
		// need do is XOR its hashCode with ON_BIT. Simples!
		final int conjugatesHC = anAss.hashCode ^ ON_BIT; // ON=>OFF, OFF=>ON
		int r = 0, w = 0; // duQ read and write index
		if ( anAss.isOn ) {
			ons.clear();  ons.add(anAss);
			offs.clear();
		} else {
			ons.clear();
			offs.clear();  offs.add(anAss);
		}
		for ( a = anAss;
			  a != null;
			  a=Q[r], Q[r]=null, r=(r+1)&Q_MASK ) {
			if ( a.isOn ) {
				// ONs (pretty much) allways cause atleast 1 OFF
				onToOffs(a); // repopulates effects
				for ( e=effects[er],effects[er]=null,er=(er+1)&E_MASK;
					  e!=null;
					  e=effects[er],effects[er]=null,er=(er+1)&E_MASK
				) { // e is an Off
					// if the Conjugate equals our initial assumption.
					// NB: relies on Ass hashcodes being dictinctive
					if ( e.hashCode == conjugatesHC ) {
						results[numResults++] = e; // contradiction
					}
					// enque e only if none of a's parents are e,
					// ie: if ( !a.hasAncestor(e) ) duQ.add(e)
					// otherwise we go into an infinite loop.
					ancestorsHC = e.hashCode;
					found = false;
					for ( Ass p=a; p.parents!=null; )
						if ( (p=p.parents.first.item).hashCode == ancestorsHC ) {
							found = true;
							break;
						}
					if ( !found && offs.add(e) ) { // Not processed yet
						Q[w] = e;
						w = (w+1) & Q_MASK;
						assert w != r;
					}
				} // wend
			// about 1 in 3 OFFs causes an ON, so offToOns returns any?
			} else if ( offToOns(a, offs) ) { // repopulates effects
				for ( e=effects[er],effects[er]=null,er=(er+1)&E_MASK;
					  e!=null;
					  e=effects[er],effects[er]=null,er=(er+1)&E_MASK
				) { // e is an On
					// if the Conjugate equals our initial assumption...
					if ( e.hashCode == conjugatesHC ) {
						results[numResults++] = e; // contradiction
					}
					if ( ons.add(e) ) { // Not processed yet
						Q[w] = e;
						w = (w+1) & Q_MASK;
						assert w != r;
					}
				}
			}
		}
		return numResults > 0;
	}

	/**
	 * Create and return a new BidirectionalCycleHint.
	 * @param dstOn the destination On Assumption
	 * @param isYChain do we seek Y-Chains (and Y-Loops)?
	 * @param isXChain do we seek X-Chains?
	 * @return a new BidirectionalCycleHint
	 */
	private BidirectionalCycleHint createBidirectionalCycleHint(final Ass dstOn) {
		assert dstOn.isOn; // Cycles start and end with an "on" assumption.
		// build a contains-array of the cells in the chain, for speed.
		final boolean[] isInChain = booleans81();
		int numCycles = 0;
		Ass a;
		for ( a=dstOn; a.parents!=null; a=a.parents.first.item ) {
			assert a.parents.size == 1; // cycles are "straight" causal chains
			isInChain[a.i] = true;
			++numCycles;
		}
		assert a.equals(dstOn); // dstOn occurs at beginning and end
		// cancel-out assumptions
		final int n = (numCycles>>1) + 1;
		final Set<Ass> ons = new LinkedHashSet<>(n, 1F);
		final Set<Ass> offs = new LinkedHashSet<>(n, 1F);
		Set<Ass> cancel;
		int sv; // shiftedValue
		for ( a=dstOn; a.parents!=null; a=a.parents.first.item ) {
			if(a.isOn) cancel=ons; else cancel=offs;
			sv = VSHFT[a.value];
			for ( Cell sib : a.cell.siblings )
				if ( (sib.maybes & sv)!=0 && !isInChain[sib.i] )
					cancel.add(new Ass(sib, a.value, false));
		}
		// remove all cells from cancelOn that are NOT in cancelOff
		ons.retainAll(offs);
		if ( ons.isEmpty() )
			return null;
		// build removable potentials
		final Pots reds = new Pots(ons.size());
		for ( Ass redAss : ons ) // McCarthy will be pleased
			reds.upsert(redAss.cell, redAss.value);
		// create and return the hint
		final Ass dstOff = reverseCycle(dstOn);
		setLinkTypes(dstOn);
		return new BidirectionalCycleHint(this, reds, yLink, xLink, dstOn, dstOff);
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
	private Ass reverseCycle(final Ass a) {
		if ( a == null )
			return null;
		// use a stack (a LIFO Queue) to reverse the order of the parent-list.
		final Deque<Ass> stack = new LinkedList<>();
		String prevExpl = null;
		for ( Ass c=a; c!=null; c=c.parents.peekFirst() ) { // c for current
			// parent is null, for now
			stack.addFirst(new Ass(c.cell, c.value, !c.isOn, null, c.cause, prevExpl));
			prevExpl = c.explanation;
			if ( c.parents == null )
				break; // the end of a's ancestoral-tree
		}
		// rewire parents in reverse order.
		Ass p = null; // p for previous
		for ( Ass c : stack ) { // c for current
			if ( p != null )
				p.addParent(c); // need method coz p.parents==null before call
			p = c;
		}
		return stack.getFirst(); // ie the first item in the parent-list
	}

	private UnaryChainHint createUnaryChainHint(final Ass target) {
		final Pots reds = createRedPots(target);
		if(reds==null) return null;
		setLinkTypes(target);
		return new UnaryChainHint(this, target, reds, yLink, xLink);
	}

	private void setLinkTypes(Ass a) {
		yLink = xLink = false;
		for ( ; a.parents!=null; a=a.parents.first.item )
			switch ( a.cause ) {
				case NakedSingle:
					yLink = true;
					if ( xLink )
						return;
					break;
				case HiddenBox:
				case HiddenRow:
				case HiddenCol:
					xLink = true;
					if ( yLink )
						return;
					break;
			}
	}
	private boolean yLink, xLink;

}
