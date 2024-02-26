package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.nusearch.schema.DigitalSpecimen;

public record DigitalSpecimenWrapper(
    @JsonProperty("ods:normalisedPhysicalSpecimenId")
    String physicalSpecimenId,
    @JsonProperty("ods:type")
    String type,
    @JsonProperty("ods:attributes")
    DigitalSpecimen attributes,
    @JsonProperty("ods:originalAttributes")
    JsonNode originalAttributes) {

}
