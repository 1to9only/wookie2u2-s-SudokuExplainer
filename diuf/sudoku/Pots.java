/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.MAYBES;
import static diuf.sudoku.Grid.MAYBES_STR;
import static diuf.sudoku.Grid.RIBS;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Values.SET_BIT;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.IntQueue;
import static diuf.sudoku.utils.IntQueue.QEMPTY;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Pots is short for Potentials, which means potential values: a set of cells,
 * each mapped to its interesting Values bitset. I am "Pots" for brevity.
 * <pre>
 * 2022 sometime IIRC, Extending {@code MyLinkedHashMap<Cell, Values>} adds a
 * few handy operations to what is otherwise a standard {@code java.utils.Map}.
 *
 * 2023-10-26 extends TreeMap especially for faster entrySet, and also more
 * memory efficient. There's LOTS of Pots, many of which have one entry, and
 * 99% are "small" sets, every one with a table of 8, wasting RAM. TreeMap
 * seems more efficient. It may not be, coz its Entry is larger.
 * </pre>
 *
 * @author Keith Corlett 2013
 */
public final class Pots extends TreeMap<Integer, Integer> {

	private static final long serialVersionUID = 947665385L;

	public static final Set<String> CRAP = new HashSet<>();

	/** The initialCapacity of the smallest MyLinkedHashMap that I extend. */
	public static final int MIN_CAPACITY = 8;
	/** THE empty Pots (immutable please). */
	public static final Pots EMPTY = new Pots(MIN_CAPACITY, 1F);

	/** Self-documenting index in toArray result: int[][thisOne]. */
	public static final int INDICE = 0;
	/** Self-documenting index in toArray result: int[][thisOne]. */
	public static final int VALUES = 1;

	// make the dummy parameters more obvious
	private static final boolean DUMMY = false;

	/**
	 * Parse $s (toString format) into a new Pots.
	 *
	 * @param grid to take cells from
	 * @param s a Pots string in the same format as {@link #toString}, from
	 *  end of {@link diuf.sudoku.solver.AHint#toFullString} in parentheses.
	 * @return a new Pots
	 */
	public static Pots parse(final Grid grid, final String s) {
		final Pots pots = new Pots();
		// "B7+6 B7-6" is two, but they not in the same Pots -> INGORE!
		if ( s.length() == 9 ) {
			final Pattern pat = Pattern.compile("[A..I]\\d[+-]\\d [A..I].*");
			if ( pat.matcher(s).matches() )
				return pots;
		}
		for ( String field : s.split(", ") )
			pots.put(Grid.indice(field.substring(0, 2)), Values.parse(field.substring(3)));
		return pots;
	}

	// NB: gets the corresponding cell in the given (current) $grid
	//     because in a nested hint a.cell is in a different Grid
	public static Pots of(final Collection<Ass> asses) {
		if ( asses==null || asses.isEmpty() )
			return null;
		final Pots result = new Pots();
		asses.forEach((e) -> result.upsert(e.indice, e.value, DUMMY));
		return result;
	}

	// --------------------------- end of statics  ----------------------------

	/**
	 * for {@link #upsertAll(int, int, int, Grid, int, boolean) }
	 * and {@link #upsertAny(Pots) }
	 */
//	private boolean any;

	/** for {@link #hashCode() } */
	private boolean hashCodeSet;
	/** for {@link #hashCode() } */
	private int hashCode;

	/**
	 * Constructor.
	 */
	public Pots() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param initialCapacity
	 */
	public Pots(final int initialCapacity) {
		super();
	}

	/**
	 * Constructs a new Pots that is a copy of $src Pots.
	 *
	 * @param src
	 */
	public Pots(final Pots src) {
		super(src);
	}

