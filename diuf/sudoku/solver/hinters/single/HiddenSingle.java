/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Indexes.first;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * HiddenSingle implements {@link Tech#HiddenSingle} Sudoku solving technique.
 * <p>
 * A HiddenSingle is the last remaining place for a value in a region, hence
 * that Cell is set to value when the hint is applied.
 * <p>
 * <b><u>RANT</u></b> (The "Rip Nico A New One" Rant)
 * <p>
 * Because this is nearly the simplest hinter, lets go through a single line of
 * code from Juillerat's implementation with a fine-toothed comb, to see why SE
 * is now "a bit" faster than it was (was 22 minutes, now 1 minute). Similar
 * principles have been applied throughout SE's codebase, for speed.
 * <p>
 * <b>NOW:</b><pre>{@code
 *   if ( numPlaces[v] == 1 ) {
 *     ... whatever ...
 *   }
 * }</pre>
 * This implementation:<ul>
 * <li>numPlaces is a local stack reference, which is faster to execute than a
 *  heap reference. I dont really understand why. I just observe that it is so.
 * <li>array-lookup to get the number of cells in this region that maybe value,
 * <li>which we push onto a register along with the constant 1, and then equate
 * the two values.
 * <li>then short-jump somewhere (no new stackframe), or fall-through.
 * </ul>
 * <p>
 * Now strap in folks, here is why my code is "a bit" faster than Nico's:<br>
 * <b>WAS:</b><pre>{@code
 *   if (region.getPotentialPositions(value).cardinality() == 1)
 * }</pre>
 * This code follows "standard" coding doctrine, especially the one about a
 * single point of truth, which one queries actively: which is data light, but
 * CPU heavy; which is a strategy to deal with NEVER having enough bloody RAM.
 * My first PC had 32K, which was pretty big at the time (64K was the biggest).
 * So we all became habituated to conserving RAM; as we remain, despite RAM now
 * coming by the Gig (my PC has 16); hence the requirement for this approach
 * has mostly dissipated. Relief! Nice one! But technology outruns brains.
 * <p>
 * SE uses 26K just to store 81 sees matrixes, instead of invoking a method 4
 * BILLION times, because its faster. My batch.bat gives the JRE 1Gb of stack,
 * which should be "adequate".
 * <p>
 * So we call {@code region.getPotentialPositions(value)} which dereferences
 * region to get its {@code getPotentialPositions} method, creates a new stack-
 * frame, pushes $value onto the stack, and long-jumps to:<pre>{@code
 *   public BitSet getPotentialPositions(int value) {
 *     BitSet result = new BitSet(REGION_SIZE);
 *     for (int index = 0; index < REGION_SIZE; index++) {
 *       result.set(index, getCell(index).hasPotentialValue(value));
 *     }
 *     return result;
 *   }
 * }</pre>
 * The first thing it does is create a new BitSet which is, IMHO, just work for
 * the Garbage Collector (GC) later on. Your council empties your rubbish-bin,
 * but you don't expect a garbo to follow you around and pick-up your lolly
 * wrappers: No, you put them in a bin yourself, yeah? The same principle can
 * (and I think should) be applied to code: just because you can create new
 * instances does not mean that you SHOULD. There is usually another way thats
 * more code which looks ugly, but is more efficient and therefore faster. Its
 * almost always faster to use the heap than the stack. But YMMV!
 * <p>
 * Q: Can a garbo pick-up 3.6 billion lolly wrappers a second? <br>
 * A: "What are you doing Dave?"
 * <p>
 * So we create a BitSet, then call {@code getCell(index)} <b>NINE</b> times,
 * ie: find the getCell method, which is a bit trickier coz it is overridden.
 * Its implementation depends upon the type of this region, so it will be any
 * of (this is a facetious argument coz its a compile time problem, but its
 * still illuminating):<br>
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
 * As an aside, there is no need to use a doubly subscripted array for cells.
 * A single index is "a bit" faster. Sigh.
 * <p>
 * So we find the method and get a new stackframe, push index onto the stack,
 * long-jump to start of method: and retrieve the cell from the matrix called
 * $cells, and also repeated arithmetic to demangle index into a matrix
 * coordinate: cells should be a flat array, coz multiplication and division
 * are SLOWER than an array look-up, perversely mod is on par) and return it;
 * then we invoke the {@code hasPotentialValue(value)} method of the returned
 * cell:<pre>{@code
 *   public boolean hasPotentialValue(int value) {
 *     return this.potentialValues.get(value);
 *   }
 * }</pre>
 * which dereferences potentialValues to find and invoke the {@code BitSet.get}
 * method (new stackframe, populate, long-jump, yada yada, return):
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
 * so we return the BitSet, upon which we call {@code cardinality()}:<pre>{@code
 *   public int cardinality() {
 *     int sum = 0;
 *     for (int i = 0; i < wordsInUse; i++)
 *       sum += Long.bitCount(words[i]);
 *     return sum;
 *   }
 * }</pre>
 * and there is allways only one word in use, so we will call
 * {@code Long.bitCount(words[i])} ONCE (it is a bit slow, sigh):<pre>{@code
 *   public static int bitCount(long i) {
 *      // HD, Figure 5-14
 *      i = i - ((i >>> 1) & 0x5555555555555555L);
 *      i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
 *      i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
 *      i = i + (i >>> 8);
 *      i = i + (i >>> 16);
 *      i = i + (i >>> 32);
 *      return (int)i & 0x7f;
 *   }
 * }</pre>
 * &lt;KRC 2022-01-11 bitCount is s__tloads faster on my i7. firmware?&gt;<br>
 * then we are back up to the <b>NINE</b> times
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
 * First let us presume that $value is true and follow the positive path:<ul>
 * <li><b>OPTION: set</b><br>
 * so let us look at the {@code set(int bitIndex)} method:<pre>{@code
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
 * which will never actually does anything because we have only 9 values to
 * store so $wordsInUse is allways 1, as is $wordsRequired, ergo BitSet is
 * being <u>ab</u>used: by which I mean "used for a purpose other than that
 * intended by its author". There is a big clue in the first line of BitSets
 * API documentation: "This class implements a vector of bits <b>that grows as
 * needed</b>". We do not need a class that "grows as needed", hence we clearly
 * should NOT be using a ____ing BitSet, even if it means rolling our own; or
 * better still do not use a class at all, as I have done.
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
 * we are back up in {@code set(int bitIndex, boolean value)} method, in which
 * the other possible path is the {@code clear()} method:<pre>{@code
 *   public void clear() {
 *     while (wordsInUse > 0)
 *       words[--wordsInUse] = 0;
 *   }
 * }</pre>
 * which will loop ONCE because (as previously ranted) $wordsInUse will only
 * ever be 1 (or it could possibly be 0).
 * </li>
 * </ul>
 * <p>
 * And after we have done all that NINE times we just return the BitSet and we
 * are done. So thats about it. Thank you umpires. Thank you ball boys.
 * <p>
 * So we have got <b>ONE</b> fairly simple line of code which does:
 * {@code array-lookup, push, push, equate, jump} verses what turns out to be
 * more akin to HOMERS BLOODY ODDESSY, which is my whole point really:
 * <b><u>LOOK OUT for Homic secretions in your tight loops!</u></b>
 * <p>
 * Can you think why the simpler implementation might be "a bit" faster:<pre>
 *        mine 82,591,363,300 / Nico 1,314,078,704,062 * 100.0 = ~6.285%
 * </pre>
 * It's still not perfect, but it's vastly improved.
 * <p>
 * Now here's the fundamental question: Is all this lovely efficiency worth the
 * effort? I generally go for "More brain, less CPU!" myself, especially in a
 * domain like Sudoku, where we play with real war machines that eat CPU (and
 * small children) for breakfast. Your position is your business.
 * <p>
 * Java has gained a reputation for being slow, but that is mostly because of
 * how a lot of Java code (including parts of the API) are written; not because
 * of anything inherent to the language itself. Java can be REALY performant,
 * just mind how you go. Just because a language encourages you to be lazy does
 * not mean that you HAVE to be lazy. Be SMART and LAZY. Best of both worlds.
 * <p>
 * And finally a jab at the java API developers. Might it be worth putting a
 * SizedBitSet in the Java API just for use by Uni students, which is where
 * a BitSet class will get most of it is use, because an expert thinks "class"
 * and "bitset" are oxymoronic. I use bitsets for speed, and that means LESS
 * method calls, ie no BitSet class, just raw datatypes and lots of ugly code;
 * which sux until you understand WHY it is so much faster than ANY class;
 * because it's ALL native. Sigh.
 * <p>
 * But I can only speak for myself, at 1.8% of the volume evidently required to
 * ever change anybodies bloody mind about anything. Most folks abandoned this
 * rant ages ago (TLDR) and went back to watching porn. I know Knuffink!
 * <p>
 * Cheers all.
 */
public final class HiddenSingle extends AHinter {

	public HiddenSingle() {
		super(Tech.HiddenSingle);
	}

	/**
	 * Adds a hint to $accu for each value which has ONE-only possible location
	 * in the region. I search each of the grids 27 regions for each value
	 * which has ONE-only possible position.
	 * <p>
	 * This implementation is utterly reliant upon the regions idxs arrays,
	 * which are maintained internally by the Grid when a maybe is removed,
	 * specifically so that this hinter (and others) can be faster. It works
	 * out faster to spend more time on modifying the Grid to produce artefacts
	 * (like idxs) which save time in the hinters. This goes directly against
	 * standard coding doctrine, which promotes a single point of truth that is
	 * queried actively, producing a SLOWER system that is composed of less,
	 * tidier, simpler, and more maintainable code. Speed costs! Get over it.
	 * <p>
	 * So: Do you want your code to be maintainable, or to run efficiently? In
	 * the big Sudoku hinters (especially A*E) it can ONLY run if it is fast.
	 * I have gone the whole hog on speed! I guess my A5E is the first ever.
	 * My A10E is probably the last ever. Sigh. PCs were previously too slow
	 * for nutters like me to want to tackle it.
	 * <p>
	 * HiddenSingle (one of the foxes) is used in BruteForce, and the Chainers,
	 * hence performance <u>really</u> matters here.
	 *
	 * @param grid to search
	 * @param accu to which I add hints
	 * @return was any (one-or-more) hint found?
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		int[] numPlaces; // the number of cells in this region which maybe v
		// presume that no hints will be found
		boolean result = false;
		// foreach of the 27 regions in the grid
		for ( ARegion region : grid.regions )
			// if this region has empty cells
			if ( region.emptyCellCount > 0 ) {
				// fastard: faster to dereference once than upto 9 times
				numPlaces = region.numPlaces;
				// foreach unsetCands in this region (in 1..9).
				// fastard: unsetCands is a Values bitset of those values that
				// have not yet been set in any cell in this region.
				// VALEUESES is an array of int-arrays whose first subscript is
				// a Values bitset; so VALUESES[region.unsetCands] is a premade
				// array of the values remaining to be set in this region.
				for ( int v : VALUESES[region.unsetCands] )
					// if there is only 1 place for v in this region
					if ( numPlaces[v] == 1 ) {
						// FOUND a HiddenSingle! so raise a hint
						result = true;
						// the first method is a bit interesting. Take a look.
						// exit-early in the batch
						if ( accu.add(new HiddenSingleHint(grid, this, region
								, region.cells[first(region.places[v])], v)) )
							return result;
					}
			}
		return result;
	}

}
