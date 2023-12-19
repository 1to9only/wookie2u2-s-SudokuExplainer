/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Config;
import static diuf.sudoku.Config.CFG;
import diuf.sudoku.solver.hinters.lock.LockingGen;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.chain.*;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.lang.reflect.Method;

/**
 * Normally one calls {@link LogicalSolverFactory#get()} to get the existing
 * LogicalSolver, not roll-your-own. LogicalSolver is a persistent (heavy with
 * solvers) multi-use instance, ie an instance solves many puzzles. The only
 * place that LogicalSolver is reconstructed is in the GUI when the wantedTechs
 * have changed.
 * <p>
 * LogicalSolverBuilder builds a new LogicalSolver, which got complicated, so
 * it became the LogicalSolverBuilder class, to keep it all together, and out
 * of LogicalSolver; so LogicalSolver concentrates on logical solving, and not
 * on putting itself together.
 * <p>
 * LogicalSolverBuilder is designed as a single-use disposable factory; so the
 * LogicalSolverFactory does
 * {@code LogicalSolver solver = new LogicalSolverBuilder().build();}
 * to create, use, and immediately dispose of the LogicalSolverBuilder.
 * <p>
 * LogicalSolverBuilder has two 'want' methods. The first is "normal", taking
 * an IHinter (an instance of a class implementing IHinter to implement a Tech)
 * and adds it to wantedHinters if Tech is wanted. The second 'want' method
 * is a bit trickier, if Tech is wanted then construct its implementation via
 * reflections, and add it to wantedHinters. To avoid constructing hinters
 * (which may be a heavy process) that arent wanted, so wont be used. Tech is a
 * lightweight-place-holder for a hinter.
 * <p>
 * Most "simple" hinters use {@code want(Tech)}, and most "complex" hinters are
 * created manually with {@code want(new ComplexHinter())}. The FiveQuickFoxes
 * {Locking, NakedPair, HiddenPair, TwoStringKite and Swampfish} are known as
 * "the basics". They are created individually, perversely with the "complex"
 * {@code want(IHinter)} method.
 * <p>
 * Be warned that I did all this because I can, and to avoid heavy construction
 * of hinters which are then not used. So caveat emptor: I did this for a want,
 * and for a reason, but my reasoning is pretty sloppy. Downside is "the basic"
 * hinters need a "standard" noargs or techOnly constructor (coz I am too thick
 * too implement constructHinter properly/completely). If you cannot do one
 * public constructor: no-args or Tech only, then this is a "complex" hinter
 * which you construct "manually".
 * <p>
 * The constructor of most subtypes of AHinter passes a Tech upto AHinter to
 * identify the implemented Sudoku solving technique, its degree, and "base"
 * difficulty, which is more predictable than actual hint Difficulty. Most
 * hinters implement one Tech, but a few (ChainerDynamic) implement multiple
 * Techs. The batch log lists both className and techName, to identify the
 * hinter.
 * <p>
 * So this is all pretty confusing, but it works (sort of), so do NOT "fix" it
 * until you understand it, ie when you dont really want-to fit it anymore.
 * Yes, its a cluster____; but its an organic cluster____, adapting to arising
 * complications. The problem with design is deciding stuff BEFORE you see the
 * consequences. Real solutions are evolutions. Refuse to over-design it.
 * Grow-a-pair and just let it evolve. Malice-aforethought!
 * <pre>
 * Design -&gt; Implement -&gt; Redesign -&gt; Reimplement... and so on.
 * More liberally: Illdesign -&gt; Misimplement -%lt; Resign -&gt; Cash!
 * </pre>
 *
 * @author Keith Corlett 2021-09-01
 */
public class LogicalSolverBuilder {

//	private static final boolean DEBUG = true;
//	private static void DEBUG(String msg) {
//		if(DEBUG)System.out.println(msg);
//	}

	/**
	 * Read the name of this method.
	 *
	 * @param ucpts means upperCleetenicPentagraphicTurtleScrotums, honest!
	 * @return Gopher Cleetus?
	 */
	private static String unknownConstructorParamTypesToString(final Iterable<Class<?>[]> ucpts) {
		assert ucpts != null;
		if ( ucpts == null ) {
			return "unknownConstructorParamTypes: null";
		}
		String result = "";
		try {
			// note the leading newLine
			for ( Iterator<Class<?>[]> it = ucpts.iterator(); it.hasNext(); ) {
				result += NL + Arrays.toString(it.next());
			}
		} catch (Exception ex) {
			result = "unknownConstructorParamTypes: "+ex;
		}
		return result;
	}

