package eu.wdaqua.qanary;

import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.communications.RestTemplateWithProcessId;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * This test class is intended to test the existence or lack of property-depending beans
 */
@SpringBootTest
public class EnvironmentalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(QanaryPipeline.class)
            .withPropertyValues("server.host=http://localhost")
            .withPropertyValues("server.port=8080")
            .withPropertyValues("qanary.endpoint=http://localhost:8080")
            .withPropertyValues("qanary.components=test")
            .withPropertyValues("qanary.ontology=test")
            .withPropertyValues("spring.application.name=test")
            .withPropertyValues("explanation.service=test")
            .withPropertyValues("spring.boot.admin.url=http://localhost:8080")
            .withPropertyValues("pipeline.as.component=false")
            .withPropertyValues("configuration.access=web")
            .withPropertyValues("springdoc.version=1.0.0");

    /**
     * Test case: Setting 'both' should lead to existence of RestTemplateWithCaching and RestTemplateWithProcessId
     */
    @Test
    public void restTemplateSettingTestCaseA() {
        contextRunner
                .withPropertyValues("rest.template.setting=both")
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context)
                                    .hasSingleBean(RestTemplateWithCaching.class)
                                    .hasSingleBean(RestTemplateWithProcessId.class)
                    );
                });
    }

    /**
     * Test case: Setting 'caching' should lead to existence of RestTemplateWithCaching (only)
     */
    @Test
    public void restTemplateSettingTestCaseB() {
        contextRunner
                .withPropertyValues("rest.template.setting=caching")
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplateWithCaching.class)
                                    .doesNotHaveBean(RestTemplateWithProcessId.class)
                    );
                });
    }

    /**
     * Test case: Setting 'logging' should lead to existence of RestTemplateWithProcessId (without caching functionality)
     */
    @Test
    public void restTemplateSettingTestCaseC() {
        contextRunner
                .withPropertyValues("rest.template.setting=logging")
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplateWithProcessId.class)
                    );
                });
    }

    /**
     * Test case: Setting non-existent property should lead to RestTemplateWithCaching
     * All Qanary components will contain caching as before without adding the property to each.
     */
    @Test
    public void restTemplateSettingTestCaseNonExistent() {
        contextRunner
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplateWithCaching.class)
                    );
                });
    }

    /**
     * Test case: Setting '' (empty) should lead to existence of RestTemplate
     * Handles a non-defined state
     */
    @Test
    public void restTemplateSettingTestCaseEmpty() {
        contextRunner
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplate.class)
                    );
                });
    }

}
