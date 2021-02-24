/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is inpired by hobiwan's HoDoKu. I haven't copied his code, but
 * I did pinch his ideas, and read his explanations. Kudos to hobiwan. Mistakes
 * are mine.
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
import diuf.sudoku.utils.Frmt;
import java.util.LinkedList;
import java.util.List;


/**
 * AlsXz implements the ALZ-XZ Sudoku solving technique.
 * <p>
 * An ALS (Almost Locked Set) XZ is the simplest of the ALS techniques,
 * involving a "chain" of just two ALS's. AFAIK, the term XZ just means
 * "two values"... I think because XY is already overloaded in Sudoku.
 *
 * @author Keith Corlett 2020 Apr 26
 */
public final class AlsXz extends AAlsHinter {

	private boolean anyDoubleLinked;

	public AlsXz() {
		// NB: I don't use HintValidator, so I don't need a LogicalSolver.
		// See super param explanations in the AlsXyChain constructor.
		// Tech, allowLockedSets, findRCCs, allowOverlaps, forwardOnly, useStartAndEnd
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
		// removeable (red) potentials Cell->Values
		// highlighted (orange) potential Cell->Values
		// fins (blue) In this case the Z potential Cell->Values
		Pots reds, oranges, blues;
		AHint hint; // the AlsXzHint (if any)
		String debugMessage; // appears below hint in the GUI
		int v1, v2 // restricted candidate values
		  , zMaybes // remaining common maybes to be the z in ALS-XZ
		  , pinkBits; // a bitset of the RC-values removed by single-link
		final List<Cell> redsList = this.redsList; // temporary storage
		final Idx both = this.both; // indices of cells in both ALSs
		final Idx zBuds = this.zBuds; // indices of buds of zAlss

		// presume that no Double Linked ALS-XZs will be found
		// NOTE: anyDoubleLinked is set to true down in the add helper method.
		anyDoubleLinked = false;

		// presume that no hints will be found
		boolean result = false;

		// The Restricted Common Candidates (the rccs array) were calculated by
		// my superclass {@link AAlsHinter#findHints} before he called me.
		// There is 1 hint possible per RCC.
		// NOTE: I avoid continue because it can be VERY slow!
		for ( Rcc rcc : rccs ) {

			// unpack the two ALSs (Almost Locked Sets) from this RCC.
			a = alss[rcc.getAls1()];
			b = alss[rcc.getAls2()];

			// unpack the RC values from the RCC
			v1 = rcc.getCand1();
			v2 = rcc.getCand2();

			// get zMaybes: the non-RC values common to both ALSs.
			zMaybes = a.maybes & b.maybes & ~VSHFT[v1] & ~VSHFT[v2];

			// single-linked ALS-XZs require zMaybes, but double-linked ALS-XZs
			// may still produce eliminations without any "normal" eliminations
			if ( zMaybes!=0 || v2!=0 ) {
				// there's something to possibly eliminate on atleast

				// clear the hint-cumulator fields
				pinkBits = 0; // a bitset of the red (removable) values
				reds = null; // null until there's something to eliminate

				if ( zMaybes != 0 ) {
					// look for single-linked eliminations on each zMaybes
					// get indices of all cells in both ALSs
					both.setOr(a.idx, b.idx);
					// foreach z: the non-RC values common to both ALSs
					for ( int z : VALUESES[zMaybes] ) { // examine 1 or 2 z's
						// rip z off nonALS's that see all z's in both ALSs.
						// get zBuds: buds of z's in both ALSs, which maybe z
						// themselves, except those in both ALSs. Skip if none.
						if ( zBuds.setAnd(a.vBuds[z], b.vBuds[z])
								.andNot(both).any() ) { // z has buddies
							// redsList=nonALSs which see all z's in both ALSs
							zBuds.cells(grid.cells, redsList);
							// z can be eliminated from redsList
							if ( reds == null )
								reds = new Pots(redsList, z);
							else
								reds.upsertAll(redsList, z);
							redsList.clear();
							// build-up the removeable (red) values
							pinkBits |= VSHFT[z];
						}
					}
				}

				// look for double-linked elims (if RCC is double-linked)
				if ( v2 != 0 )
					reds = addDoubleLinkedElims(grid, rcc, alss, reds);

				// ignore this RCC if it has no eliminations.
				if ( reds != null
				  // avert DEAD_CAT: do any eliminations actually exist?
				  // NB: This is a pretty slow crappy solution, but it works.
				  && reds.clean() ) {

					//
					// ALS-XZ found! create the hint and add it to accu
					//

					// build the highlighted (orange) pots: values in both ALSs
					// except the z/s.
					oranges = new Pots();
					for ( Cell c : a.cells(grid) )
						oranges.put(c, new Values(c.maybes.bits & ~pinkBits, F));
					for ( Cell c : b.cells(grid) )
						oranges.put(c, new Values(c.maybes.bits & ~pinkBits, F));
					// ALL removed values should ALWAYS be displayed in RED, but
					// orange overwrites red, so remove the reds from oranges.
					// Need this to cater for any double-bloody-linked elims.
					oranges.removeAll(reds);

					// build the fin (blue) pots: z values in both ALSs.
					// Blues red-values in orange-cells; to show the user the
					// candidates which caused these eliminations.
					// We get the gonners from redPots because redMaybes may be 0
					// meaning that there is no Z value, ie all eliminations are
					// X values from double-linked ALSs. This way we need not care
					// where they come from: if a value is eliminated anywhere then
					// it's feeling a bit blue.
					blues = oranges.withBits(reds.valuesOf());
					// remove reds from blues so that they appear RED.
					blues.removeAll(reds);

					// valuesString
					String values; if(v2==0) values=""+v1; else values=""+v1+" and "+v2;

					// set the bloody debugMessage
					debugMessage = "";

					// build the hint, and add it to the IAccumulator
					hint = new AlsXzHint(
						  this, a, b, pinkBits
						, null, null, reds
						, anyDoubleLinked // field set true by add method
						, values
						, Frmt.and(a.cells(grid))
						, Frmt.and(b.cells(grid))
						, debugMessage
					);
					result = true;
					if ( accu.add(hint) )
						return true; // we're using a SingleHintAccumulator (in batch)
				}
			}
		}
		// in GUI (you can't get here if we're using a SingleHintAccumulator)
		if ( result )
			accu.sort(); // the highest scoring hint comes first
		return result;
	}
	private final List<Cell> redsList = new LinkedList<>(); // cells with redValues
	private final Idx both = new Idx(); // indices of cells in both ALSs
	private final Idx zBuds = new Idx(); // indices of buds of zAlss

