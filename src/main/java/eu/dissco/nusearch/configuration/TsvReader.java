package eu.dissco.nusearch.configuration;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.tsv.TsvParserSettings;
import com.univocity.parsers.tsv.TsvRoutines;
import eu.dissco.nusearch.domain.NameUsageCsvRow;
import eu.dissco.nusearch.property.IndexingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TsvReader {

  private final IndexingProperties indexingProperties;

  @Bean
  public TsvRoutines createTsvReader(){
    var rowProcessor = new BeanListProcessor<>(NameUsageCsvRow.class);
    var tsvSettings = new TsvParserSettings();
    tsvSettings.setProcessor(rowProcessor);
    tsvSettings.setMaxCharsPerColumn(indexingProperties.getMaxCharsPerColumn());
    tsvSettings.setLineSeparatorDetectionEnabled(true);
    return new TsvRoutines(tsvSettings);
  }
}
