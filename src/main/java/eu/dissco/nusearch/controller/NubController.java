/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.dissco.nusearch.controller;

// Copied and adapted from GBIF:
// https://github.com/gbif/checklistbank/blob/master/checklistbank-nub-ws/src/main/java/org/gbif/checklistbank/ws/nub/NubResource.java

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;
import static eu.dissco.nusearch.utils.ParameterUtils.first;

import com.google.common.base.Strings;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.domain.ColNameUsageMatch2;
import eu.dissco.nusearch.domain.NameUsageRequest;
import eu.dissco.nusearch.service.NubMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.parsers.RankParser;
import org.gbif.common.parsers.core.ParseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/species",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
@Profile({STANDALONE, S3_RESOLVER})
public class NubController {

  private final NubMatchingService matchingService;

  @Autowired
  public NubController(NubMatchingService matchingService) {
    this.matchingService = matchingService;
  }


  @Operation(
      operationId = "matchNames",
      summary = "Fuzzy name match service",
      description =
          """
              Fuzzy matches scientific names against the GBIF Backbone Taxonomy with the optional
              classification provided. If a classification is provided and strict is not set to true, the default matching
              will also try to match against these if no direct match is found for the name parameter alone.
              Additionally, a lookup may be performed by providing the usageKey which will short-circuit the name-based matching
              and ONLY use the given key, either finding the concept or returning no match.
              """,
      extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0130"))
  )
  @Tag(name = "Searching names")
  @Parameters(
      value = {
          @Parameter(
              name = "name",
              description = "The scientific name to fuzzy match against. May include the authorship and year"
          ),
          @Parameter(name = "scientificName", hidden = true),
          @Parameter(
              name = "authorship",
              description = "The scientific name authorship to fuzzy match against."
          ),
          @Parameter(name = "scientificNameAuthorship", hidden = true),
          @Parameter(
              name = "rank",
              description = "Filters by taxonomic rank as given in our https://api.gbif.org/v1/enumeration/basic/Rank[Rank enum].",
              schema = @Schema(implementation = Rank.class)
          ),
          @Parameter(name = "taxonRank", hidden = true),
          @Parameter(
              name = "kingdom",
              description = "Kingdom to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "phylum",
              description = "Phylum to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "order",
              description = "Order to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "class",
              description = "Class to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "family",
              description = "Family to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "genus",
              description = "Genus to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "genericName",
              description = "Generic part of the name to match when given as atomised parts instead of the full name parameter."
          ),
          @Parameter(
              name = "specificEpithet",
              description = "Specific epithet to match."
          ),
          @Parameter(
              name = "infraspecificEpithet",
              description = "Infraspecific epithet to match."
          ),
          @Parameter(name = "classification", hidden = true),
          @Parameter(
              name = "strict",
              description = "If true it fuzzy matches only the given name, but never a taxon in the upper classification."
          ),
          @Parameter(
              name = "verbose",
              description = "If true it shows alternative matches which were considered but then rejected."
          ),
          @Parameter(
              name = "usageKey",
              description = "The usage key to look up. When provided, all other fields are ignored."
          )
      }
  )
  @ApiResponse(responseCode = "200", description = "Name usage suggestions found")
  @GetMapping(value = "match")
  public ColDpNameUsageMatch match(
      @RequestParam(value = "usageKey", required = false) String usageKey,
      @RequestParam(value = "name", required = false) String scientificName2,
      @RequestParam(value = "scientificName", required = false) String scientificName,
      @RequestParam(value = "authorship", required = false) String authorship2,
      @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
      @RequestParam(value = "rank", required = false) String rank2,
      @RequestParam(value = "taxonRank", required = false) String rank,
      @RequestParam(value = "genericName", required = false) String genericName,
      @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
      @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
      LinneanClassification classification,
      @RequestParam(value = "strict", required = false) Boolean strict,
      @RequestParam(value = "verbose", required = false) Boolean verbose) {
    return matchingService.match2(usageKey, first(scientificName, scientificName2),
        first(authorship, authorship2),
        genericName, specificEpithet, infraspecificEpithet,
        parseRank(first(rank, rank2)), classification, null, bool(strict), bool(verbose));
  }

