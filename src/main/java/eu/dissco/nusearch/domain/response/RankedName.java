package eu.dissco.nusearch.domain.response;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

public record RankedName(String key, String name, Rank rank, NomCode code) {

}
