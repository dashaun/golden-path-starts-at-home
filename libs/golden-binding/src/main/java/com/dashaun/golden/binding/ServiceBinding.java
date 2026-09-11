package com.dashaun.golden.binding;

import java.util.Map;

/**
 * One credential set, however the environment chose to deliver it.
 *
 * @param name        the binding's name, used only for logging
 * @param type        the kind of thing bound, for example "config"
 * @param source      where it was found, used only for logging
 * @param credentials the keys the binding carries
 */
public record ServiceBinding(String name, String type, String source, Map<String, String> credentials) {

    public String get(String key) {
        return credentials.get(key);
    }
}
