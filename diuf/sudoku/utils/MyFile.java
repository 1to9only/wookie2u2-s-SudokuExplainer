/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.SourceID;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * MyFile exposes static helper methods to help me deal with java.io.File's.
 * Ergo this is stuff I think java.io.File could make a better fist of dealing
 * with for us all, instead of having every mad bastard roll there own badly.
 * @author Keith Corlett 2020 Mar 22 (methods exhumed from LogicalSolverTester)
 */
public final class MyFile {

	/** System.getProperty("os.name") returns "Windows 10" on my box. */
	public static final String OS_NAME = System.getProperty("os.name");

	/** DILDOWS is not final, so you can flip it if it causes problems when
	 * accessing files on a samba drive (or whatever), but I suggest you flip it
	 * back in a finally block, so that native file-system rules are respected;
	 * unless of course your whole project is on a samba drive. Sigh. */
	public static boolean WINBLOWS = OS_NAME.toLowerCase().startsWith("windows");

	/**
	 * OS_NAME aware filePath.endsWith(ext);
	 * <pre>
	 * NB: I do not know wether or not mac-OS file-system is case sensitive,
	 *     but I presume so, coz that'd be reasonable, unlike winblows, coz
	 *     anybody who based anything on Windows is an idiot; and Apple folk
	 *     are many things, but they are not idiots.
	 * </pre>
	 * @param filename
	 * @param ext
	 * @return true if the given filename endsWith the given ext, respecting
	 * the case-sensitivity rules of the operating system upon which I run.
	 */
	public static boolean nameEndsWith(String filename, String ext) {
		// The Windows file-system IS NOT case sensitive
		if ( WINBLOWS )
			return filename.toLowerCase().endsWith(ext.toLowerCase());
		// The *nix file-system IS case sensitive
		return filename.endsWith(ext);
	}

	public static boolean nameEndsWith(File file, String ext) {
		return nameEndsWith(file.getName(), ext);
	}

	/**
	 * OS_NAME aware filePath.contains(target);
	 * <pre>
	 * NB: I do not know wether or not mac-OS file-system is case sensitive,
	 *     but I presume so, coz that'd be reasonable, unlike winblows, coz
	 *     anybody who based anything on Windows is an idiot; and Apple folk
	 *     are many things, but they are not idiots.
	 * </pre>
	 * @param filepath
	 * @param target
	 * @return true if the given filename contains the given target, respecting
	 * the case-sensitivity rules of the operating system upon which I run.
	 */
	public static boolean nameContains(String filepath, String target) {
		if ( WINBLOWS )
			// Windows file-system is not case sensitive
			return filepath.toLowerCase().contains(target.toLowerCase());
		return filepath.contains(target);
	}

	/**
	 * Does fileName exist?
	 *
	 * @param filename
	 * @return {@code new File(filename).exists()} else false on Exception.
	 */
	public static boolean exists(String filename) {
		try {
			return new File(filename).exists();
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Throws a new FileNotFoundException if a File of fileName does not exist.
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public static void mustExist(String filename) throws FileNotFoundException {
		final File file = new File(filename);
		if ( !file.exists() )
			throw new FileNotFoundException(file.getAbsolutePath());
	}

	/**
	 * Return filename minus the last period and thereafter.
	 *
	 * @param filename
	 * @return
	 */
	public static String minusExt(String filename) {
		int i = filename.lastIndexOf('.');
		if ( i < 0 )
			return filename;
		return filename.substring(0, i);
	}

}
