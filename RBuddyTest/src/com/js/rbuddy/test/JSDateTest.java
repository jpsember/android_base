package com.js.rbuddy.test;

import com.js.rbuddy.JSDate;

public class JSDateTest extends com.js.testUtils.MyTest {

	public void testCurrentDate() {
		JSDate d = JSDate.currentDate();

		String s = d.toString();
		JSDate d2 = JSDate.parse(s);
		assertStringsMatch(s, d2.toString());
	}

}
