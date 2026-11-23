package eu.dissco.nusearch.client;

import eu.dissco.nusearch.domain.request.NameUsageQuery;
import eu.dissco.nusearch.domain.response.NameUsageMatch;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface MatchingClient {

	/**
	 * Retries only on server errors (5xx) and I/O errors such as timeouts; client errors
	 * (4xx) fail directly as a retry gives the same result. The defaults here must match
	 * those in MatchingApiProperties.
	 */
	@Retryable(includes = { HttpServerErrorException.class, ResourceAccessException.class },
			maxRetriesString = "${matching-api.max-retries:3}", delayString = "${matching-api.retry-delay-in-sec:2}",
			timeUnit = TimeUnit.SECONDS, multiplier = 2, maxDelay = 30)
	@PostExchange("/v2/species/match")
	List<NameUsageMatch> getNameMatches(@RequestBody List<NameUsageQuery> names);

}
