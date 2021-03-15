/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.MyStrings;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * Settings.THE is the singleton instance of application settings.
 * Stored in: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\diuf\sudoku
 */
public final class Settings implements Cloneable {

	// The build list:
	// 6.28.139 Got solve basically right: fine tune for speed.
	// 6.29.000 save format = toString 1x81 values + 1x81 maybes (was 9x9 values
	//			+ 9x9 maybes). It's backwards compatible, so a minor release.
	// 6.29.001 flash aqua when solves with singles.
	// 6.29.002 fix bugs in undo. Ctrl-Z only, NO Ctrl-Y (Sigh) Also only in
	//			user-changes, auto-changes cannot be undone. How to?
	// 6.29.003 single click to set cell or remove maybe.
	// 6.29.004 Ctrl-P generates next when the generate dialog is open.
	// 6.29.005 fixed bug in display of BUG type 4, and added a test for it.
	// 6.30.001 been effing around with stuff for a while so I thought VERSION.
	// 6.30.006 Removed sort checkBox. Faster AChainingHint equals. Faster
	//			contains in HintsAccumulator.add. Faster Point & Claim.
	// 6.30.007 Make nesters user-selectable again. Removed cache of
	//			ancestors + numAncestors in the Ass in AChainingHint.
	//			Inlined oc = anOff.otherCell(r) in Chainer.findOffToOns(...).
	// 6.30.008 Fixed funky nesting CellReductionHint's caused by hint in "raw"
	//			grid. No biggy coz conclusion correct, tho formative logic mad.
	//			Also checked hints hashCodes.
	// 6.30.009 Finally understand those funky Nested* CellReductionHints.
	//			If source.cell != this cell refect/ignore hint.
	//			Added AlignedQuad + AlignedPent is ludicrously slow.
	// 6.30.010 Disallowed AlignedPent (unless you're a programmer) too slow.
	//			Rewrote the introduction. Fixed bug in Grid.setTheCellsValue.
	//			Fixed bad numHints and numElims. Cleaned out the to-do list.
	//			Right-click on hintsTree and hintDetails copy to clipboard.
	// 6.30.011 2019-09-09 14:55:11 Created AlignedQuadExclusion relegating
	//			AlignedSetExclusion to the dead-code bucket. Moved UnaryChain
	//			into heavies before ATEs and AQEs, as per the original Juillerat
	//			implementation. Demangled LogicalSolverTester Log.MODE's.
	// 6.30.012 2019-09-11 07:06:12 Moved Aligned* into chainers for 1465 in
	//			132,611,805,962 @ 90,520,004. That's 5 second pass in a Datsun
	//			120Y, driven by a suoer-heavy-weigth Sumo. He's in the boot.
	// 6.30.013 2019-09-11 relegated AlignedSetExclusion to the abstract class
	//			AAlignedSetExclusion and modified AlignedTripleExclusion,
	//			AlignedQuadExclusion, and AlignedPentExclusion to use protected
	//			methods exposed therein, to mostly-clean-up cut-n-paste coding.
	//			Also moved A5E to just before DynamicPlus in the LogicalSolver
	//			chainers list. It's still too slow, but doesn't matter so much.
	// 6.30.014 2019-09-15 Cleaning up "dodgy" Nested hints (again). The
	//			AChainingHint getChainHtmlLinesRecursively method now adds the
	//			nestedChain to explanation of initial ass's parents because the
	//			explanation is totaly useless without them, even though it's
	//			illegible with them. AChainingHint getSource moved to static
	//			Chainer method (which it calls) is overridden by CellReductnHint
	//			to search if source.cell!=srcCell. Exhumed IrrelevantHintExceptn
	//			from AlignedExclusionHint and put it in the getHtmlImpl of
	//			subtypes of AChainingHint: BinaryChainHint, CellReductionHint
	//			and RegionReductionHint. That'll do pig, I think.
	// 6.30.015 2019-09-18 Cleaned up multiple queues in Chainer doCycle and
	//			doUnary, and repeatedly failed at doChains, then figured out
	//			why. I won't forget. Moved LSH's to a Hash Class. Moved AHint
	//			Comparators to the AHint class. Redisaponated. Re-removed
	//			green/red (no orange) from the result maybe. Can't decide!
	// 6.30.016 2019-09-18 (again) Added Difficulty.Nightmare and IDKFA, and
	//			adjusted all the difficulties. Also added max-difficulty of
	//			each hint-type to Analysis output. Made generator + PuzzleCache
	//			more flexible to handle more (or less) Difficulty types.
	// 6.30.017 2019-09-20 made AlignedPairExclusion about 25% faster by redoing
	//			the populateCandidatesAndExcluders section. Now you can drag
	//			the toString-puzzle-text directly from Notepad++ into the grid
	//			to paste it. Created a flat cells array in the Grid which is
	//			used in preference to the matrix.
	//			1465 in 136,138,068,154 ns @ 92,927,008 each.
	// 6.30.018 2019-09-22 changed Aligned*Exclusion so that it does just the
	//			allow part of allow-or-exclude ASAP, and then does the full
	//			allow-or-exclude loop when we know there's going to be a hint.
	//			1465 in 140,987,714,832 ns @ 96,237,348
	// 6.30.019 2019-09-23 10:37 finally got Aligned*Exclusion right so that it
	//			does just the allow part of allow-or-exclude ASAP, and then we
	//			come back only if a value is not allowed to collect the full
	//			exclusion map. So in CMD with isRemovingLonelyExcluders = true:
	//			10:27:27 1465 in 115,977,000,571 ns @ 79,165,188 ns/puzzle.
	// 6.30.020 2019-09-23 17:32 polished all the Aligned*Exclusion code,
	//			including AlignedPairExclusion A2E. With A4E+A5E on:
	//			17:25:02 1465 in 144,328,015,050 ns @ 98,517,416 ns/puzzle.
	// 6.30.021 2019-09-29 11:55:55 still hacking Aligned*Exclusion. Got idxs
	//			right finally, I think. So am going to release and version.
	//			11:55:55 F10 in 239,032,018,308 ns @ 23,903,201,830 ns/puzzle
	//			13:06:45 F10 in 219,756,602,397 ns @ 21,975,660,239
	//			with A3E, A4E, and A5E before UnaryChain. We're getting there.
	// 6.30.022 2019-09-30 20:40:22 finished hacking at Aligned*Exclusion.
	//			1465 in 519,384,719,109 @ 354,528,818 ns/puzzle with A3E, A4E,
	//			& A5E all before UnaryChain. I declare that we're there yet.
	// 6.30.023 2019-10-01 16:02:23 added AlignedSeptExclusion, coz I'm nuts.
	//			Also retrofitted limit on numCandidates to A*E. A tad faster.
	//			1465 in 1,789,400,807,988 (29:49) @ 1,221,433,998 with A7E.
	// 6.30.024 2019-10-08 06:33:24 Forgive me father it has been a week since
	//			my last release. Added A8E. Renamed A*E to use a digit in-place
	//			of the wogalini, so that lists are in order. Still trying to
	//			dedog A*E, so added -REDO to LogicalSolver/Tester to parse the
	//			previous logFile for which hinters to use on each puzzle. Also
	//			diuf.sudoku.test.DiffLogs shows divergences in hint-path of 2
	//			VERBOSE_5_MODE logFiles, to be SURE there are no AWOL hints.
	//			1465 in 715,388,763,279 (11:55) @ 488,319,974 with A8E.
	// 6.30.025 2019-10-09 11:41:25 Still trying to improve A*E performance.
	//			Replaced the excludersMap with an excluders array.
	//			1465 in 580,809,800,933 (9:40) @ 396,457,202 REDO with A8E.
	// 6.30.026 2019-10-17 16:47:26 Added A9E and A10E and that's the last one.
	//			Improved A*E and Chainer performance in LogicalSolverTester
	//			-REDO by only running hintyHinters when they'll hint as
	//			determined by a previous non-REDO logFile.
	//			1465 in 103,514,882,927	(1:43) @ 70,658,623 -REDO with A10E,
	//			but the last non-REDO with everything was 52:04, ie SLOW!
	// 6.30.027 2019-10-26 21:45:27 New AHintNumberActivatableHinter extended by
	//			all heavies, for -REDO only. Also AHint.apply detects and sets
	//			subsequent Naked Singles. Synchronized: generate waits4 analyse.
	//			1465 in 127,904,212,510 (02:07) @ 87,306,629 -REDO with A10E.
	// 6.30.028 2019-11-01 07:49:28 Logging supressed while generating, and also
	//			sychronised to make the background generate thread wait for the
	//			foreground analyse to complete. Building now for a backup when
	//			I subtype Chainer into UnaryChainer and MultipleChainer.
	//			1465 in 135,020,038,901	(02:15) @ 92,163,849 -REDO with A10E.
	// 6.30.029 2019-11-02 08:50:29 Split Chainer into AChainer, UnaryChainer,
	//			and MultipleChainer. All test cases passed first time. WTWTF?
	//			1465 in 136,386,363,363	(02:16) @ 93,096,493 -REDO with A10E.
	// 6.30.030 2019-11-11 15:01:30 I just thought I should build and release.
	//			1465 in 133,050,598,498	(02:13) @ 90,819,521 -REDO with A10E.
	// 6.30.031 2019-11-12 12:15:31 A*E now checks each partially built combo
	//			isn't a superset of the maybes of any common excluder cells,
	//			so we don't bother building and testing the rest of the combo
	//			which won't be allowed anyways. This makes a huge
	//			difference to the full runtime, ie not in the faster -REDO mode
	//			1465 in 2,688,882,903,833 (44:48) @ 1,835,414,951 with A10E.
	//			This is down from a top of 58 minutes. A great relief.
	//			Also noticed that many A4E's were being missed by the previous
	//			release (atleast). I wonder how the hell that happened? Fixed!
	// 6.30.032 2019-11-17 18:13:32 A*E now removes any common excluders which
	//			are a superset of any other common excluder. It's faster again.
	//			1465 in 2,509,103,876,888 (41:49) @ 1,712,698,892 with A10E.
	// 6.30.033 2019-11-22 11:48:33 A5+E now removes maybes of subsequent cells
	//			when the cells are siblings. It's much faster again because it
	//			does more of the work less often.
	//			1465 in 1,889,295,456,806 (31:29) @ 1,289,621,472 with A10E.
	//			That's nearly half of the worste of 58 minutes. Sweet relief!
	//			1465 in   115,789,714,781 (01:55) @    79,037,347 REDO
	//			I declare 2 minutes as the border line of acceptable.
	// 6.30.034 2019-11-29 12:57:34 A*E now loops only while shiftedValue is
	//			less than or equal to the cells maybes bits, and there's a
	//			numCmnExclBits == 1 test in every Aligned*Exclusion class.
	//			1465 in 1,491,209,814,826 (24:51) @ 1,017,890,658 with A10E.
	//			which is about six and half minutes faster than the previous.
	//			1465 in   120,330,712,914 (02:00) @    82,137,005 REDO
	// 6.30.035 2019-12-10 14:28:35 Reverted to 6.30.034 2019-11-29 12:57:34 and
	//			now there's 2 of each A456E: correct and hacked: and CheckBox's
	//			in TechSelectDialog to choose between them. Turns out correct
	//			A5E + A4E takes 48 minutes, which still looks OK in the GUI.
	//			Also changes to Settings, and to hintDetailsHtml handling.
	//			The reversion broke debug + run in the IDE. I hate Netbeans.
	// 6.30.036 2019-12-12 14:04:36 Updated tests for A456E. There's still a bug
	//			that I don't understand in setting boolean preferences (just
	//			change the bastard using regedit). I'm releasing anyway because
	//			I've just got all the tests working again, so it's easy.
	// 6.30.037 2019-12-24 18:52:37 I'm releasing because it's been too long
	//			since my last release. I've been finding a fixing a few bugs.
	//			The only notable is making hidden singles (IIRC) and Chainers
	//			correctly return "any found", so no more extra searches.
	//			21:52:37 GUI saves/restores it's bounds to/from the registry.
	// 6.30.038 2019-12-31 17:16:38 I'm still pissing about with A*E. I've put
	//			the switch statement back in to cover, 1, 2, other number of
	//			common excluder bits. A4..10E all have correct and hacked
	//			versions, with a CheckBox in TechSelectDialog for each.
	// 6.30.039 2020-01-10 15:52:39 Still working on A*E: It's skips when total
	//			number of maybes is out-of-range from magic numbers which I
	//			collected earlier. It's a HACK specific to top1465.d5.mt, but
	//			it works, and I have a feeling that another puzzle set would
	//			produce very similar numbers, maybe a difference of 1-or-2 here
	//			or there. So it's a nasty little HACK but it works!
	// 6.30.040 2020-01-17 12:38:40 Still still working on A*E, which filters
	//			by various stats. It's down to 4 hours with HACKs specific to
	//			top1465.d5.mt.
	// 6.30.041 2020-01-19 07:32:41 A*E now all have at least "starter level"
	//			filters using the Counter class on mbs maybes size, col collisions,
	//			and ces common excluders size.
	// 6.30.042 2020-01-28 21:11:42 A7E_1C now has "top shelf" filters.
	//			It sorts the cells in the aligned set by:
	//				numMaybesCollisions*4 + cmnExclHits*2 + maybes.size,
	//			and filters by every stat I can think of. The total runtime is
	//			down to ? hours ? minutes with A234567E correct and A8910E
	//			hacked. I've also replaced Integer.bitCount calls with Values
	//			or Indexes SIZE look-ups. Am waiting for run in order to test.
	//			I'm reading Stephen Hawkins "The Big Questions". It's great!
	// 6.30.043 2020-01-31 19:08:43 Finished A7E_1C collisionComparator.filter,
	//			and exhumed siblingsCount filter to apply it earlier, bringing
	//			it down to 118:56 (under 2 hours) for A234567E_1C + A8910E_2H.
	// 6.30.044 2020-02-03 09:56:44 Scratching for A7E_1C performance:
	//			* tune DOG_1A "is empty tests" = about 7 minutes on top1465
	//			* reduced A7E_1C MAX_CANDIDATES from 60 to 57 = 8 minutes
	//			* revert to avb* virtual array = 3 minutes
	//			top1465.d5.mt down to 102:55 with A234567E_1C + A8910E_2H.
	//			I really really want to get under 100 minutes!
	// 6.30.045 2020-02-04 15:03:35 promulgated collisionComparator and any*
	//			technique to other A*E's. Also ANSI-C'ed all vars in hit files.
	// 6.30.046 2020-02-06 00:40:46 fixed bugs in the filters of A8E + A10E, and
	//			fixed bug in collisionComparator.score which ommits COLLISIONS!
	//			Declare ALL variables at the start of the method, like ANSI-C!
	//			top1465.d5.mt down to 95:52 with A234567E_1C + A8910E_2H
	//			I broke 100! I broke 100! I broke 100! I broke 100! I broke 100!
	// 6.30.047 2020-02-10 20:07:47 isAnyAllowed* in:
	//			* A7E_1C from 3,259 to 1,420 saving 30:39
	//			* A7E_2H from 27:43 but I've nothing to compare it to.
	//			* A6E_1C from 2,199 to   735 saving 24:24
	//			* A6E_2H from   108 to    50 saving  0:58
	//			* A5E_1C from   548 to   336 saving  3:32
	//			* A4E_1C from   149 to    79 saving  1:10 (REVERTED!)
	//			top1465.d5.mt down to 51:16 with A234567E_1C + A8910E_2H
	// 6.30.048 2020-02-11 20:39:48 isAnyAllowed* in A8E_2H and A8E_1C, and
	//			that's as far as I'm going to push it, so this is my last
	//			release, I guess. It's been not fun exactly, more engaging.
	//			top1465.d5.mt 1:38:00 with A2345678E_1C + A9/10E_2H
	// RERELEASE 6.30.048 2020-02-12 09:29:48 uncomment is1AnyAllowed4567
	//			in A8E_1C is nearly 10 minutes faster.
	//			top1465.d5.mt 1:28:21 with A2345678E_1C + A9/10E_2H
	// 6.30.049 2020-02-16 16:39:49 I've given up trying to make A8E_1C fast
	//			enough to use practically. It's acceptable the GUI, so it'll
	//			have to do. Top1465 in 111:50 with terrain following enabled.
	// 6.30.050 2020-02-21 19:35:50 I gave up trying to get A8+E fast enough to
	//			use practically, and cheated. Each A*E now uses an IntegerSet
	//			to remember which puzzles it hints from, ignoring others,
	//			bringing top1465.d5.mt down to 53 minutes with all A*E's in
	//			"correct" mode. That'll have to do.
	// 6.30.051 2020-02-22 11:59:51 Adjusted A*E_2H filters to the gsl filter,
	//			but I've got a feeling that some filters cost more than they
	//			save. You can go tune it up to warp-factor-9, I'm too lazy.
	//			top1465 in 4:26 with A234E_1C and A5678910E_2H. That'll do.
	// 6.30.052 2020-02-25 18:10:52 Bought A4E into line with A5E_1C with
	//			anyLevel through-out, and full filters. Using ARRAY aoap.
	//			New NUM_TRAILING_ZEROS. Updated inline otherCell in offToOns.
	//			top1465 !IS_HACKY in 11:46 with A5678910E_2H.
	//			top1465  IS_HACKY in 50:46 with A2345678910E_1C. She'll do.
	// 6.30.053 2020-02-28 11:35:53 Just pissing about with the performance of
	//			HiddenSet, and now NakedSet whose code changes are in this
	//			release despite not being included in this run:
	//			top1465 IS_HACKY in 53:30 with A2345678910E_1C is 2:44 SLOWER!
	//			top1465 IS_HACKY in  3:38 with A5678910E_2H is 8:08 faster.
	// 6.30.054 2020-03-05 21:46:54 Use a filter IntegerSet to expedite A*E's,
	//			coz it's much simpler than all those custom Counter filters.
	//			top1465 IS_HACKY in 3:04 with A2345678910E_1C is BS QUICK!
	// 6.30.055 2020-03-06 21:59:55 Put a WindexSet in each A*E to only examine
	//			only those aligned sets which produced a hint last time.
	//			top1465 IS_HACKY in 39:16 with A2345678910E_1C. She'll do.
	// 6.30.056 2020-03-09 19:05:56 The UBER HACK: replace HitSet, FilterSet and
	//			WindexSet with a new HitSet class, so all A*E's skip if there
	//          was no hint for this puzzle/hintNumber last run; and also jump
	//			straight to the aligned set which hinted here last run.
	//			top1465 IS_HACKY in 1:47 with A2345678910E_1C. It'll do.
	//			top1465 IS_HACKY in 1:53 with A5678910E_2H coz now all A*E's
	//			take about equal time, but the hacked ones produce less hints.
	//			But without the hacks it's still too slow to be useful:
	//			top1465 !IS_HACKY in     9:16 with A5678910E_2H is fast enough
	//			top1465 !IS_HACKY in 26:56:19 with A2345678910E_1C is too slow
	//          42#top1465 took 76 minutes, 74 minutes of which was Aligned Dec.
	// 6.30.057 2020-03-?? ??:??:?? A*E: Inlined covers method in default:'s.
	//			Put IS_HACKY in registry. Wired the GUI upto the HitSet.
	//			Ran A234567E with & without to compare number of aligned sets.
	//			I'll have to "hardcode hackery" in A8910E in order to run them
	//			in this-sort-of-scenario coz running it !IS_HACKY takes 2 days:
	//			A8E_1C 3 hrs  +  A9E_1C 9 hrs  +  A10E_1C 26 hrs
	//          top1465 !IS_HACKY in 16:00 with A5678910E_2H is 6:45 slower!
	//			which I think must be the inlined covers method. How test?
	// 6.30.058 2020-03-19 18:22:58 Still pissin about with A*E's. Pushed the
	//			HitSet + prepare and report methods up into the abstract
	//			AAlignedSetExclusionBase class. Stuck basic filtering in A10E
	//			even when !isHacky coz vanilla took 34 hours (down to 22 hours).
	// 6.30.059 2020-04-03 16:37:59 HoDoKu SingleDigitPattern first pass.
	//			Downloaded HoDoKu code to boost from. Started with sdp which is
	//			now complete: Skyscraper, TwoStringKite are algorithmically "as
	//			is". EmptyRectangle is totaly my implementation, but still based
	//			on the ideas I first saw in HoDoKu. WWing (in wing package with
	//			XYWing and XYZWing, which it most resembles, is also basically
	//			boosted "as is" from HoDoKu. Forgive me father it's been 3 busy
	//			weeks since my last release!
	// 6.30.060 Got lost in the fog, or something.
	// 6.30.061 2020-04-10 15:38:01 HoDoKu FinnedFisherman second pass, finding
	//			all Finned (non Sashimi) Fish, and test cases.
	// 6.30.062 2020-04-11 16:32:02 HoDoKu SashimiFisherman first pass, finding
	//			all Sashimi Fish, and test cases.
	// 6.30.063 2020-04-21 23:17:03 HoDoKu ALS-XZ and ALS-XY-Wing first pass,
	//          with ALS-XY requiring a dodgy check against the solution
	//			from BruteForce, and ALS-XY-Wing being slow and not finding
	//			any, and I'm pretty sure it's broken. No test cases yet.
	//			Last top1465: 08:58 with A5..10E Hacked and all the new s__t
	//			except ALS-XY-Wing which takes about 12 minutes to find nada.
	// 6.30.064 2020-04-30 10:41:04 HoDoKu ALS-XZ 2nd and 3rd pass: got ALS-XZ
	//			working properly and optimized for speed, with two test-cases.
	//			ALS-XY-Wing still finds no hints, so he's next.
	//			Last top1465: 14:54 with A5..10E hacked and ALS-XZ
	// 6.30.065 2020-05-07 12:34:05 HoDoKu ALS-XY-Wing second pass. Reverted to
	//			6.30.064 and did the minumum required to get hobiwans code for
	//			ALS-XY-Wing working in DIUF. Still no testcase for it.
	//			Last top1465: 12:39 with A5..10E hacked + ALS-XZ + ALS-XY-Wing
	// 6.30.066 2020-05-23 17:23:06 HoDoKu ALS-XY-Wing third pass. Reverted to
	//			6.30.065 multiple times trying to get mine to work properly; but
	//			in the end I decided to stick with hobiwans code, so imported
	//			all relevant code into diuf.sudoku.solver.hinters.alshdk
	//          [2020-09-23 renamed to diuf.sudoku.solver.hinters.als]
	//			Last top1465: 12:09 with A5..10E hacked + ALS-XZ + ALS-XY-Wing
	// 6.30.067 2020-05-25 10:12:07 HoDoKu ALS-XY-Chain first pass. Copy-pasted
	//			HoDoKu's ALS-XY-Wing hinter into HdkAlsXyChain. Both still use
	//			invalid(Hint) which is just like a total dirty little hack.
	//			Last top1465: 11:57 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.068 2020-05-26 10:16:08 HoDoKu ALS-XY-Chain second pass: both
	//			HdkAlsXyChain and HdkAlsXyWing use clean to remove invalid
	//			eliminations and skip if that empties the redPots. Dirty HACK!
	//			Last top1465: 12:15 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.069 2020-05-26 11:47:09 HdkAlsXyChainTest and HdkAlsXyWingTest.
	//          DiufSudoku_V6_30.069.2020-05-26.7z
	// 6.30.070 2020-06-05 09:32:10 Revert DiufSudoku_V6_30.069.2020-05-26.7z
	//			and fix some (not all) ALS-XY-Wing and ALS-Chain bugs. Chain
	//			ignores currALS which is subset (or viceversa) of one in chain.
	//			ALS-XY-Wing had some extranious blues.
	// 6.30.071 2020-06-05 20:09:11 Finally got rid of ALL invalid hints from
	//			HdkAlsXyChain by ignoring cells which are involved in an any
	//			actual locked set in this region, but it's a tad slower.
	//			Last top1465: 12:27 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.072 2020-06-15 12:47:12 Merged my AlsXz into the alshdk package and
	//			removed my als package.
	//          [2020-09-23 renamed alshdk to diuf.sudoku.solver.hinters.als]
	//			Last top1465: 12:56 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.073 2020-06-22 22:17:13 Improved GUI after Sue test. Locking
	//			now detects Siamese hints and defers to Hidden Pair/Triple, and
	//			then removes each hint whose redPots is a subset of anothers.
	//			Last top1465: 12:56 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.074 2020-06-27 14:12:14 Fixed prepare/valid bugs in HdkAls* so that
	//			Ctrl-L now works properly. Fixed all test cases.
	//			Last top1465: 13:04 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.075 2020-06-29 21:35:15 Made Idx's forEach faster by bit-twiddling.
	//			Last top1465: 12:36 with A5..10E hacked + ALS-XZ+XyWing+XyChain
	// 6.30.076 2020-07-01 19:45:16 Performance tuning with the Profiler.
	//			Faster build siblings in Grid constructor in BruteForce, also
	//			dramatically reduces memory usage. Iterate Indexes.ARRAYS in
	//			region chains. Also some code clean-up: consistent naming.
	//			Last top1465: 02:40 -A5..10E +ALS-XZ +XyWing -XyChain.
	// 6.30.077 2020-07-10 17:24:17 Using HoDoKu's "complex" Fish hinter from
	//			Sudoku Explainer turned out to be pretty easy. Making it run
	//			faster is the difficult part. It's really really really hard!
	//			Last top1465: 05:29 +Franken/Mutant 2/3/4fish.
	// 6.30.078 2020-07-20 11:42:18 Forgive me father it's been 10 days since
	//			my last release and I just need to rub one out. Actually I need
	//			to delete my log-files and I want a backup before I do so.
	//			I've been pissin about trying to make HoDoKu performant, with
	//			some (but not too much) success. The main peformance snags are
	//			hobiwans reliance on "heaver" getters, and Integer's bloody
	//			compareTo(Integer) method. It's all crap Ray!
	//			Last top1465: 04:11 +Franken/Mutant/Kraken 2/3/4fish.
	// 6.30.079 2020-07-21 12:26:19 profile to expedite getAllKrakenFishes ->
	//          TablingSolver.buildChain -> TreeMap<Int,Int>.get -> Int.of ->
	//			the size of the IntCache (4K -> 75% hit rate) but now there's
	//			s__tloads of logs so I release to back and clean them up.
	//			Last top1465: 03:20 +Franken/Mutant/Kraken 2/3/4fish.
	// BROKEN:	I also discovered that my desktop "release" SudokuExplainer no
	//			longer runs (HoDoKu ClassNotFound) so I only run in situ now.
	//			I need a real Java programmer to workout what's happened to
	//			the classpath. I just blame Nutbeans! Weird dependancy s__t!
	// 6.30.080 2020-08-01 16:34:20 Been mucking about with HoDoKu FishSolver.
	//			I got it somehow to find shiploads of hints, then reverted coz
	//			I broke it, and now I can't get back to finding all them hints.
	//			It's "working-ish" now, so I brought it over from D:, and I
	//			release to clean-up my log files.
	//			Last top1465: 03:34 +Franken/Mutant/Kraken 2/3/4fish.
	// 6.30.081 2020-08-06 22:26:21 Been pissin about trying to make FishSolver
	//			faster, then tried implementing FrankenFisherman myself and it
	//			sucks. Last top1465: 03:25 +Franken/Mutant/Kraken 2/3/4fish.
	// 6.30.082 2020-08-10 21:42:22 Implemented HdkColoring. Improved speed of
	//			HdkFisherman marginally. Standardised caching.
	//			Last top1465: 03:25 +Franken/Mutant/Kraken 2/3/4fish, but
	//			dropped Nishio (1 hint in 4 secs) and Kraken Swordfish (9 hints
	//			in 21 secs)
	// 6.30.083 2020-08-21 10:08:23 I went through the first 1000 puzzles in
	//			the GUI looking for bugs, weak hint explanations and the like.
	//			Fixed a few "ancillaries" like painting fins blue, and a bug
	//			in Locking mergeSiameseHints.
	//			Last top1465: 03:38 is 13 seconds slower with siamese check.
	//			Siamese is inefficient: processes 21,041 hints to find about a
	//			100 "extra" eliminations which'll be found anyway. It wasn't
	//			intended to ever be part of the racing uterus, but I needed it
	//			to find a puzzle which tripped over the bug; and you can switch
	//			it on and off at will. It was on for this run, that's all. I'll
	//			switch it off when I get bored with the whole siamese thing.
	// 6.30.084 2020-08-21 20:37:24 Discovered a bug in NakedSet, it was falsely
	//			returning false after finding hints. How embarassment!
	//			Last top1465: 03:24 is 16 seconds faster.
	// 6.30.085 2020-08-31 17:39:25 Simplified colors by adding a getOrange
	//			independent of getGreens and getReds. Fixed all test cases.
	//			Last top1465: 03:38 is 16 seconds slower. Using Direct hinters
	//			so not real worried.
	// 6.30.086 2020-09-01 14:59:26 Fixing logView.
	// 6.30.087 2020-09-03 08:05:27 Fixed logView. Previous release is a bust.
	//			Bugs in the load process makes it not work at all; so I must
	//			release this patched version TODAY if possible. Also fixed
	//			logView, which is what I thought I had done in the previous
	//			release, which was rushed out because I don't control when we
	//			go to town. I should NOT have released untested. That was a BIG
	//			mistake and I won't make it again. Sigh. logView now takes a
	//			Java regex (was just a plain string).
	//			Last top1465 03:03 but no Krakens so don't get excited.
	// 6.30.088 2020-09-07 11:20:28 Release because I'm at the library and I
	//			can. No notable changes to the best of my recollection.
	//			Did not actually release this. Ran out if internet connection!
	// 6.30.089 2020-09-12 11:55:29 Bug fix: Subsequent views not shown for
	//			RegionReductionHint and CellReductionHint.
	// 6.30.090 2020-09-24 13:30:30 Boost ColoringSolver as color.Coloring.
	//			Renamed package diuf.sudoku.solver.hinters.alshdk to als.
	// 6.30.091 2020-10-06 18:06:31 Boost HoDoKuFisherman does Finned, Franken,
	//			and Mutant fish. It can do Basic but doesn't coz Fisherman is
	//			faster. It's still too slow for Mutant (not really useable).
	//			Stripped Kraken from hobiwans code, which I plan to put in a
	//			KrakenFisherman class.
	//			Last top1465 18:38 because Mutants are too slow!
	// 6.30.092 2020-10-11 18:46:32 Boost Kraken Fish from HoDoKuFisherman into
	//			KrakenFisherman. It works, but it's very very very very slow.
	//			Last top1465 18:38 because Mutants are too slow!
	// 6.30.093 2020-10-14 18:03:33 New version of KrakenFisherman is twice as
	//			fast as original, so now it's just very very slow.
	//			Last top1465 35:49 with KrakenSwamp+Sword, but NO Jellyfish.
	// 6.30.094 2020-10-15 15:37:34 Replaced "interleaved" AHint.getRegions
	//			and all subtypes (30 of them) with getBase and getCovers.
	//			This change should have no consequence to non-developers, but
	//			to a techie combining 2 types of things in an array is MENTAL.
	//			Last top1465 2:34 is about expected for the wanted hinters.
	// 6.30.095 2020-10-17 22:25:35 test-cases for Idx, HdkSet and HdkSetBase.
	//			I've given-up on trying to clean-up the ALS package again. It
	//			alway fails as soon as I replace the HdkSet with my Idx. I do
	//			not know what the problem is, but I do know I'm too thick to
	//			work it out... so bugger it, it can stay as-is, it's only my
	//			techie pride wanting to eradicate the "foreign" code.
	//			Last top1465 2:34 is about expected for the wanted hinters.
	// 6.30.096 2020-10-18 06:36:36 redo release to include overnight run-log.
	//			Last top1465 132:54 with all the big fish. Not bad. Not God.
	// 6.30.097 2020-10-19 21:14:37 I finally worked-out where I went wrong and
	//			eradicated/implanted the HoDoKu types from the als package.
	// 6.30.098 2020-10-23 23:20:38 I went mad and cleaned-up. There should be
	//			no visible changes, but LOTS of dead and dying code has been
	//			removed. The only notable from a users perspective is that the
	//			Grid constructor now just calls load, instead of doing it all
	//			itself the pernickety way; so the Grid constructor now loads an
	//			invalid grid, doesn't "fix" it, and doesn't complain; just does
	//			the best it can with what it's given.
	//			Last top1465 02:37 with none of the big fish. She'll do.
	// 6.30.099 2020-11-30 07:01:39 Applied lessons from a month of performance
	//          tuning HoDoKu, especially caching in KrakenFisherman#kt2Search.
	//			Last top1465 51:24 with all the BIG FISH + A*Es.
	//          Last top1465 07:32 without the bloody mutants.
	// 6.30.100 2020-12-05 10:05:40 Polished KrakenFisherman. Added nonHinters
	//          filter to A5+E; which is what I SHOULD have done instead of all
	//          those months trying to work-out how to do it faster, so the
	//          ideal implementation now would be an A*E class implementing
	//          A5678910E and another simpler one for A234E. The key to this
	//          is the IntIntHashMap I wrote originally for HoDoKu.
	//          Last top1465 15:17 with A567E_1C, which is an improvement, I
	//          think, but do not have anything to compare it to (bad form).
	// 6.30.101 2020-12-08 07:58:41 Back-up prior to Aligned*Exclusion redo.
	//			This version has NonHinters64 and LongLongHashMap. Now I want
	//			to try iterating a stack in A*E (ala hobiwan) to remove all the
	//			the A*E classes, ie eliminate code-bloat.
	// 6.30.102 2020-12-17 10:54:42 Release align2.AlignedExclusion squashing
	//          all Aligned*Exclusion's boiler-plate code down into one complex
	//			succinct class using hobiwans stack iteration technique twice:
	//			once for cells, and again for vals.
	//			Last top1465 12:43 with A234E A5E correct and A678910E hacked.
	// 6.30.103 2021-01-01 15:17:43 Release "wing2" package with WXYZWing,
	//			UVWXYZWing, and TUVWXYZWing boosted from Sukaku, by Nicolas
	//          Juillerat (the original authors rewrite of Sudoku Explainer)
	//			who boosted them from SudokuMonster.
	// 6.30.104 2021-01-06 19:06:44 I'm releasing just to clean-up the logs.
	//			I've implemented SueDeCoq in the als package, and also tried
	//			but failed at DeathBlossom. 
	// 6.30.105 2021-01-12 10:05:45 Ship SueDeCoq. Failed at DeathBlossom.
	//			Last top1465 3:04 is OK.
	// 6.30.106 2021-01-14 09:02:46 Ship DeathBlossom.
	//			Last top1465 4:10 is a minute slower, but that's still OK coz
	//			I have no love for DeathBlossom, so I won't be using it.
	// 6.30.107 2021-01-15 09:44:47 Ship DeathBlossom.
	//			Last top1465 3:24 that's better, and a better test-case too.
	// 6.30.108 2021-01-23 14:40:48 Faster align2.AlignedExclusion, which still
	//			isn't competitive with the old align.Aligned*Exclusion classes
	//			but it's getting there, and it's just SOOOO much more succinct.
	// 6.30.109 2021-02-13 07:00:49 BigWings, faster Complex and Kraken fish.
	//			Wrote the BigWing class to succinctify the big wings. Tried and
	//			mostly failed to make Complex and KrakenFisherman faster.
	// 6.30.110 2021-02-17 14:31:50 Faster Complex and Kraken fisherman.
	// 6.30.111 2021-02-23 08:33:50 Oops! Last release used align2.


