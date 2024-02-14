package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.TestUtils.INSTITUTION_ID;
import static eu.dissco.nusearch.TestUtils.NORMALISED_PHYSICAL_SPECIMEN_ID;
import static eu.dissco.nusearch.TestUtils.givenColDpNameUsageMatch;
import static eu.dissco.nusearch.TestUtils.givenColDpNameUsageMatch2;
import static eu.dissco.nusearch.TestUtils.givenDigitalSpecimenEvent;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

import eu.dissco.nusearch.domain.Classification;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.schema.DigitalSpecimen;
import eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.nusearch.schema.EntityRelationships;
import eu.dissco.nusearch.schema.Identifications;
import eu.dissco.nusearch.schema.TaxonIdentification;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigitalSpecimenMatchingServiceTest {

  private static final Instant DATE = Instant.parse("2024-02-14T09:23:24.000Z");
  private final ExecutorService executorService = currentThreadExecutorService();
  private final NameParserGbifV1 nameParserGbifV1 = new NameParserGbifV1();
  @Mock
  private NubMatchingService nubMatchingService;
  @Mock
  private KafkaProducerService kafkaProducerService;
  private DigitalSpecimenMatchingService service;
  private MockedStatic<Instant> mockedInstant;
  private MockedStatic<Clock> mockedClock;

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
        nameParserGbifV1, kafkaProducerService);
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
    then(kafkaProducerService).should()
        .sendMessage(givenDigitalSpecimenEvent(expectedDigitalSpecimen()));
  }

  @Test
  void testHandleMessageNoMatch() {
    // Given
    var digitalSpecimenEvent = givenDigitalSpecimenEvent();
    digitalSpecimenEvent.digitalSpecimenWrapper().attributes()
        .getDwcIdentification().get(0).getTaxonIdentifications().get(0).setDwcTaxonRank("species");
    var messages = List.of(digitalSpecimenEvent);
    var classification = new Classification();
    classification.setOrder("Asparagales");
    given(nubMatchingService.match2(null, "Aa brevis", null, null, null, null, Rank.SPECIES, classification,
        null, false, true)).willReturn(givenNoMatch());

    // When
    service.handleMessages(messages);

    // Then
    then(kafkaProducerService).should()
        .sendMessage(givenDigitalSpecimenEvent(expectedDigitalSpecimenNoMatch()));
  }

  private ColDpNameUsageMatch givenNoMatch() {
    var noMatch = new ColDpNameUsageMatch();
    noMatch.setMatchType(MatchType.NONE);
    return noMatch;
  }

  private DigitalSpecimen expectedDigitalSpecimenNoMatch() {
    return new DigitalSpecimen()
        .withOdsNormalisedPhysicalSpecimenId(NORMALISED_PHYSICAL_SPECIMEN_ID)
        .withDwcInstitutionId(INSTITUTION_ID)
        .withDwcBasisOfRecord("PreservedSpecimen")
        .withOdsTopicDiscipline(OdsTopicDiscipline.UNCLASSIFIED)
        .withOdsSpecimenName("Aa brevis")
        .withDwcIdentification(List.of(
            new Identifications()
                .withDwcVerbatimIdentification("Aa brevis")
                .withTaxonIdentifications(
                    List.of(new TaxonIdentification().withDwcScientificName("Aa brevis")
                        .withDwcOrder("Asparagales")
                        .withDwcTaxonRank("species"))
                )
        ));
  }

  private DigitalSpecimen expectedDigitalSpecimen() {
    DigitalSpecimen digitalSpecimen = new DigitalSpecimen();
    digitalSpecimen.setOdsSpecimenName("Aa brevis Schltr.");
    digitalSpecimen.setOdsNormalisedPhysicalSpecimenId(NORMALISED_PHYSICAL_SPECIMEN_ID);
    digitalSpecimen.setDwcInstitutionId(INSTITUTION_ID);
    digitalSpecimen.setOdsTopicDiscipline(OdsTopicDiscipline.BOTANY);
    digitalSpecimen.setDwcBasisOfRecord("PreservedSpecimen");
    digitalSpecimen.setDwcIdentification(List.of(
        new Identifications()
            .withDwcVerbatimIdentification("Aa brevis")
            .withTaxonIdentifications(List.of(
                new eu.dissco.nusearch.schema.TaxonIdentification()
                    .withDwcTaxonID("7Q8L8")
                    .withDwcScientificName("Aa brevis Schltr.")
                    .withOdsScientificNameHtmlLabel("<i>Aa brevis</i> Schltr.")
                    .withDwcScientificNameAuthorship("Schltr.")
                    .withDwcTaxonRank("SPECIES")
                    .withDwcKingdom("Plantae")
                    .withDwcPhylum("Tracheophyta")
                    .withDwcClass("Liliopsida")
                    .withDwcFamily("Orchidaceae")
                    .withDwcOrder("Asparagales")
                    .withDwcGenus("Myrosmodes Rchb.f.")
                    .withDwcSpecificEpithet("brevis")
                    .withDwcTaxonomicStatus("SYNONYM")
                    .withDwcAcceptedNameUsage("Myrosmodes brevis (Schltr.) Garay")
                    .withDwcAcceptedNameUsageID("73SWK")
                    .withDwcGenericName("Aa")
            ))));
    digitalSpecimen.setEntityRelationships(
        List.of(new EntityRelationships()
            .withEntityRelationshipDate(Date.from(DATE))
            .withObjectEntityIri("https://www.catalogueoflife.org/data/taxon/7Q8L8")
            .withEntityRelationshipType("hasColId")
            .withEntityRelationshipCreatorName("dissco-nusearch-service")
            .withEntityRelationshipCreatorId("https://hdl.handle.net/TEST/123-123-123")
        )
    );
    return digitalSpecimen;
  }


}
