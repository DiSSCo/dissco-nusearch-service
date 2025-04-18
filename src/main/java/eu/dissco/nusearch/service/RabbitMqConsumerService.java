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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Profile({STANDALONE, S3_RESOLVER})
@AllArgsConstructor
public class RabbitMqConsumerService {

  private final ObjectMapper mapper;
  private final RabbitMqPublisherService publisherService;
  private final DigitalSpecimenMatchingService service;

  @RabbitListener(queues = {
      "${rabbitmq.queue-name:nu-search-queue}"}, containerFactory = "consumerBatchContainerFactory")
  public void getMessages(@Payload List<String> messages) {
    var events = messages.stream().map(message -> {
      try {
        return mapper.readValue(message, DigitalSpecimenEvent.class);
      } catch (JsonProcessingException e) {
        log.error("Moving message to DLQ, failed to parse event message: {}", message, e);
        publisherService.sendMessageDLQ(message);
        return null;
      }
    }).filter(Objects::nonNull).toList();
    service.handleMessages(events);
  }

}
