package com.js.android;

import static com.js.android.Tools.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.js.rbuddy.JSDate;

import android.app.Activity;
import android.os.Bundle;

public abstract class MyActivity extends Activity {

	public static final String PREFERENCE_KEY_SMALL_DEVICE_FLAG = "small_device";

	public void setLogging(boolean f) {
		mLogging = f;
	}

	protected void log(Object message) {
		if (mLogging) {
			StringBuilder sb = new StringBuilder("===> ");
			sb.append(nameOf(this));
			sb.append(" : ");
			tab(sb, 30);
			sb.append(message);
			pr(sb);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!testing()) {
			prepareSystemOut();
		}
		JSDate.setFactory(AndroidDate.androidDateFactory(this));
		AppPreferences.prepare(this);
		addResourceMappings();
		log("onCreate savedInstanceState=" + nameOf(savedInstanceState));
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		log("onSaveInstanceState outState=" + nameOf(outState));
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		log("onPause");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		log("onDestroy");
		super.onDestroy();
	}

	/**
	 * Store fragment within FragmentReference, if one has been registered with
	 * this activity
	 * 
	 * @param f
	 *            fragment
	 */
	void registerFragment(MyFragment f) {
		log("registerFragment " + nameOf(f) + "(name " + f.getName() + ")");
		FragmentReference reference = mReferenceMap.get(f.getName());
		if (reference != null) {
			reference.setFragment(f);
		}
	}

	void addReference(FragmentReference reference) {
		mReferenceMap.put(reference.getName(), reference);
		reference.refresh();
	}

	public <T extends MyFragment> FragmentReference<T> buildFragment(
			Class fragmentClass) {
		FragmentReference<T> ref = new FragmentReference<T>(this, fragmentClass);
		return ref;
	}

	public void buildFragmentOrganizer() {
		if (mFragmentOrganizer != null)
			return;
		mFragmentOrganizer = new FragmentOrganizer(this);
	}

	public FragmentOrganizer getFragmentOrganizer() {
		return mFragmentOrganizer;
	}

	private void prepareSystemOut() {
		AndroidSystemOutFilter.install();
		if (sConsoleGreetingPrinted)
			return;
		sConsoleGreetingPrinted = true;

		// Print message about app starting. Print a bunch of newlines
		// to simulate clearing the console, and for convenience,
		// print the time of day so we can figure out if the
		// output is current or not.

		String strTime = "";
		{
			Calendar cal = Calendar.getInstance();
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
					"h:mm:ss", Locale.CANADA);
			strTime = sdf.format(cal.getTime());
		}
		for (int i = 0; i < 20; i++)
			pr("\n");
		pr("--------------- Start of " + this.getClass().getSimpleName()
				+ " ----- " + strTime + " -------------\n\n\n");
	}

	/**
	 * Store the resource id associated with the resource's name, so we can
	 * refer to them by name (for example, we want to be able to refer to them
	 * within JSON strings).
	 * 
	 * There are some facilities to do this mapping using reflection, but
	 * apparently it's really slow.
	 * 
	 * @param key
	 * @param resourceId
	 */
	public void addResource(String key, int resourceId) {
		mResourceMap.put(key, resourceId);
	}

	/**
	 * Get the resource id associated with a resource name (added earlier).
	 * 
	 * @param key
	 * @return resource id
	 * @throws IllegalArgumentException
	 *             if no mapping exists
	 */
	public int getResource(String key) {
		Integer id = mResourceMap.get(key);
		if (id == null)
			throw new IllegalArgumentException(
					"no resource id mapping found for " + key);
		return id.intValue();
	}

	private void addResourceMappings() {
		addResource("photo", android.R.drawable.ic_menu_gallery);
		addResource("camera", android.R.drawable.ic_menu_camera);
		addResource("search", android.R.drawable.ic_menu_search);
	}

	private Map<String, FragmentReference> mReferenceMap = new HashMap();
	private boolean mLogging;
	private FragmentOrganizer mFragmentOrganizer;
	private Map<String, Integer> mResourceMap = new HashMap();

	private static boolean sConsoleGreetingPrinted;
}
