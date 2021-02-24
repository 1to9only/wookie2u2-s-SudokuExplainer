/**
 * Boosted from Sukaku, by Nicolas Juillerat (not the original author).
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


/**
 * A {@code Set<Cell>} backed by a {@link BitSet}. This class wraps a BitSet
 * into a Set of Cells by adding/removing the indice each cell which is
 * present/absent to the BitIdx, then when you iterate the set we turn those
 * indices back into Cells from the given Grid. My BitSet is public to that
 * you can use ALL of it's methods yourself. Each Set method is translated
 * into BitSet speak. My grid attribute is required for iteration and any
 * other method which produces Cell/s. It's public so that you can set it
 * yourself externally. I persist it in my copy constructor.
 * <p>
 * BitIdx was Sukaku CellSet. I don't know where Nicholas got CellSet from, but
 * I do not that he's not Mladen Dobrichev, who I think wrote SudokuMonster.
 * <p>
 * WARN: Be sure to clean-up any BitIdx fields, they hold the Grid in memory!
 * <p>
 * I made a few changes:<ul>
 * <li>
 * Firstly, my grid is stateful, where CellSet's was stateless; so BitIdx has a
 * public grid attribute and new constructors. The Grid methods that create
 * BitIdxs set the grid. The copy-con copies src.grid, so it's persistent; and
 * its a publicly settable attribute. That should cover all bases. The downside
 * is there's no longer a no-arg constructor, you must supply a Grid. If you
 * really need a no-arg-con then create one, but you must set the grid BEFORE
 * you iterate. If you're bean-anal then provide getters and setters to the
 * private grid attribute; for me it's just providing hooks I never use.
 * <li>
 * Secondly, the size method now (correctly) returns cardinality instead of
 * size (capacity); and I implemented the toArray(T[] a) method, because the
 * Netbeans debugger uses both these methods.
 * <li>
 * Also internally I iterate the BitSet manually with nextSetBit instead of
 * using the CellIterator, because iterators are slow.
 * </ul>
 * <p>
 * KRC 2021-01-03 BitIdx used only in diuf.sudoku.solver.hinters.wing2
 * package and the producing Grid methods, but it may be useful elsewhere.
 * <p>
 * KRC 2021-01-03 RANT: I've stated previously that I'm not a fan of ANY BitSet
 * class: it's an oxymoron. One uses a bitset for performance, but pushing the
 * complexity of using bitsets into a class slows everything down; so a BitSet
 * class is useless. Either you require the performance, and will deal with the
 * requisite complexity, or you don't. Hiding the complexity destroys most of
 * it's benefit, so just face-up to the complexity. BitSet is an oxymoron.
 * Just my 2c's worth.
 * <p>
 * KRC 2021-01-04 RANT: Same, same but different. There is no clone! I tried it
 * but for reasons I do not fully understand it didn't work; It kept NPE'ing
 * from iterate DESPITE setting the grid in clone(); so just copy-con instead!
 * <p>
 * I saw some SERIOUSLY weird s__t. The orig was modified instead of the clone,
 * which can only be explained by a Netbeans compiler bug! I am hereby accusing
 * the compiler-boys of doing some weird caching-s__t to speed-up BitSet's,
 * which are, and will always be, too slow (see above)!
 * <p>
 * I suspect compiled-code-cache wasn't updated when I swapped over to clone(),
 * so my called-method was still glommed-onto the old original BitSet instance.
 * Ergo the callee was NOT updated because it was not changed, but it needed to
 * be updated BECAUSE it's caller had changed which BitSet it points at. The
 * moral of the story is "Do NOT do compiled-code-caching". Coz It Sux! And if
 * you must do compiled-code-caching then "clean and build" MUST atleast do a
 * complete clean and build, from scratch; rebuilding EVERYthing, as if on a
 * virgin box. No more dirty stop-outs! We know how to wait.
 * <p>
 * For those who do not know, when Netbeans starts doing weird s__t, including
 * refusing to run code which compiles, or ClassNotFoundException, or won't run
 * your test-cases, or just because Netbeans bights The Big One:
 * 1. Close Netbeans.
 * 2. In Explorer: goto C:\Users\User\AppData\Roaming\NetBeans\8.2
 * 3. Select All files and Shift-Delete to completely-delete all files.
 * 4. Start Netbeans again, as if on a virgin machine (it'll take a while),
 *    * untick that STUPID intro page
 *    * redo all your fonts/formatting/etc preferences.
 *    * open all your favourite projects, to re-cache them in CURRENT state.
 * WARNING: ALL of your code history is gone! ALL preferences, settings, tools,
 * etc, etc, etc have been deleted! You're back on "virgin" install. Enjoy.
 *
 * @author Mladen Dobrichev, 2019
 * @author Keith Corlett 2020-12-22 port into DIUF SudokuExplainer
 *  renamed from CellSet, which is already used.
 */
