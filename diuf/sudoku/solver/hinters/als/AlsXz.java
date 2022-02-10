/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.solver.hinters.Validator.reportRedPots;
import static diuf.sudoku.utils.Html.colorIn;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import static diuf.sudoku.solver.hinters.Validator.VALIDATE_ALS;
import static diuf.sudoku.solver.hinters.Validator.validOffs;

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
 * KRC 2021-10-19
 * NOW: 9,341,298,800  8206  1,138,349  2103  4,441,891 ALS_XZ
 *
 * KRC 2022-01-10 J17 is about 30% slower than J8. Trying to speed-up.
 * PRE: 8,991,515,000  7908  1,137,015  2013  4,466,723 ALS_XZ
 * PST: 8,766,519,300  7908  1,108,563  2013  4,354,952 ALS_XZ
 *
 * KRC 2022-01-13 with inline Idx ops in Als.computeFields
 * NOW: 9,053,167,500  7908  1,144,811  2013  4,497,350 ALS_XZ
 * And holly snappin duckshit Batman, it's actually SLOWER! but it can't be.
 * My research shows it spend most time in Als.computFields, and I know (or
 * think I know) from experience that doing Idx ops inline is faster, just for
 * not creating the extra stackFrames, especially when we operate in ONE large
 * complex stackFrame. So I will officially be ____ed because I do not know
 * nuffin. Basically I waffle rather than accept the evidence. I have a choice.
 * Either leave it until tomorrow morning to get a cool run, or accept that
 * inline is in fact SLOWER and revert, ergo take my medicine.
 * SLW: 9,270,523,900  7908  1,172,296  2013  4,605,327 ALS_XZ
 * FST: 9,200,267,800  7908  1,163,412  2013  4,570,426 ALS_XZ
 * </pre>
 *
 * @author Keith Corlett 2020 Apr 26
 */
public final class AlsXz extends AAlsHinter
//implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+Arrays.toString(COUNTS));
//		diuf.sudoku.utils.Log.teeln(tech.name()+": min="+minGoodNumRccs+"/"+minNumRccs+" max="+maxGoodNumRccs+"/"+maxNumRccs);
//		diuf.sudoku.utils.Log.teeln(tech.name()+": RccFinderAbstract.COUNTS="+Arrays.toString(RccFinderAbstract.COUNTS));
//	}
//	private final long[] COUNTS = new long[12];
//	private int minNumRccs=Integer.MAX_VALUE, maxNumRccs=0
//		  , minGoodNumRccs=Integer.MAX_VALUE, maxGoodNumRccs=0;

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
	 * * findRccs = true run getRccs to find the common values connecting ALSs
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
	protected boolean findAlsHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
