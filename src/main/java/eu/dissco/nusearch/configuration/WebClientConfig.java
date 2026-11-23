package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.client.MatchingClient;
import eu.dissco.nusearch.property.MatchingApiProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
@EnableResilientMethods
@AllArgsConstructor
public class WebClientConfig {

	private final MatchingApiProperties properties;

	@Bean
	public MatchingClient matchingClient() {
		var httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(properties.getConnectTimeOutInSec()))
			.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeOutInSec()));

		var restClient = RestClient.builder().baseUrl(properties.getEndpoint()).requestFactory(requestFactory).build();

		var proxyFactory = HttpServiceProxyFactory.builder()
			.exchangeAdapter(RestClientAdapter.create(restClient))
			.build();
		return proxyFactory.createClient(MatchingClient.class);
	}

	@EventListener
	public void logRetry(MethodRetryEvent event) {
		if (event.isRetryAborted()) {
			log.error("Matching request failed, no more retries", event.getFailure());
		}
		else {
			log.warn("Matching request failed, retrying: {}", event.getFailure().getMessage());
		}
	}

}
