/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.chain.AChainingHint;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Hash.LSH4;
import static diuf.sudoku.utils.Hash.LSH8;
import diuf.sudoku.utils.MyLinkedList;
import java.util.ArrayList;


/**
 * An Ass(umption) is an assumption that a Cell will/not be a value. The class
 * name is shortened just for convenience, and comedic effect. Wait until we
 * wewease the Kraken.
 * <p>
 * This class is used in diuf.sudoku.solver.hinters.chain (everywhere)
 * and diuf.sudoku.solver.hinters.fish.FrankenFisherman.
 * <p>
 * Stores the Cell, value, and isOn; as well as the hashCode which uniquely
 * identifies this combination in 12 bits, which is a 2048 HashMap, which is
 * many more than ever actually add, but my hashcode's most significant bits
 * are right-most so you really want just 8 bits (64 Entries) to reduce the
 * collision rate so that get and add are "pretty fast". But, the larger the
 * HashMap table the slower the constructor. Everything costs something, but
 * normally get times far outweigh constructor times, so be generous!
 * Play with your initialCapacity's to see what works-out quickest.
 * <pre>
 *      1 2 3 4  5  6  7   8   9  10   11   12
 *      1,2,4,8,16,32,64,128,256,512,1024,2048
 * </pre>
 * <p>
 * Optionally stores a list of parent (causative) assumptions. The full chain
 * can be found from the last item of the chain (the target) by following the
 * parent references back the the source (which has no parents), hence this
 * class may also represent an entire forcing chain of assumptions.
 * <p>
 * We also optionally store a cause, explanation, and nestedChain. See the
 * attribute for details.
 * <p>
 * <b>Voluminous:</b> Asat 2019-10-11 We construct 849,531,749 Ass's during
 * a LogicalSolverTester run over top1465.d5.mt in "normal" ACCURACY mode, so
 * the performance of the constructor code itself (especially the 2nd one) is
 * bloody critical to the performance of Chainer, which is critical to overall
 * performance, in the GUI as well as LogicalSolverTester.
 */
public class Ass {

//public static long assConstructor1, assConstructor2, assConstructor3;
//Sudoku Explainer 6.30.025 ran 2019-10-10.07-46-06
//mode    : ACCURACY, NOT REDO, NOT STATS, Hacky, Hackier, Hackfect
//input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
//  assConstructor1 =     28,085,467
//  assConstructor2 =    821,422,631
//  assConstructor3 =         23,651
//  total           =    849,531,749 <<<======================================== S__T LOADS OF ASS!
// pzls	       total (ns)	(mm:ss)	        each (ns)
// 1465	2,340,199,543,783	(39: 0)	    1,597,405,831
//
//Sudoku Explainer 6.30.025 ran 2019-10-10.09-38-05
//mode    : ACCURACY, REDO, NOT STATS, Hacky, Hackier, Hackfect
//input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
//  assConstructor1 =     27,141,218
//  assConstructor2 =    809,390,516
//  assConstructor3 =         23,651
//  total           =    836,555,385
// pzls	       total (ns)	(mm:ss)	        each (ns)
// 1465	  591,615,010,081	( 9:51)	      403,832,771 <<<<====================== DONE A LOT FASTER!

//public static long ass2time;

//not used: retained for documentation: HERE is how to make a hashCode.
//Note that the hashCode field is now calculated in-situ everywhere it needs to
//be, rather than waste time on a method call. This boat is right out.
//	public static int hashCode(Cell cell, int value, boolean isOn) {
//		return (isOn?4096:0) ^ LSH8[value] ^ cell.hashCode;
//	}

	public static int hashCode(int x, int y, int value, boolean isOn) {
		// NOTE: terniaries are slow!
		//return (isOn?4096:0) ^ LSH8[value] ^ LSH4[y] ^ x;
		if ( isOn )
			return 4096 ^ LSH8[value] ^ LSH4[y] ^ x;
		return LSH8[value] ^ LSH4[y] ^ x;
	}

	/**
	 * Returns a new {@code ArrayList<Ass>} of my arguements array.
	 * @param asses to add to the new list
	 * @return a new {@code ArrayList<Ass>}
	 */
	public static ArrayList<Ass> arrayList(Ass... asses) {
		ArrayList<Ass> list = new ArrayList<>(asses.length);
		for ( Ass ass : asses )
			list.add(ass);
		return list;
	}

