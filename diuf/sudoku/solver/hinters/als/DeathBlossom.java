/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.*;
import diuf.sudoku.Grid.*;
import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.solver.hinters.Validator.*;
import diuf.sudoku.utils.Log;
import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;

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
 *
 * KRC 2021-12-30 enspeedonate: Explode Idx's and ops. Just pass down the built
 * up vars, instead of building and unbuilding them; that was stupid. Also use
 * a seperate abvBuds array (coincident with alssByValue) of the als.vBuds, to
 * save deferencing vBuds from als-struct 700 million times.
 * PRE: 7,042,097,300  9966  706,612  2543  2,769,208  DeathBlossom
 * PST: 5,063,695,500  9966  508,097  2543  1,991,229  DeathBlossom
 * The new version is about 30% faster than the old version.
 * similarly avbIndexes array saves deferencing als.idx 68 million times
 * 4,946,477,700   9966  496,335  2543  1,945,134  DeathBlossom
 * I moved DeathBlossom up the charts, before Finned*Fish
 * 4,957,628,300  10337  479,600  2580  1,921,561  DeathBlossom
 * I relegate DB back to where it was, coz it increases calls of ALL hinters.
 * Also, it's slower per call than Finned*Fish, so promo was a false economy.
 *
 * </pre>
 *
 * @author Keith Corlett 2020-01-13
 */
