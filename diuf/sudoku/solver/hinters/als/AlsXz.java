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
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.Validator;
import static diuf.sudoku.solver.hinters.Validator.ALS_VALIDATES;
import static diuf.sudoku.solver.hinters.Validator.isValid;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.utils.Html.colorIn;
import diuf.sudoku.utils.Log;
import java.util.Arrays;

/**
 * AlsXz implements the ALS-XZ (Almost Locked Set XZ) Sudoku solving technique.
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * determine there RCCs (links), and call my "custom" findHints method passing
 * everything down. My findHints method implements the ALS-XZ technique. Each
 * subclass of AAlsHinter implements it's own specific technique.
 * <p>
 * An ALS-XZ is when an Almost Locked Set is linked to a bivalue-cell (XZ) by a
 * common candidate, eliminating the other candidate of the bivalue-cell from
 * external cells which see all occurrences of that value in the ALS-XZ.
 * <p>
 * If the ALS contains both bivalue cell values then the ALS-XZ is a "locked
 * set", so all of the ALS's values can be eliminated from each external cell
 * which sees all occurrences of that value in the ALS-XZ.
 * <pre>
 * 2021-10-19  9,341,298,800  8206  1,138,349  2103  4,441,891
 * </pre>
 *
 * @author Keith Corlett 2020 Apr 26
 */
