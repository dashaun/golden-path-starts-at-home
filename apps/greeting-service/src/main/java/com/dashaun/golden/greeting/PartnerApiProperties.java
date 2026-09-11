package com.dashaun.golden.greeting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The secret half. These values are never written down anywhere a developer
 * can read them: the config server fetches them from Vault and merges them
 * into the same response as the settings above.
 *
 * Mutable for the same reason as {@link GreetingProperties}: rotation depends
 * on Spring Cloud being able to rebind this bean without a restart.
 */
@ConfigurationProperties(prefix = "partner.api")
public class PartnerApiProperties {

    private String key = "";
    private String tenant = "";

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTenant() {
        return this.tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
