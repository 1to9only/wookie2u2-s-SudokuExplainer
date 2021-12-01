/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Tech;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.fish.BasicFisherman;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.lock.Locking;
import diuf.sudoku.solver.hinters.nkdset.NakedSet;
import diuf.sudoku.solver.hinters.single.HiddenSingle;
import diuf.sudoku.solver.hinters.single.NakedSingle;
import java.util.EnumMap;

/**
 * BasicHinters constructs the basic hinters. It's public for the test-cases.
 * I was in LogicalSolver, but that means that any and all other static fields
 * are initialised when LogicalSolverBuilder invokes this static method, so I
 * have stuck him out here in his own public class so that no other crap gets
 * created as a side-effect.
 *
 * @author Keith Corlett 2021-09-16
 */
public class BasicHintersBuilder {

	/**
	 * Populates 'basics' with new basic hinters and returns it.
	 * <p>
	 * This is a method coz test-cases need a way to construct basic hinters,
	 * but LogicalSolverBuilder is package-visible by design (it'd need to be
	 * be public in order to expose me) so LogicalSolverBuilder calls this
	 * static method on LogicalSolver which CAN expose it to the test-cases.
	 *
	 * @param basics to (re)populate
	 * @return the given basics, (re)populated with new basic hinters
	 */
	public static EnumMap<Tech,IHinter> build(
			final EnumMap<Tech,IHinter> basics) {
		basics.put(Tech.NakedSingle, new NakedSingle());
		basics.put(Tech.HiddenSingle, new HiddenSingle());
		basics.put(Tech.Locking, new Locking());
		basics.put(Tech.NakedPair, new NakedSet(Tech.NakedPair));
		basics.put(Tech.HiddenPair, new HiddenSet(Tech.HiddenPair));
		basics.put(Tech.NakedTriple, new NakedSet(Tech.NakedTriple));
		basics.put(Tech.HiddenTriple, new HiddenSet(Tech.HiddenTriple));
		basics.put(Tech.Swampfish, new BasicFisherman(Tech.Swampfish));
		return basics;
	}

}