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
 * An RCC is a Restricted Common Candidate/s: one-or-two values that two ALS's
 * have in common, which all see each other (the nominative restriction);<br>
 * and optionally cannot be in the intersection of the two ALS's;<br>
 * and optionally must be more than one cell (no bivalue cells).
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

	/** Used in ALS-Chains: 0=none, 1=cand1, 2=cand2, 3=both. */
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
	 * New propagation rules for AlsChain: When finding a link in a chain the
	 * actual RCs of {@code p} are excluded from {@code this}, so
	 * {@code this.which} is adjusted accordingly.
	 * <p>
	 * Returns: is the resulting {@code this.which} != 0,<br>
	 * ie true meaning the chain continues,<br>
	 * or false meaning end-of-chain.
	 * <p>
	 * If a chain starts with a doubly-linked RC ({@code rc==null && v2!=0})
	 * then one of the two RCs is chosen depending on {@code first)
	 * (TRUE=1, FALSE=0), searching both possible links in a chain.
	 *
	 * @param p the Rcc of the previous link in the current chain
	 * @param firstTry when <tt>p==null</tt> (first link in a chain)<br>
	 *  should we use <tt>v1</tt> (the first attempt)<br>
	 *  or <tt>v2</tt> (the second attempt).
	 * @return does any RC-value remain to be examined
	 */
	public boolean whichRC(Rcc p, boolean firstTry) {
		// NOTE: terniaries are slow!
		if ( v2 == 0 )
			which = 1; // cand1 only
		else
			which = 3; // both
		if ( p == null ) {
			// start of chain: pick the RC to examine
			if ( v2 != 0 )
				if ( firstTry )
					which = 1; // examine cand1
				else
					which = 2; // examine cand2
		} else
			// continueing chain: pick my RC based on prevRC.whichRC
			switch ( p.which ) {
				case 0: break; // whichRC is already set
				case 1: which = check(p.v1, 0, v1, v2); break; // cand1 only
				case 2: which = check(p.v2, 0, v1, v2); break; // cand2 only
				case 3: which = check(p.v1, p.v2, v1, v2); break; // both cand1 and cand2
				default: break;
			}
		return which != 0;
	}

	/**
	 * Suppress duplicate candidate values from the previous to the current RCC
	 * in the chain by returning the value of the current whichRC:<br>
	 * 0=none, 1=cand1, 2=cand2, 3=both.
	 * <p>
	 * <pre>
	 * SPEED: This is a static method. All values are passed in, especially
	 * this.cand1 and this.cand2. Static methods are faster to invoke, ergo
	 * Javas this-injection (unsurprisingly) slows-down each call.
	 * I also tried:
	 * 1. all params final. No faster.
	 * 2. tried eliminating else's. Slower (WTF?). Revert.
	 * 3. I had already removed the getters and setters from als1, als2, cand1,
	 *    cand2, and whichRC to "debeanify" the Rcc class, but it's slower, but
	 *    I'm still in two minds RE reverting ALL of these changes. sigh.
	 * I am learning that hobiwan was pretty bloody good at writing FAST code,
	 * even when my previous experience tell me it might be faster to do things
	 * differently the impirical evidence keeps on telling me otherwise. I am
	 * tempted to blame "hot box" for the losses, and try again tomorrow in the
	 * cool of early morning. I have no air-conditioning, nor the money, nor da
	 * inclination to contribute to planetaty destruction by using one. If it's
	 * hot then PCs slow down. Get Over It!
	 * </pre>
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