public class BitIdx implements Set<Cell> {

	public final BitSet bits = new BitSet(81);
	public Grid grid;

	public BitIdx() {
	}

	public BitIdx(Grid grid) {
		this.grid = grid;
	}

	public BitIdx(BitIdx c) {
		grid = c.grid;
		bits.or(c.bits);
	}

	public BitIdx(BitIdx c, Grid grid) {
		bits.or(c.bits);
		this.grid = grid;
	}

	public BitIdx(BitSet b) {
		bits.or(b);
	}

	public BitIdx(Collection<?> cells) {
		for ( Object cell : cells )
			bits.set(((Cell)cell).i);
	}

	public BitIdx(int[] indices) {
		for ( int i : indices )
			bits.set(i);
	}

	public BitIdx(Cell[] cells) {
		for ( Cell cell : cells )
			bits.set(cell.i);
	}

	public BitIdx set(BitIdx src) {
		bits.clear();
		bits.or(src.bits);
		return this;
	}

	public BitIdx setAnd(BitIdx a, BitIdx b) {
		bits.clear();
		bits.or(a.bits);
		bits.and(b.bits);
		return this;
	}

	public boolean setAndAny(BitIdx a, BitIdx b) {
		bits.clear();
		bits.or(a.bits);
		bits.and(b.bits);
		return !bits.isEmpty();
	}

	public boolean setAndMin(BitIdx a, BitIdx b, int minSize) {
		bits.clear();
		bits.or(a.bits);
		bits.and(b.bits);
		return bits.cardinality() >= minSize;
	}

	@Override
	public boolean add(Cell cell) {
		bits.set(cell.i);
		return false;
	}

	public boolean addAll(BitIdx cells) {
		bits.or(cells.bits);
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Cell> cells) {
		for ( Cell cell : cells )
			bits.set(cell.i);
		return false;
	}

	@Override
	public void clear() {
		bits.clear();
	}

	@Override
	public boolean contains(Object o) {
		if ( o instanceof Cell )
			return bits.get(((Cell)o).i);
		if ( o instanceof BitIdx ) {
			BitSet clone = (BitSet)(((BitIdx)o).bits.clone());
			clone.andNot(bits);
			return clone.isEmpty();
		}
		throw new ClassCastException();
	}

	public boolean containsCell(Cell c) {
		return bits.get(c.i);
	}

	public boolean containsAll(BitIdx o) {
		BitSet clone = (BitSet)o.bits.clone();
		clone.andNot(bits);
		return clone.isEmpty();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if ( c instanceof BitIdx ) {
			BitSet cl = (BitSet)(((BitIdx) c).bits.clone());
			cl.andNot(bits);
			return cl.isEmpty();
		}
		for ( Object cell : c )
			if ( !bits.get(((Cell)cell).i) )
				return false;
		return true;
	}

	/**
	 * Do this and the given set have atleast one cell in common?
	 * @param c the set to query
	 * @return does this Set intersect the given Set?
	 */
	public boolean containsAny(BitIdx c) {
		return bits.intersects(c.bits);
	}

	@Override
	public boolean isEmpty() {
		return bits.isEmpty();
	}

	public boolean any() {
		return !bits.isEmpty();
	}

	@Override
	public boolean remove(Object o) {
		if ( o instanceof Cell ) {
			bits.clear(((Cell)o).i);
			return false;
		} else if ( o instanceof BitIdx ) {
			bits.andNot(((BitIdx) o).bits);
			return false;
		}
		throw new ClassCastException();
	}

	public boolean remove(BitIdx idx) {
		bits.andNot(idx.bits);
		return false;
	}

