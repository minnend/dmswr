package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.SwrLib;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.utils.Writer;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class BengenSWRs
{
  /** Save Bengen SWR (BaselineSWR) values in a CSV file. */
  private static void saveBengenSwrAsCsv() throws IOException
  {
    File file = new File(DataIO.getFinancePath(), "baseline-swr.csv");
    try (Writer writer = new Writer(file)) {
      writer.writeln("# Baseline Safe Withdrawal Rate based on Bengen (1994)");
      writer.writeln("# Fields:");
      writer.writeln("# 1) Retirement duration in years");
      writer.writeln("# 2) Percent stock in a stock/bond portfolio");
      writer.writeln("# 3) Safe withdrawal rate (annualized, 314 => 3.14%)");
      for (int retirementYears = 1; retirementYears <= 60; ++retirementYears) {
        for (int percentStock : SwrLib.percentStockList) {
          final int swr = BengenTable.getSWR(retirementYears, percentStock);
          writer.writef("%d,%d,%d\n", retirementYears, percentStock, swr);
        }
      }
    }
  }

  /** Generate a bar chart with Bengen SWR (BaselineSWR) values. */
  private static void generateBengenSwrGraph() throws IOException
  {
    // Bengen SWR for different retirement durations.
    final int percentStock = 75;
    Sequence seqBengenSWR = new Sequence("Bengen SWR");
    List<String> labels = new ArrayList<>();
    for (int retirementYears = 10; retirementYears <= 60; ++retirementYears) {
      final int swr = BengenTable.getSWR(retirementYears, percentStock);
      seqBengenSWR.addData(swr / 100.0);
      labels.add(String.format("%d", retirementYears));
      // System.out.printf("%d %d\n", retirementYears, swr);
    }

    // Generate graph showing Bengen SWRs.
    File file = new File(DataIO.getOutputPath(), "bengen-swr-durations.html");
    ChartConfig config = ChartConfig.build(file, ChartConfig.Type.Bar, "BaselineSWR for Different Retirement Durations",
        labels.toArray(new String[0]), null, "100%", "700px", 0.0, seqBengenSWR.getFirst(0), 0.5, ChartScaling.LINEAR,
        ChartTiming.INDEX, 0, seqBengenSWR);
    config.setAxisTitles("Retirement Duration (years)", "Withdrawal Rate");
    config.setAxisLabelFontSize(32);
    config.setAxisTitleFontSize(32);
    config.setTickFormatter(null, "return this.value + '%';");
    config.setTitleConfig("margin: 0, y: 68, style: { fontSize: 42 }");
    config.setTickInterval(2, 1);
    config.setEndOnTick(false);
    config.setDataLabelConfig(true, -90, "#fff", 2, 1, 6, 32, false);
    config.setAnimation(false);
    Chart.saveChart(config);

  }

  public static void main(String[] args) throws IOException
  {
    SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real); // DMSWR data not needed

    saveBengenSwrAsCsv();
    generateBengenSwrGraph();
  }
}
