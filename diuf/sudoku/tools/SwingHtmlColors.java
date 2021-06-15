/*
 * This is open source software. Use it. Change it. Any problems
 * are yours, not mine. This software is published without warranty,
 * including the implied warranty of merchantability. All care, but
 * no responsibility.
 */
package diuf.sudoku.tools;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * SwingHtmlColors is a JFrame that translates between Swing and HTML Colors.
 * It also allows you to select/adjust colors with the sliders, and copy the
 * Swing or HTML representation to the clipboard.
 * <p>
 * This tool helps to paint a color in Swing and HTML, which is pretty common
 * in Sudoku Explainer.
 * <p>
 * I don't do alpha: the fourth Color parameter is accepted but ignored.
 * Enhance me if you like: make parseSwing respect the fourth parameter,
 * and setter, and so on. I haven't bothered because I don't need it.
 *
 * @author Keith Corlett 2021-05-08
 */
public class SwingHtmlColors extends javax.swing.JFrame {
	
	private static final long serialVersionUID = 485834945059L;

	private int red = 255;
	private int green = 255;
	private int blue = 255;

	public int getRed() { return red; }
	public void setRed(int red) { setColor(red, green, blue); }

	public int getGreen() { return green; }
	public void setGreen(int green) { setColor(red, green, blue); }

	public int getBlue() { return blue; }
	public void setBlue(int blue) { setColor(red, green, blue); }

	/**
	 * The guts of the setters: remembers the r, g, b parameters,
	 * and displays that color in all the controls.
	 *
	 * @param red the red value 0..255
	 * @param green the green value 0..255
	 * @param blue the blue value 0..255
	 */
	private void setColor(int red, int green, int blue) {
		redSlider.setValue(this.red = red);
		greenSlider.setValue(this.green = green);
		blueSlider.setValue(this.blue = blue);
		setTxtSwingColor();
		setTxtHtmlColor();
		setPnlColor();
	}

	private void setTxtSwingColor() {
		txtSwingColor.setText("Color("+red+", "+green+", "+blue+")");
	}

	private void setTxtHtmlColor() {
		txtHtmlColor.setText("\"#"+hex(red)+hex(green)+hex(blue)+"\"");
	}

	/**
	 * Format the given value into 2 character hexadecimal.
	 *
	 * @param value
	 * @return
	 */
	private static String hex(int value) {
		assert value >= 0 && value <= 255;
		if ( value < 16 )
			return "0" + Integer.toHexString(value);
		return Integer.toHexString(value);
	}

	private void setPnlColor() {
		pnlColor.setBackground(new Color(red, green, blue));
	}

