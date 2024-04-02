package eu.dissco.nusearch.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfiguration {
  @Bean
  public ExecutorService threadPoolTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
