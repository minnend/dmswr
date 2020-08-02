package org.minnen.dmswr.tests;

import org.junit.Test;
import org.minnen.dmswr.utils.SwrLib;

import junit.framework.TestCase;

public class TestSwrLib extends TestCase
{
  @Override
  protected void setUp() throws Exception
  {
    SwrLib.setupWithDefaultFiles();
  }

  @Test
  public void testGrowthInverse()
  {
    for (int percentStock = 0; percentStock <= 100; percentStock += 5) {
      double a = SwrLib.growth(0, -1, percentStock);
      double b = SwrLib.growth(-1, 0, percentStock);
      double x = a * b;
      assertEquals(1.0, x, 1e-6);
    }
  }

  @Test
  public void testGrowthCumulative()
  {
    double a = SwrLib.growth(0, -1, 70);
    double x = 1.0;
    for (int i = 0; i < SwrLib.length(); ++i) {
      x *= SwrLib.growth(i, 70);
    }
    assertEquals(a, x, 1e-6);
  }
}