	/** An enum of the type-of the cause of an Assumption. */
	public static enum Cause {
		  NakedSingle(-1)		// regionTypeIndex not applicable
		, HiddenBox(Grid.BOX)	// 0
		, HiddenRow(Grid.ROW)	// 1
		, HiddenCol(Grid.COL)	// 2
		, Advanced(-1)			// regionTypeIndex not applicable
		;
		/**
		 * The Cause for a HiddenSingle by regionTypeIndex (rti).
		 */
		public static final Cause[] CAUSE_FOR = {
			  HiddenBox
			, HiddenRow
			, HiddenCol
		};
		public final int regionTypeIndex;
		private Cause(int regionTypeIndex) {
			this.regionTypeIndex = regionTypeIndex;
		}
	}

	// these are mandatory
	/** Always cell.i, I'm just seeing if this is any faster. */
	public final int i;
	/** The value which this Assumption guesses. */
	public final int value;
	/** The Cell to which this Assumption relates. */
	public final Cell cell;
	/** If isOn is true it means that we guess Cell is value;<br>
	 * false means we guess Cell is NOT value. */
	public final boolean isOn;

	// these are optional
	/** The immediate parent(s) of this Assumption, in a MyLinkedList because
	 * the Chainer needs to know all about my internal structure, for speed. */
	public MyLinkedList<Ass> parents;
	/** An enumeration of the type-of the cause of this assumption. */
	public final Cause cause;
	/** A one-line explanation of the scenario which lead to this Assumption. */
	public final String explanation;
	/** The nested chaining hint which substantiates this Assumption from
	 * the ChainsHintsAccumulator. */
	public final AChainingHint nestedChain;

	/**
	 * hashCode of 12 bits which uniquely identifies this Ass. Uniqueness is
	 * important because:<ul>
	 * <li>(1) we rely upon that in equals; and</li>
	 * <li>(2) Ass is used extensively as a HashMap key, so hashCode() must
	 * be fast, and it's rightmost bits must be most deterministic in order
	 * to differentiate Ass's in millions of very small HashMaps.</li>
	 * </ul>
	 * <p>
	 * The hashCode field is public final for speed. It's accessed directly by
	 * the {@code diuf.sudoku.utils.MyFunkyLinkedHashMap.getAss} method (et al)
	 * which is naughty because it assumes that the generic type K (for Key) is
	 * Ass, which it may not be. If you're dumb enough to try to getAss from a
	 * Set of Sheep, then a pox on all of your {@code List<Camel>}. Breaking da
	 * rules is just a bit faster, that's all... so do it often, but do it well
	 * and document it, and never bloody apologise for it. Sorry. Sigh.
	 * <p>
	 * Note that the hashCode field is calculated in-situ everywhere it needs
	 * to be rather than waste clock-ticks on a method call. I've pushed the
	 * boat right out.
	 */
	public final int hashCode; // calculated ONCE; returned by hashCode()

	/**
	 * Constructs an "identity Ass", with just the identity fields.
	 * <p>
	 * Note that Ass.equals only compares hashCodes, so that an "identity ass"
	 * will equal a "complete ass" with the same: Cell, value, and isOn. The
	 * unusual {@code Chains.MyAssSet.getAss} method relies on this behaviour
	 * to quickly retrieve his <b>complete</b> self from a {@code Set<Ass>}.
	 * <p>
	 * nb: These constructors are NOT chained for performance.
	 * @param cell Cell
	 * @param value int
	 * @param isOn boolean
	 */
	public Ass(Cell cell, int value, boolean isOn) {
//++assConstructor1; // 28,085,467
		assert cell!=null;
		this.i = cell.i;
		this.cell = cell;
		this.value = value;
		this.isOn = isOn;
		// 1,2,4,8,16,32,64,128,256,512,1024,2048,4096 = 1<<12
		// 1 2 3 4  5  6  7   8   9  10   11   12,  13
		// NOTE: terniaries are slow!
		if ( isOn )
			hashCode = 4096 ^ LSH8[value] ^ cell.hashCode;
		else
			hashCode = LSH8[value] ^ cell.hashCode;
		// parents is null until it is populated
		parents = null;
		this.cause = null;
		this.explanation = null;
		this.nestedChain = null;
	}

