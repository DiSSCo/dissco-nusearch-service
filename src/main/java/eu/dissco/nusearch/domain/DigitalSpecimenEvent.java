package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;

public record DigitalSpecimenEvent(
    Set<String> masList,
    DigitalSpecimenWrapper digitalSpecimenWrapper,
    List<JsonNode> digitalMediaEvents, Boolean forceMasSchedule) {

}
