/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_SIZE;
import java.io.PrintStream;

/**
 * RegionIndices generates the java code to create the Grid's REGION_INDICES,
 * which is used to create the REGION_IDXS array.
 *
 * @author Keith Corlett 2021-07-13
 */
public class RegionIndices {
public static void main(String[] args) {
	PrintStream out = System.out;
	out.println("final int[][] REGION_INDICES = new int [][] {");
	Grid grid = new Grid();
	boolean first = true;
	for (ARegion r : grid.regions) {
		if(first){first=false;out.print("\t  ");} else out.print("\t, ");
		out.format("{%2d", r.cells[0].i);
		for ( int i=1; i<REGION_SIZE; ++i )
			out.format(",%2d", r.cells[i].i);
		out.println("} // "+r.id);
	}
	out.println("};");
}

}
