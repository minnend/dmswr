package org.minnen.dmswr.utils;

import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.FinLib;

/**
 * A BondFactory creates different kinds of bonds.
 * 
 * @author David Minnen
 */
public abstract class BondFactory
{
  public static class Receipt
  {
    public Bond   bond;
    public double cash;

    public Receipt(Bond bond, double cash)
    {
      this.bond = bond;
      this.cash = cash;
    }
  }

  public abstract Receipt buy(Sequence bondData, double cash, int startIndex);

  public abstract int durationInMonths();

  public abstract String name();

  public double getQuantum()
  {
    return 100.0;
  }

  public double getMaxSpendZeroCoupon(double cash, double interestRate)
  {
    final double years = durationInMonths() / 12.0;
    double x = FinLib.getFutureValue(cash, interestRate, years);
    x = getClosestQuantum(x);
    return FinLib.getPresentValue(x, interestRate, years);
  }

  public double getParForPriceZeroCoupon(double price, double interestRate)
  {
    final double years = durationInMonths() / 12.0;
    return FinLib.getFutureValue(price, interestRate, years);
  }

  /** @return Maximum bond value you can buy with the given amount of cash. */
  public double getClosestQuantum(double cash)
  {
    final double quantum = getQuantum();
    return quantum * Math.floor(cash / quantum);
  }

  public static final BondFactory note10Year = get10YearTreasuryNoteFactory();
  public static final BondFactory bill3Month = get3MonthTreasuryBillFactory();

  /**
   * Get a BondFactory that creates 10-Year Treasury Notes.
   * 
   * These are 10-year bonds that pay a coupon every six months.
   * 
   * @return BondFactory for 10-Year Treasury Notes.
   */
  public static BondFactory get10YearTreasuryNoteFactory()
  {
    return new BondFactory()
    {
      @Override
      public Receipt buy(Sequence bondData, double cash, int startIndex)
      {
        assert cash >= 0.0;
        double spend = getClosestQuantum(cash);
        Bond bond = null;
        if (spend > 0.0) {
          bond = new Bond(bondData, spend, startIndex);
        }
        return new Receipt(bond, cash - spend);
      }

      @Override
      public int durationInMonths()
      {
        return 120;
      }

      @Override
      public String name()
      {
        return "10-Year Note";
      }
    };
  }

  /**
   * Get a BondFactory that creates 3-Month Treasury Bills.
   * 
   * These are 3-month zero-coupon bonds.
   * 
   * @return BondFactory for 3-Month Treasury Bills.
   */
  public static BondFactory get3MonthTreasuryBillFactory()
  {
    return new BondFactory()
    {
      @Override
      public Receipt buy(Sequence bondData, double cash, int startIndex)
      {
        assert cash >= 0.0;
        double rate = bondData.get(startIndex, 0);
        double spend = getMaxSpendZeroCoupon(cash, rate);
        Bond bond = null;
        if (spend > 0.0) {
          double par = getParForPriceZeroCoupon(spend, rate);
          bond = new Bond(bondData, par, 0.0, 0.0, startIndex, startIndex + durationInMonths());
        }
        return new Receipt(bond, cash - spend);
      }

      @Override
      public int durationInMonths()
      {
        return 3;
      }

      @Override
      public String name()
      {
        return "3-Month Bill";
      }
    };
  }
}
