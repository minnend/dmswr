package org.minnen.dmswr.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.SwrLib;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.utils.Writer;
import org.minnen.dmswr.utils.FinLib.Inflation;

public class BengenTable
{
  /** Holds SWR for a specific date; values have valid SWR fields, queries ignore SWR field. */
  public static Map<BengenEntry, BengenEntry> bengenMap       = new HashMap<>();

  /** Values hold sequences with all Bengen SWRs for a given retirement duration and stock percentage. */
  public static Map<BengenEntry, Sequence>    bengenSequences = new HashMap<>();

  /** Values are BaselineSWR for the given retirement duration and stock percentage (342 => 3.42%). */
  public static Map<BengenEntry, Integer>     bengenSWRs      = new HashMap<>();

  public static BengenEntry get(long time, int retirementYears, int percentStock)
  {
    BengenEntry key = new BengenEntry(time, retirementYears, percentStock);
    return bengenMap.getOrDefault(key, null);
  }

  /** @return Sequence holding CBSWR as basis points for each starting retirement (423 = 4.23%). */
  public static Sequence getAcrossTime(int retirementYears, int percentStock)
  {
    BengenEntry key = new BengenEntry(retirementYears, percentStock);
    return bengenSequences.getOrDefault(key, null);
  }

  public static int getSWR(int retirementYears, int percentStock)
  {
    BengenEntry key = new BengenEntry(retirementYears, percentStock);
    return bengenSWRs.getOrDefault(key, -1);
  }

  public static void clear()
  {
    bengenMap.clear();
    bengenSequences.clear();
    bengenSWRs.clear();
  }

  /**
   * Generate a file containing Bengen SWR result.
   * 
   * Each line in the CSV file has the form: retirement_years, percent_stock, yyyy-mm, swr. The SWR is an integer
   * representing basis points, i.e. 500 = 5.0%. This function fills in the `bengenMap` table, but not `bengenSWRs` or
   * `bengenSequences`.
   * 
   * @param file write results to this file.
   * @throws IOException
   */
  private static void generateTable(File file) throws IOException
  {
    clear();

    try (Writer writer = new Writer(file)) {
      writer.writeln("# Bengen safe withdrawal rates (SWR).");
      writer.writeln("# Withdrawal rates are annual, implying that the monthly withdrawal rate is SWR/12.0.");
      writer.writeln("# Fields:");
      writer.writeln("# 1) retirement duration in years");
      writer.writeln("# 2) percent stock");
      writer.writeln("# 3) retirement month");
      writer.writeln("# 4) safe withdrawal rate in basis points (500=5.0%)");

      for (int retirementYears = 1; retirementYears <= 60; ++retirementYears) {
        final long a = TimeLib.getTime();
        for (int percentStock : SwrLib.percentStockList) {
          // TODO include partial windows? perhaps add a field marking them as partial?
          Sequence seq = BengenMethod.calcSwrAcrossTime(retirementYears, percentStock, false);
          int minSWR = Integer.MAX_VALUE;
          for (FeatureVec v : seq) {
            final int swr = (int) Math.round(v.get(0));
            minSWR = Math.min(minSWR, swr);
            BengenEntry bengen = new BengenEntry(v.getTime(), retirementYears, percentStock, swr);
            bengenMap.put(bengen, bengen);
            writer.writeln(bengen.toCSV());
          }
          System.out.printf("%d, %3d [%s] -> %d\n", retirementYears, percentStock, TimeLib.formatYM(seq.getEndMS()),
              minSWR);
        }
        final long b = TimeLib.getTime();
        System.out.printf("%d years -> %d ms\n", retirementYears, b - a);
      }
    }
  }

  /**
   * Calculate MinSWR and protected against duration inversions.
   * 
   * @param key key for getting the sequences of SWRs
   * @return SWR that avoids duration inversions.
   */
  private static int storeSafeMinSWR(BengenEntry key)
  {
    Sequence seq = bengenSequences.get(key);
    int swr = (int) Math.round(seq.getMin().get(0));

    // Avoid inversions where MinSWR(N years) > MinSWR(N-1 years).
    if (key.retirementYears > 1) {
      final int shorterSWR = BengenTable.getSWR(key.retirementYears - 1, key.percentStock);
      assert shorterSWR > 0;
      swr = Math.min(swr, shorterSWR);
    }

    bengenSWRs.put(key, swr);
    return swr;
  }

  public static void loadTable(File file) throws IOException
  {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      BengenEntry key = null;
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

        BengenEntry info = BengenEntry.fromCSV(line);
        assert SwrLib.time(SwrLib.indexForTime(info.time)) == info.time;

        bengenMap.put(info, info);

        // Create new sequence when retirement scenario changes.
        if (key == null || info.retirementYears != key.retirementYears || info.percentStock != key.percentStock) {
          if (seq != null) { // store previous sequence and SWR
            assert key != null;
            bengenSequences.put(key, seq);
            storeSafeMinSWR(key);
          }

          // Create new key and start new sequence.
          key = new BengenEntry(info.retirementYears, info.percentStock);
          seq = new Sequence(String.format("Bengen (%d, %d)", info.retirementYears, info.percentStock));
        }

        // Add new month to sequence.
        assert seq.isEmpty() || info.time > seq.getEndMS();
        seq.addData(info.swr, info.time);
      }
      if (seq != null) { // store last sequence and final SWR
        assert key != null;
        bengenSequences.put(key, seq);
        storeSafeMinSWR(key);
      }
    }
  }

  private static void verifyTable() throws IOException
  {
    for (BengenEntry bengen : bengenMap.values()) {
      MonthlyInfo info = BengenMethod.run(bengen);
      assert info.ok() : bengen;
    }
    System.out.printf("Verified entries: %d\n", bengenMap.size());
  }

  public static void main(String[] args) throws IOException
  {
    final String mode = "generate";

    if (mode.equals("generate")) {
      SwrLib.setup(null, null, Inflation.Real); // don't load bengen or dmswr table
      File file = new File(DataIO.getFinancePath(), "bengen-table.csv");
      generateTable(file);
    } else {
      SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real); // only load bengen table
      System.out.printf("Bengen entries: %d\n", bengenMap.size());
      System.out.printf("Bengen sequences: %d\n", bengenSequences.size());
      verifyTable();
    }
  }
}
