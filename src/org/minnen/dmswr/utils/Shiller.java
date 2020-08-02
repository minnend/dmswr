package org.minnen.dmswr.utils;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.minnen.dmswr.utils.FinLib;
import org.minnen.dmswr.utils.FinLib.DividendMethod;
import org.minnen.dmswr.utils.Library;
import org.minnen.dmswr.utils.TimeLib;

/**
 * Data provided by Robert Shiller: http://www.econ.yale.edu/~shiller/data.htm
 * 
 * Unofficial info on interpreting the data: https://www.bogleheads.org/forum/viewtopic.php?t=137706
 * 
 * The APIs in the class work with a CSV file, which must be saved from the excel spreadsheet that Shiller provides.
 */
public class Shiller
{
  public static int                        PRICE         = 0;
  public static int                        DIV           = 1;
  public static int                        CPI           = 2;
  public static int                        GS10          = 3;
  public static int                        RTRP          = 4;
  public static int                        CAPE          = 5;

  public static final String               dataUrlString = "http://www.econ.yale.edu/~shiller/data/ie_data.xls";

  private static final Map<File, Sequence> cache         = new HashMap<File, Sequence>();

  public static File getPathCSV()
  {
    return new File(DataIO.getFinancePath(), "shiller.csv");
  }

  public static File getPathExcel()
  {
    return new File(DataIO.getFinancePath(), "shiller.xls");
  }

  /**
   * Load data from CSV export of Shiller's SNP/CPI excel spreadsheet.
   * 
   * @param file file to load
   * @return Sequence holding Shiller data
   * @throws IOException if there is a problem reading the file.
   */
  public static Sequence loadAll(File file) throws IOException
  {
    return loadAll(file, false);
  }

