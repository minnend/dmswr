package org.minnen.dmswr.utils;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.Shiller;

public final class FinLib
{
  public static final double SOC_SEC_AT70 = 3834.00; // http://www.ssa.gov/oact/quickcalc/
  public static final double SOC_SEC_AT65 = 2695.00; // pulled 11/2018
  public static final double SOC_SEC_AT62 = 2171.00;

  public enum DividendMethod {
    NO_REINVEST_MONTHLY, NO_REINVEST_QUARTERLY, MONTHLY, QUARTERLY, IGNORE_DIVIDENDS
  };

  public enum Inflation {
    Real, Nominal
  };

  public static final int     MonthlyClose      = 0;
  public static final int     MonthlyAverage    = 1;
  public static final int     MonthlyLow        = 2;
  public static final int     MonthlyHigh       = 3;
  public static final int     MonthlyMisc       = 4;
  public static final int     MonthlyOpen       = 5;
  public static final int     MonthlyReturn     = 6;
  public static final int     NumMonthlyFields  = 7;

  public static final int     Close             = 0;
  public static final int     Open              = 1;
  public static final int     Low               = 2;
  public static final int     High              = 3;
  public static final int     Volume            = 4;
  public static final int     AdjClose          = 5;
  public static final int     AdjHigh           = 6;
  public static final int     AdjLow            = 7;
  public static final int     AdjOpen           = 8;
  public static final int     AdjVolume         = 9;
  public static final int     DivCash           = 10;
  public static final int     SplitFactor       = 11;

  public static DecimalFormat currencyFormatter = new DecimalFormat("#,##0.00");
  public static DecimalFormat dollarFormatter   = new DecimalFormat("#,##0");

  /**
   * Compute compound annual growth rate (CAGR) based on total multiplier.
   * 
   * @param totalReturn principal multiplier over full term (e.g., 30% gain = 1.3)
   * @param nMonths number of months in term
   * @return average annual return as a percentage (e.g., 4.32 = 4.32%/yr)
   */
  public static double getAnnualReturn(double totalReturn, double nMonths)
  {
    if (nMonths <= 0.0) {
      return 1.0;
    }

    // x^n = y -> x = y ^ (1/n)
    return mul2ret(Math.pow(totalReturn, 12.0 / nMonths));
  }

  public static double getFutureValue(double presentValue, double interestRate, double nPeriods)
  {
    return presentValue * Math.pow(FinLib.ret2mul(interestRate), nPeriods);
  }

  public static double getPresentValue(double futureValue, double interestRate, double nPeriods)
  {
    return futureValue * Math.pow(FinLib.ret2mul(interestRate), -nPeriods);
  }

  /**
   * Convert value at one point in time into equivalent value at another according to inflation handling option.
   * 
   * @param cpi Sequence of CPI values
   * @param value current value
   * @param iFrom current time
   * @param iTo query time
   * @param inflationAccounting should we adjust for inflation?
   * @return equivalent value at query time
   */
  public static double adjustValue(Sequence cpi, double value, int iFrom, int iTo, Inflation inflationAccounting)
  {
    if (iFrom < 0) {
      iFrom += cpi.length();
    }
    if (iTo < 0) {
      iTo += cpi.length();
    }
    if (inflationAccounting == Inflation.Real) {
      value *= cpi.get(iTo, 0) / cpi.get(iFrom, 0);
    }
    return value;
  }

  /**
   * Generates a sequence of real returns (inflation-adjusted) from nominal returns.
   * 
   * @param cumulativeReturns nominal returns
   * @param cpi cumulative inflation
   * @return Sequence corresponding to real returns (inflation-adjusted)
   */
  public static Sequence calcRealReturns(Sequence cumulativeReturns, Sequence cpi)
  {
    // Check pre-conditions.
    assert cumulativeReturns.length() == cpi.length();
    assert cumulativeReturns.getStartMS() == cpi.getStartMS();
    assert cumulativeReturns.getEndMS() == cpi.getEndMS();

    // Build sequence of real returns from nominal.
    Sequence seq = new Sequence(cumulativeReturns.getName() + " (real)");
    seq.addData(1.0, cumulativeReturns.getStartMS());
    for (int i = 1; i < cumulativeReturns.length(); ++i) {
      double growth = getTotalReturn(cumulativeReturns, i - 1, i);
      double inflation = getTotalReturn(cpi, i - 1, i);
      double real = seq.getLast(0) * growth / inflation;
      seq.addData(real, cumulativeReturns.getTimeMS(i));
    }

    // Check post-conditions.
    assert cumulativeReturns.length() == seq.length();
    assert cumulativeReturns.getStartMS() == seq.getStartMS();
    assert cumulativeReturns.getEndMS() == seq.getEndMS();

    return seq;
  }

  /**
   * Convert cash value at one point in time into equivalent value at another according to relative CPI.
   * 
   * @param cpi Sequence of CPI values
   * @param value current value
   * @param iFrom current time
   * @param iTo query time
   * @return equivalent value at query time
   */
  public static double adjustForInflation(Sequence cpi, double value, int iFrom, int iTo)
  {
    return adjustValue(cpi, value, iFrom, iTo, Inflation.Real);
  }

  /**
   * Convert a multiplier to a return (1.042 -> 4.2%)
   * 
   * @param multiplier a multiplier (1.042 = 4.2% ROI)
   * @return a return value (4.2 = 4.2% ROI)
   */
  public static double mul2ret(double multiplier)
  {
    return (multiplier - 1.0) * 100.0;
  }