	/**
	 * Constructs a "complete ass", which has a parent Ass which is nullable.
	 * <p><b>!!!! PERFORMANCE IS CRITICAL !!!!</b> This constructor was called
	 * 821,422,631 times in ACCURACY mode for top1465.d5.mt
	 * <p>NB: These constructors are NOT chained for performance.
	 * @param cell Cell
	 * @param value int
	 * @param isOn boolean
	 * @param parent Ass
	 * @param cause Cause
	 * @param explanation String
	 * @see Chains.findOffToOns and Chains.findOnToOffs
	 */
	public Ass(Cell cell, int value, boolean isOn, Ass parent, Cause cause
			, String explanation) {
//!!!!!!!!!!! PERFORMANCE CRITICAL !!!!!!!!!!!
//++assConstructor2; // 821,422,631
//long t0 = System.nanoTime();
		this.i = cell.i;
		this.cell = cell;
		this.value = value;
		this.isOn = isOn;
		// NOTE: terniaries are slow!
		if ( isOn )
			hashCode = 4096 ^ LSH8[value] ^ cell.hashCode;
		else
			hashCode = LSH8[value] ^ cell.hashCode;
		// nullable non-final parents is 1.4 seconds faster top1465
		if ( parent == null ) { // there are 4,024,758 null parents
			parents = null;
		} else {
			// MyLinkedList is a tad faster than java.util.LinkedList
			// I tried an array, which works. It was faster, and the code is no
			// more complex really. It was fastest when array grows one by one.
			// I replaced chainers queues with circular arrays, which is harder
			// to grock, but faster with gains from inline add and poll.
			parents = new MyLinkedList<>();
			parents.linkLast(parent);
		}

		this.cause = cause;
		this.explanation = explanation;
		this.nestedChain = null;
//ass2time += System.nanoTime() - t0;
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	}

	/**
	 * Constructs a "nested ass".
	 * <p>
	 * Used by {@link diuf.sudoku.solver.hinters.chain.ChainsHintsAccumulator}
	 * which retrieves completeParents, and sets the nestedChain (nullable).
	 * <p>
	 * <b>WARN:</b> The completeParents LinkedList is stashed so it must be
	 * a new collection for each constructor.
	 * <p>
	 * NB: These constructors are NOT chained for performance.
	 *
	 * @param cell Cell
	 * @param value int
	 * @param isOn boolean
	 * @param completeParents {@code LinkedList<Ass>} <b>WARN:</b> This
	 *  parameter is stashed so it must be a new collection each time.
	 * @param cause Cause
	 * @param explanation String
	 * @param nestedChain AChainingHint
	 */
	public Ass(Cell cell, int value, boolean isOn
			, MyLinkedList<Ass> completeParents  // with ancestors
			, Cause cause, String explanation, AChainingHint nestedChain) {
//++assConstructor3; // 23,651
		this.i = cell.i;
		this.cell = cell;
		this.value = value;
		this.isOn = isOn;
		// NOTE: terniaries are slow!
		if ( isOn )
			hashCode = 4096 ^ LSH8[value] ^ cell.hashCode;
		else
			hashCode = LSH8[value] ^ cell.hashCode;
		parents = completeParents;
		// NB: use addParent if nulls ever occur!
		this.cause = cause;
		this.explanation = explanation;
		this.nestedChain = nestedChain;
	}

	/**
	 * Constructor used by KrakenFisherman's Eff class which extends Ass to do
	 * it's own chaining with a double-linked net; and so needs an Ass with a
	 * parents list (the actual parent/s and children/s to be specified later)
	 * but no cause or explanation.
	 *
	 * @param cell
	 * @param value
	 * @param isOn
	 * @param parent
	 */
	public Ass(Cell cell, int value, boolean isOn, Ass parent) {
		this.i = cell.i;
		this.cell = cell;
		this.value = value;
		this.isOn = isOn;
		// NOTE: terniaries are slow!
		//this.hashCode = (isOn?4096:0) ^ LSH8[value] ^ cell.hashCode;
		if ( isOn )
			hashCode = 4096 ^ LSH8[value] ^ cell.hashCode;
		else
			hashCode = LSH8[value] ^ cell.hashCode;
		// Eff's have parents even if the given parent is null!
		this.parents = new MyLinkedList<>();
		if ( parent != null )
			this.parents.linkLast(parent);
		this.cause = null;
		this.explanation = null;
		this.nestedChain = null;
	}

//KEEP4DOC: implemented in-line for speed in Chainer.doUnary
//	/** @return flip().hashCode(); */
//	public int conjugatesHashCode() {
//		// NB: the isOn terniary is backwards
//		return (!isOn?4096:0) ^ LSH8[value] ^ cell.hashCode;
//	}

	/**
	 * Returns a new this with the isOn field negated.
	 * <p>NB: caching it was actually slower! (WTF?)
	 * @return new Ass(cell, value, !isOn)
	 */
	public Ass flip() {
		return new Ass(cell, value, !isOn);
	}

	/** Do cell.canNotBe(value) */
	public void erase() {
		assert !isOn : "Don't call erase() on an \"On\", ya putz!";
		cell.canNotBe(value);
	}

