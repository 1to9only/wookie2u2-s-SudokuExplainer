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
		// see param explanations in the AlsXyChain constructor.
		// NB: I don't call the valid method, so I don't need a LogicalSolver
		// Tech, allowOverlaps, allowLockedSets, findRccs, lockedSets, forwardOnly
		super(Tech.ALS_XZ, true, true, true, true, false);
	}

	/**
	 * Find all ALS-XZ hints in the given Grid: Almost Locked Set XZ, where x
	 * and z refer to values which are common to a pair of ALSs. The x's are
	 * restricted, and z's are not.
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
	 * The logic behind ALS-XZ is quite simple. Because of the RC, at least
	 * one of the ALSs is turned into a Locked Set (we don't know yet which),
	 * and since both ALSs also share digit z, z is "pushed" into the other
	 * ALS, so z will definitely be placed in a cell of one of the two ALSs,
	 * therefore any cell which sees all possible placements of z in both ALSs
	 * (that is not part of either ALS) can not be z.
	 * <p>
	 * KRC edited hobiwan's explanation.
	 *
	 * @param grid the Grid to search
	 * @param candidates An array 1..9 of Idx of indices of grid cells which
	 * maybe this value.
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
	protected boolean findHints(Grid grid, Idx[] candidates, Rcc[] rccs
			, Als[] alss, IAccumulator accu) {
		// local references to constants and fields for speed
		final List<Cell> redCells = this.redCells; // temporary storage
		// my variables (all of them, ansi-C style, for speed)
		final Idx both = this.both; // indices of cells in both ALSs
		final Idx zBuds = this.zBuds; // indices of buds of zAlss
		Als[] twoAlss; // the two ALSs to which each RCC is common
		Als alsA;
		Als alsB; // the two ALSs to which each RCC is common
		Pots redPots; // red (removeable) potentials Cell->Values
		Pots orangePots; // highlighted (orange) potential Cell->Values
		Pots bluePots; // (fins) In this case the Z potential Cell->Values
		AHint hint; // the AlsXzHint (if any)
		int rc1, rc2; // restricted candidate values
		int zMaybes; // remaining common maybes to be the z in ALS-XZ
		int numRcValues; // number of Restricted Common Candidates (1 or 2 or 3?)
		int redMaybes; // a bitset of the RCC-values removed by single-link
		int bits; // a general purpose bitset
		String debugMessage; // appears below hint in the GUI

		// presume that no hints will be found
		boolean result = false;
		// presume that no Double Linked ALS-XZs will be found
		anyDoubleLinked = false;

		// the Restricted Common Candidate/s (rccs) were precalculated.
		// there is 1 hint possible per RCC.
		for ( Rcc rcc : rccs ) {

			// unpack the two AlmostLockedSets (ALSs) from this RCC.
			twoAlss = new Als[] {
				  alsA = alss[rcc.getAls1()]
				, alsB = alss[rcc.getAls2()]
			};

			// unpack the RC values from the RCC
			rc1 = rcc.getCand1();
			rc2 = rcc.getCand2();
			// get the z values: the non-RC values common to both ALSs.
			zMaybes = alsA.maybes & alsB.maybes & ~VSHFT[rc1] & ~VSHFT[rc2];
			// get the number of RC values (ie is this RCC Double Linked)
			numRcValues = rc2==0 ? 1 : 2;
			// "Anything to eliminate on" is a bit tricky. The rules are:
			// 1. single-linked XZs require a z value, but
			// 2. double-linked XZs do not (RC values might be eliminated)
			// 3. a double-linked XZ may still have z value/s.
			if ( zMaybes==0 && numRcValues==1 )
				continue; // single-linked XZ has no z

			// clear the hint-cumulator fields
			redMaybes = 0;
			redPots = null;

			if ( zMaybes != 0 ) {
				// look for single-linked eliminations on the z values
				// get indices of all cells in both ALSs
				both.setOr(alsA.idx, alsB.idx);
				// foreach z: the non-RC values common to both ALSs
				for ( int z : VALUESES[zMaybes] ) { // examine 1 or 2 z's
					// rip z from non-ALS cells that see all z's in both ALSs.
					// get zBuds: buds of z's in both ALSs, which maybe z
					// themselves, except those in both ALSs. Skip if none.
					if ( zBuds.setAnd(alsA.vBuds[z], alsB.vBuds[z])
							.andNot(both).none() )
						continue; // there are no zBuds
					// now prefabricate the parts of the hint which require z.
					// redCells = non-ALS cells which see all z's in both ALSs.
					zBuds.cells(grid, redCells);
					if ( redPots == null )
						redPots = new Pots(redCells, z);
					else
						redPots.upsertAll(redCells, z);
					// then build the removeable (red) potential values.
					redMaybes |= VSHFT[z];
				}
				// single-linked RCCs can skip here (doubles must wait)
				if ( redPots==null && numRcValues==1 )
					continue; // this RCC has no eliminations
			}

			// look for double-linked eliminations (if RCC is double-linked)
			if ( numRcValues > 1
			  && (redPots=addDoubleLinkedElims(grid, candidates, rcc, alss
						, redPots)) == null ) {
				redCells.clear();
				continue; // this RCC has no eliminations
			}
			assert redPots != null; // suppress Nutbeans possible NPE warning
			
			// check to avoid DEAD_CAT: does any elimination actually exist?
			// NOTE: Generator doesn't do DEAD_CAT. Maybe it should. Sigh.
			if ( !redPots.anyCurrent() )
				continue; // dead cat

			//
			// ALS-XZ found! create the hint and add it to accu
			//

			// build the highlighted (orange) potential values: the values in
			// both ALSs except the z/s.
			orangePots = new Pots();
			for ( Als als : twoAlss )
				for ( Cell cell : als.cells(grid) ) {
					bits = cell.maybes.bits & ~redMaybes;
					orangePots.put(cell, new Values(bits, true));
				}
			// if the cell-value is removed it must ALWAYS be displayed in RED,
			// but orange overwrites RED, hence we remove reds from oranges.
			orangePots.removeAll(redPots);
			// build the fin (blue) potential values: z values in both ALSs.
			// The blues help the user see candidates which effect the elims.
			// The blue values are cells in the oranges with a red-value. We
			// get the gonners from redPots because redMaybes may be 0 meaning
			// that there is no Z value, ie all eliminations are X values from
			// double-linked ALSs. This way we need not care where they come
			// from: if value is eliminated anywhere then it's feeling blue.
			bluePots = orangePots.withBits(redPots.valuesOf());
			// The only elimination is a blue, so remove the redPots from the
			// bluePots so that they appear RED.
			// .1.4.7.....785.9....31...74.2..41..775.962...1...78.2..762........7..236...6.57.8
			// 2689,,25,,239,,358,68,235,246,46,,,,36,,16,123,25689,89,,,29,69,58,,,36,,89,35,,,568,589,,,,48,,,,1348,148,13,,36,49,35,,,46,,59,38,,,,389,349,145,1459,159,59,489,15,,18,49,,,,234,349,12,,13,,,49,
			// 50#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
			bluePots.removeAll(redPots);
// changed the order of SudokuGridPanel.POTS_COLORS to paint blue last!
//			// if a candidate is orange AND blue it comes out orange (sigh).
//			orangePots.removeAll(bluePots);
			// build and return the hint
//			debugMessage = MyStrings.isNullOrEmpty(invalidity) ? "" : "<br><h2>"+invalidity+"</h2>";
			debugMessage = "";
			hint = new AlsXzHint(
				  this, alsA, alsB, redMaybes
				, orangePots, bluePots, redPots
				, anyDoubleLinked // this field is set by the add method
				, ""+rc1+(rc2==0?"":", "+rc2)
				, Frmt.and(alsA.cells(grid))
				, Frmt.and(alsB.cells(grid))
				, debugMessage
			);
			redCells.clear();
			result = true;
			if ( accu.add(hint) )
				return true; // we're using a SingleHintAccumulator (in batch)
		}
		// in GUI (you can't get here if we're using a SingleHintAccumulator)
		if ( result )
			accu.sort(); // the highest scoring hint comes first
		return result;
	}
	private final List<Cell> redCells = new LinkedList<>(); // cells with redValues
	private final Idx both = new Idx(); // indices of cells in both ALSs
	private final Idx zBuds = new Idx(); // indices of buds of zAlss

	/**
	 * Adds any extra eliminations for doubly (or more) linked RCCs.
	 * <p>
	 * This method should be called once, it does both A->B, and B->A.
	 * <p>
	 * NOTE: There are no double-linked ALS-XZ's in top1465, ie my standard
	 * test-cases are deficient. Sigh.
	 * <p>
	 * <b>Doubly Linked ALS-XZ</b>
	 * <p>
	 * If the two ALS have two RCCs, things get really interesting. Remember
	 * that an RCC digit can only be placed in one ALS, thus turning the other
	 * ALS into a locked set. If we have two RCCs, one of them has to be placed
	 * in ALS A, turning ALS B into a locked set, and the other must be in
	 * ALS B, turning ALS A into a locked set (which RCC will be in which ALS,
	 * is as yet unknown). Both RCCs in one ALS is impossible, coz both RCCs
	 * would be eliminated from the other ALS leaving only N-1 candidates for
	 * N cells, which is invalid.
	 * <p>
	 * What can be concluded from a Doubly Linked ALS-XZ? Both RCCs are locked
	 * into opposing ALSs, so the RCCs can be eliminated from all non ALS cells
	 * in the houses providing the RCCs. But more importantly, all non RCC
	 * digits get locked within their respective ALS, eliminating all digits
	 * outside the ALS which can see all instances of the digit in the ALS.
	 * The elimination can even be done in a cell belonging to the other ALS,
	 * so the ALS-XZ becomes cannibalistic (sharks!).
	 * <p>
	 * KRC rephrased hobiwans explanation:
	 * see: http://hodoku.sourceforge.net/en/tech_als.php
	 *
	 * @param grid The Grid to search
	 * @param candidates the indices in Grid.cells of each potential value.
	 * @param rcc The RCC that we're processing.
	 * @param reds Pots to add any extra eliminations to
	 */
	private Pots addDoubleLinkedElims(Grid grid, Idx[] candidates
			, Rcc rcc, Als[] alss, Pots reds) {

		final Idx[] buds = Grid.BUDDIES;
		// nb: rc2 is not 0, else we wouldn't be here!
		final Idx both = this.both2;
		final Idx others = this.others;
		// v-for-value vars hold x and then later z value related s__t
		final Idx vAls = this.vAls;
		final Idx vOthers = this.vOthers;

		final Als[] twoAlss = this.twoAlss;
		twoAlss[0] = alss[rcc.getAls1()];
		twoAlss[1] = alss[rcc.getAls2()];

		final int[] rcValues = this.rcValues;
		rcValues[0] = rcc.getCand1();
		rcValues[1] = rcc.getCand2();
		final int bothRcsBits = VSHFT[rcValues[0]] | VSHFT[rcValues[1]];

		// get the indices of the cells in both ALSs
		both.setOr(twoAlss[0].idx, twoAlss[1].idx);
		// both RCs are locked into one of the ALSs, so the RCs can be
		// eliminated from all non-ALS cells (others).
		others.setAllExcept(both);
		// foreach RC value (ie the x in ALS-XZ)
		for ( int x : rcValues ) {
			// get all the x's in both ALSs
			vAls.setAnd(both, candidates[x]);
			// get ALL the x's except those in the ALSs
			vOthers.setAnd(others, candidates[x]);
			// remove those which do NOT see all x's in both the ALSs
			// does buds[xo] contain all x's in both ALSs?
			vOthers.forEach1((xo) -> {
				if ( !Idx.andEqualsS2(buds[xo], vAls) )
					vOthers.remove(xo);
			});
			// we can strip x from any remaining vOthers
			if ( vOthers.any() )
				reds = add(reds, grid, vOthers, x);
		}

		// All non-RC values get locked within their ALS, to eliminate all
		// instances outside the ALS which can see all instances of the value
		// in the ALS. The elimination can even be done in a cell belonging to
		// the other ALS, so the ALS-XZ becomes shark.
		// foreach of the two ALSs
		for ( Als als : twoAlss ) {
			// foreach non-RC value of this ALS (ie the z in ALS-XZ)
			for ( int z : VALUESES[als.maybes & ~bothRcsBits] ) {
				// set vAls just to save repeatedly dereferencing the als
				// and repeated vs-array look-ups, and to keep code same.
				vAls.set(als.vs[z]);
				// get z's outside the ALS (including in other ALS)
				vOthers.set(candidates[z]).andNot(als.idx);
				// remove those which do NOT see all z's in this ALS
				vOthers.forEach1((zo) -> {
					if ( !Idx.andEqualsS2(buds[zo], vAls) )
						vOthers.remove(zo);
				});
				// we can strip z from any remaining vOthers
				if ( vOthers.any() )
					reds = add(reds, grid, vOthers, z);
			}
		}
		return reds;
	}
	private final Als[] twoAlss = new Als[2];
	private final int[] rcValues = new int[2];
	private final Idx both2 = new Idx();
	private final Idx others = new Idx();
	private final Idx vAls = new Idx();
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
