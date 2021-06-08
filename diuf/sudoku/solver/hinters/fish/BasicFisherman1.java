/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Indexes.ISIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
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
 * "stack technique" to search all possible combinations of bases; because it
 * is (I hope) faster than Nicholas Juillerats Permutations technique. The
 * Permutations class next method is a bit slow. A stack is faster (I hope),
 * but it's pretty hard to get your head around how a stack works. It's used
 * to search all possible combinations of degree bases amongst N bases having
 * 2..degree v's.
 * <p>
 * ALL BAD! This implementation is actually SLOWER than the original.
 * OLD BasicFisherman ala Juillerat:
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
 * IBFIIK: It's slower again! So stick with the original. sigh.
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
		// for debug only, not that it really matters (not called in prod).
		@Override
		public String toString() {
			return ""+index+": "+Indexes.toString(vs);
		}
	}

	/**
	 * This array contains those bases having 2..degree places for v. It's
	 * important to note that the stack index is into this array, not bases!
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
	 * covers is an array of the cover regions in this Fish.
	 */
	private final ARegion[] covers;

	/**
	 * Constructor.
	 * @param tech to implement: Swampfish=2, Swordfish=3, Jellyfish=4.
	 */
	public BasicFisherman1(Tech tech) {
		super(tech);
		// check that we were passed a Tech that I implement
		assert tech==Tech.Swampfish || tech==Tech.Swordfish || tech==Tech.Jellyfish;
		// the working stack entries are index 1..degree; 0 is just a stopper.
		stack = new BaseStackEntry[degreePlus1];
		for ( int i=0; i<degreePlus1; ++i )
			stack[i] = new BaseStackEntry();
		// the cover Regions array
		covers = new ARegion[degree];
	}

