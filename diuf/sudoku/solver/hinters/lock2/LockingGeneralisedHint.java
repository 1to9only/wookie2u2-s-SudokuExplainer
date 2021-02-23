package diuf.sudoku.solver.hinters.lock2;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
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

	private final Cell[] cells;
	private final int value;
	private final ARegion region;

	public LockingGeneralisedHint(AHinter hinter, Pots reds, int n, Cell[] cells
			, int value, ARegion region) {
		super(hinter, reds);
		this.cells = copy(n, cells); // copy the re-used array!
		this.value = value;
		this.region = region;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return new Pots(value, cells);
	}

	@Override
	public List<ARegion> getBases() {
		return Grid.regionList(this.region);
	}

	public Cell[] getSelectedCells() {
		return this.cells;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return Grid.cellSet(this.cells);
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
		return getHintTypeName()+": "+Frmt.ssv(cells)+" on value "+value
			 +" in "+region.id;
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
