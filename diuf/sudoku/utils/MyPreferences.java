/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.io.IO;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.prefs.BackingStoreException;

/**
 * MyPreferences is an alternative to {@link java.util.prefs.Preferences}
 * that uses a plain text-file in the local-file-system as the backing store,
 * instead of the Windows registry, or whatever, to provide user settings.
 * I hope that its portable between Windows/*nix/Mac. Note that this only a
 * part replacement. It does NOT support system settings, just user settings,
 * which is all I need.
 * <p>
 * I am not certain that avoiding the Windows registry is a good idea, but I
 * know enough to know that Microsoft will screw Java every chance they get, in
 * a relentless drive to make people pay more for fancy software they do not
 * want or need, so MyPreferences is warranted risk-wise, if not design-wise.
 * <p>
 * This is, AFAIK, the simplest possible Preferences implementation. This short
 * class replaces something like 10,000 lines of code in java.util.prefs
 * Preferences, Factories, implementations, and all of there moving parts, plus
 * all registry code. So yeah, this is LESS risk, I think. If Microsoft breaks
 * its own file-system to stop people using Java, then they missed. I simply
 * refuse to support an applet. Applets where a nice idea, that failed.
 * <p>
 * I am a {@code LinkedHashMap<String,String>} hence I expose the raw Map,
 * including {@code String get(String)} and {@code String put(String,String)}.
 * <p>
 * My IO class loads-from and saves-to file. Any IOExceptions are wrapped in a
 * BackingStoreException as per {@link java.util.prefs.Preferences}.
 * <p>
 * I extend LinkedHashMap instead of HashMap, so natural order persists (not
 * the pseudorandom order from a raw HashMap).
 * <p>
 * This is just my humble attempt. It cannot replace Preferences. It WILL have
 * bugs, and unkownable weaknesses.
 *
 * @author Keith Corlett 2021-07-09
 */
public final class MyPreferences extends MyLinkedHashMap<String, String> {

	private static final long serialVersionUID = 62909151862010582L;

	private final File configFile;
	public final boolean loadFailed;

	// load the file, eating any Exceptions, so it is saved on close.
	public MyPreferences(File configFile) {
		this.configFile = configFile;
		boolean itBroke;
		try {
			IO.slurpConfig((Map<String,String>)this, configFile);
			itBroke = false;
		} catch (IOException ex) {
			// nb: No Log here, incase it uses Config (chicken/egg)
			System.err.println("MyPreferences constructor: load failed: " + configFile);
			itBroke = true;
		}
		loadFailed = itBroke;
	}

	// --------------------------- int ---------------------------
	public int getInt(final String name) {
		return Integer.parseInt(super.get(name));
	}

	public int getInt(final String name, final int defualt) {
		try {
			return Integer.parseInt(super.get(name));
		} catch (NumberFormatException eaten) {
			return defualt;
		}
	}

	public void putInt(final String name, final int value) {
		super.put(name, Integer.toString(value));
	}

	// ------------------------ int array ------------------------
	/**
	 * getIntArray reads 'name' setting, and returns it as a new int array.
	 * Example: {@code "[0, 1, 2]" -> new int[] {0, 1, 2} }
	 *
	 * @param name the name of the setting to read
	 * @throws NumberFormatException, or RuntimeException from StringTokeniser
	 * @return a new int array; else throws
	 */
	public int[] getIntArray(final String name) {
		final String s = super.get(name);
		if ( s == null )
			return null;
		return IntArray.parse(s);
	}

	/**
	 * getIntArray reads 'name' setting, and returns it as a new int array; else
	 * returns defualt if anything goes bad. Example:
	 * {@code "[0, 1, 2]" -> new int[] {0, 1, 2} }
	 * <p>
	 * This is a convenience method. There is more than one way to do it.
	 *
	 * @param name the name of the setting to read
	 * @param defualt to return if it goes tits-up; null works if you need to
	 * assure yourself the setting was read, so you post-test it; or you could
	 * just use the getIntArray(name), sans defualt which throws an exception.
	 * @return a new int array; else defualt
	 */
	public int[] getIntArray(final String name, final int[] defualt) {
		try {
			final int[] a = getIntArray(name);
			if ( a == null )
				return defualt;
			return a;
		} catch (Exception eaten) {
			return defualt;
		}
	}

	public void putIntArray(final String name, final int[] values) {
		super.put(name, IntArray.format(values));
	}

	// --------------------------- boolean ---------------------------
	public boolean getBoolean(final String name) {
		return Boolean.parseBoolean(super.get(name));
	}

	public boolean getBoolean(final String name, final boolean defualt) {
		try {
			final String s = super.get(name);
			if ( s == null )
				return defualt;
			return Boolean.parseBoolean(s);
		} catch (Exception eaten) {
			return defualt;
		}
	}

	public void putBoolean(final String name, final boolean value) {
		super.put(name, Boolean.toString(value));
	}

	// --------------------------- string ---------------------------
	public String get(final String name, final String defualt) {
		final String s = super.get(name);
		if ( s == null )
			return defualt;
		return s;
	}

	// --------------------------- save ---------------------------
	public void flush() throws BackingStoreException {
		try {
			IO.saveConfig(this, configFile);
		} catch (IOException ex) {
			throw new BackingStoreException(ex);
		}
	}

}
