/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.MyLinkedHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Pots means potential values: a set of cells, each mapped to its interesting
 * Values. I'm just called "Pots" for brevity.
 * <p>
 * Extending {@code MyLinkedHashMap<Cell, Values>} allows us to add a few
 * handy operations to what is otherwise a standard <tt>java.utils.Map</tt>.
 *
 * @author Keith Corlett 2013
 */
public final class Pots extends MyLinkedHashMap<Cell, Values> {
	private static final long serialVersionUID = 947665385L;

	/** the size of smallest LinkedHashMap I create. */
	public static final int MIN_CAPACITY = 8;
	/** the EMPTY Pots is currently used only by test-cases. */
	public static final Pots EMPTY = new Pots(MIN_CAPACITY, 1F);

	public static class IToldHimWeveAlreadyGotOneException extends IllegalStateException {
		private static final long serialVersionUID = 516409800239L;
		public IToldHimWeveAlreadyGotOneException(String msg) {
			super(msg);
		}
	}

	/**
	 * deepCopy copies the src and all of it's Values, so that the returned
	 * copy is mutatable independently of the source. The key-cells are same
	 * objects, but then there's never a need to have more than one instance
	 * of any Cell object; there's only ever 81 of them in the Grid. So yeah,
	 * new Values, so you can ____ with them.
	 * @param src
	 * @return
	 */
	public static Pots deepCopy(Pots src) {
		Pots copy = new Pots();
		for ( Cell c : src.keySet() )
			copy.put(c, new Values(src.get(c)));
		return copy;
	}

	/**
	 * Make a shallow copy of this (your redPots), clear this, and return the
	 * copy.
	 * <p>
	 * copyAndClear is used on redPots, when a redPots field is used to avoid
	 * repeatedly creating a new Pots in each call when it just remains empty
	 * in most calls, because the hinter doesn't find any eliminations. So we
	 * create ONE redPots field, add eliminations to it, and then copy-it-off
	 * when we create the hint.
	 *
	 * @return a copy of this (theReds) which is then cleared
	 */
	public Pots copyAndClear() {
		Pots copy = new Pots(this);
		this.clear();
		return copy;
	}

	/** Constructor. */
	public Pots() {
		super(MIN_CAPACITY, 1F);
	}

	/** Constructor.
	 * @param initialCapacity */
	public Pots(int initialCapacity) {
		super(Math.max(initialCapacity,MIN_CAPACITY), 1F);
	}

	/**
	 * Copy Constructor: Constructs a new Pots containing a copy of the
	 * given 'src' Pots.
	 * @param src
	 */
	public Pots(Pots src) {
		super(src);
	}

	/**
	 * Constructs a new Pots (ie a HashMap) with the given 'initialCapacity'
	 * and 'loadFactor'.
	 * @param initialCapacity size of the underlying array of linked-lists.
	 * <br>NB: initialCapacity has a practical lower bound (enforced here) of
	 *  {@code Pots.MIN_CAPACITY = 8}.
	 * @param loadFactor when the {@code this.size() * loadFactor} exceeds the
	 *  capacity (array length) then {@code capacity = capacity * 1.5}, growing
	 *  the underlying array, and redistributing existing entries into it.
	 *  Growth is inherently an O(n) operation EACH TIME, so should be avoided.
	 *  The {@code size()} is the number of entries herein (not the number of
	 *  elements in the array, which confused me, once upon a time, like today).
	 */
	public Pots(int initialCapacity, float loadFactor) {
		super(Math.max(MIN_CAPACITY,initialCapacity), loadFactor);
	}

	/**
	 * Constructs a new Pots containing the given 'cell' => 'value'.
	 * <p>NB: A <b>new</b> instance of Values is put'ed.
	 * @param cell Cell to put.
	 * @param value int to add to a new Values, which is put.
	 */
	public Pots(Cell cell, int value) {
		super(MIN_CAPACITY, 1F);
		put(cell, new Values(value));
	}

