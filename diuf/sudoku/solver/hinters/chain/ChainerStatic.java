/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.Cause.*;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.hinters.ICleanUp;
import diuf.sudoku.solver.hinters.IFoxyHinter;
import static diuf.sudoku.solver.hinters.chain.AChainerBase.*;
import diuf.sudoku.solver.hinters.chain.LinkedMatrixAssSet.Node;
import java.util.Arrays;

/**
 * ChainerMulti implements only the {@link Tech#MultipleChain} Sudoku solving
 * technique. The other chaining techniques are implemented by other chainers.
 * <pre>
 * I search the Grid for static multiple XY forcing chains.
 * * Static means the grid does NOT change: Off Assumptions are NOT erased.
 * * Multiple means more than two maybes. I start chains from cells having
 *   three-or-more maybes. This fills a whole in Unary and Dynamic chains,
 *   which start from bivalue cells (ie cells with two maybes).
 * * X-links are based on region places.
 *   * An On has every other place for value in each cell.regions Offed.
 *   * An Off causes an On only if there are two places for value in any
 *     cell.region: the other place is On.
 * * Y-links are based on cell maybes.
 *   * An On has every other cell.maybe Offed.
 *   * An Off causes an On only if this cell has two maybes:
 *     the other maybe is On.
 * * A Forcing Chain is the consequences of an initial-assumption.
 *   Chaining finds hints in those consequences using:
 *   * Contradiction: anAss causes a cell-value to be both an On and Off, hence
 *     anAss is proven false, which is always nice, especially in parliament.
 *   * Cell Reduction: all maybes have the same consequence,
 *     hence that consequence is proven true.
 *   * Region Reduction: all places have the same consequence,
 *     hence that consequence is proven true.
 * </pre>
 * <p>
 * <pre>
 * KRC 2019-11-01 I have split the existing Chainer class into ChainerMulti
 * and UnaryChainer. See UnaryChainer comments for more details.
 * </pre>
 */
public final class ChainerStatic extends AChainerBase implements ICleanUp, IFoxyHinter {

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

	// We create all the following collections ONCE in my first execution.
	// They would be final except most Nested* hinters are never executed,
	// hence creating many 9k LinkedMatrixAssSets is a waste of RAM.
	// NB: LinkedMatrixAssSet constructor takes 19,549 nanos (9,270 bytes)
	// all On/Off consequences of anOn/Off // doBinaryChains, doRegionChains
	// NOTE: to* are now used as both onTo* and offTo*. No need for extra pair!
	public LinkedMatrixAssSet toOns, toOffs;
	// the On/Off consequences of sibling cell being v // doRegionsChains
	private LinkedMatrixAssSet sibOns, sibOffs;
	// the On and Off consequences of all maybes of a cell
	private LinkedMatrixAssSet cellOns, cellOffs;
	// the On/Off consequences in this region // doRegionsChains
	private LinkedMatrixAssSet rgnOns, rgnOffs;
	// the On/Off consequences of each place in region // doRegionsChains
	private LinkedMatrixAssSet[] posOns, posOffs;
	// the On/Off consequences of each cell.maybe // doRegionsChains
	private LinkedMatrixAssSet[] valOns, valOffs;

	/**
	 * One below my minimum cell.size. Normally 2, else 1 when UnaryChain is
	 * unwanted, to pick-up his hints. If you REALLY don't want UnaryChain then
	 * don't use chaining, by which I really mean stick to easy puzzles, or use
	 * another free Sudoku solver. HoDoKu is great: better GUI than this crap.
	 * GUI's aren't my thing.
	 */
	private int cellSizeFloor;

	/**
	 * Has this hinter been initialised, ie have sets been constructed.
	 */
	private boolean isInitialised;

	/**
	 * Construct a new ChainerMultiple
	 */
	public ChainerStatic() {
		this(false);
	}

	/**
	 * Actual Constructor.
	 *
	 * @param useCache normally true; false when imbedded and in test-cases
	 */
	protected ChainerStatic(final boolean useCache) {
		super(Tech.StaticChain, useCache);
		cellSizeFloor = CFG.getBoolean(Tech.UnaryChain.name()) ? 2 : 1;
	}

