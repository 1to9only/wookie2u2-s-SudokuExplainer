package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxI;
import diuf.sudoku.IdxL;
import diuf.sudoku.IntArrays.IALease;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.utils.Permutations;
import java.util.Arrays;
import static diuf.sudoku.IntArrays.iaLease;

/**
 * AlsFinderPlain is the simplest-possible AlsFinder. Iterating Permutations is
 * known slow, but it's still faster than the recursive alternative. BFIIK.
 * <pre>
 * 2021-09-14 I'm about a second faster than AlsFinderRecursive.
 * HIM 4,043,189,800 12568 321,705 2218 1,822,898 BigWings
 * ME  3,016,482,600 12550 240,357 2186 1,379,909 BigWings
 * 2021-09-28 Using the new AlsArraySet which wraps a Als[] into a
 * {@code Set<Als>} of which I use just the add method, but it's internal
 *  array is public, as it's size, so instead of an Iterator I get the "raw"
 *  array, and array-iterator over it, which is a bit faster.
 * V02 3,265,100,400 12483 261,563 2158 1,513,021 BigWings BUGGER!
 * V03 3,337,909,700 12483 267,396 2158 1,546,760 BigWings DOUBLE BUGGER!
 * V04 3,129,754,500 12483 250,721 2158 1,450,303 BigWings STILL BUGGER!
 * </pre>
 *
 * @author Keith Corlett 2021-09-14
 */
final class AlsFinderPlain extends AlsFinder {

	// DEGREE is the maximum degree + 1 (a ceiling), ie 8
	// * 9 is right out coz all empty cells in a region are never an Almost
	//   Locked Set (ALS), they are always a Locked Set (LS).
	// * We never use ALS's of 8 cells, so don't waste time finding them:
	//   * BigWings/AlsXz/AlsWing/AlsChain use 2..DEGREE_CEILING-1
	//   * BigWings: WXYZ=3, VWXYZ=4, UVWXYZ=5, TUVWXYZ=6, STUVWXYZ=7 (dropped)
	// * This constant is package visible for use the test-cases.
	// ------------------------------------------------------------------------
	// So should we go after ALSs of 7 cells?
	//
	// 8 with the lot
	//       time(ns)  calls	t/call  elims    t/elim hinter
	//     17,931,000 105881        169 56359       318 NakedSingle
	//  2,568,648,600  11593    221,568  2107 1,219,102 BigWings
    //  4,690,774,700  10039    467,255  2530 1,854,061 DeathBlossom
	//  9,296,150,600   7908  1,175,537  2013 4,618,057 ALS_XZ
    //  6,479,173,800   6633    976,808  2753 2,353,495 ALS_Wing
    // 12,726,866,000   4457  2,855,478  1775 7,170,065 ALS_Chain
	// 85,340,903,300 (01:25)
	//
	// 7 without STUVWXYZ's:
	//     18,532,500 105812       175 56340       328 NakedSingle
	//  2,466,231,200  11561   213,323  2108 1,169,938 BigWings
    //  4,191,575,300  10011   418,696  2469 1,697,681 DeathBlossom
    //  8,344,245,900   7923 1,053,167  2020 4,130,814 ALS_XZ
    //  5,928,498,400   6648   891,771  2742 2,162,107 ALS_Wing
    // 11,678,813,500   4482 2,605,714  1792 6,517,195 ALS_Chain
	// 83,674,674,500 (01:23)
	//
	// 6 without TUVWXYZ's and STUVWXYZ's:
	//     18,651,229 105926       176 56551       329 NakedSingle
	//  2,077,815,884  11541   180,037  2059 1,009,138 BigWings
	//  3,111,053,653  10026   310,298  2257 1,378,402 DeathBlossom
	//  5,457,063,601   8125   671,638  2050 2,661,982 ALS_XZ
	//  3,791,441,673   6834   554,790  2613 1,450,991 ALS_Wing
	//  7,622,340,343   4778 1,595,299  1873 4,069,589 ALS_Chain
	// 73,691,043,001 (01:13)
	//
	// CONCLUSION: skip 6s and 7s is 12 secs faster, but increases NakedSingle
	// calls by 114. So how does that sit with my stated purpose: to find the
	// simplest possible solution to any Sudoku puzzle as quickly as possible.
	// Well, honestly, it's contrary, so I accept this change only as a massive
	// expediancy. It produces degraded analysis.
	// ------------------------------------------------------------------------
	static final int DEGREE_CEILING = 6; // No TUVWXYZ, No STUVWXYZ

	// clean 'dirt' out of the array of 'n' 'cells'.
	// Do NOT call me if 'dirt' for this region is empty.
	// returns the possibly reduced number of 'cells' (n)
	private static int clean(final Idx dirt, final Cell[] cells, final int n) {
		int i, j, m;
		int result = n; // the possibly reduced number of cells
		for ( i=0; i<n; ++i )
			if ( dirt.has(cells[i].i) ) {
				for ( j=i,m=result-1; j<m; ++j )
					cells[j] = cells[j+1];
				--result;
			}
		return result;
	}

	// an array of AlsSet's, one for each size 2..DEGREE-1.
	// this is a field only for performance, to create them ONCE.
	private final AlsArraySet[] sets = new AlsArraySet[DEGREE_CEILING];
	// an array of cells having size 2..degree+1
	private final Cell[] sized = new Cell[REGION_SIZE];

