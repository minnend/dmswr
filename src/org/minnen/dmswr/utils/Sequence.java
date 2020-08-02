package org.minnen.dmswr.utils;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.minnen.dmswr.utils.FeatureVec;
import org.minnen.dmswr.utils.Random;
import org.minnen.dmswr.utils.TimeLib;

/**
 * A Sequence represents a list of multidimensional feature vectors.
 * 
 * Each sequence has a specific frequency and the absolute time of every data point can be computed.
 */
public class Sequence extends MetaStore implements Iterable<FeatureVec>
{
  /** Stores information about a single lock position. */
  public static class Lock
  {
    public static final Random rng = new Random();

    /** First index of locked region (real, inclusive). */
    public final int           iStart;

    /** Last index of locked region (real, inclusive). */
    public final int           iEnd;

    /** Key used to lock region; must be specified to unlock. */
    public final long          key;

    public final int           iPrevEnd;

    public Lock(int iStart, int iEnd, long key)
    {
      this(iStart, iEnd, -1, key);
    }

    public Lock(int iStart, int iEnd, int iPrevEnd, long key)
    {
      this.iStart = iStart;
      this.iEnd = iEnd;
      this.iPrevEnd = iPrevEnd;
      this.key = key;
    }

    public static long genKey()
    {
      return rng.nextLong();
    }
  }

  /** Used when adjusting dates, e.g. move to last day of month or last business day of month? */
  public enum LastDay {
    BUSINESS_DAY, ANY_DAY
  }

  /** Data stored in this data set. */
  private final List<FeatureVec> data  = new ArrayList<>();

  /** Name of this sequence. */
  private String                 name;

  /** Locks applied to this sequence. */
  private final Stack<Lock>      locks = new Stack<>();

  public List<String>            dimNames;
  public Map<String, Integer>    name2dim;

  /**
   * Defines behavior when searching for an index matching a given time.
   * 
   * <ul>
   * <li>Closest = closest index, either before after the given time.
   * <li>Inside = given a time range, first must be at or after and second must be at or before the first / last time.
   * <li>Outisde = given a time range, first must be at or before and second must be at or after the first / last time.
   * <li>AtOrAfter = closest index at or after the given time.
   * <li>AtOrBefore = closest index at or before the given time.
   * </ul>
   */
  public enum EndpointBehavior {
    Closest, Inside, Outside, AtOrAfter, AtOrBefore
  }

  /**
   * Create an empty, unnamed sequence.
   */
  public Sequence()
  {}

  /**
   * Create a named sequence.
   * 
   * @param name name of this sequence
   */
  public Sequence(String name)
  {
    setName(name);
  }

  /**
   * Creates a new 1D sequence with the given name that contains the specified data.
   * 
   * @param name - name of the new sequence
   * @param data - data for the sequence
   */
  public Sequence(String name, double data[])
  {
    this(name);
    for (int i = 0; i < data.length; i++)
      addData(new FeatureVec(1, data[i]));
  }

  /**
   * Creates a new 1D sequence with the given name that contains the specified data.
   * 
   * @param data - data for the sequence
   */
  public Sequence(double data[])
  {
    this(null, data);
  }

  public Sequence(List<FeatureVec> data)
  {
    this.data.addAll(data);
  }

  public String getName()
  {
    return name;
  }

  public Sequence setName(String name)
  {
    this.name = name;
    return this;
  }

  public String getDateRangeString()
  {
    return String.format("[%s] -> [%s]", TimeLib.formatDate(getStartMS()), TimeLib.formatDate(getEndMS()));
  }

  @Override
  public String toString()
  {
    return String.format("[Seq: len=%d, %dD  [%s]->[%s]]", length(), getNumDims(), TimeLib.formatDate(getStartMS()),
        TimeLib.formatDate(getEndMS()));
  }

  /**
   * Return the dimensionality of this data set. We assume that the first feature vector has the correct dimensionality.
   * Return 0 if no data is loaded.
   */
  public int getNumDims()
  {
    if (data.isEmpty()) return 0;
    return get(0).getNumDims();
  }

  /** Set the dimension names for this sequence (note: no error checking & existing values are replaced). */
  public Sequence setDimNames(List<String> names)
  {
    dimNames = names;
    if (names == null) {
      name2dim = null;
      return this;
    }

    if (name2dim == null) name2dim = new HashMap<String, Integer>();
    else name2dim.clear();
    for (int i = 0; i < names.size(); ++i) {
      name2dim.put(names.get(i), i);
    }
    return this;
  }

  /** @return list of dimension names. */
  public List<String> getDimNames()
  {
    return dimNames;
  }

