package eu.dissco.nusearch.service;

import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.RabbitMqProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;


@Slf4j
@Service
@Profile({STANDALONE, S3_RESOLVER})
@AllArgsConstructor
public class RabbitMqPublisherService {

    private final JsonMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    public void sendMessage(DigitalSpecimenEvent event) {
        rabbitTemplate.convertAndSend(rabbitMqProperties.getExchangeName(),
                rabbitMqProperties.getRoutingKeyName(), mapper.writeValueAsString(event));
    }

    public void sendMessageDLQ(Object message) {
        rabbitTemplate.convertAndSend(rabbitMqProperties.getDlqExchangeName(),
                rabbitMqProperties.getDlqRoutingKeyName(), message);
    }
}
