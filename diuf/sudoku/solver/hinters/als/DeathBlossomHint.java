// KRC 2021-01-06 DeathBlossom is broken so commented out.
// KRC 2021-01-07 DeathBlossom still broken but I'm still swinging.
// KRC 2021-01-08 DeathBlossom is a first class pain in the ass.
// KRC 2021-01-09 DeathBlossom still incomprehensibly rooted!
///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2020 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku.solver.hinters.als;
//
//import diuf.sudoku.Grid;
//import diuf.sudoku.Grid.ARegion;
//import diuf.sudoku.Grid.Cell;
//import diuf.sudoku.Pots;
//import diuf.sudoku.solver.AHint;
//import diuf.sudoku.solver.hinters.AHinter;
//import diuf.sudoku.solver.hinters.HintValidator;
//import diuf.sudoku.utils.Html;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Set;
//
///**
// * The DTO (Data Transfer Object) for a Death Blossom Hint.
// *
// * @author Keith Corlett 2021-01-06
// */
//public class DeathBlossomHint extends AHint {
//
//	private final Pots greens; // the stem cell
//	private final Pots blues; // the value used in each ALS
//	private final Set<Cell> pinkCells; // cells in the overlap between ALS's
//	private final String alses; // a string of the ALS's
//	private final List<ARegion> regions; // the region containing each ALS
//	// holding the Grid in a field in a hint is a HACK to use HintValidator.
//	// @todo remove field when HintValidator no longer required.
//	private final Grid grid;
//	public DeathBlossomHint(AHinter hinter, Pots reds, Pots greens, Pots blues
//			, Set<Cell> pinkCells, String alses, List<ARegion> regions
//			, Grid grid) {
//		super(hinter, reds);
//		this.greens = greens;
//		this.blues = blues;
//		this.pinkCells = pinkCells;
//		this.alses = alses;
//		this.regions = regions;
//		this.grid = grid;
//	}
//
//	/**
//	 * Get the cells to be highlighted with an aqua background.
//	 * <p>
//	 * In DeathBlossomHint's case, that's the stem cell.
//	 * @param viewNum 1..128
//	 * @return A Set of aqua (highlighted) Cells.
//	 */
//	@Override
//	public Set<Cell> getAquaCells(int viewNum) {
//		return greens.keySet();
//	}
//
//	/**
//	 * Get the cells to be highlighted with a pink background.
//	 * <p>
//	 * In DeathBlossomHint's case, that's the cells in overlap.
//	 * @param viewNum 1..128
//	 * @return A Set of the pink (highlighted) Cells.
//	 */
//	@Override
//	public Set<Cell> getPinkCells(int viewNum) {
//		return pinkCells; // cells in the overlap between ALS's
//	}
//
//	/**
//	 * Reliably getting the solution values from the HintValidator requires the
//	 * bloody Grid.
//	 * @stretch fix invalid hints, so !HintValidator.DEATH_BLOSSOM_USES.
//	 * @return
//	 */
//	private int[] getSolutionValues() {
//		if ( HintValidator.DEATH_BLOSSOM_USES ) {
//			if ( grid != null )
//				return HintValidator.getSolutionValues(grid);
//			return HintValidator.solutionValues; // may be null
//		}
//		return null;
//	}
//
//	/**
//	 * Invalid eliminations are purple, not red.
//	 * <p>
//	 * Given 1+ invalid redPots: to paint purple we remove from getReds (view),
//	 * but leave in redPots (data); otherwise the values redness overwrites its
//	 * purpleness. Else the hint !isInvalid (ie is valid) just return redPots.
//	 * <p>
//	 * This is not a critical process. If the grid passed to my constructor is
//	 * null, and the HintValidator doesn't already have solutionValues then
//	 * I abandon the search for invalid eliminations; so they stay red!
//	 *
//	 * @stretch fix invalid hints, so !HintValidator.DEATH_BLOSSOM_USES.
//	 *
//	 * @param viewNum
//	 * @return
//	 */
//	@Override
//	public Pots getReds(int viewNum) {
//		if ( HintValidator.DEATH_BLOSSOM_USES ) {
//			if ( isInvalid ) {
//				// go through this ass-mangle ONCE!
//				if ( validReds == null ) {
//					if ( redPots == null )
//						return redPots; // Never happens. Never say never.
//					// remove invalid eliminations from the result.
//					// get solutionValues (correct value for each cell)
//					int[] solutionValues = getSolutionValues();
//					if ( solutionValues == null )
//						return redPots; // should never happen
//					// start with an independent mutable copy of redPots
//					Pots reds = Pots.deepCopy(redPots);
//					// use an interator to allow for deletion of cells
//					for ( Iterator<Cell> it=reds.keySet().iterator(); it.hasNext(); ) {
//						Cell c = it.next();
//						// remove has no effect if given value does not exist
//						// remove returns the new size
//						if ( reds.get(c).remove(solutionValues[c.i]) == 0 )
//							// no values left in cell, so remove this cell
//							it.remove();
//					}
//					// contract for Pots says nullable but never empty
//					if ( reds.isEmpty() )
//						reds = null;
//					validReds = reds; // may be null
//				}
//				return validReds; // may be null
//			}
//		}
//		return redPots; // nullable, but never in DeathBlossom
//	}
//	private Pots validReds;
//
//	@Override
//	public Pots getGreens(int viewNum) {
//		return greens;
//	}
//
//	@Override
//	public Pots getBlues(Grid grid, int viewNum) {
//		return blues;
//	}
//
//	/**
//	 * Invalid eliminations are purple, not red.
//	 * <p>
//	 * This not critical process: If goes bad then elims stay red.
//	 *
//	 * @stretch fix invalid hints, so !HintValidator.DEATH_BLOSSOM_USES.
//	 *
//	 * @return
//	 */
//	@Override
//	public Pots getPurples() {
//		if ( HintValidator.DEATH_BLOSSOM_USES ) {
//			if ( isInvalid ) {
//				try {
//					// getReds removes invalid elims from redPots, so now all I
//					// need to do is remove getReds from redPots to leave the
//					// invalid eliminations; a double negative is a positive.
//					return new Pots(redPots).removeAll(getReds(0));
//				} catch (Throwable eaten) {
//					// Do nothing
//				}
//			}
//		}
//		return null;
//	}
//
//	@Override
//	public List<ARegion> getBases() {
//		return Grid.regionList(regions.get(0));
//	}
//
//	@Override
//	public List<ARegion> getCovers() {
//		return Grid.regionList(regions.get(1));
//	}
//
//	@Override
//	protected String toHtmlImpl() {
//		return Html.produce(this, "DeathBlossomHint.html"
//				, redPots.toString()
//				, greens.toString()
//				, blues.toString()
//				, alses
//		);
//	}
//
//	@Override
//	protected String toStringImpl() {
//		return (isInvalid?"@":"")+getHintTypeName()+": "+greens;
//	}
//
//	@Override
//	public String toFullString() {
//		if ( fs == null ) {
//			Cell stemCell = greens.keySet().iterator().next();
//			boolean stemInOverlap = pinkCells.contains(stemCell);
//			fs = super.toFullString() + ( stemInOverlap ? "!" : "" );
//		}
//		return fs;
//	}
//	private String fs;
//
//
//}
