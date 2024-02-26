package eu.dissco.nusearch.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.dissco.nusearch.component.DateDeserializer;
import eu.dissco.nusearch.component.DateSerializer;
import java.util.Date;
import org.gbif.nameparser.NameParserGbifV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  public static final String DATE_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  @Bean
  public NameParserGbifV1 nameParserGbifV1() {
    return new NameParserGbifV1(20000);
  }

  @Bean
  public ObjectMapper mapper() {
    var mapper = new ObjectMapper().findAndRegisterModules();
    SimpleModule dateModule = new SimpleModule();
    dateModule.addSerializer(Date.class, new DateSerializer());
    dateModule.addDeserializer(Date.class, new DateDeserializer());
    mapper.registerModule(dateModule);
    mapper.setSerializationInclusion(Include.NON_NULL);
    return mapper;
  }
}
