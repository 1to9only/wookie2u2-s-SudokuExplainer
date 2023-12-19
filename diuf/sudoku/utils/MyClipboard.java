/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.io.IO;
import static diuf.sudoku.utils.MyStrings.SB;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * MyClipboard exposes static method to simplify system Clipboard access.
 *
 * @author Keith Corlett 2023-09-17
 */
public class MyClipboard {

	public static String getText() {
		try {
			final String flavorName = "Plain text";
			final DataFlavor flavor = new DataFlavor(String.class, flavorName);
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			final Transferable contents;
			if ( (contents=cb.getContents(null)) == null )
				return null; // nothing on the Clipboard
			try {
				final StringBuilder sb = SB(4096);
				// trim removes the trailing NL appended by slurpSB. Sigh.
				return IO.slurpSB(sb, flavor.getReaderForText(contents)).toString().trim();
			} catch (UnsupportedFlavorException ex) { // getReaderForText
				throw new IOException("Bad flavor "+flavorName, ex);
			} catch (IOException ex) { // slurp
				throw new IOException("slurp failed", ex);
			} catch (Exception ex) { // load (I guess)
				throw new IOException("guess load failed", ex);
			}
		} catch (IOException ex) {
			return null;
		}
	}

	private MyClipboard() { }
}
