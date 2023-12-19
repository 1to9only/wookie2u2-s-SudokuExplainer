/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.Cause.CAUSE;
import static diuf.sudoku.Ass.Cause.NakedSingle;
import static diuf.sudoku.Ass.ON_BIT;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import java.util.Deque;
import java.util.LinkedList;
import static diuf.sudoku.Ass.ASHFT;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Idx.MASKED;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.solver.hinters.IFoxyHinter;
import static diuf.sudoku.solver.hinters.chain.AssAnc.ANCMASKOF;
import static diuf.sudoku.solver.hinters.chain.AssAnc.ANCVBASE;

/**
 * ChainerUnary implements da {@link Tech#UnaryChain} Sudoku solving technique.
 * I search the grid for static unary forcing chains and bidirectional cycles.
 * "Unary" means single value. My chains are static, ergo the grid does NOT
 * change. Each chain-link that I see is already in the given grid. All Forcing
 * Chains involve following the implications of an initial assumption that a
 * cell is, or is not, a value.
 * <pre>
 * <b>Forcing chains in a nutshell.</b>
 * To follow a forcing chain, we start from the initial assumption, "ass".
 * We find the direct effects of ass:
 *   if ass is an On then
 *     Y) each other maybe of this cell is Off
 *     X) foreach of this cells three regions: each other place for this
 *        value in this region is Off
 *   else (ass is an Off)
 *     Y) if cell has two maybes then the other maybe is On
 *     X) foreach of cells three regions: if region has two places for
 *        value then the other place is On
 * Then each of those effects becomes the ass to examine... and so-on...
 * exhaustively, following the whole consequence-tree.
 *
 * Basically, Y-Links are on cell maybes. X-Links are on places is regions.
 * For efficiency, my chainers always explore both link-types; then "XY-ness"
 * of a chain is determined after the fact. Every On causes an Off, typically
 * several. Only about a third of Offs cause an On, rarely more than one. Hence
 * the algorithm stops when the Offs dont cause any more Ons.
 * </pre>
 * <p>
 * I follow the consequences of assuming that a Cell is a value through cells
 * with two possible values, and regions with two places for a value; which I
 * call bivalue cells and biplaced regions. This is the simplest (and therefore
 * notionally the fastest) chaining technique, and so ChainerUnary comments
 * include explanations (the other chainers not-so-much).
 * <p>
 * A Bidirectional Cycle occurs when the initial assumption causes itself.
 * A Bidirectional Cycle is a series of regions with two places for value that
 * can (like coloring) eliminate external occurrences seeing both sets. Cycles
 * are only occasional post-coloring, any mostly non-orthogonal (ie "bent"),
 * but all four coloring hinters still miss some hints that ChainerUnary finds,
 * and ChainerUnary is less-than a tenth of the code. Keep it simple stupid!
 * <p>
 * <pre>
 * KRC 2019-11-01 Split Chainer in two: ChainerUnary and ChainerMulti; leaving
 * the shared methods, like offToOns and onToOffs, in abstract AChainerBase. I
 * have done this to distinguish between the two mostly disjunct techniques.
 * Focus makes code easier to follow, but its architecturally more complex.
 * Ye never get nothing for nothing.
 * Dynamic and Unary chains have two disjunct sets of "upper level" methods;
 * they really only share the onToOff and offToOn methods, plus a few helper
 * methods; which sounds like inheritance to me. I find this design easier to
 * follow, but its still a bitch!
 *
 * KRC 2023-07-12 FASTARDISED: doCycle and doUnary are now one-piece methods
 * which do not call anything, except to add to collections. A bit faster.
 * PRE: 7,442,864,800  3947  1,885,701  341  21,826,582 UnaryChain
 * PST: 5,983,435,200  3947  1,515,945  341  17,546,730 UnaryChain
 * </pre>
 */
public final class ChainerUnary extends AChainerBase implements IFoxyHinter {

	// a circular-array instead of a Queue.
	// 1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768 65536
	private static final int Q_SIZE = 128; // must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;

	// clear an ons or offs array
	private static void clear(final AssAnc[][] array) {
		for ( int v,i=0; i<GRID_SIZE; ++i )
			for ( v=1; v<VALUE_CEILING; ++v )
				array[i][v] = null;
	}

