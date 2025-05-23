package eu.dissco.nusearch.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

  @Positive
  private int batchSize = 500;

  @NotBlank
  private String queueName = "nu-search-queue";

  @NotBlank
  private String dlqExchangeName = "nu-search-exchange-dlq";

  @NotBlank
  private String dlqRoutingKeyName = "nu-search-dlq";

  @NotBlank
  private String exchangeName = "digital-specimen-exchange";

  @NotNull
  private String routingKeyName = "digital-specimen";

}
