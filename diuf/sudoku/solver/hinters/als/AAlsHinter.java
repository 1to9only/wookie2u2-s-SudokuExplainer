/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.Permutations;
import java.util.Arrays;
import java.util.LinkedList;


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

	// DeathBlossom uses this to mark unused params; for self-doc'ing code.
	protected static final boolean UNUSED = false;

	/** include ALSs which overlap in my findHints method? */
	protected final boolean allowOverlaps;
	/** include ALSs which include cells that are part of a Locked Set? */
	protected final boolean allowLockedSets;
	/** should I run getRccs; false means I pass you rccs=null in findHints */
	protected final boolean findRCCs;
	/** should getRccs do a fast forwardOnly search, or a full search? */
	protected final boolean forwardOnly;
	/** should getRccs populate startIndices and endIndices */
	protected final boolean useStartAndEnd;

	// inLockedSet[region][cell.i]: findLockedSets => recurseAlss when
	// !allowLockedSets (in HdkAlsXyChains only): element set to true if this
	// cell is involved in any Locked Set in this region.
	protected final boolean[][] inLockedSet;

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
	 * @param allowLockedSets if false the getAlss method ignores any cell
	 * which is part of an actual Locked Set (NakedPair/Triple/etc) in this
	 * region (ie treats the cell as if it were a set cell).
	 * <br>HdkAlsXz and HdkAlsXyWing both use true
	 * <br>HdkAlsXyChain uses false because locked sets bugger it up
	 * @param findRCCs true for "normal" ALS-use, false for DeathBlossom.<br>
	 * When false AAlsHinter does NOT run getRccs, so findHints rccs=null, and
	 * the following parameters (effecting getRccs) have no effect, so there's
	 * an UNUSED constant defined for self-documentation.
	 * @param allowOverlaps if false the getRccs method ignores two ALSs which
	 * physically overlap (ie have any cell in common).
	 * <br>HdkAlsXz and HdkAlsXyWing both use true
	 * <br>HdkAlsXyChain uses false because overlaps bugger it up
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
	public AAlsHinter(Tech tech, boolean allowLockedSets, boolean findRCCs
			, boolean allowOverlaps, boolean forwardOnly
			, boolean useStartAndEnd) {
		super(tech);
		this.allowLockedSets = allowLockedSets;
		// unused if allowLockedSets, else an array for each of the 27 regions
		if ( allowLockedSets )
			this.inLockedSet = null;
		else
			this.inLockedSet = new boolean[27][];
		this.findRCCs = findRCCs;
		this.allowOverlaps = allowOverlaps;
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
		if ( HintValidator.ALS_USES || HintValidator.DEATH_BLOSSOM_USES ) {
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
		// if my subclass said I should in my constructor
		final Rcc[] rccs;
		if ( findRCCs )
			rccs = getRccs(alss); // for all ALS's
		else
			rccs = null; // for DeathBlossom
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
		alss = new AlsSet(); // add ignores duplicates
		// start recursion once for each cell in each region
		for ( ARegion r : grid.regions ) {
			region = r; // set a field rather than pass it down on the stack
			for ( int i=0; i<9; ++i ) {
				indices.clear();
				maybes[0] = 0;
				recurseAlss(1, i);
			}
		}
		// compute fields
		for ( Als als : alss )
			als.computeFields(grid, candidates);
		return alss.toArray();
	}
	// the list of ALS's to populate
	private AlsSet alss;
	// the current region, a field rather than pass it down on the stack
	private ARegion region;
	// the indices in the Set that we're currently recursing (below)
	private final Idx indices = new Idx();
	// an element for each cell in the set which we're recursing (below);
	// each element contains the aglomeration of all maybes upto this point.
	private final int[] maybes = new int[9];

	/**
	 * Does a recursive ALS search in region starting at first.
	 * <p>
	 * The region field is pre-set to the region we're currently searching.
	 * <p>
	 * The alss field is pre-set to the ALS-Set to populate.
	 *
	 * @param N Number of cells in the current set, starts at 1
	 * @param first first index in the region to be examined, starts at i
	 */
	private void recurseAlss(int N, int first) {
		Cell cell;
		// foreach cell in this 'region', starting at 'first'
		for ( int i=first; i<9; ++i )
			// skip set cells
			if ( (cell=region.cells[i]).value == 0
			  // skip cells that are part of a Locked Set (if suppressed)
			  && (allowLockedSets || !inLockedSet[region.index][cell.i]) ) {
				indices.add(cell.i);
				maybes[N] = maybes[N-1] | cell.maybes.bits;
				// it's an ALS if these N cells have N+1 maybes between them.
				if ( VSIZE[maybes[N]]==N+1 && (INCL_SINGLE_CELLS || N>1) )
					// nb: add ignores duplicate ALS's (same indices)
					alss.add(new Als(indices, maybes[N], region));
				// continue recursion unless this is the last cell
				if ( N < 8 )
					recurseAlss(N+1, i+1);
				// remove current cell
				indices.remove(cell.i);
			}
	}

	// Find the Locked Sets (Naked Pair/Triple/etc) in each region in the grid,
	// and stash them in the lockedSets array, and the anyLockedSets array.
	private void findLockedSets(Grid grid) {
		for ( ARegion r : grid.regions ) {
			final int n = r.emptyCellCount; // the size of the master set
			Cell[] rEmptyCells = r.emptyCells(Grid.cas(n));
			// for speed: unpack maybes.bits of empty cells into an array
			final int[] maybeses = diuf.sudoku.Idx.IAS_A[n];
			for ( int i=0; i<n; ++i )
				maybeses[i] = rEmptyCells[i].maybes.bits;
			// an array of indices of cells in any lockedSet to be cached
			boolean[] array = new boolean[81]; // cooincident with Grid.cells
			int maybes;
			// foreach setSize 2..emptyCellCount-1
			for ( int ss=2; ss<n; ++ss ) // the current setSize
				// foreach possible combo of ss cells in our n empty cells
				for ( int[] perm : new Permutations(n, diuf.sudoku.Idx.IAS_B[ss]) ) {
					maybes = 0;
					for ( int i=0; i<ss; ++i )
						maybes |= maybeses[perm[i]];
					if ( VSIZE[maybes] == ss )
						// ss positions for ss values is a locked set.
						for ( int i=0; i<ss; ++i )
							// cooincident with Grid.cells
							array[rEmptyCells[perm[i]].i] = true;
				}
			// cache the lockedSet
			this.inLockedSet[r.index] = array;
		}
	}

	/**
	 * See if each distinct pair of ALSs have one or possibly two RC values.
	 * An RC value is one that is common to both ALSs where all instances of
	 * that value in both ALSs see each other (that's the restriction).
	 * <p>
	 * Two ALSs can have a maximum of two RC values, which are both stored in
	 * the one Rcc.
	 * <p>
	 * ALSs can overlap as long as the overlapping area doesn't contain an RC.
	 * <p>
	 * I'm only called locally, but I'm package visible for my test-case. sigh.
	 *
	 * @param alss
	 * @return
	 */
	Rcc[] getRccs(final Als[] alss) {
		Als a, b; // two ALSs intersect on a Restricted Common value/s (RCC)
		Rcc rcc;
		int j, cmnMaybes;
		// do we examine only alss to my right? or all of them?
		final boolean forwardOnly = this.forwardOnly;
		// the list of RCCs to populate and return the array of
		final LinkedList<Rcc> rccs = new LinkedList<>();
		// the number of ALSs to process
		final int n = alss.length;
		// get and clear the Idx fields rather than wear cost of creatining.
		// did I mention that I'm tighter than a fishes asshole?
		final Idx overlap = this.overlap.clear();
		final Idx bothVs = this.bothVs.clear();
		final Idx bothVBuds = this.bothVBuds.clear();
		// recompute start and end indices if required
		if ( useStartAndEnd && (startIndices==null || startIndices.length<n) ) {
			int newCapacity = (int)(n * 1.5);
			startIndices = new int[newCapacity];
			endIndices = new int[newCapacity];
		}
		// foreach distinct pair of ALSs (a forward only search)
		for ( int i=0; i<n; ++i ) {
			a = alss[i]; // nb: alss is an ArrayList with O(1) get
			if ( useStartAndEnd )
				startIndices[i] = rccs.size();
			if(forwardOnly) j = i + 1; else j = 0; // terniaries are slow!
			while ( j < n ) {
				b = alss[j];
				// get maybes common to als1 and als2, skip if none
				if ( (cmnMaybes=(a.maybes & b.maybes)) != 0
				  && b != a ) { // reference equals OK
					// see if the ALSs overlap (we need overlap later anyway)
					overlap.setAnd(a.idx, b.idx);
					if ( allowOverlaps || overlap.none() ) {
						// to tell between 1st and 2nd RC value of each RCC
						rcc = null;

	// <OUTDENT comment="no wrapping">
	// foreach maybe common to a and b
	for ( int v : VALUESES[cmnMaybes] ) {
		// get indices of v in both ALSs
		// none of these may be in the overlap
		if ( !bothVs.setOr(a.vs[v], b.vs[v]).andAny(overlap)
		  // if all v's in both ALSs see each other then v is an RC
		  && bothVs.andEqualsThis(bothVBuds.setAnd(a.vAll[v], b.vAll[v])) )
			if ( rcc == null )
				rccs.add(rcc=new Rcc(i, j, v));
			else // a rare second RC value in the one RCC
				rcc.setCand2(v);
	}
	// </OUTDENT>

					}
				}
				++j;
			}
			if ( useStartAndEnd )
				endIndices[i] = rccs.size();
		}
		return rccs.toArray(new Rcc[rccs.size()]);
	}
	private final Idx overlap = new Idx();
	private final Idx bothVs = new Idx();
	private final Idx bothVBuds = new Idx();
    /** The indices of the first RC in {@code rccs} for each ALS in
	 * {@code alss}. This field is set by getRccs and used by HdkAlsXyChain. */
    protected int[] startIndices = null;
    /** The indices of the last RC in {@code rccs} for each ALS in
	 * {@code alss}. This field is set by getRccs and used by HdkAlsXyChain. */
    protected int[] endIndices = null;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~ helper methods ~~~~~~~~~~~~~~~~~~~~~~~~~

	protected static Pots oranges(Grid grid, Iterable<Idx> sets) {
		Pots pots = new Pots();
		for ( Idx set : sets )
			for ( Cell cell : set.cells(grid) )
				pots.upsert(cell, cell.maybes);
		return pots;
	}
	// wrapper to convert args-array into a List, which is Iterable
	protected static Pots orangePots(Grid grid, Idx... sets) {
		return oranges(grid, Arrays.asList(sets));
	}

	/**
	 * Extend {@code MyLinkedHashSet<Als>} to make the add method check that
	 * the given ALS does not already exist in this Set.
	 * <p>
	 * Note that add uses the contains method, which is much faster in a Set
	 * than in a List, especially a LinkedList, as this was previously.
	 * <p>
	 * I extend MyLinkedHashSet instead of java.util.HashSet for the poll
	 * method, which is used in toArray.
	 */
	private static class AlsSet extends MyLinkedHashSet<Als> {
		private static final long serialVersionUID = 3356942459562L;
		@Override
		public boolean add(Als e) {
			if ( contains(e) )
				return false;
			return super.add(e);
		}
		/**
		 * WARNING: This toArray method clears this Set!
		 */
		@Override
		public Als[] toArray() {
			Als e;
			final Als[] array = new Als[super.size()];
			int cnt = 0;
			while ( (e=super.poll()) != null )
				array[cnt++] = e;
			return array;
		}
	};


}
