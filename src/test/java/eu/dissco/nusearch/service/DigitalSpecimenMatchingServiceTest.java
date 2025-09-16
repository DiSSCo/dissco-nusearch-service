package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.TestUtils.INSTITUTION_ID;
import static eu.dissco.nusearch.TestUtils.NORMALISED_PHYSICAL_SPECIMEN_ID;
import static eu.dissco.nusearch.TestUtils.givenColDpNameUsageMatch;
import static eu.dissco.nusearch.TestUtils.givenColDpNameUsageMatch2;
import static eu.dissco.nusearch.TestUtils.givenDigitalSpecimenEvent;
import static eu.dissco.nusearch.schema.Agent.Type.SCHEMA_SOFTWARE_APPLICATION;
import static eu.dissco.nusearch.schema.Identifier.OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

import eu.dissco.nusearch.domain.Classification;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.ApplicationProperties;
import eu.dissco.nusearch.schema.Agent;
import eu.dissco.nusearch.schema.DigitalSpecimen;
import eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.nusearch.schema.EntityRelationship;
import eu.dissco.nusearch.schema.Identification;
import eu.dissco.nusearch.schema.Identifier;
import eu.dissco.nusearch.schema.Identifier.DctermsType;
import eu.dissco.nusearch.schema.Identifier.OdsIdentifierStatus;
import eu.dissco.nusearch.schema.OdsHasRole;
import eu.dissco.nusearch.schema.TaxonIdentification;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import org.gbif.api.model.checklistbank.NameUsageMatch.MatchType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.NameParserGbifV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigitalSpecimenMatchingServiceTest {

  private static final Instant DATE = Instant.parse("2024-02-14T09:23:24.000Z");
  private final ExecutorService executorService = currentThreadExecutorService();
  private final NameParserGbifV1 nameParserGbifV1 = new NameParserGbifV1();
  @Captor
  ArgumentCaptor<DigitalSpecimenEvent> digitalSpecimenEventArgumentCaptor;
  @Mock
  private NubMatchingService nubMatchingService;
  @Mock
  private RabbitMqPublisherService rabbitMqService;
  private DigitalSpecimenMatchingService service;
  private MockedStatic<Instant> mockedInstant;
  private MockedStatic<Clock> mockedClock;
  private ApplicationProperties properties = new ApplicationProperties();

  private static ExecutorService currentThreadExecutorService() {
    CallerRunsPolicy callerRunsPolicy =
        new ThreadPoolExecutor.CallerRunsPolicy();

    return new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS,
        new SynchronousQueue<>(), callerRunsPolicy) {
      @Override
      public void execute(Runnable command) {
        callerRunsPolicy.rejectedExecution(command, this);
      }
    };
  }

  @BeforeEach
  void setup() {
    this.service = new DigitalSpecimenMatchingService(nubMatchingService, executorService,
        nameParserGbifV1, rabbitMqService, properties);
    Clock clock = Clock.fixed(DATE, ZoneOffset.UTC);
    Instant instant = Instant.now(clock);
    mockedInstant = mockStatic(Instant.class);
    mockedInstant.when(Instant::now).thenReturn(instant);
    mockedClock = mockStatic(Clock.class);
    mockedClock.when(Clock::systemUTC).thenReturn(clock);
  }

  @AfterEach
  void destroy() {
    mockedInstant.close();
    mockedClock.close();
  }

  @Test
  void testHandleMessage() {
    // Given
    var messages = List.of(givenDigitalSpecimenEvent());
    var classification = new Classification();
    classification.setOrder("Asparagales");
    given(nubMatchingService.match2(null, "Aa brevis", null, null, null, null, null, classification,
        null, false, true)).willReturn(givenColDpNameUsageMatch());
    given(nubMatchingService.v2(givenColDpNameUsageMatch())).willReturn(
        givenColDpNameUsageMatch2());

    // When
    service.handleMessages(messages);

    // Then
    then(rabbitMqService).should()
        .sendMessage(givenDigitalSpecimenEvent(expectedDigitalSpecimen()));
  }

  @Test
  void testHandleGeoDiversityMessageMessage() {
    // Given
    var messages = List.of(givenDigitalSpecimenEvent(OdsTopicDiscipline.ASTROGEOLOGY));

    // When
    service.handleMessages(messages);

    // Then
    then(nubMatchingService).shouldHaveNoInteractions();
    then(rabbitMqService).should()
        .sendMessage(givenDigitalSpecimenEvent(OdsTopicDiscipline.ASTROGEOLOGY));
  }

  @Test
  void testHandleMessageTwo() {
    // Given
    var digitalSpecimenEvent = givenDigitalSpecimenEvent();
    digitalSpecimenEvent.digitalSpecimenWrapper().attributes().setOdsHasIdentifications(List.of(
        new Identification().withOdsHasTaxonIdentifications(
            List.of(new TaxonIdentification().withDwcScientificName("Aa brevis")
                .withDwcOrder("Asparagales"))
        ),
        new Identification().withOdsHasTaxonIdentifications(
            List.of(new TaxonIdentification().withDwcScientificName("Aa brevis")
                    .withDwcOrder("Asparagales"),
                new TaxonIdentification().withDwcScientificName("Aa brevis")
                    .withDwcOrder("Asparagales"))
        )));
    var messages = List.of(digitalSpecimenEvent);
    var classification = new Classification();
    classification.setOrder("Asparagales");
    given(nubMatchingService.match2(null, "Aa brevis", null, null, null, null, null, classification,
        null, false, true)).willReturn(givenColDpNameUsageMatch());
    given(nubMatchingService.v2(givenColDpNameUsageMatch())).willReturn(
        givenColDpNameUsageMatch2());

    // When
    service.handleMessages(messages);

    // Then
    then(rabbitMqService).should()
        .sendMessage(digitalSpecimenEventArgumentCaptor.capture());
    var result = digitalSpecimenEventArgumentCaptor.getValue();
    assertThat(result.digitalSpecimenWrapper().attributes().getOdsHasEntityRelationships()).hasSize(
        1);
  }

  @Test
  void testHandleMessageNoMatch() {
    // Given
    var digitalSpecimenEvent = givenDigitalSpecimenEvent();
    digitalSpecimenEvent.digitalSpecimenWrapper().attributes()
        .getOdsHasIdentifications().getFirst().getOdsHasTaxonIdentifications().getFirst()
        .setDwcTaxonRank("species");
    var messages = List.of(digitalSpecimenEvent);
    var classification = new Classification();
    classification.setOrder("Asparagales");
    given(nubMatchingService.match2(null, "Aa brevis", null, null, null, null, Rank.SPECIES,
        classification,
        null, false, true)).willReturn(givenNoMatch());

    // When
    service.handleMessages(messages);

    // Then
    then(rabbitMqService).should()
        .sendMessage(givenDigitalSpecimenEvent(expectedDigitalSpecimenNoMatch()));
  }

  private ColDpNameUsageMatch givenNoMatch() {
    var noMatch = new ColDpNameUsageMatch();
    noMatch.setMatchType(MatchType.NONE);
    return noMatch;
  }

  private DigitalSpecimen expectedDigitalSpecimenNoMatch() {
    return new DigitalSpecimen()
        .withOdsNormalisedPhysicalSpecimenID(NORMALISED_PHYSICAL_SPECIMEN_ID)
        .withOdsOrganisationID(INSTITUTION_ID)
        .withDwcBasisOfRecord("PreservedSpecimen")
        .withOdsTopicDiscipline(OdsTopicDiscipline.UNCLASSIFIED)
        .withOdsSpecimenName("Aa brevis")
        .withOdsHasIdentifications(List.of(
            new Identification()
                .withDwcVerbatimIdentification("Aa brevis")
                .withOdsHasTaxonIdentifications(
                    List.of(new TaxonIdentification().withDwcScientificName("Aa brevis")
                        .withDwcOrder("Asparagales")
                        .withDwcTaxonRank("species"))
                )
        ));
  }

  private DigitalSpecimen expectedDigitalSpecimen() {
    DigitalSpecimen digitalSpecimen = new DigitalSpecimen();
    digitalSpecimen.setOdsSpecimenName("Myrosmodes brevis (Schltr.) Garay");
    digitalSpecimen.setOdsNormalisedPhysicalSpecimenID(NORMALISED_PHYSICAL_SPECIMEN_ID);
    digitalSpecimen.setOdsOrganisationID(INSTITUTION_ID);
    digitalSpecimen.setOdsTopicDiscipline(OdsTopicDiscipline.BOTANY);
    digitalSpecimen.setDwcBasisOfRecord("PreservedSpecimen");
    digitalSpecimen.setOdsHasIdentifications(List.of(
        new Identification()
            .withDwcVerbatimIdentification("Aa brevis")
            .withOdsHasTaxonIdentifications(List.of(
                new TaxonIdentification()
                    .withId("https://www.catalogueoflife.org/data/taxon/73SWK")
                    .withDwcTaxonID("https://www.catalogueoflife.org/data/taxon/73SWK")
                    .withDwcScientificName("Myrosmodes brevis (Schltr.) Garay")
                    .withOdsScientificNameHTMLLabel("<i>Myrosmodes brevis</i> (Schltr.) Garay")
                    .withDwcScientificNameAuthorship("(Schltr.) Garay")
                    .withDwcTaxonRank("SPECIES")
                    .withDwcKingdom("Plantae")
                    .withDwcPhylum("Tracheophyta")
                    .withDwcClass("Liliopsida")
                    .withDwcFamily("Orchidaceae")
                    .withDwcOrder("Asparagales")
                    .withDwcGenus("Myrosmodes Rchb.f.")
                    .withOdsGenusHTMLLabel("<i>Myrosmodes</i> Rchb.f.")
                    .withDwcSpecificEpithet("brevis")
                    .withDwcTaxonomicStatus("ACCEPTED")
                    .withDwcGenericName("Aa")
            ))));
    digitalSpecimen.setOdsHasEntityRelationships(
        List.of(new EntityRelationship()
            .withType("ods:EntityRelationship")
            .withDwcRelationshipEstablishedDate(Date.from(DATE))
            .withDwcRelatedResourceID("73SWK")
            .withDwcRelationshipOfResource("hasCOLID")
            .withOdsRelatedResourceURI(
                URI.create("https://www.catalogueoflife.org/data/taxon/73SWK"))
            .withOdsHasAgents(List.of(new Agent().withType(SCHEMA_SOFTWARE_APPLICATION)
                .withId("https://doi.org/10.5281/zenodo.14380476")
                .withSchemaName("DiSSCo Name Usage Search Service")
                .withSchemaIdentifier("https://doi.org/10.5281/zenodo.14380476")
                .withOdsHasRoles(List.of(
                    new OdsHasRole().withType("schema:Role").withSchemaRoleName("taxon-resolver")))
                .withOdsHasIdentifiers(List.of(new Identifier()
                    .withId("https://doi.org/10.5281/zenodo.14380476")
                    .withType("ods:Identifier")
                    .withDctermsType(DctermsType.DOI)
                    .withDctermsTitle("DOI")
                    .withOdsIsPartOfLabel(false)
                    .withOdsGupriLevel(GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT)
                    .withOdsIdentifierStatus(OdsIdentifierStatus.PREFERRED)
                    .withDctermsIdentifier("https://doi.org/10.5281/zenodo.14380476")
                )))
            )
        )
    );
    return digitalSpecimen;
  }


}
