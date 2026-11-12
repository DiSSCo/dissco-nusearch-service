package eu.dissco.nusearch.domain.response;

import java.util.List;

public record ExternalId(String id, String mainIndexID, String datasetKey, String clbDatasetKey, String datasetTitle,
		String parentID, String scientificName, String acceptedTaxonID, String rank, String status,
		List<String> recognizedVariants) {

}
