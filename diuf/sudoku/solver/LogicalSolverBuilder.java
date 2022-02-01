/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Run;
import diuf.sudoku.Settings;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import static diuf.sudoku.solver.LogicalSolver.constructHinter;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.align2.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.solver.hinters.lock2.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/**
 * LogicalSolverBuilder builds a new LogicalSolver, which got a bit messy, so
 * LogicalSolver's constructor is it's own class, to keep it's s__t together,
 * and allow LogicalSolver to concentrate on logical solving, and not on
 * constructing its-bloody-self. sigh.
 *
 * @author Keith Corlett 2021-09-01
 */
class LogicalSolverBuilder {

	/**
	 * true uses the new align2 package (development only). align2 eradicates
	 * all that boiler-plate code, but it's much slower for 5+ aligned sets.
	 */
	private static final boolean USE_NEW_ALIGN2 = false; //@check false

	/**
	 * If the user wants each Tech then its used to solve puzzles, else it's
	 * not used; but it's hinter may be created anyway then lost. sigh.
	 */
	private EnumSet<Tech> wantedTechs;

	/**
	 * wanted is a List of IHinter, to which the Constructor adds hinters using
	 * the various "want" methods. The list is then read into the wantedHinters
	 * array, which is used everywhere-else. Note that 'wanted' is a field only
	 * so that it can be populated by the various "want" methods, without
	 * passing it around everywhere.
	 */
	private List<IHinter> wanted;

	/**
	 * The various want methods populate 'unwanted' Techs, for later printing.
	 * This is just a way of telling the user what they don't want. sigh.
	 */
	private final EnumSet<Tech> unwanted = EnumSet.noneOf(Tech.class);

	/**
	 * My clone of THE_SETTINGS, set in the constructor, and read elsewhere.
	 */
	private Settings settings;

	/**
	 * A set of "basic hinters" (the Four Quick Foxes, et al).
	 */
	private final EnumMap<Tech,IHinter> basics;

	public LogicalSolverBuilder() {
		// populate my EnumMap of "basic hinters" Tech=>IHinter
		// I create the "basic" hinters even if they're not wanted,
		// ergo I presume that these hinters will always be wanted.
		this.basics = BasicHintersBuilder.build();
	}

