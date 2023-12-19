/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Ass;
import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyFunkyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedFifoQueue;
import diuf.sudoku.utils.MyLinkedHashSet;
import static diuf.sudoku.utils.MyMath.bound;
import static diuf.sudoku.utils.StringPrintWriter.stackTraceOf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * KrakenFishHint is the DTO for a Kraken Fish hint.
 * <p>
 * A KrakenFishHint "wraps" the "cause" ComplexFishHint to modify the reported
 * hint-type-name and hint-HTML, and display the chains with links.
 *
 * @author Keith Corlett 2020-10-07
 */
public class KrakenFishHint extends AHint  {

	// 128 is arbitrary: its intended to be not too much
	// larger than the length of the longest valid chain.
	private static final int MAX_CHAIN_LENGTH = 128; // power of 2

	private final String krakenHintTypeName;
	private final ComplexFishHint cause;
	private final List<Ass> chainTargets; // Effs thatre back-linked
	private final int candidate; // the fish candidate value
	KrakenFishHint(final Grid grid, final IHinter hinter
			, final String krakenHintTypeName, final Pots reds
			, final ComplexFishHint cause, final List<Ass> chainTargets
			, final int candidate) {
		super(grid, hinter, reds);
		this.krakenHintTypeName = krakenHintTypeName;
		this.cause = cause;
		this.chainTargets = chainTargets;
		this.candidate = candidate;
	}

	// needed to squeeze the bloody hint toString!
	@Override
	public boolean isKraken() {
		return true;
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		return cause.getGreenPots(viewNum);
	}

	@Override
	public Pots getOrangePots(int viewNum) {
		return cause.getOrangePots(viewNum);
	}

	@Override
	public Pots getBluePots(Grid grid, int viewNum) {
		return cause.getBluePots(grid, viewNum);
	}

	@Override
	public Pots getPurplePots() {
		return cause.getPurplePots();
	}

	@Override
	public Pots getYellowPots() {
		return cause.getYellowPots();
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNum) {
		return cause.getAquaBgIndices(viewNum);
	}

	@Override
	public ARegion[] getBases() {
		return cause.bases;
	}

	@Override
	public ARegion[] getCovers() {
		return cause.covers;
	}

	@Override
	public int getDifficulty() {
		if ( difficulty == null ) {
			int d = hinter.getTech().difficulty + cause.fishType.complexity();
			difficulty = bound(d, Difficulty.Fisch.min, Difficulty.Fisch.max-1);
		}
		return difficulty;
	}
	private Integer difficulty;

	/**
	 * Used by getLinks, this method is a copy-paste from AChainerBase rather
	 * than make AChainerBase visible outside of the chains package just to make
	 * this static method visible to me, or move this method to a Tools class,
	 * ie I should create a new Tools class, but find myself currently unable
	 * to do so because my tackle-box has crumpetitis and these faaaaaaarking
	 * Lemmings wont suicide by themselves ya know.
	 */
	private ArrayList<Ass> ancestors(Ass target) {
		// We need each distinct ancestor (my parents, and my parents parents,
		// and there parents, and so on) of the target Ass.
		// MyFunkyLinkedHashSet does the distinct for us, quickly & easily
		// because its add-method is add-only (it doesnt update existing).
		// A HashSet of 128 has enough bits for Ass.hashCode (8 bits).
		final Set<Ass> distinctSet = new MyFunkyLinkedHashSet<>(128, 1F);
		// we append the parents of each assumption to the end of a FIFO queue,
		// and then poll-off the head of the queue until the queue is empty.
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		for ( Ass a=target; a!=null; a=todo.poll() )
			if ( distinctSet.add(a) ) // add only, doesnt update existing
				todo.addAll(a.parents); // addAll ignores a null list
		return new ArrayList<>(distinctSet);
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			// Use a Set instead of a List, to supress duplicates.
			final Set<Link> links = new MyLinkedHashSet<>(64); // let it grow!
			for ( Ass target : chainTargets )
				for ( Ass c : ancestors(target) )
					if ( c!=null && c.parents!=null )
						for ( Ass p : c.parents )
							links.add(new Link(p, c));
			return links;
		} catch (Exception ex) {
			// Im only ever called in the GUI, so just log it.
			Log.println("KrakenFishHint.getLinks: "+ ex);
			return null;
		}
	}

	// used only in the test-cases
	public List<Ass> getChainTargets() {
		return chainTargets;
	}

	// returns a line of HTML for each chain
	private String chainsString() {
		final StringBuilder sb = SB(1024);
		try {
			final Set<String> distinct = new MyLinkedHashSet<>(MAX_CHAIN_LENGTH);
			final StringBuilder line = SB(128);
			for ( Ass target : chainTargets ) {
				if ( target != null ) {
					line.setLength(0);
					Ass.appendChain(line, target, " &lt;- ").append(NL);
					if ( distinct.add(line.toString()) ) {
						sb.append(line);
					}
				}
			}
		} catch (Throwable ex) {
			// esp out of memory error (now reported as RTE: endless loop)
			sb.setLength(0);
			sb.append("<font color=\"red\"><i>").append(NL)
			.append("KrakenFishHint.appendChains: ").append(ex).append(NL)
			.append(stackTraceOf(ex))
			.append("</i></font>").append(NL);
		}
		return sb.toString();
	}

	@Override
	protected String getHintTypeNameImpl() {
		return krakenHintTypeName;
	}

	@Override
	protected StringBuilder toStringImpl() {
		final StringBuilder sb = SB(128);
		if ( isInvalid )
			sb.append("#");
		StringBuilder cb = cause.toStringImpl();
		int i;
		if ( (i=cb.indexOf("#")) > -1 )
			cb.delete(i, i+1);
		sb.append(krakenHintTypeName).append(cb);
		return sb;
	}

	@Override
	protected String toHtmlImpl() {
		// color-in my toString() to make it easier to see whats what.
		final String coloredHint = Html.colorIn(
			toString().replaceFirst(COLON_SP, ": <b1>")
					  .replaceFirst(AND, "</b1> and <b2>")
					  .replaceFirst(" on", "</b2> on"));
		return Html.produce(this, "KrakenFishHint.html"
			, getHintTypeName()				// {0}
			, coloredHint					//  1
			, chainsString()				//  2
			, reds.toString()				//  3
			, Integer.toString(candidate)	//  4
			, debugMessage					//  5
		);
	}

}
