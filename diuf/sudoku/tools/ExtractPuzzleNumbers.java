/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.MyInteger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extract the numbers of the puzzles which contain an "Aligned Dec:", for
 * example. It now does all A*E's. Why? Well I'm sticking a "HIT" HACK in A*E
 * because they're all just too bloody slow!
 * @author Keith Corlett 2020 Feb
 */
public class ExtractPuzzleNumbers {

	private static final AE[] CORRECT_AEs = new AE[] {
		  new AE(IO.A2E_HITS, "Pair")
		, new AE(IO.A3E_HITS, "Triple")
		, new AE(IO.A4E_HITS, "Quad")
		, new AE(IO.A5E_1C_HITS, "Pent")
		, new AE(IO.A6E_1C_HITS, "Hex" )
		, new AE(IO.A7E_1C_HITS, "Sept")
		, new AE(IO.A8E_1C_HITS, "Oct" )
		, new AE(IO.A9E_1C_HITS, "Nona")
// WARNING WILL ROBINSON!!!! WARNING!!!! WARNING!!!!
// WARNING WILL ROBINSON!!!! WARNING!!!! WARNING!!!!
// WARNING WILL ROBINSON!!!! WARNING!!!! WARNING!!!!
//		, new AE(IO.A10E_1C_HITS,"Dec" )
		, new AE(IO.A10E_2H_HITS,"Dec" ) // <<================================== OI! ____ING LOOK AT THIS YA STUPID NUGGET!
// the current run should produce IO.A10E_1C_HITS
	};

	private static final AE[] HACKED_AEs = new AE[] {
		  new AE(IO.A2E_HITS, "Pair")
		, new AE(IO.A3E_HITS, "Triple")
		, new AE(IO.A4E_HITS, "Quad")
		, new AE(IO.A5E_2H_HITS, "Pent")
		, new AE(IO.A6E_2H_HITS, "Hex" )
		, new AE(IO.A7E_2H_HITS, "Sept")
		, new AE(IO.A8E_2H_HITS, "Oct" )
		, new AE(IO.A9E_2H_HITS, "Nona")
		, new AE(IO.A10E_2H_HITS,"Dec" )
	};

	private static final AE[] ALL_AEs = new AE[] {
		  new AE(IO.A2E_HITS, "Pair")
		, new AE(IO.A3E_HITS, "Triple")
		, new AE(IO.A4E_HITS, "Quad")
		, new AE(IO.A5E_1C_HITS, "Pent"), new AE(IO.A5E_2H_HITS, "Pent")
		, new AE(IO.A6E_1C_HITS, "Hex" ), new AE(IO.A6E_2H_HITS, "Hex" )
		, new AE(IO.A7E_1C_HITS, "Sept"), new AE(IO.A7E_2H_HITS, "Sept")
		, new AE(IO.A8E_1C_HITS, "Oct" ), new AE(IO.A8E_2H_HITS, "Oct" )
		, new AE(IO.A9E_1C_HITS, "Nona"), new AE(IO.A9E_2H_HITS, "Nona")
		, new AE(IO.A10E_1C_HITS,"Dec" ), new AE(IO.A10E_2H_HITS,"Dec" )
	};

	// WARN: top1465.d5.2020-02-19.11-34-01.log = A2..9E_1C and A10E_2H
	//       so extracting the _2H_HITS from it would be wrong;
	//       so I will need to run it "all hacked" to produce those.
//	private static final String LOG = "top1465.d5.2020-02-19.11-34-01.log";
	private static final String LOG = "top1465.d5.2020-02-28.00-01-21.log";
	private static final AE[] AEs = CORRECT_AEs;

	public static void main(String[] args) {
		for ( AE ae : AEs ) {
			final String alignedSetSizeHintMarker = "Aligned "+ae.word+":";
			try ( PrintStream out = new PrintStream(ae.file);
				  // nb: the SodokuSolverTester log is an input file (not the usual output file) so beware that if you ____ with it's format you will be shot with an eel. Dogs die in hot hovercraft; Angelina Jollies are always sold seperately (rather unfortunate that, I'd like two, with extra lesso, hold the mayo); and my name is Aarcamaeidonoidies Spackfiller. No it isn't! Agghhhrrrrhhh!).
				  BufferedReader in = new BufferedReader(new FileReader(LOG)) ) {
				String line;
				Set<Integer> pids = new LinkedHashSet<>(256, 0.75F); // Set for uniqueness. HashSet for O(8)'ish contains. 256 coz it grows fast enough from there.
				int hash = -1;
				String puzzleNumber = "666666";
				while ( (line=in.readLine()) != null )
					if ( (hash=line.indexOf("#")) > -1 ) // skip the first line (headers)
						puzzleNumber = line.substring(0, hash);
					else if ( line.contains(alignedSetSizeHintMarker) )
						pids.add(MyInteger.parse(puzzleNumber));
				for ( Integer pid : pids )
					out.println(pid);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * An AE (for AlignedExclusion) is an output-filename
	 * and an "Aligned ${word}:" to look for in the logfile
	 * to produce that output-file.
	 */
	private static final class AE {
		public final File file;
		public final String word;
		public AE(File file, String word) {
			this.file = file;
			this.word = word;
		}
	}

}
