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

import java.io.Serializable;


/**
 * An RCC is a Restricted Common Candidate/s: one-or-two values that two ALS's
 * have in common, which all see each other (the nominative restriction);<br>
 * and optionally cannot be in the intersection of the two ALS's;<br>
 * and optionally must be more than one cell (no bivalue cells).
 *
 * @author hobiwan, but this version has been hacked by KRC.
 */
public class Rcc implements Comparable<Rcc>, Serializable//, Cloneable
{
    private static final long serialVersionUID = 1L;

    /** Index of first ALS in a {@code List<Als>} kept externally. */
    public final int als1;

    /** Index of second ALS in a {@code List<Als>} kept externally. */
    public final int als2;

    /** First RC, must be != 0. */
    public final int cand1;

    /** Second RC; if {@code cand2==0} als1 and als2 have only one RC value. */
    public int cand2;

    /** Used in ALS-Chains: 0=none, 1=cand1, 2=cand2, 3=both. */
    public int whichRC;

    /**
     * Constructs a new {@code Rcc} for ALSs which are presumed to be singly
	 * linked upon creation, but a second RC value may be added later.
     * @param als1
     * @param als2
     * @param cand1
     */
    public Rcc(int als1, int als2, int cand1) {
        this.als1 = als1;
        this.als2 = als2;
        this.cand1 = cand1;
        this.cand2 = 0;
    }

    /**
     * New propagation rules for ALS-Chains: When finding a link in a chain
	 * the actual RCs of {@code prevRc} are excluded from {@code this}, so I
	 * adjust {@code this.whichRC} accordingly.
     * <p>
     * Returns: if resulting {@code this.whichRC > 0},<br>
	 * then return true meaning the chain continues,<br>
	 * else return false.
     * <p>
     * If a chain starts with a doubly-linked RC ({@code rc==null && cand2!=0})
	 * then one of the two RCs is chosen depending on {@code firstTry),
	 * searching both possible links in a chain.
     *
     * @param p the Rcc of the previous link in the current chain
     * @param isFirst when <tt>prevRC==null</tt> (first link in a chain)<br>
	 *  should we use <tt>cand1</tt> (the first attempt)<br>
	 *  or <tt>cand2</tt> (the second attempt).
     * @return does any RC-value remain to be examined
     */
    public boolean whichRC(Rcc p, boolean isFirst) {
		// NOTE: terniaries are slow!
		if ( cand2 == 0 )
			whichRC = 1; // cand1 only
		else
			whichRC = 3; // both
        if ( p == null ) {
			// start of chain: pick the RC to examine
            if ( cand2 != 0 )
				if ( isFirst )
					whichRC = 1; // examine cand1
				else
					whichRC = 2; // examine cand2
        } else
			// continueing chain: pick my RC based on prevRC.whichRC
			switch ( p.whichRC ) {
				case 0: break; // whichRC is already set
				case 1: whichRC = check(p.cand1, 0, cand1, cand2); break; // cand1 only
				case 2: whichRC = check(p.cand2, 0, cand1, cand2); break; // cand2 only
				case 3: whichRC = check(p.cand1, p.cand2, cand1, cand2); break; // both cand1 and cand2
				default: break;
			}
        return whichRC != 0;
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
	 * inclination to contribute to the destruction of my bloody planet by
	 * using one. If it's hot then my PC runs slower. Get Over It!
	 * An evaporative-PC-cooler might work though. sigh.
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
		} else // two ARCs
            if ( c2 == 0 ) // two ARCs one PRC
                if ( p1==c1 || p2==c1 )
					return 0;
				else
					return 1;
			else // two ARCs two PRCs
                if ( (p1==c1 && p2==c2) || (p1==c2 && p2==c1) )
					return 0;
				else if ( p1==c2 || p2==c2 )
					return 1;
				else if ( p1==c1 || p2==c1 )
					return 2;
				else
					return 3;
    }

    /**
     * Returns a string representation of {@code this}.
     * @return
     */
    @Override
    public String toString() {
        return "alss=" + als1 + "/" + als2
			 + " cands=" + cand1 + "/" + cand2
			 + " arc=" + whichRC;
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
		  && (result=cand1 - r.cand1) == 0 )
			result = cand2 - r.cand2;
        return result;
    }

}
