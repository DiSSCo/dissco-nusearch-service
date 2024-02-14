package eu.dissco.nusearch.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("kafka.publisher")
public class KafkaProducerProperties {

  @NotBlank
  private String host;

  @NotBlank
  private String topic = "digital-specimen";

}
