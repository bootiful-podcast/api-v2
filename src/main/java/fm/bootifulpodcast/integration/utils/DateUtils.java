package fm.bootifulpodcast.integration.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public abstract class DateUtils {

	public static int getYearFor(Date d) {
		return DateUtils.getCalendarFor(d).get(Calendar.YEAR);
	}

	public static Calendar getCalendarFor(Date d) {
		var c = Calendar.getInstance();
		c.setTime(d);
		return c;
	}

	public static Calendar getCurrentCalendar() {
		return getCalendarFor(new Date());
	}

	public static DateFormat dateAndTime() {
		return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
	}

	public static DateFormat date() {
		return new SimpleDateFormat("MM/dd/yyyy");
	}

}
