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
import diuf.sudoku.solver.hinters.HintValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
 * candidate of the stem cell; these are the value/s (usually one) to eliminate.
 * <p>
 * Having found a Death Blossom, if an outside cell (not in the ALSs) sees all
 * occurrences of that value (which is in all the ALSs and not in the stem) in
 * the DeathBlossom, then we can eliminate that value from the outside cell.
 *
 * @author Keith Corlett 2020-01-13
 */
public class DeathBlossom extends AAlsHinter
		implements diuf.sudoku.solver.hinters.IPreparer
//				 , diuf.sudoku.solver.hinters.ICleanUp
{
	// the default number of ALSs per candidate value.
	// We seek a number covering about 90% of use-cases. Let it grow for the
	// other 10%. Each list grows ONCE, thereafter it retains it's capacity.
	private static final int ALSS_PER_VALUE = 128;

	/**
	 * List[]s are pigs, so extend {@code List<Als>} to handle the generics.
	 */
	private static class AlsList extends ArrayList<Als> {
		private static final long serialVersionUID = 6468297166L;
		AlsList() {
			super(ALSS_PER_VALUE);
		}
	}

	private void clearAlssByValue() {
		for ( int v=1; v<VALUE_CEILING; ++v )
			alssByValue[v].clear();
	}

	/**
	 * DeathBlossomData is the association of an ALS with it's stem.maybe.
	 * There's ONE DeathBlossomData (db) per DeathBlossom instance.
	 */
	private static class DeathBlossomData {
		// all candidates in all ALSs in this DB.
		int cands;
		// candidates yet to be associated with an ALS.
		int freeCands;
		// candidates common to all ALSs in this DB.
		// NOTE: More than one value is rare in a complete DB.
		int cmnCands;
		// all cells in all ALSs in this DB.
		final Idx idx = new Idx();
		// ALSs by value: each ALS is associated with a maybe of the stem cell.
		final Als[] alssByValue = new Als[VALUE_CEILING];
		// populated in a complete DB. Cells in this DB which maybe each value.
		// only cands are populated, other values remain null.
		final Idx[] vs = new Idx[VALUE_CEILING];
		DeathBlossomData() {
			for ( int v=1; v<VALUE_CEILING; ++v )
				vs[v] = new Idx();
		}
		void clear() {
			cands = freeCands = cmnCands = 0;
			idx.clear();
			Arrays.fill(alssByValue, null);
			clearVs();
		}
		void clearVs() {
			for ( int v=1; v<VALUE_CEILING; ++v )
				vs[v].clear();
		}
	}

	// a list of all the ALSs containing each value 1..9
	private final AlsList[] alssByValue = new AlsList[VALUE_CEILING];
	// The data which is the current DeathBlossom. These fields are in a class
	// because that makes sense to me, and so clear method "zaps" the lot.
	// Note that there's ONE DeathBlossomData per DeathBlossom instance.
	private final DeathBlossomData db = new DeathBlossomData();
	// index of cells to remove maybes from
	private final Idx victims = new Idx();
	// re-use a single redPots instance, rather than create one every time only
	// to remains empty 99+% of the time. When we hint reds are copied-off and
	// theReds is cleared for next time.
	private final Pots theReds = new Pots();

	// set at start of findHints, and finally cleared.
	// Faster and easier to use fields, rather than pass-around everywhere,
	// especially when we're using recursion (less stack work).
	private Grid grid;
	// the cells in the grid which maybe each potential value 1..9
	private Idx[] candidates;
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
		for ( int v=1; v<VALUE_CEILING; ++v )
			alssByValue[v] = new AlsList();
	}

	// prepare to solve this puzzle
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
	}

	// clean-up after the puzzle is solved
