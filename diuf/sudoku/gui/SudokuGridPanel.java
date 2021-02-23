/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Bounds;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Result;
import diuf.sudoku.Settings;
import diuf.sudoku.Values;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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
 * The SudokuPanel is the JPanel which represents the Sudoku grid in the GUI,
 * including its legends (row and column labels). I know how to paint!
 * <p>User actions are redirected to {@link diuf.sudoku.gui.SudokuExplainer}
 *  which is here-in referred to as the 'engine'.
 * <p>I "glom on" to my 'parent' JFrame (which contains me in the GUI).
 * <p>I also "glom on" to the Sudoku 'grid' to be displayed.
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
	private static final Color COLOR_PURPLE	= new Color(220, 5, 220);
	private static final Color COLOR_BROWN	= new Color(150, 75, 0);
	private static final Color COLOR_DARK_BLUE = Color.BLUE.darker();
	private static final Color COLOR_LEGEND	= new Color(0, 32, 64);
	// the aqua (bluey green) cell background color.
	private static final Color COLOR_AQUA = new Color(192, 255, 255);
	// the pink cell background color.
	private static final Color COLOR_ORANGY_BLACK = orangy(Color.BLACK);
	private static final Color COLOR_GREY = new Color(222, 222, 222);

	// base border and background
	private static final Color BASE_BORDER_COLOR = new Color(0, 0, 192); // blue
	private static final Color BASE_BACKGROUND_COLOR = new Color(0, 0, 192, 12); // blue
	// cover border and background
	private static final Color COVER_BORDER_COLOR = new Color(0, 128, 0); // green
	private static final Color COVER_BACKGOUND_COLOR = new Color(0, 128, 0, 12); // green

	// COLOR_POTS are in reverse order of importance because the last color set
	// is the one rendered (he who laughs last), so BLUE overwrites everything.
	// NB: Orange is now independant of red and green. Juillerat defined orange
	//     as the combination of red and green.
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

	private static final String[] DIGITS = new String[] {
		".", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	private static final String[] LETTERS = new String[] {
		"A", "B", "C", "D", "E", "F", "G", "H", "I"
	};


	// number of corners in a triange
	private static final int NUM_POINTS = 3;
	// used to draw the arrow at the end of the link
	private static final int[] xPoints = new int[NUM_POINTS];
	private static final int[] yPoints = new int[NUM_POINTS];

	private final ArrayList<Line> paintedLines = new ArrayList<>(64); // just a guess

	private final String FONT_NAME = "Verdana";

	private int COS = 64; // CELL_OUTER_SIZE=45
	private int CIS = 58; // CELL_INNER_SIZE=39
	private int CISo2 = CIS / 2;
	private int CISo3 = CIS / 3;
	private int CISo6 = CIS / 6;
	private int V_GAP = 2;
	private int H_GAP = 42;
	private int CELL_PAD = (COS - CIS) / 2;
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
	private Dimension PREFFERED_SIZE = new Dimension(GRID_SIZE+H_GAP+V_GAP
			, GRID_SIZE+H_GAP+V_GAP);

	private Grid grid; // the current Sudoku grid to display.
	private final SudokuExplainer engine; // I "borrow" my parents engine
	private final SudokuFrame frame; // my parent component in the GUI

	private Cell focusedCell = null;
	private Cell selectedCell = null;

	private Set<Cell> auqaBGCells;
	private Set<Cell> redBGCells;
	private Set<Cell> greyBGCells;
	private Result result;
	private Collection<ARegion> bases;
	private Collection<ARegion> covers;
	private Collection<Link> links;

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
		this.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				setFocusedCell(null);
				e.consume();
			}

			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
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
						int modEx = e.getModifiersEx();
						boolean altDown = (modEx & InputEvent.ALT_DOWN_MASK)
								== InputEvent.ALT_DOWN_MASK;
						int btn = e.getButton();
						boolean rightBtn = btn==MouseEvent.BUTTON2
										|| btn==MouseEvent.BUTTON3;
						if ( rightBtn && altDown ) { //Alt-RightButton
							toggleGrey(cell);
							suppressSelect = true;
							e.consume();
						} else if ( cell.value != 0 ) {
							// Do nothing
						} else if ( cell.maybes.size == 1 ) {
							engine.setTheCellsValue(cell, cell.maybes.first());
						} else {
							int value = getMaybeAt(e.getX(), e.getY());
							if ( value != 0 ) {
								if ( btn==MouseEvent.BUTTON1 && modEx==0 ) { // plain left click
									// cell.value := the value that was clicked-on
									if ( cell.maybes.contains(value) )
										engine.setTheCellsValue(cell, value);
									else
										java.awt.Toolkit.getDefaultToolkit().beep();
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
				// hold down the mouse button over the row legend to highlight
				// all candidates of this number.
				boolean handled = false;
				if ( e.getX() < H_GAP ) {
					int v = ((e.getY() - V_GAP) / COS) + 1; // CELL_OUTER_SIZE
					if ( v>0 && v<10 ) {
						setGreenPots(engine.getGrid().getCandidatePots(v));
						handled = true;
						e.consume();
						repaint();
					}
				}
				if ( !handled )
					super.mousePressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// unhighlight all candidates. "Drag" the mouse to the right
				// and then release it to leave these candidates highlighted.
				if ( e.getX() < H_GAP ) {
					setGreenPots(null);
					e.consume();
					repaint();
				} else
					super.mouseReleased(e);
			}

		});

		this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			@Override
			public void mouseMoved(java.awt.event.MouseEvent e) {
				setFocusedCell(getCellAt(e.getX(), e.getY()));
			}
		});

		this.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				int key = e.getKeyCode();
				int modEx = e.getModifiersEx();
				boolean ctrlDown = (modEx & KeyEvent.CTRL_DOWN_MASK)
						== KeyEvent.CTRL_DOWN_MASK;
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
					if ( selectedCell!=null && ctrlDown ) {
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
				} else if ( key==KeyEvent.VK_Z && ctrlDown ) { // Ctrl-Z
					engine.undo();
					repaint();
					e.consume();
				} else if ( key==KeyEvent.VK_Y && ctrlDown ) { // Ctrl-Y
					engine.redo();
					repaint();
					e.consume();
				}
			}
			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {
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

	// returns the Cell which is at the given x,y.
	private Cell getCellAt(int x, int y) {
		int cx = (x - H_GAP) / COS; // also a lettuce, and a reserved word in maths. I can't imagine why. ____ the Romans!
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
	Grid getSudokuGrid() {
		return grid;
	}

	/** Set the grid. */
	void setSudokuGrid(Grid grid) {
		this.grid = grid;
		this.selectedCell = grid.cells[40]; // center cell 4*9+4 = 20
	}

	/** Set the aqua cell backgrounds. */
	void setAquaBGCells(Set<Cell> aquaCells) {
		this.auqaBGCells = aquaCells;
	}

	/** Set the red cell backgrounds. */
	void setRedBGCells(Set<Cell> redCells) {
		this.redBGCells = redCells;
	}

	/** Set the red cell backgrounds. */
	void setResult(Result result) {
		this.result = result;
	}

	/** Set the green potentials.
	 * NOTE that formerly green+red=orange. Now oranges are separate! */
	void setGreenPots(Pots pots) {
		COLOR_POTS[CI_GREEN] = pots;
	}

	/** Set the red potentials.
	 * NOTE that formerly green+red=orange. Now oranges are separate! */
	void setRedPots(Pots pots) {
		COLOR_POTS[CI_RED] = pots;
	}

	/** Set the orange potentials.
	 * NOTE that formerly green+red=orange. Now oranges are separate! */
	void setOrangePots(Pots pots) {
		COLOR_POTS[CI_ORANGE] = pots;
	}

	/** Set the blue potentials. Used in nested hint explanations. */
	void setBluePots(Pots pots) {
		COLOR_POTS[CI_BLUE] = pots;
	}

	/** Set the yellow potentials. Implemented for Coloring hints. */
	void setYellowPots(Pots pots) {
		COLOR_POTS[CI_YELLOW] = pots;
	}

	/** Set the purple potentials. Implemented for Coloring hints. */
	void setPurplePots(Pots pots) {
		COLOR_POTS[CI_PURPLE] = pots;
	}

	/** Set the brown potentials. Implemented for Coloring hints. */
	void setBrownPots(Pots pots) {
		COLOR_POTS[CI_BROWN] = pots;
	}

	/** Set the bases (the blues). */
	void setBases(Collection<ARegion> bases) {
		this.bases = bases;
	}

	/** Set the covers (the greens). */
	void setCovers(Collection<ARegion> covers) {
		this.covers = covers;
	}

	/** Set the links (brown arrows). */
	void setLinks(Collection<Link> links) {
		this.links = links;
	}

	/** Clears the selected and the focused cell. */
	void clearSelection(boolean isSelection) {
		if ( isSelection )
			this.selectedCell = null;
		this.focusedCell = null;
	}

	private void toggleGrey(Cell cell) {
		if ( greyBGCells==null ) {
			greyBGCells = new HashSet<>(16, 0.75F);
			greyBGCells.add(cell);
		} else if ( !greyBGCells.remove(cell) )
			greyBGCells.add(cell);
	}

	private void repaintCell(Cell cell) {
		if (cell == null)
			return;
		repaint(cell.x*COS+H_GAP, cell.y*COS+V_GAP, COS, COS);
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
		paintLegends(g); // Wally doesn't look happy.
		AffineTransform oldTransform = g2.getTransform();
		AffineTransform translate = AffineTransform.getTranslateInstance(
				H_GAP, V_GAP);
		g2.transform(translate);
		g.clearRect(0, 0, GRID_SIZE, GRID_SIZE);
		paintSelectionAndFocus(g);
		paintGrid(g);
		paintRegions(g, covers, COVER_BORDER_COLOR, COVER_BACKGOUND_COLOR);
		paintRegions(g, bases, BASE_BORDER_COLOR, BASE_BACKGROUND_COLOR);
		paintCellValues(g);
		paintLinks(g);
		g2.setTransform(oldTransform);
	}

	private void initGraphics(Graphics2D g2) {
		if (Settings.THE.get(Settings.isAntialiasing)) {
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
		for (int i=0; i<9; ++i) {
			// y-axis legend (vertical = rows)
			drawStringCentered(g, DIGITS[i+1]
					, H_GAP/2					// x
					, COS*i + V_GAP + COS/2		// y
			);
			// x-axis legend (horizontal = columns)
			drawStringCentered(g, LETTERS[i]
					, H_GAP + i*COS + COS/2		// x
					, COS*9 + V_GAP + H_GAP/2	// y
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
			else if ( greyBGCells!=null && greyBGCells.contains(cell) )
				col = COLOR_GREY;
			else if ( auqaBGCells!=null && auqaBGCells.contains(cell) )
				col = COLOR_AQUA;
			else if ( redBGCells!=null && redBGCells.contains(cell) )
				col = Color.RED;
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
	 * This is an intermediate step to translate the array of Cell=>Values into
	 * an array of Colors. We do this for efficiency: it's faster to iterate
	 * each of the Pots than it is to look-up each Cell=>Value in each Pots.
	 * There's 81*9 Cell Values by 7 Colors, so that's 5,103 HashMap.get()s to
	 * paint an empty Sudoku grid. Now we first ONCE run 7 HashMap.keySet() so
	 * ALL the subsequent however-many-gets are hits; then we execute 5,103
	 * array-lookups, and an array-lookup is MUCH faster than HashMap.get. So
	 * it's faster overall because it dramatically reduces the number of gets!
	 * Nothing But Net! The downside is memory: a matrix of 81*9 Color's. Meh!
	 * <p>NOTE: formerly green+red=orange. Now oranges are separate!
	 * @return
	 */
	private Color[][] getMaybesColors() {
		// clear all MAYBES_COLORS (a matrix[cellIndex][value-1])
		for ( int i=0; i<81; ++i )
			Arrays.fill(MAYBES_COLORS[i], null);
		// populate MAYBES_COLORS array from the array of Pots
		Pots pots;  Color color;
		Color[] cellsColors;
		for ( int i=0; i<NUM_COLOR_POTS; ++i ) {
			color = POTS_COLORS[i];
			if ( (pots=COLOR_POTS[i])!=null && !pots.isEmpty() )
				for ( Cell cell : pots.keySet() ) {
					cellsColors = MAYBES_COLORS[cell.i];
					// nb: array-iterator is MUCH faster than any Iterator
					for ( int value : Values.ARRAYS[pots.get(cell).bits] )
						// one-based-value to zero-based-array
						cellsColors[value-1] = color;
				}
		}
		return MAYBES_COLORS;
	}
	private static final Color[][] MAYBES_COLORS = new Color[81][9];

	// set the graphics-color to the color of the given Cell-value (ie maybe)
	// @return isHighlighted == color!=GRAY (the default)
	private boolean setMaybeColor(Graphics g, Cell cell, int value) {
		// one-based-value to zero-based-array
		Color c = MAYBES_COLORS[cell.i][value-1];
		g.setColor(c!=null ? c : Color.GRAY);
		return c!=null;
	}

	/**
	 * Paint each Cells value, or its maybes (potential values).
	 * @param g The Graphics to paint on.
	 */
	private void paintCellValues(Graphics g) {
		final int[][] VALUESES = Values.ARRAYS;
		final boolean isShowingMaybes = Settings.THE.get(Settings.isShowingMaybes);
		getMaybesColors(); // sets MAYBES_COLORS
		int x,y, cx,cy, i;
		boolean isHighlighted;
		for ( Cell cell : grid.cells ) {
			x = cell.x;  y = cell.y;
			if ( cell.value != 0 ) {
				cx = x*COS + CELL_PAD + CISo2;
				cy = y*COS + CELL_PAD + CISo2;
				//setCellValueColor(g, cell);
				g.setColor(cell==selectedCell?Color.BLACK:COLOR_ORANGY_BLACK);
				drawStringCentered(g, DIGITS[cell.value], cx, cy, bigFont);
			} else {
				// Paint potentials
				for ( int v : VALUESES[cell.maybes.bits] ) {
					i = v - 1;
					cx = x*COS + CELL_PAD + (i%3)*CISo3 + CISo6;
					cy = y*COS + CELL_PAD + (i/3)*CISo3 + CISo6;
					isHighlighted = setMaybeColor(g, cell, v);
					if ( result!=null && result.equals(cell, v) )
						drawStringCentered3D(g, DIGITS[v], cx,cy, smallFont3);
					else if ( isHighlighted )
						drawStringCentered3D(g, DIGITS[v], cx,cy, smallFont2);
					else if (isShowingMaybes) { // g.color is set to gray
						if ( cell == selectedCell
						  || (greyBGCells!=null && greyBGCells.contains(cell)) )
							g.setColor(Color.BLACK); // orangy(clr);
						drawStringCentered(g, DIGITS[v], cx,cy, smallFont1);
					}
				}
			}
		} // next x, y
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
					xPoints[0] = (int)(e.x + mx);
					xPoints[1] = (int)(rx + mx);
					xPoints[2] = (int)(lx + mx);
					yPoints[0] = (int)(e.y + my);
					yPoints[1] = (int)(ry + my);
					yPoints[2] = (int)(ly + my);
					g.fillPolygon(xPoints, yPoints, NUM_POINTS);
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
