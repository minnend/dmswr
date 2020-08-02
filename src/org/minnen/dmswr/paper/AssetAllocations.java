package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.MarwoodMethod;
import org.minnen.dmswr.NestEggCalculator;
import org.minnen.dmswr.SwrLib;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class AssetAllocations
{
  public static void main(String[] args) throws IOException
  {
    // TODO Generate DMSWR tables for different allocations instead of computing them here.
    SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real);

    final int retirementYears = 30;
    final int lookbackYears = 20;
    final int lookbackMonths = lookbackYears * 12;
    final int iFirstWithHistory = lookbackMonths;
    final int iStartSim = iFirstWithHistory;
    NestEggCalculator nestEggCalculator = NestEggCalculator.constant(1e6);

    List<Sequence> sequences = new ArrayList<>();
    for (int percentStock = 0; percentStock <= 100; percentStock += 25) {
      Sequence seq = new Sequence(String.format("DMSWR (%d / %d)", percentStock, 100 - percentStock));
      System.out.println(seq.getName());

      List<MonthlyInfo> infos = MarwoodMethod.findDMSWR(retirementYears, lookbackYears, percentStock,
          nestEggCalculator);
      assert infos.size() == (SwrLib.length() - iStartSim);

      for (int iRetire = iStartSim; iRetire < SwrLib.length(); ++iRetire) {
        final long now = SwrLib.time(iRetire);
        MonthlyInfo dmswr = infos.get(iRetire - iStartSim);
        // MarwoodEntry dmswr = MarwoodTable.get(SwrLib.time(iRetire), retirementYears, lookbackYears, percentStock);
        seq.addData(dmswr.swr / 100.0, now);
      }
      sequences.add(seq);
    }

    SwrLib.dumpSequences(new File(DataIO.getOutputPath(), "figure-7.txt"), sequences);

    // Create DMSWR chart (withdrawal rates vs. CBSWR).
    String title = String.format("DMSWR for Different Asset Allocations (%d years)", retirementYears);
    ChartConfig config = Chart.saveLineChart(new File(DataIO.getOutputPath(), "dmswr-asset-allocation.html"), title,
        "100%", "800px", ChartScaling.LINEAR, ChartTiming.MONTHLY, sequences);

    config.setLineWidth(3);
    config.setTitleConfig("margin: 0, y: 40, style: { fontSize: 40 }");
    config.setAxisLabelFontSize(32);
    config.setTickInterval(36, -1);
    config.setMinMaxY(2.0, 15.0);
    config.setAnimation(false);
    config.setTickFormatter("return this.value.split(' ')[1];", "return this.value + '%';");
    config.setAxisTitles("Retirement Date (rd)", null);
    config.setLegendConfig(
        "align: 'left', verticalAlign: 'top', x: 100, y: 80, layout: 'vertical', floating: true, itemStyle: {"
            + "fontSize: 32, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, "
            + "itemMarginTop: 16, itemMarginBottom: -16, shadow: true, symbolWidth: 32,");
    config.setAnimation(false);
    Chart.saveChart(config);
  }
}
