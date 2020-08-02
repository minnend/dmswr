package org.minnen.dmswr.tests;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;
import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.FinLib;
import org.minnen.dmswr.utils.TimeLib;

public class TestFinLib
{
  @Test
  public void testGetReturn()
  {
    Sequence cumulativeReturns = new Sequence(new double[] { 1.0, 1.2, 1.44, 1.296 });
    assertEquals(1.2, FinLib.getTotalReturn(cumulativeReturns, 0, 1), 1e-6);
    assertEquals(1.2, FinLib.getTotalReturn(cumulativeReturns, 1, 2), 1e-6);
    assertEquals(0.9, FinLib.getTotalReturn(cumulativeReturns, 2, 3), 1e-6);
    assertEquals(1.44, FinLib.getTotalReturn(cumulativeReturns, 0, 2), 1e-6);
    assertEquals(1.08, FinLib.getTotalReturn(cumulativeReturns, 1, 3), 1e-6);
    assertEquals(1.296, FinLib.getTotalReturn(cumulativeReturns, 0, 3), 1e-6);
  }

  @Test
  public void testMul2Ret()
  {
    assertEquals(0.0, FinLib.mul2ret(1.0), 1e-6);

    assertEquals(20.0, FinLib.mul2ret(1.2), 1e-6);
    assertEquals(2.0, FinLib.mul2ret(1.02), 1e-6);
    assertEquals(100.0, FinLib.mul2ret(2.0), 1e-6);
    assertEquals(210.0, FinLib.mul2ret(3.1), 1e-6);

    assertEquals(-10.0, FinLib.mul2ret(0.9), 1e-6);
    assertEquals(-2.0, FinLib.mul2ret(0.98), 1e-6);
    assertEquals(-50.0, FinLib.mul2ret(0.5), 1e-6);
    assertEquals(-90.0, FinLib.mul2ret(0.1), 1e-6);
  }

  @Test
  public void testRet2Mul()
  {
    assertEquals(1.0, FinLib.ret2mul(0.0), 1e-6);

    assertEquals(1.02, FinLib.ret2mul(2.0), 1e-6);
    assertEquals(1.5, FinLib.ret2mul(50.0), 1e-6);
    assertEquals(2.0, FinLib.ret2mul(100.0), 1e-6);

    assertEquals(0.98, FinLib.ret2mul(-2.0), 1e-6);
    assertEquals(0.8, FinLib.ret2mul(-20.0), 1e-6);
    assertEquals(0.2, FinLib.ret2mul(-80.0), 1e-6);
  }

  @Test
  public void testGetAnnualReturn()
  {
    assertEquals(0.0, FinLib.getAnnualReturn(1.0, 1), 1e-6);
    assertEquals(0.0, FinLib.getAnnualReturn(1.0, 6), 1e-6);
    assertEquals(0.0, FinLib.getAnnualReturn(1.0, 12), 1e-6);
    assertEquals(0.0, FinLib.getAnnualReturn(1.0, 24), 1e-6);

    assertEquals(30.0, FinLib.getAnnualReturn(1.3, 12), 1e-6);
    assertEquals(110.0, FinLib.getAnnualReturn(2.1, 12), 1e-6);
    assertEquals(-10.0, FinLib.getAnnualReturn(0.9, 12), 1e-6);
    assertEquals(-50.0, FinLib.getAnnualReturn(0.5, 12), 1e-6);

    assertEquals(100.0, FinLib.getAnnualReturn(4.0, 24), 1e-6);
    assertEquals(25.992105, FinLib.getAnnualReturn(2.0, 36), 1e-6);
    assertEquals(-2.599625357, FinLib.getAnnualReturn(0.9, 48), 1e-6);
  }