	// To Build:
	// 0. search for @todo and deal with them. A few hangovers is OK. 5 isn't.
	//    If you can't deal with it then change it to a @strech.
	// 1. search for @check and fix any settings you've changed
	//    * ensure you find usages of diuf.sudoku.utils.Debug.breakPoint
	// 2. run the test cases (Alt F6)
	//    * if any failures: dismiss or (fix and start again!)
	// 3. change the below VERSION and BUILT settings
	//    * add a comment to above builds-list: seconds = build number % 60
	// 4. if you haven't run the batch lately (you need a log-file):
	//    * clean and build LogicalSolverTester (aka the batch)
	//    * run it in CMD.
	// 5. add a comment to diuf.sudoku.solver.LogicalSolverTimings
	//    * using the log-file from a recent batch run (Step 4).
	// 5. clean and build the <default config> (the bloody GUI, not the batch)
	//    * try running the batch again, it should tell you to get stuffed.
	// 6. create a back-up IMMEDIATELY: No code changes.
	//    * do it IMMEDIATELY coz I stuffed-up previously, changing comments
	//      before committing, and so committed the ultimate putz-act:
	//      Checked-in code no compile. Procedures matter!
	//    * Put the .7z filename in bottom of LogicalSolverTimings comment.
	//
	// To Ship:
	// 1. rename .7z to .LOGS.7z and then delete the .log files from the
	//    DiufSudoku directory; then create the .7z for shipping (as above).
	//    There's no point shipping zipped-log-files. They're ____in HUGE.
	// 2. create a $release.README.txt from __RFC_LIST.txt
	// 3. Ship the .7z and .README.txt on
	//    https://sourceforge.net/projects/sudoku-explainer/
	//
	// See Also:
	//    ./__how_to_publish_this_project.txt