	/**
	 * build is used <u>ONLY</u> by {@link LogicalSolverFactory}, so there's
	 * only ONE LogicalSolver in existence at any one time, hence I am only
	 * package visible. The build method was the LogicalSolver constructor,
	 * but then I created LogicalSolverBuilder to shift all of the complexity
	 * here-in into it's own class, so that the LogicalSolver could concentrate
	 * on solving, by delegating its set-up to me.
	 * <p>
	 * Primarily I populate the wantedHinters array, for use by solve (et al).
	 * I do this using a List of wantedHinters called 'wanted', which is read
	 * into an array when the LogicalSolver is constructed, ie at the end. The
	 * 'wanted' field is a field only for access by various "want" methods,
	 * without passing it around everywhere.
	 * <p>
	 * Sudoku Explainers goal is to explain (and measure the difficulty of)
	 * Sudoku puzzles, as quickly as possible; which in reality means "as
	 * quickly as I'm able". The key to finding the simplest solution to a
	 * puzzle is to apply the simplest possible hint at each step, so I add
	 * hinters in (approximate) increasing order of complexity.
	 * <p>
	 * <u>Hinter Selection</u>:<br>
	 * NakedSingle and HiddenSingle are hardcoded because they're required.
	 * The user selects techs in the GUI which changes THE_SETTINGS read into
	 * the wantedTechs field, which is read by the various 'want' methods. Each
	 * wanted Tech has it's implementing IHinter added to the 'wanted' list.
	 * At the end the 'wanted' list is read into the wantedHinters array, which
	 * is used everywhere else. Unwanted techs are added to 'unwanted' Set for
	 * later reporting.
	 * <p>
	 * <u>Execution Order</u>:<br>
	 * The solve (et al) methods iterate the wantedHinters array, invoking
	 * hinters in the order they're added here, which is approximately by
	 * Tech.difficulty ascending. Some hinters are called sooner than there
	 * Tech.difficulty dictates, because they're faster than "easier" ones,
	 * and so evidently are "easier" than those "easier" ones, but this is
	 * arbitrary and therefore contentious.
	 * <p>
	 * Producing the simplest possible solution to a Sudoku means finding the
	 * simplest possible hint at each step, but simple is an arbitrary concept.
	 * Nanos-per-elim is empirical-data, but it depends on the quality of the
	 * implementation of each technique, which is pretty bloody arbitrary.
	 * <p>
	 * I should also define "solve (et al)": et al means "and others", which in
	 * this context means the getFirstHint and getAllHints methods, that are
	 * used by the GUI (F5 and Shft-F5) to fetch the next hint. Solve-stepping
	 * through a puzzle in the GUI is roughly equivalent to solve, but only
	 * roughly. Unfortunately, there are many differences between GUI, batch,
	 * and test-cases; so your milage may vary significantly: one divergent
	 * elimination may reroute the whole subsequent hint-path, so caveat
	 * emptor: the GUI may diverge significantly from the batch.
	 * <pre>
	 * <u>Change Log</u>:
	 * KRC 2021-06-07 simplified configureHinters. A new want method takes a
	 * Tech, which now has className and passTech attributes which are used to
	 * look-up the constructor and construct the hinter only if Tech is wanted,
	 * so unwanted hinters are not constructed.
	 * LATER: I removed passTech: the contract now is each want(Tech)'d hinter
	 * has ONE ONLY constructor, to which the Tech is passed if required.
	 *
	 * configureHinters adds to the private 'wanted' list, which is then read
	 * into the public wantedHinters array, that's used everywhere-else because
	 * array-iterators are much faster than Iterators. This is about 30 seconds
	 * faster over top1465, which is "heaps" when you consider how simple/easy
	 * it is compared to making the hinters 30 seconds faster. sigh.
	 *
	 * This also saves creating a private array in the solve method, which was
	 * nuts when you consider that the array only need be created when Settings
	 * change and the LogicalSolver is recreated. Also removed the categories.
	 * They were a useless complication. This is cleaner, in my humble opinion.
	 *
	 * KRC 2021-06-07 configureHinters has become the only constructor, coz it
	 * is now the only place configureHinters was used. The LogicalSolver now
	 * keeps a private snapshot of THE_SETTINGS. And the constructor does NOT
	 * log anything, instead it exposes logging-methods, to drop the spurious
	 * log-messages from the test-cases.
	 *
	 * KRC 2021-08-25 wantChainer creates chainer only if this Tech is wanted.
	 * It's more efficient because chainer construction is a heavy process.
	 * Each constructor ran just once, but it was still obese overall. I also
	 * moved "the extra Grid" out of chainer-constructors, so they're lighter.
	 *
	 * KRC 2021-09-01 moved LogicalSolver contructor into this build method of
	 * the new LogicalSolverBuilder class, which now encapsulates all of the
	 * complexities of building a new LogicalSolver, so that LogicalSolver can
	 * concentrate on logical solving, and not setting-itself-up for solving.
	 * I hope that this design is easier to follow.
	 * </pre>
	 * <p>
	 * See the class documentation for more discussion.
	 */
	LogicalSolver build() {
		// We add hinters to the wantedHintersList roughly by complexity, ergo
		// complexity is rising, so the order in which hinters are created is
		// the order in which they execute, which is roughly Tech.difficulty
		// ASCENDING, but some hinters are run before there difficulty dicates,
		// because they're actually faster than some "easier" ones.
		//
		// Ideally, we'd create hinters by Tech.difficulty ASCENDING, and each
		// Tech has one implementation, and each hinter implements one Tech,
		// but in reality some Techs have two impls, some hinters handle many
		// Techs, and some hinters run before there Tech.difficulty demands.
		// Reality gets complicated. Accept it, and lean into it.
		//
		// 99% of Sudokus do not require chains, 99% of the rest solve with
		// DynamicChains, 99% of the rest solve with DynamicPlus, and finally
		// ALL puzzles solve with NestedUnary, so just 0.001% of Sudoku puzzles
		// are the real challenge. SE is algorithmic ballet: Juillerat (and
		// hobiwan, et al) provided the algorithms, all I did was the ballet.
		// Shine On You Crazy Diamond! Don't Stop [Thinking About Tommorrow].

		// take a private (immutable) snapshot of THE_SETTINGS
		try {
			settings = (Settings)THE_SETTINGS.clone();
		} catch ( CloneNotSupportedException ex ) {
			// Wrap the clone exception in a generic runtime exception. sigh.
			throw new RuntimeException("THE_SETTINGS.clone() failed", ex);
		}

		// fetch wantedTechs for my "want" methods
		wantedTechs = settings.getWantedTechs();

		// various want methods add hinters to the 'wanted' list, which is
		// then read into the 'wantedHinters' array for faster iteration.
		wanted = new ArrayList<>(wantedTechs.size());

		// the unwanted-techs, populated by want methods, for later reporting.
		unwanted.clear();

		// Easy
		if ( Run.type == Run.Type.GUI ) { // useless in batch and test-cases
			want(new LonelySingle()); // 1 maybe and last place in region
		}
		// naked and hidden singles are mandatory (the user can't unwant them)
		wanted.add(basics.get(Tech.NakedSingle)); // cell has 1 maybe
		wanted.add(basics.get(Tech.HiddenSingle)); // value has 1 place in region

		// Medium
		if ( Run.type == Run.Type.GUI ) { // useless in batch and test-cases
			want(Tech.DirectNakedPair);	   // 2 cells with 2 maybes -> single
			want(Tech.DirectHiddenPair);   // 2 places for 2 values -> single
			want(Tech.DirectNakedTriple);  // 3 cells with 3 maybes -> single
			want(Tech.DirectHiddenTriple); // 3 places for 3 values -> single
		}
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// From here down (unless stated otherwise) hinters just remove maybes,
		// setting cell values only indirectly.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Locking XOR LockingGen (mandatory: not neither, nor both).
		// nb: SiameseLocking is swapped-in for Locking, if wanted.
		// nb: The GUI and batch both use whichever is specified here.
		// nb: BruteForce always uses LockingSpeedMode, regardless.
		if ( !want(basics.get(Tech.Locking)) ) { // Pointing and Claiming
			wanted.add(new LockingGen()); // No differentiation (slower)
		}

		// Hard
		want(basics.get(Tech.NakedPair));	// 2 cells with only 2 maybes
		want(basics.get(Tech.HiddenPair));	// same 2 places for 2 values in a region
		want(basics.get(Tech.NakedTriple));	// 3 cells with only 3 maybes
		want(basics.get(Tech.HiddenTriple));	// same 3 places for 3 values in a region
		want(basics.get(Tech.Swampfish));	// same 2 places in 2 rows/cols
		want(Tech.TwoStringKite);	// box and 2 biplace regions
		want(Tech.W_Wing);			// biplace row/col and 2 bivalue cells
		want(Tech.XY_Wing);			// 3 bivalue cells
		want(Tech.Skyscraper);		// 2 biplace rows/cols get bent
		want(Tech.EmptyRectangle);	// box and biplace row/col

		// Fiendish
		want(Tech.XYZ_Wing);	// trivalue + 2 bivalues
		want(Tech.Swordfish);	// same 3 places in 3 rows/cols
		// do Naked/HiddenQuad before Jellyfish: 4 coincident lines
		want(Tech.NakedQuad);	// 4 cells with only 4 maybes
		want(Tech.Jellyfish);	// same 4 places in 4 rows/cols
		want(Tech.HiddenQuad);	// same 4 places for 4 values in a region
		// NB: Coloring/GEM find all Jellyfish, so run before them if at all.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// XColoring, Medusa3D, and GEM sometimes set cell values directly,
		// but most-times they just eliminate maybes.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		want(Tech.Coloring);	// 2 color-sets and the ONLY 3+ color-sets
		want(Tech.XColoring);	// 2 color-sets with cheese
		want(Tech.Medusa3D);	// 2 color-sets with cheese and pickles
		want(Tech.GEM);			// 2 color-sets with the lot, and bacon
		want(Tech.URT); // UniqueRectangle/loop: a rectangle/loop of 2 values

		// Air
		// BigWings XOR individual S/T/U/V/WXYZ-Wing@check
		if ( !want(Tech.BigWings) ) { // All the below BigWing (faster overall)
			want(Tech.WXYZ_Wing);	  // 3 cell ALS + bivalue cell
			want(Tech.VWXYZ_Wing);	  // 4 cell ALS + bivalue cell
			want(Tech.UVWXYZ_Wing);	  // 5 cell ALS + bivalue cell
			want(Tech.TUVWXYZ_Wing);  // 6 cell ALS + bivalue cell
			want(Tech.STUVWXYZ_Wing); // 7 cell ALS + bivalue cell
		}
		// nb: BigWings gets alss that DB reuses (filtered to !allowNakedSets)
		want(Tech.NakedPent);  // DEGENERATE 5 values in 5 cells
		want(Tech.HiddenPent); // DEGENERATE 5 places for 5 values in region

		// Fish/ALS
		want(Tech.FinnedSwampfish); // includes Sashimi's
		want(Tech.FinnedSwordfish); // includes Sashimi's
		want(Tech.FinnedJellyfish); // SLOW (just)
		want(Tech.DeathBlossom); // stem of 2/3 maybes + 2/3 ALSs
		want(Tech.ALS_XZ);		 // a chain of 2 ALSs, with cheese
		want(Tech.ALS_Wing);	 // a chain of 3 ALSs, with pickles
		want(Tech.ALS_Chain);	 // a chain of 4..26 ALSs
		want(Tech.SueDeCoq);	 // Almost Almost Locked Sets (Sigh)
		// These complex Fish are nasty. Don't use Jellyfish, esp nasty ones!
		want(Tech.FrankenSwampfish);	 // NONE (DEGENATE to Finned, WTF?)
		want(Tech.FrankenSwordfish);	 // OK
		want(Tech.FrankenJellyfish);	 // SLOW (just)
		// Krakens & Mutants interleaved: Swamp, Sword, Jelly; for speed
		want(Tech.KrakenSwampfish);	// SLOW (28 seconds)
		want(Tech.MutantSwampfish);	// NONE
		want(Tech.KrakenSwordfish);	// TOO SLOW (84 seconds)
		want(Tech.MutantSwordfish);	// SLOW (39 seconds)
		want(Tech.KrakenJellyfish);	// WAY TOO SLOW (8 minutes)
		want(Tech.MutantJellyfish);	// WAY TOO SLOW (7 minutes)
		// Aligned Ally: One up from Diagon Ally.
		// the new align2 package is faster than the old align for A234E.
		wantAE(true, Tech.AlignedPair);		// OK, but none in top1465
		wantAE(true, Tech.AlignedTriple);	// OK
		wantAE(true, Tech.AlignedQuad);		// a tad slow
		// align2 takes 3*A5E, 4*A6E, 5*A7E, presume so on for 8, 9, 10.
		// the user chooses if A5+E is correct (slow) or hacked (faster).
		// Hacked searches sets aligned around two-or-more excluders, finding
		// about a third of the hints in about a tenth of the time.
		wantAE(USE_NEW_ALIGN2, Tech.AlignedPent); // a bit slow
		wantAE(USE_NEW_ALIGN2, Tech.AlignedHex);  // SLOW
		wantAE(USE_NEW_ALIGN2, Tech.AlignedSept); // TOO SLOW
		// really large Aligned sets (8+) are too slow to be allowed
		wantAE(USE_NEW_ALIGN2, Tech.AlignedOct);	  // FAR TOO SLOW
		wantAE(USE_NEW_ALIGN2, Tech.AlignedNona); // WAY TOO SLOW
		wantAE(USE_NEW_ALIGN2, Tech.AlignedDec);	  // CONSERVATIVE

		// IDKFA starts at DynamicPlus
		// * Diabolical needs a nasty-Fish, or an A*E, or a Unary/Nishio/
		//   Multiple/Dynamic chain, which are all "common enough"; but
		// * IDKFA needs a DynamicPlus/NestedUnary/Multiple/Dynamic/Plus which
		//   are "rare as rockin horse s__t" hence the ironic Difficulty name.
		//   If you solve ONE of these manually you definately know more than
		//   eff-all about solving Sudokus, and probably other things besides.
		//   Solving one of these manually may take years.
		want(Tech.UnaryChain);
		for ( Tech tech : Tech.MULTI_CHAINER_TECHS ) {
			if ( wantedTechs.contains(tech) ) {
				wanted.add(new ChainerMulti(tech, basics));
			} else {
				unwanted.add(tech);
			}
		}

		// return a new LogicalSolver
		final IHinter[] array = wanted.toArray(new IHinter[wanted.size()]);
		return new LogicalSolver(basics, array, unwanted);
	}

