package org.minnen.dmswr;

import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class CompareVPW
{
  /**
   * Rates based on the VPW calculator for a 75/25 stock/bond split retiring at age 65 with no SS or pension.
   * 
   * More information here: https://www.bogleheads.org/wiki/Variable_percentage_withdrawal
   * 
   * And the online spreadsheet is here: https://drive.google.com/open?id=1rzHAQxcAnfX9NVGLoCp9Kvv5W3OYCDdLbR6fGS-uAwE
   */
  public static final int[]   vpw_wr_stock75_age65;

  private static final int    retirementYears = 35;  // 65 through 99
  private static final int    lookbackYears   = 20;
  private static final int    percentStock    = 75;
  private static final double nestEgg         = 1e6; // retire with $1M
  static {
    vpw_wr_stock75_age65 = new int[] { 530, 540, 540, 550, 560, 570, 580, 590, 600, 620, 630, 640, 660, 680, 700, 720,
        740, 770, 800, 840, 880, 920, 970, 1040, 1110, 1200, 1300, 1440, 1610, 1840, 2170, 2660, 3470, 5100, 10000 };
  }

  private static final int[]       vpwRates = vpw_wr_stock75_age65;

  private static List<MonthlyInfo> bengenPath;
  private static List<MonthlyInfo> dmswrPath;
  private static List<MonthlyInfo> vpwPath;

  public static List<MonthlyInfo> runVPW(int retireYear, int percentStock, double nestEgg)
  {
    final int iRetire = SwrLib.indexForTime(Month.JANUARY, retireYear);
    final long retireTime = SwrLib.time(iRetire);
    final Inflation inflation = SwrLib.getInflationAdjustment();

    List<MonthlyInfo> trajectory = new ArrayList<>();
    double balance = nestEgg;
    for (int i = 0; i < vpwRates.length; ++i) {
      final int iCurrent = SwrLib.indexForTime(Month.JANUARY, retireYear + i);
      final int iNext = iCurrent + 12;

      final double wr = vpwRates[i] / 10000.0;
      final double income = balance * wr;
      final double adjustedIncome = (inflation == Inflation.Real ? income
          : income * SwrLib.inflation(iCurrent, iRetire));
      final double startBalance = balance;
      balance -= income;
      balance *= SwrLib.growth(iCurrent, iNext, percentStock);

      MonthlyInfo info = new MonthlyInfo(retireTime, SwrLib.time(iCurrent), vpwRates[i], i * 12 + 1,
          adjustedIncome / 12.0, startBalance, balance, adjustedIncome);
      trajectory.add(info);
    }
    MonthlyInfo.setFinalBalance(balance, trajectory);
    return trajectory;
  }

  public static void calculatePaths(int retireYear)
  {
    final long retireTime = TimeLib.toMs(retireYear, Month.JANUARY, 1);

    // Simulate a Bengen path.
    final int iStart = SwrLib.indexForTime(retireTime);
    final int iEnd = iStart + retirementYears * 12;
    final double minSWR = BengenTable.getSWR(retirementYears, percentStock) / 100.0;
    bengenPath = new ArrayList<>();
    BengenMethod.run(iStart, iEnd, minSWR, percentStock, nestEgg, bengenPath);

    // Simulate a DMSWR path.
    dmswrPath = MarwoodMethod.reretire(retireTime, retirementYears, lookbackYears, percentStock, nestEgg);
    assert dmswrPath.size() == (retirementYears * 12);

    // Simulate a VPW path.
    vpwPath = runVPW(retireYear, percentStock, nestEgg);
  }

  public static void createComparisonGraphRate(int retireYear) throws IOException
  {
    // Get rates for Bengen's method.
    Sequence seqBengenRate = new Sequence(String.format("MinSWR (%d years)", retirementYears));
    for (MonthlyInfo info : bengenPath) {
      if (TimeLib.ms2date(info.currentTime).getMonth() == Month.JANUARY) {
        seqBengenRate.addData(info.percent(), info.currentTime);
      }
    }
    assert seqBengenRate.size() == vpwRates.length;

    // Get rates for DMSWR.
    Sequence seqDmswrRate = new Sequence(String.format("DMSWR (%d years)", retirementYears));
    for (MonthlyInfo info : dmswrPath) {
      if (TimeLib.ms2date(info.currentTime).getMonth() == Month.JANUARY) {
        seqDmswrRate.addData(info.percent(), info.currentTime);
      }
    }
    assert seqDmswrRate.size() == vpwRates.length;

    // Get rates for VPW.
    Sequence seqVpwRate = new Sequence(String.format("VPW (%d years)", retirementYears));
    int year = retireYear;
    for (int vpw : vpwRates) {
      seqVpwRate.addData(vpw / 100.0, TimeLib.toMs(year, Month.JANUARY, 1));
      ++year;
    }

    String title = String.format("SWR Comparison (%d years, %d/%d)", retirementYears, percentStock, 100 - percentStock);
    ChartConfig config = Chart.saveLineChart(new File(DataIO.getOutputPath(), "compare-vpw-rate.html"), title, "100%",
        "800px", ChartScaling.LINEAR, ChartTiming.MONTHLY, seqVpwRate, seqDmswrRate, seqBengenRate);
    config.setColors(new String[] { "#272", "#7cb5ec", "#434348" });
    config.setLineWidth(3);
    config.setTitleConfig("margin: 0, y: 50, style: { fontSize: 36 }");
    config.setAxisLabelFontSize(32);
    // config.setTickInterval(2, -1);
    config.setMinMaxY(0.0, 100.0);
    config.setAnimation(false);
    config.setTickFormatter("return this.value.split(' ')[1];", "return this.value + '%';");
    config.setAxisTitles("Retirement Date", null);
    config.setLegendConfig(
        "align: 'left', verticalAlign: 'top', x: 120, y: 70, layout: 'vertical', floating: true, itemStyle: {"
            + "fontSize: 28, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, shadow: true, symbolWidth: 32,");
    config.setAnimation(false);
    Chart.saveChart(config);
  }

  public static void createComparisonGraphIncome(int retireYear, Inflation inflation) throws IOException
  {
    final int iRetire = SwrLib.indexForTime(Month.JANUARY, retireYear);

    // Get income for Bengen's method.
    Sequence seqBengenIncome = new Sequence(String.format("MinSWR (%d years)", retirementYears));
    for (MonthlyInfo info : bengenPath) {
      if (TimeLib.ms2date(info.currentTime).getMonth() == Month.JANUARY) {
        double income = info.bengenSalary;
        if (inflation == Inflation.Nominal) {
          income *= SwrLib.inflation(iRetire, info.index);
        }
        seqBengenIncome.addData(income, info.currentTime);
      }
    }
    assert seqBengenIncome.size() == vpwRates.length;
    System.out.printf("Bengen: $%.2f\n", bengenPath.get(0).finalBalance);

    // Get income for DMSWR.
    Sequence seqDmswrIncome = new Sequence(String.format("DMSWR (%d years)", retirementYears));
    for (MonthlyInfo info : dmswrPath) {
      if (TimeLib.ms2date(info.currentTime).getMonth() == Month.JANUARY) {
        double income = info.marwoodSalary;
        if (inflation == Inflation.Nominal) {
          income *= SwrLib.inflation(iRetire, info.index);
        }
        seqDmswrIncome.addData(income, info.currentTime);
      }
    }
    assert seqDmswrIncome.size() == vpwRates.length;
    System.out.printf(" DMSWR: $%.2f\n", dmswrPath.get(0).finalBalance);

    // Get income for VPW.
    Sequence seqVpwIncome = new Sequence(String.format("VPW (%d years)", retirementYears));
    for (MonthlyInfo info : vpwPath) {
      if (TimeLib.ms2date(info.currentTime).getMonth() == Month.JANUARY) {
        double income = info.bengenSalary;
        if (inflation == Inflation.Nominal) {
          income *= SwrLib.inflation(iRetire, info.index);
        }
        seqVpwIncome.addData(income, info.currentTime);
      }
    }
    assert seqVpwIncome.size() == vpwRates.length;
    System.out.printf("   VPW: $%.2f\n", vpwPath.get(0).finalBalance);

    String title = String.format("SWR Comparison (%d years, %d/%d, %s)", retirementYears, percentStock,
        100 - percentStock, inflation == Inflation.Real ? "real" : "nominal");
    String filename = String.format("compare-vpw-income-%s-%d.html", inflation == Inflation.Real ? "real" : "nominal",
        retireYear);
    ChartConfig config = Chart.saveLineChart(new File(DataIO.getOutputPath(), filename), title, "100%", "800px",
        ChartScaling.LINEAR, ChartTiming.MONTHLY, seqVpwIncome, seqDmswrIncome, seqBengenIncome);
    config.setColors(new String[] { "#272", "#7cb5ec", "#434348" });
    config.setLineWidth(3);
    config.setTitleConfig("margin: 0, y: 50, style: { fontSize: 36 }");
    config.setAxisLabelFontSize(24);
    // config.setTickInterval(2, -1);
    // config.setMinMaxY(0.0, 100.0);
    config.setAnimation(false);
    config.setTickFormatter("return this.value.split(' ')[1];",
        "return '$' + Highcharts.numberFormat(this.value, 0, '.', ',');");
    config.setAxisTitles("Retirement Date", null);
    // config.setLegendConfig(
    // "align: 'left', verticalAlign: 'top', x: 120, y: 70, layout: 'vertical', floating: true, itemStyle: {"
    // + "fontSize: 24, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, shadow: true, symbolWidth: 32,");
    config.setAnimation(false);
    Chart.saveChart(config);
  }

  public static void main(String[] args) throws IOException
  {
    SwrLib.setupWithDefaultFiles(Inflation.Real);

    final int retireYear = 1957;
    calculatePaths(retireYear);
    createComparisonGraphRate(retireYear);
    createComparisonGraphIncome(retireYear, Inflation.Real);

    // for (int retireYear = 1900; retireYear <= 1985; retireYear += 5) {
    // System.out.printf("%d\n", retireYear);
    // calculatePaths(retireYear);
    // createComparisonGraphIncome(retireYear, Inflation.Nominal);
    // createComparisonGraphIncome(retireYear, Inflation.Real);
    // }
  }
}
