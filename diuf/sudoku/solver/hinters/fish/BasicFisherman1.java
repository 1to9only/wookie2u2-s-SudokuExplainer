/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Indexes.ISIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BasicFisherman1 implements the basic Fish Sudoku solving technique. A basic
 * fish has N regions (bases) that have only the same N possible positions for
 * the Fish Candidate Value (v), so that v is "locked into" the cells at the
 * intersections of the bases and the covers (the cross regions), removing any
 * other possible places from the covers.
 * <p>
 * Bases are rows, and covers are cols; then<br>
 * bases are cols, and covers are rows.
 * <p>
 * BasicFisherman1 is a re-implementation of BasicFisherman using hobiwans
 * "iterable stack-frame technique" to search all possible combos of bases;
 * because it is (I hope) faster than Nicholas Juillerats Permutations. The
 * Permutations class next method is a bit slow. A stack is faster (I hope),
 * but it's pretty hard to get your head around how a stack works. It's used
 * to search all possible combinations of degree bases amongst N bases having
 * 2..degree v's.
 * <p>
 * ALL BAD! This implementation is actually SLOWER than the original.
 * OLD BasicFisherman using Juillerat's Permutations class (a bit slow):
 * 34,936,711  15857  2,203  421   82,985 Swampfish
 * 61,407,695  14112  4,351  231  265,834 Swordfish
 * 17,186,726  12145  1,415    0        0 Jellyfish
 * NEW BasicFisherman1 KRC's implementation of hobiwan's stack technique:
 * 47,547,723  15857  2,998  423  112,405 Swampfish
 * 59,638,448  14112  4,226  230  259,297 Swordfish
 * 83,077,063  12145  6,840    0        0 Jellyfish
 * <p>
 * So I stick with BasicFisherman, and retain BasicFisherman1 for edification:
 * Don't try this, because I did, and it doesn't. sigh. What I don't understand
 * is WHY the original is faster than this. I'm sure that Permutations.next is
 * a pig, and I think a stack is MUCH faster; so what went wrong? BFIIK! But
 * they are both "fast enough", so choose fruit from apples and apples.
 * <p>
 * This implementation finds 2 more Swampfish and 1 more Swordfish, which casts
 * some doubt upon the quality of the existing BasicFisherman implementation.
 * It's missing tricks! I tried to work-out exactly what the new ones are, and
 * I've just given up; but I may try again later. I've got the s__ts with it!
 * Making this s__t CORRECT is much harder than merely getting it to "work".
 * <p>
 * <p>
 * KRC 2021-05-03 I investigated BasicFisherman's "missing" hints. The original
 * searched cols then rows (I search rows then cols) to find different hints
 * first, and therefore follow a different path through the puzzle when a fish
 * occurs in both rows and cols (fairly common) so the original is fine, it's
 * just random (depending on data) as to which approach finds more fish.
 * <p>
 * I also had a go at speeding-up BasicFisherman1: with the for-v-loop down in
 * search, instead of calling search 9 times more often:
 *  50,410,316  15856  3,179  420  120,024 Swampfish
 *  67,955,459  14112  4,815  230  295,458 Swordfish
 * 103,724,266  12145  8,540    0        0 Jellyfish
 * But it's slower again! So let's just stick with the original. sigh.
 * <p>
 * KRC 2021-05-18 Trying to Make this faster again.
 *  55,137,900  17023  3,239  620   88,932 Swampfish
 *  74,345,200  14111  5,268  230  323,240 Swordfish
 * 105,820,000  12164  8,699    0        0 Jellyfish
 * And it's slower again, so try again. sigh.
 * 
 *  56,645,800  17023  3,327  620   91,364 Swampfish
 *  75,291,200  14111  5,335  230  327,353 Swordfish
 * 108,898,900  12164  8,952    0        0 Jellyfish
 * And it's slower again, so try again. sigh.
 * 
 *  55,080,200  17023  3,235  620   88,839 Swampfish
 *  71,863,700  14111  5,092  230  312,450 Swordfish
 * 104,190,200  12164  8,565    0        0 Jellyfish
 * Jesus this is frustrating. Just use the ____ing original!
 *
 * @author Keith Corlett 2021-05-01
 */
public class BasicFisherman1 extends AHinter {

	/**
	 * An entry in the "stack" of bases, which is used to process all possible
	 * combinations of degree in n bases.
	 */
	private final class BaseStackEntry {
		// the index of this base in the bases array.
		public int index;
		// a bitset of the indexes of cells which maybe v in these bases.
		public int vs;
		// debug only, not used in actual code.
		@Override
		public String toString() {
			return ""+index+": "+Indexes.toString(vs);
		}
	}

	/**
	 * This array contains those bases having 2..degree places for v.
	 * Note that the stack index is into this array (not possibleBases).
	 */
	private final ARegion[] bases = new ARegion[9];

	/**
	 * This array is used as a stack. 0 is a stopper. 1 is the first base. We
	 * calculate the cells at the current level by adding the cells which maybe
	 * v in this base to those in the previous level; so that they build-up
	 * from left-to-right. When level==degree then we have "complete fish", so
	 * we see if it eliminates anything.
	 */
	private final BaseStackEntry[] stack;

	/**
	 * Constructor.
	 * @param tech to implement: Swampfish=2, Swordfish=3, Jellyfish=4.
	 */
	public BasicFisherman1(Tech tech) {
		super(tech);
		// check that I was passed a Tech that I actually implement
		assert tech==Tech.Swampfish || tech==Tech.Swordfish || tech==Tech.Jellyfish;
		// the working stack entries are index 1..degree; 0 is just a stopper.
		stack = new BaseStackEntry[degreePlus1];
		for ( int i=0; i<degreePlus1; ++i )
			stack[i] = new BaseStackEntry();
	}