	/**
	 * create all my sets
	 */
	@Override
	public void initialise() {
		if ( isInitialised )
			return;
		int i;
		toOns  = new LinkedMatrixAssSet();
		toOffs = new LinkedMatrixAssSet();
		sibOns  = new LinkedMatrixAssSet();
		sibOffs = new LinkedMatrixAssSet();
		rgnOns  = new LinkedMatrixAssSet();
		rgnOffs = new LinkedMatrixAssSet();
		cellOns  = new LinkedMatrixAssSet();
		cellOffs = new LinkedMatrixAssSet();
		posOns  = new LinkedMatrixAssSet[REGION_SIZE];
		i=0; do posOns[i] = new LinkedMatrixAssSet(); while(++i<REGION_SIZE);
		posOffs = new LinkedMatrixAssSet[REGION_SIZE];
		i=0; do posOffs[i] = new LinkedMatrixAssSet(); while(++i<REGION_SIZE);
		valOns  = new LinkedMatrixAssSet[VALUE_CEILING];
		i=1; do valOns[i] = new LinkedMatrixAssSet(); while (++i<VALUE_CEILING);
		valOffs = new LinkedMatrixAssSet[VALUE_CEILING];
		i=1; do valOffs[i] = new LinkedMatrixAssSet(); while (++i<VALUE_CEILING);
		isInitialised = true;
	}

