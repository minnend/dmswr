package org.minnen.dmswr.tests;

import java.time.LocalDate;
import java.time.Month;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.minnen.dmswr.utils.Sequence;
import org.minnen.dmswr.utils.TimeLib;

@RunWith(Suite.class)
@SuiteClasses({ //
    TestBond.class, //
    TestFinLib.class, //
    TestLibrary.class, //
    TestSequence.class, //
    TestSwrLib.class, //
    TestTimeLib.class, //
})

public class AllTests
{
  public static Sequence buildMonthlySequence(double[] data)
  {
    return buildMonthlySequence("test", data);
  }

  public static Sequence buildMonthlySequence(String name, double[] data)
  {
    Sequence seq = new Sequence(name);
    LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);
    for (double x : data) {
      seq.addData(x, TimeLib.toMs(date));
      date = date.plusMonths(1);
    }
    return seq;
  }
}
