/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Siamese Locking Hint is the merger of two or three Pointing or Claiming
 * Hints from one region all into one hint, so that they're displayed as one in
 * the GUI. I'm not used in LogicalSolverTester where display is irrelevant;
 * nor am I used in the RecursiveAnalyser where speed is King (finding me is
 * slow).
 *
 * @author Keith Corlett 2020 June 18
 */
public class SiameseLockingHint extends AHint implements IActualHint {

	final boolean isPointing;
	final int maybesToRemove;
	final Set<Cell> cellSet;
	final Idx idx = new Idx();
	final Pots greenPots = new Pots();
	final ARegion base;
	final ARegion cover;
	final String valuesToRemove;

	private String toStr;

	/**
	 * Constructs a new SiameseLockingHint.
	 * @param hinter the hinter which produced this hint.
	 * @param hints the LockingHint[] which have been found to be Siamese.
	 * @param isPointing is this a Pointing hint, or a Claiming hint.
	 */
	public SiameseLockingHint(AHinter hinter, LockingHint[] hints, boolean isPointing) {
		super(hinter, new Pots());
		this.isPointing = isPointing;
		int maybes = 0;
		cellSet = new HashSet<>();
		for ( LockingHint pfh : hints ) {
			maybes |= VSHFT[pfh.valueToRemove];
			cellSet.addAll(pfh.cellSet);
			idx.or(pfh.idx());
			greenPots.upsertAll(pfh.greens);
			redPots.upsertAll(pfh.redPots);
		}
		valuesToRemove = Values.andS(maybesToRemove = maybes);
		base = hints[0].base;
		cover = hints[0].cover;
	}

	// Weird: Locking is only place we use one Tech for two hint-types.
	@Override
	public double getDifficulty() {
		double d = super.getDifficulty();
		if ( !isPointing ) // Claiming
			d += 0.1;
		return d;
	}

	@Override
	public String getHintTypeNameImpl() {
		if ( isPointing )
			return "Siamese Pointing";
		return "Siamese Claiming";
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return cellSet;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return greenPots;
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(base);
	}

	@Override
	public List<ARegion> getCovers() {
		return Regions.list(cover);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on <b>"+valuesToRemove+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		if ( toStr != null )
			return toStr;
		try {
			StringBuilder sb = Frmt.getSB();
			sb.append(getHintTypeName()).append(": ")
			  .append(base).append(" and ").append(cover)
			  .append(" on ").append(valuesToRemove);
			toStr = sb.toString();
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			// nb toStr is (probably) still null
		}
		return toStr;
	}

	@Override
	public String toHtmlImpl() {
		// WARN: LockingHint.html is also used in LockingHint,
		// so any arguements changes here must also happen there.
		return Html.produce(this, "LockingHint.html"
				, getHintTypeName()		// {0}
				, valuesToRemove		//  1
				, base.typeName			//  2
				, cover.typeName		//  3
				, redPots.toString()	//  4
				// {5} debugMessage hijacked to explain the Siamese concept.
				, "<p>"+NL+"<u>Explanation</u>"+NL+"<p>"+NL
				  +"Note that \"Siamese\" hints are an agglomeration of two distinct hints."+NL
				  +"It's just nice to see them as one, and so it is, that's all."+NL
		);
	}
}
