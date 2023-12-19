/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.lieDown;
import diuf.sudoku.io.StdErr;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.TextComponent;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import java.util.EventObject;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

/**
 * Utilities to provide automatic wait icons in swing.
 * <p>
 * When a long/hard action is performed as a result of a GUI action, the user
 * interface is not refreshed until the action has completed, giving the user
 * the feeling that the program is slow.
 * <p>
 * This class wraps all existing action-listeners to provide:
 * <ul>
 *  <li>Actions are performed <i>after</i> swing has refreshed the GUI.
 *  <li>Busy mouse-pointer is displayed if the action has not finished after
 *   a small delay (currently 250 milliseconds).
 * </ul>
 */
final class AutoBusy {

	static Window getWindow(final Component cmp) {
		if (cmp == null)
			return null;
		if (cmp instanceof JComponent)
			return (Window)((JComponent)cmp).getTopLevelAncestor();
		if (cmp instanceof Window)
			return (Window)cmp;
		return AutoBusy.getWindow(cmp.getParent());
	}

	static Window getWindow(final EventObject e) {
		return AutoBusy.getWindow((Component)e.getSource());
	}

	static void setBusy(final Component cmp, final boolean busy) {
		int cc;
		if(busy) cc=Cursor.WAIT_CURSOR; else cc=Cursor.DEFAULT_CURSOR;
		final Cursor cmpCursor = Cursor.getPredefinedCursor(cc);
		if(busy) cc=Cursor.WAIT_CURSOR; else cc=Cursor.TEXT_CURSOR;
		final Cursor txtCursor = Cursor.getPredefinedCursor(cc);
		if ( cmp == null )
			for ( Frame frame : Frame.getFrames() )
				AutoBusy.setCursor(frame, cmpCursor, txtCursor);
		else
			AutoBusy.setCursor(cmp, cmpCursor, txtCursor);
	}

	private static void setCursor(final Component cmp, final Cursor cmpCursor, final Cursor txtCursor) {
		if ( cmp instanceof TextComponent || cmp instanceof JTextComponent )
			cmp.setCursor(txtCursor);
		else
			cmp.setCursor(cmpCursor);
		if ( cmp instanceof Container ) {
			final Component[] childs = ((Container)cmp).getComponents();
			for ( int i=0; i<childs.length; ++i )
				AutoBusy.setCursor(childs[i], cmpCursor, txtCursor);
		}
	}

	/**
	 * This event listener class wraps all other listener and invoke them
	 * with SwingUtilities.invokeLater() to ensure the gui is refreshed first.
	 * It also displays the busy cursor (with a small delay) until all listeners
	 * have finished.
	 */
	static final class BusyActionListener implements ActionListener
			, ItemListener, ChangeListener, ListSelectionListener {
		EventListener[] als;

		BusyActionListener(final EventListener[] als) {
			this.als = als;
		}

		private final class DelayedBusy extends Thread {
			final Component cmp;
			volatile boolean isFinished = false;
			DelayedBusy(final Component cmp) {
				this.setName("AutoBusy.DelayedBusy");
				this.cmp = cmp;
			}
			@Override
			public void run() {
				lieDown(250);
				synchronized(this) {
					if (!isFinished)
						AutoBusy.setBusy(cmp, true);
				}
			}
		}

		private void event(final EventObject e) {
			final DelayedBusy db = new DelayedBusy(getWindow(e));
			db.start();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							try {
for ( int i=0,n=als.length; i<n; ++i ) {
	if ( !(als[i] instanceof BusyActionListener) ) {
		if (als[i] instanceof ActionListener)
			((ActionListener)als[i]).actionPerformed((ActionEvent)e);
		else if (als[i] instanceof ItemListener)
			((ItemListener)als[i]).itemStateChanged((ItemEvent)e);
		else if (als[i] instanceof ChangeListener)
			((ChangeListener)als[i]).stateChanged((ChangeEvent)e);
		else if (als[i] instanceof ListSelectionListener)
			((ListSelectionListener)als[i]).valueChanged((ListSelectionEvent)e);
	}
}
							} catch(Exception ex) {
								StdErr.whinge("WARN: BusyActionListener.event.run exception", ex);
							}
							synchronized(db) {
								db.isFinished = true;
								if (db.isAlive())
									db.interrupt();
								AutoBusy.setBusy(AutoBusy.getWindow(e), false);
							}
						}
					});
				}
			});
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			event(e);
		}

		@Override
		public void itemStateChanged(final ItemEvent e) {
			event(e);
		}

		@Override
		public void stateChanged(final ChangeEvent e) {
			event(e);
		}

		@Override
		public void valueChanged(final ListSelectionEvent e) {
			event(e);
		}
	}

	/**
	 * Modify the event listeners of this component and all its descendants
	 * in such a way that:
	 * <ul>
	 * <li>The event is invoked <i>after</i> swing has refreshed the user
	 *  interface
	 * <li>A busy pointer is displayed on the window if the event has not
	 * finished after a small delay (currently 250 milliseconds).
	 * </ul>
	 * @param cmp the component whose listeners to modify
	 */
	static void addFullAutoBusy(final Component cmp) {
		synchronized ( cmp.getTreeLock() ) {
			if ( cmp instanceof Container ) {
				final Component[] childs = ((Container)cmp).getComponents();
				for ( int i=0,n=childs.length; i<n; ++i )
					addFullAutoBusy(childs[i]);
			}
			if ( cmp instanceof AbstractButton )
				addAutoBusy((AbstractButton)cmp);
		}
	}

	private static void addAutoBusy(final AbstractButton button) {
		final ActionListener[] als = button.getListeners(ActionListener.class);
		if (als.length > 0 && als[0] instanceof BusyActionListener)
			return;
		for ( int i=0,n=als.length; i<n; ++i )
			button.removeActionListener(als[i]);
		button.addActionListener(new BusyActionListener(als));

		final ItemListener[] ils = button.getListeners(ItemListener.class);
		if ( ils.length>0 && ils[0] instanceof BusyActionListener )
			return;
		for ( int i=0,n=ils.length; i<n; ++i )
			button.removeItemListener(ils[i]);
		button.addItemListener(new BusyActionListener(ils));
	}

}
