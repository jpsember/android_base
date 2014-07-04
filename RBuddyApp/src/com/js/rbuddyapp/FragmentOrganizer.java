package com.js.rbuddyapp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import static com.js.android.Tools.*;

import com.js.android.AppPreferences;
import com.js.form.FormWidget;
import com.js.json.JSONEncoder;
import com.js.json.JSONParser;

/**
 * Class to encapsulate some of the complexity of dealing with fragments.
 * 
 * Conceptually, manages the display of up to two side-by-side fragments
 * 
 * 
 * Problems: [] back button doesn't update slotContents properly
 * 
 * 
 * [] some transactions should not be undoable: placing top-level fragments
 * (those visible if none other)
 * 
 * 
 */
public class FragmentOrganizer {

	// private static final int MAX_SLOTS = 2;
	private static final String BUNDLE_PERSISTENCE_KEY = "FragmentOrganizer";

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            parent activity
	 * @return
	 */
	public FragmentOrganizer(Activity parent) {
		log("Constructing for parent " + nameOf(parent));
		this.parent = parent;
		this.viewIdBase = 1900;
		numberOfSlots = 2;
		if (AppPreferences.getBoolean(
				RBuddyApp.PREFERENCE_KEY_SMALL_DEVICE_FLAG, false))
			numberOfSlots = 1;

		desiredSlotContents = new String[numberOfSlots];

		factories = new HashMap();
		// for (MyFragment.Factory f : fragmentTypes) {
		// register(f);
		// }
		if (false)
			dumpManager();
	}

	private void log(Object message) {
		if (mLogging) {
			StringBuilder sb = new StringBuilder("...> ");
			sb.append(nameOf(this));
			sb.append(" : ");
			tab(sb, 30);
			sb.append(message);
			pr(sb);
		}

	}

	public void setBaseViewId(int id) {
		this.viewIdBase = id;
	}

	public FragmentOrganizer register(MyFragment.Factory factory) {
		factories.put(factory.name(), factory);
		return this;
	}

	public void onCreate(Bundle savedInstanceState) {
		log("onCreate savedInstanceState=" + nameOf(savedInstanceState));

		// TODO also verify device is large enough
		supportsDual = (numberOfSlots > 1) && true;

		constructContainer();

		String json = null;
		if (savedInstanceState != null) {
			json = savedInstanceState.getString(BUNDLE_PERSISTENCE_KEY);
			if (db)
				pr(" restoring from JSON: " + json);
			if (json == null) {
				warning("JSON string was null!");
			} else
				restoreFromJSON(json);
		}
	}

	// private void removeAllFragments() {
	// FragmentManager m = parent.getFragmentManager();
	// for (int slot = 0; slot < numberOfSlots; slot++) {
	// MyFragment f = (MyFragment) m.findFragmentById(viewIdBase + slot);
	// if (f != null) {
	// if (db)
	// pr(" removing fragment " + f + " from slot " + slot);
	// removeFragment(f);
	// }
	// }
	// m.executePendingTransactions();
	// if (db)
	// pr("after removing slots; " + dumpManager());
	// }

	// private void removeFragment(Fragment f) {
	// FragmentManager m = parent.getFragmentManager();
	// FragmentTransaction transaction = m.beginTransaction();
	// transaction.remove(f);
	// transaction.commit();
	// }

	private void removeFragmentsFromOldViews() {
		FragmentManager m = parent.getFragmentManager();

		for (int slot = 0; slot < numberOfSlots; slot++) {
			Fragment oldFragment = m.findFragmentById(viewIdBase + slot);
			if (oldFragment == null)
				continue;
			FragmentTransaction transaction = m.beginTransaction();
			transaction.remove(oldFragment);
			transaction.commit();
			// MyFragment newFragment = get(name, true);
			// {
			// log(" adding fragment " + nameOf(newFragment) + " to slot "
			// + slot + " with name " + name);
			// FragmentTransaction transaction = m.beginTransaction();
			// transaction.add(viewIdBase + slot, newFragment, name);
			// transaction.commit();
			// }
		}
		m.executePendingTransactions();
	}

