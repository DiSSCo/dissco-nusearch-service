package eu.dissco.nusearch.domain;

import lombok.Data;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.nameparser.api.Rank;

@Data
public class ColDpRankedName {

  private String colId;
  private String scientificName;
  private String authorship;
  private String rank;
  private boolean extinct;
  private String label;
  private String labelHtml;
  private TaxonomicStatus status;

}
