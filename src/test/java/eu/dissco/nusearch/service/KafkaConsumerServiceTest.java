package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.TestUtils.MAPPER;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.then;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

  @Mock
  private DigitalSpecimenMatchingService matchingService;

  private KafkaConsumerService service;

  @BeforeEach
  void setup() {
    service = new KafkaConsumerService(MAPPER, matchingService);
  }

  @Test
  void testGetMessages() {
    // Given
    var message = givenMessage();

    // When
    service.getMessages(List.of(message));

    // Then
    then(matchingService).should().handleMessages(anyList());
  }

  @Test
  void testGetInvalidMessages() {
    // Given
    var message = givenInvalidMessage();

    // When
    service.getMessages(List.of(message));

    // Then
    then(matchingService).should().handleMessages(List.of());

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
             "AAS"
           ],
           "digitalSpecimenWrapper": {
             "ods:normalisedPhysicalSpecimenId": "http://coldb.mnhn.fr/catalognumber/mnhn/ec/ec10867",
             "ods:type": "https://doi.org/21.T11148/894b1e6cad57e921764e",
             "ods:attributes": {
               "ods:normalisedPhysicalSpecimenId": "http://coldb.mnhn.fr/catalognumber/mnhn/ec/ec10867",
               "ods:topicDiscipline": "Unclassified",
               "ods:specimenName": "Aa brevis",
               "dwc:basisOfRecord": "PreservedSpecimen",
               "dwc:institutionId": "https://ror.org/02y22ws83",
               "materialEntity": [],
               "dwc:identification": [
                 {
                   "citations": [],
                   "taxonIdentifications": [
                     {
                       "dwc:scientificName": "Aa brevis",
                       "dwc:order": "Asparagales"
                     }
                   ]
                 }
               ],
               "assertions": [],
               "occurrences": [],
               "entityRelationships": [],
               "citations": [],
               "identifiers": [],
               "chronometricAge": []
             },
             "ods:originalAttributes": {}
           },
           "digitalMediaObjectEvents": [
             {
               "digitalMediaObject": {
                 "ods:type": "https://doi.org/21.T11148/bbad8c4e101e8af01115",
                 "ods:physicalSpecimenId": "http://coldb.mnhn.fr/catalognumber/mnhn/ec/ec10867",
                 "ods:attributes": {
                   "ac:accessUri": "https://accessuri.eu/image_1",
                   "assertions": [],
                   "citations": [],
                   "identifiers": [],
                   "entityRelationships": []
                 },
                 "ods:originalAttributes": null
               },
               "enrichmentList": []
             }
           ]
         }""";
  }


}
