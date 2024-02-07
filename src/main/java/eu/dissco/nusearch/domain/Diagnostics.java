package eu.dissco.nusearch.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.vocabulary.TaxonomicStatus;

@Data
public class Diagnostics {

  private NameUsageMatch.MatchType matchType;
  private Integer confidence;
  private TaxonomicStatus status;
  private List<String> lineage = new ArrayList<>();
  private List<ColNameUsageMatch2> alternatives = new ArrayList<>();
  private String note;

}