	// Creating Collections slow, so do ONCE in initialise on first findChains,
	// coz I'm imbedded in many Nested* chainers which are then NOT called.
	// This saves some RAM. SE's memory management ethos is "just be greedy".
	// This somewhat alleviates a large downside: running out of RAM. We will
	// run out of RAM eventually, forcing a total rewrite. Question is can we
	// draw inside the lines? Don't do this commercially! You will make your
	// boss angry. My risk appetite is larger because I'm just ____ing around
	// with s__t, for fun. If the whole app ____ing bombs its really no skin
	// off anyones nose. They're only Sudoku puzzles. A nice test-bed.
	private AssAnc[] Q;

	// The ons (and offs) are filed by [indice][value] in a "plain" array.
	// These arrays where LinkedMatrixAssSets, but ChainerUnary doesn't use the
	// extra features provided by a class, and its faster to do it all natively
	// mainly because a native operation comes without the cost of invoking ANY
	// method, especially add, because add happens so often.
	// NOTE WELL: ChainerUnary's speed comes mostly from reduced stackwork.
	private AssAnc[][] ons, offs;

	/**
	 * ons and offs eliminated by the cycle.
	 * @see #cycleHint
	 */
	private LinkedMatrixAssSet cycleOns, cycleOffs;

	// are the two link-types present in this chain?
	private boolean yLink, xLink;

	/**
	 * Has this hinter been initialised.
	 */
	private boolean isInitialised;

	/**
	 * Actual Constructor.
	 *
	 * @param useCache normally true, false when imbedded and in test-cases
	 */
	ChainerUnary(final boolean useCache) {
		super(Tech.UnaryChain, useCache);
	}

	/**
	 * Constructor.
	 */
	public ChainerUnary() {
		this(true);
	}

	// Each LMAS is 2k of RAM (hence is slow to construct) tied-up in imbedded
	// NestedUnary and NestedMultiple hinters which are then never called, so
	// we delay creating sets until first invocation of findChains, which does
	// not happen in Nested* hinters, until you Shft-F5 in the GUI.
	// Ideally these sets would be final, but its all-good so long as we treat
	// them as if they were final. Clear them, instead of nulling the whole.
	@Override
	public void initialise() {
		if ( isInitialised )
			return;
		Q = new AssAnc[Q_SIZE];
		ons = new AssAnc[GRID_SIZE][VALUE_CEILING];
		offs = new AssAnc[GRID_SIZE][VALUE_CEILING];
		cycleOns = new LinkedMatrixAssSet();
		cycleOffs = new LinkedMatrixAssSet();
		isInitialised = true;
	}

	@Override
	public void deinitialise() {
		if ( !isInitialised )
			return;
		Q = null;
		ons = offs = null;
		cycleOns = cycleOffs = null;
		isInitialised = false;
	}

	/**
	 * findChains is called by supers getHints method to find chain hints in
	 * grid, which I add to the hints cache. ChainerUnary finds bidirectional
	 * cycles (doCycle), and unary value forcing chains (doUnary). A forcing
	 * chain hint is the result of following the consequences of an initial
	 * assumption that a cell is/not a value. A bidirectional cycle is when
	 * the initial assumption causes itself.
	 * <p>
	 * I could go on ad-nausea. Brevity is golden. More details are discussed
	 * as they become relevant. Next {@link #doCycle}, then {@link #doUnary}.
	 */
	@Override
	protected void findChainHints() {
		try {
			AssAnc anOn; // an Ass with ancestors: Fully Liberal!
			int values[], n, i, v;
			if ( !isInitialised )
				initialise();
			int indice = 0;
			do
				if ( sizes[indice] > 1 ) {
					values = VALUESES[maybes[indice]];
					n = values.length;
					i = 0;
					do {
						v = values[i];
						anOn = new AssAnc(indice, v, ON);
						doCycle(anOn);
						doUnary(anOn);
						doUnary(new AssAnc(indice, v, OFF));
					} while (++i < n);
					hinterrupt();
				}
			while (++indice < GRID_SIZE);
		} finally {
			if ( !isEmbedded && isInitialised )
				deinitialise();
		}
	}

