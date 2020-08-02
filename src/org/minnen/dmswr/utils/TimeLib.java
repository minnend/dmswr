package org.minnen.dmswr.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.minnen.dmswr.utils.Sequence;

public class TimeLib
{
  public final static long              TIME_ERROR = Library.LNAN;
  public final static long              TIME_BEGIN = Long.MIN_VALUE + 1;
  public final static long              TIME_END   = Long.MAX_VALUE - 1;
  public final static long              MS_IN_HOUR = 60 * 60 * 1000L;
  public final static long              MS_IN_DAY  = 24 * MS_IN_HOUR;

  public final static ZoneOffset        ZeroOffset = ZoneOffset.ofTotalSeconds(0);

  public final static DateTimeFormatter dtfTime    = DateTimeFormatter.ofPattern("yyyy MMM d hh:mm:ss a");
  public final static DateTimeFormatter dtfTimeSig = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
  public final static DateTimeFormatter dtfDate    = DateTimeFormatter.ofPattern("d MMM yyyy");
  public final static DateTimeFormatter dtfDate2   = DateTimeFormatter.ofPattern("dd MMM yyyy");
  public final static DateTimeFormatter dtfMonth   = DateTimeFormatter.ofPattern("MMM yyyy");
  public final static DateTimeFormatter dtfYMD     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public final static DateTimeFormatter dtfYM      = DateTimeFormatter.ofPattern("yyyy-MM");  
  public final static DateTimeFormatter dtfY       = DateTimeFormatter.ofPattern("yyyy");

  /**
   * Return the current time in milliseconds. This function just forwards the request to System.currentTimeMillis();
   * 
   * @return the current time in milliseconds (since midnight Jan 1, 1970, UTC)
   */
  public static long getTime()
  {
    return System.currentTimeMillis();
  }

  /**
   * @return current time (UTC) with first day of week set to sunday
   */
  public static Instant now()
  {
    return Instant.ofEpochMilli(getTime());
  }

  public static Instant ms2instance(long ms)
  {
    return Instant.ofEpochMilli(ms);
  }

  public static LocalDateTime ms2time(long ms)
  {
    return ms2time(ms, ZeroOffset);
  }

  public static LocalDateTime ms2time(long ms, ZoneId zone)
  {
    return LocalDateTime.ofInstant(ms2instance(ms), zone == null ? ZeroOffset : zone);
  }

  public static LocalDate ms2date(long ms)
  {
    return LocalDate.from(ms2time(ms));
  }

  /** @return time in ms for noon on the given date. */
  public static long toMs(LocalDate date)
  {
    return toMs(LocalDateTime.of(date, LocalTime.NOON));
  }

  public static long toMs(LocalDateTime time)
  {
    return time.toInstant(ZeroOffset).toEpochMilli();
  }

  /** @return time in ms for noon on the given year, month, day (January = 1) */
  public static long toMs(int year, int month, int day)
  {
    return toMs(LocalDate.of(year, month, day));
  }

  /** @return time in ms for noon on the given date. */
  public static long toMs(int year, Month month, int day)
  {
    return toMs(LocalDate.of(year, month, day));
  }

  /** @return fractional years between the given times. */
  public static double yearsBetween(long t1, long t2)
  {
    return monthsBetween(t1, t2) / 12.0;
  }

  /** @return fractional months between the given times. */
  public static double monthsBetween(long t1, long t2)
  {
    assert t1 != TimeLib.TIME_ERROR && t2 != TimeLib.TIME_ERROR;
    if (t2 < t1) {
      long tmp = t1;
      t1 = t2;
      t2 = tmp;
    }
    assert t1 <= t2;
    LocalDate d1 = ms2date(t1);
    LocalDate d2 = ms2date(t2);
    long nWholeMonths = ChronoUnit.MONTHS.between(d1, d2);
    LocalDate d1a = d1.plusMonths(nWholeMonths);
    if (d1a.isEqual(d2)) return nWholeMonths;

    LocalDate d1b = d1.plusMonths(nWholeMonths + 1);
    if (d1b.isEqual(d2)) return nWholeMonths + 1;
    assert d1a.isBefore(d2);
    assert d1b.isAfter(d2);

    long nDays1 = ChronoUnit.DAYS.between(d1a, d2);
    assert nDays1 > 0;
    long nDays2 = ChronoUnit.DAYS.between(d2, d1b);
    assert nDays2 > 0;

    long nDays = nDays1 + nDays2;
    return nWholeMonths + (double) nDays1 / nDays;
  }

