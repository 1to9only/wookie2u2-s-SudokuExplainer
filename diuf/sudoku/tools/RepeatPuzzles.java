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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This one is a bit crusty.
 * <pre>
 * Some years ago I replaced one of the puzzles (number 39, IIRC) in top1465
 * with a copy of an earlier one for two reasons:
 * (1) the bastard broke a big AlignedExclusion, and I simply had no idea.
 * (2) Petunia said I could, coz they are MY puzzles. Consistency is required
 *     for timings, so a ONE puzzle change causes a small statistical anomoly,
 *     whose importance soon faids. Basically, I just changed a curtain.
 * </pre>
 * I have just binned the whole specific-size-aligned-exclusion debacle. Hence
 * I need a tool to find any repeated puzzles in a .mt, and tells which line
 * numbers they are, so that, as I shall do now, I can fix it. There shall be
 * no repeat-puzzles. Breaking one rule lead to another, and another, which was
 * nice so long as Petunia left her teeth out, which she did, but only sort of.
 * <p>
 * Having run this I discover that there are NO repeats in top1465.d5.mt, so I
 * guess I fixed it, probably by adding a clue to the offensive little bastard;
 * I shall sort my output to find out.
 *
 * @author Keith Corlett 2023-06-14
 */
public class RepeatPuzzles {

	public static void main(String[] args) {
		try {
			final String input = IO.HOME+"top1465.d5.mt";
			try ( final BufferedReader reader = new BufferedReader(new FileReader(input)) ) {
				// Map puzzle->lineNumbers (Linked coz iterator order matters)
				final Map<String, List<Integer>> map = new LinkedHashMap<>(1465, 0.75F);
				String line;
				int lineNumber = 0;
				while ( (line=reader.readLine()) != null ) {
					++lineNumber;
					List<Integer> lineNumbers = map.get(line);
					if ( lineNumbers == null ) { // almost always
						lineNumbers = new LinkedList<>();
						map.put(line, lineNumbers);
					}
					lineNumbers.add(lineNumber);
				}
				if ( false ) {
					// print any repeats
					int count = 0;
					for ( String puzzle : map.keySet() ) {
						List<Integer> list = map.get(puzzle);
						if ( list.size() > 1 ) {
							String s = Arrays.toString(list.toArray());
							System.out.format("%-81s\t%s\n", puzzle, s);
							++count;
						}
					}
					System.out.format("count=%d\n", count);
				} else {
					// print a lexographical sorted list
					final List<String> list = new ArrayList<>(map.keySet());
					list.sort((o1, o2) -> o1.compareTo(o2));
					for ( String puzzle : list )
						System.out.format("%-81s\n", puzzle);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