	private void restoreDesiredFragmentsToViews() {
		FragmentManager m = parent.getFragmentManager();

		for (int slot = 0; slot < numberOfSlots; slot++) {
			String name = desiredSlotContents[slot];
			if (name == null)
				continue;
			MyFragment newFragment = get(name, true);
			{
				log(" adding fragment " + nameOf(newFragment) + " to slot "
						+ slot + " with name " + name);
				FragmentTransaction transaction = m.beginTransaction();
				transaction.add(viewIdBase + slot, newFragment, name);
				transaction.commit();
			}
			//
			//
			// Fragment oldFragment = m.findFragmentById(viewIdBase + slot);
			// if (oldFragment == null)
			// continue;
			//
			//
			// FragmentTransaction transaction = m.beginTransaction();
			// transaction.remove(oldFragment);
			// transaction.commit();
		}
		m.executePendingTransactions();
	}

	public void onResume() {
		log("onResume");
		mIsResumed = true;

		// Remove any fragments that the manager is reporting are already
		// added, since they are probably added to old views and we have new
		// ones to populate
		removeFragmentsFromOldViews();

		restoreDesiredFragmentsToViews();
	}

	public void onPause() {
		log("onPause");
		// // Remove tags from existing fragments, so when resuming we don't
		// think they're assigned to any slots
		// for (int slot = 0; slot < numberOfSlots; slot++) {
		// MyFragment f = fragmentInSlot(slot);
		// if (f == null) continue;
		// }
	}

	/**
	 * Save organizer state within bundle
	 * 
	 * @param bundle
	 */
	public void onSaveInstanceState(Bundle bundle) {
		log("onSaveInstanceState bundle=" + nameOf(bundle));
		bundle.putString(BUNDLE_PERSISTENCE_KEY, encodeToJSON());
		mIsResumed = false;
	}

	/**
	 * Determine if device can display two fragments side-by-side instead of
	 * just one
	 * 
	 * @return
	 */
	public boolean supportDualFragments() {
		return this.supportsDual;
	}

	/**
	 * Get the view that is to contain the fragments
	 * 
	 * @return
	 */
	public View getView() {
		log("getView, returning " + describe(container));
		return container;
	}

	private MyFragment fragmentInSlot(int slot) {
		FragmentManager m = parent.getFragmentManager();
		MyFragment f = (MyFragment) m.findFragmentById(viewIdBase + slot);

		// We can't seem to trust what the fragmentManager says; so if there is
		// no view in that slot,
		// assume no fragment?
		if (container.getChildCount() <= slot) {
			if (f != null)
				// die("fragment manager reporting frag " + f + " in slot " +
				// slot
				// + ", but no view exists there");
				return null;
		}

		return f;
	}

	private static String fragmentName(Fragment f) {
		String tag = null;
		if (f != null) {
			tag = f.getTag();
			if (tag == null)
				throw new IllegalStateException("fragment has no tag");
		}
		return tag;
	}

	private int slotContainingFragment(String fragmentName) {
		for (int s = 0; s < numberOfSlots; s++) {
			MyFragment f = fragmentInSlot(s);
			if (f == null)
				continue;
			if (f.getTag().equals(fragmentName))
				return s;
		}
		return -1;
	}

	/**
	 * Display a fragment within a slot
	 * 
	 * @param tag
	 *            name of fragment, or null to clear the slot
	 * @param slot
	 *            preferred slot; if greater than max displayed, uses 0
	 * @return if tag isn't null and in resumed state, the fragment displayed;
	 *         else null
	 */
	public MyFragment plot(String tag, boolean primary, boolean undoable) {
		int slot = primary ? 0 : 1;

		log("plot tag=" + tag + " slot=" + slot + " undoable=" + d(undoable));

		if (slot > 0 && !supportDualFragments())
			slot = 0;

		if (!isResumed()) {
			desiredSlotContents[slot] = tag;
			return null;
		}

		String oldName = null;
		MyFragment oldFragment = null;
		oldFragment = fragmentInSlot(slot);
		if (oldFragment != null)
			oldName = oldFragment.getTag();

		String newName = null;
		MyFragment newFragment = null;
		if (tag != null) {
			newName = tag;
			newFragment = get(tag, true);
		}
		FragmentManager m = parent.getFragmentManager();

		if (db)
			pr("oldName: " + oldName + " oldFragment: " + describe(oldFragment)
					+ "\nnewFragment: " + describe(newFragment));

		if (oldFragment != newFragment) {
			if (newName != null) {
				// If new fragment exists in another slot, just use that one
				int actualSlot = slotContainingFragment(newName);
				if (actualSlot >= 0) {
					return newFragment;
				}
			}

			do {
				FragmentTransaction transaction = m.beginTransaction();
				if (oldFragment == null) {
					if (db)
						pr("adding " + newName + " to slot " + slot);
					transaction.add(viewIdBase + slot, newFragment, newName);
				} else if (newFragment == null) {
					transaction.remove(oldFragment);
				} else {
					if (db)
						pr("replacing " + oldName + " with " + newName
								+ " in slot " + slot);

					transaction
							.replace(viewIdBase + slot, newFragment, newName);
				}
				if (undoable)
					transaction.addToBackStack(null);
				transaction.commit();

			} while (false);

			// setViewWithinSlot(getSlotView(slot), slot);
			// buildSlotView(slot), slot);
		}
		return newFragment;
	}