	/**
	 * A better name would be addHinterIfWanted but that's just too verbose.
	 * If the given tech is wanted then I instantiate it's className, passing
	 * the tech if it's only constructor accepts it.
	 * <p>
	 * Some Tech have a null className, in which case you will need to supply
	 * the className (the IHinter that is the default implementation of Tech),
	 * or just instantiate any-non-conforming-hinter manually, and pass it to
	 * {@link #want(IHinter) the other want method}, but note that the hinter
	 * will then be constructed even if it's not wanted, which I dislike, so
	 * avoid if possible.
	 * <p>
	 * Programmers don't want to write code. They want to write the code which
	 * writes the code, and that's where the trouble started. Compiler anyone?
	 *
	 * @param tech the Tech to add to wantedHinters, if the user wants it.
	 * @throws IllegalArgumentException, RuntimeException
	 */
	private boolean want(final Tech tech) {
		final boolean result;
		if ( result=wantedTechs.contains(tech) ) {
			// createHinterFor throws upon failure, so NEVER null.
			wanted.add(createHinterFor(tech));
		} else {
			unwanted.add(tech);
		}
		return result;
	}

	// create a new instance of the registered implementation of 'tech', ie the
	// IHinter whose full-class-name is tech.className.
	// NB: This method wraps constructHinter to throw the required exceptions.
	// @throws IllegalArgumentException if tech has no hinter defined
	//       , IllegalStateException if I fail to construct the hinter
	// but never returns null!
	private IHinter createHinterFor(final Tech tech) {
		if ( tech.className==null || tech.className.isEmpty() ) {
			// This'll only happen to noobs, but it happens to noobs!
			// So let's supply a nice detailed error-message, which still
			// leaves them scratching there heads. How to!?!? How to!?!?
			throw new IllegalArgumentException(Log.me()
			+" cannot instantiate the class implementing Tech "+tech.name()
			+" because its className attribute (the full-name of the"
			+" implementing class) is null/empty. Supply the className and"
			+" passTech in the Tech definition, or instantiate the hinter"
			+" manually and pass that to the other want method.");
		}
		final IHinter hinter = LogicalSolver.constructHinter(tech);
		if ( hinter == null ) {
			throw new IllegalStateException(
					Log.me()+": "+tech+"->"+tech.className);
		}
		return hinter;
	}

