/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.beep;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import static diuf.sudoku.utils.Frmt.NL;
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
 * CFG is the singleton instance of Sudoku Explainers Config.
 * <p>
 * Stored in: IO.HOME/DiufSudoku_Config.txt
 */
public final class Config implements Cloneable {

	/**
	 * When Sudoku Explainer is distributed as "just the .jar file" without a
	 * DiufSudoku_Config.txt file (not how it is designed to be distributed)
	 * it creates its own config file, presuming that files are writable.
	 * <p>
	 * Failing that it resorts to the default-setting for each setting as it is
	 * gotten from Config; so there are multiple levels of defaults, so to
	 * change a default-setting you change it here and everywhere that setting
	 * is gotten from Config, which is a pain, but it is what it is.
	 * <p>
	 * I wish Java had a here-document! BTW, I am not real sure, but I think a
	 * static method allows the string to not live past its use-by-date. What I
	 * want to do is write this to configFile, and immediately forget it.
	 */
	private static String defaultConfig() { // true @check false updated?
return
// etc
"lookAndFeelClassName=#NONE#" + NL +
"x=0" + NL +
"y=0" + NL +
"width=800" + NL +
"height=840" + NL +
"mod=-2147483647" + NL +
"isFilteringHints=true" + NL +
"isAntialiasing=true" + NL +
"isShowingMaybes=true" + NL +
"isHacky=false" + NL +
"isGreenFlash=true" + NL +
"isInvalidGridHandled=true" + NL +
"isTechSelectDialogColorful=true" + NL +
"isCachingHints=true" + NL +
"hintsCacheSize=4" + NL +
"hintsCacheBored=8" + NL +
"maxFins=5" + NL +
"maxEndofins=3" + NL +
"maxKrakenType=2" + NL +
"maxKrakenSizes=[4, 4, 3]" + NL +
"generateDifficulty=8" + NL +
// validators
Tech.Analysis.name()+"=true" + NL +
Tech.TooFewClues.name()+"=true" + NL +
Tech.TooFewValues.name()+"=true" + NL +
Tech.NotOneSolution.name()+"=true" + NL +
Tech.MissingMaybes.name()+"=true" + NL +
Tech.DoubleValues.name()+"=true" + NL +
Tech.HomelessValues.name()+"=true" + NL +
// Easy
Tech.LonelySingle.name()+"=false" + NL +
Tech.NakedSingle.name()+"=true" + NL +
Tech.HiddenSingle.name()+"=true" + NL +
// Medium
Tech.DirectNakedPair.name()+"=false" + NL +
Tech.DirectHiddenPair.name()+"=false" + NL +
Tech.DirectNakedTriple.name()+"=false" + NL +
Tech.DirectHiddenTriple.name()+"=false" + NL +
Tech.Locking.name()+"=true" + NL +
Tech.LockingBasic.name()+"=false" + NL +
// Hard
Tech.NakedPair.name()+"=true" + NL +
Tech.HiddenPair.name()+"=true" + NL +
Tech.NakedTriple.name()+"=true" + NL +
Tech.HiddenTriple.name()+"=true" + NL +
// Fiendish
Tech.TwoStringKite.name()+"=true" + NL +
Tech.Swampfish.name()+"=true" + NL +
Tech.W_Wing.name()+"=true" + NL +
Tech.XY_Wing.name()+"=true" + NL +
Tech.XYZ_Wing.name()+"=true" + NL +
Tech.Skyscraper.name()+"=true" + NL +
Tech.EmptyRectangle.name()+"=true" + NL +
// Nasty
Tech.Swordfish.name()+"=true" + NL +
Tech.URT.name()+"=true" + NL +
Tech.Jellyfish.name()+"=false" + NL +
Tech.NakedQuad.name()+"=true" + NL +
Tech.HiddenQuad.name()+"=false" + NL +
Tech.Coloring.name()+"=true" + NL +
Tech.XColoring.name()+"=true" + NL +
Tech.Medusa3D.name()+"=false" + NL +
Tech.GEM.name()+"=true" + NL +
// Alsic
Tech.BigWings.name()+"=true" + NL +
Tech.DeathBlossom.name()+"=true" + NL +
Tech.ALS_XZ.name()+"=true" + NL +
Tech.ALS_Wing.name()+"=true" + NL +
Tech.ALS_Chain.name()+"=true" + NL +
Tech.SueDeCoq.name()+"=false" + NL +
// Fisch
Tech.FinnedSwampfish.name()+"=false" + NL +
Tech.FrankenSwampfish.name()+"=false" + NL +
Tech.MutantSwampfish.name()+"=false" + NL +
Tech.FinnedSwordfish.name()+"=false" + NL +
Tech.FrankenSwordfish.name()+"=false" + NL + // best compromise
Tech.MutantSwordfish.name()+"=false" + NL +
Tech.FinnedJellyfish.name()+"=false" + NL +
Tech.FrankenJellyfish.name()+"=false" + NL +
Tech.MutantJellyfish.name()+"=false" + NL +
Tech.KrakenSwampfish.name()+"=false" + NL +
Tech.KrakenSwordfish.name()+"=false" + NL +
Tech.KrakenJellyfish.name()+"=false" + NL +
// Ligature
Tech.AlignedPair.name()+"=false" + NL +
Tech.AlignedTriple.name()+"=false" + NL +
Tech.AlignedQuad.name()+"=false" + NL +
Tech.AlignedPent.name()+"=false" + NL +
Tech.AlignedHex.name()+"=false" + NL +
Tech.AlignedSept.name()+"=false" + NL +
Tech.AlignedOct.name()+"=false" + NL +
Tech.AlignedNona.name()+"=false" + NL +
Tech.AlignedDec.name()+"=false" + NL +
// Diabolical
Tech.TableChain.name()+"=true" + NL +
Tech.Abduction.name()+"=false" + NL +
Tech.UnaryChain.name()+"=false" + NL +
Tech.StaticChain.name()+"=false" + NL +
Tech.NishioChain.name()+"=false" + NL +
Tech.DynamicChain.name()+"=true" + NL +
// IDKFA
Tech.DynamicPlus.name()+"=true" + NL +
Tech.NestedUnary.name()+"=true" + NL +
Tech.NestedStatic.name()+"=false" + NL +
Tech.NestedDynamic.name()+"=false" + NL +
Tech.NestedPlus.name()+"=false" + NL +
"";
}

