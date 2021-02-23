///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2020 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.gui;
//
//import java.io.PrintStream;
//import javax.swing.text.Document;
//
//class DocumentPrintStream extends PrintStream {
//
//	public DocumentPrintStream(Document doc) {
//		super(new DocumentOutputStream(doc));
//	}
//
////	private final JEditorPane pane;
////	private final Document doc;
////	public DocumentPrintStream(JEditorPane pane, Document doc) {
////		super(new OutputStream() {
////			@Override
////			public void write(int b) throws IOException { }
////		});
////		this.pane = pane;
////		this.doc = doc;
////	}
////	@Override
////	public PrintStream append(CharSequence csq, int start, int end) {
////		try {
////			String s = csq.toString();
////			doc.insertString(doc.getEndPosition().getOffset(), s, null);
////			return this;
////		} catch (BadLocationException ex) { // from insertString
////			throw new RuntimeException("append error", ex);
////		}
////	}
//
//}
