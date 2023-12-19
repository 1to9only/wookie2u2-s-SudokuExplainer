/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is inpired by hobiwan HoDoKu. I did not copy his code, just
 * pinched all of his ideas. Kudos to hobiwan. Mistakes are mine.
 *
 * Here is hobiwans standard licence statement:
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
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;

/**
 * AlsXz implements the {@link Tech#ALS_XZ} Sudoku solving technique. The name
 * means Almost Locked Set XZ, Where x and z are values, that are pushed along
 * the "chain".
 * <p>
 * I extend AAlsHinter which implements IHinter.findHints to find the ALSs,
 * determine there RCCs (links), and call my findAlsHints method passing it all
 * down. My findAlsHints method implements the ALS-XZ technique. Each subclass
 * of AAlsHinter implements its own specific technique.
 * <p>
 * An ALS-XZ is when two Almost Locked Sets are linked to each other by common
 * candidates that all see each other, RC-Values: Restricted Common Values,
 * eliminating some values from some cells. See findAlsHints for details.
 * <p>
 * A deeper explanation is provided in {@link #findAlsHints} comments.
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
 * It is slower, but it cant be. It spends most time in Als.computFields, and I
 * know that inline Idx-ops are faster, especially in ONE stackFrame. So IDFKA!
 * So, I either revert or accept that inline is in fact SLOWER. I refuse!
 * PRE: 9,270,523,900  7908  1,172,296  2013  4,605,327 ALS_XZ
 * PST: 9,200,267,800  7908  1,163,412  2013  4,570,426 ALS_XZ
 *
 * KRC 2023-08-23 reverted to Idx-ops for simplicity, which is slower, but it
 * matters not because I spend ~95% of my time fetching Rccs.
 * PRE: 5,157,937,800  9538    540,777  2040  2,528,400 ALS_XZ
 * PST: 5,423,133,100  9538    568,581  2040  2,658,398 ALS_XZ
 * NOTE: with MAX_ALS_SIZE=5 all the als hinters are faster; less rcc-time.
 * </pre>
 *
 * @author Keith Corlett 2020 Apr 26
 */
public final class AlsXz extends AAlsHinter {

	/**
	 * Constructor.
	 */
	public AlsXz() {
		super(Tech.ALS_XZ);
	}

	@Override
	public void setFields(Grid grid) {
		this.grid = grid;
		maybes = grid.maybes;
	}

	@Override
	public void clearFields() {
		grid = null;
		maybes = null;
	}

