package org.minnen.dmswr.data;

import org.minnen.dmswr.utils.TimeLib;

/**
 * Represents a single Bengen SWR data point.
 */
public final class BengenEntry
{
  public final long time;
  public final int  retirementYears;
  public final int  percentStock;
  public final int  swr;

  /** Build a sequence query (no time or SWR). */
  public BengenEntry(int retirementYears, int percentStock)
  {
    this(TimeLib.TIME_ERROR, retirementYears, percentStock);
  }

  /** Build a point query (no SWR). */
  public BengenEntry(long time, int retirementYears, int percentStock)
  {
    this(time, retirementYears, percentStock, -1);
  }

  public BengenEntry(long time, int retirementYears, int percentStock, int swr)
  {
    assert percentStock >= 0 && percentStock <= 100 : percentStock;

    this.time = time;
    this.retirementYears = retirementYears;
    this.percentStock = percentStock;
    this.swr = swr;
  }

  public static BengenEntry fromCSV(String line)
  {
    String[] fields = line.split(",");
    assert fields.length == 4;

    final int retirementYears = Integer.parseInt(fields[0]);
    assert retirementYears > 0;

    final int percentStock = Integer.parseInt(fields[1]);
    assert percentStock >= 0 && percentStock <= 100;

    final long time = TimeLib.parseDate(fields[2]);
    assert time != TimeLib.TIME_ERROR;

    final int swr = Integer.parseInt(fields[3]);
    assert swr > 0 && swr <= 100000;

    return new BengenEntry(time, retirementYears, percentStock, swr);
  }

  public String toCSV()
  {
    assert swr > 0;
    return String.format("%d,%d,%s,%d", retirementYears, percentStock, TimeLib.formatYM(time), swr);
  }

  @Override
  public String toString()
  {
    if (swr > 0) {
      return String.format("[%s (%d,%d) %d]", TimeLib.formatYM(time), retirementYears, percentStock, swr);
    } else if (time != TimeLib.TIME_ERROR) {
      return String.format("[%s, %d, %d]", TimeLib.formatYM(time), retirementYears, percentStock);
    } else {
      return String.format("[%d, %d]", retirementYears, percentStock);
    }
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + percentStock;
    result = prime * result + retirementYears;
    result = prime * result + (int) (time ^ (time >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    BengenEntry other = (BengenEntry) obj;
    if (percentStock != other.percentStock) return false;
    if (retirementYears != other.retirementYears) return false;
    if (time != other.time) return false;
    return true;
  }

}
