package org.minnen.dmswr;

import java.io.IOException;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.data.BengenEntry;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MarwoodEntry;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.utils.IntPair;
import org.minnen.dmswr.utils.TimeLib;

public class BengenMethod
{
  public static MonthlyInfo run(BengenEntry info)
  {
    List<MonthlyInfo> trajectory = new ArrayList<>();
    return run(info, trajectory);
  }

  public static MonthlyInfo run(BengenEntry info, List<MonthlyInfo> trajectory)
  {
    final int index = SwrLib.indexForTime(info.time);
    final double nestEgg = 1e6; // SwrLib.getNestEgg(index, 0, info.percentStock);
    return runForDuration(index, info.retirementYears, info.swr / 100.0, info.percentStock, nestEgg, trajectory);
  }

  public static MonthlyInfo run(MarwoodEntry info)
  {
    return run(info, null);
  }

  public static MonthlyInfo run(MarwoodEntry info, List<MonthlyInfo> trajectory)
  {
    final int index = SwrLib.indexForTime(info.retireTime);
    final double nestEgg = 1e6; // SwrLib.getNestEgg(index, info.lookbackYears, info.percentStock);
    return runForDuration(index, info.retirementYears, info.swr / 100.0, info.percentStock, nestEgg, trajectory);
  }

  /**
   * Simulate a retirement.
   * 
   * @param iStart index of retirement month (first withdrawal)
   * @param retirementYears number of years of retirement
   * @param withdrawalRate annual withdrawal rate as a percent (3.5 = 3.5%)
   * @param percentStock percent stock (vs. bonds) held in brokerage account
   * @param inflation if nominal, adjust for inflation here, else assume growth is pre-adjusted
   * @param trajectory if non-null, will be filled with monthly info objects (optional)
   * @return info for the final month: either the last month of retirement or the failure month
   */
  public static MonthlyInfo runForDuration(int iStart, int retirementYears, double withdrawalRate, int percentStock,
      double nestEgg, List<MonthlyInfo> trajectory)
  {
    final int iEnd = iStart + 12 * retirementYears;
    return run(iStart, iEnd, withdrawalRate, percentStock, nestEgg, trajectory);
  }

  /**
   * Simulate a Bengen-style retirement starting with a fixed withdrawal rate.
   * 
   * @param iStart index of retirement month (first withdrawal)
   * @param iEnd last index of simulation period (exclusive)
   * @param withdrawalRate annual withdrawal rate as a percent (3.5 = 3.5%) *
   * @param percentStock percent stock (vs. bonds) held in brokerage account
   * @param nestEgg portfolio balance at start of retirement
   * @param trajectory if non-null, will be filled with monthly info objects (optional)
   * @return info for the final month: either the last month of retirement or the failure month
   */
  public static MonthlyInfo run(int iStart, int iEnd, double withdrawalRate, int percentStock, double nestEgg,
      List<MonthlyInfo> trajectory)
  {
    // TODO change withdrawalRate arg to int (basis points) instead of double.
    assert iStart >= 0 && iStart < SwrLib.length();
    assert iEnd > iStart && iEnd <= SwrLib.length();
    assert withdrawalRate > 0.0 : withdrawalRate;
    assert percentStock >= 0 && percentStock <= 100;

    final long retireTime = SwrLib.time(iStart);
    final int swrBasisPoints = SwrLib.percentToBasisPoints(withdrawalRate);
    double balance = nestEgg;
    double monthlyWithdrawal = balance * withdrawalRate / 1200.0;
    final Inflation inflation = SwrLib.getInflationAdjustment();

    if (trajectory != null) trajectory.clear();

    MonthlyInfo info = null;
    for (int i = iStart; i < iEnd; ++i) {
      final double startBalance = balance;

      balance -= monthlyWithdrawal; // make withdrawal at the beginning of the month.
      if (balance > 0) {
        balance *= SwrLib.growth(i, percentStock); // remaining balance grows during the rest of month.
      }

      final double annualSalary = monthlyWithdrawal * 12;
      info = new MonthlyInfo(retireTime, SwrLib.time(i), swrBasisPoints, i - iStart + 1, monthlyWithdrawal,
          startBalance, balance, annualSalary);
      if (trajectory != null) trajectory.add(info);
      if (info.failed()) return info;

      assert balance > -1e-5; // TODO avoid floating point issues
      if (inflation == Inflation.Nominal) {
        monthlyWithdrawal *= SwrLib.inflation(i);
      }
    }

    if (trajectory != null) {
      MonthlyInfo.setFinalBalance(balance, trajectory);
    } else {
      info.finalBalance = balance;
    }
    assert info.finalBalance == balance;
    return info;
  }

