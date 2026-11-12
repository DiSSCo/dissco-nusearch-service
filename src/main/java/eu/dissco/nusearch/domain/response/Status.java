package eu.dissco.nusearch.domain.response;

public record Status(String clbDatasetKey, String datasetAlias, String datasetKey, String status, String statusCode,
		String sourceId) {

}
