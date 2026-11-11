package eu.dissco.nusearch.utils;

import eu.dissco.nusearch.configuration.LuceneConfiguration;
import eu.dissco.nusearch.configuration.TsvReader;
import eu.dissco.nusearch.property.IndexingProperties;
import eu.dissco.nusearch.repository.StorageRepositoryInterface;
import eu.dissco.nusearch.service.ColDpDownloadingService;
import eu.dissco.nusearch.service.ColDpIndexingService;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.NameParserGbifV1;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Builds a Lucene index from a COL DP zip with the production indexing code, for tests that need
 * a real index. This is required as each Lucene version needs an index also created by that specific version.
 */
public final class TestIndexBuilder {

  /** COL DP 1011, Fulgoromorpha Lists On the Web. */
  public static final Path TEST_COL_DP = Path.of("src/test/resources/test.zip");

  private TestIndexBuilder() {
    // Utility Class
  }

  public static IndexingProperties buildIndex(Path indexDir) throws Exception {
    return buildIndex(indexDir, TEST_COL_DP,
        new NameParserGbifV1(new NameParserGBIF(10_000, 0, 1000)));
  }

  public static IndexingProperties buildIndex(Path indexDir, Path colDp, NameParserGbifV1 parser)
      throws Exception {
    var properties = new IndexingProperties();
    properties.setIndexLocation(indexDir.toString());
    var downloader = mock(ColDpDownloadingService.class);
    given(downloader.downloadColDpDataset()).willReturn(colDp);
    var indexer = new ColDpIndexingService(new TsvReader(properties).createTsvReader(),
        new LuceneConfiguration(properties).cofigureIndexWriter(), properties, parser, downloader,
        mock(Environment.class), mock(StorageRepositoryInterface.class));
    indexer.setup();
    return properties;
  }
}
