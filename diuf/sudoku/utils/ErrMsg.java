///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2020 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//import javax.swing.JOptionPane;
//
///**
// * A generic error message.
// * Consists of a message with "{0}", "{1}", ... patterns
// * and zero or more arguments. The "{0}", "{1}", ...
// * patterns are replaced by the arguments when the
// * message is converted to a string by {@link #toString()}.
// */
//public final class ErrMsg {
//
//	public final String message;
//	public final boolean isFatal;
//	public final Object[] args;
//
//	public ErrMsg(String message, boolean isFatal, Object... args) {
//		this.message = message;
//		this.isFatal = isFatal;
//		this.args = args;
//	}
//
//	public int type() {
//		return isFatal ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
//	}
//
//	@Override
//	public String toString() {
//		String result = message;
//		for (int i = 0; i < args.length; i++) {
//			String pattern = "{" + i + "}";
//			String value = (args[i] == null ? "" : args[i].toString());
//			result = result.replace(pattern, value);
//		}
//		return result;
//	}
//
//}
