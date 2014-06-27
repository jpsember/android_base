package js.rbuddyapp;

import static js.basic.Tools.*;

import java.util.*;

import js.form.FormWidget;
import js.rbuddy.R;
import js.rbuddy.Receipt;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class ReceiptListActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = RBuddyApp.sharedInstance();

		LinearLayout layout = new LinearLayout(this);
		FormWidget.setDebugBgnd(layout, app.useGoogleAPI() ? "#000030"
				: "#004000");
		layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		layout.addView(constructListView());
	}

	@Override
	public void onResume() {
		super.onResume();

		if (editReceipt != null) {
			if (!app.receiptFile().exists(editReceipt.getId())) {
				invalidateReceiptList();
			}
		}
		if (!receiptListValid)
			rebuildReceiptList(this.receiptList);

		refreshEditedReceipt();
	}

	@Override
	protected void onPause() {
		super.onPause();
		app.receiptFile().flush();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.receiptlist_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			unimp("settings");
			return true;
		case R.id.action_add:
			processAddReceipt();
			return true;
		case R.id.action_testonly_generate:
			processGenerate();
			return true;
		case R.id.action_testonly_zap:
			processZap();
			return true;
		case R.id.action_testonly_toggle_photo_delay:
			AppPreferences.toggle(IPhotoStore.PREFERENCE_KEY_PHOTO_DELAY);
			return true;
		case R.id.action_testonly_enable_google_drive:
			AppPreferences.putBoolean(
					RBuddyApp.PREFERENCE_KEY_USE_GOOGLE_DRIVE_API, true);
			showGoogleDriveState();
			return true;
		case R.id.action_testonly_disable_google_drive:
			AppPreferences.putBoolean(
					RBuddyApp.PREFERENCE_KEY_USE_GOOGLE_DRIVE_API, false);
			showGoogleDriveState();
			return true;
		case R.id.action_search:
			doSearchActivity();
			return true;
		case R.id.action_testonly_exit:
			android.os.Process.killProcess(android.os.Process.myPid());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showGoogleDriveState() {
		app.toast("Google Drive is "
				+ (app.useGoogleAPI() ? "active" : "inactive")
				+ ", and will be "
				+ (AppPreferences.getBoolean(
						RBuddyApp.PREFERENCE_KEY_USE_GOOGLE_DRIVE_API, true) ? "active"
						: "inactive") + " when app restarts.");
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (false /* app.useGoogleAPI() */) {
			menu.removeItem(R.id.action_testonly_generate);
			menu.removeItem(R.id.action_testonly_zap);
		}
		unimp("it would be nice to be able to programmatically change labels, e.g., to indicate current value of togglable preference");
		return super.onPrepareOptionsMenu(menu);
	}

	private void processGenerate() {
		RBuddyApp.confirmOperation(this, "Generate some random receipts?",
				new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < 30; i++) {
							int id = app.receiptFile().allocateUniqueId();
							Receipt r = Receipt.buildRandom(id);
							app.receiptFile().add(r);
						}
						rebuildReceiptList(receiptList);
						receiptListAdapter.notifyDataSetChanged();
					}
				});
	}

	private void processZap() {
		RBuddyApp.confirmOperation(this, "Delete all receipts?",
				new Runnable() {
					@Override
					public void run() {
						app.receiptFile().clear();
						rebuildReceiptList(receiptList);
					}
				});
	}

	private List buildListOfReceipts() {
		ArrayList list = new ArrayList();
		rebuildReceiptList(list);
		return list;
	}

	private void invalidateReceiptList() {
		receiptListValid = false;
		refreshReceiptAtPosition = null;
	}

	private void rebuildReceiptList(List list) {
		list.clear();
		for (Iterator it = app.receiptFile().iterator(); it.hasNext();)
			list.add(it.next());
		Collections.sort(list, Receipt.COMPARATOR_SORT_BY_DATE);
		receiptListValid = true;

		if (receiptListAdapter != null)
			receiptListAdapter.notifyDataSetChanged();
	}

	// Construct a view to be used for the list items
	private View constructListView() {

		ListView listView = new ListView(this);

		List receiptList = buildListOfReceipts();
		ArrayAdapter arrayAdapter = new ReceiptListAdapter(this, receiptList);
		listView.setAdapter(arrayAdapter);

		// Store references to both the ArrayAdapter and the backing ArrayList,
		// to make responding to selection actions more convenient.
		this.receiptListAdapter = arrayAdapter;
		this.receiptList = receiptList;
		this.receiptListView = listView;

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView aView, View v, int position,
					long id) {
				processReceiptSelection(position);
			}
		});
		LayoutParams layoutParam = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		listView.setLayoutParams(layoutParam);

		return listView;
	}

	private void processAddReceipt() {
		Receipt r = new Receipt(app.receiptFile().allocateUniqueId());
		app.receiptFile().add(r);
		invalidateReceiptList();
		doEditReceipt(r);
	}

	private void doEditReceipt(Receipt receipt) {
		this.editReceipt = receipt;
		Intent intent = new Intent(getApplicationContext(),
				EditReceiptActivity.class);
		intent.putExtra(RBuddyApp.EXTRA_RECEIPT_ID, editReceipt.getId());
		startActivity(intent);
	}

	/**
	 * Process user selecting receipt from receipt list
	 * 
	 * @param position
	 */
	private void processReceiptSelection(int position) {
		// We will need to refresh this item when returning from the edit
		// activity in case its contents have changed
		refreshReceiptAtPosition = position;
		doEditReceipt(receiptListAdapter.getItem(position));
	}

	private void refreshEditedReceipt() {
		if (refreshReceiptAtPosition == null)
			return;
		int receiptPosition = refreshReceiptAtPosition;
		refreshReceiptAtPosition = null;

		int visiblePosition = receiptListView.getFirstVisiblePosition();
		View view = receiptListView.getChildAt(receiptPosition
				- visiblePosition);
		if (view == null)
			return;

		// This apparently updates the view's fields, which induces its update
		// on the screen
		receiptListAdapter.getView(receiptPosition, view, receiptListView);
	}

	private void doSearchActivity() {
		Intent intent = new Intent(getApplicationContext(),
				SearchActivity.class);
		if (false) {
			warning("trying out experimental activity instead");
			intent = new Intent(getApplicationContext(),
					ExperimentalActivity.class);
		}

		startActivity(intent);
	}

	// If false, we assume the current receipt list (if one exists) is invalid,
	// and a new one needs to be built
	private boolean receiptListValid;

	// If non-null, we refresh the receipt at this position in the list when
	// resuming the activity
	private Integer refreshReceiptAtPosition;

	// If non-null, this receipt was just edited
	private Receipt editReceipt;

	private ArrayAdapter<Receipt> receiptListAdapter;
	private List receiptList;
	private RBuddyApp app;
	private ListView receiptListView;
}
