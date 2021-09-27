/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.utils.Debug;


/**
 * HiddenSingle implements the Hidden Single solving technique. A "hidden
 * single" is the only remaining possible location for a given value in a
 * region. The Cell is set to that value when the hint is applied.
 * <p>
 * <b>LARGE ASIDE: <u>The "Rip Nico A New One" Rant</u>:</b>
 * <p>Because this is (nearly) the simplest hinter, lets go through the below
 * single line of code with a fine-toothed comb, to see why this version is "a
 * bit" faster than the original. These principles are applied throughout:
 * <br><b>NOW:</b><pre>{@code
 *   if ( region.idxsOf[value].size == 1 )
 * }</pre>
 * This implementation:<ul>
 * <li>dereferences the region object to get the address of the idxsOf array,
 * <li>which we offset to get this values 'Indexes' class,
 * <li>which we dereference to get the 'size' field,
 * <li>which we push onto a register along with the constant 1, and then equate
 * the two values.
 * <li>and then we short-jump somewhere (no new stackframe), or fallthrough.
 * </ul>
 * <p>Now strap in: This is a key reason why this version executes a little
 * bit faster than that the previous implementation:<br>
 * <b>WAS:</b><pre>{@code
 *   if (region.getPotentialPositions(value).cardinality() == 1)
 * }</pre>
 * This code follows "standard" coding doctrine, especially the one about
 * having a single point of truth, which you query actively: which is data
 * light, but CPU heavy; which is a strategy to deal with NEVER having enough
 * bloody RAM. My first PC had 32K, which was considered HUGE at the time. So
 * conserving RAM became such an ingrained habit that we didn't notice it.
 * <p>
 * It's worth noting that this implementation uses 26K just to store the 81
 * isSiblingOf matrixes, instead of invoking a method 4 BILLION times, just
 * because it's a bit faster, and I now have 16 Gig to play with. When I run
 * this thing from the command line I give it a max of a Gig of RAM.
 * <p>
 * So the original calls the {@code region.getPotentialPositions(value)} method,
 * which dereferences region to get it's {@code getPotentialPositions} method,
 * creates a new stackframe, pushes 'value' onto the stack, and long-jumps to
 * the start of the method: <pre>{@code
 *   public BitSet getPotentialPositions(int value) {
 *     BitSet result = new BitSet(9);
 *     for (int index = 0; index < 9; index++) {
 *       result.set(index, getCell(index).hasPotentialValue(value));
 *     }
 *     return result;
 *   }
 * }</pre>
 * The first thing it does is create a new BitSet which is, IMHO, just more work
 * for the Garbage Collector (GC) later on. Your council has a trash collection
 * service, so do you expect a garbo to follow you around and pick-up your lolly
 * wrappers? No, you put them in a bin yourself, yeah? The same principle can
 * (and I think should) be applied to code: just because you can create new
 * objects (instances of classes) doesn't mean that you SHOULD create them...
 * often there's an old-school way to solve the problem, which is more code, and
 * looks ugly, but executes MUCH more efficiently.
 * <p>
 * Could you reasonably expect your garbo to pick-up the lolly wrappers produced
 * by a lolly unwrapping machine that unwraps 3.4 billion lollies a second?
 * <pre>                    What are you doing Dave?</pre>
 * <p>
 * So we create a BitSet, then call {@code getCell(index)} <b>NINE</b> times,
 * ie: find the getCell method, which is a bit trickier because it's overridden.
 * It's implementation depends upon the type of this region, so it'll be any of
 * (this is a facetious argument coz it's a compile time problem, but it's still
 * somewhat illuminating):<br>
 * <br>
 * <b>Row</b><pre>{@code
 *  public Cell getCell(int index) {
 *    return cells[rowNum][index];
 *  }
 * }</pre>
 * <b>Column</b><pre>{@code
 *  public Cell getCell(int index) {
 *    return cells[index][columnNum];
 *  }
 * }</pre>
 * <b>Block</b><pre>{@code
 *  public Cell getCell(int index) {
 *    return cells[vNum*3 + index/3][hNum*3 + index%3];
 *  }
 * }</pre>
 * <p>
 * So we find the method and create a new stackframe, push index onto the stack,
 * long-jump to start of method: and retrieve the cell from the matrix (badly
 * named 'cells'. Also repeated arithmetic to demangle index into a matrix
 * coordinate: cells should be a flat array) and return it; then we invoke the
 * {@code hasPotentialValue(value)} method of the returned cell:<pre>{@code
 *   public boolean hasPotentialValue(int value) {
 *     return this.potentialValues.get(value);
 *   }
 * }</pre>
 * which dereferences 'this' to find and invoke the {@code BitSet.get} method:
 * <pre>{@code
 *  public boolean get(int bitIndex) {
 *    if (bitIndex < 0)
 *      throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
 *    checkInvariants();
 *    int wordIndex = wordIndex(bitIndex);
 *    return (wordIndex < wordsInUse)
 *        && ((words[wordIndex] & (1L << bitIndex)) != 0);
 *  }
 * }</pre>
 * and the {@code checkInvariants()} method is:<pre>{@code
 *   private void checkInvariants() {
 *     assert(wordsInUse == 0 || words[wordsInUse - 1] != 0);
 *     assert(wordsInUse >= 0 && wordsInUse <= words.length);
 *     assert(wordsInUse == words.length || words[wordsInUse] == 0);
 *   }
 * }</pre>
 * and the next call is to {@code wordIndex(bitIndex)} which IMHO should not be
 * a method at all (I wish Java had macros) is:<pre>{@code
 *   private static int wordIndex(int bitIndex) {
 *     return bitIndex >> ADDRESS_BITS_PER_WORD;
 *   }
 * }</pre>
 * so we return the BitSet, upon which we call the {@code cardinality()}
 * method:<pre>{@code
 *   public int cardinality() {
 *     int sum = 0;
 *     for (int i = 0; i < wordsInUse; i++)
 *       sum += Long.bitCount(words[i]);
 *     return sum;
 *   }
 * }</pre>
 * and there's allways only one word in use, so we'll call
 * {@code Long.bitCount(words[i])} ONCE (thank god):<pre>{@code
 *   public static int bitCount(long i) {
 *      // HD, Figure 5-14
 *      i = i - ((i >>> 1) & 0x5555555555555555L);
 *      i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
 *      i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
 *      i = i + (i >>> 8);
 *      i = i + (i >>> 16);
 *      i = i + (i >>> 32);
 *      return (int)i & 0x7f; // I wanr a T-Shirt with "Go'n'get 0x7f;ed!" on it
 *   }
 * }</pre>
 * then we're back up to the <b>NINE</b> times
 * {@code result.set(index, getCell(index).hasPotentialValue(value));}
 * line to invoke BitSets {@code set(int bitIndex, boolean value)} method:
 * <pre>{@code
 *   public void set(int bitIndex, boolean value) {
 *     if (value)
 *       set(bitIndex);
 *     else
 *       clear(bitIndex);
 *   }
 * }</pre>
 * First let's presume that 'value' is true and follow the positive path:<ul>
 * <li><b>OPTION: set</b><br>
 * so let's look at the {@code set(int bitIndex)} method:<pre>{@code
 *   public void set(int bitIndex) {
 *     if (bitIndex < 0)
 *       throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
 *     int wordIndex = wordIndex(bitIndex);
 *     expandTo(wordIndex);
 *     words[wordIndex] |= (1L << bitIndex); // Restores invariants
 *     checkInvariants();
 *   }
 * }</pre>
 * which calls the {@code wordIndex(bitIndex)} method <i>(repeated)</i>:
 * <pre>{@code
 *   private static int wordIndex(int bitIndex) {
 *     return bitIndex >> ADDRESS_BITS_PER_WORD;
 *   }
 * }</pre>
 * and then the {@code expandTo(int wordIndex)} method:<pre>{@code
 *  private void expandTo(int wordIndex) {
 *    int wordsRequired = wordIndex+1;
 *    if (wordsInUse < wordsRequired) {
 *      ensureCapacity(wordsRequired);
 *      wordsInUse = wordsRequired;
 *    }
 *  }
 * }</pre>
 * which will never actually does anything because we've only 9 values to store
 * so 'wordsInUse' will allways be one, as will 'wordsRequired', ergo the BitSet
 * is being <b>ab</b>used: by which I mean "used for a purpose other than that
 * which was intended by it's author". There's a big clue in the first line of
 * BitSets API documentation: "This class implements a vector of bits <b>that
 * grows as needed</b>". We don't need a class that "grows as needed", hence we
 * clearly should NOT be using a BitSet, even if it means rolling our own. Or
 * better still don't use a class at all (as I have done, sort of).
 * <p>
 * and finally the last {@code checkInvariants()} method <i>(repeated)</i>:
 * <pre>{@code
 *   private void checkInvariants() {
 *     assert(wordsInUse == 0 || words[wordsInUse - 1] != 0);
 *     assert(wordsInUse >= 0 && wordsInUse <= words.length);
 *     assert(wordsInUse == words.length || words[wordsInUse] == 0);
 *   }
 * }</pre>
 * </li>
 * <li><b>OPTION: clear</b><br>
 * we're back up in {@code set(int bitIndex, boolean value)} method, in which
 * the other possible path is the {@code clear()} method:<pre>{@code
 *   public void clear() {
 *     while (wordsInUse > 0)
 *       words[--wordsInUse] = 0;
 *   }
 * }</pre>
 * which will loop ONCE because (as previously ranted) 'wordsInUse' will only
 * ever be 1 (or it could possibly be 0).
 * </li>
 * </ul>
 * <p>
 * And after we've done all that NINE times we just return the BitSet and we're
 * done. So that's about it. Thank you umpires. Thank you ball boys.
 * <p>
 * So we've got <b>ONE</b> fairly simple line of code which does:
 * {@code dereference, offset, dereference, push, push, equate, jump} verses
 * what turns out to be more akin to HOMERS BLOODY ODDESSY, which is my whole
 * point really: <b>BEWARE of Homic secretions in your tight loops.</b>
 * <p>
 * Can you think why the simpler implementation might be "a bit" faster:<pre>
 *        this 128,830,443,543 / orig 1,314,078,704,062 * 100.0 = 9.8%
 * </pre>
 * Now here's the fundamental question: Is all this lovely efficiency worth all
 * the extra effort? I'm generally in the "More brain, less CPU!" camp myself,
 * especially in a domain like this, with real war machines in play (like
 * Aligned*Exclusion) which eat CPU for breakfast.
 * <p>
 * Your position is your affair.
 * <p>
 * There's a great blog-post along these lines by a "real" developer whose name
 * currently alludes me called something like "Schlemeil the Painters
 * algorithm". I highly recommend it.
 * <p>
 * I'm also saying here that Java gained a reputation for being slow, but that's
 * mostly because of how a lot of Java code, including parts of the API, is/was
 * designed; not because of anything inherent to the language itself.
 * You can get REAL performance out of the bastard. You just have to be careful
 * how you go. Just because the language encourages you to be lazy doesn't mean
 * that you HAVE to be lazy. You can be SMART and LAZY. Best of both worlds.
 * <p>
 * And finally a jab at the java API developers. Might it be worth putting a
 * <b>non-extensible</b> BitSet in the Java API just for use by Uni students,
 * which is exactly where a BitSet class will get most of it's use, because an
 * experienced programmer (ie me) thinks "class" and "bitset" are oxymoronic.
 * If I'm using bitsets I want speed, and that means NOT invoking methods in
 * tight loops, which means no BitSet class, just a raw datatype and a LOT of
 * funky-looking code; which totally sucks bags until you understand it, and
 * why it's faster than the alternatives.
 * <p>
 * But I can only speak for myself, at 9.8% of the volume evidently required to
 * ever change anybodies mind about anything. They abandoned this rant ages ago.
 * Gone back to looking at porn I expect. I know Knuffink!
 * <p>
 * Cheers all.
 */
