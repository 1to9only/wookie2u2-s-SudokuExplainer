/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is inpired by hobiwan's HoDoKu. I didn't copy his code, just
 * pinched all of his ideas. Kudos to hobiwan. Mistakes are mine.
 *
 * Here's hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file was NOT part of HoDuKo, but the ideas where.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * AlsXz implements the Almost Locked Set XZ Sudoku solving technique.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * determine there RCCs (connections), and call my "custom" findHints method
 * passing everything into my implementation of a specific search technique.
 * <p>
 * An ALS-XZ is when an Almost Locked Set is linked to a bivalue-cell (XZ) by a
 * common candidate, eliminating the other candidate of the bivalue-cell from
 * external cells which see all occurrences of that value in the ALS-XZ.
 * <p>
 * If the ALS contains both bivalue cell values then the ALS-XZ is a "locked
 * set", so all of the ALS's values can be eliminated from each external cell
 * which sees all occurrences of that value in the ALS-XZ.
 *
 * @author Keith Corlett 2020 Apr 26
 */
public final class AlsXz extends AAlsHinter
//implements diuf.sudoku.solver.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("%s: ttlRccs=%,d\n", tech.name(), ttlRccs);
//		diuf.sudoku.utils.Log.teef("%s: alss=%,d rccs=%,d self=%,d\n"
//				, tech.name(), tookAlss, tookRccs, took);
//	}
//	private long ttlRccs;
//	private long took;
//ALS_XZ:    ttlRccs=    8,801,276
//ALS_Wing:  ttlRccs=    6,085,665
//ALS_Chain: ttlRccs=3,100,592,090
//ALS_XZ: alss=902,700 rccs=13,988,600,100 self=303,228,500
//    14,196,723,200	   9791	     1,449,976	   3955	     3,589,563	ALS-XZ

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.ALS_XZ
	 * * allowNakedSets = true my Almost Locked Sets include cells that are
	 *   part of a Locked Set in the region.
	 * * findRCCs = true run getRccs to find the common values connecting ALSs
	 * * allowOverlaps = true the ALSs are allowed to physically overlap
	 * * forwardOnly = true do a forward only search for RCCs
	 * * useStartAndEnd = false I don't need start and end indexes for each ALS
	 * </pre>
	 */
	public AlsXz() {
		super(Tech.ALS_XZ, true, true, true, true, false);
	}

	/**
	 * Find all ALS-XZ hints in the given Grid: Almost Locked Set XZ, where x
	 * and z refer to values that are common to a pair of ALSs. The x's are
	 * restricted, and z's are not, they're just common. sigh.
	 * <p>
	 * This is the simplest ALS technique. We find two ALSs which share a
	 * Restricted Common (RC) value called 'x'. If both ALSs also contain
	 * another (not an RC) common value called 'z', then z may be eliminated
	 * from all non-ALS cells which see ALL instances of z in both ALSs.
	 * <p>
	 * An ALS-XZ is in fact an ALS Chain of length 2. It's processed separately
	 * because it has special rules when the two ALSs share two restricted
	 * common candidates (RC values). This pattern is called a "Double Linked"
	 * ALS-XZ.
	 * <p>
	 * The logic behind ALS-XZ is simple enough: Because the RC-value must be
	 * in one of these two ALSs, one of the ALSs becomes a Locked Set (we don't
	 * know yet which), and these two ALSs also share a z-value, that's pushed
	 * into the other ALS, so z must be in one of the two ALSs, hence external
	 * cells (not in either ALS) seeing all z's in both ALSs can not be z.
	 * <p>
	 * KRC edited hobiwan's explanation.
	 *
	 * @param grid the Grid to search
	 * @param candidates array of Idx of grid cells which maybe each value 1..9.<br>
	 *  NO LONGER USED in this implementation, but not removed coz it may still
	 *  be used in the other sub-classes of AAlsHinter
	 * @param alss array of Almost Locked Sets (ALSs)
	 * @param numAlss number of ALSs in the alss array
	 * @param rccs array of Restricted Common Candidates (RCCs) which is the
	 * intersection of two Almost Locked Sets (ALSs) on one-or-more Restricted
	 * Common (RC) value. The restriction is that all instances of the RC value
	 * in both ALSs see (same box, row, or col) each other; and an RC value is
	 * NOT in the physical overlap (if any) of the two ALSs
	 * @param numRccs number of RCCs in the rccs array
	 * @param accu the IAccumulator to which we add hints
	 * @return true if any hints were found, else false. As always, if the
	 * IAccumulators add method returns true then I return true (ie exit-early
	 * when the first hint is found)
	 */
	@Override
	protected boolean findHints(final Grid grid, final Idx[] candidates
			, final Als[] alss, final int numAlss, final Rcc[] rccs
			, final int numRccs, final IAccumulator accu) {
		Rcc rcc; // the current Restricted Common Candidate
		Als a, b; // the two ALSs to which each RCC-value is common
		Idx ai, bi; // Idx's in ALS a and b
		AHint hint; // the AlsXzHint (if any)
		int v1, v2 // restricted candidate values
		  , zs // bitset of values common to both ALSs except the RC-values
		  , zsZapped // bitset of z-values (non-RCs) removed by single-link
		  , bt0,bt1,bt2 // both.setOr(a.idx, b.idx)
		  , vt0,vt1,vt2; // victims.setAnd(a.vBuds[z], b.vBuds[z]).andNot(both)
		boolean sngLnkd // singleLinked: are there any z-values
			  , dblLnkd; // doubleLinked: rcc.cand2!=0, then any double-elims
//		final long start = System.nanoTime();
//		final Idx both = this.both; // cells in both ALSs
		final Idx victims = AlsXz.VICTIMS; // buds of all z's in both ALSs
		// presume that no hints will be found
		boolean result = false;
//		ttlRccs += rccs.length;
		// NOTE: avoid continue because its slow.
		for ( int i=0; i<numRccs; ++i )
			// singleLinked ALS-XZs need z-values;
			// doubleLinked ALS-XZs can still eliminate even without z-values.
			if ( ( sngLnkd = (zs = alss[(rcc=rccs[i]).als1].maybes
								 & alss[rcc.als2].maybes
								 & ~VSHFT[rcc.v1]
								 & ~VSHFT[rcc.v2]) != 0 )
			   | ( dblLnkd = rcc.v2 != 0 ) ) {
				a = alss[rcc.als1];
				b = alss[rcc.als2];
				v1 = rcc.v1;
				v2 = rcc.v2;
				zsZapped = 0;
				if ( sngLnkd ) {
					// look to eliminate each z-value
					// get indices of all cells in both ALSs
// inline for speed: eliminate method calls
//					both.setOr(a.idx, b.idx);
					bt0 = (ai=a.idx).a0 | (bi=b.idx).a0;
					bt1 = ai.a1 | bi.a1;
					bt2 = ai.a2 | bi.a2;
					// foreach z: a non-RC value common to both ALSs
					for ( int z : VALUESES[zs] ) { // examine 1 or 2 z's
						// if any external cell/s see all z's in both ALSs.
// inline for speed: eliminate method calls
//						if ( victims.setAnd(a.vBuds[z], b.vBuds[z])
//								.andNot(both).any() ) {
						if ( ( (vt0=(ai=a.vBuds[z]).a0 & (bi=b.vBuds[z]).a0 & ~bt0)
							 | (vt1=ai.a1 & bi.a1 & ~bt1)
							 | (vt2=ai.a2 & bi.a2 & ~bt2) ) != 0 ) {
							// add the removable (red) potentials
							REDS.upsertAll(victims.set(vt0,vt1,vt2), grid, z);
							// build-up bitset of z-values zapped by singleLink
							zsZapped |= VSHFT[z];
						}
					}
				}
				if ( dblLnkd ) {
					dblLnkd = false;
					// 1. eliminate x's outside the ALS which see all x's in both ALSs
					if ( VICTIMS.setAndAny(a.vBuds[v1], b.vBuds[v1]) )
						dblLnkd = REDS.upsertAll(VICTIMS, grid, v1);
					if ( VICTIMS.setAndAny(a.vBuds[v2], b.vBuds[v2]) )
						dblLnkd |= REDS.upsertAll(VICTIMS, grid, v2);
					// 2. eliminate all z's outside the ALS that see all z's in the ALS
					for ( int z : VALUESES[a.maybes & ~VSHFT[v1] & ~VSHFT[v2]] )
						if ( a.vBuds[z].any() )
							dblLnkd |= REDS.upsertAll(a.vBuds[z], grid, z);
					for ( int z : VALUESES[b.maybes & ~VSHFT[v1] & ~VSHFT[v2]] )
						if ( b.vBuds[z].any() )
							dblLnkd |= REDS.upsertAll(b.vBuds[z], grid, z);
				}
				if ( !REDS.isEmpty() ) {
					// FOUND ALS-XZ! create the hint and add it to accu
					hint = createHint(grid, a, b, zsZapped, v1, v2, dblLnkd);
					result = true;
					if ( accu.add(hint) )
						break;
					dblLnkd = false;
				}
			}
		// GUI only (we don't get here if accu is a SingleHintAccumulator)
		if ( result )
			accu.sort(); // put the highest scoring hint first
//		took += System.nanoTime() - start;
		return result;
	}
	private static final Pots REDS = new Pots(); // removeable (red) potentials Cell->Values
