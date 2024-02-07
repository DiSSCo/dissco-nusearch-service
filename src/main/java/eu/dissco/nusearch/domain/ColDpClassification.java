package eu.dissco.nusearch.domain;

import lombok.Data;
import org.gbif.api.vocabulary.TaxonomicStatus;

@Data
public class ColDpClassification {

  private String colId;
  private String scientificName;
  private String authorship;
  private String rank;
  private TaxonomicStatus status;
  private boolean extinct;

}
