/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.COL_OF;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.FIRST_COL;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Result;
import diuf.sudoku.Settings;
import static diuf.sudoku.Settings.THE_SETTINGS;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.als.Als;
import static diuf.sudoku.utils.Frmt.*;
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
import static diuf.sudoku.Grid.ROW_OF;
import static diuf.sudoku.Grid.SQRT;
import static diuf.sudoku.Grid.BY9;


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
public class SudokuGridPanel extends JPanel {

	private static final long serialVersionUID = 3709127163156966626L;

	private static final int N = REGION_SIZE; // 9 = num cells in region
	private static final int M = N - 1;		  // 8 = last cell index
	private static final int R = SQRT;		  // 3 = squareroot of REGION_SIZE

	// a cache of orangified colors.
	// No idea how big it should be, but 16 is big enough to start growing
	private static final Map<Color, Color> ORANGY_COLOR_CACHE = new HashMap<>(16);

	private static final Color COLOR_ORANGE = new Color(255, 185, 0);
	private static final Color COLOR_GREEN = new Color(0, 224, 0);
	private static final Color COLOR_BLUE = new Color(0, 0, 255);
	private static final Color COLOR_BLACK = new Color(0, 0, 0);
	private static final Color COLOR_PURPLE	= new Color(220, 5, 220);
	private static final Color COLOR_BROWN	= new Color(150, 75, 0);
	private static final Color COLOR_BLUE_DARK = Color.BLUE.darker();
	private static final Color COLOR_LEGEND	= new Color(0, 32, 64);
	private static final Color COLOR_BLUE_LIGHT	= new Color(216, 230, 255);

