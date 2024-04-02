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
package eu.dissco.nusearch.service;

import static org.gbif.api.vocabulary.Rank.FAMILY;
import static org.gbif.api.vocabulary.Rank.GENUS;
import static org.gbif.api.vocabulary.Rank.INFRASPECIFIC_NAME;
import static org.gbif.api.vocabulary.Rank.KINGDOM;
import static org.gbif.api.vocabulary.Rank.SPECIES;
import static org.gbif.api.vocabulary.Rank.SPECIES_AGGREGATE;
import static org.gbif.api.vocabulary.Rank.SUBGENUS;
import static org.gbif.api.vocabulary.Rank.SUBSPECIES;
import static org.gbif.api.vocabulary.Rank.UNRANKED;
import static org.gbif.api.vocabulary.Rank.VARIETY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.gbif.api.vocabulary.Rank;
import org.junit.jupiter.api.Test;

class NubMatchingServiceImplTest {

  @Test
  void rankSimilarity() {
    assertEquals(6, NubMatchingService.rankSimilarity(FAMILY, FAMILY));
    assertEquals(6, NubMatchingService.rankSimilarity(SPECIES, SPECIES));
    assertEquals(-1, NubMatchingService.rankSimilarity(GENUS, SUBGENUS));
    assertEquals(2, NubMatchingService.rankSimilarity(SPECIES, SPECIES_AGGREGATE));
    assertEquals(6, NubMatchingService.rankSimilarity(UNRANKED, UNRANKED));
    assertEquals(-1, NubMatchingService.rankSimilarity(UNRANKED, null));
    assertEquals(0, NubMatchingService.rankSimilarity(FAMILY, UNRANKED));
    assertEquals(0, NubMatchingService.rankSimilarity(SPECIES, UNRANKED));
    assertEquals(-9, NubMatchingService.rankSimilarity(SUBSPECIES, VARIETY));
    assertEquals(2, NubMatchingService.rankSimilarity(SUBSPECIES, INFRASPECIFIC_NAME));
    assertEquals(-35, NubMatchingService.rankSimilarity(GENUS, Rank.CLASS));
    assertEquals(-35, NubMatchingService.rankSimilarity(GENUS, FAMILY));
    assertEquals(-28, NubMatchingService.rankSimilarity(FAMILY, KINGDOM));
  }

  @Test
  void testNormConfidence2() {
    for (int x=80; x<150; x++) {
      System.out.println(x + " -> " + NubMatchingService.normConfidence(x));
    }
  }

  @Test
  void testNormConfidence() {
    assertEquals(0, NubMatchingService.normConfidence(0));
    assertEquals(0, NubMatchingService.normConfidence(-1));
    assertEquals(0, NubMatchingService.normConfidence(-10000));
    assertEquals(1, NubMatchingService.normConfidence(1));
    assertEquals(10, NubMatchingService.normConfidence(10));
    assertEquals(20, NubMatchingService.normConfidence(20));
    assertEquals(30, NubMatchingService.normConfidence(30));
    assertEquals(50, NubMatchingService.normConfidence(50));
    assertEquals(60, NubMatchingService.normConfidence(60));
    assertEquals(70, NubMatchingService.normConfidence(70));
    assertEquals(80, NubMatchingService.normConfidence(80));
    assertEquals(85, NubMatchingService.normConfidence(85));
    assertEquals(88, NubMatchingService.normConfidence(90));
    assertEquals(89, NubMatchingService.normConfidence(92));
    assertEquals(91, NubMatchingService.normConfidence(95));
    assertEquals(92, NubMatchingService.normConfidence(98));
    assertEquals(92, NubMatchingService.normConfidence(99));
    assertEquals(93, NubMatchingService.normConfidence(100));
    assertEquals(95, NubMatchingService.normConfidence(105));
    assertEquals(96, NubMatchingService.normConfidence(110));
    assertEquals(97, NubMatchingService.normConfidence(115));
    assertEquals(99, NubMatchingService.normConfidence(120));
    assertEquals(100, NubMatchingService.normConfidence(125));
    assertEquals(100, NubMatchingService.normConfidence(130));
    assertEquals(100, NubMatchingService.normConfidence(150));
    assertEquals(100, NubMatchingService.normConfidence(175));
    assertEquals(100, NubMatchingService.normConfidence(200));
    assertEquals(100, NubMatchingService.normConfidence(1000));
  }

}
