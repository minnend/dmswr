package org.minnen.dmswr.viz;

import java.util.Arrays;

/**
 * Contains pre-defined color sets for visualization. The colors are chosen to be perceptually different to facilitate
 * data visualization.
 *
 * Color sets generated by I Want Hue: http://tools.medialab.sciences-po.fr/iwanthue/
 */
public class Colors
{
  public final static int[][]  rgb30 = { { 106, 220, 138 }, { 209, 77, 224 }, { 219, 77, 34 }, { 90, 109, 144 },
      { 210, 183, 60 }, { 125, 86, 45 }, { 203, 61, 101 }, { 99, 124, 219 }, { 131, 219, 198 }, { 148, 54, 128 },
      { 108, 219, 66 }, { 101, 153, 51 }, { 200, 150, 205 }, { 216, 138, 138 }, { 201, 213, 135 }, { 105, 179, 214 },
      { 97, 157, 107 }, { 73, 117, 111 }, { 129, 80, 97 }, { 198, 224, 73 }, { 208, 138, 49 }, { 166, 105, 213 },
      { 86, 105, 48 }, { 197, 193, 213 }, { 170, 77, 56 }, { 201, 149, 99 }, { 219, 66, 180 }, { 101, 81, 145 },
      { 220, 99, 162 }, { 210, 62, 65 } };

  public final static String[] hex30 = { "#6ADC8A", "#D14DE0", "#DB4D22", "#5A6D90", "#D2B73C", "#7D562D", "#CB3D65",
      "#637CDB", "#83DBC6", "#943680", "#6CDB42", "#659933", "#C896CD", "#D88A8A", "#C9D587", "#69B3D6", "#619D6B",
      "#49756F", "#815061", "#C6E049", "#D08A31", "#A669D5", "#566930", "#C5C1D5", "#AA4D38", "#C99563", "#DB42B4",
      "#655191", "#DC63A2", "#D23E41" };

  public final static int[][]  rgb12 = { { 110, 173, 192 }, { 212, 83, 51 }, { 135, 212, 76 }, { 204, 81, 200 },
      { 201, 78, 118 }, { 118, 215, 162 }, { 211, 187, 72 }, { 120, 94, 128 }, { 115, 113, 204 }, { 92, 130, 68 },
      { 171, 113, 68 }, { 207, 153, 201 } };

  public final static String[] hex12 = { "#6EADC0", "#D45333", "#87D44C", "#CC51C8", "#C94E76", "#76D7A2", "#D3BB48",
      "#785E80", "#7371CC", "#5C8244", "#AB7144", "#CF99C9" };

  public final static int[][]  rgb5  = { { 200, 105, 209 }, { 125, 200, 83 }, { 227, 84, 103 }, { 209, 144, 45 },
      { 108, 149, 211 } };

  public final static String[] hex5  = { "#C869D1", "#7DC853", "#E35467", "#D1902D", "#6C95D3" };

  public static int[][] getRGB(int n)
  {
    if (n >= 12) return rgb30;
    else if (n > 5) return rgb12;
    else return rgb5;
  }

  public static String[] getHex(int n)
  {
    if (n >= 12) return hex30;
    else if (n > 5) return hex12;
    else return hex5;
  }

  /**
   * Interpolate a value, e.g. for linear interpolation between anchor colors.
   * 
   * Example: interpolate(50, 0, 100, 20, 30) --> 25.
   * 
   * @param x value at which to interpolate
   * @param pairs an even number of integers where the first half are keys and the second half are values.
   * @return interpolated color.
   */
  public static int interpolate(int x, int... pairs)
  {
    int n = pairs.length;
    assert n % 2 == 0;
    n = n / 2;

    int[] keys = new int[n];
    int[] values = new int[n];
    for (int i = 0; i < n; ++i) {
      keys[i] = pairs[i];
      values[i] = pairs[n + i];
    }

    if (x <= keys[0]) return values[0];
    if (x >= keys[n - 1]) return values[n - 1];

    int index = Arrays.binarySearch(keys, x);
    if (index >= 0) return values[index];

    index = -(index + 1);

    final int a = keys[index - 1];
    final int b = keys[index];
    assert a <= x && b >= x;

    final int p = values[index - 1];
    final int q = values[index];

    final double percent = (double) (x - a) / (b - a);
    return p + (int) Math.round((q - p) * percent);
  }

  /** @return colormap holding colors in the form "rgb(R, G, B)". */
  public static String[] buildRedToGreenColorMap()
  {
    // Build color table; colors adapted from: https://learnui.design/tools/data-color-picker.html#divergent and
    // https://earlyretirementnow.com/2016/12/07/the-ultimate-guide-to-safe-withdrawal-rates-part-1-intro/
    String[] colors = new String[101];
    for (int i = 0; i < colors.length; ++i) {
      final int red = Colors.interpolate(i, //
          0, 30, 50, 80, 100, //
          255, 250, 253, 241, 100);
      final int green = Colors.interpolate(i, //
          0, 30, 50, 80, 100, //
          90, 148, 183, 232, 191);
      final int blue = Colors.interpolate(i, //
          0, 30, 50, 80, 100, //
          80, 116, 122, 133, 124);
      assert red >= 0 && red <= 255 : red;
      assert green >= 0 && green <= 255 : green;
      assert blue >= 0 && blue <= 255 : blue;
      colors[i] = String.format("rgb(%d, %d, %d)", red, green, blue);
    }
    return colors;
  }
}