public class DeathBlossom extends AAlsHinter
		implements diuf.sudoku.solver.hinters.IPrepare
				 , diuf.sudoku.solver.hinters.IReporter
{
	@Override
	public void report() {
		// enlarge the ABV_SIZE, in the code, ya putz.
		if ( isGrower ) {
			isGrower = false;
			Log.teeln("WARN: "+tech.name()+": private static final int AVB_SIZE = "+maxAbvSize+";");
		}
	}
	private static boolean isGrower = false;

	// index of cells to remove maybes from
	private static final Idx VICTIMS = new Idx();

	// re-use a single REDS, rather than create an instance every call only to
	// remain empty 79% of times. When we hint reds are copied-off and REDS is
	// cleared for next time.
	private static final Pots REDS = new Pots();

	// ALSS_BY_VALUE_SIZE: I suspect 255 is a many as there can be per value.
	private static final int ABV_SIZE = 256;

	// an array-of-arrays of all the ALSs containing each value 1..9
	// nb: this was a List<Als>[] but that involves an Iterator, which is about
	// four seconds slower over top1465, but the array uses a bit more RAM:
	// PRE: 8,953,518,000  10397  861,163  2573  3,479,797  DeathBlossom
	// PST: 5,929,003,322  10397  570,260  2573  2,304,315  DeathBlossom
	private final Als[][] alssByValue = new Als[VALUE_CEILING][ABV_SIZE];
	private final int[] numAlssByValue = new int[VALUE_CEILING];
	// FASTARDISED: use a seperate array of als.vBuds, rather than dereference
	// it from the als struct 700 million times. Store EXACTLY what you need in
	// a seperate array that is coincident with alssByValue.
	private final Idx[][] abvBuddies = new Idx[VALUE_CEILING][ABV_SIZE];
	// FASTARDISED: no deref als.idx 68 million times, coinc with alssByValue
	// PST: 4,946,477,700  9966  496,335  2543  1,945,134  DeathBlossom
	private final Idx[][] abvIndexes = new Idx[VALUE_CEILING][ABV_SIZE];
	// FASTARDISED: no deref als.maybes 30 million times, coinc wit alssByValue
	// PST: 4,780,816,000  9966  479,712  2543  1,879,990  DeathBlossom
	// That's about half the time of the original List implementation. Yeah!
	private final int[][] abvMaybes = new int[VALUE_CEILING][ABV_SIZE];

	// alss by stem.maybe in this DeathBlossom: each ALS is associated with a
	// maybe of the stem cell. I'm no longer called dbAlssByValue in order to
	// differentiate me from the alssByValue array, containing ALL alss, which
	// confundissed me, so I renamed this one. Sigh. I'm built-up by recurse as
	// we find an ALS that fits each stem.maybe.
	final Als[] dbAbv = new Als[VALUE_CEILING];
	private void clearDbAbv() {
		fill(dbAbv, null);
	}

	// indices of cells which maybe value 1..9 in a complete DeathBlossom.
	// Only this DB's commonCands are populated, other v's remain null.
	final Idx[] dbVs = new Idx[VALUE_CEILING];
	private void clearDbVs() {
		for ( int v=1; v<VALUE_CEILING; ++v )
			dbVs[v].clear();
	}

	// indices of the cells in this DeathBlossom
	// this Idx is only set when we hint, from the exploded db0,db1,db2 params
	// that are built-up by successive calls to recurse.
	private final Idx dbIdx = new Idx();

	// set at start of findHints, and finally cleared.
	// Faster and easier to use fields, rather than pass-around everywhere,
	// especially when we're using recursion (less stack work).
	private Grid grid;
	// Grid.cells.size
	private int[] cellSize;
	// Grid.cells
	private Cell[] gridCells;
	// Grid.maybes
	private int[] gridMaybes;
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
	 * * allowNakedSets = false: cells in NakedSets are precluded by getAlss
	 *   coz they break DeathBlossom logic, how exactly I do NOT comprehend.
	 *   Actually, getAlss already has alss including NakedSets from BigWings,
	 *   so just filters-out alss which contain a cell that's in a NakedSet in
	 *   this als.region, which is a bit faster.
	 * * findRccs = false: DeathBlossom builds it's own alssByValue instead.
	 * * allowOverlaps = UNUSED.
	 * * forwardOnly = UNUSED.
	 * * useStartAndEnd = UNUSED.
	 * </pre>
	 */
	public DeathBlossom() {
		super(Tech.DeathBlossom, false);
		for ( int v=1; v<VALUE_CEILING; ++v )
			dbVs[v] = new Idx();
	}

	// prepare to solve this puzzle
	// My super provides my prepare method, but doesn't implements IPrepare,
	// to leave it upto each subclass whether or not it can be disabled.
	// Every hinter that's ever gone deadcat MUST implement IPrepare, or once
	// disabled it stays disabled, permanently, for the length of the run.
	// DeathBlossom went deadcat many times during development, so I shot it.
	// It's Mr Death. He's here about the reaping. None shall pass! But you're
	// leg's off. No it isn't. Modern politics. Sigh.
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
	}

	// Returns the maximum number of ALS in any alssByValue array.
	private int maxAlssByValue() {
		int max = 0;
		// backwards coz observed 9 has more ALSs than others.
		for ( int v=VALUE_CEILING-1; v>0; --v )
			if ( numAlssByValue[v] > max )
				max = numAlssByValue[v];
		return max;
	}
	private int maxAbvSize;

	// clean-up after the puzzle is solved
	private void clean() {
		// calculate max maxAlssByValue of any grid, before clearing the array.
		// No need to bother if the bastard hasn't grown.
		if ( isGrower )
			maxAbvSize = Math.max(maxAbvSize, maxAlssByValue());
		// clear alssByValue
		fill(numAlssByValue, 0);
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			fill(alssByValue[v], null);
			fill(abvBuddies[v], null);
			fill(abvIndexes[v], null);
			fill(abvMaybes[v], 0);
		}
		clearDbAbv();
		clearDbVs();
		dbIdx.clear(); // anally non-retentive!
		// forget all references set by findHints
		this.grid = null;
		this.cellSize = null;
		this.gridCells = null;
		this.gridMaybes = null;
		this.idxs = null;
		this.accu = null;
	}

	private static Als[] copy(final Als[] src, final Als[] dst, final int length) {
		arraycopy(src, 0, dst, 0, length);
		return dst;
	}

	private static Idx[] copy(final Idx[] src, final Idx[] dst, final int length) {
		arraycopy(src, 0, dst, 0, length);
		return dst;
	}

	private static int[] copy(final int[] src, final int[] dst, final int length) {
		arraycopy(src, 0, dst, 0, length);
		return dst;
	}

	// enlarge existing by 10% + 1
	private void grow(final int v) {
		isGrower = true;
		final int oldSize = alssByValue[v].length;
		final int newSize = (int)(oldSize * 1.1 + 0.5); // 110% rounded up
		Log.println(Log.me()+": "+v+": "+oldSize+" -> "+newSize);
		alssByValue[v] = copy(alssByValue[v], new Als[newSize], oldSize);
		abvBuddies[v] = copy(abvBuddies[v], new Idx[newSize], oldSize);
		abvIndexes[v] = copy(abvIndexes[v], new Idx[newSize], oldSize);
		abvMaybes[v] = copy(abvMaybes[v], new int[newSize], oldSize);
	}

	/**
	 * Finds first/all Death Blossoms in the given Grid.
	 * <p>
	 * This findAlsHints method is called via supers IHinter findHints method,
	 * passing the rccs, alss, and candidates (common to all als hinters).
	 * <p>
	 * I call the recurse method for each eligible "stem" cell; recurse
	 * associates each stem candidate with an ALS containing that value,
	 * and searches each completed DeathBlossom for eliminations.
	 *
	 * @return any hint/s found?
	 */
	@Override
	protected boolean findAlsHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		// BUG: LogicalSolver calls me after I'm disabled!
		if ( !isEnabled )
			return false;
		Als als;
		this.grid = grid;
		this.cellSize = grid.size;
		this.gridCells = grid.cells;
		this.gridMaybes = grid.maybes;
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
			// build alssByValue: an index of all the ALS's by there values
			fill(numAlssByValue, 0);
			for ( int i=0; i<numAlss; ++i ) {
				for ( int v : VALUESES[(als=alss[i]).maybes] ) {
					// if there are any cells in the grid that see all v's
					// in this ALS, then file this ALS under v.
					if ( als.vBuds[v].any() ) {
						final int ai = numAlssByValue[v]++;
						if ( ai >= alssByValue[v].length )
							grow(v);
						alssByValue[v][ai] = als;
						abvBuddies[v][ai] = als.vBuds[v];
						abvIndexes[v][ai] = als.idx;
						abvMaybes[v][ai] = als.maybes;
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

			// foreach stem in grid.cells where size in 2..4
			for ( int stem=0; stem<GRID_SIZE; ++stem )
				if ( cellSize[stem]>1 && cellSize[stem]<4 ) {
					// seek an ALS for each stem.maybe (each freeCand).
					// ALS's must share a common value other than stem.maybes,
					// so we start with all values other than stem.maybes.
					// the stem.maybes to assign to each ALS
					// if the DeathBlossom has any eliminations then hint
					if ( (result|=recurse(stem, 0,0,0
							, VALL & ~gridMaybes[stem] // commonCands are all except stem.maybes
							, gridMaybes[stem])) // freeCands
							&& onlyOne )
						return result; // exit early
				}
		} finally {
			clean();
		}
		return result;
	}

	// find an ALS for each stem.maybe to form a complete DB,
	// then search each completed DB for eliminations.
	// @param stem: indice of a cell with 2/3 maybes, each of which I associate
	//  with an als sharing a value where all instance of that value in the als
	//  als see the stem, and alss cannot overlap, and all alss share a common
	//  value other than stem.maybes, which becomes the value to eliminate
	// @param db0,db1,db2 an exploded Idx of the cells in this DeathBlossom
	// @param commonCands a bitset of values common to all alss in this DB
	// @param freeCands stem cell candidates that are yet to be unassociated
	//  with an als
	private boolean recurse(final int stem, final int db0, final int db1
			, final int db2, final int commonCands, final int freeCands) {
		// presume that no hint will be found
		boolean result = false;
		if ( freeCands != 0 ) {
			Idx ai; // als.idx
			// get the next free (unassociated) stem.maybe
			final int v = VFIRST[freeCands];
			final Als[] abv = alssByValue[v];
			final Idx[] abvBuds = abvBuddies[v];
			final Idx[] abvIdxs = abvIndexes[v];
			final int[] abvMybs = abvMaybes[v];
			// foreach ALS having v as a candidate, find a matching Als and
			// recurse some more, to associate the next stem.maybe, until all
			// stem maybes are associated, so you go down else path to examine
			// this bloody DeathBlossom.
			for ( int i=0,n=numAlssByValue[v]; i<n; ++i ) {
				// if each als.cell that maybe v sees the stem cell.
				// nb: als.vBuds[v] are the buds of all als.cells that maybe v.
				// nb: this loop runs 720 million times in top1465, so calling
				// a method up-front is definately NOT ideal, but what else?
				if ( abvBuds[i].has(stem) // <<<<<<< ALL METHOD CALLS ARE SLOW!
				  // and the ALSs can't overlap
				  // nb: this phrase is parsed only 68 million times
				  && ( (db0 & (ai=abvIdxs[i]).a0)
					 | (db1 & ai.a1)
					 | (db2 & ai.a2) ) == 0
				  // and the ALSs share a common maybe other than stem.maybes
				  // nb: this phrase is parsed only 30 million times
				  && (commonCands & abvMybs[i]) != 0
				) {
					// nb: this happens just 20-something million times. Sigh.
					// add this ALS to db.abv, under it's value
					dbAbv[v] = abv[i];
					// try to find an ALS for the next freeCand, or if there
					// are none then search this completed DB for eliminations
					if ( (result|=recurse(stem, db0|ai.a0, db1|ai.a1, db2|ai.a2
							, commonCands & abv[i].maybes
							, freeCands & ~VSHFT[v])) && onlyOne ) {
						dbAbv[v] = null; // I clean-up myself as I go
						return result; // exit early
					}
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
			// nb: this happens only about 20 million times (not uber-critical)

			// get cells which maybe each value common to all ALSs.
			clearDbVs();
			for ( int v : VALUESES[gridMaybes[stem]] ) // to get each ALS
				for ( int cv : VALUESES[commonCands] ) // common value
					dbVs[cv].or(dbAbv[v].vs[cv]);
			// populate theReds field with removable Cell=>Values
			// foreach value that is common to all ALSs in this DeathBlossom
			// which is NOT a candidate of the stem-cell. In order to get here
			// at least one cv exists.
			boolean any = false;
			for ( int cv : VALUESES[commonCands] )
				// victims are cells which:
				if ( dbVs[cv].commonBuddies(VICTIMS) // see all cv's in the DB
						.and(idxs[cv]) // and maybe cv itself
						.andNot(dbIdx.set(db0,db1,db2)) // and not in any ALS
						.any() ) // and if there's any remaining
					any |= REDS.upsertAll(VICTIMS, grid, cv); // kill them all
			if ( any ) {
				// FOUND DeathBlossom, with eliminations.
				final AHint hint = new DeathBlossomHint(this
						, REDS.copyAndClear()
						, gridCells[stem]
						, getAlssNew(gridCells[stem])
						, dbAbv.clone());
				// validate it
				if ( VALIDATE_DEATH_BLOSSOM ) {
					if ( !validOffs(grid, hint.reds) ) {
						hint.isInvalid = true;
						reportRedPots(tech.name(), grid, hint.toFullString());
						clearDbAbv();
						if ( Run.type != Run.Type.GUI )
							return false; // skip this hint
					}
				}
				// and add it to the IAccumulator
				result = true;
				if ( accu.add(hint) )
					return result; // exit early
			}
		}
		return result;
	}

	/**
	 * Returns a new Als[] of the alss associated with the given stem cell.
	 * Each ALS therein is associated with each potential value of the given
	 * stem Cell in this DeathBlossomData.
	 *
	 * @param stem to fetch the alss of
	 * @return a new Als[] of the alss associated with this stem cell.
	 */
	private Als[] getAlssNew(final Cell stem) {
		// build a list of the ALS's
		final Als[] result = new Als[stem.size];
		int cnt = 0;
		for ( int v : VALUESES[stem.maybes] )
			result[cnt++] = dbAbv[v];
		return result;
	}

}
