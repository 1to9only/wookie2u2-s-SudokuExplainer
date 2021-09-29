/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Ass.Cause.CAUSE_FOR_REGION_TYPE;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.gen.IInterruptMonitor;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintsList;
import diuf.sudoku.solver.hinters.ICleanUp;
import diuf.sudoku.solver.hinters.HintValidator;
import static diuf.sudoku.utils.Frmt.OR;
import static diuf.sudoku.utils.Frmt.PLUS;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.Hash;
import diuf.sudoku.utils.Log;
import java.util.Collection;
import java.util.Iterator;


/**
 * AChainer is an abstract engine for searching a Grid for forcing chains. It
 * is extended by UnaryChainer and by MultipleChainer to implement all Sudoku
 * solving techniques that involve following a chain of implications persueant
 * to an assumption of the value of a Cell within the Grid, including all types
 * of Forcing Chains and Bidirectional Cycles.
 * <p>
 * This code's a bit faster than that which I inherited from Nicolas Juillerat.
 * On my ~2900 Mhz intel i7, a slightly modified Juillerat took 21 minutes and
 * 54 seconds to solve top1465.d5.mt containing 1465 Sudoku puzzles, about 900
 * of which are too hard to solve manually. On 2019-10-12 in ACCURACY Mode the
 * latest solved top1465.d5.mt in 2 minutes and 10 seconds using Juillerats
 * techniques + NakedQuad. So yeah, just a little bit faster.
 * <p>
 * The -REDO switch (which uses only previously successful hinters read from
 * the log-file of a previous run) brought that down to 2 minutes 3 seconds.
 * <p>
 * The -SPEED -STATS switches (which throws all it's toys out of the pram) took
 * 40.8 seconds to solve those same 1465 puzzles, which is ~3.1% of the time.
 * Butyacannobeat Knuths recursive guesser at 992,370,784, ie 1465 puzzles in
 * just under a bloody second. Lick that one Scoobie!
 * <p>
 * Basically no expense is spared to find hints faster here (even correctness
 * takes a hit on occasion) including replacement of Javas HashSet class with
 * LinkedMatrixAssSet, for when a Hash-head is just too slow. My definition of
 * "correct" is: each hint is the simplest available.
 * <p>
 * If you can do it faster then please do so, but if you even read this code
 * then you MUST share your implementation. No secret squirrel business.
 * <p>
 * KRC AUG 2019 The Chainer used to apply the first found hint, it now always
 * finds all hints, and (when one hint is wanted) returns the most effective
 * (highest score) hint. This is slower but it allows us to step through a
 * solution in the GUI following the same path (hints) that LogicalSolverTester
 * used to solve the puzzle, which is exactly what I want. This change has
 * improved my development life immensely, and I find that my perception of the
 * importance of performance continues to reduce over time.
 * <p>
 * KRC 2019 OCT I'm proud of working-out that the fastest way to figure-out if
 * we already know the conjugate is by calculating the conjugates hashCode then
 * just comparing each calculated Ass.hashCode with it, instead of the old way
 * conjugating each calculated Ass, which double-hammers the Ass constructor to
 * create ship-loads of barely-used garbage Ass's, which end-up bogging the GC.
 * <p>
 * KRC 2011-11-01 I've split the existing Chainer class into UnaryChainer and
 * MultipleChainer, leaving the shared methods in this class, which is now
 * abstract, so is now AChainer. I'm doing this because the methods and even
 * fields of Unary and Multiple chaining are disjunct, especially at the upper
 * levels; they really only share the onToOff and OffToOn methods, and the few
 * differences between the two at the bottom levels are handled with call-backs
 * to the two subtypes.
 * <p>
 * This is experiment (FFMS!) worked first time, and it makes the code dryer,
 * and therefore (I hope) a bit easier to follow. A class that's 60% unused in
 * 75% of use-cases is s__t. A class should be focused on solving a specific
 * problem. I just narrowed-down the problem, and simplicity has (I hope, I'm
 * too deep into this s__t to judge) followed. Mind you, using inheritance to
 * make things simpler sounds oxymoronic.
 */
