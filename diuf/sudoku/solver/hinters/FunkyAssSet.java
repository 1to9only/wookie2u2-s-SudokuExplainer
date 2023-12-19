/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.ON_BIT;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.MyFunkyLinkedHashSet;
import static diuf.sudoku.Ass.ASHFT;

/**
 * A FunkyAssSet is a {@code MyFunkyLinkedHashSet<Ass>} with a copy-con that
 * does not double-up initialCapacity, and the {@code getAss(Cell, value)}
 * method specified by IAssSet necessitating that isOn is passed to my
 * constructor. Assumptions in this Set must either be all "On" or all "Off"
 * for getAss(Cell,int) to work, which IS happily the case in [almost] all the
 * Chainers assumption-sets, at the time of writing (2017 Dec).
 * <p>
 * 2018 Jan: except "effects" in doMultipleDynamicChains which is now an
 * {@code IMyPollSet<Ass>} to NOT expose the getAss method, to avert crap
 * results, and put(humour) in your lunch-box.
 * <p>
 * 2021-07-20 Moved from diuf.suduku.utils because utils should (IMHO) remain
 * ignorant of application-types, so that it is portable between projects.
 * <p>
 * 2023-06-02 Moved from diuf.sudoku.solver.hinters.chain.
 */
public class FunkyAssSet extends MyFunkyLinkedHashSet<Ass> implements IAssSet {

	/** the Ass.hashCode part for the given isOn. */
	private final int onBit;

	/**
	 * Package visible. Use factory {@link FunkyAssSetSizes.newFunkyAssSet}.
	 *
	 * @param initialCapacity the initial size of the Set.
	 * @param loadFactor the size vs capacity factor.
	 * @param isOn Does this Set contain "on"s or "off"s.
	 */
	FunkyAssSet(final int initialCapacity, final float loadFactor, final boolean isOn) {
		super(initialCapacity, loadFactor);
		this.onBit = isOn ? ON_BIT : 0;
	}

	/**
	 * Use the factory {@link FunkyAssSetSizes.newFunkyAssSet}.
	 * I am publicly visible for direct use in ChainerMulti, which copies LOTS
	 * of sets which all have a predetermined maximum capacity. The set is
	 * created ONCE, as a copy of an existing set, and then we never add to it,
	 * only remove from it.
	 * <p>
	 * This copy-constructor clears the given {@code IMySet<Ass> c} if
	 * isClearing: it is more efficient to poll() each element out of $c than
	 * it is to addAll and then clear $c.
	 *
	 * @param isOn
	 * @param src
	 * @param isClearing
	 */
	public FunkyAssSet(final boolean isOn, final IMySet<Ass> src
			, final boolean isClearing) { // clears src
		super(src.size(), 1F);
		this.onBit = isOn ? ON_BIT : 0;
		if ( isClearing ) { // steal all from c
			Ass a;
			while ( (a=src.poll()) != null )
				add(a);
		} else {
			addAll(src); // just add all from c
		}
	}

	/**
	 * Gets the Ass which has (Cell, value, and presumably this.isOn).
	 * <p>
	 * Specified by the {@code IAssSet} interface.
	 *
	 * @param cell the Cell
	 * @param v the value
	 * @return the complete Assumption, with ancestors.
	 */
	@Override
	public Ass getAss(final Cell cell, final int v) {
		return getAss(cell.indice, v);
	}

	/**
	 * Gets the Ass which has (indice, value, and presumably this.isOn).
	 * <p>
	 * Specified by the {@code IAssSet} interface.
	 *
	 * @param indice the index of the cell in Grid.cells
	 * @param v the value
	 * @return the complete Assumption, with ancestors.
	 */
	@Override
	public Ass getAss(final int indice, final int v) {
		// translate indice and value into an Ass.hashCode
		return super.getAss(onBit ^ ASHFT[v] ^ indice);
	}

	/**
	 * contains(Ass) implements IAssSets contains(Ass) by delegating back up
	 * to my supers contains(Object), so I am just a narrowing-type-caster.
	 * <p>
	 * The contains method is not currently used on a FunkyAssSet, but is used
	 * on the alternate implementation of IAssSet
	 * ({@link diuf.sudoku.solver.hinters.chain.LinkedMatrixAssSet})
	 * , so is in the IAssSet interface, so I am forced to implement it.
	 * <p>
	 * I think the best answer is it should not be public, and not defined in
	 * the IAssSet interface. Sigh. Maybe I should fix that, but I am too lazy.
	 * I am never called, and should work if I am, so I see no harm, but "should
	 * work" is an untested contention, not a fact. I am wishonomically valid.
	 *
	 * @param a
	 * @return Does this FunkyAssSet contain this Ass?
	 */
	@Override
	public boolean contains(final Ass a) {
		return super.contains(a);
	}

}