	// open MyPreferences(file), which is NEVER null but may be empty.
	// This method is used ONLY to construct the CFG once, at start up.
	// To load another configFile, use the instance load(File) method.
	private static MyPreferences open(final File configFile) {
		// if configFile !exist save the defaultConfig (above) to it
		if ( !configFile.exists() ) {
			try {
				IO.save(defaultConfig(), configFile);
			} catch (Exception ex) {
				// nb: ca not use Log here, coz it uses Config (chicken/egg)
				System.err.println("Config.open: failed to create: "+configFile);
				ex.printStackTrace(System.err);
			}
		}
		// load the configFile
		// @return a new instance of MyPreferences with configFile loaded;
		//  else (load failed) return an UNSPECIFIED Map (probably empty),
		//  and prefs.trouble is true.
		return new MyPreferences(configFile);
	}

	// modCount needs to be never 0
	private static int skip0(final int value) {
		if ( value == 0 )
			return 1;
		return value;
	}

	private static final String MODIFICATION_COUNT_KEY_TAG = "mod";

	/**
	 * The names of settings are public constants so that here is THE name, to
	 * keep it consistent. A spelling mistake cost me an hour, so I am now
	 * fully const-ipated. Now THAT's a ____ing dad joke.
	 * <p>
	 * Please add any/all new setting names here. It's nice to share.
	 */
	public static final String
		// booleans
		  isAntialiasing = "isAntialiasing" // GUI painting
		, isShowingMaybes = "isShowingMaybes" // GUI little grey digits
		, isFilteringHints = "isFilteringHints" // GUI hide extraneious hints
		, isHacky = "isHacky" // enables top1465 nasty special treatments
		, isGreenFlash = "isGreenFlash" // GUI flashBackgroundAqua
		, isInvalidGridHandled = "isInvalidGridHandled" // GUI attempts to handle an invalid grid, after a dodgy hint has been applied
		, isCachingHints = "isCachingHints" // GUI uses cacheHints to encache hints before getAllHints is even run by the user, to (mostly, yes NestedUnary I am looking at you) make the GUI snappy when you step through solving a puzzle by just repeatedly pressing enter.
		// ints
		, maxFins = "maxFins"
		, maxEndofins = "maxEndofins"
		, hintsCacheSize = "hintsCacheSize"
		, hintsCacheBored = "hintsCacheBored"
		// strings
		, logToFollow = "logToFollow"
	;

