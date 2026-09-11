package com.dashaun.golden.greeting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ordinary settings. Every value here arrives from the config server's
 * git-backed store, so all of it is reviewable in a pull request.
 *
 * These are mutable on purpose. Spring Cloud rebinds configuration properties
 * in place when the application refreshes, and it cannot do that to a record.
 */
@ConfigurationProperties(prefix = "greeting")
public class GreetingProperties {

    private String message = "";
    private String audience = "";
    private String theme = "";

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAudience() {
        return this.audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTheme() {
        return this.theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