	// the grid to search
	private Grid grid;
	// the IAccumulator to which I add hints
	private IAccumulator accu;

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		this.grid = grid;
		this.accu = accu;
		boolean result = false;
		try {
			if ( accu.isSingle() ) {
				if ( search(grid.rows, grid.cols)
				  || search(grid.cols, grid.rows) )
					return true;
			} else {
				result |= search(grid.rows, grid.cols)
						| search(grid.cols, grid.rows);
			}
		} finally {
			this.grid = null;
			this.accu = null;
			Arrays.fill(this.bases, null);
		}
		return result;
	}

	/**
	 * Search all combinations of bases in N possibleBases having 2..degree v's
	 * for a Fish of size degree. Create a hint for each Fish and add it to the
	 * accu. If the accu says "stop now" then I stop, to produce one hint.
	 *
	 * @param possibleBases grid.rows/cols
	 * @param covers grid.cols/rows
	 * @return were any hints found?
	 */
	private boolean search(ARegion[] possibleBases, ARegion[] covers) {
		// Search each possible combination of degree bases in n bases
		BaseStackEntry c; // current stack entry
		int level // current level in the stack
			, i // ubiquitios index
			, card // cardinality: number of v's in this possible base
			, n; // number of bases
		// presume that no hint will be found
		boolean result = false;
		// foreach possible fish candidate value
		for ( int v=1; v<10; ++v ) {
//System.out.println(""+v+" possibleBases: "+Regions.toString(possibleBases, 9));
			// bases array := possibleBases having 2..degree places for v
			n = 0;
			for ( ARegion base : possibleBases )
				// cardinality: the number of v's in this possibleBase
				if ( (card=base.indexesOf[v].size)>1 && card<degreePlus1 )
					bases[n++] = base;
//System.out.println(""+v+": bases: "+Regions.toString(bases, n));
			// if there's sufficient bases to form a fish of my size
			if ( n >= degree ) {
				// the first level is 1. Start at it's first base.
				stack[level=1].index = 0;
				// keep going until all levels of the stack are exhausted; we
				// build the stack up from left-to-right. Stack is an array of
				// degreePlus1 BaseStackEntry. 0 is just a stopper; active
				// entries are at index 1..degree.
				// ============================================================
				// PERFORMANCE this loop runs billions of times, so do the
				// minimum required, efficiently.
				// ============================================================
				LOOP: for(;;) {
					// fallback levels while this level is exhausted
					// and set the current stack entry
					while ( (c=stack[level]).index >= n )
						if ( --level < 1 )
							break LOOP; // all done
					// current v's = previous v's + this bases v's
					c.vs = stack[level-1].vs | bases[c.index++].indexesOf[v].bits;
					// if the fish is not yet complete
					if ( level < degree ) {
						// if there's not already too many v's in these bases
						if ( ISIZE[c.vs] < degreePlus1 )
							// move onto the next level in the stack, starting
							// with the base after current base (forward-only).
							stack[++level].index = c.index;
					} else if ( ISIZE[c.vs] == degree ) {
						// It's a fish, but does it eliminate anything?
						// ====================================================
						// PERFORMANCE not such an issue here; it runs
						// thousands of times, not billions.
						// ====================================================
						// get indices of the v's in the bases
						baseVs.set(bases[stack[1].index-1].idxs[v]);
						for ( i=2; i<degreePlus1; ++i )
							baseVs.or(bases[stack[i].index-1].idxs[v]);
						// get indices of the v's in the covers
						coverVs.clear();
						for ( i=0; i<9; ++i )
							if ( (c.vs & ISHFT[i]) != 0 )
								coverVs.or(covers[i].idxs[v]);
						// if there's any v's in covers and not bases
						if ( coverVs.andNot(baseVs).any() ) {
							// FOUND a Fish, with eliminations!
							final Pots reds = new Pots();
							final int fsv = VSHFT[v];
							coverVs.forEach(grid.cells, (cc) ->
								reds.put(cc, fsv)
							);
							final AHint hint = createHint(v, reds
									, Regions.list(degree, covers, c.vs));
							result = true;
							if ( accu.add(hint) )
								return result; // exit-early
						}
					}
				}
			}
		}
		return result;
	}
	// indices of v's in the base regions
	private final Idx baseVs = new Idx();
	// indices of v's in the cover regions
	private final Idx coverVs = new Idx();

	/**
	 * Construct a new BasicFishHint, mostly from fields.
	 *
	 * @param v the Fish candidate value
	 * @param reds the removable Cell=>Values
	 * @param coversL a List of cover regions in this fish
	 * @return a new BasicFishHint, always.
	 */
	private AHint createHint(final int v, final Pots reds, final List<ARegion> coversL) {
		// get highlighted (green) potentials = the corners
		final Pots greens = new Pots();
		final int sv = VSHFT[v];
		for ( int i=1; i<degreePlus1; ++i )
			for ( Cell cc : bases[stack[i].index-1].cells )
				if ( (cc.maybes & VSHFT[v]) != 0 )
					greens.put(cc, sv);
		// get a List of the base regions in this fish
		final List<ARegion> basesL = new ArrayList<>(degree);
		for ( int i=1; i<degreePlus1; ++i )
			basesL.add(bases[stack[i].index-1]);
		// construct the hint and return it
		return new BasicFishHint(this, reds, v, greens, EMPTY_STRING, basesL, coversL);
	}

}
