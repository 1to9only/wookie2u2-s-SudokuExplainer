/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Bounds;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Result;
import diuf.sudoku.Settings;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.hinters.als.Als;
import static diuf.sudoku.utils.Frmt.DIGITS;
import static diuf.sudoku.utils.Frmt.LETTERS;
import static diuf.sudoku.utils.Frmt.LOWERCASE_LETTERS;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import diuf.sudoku.utils.Log;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static java.awt.event.MouseEvent.BUTTON1;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * The SudokuGridPanel is the JPanel which represents the Sudoku grid in the
 * GUI, including its legends (row and column labels).
 * <p>
 * User actions are redirected to {@link diuf.sudoku.gui.SudokuExplainer}
 * which is here-in referred to as the 'engine'.
 * <p>
 * I "glom on" to my 'parent' JFrame (which contains me in the GUI).
 * <p>
 * I also "glom on" to the Sudoku 'grid' to be displayed.
 *
 * @see diuf.sudoku.gui.SudokuFrame
 * @see diuf.sudoku.gui.SudokuExplainer
 */
class SudokuGridPanel extends JPanel {

	private static final long serialVersionUID = 3709127163156966626L;

	// a cache of orangified colors.
	// No idea how big it should be, but 16 is big enough to start growing
	private static final Map<Color, Color> ORANGY_COLOR_CACHE = new HashMap<>(16);

	private static final Color COLOR_ORANGE = new Color(255, 185, 0);
	private static final Color COLOR_GREEN = new Color(0, 224, 0);
	private static final Color COLOR_BLUE = new Color(0, 0, 255);
	private static final Color COLOR_BLACK = new Color(0, 0, 0);
	private static final Color COLOR_PURPLE	= new Color(220, 5, 220);
	private static final Color COLOR_BROWN	= new Color(150, 75, 0);
	private static final Color COLOR_DARK_BLUE = Color.BLUE.darker();
	private static final Color COLOR_LEGEND	= new Color(0, 32, 64);
	// the aqua (bluey green) foreground color
	private static final Color COLOR_AQUA = new Color(192, 255, 255);
	// the dark aqua foreground color
	private static final Color COLOR_AQUA_DARK = new Color(0, 153, 153);
	// the dark yellow/green forground color
	private static final Color COLOR_YELLOW_DARK = new Color(204, 204, 0);
	// the pink cell foreground color
	private static final Color COLOR_PINK = new Color(255, 204, 255);
	// the green cell background color
	private static final Color COLOR_GREEN_BG = new Color(204, 255, 204, 153);
	// the aqua cell background color
	private static final Color COLOR_AQUA_BG = new Color(192, 255, 255, 24);
	// the orange cell background color
	private static final Color COLOR_ORANGE_BG = new Color(255, 204, 102, 24);
	// the blue cell background color
	private static final Color COLOR_BLUE_BG = new Color(204, 204, 255, 153);
	// the yellow cell background color
	private static final Color COLOR_YELLOW_BG = new Color(255, 255, 75, 24);
	// the brown cell background color
	private static final Color COLOR_BROWN_BG = new Color(204, 51, 0, 12);
	// a dark orange.
	private static final Color COLOR_ORANGY_BLACK = orangy(Color.BLACK);

	// base border and background
	private static final Color COLOR_BASE_BORDER = new Color(0, 0, 192); // blue
	private static final Color COLOR_BASE_BG = new Color(0, 0, 192, 12); // blue
	// cover border and background
	private static final Color COLOR_COVER_BORDER = new Color(0, 128, 0); // green
	private static final Color COLOR_COVER_BG = new Color(0, 128, 0, 12); // green
	// ALS border and foreground (value) colors
	private static final Color[] ALS_COLORS = {
			  COLOR_BASE_BORDER // blue
			, COLOR_COVER_BORDER // green
			, COLOR_AQUA_DARK
			, COLOR_YELLOW_DARK
			, COLOR_BROWN
	};
	// ALS region bacground colors
	private static final Color[] ALS_BG_COLORS = {
			  COLOR_BASE_BG // blue
			, COLOR_COVER_BG // green
			, COLOR_AQUA_BG
			, COLOR_YELLOW_BG
			, COLOR_BROWN_BG
	};

	// COLOR_POTS are in reverse order of importance because the last color set
	// is the one rendered (he who laughs last), so BLUE overwrites everything.
	// NB: Orange is now independant of red and green. Juillerat defined orange
	//     as the combination of red and green, which was crap!
	// NB: all COLOR_POTS index-references must use these constants, otherwise
	//     it renders them basically useless.
	private static final int CI_BROWN = 0;	// COLOR_INDEX
	private static final int CI_PURPLE = 1;
	private static final int CI_YELLOW = 2;
	private static final int CI_ORANGE = 3;
	private static final int CI_RED = 4;
	private static final int CI_GREEN = 5;
	private static final int CI_BLUE = 6;
	private static final int NUM_COLOR_POTS = 7;
	private static final Pots[] COLOR_POTS = new Pots[NUM_COLOR_POTS];

	// POTS_COLORS is array of the colors we highlight cell-values in, so that
	// can just look-up the color to paint using an array index (ie quickly).
	private static final Color[] POTS_COLORS = new Color[NUM_COLOR_POTS];
	static {
		POTS_COLORS[CI_BROWN] = COLOR_BROWN;
		POTS_COLORS[CI_PURPLE] = COLOR_PURPLE;
		POTS_COLORS[CI_YELLOW] = Color.YELLOW;
		POTS_COLORS[CI_ORANGE] = COLOR_ORANGE;
		POTS_COLORS[CI_RED] = Color.RED;
		POTS_COLORS[CI_GREEN] = COLOR_GREEN;
		POTS_COLORS[CI_BLUE] = Color.BLUE;
	}

