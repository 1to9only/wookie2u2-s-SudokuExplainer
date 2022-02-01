/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.io.IO;
import diuf.sudoku.Ass;
import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.MyStrings.replaceAll;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A Helper class that loads and formats HTML.
 * @author Keith Corlett
 */
public final class Html {

	// Welcome.html is the largest .html file at just under 2K (1,995 bytes)
	private static final int BUFFER_SIZE = 2 * 1024;

	public static final Map<String, String> CACHE = new HashMap<>(64, .098F);

	// the maximum size of the arguments array passed to the format method.
	private static final int MAX_ARGS = 32;

	public static void save(Object caller, String filename, String content
			, boolean wantCache) throws ResourceException {
		final String key = getKey(caller, filename);
		final String classpath = "test\\" + caller.getClass().getPackage().getName().replace('.', '\\');
		final String filepath = IO.HOME + classpath + '\\' + filename;
		try ( PrintStream output = new PrintStream(filepath) ) {
			if ( output == null )
				throw new ResourceException("Resource not found: "+key);
			output.print(content);
			if ( wantCache )
				CACHE.put(key, content); // cache content for the coming load
		} catch (IOException ex) {
			throw new ResourceException("Resource save error: "+key+NL
					+ex.toString(), ex);
		}
	}

	public static String actuallyLoad(Object caller, String filename
			, String key) throws ResourceException {
		if(key==null) key=getKey(caller, filename);
		final Class<?> cc = caller.getClass();
		final InputStream input = cc.getResourceAsStream(filename);
		if ( input == null )
			throw new ResourceException("Resource not found: "+key);
		try ( BufferedReader reader = new BufferedReader(
				new InputStreamReader(input, "ISO-8859-1")) ) {
			char[] buffer = new char[BUFFER_SIZE];
			StringBuilder mysb = new StringBuilder(BUFFER_SIZE); // grows
			int n;
			while ( (n=reader.read(buffer)) > 0 )
				mysb.append(buffer, 0, n);
			return mysb.toString(); // copies char array but like whatever!
		} catch (IOException ex) {
			throw new ResourceException("Resource load error: "+key+NL
					+ex.toString(), ex);
		}
	}

	public static String getKey(Object caller, String filename) {
		return caller.getClass().getName()+"#"+filename;
	}

	/**
	 * Load the specified HTML file.
	 * @param caller the caller object (used to fetch the class loader)
	 * @param filename the name of the HTML file
	 * @param wantColor true if you want any custom color tags in the HTML
	 *  replaced with standard HTML font tags.
	 * @param wantCache true if it's OK to store the HTML content in a HashMap
	 *  instead of repeatedly extracting him out of the jar-file. If you're
	 *  running low on RAM then the latter might actually be faster overall.
	 * @return the content of the HTML file
	 * @throws diuf.sudoku.utils.ResourceException
	 */
	public static String cachedLoad(Object caller, String filename
			, boolean wantColor, boolean wantCache) throws ResourceException {
		// first look in the CACHE (if wantCache)
		final String key = getKey(caller, filename);
		String html;
		if ( wantCache ) {
			html = CACHE.get(key);
			if ( html != null )
				return html;
		}
		// read the html-file
		html = actuallyLoad(caller, filename, key); // throws ResourceException
		// replace custom color tags (eg b1, b2) with standard HTML font tags
		if ( wantColor )
			html = colorIn(html);
		if ( wantCache )
			CACHE.put(key, html); // it's up to you to be consistent with colors
		return html;
	}

	/**
	 * Load the specified HTML file.
	 * @param caller the caller object (used to fetch the class loader)
	 * @param filename the name of the HTML file
	 * @param wantColor if true then colorIn is called BEFORE the document is
	 * cached to replace custom color tags with regular HTML color tags.
	 * You must be careful with this. Some documents can't be cached because
	 * there coloring-in varies each time. A*E hint comes to mind. Or maybe we
	 * should cache the document BEFORE it's colored-in and just run color
	 * in every time load is requested, and maybe have a switch to cache before
	 * or after coloring-in... but FMS that's all getting complicated fast, and
	 * I don't like complications. I like s__t that works. And there one goes
	 * now out of my ass at about forty! Yearrrs, thank you Ritchie. More gopher
	 * Cleetus? Cockroaches? Ten bucks on the octopus! Yep, McFly's ____ed!
	 * Looks like Darwin laughs last. But who knew, right?
	 * @param wantCache if true then the document is cached to speed up
	 * repeated load requests.
	 * @return the content of the HTML file
	 */
	public static String load(Object caller, String filename, boolean wantColor, boolean wantCache) {
		try {
			return cachedLoad(caller, filename, wantColor, wantCache);
		} catch (ResourceException ex) {
			System.err.println(ex.getMessage());
			Throwable cause = ex.getCause();
			if ( cause != null )
				cause.printStackTrace(System.err);
			// Htmlificate the error message
			return "<html><body><font color=\"red\"><pre>"
				  + ex.getMessage() // includes cause message (if any)
				  + "</pre></font></html></body>";
		}
	}

