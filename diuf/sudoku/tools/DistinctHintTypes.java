/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract distinct hint-types from a batch.log.
 *
 * @author Keith Corlett 2023-05-11
 */
public class DistinctHintTypes {

	public static void main(String[] args) {
		try {
			final String input = IO.HOME+"top1465.d5.2023-10-07.14-43-09.log";
//			final Matcher re = Pattern.compile(".*\\tSuperfischer.*").matcher(""); // Superfischerman or SuperfischerPerm
//			final Matcher re = Pattern.compile(".*\\t(FrankenJellyfish|MutantSwordfish|KrakenSwampfish)            .*").matcher("");
//			final Matcher re = Pattern.compile(".*\\t(UnaryChain|NishioChain|MultipleChain|DynamicChain|DynamicPlus)                .*").matcher("");
//			final Matcher re = Pattern.compile(".*\\tFrankenSwordfish              \t.*").matcher("");
//			final Matcher re = Pattern.compile(".*\\\tFrankenSwampfish              \t.*").matcher("");
//			final Matcher re = Pattern.compile(".*\\\tMutantSwordfish               \t.*").matcher("");
//			final Matcher re = Pattern.compile(".*\\\tBigWings                      \t.*").matcher("");
			final Matcher re = Pattern.compile(".*\\t(MutantSwordfish|FrankenJellyfish)            .*").matcher("");
			try ( final BufferedReader reader = new BufferedReader(new FileReader(input)) ) {
				// read distinct hintNames into a Map
				final Map<String, Integer> a = new HashMap<>(16, 0.75F);
				final Map<String, Integer> b = new HashMap<>(16, 0.75F);
				String line, hintName;
				int i;
				Integer existing;
				while ( (line=reader.readLine()) != null ) {
					if ( line.length() > 65
					  && (i=line.indexOf(':')) > 65
					  && re.reset(line).matches()
					) {
						// hinter name (unsure about the end-index)
//						System.out.println(line.substring(34, 60));
						hintName = line.substring(65, i);
						if ( line.charAt(34) == 'M' ) { // "MutantSwordfish"
							if ( (existing=a.get(hintName)) == null )
								a.put(hintName, 1);
							else
								a.put(hintName, existing + 1);
						} else { // FrankenJellyfish
//							System.out.println(line);
							if ( (existing=b.get(hintName)) == null )
								b.put(hintName, 1);
							else
								b.put(hintName, existing + 1);
						}
					}
				}
				// print them
				System.out.println("MutantSwordfish");
				for ( String key : a.keySet() )
					System.out.format("   %-42s\t%d\n", key, a.get(key));
				System.out.println("FrankenJellyfish");
				for ( String key : b.keySet() )
					System.out.format("   %-42s\t%d\n", key, b.get(key));
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}

/*
Finned Swampfish                          	 108
Finned Mutant Swampfish                   	  59
Kraken Type 1: Finned Swampfish           	 715
Kraken Type 1: Sashimi Swampfish          	   2
Kraken Type 1: Finned Franken Swampfish   	2672
Kraken Type 1: Finned Mutant Swampfish    	 747
Kraken Type 2: Finned Swampfish           	 806
Kraken Type 2: Finned Franken Swampfish   	 633
Kraken Type 2: Finned Mutant Swampfish    	 501
*/
