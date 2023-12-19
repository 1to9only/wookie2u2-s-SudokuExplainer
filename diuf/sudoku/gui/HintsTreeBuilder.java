/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.solver.AHint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// note: use spaces in the pre block, not tabs!
/**
 * The HintsTreeBuilder class is a single-use builder of a hints tree:
 * <pre>{@code
 * + Hints (2 hints)                    <= root
 *   + Direct Hints (1 hint)            <= topNode: direct, warning, indirect
 *     + Hidden Single (1 hint)         <= hinterNode
 *       + Hidden Single: H9=3 in box 9 <= hintNode (leaf)
 *   + Indirect Hints (1 hint)
 *     + Point and Claim (1 hint)
 *       + Claiming: I1, I2 on 3 in col I and box 3
 * }</pre>
 * There are 3 maps from the 3 topNodes to whatever hinterNodes appear in the
 * given hints List. An empty map is not displayed.
 */
final class HintsTreeBuilder {

	private final HintNode root = new HintNode("Hints");

	private HintNode getDirectNode() {
		if ( directNode == null )
			root.insert(directNode=new HintNode("Direct Hints"), 0);
		return directNode;
	}
	private HintNode directNode = null;

	private HintNode getWarningNode() {
		if ( warningNode == null )
			root.add(warningNode=new HintNode("Warnings"));
		return warningNode;
	}
	private HintNode warningNode = null;

	private HintNode getIndirectNode() {
		if ( indirectNode == null )
			root.add(indirectNode=new HintNode("Indirect Hints"));
		return indirectNode;
	}
	private HintNode indirectNode = null;

	private HintNode getTopNode(final int hintType) {
		switch ( hintType ) {
		case AHint.DIRECT: return getDirectNode();
		case AHint.WARNING: return getWarningNode();
		default: return getIndirectNode();
		}
	}

	/**
	 * build a Tree of the hints
	 * @param hints are pre-found, pre-filtered and pre-sorted.
	 * @return
	 */
	HintNode build(final List<AHint> hints) {
		// three (Direct|Warning|Inderect) Maps of hinterName => hinterNode
		@SuppressWarnings({"unchecked", "rawtypes"})
		final Map<String,HintNode>[] maps = new Map[] {
			new HashMap<>(), new HashMap<>(), new HashMap<>()
		};
		// foreach hint: add new HintNode to root/theTopNode/hinterNode/
		for ( AHint h : hints ) {
			// choose the appropriate Map for this type of hint
			final int hType;
			if ( h.type > 2 )
				hType = 2;
			else
				hType = h.type;
			final Map<String,HintNode> map = maps[hType];
			// get hinter name (not hintTypeName which defaults to hinterName)
			final String hName;
			if ( h.hinter == null )
				// we get away with it coz its the only hint without a hinter
				hName = h.getClass().getSimpleName().replaceAll("Hint$", "");
			else
				hName = h.hinter.toString(); // returns tech.name()
			// get the existing hinterNode, or create a new one
			HintNode n = map.get(hName);
			if ( n == null ) {
				n = new HintNode(hName);
				final HintNode t = getTopNode(h.type);
				t.add(n);
				map.put(hName, n);
			}
			// add the hint to the hinterNode
			n.add(new HintNode(h));
		}
		if ( root != null )
			root.appendHintsCountToName();
		if ( directNode != null )
			directNode.appendHintsCountToName();
		if ( indirectNode != null )
			indirectNode.appendHintsCountToName();
		for ( HintNode n : maps[AHint.DIRECT].values() )
			n.appendHintsCountToName();
		for ( HintNode n : maps[AHint.INDIRECT].values() )
			n.appendHintsCountToName();
		return root;
	}

}
