package eu.dissco.nusearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
@ConfigurationPropertiesScan
public class NuSearchApplication {

  public static void main(String[] args) {
    var context = SpringApplication.run(NuSearchApplication.class, args);
    if (context.getEnvironment().matchesProfiles(Profiles.S3_INDEXER)) {
      context.close();
    }
  }

}
