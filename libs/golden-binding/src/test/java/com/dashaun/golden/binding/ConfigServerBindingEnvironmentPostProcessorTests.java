package com.dashaun.golden.binding;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServerBindingEnvironmentPostProcessorTests {

    private final ConfigServerBindingEnvironmentPostProcessor processor =
            new ConfigServerBindingEnvironmentPostProcessor(deferredLogFactory());

    @Test
    void readsABindingFromAServiceBindingRootDirectory(@TempDir Path root) throws Exception {
        Path binding = Files.createDirectories(root.resolve("golden-config"));
        Files.writeString(binding.resolve("type"), "config");
        Files.writeString(binding.resolve("uri"), "https://config-server.example.com");
        Files.writeString(binding.resolve("username"), "golden");
        Files.writeString(binding.resolve("password"), "s3cret");

        MockEnvironment environment = new MockEnvironment()
                .withProperty(ServiceBindingRootSource.ROOT_VARIABLE, root.toString());

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.config.uri"))
                .isEqualTo("https://config-server.example.com");
        assertThat(environment.getProperty("spring.cloud.config.username")).isEqualTo("golden");
        assertThat(environment.getProperty("spring.cloud.config.password")).isEqualTo("s3cret");
    }

    @Test
    void readsTheSameBindingFromVcapServices() {
        String vcap = """
                {
                  "user-provided": [
                    {
                      "name": "golden-config",
                      "tags": ["config"],
                      "credentials": {
                        "uri": "https://config-server.example.com",
                        "username": "golden",
                        "password": "s3cret"
                      }
                    }
                  ]
                }
                """;

        MockEnvironment environment = new MockEnvironment()
                .withProperty(VcapServicesSource.VCAP_VARIABLE, vcap);

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.config.uri"))
                .isEqualTo("https://config-server.example.com");
        assertThat(environment.getProperty("spring.cloud.config.username")).isEqualTo("golden");
        assertThat(environment.getProperty("spring.cloud.config.password")).isEqualTo("s3cret");
    }

    @Test
    void ignoresBindingsOfOtherTypes() {
        String vcap = """
                {"user-provided":[{"name":"a-database","tags":["postgresql"],
                 "credentials":{"uri":"postgres://example"}}]}
                """;

        MockEnvironment environment = new MockEnvironment()
                .withProperty(VcapServicesSource.VCAP_VARIABLE, vcap);

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.config.uri")).isNull();
    }

    @Test
    void doesNothingWhenNothingIsBound() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.config.uri")).isNull();
    }

    private static DeferredLogFactory deferredLogFactory() {
        return destination -> new org.apache.commons.logging.impl.NoOpLog();
    }
}
