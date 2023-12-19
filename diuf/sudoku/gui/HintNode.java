/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.solver.AHint;
import java.util.LinkedList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A tree node representing a hint in the hints tree
 * of the user interface.
 */
final class HintNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 7857073221166387482L;

	private final AHint hint;
	private String name;

	HintNode(final AHint hint) {
		super();
		this.hint = hint;
//DEBUG to see elims, but beware some do not override toFullString
//		this.name = hint.toFullString();
		this.name = hint.toString();
		if ( hint.hinter!=null && hint.hinter.isNesting() )
			name += " (" + hint.getViewCount() + ")";
	}

	HintNode(final String name) {
		super();
		this.hint = null;
		this.name = name;
	}

	AHint getHint() {
		return this.hint;
	}

	String getName() {
		return this.name;
	}

	boolean hasHint() {
		return this.hint != null;
	}

	@Override
	public boolean getAllowsChildren() {
		return this.hint == null;
	}

	HintNode getNodeFor(final AHint hint) {
		if(hint == null) return null;
		if(hint.equals(this.hint)) return this;
		HintNode node;
		for ( int i=0,n=getChildCount(); i<n; ++i )
			if ( (node=((HintNode)getChildAt(i)).getNodeFor(hint)) != null )
				return node;
		return null;
	}

	List<HintNode> getHintNodes() {
		if ( hns != null )
			return hns; // cached!
		hns = new LinkedList<>();
		if ( this.hint != null )
			hns.add(this);
		else
			recurseChildren(hns);
		return hns;
	}
	private List<HintNode> hns; // hintNodes

	private void recurseChildren(final List<HintNode> result) {
		assert this.hint == null; // ie: this is NOT a leaf
		HintNode child;
		for ( int i=0,n=getChildCount(); i<n; ++i )
			if ( (child=(HintNode)getChildAt(i)).hint != null )
				result.add(child);
			else
				child.recurseChildren(result);
	}

	private int countHints() {
		if ( this.hint != null )
			return 1;
		return getHintNodes().size();
	}

	void appendHintsCountToName() {
		final int count = countHints();
		if ( count == 1 )
			this.name += " (" + count + " hint)";
		else
			this.name += " (" + count + " hints)";
	}

	@Override
	public String toString() {
		return this.name;
	}

}
