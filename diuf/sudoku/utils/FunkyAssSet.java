/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid;
import diuf.sudoku.Ass;
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
 * I'd highly recommend putting some humour in your kids lunch boxes,
 * especially irksome smarty-panty thinky-outie-sidie-squarey flavour.<br>
 * Monty Python will do perfectly: Learn to create, not memorise!<br>
 * Rodney Rude is good too: There's more than one way to do it!<br>
 * Jasper Carrot doesn't go well with pineapple, so sack Morrison.<br>
 * Billy down! Billy down! Now we've all got a place to park our bikes.<br>
 * I propose that a group of bicyclists should be known as a Billie. Bike
 * racks should be called Connollies. Jeez I will miss him.
 */
public class FunkyAssSet extends MyFunkyLinkedHashSet<Ass>
		implements IAssSet {
	
	/** the Ass.hashCode part for the given isOn. */
	private final int isOnMask;
	
	/** Constructor.
	 * @param initialCapacity the initial size of the Set.
	 * @param loadFactor the size vs capacity factor.
	 * @param isOn Does this Set contain "on"s or "off"s. */
	public FunkyAssSet(int initialCapacity, float loadFactor, boolean isOn) {
		super(initialCapacity, loadFactor);
		this.isOnMask = isOn?4096:0;
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
		this.isOnMask = isOn?4096:0;
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
		this.isOnMask = isOn?4096:0;
		addAll(c);
	}
	
	/** Gets the Ass which has (Cell, value, and presumably this.isOn).
	 * <p>
	 * Specified by the {@code IAssSet} interface.
	 * @param cell the Cell
	 * @param v the value
	 * @return the complete Assumption, with ancestors.
	 */
	@Override
	public Ass getAss(Grid.Cell cell, int v) {
		// translate the cell and value into an Ass hashCode
		return super.getAss(isOnMask ^ Hash.LSH8[v] ^ cell.hashCode);
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
