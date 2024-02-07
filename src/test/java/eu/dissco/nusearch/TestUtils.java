package eu.dissco.nusearch;

import static org.gbif.api.model.checklistbank.NameUsageMatch.MatchType.EXACT;
import static org.gbif.api.vocabulary.TaxonomicStatus.ACCEPTED;
import static org.gbif.api.vocabulary.TaxonomicStatus.SYNONYM;

import eu.dissco.nusearch.domain.ColDpClassification;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.domain.ColDpRankedName;
import eu.dissco.nusearch.domain.ColNameUsageMatch2;
import eu.dissco.nusearch.domain.Diagnostics;
import java.util.List;
import org.gbif.api.vocabulary.Rank;

public class TestUtils {

  public static ColDpNameUsageMatch givenColDpNameUsageMatch() {
    ColDpNameUsageMatch colDpNameUsageMatch = new ColDpNameUsageMatch();
    colDpNameUsageMatch.setConfidence(100);
    colDpNameUsageMatch.setNote(null);
    colDpNameUsageMatch.setMatchType(EXACT);
    colDpNameUsageMatch.setCanonicalName("Aa brevis");
    colDpNameUsageMatch.setColId("7Q8L8");
    colDpNameUsageMatch.setColParentId("73SWK");
    colDpNameUsageMatch.setRank(Rank.SPECIES);
    colDpNameUsageMatch.setTaxonomicStatus(SYNONYM);
    colDpNameUsageMatch.setScientificName("Aa brevis");
    colDpNameUsageMatch.setAuthorship("Schltr.");
    colDpNameUsageMatch.setSpecificEpithet("brevis");
    colDpNameUsageMatch.setGenericName("Aa");
    colDpNameUsageMatch.setCode("botanical");
    colDpNameUsageMatch.setKingdom("Plantae");
    colDpNameUsageMatch.setPhylum("Tracheophyta");
    colDpNameUsageMatch.setClazz("Liliopsida");
    colDpNameUsageMatch.setOrder("Asparagales");
    colDpNameUsageMatch.setFamily("Orchidaceae");
    colDpNameUsageMatch.setGenus("Myrosmodes");
    colDpNameUsageMatch.setSubgenus(null);
    colDpNameUsageMatch.setSpecies("Myrosmodes brevis");
    colDpNameUsageMatch.setAlternatives(null);
    colDpNameUsageMatch.setClassifications(givenColDpClassification());
    return colDpNameUsageMatch;
  }

  private static List<ColDpClassification> givenColDpClassification() {
    return List.of(givenClassificationKingdom(), givenClassificationPhylum(),
        givenClassificationClass(),
        givenClassificationOrder(), givenClassificationFamily(), givenClassificationGenus(),
        givenClassificationUnranked());
  }

  private static ColDpClassification givenClassificationUnranked() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("5T6MX");
    colDpClassification.setScientificName("Biota");
    colDpClassification.setAuthorship(null);
    colDpClassification.setRank("unranked");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  private static ColDpClassification givenClassificationGenus() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("8VZKC");
    colDpClassification.setScientificName("Myrosmodes");
    colDpClassification.setAuthorship("Rchb.f.");
    colDpClassification.setRank("genus");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  private static ColDpClassification givenClassificationFamily() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("DPL");
    colDpClassification.setScientificName("Orchidaceae");
    colDpClassification.setAuthorship(null);
    colDpClassification.setRank("family");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  private static ColDpClassification givenClassificationOrder() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("SP");
    colDpClassification.setScientificName("Asparagales");
    colDpClassification.setAuthorship(null);
    colDpClassification.setRank("order");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  private static ColDpClassification givenClassificationClass() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("L2L");
    colDpClassification.setScientificName("Liliopsida");
    colDpClassification.setAuthorship(null);
    colDpClassification.setRank("class");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  private static ColDpClassification givenClassificationPhylum() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("TP");
    colDpClassification.setScientificName("Tracheophyta");
    colDpClassification.setAuthorship(null);
    colDpClassification.setRank("phylum");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  private static ColDpClassification givenClassificationKingdom() {
    var colDpClassification = new ColDpClassification();
    colDpClassification.setColId("P");
    colDpClassification.setScientificName("Plantae");
    colDpClassification.setAuthorship(null);
    colDpClassification.setRank("kingdom");
    colDpClassification.setStatus(ACCEPTED);
    colDpClassification.setExtinct(false);
    return colDpClassification;
  }