	/**
	 * A better name would be addThisHinterIfItsWanted but it's too long.
	 * If hinter.getTech() is wanted then add it to the wantedHintersList;
	 * otherwise add the tech to the unwatedTechs list, for printing
	 * once we're done with all these bloody want methods.
	 * <p>
	 * NOTE: "simple" hinters use {@link #want(Tech) the other want method},
	 * where this one is used for both "core" hinters (the "Four Quick Foxes")
	 * and the "complex" hinters, such as Aligned*Exclusion and Chainers.
	 *
	 * @param hinter to want, or not.
	 */
	private boolean want(final IHinter hinter) {
		final boolean result;
		final Tech tech = hinter.getTech();
		if ( result=wantedTechs.contains(tech) ) {
			wanted.add(hinter);
		} else {
			unwanted.add(tech);
		}
		return result;
	}

	/**
	 * wantAlignedExclusion sees if the given Tech is wanted by the user. If so
	 * it creates an instance of an IHinter which implements the given Aligned
	 * Exclusion tech, and adds it to the wantedHintersList. If not then this
	 * Tech is added to unwatedTechs for later printing.
	 * <p>
	 * Two packages implement AlignedExclusion: The old align package is a mess
	 * of fast boiler-plate code. The new align2 package is cleaner but slower.
	 * The new one is actually faster for A234E, but takes about thrice A5E,
	 * four times A6E, five times A7E, and and presumably so on for A8910E,
	 * which are already too slow, so I haven't even bothered to time them.
	 * <p>
	 * The "registered" Tech.className for AE is the "old" align package class,
	 * that is still used for A5678910E (sigh), while A234E uses the new one.
	 * <p>
	 * NOTE that all Aligned*Exclusion are NOT wanted by default because they
	 * are all still, despite my bestest efforts, <b>TOO BLOODY SLOW</b>! What
	 * is actually required, if we are ever going to actually use AE, is a
	 * completely new and much faster way of finding in/valid combos, but I am
	 * simply too stupid to imagineer it.
	 *
	 * @param useNewAlign2 use the new "align2" package, or the old "align"
	 *  package; A234E are always true (new), and for A5678910E pass me the
	 *  value of the USE_NEW_ALIGN2 constant, which is currently false (old)
	 * @param tech one of the Aligned* Tech's (I validate isAligned, ergo
	 *  {@code tech.name().startsWith("Aligned")})
	 * @throws IllegalArgumentException if the given tech is unknown
	 * @throws IllegalStateException if the hinter cannot be constructed
	 */
	private boolean wantAE(final boolean useNewAlign2, final Tech tech) {
		assert tech.isAligned;
		if ( useNewAlign2 ) {
			// new AE constructor reads "isa${degree}ehacked" Setting itself.
			// There is no more class per size and no more _1C/_2H versions,
			// eliminating all that "boiler-plate" code, but the new version is
			// MUCH slower for A5E upwards, only getting worse as N grows, so I
			// reckon the new A10E correct would run for 8-10 days (I haven't
			// tried it) which makes me feel stupit, mainly because I am. sigh.
			return want(new AlignedExclusion(tech));
		}
		switch ( tech ) {
			// there is no _2H hacked version of A234E
			case AlignedPair:
			case AlignedTriple:
			case AlignedQuad:
				// the registered Impl's are the old-school ones!
				return want(tech);
			// user chooses if A5..10E is _2H hacked or _1C correct
			case AlignedPent:
			case AlignedHex:
			case AlignedSept:
			case AlignedOct:
			case AlignedNona:
			case AlignedDec:
				if ( settings.getBoolean(tech.name(), tech.defaultWanted) ) {
					// registered Tech for A5+E are the old-school _2H ones
					String className = tech.className; // _2H
					// the "registered" impl is the default _2H hacked version,
					// so read "isa${degree}ehacked" and swap over to the _1C
					// correct version if that's what the user wants, but the
					// default for all A5+E is true (hacked), else too slow.
					final String setting = "isa"+tech.degree+"ehacked";
					if ( !settings.getBoolean(setting, true) ) {
						className = className.replaceFirst("_2H", "_1C");
					}
					final IHinter hinter = constructHinter(tech, className);
					if ( hinter == null ) {
						throw new IllegalStateException(
							Log.me()+": null hinter: "+tech+"->"+className);
					}
					wanted.add(hinter);
					return true;
				} else {
					unwanted.add(tech);
					return false;
				}
			default:
				throw new IllegalArgumentException("Unknown tech: "+tech);
		}
	}

}