	/**
	 * Constructs a new Pots containing the given 'cell' => <b>the given
	 * instance</b> of Values.
	 * <p><b>BEWARE:</b> This constructor adds the given instance of Values to
	 * the Map, so that any subsequent modifications to that Values instance
	 * will show up in this Pots. You only want to use this one if you really
	 * do understand what you're doing, and sh1t gets complex fast, so I doubt
	 * that.
	 * @param cell Cell to put.
	 * @param theValues Values to put.
	 */
	public Pots(Cell cell, Values theValues) {
		super(MIN_CAPACITY, 1F);
		put(cell, theValues);
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * This method takes a {@code Cell[]} to avoid varargs overheads.
	 *
	 * @param cells {@code Cell[]} to put.
	 * @param value int to add to a new Values, which is put.
	 */
	public Pots(Cell[] cells, int value) {
		super(Math.max(MIN_CAPACITY,cells.length), 1F);
		int sv = VSHFT[value];
		for ( Cell cell : cells )
			if ( (cell.maybes.bits & sv) != 0 )
				put(cell, new Values(value));
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * This method takes {@code Cell a, Cell b} to avoid varargs overheads.
	 *
	 * @param value int to add to a new Values, which is put.
	 * @param a the first cell to put.
	 * @param b the second cell to put.
	 */
	public Pots(int value, Cell a, Cell b) {
		super(MIN_CAPACITY, 1F);
		int sv = VSHFT[value];
		if ( (a.maybes.bits & sv) != 0 )
			put(a, new Values(value));
		if ( (b.maybes.bits & sv) != 0 )
			put(b, new Values(value));
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * This method takes {@code Cell a, Cell b, Cell c, Cell d} to avoid
	 * varargs overheads.
	 *
	 * @param value int to add to a new Values, which is put.
	 * @param a the first cell to put.
	 * @param b the second cell to put.
	 * @param c the third cell to put.
	 * @param d the fourth cell to put.
	 */
	public Pots(int value, Cell a, Cell b, Cell c, Cell d) {
		super(MIN_CAPACITY, 1F);
		int sv = VSHFT[value];
		if ( (a.maybes.bits & sv) != 0 )
			put(a, new Values(value));
		if ( (b.maybes.bits & sv) != 0 )
			put(b, new Values(value));
		if ( (c.maybes.bits & sv) != 0 )
			put(c, new Values(value));
		if ( (d.maybes.bits & sv) != 0 )
			put(d, new Values(value));
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * NB: A <b>new</b> instance of Values is put'ed.
	 *
	 * @param value int to add to a new Values, which is put.
	 * @param cellss Cell[]... (an arguements array of cell arrays) to put.
	 */
	public Pots(int value, Cell[]... cellss) {
		for ( Cell[] cells : cellss )
			for ( Cell cell : cells )
				put(cell, new Values(value));
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in 'cells' =>
	 * a <b>new instance</b> of Values which contains the given 'values'.
	 *
	 * @param cells Cell's to put.
	 * @param values Values to copy into new Values objects which are put.
	 */
	public Pots(Cell[] cells, Values values) {
		super(Math.max(MIN_CAPACITY,cells.length), 1F);
		for ( Cell cell : cells ) {
			if(cell==null) break; // handle a null terminated cell list
			put(cell, new Values(values)); // put a new Values, to be safe
		}
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in 'cells' =>
	 * a <b>new instance</b> of Values which contains the given 'values'.
	 *
	 * @param cells Cell's to put.
	 * @param v the value.
	 */
	public Pots(List<Cell> cells, int v) {
		super(Math.max(MIN_CAPACITY,cells.size()), 1F);
		for ( Cell cell : cells )
			put(cell, new Values(v)); // put a new Values for each to be safe
	}

	/**
	 * Constructor: Constructs a new Pots containing all of the given cells
	 * to a new Values containing the given value.
	 *
	 * @param cells
	 * @param value
	 */
	public Pots(Collection<Cell> cells, int value) {
		this(cells.size(), 1F);
		for (Cell c : cells)
			put(c, new Values(value));
	}

	/**
	 * Constructor: Constructs a new Pots from a re-usable cells array, all
	 * to the given value (used for LockingGeneralised greenPots only).
	 *
	 * @param value
	 * @param numCells
	 * @param cells
	 */
	public Pots(int value, int numCells, Cell[] cells) {
		this(numCells, 1F);
		for ( int i=0; i<numCells; ++i )
			upsert(cells[i], value);
	}

	public Pots(Grid grid, Idx idx, int cands) {
		for ( Cell c : idx.cells(grid) )
			put(c, new Values(c.maybes.bits & cands, false));
	}

	/**
	 * Add all these others to this Pots.
	 *
	 * @param others
	 */
	public Pots addAll(Pots others) {
		// head-off adding myself to myself
		if ( others == this )
			return this;
		for ( Cell cell : others.keySet() ) {
			Values otherValues = others.get(cell);
			Values myExistingValues = get(cell);
			if ( myExistingValues == null )
				put(cell, new Values(otherValues));
			else
				myExistingValues.add(otherValues);
		}
		return this;
	}

	/**
	 * Add the given values to all the given cells. The values added are the
	 * intersection of each cells maybes any the given Values. If there are
	 * none then cell is skipped; and then we return "were any added".
	 *
	 * @param cells
	 * @param values
	 * @return were any added
	 */
	public boolean addAll(Cell[] cells, Values values) {
		Values vs;
		boolean any = false;
		for ( Cell cell : cells ) {
			vs = cell.maybes.intersect(values);
			if ( !vs.isEmpty() ) {
				put(cell, vs);
				any = true;
			}
		}
		return any;
	}

	/**
	 * Return a new Pots of my intersections with bits, ie:
	 * <pre>
	 * select cell, cell.maybes.bits & bits
	 * from thisPots
	 * where cell.maybes.bits & bits != 0
	 * </pre>
	 * <p>
	 * Note: actual cell.maybes are used instead of the values in this Pots,
	 * so this Pots is used only as a cell-list.
	 *
	 * @param bits
	 * @return a new Pots of my intersections with bits.
	 */
	public Pots withBits(int bits) {
		Pots result = new Pots();
		int cbits; // cells bits
		for ( Cell cell : keySet() ) {
			cbits = cell.maybes.bits & bits;
			if ( cbits != 0 )
				result.put(cell, new Values(cbits, false));
		}
		return result;
	}

	/**
	 * putAll (COPYING Values) from the given 'map', and return this Pots.
	 * <p>
	 * IMHO this is how putAll should be, for safety.
	 *
	 * @param map A Pots to putAll into this Pots.
	 * @return this Pots.
	 */
	public Pots putAll2(Map<? extends Cell, ? extends Values> map) {
		for ( Cell c : map.keySet() )
			put(c, new Values(map.get(c)) ); // inserts new Values
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
	public void putAll(Map<? extends Cell, ? extends Values> map) {
		assert false : "use putAll2 instead, coz it's SAFE!"; // techies only
		putAll2(map);
	}

	/**
	 * retainAll removes any not-keepers (which is a bitset) from the values of
	 * each cell in this Pots, and if that leaves that Values empty then we
	 * also remove the Cell.
	 *
	 * @param keepers
	 * @return
	 */
	public Pots retainAll(int keepers) {
		for ( Cell cell : keySet() )
			if ( get(cell).retainAll(keepers) == 0 )
				remove(cell);
		return this;
	}

	/**
	 * Put the given Cell and Values if the Values is NOT null-or-empty.
	 *
	 * @param cell Cell to put.
	 * @param values Values to put if they are not empty.
	 */
	public void putIfNotEmpty(Cell cell, Values values) {
		if ( values!=null && values.bits!=0 )
			put(cell, values);
	}

	/**
	 * Update-or-insert: if 'cell' is already in this Pots then add the given
	 * values to the existing Values object, else put a <b>new</b> Values of
	 * the given 'values' object (don't add the given Values object!)
	 *
	 * @param cell Cell key to add/insert
	 * @param values Values to add/insert
	 * @return was this Pots modified: cell added=>true, value/s added=>true;
	 *  but if cell pre-exists with ALL of the given values then I return false
	 */
	public boolean upsert(Cell cell, Values values) {
		final Values existing = get(cell);
		if ( existing != null ) {
			int pre = existing.size;
			return existing.add(values).size > pre;
		}
		// nb: presume the given values is reused, so we put a copy
		put(cell, new Values(values));
		return true;
	}

	/**
	 * Update-or-insert: if 'cell' is already in this Pots then add the given
	 * value to the existing Values instance, else put a <b>new</b> instance
	 * of Values containing the given 'value'.
	 *
	 * @param cell Cell key to add/insert
	 * @param value int value to add/insert
	 * @return true if this Pots was modified, so if the cell is added I always
	 *  return true, but if another value is added to an existing cell then I
	 *  return was the value actually added; ie I return false if the value was
	 *  already in cells values. This allows you to keep count of the values
	 *  actually added to a Pots using the upsert method; or you could just
	 *  count the bastards afterwards. sigh.
	 */
	public boolean upsert(Cell cell, int value) {
		final Values existing;
		if ( (existing=get(cell)) != null )
			return existing.add(value);
		put(cell, new Values(value));
		return true;
	}

	/**
	 * Update-or-insert these 'bits' (a bitset of candidate values).
	 * If 'cell' is already in this Pots then add bits to it's existing Values;
	 * else (the cell is not in this Pots) then create a new instance of Values
	 * containing the given 'bits'.
	 *
	 * @param cell the Cell to upsert to
	 * @param bits the Values.bits to be upserted
	 * @param dummy just a marker parameter which differentiates this methods
	 *  signature from all other method signatures in the Pots class.
	 * @return true if this Pots was modified, so if the cell is added I always
	 *  return true, but if another value is added to an existing cell then I
	 *  return was the value actually added; ie I return false if the value was
	 *  already in cells values. This allows you to keep count of the values
	 *  actually added to a Pots using the upsert method; or you could just
	 *  count the bastards afterwards. sigh.
	 */
	public boolean upsert(Cell cell, int bits, boolean dummy) {
		final Values existing;
		if ( (existing=get(cell)) != null )
			return existing.addBits(bits);
		put(cell, new Values(bits, dummy));
		return true;
	}

	/**
	 * upsert all the other Pots.
	 *
	 * @param others Pots to be upserted.
	 * @return this Pots.
	 */
	public Pots upsertAll(Pots others) {
		if ( others != null )
			for ( Cell cell : others.keySet() )
				upsert(cell, others.get(cell));
		return this;
	}

	/**
	 * Upsert numCells cells with Values v.
	 *
	 * @param v the value to be eliminated/set
	 * @param numCells the number of cells actually in the cells array
	 * @param cells the re-usable array of cells
	 * @return this Pots.
	 */
	public Pots upsertAll(int v, int numCells, Cell[] cells) {
		for ( int i=0; i<numCells; ++i )
			upsert(cells[i], new Values(v));
		return this;
	}

	/**
	 * Upsert (update or insert) each cell in idx which maybe(v). Update means
	 * v is added to the existing Values instance for this Cell. Insert means
	 * the cell is added with a new Values(v). If a cell in 'idx' is not
	 * maybe(v) then it's skipped, which I find convenient.
	 *
	 * @param idx containing indices of cells to be (probably) added
	 * @param grid the grid itself
	 * @param v the value to add for each cell, presuming that cell.maybe(v),
	 *  else this cell is skipped.
	 * @return any changes.
	 */
	public boolean upsertAll(Idx idx, Grid grid, int v) {
		final int sv = VSHFT[v];
		boolean result = false;
		for ( int i : idx.toArrayA() )
			if ( (grid.cells[i].maybes.bits & sv) != 0 )
				result |= upsert(grid.cells[i], v);
		return result;
	}

	/**
	 * insert is the antithesis of upsert. It ONLY inserts the given cell in
	 * this Pots (for creating a setPots). If the cell already exists then it
	 * throws an IllegalStateException: "$cell+$existingValue+$newValue",
	 * even if the new value is the same as the existing value!
	 *
	 * @param cell
	 * @param values
	 * @throws IToldHimWeveAlreadyGotOneException if the cell already exists
	 *  in this Pots.
	 */
	public void insert(Cell cell, Values values) {
		Values existing = get(cell);
		if ( existing != null )
			throw new IToldHimWeveAlreadyGotOneException(cell.id+"+"+existing+"+"+values);
		put(cell, values);
	}

	/**
	 * Populate this Pots with {@code r.cells[bits].maybe(v) => new Values(v)},
	 * ie with the cell at each set-bit (1) in 'bits' in 'r'.cells that
	 * maybe v, to a new Values(v) to avert any future collisions.
	 * <p>
	 * Each cell is put (not upsert) replacing any existing Values for cell.
	 * <p>
	 * Called by {@link diuf.sudoku.solver.hinters.fish.BasicFisherman#search}.
	 *
	 * @param r the ARegion to eliminate from
	 * @param bits a bitset of indexes into cells array
	 * @param v a new Values(value) is created for each cell, to avert any
	 *  future collisions.
	 */
	public void addAll(ARegion r, int bits, int v) {
		final int sv = VSHFT[v];
		final Cell[] cells = r.cells;
		for ( int i : INDEXES[bits] )
			if ( (cells[i].maybes.bits & sv) != 0 )
				put(cells[i], new Values(v));
	}

	/**
	 * Remove all these cells from these Pots.
	 *
	 * @param cells to remove.
	 */
	public void removeAll(Iterable<Cell> cells) {
		if ( cells != null )
			for ( Cell cell : cells )
				remove(cell);
	}

	/**
	 * Remove each value in each cell in others from this Pots and if that
	 * leaves a cell (key) with no values (data) then cell is also removed.
	 * Note that this may result in an empty set!
	 *
	 * @param others
	 * @return
	 */
	public Pots removeAll(Pots others) {
		if ( others != null ) {
			Values mine;
			for ( Cell c : others.keySet() )
				// remove others values from my values, and if that leaves the my
				// values empty then remove my cell also
				if ( (mine=get(c))!=null && mine.remove(others.get(c)).size==0 )
					remove(c);
		}
		return this; // may be empty
	}

	/**
	 * Remove these pots from all of the given potss.
	 *
	 * @param potss
	 */
	public void removeFromAll(Pots... potss) {
		for ( Pots pots : potss )
			if ( pots != null ) // otherwise there's NPE's
				pots.removeAll(this);
	}

	/**
	 * Returns a new Pots of: the same value appears in the same cell in this
	 * and the other Pots.
	 *
	 * @param others
	 * @return
	 */
	public Pots intersection(Pots others) {
		Pots result = new Pots();
		Values myValues, otherValues;
		int commonBits; // the Values.bits common to the current cell in both this Pots and others Pots
		for ( Cell cell : keySet() )
			// myValues SHOULD never be empty, it's only done this way to push
			// the get down into the if statement! So this'll always be true in
			// normal-usage. If empty cells start appearing in Pots then this
			// part atleast will handle them correctly. Now it's just the whole
			// rest of the system you have to worry about. That's nice!
			if ( (myValues=get(cell)).bits != 0
			  // but otherValue pretty obviously can be null.
			  && (otherValues=others.get(cell)) != null
			  // are there any common values?
			  && (commonBits=myValues.bits & otherValues.bits) != 0 )
				// add the common values to the result.
				result.put(cell, new Values(commonBits, false));
		return result; // which may still be empty!
	}

	/** @return the total of the sizes of all the values in these Pots. */
	public int totalSize() {
		int sum = 0;
		for ( Values vs : values() )
			sum += vs.size;
		return sum;
	}

	/**
	 * valuesOf aggregates all values of all cells in this Pots into a bitset.
	 *
	 * @return a bitset of all the values in this pots.
	 */
	public int valuesOf() {
		int ag = 0; // aggregate
		for ( Cell c : keySet() )
			ag |= get(c).bits;
		return ag; // Go for gold baby!
	}

	/**
	 * Is this Pots a subset of these other Pots?<br>
	 * ie Does other contain all eliminations in this Pots?
	 *
	 * @param other
	 * @return
	 */
	public boolean isSubsetOf(Pots other) {
		Values values;
		for ( Cell key : other.keySet() )
			if ( (values=get(key)) == null
			  || !other.get(key).containsAll(values) )
				return false;
		return true;
	}

	/**
	 * Returns true if any of my eliminations (I'm a redPots) still exist, else
	 * false (meaning it's a "dead" hint).
	 *
	 * @return are any of my value/s still in the cell?
	 */
	public boolean anyCurrent() {
		final int[] SHFT = VSHFT;
		for ( Cell cell : keySet() ) // there's one for the advertisers
			for ( int v : VALUESES[get(cell).bits] )
				if ( (cell.maybes.bits & SHFT[v]) != 0 )
					return true;
		return false;
	}

	/**
	 * Set the values of the cells in this pots. This pots is presumed to be
	 * a setPots, not a normal "redPots", or greens, or anything stupid.
	 *
	 * @param isAutosolving
	 * @return
	 */
	public int setCells(boolean isAutosolving) {
		Cell cell;
		Values values;
		int numSet = 0;
		for ( java.util.Map.Entry<Cell,Values> e : entrySet() ) {
			cell = e.getKey();
			values = e.getValue();
//System.out.println("Pots.setCells: set "+cell+" => "+values);
//if ( "I9".equals(cell.id) )
//	Debug.breakpoint();
			assert values.size == 1; // ONE value per cell in a setPots!
			numSet += cell.set(FIRST_VALUE[values.bits], 0, isAutosolving, null);
		}
		return numSet;
	}

	private static StringBuilder sb = null;
	private static StringBuilder getSB() {
		if ( sb == null )
			sb = new StringBuilder(9*16); // not massive, but MANY
		else
			sb.setLength(0); // clear the buffer
		return sb;
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
			Cell cell = firstKey();
			return cell.id+"-"+get(cell);
		}
		// select "${cell.id}-${get(cell)}" from this
		String[] strings = new String[n];
		int i = 0;
		for ( Cell cell : keySet() )
			strings[i++] = cell.id+"-"+get(cell);
		// order by cell.id
		Arrays.sort(strings);
		// stringify the Pots-strings
		StringBuilder mySB = getSB();
		i = 0;
		for ( String s : strings ) {
			if ( ++i > 1 )
				mySB.append(", ");
			mySB.append(s);
		}
		return mySB.toString();
	}

	public String cells() {
		return Frmt.ssv(keySet());
	}

	/**
	 * WARNING: Pots partially upholds the {@link java.lang.Object#hashCode}
	 * contract! All good if Pots remains unchanged once used as a key.
	 * <p>
	 * I'm caching hashCode coz calculating it is an O(n) operation, and
	 * hashCode is hammered once you use a Pots as a key in a Map, which
	 * I now do in ComplexFisherman. sigh.
	 * <p>
	 * Note that caching the hashCode implies that a Pots must NOT be modified
	 * after it's been used as a Hash key. If you do modify a Pots after you
	 * have put it as a key, then the equals-hashCode contract is broken, so
	 * your code (most probably) won't behave consistently, all-though it may
	 * do on occasion, just to keep you guessing. sigh.
	 */
	@Override
	public int hashCode() {
		if ( !hashCodeSet ) {
			hashCode = super.hashCode();
			hashCodeSet = true;
		}
		return hashCode;
	}
	private boolean hashCodeSet;
	private int hashCode;

	/**
	 * Does this Pots equal the given Object? Returns true if obj is a Pots
	 * (or subclass thereof) with the same hashCode as this Pots
	 * <p>
	 * WARNING: Pots partially upholds the {@link java.lang.Object#hashCode}
	 * contract! All good if Pots remains unchanged once used as a key.
	 * <p>
	 * See {@link #hashCode()}.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof Pots && equals((Pots)o);
	}
	// NOTE that we MUST call the hashCode() method, not just reference the
	// hashCode field because we use this method to equals Pots in the test
	// cases, and (unlike the rest of the codebase) that's the first time
	// that hashCode() is called, so the testcase comparisons (which I only
	// sort-of rely-on for correctness) significantly slow down the actual
	// code which can, AFAIK, get away with using hashCode! FFMFS! F! F! F!
	public boolean equals(Pots o) {
		return hashCode() == o.hashCode();
	}

	/**
	 * Only called on redPots to remove eliminations that are not in the grid.
	 *
	 * @return !isEmpty() ie any remaining
	 */
	public boolean clean() {
		Cell c;
		Values vs;
		// use an Iterator to delete as we go
		for ( Iterator<Cell> it=keySet().iterator(); it.hasNext(); )
			// remove any elims which aren't in this cells maybes
			// and if that leaves none then also remove this cell
			if ( (vs=get(c=it.next())).removeBits(vs.bits & ~c.maybes.bits) == 0 )
				it.remove();
		return !isEmpty(); // ie any remaining
	}

	public int firstCellIndice() {
		for ( Cell c : keySet() )
			return c.i;
		return 0; // something other than -1
	}

	public void toIdx(Idx result) {
		result.clear();
		for ( Cell c : keySet() )
			result.add(c.i);
	}

	/**
	 * Translate this Pots into an array of Idx's, one per value.
	 *
	 * @param result Idx[10], one for each value 1..9 (0 is not referenced).
	 */
	public void toIdxs(Idx[] result) {
		for ( int v=1; v<10; ++v )
			result[v].clear();
		entrySet().forEach((e) -> {
			int i = e.getKey().i;
			for ( int v : VALUESES[e.getValue().bits] )
				result[v].add(i);
		});
	}

}
