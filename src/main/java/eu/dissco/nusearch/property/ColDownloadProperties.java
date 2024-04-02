package eu.dissco.nusearch.property;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "col")
public class ColDownloadProperties {

  private boolean synonyms = true;

  private boolean extended = true;

  private boolean extinct = true;

  @Positive
  private int exportStatusRetryTime = 500;

  @Positive
  private int exportStatusRetryCount = 10;

}