	// number of corners in a triange
	private static final int NUM_POINTS = 3;
	// used to draw the arrow at the end of the link
	private static final int[] X_POINTS = new int[NUM_POINTS];
	private static final int[] Y_POINTS = new int[NUM_POINTS];

	private final ArrayList<Line> paintedLines = new ArrayList<>(64); // just a guess

	private final String FONT_NAME = "Verdana";

	private int COS = 64; // CELL_OUTER_SIZE // was 45 // was a lettuce
	private int CIS = 58; // CELL_INNER_SIZE // was 39
	private int CISo2 = CIS / 2;
	private int CISo3 = CIS / 3;
	private int CISo6 = CIS / 6;
	private int V_GAP = 2; // VERTICAL_GAP
	private int H_GAP = 42; // HORIZONTAL_GAP
	private int CELL_PAD = (COS - CIS) / 2; // (64-58)/2=3
	private int GRID_SIZE = COS * 9;
	private int FONT_SIZE_1 = 14; // 12
	private int FONT_SIZE_2 = 18; // 14
	private int FONT_SIZE_3 = 24; // 18
	private int FONT_SIZE_4 = 36; // 24
	private int FONT_SIZE_5 = 54; // 36
	// KRC 2019-09-06: made arrowHeads larger to see at 1080 (was 5 and 2)
	// and made them scale back down, if that ever happens
	// and made the LINK_OFFSET adjust to rescaling.
	private int ARROW_LENGTH = 9; // 5
	private int ARROW_WIDTH  = 3; // 2
	private double LINK_OFFSET = 4.0;  // 3.0
	private Dimension PREFFERED_SIZE
			= new Dimension(GRID_SIZE+H_GAP+V_GAP, GRID_SIZE+H_GAP+V_GAP);
	private final int BELOW_GRID = V_GAP + GRID_SIZE; // top of column labels

	private Grid grid; // the current Sudoku grid to display.
	private final SudokuExplainer engine; // I "borrow" my parents engine
	private final SudokuFrame frame; // my parent component in the GUI

	private Cell focusedCell = null;
	private Cell selectedCell = null;

	private Set<Cell> auqaBGCells;
	private Set<Cell> pinkBGCells;
	private Collection<ARegion> pinkRegions;
	private Set<Cell> redBGCells;
	private Set<Cell> greenBGCells;
	private Set<Cell> orangeBGCells;
	private Set<Cell> blueBGCells;
	private Set<Cell> yellowBGCells;
	private Result result;
	// cells to be set: Cell=>Values containing a single value
	private Pots results;
	private int resultColor;
	private Collection<ARegion> bases;
	private Collection<ARegion> covers;
	private Collection<Als> alss;
	private Collection<Link> links;
	private Idx[][] supers;
	private Idx[][] subs;

	private final Font smallFont1;
	private final Font smallFont2;
	private final Font smallFont3;
	private final Font legendFont;
	private final Font bigFont;

	private Color fillBGColor = Color.WHITE;