	public void remove(Cell c) {
		bits.clear(c.i);
	}

	public void removeAll(Cell[] cells) {
		for ( Cell c : cells )
			bits.clear(c.i);
	}

	public void removeAll(BitIdx idx) {
		bits.andNot(idx.bits);
	}

	@Override
	public boolean removeAll(Collection<?> cells) {
		for ( Object cell : cells )
			bits.clear(((Cell)cell).i);
		return false;
	}

	public boolean retainAllAny(BitIdx idx) {
		bits.and(idx.bits);
		return !bits.isEmpty();
	}

	public boolean retainAllNone(BitIdx idx) {
		bits.and(idx.bits);
		return bits.isEmpty();
	}

	public void retainAll(BitIdx idx) {
		bits.and(idx.bits);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		BitIdx other = new BitIdx(c);
		bits.and(other.bits);
		return false;
	}

	public interface CellFilter {
		public boolean accept(Cell c);
	}
//useless as tits on a bull	
//	/**
//	 * removeAll cells which do NOT match the given CellFilter.
//	 * Typically CellFilter is implemented with a lambda expression.
//	 * @param f
//	 */
//	public BitIdx filter(CellFilter f) {
//		for ( Iterator<Cell> it=this.iterator(); it.hasNext(); )
//			if ( !f.accept(it.next()) )
//				it.remove();
//		return this;
//	}
	/**
	 * Returns a new BitIdx containing this.filter(CellFilter).
	 * Typically CellFilter is implemented with a lambda expression.
	 * @param f
	 * @return
	 */
	public BitIdx where(CellFilter f) {
		final BitIdx result = new BitIdx(this); // clone
		for ( Iterator<Cell> it=result.iterator(); it.hasNext(); )
			if ( !f.accept(it.next()) )
				it.remove();
		return result;
	}

	@Override
	public int size() {
		return bits.cardinality(); // number of set bits
	}

	public int length() {
		return bits.size(); // capacity of the array, in bits
	}

	@Override
	public Object[] toArray() {
		Cell[] array = new Cell[bits.cardinality()];
		int cnt = 0;
		for ( int i=bits.nextSetBit(0); i>-1; i=bits.nextSetBit(i+1) )
			array[cnt++] = grid.cells[i];
		return array;
	}

	/**
	 * Populate the given array with Cells in this CellSet
	 * @param <T> must be Cell (or a subtype there of, I guess, of which there
	 *  are none, currently).
	 * @param a the array to populate, which is presumed to be large enough.
	 *  Note that Cells are "blind copied" into a, so if 'a' is larger than the
	 *  size() of this CellSet then extraneous elements are left as they where.
	 * @return the given array, null terminated if bigger than me.
	 * @throws ClassCastException if the element type of the passed array is
	 *  not Cell (or a subclass thereof, of which there aren't any (yet)). This
	 *  assmongel was brought to you by Gilligan and the Java API.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		int cnt = 0;
		for ( int i=bits.nextSetBit(0); i>-1; i=bits.nextSetBit(i+1) )
			a[cnt++] = (T)grid.cells[i];
		// null-terminate the array if it's too big
		if ( cnt < a.length )
			a[cnt] = null;
		return a;
	}

	@Override
	public String toString() {
		final int n = bits.cardinality();
		if ( n == 0 )
			return "";
		StringBuilder sb = new StringBuilder(n * 14);
		int i = bits.nextSetBit(0);
		sb.append(grid.cells[i].toFullString());
		while ( (i=bits.nextSetBit(i+1)) > -1 )
			sb.append(' ').append(grid.cells[i].toFullString());
		return sb.toString();
	}

	public class CellIterator implements Iterator<Cell> {
		private int previous = -1;
		@Override
		public boolean hasNext() {
			return bits.nextSetBit(previous + 1) != -1;
		}
		@Override
		public Cell next() {
			previous = bits.nextSetBit(previous + 1);
			return grid.cells[previous];
		}
		@Override
		public void remove() {
			bits.clear(previous);
		}
	}

	/**
	 * NOTE: Requires the grid field to be set, either through the constructor
	 * which takes the Grid, or by directly setting the public grid field.
	 * @return a new instance of CellIterator
	 */
	@Override
	public Iterator<Cell> iterator() {
		return new CellIterator();
	}

}