	/**
	 * Constructs a new Pots (ie a HashMap) with the given $initialCapacity and
	 * $loadFactor.
	 *
	 * @param initialCapacity size of the underlying array of linked-lists.<br>
	 *  initialCapacity has a practical lower bound (enforced here) of 8
	 * @param loadFactor when the {@code this.size() * loadFactor} exceeds the
	 *  capacity (array length) then {@code capacity = capacity * 1.5}, growing
	 *  the underlying array, and redistributing existing entries into it.
	 *  Growth is inherently an O(n) operation EACH TIME, so should be avoided.
	 *  The {@code size()} is the number of entries herein (not the number of
	 *  elements in the array, which confused me, once upon a time, like today).
	 */
	public Pots(final int initialCapacity, final float loadFactor) {
		super();
	}

	/**
	 * Constructs a new Pots containing the given $cell =&gt; $value.
	 *
	 * @param indice of the Cell that may be this value
	 * @param value a potential value of cell
	 */
	public Pots(final int indice, final int value) {
		this(indice, VSHFT[value], DUMMY);
	}

	/**
	 * Constructs a new Pots containing the given $cell => $cands.
	 *
	 * @param indice of the Cell that may be this value
	 * @param cands a bitset of the values associated with cell
	 * @param dummy nothing to see here
	 */
	public Pots(final int indice, final int cands, final boolean dummy) {
		super();
		put(indice, cands);
	}

	/**
	 * Constructs a new Pots of $cells =&gt; $value.
	 * <p>
	 * Varargs is fast enough here, because I'm only used when hinting, so I'm
	 * NOT hammered; but if you construct Pots as part of your search, or any
	 * other place that executes often, then it WILL be faster to use another
	 * constructor. Varargs is "too slow" for heavy use. Each invocation has a
	 * small additional cost, which quickly adds-up.
	 *
	 * @param value the plain value of each cell in cells.
	 * @param cells to put
	 */
	public Pots(final int value, final Cell... cells) {
		super();
		final int sv = VSHFT[value];
		for ( Cell c : cells )
			if((c.maybes & sv)>0) put(c.indice, sv);
	}

	/**
	 * Constructor for all maybes of all of these cells.
	 *
	 * @param cells
	 */
	public Pots(final Cell[] cells) {
		super();
		for ( Cell c : cells )
			put(c.indice, c.maybes);
	}

	/**
	 * Constructs a new Pots containing the given $cells =&gt; $value.
	 * <p>
	 * nb: This method takes a {@code Cell[]} to avoid varargs overheads.
	 *
	 * @param cells to add to the new Pots
	 * @param value of each cell in the new Pots
	 */
	public Pots(final Cell[] cells, final int value) {
		this(cells, VSHFT[value], DUMMY);
	}

	/**
	 * Constructs a new Pots containing the given $cells =&gt; $value.
	 * <p>
	 * This method takes a {@code Cell[]} to avoid varargs overheads.
	 *
	 * @param cells {@code Cell[]} to put.
	 * @param cands bitset of new Values, which is put.
	 * @param dummy to spit if and when I feel like it.
	 */
	public Pots(final Cell[] cells, final int cands, final boolean dummy) {
		super();
		int mine;
		for ( final Cell cell : cells )
			if ( (mine=cell.maybes & cands) > 0 ) // 9bits
				put(cell.indice, mine);
	}

