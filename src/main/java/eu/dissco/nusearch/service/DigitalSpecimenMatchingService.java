package eu.dissco.nusearch.service;

import eu.dissco.nusearch.client.MatchingClient;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.domain.matching.request.ClassificationQuery;
import eu.dissco.nusearch.domain.matching.request.NameUsageQuery;
import eu.dissco.nusearch.domain.response.MatchType;
import eu.dissco.nusearch.domain.response.NameUsageMatch;
import eu.dissco.nusearch.domain.response.RankedName;
import eu.dissco.nusearch.domain.response.Usage;
import eu.dissco.nusearch.property.ApplicationProperties;
import eu.dissco.nusearch.schema.*;
import eu.dissco.nusearch.schema.Agent.Type;
import eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.nusearch.schema.Identifier.DctermsType;
import eu.dissco.nusearch.schema.Identifier.OdsGupriLevel;
import eu.dissco.nusearch.schema.Identifier.OdsIdentifierStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParseResult;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.util.RankUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalSpecimenMatchingService {

    private static final String COL_URI = "https://www.catalogueoflife.org/data/taxon/";
    private static final List<String> FOSSIL_BASIS_OF_RECORD = List.of("FOSSILSPECIMEN",
            "FOSSIL SPECIMEN", "FOSSIL");
    private static final List<String> EXTRATERRESTRIAL_BASIS_OF_RECORD = List.of("METEORITE",
            "METEORITESPECIMEN", "METEORITE SPECIMEN");
    private static final List<String> EARTH_SYSTEM_BASIS_OF_RECORD = List.of("ROCK", "MINERAL",
            "ROCKSPECIMEN", "ROCK SPECIMEN", "MINERALSPECIMEN", "MINERAL SPECIMEN");
    private static final String COL_RELATION_OF_RESOURCE = "hasCOLID";
    private static final Set<OdsTopicDiscipline> BIO_TOPIC_DISCIPLINES = Set.of(MICROBIOLOGY,
            ANTHROPOLOGY, BOTANY, ZOOLOGY, PALAEONTOLOGY, OTHER_BIODIVERSITY, ECOLOGY, UNCLASSIFIED);
    // matching-ws accepts at most 1000 queries per call
    private static final int MAX_QUERIES_PER_CALL = 1000;

    private final MatchingClient client;
    private final NameParser nameParser;
    private final RabbitMqPublisherService publisherService;
    private final ApplicationProperties properties;

    public void handleMessages(List<DigitalSpecimenEvent> events) {
        log.info("Received a batch of {} events", events.size());
        var eventsToResolve = new ArrayList<DigitalSpecimenEvent>();
        var pendingMatches = new ArrayList<PendingMatch>();
        for (var event : events) {
            if (isTaxResolutionNeeded(event)) {
                eventsToResolve.add(event);
                pendingMatches.addAll(collectQueries(event));
            } else {
                log.debug("Skipping taxonomic resolution for event: {} with topicDisciplines: {}",
                        event.digitalSpecimenWrapper().physicalSpecimenID(),
                        event.digitalSpecimenWrapper().attributes().getDwcBasisOfRecord());
            }
        }
        var matchedUsages = matchAll(pendingMatches);
        for (var event : eventsToResolve) {
            addEntityRelationships(event, matchedUsages.getOrDefault(event, List.of()));
            setUpdatedSpecimenName(event);
            setUpdatedTopicDiscipline(event);
        }
        events.forEach(publisherService::sendMessage);
        log.info("Processed {} events", events.size());
    }

    private Map<DigitalSpecimenEvent, List<Usage>> matchAll(List<PendingMatch> pendingMatches) {
        var matchedUsages = new IdentityHashMap<DigitalSpecimenEvent, List<Usage>>();
        for (int start = 0; start < pendingMatches.size(); start += MAX_QUERIES_PER_CALL) {
            var chunk = pendingMatches.subList(start,
                    Math.min(start + MAX_QUERIES_PER_CALL, pendingMatches.size()));
            var results = client.getNameMatches(chunk.stream().map(PendingMatch::query).toList());
            if (results.size() != chunk.size()) {
                throw new IllegalStateException("Matching service returned " + results.size()
                        + " results for " + chunk.size() + " queries");
            }
            for (int i = 0; i < chunk.size(); i++) {
                var pending = chunk.get(i);
                var match = results.get(i);
                if (match.diagnostics() == null || match.diagnostics().matchType() == MatchType.NONE) {
                    log.info("No match found for taxon: {}", pending.taxonIdentification());
                } else {
                    setTaxonIdentificationValues(pending.taxonIdentification(), match);
                    matchedUsages.computeIfAbsent(pending.event(), _ -> new ArrayList<>())
                            .add(determineAcceptedUsage(match));
                }
            }
        }
        return matchedUsages;
    }

    private List<PendingMatch> collectQueries(DigitalSpecimenEvent event) {
        var pendingMatches = new ArrayList<PendingMatch>();
        var identifications = event.digitalSpecimenWrapper().attributes().getOdsHasIdentifications();
        if (identifications == null) {
            log.debug("{} contains no identifications that need to be resolved", event.digitalSpecimenWrapper().physicalSpecimenID());
            return pendingMatches;
        }
        for (var identification : identifications) {
            var taxonIdentifications = identification.getOdsHasTaxonIdentifications();
            if (taxonIdentifications == null) {
                continue;
            }
            var verbatimIdentification = new StringBuilder();
            for (var taxonIdentification : taxonIdentifications) {
                mapTaxonIdentification(event, taxonIdentification, verbatimIdentification, pendingMatches);
            }
            if (!verbatimIdentification.isEmpty()) {
                identification.setDwcVerbatimIdentification(verbatimIdentification.toString());
            }
        }
        return pendingMatches;
    }

    private static void mapTaxonIdentification(DigitalSpecimenEvent event, TaxonIdentification taxonIdentification, StringBuilder verbatimIdentification, List<PendingMatch> pendingMatches) {
        log.debug("Handling taxon identification: {}", taxonIdentification);
        var specimenName = taxonIdentification.getDwcScientificName();
        if (specimenName == null || specimenName.isEmpty()) {
            specimenName = taxonIdentification.getDwcAcceptedNameUsage();
        }
        if (specimenName == null || specimenName.isBlank()) {
            log.debug("No name to match in taxon identification: {}", taxonIdentification);
            return;
        }
        if (!verbatimIdentification.isEmpty()) {
            verbatimIdentification.append(" | ");
        }
        verbatimIdentification.append(specimenName);
        pendingMatches.add(new PendingMatch(event, taxonIdentification,
                toQuery(taxonIdentification, specimenName)));
    }

    private static NameUsageQuery toQuery(TaxonIdentification taxonIdentification,
                                          String specimenName) {
        return NameUsageQuery.builder()
                .scientificName(specimenName)
                .authorship(taxonIdentification.getDwcScientificNameAuthorship())
                .genericName(taxonIdentification.getDwcGenericName())
                .specificEpithet(taxonIdentification.getDwcSpecificEpithet())
                .infraSpecificEpithet(taxonIdentification.getDwcInfraspecificEpithet())
                .rank(parseRank(taxonIdentification.getDwcTaxonRank()))
                .classification(new ClassificationQuery(
                        taxonIdentification.getDwcKingdom(),
                        taxonIdentification.getDwcPhylum(),
                        taxonIdentification.getDwcClass(),
                        taxonIdentification.getDwcOrder(),
                        null,
                        taxonIdentification.getDwcFamily(),
                        null,
                        null,
                        null,
                        taxonIdentification.getDwcGenus(),
                        taxonIdentification.getDwcSubgenus(),
                        null))
                .strict(false)
                .verbose(false)
                .build();
    }

    static Rank parseRank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var normalised = value.trim().toUpperCase().replace(' ', '_');
        try {
            return Rank.valueOf(normalised);
        } catch (IllegalArgumentException _) {
            return RankUtils.inferRank(value.trim());
        }
    }

    private void setTaxonIdentificationValues(TaxonIdentification taxonIdentification,
                                              NameUsageMatch match) {
        var usage = determineAcceptedUsage(match);
        taxonIdentification.setDwcNamePublishedInYear(publishedInYear(usage));
        taxonIdentification.setDwcInfragenericEpithet(usage.infragenericEpithet());
        taxonIdentification.setDwcInfraspecificEpithet(usage.infraspecificEpithet());
        taxonIdentification.setDwcSpecificEpithet(usage.specificEpithet());
        taxonIdentification.setDwcGenericName(usage.genericName());
        setTaxonClassification(taxonIdentification, match.classification());
        taxonIdentification.setDwcTaxonID(COL_URI + usage.key());
        taxonIdentification.setId(COL_URI + usage.key());
        // matching-ws leaves the status of the accepted usage empty
        taxonIdentification.setDwcTaxonomicStatus(
                match.acceptedUsage() != null ? "ACCEPTED" : usage.status());
        taxonIdentification.setDwcScientificName(usage.name());
        taxonIdentification.setOdsScientificNameHTMLLabel(usage.formattedName());
        taxonIdentification.setDwcScientificNameAuthorship(usage.authorship());
        taxonIdentification.setDwcTaxonRank(usage.rank() != null ? usage.rank().name() : null);
    }

    /**
     * The year of the combination authorship, parsed with the same parser version as matching-ws
     * so the result is the same one GBIF would give.
     */
    private String publishedInYear(Usage usage) {
        if (usage == null || usage.name() == null || usage.name().isEmpty()) {
            return null;
        }
        var result = nameParser.parse(usage.name(), null, usage.rank(), usage.code());
        ParsedName parsedName = result instanceof ParseResult.Informal informal
                ? informal.toParsedName()
                : result.parsed().orElse(null);
        return parsedName != null ? parsedName.getCombinationAuthorship().getYear() : null;
    }

    private static void setTaxonClassification(TaxonIdentification taxonIdentification,
                                               List<RankedName> classification) {
        if (classification == null) {
            return;
        }
        for (var rankedName : classification) {
            if (rankedName.rank() == null) {
                continue;
            }
            var name = rankedName.name();
            switch (rankedName.rank()) {
                case KINGDOM -> taxonIdentification.setDwcKingdom(name);
                case PHYLUM -> taxonIdentification.setDwcPhylum(name);
                case CLASS -> taxonIdentification.setDwcClass(name);
                case ORDER -> taxonIdentification.setDwcOrder(name);
                case SUPERFAMILY -> taxonIdentification.setDwcSuperfamily(name);
                case FAMILY -> taxonIdentification.setDwcFamily(name);
                case SUBFAMILY -> taxonIdentification.setDwcSubfamily(name);
                case GENUS -> taxonIdentification.setDwcGenus(name);
                case SUBGENUS -> taxonIdentification.setDwcSubgenus(name);
                case TRIBE -> taxonIdentification.setDwcTribe(name);
                case SUBTRIBE -> taxonIdentification.setDwcSubtribe(name);
                default -> log.debug("Cannot map rank to dwc field: {}", rankedName.rank());
            }
        }
    }

    private static Usage determineAcceptedUsage(NameUsageMatch match) {
        return match.acceptedUsage() != null ? match.acceptedUsage() : match.usage();
    }

    private void addEntityRelationships(DigitalSpecimenEvent event, List<Usage> matchedUsages) {
        var existingColIds = colIdList(event);
        matchedUsages.stream()
                .filter(usage -> !existingColIds.contains(usage.key()))
                .collect(Collectors.toMap(Usage::key, usage -> usage,
                        (existing, _) -> existing, LinkedHashMap::new))
                .values()
                .forEach(usage -> addEntityRelationship(usage, event));
    }

    private void addEntityRelationship(Usage usage, DigitalSpecimenEvent event) {
        event.digitalSpecimenWrapper().attributes().getOdsHasEntityRelationships()
                .add(new EntityRelationship()
                        .withType("ods:EntityRelationship")
                        .withDwcRelationshipEstablishedDate(Date.from(Instant.now()))
                        .withDwcRelationshipOfResource(COL_RELATION_OF_RESOURCE)
                        .withOdsRelatedResourceURI(URI.create(COL_URI + usage.key()))
                        .withDwcRelatedResourceID(usage.key())
                        .withOdsHasAgents(List.of(new Agent()
                                .withType(Type.SCHEMA_SOFTWARE_APPLICATION)
                                .withId(properties.getPid())
                                .withSchemaIdentifier(properties.getPid())
                                .withSchemaName(properties.getName())
                                .withOdsHasRoles(List.of(new OdsHasRole()
                                        .withSchemaRoleName("taxon-resolver")
                                        .withType("schema:Role")))
                                .withOdsHasIdentifiers(List.of(new Identifier()
                                        .withId(properties.getPid())
                                        .withType("ods:Identifier")
                                        .withDctermsType(DctermsType.DOI)
                                        .withDctermsTitle("DOI")
                                        .withOdsIsPartOfLabel(false)
                                        .withOdsGupriLevel(
                                                OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT)
                                        .withOdsIdentifierStatus(OdsIdentifierStatus.PREFERRED)
                                        .withDctermsIdentifier(properties.getPid()))))));
    }

    private static Set<String> colIdList(DigitalSpecimenEvent event) {
        return event.digitalSpecimenWrapper().attributes().getOdsHasEntityRelationships()
                .stream()
                .filter(er -> COL_RELATION_OF_RESOURCE.equals(er.getDwcRelationshipOfResource()))
                .map(EntityRelationship::getDwcRelatedResourceID)
                .collect(Collectors.toSet());
    }

    private static boolean isTaxResolutionNeeded(DigitalSpecimenEvent event) {
        var ds = event.digitalSpecimenWrapper().attributes();
        return BIO_TOPIC_DISCIPLINES.contains(ds.getOdsTopicDiscipline());
    }

    private static Identification retrieveAcceptedIdentification(DigitalSpecimen ds) {
        if (ds.getOdsHasIdentifications() != null && !ds.getOdsHasIdentifications().isEmpty()) {
            if (ds.getOdsHasIdentifications().size() == 1) {
                return ds.getOdsHasIdentifications().getFirst();
            }
            for (var identification : ds.getOdsHasIdentifications()) {
                if (Boolean.TRUE.equals(identification.getOdsIsVerifiedIdentification())) {
                    return identification;
                }
            }
        }
        return null;
    }

    private static void setUpdatedSpecimenName(DigitalSpecimenEvent event) {
        var acceptedIdentification = retrieveAcceptedIdentification(
                event.digitalSpecimenWrapper().attributes());
        if (acceptedIdentification != null
                && acceptedIdentification.getOdsHasTaxonIdentifications() != null
                && !acceptedIdentification.getOdsHasTaxonIdentifications().isEmpty()) {
            event.digitalSpecimenWrapper().attributes().setOdsSpecimenName(
                    acceptedIdentification.getOdsHasTaxonIdentifications().getFirst()
                            .getDwcScientificName());
        }
    }

    private static void setUpdatedTopicDiscipline(DigitalSpecimenEvent event) {
        var ds = event.digitalSpecimenWrapper().attributes();
        var basisOfRecord = ds.getDwcBasisOfRecord();
        var acceptedIdentification = retrieveAcceptedIdentification(ds);
        if (acceptedIdentification != null
                && acceptedIdentification.getOdsHasTaxonIdentifications() != null
                && !acceptedIdentification.getOdsHasTaxonIdentifications().isEmpty()) {
            ds.setOdsTopicDiscipline(getDiscipline(basisOfRecord,
                    acceptedIdentification.getOdsHasTaxonIdentifications().getFirst().getDwcKingdom()));
        } else {
            ds.setOdsTopicDiscipline(getDiscipline(basisOfRecord, null));
        }
    }

    private static OdsTopicDiscipline getDiscipline(String basisOfRecord, String kingdom) {
        if (basisOfRecord == null) {
            return UNCLASSIFIED;
        }
        var harmonisedBasisOfRecord = basisOfRecord.trim().toUpperCase();
        if (FOSSIL_BASIS_OF_RECORD.contains(harmonisedBasisOfRecord)) {
            return PALAEONTOLOGY;
        } else if (EXTRATERRESTRIAL_BASIS_OF_RECORD.contains(harmonisedBasisOfRecord)) {
            return OdsTopicDiscipline.ASTROGEOLOGY;
        } else if (EARTH_SYSTEM_BASIS_OF_RECORD.contains(harmonisedBasisOfRecord)) {
            return OdsTopicDiscipline.GEOLOGY;
        } else if (kingdom == null) {
            return UNCLASSIFIED;
        }
        return switch (kingdom.trim().toUpperCase()) {
            case "ANIMALIA" -> ZOOLOGY;
            case "PLANTAE" -> BOTANY;
            case "BACTERIA" -> MICROBIOLOGY;
            default -> UNCLASSIFIED;
        };
    }

    private record PendingMatch(DigitalSpecimenEvent event, TaxonIdentification taxonIdentification,
                                NameUsageQuery query) {

    }
}
