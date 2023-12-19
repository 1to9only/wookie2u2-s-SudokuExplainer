/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.Cause.CAUSE;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.COL;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.ICleanUp;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Values.VLAST;
import static diuf.sudoku.Values.VSHFT;
import static java.util.Arrays.fill;

/**
 * ChainerNishio implements the {@link Tech#NishioChain} Sudoku solving
 * technique.
 * <p>
 * Nishios are Dynamic Contradictions. Nishio has no Region or Cell reduction.
 * <p>
 * KRC 2019-07-14 split from ChainerMulti, for speed. Splitting-out Nishio
 * reduces overheads so all the chainers are a bit faster.
 */
public final class ChainerNishio extends AChainerBase implements ICleanUp {

//	private static final boolean DEBUG = false;
//	private static void DEBUG(String msg) {
//		System.out.println(msg);
//	}
//	private static void DEBUG(String frmt, Object... args) {
//		System.out.format(frmt, args);
//	}

	private static final int ON_SIZE = 128; // a power of 2
	private static final int ON_MASK = ON_SIZE - 1;
	private static final Ass[] ONQ = new Ass[ON_SIZE];

	private static final int OFF_SIZE = 256; // a power of 2
	private static final int OFF_MASK = OFF_SIZE - 1;
	private static final Ass[] OFFQ = new Ass[OFF_SIZE];

	/**
	 * The initial (unmodified) grid used to find parent (causal) Assumptions.
	 * We search for cell-vs that have been removed from the current grid that
	 * are still in this unmodified initGrid.
	 */
	private Grid initGrid;
	private Cell[] initCells;

	// nb: nishio isnt imbedded, so create final sets during instantiation.
	// If ChainerNisio is unwanted its not instantiated.
	// If ChainerNisio is wanted then my findChains will probably be invoked.
	// SE is not designed to solve easy puzzles.
	// all On/Off consequences of the initial assumption
	private LinkedMatrixAssSet ons;
	private LinkedMatrixAssSet offs;
	private LinkedMatrixAssSet.Node[][] onNodes;
	private LinkedMatrixAssSet.Node[][] offNodes;

	/**
	 * Construct a new ChainerNishio with cache enabled.
	 */
	public ChainerNishio() {
		super(Tech.NishioChain, false);
	}

	// nb: ChainerNishio is NOT imbedded, so private
	private void initialise() {
		initGrid = new Grid();
		initCells = initGrid.cells;
		ons = new LinkedMatrixAssSet(); onNodes = ons.nodes;
		offs = new LinkedMatrixAssSet(); offNodes = offs.nodes;
	}

