/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.utils.Hash.ON_BIT;
import static diuf.sudoku.utils.Hash.SHFT_V;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.MyFunkyLinkedHashSet;
import java.util.Collection;

/**
 * A FunkyAssSet is shorthand for a {@code MyFunkyLinkedHashSet<Ass>} with
 * a copy-con which doesn't double-up initialCapacity, and the
 * {@code getAss(Cell, value)} method specified by IAssSet necessitating
 * that isOn is passed to my constructor; ergo the Assumptions in this Set
 * must either be all "On" or all "Off" for getAss(Cell,int) to work, which
 * IS happily the case in [almost] all the Chainers assumption-sets, at the
 * time of writing (2017 Dec).
 * <p>
 * 2018 Jan: except "effects" in doMultipleDynamicChains which is now an
 * {@code IMyPollSet<Ass>} to NOT expose the getAss method, thus averting
 * this potential problem; and also puts some humour in your lunch-box.
 * <p>
 * 2021-07-20 Moved from diuf.suduku.utils because utils should remain
 * ignorant of application-types, so that it's portable between projects.
 */
public class FunkyAssSet extends MyFunkyLinkedHashSet<Ass> implements IAssSet {

	/** the Ass.hashCode part for the given isOn. */
	private final int onBit;

	/** Constructor.
	 * @param initialCapacity the initial size of the Set.
	 * @param loadFactor the size vs capacity factor.
	 * @param isOn Does this Set contain "on"s or "off"s. */
	public FunkyAssSet(int initialCapacity, float loadFactor, boolean isOn) {
		super(initialCapacity, loadFactor);
		this.onBit = isOn ? ON_BIT : 0;
	}

	/** This copy-constructor clears the given {@code IMySet<Ass> c} if
	 * isClearing: it's more efficient to poll() each element out of c than
	 * it is to addAll and then clear c.
	 * @param initialCapacity
	 * @param loadFactor
	 * @param isOn
	 * @param c
	 * @param isClearing */
	public FunkyAssSet(int initialCapacity, float loadFactor, boolean isOn
			, IMySet<Ass> c, boolean isClearing) { // clears c
		super(Math.max(initialCapacity,c.size()), loadFactor);
		this.onBit = isOn ? ON_BIT : 0;
		if ( isClearing ) // steal all from c
			for ( Ass a=c.poll(); a!=null; a=c.poll() ) // removes head
				add(a);
		else // just add all from c
			addAll(c);
	}

	/**
	 * A basic copy constructor to use FunkyAssSet in KrakenFisherman.
	 * @param c
	 * @param isOn
	 */
	public FunkyAssSet(Collection<Ass> c, boolean isOn) {
		super(c.size(), 0.75F);
		this.onBit = isOn ? ON_BIT : 0;
		addAll(c);
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
	public Ass getAss(Cell cell, int v) {
		return getAss(cell.i, v);
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
	public Ass getAss(int indice, int v) {
		// translate indice and value into an Ass.hashCode
		return super.getAss(onBit ^ SHFT_V[v] ^ indice);
	}

	/**
	 * contains(Ass) implements IAssSet's contains(Ass) by delegating to
	 * supers contains(Object). This method is not actually used.
	 * @param a
	 * @return Does this FunkyAssSet contain this Ass?
	 */
	@Override
	public boolean contains(Ass a) {
		return super.contains(a);
	}
}
