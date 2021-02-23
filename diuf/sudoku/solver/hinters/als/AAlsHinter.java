/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Permutations;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * The HoDoKu Abstract AlmostLockedSet Hinter class holds implementation code
 * common to the HdkAlsXz, HdkAlsXyWing and HdkAlsXyChain classes.
 * <p>
 * Actually, I implement getHints to get: candidates, lockedSets, ALSs, and
 * RCCs; and then I pass it all along to my subtypes findHints method.
 *
 * @author Keith Corlett 2020 May 24
 */
abstract class AAlsHinter extends AHinter
		implements diuf.sudoku.solver.IPreparer
{
	// the default alss List capacity.
	// anything from 64..4096 is doable.
	// note that ArrayLists growth is length*1.5, not double like a Hash.
	protected static final int NUM_ALSS = 256; // Observed 283

	// include single bivalue Cells in the ALSs list?
	// nb: 1 cell with 2 potential values is by definition an ALS. I don't know
	// what problems it causes if you set this to true. You have been warned!
	protected static final boolean INCL_SINGLE_CELLS = false;

	// the default rccs List capacity
	protected static final int NUM_RCCS = 2000;

	// local references to constants
	protected static final int[][] VALUESES = Values.ARRAYS;
	protected static final int[] SIZE = Values.SIZE;
	protected static final int[] SHFT = Values.SHFT;

	/** include ALSs which overlap in my findHints method? */
	protected final boolean allowOverlaps;
	/** include ALSs which include cells that are part of a Locked Set? */
	protected final boolean allowLockedSets;
	/** should getRccs do a fast forwardOnly search, or a full search? */
	protected final boolean forwardOnly;
	/** should getRccs populate startIndices and endIndices. */
	private final boolean useStartAndEnd;

	// inLockedSet[region][cell.i]: findLockedSets => recurseAlss when
	// !allowLockedSets (in HdkAlsXyChains only): element set to true if this
	// cell is involved in any Locked Set in this region.
	private final boolean[][] inLockedSet;

	/**
	 * Construct a new abstract ALS hinter.
	 * <p>
	 * HdkAlsXz does not use the valid method<br>
	 * HdkAlsXyWing and HdkAlsXyChain both use the valid method
	 * <p>
	 * ASAT 2020-06-28 10:30 the valid method is no longer used, so the
	 * logicalSolver parameter is now superfluous, but it's been retained
	 * rather than start an in-out in-out change chain. Please note that a
	 * hinter should not be aware of the existence of a LogicalSolver, let
	 * alone it's internal workings, like the valid method is. So now I'm
	 * persisting in breaking my own rules for no good reason. Sigh.
	 *
	 * @param tech the Tech(nique) which we're implementing, in my case Als*
	 * @param allowOverlaps if false the getRccs method ignores two ALSs which
	 * physically overlap (ie have any cell in common).
	 * <br>HdkAlsXz and HdkAlsXyWing both use true
	 * <br>HdkAlsXyChain uses false because overlaps bugger it up
	 * @param allowLockedSets if false the getAlss method ignores any cell
	 * which is part of an actual Locked Set (NakedPair/Triple/etc) in this
	 * region (ie treats the cell as if it were a set cell).
	 * <br>HdkAlsXz and HdkAlsXyWing both use true
	 * <br>HdkAlsXyChain uses false because locked sets bugger it up
	 * @param forwardOnly if true then getRccs does a faster forward-only
	 * search of the ALS-list, but if false it does a full search of all the
	 * possible combinations of ALSs.
	 * <br>HdkAlsXz and HdkAlsXyWing both use true
	 * <br>HdkAlsXyChain uses false to find more chains. It's slow anyway!
	 * @param useStartAndEnd if true then getRccs populates the startIndices
	 * and endIndices arrays.
	 * <br>HdkAlsXz and HdkAlsXyWing both use false
	 * <br>HdkAlsXyChain uses true because it's awkward
	 */
	public AAlsHinter(Tech tech, boolean allowOverlaps
			, boolean allowLockedSets, boolean forwardOnly
			, boolean useStartAndEnd) {
		super(tech);
		this.allowOverlaps = allowOverlaps;
		this.allowLockedSets = allowLockedSets;
		// unused if allowLockedSets, else an array for each of the 27 regions
		this.inLockedSet = allowLockedSets ? null : new boolean[27][];
		this.forwardOnly = forwardOnly;
		this.useStartAndEnd = useStartAndEnd;
	}

	/**
	 * Prepare is called after the LogicalSolver is configured and before we
	 * attempt to solve each Grid. It gives the LogicalSolver and it's hinters
	 * a chance to prepare to solve this new Grid; as distinct from getHints on
	 * probably-the-existing-grid. Also note that Grid now has an id field
	 * which identifies the puzzle that's loaded into the grid
	 *
	 * @param grid
	 */
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		if ( HintValidator.ALS_USES ) {
			// clear any existing invalidities before processing a new Grid.
			// note that HintValidator.solutionValues is already set.
			HintValidator.invalidities.clear();
		}
	}

	/**
	 * findHints is implemented by my subclasses to do the actual hint search.
	 * I pass him everything I know (whether he uses it or not):
	 * @param grid The Grid that we're solving
	 * @param candidates an array of the indices of Grid cells which maybe
	 *  each potential value 1..9
	 * @param rccs The Restricted Common Candidates
	 * @param alss The Almost Locked Sets
	 * @param accu The IAccumulator to which we add any hints
	 * @return true if a hint was found, else false
	 */
	protected abstract boolean findHints(Grid grid, Idx[] candidates
			, Rcc[] rccs, Als[] alss, IAccumulator accu);

	/**
	 * Find Almost Locked Set hints in Grid and add them to IAccumulator.
	 * <p>
	 * @param grid the grid to search for hints
	 * @param accu the chosen implementation of IAccumulator
	 * @return true if any hint/s were found. Different implementations of
	 *  IAccumulator find one or all hints
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// get an array 1..9 of indices of grid cells which maybe each value.
		Idx[] candidates = grid.getIdxs();
		// get the Almost Locked Sets (N cells with N+1 values)
		final Als[] alss = getAlss(grid, candidates);
		// get the Restricted Common Candidates of those ALSs
		final Rcc[] rccs = getRccs(alss);
		// call my sub-types ALS hint search: rummage the RCCs to see if any
		// of them fit the pattern, and if so raise a hint and add it to accu.
		return findHints(grid, candidates, rccs, alss, accu);
	}

	/**
	 * getAlss starts the recursive ALS search at each cell in each region,
	 * then computeFields of each result ALS (don't waste time computing unused
	 * ALSs) and return them.
	 * <p>
	 * I'm only called locally, but I'm package visible for my test-case.
	 *
	 * @param grid
	 * @param candidates
	 * @return the ALSs in the given grid
	 */
	Als[] getAlss(Grid grid, Idx[] candidates) {
		// if Locked Sets aren't allowed then we'd better find them now ONCE,
		// so that we can check the array later in recurseAlss to ignore them.
		if ( !allowLockedSets )
			findLockedSets(grid);
		// the List of ALSs to populate and return.
		// nb: add ignores an ALS with duplicate indices.
		@SuppressWarnings("serial")
		LinkedList<Als> alss = new LinkedList<Als>() {
			@Override
			public boolean add(Als e) {
				if ( contains(e) )
					return false;
				return super.add(e);
			}
		};
		// start recursion once for each cell in each region
		for ( ARegion region : grid.regions )
			for ( int i=0; i<9; ++i ) {
				indices.clear();
				maybes[0] = 0;
				recurseAlss(1, i, region, alss);
			}
		// compute fields
		for ( Als als : alss )
			als.computeFields(grid, candidates);
		return toHdkAlsArray(alss);
	}
	// the indices in the Set that we're currently recursing (below)
	private final Idx indices = new Idx();
	// an element for each cell in the set which we're recursing (below);
	// each element contains the aglomeration of all maybes upto this point.
	private final int[] maybes = new int[9];

	// List.toArray was slow, so I roll my own to see if I can do better.
	private Als[] toHdkAlsArray(LinkedList<Als> list) {
		Als[] array = new Als[list.size()];
		Als e;
		int cnt = 0;
		while ( (e=list.poll()) != null )
			array[cnt++] = e;
		return array;
	}

	/**
	 * Does a recursive ALS search in region starting at startIndex.
	 * @param N Number of cells in the current set, starts at 1
	 * @param first first index in the region to be examined
	 * @param region the current region to search
	 * @param alss List of ALSs to populate
	 */
	private void recurseAlss(int N, int first, ARegion region
			, List<Als> alss) {
		Cell cell;
		// foreach cell in this 'region', starting at 'first'
		for ( int i=first; i<9; ++i ) {
			// ignore set cells
			if ( (cell=region.cells[i]).value != 0
			  // ignore cells that take part in a Locked Set (if suppressed)
			  || (!allowLockedSets && inLockedSet[region.index][cell.i]) )
				continue; // ignore this cell
			indices.add(cell.i);
			maybes[N] = maybes[N-1] | cell.maybes.bits;
			// it's an ALS if these N cells have N+1 maybes between them.
			if ( SIZE[maybes[N]] == N+1 && (INCL_SINGLE_CELLS || N>1) )
				alss.add(new Als(indices, maybes[N], region));
			// continue recursion
			if ( N < 8 )
				recurseAlss(N+1, i+1, region, alss);
			// remove current cell
			indices.remove(cell.i);
		}
	}

	// Find the Locked Sets (Naked Pair/Triple/etc) in each region in the grid,
	// and stash them in the lockedSets array, and the anyLockedSets array.
	private void findLockedSets(Grid grid) {
		for ( ARegion region : grid.regions ) {
			final int n = region.emptyCellCount; // the size of the master set
			Cell[] rEmptyCells = region.emptyCells(Grid.cas(n));
			// for speed: unpack maybes.bits of empty cells into an array
			final int[] maybeses = diuf.sudoku.Idx.iasA[n];
			for ( int i=0; i<n; ++i )
				maybeses[i] = rEmptyCells[i].maybes.bits;
			// an array of indices of cells in any lockedSet to be cached
			boolean[] array = new boolean[81]; // cooincident with Grid.cells
			int maybes;
			// foreach setSize 2..emptyCellCount-1
			for ( int ss=2; ss<n; ++ss ) // the current setSize
				// foreach possible combo of ss cells in our n empty cells
				for ( int[] perm : new Permutations(n, diuf.sudoku.Idx.iasB[ss]) ) {
					maybes = 0;
					for ( int i=0; i<ss; ++i )
						maybes |= maybeses[perm[i]];
					if ( SIZE[maybes] == ss )
						// ss positions for ss values is a locked set.
						for ( int i=0; i<ss; ++i )
							// cooincident with Grid.cells
							array[rEmptyCells[perm[i]].i] = true;
				}
			// cache the lockedSet
			this.inLockedSet[region.index] = array;
		}
	}

	/**
	 * See if each distinct pair of ALSs have one or two RC values. An RC value
	 * is one that is common to both ALSs where all instances of that value
	 * in both ALSs see each other. Two ALSs can have a maximum of two RCs.
	 * <p>
	 * ALS with RC(s) may overlap as long as the overlapping area doesn't
	 * contain an RC.
	 * <p>
	 * I'm only called locally, but I'm package visible for my test-case.
	 *
	 * @param alss
	 * @return
	 */
	Rcc[] getRccs(final Als[] alss) {
		// final local reference to field for speed (top1465 4 seconds faster)
		final boolean forwardOnly = this.forwardOnly;
		// the list if RCCs to populate and return
		final LinkedList<Rcc> rccs = new LinkedList<>();
		// the number of ALSs to process
		final int n = alss.length;
		// my variables
		Als als1;
		Als als2;
		Rcc rcc;
		Idx overlap = new Idx();
		Idx bothVs = new Idx();
		Idx bothVBuds = new Idx();
		int j, cmnMaybes;
		// recompute start and end indices if required
		if ( useStartAndEnd && (startIndices==null || startIndices.length<n) ) {
			int newCapacity = (int)(n * 1.5);
			startIndices = new int[newCapacity];
			endIndices = new int[newCapacity];
		}
		// foreach distinct pair of ALSs (a forward only search)
		for ( int i=0; i<n; ++i ) {
			als1 = alss[i]; // nb: alss is an ArrayList with O(1) get
			if ( useStartAndEnd )
				startIndices[i] = rccs.size();
			for ( j=forwardOnly?i+1:0; j<n; ++j ) {
				als2 = alss[j];
				// get maybes common to als1 and als2, skip if none
				if ( (cmnMaybes=(als1.maybes & als2.maybes)) == 0 )
					continue; // nothing to do!
				if ( als2 == als1 ) // reference equals OK
					continue;
				// see if the ALSs overlap (we need overlap later anyway)
				overlap.setAnd(als1.set, als2.set);
				if ( !allowOverlaps && overlap.any() )
					continue; // overlaps are supressed
				// set my rcc to null so that we can differentiate between the
				// first and second RC value of each RCC
				rcc = null;
				// foreach maybe common to als1 and als2
				for ( int v : VALUESES[cmnMaybes] ) {
					// get indices of v in both ALSs
					// none of these may be in the overlap
					if ( bothVs.setOr(als1.vs[v], als2.vs[v]).andAny(overlap) )
						continue; // at least one v is in overlap
					// if all v's in both ALSs see each other then v is RCC
					bothVBuds.setAnd(als1.vAll[v], als2.vAll[v]);
					// BEFORE THE LOOP
					// overlap.setAnd(als1.indices, als2.indices);
					// IN THE LOOP
					// rcIndices.setOr(als1.vs[v], als2.vs[v]);
                    // if ( !rcIndices.andEmpty(overlap) )
                    //    continue; // forbidden
					// rcBuds.setAnd(als1.vBuds[v], als2.vBuds[v]);
					// if (rcIndices.andEquals(rcBuds)) { // ((rcIndices & rcBuds) == rcBuds)
					if ( bothVs.andEqualsThis(bothVBuds) )
						if ( rcc == null )
							rccs.add(rcc=new Rcc(i, j, v));
						else
							rcc.setCand2(v);
				}
			}
			if ( useStartAndEnd )
				endIndices[i] = rccs.size();
		}
		return rccs.toArray(new Rcc[rccs.size()]);
	}
    /** The indices of the first RC in {@code rccs} for each ALS in
	 * {@code alss}. This field is set by getRccs and used by HdkAlsXyChain. */
    protected int[] startIndices = null;
    /** The indices of the last RC in {@code rccs} for each ALS in
	 * {@code alss}. This field is set by getRccs and used by HdkAlsXyChain. */
    protected int[] endIndices = null;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~ helper methods ~~~~~~~~~~~~~~~~~~~~~~~~~

	protected static Pots orangePots(Grid grid, Iterable<Idx> sets) {
		Pots pots = new Pots();
		for ( Idx set : sets )
			for ( Cell cell : set.cells(grid) )
				pots.upsert(cell, cell.maybes);
		return pots;
	}
	// wrapper to convert args-array into a List, which is Iterable
	protected static Pots orangePots(Grid grid, Idx... sets) {
		return orangePots(grid, Arrays.asList(sets));
	}

}