//	private final Idx both = new Idx(); // indices of cells in both ALSs
	private static final Idx VICTIMS = new Idx(); // indices of buds of zAlss

	/**
	 * Create a new AlsXzHint.
	 *
	 * @param grid the grid
	 * @param a first ALS
	 * @param b second ALS
	 * @param zsZapped bitset of z-values (non-RCs) removed by single-link
	 * @param reds removable (red) potentials
	 * @param v1 first RC value
	 * @param v2 second RC value; 0 for none
	 * @return
	 */
	private AHint createHint(Grid grid, Als a, Als b, int zsZapped, int v1
			, int v2, boolean anyDoubleLinked) {
		// build highlighted (orange): RCC's = values in both ALSs except z's.
		final Pots oranges = new Pots();
		for ( Cell c : a.cells(grid) )
			oranges.put(c, new Values(c.maybes.bits & ~zsZapped, F));
		for ( Cell c : b.cells(grid) )
			oranges.put(c, new Values(c.maybes.bits & ~zsZapped, F));
		// ALL removed values should ALWAYS be RED, but orange overwrites red,
		// so remove reds from oranges, to cater for any double-linked elims.
		oranges.removeAll(REDS);
		// build the fin (blue) pots: ALL values removed from both ALSs.
		// Blues are eliminated-values in the orange-cells; to show candidates
		// which caused these eliminations. We get the values from redPots coz
		// zsZapped may be 0 meaning that there is no Z value, ie all elims are
		// X values from double-linked ALSs. This way we don't care where they
		// are from: just all eliminated-values are painted blue.
		final Pots blues = oranges.withBits(REDS.valuesOf());
		// remove any eliminations from the blues so that they appear RED.
		blues.removeAll(REDS);
		// build a string of the RCC-value/s
		String rccs = Integer.toString(v1);
		if ( v2 != 0 )
			rccs += " and " + v2;
		// debugMessage
		final String tag = "";
		// build the hint, and add it to the IAccumulator
		return new AlsXzHint(this, a, b, zsZapped, REDS.copyAndClear()
				, anyDoubleLinked, rccs, tag);
	}

}
