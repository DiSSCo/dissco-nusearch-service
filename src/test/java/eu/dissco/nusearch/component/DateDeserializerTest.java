package eu.dissco.nusearch.component;

import static eu.dissco.nusearch.TestUtils.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateDeserializerTest {

  private final DateDeserializer deserializer = new DateDeserializer();
  @Mock
  private JsonParser jsonParser;

  @Test
  void testSerializer() throws IOException {
    // Given
    var stringDate = "2024-02-14T07:45:22.000Z";
    var expected = Date.from(Instant.parse(stringDate));
    given(jsonParser.getText()).willReturn(stringDate);

    // When
    var result = deserializer.deserialize(jsonParser, MAPPER.getDeserializationContext());

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testSerializerIOException() throws IOException {
    // Given
    given(jsonParser.getText()).willThrow(IOException.class);

    // When
    var result = deserializer.deserialize(jsonParser, MAPPER.getDeserializationContext());

    // Then
    assertThat(result).isNull();
  }
}