  /**
   * Convert a return to a multiplier (4.2% -> 1.042)
   * 
   * @param ret a return value (4.2 = 4.2% ROI)
   * @return a multiplier (1.042 = 4.2% ROI)
   */
  public static double ret2mul(double ret)
  {
    return ret / 100.0 + 1.0;
  }

  /**
   * Append dimension that stores likelihood of meeting or exceeding the current ROI
   * 
   * @param h histogram sequence (frequencies expected in dim=2)
   * @return new sequence with new dimension storing likelihood of higher return
   */
  public static Sequence addReturnLikelihoods(Sequence h, boolean bInvert)
  {
    int n = h.size();
    double[] freq = h.extractDim(2);
    double[] cum = new double[n];

    for (int i = 1; i < n; i++)
      cum[i] = cum[i - 1] + freq[i - 1];

    if (bInvert) {
      // we want likelihood of getting a higher return, not lower
      for (int i = 0; i < n; i++)
        cum[i] = 1.0 - cum[i];
    }

    return h._appendDims(new Sequence(cum));
  }

  public static Sequence appendROISeq(Sequence a, Sequence b)
  {
    if (a == null) return b.extractDims(0, 3);

    final int indexDataB = 3;
    int D = a.getNumDims();
    double gap = a.get(1, 0) - a.get(0, 0);
    int na = a.size();
    int nb = b.size();
    double startA = a.get(0, 0);
    double startB = b.get(0, 0);
    double endA = a.get(na - 1, 0);
    double endB = b.get(nb - 1, 0);
    double start = Math.min(startA, startB);
    double end = Math.max(endA, endB);

    double firstValueA = a.get(0, D - 1);
    double lastValueA = a.get(na - 1, D - 1);
    double firstValueB = b.get(0, indexDataB);
    double lastValueB = b.get(nb - 1, indexDataB);

    int n = (int) Math.round((end - start) / gap) + 1;
    // System.out.printf("Merge: (%.2f -> %.2f) + (%.2f -> %.2f) = (%.2f -> %.2f) %d\n", startA, endA, startB, endB,
    // start, end, n);
    // System.out.printf(" A: [%.3f -> %.3f] B: [%.3f -> %.3f]\n", firstValueA, lastValueA, firstValueB, lastValueB);

    double eps = 1e-5;
    int nd = a.getNumDims(); // one for roi, rest is real data
    Sequence seq = new Sequence();
    double roi = start;
    int ia = 0, ib = 0;
    for (int i = 0; i < n; i++, roi += gap) {
      // start current feature vec with data from seqA
      FeatureVec fv = null;
      if (ia >= na) { // seqA ran out of data
        fv = new FeatureVec(nd, roi, lastValueA);
      } else {
        double ra = a.get(ia, 0);
        if (ra - eps > roi) { // have not started seqA yet
          fv = new FeatureVec(nd, roi, firstValueA);
        } else { // copy current entry from seqA
          assert (Math.abs(ra - roi) < eps);
          fv = new FeatureVec(a.get(ia++));
        }
      }

      // now add data from seqB
      if (ib >= nb) { // seqB ran out of data
        fv._appendDim(lastValueB);
      } else {
        double rb = b.get(ib, 0);
        if (rb - eps > roi) { // have not started seqB yet
          fv._appendDim(firstValueB);
        } else { // copy current entry from seqB
          assert (Math.abs(rb - roi) < eps);
          fv._appendDim(b.get(ib++, indexDataB));
        }
      }

      // add the new entry to the combined sequence
      seq.addData(fv);
    }

    return seq;
  }

  /**
   * Calculate forward returns for all periods with the given duration.
   * 
   * @param monthlyReturns sequence of cumulative returns for the investment strategy (monthly data)
   * @param nMonths number of months in the market
   * @return Sequence containing returns for each time period of the given duration; if nMonths < 12 or there is
   *         insufficient data to fill a single period, the values are total returns, else they are CAGRs.
   */
  public static Sequence calcReturnsForMonths(Sequence monthlyReturns, int nMonths)
  {
    final int N = monthlyReturns.size();
    String name = String.format("%s (%s)", monthlyReturns.getName(), TimeLib.formatDurationMonths(nMonths));
    Sequence rois = new Sequence(name);
    if (N <= 0) {
      return rois;
    } else if (N < nMonths) { // no full periods so return ROI for the one partial period.
      double growth = FinLib.getTotalReturn(monthlyReturns, 0, N - 1);
      double roi = mul2ret(growth);
      rois.addData(roi, monthlyReturns.getStartMS());
    } else {
      for (int i = 0; i + nMonths <= N; ++i) {
        double roi = getTotalReturn(monthlyReturns, i, i + nMonths - 1);
        if (nMonths >= 12) {
          roi = getAnnualReturn(roi, nMonths);
        } else {
          roi = mul2ret(roi);
        }
        rois.addData(roi, monthlyReturns.getTimeMS(i));
      }
      assert rois.size() > 0;
    }
    return rois;
  }

  /**
   * Calculates annual salary needed to generate the given monthly take-home cash.
   * 
   * @param monthlyCash desired monthly take-home cash
   * @param effectiveTaxRate effective (total) tax rate (0.3 = 30%)
   * @return required annual salary to generate monthly cash
   */
  public static double calcAnnualSalary(double monthlyCash, double effectiveTaxRate)
  {
    return monthlyCash * 12 / (1.0 - effectiveTaxRate);
  }

