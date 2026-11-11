package eu.dissco.nusearch.utils;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GbifTextUtilsTest {

    private static Map<String, String> mapOf(String... keyValues) {
        var map = new HashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    static Stream<Arguments> streamToMapCases() {
        return Stream.of(
                arguments(Named.of("key and value columns, as in authormap.txt",
                                "L.\tLinn\tLinnaeus\nDC.\tde C.\tDe Candolle\n"), 0, 2, true,
                        mapOf("L.", "Linnaeus", "DC.", "De Candolle")),
                arguments(Named.of("comments, blank and short lines are skipped",
                                "# comment\n\n   \t\nonly-one-column\na\tb\n"), 0, 1, true,
                        mapOf("a", "b")),
                arguments(Named.of("indented # is not a comment, as in gbif-common",
                                "  #a\tb\n"), 0, 1, true,
                        mapOf("#a", "b")),
                arguments(Named.of("trimToNull trims and turns blanks into null",
                                " a \t b \nc\t \t\n"), 0, 1, true,
                        mapOf("a", "b", "c", null)),
                arguments(Named.of("without trimToNull whitespace is kept",
                                " a \t b \nc\t \t\n"), 0, 1, false,
                        mapOf(" a ", " b ", "c", " ")),
                arguments(Named.of("later line wins for a duplicate key",
                                "a\tfirst\na\tsecond\n"), 0, 1, true,
                        mapOf("a", "second")),
                arguments(Named.of("key column after value column, short line skipped",
                                "v1\tk1\nonly-value\nv2\tk2\textra\n"), 1, 0, true,
                        mapOf("k1", "v1", "k2", "v2")),
                arguments(Named.of("UTF-8 content",
                                "Müll.\tMüller\n"), 0, 1, true,
                        mapOf("Müll.", "Müller"))
        );
    }

    static Stream<Arguments> streamToSetCases() {
        return Stream.of(
                arguments(Named.of("lines are trimmed", " Incertae sedis \n"),
                        Set.of("Incertae sedis")),
                arguments(Named.of("comments, also indented, are skipped",
                        "# comment\n  # indented comment\nunassigned\n"), Set.of("unassigned")),
                arguments(Named.of("blank lines are skipped", "\n   \n\t\nunassigned\n"),
                        Set.of("unassigned")),
                arguments(Named.of("duplicates are removed", "unassigned\nunassigned\n unassigned\n"),
                        Set.of("unassigned")),
                arguments(Named.of("UTF-8 content", "Müller\n"), Set.of("Müller")),
                arguments(Named.of("empty input", ""), Set.of())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "Müller, Muller",
            "Abiés albà, Abies alba",
            "Abiés, Abies",
            "Hieracium scorzoneræfolium, Hieracium scorzoneraefolium",
            "Straße, Strasse",
            "Æsculus, AEsculus",
            "Œnothera œ, OEnothera oe",
            "Øresund ø, Oresund o",
            "Łódź ł, Lodz l",
            "Ðð đ, Dd d",
            "Ŧŧ, Tt"
    })
    
    void testFoldToAscii(String input, String expected) {
        // When
        var result = GbifTextUtils.foldToAscii(input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Abies alba", "日本 ×Agropogon"})
    void testFoldToAsciiUnchanged(String input) {
        // When
        var result = GbifTextUtils.foldToAscii(input);

        // Then
        assertThat(result).isEqualTo(input);
    }

    @ParameterizedTest
    @MethodSource("streamToMapCases")
    void testStreamToMap(String content, int key, int value, boolean trimToNull,
                         Map<String, String> expected) throws IOException {
        // When
        var result = GbifTextUtils.streamToMap(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), key, value, trimToNull);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testStreamToMapAddsToGivenMap() throws IOException {
        // Given
        var existing = mapOf("x", "y");

        // When
        var result = GbifTextUtils.streamToMap(new ByteArrayInputStream("a\tb\n".getBytes(StandardCharsets.UTF_8)), existing, 0, 1, true);

        // Then
        assertThat(result).isSameAs(existing).containsOnly(entry("x", "y"), entry("a", "b"));
    }

    @ParameterizedTest
    @MethodSource("streamToSetCases")
    void testStreamToSet(String content, Set<String> expected) throws IOException {
        // When
        var result = GbifTextUtils.streamToSet(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        // Then
        assertThat(result).isEqualTo(expected);
    }

}
