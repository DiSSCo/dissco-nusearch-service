package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Data;
import org.gbif.api.model.common.LinneanClassification;

@Data
public class NameUsageRequest {

  @JsonProperty("classification")
  private LinneanClassification classification;
  @JsonProperty("exclude")
  private Set<String> exclude;
  @JsonProperty("strict")
  private Boolean strict;
  @JsonProperty("verbose")
  private Boolean verbose;
  @JsonProperty("usageKey")
  private String usageKey;
  @JsonProperty("name")
  private String scientificName2;
  @JsonProperty("scientificName")
  private String scientificName;
  @JsonProperty("authorship")
  private String authorship2;
  @JsonProperty("scientificNameAuthorship")
  private String authorship;
  @JsonProperty("rank")
  private String rank2;
  @JsonProperty("taxonRank")
  private String rank;
  @JsonProperty("genericName")
  private String genericName;
  @JsonProperty("specificEpithet")
  private String specificEpithet;
  @JsonProperty("infraspecificEpithet")
  private String infraspecificEpithet;
}
