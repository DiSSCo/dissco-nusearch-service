package eu.dissco.nusearch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.KafkaProducerProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
