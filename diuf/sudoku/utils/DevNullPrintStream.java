///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2023 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.PrintStream;
//import java.io.UnsupportedEncodingException;
//import java.util.Locale;
//
//
///**
// * A drop-in replacement for {@code PrintStream} that does not do anything.
// * That is one way to shut Kylie Mole up.
// * @author Keith Corlett 2016
// */
//public final class DevNullPrintStream extends PrintStream {
//	private static final OutputStream os = new OutputStream() {
//		@Override
//		public void write(int b) throws IOException { }
//	};
//	public static final DevNullPrintStream out = new DevNullPrintStream(os);
//
//	// NB: provide all PrintStream constructors to swap me in/out easily.
//	private DevNullPrintStream(OutputStream out) { super(os); }
//	private DevNullPrintStream(OutputStream out, boolean autoFlush) { super(os); }
//	private DevNullPrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException{ super(os); }
//	private DevNullPrintStream(String fileName) throws FileNotFoundException { super(os); }
//	private DevNullPrintStream(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException { super(os); }
//	private DevNullPrintStream(File file) throws FileNotFoundException { super(os); }
//	private DevNullPrintStream(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException { super(os); }
//
//	public void flush() { }
//	public void close() { }
//	public boolean checkError() { return false; }
//	protected void setError() { }
//	protected void clearError() { }
//	public void write(int b) { }
//	public void write(byte buf[], int off, int len) { }
//
//	public void print(boolean b) { }
//	public void print(char c) { }
//	public void print(int i) { }
//	public void print(long l) { }
//	public void print(float f) { }
//	public void print(double d) { }
//	public void print(char s[]) { }
//	public void print(String s) { }
//	public void print(Object obj) { }
//
//	public void println() { }
//	public void println(boolean x) { }
//	public void println(char x) { }
//	public void println(int x) { }
//	public void println(long x) { }
//	public void println(float x) { }
//	public void println(double x) { }
//	public void println(char x[]) { }
//	public void println(String x) { }
//	public void println(Object x) { }
//
//	public PrintStream format(String format, Object ... args) { return this; }
//	public PrintStream format(Locale l, String format, Object ... args) { return this; }
//	public PrintStream printf(String format, Object ... args) { return this; }
//	public PrintStream printf(Locale l, String format, Object ... args) { return this; }
//	public PrintStream append(CharSequence csq) { return this; }
//	public PrintStream append(CharSequence csq, int start, int end) { return this; }
//	public PrintStream append(char c) { return this; }
//
//}
