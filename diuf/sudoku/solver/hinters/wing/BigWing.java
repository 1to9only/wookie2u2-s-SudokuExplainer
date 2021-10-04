package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.Arrays;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Grid.LATER_BUDS;
import static diuf.sudoku.Values.VFIRST;

/**
 * Implementation of the "S/T/U/V/WXYZ-Wing" solving techniques.
 * <pre>
 * Similar to ALS-XZ
 * with the larger ALS being of 3..8 cells
 * *     WXYZ_Wing the ALS has 3 cells sized 2..4
 * *    VWXYZ_Wing the ALS has 4 cells sized 2..5
 * *   UVWXYZ_Wing the ALS has 5 cells sized 2..6
 * *  TUVWXYZ_Wing the ALS has 6 cells sized 2..6
 * * STUVWXYZ_Wing the ALS has 7 cells sized 2..8
 * and the smaller ALS being just a bivalue cell;
 * catches the double linked version which is similar to Sue-De-Coq
 * <p>
 * NOTE: BigWing is only use of BitIdx, which pushes feelers into everything,
 * so replacing BitIdx with the "normal" Idx eradicates lots of code. KRC has
 * tried and failed to do this a couple of times. In the end I kept BitIdx,
 * letting it grow into every-bloody-thing. sigh.
 *
 * KRC 2021-06-10 Having failed to replace BigWing with the BigWings class,
 * I decided to just replaced BitIdx with Idx in BigWing, to allow me to remove
 * the BitIdx class, which I hate because you have to set and clear it's grid.
 * Also deleted all BitIdx dependant code from Grid. All-up we're maybe 3000
 * lines lighter now.
 * </pre>
 *
 * @author Keith Corlett 2010 May IIRC My code based on Juillerat's concept.
 */
public class BigWing extends AHinter {

	/**
	 * Does value form a Wing in these cells? Ie do all occurrences of value
	 * in the ALS (there must be atleast one) see the YZ cell?
	 *
	 * @param cand the bitset representation of the candidate value,
	 *  VSHFT[x] or VSHFT[z] in ALS-XZ terms. Usually called sv, and not
	 *  usually passed around
	 * @param yz cell contains values x and z, to complete the wing
	 * @param als the cells in this Almost Locked Set
	 * @return do all ALS cells which maybe value see the YZ cell?
	 */
	private static boolean isWing(final int cand, Cell yz, Cell[] als) {
//		if ( true ) { // @check true
			for ( Cell c : als )
				if ( (c.maybes & cand)!=0 && yz.notSees[c.i] )
					return false;
			return true;
//		} else { // debug version: check yz shares both values with ALS
//			boolean any = false;
//			for ( Cell c : als )
//				if ( (c.maybes & cand) != 0 ) {
//					any = true;
//					if ( yz.notSees[c.i] )
//						return false;
//				}
//			assert any : "any should allways be true because we pretest that: VSIZE[cands[i] & (b=yz.maybes)] == 2";
//			return any;
//		}
	}

	/**
	 * Eliminate 'value' from the 'wing' pattern, adding eliminations to reds,
	 * and return were any found?
	 *
	 * @param isStrong are these eliminations strong ones. A strong elimination
	 *  sees all v's in the wing, including the yz-cell. A weak elimination
	 *  only applies to ALS-only-values in a double-linked wing, and must see
	 *  all v's in the ALS (not the yz-cell).
	 * @param v the value to eliminate; xz.z or xz.x
	 * @param als an array of the ALS cells
	 * @param yz the bivalue cell which completes the Wing pattern
	 * @param reds theReds: THE removable (red) potentials
	 * @return where any eliminations found?
	 */
	private static boolean elim(final boolean isStrong, final int v
			, final Cell[] als, final Cell yz, final Pots reds
			, final Grid grid) {
		final int sv = VSHFT[v]; //shiftedValue
		VICTIMS.set(candidates[v]);
		for ( Cell c : als )
			if ( (c.maybes & sv) != 0 )
				VICTIMS.and(BUDDIES[c.i]);
		if ( isStrong ) // buddy of ALL cells which maybe v, including yz
			VICTIMS.and(BUDDIES[yz.i]);
		else // weak: just remove yz
			VICTIMS.remove(yz.i);
		if ( VICTIMS.none() )
			return false;
		boolean result = false;
		for ( Cell victim : VICTIMS.cells(grid, new Cell[VICTIMS.size()]) )
			result |= reds.upsert(victim, v);
		return result;
	}
	// the victims are the removable cells
	private static final Idx VICTIMS = new Idx();

	// ============================ instance stuff ============================

	// degree is the number of cells in an ALS.
	// WXYZ=3, UWXYZ=4, TUWXYZ=5, STUWXYZ=6
	private final int degreePlus2 = degree + 2;
	// degree is the number of cells in an ALS.
	private final int degreeMinus1 = degree - 1;
	// Idx of the cells at each level 0..degreeMinus1
	private final Idx[] sets = new Idx[degree];
	// the $degree cells in this ALS (Almost Locked Set)
	private final Cell[] als = new Cell[degree];
	// candidates of index+1 cells combined
	private final int[] cands = new int[degree];
	// the removable (red) potentials
	private final Pots theReds = new Pots();
	// the yzs cell set
	private final Idx yzs = new Idx();

	// ---- set and cleared by findHints ----
	// accu.isSingle() ie is only one hint sought
	private boolean onlyOne;
	// indices of cells in grid with maybesSize==2
	private Idx bivalues;
	// indices of cells in grid with value==0
	private Idx empties;
	// indices of grid cells which maybe each value 1..9
	private static Idx[] candidates;
	// the grid to search
	private Grid grid;
	// the hints collector
	private IAccumulator accu;