	// nb: ChainerNishio is NOT imbedded, so private
	private void deinitialise() {
		initCells = null;
		initGrid = null;
		onNodes = offNodes = null;
		ons = offs = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void findChainHints() {
		assert isDynamic; // Nishios are Dynamic X Contradictions
		Ass ass, con[];
		int bits, values[], m, i, value; // bitset, array, last, index, value
		initialise();
		try {
			for ( int indice=0; indice<GRID_SIZE; ++indice ) {
				if ( sizes[indice] > 1 ) {
					for ( values=VALUESES[bits=maybes[indice]], m=VLAST[bits], i=0; ; ) {
						value = values[i];
						// nb: Nishio has no reduction, so each UE is independant
						try {
							// On: assume cell IS value, calculate effects,
							// adding any contradiction hints
							if ( (con=doChains(ass=new Ass(indice, value, ON))) != null ) {
								// Contradiction: On and Off so initialAss is false
								hints.add(binaryChain(con[0], con[1], ass, ass, OFF, T, 0));
							}
						} catch ( UnsolvableException ex ) {
							// from cell.canNotBe(v). never. never say never.
						}
						try {
							// Off: assume cell IS NOT value, calculate effects,
							// adding any contradiction hints
							if ( (con=doChains(ass=new Ass(indice, value, OFF))) != null ) {
								// Contradiction: On and Off so initialAss is false
								hints.add(binaryChain(con[0], con[1], ass, ass, ON, T, 1));
							}
						} catch ( UnsolvableException ex ) {
							// from cell.canNotBe(v). never. never say never.
						}
						if(++i > m) break;
					}
					hinterrupt();
				}
			}
		} finally {
			deinitialise();
		}
	}

	/**
	 * Calculate the Dynamic X forcing chain consequences of the given initial
	 * assumption in the grid.
	 *
	 * @param a the initial assumption, either an On or an Off.
	 * @return first contradiction: an On and an Off (ordered). <br>
	 *  anAss causes both On and Off of a cell-value, which is absurd, hence
	 *  the initial assumption is proven false. <br>
	 *  For example: A1+1 -&gt H6+6 and H6-6, hence A1 cannot be 1. <br>
	 *  null means that no such contradiction exists. This is complete logic,
	 *  so null doesnt just mean not found, it means does not exist.
	 */
	private Ass[] doChains(Ass a) {
		Cell cell; // ass.cell
		ARegion cRegion; // one of the ass.cell.regions
		int rIndices[] // ass.cell.regions[rti].indices
		  , cPlaceIn[] // ass.cell.placeIn
		  , array[], n, i // array, size, index (ONE stackframe)
		  , value // ass.value
		  , indice // ass.indice, hijacked as sibling indice
		  , rPlaces // bitset of indexes in region.cells
		;
		int onR=0,onW=0, offR=0,offW=0; // ONQ/OFFQ read/write index
		boolean earlyExit = true;
		initGrid.copyFrom(grid);
		ons.clear(); onNodes = ons.nodes;
		offs.clear(); offNodes = offs.nodes;
		// add the initial assumption to the appropriate Set
		if ( a.isOn )
			ons.add(a);
		else
			offs.add(a);
		try {
			do {
				value = a.value;
				cell = cells[a.indice];
				cPlaceIn = cell.placeIn;
				if ( a.isOn ) { // every On has Off effect/s, most have many.
					// X-Link: other places for value in my regions are Off
					rIndices = (cRegion=cell.box).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[BOX]], n=array.length, i=0; i<n; ++i ) {
						// detect contradiction
						if ( onNodes[indice=rIndices[array[i]]][value] != null ) {
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX])
							};
						// avert tangle: process each On ONCE
						} else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
					rIndices = (cRegion=cell.row).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[ROW]], n=array.length, i=0; i<n; ++i ) {
						// detect contradiction
						if ( onNodes[indice=rIndices[array[i]]][value] != null ) {
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW])
							};
						// avert tangle: process each On ONCE
						} else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
					rIndices = (cRegion=cell.col).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[COL]], n=array.length, i=0; i<n; ++i ) {
						// detect contradiction
						if ( onNodes[indice=rIndices[array[i]]][value] != null ) {
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL])
							};
						// avert tangle: process each On ONCE
						} else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
				} else { // a is an Off: ~30% of Offs have On effects
					// X-Link: if numPlaces[v]==2 then other place is On
					if ( (cRegion=cell.box).numPlaces[value] == 2 ) {
						rPlaces = cRegion.places[value];
						indice = (rIndices=cRegion.indices)[IFIRST[rPlaces & ~cPlaceIn[BOX]]];
						// detect contradiction
						if ( offNodes[indice][value] != null ) {
							return new Ass[] {
								on(indice, value, a, BOX, rPlaces, rIndices)
							  , offNodes[indice][value].ass
							};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][value] == null ) {
							ONQ[onW] = ons.justAdd(on(indice, value, a, BOX, rPlaces, rIndices));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					if ( (cRegion=cell.row).numPlaces[value] == 2 ) {
						rPlaces = cRegion.places[value];
						indice = (rIndices=cRegion.indices)[IFIRST[rPlaces & ~cPlaceIn[ROW]]];
						// detect contradiction
						if ( offNodes[indice][value] != null ) {
							return new Ass[] {
								on(indice, value, a, ROW, rPlaces, rIndices)
							  , offNodes[indice][value].ass
							};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][value] == null ) {
							ONQ[onW] = ons.justAdd(on(indice, value, a, ROW, rPlaces, rIndices));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					if ( (cRegion=cell.col).numPlaces[value] == 2 ) {
						rPlaces = cRegion.places[value];
						indice = (rIndices=cRegion.indices)[IFIRST[rPlaces & ~cPlaceIn[COL]]];
						// detect contradiction
						if ( offNodes[indice][value] != null ) {
							return new Ass[] {
								on(indice, value, a, COL, rPlaces, rIndices)
							  , offNodes[indice][value].ass
							};
						// avert tangle: process each On ONCE
						} else if ( onNodes[indice][value] == null ) {
							ONQ[onW] = ons.justAdd(on(indice, value, a, COL, rPlaces, rIndices));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					cell.removeMaybes(VSHFT[value]);
				}
				// process all Ons before any Off for when isDynamic
				if ( (a=ONQ[onR]) != null ) {
					ONQ[onR] = null;
					onR = (onR + 1) & ON_MASK;
				} else if ( (a=OFFQ[offR]) != null ) {
					OFFQ[offR] = null;
					offR = (offR + 1) & OFF_MASK;
				}
			} while ( a != null );
			earlyExit = false;
		} finally {
			grid.copyFrom(initGrid);
			if ( earlyExit ) {
				if ( onR < onW )
					fill(ONQ, onR, onW+1, null);
				else if ( onR > onW )
					fill(ONQ, onW, onR+1, null);
				if ( offR < offW )
					fill(OFFQ, offR, offW+1, null);
				else if ( offR > offW )
					fill(OFFQ, offW, offR+1, null);
			}
		}
		return null;
	}

	// construct a new On consequence with hiddenParents
	private Ass on(final int indice, final int value, final Ass parent
			, final int rti, final int rPlaces, final int[] rIndices) {
		Ass hiddenParent;
		int array[], n, i;
		final Ass effect = new Ass(indice, value, ON, parent, CAUSE[rti], ONLYPOS[rti]);
		for ( array=INDEXES[initCells[indice].regions[rti].places[value] & ~rPlaces], n=array.length, i=0; i<n; ++i )
			if ( (hiddenParent=offNodes[rIndices[array[i]]][value].ass) != null )
				effect.addParent(hiddenParent);
		return effect;
	}

	/**
	 * Implements ICleanUp.
	 */
	@Override
	protected final void cleanUpImpl() {
	}

}
