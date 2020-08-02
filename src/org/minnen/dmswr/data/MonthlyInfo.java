package org.minnen.dmswr.data;

import java.util.List;

import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.utils.TimeLib;

/**
 * Holds information about the results of a retirement simulation for a particular month.
 */
public class MonthlyInfo
{
  /** Time (in ms) when the retirement started. */
  public final long   retireTime;

  /** Time (in ms) corresponding to this month. */
  public final long   currentTime;

  /** Index into a data sequence (stock returns, bonds, cpi, etc.). */
  public final int    index;

  /** Number of months of retirement so far. */
  public final int    retirementMonth;

  /** Monthly "salary", i.e. the withdrawal amount for this month. */
  public final double monthlyIncome;

  /** Account balance at the beginning of the month. */
  public final double startBalance;

  /** Account balance at the end of the month. */
  public final double endBalance;

  /** DMSWR in basis points (314 = 3.14%). */
  public final int    swr;

  /** Number of virtual retirement months for calculating DMSWR. */
  public final int    virtualRetirementMonths;

  /** Implied annual salary for the Bengen method. */
  public final double bengenSalary;

  /** Implied annual salary for the Marwood method. */
  public final double marwoodSalary;

  /** Implied annual salary for the Crystal Ball method. */
  public final double crystalSalary;

  /** Final balance at the end of retirement (not end of this month). */
  public double       finalBalance;           // TODO safer to make this `final` but complicates calculations

  /** Construct MonthlyInfo for the Bengen method. */
  public MonthlyInfo(long retireTime, long currentTime, int swr, int retirementMonth, double monthlyIncome,
      double startBalance, double endBalance, double bengenSalary)
  {
    this.retireTime = retireTime;
    this.currentTime = currentTime;
    this.index = SwrLib.indexForTime(currentTime);
    this.swr = swr;
    this.retirementMonth = retirementMonth;
    this.monthlyIncome = monthlyIncome;
    this.startBalance = startBalance;
    this.endBalance = endBalance;
    this.bengenSalary = bengenSalary;

    this.finalBalance = Double.NaN; // must be filled in later

    // The following values are not used for a Bengen simulation.
    this.virtualRetirementMonths = -1;
    this.marwoodSalary = Double.NaN;
    this.crystalSalary = Double.NaN;
  }

  /** Construct MonthlyInfo for DMSWR. */
  public MonthlyInfo(long retireTime, long currentTime, int retirementMonth, double monthlyIncome, double startBalance,
      double endBalance, int dmswr, int virtualRetirementMonths, double bengenSalary, double marwoodSalary,
      double crystalSalary)
  {
    this.retireTime = retireTime;
    this.currentTime = currentTime;
    this.index = SwrLib.indexForTime(currentTime);
    this.retirementMonth = retirementMonth;
    this.monthlyIncome = monthlyIncome;
    this.startBalance = startBalance;
    this.endBalance = endBalance;

    this.swr = dmswr;
    this.virtualRetirementMonths = virtualRetirementMonths;
    this.bengenSalary = bengenSalary;
    this.marwoodSalary = marwoodSalary;
    this.crystalSalary = crystalSalary;

    this.finalBalance = Double.NaN; // must be filled in later
  }

  /** @return annualized withdrawal rate (salary / balance) as a percent (4.0 = 4.0% per year). */
  public double percent()
  {
    return monthlyIncome * 12.0 / startBalance * 100.0;
  }

  /** @return true if this month failed, i.e. the balance couldn't cover the salary. */
  public boolean failed()
  {
    return monthlyIncome >= startBalance;
  }

  /** @return true if this month succeeded, i.e. the balance was larger than the salary. */
  public boolean ok()
  {
    return !failed();
  }

  @Override
  public String toString()
  {
    return String.format("[%s|%s: $%.2f, $%.2f = %.3f%%]", TimeLib.formatMonth(currentTime), index, monthlyIncome,
        startBalance, percent());
  }

  public static List<MonthlyInfo> setFinalBalance(double finalBalance, List<MonthlyInfo> list)
  {
    for (MonthlyInfo info : list) {
      info.finalBalance = finalBalance;
    }
    return list;
  }
}