  @Operation(
      operationId = "matchNames v2",
      summary = "Fuzzy name match service v2",
      description =
          """
              Name matching service is based on the match endpoint but format the result differently.
              This is the preferred endpoint to use as it doesn't have the limitations of the original match endpoint.
              This endpoint also provides the option to exclude parts of the taxonomic tree from matching.
              """,
      extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0130"))
  )
  @Tag(name = "Searching names")
  @Parameters(
      value = {
          @Parameter(
              name = "name",
              description = "The scientific name to fuzzy match against. May include the authorship and year"
          ),
          @Parameter(name = "scientificName", hidden = true),
          @Parameter(
              name = "authorship",
              description = "The scientific name authorship to fuzzy match against."
          ),
          @Parameter(name = "scientificNameAuthorship", hidden = true),
          @Parameter(
              name = "rank",
              description = "Filters by taxonomic rank as given in our https://api.gbif.org/v1/enumeration/basic/Rank[Rank enum].",
              schema = @Schema(implementation = Rank.class)
          ),
          @Parameter(name = "taxonRank", hidden = true),
          @Parameter(
              name = "kingdom",
              description = "Kingdom to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "phylum",
              description = "Phylum to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "order",
              description = "Order to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "class",
              description = "Class to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "family",
              description = "Family to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "genus",
              description = "Genus to match.",
              in = ParameterIn.QUERY
          ),
          @Parameter(
              name = "genericName",
              description = "Generic part of the name to match when given as atomised parts instead of the full name parameter."
          ),
          @Parameter(
              name = "specificEpithet",
              description = "Specific epithet to match."
          ),
          @Parameter(
              name = "infraspecificEpithet",
              description = "Infraspecific epithet to match."
          ),
          @Parameter(name = "classification", hidden = true),
          @Parameter(
              name = "strict",
              description = "If true it fuzzy matches only the given name, but never a taxon in the upper classification."
          ),
          @Parameter(
              name = "exclude",
              description = "Can be used to provide ColId keys of higher taxa to exclude from matching."
          ),
          @Parameter(
              name = "verbose",
              description = "If true it shows alternative matches which were considered but then rejected."
          ),
          @Parameter(
              name = "usageKey",
              description = "The usage key to look up. When provided, all other fields are ignored."
          )
      }
  )
  @ApiResponse(responseCode = "200", description = "Name usage suggestions found")
  @GetMapping(value = "match2")
  public ColNameUsageMatch2 match2(
      @RequestParam(value = "usageKey", required = false) String usageKey,
      @RequestParam(value = "name", required = false) String scientificName2,
      @RequestParam(value = "scientificName", required = false) String scientificName,
      @RequestParam(value = "authorship", required = false) String authorship2,
      @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
      @RequestParam(value = "rank", required = false) String rank2,
      @RequestParam(value = "taxonRank", required = false) String rank,
      @RequestParam(value = "genericName", required = false) String genericName,
      @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
      @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
      LinneanClassification classification,
      // higher taxon ids to exclude from matching, see https://github.com/gbif/portal-feedback/issues/4361
      @RequestParam(value = "exclude", required = false) Set<String> exclude,
      @RequestParam(value = "strict", required = false) Boolean strict,
      @RequestParam(value = "verbose", required = false) Boolean verbose) {
    return matchingService.v2(matchingService.match2(
        usageKey, first(scientificName, scientificName2), first(authorship, authorship2),
        genericName, specificEpithet, infraspecificEpithet,
        parseRank(first(rank, rank2)),
        classification, exclude, bool(strict), bool(verbose)));
  }

  @Operation(
      operationId = "batch matchNames v2",
      summary = "A batch endpoint to run the match v2",
      description =
          """
              This endpoint enables you to run multiple match v2 requests in a single call.
              This will reduce the overhead of multiple HTTP requests and is especially useful when you need to match a large number of names.
              """,
      extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0130"))
  )
  @Tag(name = "Searching names")
  @ApiResponse(responseCode = "200", description = "Name usage suggestions found")
  @PostMapping(value = "batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ColNameUsageMatch2> batch(@RequestBody List<NameUsageRequest> request) {
    var resultList = new ArrayList<ColNameUsageMatch2>();
    request.forEach(
        match -> resultList.add(matchingService.v2(matchingService.match2(match.getUsageKey(),
            first(match.getScientificName(), match.getScientificName2()),
            first(match.getAuthorship(), match.getAuthorship2()),
            match.getGenericName(), match.getSpecificEpithet(), match.getInfraspecificEpithet(),
            parseRank(first(match.getRank(), match.getRank2())), match.getClassification(),
            match.getExclude(), bool(match.getStrict()), bool(match.getVerbose())))));
    return resultList;
  }

  @Operation(
      operationId = "auto-complete",
      summary = "Auto-complete service",
      description =
          """
              Provides a simple auto-complete service for scientific names. 
              Based on the prefix provided it will provide an X number of suggestions.
              Result will be displayed in alphabetical order.
              """
  )
  @Tag(name = "Searching names")
  @Parameters(
      value = {
          @Parameter(
              name = "prefix",
              description = "The prefix that will be used to search for the name starting with the given prefix.",
              required = true
          ),
          @Parameter(
              name = "limit",
              description = "The amount of suggestions to be returned. Default is 5."
          )
      }
  )
  @ApiResponse(responseCode = "200", description = "Name usage suggestions found")
  @GetMapping(value = "auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ColNameUsageMatch2> autocomplete(
      @RequestParam(value = "prefix") String prefix,
      @RequestParam(value = "limit", required = false, defaultValue = "5") Integer limit) {
    return matchingService.autocomplete(prefix, limit);
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

  private boolean bool(Boolean bool) {
    return bool != null && bool;
  }

}
