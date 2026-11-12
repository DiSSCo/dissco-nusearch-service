package eu.dissco.nusearch.client;

import eu.dissco.nusearch.domain.matching.request.NameUsageQuery;
import eu.dissco.nusearch.domain.response.NameUsageMatch;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface MatchingClient {

	@PostExchange("/v2/species/match")
	List<NameUsageMatch> getNameMatches(@RequestBody List<NameUsageQuery> names);

}