  /**
   * Load data from CSV export of Shiller's SNP/CPI excel spreadsheet.
   * 
   * @param file file to load
   * @param allowMissingData if True, missing dividend data will be set to NaN, else the last data point will correspond
   *          to the last month with complete data.
   * @return Sequence holding Shiller data
   * @throws IOException if there is a problem reading the file.
   */
  public static Sequence loadAll(File file, boolean allowMissingData) throws IOException
  {
    Sequence seq = cache.get(file);
    if (seq != null) return seq;

    if (!file.canRead()) {
      throw new IOException(String.format("Can't read Shiller file (%s)", file.getPath()));
    }
    System.out.printf("Loading Shiller data: [%s]\n", file.getPath());
    seq = new Sequence("Shiller Financial Data");
    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = in.readLine()) != null) {
        try {
          String[] toks = line.trim().split(",");
          if (toks == null || toks.length < 5) {
            continue; // want at least: date, p, d, e, cpi
          }

          // date - odd parsing because 2017.1 = October 2017.
          double date = Double.parseDouble(toks[0]);
          int year = (int) Math.floor(date);
          int month = (int) Math.round((date - year) * 100);

          // snp price -- average of closing prices for the month
          double price = Double.parseDouble(toks[1]);

          // snp dividend -- data is annual dollar value, we want monthly
          // note: dividend data is quarterly and linearly interpolated to get monthly data
          double div = Library.tryParse(toks[2], Double.NaN) / 12.0;
          if (!allowMissingData && Double.isNaN((div))) break;

          // cpi
          double cpi = Double.parseDouble(toks[4]);

          // GS10 rate
          double gs10 = Double.parseDouble(toks[6]);

          // real total return price
          double rtrp = Double.parseDouble(toks[9]);

          // CAPE
          double cape = Library.tryParse(toks[12], 0.0);

          long timeMS = TimeLib.toMs(year, month, 1);
          seq.addData(new FeatureVec(6, price, div, cpi, gs10, rtrp, cape), timeMS);

          // System.out.printf("%d/%d: $%.2f $%.2f $%.2f\n", year, month, price, div, cpi);
        } catch (NumberFormatException nfe) {
          // System.err.println("Bad Line: " + line);
          if (seq.isEmpty()) continue;
          else break;
        }
      }

      cache.put(file, seq);
      return seq;
    }
  }

  /**
   * Load S&P 500 price data based on Shiller's monthly data.
   * 
   * Each price is a monthly average. If dividends are included, older prices are adjusted to make the returns accurate
   * if dividends were re-invested at the end of each month.
   * 
   * @return Sequence with one dimension holding monthly S&P prices
   */
  public static Sequence loadSNP(File file, DividendMethod divMethod) throws IOException
  {
    Sequence snp = Shiller.loadAll(file);
    return FinLib.calcSnpReturns(snp, 0, -1, divMethod);
  }

  /** Download Shiller data in .xls format and convert to .csv. */
  public static boolean downloadData() throws IOException
  {
    File shillerExcel = getPathExcel();
    if (DataIO.shouldDownloadUpdate(shillerExcel)) {
      if (!DataIO.copyUrlToFile(dataUrlString, shillerExcel)) {
        throw new IOException("Failed to download Shiller Excel file.");
      }
    }

    FileInputStream file = new FileInputStream(shillerExcel);
    StringBuilder sb = new StringBuilder();
    try (Workbook workbook = new HSSFWorkbook(file)) {
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      Sheet sheet = workbook.getSheet("Data");
      String[] values = null;
      for (Row row : sheet) {
        int nColumns = row.getLastCellNum();
        if (values == null || values.length != nColumns) {
          values = new String[nColumns];
        }
        int n = 0;
        for (Cell cell : row) {
          cell = evaluator.evaluateInCell(cell);
          int cellIndex = cell.getColumnIndex();
          switch (cell.getCellTypeEnum()) {
          case NUMERIC:
            String s = String.format("%.8f", cell.getNumericCellValue());
            // Remove trailing zeros up to the hundredths digit.
            final int iDot = s.lastIndexOf('.');
            if (iDot > 0) {
              int iLastNonZero = s.length() - 1;
              while (s.charAt(iLastNonZero) == '0' && iLastNonZero > iDot + 2) {
                --iLastNonZero;
              }
              s = s.substring(0, iLastNonZero + 1);
            }
            values[cellIndex] = s;
            ++n;
            break;
          case STRING:
            values[cellIndex] = cell.getStringCellValue();
            break;
          default:
            values[cellIndex] = "";
            break;
          }
        }
        if (n > 2) {
          String line = String.join(",", values);
          sb.append(line);
          sb.append('\n');
        }
      }
    }

    try (FileWriter writer = new FileWriter(getPathCSV())) {
      writer.write(sb.toString());
    }

    return true;
  }

  public static void main(String[] args) throws IOException
  {
    downloadData();

    Sequence snpNoDivs = Shiller.loadSNP(getPathCSV(), DividendMethod.NO_REINVEST_MONTHLY);
    Sequence snpWithDivs = Shiller.loadSNP(getPathCSV(), DividendMethod.MONTHLY);

    System.out.printf("  No divs (%d): [%s] -> [%s]\n", snpNoDivs.length(), TimeLib.formatDate(snpNoDivs.getStartMS()),
        TimeLib.formatDate(snpNoDivs.getEndMS()));
    System.out.printf("With divs (%d): [%s] -> [%s]\n", snpWithDivs.length(),
        TimeLib.formatDate(snpWithDivs.getStartMS()), TimeLib.formatDate(snpWithDivs.getEndMS()));

    double total = FinLib.getTotalReturn(snpNoDivs);
    double annual = FinLib.getAnnualReturn(total, snpNoDivs.getLengthMonths());
    System.out.printf("Total Returns (w/o divs): %9.2f => %.3f%%\n", total, annual);

    total = FinLib.getTotalReturn(snpWithDivs);
    annual = FinLib.getAnnualReturn(total, snpWithDivs.getLengthMonths());
    System.out.printf("  Total Returns (w/divs): %9.2f => %.3f%%\n", total, annual);
  }
}
