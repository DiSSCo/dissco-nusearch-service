package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.KafkaProducerProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile({STANDALONE, S3_RESOLVER})
@AllArgsConstructor
public class KafkaProducerService {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;
  private final KafkaProducerProperties properties;

  public void sendMessage(DigitalSpecimenEvent event) {
    try {
      var future = kafkaTemplate.send(properties.getTopic(),
          mapper.writeValueAsString(event));
      future.whenComplete((result, ex) -> {
        if (ex != null) {
          log.error("Unable to send message: {}", event, ex);
        }
      });
    } catch (JsonProcessingException e) {
      log.error("Unable to send message: {}, parsing to string failed", event, e);
    }

  }
}
