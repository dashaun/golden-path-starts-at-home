package com.dashaun.golden.binding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Turns a bound config server into the three properties Spring Cloud Config
 * needs, before {@code spring.config.import} is resolved.
 *
 * An application therefore never names a host, a user, or a password. It says
 * it wants a config server and the platform says which one. The laptop, Cloud
 * Foundry, and Kubernetes each keep the binding somewhere different, and this
 * is the only class that knows about the difference.
 */
public class ConfigServerBindingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** The binding type this looks for, whatever the instance happens to be named. */
    public static final String BINDING_TYPE = "config";

    private static final String PROPERTY_SOURCE_NAME = "goldenConfigServerBinding";

    private final Log log;
    private final List<BindingSource> sources = List.of(new ServiceBindingRootSource(), new VcapServicesSource());

    public ConfigServerBindingEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(ConfigServerBindingEnvironmentPostProcessor.class);
    }

    @Override
    public int getOrder() {
        // Early enough that spring.config.import=configserver: already has a uri.
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        apply(environment);
    }

    private void apply(ConfigurableEnvironment environment) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Optional<ServiceBinding> binding = sources.stream()
                .flatMap(source -> source.bindings(environment).stream())
                .filter(candidate -> BINDING_TYPE.equalsIgnoreCase(candidate.type()))
                .findFirst();

        if (binding.isEmpty()) {
            log.debug("No config server binding found; leaving spring.cloud.config.* alone");
            return;
        }

        ServiceBinding found = binding.get();
        Map<String, Object> properties = new LinkedHashMap<>();
        put(properties, "spring.cloud.config.uri", found.get("uri"));
        put(properties, "spring.cloud.config.username", found.get("username"));
        put(properties, "spring.cloud.config.password", found.get("password"));
        put(properties, "spring.cloud.config.label", found.get("label"));

        if (properties.isEmpty()) {
            log.warn("Binding '%s' has type '%s' but carries no uri".formatted(found.name(), found.type()));
            return;
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        log.info("Bound to config server '%s' from %s".formatted(found.name(), found.source()));
    }

    private static void put(Map<String, Object> properties, String key, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(key, value);
        }
    }
}
