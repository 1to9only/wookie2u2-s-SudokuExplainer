///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2020 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//import java.io.Closeable;
//
///**
// * The Closables static helper class exposes static helper methods for dealing
// * with instances of the java.io.Closeable interface.
// *
// * It was written to help deal with the diuf.sudoku.solver.hinters.align.Filter
// * class, which is becoming a problemo, and may be shot!
// *
// * @author Keith Corlett 2020 Feb
// */
//public final class Closeables {
//
//	/**
//	 * Close all these Closeables, eating any exceptions. Note that close()
//	 * declares that it throws IOException, as per java.io.Closeable interface,
//	 * but never actually throws anything; instead it eats Throwable which
//	 * includes ALL Errors and Exceptions, be they IO or otherwise.
//	 * <p>
//	 * The point is that if close fails then there is NOTHING that anyone can
//	 * actually DO about it; so the little bastard MUST die silently, so that
//	 * we can get on with closing everything. If your flush fails that's just
//	 * tough s__t: "Houston we have a floater". But when was the last time a
//	 * flush actually failed? Alright maybe it happened once or twice on tape.
//	 * But I'm not even USING my hard-disk any longer (or that either), let
//	 * alone bloody tape, let alone a punched bloody card. I can rent an
//	 * "assured network" FFS! Though I wouldn't be stoopid enuf to take the
//	 * name seriously. Give it maybe 50 years and they'll be able to actually
//	 * assure the bastard.
//	 * <p>
//	 * <pre>{@code
//	 *     So, Q: Where do old habits go to die?
//	 *         A: Church!
//	 * }</pre>
//	 * <p>
//	 * The alternative worste case scenario is an endless loop: where an error
//	 * handler catches an Exception so it starts closing everything, which
//	 * causes an Exception that's sent through the same error handler so it
//	 * starts closing everything.
//	 * <p>
//	 * But it's only waifer thin! This is my humble attempt decreosotic upward
//	 * mismanagement. The King is Dead. ____ 'im, e's only a peasant! Long live
//	 * Tezza. I liked Tezza. Suddenly I feel like spam.
//	 * <p>
//	 * I suggest we put this rather Chineese solution into a Croseable interface
//	 * which declares the crose() method, which does not throw any exceptions.
//	 * Obviously we'll also need a twy keyword for autocrosing, and we'll call
//	 * the rewrite of java.io.* the Jones project, and we wewease it with fresh
//	 * fruit, possibly a banana.
//	 * <p>
//	 * Personally, I like the unclosable stderr stream solution: so that calls
//	 * to close() stderr do nothing and just return "ok", so that stderr cannot
//	 * be closed; so that everyone can always write to stderr without fear that
//	 * it has already been closed... and the very last thing the O/S does when
//	 * shutting-down each process is flush and close it's stderr stream, using
//	 * the secret crose method. Racism is always an option, but <b>NEVER</b> to
//	 * be taken seriously, and that DOES mean NEVER. Never say never. Them's
//	 * the rules Macca.
//	 * <p>
//	 * 
//	 * @param closeables An arguements array of Closeable (ie Filter) to close.
//	 */
//	public static void close(Closeable... closeables) {
//		for ( Closeable closeable : closeables )
//			if ( closeable != null )
//				try {
//					closeable.close();
//				} catch (Throwable eaten) { // never thrown by Filter!
//					// Do nothing.
//				}
//	}
//
//}
