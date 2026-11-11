/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.dissco.nusearch.utils;

// Copied and adapted from GBIF gbif-common 1.2 (org.gbif.utils.file.FileUtils and
// org.gbif.utils.text.StringUtils) so the service does not need gbif-common, which bundles an old
// Jackson 2.14.1.
// https://github.com/gbif/gbif-common/tree/gbif-common-1.2/src/main/java/org/gbif/utils

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class GbifTextUtils {

  private static final Pattern TAB_DELIMITED = Pattern.compile("\t");
  private static final Pattern MARKER = Pattern.compile("\\p{M}");

  private GbifTextUtils() {
  }

  /**
   * Reads a UTF-8, tab delimited stream into a map, skipping blank lines and lines starting with
   * #. Lines with too few columns are ignored.
   */
  public static Map<String, String> streamToMap(InputStream source, int key, int value,
      boolean trimToNull) throws IOException {
    return streamToMap(source, new HashMap<>(), key, value, trimToNull);
  }

  /** As {@link #streamToMap(InputStream, int, int, boolean)}, adding to the given map. */
  public static Map<String, String> streamToMap(InputStream source, Map<String, String> result,
      int key, int value, boolean trimToNull) throws IOException {
    // gbif-common uses "key > value ? key : value + 1", which fails on short lines when key > value
    int maxCols = Math.max(key, value) + 1;
    try (var reader = utf8Reader(source)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (ignore(line)) {
          continue;
        }
        String[] parts = TAB_DELIMITED.split(line);
        if (maxCols <= parts.length) {
          if (trimToNull) {
            result.put(StringUtils.trimToNull(parts[key]), StringUtils.trimToNull(parts[value]));
          } else {
            result.put(parts[key], parts[value]);
          }
        }
      }
    }
    return result;
  }

  /**
   * Reads a UTF-8 stream into a set of trimmed lines, skipping blank lines and lines starting with
   * #.
   */
  public static Set<String> streamToSet(InputStream source) throws IOException {
    var result = new HashSet<String>();
    try (var reader = utf8Reader(source)) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!ignore(line)) {
          result.add(line);
        }
      }
    }
    return result;
  }

  /**
   * Removes accents and diacritics and expands common ligatures, e.g. ß to ss.
   *
   * @return the ASCII equivalent, or null for null input
   */
  public static String foldToAscii(String x) {
    if (x == null) {
      return null;
    }
    x = replaceSpecialCases(x);
    x = Normalizer.normalize(x, Normalizer.Form.NFD);
    return MARKER.matcher(x).replaceAll("");
  }

  private static String replaceSpecialCases(String x) {
    var sb = new StringBuilder(x.length());
    for (int i = 0; i < x.length(); i++) {
      char c = x.charAt(i);
      switch (c) {
        case 'ß' -> sb.append("ss");
        case 'Æ' -> sb.append("AE");
        case 'æ' -> sb.append("ae");
        case 'Ð' -> sb.append("D");
        case 'đ', 'ð' -> sb.append("d");
        case 'Ø' -> sb.append("O");
        case 'ø' -> sb.append("o");
        case 'Œ' -> sb.append("OE");
        case 'œ' -> sb.append("oe");
        case 'Ŧ' -> sb.append("T");
        case 'ŧ' -> sb.append("t");
        case 'Ł' -> sb.append("L");
        case 'ł' -> sb.append("l");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

  private static BufferedReader utf8Reader(InputStream source) {
    return new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
  }

  private static boolean ignore(String line) {
    return StringUtils.trimToNull(line) == null || line.startsWith("#");
  }
}
