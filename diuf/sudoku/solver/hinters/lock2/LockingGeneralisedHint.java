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
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * A Generalised Locking hint DTO.
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
	 * @param hinter the instance of LockingGeneralised that created this hint
	 * @param reds the removable (red) Cell=&gt;Values
	 * @param cells region.atNew(region.ridx[value].bits) note that I
	 *  store the given cell array, so you must pass me a new array
	 * @param value the locking candidate value
	 * @param region the region we found this hint in, ie the region we claim
	 *  for (not the region we claim from).
	 */
	public LockingGeneralisedHint(AHinter hinter, Pots reds, Cell[] cells
			, int value, ARegion region) {
		super(hinter, reds);
		this.cells = cells;
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

	@Override
	public List<ARegion> getCovers() {
		return Regions.list(Regions.otherCommon(cells, region));
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
		for ( int i : INDEXES[region.ridx[value].bits] ) {
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
		return s+ON+value+IN+region.id;
	}

	@Override
	public String toStringImpl() {
		return Frmu.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.ssv(cells)).append(ON).append(value)
		  .append(IN).append(region.id)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(
				  this
				, "LockingGeneralisedHint.html"
				, region.id
				, Integer.toString(value)
				, getHintTypeName()
		);
	}
}