	public static final String TITLE = "DiufSudoku";
	public static final String VERSION = "6.30.111";
	public static final String BUILT = "2021-02-23 08:33:50";
	// APPLICATION_TITLE_AND_VERSION is just too long, so I went bush!
	public static final String ATV = TITLE+" "+VERSION;

	/**
	 * The boolean setting name Strings are public so that here is THE name of
	 * each boolean setting, to keep it consistent throughout. I do it this way
	 * now because a simple spelling mistake cost me an hour. Sigh.
	 * <p>
	 * NOTE WELL that if you forget to add a new field to BOOLEAN_FIELD_NAMES
	 * it won't be saved or retrieved by the Settings load and save methods,
	 * so it'll switch in the GUI, but won't persist between sessions.
	 */
	public static final String isAntialiasing, isShowingMaybes
		, isFilteringHints, isHacky, isGreenFlash
		, isa4ehacked, isa5ehacked, isa6ehacked, isa7ehacked, isa8ehacked, isa9ehacked, isa10ehacked
		;

	/**
	 * A List of the names of all my "standard" boolean fields. Each string has
	 * ONE definition rather than (as just happened) misslepping the bastards!
	 * <p>
	 * NOTE WELL that if you forget to add a new field to BOOLEAN_FIELD_NAMES
	 * it won't be saved or retrieved by the Settings load and save methods,
	 * so it'll switch in the GUI, but won't persist between sessions.
	 */
	private static final String[] BOOLEAN_FIELD_NAMES = new String[] {
		  isAntialiasing = "isAntialiasing" // GUI painting
		, isShowingMaybes = "isShowingMaybes" // GUI little grey digits
		, isFilteringHints = "isFilteringHints" // GUI hide extraneious hints
		, isHacky = "isHacky" // enables top1465 nasty special treatments
		, isGreenFlash = "isGreenFlash" // GUI flashBackgroundAqua
		, isa4ehacked = "isa4ehacked" // users choice
		, isa5ehacked = "isa5ehacked"
		, isa6ehacked = "isa6ehacked"
		, isa7ehacked = "isa7ehacked"
		, isa8ehacked = "isa8ehacked" // always true
		, isa9ehacked = "isa9ehacked"
		, isa10ehacked = "isa10ehacked"
	};

