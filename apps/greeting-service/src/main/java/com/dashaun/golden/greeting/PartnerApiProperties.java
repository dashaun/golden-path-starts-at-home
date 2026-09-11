package com.dashaun.golden.greeting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The secret half. These values are never written down anywhere a developer
 * can read them: the config server fetches them from Vault and merges them
 * into the same response as the settings above.
 */
@ConfigurationProperties(prefix = "partner.api")
public record PartnerApiProperties(String key, String tenant) {
}