	public AlsFinderPlain() {
		// sets are by size, and size is 2..degree-1
		for ( int i=2; i<DEGREE_CEILING; ++i ) {
			sets[i] = new AlsArraySet(); // no duplicate filter
		}
	}

	// find all distinct Almost Locked Sets in 'grid' sized 2..7.
	// If allowNakedSets is false then
	//    any cell that is part of a LockedSet in this region is ignored.
	// add each Als found to the 'alss' array, and return how many.
	@Override
	int getAlss(final Grid grid, final Als[] alss, final boolean allowNakedSets) {
//long start, t;
		// ANSI-C style variables, to minimise stack-work
		// reference-type is implementation-type because I use array field.
		AlsArraySet alsSet;
		int[] mb; // maybes of sized cells, coincident with 'sized'
		int i // ubiqituous index
		  , D // maximum number of cells in an ALS
		  , d // degree: the size of each ALS
		  , c // d - 1: floor for number of cells, for > instead of >=
		  , e // d + 1: number of maybes in each ALS of d cells
		  , f // d + 2: one more than e, to use < instead of <=
		  , m // combination of maybes of these cells
		  , n; // number of cells in region having size 2..degree+1
//start = System.nanoTime();
		// local stack-references for speed
		final AlsArraySet[] alsSets = this.sets;
		final Cell[] sized = this.sized;
		// the number of ALS's found (my return value)
		int result = 0;
		// if allowNakedSets then noDirt just stays true; else noDirt is set
		// to "Does this region contain no nakedSets" and most regions have
		// no nakedSets, so we skip clean'ing dirt, which is a bit faster.
		boolean noDirt = allowNakedSets; // remains true when allowNakedSets
		Idx dirt = null; // indices of cells in nakedSet/s in this region
		// if nakedSet's are suppressed then find nakedSetIdxs
		if ( !allowNakedSets )
			setNakedSetIdxs(grid); // set nakedSetIdxs
		// foreach region in the grid (boxs, rows, cols)
		for ( ARegion r : grid.regions ) {
			// are there any nakedSets in this region?
			if ( !allowNakedSets )
				noDirt = (dirt=nakedSetIdxs[r.index]).none();
			// D is ceiling for num cells in each ALS capped at DEGREE_CEILING.
			// It's a ceiling coz all empty cells in a region are a LockedSet.
			D = Math.min(r.emptyCellCount, DEGREE_CEILING);
			// foreach degree: the number of cells in each ALS
			// d = the current degree
			// c = floor for number of sized cells (one less than d)
			// e = number of maybes in an ALS (one more than d)
			// f = one more than that, to use < instead of <=
			for ( c=1,d=2,e=3,f=4; d<D; c=d,d=e,e=f++ ) {
				// the alsSet for this degree
				alsSet = alsSets[d];
				// get region.cells with size 2..d+1; an ALS of d cells can't
				// contain a cell with more than d+1 maybes, so ignore them.
				// NOTE: There is confusion over whether we need >= d or > d
				// cells in a region from which to form an ALS of d cells.
				// The test-cases require >= d but I think it should be > d
				// because all empty cells in a region form a LockedSet, but
				// that makes AlsXzTest fail, and I don't understand why.
				if ( (n=r.sized(f, sized)) > c
				  // if any nakedSets in region then ignore there cells
				  && ( noDirt || (n=clean(dirt, sized, n)) > c ) )
					// read maybes of sized, to not derefence repeatedly,
					// because the number of permutations is exponential.
					try ( final IALease mbLease = iaLease(n);
						  final IALease paLease = iaLease(d) ) {
						mb = mbLease.array; // maybes
						mb[0] = sized[0].maybes;
						mb[1] = sized[1].maybes;
						for ( i=2; i<n; ++i )
							mb[i] = sized[i].maybes;
						// foreach distinct combo of d among n cells in region,
						// each having 2..d+1 maybes (minus nakedSets)
						for ( int[] pa : new Permutations(n, paLease.array) )
							// build m = aggregate of maybes in 'perm' cells.
							// do first two cells and skip if theres already
							// excess maybes, for speed.
							if ( VSIZE[m=mb[pa[0]]|mb[pa[1]]] < f ) {
								// or-in maybes of remainder of degree cells
								for ( i=2; i<d; ++i )
									m |= mb[pa[i]];
								// if there's 1 more maybe than cells then
								if ( VSIZE[m] == e )
									// create a new Als and add to sets[size]
									// nb: Plain produces NO duplicates, so my
									//     add doesn't check, so I'm faster.
									alsSet.add(new Als(IdxI.of(sized, pa, d), m, r));
							}
					}
			}
		}
//COUNTS[0] += (t=System.nanoTime()) - start;
//start = t;
		// add each als to the alss array, and computeFields of each
		for ( d=2; d<DEGREE_CEILING; ++d ) { // degree
			// downside of generics: array is an Object[] so we must cast each
			// Object back to Als before we use it.
			final Object[] objects = (alsSet=alsSets[d]).array;
			for ( i=0,n=alsSet.size; i<n; ++i ) {
				alss[result] = ((Als)objects[i]).computeFields(grid, result);
				++result;
			}
			// and clear this set (sets is a field for performance)
			alsSet.clear();
		}
//COUNTS[1] += System.nanoTime() - start;
		// clean-up: a cell reference holds the whole grid in memory
		Arrays.fill(sized, null);
		return result;
	}

}
