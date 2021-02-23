/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.io.IO;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Extracted pasteClipboardTo from Grid coz it imports java.awt.datatransfer.*;
 *
 * @author Keith Corlett 2020-06-06
 */
public class GridFactory {

	/** Paste the contents of the O/S clipboard into the given grid: The
	 * expected format is that produced by toString().
	 * @param grid Grid to receive the wisdom of the clipboard.
	 * @throws java.io.IOException when it failed. */
	public static void pasteClipboardTo(Grid grid) throws IOException {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(grid);
		if ( contents == null )
			throw new IOException("Empty clipboard");
		final DataFlavor flavor = new DataFlavor(String.class, "Plain text");
		try {
			grid.load(IO.slurp(flavor.getReaderForText(contents)));
			if ( !grid.isMaybesLoaded )
				grid.rebuildMaybesAndS__t();
		} catch (UnsupportedFlavorException ex) {
			throw new IOException("Bad flavor \"Plain text\"", ex);
		}
	}

}
