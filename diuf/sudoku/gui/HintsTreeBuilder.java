/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.solver.AHint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A single-use builder of the hints tree:
 * {@code 
 * + Hints (2 hints)					<= root
 *   + Direct Hints (1 hint)			<= topNode: direct, warning, indirect
 *     + Hidden Single (1 hint)			<= hinterNode
 *       + Hidden Single: H9=3 in box 9	<= hintNode (leaf)
 *   + Indirect Hints (1 hint)
 *     + Point and Claim (1 hint)
 *       + Claiming: I1, I2 on 3 in col I and box 3
 * }
 * The three maps are from the three topNodes to whatever hinterNodes appear
 * in the given hints List.
 */
public final class HintsTreeBuilder {

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

	private HintNode getTopNode(int hintType) {
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
	public HintNode build(List<AHint> hints) {
		// three (Direct|Warning|Inderect) Maps of hinterName => hinterNode
		@SuppressWarnings({"unchecked", "rawtypes"})
		Map<String,HintNode>[] maps = new Map[] {
			new HashMap<>(), new HashMap<>(), new HashMap<>()
		};
		// foreach hint: add new HintNode to root/theTopNode/hinterNode/
		for ( AHint hint : hints ) {
			// choose the appropriate Map for this type of hint
			final Map<String,HintNode> theMap = maps[Math.min(hint.type,2)];
			// get hinter name, not hintTypeName which defaults to hinterName.
			final String hinterName = hint.hinter.toString();
			HintNode hinterNode = theMap.get(hinterName);
			if ( hinterNode == null ) { // create a new hinterNode
				hinterNode = new HintNode(hinterName);
				final HintNode theTopNode = getTopNode(hint.type);
				theTopNode.add(hinterNode);
				theMap.put(hinterName, hinterNode);
			}
			hinterNode.add(new HintNode(hint));
		}
		if ( root != null )
			root.appendHintsCountToName();
		if ( directNode != null )
			directNode.appendHintsCountToName();
		if ( indirectNode != null )
			indirectNode.appendHintsCountToName();
		for ( HintNode hinterNode : maps[AHint.DIRECT].values() )
			hinterNode.appendHintsCountToName();
		for ( HintNode hinterNode : maps[AHint.INDIRECT].values() )
			hinterNode.appendHintsCountToName();
		return root;
	}

}
