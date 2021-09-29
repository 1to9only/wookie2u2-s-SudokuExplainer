/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.io.StdErr;
import static diuf.sudoku.utils.Frmt.PERIOD;
import diuf.sudoku.utils.MyStrings;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * THE_SETTINGS is the singleton instance of application settings.
 * Stored in: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\diuf\sudoku
 */
public final class Settings implements Cloneable {

	/**
	 * The boolean setting name Strings are public so that here is THE name of
	 * each boolean setting, to keep it consistent throughout. I do it this way
	 * now because a simple spelling mistake cost me an hour. Sigh.
	 * <p>
	 * NOTE WELL that if you forget to add a new field to BOOLEAN_FIELD_NAMES
	 * it won't be saved or retrieved by the Settings load and save methods,
	 * so it'll switch in the GUI, but won't persist between sessions.
	 */
	public static final String isAntialiasing, isShowingMaybes
		, isFilteringHints, isHacky, isGreenFlash, isa4ehacked, isa5ehacked
		, isa6ehacked, isa7ehacked, isa8ehacked, isa9ehacked, isa10ehacked
		;

	/**
	 * A List of the names of all my "standard" boolean fields. Each string has
	 * ONE definition rather than (as just happened) misslepping the barstads!
	 * <p>
	 * NOTE WELL that if you forget to add a new field to BOOLEAN_FIELD_NAMES
	 * it won't be saved or retrieved by the Settings load and save methods,
	 * so it'll switch in the GUI, but won't persist between sessions.
	 */
	private static final String[] BOOLEAN_FIELD_NAMES = new String[] {
		  isAntialiasing = "isAntialiasing" // GUI painting
		, isShowingMaybes = "isShowingMaybes" // GUI little grey digits
		, isFilteringHints = "isFilteringHints" // GUI hide extraneious hints
		, isHacky = "isHacky" // enables top1465 nasty special treatments
		, isGreenFlash = "isGreenFlash" // GUI flashBackgroundAqua
		, isa4ehacked = "isa4ehacked" // users choice
		, isa5ehacked = "isa5ehacked"
		, isa6ehacked = "isa6ehacked"
		, isa7ehacked = "isa7ehacked"
		, isa8ehacked = "isa8ehacked" // always true
		, isa9ehacked = "isa9ehacked"
		, isa10ehacked = "isa10ehacked"
	};

	public static final EnumSet<Tech> ALL_TECHS = EnumSet.allOf(Tech.class);

	public static final Settings THE_SETTINGS = new Settings(); // this is a Singleton

	// DATE and TIME formats for the project.
	// eg: 2019-10-03.07-34-17 meaning that it's thirty-four minutes past seven
	// o'clock in the morning on Thursday, October the third in the year of our
	// halfwitted hot-crossed pillock two thousand and nineteen, but it's OK.
	// I've got my teeth back-in now! And I don't want to Chineese either.
	// 'F' ISO 8601 complete date formatted as "%tY-%tm-%td".
	// 'H' 2 digit 24-hour  : 00 - 23.
	// 'M' 2 digit Minute   : 00 - 59.
	// 'S' 2 digit Seconds  : 00 - 60 (leap seconds). (More in MyTester)
	public static final String DATE_FORMAT = "%1$tF";
	public static final String TIME_FORMAT = "%1$tH-%1$tM-%1$tS";
	public static final String DATE_TIME_FORMAT = DATE_FORMAT+PERIOD+TIME_FORMAT;
	// set ONCE by LogicalSolverTester at the start of a run using now()
	public static String now;
	private static long startNanos;

