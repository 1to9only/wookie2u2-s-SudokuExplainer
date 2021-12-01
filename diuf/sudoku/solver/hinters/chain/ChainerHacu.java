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
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.SummaryHint;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Values.VALUESES;
import java.util.List;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Collection;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.utils.Log;
import java.util.Comparator;

/**
 * The ChainerHintsAccumulator is used by MultipleChainer.getHinterEffects
 * which is only used when degree>0 which implies isDynamic. It translates an
 * AHint into a list of Assumptions called effects. The hints produced by all
 * of MultipleChainer hinters implement the IChildHint interface to provide me
 * with the getParents method which cleverly searches the current and initial
 * grids for erased-maybes, in order to locate the Ass's which caused (are
 * parents of) the Ass I'm creating. All the code that's specific to each type
 * of hint is in the hints class.
 */
public final class ChainerHacu implements IAccumulator {

	private final Grid initGrid;
	private final Grid currGrid;
	private final IAssSet parentOffs;
	private final List<Ass> effects;

	/**
	 * Constructor.
	 *
	 * @param ig the initial grid (without any chaining erasures)
	 * @param cg the current grid (with chaining erasures)
	 * @param rents the parent Ass's in which I find the parents of each Ass
	 *  that I create, each having it's own parents, and therefore forming a
	 *  backward-chain, where each node only knows it's parent/s.
	 * @param effects the Ass List to which I add
	 */
	public ChainerHacu(Grid ig, Grid cg, IAssSet rents, List<Ass> effects) {
		this.initGrid = ig;
		this.currGrid = cg;
		this.parentOffs = rents;
		this.effects = effects;
	}

	@Override
	public boolean isSingle() { return false; }

	@Override
	public void reset() {
		// do nothing, and won't be invoked anyway. It's just part of the
		// interface which isn't currently used in Chainer, but I suppose
		// it conceivably could be in future, so here I am, waiting.
	}

	@Override
	public boolean add(AHint hint) {
		if ( hint==null )
			return false;
		prevHint = hint;
		if ( hint.reds == null ) {
			// except Summary of hint/s already applied by HintsApplicumulator
			if ( !(hint instanceof SummaryHint) )
				StdErr.whingeLns(Log.me()+": null redPots in: "+hint, initGrid, currGrid);
			return false;
		}
		if ( hint.reds.isEmpty() )
			return StdErr.whinge(Log.me()+": Empty redPots in "+hint);
		final MyLinkedList<Ass> parents; // effect Ass's parent Ass's
		try {
			// all hints produced by Chains.hinters implement IChildHint,
			// but the AHint argument type is specified by IHintsAccu.
//			if ( !(hint instanceof IChildHint) )
//				Debug.breakpoint();
			IChildHint childHint = (IChildHint)hint;
			parents = childHint.getParents(initGrid, currGrid, parentOffs);
		} catch (UnsolvableException ignoreThisHint) {
			// thrown by PointFishHint.getParents because this hint is NOT a
			// chained hint: ie it has NO parents, ie it's the RAW grid!
			// Creating a chain from non-chained hints is NUTTY!
			return false;
		}
		boolean result = false;
		final Pots redPots = hint.reds;
		final AChainingHint nestedChain;
		if ( hint instanceof AChainingHint )
			nestedChain = (AChainingHint)hint;
		else
			nestedChain = null;
		for ( java.util.Map.Entry<Cell,Integer> e : redPots.entrySet() )
			for ( int v : VALUESES[e.getValue()] )
				result |= effects.add(new Ass(e.getKey(), v, false, parents
						, Cause.Advanced, hint.toString() // explanation
						, nestedChain));
		return result;
	}
	private AHint prevHint;

	@Override
	public AHint getHint() {
		final AHint h = prevHint;
		prevHint = null;
		return h;
	}

	@Override
	public boolean addAll(Collection<? extends AHint> hints) {
		boolean result = false;
		for ( AHint hint : hints )
			result |= add(hint);
		return result;
	}

	@Override
	public AHint peek() {
		return prevHint;
	}

	@Override
	public AHint peekLast() {
		return null;
	}

	@Override
	public boolean any() {
		return !effects.isEmpty();
	}

	@Override
	public int size() {
		return effects.size();
	}

	@Override
	public void sort(Comparator<AHint> comparator) {
		// a no-op
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		// a no-op
	}

	@Override
	public List<? extends AHint> getList() {
		throw new UnsupportedOperationException("Not supported.");
	}

}
