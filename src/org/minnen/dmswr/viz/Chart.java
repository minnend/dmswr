package org.minnen.dmswr.viz;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.FinLib;
import org.minnen.dmswr.utils.TimeLib;
import org.minnen.dmswr.utils.Writer;
import org.minnen.dmswr.viz.ChartConfig.ChartScaling;
import org.minnen.dmswr.viz.ChartConfig.ChartTiming;

public class Chart
{
  public final static String  jquery         = "https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js";
  public final static boolean bVerticalLine  = false;
  public final static Pattern patternNegZero = Pattern.compile("-0?\\.?0*");

  public static ChartConfig saveLineChart(File file, String title, String width, String height, ChartScaling scaling,
      ChartTiming timing, Sequence... seqs) throws IOException
  {
    ChartConfig config = ChartConfig.buildLine(file, title, width, height, scaling, timing, seqs);
    saveChart(config);
    return config;
  }

  public static ChartConfig saveLineChart(File file, String title, String width, String height, ChartScaling scaling,
      ChartTiming timing, List<Sequence> seqs) throws IOException
  {
    ChartConfig config = ChartConfig.buildLine(file, title, width, height, scaling, timing, seqs);
    saveChart(config);
    return config;
  }

  public static ChartConfig saveChart(File file, ChartConfig.Type chartType, String title, String[] labels,
      String[] colors, String width, String height, double ymin, double ymax, double minorTickIntervalY,
      ChartScaling scaling, ChartTiming timing, int dim, Sequence... seqs) throws IOException
  {
    ChartConfig config = ChartConfig.build(file, chartType, title, labels, colors, width, height, ymin, ymax,
        minorTickIntervalY, scaling, timing, dim, seqs);
    saveChart(config);
    return config;
  }

  /** @return string representation of `ms` according to `timing`. */
  private static String formatTime(int index, long ms, ChartTiming timing)
  {
    if (timing == ChartTiming.DAILY) {
      return TimeLib.formatDate(ms);
    } else if (timing == ChartTiming.MONTHLY) {
      return TimeLib.formatMonth(ms);
    } else if (timing == ChartTiming.ANNUAL) {
      return String.format("%d", TimeLib.ms2date(ms).getYear());
    } else if (timing == ChartTiming.INDEX) {
      return String.format("%d", index);
    } else {
      throw new NotImplementedException("Unknown chart timing: " + timing);
    }
  }

  /** Adds text for each plot band to the given writer. */
  private static void addPlotBands(List<PlotBand> bands, Writer writer) throws IOException
  {
    if (bands == null || bands.isEmpty()) return;
    writer.write("   plotBands: [\n");
    for (PlotBand band : bands) {
      writer.writef("    { from: %g, to: %g, color: '%s' },\n", band.from, band.to, band.color);
    }
    writer.write("   ],\n");
  }

  /** Adds text for each plot line to the given writer. */
  private static void addPlotLines(List<PlotLine> lines, Writer writer) throws IOException
  {
    if (lines == null || lines.isEmpty()) return;
    writer.write("   plotLines: [\n");
    for (PlotLine line : lines) {
      writer.writef("    { value: %g, width: %g, color: '%s', dashStyle: '%s', },\n", line.value, line.width,
          line.color, line.dashStyle);
    }
    writer.write("   ],\n");
  }

