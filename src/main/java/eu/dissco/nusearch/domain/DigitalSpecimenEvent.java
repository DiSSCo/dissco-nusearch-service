package eu.dissco.nusearch.domain;

import java.util.List;

public record DigitalSpecimenEvent(
    List<String> enrichmentList,
    DigitalSpecimenWrapper digitalSpecimenWrapper,
    List<DigitalMediaObjectEventWithoutDoi> digitalMediaObjectEvents) {

}