	public static final EnumSet<Tech> ALL_TECHS = EnumSet.allOf(Tech.class);

	public static final Settings THE = new Settings(); // this is a Singleton

	// DATE and TIME formats for the project.
	// eg: 2019-10-03.07-34-17 meaning that it's thirty-four minutes past seven
	// o'clock in the morning on Thursday, October the third in the year of our
	// halfwitted hot-crossed pillock two thousand and nineteen, but it's OK.
	// I've got my teeth back-in now! And I don't want to Chineese either.
	// 'F' ISO 8601 complete date formatted as "%tY-%tm-%td".
	// 'H' 2 digit 24-hour  : 00 - 23.
	// 'M' 2 digit Minute   : 00 - 59.
	// 'S' 2 digit Seconds  : 00 - 60 (leap seconds). (More in MyTester)
	public static final String DATE_FORMAT = "%1$tF";
	public static final String TIME_FORMAT = "%1$tH-%1$tM-%1$tS";
	public static final String DATE_TIME_FORMAT = DATE_FORMAT+"."+TIME_FORMAT;
	// set ONCE by LogicalSolverTester at the start of a run using now()
	public static String now;
	private static long startNanos;

	public static String now() {
		startNanos = System.nanoTime();
		return now = String.format(DATE_TIME_FORMAT, new Date());
	}
	public static String took() {
		return elapsed(System.nanoTime() - startNanos);
	}
	public static String elapsed(long nanos) {
		return MyStrings.format("%,d", nanos);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ instance stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final Preferences preferences;
	private final Map<String, Boolean> booleans = new HashMap<>(16, 0.75F);

	private String lookAndFeelClassName = null;
	private EnumSet<Tech> wantedTechniques = EnumSet.noneOf(Tech.class); // an empty EnumSet of Tech's

	private int modificationCount; // defaults to 0 automagically
	private static final String MODIFICATION_COUNT_KEY_NAME = "mod";

	private Settings() {
		// glom onto the Preferences for the application
		preferences = Preferences.userNodeForPackage(Settings.class);
		load();
		xml = this.toXml();
	}

	public Rectangle getBounds() {
		return new Rectangle(
			  preferences.getInt("x",0)
			, preferences.getInt("y",0)
			, preferences.getInt("width",3000)
			, preferences.getInt("height",1080)
		);
	}

	public void setBounds(Rectangle bounds) {
		try {
			preferences.putInt("x", bounds.x);
			preferences.putInt("y", bounds.y);
			preferences.putInt("width", bounds.width);
			preferences.putInt("height", bounds.height);
			preferences.flush();
		} catch (BackingStoreException ex) {
			StdErr.whinge(ex);
		}
	}

	public boolean get(String name) {
		return get(name, false); // the default default is false.
	}
	// this get method (with defualt) is currently only used by the set method.
	public boolean get(String name, boolean defualt) {
		Boolean r = booleans.get(name);
		if ( r == null )
			return defualt;
		return r.booleanValue();
	}
	// if the registry entry does not exist yet we return value, as well as set
	// the registry entry to value; which is a bit odd, but it works for me.
	public boolean set(String name, boolean value) {
		boolean pre = get(name, value);
		this.booleans.put(name, value);
		return pre;
	}

	public boolean getBoolean(String nom, boolean defualt) {
		try {
			return preferences.getBoolean(nom, defualt);
		} catch (Exception ex) {
			StdErr.carp("preferences.getBoolean("+nom+", "+defualt+") failed", ex);
			return false; // you can't get here!
		}
	}

	public int getInt(String name, int defualt) {
		try {
			return preferences.getInt(name, defualt);
		} catch (Throwable eaten) {
			return defualt;
		}
	}
	public void putInt(String name, int value) {
		try {
			preferences.putInt(name, value);
		} catch (Throwable eaten) {
			// Do nothing
		}
	}

	public String getLookAndFeelClassName() {
		return lookAndFeelClassName;
	}
	public String setLookAndFeelClassName(String lookAndFeelClassName) {
		String pre = this.lookAndFeelClassName;
		this.lookAndFeelClassName = lookAndFeelClassName;
		return pre;
	}

	public int getNumWantedTechniques() {
		return wantedTechniques.size();
	}
	public EnumSet<Tech> getWantedTechniques() {
		return EnumSet.copyOf(wantedTechniques);
	}
	public EnumSet<Tech> setWantedTechniques(EnumSet<Tech> techs) {
		EnumSet<Tech> pre = EnumSet.copyOf(wantedTechniques);
		wantedTechniques = techs;
		return pre;
	}
	public void justSetWantedTechniques(EnumSet<Tech> techs) {
		wantedTechniques = techs;
	}

	public boolean areAllWanted(Tech... rules) {
		for ( Tech r : rules )
			if ( !this.wantedTechniques.contains(r) )
				return false;
		return true;
	}

	public int getModificationCount() {
		return modificationCount;
	}

	private void load() {
		if (preferences == null)
			return; // What else can we do here?
		try {
			for ( String fieldName : BOOLEAN_FIELD_NAMES )
				booleans.put(fieldName, preferences.getBoolean(fieldName, true));
			lookAndFeelClassName = preferences.get("lookAndFeelClassName", lookAndFeelClassName);
			wantedTechniques.clear();
			for ( Tech t : ALL_TECHS )
				if ( preferences.getBoolean(t.nom, true) )
					wantedTechniques.add(t);
			modificationCount = preferences.getInt(MODIFICATION_COUNT_KEY_NAME, Integer.MIN_VALUE);
//			System.out.println("Settings.load: mod="+mod);
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	public void save() {
		if (preferences == null)
			return; // What else can we do here?
		try {
			for ( String fieldName : BOOLEAN_FIELD_NAMES )
				preferences.putBoolean(fieldName, get(fieldName)); // defaults to false!
			if ( lookAndFeelClassName != null )
				preferences.put("lookAndFeelClassName", lookAndFeelClassName);
			for ( Tech t : ALL_TECHS )
				preferences.putBoolean(t.nom, wantedTechniques.contains(t));
//			// increment and store the modification count
			preferences.putInt(MODIFICATION_COUNT_KEY_NAME, ++modificationCount);
			try {
				preferences.flush();
			} catch (BackingStoreException ex) {
				StdErr.whinge(ex);
			}
			// update the xml which backs equals, hashCode and clone
			xml = this.toXml();
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	/**
	 * Returns a byte array containing the XML of my preferences.
	 * @return byte[] of preferences XML
	 */
	public byte[] toXml() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			preferences.exportSubtree(baos);
			return baos.toByteArray();
		} catch (Exception ex) {
			StdErr.carp("Settings.equals "+ex.getClass().getSimpleName(), ex);
			return null; // you can't get here
		}
	}

	/**
	 * Does the given Settings contain the same Preferences as this Settings?
	 * NOTE: equals compares the Preferences only, so if any settings are (in
	 * future) persisted elsewhere, then you'll also need to compare them here.
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null
			&& (obj instanceof Settings)
			&& equals((Settings)obj);
	}
	/**
	 * Does the given Settings contain the same Preferences as this Settings?
	 * <p>
	 * WARN: other is presumed to be NOT null!
	 * <p>
	 * NOTE: equals compares the Preferences only, so if any settings are (in
	 * future) persisted elsewhere, then you'll also need to compare them here.
	 * @param other the other Settings to compare this to.
	 * @return java.util.Arrays.equals(xml, other.xml);
	 */
	public boolean equals(Settings other) {
		return Arrays.equals(xml, other.xml);
	}

	/**
	 * Returns the identity hashCode: ie Object.hashCode(), to guarantee that
	 * a Settings object always reports the same hashCode to Hash*, breaking
	 * the spirit of the equals-hashCode contract! That the hashCode can and
	 * probably should change when the fields compared by equals change.
	 * <p>
	 * I do not expect this to cause issues because you wouldn't create a Hash*
	 * of Settings! There is one Settings per app! Hence the hashCode debate is
	 * a ____ing waste of brain.
	 * @return super.hashCode();
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Settings copy = (Settings) super.clone();
		copy.lookAndFeelClassName = this.lookAndFeelClassName;
		copy.wantedTechniques = EnumSet.copyOf(wantedTechniques);
		copy.xml = copy.toXml();
		return copy;
	}
	byte[] xml;

}
