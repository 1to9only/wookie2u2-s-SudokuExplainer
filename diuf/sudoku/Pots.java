/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISIZE;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.utils.MyLinkedHashMap;
import static diuf.sudoku.utils.Frmt.COMMA_SP;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import diuf.sudoku.utils.Frmu;
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
public final class Pots extends MyLinkedHashMap<Cell, Integer> {

	private static final long serialVersionUID = 947665385L;

	/** the size of smallest LinkedHashMap I create. */
	public static final int MIN_CAPACITY = 8;
	/** the EMPTY Pots is currently used only by test-cases. */
	public static final Pots EMPTY = new Pots(MIN_CAPACITY, 1F);

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
		final Pots copy = new Pots();
		src.entrySet().forEach((e) -> {
			copy.put(e.getKey(), e.getValue());
		});
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
		final Pots copy = new Pots(this);
		this.clear();
		return copy;
	}

	/** Constructor. */
	public Pots() {
		super(MIN_CAPACITY, 1F);
	}

	/** Constructor.
	 * @param initialCapacity */
	public Pots(final int initialCapacity) {
		super(Math.max(initialCapacity,MIN_CAPACITY), 1F);
	}

	/**
	 * Copy Constructor: Constructs a new Pots containing a copy of the
	 * given 'src' Pots.
	 * @param src
	 */
	public Pots(final Pots src) {
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
	public Pots(final int initialCapacity, final float loadFactor) {
		super(Math.max(MIN_CAPACITY,initialCapacity), loadFactor);
	}

	/**
	 * Constructs a new Pots containing the given 'cell' => 'value'.
	 * <p>NB: A <b>new</b> instance of Values is put'ed.
	 * @param cell Cell to put.
	 * @param value int to add to a new Values, which is put.
	 */
	public Pots(final Cell cell, final int value) {
		this(cell, VSHFT[value], false);
	}

	/**
	 * Constructs a new Pots containing the given 'cell' => 'bits'.
	 *
	 * @param cell Cell to put.
	 * @param bits a bitset of the values associated with cell
	 * @param dummy nothing to see here
	 */
	public Pots(final Cell cell, final int bits, boolean dummy) {
		super(MIN_CAPACITY, 1F);
		put(cell, bits);
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
	public Pots(final int value, final Cell a, final Cell b) {
		super(MIN_CAPACITY, 1F);
		final int sv = VSHFT[value];
		final Integer values = sv;
		if((a.maybes & sv)!=0) put(a, values);
		if((b.maybes & sv)!=0) put(b, values);
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
	public Pots(final int value, final Cell a, final Cell b, final Cell c, final Cell d) {
		super(MIN_CAPACITY, 1F);
		final int sv = VSHFT[value];
		final Integer values = sv;
		if((a.maybes & sv)!=0) put(a, values);
		if((b.maybes & sv)!=0) put(b, values);
		if((c.maybes & sv)!=0) put(c, values);
		if((d.maybes & sv)!=0) put(d, values);
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in 'cells' =>
	 * a <b>new instance</b> of Values which contains the given 'values'.
	 *
	 * @param cells Cell's to put.
	 * @param v the value.
	 */
	public Pots(final List<Cell> cells, final int v) {
		super(Math.max(MIN_CAPACITY,cells.size()), 1F);
		for ( final Cell cell : cells )
			put(cell, VSHFT[v]); // put a new Values for each to be safe
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in 'cells' =>
	 * a <b>new instance</b> of Values which contains the intersection of this
	 * cell with the given 'bitset'.
	 *
	 * @param cells Cell's to pit
	 * @param values a bitset of the values to add
	 * @param dummy a dummy parameter which differentiates this bitset
	 *  constructor from the "normal" value constructor.
	 */
	public Pots(List<Cell> cells, int values, boolean dummy) {
		super(cells.size(), 1F);
		for ( Cell cell : cells )
			put(cell, cell.maybes & values);
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in 'cells' =>
	 * a <b>new instance</b> of Values which contains the intersection of this
	 * cell with the given 'bitset'.
	 *
	 * @param cells to pot
	 * @param indexes a bitset of indexes of cells to pot
	 * @param values a bitset of the values to add
	 * @param dummy1 a dummy parameter.
	 * @param dummy2 a dummy parameter.
	 */
	public Pots(Cell[] cells, int indexes, int values, boolean dummy1, boolean dummy2) {
		super(ISIZE[indexes], 1F);
		Cell cell;
		int x;
		for ( int i : INDEXES[indexes] )
			if ( (x=(cell=cells[i]).maybes & values) != 0 )
				put(cell, x);
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
			put(c, VSHFT[value]);
	}

	/**
	 * Constructor for all maybes of all of these cells.
	 *
	 * @param cells 
	 */
	public Pots(Cell[] cells) {
		super(cells.length, 1F);
		for ( Cell c : cells )
			put(c, c.maybes);
	}

	/**
	 * Constructor for fixed-size cells array: Constructs a new Pots from a
	 * re-usable fixed-sized cells array, all to the given value.
	 * <p>
	 * Currently only used by Locking, but others may follow, for speed.
	 *
	 * @param cells a fixed-size array of cells
	 * @param n the number of cells to add
	 * @param value
	 */
	public Pots(Cell[] cells, int n, int value) {
		this(cells, n, VSHFT[value], false);
	}

	/**
	 * Constructor for (maybes & values) of first n cells.
	 * If a cell has no values then it is NOT added.
	 *
	 * @param cells
	 * @param n
	 * @param values
	 * @param dummy 
	 */
	public Pots(Cell[] cells, int n, int values, boolean dummy) {
		this(n, 1F);
		for ( int i=0,cands; i<n; ++i )
			if ( (cands=cells[i].maybes & values) != 0 )
				put(cells[i], cands);
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * This method takes a {@code Cell[]} to avoid varargs overheads.
	 *
	 * @param cells {@code Cell[]} to put.
	 * @param value int to add to a new Values, which is put.
	 */
	public Pots(final Cell[] cells, final int value) {
		this(cells, VSHFT[value], false);
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * This method takes a {@code Cell[]} to avoid varargs overheads.
	 *
	 * @param cells {@code Cell[]} to put.
	 * @param values bitset of new Values, which is put.
	 * @param dummy to spit if and when I feel like it.
	 */
	public Pots(final Cell[] cells, final int values, boolean dummy) {
		super(Math.max(MIN_CAPACITY,cells.length), 1F);
		for ( Cell cell : cells )
			if((cell.maybes & values)!=0) put(cell, values);
	}

	/**
	 * Constructs a new Pots containing an entry for each Cell in 'cells' =>
	 * a <b>new instance</b> of Values which contains the given 'values'.
	 *
	 * @param cells Cell's to put.
	 * @param values Values to copy into new Values objects which are put.
	 */
	public Pots(final Cell[] cells, final Integer values) {
		super(Math.max(MIN_CAPACITY,cells.length), 1F);
		for ( final Cell cell : cells ) {
			if(cell==null) break; // handle a null terminated cell list
			put(cell, values); // put a new Values, to be safe
		}
	}

	/**
	 * Constructs a new Pots containing the given 'cells' => 'value'.
	 * <p>
	 * NB: A <b>new</b> instance of Values is put'ed.
	 *
	 * @param value int to add to a new Values, which is put.
	 * @param cellss Cell[]... (an arguements array of cell arrays) to put.
	 */
	public Pots(final int value, final Cell[]... cellss) {
		final Integer values = VSHFT[value];
		for ( final Cell[] cells : cellss )
			for ( final Cell cell : cells )
				put(cell, values);
	}

	/**
	 * Constructor for Idx's: add each cell from the grid that in the idx;
	 * the values for each cell are cands that are in each cell, and are
	 * presumed to intersect.
	 *
	 * @param grid
	 * @param idx
	 * @param cands
	 */
	public Pots(Grid grid, Idx idx, int cands) {
		for ( Cell c : idx.cellsA(grid) )
			put(c, c.maybes & cands);
	}

	/**
	 * Add all these others to this Pots.
	 *
	 * @param others
	 * @return this Pots for method chaining
	 */
	public Pots addAll(Pots others) {
		// head-off adding myself to myself
		if ( others==null || others==this )
			return this;
		others.entrySet().forEach((e) -> {
			final Cell cell = e.getKey();
			final Integer otherValues = e.getValue();
			final Integer myExistingValues = get(cell);
			if ( myExistingValues == null )
				put(cell, otherValues);
			else
				put(cell, otherValues | myExistingValues);
		});
		return this;
	}

	/**
	 * Add the given values to all the given cells. The values added are the
	 * intersection of each cells maybes any the given Values. If there are
	 * none then cell is skipped; and then we return "were any added".
	 *
	 * @param cells
	 * @param bits
	 * @return were any added
	 */
	public boolean addAll(Cell[] cells, int bits) {
		int vs;
		boolean any = false;
		for ( Cell cell : cells )
			if ( (vs=cell.maybes & bits) != 0 )
				any |= put(cell, vs) == null;
		return any;
	}

	/**
	 * Return a new Pots of my intersections with bits, ie:
	 * <pre>
	 * select cell, cell.maybes & bits
	 * from thisPots
	 * where cell.maybes & bits != 0
	 * </pre>
	 * <p>
	 * Note: actual cell.maybes are used instead of the values in this Pots,
	 * so this Pots is used only as a cell-list.
	 *
	 * @param values
	 * @return a new Pots of my intersections with bits.
	 */
	public Pots withBits(int values) {
		final Pots result = new Pots();
		int cands; // cells maybes that're also in values
		for ( Cell cell : keySet() )
			if ( (cands=cell.maybes & values) != 0 )
				result.put(cell, cands);
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
	public Pots putAll2(Map<? extends Cell, ? extends Integer> map) {
		map.entrySet().forEach((e) -> {
			put(e.getKey(), e.getValue()); // inserts new Values
		});
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
	public void putAll(Map<? extends Cell, ? extends Integer> map) {
		assert false : "use putAll2 instead, coz it's SAFE!"; // techies only
		putAll2(map);
	}

//not_used
//	/**
//	 * retainAll removes any not-keepers (which is a bitset) from the values of
//	 * each cell in this Pots, and if that leaves that Values empty then we
//	 * also remove the Cell.
//	 *
//	 * @param keepers
//	 * @return
//	 */
//	public Pots retainAll(int keepers) {
//		for ( java.util.Map.Entry<Cell,Values> e : entrySet() )
//			if ( e.getValue().retainAll(keepers) == 0 )
//				remove(e.getKey());
//		return this;
//	}

	/**
	 * Put the given Cell and Values if the Values is NOT null-or-empty.
	 *
	 * @param cell Cell to put.
	 * @param values Values to put if they are not empty.
	 */
	public void putIfNotEmpty(Cell cell, Integer values) {
		if ( values!=null && values!=0 )
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
	public boolean upsert(Cell cell, Integer values) {
		return upsert(cell, values, false);
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
		return upsert(cell, VSHFT[value], false);
	}

	/**
	 * Update-or-insert these 'bits' (a bitset of candidate values).
	 * If 'cell' is already in this Pots then add bits to it's existing Values;
	 * else (the cell is not in this Pots) then create a new instance of Values
	 * containing the given 'bits'.
	 *
	 * @param cell the Cell to upsert to
	 * @param values the Values.bits to be upserted
	 * @param dummy just a marker parameter which differentiates this methods
	 *  signature from all other method signatures in the Pots class.
	 * @return true if this Pots was modified, so if the cell is added I always
	 *  return true, but if another value is added to an existing cell then I
	 *  return was the value actually added; ie I return false if the value was
	 *  already in cells values. This allows you to keep count of the values
	 *  actually added to a Pots using the upsert method; or you could just
	 *  count the bastards afterwards. sigh.
	 */
	public boolean upsert(Cell cell, int values, boolean dummy) {
		final Integer existing;
		if ( (existing=get(cell)) != null ) { 
			if ( (existing | values) == existing )
				return false;
			return put(cell, existing | values) != null;
		}
		return addOnly(cell, values);
	}

	/**
	 * upsert all the other Pots.
	 *
	 * @param others Pots to be upserted.
	 * @return this Pots.
	 */
	public Pots upsertAll(Pots others) {
		if ( others != null )
			for ( java.util.Map.Entry<Cell,Integer> e : others.entrySet() )
				upsert(e.getKey(), e.getValue());
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
		Integer values = VSHFT[v];
		for ( int i=0; i<numCells; ++i )
			upsert(cells[i], values);
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
		boolean any = false;
		for ( int i : idx.toArrayA() )
			if ( (grid.cells[i].maybes & sv) != 0 )
				any |= upsert(grid.cells[i], sv, false);
		return any;
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
	public void insert(Cell cell, int values) {
		Integer existing = get(cell);
		if ( existing != null )
			throw new IToldHimWeveAlreadyGotOneException(cell.id+PLUS+existing+PLUS+Values.toString(values));
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
	 * @param cells the cells to eliminate from (ARegion.cells)
	 * @param indexes a bitset of indexes into cells array (ARegion.indexesOf)
	 * @param v a new Values(value) is created for each cell, to avert any
	 *  future collisions.
	 */
	public void addAll(final Cell[] cells, final int indexes, final int v) {
		final int sv = VSHFT[v];
		Integer values = sv;
		for ( int i : INDEXES[indexes] )
			if ( (cells[i].maybes & sv) != 0 )
				put(cells[i], values);
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
			Integer mine;
			for ( java.util.Map.Entry<Cell,Integer> e : others.entrySet() )
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
		Cell cell;
		Integer myValues, otherValues;
		int commonValues; // values common to this cell in both me and others
		final Pots result = new Pots();
		for ( java.util.Map.Entry<Cell,Integer> e : entrySet() )
			if ( (myValues=e.getValue()) != 0
			  // but otherValue pretty obviously can be null.
			  && (otherValues=others.get(cell=e.getKey())) != null
			  // are there any common values?
			  && (commonValues=myValues & otherValues) != 0 )
				// add the common values to the result.
				result.put(cell, commonValues);
		return result; // which may still be empty!
	}

	/** @return the total of the sizes of all the values in these Pots. */
	public int totalSize() {
		int sum = 0;
		for ( int sv : values() )
			sum += VSIZE[sv];
		return sum;
	}

	/**
	 * valuesOf aggregates all values of all cells in this Pots into a bitset.
	 *
	 * @return a bitset of all the values in this pots.
	 */
	public int valuesOf() {
		int all = 0;
		for ( int sv : values() )
			all |= sv;
		return all;
	}

	/**
	 * Is this Pots a subset of these other Pots?<br>
	 * ie Does other contain all eliminations in this Pots?
	 *
	 * @param others
	 * @return
	 */
	public boolean isSubsetOf(Pots others) {
		Integer myValues;
		for ( java.util.Map.Entry<Cell,Integer> e : others.entrySet() )
			if ( (myValues=get(e.getKey())) == null
			  || (e.getValue() & myValues) != myValues )
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
		for ( java.util.Map.Entry<Cell,Integer> e : entrySet() )
			for ( int v : VALUESES[e.getValue()] )
				if ( (e.getKey().maybes & VSHFT[v]) != 0 )
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
		int sv;
		int numSet = 0;
		for ( java.util.Map.Entry<Cell,Integer> e : entrySet() ) {
			cell = e.getKey();
			sv = e.getValue();
//System.out.println("Pots.setCells: set "+cell+PRODUCES+values);
//if ( "I9".equals(cell.id) )
//	Debug.breakpoint();
			assert VSIZE[sv] == 1; // ONE value per cell in a setPots!
			if ( Run.type==Run.Type.Batch && !isAutosolving )
				numSet += cell.set(VFIRST[sv]);
			else
				numSet += cell.set(VFIRST[sv], 0, isAutosolving, null);
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
			Cell cell = firstKey();
			return cell.id+MINUS+Values.toString(get(cell));
		}
		// select "A1-49" from this
		String[] strings = new String[n];
		int i = 0, totalSize = 0;
		for ( java.util.Map.Entry<Cell,Integer> e : entrySet() ) {
			strings[i] = e.getKey().id+MINUS+Values.toString(e.getValue());
			totalSize += strings[i].length() + 2; // 2 for the COMMA_SP
			++i;
		}
		// order by cell.id
		Arrays.sort(strings);
		// stringify the Pots-strings
		StringBuilder sb = new StringBuilder(totalSize);
		for ( i=0; i<n; ++i ) {
			if ( i > 0 )
				sb.append(COMMA_SP);
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	public String cells() {
		return Frmu.ssv(keySet());
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
		Integer newValues;
		// use an Iterator to delete as we go
		java.util.Map.Entry<Cell,Integer> e;
		for ( Iterator<java.util.Map.Entry<Cell,Integer>> it=entrySet().iterator(); it.hasNext(); )
			if ( ((newValues=(e=it.next()).getValue() & (c=e.getKey()).maybes)) == 0 )
				it.remove();
			else
				put(c, newValues);
		return !isEmpty(); // ie any remaining
	}

	public int firstCellIndice() {
		for ( Cell c : keySet() )
			return c.i;
		return 0; // something other than -1
	}

//not_used
//	public void toIdx(Idx result) {
//		result.clear();
//		for ( Cell c : keySet() )
//			result.add(c.i);
//	}

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
			for ( int v : VALUESES[e.getValue()] )
				result[v].add(i);
		});
	}

	public static class IToldHimWeveAlreadyGotOneException extends IllegalStateException {
		private static final long serialVersionUID = 516409800239L;
		public IToldHimWeveAlreadyGotOneException(String msg) {
			super(msg);
		}
	}

}
