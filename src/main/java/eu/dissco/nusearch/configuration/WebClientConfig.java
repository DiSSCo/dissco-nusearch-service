package eu.dissco.nusearch.configuration;

import eu.dissco.nusearch.client.MatchingClient;
import eu.dissco.nusearch.property.MatchingApiProperties;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@AllArgsConstructor
public class WebClientConfig {

    private final MatchingApiProperties properties;

    @Bean
    public MatchingClient matchingClient() {
        // Initialize HTTP client
        RestClient restClient = RestClient.create(properties.getEndpoint());

        // Create factory for client proxies
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        // Create client proxy
        return proxyFactory.createClient(MatchingClient.class);
    }

}
