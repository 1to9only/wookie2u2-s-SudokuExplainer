/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.*;
import diuf.sudoku.Grid.*;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.Validator;
import diuf.sudoku.utils.Log;
import java.util.Arrays;


/**
 * DeathBlossom implements the Death Blossom Sudoku solving technique.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * but in DeathBlossom only doesn't bother determining the RCCs (connections).
 * He calls my "custom" findHints method passing along the ALSs. To be clear:
 * I determine my own RCC's!
 * <p>
 * Explanation from https://www.sudopedia.org/wiki/Solving_Technique
 * <p>
 * A Death Blossom consists of a stem cell and an Almost Locked Set (or ALS)
 * for each of the stem cell's candidates. The ALS associated with a particular
 * stem candidate has that value as one of its own candidates, and within the
 * ALS, every cell that has the value as a candidate sees the stem cell. The
 * ALSs can't overlap; ie no cell can belong to more than one ALS. Also, there
 * must be at least one value that is a candidate of every ALS, but is not a
 * candidate of the stem cell; these are the value/s (usually 1) to eliminate.
 * <p>
 * Having found a Death Blossom, if an outside cell (not in the ALSs) sees all
 * occurrences of that value (which is in all the ALSs and not in the stem) in
 * the DeathBlossom, then we can eliminate that value from the outside cell.
 * <pre>
 * KRC 2021-11-02 alssByValue {@code List<Als>[]} now Als[][] avoiding Iterator
 * to save about 3 seconds over top1465, costs more code, and a bit more RAM.
 * </pre>
 *
 * @author Keith Corlett 2020-01-13
 */
