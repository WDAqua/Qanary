package eu.wdaqua.qanary;

import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.communications.RestTemplateWithProcessId;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertAll;

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

    @Test
    public void restTemplateSettingTestCaseA() {
        contextRunner
                .withPropertyValues("rest.template.setting=A")
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplateWithCaching.class)
                                    .hasSingleBean(RestTemplateWithProcessId.class)
                    );
                });
    }

    @Test
    public void restTemplateSettingTestCaseB() {
        contextRunner
                .withPropertyValues("rest.template.setting=B")
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplateWithCaching.class)
                                    .doesNotHaveBean(RestTemplateWithProcessId.class)
                    );
                });
    }

    @Test
    public void restTemplateSettingTestCaseC() {
        contextRunner
                .withPropertyValues("rest.template.setting=C")
                .run(context -> {
                    assertAll(
                            () -> Assertions.assertThat(context).hasSingleBean(RestTemplateWithProcessId.class)
                    );
                });
    }
}
