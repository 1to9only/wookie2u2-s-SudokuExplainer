/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

// Rule: .* your own s__t. Never .* import anything you cannot modify, but you
// can .* import anything that you can modify to resolve any name-conflicts.
// The only possible exception is Swing, coz there are too many of them.
import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.solver.hinters.chain.AChainingHint;
import diuf.sudoku.solver.hinters.chain.ChainerUnary;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Frmt.PLUS_OR_MINUS;
import diuf.sudoku.utils.MyLinkedList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * An Ass(umption) is an assumption that a Cell will/not be a value. The class
 * name is shortened just for convenience, and comedic effect. Wait until we
 * wewease da Kraken. Effit!
 * <p>
 * This class is used in diuf.sudoku.solver.hinters.chain (everywhere) and
 * {@link diuf.sudoku.solver.hinters.fish.KrakenFisherman}.
 * <p>
 * Stores the Cell, value, and isOn; as well as the hashCode which uniquely
 * identifies this combination in 12 bits, which is a 2048 HashMap, which is
 * many more than ever actually add, but my hashcodes most significant bits
 * are right-most so you really want just 8 bits (64 Entries) to reduce the
 * collision rate so that get and add are "pretty fast". But, the larger the
 * HashMap table the slower the constructor. Everything costs something, but
 * normally get times far outweigh constructor times, so be generous!
 * Play with your initialCapacitys to see what works-out quickest.
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
 * <b>Voluminous:</b> Asat 2019-10-11 We construct 849,531,749 Asses during
 * a LogicalSolverTester run over top1465.d5.mt in "normal" ACCURACY mode, so
 * the performance of the constructor code itself (especially the 2nd one) is
 * bloody critical to the performance of Chainer, which is critical to overall
 * performance, in the GUI as well as LogicalSolverTester.
 */
public class Ass implements Comparable<Ass> {

//	public static final long[] COUNTS = new long[1];

	/** Self-documenting code: On Assumptions pass isOn = ON. */
	public static final boolean ON = true;

	/** Self-documenting code: Off Assumptions pass isOn = OFF. */
	public static final boolean OFF = false;

	/** The set (1) bit that denotes an ON assumption. */
	public static final int ON_BIT = 1<<11; // 2048 (the twelth bit)

	/** The shift for values, currently 7 (now 1 is 0, coz there is no 0) */
	public static final int[] ASHFT = new int[] {
	//  0  1  2     3     4     5     6     7     8     9
		0, 0, 1<<7, 2<<7, 3<<7, 4<<7, 5<<7, 6<<7, 7<<7, 8<<7
	};

	/** The indice is 7 bits. */
	private static final int BITS7 = 1<<7-1;

	/** The value is 4 bits. */
	private static final int BITS4 = 1<<4-1;

	/**
	 * This method exists to define how to calculate the hashCode for an Ass,
	 * but its not used (much) because calculating it in-line is faster.
	 * <pre>
	 * hashCode:
	 * indice is 0..80, which is 1010000‬, ie 7 bits.
	 * value is 1..9, which is 1001, ie 4 bits (&lt;&lt;7).
	 * hence ON_BIT is the twelth bit (1&lt;&lt;11).
	 * </pre>
	 *
	 * @param indice the indice of the Cell in Grid.cells.
	 * @param value the value of this assumption
	 * @param isOn true means this an ON assumption, else OFF
	 * @return an Ass.hashCode
	 */
	public static int hashCode(final int indice, final int value, final boolean isOn) {
		if ( isOn )
			return ON_BIT ^ ASHFT[value] ^ indice;
		return ASHFT[value] ^ indice;
	}

	public static int indiceOf(final int hashCode) {
		return hashCode & BITS7; // the first 7 bits
	}

	public static int valueOf(final int hashCode) {
		return hashCode>>7 & BITS4; // bits 8..11 (inclusive)
	}

