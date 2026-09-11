package com.dashaun.golden.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param name         how the agent introduces itself
 * @param maxToolCalls the budget the agent is told to stay inside for one question
 */
@ConfigurationProperties(prefix = "golden.agent")
public record StewardProperties(String name, int maxToolCalls) {

    public StewardProperties {
        name = (name == null || name.isBlank()) ? "repository-steward" : name;
        maxToolCalls = maxToolCalls <= 0 ? 12 : maxToolCalls;
    }
}
