package org.minnen.dmswr.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.MarwoodMethod;
import org.minnen.dmswr.NestEggCalculator;
import org.minnen.dmswr.utils.Library;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.utils.Writer;
import org.minnen.dmswr.utils.FinLib.Inflation;

public class MarwoodTable
{
  /** Values have valid SWR fields, queries ignore SWR field. */
  public static Map<MarwoodEntry, MarwoodEntry>      marwoodMap          = new HashMap<>();

  /** Sequences with all DMSWRs for a given retirement duration, lookback window, and stock percentage. */
  public static Map<MarwoodEntry, Sequence>          marwoodSequences    = new HashMap<>();

  /** Retirement trajectories for a given retirement date, duration, lookback window, and stock percentage. */
  public static Map<MarwoodEntry, List<MonthlyInfo>> marwoodTrajectories = new HashMap<>();

  /** SWR for the given retirement duration, lookback window, and stock percentage. */
  public static Map<MarwoodEntry, Integer>           marwoodSWRs         = new HashMap<>();

  public static MarwoodEntry get(long retireTime, int retirementYears, int lookbackYears, int percentStock)
  {
    MarwoodEntry key = new MarwoodEntry(retireTime, retirementYears, lookbackYears, percentStock);
    return marwoodMap.getOrDefault(key, null);
  }

  /** @return Sequence of DMSWR info vectors for initial retirement months. */
  public static Sequence getAcrossTime(int retirementYears, int lookbackYears, int percentStock)
  {
    MarwoodEntry key = new MarwoodEntry(retirementYears, lookbackYears, percentStock);
    return marwoodSequences.getOrDefault(key, null);
  }

  public static int getSWR(int retirementYears, int lookbackYears, int percentStock)
  {
    MarwoodEntry key = new MarwoodEntry(retirementYears, lookbackYears, percentStock);
    return marwoodSWRs.get(key);
  }

  /** @return Sequence of DMSWR info vectors for initial retirement months. */
  public static List<MonthlyInfo> getTrajectory(long retireTime, int retirementYears, int lookbackYears,
      int percentStock)
  {
    MarwoodEntry key = new MarwoodEntry(retireTime, retirementYears, lookbackYears, percentStock);
    return marwoodTrajectories.getOrDefault(key, null);
  }

  public static void clear()
  {
    marwoodMap.clear();
    marwoodSequences.clear();
    marwoodSWRs.clear();
    marwoodTrajectories.clear();
  }

  private static void generateTable(File file, int percentStock, int lookbackYears, boolean reretire) throws IOException
  {
    // TODO ok to have annualized withdrawal rates above 100%? Can be "sane" since withdrawals are monthly and growth
    // can be large enough to compensate on annual basis. Example: 1921-12 with 1 year retirement, 75/25 allocation.

    clear();
    NestEggCalculator nestEggCalculator = NestEggCalculator.constant(1e6);

    try (Writer writer = new Writer(file)) {
      writer.writeln("# DMSWR (safe withdrawal rates).");
      writer.writeln("# Withdrawal rates are annual, implying that the monthly withdrawal rate is SWR/12.0.");
      writer.writeln("# Fields:");
      writer.writeln("# 1) retirement duration in years");
      writer.writeln("# 2) lookback window in years");
      writer.writeln("# 3) percent stock");
      writer.writeln("# 4) retirement month");
      writer.writeln("# 5) current month");
      writer.writeln("# 6) DMSWR in basis points (500=5.0%)");
      writer.writeln("# 7) virtual retirement months - length of \"virtual retirement\" for best SWR");
      writer.writeln("# 8) final balance - balance at the end of retirement");
      writer.writeln("# 9) Bengen (MinSWR) income - annualized income using the Bengen SWR");
      writer.writeln("# 10) DMSWR income - annualized income using the DMSWR method");
      writer.writeln("# 11) CBSWR income - annualized income if we withdrew the maximum safe rate");
      for (int retirementYears = 1; retirementYears <= 40; ++retirementYears) {
        final long a = TimeLib.getTime();
        List<MonthlyInfo> marwoodList = MarwoodMethod.findDMSWR(retirementYears, lookbackYears, percentStock,
            nestEggCalculator);
        final long b = TimeLib.getTime();
        System.out.printf("%d  N=%d  (%d ms)\n", retirementYears, marwoodList.size(), b - a);

        // First add all results to the table since they're needed for re-retiring.
        for (MonthlyInfo info : marwoodList) {
          MarwoodEntry entry = new MarwoodEntry(retirementYears, lookbackYears, percentStock, info);
          assert entry.isRetirementStart();
          marwoodMap.put(entry, entry);
          writer.writeln(entry.toCSV());
        }

        // Now generate data for re-retiring.
        if (!reretire) continue;

        for (MonthlyInfo startInfo : marwoodList) {
          final double nestEgg = nestEggCalculator.getNestEgg(startInfo.index, lookbackYears, lookbackYears,
              percentStock);
          List<MonthlyInfo> trajectory = MarwoodMethod.reretire(startInfo.currentTime, retirementYears, lookbackYears,
              percentStock, nestEgg);

          MarwoodEntry newEntry = new MarwoodEntry(retirementYears, lookbackYears, percentStock, trajectory.get(0));
          MarwoodEntry oldEntry = MarwoodTable.marwoodMap.get(newEntry);
          assert newEntry.equals(oldEntry); // only tests that the key fields match
          assert newEntry.isRetirementStart();
          assert newEntry.swr == oldEntry.swr || (newEntry.swr == 2000 && oldEntry.swr > 2000); // 20% cap
          assert Library.almostEqual(newEntry.bengenSalary, oldEntry.bengenSalary, 1e-5);
          assert Library.almostEqual(newEntry.crystalSalary, oldEntry.crystalSalary, 1e-5);
          assert Library.almostEqual(newEntry.marwoodSalary, oldEntry.marwoodSalary, 1e-5);

          for (MonthlyInfo info : trajectory) {
            MarwoodEntry entry = new MarwoodEntry(retirementYears, lookbackYears, percentStock, info);
            if (marwoodMap.containsKey(entry)) {
              assert entry.isRetirementStart(); // data for retirement start dates are already in the table
            } else {
              // This entry is for a re-retire trajectory so add it to the table.
              assert entry.currentTime > entry.retireTime;
              marwoodMap.put(entry, entry);
              writer.writeln(entry.toCSV());
            }
          }

        }
      }
    }
  }

