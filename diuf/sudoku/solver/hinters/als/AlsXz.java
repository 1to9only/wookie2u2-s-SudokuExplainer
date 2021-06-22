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

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.ALS_XZ
	 * * allowLockedSets = true my Almost Locked Sets include cells that are
	 *   part of a Locked Set in the region.
	 * * findRCCs = true run getRccs to find values connecting ALSs
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
	 * because it has special rules when the two ALSs share two (or more) RC
	 * values. This pattern is called "Double Linked" ALSs.
	 * <p>
	 * The logic behind ALS-XZ is simple enough: Because of the RC-value, one
	 * of the ALSs is turned into a Locked Set (we don't know yet which), and
	 * these two ALSs also share the z-value. Z is pushed into the other ALS,
	 * so z must be in one of the two ALSs, hence any cell (not in either ALS)
	 * which sees all z's in both ALSs can not be z.
	 * <p>
	 * KRC edited hobiwan's explanation.
	 *
	 * @param grid the Grid to search
	 * @param vs array of Idx of grid cells which maybe each value 1..9.<br>
	 *  NO LONGER USED in this implementation, but not removed coz it may still
	 *  be used in the other sub-classes of AAlsHinter
	 * @param rccs The list of Restricted Common Candidates (RCCs) which is the
	 * intersection of two Almost Locked Sets (ALSs) on one-or-more Restricted
	 * Common (RC) value. The restriction is that all instances of the RC value
	 * in both ALSs see (same box, row, or col) each other; and an RC value is
	 * NOT in the physical overlap (if any) of the two ALSs
	 * @param alss the list of Almost Locked Sets (ALSs)
	 * @param accu the IAccumulator to which we add hints
	 * @return true if any hints were found, else false. As always, if the
	 * IAccumulators add method returns true then I return true (ie exit-early
	 * when the first hint is found)
	 */
	@Override
	protected boolean findHints(Grid grid, Idx[] vs, Rcc[] rccs, Als[] alss
			, IAccumulator accu) {
		Als a, b; // the two ALSs to which each RCC is common
		Pots reds; // removeable (red) potentials Cell->Values
		AHint hint; // the AlsXzHint (if any)
		int v1, v2 // restricted candidate values
		  , zs // bitset of values common to both ALSs except the RC-values
		  , zsZapped; // bitset of z-values (non-RCs) removed by single-link
//		final Idx both = this.both; // cells in both ALSs
//		final Idx victims = this.victims; // buds of all z's in both ALSs

		// presume that no double-linked ALS-XZ eliminations will be found
		// NOTE: anyDoubleLinked is set to true down in the add helper method,
		// which is called ONLY by the addDoubleLinkedElims method. Sorry for
		// the trickyness.
		anyDoubleLinked = false;

		// presume that no hints will be found
		boolean result = false;

//		ttlRccs += rccs.length;

		// The rccs array was calculated by {@link AAlsHinter#findHints} before
		// he called me. There is 1 hint possible per RCC.
		// NOTE: avoid continue because its slow.
		for ( Rcc rcc : rccs ) {
			// unpack the two ALSs (Almost Locked Sets) from this RCC.
			a = alss[rcc.als1];
			b = alss[rcc.als2];
			// unpack the RC values from the RCC
			v1 = rcc.cand1;
			v2 = rcc.cand2;
			// get the z's: the non-RC values common to both ALSs.
			zs = a.maybes & b.maybes & ~VSHFT[v1] & ~VSHFT[v2];
			// if we have something to eliminate on:
			// single-linked ALS-XZs need zs;
			// double-linked ALS-XZs can still eliminate even without zs.
			if ( zs!=0 || v2!=0 ) {
				// clear the accumulated fields
				zsZapped = 0; // bitset of z-values removed by single-link
				reds = null; // null until we find something to eliminate
				// if this ALS-XZ is single-linked
				if ( zs != 0 ) {
					// look to eliminate each z-value
					// get indices of all cells in both ALSs
					both.setOr(a.idx, b.idx);
					// foreach z: a non-RC value common to both ALSs
					for ( int z : VALUESES[zs] ) { // examine 1 or 2 z's
						// if any external cell/s see all z's in both ALSs.
						if ( victims.setAnd(a.vBuds[z], b.vBuds[z])
								.andNot(both).any() ) {
							// create/add the removable (red) Cell=>Values
							if ( reds == null )
								reds = new Pots();
							reds.upsertAll(victims, grid, z);
							// build-up bitset of z-values zapped by singleLink
							zsZapped |= VSHFT[z];
						}
					}
				}
				// if RCC is double-linked then look for double-linked elims
				if ( v2 != 0 )
					reds = addDoubleLinkedElims(grid, a, b, v1, v2, reds);
				// ignore this RCC if it has no eliminations.
				if ( reds != null
				  // this crap is slow but works: no more no-elimination hints
				  && reds.clean() ) {
					// FOUND ALS-XZ! create the hint and add it to accu
					hint = createHint(grid, a, b, zsZapped, reds, v1, v2);
					result = true;
					if ( accu.add(hint) )
						return result; // exit-early in batch
				}
			}
		}
		// GUI only (we don't get here if accu is a SingleHintAccumulator)
		if ( result )
			accu.sort(); // put the highest scoring hint first
		return result;
	}
	private boolean anyDoubleLinked; // is any double-linked eliminations
	private final Idx both = new Idx(); // indices of cells in both ALSs
	private final Idx victims = new Idx(); // indices of buds of zAlss

	/**
	 * Adds additional eliminations for double-linked ALSs, ie when RCC.cand2
	 * is not 0.
	 * <p>
	 * This method should be called once, it does both A->B, and B->A.
	 * <p>
	 * hobiwan explains at: http://hodoku.sourceforge.net/en/tech_als.php
	 * <p>
	 * <u>Double-Linked ALS-XZ</u>
	 * <p>
	 * If the two ALSs have two RC values then things get really interesting.
	 * Recall that an RC value can only be placed in one ALS, thus turning the
	 * other ALS into a locked set. If we have two RCs, one of them has to be
	 * placed in ALS A turning ALS B into a locked set, and the other must be
	 * in ALS B turning ALS A into a locked set; which RC is in which ALS is
	 * as yet unknown. Both RC values in one ALS is impossible, coz both RCs
	 * would be eliminated from the other ALS leaving only N-1 candidates to
	 * fill N cells, which is invalid.
	 * <p>
	 * So what do double-linked ALS-XZ's eliminate? Both RCs are locked into
	 * opposing ALSs, so (as per normal) the RCs can be eliminated from non-ALS
	 * cells; but more importantly, each non-RC value (v) is locked into its
	 * ALS, eliminating v from cells outside this ALS (including the other ALS)
	 * which see all v's in this ALS.
	 *
	 * @param grid the Grid to search
	 * @param a first ALS
	 * @param b second ALS
	 * @param v1 first RC value
	 * @param v2 second RC value; never 0 (or it's not double-linked)
	 * @param reds Pots to which I add any double-linked-eliminations; if null
	 *  passed then a new Pots is created and returned if there's any DL-elims.
	 * @return reds if any elims, else null
	 */
	private Pots addDoubleLinkedElims(final Grid grid, final Als a, final Als b
			, final int v1, final int v2, Pots reds) {
		// 1. eliminate x's outside the ALS which see all x's in both ALSs
		if ( victims.setAndAny(a.vBuds[v1], b.vBuds[v1]) )
			reds = elim(reds, grid, victims, v1);
		if ( victims.setAndAny(a.vBuds[v2], b.vBuds[v2]) )
			reds = elim(reds, grid, victims, v2);
		// 2. eliminate all z's outside the ALS that see all z's in the ALS
		final int bothRcs = VSHFT[v1] | VSHFT[v2];
		for ( int z : VALUESES[a.maybes & ~bothRcs] )
			if ( a.vBuds[z].any() )
				reds = elim(reds, grid, a.vBuds[z], z);
		for ( int z : VALUESES[b.maybes & ~bothRcs] )
			if ( b.vBuds[z].any() )
				reds = elim(reds, grid, b.vBuds[z], z);
		return reds;
	}

	// Called ONLY by addDoubleLinkedElims to actually add double-linked elims
	// to reds, which is created if it doesn't exist yet, and always returned.
	private Pots elim(Pots reds, Grid grid, Idx idx, int v) {
		if ( reds == null )
			reds = new Pots();
		reds.upsertAll(idx, grid, v);
		anyDoubleLinked = true;
		return reds;
	}

	/**
	 * Create a new AlsXzHint.
	 *
	 * @param grid the grid
	 * @param a first ALS
	 * @param b second ALS
	 * @param zsZapped bitset of z-values (non-RCs) removed by single-link
	 * @param reds removable (red) potentials
	 * @param rc1 first RC value
	 * @param rc2 second RC value; 0 for none
	 * @return
	 */
	private AHint createHint(Grid grid, Als a, Als b, int zsZapped, Pots reds
			, int rc1, int rc2) {
		// build highlighted (orange): RCC's = values in both ALSs except z's.
		final Pots oranges = new Pots();
		for ( Cell c : a.cells(grid) )
			oranges.put(c, new Values(c.maybes.bits & ~zsZapped, F));
		for ( Cell c : b.cells(grid) )
			oranges.put(c, new Values(c.maybes.bits & ~zsZapped, F));
		// ALL removed values should ALWAYS be RED, but orange overwrites red,
		// so remove reds from oranges, to cater for any double-linked elims.
		oranges.removeAll(reds);
		// build the fin (blue) pots: ALL values removed from both ALSs.
		// Blues are eliminated-values in the orange-cells; to show candidates
		// which caused these eliminations. We get the values from redPots coz
		// zsZapped may be 0 meaning that there is no Z value, ie all elims are
		// X values from double-linked ALSs. This way we don't care where they
		// are from: just all eliminated-values are painted blue.
		final Pots blues = oranges.withBits(reds.valuesOf());
		// remove any eliminations from the blues so that they appear RED.
		blues.removeAll(reds);
		// build a string of the RCC-value/s
		String rccsString = Integer.toString(rc1);
		if ( rc2 != 0 )
			rccsString += " and " + rc2;
		// set the bloody debugMessage
		final String debugMessage = "";
		// build the hint, and add it to the IAccumulator
		return new AlsXzHint(this, a, b, zsZapped, reds, anyDoubleLinked
				, rccsString, debugMessage);
	}

//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("%s: ttlRccs=%,d\n", tech.name(), ttlRccs);
//	}
//	private long ttlRccs;
//ALS_XZ:    ttlRccs=    8,801,276
//ALS_Wing:  ttlRccs=    6,085,665
//ALS_Chain: ttlRccs=3,100,592,090

}
