package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.data.BengenEntry;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MarwoodTable;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.utils.FinLib.Inflation;

/**
 * Perform a walk-forward analysis to see when the BaselineSWR or DMSWR would have failed if it was followed in the
 * past.
 */
public class WalkForwardAnalysis
{
  public enum Method {
    BaselineSWR, DMSWR
  }

  public static void main(String[] args) throws IOException
  {
    final int retirementYears = 30;
    final int percentStock = 75;
    final int lookbackYears = 20;
    final Method method = Method.BaselineSWR;
    int partial_retirement_years = 20; // if positive, graph partial retirements of at least this duration

    SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real);

    // Initialize map of retirement duration to minimum observed SWR.
    Map<Integer, Integer> minSwrMap = new TreeMap<>();
    for (int i = 1; i <= retirementYears; ++i) {
      minSwrMap.put(i, 10000); // initialize with the largest possible value (10,000 = 100%)
    }

    final int iMinStart = (method == Method.BaselineSWR ? 0 : lookbackYears * 12);

    Sequence seqTrueSWR = new Sequence(method + " (True)");
    Sequence seqEstimatedSWR = new Sequence(method + " (Point-in-Time)");
    Sequence seqCBSWR = new Sequence("CBSWR"); // for retirement windows that have ended
    Sequence seqCbswrPartial = new Sequence("CBSWR (upper-bound)"); // for partial (incomplete / ongoing) retirements
    for (int iEnd = 0; iEnd < SwrLib.length(); ++iEnd) { // loop over all possible retirement end dates
      final long endTime = SwrLib.time(iEnd);

      // Add data to CBSWR (and partial) sequences.
      BengenEntry bengen = BengenTable.get(endTime, retirementYears, percentStock);
      if (bengen != null) {
        seqCBSWR.addData(bengen.swr / 100.0, endTime);
      } else if (partial_retirement_years > 0) {
        // No entry => retirement period has not finished. We can allow partial retirements.
        final int maxYears = (int) Math.floor((SwrLib.length() - iEnd) / 12.0);
        if (maxYears >= partial_retirement_years) {
          if (seqCbswrPartial.isEmpty()) {
            seqCbswrPartial.addData(seqCBSWR.getLast());
          }
          bengen = BengenTable.get(endTime, maxYears, percentStock);
          seqCbswrPartial.addData(bengen.swr / 100.0, endTime);
        }
      }

      // Must iterate over [1..N] years to avoid inversions where a WR seems to work for N years but fails for shorter
      // durations because the failure occurs for a start date less than N years before today.
      for (int years = 1; years <= retirementYears; ++years) {
        int iStart = iEnd - years * 12; // analyze retirement from iStart -> iEnd
        if (iStart < iMinStart) continue; // not enough historical data
        final long startTime = SwrLib.time(iStart);

        int swr = 0;
        if (method == Method.BaselineSWR) {
          swr = BengenTable.get(startTime, years, percentStock).swr;
        } else {
          swr = MarwoodTable.get(startTime, years, lookbackYears, percentStock).swr;
        }
        assert swr > 0; // && swr <= 10000; // TODO swr can be > 100% on annualized basis.

        // Ensure SWR is never higher than SWR for shorter retirement.
        if (years > 1) {
          swr = Math.min(swr, minSwrMap.get(years - 1));
        }

        if (swr < minSwrMap.get(years)) { // implies current WR is smaller than all historical rates
          if (years == retirementYears) {
            System.out.printf("[%s -> %d] %d -> %d\n", TimeLib.formatMonth(startTime),
                TimeLib.ms2date(endTime).getYear(), minSwrMap.get(years), swr);
          }
          minSwrMap.put(years, swr);

          // Forward update to ensure no duration inversions.
          for (int longerYears = years + 1; longerYears <= retirementYears; ++longerYears) {
            if (swr < minSwrMap.get(longerYears)) {
              // if (longerYears == retirementYears) {
              // System.out.printf("[%s -> %d] %d -> %d |update from %d years\n", TimeLib.formatMonth(startTime),
              // TimeLib.ms2date(endTime).getYear(), minSwrMap.get(longerYears), swr, years);
              // }
              minSwrMap.put(longerYears, swr);
            }
          }
        }
      }

      final double swr = minSwrMap.get(retirementYears) / 100.0; // SWR using data through `endTime`
      final int iStart = iEnd - retirementYears * 12;
      if (iStart >= 0) {
        final long startTime = SwrLib.time(iStart);
        seqTrueSWR.addData(swr, startTime); // "true" because data through `endTime` is not available at `startTime`
      }
      seqEstimatedSWR.addData(swr, endTime); // "estimate" because data is available at `endTime`
    }

