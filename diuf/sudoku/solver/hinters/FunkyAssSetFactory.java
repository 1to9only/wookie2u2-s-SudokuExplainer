/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMySet;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.StringPrintStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FunkyAssSetFactory creates {@link FunkyAssSet}, but when {@link #REPORT} you
 * get a free upgrade to {@link FunkyAssSetSized} which extends FunkyAssSet.
 * The FunkyAssSetSized constructor registers itself with FunkyAssSetFactory,
 * and its resizeMonitor tracks its maxCapacity, then you call report at the
 * end of a batch-run, to report the maxCapacity of each FunkyAssSetSized.
 * <p>
 * Asat 2023-09-10 FunkyAssSetFactory is used ONLY in KrakenFisherman to create
 * its effects set, which is large enough to not need to grow, so this whole
 * fiasco is now useless in practice, but has been retained anyway, because I
 * still think its a good idea. I just wish I was smart enough to implement it
 * all totally generically: HashSetFactory remembers how big they need to be.
 * <p>
 * FYI: formerly FunkyAssSetFactory was used extensively in the chainers, to
 * create ?many-millions? of good-sized-HashSets, but we now use ONE permanent
 * {@link LinkedMatrixAssSet} in there place (hammering clear), for speed.
 *
 * @author Keith Corlett 2023-06-24 exhumed from chains and refactor all.
 */
public final class FunkyAssSetFactory {

	/**
	 * true prints the maximum capacity (not size) of each FunkyAssSet, to
	 * enspeedonate by making initialCapacity cover about 90% of use cases.
	 * <p>
	 * If a set needs to increase capacity once its not a problem. It effects
	 * performance when the set grows (an O(n) operation) multiple times.
	 * It kills performance when ALL sets grow ALL the time. This is a reason
	 * to reuse sets, and having done that I thought it might be nice if each
	 * set could tell me how big it wants to be, resulting in this Factory.
	 */
	public static final boolean REPORT = false; // @check false

	public static IAssSet newFunkyAssSet(final int initialCapacity, final float loadFactor, final boolean isOn) {
		if ( REPORT ) {
			return new FunkyAssSetSized(initialCapacity, loadFactor, isOn);
		}
		return new FunkyAssSet(initialCapacity, loadFactor, isOn);
	}

	public static IAssSet newFunkyAssSet(final boolean isOn, final IMySet<Ass> src, final boolean isClearing) {
		if ( REPORT ) {
			return new FunkyAssSetSized(isOn, src, isClearing);
		}
		return new FunkyAssSet(isOn, src, isClearing);
	}

	/**
	 * Report the FunkyAssSetSizes. See {@link AAdamsD} for an explanation.
	 */
	public static void report() {
		Log.teef("%s", sizes());
	}

	// report and also handy for debugging
	public static String sizes() {
		PrintStream out = new StringPrintStream();
		OverallSizes.THE.report(out);
		return out.toString();
	}

	private static final Map<String, FunkyAssSetSized> MAP = new LinkedHashMap<>();

	// You cannot hide a static inner class constructor. Sigh.
	private FunkyAssSetFactory() {}

	/**
	 * OverallSizes is a helper class that prints the max-capacity of each
	 * {@link FunkyAssSetSized}, so you can work-out the best initialCapacity
	 * for each HashSet.
	 * <p>
	 * I want to avoid wasting time repeatedly O(n) copying into a new table
	 * because I was too lazy to workout how big the HashSet-table needed to be
	 * in the first place. Hence I am lazy, as apposed to merely lazy.
	 * <p>
	 * HashMap-too-small causes oversized buckets, which can put a rather large
	 * performance impost on each access (it runs like a three-legged dog).
	 * HashMap-too-large is also a bit slow, but too big is less slow than too
	 * small, hence medium is nothing but trouble, ie the best compromise.
	 * <p>
	 * What you really want is an initialCapacity that covers 90% of use-cases,
	 * and let it grow ONCE for the other 10% of use-cases. You cant always get
	 * what you want, but if you try sometimes then you get what is as fast as
	 * possible, so you get to see Lucies knees, obviously!
	 *
	 * @author Keith Corlett 2017 IIRC
	 */
	private static final class OverallSizes extends LinkedHashMap<String, Integer> {

		private static final long serialVersionUID = 345675231L;

		static final OverallSizes THE = new OverallSizes();

		private void max(final String id, final int maxCapacity) {
			final Integer existing = OverallSizes.THE.get(id);
			if ( existing==null || maxCapacity > existing ) {
				OverallSizes.THE.put(id, maxCapacity);
			}
		}

		private void report(final PrintStream out) {
			if ( isEmpty() ) {
				out.format("WARN: %s: none\n", Log.me());
				return;
			}
			out.println();
			for ( Map.Entry<String,Integer> e : entrySet() ) {
				out.format("%s %,6d\n", e.getKey(), e.getValue());
			}
			out.flush();
		}

		private OverallSizes() {}

	}

	/**
	 * A FunkyAssSetSized is a {@link FunkyAssSet} that tracks its maxSize with
	 * {@link FunkyAssSetFactory}, to workout how big your FunkyAssSet needs to
	 * be (determinable only at runtime). Switch on {@link #REPORT}.
	 * Run the batch. See the bottom the log. Simples.
	 * <p>
	 * Each FunkyAssSet2 constructed registers itself with FunkyAssSetSizes.
	 * NOTE: A good initialCapacity covers atleast 90% of cases (100% if thats
	 * not too big, or you are creating the set ONCE). Repeated growth is a pet
	 * hate of mine. I find it exponentially retarded/annoying! Hence I have
	 * gone to the effort of this roundabout-way of avoiding it. Maybe this can
	 * be done simpler/better/easier/generically by someone with a BIG IQ?
	 *
	 * @author Keith Corlett 2023-06-24 Exhumed from chains and refactored.
	 */
	public static final class FunkyAssSetSized extends FunkyAssSet { // 1465 in 416 secs at 0.284

		private int maxCapacity;
		private final String id;

		/**
		 * Private! Use factory {@link FunkyAssSetSizes.newFunkyAssSet}.
		 *
		 * @param initialCapacity
		 * @param loadFactor
		 * @param isOn
		 */
		private FunkyAssSetSized(final int initialCapacity, final float loadFactor
				, final boolean isOn) {
			super(initialCapacity, loadFactor, isOn);
			super.map.resizeMonitor = (newSize) -> resized(newSize);
			// the line of code which constructs this set.
			id = Thread.currentThread().getStackTrace()[4].toString();
			maxCapacity = initialCapacity;
			FunkyAssSetFactory.MAP.put(id, this);
		}

		/**
		 * Private! Use factory {@link FunkyAssSetSizes.newFunkyAssSet}.
		 * <p>
		 * This copy-constructor clears the given {@code IMySet<Ass> c} if
		 * isClearing: it is more efficient to poll() each element out of $c than
		 * it is to addAll and then clear $c.
		 *
		 * @param isOn
		 * @param src
		 * @param isClearing
		 */
		private FunkyAssSetSized(final boolean isOn, final IMySet<Ass> src, final boolean isClearing) {
			super(src.size(), 1F, isOn);
			super.map.resizeMonitor = (newSize) -> resized(newSize);
			// the line of code which constructs this set.
			id = Thread.currentThread().getStackTrace()[4].toString();
			maxCapacity = src.size();
			FunkyAssSetFactory.MAP.put(id, this);
			if ( isClearing ) {
				for ( Ass ass=src.poll(); ass!=null; ass=src.poll() ) {
					add(ass);
				}
			} else {
				addAll(src);
			}
		}

		protected void resized(final int newCapacity) {
			if ( newCapacity > maxCapacity ) {
				maxCapacity = newCapacity;
				OverallSizes.THE.max(id, newCapacity);
			}
		}

	}

}