//I'm now called locally so no longer implement ICleanUp. Never wipe twice.
//	@Override
	public void cleanUp() {
		clearAlssByValue();
		db.clear();
		Cells.cleanCasA();
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
	protected boolean findHints(Grid grid, Idx[] candidates, Als[] alss
			, int numAlss, Rcc[] rccs, int numRccs, IAccumulator accu) {
		// BUG: LogicalSolver calls me after I'm disabled!
		if ( !isEnabled )
			return false;
		this.grid = grid;
		this.candidates = candidates;
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
			for ( int i=0; i<numAlss; ++i )
				for ( int v : VALUESES[(als=alss[i]).maybes] )
					// if there are any cells in the grid which see all v's in
					// this ALS, then file this ALS under v.
					if ( als.vBuds[v].any() )
						alssByValue[v].add(als);
			// get grid cells with 2..3 maybes: 2 is mandatory. 3 is because I
			// find 0 hints on stems with 4+ maybes in top1465, but may occurr
			// in other puzzles. All the other code handles 4. For correctness
			// you should bump the < 4 upto < 5, until it is proven that 4 can
			// never produce a hint, as I suspect. Sometimes I'm too efficient.
			final Idx stems = grid.getEmptiesWhere((c) -> {
				return c.size < 4;
			});
			// foreach empty cell in the grid
			for ( Cell stem : stems.cellsA(grid) ) {
				// initialise DeathBlossom data
				db.cands = 0; // empty
				// the stem.maybes to assign to each ALS
				db.freeCands = stem.maybes;
				// each ALSs cells are added to db.idx
				db.idx.clear();
				// ALS's must share a common value other than stem.maybes
				db.cmnCands = VALL & ~stem.maybes;
				// seek an ALS for each stem.maybe (each freeCand)
				// if the DeathBlossom has any eliminations then hint
				if ( (result|=recurse(stem)) && onlyOne )
					return result; // exit early
			}
		} finally {
			// forget all Cell references
			this.grid = null;
			this.candidates = null;
			this.accu = null;
			cleanUp();
		}
		return result;
	}

	// seek an ALS for each stem.maybe to form a complete DB, then search each
	// complete DB for eliminations.
	private boolean recurse(Cell stem) {
		// presume that no hint will be found
		boolean result = false;
		if ( db.freeCands != 0 ) {
			// preCands: maybes of all ALS's already in this DB
			// preCmnCands: maybes common to all ALS's already in this DB
			int preCands, preCmnCands;
			// find an ALS for the next free (unassociated) stem.maybe
			int v = VFIRST[db.freeCands];
			// foreach ALS with v as a candidate
			for ( Als als : alssByValue[v] ) {
				// each ALS.cell which maybe value sees the stem
				// vBuds[v] is buddies common to all ALS.cells which maybe v
				if ( als.vBuds[v].has(stem.i)
				  // the ALSs can't overlap
				  && db.idx.andNone(als.idx)
				  // the ALSs share a common maybe other than stem.maybes
				  && (db.cmnCands & als.maybes) != 0
				) {
					// add this ALS to my DeathBlossom
					preCands = db.cands; // save for after
					db.cands |= als.maybes;
					preCmnCands = db.cmnCands; // save for after
					db.cmnCands &= als.maybes; // not empty
					db.freeCands &= ~VSHFT[v];
					db.alssByValue[v] = als;
					db.idx.or(als.idx);
					// try to find an ALS for the next freeCand, or if there
					// are none then search this completed DB for eliminations
					if ( (result|=recurse(stem)) && onlyOne )
						return result; // exit early
					// remove this ALS from my DeathBlossom
					db.cands = preCands;
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
			for ( int v : VALUESES[stem.maybes] ) // to get each ALS
				for ( int cv : VALUESES[db.cmnCands] ) // common value
					db.vs[cv].or(db.alssByValue[v].vs[cv]);
			// populate theReds field with removable Cell=>Values
			// foreach value that is common to all ALSs in this DeathBlossom
			// which is NOT a candidate of the stem-cell. In order to get here
			// at least one cv exists.
			for ( int cv : VALUESES[db.cmnCands] ) {
				// build the victims Idx = cells which:
				db.vs[cv].commonBuddies(victims); // sees all cv's in the DB
				victims.and(candidates[cv]); // and maybe cv itself
				victims.andNot(db.idx); // and is not in any of the ALSs
				if ( victims.any() )
					victims.forEach(grid.cells, (c)->theReds.upsert(c, cv));
			}
			if ( !theReds.isEmpty()) {
				// Found a DeathBlossom with eliminations.
				final AHint hint = createHint(stem);
				// validate it
				if ( HintValidator.DEATH_BLOSSOM_USES ) {
					if ( !HintValidator.isValid(grid, hint.redPots) ) {
						hint.isInvalid = true;
						HintValidator.report(tech.name(), grid, hint.toFullString());
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

	private AHint createHint(Cell stem) {
		// copy-off theReds field and clear them for next time
		Pots reds = new Pots(theReds);
		theReds.clear();
		// build a list of the ALS's
		List<Als> alss = new ArrayList<>(stem.size);
		for ( int v : VALUESES[stem.maybes] )
			alss.add(db.alssByValue[v]);
		// copy-off the ALS's by value array (which is re-used)
		Als[] abv = db.alssByValue.clone();
		// create and return the hint
		return new DeathBlossomHint(this, reds, stem, alss, abv);
	}

}
