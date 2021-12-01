package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxL;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.utils.Permutations;
import java.util.Arrays;

/**
 * AlsFinderPlain is the simplest-possible AlsFinder. Iterating Permutations
 * is "a bit slow", but it's proven faster than the recursive alternative.
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
	//   * AlsXz/Wing/Chain use 2..7
	//   * BigWings: WXYZ=3, VWXYZ=4, UVWXYZ=5, TUVWXYZ=6, STUWXYZ=7
	private static final int DEGREE_CEILING = 8;

	// clean 'dirt' out of the array of 'n' 'cells'.
	// Do NOT call me if 'dirt' for this region is empty.
	// returns the possibly reduced number of 'cells' (n)
	private static int clean(final Idx dirt, final Cell[] cells, final int n) {
		int result = n; // the possibly reduced number of cells
		for ( int i=0; i<n; ++i ) {
			if ( dirt.has(cells[i].i) ) {
				for ( int j=i,m=result-1; j<m; ++j ) {
					cells[j] = cells[j+1];
				}
				--result;
			}
		}
		return result;
	}

	// an array of AlsSet's, one for each size 2..DEGREE-1.
	// this is a field only for performance, to create them ONCE.
	private final AlsArraySet[] sets = new AlsArraySet[DEGREE_CEILING];
	// an array of cells having size 2..degree+1
	private final Cell[] sized = new Cell[9];

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
		// ANSI-C style variables, to minimise stack-work
		// reference-type is implementation-type because I use array field.
		AlsArraySet set;
		int[] pa // the permutations array (degree sized)
		    , maybes; // maybes of sized cells, coincident with 'sized'
		int i // ubiqituous index
		  , D // maximum number of cells in an ALS
		  , d // degree: the size of each ALS
		  , c // d - 1: floor for number of cells, for > instead of >=
		  , e // d + 1: number of maybes in each ALS of d cells
		  , f // d + 2: one more than e, to use < instead of <=
		  , m // combination of maybes of these cells
		  , n; // number of cells in region having size 2..degree+1
		// local stack-references for speed
		final AlsArraySet[] sets = this.sets;
		final Cell[] sized = this.sized;
		// the number of ALS's found (my return value)
		int result = 0;
		// if allowNakedSets then noDirt just stays true; else noDirt is set
		// to "Does this region contain no nakedSets" and most regions have
		// no nakedSets, so we skip clean'ing dirt, which is a bit faster.
		boolean noDirt = allowNakedSets; // stays true if allowNakedSets
		Idx dirt = null; // indices of cells in nakedSet/s in this region
		// if nakedSet's are suppressed then find nakedSetIdxs
		if ( !allowNakedSets ) {
			setNakedSetIdxs(grid); // set nakedSetIdxs
		}
		// foreach region in the grid (boxs, rows, cols)
		for ( ARegion r : grid.regions ) {
			// are there any nakedSets in this region?
			if ( !allowNakedSets ) {
				noDirt = (dirt=nakedSetIdxs[r.index]).none();
			}
			// D is the maximum number of cells in each ALS + 1 (ceiling),
			// which is capped at 8 coz we never use ALS's bigger than 7, and
			// my max-ALS-size is numEmpties-1 coz numEmpties is always an LS.
			D = r.emptyCellCount;
			if ( D > DEGREE_CEILING ) {
				D = DEGREE_CEILING;
			}
			// foreach degree: the number of cells in each ALS
			// d = the current degree
			// c = floor for number of sized cells (one less than d)
			// e = number of maybes in an ALS (one more than d)
			// f = one more than that, to use < instead of <=
			for ( c=1,d=2,e=3,f=4; d<D; c=d,d=e,e=f++ ) {
				// the permutations array
				pa = Idx.IAS_A[d];
				// the alsSet for this degree
				set = sets[d];
				// get region.cells with size 2..d+1; an ALS of d cells can't
				// contain a cell with more than d+1 maybes, so ignore them.
				// NOTE: There is confusion over whether we need >= d or > d
				// cells in a region from which to form an ALS of d cells.
				// The test-cases require >= d but I think it should be > d
				// because all empty cells in a region form a LockedSet, but
				// that makes AlsXzTest fail, and I don't understand why.
				if ( (n=r.sized(f, sized)) > c
				  // if any nakedSets in region then ignore there cells
				  && ( noDirt || (n=clean(dirt, sized, n)) > c ) ) {
					// read maybes of sized, to not derefence repeatedly,
					// because the number of permutations is exponential.
					maybes = Idx.IAS_B[n];
					maybes[0] = sized[0].maybes;
					maybes[1] = sized[1].maybes;
					for ( i=2; i<n; ++i ) {
						maybes[i] = sized[i].maybes;
					}
					// foreach distinct combo of d among n cells in region,
					// which of each wich has 2..d+1 maybes (minus nakedSets)
					for ( int[] perm : new Permutations(n, pa) ) {
						// build m = the aggregate of maybes in 'perm' cells.
						// do the first two cells and tap-out if theres already
						// too many maybes (for speed).
						if ( VSIZE[m=maybes[perm[0]] | maybes[perm[1]]] < f ) {
							// or-in maybes of remainder of degree cells
							for ( i=2; i<d; ++i ) {
								m |= maybes[perm[i]];
							}
							// if there's 1 more maybe than cells then
							if ( VSIZE[m] == e ) {
								// create a new Als and add to sets[size]
								// nb: Plain algorithm produces NO duplicates,
								//     so add doesn't check, so I'm faster.
								set.add(new Als(IdxL.of(sized, perm, d), m, r));
							}
						}
					}
				}
			}
		}
		// add each als to the alss array, and computeFields of each
		for ( d=2; d<DEGREE_CEILING; ++d ) { // degree
			// downside of generics: array is an Object[] so we must cast each
			// Object back to Als before we use it.
			final Object[] objects = (set=sets[d]).array;
			for ( i=0,n=set.size; i<n; ++i ) {
				alss[result] = ((Als)objects[i]).computeFields(grid, result);
				++result;
			}
			// and clear this set (sets is a field for performance)
			set.clear();
		}
		// clean-up: a cell reference holds the whole grid in memory
		Arrays.fill(sized, null);
		Cells.cleanCasA();
		return result;
	}

}
