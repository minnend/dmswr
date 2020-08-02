package org.minnen.dmswr.paper;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.minnen.dmswr.utils.DataIO;
import org.minnen.dmswr.utils.SwrLib;
import org.minnen.dmswr.BengenMethod;
import org.minnen.dmswr.utils.Writer;
import org.minnen.dmswr.viz.Colors;
import org.minnen.dmswr.utils.FinLib.Inflation;

public class BengenSuccessChart
{
  public static String[] buildConservativeRedToGreenColorMap()
  {
    // Build color table; colors adapted from: https://learnui.design/tools/data-color-picker.html#divergent and
    // https://earlyretirementnow.com/2016/12/07/the-ultimate-guide-to-safe-withdrawal-rates-part-1-intro/
    String[] colors = new String[101];
    for (int i = 0; i < colors.length; ++i) {
      final int red = Colors.interpolate(i, //
          0, 30, 50, 90, 100, //
          255, 250, 253, 241, 100);
      final int green = Colors.interpolate(i, //
          0, 30, 50, 80, 100, //
          90, 148, 183, 232, 191);
      final int blue = Colors.interpolate(i, //
          0, 30, 50, 80, 100, //
          80, 116, 122, 133, 124);
      assert red >= 0 && red <= 255 : red;
      assert green >= 0 && green <= 255 : green;
      assert blue >= 0 && blue <= 255 : blue;
      colors[i] = String.format("rgb(%d, %d, %d)", red, green, blue);
      // colors[i] = String.format("%d, %d, %d", red, green, blue);
    }
    return colors;
  }

  public static void createSuccessChart(File file, int[] retirementYearsList, int[] percentStockList,
      int[] withdrawalRateList) throws IOException
  {
    String[] colors = buildConservativeRedToGreenColorMap();
    // String[] colors = Colors.buildRedToGreenColorMap();

    StringWriter sw = new StringWriter(8192);
    try (Writer writer = new Writer(sw)) {
      writer.write("<html><head>\n");
      writer.write("<title>Bengen Success Chart</title>\n");
      writer.write("<link rel='stylesheet' href='css/success-chart.css'>\n");
      writer.write("</head>\n");

      writer.write("<body>\n");
      writer.write("<table class='success-chart'>\n");

      writer.write("<tr>");
      writer.write("<th class='thick-right' colspan=2 rowspan=2></th>");
      writer.writef("<th colspan=%d>Annual Withdrawal Rate</th>", withdrawalRateList.length);
      writer.write("</tr>\n");

      writer.write("<tr>");
      for (int withdrawalRate : withdrawalRateList) {
        writer.writef("<th>%.2f%%</th>", withdrawalRate / 100.0);
      }
      writer.write("</tr>\n");

      for (int iPercent = 0; iPercent < percentStockList.length; ++iPercent) {
        final int percentStock = percentStockList[iPercent];
        writer.writef("<tr class='thick-top'><th rowspan=%d>%d%%<br/>Stock</th>\n", retirementYearsList.length,
            percentStock);
        for (int iYears = 0; iYears < retirementYearsList.length; ++iYears) {
          if (iYears > 0) writer.write("<tr>");
          final int retirementYears = retirementYearsList[iYears];
          final int n = SwrLib.lastIndex(retirementYears) + 1;
          writer.writef("<th class='thick-right'>%d Years</th>", retirementYears);
          for (int withdrawalRate : withdrawalRateList) {
            final int nWin = BengenMethod.getSuccessFail(withdrawalRate, retirementYears, percentStock).first;
            int percent = (int) Math.round(nWin * 100.0 / n);
            if (percent == 100 && nWin < n) percent = 99; // don't round to 100%
            writer.writef("<td style='background-color: %s'>%d%%</td>", colors[percent], percent);
          }
          writer.write("</tr>\n");
        }
      }

      writer.write("</table>\n");
      writer.write("</body></html>\n");
    }

    // Dump string to file.
    try (Writer writer = new Writer(file)) {
      writer.write(sw.toString());
    }
  }

  public static void printColors()
  {
    String[] colors = buildConservativeRedToGreenColorMap();
    for (int i = 0; i < colors.length; ++i) {
      System.out.printf("%d, %s\n", i, colors[i]);
    }
  }

  public static void main(String[] args) throws IOException
  {
    // printColors();

    SwrLib.setup(SwrLib.getDefaultBengenFile(), null, Inflation.Real); // DMSWR data not needed

    File file = new File(DataIO.getOutputPath(), "bengen-success-chart.html");
    int[] durations = new int[] { 30, 40, 50 };
    int[] percentStockList = new int[] { 100, 75, 50, 25, 0 };
    int[] withdrawalRates = new int[] { 300, 325, 350, 375, 400, 425, 450, 475, 500 };
    createSuccessChart(file, durations, percentStockList, withdrawalRates);
  }
}
