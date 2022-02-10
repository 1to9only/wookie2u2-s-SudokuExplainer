/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Tech.techNames;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import static diuf.sudoku.utils.Frmt.PERIOD;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyPreferences;
import diuf.sudoku.utils.MyStrings;
import java.awt.Rectangle;
import java.io.File;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.prefs.BackingStoreException;


/**
 * THE_SETTINGS is the singleton instance of Sudoku Explainer's Settings.
 * <p>
 * Stored in: HOME\
 */
public final class Settings implements Cloneable {

	/**
	 * When Sudoku Explainer is distributed as "just the .jar file" without a
	 * DiufSudoku_Settings.txt file (not how it's designed to be distributed)
	 * it creates it's own settings file, presuming the file is writable.
	 * <p>
	 * Failing that it resorts to the default-setting for each setting as it's
	 * gotten from Settings; so there are multiple levels of defaults, so to
	 * change a default-setting you change it here and everywhere that setting
	 * is gotten from Settings, which is a pain, but it is what it is.
	 */
	private static final String DEFAULT_SETTINGS = // @check updated?
// settings
"x=0\n" +
"y=0\n" +
"width=1024\n" +
"height=1024\n" +
"mod=-2147483647\n" +
"isFilteringHints=true\n" +
"isAntialiasing=true\n" +
"isShowingMaybes=true\n" +
"isHacky=false\n" +
"isGreenFlash=true\n" +
// validators
Tech.Analysis.name()+"=true\n" +
Tech.TooFewClues.name()+"=true\n" +
Tech.TooFewValues.name()+"=true\n" +
Tech.NotOneSolution.name()+"=true\n" +
Tech.MissingMaybes.name()+"=true\n" +
Tech.DoubleValues.name()+"=true\n" +
Tech.HomelessValues.name()+"=true\n" +
// Sudoku solving techniques
Tech.LonelySingle.name()+"=false\n" +
Tech.NakedSingle.name()+"=true\n" +
Tech.HiddenSingle.name()+"=true\n" +
Tech.Locking.name()+"=true\n" +
Tech.LockingBasic.name()+"=false\n" +
Tech.DirectNakedPair.name()+"=false\n" +
Tech.DirectHiddenPair.name()+"=false\n" +
Tech.DirectNakedTriple.name()+"=false\n" +
Tech.DirectHiddenTriple.name()+"=false\n" +
Tech.NakedPair.name()+"=true\n" +
Tech.HiddenPair.name()+"=true\n" +
Tech.NakedTriple.name()+"=true\n" +
Tech.HiddenTriple.name()+"=true\n" +
Tech.Swampfish.name()+"=true\n" +
Tech.TwoStringKite.name()+"=true\n" +
Tech.XY_Wing.name()+"=true\n" +
Tech.XYZ_Wing.name()+"=true\n" +
Tech.W_Wing.name()+"=true\n" +
Tech.Swordfish.name()+"=true\n" +
Tech.Skyscraper.name()+"=true\n" +
Tech.EmptyRectangle.name()+"=true\n" +
Tech.Jellyfish.name()+"=false\n" +
Tech.Coloring.name()+"=true\n" +
Tech.XColoring.name()+"=true\n" +
Tech.Medusa3D.name()+"=false\n" +
Tech.GEM.name()+"=true\n" +
Tech.NakedQuad.name()+"=true\n" +
Tech.HiddenQuad.name()+"=true\n" +
Tech.BigWings.name()+"=true\n" +
Tech.WXYZ_Wing.name()+"=false\n" +
Tech.VWXYZ_Wing.name()+"=false\n" +
Tech.UVWXYZ_Wing.name()+"=false\n" +
Tech.TUVWXYZ_Wing.name()+"=false\n" +
Tech.STUVWXYZ_Wing.name()+"=false\n" +
Tech.UniqueRectangle.name()+"=true\n" +
Tech.FinnedSwampfish.name()+"=true\n" +
Tech.FinnedSwordfish.name()+"=true\n" +
Tech.FinnedJellyfish.name()+"=false\n" +
Tech.DeathBlossom.name()+"=true\n" +
Tech.ALS_XZ.name()+"=true\n" +
Tech.ALS_Wing.name()+"=true\n" +
Tech.ALS_Chain.name()+"=true\n" +
Tech.SueDeCoq.name()+"=false\n" +
Tech.FrankenSwampfish.name()+"=false\n" +
Tech.FrankenSwordfish.name()+"=false\n" +
Tech.FrankenJellyfish.name()+"=false\n" +
Tech.KrakenSwampfish.name()+"=false\n" +
Tech.MutantSwampfish.name()+"=false\n" +
Tech.KrakenSwordfish.name()+"=false\n" +
Tech.MutantSwordfish.name()+"=false\n" +
Tech.KrakenJellyfish.name()+"=false\n" +
Tech.MutantJellyfish.name()+"=false\n" +
Tech.AlignedPair.name()+"=false\n" +
Tech.AlignedTriple.name()+"=false\n" +
Tech.AlignedQuad.name()+"=false\nisa4ehacked=false\n" +
Tech.AlignedPent.name()+"=false\nisa5ehacked=true\n" +
Tech.AlignedHex.name()+"=false\nisa6ehacked=true\n" +
Tech.AlignedSept.name()+"=false\nisa7ehacked=true\n" +
Tech.AlignedOct.name()+"=false\nisa8ehacked=true\n" +
Tech.AlignedNona.name()+"=false\nisa9ehacked=true\n" +
Tech.AlignedDec.name()+"=false\nisa10ehacked=true\n" +
Tech.UnaryChain.name()+"=true\n" +
Tech.NishioChain.name()+"=true\n" +
Tech.MultipleChain.name()+"=true\n" +
Tech.DynamicChain.name()+"=true\n" +
Tech.DynamicPlus.name()+"=true\n" +
Tech.NestedUnary.name()+"=true\n" +
Tech.NestedMultiple.name()+"=false\n" +
Tech.NestedDynamic.name()+"=false\n" +
Tech.NestedPlus.name()+"=false\n" +
"";

