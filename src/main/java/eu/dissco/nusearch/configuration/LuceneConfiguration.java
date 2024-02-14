package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.component.ScientificNameAnalyzer;
import eu.dissco.nusearch.property.IndexingProperties;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@AllArgsConstructor
public class LuceneConfiguration {

  private final IndexingProperties properties;

  @Bean
  public IndexWriter cofigureIndexWriter() throws IOException {
    var indexWriterConfig = new IndexWriterConfig(new ScientificNameAnalyzer());
    var indexDirectory = FSDirectory.open(Paths.get(properties.getIndexLocation()));
    return new IndexWriter(indexDirectory, indexWriterConfig);
  }

  @Bean
  @DependsOn("colDpIndexingService")
  public IndexSearcher configureIndexSearcher() throws IOException {
    var indexDirectory = FSDirectory.open(Paths.get(properties.getIndexLocation()));
    var indexReader = DirectoryReader.open(indexDirectory);
    return new IndexSearcher(indexReader, Executors.newVirtualThreadPerTaskExecutor());
  }
}