  public static ColNameUsageMatch2 createColNameUsageMatch2() {
    ColNameUsageMatch2 colNameUsageMatch2 = new ColNameUsageMatch2();
    colNameUsageMatch2.setSynonym(true);
    colNameUsageMatch2.setUsage(givenNameSynonymNameUsage());
    colNameUsageMatch2.setAcceptedUsage(givenAcceptedNameUsage());
    colNameUsageMatch2.setNomenclature(null);
    colNameUsageMatch2.setClassification(givenClassification());
    colNameUsageMatch2.setDiagnostics(givenDiagnostics());
    return colNameUsageMatch2;
  }

  private static Diagnostics givenDiagnostics() {
    var diagnostics = new Diagnostics();
    diagnostics.setConfidence(100);
    diagnostics.setStatus(SYNONYM);
    return diagnostics;
  }

  private static List<ColDpRankedName> givenClassification() {
    return List.of(givenKingdom(), givenClass(), givenFamily(), givenPhylum(), givenUnranked(),
        givenGenus(), givenOrder());
  }

  private static ColDpRankedName givenOrder() {
    var name = new ColDpRankedName();
    name.setColId("SP");
    name.setScientificName("Asparagales");
    name.setRank("order");
    name.setExtinct(false);
    name.setLabel("Asparagales");
    name.setLabelHtml("Asparagales");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenGenus() {
    var name = new ColDpRankedName();
    name.setColId("8VZKC");
    name.setScientificName("Myrosmodes");
    name.setAuthorship("Rchb.f.");
    name.setRank("genus");
    name.setExtinct(false);
    name.setLabel("Myrosmodes Rchb.f.");
    name.setLabelHtml("<i>Myrosmodes</i> Rchb.f.");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenUnranked() {
    var name = new ColDpRankedName();
    name.setColId("5T6MX");
    name.setScientificName("Biota");
    name.setRank("unranked");
    name.setExtinct(false);
    name.setLabel("Biota");
    name.setLabelHtml("<i>Biota</i>");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenPhylum() {
    var name = new ColDpRankedName();
    name.setColId("TP");
    name.setScientificName("Tracheophyta");
    name.setRank("phylum");
    name.setExtinct(false);
    name.setLabel("Tracheophyta");
    name.setLabelHtml("Tracheophyta");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenFamily() {
    var name = new ColDpRankedName();
    name.setColId("DPL");
    name.setScientificName("Orchidaceae");
    name.setRank("family");
    name.setExtinct(false);
    name.setLabel("Orchidaceae");
    name.setLabelHtml("Orchidaceae");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenClass() {
    var name = new ColDpRankedName();
    name.setColId("L2L");
    name.setScientificName("Liliopsida");
    name.setRank("class");
    name.setExtinct(false);
    name.setLabel("Liliopsida");
    name.setLabelHtml("Liliopsida");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenKingdom() {
    var name = new ColDpRankedName();
    name.setColId("P");
    name.setScientificName("Plantae");
    name.setRank("kingdom");
    name.setExtinct(false);
    name.setLabel("Plantae");
    name.setLabelHtml("Plantae");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenAcceptedNameUsage() {
    var name = new ColDpRankedName();
    name.setColId("73SWK");
    name.setScientificName("Myrosmodes brevis");
    name.setAuthorship("(Schltr.) Garay");
    name.setRank("SPECIES");
    name.setExtinct(false);
    name.setLabel("Myrosmodes brevis (Schltr.) Garay");
    name.setLabelHtml("<i>Myrosmodes brevis</i> (Schltr.) Garay");
    name.setStatus(ACCEPTED);
    return name;
  }

  private static ColDpRankedName givenNameSynonymNameUsage() {
    var name = new ColDpRankedName();
    name.setColId("7Q8L8");
    name.setScientificName("Aa brevis");
    name.setAuthorship("Schltr.");
    name.setRank("SPECIES");
    name.setExtinct(false);
    name.setLabel("Aa brevis Schltr.");
    name.setLabelHtml("<i>Aa brevis</i> Schltr.");
    name.setStatus(SYNONYM);
    return name;
  }

}
