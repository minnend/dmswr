package org.minnen.dmswr.tests;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;
import org.minnen.dmswr.utils.Sequence;

public class TestSequence
{
  final double eps = 1e-8;

  @Test
  public void testMinMax()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isEmpty());
    assertEquals(4, seq.size());
    assertEquals(4, seq.length());
    assertEquals(1, seq.getNumDims());
    assertFalse(seq.isLocked());
    assertEquals("test", seq.getName());
    assertEquals(1, seq.getMin().get(0), eps);
    assertEquals(4, seq.getMax().get(0), eps);
  }

  @Test
  public void testSubseq()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });

    Sequence subseq = seq.subseq(2);
    assertEquals(2, subseq.length());
    assertArrayEquals(new double[] { 4, 3 }, subseq.extractDim(0), eps);

    subseq = seq.subseq(1, 2);
    assertEquals(2, subseq.length());
    assertArrayEquals(new double[] { 1, 4 }, subseq.extractDim(0), eps);

    final long key = 1234;
    seq.lock(1, 3, key);
    subseq = seq.subseq(1, 2);
    assertEquals(2, subseq.length());
    assertArrayEquals(new double[] { 4, 3 }, subseq.extractDim(0), eps);
  }

  @Test
  public void testExtractDim()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });

    double[] a = seq.extractDim(0);
    assertEquals(4, a.length);
    assertArrayEquals(new double[] { 2, 1, 4, 3 }, a, eps);

    a = seq.extractDim(0, 2);
    assertEquals(2, a.length);
    assertArrayEquals(new double[] { 4, 3 }, a, eps);

    a = seq.extractDim(0, 1, 2);
    assertEquals(2, a.length);
    assertArrayEquals(new double[] { 1, 4 }, a, eps);

    final long key = 1234;
    seq.lock(1, 3, key);
    a = seq.extractDim(0, 1, 2);
    assertEquals(2, a.length);
    assertArrayEquals(new double[] { 4, 3 }, a, eps);
  }

  @Test
  public void testLock()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isLocked());
    final long key = 1234;
    seq.lock(0, 3, key);
    assertTrue(seq.isLocked());
    assertEquals(4, seq.length());
    assertEquals(1, seq.get(1, 0), eps);
    assertEquals(3, seq.get(3, 0), eps);

    seq.lock(1, 2, key);
    assertTrue(seq.isLocked());
    assertEquals(2, seq.length());
    assertEquals(1, seq.get(0, 0), eps);
    assertEquals(4, seq.get(1, 0), eps);
  }

  @Test
  public void testMultipleLocks()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isLocked());
    final long key = 1234;

    // Three locks.
    seq.lock(0, 3, key);
    assertTrue(seq.isLocked());

    seq.lock(0, 2, key);
    assertTrue(seq.isLocked());

    seq.lock(0, 1, key);
    assertTrue(seq.isLocked());

    // Unlock all three.
    seq.unlock(key);
    assertTrue(seq.isLocked());

    seq.unlock(key);
    assertTrue(seq.isLocked());

    seq.unlock(key);
    assertFalse(seq.isLocked());
  }

  @Test(expected = RuntimeException.class)
  public void testSecondLockOutOfBounds()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isLocked());
    final long key1 = 1234;
    seq.lock(1, 2, key1);
    assertTrue(seq.isLocked());
    assertEquals(2, seq.length());
    assertEquals(1, seq.get(0, 0), eps);
    assertEquals(4, seq.get(1, 0), eps);

    final long key2 = 5678;
    seq.lock(0, 3, key2);
  }

  @Test(expected = RuntimeException.class)
  public void testLockBadKey()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isLocked());

    final long key = 1234;
    final long badKey = 4321;
    assertNotEquals(key, badKey);

    seq.lock(0, 3, key);
    assertTrue(seq.isLocked());
    seq.unlock(badKey);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testBeforeLockViolation()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isLocked());
    final long key = 1234;
    seq.lock(1, 2, key);
    assertTrue(seq.isLocked());
    assertEquals(2, seq.length());
    seq.get(-10);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testAfterLockViolation()
  {
    Sequence seq = new Sequence("test", new double[] { 2, 1, 4, 3 });
    assertFalse(seq.isLocked());
    final long key = 1234;
    seq.lock(0, 2, key);
    assertTrue(seq.isLocked());
    assertEquals(3, seq.length());
    seq.get(3);
  }

  @Test
  public void testAverage()
  {
    Sequence seq = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0 });

    assertEquals(1.0, seq.average(0, 0).get(0), eps);
    assertEquals(1.5, seq.average(0, 1).get(0), eps);
    assertEquals(2.0, seq.average(0, 2).get(0), eps);
    assertEquals(2.5, seq.average(0, 3).get(0), eps);

    assertEquals(3.5, seq.average(2, 3).get(0), eps);
    assertEquals(4.0, seq.average(3, 3).get(0), eps);
  }

  @Test
  public void testAverageDim()
  {
    Sequence seq = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0 });

    assertEquals(1.0, seq.average(0, 0, 0), eps);
    assertEquals(1.5, seq.average(0, 1, 0), eps);
    assertEquals(2.0, seq.average(0, 2, 0), eps);
    assertEquals(2.5, seq.average(0, 3, 0), eps);

    assertEquals(3.5, seq.average(2, 3, 0), eps);
    assertEquals(4.0, seq.average(3, 3, 0), eps);
  }

  @Test
  public void testIntegral()
  {
    Sequence seq = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0 });
    Sequence integral = seq.getIntegralSeq();
    assert integral.matches(seq);

    assertEquals(1.0, integral.get(0, 0), eps);
    assertEquals(3.0, integral.get(1, 0), eps);
    assertEquals(6.0, integral.get(2, 0), eps);
    assertEquals(10.0, integral.get(3, 0), eps);
  }

  @Test
  public void testIntegralSum()
  {
    Sequence seq = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0 });
    Sequence integral = seq.getIntegralSeq();
    assert integral.matches(seq);

    assertEquals(3.0, integral.integralSum(2, -2), eps);
    assertEquals(10.0, integral.integralSum(0, -1), eps);
    assertEquals(10.0, integral.integralSum(-1, -1), eps);
    assertEquals(1.0, integral.integralSum(-1, 0), eps);
    assertEquals(7.0, integral.integralSum(2, 3), eps);
    assertEquals(3.0, integral.integralSum(2, 2), eps);
    assertEquals(3.0, integral.integralSum(-5, 1), eps);

    assertThrows(AssertionError.class, () -> {
      integral.integralSum(3, 2);
    });
  }

  @Test
  public void testIntegralAverage()
  {
    Sequence seq = new Sequence(new double[] { 1.0, 2.0, 3.0, 4.0 });
    Sequence integral = seq.getIntegralSeq();
    assert integral.matches(seq);

    assertEquals(3.0, integral.integralAverage(2, -2), eps);
    assertEquals(2.5, integral.integralAverage(0, -1), eps);
    assertEquals(2.5, integral.integralAverage(-1, -1), eps);
    assertEquals(1.0, integral.integralAverage(-1, 0), eps);
    assertEquals(3.5, integral.integralAverage(2, 3), eps);
    assertEquals(3.0, integral.integralAverage(2, 2), eps);
    assertEquals(1.5, integral.integralAverage(-5, 1), eps);

    assertThrows(AssertionError.class, () -> {
      integral.integralAverage(3, 2);
    });
  }
}
