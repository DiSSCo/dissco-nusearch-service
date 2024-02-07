package eu.dissco.nusearch.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.nameparser.NameParserGbifV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public NameParserGbifV1 nameParserGbifV1() {
    return new NameParserGbifV1(20000);
  }

  @Bean
  public ObjectMapper mapper() {
    var mapper =new ObjectMapper();
    mapper.findAndRegisterModules();
    mapper.setSerializationInclusion(Include.NON_NULL);
    return mapper;
  }
}