	/**
	 * Construct and return an instance of clazz (that implements Tech).
	 *
	 * @param tech to pass to the constructor, if it is required; else you can
	 *  pass null, but beware any error message might read badly.
	 * @param clazz which (presumably) implements Tech
	 * @param isNoisy when true (as by default) I log any exceptions;
	 *  when false I stay silent and simply return null
	 * @return a new instance of clazz.
	 */
	static IHinter constructHinter(final Tech tech, final Class<?> clazz, final boolean isNoisy) {
		try {
			// get the public constructors
			final Constructor<?>[] constructors = clazz.getConstructors();
			// no public constructors
			if ( constructors==null || constructors.length == 0 ) {
				// look for a factory method instead
				final Method[] methods = clazz.getDeclaredMethods();
				if ( tech == null ) {
					// look for factory(noArgs).
					for ( final Method m : methods ) {
						if ( m.getReturnType() == clazz
						  && m.getParameterCount() == 0 ) {
							return (IHinter)m.invoke(m, (Object[])null);
						}
					}
					throw new IllegalArgumentException("tech="+tech+" clazz="+clazz.getSimpleName()+" has no public constructors, nor a factory(noArgs) method. The tech parameter may be mandatory, but null was passed.");
				} else {
					// prefer factory(Tech) (a hinter may have both)
					for ( final Method m : methods ) {
						if ( m.getReturnType() == clazz
						  && m.getParameterCount() == 1
						  && m.getParameterTypes()[0] == Tech.class ) {
							return (IHinter)m.invoke(m, tech);
						}
					}
					// then factory(noArgs)
					for ( final Method m : methods ) {
						if ( m.getReturnType() == clazz
						  && m.getParameterCount() == 0 ) {
							return (IHinter)m.invoke(m, (Object[])null);
						}
					}
					throw new IllegalArgumentException("tech="+tech+" clazz="+clazz.getSimpleName()+" has no public constructors, nor a factory(Tech) method, nor a factory(noArgs) method.");
				}
			}
			// get and execute the first "known" constructor,
			// ie either noargs, or one Tech arg; else its "unknown"
			List<Class<?>[]> ucpts = null; // unknown constructor param types
			if ( tech == null ) {
				// constructor(noArgs)
				for ( final Constructor<?> c : constructors ) {
					final Class<?>[] pts = c.getParameterTypes();
					if ( pts.length == 0 ) {
						return (IHinter)c.newInstance();
					} else {
						if(ucpts==null) ucpts = new LinkedList<>();
						ucpts.add(pts);
					}
				}
			} else {
				// first constructor(tech)
				// then constructor(noArgs)
				if ( constructors.length > 1 ) {
					// order by number of parameters descending
					Arrays.sort(constructors, (Constructor<?> a, Constructor<?> b) ->
						b.getParameterTypes().length - a.getParameterTypes().length);
					System.out.println("INFO: Multiple "+tech+" constructors: "+Arrays.toString(constructors));
				}
				int i=0, n=constructors.length;
				for ( final Constructor<?> c : constructors ) {
					final Class<?>[] pts = c.getParameterTypes();
					if ( pts.length==1 && pts[0]==Tech.class ) {
						// if InstantiationException try next constr instead.
						// Hit when extends non-abstract parent hinter class;
						// Made constr protected but keep for future oopses!
						// It CAN work anyway, so it probably should.
						try {
							return (IHinter)c.newInstance(tech);
						} catch (InstantiationException ex) {
							if ( i == n-1 ) {
								throw ex;
							} // else try the other constructor
						}
					} else if ( pts.length == 0 ) {
						try {
							return (IHinter)c.newInstance();
						} catch (InstantiationException ex) {
							if ( i == n-1 ) {
								throw ex;
							} // else try the other constructor
						}
					} else {
						if ( ucpts == null ) {
							ucpts = new LinkedList<>();
						}
						ucpts.add(pts);
					}
					++i;
				}
			}
			// we should have found and executed a constructor and returned it
			// so to get here no "known" constructor was found, leaving the
			// "unknown constructors", coz all classes have constructors; they
			// may all be private, but they must exist. So spit the dummy if
			// isNoisy, else we simply return null.
			if ( isNoisy ) {
				throw new IllegalArgumentException(clazz.getCanonicalName()+" has unknown constructor parameter types: "+unknownConstructorParamTypesToString(ucpts));
			}
		} catch (IllegalAccessException | IllegalArgumentException | InstantiationException | SecurityException | InvocationTargetException ex) {
			if ( isNoisy ) {
				ex.printStackTrace(System.err);
			}
		}
		return null;
	}

