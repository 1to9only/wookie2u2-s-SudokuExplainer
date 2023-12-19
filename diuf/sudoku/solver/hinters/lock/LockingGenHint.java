package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.IFoxyHint;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Set;

/**
 * A Generalised Locking hint DTO.
 */
public class LockingGenHint extends AHint implements IFoxyHint {

	// the candidate value that is locked into the base region
	private final int lockValue;
	// the base region in which we found this hint
	private final ARegion region;
	// the cells which maybe value in this region
	private final Cell[] cells;
	// indices of the cells
	private final Idx indices;

	/**
	 * Constructor.
	 *
	 * @param hinter the instance of LockingBasic that created this hint
	 * @param region the region we found this hint in
	 * @param lockValue the locking value
	 * @param reds the removable (red) Cell=&gt;Values
	 */
	public LockingGenHint(final IHinter hinter, final ARegion region
			, final int lockValue, final Pots reds) {
		super(region.getGrid(), hinter, reds);
		this.lockValue = lockValue;
		this.region = region;
		this.cells = region.atNew(region.places[lockValue]);
		this.indices = new Idx(cells);
	}

	@Override
	public Pots getGreenPots(final int viewNum) {
		return new Pots(cells, lockValue); // cells which maybe lockValue
	}

	@Override
	public ARegion[] getBases() {
		return Regions.array(this.region);
	}

	@Override
	public ARegion[] getCovers() {
		return Regions.array(Regions.otherCommonRegion(indices, region));
	}

	@Override
	public Set<Integer> getAquaBgIndices(final int viewNum) {
		return indices;
	}

	@Override
	public int getViewCount() {
		return 1;
	}

	@Override
	public MyLinkedList<Ass> getParents(final Grid initGrid
			, final Grid currGrid, final IAssSet parentOffs) {
		int indice;
		final MyLinkedList<Ass> result = new MyLinkedList<>();
		final int[] rindices = region.indices;
		for ( int i : INDEXES[region.places[lockValue]] ) {
			if ( (initGrid.maybes[indice=rindices[i]] & lockValue) > 0
			  && (currGrid.maybes[indice] & lockValue) < 1 ) {
				result.add(parentOffs.getAss(indice, lockValue));
			}
		}
		return result;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof LockingGenHint && equals((LockingGenHint)o);
	}
	public boolean equals(final LockingGenHint o) {
		return lockValue == o.lockValue
			&& indices.equals(o.indices);
	}

	@Override
	public int hashCode() {
		return indices.hashCode() ^ lockValue;
	}

	public String getClueHtml(final Grid grid, final boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( !isBig )
			return s;
		return s+ON+lockValue+IN+region.label;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.ssv(cells)).append(ON).append(lockValue)
		  .append(IN).append(region.label);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "LockingGenHint.html"
				, region.label					//{0}
				, Integer.toString(lockValue)	// 1
				, getHintTypeName()				// 2
		);
	}

}
