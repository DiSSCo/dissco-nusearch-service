package eu.dissco.nusearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NuSearchApplication {

	static void main(String[] args) {
		SpringApplication.run(NuSearchApplication.class, args);
	}

}
