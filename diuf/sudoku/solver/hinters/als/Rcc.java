/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The Rcc class is boosted from HoDoKu's RestrictedCommonCandidate class.
 * Kudos to hobiwan. Mistakes are mine.
 *
 * Here's hobiwans standard licence statement:
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

import static diuf.sudoku.Values.VSHFT;

/**
 * An RCC is a Restricted Common Candidate.
 * <pre>
 * That is one-or-two values of two ALSs:
 *   that all see each other (that's the nominative restriction);
 *   and are not in the overlap (if any) of the two ALS's;
 *   and are more than one cell (a bivalue cell is NOT an ALS in my world);
 *   option allowOverlaps: are the two ALS's allowed to overlap;
 *   option forwardOnly: true for distinct pairs {a-b only}
 *                   or false for ALL pairs {a-b and b-a}
 * </pre>
 * KRC: Rcc is debeanified: it exposes public fields instead of getters/setters
 * on private fields, coz these getters/setters added 0 value, and are slower.
 * It's faster to access a public field than it is to invoke a getter/setter.
 * However, my performance-testing post debeanification was SLOWER, hence I am
 * steadfastly ignoring the ____ out of it, in the hope that it will just go
 * away, a bit like the Liberal and his physicist. sigh.
 * <p>
 * KRC 2021-10-12 Rcc is now immutable: it's state is completely set in the
 * constructor, and this instance cannot subsequently be modified. I'm vague
 * about my motives for this change: "it just feels right". I was right! This
 * simple change reduces top1465 time by 4 seconds. Also removed the unused
 * {@code implements Serializable, Comparable<Rcc>}, just to keep it simple.
 *
 * @author hobiwan, but this version has been well-hacked by KRC.
 */
public class Rcc {

	/** Index of source-ALS in the external alss array. */
	public final int source;

	/** Index of related-ALS in the external alss array. */
	public final int related;

	/** First RC, must be != 0. */
	public final int v1;

	// I'd make v2 final except it makes creating an Rcc much harder.
	/** Second RC; if {@code v2==0} als1 and als2 have only one RC value. */
	public final int v2;

	/** Cands is a bitset of v1 and v2. */
	public final int cands;

	/**
	 * The AlsChain constructor, with the leaf parameter.
	 * <p>
	 * Constructs a new {@code Rcc}. An RCC is the one-or-two values that link
	 * the sourceAls to the relatedAls, where all occurrences of those value/s
	 * in either ALS see each other, so that either the sourceAls will contain
	 * the value, or the relatedAls will contain the value, but not both.
	 *
	 * @param source index of the sourceAls in the alss array
	 * @param related index of the relatedAls in the alss array
	 * @param v1 the first RC-value, a restricted (they all see each other,
	 *  and none appear in the physical overlap, if any, of these two ALSs)
	 *  candidate value common to both the source and related ALSs
	 * @param v2 the occasional second RC-value
	 */
	public Rcc(final int source, final int related, final int v1, final int v2) {
		this.source = source;
		this.related = related;
		this.cands = VSHFT[this.v1=v1] | VSHFT[this.v2=v2];
	}

	/**
	 * Returns a String representation of this Rcc.
	 * <p>
	 * Used only to DEBUG, so you can do whatever you like to it.
	 *
	 * @return a String representation of this Rcc
	 */
	@Override
	public String toString() {
		return "alss=" + source + "/" + related
			 + " cands=" + v1 + "/" + v2
//			 + " ("+cands+")"
			 ;
	}

}
