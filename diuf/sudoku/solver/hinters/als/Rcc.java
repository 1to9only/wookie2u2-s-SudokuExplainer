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

/**
 * An RCC is a Restricted Common Candidate, that is one-or-two values that two
 * ALS's have in common, which all see each other (the nominative restriction);<br>
 * and must not be in the overlap (if any) of the two ALS's;<br>
 * and must be more than one cell (a bivalue cell is NOT an ALS);<br>
 * and optionally allowOverlaps: are the two ALS's allowed to overlap;<br>
 * and optionally forwardOnly: examine forwardOnly pairs of alss or ALL pairs.<br>
 * <p>
 * KRC: My version is debeanified, ergo it exposes public fields instead of
 * public getters/setters on private fields, because the getters/setters add
 * no value, and are, in my experience, just a bit slower: ergo it's faster
 * to access a public field than it is to invoke a getter/setter. However, my
 * performance-testing post debeanification was SLOWER, which I'm steadfastly
 * ignoring the ____ out of. sigh.
 *
 * @author hobiwan, but this version has been hacked by KRC.
 */
public class Rcc implements Comparable<Rcc>, java.io.Serializable
{
	private static final long serialVersionUID = 4153506457540778L;

	/** Index of first ALS in a {@code List<Als>} kept externally. */
	public final int als1;

	/** Index of second ALS in a {@code List<Als>} kept externally. */
	public final int als2;

	/** First RC, must be != 0. */
	public final int v1;

	/** Second RC; if {@code v2==0} als1 and als2 have only one RC value. */
	public int v2;

	/** Used in ALS-Chains: 0=none, 1=v1, 2=v2, 3=both. */
	public int which;

	/**
	 * Constructs a new {@code Rcc} for ALSs which are presumed to be singly
	 * linked upon creation, but a second RC value may be added later.
	 * @param als1
	 * @param als2
	 * @param v1
	 */
	public Rcc(int als1, int als2, int v1) {
		this.als1 = als1;
		this.als2 = als2;
		this.v1 = v1;
		this.v2 = 0;
	}

	/**
	 * New propagation rules for AlsChain: When finding a link in a chain,
	 * {@code this.which} is adjusted to exclude the active RC of {@code p}.
	 * <p>
	 * Returns: is the resulting {@code this.which} != 0,<br>
	 * ie true meaning the chain continues,<br>
	 * or false meaning no more chain here, so we stop following this chain.
	 * <p>
	 * If a chain starts with a doubly-linked RC ({@code p==null && v2!=0})
	 * then one of the two RCs is chosen depending on {@code firstTry)}
	 * (TRUE=1, FALSE=0), searching both possible links in a chain.
	 *
	 * @param p the Rcc of the previous link in the current chain
	 * @param firstTry when <tt>p==null</tt> (first link in a chain)<br>
	 *  should we use <tt>v1</tt> (the first attempt)<br>
	 *  or <tt>v2</tt> (the second attempt).
	 * @return does any RC-value remain to be examined
	 */
	public boolean whichRC(Rcc p, boolean firstTry) {
		if ( p == null ) {
			// start of chain: pick the RC to examine
			if ( v2==0 || firstTry ) {
				which = 1; // v1 only
			} else {
				// confirmed this happens in top1465, but not often
				which = 2; // v2 only
			}
		} else {
			// ongoing chain: pick my RC based on p.which
			switch ( p.which ) {
				case 1:
					which = check(p.v1, 0, v1, v2); // v1 only
					break;
				case 2:
					which = check(p.v2, 0, v1, v2); // v2 only
					break;
				case 3:
					// confirmed this never happens in top1465
					which = check(p.v1, p.v2, v1, v2); // both
					break;
				case 0: // I think 0 can NEVER happen, but just in case:
					// confirmed this never happens in top1465
					if ( v2 == 0 ) {
						which = 1; // v1 only
					} else {
						which = 3; // both
					}
					break;
			}
		}
		return which != 0;
	}

	/**
	 * Suppress duplicate candidate values from the previous to the current
	 * RCC in the chain by returning the current which:<br>
	 * 0=none, 1=v1, 2=v2, 3=both.
	 * <p>
	 * <pre>
	 * SPEED: This is a static method. All values are passed in, especially
	 * this.v1 and this.v2. Static methods are faster to invoke, ergo Javas
	 * this-injection (unsurprisingly) markedly slows-down each invocation.
	 * I also tried:
	 * 1. eliminating else's. Slower (WTF?). Revert.
	 * 2. I already debeanified Rcc by removing gets and sets for als1, als2,
	 *    v1, v2, and which, but it's slower, and I don't know why, but I'm
	 *    accepting the debeanified version anyway, coz beans suck. sigh.
	 * But jeez hobiwan was pretty bloody good at writing FAST code!
	 * </pre>
	 * KRC 2021-07-28 I'm tempted to try storing the bitset version of the RCC
	 * values, as well as as the decoded plain values, so that I can just &~
	 * the previous RC out of the current RC, and if it's not zero then they're
	 * not the same value, but I'm still unsure what all of this actually does,
	 * so I'm writing a big long useless nervous comment instead of getting on
	 * with it because I'm nervous about it, which I shouldn't be, I should
	 * just take a back-up (ie do a release) and get-on with breaking s__t, so
	 * I can satisfy my curiousity with very little risk.
	 *
	 * @param p1 previous RCC first candidate
	 * @param p2 previous RCC second candidate (may be 0)
	 * @param c1 current RCC first candidate
	 * @param c2 current RCC Second candidate (may be 0)
	 * @return the value of this.whichRC.
	 */
	private static int check(final int p1, final int p2, final int c1, final int c2) {
		// NOTE: terniaries are slow!
		if ( p2 == 0 ) { // one previous RC
			if ( c2 == 0 ) // one current RC
				if ( p1 == c1 )
					return 0;
				else
					return 1;
			else // two current RCs
				if ( p1 == c2 )
					return 1;
				else if ( p1 == c1 )
					return 2;
				else
					return 3;
		} else { // two previous RC's
			if ( c2 == 0 ) // one current RC
				if ( p1==c1 || p2==c1 )
					return 0;
				else
					return 1;
			else // two current RC's
				if ( (p1==c1 && p2==c2) || (p1==c2 && p2==c1) )
					return 0;
				else if ( p1==c2 || p2==c2 )
					return 1;
				else if ( p1==c1 || p2==c1 )
					return 2;
				else
					return 3;
		}
	}

	/**
	 * Returns a string representation of {@code this}.
	 * @return
	 */
	@Override
	public String toString() {
		return "alss=" + als1 + "/" + als2
			 + " cands=" + v1 + "/" + v2
			 + " which=" + which;
	}

	/**
	 * Compares this RCC with the given one.
	 * @param r
	 * @return less than 0 if this comes before that;<br>
	 *  or greater than 0 if this comes after that;<br>
	 *  or 0 if they're the same in this ordering.
	 */
	@Override
	public int compareTo(Rcc r) {
		int result;
		if ( (result=als1 - r.als1) == 0
		  && (result=als2 - r.als2) == 0
		  && (result=v1 - r.v1) == 0 )
			result = v2 - r.v2;
		return result;
	}

}