  /** Index of the dimension with the given `name`. */
  public int getDim(String name)
  {
    return name2dim.getOrDefault(name, -1);
  }

  /** @return name of the i'th dimension. */
  public String getDimName(int dim)
  {
    return dimNames.get(dim);
  }

  /**
   * Lock this sequence using real indices (i.e. indices that ignore existing locks).
   * 
   * @param iStartReal start index of the new lock
   * @param iEndReal end index of the new lock
   * @param iPrevEnd end index of previous lock (or full sequence if no locks)
   * @param key key value required to unlock this sequence.
   * @return this sequence.
   */
  private Sequence lockReal(int iStartReal, int iEndReal, int iPrevEnd, long key)
  {
    assert iStartReal >= 0;
    assert iEndReal < data.size();
    if (isLocked()) {
      Lock lock = locks.peek();
      assert iStartReal >= lock.iStart;
      assert iEndReal <= lock.iEnd;
    }

    Lock lock = new Lock(iStartReal, iEndReal, iPrevEnd, key);
    locks.push(lock);
    return this;
  }

  /**
   * Lock this sequence so that only elements in [iStart, iEnd] can be access.
   * 
   * The typical motivation for locking a sequence is ensuring against bugs that allow a strategy to "look in to the
   * future". When simulating a particular point in time, you can lock the sequence to points in the past [0, t-1] and
   * protect against accidental cheating.
   * 
   * @param iStart first index that can be accessed (inclusive)
   * @param iEnd last index that can be accessed (inclusive)
   * @return key to unlock this sequence.
   */
  public Sequence lock(int iStart, int iEnd, long key)
  {
    if (iStart < 0 || iStart >= length()) {
      throw new IndexOutOfBoundsException(String.format("Start[%s]: %d vs [0, %d]", name, iStart, length() - 1));
    }
    if (iEnd < 0 || iEnd >= length()) {
      throw new IndexOutOfBoundsException(String.format("End[%s]: %d vs [0, %d]", name, iEnd, length() - 1));
    }
    if (iStart > iEnd) {
      throw new IndexOutOfBoundsException(String.format("Start after End (%d vs %d)", iStart, iEnd));
    }

    // Locks store real indices so adjust from relative to real.
    iStart = adjustIndex(iStart);
    iEnd = adjustIndex(iEnd);
    return lockReal(iStart, iEnd, -1, key);
  }

  /** Replace the existing lock (if there is one) with the given range. */
  public Sequence relock(long startMs, long endMs, EndpointBehavior endpointBehavior, long key)
  {
    int iPrevEnd = -1;
    if (isLocked(key)) {
      iPrevEnd = locks.peek().iEnd;
      unlock(key); // Verify key and remove last lock
    }

    IndexRange range = getIndices(startMs, endMs, endpointBehavior);
    return lockReal(range.first, range.second, iPrevEnd, key);
  }

  /**
   * Lock this sequence to match the bounds of the given sequences.
   * 
   * @return key to unlock this sequence.
   */
  public Sequence lockToMatch(Sequence seq, long key)
  {
    int iStart = getClosestIndex(seq.getStartMS());
    int iEnd = getClosestIndex(seq.getEndMS());
    assert iEnd - iStart + 1 == seq.length();
    return lock(iStart, iEnd, key);
  }

  /**
   * Unlock this sequence.
   * 
   * @param key key used to unlock the sequence (must match key used to lock it).
   * @return this sequence
   */
  public Sequence unlock(long key)
  {
    Lock lock = locks.peek();
    if (key == lock.key) {
      locks.pop();
      return this;
    } else {
      throw new RuntimeException(String.format("Tried to unlock sequence with wrong key (%s).", name));
    }
  }

  /** @return True if this sequence is locked. */
  public boolean isLocked()
  {
    return !locks.isEmpty();
  }

  /** @return True if the most recent lock uses the given key. */
  public boolean isLocked(long key)
  {
    if (locks.isEmpty()) return false;
    return locks.peek().key == key;
  }

  public List<FeatureVec> getData()
  {
    return data;
  }

  /** @return First real (internal) index that respects lock. */
  private int getFirstIndex()
  {
    if (locks.isEmpty()) {
      return 0;
    } else {
      return locks.peek().iStart;
    }
  }

  /** @return Last real (internal) index that respects lock. */
  private int getLastIndex()
  {
    if (locks.isEmpty()) {
      return data.size() - 1;
    } else {
      return locks.peek().iEnd;
    }
  }