  /**
   * Calculate total return for the given segment (1.10 = 10% return).
   * 
   * @param cumulativeReturns sequence of cumulative returns for the investment strategy
   * @param iFrom index of starting point for calculation
   * @param iTo index of ending point for calculation (inclusive)
   * @param iDim dimension to use for price data.
   * @return total return from iFrom to iTo.
   */
  public static double getTotalReturn(Sequence cumulativeReturns, int iFrom, int iTo, int iDim)
  {
    if (cumulativeReturns.isEmpty()) return 1.0;
    return cumulativeReturns.get(iTo, iDim) / cumulativeReturns.get(iFrom, iDim);
  }

  /**
   * Calculate total return for the given segment.
   * 
   * @param cumulativeReturns sequence of cumulative returns for the investment strategy
   * @param iFrom index of starting point for calculation
   * @param iTo index of ending point for calculation (inclusive)
   * @return total return from iFrom to iTo.
   */
  public static double getTotalReturn(Sequence cumulativeReturns, int iFrom, int iTo)
  {
    return getTotalReturn(cumulativeReturns, iFrom, iTo, 0);
  }

  /** @return total return for full sequence (first to last date). */
  public static double getTotalReturn(Sequence cumulativeReturns)
  {
    return getTotalReturn(cumulativeReturns, 0, -1);
  }

  /**
   * Compute ending balance; not inflation-adjusted, assuming we re-invest dividends
   * 
   * @param cumulativeReturns sequence of cumulative returns for the investment strategy
   * @param cpi sequence of CPI values
   * @param principal initial funds
   * @param annualWithdrawal annual withdrawal amount (annual "salary")
   * @param adjustWithdrawalForInflation use CPI to adjust withdrawal for constant purchasing power
   * @param expenseRatio percentage of portfolio taken by manager (2.1 = 2.1% per year)
   * @param retireAge age at retirement (start of simulation)
   * @param ssAge age when you first receive SS benefits
   * @param ssMonthly expected monthly SS benefit in today's dollar
   * @param iStart first index in SNP
   * @param nMonths number of months (ticks) in S&P to consider
   * @return ending balance
   */
  public static double calcEndBalance(Sequence cumulativeReturns, Sequence cpi, double principal,
      double annualWithdrawal, double expenseRatio, boolean adjustWithdrawalForInflation, double retireAge,
      double ssAge, double ssMonthly, int iStart, int nMonths)
  {
    int nData = cumulativeReturns.size();
    assert cpi.size() == nData;
    if (iStart < 0 || nMonths < 1 || iStart + nMonths >= nData) {
      return Double.NaN;
    }

    double age = retireAge;
    double monthlyWithdrawal = annualWithdrawal / 12.0;
    double monthlyExpenseRatio = (expenseRatio / 100.0) / 12.0;
    double balance = principal;
    for (int i = iStart; i < iStart + nMonths; ++i) {
      double adjMonthlyWithdrawal = monthlyWithdrawal;
      if (adjustWithdrawalForInflation) {
        // Update monthly withdrawal for inflation.
        adjMonthlyWithdrawal = adjustForInflation(cpi, monthlyWithdrawal, iStart, i);
      }

      // Get paid by social security if we're old enough.
      if (age >= ssAge) {
        balance += adjustForInflation(cpi, ssMonthly, nData - 1, i);
      }

      // Withdraw money at beginning of month.
      balance -= adjMonthlyWithdrawal;
      if (balance < 0.0) {
        return 0.0; // ran out of money!
      }

      // Update balance based on investment returns.
      balance *= getTotalReturn(cumulativeReturns, i, i + 1);

      // Broker gets their cut.
      balance *= (1.0 - monthlyExpenseRatio);

      // Now we're one month older.
      age += Library.ONE_TWELFTH;
    }

    return balance;
  }

  public static double calcEndBalance(Sequence inflationAdjustedReturns, double principal, double annualWithdrawal,
      double expenseRatio, int iStart, int nMonths)
  {
    int nData = inflationAdjustedReturns.size();
    assert (iStart >= 0 && nMonths >= 1 && iStart + nMonths < nData);

    double monthlyWithdrawal = annualWithdrawal / 12.0;
    double monthlyExpenseRatio = (expenseRatio / 100.0) / 12.0; // TODO x/12 or x^(1/12)?
    double balance = principal;
    for (int i = iStart; i < iStart + nMonths; ++i) {
      // Withdraw money at beginning of month.
      balance -= monthlyWithdrawal;
      if (balance < 0.0) {
        return 0.0; // ran out of money!
      }

      // Update balance based on investment returns.
      balance *= getTotalReturn(inflationAdjustedReturns, i, i + 1);

      // Broker gets their cut.
      balance *= (1.0 - monthlyExpenseRatio);
    }

    return balance;
  }

  /**
   * Compute ending balance; not inflation-adjusted, assuming we re-invest dividends
   * 
   * @param cumulativeReturns sequence of cumulative returns for the investment strategy
   * @param principal initial funds
   * @param depStartOfYear deposit at the beginning of each calendar year
   * @param depEndOfMonth deposit at the end of each month
   * @param expenseRatio percentage of portfolio taken by manager (2.1 = 2.1% per year)
   * @param iStart first index in SNP
   * @param nMonths number of months (ticks) in S&P to consider
   * @return ending balance
   */
  public static double calcEndSavings(Sequence cumulativeReturns, double principal, double depStartOfYear,
      double depEndOfMonth, double expenseRatio, int iStart, int nMonths)
  {
    final int nData = cumulativeReturns.size();
    assert (iStart >= 0 && nMonths >= 1 && iStart + nMonths < nData);

    final double monthlyExpenseRatio = (expenseRatio / 100.0) / 12.0;
    double balance = principal;
    // double totalDeposit = principal;
    for (int i = iStart; i < iStart + nMonths; ++i) {
      // Make deposit at start of year.
      LocalDate date = TimeLib.ms2date(cumulativeReturns.getTimeMS(i));
      if (date.getMonth() == Month.JANUARY) {
        balance += depStartOfYear;
        // totalDeposit += depStartOfYear;
      }

      // Update balance based on investment returns.
      balance *= getTotalReturn(cumulativeReturns, i, i + 1);

      // Broker gets their cut.
      balance *= (1.0 - monthlyExpenseRatio);

      // Deposit at end of the month.
      balance += depEndOfMonth;
      // totalDeposit += depEndOfMonth;
    }

    return balance;
  }

