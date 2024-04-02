package eu.dissco.nusearch.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gbif.api.vocabulary.Rank.SPECIES;
import static org.gbif.api.vocabulary.TaxonomicStatus.ACCEPTED;
import static org.junit.Assert.assertThrows;

import eu.dissco.nusearch.component.ScientificNameAnalyzer;
import eu.dissco.nusearch.configuration.LuceneConfiguration;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.property.IndexingProperties;
import java.io.IOException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.AlreadyClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NubIndexIT {

  private final ScientificNameAnalyzer analyzer = new ScientificNameAnalyzer();
  private NubIndex index;

  private IndexSearcher indexSearcher;

  private static void validateResult(ColDpNameUsageMatch result) {
    assertThat(result.getCanonicalName()).isEqualTo("Lamenia vitrea");
    assertThat(result.getRank()).isEqualTo(SPECIES);
    assertThat(result.getTaxonomicStatus()).isEqualTo(ACCEPTED);
    assertThat(result.getColParentId()).isEqualTo("x3MB");
    assertThat(result.getAuthorship()).isEqualTo("(Muir, 1913)");
    assertThat(result.getClassifications()).hasSize(7);
  }

  @BeforeEach
  void setup() throws IOException {
    var properties = new IndexingProperties();
    // Index at this location is prefilled and based on colDP 1011, Fulgoromorpha Lists
    properties.setIndexLocation("src/test/resources/index");
    indexSearcher = new LuceneConfiguration(properties).configureIndexSearcher();
    index = new NubIndex(indexSearcher, analyzer);
  }

  @Test
  void testMatchByUsageId() {
    // Given

    // When
    var result = index.matchByUsageId("2214");

    // Then
    validateResult(result);
  }

  @Test
  void testMatchByName() {
    // Given

    // When
    var result = index.matchByName("Lamenia vitrea", false, 1).get(0);

    // Then
    validateResult(result);
  }

  @Test
  void testMatchByNameFuzzy() {
    // Given

    // When
    var result = index.matchByName("Lameni vitre", true, 1).get(0);

    // Then
    validateResult(result);
  }

  @Test
  void testMatchByNameIOException() throws IOException {
    // Given
    indexSearcher.getTopReaderContext().reader().close();

    // When
    assertThrows(AlreadyClosedException.class, () -> index.matchByName("Lameni vitre", true, 1));
  }


  @Test
  void testAutoComplete() {
    // Given

    // When
    var result = index.autocomplete("A", 10);

    // Then
    assertThat(result).hasSize(10);
    assertThat(result.get(0).getCanonicalName()).isEqualTo("Aafrita");
  }


}
