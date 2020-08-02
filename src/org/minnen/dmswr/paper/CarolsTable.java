package org.minnen.dmswr.paper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.utils.FinLib;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.utils.FinLib.Inflation;

public class CarolsTable
{
  public static void createCarolTable(int lookbackYears, int percentStock)
  {
    final int[] years = new int[] { 1970, 1971, 1972, 1973, 1974, 1975, 1980, 1985, 1990, 1995, 2000, 2005 };
    Map<Integer, Integer> yearsToPrint = new TreeMap<>();
    for (int i = 0; i < years.length; ++i) {
      final int year = years[i];
      final int next = i + 1 < years.length ? years[i + 1] : year + 1;
      yearsToPrint.put(year, next);
    }

    final double nestEgg = 1e6;
    final int retirementYears = 35;
    final int finalYear = years[0] + retirementYears - 1;
    final long timeStart = TimeLib.toMs(years[0], Month.JANUARY, 1);
    final long timeEnd = TimeLib.toMs(years[0] + retirementYears - 1, Month.DECEMBER, 1);
    final int iStart = SwrLib.indexForTime(timeStart);
    final int iEnd = SwrLib.indexForTime(timeEnd);

    final double bengenSWR = BengenTable.getSWR(retirementYears, percentStock) / 100.0;
    List<MonthlyInfo> bengenTrajectory = new ArrayList<>();
    BengenMethod.run(iStart, iEnd + 1, bengenSWR, percentStock, nestEgg, bengenTrajectory);
    assert bengenTrajectory.size() == (iEnd - iStart + 1);

    // List<MonthlyInfo> dmTrajectory = MarwoodMethod.reretire(timeStart, retirementYears, lookbackYears, percentStock,
    // Inflation.Nominal, nestEgg);
    // assert dmTrajectory.size() == (iEnd - iStart + 1);

    for (MonthlyInfo bengenInfo : bengenTrajectory) {
      final int index = bengenInfo.index;
      final LocalDate date = TimeLib.ms2date(bengenInfo.currentTime);

      final boolean isNewYear = (date.getMonth() == Month.JANUARY && yearsToPrint.containsKey(date.getYear()));
      final boolean isLastMonth = (date.getMonth() == Month.DECEMBER && date.getYear() == finalYear);
      if (!isNewYear && !isLastMonth) continue;
      final int year = (isLastMonth ? finalYear + 1 : date.getYear());

      final int nextRowYear = isLastMonth ? year + 1 : yearsToPrint.get(date.getYear());
      final int yearsBetween = nextRowYear - year;

      // MonthlyInfo dmInfo = dmTrajectory.get(index - iStart);
      // assert (dmInfo.retireTime == bengenInfo.retireTime) && (dmInfo.currentTime == bengenInfo.currentTime);
      // assert dmInfo.index == index;
      // assert Library.almostEqual(bengenInfo.bengenSalary, dmInfo.bengenSalary, 1e-6);

      final double totalGrowth = SwrLib.growth(index, index + 12 * yearsBetween, percentStock);
      final double annualizedGrowth = Math.pow(totalGrowth, 1.0 / yearsBetween);

      // growth *= SwrLib.inflation(index + 12, index); // adjust market returns for inflation
      final double totalInflation = SwrLib.inflation(index, index + 12 * yearsBetween);
      final double annualizedInflation = Math.pow(totalInflation, 1.0 / yearsBetween);
      final int yearsLeft = Math.max(0, retirementYears - (year - years[0]));

      final double inflation = SwrLib.inflation(bengenInfo.index, iStart); // inflation multiplier to retirement date
      // final double dmSalaryNominal = dmInfo.monthlyIncome * 12;
      final double bengenSalaryNominal = bengenInfo.monthlyIncome * 12;

      final double bengenBalance = (isLastMonth ? bengenInfo.endBalance : bengenInfo.startBalance);
      // final double dmBalance = (isLastMonth ? dmInfo.endBalance : dmInfo.startBalance);

      // final long lookbackDate = SwrLib.time(dmInfo.index - dmInfo.virtualRetirementMonths);
      // final double dmswr = dmInfo.swr / 100.0;

      final double withdrawalPercent = bengenSalaryNominal / bengenBalance * 100.0;

      final String sBengenBalance = String.format("\\$%s", FinLib.dollarFormatter.format(bengenBalance));
      final String sBengenBalanceReal = String.format("\\$%s",
          FinLib.dollarFormatter.format(bengenBalance * inflation));
      // final String sDmswrBalance = String.format("\\$%s", FinLib.dollarFormatter.format(dmBalance));
      final String sBengenSalary = String.format("\\$%s", FinLib.dollarFormatter.format(bengenSalaryNominal));
      final String sBengenSalaryReal = String.format("\\$%s",
          FinLib.dollarFormatter.format(bengenSalaryNominal * inflation));
      // final String sDmswrSalary = String.format("\\$%s", FinLib.dollarFormatter.format(dmSalaryNominal));

      System.out.printf(
          "%d & %2d & %12s & %12s & %10s & %10s & %5.1f\\hspace{.5pt}\\%% & %5.2f\\hspace{.5pt}\\%% & %5.2f\\hspace{.5pt}\\%% \\\\\n",
          year, yearsLeft, sBengenBalance, sBengenBalanceReal, sBengenSalary, sBengenSalaryReal,
          FinLib.mul2ret(annualizedGrowth), FinLib.mul2ret(annualizedInflation), withdrawalPercent);
    }
  }

  public static void main(String[] args) throws IOException
  {
    final int lookbackYears = 20;
    final int percentStock = 75;

    SwrLib.setupWithDefaultFiles(Inflation.Nominal);
    createCarolTable(lookbackYears, percentStock);
  }
}
