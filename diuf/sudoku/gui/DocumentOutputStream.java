///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2022 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.gui;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import javax.swing.text.BadLocationException;
//import javax.swing.text.Document;
//
//class DocumentOutputStream extends OutputStream {
//
//	private final Document doc;
//	public DocumentOutputStream(Document doc) {
//		this.doc = doc;
//	}
//
//    /**
//     * Writes the specified byte to this output stream. The general
//     * contract for {@code write} is that one byte is written
//     * to the output stream. The byte to be written is the eight
//     * low-order bits of the argument {@code b}. The 24
//     * high-order bits of {@code b} are ignored.
//     * <p>
//     * Subclasses of {@code OutputStream} must provide an
//     * implementation for this method.
//     *
//     * @param      b   the {@code byte}.
//     * @exception  IOException  if an I/O error occurs. In particular,
//     *             an {@code IOException} may be thrown if the
//     *             output stream has been closed.
//     */
//	@Override
//	public void write(int i) throws IOException {
//		bytes[len++] = (byte)i;
//		if ( i==10 || len==bytes.length-5) { // flush the buffer
//			if (i==10)
//				--len; // reverse over the 10
//			bytes[len++] = (byte)'<';
//			bytes[len++] = (byte)'b';
//			bytes[len++] = (byte)'r';
//			bytes[len++] = (byte)'>';
//			bytes[len++] = (byte)10;
//			String s = new String(bytes, 0, len);
//			len = 0;
//			try {
//				doc.insertString(doc.getEndPosition().getOffset(), s, null);
//			} catch (BadLocationException ex) {
//				throw new IOException("Bugger!", ex);
//			}
//		}
//	}
//	private int len = 0;
//	private int[] bytes = new byte[1024];
//
//}
