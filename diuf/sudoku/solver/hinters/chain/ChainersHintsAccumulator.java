/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.AppliedHintsSummaryHint;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Values.VALUESES;
import java.util.List;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Collection;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * The ChainersHintsAccumulator is used by MultipleChainer.getHinterEffects
 * which is only used when degree>0 which implies isDynamic. It translates an
 * AHint into a list of Assumptions called effects. The hints produced by all
 * of MultipleChainer hinters implement the IChildHint interface to provide me
 * with the getParents method which cleverly searches the current and initial
 * grids for erased-maybes, in order to locate the Ass's which caused (are
 * parents of) the Ass I'm creating. All the code that's specific to each type
 * of hint is in the hints class.
 */
public final class ChainersHintsAccumulator implements IAccumulator {

	private final Grid initGrid;
	private final Grid currGrid;
	private final IAssSet parentOffs;
	private final List<Ass> effects;

	public ChainersHintsAccumulator(
		  Grid initGrid
		, Grid currGrid
		, IAssSet parentOffs
		, List<Ass> effects
	) {
		this.initGrid = initGrid;
		this.currGrid = currGrid;
		this.parentOffs = parentOffs;
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
		if ( hint.redPots == null ) {
			// except Summary of hint/s already applied by HintsApplicumulator
			if ( !(hint instanceof AppliedHintsSummaryHint) )
				StdErr.whingeLns(StdErr.me()+": null redPots in: "+hint, initGrid, currGrid);
			return false;
		}
		if ( hint.redPots.isEmpty() )
			return StdErr.whinge(StdErr.me()+": Empty redPots in "+hint);
		final MyLinkedList<Ass> parents; // effect Ass's parent Ass's
		try {
			// all hints produced by Chains.hinters implement IChildHint,
			// but the AHint argument type is specified by IHintsAccu.
			IChildHint childHint = (IChildHint)hint;
			parents = childHint.getParents(initGrid, currGrid, parentOffs);
		} catch (UnsolvableException ignoreThisHint) {
			// thrown by PointFishHint.getParents because this hint is NOT a
			// chained hint: ie it has NO parents, ie it's the RAW grid!
			// Creating a chain from non-chained hints is NUTTY!
			return false;
		}
		boolean result = false;
		final Pots redPots = hint.redPots;
		final AChainingHint nestedChain;
		if ( hint instanceof AChainingHint )
			nestedChain = (AChainingHint)hint;
		else
			nestedChain = null;
		for ( Cell cell : redPots.keySet() )
			for ( int v : VALUESES[redPots.get(cell).bits] )
				result |= effects.add(new Ass(cell, v, false, parents
						, Cause.Advanced, hint.toString() // explanation
						, nestedChain));
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends AHint> hints) {
		boolean result = false;
		for ( AHint hint : hints )
			result |= add(hint);
		return result;
	}

	@Override
	public AHint getHint() {
		return null;
	}

	@Override
	public AHint peek() {
		return null;
	}

	@Override
	public AHint peekLast() {
		return null;
	}

	@Override
	public boolean hasAny() {
		return !effects.isEmpty();
	}

	@Override
	public int size() {
		return effects.size();
	}

	@Override
	public void sort() {
		// a no-op
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		// a no-op
	}

}