	/**
	 * Find all ALS-XZ hints in the given Grid: Almost Locked Set XZ, where x
	 * and z refer to values that are common to a pair of ALSs. The xs are
	 * restricted, and zs are not, they are just common. Bloody English! Sigh.
	 * <p>
	 * This is the simplest ALS technique. We find two ALSs which share a
	 * Restricted Common (RC) value called 'x'. If both ALSs also contain
	 * another (not an RC) common value called 'z', then z may be eliminated
	 * from all non-ALS cells which see ALL instances of z in both ALSs.
	 * <p>
	 * An ALS-XZ is an ALS Chain of length 2. Its handled separately because it
	 * has special rules when the two ALSs share 2 restricted common candidates
	 * (RC values). This is called a "Double Linked" ALS-XZ.
	 * <p>
	 * The logic behind ALS-XZ is simple enough: Because the RC-value must be
	 * in one of these two ALSs, one of the ALSs becomes a Locked Set (we know
	 * not yet which), and these two ALSs also share a z-value, that is pushed
	 * into the other ALS, hence z must be in one of the two ALSs, therefore
	 * external zs (not in either ALS) seeing all zs in both ALSs are not z.
	 * <p>
	 * DoubleLinked means the whole ALS-XZ is a locked-set, eliminating both
	 * RC-values from external cells (cells outside both ALSs) seeing all
	 * occurrences in both ALSs.
	 * <p>
	 * Further DoubleLinked eliminations are also possible. All non-RC-values
	 * (ks) in either ALS can be removed from all external cells (cells outside
	 * this ALS, including the other ALS) which see all ks in this ALS. The
	 * logic is only ONE value may be removed from ALS before it goes invalid
	 * (insufficient values to fill its cells), so we know that x is one ALS
	 * and z is in the other or vice-versa, hence every other value (for k in
	 * als.maybes & ~rcc.cands) in either ALS is locked into this ALS therefore
	 * we can remove k from external cells seeing all ks in this ALS, including
	 * the other ALS (cannibalism).
	 * <p>
	 * DoubleLinked Cannibalism: non-RC-values may be common to both ALSs where
	 * ALL occurrences do not see each other, but cannibalism (elimination from
	 * the other ALS) can still happen because one-or-more k in the other ALS
	 * might see all ks in this ALS (but ALL ks in both ALSs do not ALL see
	 * each other, got it?).
	 * <p>
	 * KRC edited hobiwans explanation.
	 *
	 * @return where hint/s found. As always, if IAccumulator.add returns true
	 *  then I return true (ie exit-early when the first hint is found)
	 */
	@Override
	protected boolean findAlsHints() {
		// As always, presume that no hints will be found.
		final Idx victims = new Idx(); // indices of cells to eliminate from
		final Pots reds = new Pots(); // removeable (red) Cell->Values
		int zsZapped = 0; // bitset of zs (non-rcs) removed by single-link
		boolean any = false // any elims (single or double linked)
			  , anyD = false // any double-linked elims
			  , result = false; // presume that no hint/s will be found
		// foreach rcc in rccs
		int i = 0;
		do {
			// unpack the rcc
			final Rcc rcc = RCCS[i]; // Restricted Common Candidate/s
			final Als a = ALSS[rcc.source]; // first ALS
			final Als b = ALSS[rcc.related]; // second ALS
			final int v1 = rcc.v1; // first common value
			final int v2 = rcc.v2; // second common value, if any, else 0.
			final int cands = rcc.cands; // v1 and v2 as a Values bitset
			// unpack the two ALSs
			final Idx[] avBuds = a.vBuds; // vs seeing all vs in ALS a
			final int aMaybes = a.maybes; // bitset of ALS a potential values
			final Idx[] bvBuds = b.vBuds; // vs seeing all vs in ALS b
			final int bMaybes = b.maybes; // bitset of ALS b potential values
			// singleLinked zValues: maybes in both ALSs except RCC value/s;
			// ergo "extra" common values, that dont all see each other.
			final int zs = aMaybes & bMaybes & ~cands;
			if ( zs > 0 ) { // 9bits
				// any single-linked elims?
				// foreach z-value: non-RC-values common to both ALSs
				for ( int z : VALUESES[zs] ) {
					// if any external zs see all zs in both ALSs.
					// nb: vBuds excludes own zs, so a&b is externals only.
					if ( victims.setAndAny(avBuds[z], bvBuds[z]) ) {
						// add the removable (red) potentials
						any |= reds.upsertAll(victims, maybes, z);
						// build a bitset of zs eliminated
						zsZapped |= VSHFT[z];
					}
				}
			}
			// doubleLinked AlsXzs can still eliminate without z-values.
			// just ~5.38% of rccs are doubleLinked,
			// and ~56.75 of them are ONLY doubleLinked
			if ( v2 > 0 ) { // NEVER negative
				// these ALSs are double linked, so any (possibly extra) elims?
				// 1a. elim external v1 that see all v1 in both ALSs.
				anyD = victims.setAndAny(avBuds[v1], bvBuds[v1])
					&& reds.upsertAll(victims, maybes, v1);
				// nb: this line joined to above with || was slower.
				// 1b. elim external v2 that see all v2 in both ALSs.
				anyD |= victims.setAndAny(avBuds[v2], bvBuds[v2])
					&& reds.upsertAll(victims, maybes, v2);
				// 2. elim non-RC-values (ks) that see all k in this ALS,
				//    including the other ALS (cannibalism).
				for ( int k : VALUESES[aMaybes & ~cands] )
					if ( avBuds[k].any() )
						anyD |= reds.upsertAll(avBuds[k], maybes, k);
				for ( int k : VALUESES[bMaybes & ~cands] )
					if ( bvBuds[k].any() )
						anyD |= reds.upsertAll(bvBuds[k], maybes, k);
				any |= anyD;
			}
			if ( any ) {
				// AlsXz FOUND!
				result = true;
				// nb: hint uses anyDoubleLinked to show AlsXzHintBig.html
				if ( accu.add(new AlsXzHint(grid, this, a, b, v1, v2, zsZapped
						, reds.copyAndClear(), anyD)) )
					break;
				// reset for next-time
				any = anyD = false;
				zsZapped = 0;
			}
		} while (++i < numRccs);
		return result;
	}

}
