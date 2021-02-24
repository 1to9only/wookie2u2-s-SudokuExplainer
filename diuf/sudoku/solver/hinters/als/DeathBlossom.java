/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.*;
import diuf.sudoku.Grid.*;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.HintValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * DeathBlossom implements the Death Blossom solving technique.
 * <p>
 * Here's an explanation from https://www.sudopedia.org/wiki/Solving_Technique
 * <p>
 * A Death Blossom consists of a "stem" cell and an Almost Locked Set (or ALS)
 * for each of the stem cell's candidates. The ALS associated with a particular
 * candidate of the stem cell has that value as one of its own candidates, and
 * within the ALS, every cell that has the value as a candidate can see the
 * stem cell. The ALSes can't overlap; i.e., no cell can belong to more than
 * one ALS. Also, there must be at least one value that is a candidate of every
 * ALS, but is not a candidate of the stem cell.
 * <p>
 * Once we've found a Death Blossom, if an outside cell that doesn't belong to
 * one of the ALSes (and isn't the stem cell) can see every cell in each ALS
 * that has that value as a candidate, and the value isn't a candidate of the
 * stem cell, then that value can be eliminated from the outside cell.
 *
 * @author Keith Corlett 2020-01-13
 */
public class DeathBlossom extends AAlsHinter
		implements diuf.sudoku.solver.IPreparer
				 , diuf.sudoku.solver.hinters.ICleanUp
{
	// the default number of ALSS per candidate value
	private static final int ALSS_PER_VALUE = 128; // let it grow

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
		for ( int v=1; v<10; ++v )
			alssByValue[v].clear();
	}

	/**
	 * DeathBlossomData is the association of an ALS with it's stem.maybe.
	 */
	private static class DeathBlossomData {
		// all candidates in all ALSs in this DeathBlossom
		int cands;
		// candidates yet to be associated with an ALS
		int freeCands;
		// candidates common to ALSs in this DeathBlossom
		int cmnCands;
		// all cells in all ALSs in this DB
		final Idx idx = new Idx();
		// ALSs in the current DeathBlossomData, by value
		// each ALS is associated with a stem.maybe
		final Als[] alssByValue = new Als[10];
		// Populated in a complete DB: cells in this DB which maybe value
		// only cands are populated, other values remain null
		final Idx[] vs = new Idx[10];
		DeathBlossomData() {
			for ( int v=1; v<10; ++v )
				vs[v] = new Idx();
		}
		void clear() {
			cands = freeCands = cmnCands = 0;
			idx.clear();
			Arrays.fill(alssByValue, null);
			clearVs();
		}
		void clearVs() {
			for ( int v=1; v<10; ++v )
				vs[v].clear();
		}
	}

	// a list of all the ALSs containing each value 1..9
	private final AlsList[] alssByValue = new AlsList[10];
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

	public DeathBlossom() {
		// tech, allowLockedSets, findRCCs, allowOverlaps, forwardOnly, useStartAndEnd
		super(Tech.DeathBlossom, false, false, UNUSED, UNUSED, UNUSED);
		// populate array
		for ( int v=1; v<10; ++v )
			alssByValue[v] = new AlsList();
	}

	// prepare to solve this puzzle
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
	}

	// clean-up after the puzzle is solved
	@Override
	public void cleanUp() {
		clearAlssByValue();
		db.clear();
	}

	/**
	 * Finds first/all Death Blossoms in the given Grid.
	 * @return any hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, Idx[] candidates, Rcc[] rccs
			, Als[] alss, IAccumulator accu) {

		// BUG: LogicalSolver keeps calling me, even though I'm disabled!
		if ( !isEnabled )
			return false;

		assert rccs == null; // I find my own, thanks.
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
			// build an index (alssByValue) of all the ALS's by there values
			for ( Als als : alss )
				for ( int v : VALUESES[als.maybes] )
					// add this ALS if there are any cells in the grid which
					// see all occurrences of v in this ALS. A cell with an
					// empty vBuds[v] can't contribute to a DB.
					if ( als.vBuds[v].any() )
						alssByValue[v].add(als);
			// get grid cells with 2..3 maybes: 2 is mandatory, but the 3 is
			// because I find 0 hints on stems with 4+ maybes in top1465; but
			// the hint and everything handles 4. Change it up if you must.
			final Idx stems = grid.getEmptiesWhere((c) -> {
				return c.maybes.size < 4;
			});
			// foreach empty cell in the grid
			for ( Cell stem : stems.cells(grid) ) { // Iterator. Meh!
				// initialise DeathBlossom data
				db.cands = 0; // empty
				// the stem.maybes assigned to each ALS
				db.freeCands = stem.maybes.bits;
				// each ALS gets added to db.idx
				db.idx.clear();
				// ALS's must share a common value other than stem.maybes
				db.cmnCands = Values.ALL_BITS & ~stem.maybes.bits;
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
			clearAlssByValue();
			db.clear();
		}
		return result;
	}

	// seek an ALS for each stem.maybe
	// then we search each complete DeathBlossom for eliminations
	private boolean recurse(Cell stem) {
		// preCands: maybes of all ALS's in this DB before we or-in this ALS
		// preCmnCands: maybes common to all ALS's in DB before and-in this ALS
		int preCands, preCmnCands;
		// presume that no hint will be found
		boolean result = false;
		if ( db.freeCands != 0 ) {
			// find an ALS for the next free (unassociated) stem.maybe
			int v = FIRST_VALUE[db.freeCands];
			// foreach ALS with v as a candidate
			for ( Als als : alssByValue[v] ) {
				// each ALS.cell which maybe value sees the stem
				// vBuds[v] is buddies common to all ALS.cells which maybe v
				if ( als.vBuds[v].contains(stem.i)
				  // the ALSs can't overlap
				  && !db.idx.andAny(als.idx)
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
					// try to find an ALS for the next freeCand
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
			// Found a DeathBlossom, but does it remove anything?
			// each stem.maybe is now associated with an ALS (the petal)
			// * The ALS for each stem.maybe has that value, and
			//   each ALS.cell which maybe value sees the stem.
			// * The ALSs don't overlap
			// * The ALSs share atleast one common maybe, not in stem.maybes.
			//   If an outside cell (not in ALS or the stem) sees all cells in
			//   all ALSs which maybe value, then eliminate value from os cell.

			// get all cells in DB which maybe each value common to all DBs.
			db.clearVs();
			for ( int v : VALUESES[stem.maybes.bits] ) // to get the ALS
				for ( int cv : VALUESES[db.cmnCands] ) // common value
					db.vs[cv].or(db.alssByValue[v].vs[cv]);
			// populate theReds field with removable Cell=>Values
			for ( int cv : VALUESES[db.cmnCands] ) {
				// victims = cells which see all cv's in the DB
				grid.cmnBuds(db.vs[cv], victims);
				victims.and(candidates[cv]); // which maybe cv
				victims.andNot(db.idx); // not in the DB
				victims.remove(stem.i); // not the stem cell
				if ( victims.any() )
					victims.forEach(grid.cells, (c)->theReds.upsert(c, cv));
			}
			if ( !theReds.isEmpty()) {
				// Found a DeathBlossom, with eliminations!
				AHint hint = createHint(stem);
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
		final int n = stem.maybes.size; // (ie num ALSs)
		List<Pots> alsPots = new ArrayList<>(n);
		List<ARegion> regions = new ArrayList<>(n);
		int i;
		for ( i=0; i<n; ++i )
			alsPots.add(new Pots());
		// foreach stem cell maybe (ie each ALS)
		i = 0;
		for ( int v : VALUESES[stem.maybes.bits] ) {
			Als als = db.alssByValue[v];
			final Pots pots = alsPots.get(i++); // for lambda. sigh.
			als.vs[v].forEach(grid.cells, (c)->pots.put(c, new Values(v)));
			regions.add(als.region);
		}
		// copy-off theReds and clear them for next time
		Pots reds = new Pots(theReds);
		theReds.clear();
		// create and return the hint
		return new DeathBlossomHint(this, reds, alsPots, stem
				, db.alssByValue, regions, grid);
	}

}
