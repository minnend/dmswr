package org.minnen.dmswr.data;

import org.minnen.dmswr.utils.TimeLib;

/**
 * Represents a single DMSWR data point.
 */
public final class MarwoodEntry
{
  // TODO verify that `finalBalance` is for the end of the final month, not the beginning.

  public final long   retireTime;
  public final long   currentTime;
  public final int    retirementYears;
  public final int    lookbackYears;
  public final int    percentStock;
  public final int    swr;
  public final int    virtualRetirementMonths;
  public final double finalBalance;
  public final double bengenSalary;
  public final double marwoodSalary;
  public final double crystalSalary;

  /** Build a query for a sequence of starting DMSWR values. */
  public MarwoodEntry(int retirementYears, int lookbackYears, int percentStock)
  {
    this(TimeLib.TIME_ERROR, TimeLib.TIME_ERROR, retirementYears, lookbackYears, percentStock);
  }

  /** Build a query for a retirement path. */
  public MarwoodEntry(long retireTime, int retirementYears, int lookbackYears, int percentStock)
  {
    this(retireTime, retireTime, retirementYears, lookbackYears, percentStock);
  }

  /** Build a point query. */
  public MarwoodEntry(long retireTime, long currentTime, int retirementYears, int lookbackYears, int percentStock)
  {
    this(retireTime, currentTime, retirementYears, lookbackYears, percentStock, -1, -1, Double.NaN, Double.NaN,
        Double.NaN, Double.NaN);
  }

  public MarwoodEntry(int retirementYears, int lookbackYears, int percentStock, MonthlyInfo info)
  {
    this.retireTime = info.retireTime;
    this.currentTime = info.currentTime;
    this.retirementYears = retirementYears;
    this.lookbackYears = lookbackYears;
    this.percentStock = percentStock;
    this.swr = info.swr;
    this.virtualRetirementMonths = info.virtualRetirementMonths;
    this.finalBalance = info.finalBalance;
    this.bengenSalary = info.bengenSalary;
    this.marwoodSalary = info.marwoodSalary;
    this.crystalSalary = info.crystalSalary;
  }

  public MarwoodEntry(long retireTime, long currentTime, int retirementYears, int lookbackYears, int percentStock,
      int swr, int virtualRetirementMonths, double finalBalance, double bengenSalary, double marwoodSalary,
      double crystalSalary)
  {
    this.retireTime = retireTime;
    this.currentTime = currentTime;
    this.retirementYears = retirementYears;
    this.lookbackYears = lookbackYears;
    this.percentStock = percentStock;
    this.swr = swr;
    this.virtualRetirementMonths = virtualRetirementMonths;
    this.finalBalance = finalBalance;
    this.bengenSalary = bengenSalary;
    this.marwoodSalary = marwoodSalary;
    this.crystalSalary = crystalSalary;
  }

  public static MarwoodEntry fromCSV(String line)
  {
    String[] fields = line.split(",");
    assert fields.length == 11;

    final int retirementYears = Integer.parseInt(fields[0]);
    assert retirementYears > 0;

    final int lookbackYears = Integer.parseInt(fields[1]);
    assert lookbackYears >= 0;

    final int percentStock = Integer.parseInt(fields[2]);
    assert percentStock >= 0 && percentStock <= 100;

    final long retireTime = TimeLib.parseDate(fields[3]);
    assert retireTime != TimeLib.TIME_ERROR;

    final long currentTime = TimeLib.parseDate(fields[4]);
    assert currentTime != TimeLib.TIME_ERROR;
    assert currentTime >= retireTime;

    final int swr = Integer.parseInt(fields[5]);
    assert swr > 0 && swr <= 100000;

    final int virtualRetirementMonths = Integer.parseInt(fields[6]);
    assert virtualRetirementMonths >= 0;

    final double finalBalance = Double.parseDouble(fields[7]);
    final double bengenSalary = Double.parseDouble(fields[8]);
    final double marwoodSalary = Double.parseDouble(fields[9]);
    final double crystalSalary = Double.parseDouble(fields[10]);

    return new MarwoodEntry(retireTime, currentTime, retirementYears, lookbackYears, percentStock, swr,
        virtualRetirementMonths, finalBalance, bengenSalary, marwoodSalary, crystalSalary);
  }

  public String toCSV()
  {
    assert swr > 0;
    return String.format("%d,%d,%d,%s,%s,%d,%d,%.2f,%.2f,%.2f,%.2f", retirementYears, lookbackYears, percentStock,
        TimeLib.formatYM(retireTime), TimeLib.formatYM(currentTime), swr, virtualRetirementMonths, finalBalance,
        bengenSalary, marwoodSalary, crystalSalary);
  }

  @Override
  public String toString()
  {
    if (swr > 0) {
      return String.format("[%s (%d,%d,%d) %d]", TimeLib.formatYM(retireTime), retirementYears, lookbackYears,
          percentStock, swr);
    } else if (retireTime != TimeLib.TIME_ERROR) {
      return String.format("[%s, %d, %d, %d]", TimeLib.formatYM(retireTime), retirementYears, lookbackYears,
          percentStock);
    } else {
      return String.format("[%d, %d, %d]", retirementYears, lookbackYears, percentStock);
    }
  }

  /** @return True if this is the first month of a retirement. */
  public boolean isRetirementStart()
  {
    return currentTime == retireTime;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (currentTime ^ (currentTime >>> 32));
    result = prime * result + lookbackYears;
    result = prime * result + percentStock;
    result = prime * result + (int) (retireTime ^ (retireTime >>> 32));
    result = prime * result + retirementYears;
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MarwoodEntry other = (MarwoodEntry) obj;
    if (currentTime != other.currentTime) return false;
    if (lookbackYears != other.lookbackYears) return false;
    if (percentStock != other.percentStock) return false;
    if (retireTime != other.retireTime) return false;
    if (retirementYears != other.retirementYears) return false;
    return true;
  }

}
