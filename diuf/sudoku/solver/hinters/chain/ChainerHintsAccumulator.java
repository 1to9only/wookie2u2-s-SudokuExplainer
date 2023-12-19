/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Ass.OFF;
import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.IFoxyHint;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.List;
import java.util.Collection;
import java.util.Comparator;

/**
 * CHA is used only by {@link ChainerDynamic#fourQuickFoxes} when isFoxy, ie
 * degree > 0. CHA translates found AHints into the effects Ass List. The hints
 * produced by all {@link ChainerDynamic#foxes} implement {@link IFoxyHint} to
 * expose the getParents method, which searches the currentGrid and initialGrid
 * for erased-maybes, to find the Offs that caused (are parents of) the current
 * AHint, which thus becomes a new "Foxy" Off that is processed just-like any
 * other Off. This is a clever separation of concerns. ++juillerat.kudos;
 * <p>
 * "But Offs dont cause Offs" you say. Well they do actually. This divergence
 * from the "normal" On-&gt;Off-&gt;On-&gt;Off pattern keeps causing bugs in
 * AChainingHint. Let it. Forgive yourself and just keep fixing them. Its easy
 * to forget the odd man out.
 * <p>
 * All the code that is specific to each type of hint is in the hints class,
 * which results in a clean separation of concerns: each hint knows its s__t;
 * such that nobody else need know my s__t. Elegant!
 */
public final class ChainerHintsAccumulator implements IAccumulator {

	private final Grid initialGrid;
	private final Grid currentGrid;
	private final IAssSet parentOffs;
	private final Collection<Ass> results;

	// set by add, retrieved via poll/peek
	private AHint previousHint;

	/**
	 * Constructor.
	 *
	 * @param initialGrid the initial grid (without any chaining erasures)
	 * @param currentGrid the current grid (with chaining erasures)
	 * @param parentOffs the parent Off Asses from which IChildHint.getParents
	 *  (which is implemented by hints raised by every ChainerMulti.hinters)
	 *  gets the parents of each Ass that I create, each having its own parents
	 *  (except the initialOn), to form a backward chain, where each node knows
	 *  all of its parents (and nothing of its children)
	 * @param results the List of Asses I add to
	 */
	public ChainerHintsAccumulator(
			  final Grid initialGrid // IN sans erasures
			, final Grid currentGrid // IN with erasures
			, final IAssSet parentOffs // IN potential parents
			, final Collection<Ass> results // OUT effect Asses
	) {
		this.initialGrid = initialGrid;
		this.currentGrid = currentGrid;
		this.parentOffs = parentOffs;
		this.results = results;
	}

	// ONE hint per call!
	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean isChaining() {
		return true;
	}

	@Override
	public void clear() {
		// a no-op
	}

	/**
	 * This add method is NOT normal. It "parses" the given hint into results,
	 * which is a {@code List<Ass>} (not hints). It also records the last added
	 * hint in the previousHint field, for retrieval via the poll/peek methods.
	 *
	 * @param hint
	 * @return
	 */
	@Override
	public boolean add(final AHint hint) {
		// an IAccumulator simply ignores any request to add a null hint.
		if ( hint == null )
			return false;
		// get the rents (effects parents)
		final MyLinkedList<Ass> rents;
		try {
			// all hints produced by ChainerDynamic.foxes implement IFoxyHint
			IFoxyHint fox = (IFoxyHint)hint;
			rents = fox.getParents(initialGrid, currentGrid, parentOffs);
		} catch (UnsolvableException alice) {
			// UE is thrown by getParents when this hint has NO parents, ie its
			// in the RAW grid, ie its NOT chained, ergo we should not be here.
			// We get here in Shft-F5, where rules say Knee to old women, so we
			// just ignore the living ____ out of it, as if it never was.
			return false;
		}
		// populate results
		boolean result = false;
		final Pots reds = hint.reds;
		// get the nestedChain, if any (imbedded chainers produce nestedChains)
		final AChainingHint nestedChain = hint instanceof AChainingHint
				? (AChainingHint)hint
				: null;
		for ( java.util.Map.Entry<Integer,Integer> e : reds.entrySet() )
			// AFAIK all foxy hinters eliminate one value but I handle multiple
			// values anyway, in case thats not true, now or in the future.
			for ( int v : VALUESES[e.getValue()] )
				result |= results.add(
					new Ass(
						  e.getKey()		// cell
						, v					// value
						, OFF				// this is an Off
						, rents				// from getParents
						, Cause.Foxy		// an Ass from a Foxy hinter
						, hint.toString()	// explanation
						, nestedChain		// nullable
					)
				);
		// for peek and poll
		if ( result )
			previousHint = hint;
		return result; // true
	}

	@Override
	public AHint poll() {
		final AHint result = previousHint;
		previousHint = null;
		return result;
	}

	@Override
	public boolean addAll(final Collection<? extends AHint> hints) {
		boolean any = false;
		if ( hints==null || hints.isEmpty() ) {
			return any;
		}
		// WARN: must call add to use overrides!
		for ( AHint h : hints ) {
			any |= add(h);
		}
		return any;
	}

	@Override
	public AHint peek() {
		return previousHint;
	}

	@Override
	public AHint peekLast() {
		return null;
	}

	@Override
	public boolean any() {
		return size() > 0;
	}

	@Override
	public int size() {
		return results.size();
	}

	@Override
	public void sort(final Comparator<AHint> comparator) {
		// a no-op
	}

	@Override
	public void removeAll(final List<AHint> toRemove) {
		// a no-op
	}

	@Override
	public List<AHint> getList() {
		throw new UnsupportedOperationException("Not supported.");
	}

}