  @Test
  public void testGetNameWithBreak()
  {
    assertEquals("", FinLib.getNameWithBreak(null));
    assertEquals("", FinLib.getNameWithBreak(""));
    assertEquals(" ", FinLib.getNameWithBreak(" "));
    assertEquals("foo", FinLib.getNameWithBreak("foo"));
    assertEquals("foo<br/>(bar)", FinLib.getNameWithBreak("foo (bar)"));
    assertEquals("foo (bar)<br/>(buzz)", FinLib.getNameWithBreak("foo (bar) (buzz)"));
  }

  @Test
  public void testGetBaseName()
  {
    assertEquals("", FinLib.getBaseName(null));
    assertEquals("", FinLib.getBaseName(""));
    assertEquals(" ", FinLib.getBaseName(" "));
    assertEquals("foo", FinLib.getBaseName("foo"));
    assertEquals("foo", FinLib.getBaseName("foo (bar)"));
    assertEquals("foo (bar)", FinLib.getBaseName("foo (bar) (buzz)"));
  }

  @Test
  public void testGetNameSuffix()
  {
    assertEquals("", FinLib.getNameSuffix(null));
    assertEquals("", FinLib.getNameSuffix(""));
    assertEquals("", FinLib.getNameSuffix(" "));
    assertEquals("", FinLib.getNameSuffix("foo"));
    assertEquals("(bar)", FinLib.getNameSuffix("foo (bar)"));
    assertEquals("(buzz)", FinLib.getNameSuffix("foo (bar) (buzz)"));
  }

  @Test
  public void testGetBoldedName()
  {
    assertEquals("", FinLib.getBoldedName(null));
    assertEquals("", FinLib.getBoldedName(""));
    assertEquals("<b> </b>", FinLib.getBoldedName(" "));
    assertEquals("<b>foo</b>", FinLib.getBoldedName("foo"));
    assertEquals("<b>foo</b> (bar)", FinLib.getBoldedName("foo (bar)"));
    assertEquals("<b>foo (bar)</b> (buzz)", FinLib.getBoldedName("foo (bar) (buzz)"));
  }

