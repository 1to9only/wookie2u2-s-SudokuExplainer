/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * ABigWingWingHint is an abstract data transfer object (DTO) which implements
 * methods common to all of the "big wing" hint types: WXYZWingHint,
 * VWXYZWingHint, UVWXYZWingHint, and TUVWXYZWingHint.
 *
 * @author Keith Corlett 2021-01-10 extracted similarities
 */
public abstract class ABigWingHint extends AHint implements IChildHint {

	protected final int[] wingValues;
	protected final int x;
	protected final int z;
	protected final boolean both;
	protected final Cell yz;
	protected final Cell[] wing;
	protected final Cell[] all;
	public ABigWingHint(AHinter hinter, Pots redPots, int wingCands, Cell yz
			, XZ yzVs, Cell[] wing, Cell[] all) {
		super(hinter, redPots);
		this.x = yzVs.x;
		this.z = yzVs.z;
		this.both = yzVs.both;
		this.yz = yz;
		this.wing = wing;
		this.all = all;
		this.wingValues = VALUESES[wingCands ^ yz.maybes.bits];
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return Grid.cellSet(wing);
	}

	@Override
	public Set<Cell> getPinkCells(int viewNum) {
		return Grid.cellSet(yz);
	}

    @Override
    public Pots getGreens(int viewNum) {
        Pots result = new Pots();
		if ( both ) // all green
			for ( Cell c : all )
				result.put(c, new Values(c.maybes));
		else // z green
			for ( Cell c : all )
				if( c.maybe(z) )
					result.put(c, new Values(z));
		return result;
    }

	protected int getWingValue(int i) {
		if ( i < wingValues.length )
			return wingValues[i];
		return 0;
	}

	@Override
	public double getDifficulty() {
		return hinter.tech.difficulty + Math.abs(bigCard() - 3) * 0.01;
    }

	private void addLinks(Collection<Link> result, int v) {
		for ( Cell c : wing )
			if ( c.maybe(v) && c!=yz )
				result.add(new Link(yz, v, c, v));
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			final int n = both ? wing.length*2 : wing.length;
			Collection<Link> result = new ArrayList<>(n);
			addLinks(result, x);
			if ( both )
				addLinks(result, z);
			return result;
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("ABigWingHint.getLinks: "+ ex);
			return null;
		}
	}

    public String getSuffix() {
		return both ? "2" : "1";
	}

	@Override
	public String getHintTypeNameImpl() {
        return hinter.tech.nom + getSuffix();
    }

	protected String wingValues() {
		return Frmt.csv(wingValues);
	}

	@Override
    public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeNameImpl();
        if ( isBig )
			s += " on the values "+wingValues()+" and <b>"+z+"</b>";
		return s;
    }

    @Override
    public String toStringImpl() {
		String s = getHintTypeName()+": "+Frmt.ssv(all)+" on value";
 		if ( both )
			s += "s " + x + "," + z;
		else
			s += " "+z;
		return s;
    }

	protected int bigCard() {
		int max = 0;
		for ( Cell c : wing )
			max = Math.max(c.maybes.size, max);
		return max;
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid, IAssSet parentOffs) {
		MyLinkedList<Ass> result = new MyLinkedList<>();
		// foreach value which is eliminated anywhere by this hint
		for ( int v : Values.VALUESES[redPots.valuesOf()] )
			for ( Cell c : all )
				if ( initGrid.cells[c.i].maybe(v)
				  && !currGrid.cells[c.i].maybe(v) )
					result.add(parentOffs.getAss(c, v));
		return result;
	}

	protected int wingSize() {
		int cnt = 0;
		for ( Cell c : wing )
			cnt += c.maybes.size;
		return cnt;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ABigWingHint && equals((ABigWingHint) o);
	}

	public boolean equals(ABigWingHint o) {
		return Arrays.equals(all, o.all);
	}

	@Override
	public int hashCode() {
		// use the Cell.hashCode field instead of calling hashCode()
		int hc = 0;
		for ( Cell c : all )
			hc ^= c.hashCode;
		return hc;
	}

}
