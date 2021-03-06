package diuf.sudoku.solver.hinters.lock2;

import diuf.sudoku.Ass;
import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * A Generalised Locking hint
 */
public class LockingGeneralisedHint extends AHint implements IChildHint {

	// the cells which maybe value in this region
	private final Cell[] cells;
	// the locking candidate value
	private final int value;
	// the region
	private final ARegion region;

	/**
	 * Constructor.
	 *
	 * @param hinter the instance of LockingGeneralised
	 * @param reds the removable (red) Cell=&gt;Values
	 * @param indexes region.indexesOf[value].bits
	 * @param value the locking candidate value
	 * @param region the region containing
	 */
	public LockingGeneralisedHint(AHinter hinter, Pots reds, int indexes
			, int value, ARegion region) {
		super(hinter, reds);
		this.cells = region.atNew(indexes);
		this.value = value;
		this.region = region;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return new Pots(value, cells); // cells which maybe value
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(this.region);
	}

	public Cell[] getSelectedCells() {
		return this.cells;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return Cells.set(this.cells);
	}

	@Override
	public int getViewCount() {
		return 1;
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid, IAssSet parentOffs) {
		MyLinkedList<Ass> result = new MyLinkedList<>();
		for ( int i : INDEXES[region.indexesOf[value].bits] ) {
			Cell cell = region.cells[i];
			if ( initGrid.cells[cell.i].maybe(value)
			  && !currGrid.cells[cell.i].maybe(value) )
				result.add(parentOffs.getAss(cell, value));
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof LockingGeneralisedHint && equals((LockingGeneralisedHint)o);
	}
	public boolean equals(LockingGeneralisedHint o) {
		return Arrays.equals(cells, o.cells);
	}

	@Override
	public int hashCode() {
		return cells[0].hashCode();
	}

	public String getClueHtml(Grid grid, boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( !isBig )
			return s;
		return s+" on "+value+" in "+region.id;
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+Frmt.ssv(cells)+" on "+value+" in "
			 +region.id;
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "LockingGeneralisedHint.html"
				, region.id
				, Integer.toString(value)
				, getHintTypeName()
		);
	}
}
