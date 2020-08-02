package org.minnen.dmswr.utils;

import java.util.*;

import org.minnen.dmswr.utils.Library;
import org.minnen.dmswr.utils.TimeLib;

/** represents a vector in R^n */
public class FeatureVec extends MetaStore
{
  /** actual data */
  private double[] vec;
  private double   weight    = 1.0;
  private long     timestamp = TimeLib.TIME_ERROR;
  private String   name;

  /**
   * Create a feature vec from the double array
   * 
   * @param vec data for feature vec
   */
  public FeatureVec(double vec[])
  {
    this.vec = vec.clone();
  }

  /**
   * Duplicate the given feature vec
   * 
   * @param fv feature vec to duplicate
   */
  public FeatureVec(FeatureVec fv)
  {
    copyFrom(fv);
  }

  /** create a vector with dimensionality zero (use appendDim to add dimensions) */
  public FeatureVec()
  {
    vec = new double[0];
  }

  /**
   * Create a named feature vector with 'nDims' using the supplied values; if too few are given, the last one is
   * repeated (or zero if no values are provided).
   */
  public FeatureVec(String name, int nDims, double... x)
  {
    this(nDims, x);
    this.name = name;
  }

  /**
   * Create a feature vector with 'nDims' using the supplied values; if too few are given, the last one is repeated (or
   * zero if no values are provided).
   */
  public FeatureVec(int nDims, double... x)
  {
    assert x.length == nDims || x.length == 0;
    if (x.length == 0) {
      vec = new double[nDims];
    } else {
      vec = Arrays.copyOf(x, nDims);
    }
  }

  public String getName()
  {
    return name;
  }

  public FeatureVec setName(String name)
  {
    this.name = name;
    return this;
  }

  /** @return this*x' */
  public double[][] cross(FeatureVec x)
  {
    int D1 = getNumDims();
    int D2 = x.getNumDims();
    double[] a = x.get();
    double[][] ret = new double[D1][D2];

    if (D1 <= D2) {
      // square or wide matrix
      for (int i = 0; i < D1; i++)
        for (int j = 0; j < i; j++)
          ret[i][j] = ret[j][i] = vec[i] * a[j];
      for (int i = 0; i < D1; i++)
        ret[i][i] = vec[i] * a[i];
      for (int i = 0; i < D1; i++)
        for (int j = i + 1; j < D2; j++)
          ret[i][j] = vec[i] * a[j];
    } else {
      // tall matrix
      for (int i = 0; i < D2; i++)
        for (int j = 0; j < i; j++)
          ret[i][j] = ret[j][i] = vec[i] * a[j];
      for (int i = 0; i < D2; i++)
        ret[i][i] = vec[i] * a[i];
      for (int i = D2; i < D1; i++)
        for (int j = 0; j < D2; j++)
          ret[i][j] = vec[i] * a[j];
    }

    return ret;
  }

  /** set the time stamp of this point (ms) */
  public FeatureVec setTime(long ms)
  {
    timestamp = ms;
    return this;
  }

  /** @return time stamp for this point (ms) */
  public long getTime()
  {
    return timestamp;
  }

  public FeatureVec setWeight(double weight)
  {
    this.weight = weight;
    return this;
  }

  public double getWeight()
  {
    return weight;
  }

  /** @return true if the timestamp for this point is valid */
  public boolean hasTime()
  {
    return timestamp != TimeLib.TIME_ERROR;
  }

  /** @return true if no dimensions are NaN */
  public boolean isValid()
  {
    if (vec == null) return false;
    for (int i = 0; i < vec.length; i++)
      if (Double.isNaN(vec[i])) return false;
    return true;
  }

  /** @return dimensionality of this feature vector */
  public int getNumDims()
  {
    return (vec == null ? 0 : vec.length);
  }

  /** @return vector data as a double array (actual reference, not a copy!) */
  public double[] get()
  {
    return vec;
  }

  /** @return vector data as a double array (copy of internal data) */
  public double[] toArray()
  {
    return vec.clone();
  }

  /** @return value of d^{th} dimension */
  public double get(int d)
  {
    return vec[d];
  }

  /** set the value of the d^{th} dimension to v */
  public FeatureVec set(int d, double v)
  {
    vec[d] = v;
    return this;
  }

  /** Copy features from FV starting at dimension d. */
  public int set(int d, FeatureVec fv)
  {
    final int D = fv.getNumDims();
    System.arraycopy(fv.get(), 0, vec, d, D);
    return d + D;
  }

