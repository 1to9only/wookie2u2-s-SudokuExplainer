/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.PuzzleID;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.Log;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


/**
 * A self-persistent List of recently accessed (opened or saved) files.
 * @author Keith Corlett 2017 Dec
 */
class RecentFiles implements Closeable {

	// The maximum number of things to remember - how recent is recent?<br>
	// I blew this out to from 100 to 1000, coz it really doesn't matter, it
	// just makes the scrollbar a bit useless, that's all. And that's mainly
	// because large ScrollBars are bit ____in useless. Sigh.
	private static final int MAX_SIZE = 1000;

	private final ArrayList<PuzzleID> list = new ArrayList<>(MAX_SIZE);
	private final PuzzleID[] array = new PuzzleID[MAX_SIZE];
	PuzzleID[] toArray() {
		return list.toArray(array);
	}

	private static RecentFiles me;
	public static RecentFiles getInstance() {
		if ( me == null )
			me = new RecentFiles();
		return me;
	}

	/** PRIVATE Constructor: use getInstance() instead. */
	private RecentFiles() {
		if ( IO.RECENT_FILES.exists() ) // avert FileNotFoundException
			try ( BufferedReader reader = new BufferedReader(new FileReader(IO.RECENT_FILES)) ) {
				int lineCount = 0;
				String line;
				while ( (line=reader.readLine()) != null ) {
					list.add(PuzzleID.parse(line));
					if ( ++lineCount >= MAX_SIZE )
						break;
				}
			} catch (IOException ex) {
				StdErr.whinge(Log.me()+" exception", ex);
//				throw new RuntimeException("Unreadable "+FILE, ex);
			}
	}

	/** @return the PuzzleID of the most recently accessed file. */
	PuzzleID mostRecent() {
		if ( list.isEmpty() )
			return null;
		return list.get(0);
	}

	/** Make this PuzzleID the most recently accessed file, returning the given
	 * puzzleID so that you can chain with me. Note: If the given pid is null or
	 * empty then returns EMPTY_PUZZLE_ID; not null, as you might expect. */
	PuzzleID add(PuzzleID pid) {
		if ( pid==null || pid.file==null )
			return EMPTY_PUZZLE_ID;
		int i = list.indexOf(pid);
		if ( i == 0 )
			return pid;
		if ( i > 0 ) { // move file to head of list
			list.remove(pid);
		} else { // inserting, so drop the least recently used file
			while ( list.size() >= MAX_SIZE )
				list.remove(MAX_SIZE-1);
		}
		// then insert file at top of the list
		list.add(0, pid);
		return pid;
	}
	private static final PuzzleID EMPTY_PUZZLE_ID = new PuzzleID(null, 0);

	/**
	 * Saves the RecentFiles.list to IO.RECENT_FILES.
	 * @throws any IOException
	 */
	@Override
	public void close() throws java.io.IOException {
		try ( PrintWriter writer = new PrintWriter(IO.RECENT_FILES) ) {
			for ( PuzzleID entry : list )
				if ( entry != null )
					writer.println(entry);
		}
	}
}