  @Test
  public void testCalcRealReturns_InflationCancelsGains()
  {
    Sequence nominal = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
    Sequence cpi = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
    Sequence real = FinLib.calcRealReturns(nominal, cpi);

    double[] expected = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0 };
    assertArrayEquals(expected, real.extractDim(0), 1e-8);
  }

  @Test
  public void testCalcRealReturns_NoInflation()
  {
    Sequence nominal = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
    Sequence cpi = new Sequence(new double[] { 1.0, 1.0, 1.0, 1.0, 1.0 });
    Sequence real = FinLib.calcRealReturns(nominal, cpi);

    double[] expected = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
    assertArrayEquals(expected, real.extractDim(0), 1e-8);
  }

  @Test
  public void testCalcRealReturns_Deflation()
  {
    Sequence nominal = new Sequence(new double[] { 1.0, 1.0, 1.0, 1.0, 1.0 });
    Sequence cpi = new Sequence(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 });
    Sequence real = FinLib.calcRealReturns(nominal, cpi);

    double[] expected = new double[] { 1.0, 1.25, 5.0 / 3.0, 2.5, 5.0 };
    assertArrayEquals(expected, real.extractDim(0), 1e-8);
  }

  @Test
  public void testCalcRealReturns_DeflationPlusGains()
  {
    Sequence nominal = new Sequence(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 });
    Sequence cpi = new Sequence(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 });
    Sequence real = FinLib.calcRealReturns(nominal, cpi);

    double[] expected = new double[] { 1.0, 2.5, 20.0 / 3.0, 20.0, 80.0 };
    assertArrayEquals(expected, real.extractDim(0), 1e-8);
  }

  @Test
  public void testIsLTG()
  {
    LocalDate d1, d2;

    d1 = LocalDate.of(2000, 1, 1);
    d2 = LocalDate.of(1999, 1, 1);
    assertFalse(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(2000, 1, 1);
    d2 = LocalDate.of(2000, 1, 1);
    assertFalse(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(2000, 1, 1);
    d2 = LocalDate.of(2001, 1, 1);
    assertFalse(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(2000, 1, 1);
    d2 = LocalDate.of(2001, 1, 2);
    assertTrue(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(2000, 1, 1);
    d2 = LocalDate.of(2001, 2, 1);
    assertTrue(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(2000, 1, 1);
    d2 = LocalDate.of(2002, 1, 1);
    assertTrue(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(1999, 2, 28);
    d2 = LocalDate.of(2000, 2, 29);
    assertFalse(FinLib.isLTG(d1, d2));

    d1 = LocalDate.of(1999, 2, 28);
    d2 = LocalDate.of(2000, 3, 1);
    assertTrue(FinLib.isLTG(d1, d2));
  }

  @Test
  public void testCalcDrawdown()
  {
    Sequence returns = new Sequence(new double[] { 1, 2, 1, 0, 1, 2, 3, 2, 1 });
    Sequence drawdown = FinLib.calcDrawdown(returns);

    double[] expected = new double[] { 0, 0, -50, -100, -50, 0, 0, -100.0 / 3, -200.0 / 3 };
    assertArrayEquals(expected, drawdown.extractDim(0), 1e-5);
  }

  @Test
  public void testCalcReturnsForMonths_Empty()
  {
    Sequence seq = new Sequence("test");
    Sequence returns = FinLib.calcReturnsForMonths(seq, 12);
    assertEquals(0, returns.size());
  }

  @Test
  public void testCalcReturnsForMonths_Monthly()
  {
    Sequence seq = new Sequence("test");
    for (int i = 1; i <= 12; ++i) {
      seq.addData(i, LocalDate.of(2000, i, 1));
    }
    assertEquals(12, seq.length());

    // One partial 18 month period.
    Sequence returns = FinLib.calcReturnsForMonths(seq, 18);
    assertEquals(1, returns.size());
    assertEquals(1, returns.getNumDims());
    double m = FinLib.ret2mul(returns.get(0, 0));
    assertEquals(12.0, m, 1e-6);

    // Only one 12 month period.
    returns = FinLib.calcReturnsForMonths(seq, 12);
    assertEquals(1, returns.size());
    assertEquals(1, returns.getNumDims());
    m = FinLib.ret2mul(returns.get(0, 0));
    assertEquals(12.0, m, 1e-6);

    // Two 11 month periods.
    returns = FinLib.calcReturnsForMonths(seq, 11);
    assertEquals(2, returns.size());
    assertEquals(1, returns.getNumDims());
    assertEquals(11.0, FinLib.ret2mul(returns.get(0, 0)), 1e-6);
    assertEquals(6.0, FinLib.ret2mul(returns.get(1, 0)), 1e-6);

    // Seven 6 month periods.
    returns = FinLib.calcReturnsForMonths(seq, 6);
    assertEquals(7, returns.size());
    assertEquals(1, returns.getNumDims());
    double[] expected = new double[] { 6.0, 7.0 / 2.0, 8.0 / 3.0, 9.0 / 4.0, 2.0, 11.0 / 6.0, 12.0 / 7.0 };
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], FinLib.ret2mul(returns.get(i, 0)), 1e-6);
    }
  }

  @Test
  public void testDailyToMonthly()
  {
    // Create daily data.
    Sequence daily = new Sequence("test");
    LocalDate date = LocalDate.of(2000, Month.JANUARY, 25);
    date = TimeLib.getClosestBusinessDay(date, false);
    assertTrue(TimeLib.isBusinessDay(date));
    LocalDate startDate = date;

    daily.addData(1.0, date);
    for (int i = 1; i < 1995; ++i) { // late Jan 2000 -> early July 2005
      date = date.plusDays(1);
      if (TimeLib.isBusinessDay(date)) {
        daily.addData(daily.getLast(0) * 1.01, date);
      }
    }

    // Convert to monthly data and validate timestamps.
    date = startDate.plusMonths(1); // not enough days of data in first month
    Sequence monthly = FinLib.dailyToMonthly(daily);
    for (FeatureVec x : monthly) {
      assertTrue(TimeLib.isSameMonth(date, TimeLib.ms2date(x.getTime())));
      date = date.plusMonths(1);
    }
    assertEquals(Month.JULY, date.getMonth()); // not enough days in last month
  }

  @Test
  public void testCumulativeToReturns()
  {
    double[] returns = new double[] { 10.0, 5.0, -10.0, 0.0, 2.0, -3.0 };

    Sequence cumulative = new Sequence("test");
    cumulative.addData(1.0, LocalDate.of(2000, Month.JANUARY, 1));
    for (int i = 0; i < returns.length; ++i) {
      double x = cumulative.get(i, 0);
      double r = FinLib.ret2mul(returns[i]);
      cumulative.addData(x * r, LocalDate.of(2000, Month.JANUARY, i + 2));
    }

    Sequence seq = FinLib.cumulativeToReturns(cumulative);
    assertEquals(returns.length, seq.length());
    for (int i = 0; i < returns.length; ++i) {
      assertEquals(returns[i], seq.get(i, 0), 1e-6);
    }
  }

  @Test
  public void testCumulativeFromReturns()
  {
    double[] returnArray = new double[] { 10.0, 5.0, -10.0, 0.0, 2.0, -3.0 };

    Sequence cumulative = new Sequence("test-cumulative");
    Sequence returns = new Sequence("test-returns");
    cumulative.addData(1.0, LocalDate.of(2000, Month.JANUARY, 1));
    for (int i = 0; i < returnArray.length; ++i) {
      double x = cumulative.get(i, 0);
      double r = FinLib.ret2mul(returnArray[i]);
      cumulative.addData(x * r, LocalDate.of(2000, Month.JANUARY, i + 2));
      returns.addData(returnArray[i], LocalDate.of(2000, Month.JANUARY, i + 1));
    }

    Sequence seq = FinLib.cumulativeFromReturns(returns, 1.0, 0.0);
    assertTrue(seq.matches(cumulative));
    for (int i = 0; i < seq.length(); ++i) {
      double x = seq.get(i, 0);
      double y = cumulative.get(i, 0);
      assertEquals(x, y, 1e-6);
    }

    // Test round trip (back to `returns`).
    seq = FinLib.cumulativeToReturns(seq);
    assertTrue(seq.matches(returns));
    for (int i = 0; i < seq.length(); ++i) {
      double x = seq.get(i, 0);
      double y = returns.get(i, 0);
      assertEquals(x, y, 1e-6);
    }
  }

  @Test
  public void testSharpe()
  {
    // Example 2 from https://en.wikipedia.org/wiki/Sharpe_ratio
    Sequence portfolio = new Sequence("portfolio returns");
    portfolio.addData(-0.5);
    portfolio.addData(0.1);
    portfolio.addData(0.5);

    Sequence benchmark = new Sequence("benchmark returns");
    benchmark.addData(-0.48419);
    benchmark.addData(0.17234);
    benchmark.addData(0.46110);

    double sharpe = FinLib.sharpe(portfolio, benchmark);
    double expected = -0.2951444;
    assertEquals(expected, sharpe, 1e-6);
  }

  @Test
  public void testPresentValue()
  {
    final double value = 1000.0;
    final double interestRate = 10.0;
    final int nPeriods = 5;
    double pv = FinLib.getPresentValue(value, interestRate, nPeriods);
    assertEquals(620.92, pv, 0.0051); // from: https://en.wikipedia.org/wiki/Present_value#Present_value_of_a_lump_sum

    double fv = FinLib.getFutureValue(pv, interestRate, nPeriods);
    assertEquals(value, fv, 1e-6);
  }
}
