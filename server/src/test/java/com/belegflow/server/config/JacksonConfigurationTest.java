package com.belegflow.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Loads the real application.yml without a database: regression for uploaded CI log 106009699774. */
class JacksonConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test void productionYamlBindsAndCreatesJackson3Mapper() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JsonMapper.class);
        });
    }
    @Test void localDateUsesIsoTextNotAnArray() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JsonMapper.class).writeValueAsString(LocalDate.of(2026,10,18)))
                .isEqualTo("\"2026-10-18\"");
        });
    }
    @Test void instantUsesIsoTextNotEpochNumbers() {
        runner.run(context -> assertThat(context.getBean(JsonMapper.class)
            .writeValueAsString(Instant.parse("2026-10-18T12:00:00Z")))
            .isEqualTo("\"2026-10-18T12:00:00Z\""));
    }
    @Test void legacySerializationPropertyReproducesTheOriginalFailure() {
        runner.withPropertyValues("spring.jackson.serialization.write-dates-as-timestamps=false")
            .run(context -> assertThat(context).hasFailed());
    }
}
