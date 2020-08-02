package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.SwrLib;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MonthlyInfo;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

/** Note: this graph was experimental is NOT used in the paper. */
public class DrawdownGraph
{
  public static void createDrawdownGraph(int percentStock) throws IOException
  {
    final int firstYear = 1970;
    final int finalRetirementYear = firstYear + 35;
    final int iEnd = SwrLib.indexForTime(TimeLib.toMs(finalRetirementYear, Month.JANUARY, 1));

    List<Sequence> paths = new ArrayList<>();
    for (int year = firstYear; year < finalRetirementYear; ++year) {
      final int retirementYears = finalRetirementYear - year;
      if (retirementYears < 30) break;
      final double bengenSWR = BengenTable.getSWR(retirementYears, percentStock) / 100.0;

      final int iStart = SwrLib.indexForTime(TimeLib.toMs(year, Month.JANUARY, 1));
      List<MonthlyInfo> bengenTrajectory = new ArrayList<>();
      BengenMethod.run(iStart, iEnd, bengenSWR, percentStock, 1e6, bengenTrajectory);

      Sequence path = new Sequence(String.format("Drawdown Path (%d)", year));
      for (MonthlyInfo info : bengenTrajectory) {
        path.addData(info.percent(), info.currentTime);
      }
      paths.add(path);
    }

    File file = new File(DataIO.getOutputPath(), "drawdown-paths.html");
    ChartConfig config = Chart.saveLineChart(file, "Actual Withdrawal Percents", "100%", "800px", ChartScaling.LINEAR,
        ChartTiming.MONTHLY, paths);
    config.setTickInterval(12, 1);
    config.setTickFormatter("return this.value.split(' ')[1];", "return this.value + '%';");
    // config.setColors(new String[] { "#eee" });
    Chart.saveChart(config);
  }

  public static void main(String[] args) throws IOException
  {
    SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real);

    final int percentStock = 75;
    createDrawdownGraph(percentStock);
  }
}
