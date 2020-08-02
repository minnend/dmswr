package org.minnen.dmswr.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.minnen.dmswr.utils.Library;
import org.minnen.dmswr.utils.TimeLib;

public class DataIO
{
  private static final File defaultOutputPath  = new File("e:/web");
  private static final File defaultFinancePath = new File("e:/research/finance");

  private static File       outputPath;
  private static File       financePath;

  /** Configure data path and authentication keys from the given `config`. */
  public static void configure(Configuration config)
  {
    if (config.containsKey("path.finance")) {
      DataIO.setFinancePath(new File(config.getString("path.finance")));
    }
    if (config.containsKey("path.output")) {
      DataIO.setOutputPath(new File(config.getString("path.output")));
    }
  }

  public static File getOutputPath()
  {
    if (outputPath == null) {
      outputPath = defaultOutputPath;
      if (!outputPath.exists()) outputPath.mkdirs();
    }
    return outputPath;
  }

  public static File getFinancePath()
  {
    if (financePath == null) {
      financePath = defaultFinancePath;
      if (!financePath.exists()) financePath.mkdirs();
    }
    return financePath;
  }

  public static void setOutputPath(File path)
  {
    if (path != null && path.equals(outputPath)) return;
    outputPath = path;
    if (!outputPath.exists()) outputPath.mkdirs();
  }

  public static void setFinancePath(File path)
  {
    if (path != null && path.equals(financePath)) return;
    financePath = path;
    if (!financePath.exists()) financePath.mkdirs();
  }

  public static void setOutputPath(String path)
  {
    setOutputPath(new File(path));
  }

  public static void setFinancePath(String path)
  {
    setFinancePath(new File(path));
  }

  /**
   * Load data from CSV file of <date>,<value>.
   * 
   * @param file file to load
   * @return Sequence with data loaded from the given file.
   */
  public static Sequence loadDateValueCSV(File file) throws IOException
  {
    if (!file.canRead()) {
      throw new IOException(String.format("Can't read CSV file (%s)", file.getPath()));
    }
    // System.out.printf("Loading CSV data file: [%s]\n", file.getPath());

    BufferedReader in = new BufferedReader(new FileReader(file));

    String name = FilenameUtils.getBaseName(file.getName());
    Sequence data = new Sequence(name);
    String line;
    while ((line = in.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty() || line.startsWith("\"") || line.toLowerCase().startsWith("date")) {
        continue;
      }
      String[] toks = line.trim().split("[,\\s]+");
      if (toks == null || toks.length != 2) {
        // System.err.printf("Error parsing CSV data: [%s]\n", line);
        continue;
      }

      // Skip missing data.
      if (toks[1].equals(".") || toks[1].equals("ND")) {
        continue;
      }

      String[] dateFields = toks[0].split("-");
      try {
        int year = Integer.parseInt(dateFields[0]);
        int month = Integer.parseInt(dateFields[1]);
        int day = 1;
        if (dateFields.length > 2) {
          day = Integer.parseInt(dateFields[2]);
        }
        double rate = Double.parseDouble(toks[1]);

        data.addData(rate, TimeLib.toMs(year, month, day));
      } catch (NumberFormatException e) {
        // System.err.printf("Error parsing CSV data: [%s]\n", line);
        continue;
      }
    }
    in.close();
    if (data.getStartMS() > data.getEndMS()) {
      data.reverse();
    }
    return data;
  }