	/**
	 * Construct and return an instance of clazz (that implements Tech).
	 *
	 * @param tech to pass to the constructor, if it is required; else you can
	 *  pass null, but beware any error message might read badly.
	 * @param clazz which (presumably) implements Tech
	 * @return a new instance of clazz.
	 */
	static IHinter constructHinter(final Tech tech, final Class<?> clazz) {
		return constructHinter(tech, clazz, true);
	}

	/**
	 * Constructs and returns a new instance of ${className} which implements
	 * the given 'tech', passing the 'tech' if the constructor takes it.
	 * <p>
	 * I am package visible for use by LogicalSolverBuilder. I am static because
	 * I do not reference the current instance, if any.
	 * <p>
	 * This method is design for use with a "simple" hinter, which has ONE ONLY
	 * constructor, which may take a Tech parameter. Anything more complex gets
	 * hard, so use {@link #want(IHinter) the other want method} instead.
	 * I just use the "first" constructor (according to reflections).
	 * <p>
	 * FYI: I am bound to simple hinters because I am too dumb to work-out how to
	 * choose between constructors with multiple parameter-types. It is possible
	 * to expand the definition of simple to other parameters, BUT before doing
	 * so be sure it is for many (not just one) hinters, and try to NOT pass it
	 * as a constructor parameter: often a simple setter works, even if its not
	 * good design. Most final fields do not actually NEED to be final. Can you
	 * add the wanted-parameter to the Tech enum?
	 *
	 * @param tech the Tech parameter to the constructor
	 * @param className the full name of the IHinter class to be constructed
	 * @return a new instance of ${className} which must implement IHinter
	 */
	static IHinter constructHinter(final Tech tech, final String className) {
		try {
			return constructHinter(tech, Class.forName(className));
		} catch (ClassNotFoundException ex) {
			Log.teeTrace("WARN: "+Log.me()+"("+tech+", "+className+") caught ", ex);
			return null;
		}
	}

	static IHinter constructHinter(final Tech tech) {
		return constructHinter(tech, tech.className);
	}

	// create a new instance of the registered implementation of 'tech',
	// ie the IHinter whose full-class-name is tech.className.
	// NB: This method wraps constructHinter to throw the required exceptions.
	// @throws IllegalArgumentException if tech has no hinter defined
	//       , IllegalStateException if I fail to construct the hinter
	// but never returns null!
	public static IHinter createHinterFor(final Tech tech) {
		if ( tech.className==null || tech.className.isEmpty() ) {
			// This'll only happen to noobs, but it happens to noobs!
			// So let us supply a nice detailed error-message, which still
			// leaves them scratching there heads. How to!?!? How to!?!?
			throw new IllegalArgumentException(Log.me()
			+": cannot instantiate the class implementing tech="+tech.name()
			+" because its className attribute (the full-name of the"
			+" implementing class) is null/empty. Tech.className is required"
			+" by the auto-constructor.");
		}
		final IHinter hinter = constructHinter(tech);
		if ( hinter == null ) {
			// Failed: root cause of problem has already been logged, now this
			// Tech will be added to the errors Set, for MORE reporting later.
			// So all I need do now is say which Tech/clazz is rooted.
			System.err.println("WARN: "+Log.me()+": Failed to construct class="+tech.className+" tech="+tech);
		}
		return hinter;
	}

	// ============================= instance land ============================

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
	 * This is just a way of telling the user what they do not want. sigh.
	 */
	private final EnumSet<Tech> unwanted = EnumSet.noneOf(Tech.class);

	/**
	 * errors is a Set of Techs constructHinter failed on.
	 */
	private final EnumSet<Tech> errors = EnumSet.noneOf(Tech.class);

	/**
	 * A set of "basic hinters" (the Four Quick Foxes, et al).
	 */
	private final EnumMap<Tech,IHinter> basics;

