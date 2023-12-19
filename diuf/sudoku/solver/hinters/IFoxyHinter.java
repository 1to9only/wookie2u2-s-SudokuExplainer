/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

/**
 * IFoxyHinter is a marker interface for IHinters that are "foxes" in
 * {@link diuf.sudoku.solver.hinters.chain.ChainerDynamic#fourQuickFoxes }.
 * ChainerDynamic is itself an IFoxyHinter. He knows Chrissie Amphlette.
 * Though he never actually embedded her, per se. Not guilty your honour!
 * <p>
 * IFoxyHinter is empty. It specifies no methods. It exists for humans.
 * This interfaces exists just to say "this hinter is a bit special".
 * It says all of this hinters hint-types implement IFoxyHint, and that
 * (Chainers excepted) I override findHints(Grid, IAccumulator), for speed.
 * <p>
 * To find all of the FourQuickFoxes you can search usages of IFoxyHinter.
 * You'll find that, like many programmers, I'm numeratively and linguistically
 * challenged, and fairly challenging, but you get a bit of that, mainly on the
 * big jobs. All hints produced by those hinters implement {@link IFoxyHint},
 * so you can search for usages of IFoxyHint to find all da getParents methods.
 * That's about it. No use whatever, but still useful.
 *
 * @author Keith Corlett 2023-09-21
 */
public interface IFoxyHinter extends IHinter {

	public default void initialise() {
		// Do nothing
	}

	public default void deinitialise() {
		// Do nothing
	}

}
