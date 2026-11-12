package eu.dissco.nusearch.service;

import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.RabbitMqProperties;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@AllArgsConstructor
public class RabbitMqPublisherService {

	private final JsonMapper mapper;

	private final RabbitTemplate rabbitTemplate;

	private final RabbitMqProperties rabbitMqProperties;

	public void sendMessage(DigitalSpecimenEvent event) {
		rabbitTemplate.convertAndSend(rabbitMqProperties.getExchangeName(), rabbitMqProperties.getRoutingKeyName(),
				mapper.writeValueAsString(event));
	}

}
