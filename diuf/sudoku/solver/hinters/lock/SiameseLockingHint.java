/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
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
public class SiameseLockingHint extends AHint {

	final String hintTypeName;
	final int maybesToRemove;
	final Set<Cell> cellSet;
	final Idx idx = new Idx();
	final Pots highPots = new Pots();
	final ARegion base;
	final ARegion cover;
	final String andedMaybesToRemove;

	private String toStr;

	/**
	 * Constructs a new SiameseLockingHint.
	 * @param hinter the hinter which produced this hint.
	 * @param hints the LockingHint[] which have been found to be Siamese.
	 * @param isPointing is this a Pointing hint, or a Claiming hint.
	 */
	public SiameseLockingHint(AHinter hinter, LockingHint[] hints, boolean isPointing) {
		super(hinter, new Pots());
		this.hintTypeName = isPointing ? "Siamese Pointing" : "Siamese Claiming";
		int maybes = 0;
		cellSet = new HashSet<>();
		for ( LockingHint pfh : hints ) {
			maybes |= Values.SHFT[pfh.valueToRemove];
			cellSet.addAll(pfh.cellSet);
			idx.or(pfh.idx());
			highPots.upsertAll(pfh.greenPots);
			redPots.upsertAll(pfh.redPots);
		}
		andedMaybesToRemove = Values.and(maybesToRemove = maybes);
		base = hints[0].base;
		cover = hints[0].cover;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return cellSet;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return highPots;
	}

	@Override
	public Pots getReds(int viewNum) {
		return redPots;
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
	public String getHintTypeNameImpl() {
		return hintTypeName;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a " + hintTypeName
			+(isBig ? " on <b>"+andedMaybesToRemove+"</b>" : "");
	}

	@Override
	public String toStringImpl() {
		if ( toStr != null )
			return toStr;
		try {
			StringBuilder sb = Frmt.getSB();
			sb.append(hintTypeName).append(": ")
			  .append(base).append(" and ").append(cover)
			  .append(" on ").append(andedMaybesToRemove);
			toStr = sb.toString();
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			// nb toStr is (probably) still null
		}
		return toStr;
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "LockingHint.html"
				, hintTypeName			// {0}
				, andedMaybesToRemove	//  1
				, base.typeName			//  2
				, cover.typeName		//  3
		);
	}
}