	SudokuGridPanel(Grid grid, SudokuExplainer engine, SudokuFrame frame) {
		super();
		this.grid = grid;		// input
		this.engine = engine;	// process
		this.frame = frame;		// output
		if ( getToolkit().getScreenSize().height < 1080 ) // was 750
			rescaleDown();
		initialise();
		super.setOpaque(false);
		smallFont1 = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE_1);
		smallFont2 = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_2);
		smallFont3 = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_3);
		legendFont = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_4);
		bigFont    = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_5);
	}

	private void rescaleDown() {
		COS = 45; // rescale(CELL_OUTER_SIZE);
		CIS = 39; // rescale(CELL_INNER_SIZE);
		CISo2 = CIS / 2;
		CISo3 = CIS / 3;
		CISo6 = CIS / 6;
		V_GAP = rescale(V_GAP);
		H_GAP = rescale(H_GAP);
		CELL_PAD = rescale(CELL_PAD);
		GRID_SIZE = rescale(GRID_SIZE);
		FONT_SIZE_1 = 12;
		FONT_SIZE_2 = 14;
		FONT_SIZE_3 = 18;
		FONT_SIZE_4 = 24;
		FONT_SIZE_5 = 36;
		ARROW_LENGTH = 5;
		ARROW_WIDTH = 2;
		LINK_OFFSET = 3.0;
		PREFFERED_SIZE = new Dimension(GRID_SIZE+H_GAP+V_GAP
				                     , GRID_SIZE+H_GAP+V_GAP);
	}

	private int rescale(int value) {
		return (int)(value * 0.66666666666666); //(int)(value * 2 / 3.0);
	}

	private void initialise() {
		this.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				setFocusedCell(null);
				e.consume();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// ignore mouse-down in row legends
				if ( e.getX() >= H_GAP ) {
					// get the cell at x and y, if any
					Cell cell = getCellAt(e.getX(), e.getY());
					if ( cell != null ) {
						boolean suppressSelect = false;
//int onmask = SHIFT_DOWN_MASK | BUTTON1_DOWN_MASK;
//int offmask = CTRL_DOWN_MASK;
//if ((event.getModifiersEx() & (onmask | offmask)) == onmask) {
//	// is shift-only-left-click (not Ctrl-shift-left-click or anything)
//}
						int mod = e.getModifiersEx();
						boolean alt = (mod & ALT_DOWN_MASK) == ALT_DOWN_MASK;
						int cnt = e.getClickCount();
						int btn = e.getButton();
						if ( alt ) {
							if ( cnt == 2 ) {
								greenBGCells = new HashSet<>();
								blueBGCells = new HashSet<>();
								repaint();
							} else if ( btn==BUTTON1 ) // Alt-LeftButton
								toggleGreen(cell);
							else // Alt - (Right or Middle) Button
								toggleBlue(cell);
							// stop this click selecting the cell
							suppressSelect = true;
							// NOTE: all other possible Alt-clicks are eaten
							e.consume();
						} else if ( cell.value != 0 ) {
							// Do nothing
						} else if ( cell.maybes.size == 1 ) {
							engine.setTheCellsValue(cell, cell.maybes.first());
						} else {
							int value = getMaybeAt(e.getX(), e.getY());
							if ( value != 0 ) {
								if ( btn==BUTTON1 && mod==0 ) { // plain left click
									// cell.value := the value that was clicked-on
									if ( cell.maybes.contains(value) )
										engine.setTheCellsValue(cell, value);
// Annoying when you're navigating around the grid with the mouse.
// Not annoying when you're trying to set a cells value and miss.
// There's more navigating than there is setting-and-missing, so it's out!
//									else
//										engine.beep();
								} else {
									// just remove/restore the maybe value
									engine.maybeTyped(cell, value);
									repaintCell(cell);
								}
							}
						}
						if ( !suppressSelect )
							setSelectedCell(cell);
						e.consume();
					}
				}
				SudokuGridPanel.super.requestFocusInWindow();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if ( e.getY() > BELOW_GRID ) { // below grid (column letters)
					if ( e.getX() < H_GAP ) { // left of the 'A'
						setRedBGCells(grid.getWrongens());
						e.consume();
						repaint();
					} else if ( e.getButton() == BUTTON1 ) { // left button
						final int i = (e.getX()-H_GAP) / COS;
						if ( i>-1 && i<LETTERS.length ) {
							setGreenPots(engine.cheat(grid, LETTERS[i]));
							e.consume();
							repaint();
						}
					} else { // right/middle button
						final int i = (e.getX()-H_GAP) / COS;
						if ( i>-1 && i<LOWERCASE_LETTERS.length ) {
							setGreenPots(engine.cheat(grid, LOWERCASE_LETTERS[i]));
							e.consume();
							repaint();
						}
					}
				} else if ( e.getX() < H_GAP ) { // left of grid (row numbers)
					// hold down the left button over a row legend
					//      to highlight all candidates of this number.
					// hold down the right/middle button over a row legend
					//      to highlight cells with this many potential values.
					final int v = ((e.getY()-V_GAP) / COS) + 1;
					if ( v>0 && v<10 ) {
						if ( e.getButton() == BUTTON1 ) // left
							setGreenPots(grid.getCandidatePots(v));
						else if ( v<9 || engine.cheatMode ) // right or middle
							setGreenPots(grid.getMaybesSizePots(v));
						else
							engine.beep();
						e.consume();
						repaint();
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// unhighlight all candidates. "Drag" the mouse to the right
				// and then release it to leave these candidates highlighted.
				if ( e.getX()<H_GAP || e.getY()>BELOW_GRID ) {
					setGreenPots(null);
					e.consume();
					repaint();
				}
			}

		});

		this.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				setFocusedCell(getCellAt(e.getX(), e.getY()));
			}
		});

		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				int mod = e.getModifiersEx();
				boolean ctrl = (mod & CTRL_DOWN_MASK) == CTRL_DOWN_MASK;
				if ( key==KeyEvent.VK_LEFT
				  || key==KeyEvent.VK_UP
				  || key==KeyEvent.VK_RIGHT
				  || key==KeyEvent.VK_DOWN ) {
					setFocusedCell(null);
					if ( selectedCell == null ) {
						setSelectedCell(grid.cells[40]); // 4*9+4 is centre cell
					} else {
						int x=selectedCell.x, y=selectedCell.y;
						switch (key) {
						case KeyEvent.VK_LEFT:  x=x==0 ? 8 : (x+8)%9; break;
						case KeyEvent.VK_RIGHT: x=x==8 ? 0 : (x+1)%9; break;
						case KeyEvent.VK_UP:    y=y==0 ? 8 : (y+8)%9; break;
						case KeyEvent.VK_DOWN:  y=y==8 ? 0 : (y+1)%9; break;
						}
						setSelectedCell(grid.cells[y*9+x]);
					}
					e.consume();
				} else if ( key==KeyEvent.VK_DELETE
						 || key==KeyEvent.VK_BACK_SPACE ) {
					if ( selectedCell != null ) {
						engine.setTheCellsValue(selectedCell, 0);
						repaint();
						e.consume();
					}
				} else if ( key>=KeyEvent.VK_1 && key<=KeyEvent.VK_9 ) { // Ctrl-1..9
					if ( selectedCell!=null && ctrl ) {
						int value = key - KeyEvent.VK_0;
						engine.maybeTyped(selectedCell, value);
						repaintCell(selectedCell);
						e.consume();
					}
				} else if ( key == KeyEvent.VK_ESCAPE ) {
					setSelectedCell(null);
					engine.clearHints();
					repaint();
					e.consume();
				} else if ( key==KeyEvent.VK_Z && ctrl ) { // Ctrl-Z
					engine.undo();
					repaint();
					e.consume();
				} else if ( key==KeyEvent.VK_Y && ctrl ) { // Ctrl-Y
					engine.redo();
					repaint();
					e.consume();
				}
			}
			@Override
			public void keyTyped(KeyEvent e) {
				boolean isProcessed = false;
				if ( selectedCell != null ) {
					char ch = e.getKeyChar();
					if ( ch>='1' && ch<='9' ) {
						int value = ch - '0';
						Cell sCell = selectedCell;
						engine.setTheCellsValue(selectedCell, value);
						isProcessed = true;
						selectedCell = sCell;
						repaint();
						e.consume();
					} else if ( (ch==' ' || ch=='0') ) {
						engine.setTheCellsValue(selectedCell, 0);
						repaint();
						isProcessed = true;
						e.consume();
					} else if ( ch=='\r' || ch=='\n' ) {
						setSelectedCell(null);
						frame.getBtnSolveStep().requestFocusInWindow();
						repaint();
						isProcessed = true;
						e.consume();
					}
				}
				if (!isProcessed && e.getComponent() != SudokuGridPanel.this.frame){
					e.setSource(SudokuGridPanel.this.frame);
					dispatchEvent(e);
				}
			}
		});
	} // end initialise() method.

	private void toggleGreen(Cell cell) {
		// if it's in green then it's not in blue
		if(blueBGCells!=null)blueBGCells.remove(cell);
		if ( greenBGCells==null ) {
			greenBGCells = new HashSet<>(16, 0.75F);
			greenBGCells.add(cell);
		} else if ( !greenBGCells.remove(cell) )
			greenBGCells.add(cell);
	}

	private void toggleBlue(Cell cell) {
		// if it's in blue then it's not in green
		if(greenBGCells!=null)greenBGCells.remove(cell);
		if ( blueBGCells==null ) {
			blueBGCells = new HashSet<>(16, 0.75F);
			blueBGCells.add(cell);
		} else if ( !blueBGCells.remove(cell) )
			blueBGCells.add(cell);
	}

	// returns the Cell which is at the given x,y.
	private Cell getCellAt(int x, int y) {
		int cx = (x - H_GAP) / COS;
		if(cx<0||cx>8) return null;
		int cy = (y - V_GAP) / COS;
		if(cy<0||cy>8) return null;
		return grid.cells[cy*9+cx];
	}

	// returns the potential value that is at (or should be at) the given x,y.
	private int getMaybeAt(int x, int y) {
		// Get cell's top-left corner, or left-top corner in an xy world.
		int cx = (x - H_GAP) / COS;
		int cy = (y - V_GAP) / COS;
		if ( cx<0||cx>8 || cy<0||cy>8 )
			return 0;
		Cell cell = grid.cells[cy*9+cx]; // dip back into yx world. Confusing!
		if ( cell == null )
			return 0;
		// Substract cell's corner // yep, flip again. My brain hurts already!
		x = x - cx*COS - H_GAP;
		y = y - cy*COS - V_GAP;
		// Get the maybe
		int mx = (x - CELL_PAD) / CISo3;
		int my = (y - CELL_PAD) / CISo3;
		if ( mx<0||mx>2 || my<0||my>2 )
			return 0;
		return my*3 + mx + 1; // finally something an idjit like me understands!
	}

	/** @return the grid. */
	Grid getGrid() {
		return grid;
	}

	/** Set the grid. */
	void setGrid(Grid grid) {
		this.grid = grid;
		this.selectedCell = grid.cells[40]; // center cell 4*9+4 = 20
	}

	/** Set the aqua cell backgrounds. */
	void setAquaBGCells(Set<Cell> cells) {
		this.auqaBGCells = cells;
	}

	/** Set the pink cell backgrounds. */
	void setPinkBGCells(Set<Cell> cells) {
		this.pinkBGCells = cells;
	}

	/** Set the regions to be outlined in pink. */
	void setPinkRegions(Collection<ARegion> pinkRegions) {
		this.pinkRegions = pinkRegions;
	}

	/** Set the red cell backgrounds. */
	void setRedBGCells(Set<Cell> cells) {
		this.redBGCells = cells;
	}

	/** Set the green cell backgrounds. */
	void setGreenBGCells(Set<Cell> cells) {
		this.greenBGCells = cells;
	}

	/** Set the orange cell backgrounds. */
	void setOrangeBGCells(Set<Cell> cells) {
		this.orangeBGCells = cells;
	}

	/** Set the blue cell backgrounds. */
	void setBlueBGCells(Set<Cell> cells) {
		this.blueBGCells = cells;
	}

	/** Set the yellow cell backgrounds. */
	void setYellowBGCells(Set<Cell> cells) {
		this.yellowBGCells = cells;
	}

	/** Set the result: a cell value to paint larger. */
	void setResult(Result result) {
		this.result = result;
	}

	/** Set the results: cell values to paint larger. */
	void setResults(Pots results) {
		this.results = results;
	}
	/** -1 for none, 0 for green, 1 for blue. */
	void setResultColor(int resultColor) {
		this.resultColor = resultColor;
	}

	// it's faster to detect empty ONCE here than repeatedly downstream
	private static void setColorPots(int colorIndex, Pots pots) {
		if ( pots==null || pots.isEmpty() )
			COLOR_POTS[colorIndex] = null;
		else
			COLOR_POTS[colorIndex] = pots;
	}

	/** Set the green potentials.
	 * NOTE that formerly green+red=orange. Now oranges are separate! */
	void setGreenPots(Pots pots) {
		setColorPots(CI_GREEN, pots);
	}

	/** Set the red potentials.
	 * NOTE that formerly green+red=orange. Now oranges are separate! */
	void setRedPots(Pots pots) {
		setColorPots(CI_RED, pots);
	}

	/** Set the orange potentials.
	 * NOTE that formerly green+red=orange. Now oranges are separate! */
	void setOrangePots(Pots pots) {
		setColorPots(CI_ORANGE, pots);
	}

	/** Set the blue potentials. Used in nested hint explanations. */
	void setBluePots(Pots pots) {
		setColorPots(CI_BLUE, pots);
	}

	/** Set the yellow potentials. Implemented for Coloring hints. */
	void setYellowPots(Pots pots) {
		setColorPots(CI_YELLOW, pots);
	}

	/** Set the purple potentials. Implemented for Coloring hints. */
	void setPurplePots(Pots pots) {
		setColorPots(CI_PURPLE, pots);
	}

	/** Set the brown potentials. Implemented for Coloring hints. */
	void setBrownPots(Pots pots) {
		setColorPots(CI_BROWN, pots);
	}

	/** Set the bases (the blues). */
	void setBases(Collection<ARegion> bases) {
		this.bases = bases;
	}

	/** Set the covers (the greens). */
	void setCovers(Collection<ARegion> covers) {
		this.covers = covers;
	}

	/** Set the brown potentials. Implemented for Coloring hints. */
	void setAlss(Collection<Als> alss) {
		this.alss = alss;
	}

	/** Set the links (brown arrows). */
	void setLinks(Collection<Link> links) {
		this.links = links;
	}

	/** Set the Super markers (+'s in GREEN and BLUE). */
	void setSupers(Idx[][] supers) {
		this.supers = supers;
	}

	/** Set the Sub markers (-'s in GREEN and BLUE). */
	void setSubs(Idx[][] subs) {
		this.subs = subs;
	}

	/**
	 * Clears the focused cell,
	 * and if 'isSelection' then clears the selectedCell.
	 */
	void clearSelection(boolean isSelection) {
		this.focusedCell = null;
		if ( isSelection )
			this.selectedCell = null;
	}

	private void repaintCell(Cell cell) {
		if ( cell != null )
			repaint(cell.x*COS+H_GAP, cell.y*COS+V_GAP, COS, COS);
	}

	public void setFocusedCellS(String id) {
		if ( id == null )
			setFocusedCell(null);
		else
			setFocusedCell(grid.get(id));
	}

	private void setFocusedCell(Cell cell) {
		repaintCell(this.focusedCell);
		this.focusedCell = cell;
		repaintCell(this.focusedCell);
	}

	private void setSelectedCell(Cell cell) {
		repaintCell(this.selectedCell);
		this.selectedCell = cell;
		repaintCell(this.selectedCell);
	}

	/** Get the preferred size of this panel. */
	@Override
	public Dimension getPreferredSize() {
		return PREFFERED_SIZE;
	}

	/** Get the minimum size of this panel. */
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	private void drawStringCentered(Graphics g, String s, int x, int y) {
		Rectangle2D rect = g.getFontMetrics().getStringBounds(s, g);
		double px = x - rect.getWidth() / 2;
		double py = y - rect.getHeight() / 2 - rect.getY();
		g.drawString(s, (int)(px + 0.5), (int)(py + 0.5));
	}

	private void drawStringCentered(Graphics g, String s, int x, int y, Font f) {
		g.setFont(f);
		drawStringCentered(g, s, x,y);
	}

	private void drawStringCentered3D(Graphics g, String s, int x, int y, Font f) {
		g.setFont(f);
		Color color = g.getColor();
		g.setColor(Color.black);  drawStringCentered(g, s, x,   y+1);
		g.setColor(Color.yellow); drawStringCentered(g, s, x-1, y);
		g.setColor(color);        drawStringCentered(g, s, x,   y);
	}

	private static Color orangy(Color col) {
		Color orangyCol = ORANGY_COLOR_CACHE.get(col);
		if ( orangyCol != null )
			return orangyCol;
		orangyCol = new Color(
			// my orange = (255, 185, 0) that's (R, G, B)
			  (col.getRed()   + 255) / 2
			, (col.getGreen() + 185) / 2
			, (col.getBlue()  +   0) / 2
		);
		ORANGY_COLOR_CACHE.put(col, orangyCol);
		return orangyCol;
	}

	/** Paint this JPanel. */
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		initGraphics(g2);
		paintLegends(g); // Wally wasn't happy.
		AffineTransform oldTransform = g2.getTransform();
		AffineTransform translate = AffineTransform.getTranslateInstance(
				H_GAP, V_GAP);
		g2.transform(translate);
		g.clearRect(0, 0, GRID_SIZE, GRID_SIZE);
		paintSelectionAndFocus(g);
		paintGrid(g);
		if ( alss != null ) {
			// paint the background of ALS cells only
			paintAlss(g, alss);
		} else {
			// paint regions for non-ALS's
			paintRegions(g, covers, COLOR_COVER_BORDER, COLOR_COVER_BG);
			paintRegions(g, bases, COLOR_BASE_BORDER, COLOR_BASE_BG);
			paintRegions(g, pinkRegions, COLOR_PINK, null);
		}
		paintCellValues(g);
		paintSuperAndSubMarkers(g);
		paintLinks(g);
		g2.setTransform(oldTransform);
	}

	private void initGraphics(Graphics2D g2) {
		if (THE_SETTINGS.get(Settings.isAntialiasing)) {
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		} else {
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		}
	}

	private void paintLegends(Graphics g) {
		g.setFont(legendFont);
		g.setColor(COLOR_LEGEND);
		for ( int i=0; i<9; ++i ) {
			// y-axis legend (vertical = row labels: 1..9)
			drawStringCentered(g, DIGITS[i+1]
					, H_GAP/2					// x
					, V_GAP + COS*i + COS/2		// y
			);
			// x-axis legend (horizontal = column labels: A..I)
			drawStringCentered(g, LETTERS[i]
					, H_GAP + i*COS + COS/2		// x
					, V_GAP + COS*9 + H_GAP/2	// y
			);
		}
	}

	// makes the grid background go AQUA for an eigth of a second,
	// to tell the user that this puzzle now solves just with singles,
	void flashBackgroundAqua() {
		flashBackground(COLOR_AQUA);
	}

	// makes the grid background go PINK for an eigth of a second,
	// to tell the user "You ____ed-up!" and also the goes "ping".
	void flashBackgroundPink() {
		flashBackground(Color.PINK);
	}

	// makes the grid background go Color for an eigth of a second
	private void flashBackground(Color c) {
		if ( fillBGColor == c )
			return; // trying to step these running into each other
		fillBGColor = c;
		repaint();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// wait for an eight of a second then repaint it white again.
				try{Thread.sleep(125);}catch(InterruptedException eaten){}
				fillBGColor = Color.WHITE;
				repaint();
			}
		});
	}

	private void paintSelectionAndFocus(Graphics g) {
		for ( Cell cell : grid.cells ) {
			Color col = fillBGColor;
			if ( cell == selectedCell )
				col = Color.ORANGE;
			else if ( cell == focusedCell )
				col = Color.YELLOW;
			else if ( auqaBGCells!=null && auqaBGCells.contains(cell) )
				col = COLOR_AQUA;
			else if ( pinkBGCells!=null && pinkBGCells.contains(cell) )
				col = COLOR_PINK;
			else if ( redBGCells!=null && redBGCells.contains(cell) )
				col = Color.RED;
			else if ( blueBGCells!=null && blueBGCells.contains(cell) )
				col = COLOR_BLUE_BG;
			else if ( greenBGCells!=null && greenBGCells.contains(cell) )
				col = COLOR_GREEN_BG;
			else if ( orangeBGCells!=null && orangeBGCells.contains(cell) )
				col = COLOR_ORANGE_BG;
			else if ( yellowBGCells!=null && yellowBGCells.contains(cell) )
				col = COLOR_YELLOW_BG;
			g.setColor(col);
			g.fillRect(cell.x*COS, cell.y*COS, COS, COS);
		} // next x, y
	}

	private void paintGrid(Graphics g) {
		int lineWidth, offset;
		for ( int i=0; i<10; ++i ) {
			if ( i % 3 == 0 ) {
				lineWidth = 4;
				g.setColor(Color.black);
			} else {
				lineWidth = 2;
				g.setColor(COLOR_DARK_BLUE);
			}
			offset = lineWidth / 2;
			g.fillRect(i*COS-offset, 0-offset, lineWidth, GRID_SIZE+lineWidth);
			g.fillRect(0-offset, i*COS-offset, GRID_SIZE+lineWidth, lineWidth);
		}
	}

	private void paintRegions(
			  final Graphics g
			, final Collection<ARegion> regions
			, final Color borderColor
			, final Color backgroundColor // null means no shading
	) {
		if ( regions == null )
			return;
		for ( ARegion region : regions ) {
			if ( region == null )
				continue;
			// nb: having the region know it's bounds is a bit hacky coz I've
			// injected a presentation concern into the model. If it s__ts you
			// then rip the bound attribute out of region and create a
			// {@code private static final Bounds[] BOUNDS} array here and
			// populate with the Bounds constructor currently imbedded in the
			// Box, Row, and Col constructors. I won't coz it works. Bounds
			// is STILL an attribite of the region, even if only presentation.
			// Best of both worlds is a method, with cache, to avoid creating
			// new EVERY TIME we paint the bastard, which is where I started.
			Bounds b = region.bounds; // here's one I prepared earlier
			g.setColor(borderColor); // blue or green
			for ( int s=-2; s<3; s++ ) // 5 times: -2, -1, 0, 1, 2
				g.drawRect(b.x*COS+s, b.y*COS+s, b.w*COS-s*2, b.h*COS-s*2);
			if ( backgroundColor != null ) {
				g.setColor(backgroundColor); // blue or green shading (alpha)
				g.fillRect(b.x*COS+3, b.y*COS+3, b.w*COS-6, b.h*COS-6);
			}
		} // next region
	}

	/**
	 * paint the ALSs, if any, in me the SudokuGridPanel. Note that alss may be
	 * null, in which case I am never called, but it should never be empty.
	 *
	 * @param g
	 * @param alss
	 */
	private void paintAlss(final Graphics g, Collection<Als> alss) {
		// If you're seeing this then add another ALS_COLORS and ALS_BG_COLORS.
		if ( alss.size() > ALS_COLORS.length )
			Log.println("WARN: paintAlss: more ALS's than colors!");
		int i = 0; // the color index
		for ( Als a : alss ) {
			if ( a != null ) { // the last als may be null. sigh.
				// null backgroundColor: I paint backgrounds of my cells.
				paintRegions(g, a.regions(), ALS_COLORS[i], null);
				for ( Cell cell : a.cells ) {
					// paint the cell background
					g.setColor(ALS_BG_COLORS[i]);
					g.fillRect(cell.x*COS+2, cell.y*COS+2, COS-4, COS-4);
					// paint the foreground
					g.setColor(ALS_COLORS[i]);
					for ( int v : VALUESES[cell.maybes.bits] )
						drawStringCentered3D(g, DIGITS[v]
							, cell.x*COS + CELL_PAD + ((v-1)%3)*CISo3 + CISo6
							, cell.y*COS + CELL_PAD + ((v-1)/3)*CISo3 + CISo6
							, smallFont2
						);
				}
				// prevent AIOOBE if there's ever more ALS's than there are colors,
				// by making the color index wrap around to zero. If you see two
				// blue ALSs then add another ALS_COLORS and ALS_BG_COLORS.
				i = (i+1) % ALS_COLORS.length;
			}
		}
	}

	/**
	 * This is an intermediate step to translate the array of Cell=>Values into
	 * an array of Colors. We do this for efficiency: it's faster to iterate
	 * each of the Pots than it is to look-up each Cell=>Value in each Pots.
	 * There's 81*9 Cell Values by 7 Colors, so that's 5,103 HashMap.get()s to
	 * paint an empty Sudoku grid. Now we first ONCE run 7 HashMap.keySet() so
	 * ALL the subsequent however-many-gets are hits; then we execute 5,103
	 * array-lookups, and an array-lookup is MUCH faster than HashMap.get. So
	 * it's faster overall because it dramatically reduces the number of gets!
	 * Nothing But Net! The downside is memory: a matrix of 81*9 Color's. Meh!
	 * <p>
	 * NOTE: formerly green+red=orange. Now oranges are separate!
	 * <p>
	 * KRC 2021-06-21 I'm replacing all keySet->get with entrySet, because it's
	 * faster, and I've done so here. I think it's worth noting the original
	 * did upto 5,103 gets, and now we do precisely 0, which just goes to show
	 * that there really are more than one ways to skin a cat, just some are a
	 * bit more efficient than others. I think this is now "fairly efficient".
	 * The only possible enhancement that I can see is making paintCellValues
	 * (my caller) default to Color.GRAY instead of defaulting to GRAY down in
	 * the setMaybeColor function, which would then no longer need to exist;
	 * we could just look-up the [cell-indice][value-1] in MAYBES_COLORS and
	 * if it's null we use GRAY. I'd also like to delay creating MAYBES_COLORS
	 * sub-arrays until required, because we have an array of 9 Colors for each
	 * of atleast 17 clues, which is 17*9=153 unused Color, which isn't small,
	 * so its a bit of a waste of memory: 153*(4+4+4+4+4)=3,060 bytes.
	 */
	private void initMaybesColors() {
		// clear all MAYBES_COLORS (a matrix[cellIndex][value-1])
		for ( int i=0; i<81; ++i )
			Arrays.fill(MAYBES_COLORS[i], null);
		// populate the MAYBES_COLORS array from the Pots array
		Pots pots;  Color color;
		Color[] cellsMaybesColor;
		for ( int i=0; i<NUM_COLOR_POTS; ++i ) {
			color = POTS_COLORS[i];
			if ( (pots=COLOR_POTS[i]) != null ) {
				assert !pots.isEmpty() : "empty pots denied, for speed!";
				for ( java.util.Map.Entry<Cell,Values> e : pots.entrySet() ) {
					cellsMaybesColor = MAYBES_COLORS[e.getKey().i];
					//nb: INDEXES[i] is VALUES[i] with one removed from each v,
					//to cater for a one-based-value in a zero-based-array.
					//Just beware that the returned v is acually vMinus1!
					for ( int vMinus1 : INDEXES[e.getValue().bits] )
						cellsMaybesColor[vMinus1] = color;
				}
			}
		}
	}
	// first index is indice 0..80, second index is value-1 0..8.
	private static final Color[][] MAYBES_COLORS = new Color[81][9];

	// set the graphics-color to the color of the given Cell-value (ie maybe)
	// @return isHighlighted == color!=GRAY (the default)
	private boolean setMaybeColor(Graphics g, int i, int vMinus1) {
		final Color color;
		// value-1 for one-based-value to zero-based-array
		if ( (color=MAYBES_COLORS[i][vMinus1]) != null ) {
			g.setColor(color);
			return true;
		} else {
			g.setColor(Color.GRAY);
			return false;
		}
	}

	/**
	 * Paint each Cells value, or its maybes (potential values).
	 * @param g The Graphics to paint on.
	 */
	private void paintCellValues(Graphics g) {
		final boolean isShowingMaybes = THE_SETTINGS.get(Settings.isShowingMaybes);
		initMaybesColors(); // sets MAYBES_COLORS
		Values values;
		int x,y, cx,cy, i;
		boolean isHighlighted;
		for ( Cell cell : grid.cells ) {
			x = cell.x;  y = cell.y;
			if ( cell.value != 0 ) {
				cx = x*COS + CELL_PAD + CISo2;
				cy = y*COS + CELL_PAD + CISo2;
				g.setColor(cell==selectedCell?Color.BLACK:COLOR_ORANGY_BLACK);
				drawStringCentered(g, DIGITS[cell.value], cx, cy, bigFont);
			} else {
				// Paint potentials
				for ( int v : VALUESES[cell.maybes.bits] ) {
					i = v - 1;
					cx = x*COS + CELL_PAD + (i%3)*CISo3 + CISo6;
					cy = y*COS + CELL_PAD + (i/3)*CISo3 + CISo6;
					isHighlighted = setMaybeColor(g, cell.i, v-1);
					if ( result!=null && result.equals(cell, v) )
						drawStringCentered3D(g, DIGITS[v], cx,cy, smallFont3);
					else if ( results!=null && (values=results.get(cell))!=null
						   && values.contains(v) ) {
						g.setColor(resultColor()); // result ON's green/blue
						drawStringCentered3D(g, DIGITS[v], cx,cy, smallFont3);
					} else if ( isHighlighted )
						drawStringCentered3D(g, DIGITS[v], cx,cy, smallFont2);
					else if (isShowingMaybes) { // g.color is set to gray
						drawStringCentered(g, DIGITS[v], cx,cy, smallFont1);
					}
				}
			}
		} // next x, y
	}

	// Super and Sub marker colors
	private static final Color[] SS_COLORS = {COLOR_GREEN, COLOR_BLUE};

	private void paintSuperAndSubMarkers(Graphics g) {
		if ( supers==null || subs==null )
			return;
		g.setFont(smallFont2);
		for ( int c=0; c<2; ++c ) {
			g.setColor(SS_COLORS[c]);
			for ( int v=1; v<10; ++v ) {
				if ( subs[c][v].any() )
					paintMarkers(g, v, subs[c][v], MINUS, 8);
				if ( supers[c][v].any() )
					paintMarkers(g, v, supers[c][v], PLUS, 10);
			}
		}
		// over-paint RED any that're in both sub colors.
		g.setColor(Color.RED);
		for ( int v=1; v<10; ++v )
			if ( tmp.setAndAny(subs[0][v], subs[1][v]) )
				paintMarkers(g, v, tmp, MINUS, 8);
	}
	private final Idx tmp = new Idx();

	// draw String s centered on v in each cell in idx
	private void paintMarkers(Graphics g, int v, Idx idx, String s, int offset) {
		final int u = v - 1;
		for ( int i : idx.toArrayB() )
			// x,y are the centre point to paint at (I expected top left)
			drawStringCentered(g, s
					, i%9*COS + CELL_PAD + (u%3)*CISo3 + CISo6 + offset // horizontal
					, i/9*COS + CELL_PAD + (u/3)*CISo3 + CISo6 // vertical
			);
	}

	private Color resultColor() {
		switch ( resultColor ) {
			case 0: return COLOR_GREEN;
			case 1: return COLOR_BLUE;
			default: return COLOR_BLACK; // should never happen!
		}
	}

	private final class Line {
		public final int sx, sy, ex, ey;
		public Line(int sx, int sy, int ex, int ey) {
			this.sx=sx; this.sy=sy; this.ex=ex; this.ey=ey;
		}
		private int distanceUnscaled(int px, int py) {
			// Vectorial product, without normalization by length
			return (px - sx) * (ey - sy) - (py - sy) * (ex - sx);
		}
		private boolean intervalOverlaps(int s1, int e1, int s2, int e2) {
			if (s1 > e1) { // Swap
				s1 ^= e1;
				e1 = s1^e1;
				s1 ^= e1;
			}
			if (s2 > e2) { // Swap
				s2 ^= e2;
				e2 = s2^e2;
				s2 ^= e2;
			}
			return s1 < e2 && e1 > s2;
		}
		private boolean overlaps(Line other) {
			if ( distanceUnscaled(other.sx, other.sy) == 0
			  && distanceUnscaled(other.ex, other.ey) == 0 ) {
				// Both lines are on the same right
				return intervalOverlaps(this.sx, this.ex, other.sx, other.ex)
					|| intervalOverlaps(this.sy, this.ey, other.sy, other.ey);
			}
			return false;
		}
	}

	private static final class DblPoint {
		public double x;
		public double y;
	}

	private static final class IntPoint {
		public int x;
		public int y;
		@Override
		public boolean equals(Object obj) {
			if ( obj instanceof Cell ) {
				Cell that = (Cell)obj;
				return x==that.x && y==that.y;
			} else if ( obj instanceof IntPoint ) {
				IntPoint that = (IntPoint)obj;
				return x==that.x && y==that.y;
			} else if ( obj instanceof DblPoint ) {
				DblPoint that = (DblPoint)obj;
				return x==(int)that.x && y==(int)that.y;
			}
			return false;
		}
		@Override
		public int hashCode() {
			return y<<4 + x; // consistent with Grid.Cell.hashCode(), which isn't an imperative or anything.
		}

	}

	// sets (mutates) 'p' to the position of cell.value
	// NB: doing it this way we only need create 2 points, less G = less GC.
	private void position(Cell cell, int value, DblPoint p) {
		double x = cell.x * COS + CELL_PAD + CISo6;
		double y = cell.y * COS + CELL_PAD + CISo6;
		if (value > 0) {
			p.x = x + ((value - 1) % 3) * CISo3;
			p.y = y + ((value - 1) / 3) * CISo3;
		} else {
			p.x = x + CISo3;
			p.y = y + CISo3;
		}
	}

	private void paintLinks(Graphics g) {
		if ( links==null || links.size()==0 )
			return;
		g.setColor(Color.orange);
		DblPoint s=new DblPoint(), e=new DblPoint(); // s=source, e=end
		Line line;
		// s=shift, u=unit, m=move, l=left, r=right
		// I'm pretty sure the computer doesn't actually need a couple of these
		// variables, but I'm a human being, and a piss-poor mathematician at
		// that, so I do, so I'm keeping them.
		double sx,sy, length, ux,uy, mx,my, lx,ly, rx,ry;
		int overlapCount;
		for ( Link link : links ) {
			position(link.srcCell, link.srcValue, s); // get the starting point
			position(link.endCell, link.endValue, e); // get the ending point
			// Get unity vector
			sx = e.x - s.x; // shiftX = end.x - start.x
			sy = e.y - s.y; // shiftY = end.y - start.y
			// the length of the hippopotamus equals the square root of the
			// sum of the square of the other two side.
			length = Math.sqrt( sx*sx + sy*sy );
			ux = sx / length; // the amount we move horizontaly per unit
			uy = sy / length; // the amount we move vertically per unit

			// Build line object
			line = new Line((int)s.x, (int)s.y, (int)e.x, (int)e.y);

			// Count number of overlapping lines
			overlapCount = 0;
			for ( Line other : paintedLines )
				if ( line.overlaps(other) )
					++overlapCount;

			// Move the line perpendicularly to go away from overlapping lines
			mx = uy * ((overlapCount+1)/2) * LINK_OFFSET; // horizontally
			my = ux * ((overlapCount+1)/2) * LINK_OFFSET; // vertically
			if (overlapCount % 2 == 0)
				mx = -mx; // negate the x to move by
			else
				my = -my; // negate the y to move by
			// suppress arrows on short links, coz there ain't room for em.
			if ( length >= (CIS>>1) ) { // length >= CELL_INNER_SIZE/2
				// Truncate end points (move of "middle" of potential value)
				if (link.srcValue > 0) {
					s.x += ux * CISo6; // startX += unitX * (CELL_INNER_SIZE/6)
					s.y += uy * CISo6;
				}
				if (link.endValue > 0) {
					e.x -= ux * CISo6; // endX -= unitX * (CELL_INNER_SIZE/6)
					e.y -= uy * CISo6;
				}
				if (link.endValue > 0) {
					// Draw arrow at end point
					lx = e.x - ux*ARROW_LENGTH + uy*ARROW_WIDTH;
					ly = e.y - uy*ARROW_LENGTH - ux*ARROW_WIDTH;
					rx = e.x - ux*ARROW_LENGTH - uy*ARROW_WIDTH;
					ry = e.y - uy*ARROW_LENGTH + ux*ARROW_WIDTH;
					X_POINTS[0] = (int)(e.x + mx);
					X_POINTS[1] = (int)(rx + mx);
					X_POINTS[2] = (int)(lx + mx);
					Y_POINTS[0] = (int)(e.y + my);
					Y_POINTS[1] = (int)(ry + my);
					Y_POINTS[2] = (int)(ly + my);
					g.fillPolygon(X_POINTS, Y_POINTS, NUM_POINTS);
				}
				paintedLines.add(line);
			}
			// Draw the line
			g.drawLine(
					  (int)(s.x + mx)
					, (int)(s.y + my)
					, (int)(e.x + mx)
					, (int)(e.y + my)
			);
		} // next link
		paintedLines.clear(); // let gc cleanup the Lines but keep the container
	}

}