public final class AlsXz extends AAlsHinter
//implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private static long[] COUNTS = new long[4];

	// weird: LogicalAnalyserTest has different recursive results coz FINDERS
	// put alss in different orders, which changes the whole puzzle-solve-path
	// so LogicalAnalyserTest needs to know the name of the ALS_FINDER class,
	// to build into it's result-file-name, so now there is a seperate test for
	// each subtype of AlsFinder; there may be others in future, because
	// AlsFinderRecursive is still slow!
	public static String alsFinderName() {
		return ALS_FINDER.getClass().getSimpleName();
	}

	// removeable (red) potentials Cell->Values
	private final Pots reds = new Pots();
	// indices of cells to remove from (buds of all z's in both ALSs)
	private final Idx victims = new Idx();

	/**
	 * Constructor.
	 * <pre>Super constructor parameters:
	 * * tech = Tech.ALS_XZ
	 * * allowNakedSets = true my Almost Locked Sets include cells that are
	 *   part of a Locked Set in the region.
	 * * findRCCs = true run getRccs to find the common values connecting ALSs
	 * * allowOverlaps = true the ALSs are allowed to physically overlap
	 * * forwardOnly = true do a forward only search for RCCs
	 * </pre>
	 */
	public AlsXz() {
		super(Tech.ALS_XZ, true, true, true, true);
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
	protected boolean findHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		Rcc rcc; // the current Restricted Common Candidate
		Idx aI, bI; // Idx's of ALS a, ALS b
		int [] za; // VALUESES[zs]
		int zs // bitset of values in ALS a and ALS b except the RC-values
		  , zi,zn,z // za-index, za.length, z-value
		  , i0,i1,i2; // exploded victims.setAnd(a.vBuds[z], b.vBuds[z])
		// ALS a, ALS b
		Als a=null, b=null;
		// bitset of zs (non-rcs) removed by single-link
		int zsZapped = 0;
		// any doubleLinked eliminations (from ~5% of rccs where v2!=0)
		boolean doubleLinked = false;
		// are there any eliminations (either single or double linked)
		boolean ok = false;
		// these are for speed only: stack references to heap fields, because
		// heap references seem to be slower than stack references, even if you
		// can't create a simple struct on the heap in Java (you can in C#) coz
		// in Java EVERYthing is blessed. If you demur then simply press Ctrl-E
		// seven times. You can please some of the people some of the time.
		final Idx victims = this.victims; // indices to eliminate from
		final Pots reds = this.reds; // cell-values to eliminate
		// presume that no hints will be found
		boolean result = false;
		// foreach rcc in rccs
		for ( int i=0; i<numRccs; ++i ) {
			rcc = rccs[i];
			// singleLinked AlsXzs need z-values
			// ~90.75% of rccs are singleLinked
			if ( (zs = alss[rcc.source].maybes
				     & alss[rcc.related].maybes
					 & ~rcc.cands) != 0
			) {
				a = alss[rcc.source];
				b = alss[rcc.related];
				// foreach z-value (non-RC-value common to both ALSs)
				for ( za=VALUESES[zs],zn=za.length,zi=0; zi<zn; ++zi ) {
					// if any external zs see all zs in both ALSs.
					// nb: vBuds excludes own zs, so a&b are all externals.
					if ( ( (i0=(aI=a.vBuds[z=za[zi]]).a0 & (bI=b.vBuds[z]).a0)
						 | (i1=aI.a1 & bI.a1)
						 | (i2=aI.a2 & bI.a2) ) != 0
					) {
						// add the removable (red) potentials
						// fastard: set ok directly here (not singleLinked)
						ok |= reds.upsertAll(victims.set(i0,i1,i2), grid, z);
						// build a bitset of zs eliminated
						zsZapped |= VSHFT[z];
					}
				}
			}
			// doubleLinked AlsXzs can still eliminate without z-values.
			// just ~5.38% of rccs are doubleLinked,
			//  and ~56.75 of them are ONLY doubleLinked
			if ( rcc.v2 != 0 ) {
				a = alss[rcc.source];
				b = alss[rcc.related];
				// the hint needs a switch for AlsXzHintBig.html
				doubleLinked = false;
				// 1. elim xs outside ALS which see all xs in both ALSs.
				// Should victims exclude xs in the other ALS? I think so.
				if ( ( (i0=(aI=a.vBuds[rcc.v1]).a0 & (bI=b.vBuds[rcc.v1]).a0)
					 | (i1=aI.a1 & bI.a1)
					 | (i2=aI.a2 & bI.a2) ) != 0 ) {
					doubleLinked = reds.upsertAll(victims.set(i0,i1,i2), grid, rcc.v1);
				}
				if ( ( (i0=(aI=a.vBuds[rcc.v2]).a0 & (bI=b.vBuds[rcc.v2]).a0)
					 | (i1=aI.a1 & bI.a1)
					 | (i2=aI.a2 & bI.a2) ) != 0 ) {
					doubleLinked |= reds.upsertAll(victims.set(i0,i1,i2), grid, rcc.v2);
				}
				// 2. elim external zs that see all zs in each ALS,
				//    including the other ALS (cannibalism).
				for ( za=VALUESES[a.maybes & ~rcc.cands],zn=za.length,zi=0; zi<zn; ++zi ) {
					if ( ((aI=a.vBuds[z=za[zi]]).a0 | aI.a1 | aI.a2) != 0 ) {
						doubleLinked |= reds.upsertAll(aI, grid, z);
					}
				}
				for ( za=VALUESES[b.maybes & ~rcc.cands],zn=za.length,zi=0; zi<zn; ++zi ) {
					if ( ((bI=b.vBuds[z=za[zi]]).a0 | bI.a1 | bI.a2) != 0 ) {
						doubleLinked |= reds.upsertAll(bI, grid, z);
					}
				}
				// fastard: setting ok here happens MUCH less often than
				// if ( singleLinked || doubleLinked ) would below.
				ok |= doubleLinked;
			}
			if ( ok ) {
				// AlsXz FOUND!
				final AlsXzHint hint = createHint(grid, a, b, zsZapped
						, rcc.v1, rcc.v2, doubleLinked);
				zsZapped = 0;
				doubleLinked = false;
				// is it really ok?
				if ( ALS_VALIDATES & !isValid(grid, hint.reds) ) {
					ok = handle(hint, grid, Arrays.asList(a, b));
				}
				if ( ok ) {
					ok = false;
					result = true;
					if ( accu.add(hint) ) {
						break;
					}
				}
			}
		}
		// GUI only (we don't get here if accu is a SingleHintAccumulator)
		if ( result ) {
			accu.sort(null); // put the highest scoring hint first
		}
		return result;
	}

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
	private AlsXzHint createHint(Grid grid, Als a, Als b, int zsZapped, int v1
			, int v2, boolean doubleLinked) {
		// build highlighted (orange) Pots: vs in each ALS except zs.
		final Pots oranges = new Pots();
		for ( Cell c : a.cells(grid) ) {
			oranges.put(c, c.maybes & ~zsZapped);
		}
		for ( Cell c : b.cells(grid) ) {
			oranges.put(c, c.maybes & ~zsZapped);
		}
		// all removed values should always be red, but orange overwrites red,
		// so remove reds from oranges, to cater for any double-linked elims.
		// What I should do is change the painter, but I'm too scared because
		// I have no idea what it'll do to other hint-types. IIRC orange splats
		// red coz somebody relies on it, but I can't remember who, or why.
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
		// build the hint, and add it to the IAccumulator
		// nb: reds field is copied-and-cleared, so validator uses hint.reds.
		return new AlsXzHint(this, a, v1, v2, b, zsZapped, reds.copyAndClear()
				, doubleLinked);
	}

	// Always report it, then throw or return true to add it
	private boolean handle(AlsXzHint hint, Grid grid, Iterable<Als> alss) {
		Validator.report(tech.name(), grid, alss);
		if ( Run.type == Run.Type.GUI && Run.ASSERTS_ENABLED ) {
			// techies: we're in the GUI in java -ea
			hint.isInvalid = true;
			hint.debugMessage = colorIn("<p><r>"+prevMessage+"</r>");
			return true;
		}
		// else we're in a prod-GUI, or a test-case, or the batch
		throw new UnsolvableException(Log.me()+": bad reds: "+hint.reds);
	}

}
