/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyFunkyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedFifoQueue;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.MyStrings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * KrakenFishHint is the data transfer object for a Kraken Fish hint.
 * <p>
 * A KrakenFishHint "wraps" the "cause" ComplexFishHint to modify the reported
 * hint-type-name and hint-HTML, and provide links to portray the chains.
 *
 * @author Keith Corlett 2020-10-07
 */
public class KrakenFishHint extends AHint implements IActualHint {

	static enum Type {
		  ONE("Kraken type 1")
		, TWO("Kraken type 2")
		;
		public final String name;
		Type(String name) {
			this.name = name;
		}
	}

	private final ComplexFishHint cause;
	private final Type hType;
//not used
//	private final Values valsToRemove;
	private final List<Ass> chains;
//not used
//	private final Idx fins;
	KrakenFishHint(AHinter hinter
			, Pots reds
			, ComplexFishHint cause
//			, Values valsToRemove
			, Type type
			, List<Ass> chains
//			, Idx fins
	) {
		super(hinter, reds);
		this.cause = cause;
		this.hType = type;
//		this.valsToRemove = valsToRemove;
		this.chains = chains;
//		this.fins = fins;
		cleanPots();
	}

	private void cleanPots() {
		// add the cause hints eliminations (if any) to mine
		// and if it's a red then it's a RED only!
		redPots.addAll(cause.redPots).removeFromAll(getGreens(0), getOranges(0)
					, getBlues(null, 0), getPurples(), getYellows());
	}

	@Override
	public Pots getGreens(int viewNum) {
		return cause.getGreens(viewNum);
	}

	@Override
	public Pots getOranges(int viewNum) {
		return cause.getOranges(viewNum);
	}

	@Override
	public Pots getBlues(Grid grid, int viewNum) {
		return cause.getBlues(grid, viewNum);
	}

	@Override
	public Pots getPurples() {
		return cause.getPurples();
	}

	@Override
	public Pots getYellows() {
		return cause.getYellows();
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return cause.getAquaCells(viewNum);
	}

	@Override
	public List<ARegion> getBases() {
		return cause.bases;
	}

	@Override
	public List<ARegion> getCovers() {
		return cause.covers;
	}

	/**
	 * Used by getLinks (a copy-paste from AChainer rather than make AChainer
	 * visible outside of the package just to make this static method visible
	 * to me, or move this method to a FishTools class, ie I should create a
	 * new FishTools class, but find myself currently unable to do so coz my
	 * tackle-box has crumpetitis and lemmings won't suicide by themselves).
	 */
	private ArrayList<Ass> getAncestorsList(Ass target) {
		// We need each distinct ancestor (my parents, and my parents parents,
		// and there parents, and so on) of the target Ass.
		// MyFunkyLinkedHashSet does the distinct for us, quickly & easily
		// because it's add-method is add-only (it doesn't update existing).
		// A HashSet of 128 has enough bits for Ass.hashCode (8 bits).
		final Set<Ass> distinctSet = new MyFunkyLinkedHashSet<>(128, 1F);
		// we append the parents of each assumption to the end of a FIFO queue,
		// and then poll-off the head of the queue until the queue is empty.
		final MyLinkedFifoQueue<Ass> todo = new MyLinkedFifoQueue<>();
		for ( Ass a=target; a!=null; a=todo.poll() )
			if ( distinctSet.add(a) ) // add only, doesn't update existing
				todo.addAll(a.parents); // addAll ignores a null list
		return new ArrayList<>(distinctSet);
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			// Unusually, use a distinct-set instead of list, otherwise the
			// grid disappears under a plethora of extraneous duplicate links.
			final Set<Link> links = new MyLinkedHashSet<>(512);
			chains.forEach((target) -> {
				getAncestorsList(target).stream()
					.filter((c) -> ( c!=null
								  && c.parents!=null
								  && c.parents.size>0 ))
					.forEachOrdered((c) -> {
						c.parents.forEach((p) -> {
							// p for parent, c for child
							links.add(new Link(p, c));
						});
					});
			});
			return links;
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("KrakenFishHint.getLinks: "+ ex);
			return null;
		}
	}

	// append a line of HTML to sb for each chain
	private StringBuilder appendChains(StringBuilder sb) {
		final int initialLength = sb.length();
		try {
			// kf2: each target ass is the same elimination, but the chain to
			// get there differs, each ending in one of the fin (blue) cells.
			final Set<String> set = new MyLinkedHashSet<>(512);
			StringBuilder line = new StringBuilder(128); // just a guess
			int len; // we need an arbitrary max chain length, apparently
			for ( Ass target : chains ) {
				if ( target == null )
					continue; //WTF: Can't happen, but happened (now fixed)
				line.setLength(0); // clear the line
				line.append(target);
				len = 0;
				for ( Ass a=target.firstParent(); a!=null; a=a.firstParent() ) {
					// " <- H4-6"
					line.append(" &lt;- ").append(a);
					// 128 is arbitrary: it's intended to be not too much
					// larger than the length of the longest valid chain.
					if ( ++len > 127 ) // happened in KrakenFisherman1
						throw new RuntimeException("endless loop");
				}
				line.append(NL);
				if ( set.add(line.toString()) )
					sb.append(line);

			}
		} catch (Throwable ex) {
			// esp out of memory error (now reported as RTE: endless loop)
			sb.setLength(initialLength);
			sb.append("<font color=\"red\"><i>")
			  .append("KrakenFishHint.appendChains: ").append(ex)
			  .append("</i></font>").append(NL);
		}
		return sb;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return hType.name + ": " + cause.getHintTypeName();
	}

	@Override
	protected String toStringImpl() {
		if ( isInvalid )
			return "#" + hType.name + ": " + cause;
		return hType.name + ": " + cause;
	}

	@Override
	protected String toHtmlImpl() {
		// add some color to toString() to make it easier to see what's what.
		// NOTE: replaceSecond to cater for the odd "wrapped" hint name:
		// Kraken type 2: Finned Swampfish: col D, col H and row 2, row 6 on 3
		String coloredHint = MyStrings.replaceSecond(toString(), ": ", ": <b1>")
				.replaceFirst(" and ", "</b1> and <b2>")
				.replaceFirst(" on", "</b2> on");
		coloredHint = Html.colorIn(coloredHint);
		final String chainsString = appendChains(new StringBuilder(1024))
				.toString();
		return Html.produce(this, "KrakenFishHint.html"
			, getHintTypeName()		// {0}
			, coloredHint			//  1
			, chainsString			//  2
			, redPots.toString()	//  3
		);
	}

}