  /** @return true if the withdrawal rate works for all retirement starting times. */
  public static boolean isSafe(int withdrawalRate, int retirementYears, int percentStock)
  {
    final int lastIndex = SwrLib.lastIndex(retirementYears);
    final double floatWR = withdrawalRate / 100.0;
    for (int i = 0; i <= lastIndex; ++i) {
      MonthlyInfo info = BengenMethod.runForDuration(i, retirementYears, floatWR, percentStock, 1e6, null);
      if (info.failed()) return false;
      assert info.endBalance > 0 && info.monthlyIncome > 0;
    }
    return true;
  }

  /**
   * Find the SWR for a given retirement date.
   * 
   * @param index index of retirement date
   * @param retirementYears number of years of retirement
   * @param percentStock percent stock (vs bonds) in asset allocation (70 = 70%)
   * @param quantum withdrawalRate % quantum == 0
   * @return safe withdrawal rate for the given retirement index and parameters
   */
  public static int findSwrForDate(int index, int retirementYears, int percentStock, int quantum)
  {
    assert index <= SwrLib.lastIndex(retirementYears);
    final int iEnd = index + retirementYears * 12;
    return findSwrForWindow(index, iEnd, percentStock, quantum);
  }

  public static int findSwrForWindow(int iStart, int iEnd, int percentStock, int quantum)
  {
    assert quantum >= 1;

    int lowSWR = 0;
    int highSWR = 10001;

    final int years = (iEnd - iStart) / 12;
    BengenEntry entry = BengenTable.get(SwrLib.time(iStart), years - 1, percentStock);
    if (entry != null) {
      highSWR = entry.swr; // SWR for N years can't be larger than SWR for (N-1) years
    }

    while (highSWR - lowSWR > quantum) {
      final int swr = (lowSWR + highSWR) / (2 * quantum) * quantum;
      assert swr >= lowSWR && swr <= highSWR && swr % quantum == 0 : swr;
      MonthlyInfo info = BengenMethod.run(iStart, iEnd, swr / 100.0, percentStock, 1e6, null);
      if (info.ok()) {
        lowSWR = swr;
      } else {
        highSWR = swr;
      }
    }
    return lowSWR;
  }

  /**
   * Determine the Bengen SWR for a retirement of `years` with a `percentStock` held in stock.
   * 
   * All calculated SWRs will be rounded down to the nearest five basis points (3.27% -> 3.25%).
   * 
   * @param retirementYears length of retirement, i.e. the account balance must be >= 0 for this many years
   * @param percentStock the percent of stock (vs. bond) in the brokerage account
   * @return the largest Bengen SWR as an annualized percent (325 = 3.25%)
   */
  public static int findSWR(int retirementYears, int percentStock)
  {
    return findSWR(retirementYears, percentStock, 5);
  }

  /**
   * Determine the Bengen SWR for a retirement of `years` with a `percentStock` held in stock.
   * 
   * @param retirementYears length of retirement, i.e. the account balance must be >= 0 for this many years
   * @param percentStock the percent of stock (vs. bond) in the brokerage account
   * @param quantum require the SWR to have a multiple of this number of basis points
   * @return the largest Bengen SWR as an annualized percent (325 = 3.25%)
   */
  public static int findSWR(int retirementYears, int percentStock, int quantum)
  {
    assert retirementYears > 0 && percentStock >= 0 && percentStock <= 100 && quantum >= 1;

    // Binary search for largest WR that is always safe.
    int lowSWR = 10; // 0.1% will always works
    int highSWR = 10000; // never go over 100%
    while (highSWR - lowSWR > quantum) {
      final int swr = (lowSWR + highSWR) / (2 * quantum) * quantum;
      assert swr >= lowSWR && swr <= highSWR && swr % quantum == 0 : swr;
      if (BengenMethod.isSafe(swr, retirementYears, percentStock)) {
        lowSWR = swr;
      } else {
        highSWR = swr;
      }
    }
    return lowSWR;
  }

