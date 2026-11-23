package eu.dissco.nusearch.domain.response;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

public record Usage(String key, String name, String canonicalName, String authorship, Rank rank, NomCode code,
		String status, String uninomial, String genericName, String infragenericEpithet, String specificEpithet,
		String infraspecificEpithet, String type, String formattedName) {

}
