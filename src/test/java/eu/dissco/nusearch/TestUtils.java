package eu.dissco.nusearch;

import static org.gbif.api.model.checklistbank.NameUsageMatch.MatchType.EXACT;
import static org.gbif.api.vocabulary.TaxonomicStatus.ACCEPTED;
import static org.gbif.api.vocabulary.TaxonomicStatus.SYNONYM;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.domain.ColDpClassification;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.domain.ColDpRankedName;
import eu.dissco.nusearch.domain.ColNameUsageMatch2;
import eu.dissco.nusearch.domain.Diagnostics;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.domain.DigitalSpecimenWrapper;
import eu.dissco.nusearch.schema.DigitalSpecimen;
import eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.nusearch.schema.Identification;
import eu.dissco.nusearch.schema.OdsHasTaxonIdentification;
import java.util.List;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

public class TestUtils {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  public static String INSTITUTION_ID = "https://ror.org/02y22ws83";
  public static String NORMALISED_PHYSICAL_SPECIMEN_ID = "http://coldb.mnhn.fr/catalognumber/mnhn/ec/ec10867";


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

  public static ColNameUsageMatch2 givenColDpNameUsageMatch2() {
    var colNameUsageMatch2 = new ColNameUsageMatch2();
    colNameUsageMatch2.setSynonym(true);
    colNameUsageMatch2.setUsage(givenColDpRankedName("7Q8L8", "Aa brevis", "Schltr.", "SPECIES",
        false, "Aa brevis Schltr.", "<i>Aa brevis</i> Schltr.", SYNONYM));
    colNameUsageMatch2.setAcceptedUsage(givenColDpRankedName("73SWK", "Myrosmodes brevis",
        "(Schltr.) Garay", "SPECIES", false, "Myrosmodes brevis (Schltr.) Garay",
        "<i>Myrosmodes brevis</i> (Schltr.) Garay", ACCEPTED));
    colNameUsageMatch2.setNomenclature(null);
    colNameUsageMatch2.setClassification(givenRankedNameClassification());
    colNameUsageMatch2.setDiagnostics(givenDiagnostics());
    return colNameUsageMatch2;
  }

  private static List<ColDpRankedName> givenRankedNameClassification() {
    return List.of(
        givenColDpRankedName("P", "Plantae", null, "kingdom", false, "Plantae", "Plantae",
            ACCEPTED),
        givenColDpRankedName("TP", "Tracheophyta", null, "phylum", false, "Tracheophyta", "Tracheophyta",
            ACCEPTED),
        givenColDpRankedName("L2L", "Liliopsida", null, "class", false, "Liliopsida", "Liliopsida",
            ACCEPTED),
        givenColDpRankedName("SP", "Asparagales", null, "order", false, "Asparagales", "Asparagales", ACCEPTED),
        givenColDpRankedName("DPL", "Orchidaceae", null, "family", false, "Orchidaceae", "Orchidaceae", ACCEPTED),
        givenColDpRankedName("8VZKC", "Myrosmodes", "Rchb.f.", "genus", false, "Myrosmodes Rchb.f.",
            "<i>Myrosmodes</i> Rchb.f.", ACCEPTED),
        givenColDpRankedName("5T6MX", "Biota", null, "unranked", false, "Biota", "<i>Biota</i>",
            ACCEPTED)
    );
  }

  private static ColDpRankedName givenColDpRankedName(String colId, String scientificName,
      String authorship, String rank, boolean extinct, String label, String labelHtml,
      TaxonomicStatus status) {
    var colDpRankedName = new ColDpRankedName();
    colDpRankedName.setColId(colId);
    colDpRankedName.setScientificName(scientificName);
    colDpRankedName.setAuthorship(authorship);
    colDpRankedName.setRank(rank);
    colDpRankedName.setExtinct(extinct);
    colDpRankedName.setLabel(label);
    colDpRankedName.setLabelHtml(labelHtml);
    colDpRankedName.setStatus(status);
    return colDpRankedName;
  }

  public static DigitalSpecimenEvent givenDigitalSpecimenEvent() {
    return givenDigitalSpecimenEvent(givenDigitalSpecimen());
  }
  public static DigitalSpecimenEvent givenDigitalSpecimenEvent(DigitalSpecimen digitalSpecimen) {
    return new DigitalSpecimenEvent(List.of("AAS"),
        new DigitalSpecimenWrapper(NORMALISED_PHYSICAL_SPECIMEN_ID,
            "https://doi.org/21.T11148/894b1e6cad57e921764e", digitalSpecimen,
            MAPPER.createObjectNode()),
        List.of(new ObjectMapper().createObjectNode()));
  }

  private static DigitalSpecimen givenDigitalSpecimen() {
    return new DigitalSpecimen()
        .withOdsNormalisedPhysicalSpecimenID(NORMALISED_PHYSICAL_SPECIMEN_ID)
        .withOdsOrganisationID(INSTITUTION_ID)
        .withDwcBasisOfRecord("PreservedSpecimen")
        .withOdsTopicDiscipline(OdsTopicDiscipline.UNCLASSIFIED)
        .withOdsSpecimenName("Aa brevis")
        .withOdsHasIdentification(List.of(
            new Identification().withOdsHasTaxonIdentification(
                List.of(new OdsHasTaxonIdentification().withDwcScientificName("Aa brevis")
                    .withDwcOrder("Asparagales"))
            )
        ));
  }

}
