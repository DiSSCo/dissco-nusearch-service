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
  private String name = "DiSSCo Name Usage Search Service";

  @NotBlank
  private String pid = "https://doi.org/10.5281/zenodo.14380476";

  @Positive
  private int nameParserMaxThreads = 1000;

  @Positive
  private int nameParserThreadTimeOut = 100;

}
