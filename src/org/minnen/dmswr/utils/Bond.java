package org.minnen.dmswr.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.FinLib;

/**
 * Model a bond.
 * 
 * Information on bond modeling:
 * 
 * http://admainnew.morningstar.com/directhelp/Morningstar_UST_Constant_Maturity_Return_Methodology.pdf
 * 
 * https://en.wikipedia.org/wiki/Bond_valuation
 * 
 * https://dqydj.com/bond-pricing-calculator/
 * 
 * https://www.bogleheads.org/wiki/Bond_pricing
 * 
 * See the unit tests for examples and more references.
 */
public class Bond
{
  private final Sequence bondData;
  public final double    par;
  public final double    coupon;
  public final double    annualFreq;
  public final int       startIndex, endIndex;

  public enum DivOrPow {
    DivideBy12, TwelfthRoot
  }

  public Bond(Sequence bondData, double par, double coupon, double annualFreq, int startIndex, int endIndex)
  {
    assert par > 0.0;
    assert coupon >= 0.0;
    assert annualFreq >= 0.0;
    assert endIndex > startIndex;

    this.bondData = bondData;
    this.par = par;
    this.coupon = coupon;
    this.annualFreq = annualFreq;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  /**
   * Create a 10-year bond that pays semi-annually.
   * 
   * @param bondData interest rate data
   * @param par face value of bond
   * @param startIndex index into bondData when the bond is purchased
   */
  public Bond(Sequence bondData, double par, int startIndex)
  {
    this(bondData, par, par * bondData.get(startIndex, 0) / 100.0, 2, startIndex, startIndex + 120);
  }

  public boolean paysThisMonth(int index)
  {
    if (!isActive(index) || index == startIndex) {
      return false;
    }

    int diff = index - startIndex;
    int monthsBetweenPayments = getMonthsBetweenPayments();
    return (diff % monthsBetweenPayments) == 0;
  }

  public boolean isActive(int index)
  {
    return index >= startIndex && index <= endIndex;
  }

  public boolean isExpiring(int index)
  {
    return index == endIndex;
  }

  public double couponPayment()
  {
    if (coupon == 0.0 || annualFreq == 0.0) {
      return 0.0;
    } else {
      return coupon / annualFreq;
    }
  }

  public double paymentThisMonth(int index)
  {
    double couponPayment = (paysThisMonth(index) ? couponPayment() : 0.0);
    double parPayment = (index == endIndex ? par : 0.0);
    return couponPayment + parPayment;
  }

  /** @return number of months bond is active */
  public int duration()
  {
    return endIndex - startIndex;
  }

  public int getMonthsBetweenPayments()
  {
    if (annualFreq <= 0.0) {
      return duration();
    } else {
      return (int) Math.round(12.0 / annualFreq);
    }
  }

  public int getNextPaymentMonth(int index)
  {
    for (int i = index; i <= endIndex; ++i) {
      if (paysThisMonth(i)) {
        return i;
      }
    }
    return -1;
  }

  public double price(int index)
  {
    if (!isActive(index) || index == endIndex) {
      return 0.0;
    }

    double interestRate = bondData.get(index, 0);

    // Calculations for number of payments left.
    int nextPaymentIndex = getNextPaymentMonth(index);
    assert nextPaymentIndex >= index;
    int numMonthsToPayment = nextPaymentIndex - index;

    int monthsBetweenPayments = getMonthsBetweenPayments();
    int n = (int) Math.ceil((double) (endIndex - index) / monthsBetweenPayments);
    double years = annualFreq <= 0.0 ? numMonthsToPayment / 12.0 : n / annualFreq;

    // System.out.printf("| index=%d interest=%.3f nextPayment=%d monthsBetween=%d n=%d years=%f\n", index,
    // interestRate, nextPaymentIndex, monthsBetweenPayments, n, years);

    // Calculations for fractional interest.
    // TODO should calculate fraction based on *days* not months since that's how real pricing works and different
    // months have different numbers of days.
    double fractionalInterest = 0.0;
    assert numMonthsToPayment >= 0 && numMonthsToPayment <= monthsBetweenPayments;
    if (numMonthsToPayment > 0) {
      fractionalInterest = 1.0 - (double) numMonthsToPayment / monthsBetweenPayments;
    }

    double price = calcPrice(coupon, interestRate, par, years, annualFreq, fractionalInterest);
    // System.out.printf("| monthsToPayment=%d fractionalInterest=%f price=%.2f\n", numMonthsToPayment,
    // fractionalInterest, price);
    assert !Double.isNaN(price);
    return price;
  }

  /** Calculates the dirty price of a zero-coupon bond. */
  public static double calcPriceZeroCoupon(double annualInterestRate, double parValue, double years)
  {
    return calcPrice(0, annualInterestRate, parValue, years, 0, 0);
  }

  /**
   * Calculates the dirty price of a bond = clean price plus any accrued interest.
   * 
   * @param annualCoupon total interest payments for each year
   * @param annualInterestRate current interest rate
   * @param parValue amount paid at maturity
   * @param years years until maturity
   * @param paymentsPerYear number of times per year that the coupon is paid
   * @param fractionalInterest fraction of coupon payment period already passed [0..1)
   * @return current price of the bond
   */
  public static double calcPrice(double annualCoupon, double annualInterestRate, double parValue, double years,
      double paymentsPerYear, double fractionalInterest)
  {
    if (paymentsPerYear <= 0.0) {
      paymentsPerYear = 1.0;
    }
    final double nPayments = years * paymentsPerYear; // number of payments left
    final double couponPayment = annualCoupon / paymentsPerYear;
    final double effIR = (annualInterestRate / 100.0) / paymentsPerYear; // effective interest rate per payment

    final double valueFactor = Math.pow(1.0 + effIR, -nPayments); // value factor used for present value

    // Price due to payment at maturity is present value.
    final double maturityPrice = parValue * valueFactor;

    // Price due to the coupon payments is modeled as a fixed term annuity (present value of a fixed number of
    // payments). Details: https://en.wikipedia.org/wiki/Present_value#Present_value_of_an_annuity
    double couponPrice = 0.0;
    if (couponPayment > 0 && annualInterestRate > 0) { // check interest rate to avoid zero div zero.
      couponPrice = couponPayment * (1.0 - valueFactor) / effIR;
    }
    final double cleanPrice = couponPrice + maturityPrice;

    // If we're in between coupon payments, account for accrued interest since last coupon.
    // Details: http://www.economics-finance.org/jefe/fin/Secrestpaper.pdf
    // TODO also support simple (linear) accrued interest?
    double accruedInterest = 0.0;
    if (fractionalInterest > 0) {
      double futurePrice = calcPrice(annualCoupon, annualInterestRate, parValue, years - 1.0 / paymentsPerYear,
          paymentsPerYear, 0.0) + couponPayment;
      double priceRatio = futurePrice / cleanPrice;
      accruedInterest = cleanPrice * (Math.pow(priceRatio, fractionalInterest) - 1.0);
    }

    return cleanPrice + accruedInterest;
  }

  /**
   * Calculates bond ROI for the given range using the rebuy approach.
   * 
   * Each month, the existing bond is sold and a new one is purchased. Bonds are modeled as 10-year bonds that pay
   * semi-annually.
   * 
   * @param factory buys a particular kind of bond
   * @param bondData interest rates for bonds
   * @param iStart start simulation at this index in the data sequence (negative => count back from end of sequence)
   * @param iEnd end simulation at this index in the data sequence (negative => count back from end of sequence)
   * @return sequence of ROIs
   */
  public static Sequence calcReturnsRebuy(BondFactory factory, Sequence bondData, int iStart, int iEnd)
  {
    if (iStart < 0) {
      iStart += bondData.length();
    }
    if (iEnd < 0) {
      iEnd += bondData.length();
    }
    if (iStart < 0 || iEnd < iStart || iEnd >= bondData.size()) {
      throw new IllegalArgumentException(String.format("iStart=%d, iEnd=%d, size=%d", iStart, iEnd, bondData.size()));
    }

    final double principal = 1e6; // start with $1M to minimize effect of bond quantum
    double cash = principal;
    Sequence seq = new Sequence(factory.name() + " (Rebuy)");
    seq.addData(cash, bondData.getTimeMS(iStart));

    for (int i = iStart; i < iEnd; ++i) {
      // Buy bond at start of this month.
      BondFactory.Receipt receipt = factory.buy(bondData, cash, i);
      Bond bond = receipt.bond;
      cash = receipt.cash;

      // Sell bond at end of the month (we use start of next month).
      cash += bond.price(i + 1);

      // Add sequence data point for new month.
      seq.addData(cash, bondData.getTimeMS(i + 1));
    }

    return seq._div(principal);
  }

  /**
   * Calculates bond ROI for the given range using the hold-to-maturity approach.
   * 
   * All bonds are held to maturity and coupon payments are used to buy more bonds.
   * 
   * @param factory buys a particular kind of bond
   * @param bondData interest rates for bonds
   * @param iStart start simulation at this index in the data sequence (negative => count back from end of sequence)
   * @param iEnd end simulation at this index in the data sequence (negative => count back from end of sequence)
   * @return sequence of ROIs
   */
  public static Sequence calcReturnsHold(BondFactory factory, Sequence bondData, int iStart, int iEnd)
  {
    if (iStart < 0) {
      iStart += bondData.length();
    }
    if (iEnd < 0) {
      iEnd += bondData.length();
    }
    if (iStart < 0 || iEnd < iStart || iEnd >= bondData.size()) {
      throw new IllegalArgumentException(String.format("iStart=%d, iEnd=%d, size=%d", iStart, iEnd, bondData.size()));
    }

    final double principal = 1e6; // start with $1M to minimize effect of bond quantum
    double cash = principal;
    Sequence seq = new Sequence(factory.name() + " (Hold)");
    seq.addData(cash, bondData.getTimeMS(iStart));
    List<Bond> bonds = new ArrayList<Bond>(); // TODO not an efficient data structure

    for (int i = iStart; i < iEnd; ++i) {
      // Collect from existing bonds.
      Iterator<Bond> it = bonds.iterator();
      while (it.hasNext()) {
        Bond bond = it.next();
        cash += bond.paymentThisMonth(i);
        assert bond.isActive(i);
        if (bond.isExpiring(i)) {
          it.remove();
        }
      }

      // Buy new bonds.
      BondFactory.Receipt receipt = factory.buy(bondData, cash, i);
      cash = receipt.cash;
      if (receipt.bond != null) {
        bonds.add(receipt.bond);
      }

      // Add sequence data point for new month.
      double value = cash;
      for (Bond bond : bonds) {
        value += bond.price(i);
      }
      seq.addData(value, bondData.getTimeMS(i + 1));
    }
    return seq._div(principal);
  }

  /**
   * Calculates bond ROI for the given range assuming the interest is paid directly.
   * 
   * Each month, we assume we earn interestRate / 12 percent. Note that this approach is NOT correct.
   * 
   * @param factory buys a particular kind of bond
   * @param bondData interest rates for bonds
   * @param iStart start simulation at this index in the data sequence (negative => count back from end of sequence)
   * @param iEnd end simulation at this index in the data sequence (negative => count back from end of sequence)
   * @param divOrPow calculate monthly rate by dividing annual rate by 12 or taking twelfth root.
   * @return sequence of ROIs
   */
  public static Sequence calcReturnsNaiveInterest(BondFactory factory, Sequence bondData, int iStart, int iEnd,
      DivOrPow divOrPow)
  {
    if (iStart < 0) {
      iStart += bondData.length();
    }
    if (iEnd < 0) {
      iEnd += bondData.length();
    }
    if (iStart < 0 || iEnd < iStart || iEnd >= bondData.size()) {
      throw new IllegalArgumentException(String.format("iStart=%d, iEnd=%d, size=%d", iStart, iEnd, bondData.size()));
    }

    final double principal = 1e6; // start with $1M to minimize effect of bond quantum
    double balance = principal;
    String name = String.format("%s (Naive Interest - %s)", factory.name(),
        divOrPow == DivOrPow.DivideBy12 ? "Div12" : "Pow12");
    Sequence seq = new Sequence(name);
    seq.addData(balance, bondData.getTimeMS(iStart));

    for (int i = iStart; i < iEnd; ++i) {
      double growth;
      if (divOrPow == DivOrPow.DivideBy12) {
        growth = bondData.get(i, 0) / 12.0;
      } else {
        double r = FinLib.ret2mul(bondData.get(i, 0));
        r = Math.pow(r, 1.0 / 12.0); // r^(1/12) is more accurate to hit annual rate
        growth = FinLib.mul2ret(r);
      }
      balance *= FinLib.ret2mul(growth);
      seq.addData(balance, bondData.getTimeMS(i + 1));
    }

    return seq._div(principal);
  }

  public static Sequence calcBondReturnsYTM(Sequence bondData)
  {
    // More info: https://courses.lumenlearning.com/boundless-finance/chapter/valuing-bonds/
    Sequence seq = new Sequence("Bonds (YTM)");
    seq.addData(1.0, bondData.getStartMS());

    final double par = 100.0; // exact value doesn't matter since it divides out
    for (int i = 1; i < bondData.size(); ++i) {
      final double prevYTM = bondData.get(i - 1, 0);
      final double curYTM = bondData.get(i, 0);

      final double purchase = FinLib.getPresentValue(par, prevYTM, 10);
      final double sell = FinLib.getPresentValue(par, curYTM, 10 - 1.0 / 12.0);

      final double r = sell / purchase;
      final double cumulative = seq.getLast(0) * r;
      seq.addData(cumulative, bondData.getTimeMS(i));
    }
    return seq;
  }
}
