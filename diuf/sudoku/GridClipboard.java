/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.io.IO;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.io.IOException;

/**
 * All the clipboard related methods from Grid, to keep my s__t together.
 *
 * @author Keith Corlett 2020-06-06
 */
public class GridClipboard {

	/**
	 * Paste the contents of the operating-systems clipboard into this grid.
	 * The expected format is that produced by {@link Grid#toString}.
	 *
	 * @param grid Grid to repopulate from the clipboard.
	 * @throws java.io.IOException when it failed.
	 */
	public static void pasteTo(Grid grid) throws IOException {
		final String flavorName = "Plain text";
		final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		final Transferable contents = cb.getContents(grid);
		if ( contents == null ) {
			throw new IOException("Empty clipboard");
		}
		final DataFlavor flavor = new DataFlavor(String.class, flavorName);
		try {
			grid.load(IO.slurp(flavor.getReaderForText(contents)));
		} catch (UnsupportedFlavorException ex) { // getReaderForText
			throw new IOException("Bad flavor "+flavorName, ex);
		} catch (IOException ex) { // slurp
			throw new IOException("slurp failed", ex);
		} catch (Exception ex) { // load (I guess)
			throw new IOException("guess load failed", ex);
		}
	}

	/**
	 * Copies the given String to Systems clipboard.
	 * @param s string to copy to clipboard
	 */
	public static void copyTo(String s) {
		StringSelection ss = new StringSelection(s);
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard clipboard = tk.getSystemClipboard();
		clipboard.setContents(ss, ss);
	}

	/**
	 * Reads the String content of the drop event and returns it.
	 *
	 * @param dtde from {@code DropTargetAdapter.drop}
	 * @return contents of the drop event.
	 * @throws UnsupportedFlavorException
	 * @throws IOException
	 */
	public static String read(DropTargetDropEvent dtde)
			throws UnsupportedFlavorException, IOException {
		dtde.acceptDrop(DnDConstants.ACTION_COPY);
		// it works only when I hardcode it!
		DataFlavor flavor = dtde.getCurrentDataFlavors()[2];
		assert "java.awt.datatransfer.DataFlavor[mimetype=text/plain;representationclass=java.lang.String]"
				.equals(flavor.toString());
		Transferable transferable = dtde.getTransferable();
		String s = (String)transferable.getTransferData(flavor);
		dtde.dropComplete(true);
		return s;
	}

}
