/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;


/**
 * Dump System environment and properties to stdout.
 *
 * @author Keith Corlett 2022-01-23
 */
public class PropertiesDump {

	public static void main(String[] args) {
		try {
			System.out.println("\nSystem.getEnv:");
			System.getenv().forEach((n,v) -> System.out.format("%s=%s\n", n, v));
			System.out.println("\nSystem.getProperties:");
			System.getProperties().forEach((n,v) -> System.out.format("%s=%s\n", n, v));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