  /**
   * Convert locked index to real index.
   * 
   * @return index adjusted to respect current lock boundaries.
   */
  private int adjustIndex(int i)
  {
    int iStart = getFirstIndex();
    if (i < 0 || i >= length()) {
      int iEnd = getLastIndex();
      throw new IndexOutOfBoundsException(String.format("%d vs. [%d, %d]", i, iStart, iEnd));
    }
    return iStart + i;
  }

  /** @return length of this sequence */
  public int size()
  {
    return getLastIndex() - getFirstIndex() + 1;
  }

  /** @return length of this sequence */
  public int length()
  {
    return size();
  }

  /** @return i^th feature vector */
  public FeatureVec get(int i)
  {
    if (i < 0) {
      i += length();
    }
    i = adjustIndex(i);
    return data.get(i);
  }

  /** @return value of the d^th dimension in the i^th feature vector */
  public double get(int i, int d)
  {
    return get(i).get(d);
  }

  /** set the i^th feature vector */
  public void set(int i, FeatureVec fv)
  {
    i = adjustIndex(i);
    data.set(i, fv);
  }

  /** set the d^th dimension in the i^th feature vector */
  public void set(int i, int d, double x)
  {
    get(i).set(d, x);
  }

  /** @return first feature vector in this sequence. */
  public FeatureVec getFirst()
  {
    return get(0);
  }

  /** @return value of given dimension of first feature vector in this sequence. */
  public double getFirst(int d)
  {
    return getFirst().get(d);
  }

  /** @return last feature vector in this sequence. */
  public FeatureVec getLast()
  {
    return get(length() - 1);
  }

  /** @return value of given dimension of last feature vector in this sequence. */
  public double getLast(int d)
  {
    return getLast().get(d);
  }

  /** @return FeatureVec with minimum value for each dimension. */
  public FeatureVec getMin()
  {
    if (isEmpty()) {
      return null;
    }
    FeatureVec v = new FeatureVec(getNumDims());
    v.copyFrom(get(0));
    for (int i = 1; i < length(); ++i) {
      v._min(get(i));
    }
    return v;
  }

  /** @return index of the max value for the given dimension. */
  public int argmax(int iDim)
  {
    if (isEmpty()) return -1;

    int iMax = 0;
    double vmax = get(0, iDim);

    for (int i = 1; i < length(); ++i) {
      double v = get(i, iDim);
      if (v > vmax) {
        vmax = v;
        iMax = i;
      }
    }
    return iMax;
  }

  /** @return FeatureVec with maximum value for each dimension. */
  public FeatureVec getMax()
  {
    if (isEmpty()) {
      return null;
    }
    FeatureVec v = new FeatureVec(getNumDims());
    v.copyFrom(get(0));
    for (int i = 1; i < length(); ++i) {
      v._max(get(i));
    }
    return v;
  }

  /** @return start time (time of first sample in ms) of this sequence */
  public long getStartMS()
  {
    if (isEmpty()) {
      return TimeLib.TIME_ERROR;
    } else {
      return getFirst().getTime();
    }
  }

  /** @return end time (time of last sample in ms) of this sequence */
  public long getEndMS()
  {
    if (isEmpty()) {
      return TimeLib.TIME_ERROR;
    } else {
      return getLast().getTime();
    }
  }

  /** @return length of this sequence in milliseconds */
  public long getLengthMS()
  {
    return getEndMS() - getStartMS();
  }

  /** @return length of this sequence in fractional months */
  public double getLengthMonths()
  {
    return TimeLib.monthsBetween(getStartMS(), getEndMS());
  }

  /** @return average time between data points (in ms) */
  public long getMeanTimeStep()
  {
    if (length() == 0) return 0;
    return getLengthMS() / (length() - 1);
  }

  /** @return time in ms of the given data frame */
  public long getTimeMS(int i)
  {
    return get(i).getTime();
  }

  public void setTime(int i, long ms)
  {
    get(i).setTime(ms);
  }

  /** @return true if this data set has no data */
  public boolean isEmpty()
  {
    return data.isEmpty();
  }

  /**
   * Append the vectors in the given seq to each vector in this sequence.
   * 
   * @param seq holds vectors to append
   * @return this Sequence
   */
  public Sequence _appendDims(Sequence seq)
  {
    assert !isLocked();
    assert length() == seq.length();
    for (int i = 0; i < length(); ++i) {
      get(i)._appendDims(seq.get(i));
    }
    return this;
  }

  public int addData(FeatureVec value)
  {
    assert (value != null);
    data.add(value);
    return data.size() - 1;
  }

  /** add a time stamped feature vector to the end of this sequence */
  public int addData(FeatureVec value, long ms)
  {
    int ix = addData(value);
    setTime(ix, ms);
    return ix;
  }

  /** add a time stamped feature vector to the end of this sequence */
  public int addData(FeatureVec value, LocalDate date)
  {
    return addData(value, TimeLib.toMs(date));
  }