  public static void saveChart(ChartConfig config) throws IOException
  {
    Sequence[] seqs = config.data;

    // saveDataCSV(config, seqs);
    try (Writer writer = new Writer(config.file)) {
      writer.write("<html><head>\n");
      writer.writef("<script src=\"%s\"></script>\n", jquery);
      writer.writef("<script src=\"%s/js/highcharts.js\"></script>\n",
          config.pathToBase == null ? "." : config.pathToBase);
      writer.write("<script type=\"text/javascript\">\n");

      writer.write("$(function () {\n");
      writer.writef(" $('#%s').highcharts({\n", config.containerName);
      writer.writef("  title: { text: %s, %s },\n", config.title == null ? "null" : "'" + config.title + "'",
          config.titleConfig == null ? "" : config.titleConfig);
      if (config.legendConfig != null && !config.legendConfig.isEmpty()) {
        writer.write("  legend: { " + config.legendConfig + " },\n");
      }
      if (bVerticalLine) {
        writer.write("  tooltip: {\n");
        writer.write("    crosshairs: {\n");
        writer.write("      color: 'green',\n");
        writer.write("      dashStyle: 'solid'\n");
        writer.write("    },\n");
        writer.write("    shared: true,\n");
        writer.write("    enabled: false\n");
        writer.write("  },\n");
      }
      if (config.type != ChartConfig.Type.Line) {
        writer.writef("  chart: { type: '%s' },\n", ChartConfig.chart2name(config.type));
      }
      writer.write("  plotOptions: {\n");
      if (config.type == ChartConfig.Type.Bar) {
        writer.write("   column: {\n");
        if (config.colors != null) {
          writer.write("    colorByPoint: true,\n");
        }
        writer.write("    pointPadding: 0,\n");
        writer.write("    groupPadding: 0.1,\n");
        writer.write("    borderWidth: 0\n");
        writer.write("   },\n");
      } else if (config.type == ChartConfig.Type.Area || config.type == ChartConfig.Type.PosNegArea) {
        if (seqs.length == 1) {
          writer.write("   area: {\n");
          writer.write("    fillColor: {\n");
          writer.write("      linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },\n");
          writer.write("      stops: [\n");
          if (config.type == ChartConfig.Type.Area) {
            writer.write("        [0, Highcharts.getOptions().colors[0]],\n");
            writer
                .write("        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]\n");
          } else {
            assert config.type == ChartConfig.Type.PosNegArea;
            FeatureVec vmin = seqs[0].getMin();
            FeatureVec vmax = seqs[0].getMax();
            for (int i = 1; i < seqs.length; ++i) {
              vmin._min(seqs[i].getMin());
              vmax._max(seqs[i].getMax());
            }
            double vzero = vmax.get(0) / (vmax.get(0) - vmin.get(0));

            writer.write("        [0, 'rgb(0,255,0)'],\n");
            writer.writef("        [%f, 'rgba(0,255,0,0.5)'],\n", vzero);
            writer.writef("        [%f, 'rgba(255,0,0,0.5)'],\n", vzero + 0.01);
            writer.write("        [1, 'rgb(255,0,0)'],\n");
          }
          writer.write("      ]},\n");
          writer.write("    marker: { radius: 2 },\n");
          writer.write("    lineWidth: 1,\n");
          writer.write("    states: {\n");
          writer.write("      hover: { lineWidth: 1 }\n");
          writer.write("    },\n");
          // writer.write(" threshold: null\n");
          writer.write("   },\n");
        }
      } else if (config.type == ChartConfig.Type.Line) {
        writer.write("    line: { marker: { enabled: false },\n");
        writer.writef("            lineWidth: %d, },\n", config.lineWidth);
      }
      writer.writeln("    series: {");
      writer.writef("      animation: %s,\n", config.animation ? "true" : "false");
      writer.writef("      fillOpacity: %.2f,\n", config.fillOpacity);
      writer.writef("      shadow: %s,\n", config.shadow ? "true" : "false");
      writer.writeln("    },");
      writer.writeln("  },"); // end plotOptions

      writer.write("  xAxis: {\n");

      if (config.xTickInterval >= 0) {
        writer.writef("   tickInterval: %d,\n", config.xTickInterval);
      }

      if (config.xAxisTitle != null) {
        writer.writeln("   title: {");
        writer.writef("      text: '%s',\n", config.xAxisTitle);
        writer.writef("      style: { fontSize: %d, },\n", config.axisLabelFontSize);
        writer.writeln("   },");
      }

      writer.write("   labels: {\n");
      writer.write("    style: {\n");
      writer.writef("      fontSize: %d\n", config.axisLabelFontSize);
      writer.write("    },\n");
      writer.writef("    y: %d,\n", config.axisLabelFontSize + 2);
      if (config.xTickFormatter != null) {
        writer.write("    formatter: function () {\n");
        writer.writef("      %s\n", config.xTickFormatter);
        writer.write("    },\n");
      }
      writer.write("   },\n"); // end labels

      if (config.labels != null || config.timing != ChartTiming.INDEX) {
        writer.write("   categories: [");
        if (config.labels != null) {
          assert config.labels.length == seqs[0].size();
          for (int i = 0; i < config.labels.length; ++i) {
            writer.write("'" + config.labels[i] + "'");
            if (i < config.labels.length - 1) {
              writer.write(",");
            }
          }
        } else {
          for (int i = 0; i < seqs[0].size(); ++i) {
            writer.writef("'%s'", formatTime(i, seqs[0].getTimeMS(i), config.timing));
            if (i < seqs[0].size() - 1) {
              writer.write(",");
            }
          }
        }
        writer.write("],\n"); // end categories
      }

      addPlotBands(config.xBands, writer);
      addPlotLines(config.xLines, writer);
      writer.write("  },\n"); // xAxis

      writer.write("  yAxis: {\n");
      writer.writef("   endOnTick: %s,\n", config.endOnTick ? "true" : "false");
      if (config.yTickInterval >= 0) {
        writer.writef("   tickInterval: %d,\n", config.yTickInterval);
      }

      writer.write("   labels: {\n");
      writer.write("    style: {\n");
      writer.writef("      fontSize: %d\n", config.axisLabelFontSize);
      writer.write("    },\n");
      if (config.yTickFormatter != null) {
        writer.write("    formatter: function () {\n");
        writer.writef("      %s\n", config.yTickFormatter);
        writer.write("    },\n");
      }
      writer.write("   },\n");

      if (config.logarthimicYAxis) {
        writer.write("   type: 'logarithmic',\n");
      }
      if (!Double.isNaN(config.minorTickIntervalY)) {
        writer.writef("   minorTickInterval: %.3f,\n", config.minorTickIntervalY);
      }
      if (!Double.isNaN(config.ymin)) {
        writer.writef("   min: %.3f,\n", config.ymin);
      }
      if (!Double.isNaN(config.ymax)) {
        writer.writef("   max: %.3f,\n", config.ymax);
      }
      writer.write("   title: { text: null },\n");
      addPlotBands(config.yBands, writer);
      addPlotLines(config.yLines, writer);
      writer.write("  },\n"); // yAxis

      if (config.type == ChartConfig.Type.Line) {
        writer.write("  chart: {\n");
        if (config.title == null) {
          writer.writeln("   marginTop: 20,"); // need extra space at top since no title to push graph down
        }
        writer.write("   zoomType: 'xy',\n");
        writer.write("  },\n");
      }

      if (config.colors != null) {
        writer.write("  colors: [");
        for (int i = 0; i < config.colors.length; ++i) {
          writer.writef("'%s'", config.colors[i]);
          if (i < config.colors.length - 1) {
            writer.write(',');
          }
        }
        writer.write("],\n");
      }

      writer.write("  series: [\n");
      for (int i = 0; i < seqs.length; ++i) {
        Sequence seq = seqs[i];
        writer.writef("  { name: '%s',\n", seq.getName());

        if (seq.hasMeta("zIndex")) {
          int zIndex = (int) seq.getMeta("zIndex");
          writer.writef("    zIndex: %d,\n", zIndex);
        }

        if (config.dataLabelConfig != null) {
          writer.writeln(config.dataLabelConfig);
        }
        writer.write("    data: [");
        if (seq.length() == seqs[0].length() || config.labels != null) {
          // Only write `y` values.
          for (int t = 0; t < seq.length(); ++t) {
            double y = seq.get(t, config.iDim);
            writer.writef("%.6f%s", y, t == seq.size() - 1 ? "" : ", ");
          }
        } else {
          // Write `(x, y)` values.
          for (int t = 0; t < seq.length(); ++t) {
            int index = t;
            if (config.timing != ChartTiming.INDEX) {
              index = seqs[0].getClosestIndex(seq.getTimeMS(t));
            }
            double y = seq.get(t, config.iDim);
            writer.writef("[%d, %.6f]%s", index, y, t == seq.size() - 1 ? "" : ", ");
          }
        }
        writer.write("],");
        int lineWidth = (Integer) seq.getMeta("lineWidth", 0);
        if (lineWidth > 0) {
          writer.writef(" lineWidth: %d,\n", lineWidth);
        }
        writer.write(" },\n");
      }
      writer.write("  ]\n");
      writer.write(" });\n");
      writer.write("});\n");

      writer.write("</script></head><body>\n");
      writer.writef("<div id=\"%s\" style=\"width:%s; height:%s;\" />\n", config.containerName, config.width,
          config.height);
      writer.write("</body></html>\n");
    }
  }