public final class HiddenSingle extends AHinter {

	public HiddenSingle() {
		super(Tech.HiddenSingle);
	}

	/**
	 * Adds a new hint to 'accu' for each value which has 1 possible location
	 * in the region. Searches each of the grids 27 regions for each value
	 * which has just 1 possible position.
	 * <p>
	 * This implementation is utterly reliant upon the regions idxsOf arrays,
	 * which are maintained internally by the Grid when a maybe is removed,
	 * specifically so that this hinter (and others) can be faster. It works out
	 * cheaper to spend more time on maintenance to produce an artefact (the
	 * idxsOf array) which saves time on repetitiously querying. This goes
	 * against standard coding doctrine, which promotes a single point of truth
	 * that's actively queried, and evidently produces a SLOWER system that's
	 * composed of less, tidier, simpler, and more maintainable code.
	 * <p>
	 * Do you want your code to look good, and therefore be maintainable, or to
	 * run efficiently? In the "big" Sudoku hinters (especially A*E) it can ONLY
	 * run if it runs quickly, so I've gone the whole hog on speed! And I'm
	 * guessing that I've done A5+E for the first time, anywhere. PC's were too
	 * slow previously to allow nutter like me to take on challenges like that.
	 * <p>
	 * See the rant in this classes comments for more.
	 *
	 * @return was one-or-more hint found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Indexes[] rio; // regions indexesOf [values 1..9].
		int v; // the value we're look at in the current region
		Cell cell; // the only cell in region which maybe value
		AHint hint; // the HiddenSingleHint, where applicable
		boolean result = false; // presume failure, ie no hints found.
		for ( ARegion region : grid.regions ) { // 27 (9 Box, 9 Row, 9 Col)
//if ( "box 9".equals(region.id) )
//	Debug.breakpoint();
			if ( region.emptyCellCount > 0 ) { // ignore filled regions
				rio = region.indexesOf;
				for ( v=1; v<10; ++v ) {
					if ( rio[v].size == 1 ) { // there's 1 location for v in region
						// create the hint
						cell = region.cells[rio[v].first()];
						hint = new HiddenSingleHint(this, region, cell, v);
						// exit immediately if accu.add says we should
						if ( accu.add(hint) )
							return true;
						result = true; // Yeah, we found (at least) one hint!
					}
				}
			}
		}
		return result;
	}
}