	/**
	 * destroy all my sets
	 */
	@Override
	public void deinitialise() {
		if ( !isInitialised )
			return;
		int i;
		i=0; do posOns[i] = posOffs[i] = null; while(++i<REGION_SIZE);
		i=1; do valOns[i] = valOffs[i] = null; while(++i<VALUE_CEILING);
		posOns = posOffs = valOns = valOffs = null;
		toOns = toOffs = sibOns  = sibOffs = rgnOns  = rgnOffs = cellOns = cellOffs = null;
		isInitialised = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void findChainHints() {
		assert isMultiple && !isDynamic;
		try {
			Ass e;
			int values[], vi, vn, value;
			if ( !isInitialised )
				initialise();
			int indice = 0;
			do {
				// Multiple means > 2 (Unary and Dynamic do > 1), but when
				// UnaryChain is NOT wanted I search cells with size > 1, so
				// we still find UnaryChains, even when UnaryChain not wanted.
				// If you really dont want UnaryChains then dont do chaining,
				// which really means avoiding puzzles requiring chaining.
				if ( VSIZE[maybes[indice]] > cellSizeFloor ) {
					// foreach cell.maybe
					values = VALUESES[maybes[indice]];
					vn = values.length;
					vi = 0;
					value = values[vi];
					// 1) calculate consequences of $cell+$value
					doChains(new Ass(indice, value, ON), toOns, toOffs);
					// 2) region reduction: all places for v in region have
					//    the same effect (using toOn/Offs calc'd above)
					doRegionsChains(indice, value);
					// 3a) collect sets for cell reduction
					boolean anyOns = cellOns.addAll(toOns);
					boolean anyOffs = cellOffs.addAll(toOffs);
					valOns[value].pollAll(toOns); // clears toOns
					valOffs[value].pollAll(toOffs); // clears toOffs
					++vi;
					do {
						value = values[vi];
						// 1) calculate consequences of $cell+$value
						doChains(new Ass(indice, value, ON), toOns, toOffs);
						// 2) region reduction: all places for v in region have
						//    the same effect (using toOn/Offs calc'd above)
						doRegionsChains(indice, value);
						// 3a) collect sets for cell reduction
						if ( anyOns )
							anyOns &= cellOns.retainAll(toOns.nodes);
						anyOffs &= cellOffs.retainAll(toOffs.nodes);
						valOns[value].pollAll(toOns); // clears toOns
						valOffs[value].pollAll(toOffs); // clears toOffs
					} while ( ++vi < vn );
					// 3b) do cell reduction
					if ( anyOns )
						while ( (e=cellOns.poll()) != null )
							hints.add(cellReduction(indice, e, valOns, 0));
					if ( anyOffs )
						while ( (e=cellOffs.poll()) != null )
							hints.add(cellReduction(indice, e, valOffs, 1));
					vi = 0;
					do {
						value = values[vi];
						valOns[value].clear();
						valOffs[value].clear();
					} while ( ++vi < vn );
					hinterrupt();
				}
			} while (++indice < GRID_SIZE );;
		} finally {
			if ( !isEmbedded && isInitialised )
				deinitialise();
		}
	}

	/**
	 * Find the consequences of the initial assumption in the grid.
	 *
	 * @param a the initial assumption, either an On or an Off.
	 * @param ons OUT the On consequences of initAss.
	 * @param offs OUT the Off consequences of initAss.
	 * @return first contradiction: an On and an Off (ordered). <br>
	 *  anAss causes both On and Off of a cell-value, which is absurd, hence
	 *  the initial assumption is proven false. <br>
	 *  For example: A1+1 -&gt H6+6 and H6-6, hence A1 cannot be 1. <br>
	 *  null means that no such contradiction exists. This is complete logic,
	 *  so null doesnt just mean not found, it means does not exist.
	 */
	public Ass[] doChains(
		  Ass a // IN Initial Assumption
		, final LinkedMatrixAssSet ons // OUT
		, final LinkedMatrixAssSet offs // OUT
	) {
		ARegion cRegion; // one of the a.cell.regions
		Cell cell; // a.cell
		int indices[] // region.indices
		  , cPlaceIn[] // cell.placeIn
		  , array[], n, i // array, size, index (ONE stackframe)
		  , value // a.value
		  , indice // a.cell.indice, hijacked as sibling index
		  , other // two uses: otherValue or otherIndice
		;
		boolean earlyExit = true; // set true at end-of-method, else cleanUp
		int onR=0, onW=0, offR=0, offW=0; // ONQ/OFFQ read/write index
		final Node[][] onNodes = ons.nodes;
		final Node[][] offNodes = offs.nodes;
		assert ons.isEmpty();
		assert offs.isEmpty();
		// add the initial assumption to the appropriate Set
		if ( a.isOn )
			ons.add(a);
		else
			offs.add(a);
		try {
			do {
				value = a.value;
				cell = cells[indice=a.indice];
				if ( a.isOn ) { // every On has Off effect/s, most have many
					// (1) Y-Link: other maybes of this cell are Off
					for ( array=VALUESES[maybes[indice] & ~VSHFT[value]], n=array.length, i=0; i<n; ++i ) {
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice][other=array[i]] != null ) {
							return new Ass[] {
								onNodes[indice][other].ass
							  , new Ass(indice, other, OFF, a, NakedSingle, ONEVALUE)
							};
						} else if ( offNodes[indice][other] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, other, OFF, a, NakedSingle, ONEVALUE));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
					// (2) X-Link: other places for value are Off
					cPlaceIn = cell.placeIn;
					indices = (cRegion=cell.box).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[BOX]], n=array.length, i=0; i<n; ++i ) {
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice=indices[array[i]]][value] != null ) {
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX])
							};
						} else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
					indices = (cRegion=cell.row).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[ROW]], n=array.length, i=0; i<n; ++i ) {
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice=indices[array[i]]][value] != null ) {
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW])
							};
						} else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
					indices = (cRegion=cell.col).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[COL]], n=array.length, i=0; i<n; ++i ) {
						// check Contradiction: On and Off, so initAss is false
						if ( onNodes[indice=indices[array[i]]][value] != null ) {
							return new Ass[] {
								onNodes[indice][value].ass
							  , new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL])
							};
						} else if ( offNodes[indice][value] == null ) {
							OFFQ[offW] = offs.justAdd(new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL]));
							offW = (offW + 1) & OFF_MASK;
							assert offW != offR;
						}
					}
				} else { // ass is an Off: ~30% of Offs cause an On
					// (1) Y-Link: if cell has 2 maybes the other one is On
					if ( cell.size == 2 ) {
						other = VFIRST[cell.maybes & ~VSHFT[value]];
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice][other] != null ) {
							return new Ass[] {
								  new Ass(indice, other, ON, a, NakedSingle, ONLYVALUE)
								, offNodes[indice][other].ass
							};
						} else if ( onNodes[indice][other] == null ) {
							ONQ[onW] = ons.justAdd(new Ass(indice, other, ON, a, NakedSingle, ONLYVALUE));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					// (2) X-Link: if region has 2 places then other one is On
					if ( (cRegion=cell.box).numPlaces[value] == 2 ) {
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[BOX]]];
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice][value] != null ) {
							return new Ass[] {
								  new Ass(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX])
								, offNodes[indice][value].ass
							};
						} else if ( onNodes[indice][value] == null ) {
							ONQ[onW] = ons.justAdd(new Ass(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					if ( (cRegion=cell.row).numPlaces[value] == 2 ) {
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[ROW]]];
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice][value] != null ) {
							return new Ass[] {
								  new Ass(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW])
								, offNodes[indice][value].ass
							};
						} else if ( onNodes[indice][value] == null ) {
							ONQ[onW] = ons.justAdd(new Ass(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
					if ( (cRegion=cell.col).numPlaces[value] == 2 ) {
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[COL]]];
						// check Contradiction: On and Off, so initAss is false
						if ( offNodes[indice][value] != null ) {
							return new Ass[] {
								  new Ass(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL])
								, offNodes[indice][value].ass
							};
						} else if ( onNodes[indice][value] == null ) {
							ONQ[onW] = ons.justAdd(new Ass(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]));
							onW = (onW + 1) & ON_MASK;
							assert onW != onR;
						}
					}
				}
				// process all Ons before any Off
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
			// cleanUp upon Contradiction, or Exception
			if ( earlyExit ) {
				if ( onR < onW )
					Arrays.fill(ONQ, onR, onW+1, null);
				else if ( onR > onW )
					Arrays.fill(ONQ, onW, onR+1, null);
				if ( offR < offW )
					Arrays.fill(OFFQ, offR, offW+1, null);
				else if ( offR > offW )
					Arrays.fill(OFFQ, offW, offR+1, null);
			}
		}
		return null;
	}

	/**
	 * Do Region Chaining: A region chain occurs when all possible positions
	 * of a value have the same consequence.
	 * <p>
	 * EG: No matter where we put the 5 in box 4 then F6 cannot be 2.
	 * <p>
	 * 444 calls/puzzle for first 10 in top1465.d2, ie not too many, however
	 * this is still the slowest/heaviest method in the solve process.
	 * <p>
	 * The only reason the 2 "sib" Sets (and effects) are passed-in is
	 * efficiency: We avoid creating 2 {@code LinkedMatrixAssSet}s (a slow
	 * process) for each potential value of each Cell (mathematically max 1,152
	 * Sets per call to getMultipleDynamicChains). Instead, we create the 2
	 * Sets ONCE and clear them as required, which is a bit faster. They are
	 * not intended for use outside of this method.
	 * <p>
	 * In the params:<br>
	 * <b>IN</b> means s__t to search. <br>
	 * <b>OUT</b> means this collection is populated for external reference. <br>
	 * <b>MY</b> means not for external reference.
	 *
	 * @param c IN the Cell of the initial On assumption (anOn).
	 * @param v IN the value of the initial On assumption (anOn).
	 */
	private void doRegionsChains(final int indice, final int v) {
		Ass a; // current Assumption
		ARegion cRegions[], region; // cell.regions, one of
		int indices[] // region.indices
		  , ri // cRegions index
		  , places[], pn, pi, place; // array, size, index, value
		boolean anyOns, anyOffs; // rgnOns/Offs any?
		boolean earlyExit = true; // set false at end, except upon exception
		try {
			cRegions = cells[indice].regions;
			ri = 0;
			do { // box/row/col
				// nb: faster, I think, to not imbed this in next line.
				// I think it runs out of registers. Wish I could KNOW.
				// I want an IDE optimiser tool that shows the machine.
				region = cRegions[ri];
				places = INDEXES[region.places[v]];
				// if region has 2-or-more places for v
				if ( (pn=places.length) > 1
				  // and we are seeing this region for the first time,
				  // ie examine each region-value once (from first cell),
				  // to avert examining whole region foreach place therein,
				  // which wastes time producing repeat hints (no new ones)
				  && region.indices[place=places[0]] == indice
				) {
					// nb: faster, I think, to not imbed this in previous line.
					// I think its running out of registers again.
					indices = region.indices;
					// for the first place for value in this region (i)
					// nb: pre-populated onToOns/Offs dont change here-in
					rgnOns.addAll(toOns);
					rgnOffs.addAll(toOffs);
					posOns[place].addAll(toOns);
					posOffs[place].addAll(toOffs);
					anyOns = anyOffs = true; // stop when both false
					// foreach subsequent place for value in this region
					// note that j is the-index-of-the-index i
					pi = 1;
					do {
						place = places[pi];
						// get effects of cell being value in sibOns/Offs
						doChains(new Ass(indices[place], v, ON), sibOns, sibOffs);
						if ( anyOns && (anyOns=rgnOns.retainAllAny(sibOns)) )
							posOns[place].pollAll(sibOns);
						else
							sibOns.clear();
						if ( anyOffs &= rgnOffs.retainAllAny(sibOffs) )
							posOffs[place].pollAll(sibOffs);
						else
							sibOffs.clear();
						assert sibOns.isEmpty() && sibOffs.isEmpty();
					} while ( ++pi < pn );
					// turn any surviving region Ons into hints.
					if ( anyOns )
						while( (a=rgnOns.poll()) != null )
							hints.add(regionReduction(region, v, a, posOns));
					// turn any surviving region Offs into hints.
					if ( anyOffs )
						while( (a=rgnOffs.poll()) != null )
							hints.add(regionReduction(region, v, a, posOffs));
					// clear posOns/posOffs // be kind to the GC
					pi = 0;
					do {
						place = places[pi];
						posOns[place].clear();
						posOffs[place].clear();
					} while ( ++pi < pn );
				}
			} while ( ++ri < 3 );
			earlyExit = false;
		} finally {
			// cleanUp after UnsolvableException
			if ( earlyExit ) {
				sibOns.clear();
				sibOffs.clear();
				rgnOns.clear();
				rgnOffs.clear();
				place = 0;
				do {
					posOns[place].clear();
					posOffs[place].clear();
				} while (++place < REGION_SIZE);
			}
		}
	}

	/**
	 * Implements ICleanUp.
	 */
	@Override
	protected final void cleanUpImpl() {
	}

}
