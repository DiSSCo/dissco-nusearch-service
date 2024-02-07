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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.dissco.nusearch.utils.RankUtils;
import org.gbif.api.vocabulary.Rank;
import org.junit.jupiter.api.Test;

class RankUtilsTest {

  @Test
  void testNextHigherLinneanRank() throws Exception {
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.GENUS));
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.GENUS, RankUtils.nextHigherLinneanRank(Rank.SUBGENUS));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.KINGDOM));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextHigherLinneanRank(Rank.VARIETY));
  }
}