  /**
   * Add (append) a value to this data set.
   * 
   * @param value x to add
   * @return index where fv was placed
   */
  public int addData(double value)
  {
    assert isEmpty() || getNumDims() == 1;
    data.add(new FeatureVec(1, value));
    return data.size() - 1;
  }

  /** add a time stamped feature vector to the end of this sequence */
  public int addData(double x, long ms)
  {
    int ix = addData(x);
    setTime(ix, ms);
    return ix;
  }

  /** add a time stamped feature vector to the end of this sequence */
  public int addData(double x, LocalDate date)
  {
    return addData(x, TimeLib.toMs(date));
  }

  /**
   * Add `x` to each element of each frame.
   * 
   * @param x value to add
   * @return this data set
   */
  public Sequence _add(double x)
  {
    for (FeatureVec fv : data)
      fv._add(x);
    return this;
  }

  /**
   * Subtract `x` to each element of each frame.
   * 
   * @param x value to subtract
   * @return this data set
   */
  public Sequence _sub(double x)
  {
    for (FeatureVec fv : data)
      fv._sub(x);
    return this;
  }

  /**
   * Multiply each element of each frame by the given value.
   * 
   * @param x value to multiply by
   * @return this data set
   */
  public Sequence _mul(double x)
  {
    for (FeatureVec fv : data)
      fv._mul(x);
    return this;
  }

  /**
   * Multiply each element of each frame by the given vector.
   * 
   * @param v vector to multiply with (dimensions must match)
   * @return this data set
   */
  public Sequence _mul(FeatureVec v)
  {
    for (FeatureVec fv : data)
      fv._mul(v);
    return this;
  }

  /**
   * Divide each element of each frame by the given value.
   * 
   * @param x value to divide by
   * @return this data set
   */
  public Sequence _div(double x)
  {
    for (FeatureVec fv : data)
      fv._div(x);
    return this;
  }

  public Sequence div(double x)
  {
    return dup()._div(x);
  }

  /** In-place natural logarithm. */
  public Sequence _log()
  {
    for (FeatureVec fv : data) {
      fv._log();
    }
    return this;
  }

  /**
   * @return index of the data point closest to the given time without respecting lock boundaries.
   */
  private int getClosestRealIndex(long ms)
  {

    int n = data.size();
    if (n == 0) return -1;
    int a = 0;
    long ta = data.get(a).getTime();
    int b = n - 1;
    long tb = data.get(b).getTime();
    if (ms <= ta) return a;
    if (ms >= tb) return b;
    while (a + 1 < b) {
      int m = (a + b) / 2;
      long tm = data.get(m).getTime();
      if (tm == ms) return m;
      if (ms < tm) b = m;
      else a = m;
    }

    long da = Math.abs(ms - data.get(a).getTime());
    long dap1 = (a + 1 < n ? Math.abs(ms - data.get(a + 1).getTime()) : Long.MAX_VALUE);
    if (da <= dap1) return a;
    else return a + 1;
  }

  /**
   * @return index of the data point closest to the given time considering lock boundaries.
   */
  public int getClosestIndex(long ms)
  {
    int index = getClosestRealIndex(ms);
    index = Math.max(index, getFirstIndex());
    index = Math.min(index, getLastIndex());
    return index - getFirstIndex();
  }

  /**
   * @return index of the data point with the exact time; -1 if none found.
   */
  public int getIndexAt(long ms)
  {
    int index = getClosestIndex(ms);
    return (ms == getTimeMS(index) ? index : -1);
  }

  /**
   * @return index of the data point at or before the given time (if possible).
   */
  public int getIndexAtOrBefore(long ms)
  {
    int i = -1;

    // Heuristic check since a typical use case is incremental locking.
    if (isLocked()) {
      int iPrevLockEnd = locks.peek().iPrevEnd;
      if (iPrevLockEnd >= 0) {
        long t1 = data.get(iPrevLockEnd).getTime();
        long t2 = iPrevLockEnd + 1 < data.size() ? data.get(iPrevLockEnd + 1).getTime() : TimeLib.TIME_END;
        long t3 = iPrevLockEnd + 2 < data.size() ? data.get(iPrevLockEnd + 2).getTime() : TimeLib.TIME_END;
        if (ms >= t1 && ms <= t3) {
          if (ms == t3) {
            return iPrevLockEnd + 2;
          } else if (t2 <= ms) {
            return iPrevLockEnd + 1;
          } else {
            assert t1 <= ms;
            return iPrevLockEnd;
          }
        }
      }
    }

    // If the heuristic failed, search for the correct index.
    if (i < 0) {
      i = getClosestIndex(ms);
      if (i >= 0 && get(i).getTime() > ms) {
        --i;
      }
    }

    assert i < 0 || getTimeMS(i) <= ms;
    assert i + 1 >= length() || getTimeMS(i + 1) > ms;
    return i;
  }