//		minNumRccs = Math.min(minNumRccs, numRccs);
//		maxNumRccs = Math.max(maxNumRccs, numRccs);
		// HACK: specific to top1465, which may miss hints in other puzzles.
		if ( numRccs > 3751 ) // ALS_XZ: min=171/171 max=3751/3996
			return false;
		Rcc rcc; // the current Restricted Common Candidate
		Idx aI, bI; // Idx's of ALS a, ALS b
		int[] za; // VALUESES[zs]
		int zs // bitset of values in ALS a and ALS b except the RC-values
		  , zi,zn,z // za-index, za.length, z-value
		  , i0,i1,i2; // exploded victims.setAnd(a.vBuds[z], b.vBuds[z])
		Als a=null, b=null; // ALS a, ALS b
		int zsZapped = 0; // bitset of zs (non-rcs) removed by single-link
		boolean doubleLinked = false // any doubleLinked elims
			  , anyElims = false // any elims (single or double linked)
			  , result = false; // presume that no hints will be found
		// foreach rcc in rccs
		for ( int i=0; i<numRccs; ++i ) {
			// singleLinked AlsXzs need zValues
			// zValues = maybes common to both ALSs except the RCC value/s,
			// ergo any "extra" common values.
			// nb: ~90.75% of rccs are singleLinked
			if ( ( zs = alss[(rcc=rccs[i]).source].maybes
				      & alss[rcc.related].maybes
					  & ~rcc.cands ) != 0
			) {
				// these ALSs are single linked, but any elims?
				// foreach z-value (non-RC-value common to both ALSs)
				for ( a=alss[rcc.source],b=alss[rcc.related],za=VALUESES[zs]
						,zn=za.length,zi=0; zi<zn; ++zi )
					// if any external zs see all zs in both ALSs.
					// nb: vBuds excludes own zs, so a&b are all externals.
					if ( ( (i0=(aI=a.vBuds[z=za[zi]]).a0 & (bI=b.vBuds[z]).a0)
						 | (i1=aI.a1 & bI.a1)
						 | (i2=aI.a2 & bI.a2) ) != 0
					) {
						// add the removable (red) potentials
						// fastard: set anyElims here (ie anySingleLinkedElims)
						anyElims |= reds.upsertAll(victims.set(i0,i1,i2), grid, z);
						// build a bitset of zs eliminated
						zsZapped |= VSHFT[z];
					}
			}
			// doubleLinked AlsXzs can still eliminate without z-values.
			// just ~5.38% of rccs are doubleLinked,
			// and ~56.75 of them are ONLY doubleLinked
			if ( rcc.v2 != 0 ) {
				// these ALSs are double linked, so any (possibly extra) elims?
				// nb: doubleLinked might be better called anyDoubleLinkedElims
				//     but TLDR: Too Long Didnot Read.
				a = alss[rcc.source];
				b = alss[rcc.related];
				// 1. elim external v1/v2 that see all v1/v2 in both ALSs.
				doubleLinked = ( (i0=(aI=a.vBuds[z=rcc.v1]).a0 & (bI=b.vBuds[z]).a0)
							   | (i1=aI.a1 & bI.a1)
							   | (i2=aI.a2 & bI.a2) ) != 0
						&& reds.upsertAll(victims.set(i0,i1,i2), grid, z);
				// nb: I joined this line to above with ||, but it MUCH slower,
				// so performance limits how complex one line of code can be.
				doubleLinked |= ( (i0=(aI=a.vBuds[z=rcc.v2]).a0 & (bI=b.vBuds[z]).a0)
								| (i1=aI.a1 & bI.a1)
							    | (i2=aI.a2 & bI.a2) ) != 0
						&& reds.upsertAll(victims.set(i0,i1,i2), grid, z);
				// 2. elim unused-cands that see all v's in either ALS
				//    including the other ALS (cannibalism).
				for ( za=VALUESES[a.maybes & ~rcc.cands],zn=za.length,zi=0; zi<zn; ++zi )
					if ( ((aI=a.vBuds[z=za[zi]]).a0 | aI.a1 | aI.a2) != 0 )
						doubleLinked |= reds.upsertAll(aI, grid, z);
				for ( za=VALUESES[b.maybes & ~rcc.cands],zn=za.length,zi=0; zi<zn; ++zi )
					if ( ((bI=b.vBuds[z=za[zi]]).a0 | bI.a1 | bI.a2) != 0 )
						doubleLinked |= reds.upsertAll(bI, grid, z);
				// fastard: setting anyElims here happens MUCH less often than
				// if ( singleLinked || doubleLinked ) would below.
				anyElims |= doubleLinked;
			}
			if ( anyElims ) {
//				minGoodNumRccs = Math.min(minGoodNumRccs, numRccs);
//				maxGoodNumRccs = Math.max(maxGoodNumRccs, numRccs);
				// AlsXz FOUND!
				// nb: the hint needs doubleLinked to show AlsXzHintBig.html
				final AlsXzHint hint = new AlsXzHint(this, a, rcc.v1, rcc.v2, b
						, zsZapped, reds.copyAndClear(), doubleLinked);
				// if I validate: is this hint ok?
				if ( VALIDATE_ALS ) {
					if ( !validOffs(grid, hint.reds) )
						anyElims = handle(hint, grid, Arrays.asList(a, b));
				}
				if ( anyElims ) {
					result = true;
					if ( accu.add(hint) )
						break;
				}
				anyElims = doubleLinked = false;
				zsZapped = 0;
			}
		// GUI only (we don't get here if accu is a SingleHintAccumulator)
		}
		if ( result )
			accu.sort(null); // put the highest scoring hint first
		return result;
	}

	// Always report it, then throw or return true to add it
	private boolean handle(final AlsXzHint hint, final Grid grid, final Iterable<Als> alss) {
		reportRedPots(tech.name(), grid, alss);
		if ( Run.type == Run.Type.GUI && Run.ASSERTS_ENABLED ) {
			// techies: we're in the GUI in java -ea
			hint.setIsInvalid(true);
			hint.setDebugMessage(colorIn("<p><r>"+prevMessage+"</r>"));
			return true;
		}
		// else we're in a prod-GUI, or a test-case, or the batch
		throw new UnsolvableException(Log.me()+": bad reds: "+hint.reds);
	}

}
