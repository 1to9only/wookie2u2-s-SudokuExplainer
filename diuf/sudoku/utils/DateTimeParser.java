/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.MyStrings.SB;
import java.util.Calendar;

/**
 * Parse date-time tokens into the current date and time values.
 *
 * @author Keith Corlett 2023-08-25
 */
public class DateTimeParser {

	// Read a "word" from $s, starting at $i, and stopping at the first
	// nonalpha character or $len, whichever comes first.
	// The given $i is preset to $+1 (the char following the dollar sign).
	private static String readWord(final String s, int i, final int len) {
		char c;
		final StringBuilder token = SB(s.length()); // result
		for ( ; i<len; ++i ) {
			if ( ((c=s.charAt(i))>='A' && c<='Z') || (c>='a' && c<='z') ) {
				token.append(c);
			} else {
				break;
			}
		}
		return token.toString();
	}

	private int parse(final int x, final int len) {
		result.append(Frmt.lfill(calendar.get(x), len));
		return len + 1;
	}

	private int parse(final int x, final int mod, final int len) {
		result.append(Frmt.lfill(calendar.get(x) % mod, len));
		return len + 1;
	}

	final StringBuilder result = SB(32);
	final Calendar calendar = Calendar.getInstance();

	// Translate: SudokuExplainer.$YYYY-$MM-$DD.$hh-$mm-$ss.log
	//      into: SudokuExplainer.2023-08-20-08-44-41.log
	public String parse(final String s) {
		if ( !s.contains("$") ) {
			return s;
		}
		String token;
		int p = 0; // previous index
		final int len = s.length();
		result.ensureCapacity(len);
		for ( int i = s.indexOf("$"); ; ) { // index
			// append the prefix (previous..startOfNextToken)
			result.append(s.substring(p, i));
			// previous = current index
			p = i;
			// read the token, and append its translation
			switch ( token=readWord(s, i+1, len) ) {
			case "YYYY": p += parse(Calendar.YEAR, 4); break;
			case "YY":   p += parse(Calendar.YEAR, 100, 2); break;
			case "MM":   p += parse(Calendar.MONTH, 2); break;
			case "DD":   p += parse(Calendar.DAY_OF_MONTH, 2); break;
			case "hh":   p += parse(Calendar.HOUR, 2); break;
			case "mm":   p += parse(Calendar.MINUTE, 2); break;
			case "ss":   p += parse(Calendar.SECOND, 2); break;
			default:
				// unknown token: pass-through as-is, for other parsers.
				// there is no log to log-it to, yet.
				System.out.println("WARN: "+Log.me()+": uknown token: $"+token);
				result.append('$').append(token);
				p += token.length() + 1;
			}
			// find the start of the next token
			if ( (i=s.indexOf("$", i+1)) < 0 ) {
				// no next: append the suffix and return toString
				return result.append(s.substring(p, s.length())).toString();
			}
		}
	}

}
