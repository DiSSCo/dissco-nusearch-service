package eu.dissco.nusearch.service;

import com.google.common.base.Strings;
import eu.dissco.nusearch.domain.Classification;
import eu.dissco.nusearch.domain.ColNameUsageMatch2;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.nusearch.schema.Identifications;
import eu.dissco.nusearch.schema.TaxonIdentification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.NameUsageMatch.MatchType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.parsers.RankParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.nameparser.NameParserGbifV1;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class DigitalSpecimenMatchingService {

  private static final List<String> FOSSIL_BASIS_OF_RECORD = List.of("FOSSILSPECIMEN",
      "FOSSIL SPECIMEN", "FOSSIL");
  private static final List<String> EXTRATERRESTRIAL_BASIS_OF_RECORD = List.of("METEORITE",
      "METEORITESPECIMEN", "METEORITE SPECIMEN");
  private static final List<String> EARTH_SYSTEM_BASIS_OF_RECORD = List.of("ROCK", "MINERAL",
      "ROCKSPECIMEN", "ROCK SPECIMEN", "MINERALSPECIMEN", "MINERAL SPECIMEN");

  private final NubMatchingService nubMatchingService;
  private final ExecutorService executorService;
  private final NameParserGbifV1 nameParserGbifV1;
  private final KafkaProducerService kafkaProducerService;

  private static void setTaxonClassification(
      eu.dissco.nusearch.schema.TaxonIdentification taxonIdentification,
      ColNameUsageMatch2 v2result) {
    for (var classification : v2result.getClassification()) {
      switch (classification.getRank()) {
        case "kingdom":
          taxonIdentification.setDwcKingdom(classification.getLabel());
          break;
        case "phylum":
          taxonIdentification.setDwcPhylum(classification.getLabel());
          break;
        case "class":
          taxonIdentification.setDwcClass(classification.getLabel());
          break;
        case "order":
          taxonIdentification.setDwcOrder(classification.getLabel());
          break;
        case "superfamily":
          taxonIdentification.setDwcSuperfamily(classification.getLabel());
          break;
        case "family":
          taxonIdentification.setDwcFamily(classification.getLabel());
          break;
        case "subfamily":
          taxonIdentification.setDwcSubfamily(classification.getLabel());
          break;
        case "genus":
          taxonIdentification.setDwcGenus(classification.getLabel());
          break;
        case "subgenus":
          taxonIdentification.setDwcSubgenus(classification.getLabel());
          break;
        case "tribe":
          taxonIdentification.setDwcTribe(classification.getLabel());
          break;
        case "subtribe":
          taxonIdentification.setDwcSubtribe(classification.getLabel());
          break;
        default:
          log.debug("Cannot map rank to dwc field: {}", classification.getRank());
      }

    }
  }

  private static Identifications retrieveAcceptedIdentification(
      eu.dissco.nusearch.schema.DigitalSpecimen ds) {
    if (ds.getDwcIdentification() != null && !ds.getDwcIdentification().isEmpty()) {
      if (ds.getDwcIdentification().size() == 1) {
        return ds.getDwcIdentification().getFirst();
      }
      for (eu.dissco.nusearch.schema.Identifications identification : ds.getDwcIdentification()) {
        if (Boolean.TRUE.equals(identification.getDwcIdentificationVerificationStatus())) {
          return identification;
        }
      }
    }
    return null;
  }

  private static void setUpdatedSpecimenName(DigitalSpecimenEvent event) {
    var acceptedIdentification = retrieveAcceptedIdentification(
        event.digitalSpecimenWrapper().attributes());
    if (acceptedIdentification != null && acceptedIdentification.getTaxonIdentifications() != null
        && !acceptedIdentification.getTaxonIdentifications().isEmpty()) {
      event.digitalSpecimenWrapper().attributes().setOdsSpecimenName(
          acceptedIdentification.getTaxonIdentifications().getFirst().getDwcScientificName());
    }
  }

  private static void addEntityRelationship(ColNameUsageMatch2 taxonMatchResult,
      DigitalSpecimenEvent event) {
    event.digitalSpecimenWrapper().attributes().getEntityRelationships()
        .add(new eu.dissco.nusearch.schema.EntityRelationships()
            .withEntityRelationshipDate(Date.from(Instant.now()))
            .withEntityRelationshipType("hasColId")
            .withEntityRelationshipCreatorName("dissco-nusearch-service")
            .withEntityRelationshipCreatorId("https://hdl.handle.net/TEST/123-123-123")
            .withObjectEntityIri(
                "https://www.catalogueoflife.org/data/taxon/" + taxonMatchResult.getUsage()
                    .getColId()));
  }

  private void setTaxonIdentificationValues(
      eu.dissco.nusearch.schema.TaxonIdentification taxonIdentification,
      ColNameUsageMatch2 v2result, String genericName) {
    try {
      var parsedName = nameParserGbifV1.parse(v2result.getUsage().getLabel());
      taxonIdentification.setDwcNamePublishedInYear(parsedName.getYear());
      taxonIdentification.setDwcCultivarEpithet(parsedName.getCultivarEpithet());
      taxonIdentification.setDwcInfragenericEpithet(parsedName.getInfraGeneric());
      taxonIdentification.setDwcInfraspecificEpithet(parsedName.getInfraSpecificEpithet());
      taxonIdentification.setDwcSpecificEpithet(parsedName.getSpecificEpithet());
      taxonIdentification.setDwcNameAccordingTo(parsedName.getSensu());
      taxonIdentification.setDwcGenericName(genericName);
    } catch (UnparsableException e) {
      log.error("Unable to fill in fields due to unparsable name: {}",
          v2result.getUsage().getLabel(), e);
    }
    setTaxonClassification(taxonIdentification, v2result);
    taxonIdentification.setDwcTaxonID(v2result.getUsage().getColId());
    taxonIdentification.setDwcTaxonomicStatus(v2result.getUsage().getStatus().toString());
    taxonIdentification.setDwcScientificName(v2result.getUsage().getLabel());
    taxonIdentification.setOdsScientificNameHtmlLabel(v2result.getUsage().getLabelHtml());
    taxonIdentification.setDwcScientificNameAuthorship(v2result.getUsage().getAuthorship());
    if (v2result.getAcceptedUsage() != null) {
      taxonIdentification.setDwcAcceptedNameUsage(v2result.getAcceptedUsage().getLabel());
      taxonIdentification.setDwcAcceptedNameUsageID(v2result.getAcceptedUsage().getColId());
    }
    taxonIdentification.setDwcTaxonRank(v2result.getUsage().getRank());
  }

  public void handleMessages(List<DigitalSpecimenEvent> events) {
    log.info("Received a batch of {} events", events.size());
    var futures = new ArrayList<CompletableFuture<Void>>();
    for (var event : events) {
      log.debug("Handling event: {}", event);
      futures.add(CompletableFuture.runAsync(() -> handleEvent(event),
          executorService));
    }
    var combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    combinedFuture.join();
    log.info("Processed {} events", events.size());
  }

  private void handleEvent(DigitalSpecimenEvent event) {
    var identifications = event.digitalSpecimenWrapper().attributes().getDwcIdentification();
    for (eu.dissco.nusearch.schema.Identifications identification : identifications) {
      log.debug("Handling taxon: {}", identification);
      var taxonMatchResults = handleIdentification(identification);
      for (var taxonMatchResult : taxonMatchResults) {
        addEntityRelationship(taxonMatchResult, event);
      }
    }
    setUpdatedSpecimenName(event);
    setUpdatedTopicDiscipline(event);
    kafkaProducerService.sendMessage(event);
  }

  private void setUpdatedTopicDiscipline(DigitalSpecimenEvent event) {
    var ds = event.digitalSpecimenWrapper().attributes();
    var basisOfRecord = ds.getDwcBasisOfRecord();
    var acceptedIdentification = retrieveAcceptedIdentification(ds);
    if (acceptedIdentification != null && acceptedIdentification.getTaxonIdentifications() != null
        && !acceptedIdentification.getTaxonIdentifications().isEmpty()) {
      ds.setOdsTopicDiscipline(getDiscipline(basisOfRecord,
          acceptedIdentification.getTaxonIdentifications().getFirst().getDwcKingdom()));
    } else {
      ds.setOdsTopicDiscipline(getDiscipline(basisOfRecord, null));
    }
  }

  private OdsTopicDiscipline getDiscipline(String basisOfRecord, String kingdom) {
    if (basisOfRecord != null) {
      var harBasisOfRecord = basisOfRecord.trim().toUpperCase();
      if (FOSSIL_BASIS_OF_RECORD.contains(harBasisOfRecord)) {
        return OdsTopicDiscipline.PALAEONTOLOGY;
      } else if (EXTRATERRESTRIAL_BASIS_OF_RECORD.contains(harBasisOfRecord)) {
        return OdsTopicDiscipline.ASTROGEOLOGY;
      } else if (EARTH_SYSTEM_BASIS_OF_RECORD.contains(harBasisOfRecord)) {
        return OdsTopicDiscipline.GEOLOGY;
      } else if (kingdom != null) {
        var harmonisedKingdom = kingdom.trim().toUpperCase();
        switch (harmonisedKingdom) {
          case "ANIMALIA" -> {
            return OdsTopicDiscipline.ZOOLOGY;
          }
          case "PLANTAE" -> {
            return OdsTopicDiscipline.BOTANY;
          }
          case "BACTERIA" -> {
            return OdsTopicDiscipline.MICROBIOLOGY;
          }
          default -> {
            return OdsTopicDiscipline.UNCLASSIFIED;
          }
        }
      } else {
        return OdsTopicDiscipline.UNCLASSIFIED;
      }
    } else {
      return OdsTopicDiscipline.UNCLASSIFIED;
    }
  }

  private List<ColNameUsageMatch2> handleIdentification(Identifications identification) {
    var taxonIdentifications = identification.getTaxonIdentifications();
    var verbatimIdentification = new StringBuilder();
    var taxonMatchResults = new ArrayList<ColNameUsageMatch2>();
    for (eu.dissco.nusearch.schema.TaxonIdentification taxonIdentification : taxonIdentifications) {
      log.debug("Handling taxon identification: {}", taxonIdentification);
      if (!verbatimIdentification.isEmpty()) {
        verbatimIdentification.append(" | ");
      }
      verbatimIdentification.append(taxonIdentification.getDwcScientificName());
      var matchResult = matchTaxon(taxonIdentification);
      if (matchResult != null) {
        taxonMatchResults.add(matchResult);
      }
    }
    if (!verbatimIdentification.isEmpty()) {
      identification.setDwcVerbatimIdentification(verbatimIdentification.toString());
    }
    return taxonMatchResults;
  }

  private ColNameUsageMatch2 matchTaxon(TaxonIdentification taxonIdentification) {
    var classification = retrieveClassification(taxonIdentification);
    var resultMatch =
        nubMatchingService.match2(null, taxonIdentification.getDwcScientificName(),
            taxonIdentification.getDwcScientificNameAuthorship(),
            taxonIdentification.getDwcGenericName(), taxonIdentification.getDwcSpecificEpithet(),
            taxonIdentification.getDwcInfraspecificEpithet(),
            parseRank(taxonIdentification.getDwcTaxonRank()),
            classification, null, false, true);
    if (resultMatch.getMatchType() == MatchType.NONE) {
      log.info("No match found for taxon: {}", taxonIdentification);
      return null;
    }
    var result = nubMatchingService.v2(resultMatch);
    log.debug("Matching result: {}", result);
    setTaxonIdentificationValues(taxonIdentification, result, resultMatch.getGenericName());
    return result;
  }

  private Classification retrieveClassification(
      eu.dissco.nusearch.schema.TaxonIdentification taxonIdentification) {
    var classification = new Classification();
    classification.setKingdom(taxonIdentification.getDwcKingdom());
    classification.setPhylum(taxonIdentification.getDwcPhylum());
    classification.setClazz(taxonIdentification.getDwcClass());
    classification.setOrder(taxonIdentification.getDwcOrder());
    classification.setFamily(taxonIdentification.getDwcFamily());
    classification.setGenus(taxonIdentification.getDwcGenus());
    classification.setSubgenus(taxonIdentification.getDwcSubgenus());
    return classification;
  }

  private Rank parseRank(String value) throws IllegalArgumentException {
    if (!Strings.isNullOrEmpty(value)) {
      ParseResult<Rank> pr = RankParser.getInstance().parse(value);
      if (pr.isSuccessful()) {
        return pr.getPayload();
      }
    }
    return null;
  }
}
