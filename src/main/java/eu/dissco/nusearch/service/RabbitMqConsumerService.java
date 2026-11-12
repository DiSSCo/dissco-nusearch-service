package eu.dissco.nusearch.service;

import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@AllArgsConstructor
public class RabbitMqConsumerService {

	private final JsonMapper mapper;

	private final DigitalSpecimenMatchingService service;

	@RabbitListener(queues = { "${rabbitmq.queue-name:nu-search-queue}" },
			containerFactory = "consumerBatchContainerFactory")
	public void getMessages(@Payload List<String> messages) {
		var events = messages.stream().map(message -> mapper.readValue(message, DigitalSpecimenEvent.class)).toList();
		service.handleMessages(events);
	}

}
