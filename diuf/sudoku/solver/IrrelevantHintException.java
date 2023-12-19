/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * IrrelevantHintException is thrown when
 * {@link diuf.sudoku.solver.hinters.align.AlignedExclusionHint#appendTo}
 * (while generating HTML for display) trips overs a hint with zero relevant
 * combo/s in the combosMap, ie the whole hint is irrelevant, which should not
 * happen, but has, due to bugs upstream, so we are reactive programming here,
 * dealing with the symptom (not the disease) as best we can at this stage.
 * <p>
 * 2019-09-13 KRC We had the same situation in CellReductionHint, so I promoted
 * this class out of AlignedExclusionHint into the solver package.
 *
 * @author Keith Corlett 2019 SEPT
 */
public class IrrelevantHintException extends RuntimeException {

	private static final long serialVersionUID = 923501423L;

	public IrrelevantHintException() { }

}
