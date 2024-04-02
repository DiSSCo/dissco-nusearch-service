package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
@Profile({Profiles.S3_RESOLVER, Profiles.S3_INDEXER})
public class S3Configuration {

  @Bean
  public S3AsyncClient s3Client() {
    return S3AsyncClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.EU_WEST_1)
        .build();
  }

  @Bean
  public S3TransferManager transferManager() {
    return S3TransferManager.builder().s3Client(s3Client()).build();
  }

}
