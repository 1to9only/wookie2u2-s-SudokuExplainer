/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Tech;
import diuf.sudoku.solver.hinters.naked.*;
import diuf.sudoku.solver.hinters.hidden.*;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.sdp.TwoStringKite;
import diuf.sudoku.solver.hinters.single.*;
import java.util.EnumMap;

/**
 * BasicHinters constructs the basic hinters. I am public for the test-cases.
 * I was in LogicalSolver, but that means that any and all other static fields
 * are initialised when LogicalSolverBuilder invokes this static method, so I
 * am out here in my own public class so that none of LogicalSolvers other
 * static crap gets created as a side-effect of invocation.
 *
 * @author Keith Corlett 2021-09-16
 */
public class BasicHintersBuilder {

	/**
	 * Populates the basics EnumSet with new basic hinters and returns it.
	 * <p>
	 * Note that the "basic hinters" are always constructed, for use in the
	 * chainers, even if they are not wanted by the user, which matters not
	 * because they are all pretty fast to construct, nor do they use a lot
	 * of RAM. No heavyweights here.
	 * <p>
	 * This method has its own public class because the test-cases need to
	 * construct basic hinters. LogicalSolverBuilder is package-visible by
	 * design (it would need to be be public in order to expose this method)
	 * so LogicalSolverBuilder calls this BasicHintersBuilder static factory
	 * method, which CAN also be exposed to the test-cases. Simples.
	 *
	 * @return new basic hinters in an {@code EnumMap<Tech,IHinter>}.
	 */
	public static EnumMap<Tech,IHinter> build() {
		final EnumMap<Tech,IHinter> basics = new EnumMap<>(Tech.class);
		basics.put(Tech.NakedSingle, new NakedSingle());
		basics.put(Tech.HiddenSingle, new HiddenSingle());
		basics.put(Tech.Locking, new Locking());
		basics.put(Tech.NakedPair, new NakedPair());
		basics.put(Tech.HiddenPair, new HiddenPair());
		basics.put(Tech.NakedTriple, new NakedTriple());
		basics.put(Tech.HiddenTriple, new HiddenTriple());
		basics.put(Tech.TwoStringKite, new TwoStringKite());
		basics.put(Tech.Swampfish, new Fisherman(Tech.Swampfish));
		return basics;
	}

}