	public LogicalSolverBuilder() {
		// populate my EnumMap of "basic hinters" Tech=>IHinter
		// I create the "basic" hinters even if they are not wanted,
		// ergo I presume that these hinters will always be wanted.
		this.basics = BasicHintersBuilder.build();
	}

	/**
	 * Build is used <u>ONLY</u> by {@link LogicalSolverFactory}, so there is
	 * only ONE LogicalSolver in existence at any one time, hence I am only
	 * package visible. The build method was the LogicalSolver constructor,
	 * but then I created LogicalSolverBuilder to shift all of the complexity
	 * here-in into it is own class, so that the LogicalSolver concentrates
	 * on solving, by delegating its construction to me.
	 * <p>
	 * Primarily I populate the wantedHinters array, for use by solve (et al).
	 * I do this using a List of wantedHinters called 'wanted', which is read
	 * into an array when the LogicalSolver is constructed, ie at the end. The
	 * 'wanted' field is a field only for access by various "want" methods,
	 * without passing it around everywhere.
	 * <p>
	 * Sudoku Explainers goal is to explain (and measure the difficulty of)
	 * Sudoku puzzles, as quickly as possible; which in reality means "as
	 * quickly as I am able". The key to finding the simplest solution to a
	 * puzzle is to apply the simplest possible hint at each step, so I add
	 * hinters in (approximate) increasing order of complexity.
	 * <p>
	 * Sudokuists have varying ideas about "complexity" of some Techs. Thats
	 * fine. My basis for "complexity" is execution time, which is obviously
	 * dependant upon my skill as a programmer. I am pretty good, but I am NOT
	 * perfect. Consider yourself warned. Also there is a cluster of hinters
	 * (NastyFish and AlignedExclusion) after the ALS group, and before
	 * UnaryChains, that break this rule, and so are pretty much unusable: they
	 * take much longer than chaining, to find less hints. Chainers are FAST!
	 * <p>
	 * <u>Hinter Selection</u>:<br>
	 * NakedSingle and HiddenSingle are hardcoded because they are required.
	 * The user selects techs in the GUI which changes CFG read into the
	 * wantedTechs field, which is read by the various 'want' methods. Each
	 * wanted Tech has it is implementing IHinter added to the 'wanted' list.
	 * At the end the 'wanted' list is read into the wantedHinters array, which
	 * is used everywhere else. Unwanted techs are added to 'unwanted' Set for
	 * later reporting.
	 * <p>
	 * <u>Execution Order</u>:<br>
	 * The solve (et al) methods iterate the wantedHinters array, invoking
	 * hinters in the order they are added here, which is approximately by
	 * Tech.difficulty ascending. Some hinters are called sooner than there
	 * Tech.difficulty dictates, because they are faster than easier ones,
	 * and hence are evidently easier than the easier ones.
	 * <p>
	 * Producing the simplest possible solution to a Sudoku means finding the
	 * simplest possible hint at each step, but simple is an arbitrary concept.
	 * Nanos-per-elim is empirical-data, but it depends on the quality of the
	 * implementation of each technique, which is, sadly, pretty arbitrary.
	 * <p>
	 * So simplicity/complexity is an arbitrary measure, which is debated.
	 * Demurring to runtime is atleast empirical (ie has standard measure).
	 * This is just MY answer. It is imperfect, because I am imperfect.
	 * Mines faster than yours, nana-na-na. You have been warned.
	 * <p>
	 * Solve, in this context includes getAll/FirstHint methods, used by the
	 * GUI (F5 and Shft-F5) to fetch the next hint. Solve-stepping through a
	 * puzzle in the GUI is roughly equivalent to solve, but only roughly.
	 * There are many differences between GUI, batch, and test-cases; so your
	 * milage may vary significantly: one divergent elimination reroutes the
	 * whole subsequent hint-path; so caveat emptor: a GUI solve may diverge
	 * significantly from the batch, and from a test-case. Disabling "filter
	 * hints" in the GUI significantly mitigates this problem, listing hints
	 * in the order in which they were added ("natural order"), to try to
	 * follow the same hint-path as the batch; but it still diverges. Sigh.
	 * This is where the batch.log puzzles and the GUIs logView step in.
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
	 * into the public wantedHinters array, that is used everywhere-else because
	 * array-iterators are much faster than Iterators. This is about 30 seconds
	 * faster over top1465, which is "heaps" when you consider how simple/easy
	 * it is compared to making the hinters 30 seconds faster. sigh.
	 *
	 * This also saves creating a private array in the solve method, which was
	 * nuts when you consider that the array only need be created when Config
	 * change and the LogicalSolver is recreated. Also removed the categories.
	 * They were a useless complication. This is cleaner, in my humble opinion.
	 *
	 * KRC 2021-06-07 configureHinters has become the only constructor, coz it
	 * is now the only place configureHinters was used. The LogicalSolver now
	 * keeps a private snapshot of Config.CFG. And the constructor does NOT
	 * log anything, instead it exposes logging-methods, to drop the spurious
	 * log-messages from the test-cases.
	 *
	 * KRC 2021-08-25 wantChainer creates chainer only if this Tech is wanted.
	 * It is more efficient because chainer construction is a heavy process.
	 * Each constructor ran just once, but it was still obese overall. I also
	 * moved "the extra Grid" out of chainer-constructors, so they are lighter.
	 *
	 * KRC 2021-09-01 moved LogicalSolver contructor into this build method of
	 * the new LogicalSolverBuilder class, which now encapsulates all of the
	 * complexities of building a new LogicalSolver, so that LogicalSolver can
	 * concentrate on logical solving, and not setting-itself-up for solving.
	 * I hope that this design is easier to follow.
	 *
	 * KRC 2023-05-31 added the errors EnumSet. Also pass wantedHinters to
	 * LogicalSolver, because EnumSet.contains is convenient. Let it Bleed!
	 * </pre>
	 * <p>
	 * See the class documentation for more discussion.
	 * @return a new LogicalSolver
	 */
	public LogicalSolver build() {
		// Add hinters to "wanted" by Tech.difficulty ASCENDING. The order in
		// which hinters are created/added is the order in which they execute.
		//
		// Ideally each Tech has an implementation, and each hinter implements
		// a Tech. In practice some Techs have multiple impls, and some hinters
		// implement many Techs (esp ChainerDynamic). Reality gets complicated.
		//
		// 99% of Sudokus do not require chains, 99% of the rest solve with
		// DynamicChains, 99% of the rest solve with DynamicPlus, and finally
		// ALL puzzles solve with NestedUnary, that's just 0.001% of Sudokus.

		// fetch wantedTechs for my "want" methods, and setup the wanted list.
		// various want methods add hinters to wanted (a list), which is then
		// read into wantedHinters array for fast iteration in LogicalSolver.
		wanted = new ArrayList<>(isWantedTech());

		// the unwanted-techs, populated by want methods, for later reporting.
		unwanted.clear();

		// Easy
		if ( Run.isGui() ) // useless in batch and test-cases
			want(Tech.LonelySingle); // 1 maybe and last place in region
		// naked and hidden singles are mandatory (the user cannot unwant them)
		wanted.add(basics.get(Tech.NakedSingle)); // single maybe in cell
		wanted.add(basics.get(Tech.HiddenSingle)); // single place in region

		// Medium
		if ( Run.isGui() ) { // useless in batch and test-cases
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
		if ( !want(basics.get(Tech.Locking)) ) // Pointing and Claiming
			wanted.add(new LockingGen()); // No differentiation (slower)

		// Hard
		want(basics.get(Tech.NakedPair));	// 2 cells with only 2 maybes
		want(basics.get(Tech.HiddenPair));	// 2 places for 2 values in region
		want(basics.get(Tech.NakedTriple));	// 3 cells with only 3 maybes
		want(basics.get(Tech.HiddenTriple));	// 3 places for 3 values in region

		// Fiendish
		want(basics.get(Tech.TwoStringKite)); // box + 2 biplace rgns
		want(basics.get(Tech.Swampfish));	// same 2 places in 2 rows/cols
		want(Tech.W_Wing);			 // biplace row/col and 2 bivalue cells
		want(Tech.XY_Wing);			 // 3 bivalue cells
		want(Tech.XYZ_Wing);		 // trivalue + 2 bivalue cells
		want(Tech.Skyscraper);		 // 2 biplace rows/cols smoke cones
		want(Tech.EmptyRectangle);	 // biline box links two biplace lines

		// Nasty
		want(Tech.Swordfish);		 // same 3 places in 3 rows/cols
		want(Tech.URT);				 // UniqueRectangle/loop: loop of 2 maybes
		want(Tech.NakedQuad);		 // 4 cells with only 4 maybes
		want(Tech.HiddenQuad);		 // same 4 places for 4 values in a region
		want(Tech.Jellyfish);		 // same 4 places in 4 rows/cols
		want(Tech.Coloring);		 // 2 color-sets and the ONLY 3+ color-sets
		want(Tech.XColoring);		 // 2 color-sets with cheese
		want(Tech.Medusa3D);		 // 2 color-sets with cheese and pickles
		want(Tech.GEM);				 // 2 color-sets with the lot, and bacon

		// Alsic
		want(Tech.BigWings);			 // S/T/U/V/WXYZ-Wing: 4/5/6/7/8 + bivalue
		want(Tech.DeathBlossom);	 // stem of 2/3 maybes + 2/3 ALSs
		want(Tech.ALS_XZ);			 // a chain of 2 ALSs, cheese and pickle
		want(Tech.ALS_Wing);		 // a chain of 3 ALSs, for speed
		want(Tech.ALS_Chain);		 // a chain of 4..26 ALSs
		want(Tech.SueDeCoq);		 // Almost Almost Locked Sets (Sigh)

		// Fisch!
		want(Tech.FinnedSwampfish);  // OK
		want(Tech.FrankenSwampfish);	 // NONE
		want(Tech.MutantSwampfish);	 // NONE
		want(Tech.FinnedSwordfish);  // OK
		want(Tech.FrankenSwordfish);	 // OK
		want(Tech.MutantSwordfish);	 // SLOW (39 secs)
		want(Tech.FinnedJellyfish);  // SLOW (just)
		want(Tech.FrankenJellyfish);	 // SLOW (just)
		want(Tech.MutantJellyfish);	 // WAY TOO SLOW (7 mins)
		want(Tech.KrakenSwampfish);	 // SLOW (28 secs) Kraken Sux!
		want(Tech.KrakenSwordfish);	 // TOO SLOW (84 secs) Kraken Sux!
		want(Tech.KrakenJellyfish);	 // WAY TOO SLOW (8 mins) Kraken Sux!

		// Aligned Ally: Opposite Diagon Ally.
		want(Tech.AlignedPair);		 // OK (top1465 now none)
		want(Tech.AlignedTriple);	 // OK
		want(Tech.AlignedQuad);		 // a bit slow
		want(Tech.AlignedPent);		 // slow
		want(Tech.AlignedHex);		 // SLOW
		want(Tech.AlignedSept);		 // TOO SLOW
		want(Tech.AlignedOct);		 // FAR TOO SLOW
		want(Tech.AlignedNona);		 // WAY TOO SLOW
		want(Tech.AlignedDec);		 // CONSERVATIVE

		// Diabolical
		// StaticChain needs TableChain, Abduction, or UnaryChain, else it
		// produces invalid hints on bivalue cells. I don't understand why.
		// nb: unusual bitwise-or operator, so all are created if wanted.
		if ( want(Tech.TableChain)	 // WANT
		   | want(Tech.Abduction)	 // DROP
		   | want(Tech.UnaryChain) ) // DROP
			want(Tech.StaticChain);	 // WANT
		want(Tech.NishioChain);		 // DROP (X-Links only are slower)
		wantCD(Tech.DynamicChain);	 // WANT (XY-Links are faster)

		// IDKFA
		wantCD(Tech.DynamicPlus);	 // WANT Near catch-all
		wantCD(Tech.NestedUnary);	 // KEEP catch-all
		wantCD(Tech.NestedStatic);	 // DROP overkill
		wantCD(Tech.NestedDynamic);	 // DROP double overkill
		wantCD(Tech.NestedPlus);		 // DROP triple overkill. BUGS!

		final IHinter[] wantedHinters = wanted.toArray(new IHinter[wanted.size()]);
		return new LogicalSolver(basics, wantedHinters, unwanted, errors);
	}

	/**
	 * isWantedTech: is this Tech wanted by the user (TechSelectDialog).
	 * <p>
	 * If the user wants this Tech then the hinter (an implementation of Tech)
	 * is added to wantedHinters, and is subsequently used to solve/analyse
	 * puzzles; else the hinter is not constructed. Hinters can be RAM-heavy,
	 * hence slow to construct, so I avoid constructing unused hinters.
	 * <p>
	 * This array exists to enspeedonate the want methods, which is simply
	 * over-optimisation, because LogicalSolverBuilder executes VERY rarely.
	 * I do so only because I am mad. Sigh.
	 */
	private boolean[] isWantedTech;

	/**
	 * Set-up boolean[] isWantedTech to avert repeated O(n) EnumSet.contains.
	 * This is O(n) ONCE, and each subsequent former contains call is JUST an
	 * array-lookup.
	 * <p>
	 * isWantedTech's index is Tech.ordinal(), hence isWantedTech is coincident
	 * with Tech: a parallel array, like an extra field, but fast! And simples.
	 * <p>
	 * From LogicalSolverBuilders perspective wantedTechs is a WORM (write once
	 * read many) Set. The TechSelectDialog mutates a wantedHinters EnumSet to
	 * represent the users wishes, then logically creates an immutable Set from
	 * that for my (and LogicalSolvers) use. I merely take advantage of the
	 * immutability of the Set, to translate it into a faster-to-read format,
	 * that's all. It's simple once you understand it. If the wantedHinters was
	 * not immutable this wouldn't work. You'd end-up pulling your own hair out
	 * trying to get it to work, so just don't. Immutable is fast. Mutable not
	 * so much. Got it. Schlemeil can kiss my ____ing ass.
	 * <p>
	 * This is "extreme" masturbation. Performance here matters not, because
	 * LogicalSolverBuilder executes rarely. To go next level make the
	 * tech.ordinal() method a public field, but this is more than fast enough,
	 * simply because want is called tens of times, not thousands. Java tends
	 * towards wasty wasteful wastefulness. A secret tyranny of numbers, hidden
	 * in the implementation. An inability to rub one out in under 3.8 seconds
	 * is Unaustralian. Schlemeil is on notice. I just wish he would get over
	 * his competitive wanking fetish. Sharing is the only answer, because it
	 * works. But we're not Tasmanian. Sigh.
	 *
	 * @see #isWantedTech
	 *
	 * @return the CFG.getWantedTechs().size(), ie the number of wanted Techs
	 */
	private int isWantedTech() {
		isWantedTech = new boolean[Config.ALL_TECHS.size()];
		final EnumSet<Tech> wts = CFG.getWantedTechs();
		for ( Tech t : wts )
			isWantedTech[t.ordinal()] = true;
		return wts.size();
	}

	/**
	 * A better name would be addThisHinterIfItsWanted but it is too long.
	 * If the given tech is wanted then I instantiate its className, passing
	 * the tech if it is only constructor accepts it.
	 * <p>
	 * Some Tech have a null className, in which case you will need to supply
	 * the className (the IHinter that is the default implementation of Tech),
	 * or just instantiate any-non-conforming-hinter manually, and pass it to
	 * {@link #want(IHinter) the other want method}, but note that the hinter
	 * will then be constructed even if it is not wanted, which I dislike, so
	 * avoid if possible.
	 * <p>
	 * Programmers do not want to write code. They want to write the code which
	 * writes the code, and that is where the trouble started. Compiler anyone?
	 *
	 * @param tech the Tech to add to wantedHinters, if the user wants it.
	 * @throws IllegalArgumentException, RuntimeException
	 */
	private boolean want(final Tech tech) {
		final boolean result = isWantedTech[tech.ordinal()];
		if ( result ) {
			final IHinter hinter = createHinterFor(tech);
			if ( hinter == null )
				errors.add(tech);
			else
				wanted.add(hinter);
		} else
			unwanted.add(tech);
		return result;
	}

	/**
	 * A better name would be addThisHinterIfItsWanted but it is too long.
	 * If hinter.getTech() is wanted then add hinter to the wantedHinters;
	 * otherwise add the tech to unwanted, for printing once we are done
	 * wanting everything.
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
		if ( result=isWantedTech[tech.ordinal()] )
			wanted.add(hinter);
		else
			unwanted.add(tech);
		return result;
	}

	/**
	 * Want for ChainerDynamic, whose constructor takes the basics parameter.
	 *
	 * @param hinter to want, or not
	 */
	private void wantCD(final Tech tech) {
		if ( isWantedTech[tech.ordinal()] )
			wanted.add(new ChainerDynamic(tech, basics, true));
		else
			unwanted.add(tech);
	}

}