  public static void saveScatter(File file, String title, String width, String height, int dim, Sequence returns1,
      Sequence returns2) throws IOException
  {
    assert returns1.length() == returns2.length();

    // Split return pairs into above / below y=x line.
    Sequence above = new Sequence();
    Sequence below = new Sequence();
    for (int i = 0; i < returns1.length(); ++i) {
      double x = returns1.get(i, dim);
      double y = returns2.get(i, dim);
      FeatureVec v = new FeatureVec(2, x, y);
      if (x >= y) {
        below.addData(v);
      } else {
        above.addData(v);
      }
    }

    // Find min/max values for drawing a y=x line.
    double vmin = Math.max(returns1.getMin().get(dim), returns2.getMin().get(dim));
    double vmax = Math.min(returns1.getMax().get(dim), returns2.getMax().get(dim));

    // Write HTML to generate the graph.
    try (Writer writer = new Writer(file)) {
      writer.write("<html><head>\n");
      writer.writef("<script src=\"%s\"></script>\n", jquery);
      writer.write("<script src=\"js/highcharts.js\"></script>\n");
      writer.write("  <script type=\"text/javascript\">\n");
      writer.write("$(function () {\n");
      writer.write(" $('#chart').highcharts({\n");
      writer.write("  title: { text: '" + title + "' },\n");
      writer.write("  chart: {\n");
      writer.write("   type: 'scatter',\n");
      writer.write("   zoomType: 'xy'\n");
      writer.write("  },\n");
      writer.write("  xAxis: {\n");
      writer.write("   title: {\n");
      writer.write("    text: '" + returns1.getName() + "',\n");
      writer.write("    style: {\n");
      writer.write("     fontSize: '18px',\n");
      writer.write("    }\n");
      writer.write("   }\n");
      writer.write("  },\n");
      writer.write("  yAxis: {\n");
      writer.write("   title: {\n");
      writer.write("    text: '" + returns2.getName() + "',\n");
      writer.write("    style: {\n");
      writer.write("     fontSize: '18px',\n");
      writer.write("    }\n");
      writer.write("   }\n");
      writer.write("  },\n");
      writer.write("  legend: { enabled: false },\n");
      writer.write("  plotOptions: {\n");
      writer.write("   scatter: {\n");
      writer.write("    marker: { radius: 3, symbol: 'circle' },\n");
      writer.write("    tooltip: {\n");
      writer.write("     headerFormat: '',\n");
      writer.write("     pointFormat: '" + returns1.getName() + ": <b>{point.x}</b><br/>" + returns2.getName()
          + ": <b>{point.y}</b>'\n");
      writer.write("    }\n");
      writer.write("   }\n");
      writer.write("  },\n");
      writer.write("  series: [{\n");
      writer.write("   type: 'line',\n");
      writer.writef("   data: [[%f, %f], [%f, %f]],\n", vmin, vmin, vmax, vmax);
      writer.write("   color: 'rgba(0,0,0,0.2)',\n");
      writer.write("   marker: { enabled: false },\n");
      writer.write("   states: { hover: { lineWidth: 0 } },\n");
      writer.write("   enableMouseTracking: false\n");
      writer.write("  },{\n");
      writer.write("   name: 'Returns (above)',\n");
      writer.write("   color: 'rgba(83, 223, 83, 0.5)',\n");
      writer.write("   data: [");
      for (int i = 0; i < above.length(); ++i) {
        double x = above.get(i, 0);
        double y = above.get(i, 1);
        writer.writef("[%.3f,%.3f]", x, y);
        if (i < above.length() - 1) {
          writer.write(",");
        }
      }
      writer.write("]}, {\n");
      writer.write("   name: 'Returns (below)',\n");
      writer.write("   color: 'rgba(223, 83, 83, 0.5)',\n");
      writer.write("   data: [");
      for (int i = 0; i < below.length(); ++i) {
        double x = below.get(i, 0);
        double y = below.get(i, 1);
        writer.writef("[%.3f,%.3f]", x, y);
        if (i < below.length() - 1) {
          writer.write(",");
        }
      }
      writer.write("  ]}]\n");
      writer.write(" });\n");
      writer.write("});\n");

      writer.write("</script></head><body>\n");
      writer.writef("<div id=\"chart\" style=\"width:%s; height:%s;\" />\n", width, height);
      writer.write("</body></html>\n");
    }
  }

