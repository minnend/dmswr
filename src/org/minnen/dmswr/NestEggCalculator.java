package org.minnen.dmswr;

/** Calculates the initial retirement portfolio value (a "nest egg"). */
public abstract class NestEggCalculator
{
  public abstract double getNestEgg(int iCurrent, int iStartSim, int lookbackYears, int percentStock);

  /** Ignore arguments and always return the same value. */
  public static NestEggCalculator constant(double value)
  {
    return new NestEggCalculator()
    {
      @Override
      public double getNestEgg(int iCurrent, int iStartSim, int lookbackYears, int percentStock)
      {
        return value;
      }
    };
  }

  /**
   * Undo inflation and then move forward using market growth.
   *
   * The nest egg is `value` adjusted for inflation back to the first index in the simulation and then projected forward
   * according to market growth. *
   */
  public static NestEggCalculator inflationThenGrowth(double value, boolean removeInitialInflation)
  {
    return new NestEggCalculator()
    {
      @Override
      public double getNestEgg(int iCurrent, int iStartSim, int lookbackYears, int percentStock)
      {
        final int lookbackMonths = lookbackYears * 12;
        assert iStartSim >= lookbackMonths;

        double nestEgg = value;
        if (removeInitialInflation) {
          nestEgg *= SwrLib.inflation(-1, lookbackMonths); // adjusted for inflation to start of sim
        }
        nestEgg *= SwrLib.growth(lookbackMonths, iCurrent, percentStock); // update forward based on market growth
        return nestEgg;
      }
    };
  }

  /**
   * 
   *
   * The nest egg starts with `value`. Every month, `monthlySavings` is saved and the portfolio value moves up or down
   * with the market. If Inflation is set to `Real`, the value at `iCurrent` is adjusted to `iStartSim` dollars.
   */
  public static NestEggCalculator monthlySavings(double value, double monthlySavings, boolean removeInitialInflation,
      boolean inflationToStart, boolean inflationToToday)
  {
    return new NestEggCalculator()
    {
      @Override
      public double getNestEgg(int iCurrent, int iStartSim, int lookbackYears, int percentStock)
      {
        assert iCurrent >= iStartSim;
        assert lookbackYears >= 0;
        assert !inflationToStart || !inflationToToday; // can't do both
        final int lookbackMonths = lookbackYears * 12;

        double nestEgg = value;
        if (removeInitialInflation) {
          nestEgg *= SwrLib.inflation(-1, lookbackMonths); // adjusted for inflation to start of sim
        }
        for (int i = iStartSim; i < iCurrent; ++i) {
          nestEgg += monthlySavings;
          nestEgg *= SwrLib.growth(i, percentStock); // update forward based on market growth
        }
        if (inflationToStart) {
          nestEgg *= SwrLib.inflation(iCurrent, iStartSim); // adjust for inflation to beginning of sim
        } else if (inflationToToday) {
          nestEgg *= SwrLib.inflation(iCurrent, -1); // adjust for inflation to today
        }
        return nestEgg;
      }
    };
  }
}
