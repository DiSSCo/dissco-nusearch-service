package eu.dissco.nusearch.domain.response;

import java.util.List;
import java.util.Map;

public record Diagnostics(
    MatchType matchType,
    List<String> issues,
    List<String> processingFlags,
    Integer confidence,
    String note,
    long timeTaken,
    List<NameUsageMatch> alternatives,
    Map<String, Long> timings,
    ExternalId matchedID) {

}
