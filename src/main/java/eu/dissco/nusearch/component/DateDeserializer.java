package eu.dissco.nusearch.component;

import static eu.dissco.nusearch.configuration.ApplicationConfiguration.DATE_STRING;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateDeserializer extends JsonDeserializer<Date> {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_STRING).withZone(
      ZoneOffset.UTC);

  @Override
  public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    try {
      return Date.from(Instant.from(formatter.parse(jsonParser.getText())));
    } catch (IOException e) {
      log.error("An error has occurred deserializing a date. More information: {}", e.getMessage());
      return null;
    }
  }
}
