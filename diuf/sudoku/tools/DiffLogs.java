//KRC 2020-07-20: No use in nearly a year. Time to go?
///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2022 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.tools;
//
//import diuf.sudoku.io.IO;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.util.regex.Pattern;
//
//
///**
// * Shows the divergence of the hint-paths in 2 LogicalSolverTester
// * Log.MODE=VERBOSE_5_MODE logFiles. IE: it prints the first hint in each
// * puzzle which differs.
// * <p>
// * I presumed that all hints would differ from that point on, so I was a bit
// * surprised to find that this is commonly not the case. Two hinters find the
// * same elimination fairly commonly (maybe 20% of the time, I guess).
// * <p>
// * NB: this algorithm (et al) is dependant on "^\\d+#" matching the start of the
// * puzzle-header-line and being the only lines in the file witch match; and a
// * bunch of other stuff like %5d hint-numbers. IE: One f__ks with the logFile
// * format at ones own peril.
// *
// * @author Keith Corlett 2019 OCT
// */
//public class DiffLogs {
//
//	public static void main(String[] args) {
//
//		final Pattern puzzleLinePattern = Pattern.compile("[.123456789]+");
//
//		File aFile = IO.KEEPER_LOG;
//
////		// Check the logFile of a -REDO run of the latest version.
////		File bFile = new File(IO.HOME+"top1465.d5.log");
////		// OR Check the latest KEEPER_LOG.
//		File bFile = new File(IO.HOME+"top1465.d5.2020-02-02.15-13-02.log");
//
//		String[] puzzleData = new String[2];
//		String startOfPuzzleHeaderLine, puzzleHeaderLine, aHintLine, bHintLine;
//		String a, b, aHint, bHint;
//		PrintStream out = System.out;
//		int count = 0; // number of divergent puzzles
//		try ( BufferedReader aRdr = new BufferedReader(new FileReader(aFile))
//			; BufferedReader bRdr = new BufferedReader(new FileReader(bFile)) ){
//
//			out.println(aFile.getName());
//			out.println(bFile.getName());
//			// we're reading both files simultaniously. Each line handled ONCE,
//			// except the puzzleHeaderLine and puzzleData that we remember in
//			// case the hints differ, in which case we output them.
//			for ( int pid=1; pid<1466; ++pid ) {
//				startOfPuzzleHeaderLine = ""+pid+"#";
//
//				// skip aReader down to the puzzleHeaderLine
//				//11#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.4..1.2.......9.7..1..........43.6..8......5....2.....7.5..8......6..3..9........
//				if ( (puzzleHeaderLine=nextLineStartingWith(aRdr, startOfPuzzleHeaderLine)) == null )
//					throw new RuntimeException("Not found in a: "+startOfPuzzleHeaderLine);
//				// skip bReader down to the same puzzle
//				if ( (b=nextLineStartingWith(bRdr, startOfPuzzleHeaderLine)) == null )
//					throw new RuntimeException("Not found in b: "+startOfPuzzleHeaderLine);
//				// let's just check that the two puzzleHeaderLines match
//				assert b.equals(puzzleHeaderLine);
//
//				do {
//					// get these 3 (or 4) lines from both readers
////in_b_only:		//wantedEnabledHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedSept, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus
//					//<this is an empty-line in the log-file>
////puzzleData[0]		//.4..1.2......49.7..1...2......43.6..8.3....5....28....7.5..8......6..3..9........
////puzzleData[1]		//356,,79,3578,,367,,3689,35689,2356,2568,268,35,,,15,,1356,356,,79,3578,567,,4589,34689,345689,125,2579,12,,,57,,1289,12789,,267,,179,679,167,147,,1247,146,5679,146,,,1567,179,139,1379,,36,,139,29,,149,12469,12469,124,28,1248,,2579,147,,1289,125789,,2368,12468,1357,257,134,1578,1268,125678
//
//					do {
//						a = aRdr.readLine();
//					} while ( !puzzleLinePattern.matcher(a).matches() );
//					puzzleData[0] = a;
//					puzzleData[1] = aRdr.readLine();
//					do {
//						b = bRdr.readLine();
//					} while ( !puzzleLinePattern.matcher(b).matches() );
////					if ( !b.equals(puzzleData[0]) )
////						Debug.breakpoint();
//					assert b.equals(puzzleData[0]);
//					b = bRdr.readLine();
//					assert b.equals(puzzleData[1]);
//
//					// now read the hint-line from both readers, and diff them
//					//hid  	       time(ns)	ce	mayb	eli	hinter                         	hint
//					//33   	    753,915,642	22	 208	  1	Aligned Sept                  	Aligned Sept: A4, C4, B5, A6, G6, H6, I6 (B5-6)
//					aHintLine = aRdr.readLine();
//					//33   	     69,586,245	22	 208	  1	Unary Chain                   	Unary XY Chain: B4-7 (B4-7)
//					bHintLine = bRdr.readLine();
//					// lets just check that the hint numbers match (in %d5 format)
//					assert bHintLine.substring(0,5).equals(aHintLine.substring(0,5));
//					// nb: char 65 (0 based) is the start of the hint-type-name
//					// nb: there's tabs in them there hills!
//					aHint = aHintLine.substring(65);
//					bHint = bHintLine.substring(65);
//					if ( !aHint.equals(bHint) ) {
//						if ( 
//							// the keep log has Direct* hinters enabled and the current version doesn't.
//							!aHint.startsWith("Direct ")
////							// Aligned Quads are hacked
////							&& !bHint.startsWith("Aligned Quad")
////							// Aligned Pents are hacked
////							&& !bHint.startsWith("Aligned Pent")
//						) {
//							++count;
//							out.println(puzzleHeaderLine);
//							out.println(puzzleData[0]);
//							out.println(puzzleData[1]);
//							out.println(aHintLine);
//							out.println(bHintLine);
//						}
//						break; // out of the do while loop, and go onto the next puzzle
//					}
//				// This is matching the number of maybes remaining in what will
//				// be the last hintLine. It's a bit trixie, but it works.
//				// NB: if b isn't there yet then don't blame me, Labour dun it!
//				} while ( !"   0".equals(aHintLine.substring(25, 29)) );
//			} // next puzzle number (pid)
//			// breaking the silence is golden rule here, but it works for me.
//			out.println("DiffLogs: divergences "+count);
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//		}
//	}
//
//	private static String nextLineStartingWith(BufferedReader reader, String s) throws IOException {
//		String line = null;
//		while ( (line=reader.readLine()) != null )
//			if ( line.startsWith(s) )
//				return line;
//		return null;
//	}
//
//}
//
//
///*
//SAMPLE OUTPUT:
//
//run:
//2#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	..1.......2..8.3.1..4.63......4...3.....95...9....16.2.6....4.7..57.....7....98..
//..1.......2..8.3.1..4163......4...3.....95...9....16.2.6....4.7..57.....7....98..
//368,38,,29,25,47,59,4678,46,56,,79,59,,47,,467,,58,79,,,,,279,278,589,128,1578,268,,27,68,159,,589,12348,1348,236,236,,,17,1478,48,,45,378,38,37,,,45,,123,,289,358,135,28,,1259,,12348,89,,,134,268,12,1269,36,,134,23,356,14,,,1256,356
//   52	     19,232,464	25	 152	  1	Aligned Pent                  	Aligned Pent: D1, D2, F4, D7, D9 (D9-3)
//   52	     28,870,911	25	 152	  1	Aligned Hex                   	Aligned Hex: D1, D2, E4, F4, D7, D9 (D9-3)
//DiffLogs: divergences 1
//BUILD SUCCESSFUL (total time: 0 seconds)
//
//
//THIS WAS MY RFC:
//
//What I need now is a really really smart log-file-differ, that'll go through
//two log-files and find any divergencies in the hint-path through each puzzle
//so I'll be able to see when/where Hackfest (et al) misses a hint. When I
//know that I can look at each puzzle in IS_KYLIE_MOLE mode to see why the
//hint is being missed, and then I might be able to imagineer a solution.
//
//So to-do I want to diff two log-files in the same format, let's say
//VERBOSE_5_MODE only for now, and have it output two files:
//   diff.L.$puzzleNumber.txt
//   diff.R.$puzzleNumber.txt
//for each puzzle where the hints diverge. I'll also need a summary to stdout
//which tells me the puzzleNumber and the first hint that they diverge at, and
//the two divergent hint-lines. It shouldn't be too hard to diff the log lines
//ignoring the timings.
//
//Because these file-names aren't distinct it should probably produce it's
//own output directory: diff_YYYYMMDD_hhmmss under the current (start-up)
//directory.
//
//The ideal stdout would be something like:
//
//11#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.4..1.2.......9.7..1..........43.6..8......5....2.....7.5..8......6..3..9........
//.4..1.2......49.7..1...2......43.6..8.3....5....28....7.5..8......6..3..9........
//356,,79,3578,,367,,3689,35689,2356,2568,268,35,,,15,,1356,356,,79,3578,567,,4589,34689,345689,125,2579,12,,,57,,1289,12789,,267,,179,679,167,147,,1247,146,5679,146,,,1567,179,139,1379,,36,,139,29,,149,12469,12469,124,28,1248,,2579,147,,1289,125789,,2368,12468,1357,257,134,1578,1268,125678
//   33	    753,915,642	22	 208	  1	Aligned Sept                  	Aligned Sept: A4, C4, B5, A6, G6, H6, I6 (B5-6)
//   33	     69,586,245	22	 208	  1	Unary Chain                   	Unary XY Chain: B4-7 (B4-7)
//
//and the two diff-files would contain the whole of both puzzles
//from
//11#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.4..1.2.......9.7..1..........43.6..8......5....2.....7.5..8......6..3..9........
//to
//   11	    4,965,921,633	    3,876,144,604	  424	   56	  709	8.50	Dynamic Chain    	.4..1.2.......9.7..1..........43.6..8......5....2.....7.5..8......6..3..9........
//
//I'm thinking that the two diff-files might be overkill because I can find and
//visually diff two puzzles in there source log files, which works well. What I
//can't/won't do is that process manually looking for any differences accross
//1465 puzzles. That process MUST be automated if it's going to happen at all,
//and without it I'm really just guessing that my code changes are not missing
//hints, which is BAD/RISKY/NAUGHTY and has never even heard of Neut.
//
//*/
