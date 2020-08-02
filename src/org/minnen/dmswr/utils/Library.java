package org.minnen.dmswr.utils;

import java.util.*;
import java.util.function.Predicate;

public final class Library
{
  public final static long    LNAN         = Long.MIN_VALUE;
  public final static int     INDEX_ERROR  = Integer.MIN_VALUE;

  public final static double  FPMIN        = Double.MIN_VALUE;
  public final static double  INF          = Double.POSITIVE_INFINITY;
  public final static double  NEGINF       = Double.NEGATIVE_INFINITY;

  /** log(0.0) = -infinity */
  public final static double  LOG_ZERO     = NEGINF;

  /** log(1.0) = 0.0 */
  public final static double  LOG_ONE      = 0.0;

  public final static double  LOG_TWO      = Math.log(2.0);
  public final static double  MINV_ABS     = 1.0e-9;
  public final static double  TWO_PI       = 2.0 * Math.PI;
  public final static double  PI_OVER_TWO  = Math.PI / 2.0;
  public final static double  SQRT_2PI     = Math.sqrt(TWO_PI);
  public final static double  SQRT_2       = Math.sqrt(2.0);
  public static final double  ONE_TWELFTH  = 1.0 / 12.0;

  public final static long    AppStartTime = TimeLib.getTime();
  public static final String  os           = System.getProperty("os.name");
  public static final boolean bWindows     = os.startsWith("Win");

  public static final Random  rng          = new Random();

  public static void copy(double from[], double[] to)
  {
    int n = Math.min(from.length, to.length);
    System.arraycopy(from, 0, to, 0, n);
  }

