/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;


/**
 * IrrelevantHintException is thrown when AlignedExclusionHint appendTo (while 
 * generating HTML for display) trips overs a hint with 0 relevant combo/s in
 * combosMap (ie the whole hint is irrelevant), which shouldn't happen, but has
 * due to bugs upstream, so we're defensive programming here... dealing with the
 * situation as best we can.
 * <p>2019-09-13 KRC We had the same situation in CellReductionHint, so I 
 * promoted this class out of AlignedExclusionHint into the solver package.
 * @stretch Why are these happening? Make Them Stop! Make Them Stop!
 * @author Keith Corlett 2019 SEPT
 */
public class IrrelevantHintException extends RuntimeException {
	private static final long serialVersionUID = 923501423L;
	public IrrelevantHintException() { }
}
