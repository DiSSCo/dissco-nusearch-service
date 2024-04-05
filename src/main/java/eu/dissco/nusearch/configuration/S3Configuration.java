package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.Profiles;
import eu.dissco.nusearch.property.S3Properties;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
@AllArgsConstructor
@Profile({Profiles.S3_RESOLVER, Profiles.S3_INDEXER})
public class S3Configuration {

  private final S3Properties s3Properties;

  @Bean
  public S3AsyncClient s3Client() {
    return S3AsyncClient.crtBuilder()
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.getAccessKey(),
                s3Properties.getAccessSecret())))
        .region(Region.EU_WEST_2)
        .build();
  }

  @Bean
  public S3TransferManager transferManager() {
    return S3TransferManager.builder().s3Client(s3Client()).build();
  }

}