  public static void loadTable(File file) throws IOException
  {
    clear();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      MarwoodEntry marwoodKey = null;
      Sequence seq = null;
      while (true) {
        String line = reader.readLine();
        if (line == null) break;

        // Remove comments.
        int i = line.indexOf("#");
        if (i >= 0) {
          line = line.substring(0, i);
        }

        // Remove whitespace and skip empty lines.
        line = line.trim();
        if (line.isEmpty()) continue;

        MarwoodEntry info = MarwoodEntry.fromCSV(line);
        assert SwrLib.time(SwrLib.indexForTime(info.retireTime)) == info.retireTime;

        marwoodMap.put(info, info);

        if (info.isRetirementStart()) {
          // Create new sequence when retirement scenario changes.
          if (marwoodKey == null || info.retirementYears != marwoodKey.retirementYears
              || info.lookbackYears != marwoodKey.lookbackYears || info.percentStock != marwoodKey.percentStock) {
            if (seq != null) { // store previous sequence and SWR
              marwoodSequences.put(marwoodKey, seq);
              final int swr = (int) Math.round(seq.getMin().get(0));
              marwoodSWRs.put(marwoodKey, swr);
            }

            marwoodKey = new MarwoodEntry(info.retirementYears, info.lookbackYears, info.percentStock);
            seq = new Sequence(
                String.format("Marwood (%d, %d, %d)", info.retirementYears, info.lookbackYears, info.percentStock));
          }

          // Add new month to sequence.
          assert seq.isEmpty() || info.retireTime > seq.getEndMS();
          seq.addData(info.swr, info.retireTime);
        }
      }
      if (seq != null) { // store last sequence and final SWR
        marwoodSequences.put(marwoodKey, seq);
        final int swr = (int) Math.round(seq.getMin().get(0));
        marwoodSWRs.put(marwoodKey, swr);
      }
    }
  }

  /** Simulate re-retiring to boost withdrawals after the original retirement date. */
  public static void genReRetireTable(int retirementYears, int lookbackYears, int percentStock) throws IOException
  {
    final double nestEgg = 1e6;
    final int lookbackMonths = lookbackYears * 12;
    for (int i = lookbackMonths; i < SwrLib.lastIndex(retirementYears); ++i) {
      final long retireTime = SwrLib.time(i);
      List<MonthlyInfo> trajectory = MarwoodMethod.reretire(retireTime, retirementYears, lookbackYears, percentStock,
          nestEgg);

      MarwoodEntry key = new MarwoodEntry(retireTime, retirementYears, lookbackYears, percentStock);
      MarwoodTable.marwoodTrajectories.put(key, trajectory);
    }
  }

  private static void verifyTable() throws IOException
  {
    for (MarwoodEntry marwood : marwoodMap.values()) {
      if (marwood.isRetirementStart()) {
        MonthlyInfo info = BengenMethod.run(marwood);
        assert info.ok();
      }
    }
    System.out.printf("Verified entries: %d\n", marwoodMap.size());
  }

  public static void main(String[] args) throws IOException
  {
    final String mode = "generate"; // generate or verify
    final int lookbackYears = 20;
    final boolean reretire = false;
    final int[] percentStockList = new int[] { 0, 10, 20, 25, 30, 40, 50, 60, 70, 75, 80, 90, 100 };
    // final int[] percentStockList = new int[] { 75 };

    if (mode.equals("generate")) {
      SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real); // only load bengen table
      for (int percentStock : percentStockList) {
        System.out.printf("Percent Stock: %d\n", percentStock);

        final String filename = String.format("dmswr-stock%d-lookback%d.csv", percentStock, lookbackYears);
        final File file = new File(DataIO.getFinancePath(), filename);

        generateTable(file, percentStock, lookbackYears, reretire);
      }
    } else {
      for (int percentStock : percentStockList) {
        final String filename = String.format("dmswr-stock%d-lookback%d.csv", percentStock, lookbackYears);
        final File file = new File(DataIO.getFinancePath(), filename);

        SwrLib.setup(SwrLib.getDefaultBengenFile(), file, Inflation.Real);
        System.out.printf("DMSWR entries: %d\n", marwoodMap.size());
        System.out.printf("DMSWR sequences: %d\n", marwoodSequences.size());
        verifyTable();
      }
    }
  }
}