  public static void saveDateValueCSV(File file, Sequence seq, int iDim) throws IOException
  {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      for (FeatureVec x : seq) {
        writer.write(String.format("%s,%f\n", TimeLib.formatYMD(x.getTime()), x.get(iDim)));
      }
    }
  }

  /**
   * Load data from a CSV file.
   * 
   * @param file file to load
   * @param dims data dimensions (columns) to load; column 0 is assumed to hold the date so a zero in `dims` will load
   *          the first data element (column 1). If null, all data dimensions will be read.
   * @return Sequence with data loaded from the given file.
   */
  public static Sequence loadCSV(File file, int[] dims) throws IOException
  {
    if (!file.canRead()) {
      throw new IOException(String.format("Can't read CSV file (%s)", file.getPath()));
    }
    // System.out.printf("Loading CSV file: [%s]\n", file.getPath());

    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      String name = file.getName().replaceFirst("[\\.][^\\\\/\\.]+$", "");
      Sequence data = new Sequence(name);
      String line;
      int nLinesRead = 0;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        ++nLinesRead;

        String[] fields = line.trim().split(",");
        for (int i = 0; i < fields.length; ++i) {
          // TODO better handling of quotes in CSV.
          fields[i] = fields[i].trim().replaceAll("(^\"|\"$)", "");
        }

        // Parse header if this is the first line.
        if (nLinesRead == 1) {
          List<String> dimNames = new ArrayList<>();
          for (int i = 1; i < fields.length; ++i) {
            dimNames.add(fields[i]);
          }
          data.setDimNames(dimNames);

          if (dims == null) {
            dims = Library.genIdentityArray(fields.length - 1); // load all fields (other than date)
          }
        } else {
          long time = TimeLib.parseDate(fields[0]);
          FeatureVec v = new FeatureVec(dims.length);
          for (int d = 0; d < dims.length; ++d) {
            try {
              String s = fields[dims[d] + 1].toLowerCase();
              double value = Double.NaN;
              if (!s.equals("none") && !s.equals("n/a")) {
                value = Double.parseDouble(s);
              }
              v.set(d, value);
            } catch (NumberFormatException e) {
              System.err.printf("Error parsing CSV data: d=%d  dim=%d  field=%s\n", d, dims[d], fields[dims[d] + 1]);
              return null;
            }
          }
          data.addData(v, time);
        }
      }
      if (data.getStartMS() > data.getEndMS()) {
        data.reverse();
      }
      return data;
    }
  }

  /**
   * Load sequences from a CSV file.
   * 
   * The assumption is that the first column has date information and each additional column is a separate sequence. A
   * header on the first line gives the name of each sequence (must start with "date").
   * 
   * @param file file to load
   * @return List of Sequences with data loaded from the given file.
   */
  public static List<Sequence> loadSequenceCSV(File file) throws IOException
  {
    if (!file.canRead()) {
      throw new IOException(String.format("Can't read CSV file (%s)", file.getPath()));
    }
    System.out.printf("Loading CSV file: [%s]\n", file.getPath());

    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      List<Sequence> seqs = new ArrayList<>();

      String line;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        String[] toks = line.trim().split(",");
        for (int i = 0; i < toks.length; ++i) {
          toks[i] = toks[i].trim();
        }

        // Check for header.
        if (toks[0].toLowerCase().startsWith("date")) {
          if (!seqs.isEmpty()) {
            throw new IOException("Found second header line.");
          }
          for (int i = 1; i < toks.length; ++i) {
            seqs.add(new Sequence(toks[i]));
          }
          continue;
        }

        try {
          long time = TimeLib.parseDate(toks[0]);
          if (toks.length != seqs.size() + 1) {
            throw new IOException(String.format("Expected %d fields, but only found %d", seqs.size() + 1, toks.length));
          }
          for (int i = 1; i < toks.length; ++i) {
            seqs.get(i - 1).addData(new FeatureVec(1, Double.parseDouble(toks[i])).setTime(time));
          }
        } catch (NumberFormatException e) {
          throw new IOException(String.format("Error parsing CSV data: [%s]\n", line));
        }
      }
      return seqs;
    }
  }

  private static void printUrlInfo(URL url)
  {
    try {
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      if ("Not Found".equals(con.getResponseMessage())) {
        System.out.printf("URL: %s (Not Found)\n", url);
      } else {
        System.out.printf("URL: %s\n", url);

        System.out.printf("Connect Timeout: %d\n", con.getConnectTimeout());
        System.out.printf("Read Timeout: %d\n", con.getReadTimeout());
        // con.setConnectTimeout(1000);
        // con.setReadTimeout(1000);
        System.out.printf("Request Method: %s\n", con.getRequestMethod());
        System.out.printf("Response Message: %s\n", con.getResponseMessage());
        System.out.printf("Response Code: %d\n", con.getResponseCode());

        for (int i = 0;; ++i) {
          String s = con.getHeaderField(i);
          if (s == null) break;
          System.out.printf("Header %d: %s\n", i, s);
        }
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  /** @return string contents at the given URL. */
  public static String copyUrlToString(URL url)
  {
    try {
      return IOUtils.toString(url, "UTF-8");
    } catch (IOException e) {
      // e.printStackTrace();
      printUrlInfo(url);
      return null;
    }
  }

  /** @return string contents at the given URL. */
  public static String copyUrlToString(String address)
  {
    try {
      URL url = new URL(address);
      return copyUrlToString(url);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return null;
    }
  }

  /** Copy URL contents to the given file (overwrite if file exists). */
  public static boolean copyUrlToFile(String address, File file)
  {
    try {
      URL url = new URL(address);
      return copyUrlToFile(url, file);
    } catch (MalformedURLException e) {
      return false;
    }
  }

  /** Copy URL contents to the given file (overwrite if file exists). */
  public static boolean copyUrlToFile(URL url, File file)
  {
    try (InputStream input = url.openStream()) {
      Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      printUrlInfo(url);
      return false;
    }
  }

  /** @return true if `file` is older than `ms` milliseconds according to the last-modified timestamp. */
  public static boolean isFileOlder(File file, long ms)
  {
    if (ms > 0L) {
      long age = TimeLib.getTime() - file.lastModified();
      if (age < ms) return false;
    }
    return true;
  }

  public static boolean shouldDownloadUpdate(File file) throws IOException
  {
    return shouldDownloadUpdate(file, 8 * TimeLib.MS_IN_HOUR);
  }

  public static boolean shouldDownloadUpdate(File file, long replaceAgeMs) throws IOException
  {
    if (replaceAgeMs == TimeLib.TIME_END || replaceAgeMs == TimeLib.TIME_ERROR) return false;

    if (!file.exists()) return true;
    if (!file.isFile() || !file.canWrite()) {
      throw new IOException(String.format("File is not writeable (%s).\n", file.getAbsolutePath()));
    }
    if (DataIO.isFileOlder(file, replaceAgeMs)) return true;

    // System.out.printf("Recent file already exists (%s).\n", file.getName());
    return false;
  }

  /** Unzip zipFile into unzipDir (same directory if null). */
  public static void unzipFile(File zipFile, File unzipDir) throws IOException
  {
    if (unzipDir == null) {
      unzipDir = zipFile.getParentFile();
    }
    byte[] buffer = new byte[32 * 1024];
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        String fileName = zipEntry.getName();
        File newFile = new File(unzipDir, fileName);
        FileOutputStream fos = new FileOutputStream(newFile);
        int len;
        while ((len = zis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
        }
        fos.close();
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }

  /** Split string using commas and trim each field. */
  public static String[] splitCSV(String line)
  {
    return splitCSV(line, ",");
  }

  /** Split string and trim each field. */
  public static String[] splitCSV(String line, String regex)
  {
    if (line == null) return null;
    String[] toks = line.trim().split(regex);
    if (toks == null) return null;
    for (int i = 0; i < toks.length; ++i) {
      toks[i] = toks[i].trim();
    }
    return toks;
  }

  public static double parseDouble(String s)
  {
    if (s == null || s.toLowerCase().equals("n/a")) return Double.NaN;

    double scale = 1.0;
    if (s.endsWith("M")) {
      scale = 1e6;
      s = s.substring(0, s.length() - 1);
    } else if (s.endsWith("B")) {
      scale = 1e9;
      s = s.substring(0, s.length() - 1);
    } else if (s.endsWith("%")) {
      s = s.substring(0, s.length() - 1);
    }
    s = s.replaceAll(",", "");
    return scale * Double.parseDouble(s);
  }
}
