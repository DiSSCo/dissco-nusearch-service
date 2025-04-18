package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.RabbitMqProperties;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Profile({STANDALONE, S3_RESOLVER})
@AllArgsConstructor
public class RabbitMqService {

  private final ObjectMapper mapper;
  private final RabbitTemplate rabbitTemplate;
  private final DigitalSpecimenMatchingService service;
  private final RabbitMqProperties rabbitMqProperties;

  @RabbitListener(queues = "#{rabbitMQProperties.queueName}", containerFactory = "consumerBatchContainerFactory")
  public void getMessages(@Payload List<String> messages) {
    var events = messages.stream().map(message -> {
      try {
        return mapper.readValue(message, DigitalSpecimenEvent.class);
      } catch (JsonProcessingException e) {
        log.error("Moving message to DLQ, failed to parse event message: {}", message, e);
        sendMessageDLQ(message);
        return null;
      }
    }).filter(Objects::nonNull).toList();
    service.handleMessages(events);
  }

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
