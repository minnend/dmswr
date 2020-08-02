package org.minnen.dmswr.viz;

public class PlotLine
{
  public final double value;
  public final double width;
  public final String color;
  public final String dashStyle;

  // style options:
  // 'Solid',
  // 'ShortDash',
  // 'ShortDot',
  // 'ShortDashDot',
  // 'ShortDashDotDot',
  // 'Dot',
  // 'Dash',
  // 'LongDash',
  // 'DashDot',
  // 'LongDashDot',
  // 'LongDashDotDot'

  public PlotLine(double value, double width, String color, String dashStyle)
  {
    this.value = value;
    this.width = width;
    this.color = color;
    this.dashStyle = dashStyle;
  }

  public PlotLine(double value, double width, String color)
  {
    this(value, width, color, "Solid");
  }
}