  public static void calcSavings(double principal, double depStartOfYear, double depStartOfMonth, double cagr,
      int years)
  {
    double annualReturn = ret2mul(cagr);
    double monthlyReturn = Math.pow(annualReturn, Library.ONE_TWELFTH);
    double balance = principal;
    System.out.println("Starting Balance: $" + currencyFormatter.format(balance));
    for (int year = 0; year < years; ++year) {
      balance += depStartOfYear;
      for (int month = 0; month < 12; ++month) {
        balance += depStartOfMonth;
        balance *= monthlyReturn;
      }
      System.out.printf("End of Year %d (%d): $%s\n", year + 2015, year + 35, currencyFormatter.format(balance));
    }
  }

  /**
   * Calculate quarterly dividend payments.
   * 
   * The time range is: [max(index-2, iMinIndex)..index], but the return value is zero if snp[index] does not represent
   * the end of a quarter (March, June, September, or December).
   * 
   * @return value of quarterly dividend paid at time snp[index].
   */
  private static double getQuarterlyDividendsPerShare(Sequence snp, int index, int iMinIndex)
  {
    double div = 0.0;

    // Dividends at the end of every quarter (march, june, september, december).
    LocalDate date = TimeLib.ms2date(snp.getTimeMS(index));
    Month month = date.getMonth();
    if (month == Month.MARCH || month == Month.JUNE || month == Month.SEPTEMBER || month == Month.DECEMBER) {
      // Time for a dividend!
      for (int j = 0; j < 3; j++) {
        if (index - j < iMinIndex) break;
        div += snp.get(index - j, Shiller.DIV);
      }
    }
    return div;
  }

  /**
   * Calculates S&P returns from Shiller data for the given range.
   * 
   * Note that Shiller's data uses monthly average closing prices, and dividends are interpolated from quarterly or
   * annual payments. So the return results are reasonable for high-level analysis but are not useful for more detailed
   * simulations (use adjusted daily data instead).
   * 
   * @param snp sequence of prices (dim=Shiller.PRICE) and dividends (dim=Shiller.DIV).
   * @param iStart first index in S&P to consider (negative => count back from end of sequence).
   * @param iEnd last index in S&P to consider (negative => count back from end of sequence).
   * @param divMethod how should we handle dividend reinvestment.
   * 
   * @return sequence with two dimensions: the first (0) holds the invested balance, while the second (1) holds any cash
   *         from dividends.
   */
  public static Sequence calcSnpReturns(Sequence snp, int iStart, int iEnd, DividendMethod divMethod)
  {
    if (iStart < 0) {
      iStart += snp.length();
    }
    if (iEnd < 0) {
      iEnd += snp.length();
    }
    if (iStart < 0 || iEnd < iStart || iEnd >= snp.length()) {
      throw new IllegalArgumentException(String.format("iStart=%d, iEnd=%d, length=%d", iStart, iEnd, snp.length()));
    }

    Sequence seq = new Sequence("S&P-" + divMethod);

    double divCash = 0.0; // no cash from dividends to start
    double shares = 1.0; // track number of shares to calculate total dividend payment

    for (int i = iStart; i <= iEnd; ++i) {
      final double price = snp.get(i, Shiller.PRICE);
      final double divMonthly = shares * snp.get(i, Shiller.DIV);
      final double divQuarterly = shares * getQuarterlyDividendsPerShare(snp, i, iStart);

      double divReinvest = 0.0;
      if (divMethod == DividendMethod.NO_REINVEST_MONTHLY) {
        divCash += divMonthly; // No dividend reinvestment so all dividends go to cash.
      } else if (divMethod == DividendMethod.MONTHLY) {
        divReinvest = divMonthly; // Dividends reinvested at the end of every month.
      } else if (divMethod == DividendMethod.NO_REINVEST_QUARTERLY) {
        divCash += divQuarterly; // Quarterly dividends saved as cash.
      } else if (divMethod == DividendMethod.QUARTERLY) {
        divReinvest += divQuarterly; // Quarterly dividends reinvested.
      } else {
        assert divMethod == DividendMethod.IGNORE_DIVIDENDS;
        // Nothing to do when we're ignoring dividends.
      }

      // Dividends earmarked for reinvestment are used to buy more shares.
      shares += divReinvest / price;
      final double balance = shares * price + divCash;
      seq.addData(balance, snp.getTimeMS(i));
    }

    // Normalize sequence so that values correspond to total returns (multiplicative).
    seq._div(seq.getFirst(Shiller.PRICE));

    return seq;
  }

