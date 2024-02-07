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

import eu.dissco.nusearch.component.ModifiedDamerauLevenshtein;
import eu.dissco.nusearch.component.ScientificNameSimilarity;
import eu.dissco.nusearch.component.StringSimilarity;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

class StringSimilarityComparisonTest {

  private final StopWatch watch = new StopWatch();
  private final StringSimilarity mdl2 = new ModifiedDamerauLevenshtein(2);
  private final StringSimilarity mdl3 = new ModifiedDamerauLevenshtein(3);
  private final StringSimilarity scin = new ScientificNameSimilarity();

  @Test
  void testSimilarities() {
    compare("Abies alba", "Abies alba");
    compare("Abies alba", "Abies alta");
    compare("Linaria pedunculata", "Linaria pedinculata");
    compare("Linaria pedunculata", "Lunaria pedunculata");
    compare("Linaria pedunculata", "Linariya pedonculata");
    compare("Linaria pedunculata vulgaris", "Lunaria pedunculata vulgaris");
    compare("Linaria pedunculata vulgaris", "Linaria pedunculata vandalis");
    compare("Oreina elegans", "Orfelia elegans");
    compare("Lucina scotti", "Lucina wattsi");
    compare("scotti", "wattsi");
  }

  private void compare(String x1, String x2) {
    System.out.println("\n" + x1 + "  ~  " + x2);
    sim("MDL2", mdl2, x1, x2);
    sim("MDL3", mdl3, x1, x2);
    sim("SciN", scin, x1, x2);
  }

  private void sim(String name, StringSimilarity sim, String x1, String x2) {
    watch.reset();
    watch.start();
    System.out.println('\t' + name + " = " + sim.getSimilarity(x1, x2) + '\t' + watch.getNanoTime());
  }


}