	public BigWing(Tech tech) {
		super(tech);
		// nb: sets[0] instance is set directly
		for ( int i=0; i<degree; ++i )
			sets[i] = new Idx();
	}

	/**
	 * Find first/all big-wings of size $degree in Grid and add them to accu.
	 * First or all depends on the IAccumulator: if accu.add returns true then
	 * I exit-early. The $degree is set by the Tech passed to my constructor.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// presume that no hint will be found
		boolean result = false;
		try {
			this.grid = grid;
			this.accu = accu;
//			if ( true ) { // @check true
				// CACHED for speed
				bivalues = grid.getBivalue();
				candidates = grid.idxs;
				empties = grid.getEmpties();
//			} else {
//				// UNCACHED for debugging (to reveal a caching issue)
//				bivalues = grid.idx(new Idx(), c->{return c.size==2;});
//				// WARNING: hard-resets the contents of cached idxs
//				candidates = grid.getIdxsImpl();
//				empties = grid.idx(new Idx(), c->{return c.value==0;});
//			}
			onlyOne = accu.isSingle();
			// indices of cells with size 2..4 for WXYZ.degree==3
			sets[0] = empties.where(grid.cells, (c) -> {
				return c.size>1 && c.size<degreePlus2;
			});
			// foreach first-cell-in-an-ALS
			for ( Cell c : sets[0].cells(grid, new Cell[sets[0].size()]) ) {
				als[0] = c; // the first cell in the almost locked set
				cands[0] = c.maybes; // the initial cands
				// check for enough cells (we need degree cells for an ALS)
				// sets[0] contains only cells with <= degree+1 maybes, so
				// there's no need to recheck for it here.
				if ( sets[1].setAndMin(sets[0], LATER_BUDS[c.i], degreeMinus1)
				  // move right to find the next ALS cell, to find a BigWing
				  && (result|=recurse(1)) && onlyOne )
					break;
			}
		} finally {
			// forget all grid and cell references (for GC)
			this.grid = null;
			this.accu = null;
			Arrays.fill(als, null);
			theReds.clear();
		}
		return result;
	}

	/**
	 * Recursively build-up $degree cells in the ALS array, then check each YZ
	 * bivalue cell for a BigWing; add hints to accu; returning any.
	 * <p>
	 * Note that recursion is used just put an arbitrary number of cells in the
	 * ALS array (there's no fancy permutations search or anything); its just
	 * the first way I thought of. There may be a faster iterative way. All
	 * recurse variables (except result) are fields, to minimise stack usage.
	 * eliminate is called 30,000 times in top1465 so performance matters.
	 * <p>
	 * All my helper methods are statics, so everything is passed into them,
	 * and there are no leakages.
	 *
	 * @param i I am always invoked with 1;<br>
	 *  als[0], cands[0] and sets[0] are pre-initialised
	 * @return any hint/s found?
	 */
	private boolean recurse(final int i) {
		boolean result = false;
		for ( Cell c : sets[i].cells(grid, new Cell[sets[i].size()]) ) {
			als[i] = c;
			if ( i < degreeMinus1 ) { // incomplete ALS
				// if existing + this cell together have <= degree+1 maybes
				if ( VSIZE[cands[i]=cands[i-1]|c.maybes] < degreePlus2
				  // need degree cells to form an ALS, this is the (i+1)'th
				  && sets[i+1].setAndMin(sets[i], LATER_BUDS[c.i], degreeMinus1-i)
				  && (result|=recurse(i+1)) && onlyOne )
					return result;
			// degree cells with degree+1 maybes is an Almost Locked Set
			} else if ( VSIZE[cands[i]=cands[i-1]|c.maybes] == degreePlus1 ) {
				yzs.set(BUDDIES[als[0].i]);
				for ( j=1; j<degree; ++j )
					yzs.or(BUDDIES[als[j].i]);
				yzs.removeAll(als);
				if ( yzs.and(bivalues).any() )
					for ( Cell yz : yzs.cells(grid, new Cell[yzs.size()]) )
						if ( VSIZE[cands[i] & (b=yz.maybes)] == 2
						  && ( (xWing=isWing(VSHFT[x=VFIRST[b]], yz, als))
							 | (zWing=isWing(VSHFT[z=VFIRST[b & ~VSHFT[x]]], yz, als)) )
						) {
							if ( !xWing ) {
								j = x;
								x = z;
								z = j;
							}
							strongZ = elim(true, z, als, yz, theReds, grid);
							if ( both = xWing & zWing ) {
								final boolean strongX = elim(true, x, als, yz, theReds, grid);
								boolean weak = false;
								for ( int w : VALUESES[cands[i] ^ b] ) {
									weak |= elim(false, w, als, yz, theReds, grid);
								}
								if ( !weak ) {
									both = strongX & strongZ;
									if ( !strongZ ) {
										j = x;
										x = z;
										z = j;
									}
								}
							}
							if ( !theReds.isEmpty() ) {
								// FOUND a BigWing on x and possibly z
								final Pots oranges = new Pots(als, x);
								oranges.put(yz, VSHFT[x]);
								final AHint hint = new BigWingHint(this
										, theReds.copyAndClear(), yz, x, z
										, both, cands[i], als, oranges);
								result = true;
								if ( accu.add(hint) )
									return result;
							}
						}
			}
		}
		return result;
	}
	// recurse variables are fields to save on stack-work.
	private boolean xWing, zWing, both, strongZ;
	private int j, x, z, b;

}
