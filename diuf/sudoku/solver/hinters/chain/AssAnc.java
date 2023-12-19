/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Idx.MASKED;
import static diuf.sudoku.Idx.THREE;

/**
 * AssAnc is an Ass(umption) with ancestors, an array of exploded Idxs of all
 * assumptions that must be true in order for this assumption to exist. Note
 * that ancestors ignores parent.isOn by design.
 * <p>
 * The AssAnc class exists because only ChainerUnary uses ancestors (the other
 * chainers do not) so I want a way of providing an additional ancestors field
 * to ChainerUnary only. Extending Ass is the only way, AFAIK.
 *
 * @author Keith Corlett 2023-09-04
 */
public class AssAnc extends Ass {

	// Idx now demurs from ancestors. ancestors remains 3 * 27bit ints, whereas
	// Idx is now 54bit long + 27bit int, hence AssAnc needs its own "MASKOF".
	/**
	 * AncestorMaskOf: part-index of an 27bit-Idx-element in {@link ancestors}.
	 * {@code if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) }
	 */
	public static final int[] ANCMASKOF = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
	};

	/**
	 * AncestorValueBase: part-index of an 27bit-Idx-element in {@link ancestors}.
	 * {@code if ( (ancestors[ANCVBASE[value]+ANCMASKOF[indice]] & MASKED[indice]) < 1 ) }
	 */
	public static final int[] ANCVBASE = new int[VALUE_CEILING];
	static {
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			ANCVBASE[v] = (v-1) * THREE;
		}
	}

	/**
	 * An array of 27bit-Idx-elements.
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * // if ass DOES NOT have ancestor {value,indice}
	 * if ( (ass.ancestors[ANCVBASE[value]+ANCMASKOF[indice]] & MASKED[indice]) < 1 )
	 * }</pre>
	 * <p>
	 * Note that ancestors ignores parent.isOn (parity) by design. I just need
	 * to know if this effect has ANY ancestor with {value,indice}. I care not
	 * for the parity (isOn-ness) of that ancestor, and this is more compact.
	 */
	public final int[] ancestors;

	/**
	 * Constructor for the initial assumption, which has no ancestors.
	 *
	 * @param indice
	 * @param value
	 * @param isOn
	 */
	public AssAnc(int indice, int value, boolean isOn) {
		super(indice, value, isOn);
		ancestors = new int[27];
	}

	/**
	 * Constructor for all subsequent assumptions, where the parent is NEVER
	 * null, and my ancestors are my parents ancestors plus my parent.
	 *
	 * @param indice
	 * @param value
	 * @param isOn
	 * @param parent
	 * @param cause
	 * @param explanation
	 */
	public AssAnc(final int indice, final int value, final boolean isOn
		, final AssAnc parent, final Cause cause, final String explanation) {
		super(indice, value, isOn, parent, cause, explanation);
		this.ancestors = parent.ancestors.clone();
		this.ancestors[ANCVBASE[parent.value]+ANCMASKOF[parent.indice]] |= MASKED[parent.indice];
	}

}
