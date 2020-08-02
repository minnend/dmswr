package org.minnen.dmswr.utils;

import org.minnen.dmswr.utils.IntPair;

/** Holds a start and end index. */
public class IndexRange extends IntPair
{
  public IndexRange(int iStart, int iEnd)
  {
    super(iStart, iEnd);
  }

  public int length()
  {
    return second - first + 1;
  }

  @Override
  public String toString()
  {
    return String.format("[%d,%d]", first, second);
  }
}
