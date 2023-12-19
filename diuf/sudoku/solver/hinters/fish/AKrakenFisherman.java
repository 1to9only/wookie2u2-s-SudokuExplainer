/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.table.ATableChainer.Eff;
import static diuf.sudoku.solver.hinters.table.ATableChainer.TABLES;
import diuf.sudoku.utils.IAssSet;
import static java.util.Arrays.fill;

/**
 * Abstract Kraken Fisherman interfaces between ATableChainer.Tables
 * (ATableChainer is now over in the tables package)
 * and KrakenFisherman and MutantJellyfishKrackenFisherman.
 *
 * @author Keith Corlett 2023-10-31
 */
public abstract class AKrakenFisherman extends AComplexFisherman
		implements diuf.sudoku.solver.hinters.IPrepare
		, diuf.sudoku.solver.hinters.ICleanUp
{

	/**
	 * Set NO_CHANGED_VALUES_HACK to true to suppress this dirty little hack.
	 * false makes KrackFisherman take 25% of time to find ~60% of hints, and
	 * personally I will accept anything that makes Krakens faster. But I am
	 * well biased against Krakens. I think Krakens are stupid and WRONG. So
	 * flick this switch if you like waiting. I am impatient so false for me.
	 */
	protected static final boolean NO_CHANGED_VALUES_HACK = false;

	// searchKrakens TABLES.ons[cand]
	protected static Eff[] ktOns;

	// exploded grid.idxs
	// nb: retained between calls, in order to calculate changedCands
	protected final long[] vidxM0 = new long[VALUE_CEILING];
	protected final int[] vidxM1 = new int[VALUE_CEILING];

	protected int changedCands;

	public AKrakenFisherman(final Tech tech) {
		this(tech, true);
	}

	protected AKrakenFisherman(final Tech tech, final boolean useCache) {
		super(tech, true);
	}

	public void initialiseTables() {
		// initialise exits-early if its already done for this grid.
		// nb: ALWAYS initialise in Generator, coz grid changes madly
		TABLES.initialise(grid, Run.isGenerator(), true);
		// explode grid.idxs into parallel arrays, coz array look-up is
		// faster than deref, and idxs are hammered downstairs.
		// HACK: search only candidates that have changed since the last
		// findFish, which finds ~60% of hints in ~25% of time.
		changedCands = 0;
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			if ( vidxM0[v] != idxs[v].m0
			  || vidxM1[v] != idxs[v].m1 )
				changedCands |= VSHFT[v];
			vidxM0[v] = idxs[v].m0;
			vidxM1[v] = idxs[v].m1;
		}
	}

	protected abstract boolean findKrakenFishHints();

	@Override
	protected boolean findComplexFishHints() {
		initialiseTables();
		return findKrakenFishHints();
	}

	@Override
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		// initialise for each puzzle: problem with Swordfish and Jellyfish
		// not re-initialising the kt2Cache.
		TABLES.invalidate();
	}

	/**
	 * cleanUp after each puzzle is solved.
	 */
	@Override
	public void cleanUp() {
		hints.clear();
	}

	// isKrakenHead && Superfisch.degree>=3 disable me in Generate.
	@Override
	public boolean isKrakHead() {
		return true;
	}

}