	/**
	 * Creates new form MainFrame
	 */
	public SwingHtmlColors() {
		initComponents();
		setTxtSwingColor();
		setTxtHtmlColor();
		txtSwingColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parseSwing(e.getActionCommand());
			}
		});
		txtHtmlColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parseHtml(e.getActionCommand());
			}
		});
	}

	/**
	 * Parse the given Swing Color into r, g, b
	 * and display the bastard.
	 *
	 * @param swing
	 */
	private void parseSwing(String swing) {
		// remove leading and trailing junk, leaving 3 (or 4) ints
		swing = swing.replaceFirst("^.*[Cc][Oo][Ll][Oo][Rr]\\(", "")
				     .replaceFirst("\\).*$", "");
		// split the String on commas
		String[] fields = swing.split(", *");
		// we need atleast 3 fields
		if ( fields.length > 2 )
			try {
				int r, g, b;
				r = Integer.valueOf(fields[0]);
				g = Integer.valueOf(fields[1]);
				b = Integer.valueOf(fields[2]);
				setColor(r, g, b);
			} catch ( NumberFormatException ex ) {
				System.out.println(ex);
				beep();
			}
		else
			beep(); // tell the user it ____ed-up
	}

	/**
	 * Parse the given HTML Color into r, g, b
	 * and display the bastard.
	 *
	 * @param html
	 */
	private void parseHtml(String html) {
		// remove leading and trailing junk, leaving 3 hexadecimals
		html = html.replaceFirst("^.*#", "")
				   .replaceFirst("[^0-9A-Fa-f]*$", "");
		// split html into fields of 2 chars each
		final List<String> fields = split(html, 2);
		// we need atleast 3 fields
		if ( fields.size() > 2 )
			try {
				final int r, g, b;
				r = Integer.parseInt(fields.get(0), 16);
				g = Integer.parseInt(fields.get(1), 16);
				b = Integer.parseInt(fields.get(2), 16);
				setColor(r, g, b);
			} catch ( NumberFormatException ex ) {
				System.out.println(ex);
				beep();
			}
		else
			beep(); // tell the user it ____ed-up
	}

	/**
	 * Splits the String s into substrings of fieldLength each, and returns
	 * them in a new ArrayList.
	 *
	 * @param s
	 * @param fieldLength
	 * @return
	 */
	private static List<String> split(final String s, final int fieldLength) {
		assert fieldLength > 0 && fieldLength < 256;
		final List<String> result;
		final int length = s.length();
		if ( length > fieldLength ) {
			// there are multiple fields in the s parameter
			result = new ArrayList<>(length/fieldLength);
			for ( int begin=0,end; begin<length; begin+=fieldLength ) {
				end = begin + fieldLength;
				if ( end > length ) // the last field
					result.add(s.substring(begin));
				else
					result.add(s.substring(begin, end));
			}
		} else {
			// there's only one field in the s parameter
			result = new ArrayList<>(1);
			result.add(s);
		}
		return result;
	}

	public static void beep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"}) // Binding
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        pnlColor = new javax.swing.JPanel();
        txtSwingColor = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtHtmlColor = new javax.swing.JTextField();
        redSlider = new javax.swing.JSlider();
        greenSlider = new javax.swing.JSlider();
        blueSlider = new javax.swing.JSlider();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Swing HTML Colors");
        setBackground(new java.awt.Color(255, 255, 255));

        pnlColor.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setText("Swing");

        jLabel2.setText("HTML");

        javax.swing.GroupLayout pnlColorLayout = new javax.swing.GroupLayout(pnlColor);
        pnlColor.setLayout(pnlColorLayout);
        pnlColorLayout.setHorizontalGroup(
            pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlColorLayout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtSwingColor)
                    .addComponent(txtHtmlColor))
                .addContainerGap())
        );
        pnlColorLayout.setVerticalGroup(
            pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlColorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSwingColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtHtmlColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(42, Short.MAX_VALUE))
        );

        redSlider.setForeground(new java.awt.Color(255, 0, 0));
        redSlider.setMaximum(255);
        redSlider.setName("redSlider"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${red}"), redSlider, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        greenSlider.setForeground(new java.awt.Color(0, 255, 0));
        greenSlider.setMaximum(255);
        greenSlider.setName("blueSlider"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${green}"), greenSlider, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        blueSlider.setForeground(new java.awt.Color(0, 0, 255));
        blueSlider.setMaximum(255);
        blueSlider.setName("blueSlider"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${blue}"), blueSlider, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        jLabel3.setForeground(new java.awt.Color(255, 0, 0));
        jLabel3.setText("red");

        jLabel4.setForeground(new java.awt.Color(0, 255, 0));
        jLabel4.setText("green");

        jLabel5.setForeground(new java.awt.Color(0, 0, 255));
        jLabel5.setText("blue");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlColor, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(blueSlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
                            .addComponent(greenSlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(redSlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(redSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(greenSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(blueSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
//		/* Set the Nimbus look and feel */
//		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
//		 */
//		try {
//			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//				if ("Nimbus".equals(info.getName())) {
//					javax.swing.UIManager.setLookAndFeel(info.getClassName());
//					break;
//				}
//			}
//		} catch (ClassNotFoundException ex) {
//			java.util.logging.Logger.getLogger(SwingHtmlColors.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//		} catch (InstantiationException ex) {
//			java.util.logging.Logger.getLogger(SwingHtmlColors.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//		} catch (IllegalAccessException ex) {
//			java.util.logging.Logger.getLogger(SwingHtmlColors.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
//			java.util.logging.Logger.getLogger(SwingHtmlColors.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//		}
//		//</editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new SwingHtmlColors().setVisible(true);
			}
		});
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider blueSlider;
    private javax.swing.JSlider greenSlider;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel pnlColor;
    private javax.swing.JSlider redSlider;
    private javax.swing.JTextField txtHtmlColor;
    private javax.swing.JTextField txtSwingColor;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
