package eu.dissco.nusearch.domain.response;

import java.util.List;

public record NameUsageMatch(Usage usage, Usage acceptedUsage, List<RankedName> classification, Diagnostics diagnostics,
		List<Status> additionalStatus, List<String> environments, boolean synonym, Long left, Long right) {

}