	// open MyPreferences(file), which is NEVER null but may be empty.
	private static MyPreferences open(File file) {
		// if the file doesn't exist save the default default settings to it
		if ( !file.exists() ) {
			try {
				IO.save(DEFAULT_SETTINGS, file);
			} catch (Exception ex) {
				// nb: can't use Log here, coz it uses Settings (chicken/egg)
				System.err.println("Settings.open: failed to create: "+file);
				ex.printStackTrace(System.err);
			}
		}
		// load the file, returning an empty Map if load fails for ANY reason
		return new MyPreferences(file);
	}

	// modCount needs to be never 0
	private static int skip0(int value) {
		if ( value == 0 )
			return 1;
		return value;
	}

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

	public static final Settings THE_SETTINGS = new Settings(IO.SETTINGS); // this is a Singleton

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

	private final MyPreferences prefs;

	private EnumSet<Tech> wantedTechs = EnumSet.noneOf(Tech.class);

	private static final String MODIFICATION_COUNT_KEY_NAME = "mod";

	// modification count
	private int modCount = Integer.MIN_VALUE; // default only

	private Settings(File file) {
		// NEVER null, but empty if load fails
		prefs = open(file); // a Map<String,String>
		load();
	}

	public Rectangle getBounds() {
		return new Rectangle(
			  prefs.getInt("x", 0)
			, prefs.getInt("y", 0)
			, prefs.getInt("width", 3080)
			, prefs.getInt("height", 1080)
		);
	}

	public void setBounds(Rectangle bounds) {
		if ( prefs == null )
			return;
		try {
			prefs.putInt("x", bounds.x);
			prefs.putInt("y", bounds.y);
			prefs.putInt("width", bounds.width);
			prefs.putInt("height", bounds.height);
			prefs.flush();
		} catch (BackingStoreException ex) {
			StdErr.whinge(Log.me()+" BackingStoreException", ex);
		}
	}

	public boolean getBoolean(String name) {
		return getBoolean(name, false); // the default default is false.
	}
	// this get method (with defualt) is currently only used by the set method.
	public boolean getBoolean(String name, boolean defualt) {
		return prefs.getBoolean(name, defualt);
	}

	// if the registry entry does not exist yet we return value, as well as set
	// the registry entry to value; which is a bit odd, but it works for me.
	public boolean setBoolean(String name, boolean value) {
		boolean pre = getBoolean(name, value);
		prefs.putBoolean(name, value);
		return pre;
	}

	/** @return THE_SETTINGS.get(Settings.isFilteringHints) */
	public boolean isFilteringHints() { return getBoolean(isFilteringHints); }
	public boolean isFilteringHints(boolean defualt) { return getBoolean(isFilteringHints, defualt); }
	public boolean setIsFilteringHints(boolean b) { return setBoolean(isFilteringHints, b); }

	public int getInt(String name, int defualt) {
		return prefs.getInt(name, defualt);
	}
	public void putInt(String name, int value) {
		++modCount;
		prefs.putInt(name, value);
	}

	public void unwant(Tech t) {
		wantedTechs.remove(t);
	}
	public int getWantedTechsSize() {
		return wantedTechs.size();
	}
	public EnumSet<Tech> getWantedTechs() {
		return EnumSet.copyOf(wantedTechs);
	}
	public EnumSet<Tech> setWantedTechs(EnumSet<Tech> techs) {
		++modCount;
		EnumSet<Tech> pre = EnumSet.copyOf(wantedTechs);
		wantedTechs = techs;
		return pre;
	}
	public void justSetWantedTechs(EnumSet<Tech> techs) {
		wantedTechs = techs;
	}

