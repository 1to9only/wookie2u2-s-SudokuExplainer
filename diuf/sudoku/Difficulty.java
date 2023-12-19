/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.Frmt.lng;
import java.util.EnumSet;

/**
 * The Difficulty enum is used only to generate Sudokus.
 * <p>
 * The Difficulty of a Sudoku Puzzle is closely aligned with the difficulty
 * field of the Tech(nique) enum, and theres a random smatter of "custom"
 * {@code getDifficulty()} methods in various {@code *Hint} classes.
 * All "custom" hints ADD difficult, they NEVER take it away, so a hinters
 * difficulty is always atleast a floor for the difficulty of hints that it
 * produces (most commonly ALL hints it produces ARE exactly that difficulty).
 * Every hint guarantees to NEVER boost its difficulty beyond its Difficulty
 * maximum boundary. The boost the Nested chainers is nominally unbounded.
 * Thats why IDKFAs top-end is so blown out (100).
 * <p>
 * Each Difficulty category (ie this enum) is used to select/reject a puzzle
 * in the {@link diuf.sudoku.gui.GenerateDialog}. The rest of the code-base is
 * getting along fine without any Difficulty, and I personally think it would
 * be good if it stayed that way, considering how much of an pain-in-the-ass
 * maintaining the Difficulty boundaries has turned out to be. This pain has
 * been mitigated by the adoption of the floor and ceiling Tech parameters to
 * my constructor, so that my min and max are automatically the {@code first}
 * and {@code last+1} Tech.difficulty.
 * <p>
 * IDFKA is ironic! Its an acronym inherited from Doom meaning "I Dont Know
 * ____ All" [About Nothing]. Its ironic because IDKFA puzzles are about as
 * rare as chickens lips, so producing one implies that one knows rather a lot
 * more than "____ All" about programming at least, if not nothing.
 * <p>
 * I am not saying what FTL stands for, but If you dont already know then its
 * a reasonable prediction that you are too stupid to solve them, so you should
 * probably just ignore there existence, and work on forgiving me for being a
 * insufferable smartass. I hope I hurt your feelings, because I sure as hell
 * cant solve them, without a computer, so I feel bad about it, so dont feel
 * bad about it. IDKFA puzzles are REALLY REALLY hard, and so are REALLY REALLY
 * rare, so are REALLY REALLY hard to generate randomly. Thats all that really
 * needs saying. Forgive me. I am not an ___hole really, just a sexual organ!
 * <p>
 * Difficulty was exhumed to {@code diuf.sudoku} for public visibility.
 * Formerly Difficulty was an inner-class of the GenerateDialog.
 * <p>
 * <b>WARN:</b> If you change any hints difficulty then also please:<ul>
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

	// WARN: Dont static import Tech.* coz some names collide with hinters,
	// sending the generator (and lambdas) ____ing mental. Sometimes changes
	// are NOT detected by Netbeans, but java.exe always sees changes.
	// To deal with this issue: Clean and build, then run.
	// Still no work: delete C:/Users/User/AppData/Local/NetBeans/Cache/8.2,
	// clean and build, then run.
	//Name		(floor,					ceiling,				licence)
	  Easy		(null,					Tech.DirectNakedPair,	"First Try")
	, Medium	(Tech.DirectNakedPair,	Tech.NakedPair,			"L Plates")
	, Hard		(Tech.NakedPair,		Tech.TwoStringKite,		"P Plates")
	, Fiendish	(Tech.TwoStringKite,		Tech.Swordfish,			"Open Licence")
	, Nasty		(Tech.Swordfish,			Tech.BigWings,			"CAMS Licence")
	, Alsic		(Tech.BigWings,			Tech.FinnedSwampfish,	"Pilots Licence")
	, Fisch		(Tech.FinnedSwampfish,	Tech.AlignedPair,		"What Fisch?")
	, Ligature	(Tech.AlignedPair,		Tech.TableChain,		"Just shoot me")
	, Diabolical(Tech.TableChain,		Tech.DynamicPlus,		"Space Cadet!")
	, IDKFA		(Tech.DynamicPlus,		null,					"Bistromathics")
	;

	// Weird: Difficulty.values() reversed (hardest to easyest).
	public static Difficulty[] reverseValues() {
		final Difficulty[] values = values();
		final int n = values.length;
		final Difficulty[] result = new Difficulty[n];
		for ( int i=0,m=n-1; i<n; ++i )
			result[i] = values[m-i];
		return result;
	}

	/**
	 * get the Difficulty for this target maximum-difficulty-of-a-puzzle.
	 *
	 * @param targetMaxD the maximum of the hint difficulties in this puzzle. <br>
	 *  If you pass me a targetMaxD >= 100.0D then I return IDKFA (hardest).
	 * @return the lowest Difficulty whose max exceeds targetMaxD
	 */
	public static Difficulty get(final int targetMaxD) {
		for ( Difficulty d : values() )
			if ( targetMaxD < d.max )
				return d;
		return IDKFA;
	}

	public static boolean isaFloor(final Tech target) {
		for ( Difficulty d : values() )
			if ( d.floor == target )
				return true;
		return false;
	}

	// ---------------------------- instance stuff ---------------------------

	/**
	 * starting (floor) Tech. (WARN: null for Easy).
	 */
	private final Tech floor;

	/**
	 * minimum (floor) difficulty.
	 */
	public final int min;

	/**
	 * maximum (ceiling) difficulty.
	 */
	public final int max;

	/**
	 * just some silly text, to lighten the mood, and also a brain-hook.
	 */
	public final String licence;

	/**
	 * cache my html
	 */
	private String html;

	private Difficulty(final Tech floor, final Tech ceil, final String licence) {
		this.floor = floor; // nullable
		this.min = floor!=null ? floor.difficulty : 1; // Easy
		this.max = ceil!=null ? ceil.difficulty : 1000; // IDKFA
		this.licence = licence;
	}

	/**
	 * Get the techs in this Difficulty.
	 *
	 * @return a new {@code EnumSet<Tech>} of techs in this Difficulty.
	 */
	public EnumSet<Tech> getTechs() {
		return Tech.byDifficulty(min, max);
	}

	/**
	 * Get HTML describing this Difficulty.
	 * <p>
	 * The HTML string is cached, which is safe because Difficulty is invariant
	 * by design, that is, all fields are immutable, ergo nothing changes. If
	 * you introduce a mutable field then firstly do not, and secondly get rid
	 * of the cache for HTML, and also blow your nose REALLY hard and then
	 * double-check your hanky for s__t. Just do not. OK?
	 *
	 * @return HTML describing this Difficulty
	 */
	public String getHtml() {
		if ( html == null ) {
			html = "<html><body>"+NL
				+"<b>"+ordinal()+" "+name()+"</b>: "+Tech.names(getTechs())+NL
				+"<p><b>Rating</b>: "+lng(min)+" - "+lng(max)+" ["+licence+"]"+NL
				+"</body></html>"+NL;
		}
		return html;
	}

}
