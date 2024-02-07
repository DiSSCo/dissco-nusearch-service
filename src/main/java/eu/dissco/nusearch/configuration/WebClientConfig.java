package eu.dissco.nusearch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient() {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
        .baseUrl("https://api.checklistbank.org/")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

}
