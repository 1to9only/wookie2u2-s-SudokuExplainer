/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_SIZE;
import java.io.PrintStream;

/**
 * RegionIndices generates Java code to create Grid.REGION_INDICES.
 *
 * @author Keith Corlett 2021-07-13
 */
public final class RegionIndices {

	public static void main(String[] args) {
		PrintStream out = System.out;
		out.println("final int[][] REGION_INDICES = new int [][] {");
		Grid grid = new Grid();
		boolean first = true;
		for ( ARegion r : grid.regions ) {
			if(first){first=false;out.print("\t  ");} else out.print("\t, ");
			out.format("{%2d", r.cells[0].indice);
			for ( int i=1; i<REGION_SIZE; ++i )
				out.format(",%2d", r.cells[i].indice);
			out.println("} // "+r.label);
		}
		out.println("};");
	}

	private RegionIndices() { } // Never used

}