  public static Sequence calcCorrelation(Sequence returns1, Sequence returns2, int window)
  {
    final int n = returns1.length() - 1;
    double[] r1 = new double[n];
    double[] r2 = new double[n];
    for (int i = 1; i <= n; ++i) {
      r1[i - 1] = returns1.get(i, 0) / returns1.get(i - 1, 0);
      r2[i - 1] = returns2.get(i, 0) / returns2.get(i - 1, 0);
    }

    Sequence corr = new Sequence(String.format("Correlation (%s): %s vs. %s", TimeLib.formatDurationMonths(window),
        returns1.getName(), returns2.getName()));
    double[] a = new double[window];
    double[] b = new double[window];
    for (int i = window; i < r1.length; ++i) {
      // Library.copy(r1, a, i - window, 0, window);
      System.arraycopy(r1, i - window, a, 0, window);
      // Library.copy(r2, b, i - window, 0, window);
      System.arraycopy(r2, i - window, b, 0, window);
      corr.addData(Library.correlation(a, b), returns1.getTimeMS(i - window / 2));
    }
    return corr;
  }

  public static double calcCorrelation(Sequence prices1, Sequence prices2, int iStart, int iEnd, int iDim)
  {
    double[] r1 = getDailyReturns(prices1, iStart, iEnd, iDim);
    double[] r2 = getDailyReturns(prices2, iStart, iEnd, iDim);
    return Library.correlation(r1, r2);
  }

  /**
   * Calculate the tick-by-tick returns from the given sequence of prices.
   * 
   * There will be N = iEnd - iStart returns. The first one is from (iStart) to (iStart+1).
   * 
   * @param prices sequence of prices
   * @param iStart first index to include
   * @param iEnd last index to include
   * @param iDim dimension of <code>prices</code> to extract
   * @return array containing per-tick returns (1.2 = 1.2% return)
   */
  public static double[] getDailyReturns(Sequence prices, int iStart, int iEnd, int iDim)
  {
    return getReturns(prices, 1, iStart, iEnd, iDim);
  }

  /**
   * Calculate the tick-by-tick returns from the given sequence of prices.
   * 
   * There will be N = iEnd - iStart returns. The first one is from (iStart) to (iStart+1).
   * 
   * @param prices sequence of prices
   * @param delay number of days over which to calculate returns
   * @param iStart first index to include
   * @param iEnd last index to include
   * @param iDim dimension of <code>prices</code> to extract
   * @return array containing per-tick returns (1.2 = 1.2% return)
   */
  public static double[] getReturns(Sequence prices, int delay, int iStart, int iEnd, int iDim)
  {
    assert delay > 0;
    if (iStart < 0) iStart += prices.length();
    if (iEnd < 0) iEnd += prices.length();
    assert iEnd > iStart;
    final int N = iEnd - iStart;
    double[] r = new double[N];
    int ir = 0;
    for (int i = iStart + delay; i <= iEnd; ++i) {
      double mul = prices.get(i, iDim) / prices.get(i - delay, iDim);
      r[ir++] = FinLib.mul2ret(mul);
    }
    assert ir == (N - delay + 1);
    return r;
  }

  /**
   * Returns the same name with a {@literal <br/>
   * } inserted before the last open paren.
   * 
   * Examples:
   * <ul>
   * <li>"foo" -> "foo"
   * <li>"foo (bar)" -> "foo{@literal <br/>
   * } (bar)"
   * <li>"foo (bar) (buzz)" -> "foo (bar){@literal <br/>
   * }(buzz)"
   * </ul>
   * 
   * @param name the name to modify
   * @return name with {@literal <br/>} inserted before last open paren
   */
  public static String getNameWithBreak(String name)
  {
    if (name == null) {
      return "";
    }
    int i = name.lastIndexOf(" (");
    if (i >= 0) {
      return name.substring(0, i) + "<br/>" + name.substring(i + 1);
    } else {
      return name;
    }
  }

  /**
   * Returns the same name with base name bolded.
   * 
   * Examples:
   * <ul>
   * <li>"foo" -> "<b>foo</b>"
   * <li>"foo (bar)" -> "<b>foo</b> (bar)"
   * <li>"foo (bar) (buzz)" -> "<b>foo (bar)</b> (buzz)"
   * </ul>
   * 
   * @param name the name to modify
   * @return name with {@literal <br/>} inserted before last open paren
   */
  public static String getBoldedName(String name)
  {
    String base = getBaseName(name);
    if (base.length() > 0) {
      base = String.format("<b>%s</b>", base);
    }
    String suffix = getNameSuffix(name);
    if (suffix.length() > 0) {
      suffix = " " + suffix;
    }
    return base + suffix;
  }

  /**
   * Returns all text before the last open paren.
   * 
   * Examples:
   * <ul>
   * <li>"foo" -> "foo"
   * <li>"foo (bar)" -> "foo"
   * <li>"foo (bar) (buzz)" -> "foo (bar)"
   * </ul>
   * 
   * @param name the name to modify
   * @return all text before last open paren
   */
  public static String getBaseName(String name)
  {
    if (name == null) {
      return "";
    }
    int i = name.lastIndexOf(" (");
    if (i >= 0) {
      return name.substring(0, i);
    } else {
      return name;
    }
  }

  /**
   * Returns everything after (and including) last paren.
   * 
   * Examples:
   * <ul>
   * <li>"foo" -> ""
   * <li>"foo (bar)" -> "(bar)"
   * <li>"foo (bar) (buzz)" -> "(buzz)"
   * </ul>
   * 
   * @param name the name to modify
   * @return all text in last parenthetical
   */
  public static String getNameSuffix(String name)
  {
    if (name == null) {
      return "";
    }
    int i = name.lastIndexOf(" (");
    if (i >= 0) {
      return name.substring(i + 1);
    } else {
      return "";
    }
  }

