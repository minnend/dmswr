package org.minnen.dmswr.tests;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

import org.junit.Test;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.TimeLib;

public class TestTimeLib
{
  @Test
  public void testMonthsBetween()
  {
    final double eps = 1 - 4;
    long t1, t2;

    t1 = TimeLib.toMs(2015, Month.DECEMBER, 20);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 20);
    assertEquals(0.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2015, Month.DECEMBER, 1);
    t2 = TimeLib.toMs(2016, Month.JANUARY, 1);
    assertEquals(1.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2015, Month.DECEMBER, 1);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 31);
    assertEquals(30.0 / 31.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2015, Month.NOVEMBER, 20);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 20);
    assertEquals(1.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2015, Month.NOVEMBER, 20);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 19);
    assertEquals(29.0 / 30.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2015, Month.NOVEMBER, 30);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 1);
    assertEquals(1.0 / 30.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2015, Month.NOVEMBER, 1);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 30);
    assertEquals(1.0 + 29.0 / 31.0, TimeLib.monthsBetween(t1, t2), eps);

    t1 = TimeLib.toMs(2013, Month.NOVEMBER, 1);
    t2 = TimeLib.toMs(2015, Month.DECEMBER, 30);
    assertEquals(25 + 29.0 / 31.0, TimeLib.monthsBetween(t1, t2), eps);
  }

  @Test
  public void testIsSameWeek()
  {
    LocalDate d1, d2;

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = LocalDate.of(2015, Month.DECEMBER, 21);
    assertTrue(TimeLib.isSameWeek(d1, d2));

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = LocalDate.of(2015, Month.DECEMBER, 22);
    assertTrue(TimeLib.isSameWeek(d1, d2));
    assertTrue(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = LocalDate.of(2015, Month.DECEMBER, 23);
    assertTrue(TimeLib.isSameWeek(d1, d2));
    assertTrue(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = LocalDate.of(2015, Month.DECEMBER, 25);
    assertTrue(TimeLib.isSameWeek(d1, d2));
    assertTrue(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = LocalDate.of(2015, Month.DECEMBER, 27);
    assertTrue(TimeLib.isSameWeek(d1, d2));
    assertTrue(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = LocalDate.of(2015, Month.DECEMBER, 28);
    assertFalse(TimeLib.isSameWeek(d1, d2));
    assertFalse(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 18);
    d2 = LocalDate.of(2015, Month.DECEMBER, 21);
    assertFalse(TimeLib.isSameWeek(d1, d2));
    assertFalse(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 20);
    d2 = LocalDate.of(2015, Month.DECEMBER, 27);
    assertFalse(TimeLib.isSameWeek(d1, d2));
    assertFalse(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 31);
    d2 = LocalDate.of(2016, Month.JANUARY, 1);
    assertTrue(TimeLib.isSameWeek(d1, d2));
    assertTrue(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 31);
    d2 = LocalDate.of(2016, Month.JANUARY, 4);
    assertFalse(TimeLib.isSameWeek(d1, d2));
    assertFalse(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2015, Month.DECEMBER, 31);
    d2 = LocalDate.of(2016, Month.JANUARY, 4);
    assertFalse(TimeLib.isSameWeek(d1, d2));
    assertFalse(TimeLib.isSameWeek(d2, d1));

    // Leap year.
    d1 = LocalDate.of(2016, Month.FEBRUARY, 28);
    d2 = LocalDate.of(2016, Month.FEBRUARY, 29);
    assertFalse(TimeLib.isSameWeek(d1, d2));
    assertFalse(TimeLib.isSameWeek(d2, d1));

    d1 = LocalDate.of(2016, Month.FEBRUARY, 29);
    d2 = LocalDate.of(2016, Month.MARCH, 1);
    assertTrue(TimeLib.isSameWeek(d1, d2));
    assertTrue(TimeLib.isSameWeek(d2, d1));
  }

  @Test
  public void testIsSameMonth()
  {
    LocalDate d1, d2;

    d1 = LocalDate.of(2015, Month.DECEMBER, 20);
    d2 = LocalDate.of(2015, Month.DECEMBER, 20);
    assertTrue(TimeLib.isSameMonth(d1, d2));

    d1 = LocalDate.of(2015, Month.DECEMBER, 20);
    d2 = LocalDate.of(2015, Month.NOVEMBER, 20);
    assertFalse(TimeLib.isSameMonth(d1, d2));

    d1 = LocalDate.of(2015, Month.DECEMBER, 1);
    d2 = LocalDate.of(2015, Month.DECEMBER, 31);
    assertTrue(TimeLib.isSameMonth(d1, d2));

    d1 = LocalDate.of(2015, Month.DECEMBER, 1);
    d2 = LocalDate.of(2016, Month.DECEMBER, 31);
    assertFalse(TimeLib.isSameMonth(d1, d2));

    d1 = LocalDate.of(2015, Month.FEBRUARY, 28);
    d2 = LocalDate.of(2016, Month.FEBRUARY, 28);
    assertFalse(TimeLib.isSameMonth(d1, d2));

    d1 = LocalDate.of(2016, Month.FEBRUARY, 28);
    d2 = LocalDate.of(2016, Month.FEBRUARY, 29);
    assertTrue(TimeLib.isSameMonth(d1, d2));
  }

  @Test
  public void testToPreviousBusinessDay()
  {
    LocalDate date;

    date = TimeLib.toPreviousBusinessDay(LocalDate.of(2015, Month.NOVEMBER, 1));
    assertEquals(2015, date.getYear());
    assertEquals(Month.OCTOBER, date.getMonth());
    assertEquals(30, date.getDayOfMonth());

    date = TimeLib.toPreviousBusinessDay(LocalDate.of(2015, Month.NOVEMBER, 30));
    assertEquals(2015, date.getYear());
    assertEquals(Month.NOVEMBER, date.getMonth());
    assertEquals(27, date.getDayOfMonth());

    date = TimeLib.toPreviousBusinessDay(LocalDate.of(2015, Month.OCTOBER, 30));
    assertEquals(2015, date.getYear());
    assertEquals(Month.OCTOBER, date.getMonth());
    assertEquals(29, date.getDayOfMonth());

    date = TimeLib.toPreviousBusinessDay(LocalDate.of(2016, Month.JANUARY, 1));
    assertEquals(2015, date.getYear());
    assertEquals(Month.DECEMBER, date.getMonth());
    assertEquals(31, date.getDayOfMonth());

    date = TimeLib.toPreviousBusinessDay(LocalDate.of(2016, Month.MARCH, 1));
    assertEquals(2016, date.getYear());
    assertEquals(Month.FEBRUARY, date.getMonth());
    assertEquals(29, date.getDayOfMonth());
  }

  @Test
  public void testToNextBusinessDay()
  {
    LocalDate date;

    date = TimeLib.toNextBusinessDay(LocalDate.of(2015, Month.NOVEMBER, 1));
    assertEquals(2015, date.getYear());
    assertEquals(Month.NOVEMBER, date.getMonth());
    assertEquals(2, date.getDayOfMonth());

    date = TimeLib.toNextBusinessDay(LocalDate.of(2015, Month.NOVEMBER, 30));
    assertEquals(2015, date.getYear());
    assertEquals(Month.DECEMBER, date.getMonth());
    assertEquals(1, date.getDayOfMonth());

    date = TimeLib.toNextBusinessDay(LocalDate.of(2015, Month.OCTOBER, 31));
    assertEquals(2015, date.getYear());
    assertEquals(Month.NOVEMBER, date.getMonth());
    assertEquals(2, date.getDayOfMonth());

    date = TimeLib.toNextBusinessDay(LocalDate.of(2016, Month.JANUARY, 1));
    assertEquals(2016, date.getYear());
    assertEquals(Month.JANUARY, date.getMonth());
    assertEquals(4, date.getDayOfMonth());

    date = TimeLib.toNextBusinessDay(LocalDate.of(2016, Month.FEBRUARY, 26));
    assertEquals(2016, date.getYear());
    assertEquals(Month.FEBRUARY, date.getMonth());
    assertEquals(29, date.getDayOfMonth());
  }

  @Test
  public void testToFirstBusinessDayOfMonth()
  {
    LocalDate d1, d2;

    d1 = LocalDate.of(2015, Month.DECEMBER, 20);
    d2 = TimeLib.toFirstBusinessDayOfMonth(d1);
    assertTrue(TimeLib.isSameMonth(d1, d2));
    assertEquals(2015, d2.getYear());
    assertEquals(1, d2.getDayOfMonth());

    d1 = LocalDate.of(2016, Month.FEBRUARY, 20);
    d2 = TimeLib.toFirstBusinessDayOfMonth(d1);
    assertTrue(TimeLib.isSameMonth(d1, d2));
    assertEquals(1, d2.getDayOfMonth());

    d1 = LocalDate.of(2016, Month.MAY, 30);
    d2 = TimeLib.toFirstBusinessDayOfMonth(d1);
    assertTrue(TimeLib.isSameMonth(d1, d2));
    assertEquals(2, d2.getDayOfMonth());
  }

  @Test
  public void testToLastBusinessDayOfMonth()
  {
    LocalDate d1, d2;

    d1 = LocalDate.of(2015, Month.DECEMBER, 20);
    d2 = TimeLib.toLastBusinessDayOfMonth(d1);
    assertTrue(TimeLib.isSameMonth(d1, d2));
    assertEquals(2015, d2.getYear());
    assertEquals(Month.DECEMBER, d2.getMonth());
    assertEquals(31, d2.getDayOfMonth());

    d1 = LocalDate.of(2016, Month.FEBRUARY, 1);
    d2 = TimeLib.toLastBusinessDayOfMonth(d1);
    assertTrue(TimeLib.isSameMonth(d1, d2));
    assertEquals(2016, d2.getYear());
    assertEquals(Month.FEBRUARY, d2.getMonth());
    assertEquals(29, d2.getDayOfMonth());

    d1 = LocalDate.of(2015, Month.OCTOBER, 1);
    d2 = TimeLib.toLastBusinessDayOfMonth(d1);
    assertTrue(TimeLib.isSameMonth(d1, d2));
    assertEquals(2015, d2.getYear());
    assertEquals(Month.OCTOBER, d2.getMonth());
    assertEquals(30, d2.getDayOfMonth());
  }

  @Test
  public void testClosestBusinessDay()
  {
    LocalDate d1, d2;

    d1 = LocalDate.of(2015, Month.DECEMBER, 21);
    d2 = TimeLib.getClosestBusinessDay(d1, true);
    assertEquals(2015, d2.getYear());
    assertEquals(Month.DECEMBER, d2.getMonth());
    assertEquals(21, d2.getDayOfMonth());

    d1 = LocalDate.of(2015, Month.DECEMBER, 20);
    d2 = TimeLib.getClosestBusinessDay(d1, true);
    assertEquals(2015, d2.getYear());
    assertEquals(Month.DECEMBER, d2.getMonth());
    assertEquals(21, d2.getDayOfMonth());

    d1 = LocalDate.of(2015, Month.OCTOBER, 31);
    d2 = TimeLib.getClosestBusinessDay(d1, true);
    assertEquals(2015, d2.getYear());
    assertEquals(Month.OCTOBER, d2.getMonth());
    assertEquals(30, d2.getDayOfMonth());

    d1 = LocalDate.of(2015, Month.AUGUST, 1);
    d2 = TimeLib.getClosestBusinessDay(d1, true);
    assertEquals(2015, d2.getYear());
    assertEquals(Month.JULY, d2.getMonth());
    assertEquals(31, d2.getDayOfMonth());

    d1 = LocalDate.of(2015, Month.AUGUST, 1);
    d2 = TimeLib.getClosestBusinessDay(d1, false);
    assertEquals(2015, d2.getYear());
    assertEquals(Month.AUGUST, d2.getMonth());
    assertEquals(3, d2.getDayOfMonth());
  }

  @Test
  public void testGetNthDayOfWeek()
  {
    LocalDate date;

    // Thanksgiving = Fourth Thursday of November.
    date = TimeLib.getNthDayOfWeek(2015, Month.NOVEMBER, 4, DayOfWeek.THURSDAY);
    assertEquals(LocalDate.of(2015, 11, 26), date);

    date = TimeLib.getNthDayOfWeek(2016, Month.NOVEMBER, 4, DayOfWeek.THURSDAY);
    assertEquals(LocalDate.of(2016, 11, 24), date);

    date = TimeLib.getNthDayOfWeek(1990, Month.NOVEMBER, 4, DayOfWeek.THURSDAY);
    assertEquals(LocalDate.of(1990, 11, 22), date);

    date = TimeLib.getNthDayOfWeek(1900, Month.NOVEMBER, 4, DayOfWeek.THURSDAY);
    assertEquals(LocalDate.of(1900, 11, 22), date);

    // Memorial Day = Last Monday of May.
    date = TimeLib.getNthDayOfWeek(2015, Month.MAY, -1, DayOfWeek.MONDAY);
    assertEquals(LocalDate.of(2015, 5, 25), date);

    date = TimeLib.getNthDayOfWeek(2014, Month.MAY, -1, DayOfWeek.MONDAY);
    assertEquals(LocalDate.of(2014, 5, 26), date);

    date = TimeLib.getNthDayOfWeek(2016, Month.MAY, -1, DayOfWeek.MONDAY);
    assertEquals(LocalDate.of(2016, 5, 30), date);

    // MLK Day = Third Monday of January.
    date = TimeLib.getNthDayOfWeek(2016, Month.JANUARY, 3, DayOfWeek.MONDAY);
    assertEquals(LocalDate.of(2016, 1, 18), date);

    date = TimeLib.getNthDayOfWeek(2015, Month.JANUARY, 3, DayOfWeek.MONDAY);
    assertEquals(LocalDate.of(2015, 1, 19), date);

    date = TimeLib.getNthDayOfWeek(2014, Month.JANUARY, 3, DayOfWeek.MONDAY);
    assertEquals(LocalDate.of(2014, 1, 20), date);
  }

  @Test
  public void testGetEaster()
  {
    // https://en.wikipedia.org/wiki/List_of_dates_for_Easter

    int year = 2015;
    LocalDate date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 4, 5), date);

    year = 2016;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 3, 27), date);

    year = 2014;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 4, 20), date);

    year = 2013;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 3, 31), date);

    year = 2012;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 4, 8), date);

    year = 2011;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 4, 24), date);

    year = 2000;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 4, 23), date);

    year = 1995;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 4, 16), date);

    year = 2035;
    date = TimeLib.getEaster(year);
    assertEquals(LocalDate.of(year, 3, 25), date);
  }

  @Test
  public void testisHoliday()
  {
    // https://www.nyse.com/markets/hours-calendars

    // New Years Day.
    LocalDate date = LocalDate.of(2015, 1, 1);
    assertTrue(TimeLib.isHoliday(date));

    // New Years Day on Sunday.
    date = LocalDate.of(2006, 1, 2);
    assertTrue(TimeLib.isHoliday(date));

    // MLK Day.
    date = LocalDate.of(2015, 1, 19);
    assertTrue(TimeLib.isHoliday(date));
    date = LocalDate.of(1995, 1, 16);
    assertTrue(TimeLib.isHoliday(date));
    date = LocalDate.of(1986, 1, 20);
    assertTrue(TimeLib.isHoliday(date));

    date = LocalDate.of(1985, 1, 21);
    assertFalse(TimeLib.isHoliday(date)); // MLK Day start in 1986

    // Washington's Birthday.
    date = LocalDate.of(2015, 2, 16);
    assertTrue(TimeLib.isHoliday(date));

    // Good Friday.
    date = LocalDate.of(2015, 4, 3);
    assertTrue(TimeLib.isHoliday(date));

    // Memorial Day.
    date = LocalDate.of(2015, 5, 25);
    assertTrue(TimeLib.isHoliday(date));

    // Independence Day.
    date = LocalDate.of(2015, 7, 4);
    assertTrue(TimeLib.isHoliday(date));

    // Independence Day on Saturday.
    date = LocalDate.of(2009, 7, 3);
    assertTrue(TimeLib.isHoliday(date));

    // Independence Day on Sunday.
    date = LocalDate.of(1999, 7, 5);
    assertTrue(TimeLib.isHoliday(date));

    // Labor Day.
    date = LocalDate.of(2015, 9, 7);
    assertTrue(TimeLib.isHoliday(date));

    // Thanksgiving.
    date = LocalDate.of(2015, 11, 26);
    assertTrue(TimeLib.isHoliday(date));

    // Christmas.
    date = LocalDate.of(2015, 12, 25);
    assertTrue(TimeLib.isHoliday(date));

    // Christmas on Saturday
    date = LocalDate.of(2010, 12, 24);
    assertTrue(TimeLib.isHoliday(date));

    // Christmas on Sunday
    date = LocalDate.of(2005, 12, 26);
    assertTrue(TimeLib.isHoliday(date));

    // Not holidays.
    date = LocalDate.of(2015, 1, 2);
    assertFalse(TimeLib.isHoliday(date));

    date = LocalDate.of(2015, 12, 31);
    assertFalse(TimeLib.isHoliday(date));
  }

  @Test
  public void testLastBusinessDayOfWeek()
  {
    LocalDate date;

    date = LocalDate.of(2016, Month.JANUARY, 8);
    assertEquals(LocalDate.of(2016, Month.JANUARY, 8), TimeLib.toLastBusinessDayOfWeek(date));

    date = LocalDate.of(2016, Month.JANUARY, 7);
    assertEquals(LocalDate.of(2016, Month.JANUARY, 8), TimeLib.toLastBusinessDayOfWeek(date));

    date = LocalDate.of(2016, Month.JANUARY, 4);
    assertEquals(LocalDate.of(2016, Month.JANUARY, 8), TimeLib.toLastBusinessDayOfWeek(date));

    date = LocalDate.of(2016, Month.JANUARY, 10);
    assertEquals(LocalDate.of(2016, Month.JANUARY, 8), TimeLib.toLastBusinessDayOfWeek(date));

    // January 1 is a holiday.
    date = LocalDate.of(2016, Month.JANUARY, 1);
    assertEquals(LocalDate.of(2015, Month.DECEMBER, 31), TimeLib.toLastBusinessDayOfWeek(date));

    date = LocalDate.of(2016, Month.FEBRUARY, 10);
    assertEquals(LocalDate.of(2016, Month.FEBRUARY, 12), TimeLib.toLastBusinessDayOfWeek(date));

    date = LocalDate.of(2016, Month.FEBRUARY, 29);
    assertEquals(LocalDate.of(2016, Month.MARCH, 4), TimeLib.toLastBusinessDayOfWeek(date));
  }

  @Test
  public void testGetNumBusinessDays()
  {
    assertEquals(19, TimeLib.getNumBusinessDays(YearMonth.of(2016, Month.JANUARY)));
    assertEquals(20, TimeLib.getNumBusinessDays(YearMonth.of(2016, Month.FEBRUARY)));
    assertEquals(22, TimeLib.getNumBusinessDays(YearMonth.of(2016, Month.MARCH)));
  }

  @Test
  public void testIsMonthly_Yes()
  {
    Sequence seq = new Sequence("test");
    for (int i = 1; i <= 12; ++i) {
      seq.addData(i, LocalDate.of(2000, i, 1));
    }
    assertTrue(TimeLib.isMonthly(seq));
  }

  @Test
  public void testIsMonthly_No()
  {
    Sequence seq = new Sequence("test");
    for (int i = 1; i <= 12; ++i) {
      seq.addData(i, LocalDate.of(2000, Month.JANUARY, i));
    }
    assertFalse(TimeLib.isMonthly(seq));
  }
}
