///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2023 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//import diuf.sudoku.utils.Counter;
//import diuf.sudoku.Config;
//
//
///**
// * Filter extends Counter to hide all the details by storing Min and Max in the
// * registry. I implement Closeable and <b>you MUST close(); me</b> to make me
// * useful, so that I store my Min and Max in the registry, so that next run I
// * can read Min and Max back and skip some non-hinting-alignedSets (ie s__t).
// * <p>
// * A*E's which use Filter's should also declare that they implement IFilterer,
// * which is actually implemented in AAlignedSetExclusionBase with methods that
// * just delegate back to me (the Filter). Look at the methods in IFilterer and
// * the below static methods. It is pretty straight forward; and if you need to
// * do "something more interesting" than you can override these default
// * implementations... like a MaxFilter just for instance.
// * <p>
// * Note that IFilterer extends java.io.Closeable, and LogicalSolverTester closes
// * its LogicalSolver which closes each Closeable IHinter, which closes each of
// * it is "filters" (by default), so that each Min and Max is stored in registry.
// * <p>
// * Aligned8Exclusion_1C is currently the only place that Filter is used.
// * @author Keith Corlett
// * <pre>
// * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// *				     @Deprecated: IT IS TOO BLOODY SLOW!
// * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// * Using Filter's in Aligned8Exclusion_1C and _2H proved to be too slow.
// * It is faster with the previous: use a Counter and a manual-filter and just
// * comment them in/out as required. The only gain is the IReporter interface.
// * Not to mention how much of pain in the ass it is to set them all up and
// * buggerise around with the registry, and backup and restore, and OH GOD!!!
// * </pre>
// */
//@Deprecated
//public class Filter extends Counter implements java.io.Closeable {
//
//	/**
//	 * recount makes each filter not filter in this run, but it re-counts the
//	 * hit_min and hit_max which it saves to the registry so that those values
//	 * can be used (as fMin and fMax) in the next run.
//	 * @param filters an arguements array of Filter to be recounted.
//	 */
//	public static final void recount(Filter... filters) {
//		for ( Filter f : filters ) {
//			// so that reject allways returns false (ie everything passes), and
//			// then at the end of the run the hit_min and hit_max are saved to
//			// the registry, so those values go into fMin and fMax next time.
//			f.fMin = Integer.MIN_VALUE;
//			f.fMax = Integer.MAX_VALUE;
//		}
//	}
//
//	// Counter already has min & max, so they are f for Filter.
//	private int fMin, fMax;
//
//	/**
//	 * Filter constructor. name is converted to lowerCase and used as part
//	 * of two registry entry names Min, and Max.
//	 * @param name converted to lowerCase.
//	 */
//	public Filter(String name) {
//		super(name);
//		// getInt reads the raw Preferences
//		fMin = CFG.getInt(name+"Min", Integer.MIN_VALUE);
//		fMax = CFG.getInt(name+"Max", Integer.MAX_VALUE);
//	}
//
//	/**
//	 * The only way for reject to actually reject anything is for it to read
//	 * fMin and fMax from the registry, which is only set by the below close
//	 * method which sets the registry = hit_min and mit_max; which are only
//	 * set by the hit method.
//	 * <p>
//	 * So the story is:<ul>
//	 * <li>the first time you run me I just initialise myself (ie save to the
//	 *    registry); and
//	 * <li>each subsequent time you run me I reject values and then save myself,
//	 *    so if the "accept window" narrows then I follow it.
//	 * </ul>
//	 * @param val
//	 * @return true to reject (ie not process) this aligned set.
//	 */
//	public boolean reject(int val) {
//		super.count(val); // increments cnt and sets min & max
//		if(val>fMax) return true;
//		if(val<fMin) return true;
//		++super.pass;
//		return false;
//	}
//
//	@Override
//	public void close() throws java.io.IOException {
//		super.save();
//	}
//
//}
