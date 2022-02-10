/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.Frmt.dbl;
import java.util.LinkedList;
import java.util.List;

/**
 * The Difficulty enum is used only to generate Sudokus.
 * <p>
 * The Difficulty of a Sudoku Puzzle is closely aligned with the difficulty
 * field of the Tech(nique) enum, and there's a random smatter of "custom"
 * {@code getDifficulty()} methods in various {@code *Hint} classes.
 * All "custom" hints ADD difficult, they NEVER take it away, so a hinters
 * difficulty is always atleast a floor for the difficulty of hints that it
 * produces (most commonly ALL hints it produces ARE exactly that difficulty).
 * <p>
 * Each Difficulty category (ie this enum) is used to select/reject a puzzle
 * in the {@link diuf.sudoku.gui.GenerateDialog}. The rest of the code-base is
 * getting along fine without any Difficulty, and I personally think it'd be
 * good if it stayed that way, considering how much of an pain-in-the-ass
 * maintaining the Difficulty boundaries has turned out to be. This pain has
 * been mitigated by the adoption of the floor and ceiling Tech parameters to
 * my constructor, so that my min and max are automatically the {@code first}
 * and {@code last+1} Tech.difficulty.
 * <p>
 * IDFKA is ironic! It's an acronym inherited from Doom meaning "I Don't Know
 * ____ All" [About Nothing]. It's ironic because IDKFA puzzles are about as
 * rare as chickens lips, so producing one implies that one knows rather a lot
 * more than "____ All" about programming at least.
 * <p>
 * I'm not saying what FTL stands for, but If you don't already know then it's
 * a reasonable prediction that you're too stupid to solve them, so you should
 * probably just ignore there existence, and work on forgiving me for being a
 * insufferable smartass. I hope I hurt your feelings, because I sure as hell
 * can't solve them, without a computer, so I feel bad about it, so don't feel
 * too bad about it. IDKFA puzzles are REALLY REALLY hard, and so are REALLY
 * REALLY rare, so are REALLY REALLY hard to generate randomly. That's all that
 * really needs saying. Forgive me. I'm not an ___hole really, just a smartass.
 * <p>
 * Difficulty was exhumed to {@code diuf.sudoku} for public visibility.
 * Formerly Difficulty was an inner-class of the GenerateDialog.
 * <p>
 * <b>WARNING:</b> If you change any hints difficulty then also please:<ul>
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#LogicalSolver} to keep order of
 *   wantedHinters near to Tech.difficulty; so hinters are run in increasing
 *   difficulty, to produce the simplest possible solution to each puzzle.
 *  <li>{@link diuf.sudoku.Tech} // double check your difficulties
 *  <li>{@link diuf.sudoku.Difficulty} // double check the ranges
 * </ul>
 *
 * @author Keith Corlett 2018 Mar - was an inner class of GenerateDialog
 */
public enum Difficulty {

	//nb: do NOT static import Tech.* coz there names collide with hinters,
	// which seems to send generator, and possibly lambda's ____ing mental.
	//Name		(floor,					ceiling,				licence)
	  Easy		(null,					Tech.DirectNakedPair,	"Testing Testing 123")
	, Medium	(Tech.DirectNakedPair,	Tech.NakedPair,			"L Plates")
	, Hard		(Tech.NakedPair,		Tech.XYZ_Wing,			"P Plates")
	, Fiendish	(Tech.XYZ_Wing,			Tech.BigWings,			"Car Licence")
	, Airotic	(Tech.BigWings,			Tech.FinnedSwampfish,	"Air Licence")
	, AlsFish	(Tech.FinnedSwampfish,	Tech.AlignedPair,		"Alice fishes?")
	, Ligature	(Tech.AlignedPair,		Tech.UnaryChain,		"Aligned nonsense")
	, Diabolical(Tech.UnaryChain,		Tech.DynamicPlus,		"NASA Licence")
	, IDKFA		(Tech.DynamicPlus,		null,					"FTL Licence")
	;

	// Weird: Difficulty.values() reversed.
	public static Difficulty[] reverseDifficulties() {
		final Difficulty[] values = values();
		final int n = values.length;
		final Difficulty[] result = new Difficulty[n];
		for ( int i=0,m=n-1; i<n; ++i )
			result[i] = values[m-i];
		return result;
	}

	// get a list of Tech's from min (inclusive) to less than max (exclusive)
	public static List<Tech> techs(double min, double max) {
		final List<Tech> results = new LinkedList<>();
		for ( Tech tech : Tech.values() )
			if ( tech.difficulty>=min && tech.difficulty<max )
				results.add(tech);
		return results;
	}

	// return a CSV String of the names of these 'techs'
	public static String names(List<Tech> techs) {
		final StringBuilder sb = new StringBuilder(techs.size()*22);
		for ( Tech tech : techs ) {
			if ( sb.length() > 0 )
				sb.append(CSP);
			sb.append(tech.name());
		}
		return sb.toString();
	}

	/**
	 * get the Difficulty for this target maximum-difficulty-of-a-puzzle.
	 *
	 * @param targetMaxD the maximum of the hint difficulties in this puzzle. <br>
	 *  If you pass me a targetMaxD >= 100.0D then I return IDKFA (hardest).
	 * @return the lowest Difficulty whose max exceeds targetMaxD
	 */
	public static Difficulty get(double targetMaxD) {
		for ( Difficulty d : values() )
			if ( targetMaxD < d.max )
				return d;
		return IDKFA;
	}

	public static boolean isaFloor(Tech targetTech) {
		for ( Difficulty d : values() )
			if ( d.floor == targetTech )
				return true;
		return false;
	}

	/**
	 * starting (floor) Tech. (WARN: null for Easy).
	 */
	private final Tech floor;
	/**
	 * minimum (floor) difficulty.
	 */
	public final double min;
	/**
	 * maximum (ceiling) difficulty.
	 */
	public final double max;
	/**
	 * just some silly text, to lighten the mood, and also a brain-hook.
	 */
	public final String licence;

	private Difficulty(final Tech floor, final Tech ceil, final String licence) {
		this.floor = floor; // nullable
		this.min = floor!=null ? floor.difficulty : 1.0; // Easy
		this.max = ceil!=null ? ceil.difficulty : 100.0; // IDKFA
		this.licence = licence;
	}

	/**
	 * get techs in this Difficulty.
	 *
	 * @return an array of Tech that are contained in this Difficulty.
	 */
	public Tech[] techs() {
		return Tech.array(techs(min, max));
	}

	/**
	 * Get HTML describing this Difficulty.
	 * <p>
	 * The HTML string is cached, which is safe because Difficulty is invariant
	 * by design, that is, all fields are immutable, ergo nothing changes. If
	 * you introduce a mutable field then firstly don't, and secondly get rid
	 * of the cache for HTML, and also blow your nose REALLY hard and then
	 * double-check your hanky for s__t. Just don't. OK?
	 *
	 * @return HTML describing this Difficulty
	 */
	public String getHtml() {
		if ( html == null )
			html = "<html><body>"+NL
				+"<b>"+ordinal()+" "+name()+"</b>: "+names(techs(min, max))+NL
				+"<p><b>Rating</b>: "+dbl(min)+" - "+dbl(max)+" ["+licence+"]"+NL
				+"</body></html>"+NL;
		return html;
	}
	private String html;

}