	public boolean allWanted(Tech[] techs) {
		for ( Tech tech : techs ) {
			if ( !this.wantedTechs.contains(tech) ) {
				System.out.println("MIA: "+tech);
				return false;
			}
		}
		return true;
	}

	public boolean anyWanted(Tech[] techs) {
		for ( Tech tech : techs ) {
			if ( this.wantedTechs.contains(tech) ) {
				return true;
			}
		}
		System.out.println("MIA: Any of "+techNames(techs));
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
		if ( prefs == null )
			throw new NullPointerException("preferences are null!");
		try {
			wantedTechs.clear();
			for ( Tech t : ALL_TECHS )
				if ( prefs.getBoolean(t.name(), t.defaultWanted) )
					wantedTechs.add(t);
			// get modCount skipping 0, the default value for ints.
			modCount = prefs.getInt(MODIFICATION_COUNT_KEY_NAME, modCount);
//			System.out.println("Settings.load: modCount="+modCount);
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	public void save() {
		// usages: Test-cases use static THE_SETTINGS so Netbeans misses there
		// usages of save(). This is another way around the problem.
		// To be clear: test-cases should never THE_SETTINGS.save()!
		if ( false ) {
			new Throwable().printStackTrace(System.out);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
		if ( prefs == null )
			throw new NullPointerException("preferences are null!");
		try {
			for ( String fieldName : BOOLEAN_FIELD_NAMES )
				prefs.putBoolean(fieldName, getBoolean(fieldName)); // defaults to false!
			for ( Tech t : ALL_TECHS )
				prefs.putBoolean(t.name(), wantedTechs.contains(t));
//			// increment and store the modification count
			prefs.putInt(MODIFICATION_COUNT_KEY_NAME, skip0(++modCount));
			try {
				prefs.flush();
			} catch (BackingStoreException ex) {
				StdErr.whinge(Log.me()+" BackingStoreException", ex);
			}
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	public void close() {
//		save();
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
	 * nb: I was preferences.equals(other.preferences) but it produced results
	 * that I'm too thick to understand, so I DIY. This I understand. The old
	 * version was diff'ing XML that I'm too lazy to produce just for equals.
	 * <p>
	 * This method is now not AFAIK used, atleast LogicalSolverFactor.recreate
	 * no-longer relies upon it; it just recreates the bastard coz it was told
	 * to, and if it's the same as the old one then tough, which may be a waste
	 * but at-least it's a SIMPLE waste. sigh.
	 *
	 * @param other the other Settings to compare this to.
	 * @return {@code preferences.equals(other.preferences)} delegating down to
	 * AbstractCollection equals
	 */
	public boolean equals(Settings other) {
		if ( other == this )
			return true;
		if ( other == null )
			return false;
		// are they different versions?
		if ( other.modCount != modCount )
			return false;
		final MyPreferences me = prefs; // NEVER null but may be empty!
		final MyPreferences op = other.prefs; // NEVER null but may be empty!
		if ( me.size() != op.size() )
			return false;
		for ( Map.Entry<String,String> e : me.entrySet() )
			if ( !e.getValue().equals(op.get(e.getKey())) )
				return false;
		return true;
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
		return copy;
	}

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
		preAeHacked = new boolean[] {
			// set returns the previous value of this setting
			  setBoolean(isa4ehacked, newHacked[0])
			, setBoolean(isa5ehacked, newHacked[1])
			, setBoolean(isa6ehacked, newHacked[2])
			, setBoolean(isa7ehacked, newHacked[3])
			, setBoolean(isa8ehacked, newHacked[4])
			, setBoolean(isa9ehacked, newHacked[5])
			, setBoolean(isa10ehacked, newHacked[6])
		};
		preWantedTechs = wantedTechs.clone();
		wantedTechs = newWantedTechs;
		isCheckPointed = true;
	}
	private boolean[] preAeHacked; // pre image
	private EnumSet<Tech> preWantedTechs; // pre image
	private boolean isCheckPointed; // has a pre image been taken
	/**
	 * For LogicalAnalyserTest: Revert settings to the previous savePoint.
	 */
	public void revert() {
		if ( !isCheckPointed )
			throw new IllegalStateException("No checkPoint to revert to!");
		setBoolean(isa4ehacked, preAeHacked[0]);
		setBoolean(isa5ehacked, preAeHacked[1]);
		setBoolean(isa6ehacked, preAeHacked[2]);
		setBoolean(isa7ehacked, preAeHacked[3]);
		setBoolean(isa8ehacked, preAeHacked[4]);
		setBoolean(isa9ehacked, preAeHacked[5]);
		setBoolean(isa10ehacked, preAeHacked[6]);
		wantedTechs = preWantedTechs;
		// savePoint and revert are single use
		preWantedTechs = null;
		preAeHacked = null;
		isCheckPointed = false;
	}

}
