/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Tech;
import java.util.LinkedHashSet;


/**
 * AHintNumberActivatableHinter is an abstract class which allows any AHinter
 * to be activated only for the given set of hint-numbers, else deactivated.
 * <p>This whole class exists to support a fast (Luke 01:27) HACK!
 * @author Keith Corlett 2019 OCT
 */
public abstract class AHintNumberActivatableHinter
		extends AHinter
		implements INumberedHinter, ICleanUp
{
	/** this class will be activated for only the given hintNumbers, which
	 * are extracted from a previous log-file. Ergo cheat like a mofo. */
	public LinkedHashSet<Integer> hintNumbers = null;

	/** the minimum hintNumber at which this technique finds a hint. */
	public final int firstHintNumber;
	private boolean isSingleHintNumber;
	private int singleHintNumber;

	/**
	 * Constructs a new abstract hint-number activatable hinter.
	 * @param tech the Sudoku Solving Technique which you're implementing.
	 * @param firstHintNumber the minimum hintNumber at which that Sudoku
	 * Solving Technique finds a hint. If you don't know yet then just use 1
	 * and leave yourself a at-to-do to fill this in later from a logFile of
	 * your software on your puzzles. Unfortunately this value is allways a
	 * local one, garnered from experience. There is no correct answer other
	 * than 1 (or 0), meaning "allways active".
	 */
	public AHintNumberActivatableHinter(Tech tech, int firstHintNumber) {
		super(tech);
		this.firstHintNumber = firstHintNumber;
	}

	@Override
	public void cleanUp() {
		hintNumbers = null;
	}

	@Override
	public void setHintNumbers(LinkedHashSet<Integer> hintNumbers) {
		this.isSingleHintNumber = false;
		this.hintNumbers = null;
		if ( hintNumbers == null )
			return;
		switch ( hintNumbers.size() ) {
		case 0:
			break;
		case 1:
			this.isSingleHintNumber = true;
			this.singleHintNumber = hintNumbers.iterator().next();
			break;
		default:
			this.hintNumbers = hintNumbers;
		}
	}

	@Override
	public boolean isActuallyHintNumberActivated() {
		return isSingleHintNumber || hintNumbers!=null;
	}

	@Override
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public boolean activate(int hintNumber) {
		if ( this.isSingleHintNumber )
			return this.isActive = hintNumber == this.singleHintNumber;
		if ( this.hintNumbers == null ) // nb: hintNumbers is NEVER empty
			return this.isActive = hintNumber >= this.firstHintNumber;
		return this.isActive = this.hintNumbers.contains(hintNumber);
	}

}
