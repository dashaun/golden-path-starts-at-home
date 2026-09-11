package com.dashaun.golden.greeting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ordinary settings. Every value here arrives from the config server's
 * git-backed store, so all of it is reviewable in a pull request.
 */
@ConfigurationProperties(prefix = "greeting")
public record GreetingProperties(String message, String audience, String theme) {
}
