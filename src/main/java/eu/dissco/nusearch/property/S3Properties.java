package eu.dissco.nusearch.property;

import eu.dissco.nusearch.Profiles;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Profile({Profiles.S3_RESOLVER, Profiles.S3_INDEXER})
@ConfigurationProperties(prefix = "s3")
public class S3Properties {

  @NotBlank
  private String accessKey;

  @NotBlank
  private String accessSecret;

}
