package eu.dissco.nusearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

@Data
public class ColDpNameUsage implements LinneanClassification {

  private String colId;
  private String colParentId;
  private Rank rank;
  private TaxonomicStatus taxonomicStatus;
  private String scientificName;
  private String authorship;
  private String specificEpithet;
  private String genericName;
  private String code;
  private String nameStatus;
  private boolean extinct;
  private String kingdom;
  private String phylum;
  @JsonProperty("class")
  private String clazz;
  private String order;
  private String family;
  private String genus;
  private String subgenus;
  private String species;
  private List<ColDpClassification> classifications;

  @Nullable
  @Override
  public String getHigherRank(Rank rank) {
    return ClassificationUtils.getHigherRank(this, rank);
  }

  public String getHigherRankKey(Rank r) {
    for ( var classification :classifications) {
        if (classification.getRank().equals(r.toString().toLowerCase())) {
            return classification.getColId();
        }
    }
    return null;
  }
}