	/**
	 * Find Bidirectional Cycles in the Grid starting from anOn.
	 * <p>
	 * A Cycle starts (and ends) with an On assumption that causes itself. The
	 * cells form a bidirectional cycle: theres exactly two ways of placing dis
	 * value in these cells, forming two different possible configurations. The
	 * value appears in a region regardless of which configuration turns out to
	 * be correct. Because one of the two configurations must be correct, other
	 * occurrences of this value can be removed from effected regions.
	 * <p>
	 * Mind why this is a BFS and works. I learned that cycles are only found
	 * by DFS. Maybe we are missing loops? ~NJ.
	 * <p>
	 * The ons and offs sets are cleared in my finally block. Every doCycle and
	 * doUnary MUST start with empty ons and offs, and post-cleanUp is neater.
	 * <p>
	 * <pre>
	 * FASTARDISATIONS:
	 * This method is fully fastardised. One way to faster code (as apposed to
	 * a faster algorithm) is reducing stackwork. A "tight loop" creates no new
	 * variables, and preferably calls no methods. All my vars are predeclared
	 * (ANSII-C style) for ONE stackframe. This includes iterator vars, as in
	 * "for(int i:iterable)", which is faster, but is messy, by which I mean
	 * excessively complicated. All such loops are pretested, coz the source
	 * arrays are empty in the Nested* hinters only (post-testing is faster for
	 * non-empty arrays).
	 *
	 * I inlined the "hasAncestor" query on {@link AssAnc#ancestors} using a
	 * fast-O(1) bitwise operation. This comes at the expense of creating and
	 * populating the ancestors array in AssAnc's constructor, but atleast
	 * array.clone is faster than new array. We hasAncestor slightly more often
	 * than we new AssAnc, so it works-out just marginally faster. Sigh.
	 *
	 * Construction of a new AssAnc is delayed until I know that its required,
	 * thus eliminating construction of garbage objects, to reduce GC.
	 *
	 * Cycle detection is optimised by pre-calculating the Off hashCode just to
	 * save ^ON_BIT on every test. This is REALLY confusing til you understand
	 * it, when it becomes merely annoying. Its a wet sock. Pull it on.
	 *
	 * The ass-loop is post-tested coz there is always a first ass.
	 *
	 * The rti (regionTypeIndex) loops are likewise post-tested, and the (&gt;)
	 * greater-than operator is fast, and a "++i" pre-increment is faster than
	 * a "i++" post-increment, hence "if(++rti > 2) break;" which I think is as
	 * fast as possible, though I recognise that I could be totes-wrong. I lack
	 * the brains to tackle a mathematical proof.
	 *
	 * The Q (the AssAnc[] queue) is an endless-array, ie just an array with
	 * indexes that wrap-around (go back-to 0) when they reach end-of-queue.
	 * This is faster than a Queue (or Deqeue) mostly because its all native,
	 * hence involves no method calls.
	 *
	 * onToOff and offToOn got axed to reduce stackwork. FFS!
	 *
	 * The order in which variables appear, and whether or not reading them is
	 * inlined, is intended to optimise the use of registers and the CPU cache.
	 * Basically, the more recently you set the bastard, the faster its likely
	 * to be to read the bastard, until something else uses that register, in
	 * which case the CPU cache yields second-order-fastness, then when your
	 * var falls out of the CPU cache the next read is fully-slow again. This
	 * limits the number of variables you can use in a fast code block.
	 *
	 * The decision whether to "(x=array[i])" inline a var-set or not is totes
	 * black-box to me. I performance test, and fiddle, and retest to find a
	 * fastish set-up. Netbeans profiler vagaries mean I cant know I hit "the
	 * fastest" setup. I haven't discovered principles on which to base these
	 * decisions; so I just ____around with it, and hope for the best. I wish
	 * Netbeans had an "Optimise" tool/menu for this task, et al. Sigh. The
	 * trick is to minimise operations, and maximise register then cache hits.
	 * It might be faster to do more ops if you can keep everything that needs
	 * to be in scope on the registers at-this-time. Going-off the CPU-cache
	 * kills performance, plain and simple. I reckon that must be why bitCount
	 * and Collections are so much faster than they look. Either that or the NB
	 * profiler fudges it, to avert complaints to head-orifice.
	 *
	 * Finding REAL performance is hard in Java! Just my humble opinion.
	 * </pre>
	 *
	 * @param a the initial On assumption. Note that working assumptions in
	 *  ChainerUnary are AssAnc, with the ancestors field, but the hints dont
	 *  use the ancestors field, hence there reference-type is plain Ass, and
	 *  the hint-methods create plain Asses.
	 * @return any hint/s found
	 */
	public boolean doCycle(AssAnc a) {
		assert a.isOn; // cycles initial assumption is an On
		Cell cell; // grid.cells[ass.indice]
		ARegion cRegion; // one of the ass.cell.regions
		int ancestors[] // ass.ancestors
		  , indices[] // region.indices
		  , cPlaceIn[] // cell.placeIn
		  , array[], n, i // array, size, index (ONE stackframe)
		  , value // ass.value
		  , indice // ass.cell.indice, hijacked as sibling-cells-indice
		  , other // two uses: otherValue and otherIndice
		;
		boolean result = false;
		int qr=0, qw=0; // Q read/write index
		// anOn.hashCode (identity of initialOn) to avert tangles quickly.
		// nb: Use the Offs hashCode to save ^ON_BIT on every test, for speed.
		final int initHC = a.hashCode ^ ON_BIT;
		try {
			ons[a.indice][a.value] = a;
			do {
				value = a.value;
				cell = cells[indice=a.indice];
				if ( a.isOn ) {
					// (1) Y-Link: other maybes of this cell get Offed.
					ancestors = a.ancestors;
					for ( array=VALUESES[cell.maybes & ~VSHFT[value]], n=array.length, i=0; i<n; ++i )
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[other=array[i]]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][other] = new AssAnc(indice, other, OFF, a, NakedSingle, ONEVALUE);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					// (2) X-Link: other places for value get Offed.
					cPlaceIn = cell.placeIn;
					indices = (cRegion=cell.box).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[BOX]], n=array.length, i=0; i<n; ++i )
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice=indices[array[i]]]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][value] = new AssAnc(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					indices = (cRegion=cell.row).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[ROW]], n=array.length, i=0; i<n; ++i )
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice=indices[array[i]]]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][value] = new AssAnc(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					indices = (cRegion=cell.col).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[COL]], n=array.length, i=0; i<n; ++i )
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice=indices[array[i]]]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][value] = new AssAnc(indice, value, OFF, a, CAUSE[COL], ONETIME[COL]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
				} else {
					// (1) Y-Link: if cell has two maybes then other value is On.
					if ( cell.size == 2 ) {
						// detect cycle: initialOn causes itself
						if ( (ASHFT[other=VFIRST[cell.maybes & ~VSHFT[value]]]^indice) == initHC ) {
							AChainingHint hint = cycleHint(new Ass(indice, other, ON, a, NakedSingle, ONLYVALUE));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][other] == null ) {
							Q[qw] = ons[indice][other] = new AssAnc(indice, other, ON, a, NakedSingle, ONLYVALUE);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					// (2) X-Link: if region has two places then other place is On.
					// foreach region in cell.regions: box, row, col
					if ( (cRegion=cell.box).numPlaces[value] == 2 ) {
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[BOX]]];
						// detect cycle: initialOn causes itself
						if ( (indice^ASHFT[value]) == initHC ) {
							AChainingHint hint = cycleHint(new Ass(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][value] == null ) {
							Q[qw] = ons[indice][value] = new AssAnc(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					if ( (cRegion=cell.row).numPlaces[value] == 2 ) {
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[ROW]]];
						// detect cycle: initialOn causes itself
						if ( (indice^ASHFT[value]) == initHC ) {
							AChainingHint hint = cycleHint(new Ass(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][value] == null ) {
							Q[qw] = ons[indice][value] = new AssAnc(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					if ( (cRegion=cell.col).numPlaces[value] == 2 ) {
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[COL]]];
						// detect cycle: initialOn causes itself
						if ( (indice^ASHFT[value]) == initHC ) {
							AChainingHint hint = cycleHint(new Ass(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][value] == null ) {
							Q[qw] = ons[indice][value] = new AssAnc(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
				}
				a=Q[qr]; Q[qr]=null; qr=(qr+1)&Q_MASK;
			} while (a != null);
		} finally {
			clear(ons);
			clear(offs);
		}
		return result;
	}

	/**
	 * Follow the forcing chains from the given "on" or "off" assumption.
	 * <p>
	 * I find unary (single value) contradiction forcing chain hints. There is
	 * no binary reduction in unary chains.
	 * <p>
	 * The ons and offs sets are cleared in my finally block. Every doCycle and
	 * doUnary MUST start with empty ons and offs, and its neater to clean-up
	 * after processing.
	 *
	 * @param a an "On" or an "Off" assumption to follow. Note that working
	 *  assumptions in ChainerUnary are AssAnc, with the ancestors field, but
	 *  the hints require Ass only (no ancestors)
	 * @return any hint/s found
	 */
	public boolean doUnary(AssAnc a) {
		boolean result = false;
		ARegion cRegion; // one of the ass.cell.regions
		Cell cell; // cells[ass.indice]
		int indices[] // region.indices
		  , cPlaceIn[] // cell.placeIn
		  , ancestors[] // ass.ancestors
		  , array[], n, i // array, size, index (ONE stackframe)
		  , value // ass.value
		  , other // two uses: otherValue or otherIndice
		  , indice // indice of ass.cell, hijacked as sibling indice
		;
		int qr=0, qw=0; // Q read/write index
		// hashCode of contradiction of anAss, for fast testing
		final int contraHC = a.hashCode ^ ON_BIT;
		try {
			if ( a.isOn )
				ons[a.indice][a.value] = a;
			else
				offs[a.indice][a.value] = a;
			do {
				value = a.value;
				cell = cells[indice=a.indice];
				if ( a.isOn ) {
					ancestors = a.ancestors;
					// (1) Y-Link: all other maybes of this cell are Off
					for ( array=VALUESES[cell.maybes & ~VSHFT[value]],n=array.length,i=0; i<n; ++i ) {
						// detect contradiction
						if ( (ASHFT[other=array[i]]^indice) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, other, OFF, a, NakedSingle, ONEVALUE));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[other]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][other] = new AssAnc(indice, other, OFF, a, NakedSingle, ONEVALUE);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					// (2) X-Link: all other places for value in this cells
					//             three regions are Off
					cPlaceIn = cell.placeIn;
					indices = (cRegion=cell.box).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[BOX]],n=array.length,i=0; i<n; ++i ) {
						// detect contradiction
						if ( (ASHFT[value]^(indice=indices[array[i]])) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][value] = new AssAnc(indice, value, OFF, a, CAUSE[BOX], ONETIME[BOX]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					indices = (cRegion=cell.row).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[ROW]],n=array.length,i=0; i<n; ++i ) {
						// detect contradiction
						if ( (ASHFT[value]^(indice=indices[array[i]])) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][value] = new AssAnc(indice, value, OFF, a, CAUSE[ROW], ONETIME[ROW]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					indices = (cRegion=cell.col).indices;
					for ( array=INDEXES[cRegion.places[value] & ~cPlaceIn[COL]],n=array.length,i=0; i<n; ++i ) {
						// detect contradiction
						if ( (ASHFT[value]^(indice=indices[array[i]])) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, value, OFF, a, CAUSE[COL], ONETIME[COL]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: ignore effect if it caused ass
						if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) {
							Q[qw] = offs[indice][value] = new AssAnc(indice, value, OFF, a, CAUSE[COL], ONETIME[COL]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
				} else {
					// (1) Y-Link: if cell has two maybes then the other value
					//     is On
					if ( cell.size == 2 ) {
						// the other maybe
						other = VFIRST[cell.maybes & ~VSHFT[value]];
						// detect contradiction
						if ( (ASHFT[other]^indice^ON_BIT) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, other, ON, a, NakedSingle, ONLYVALUE));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][other] == null ) {
							Q[qw] = ons[indice][other] = new AssAnc(indice, other, ON, a, NakedSingle, ONLYVALUE);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					// (2) X-Link: if any of this cells three regions has two
					//     places for this value then the other place is On
					if ( (cRegion=cell.box).numPlaces[value] == 2 ) {
						// the other place for value in region
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[BOX]]];
						// detect contradiction
						if ( (indice^ASHFT[value]^ON_BIT) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][value] == null ) {
							Q[qw] = ons[indice][value] = new AssAnc(indice, value, ON, a, CAUSE[BOX], ONLYPOS[BOX]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					if ( (cRegion=cell.row).numPlaces[value] == 2 ) {
						// the other place for value in region
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[ROW]]];
						// detect contradiction
						if ( (indice^ASHFT[value]^ON_BIT) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][value] == null ) {
							Q[qw] = ons[indice][value] = new AssAnc(indice, value, ON, a, CAUSE[ROW], ONLYPOS[ROW]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
					if ( (cRegion=cell.col).numPlaces[value] == 2 ) {
						// the other place for value in region
						indice = cRegion.indices[IFIRST[cRegion.places[value] & ~cell.placeIn[COL]]];
						// detect contradiction
						if ( (indice^ASHFT[value]^ON_BIT) == contraHC ) {
							AChainingHint hint = unaryHint(new Ass(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]));
							if ( hint != null ) {
								hints.add(hint);
								result = true;
							}
						}
						// avert tangle: process each On ONCE
						if ( ons[indice][value] == null ) {
							Q[qw] = ons[indice][value] = new AssAnc(indice, value, ON, a, CAUSE[COL], ONLYPOS[COL]);
							qw = (qw+1) & Q_MASK;
							assert qw != qr;
						}
					}
				}
				a=Q[qr]; Q[qr]=null; qr=(qr+1)&Q_MASK;
			} while (a != null);
		} finally {
			clear(ons);
			clear(offs);
		}
		return result;
	}

	/**
	 * Build a new CycleHint, when the initialOn is found to cause itself. Note
	 * that a cycle starts and ends in the same Assumption.
	 * <p>
	 * My job here is to find the eliminations, if any. Most dont. There are
	 * two ways to place this value in these regions, ie two configurations,
	 * one of which MUST be true, hence each region that contains a cell
	 * <u>in both parities</u> (ie either configuration) is a Locked Set,
	 * eliminating value from external cells that see both parities.
	 * <p>
	 * Most of my task is calculating "in both parities", which boils down to:
	 * Does each region contain a cell in both parities? This is calculated
	 * using two Sets, cycleEvens and cycleOdds. If a region is in both sets
	 * then eliminate vs seeing both cells. The code is a mess, for speed. Soz!
	 *
	 * @param dstOn the destination On Assumption: the start of a cycle, which
	 *  may or (far more likely) may not produce eliminations. Note that hints
	 *  require Ass only (no ancestors)
	 * @return a new CycleHint
	 */
	private CycleHint cycleHint(final Ass dstOn) {
		assert dstOn.isOn;
		assert dstOn.parents.size == 1;
		ARegion r; // a region in cell.regions
		Cell c; // ass.cell
		Ass a; // the current assumption
		LinkedMatrixAssSet parity; // alternately cycleEvens or cycleOdds
		int indices[] // region.indices
		  , placeIn[] // cell.placeIn
		  , array[], n, i // array, size, index
		  , v // ass.value
		;
		// cycle: places bitset of asses in this cycle, by region.
		// nb: my values are 9bit Index bitsets as-per region.places.
		// nb: dstOn (start-of cycle) has ancestors, so post-test for speed.
		// nb: creating an array here costs, but cycleHint is NOT hammered,
		//     so I think its faster overall to not tie-up the RAM.
		final int[] cycle = new int[NUM_REGIONS];
		a = dstOn;
		do {
			placeIn = (c=cells[a.indice]).placeIn;
			cycle[c.box.index] |= placeIn[BOX];
			cycle[c.row.index] |= placeIn[ROW];
			cycle[c.col.index] |= placeIn[COL];
		} while ((a=a.firstParent) != null);
		// add external maybes to odds/evens.
		a = dstOn;
		do {
			if ( a.isOn )
				parity = cycleOns;
			else
				parity = cycleOffs;
			v = a.value;
			c = cells[a.indice];
			indices = (r=c.box).indices;
			for ( array=INDEXES[r.places[v] & ~cycle[r.index]], n=array.length, i=0; i<n; ++i )
				parity.add(new Ass(indices[array[i]], v, OFF));
			indices = (r=c.row).indices;
			for ( array=INDEXES[r.places[v] & ~cycle[r.index]], n=array.length, i=0; i<n; ++i )
				parity.add(new Ass(indices[array[i]], v, OFF));
			indices = (r=c.col).indices;
			for ( array=INDEXES[r.places[v] & ~cycle[r.index]], n=array.length, i=0; i<n; ++i )
				parity.add(new Ass(indices[array[i]], v, OFF));
		} while ((a=a.firstParent) != null);
		cycleOns.retainAll(cycleOffs.nodes);
		cycleOffs.clear();
		if ( cycleOns.isEmpty() )
			return null;
		final Pots reds = new Pots(cycleOns.size());
		while ( (a=cycleOns.poll()) != null )
			reds.upsert(a.indice, VSHFT[a.value], DUMMY);
		final Ass dstOff = reverseCycle(dstOn);
		setLinkTypes(dstOn); // sets yLink and xLink fields
		return new CycleHint(grid, this, reds, yLink, xLink, dstOn, dstOff);
	}

	/**
	 * Reverses the Bidirectional Cycle starting (and ending) with $ass.
	 * <p>
	 * We start with dstOn and we end-up with dstOff: the opposite cycle. Each
	 * Ass is negated, ie Ons become Offs and vice-versa. Each explanation is
	 * moved one up the stack, effectively reversing them. This wont make sense
	 * until you see the two chains side-by-side. Its not just you. Flipping
	 * polarity, order, and explanations is a total head____. It is simple code
	 * that does a complex job.
	 * <pre>
	 * TIPS:
	 * * Use GUI logView to see any specific type of hint: {@code .*Cycle.*}
	 * * To see the two cycles use the view changer ComboBox. The html of each
	 *   specific consequence chain includes the explanations.
	 * * Debug with {@link Ass#pleaseExplain(Ass) }.
	 * * Debug with {@link Ass#toStringChain(Ass) }.
	 * </pre>
	 *
	 * @param targetOn the consequence end (and start) of the cycle to reverse
	 * @return the start (and end) of the new reversed cycle.
	 */
	private Ass reverseCycle(final Ass targetOn) {
		if(targetOn==null) return null;
		final Deque<Ass> stack = new LinkedList<>(); // LastInFirstOut
		Ass a = targetOn;
		String pe = null; // previousExplanation, the first is null
		do {
			// each new ass has null parent, and explanation of previous ass
			// and its added at the start of the stack, ie reverse order.
			stack.addFirst(new Ass(a.indice, a.value, !a.isOn, null, a.cause, pe));
			pe = a.explanation;
		} while ((a=a.firstParent) != null);
		// reassemble parents linked link: each prev takes curr as its parent.
		Ass prev = null; // previous Ass
		for ( Ass curr : stack ) {
			if(prev!=null) prev.addParent(curr);
			prev = curr;
		}
		return stack.getFirst(); // the start of the reversed cycle (dstOff)
	}

	/**
	 * Create a new UnaryChainHint from the given $target Ass.
	 *
	 * @param grid the grid we search (thats the current one, dopey)
	 * @param target the consequence Ass (not the initialOn, dopey)
	 * @return a new UnaryChainHint, or null meaning no eliminations
	 */
	private UnaryChainHint unaryHint(final Ass target) {
		final Pots reds = createRedPots(target);
		if ( reds == null )
			return null;
		setLinkTypes(target);
		return new UnaryChainHint(grid, this, target, reds, yLink, xLink);
	}

	/**
	 * Calculate the yLink and xLink fields, only when hinting, for speed.
	 * <p>
	 * Ass.cause is a fast way to determine each linkType, for speed.
	 * Return as soon as we have seen both types, for speed.
	 * <p>
	 *
	 * @param a the Ass to examine (that's the target ass, dopey; I examine
	 *  his whole lineage, ie his parent, and his parent, and so on...). <br>
	 *  The given ass is NEVER the initialAssumption, always the start of a
	 *  Cycle or Chain, which always has length (ergo parents).
	 */
	private void setLinkTypes(Ass a) {
		// ass is NEVER the initialAssumption
		assert a.firstParent != null;
		yLink = xLink = false;
		// foreach parent EXCEPT the initialAssumption (where cause == null)
		for ( ; a.firstParent!=null; a=a.firstParent ) {
			switch ( a.cause ) {
			case NakedSingle:
				yLink = true;
				if(xLink) return;
				break;
			case HiddenBox:
			case HiddenRow:
			case HiddenCol:
				xLink = true;
				if(yLink) return;
				break;
			}
		}
		// ass is NEVER the initialAssumption, so one of these MUST be set.
		assert yLink || xLink;
	}

	/**
	 * Implement ICleanUp
	 */
	@Override
	protected final void cleanUpImpl() {
	}

}
