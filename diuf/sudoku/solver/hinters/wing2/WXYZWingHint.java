package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;


/**
 * WXYZWingHint is the data transfer object (DTO) for WXYZ-Wing hints.
 * <p>
 * I extend ABigWingHint for a shared implementation of all the mess.
 */
public class WXYZWingHint extends ABigWingHint {

    public WXYZWingHint(AHinter hinter, Pots reds, int wingCands, XZ yzVs
			, Cell yz, Cell[] wing, Cell[] all
	) {
        super(hinter, reds, wingCands, yz, yzVs, wing, all);
    }

    @Override
    public String toHtmlImpl() {
		String filename = both
				? "WXYZWing2Hint.html"
				: "WXYZWingHint.html";
        return Html.produce(this, filename
				, wing[0].id			//{0}
				, wing[1].id			// 1
				, wing[2].id			// 2
				, yz.id					// 3
				, z						// 4
				, getWingValue(0)		// 5 
				, getWingValue(1)		// 6
				, getWingValue(2)		// 7
				, x						// 8
				, bigCard()				// 9
				, wingSize()			//10
				, getSuffix()			//11
				, "box, row, or col"	//12
		);
    }

}
