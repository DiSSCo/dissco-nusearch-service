package eu.dissco.nusearch.domain.matching.request;

public record ClassificationQuery(String kingdom, String phylum, String clazz, String order, String superfamily,
		String family, String subfamily, String tribe, String subtribe, String genus, String subgenus, String species) {
}