  public static Sequence calcDrawdown(Sequence cumulativeReturns)
  {
    return calcDrawdown(cumulativeReturns, 0, -1);
  }

  /**
   * Returns a sequence of drawdown values for the given return sequence.
   * 
   * Note that drawdown values are non-positive percentages (-50.0 = 50% drawdown).
   * 
   * @param cumulativeReturns Sequence of cumulative returns. This function assumes that the first dimension holds the
   *          return values.
   * @param iFrom start with this index
   * @param iTo end with this index (inclusive).
   * @return Sequence with the same length as `cumulativeReturns` where each value is the corresponding drawdown.
   */
  public static Sequence calcDrawdown(Sequence cumulativeReturns, int iFrom, int iTo)
  {
    if (iFrom < 0) iFrom += cumulativeReturns.length();
    if (iTo < 0) iTo += cumulativeReturns.length();

    Sequence seq = new Sequence(cumulativeReturns.getName() + " - Drawdown");
    double peakReturn = cumulativeReturns.get(iFrom, 0);
    for (int i = iFrom; i <= iTo; ++i) {
      final double value = cumulativeReturns.get(i, 0);
      double drawdown;
      if (value >= peakReturn) {
        peakReturn = value;
        drawdown = 0.0;
      } else {
        drawdown = -100.0 * (peakReturn - value) / peakReturn;
      }
      seq.addData(drawdown, cumulativeReturns.getTimeMS(i));
    }

    assert seq.length() == (iTo - iFrom + 1);
    return seq;
  }

  /** @return Sequence of monthly values inferred from daily data. */
  public static Sequence dailyToMonthly(Sequence daily)
  {
    return dailyToMonthly(daily, 0, 0);
  }

  /** @return Sequence of monthly values inferred from daily data. */
  public static Sequence dailyToMonthly(Sequence daily, int dim, int nJitter)
  {
    return dailyToMonthly(daily, dim, nJitter, 12);
  }

  /**
   * Converts a Sequence with daily data to a Sequence with monthly data.
   * 
   * @param daily Sequence of daily data
   * @param dim dimension in `daily` to extract
   * @param nJitter amount of jitter to apply when choosing monthly close price.
   * @param minDaysPerMonth skip a month if it doesn't have at least this many daily values.
   * @return Sequence holding monthly data
   */
  public static Sequence dailyToMonthly(Sequence daily, int dim, int nJitter, int minDaysPerMonth)
  {
    final int N = daily.length();
    List<Integer> monthEndIndices = new ArrayList<>();

    Sequence monthly = new Sequence(daily.getName());
    boolean haveGoodMonth = false;
    boolean haveBadMonth = false;
    int i = 0;
    while (i < N) {
      FeatureVec v = daily.get(i);
      LocalDate date = TimeLib.ms2date(v.getTime());
      Month month = date.getMonth();
      FeatureVec m = new FeatureVec(NumMonthlyFields);
      m.fill(v.get(0)); // will seed monthly average/low/high and set open price
      int nDaysInMonth = 1;
      int j = i + 1;
      for (; j < N; ++j) {
        FeatureVec w = daily.get(j);
        date = TimeLib.ms2date(w.getTime());
        if (date.getMonth() != month) break; // new month
        double x = w.get(dim);

        m.set(MonthlyClose, x);
        m.set(MonthlyAverage, m.get(MonthlyAverage) + x); // accumulate sum here, will divide later
        if (x < m.get(MonthlyLow)) {
          m.set(MonthlyLow, x);
        }
        if (x > m.get(MonthlyHigh)) {
          m.set(MonthlyHigh, x);
        }
        m.set(MonthlyMisc, x);
        ++nDaysInMonth;
      }

      assert nDaysInMonth == (j - i);
      if (nDaysInMonth >= minDaysPerMonth) {
        if (haveBadMonth) {
          throw new IllegalArgumentException(String.format("Bad Month: %s", TimeLib.formatMonth(v.getTime())));
        }
        monthEndIndices.add(j - 1);
        m.set(MonthlyAverage, m.get(MonthlyAverage) / nDaysInMonth);
        double r = m.get(MonthlyClose) / m.get(MonthlyOpen);  // set return as a multiplier (1.04, not 4%).
        m.set(MonthlyReturn, r);
        date = TimeLib.ms2date(v.getTime()).withDayOfMonth(1);
        date = TimeLib.getClosestBusinessDay(date, false);
        monthly.addData(m, TimeLib.toMs(date));
        haveGoodMonth = true;
      } else if (haveGoodMonth) {
        haveBadMonth = true;
      }

      i = j; // skip to first index in new month
    }

    if (nJitter > 0) {
      assert monthEndIndices.size() == monthly.size();
      Random rng = new Random();
      int offset = -nJitter + rng.nextInt(nJitter * 2 + 1);
      for (i = 0; i < monthly.size(); ++i) {
        // int offset = -nJitter + rng.nextInt(nJitter * 2 + 1);
        assert offset >= -nJitter && offset <= nJitter;
        int x = monthEndIndices.get(i);
        int y = Math.min(Math.max(x + offset, 0), daily.length() - 1);
        monthly.get(i).set(MonthlyClose, daily.get(y, dim));
        monthEndIndices.set(i, y);
        // System.out.printf("%d: %d=[%s] -> [%s]\n", i, x, Library.formatDate(daily.getTimeMS(x)),
        // Library.formatDate(daily.getTimeMS(y)));
      }

      double alpha = 0.8;
      double ema = daily.get(0, dim);
      int iNext = 0;
      for (i = 0; i < daily.size() && iNext < monthEndIndices.size(); ++i) {
        ema = ema * alpha + daily.get(i, dim) * (1.0 - alpha);
        if (i == monthEndIndices.get(iNext)) {
          monthly.get(iNext).set(MonthlyMisc, ema);
          ema = daily.get(Math.min(i + 1, daily.length() - 1), dim);
          ++iNext;
        }
      }
    }

    return monthly;
  }