	/**
	 * Adds additional eliminations for double-linked ALSs, ie when RCC.cand2
	 * is not 0.
	 * <p>
	 * This method should be called once, it does both A->B, and B->A.
	 * <p>
	 * hobiwans explanation: http://hodoku.sourceforge.net/en/tech_als.php
	 * <p>
	 * <u>Double-Linked ALS-XZ</u>
	 * <p>
	 * If the two ALSs have two RC values then things get really interesting.
	 * Recall that an RC value can only be placed in one ALS, thus turning the
	 * other ALS into a locked set. If we have two RCs, one of them has to be
	 * placed in ALS A turning ALS B into a locked set, and the other must be
	 * in ALS B turning ALS A into a locked set; which RC is in which ALS, is
	 * as yet unknown. Both RC values in one ALS is impossible, coz both RCs
	 * would be eliminated from the other ALS leaving only N-1 candidates to
	 * fill N cells, which is invalid.
	 * <p>
	 * So what do double-linked ALS-XZ's eliminate? Both RCs are locked into
	 * opposing ALSs, so (as per normal) the RCs can be eliminated from non-ALS
	 * cells; but more importantly, each non-RC value (v) is locked into its
	 * ALS, eliminating v from cells outside the ALS (including the other ALS)
	 * which see all v's in the ALS.
	 *
	 * @param grid The Grid to search
	 * @param rcc The RCC that we're processing.
	 * @param reds Pots to add any extra eliminations to
	 * @return reds, if there are any eliminations, else null.<br>
	 *  if the passed-in reds is not null then<br>
	 *  double-linked eliminations are added to the existing Pots;<br>
	 *  else (passed-in reds is null)<br>
	 *  a new Pots is returned if any double-linked eliminations are found.
	 */
	private Pots addDoubleLinkedElims(Grid grid, Rcc rcc, Als[] alss, Pots reds) {
		final Als a=alss[rcc.getAls1()], b=alss[rcc.getAls2()];
		final int rc1=rcc.getCand1(), rc2=rcc.getCand2();
		// 1. eliminate x's outside the ALS which see all x's in both ALSs
		if ( vOthers.setAndAny(a.vBuds[rc1], b.vBuds[rc1]) )
			reds = add(reds, grid, vOthers, rc1);
		if ( vOthers.setAndAny(a.vBuds[rc2], b.vBuds[rc2]) )
			reds = add(reds, grid, vOthers, rc2);
		// 2. eliminate all z's outside the ALS that see all z's in the ALS
		final int bothRcs = VSHFT[rc1] | VSHFT[rc2];
		for ( int z : VALUESES[a.maybes & ~bothRcs] )
			if ( a.vBuds[z].any() )
				reds = add(reds, grid, a.vBuds[z], z);
		for ( int z : VALUESES[b.maybes & ~bothRcs] )
			if ( b.vBuds[z].any() )
				reds = add(reds, grid, b.vBuds[z], z);
		return reds;
	}
	private final Idx vOthers = new Idx();

	private Pots add(Pots reds, Grid grid, Idx set, int v) {
		if ( reds == null )
			reds = new Pots();
		for ( Cell cell : set.cells(grid) )
			reds.upsert(cell, v);
		anyDoubleLinked = true;
		return reds;
	}

}
