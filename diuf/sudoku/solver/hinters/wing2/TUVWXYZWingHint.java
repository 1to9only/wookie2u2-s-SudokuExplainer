package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;


/**
 * TUVWXYZWingHint is the data transfer object (DTO) for a TUVWXYZ-Wing hint.
 * <p>
 * I extend ABigWingHint which implements all the common crap.
 */
public class TUVWXYZWingHint extends ABigWingHint {

	// this, reds, yz, yzVs, wingCands, wing, all
	public TUVWXYZWingHint(AHinter hinter, Pots reds, Cell yz, XZ yzVs
			, int wingCands, Cell[] wing, Cell[] all) {
		super(hinter, reds, wingCands, yz, yzVs, wing, all);
	}

	@Override
	public String toHtmlImpl() {
		String filename = both
				? "TUVWXYZWing2Hint.html"
				: "TUVWXYZWingHint.html";
		return Html.produce(hinter, filename
				, wing[0].id		//{0}
				, wing[3].id		// 1
				, wing[4].id		// 2
				, wing[5].id		// 3
				, yz.id				// 4
				, z					// 5
				, getWingValue(0)	// 6
				, getWingValue(1)	// 7
				, getWingValue(2)	// 8
				, x					// 9
				, bigCard()			//10
				, wingSize()		//11
				, getSuffix()		//12
				, "box, row, col"	//13
				, wing[2].id		//14
				, wing[1].id		//15
		);
	}

}