  /**
   * Converts a Sequence of daily or monthly cumulative returns to return per year (not cumulative).
   * 
   * @param returns Sequence of daily or monthly cumulative returns
   * @param dim dimension in `returns` to extract
   * @return Sequence holding return per year (1.4 = 1.4%)
   */
  public static Sequence toReturnPerYear(Sequence returns, int dim)
  {
    Sequence annual = new Sequence(returns.getName());

    int startYear = TimeLib.ms2date(returns.getStartMS()).getYear();
    int endYear = TimeLib.ms2date(returns.getEndMS()).getYear();
    int prevEnd = -1;
    for (int year = startYear; year <= endYear; ++year) {
      long jan1 = TimeLib.toMs(year, Month.JANUARY, 1);
      long dec31 = TimeLib.toMs(year, Month.DECEMBER, 31);
      int a = prevEnd; // ensure we don't skip any data
      if (a < 0) a = returns.getClosestIndex(jan1);
      int b = returns.getClosestIndex(dec31);
      prevEnd = b;
      double nMonths = TimeLib.monthsBetween(Math.max(returns.getTimeMS(a), jan1),
          Math.min(returns.getTimeMS(b), dec31));
      if (nMonths < 11.5) {
        // System.out.printf("Too short (%d): %.2f [%s] -> [%s]\n", year, nMonths,
        // TimeLib.formatDate(returns.getTimeMS(a)), TimeLib.formatDate(returns.getTimeMS(b)));
        continue;
      }

      // System.out.printf("[%s] -> [%s]\n", TimeLib.formatDate(returns.getTimeMS(a)),
      // TimeLib.formatDate(returns.getTimeMS(b)));
      double mul = FinLib.getTotalReturn(returns, a, b, dim);
      annual.addData(mul2ret(mul), jan1);
    }

    return annual;
  }

  public static Sequence pad(Sequence seq, Sequence ref, double value)
  {
    Sequence padded = new Sequence(seq.getName());

    // Pad front of sequence.
    if (seq.getStartMS() > ref.getStartMS()) {
      int offset = ref.getClosestIndex(seq.getStartMS());
      assert seq.getStartMS() == ref.getTimeMS(offset);
      Sequence prefix = new Sequence();
      for (int i = 0; i < offset; ++i) {
        prefix.addData(value, ref.getTimeMS(i));
      }
      padded.append(prefix);
    }

    // Copy sequence.
    padded.append(seq);

    // Pad back of sequence.
    if (seq.getEndMS() < ref.getEndMS()) {
      int offset = ref.getClosestIndex(seq.getEndMS());
      assert seq.getEndMS() == ref.getTimeMS(offset);
      Sequence suffix = new Sequence();
      for (int i = offset + 1; i < ref.length(); ++i) {
        suffix.addData(value, ref.getTimeMS(i));
      }
      padded.append(suffix);
    }

    assert padded.length() >= ref.length();
    assert padded.getStartMS() <= ref.getStartMS();
    assert padded.getEndMS() >= ref.getEndMS();

    return padded;
  }

  /**
   * Calculate growth of cash given interest rates
   * 
   * @param interestRates interest rates (annualized, 4.3=4.3%)
   * @return cumulative returns
   */
  public static Sequence calcInterestReturns(Sequence interestRates)
  {
    Sequence seq = new Sequence("cash");
    double balance = 1.0;
    seq.addData(balance, interestRates.getStartMS());
    for (int i = 1; i < interestRates.length(); ++i) {
      double annualRate = interestRates.get(i, 0);
      double annualMul = FinLib.ret2mul(annualRate);
      double monthlyRate = Math.pow(annualMul, Library.ONE_TWELFTH);
      balance *= monthlyRate;
      seq.addData(balance, interestRates.getTimeMS(i));
    }
    return seq;
  }

  public static String formatDollars(double x)
  {
    if (x < 1500.0) {
      return dollarFormatter.format(x);
    } else if (x < 100000.0) {
      return String.format("%.1fk", x / 1000.0);
    } else if (x < 1000000.0) {
      return String.format("%.0fk", x / 1000.0);
    } else {
      return String.format("%.1fm", x / 1000000.0);
    }
  }

  public static String buildMixedName(String[] names, int[] percents)
  {
    assert names.length == percents.length;

    StringBuilder sb = new StringBuilder(names[0]);
    for (int i = 1; i < names.length; ++i) {
      sb.append("/");
      sb.append(names[i]);
    }
    sb.append("-");
    sb.append(percents[0]);
    for (int i = 1; i < percents.length; ++i) {
      sb.append("/");
      sb.append(percents[i]);
    }
    return sb.toString();
  }

  public static boolean isLTG(LocalDate buyDate, LocalDate sellDate)
  {
    if (sellDate.isBefore(buyDate)) { // handle nonsense case of sell before buy
      return false;
    }

    Period period = Period.between(buyDate, sellDate);
    assert period.getMonths() < 12;
    if (period.getYears() < 1) return false;
    if (period.getYears() > 1) return true;
    assert period.getYears() == 1;

    // If sell date is Feb 29, must hold one extra day.
    int nMinDays = (sellDate.getMonth() == Month.FEBRUARY && sellDate.getDayOfMonth() == 29 ? 2 : 1);
    return period.getMonths() > 0 || period.getDays() >= nMinDays;
  }

