package eu.dissco.nusearch.controller;

import static eu.dissco.nusearch.TestUtils.createColNameUsageMatch2;
import static eu.dissco.nusearch.TestUtils.givenColDpNameUsageMatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import eu.dissco.nusearch.domain.NameUsageRequest;
import eu.dissco.nusearch.service.NubMatchingService;
import java.util.List;
import org.gbif.api.vocabulary.Rank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NubControllerTest {

  @Mock
  private NubMatchingService matchingService;

  private NubController nubController;

  @BeforeEach
  void setup() {
    nubController = new NubController(matchingService);
  }

  @Test
  void testMatch() {
    // Given
    var key = "7Q8L8";
    var expected = givenColDpNameUsageMatch();
    given(
        matchingService.match2("7Q8L8", null, null, null, null, null, null, null, null, false,
            false))
        .willReturn(expected);

    // When
    var result = nubController.match(key, null, null, null, null, null, null, null, null,
        null, null, null, null);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testMatchV2() {
    // Given
    var name = "Aa brevis Schltr.";
    var expected = createColNameUsageMatch2();
    var colDpNameUsage = givenColDpNameUsageMatch();
    given(
        matchingService.match2(null, name, null, null, null, null, Rank.SPECIES, null, null, false,
            false))
        .willReturn(colDpNameUsage);
    given(matchingService.v2(colDpNameUsage)).willReturn(expected);

    // When
    var result = nubController.match2(null, null, name, null, null, null, "species", null, null,
        null, null, null, null, null);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testBatch() {
    // Given
    var name = "Aa brevis Schltr.";
    var request = new NameUsageRequest();
    request.setScientificName(name);
    request.setRank("species");
    var expected = createColNameUsageMatch2();
    var colDpNameUsage = givenColDpNameUsageMatch();
    given(
        matchingService.match2(null, name, null, null, null, null, Rank.SPECIES, null, null, false,
            false))
        .willReturn(colDpNameUsage);
    given(matchingService.v2(colDpNameUsage)).willReturn(expected);

    // When
    var result = nubController.batch(List.of(request));

    // Then
    assertThat(result).containsExactly(expected);
  }

  @Test
  void testAutoComplete() {
    // Given
    var request = "aa";
    var expected = List.of(createColNameUsageMatch2());
    given(matchingService.autocomplete(request, 10)).willReturn(expected);

    // When
    var result = nubController.autocomplete(request, 10);

    // Then
    assertThat(result).isEqualTo(expected);
  }

}
