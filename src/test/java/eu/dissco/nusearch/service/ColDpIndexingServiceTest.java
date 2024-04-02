package eu.dissco.nusearch.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.univocity.parsers.tsv.TsvRoutines;
import eu.dissco.nusearch.Profiles;
import eu.dissco.nusearch.configuration.TsvReader;
import eu.dissco.nusearch.property.IndexingProperties;
import eu.dissco.nusearch.repository.StorageRepositoryInterface;
import java.nio.file.Path;
import org.apache.lucene.index.IndexWriter;
import org.gbif.nameparser.NameParserGbifV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

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
  @Mock
  private Environment environment;
  @Mock
  private StorageRepositoryInterface storageRepository;

  private ColDpIndexingService service;

  @BeforeEach
  void setup() {
    service = new ColDpIndexingService(tsvRoutines, indexWriter, properties, nameParserGbifV1,
        colDpDownloadingService, environment, storageRepository);
  }

  @Test
  void testFullIndexing() throws Exception {
    // Given
    given(colDpDownloadingService.downloadColDpDataset()).willReturn(Path.of("src/test/resources/test.zip"));

    // When / Then
    service.setup();
  }

  @Test
  void testS3Indexing() throws Exception {
    // Given
    given(environment.matchesProfiles(Profiles.S3_RESOLVER)).willReturn(false);
    given(colDpDownloadingService.downloadColDpDataset()).willReturn(Path.of("src/test/resources/test.zip"));
    given(environment.matchesProfiles(Profiles.S3_INDEXER)).willReturn(true);

    // When
    service.setup();

    // Then
    then(storageRepository).should().uploadIndex(properties.getIndexLocation());
  }

  @Test
  void testS3Resolver() throws Exception {
    // Given
    given(environment.matchesProfiles(Profiles.S3_RESOLVER)).willReturn(true);

    // When
    service.setup();

    // Then
    then(storageRepository).should().downloadIndex(properties.getIndexLocation());
  }
}
