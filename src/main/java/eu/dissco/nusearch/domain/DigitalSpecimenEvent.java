package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record DigitalSpecimenEvent(
    List<String> enrichmentList,
    DigitalSpecimenWrapper digitalSpecimenWrapper,
    List<JsonNode> digitalMediaObjectEvents) {

}
