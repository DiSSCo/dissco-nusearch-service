package eu.dissco.nusearch.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

  @NotBlank
  private String name = "dissco-nusearch-service";

  @NotBlank
  private String pid = "https://hdl.handle.net/TEST/123-123-123";

  @Positive
  private int nameParserMaxThreads = 1000;

  @Positive
  private int nameParserThreadTimeOut = 100;

}
