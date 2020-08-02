package org.minnen.dmswr.utils;

import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.data.BengenTable;
import org.minnen.dmswr.data.MarwoodTable;
import org.minnen.dmswr.utils.FinLib.Inflation;
import org.minnen.dmswr.viz.Chart;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class SwrLib
{
  /** Stock, bonds, and CPI (inflation data). */
  public static Sequence                stock, bonds;
  private static Sequence               cpi;

  /** Each "Mul" sequence holds the multiplier representing growth for each month (1.01 = 1% growth). */
  private static Sequence               stockMul, bondsMul, cpiMul;

  private static Sequence               shiller;

  /** Mixed stock/bond cumulative returns keyed by stock percent (70 = 70% stocks / 30% bonds). */
  private static Map<Integer, Sequence> mixedMap;

  /** Was the data adjusted for inflation (Real) or not (Nominal)? */
  private static Inflation              inflationAdjustment;

  public static final int[]             percentStockList;

  static {
    percentStockList = new int[] { 0, 10, 20, 25, 30, 40, 50, 60, 70, 75, 80, 90, 100 };

    DataIO.setFinancePath("E:/research/finance/dmswr-data"); // set data directory for this project
  }

  public static Inflation getInflationAdjustment()
  {
    return inflationAdjustment;
  }

  /** @return timestamp (in ms) for the i'th data point. */
  public static long time(int i)
  {
    return stockMul.getTimeMS(i);
  }

  /** @return closest index for the given time */
  public static int indexForTime(long ms)
  {
    return stockMul.getClosestIndex(ms);
  }

  /** @return closest index for the given time */
  public static int indexForTime(Month month, int year)
  {
    return stockMul.getClosestIndex(TimeLib.toMs(year, month, 1));
  }

  /** @return number of elements in the underlying data. */
  public static int length()
  {
    return stockMul.length();
  }

  /** @return index of the last month for which we can simulate a `years` retirement. */
  public static int lastIndex(int retirementYears)
  {
    return length() - retirementYears * 12;
  }

  /** @return percent as basis points, e.g. 3.2% -> 320. */
  public static int percentToBasisPoints(double percent)
  {
    return (int) Math.floor(percent * 100 + 1e-5);
  }

  /** @return total growth for a stock/bond portfolio over [from..to]. */
  public static double growth(int from, int to, int percentStock)
  {
    if (from == to) return 1.0;
    assert percentStock >= 0 && percentStock <= 100;
    Sequence mixed = mixedMap.get(percentStock);
    return mixed.get(to, 0) / mixed.get(from, 0);
  }

  /**
   * Calculate the investment growth for the i'th month.
   * 
   * @param i index of month
   * @param percentStock percent invested in stock vs. bonds (70 => 70%)
   * @return growth for month `i` as a multiplier (4% => 1.04)
   */
  public static double growth(int i, int percentStock)
  {
    assert percentStock >= 0 && percentStock <= 100;
    if (percentStock == 100) {
      return stockMul.get(i, 0);
    } else if (percentStock == 0) {
      return bondsMul.get(i, 0);
    } else {
      final double alpha = percentStock / 100.0;
      final double beta = 1.0 - alpha;
      return stockMul.get(i, 0) * alpha + bondsMul.get(i, 0) * beta;
    }
  }

  /** @return inflation (as a multiplier) at `index` (i.e. from [index..index+1]). */
  public static double inflation(int index)
  {
    return cpi.get(index + 1, 0) / cpi.get(index, 0);
  }

  /** @return inflation (as a multiplier) over [from..to]. */
  public static double inflation(int from, int to)
  {
    if (from == to) return 1.0;
    return cpi.get(to, 0) / cpi.get(from, 0);
  }

  /** Verify that we're matching the "Real Total Return Price" from Shiller's spreadsheet. */
  private static Sequence calcSnpReturns(Inflation adjustForInflation)
  {
    double finalCPI = shiller.get(-1, Shiller.CPI);
    Sequence snp = new Sequence("Stock" + (adjustForInflation == Inflation.Real ? " (real)" : " (nominal)"));
    double shares = 1.0; // track number of shares to calculate total dividend payment
    for (int i = 0; i < shiller.size(); ++i) {
      final double cpi = shiller.get(i, Shiller.CPI);
      final double inflation = finalCPI / cpi;
      final double price = shiller.get(i, Shiller.PRICE);
      final double realPrice = price * inflation;
      final double dividend = shares * shiller.get(i, Shiller.DIV); // div data may be missing
      if (i > 0 && !Double.isNaN(dividend)) { // why not i==0? Shiller does it this way
        shares += dividend / price;
      }
      double balance = shares * price;
      if (adjustForInflation == Inflation.Real) balance *= inflation;
      // System.out.printf("%d [%s] $%.2f -> $%.2f (cpi=%.2f) $%.2f %.3f shares\n", i,
      // TimeLib.formatMonth(shiller.getTimeMS(i)), price, realPrice, cpi, balance, shares);
      snp.addData(balance, shiller.getTimeMS(i));
    }
    return snp;
  }

  public static Sequence adjustForInflation(Sequence seq, Sequence cpi)
  {
    assert seq.matches(cpi);
    final double finalCPI = cpi.getLast(0);
    Sequence seqAdjusted = new Sequence(seq.getName() + "(real)");
    for (int i = 0; i < seq.size(); ++i) {
      final double inflation = finalCPI / cpi.get(i, 0);
      seqAdjusted.addData(seq.get(i).mul(inflation));
    }
    assert seqAdjusted.matches(seq);
    return seqAdjusted;
  }

  public static File getDefaultBengenFile()
  {
    return new File(DataIO.getFinancePath(), "bengen-table.csv");
  }

  public static File getDefaultDmswrFile()
  {
    return new File(DataIO.getFinancePath(), "dmswr-stock75-lookback20.csv");
  }

  /** Load (inflation-adjusted) data and initialize / calculate static data sequences. */
  public static void setupWithDefaultFiles() throws IOException
  {
    setup(getDefaultBengenFile(), getDefaultDmswrFile(), Inflation.Real);
  }

  /** Load data and initialize / calculate static data sequences. */
  public static void setupWithDefaultFiles(Inflation inflation) throws IOException
  {
    setup(getDefaultBengenFile(), getDefaultDmswrFile(), inflation);
  }

  /** Load data and initialize / calculate static data sequences. */
  public static void setup(File bengenFile, File dmswrFile, Inflation inflation) throws IOException
  {
    // TODO If we download new data, Bengen and Marwood tables must be regenerated.
    // TODO last row in shiller data may be for a partial month and should be discarded.
    // Shiller.downloadData();
    shiller = Shiller.loadAll(Shiller.getPathCSV(), true);

    Sequence bondData = shiller.extractDimAsSeq(Shiller.GS10).setName("GS10");
    bonds = Bond.calcBondReturnsYTM(bondData);
    // bonds = Bond.calcReturnsRebuy(BondFactory.note10Year, bondData, 0, -1);
    // bonds = Bond.calcReturnsNaiveInterest(BondFactory.note10Year, bondData, 0, -1, DivOrPow.DivideBy12);
    // bonds = Bond.calcReturnsHold(BondFactory.note10Year, bondData, 0, -1);

    cpi = shiller.extractDimAsSeq(Shiller.CPI).setName("CPI");
    SwrLib.inflationAdjustment = inflation;
    if (inflation == Inflation.Real) {
      stock = shiller.extractDimAsSeq(Shiller.RTRP).setName("Stock (real)");
      bonds = adjustForInflation(bonds, cpi).setName("Bonds (real)");
    } else {
      stock = calcSnpReturns(Inflation.Nominal).setName("Stock (nominal)");
      bonds.setName("Bonds (nominal)");
    }

    System.out.println(stock);
    assert bonds.matches(stock);
    assert cpi.matches(stock);

    cpi._div(cpi.getFirst(0));
    stock._div(stock.getFirst(0));
    bonds._div(bonds.getFirst(0));

    stockMul = stock.derivativeMul();
    bondsMul = bonds.derivativeMul();
    cpiMul = cpi.derivativeMul();
    assert bondsMul.matches(stockMul);
    assert cpiMul.matches(stockMul);

    mixedMap = new HashMap<>();
    for (int percentStock = 0; percentStock <= 100; percentStock += 5) {
      // Note that stock*alpha + bonds*(1-alpha) models an initial split *without* rebalancing. We want to include
      // rebalancing (monthly, for simplicity) so the cumulative returns must be calculated month-by-month.
      Sequence mixed = new Sequence(String.format("Mixed (%d / %d)", percentStock, 100 - percentStock));
      double x = 1.0;
      for (int i = 0;; ++i) {
        mixed.addData(x, stock.getTimeMS(i));
        if (i >= length()) break; // can't compute growth because there's no more data
        x *= growth(i, percentStock);
      }
      mixedMap.put(percentStock, mixed);
      assert mixed.matches(stock);
    }

    // Load pre-computed bengen results.
    if (bengenFile != null) {
      System.out.printf("Load Bengen Data: [%s]\n", bengenFile);
      BengenTable.loadTable(bengenFile);
    }

    // Load pre-computed DMSWR results.
    if (dmswrFile != null) {
      System.out.printf("Load DMSWR Data: [%s]\n", dmswrFile);
      MarwoodTable.loadTable(dmswrFile);
    }
  }

  /** Save an interactive chart with stock and bond data as a local HTML file. */
  public static void saveGraph() throws IOException
  {
    // Save graph of asset growth and related curves (stock, bonds, mixed, CPI, etc.).
    Sequence snpReal = calcSnpReturns(Inflation.Real);
    snpReal.setName("S&P (real, calculated)");
    snpReal._div(snpReal.getFirst(0));

    Sequence snpNominal = calcSnpReturns(Inflation.Nominal);
    snpNominal.setName("S&P (nominal, calculated)");
    snpNominal._div(snpNominal.getFirst(0));

    Chart.saveLineChart(new File(DataIO.getOutputPath(), "shiller.html"), "Shiller Data", "100%", "800px",
        ChartScaling.LOGARITHMIC, ChartTiming.MONTHLY, snpReal, snpNominal, stock, bonds, mixedMap.get(70), cpi);
  }

  public static void main(String[] args) throws IOException
  {
    setupWithDefaultFiles();
    saveGraph();

    // Calculate and print the success rate for different WRs and stock/bond mixes.
    final int retirementYears = 30;
    for (int withdrawalRate = 300; withdrawalRate <= 400; withdrawalRate += 25) {
      for (int percentStock = 0; percentStock <= 100; percentStock += 25) {
        IntPair winFail = BengenMethod.getSuccessFail(withdrawalRate, retirementYears, percentStock);
        final int nWin = winFail.first;
        final int nFail = winFail.second;
        System.out.printf("%.2f%%  %3d%%| %6.2f%%   Failed: %3d / %d\n", withdrawalRate / 100.0, percentStock,
            100.0 * nWin / (nWin + nFail), nFail, nWin + nFail);
      }
    }
  }

  /**
   * Dump CSV data to help create an excel spreadsheet for JFP.
   */
  public static void dumpSequences(File file, List<Sequence> seqs) throws IOException
  {
    dumpSequences(file, seqs.toArray(new Sequence[seqs.size()]));
  }

  /**
   * Dump CSV data to help create an excel spreadsheet for JFP.
   */
  public static void dumpSequences(File file, Sequence... seqs) throws IOException
  {
    // Find longest sequence.
    int length = 0;
    Sequence longest = null;
    for (Sequence seq : seqs) {
      if (seq.length() > length) {
        length = seq.length();
        longest = seq;
      }
    }

    try (Writer writer = new Writer(file)) {
      for (int i = 0; i < length; ++i) {
        long ms = longest.getTimeMS(i);
        writer.writef("%s,%s", TimeLib.formatYM(ms), TimeLib.ms2date(ms).format(TimeLib.dtfY));
        for (Sequence seq : seqs) {
          writer.write(",");
          if (i < seq.length()) {
            writer.writef("%f", seq.get(i, 0));
          }
        }
        writer.writeln();
      }
    }
  }
}
