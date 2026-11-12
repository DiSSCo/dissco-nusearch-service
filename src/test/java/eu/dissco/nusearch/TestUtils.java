package eu.dissco.nusearch;

import eu.dissco.nusearch.domain.DigitalSpecimenEvent;
import eu.dissco.nusearch.domain.DigitalSpecimenWrapper;
import eu.dissco.nusearch.domain.response.*;
import eu.dissco.nusearch.schema.DigitalSpecimen;
import eu.dissco.nusearch.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.nusearch.schema.Identification;
import eu.dissco.nusearch.schema.TaxonIdentification;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

public class TestUtils {

	public static final JsonMapper MAPPER = new JsonMapper();

	public static final String INSTITUTION_ID = "https://ror.org/02y22ws83";

	public static final String NORMALISED_PHYSICAL_SPECIMEN_ID = "http://coldb.mnhn.fr/catalognumber/mnhn/ec/ec10867";

	public static final String MATCHED_COL_ID = "7Q8L8";

	public static final String ACCEPTED_COL_ID = "73SWK";

	private TestUtils() {
	}

	/**
	 * matching-ws response for "Aa brevis": a synonym of Myrosmodes brevis (Schltr.)
	 * Garay.
	 */
	public static NameUsageMatch givenNameUsageMatch() {
		return givenNameUsageMatch(givenAcceptedUsage("Myrosmodes brevis (Schltr.) Garay", "(Schltr.) Garay"),
				"Plantae");
	}

	public static NameUsageMatch givenNameUsageMatch(Usage acceptedUsage, String kingdom) {
		return new NameUsageMatch(
				new Usage(MATCHED_COL_ID, "Aa brevis Schltr.", "Aa brevis", "Schltr.", Rank.SPECIES, NomCode.BOTANICAL,
						"SYNONYM", null, "Aa", null, "brevis", null, "SCIENTIFIC", "<i>Aa</i> <i>brevis</i> Schltr."),
				acceptedUsage,
				List.of(new RankedName("5T6MX", "Biota", Rank.UNRANKED, null),
						new RankedName("P", kingdom, Rank.KINGDOM, null),
						new RankedName("TP", "Tracheophyta", Rank.PHYLUM, null),
						new RankedName("LI", "Liliopsida", Rank.CLASS, null),
						new RankedName("ASP", "Asparagales", Rank.ORDER, null),
						new RankedName("ORC", "Orchidaceae", Rank.FAMILY, null),
						new RankedName("8VZKC", "Myrosmodes", Rank.GENUS, null),
						new RankedName(ACCEPTED_COL_ID, "Myrosmodes brevis", Rank.SPECIES, null)),
				givenDiagnostics(MatchType.EXACT), null, null, true, null, null);
	}

	public static Usage givenAcceptedUsage(String nameWithAuthorship, String authorship) {
		return new Usage(ACCEPTED_COL_ID, nameWithAuthorship, "Myrosmodes brevis", authorship, Rank.SPECIES,
				NomCode.BOTANICAL, null, null, "Myrosmodes", null, "brevis", null, "SCIENTIFIC",
				"<i>Myrosmodes</i> <i>brevis</i> (Schltr.) Garay");
	}

	public static NameUsageMatch givenNoMatch() {
		return new NameUsageMatch(null, null, List.of(), givenDiagnostics(MatchType.NONE), null, null, false, null,
				null);
	}

	private static Diagnostics givenDiagnostics(MatchType matchType) {
		return new Diagnostics(matchType, List.of(), List.of(), matchType == MatchType.NONE ? 100 : 98, null, 1L, null,
				null, null);
	}

	public static DigitalSpecimenEvent givenDigitalSpecimenEvent() {
		return givenDigitalSpecimenEvent(givenDigitalSpecimen(OdsTopicDiscipline.BOTANY));
	}

	public static DigitalSpecimenEvent givenDigitalSpecimenEvent(OdsTopicDiscipline topicDiscipline) {
		return givenDigitalSpecimenEvent(givenDigitalSpecimen(topicDiscipline));
	}

	public static DigitalSpecimenEvent givenDigitalSpecimenEvent(DigitalSpecimen digitalSpecimen) {
		return new DigitalSpecimenEvent(Set.of("AAS"),
				new DigitalSpecimenWrapper(NORMALISED_PHYSICAL_SPECIMEN_ID,
						"https://doi.org/21.T11148/894b1e6cad57e921764e", digitalSpecimen, MAPPER.createObjectNode()),
				List.of(MAPPER.createObjectNode()), false, false);
	}

	public static DigitalSpecimen givenDigitalSpecimen(OdsTopicDiscipline topicDiscipline) {
		return givenDigitalSpecimen(topicDiscipline, "PreservedSpecimen",
				new TaxonIdentification().withDwcScientificName("Aa brevis").withDwcOrder("Asparagales"));
	}

	public static DigitalSpecimen givenDigitalSpecimen(OdsTopicDiscipline topicDiscipline, String basisOfRecord,
			TaxonIdentification... taxonIdentifications) {
		return new DigitalSpecimen().withOdsNormalisedPhysicalSpecimenID(NORMALISED_PHYSICAL_SPECIMEN_ID)
			.withOdsOrganisationID(INSTITUTION_ID)
			.withDwcBasisOfRecord(basisOfRecord)
			.withOdsTopicDiscipline(topicDiscipline)
			.withOdsSpecimenName("Aa brevis")
			.withOdsHasIdentifications(
					List.of(new Identification().withOdsHasTaxonIdentifications(List.of(taxonIdentifications))));
	}

}