  /**
   * Calculate SWR for all retirement periods (342 => 3.42%).
   * 
   * Note that this function does NOT ensure that SWR(N years) <= SWR(N-1 years). This check is performed when the
   * Bengen Table is loaded.
   * 
   * @param retirementYears length of retirement, i.e. the account balance must be >= 0 for this many years
   * @param percentStock the percent of stock (vs. bond) in the brokerage account
   * @param includePartialWindows if True, include SWR for incomplete/ongoing retirements
   * @return Sequence holding the SWR for each starting month.
   */
  public static Sequence calcSwrAcrossTime(int retirementYears, int percentStock, boolean includePartialWindows)
  {
    Sequence seq = new Sequence(
        String.format("%d year SWR (%d/%d)", retirementYears, percentStock, 100 - percentStock));

    final int retirementMonths = retirementYears * 12;
    final int lastIndex = includePartialWindows ? SwrLib.length() - 1 : SwrLib.lastIndex(retirementYears);
    for (int i = 0; i <= lastIndex; ++i) {
      final int iEnd = Math.min(i + retirementMonths, SwrLib.length());
      final int swr = findSwrForWindow(i, iEnd, percentStock, 1);
      seq.addData(swr, SwrLib.time(i));
    }
    return seq;
  }

  /** Print information about the best Bengen SWR for different retirement durations. */
  public static void printSWRs(int[] retirementYearsList, int[] percentStockList)
  {
    for (int retirementYears : retirementYearsList) {
      for (int percentStock : percentStockList) {
        final int swr = BengenTable.getSWR(retirementYears, percentStock);
        System.out.printf("%d %d: %d\n", retirementYears, percentStock, swr);
      }
    }
  }

  public static IntPair getSuccessFail(int withdrawalRate, int retirementYears, int percentStock)
  {
    // TODO how to handle case where `withdrawalRate` would fail for a shorter duration starting a time that's too
    // recent to fit a full `retirementYears` retirement?
    final int n = SwrLib.lastIndex(retirementYears) + 1;
    int nWin = 0;
    for (int i = 0; i < n; ++i) {
      final int swr = BengenTable.get(SwrLib.time(i), retirementYears, percentStock).swr;
      if (withdrawalRate <= swr) ++nWin;
    }
    final int nFail = n - nWin;
    return new IntPair(nWin, nFail);
  }

  public static void main(String[] args) throws IOException
  {
    SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real); // DMSWR data not needed

    // Examples from paper.
    MonthlyInfo info = BengenMethod.runForDuration(SwrLib.indexForTime(Month.JANUARY, 1950), 30, 4.0, 50, 1e6, null);
    System.out.printf("[%s] -> [%s] %.2f%% -> %f\n", TimeLib.formatYM(info.retireTime),
        TimeLib.formatYM(info.currentTime), info.swr / 100.0, info.finalBalance / 1e6);

    List<MonthlyInfo> trajectory = new ArrayList<>();
    info = BengenMethod.runForDuration(SwrLib.indexForTime(Month.JANUARY, 1965), 30, 8.0, 50, 1e6, trajectory);
    System.out.printf("[%s] -> [%s] %.2f%% -> %f\n", TimeLib.formatYM(info.retireTime),
        TimeLib.formatYM(info.currentTime), info.swr / 100.0, info.finalBalance / 1e6);
    System.out.printf("Failing month: %s\n", trajectory.get(trajectory.size() - 1));

    // Worst time to start a 35-year retirement with a 75/25 asset allocation.
    int swr = BengenTable.getSWR(35, 75);
    System.out.printf("SWR (35, 75): %d\n", swr);
    for (FeatureVec v : BengenTable.getAcrossTime(35, 75)) {
      final int wr = (int) Math.round(v.get(0));
      assert wr >= swr;
      if (wr == swr) {
        System.out.printf(" [%s] <-- worst month to retire\n", TimeLib.formatYM(v.getTime()));
      }
    }

    // Jan 1966 retiree ending in Dec 2004.
    System.out.println("Retiree 1966 -> 2004, WR in Jan 1975");
    int retirementYears = 39;
    int percentStock = 75;
    double wr = BengenTable.getSWR(retirementYears, percentStock) / 100.0;
    info = BengenMethod.runForDuration(SwrLib.indexForTime(Month.JANUARY, 1966), retirementYears, wr, percentStock, 1e6,
        trajectory);
    System.out.printf("[%s] -> [%s] %.2f%% -> %f\n", TimeLib.formatYM(info.retireTime),
        TimeLib.formatYM(info.currentTime), info.swr / 100.0, info.finalBalance / 1e6);
    info = trajectory.get(9 * 12); // Jan 1975 = 9 years later
    System.out.println(info);
  }
}
