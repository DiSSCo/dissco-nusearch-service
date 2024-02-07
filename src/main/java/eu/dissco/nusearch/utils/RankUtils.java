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

// Copied and adapted from GBIF:
// https://github.com/gbif/checklistbank/blob/master/checklistbank-common/src/main/java/org/gbif/checklistbank/utils/RankUtils.java

import com.google.common.collect.Lists;
import java.util.List;
import org.gbif.api.vocabulary.Rank;

/**
 *
 */
public class RankUtils {

  private static final List<Rank> LINNEAN_RANKS_REVERSE = Lists.reverse(Rank.LINNEAN_RANKS);

  private RankUtils() {
    // Utility class
  }

  public static Rank nextHigherLinneanRank(Rank rank) {
    for (Rank r : LINNEAN_RANKS_REVERSE) {
      if (r.ordinal() < rank.ordinal()) {
        return r;
      }
    }
    return null;
  }

  /**
   * @return true if the ranks given do not contradict each other
   */
  public static boolean match(Rank r1, Rank r2) {
    if (r1 == null || r1 == Rank.UNRANKED ||
        r2 == null || r2 == Rank.UNRANKED) {
      return true;
    }

    if (r1 == Rank.INFRASPECIFIC_NAME) {
      return r2.isInfraspecific();
    } else if (r1 == Rank.INFRASUBSPECIFIC_NAME) {
      return r2.isInfraspecific() && r2 != Rank.SUBSPECIES;

    } else if (r2 == Rank.INFRASPECIFIC_NAME) {
      return r1.isInfraspecific();
    } else if (r2 == Rank.INFRASUBSPECIFIC_NAME) {
      return r1.isInfraspecific() && r1 != Rank.SUBSPECIES;
    }

    return r1 == r2;
  }
}