    // Don't plot early years because there's very little data to support them.
    final long startGraphTime = TimeLib.toMs(1920, Month.JANUARY, 1);
    final long endGraphTime = TimeLib.toMs(2000, Month.MARCH, 1); // TODO 20 years before last data point
    seqTrueSWR = seqTrueSWR.subseq(startGraphTime, endGraphTime);
    seqEstimatedSWR = seqEstimatedSWR.subseq(startGraphTime, endGraphTime);
    seqCBSWR = seqCBSWR.subseq(startGraphTime, endGraphTime);
    seqCbswrPartial = seqCbswrPartial.subseq(startGraphTime, endGraphTime);

    // Collect failure rate stats.
    assert seqCBSWR.getStartMS() == seqEstimatedSWR.getStartMS();
    int nFail = 0;
    int nWin = 0;
    final int bpsAdjustment = 0; // set to a higher value to test reductions
    int maxGap = 0;
    for (int i = 0; i < seqCBSWR.length(); ++i) {
      int x = (int) Math.round(seqCBSWR.get(i, 0) * 100.0);
      int y = (int) Math.round(seqEstimatedSWR.get(i, 0) * 100.0);
      y -= bpsAdjustment; // calc stats using an adjusted SWR
      if (x < y) {
        ++nFail;
        final int gap = y - x;
        maxGap = Math.max(gap, maxGap);
        System.out.printf("[%s]: %d vs. %d = %d\n", TimeLib.formatMonth(seqCBSWR.getTimeMS(i)), x, y, gap);
      } else {
        ++nWin;
      }
    }
    int n = nWin + nFail;
    System.out.printf("fail=%d (%.3f%%)  win=%d (%.3f%%)  maxGap=%d\n", nFail, 100.0 * nFail / n, nWin,
        100.0 * nWin / n, maxGap);

    SwrLib.dumpSequences(new File(DataIO.getOutputPath(), "figure-8.txt"), seqEstimatedSWR, seqCBSWR, seqCbswrPartial);

    String filename = String.format("%s-swr-walk-forward.html", method == Method.DMSWR ? "dmswr" : "bengen");
    File file = new File(DataIO.getOutputPath(), filename);
    seqEstimatedSWR.setMeta("zIndex", 2);
    seqCBSWR.setMeta("zIndex", 1);
    seqCbswrPartial.setMeta("zIndex", 1);
    ChartConfig config = ChartConfig.build(file, ChartConfig.Type.Area, "SWR Walk-Forward Analysis", null, null, "100%",
        "800px", 3.0, 8.0, 0.25, ChartScaling.LINEAR, ChartTiming.MONTHLY, 0, seqEstimatedSWR, seqCBSWR,
        seqCbswrPartial);
    config.setColors(new String[] { "#434348", "#7cb5ec", "#7cb5ec" });
    config.setAxisLabelFontSize(28);
    config.setAxisTitleFontSize(28);
    config.setLineWidth(3);
    config.setFillOpacity(0.35);
    config.setTickFormatter(null, "return this.value + '%';");
    config.setTitleConfig("margin: 0, y: 50, style: { fontSize: 36 }");
    config.setTickInterval(24, 1);
    config.setTickFormatter("return this.value.split(' ')[1];", "return this.value + '%';");
    // config.setDataLabelConfig(true, -90, "#fff", 2, 1, 4, 20, false);
    config.setLegendConfig(
        "align: 'right', verticalAlign: 'top', x: -10, y: 90, layout: 'vertical', floating: true, itemStyle: {"
            + "fontSize: 24, }, backgroundColor: '#fff', borderWidth: 1, padding: 12, itemMarginTop: 8, "
            + "itemMarginBottom: 0, shadow: true, symbolWidth: 32,");
    config.setAnimation(false);
    Chart.saveChart(config);

    // TODO add support to chart config so that manual modification is not needed.
    // Need to manually add the following to the upper-bound sequence.
    // dashStyle: "ShortDash",
    // showInLegend: false,
  }
}
