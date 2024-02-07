package eu.dissco.nusearch.domain;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.gbif.api.model.checklistbank.NameUsageMatch.MatchType;


@Data
@EqualsAndHashCode(callSuper = true)
public class ColDpNameUsageMatch extends ColDpNameUsage {

  private Integer confidence;
  private String note;
  private MatchType matchType;
  private String canonicalName;
  private List<ColDpNameUsageMatch> alternatives;

}
