package eu.dissco.nusearch.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "matching-api")
public class MatchingApiProperties {

	@NotBlank
	private String endpoint;

	@Positive
	private int timeOutInSec = 60;

	@Positive
	private int connectTimeOutInSec = 5;

}