	public static boolean isOnOf(final int hashCode) {
		return (hashCode & ON_BIT) > 0; // the twelth bit
	}

	/**
	 * Returns a new {@code ArrayList<Ass>} of my arguments array.
	 *
	 * @param asses to add to the new list
	 * @return a new {@code ArrayList<Ass>}
	 */
	public static ArrayList<Ass> arrayList(final Ass... asses) {
		final ArrayList<Ass> list = new ArrayList<>(asses.length);
		for ( Ass ass : asses )
			list.add(ass);
		return list;
	}

	/**
	 * Returns a new {@code LinkedList<Ass>} of my arguments array.
	 *
	 * @param array to add to the new list
	 * @param n number of Asses to read from array
	 * @return a new {@code ArrayList<Ass>}
	 */
	public static List<Ass> linkedList(final Ass[] array, final int n) {
		final LinkedList<Ass> result = new LinkedList<>();
		for ( int i=0; i<n; ++i )
			result.add(array[i]);
		return result;
	}

	/** An enum of the type-of the cause of an Assumption. */
	public static enum Cause {
		  NakedSingle(-1)		// regionTypeIndex not applicable
		, HiddenBox(Grid.BOX)	// 0
		, HiddenRow(Grid.ROW)	// 1
		, HiddenCol(Grid.COL)	// 2
		, Foxy(-1)				// regionTypeIndex not applicable
		;
		/**
		 * The Cause for a HiddenSingle by regionTypeIndex (rti), which must
		 * remain coincident with Grids BOX, ROW, and COL constants.
		 */
		public static final Cause[] CAUSE = {
			  HiddenBox // 0=BOX
			, HiddenRow // 1=ROW
			, HiddenCol // 2=COL
		};
		/** regionTypeIndex. */
		public final int rti;
		private Cause(final int regionTypeIndex) {
			this.rti = regionTypeIndex;
		}
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * append the asses in the chain ending in $a to $sp, separated by $sep.
	 *
	 * @param sb StratavariousBungler to append to
	 * @param a to stringify
	 * @param sep you gotta keep em
	 * @return a longer strat
	 */
	public static StringBuilder appendChain(final StringBuilder sb, Ass a, final String sep) {
		if ( a != null ) {
			sb.append(a);
			while ( (a=a.firstParent) != null )
				sb.append(sep).append(a);
		}
		return sb;
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * returns a string of the chain in $asses, separated by $sep
	 * <pre>{@code
	 * String: toStringChain(asses, " <- ")
	 *   HTML: toStringChain(asses, " &lt;- ")
	 * }</pre>
	 &
	 * @param asses target asees to see the chains of, back to the initial
	 *  assumption
	 * @param sep arator
	 * @return a string of the chains in asses
	 */
	public static String toStringChain(final Ass[] asses, final String sep) {
		if ( asses == null || asses.length==0 )
			return "";
		final StringBuilder sb = SB(2048);
		int cnt = 0;
		for ( Ass a : asses ) {
			if ( a != null ) {
				if ( ++cnt > 1 ) {
					sb.append(NL);
				}
				appendChain(sb, a, sep);
			}
		}
		return sb.toString();
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * See the chain of this ass, all the way back to the initial assumption.
	 *
	 * @param ass the "target" ass to see the chain of
	 * @param sep arator
	 * @return A String containing the chain of asses back to the initial
	 *  assumption from the given "target" ass
	 */
	public static String toStringChain(final Ass ass, final String sep) {
		return appendChain(SB(128), ass, sep).toString();
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * See the chain of these asses, in String format (not HTML safe), all the
	 * way back to the initial assumption.
	 *
	 * @param asses the "target" asses to see the chains of
	 * @return A String containing the chains of asses back to the initial
	 *  assumption from the given "target" asses
	 */
	public static String toStringChain(final Ass[] asses) {
		return toStringChain(asses, " <- ");
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * Returns a string representation of the forcing chain
	 * from the given Ass(umption)
	 * back to the initialOn (the starting assumption).
	 * <pre>
	 * For example: Ass.toStringChain(ass), where ass is E4-1, returns
	 * {@code "E4-1 <- I4+4 <- G4-4 <- G1+4"}
	 * so G1+4 was the initial assumption, and heres how it causes E4-1.
	 * </pre>
	 * This method was written to see the whole chain in the debugger,
	 * though it may get used in real code; like printing hint-html.
	 * <p>
	 * TIP: In the GUI, when you select a cell-id in the hint-html that cell
	 * is selected (yellow backgrounded) in the gridPanel (ie the puzzle),
	 * which is really handy for following a chain; saves you converting chess
	 * notation into a place (which I suck at).
	 *
	 * @param ass the targetAss (ie the "consequent" Ass, with parents)
	 * @return see example in method comments; if the given ass is null then
	 *  the empty string is returned
	 */
	public static String toStringChain(final Ass ass) {
		return toStringChain(ass, " <- ");
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * See the chains from these $asses, all the way back to the initial
	 * assumption.
	 *
	 * @param asses "target" asses to see the chains of
	 * @param sep arator
	 * @return A String of the chains of these asses
	 */
	public static String toStringChain(final Collection<Ass> asses, final String sep) {
		if ( asses == null || asses.isEmpty() )
			return "";
		final StringBuilder sb = SB(128);
		int cnt = 0;
		for ( Ass a : asses ) {
			if ( a != null ) {
				if ( ++cnt > 1 )
					sb.append(NL);
				appendChain(sb, a, sep);
			}
		}
		return sb.toString();
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * See the chains back-from $asses in String format (NOT html safe).
	 *
	 * @param asses "target" asses to see the chains of
	 * @return A String of the chains of these asses
	 */
	public static String toStringChain(Collection<Ass> asses) {
		return toStringChain(asses, " <- ");
	}

//DEBUG: retain all toStringChain, even if unused
	/**
	 * For debugging {@link ChainerUnary#reverseCycle(Ass) }.
	 *
	 * @param ass
	 * @return the cycle with explanations, one per line
	 */
	public static String pleaseExplain(Ass ass) {
		final StringBuilder sb = SB(128);
		if ( ass != null ) {
			sb.append(ass).append(" ").append(ass.explanation);
			while ( (ass=ass.firstParent) != null ) {
				sb.append(NL).append(ass).append(" ").append(ass.explanation);
			}
		}
		return sb.toString();
	}

	// these are mandatory
	/** Always cell.i, Im just seeing if this is any faster. */
	public final int indice;

	/** The value which this Assumption guesses. */
	public final int value;

	/** If isOn is true it means that we guess Cell is value;<br>
	 * false means we guess Cell is NOT value. */
	public final boolean isOn;

	// these are optional
	/** The immediate parent(s) of this Ass, as a MyLinkedList because the
	 * Chainers need access to the actual linked-list, for speed.
	 * <p>
	 * nb: parents may be null, but NEVER empty! If there is no parent then the
	 * parents list remains null, to save wasting time creating an empty list
	 * that subsequently remains empty and also to save time repeatedly testing
	 * that a non-null parents list is not bloody empty, so null means none! */
	public MyLinkedList<Ass> parents;

	/** The first parent-Ass of this Ass, ergo my direct "cause". This "parent"
	 * assumption is true in order for this Ass to exist. */
	public Ass firstParent;

	/** An enumeration of the type-of the cause of this assumption. */
	public final Cause cause;

	/** A one-line explanation of the scenario which lead to this Assumption. */
	public final String explanation;

	/** The nested chaining hint which substantiates this Assumption from
	 * the ChainsHintsAccumulator. */
	public final AChainingHint nestedChain;

	/**
	 * Ass.hashCode uniquely identifies this Ass. Its final: set ONCE in the
	 * constructor, and cannot change. Ass is an immutable DTO.
	 * <p>
	 * This <b>REALLY</b> matters because:<ul>
	 * <li>{@link equals(Ass)} relies on distinct hashCodes
	 * <li>Ass is used EXTENSIVELY as a HashMap key
	 * <li>hashCode() must be fast (not just immutable and deterministic)
	 * <li>hashCodes rightmost-bits (LSBR) are most deterministic, for good
	 *  differentiation of Asses in small HashMaps
	 * <li>where possible, prefer the hashCode field to hashCode()
	 * <li>NOTE: experts tell me this is wrong/bad, and to look at the PCODE.
	 *  Righto, so how does one see the PCODE, let alone the actual optimised
	 *  compiled-code? Experts say preferring direct field access to accessor
	 *  methods may be counter-productive coz it confuses the optimiser, but
	 *  all I have to go on are timings, which demur, therefore I conclude that
	 *  every stack-frame slows us down. Stack-frame creation costs, hence I
	 *  choose to give it no excuse to create stack-frames in my tight loops.
	 *  You may demur, and you might even be right. Test it 750,000,000 times.
	 *  Now swap the order of your two tests. WTF? Java sux! Sigh.
	 * <li>NOTE: I pushed the boat right out. hashCode is calculated in-situ
	 *  where required rather than new Ass(.*).hashCode, which is a waste. This
	 *  is a BIG mess. Its unmaintainable coz it leaks Asses impl detail (how
	 *  to calculate Ass.hashCode) all over the bloody shop. If this was a real
	 *  system you would atleast use the static hashCode method. Im trying too
	 *  hard. Sigh.
	 * </ul>
	 * <p>
	 * The hashCode field is public final, for direct access, for speed.
	 * <p>
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
	 * @param indice of Cell
	 * @param value int
	 * @param isOn boolean
	 */
	public Ass(final int indice, final int value, final boolean isOn) { // 28,085,467
		this.indice = indice;
		this.value = value;
		this.isOn = isOn;
		// max cell.i is 80, which is 7 bits
		// value is left shifted 7 bits, but 1 encodes as 0 (9 options),
		// to reduce the max encoded value to 8, which is 4 bits.
		// the ON_BIT is the twelth bit, so thats 2048 possible HCs.
		// Check: (81-17)=64 cells by 9 maybes = 576 * 2 for on/off = 1152,
		//        which exceeds 1024, so HC wont fit in 8 bits. OK.
		// 1 2 3 4  5  6  7   8   9  10   11   12
		// 1 2 4 8 16 32 64 128 256 512 1024 2048
		if ( isOn )
			hashCode = ON_BIT ^ ASHFT[value] ^ indice;
		else
			hashCode = ASHFT[value] ^ indice;
		// parents is null until it is populated
		this.parents = null;
		this.firstParent = null;
		this.cause = null;
		this.explanation = null;
		this.nestedChain = null;
	}

	/**
	 * Constructs a "complete ass", which has a parent Ass which is nullable.
	 * <p>
	 * <b>HAMMERED</b> 821,422,631 times in top1465.
	 *
	 * @param indice of Cell
	 * @param value int
	 * @param isOn boolean
	 * @param parent Ass
	 * @param cause Cause
	 * @param explanation String
	 * @see Chains.findOffToOns and Chains.findOnToOffs
	 */
	public Ass(final int indice, final int value, final boolean isOn
			, final Ass parent, final Cause cause, final String explanation) { // 821,422,631
		this.indice = indice;
		this.value = value;
		this.isOn = isOn;
		if ( isOn )
			hashCode = ON_BIT ^ ASHFT[value] ^ indice;
		else
			hashCode = ASHFT[value] ^ indice;
		this.firstParent = parent; // even if null
		if ( parent == null ) // 4,024,758
			this.parents = null;
		else { // 478,931,278
			// MyLinkedList evil public fields faster than java.util.LinkedList
			this.parents = new MyLinkedList<>();
			this.parents.linkLast(parent);
		}

		this.cause = cause;
		this.explanation = explanation;
		this.nestedChain = null;
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
	 * @param indice of Cell
	 * @param value int
	 * @param isOn boolean
	 * @param completeParents {@code LinkedList<Ass>} <b>WARN:</b> This
	 *  parameter is stashed so it must be a new collection each time.
	 * @param cause Cause
	 * @param explanation String
	 * @param nestedChain AChainingHint
	 */
	public Ass(final int indice, final int value, final boolean isOn
			, final MyLinkedList<Ass> completeParents  // with ancestors
			, final Cause cause, final String explanation
			, final AChainingHint nestedChain) { // 23,651
		this.indice = indice;
		this.value = value;
		this.isOn = isOn;
		if ( isOn )
			hashCode = ON_BIT ^ ASHFT[value] ^ indice;
		else
			hashCode = ASHFT[value] ^ indice;
		if ( completeParents.first == null) {
			this.parents = null; // nullable but never empty
			this.firstParent = null;
		} else {
			this.parents = completeParents;
			this.firstParent = completeParents.first.item;
		}
		// NB: use addParent if nulls ever occur!
		this.cause = cause;
		this.explanation = explanation;
		this.nestedChain = nestedChain;
	}

	/**
	 * Constructor used by KrakenFishermans Eff class which extends Ass to do
	 * its own chaining with a double-linked net; and so needs an Ass with a
	 * parents list (the actual parent/s and children/s to be specified later)
	 * but no cause or explanation.
	 *
	 * @param indice of cell
	 * @param value the potential value
	 * @param isOn true for an ON, false for an OFF
	 * @param parent the parent assumption (is true for me to apply); or<br>
	 *  null means my parents remain empty (usually the initial assumption)
	 */
	public Ass(final int indice, final int value, final boolean isOn
			, final Ass parent) {
		this.indice = indice;
		this.value = value;
		this.isOn = isOn;
		if ( isOn )
			hashCode = ON_BIT ^ ASHFT[value] ^ indice;
		else
			hashCode = ASHFT[value] ^ indice;
		// Effs have parents even if the given parent is null!
		this.parents = new MyLinkedList<>();
		if ( parent != null )
			this.parents.linkLast(parent);
		this.firstParent = parent;
		this.cause = null;
		this.explanation = null;
		this.nestedChain = null;
	}

	/**
	 * Returns a new Ass with my isOn field negated.
	 *
	 * @return new Ass(cell, value, !isOn)
	 */
	public Ass flip() {
		return new Ass(indice, value, !isOn);
	}

	/**
	 * Does this assumption have any parents?
	 * <p>
	 * The contract is that a parents list may be null but NEVER empty. An ass
	 * that is created without a parent is given a null parents list, and then
	 * the parents list is created if/when the first parent is added to it; to
	 * save checking for an empty parents list BILLIONS of times.
	 *
	 * @return {@code parents!=null && parents.size()>0}
	 */
	public boolean hasParents() {
//		return parents!=null && parents.size>0;
		return firstParent != null;
	}

	/**
	 * Add the given parent to this Assumptions list of parents. Caters for a
	 * parent being added to an Ass that was created with a null parent, and
	 * therefore has a null parents list.
	 * <p>
	 * Im now ONLY used in UnaryChainer.reverseCycle, where Im essential.
	 * UnaryChainer.reverseCycle is ONLY place where new parents list created;
	 * and he only in createBidirectionalCycleHint (ie not too often) so there
	 * is nothing to worry about, except ALL other calls to addParent should
	 * call parents.linkLast(parent) directly, which I do now with a... sigh.
	 *
	 * @param parent Ass to add
	 * @return always returns true, and does NOT throw any RuntimeExceptions.
	 */
	public boolean addParent(final Ass parent) {
		assert parent != null;
		if ( firstParent == null ) {
			firstParent = parent;
			parents = new MyLinkedList<>();
		}
		parents.linkLast(parent);
		return true;
	}

	/** @return {@code cell.id+"+/-"+value} */
	public String contra() {
		return CELL_IDS[indice]+PLUS_OR_MINUS+value;
	}

	/**
	 * Stringify this assumption using linguistically weaker language.
	 *
	 * @return "A1 (contains/does not contain) the value 1" depending on isOn.
	 */
	public String weak() {
		// nb: if you change the result then you also need to do a search for
		//     sourceWeakPrefix and change it as well.
		// nb: terniaries are fast enough when were building html
		return CELL_IDS[indice]
			 + (isOn ? " contains" : " does not contain")
			 + " the value " + value;
	}

	/**
	 * Creating a new Ass just to get its weak string was dumb!
	 * <pre>
	 * , (src.isOn ? src : src.flip()).weak()	// if {0} then {2}
	 * , (src.isOn ? src.flip() : src).weak()	// if {1} then {2}
	 * </pre>
	 * <p>becomes
	 * <pre>
	 * , src.weak(true)		// if {0} then {2}
	 * , src.weak(false)	// if {1} then {2}
	 * </pre>
	 *
	 * @param parity boolean true for isOn, else false. You will call me
	 *  once for each parity (as per the above) for each contradiction.
	 * @return "A1 (contains/does not contain) the value 1"
	 *  depending on isOn==parity.
	 */
	public String weak(final boolean parity) {
		// nb: terniaries are fast enough when were building html
		return CELL_IDS[indice]
			 + (isOn==parity ? " contains" : " does not contain")
			 + " the value " + value;
	}

	/**
	 * Stringify this assumption using linguistically stronger language.
	 *
	 * @return "A1 (must/cannot) contain the value 1" depending on isOn.
	 */
	public String strong() {
		return CELL_IDS[indice]
			 + (isOn ? " must contain" : " cannot contain")
			 + " the value " + value;
	}

	/**
	 * toString is now used in a anger:
	 * {@link diuf.sudoku.solver.hinters.chain.AChainingHint#getChainHtml(diuf.sudoku.Ass) }
	 *
	 * @return {@code cell.id+(isOn?PLUS:MINUS)+value}
	 */
	@Override
	public String toString() {
		if ( ts != null )
			return ts;
		return ts = CELL_IDS[indice] + (isOn?PLUS:MINUS) + value;
	}
	private String ts;

	/**
	 * Is this Object an Ass with the same hashCode (isOn|value|cell) as me?
	 * <p>
	 * <b>WARNING</b>: The fact that Ass.equals just equates hashCodes is
	 * relied upon by the getCell methods of all IAssSet implementations.
	 * So review them <b>BEFORE</b> you change this, and dont change this!
	 *
	 * @param o the other Object to equate this with
	 * @return boolean are these two Asses equivalent? Cheeky!
	 */
	@Override
	public boolean equals(final Object o) {
		return o instanceof Ass && ((Ass)o).hashCode == hashCode;
	}

	/**
	 * Does my hashCode equal that of the given Ass.
	 * <pre>
	 * Ass.hashCode is (atypically) distinct.
	 * Ass.hashCode uniquely identifies ONE specific assumption.
	 * Ass.hashCode serves as the identity of an assumption.
	 * Two Asses with the same hashCode are the same assumption.
	 * Two Asses with different hashCodes are NOT the same assumption.
	 * Every Ass assuming $cell+/-$value has the same hashCode. Lineage varies!
	 * </pre>
	 *
	 * @param a
	 * @return
	 */
	public boolean equals(final Ass a) {
		return a.hashCode == hashCode;
	}

	/** @return {@code (isOn?1<<11:0)^value<<7^cell.i} */
	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public int compareTo(final Ass other) {
		return hashCode - other.hashCode;
	}

}
