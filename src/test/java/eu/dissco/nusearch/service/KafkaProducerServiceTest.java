package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.TestUtils.MAPPER;
import static eu.dissco.nusearch.TestUtils.givenDigitalSpecimenEvent;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.nusearch.property.KafkaProducerProperties;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;
  @Mock
  private SendResult<String, String> sendResult;
  @Mock
  private KafkaProducerProperties properties;
  private KafkaProducerService service;

  @BeforeEach
  void setup() {
    this.service = new KafkaProducerService(kafkaTemplate, MAPPER, properties);
  }

  @Test
  void testSendMessage() throws JsonProcessingException {
    // Given
    var future = CompletableFuture.completedFuture(sendResult);
    given(kafkaTemplate.send(anyString(), anyString())).willReturn(future);
    var digitalSpecimenEvent = givenDigitalSpecimenEvent();
    given(properties.getTopic()).willReturn("test-topic");

    // When
    service.sendMessage(digitalSpecimenEvent);

    // Then
    then(kafkaTemplate).should()
        .send("test-topic", MAPPER.writeValueAsString(digitalSpecimenEvent));
  }



}