	/** @return the other value (ie not this.value) which this.cell may be. */
	public int otherValue() {
		assert cell.size == 2;
		if ( VFIRST[cell.maybes] == value ) {
			return VFIRST[cell.maybes & ~VSHFT[VFIRST[cell.maybes]]];
		}
		return VFIRST[cell.maybes];
	}

//KEEP_4_DOC: This method no longer used but retain as documentation.
//	/** @return the other Cell in the given region which maybe this.value. */
//	Cell otherCellIn(ARegion region) {
//		Indexes riv = region.ridx[value];
//		assert riv.size == 2;
//		int i = riv.first();
//		Cell otherCell = region.cells[i];
//		if ( otherCell == cell )
//			otherCell = region.cells[riv.next(i+1)];
//		return otherCell;
//	}

	/**
	 * Does this assumption have any parents?
	 * @return {@code parents!=null && parents.size()>0}
	 */
	public boolean hasParents() {
		return parents!=null && parents.size>0;
	}

	public Ass firstParent() {
		if ( parents == null )
			return null;
		return parents.peekFirst();
	}

	/**
	 * Add the given parent to this Assumptions list of parents. Caters for a
	 * parent being added to an Ass that was created with a null parent, and
	 * therefore has a null parents list.
	 * <p>
	 * I'm now ONLY used in UnaryChainer.reverseCycle, where I'm essential.
	 * UnaryChainer.reverseCycle is ONLY place where new parents list created;
	 * and he only in createBidirectionalCycleHint (ie not too often) so there
	 * is nothing to worry about, except ALL other calls to addParent should
	 * call parents.linkLast(parent) directly, which I do now with a... sigh.
	 *
	 * @param parent Ass to add
	 * @return always returns true, and does NOT throw any RuntimeExceptions.
	 */
	public final boolean addParent(Ass parent) {
		if ( parents == null )
			parents = new MyLinkedList<>();
		parents.linkLast(parent);
		return true;
	}

	/** @return {@code cell.id+"+/-"+value} */
	public String contra() {
		return cell.id+"+/-"+value;
	}

	/**
	 * Creating a new Ass just to get it's weak string was dumb!
	 * <pre>
	 * , (src.isOn ? src : src.flip()).weak()	// if {0} then 2
	 * , (src.isOn ? src.flip() : src).weak()	// if {1} then 2
	 * </pre>
	 * <p>becomes
	 * <pre>
	 * , src.weak(true)		// if {0} then 2
	 * , src.weak(false)	// if {1} then 2
	 * </pre>
	 * @param parity boolean true for isOn, else false. You'll be calling me
	 *  once for each parity, as per the above.
	 * @return "A1 (contains/does not contain) the value 1" depending
	 *  upon if isOn==parity.
	 */
	public String weak(boolean parity) {
		// nb: terniaries are fast enough when we're building html
		return cell.id
			 + (isOn==parity ? " contains" : " does not contain")
			 + " the value " + value;
	}

	/**
	 * Stringify this assumption using linguistically weaker language.
	 * @return "A1 (contains/does not contain) the value 1" depending on isOn.
	 */
	public String weak() {
		// nb: if you change the result then you also need to do a search for
		//     sourceWeakPrefix and change it as well.
		// nb: terniaries are fast enough when we're building html
		return cell.id
			 + (isOn ? " contains" : " does not contain")
			 + " the value " + value;
	}

	/**
	 * Stringify this assumption using linguistically stronger language.
	 * @return "A1 (must/cannot) contain the value 1" depending on isOn.
	 */
	public String strong() {
		return cell.id
			 + (isOn ? " must contain" : " cannot contain")
			 + " the value " + value;
	}

	/** @return {@code cell.id+(isOn?PLUS:MINUS)+value} */
	@Override
	public String toString() {
		if ( true ) { // @check true
			if(ts!=null) return ts;
			return ts = cell.id + (isOn?PLUS:MINUS) + value;
		} else { // DEBUG: no caching
			return cell.id + (isOn?PLUS:MINUS) + value;
		}
	}
	private String ts;

	/**
	 * Is this Object an Ass with the same hashCode (isOn|value|cell) as me?
	 * <p>
	 * <b>WARNING</b>: The fact that Ass.equals just equates hashCodes is
	 * relied upon by the getCell methods of all IAssSet implementations.
	 * So review them <b>BEFORE</b> you change this, and don't change this!
	 * @param o the other Object to equate this with
	 * @return boolean are these two Ass's equivalent? Cheeky!
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof Ass && ((Ass)o).hashCode == hashCode;
	}

	public boolean equals(Ass a) {
		return a.hashCode == hashCode;
	}

	/** @return {@code (isOn?1<<12:0)^value<<8^cell.hashCode}. See the class
	 * comment for details of a good HashSet/Map initialCapacity. */
	@Override
	public int hashCode() {
		return hashCode;
	}
}