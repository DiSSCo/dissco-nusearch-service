package eu.dissco.nusearch.domain;

import com.univocity.parsers.annotations.Parsed;
import lombok.Data;

@Data
public class NameUsageCsvRow {

  @Parsed(field = "col:ID")
  private String id;

  @Parsed(field = "col:parentID")
  private String parentId;

  @Parsed(field = "col:basionymID")
  private String basionymId;

  @Parsed(field = "col:status")
  private String status;

  @Parsed(field = "col:rank")
  private String rank;

  @Parsed(field = "col:scientificName")
  private String scientificName;

  @Parsed(field = "col:authorship")
  private String authorship;

  @Parsed(field = "col:specificEpithet")
  private String specificEpithet;

  @Parsed(field = "col:genericName")
  private String genericName;

  @Parsed(field = "col:code")
  private String code;

  @Parsed(field = "col:nameStatus")
  private String nameStatus;

  @Parsed(field = "col:extinct")
  private String extinct;

}
