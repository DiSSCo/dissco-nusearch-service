package eu.dissco.nusearch.component;

import static eu.dissco.nusearch.configuration.ApplicationConfiguration.DATE_STRING;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateSerializer extends JsonSerializer<Date> {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_STRING).withZone(
      ZoneOffset.UTC);

  @Override
  public void serialize(Date value, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) {
    try {
      jsonGenerator.writeString(formatter.format(value.toInstant()));
    } catch (IOException e) {
      log.error("An error has occurred serializing a date. More information: {}", e.getMessage());
    }
  }
}