  /**
   * Convenience function that takes individual arguments and builds a ChartConfig.
   */
  public static ChartConfig saveScatterPlot(File file, String title, String width, String height, double radius,
      String[] dimNames, Sequence... scatter) throws IOException
  {
    ChartConfig config = ChartConfig.buildScatter(file, title, width, height, radius, dimNames, scatter);
    config.setLegendConfig(String.format("enabled: %s,", scatter.length > 1));
    saveScatterPlot(config);
    return config;
  }

  public static void saveScatterPlot(ChartConfig config) throws IOException
  {
    boolean hasNames = false;
    for (Sequence scatter : config.data) {
      assert scatter.getNumDims() >= 2;

      // Determine if any point has a name.
      if (!hasNames) {
        for (FeatureVec v : scatter) {
          if (v.getName() != null && !v.getName().isEmpty()) {
            hasNames = true;
            break;
          }
        }
      }
    }

    String[] dimNames = config.dimNames == null ? new String[] { "x", "y" } : config.dimNames;

    // Write HTML to generate the graph.
    try (Writer writer = new Writer(config.file)) {
      writer.write("<html><head>\n");
      writer.writef("<script src=\"%s\"></script>\n", jquery);
      if (config.type == ChartConfig.Type.Scatter) {
        writer.write("<script src=\"js/highcharts.js\"></script>\n");
      } else {
        writer.write("<script src=\"https://code.highcharts.com/highcharts.js\"></script>\n");
        writer.write("<script src=\"https://code.highcharts.com/highcharts-more.js\"></script>\n");
      }
      writer.write("  <script type=\"text/javascript\">\n");
      writer.write("$(function () {\n");
      writer.write(" $('#chart').highcharts({\n");
      writer.write("  title: { text: " + ChartConfig.getQuotedString(config.title) + " },\n");
      writer.write("  chart: {\n");
      writer.write("   type: '" + ChartConfig.chart2name(config.type) + "',\n");
      writer.write("   zoomType: 'xy'\n");
      writer.write("  },\n");
      writer.write("  xAxis: {\n");
      writer.write("   gridLineWidth: 1,\n");
      writer.write("   title: {\n");
      writer.write("    text: " + ChartConfig.getQuotedString(config.xAxisTitle) + ",\n");
      writer.write("    style: {\n");
      writer.write("     fontSize: '" + config.axisTitleFontSize + "px',\n");
      writer.write("    }\n");
      writer.write("   },\n");
      writer.write("   labels: {\n");
      writer.write("    style: {\n");
      writer.write("     fontSize: '" + config.axisLabelFontSize + "px',\n");
      writer.write("    }\n");
      writer.write("   },\n");
      writer.write("  },\n");
      writer.write("  yAxis: {\n");
      writer.write("   title: {\n");
      writer.write("    text: " + ChartConfig.getQuotedString(config.yAxisTitle) + ",\n");
      writer.write("    style: {\n");
      writer.write("     fontSize: '" + config.axisTitleFontSize + "px',\n");
      writer.write("    }\n");
      writer.write("   },\n");
      writer.write("   labels: {\n");
      writer.write("    style: {\n");
      writer.write("     fontSize: '" + config.axisLabelFontSize + "px',\n");
      writer.write("    }\n");
      writer.write("   },\n");
      writer.write("  },\n");
      if (config.legendConfig != null && !config.legendConfig.isEmpty()) {
        writer.write("  legend: { " + config.legendConfig + " },\n");
      }
      writer.write("  plotOptions: {\n");
      if (config.type == ChartConfig.Type.Scatter) {
        writer.write("   scatter: {\n");
        writer.writef("    marker: { radius: %.1f, symbol: 'circle' },\n", config.radius);
        writer.write("    dataLabels: {\n");
        writer.write("     enabled: " + config.showDataLabels + "\n");
        writer.write("    },\n");
        writer.write("   },\n");
        writer.write("   series: {\n");
        writer.write("    turboThreshold: 0\n");
        writer.write("   }\n");
      } else if (config.type == ChartConfig.Type.Bubble) {
        writer.write("   bubble: {\n");
        writer.write("    minSize: '" + config.minBubble + "',\n");
        writer.write("    maxSize:'" + config.maxBubble + "',\n");
        writer.write("   },\n");
      }
      writer.write("  },\n");
      writer.write("  tooltip: {\n");
      writer.write("   enabled: " + config.showToolTips + ",\n");
      if (config.showToolTips) {
        if (config.data.length <= 1) {
          writer.write("    headerFormat: '',\n");
        }
        String header = hasNames ? "<b>{point.name}</b><br/>" : "";
        StringBuilder zs = new StringBuilder();
        int zIndex = 0;
        for (int i = 0; i < dimNames.length; ++i) {
          if (i == config.xIndex || i == config.yIndex) continue;
          zs.append(String.format("<br/>%s: <b>{point.z%s}</b>", dimNames[i],
              zIndex == 0 ? "" : String.format("%d", zIndex + 1)));
          ++zIndex;
        }
        String format = String.format("    pointFormat: '%s%s: <b>{point.x}</b><br/>%s: <b>{point.y}</b>%s'\n", header,
            dimNames[config.xIndex], config.dimNames[config.yIndex], zs);
        writer.write(format);
      }
      writer.write("  },\n");
      writer.write("  series: [\n");
      for (Sequence scatter : config.data) {
        writer.write("   {\n");
        if (scatter.getName() != null && !scatter.getName().isEmpty()) {
          writer.writef("  name: '%s',\n", scatter.getName());
        }
        writer.write("    data: [\n");
        for (FeatureVec v : scatter) {
          StringBuilder dataString = new StringBuilder(
              String.format("x:%.3f,y:%.3f", v.get(config.xIndex), v.get(config.yIndex)));
          int nDims = Math.min(v.getNumDims(), config.dimNames.length);
          int zIndex = 0;
          for (int i = 0; i < nDims; ++i) {
            if (i == config.xIndex || i == config.yIndex) continue;
            dataString.append(String.format(",z%s:%.3f", zIndex == 0 ? "" : String.format("%d", zIndex + 1), v.get(i)));
            ++zIndex;
          }
          if (hasNames) {
            String name = FinLib.getBaseName(v.getName());
            writer.writef("{%s,name:'%s'},\n", dataString, FinLib.getBaseName(name));
          } else {
            writer.writef("{%s},\n", dataString);
          }
        }
        writer.write("]},\n");
      }
      writer.write(" ]});\n");
      writer.write("});\n");

      writer.write("</script></head><body>\n");
      writer.writef("<div id=\"chart\" style=\"width:%s; height:%s;\" />\n", config.width, config.height);
      writer.write("</body></html>\n");
    }
  }
}
