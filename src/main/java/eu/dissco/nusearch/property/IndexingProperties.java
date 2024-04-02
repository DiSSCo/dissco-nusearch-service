package eu.dissco.nusearch.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "indexing")
public class IndexingProperties {

  @Positive
  private int colDataset;

  @NotBlank
  private String colUsername;

  @NotBlank
  private String colPassword;

  private String indexLocation = "src/main/resources/index";

  private String tempColdpLocation = "src/main/resources/sample.zip";
}
