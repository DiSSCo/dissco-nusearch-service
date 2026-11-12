package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.client.MatchingClient;
import eu.dissco.nusearch.property.MatchingApiProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
@AllArgsConstructor
public class WebClientConfig {

	private static final int MAX_RETRIES = 3;

	private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

	private final MatchingApiProperties properties;

	private static ClientHttpResponse retry(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		for (int attempt = 1;; attempt++) {
			try {
				var response = execution.execute(request, body);
				if (response.getStatusCode().is2xxSuccessful() || attempt > MAX_RETRIES) {
					return response;
				}
				log.warn("Matching request failed with {}, retry {} of {}", response.getStatusCode(), attempt,
						MAX_RETRIES);
				response.close();
			}
			catch (IOException e) {
				if (attempt > MAX_RETRIES) {
					throw e;
				}
				log.warn("Matching request failed with {}, retry {} of {}", e, attempt, MAX_RETRIES);
			}
			sleep();
		}
	}

	private static void sleep() throws IOException {
		try {
			Thread.sleep(RETRY_DELAY);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting to retry", e);
		}
	}

	@Bean
	public MatchingClient matchingClient() {
		var httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(properties.getConnectTimeOutInSec()))
			.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeOutInSec()));

		var restClient = RestClient.builder()
			.baseUrl(properties.getEndpoint())
			.requestFactory(requestFactory)
			.requestInterceptor(WebClientConfig::retry)
			.build();

		var proxyFactory = HttpServiceProxyFactory.builder()
			.exchangeAdapter(RestClientAdapter.create(restClient))
			.build();
		return proxyFactory.createClient(MatchingClient.class);
	}

}