	/**
	 * Load the specified HTML file.
	 * <p>Custom color tags are replaced by regular HTML color tags.
	 * <p>NOTE: This is the original method.
	 * <p>The non-params wantColor and wantCache both default to true.
	 * @param caller the caller object (used to fetch the class loader)
	 * @param filename the name of the HTML file
	 * @return the content of the HTML file
	 */
	public static String load(Object caller, String filename) {
		return load(caller, filename, true, true);
	}

	/**
	 * Produces HTML of the given hint from fileName with the given args.
	 * This is just lazy-ass shorthand for load and format methods.
	 * @param hint
	 * @param fileName
	 * @param args
	 * @return
	 */
	public static String produce(Object hint, String fileName, Object... args) {
		return format(load(hint, fileName), args);
	}

	// indexes of "<font color ="?"> elements in COLORS
	private static final int RED    = 1;
	private static final int CYAN1  = 5;
//	private static final int GREEN  = 9;
//	private static final int ORANGE = 13;
//	private static final int PINK   = 17;
//	private static final int BLUE1  = 21;
//	private static final int BLUE2  = 25;
//	private static final int CYAN2  = 29;

	private static final String[] COLORS = new String[] { // startIndex
	 "<r>", "<font color=\"red\">"     ,"</r>", "</font>" //  0 red candidate
	,"<c>", "<font color=\"#00AAAA\">" ,"</c>", "</font>" //  4 cyan cell bg
	,"<g>", "<font color=\"#009000\">" ,"</g>", "</font>" //  8 green candi
	,"<o>", "<font color=\"#E08000\">" ,"</o>", "</font>" // 12 orange candi
	,"<k>", "<font color=\"#FF00FF\">" ,"</k>", "</font>" // 16 pink candi
	,"<b1>","<font color=\"#0000A0\">" ,"</b1>","</font>" // 20 blue region
	,"<b2>","<font color=\"#009000\">" ,"</b2>","</font>" // 24 green region
	,"<b3>","<font color=\"#964B00\">" ,"</b5>","</font>" // 36 brown ALS
	,"<b4>","<font color=\"#009090\">" ,"</b3>","</font>" // 28 dark aqua ALS
	,"<b5>","<font color=\"#DC05DC\">" ,"</b6>","</font>" // 40 purple SDC/ALS
	,"<b6>","<font color=\"#CCCC00\">" ,"</b4>","</font>" // 32 yellow ALS
	,"<b7>","<font color=\"#000000\">" ,"</b7>","</font>" // 44 light blue ALS // black coz D8E6FF is unreadable
	};

	private static String color(String html, int i) {
		return COLORS[i] + html + "</font>";
	}

	/**
	 * Replace custom color tags by regular HTML color tags.
	 * @param html the HTML to convert
	 * @return the converted HTML
	 */
	public static String colorIn(String html) {
		for ( int i=0, N=COLORS.length; i<N; i+=2 )
			html = replaceAll(html, COLORS[i], COLORS[i+1]);
		return html;
	}

	public static String strongCyanOrRed(Ass a) {
		return color(a.strong(), a.isOn?Html.CYAN1:Html.RED);
	}

	public static String weakCyanOrRed(Ass a) {
		return color(a.weak(), a.isOn?Html.CYAN1:Html.RED);
	}

	private static final String[] PATTERNS = new String[] {
		  "{0}", "{1}", "{2}", "{3}", "{4}", "{5}", "{6}", "{7}", "{8}", "{9}"
		,"{10}","{11}","{12}","{13}","{14}","{15}","{16}","{17}","{18}","{19}"
		,"{20}","{21}","{22}","{23}","{24}","{25}","{26}","{27}","{28}","{29}"
		,"{30}","{31}"
	};

	/**
	 * Format the HTML (like Formatters format method).<br>
	 * <b>Search and Replace</b><br>
	 * the patterns {@code "{0}", "{1}", etc}<br>
	 * with the values {@code args[0], args[1], etc}.
	 *
	 * @param html String html to be formatted containing {@code {0}, {1}, etc}.
	 * The frmt string in Formatter.format().
	 * @param args {@code Object...} arguments array of Object. The values to
	 *  be inserted into the html. The args array in Formatter.format().
	 * @return the formatted HTML as a String.
	 */
	public static String format(String html, Object... args) {
		if ( args.length > MAX_ARGS )
			throw new IllegalArgumentException("args.length="+args.length+" > "+MAX_ARGS);
		for ( int i=0,n=args.length; i<n; ++i )
			// NB: String.valueOf(s) is a null-safe version of s.toString()
			html = replaceAll(html, PATTERNS[i], String.valueOf(args[i]));
		return html;
	}

	public static String safe(String s) {
		return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

}