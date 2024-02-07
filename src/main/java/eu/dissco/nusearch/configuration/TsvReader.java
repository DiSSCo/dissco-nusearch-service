package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.domain.NameUsageCsvRow;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.tsv.TsvParserSettings;
import com.univocity.parsers.tsv.TsvRoutines;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TsvReader {

  @Bean
  public TsvRoutines createTsvReader(){
    var rowProcessor = new BeanListProcessor<>(NameUsageCsvRow.class);
    var tsvSettings = new TsvParserSettings();
    tsvSettings.setProcessor(rowProcessor);
    tsvSettings.setMaxCharsPerColumn(49152);
    tsvSettings.setLineSeparatorDetectionEnabled(true);
    return new TsvRoutines(tsvSettings);
  }
}
