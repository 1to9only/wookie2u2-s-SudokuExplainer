/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
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
// Almost Locked Set from HoDoKu

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An RCC is a Restricted Common Candidate, which is the one-or-two values that
 * two ALS's have in common, which all see each other (that's the nominative
 * restriction); and optionally cannot be in the intersection of the two ALS's;
 * and optionally must be more than one cell (no bivalue cells as ALS's).
 *
 * @author hobiwan originally but this ones a bit of a KRC hack, with fruit
 * juice, but no gannets.
 */
public class Rcc implements Comparable<Rcc>, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    /** Index of first ALS in a {@code List<Als>} kept externally. */
    private int als1;

    /** Index of second ALS in a {@code List<Als>} kept externally. */
    private int als2;

    /** First RC, must be != 0. */
    private int cand1;

    /** Second rc; if {@code cand2==0} als1 and als2 have only one RC value. */
    private int cand2;

    /** Used for propagation checks in ALS-Chains
	 * (see {@link AlsSolver#getAlsXYChain()} for details).
     * 0: none, 1: cand1 only, 2: cand2 only, 3: both.
     */
    private int actualRC;

    /**
     * Creates a new instance of {@code RestricteCommon}.
     */
    public Rcc() {
    }

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
     * Returns a shallow copy of {@code this}. Since the class holds only
     * base types, this is sufficient.
     *
     * @return
     */
    @Override
    public Object clone() {
        try {
            Rcc newRC = (Rcc)super.clone();
            return newRC;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning (RC)", ex);
            return null;
        }
    }

    /**
     * New propagation rules for ALS-Chains: the actual RCs of parameter
     * {@code rc} are excluded from {@code this},
     * {@code this.actualRC} is adjusted as necessary;
     * if {@code this.actualRC} is greater than {@code 0} the
     * chain can be continued and
     * true is returned, else false is returned.<br><br>
     *
     * If a chain starts with a doubly linked RC ({@code rc == null},
	 * {@code cand2 != 0}), one of the RCs can be chosen freely; this results
	 * in two different tries for the chain search.
     *
     * @param prevRC RC of the previous link in a chain
     * @param firstTry Only used, if {@code rc == null}: if set, {@code cand1}
	 *  is used else {@code cand2}
     * @return true if an actual RC remains, false otherwise
     */
    public boolean checkRC(Rcc prevRC, boolean firstTry) {
        actualRC = cand2==0 ? 1 : 3;
        if ( prevRC == null ) { // previous rc is not provided
			// start of chain: pick your RC
            if ( cand2 != 0 )
                actualRC = firstTry ? 1 : 2;
        } else
			switch ( prevRC.actualRC ) {
				case 0: break; // already done
				case 1: actualRC = checkRCInt(prevRC.cand1, 0, cand1, cand2); break;
				case 2: actualRC = checkRCInt(prevRC.cand2, 0, cand1, cand2); break;
				case 3: actualRC = checkRCInt(prevRC.cand1, prevRC.cand1, cand1, cand2); break;
				default: break;
			}
        return actualRC != 0;
    }

    /**
     * Checks duplicates (all possible combinations); both {@code c12} and
	 * {@code c22} (the second RC values) may be 0, meaning none.
     *
     * @param c11 First ARC of first link
     * @param c12 Second ARC of first link (may be 0)
     * @param c21 First PRC of second link
     * @param c22 Second PRC of second link (may be 0)
     * @return
     */
    private int checkRCInt(int c11, int c12, int c21, int c22) {
        if ( c12 == 0 ) // one ARC
            if (c22 == 0) // one ARC one PRC
                return c11 == c21 ? 0 : 1;
			else // one ARC two PRCs
                return c11 == c22 ? 1
					 : c11 == c21 ? 2
					 : 3;
		else // two ARCs
            if (c22 == 0) // two ARCs one PRC
                return c11==c21 || c12==c21 ? 0 : 1;
            else // two ARCs two PRCs
                return ((c11==c21 && c12==c22) || (c11==c22 && c12==c21)) ? 0
					 : (c11==c22 || c12==c22) ? 1
					 : (c11==c21 || c12==c21) ? 2
				     : 3;
    }

    /**
     * Returns a string representation of {@code this}.
     * @return
     */
    @Override
    public String toString() {
        return "alss=" + als1 + "/" + als2
			 + " cands=" + cand1 + "/" + cand2
			 + " arc=" + actualRC;
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

    /**
     * Getter for {@link #als1}.
     * @return
     */
    public int getAls1() {
        return als1;
    }

    /**
     * Setter for {@link #als1}.
     * @param als1
     */
    public void setAls1(int als1) {
        this.als1 = als1;
    }

    /**
     * Getter for {@link #als2}.
     * @return
     */
    public int getAls2() {
        return als2;
    }

    /**
     * Setter for {@link #als2}.
     * @param als2
     */
    public void setAls2(int als2) {
        this.als2 = als2;
    }

    /**
     * Getter for {@link #cand1}.
     * @return
     */
    public int getCand1() {
        return cand1;
    }

    /**
     * Setter for {@link #cand1}.
     * @param cand1
     */
    public void setCand1(int cand1) {
        this.cand1 = cand1;
    }

    /**
     * Getter for {@link #cand2}.
     * @return
     */
    public int getCand2() {
        return cand2;
    }

    /**
     * Setter for {@link #cand2}.
     * @param cand2
     */
    public void setCand2(int cand2) {
        this.cand2 = cand2;
    }

    /**
     * Getter for {@link #actualRC}.
     * @return
     */
    public int getActualRC() {
        return actualRC;
    }

    /**
     * Setter for {@link #actualRC}.
     * @param actualRC
     */
    public void setActualRC(int actualRC) {
        this.actualRC = actualRC;
    }

}
