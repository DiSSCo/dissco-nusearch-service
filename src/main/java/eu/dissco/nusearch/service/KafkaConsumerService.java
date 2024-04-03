package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile({STANDALONE, S3_RESOLVER})
@AllArgsConstructor
public class KafkaConsumerService {

  private final ObjectMapper mapper;
  private final DigitalSpecimenMatchingService service;


  @KafkaListener(topics = "${kafka.consumer.topic}")
  public void getMessages(@Payload List<String> messages) {
    var events = messages.stream().map(message -> {
      try {
        return mapper.readValue(message, DigitalSpecimenEvent.class);
      } catch (JsonProcessingException e) {
        log.error("Moving message to DLQ, failed to parse event message", e);
        return null;
      }
    }).filter(Objects::nonNull).toList();
    service.handleMessages(events);
  }

}
