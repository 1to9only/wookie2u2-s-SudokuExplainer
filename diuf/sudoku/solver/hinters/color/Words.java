/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import static diuf.sudoku.utils.Frmt.NL;


/**
 * Words used in Medusa3D and GEM, because my GEM is just Medusa3D on roids.
 * It seems to be a bit faster to reduce common Strings to a single instance.
 * This technique is only used where it matters. So the code presents as an
 * why-is-it-different-puzzle. You will work it out, or not. Life's like that.
 *
 * @author Keith Corlett 2021-06-29
 */
class Words {

	/** green and blue. */
	static final String green = "green";
	static final String blue = "blue";

	// html tags
	static final String KON = "<k>", KOFF = "</k>";		// pink
	static final String GON = "<g>", GOFF = "</g>";		// green
	static final String BON = "<b1>", BOFF = "</b1>";	// blue
	static final String RON = "<r>", ROFF = "</r>";		// red
	static final String BOLDON = "<b>", BOLDOFF = "</b>"; // bold
	// used to differentiate an "On" from a "full colors" cell-value
	static final String ON_MARKER = "$";
	// the heading for the promotions section in the html
	static final String PROMOTIONS_LABEL = "<u>Promotions</u>";
	static final String CONTRADICTION_LABEL = "<u>Contradiction</u>";
	static final String ELIMINATIONS_LABEL = "<u>Eliminations</u>";
	static final String CONFIRMATION_LABEL = "<u>Confirmation</u>";
	static final String MONOBOXS_LABEL = NL+"<u>Mono Boxs</u>"+NL;
	// bungy jumpers should go to the dentists instead
	static final String COMMA_SO = ", so ";
	static final String HAS = " has ";
	static final String MULTIPLE = "multiple ";
	static final String A = "a ";
	static final String IS = " is ";
	static final String ONLY = " only ";
	static final String LEAVES = " leaves ";
	static final String COMMA_WHICH_LEAVES = ", which leaves ";
	static final String PRODUCES = " => ";
	static final String ALL = " all ";
	static final String CANT_BE = " cannot be ";
	static final String HAS_BOTH = " has both ";
	static final String ELIMINATING = " eliminating ";
	static final String SEE = " see ";
	static final String SEES_BOTH = " sees both ";
	static final String SEES_SP = "sees ";
	static final String AND_HAS = " and has ";
	static final String APOSTROPHE_S = "'s";
	static final String ALL_OTHER_VALUES = "all other values";
	static final String CONJUGATE_IN = " conjugate in ";
	static final String ONLY_OTHER_VALUE_IS = " only other value is ";
	// THESE END IN A PERIOD
	static final String IS_TRUE = " is true.";
	static final String MUST_BE_TRUE = " must be true.";
	static final String IS_ALL_TRUE = " is ALL true.";

	private Words(){} // never called

}
