package eu.dissco.nusearch.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.Nulls;
import eu.dissco.nusearch.property.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.NameParserGbifV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.fasterxml.jackson.annotation.JsonSetter.Value;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfiguration {

    public static final String DATE_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private final ApplicationProperties properties;

    @Bean
    public NameParserGbifV1 nameParserGbifV1() {
        return new NameParserGbifV1(new NameParserGBIF(properties.getNameParserThreadTimeOut(), 0,
                properties.getNameParserMaxThreads()));
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .defaultDateFormat(new SimpleDateFormat(DATE_STRING))
                .defaultTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
                // To get null lists, maps, and sets as empty collections instead:
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(Include.NON_NULL))
                .withConfigOverride(List.class, cfg -> cfg.setNullHandling(Value.forValueNulls(Nulls.AS_EMPTY)))
                .withConfigOverride(Map.class, cfg -> cfg.setNullHandling(Value.forValueNulls(Nulls.AS_EMPTY)))
                .withConfigOverride(Set.class, cfg -> cfg.setNullHandling(Value.forValueNulls(Nulls.AS_EMPTY)))
                .build();
    }
}
