/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Constants;
import static diuf.sudoku.utils.MyStrings.substring;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * This form converts between Javas RGB colors and HTML formatted colors. Its
 * not much use now because Netbeans color editor has a HTML tab, which is far
 * more advanced, but doing this is a nice way to explore JDK17s Swing frame
 * editor, so I am writing it anyway, just for fun. I purposely dedesignered
 * this form, because Javas form designer is still a piece of s__t.
 *
 * @author Keith Corlett 2022-01-08
 */
final class HtmlColor extends JFrame {

	private static final long serialVersionUID = 2754990001L;

	private static int beep() {
		Constants.beep();
		return 0;
	}

	private static String dec(final int i) {
		return Integer.toString(i);
	}
	private static int parseDec(final String s) {
		try {
			return Integer.parseInt(s); // base 10 is the default
		} catch(NumberFormatException ex) {
			return beep();
		}
	}

	private static String hex(final int i) {
		String s = Integer.toString(i, 16);
		while ( s.length() < 2 )
			s = "0"+s;
		return s.toUpperCase(); // "#FFFFFF" is easier to read than "#ffffff"
	}
	private static int parseHex(final String s) {
		try {
			return Integer.parseInt(s, 16); // hexadecimal
		} catch (NumberFormatException ex) {
			return beep();
		}
	}

	private static String htmlColor(final int r, final int grid, final int b) {
		return "#"+hex(r)+hex(grid)+hex(b);
	}

	private final int h, w; // height and width of pnlColor

    private JButton btnClose;
    private JLabel lblBlue;
    private JLabel lblGreen;
    private JLabel lblHtml;
    private JLabel lblRed;
    private JPanel pnlColor;
    private JTextField txtBlue;
    private JTextField txtGreen;
    private JTextField txtHtml;
    private JTextField txtRed;

	/**
	 * Creates new form HtmlColor
	 */
	public HtmlColor() {
		initComponents();
		h = pnlColor.getHeight();
		w = pnlColor.getWidth();
	}

	/**
	 * initComponents is called by the constructor to initialize the form.
	 */
	@SuppressWarnings({"unchecked", "deprecation"})
    private void initComponents() {

        lblRed = new JLabel();
        txtRed = new JTextField();
        lblGreen = new JLabel();
        txtGreen = new JTextField();
        lblBlue = new JLabel();
        txtBlue = new JTextField();
        lblHtml = new JLabel();
        txtHtml = new JTextField();
        btnClose = new JButton();
        pnlColor = new JPanel();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("HTML Colors");

        lblRed.setText("Red"); // NOI18N

        txtRed.setText("255");
        txtRed.setName("txtRed"); // NOI18N
        txtRed.addFocusListener(new FocusAdapter() {
			@Override
            public void focusLost(FocusEvent evt) {
                txtRedFocusLost(evt);
            }
        });

        lblGreen.setText("Green");

        txtGreen.setText("255");
        txtGreen.setName("txtGreen"); // NOI18N
        txtGreen.addFocusListener(new FocusAdapter() {
			@Override
            public void focusLost(FocusEvent evt) {
                txtGreenFocusLost(evt);
            }
        });

        lblBlue.setText("Blue");

        txtBlue.setText("255");
        txtBlue.setName("txtBlue"); // NOI18N
        txtBlue.addFocusListener(new FocusAdapter() {
			@Override
            public void focusLost(FocusEvent evt) {
                txtBlueFocusLost(evt);
            }
        });

        lblHtml.setText("HTML");

        txtHtml.setText("#AAAAAA");
        txtHtml.setName("txtHtml"); // NOI18N
        txtHtml.addFocusListener(new FocusAdapter() {
			@Override
            public void focusLost(FocusEvent evt) {
                txtHtmlFocusLost(evt);
            }
        });

        btnClose.setLabel("Close");
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        pnlColor.setBackground(new Color(204, 255, 255));
        pnlColor.setBorder(BorderFactory.createTitledBorder("color"));
        pnlColor.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
            public void mouseMoved(MouseEvent evt) {
                pnlColorMouseMoved(evt);
            }
        });

        GroupLayout pnlColorLayout = new GroupLayout(pnlColor);
        pnlColor.setLayout(pnlColorLayout);
        pnlColorLayout.setHorizontalGroup(
            pnlColorLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlColorLayout.setVerticalGroup(
            pnlColorLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRed, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtRed))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblGreen, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtGreen))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblBlue, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtBlue))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblHtml, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtHtml, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnClose)
                        .addGap(0, 167, Short.MAX_VALUE))
                    .addComponent(pnlColor, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(txtRed, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRed))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(lblGreen)
                            .addComponent(txtGreen, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(lblBlue)
                            .addComponent(txtBlue, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(lblHtml)
                            .addComponent(txtHtml, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addComponent(pnlColor, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(btnClose)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private void txtRedFocusLost(FocusEvent evt) {
        updateHtml();
    }

    private void txtGreenFocusLost(FocusEvent evt) {
        updateHtml();
    }

    private void txtBlueFocusLost(FocusEvent evt) {
        updateHtml();
    }

    private void txtHtmlFocusLost(FocusEvent evt) {
        updateRGB(txtHtml.getText());
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        dispose();
    }

    private void pnlColorMouseMoved(MouseEvent evt) {
        updateFromPanel(evt.getX(), evt.getY());
    }

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new HtmlColor().setVisible(true);
			}
		});
	}

	private void updateHtml() {
		final int r = parseDec(txtRed.getText());
		final int g = parseDec(txtGreen.getText());
		final int b = parseDec(txtBlue.getText());
		txtHtml.setText(htmlColor(r, g, b));
		pnlColor.setBackground(new Color(r, g, b));
	}

	private void updateRGB(final String s) {
		int r=0, g=0, b=0;
		if ( s != null ) {
			final String m = s.length()==6 && s.charAt(0)!='#' ? "#"+s : s;
			if ( (m.length()==7 && m.charAt(0)=='#') ) {
				r = parseHex(substring(m, 1,3));
				g = parseHex(substring(m, 3,5));
				b = parseHex(substring(m, 5,7));
			} else beep();
		} else beep();
		txtRed.setText(dec(r));
		txtGreen.setText(dec(g));
		txtBlue.setText(dec(b));
		pnlColor.setBackground(new Color(r, g, b));
	}

	// Use the mouse to set r,g,b in pnlColor, then
	// press the TAB key to update the HTML text.
	// WARN: This approach cannot produce colors high in both Green & Blue.
	private void updateFromPanel(final int x, final int y) {
		final int r = 255 * y / h;
		final int g = 255 * x / w;
		final int b = 255 - g;
		txtRed.setText(dec(r));
		txtGreen.setText(dec(g));
		txtBlue.setText(dec(b));
		pnlColor.setBackground(new Color(r, g, b));
	}

}