	// CYAN is for the ALS's only, everything else is "AQUA".
	private static final Color COLOR_CYAN = Color.CYAN;
	// use a darker cyan foreground color, else the digits "look funny"
	private static final Color COLOR_CYAN_DARK = COLOR_CYAN.darker();
	// the aqua (bluey green) foreground color
	private static final Color COLOR_AQUA = new Color(192, 255, 255);
	// the dark aqua foreground color
	private static final Color COLOR_AQUA_DARK = COLOR_AQUA.darker();
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
	// the purple cell background color
	private static final Color COLOR_PURPLE_BG = new Color(255, 204, 255, 153);
	// the light blue cell background color
	private static final Color COLOR_LIGHT_BLUE_BG = new Color(216, 230, 255, 24);
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
			, COLOR_BROWN
			, COLOR_CYAN // nb cyan instead of aqua which is too close to green
			, COLOR_PURPLE
			, COLOR_YELLOW_DARK
			, COLOR_BLUE_LIGHT
	};
	// ALS region bacground colors
	private static final Color[] ALS_BG_COLORS = {
			  COLOR_BASE_BG // blue
			, COLOR_COVER_BG // green
			, COLOR_BROWN_BG
			, COLOR_AQUA_BG
			, COLOR_PURPLE_BG
			, COLOR_YELLOW_BG
			, COLOR_LIGHT_BLUE_BG
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

	// LOWTERS is short for LOWERCASE_LETTERS, which are the column labels
	// from left-to-right, in lower-case, mapped to the right-mouse-button.
	private static final String[] LOWTERS = new String[] {
		"a", "b", "c", "d", "e", "f", "g", "h", "i"
	};

	/** The rectangle around each region, in cells. */
	private final Bounds[] bounds = new Bounds[NUM_REGIONS];

	private final ArrayList<Line> paintedLines = new ArrayList<>(64); //guess

	private final String FONT_NAME = "Verdana";

	// COS should be a product of 6: 42,48,54,60,66,72,78
	// BFIIK: 6 is maxWidth of the inter-region borders etc, which from a cells
	// perspective are just "border incursions", for which I must leave room.
	private int COS = 66;			// CELL_OUTER_SIZE // and a lettuce.
	private int CIS = COS - 6;		// CELL_INNER_SIZE // and a sibling?
	private int CISo2 = CIS / 2;
	private int CISo3 = CIS / 3;
	private int CISo6 = CIS / 6;
	private int V_GAP = 2;			// VERTICAL_GAP
	private int H_GAP = 42;			// HORIZONTAL_GAP
	private int CELL_PAD = (COS - CIS) / 2; // (64-58)/2=3
	private int MY_SIZE = COS * REGION_SIZE;
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
			= new Dimension(MY_SIZE+H_GAP+V_GAP, MY_SIZE+H_GAP+V_GAP);
	private final int BELOW_GRID = V_GAP + MY_SIZE; // top of column labels

	private Grid grid; // the current Sudoku grid to display.
	private final SudokuExplainer engine; // I "borrow" my parents engine
	private final SudokuFrame frame; // my parent component in the GUI

	private Cell focusedCell = null;
	private Cell selectedCell = null;

	private Set<Cell> auqaBGCells;
	private Set<Cell> pinkBGCells;
	private ARegion[] pinkos;
	private Set<Cell> redBGCells;
	private Set<Cell> greenBGCells;
	private Set<Cell> orangeBGCells;
	private Set<Cell> blueBGCells;
	private Set<Cell> yellowBGCells;
	// the cell-value to paint larger
	private Result theResult;
	// cells potential-values to paint larger
	private Pots results;
	private int resultColor;
	private ARegion[] bases;
	private ARegion[] covers;
	private Als[] alss;
	private Collection<Link> links;
	private Idx[][] supers;
	private Idx[][] subs;

	private final Font smallFont1;
	private final Font smallFont2;
	private final Font smallFont3;
	private final Font legendFont;
	private final Font bigFont;

	private Color fillBGColor = Color.WHITE;

	// y-axis (vertical) legend coordinates
	private int yAxisX;
	private final int[] yAxisY = new int[REGION_SIZE];

	// x-axis (horizontal) legend coordinates
	private final int[] xAxisX = new int[REGION_SIZE];
	private int xAxisY;

	// save multipling everything by COS all the time
	private final int[] byCOS = new int[VALUE_CEILING];

	// save multipling everything by COS all the time
	private final int[] positions = new int[REGION_SIZE];

	SudokuGridPanel(Grid grid, SudokuExplainer engine, SudokuFrame frame) {
		super();
		this.grid = grid;		// input
		this.engine = engine;	// process
		this.frame = frame;		// output
		initialise();
		super.setOpaque(false);
		smallFont1 = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE_1);
		smallFont2 = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_2);
		smallFont3 = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_3);
		legendFont = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_4);
		bigFont    = new Font(FONT_NAME, Font.BOLD, FONT_SIZE_5);
	}

	// set the COS, and the scale to then rescale everything-else in line with
	// the magnitide of the change in the COS, so that I (the programmer) only
	// have to pick a newCos and the other sizes change proportionally.
	private int changeCos(final int newCos) {
		final double oldCos = COS;
		scale = (double)newCos / oldCos;
		return COS = newCos;
	}
	private double scale;
	private int rescale(double d) {
		return (int)(d * scale + 0.5D);
	}

	private void rescaleDown() {
		// CELL_OUTER_SIZE should be a product of 6: 42,48,54,60,66,72,78
		// 6 is the max-width of the inter-region borders, which are NOT scaled
		// down coz I have no idea how. 48 is a bit cramped. 54 isn't too bad.
		COS = changeCos(54);
		// CELL_INNER_SIZE is - 6 to leave a gap of 3 each side of the cell
		CIS = COS - 6;
		CISo2 = CIS / 2;
		CISo3 = CIS / 3;
		CISo6 = CIS / 6;
		V_GAP = rescale(V_GAP);				// was 2
		H_GAP = rescale(H_GAP);				// was 42
		CELL_PAD = (COS - CIS) / 2;			// was 3
		MY_SIZE = COS * REGION_SIZE;		// was 576
		FONT_SIZE_1 = rescale(FONT_SIZE_1);
		FONT_SIZE_2 = rescale(FONT_SIZE_2);
		FONT_SIZE_3 = rescale(FONT_SIZE_3);
		FONT_SIZE_4 = rescale(FONT_SIZE_4);
		FONT_SIZE_5 = rescale(FONT_SIZE_5);
		ARROW_LENGTH = rescale(ARROW_LENGTH);
		ARROW_WIDTH = rescale(ARROW_WIDTH);
		LINK_OFFSET = rescale(LINK_OFFSET); // it's a double that should contain an int value!
		// MY_SIZE is CELL_OUTER_SIZE * number of cells per row/col.
		// + H_GAP + V_GAP is simplest AFAIK way to square-off a rectangle.
		// + CELL_PAD coz the math leaves me a tad too smal. BFIIK. I think the
		//   math is based on border centres, excluding widths.
		final int d = MY_SIZE + H_GAP + V_GAP + CELL_PAD;
		PREFFERED_SIZE = new Dimension(d, d);
	}

	private void createByCOS() {
		// save multipling everything by COS all the time
		for ( int i=0; i<VALUE_CEILING; ++i) {
			byCOS[i] = i * COS;
		}
	}

	private void createPositions() {
		for ( int i=0; i<REGION_SIZE; ++i) {
			positions[i] = byCOS[i] + CELL_PAD + CISo6;
		}
	}

	// we need calculate the positions of the legends ONCE, so that every time
	// we repaint we can just look-up the x and y of the x-axis legend and the
	// y-axis legend, to save CPU-time.
	// positionLegends uses COS, so run positionLegends AFTER rescaleDown!
	private void createLegendPositions() {
		// y-axis legend coordinates
		yAxisX = H_GAP / 2;
		for ( int i=0; i<REGION_SIZE; ++i ) {
			yAxisY[i] = V_GAP + byCOS[i] + COS/2;
		}
		// x-axis legend coordinates
		for ( int i=0; i<REGION_SIZE; ++i ) {
			xAxisX[i] = H_GAP + byCOS[i] + COS/2;
		}
		xAxisY = V_GAP + byCOS[N] + H_GAP/2;
	}

	// we need calculate the coordinates of the bounds of each region ONCE, so
	// that every time we repaint we need not multiply the cell-coordinates by
	// COS, to save CPU-time.
	// the Bounds constructor uses COS, so run createBounds AFTER rescaleDown!
	private void createBounds() {
		for ( int i=0; i<REGION_SIZE; ++i ) {
			bounds[i] = new Bounds(i%R*R, i/R*R, R, R);		// box
			bounds[i+REGION_SIZE] = new Bounds(0, i, N, 1);	// row
			bounds[i+FIRST_COL] = new Bounds(i, 0, 1, N);	// col
		}
	}

	private void initialise() {

		// reduce the cell-size if necessary
		if ( getToolkit().getScreenSize().height < 1080 ) { // was 750
			rescaleDown();
		}
		// these all use cell-size (et al) so must be run AFTER rescaleDown
		createByCOS();
		createPositions();
		createLegendPositions();
		createBounds();

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
						final int mod = e.getModifiersEx();
						final boolean alt = (mod & ALT_DOWN_MASK) == ALT_DOWN_MASK;
						final int cnt = e.getClickCount();
						final int btn = e.getButton();
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
						} else if ( cell.size == 1 ) {
							engine.setTheCellsValue(cell, VFIRST[cell.maybes]);
						} else {
							int v = getMaybeAt(e.getX(), e.getY());
							if ( v != 0 ) {
								if ( btn==BUTTON1 && mod==0 ) { // plain left click
									// cell.value := the value that was clicked-on
									if ( (cell.maybes & VSHFT[v]) != 0 )
										engine.setTheCellsValue(cell, v);
// Annoying when you're navigating around the grid with the mouse.
// Not annoying when you're trying to set a cells value and miss.
// There's more navigating than there is setting-and-missing, so it's out!
//									else
//										engine.beep();
								} else {
									// just remove/restore the maybe value
									engine.maybeTyped(cell, v);
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
							setGreenPots(engine.cheat(LETTERS[i], grid));
							e.consume();
							repaint();
						}
					} else { // right/middle button
						final int i = (e.getX()-H_GAP) / COS;
						if ( i>-1 && i<LOWTERS.length ) {
							setGreenPots(engine.cheat(LOWTERS[i], grid));
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
					if ( v>0 && v<VALUE_CEILING ) {
						if ( e.getButton() == BUTTON1 ) // left
							setGreenPots(grid.getCandidatePots(v));
						else if ( v<REGION_SIZE || engine.cheater ) //right/mid
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
				// first get the cell over which the mouse is located
				final Cell c = getCellAt(e.getX(), e.getY());
				// the default cheatName is "" to clear it when cheats aren't
				// enabled or the mouse is elsewhere over the grid
				String cheatName = "";
				// if c is null then we might be below the grid, so
				if ( c == null ) {
					// if cheats are authorised and we are below the grid then
					if ( engine.cheater && e.getY()>BELOW_GRID ) {
						// fetch the name of this cheat
						cheatName = engine.cheatName(e.getX() / COS);
					}
				} else { // paint cell over which mouse is located yellow
					setFocusedCell(c);
				}
				// display the cheatName (if any) next to the view combo
				frame.getLblCheatName().setText(cheatName);
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
						case KeyEvent.VK_LEFT:  x=x==0 ? M : (x+M)%N; break;
						case KeyEvent.VK_RIGHT: x=x==M ? 0 : (x+1)%N; break;
						case KeyEvent.VK_UP:    y=y==0 ? M : (y+M)%N; break;
						case KeyEvent.VK_DOWN:  y=y==M ? 0 : (y+1)%N; break;
						}
						setSelectedCell(grid.cells[BY9[y]+x]);
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
	}

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
		final int cx = (x - H_GAP) / COS;
		if(cx<0||cx>8) return null;
		final int cy = (y - V_GAP) / COS;
		if(cy<0||cy>8) return null;
		return grid.cells[BY9[cy]+cx];
	}

	private boolean in(final int i, final int min, final int ceiling) {
		return i>=min && i<ceiling;
	}

	// returns the potential value that is at (or should be at) the given x,y;
	// converting a screen-mouse-click into a cells potential value
	private int getMaybeAt(int x, int y) {
		// get cell's top-left corner
		final int cx = (x - H_GAP) / COS;
		final int cy = (y - V_GAP) / COS;
		if ( cx<0||cx>8 || cy<0||cy>8 )
			return 0;
		assert in(BY9[cy]+cx, 0, 81);
		// substract cell's top-left corner
		x = x - byCOS[cx] - H_GAP;
		y = y - byCOS[cy] - V_GAP;
		// get the maybe from position in cell
		final int mx = (x - CELL_PAD) / CISo3;
		final int my = (y - CELL_PAD) / CISo3;
		if ( mx<0||mx>2 || my<0||my>2 )
			return 0;
		return my*3 + mx + 1; // the potential value
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
	void setPinkos(ARegion[] pinkRegions) {
		this.pinkos = pinkRegions;
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
		this.theResult = result;
	}

	/** Set results: cell potential values to paint larger. */
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
	void setBases(ARegion[] bases) {
		this.bases = bases;
	}

	/** Set the covers (the greens). */
	void setCovers(ARegion[] covers) {
		this.covers = covers;
	}

	/** Set the brown potentials. Implemented for Coloring hints. */
	void setAlss(Als[] alss) {
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
	 * Clears focusedCell, and if isSelection then also clears selectedCell.
	 */
	void clearSelection(boolean isSelection) {
		this.focusedCell = null;
		if ( isSelection )
			this.selectedCell = null;
	}

	private void repaintCell(Cell cell) {
		if ( cell != null )
			repaint(byCOS[cell.x]+H_GAP, byCOS[cell.y]+V_GAP, COS, COS);
	}

	// FYI: called setFocusedCellById because setFocusedCell(null)
	//      is a compiler error when setFocusedCell is overloaded
	public void setFocusedCellById(String id) {
		if ( id == null ) // nb: id is never empty
			setFocusedCell(null);
		else
			setFocusedCell(grid.get(id));
	}

	private void setFocusedCell(Cell cell) {
		repaintCell(this.focusedCell); // clear existing
		this.focusedCell = cell;
		repaintCell(this.focusedCell);
	}

	private void setSelectedCell(Cell cell) {
		repaintCell(this.selectedCell); // clear existing
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

	private static void drawStringCentered(final Graphics g, final String s, final int x, final int y) {
		final Rectangle2D r = g.getFontMetrics().getStringBounds(s, g);
		final double px = x - r.getWidth()/2 + 0.5;
		final double py = y - r.getHeight()/2 - r.getY() + 0.5;
		g.drawString(s, (int)px, (int)py);
	}

	private static void drawStringCentered(final Graphics g, final String s, final int x, final int y, final Font f) {
		g.setFont(f);
		drawStringCentered(g, s, x, y);
	}

	private static void drawStringCentered(final Graphics g, final String s, final int x, final int y, final Color c) {
		g.setColor(c);
		drawStringCentered(g, s, x, y);
	}

	private static void drawStringCentered3D(final Graphics g, final String s, final int x, final int y, final Font f) {
		g.setFont(f);
		final Color theGivenColor = g.getColor();
		drawStringCentered(g, s, x,   y+1, Color.black);
		drawStringCentered(g, s, x-1, y,   Color.yellow);
		drawStringCentered(g, s, x,   y,   theGivenColor);
	}

	private static Color orangy(Color col) {
		Color orangy = ORANGY_COLOR_CACHE.get(col);
		if ( orangy != null )
			return orangy;
		orangy = new Color(
			// my orange = (255, 185, 0) that's (R, G, B)
			  (col.getRed()   + 255) / 2
			, (col.getGreen() + 185) / 2
			, (col.getBlue()  +   0) / 2
		);
		ORANGY_COLOR_CACHE.put(col, orangy);
		return orangy;
	}

	/** Paint this JPanel. */
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		initGraphics(g2);
		paintLegends(g);
		AffineTransform preTrans = g2.getTransform();
		AffineTransform translate = AffineTransform.getTranslateInstance(
				H_GAP, V_GAP);
		g2.transform(translate);
		g.clearRect(0, 0, MY_SIZE, MY_SIZE);
		paintSelectionAndFocus(g);
		paintGrid(g);
		if ( alss != null ) {
			// paint the background of ALS cells only
			paintAlss(g, alss);
		} else {
			// paint regions for non-ALS's
			paintRegions(g, covers, COLOR_COVER_BORDER, COLOR_COVER_BG);
			paintRegions(g, bases, COLOR_BASE_BORDER, COLOR_BASE_BG);
			paintRegions(g, pinkos, COLOR_PINK, null);
		}
		paintCell(g);
		paintSuperAndSubMarkers(g);
		paintLinks(g);
		g2.setTransform(preTrans);
	}

	private void initGraphics(Graphics2D g2) {
		if ( THE_SETTINGS.getBoolean(Settings.isAntialiasing) ) {
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
		for ( int i=0; i<REGION_SIZE; ++i ) {
			// y-axis legend (vertical = row labels: 1..9)
			drawStringCentered(g, DIGITS[i+1], yAxisX, yAxisY[i]);
			// x-axis legend (horizontal = column labels: A..I)
			drawStringCentered(g, LETTERS[i], xAxisX[i], xAxisY);
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
			return; // trying to stop these running into each other
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
			Color c = fillBGColor;
			if ( cell == selectedCell )
				c = Color.ORANGE;
			else if ( cell == focusedCell )
				c = Color.YELLOW;
			else if ( auqaBGCells!=null && auqaBGCells.contains(cell) )
				c = COLOR_AQUA;
			else if ( pinkBGCells!=null && pinkBGCells.contains(cell) )
				c = COLOR_PINK;
			else if ( redBGCells!=null && redBGCells.contains(cell) )
				c = Color.RED;
			else if ( blueBGCells!=null && blueBGCells.contains(cell) )
				c = COLOR_BLUE_BG;
			else if ( greenBGCells!=null && greenBGCells.contains(cell) )
				c = COLOR_GREEN_BG;
			else if ( orangeBGCells!=null && orangeBGCells.contains(cell) )
				c = COLOR_ORANGE_BG;
			else if ( yellowBGCells!=null && yellowBGCells.contains(cell) )
				c = COLOR_YELLOW_BG;
			g.setColor(c);
			g.fillRect(byCOS[cell.x], byCOS[cell.y], COS, COS);
		}
	}

	private void paintGrid(Graphics g) {
		int lineWidth, offset;
		for ( int i=0; i<VALUE_CEILING; ++i ) {
			if ( i % 3 == 0 ) {
				lineWidth = 4;
				g.setColor(Color.black);
			} else {
				lineWidth = 2;
				g.setColor(COLOR_BLUE_DARK);
			}
			offset = lineWidth / 2;
			g.fillRect(byCOS[i]-offset, 0-offset, lineWidth, MY_SIZE+lineWidth);
			g.fillRect(0-offset, byCOS[i]-offset, MY_SIZE+lineWidth, lineWidth);
		}
	}

	private void paintRegions(
			  final Graphics g
			, final ARegion[] regions
			, final Color borderColor
			, final Color backgroundColor // null means no shading
	) {
		if ( regions != null ) {
			for ( ARegion region : regions ) {
				if ( region == null )
					continue;
				final Bounds b = bounds[region.index];
				g.setColor(borderColor); // solid color
				for ( int s=-2; s<3; s++ ) // 5 times: -2, -1, 0, 1, 2
					g.drawRect(b.x+s, b.y+s, b.w-s*2, b.h-s*2);
				if ( backgroundColor != null ) {
					g.setColor(backgroundColor); // shading (alpha)
					g.fillRect(b.x+3, b.y+3, b.w-6, b.h-6);
				}
			}
		}
	}

	private boolean isSelected(final Als als) {
		final int sai = selectedAlsIndex;
		if ( sai > -1 ) {
			if ( sai < alss.length ) {
				if ( als == alss[sai] ) {
					return true;
				}
			} else { // clear invalid
				selectedAlsIndex = -1;
			}
		}
		return false;
	}

	/**
	 * Paint the given ALS using color 'i'.
	 *
	 * @param g the Graphics to paint on
	 * @param als the ALS to paint
	 * @param c the color index, for ALS_COLORS/ALS_BG_COLORS, ie
	 *  {@code alsIndex % ALS_COLORS.length} to avert AIOOBE;
	 *  or special case -1 to "flash highlight" this ALS in YELLOW.
	 */
	private void paintAls(final Graphics g, final Als als, final int c) {
		// the foreGroundColor switches-up to yellow when c is -1 (selected)
		final Color fgc = c==-1 ? Color.YELLOW : ALS_COLORS[c];
		// Draw ALS border inside the black region border, in a finer pen. The
		// problem is avoiding overpaints, so draw INSIDE da region border, but
		// region borders are not all the same width, so compromise by partly
		// overPainting the thicker-black border between boxs, but MUST start
		// at 1 to catch the edge of grey border between cols/rows, so for a
		// narrower line retard terminus rather than advancing origin.
		// FYI: Draw inside da region border to reduce overpainting. It used to
		// paint over the whole thicker-black-inter-box-border, but then we see
		// only the last border at each intersection between ALS regions, which
		// are many, so now we avert the whole problem, so that the user sees a
		// pointer to each ALS, whose cells may still overlap, so da user still
		// needs to select each ALS to examine it, so this just helps not Cures
		// the problem. Road to Nowhere. Plantpot. Chapman's Bridge. Desperado.
		// Dueling Dalton. Dueling Bangos! 42. Sigh.
		final ARegion region = als.region;
		if ( region != null ) {
			final Bounds b = bounds[region.index];
			g.setColor(fgc); // solid color
			for ( int s=1; s<4; ++s ) // thrice: 1, 2, 3
				g.drawRect(b.x+s, b.y+s, b.w-s*2, b.h-s*2);
		}
		// paint each cell in this ALS
		for ( Cell cell : als.idx.cellsNew(grid.cells) ) {
			// paint the cell background
			g.setColor(c==-1 ? Color.YELLOW : ALS_BG_COLORS[c]);
			g.fillRect(byCOS[cell.x]+2, byCOS[cell.y]+2, COS-4, COS-4);
			// use a darker aqua foreground color, else the digits "look funny"
			// but retain aqua border-color to match the hint-html-text, which
			// is strange, but works. I should move aqua away from green. Sigh.
			g.setColor(fgc==COLOR_CYAN ? COLOR_CYAN_DARK : fgc);
			// paint the cell foreground
			for ( int v : VALUESES[cell.maybes] ) {
				paintMaybe3D(g, cell, v, smallFont2);
			}
		}
	}

	/**
	 * paint the ALSs, if any, in me the SudokuGridPanel. Note that alss may be
	 * null, in which case I am never called, but it should never be empty.
	 *
	 * @param g
	 * @param alss
	 */
	private void paintAlss(final Graphics g, Als[] alss) {
		// If you're seeing this then add another ALS_COLORS and ALS_BG_COLORS.
		if ( alss.length > ALS_COLORS.length )
			Log.println("WARN: paintAlss: more alss than ALS_COLORS!");
		int i = 1; // the alss index, skip the first ALS, for now.
		for ( ; i<alss.length; ++i ) {
			// the last ALS may be null. sigh.
			if ( alss[i] == null )
				break;
			paintAls(g, alss[i], i % ALS_COLORS.length);
		}
		// paint the first ALS last, so that the first ALS is always on top,
		// and the last ALS is the one underneath it... they rarely overlap.
		paintAls(g, alss[0], 0);

		// paint the selected one again!
		i = selectedAlsIndex;
		if ( i > -1 ) {
			// paint cells of the selectedAls YELLOW
			paintAls(g, alss[i], -1);
			// wait a while then repaint alss again "normally".
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try{Thread.sleep(333);}catch(InterruptedException eaten){}
					selectedAlsIndex = -1;
					repaint();
				}
			});
		}
		// if any ALS is selected then
		// nb: I don't understand this, and I wrote it. GUI's just shit me!
		if ( i > -1 ) {
			if ( i >= alss.length ) {
				// selection invalid, so unselect it
				selectedAlsIndex = -1;
			} else {
				// repaint the selected ALS over the top of the others.
				paintAls(g, alss[i], i % ALS_COLORS.length);
			}
		}
	}

	/**
	 * Double-click on the ALS-ID (ie the a in (a)) in the hint detail area to
	 * repaint this ALS over the others, so that you can see the bastard.
	 *
	 * @param i the index of this als in hint.alss
	 */
	void selectAls(final int i) {
		if ( i>-1 && i<alss.length ) {
			selectedAlsIndex = i;
		} else {
			selectedAlsIndex = -1;
		}
		repaint();
	}
	private int selectedAlsIndex = -1; // NONE

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
		for ( int i=0; i<GRID_SIZE; ++i )
			Arrays.fill(MAYBES_COLORS[i], null);
		// populate the MAYBES_COLORS array from the Pots array
		Pots pots;  Color color;
		Color[] cellsMaybesColor;
		for ( int i=0; i<NUM_COLOR_POTS; ++i ) {
			color = POTS_COLORS[i];
			if ( (pots=COLOR_POTS[i]) != null ) {
				assert !pots.isEmpty() : "empty pots denied, for speed!";
				for ( java.util.Map.Entry<Cell,Integer> e : pots.entrySet() ) {
					cellsMaybesColor = MAYBES_COLORS[e.getKey().i];
					//nb: INDEXES[i] is VALUES[i] with one removed from each v,
					//to cater for a one-based-value in a zero-based-array.
					//Just beware that the returned v is acually vMinus1!
					for ( int vMinus1 : INDEXES[e.getValue()] )
						cellsMaybesColor[vMinus1] = color;
				}
			}
		}
	}
	// first index is indice 0..80, second index is value-1 0..8.
	private static final Color[][] MAYBES_COLORS
			= new Color[GRID_SIZE][REGION_SIZE];

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

	// paint the non-zero value of this cell.
	// paint* methods are instance wrappers for the static draw methods.
	// paint* exist to encapsulate the math that works-out the x and y.
	// It now also encapsulates working-out color for the un/selected cell.
	private void paintCellValue(final Graphics g, final Cell c) {
		g.setColor(c==selectedCell ? Color.BLACK : COLOR_ORANGY_BLACK);
		drawStringCentered(g, DIGITS[c.value]
				, byCOS[c.x] + CELL_PAD + CISo2
				, byCOS[c.y] + CELL_PAD + CISo2
				, bigFont);
	}

	// paint a 3D (highlighted) cell-maybe in the current color using 'font'.
	// paint* methods are instance wrappers for the static draw methods.
	// paint* exist to encapsulate the math that works-out the x and y.
	private void paintMaybe3D(final Graphics g, final Cell c, final int v, final Font font) {
		drawStringCentered3D(g, DIGITS[v]
			, byCOS[c.x] + CELL_PAD + ((v-1)%3)*CISo3 + CISo6
			, byCOS[c.y] + CELL_PAD + ((v-1)/3)*CISo3 + CISo6
			, font
		);
	}

	// paint a "plain" cell-maybe in the current color.
	// paint* methods are instance wrappers for the static draw methods.
	// paint* exist to encapsulate the math that works-out the x and y.
	private void paintMaybe(final Graphics g, final Cell c, final int v) {
		drawStringCentered(g, DIGITS[v]
			, byCOS[c.x] + CELL_PAD + ((v-1)%3)*CISo3 + CISo6
			, byCOS[c.y] + CELL_PAD + ((v-1)/3)*CISo3 + CISo6
			, smallFont1
		);
	}

	/**
	 * Paint each Cells value, or its maybes (potential values) in forground.
	 * Note that I do NOT clear the cell before I paint it, ergo I overwrite
	 * the pre-existing background.
	 *
	 * @param g The Graphics to paint on.
	 */
	private void paintCell(Graphics g) {
		Integer values;
		boolean isHighlighted;
		final boolean show = THE_SETTINGS.getBoolean(Settings.isShowingMaybes);
		initMaybesColors(); // sets MAYBES_COLORS
		for ( Cell cell : grid.cells ) {
			if ( cell.value != 0 ) {
				paintCellValue(g, cell);
			} else {
				for ( int v : VALUESES[cell.maybes] ) {
					isHighlighted = setMaybeColor(g, cell.i, v-1);
					if ( theResult!=null && theResult.equals(cell, v) )
						paintMaybe3D(g, cell, v, smallFont3); // larger font
					else if ( results != null
						   && (values=results.get(cell)) != null
						   && (values & VSHFT[v]) != 0 ) {
						g.setColor(resultColor()); // result ON's green/blue
						paintMaybe3D(g, cell, v, smallFont3); // larger font
					} else if ( isHighlighted ) {
						paintMaybe3D(g, cell, v, smallFont2);
					} else if (show) { // g.color is gray
						paintMaybe(g, cell, v);
					}
				}
			}
		}
	}

	// Super and Sub marker colors
	private static final Color[] SS_COLORS = {COLOR_GREEN, COLOR_BLUE};

	// paint supers and subs, if any
	// I overwrite whatevers in the background, so you can paint me over the
	// cell's maybes produced by paintCell
	private void paintSuperAndSubMarkers(Graphics g) {
		if ( supers==null || subs==null )
			return;
		g.setFont(smallFont2);
		for ( int c=0; c<2; ++c ) { // GREEN, BLUE
			g.setColor(SS_COLORS[c]);
			for ( int v=1; v<VALUE_CEILING; ++v ) {
				if ( subs[c][v].any() )
					paintMarkers(g, v, subs[c][v], MINUS, 8);
				if ( supers[c][v].any() )
					paintMarkers(g, v, supers[c][v], PLUS, 10);
			}
		}
		// over-paint RED any that're in both sub colors.
		g.setColor(Color.RED);
		for ( int v=1; v<VALUE_CEILING; ++v )
			if ( tmp.setAndAny(subs[0][v], subs[1][v]) )
				paintMarkers(g, v, tmp, MINUS, 8);
	}
	private final Idx tmp = new Idx();

	// draw String s centered on v in each cell in idx
	private void paintMarkers(Graphics g, int v, Idx idx, String s, int offset) {
		final int u = v - 1;
		final int xo = CELL_PAD + (u%R)*CISo3 + CISo6 + offset; // xOffset
		final int yo = CELL_PAD + (u/R)*CISo3 + CISo6; // yOffset
		// note: x,y are the centre point to paint at (not the top-left)
		idx.forEach((i) -> drawStringCentered(g, s, byCOS[i%N]+xo, byCOS[i/N]+yo));
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
		// vectorial product, without normalization by length
		private int distanceUnscaled(int x, int y) {
			return (x - sx)*(ey - sy) - (y - sy)*(ex - sx);
		}
		private boolean intervalOverlaps(int s1, int e1, int s2, int e2) {
			if (s1 > e1) { // swap
				s1 ^= e1;
				e1 = s1^e1;
				s1 ^= e1;
			}
			if (s2 > e2) { // swap
				s2 ^= e2;
				e2 = s2^e2;
				s2 ^= e2;
			}
			return s1 < e2 && e1 > s2;
		}
		// basically, if there MBR's overlap then test for overlap
		private boolean overlaps(Line o) {
			if ( distanceUnscaled(o.sx, o.sy) == 0
			  && distanceUnscaled(o.ex, o.ey) == 0 ) {
				// both lines are on the same right
				return intervalOverlaps(sx, ex, o.sx, o.ex)
					|| intervalOverlaps(sy, ey, o.sy, o.ey);
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
	// NB: mutates DblPoint's state, only the reference is final.
	private void position(final int i, final int v, final DblPoint p) {
		final int x = COL_OF[i];
		final int y = ROW_OF[i];
		if ( v > 0 ) {
			final int u = v - 1;
			p.x = positions[x] + u%3*CISo3;
			p.y = positions[y] + u/3*CISo3;
		} else {
			p.x = positions[x] + CISo3;
			p.y = positions[y] + CISo3;
		}
	}

	private void paintLinks(Graphics g) {
		if ( links==null || links.size()==0 )
			return;
		g.setColor(Color.orange);
		DblPoint s=new DblPoint(), e=new DblPoint(); // s=source, e=end
		Line myLine;
		// s=shift, u=unit, m=move, l=left, r=right
		// nb: pretty sure we don't actually need a couple of these variables,
		// but I'm a human being who is a poor mathematician, so I do, so I'm
		// keeping them, even if they're not actually required.
		double sx,sy, length, ux,uy, mx,my, lx,ly, rx,ry;
		int overlaps; // number of already painted lines I cross
		for ( Link link : links ) {
			position(link.srcIndice, link.srcValue, s); // source point
			position(link.endIndice, link.endValue, e); // end point
			// get unit vector
			sx = e.x - s.x; // shiftX = end.x - start.x
			sy = e.y - s.y; // shiftY = end.y - start.y
			// PYTHONS RULE: the length of the hippopotamus equals the square
			// root of the sum of the square of the other two sides. Ya can't
			// fool me mate! Hippopotami are round. I means they're da roundest
			// animal around, on ground. And then I shot'im.
			length = Math.sqrt( sx*sx + sy*sy );
			ux = sx / length; // the amount we move horizontaly per unit
			uy = sy / length; // the amount we move vertically per unit
			// build my Line, and count number of overlapping lines
			myLine = new Line((int)s.x, (int)s.y, (int)e.x, (int)e.y);
			overlaps = 0;
			for ( Line paintedLine : paintedLines )
				if ( myLine.overlaps(paintedLine) )
					++overlaps;
			// move the line perpendicularly to go away from overlapping lines
			mx = uy * ((overlaps+1)/2) * LINK_OFFSET; // horizontally
			my = ux * ((overlaps+1)/2) * LINK_OFFSET; // vertically
			if (overlaps % 2 == 0)
				mx = -mx; // negate the x to move by
			else
				my = -my; // negate the y to move by
			// suppress arrows on short links, coz there ain't room for em.
			if ( length >= (CIS>>1) ) { // length >= CELL_INNER_SIZE/2
				// truncate end points (move of "middle" of potential value)
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
				paintedLines.add(myLine);
			}
			// draw the Line
			g.drawLine(
					  (int)(s.x + mx)
					, (int)(s.y + my)
					, (int)(e.x + mx)
					, (int)(e.y + my)
			);
		} // next link
		paintedLines.clear(); // let gc cleanup the Lines but keep the container
	}

	/**
	 * The (x (left), y (top), width, height) of a region, in cells.
	 * <p>
	 * Inputs are in cells, values stored and in screen-coordinates, to save
	 * repeatedly multiplying by the cell outer size (COS).
	 * <p>
	 * The Bounds class is only used in {@link #paintRegions}, it's just a way
	 * of calculating ONCE and storing the outline of each ARegion.
	 */
	private final class Bounds {
		public final int x, y, w, h;
		public Bounds(final int x, final int y, final int w, final int h) {
			this.x = byCOS[x];
			this.y = byCOS[y];
			this.w = byCOS[w];
			this.h = byCOS[h];
		}
		@Override
		public boolean equals(Object o) {
			return o!=null && (o instanceof Bounds) && equals((Bounds)o);
		}
		@Override
		public int hashCode() {
			return (w<<12) ^ (h<<10) ^ (x<<5) ^ y ;
		}
		public boolean equals(final Bounds o) {
			return x==o.x && y==o.y && w==o.w && h==o.h;
		}
		@Override
		public String toString() {
			return "x="+x+", y="+y+", w="+w+", h="+h;
		}
	}

}