public abstract class AChainer extends AHinter
		implements ICleanUp
{
//	/** If true && VERBOSE_3_MODE then I write stuff to Log.out */
//	public static final boolean IS_NOISY = false; // check false
//
//	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on.
//	 * @param msg */
//	protected static void noiseln(String msg) {
//		if (Log.MODE >= Log.VERBOSE_3_MODE) {
//			Log.format(NL+msg+NL);
//		}
//	}
//
//	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on.
//	 * @param fmt a PrintStream.format format argument
//	 * @param args a PrintStream.format args list */
//	protected static void noisef(String fmt, Object... args) {
//		if (Log.MODE >= Log.VERBOSE_3_MODE) {
//			Log.format(fmt, args);
//		}
//	}

	/** a local shortcut to Hash's LSH8, used in Ass hashCodes. */
	protected static final int[] LSH8 = Hash.LSH8;

	/**
	 * If true the typeID field (\\d) is included in the hint-type.
	 * This is for developers to back-trace where a hint came from,
	 * but it is incomprehensible noise to the end-user,
	 * and interferes with the sorting of hints,
	 * So I must ALWAYS be set back to false for a release build!
	 */
	protected static final boolean WANT_TYPE_ID = false; // @check false

	/** Are multiple cell and region chains sought. Multiple means more than 2
	 * potential values, or more than 2 possible positions. */
	protected final boolean isMultiple;
	/** Is this Chainer removing maybes as it goes? */
	protected final boolean isDynamic;
	/** Is this Chainer looking for Dynamic Contradictions. */
	protected final boolean isNishio;
	/** Is this Chainer imbedded (nested) inside another Chainer? */
	protected final boolean isImbedded;
	/** interruptMonitor.isInterrupted() means please stop-now. */
	protected final IInterruptMonitor interruptMonitor;

	private final HintsList hints = new HintsList();

	/**
	 * Constructs an abstract Chainer: an engine for searching a Sudoku Grid
	 * for forcing chains.
	 * @param tech a Tech with isChainer==true,
	 * @param isImbedded true only when this is an imbedded hinter, ie is nested
	 * inside another hinter.
	 */
	protected AChainer(Tech tech, boolean isImbedded, IInterruptMonitor im) {
		super(tech);
		assert tech.isChainer;
		this.isMultiple	 = tech.isMultiple;
		this.isDynamic	 = tech.isDynamic;
		this.isNishio	 = tech.isNishio;
		this.isImbedded  = isImbedded;
		this.interruptMonitor = im;
		assert degree>=0 && degree<=5;
	}
	protected AChainer(Tech tech) {
		this(tech, false, null); // not imbedded
	}

	/**
	 * Clear out the hints cache.
	 * <p>
	 * This implementation is overridden by both UnaryChainer and
	 * MultipleChainer to call me back, then cleanUp there own s__t.
	 */
	@Override
	public void cleanUp() {
		hints.clear();
	}

	/**
	 * Find any chaining hints in the given grid and add them to hints list.
	 * This method is implemented by my subtypes: UnaryChainer, MultipleChainer
	 * to find the bloody hints already already. It's done this way to allow my
	 * findHints method to cache hints.
	 *
	 * @param grid Grid to search
	 * @param hints HintsList to add hints to.
	 */
	protected abstract void findChainHints(Grid grid, HintsList hints);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This implementation follows forcing chains through the grid to the hints.
	 * <p>
	 * Note that hints are cached locally.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		assert grid != null;
		assert accu != null;

		// first try the existing hints (a hint cache)
		// do NOT cache if I'm an imbedded hinter (buggers-up chaining)
		// I do not understand why/how it buggers-up chaining, all I know is
		// that when I put-in the cache NestedUnary produced invalid hints.
		if ( !isImbedded && hints.size() > 0 ) {
			AHint cached;
			// nb: I poll: so I throw away the "dead wood"; hints are sorted by
			// score (number of eliminations) DESCENDING, so we take the most
			// effective hint (not simplest, but all hints come from the same
			// Chainer, so they're in the same ball park atleast).
			while ( (cached=hints.pollFirst()) != null )
				// if either any elimination in the cached hint is still there
				if ( cached.redPots.anyCurrent()
				  // or cached hint sets a cell which has not yet been set
				  || cached.cell!=null && cached.cell.value==0 ) {
					// -> then return the cached hint so that it is applied,
					accu.add(cached);
					return true; // always, regardless of accu.add's retVal
				}
		}

		// implemented by my subtypes to find the bloody hints.
		findChainHints(grid, hints);

		if ( HintValidator.CHAINER_USES ) {
			// valid nested hints, they've got problems!
			if ( tech.isNested  )
				validateTheHints(grid, hints);
		}

		processFunkyAssSizes();
//		if ( IS_NOISY )
//			noisef("\t\t\t%s %d hints%s", toString(), hints.size(), NL);
		final int n = hints.size();
		if ( n == 0 )
			return false; // we found no hints.
		if ( n == 1 ) {
			// allways just flick-pass a single hint as itself
			accu.add(hints.pollFirst());
			// nb: ignore accu.add's return value because we've already stopped
			// searching, so wether or not caller wants to stop is irrelevant.
			// n==1 so return true, meaning yes we found a hint. Returning
			// accu.add's retval was firing the next chainer until one found
			// multiple hints. Oops! My bad!
			return true; // coz we know n == 1
		}
		if ( accu.isSingle() ) {
			// caller wants one hint, but we're not in SPEED mode, so
			// find the most effective (highest scoring) hint to apply.
			accu.add(hints.pollFirst());
			// nb: ignore accu.add's retval coz we already stopped searching.
			return true; // coz we know n != 0
		}
		// pass through multiple hints
		accu.addAll(hints);
		// Do NOT cache hints if I am an imbedded (nested) Chainer.
		if ( isImbedded )
			hints.clear();
		// nb: ignore accu.add's return value coz we already stopped searching.
		return true; // coz we know n != 0
	}

	/**
	 * Calculates the "off" effects of "anOn".
	 * <p>
	 * Note that there is no isXChain parameter because it's always true;
	 * ie other possible positions for this value in its regions always get
	 * "off"ed; because that's the fundamental rule of Sudoku.
	 * <p>
	 * Called 9,927,934 times in top1465.
	 *
	 * @param anOn an "on" assumption to find the "off" effects of
	 * @param isYChain true means that all other potential values of this cell
	 * get "off"ed
	 * @param effects the Collection of Assumptions to populate with "off"s
	 */
	protected void onToOffs(Ass anOn, boolean isYChain, Collection<Ass> effects) {
		assert anOn.isOn && effects.isEmpty();
		final Cell cell = anOn.cell;
		final int value = anOn.value;
		// Rule 1: other potential values of this cell get "off"ed.
		if ( isYChain ) { // Y-Chain: look for consequent naked singles
			final int ovb; // otherValuesBits
			// nb: "empty test" for dynamic mode where grid has naked singles
			if ( (ovb=cell.maybes.bits & ~VSHFT[value]) != 0 )
				for ( int v : VALUESES[ovb] )
					effects.add(new Ass(cell, v, false, anOn, Cause.NakedSingle
							, "the cell can contain only one value"));
		}
		// Rule 2: X-Chain: other possible positions for value get "off"ed.
		// NB: done this way because we need to know which region we're in;
		// and it is faster to "repeat" the "same" code three times. YMMV.
		final int sv = VSHFT[value]; // shiftedValue
		for ( Cell sib : cell.box.cells )
			if ( (sib.maybes.bits & sv)!=0 && sib!=cell )
				effects.add(new Ass(sib, value, false, anOn
						, Cause.CAUSE_FOR_REGION_TYPE[Grid.BOX]
						, "the value can occur only once in the box"));
		for ( Cell sib : cell.row.cells )
			if ( (sib.maybes.bits & sv)!=0 && sib!=cell )
				effects.add(new Ass(sib, value, false, anOn
						, Cause.CAUSE_FOR_REGION_TYPE[Grid.ROW]
						, "the value can occur only once in the row"));
		for ( Cell sib : cell.col.cells )
			if ( (sib.maybes.bits & sv)!=0 && sib!=cell )
				effects.add(new Ass(sib, value, false, anOn
						, Cause.CAUSE_FOR_REGION_TYPE[Grid.COL]
						, "the value can occur only once in the col"));
	}

	/**
	 * MultipleChainer overrides this method; UnaryChainer just retains this
	 * no-op, but never calls it. This method is called by offToOns only in
	 * MultipleChainer when it isDynamic, to add any "hidden" causal parent
	 * assumptions to the effect Ass's parent-list.
	 * @param effect
	 * @param cell
	 * @param prntOffs
	 */
	protected void addHiddenParentsOfCell(Ass effect, Cell cell
		, IAssSet prntOffs) {
		// NB: this implementation never invoked. MultipleChainer overrides me.
	}

	/**
	 * MultipleChainer overrides this method; UnaryChainer just retains this
	 * no-op, but never calls it. This method is called by offToOns only in
	 * MultipleChainer when it isDynamic, to add any "hidden" causal parent
	 * assumptions to the effects parent-list. Apologies for parameters list
	 * length, it just kept growing.
	 *
	 * @param effect
	 * @param v value
	 * @param region
	 * @param currPlaces grid.region.indexesOf[value].bits
	 * @param oci otherCellIndice
	 * @param rti regionTypeIndex
	 * @param rents A set of complete parent offs, must be an IAssSet for that
	 *  weird getAss method, which I rely on, for speed, and comfort.
	 */
	protected void addHiddenParentsOfRegion(int oci, int rti, int v
			, int currPlaces, ARegion region, IAssSet rents, Ass effect) {
		// NB: this implementation never invoked. MultipleChainer overrides me.
	}

	//<NO_WRAPPING comment="Line wrapping is far too much of a pain in the ass here, so I have not bothered. Get over it.">
	/**
	 * Calculates the direct "on" effects of "anOff".
	 * Note that either isXChain or isYChain (or both) must be true.
	 * <p>USES: initGrid when isDynamic.
	 * <p>HAMMERED: called 31,758,114 times in top1465
	 *
	 * @param anOff the "off" Assumption to find the effects of.
	 * @param prntOffs the Set of parent "off" Assumptions (each complete with
	 *  its parents), from which I fetch the parents of the Asses I create
	 * @param isXChain true means if there are only 2 possible positions for
	 *  anOff.value in the region, then the other position gets "on"ed?
	 * @param isYChain true means if there are only 2 potential values for
	 *  anOn.cell then the other value gets "on"ed?
	 * @param effects the Collection of Assumptions to populate
	 * @return did we find any Ons? (just a bit faster than !effects.isEmpty())
	 */
	protected boolean offToOns(
		  Ass anOff, IAssSet prntOffs
		, boolean isXChain, boolean isYChain
		, Collection<Ass> effects
	) {
		assert !anOff.isOn;
		assert isXChain || isYChain;
		assert effects.isEmpty();
		final Cell cell = anOff.cell;
		boolean result = false;

		// Rule 1: Y-Chain: if there are only two potential values for this
		// cell then the other value gets "on"ed.
		Ass effectOn; //effectOn and his parent.
		if ( isYChain && cell.maybes.size==2 ) {
			result |= effects.add(effectOn = new Ass(cell, anOff.otherValue(), true, anOff, Cause.NakedSingle, ONLYVALUE));
			// add any erased parents of eOn to eOn (only in MultipleChainer)
			if ( isDynamic )
				// call-back MultipleChainer coz it has the current grid
				// and the initial grid, which I do not have access to.
				addHiddenParentsOfCell(effectOn, cell, prntOffs);
		}

		// Rule 2: X-Chain: if there are only two positions for this value in
		// the region then the other position gets "on"ed.
		if ( !isXChain )
			return result;
		final int v = anOff.value; // the value of the initial "off" Ass
		// foreach of the cells regions which has 2 positions for value
		for ( int rti=0; rti<3; ++rti ) { // regionTypeIndex: BOX, ROW, COL
			// skip unless there are 2 possible positions for value in region
			if ( cell.regions[rti].indexesOf[v].size == 2 ) {
				final ARegion region; // the current region which contains this cell
				final int places; // indexes of cells in region which maybe v (current grid)
				final Cell otherCell; // the other cell in this region which maybe v
				// This cluster____ was brought you by the word SPEED and the number 42.
				// Create the effectOn Assumption and add it to effects list
				// with inline otherCell = anOff.otherCellIn(region), a method
				// which calls 1-or-2 methods, each calling a method, which is
				// all a bit slow Redge, and so has been "inlined", for speed.
				// HAMMERED: top1465 ACCURACY: 209,704,756 iterations
				// BUT IT'S INCOMPREHENSIBLE, SO THIS LINE DOES:
				// (1) region = cell.regions[rti]; // not cached above coz hit rate too low.
				// (2) places = region.indexesOf[v].bits; // region.cells indexes of cell which maybe v
				// (3) otherCell = region.cells[IFIRST[places & ~ISHFT[cell.indexIn[rti]]]]; // the cell at the first [places except me] in this region
				// (4) effectOn = new Ass(otherCell, v, true, anOff, region.cause, ONLYPOS[rti]);
				// NOTES: Created Cell.indexIn and Indexes.IFIRST arrays
				// especially for this cluster____; and promulgated for speed.
				// The Indexes.IFIRST array was built for this also. It has the
				// results of Integer.numberOfTrailingZeros(i).
				// Likewise Cause.CAUSE_FOR_REGION_TYPE was created for me, and
				// my ONLYPOS array interns each String rather than building an
				// individual String on the fly a total of 209,704,756 times.
				// Array look-ups are fast. More thunk, less work, more speed!
				effectOn = new Ass(
					  otherCell = (region=cell.regions[rti]).cells[IFIRST[(places=region.indexesOf[v].bits) & ~ISHFT[cell.indexIn[rti]]]]
					, v, true, anOff, CAUSE_FOR_REGION_TYPE[rti], ONLYPOS[rti]
				);
				result |= effects.add(effectOn);
				// Dynamic: effectOn.parents += Ass's that erased this value in this region.
				if ( isDynamic ) // ie I am a MultipleChainer in dynamic mode
					// call-back my MultipleChainer subclass because it has the current and initial grids, to which I have no access.
					addHiddenParentsOfRegion(otherCell.i, rti, v, places, region, prntOffs, effectOn);
			}
		} // next regionTypeIndex
		return result;
	}
	//</NO_WRAPPING>
	// use only interned (static final) strings in assumptions, so we store
	// an address rather than a copy of the string in each assumption.
	private static final String ONLYVALUE =
		  "only remaining potential value in the cell";
	private static final String[] ONLYPOS = new String[] {
		  "only remaining position in the box"
		, "only remaining position in the row"
		, "only remaining position in the column"
	};

	/**
	 * returns target.parents.first.first...
	 * <p>This method is package visible because it's used by AChainingHint
	 * and MultipleChainer, instead of implementing it twice.
	 * @param target the Ass to find the root-cause of
	 * @return the root-cause Assumption which caused this Assumption
	 */
	static Ass getSource(Ass target) {
		Ass a = target;
		while ( a.parents!=null && a.parents.size>0 )
			a = a.parents.first.item;
		return a;
	}

	protected Pots createRedPots(Ass target) {
		final Values redVals;
		if ( target.isOn ) { // target is an "On"
			// chaining a NakedSingle nulled all my pinkBits! Hmmm.
			if ( target.cell.maybes.bits == VSHFT[target.value] )
				return null; // I don't think this ever happens
			// NB: target is an "On" -> all other cells potential values
			redVals = target.cell.maybes.minus(target.value);
		} else // target is an "Off" -> just the value
			redVals = new Values(target.value);
		assert !redVals.isEmpty();
		return new Pots(target.cell, redVals);
	}

	private void validateTheHints(Grid grid, HintsList hints) {
		final int[] svs = grid.getSolutionValues();
		for ( Iterator<AHint> it = hints.iterator(); it.hasNext(); ) {
			AHint hint = it.next();
			if ( hint == null ) {
				// should never happen! Never say never.
				Log.println("AChainer.validateTheHints:");
				Log.println("null hint!");
				it.remove(); // all I can do it skip it. Sigh.
			} else if ( hint.value!=0 && hint.cell!=null ) {
				int sv = svs[hint.cell.i];
				if ( hint.value != sv ) {
					String problem = "hint says "+hint.cell.id+PLUS+hint.value+" when solution value is "+hint.cell.id+PLUS+sv;
					Log.println("AChainer.validateTheHints:");
					Log.println("Invalid hint ("+problem+"): "+hint.toFullString());
					Log.println("Invalid grid:\n"+grid);
					it.remove(); // all I can do it skip it. Sigh.
				}
			} else if ( hint.redPots!=null && !hint.redPots.isEmpty() ) {
				if ( !HintValidator.isValid(grid, hint.redPots) ) {
					String problem = "invalidity: "+HintValidator.invalidity;
					Log.println("AChainer.validateTheHints:");
					Log.println("Invalid hint ("+problem+"): "+hint.toFullString());
					Log.println("Invalid grid:\n"+grid);
					it.remove(); // all I can do it skip it. Sigh.
				}
			} else {
				// This should never happen. Every hint eliminates a cell value
				// OR sets a cell value. Never say never.
				Log.println("AChainer.validateTheHints:");
				Log.println("Dodgy: MIA "+hint.cell+PLUS+hint.value+OR+hint.redPots);
				Log.println("Dodgy hint: No elims in: "+hint.toFullString());
				Log.println("Dodgy grid:\n"+grid);
				it.remove(); // all I can do it skip it. Sigh.
			}
		}
	}

	/**
	 * A FunkyAssSet2 is a {@link FunkyAssSet} which tracks it's
	 * own maxSize using FunkyAssSetSizes (below). To workout how big your
	 * FunkyAssSet's need to be (determinable only at runtime) search for the
	 * FunkyAssSet constructors and replace them with with FunkyAssSet2; then
	 * run the LogicalSolverTester which dump all FunkyAssSet2-sizes to stdout.
	 * NB: A good initialCapacity covers atleast 90% of cases (or 100% if that
	 * is not too big, or you are creating the set just ONCE). Repeated growth
	 * is my pet hate. I find it exponentially retarded/annoying! Hence I go
	 * to the effort of this reasonably-roundabout-way to bloody avoid it.
	 * <pre>
	 * Observed:
	 *	94	getUnaryChainsAndCycles(Chainer.java:327)
	 * 144	getUnaryChainsAndCycles(Chainer.java:328)
	 *	 2	doUnaryChainAndCycle(Chainer.java:404)
	 *	63	getMultipleOrDynamicChains(Chainer.java:530)
	 * 178	getMultipleOrDynamicChains(Chainer.java:531)
	 *	62	getMultipleOrDynamicChains(Chainer.java:535)
	 * 183	getMultipleOrDynamicChains(Chainer.java:536)
	 * </pre>
	 */
	protected static final class FunkyAssSet2 extends FunkyAssSet { // 1465 in 416 secs at 0.284
		public int maxSize = 0;
		public FunkyAssSet2(int initialCapacity, float loadFactor, boolean isOn) {
			super(initialCapacity, loadFactor, isOn);
			// get a representation of the line of code which created me.
			// Note that this relies on my caller actually creating me, not
			// delegating creation to a helper-method.
			String id = Thread.currentThread().getStackTrace()[2].toString();
			FunkyAssSizes.THE.put(id, this);
		}
		/** This copy-constructor clears the given {@code IMySet<Ass> c} if
		 * isClearing: it's more efficient to poll() each element out of c
		 * than it is to addAll and then clear c.
		 * @param initialCapacity
		 * @param loadFactor
		 * @param isOn
		 * @param c
		 * @param isClearing */
		public FunkyAssSet2(int initialCapacity, float loadFactor, boolean isOn
				, IMySet<Ass> c, boolean isClearing) { // can clear c
			super(Math.max(initialCapacity,c.size()), loadFactor, isOn);
			String id = Thread.currentThread().getStackTrace()[2].toString();
			FunkyAssSizes.THE.put(id, this);
			if ( isClearing ) // steal all from c
				for ( Ass a=c.poll(); a!=null; a=c.poll() ) // removes head
					add(a);
			else
				addAll(c);
		}
		@Override
		public boolean add(Ass o) {
			maxSize = Math.max(size()+1, maxSize);
			return super.add(o);
		}
	}

	protected static final class FunkyAssSizes
			extends java.util.LinkedHashMap<String, FunkyAssSet2> {
		private static final long serialVersionUID = 173460243L;
		public static final FunkyAssSizes THE = new FunkyAssSizes();
		private FunkyAssSizes() {} // can't hide a static inner class constructor
	}

	protected void processFunkyAssSizes() {
		final FunkyAssSizes sizes = FunkyAssSizes.THE; // a set of the sizes of sets.
		if ( sizes.size() > 0 ) {
			final OverallSizes overalls = OverallSizes.THE;
			for ( java.util.Map.Entry<String,FunkyAssSet2> e : sizes.entrySet() )
				overalls.max(e.getKey(), e.getValue().maxSize);
			sizes.clear();
		}
	}

	protected final boolean interrupted() {
		final IInterruptMonitor im = this.interruptMonitor;
		return im!=null && im.isInterrupted();
	}

}