  /**
   * @return index of the data point at or before the given time (if possible).
   */
  public int getIndexAtOrAfter(long ms)
  {
    int i = getClosestIndex(ms);
    if (i < length() && get(i).getTime() < ms) {
      ++i;
    }
    if (i >= length()) {
      i = -1;
    }
    assert i < 0 || getTimeMS(i) >= ms;
    return i;
  }

  public int getIndexForTime(long ms, EndpointBehavior endpointBehavior)
  {
    if (endpointBehavior == EndpointBehavior.Closest) {
      return getClosestIndex(ms);
    } else if (endpointBehavior == EndpointBehavior.AtOrBefore) {
      return getIndexAtOrBefore(ms);
    } else if (endpointBehavior == EndpointBehavior.AtOrAfter) {
      return getIndexAtOrAfter(ms);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  /** Add all data from the given list to the end of this sequence. */
  public Sequence append(List<FeatureVec> a)
  {
    data.addAll(a);
    return this;
  }

  /** Add all data from the given sequence to the end of this sequence. */
  public Sequence append(Sequence seq)
  {
    data.addAll(seq.data);
    return this;
  }

  /** Add all data from the given sequence to the beginning of this sequence. */
  public Sequence prepend(Sequence seq)
  {
    data.addAll(0, seq.data);
    return this;
  }

  @Override
  public Iterator<FeatureVec> iterator()
  {
    return data.iterator();
  }

  /**
   * Extract a subset (suffix) from the given dimension and return it in an array.
   * 
   * @param iDim - index of the dimension from which to extract
   * @param iStart - first index in the subset
   * @return double array representing the subseq
   */
  public double[] extractDim(int iDim, int iStart)
  {
    return extractDim(iDim, iStart, length() - iStart);
  }

  /**
   * Extract a subset from the given dimension and return it in an array.
   * 
   * @param iDim - index of the dimension from which to extract
   * @param iStart - first index in the subset
   * @param len - length of subset to extract
   * @return double array representing the subseq
   */
  public double[] extractDim(int iDim, int iStart, int len)
  {
    double[] ret = new double[len];
    for (int i = 0; i < len; i++)
      ret[i] = get(i + iStart, iDim);
    return ret;
  }

  /**
   * Extract the given dimension and return it in an array.
   * 
   * @param iDim - index of the dimension to retrieve
   * @return the given dimension as a double array
   */
  public double[] extractDim(int iDim)
  {
    return extractDim(iDim, 0, length());
  }

  /**
   * Returns a new dataset comprised of all time steps in this dataset but with different dimensions. The new dataset
   * will have one dimension corresponding to each (zero-based) element in dims.
   * 
   * Thus, to select the first and third dimensions from a 3D dataset: Sequence data2D = data3D.selectDims(new
   * int[]{0,2});
   */
  public Sequence extractDimAsSeq(int dim)
  {
    Sequence ret = new Sequence(getName());
    final int n = length();
    for (int i = 0; i < n; i++)
      ret.addData(get(i, dim), getTimeMS(i));
    return ret;
  }

  /**
   * Returns a new dataset comprised of all time steps in this dataset but with different dimensions. The new dataset
   * will have one dimension corresponding to each (zero-based) element in dims.
   * 
   * Thus, to select the first and third dimensions from a 3D dataset: Sequence data2D = data3D.selectDims(new
   * int[]{0,2});
   */
  public Sequence extractDims(int... dims)
  {
    Sequence ret = new Sequence(getName());
    final int n = length();
    for (int i = 0; i < n; i++)
      ret.addData(get(i).selectDims(dims));
    return ret;
  }

  /** @return closest index in data sequence for the given year and month (January == 1). */
  public int getIndexForDate(int year, int month)
  {
    long ms = TimeLib.toMs(year, month, 1);
    return getClosestIndex(ms);
  }

  /** @return subsequence starting at index iStart through end of the sequence. */
  public Sequence subseq(int iStart)
  {
    return subseq(iStart, length() - iStart);
  }

  /** @return subsequence with numElements starting at index iStart. */
  public Sequence subseq(int iStart, int numElements)
  {
    final int N = length();
    if (iStart < 0) {
      iStart += N;
    }
    if (numElements < 0) {
      numElements += (N - iStart) + 1;
    }
    assert iStart >= 0 && numElements > 0;
    Sequence seq = new Sequence(name);
    for (int i = 0; i < numElements; ++i) {
      seq.addData(get(iStart + i));
    }
    assert seq.length() == numElements;
    return seq;
  }

  /** @return new sequence with times matching `ref` and data from this sequence. */
  public Sequence matchTimes(Sequence ref, long eps)
  {
    Sequence seq = new Sequence(name);
    for (FeatureVec x : ref) {
      int i = this.getClosestIndex(x.getTime());
      long delta = Math.abs(x.getTime() - this.getTimeMS(i));
      if (delta <= eps) {
        seq.addData(this.get(i).dup());
      }
    }
    return seq;
  }

  public IndexRange getIndices(long startMs, long endMs, EndpointBehavior endpointBehavior)
  {
    assert startMs <= endMs;
    int i, j;
    if (endpointBehavior == EndpointBehavior.Closest) {
      i = getClosestIndex(startMs);
      j = getClosestIndex(endMs);
    } else if (endpointBehavior == EndpointBehavior.Inside) {
      i = getIndexAtOrAfter(startMs);
      j = getIndexAtOrBefore(endMs);
    } else if (endpointBehavior == EndpointBehavior.Outside) {
      i = getIndexAtOrBefore(startMs);
      j = getIndexAtOrAfter(endMs);
    } else if (endpointBehavior == EndpointBehavior.AtOrBefore) {
      i = getIndexAtOrBefore(startMs);
      j = getIndexAtOrBefore(endMs);
    } else {
      assert endpointBehavior == EndpointBehavior.AtOrAfter;
      i = getIndexAtOrAfter(startMs);
      j = getIndexAtOrAfter(endMs);
    }
    if (i < 0 || j < 0) return null;
    assert i <= j : String.format("Indices: %d, %d", i, j);
    return new IndexRange(i, j);
  }

  /** @return subsequence based on start/end times. */
  public Sequence subseq(long startMs, long endMs, EndpointBehavior endpointBehavior)
  {
    IndexRange range = getIndices(startMs, endMs, endpointBehavior);
    return subseq(range.first, range.length());
  }

  /** @return subsequence that does not extend beyond start/end times. */
  public Sequence subseq(long startMs, long endMs)
  {
    return subseq(startMs, endMs, EndpointBehavior.Inside);
  }

  /** @return new sequence equal to this sequence added (summed with) the given sequence. */
  public Sequence add(Sequence seq)
  {
    assert length() == seq.length();
    Sequence ret = new Sequence(getName() + " + " + seq.getName());
    for (int i = 0; i < length(); ++i) {
      FeatureVec x = get(i);
      FeatureVec y = seq.get(i);
      ret.addData(x.add(y), getTimeMS(i));
    }
    return ret;
  }

  /** @return new sequence equal to given sequence subtracted from this sequence. */
  public Sequence sub(Sequence seq)
  {
    assert length() == seq.length() : String.format("%d vs. %d", length(), seq.length());
    Sequence ret = new Sequence(getName() + " - " + seq.getName());
    for (int i = 0; i < length(); ++i) {
      FeatureVec x = get(i);
      FeatureVec y = seq.get(i);
      ret.addData(x.sub(y), getTimeMS(i));
    }
    return ret;
  }

  /** @return new sequence equal to given sequence multiplied with this sequence (component-wise). */
  public Sequence mul(Sequence seq)
  {
    assert length() == seq.length();
    Sequence ret = new Sequence(getName() + " - " + seq.getName());
    for (int i = 0; i < length(); ++i) {
      FeatureVec x = get(i);
      FeatureVec y = seq.get(i);
      ret.addData(x.mul(y), getTimeMS(i));
    }
    return ret;
  }

  /** @return new sequence equal to this sequence divided by the given sequence (component-wise). */
  public Sequence div(Sequence divisor)
  {
    assert length() == divisor.length();
    Sequence seq = new Sequence(getName() + " / " + divisor.getName());
    for (int i = 0; i < length(); ++i) {
      FeatureVec x = get(i);
      FeatureVec y = divisor.get(i);
      seq.addData(x.div(y), getTimeMS(i));
    }
    return seq;
  }

  /** @return this sequence after replacing all values with max between this and other sequence (component-wise). */
  public Sequence _max(Sequence seq)
  {
    assert length() == seq.length();
    for (int i = 0; i < length(); ++i) {
      FeatureVec x = get(i);
      FeatureVec y = seq.get(i);
      x._max(y);
    }
    return seq;
  }

  /**
   * Calculate the average in [iStart, iEnd] for the given sequence.
   * 
   * @param iStart first index (inclusive)
   * @param iEnd last index (inclusive)
   * @return vector of averages in [iStart, iEnd]
   */
  public FeatureVec average(int iStart, int iEnd)
  {
    if (iStart < 0) {
      iStart += length();
    }
    assert iStart >= 0;

    if (iEnd < 0) {
      iEnd += length();
    }
    assert iEnd >= 0;

    final int N = iEnd - iStart + 1;
    assert N > 0;

    FeatureVec average = new FeatureVec(getNumDims());
    for (int j = iStart; j <= iEnd; ++j) {
      average._add(get(j));
    }
    return average._div(N);
  }

  /**
   * Calculate the average in [iStart, iEnd] for the given sequence.
   * 
   * @param iStart first index (inclusive)
   * @param iEnd last index (inclusive)
   * @param iDim index of dimension over which to calculate the average
   * @return vector of averages in [iStart, iEnd]
   */
  public double average(int iStart, int iEnd, int iDim)
  {
    if (iStart < 0) {
      iStart += length();
    }
    assert iStart >= 0;

    if (iEnd < 0) {
      iEnd += length();
    }
    assert iEnd >= 0;

    final int N = iEnd - iStart + 1;
    assert N > 0;

    double sum = 0.0;
    for (int i = iStart; i <= iEnd; ++i) {
      sum += get(i, iDim);
    }
    return sum / N;
  }

  /** Reverse elements of this sequence (in-place). */
  public void reverse()
  {
    final int N = length();
    for (int i = 0;; ++i) {
      int j = N - i - 1;
      if (i >= j) {
        break;
      }
      FeatureVec tmp = data.get(i);
      data.set(i, data.get(j));
      data.set(j, tmp);
    }
  }

  /**
   * Duplicate this sequence.
   * 
   * @return a deep copy of this sequence.
   */
  public Sequence dup()
  {
    Sequence seq = new Sequence(getName());
    for (FeatureVec v : data) {
      seq.addData(new FeatureVec(v));
    }
    seq.locks.addAll(locks);
    return seq;
  }

  /**
   * @return true if the length and time bounds match.
   */
  public boolean matches(Sequence other)
  {
    return (other != null && other.length() == length() && other.getStartMS() == getStartMS()
        && other.getEndMS() == getEndMS());
  }

  /** @return true if length and individual timestamps match. */
  public boolean sameTimestamps(Sequence other)
  {
    if (other == null) return false;
    if (length() != other.length()) return false;
    for (int i = 0; i < other.length(); ++i) {
      if (getTimeMS(i) != other.getTimeMS(i)) return false;
    }
    return true;
  }

  /** @return Sequence holding difference between neighboring elements of this sequences. */
  public Sequence derivative()
  {
    Sequence deriv = new Sequence(getName() + "- Derivative");
    FeatureVec prev = get(0);
    for (int i = 1; i < length(); ++i) {
      FeatureVec cur = get(i);
      FeatureVec diff = cur.sub(prev);
      deriv.addData(diff, prev.getTime());
      prev = cur;
    }
    return deriv;
  }

  /** @return Sequence holding ratio between neighboring elements of this sequences ("multiplicative derivative"). */
  public Sequence derivativeMul()
  {
    Sequence deriv = new Sequence(getName() + "- Multiplicative-Derivative");
    FeatureVec prev = get(0);
    for (int i = 1; i < length(); ++i) {
      FeatureVec cur = get(i);
      FeatureVec growth = cur.div(prev);
      deriv.addData(growth, prev.getTime());
      prev = cur;
    }
    return deriv;
  }

  /** In-place adjustment of date to end of month (either calendar or last business day). */
  public Sequence adjustDatesToEndOfMonth(LastDay lastDay)
  {
    for (FeatureVec v : data) {
      LocalDate date = TimeLib.ms2date(v.getTime());
      if (lastDay == LastDay.BUSINESS_DAY) {
        date = TimeLib.toLastBusinessDayOfMonth(date);
      } else {
        assert lastDay == LastDay.ANY_DAY;
        date = date.with(TemporalAdjusters.lastDayOfMonth());
      }
      v.setTime(TimeLib.toMs(date));
    }
    return this;
  }

  /** Change all timestamps to the last day of the month. */
  public void adjustDatesToEndOfMonth()
  {
    adjustDatesToEndOfMonth(LastDay.ANY_DAY);
  }

  public void adjustDatesToEndOfQuarter(LastDay lastDay)
  {
    // Adjust dates forward two months (from first to last month of quarter).
    for (FeatureVec v : data) {
      LocalDate date = TimeLib.ms2date(v.getTime());
      assert date.getDayOfMonth() == 1;
      final Month month = date.getMonth();
      assert month == Month.JANUARY || month == Month.APRIL || month == Month.JULY || month == Month.OCTOBER;
      date = date.plusMonths(2);
      v.setTime(TimeLib.toMs(date));
    }
    adjustDatesToEndOfMonth(lastDay);
  }

  /** Change all timestamps to the last business day of the month. */
  public void adjustDatesToEndOfQuarter()
  {
    adjustDatesToEndOfQuarter(LastDay.ANY_DAY);
  }

  /** An integral sequence holds the sum over [0, t] for each time step t. */
  public Sequence getIntegralSeq()
  {
    Sequence seq = new Sequence(name + "-integral");
    if (length() > 0) {
      FeatureVec sum = new FeatureVec(getNumDims());
      for (FeatureVec v : data) {
        sum = sum.add(v);
        seq.addData(sum, v.getTime());
      }
    }
    assert seq.matches(this);
    return seq;
  }

  /**
   * Calculate the sum in [iStart, iEnd] assuming this is an integral sequence for dim=0.
   */
  public double integralSum(int iStart, int iEnd)
  {
    return integralSum(iStart, iEnd, 0);
  }

  /**
   * Calculate the sum in [iStart, iEnd] assuming this is an integral sequence.
   * 
   * Use getIntegralSeq() to calculate an integral sequence from a raw sequence. The integral sequence holds the
   * cumulative sum over the base sequence.
   * 
   * @param iStart first index (inclusive); negative indices are NOT adjusted.
   * @param iEnd last index (inclusive); negative indices will be adjusted relative to end of sequence.
   * @param iDim index of dimension over which to calculate the sum
   * @return sum of values in [iStart, iEnd] assuming this is an integral sequence.
   */
  public double integralSum(int iStart, int iEnd, int iDim)
  {
    if (iEnd < 0) {
      iEnd += length();
    }
    assert iStart <= iEnd;

    double sum = get(iEnd, iDim);
    if (iStart > 0) {
      sum -= get(iStart - 1, iDim);
    }

    return sum;
  }

  /**
   * Calculate the average over [iStart, iEnd] assuming this is an integral sequence for dim=0.
   */
  public double integralAverage(int iStart, int iEnd)
  {
    return integralAverage(iStart, iEnd, 0);
  }

  /**
   * Calculate the average over [iStart, iEnd] assuming this is an integral sequence.
   * 
   * Use getIntegralSeq() to calculate an integral sequence from a raw sequence. The integral sequence holds the
   * cumulative sum over the base sequence.
   * 
   * @param iStart first index (inclusive); negative indices are NOT adjusted.
   * @param iEnd last index (inclusive); negative indices will be adjusted relative to end of sequence.
   * @param iDim index of dimension over which to calculate the average
   * @return average of values in [iStart, iEnd] assuming this is an integral sequence.
   */
  public double integralAverage(int iStart, int iEnd, int iDim)
  {
    if (iEnd < 0) {
      iEnd += length();
    }
    final double sum = integralSum(iStart, iEnd, iDim);
    final int n = iEnd - Math.max(iStart, 0) + 1;
    return sum / n;
  }

  /** @return sequence holding a simple moving average for this sequence. */
  public Sequence calcSMA(int nBack)
  {
    Sequence seq = new Sequence(String.format("%s [SMA:%d]", getName(), nBack));
    for (int i = 0; i < length(); ++i) {
      int j = Math.max(0, i - nBack);
      seq.addData(average(j, i), getTimeMS(i));
    }
    return seq;
  }

  public static Comparator<Sequence> getStartDateComparator()
  {
    return new Comparator<Sequence>()
    {
      @Override
      public int compare(Sequence a, Sequence b)
      {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a.getStartMS() < b.getStartMS()) return -1;
        if (a.getStartMS() > b.getStartMS()) return 1;
        return a.getName().compareTo(b.getName());
      }
    };
  }

  /** @return index of sequence that matches the query name. */
  public static int findByName(String queryName, List<Sequence> seqs)
  {
    // Look for a perfect match.
    for (int i = 0; i < seqs.size(); ++i) {
      String seqName = seqs.get(i).getName();
      if (seqName.equals(queryName)) return i;
    }

    // Look for a partial match.
    queryName = queryName.toLowerCase();
    for (int i = 0; i < seqs.size(); ++i) {
      String seqName = seqs.get(i).getName().toLowerCase();
      if (seqName.contains(queryName)) return i;
    }
    return -1;
  }

  /** @return Sequence that matches the query name. */
  public static Sequence getSeq(String name, List<Sequence> seqs)
  {
    int i = findByName(name, seqs);
    return i < 0 ? null : seqs.get(i);
  }
}