  /** Return an array that is the concatenation of the input arrays. */
  public static double[] concat(double[] a, double... b)
  {
    double[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  /** Return an array that is the concatenation of the input arrays. */
  public static <T> T[] concat(T[] a, T[] b)
  {
    T[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  /**
   * Try to parse the string as a double.
   * 
   * @param s string to parse
   * @param failValue return this value if parse fails
   * @return numeric value of s or failValue if parsing fails
   */
  public static double tryParse(String s, double failValue)
  {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException nfe) {
      return failValue;
    }
  }

  /** @return array of length n with a[i] == i */
  public static int[] genIdentityArray(int n)
  {
    int[] a = new int[n];
    for (int i = 0; i < n; i++) {
      a[i] = i;
    }
    return a;
  }

  /** @return array of length n with values [0,n-1] in random order. */
  public static int[] shuffle(int n)
  {
    int[] a = genIdentityArray(n);
    for (int i = a.length - 1; i > 0; i--) {
      int index = rng.nextInt(i + 1);
      int t = a[index];
      a[index] = a[i];
      a[i] = t;
    }
    return a;
  }

  /**
   * Sort the given array and return the resulting indices
   * 
   * @param a data to sort
   * @param bAscending true = ascending sort, false = descending
   * @return index of original location of data
   */
  public static int[] sort(double[] a, boolean bAscending)
  {
    int n = a.length;
    int[] ii = genIdentityArray(n);
    sort(a, ii, 0, n - 1);
    if (!bAscending) {
      for (int i = 0; i < n - i - 1; ++i) {
        swap(a, ii, i, n - i - 1);
      }
    }
    return ii;
  }

  /**
   * Internal insertion sort routine for subarrays that is used by quicksort.
   * 
   * @param a an array of Comparable items.
   * @param ii array of indices that will be rearranged to match sort
   * @param low the left-most index of the subarray.
   * @param n the number of items to sort.
   */
  private static void sort(double[] a, int[] ii, int low, int high)
  {
    for (int p = low + 1; p <= high; p++) {
      double tmp = a[p];
      int j;
      for (j = p; j > low && tmp < a[j - 1]; j--) {
        a[j] = a[j - 1];
        ii[j] = ii[j - 1];
      }
      a[j] = tmp;
      ii[j] = p;
    }
  }

  /**
   * Sort the given array and return the resulting indices
   * 
   * @param a data to sort
   * @param bAscending true = ascending sort, false = descending
   * @return index of original location of data
   */
  public static int[] sort(int[] a, boolean bAscending)
  {
    int n = a.length;
    int[] ii = genIdentityArray(n);
    sort(a, ii, 0, n - 1);
    if (!bAscending) {
      for (int i = 0; i < n - i - 1; ++i) {
        swap(a, ii, i, n - i - 1);
      }
    }
    return ii;
  }

  /**
   * Internal insertion sort routine for subarrays that is used by quicksort.
   * 
   * @param a an array of Comparable items.
   * @param ii array of indices that will be rearranged to match sort
   * @param low the left-most index of the subarray.
   * @param n the number of items to sort.
   */
  private static void sort(int[] a, int[] ii, int low, int high)
  {
    for (int p = low + 1; p <= high; p++) {
      int tmp = a[p];
      int j;
      for (j = p; j > low && tmp < a[j - 1]; j--) {
        a[j] = a[j - 1];
        ii[j] = ii[j - 1];
      }
      a[j] = tmp;
      ii[j] = p;
    }
  }

  /**
   * Method to swap to elements in an array.
   * 
   * @param a an array of doubles.
   * @param ii array of indices that will be rearranged to match sort
   * @param index1 the index of the first object.
   * @param index2 the index of the second object.
   */
  public static void swap(double[] a, int[] ii, int index1, int index2)
  {
    double tmp = a[index1];
    a[index1] = a[index2];
    a[index2] = tmp;

    int itmp = ii[index1];
    ii[index1] = ii[index2];
    ii[index2] = itmp;
  }

  /**
   * Method to swap to elements in an array.
   * 
   * @param a an array of ints.
   * @param ii array of indices that will be rearranged to match sort
   * @param index1 the index of the first object.
   * @param index2 the index of the second object.
   */
  public static void swap(int[] a, int[] ii, int index1, int index2)
  {
    int tmp = a[index1];
    a[index1] = a[index2];
    a[index2] = tmp;

    int itmp = ii[index1];
    ii[index1] = ii[index2];
    ii[index2] = itmp;
  }

  /** Reorder the elements of a according to the indices of ii. */
  public static void reorder(double[] a, int[] ii)
  {
    double[] b = Arrays.copyOf(a, a.length);
    for (int i = 0; i < a.length; ++i) {
      b[i] = a[ii[i]];
    }
    System.arraycopy(b, 0, a, 0, a.length);
  }

  /**
   * Sort the given array and return the resulting indices
   * 
   * @param a data to sort
   * @param bAscending true = ascending sort, false = descending
   * @return index of original location of data
   */
  public static <T extends Comparable<T>> int[] sort(T[] a, boolean bAscending)
  {
    int n = a.length;
    int[] ii = genIdentityArray(n);
    sort(a, ii, 0, n - 1);
    if (!bAscending) {
      for (int i = 0; i < n - i - 1; ++i) {
        swap(a, ii, i, n - i - 1);
      }
    }
    return ii;
  }

  /**
   * Internal insertion sort routine for subarrays that is used by quicksort.
   * 
   * @param a an array of Comparable items.
   * @param ii array of indices that will be rearranged to match sort
   * @param low the left-most index of the subarray.
   * @param n the number of items to sort.
   */
  private static <T extends Comparable<T>> void sort(T[] a, int[] ii, int low, int high)
  {
    for (int p = low + 1; p <= high; p++) {
      T tmp = a[p];
      int j;
      for (j = p; j > low && tmp.compareTo(a[j - 1]) < 0; j--) {
        a[j] = a[j - 1];
        ii[j] = ii[j - 1];
      }
      a[j] = tmp;
      ii[j] = p;
    }
  }

  /**
   * Method to swap to elements in an array.
   * 
   * @param a an array of objects.
   * @param ii array of indices that will be rearranged to match sort
   * @param index1 the index of the first object.
   * @param index2 the index of the second object.
   */
  public static <T extends Comparable<T>> void swap(T[] a, int[] ii, int index1, int index2)
  {
    T tmp = a[index1];
    a[index1] = a[index2];
    a[index2] = tmp;

    int itmp = ii[index1];
    ii[index1] = ii[index2];
    ii[index2] = itmp;
  }

  /** @return minimum of the values in the given array. */
  public static int min(int... a)
  {
    return min(a, 0, a.length - 1);
  }

  /** @return minimum of the values in [iStart, iEnd] for the given array. */
  public static int min(int[] a, int iStart, int iEnd)
  {
    assert iStart <= iEnd;
    int min = a[iStart];
    for (int i = iStart + 1; i <= iEnd; ++i) {
      if (a[i] < min) min = a[i];
    }
    return min;
  }

  /** @return minimum of the values in the given array. */
  public static double min(double... a)
  {
    return min(a, 0, a.length - 1);
  }

  /** @return minimum of the values in [iStart, iEnd] for the given array. */
  public static double min(double[] a, int iStart, int iEnd)
  {
    assert iStart <= iEnd;
    double min = a[iStart];
    for (int i = iStart + 1; i <= iEnd; ++i) {
      if (a[i] < min) min = a[i];
    }
    return min;
  }

  /** @return maximum of the values in the given array. */
  public static double max(double... a)
  {
    return max(a, 0, a.length - 1);
  }

  /** @return maximum of the values in [iStart, iEnd] for the given array. */
  public static double max(double[] a, int iStart, int iEnd)
  {
    assert iStart <= iEnd;
    double max = a[iStart];
    for (int i = iStart + 1; i <= iEnd; ++i) {
      if (a[i] > max) max = a[i];
    }
    return max;
  }

  /** @return arithmetic mean (average) of the values in the given array. */
  public static double mean(double... a)
  {
    if (a == null || a.length == 0) return Double.NaN;
    return sum(a) / a.length;
  }

  /** @return geometry mean of the values in the given array. */
  public static double geometricMean(double... a)
  {
    if (a == null || a.length == 0) return Double.NaN;
    double sum = 0.0;
    for (double x : a) {
      sum += Math.log(x);
    }
    return Math.exp(sum / a.length);
  }

  public static double variance(double... a)
  {
    if (a.length < 2) {
      return 0.0;
    }

    double mean = mean(a);
    double s1 = 0.0, s2 = 0.0;
    for (int i = 0; i < a.length; ++i) {
      double diff = a[i] - mean;
      s1 += diff * diff;
      s2 += diff;
    }
    return (s1 - s2 * s2 / a.length) / (a.length - 1);
  }

  public static double stdev(double... a)
  {
    return Math.sqrt(variance(a));
  }

  public static double[][] correlation(double[][] r)
  {
    final int n = r.length;
    double[][] cm = new double[n][n];
    for (int i = 0; i < n; ++i) {
      cm[i][i] = 1.0;
      for (int j = i + 1; j < n; ++j) {
        double p = correlation(r[i], r[j]);
        assert !Double.isNaN(p);
        cm[i][j] = cm[j][i] = p;
      }
    }
    return cm;
  }

  public static double[] cov2dev(double[][] cov)
  {
    final int n = cov.length;
    double[] dev = new double[n];
    for (int i = 0; i < n; ++i) {
      dev[i] = Math.sqrt(cov[i][i]);
    }
    return dev;
  }

  public static double correlation(double[] a, double[] b)
  {
    assert a.length == b.length;

    double sa = stdev(a);
    double sb = stdev(b);
    if (Math.abs(sa) < 1e-8 || Math.abs(sb) < 1e-8) return 0.0;

    double cov = covariance(a, b);
    return cov / (sa * sb);
  }

  public static double[][] covariance(double[][] r)
  {
    final int n = r.length;
    double[][] cm = new double[n][n];
    for (int i = 0; i < n; ++i) {
      cm[i][i] = variance(r[i]);
      for (int j = i + 1; j < n; ++j) {
        double p = covariance(r[i], r[j]);
        assert !Double.isNaN(p);
        cm[i][j] = cm[j][i] = p;
      }
    }
    return cm;
  }

  public static double covariance(double[] a, double[] b)
  {
    assert a.length == b.length;

    double ma = mean(a);
    double mb = mean(b);

    double sum = 0.0;
    for (int i = 0; i < a.length; ++i) {
      double da = a[i] - ma;
      double db = b[i] - mb;
      sum += da * db;
    }

    return sum / (a.length - 1);
  }

  /** Return the NxN identity matrix. */
  public static double[][] eye(int n)
  {
    double[][] m = new double[n][n];
    for (int i = 0; i < n; ++i) {
      m[i][i] = 1.0;
    }
    return m;
  }

  public static int sum(int[] a)
  {
    return sum(a, 0, -1);
  }

  public static int sum(int[] a, int iStart, int iEnd)
  {
    if (iStart < 0) {
      iStart += a.length;
    }
    if (iEnd < 0) {
      iEnd += a.length;
    }
    int sum = 0;
    for (int i = iStart; i <= iEnd; ++i) {
      sum += a[i];
    }
    return sum;
  }

  public static long sum(long[] a)
  {
    return sum(a, 0, -1);
  }

  public static long sum(long[] a, int iStart, int iEnd)
  {
    if (iStart < 0) {
      iStart += a.length;
    }
    if (iEnd < 0) {
      iEnd += a.length;
    }
    long sum = 0;
    for (int i = iStart; i <= iEnd; ++i) {
      sum += a[i];
    }
    return sum;
  }

  public static double sum(double[] a)
  {
    return sum(a, 0, -1);
  }

  /** @return sum of values in [iStart, iEnd]. */
  public static double sum(double[] a, int iStart, int iEnd)
  {
    if (iStart < 0) {
      iStart += a.length;
    }
    if (iEnd < 0) {
      iEnd += a.length;
    }
    double sum = 0;
    for (int i = iStart; i <= iEnd; ++i) {
      sum += a[i];
    }
    return sum;
  }

  public static int argmax(double[] a)
  {
    int iMax = 0;
    for (int i = 1; i < a.length; ++i) {
      if (a[i] > a[iMax]) {
        iMax = i;
      }
    }
    return iMax;
  }

  /** @return index of largest value if it is largest by `margin` or more, else return -1. */
  public static int argmax(double[] a, double margin)
  {
    int iMax = argmax(a);
    for (int i = 0; i < a.length; ++i) {
      if (i == iMax) continue;
      if (a[iMax] - a[i] < margin) return -1;
    }
    return iMax;
  }

  public static String prefix(String s, String marker)
  {
    int n = s.indexOf(marker);
    if (n < 0) {
      return s;
    } else {
      return s.substring(0, n);
    }
  }

  public static boolean almostEqual(double a, double b, double eps)
  {
    if (Double.isNaN(a) || Double.isNaN(b)) {
      return (Double.isNaN(a) && Double.isNaN(b)); // equal if both vars are NaN
    }
    return (Math.abs(b - a) < eps);
  }

  public static boolean almostEqual(double[] a, double[] b, double eps)
  {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; ++i) {
      if (!almostEqual(a[i], b[i], eps)) return false;
    }
    return true;
  }

  public static int numBits(long x)
  {
    x = x - ((x >>> 1) & 0x55555555);
    x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
    return (int) ((((x + (x >>> 4)) & 0x0F0F0F0F) * 0x01010101) >>> 24);
  }

  public static int numBits(int x)
  {
    x = x - ((x >>> 1) & 0x55555555);
    x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
    return (((x + (x >>> 4)) & 0x0F0F0F0F) * 0x01010101) >>> 24;
  }

  /** @return 1.0 / (1 + e^[-(x - x0) / sigma]) */
  public static double sigmoid(double x, double sigma, double x0)
  {
    double t = (x - x0) / sigma;
    double y = 1.0 + Math.exp(-t);
    return 1.0 / y;
  }

  public static List<? extends Object> removeNulls(List<? extends Object> list)
  {
    list.removeIf(new Predicate<Object>()
    {
      @Override
      public boolean test(Object v)
      {
        return v == null;
      }
    });
    return list;
  }
}