	/**
	 * Constructor for (maybes & values) of first n cells.
	 * If a cell has no values then it is NOT added.
	 *
	 * @param cells
	 * @param n
	 * @param cands
	 * @param dummy
	 */
	public Pots(final Cell[] cells, final int n, final int cands, final boolean dummy) {
		super();
		Cell cell;
		for ( int myCands,i=0; i<n; ++i )
			if ( (myCands=(cell=cells[i]).maybes & cands) > 0 ) // 9bits
				put(cell.indice, myCands);
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in $cells =&gt;
	 * the intersection of each cell.maybes with the given $cands.
	 *
	 * @param cells to pot
	 * @param indexes a bitset of indexes of cells to pot
	 * @param cands a bitset of the values to add
	 * @param dummy1 a dummy parameter.
	 * @param dummy2 a dummy parameter.
	 */
	public Pots(final Cell[] cells, final int indexes, final int cands
			, final boolean dummy1, final boolean dummy2) {
		super();
		Cell cell; int mine;
		for ( int i : INDEXES[indexes] )
			if ( (mine=(cell=cells[i]).maybes & cands) > 0 ) // 9bits
				put(cell.indice, mine);
	}

	/**
	 * Construct a new Pots of idx cells in grid to cands.
	 *
	 * @param idx indices of cells to add
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param v the plain value to eliminate (not cands)
	 */
	public Pots(final Idx idx, final int[] gridMaybes, final int v) {
		super();
		upsertAll(idx.m0, idx.m1, gridMaybes, VSHFT[v], DUMMY);
	}

	/**
	 * Construct a new Pots of idx cells in grid to cands.
	 *
	 * @param idx indices of cells to add
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param cands a bitset of the values to eliminate. The result contains
	 *  only cands that exist in each cell
	 * @param dummy says this method takes cands (not value)
	 */
	public Pots(final Idx idx, final int[] gridMaybes, final int cands, final boolean dummy) {
		super();
		upsertAll(idx.m0,idx.m1, gridMaybes, cands, dummy);
	}

	/**
	 * Construct a new Pots of idx cells in grid to cands.
	 * This one checks each cands exists in each cell.
	 *
	 * @param m0 mask 0: 54bit long
	 * @param m1 mask 1: 27bit int
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param cands a bitset of the values to eliminate. The result contains
	 *  only cands that exist in each cell
	 * @param dummy says this method takes cands (not value)
	 */
	public Pots(final long m0, final int m1, final int[] gridMaybes, final int cands, final boolean dummy) {
		super();
		upsertAll(m0,m1, gridMaybes, cands, dummy);
	}

	/**
	 * Construct a new Pots of idx cells in grid to cands.
	 * This one DOES NOT check each cands exists in each cell.
	 *
	 * @param m0 mask 0: 54bit long
	 * @param m1 mask 1: 27bit int
	 * @param cands a bitset of the values to eliminate. The result contains
	 *  only cands that exist in each cell
	 * @param dummy says this method takes cands (not value)
	 */
	public Pots(final long m0, final int m1, final int cands, final boolean dummy) {
		super();
		upsertAll(m0,m1, cands, dummy);
	}

	/**
	 * Construct a new Pots of hints cell-values.
	 *
	 * @param hints to convert back into Pots. These must ALL be direct hints,
	 *  with the cell field set, or you will get an NPE
	 */
	public Pots(final Iterable<AHint> hints) {
		hints.forEach((h)->put(h.cell.indice, VSHFT[h.value]));
	}

	/**
	 * Clear all the Cell=&gt;Values from this Pots and return this Pots.
	 *
	 * @return this Pots
	 */
	public Pots clearMe() {
		clear();
		return this;
	}

	public boolean any() {
		return size() > 0;
	}

	@Override
	public Integer get(final Object key) {
		if ( key instanceof Integer )
			return super.get(key);
		if ( key instanceof Cell ) {
			CRAP.add(StackTrace.of(new RuntimeException()));
			return super.get(((Cell)key).indice);
		}
		throw new IllegalArgumentException(
				"Bad key "+key.getClass().getCanonicalName()+": "+key);
	}

	/**
	 * Does this Pots contain this Assumption?
	 *
	 * @param a the Ass to examine
	 * @return {@code (get(a.indice) & VSHFT[a.value]) > 0} handling null.
	 */
	public boolean has(final Ass a) {
		final Integer cands = get(a.indice);
		return cands!=null && (cands & VSHFT[a.value]) > 0; // 9bits
	}

	/**
	 * Overload put, translating cellId into indice, for the test-cases.
	 * <p>
	 * WARN: NOT for real code. Grid.indice is slow. Don't hammer me!
	 *
	 * @param cellId
	 * @param value
	 * @return existing value, if any
	 */
	public Integer put(final String cellId, final Integer value) {
		return super.put(Grid.indice(cellId), value);
	}

	/**
	 * The copyAndClearMusicFactory makes a shallow copy of this (your reds),
	 * clears this, and returns the copy.
	 * <p>
	 * copyAndClear is used on redPots, when a redPots field is used to avoid
	 * repeatedly creating a new Pots in each invocation when it almost always
	 * remains empty, because the hinter does not find any eliminations. So we
	 * have ONE redPots field which we add eliminations to, then copy-them-off
	 * when we create a hint. Pretty obviously, this is all about speed.
	 *
	 * @return a copy of this (theReds) which is then cleared
	 */
	public Pots copyAndClear() {
		final Pots copy = new Pots(this);
		this.clear();
		return copy;
	}

	/**
	 * Add all these others to this Pots.
	 *
	 * @param others to be added
	 * @return this Pots for method chaining
	 */
	public Pots addAll(final Pots others) {
		// do nothing if others is null, and head-off adding myself to myself,
		// and if others is empty then likewise there is nothing to see here.
		if ( others!=null && others!=this && !others.isEmpty())
			others.entrySet().forEach((e) -> upsert(e.getKey(), e.getValue(), DUMMY));
		return this;
	}

	/**
	 * Add all these others to this Pots.
	 *
	 * @param others to be added
	 * @return this Pots for method chaining
	 */
	public boolean addAllAny(final Pots others) {
		boolean result = false;
		if ( others==null || others==this )
			return result;
		for ( Map.Entry<Integer, Integer> e : others.entrySet() )
			result |= upsert(e.getKey(), e.getValue(), DUMMY);
		return result;
	}

	/**
	 * putAll (COPYING Values) from the given $map, and return this Pots.
	 * <p>
	 * IMHO this is how putAll should be, for safety.
	 *
	 * @param map A Pots to putAll into this Pots.
	 * @return this Pots.
	 */
	public Pots putAll2(final Map<? extends Integer, ? extends Integer> map) {
		map.entrySet().forEach((e) -> put(e.getKey(), e.getValue()));
		return this;
	}

	/**
	 * This method exists to override supers putAll to force developers (who
	 * always run with -ea) to call putAll2 instead, else (in production) I
	 * just delegate to putAll2.
	 *
	 * @param map
	 */
	@Override
	public void putAll(final Map<? extends Integer, ? extends Integer> map) {
		assert false : "use putAll2 instead, coz it is SAFE!"; // techies only
		putAll2(map);
	}

	/**
	 * upsert: Update-or-insert these $cands (a bitset of candidate values).
	 * If $cell is already in this Pots then add cands to its values; else
	 * (the cell is not in this Pots) then create a new entry for $cell
	 * containing the given $cands.
	 *
	 * @param indice of Cell key to upsert
	 * @param cands a bitset of the values to be upserted; not 0 dopey
	 * @param dummy nothing to see here
	 * @return true if this Pots was modified:
	 *  true if the cell is added;
	 *  true if a value is added to an existing cell;
	 *  false if cell exists and no new values added
	 */
	public boolean upsert(final int indice, final int cands, final boolean dummy) {
		// if there are no values to add to this cell do NOT insert the cell.
		// This check is delayed until last moment for convenience upstairs.
		// It happens rarely enough for invocation to not be a big speed-drag.
		if ( cands == 0 )
			return false;
		final Integer existing = get(indice);
		if ( existing == null )
			return put(indice, cands) == null;
		final int aggregate = existing | cands;
		return aggregate != existing
			&& put(indice, aggregate) != null;
	}

	/**
	 * Upsert all the other Pots.
	 *
	 * @param src Pots to be upserted.
	 * @return this Pots.
	 */
	public Pots upsertAll(final Pots src) {
		if ( src != null )
			src.entrySet().forEach(
				(e) -> upsert(e.getKey(), e.getValue(), DUMMY)
			);
		return this;
	}

	/**
	 * Upsert all the other Pots.
	 *
	 * @param src Pots to be upserted.
	 * @return this Pots.
	 */
	public boolean upsertAny(final Pots src) {
		boolean any = false;
		if ( src != null )
			for ( Map.Entry<Integer,Integer> e : src.entrySet() )
				any |= upsert(e.getKey(), e.getValue(), DUMMY);
		return any;
	}

	/**
	 * Upsert (update or insert) each cell in idx which maybe(v).
	 * Note that this method skips cands that arent maybes.
	 *
	 * @param m0 mask 0: 54 bit long
	 * @param m1 mask 1: 27 bit int
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param cands VSHFT[value] to add for each cell, if cell.maybe(v),
	 *  else this cell is skipped.
	 * @param dummy unused marker to say cands (a bitset), not values (plain)
	 * @return any changes.
	 */
	public boolean upsertAll(final long m0, final int m1, final int[] gridMaybes, final int cands, final boolean dummy) {
		boolean any = false;
		if ( (m0|m1) > 0L ) {
			int i;
			for ( final IntQueue q=new Idx.MyIntQueue(m0,m1); (i=q.poll())>QEMPTY; )
				any |= (gridMaybes[i] & cands) > 0
					&& upsert(i, gridMaybes[i] & cands, DUMMY);
		}
		return any;
	}

	/**
	 * Upsert (update or insert) each cell in indices which maybe(v).
	 * Note that this method skips cands that arent maybes.
	 *
	 * @param indices of cells to add to this Pots
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param cands VSHFT[value] to add for each cell, if cell.maybe(v),
	 *  else this cell is skipped.
	 * @param dummy unused marker to say cands (a bitset), not values (plain)
	 * @return any changes.
	 */
	public boolean upsertAll(final int[] indices, final int[] gridMaybes, final int cands, final boolean dummy) {
		boolean any = false;
		for ( int i : indices )
			any |= (gridMaybes[i] & cands) > 0
				&& upsert(i, gridMaybes[i] & cands, DUMMY);
		return any;
	}

	/**
	 * Upsert (update or insert) each cell in idx with cands.
	 * Note that this method does NOT skip cands that arent maybes!
	 *
	 * @param m0 mask 0: 54bit long
	 * @param m1 mask 1: 27bit int
	 * @param cands VSHFT[value] to add for each cell, if cell.maybe(v),
	 *  else this cell is skipped.
	 * @param dummy unused marker to say cands (a bitset), not values (plain)
	 * @return any changes.
	 */
	public boolean upsertAll(final long m0, final int m1, final int cands, final boolean dummy) {
		boolean any = false;
		if ( (m0|m1) > 0L ) {
			int i;
			for ( final IntQueue q=new Idx.MyIntQueue(m0,m1); (i=q.poll())>QEMPTY; )
				any |= upsert(i, cands, DUMMY);
		}
		return any;
	}

	/**
	 * Upsert (update or insert) each cell in idx which maybe(v).
	 * Update means v is added to the existing values for this Cell.
	 * Insert means the cell is added with the given value.
	 * If a cell in $idx is not maybe(v) then it is skipped.
	 *
	 * @param idx containing indices of cells to be (probably) added
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param v the value to add for each cell, presuming that cell.maybe(v),
	 *  else this cell is skipped.
	 * @return any changes.
	 */
	public boolean upsertAll(final Idx idx, final int[] gridMaybes, final int v) {
		return upsertAll(idx.m0,idx.m1, gridMaybes, VSHFT[v], DUMMY);
	}

	/**
	 * Upsert (update or insert) each cell in idx which maybe(v).
	 * Update means v is added to the existing values for this Cell.
	 * Insert means the cell is added with the given value.
	 * If a cell in $idx is not maybe(v) then it is skipped.
	 *
	 * @param idx containing indices of cells to be (probably) added
	 * @param gridMaybes grid.maybes of the grid we search
	 * @param cands bitset of values to add for each cell
	 * @param dummy differentiate this cands version from the value version.
	 * @return any changes.
	 */
	public boolean upsertAll(final Idx idx, final int[] gridMaybes, final int cands, final boolean dummy) {
		return upsertAll(idx.m0,idx.m1, gridMaybes, cands, DUMMY);
	}

	/**
	 * insertOnly ONLY inserts the given cell in this Pots (for a setPots).
	 * If the cell exists then throws a IToldHimWeveAlreadyGotOneException,
	 * even if the new value is the same as the existing value!
	 *
	 * @param indice identifying cell to insert
	 * @param values of this cell
	 * @throws IToldHimWeveAlreadyGotOneException if the cell already exists
	 *  in this Pots.
	 */
	public void insertOnly(final int indice, final int values) {
		final Integer existing = get(indice);
		if ( existing != null )
			throw new IToldHimWeveAlreadyGotOneException(CELL_IDS[indice]+PLUS+existing+PLUS+MAYBES_STR[values]);
		put(indice, values);
	}

	/**
	 * Remove all these cells from these Pots.
	 *
	 * @param cells to remove.
	 */
	public void removeAll(final Iterable<Cell> cells) {
		if ( cells != null )
			for ( Cell cell : cells )
				remove(cell.indice);
	}

	/**
	 * Remove each value in each cell in others from this Pots and if that
	 * leaves a cell (key) with no values (data) then cell is also removed.
	 * Note that this may result in an empty set!
	 *
	 * @param others
	 * @return
	 */
	public Pots removeAll(final Pots others) {
		if ( others != null ) {
			Integer mine;
			for ( java.util.Map.Entry<Integer,Integer> e : others.entrySet() )
				// remove others values from my values, and if that leaves my
				// values empty then remove my cell also
				if ( (mine=get(e.getKey()))!=null ) {
					final int now;
					put(e.getKey(), now=mine & ~e.getValue());
					if ( now == 0 )
						remove(e.getKey());
				}
		}
		return this; // may be empty
	}

	/**
	 * Remove these pots from all of the given potss.
	 *
	 * @param potss
	 */
	public void removeFromAll(final Pots... potss) {
		if ( isEmpty() )
			return;
		for ( Pots pots : potss )
			if ( pots != null )
				pots.removeAll(this);
	}

	/**
	 * Returns a new Pots of: the same value appears in the same cell in this
	 * and the other Pots.
	 *
	 * @param others
	 * @return
	 */
	public Pots intersection(final Pots others) {
		Integer indice, myValues, otherValues;
		int commonValues; // values common to this cell in both me and others
		final Pots result = new Pots();
		for ( java.util.Map.Entry<Integer,Integer> e : entrySet() )
			if ( (myValues=e.getValue()) > 0 // 9bits
			  // but otherValue pretty obviously can be null.
			  && (otherValues=others.get(indice=e.getKey())) != null
			  // are there any common values?
			  && (commonValues=myValues & otherValues) > 0 ) // 9bits
				// add the common values to the result.
				result.put(indice, commonValues);
		return result; // which may still be empty!
	}

	/** @return the total of the sizes of all the values in these Pots. */
	public int totalSize() {
		int result = 0;
		for ( int value : values() )
			result += VSIZE[value];
		return result;
	}

	/**
	 * Returns an aggregate all values of all cells in this Pots as a bitset.
	 *
	 * @return a bitset of all the values in this pots.
	 */
	public int candsOf() {
		int result = 0;
		for ( int cands : values() )
			result |= cands;
		return result;
	}

	/**
	 * Is this Pots a subset of these other Pots?<br>
	 * ie Does other contain all eliminations in this Pots?
	 *
	 * @param others
	 * @return
	 */
	public boolean isSubsetOf(final Pots others) {
		Integer myValues;
		for ( java.util.Map.Entry<Integer,Integer> e : others.entrySet() )
			if ( (myValues=get(e.getKey())) == null
			  || (e.getValue() & myValues) != myValues )
				return false;
		return true;
	}

	/**
	 * Returns true if any of my eliminations (I am a reds) are still in the
	 * given $grid.maybes; else false meaning "Yeah, nah", ergo your hint is
	 * completely stale, old, gorn-off, dead, totes parrotosis, useless, and
	 * has no on-going value.
	 *
	 * @param grid the current grid
	 * @return are any of my value/s still in the cell?
	 */
	public boolean anyCurrent(final Grid grid) {
		final int[] maybes = grid.maybes;
		for ( java.util.Map.Entry<Integer,Integer> e : entrySet() )
			if ( (maybes[e.getKey()] & e.getValue()) > 0 )
				return true;
		return false;
	}

	/**
	 * Set the values of the cells in this pots. This pots is presumed to be
	 * a setPots, not a normal "redPots", or greens, or anything stupid.
	 *
	 * @param isAutosolving
	 * @param grid
	 * @return
	 */
	public int setCells(final boolean isAutosolving, final Grid grid) {
		Integer indice;
		int sv, numSet = 0;
		final Cell cells[] = grid.cells;
		for ( java.util.Map.Entry<Integer,Integer> e : entrySet() ) {
			indice = e.getKey();
			sv = e.getValue();
			assert VSIZE[sv] == 1; // ONE value per cell in a setPots!
			if ( Run.isBatch() && !isAutosolving )
				numSet += cells[indice].set(VFIRST[sv]);
			else
				numSet += cells[indice].set(VFIRST[sv], 0, isAutosolving, null);
		}
		return numSet;
	}

	/**
	 * Returns CSV of "$cell.id-$values"
	 *
	 * @return
	 */
	@Override
	public String toString() {
		final int n = size();
		if ( n == 0 )
			return "";
		if ( n == 1 ) {
			final Integer indice = firstKey();
			return CELL_IDS[indice]+MINUS+MAYBES_STR[get(indice)];
		}
		// select "A1-49" from this
		final String sep = CSP;
		final int sepLen = sep.length();
		int i = 0, totalLength = 0;
		final String[] array = new String[n];
		final StringBuilder sb = SB(16);
		for ( java.util.Map.Entry<Integer,Integer> e : entrySet() ) {
			sb.setLength(0);
			array[i] = sb.append(CELL_IDS[e.getKey()]).append(MINUS)
					   .append(MAYBES[e.getValue()]).toString();
			totalLength += array[i++].length() + sepLen;
		}
		// order by cell.id
		Arrays.sort(array);
		// stringify the array
		final StringBuilder result = SB(totalLength);
		result.append(array[0]);
		for ( i=1; i<n; ++i )
			result.append(sep).append(array[i]);
		return result.toString();
	}

	/**
	 * Returns an int[size()][2] (an array of int-arrays) of this Pots.
	 * <pre>
	 * In [][thisOne] the first element [0] is indice, and second element [1]
	 * is values, so I expose two self-documenting index constants:
	 * * {@link #INDICE} (0) is the cell indice, and
	 * * {@link #VALUES} (1) is a bitset of cells values
	 * </pre>
	 * <p>
	 * Note that its a bit faster to iterate an array than a Set, but not much
	 * faster, and I must iterate the Set in order to create the array, which
	 * also takes time to allocate, so toArray is faster instead of REPEATEDLY
	 * iterating the Set. If you iterate it ONCE then just iterate it yourself.
	 * If you can cache the array (immutable Pots) then it starts to pay-back.
	 * Multi-dimensional array creation is a bit slow in Java. I know not why.
	 *
	 * @return a new int[size()][2]
	 */
	public int[][] toArray() {
		final int[][] array = new int[size()][2];
		int i = 0;
		for ( Map.Entry<Integer, Integer> g : entrySet() ) {
			array[i][INDICE] = g.getKey();
			array[i][VALUES] = g.getValue();
			++i;
		}
		return array;
	}

	/**
	 * Returns SSV (Space Separated Values) of the ids of cells in this Pots.
	 * <p>
	 * For example: "A4 B4 H4"
	 *
	 * @return SSV of my cell-ids.
	 */
	public String ids() {
		return Frmu.ids(keySet());
	}

	public int commonRibs() {
		int crs = 0;
		boolean first = true;
		for ( int indice : keySet() ) {
			if ( first ) {
				first = false;
				crs = RIBS[indice];
			} else {
				crs &= RIBS[indice];
			}
		}
		return crs;
	}

	/**
	 * WARN: Pots partially upholds the {@link java.lang.Object#hashCode}
	 * contract! All good if Pots remains unchanged once used as a key.
	 * <p>
	 * I am caching hashCode coz calculating it is an O(n) operation, and
	 * hashCode is hammered once you use a Pots as a key in a Map, which
	 * I now do in ComplexFisherman. sigh.
	 * <p>
	 * Note that caching the hashCode implies that a Pots must NOT be modified
	 * after it is been used as a Hash key. If you do modify a Pots after you
	 * have put it as a key, then the equals-hashCode contract is broken, so
	 * your code (most probably) will not behave consistently, all-though it
	 * may do on occasion, just to keep you guessing. sigh.
	 *
	 * @return the CACHED hash-code of this Pots
	 */
	@Override
	public int hashCode() {
		if ( !hashCodeSet ) {
			hashCode = super.hashCode();
			hashCodeSet = true;
		}
		return hashCode;
	}

	/**
	 * Does this pots containsAll every cell and every value in others?
	 *
	 * @param him to test
	 * @return this contains everything in others
	 */
	public boolean contains(final Pots him) {
		Integer mine, his;
		for ( Map.Entry<Integer, Integer> he : him.entrySet() ) {
			if ( (mine=get(he.getKey())) == null )
				return false;
			his = he.getValue();
			if ( (mine & his) != his )
				return false;
		}
		return true;
	}

	/**
	 * Does this Pots equal the given Object? Returns true if obj is a Pots
	 * (or subclass thereof) with the same hashCode as this Pots
	 * <p>
	 * WARN: Pots partially upholds the {@link java.lang.Object#hashCode}
	 * contract! All good if Pots remains unchanged once used as a key.
	 * <p>
	 * See {@link #hashCode()}.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(final Object o) {
		return o instanceof Pots && equals((Pots)o);
	}
	// NOTE that we MUST call the hashCode() method, not just reference the
	// hashCode field because we use this method to equals Pots in the test
	// cases, and (unlike the rest of the codebase) that is the first time
	// that hashCode() is called, so the testcase comparisons (which I only
	// sort-of rely-on for correctness) significantly slow down the actual
	// code which can, AFAIK, get away with using hashCode! FFMFS! F! F! F!
	public boolean equals(final Pots o) {
		return hashCode() == o.hashCode();
	}

	public int firstCellIndice() {
//		for ( Integer indice : keySet() )
//			return indice;
//		return 0; // something other than -1
		return super.firstKey();
	}

	/**
	 * Translate this Pots into an Idx per value.
	 *
	 * @param result Idx[VALUE_CEILING], one for each value 1..9 (0 is not referenced).
	 */
	public void toIdxs(final Idx[] result) {
		int v=1; do result[v].clear(); while(++v<VALUE_CEILING);
		entrySet().forEach((e) -> {
			final int indice = e.getKey();
			for ( int v2 : VALUESES[e.getValue()] )
				result[v2].add(indice);
		});
	}

	/**
	 * Count the number of values in this Pots.
	 *
	 * @return the number of values in this Pots
	 */
	public int count() {
		int count = 0;
		for ( Map.Entry<Integer, Integer> e : entrySet() )
			count += VSIZE[e.getValue()];
		return count;
	}

	/**
	 * Get cells in this Pots as an Idx.
	 *
	 * @return a new Idx of cells in this Pots.
	 */
	public Idx idx() {
		final Idx result = new Idx();
		for ( int indice : keySet() )
			result.add(indice);
		return result;
	}

	public Integer firstValue() {
		return get(super.firstKey());
	}

	/**
	 * Are there any Ons in this Pots, ie is this Pots an On results.
	 * <p>
	 * FYI, When Pots is used as Results then the Ons values are differentiated
	 * by setting the SET_BIT in the values field. On Results, by definition,
	 * have ONE value per indice. This is the value that the cell is set to
	 * when this hint is applied. It is expected that Results are either all
	 * Ons or all Offs, never a mixed-bag.
	 *
	 * @return Does this pots contain any SET_BIT
	 */
	public boolean anyOns() {
		for ( int value : values() )
			if ( (value & SET_BIT) > 0 )
				return true;
		return false;
	}

	public static class IToldHimWeveAlreadyGotOneException extends IllegalStateException {
		private static final long serialVersionUID = 516409800239L;
		public IToldHimWeveAlreadyGotOneException(String msg) {
			super(msg);
		}
	}

}
