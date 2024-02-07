package eu.dissco.nusearch.service;

import static org.mockito.BDDMockito.given;

import eu.dissco.nusearch.configuration.TsvReader;
import eu.dissco.nusearch.property.IndexingProperties;
import com.univocity.parsers.tsv.TsvRoutines;
import eu.dissco.nusearch.service.ColDpDownloadingService;
import eu.dissco.nusearch.service.ColDpIndexingService;
import java.nio.file.Path;
import org.apache.lucene.index.IndexWriter;
import org.gbif.nameparser.NameParserGbifV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ColDpIndexingServiceTest {

  private final TsvRoutines tsvRoutines = new TsvReader().createTsvReader();
  @Mock
  private IndexWriter indexWriter;
  @Mock
  private IndexingProperties properties;
  @Mock
  private NameParserGbifV1 nameParserGbifV1;
  @Mock
  private ColDpDownloadingService colDpDownloadingService;

  private ColDpIndexingService service;

  @BeforeEach
  void setup() {
    service = new ColDpIndexingService(tsvRoutines, indexWriter, properties, nameParserGbifV1,
        colDpDownloadingService);
  }

  @Test
  void testFullIndexing() throws Exception {
    // Given
    given(properties.isIndexAtStartup()).willReturn(true);
    given(colDpDownloadingService.downloadColDpDataset()).willReturn(Path.of("src/test/resources/test.zip"));

    // When / Then
    service.setup();
  }
}
