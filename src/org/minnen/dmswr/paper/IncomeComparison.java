package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.List;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.MarwoodMethod;
import org.minnen.dmswr.NestEggCalculator;
import org.minnen.dmswr.data.MarwoodTable;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class IncomeComparison
{
  /**
   * Generate graph covering the full time range (starting in 1871 + `lookbackYears`).
   */
  public static void createLongIncomeGraph(int retirementYears, int lookbackYears, int percentStock) throws IOException
  {
    final int lookbackMonths = lookbackYears * 12;
    final int iFirstWithHistory = lookbackMonths;
    final int iStartSim = iFirstWithHistory;

    Sequence seqBengenSalary = new Sequence(String.format("BaselineSWR (%d years)", retirementYears));
    Sequence seqCrystalSalary = new Sequence(String.format("CBSWR (%d years)", retirementYears));
    Sequence seqMarwoodSalary = new Sequence(String.format("DMSWR (rd, rd+%d years)", retirementYears));

    NestEggCalculator nestEggCalculator = NestEggCalculator.inflationThenGrowth(1000, false);
    List<MonthlyInfo> infos = MarwoodMethod.findDMSWR(retirementYears, lookbackYears, percentStock, nestEggCalculator);
    assert infos.size() == (SwrLib.length() - iStartSim);

    for (int iRetire = iStartSim; iRetire < SwrLib.length(); ++iRetire) {
      final long now = SwrLib.time(iRetire);
      MonthlyInfo dmswr = infos.get(iRetire - iStartSim);

      if (iRetire == iStartSim || iRetire == SwrLib.length() - 1) System.out.printf("%d\n", dmswr.swr);
      seqMarwoodSalary.addData(dmswr.marwoodSalary, now);
      seqBengenSalary.addData(dmswr.bengenSalary, now);
      seqCrystalSalary.addData(dmswr.crystalSalary, now);
    }

    // Create DMSWR income chart vs. Bengen income.
    ChartConfig config = Chart.saveLineChart(
        new File(DataIO.getOutputPath(),
            String.format("dmswr-income-long-%d-%d-%d.html", retirementYears, lookbackYears, percentStock)),
        "Annualized Income (Nominal Dollars)", "100%", "800px", ChartScaling.LOGARITHMIC, ChartTiming.MONTHLY,
        seqBengenSalary, seqMarwoodSalary);
    // config.setColors(new String[] { "#434348", "#272" });
    config.setLineWidth(5);
    config.setAxisLabelFontSize(28);
    config.setTitleConfig("margin: 0, y: 40, style: { fontSize: 36 }");
    config.setTickInterval(48, -1);
    config.setTickFormatter("return this.value.split(' ')[1];", "return '$' + this.value;");
    config.setLegendConfig(
        "align: 'left', verticalAlign: 'top', x: 160, y: 70, layout: 'vertical', floating: true, itemStyle: {"
            + "fontSize: 28, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, shadow: true, symbolWidth: 32,");
    Chart.saveChart(config);
  }

  public static void createShortIncomeGraph(int startYear, int graphDurationYears, int retirementYears,
      int lookbackYears, int percentStock, boolean includeLegend) throws IOException
  {
    final int lookbackMonths = lookbackYears * 12;
    final int iStartSim = SwrLib.indexForTime(TimeLib.toMs(startYear, Month.JANUARY, 1));
    assert iStartSim >= lookbackMonths; // else not enough lookback data

    final int iEndSim = SwrLib.indexForTime(TimeLib.toMs(startYear + graphDurationYears - 1, Month.DECEMBER, 1));
    assert iEndSim <= SwrLib.length();

    Sequence seqBengenSalary = new Sequence(String.format("BaselineSWR(%d years)", retirementYears));
    Sequence seqMarwoodSalary = new Sequence(String.format("DMSWR(%d years)", retirementYears));

    NestEggCalculator nestEggCalculator = NestEggCalculator.monthlySavings(1e6, 500, false, true, false);
    List<MonthlyInfo> infos = MarwoodMethod.findDMSWR(iStartSim, iEndSim, retirementYears, lookbackYears, percentStock,
        nestEggCalculator);
    assert infos.size() == (iEndSim - iStartSim + 1);

    for (int iRetire = iStartSim; iRetire <= iEndSim; ++iRetire) {
      final long now = SwrLib.time(iRetire);
      MonthlyInfo dmswr = infos.get(iRetire - iStartSim);
      seqMarwoodSalary.addData(dmswr.marwoodSalary, now);
      seqBengenSalary.addData(dmswr.bengenSalary, now);
    }

    String filename = String.format("figure-6-%d.txt", startYear);
    SwrLib.dumpSequences(new File(DataIO.getOutputPath(), filename), seqBengenSalary, seqMarwoodSalary);

    // Create DMSWR income chart vs. Bengen income.
    File file = new File(DataIO.getOutputPath(),
        String.format("dmswr-income-%d-%d-%d-%d.html", startYear, retirementYears, lookbackYears, percentStock));
    String title = null; // "Annualized Income (Real Dollars)";
    ChartConfig config = Chart.saveLineChart(file, title, "100%", "800px", ChartScaling.LINEAR, ChartTiming.MONTHLY,
        seqBengenSalary, seqMarwoodSalary);
    config.setLineWidth(5);
    config.setAxisLabelFontSize(28);
    config.setTitleConfig("margin: 0, y: 40, style: { fontSize: 32 }");
    config.setTickInterval(12, 10000);
    // config.setMinorTickIntervalY(5000);
    config.setMinMaxY(15000, 80000);
    config.setTickFormatter("return this.value.split(' ')[1];",
        "return '$' + Highcharts.numberFormat(this.value, 0, '.', ',');");
    if (includeLegend) {
      config.setLegendConfig(
          "align: 'left', verticalAlign: 'top', x: 160, y: 30, layout: 'vertical', floating: true, itemStyle: {"
              + "fontSize: 28, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, shadow: true, symbolWidth: 32,");
    }
    Chart.saveChart(config);
  }

  public static void main(String[] args) throws IOException
  {
    final int retirementYears = 30;
    final int lookbackYears = 20;
    final int percentStock = 75;

    SwrLib.setupWithDefaultFiles(Inflation.Nominal);
    System.out.printf("DMSWR entries: %d\n", MarwoodTable.marwoodMap.size());
    System.out.printf("DMSWR sequences: %d\n", MarwoodTable.marwoodSequences.size());

    createLongIncomeGraph(retirementYears, lookbackYears, percentStock);

    createShortIncomeGraph(1929, 10, retirementYears, lookbackYears, percentStock, true);
    createShortIncomeGraph(1960, 10, retirementYears, lookbackYears, percentStock, false);
    createShortIncomeGraph(2000, 10, retirementYears, lookbackYears, percentStock, false);
  }
}