  /** Set all values at once. */
  public void set(double[] values)
  {
    assert values.length == vec.length;
    System.arraycopy(values, 0, vec, 0, values.length);
  }

  /** set the value of all dimensions to x */
  public FeatureVec fill(double x)
  {
    Arrays.fill(vec, x);
    return this;
  }

  /**
   * Copies the values of the argument into this feature vector.
   */
  public void copyFrom(FeatureVec fv)
  {
    int n = fv.getNumDims();
    name = fv.name;
    if (vec == null || getNumDims() != n) vec = new double[n];
    for (int i = 0; i < n; i++)
      vec[i] = fv.get(i);
    timestamp = fv.timestamp;
  }

  public FeatureVec dup()
  {
    return new FeatureVec(this);
  }

  /**
   * @return new feature vector with just those dimensions specified by the dims array (and in that order).
   */
  public FeatureVec selectDims(int... dims)
  {
    int nd = dims.length;
    FeatureVec ret = new FeatureVec(nd);
    ret.setTime(getTime());
    ret.setName(getName());
    for (int i = 0; i < nd; i++)
      ret.set(i, get(dims[i]));
    return ret;
  }

  /**
   * Compute the absolute distance between this and the given vector
   * 
   * @param fv compute absolute distance to this vector
   * @return absolute distance
   */
  public double absdist(FeatureVec fv)
  {
    double sum = 0;
    for (int d = 0; d < vec.length; d++)
      sum += Math.abs(fv.vec[d] - vec[d]);
    return sum;
  }

  /**
   * Compute the squared distance between the vectors
   * 
   * @param fv compute squared distance to this vector
   * @return squared distance
   */
  public double dist2(FeatureVec fv)
  {
    double ret = 0.0;
    for (int d = 0; d < vec.length; d++) {
      double diff = vec[d] - fv.vec[d];
      ret += diff * diff;
    }
    return ret;
  }

  /**
   * Compute the distance between the vectors
   * 
   * @param fv compute distance to this vector
   * @return distance
   */
  public double dist(FeatureVec fv)
  {
    return Math.sqrt(dist2(fv));
  }

  /**
   * Create a feature vec of zeros
   * 
   * @param n dimensionality
   * @return new vector
   */
  public static FeatureVec zeros(int n)
  {
    return new FeatureVec(n).fill(0.0);
  }

  /** set all dimensions to zero */
  public void zero()
  {
    for (int i = 0; i < vec.length; i++)
      vec[i] = 0.0;
  }

  /**
   * Create a feature vec of ones
   * 
   * @param n dimensionality
   * @return new vector
   */
  public static FeatureVec ones(int n)
  {
    return new FeatureVec(n).fill(1.0);
  }

  /**
   * @return sum of elements of this feature vec
   */
  public double sum()
  {
    double v = 0;
    for (int i = 0; i < vec.length; i++)
      v += vec[i];
    return v;
  }

  /** @return mean (average) of elements of this feature vec */
  public double mean()
  {
    return sum() / getNumDims();
  }

  /**
   * in-place addition
   * 
   * @param fv summand
   */
  public FeatureVec _add(FeatureVec fv)
  {
    int n = getNumDims();
    assert n == fv.getNumDims() : String.format("this.nDims=%d  fv.nDims=%d\n", n, fv.getNumDims());
    for (int i = 0; i < n; i++)
      vec[i] += fv.get(i);
    return this;
  }

