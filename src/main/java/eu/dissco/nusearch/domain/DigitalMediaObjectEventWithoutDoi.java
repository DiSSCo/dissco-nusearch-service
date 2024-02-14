package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DigitalMediaObjectEventWithoutDoi(
    List<String> enrichmentList,
    @JsonProperty("digitalMediaObject")
    DigitalMediaObjectWithoutDoi digitalMediaObjectWithoutDoi) {

}
