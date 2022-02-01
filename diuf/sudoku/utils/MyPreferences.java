/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.io.IO;
import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

/**
 * MyPreferences is a-drop-in-replacement for java.util.prefs.Preferences
 * using a file in the local-file-system as a backing store instead of the
 * Windows registry.
 * <p>
 * I'm not certain that avoiding the Windows registry is a good idea, but
 * I do know enough to know that Microsoft will stuff-up Java every chance
 * they get, in an endless drive to make people pay more for software, so
 * I'm warranted risk-wise, if not design-wise.
 * <p>
 * This is, AFAIK, the simplest possible Preferences implementation. This
 * 117 line class replaces something like 10,000 in standard Preferences,
 * Factories, implementations, and all of there moving parts, plus all of
 * the actual registry code. So yeah, this is just LESS risk, I think.
 * <p>
 * I am a {@code java.util.LinkedHashMap<String,String>} so I expose the
 * "raw" String methods from java.util.Map: {@code String get(String)} and
 * {@code String put(String,String)}.
 * <p>
 * I use my IO class to load-from and save-to file. Any IOExceptions are
 * wrapped in a BackingStoreException as per
 * {@link java.util.prefs.Preferences}.
 * <p>
 * I extend LinkedHashMap instead of HashMap so that the order of Settings
 * persist as listed.
 *
 * @author Keith Corlett 2021-07-09
 */
public class MyPreferences extends java.util.LinkedHashMap<String,String> {
	
	private static final long serialVersionUID = 62909151862010582L;

	public final File file;
	public final boolean trouble;

	// load the file, eating any Exceptions, so it's saved on close.
	public MyPreferences(File file) {
		this.file = file;
		boolean anyTrouble;
		try {
			IO.slurp(this, file);
			anyTrouble = false;
		} catch (Exception ex) {
			// nb: can't use Log here, coz it uses Settings (chicken/egg)
			System.err.println("MyPreferences ctor: load failed: "+file);
			anyTrouble = true;
		}
		trouble = anyTrouble;
	}

	// --------------------------- int ---------------------------

	public int getInt(String name) {
		return Integer.parseInt(super.get(name));
	}
	public int getInt(String name, int defualt) {
		int result;
		try {
			String s = super.get(name);
			if ( s == null )
				result = defualt;
			else
				result = Integer.parseInt(s);
		} catch (NumberFormatException eaten) {
			result = defualt;
		}
		return result;
	}
	public void putInt(String name, int value) {
		super.put(name, Integer.toString(value));
	}

	// --------------------------- boolean ---------------------------

	public boolean getBoolean(String name) {
		return Boolean.parseBoolean(super.get(name));
	}
	public boolean getBoolean(String name, boolean defualt) {
		boolean result;
		try {
			String s = super.get(name);
			if ( s == null )
				result = defualt;
			else
				result = Boolean.parseBoolean(s);
		} catch (NumberFormatException eaten) {
			result = defualt;
		}
		return result;
	}
	public void putBoolean(String name, boolean value) {
		super.put(name, Boolean.toString(value));
	}

	// --------------------------- string ---------------------------

	public String get(String name, String defualt) {
		String s = super.get(name);
		final String result;
		if ( s == null )
			result = defualt;
		else
			result = s;
		return result;
	}

	// --------------------------- save ---------------------------

	public void flush() throws BackingStoreException {
		try {
			IO.save(this, file);
		} catch (IOException ex) {
			throw new BackingStoreException(ex);
		}
	}

}