  /**
   * in-place addition
   * 
   * @param x summand
   */
  public FeatureVec _add(double x)
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] += x;
    return this;
  }

  /**
   * in-place subtraction
   * 
   * @param fv vector to subtract
   */
  public FeatureVec _sub(FeatureVec fv)
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] -= fv.get(i);
    return this;
  }

  /**
   * in-place subtraction
   * 
   * @param x value to substract
   */
  public FeatureVec _sub(double x)
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] -= x;
    return this;
  }

  /**
   * in-place multiplicatin
   * 
   * @param fv multiplier
   */
  public FeatureVec _mul(FeatureVec fv)
  {
    int n = getNumDims();
    assert n == fv.getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] *= fv.get(i);
    return this;
  }

  /**
   * in-place multiplication
   * 
   * @param x multiplier
   */
  public FeatureVec _mul(double x)
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] *= x;
    return this;
  }

  /**
   * in-place division
   * 
   * @param fv vector divisor
   */
  public FeatureVec _div(FeatureVec fv)
  {
    int n = getNumDims();
    double[] a = fv.get();
    for (int i = 0; i < n; i++)
      vec[i] /= a[i];
    return this;
  }

  /**
   * in-place division
   * 
   * @param x divisor
   */
  public FeatureVec _div(double x)
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] /= x;
    return this;
  }

  /**
   * in-place natural logarithm (signed)
   * 
   * @return this vector
   */
  public FeatureVec _log()
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++) {
      double sign = Math.signum(vec[i]);
      vec[i] = sign * Math.log(Math.abs(vec[i]));
    }
    return this;
  }

  /**
   * @return new vector with each element replaced with its (signed) natural log
   */
  public FeatureVec log()
  {
    return new FeatureVec(this)._log();
  }

  /**
   * in-place natural logarithm plus one (signed)
   * 
   * @return this vector
   */
  public FeatureVec _log1p()
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++) {
      double sign = Math.signum(vec[i]);
      vec[i] = sign * Math.log1p(Math.abs(vec[i]));
    }
    return this;
  }

  /**
   * @return new vector with each element replaced with log(x+1) (signed)
   */
  public FeatureVec log1p()
  {
    return new FeatureVec(this)._log1p();
  }

  /**
   * in-place absolute value
   */
  public FeatureVec _abs()
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] = Math.abs(vec[i]);
    return this;
  }

  /**
   * @return vector with sign (-1/0/+1) for each dimension
   */
  public FeatureVec sign()
  {
    FeatureVec fv = new FeatureVec(this);
    return fv._sign();
  }

  /** in-place calculation of sign (-1/0/+1) of each dimension */
  public FeatureVec _sign()
  {
    int D = getNumDims();
    for (int d = 0; d < D; d++)
      vec[d] = Math.signum(vec[d]);
    return this;
  }

  /**
   * in-place squaring of elements
   */
  public FeatureVec _sqr()
  {
    int n = getNumDims();
    for (int i = 0; i < n; i++)
      vec[i] = vec[i] * vec[i];
    return this;
  }

  /** @return new vector with values equal to the squared value in this vector */
  public FeatureVec sqr()
  {
    return new FeatureVec(this)._sqr();
  }

  /** @return new vector with values equal to the absolute value of this vector */
  public FeatureVec abs()
  {
    return new FeatureVec(this)._abs();
  }

  /** @return new vector representing sum of this vector and given vector */
  public FeatureVec add(FeatureVec fv)
  {
    return new FeatureVec(this)._add(fv);
  }

  /** @return new vector with x added to each value in this vector */
  public FeatureVec add(double x)
  {
    return new FeatureVec(this)._add(x);
  }

  /** @return new vector representing difference of this vector and the given vector */
  public FeatureVec sub(FeatureVec fv)
  {
    return new FeatureVec(this)._sub(fv);
  }

  /** @return new vector with x subtracted from each value in this vector */
  public FeatureVec sub(double x)
  {
    return new FeatureVec(this)._sub(x);
  }

  /** @return new vector representing the component-wise product of this vector and the given vector */
  public FeatureVec mul(FeatureVec fv)
  {
    return new FeatureVec(this)._mul(fv);
  }

  /** @return new vector with x multiplied with each value in this vector */
  public FeatureVec mul(double x)
  {
    return new FeatureVec(this)._mul(x);
  }

  /** @return new vector representing the component-wise quotient of this vector and the given vector */
  public FeatureVec div(FeatureVec fv)
  {
    return new FeatureVec(this)._div(fv);
  }

  /** @return new vector with each value in this vector divided by x. */
  public FeatureVec div(double x)
  {
    return new FeatureVec(this)._div(x);
  }

  /** @return dot product of this vector and the given vector. */
  public double dot(FeatureVec fv)
  {
    int n = getNumDims();
    assert n == fv.getNumDims();
    double ret = 0.0;
    for (int i = 0; i < n; i++)
      ret += vec[i] * fv.vec[i];
    return ret;
  }

  /**
   * @return projection of this vector onto the given vector
   */
  public FeatureVec projv(FeatureVec fv)
  {
    double a = this.dot(fv);
    double b = fv.dot(fv);
    return fv._mul(a / b);
  }

  /**
   * @return projection of this vector onto the vector a->p
   */
  public FeatureVec projv(FeatureVec a, FeatureVec b)
  {
    return this.sub(b).projv(a.sub(b))._add(b);
  }

  /**
   * @return position along the given vector (after normalization) of the projection. So the projection can be recovered
   *         by a*v/|v| if 'a' is the return value and 'v' is the given vector.
   */
  public double projLen(FeatureVec fv)
  {
    return this.dot(fv) / fv.norm();
  }

  /** @return distance from this vector to its projection on the given vector */
  public double projDist(FeatureVec fv)
  {
    return Math.sqrt(projDist2(fv));
  }

  /** @return squared distance from this vector to its projection on the given vector */
  public double projDist2(FeatureVec fv)
  {
    return this.dist2(this.projv(fv));
  }

  /** @return squared L2 norm of this vector */
  public double norm2()
  {
    int n = getNumDims();
    double ret = 0.0;
    for (int i = 0; i < n; i++)
      ret += vec[i] * vec[i];
    return ret;
  }

  /** @return L2 norm of this vector */
  public double norm()
  {
    return Math.sqrt(norm2());
  }

  /** in-place square root */
  public FeatureVec _sqrt()
  {
    for (int i = 0; i < vec.length; i++)
      vec[i] = Math.sqrt(vec[i]);
    return this;
  }

  /** @return vector with each dimension equal to the square root of the value in this vector */
  public FeatureVec sqrt()
  {
    return new FeatureVec(this)._sqrt();
  }

  /** @return this vector after it has been normalize to be a unit vector */
  public FeatureVec _unit()
  {
    return _div(norm());
  }

  /** @return unit vector in the direction of this vector */
  public FeatureVec unit()
  {
    return div(norm());
  }

  /** @return vector with each dimension equal to the smaller of this vector and the given vector */
  public FeatureVec min(FeatureVec fv)
  {
    return new FeatureVec(this)._min(fv);
  }

  /** in-place minimum value selection */
  public FeatureVec _min(FeatureVec fv)
  {
    int d = getNumDims();
    assert fv.getNumDims() == d : String.format("dimensionality: local=%d  param=%d", d, fv.getNumDims());
    for (int i = 0; i < d; i++)
      vec[i] = Math.min(vec[i], fv.get(i));
    return this;
  }

  /** @return smallest value across all of the dimensions */
  public double min()
  {
    double x = vec[0];
    for (int i = 1; i < vec.length; i++)
      x = Math.min(x, vec[i]);
    return x;
  }

  /** @return true if all dimensions are smaller than the corresponding value in the given vector */
  public boolean lessThan(FeatureVec fv)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] >= fv.get(d)) return false;
    return true;
  }

  /** @return true if all dimensions are smaller than or equal to the corresponding value in the given vector */
  public boolean leqThan(FeatureVec fv)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] > fv.get(d)) return false;
    return true;
  }

  /** @return true if all dimensions are larger than or equal to the corresponding value in the given vector */
  public boolean geqThan(FeatureVec fv)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] < fv.get(d)) return false;
    return true;
  }

  /** @return true if all dimensions are larger than the corresponding value in the given vector */
  public boolean greaterThan(FeatureVec fv)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] <= fv.get(d)) return false;
    return true;
  }

  /** @return true if all dimensions are smaller than the given value */
  public boolean lessThan(double x)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] >= x) return false;
    return true;
  }

  /** @return true if all dimensions are smaller than or equal to the given value */
  public boolean leqThan(double x)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] > x) return false;
    return true;
  }

  /** @return true if all dimensions are larger than or equal to the given value */
  public boolean geqThan(double x)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] < x) return false;
    return true;
  }

  /** @return true if all dimensions are larger than the given value */
  public boolean greaterThan(double x)
  {
    for (int d = 0; d < vec.length; d++)
      if (vec[d] <= x) return false;
    return true;
  }

  /** @return largest value across all of the dimensions */
  public double max()
  {
    double x = vec[0];
    for (int i = 1; i < vec.length; i++)
      x = Math.max(x, vec[i]);
    return x;
  }

  public FeatureVec max(FeatureVec fv)
  {
    FeatureVec ret = new FeatureVec(this);
    return ret._max(fv);
  }

  public FeatureVec _max(FeatureVec fv)
  {
    int d = getNumDims();
    assert fv.getNumDims() == d;
    for (int i = 0; i < d; i++)
      vec[i] = Math.max(vec[i], fv.get(i));
    return this;
  }

  public FeatureVec min(double x)
  {
    int d = getNumDims();
    FeatureVec ret = new FeatureVec(d);
    for (int i = 0; i < d; i++)
      ret.set(i, Math.min(vec[i], d));
    return ret;
  }

  public FeatureVec max(double x)
  {
    int d = getNumDims();
    FeatureVec ret = new FeatureVec(d);
    for (int i = 0; i < d; i++)
      ret.set(i, Math.max(vec[i], x));
    return ret;
  }

  /**
   * add the given value as an extra dimension
   * 
   * @param x value of the new dimension
   * @return this vector
   */
  public FeatureVec _appendDim(double x)
  {
    vec = Library.concat(vec, x);
    return this;
  }

  /**
   * @return new vector with the given value appended as an extra dimension
   */
  public FeatureVec appendDim(double x)
  {
    return new FeatureVec(this)._appendDim(x);
  }

  /**
   * Add the values in the given vector as extra dimensions.
   * 
   * @param x vector to add
   * @return this vector
   */
  public FeatureVec _appendDims(FeatureVec x)
  {
    vec = Library.concat(vec, x.vec);
    return this;
  }

  /**
   * @return new vector with the given vectorappended as extra dimensions
   */
  public FeatureVec appendDims(FeatureVec x)
  {
    return new FeatureVec(this)._appendDims(x);
  }

  /**
   * add the given values as extra dimensions
   * 
   * @param x values of the new dimensions
   * @return this vector
   */
  public FeatureVec _appendDim(double[] x)
  {
    vec = Library.concat(vec, x);
    return this;
  }

  /**
   * @return new vector with the given values appended as extra dimensions
   */
  public FeatureVec appendDim(double[] x)
  {
    return new FeatureVec(this)._appendDim(x);
  }

  /**
   * add the given values as extra dimensions
   * 
   * @param x vector to append
   * @return this vector
   */
  public FeatureVec _appendDim(FeatureVec x)
  {
    return this._appendDim(x.get());
  }

  /**
   * @return new vector with the given vector appended
   */
  public FeatureVec appendDim(FeatureVec x)
  {
    return new FeatureVec(this)._appendDim(x);
  }

  /** @return FeatureVec with a subset of dimensions in this vector. */
  public FeatureVec subspace(int... dims)
  {
    double[] a = new double[dims.length];
    for (int i = 0; i < dims.length; ++i) {
      a[i] = this.vec[dims[i]];
    }
    return new FeatureVec(getName(), dims.length, a);
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append(String.format("[%.3f", vec[0]));
    for (int i = 1; i < vec.length; i++)
      sb.append(String.format(",%.3f", vec[i]));
    sb.append("]");
    return sb.toString();
  }

  public static FeatureVec fromString(String line)
  {
    if (line == null) return null;
    line = line.trim();
    if (!line.startsWith("[") || !line.endsWith("]")) return null;
    line = line.substring(1, line.length() - 1);
    String[] fields = line.split(",");
    double[] values = new double[fields.length];
    for (int i = 0; i < fields.length; ++i) {
      values[i] = Double.parseDouble(fields[i]);
    }
    return new FeatureVec(values);
  }

  /** Convert the first dimension of the given list into an array */
  public static double[] toArray1D(List<? extends FeatureVec> data)
  {
    double[] a = new double[data.size()];
    for (int i = 0; i < a.length; i++)
      a[i] = data.get(i).get(0);
    return a;
  }

  /**
   * Convert the data in the given list to a 2D array (DxN). Each row corresponds to a different dimension.
   */
  public static double[][] toSeqArray(List<? extends FeatureVec> data)
  {
    int N = data.size();
    int D = data.get(0).getNumDims();
    double[][] a = new double[D][N];
    for (int i = 0; i < N; i++) {
      FeatureVec fv = data.get(i);
      for (int d = 0; d < D; d++)
        a[d][i] = fv.get(d);
    }
    return a;
  }

  /**
   * Convert the data in the given list to a 2D array (NxD). Rows are feature vectors.
   */
  public static double[][] toFrameArray(List<? extends FeatureVec> data)
  {
    int N = data.size();
    double[][] a = new double[N][];
    for (int i = 0; i < N; i++)
      a[i] = data.get(i).toArray();
    return a;
  }
}
