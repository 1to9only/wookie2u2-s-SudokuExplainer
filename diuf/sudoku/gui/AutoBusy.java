/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

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
 * Utilities to improve Swings perceived speed.
 * <p>
 * When a long/hard action is performed as a result of a GUI action, the user
 * interface is not refreshed until the action has completed, giving the user
 * the feeling that the program is slow.
 * <p>
 * This class wraps all existing action-listeners to provide:
 * <ul>
 *  <li>The actions are performed <i>after</i> swing has refreshed the GUI.
 *  <li>The busy mouse-pointer is displayed if the action hasn't finished after
 *   a small delay (currently 250 milliseconds, or a quarter of a second).
 * </ul>
 */
public final class AutoBusy {

	static Window getWindow(Component cmp) {
		if (cmp == null)
			return null;
		if (cmp instanceof JComponent)
			return (Window)((JComponent)cmp).getTopLevelAncestor();
		if (cmp instanceof Window)
			return (Window)cmp;
		return AutoBusy.getWindow(cmp.getParent());
	}

	static Window getWindow(EventObject e) {
		return AutoBusy.getWindow((Component)e.getSource());
	}

	public static void setBusy(Component cmp, boolean busy) {
		int cc;
		if(busy) cc=Cursor.WAIT_CURSOR; else cc=Cursor.DEFAULT_CURSOR;
		Cursor cmpCursor = Cursor.getPredefinedCursor(cc);
		if(busy) cc=Cursor.WAIT_CURSOR; else cc=Cursor.TEXT_CURSOR;
		Cursor txtCursor = Cursor.getPredefinedCursor(cc);
		if ( cmp == null )
			for ( Frame frame : Frame.getFrames() )
				AutoBusy.setCursor(frame, cmpCursor, txtCursor);
		else
			AutoBusy.setCursor(cmp, cmpCursor, txtCursor);
	}

	private static void setCursor(Component cmp, Cursor cmpCursor, Cursor txtCursor) {
		if (cmp instanceof TextComponent || cmp instanceof JTextComponent)
			cmp.setCursor(txtCursor);
		else
			cmp.setCursor(cmpCursor);
		if (cmp instanceof Container) {
			Component[] childs = ((Container)cmp).getComponents();
			for (int i = 0; i < childs.length; i++)
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
		public BusyActionListener(EventListener[] als) {
			this.als = als;
		}
		private final class DelayedBusy extends Thread {
			Component cmp;
			public volatile boolean isFinished = false;
			public DelayedBusy(Component cmp) {
				this.setName("AutoBusy.DelayedBusy");
				this.cmp = cmp;
			}
			@Override
			public void run() {
				try{Thread.sleep(250);}catch(InterruptedException eaten){}
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
for (int i = 0; i < als.length; i++) {
	if (!(als[i] instanceof BusyActionListener)) {
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
							} catch(Throwable ex) {
								StdErr.whinge("BusyActionListener.event.run exception", ex);
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
		public void actionPerformed(ActionEvent e) {
			event(e);
		}
		@Override
		public void itemStateChanged(ItemEvent e) {
			event(e);
		}
		@Override
		public void stateChanged(ChangeEvent e) {
			event(e);
		}
		@Override
		public void valueChanged(ListSelectionEvent e) {
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
	public static void addFullAutoBusy(Component cmp) {
		synchronized (cmp.getTreeLock()) {
			if (cmp instanceof Container) {
				Component[] childs = ((Container)cmp).getComponents();
				for (int i = 0; i < childs.length; i++)
					addFullAutoBusy(childs[i]);
			}
			if (cmp instanceof AbstractButton) {
				addAutoBusy((AbstractButton)cmp);
			}
		}
	}

	private static void addAutoBusy(AbstractButton button) {
		ActionListener[] als = button.getListeners(ActionListener.class);
		if (als.length > 0 && als[0] instanceof BusyActionListener)
			return;
		for (int i = 0; i < als.length; i++)
			button.removeActionListener(als[i]);
		button.addActionListener(new BusyActionListener(als));

		ItemListener[] ils = button.getListeners(ItemListener.class);
		if (ils.length > 0 && ils[0] instanceof BusyActionListener)
			return;
		for (int i = 0; i < ils.length; i++)
			button.removeItemListener(ils[i]);
		button.addItemListener(new BusyActionListener(ils));
	}

}