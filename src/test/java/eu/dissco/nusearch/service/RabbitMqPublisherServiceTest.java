package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.TestUtils.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.property.RabbitMqProperties;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class RabbitMqPublisherServiceTest {

  private static RabbitMQContainer container;
  private static RabbitMqPublisherService rabbitMqService;
  private static RabbitTemplate rabbitTemplate;

  @BeforeAll
  static void setupContainer() throws IOException, InterruptedException {
    container = new RabbitMQContainer("rabbitmq:4.0.8-management-alpine");
    container.start();
    // Declare nu-search exchange, queue and binding
    declareRabbitResources("nu-search-exchange", "nu-search-queue", "nu-search");
    // Declare dlq exchange, queue and binding
    declareRabbitResources("nu-search-exchange-dlq", "nu-search-queue-dlq", "nu-search-dlq");
    // Declare digital specimen exchange, queue and binding
    declareRabbitResources("digital-specimen-exchange", "digital-specimen-queue",
        "digital-specimen");

    CachingConnectionFactory factory = new CachingConnectionFactory(container.getHost());
    factory.setPort(container.getAmqpPort());
    factory.setUsername(container.getAdminUsername());
    factory.setPassword(container.getAdminPassword());
    rabbitTemplate = new RabbitTemplate(factory);
    rabbitTemplate.setReceiveTimeout(100L);
  }

  private static void declareRabbitResources(String exchangeName, String queueName,
      String routingKey)
      throws IOException, InterruptedException {
    container.execInContainer("rabbitmqadmin", "declare", "exchange", "name=" + exchangeName,
        "type=direct", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "queue", "name=" + queueName,
        "queue_type=quorum", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "binding", "source=" + exchangeName,
        "destination_type=queue", "destination=" + queueName, "routing_key=" + routingKey);
  }

  @AfterAll
  static void shutdownContainer() {
    container.stop();
  }

  @BeforeEach
  void setup() {
    rabbitMqService = new RabbitMqPublisherService(MAPPER, rabbitTemplate,
        new RabbitMqProperties());
  }

  @Test
  void testInvalidReceiveMessage() {
    // Given
    var invalidMessage = givenInvalidMessage();

    // When
    rabbitMqService.sendMessageDLQ(invalidMessage);

    // Then
    var result = rabbitTemplate.receive("nu-search-queue-dlq");
    assertThat(new String(result.getBody())).isEqualTo(invalidMessage);
  }

  @Test
  void testPublishMessage() throws JsonProcessingException {
    // Given
    var event = MAPPER.readValue(givenMessage(), DigitalSpecimenEvent.class);

    // When
    rabbitMqService.sendMessage(event);

    // Then
    var result = rabbitTemplate.receive("digital-specimen-queue");
    assertThat(
        MAPPER.readValue(new String(result.getBody()), DigitalSpecimenEvent.class)).isEqualTo(
        event);
  }


  private String givenInvalidMessage() {
    return """
        {
          "enrichmentList": [
            "AAS"
          ],
          "digitalSpecimen": {
            "type": "https://doi.org/21.T11148/894b1e6cad57e921764e",
            "physicalSpecimenId": "https://geocollections.info/specimen/23602",
            "physicalSpecimenIdType": "global",
            "specimenName": "Biota",
            "organisationId": "https://ror.org/0443cwa12",
            "datasetId": null,
            "physicalSpecimenCollection": null,
            "sourceSystemId": "20.5000.1025/MN0-5XP-FFD",
            "data": {},
            "originalData": {},
            "dwcaId": null
          }
        }""";
  }

  private String givenMessage() {
    return """
        {
          "enrichmentList": [
            "OCR"
            ],
          "digitalSpecimenWrapper": {
            "ods:normalisedPhysicalSpecimenID": "https://geocollections.info/specimen/23602",
            "ods:type": "https://doi.org/21.T11148/894b1e6cad57e921764e",
            "ods:attributes": {
              "ods:physicalSpecimenIDType": "Global",
              "ods:physicalSpecimenID":"https://geocollections.info/specimen/23602",
              "ods:organisationID": "https://ror.org/0443cwa12",
              "ods:organisationName": "National Museum of Natural History",
              "ods:normalisedPhysicalSpecimenID": "https://geocollections.info/specimen/23602",
              "ods:specimenName": "Biota",
              "dwc:datasetName": null,
              "dwc:collectionID": null,
              "ods:sourceSystemID": "https://hdl.handle.net/TEST/57Z-6PC-64W",
              "ods:sourceSystemName": "A very nice source system",
              "dcterms:license": "http://creativecommons.org/licenses/by-nc/4.0/",
              "dcterms:modified": "2022-11-01T09:59:24.000Z",
              "ods:topicDiscipline": "Botany",
              "ods:isMarkedAsType": true,
              "ods:isKnownToContainMedia": false,
              "ods:livingOrPreserved": "Preserved"
            },
            "ods:originalAttributes": {
                "abcd:unitID": "152-4972",
                "abcd:sourceID": "GIT",
                "abcd:unitGUID": "https://geocollections.info/specimen/23646",
                "abcd:recordURI": "https://geocollections.info/specimen/23646",
                "abcd:recordBasis": "FossilSpecimen",
                "abcd:unitIDNumeric": 23646,
                "abcd:dateLastEdited": "2004-06-09T10:17:54.000+00:00",
                "abcd:kindOfUnit/0/value": "",
                "abcd:sourceInstitutionID": "Department of Geology, TalTech",
                "abcd:kindOfUnit/0/language": "en",
                "abcd:gathering/country/name/value": "Estonia",
                "abcd:gathering/localityText/value": "Laeva 297 borehole",
                "abcd:gathering/country/iso3166Code": "EE",
                "abcd:gathering/localityText/language": "en",
                "abcd:gathering/altitude/measurementOrFactText/value": "39.9",
                "abcd:identifications/identification/0/preferredFlag": true,
                "abcd:gathering/depth/measurementOrFactAtomised/lowerValue/value": "165",
                "abcd:gathering/depth/measurementOrFactAtomised/unitOfMeasurement": "m",
                "abcd:gathering/siteCoordinateSets/siteCoordinates/0/coordinatesLatLong/spatialDatum": "WGS84",
                "abcd:gathering/stratigraphy/chronostratigraphicTerms/chronostratigraphicTerm/0/term": "Pirgu Stage",
                "abcd:gathering/stratigraphy/chronostratigraphicTerms/chronostratigraphicTerm/1/term": "Katian",
                "abcd:gathering/siteCoordinateSets/siteCoordinates/0/coordinatesLatLong/latitudeDecimal": 58.489269,
                "abcd:gathering/siteCoordinateSets/siteCoordinates/0/coordinatesLatLong/longitudeDecimal": 26.385719,
                "abcd:gathering/stratigraphy/chronostratigraphicTerms/chronostratigraphicTerm/0/language": "en",
                "abcd:gathering/stratigraphy/chronostratigraphicTerms/chronostratigraphicTerm/1/language": "en",
                "abcd:identifications/identification/0/result/taxonIdentified/scientificName/fullScientificNameString": "Biota",
                "abcd-efg:earthScienceSpecimen/unitStratigraphicDetermination/chronostratigraphicAttributions/chronostratigraphicAttribution/0/chronostratigraphicName": "Pirgu Stage",
                "abcd-efg:earthScienceSpecimen/unitStratigraphicDetermination/chronostratigraphicAttributions/chronostratigraphicAttribution/0/chronoStratigraphicDivision": "Stage"
              }
          },
          "digitalMediaEvents": []
        }""";
  }
}
