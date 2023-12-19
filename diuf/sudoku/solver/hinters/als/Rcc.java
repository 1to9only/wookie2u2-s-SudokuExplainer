/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The Rcc class is boosted from HoDoKu RestrictedCommonCandidate class.
 * Kudos to hobiwan. Mistakes are mine.
 *
 * Here is hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
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

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;

/**
 * RCC means Restricted Common Candidate/s: one-or-two-values linking a source
 * ALS to a related ALS, where all occurrences of those value/s in either ALS
 * see each other, hence one of the two ALSs must contain the value.
 * <pre>
 * An RC-value is a value in two ALSs:
 *   that all see each other (that is the nominative restriction);
 *   and are not in the overlap (if any) of the two ALSs;
 *   and are more than one cell (a bivalue cell is NOT an ALS in SE).
 * Approximately 9% of RCCs have two RC-values, never three.
 * </pre>
 * KRC: Rcc is debeanified: it exposes public fields instead of getters/setters
 * on private fields, coz these getters/setters added 0 value, and are slower.
 * It is faster to access a public field than it is to invoke a getter/setter.
 * However, my performance-testing post debeanification was SLOWER, hence I am
 * steadfastly ignoring the ____ out of, in the hope that it will all just go
 * away, much like a Conservative and reality. Have you asked your gopher about
 * Cleetus change? Mockingjay is afraid it's quite extensive. Sigh.
 * <p>
 * KRC 2021-10-12 Rcc is immutable: state is set completely in the constructor,
 * and cannot subsequently change. My motives for this change are pretty vague.
 * It just feels right. It reduces top1465 time by 4 seconds. Removed unused
 * {@code implements Serializable, Comparable<Rcc>} to keep it simple.
 *
 * @author hobiwan, but KRCs Rcc has been stupified.
 */
public final class Rcc {

	/** Index of source-ALS in the external alss array. */
	public final int source;

	/** Index of related-ALS in the external alss array. */
	public final int related;

	/** Cands is a bitset of v1 and v2. */
	public final int cands;

	/** First RC, must be &gt; 0. */
	public final int v1;

	/** Second RC; if {@code v2==0} als1 and als2 have only one RC value.
	 * About 95% of RCCs have just one RC value. The rest have two. There are
	 * no RCCs with three RC-values, hence the software does not support it.
	 */
	public final int v2;

	/**
	 * Constructor.
	 *
	 * @param source the index of the sourceAls in the alss array
	 * @param related the index of the relatedAls in the alss array
	 * @param cands both RC-values as a bitset
	 */
	public Rcc(final int source, final int related, final int cands) {
		this.source = source;
		this.related = related;
		this.cands = cands;
		// first RC-value is mandatory (always >0).
		this.v1 = VFIRST[cands];
		// second RC-value is optional. Used in ~9% of Rccs. 0 means none.
		this.v2 = VFIRST[cands & ~VSHFT[v1]]; // nb: VFIRST[0] is 0
	}

	/**
	 * For debugging.
	 *
	 * @return a String representation of this Rcc
	 */
	@Override
	public String toString() {
		return SB(32).append("alss=").append(source).append("/").append(related)
		.append(" cands=").append(v1).append("/").append(v2).toString();
	}

}
