///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2021 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.utils;
//
//
///**
// * Fast sine and cosine approximations.
// */
//public final class FastSinCos {
//
//	private static final double B = 4.0 / Math.PI;
//	private static final double C = -4.0 / (Math.PI * Math.PI);
//	private static final double P = 0.218;
//	private static final double HPI = Math.PI / 2.0;
//	private static final double PI2 = Math.PI * 2.0;
//	private static final double SPI = Math.PI * 1.5;
//
//	public static double wrap(double angle) {
//		angle %= PI2;
//		if (angle > Math.PI)
//			angle -= PI2;
//		else if (angle < -Math.PI)
//			angle += PI2;
//		assert angle >= -Math.PI && angle <= Math.PI;
//		return angle;
//	}
//
//	/**
//	 * Fast sine approximation
//	 * @param theta the argument
//	 * @return sin(theta)
//	 */
//	public static double fastSin(double theta) {
//		return fastSin0(wrap(theta));
//	}
//
//	/**
//	 * Fast cosine approximation
//	 * @param theta the argument
//	 * @return cos(theta)
//	 */
//	public static double fastCos(double theta) {
//		return fastSin0(wrap(theta + HPI));
//	}
//
//	/**
//	 * Fast sine approximation. Slightly faster than
//	 * {@link #fastSin(double)} but the argument must
//	 * be within the [-&pi;..&pi;] range!
//	 * @param theta the argument.
//	 * @return sin(theta)
//	 */
//	public static double fastSin0(double theta) {
//		double y = B * theta + C * theta * Math.abs(theta);
//		y = P * (y * Math.abs(y) - y) + y;
//		return y;
//	}
//
//	/**
//	 * Fast cosine approximation. Can be slightly faster than
//	 * {@link #fastCos(double)} but the argument must
//	 * be within the [-&pi;..&pi;] range!
//	 * @param theta the argument.
//	 * @return cos(theta)
//	 */
//	public static double fastCos0(double theta) {
//		if (theta > HPI) {
//			theta -= SPI; // - 1.5pi
//		} else {
//			theta += HPI; // + 0.5pi
//		}
//		return fastSin0(theta);
//	}
//}