  /**
   * Returns index corresponding to first entry of new decade (e.g. January 1880).
   * 
   * @param seq Sequence with timestamps
   * @return index for first decade or -1 if none
   */
  public static int findStartofFirstDecade(Sequence seq, boolean bCheckDate)
  {
    // Find start of decade.
    for (int i = 0; i < seq.length(); ++i) {
      LocalDate date = ms2date(seq.getTimeMS(i));
      if (date.getMonth() == Month.JANUARY && date.getYear() % 10 == 0
          && (!bCheckDate || date.equals(TimeLib.toFirstBusinessDayOfMonth(date)))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns index corresponding to first entry of new year (e.g. January 1881).
   * 
   * @param seq Sequence with timestamps
   * @param bCheckData require that date is first business day of January.
   * @return index for first year or -1 if none
   */
  public static int findStartofFirstYear(Sequence seq, boolean bCheckDate)
  {
    // Find start of year.
    for (int i = 0; i < seq.length(); ++i) {
      LocalDate date = ms2date(seq.getTimeMS(i));
      if (date.getMonth() == Month.JANUARY && (!bCheckDate || date.equals(TimeLib.toFirstBusinessDayOfMonth(date)))) {
        return i;
      }
    }
    return -1;
  }

  public static long calcCommonStart(List<Sequence> seqs)
  {
    return calcCommonStart(seqs.toArray(new Sequence[seqs.size()]));
  }

  public static long calcCommonStart(Sequence... seqs)
  {
    long last = seqs[0].getStartMS();
    for (Sequence seq : seqs) {
      if (seq == null) continue;
      if (seq.getStartMS() > last) {
        last = seq.getStartMS();
      }
    }
    return last;
  }

  public static long calcCommonEnd(List<Sequence> seqs)
  {
    return calcCommonEnd(seqs.toArray(new Sequence[seqs.size()]));
  }

  public static long calcCommonEnd(Sequence... seqs)
  {
    long last = seqs[0].getEndMS();
    for (Sequence seq : seqs) {
      if (seq == null) continue;
      if (seq.getEndMS() < last) {
        last = seq.getEndMS();
      }
    }
    return last;
  }

  /**
   * Determines if the two dates are part of the same week.
   * 
   * A week runs from Monday to Sunday without regard for month or year changes.
   * 
   * @param d1 first day
   * @param d2 second day
   * @return True if the two dates are from the same week.
   */
  public static boolean isSameWeek(LocalDate d1, LocalDate d2)
  {
    if (d1.equals(d2)) return true;
    if (d1.isAfter(d2)) {
      LocalDate tmp = d1;
      d1 = d2;
      d2 = tmp;
    }
    assert d1.isBefore(d2);

    long nDaysBetween = ChronoUnit.DAYS.between(d1, d2);
    if (nDaysBetween >= 7) return false;

    int day1 = d1.getDayOfWeek().getValue();
    int day2 = d2.getDayOfWeek().getValue();
    assert day1 != day2;
    return day1 < day2;
  }

  /** @return True if the two dates are for the same month of the same year. */
  public static boolean isSameMonth(LocalDate d1, LocalDate d2)
  {
    return d1.getYear() == d2.getYear() && d1.getMonth() == d2.getMonth();
  }

  /** @return string representation of the given day (year, month, date) */
  public static String formatTime(long ms)
  {
    return formatTime(ms, null);
  }

  public static String formatTime(long ms, ZoneId zone)
  {
    return formatTime(ms, dtfTime, zone);
  }

  /** @return string representation of the given day (year, month, date) */
  public static String formatTimeSig(long ms)
  {
    return formatTime(ms, dtfTimeSig, ZoneId.systemDefault());
  }

  /** @return string representation of the given day (year, month, date) */
  public static String formatTime(long ms, DateTimeFormatter dtf, ZoneId zone)
  {
    if (ms == TIME_ERROR || ms == Long.MAX_VALUE) return null;
    return ms2time(ms, zone).format(dtf);
  }

  /** @return string representation of the given day (d MMM YYY) */
  public static String formatDate(long ms)
  {
    if (ms == TIME_ERROR || ms == Long.MAX_VALUE) return null;
    else if (ms == TIME_BEGIN) return "Begin";
    else if (ms == TIME_END) return "End";
    else return ms2date(ms).format(dtfDate);
  }

  /** @return string representation of the given day (dd MMM YYY) */
  public static String formatDate2(long ms)
  {
    if (ms == TIME_ERROR || ms == Long.MAX_VALUE) return null;
    return ms2date(ms).format(dtfDate2);
  }

  /** @return string representation of the given month (year, month) */
  public static String formatMonth(long ms)
  {
    if (ms == TIME_ERROR || ms == Long.MAX_VALUE) return null;
    return ms2date(ms).format(dtfMonth);
  }

  /** @return string representation of the given time as year-month-day */
  public static String formatYMD(long ms)
  {
    if (ms == TIME_ERROR || ms == Long.MAX_VALUE) return null;
    return ms2date(ms).format(dtfYMD);
  }

  /** @return string representation of the given time as year-month */
  public static String formatYM(long ms)
  {
    if (ms == TIME_ERROR || ms == Long.MAX_VALUE) return null;
    return ms2date(ms).format(dtfYM);
  }

  /** @return LocalDate for the given date moved to the first business day of the month. */
  public static LocalDate toFirstBusinessDayOfMonth(LocalDate date)
  {
    LocalDate lastOfPrevMonth = date.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    return toNextBusinessDay(lastOfPrevMonth);
  }

  /** @return LocalDate for the given date moved to the last business day of the month. */
  public static LocalDate toLastBusinessDayOfMonth(LocalDate date)
  {
    LocalDate firstOfNextMonth = date.with(TemporalAdjusters.firstDayOfNextMonth());
    return toPreviousBusinessDay(firstOfNextMonth);
  }

  /** @return human readable string representing the given amount of time */
  public static String formatDuration(long ms)
  {
    return formatDuration(ms, 1);
  }

  /** @return human readable string representing the given amount of time */
  public static String formatDuration(long ms, int nSigDig)
  {
    boolean bNeg = (ms < 0);
    ms = Math.abs(ms);

    double second = 1000.0;
    double minute = 60 * second;
    double hour = 60 * minute;
    double day = 24 * hour;
    double week = 7 * day;
    double month = 30.4167 * day;
    double year = 365 * day;

    String sNum = String.format("%%.%df", nSigDig);
    String sRet;
    if (Math.abs(ms - year) < day) sRet = "1 year";
    else if (ms >= 12 * month) sRet = String.format(sNum + " years", ms / year);
    else if (ms > 6 * week) sRet = String.format(sNum + " months", ms / month);
    else if (ms > 10 * day) sRet = String.format(sNum + " weeks", ms / week);
    else if (ms > day) sRet = String.format(sNum + " days", ms / day);
    else if (ms > hour) sRet = String.format(sNum + " hours", ms / hour);
    else if (ms > minute) sRet = String.format(sNum + " min", ms / minute);
    else if (ms > second) sRet = String.format(sNum + "s", ms / second);
    else sRet = String.format("%dms", ms);

    if (bNeg) sRet = "-" + sRet;

    return sRet;
  }

  public static String formatDurationMonths(int nMonths)
  {
    if (nMonths == 12) {
      return "1 year";
    }
    if (nMonths < 18) {
      return String.format("%d months", nMonths);
    }
    if (nMonths % 12 == 0) {
      return String.format("%d years", nMonths / 12);
    }
    return String.format("%.1f years", nMonths / 12.0);
  }

  public static boolean isWeekend(LocalDate date)
  {
    DayOfWeek day = date.getDayOfWeek();
    return (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
  }

  public static boolean isHoliday(LocalDate date)
  {
    MonthDay md = MonthDay.from(date);

    // New Years.
    if (md.equals(MonthDay.of(1, 1))) return true;
    if (md.equals(MonthDay.of(1, 2)) && date.getDayOfWeek() == DayOfWeek.MONDAY) return true;

    // MLK Day (first observed in 1986).
    if (date.getYear() >= 1986) {
      LocalDate mlk = TimeLib.getNthDayOfWeek(date.getYear(), Month.JANUARY, 3, DayOfWeek.MONDAY);
      if (date.equals(mlk)) return true;
    }

    // Washington's Birthday.
    LocalDate washington = TimeLib.getNthDayOfWeek(date.getYear(), Month.FEBRUARY, 3, DayOfWeek.MONDAY);
    if (date.equals(washington)) return true;

    // Good Friday.
    LocalDate easter = TimeLib.getEaster(date.getYear());
    LocalDate goodFriday = easter.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
    assert goodFriday.getDayOfWeek() == DayOfWeek.FRIDAY;
    if (date.equals(goodFriday)) return true;

    // Memorial Day
    LocalDate memorial = TimeLib.getNthDayOfWeek(date.getYear(), Month.MAY, -1, DayOfWeek.MONDAY);
    if (date.equals(memorial)) return true;

    // Independence Day.
    if (md.equals(MonthDay.of(7, 4))) return true;
    if (md.equals(MonthDay.of(7, 3)) && date.getDayOfWeek() == DayOfWeek.FRIDAY) return true;
    if (md.equals(MonthDay.of(7, 5)) && date.getDayOfWeek() == DayOfWeek.MONDAY) return true;

    // Labor Day.
    LocalDate labor = TimeLib.getNthDayOfWeek(date.getYear(), Month.SEPTEMBER, 1, DayOfWeek.MONDAY);
    if (date.equals(labor)) return true;

    // Thanksgiving.
    LocalDate thanksgiving = TimeLib.getNthDayOfWeek(date.getYear(), Month.NOVEMBER, 4, DayOfWeek.THURSDAY);
    if (date.equals(thanksgiving)) return true;

    // Christmas.
    if (md.equals(MonthDay.of(12, 25))) return true;
    if (md.equals(MonthDay.of(12, 24)) && date.getDayOfWeek() == DayOfWeek.FRIDAY) return true;
    if (md.equals(MonthDay.of(12, 26)) && date.getDayOfWeek() == DayOfWeek.MONDAY) return true;

    return false;
  }

  public static boolean isBusinessDay(LocalDate date)
  {
    return !isWeekend(date) && !isHoliday(date);
  }

  public static long toLastBusinessDayOfWeek(long ms)
  {
    return toMs(toLastBusinessDayOfWeek(ms2date(ms)));
  }

  /**
   * Return the last business day in the week containing the given date. A week runs from Monday to Sunday.
   * 
   * @return last business day in the week containing the given date.
   */
  public static LocalDate toLastBusinessDayOfWeek(LocalDate date)
  {
    LocalDate lastBusinessDayOfWeek = isBusinessDay(date) ? date : null;

    LocalDate next = date;
    while (true) {
      next = toNextBusinessDay(next);
      if (!isSameWeek(next, date)) break;
      lastBusinessDayOfWeek = next;
    }

    if (lastBusinessDayOfWeek == null) {
      LocalDate prev = toPreviousBusinessDay(date);
      if (isSameWeek(prev, date)) {
        lastBusinessDayOfWeek = prev;
      }
    }

    return lastBusinessDayOfWeek;
  }

  public static long toPreviousBusinessDay(long ms)
  {
    return toMs(toPreviousBusinessDay(ms2date(ms)));
  }

  public static LocalDate toPreviousBusinessDay(LocalDate date)
  {
    while (true) {
      date = date.minusDays(1);
      if (isBusinessDay(date)) break;
    }
    return date;
  }

  public static long toNextBusinessDay(long ms)
  {
    return toMs(toNextBusinessDay(ms2date(ms)));
  }

  public static LocalDate toNextBusinessDay(LocalDate date)
  {
    while (true) {
      date = date.plusDays(1);
      if (isBusinessDay(date)) break;
    }
    return date;
  }

  public static long plusBusinessDays(long ms, int nBusinessDays)
  {
    return toMs(plusBusinessDays(ms2date(ms), nBusinessDays));
  }

  public static LocalDate plusBusinessDays(LocalDate date, int nBusinessDays)
  {
    if (nBusinessDays < 0) {
      for (int i = 0; i > nBusinessDays; --i) {
        date = toPreviousBusinessDay(date);
      }
    } else if (nBusinessDays > 0) {
      for (int i = 0; i < nBusinessDays; ++i) {
        date = toNextBusinessDay(date);
      }
    }
    return date;
  }

  /**
   * @return date of the closest business day on the requested day.
   */
  public static LocalDate getClosestBusinessDay(LocalDate date, boolean bAcceptDifferentMonth)
  {
    // If base is a business day, we're done.
    DayOfWeek day = date.getDayOfWeek();
    if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
      return date;
    }

    // Find closest business day forward and backward.
    LocalDate prevDate = toPreviousBusinessDay(date);
    LocalDate nextDate = toNextBusinessDay(date);

    // Figure out which is closer.
    long daysPrev = ChronoUnit.DAYS.between(prevDate, date);
    long daysNext = ChronoUnit.DAYS.between(date, nextDate);
    LocalDate closestDate = (daysPrev <= daysNext ? prevDate : nextDate);

    // If month doesn't matter, return closest date.
    if (bAcceptDifferentMonth) return closestDate;

    // Month does matter so check to see if closest date has the same month.
    if (isSameMonth(date, closestDate)) return closestDate;
    LocalDate fartherDate = (daysPrev <= daysNext ? nextDate : prevDate);
    assert fartherDate != closestDate;
    assert isSameMonth(date, fartherDate);
    return fartherDate;
  }

  /** @return number of business days in the given month. */
  public static int getNumBusinessDays(YearMonth month)
  {
    int n = 0;
    LocalDate date = month.atDay(1);
    while (date.get(ChronoField.MONTH_OF_YEAR) == month.getMonthValue()) {
      if (isBusinessDay(date)) ++n;
      date = date.plusDays(1);
    }
    return n;
  }

  /**
   * @return A date representing the Nth DoW in the given month/year (e.g. 3rd Monday of November, 1956).
   */
  public static LocalDate getNthDayOfWeek(int year, Month month, int nth, DayOfWeek dow)
  {
    assert nth != 0;

    LocalDate date;
    if (nth > 0) {
      // Counting forward, e.g. 2nd Monday of month.
      date = LocalDate.of(year, month, 1).with(TemporalAdjusters.firstInMonth(dow));
      if (nth > 1) {
        date = date.plusDays(7 * (nth - 1));
      }
    } else {
      // Counting backward, e.g. 2nd to last Monday of month.
      assert nth < 0;
      date = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(dow));
      if (nth < -1) {
        date = date.minusDays(7 * (nth + 1));
      }
    }
    assert date.getDayOfWeek() == dow;
    assert date.getMonth() == month;
    assert date.getYear() == year;
    return date;
  }

  /**
   * Calculate date of Easter in the given year.
   * 
   * Only valid for Gregorian Years. More info here: https://en.wikipedia.org/wiki/Computus#Other_algorithms
   * 
   * @return Date of Easter in the given year.
   */
  public static LocalDate getEaster(int year)
  {
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h + 22 * l) / 451;
    int n = (h + l - 7 * m + 114) / 31;
    int p = (h + l - 7 * m + 114) % 31;
    LocalDate date = LocalDate.of(year, n, p + 1);
    assert date.getDayOfWeek() == DayOfWeek.SUNDAY;
    return date;
  }

  /** @return ms for data in YYYY-MM or YYYY-MM-DD format. */
  public static long parseDate(String date) throws NumberFormatException
  {
    String[] dateFields = date.split("[-/]");

    // TODO use java.time parser.
    int year = Integer.parseInt(dateFields[0]);
    int month = Integer.parseInt(dateFields[1]);
    int day = 1;
    if (dateFields.length > 2) {
      day = Integer.parseInt(dateFields[2]);
    }

    return TimeLib.toMs(year, month, day);
  }

  /** @return True if `seq` holds monthly data (one data point per month and contiguous). */
  public static boolean isMonthly(Sequence seq)
  {
    if (seq == null) return false;
    if (seq.isEmpty()) return true;

    LocalDate prev = TimeLib.ms2date(seq.getTimeMS(0));
    for (int i = 1; i < seq.length(); ++i) {
      LocalDate expected = prev.plusMonths(1);
      LocalDate current = TimeLib.ms2date(seq.getTimeMS(i));
      if (current.getMonth() != expected.getMonth()) return false;
      prev = current;
    }
    return true;
  }

  public static String dayOfWeek(LocalDate date)
  {
    String dayOfWeek = date.getDayOfWeek().toString();
    return dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase();
  }
}
