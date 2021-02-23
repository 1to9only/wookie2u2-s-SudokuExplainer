/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import java.util.LinkedHashSet;


/**
 * A hintNumber activated hinter: a fancy-pants hinter which activates only at
 * the given hintNumbers; the rest of the time it's deactivated, so that the
 * LogicalSolver doesn't invoke it.
 * <p>
 * This interface is for my Aligned*Exclusion hinters which are still TOO SLOW.
 * I parse the previous logFile so I know which hintNumbers each hinter hints
 * at. It's a total hack, but to me this is within the bounds of the challenge:
 * to solve these puzzles using logic as accurately and quickly as possible.
 * I mean this is still logic and it's still accurate. and it's much faster:
 * like 2 minutes verses 45 minutes.
 * @author Keith Corlett 2019 OCT
 */
public interface INumberedHinter {
	public void setHintNumbers(LinkedHashSet<Integer> hintNumbers);
	public boolean isActuallyHintNumberActivated();
	public boolean activate(int hintNumber); // 1-based
	public void setIsActive(boolean isActive);
}