	/**
	 * Encode state to JSON string
	 * 
	 * @return JSON string
	 */
	private String encodeToJSON() {
		JSONEncoder json = new JSONEncoder();
		json.enterList();
		for (int slot = 0; slot < numberOfSlots; slot++) {
			String name = fragmentName(fragmentInSlot(slot));
			if (name == null)
				name = "";
			json.encode(name);
		}
		json.exit();
		return json.toString();
	}

	/**
	 * Restore state from JSON string
	 * 
	 * @param jsonString
	 */
	private void restoreFromJSON(String jsonString) {
		JSONParser p = new JSONParser(jsonString);
		p.enterList();
		desiredSlotContents = new String[numberOfSlots];
		for (int slot = 0; slot < numberOfSlots; slot++) {
			String name = p.nextString();
			if (name.isEmpty())
				name = null;
			desiredSlotContents[slot] = name;
		}
		p.exit();
	}

	private void constructContainer() {
		// Create view with two panels side by side
		LinearLayout layout = new LinearLayout(parent);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		FormWidget.setDebugBgnd(layout, "#602020");
		container = layout;
		log("set container to  " + nameOf(container));

		for (int slot = 0; slot < numberOfSlots; slot++) {
			View v = buildSlotView(slot);
			log(" adding view " + describe(v) + " to slot " + slot);
			container.addView(v, slot, new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
		}
	}

	private ViewGroup buildSlotView(int slot) {
		FrameLayout f2 = new FrameLayout(parent);
		f2.setId(viewIdBase + slot);
		FormWidget.setDebugBgnd(f2, (slot == 0) ? "#206020" : "#202060");
		log("built slot#" + slot + " view " + describe(f2));
		return f2;
	}

	/**
	 * Get fragment; construct if necessary
	 * 
	 * @param factory
	 * @param constructIfMissing
	 *            if true, and no such fragment found, a new one is constructed
	 * @return fragment, or null
	 */
	private MyFragment get(String tag, boolean constructIfMissing) {
		FragmentManager m = parent.getFragmentManager();
		MyFragment f = (MyFragment) m.findFragmentByTag(tag);
		if (f == null && constructIfMissing) {
			f = factory(tag).construct();
			if (db)
				pr("\n  ======================= Constructed instance of " + tag
						+ "\n");
		}
		return f;
	}

	private MyFragment.Factory factory(String tag) {
		MyFragment.Factory f = factories.get(tag);
		if (f == null)
			throw new IllegalArgumentException("no factory registered for: "
					+ tag);
		return f;
	}

	private String dumpManager() {
		FragmentManager m = parent.getFragmentManager();
		if (false) {
			StringWriter w = new StringWriter();
			m.dump("Mgr:", null, new PrintWriter(w), null);
			return w.toString();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Manager fragments:");
		for (String tag : factories.keySet()) {
			MyFragment f = (MyFragment) m.findFragmentByTag(tag);
			if (f == null)
				continue;
			sb.append("\n  " + tag + " : " + f);
		}
		sb.append("\n");
		return sb.toString();
	}

	public String info(MyFragment f) {
		if (f == null)
			return "<null>";
		StringBuilder sb = new StringBuilder(nameOf(f));
		sb.append(" [");
		if (f.isAdded())
			sb.append(" added");
		if (f.isDetached())
			sb.append(" detached");
		if (f.isHidden())
			sb.append(" hidden");
		if (f.isInLayout())
			sb.append(" inLayout");
		if (f.isVisible())
			sb.append(" visible");
		sb.append(" ]");
		return sb.toString();
	}

	private boolean isResumed() {
		return mIsResumed;
	}

	// Map of factories, keyed by tag
	private Map<String, MyFragment.Factory> factories;
	private Activity parent;
	private LinearLayout container;
	private int viewIdBase;
	private String[] desiredSlotContents;

	private boolean mIsResumed;
	private boolean supportsDual;
	private int numberOfSlots;
	private boolean mLogging = true;
}
