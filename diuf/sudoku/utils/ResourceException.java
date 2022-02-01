/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * ResourceException is thrown by Html.actuallyLoad when either a resource 
 * (a HTML-file that's been packed into the .jar file) is not found or an
 * IOException (cause) is thrown when we're loading the little fooker.
 * @author Keith Corlett 2018 Mar
 */
public final class ResourceException extends Exception {
	private static final long serialVersionUID = 124897010L;
	public ResourceException() { }
	public ResourceException(String msg) { super(msg); }
	public ResourceException(String msg, Throwable cause) { super(msg, cause); }
}
