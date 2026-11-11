package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.TestUtils.ACCEPTED_COL_ID;
import static eu.dissco.nusearch.TestUtils.INSTITUTION_ID;
import static eu.dissco.nusearch.TestUtils.NORMALISED_PHYSICAL_SPECIMEN_ID;
import static eu.dissco.nusearch.TestUtils.givenAcceptedUsage;
import static eu.dissco.nusearch.TestUtils.givenDigitalSpecimen;
import static eu.dissco.nusearch.TestUtils.givenDigitalSpecimenEvent;
import static eu.dissco.nusearch.TestUtils.givenNameUsageMatch;
import static eu.dissco.nusearch.TestUtils.givenNoMatch;
import static eu.dissco.nusearch.schema.Agent.Type.SCHEMA_SOFTWARE_APPLICATION;
import static eu.dissco.nusearch.schema.Identifier.OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import eu.dissco.nusearch.client.MatchingClient;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.domain.matching.request.ClassificationQuery;
import eu.dissco.nusearch.domain.matching.request.NameUsageQuery;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.rust.NameParserRust;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigitalSpecimenMatchingServiceTest {

  private static final Instant DATE = Instant.parse("2024-02-14T09:23:24.000Z");
  private static final String COL_URI = "https://www.catalogueoflife.org/data/taxon/";
  private static final NameUsageQuery AA_BREVIS_QUERY = NameUsageQuery.builder()
      .scientificName("Aa brevis")
      .classification(new ClassificationQuery(null, null, null, "Asparagales", null, null, null,
          null, null, null, null, null))
      .strict(false)
      .verbose(false)
      .build();

  @Mock
  private MatchingClient client;
  @Mock
  private RabbitMqPublisherService publisherService;
  @Captor
  private ArgumentCaptor<DigitalSpecimenEvent> eventCaptor;
  @Captor
  private ArgumentCaptor<List<NameUsageQuery>> queryCaptor;
  private DigitalSpecimenMatchingService service;
  private MockedStatic<Instant> mockedInstant;
  private MockedStatic<Clock> mockedClock;

  static Stream<Arguments> publishedInYearCases() {
    return Stream.of(
        arguments("Myrosmodes brevis (Schltr.) Garay", "(Schltr.) Garay", null),
        arguments("Myrosmodes brevis Garay, 1977", "Garay, 1977", "1977"),
        arguments("Myrosmodes brevis (Schltr., 1920) Garay, 1977", "(Schltr., 1920) Garay, 1977",
            "1977"),
        arguments("Myrosmodes brevis (Schltr., 1920)", "(Schltr., 1920)", null));
  }

  static Stream<Arguments> topicDisciplineCases() {
    return Stream.of(
        arguments("PreservedSpecimen", "Plantae", OdsTopicDiscipline.BOTANY),
        arguments("PreservedSpecimen", "Animalia", OdsTopicDiscipline.ZOOLOGY),
        arguments("PreservedSpecimen", "Bacteria", OdsTopicDiscipline.MICROBIOLOGY),
        arguments("PreservedSpecimen", "Fungi", OdsTopicDiscipline.UNCLASSIFIED),
        arguments("FossilSpecimen", "Plantae", OdsTopicDiscipline.PALAEONTOLOGY),
        arguments(" fossil specimen ", "Plantae", OdsTopicDiscipline.PALAEONTOLOGY),
        arguments("Meteorite", "Plantae", OdsTopicDiscipline.ASTROGEOLOGY),
        arguments("RockSpecimen", "Plantae", OdsTopicDiscipline.GEOLOGY),
        arguments(null, "Plantae", OdsTopicDiscipline.UNCLASSIFIED));
  }

  private static DigitalSpecimen expectedDigitalSpecimen() {
    return new DigitalSpecimen()
        .withOdsNormalisedPhysicalSpecimenID(NORMALISED_PHYSICAL_SPECIMEN_ID)
        .withOdsOrganisationID(INSTITUTION_ID)
        .withDwcBasisOfRecord("PreservedSpecimen")
        .withOdsTopicDiscipline(OdsTopicDiscipline.BOTANY)
        .withOdsSpecimenName("Myrosmodes brevis (Schltr.) Garay")
        .withOdsHasIdentifications(List.of(
            new Identification()
                .withDwcVerbatimIdentification("Aa brevis")
                .withOdsHasTaxonIdentifications(List.of(
                    new TaxonIdentification()
                        .withId(COL_URI + ACCEPTED_COL_ID)
                        .withDwcTaxonID(COL_URI + ACCEPTED_COL_ID)
                        .withDwcScientificName("Myrosmodes brevis (Schltr.) Garay")
                        .withOdsScientificNameHTMLLabel(
                            "<i>Myrosmodes</i> <i>brevis</i> (Schltr.) Garay")
                        .withDwcScientificNameAuthorship("(Schltr.) Garay")
                        .withDwcTaxonRank("SPECIES")
                        .withDwcKingdom("Plantae")
                        .withDwcPhylum("Tracheophyta")
                        .withDwcClass("Liliopsida")
                        .withDwcOrder("Asparagales")
                        .withDwcFamily("Orchidaceae")
                        .withDwcGenus("Myrosmodes")
                        .withDwcSpecificEpithet("brevis")
                        .withDwcTaxonomicStatus("ACCEPTED")
                        .withDwcGenericName("Myrosmodes")))))
        .withOdsHasEntityRelationships(List.of(expectedEntityRelationship()));
  }

  private static EntityRelationship expectedEntityRelationship() {
    return new EntityRelationship()
        .withType("ods:EntityRelationship")
        .withDwcRelationshipEstablishedDate(Date.from(DATE))
        .withDwcRelatedResourceID(ACCEPTED_COL_ID)
        .withDwcRelationshipOfResource("hasCOLID")
        .withOdsRelatedResourceURI(URI.create(COL_URI + ACCEPTED_COL_ID))
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
                .withDctermsIdentifier("https://doi.org/10.5281/zenodo.14380476")))));
  }

  @BeforeEach
  void setup() {
    service = new DigitalSpecimenMatchingService(client, new NameParserRust(), publisherService,
        new ApplicationProperties());
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
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(
        List.of(givenNameUsageMatch()));

    // When
    service.handleMessages(List.of(givenDigitalSpecimenEvent()));

    // Then
    then(publisherService).should()
        .sendMessage(givenDigitalSpecimenEvent(expectedDigitalSpecimen()));
  }

  @Test
  void testHandleMessageQuery() {
    // Given
    var taxonIdentification = new TaxonIdentification()
        .withDwcScientificName("Myrosmodes brevis")
        .withDwcScientificNameAuthorship("(Schltr.) Garay")
        .withDwcGenericName("Myrosmodes")
        .withDwcSpecificEpithet("brevis")
        .withDwcInfraspecificEpithet("alba")
        .withDwcTaxonRank("subsp.")
        .withDwcKingdom("Plantae")
        .withDwcPhylum("Tracheophyta")
        .withDwcClass("Liliopsida")
        .withDwcOrder("Asparagales")
        .withDwcFamily("Orchidaceae")
        .withDwcGenus("Myrosmodes")
        .withDwcSubgenus("Myrosmodes");
    var event = givenDigitalSpecimenEvent(
        givenDigitalSpecimen(OdsTopicDiscipline.BOTANY, "PreservedSpecimen", taxonIdentification));
    var expectedQuery = NameUsageQuery.builder()
        .scientificName("Myrosmodes brevis")
        .authorship("(Schltr.) Garay")
        .genericName("Myrosmodes")
        .specificEpithet("brevis")
        .infraSpecificEpithet("alba")
        .rank(Rank.SUBSPECIES)
        .classification(new ClassificationQuery("Plantae", "Tracheophyta", "Liliopsida",
            "Asparagales", null, "Orchidaceae", null, null, null, "Myrosmodes", "Myrosmodes", null))
        .strict(false)
        .verbose(false)
        .build();
    given(client.getNameMatches(List.of(expectedQuery))).willReturn(List.of(givenNoMatch()));

    // When
    service.handleMessages(List.of(event));

    // Then
    then(client).should().getNameMatches(List.of(expectedQuery));
  }

  @Test
  void testHandleMessageAcceptedNameUsageFallback() {
    // Given
    var taxonIdentification = new TaxonIdentification()
        .withDwcScientificName("")
        .withDwcAcceptedNameUsage("Aa brevis")
        .withDwcOrder("Asparagales");
    var event = givenDigitalSpecimenEvent(
        givenDigitalSpecimen(OdsTopicDiscipline.BOTANY, "PreservedSpecimen", taxonIdentification));
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(List.of(givenNoMatch()));

    // When
    service.handleMessages(List.of(event));

    // Then
    then(client).should().getNameMatches(List.of(AA_BREVIS_QUERY));
  }

  @ParameterizedTest
  @EnumSource(value = OdsTopicDiscipline.class, names = {"ASTROGEOLOGY", "GEOLOGY",
      "OTHER_GEODIVERSITY"})
  void testHandleGeoDiversityMessage(OdsTopicDiscipline topicDiscipline) {
    // Given
    var messages = List.of(givenDigitalSpecimenEvent(topicDiscipline));

    // When
    service.handleMessages(messages);

    // Then
    then(client).shouldHaveNoInteractions();
    then(publisherService).should().sendMessage(givenDigitalSpecimenEvent(topicDiscipline));
  }

  @Test
  void testHandleMessageNoMatch() {
    // Given
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(List.of(givenNoMatch()));
    var expected = givenDigitalSpecimen(OdsTopicDiscipline.UNCLASSIFIED);
    expected.getOdsHasIdentifications().getFirst().setDwcVerbatimIdentification("Aa brevis");

    // When
    service.handleMessages(List.of(givenDigitalSpecimenEvent()));

    // Then
    then(publisherService).should().sendMessage(givenDigitalSpecimenEvent(expected));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void testHandleMessageWithoutName(String name) {
    // Given
    var taxonIdentification = new TaxonIdentification()
        .withDwcScientificName(name)
        .withDwcAcceptedNameUsage(name);
    var specimen = givenDigitalSpecimen(OdsTopicDiscipline.BOTANY, "PreservedSpecimen",
        taxonIdentification);

    // When
    service.handleMessages(List.of(givenDigitalSpecimenEvent(specimen)));

    // Then
    then(client).shouldHaveNoInteractions();
    then(publisherService).should().sendMessage(eventCaptor.capture());
    var identification = eventCaptor.getValue().digitalSpecimenWrapper().attributes()
        .getOdsHasIdentifications().getFirst();
    assertThat(identification.getDwcVerbatimIdentification()).isNull();
  }

  @Test
  void testHandleMessageMultipleIdentifications() {
    // Given
    var event = givenDigitalSpecimenEvent();
    event.digitalSpecimenWrapper().attributes().setOdsHasIdentifications(List.of(
        new Identification().withOdsHasTaxonIdentifications(List.of(
            new TaxonIdentification().withDwcScientificName("Aa brevis")
                .withDwcOrder("Asparagales"))),
        new Identification().withOdsHasTaxonIdentifications(List.of(
            new TaxonIdentification().withDwcScientificName("Aa brevis")
                .withDwcOrder("Asparagales"),
            new TaxonIdentification().withDwcScientificName("Aa brevis")
                .withDwcOrder("Asparagales")))));
    var queries = Collections.nCopies(3, AA_BREVIS_QUERY);
    given(client.getNameMatches(queries)).willReturn(
        Collections.nCopies(3, givenNameUsageMatch()));

    // When
    service.handleMessages(List.of(event));

    // Then
    then(publisherService).should().sendMessage(eventCaptor.capture());
    var result = eventCaptor.getValue().digitalSpecimenWrapper().attributes();
    assertThat(result.getOdsHasEntityRelationships()).containsExactly(
        expectedEntityRelationship());
    assertThat(result.getOdsHasIdentifications().get(1).getDwcVerbatimIdentification())
        .isEqualTo("Aa brevis | Aa brevis");
  }

  @Test
  void testHandleMessageExistingColId() {
    // Given
    var specimen = givenDigitalSpecimen(OdsTopicDiscipline.BOTANY);
    specimen.setOdsHasEntityRelationships(new ArrayList<>(List.of(new EntityRelationship()
        .withDwcRelationshipOfResource("hasCOLID")
        .withDwcRelatedResourceID(ACCEPTED_COL_ID))));
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(
        List.of(givenNameUsageMatch()));

    // When
    service.handleMessages(List.of(givenDigitalSpecimenEvent(specimen)));

    // Then
    then(publisherService).should().sendMessage(eventCaptor.capture());
    assertThat(eventCaptor.getValue().digitalSpecimenWrapper().attributes()
        .getOdsHasEntityRelationships()).hasSize(1);
  }

  @Test
  void testHandleMessagesSplitsLargeBatches() {
    // Given
    var events = new ArrayList<DigitalSpecimenEvent>();
    for (int i = 0; i < 1001; i++) {
      events.add(givenDigitalSpecimenEvent());
    }
    given(client.getNameMatches(anyList())).willAnswer(invocation -> {
      List<NameUsageQuery> queries = invocation.getArgument(0);
      return Collections.nCopies(queries.size(), givenNameUsageMatch());
    });

    // When
    service.handleMessages(events);

    // Then
    then(client).should(times(2)).getNameMatches(queryCaptor.capture());
    assertThat(queryCaptor.getAllValues()).extracting(List::size).containsExactly(1000, 1);
    then(publisherService).should(times(1001)).sendMessage(eventCaptor.capture());
    assertThat(eventCaptor.getAllValues()).allSatisfy(event ->
        assertThat(event.digitalSpecimenWrapper().attributes().getOdsHasEntityRelationships())
            .hasSize(1));
  }

  @Test
  void testHandleMessagesWrongNumberOfResults() {
    // Given
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(List.of());
    var events = List.of(givenDigitalSpecimenEvent());

    // When / Then
    assertThatThrownBy(() -> service.handleMessages(events))
        .isInstanceOf(IllegalStateException.class);
    then(publisherService).shouldHaveNoInteractions();
  }

  @ParameterizedTest
  @MethodSource("publishedInYearCases")
  void testPublishedInYear(String acceptedName, String authorship, String expectedYear) {
    // Given
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(
        List.of(givenNameUsageMatch(givenAcceptedUsage(acceptedName, authorship), "Plantae")));

    // When
    service.handleMessages(List.of(givenDigitalSpecimenEvent()));

    // Then
    then(publisherService).should().sendMessage(eventCaptor.capture());
    var taxonIdentification = eventCaptor.getValue().digitalSpecimenWrapper().attributes()
        .getOdsHasIdentifications().getFirst().getOdsHasTaxonIdentifications().getFirst();
    assertThat(taxonIdentification.getDwcNamePublishedInYear()).isEqualTo(expectedYear);
  }

  @ParameterizedTest
  @MethodSource("topicDisciplineCases")
  void testTopicDiscipline(String basisOfRecord, String kingdom,
      OdsTopicDiscipline expected) {
    // Given
    var specimen = givenDigitalSpecimen(OdsTopicDiscipline.BOTANY, basisOfRecord,
        new TaxonIdentification().withDwcScientificName("Aa brevis").withDwcOrder("Asparagales"));
    given(client.getNameMatches(List.of(AA_BREVIS_QUERY))).willReturn(
        List.of(givenNameUsageMatch(givenAcceptedUsage("Myrosmodes brevis (Schltr.) Garay",
            "(Schltr.) Garay"), kingdom)));

    // When
    service.handleMessages(List.of(givenDigitalSpecimenEvent(specimen)));

    // Then
    then(publisherService).should().sendMessage(eventCaptor.capture());
    assertThat(eventCaptor.getValue().digitalSpecimenWrapper().attributes()
        .getOdsTopicDiscipline()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "SPECIES, SPECIES",
      "species, SPECIES",
      "' Species ', SPECIES",
      "sp., SPECIES",
      "subsp., SUBSPECIES",
      "var., VARIETY",
      "f., FORM",
      "infraspecific name, INFRASPECIFIC_NAME",
      "SECTION, SECTION_BOTANY",
      "SUBSECTION, SUBSECTION_BOTANY",
      "SERIES, SERIES_BOTANY",
      "SUBSERIES, SUBSERIES_BOTANY",
      "unranked, UNRANKED",
      "soort,",
      "xyz,"
  })
  void testParseRank(String value, Rank expected) {
    // When
    var result = DigitalSpecimenMatchingService.parseRank(value);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void testParseRankBlank(String value) {
    // When
    var result = DigitalSpecimenMatchingService.parseRank(value);

    // Then
    assertThat(result).isNull();
  }

}
