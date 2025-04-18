package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.RabbitMqProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Profile({STANDALONE, S3_RESOLVER})
@AllArgsConstructor
public class RabbitMqPublisherService {

  private final ObjectMapper mapper;
  private final RabbitTemplate rabbitTemplate;
  private final RabbitMqProperties rabbitMqProperties;

  public void sendMessage(DigitalSpecimenEvent event) {
    try {
      rabbitTemplate.convertAndSend(rabbitMqProperties.getExchangeName(),
          rabbitMqProperties.getRoutingKeyName(), mapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      log.error("Unable to send message: {}, parsing to string failed", event, e);
    }
  }

  public void sendMessageDLQ(Object message) {
    rabbitTemplate.convertAndSend(rabbitMqProperties.getDlqExchangeName(),
        rabbitMqProperties.getDlqRoutingKeyName(), message);
  }
}
