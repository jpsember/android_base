package js.rbuddy;

import java.util.Date;

import static js.basic.Tools.*;

public class Receipt {

	/**
	 * Constructor
	 * 
	 * Sets date to current date
	 */
	public Receipt() {
	}

	public Date getDate() {
		return this.date;
	}

	public void setDate(Date date) {
		warning("Java's Date class has lots of problems; look for alternate?");
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String s) {

		s = s.trim();

		int state = 1;
		StringBuilder s_result = new StringBuilder();

		int i_pos;
		for (i_pos = 0; i_pos < s.length(); i_pos++) {

			char c_thischar = s.charAt(i_pos);

			boolean b_is_whitespace = (c_thischar <= ' ');

			switch (state) {

			// last character was not whitespace
			case 1:
				// but this one is...
				if (b_is_whitespace) {
					state = 2;
				} else {
					s_result.append(c_thischar);
					state = 1;
				}
				break;

			// last character was whitespace
			case 2:
				// and so is this one...
				if (b_is_whitespace) {
					state = 2;
				} else {
					s_result.append(' ');
					s_result.append(c_thischar);
					state = 1;
				}
				break;
			}

		}

		summary = s_result.toString();

	}

	@Override
	public String toString() {
		return "Receipt summary='" + summary + "'";
	}

	private Date date;
	private String summary;

}