package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.MarwoodMethod;
import org.minnen.dmswr.NestEggCalculator;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MarwoodTable;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class DmswrGraph
{
  public static void createDmswrGraph(int retirementYears, int lookbackYears, int percentStock) throws IOException
  {
    final int bengenSWR = BengenTable.getSWR(retirementYears, percentStock);
    final int lookbackMonths = lookbackYears * 12;
    final int iFirstWithHistory = lookbackMonths;
    final int iStartSim = iFirstWithHistory;

    Sequence seqMarwoodSWR = new Sequence(String.format("DMSWR (%d years)", retirementYears));

    NestEggCalculator nestEggCalculator = NestEggCalculator.constant(1e6);
    List<MonthlyInfo> dmswrPath = MarwoodMethod.findDMSWR(retirementYears, lookbackYears, percentStock,
        nestEggCalculator);
    assert dmswrPath.size() == (SwrLib.length() - iStartSim);

    for (int iRetire = iStartSim; iRetire < SwrLib.length(); ++iRetire) {
      final long now = SwrLib.time(iRetire);
      MonthlyInfo dmswr = dmswrPath.get(iRetire - iStartSim);
      // MarwoodEntry dmswr = MarwoodTable.get(SwrLib.time(iRetire), retirementYears, lookbackYears, percentStock);

      if (iRetire == iStartSim || iRetire == SwrLib.length() - 1) System.out.printf("%d\n", dmswr.swr);
      seqMarwoodSWR.addData(dmswr.swr / 100.0, now);
    }

    // The crystal ball SWR is the same as Bengen per retirement start date.
    Sequence seqCrystalSWR = BengenMethod.calcSwrAcrossTime(retirementYears, percentStock, false);
    seqCrystalSWR.setName(String.format("CBSWR (%d years)", retirementYears));
    seqCrystalSWR._div(100.0); // convert basis points to percentages (342 -> 3.42).
    int index = seqCrystalSWR.getIndexAtOrAfter(seqMarwoodSWR.getStartMS());
    seqCrystalSWR = seqCrystalSWR.subseq(index);

    // Create sequence for BaselineSWR. It's constant, but it's better to plot it so that it shows up in the legend.
    final double bengenAsPercent = bengenSWR / 100.0;
    Sequence seqBengenSWR = new Sequence(String.format("BaselineSWR (%d years)", retirementYears));
    for (FeatureVec v : seqMarwoodSWR) {
      seqBengenSWR.addData(bengenAsPercent, v.getTime());
    }

    SwrLib.dumpSequences(new File(DataIO.getOutputPath(), "dmswr.txt"), seqCrystalSWR, seqMarwoodSWR, seqBengenSWR);

    // Create DMSWR chart (withdrawal rates vs. CBSWR).
    String title = String.format("Safe Withdrawal Rates (%d years, %d%% stock / %d%% bonds)", retirementYears,
        percentStock, 100 - percentStock);
    ChartConfig config = Chart.saveLineChart(new File(DataIO.getOutputPath(), "dmswr.html"), title, "100%", "800px",
        ChartScaling.LINEAR, ChartTiming.MONTHLY, seqBengenSWR, seqCrystalSWR, seqMarwoodSWR);
    // config.addPlotLineY(new PlotLine(bengenSWR / 100.0, 3.0, "#272", "dash"));
    config.setColors(new String[] { "#272", "#7cb5ec", "#434348" });
    config.setLineWidth(3);
    config.setTitleConfig("margin: 0, y: 40, style: { fontSize: 40 }");
    config.setAxisLabelFontSize(32);
    config.setTickInterval(36, -1);
    config.setMinMaxY(3.0, 14.0);
    config.setAnimation(false);
    config.setTickFormatter("return this.value.split(' ')[1];", "return this.value + '%';");
    config.setAxisTitles("Retirement Date (rd)", null);
    config.setLegendConfig(
        "align: 'right', verticalAlign: 'top', x: -20, y: 70, layout: 'vertical', floating: true, itemStyle: {"
            + "fontSize: 32, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, shadow: true, symbolWidth: 32,");
    config.setAnimation(false);
    Chart.saveChart(config);
  }

  public static void main(String[] args) throws IOException
  {
    final int retirementYears = 30;
    final int lookbackYears = 20;
    final int percentStock = 75;
    
    SwrLib.setupWithDefaultFiles();
    System.out.printf("DMSWR entries: %d\n", MarwoodTable.marwoodMap.size());
    System.out.printf("DMSWR sequences: %d\n", MarwoodTable.marwoodSequences.size());

    createDmswrGraph(retirementYears, lookbackYears, percentStock);
  }
}
