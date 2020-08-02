package org.minnen.dmswr.tests;

import static org.junit.Assert.*;

import org.junit.Test;
import org.minnen.dmswr.utils.Bond;
import org.minnen.dmswr.utils.FinLib;

public class TestBond
{
  @Test
  public void testCalcPrice()
  {
    // source: https://faculty.unlv.edu/msullivan/Chapter%207.pdf
    double tol = 0.0051; // site gives answer to nearest penny
    double p = Bond.calcPrice(98.75, 7, 1000, 10, 1, 0);
    assertEquals(p, 1201.93, tol);

    p = Bond.calcPrice(98.75, 9, 1000, 10, 1, 0);
    assertEquals(p, 1056.15, tol);

    p = Bond.calcPrice(98.75, 11, 1000, 10, 1, 0);
    assertEquals(p, 933.75, tol);

    p = Bond.calcPrice(145, 11, 1000, 16, 2, 0);
    assertEquals(p, 1260.82, tol);

    p = Bond.calcPrice(0, 9, 1000, 30, 0, 0);
    assertEquals(p, 75.37, tol);

    // source: http://users.wfu.edu/palmitar/Law&Valuation/chapter%204/4-2-2.htm
    p = Bond.calcPrice(86, 8, 1000, 10, 1, 0);
    assertEquals(p, 1040.26, tol);

    p = Bond.calcPrice(86, 8, 1000, 10, 2, 0);
    assertEquals(p, 1040.77, tol);

    // source: http://www.economics-finance.org/jefe/fin/Secrestpaper.pdf
    p = Bond.calcPrice(120, 8, 1000, 2, 2, 72.0 / 182.0);
    assertEquals(p, 1089.37, 6e-2);

    p = Bond.calcPrice(120, 8, 1000, 2, 2, 0);
    assertEquals(p, 1072.5979, 6e-5);

    p = Bond.calcPrice(120, 8, 1000, 1.5, 2, 0); // just after first of four coupon payments
    assertEquals(p, 1055.5018, 6e-5);

    // source: http://accountingexplained.com/financial/lt-liabilities/bond-price
    tol = 0.51; // site only gives quote to nearest dollar
    p = Bond.calcPrice(8000, 10, 100000, 10, 1, 0);
    assertEquals(p, 87711, tol);

    p = Bond.calcPrice(9000, 8, 100000, 10, 2, 0);
    assertEquals(p, 106795, tol);

    // source: https://www.csie.ntu.edu.tw/~lyuu/finance1/2009/20090225.pdf
    p = Bond.calcPrice(0, 8.0, 100.0, 20, 2, 0);
    assertEquals(p, 20.83, tol);

    p = Bond.calcPrice(0, 8.0, 100.0, 10, 2, 0);
    assertEquals(p, 45.64, tol);

    p = Bond.calcPrice(10, 15, 100, 10, 2, 0);
    assertEquals(p, 74.5138, 6e-5);

    // source: https://xplaind.com/910710/dirty-price
    p = Bond.calcPrice(32.5, 0.89, 1000, 2, 2, 0); // clean price at previous coupon
    assertEquals(p, 1046.68, 6e-3);

    p = Bond.calcPrice(32.5, 0.89, 1000, 2, 2, 169.0 / 180.0); // dirty price in between
    assertEquals(p, 1051.05, 6e-3);

    // source: http://www.tvmcalcs.com/index.php/calculators/apps/excel_bond_valuation
    p = Bond.calcPrice(80, 9.5, 1000, 3, 2, 0);
    assertEquals(p, 961.63, 6e-3);
  }

  @Test
  public void testCalcPriceZeroCoupon()
  {
    // source: https://financeformulas.net/Zero_Coupon_Bond_Value.html
    double p = Bond.calcPriceZeroCoupon(6, 100, 5);
    assertEquals(74.73, p, 6e-3);
    assertEquals(FinLib.getPresentValue(100, 6, 5), p, 1e-6);

    p = Bond.calcPriceZeroCoupon(5, 100, 5);
    assertEquals(78.35, p, 6e-3);
    assertEquals(FinLib.getPresentValue(100, 5, 5), p, 1e-6);

    // Dirty prices.
    p = Bond.calcPriceZeroCoupon(5, 100, 9.5);
    assertEquals(FinLib.getPresentValue(100, 5, 9.5), p, 1e-6);
  }
}
