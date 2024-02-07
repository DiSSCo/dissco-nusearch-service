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

// Copied and adapted from COL:
// https://github.com/CatalogueOfLife/backend/blob/master/api/src/main/java/life/catalogue/common/tax/NameFormatter.java
// https://github.com/CatalogueOfLife/backend/blob/master/api/src/main/java/life/catalogue/api/model/NameUsageBase.java

import com.google.common.annotations.VisibleForTesting;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.nameparser.api.Rank;

public class NameFormatter {

  public static final char EXTINCT_SYMBOL = '†';
  private static final Pattern RANK_MATCHER = Pattern.compile(
      "^(.+[a-z]) ((?:notho)?(?:infra|super|sub)?(?:gx|natio|morph|klepton|lusus|strain|chemoform|(?:subsp|f\\. ?sp|[a-z]{1,4})\\.|[a-z]{3,6}var\\.?))( [a-z][^ ]*?)?( .+)?$");
  private static final String EPITHET = "[a-z0-9ïëöüäåéèčáàæœ-]+";
  @VisibleForTesting
  private static final Pattern LINNEAN_NAME_NO_AUTHOR = Pattern.compile(
      "^[A-ZÆŒ]" + EPITHET                  // genus
          + "(?: \\([A-ZÆŒ]" + EPITHET + "\\))?"  // infrageneric
          + "(?: " + EPITHET                    // species
          + "(?: " + EPITHET + ")?"            // subspecies
          + ")?$");

  public static StringBuilder labelBuilder(boolean extinct, String scientificName,
      String authorship, Rank rank, TaxonomicStatus status, boolean html) {
    StringBuilder sb = new StringBuilder();
    if (Boolean.TRUE.equals(extinct)) {
      sb.append(EXTINCT_SYMBOL);
    }
    if (html) {

      sb.append(scientificNameHtml(scientificName, rank));
    } else {
      sb.append(scientificName);
    }
    if (status == TaxonomicStatus.MISAPPLIED) {
      // default for misapplied names: https://github.com/gbif/name-parser/issues/87
      sb.append(" ").append("auct. non");
    }
    if (authorship != null) {
      sb.append(" ").append(authorship);
    }
    return sb;
  }

  /**
   * Adds italics around the epithets but not rank markers or higher ranked names.
   */
  public static String scientificNameHtml(String scientificName, Rank rank) {
    // only genus names and below are shown in italics
    if (scientificName != null && rank != null && rank.ordinal() >= Rank.GENUS.ordinal()) {
      Matcher m = RANK_MATCHER.matcher(scientificName);
      if (m.find()) {
        StringBuilder sb = new StringBuilder();
        sb.append(NameFormatter.inItalics(m.group(1)));
        sb.append(" ");
        sb.append(m.group(2));
        if (m.group(3) != null) {
          sb.append(" ");
          sb.append(NameFormatter.inItalics(m.group(3).trim()));
        }
        if (m.group(4) != null) {
          sb.append(" ");
          sb.append(m.group(4).trim());
        }
        return sb.toString();

      } else {
        m = LINNEAN_NAME_NO_AUTHOR.matcher(scientificName);
        if (m.find()) {
          return NameFormatter.inItalics(scientificName);
        }
      }
    }
    //TODO: Candidatus or Ca.
    return scientificName;
  }

  public static String inItalics(String x) {
    return "<i>" + x + "</i>";
  }


}
