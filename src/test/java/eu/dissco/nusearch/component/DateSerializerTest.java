package eu.dissco.nusearch.component;

import static eu.dissco.nusearch.TestUtils.MAPPER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateSerializerTest {

  @Mock
  private JsonGenerator jsonGenerator;
  private final DateSerializer serializer = new DateSerializer();

  @Test
  void testSerializer() throws IOException {
    // Given
    var date = Date.from(Instant.parse("2024-02-14T07:45:22Z"));

    // When
    serializer.serialize(date, jsonGenerator, MAPPER.getSerializerProvider());

    // Then
    then(jsonGenerator).should().writeString("2024-02-14T07:45:22.000Z");
  }

  @Test
  void testSerializerIOException() throws IOException {
    // Given
    var date = Date.from(Instant.parse("2024-02-14T07:45:22Z"));
    willThrow(new IOException()).given(jsonGenerator).writeString(anyString());

    // When
    serializer.serialize(date, jsonGenerator, MAPPER.getSerializerProvider());

    // Then
  }
}
