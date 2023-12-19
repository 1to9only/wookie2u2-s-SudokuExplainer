/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * Build holds the project-name, version number and build date-time,
 * and quite a large comment: one for each build.
 * <p>
 * <pre>
 * A precis of ./__how_to_publish_this_project.txt
 *
 * To Build:
 * 0. search for @todo and deal with them. A few hangovers is OK. 5 isnt.
 *    If you cant or wont deal with it now then change it to a @strech.
 *    Im more relaxed about @strechs because theyre a stretch.
 * 1. search for @check and fix any config you have changed
 *    * in a hurry: false.*@check true|true.*@check false
 *    * find usages: {@link diuf.sudoku.utils.Debug#breakpoint() }.
 *      Its embarrassing when they make it into prod, but mostly harmless.
 * 2. run the test cases (Alt F6)
 *    * if any failures: dismiss/fix them all and then start again. A few known
 *      test-case failures are OK. More than about three sux.
 *    * you may need to REPRODUCE some to update the hint-html. Careful with
 *      that axe Eugeene. Diff the EXPECT and ACTUAL html in your text-editor.
 * 3. change the below VERSION and BUILT strings
 *    * change the build number and date time (in this class)
 *      - build-time-seconds = build number % 60
 *    * add a comment at the end of this comment-block
 * 4. if you havent run the batch lately (you need a log-file):
 *    * clean and build LogicalSolverTester (aka the batch)
 *    * run it in CMD with batch.bat (java.exe seems to optimise better than
 *      Netbeans java-runtime, but that might just be my perception), and one
 *      does NOT want to become Netbeans dependant, which may happen silently
 *      if thats your only execution environment.
 * 5. add a comment to {@link diuf.sudoku.BuildTimings}
 *    * using the log-file from a recent batch run (Step 4).
 * 5. clean and build the &lt;default config&gt; (the GUI, not the batch, putz)
 *    * try running the batch again. it should produce an error message.
 *    * change the file-name to SudokuExplainer.log and go again:
 *      * Does the GUI work at all?
 *      * Help ~ about: is the release number date and time correct?
 *      * Analyse some puzzles (happens when you Ctrl-P next-puzzle). Look ok?
 *      * F5-enter through the first 2-or-3 puzzles in top1465.d5.mt. Look ok?
 *      * Generate an IDKFA, or two. No crash?
 *      * Ctrl-L your recently modified hint-types. Look ok?
 * 6. commit: create a .7z back-up IMMEDIATELY: No code changes!
 *    * ANY line change renders production stack-traces misleading.
 *    * I once broke it fixing comments after testing but before committing.
 *
 * To Ship:
 * 1. delete *.log from DiufSudoku; then create the .7z for shipping.
 *    No point shipping log-files.
 * 2. create a $release.README.txt from README.txt
 * 3. Ship the $release.7z and $release.README.txt to
 *    https://sourceforge.net/projects/sudoku-explainer/
 *
 * The build list:
 * 6.28.139 Got solve basically right: fine tune for speed.
 * 6.29.000 save format = toString 1x81 values + 1x81 maybes (was 9x9 values
 *			+ 9x9 maybes). Its backwards compatible, so a minor release.
 * 6.29.001 flash aqua when solves with singles.
 * 6.29.002 fix bugs in undo. Ctrl-Z only, NO Ctrl-Y (Sigh) Also only in
 *			user-changes, auto-changes cannot be undone. How to?
 * 6.29.003 single click to set cell or remove maybe.
 * 6.29.004 Ctrl-P generates next when the generate dialog is open.
 * 6.29.005 fixed bug in display of BUG type 4, and added a test for it.
 * 6.30.001 been effing around with stuff for a while so I thought VERSION.
 * 6.30.006 Removed sort checkBox. Faster AChainingHint equals. Faster
 *			contains in HintsAccumulator.add. Faster Point & Claim.
 * 6.30.007 Make nesters user-selectable again. Removed cache of
 *			ancestors + numAncestors in the Ass in AChainingHint.
 *			Inlined oc = anOff.otherCell(r) in Chainer.findOffToOns(...).
 * 6.30.008 Fixed funky nesting CellReductionHints caused by hint in "raw"
 *			grid. No biggy coz conclusion correct, tho formative logic mad.
 *			Also checked hints hashCodes.
 * 6.30.009 Finally understand those funky Nested* CellReductionHints.
 *			If source.cell != this cell refect/ignore hint.
 *			Added AlignedQuad + AlignedPent is ludicrously slow.
 * 6.30.010 Disallowed AlignedPent (unless youre a programmer) too slow.
 *			Rewrote the introduction. Fixed bug in Grid.setTheCellsValue.
 *			Fixed bad numHints and numElims. Cleaned out the to-do list.
 *			Right-click on hintsTree and hintDetails copy to clipboard.
 * 6.30.011 2019-09-09 14:55:11 Created AlignedQuadExclusion relegating
 *			AlignedSetExclusion to the dead-code bucket. Moved UnaryChain
 *			into heavies before ATEs and AQEs, as per the original Juillerat
 *			implementation. Demangled LogicalSolverTester Log.MODEs.
 * 6.30.012 2019-09-11 07:06:12 Moved Aligned* into chainers for 1465 in
 *			132,611,805,962 @ 90,520,004. Thats 5 second pass in a Datsun
 *			120Y, driven by a suoer-heavy-weigth Sumo. Hes in the boot.
 * 6.30.013 2019-09-11 relegated AlignedSetExclusion to the abstract class
 *			AAlignedSetExclusion and modified AlignedTripleExclusion,
 *			AlignedQuadExclusion, and AlignedPentExclusion to use protected
 *			methods exposed therein, to mostly-clean-up cut-n-paste coding.
 *			Also moved A5E to just before DynamicPlus in the LogicalSolver
 *			chainers list. Its still too slow, but doesnt matter so much.
 * 6.30.014 2019-09-15 Cleaning up "dodgy" Nested hints (again). The
 *			AChainingHint getChainHtmlLinesRecursively method now adds the
 *			nestedChain to explanation of initial asses parents because the
 *			explanation is totaly useless without them, even though its
 *			illegible with them. AChainingHint getSource moved to static
 *			Chainer method (which it calls) is overridden by CellReductnHint
 *			to search if source.cell!=srcCell. Exhumed IrrelevantHintExceptn
 *			from AlignedExclusionHint and put it in the getHtmlImpl of
 *			subtypes of AChainingHint: BinaryChainHint, CellReductionHint
 *			and RegionReductionHint. Thatll do pig, I think.
 * 6.30.015 2019-09-18 Cleaned up multiple queues in Chainer doCycle and
 *			doUnary, and repeatedly failed at doChains, then figured out
 *			why. I wont forget. Moved LSHs to a Hash Class. Moved AHint
 *			Comparators to the AHint class. Redisaponated. Re-removed
 *			green/red (no orange) from the result maybe. Cant decide!
 * 6.30.016 2019-09-18 (again) Added Difficulty.Nightmare and IDKFA, and
 *			adjusted all the difficulties. Also added max-difficulty of
 *			each hint-type to Analysis output. Made generator + PuzzleCache
 *			more flexible to handle more (or less) Difficulty types.
 * 6.30.017 2019-09-20 made AlignedPairExclusion about 25% faster by redoing
 *			the populateCandidatesAndExcluders section. Now you can drag
 *			the toString-puzzle-text directly from Notepad++ into the grid
 *			to paste it. Created a flat cells array in the Grid which is
 *			used in preference to the matrix.
 *			1465 in 136,138,068,154 ns @ 92,927,008 each.
 * 6.30.018 2019-09-22 changed Aligned*Exclusion so that it does just the
 *			allow part of allow-or-exclude ASAP, and then does the full
 *			allow-or-exclude loop when we know theres going to be a hint.
 *			1465 in 140,987,714,832 ns @ 96,237,348
 * 6.30.019 2019-09-23 10:37 finally got Aligned*Exclusion right so that it
 *			does just the allow part of allow-or-exclude ASAP, and then we
 *			come back only if a value is not allowed to collect the full
 *			exclusion map. So in CMD with isRemovingLonelyExcluders = true:
 *			10:27:27 1465 in 115,977,000,571 ns @ 79,165,188 ns/puzzle.
 * 6.30.020 2019-09-23 17:32 polished all the Aligned*Exclusion code,
 *			including AlignedPairExclusion A2E. With A4E+A5E on:
 *			17:25:02 1465 in 144,328,015,050 ns @ 98,517,416 ns/puzzle.
 * 6.30.021 2019-09-29 11:55:55 still hacking Aligned*Exclusion. Got idxs
 *			right finally, I think. So am going to release and version.
 *			11:55:55 F10 in 239,032,018,308 ns @ 23,903,201,830 ns/puzzle
 *			13:06:45 F10 in 219,756,602,397 ns @ 21,975,660,239
 *			with A3E, A4E, and A5E before UnaryChain. Were getting there.
 * 6.30.022 2019-09-30 20:40:22 finished hacking at Aligned*Exclusion.
 *			1465 in 519,384,719,109 @ 354,528,818 ns/puzzle with A3E, A4E,
 *			& A5E all before UnaryChain. I declare that were there yet.
 * 6.30.023 2019-10-01 16:02:23 added AlignedSeptExclusion, coz Im nuts.
 *			Also retrofitted limit on numCandidates to A*E. A tad faster.
 *			1465 in 1,789,400,807,988 (29:49) @ 1,221,433,998 with A7E.
 * 6.30.024 2019-10-08 06:33:24 Forgive me father it has been a week since
 *			my last release. Added A8E. Renamed A*E to use a digit in-place
 *			of the wogalini, so that lists are in order. Still trying to
 *			dedog A*E, so added -REDO to LogicalSolver/Tester to parse the
 *			previous logFile for which hinters to use on each puzzle. Also
 *			diuf.sudoku.test.DiffLogs shows divergences in hint-path of 2
 *			VERBOSE_5_MODE logFiles, to be SURE there are no AWOL hints.
 *			1465 in 715,388,763,279 (11:55) @ 488,319,974 with A8E.
 * 6.30.025 2019-10-09 11:41:25 Still trying to improve A*E performance.
 *			Replaced the excludersMap with an excluders array.
 *			1465 in 580,809,800,933 (9:40) @ 396,457,202 REDO with A8E.
 * 6.30.026 2019-10-17 16:47:26 Added A9E and A10E and thats the last one.
 *			Improved A*E and Chainer performance in LogicalSolverTester
 *			-REDO by only running hintyHinters when theyll hint as
 *			determined by a previous non-REDO logFile.
 *			1465 in 103,514,882,927	(1:43) @ 70,658,623 -REDO with A10E,
 *			but the last non-REDO with everything was 52:04, ie SLOW!
 * 6.30.027 2019-10-26 21:45:27 New AHintNumberActivatableHinter extended by
 *			all heavies, for -REDO only. Also AHint.apply detects and sets
 *			subsequent Naked Singles. Synchronized: generate waits4 analyse.
 *			1465 in 127,904,212,510 (02:07) @ 87,306,629 -REDO with A10E.
 * 6.30.028 2019-11-01 07:49:28 Logging supressed while generating, and also
 *			sychronised to make the background generate thread wait for the
 *			foreground analyse to complete. Building now for a backup when
 *			I subtype Chainer into UnaryChainer and ChainerMulti.
 *			1465 in 135,020,038,901	(02:15) @ 92,163,849 -REDO with A10E.
 * 6.30.029 2019-11-02 08:50:29 Split Chainer into AChainerBase, UnaryChainer,
 *			and ChainerMulti. All test cases passed first time. WTWTF?
 *			1465 in 136,386,363,363	(02:16) @ 93,096,493 -REDO with A10E.
 * 6.30.030 2019-11-11 15:01:30 I just thought I should build and release.
 *			1465 in 133,050,598,498	(02:13) @ 90,819,521 -REDO with A10E.
 * 6.30.031 2019-11-12 12:15:31 A*E now checks each partially built combo
 *			isnt a superset of the maybes of any common excluder cells,
 *			so we dont bother building and testing the rest of the combo
 *			which wont be allowed anyways. This makes a huge
 *			difference to the full runtime, ie not in the faster -REDO mode
 *			1465 in 2,688,882,903,833 (44:48) @ 1,835,414,951 with A10E.
 *			This is down from a top of 58 minutes. A great relief.
 *			Also noticed that many A4Es were being missed by the previous
 *			release (atleast). I wonder how the hell that happened? Fixed!
 * 6.30.032 2019-11-17 18:13:32 A*E now removes any common excluders which
 *			are a superset of any other common excluder. Its faster again.
 *			1465 in 2,509,103,876,888 (41:49) @ 1,712,698,892 with A10E.
 * 6.30.033 2019-11-22 11:48:33 A5+E now removes maybes of subsequent cells
 *			when the cells are siblings. Its much faster again because it
 *			does more of the work less often.
 *			1465 in 1,889,295,456,806 (31:29) @ 1,289,621,472 with A10E.
 *			Thats nearly half of the worste of 58 minutes. Sweet relief!
 *			1465 in   115,789,714,781 (01:55) @    79,037,347 REDO
 *			I declare 2 minutes as the border line of acceptable.
 * 6.30.034 2019-11-29 12:57:34 A*E now loops only while shiftedValue is
 *			less than or equal to the cells maybes bits, and theres a
 *			numCmnExclBits == 1 test in every Aligned*Exclusion class.
 *			1465 in 1,491,209,814,826 (24:51) @ 1,017,890,658 with A10E.
 *			which is about six and half minutes faster than the previous.
 *			1465 in   120,330,712,914 (02:00) @    82,137,005 REDO
 * 6.30.035 2019-12-10 14:28:35 Reverted to 6.30.034 2019-11-29 12:57:34 so
 *			now theres 2 of each A456E: correct and hacked: and CheckBoxs
 *			in TechSelectDialog to choose between them. Turns out correct
 *			A5E + A4E takes 48 minutes, which still looks OK in the GUI.
 *			Also changes to Config, and to hintDetailsHtml handling.
 *			The reversion broke debug + run in the IDE. I hate Netbeans.
 * 6.30.036 2019-12-12 14:04:36 Updated tests for A456E. Theres still bug
 *			that I dont understand with setting boolean preferences (just
 *			change the bastard using regedit). Im releasing anyway because
 *			Ive just got all the tests working again, so its easy.
 * 6.30.037 2019-12-24 18:52:37 Im releasing because its been too long
 *			since my last release. Ive been finding a fixing a few bugs.
 *			The only notable is making hidden singles (IIRC) and Chainers
 *			correctly return "any found", so no more extra searches.
 *			21:52:37 GUI saves/restores its bounds to/from the registry.
 * 6.30.038 2019-12-31 17:16:38 Im still pissing about with A*E. Ive put
 *			the switch statement back in to cover, 1, 2, other number of
 *			common excluder bits. A4..10E all have correct and hacked
 *			versions, with a CheckBox in TechSelectDialog for each.
 * 6.30.039 2020-01-10 15:52:39 Still working on A*E: Its skips when total
 *			number of maybes is out-of-range from magic numbers which I
 *			collected earlier. Its a HACK specific to top1465.d5.mt, but
 *			it works, and I have a feeling that another puzzle set would
 *			produce very similar numbers, maybe a difference of 1-or-2 here
 *			or there. So its a nasty little HACK but it works!
 * 6.30.040 2020-01-17 12:38:40 Still still working on A*E, which filters
 *			by various stats. Its down to 4 hours with HACKs specific to
 *			top1465.d5.mt.
 * 6.30.041 2020-01-19 07:32:41 A*E now all have at least "starter level"
 *			filters using the Counter class on mbs maybes size,
 *			col collisions, and ces common excluders size.
 * 6.30.042 2020-01-28 21:11:42 A7E_1C now has "top shelf" filters.
 *			It sorts the cells in the aligned set by:
 *				numMaybesCollisions*4 + cmnExclHits*2 + maybesSize,
 *			and filters by every stat I can think of. The total runtime is
 *			down to ? hours ? minutes with A234567E correct and A8910E
 *			hacked. Ive also replaced Integer.bitCount calls with Values
 *			or Indexes SIZE look-ups. Am waiting for run in order to test.
 *			Im reading Stephen Hawkins "The Big Questions". Its great!
 * 6.30.043 2020-01-31 19:08:43 Finished A7E_1C collisionComparator.filter,
 *			and exhumed siblingsCount filter to apply it earlier, bringing
 *			it down to 118:56 (under 2 hours) for A234567E_1C + A8910E_2H.
 * 6.30.044 2020-02-03 09:56:44 Scratching for A7E_1C performance:
 *			* tune DOG_1A "is empty tests" = about 7 minutes on top1465
 *			* reduced A7E_1C MAX_CANDIDATES from 60 to 57 = 8 minutes
 *			* revert to avb* virtual array = 3 minutes
 *			top1465.d5.mt down to 102:55 with A234567E_1C + A8910E_2H.
 *			I really really want to get under 100 minutes!
 * 6.30.045 2020-02-04 15:03:35 promulgated collisionComparator and any*
 *			technique to other A*Es. Also ANSI-Ced all vars in hit files.
 * 6.30.046 2020-02-06 00:40:46 fixed bugs in the filters of A8E+A10E, and
 *			fixed bug in collisionComparator.score which ommits COLLISIONS!
 *			Declare ALL variables at the start of the method, like ANSI-C!
 *			top1465.d5.mt down to 95:52 with A234567E_1C + A8910E_2H
 *			I broke 100! I broke 100! I broke 100! I broke 100!
 * 6.30.047 2020-02-10 20:07:47 isAnyAllowed* in:
 *			* A7E_1C from 3,259 to 1,420 saving 30:39
 *			* A7E_2H from 27:43 but Ive nothing to compare it to.
 *			* A6E_1C from 2,199 to   735 saving 24:24
 *			* A6E_2H from   108 to    50 saving  0:58
 *			* A5E_1C from   548 to   336 saving  3:32
 *			* A4E_1C from   149 to    79 saving  1:10 (REVERTED!)
 *			top1465.d5.mt down to 51:16 with A234567E_1C + A8910E_2H
 * 6.30.048 2020-02-11 20:39:48 isAnyAllowed* in A8E_2H and A8E_1C, and
 *			thats as far as Im going to push it, so this is my last
 *			release, I guess. Its been not fun exactly, more engaging.
 *			top1465.d5.mt 1:38:00 with A2345678E_1C + A9/10E_2H
 * RERELEASE 6.30.048 2020-02-12 09:29:48 uncomment is1AnyAllowed4567
 *			in A8E_1C is nearly 10 minutes faster.
 *			top1465.d5.mt 1:28:21 with A2345678E_1C + A9/10E_2H
 * 6.30.049 2020-02-16 16:39:49 Ive given up trying to make A8E_1C fast
 *			enough to use practically. Its acceptable the GUI, so itll
 *			have to do. Top1465 in 111:50 with terrain following enabled.
 * 6.30.050 2020-02-21 19:35:50 I gave up trying to get A8+E fast enough to
 *			use practically, and cheated. Each A*E now uses an IntegerSet
 *			to remember which puzzles it hints from, ignoring others,
 *			bringing top1465.d5.mt down to 53 minutes with all A*Es in
 *			"correct" mode. Thatll have to do.
 * 6.30.051 2020-02-22 11:59:51 Adjusted A*E_2H filters to the gsl filter,
 *			but Ive got a feeling that some filters cost more than they
 *			save. You can go tune it up to warp-factor-9, Im too lazy.
 *			top1465 in 4:26 with A234E_1C and A5678910E_2H. Thatll do.
 * 6.30.052 2020-02-25 18:10:52 Bought A4E into line with A5E_1C with
 *			anyLevel through-out, and full filters. Using ARRAY aoap.
 *			New NUM_TRAILING_ZEROS. Updated inline otherCell in offToOns.
 *			top1465 !IS_HACKY in 11:46 with A5678910E_2H.
 *			top1465  IS_HACKY in 50:46 with A2345678910E_1C. Shell do.
 * 6.30.053 2020-02-28 11:35:53 Just pissing about with the performance of
 *			HiddenSet, and now NakedSet whose code changes are in this
 *			release despite not being included in this run:
 *			top1465 IS_HACKY in 53:30 with A2345678910E_1C is 2:44 SLOWER!
 *			top1465 IS_HACKY in  3:38 with A5678910E_2H is 8:08 faster.
 * 6.30.054 2020-03-05 21:46:54 Use a filter IntegerSet to expedite A*Es,
 *			coz its much simpler than all those custom Counter filters.
 *			top1465 IS_HACKY in 3:04 with A2345678910E_1C is BS QUICK!
 * 6.30.055 2020-03-06 21:59:55 Put a WindexSet in each A*E to only examine
 *			only those aligned sets which produced a hint last time.
 *			top1465 IS_HACKY in 39:16 with A2345678910E_1C. Shell do.
 * 6.30.056 2020-03-09 19:05:56 The UBER HACK replace HitSet, FilterSet and
 *			WindexSet with a new HitSet class, so all A*Es skip if there
 *			was no hint for this puzzle/hintNumber last run; and also jump
 *			straight to the aligned set which hinted here last run.
 *			top1465 IS_HACKY in 1:47 with A2345678910E_1C. Itll do.
 *			top1465 IS_HACKY in 1:53 with A5678910E_2H coz now all A*Es
 *			take about equal time, but the hacked ones produce less hints.
 *			But without the hacks its still too slow to be useful:
 *			top1465 !IS_HACKY in     9:16 with A5678910E_2H is fast enough
 *			top1465 !IS_HACKY in 26:56:19 with A2345678910E_1C is too slow
 *			42#top1465 took 76 minutes, 74 mins of which was Aligned Dec.
 * 6.30.057 2020-03-?? ??:??:?? A*E: Inlined covers method in default:s.
 *			Put IS_HACKY in registry. Wired the GUI upto the HitSet.
 *			Ran A234567E with & without to compare number of aligned sets.
 *			Ill have to "hardcode hackery" in A8910E in order to run them
 *			in this-sort-of-scenario coz running it !IS_HACKY takes 2 days:
 *			A8E_1C 3 hrs  +  A9E_1C 9 hrs  +  A10E_1C 26 hrs
 *			top1465 !IS_HACKY in 16:00 with A5678910E_2H is 6:45 slower!
 *			which I think must be the inlined covers method. How test?
 * 6.30.058 2020-03-19 18:22:58 Still pissin about with A*Es. Pushed the
 *			HitSet + prepare and report methods up into the abstract
 *			AAlignedSetExclusionBase class. Stuck basic filtering in A10E
 *			even when !isHacky coz vanilla took 34 hours (down to 22 hrs).
 * 6.30.059 2020-04-03 16:37:59 HoDoKu SingleDigitPattern first pass.
 *			Downloaded HoDoKu code to boost from. Started with sdp which is
 *			now complete: Skyscraper, TwoStringKite are algorithmically "as
 *			is". EmptyRectangle is totaly my code, but still based on
 *			hobiwans (HoDoKu) ideas. WWing (in wing package with XYWing and
 *			XYZWing, which it most resembles, is also basically boosted "as
 *			is" from HoDoKu. Forgive me father, its been 3 weeks since my
 *			last release!
 * 6.30.060 Got lost in the fog, or something.
 * 6.30.061 2020-04-10 15:38:01 HoDoKu FinnedFisherman second pass, finding
 *			all Finned (non Sashimi) Fish, and test cases.
 * 6.30.062 2020-04-11 16:32:02 HoDoKu SashimiFisherman first pass, finding
 *			all Sashimi Fish, and test cases.
 * 6.30.063 2020-04-21 23:17:03 HoDoKu ALS-XZ and ALS-XY-Wing first pass,
 *			with ALS-XY requiring a dodgy check against the solution
 *			from BruteForce, and ALS-XY-Wing being slow and not finding
 *			any, and Im pretty sure its broken. No test cases yet.
 *			Last top1465: 08:58 with A5..10E Hacked and all the new s__t
 *			except ALS-XY-Wing which takes about 12 minutes to find nada.
 * 6.30.064 2020-04-30 10:41:04 HoDoKu ALS-XZ 2nd and 3rd pass: got ALS-XZ
 *			working properly and optimized for speed, with two test-cases.
 *			ALS-XY-Wing still finds no hints, so hes next.
 *			Last top1465: 14:54 with A5..10E hacked and ALS-XZ
 * 6.30.065 2020-05-07 12:34:05 HoDoKu ALS-XY-Wing second pass. Reverted to
 *			6.30.064 and did the minumum required to get hobiwans code for
 *			ALS-XY-Wing working in DIUF. Still no testcase for it.
 *			Last top1465: 12:39 with A5..10E hacked + ALS-XZ + ALS-XY-Wing
 * 6.30.066 2020-05-23 17:23:06 HoDoKu ALS-XY-Wing third pass. Reverted to
 *			6.30.065 multiple times to get it to work properly; but in the
 *			end I stuck-with hobiwans code, so imported all relevant code
 *			into diuf.sudoku.solver.hinters.alshdk
 *			[2020-09-23 renamed to diuf.sudoku.solver.hinters.als]
 *			Last top1465: 12:09 with A5..10E hacked + ALS-XZ + ALS-XY-Wing
 * 6.30.067 2020-05-25 10:12:07 HoDoKu ALS-XY-Chain first pass. Copy-pasted
 *			HoDoKus ALS-XY-Wing hinter into HdkAlsXyChain. Both still use
 *			invalid(Hint) which is just like a total dirty little hack.
 *			Last top1465: 11:57 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.068 2020-05-26 10:16:08 HoDoKu ALS-XY-Chain second pass: both
 *			HdkAlsXyChain and HdkAlsXyWing use clean to remove invalid
 *			eliminations and skip if that empties the redPots. Dirty HACK!
 *			Last top1465: 12:15 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.069 2020-05-26 11:47:09 HdkAlsXyChainTest and HdkAlsXyWingTest.
 *			DiufSudoku_V6_30.069.2020-05-26.7z
 * 6.30.070 2020-06-05 09:32:10 Revert DiufSudoku_V6_30.069.2020-05-26.7z
 *			and fix some (not all) ALS-XY-Wing and ALS-Chain bugs. Chain
 *			ignores currALS which is subset (or viceversa) of one in chain.
 *			ALS-XY-Wing had some extranious blues.
 * 6.30.071 2020-06-05 20:09:11 Finally got rid of ALL invalid hints from
 *			HdkAlsXyChain by ignoring cells which are involved in an any
 *			actual locked set in this region, but its a tad slower.
 *			Last top1465: 12:27 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.072 2020-06-15 12:47:12 Merged my AlsXz into the alshdk package and
 *			removed my als package.
 *			[2020-09-23 renamed alshdk to diuf.sudoku.solver.hinters.als]
 *			Last top1465: 12:56 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.073 2020-06-22 22:17:13 Improved GUI after Sue test. Locking
 *			now detects Siamese hints and defers to Hidden Pair/Triple, and
 *			then removes each hint whose redPots is a subset of anothers.
 *			Last top1465: 12:56 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.074 2020-06-27 14:12:14 Fixed prepare/valid bugs in HdkAls* so that
 *			Ctrl-L now works properly. Fixed all test cases.
 *			Last top1465: 13:04 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.075 2020-06-29 21:35:15 Made Idxs forEach faster by bit-twiddling.
 *			Last top1465: 12:36 with A5..10E hacked + ALS-XZ+XyWing+XyChain
 * 6.30.076 2020-07-01 19:45:16 Performance tuning with the Profiler.
 *			Faster build siblings in Grid constructor in BruteForce, also
 *			dramatically reduces memory usage. Iterate Indexes.ARRAYS in
 *			region chains. Also some code clean-up: consistent naming.
 *			Last top1465: 02:40 -A5..10E +ALS-XZ +XyWing -XyChain.
 * 6.30.077 2020-07-10 17:24:17 Using HoDoKus "complex" Fish hinter from
 *			Sudoku Explainer turned out to be pretty easy. Making it run
 *			faster is the difficult part. Its really really really hard!
 *			Last top1465: 05:29 +Franken/Mutant 2/3/4fish.
 * 6.30.078 2020-07-20 11:42:18 Forgive me father its been 10 days since
 *			my last release and I just need to rub one out. Actually I need
 *			to delete my log-files and I want a backup before I do so.
 *			Ive been pissin about trying to make HoDoKu performant, with
 *			some (but not too much) success. The main peformance snags are
 *			hobiwans reliance on "heaver" getters, and Integers bloody
 *			compareTo(Integer) method. Its all crap Ray!
 *			Last top1465: 04:11 +Franken/Mutant/Kraken 2/3/4fish.
 * 6.30.079 2020-07-21 12:26:19 profile to expedite getAllKrakenFishes ->
 *			TablingSolver.buildChain -> TreeMap<Int,Int>.get -> Int.of ->
 *			the size of the IntCache (4K -> 75% hit rate) but now theres
 *			s__tloads of logs so I release to back and clean them up.
 *			Last top1465: 03:20 +Franken/Mutant/Kraken 2/3/4fish.
 * BROKEN:	I also discovered that my desktop "release" SudokuExplainer no
 *			longer runs (HoDoKu ClassNotFound) so I only run in situ now.
 *			I need a real Java programmer to workout whats happened to
 *			the classpath. I just blame Nutbeans! Weird dependancy s__t!
 * 6.30.080 2020-08-01 16:34:20 Been mucking about with HoDoKu FishSolver.
 *			I got it somehow to find shiploads of hints, then reverted coz
 *			I broke it, and now I cant get back to finding all them hints.
 *			Its "working-ish" now, so I brought it over from D:, and I
 *			release to clean-up my log files.
 *			Last top1465: 03:34 +Franken/Mutant/Kraken 2/3/4fish.
 * 6.30.081 2020-08-06 22:26:21 Been pissin about trying to make FishSolver
 *			faster, then tried implementing FrankenFisherman myself and it
 *			sucks. Last top1465: 03:25 +Franken/Mutant/Kraken 2/3/4fish.
 * 6.30.082 2020-08-10 21:42:22 Implemented HdkColoring. Improved speed of
 *			HdkFisherman marginally. Standardised caching.
 *			Last top1465: 03:25 +Franken/Mutant/Kraken 2/3/4fish, but
 *			dropped Nishio (1 hint in 4 secs) and Kraken Swordfish (9 hints
 *			in 21 secs)
 * 6.30.083 2020-08-21 10:08:23 I went through the first 1000 puzzles in
 *			the GUI looking for bugs, weak hint explanations and the like.
 *			Fixed a few "ancillaries" like painting fins blue, and a bug
 *			in Locking mergeSiameseHints.
 *			Last top1465: 03:38 is 13 seconds slower with siamese check.
 *			Siamese is inefficient: processes 21,041 hints to find about a
 *			100 "extra" eliminations that will be found anyway. It wasnt
 *			intended to ever be part of the racing uterus, but I needed it
 *			to find a puzzle which tripped over the bug; and you can switch
 *			it on and off at will. It was on for this run, thats all. Ill
 *			switch it off when I get bored with the whole siamese thing.
 * 6.30.084 2020-08-21 20:37:24 Discovered bug in NakedSet, it was falsely
 *			returning false after finding hints. How embarassment!
 *			Last top1465: 03:24 is 16 seconds faster.
 * 6.30.085 2020-08-31 17:39:25 Simplified colors by adding a getOrange
 *			independent of getGreens and getReds. Fixed all test cases.
 *			Last top1465: 03:38 is 16 seconds slower. Using Direct hinters
 *			so not real worried.
 * 6.30.086 2020-09-01 14:59:26 Fixing logView.
 * 6.30.087 2020-09-03 08:05:27 Fixed logView. Previous release is a bust.
 *			Bugs in the load process makes it not work at all; so I must
 *			release this patched version TODAY if possible. Also fixed
 *			logView, which is what I thought I had done in the previous
 *			release, which was rushed out because I dont control when we
 *			go to town. I should NOT have released untested. That was a BIG
 *			mistake and I wont make it again. Sigh. logView now takes a
 *			Java regex (was just a plain string).
 *			Last top1465 03:03 but no Krakens so dont get excited.
 * 6.30.088 2020-09-07 11:20:28 Release because Im at the library and I
 *			can. No notable changes to the best of my recollection.
 *			Did not actually release this. Ran out if internet connection!
 * 6.30.089 2020-09-12 11:55:29 Bug fix: Subsequent views not shown for
 *			RegionReductionHint and CellReductionHint.
 * 6.30.090 2020-09-24 13:30:30 Boost ColoringSolver as color.Coloring.
 *			Renamed package diuf.sudoku.solver.hinters.alshdk to als.
 * 6.30.091 2020-10-06 18:06:31 Boost HoDoKuFisherman does Finned, Franken,
 *			and Mutant fish. It can do Basic but doesnt coz Fisherman is
 *			faster. Its still too slow for Mutant (not really useable).
 *			Stripped Kraken from hobiwans code, which I plan to put in a
 *			KrakenFisherman class.
 *			Last top1465 18:38 because Mutants are too slow!
 * 6.30.092 2020-10-11 18:46:32 Boost Kraken Fish from HoDoKuFisherman into
 *			KrakenFisherman. It works, but its very very very very slow.
 *			Last top1465 18:38 because Mutants are too slow!
 * 6.30.093 2020-10-14 18:03:33 New version of KrakenFisherman is twice as
 *			fast as original, so now its just very very slow.
 *			Last top1465 35:49 with KrakenSwamp+Sword, but NO Jellyfish.
 * 6.30.094 2020-10-15 15:37:34 Replaced "interleaved" AHint.getRegions
 *			and all subtypes (30 of them) with getBase and getCovers.
 *			This change should have no consequence to non-developers, but
 *			to a techie combining 2 types of things in an array is MENTAL.
 *			Last top1465 2:34 is about expected for the wanted hinters.
 * 6.30.095 2020-10-17 22:25:35 test-cases for Idx, HdkSet and HdkSetBase.
 *			Ive given-up on trying to clean-up the ALS package again. It
 *			alway fails as soon as I replace the HdkSet with my Idx. I do
 *			not know what the problem is, but I do know Im too thick to
 *			work it out... so bugger it, it can stay as-is, its only my
 *			techie pride wanting to eradicate the "foreign" code.
 *			Last top1465 2:34 is about expected for the wanted hinters.
 * 6.30.096 2020-10-18 06:36:36 redo release to include overnight run-log.
 *			Last top1465 132:54 with all the big fish. Not bad. Not God.
 * 6.30.097 2020-10-19 21:14:37 I finally worked-out where I went wrong and
 *			eradicated/implanted the HoDoKu types from the als package.
 * 6.30.098 2020-10-23 23:20:38 I went mad and cleaned-up. There should be
 *			no visible changes, but LOTS of dead and dying code has been
 *			removed. The only notable from a users perspective is that the
 *			Grid constructor now just calls load, instead of doing it all
 *			itself the pernickety way; so the Grid constructor now loads an
 *			invalid grid, doesnt "fix" it, and doesnt complain; just does
 *			the best it can with what its given.
 *			Last top1465 02:37 with none of the big fish. Shell do.
 * 6.30.099 2020-11-30 07:01:39 Applied lessons from a month of performance
 *			tuning HoDoKu, especially caching in KrakenFisherman#kt2Search.
 *			Last top1465 51:24 with all the BIG FISH + A*Es.
 *			Last top1465 07:32 without the bloody mutants.
 * 6.30.100 2020-12-05 10:05:40 Polished KrakenFisherman. Added nonHinters
 *			filter to A5+E; which is what I SHOULD have done instead of all
 *			those months trying to work-out how to do it faster, so the
 *			ideal implementation now would be an A*E class implementing
 *			A5678910E and another simpler one for A234E. The key to this
 *			is the IntIntHashMap I wrote originally for HoDoKu.
 *			Last top1465 15:17 with A567E_1C, which is an improvement, I
 *			think, but do not have anything to compare it to (bad form).
 * 6.30.101 2020-12-08 07:58:41 Back-up prior to Aligned*Exclusion redo.
 *			This version has NonHinters64 and LongLongHashMap. Now I want
 *			to try iterating a stack in A*E (ala hobiwan) to remove all the
 *			the A*E classes, ie eliminate code-bloat.
 * 6.30.102 2020-12-17 10:54:42 Release align2.AlignedExclusion squashing
 *			all Aligned*Exclusions boiler-plate code down into one complex
 *			succinct class using hobiwans stack iteration technique twice:
 *			once for cells, and again for vals.
 *			Last top1465 12:43 with A234E A5E correct and A678910E hacked.
 * 6.30.103 2021-01-01 15:17:43 Release "wing2" package with WXYZWing,
 *			UVWXYZWing, and TUVWXYZWing boosted from Sukaku, by Nicolas
 *			Juillerat (the original authors rewrite of Sudoku Explainer)
 *			who boosted them from SudokuMonster.
 * 6.30.104 2021-01-06 19:06:44 Im releasing just to clean-up the logs.
 *			Ive implemented SueDeCoq in the als package, and also tried
 *			but failed at DeathBlossom.
 * 6.30.105 2021-01-12 10:05:45 Ship SueDeCoq. Failed at DeathBlossom.
 *			Last top1465 3:04 is OK.
 * 6.30.106 2021-01-14 09:02:46 Ship DeathBlossom.
 *			Last top1465 4:10 is a minute slower, but thats still OK coz
 *			I have no love for DeathBlossom, so I wont be using it.
 * 6.30.107 2021-01-15 09:44:47 Ship DeathBlossom.
 *			Last top1465 3:24 thats better, and a better test-case too.
 * 6.30.108 2021-01-23 14:40:48 Faster align2.AlignedExclusion, which still
 *			isnt competitive with the old align.Aligned*Exclusion classes
 *			but its getting there, and its just SOOOO much more succinct.
 * 6.30.109 2021-02-13 07:00:49 BigWings, faster Complex and Kraken fish.
 *			Wrote the BigWing class to succinctify the big wings. Tried and
 *			mostly failed to make Complex and KrakenFisherman faster.
 * 6.30.110 2021-02-17 14:31:50 Faster Complex and Kraken fisherman.
 * 6.30.111 2021-02-23 08:33:51 Oops! Last release used align2.
 * 6.30.112 2021-03-04 10:59:52 Hidden Unique Rectangles and XColoring.
 *			Last top1465 run took 04:36, which is over a minute slower,
 *			which is a worry, but still acceptable.
 * 6.30.113 2021-03-11 16:38:53 3D Madusa Coloring with cheese.
 *			Last top1465 run took 03:26 so were back on track.
 * 6.30.114 2021-03-22 12:04:54 GEM (Graded Equivalence Marks) Coloring.
 *			Last top1465 run 03:26, same as last time. Still back on track.
 * 6.30.115 2021-03-24 08:53:55 GEM mark 2 finds a few more hints looking
 *			for contradictions in ons.
 *			Last top1465 run 03:23, 3 seconds faster than last time.
 * 6.30.116 2021-03-25 13:28:55 GEM mark 3 with its own Multi hint.
 *			Last top1465 run 03:25, 2 seconds slower than last time.
 * 6.30.117 2021-03-27 14:02:57 GEM mark 4 sets subsequent singles to find
 *			130790 eliminations verses 77283 previously and 1.5 sec faster.
 * 6.30.118 2021-03-30 20:21:58 GEM mark 5 efficiency.
 * 6.30.119 2021-04-01 09:15:59 GEM mark 6 improved explanation.
 * 6.30.120 2021-04-04 06:11:00 GEM mark 7 more eliminations.
 * 6.30.121 2021-04-04 10:01:01 GEM mark 8 even more eliminations, chasing
 *			Naked/Hidden Singles from little hints as well. Really greedy!
 * 6.30.122 2021-04-05 07:17:02 GEM mark 9 fix bug in mark 8.
 * 6.30.123 2021-04-07 19:50:03 GEM mark 10 seriously greedy gemSolve.
 * 6.30.124 2021-04-11 09:26:04 GEM mark 11 uber greedy gemSolve using
 *			additional hinters. Last top1465 run took 03:15.
 * 6.30.125 2021-04-13 10:04:05 GEM mark 12 ultra greedy gemSolve using all
 *			hinters under 20ms/elim. Last top1465 run took 03:09.
 * 6.30.126 2021-04-17 06:09:06 GEM mark 13 Moved coloring up the hinters
 *			list and dropped slowies. Last top1465 run took 02:09.
 * 6.30.127 2021-04-24 08:18:07 GEM mark 14 is not greedy. sigh.
 * 6.30.128 2021-05-04 14:37:08 No real changes, just cleaning up crap.
 * 6.30.129 2021-05-13 10:25:09 Build for backup before I experiment with
 *			some GUI changes. Not a release.
 * 6.30.130 2021-05-17 10:19:10 GenerateDialog and TechSelectDialog are
 *			now more flexible. The description in the GenerateDialog is
 *			a generated list of Techs in this Difficulty range. The Tech
 *			difficulty (a base for hints of this type) is displayed in the
 *			TechSelectDialog. The user can look at TechSelect and Generate
 *			side-by-side to see which techs generate Difficulty requires,
 *			but unfortunately TechSelect is still modal. sigh.
 * 6.30.131 2021-05-19 08:07:11 Building for release because were going
 *			shopping today. Ive been pissing-about with BasicFisherman
 *			to make it faster than BasicFisherman, and failing. sigh.
 * 6.30.132 2021-05-25 09:24:12 Release coz were shopping today. Ive been
 *			updating comments, hint explanations, and documentation. Still
 *			no comprende why BigWing swaps x and z. Its nonsensical.
 * 6.30.133 2021-05-30 14:26:13 Build to clean-up logs and create backup.
 *			Ive just been pissin-about making SDP/ALS hinters faster.
 * 6.30.134 2021-06-01 17:56:14 Made all ALS hinters cache ALSs and RCCs,
 *			saving 20 seconds on top1465.
 * 6.30.135 2021-06-04 16:04:15 Speed-up AAlsHinter by introducing new
 *			AlsFinder and RccFinder classes.
 * 6.30.136 2021-06-05 11:19:16 Split RccFinder on forwardOnly.
 * 6.30.137 2021-06-09 15:28:17 Cleaning-up messy crap, and stuff I dont
 *			use any longer.
 * 6.30.138 2021-06-13 11:43:18 Still cleaning-up my own mess.
 * 6.30.139 2021-06-15 21:35:19 AlsXz speed especially RccFinder. Split
 *			getRccsForwardOnly into getRccsForwardOnlyAllowOverlaps and
 *			getRccsForwardOnlyNoOverlaps, and optimised to save ~3secs/run.
 * 6.30.140 2021-06-19 12:06:20 Removed Tech.nom.
 * 6.30.141 2021-06-22 16:02:21 LogicalSolver config hinter summaries in
 *			LogicalSolverTester log are now in wantedHinters order.
 *			Replace keySet+get with entrySet on all HashMaps. IMySet.visit.
 *			Split Frmu out of Frmt.
 * 6.30.142 2021-06-23 10:06:22 new Locking.pointFromElims method = 4 seconds.
 * 6.30.143 2021-06-25 13:55:23 cleaning-up: Tech.nom gone. AHint.apply takes
 *			grid. HashMaps entrySet. LogicalSolver clones CFG.
 *			IMySet.visit. Split Frmu from Frmt. Locking.pointFromElims.
 *			New Build class. Improved AboutDialog. Config.THE renamed.
 *			jar runs without files. Fixed A*E HitSet.
 * 6.30.144 2021-06-25 19:36:24 Fixed bugs in jar runs without files.
 * 6.30.145 2021-06-27 22:23:25 Cheats obfiscated. Less 4 quick fox instances.
 *			TooFewClues and TooFewValues ONCE per puzzle.
 * 6.30.146 2021-06-29 22:26:26 Cleaning-up Log and stdout, which are still a
 *			bloody nightmare. I should port it to Logging, but Im too lazy.
 *			Constantised common strings, but its no faster. sigh.
 * 6.30.147 2021-07-04 00:05:27 NOT Cleaned-up NakedSets Collections. maybes
 *			and maybesSize, with no Values instances. Sexy GRID_WATCH. Turbo
 *			mode. Fixed TreeSet comparator by adding toString.
 * 6.30.148 2021-07-09 06:25:28 Redid Grid.numSet and totalSize, change WWing,
 *			simplified cell.set, Cells and Regions test-cases.
 * 6.30.149 2021-07-09 17:45:29 Config written-to/read-from file instead of
 *			the Windows registry.
 * 6.30.150 2021-07-12 09:58:30 Generate -> analyseDifficulty now validates,
 *			and so rebuildMaybesAndS__t, solving all generates index issues.
 * 6.30.151 2021-07-12 21:56:31 SiameseLocking factored-out of Locking, which
 *			is now basically back to itself, and comprehensible.
 * 6.30.152 2021-07-13 23:00:32 indexes maintained on the fly.
 * 6.30.153 2021-07-17 08:29:33 make generate stoppable.
 * 6.30.154 2021-07-18 11:08:33 make generate stoppable, not 30 seconds slower.
 * 6.30.155 2021-07-18 17:56:35 make generate actually stop. sigh.
 * 6.30.156 2021-07-19 09:25:36 I wish I could stop stopping Generate.
 * 6.30.157 2021-07-20 08:51:37 jar-only-distribution rolls its own Config.
 * 6.30.158 2021-07-28 11:22:38 build before changing how AlsChain works-out
 *			rcc.which, to enspeedonate.
 * 6.30.159 2021-07-30 06:38:39 RccFinder completely decomposed.
 * 6.30.160 2021-08-11 07:10:40 build to clean-up old logs.
 * 6.30.161 2021-08-12 12:57:41 BasicFisherman now faster than BasicFisherman.
 * 6.30.162 2021-09-04 04:57:42 Hidden/NakedSetDirect, Tech.MULTI_CHAINERS,
 *			de-obfuscate Cheats, Grid.idxs not passed into ALS-hinters,
 *			LogicalSolverBuilder; build for back-up and to clean-up logs
 * 6.30.163 2021-09-06 11:23:43 Minor tweaks to align2.AlignedExclusion.
 *			Release coz Im shopping.
 * 6.30.164 2021-09-21 09:28:44 failed to speed-up AlsWing and AlsChain. sigh.
 *			I build just to clean-up the log-files.
 * 6.30.165 2021-09-29 was rooted so I deleted it.
 * 6.30.166 2021-10-07 09:00:46 new AlsChain1 intended to be a faster AlsChain,
 *			but its not right. Its faster, but relies on the Validator to
 *			suppress bad hints; so unusable. Build to clean-up old log-files.
 * 6.30.167 2021-10-10 16:35:47 AlsChain now takes about 4.5 times to find 1.5
 *			times as many hints. I also reordered the hinters but thats slower
 *			too. sigh. Im just not was worried about performance as I was coz
 *			its all now "basically fast enough", and any top1465 time under
 *			about three minutes feels "snappy enough" in the GUI (on an i7).
 * 6.30.168 2021-10-11 17:33:48 AlsChain faster, so now it takes 3 times to
 *			find 1.5 times as many hints, by caching the related RCCs by
 *			previousAlsIndex and previousCandToExclude.
 * 6.30.169 2021-10-13 10:18:49 AlsChain now filters its filter: da inline loop
 *			method (an O(n) operation) executes only when the new ALS (a) might
 *			be in the loop. Its a bit faster.
 * 6.30.170 2021-10-17 11:01:50 "nieve" AlsChain is about 10% slower dan "fast"
 *			but is maintainable; so LENGTH=6, TIMEOUT=500 elims 1776 in 03:06,
 *			and top1465 in 03:00 still feels "pretty snappy" in the GUI, so OK.
 * 6.30.171 2021-10-18 19:02:51 inlined everything in AlsChain again, for speed
 *			and its about 20% faster than the nieve version.
 * 6.30.172 2021-10-19 09:37:52 Still pissin about wit AlsChain. Dropped LENGTH
 *			to 6, reinstated length loop and moved timeout into i loop.
 * 6.30.173 2021-10-20 10:21:53 Updated AlsWing and AlsXz with learnings from
 *			AlsChain. Theyre both just a little faster, and cleaner, IMHO.
 * 6.30.174 2021-10-20 14:29:54 AlsChain.TIMEOUT from 125 to 16, which costs 23
 *			elims but saves 34 seconds.
 * 6.30.175 2021-10-21 09:30:55 Removed Idx.pots method coz mutating a param is
 *			poor form: mutate THIS, not that, ergo Pots.upsertAll. My bad. Soz.
 * 6.30.176 2021-10-22 18:11:56 Reduced AlsChain.TIMEOUT to just 8 ms. Tried
 *			making the RccFinder find only Rccs with one RC-value and an extra
 *			common value, finds ~60% in ~30% of time. @stretch AlsChain Hacked.
 * 6.30.177 2021-10-25 11:08:57 Still playing with AlsChain. I hit a problem
 *			with the non-determinism of the TIMEOUT approach so Ive ripped-out
 *			the growing length search and dropped LENGTH back to 4, but da code
 *			that does length-growing is still there but commented out. It finds
 *			less AlsChains, in about the same time, but is deterministic; which
 *			I now feel trumps all other concerns, even correctness. Id rather
 *			be consistently wrong than occassionaly right. So that worked out
 *			well. Sighing lols.
 * 6.30.178 2021-10-26 13:34:58 Separate Techs for ALS_Chain_4..ALS_Chain_7
 *			Each AlsChain now finds all 4..degree chains, so you select one of
 *			them at a time, coz its a bit faster. RCC_CACHE is back for speed.
 *			TechSelectDialog selects one ALS_Chain_* at a time.
 * 6.30.179 2021-10-27 17:56:59 AlsChain rccCache has its own IAS (a cache of
 *			int[]s) instead of creating them on the fly, so I got ALS_Chain_7
 *			into my three minute window, to keep the GUI "pretty snappy".
 * 6.30.180 2021-10-28 10:10:10 AlsChain has ALS_HAS_ANY_RELATED_RCCS and alsCeiling,
 *			so its a tad faster. The hunter, is still endlessly seeking.
 * 6.30.181 2021-10-30 14:52:01 AlsChain DUD_BRANCH pruned when branch known to
 *			not hint in [alsIndex][prevCand], cutting exponential work. Im a
 *			bit proud of this. BigWings bug: Grid.getBivalue didnt clear Idx.
 *			My bad! How embarassment! Head reshrunk.
 * 6.30.182 2021-11-01 16:27:02 KrakenFisherman is much faster. sigh.
 * 6.30.183 2021-11-02 20:25:03 eradicating Collections in favour of arrays,
 *			especially the bases and covers in the hints.
 * 6.30.184 2021-11-04 12:40:04 Remove IAS from AlsChain.
 * 6.30.185 2021-11-04 18:37:05 HACK: Reset DUD_BRANCH per startAls instead of
 *			per startAls/startCand (ie FAR less often) to find 10 less elims in
 *			about half of the time.
 * 6.30.186 2021-11-06 11:06:06 The final development build, so bug fixes only
 *			from here on. AlsChain is about 1.5 secs faster. Chainer cache now
 *			refreshes when a cell is set, to avert REALLY funny looking hints.
 *			Also dropped BUG in favour of Coloring/GEM which are better value.
 * 6.30.187 2021-11-12 09:33:07 Bug Fixes and things that annoy me, including:
 *			DirectNakedPair hang-over reds, Regions.common uses Grid.RIBS, no
 *			XColoring hint-details in batch, AlsChain is a bit faster, ALS
 *			rendering changed.
 * 6.30.188 2021-11-25 20:15:08 Fixing bugs and oversights in NakedSetDirect,
 *			Regions.common, XColoring, AlsChain, and WWing.
 * 6.30.189 2021-11-28 06:51:09 Fix two bugs in EmptyRectangle dat made it miss
 *			hints. Was 48elims/21ms now 588/85, so thats 12 times the elims in
 *			about 4 times the time. Better.
 * 6.30.190 2021-12-01 18:22:10 Fix ColoringHint. Check Coloring, XColoring,
 *			Medusa3D, and GEM. Check BigWing. Idx leases-out int-arrays.
 * 6.30.191 2021-12-03 10:05:11 Eradicated semithunked-code from Locking and
 *			LockingSpeedMode, making them easier to follow.
 * 6.30.192 2021-12-03 13:12:12 Removed Grid.Cell.hashCode using Grid.Cell.i
 *			(indice) instead coz its a bit shorter, and more significant bits
 *			further left is more deterministic, ie reduces hash-collisions.
 * 6.30.193 Got the chop!
 * 6.30.194 2021-12-04 08:00:14 Chainers use circular-arrays instead of Queues.
 *			This makes Chainers about 10% faster, but CAs are messy, so try
 *			this at home folks, not in a production system. Ill probably get
 *			the shits with CAs again, and revert them, again, in future. Sigh.
 *			ChainerUnary and ChainerMulti now have Ctrl-F6 test-cases.
 * 6.30.195 2021-12-06 10:55:15 Use circular-arrays instead of Queues for the
 *			local On and Off Queues in ChainerMulti.doChains.
 * 6.30.196 2021-12-08 06:12:16 Wee fixes to Coloring, XColoring and AlsChain.
 * 6.30.197 2021-12-16 04:52:17 Fix bugs, and bring UniqueRectangle upto date.
 * 6.30.198 2021-12-18 11:25:18 Endless Pissing About. BigWing speed and move
 *			and rename Idx visitor interfaces.
 * 6.30.199 2021-12-24 05:59:19 Reorder Techs to run faster hinters first which
 *			forces a rejig of Difficultys to reduce largest, so I went the
 *			whole hog and split Techs into 10 Difficulty, for nicer Generator.
 *			Also Generator hit bug on Locking.grid, so I removed the field.
 * 6.30.200 2021-12-27 07:11:20 faster BigWings. new IntArrays class. Chainers
 *			isX/YChain fields. Remove CASs from Cells.
 * 6.30.201 2021-12-27 12:21:21 Cells and IntArrays use MyArrayList instead of
 *			an Iterator.
 * 6.30.202 2021-12-28 11:33:22 Chainers need not check for empty parents list
 *			because new rule parents is null or populated; NEVER empty.
 * 6.30.203 2021-12-31 13:42:23 Faster DeathBlossom.
 * 6.30.204 2022-01-01 10:11:24 RccFinders idx caching. Promoted NakedQuad and
 *			HiddenQuad.
 * 6.30.205 2022-01-06 16:36:25 ChainerUnary seeks only XY-Cycles/Chains.
 *			Reverted MyLinkedHashMap.clear to use linked list on small maps.
 *			IDKFA is now the correct DynamicPlus+ only (not high DynamicChain).
 * 6.30.206 2022-01-13 15:25:26 tried to upgrade to Java 1.7 but VM is about
 *			30% slower than 1.8 for this rather odd application, so I decided
 *			against the upgrade. You can run it in later VMs but sometime
 *			between 1.8.0_271 and 17.0.1 performance went to pot. Also reduced
 *			AlsFinderPlain.DEGREE_CEILING from 8 to 7 to skip alss with 7 cells
 *			(STUVWXYZ_Wings) because they take more time than they are worth.
 * 6.30.207 2022-01-19 11:29:27 Reduced ALS_CEILING to 6 coz big ALS dont earn
 *			there salts. EmptyRectangle is also a bit faster. I build to backup
 *			pre split ridxs into int[] places (.bits) and numPlaces (.size).
 * 6.30.208 2022-01-22 11:19:28 NakedSet uses new knownNakedSets to avoid
 *			reprocessing the same nakedSet repeatedly when it still wont hint.
 *			Im only building to back-up the logs which are now too numerous.
 * 6.30.209 2023-03-26 11:13:29 Inlined code in WWing for speed, which is
 *			actually SLOWER. Sigh.
 * 6.30.210 2023-04-26 08:28:30 Various small ineffectual changes; actually I
 *			mostly just improved comments and comments. Changed welcome.html
 *			and improved explanations in some hints.
 * 6.30.211 2023-05-21 15:50:31 Superfisch is Fish AFAP; a replacement for my
 *			disaster of seperate multiple Complex and KrakenFisherman. My bad!
 *			Superfischerman is a "hidden feature". Hackers will discover it.
 * 6.30.212 Superfisch Configuration dialog unhides Superfisch. Its still a
 *			hackers toy, but normal people will now probably find it.
 * 6.30.213 2023-05-26 21:31 Permufisherman does Superfisch using Permutations.
 *			I hope its faster. Incorrect as yet. This is not a release build;
 *			I just need a backup. I will hone it and match race them later.
 * 6.30.214 2023-05-27 11:34 Permufisherm renamed SuperfischermanByPermutations
 *			and chop Kraken Type 1 from searchKraken. All KT1s are KT2s, hence
 *			seeking KT1s is a waste of time (so long as its both or neither).
 * 6.30.215 2023-05-28 16:35 SuperfischermanByPermutations is SuperfischerPerm
 *			coz it was too long to fit in existing batch log format. There is
 *			a new independant Superfisch instead of having basically the same
 *			class in Superfisherman and SuperfisherPerm. The Superfish Dialog
 *			now allows user to set maxFishSize for each fishType. Everything
 *			has been heavily performance tested. Still no HintCache. Sigh.
 * 6.30.217 2023-06-12 Upgraded Complex/KrakenFisherman to seek all sizes and
 *			types of fish upto the maximum specified by there nominal Tech;
 *			So  MutantJellyfish finds Basic/Franken/Mutant Swamp/Sword/Jelly.
 *			And KrakenJellyfish finds Basic/Franken/Mutant Swamp/Sword/Jelly.
 *			The Superfisch config determine there maxFins and maxEndofins,
 *			and there maxSizes/maxKrakenSizes. Also KrakenType1 now disabled.
 * 6.30.218 2023-06-16 Axe man! The old Aligned*Exclusion_(1C|2H) got chopped.
 *			The newer maintainable AlignedExclusion is now the align package.
 *			The old wing.BigWing class got chopped in favour of als.BigWings.
 *			I now plan to replace double difficulty with a simpler int.
 * 6.30.219 2023-06-17 Swap difficulty from double to int.
 * 6.30.220 2034-06-18 Clean-up bugs; esp ComplexFisherman usedBases. This one
 *			is a release candidate.
 * 6.30.222 2023-07-04 Only minor changes. Als.vBudsX Exploded Idx is notable.
 *			I build now before changing Idx 3 ints to array of 3 ints, so that
 *			all Idxs are semi-exploded. Creation will be slower, but I hope to
 *			pick-it-up on faster add/remove/query operations.
 * 6.30.223 2023-07-12 AlsFinderPlain fastardised. Dropped AlsChainFast because
 *			AlsChainGui was faster, and more capable. DeathBlossom fastardised.
 *			ChainerMulti fastardised: now has its own COMPLETE doChains.
 * 6.30.224 2023-07-14 Fastardised the chainers. doChains is now all one method
 *			with no onToOffs and offToOns methods for speed. Split ChainerMulti
 *			into ChainerMultiple, ChainerNishio, and ChainerDynamic rather than
 *			waste time on all those isMultiple, isDynamic, etc decisions.
 * 6.30.225 2023-07-20 Split Naked/HiddenSet into Pair/TripleQuad to read the
 *			permutation efficiently (one statement), which is only possible
 *			when you know at compile-time how large each permutation is.
 * 6.30.226 2023-07-26 I continued trying to speed things up. The AlsFinder is
 *			now just a class. The AAlsFinder and AlsFinderRecursive got cut.
 *			Reverted to standard VALUESES loops coz bit-twiddling was slower.
 * 6.30.227 2023-07-30 De-optimised RccFinders because the extra complexity was
 *			not worth 4 seconds. Anything under about 5 minutes feels OK in GUI
 *			which now has a hint caching switch.
 * 6.30.228 2023-08-05 Just Pissin about trying to make da slow hinters faster.
 *			This time I have been elbows deep in RccFinders ass. Its still not
 *			pregnant. Sigh. I modified AlsChain, making it slower. EPIC FAIL!
 * 6.30.229 2023-08-08 Dropped IALease. Reduced MAX_ALS_SIZE from 8 to 5, to
 *			make AlsChain faster.
 * 6.30.230 2023-08-10 Delay set creation in chainers until first findChains,
 *			and replace IAfter with ICleanUp (same s__t).
 * 6.30.231 2023-08-14 Chainers have changed, for better or for worse.
 * 6.30.232 2023-08-17 RccFinderAllRecycle parses RccFinderForwardOnly results
 *			into RccFinderAll results, ie A-&gt;B into A-&gt;B and B-&gt;B.
 * 6.30.233 2023-08-25 DeathBlossom split search method out of recurse. I build
 *			only because its overdue.
 * 6.30.234 2023-08-26 Fixed the Swing NPE bug by making JTree fixed-height.
 *			Also I just had a lightning fast batch in profiler, so I want to
 *			retain this version of everything before it changes.
 * 6.30.235 2023-09-04 UnaryChainer uses AssAnc extends Ass, to keep ancestors
 *			out of the other chainers, because they dont use hasAncestor.
 * 6.30.236 2023-09-05 Removed Ass.cell and Pots key is now Integer indice.
 *			SudokuGridPanel is now indice-based. All get${Color}Cells methods
 *			return {@code Set<Integer>}.
 * 6.30.237 2023-09-10 All hints take IHinter, instead of AHinter.
 * 6.30.238 2023-09-11 Just made stuff faster.
 * 6.30.239 2023-09-17 Made LinkedMatrixAssSet simpler but slower. This is not
 *			a release build, merely a staging point.
 * 6.30.240 2023-09-19 Solved the slower problem: solve countSolutions. Oops!
 * 6.30.241 2023-09-20 Renovated test-cases.
 * 6.30.242 2023-09-21 findHints(Grid, IAccumulator) sets standard grid fields
 *			and calls findHints() that is overwritten by slower hinters. Fast
 *			hinters still override findHints(Grid, IAccumulator) for speed.
 * 6.30.243 2023-09-25 Enspeedonating hinters. Fixed a "reset bug" in Frutant
 *			Fisherman, which is about a second faster as a result.
 * 6.30.244 2023-09-29 I build because I should do so every few days, when I am
 *			working on SE.
 * 6.30.245 2023-10-07 I build because I should. I've been making stuff faster.
 *			Split FrutantFisherman into FrankenFisherman and MutantFisherman.
 * 6.30.246 2023-10-12 Made complex Fisherman and Chainers faster.
 * 6.30.247 2023-10-13 Made AlsWing faster using the "All" RCCs plus starts and
 *			ends, as per AlsChain. This version is ONLY suitable ONLY for use
 *			with AlsChain, else getting the "All" RCCs is a waste of time. With
 *			AlsChain it matters not when we do the upgrade, so its faster.
 * 6.30.248 2023-10-16 Idx masks are now a long (54bit) and b int (27bit).
 * 6.30.249 2023-10-18 Marginally faster complex fisherman. I have an idea.
 * 6.30.250 2023-10-19 Coloring and Chainers (RAM hoggers) now release there
 *			sets/buffers when not in use. This is actually slower but reduces
 *			RAM usage, so the GC no longer goes into overdrive.
 * 6.30.251 lost coz I reverted back past it
 * 6.30.252 lost coz I reverted back past it
 * 6.30.253 2023-10-21 Imbedded Chainers now initialise and deinitialise with
 *			there parent ChainerDynamic, not individually, for speed. FYI,
 *			251 and 252 lost coz I reverted back to 250 and recoded changes.
 * 6.30.254 2023-10-25 I have freed more heapspace, and made many hinters a bit
 *			faster with post-tested-loops where possible. That and other
 *			improvements to XColoring and GEM make them ~10% faster.
 * 6.30.255 2023-10-27 Finished eliminating hang-over large sets and buffers
 *			from all hinters, to reduce heap usage by limiting the amount held
 *			by currently-not-in-use hinters.
 * 6.30.256 2023-10-28 Split unpredicated KrakenFisherman into KrackFisherman,
 *			leaving predicated in KrakenFisherman. Also HACK changeCands finds
 *			~60% of elims in ~25% of time. She'll do.
 * 6.30.258 2023-11-05 New tables.TableAbduction is reduction, doing effects
 *			itself, where TableReduction relies on pre-calculated elims. This
 *			approach finds many more elims, but is much slower overall.
 * 6.30.259 2023-11-07 table.Contradictions gets its contradictions from
 *			{@link diuf.sudoku.solver.hinters.table.ATableChainer.Tables#doElims() },
 *			checks they are still fresh and turns them into hints.
 * 6.30.260 2023-11-08 CellReductionHint highlights/links multiple elims.
 *			CachingHintsAccumulator returns "did it add" making Contradiction
 *			and Reduction faster.
 * 6.30.261 2023-11-12 faster Idx iteration with IntQueue. Less method calls.
 *			The code is also MUCH simpler. The old way was a cluster____. Also
 *			deleted all of the visit/each methods. The only one I really miss
 *			is whileTrue, so it will reappear next release, probably.
 * 6.30.262 2023-11-13 faster Idx iteration: inline numberOfTrailingZeros; by
 *			Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
 *			Faster Grid.toString. Faster LogicalSlvrTester.printPuzzleSummary.
 * 6.30.263 2023-11-18 TableChains replaces Contradiction and Reduction.
 * 6.30.264 2023-11-23 GEM is about 20% faster thanks to IdxPlusBuds now caches
 *			an array of indices to iterate, instead of repeatedly iterating an
 *			unchanged Idx using MyIntQueue, which is inefficient.
 * 6.30.265 2023-11-28 Coloring, XColoring, and GEM use new IdxC, an Idx that
 *			caches it's indices array and plusBuds.
 * 6.30.266 2023-12-05 Idx masks renamed. GEM moves offCounts and only to Idx.
 *			toStringImpl returns StringBuilder, as do several contributors.
 *			SB(capacity) method for powerOf2 StringBuilders everywhere.
 *			Hinters set/clearFields to reduce fields being set/cleared.
 * </pre>
 *
 * @author Keith Corlett 2013 - 2023
 */
public final class Build {

	/** The TITLE of this application. */
	public static final String TITLE = "DiufSudoku";

	/**
	 * The VERSION.SUB.BUILD number of this build.
	 * <pre>
	 * VERSION changes when default input/output file format changes;
	 * SUB should change when "other dependencies" change (I am bad);
	 * BUILD increments every other time.
	 * </pre>
	 */
	public static final String VERSION = "6.30.266";

	/** The date and time I BUILT this build. */
	public static final String BUILT = "2023-12-05 08:34";

	/** ApplicationTitleandVersion (for presentation). */
	public static final String ATV = TITLE + " " + VERSION;

	private Build() { } // never called

}
