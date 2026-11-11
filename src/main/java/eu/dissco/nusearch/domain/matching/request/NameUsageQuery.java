package eu.dissco.nusearch.domain.matching.request;

import lombok.Builder;
import org.gbif.nameparser.api.Rank;

import java.util.Set;

@Builder
public record NameUsageQuery(
        String usageKey,
        String taxonID,
        String taxonConceptID,
        String scientificNameID,
        String scientificName,
        String authorship,
        String genericName,
        String specificEpithet,
        String infraSpecificEpithet,
        Rank rank,
        ClassificationQuery classification,
        Set<String> exclude,
        Boolean strict,
        Boolean verbose) {
}