public class DeathBlossom extends AAlsHinter
		implements diuf.sudoku.solver.hinters.IPreparer
				 , diuf.sudoku.solver.hinters.IReporter
{
	@Override
	public void report() {
		if ( isGrower )
			Log.teeln(tech.name()+": private static final int ALSS_BY_VALUE_SIZE = "+maxNumAlssByValue()+";");
	}
	private static boolean isGrower = false;

	// Returns the maximum number of ALS in any alssByValue array.
	private int maxNumAlssByValue() {
		int max = 0;
		// backwards coz observed 9 has more ALSs than others.
		for ( int v=VALUE_CEILING-1; v>0; --v )
			if ( numAlssByValue[v] > max )
				max = alssByValue[v].length;
		return max;
	}

	// observed 255 for 9 in top1465, but they might need to grow a bit more!
	// I ummed and erred about a size per value, and decided against, but the
	// lowest observed was only ~200, so there's some fat here! I now suspect
	// that 255 is a many ALSs as there can be per value, coz I can't fool it.
	private static final int ALSS_BY_VALUE_SIZE = 256;

	// an array-of-arrays of all the ALSs containing each value 1..9
	// nb: this was a List<Als>[] but that involves an Iterator, which is about
	// four seconds slower over top1465, but the array uses a bit more RAM:
	// PRE: 8,953,518,000  10397  861,163  2573  3,479,797  DeathBlossom
	// PST: 5,929,003,322  10397  570,260  2573  2,304,315  DeathBlossom
	private final Als[][] alssByValue = new Als[VALUE_CEILING][];
	private final int[] numAlssByValue = new int[VALUE_CEILING];

	// The data which is the current DeathBlossom. These fields are in a class
	// because that makes sense to me, and so clear method "zaps" the lot.
	// Note that there's ONE DeathBlossomData per DeathBlossom instance.
	private final DeathBlossomData db = new DeathBlossomData();
	// index of cells to remove maybes from
	private final Idx victims = new Idx();
	// re-use a single redPots instance, rather than create one every time only
	// to remains empty 99+% of the time. When we hint reds are copied-off and
	// theReds is cleared for next time.
	private final Pots reds = new Pots();

	// set at start of findHints, and finally cleared.
	// Faster and easier to use fields, rather than pass-around everywhere,
	// especially when we're using recursion (less stack work).
	private Grid grid;
	// the cells in the grid which maybe each potential value 1..9
	private Idx[] idxs;
	// the IAccumulator to which I add hints
	private IAccumulator accu;
	// fast shorthand for accu.isSingle(), ergo is onlyOne hint wanted?
	private boolean onlyOne;

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.DeathBlossom
	 * * allowNakedSets = false in getAlss, no Almost Locked Set may contain
	 *   a cell in any Locked Set in the region; else invalid ALS-Chain hints,
	 *   so KRC supressed them.
	 * * findRCCs = false DeathBlossom is the only ALS-hinter that finds it's
	 *   own RCCs, hence the subsequent params are not used in this call only.
	 * * allowOverlaps = UNUSED.
	 * * forwardOnly = UNUSED.
	 * * useStartAndEnd = UNUSED.
	 * </pre>
	 */
	public DeathBlossom() {
		super(Tech.DeathBlossom, false);
		// populate array
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			alssByValue[v] = new Als[ALSS_BY_VALUE_SIZE];
		}
	}

	// prepare to solve this puzzle
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
	}

	// clean-up after the puzzle is solved
	private void clean() {
		// forget all Cell references
		this.grid = null;
		this.idxs = null;
		this.accu = null;
		clearAlssByValue();
		db.clear();
		Cells.cleanCasA();
	}

	private void clearAlssByValue() {
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			numAlssByValue[v] = 0;
			Arrays.fill(alssByValue[v], null);
		}
	}

	// enlarge existing by 10% + 1
	private static Als[] grow(final int v, final Als[] existing) {
		final int oldSize = existing.length;
		final int newSize = (int)(oldSize * 1.1) + 1;
		final Als[] result = new Als[newSize];
		System.arraycopy(existing, 0, result, 0, oldSize);
		Log.println(Log.me()+": "+v+": "+oldSize+" -> "+newSize);
		isGrower = true;
		return result;
	}

	/**
	 * Finds first/all Death Blossoms in the given Grid.
	 * <p>
	 * This findHints method is called via my supers IHinter findHints method,
	 * passing the rccs, alss, and candidates (common to all als hinters).
	 * <p>
	 * I call the recurse method for each eligible "stem" cell; recurse
	 * associates each stem candidate with an ALS containing that value,
	 * and then searches each completed Death Blossom for eliminations.
	 *
	 * @return any hint/s found?
	 */
	@Override
	protected boolean findHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		// BUG: LogicalSolver calls me after I'm disabled!
		if ( !isEnabled )
			return false;
		this.grid = grid;
		this.idxs = grid.idxs;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		// A DeathBlossom is a stem and an ALS for each of it's maybes.
		// * The ALS for each stem.maybe has that value, and
		//   each ALS.cell which maybe value sees the stem.
		// * The ALSs can't overlap.
		// * There's 1+ value that's a candidate of each ALS but not stem.
		boolean result = false;
		try {
			// build an index of all the ALS's by there values
			Als als;
			Arrays.fill(numAlssByValue, 0);
			for ( int i=0; i<numAlss; ++i ) {
				als = alss[i];
				for ( int v : VALUESES[als.maybes] ) {
					// if there are any cells in the grid that see all v's
					// in this ALS, then file this ALS under v.
					if ( als.vBuds[v].any() ) {
						final int abvIndex = numAlssByValue[v]++;
						try {
							alssByValue[v][abvIndex] = als;
						} catch ( ArrayIndexOutOfBoundsException handled ) {
							alssByValue[v] = grow(v, alssByValue[v]);
							alssByValue[v][abvIndex] = als;
						}
					}
				}
			}
			// stems are grid cells with 2..3 maybes: I tap-out early at 3 coz
			// I find 0 hints on stems with 4+ maybes in top1465, but this may
			// occur in other puzzles, I simply cannot say, so all other code
			// handles 4; and correctness demands that we increase < 4 to < 5,
			// until it is proven that 4 CAN NOT produce a hint.
			//
			// This implementation is excessively efficient, simply because I'm
			// too stupid to prove that 4 can't hint! I simply observe that it
			// is DOES NOT HINT, and hence impose a reasonable but entirely
			// arbitrary (unless proven otherwise) limit, because I really like
			// speed (the smarty-pants non-chemical kind, obviously)!
			//
			// foreach stem in grid.cells where size in 2..4
			for ( Cell stem : grid.cells ) {
				if ( stem.size>1 && stem.size<4 ) {
					// initialise DeathBlossom data
					// the stem.maybes to assign to each ALS
					db.freeCands = stem.maybes;
					// ALS's must share a common value other than stem.maybes,
					// so we start with all values other than stem.maybes
					db.cmnCands = VALL & ~stem.maybes;
					// each ALSs cells are added to db.idx
					db.idx.clear();
					// seek an ALS for each stem.maybe (each freeCand)
					// if the DeathBlossom has any eliminations then hint
					if ( (result|=recurse(stem)) && onlyOne ) {
						return result; // exit early
					}
				}
			}
		} finally {
			clean();
		}
		return result;
	}

	// seek an ALS for each stem.maybe to form a complete DB, then search each
	// complete DB for eliminations.
	private boolean recurse(Cell stem) {
		// presume that no hint will be found
		boolean result = false;
		if ( db.freeCands != 0 ) {
			// preCmnCands: maybes common to all ALS's already in this DB
			int preCmnCands;
			// get the next free (unassociated) stem.maybe
			final int v = VFIRST[db.freeCands];
			// read the indice of the stem cell ONCE
			final int i = stem.i;
			// foreach ALS having v as a candidate, find a matching Als and
			// recurse some more, to associate the next stem.maybe, until all
			// stem maybes are associated, so you go down else path to examine
			// this bloody DeathBlossom.
			final Als[] abv = alssByValue[v];
			Als als;
			for ( int ai=0,n=numAlssByValue[v]; ai<n; ++ai ) {
				// each ALS.cell which maybe value sees the stem
				// vBuds[v] is buddies common to all ALS.cells which maybe v
				if ( (als=abv[ai]).vBuds[v].has(i)
				  // the ALSs can't overlap
				  && db.idx.disjunct(als.idx)
				  // the ALSs share a common maybe other than stem.maybes
				  && (db.cmnCands & als.maybes) != 0
				) {
					// add this ALS to my DeathBlossom
					preCmnCands = db.cmnCands; // save for after
					db.cmnCands &= als.maybes; // not empty
					db.freeCands &= ~VSHFT[v];
					db.alssByValue[v] = als;
					db.idx.or(als.idx);
					// try to find an ALS for the next freeCand, or if there
					// are none then search this completed DB for eliminations
					if ( (result|=recurse(stem)) && onlyOne ) {
						return result; // exit early
					}
					// remove this ALS from my DeathBlossom
					db.cmnCands = preCmnCands;
					db.freeCands |= VSHFT[v];
					db.alssByValue[v] = null;
					db.idx.andNot(als.idx);
				}
			}
		} else {
			// Found a DeathBlossom, but does it eliminate anything?
			// each stem.maybe is now associated with an ALS (the petal)
			// * The ALS for each stem.maybe has that value, and
			//   each ALS.cell which maybe value sees the stem.
			// * The ALSs don't overlap
			// * The ALSs share atleast one common maybe, not in stem.maybes.
			//   If an outside cell (not in ALSs or stem) sees all cells in all
			//   ALSs which maybe value, then eliminate value from that cell.
			// get cells which maybe each value common to all ALSs.
			db.clearVs();
			for ( int v : VALUESES[stem.maybes] ) { // to get each ALS
				for ( int cv : VALUESES[db.cmnCands] ) { // common value
					db.vs[cv].or(db.alssByValue[v].vs[cv]);
				}
			}
			// populate theReds field with removable Cell=>Values
			// foreach value that is common to all ALSs in this DeathBlossom
			// which is NOT a candidate of the stem-cell. In order to get here
			// at least one cv exists.
			for ( int cv : VALUESES[db.cmnCands] ) {
				// build the victims Idx = cells which:
				db.vs[cv].commonBuddies(victims); // sees all cv's in the DB
				victims.and(idxs[cv]); // and maybe cv itself
				victims.andNot(db.idx); // and is not in any of the ALSs
				if ( victims.any() )
					reds.upsertAll(victims, grid, cv);
			}
			if ( !reds.isEmpty()) {
				// FOUND DeathBlossom, with eliminations.
				final AHint hint = new DeathBlossomHint(this
						, reds.copyAndClear(), stem, db.alss(stem)
						, db.alssByValue.clone());
				// validate it
				if ( Validator.DEATH_BLOSSOM_VALIDATES ) {
					if ( !Validator.isValid(grid, hint.reds) ) {
						hint.isInvalid = true;
						Validator.report(tech.name(), grid, hint.toFullString());
						if ( Run.type != Run.Type.GUI ) {
							return false; // skip this hint
						}
					}
				}
				// and add it to the IAccumulator
				result = true;
				if ( accu.add(hint) ) {
					return result; // exit early
				}
			}
		}
		return result;
	}

	/**
	 * DeathBlossomData is the association of an ALS with it's stem.maybe.
	 * There's ONE DeathBlossomData (db) per DeathBlossom instance.
	 */
	private static class DeathBlossomData {
		// candidates yet to be associated with an ALS.
		int freeCands;
		// candidates common to all ALSs in this DB.
		// NOTE: More than one value is rare in a complete DB.
		int cmnCands;
		// all cells in all ALSs in this DB.
		final Idx idx = new Idx();
		// ALSs by value: each ALS is associated with a maybe of the stem cell.
		final Als[] alssByValue = new Als[VALUE_CEILING];
		// Cells in this DB which maybe each value. Populated in a complete DB.
		// Only cands are populated, other values remain empty.
		final Idx[] vs = new Idx[VALUE_CEILING];
		DeathBlossomData() {
			for ( int v=1; v<VALUE_CEILING; ++v ) {
				vs[v] = new Idx();
			}
		}
		void clear() {
			freeCands = cmnCands = 0;
			idx.clear();
			Arrays.fill(alssByValue, null);
			clearVs();
		}
		void clearVs() {
			for ( int v=1; v<VALUE_CEILING; ++v ) {
				vs[v].clear();
			}
		}
		/**
		 * Returns a new Als[] of the alss associated with the given stem cell.
		 * Each ALS therein is associated with each potential value of the
		 * given stem Cell in this DeathBlossomData.
		 *
		 * @param stem to fetch the alss of
		 * @return a new Als[] of the alss associated with this stem cell.
		 */
		Als[] alss(final Cell stem) {
			// build a list of the ALS's
			final Als[] result = new Als[stem.size];
			int cnt = 0;
			for ( int v : VALUESES[stem.maybes] ) {
				result[cnt++] = alssByValue[v];
			}
			return result;
		}
	}

}