	/**
	 * The Singleton instance of all Techs.
	 */
	public static final EnumSet<Tech> ALL_TECHS = EnumSet.allOf(Tech.class);

	// DATE and TIME formats for the project.
	// eg: 2019-10-03.07-34-17 meaning that its thirty-four minutes past seven
	// oclock in the morning on Thursday, October the third in the year of our
	// halfwitted hot-crossed pillock two thousand and nineteen, but its OK.
	// Ive got my teeth back-in now! And I dont want to Chineese either.
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

	/**
	 * startDateTime is called <b>ONCE</b> during application start-up!
	 *
	 * @return the start-date-time in {@link #DATE_TIME_FORMAT}
	 */
	public static String startDateTime() {
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

	/**
	 * The Singleton instance of Sudoku Explainers configuration.
	 */
	public static final Config CFG = new Config(IO.CONFIG_FILE);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~ instance stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private MyPreferences prefs;

	private EnumSet<Tech> wantedTechs = EnumSet.noneOf(Tech.class);

	// modification count
	private int modCount = Integer.MIN_VALUE; // default only

	// Config is a singleton, so its only constructor is private.
	private Config(final File configFile) {
		// NEVER null, but empty if load fails
		prefs = open(configFile); // a Map<String,String>
		if ( !prefs.loadFailed )
			load();
	}

	/**
	 * Load the given $configFile into this instance of Config.
	 *
	 * @param configFile
	 * @return success (false means there was exception/s)
	 */
	public boolean load(final File configFile) {
		prefs = new MyPreferences(configFile);
		if ( !prefs.loadFailed )
			load();
		return !prefs.loadFailed;
	}

	public Rectangle getBounds() {
		return new Rectangle(
			  prefs.getInt("x", 0)
			, prefs.getInt("y", 0)
			, prefs.getInt("width", 3080)
			, prefs.getInt("height", 1080)
		);
	}

	public void setBounds(final Rectangle bounds) {
		if ( prefs == null )
			return;
		try {
			prefs.putInt("x", bounds.x);
			prefs.putInt("y", bounds.y);
			prefs.putInt("width", bounds.width);
			prefs.putInt("height", bounds.height);
			prefs.flush();
		} catch (BackingStoreException ex) {
			StdErr.whinge("WARN: "+Log.me()+" BackingStoreException", ex);
		}
	}

	public boolean getBoolean(final String name) {
		return getBoolean(name, false); // the default default is false.
	}

	// this get method (with defualt) is currently only used by the set method.
	public boolean getBoolean(final String name, final boolean defualt) {
		return prefs.getBoolean(name, defualt);
	}

	public String getString(final String name) {
		return getString(name, null); // the default default is null.
	}

	public String getString(final String name, final String defualt) {
		return prefs.get(name, defualt);
	}

	public String setString(final String name, final String value) {
		return prefs.put(name, value);
	}

	// if the registry entry does not exist yet we return value, as well as set
	// the registry entry to value; which is a bit odd, but it works for me.
	public boolean setBoolean(final String name, final boolean value) {
		boolean pre = getBoolean(name, value);
		prefs.putBoolean(name, value);
		return pre;
	}

	public int getInt(final String name, final int defualt) {
		return prefs.getInt(name, defualt);
	}

	public void putInt(final String name, final int value) {
		++modCount;
		prefs.putInt(name, value);
	}

	public int[] getIntArray(final String name, final int[] defualt) {
		return prefs.getIntArray(name, defualt);
	}

	public void putIntArray(final String name, final int[] values) {
		++modCount;
		prefs.putIntArray(name, values);
	}

	/** @return CFG.getBoolean(Config.isFilteringHints, true) */
	public boolean isFilteringHints() {
		return getBoolean(isFilteringHints, true);
	}

	/** @return CFG.setBoolean(Config.isFilteringHints, b) */
	public boolean setIsFilteringHints(final boolean b) {
		return setBoolean(isFilteringHints, b);
	}

	private static final String GENERATE_DIFFICULTY_TAG = "generateDifficulty";
	public Difficulty getGenerateDifficulty(final Difficulty defualt) {
		return Difficulty.values()[getInt(GENERATE_DIFFICULTY_TAG, defualt.ordinal())];
	}

	public Difficulty setGenerateDifficulty(final Difficulty d) {
		Difficulty old = getGenerateDifficulty(d);
		putInt(GENERATE_DIFFICULTY_TAG, d.ordinal());
		return old;
	}

	/**
	 * -1=NONE, 0=BASIC, 1=FRANKEN, 2=MUTANT
	 * @return the maximum kraken fishType
	 */
	public int maxKrakenType() {
		return getInt("maxKrakenType", 2); // MUTANT_FTYPE
	}

	/**
	 * Get an int-array of the max sizes for each of the three types of complex
	 * fish: BASIC, FRANKEN, MUTANT.
	 *
	 * @return a new {@code int[]} of the maximum size of each type of Kraken
	 *  Fish. {4,3,2} for example.
	 */
	public int[] maxKrakenSizes() {
		return getIntArray("maxKrakenSizes", null);
	}

	public boolean wantAnyBasicFish() {
		return getBoolean(Tech.Swampfish.name(), false)
			|| getBoolean(Tech.Swordfish.name(), false)
			|| getBoolean(Tech.Jellyfish.name(), false);
	}

	public void unwant(final Tech t) {
		wts = null;
		wantedTechs.remove(t);
	}

	public int getWantedTechsSize() {
		return wantedTechs.size();
	}

	public EnumSet<Tech> getWantedTechs() {
		return EnumSet.copyOf(wantedTechs);
	}

	private boolean[] wts;
	public boolean isWanted(Tech t) {
		if ( wts == null ) {
			wts = new boolean[ALL_TECHS.size()];
			for ( Tech wt : wantedTechs )
				wts[wt.ordinal()] = true;
		}
		return wts[t.ordinal()];
	}

	public EnumSet<Tech> setWantedTechs(final EnumSet<Tech> techs) {
		++modCount;
		EnumSet<Tech> pre = EnumSet.copyOf(wantedTechs);
		wantedTechs = techs;
		return pre;
	}

	/**
	 * For use only in the TechSelectDialog. Set wantedTechs and save them to
	 * the preferences file; in an all or nothing operation (hopefully).
	 *
	 * @param techs to save
	 */
	public void setAndSaveWantedTechs(final EnumSet<Tech> techs) {
		if ( techs == null )
			throw new NullPointerException("techs are null!");
		if ( prefs == null )
			throw new NullPointerException("preferences are null!");
		try {
			wantedTechs = techs;
			// push all Techs into prefs
			for ( Tech t : ALL_TECHS )
				prefs.putBoolean(t.name(), isWanted(t));
			try {
				prefs.flush();
			} catch (BackingStoreException ex) {
				StdErr.whinge("WARN: "+Log.me()+" BackingStoreException", ex);
			}
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	/**
	 * Are all of the given techs wanted?
	 *
	 * @param techs to examine
	 * @return are they all wanted?
	 */
	public boolean allWanted(final Iterable<Tech> techs) {
		for ( Tech tech : techs )
			if ( !isWanted(tech) )
				return false;
		return true;
	}

	/**
	 * Are any of the given techs wanted?
	 *
	 * @param techs to examine
	 * @return any wanted?
	 */
	public boolean anyWanted(final Iterable<Tech> techs) {
		for ( Tech tech : techs )
			if ( isWanted(tech) )
				return true;
		return false;
	}

	/**
	 * Get the Modification Count: this number is incremented whenever the
	 * Config are saved to the underlying Preferences (Windows registry).
	 * The Config are saved when you press OK the TechSelectDialog, and when
	 * the GUI closes.
	 * <p>
	 * Note that the main window location is also saved in registry, but it
	 * does not count as a Setting, so modCount does not change upon save.
	 * <p>
	 * ModCount persists between runs. It starts at Integer.MIN_VALUE, which is
	 * -2,147,483,648 and 0 (if anybody ever gets that far) is skipped, so that
	 * modCount will not collide with the default value for ints.
	 *
	 * @return the modification count of these Config.
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
			modCount = prefs.getInt(MODIFICATION_COUNT_KEY_TAG, modCount);
//			System.out.println("Config.load: modCount="+modCount);
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	public void save() {
		// usages: Test-cases use static CFG so Netbeans misses there
		// usages of save(). This is another way around the problem.
		// To be clear: test-cases should never CFG.save() and therefore
		// must avoid code that calls CFG.save(), like config dialogs.
		if ( false ) {
			new Throwable().printStackTrace(System.out);
			beep();
		}
		if ( prefs == null )
			throw new NullPointerException("preferences are null!");
		try {
			// save wantedTechs into prefs
			for ( Tech t : ALL_TECHS )
				prefs.putBoolean(t.name(), isWanted(t));
			// increment the modification count
			prefs.putInt(MODIFICATION_COUNT_KEY_TAG, skip0(++modCount));
			try {
				prefs.flush();
			} catch (BackingStoreException ex) {
				StdErr.whinge("WARN: "+Log.me()+" BackingStoreException", ex);
			}
		} catch (SecurityException ex) {
			// Maybe we are running from an applet. Do nothing
		}
	}

	public void close() {
//		save();
	}

	/**
	 * Does $obj contain the same Preferences as this Config?
	 * NOTE: equals compares Preferences only, so if any future setting is
	 * persisted elsewhere, then you will also need to compare them here.
	 *
	 * @param obj to compare
	 * @return are preferences equals
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj != null
			&& (obj instanceof Config)
			&& equals((Config)obj);
	}

	/**
	 * Does the given Config contain the same Preferences as this Config?
	 * <p>
	 * nb: I was preferences.equals(other.preferences) but it produced results
	 * that I am too thick to understand, so I DIY. This I understand. The old
	 * version was diffing XML that I am too lazy to produce just for equals.
	 * <p>
	 * This method is now not AFAIK used, atleast LogicalSolverFactor.recreate
	 * no-longer relies upon it; it just recreates the bastard coz it was told
	 * to, and if its the same as the old one then tough, which may be a waste
	 * but at-least its a SIMPLE waste. sigh.
	 *
	 * @param other the other Config to compare this to.
	 * @return {@code preferences.equals(other.preferences)} delegating down to
	 * AbstractCollection equals
	 */
	public boolean equals(final Config other) {
		if ( other == this )
			return true;
		if ( other == null )
			return false;
		// are they different versions?
		if ( other.modCount != modCount )
			return false;
		final MyPreferences me = prefs; // NEVER null, possibly empty!
		final MyPreferences him = other.prefs; // NEVER null, possibly empty!
		if ( me.size() != him.size() )
			return false;
		for ( Map.Entry<String,String> e : me.entrySet() )
			if ( !e.getValue().equals(him.get(e.getKey())) )
				return false;
		return true;
	}

	/**
	 * Returns the identity hashCode: ie Object.hashCode(), to guarantee that
	 * a Config object always reports the same hashCode to Hash*, breaking
	 * the spirit of the equals-hashCode contract! That the hashCode can and
	 * probably should change when the fields compared by equals change.
	 * <p>
	 * I do not expect this to cause issues because you would not create a Hash
	 * of Config! There is one Config per app! Hence the hashCode debate is
	 * a ____ing waste of brain.
	 * @return super.hashCode();
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Config copy = (Config)super.clone();
		return copy;
	}

}