//	private Grid grid;
	private IAccumulator accu;

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
//		this.grid = grid;
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
//			this.grid = null;
			this.accu = null;
			Arrays.fill(this.bases, null);
			Arrays.fill(this.covers, null);
		}
		return result;
	}

	/**
	 * Search all combinations of bases in N possibleBases having 2..degree v's
	 * for a Fish of size degree. Create a hint for each Fish and add it to the
	 * accu. If the accu says "stop now" then we stop now, producing one hint.
	 *
	 * @param possibleBases grid.rows/cols
	 * @param possibleCovers grid.cols/rows
	 * @return were any hints found?
	 */
	private boolean search(ARegion[] possibleBases, ARegion[] possibleCovers) {
		// Search each possible combination of degree bases in n bases
		// the previous and current stack entry
		BaseStackEntry p, c;
		// the current level in the stack of base-entries
		int level, i, tmp, b, baseBits, n, m;
		// the removable (red) potentials, if any
		Pots reds = null;
		// presume that no hint will be found
		boolean result = false;
		// for each possible fish candidate value
		for ( int v=1; v<10; ++v ) {
//System.out.println(""+v+" possibleBases: "+Regions.toString(possibleBases, 9));
			// bases array := possibleBases having 2..degree places for v
			n = 0;
			for ( ARegion r : possibleBases )
				// tmp is the number of v's in this possibleBase
				if ( (tmp=r.indexesOf[v].size)>1 && tmp<degreePlus1 )
					bases[n++] = r;
//System.out.println(""+v+": bases: "+Regions.toString(bases, n));
			// if there's sufficient bases to form a fish of my size
			if ( n >= degree ) {
				// the first level is 1. We always start at it's beginning.
				stack[level=1].index = 0;
				// it's faster to minus 1 ONCE than repeatedly >= n
				m = n - 1;
				// keep going until all levels of the stack are exhausted; we
				// build the stack up from left to right. 0 is just a stopper;
				// active entries are at index 1..degree. Stack is an array of
				// degreePlus1 BaseStackEntry.
				// ============================================================
				// PERFORMANCE this loop runs billions of times, so we do the
				// minimum required as efficiently as possible.
				// ============================================================
				LOOP: for(;;) {
					// fallback levels while this level is exhausted
					while ( (c=stack[level]).index > m )
						if ( --level < 1 )
							break LOOP; // all done
					// get the previous stack entry
					p = stack[level - 1];
					// current v's = previous v's + this bases v's
					// uses the current base index, and post-increments it.
					// to be clear, this builds-up, from left-to-right a bitset
					// of the indexes of all the cells in the bases which maybe
					// v, so when level==degree c.vs is all v's in degree bases
					c.vs = p.vs | bases[c.index++].indexesOf[v].bits;
					// if the fish is not yet complete
					if ( level < degree ) {
						// move onto the next level in the stack
						stack[++level].index = c.index; //index is already next
					} else {
						// we have degree bases (ie the fish is "complete")
						assert level == degree;
//System.out.print("complete: "+v+":");
//for ( i=1; i<degreePlus1; ++i )
//	System.out.print(" "+bases[stack[i].index-1].id);
//System.out.println(" = "+diuf.sudoku.Indexes.toString(c.vs));
						// but do they contain degree v's?
						if ( ISIZE[c.vs] == degree ) {
							// Found a Fish, but does it produce any eliminations?
							// ================================================
							// PERFORMANCE not a big issue from here down; this
							// runs thousands of times, not billions.
							// ================================================
//System.out.print("fish: "+v+":");
//for ( i=1; i<degreePlus1; ++i )
//	System.out.print(" "+bases[stack[i].index-1].id);
//System.out.println(" = "+diuf.sudoku.Indexes.toString(c.vs));
							// build a bitset of bases, to strip from cover v's
							baseBits = 0;
							for ( i=1; i<degreePlus1; ++i )
								baseBits |= ISHFT[bases[stack[i].index-1].index % 9];
//System.out.println("baseBits: "+Indexes.toString(baseBits));
							// get the cover regions
							Regions.select(possibleCovers, c.vs, covers);
//System.out.println("covers: "+Regions.toString(covers, nn));
							// build removable (red) Cell=>Values
							// v's in the covers but not in the bases
							for ( ARegion cover : covers )
								// tmp is removable v's = cover v's - the bases
								if ( (tmp=cover.indexesOf[v].bits & ~baseBits) != 0 ) {
//System.out.println("pink: "+covers[i].id+" "+Indexes.toString(pink));
									for ( b=0; b<9; ++b )
										if ( (tmp & ISHFT[b]) != 0 ) {
											if ( reds == null )
												reds = new Pots();
											reds.put(cover.cells[b], new Values(v));
										}
								}
							// if there are any eliminations
							if ( reds != null ) {
								// FOUND a Fish, with eliminations!
//System.out.println("reds: "+reds);
								// create the hint
								final AHint hint = createHint(v, reds);
//System.out.println("hint: "+hint);
								// remember that we found a hint
								result = true;
								// add the hint to the accu
								// if the passed accu isSingle then stop
								if ( accu.add(hint) )
									return result;
								// clean-up for next time
								reds = null;
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Construct a new BasicFishHint, mostly from fields.
	 *
	 * @param v the Fish candidate value
	 * @param reds the removable Cell=>Values
	 * @return a new BasicFishHint, always.
	 */
	private AHint createHint(final int v, final Pots reds) {
		// get highlighted (green) potentials = the corners
		final Pots greens = new Pots();
		for ( int i=1; i<degreePlus1; ++i )
			for ( Cell cc : bases[stack[i].index-1].cells )
				if ( (cc.maybes.bits & VSHFT[v]) != 0 )
					greens.put(cc, new Values(v));
		// get a List of the base regions in this fish
		final List<ARegion> basesL = new ArrayList<>(degree);
		for ( int i=1; i<degreePlus1; ++i )
			basesL.add(bases[stack[i].index-1]);
		// get a List of cover regions in this fish
		final List<ARegion> coversL = Regions.list(covers);
		// construct the hint and return it
		return new BasicFishHint(this, reds, v, greens, "", basesL, coversL);
	}

}
