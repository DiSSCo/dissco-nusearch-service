package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.nusearch.schema.DigitalEntity;


public record DigitalMediaObjectWithoutDoi(
    @JsonProperty("ods:type")
    String type,
    @JsonProperty("ods:physicalSpecimenId")
    String physicalSpecimenId,
    @JsonProperty("ods:attributes")
    DigitalEntity attributes,
    @JsonProperty("ods:originalAttributes")
    JsonNode originalAttributes) {

}
