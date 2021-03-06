package com.js.rbuddy.test;

import java.util.ArrayList;
import java.util.List;

import com.js.basic.Tools;
import com.js.json.JSONEncoder;
import com.js.rbuddy.JSDate;
import com.js.rbuddy.Cost;
import com.js.rbuddy.Receipt;
import com.js.rbuddy.ReceiptFilter;
import com.js.rbuddy.TagSet;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTest;
import static com.js.basic.Tools.*;

public class ReceiptFilterTest extends MyTest {

	@Override
	protected void setUp() {
		super.setUp();
		Tools.seedRandom(1965); // ensure consistent random numbers
	}

	private List<Receipt> generateReceipts() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < 50; i++) {
			list.add(Receipt.buildRandom(1 + i));
		}
		return list;
	}

	private void applyTestFilter(ReceiptFilter rf) {
		applyTestFilter(rf, false);
	}

	private void applyTestFilter(ReceiptFilter rf,
			boolean replaceExistingSnapshot) {

		// TODO: get finally working properly, and discard snapshot if
		// exception?
		List<Receipt> receipts = generateReceipts();
		IOSnapshot.open(replaceExistingSnapshot);
		pr(JSONEncoder.toJSON(rf));
		for (Receipt r : receipts) {
			boolean pass = rf.apply(r);
			pr((pass ? "YES      " : "         ") + r);
		}
		IOSnapshot.close();
	}

	public void testMinCostFilter() {
		ReceiptFilter rf = new ReceiptFilter();
		rf.setMinCostActive(true);
		rf.setMinCost(new Cost(10.0));
		applyTestFilter(rf);
	}

	public void testNoConditionsFilter() {
		ReceiptFilter rf = new ReceiptFilter();
		rf.setMinCostActive(false);
		applyTestFilter(rf);
	}

	public void testMissingMinCostFails() {
		ReceiptFilter rf = new ReceiptFilter();
		rf.setMinCostActive(true); // but note we are not defining a min cost
		try {
			applyTestFilter(rf);
			failMissingException();
		} catch (NullPointerException e) {
		}
	}

	public void testMinDateFilter() {
		ReceiptFilter rf = new ReceiptFilter();
		rf.setMinDateActive(true);
		rf.setMinDate(JSDate.buildFromValues(2010, 10, 20));
		applyTestFilter(rf);
	}

	/**
	 * Build filter, store in instance field 'f'; does nothing if already built
	 * 
	 * @return filter f
	 */
	private ReceiptFilter f() {
		if (f == null) {
			f = new ReceiptFilter();
		}
		return f;
	}

	public void testStoresTagSet() {
		TagSet s = TagSet.parse("alpha,bravo,charlie delta,epsilon");

		f();
		f.setInclusiveTags(s);
		assertEquals(s.size(), f.getInclusiveTags().size());
	}

	public void testFiltering() {
		
		ReceiptFilter rf = new ReceiptFilter();
		
		JSDate min_date = JSDate.buildFromValues(2014, 4, 4);
		rf.setMinDate(min_date);
		rf.setMinDateActive(true);

		JSDate max_date = JSDate.buildFromValues(2014, 7, 7);
		rf.setMaxDate(max_date);
		rf.setMaxDateActive(true);

		Cost min_cost = new Cost("123.45");
		rf.setMinCost(min_cost);
		rf.setMinCostActive(true);

		Cost max_cost = new Cost("678.90");
		rf.setMaxCost(max_cost);
		rf.setMaxCostActive(true);

		TagSet inc_ts = TagSet.parse("Florida,Georgia,Alabama");
		rf.setInclusiveTags(inc_ts);
		rf.setInclusiveTagsActive(true);

		TagSet exc_ts = TagSet.parse("Florida");
		rf.setExclusiveTags(exc_ts);
		rf.setExclusiveTagsActive(true);

		Receipt r= new Receipt(1);
		
		JSDate test_date = JSDate.buildFromValues(2014, 5, 12);
		r.setDate(test_date);
		
		Cost test_cost = new Cost("500");
		r.setCost(test_cost);
		
		TagSet test_ts = TagSet.parse("Florida");
		r.setTags(test_ts);
		
		assertTrue(rf.apply(r));
		
	}

	private ReceiptFilter f;
}