  public static double portfolioDev(double[] w, double[] dev, double[][] corr)
  {
    final int n = w.length;
    assert corr.length == n;

    double v = 0.0;
    for (int i = 0; i < n; ++i) {
      if (Math.abs(w[i]) < 1e-8) continue;
      double ki = w[i] * dev[i];
      v += ki * ki;
      for (int j = i + 1; j < n; ++j) {
        v += 2.0 * ki * w[j] * dev[j] * corr[i][j];
      }
    }

    System.out.printf("v=%f\n", v);
    return Math.sqrt(v);
  }

  /** @return sharpe ratio for `returns` (1.2 = 1.2% growth) relative to `benchmark`, which can be null. */
  public static double sharpe(Sequence returns, Sequence benchmark)
  {
    if (returns == null || returns.isEmpty()) return 0.0;
    final int N = returns.length();
    assert benchmark == null || benchmark.length() == N;

    double[] excess = returns.extractDim(0);
    if (benchmark != null) {
      for (int i = 0; i < N; ++i) {
        excess[i] -= benchmark.get(i, 0);
      }
    }

    double sdev = Library.stdev(excess);
    return Math.abs(sdev) < 1e-8 ? 0.0 : Library.mean(excess) / sdev;
  }

  /**
   * Calculates Sharpe ratio with an adjustment to annualize daily returns.
   * 
   * This function uses a simple (and not very accurate) method for annualizing returns. More info here:
   * https://augmentedtrader.com/2015/09/02/why-multiply-by-sqrt252-to-compute-the-sharpe-ratio/
   * 
   * @return sharpe ratio for `returns` relative to `benchmark`, which can be null.
   */
  public static double sharpeDaily(Sequence dailyReturns, Sequence benchmark)
  {
    return Math.sqrt(252) * sharpe(dailyReturns, benchmark);
  }

  /**
   * Calculates Sharpe ratio with an adjustment to annualize monthly returns.
   * 
   * This function uses a simple (and not very accurate) method for annualizing returns. More info here:
   * https://augmentedtrader.com/2015/09/02/why-multiply-by-sqrt252-to-compute-the-sharpe-ratio/
   * 
   * @return sharpe ratio for `returns` relative to `benchmark`, which can be null.
   */
  public static double sharpeMonthly(Sequence monthlyReturns, Sequence benchmark)
  {
    return Math.sqrt(12) * sharpe(monthlyReturns, benchmark);
  }

  /** @return Sequence with average(t-ta, t-tb) values. */
  public static Sequence sma(Sequence seq, int ta, int tb, int iDim)
  {
    assert ta >= tb;
    Sequence sma = new Sequence(seq.getName() + "-sma");
    final int iLast = seq.length() - 1;
    for (int t = 0; t <= iLast; ++t) {
      final int iBaseA = Math.max(t - ta, 0);
      final int iBaseB = Math.max(t - tb, 0);
      double v = seq.average(iBaseA, iBaseB, iDim);
      sma.addData(v, seq.getTimeMS(t));
    }
    return sma;
  }

  /** In-place normalization so that seq[0][0] == 1.0. */
  public static Sequence normalizeReturns(Sequence seq)
  {
    return seq._div(seq.getFirst(0));
  }

  /** Takes a sequence of returns (1.2 = 1.2% growth) and returns sequence of cumulative returns. */
  public static Sequence cumulativeFromReturns(Sequence returnSeq, double principle, double contributionPerStep)
  {
    Sequence seq = new Sequence("Cumulative " + returnSeq.getName());
    seq.copyMeta(returnSeq);
    double balance = principle;

    seq.addData(balance, returnSeq.getStartMS());
    for (int i = 0; i < returnSeq.length(); ++i) {
      if (i > 0) balance += contributionPerStep; // no contribution at very beginning
      balance *= ret2mul(returnSeq.get(i, 0));

      long ms = TimeLib.TIME_ERROR;
      if (i + 1 < returnSeq.length()) {
        ms = returnSeq.getTimeMS(i + 1);
      } else {
        // TODO better handling for calculating time of last entry (past end of `returnSeq`).
        if (returnSeq.length() == 1) {
          ms = returnSeq.getEndMS() + TimeLib.MS_IN_DAY;
        } else {
          long a = returnSeq.getTimeMS(returnSeq.length() - 2);
          long b = returnSeq.getEndMS();
          ms = b + (b - a);
        }
      }
      seq.addData(new FeatureVec(1, balance), ms);
    }
    return seq._div(principle);
  }

  /** Takes a sequence of cumulative returns and returns a sequence of discrete returns (1.2 = 1.2% growth). */
  public static Sequence cumulativeToReturns(Sequence cumulativeReturns)
  {
    if (cumulativeReturns == null || cumulativeReturns.length() < 2) return null;

    Sequence seq = new Sequence("Returns " + cumulativeReturns.getName());
    seq.copyMeta(cumulativeReturns);

    for (int i = 1; i < cumulativeReturns.length(); ++i) {
      FeatureVec a = cumulativeReturns.get(i - 1);
      FeatureVec b = cumulativeReturns.get(i);
      double r = mul2ret(b.get(0) / a.get(0));
      seq.addData(new FeatureVec(1, r), a.getTime());
    }
    return seq;
  }
}