	public static String now() {
		startNanos = System.nanoTime();
		return now = String.format(DATE_TIME_FORMAT, new Date());
	}
	public static long tookNanos() {
		return System.nanoTime() - startNanos;
	}
	public static String took() {
		return format(tookNanos());
	}
	public static String NANOS_FORMAT = "%,15d";
	public static String format(long nanos) {
		return MyStrings.format(NANOS_FORMAT, nanos);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ instance stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final Preferences preferences;
	private final Map<String, Boolean> booleans = new HashMap<>(16, 0.75F);

	private String lookAndFeelClassName = null;
	// wantedTechs starts life as an empty EnumSet of Tech's
	private EnumSet<Tech> wantedTechs = EnumSet.noneOf(Tech.class);

	private int modCount; // defaults to 0 automagically
	private static final String MODIFICATION_COUNT_KEY_NAME = "mod";

	private Settings() {
		// glom onto the Preferences for the application
		preferences = Preferences.userNodeForPackage(Settings.class);
		load();
		xml = this.toXml();
	}

	public Rectangle getBounds() {
		return new Rectangle(
			  preferences.getInt("x",0)
			, preferences.getInt("y",0)
			, preferences.getInt("width",3000)
			, preferences.getInt("height",1080)
		);
	}

	public void setBounds(Rectangle bounds) {
		if ( preferences != null )
			try {
				preferences.putInt("x", bounds.x);
				preferences.putInt("y", bounds.y);
				preferences.putInt("width", bounds.width);
				preferences.putInt("height", bounds.height);
				preferences.flush();
			} catch (BackingStoreException ex) {
				StdErr.whinge(ex);
			}
	}

	public boolean get(String name) {
		return get(name, false); // the default default is false.
	}
	// this get method (with defualt) is currently only used by the set method.
	public boolean get(String name, boolean defualt) {
		Boolean r = booleans.get(name);
		if ( r == null )
			return defualt;
		return r.booleanValue();
	}
	// if the registry entry does not exist yet we return value, as well as set
	// the registry entry to value; which is a bit odd, but it works for me.
	public boolean set(String name, boolean value) {
		boolean pre = get(name, value);
		this.booleans.put(name, value);
		return pre;
	}

	public boolean getBoolean(String name, boolean defualt) {
		try {
			return preferences.getBoolean(name, defualt);
		} catch (Exception ex) {
			StdErr.carp("preferences.getBoolean("+name+", "+defualt+") failed", ex);
			return false; // you can't get here!
		}
	}

	public int getInt(String name, int defualt) {
		try {
			return preferences.getInt(name, defualt);
		} catch (Throwable eaten) {
			return defualt;
		}
	}
	public void putInt(String name, int value) {
		try {
			preferences.putInt(name, value);
		} catch (Throwable eaten) {
			// Do nothing
		}
	}

	public String getLookAndFeelClassName() {
		return lookAndFeelClassName;
	}
	public String setLookAndFeelClassName(String lookAndFeelClassName) {
		String pre = this.lookAndFeelClassName;
		this.lookAndFeelClassName = lookAndFeelClassName;
		return pre;
	}

	public int getWantedTechsSize() {
		return wantedTechs.size();
	}
	public EnumSet<Tech> getWantedTechs() {
		return EnumSet.copyOf(wantedTechs);
	}
	public EnumSet<Tech> setWantedTechs(EnumSet<Tech> techs) {
		EnumSet<Tech> pre = EnumSet.copyOf(wantedTechs);
		wantedTechs = techs;
		return pre;
	}
	public void justSetWantedTechs(EnumSet<Tech> techs) {
		wantedTechs = techs;
	}

	public boolean allWanted(Tech... rules) {
		for ( Tech r : rules )
			if ( !this.wantedTechs.contains(r) )
				return false;
		return true;
	}

	public boolean anyWanted(Tech... rules) {
		for ( Tech r : rules )
			if ( this.wantedTechs.contains(r) )
				return true;
		return false;
	}

	public boolean anyWanted(List<Tech> rules) {
		for ( Tech r : rules )
			if ( this.wantedTechs.contains(r) )
				return true;
		return false;
	}

	/**
	 * Get the Modification Count: this number is incremented whenever the
	 * Settings are saved to the underlying Preferences (Windows registry).
	 * The Settings are saved when you press OK the TechSelectDialog, and when
	 * the GUI closes.
	 * <p>
	 * Note that the main window location is also saved in registry, but it
	 * doesn't count as a Setting, so modCount doesn't change upon save.
	 * <p>
	 * ModCount persists between runs. It starts at Integer.MIN_VALUE, which is
	 * -2,147,483,648 and 0 (if anybody ever gets that far) is skipped, so that
	 * modCount won't collide with the default value for ints.
	 *
	 * @return the modification count of these Settings.
	 */
	public int getModCount() {
		return modCount;
	}

	private void load() {
		if ( preferences == null )
			throw new NullPointerException("preferences are null!");
		try {
			for ( String fieldName : BOOLEAN_FIELD_NAMES )
				booleans.put(fieldName, preferences.getBoolean(fieldName, true));
			lookAndFeelClassName = preferences.get("lookAndFeelClassName", lookAndFeelClassName);
			wantedTechs.clear();
			for ( Tech t : ALL_TECHS )
				if ( preferences.getBoolean(t.name(), t.defaultWanted) )
					wantedTechs.add(t);
			// get modCount skipping 0, the default value for ints.
			modCount = skip0(preferences.getInt(MODIFICATION_COUNT_KEY_NAME, Integer.MIN_VALUE));
//			System.out.println("Settings.load: modCount="+modCount);
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	private static int skip0(int value) {
		if ( value == 0 )
			return 1;
		return value;
	}

	public void save() {
		// usages: Test-cases use static THE_SETTINGS so Netbeans misses there
		// usages of save(). This is another way around the problem.
		// To be clear: test-cases should never THE_SETTINGS.save()!
		if ( false ) {
			new Throwable().printStackTrace(System.out);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
		if ( preferences == null )
			throw new NullPointerException("preferences are null!");
		try {
			for ( String fieldName : BOOLEAN_FIELD_NAMES )
				preferences.putBoolean(fieldName, get(fieldName)); // defaults to false!
			if ( lookAndFeelClassName != null )
				preferences.put("lookAndFeelClassName", lookAndFeelClassName);
			for ( Tech t : ALL_TECHS )
				preferences.putBoolean(t.name(), wantedTechs.contains(t));
//			// increment and store the modification count
			preferences.putInt(MODIFICATION_COUNT_KEY_NAME, ++modCount);
			try {
				preferences.flush();
			} catch (BackingStoreException ex) {
				StdErr.whinge(ex);
			}
			// update the xml which backs equals, hashCode and clone
			xml = this.toXml();
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	public void close() {
//		save();
	}

	/**
	 * Returns a byte array containing the XML of my preferences.
	 * @return byte[] of preferences XML
	 */
	public byte[] toXml() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			preferences.exportSubtree(baos);
			return baos.toByteArray();
		} catch (Exception ex) {
			StdErr.carp("Settings.equals "+ex.getClass().getSimpleName(), ex);
			return null; // you can't get here
		}
	}

	/**
	 * Does the given Settings contain the same Preferences as this Settings?
	 * NOTE: equals compares the Preferences only, so if any settings are (in
	 * future) persisted elsewhere, then you'll also need to compare them here.
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null
			&& (obj instanceof Settings)
			&& equals((Settings)obj);
	}
	/**
	 * Does the given Settings contain the same Preferences as this Settings?
	 * <p>
	 * WARN: other is presumed to be NOT null!
	 * <p>
	 * NOTE: equals compares the Preferences only, so if any settings are (in
	 * future) persisted elsewhere, then you'll also need to compare them here.
	 * @param other the other Settings to compare this to.
	 * @return java.util.Arrays.equals(xml, other.xml);
	 */
	public boolean equals(Settings other) {
		return Arrays.equals(xml, other.xml);
	}

	/**
	 * Returns the identity hashCode: ie Object.hashCode(), to guarantee that
	 * a Settings object always reports the same hashCode to Hash*, breaking
	 * the spirit of the equals-hashCode contract! That the hashCode can and
	 * probably should change when the fields compared by equals change.
	 * <p>
	 * I do not expect this to cause issues because you wouldn't create a Hash*
	 * of Settings! There is one Settings per app! Hence the hashCode debate is
	 * a ____ing waste of brain.
	 * @return super.hashCode();
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Settings copy = (Settings)super.clone();
//Debugger indicates that these are already copied (but how?)
//		copy.lookAndFeelClassName = this.lookAndFeelClassName;
//		copy.wantedTechs = EnumSet.copyOf(wantedTechs);
//		copy.xml = copy.toXml();
		return copy;
	}
	byte[] xml;

	// --------------------- FOR LogicalAnalyserTest ONLY ---------------------

	/**
	 * For LogicalAnalyserTest: set the given settings and create a checkPoint
	 * to revert to later. The given settings are set but are NOT saved, so
	 * that we can create a new LogicalSolver from non-registry Settings,
	 * without permanently effecting THE_SETTINGS.
	 * <p>
	 * You call checkPoint to change THE_SETTINGS to the given settings,
	 * then create your LogicalSolver,
	 * then you <u>finally</u> call revert, or THE_SETTINGS stay screwy,
	 * and when the LogicalSolver is closed they are saved (badly thunk)!
	 *
	 * @param newHacked
	 * @param newWantedTechs
	 */
	public void checkPoint(final EnumSet<Tech> newWantedTechs, final boolean[] newHacked) {
		checkPointHacked = new boolean[] {
			// set returns the previous value of this setting
			  set(isa4ehacked, newHacked[0])
			, set(isa5ehacked, newHacked[1])
			, set(isa6ehacked, newHacked[2])
			, set(isa7ehacked, newHacked[3])
			, set(isa8ehacked, newHacked[4])
			, set(isa9ehacked, newHacked[5])
			, set(isa10ehacked, newHacked[6])
		};
		checkPointWanted = wantedTechs.clone();
		wantedTechs = newWantedTechs;
		checkPointed = true;
	}
	private boolean[] checkPointHacked; // pre image
	private EnumSet<Tech> checkPointWanted; // pre image
	private boolean checkPointed; // has a pre image been taken
	/**
	 * For LogicalAnalyserTest: Revert settings to the previous savePoint.
	 */
	public void revert() {
		if ( !checkPointed )
			throw new IllegalStateException("No checkPoint to revert to!");
		set(isa4ehacked, checkPointHacked[0]);
		set(isa5ehacked, checkPointHacked[1]);
		set(isa6ehacked, checkPointHacked[2]);
		set(isa7ehacked, checkPointHacked[3]);
		set(isa8ehacked, checkPointHacked[4]);
		set(isa9ehacked, checkPointHacked[5]);
		set(isa10ehacked, checkPointHacked[6]);
		wantedTechs = checkPointWanted;
		// savePoint and revert are single use
		checkPointWanted = null;
		checkPointHacked = null;
		checkPointed = false;
	}

}
