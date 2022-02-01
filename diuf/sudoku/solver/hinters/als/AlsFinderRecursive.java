//KRC 2021-10-19 class no longer used so commented out. Delete if you like.
//AlsFinderPlain is simpler, and finds ALSs in the "correct" order. Sigh.
//package diuf.sudoku.solver.hinters.als;
//
//import diuf.sudoku.Grid;
//import diuf.sudoku.Grid.ARegion;
//import diuf.sudoku.Grid.Cell;
//import static diuf.sudoku.Grid.LATER_BUDS;
//import diuf.sudoku.Idx;
//import diuf.sudoku.IdxL;
//import diuf.sudoku.Regions;
//import static diuf.sudoku.Values.VSIZE;
//import java.util.Arrays;
//
///**
// * AlsFinderRecursive extends AlsFinder to override getAlss to use the stack
// * recursion technique from BigWing, which appears to be a bit faster.
// * <pre>
// * AlsFinderRecursive finds more hints, but is just a tad slower, sigh.
// * Using AlsFinder 2021-06-11.09-28-58
// *  3,814,813,600  12254    311,311  2210   1,726,160 Big-Wings
// * 12,173,383,300   9867  1,233,747  3962   3,072,534 ALS-XZ
// *  9,830,757,900   7175  1,370,140  3406   2,886,305 ALS-Wing
// *  7,295,875,000   4435  1,645,067   567  12,867,504 ALS-Chain
// * Using AlsFinderRecursive 2021-06-11.20-57-40
// *  3,902,282,300  12096    322,609  2235   1,745,987 Big-Wings
// * 10,549,029,000   9697  1,087,865  3222   3,274,062 ALS-XZ
// *  5,199,827,200   7556    688,171  2801   1,856,418 ALS-Wing
// *  7,881,250,700   5350  1,473,130   347  22,712,538 ALS-Chain
// * Again 2021-06-11.21-21-13
// *  3,883,998,700  12096    321,097  2235   1,737,807 Big-Wings
// * 10,666,916,700   9697  1,100,022  3222   3,310,650 ALS-XZ
// *  5,346,636,300   7556    707,601  2801   1,908,831 ALS-Wing
// *  7,906,640,300   5350  1,477,876   347  22,785,706 ALS-Chain
// * Again 2021-06-11.21-27-20
// *  3,897,708,800  12096    322,231  2235   1,743,941 Big-Wings
// * 10,568,998,000   9697  1,089,924  3222   3,280,260 ALS-XZ
// *  5,214,098,100   7556    690,060  2801   1,861,513 ALS-Wing
// *  7,764,569,200   5350  1,451,321   347  22,376,280 ALS-Chain
// * And the whole run takes 2:04, which ain't too shabby.
// *
// * KRC 2021-06-13 Discovered that test-cases for AlsXz, AlsWing, and AlsChain
// * fail when AAlsHinter uses AlsFinderRecursive, and they work as soon as I
// * revert to AlsFinder. I think means that AlsFinder is finding ALS's that
// * AlsFinderRecursive misses, or it could be that differing orders of ALS's
// * breaks logic in the hinters. I'll investigate.
// * This is embarassing: start degree is now 2 (not 3); and pass degree (not i)
// * to ARegions.commonN, so AlsFinderRecursive now finds 2-cell-ALS's.
// * Now Als*Test and LogicalAnalyserTest have separate tests for AlsFinder
// * and AlsFinderRecursive. LogicalAnalyserTest failed coz AlsXz finds different
// * ALS-XZ coz AlsFinderRecursive puts it's alss in a different order, hence
// * LogicalAnalyser's whole hint-chain differs from that point on. sigh.
// *
// * KRC 2021-06-16 Use new DEGREE constant to create the arrays ONCE, and reuse
// * them. So also new Cell.copy method, and new Als constructor which does not
// * copy the given array (package visible). Also cleaned-up memory leak in the
// * common regions array. sigh.
// * </pre>
// *
// * @author Keith Corlett 2021-06-11 based on existing BigWing code
// */
//final class AlsFinderRecursive extends AlsFinder {
//
//	// the largest degree used + 1 (a ceiling): 8
//	// degree is number of cells in ALS.
//	// a region of 9 cells is an LS, not an ALS.
//	// we don't use ALS's of 8 cells, so don't waste time finding them:
//	// * AlsXz/Wing/Chain use 2..7
//	// * BigWings: WXYZ=3, VWXYZ=4, UVWXYZ=5, TUVWXYZ=6, STUWXYZ=7
//	private static final int DEGREE = 8; // was 9, but 8's never used!
//
//	// shorthand
//	private static final boolean F = false;
//
//	// the regions common to the ALS cells
//	private final ARegion[] crs = new ARegion[2]; // 2 is enough
//
//	// Idx of the cells at each level 0..degreeMinus1
//	private final Idx[] x;
//	// the $degree cells in this ALS (Almost Locked Set)
//	private final Cell[] als;
//	// candidates of index+1 cells combined
//	private final int[] cands;
//
//	// ---- set and cleared by findHints ----
//	// the grid to search
//	private Grid grid;
//	private Cell[] cells;
//	private int d, dP1, dP2, dM1;
//	// A Set<Als> which ignores add duplicate
//	private AlsSet set;
//
//	public AlsFinderRecursive() {
//		x = new Idx[DEGREE];
//		for ( int i=0; i<DEGREE; ++i )
//			x[i] = new Idx();
//		als = new Cell[DEGREE];
//		// candidates of index+1 cells combined
//		cands = new int[DEGREE];
//	}
//
//	// find all distinct Almost Locked Sets in this grid
//	@Override
//	int getAlss(final Grid grid, final Als[] alss, final boolean allowNakedSets) {
//		int cnt = 0; // the return value: number of ALS's found
//		try {
//			this.grid = grid;
//			this.cells = grid.cells;
//			if ( !allowNakedSets && setNakedSetIdxs(grid) ) {
//				// ignore duplicate and nakedSet
//				set = new AlsSetNoNakedSets();
//			} else {
//				// ignore duplicate
//				set = new AlsSet();
//			}
//			// find ALS's by degree so returned alss is ordered by size
//			// degree is the number of cells in an ALS we currently seek
//			// DEGREE is the maximum number of cells in an ALS + 1
//			for ( d=2; d<DEGREE; ++d ) {
//				dM1 = d - 1;
//				dP1 = d + 1;
//				dP2 = d + 2;
//				// sets[0] = indices of cells with size 2..degree+1
//				grid.idx((c) -> c.size>1 && c.size<dP2, x[0].clear());
//				// foreach first-cell-in-an-ALS
//				for ( Cell c : x[0].cells(cells) ) {
//					als[0] = c; // the first cell in the almost locked set
//					cands[0] = c.maybes; // the initial cands
//					// check for enough cells (we need degree cells for an ALS)
//					// sets[0] contains only cells with <= degree+1 maybes, so
//					// there's no need to recheck for it here.
//					if ( x[1].setAndMin(x[0], LATER_BUDS[c.i], dM1) ) {
//						// go onto the second ALS cell, and on from there
//						recurse(1);
//					}
//				}
//			}
//			// copy and computeFields
//			for ( Als als : set ) {
////				System.out.format("%d\t%s\n", cnt, als);
//				alss[cnt] = als.computeFields(grid, cnt);
//				++cnt;
//			}
//			// clear the Set, for next-time
//			set.clear();
//		} finally {
//			// forget all grid and cell references (for GC)
//			this.grid = null;
//			Arrays.fill(als, null);
//			crs[0] = crs[1] = null;
//		}
//		return cnt;
//	}
//
//	/**
//	 * Recursively build-up $degree cells in the als array, then check to see
//	 * if it's an ALS (N cells with N+1 maybes between them) and if so add a
//	 * new Als to the alsSet, whose add method ignores repeats, and if it's an
//	 * AlsSetNoNakedSets also ignores an ALS that contains a cell in a NakedSet
//	 * in this region.
//	 * <p>
//	 * Note that recursion is used just put an arbitrary number of cells in the
//	 * als array (there's no fancy permutations search or anything); its just
//	 * the first way I thought of. There may be a faster iterative way that I'm
//	 * unaware of. Performance is the key issue here.
//	 *
//	 * @param i invoked with 1. als[0], cands[0], sets[0] are pre-init'd
//	 * @return any hint/s found?
//	 */
//	private boolean recurse(final int i) {
//		final int h = i - 1;
//		final int j = i + 1;
//		boolean result = false;
//		for ( Cell c : x[i].cellsA(cells) ) {
//			als[i] = c;
//			// if als cells share a common region
//			if ( Regions.anyCommon(als, j) ) {
//				// if the ALS is incomplete (contains less than degree cells)
//				if ( i < dM1 ) {
//					// if existing + this cell together have <= degree+1 maybes
//					if ( VSIZE[cands[i]=cands[h]|c.maybes] < dP2
//					  // need degree cells to form an ALS, this is the (i+1)'th
//					  && x[j].setAndMin(x[i], LATER_BUDS[c.i], dM1-i)
//					) {
//						// move right to als[i+1], to find next cell in ALS
//						result |= recurse(j);
//					}
//				// else the ALS is complete (contains degree cells)
//				// degree cells with degree+1 maybes is an Almost Locked Set
//				} else if ( VSIZE[cands[i]=cands[h]|c.maybes] == dP1 ) {
//					//long start = System.nanoTime();
//					// one Idx even for multiple common regions.
//					// als cells have atleast 1 common region to get here.
//					final IdxL idx = IdxL.of(als, d);
//					// foreach region common to these als cells
//					final int n = Regions.commonN(als, d, crs);
//					for ( int k=0; k<n; ++k ) {
//						result |= set.add(new Als(idx, cands[i], crs[k]));
//					}
//					//took += System.nanoTime() - start; // defined in AlsFinder, reported in BigWings
//				}
//			}
//		}
//		return result;
//	}
//
//}
